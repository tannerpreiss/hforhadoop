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
  public String MULTICAST_INTERFACE_NAME;
  public int    GOSSIP_PORT; // Port for gossip requests
  public int    PACKET_SIZE; // Size of buffer for receiving packets
  public int    NODE_THRESHOLD; // Number of nodes before election
  public int    PING_PORT; // Port for host machine to ping VM
  public boolean PRINT_DEBUG;

  public static Config configure() {
    Config config = new Config();
    config.PORT_NUM          = 222;
    config.GOSSIP_PING       = 1000;
    config.GOSSIP_CLEAN      = 40000;
    config.MULTICAST_PORT    = 6789;
    config.MULTICAST_ADDRESS = "228.5.6.7";
    config.MULTICAST_WAIT    = 5000;
    config.INTERFACE_NAME    = "eth1";
//    config.INTERFACE_NAME    = "en0";
    config.MULTICAST_INTERFACE_NAME    = "eth1";
//    config.MULTICAST_INTERFACE_NAME    = "en0";
    config.GOSSIP_PORT       = 16000;
    config.PACKET_SIZE       = 1024 * 4;
    config.NODE_THRESHOLD    = 3;
    config.PING_PORT         = 9090;
    config.PRINT_DEBUG = false;
    return config;
  }
}
