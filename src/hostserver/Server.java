package hostserver;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import gossip.Config;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import vm_control.Shell;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by elmer on 12/7/14.
 */
public class Server {

  private static Config config = Config.configure();
  private static Map<String, Boolean> status;
  private static int           counter     = 0;
  private static String        vmAddr      = "";
  private static AtomicBoolean keepPinging = new AtomicBoolean(true);

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

  private class ShellExecutor implements Runnable {
    private String cmd;
    public ShellExecutor(String command) {
      this.cmd = command;
    }
    public void run() {
      if (cmd.equals("start")) {
        runStart();
      } else if (cmd.equals("stop")) {
        runStop();
      } else if (cmd.equals("ping")) {
        runPing();
      } else if (cmd.equals("listen")) {
        runListen();
      }
    }

    public void runStart() {
      Shell.executeCommand("python start_vm.py");
      System.out.println("VM is started");
      synchronized (vmAddr) {
        while (vmAddr.equals("")) {
          vmAddr = Shell.executeCommand("python get_ip.py").trim();
          System.out.println(vmAddr.trim());
          try {
            Thread.sleep(2000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
      System.out.println("VM Address: " + vmAddr);
      synchronized (status) { status.put("virtual_machine", true); }
      System.out.println("Start to ping VM");
      Thread ping = new Thread(new ShellExecutor("ping"));
      ping.start();
      System.out.println("Start listening to VM");
      Thread listener = new Thread(new ShellExecutor("listen"));
      listener.start();
    }

    public void runStop() {
      Shell.executeCommand("python stop_vm.py");
      System.out.println("VM is shutdown");
      synchronized (status) { status.put("virtual_machine", false); }
    }

    public void runPing() {
      try {
        DatagramSocket socket = new DatagramSocket(config.PING_PORT);
        while (keepPinging.get()) {
          byte[] buff = "are you there?".getBytes();
          DatagramPacket packet =
            new DatagramPacket(buff, buff.length,
                               InetAddress.getByName(vmAddr), config.PING_PORT);
          socket.send(packet);
        }
      } catch (UnknownHostException e) {
        e.printStackTrace();
      } catch (SocketException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    public void runListen() {
      try {
        DatagramSocket socket = new DatagramSocket(config.PING_PORT);
        byte[] buff = new byte[256];
        DatagramPacket packet = new DatagramPacket(buff, buff.length);
        socket.receive(packet);
        if (new String(buff).equals("i'm here")) {
          keepPinging.set(false);
          synchronized (status) { status.put("gossip", true); }
        }
      } catch (SocketException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
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
      Thread script = new Thread(new ShellExecutor("start"));
      script.start();

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
      Thread script = new Thread(new ShellExecutor("stop"));
      script.start();

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
    synchronized (status) {
      status.put("virtual_machine", false);
      status.put("gossip", false);
      status.put("in_group", false);
      status.put("master_elected", false);
      status.put("hadoop_started", false);
    }

//    Scanner in = new Scanner(System.in);
//    System.out.println("Port number:");
//    int port = in.nextInt();
//    Server s = new Server(port);
    System.out.println("Start server");
    Server s = new Server(8080);
  }
}
