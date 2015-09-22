package firewallListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.Queue;
import java.util.LinkedList;

public class ThreadPoolDispatcher {
	
    static final String QUEUENUMTHREADS = "1";
    static final String DISPATCHNUMTHREADS = "1";
    static final String THREADPROP = "Threads";

    // 패킷디스패치 루프의 슬립 타입을 '초' 단위로 지정
    private static final double PACKETDISPATCHLOOP_SLEEPTIME = 0.3;
    private final int HEADER_SIZE = 4;
    private int queueNumThreads;
    private int dispatchNumThreads;

    private Queue<String> queue = new LinkedList<String>();
    private int Semaphore = 1;

    public ThreadPoolDispatcher() {
        queueNumThreads = Integer.parseInt(System.getProperty(THREADPROP, QUEUENUMTHREADS));
        dispatchNumThreads = Integer.parseInt(System.getProperty(THREADPROP, DISPATCHNUMTHREADS));
    }
    
	public void dispatch(final ServerSocket serverSocket) {
        System.out.println("Firewall QueueListener ThreadPool Size: " + Integer.toString(queueNumThreads));

        for (int i = 0; i < queueNumThreads; i++) {
            Thread thread = new Thread() {
                public void run() {
                    queueListenerLoop(serverSocket);
                }
            };
            thread.start();
        }

        System.out.println("Firewall dispatchLoop ThreadPool Size: " + Integer.toString(dispatchNumThreads));

        for (int i = 0; i < (dispatchNumThreads - 1); i++) {
            Thread thread = new Thread() {
                public void run() {
                    packetDispatchLoop();
                }
            };
            thread.start();
        }

        packetDispatchLoop();
	}
	
    private void queueListenerLoop(ServerSocket serverSocket) {
        while( true ) {

            try {
                Socket socket = serverSocket.accept();

                DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
                int headerSize = dataInputStream.readInt();

                dataInputStream = new DataInputStream(socket.getInputStream());
                int payloadSize = dataInputStream.readInt();

                InputStream inputStream = socket.getInputStream();

                byte[] headerBuffer = new byte[headerSize];
                inputStream.read(headerBuffer);
                String header = new String(headerBuffer);

                inputStream = socket.getInputStream();

                byte[] payloadBuffer = new byte[payloadSize];
                inputStream.read(payloadBuffer);
                String payload = new String(payloadBuffer);

                queue.offer(headerSize + "|" + payloadSize + "|" + header + "|" + payload);

                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void packetDispatchLoop() {

    	while( true ) {

            if (queue.peek() != null) {
                String packet = queue.poll();

                if (packet != null) {
				    Runnable eventDemultiplexer = new EventDemultiplexer(packet);
	        	    eventDemultiplexer.run();
                }
            }
            else {
                try {
                    Thread.sleep((long)(PACKETDISPATCHLOOP_SLEEPTIME*1000));
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
