package gossip;

import logger.Logger;

import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Gossip {

  public static Config config = Config.configure();

  public static void main(String args[]) throws InterruptedException, SocketException, UnknownHostException {
    System.out.println("Size: " + args.length);
    System.out.println(args[0] + " " + args[1] + " " + args[2]);
    Logger log = getLogger(args);
    Node node = new Node(log);
    node.start_listeners(node);
  }

  public static Logger getLogger(String args[]) {
    boolean printConsole = true; // Default to true
    boolean loggerServer = true; // Default to false
    if (args[0].equalsIgnoreCase("n")) { printConsole = false; }
    if (args[1].equalsIgnoreCase("n")) { loggerServer = false; }
    int logServerPort = Integer.parseInt(args[2]);
    if (logServerPort == 0) { logServerPort = 8001; }

    return new Logger(printConsole, loggerServer, logServerPort);
  }

  public static String getAddress(DatagramPacket packet) {
    return packet.getAddress().toString().substring(1) + ":" + config.GOSSIP_PORT;
  }
}
