package gossip;

/**
 * Created by elmer on 11/22/14.
 */
public class Config {
  public int    PORT_NUM; // Default port number for requests
  public int    GOSSIP_PING; // Time between gossip pings
  public int    GOSSIP_CLEAN;  // Time for unresponsive nodes
  public int    MULTICAST_PORT; // Port for multicast requests
  public String MULTICAST_ADDRESS; // Address for multicast request
  public int    MULTICAST_WAIT;
  public String INTERFACE_NAME; // Interface name
  public int    GOSSIP_PORT; // Port for gossip requests
  public int    PACKET_SIZE; // Size of buffer for receiving packets
  public int    NODE_THRESHOLD; // Number of nodes before election
}
