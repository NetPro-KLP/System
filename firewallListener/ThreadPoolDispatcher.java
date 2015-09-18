package firewallListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.Queue;
import java.util.LinkedList;

public class ThreadPoolDispatcher {
	
    static final String QUEUENUMTHREADS = "10";
    static final String DISPATCHNUMTHREADS = "15";
    static final String THREADPROP = "Threads";

    private final int HEADER_SIZE = 4;
    private int queueNumThreads;
    private int dispatchNumThreads;

    private Queue<String> queue = new LinkedList<String>();

    public ThreadPoolDispatcher() {
        queueNumThreads = Integer.parseInt(System.getProperty(THREADPROP, QUEUENUMTHREADS));
        dispatchNumThreads = Integer.parseInt(System.getProperty(THREADPROP, DISPATCHNUMTHREADS));
    }
    
	public void dispatch(final ServerSocket serverSocket) {
        System.out.println("Firewall QueueListener ThreadPool Size: " + Integer.toString(queueNumThreads));

        for (int i = 0; i < queueNumThreads; i++) {
            Thread thread = new Thread() {
                public void run() {
                    QueueListener(serverSocket);
                }
            };
            thread.start();
        }

        System.out.println("Firewall dispatchLoop ThreadPool Size: " + Integer.toString(dispatchNumThreads));

        for (int i = 0; i < (dispatchNumThreads - 1); i++) {
            Thread thread = new Thread() {
                public void run() {
                    dispatchLoop();
                }
            };
            thread.start();
        }

        dispatchLoop();
	}
	
    private void QueueListener(ServerSocket serverSocket) {
        while( true ) {

            try {
                Socket socket = serverSocket.accept();
                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                int headerSize = dataInputStream.readInt();

                InputStream inputStream = socket.getInputStream();

                byte[] headerBuffer = new byte[headerSize];
                inputStream.read(headerBuffer);
                String header = new String(headerBuffer);

                dataInputStream = new DataInputStream(socket.getInputStream());
                int payloadSize = dataInputStream.readInt();

                inputStream =socket.getInputStream();

                byte[] payloadBuffer = new byte[payloadSize];
                inputStream.read(payloadBuffer);
                String payload = new String(payloadBuffer);

                queue.offer(headerSize + header + payloadSize + payload);

                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void dispatchLoop() {
/*
    	while( true ) {

			try {
				Runnable demultiplexer = new Demultiplexer(socket);
	        	demultiplexer.run();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
    */
    }
}
