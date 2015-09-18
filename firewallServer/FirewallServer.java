package firewallServer;

import java.io.IOException;
import java.net.ServerSocket;

public class FirewallServer {
    private ServerSocket serverSocket;
    
    public FirewallServer(int port) {
        try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public void startServer() {
    	
		Dispatcher dispatcher = new ThreadPerDispatcher();
    	dispatcher.dispatch(serverSocket);
    	
    }
}
