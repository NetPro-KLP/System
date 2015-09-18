package firewallListener;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ThreadPoolDispatcher {
	
    static final String NUMTHREADS = "10";
    static final String THREADPROP = "Threads";

    private int numThreads;

    public ThreadPoolDispatcher() {
        numThreads = Integer.parseInt(System.getProperty(THREADPROP, NUMTHREADS));
    }
    
	public void dispatch(final ServerSocket serverSocket) {
        System.out.println("FirewallListener ThreadPool Size: " + Integer.toString(numThreads));

        for (int i = 0; i < (numThreads - 1); i++) {
            Thread thread = new Thread() {
                public void run() {
                    dispatchLoop(serverSocket);
                }
            };
            thread.start();
        }
        
        dispatchLoop(serverSocket);
	}
	
    private void dispatchLoop(ServerSocket serverSocket) {

    	while( true ) {

			try {
				Socket socket = serverSocket.accept();
				Runnable demultiplexer = new Demultiplexer(socket);
	        	demultiplexer.run();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
    }
}
