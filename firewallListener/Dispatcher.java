package firewallListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.Queue;
import java.util.LinkedList;

public class Dispatcher {
	
    static final String QUEUENUMTHREADS = "2";
    static final String DISPATCHNUMTHREADS = "1";
    static final String THREADPROP = "Threads";

    // 패킷디스패치 루프의 슬립 타입을 'ms' 단위로 지정
    private static final int PACKETDISPATCHLOOP_SLEEPTIME = 30;
    private final int HEADER_SIZE = 4;
    private int queueNumThreads;
    private int dispatchNumThreads;

    private Queue<QueueListenedInfo> queue = new LinkedList<QueueListenedInfo>();

    public Dispatcher() {
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
                //int headerSize = dataInputStream.readInt();

                //dataInputStream = new DataInputStream(socket.getInputStream());
                int payloadSize = dataInputStream.readInt();

                InputStream inputStream = socket.getInputStream();

                //byte[] headerBuffer = new byte[headerSize];
                //inputStream.read(headerBuffer);
                //String header = new String(headerBuffer);

                //inputStream = socket.getInputStream();

                byte[] payloadBuffer = new byte[payloadSize];
                inputStream.read(payloadBuffer);
                String payload = new String(payloadBuffer);

                QueueListenedInfo queueListenedInfo = new QueueListenedInfo(socket, /*headerSize + "|" +*/ payloadSize + "|"/* + header + "|"*/ + payload);
                queue.offer(queueListenedInfo);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void packetDispatchLoop() {

    	while( true ) {

            if (queue.peek() != null) {
                QueueListenedInfo receivedInfo = queue.poll();

                if (receivedInfo != null) {
                    Thread thread = new Thread() {
                        public void run() {
                            Runnable demultiplexer = new Demultiplexer(receivedInfo);
                            demultiplexer.run();
                        }
                    };

                    thread.start();
                }
            }
            else {
                try {
                    Thread.sleep(PACKETDISPATCHLOOP_SLEEPTIME);
                } catch(InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
