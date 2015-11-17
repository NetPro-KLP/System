package firewallListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;

import java.net.ServerSocket;
import java.net.Socket;

import java.util.Queue;
import java.util.LinkedList;
import java.util.StringTokenizer;

public class Dispatcher {
	
    static final String QUEUENUMTHREADS = "1";
    static final String DISPATCHNUMTHREADS = "1";
    static final String THREADPROP = "Threads";

    // 패킷디스패치 루프의 슬립 타입을 'ms' 단위로 지정
    private static final int PACKETDISPATCHLOOP_SLEEPTIME = 30;
    private static final int HEADER_SIZE = 50;
    private static final int PAYLOAD_SIZE = 120;
    private static final int HEADER_TOKEN_NUM = 4;
    private static final int PAYLOAD_TOKEN_NUM = 13;
    private int queueNumThreads;
    private int dispatchNumThreads;

    private Queue<QueueListenedInfo> queue = new LinkedList<QueueListenedInfo>();

    public Dispatcher() {
        queueNumThreads = Integer.parseInt(System.getProperty(THREADPROP, QUEUENUMTHREADS));
        dispatchNumThreads = Integer.parseInt(System.getProperty(THREADPROP, DISPATCHNUMTHREADS));
    }
    
    public void dispatch(final ServerSocket serverSocket) {
        EventHandler eventHandler = new EventHandler("localhost", "root", 
            "klpsoma123");

        eventHandler.checkGeoipBlacklist();
        eventHandler.checkPacketTable();
        eventHandler.checkBanUser();

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
                byte[] buffer = new byte[HEADER_SIZE];
                dataInputStream.read(buffer);
                String data = new String(buffer);

                String[] params = new String[HEADER_TOKEN_NUM];
                StringTokenizer token = new StringTokenizer(data, "|");
                
                int i = 0;

                while (token.hasMoreTokens() && i < 4) {
                  params[i++] = token.nextToken();
                }

                System.out.println("received header: " + data);

                long firewallIp = Long.parseLong(params[0]);
                int rowNum = Integer.parseInt(params[1]);
                String code = params[2];

                for (i = 0; i < rowNum; i++) {
                  if (code.equals("ini")) {
                    QueueListenedInfo queueListenedInfo = new QueueListenedInfo(
                        socket, firewallIp, code);
                    queue.offer(queueListenedInfo);
                  } else if (code.equals("exp") || code.equals("alm")) {
                    buffer = new byte[PAYLOAD_SIZE];
                    dataInputStream.read(buffer);
                    data = new String(buffer);

                    System.out.println("payload: " + data);

                    QueueListenedInfo queueListenedInfo = null;

                    if (code.equals("exp")) {
                      queueListenedInfo = new QueueListenedInfo (
                          firewallIp, code, data);
                      queue.offer(queueListenedInfo);
                    } else {
                      byte[] nameByte = new byte[100];
                      dataInputStream.read(nameByte, 0, nameByte.length);
                      String name = new String(nameByte);

                      byte[] hazardByte = new byte[4];
                      dataInputStream.read(hazardByte, 0, hazardByte.length);
                      int hazard = byteToInt(hazardByte);

                      byte[] payloadByte = new byte[1000];
                      dataInputStream.read(payloadByte, 0, payloadByte.length);
                      String payload = new String(payloadByte);

                      byte[] createdAtByte = new byte[19];
                      dataInputStream.read(createdAtByte, 0, createdAtByte.length);
                      String createdAt = new String(createdAtByte);

                      queueListenedInfo = new QueueListenedInfo (
                          socket, firewallIp, code, data + "|" + name + "|" +
                          hazard + "|" + payload + "|" + createdAt);
                    }
                  }
                }
                
                if (code.equals("exp"))
                  socket.close();

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

    private int byteToInt (byte[] arr) {
      return (arr[0] & 0xff) << 24 | (arr[1] & 0xff) << 16 |
             (arr[2] & 0xff) << 8 | (arr[3] & 0xff);
    }

    private long addrToLong (byte[] arr) {
      long addrLong1 = arr[0] & 0xff;
      long addrLong2 = arr[1] & 0xff;
      long addrLong3 = arr[2] & 0xff;
      long addrLong4 = arr[3] & 0xff;

      long addrLong = (addrLong1 << 24) | (addrLong2 << 16) | (addrLong3 << 8) |
        addrLong4;

      return addrLong;
    }

    private String portToString (byte[] arr) {
      int port = (arr[1] & 0xff) << 8 | (arr[0] & 0xff);

      return Integer.toString(port);
    }
}
