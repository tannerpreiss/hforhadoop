package NetworkDiscovery;

/**
 * Created by Rich on 10/22/14.
 */
public class DiscoveryClient {

    public DiscoveryClient() {
        // do nothing
    }

    public void start() {
        Thread discoveryThread = new Thread(DiscoveryThread.getInstance());
        discoveryThread.start();
    }
}
