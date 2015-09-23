package firewallListener;

import java.io.IOException;
import java.net.Socket;

public class EventDemultiplexer implements Runnable {

    private Socket socket;
    private String packet;
	
	public EventDemultiplexer(QueueListenedInfo receivedInfo) {
        this.socket = receivedInfo.getSocket();
        this.packet = receivedInfo.getPacket();
	}
	
	public void run() {

        try {
            PacketAnalyzer packetAnalyzer = new PacketAnalyzer(this.packet);

            String header = packetAnalyzer.getHeader();
            String code = packetAnalyzer.getCode();
            String reqRes = packetAnalyzer.getReqRes();
            String firewall = packetAnalyzer.getFirewall();
            String contents = packetAnalyzer.getContents();

            System.out.println(header);
            System.out.println(code);
            System.out.println(reqRes);
            System.out.println(firewall);
            System.out.println(contents);

            EventHandler eventHandler = new EventHandler();

            switch(header) {
                case "NULL":
                    eventHandler.NopeEvent();
                    break;
                default:
                    break;
            }

            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
}
