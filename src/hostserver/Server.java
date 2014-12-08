package hostserver;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Created by elmer on 12/7/14.
 */
public class Server {

  public static Map<String, Boolean> status;
  public static int counter = 0;

  public Server(int port) throws Exception {

    // Configure and start the server
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    // Create members context
    server.createContext(Handlers.START.getPath(),
                         new MyHandler(Handlers.START));
    // Create event context
    server.createContext(Handlers.END.getPath(),
                         new MyHandler(Handlers.END));
    // Create status context
    server.createContext(Handlers.STATUS.getPath(),
                         new MyHandler(Handlers.STATUS));
    server.setExecutor(null); // creates a default executor
    server.start(); // Starts server in a new thread
  }

  public enum Handlers {
    START("/start"),
    END("/stop"),
    STATUS("/status");

    private final String path;

    Handlers(String handler_path) {
      path = handler_path;
    }

    String getPath() {
      return path;
    }
  }

  class MyHandler implements HttpHandler {
    private Handlers my_handler;

    public MyHandler(Handlers handler) {
      my_handler = handler;
    }

    public Map<String, String> splitQuery(String query)
      throws UnsupportedEncodingException {
      if (query == null) {
        return null;
      }
      Map<String, String> query_pairs = new LinkedHashMap<String, String>();
      String[] pairs = query.split("&");
      for (String pair : pairs) {
        int idx = pair.indexOf("=");
        query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                        URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
      }
      return query_pairs;
    }

    public void handle(HttpExchange httpExchange) throws IOException {
      // Print request information
      URI uri = httpExchange.getRequestURI();
      Map<String, String> params = splitQuery(uri.getQuery());
      if (my_handler == Handlers.START) {
        startVM(httpExchange, params);
      } else if (my_handler == Handlers.END) {
        stopVM(httpExchange, params);
      } else if (my_handler == Handlers.STATUS) {
        getStatus(httpExchange, params);
      } else {
        throw new UnsupportedEncodingException("No handler");
      }
    }

    @SuppressWarnings("unchecked")
    public void startVM(HttpExchange httpExchange,
                        Map<String, String> params) throws IOException {
      // Build response
      StringBuilder str = new StringBuilder();
      str.append(params.get("callback")).append("("); // Set callback function
      JSONObject obj = new JSONObject();
      obj.put("start", true);
      str.append(obj.toJSONString());
      str.append(");");
      String response = str.toString();

      sendJSONResponse(httpExchange, response.getBytes());
    }

    @SuppressWarnings("unchecked")
    public void stopVM(HttpExchange httpExchange,
                       Map<String, String> params) throws IOException {
      // Build response
      StringBuilder str = new StringBuilder();
      str.append(params.get("callback")).append("("); // Set callback function
      JSONObject obj = new JSONObject();
      obj.put("end", true);
      str.append(obj.toJSONString());
      str.append(");");
      String response = str.toString();

      sendJSONResponse(httpExchange, response.getBytes());
    }

    @SuppressWarnings("unchecked")
    public void getStatus(HttpExchange httpExchange,
                          Map<String, String> params) throws IOException {
      counter++;
      System.out.println("Counter: " + counter);
      if (counter/10 == 1) { status.put("virtual_machine", true); }
      else if (counter/10 == 2) { status.put("gossip", true); }
      else if (counter/10 == 3) { status.put("in_group", true); }
      else if (counter/10 == 4) { status.put("master_elected", true); }
      else if (counter/10 == 5) { status.put("hadoop_started", true); }
      // Build response
      StringBuilder str = new StringBuilder();
      str.append(params.get("callback")).append("("); // Set callback function

      JSONObject obj = new JSONObject();
      for (String k : status.keySet()) {
        obj.put(k, status.get(k));
      }
      str.append(obj.toJSONString());
      str.append(");");
      String response = str.toString();

      sendJSONResponse(httpExchange, response.getBytes());
    }

    public void sendJSONResponse(HttpExchange httpExchange, byte[] msg)
      throws IOException {
      // Set header for HTML file
      Headers header = httpExchange.getResponseHeaders();
      header.add("Content-Type", "application/json");
      // Send response to client
      httpExchange.sendResponseHeaders(200, msg.length);
      OutputStream os = httpExchange.getResponseBody();
      os.write(msg);
      os.close();
    }
  }

  public static void main(String args[]) throws Exception {
    // Set status map
    status = new HashMap<String, Boolean>();
    status.put("virtual_machine", false);
    status.put("gossip", false);
    status.put("in_group", false);
    status.put("master_elected", false);
    status.put("hadoop_started", false);

//    Scanner in = new Scanner(System.in);
//    System.out.println("Port number:");
//    int port = in.nextInt();
//    Server s = new Server(port);
    Server s = new Server(8080);
  }
}
