package firewallListener;

import java.io.IOException;
import java.net.Socket;

public class Demultiplexer implements Runnable {

    private Socket socket;
    private String packet;
	
	public Demultiplexer(QueueListenedInfo receivedInfo) {
        this.socket = receivedInfo.getSocket();
        this.packet = receivedInfo.getPacket();
	}
	
	public void run() {

        try {
            PacketAnalyzer packetAnalyzer = new PacketAnalyzer(this.packet);

            //String header = packetAnalyzer.getHeader();
            int payloadSize = packetAnalyzer.getPayloadSize();
            String code = packetAnalyzer.getCode();
            String reqRes = packetAnalyzer.getReqRes();
            String firewall = packetAnalyzer.getFirewall();
            String contents = packetAnalyzer.getContents();

            //System.out.println(header);
            System.out.println("payloadSize: " + Integer.toString(payloadSize));
            System.out.println("code: " + code);
            System.out.println("req/res: " + reqRes);
            System.out.println("firewall: " + firewall);
            System.out.println("contents: " + contents);

            EventHandler eventHandler = new EventHandler("localhost",
                "root", "klpsoma123");

            switch(code) {
                case "NULL":
                    eventHandler.nopeEvent();
                    break;
                case "pack":
                    eventHandler.expiredEvent();
                    break;
                case "init":
                    eventHandler.initEvent();
                default:
                    break;
            }

            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
}
