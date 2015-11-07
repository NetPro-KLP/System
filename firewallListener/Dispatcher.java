package firewallListener;

import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.Queue;
import java.util.LinkedList;

public class Dispatcher {
	
    static final String QUEUENUMTHREADS = "3";
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
        EventHandler eventHandler = new EventHandler("localhost", "root", 
            "klpsoma123");

        eventHandler.checkGeoipBlacklist();
        eventHandler.checkPacketTable();

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
                byte swapByte;
                byte[] expByte = new byte[4];
                byte[] iniByte = new byte[4];

                expByte[0] = 101;
                expByte[1] = 120;
                expByte[2] = 112;
                expByte[3] = 0;

                String expString = new String(expByte);

                System.out.println("FUCKU");

                byte[] firewallIpByte = new byte[4];
                dataInputStream.read(firewallIpByte, 0, firewallIpByte.length);
                long firewallIp = addrToLong(firewallIpByte);

                byte[] rowNumByte = new byte[4];
                dataInputStream.read(rowNumByte, 0, rowNumByte.length);
                swapByte = rowNumByte[0];
                rowNumByte[0] = rowNumByte[3];
                rowNumByte[3] = swapByte;
                swapByte = rowNumByte[1];
                rowNumByte[1] = rowNumByte[2];
                rowNumByte[2] = swapByte;
                int rowNum = byteToInt(rowNumByte);

                byte[] codeBuffer = new byte[4];
                dataInputStream.read(codeBuffer, 0, codeBuffer.length);
                String code = new String(codeBuffer);
                System.out.println("code: " + code);

                if (code.equals("ini")) {
                  QueueListenedInfo queueListenedInfo = new QueueListenedInfo(
                      socket, firewallIp, code);
                  queue.offer(queueListenedInfo);
                } else if (code.equals(expString) || code.equals("alm")) {
                  byte[] saddrByte = new byte[4];
                  dataInputStream.read(saddrByte, 0, saddrByte.length);
                  long saddr = addrToLong(saddrByte);

                  System.out.println("saddrByte");
                  System.out.println(saddrByte[0] & 0xff);
                  System.out.println(saddrByte[1] & 0xff);
                  System.out.println(saddrByte[2] & 0xff);
                  System.out.println(saddrByte[3] & 0xff);

                  byte[] srcByte = new byte[2];
                  dataInputStream.read(srcByte, 0, srcByte.length);
                  String src = portToString(srcByte);

                  System.out.println("srcByte");
                  System.out.println(srcByte[0] & 0xff);
                  System.out.println(srcByte[1] & 0xff);

                  byte[] daddrByte = new byte[4];
                  dataInputStream.read(daddrByte, 0, daddrByte.length);
                  long daddr = addrToLong(daddrByte);

                  System.out.println("daddrByte");
                  System.out.println(daddrByte[0] & 0xff);
                  System.out.println(daddrByte[1] & 0xff);
                  System.out.println(daddrByte[2] & 0xff);
                  System.out.println(daddrByte[3] & 0xff);

                  byte[] dstByte = new byte[2];
                  dataInputStream.read(dstByte, 0, dstByte.length);
                  String dst = portToString(dstByte);

                  System.out.println("dstByte");
                  System.out.println(dstByte[0] & 0xff);
                  System.out.println(dstByte[1] & 0xff);

                  byte[] tcpudpByte = new byte[1];
                  dataInputStream.read(tcpudpByte, 0, tcpudpByte.length);
                  int tcpudp = tcpudpByte[0] & 0xff;

                  System.out.println("tcpudpByte");
                  System.out.println(tcpudpByte[0] & 0xff);

                  byte[] warnByte = new byte[4];
                  dataInputStream.read(warnByte, 0, warnByte.length);
                  int warn = byteToInt(warnByte);

                  System.out.println("warnByte");
                  System.out.println(warnByte[0] & 0xff);
                  System.out.println(warnByte[1] & 0xff);
                  System.out.println(warnByte[2] & 0xff);
                  System.out.println(warnByte[3] & 0xff);

                  byte[] dangerByte = new byte[4];
                  dataInputStream.read(dangerByte, 0, dangerByte.length);
                  int danger = byteToInt(dangerByte);

                  System.out.println("dangerByte");
                  System.out.println(dangerByte[0] & 0xff);
                  System.out.println(dangerByte[1] & 0xff);
                  System.out.println(dangerByte[2] & 0xff);
                  System.out.println(dangerByte[3] & 0xff);

                  byte[] packetCountByte = new byte[4];
                  dataInputStream.read(packetCountByte, 0, packetCountByte.length);
                  int packetCount = byteToInt(packetCountByte);

                  System.out.println("packetCountByte");
                  System.out.println(packetCountByte[0] & 0xff);
                  System.out.println(packetCountByte[1] & 0xff);
                  System.out.println(packetCountByte[2] & 0xff);
                  System.out.println(packetCountByte[3] & 0xff);

                  byte[] totalbytesByte = new byte[4];
                  dataInputStream.read(totalbytesByte, 0,
                      totalbytesByte.length);
                  int totalbytes = byteToInt(totalbytesByte);

                  System.out.println("totalbytesByte");
                  System.out.println(totalbytesByte[0] & 0xff);
                  System.out.println(totalbytesByte[1] & 0xff);
                  System.out.println(totalbytesByte[2] & 0xff);
                  System.out.println(totalbytesByte[3] & 0xff);

                  byte[] starttimeByte = new byte[19];
                  dataInputStream.read(starttimeByte, 0, starttimeByte.length);
                  String starttime = new String(starttimeByte);

                  System.out.println("starttimeByte");
                  System.out.println(starttimeByte[0] & 0xff);
                  System.out.println(starttimeByte[1] & 0xff);
                  System.out.println(starttimeByte[2] & 0xff);
                  System.out.println(starttimeByte[3] & 0xff);

                  byte[] endtimeByte = new byte[19];
                  dataInputStream.read(endtimeByte, 0, endtimeByte.length);
                  String endtime = new String(endtimeByte);

                  System.out.println("endtimeByte");
                  System.out.println(endtimeByte[0] & 0xff);
                  System.out.println(endtimeByte[1] & 0xff);
                  System.out.println(endtimeByte[2] & 0xff);
                  System.out.println(endtimeByte[3] & 0xff);

                  QueueListenedInfo queueListenedInfo = null;
                  String packet = saddr + "|" + src + "|" + daddr
                        + "|" + dst + "|" + tcpudp + "|" + warn
                        + "|" + danger + "|" + packetCount + "|" + totalbytes
                        + "|" + starttime + "|" + endtime;

                  System.out.println("packet: " + packet);
                  if (code.equals("exp")) {
                    queueListenedInfo = new QueueListenedInfo (
                        socket, firewallIp, code, rowNum + "|" + packet);
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
                        socket, firewallIp, code, packet + "|" + name + "|" +
                        hazard + "|" + payload + "|" + createdAt);
                  }

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

    private long addrToLong (byte[] arr) {
      long addrLong1 = arr[0] & 0xff;
      long addrLong2 = arr[1] & 0xff;
      long addrLong3 = arr[2] & 0xff;
      long addrLong4 = arr[3] & 0xff;

      long addrLong = (addrLong1 << 24) + (addrLong2 << 16) + (addrLong3 << 8) +
        addrLong4;

      return addrLong;
    }

    private String portToString (byte[] arr) {
      int port = (arr[1] & 0xff) << 8 | (arr[0] & 0xff);

      return Integer.toString(port);
    }
}
