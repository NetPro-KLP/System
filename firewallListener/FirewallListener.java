package firewallListener;

import java.io.IOException;
import java.net.ServerSocket;

public class FirewallListener {
    private ServerSocket serverSocket;
    
    public FirewallListener(int port) {
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
