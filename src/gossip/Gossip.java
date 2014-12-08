package gossip;

import logger.Logger;

import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Gossip {

  public static Config config = Config.configure();

  public static void main(String args[]) throws InterruptedException, SocketException, UnknownHostException {
    Logger log = getLogger();
    Node node = new Node(log);
    node.start_listeners(node);
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
