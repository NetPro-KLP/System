package webServer;

import java.io.IOException;
import javax.websocket.Session;

public class WebServer {
    private Session serverSession;
    private HandleMap handleMap;
    
    public WebServer(int port) {
    	handleMap = new HandleMap();
        try {
		serverSocket = new ServerSocket(port);
	} catch (IOException e) {
		e.printStackTrace();
	}
    }
    
    public void startServer() {
	Dispatcher dispatcher = new ThreadPerDispatcher();
    	dispatcher.dispatch(serverSession, handleMap);
    }
    
    public void registerHandler(String header, EventHandler handler) {
    	handleMap.put(header, handler);
    }
    
    public void registerHandler(EventHandler handler) {
    	handleMap.put(handler.getHandler(), handler);
    }

    public void removeHandler(EventHandler handler) {
    	handleMap.remove(handler.getHandler());
    }
}
