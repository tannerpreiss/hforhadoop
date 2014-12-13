package hostserver;

import cmd.Shell;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import gossip.Config;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by elmer on 12/7/14.
 */
public class Server {

  private static Config config = Config.configure();
  private static Map<String, String> status;
  private static String        vmAddr      = "";
  private static AtomicBoolean keepPinging = new AtomicBoolean(true);
  private static String vmName;
  private static int vmNumber = 0;
  private static int localhost_port;
  private static int receive_port;
  private static boolean format_time = false;

  public static String getTimeStamp() {
    if (format_time) {
      return new SimpleDateFormat("HH:mm:ss.SSS").format(new Timestamp(System.currentTimeMillis()));
    } else {
      return Long.toString(System.currentTimeMillis());
    }
  }

  public static void printLog(String str) {
    System.out.println("==> " + vmName + ": " + str);
  }

  public static void printTime(String str) {
    System.out.println("[TEST] " + vmName + ": " + str);
  }

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
    server.createContext(Handlers.VM_ADDR.getPath(),
                         new MyHandler(Handlers.VM_ADDR));
    server.setExecutor(null); // creates a default executor
    server.start(); // Starts server in a new thread
  }

  public enum Handlers {
    START("/start"),
    END("/stop"),
    STATUS("/status"),
    VM_ADDR("/vmaddr");

    private final String path;

    Handlers(String handler_path) {
      path = handler_path;
    }

    String getPath() {
      return path;
    }
  }

  private class TaskExecutor implements Runnable {
    private String cmd;

    public TaskExecutor(String command) {
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
      String result = Shell.executeCommand("./vagrant_start.sh " + vmName).trim();
      if (result.indexOf("\n") != -1) {
        result = result.substring(0, result.indexOf("\n")).trim();
      }
      synchronized (vmAddr) {
        vmAddr = result;
      }
      printLog("VM started - [" + vmName + "] IP Address: " + vmAddr);
      synchronized (status) {
        String time = Server.getTimeStamp();
        status.put("virtual_machine", time);
        printTime(time + " ");
      }
      printLog("Start to ping VM");
      Thread ping = new Thread(new TaskExecutor("ping"));
      ping.start();
      printLog("Start listening to VM");
      Thread listener = new Thread(new TaskExecutor("listen"));
      listener.start();
    }

    public void runStop() {
      String result = Shell.executeCommand("./vagrant_stop.sh " + vmName);
      printLog("VM stopped [" + vmName + "]");
//      printLog("VM is shutdown");
      synchronized (status) {
        status.put("virtual_machine", null);
        status.put("gossip", null);
        status.put("in_group", null);
        status.put("master_elected", null);
        status.put("hadoop_started", null);
      }
      System.exit(0);
    }

    public void runPing() {
      try {
        DatagramSocket socket = new DatagramSocket(0);
        printLog("Ping VM: " + vmAddr + ":" + config.PING_PORT);
        while (keepPinging.get()) {
          byte[] buff = Integer.toString(receive_port).getBytes();
          DatagramPacket packet =
            new DatagramPacket(buff, buff.length,
                               InetAddress.getByName(vmAddr), config.PING_PORT);
          socket.send(packet);
          Thread.sleep(2000);
        }
      } catch (UnknownHostException e) {
        e.printStackTrace();
      } catch (SocketException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    public void runListen() {
      try {
        // Start listening for acknowledgement
        DatagramSocket socket = new DatagramSocket(receive_port);
        byte[] buff = new byte[256];
        DatagramPacket packet = new DatagramPacket(buff, buff.length);
        printLog("Listening for acknowledgement - port: " + receive_port);
        socket.receive(packet);

        // Received acknowledgement
        printLog("Acknowledgement received!");
        printTime(Server.getTimeStamp() + " start_gossip");
        keepPinging.set(false);
        synchronized (status) {
          status.put("gossip", Server.getTimeStamp());
        }

        AtomicBoolean keepRunning = new AtomicBoolean(true);
        int count = 0;
        printLog("Listening for events");
        while (keepRunning.get()) {
          // Start listening for acknowledgement
          packet = new DatagramPacket(buff, buff.length);
          socket.receive(packet);

          // Received acknowledgement
          String data = new String(packet.getData(), 0, packet.getLength()).trim();
          printLog("Received data: " + data);
          if (data.indexOf("in_group") != -1) {
            synchronized (status) {
              String time = Server.getTimeStamp();
              status.put("in_group", time);
              printTime(time + " in_group");
            }
            count++;
          } else if (data.indexOf("master_elected") != -1) {
            synchronized (status) {
              String time = Server.getTimeStamp();
              status.put("master_elected", time);
              printTime(time + " master_elected");
            }
            count++;
          } else if (data.indexOf("hadoop_started") != -1) {
            synchronized (status) {
              String time = Server.getTimeStamp();
              status.put("hadoop_started", time);
              printTime(time + " hadoop_started");
            }
            count++;
          }
          if (count > 2) {
            break;
          }
        }
        printLog("Stop listening");
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
      } else if (my_handler == Handlers.VM_ADDR) {
        getVMAddr(httpExchange, params);
      } else {
        throw new UnsupportedEncodingException("No handler");
      }
    }

    @SuppressWarnings("unchecked")
    public void startVM(HttpExchange httpExchange,
                        Map<String, String> params) throws IOException {
      printLog("[Start VM]");
      Thread script = new Thread(new TaskExecutor("start"));
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
      printLog("[Stop VM]");
      Thread script = new Thread(new TaskExecutor("stop"));
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
      synchronized (status) {
        for (String k : status.keySet()) {
          obj.put(k, status.get(k));
        }
      }

      str.append(obj.toJSONString());
      str.append(");");
      String response = str.toString();

      sendJSONResponse(httpExchange, response.getBytes());
    }

    public void getVMAddr(HttpExchange httpExchange, Map<String, String> params) throws IOException {
      printLog("[Get VM Address]");
      // Build response
      StringBuilder str = new StringBuilder();
      str.append(params.get("callback")).append("("); // Set callback function

      JSONObject obj = new JSONObject();
      synchronized (vmAddr) {
        obj.put("addr", vmAddr);
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
    // Set ipv4 stack only!
    Properties props = System.getProperties();
    props.setProperty("java.net.preferIPv4Stack","true");
    System.setProperties(props);

    // Set status map
    status = new HashMap<String, String>();
    synchronized (status) {
      status.put("virtual_machine", null);
      status.put("gossip", null);
      status.put("in_group", null);
      status.put("master_elected", null);
      status.put("hadoop_started", null);
    }
    vmNumber = Integer.parseInt(args[0]);
    localhost_port = 12000 + vmNumber;
    receive_port = 14000 + vmNumber;

    vmName = "frosty-" + vmNumber;

    Server s = new Server(localhost_port);
    printLog("Server on");
  }
}
