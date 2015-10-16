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
                //int payloadSize = dataInputStream.readInt();

                //InputStream inputStream = socket.getInputStream();

                byte[] firewallIpByte = new byte[4];
                dataInputStream.read(firewallIpByte, 0, firewallIpByte.length);
                String firewallIp = addrToString(firewallIpByte);

                byte[] rowNumByte = new byte[4];
                dataInputStream.read(rowNumByte, 0, rowNumByte.length);
                int rowNum = byteToInt(rowNumByte);

                //byte[] headerBuffer = new byte[headerSize];
                //inputStream.read(headerBuffer);
                //String header = new String(headerBuffer);

                //inputStream = socket.getInputStream();

                byte[] codeBuffer = new byte[4];
                dataInputStream.read(codeBuffer, 0, codeBuffer.length);
                String code = new String(codeBuffer);

                if (code.equals("ini")) {
                  QueueListenedInfo queueListenedInfo = new QueueListenedInfo(
                      socket, firewallIp, code);
                  queue.offer(queueListenedInfo);
                } else {
                  byte[] saddrByte = new byte[4];
                  dataInputStream.read(saddrByte, 0, saddrByte.length);
                  String saddr = addrToString(saddrByte);

                  byte[] srcByte = new byte[2];
                  dataInputStream.read(srcByte, 0, srcByte.length);
                  String src = portToString(srcByte);

                  byte[] daddrByte = new byte[4];
                  dataInputStream.read(daddrByte, 0, daddrByte.length);
                  String daddr = addrToString(daddrByte);

                  byte[] dstByte = new byte[2];
                  dataInputStream.read(dstByte, 0, dstByte.length);
                  String dst = portToString(dstByte);

                  byte[] protocolByte = new byte[2];
                  dataInputStream.read(protocolByte, 0, protocolByte.length);
                  String protocol = portToString(protocolByte);

                  byte[] tcpudpByte = new byte[1];
                  dataInputStream.read(tcpudpByte, 0, tcpudpByte.length);
                  int tcpudp = tcpudpByte[0] & 0xff;

                  byte[] warnByte = new byte[4];
                  dataInputStream.read(warnByte, 0, warnByte.length);
                  int warn = byteToInt(warnByte);

                  byte[] dangerByte = new byte[4];
                  dataInputStream.read(dangerByte, 0, dangerByte.length);
                  int danger = byteToInt(dangerByte);

                  byte[] packetCountByte = new byte[4];
                  dataInputStream.read(packetCountByte, 0, packetCountByte.length);
                  int packetCount = byteToInt(packetCountByte);

                  byte[] totalbytesByte = new byte[4];
                  dataInputStream.read(totalbytesByte, 0,
                      totalbytesByte.length);
                  int totalbytes = byteToInt(totalbytesByte);

                  QueueListenedInfo queueListenedInfo = new QueueListenedInfo (
                      socket, firewallIp, code, rowNum + "|" + saddr + "|" + src + "|" + daddr
                      + "|" + dst + "|" + protocol + "|" + tcpudp + "|" + warn
                      + "|" + danger + "|" + packetCount + "|" + totalbytes);
                  queue.offer(queueListenedInfo);
                }

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

    private String addrToString (byte[] arr) {
      int addrInt1 = arr[0] & 0xff;
      int addrInt2 = arr[1] & 0xff;
      int addrInt3 = arr[2] & 0xff;
      int addrInt4 = arr[3] & 0xff;

      return Integer.toString(addrInt1) + "." + Integer.toString(addrInt2) +
        "." + Integer.toString(addrInt3) + "." + Integer.toString(addrInt4);
    }

    private String portToString (byte[] arr) {
      int port = (arr[1] & 0xff) << 8 | (arr[0] & 0xff);

      return Integer.toString(port);
    }
}
