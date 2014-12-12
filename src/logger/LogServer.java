package logger;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import gossip.Config;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class LogServer {

  private Logger log;
  private Config config = Config.configure();
  public String msg;

  public LogServer(Logger logger, int port) throws Exception {
    log = logger;

    // Configure and start the server
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    // Create members context
    server.createContext(Handlers.MEMBERS.getPath(),
                         new MyHandler(Handlers.MEMBERS));
    // Create event context
    server.createContext(Handlers.EVENTS.getPath(),
                         new MyHandler(Handlers.EVENTS));
    server.setExecutor(null); // creates a default executor
    server.start(); // Starts server in a new thread

    // Start thread for communicating to host machine
    new Thread(new PingListener()).start();
  }

  public class PingListener implements Runnable {
    public PingListener() {

    }

    public void run() {
      try {
        // Receive ping from host
        InetAddress addr = NetworkInterface.getByName(config.INTERFACE_NAME).getInetAddresses().nextElement();
        log.addInfo("LOGGER: Waiting for host ping on addr: " + addr.getHostAddress() + ":" + config.PING_PORT);
        DatagramSocket socket = new DatagramSocket(config.PING_PORT, addr);
        byte[] buff = new byte[256];
        DatagramPacket packet = new DatagramPacket(buff, buff.length);
        socket.receive(packet);
        String port_str = new String(packet.getData(), 0, packet.getLength());
        int port_num = Integer.parseInt(port_str);
        log.setHostAddr(packet.getAddress());
        log.setHostPort(port_num);
        log.addInfo("LOGGER: Received host ping - " + log.getHostAddr().toString() + ":" + log.getHostPort());

        // Send ping back to host
        buff = "I'm here".getBytes();
        packet = new DatagramPacket(buff, buff.length, log.getHostAddr(), log.getHostPort());
        socket.send(packet);
        log.addInfo("LOGGER: Send ping back to host");

        while (true) {
          synchronized (log) {
            log.wait();
          }
          buff = msg.getBytes();
          packet = new DatagramPacket(buff, buff.length, log.getHostAddr(), log.getHostPort());
          socket.send(packet);
        }
      } catch (SocketException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public enum Handlers {
    MEMBERS ("/members"),
    EVENTS ("/events");

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
      Map<String, String> params = splitQuery(uri.getQuery());
      if (my_handler == Handlers.MEMBERS) {
        getMembers(t, params);
      } else if (my_handler == Handlers.EVENTS) {
        getEvents(t, params);
      } else {
        System.out.println("No handler");
      }
    }

    @SuppressWarnings("unchecked")
    public void getMembers(HttpExchange t, Map<String, String> params) throws IOException {
      // Build response
      StringBuilder str = new StringBuilder();
      str.append(params.get("callback")).append("("); // Set callback function

      JSONArray arr = log.getMembersJSON();
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
      JSONArray arr = log.getEventsJSON();

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
  }
}
