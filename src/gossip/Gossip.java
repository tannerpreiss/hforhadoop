package gossip;

import logger.Logger;

import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Gossip {

  public static Config config = new Config();

  public static void main(String args[]) throws InterruptedException, SocketException, UnknownHostException {
    initConfig();
    Logger log = getLogger();
    Node node = new Node(log);
    node.start_listeners(node);
  }

  public static void initConfig() {
    config.PORT_NUM          = 222; // Default port number for requests
    config.GOSSIP_PING       = 5000; // Time between gossip pings
    config.GOSSIP_CLEAN      = 40000;  // Time for unresponsive nodes
    config.MULTICAST_PORT    = 6789; // Port for multicast requests
    config.MULTICAST_ADDRESS = "228.5.6.7"; // Address for multicast request
    config.INTERFACE_NAME    = "en1"; // Interface name
    config.GOSSIP_PORT       = 9999; // Port for gossip requests
    config.PACKET_SIZE       = 1024 * 4; // Size of buffer for receiving packets
  }

  public static Logger getLogger() {
    Scanner in = new Scanner(System.in);
    boolean printConsole = true; // Default to true
    boolean loggerServer = false; // Default to false
    int logServerPort = 0;
    System.out.println("Print to console? (Y | N)");
    String input = in.next();
    if (input.equalsIgnoreCase("n")) { printConsole = false; }
    System.out.println("Start log server? (Y | N)");
    input = in.next();
    if (input.equalsIgnoreCase("y")) { loggerServer = true; }
    if (loggerServer) {
      System.out.println("Port number for log server:");
      logServerPort = in.nextInt();
    }
    if (logServerPort == 0) { logServerPort = 8001; }
    return new Logger(printConsole, loggerServer, logServerPort);
  }

  public static String getAddress(DatagramPacket packet) {
    return packet.getAddress().toString().substring(1) + ":" + config.GOSSIP_PORT;
  }
}
