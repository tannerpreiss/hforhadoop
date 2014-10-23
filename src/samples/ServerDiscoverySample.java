package samples;

import NetworkDiscovery.DiscoveryThread;

/**
 * Created by Rich on 10/22/14.
 */
public class ServerDiscoverySample {

    public static void main (String[] args)
    {
        DiscoveryThread client = new DiscoveryThread();
        client.run();
    }
}
