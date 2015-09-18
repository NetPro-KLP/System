package firewallListener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ThreadPerDispatcher {
	
	public void dispatch(ServerSocket serverSocket) {
		while( true ) {
			try {
				Socket socket = serverSocket.accept();
				
				Runnable demultiplexer = new Demultiplexer(socket);
	            Thread thread = new Thread(demultiplexer);
	            thread.start();
	            
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
