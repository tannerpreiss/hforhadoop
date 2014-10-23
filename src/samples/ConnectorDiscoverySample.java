package samples;

import NetworkDiscovery.ConnectorClient;

/**
 * Created by Rich on 10/22/14.
 */
public class ConnectorDiscoverySample {

    public static void main (String[] args)
    {
        ConnectorClient client = new ConnectorClient();
        client.start();
    }
}
