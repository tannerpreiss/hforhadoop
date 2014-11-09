package gossip;

import java.net.SocketException;
import java.net.UnknownHostException;

public class Gossip {

  // Global Variables

  public final static int    PORT_NUM          = 222; // Default port number for requests
  public final static int    GOSSIP_PING       = 10; // Time between gossip pings
  public final static int    GOSSIP_CLEAN      = 1000;  // Time for unresponsive nodes
  public final static int    MULTICAST_PORT    = 6789; // Port for multicast requests
  public final static String MULTICAST_ADDRESS = "228.5.6.7"; // Address for multicast request
  public final static String INTERFACE_NAME    = "en1"; // Interface name
  public final static int    GOSSIP_PORT       = 9999; // Port for gossip requests

  public void main(String args[]) throws InterruptedException, SocketException, UnknownHostException {
    Node node = new Node();
    node.start_listeners(node);
  }
}
