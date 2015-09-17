package firewallServer;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;

public class FirewallServer {
    private int port = 30000;
    private static int threadPoolSize = 10;
    private static int initialSize = 5;

    private NioHandleMap handleMap;
}
