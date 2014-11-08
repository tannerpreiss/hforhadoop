package http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import gossip.Logger;
import gossip.Member;
import gossip.Node;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public class LogServer {

  private Logger my_log;

  public LogServer(Logger logger) throws Exception {
    my_log = logger;

    // Configure and start the server
    HttpServer server = HttpServer.create(new InetSocketAddress(8001), 0);
    // Create members context
    server.createContext(Handlers.MEMBERS.getPath(),
        new MyHandler(Handlers.MEMBERS));
    // Create event context
    server.createContext(Handlers.EVENTS.getPath(),
        new MyHandler(Handlers.EVENTS));
    // Create index file context
    server.createContext(Handlers.INDEX.getPath(),
                         new MyHandler(Handlers.INDEX));
    server.setExecutor(null); // creates a default executor
    server.start(); // Starts server in a new thread
  }

  public enum Handlers {
    MEMBERS ("/members"),
    EVENTS ("/events"),
    INDEX ("/");

    private final String path;

    Handlers(String handler_path) {
      path = handler_path;
    }

    String getPath() { return path; }
  }

  class MyHandler implements HttpHandler {
    private Handlers my_handler;

    public MyHandler(Handlers handler) {
      my_handler = handler;
    }

    public Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
      if (query == null) { return null; }
      Map<String, String> query_pairs = new LinkedHashMap<String, String>();
      String[] pairs = query.split("&");
      for (String pair : pairs) {
        int idx = pair.indexOf("=");
        query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
      }
      return query_pairs;
    }

    public void handle(HttpExchange t) throws IOException {
      // Print request information
      URI uri = t.getRequestURI();
      System.out.println("Path: " + uri.getPath());
      System.out.println("Host: " + uri.getHost());
      System.out.println("Port: " + uri.getPort());
      System.out.println("Query: " + uri.getQuery());
      Map<String, String> params = splitQuery(uri.getQuery());
      System.out.println("Check");
//      Headers request_header = t.getRequestHeaders();
//      Set<String> keys = request_header.keySet();
//      for (String k : keys) {
//        System.out.println(k + ": " + request_header.get(k));
//      }
      System.out.println("------------------------------------------\n");
      if (my_handler == Handlers.MEMBERS) {
        System.out.println("Member handler");
        getMembers(t, params);
      } else if (my_handler == Handlers.EVENTS) {
        System.out.println("Event handler");
        getEvents(t, params);
      } else if (my_handler == Handlers.INDEX) {
        System.out.println("Index handler");
        getIndex(t);
      } else {
        System.out.println("No handler");
      }
    }

    @SuppressWarnings("unchecked")
    public void getMembers(HttpExchange t, Map<String, String> params) throws IOException {
      // Build response
      StringBuilder str = new StringBuilder();
      str.append(params.get("callback")).append("("); // Set callback function

      Node node = my_log.getNodeObj();
      JSONArray arr = new JSONArray();
      synchronized (node.getMembers()) {
        for (Member m : node.getMembers()) {
          arr.add(m.toJSON());
        }
      }
      JSONObject obj = new JSONObject();
      obj.put("member_list", arr);

      str.append(obj.toString());
      str.append(");");
      String response = str.toString();

      // Set header for HTML file
      Headers h = t.getResponseHeaders();
      h.add("Content-Type","application/json");
      t.sendResponseHeaders(200, response.length());
      OutputStream os = t.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }

    @SuppressWarnings("unchecked")
    public void getEvents(HttpExchange t, Map<String, String> params) throws IOException {
      // Build response
      StringBuilder str = new StringBuilder();
      str.append(params.get("callback")).append("("); // Set callback function
      ArrayList<String> events = my_log.getEvents();
      JSONArray arr = new JSONArray();
      synchronized (events) {
        for (String e : events) {
          arr.add(e);
        }
        events.clear();
      }

      JSONObject obj = new JSONObject();
      obj.put("event_list", arr);

      str.append(obj.toString());
      str.append(");");
      String response = str.toString();

      Headers h = t.getResponseHeaders();
      h.add("Content-Type", "application/json");

      t.sendResponseHeaders(200, response.length());
      OutputStream os = t.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }

    public void getIndex(HttpExchange t) throws IOException {
      // Set header for HTML file
      Headers h = t.getResponseHeaders();
      h.add("Content-Type", "text/HTML");

      // Read file into byte array
      File file = new File("index.html");
      byte[] byte_array = new byte[(int)file.length()];
      FileInputStream fis = new FileInputStream(file);
      BufferedInputStream bis = new BufferedInputStream(fis);
      bis.read(byte_array, 0, byte_array.length);

      // Send response to client
      t.sendResponseHeaders(200, file.length());
      OutputStream os = t.getResponseBody();
      os.write(byte_array, 0, byte_array.length);
      os.close();
    }
  }
}
