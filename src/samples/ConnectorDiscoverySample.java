package samples;

import NetworkDiscovery.ConnectorThread;

/**
 * Created by Rich on 10/22/14.
 */
public class ConnectorDiscoverySample {

    public static void main (String[] args)
    {
        ConnectorThread client = new ConnectorThread();
        client.run();
    }
}