package firewallListener;

import java.io.IOException;
import java.net.Socket;

public class EventDemultiplexer implements Runnable {

    private String packet;
	
	public EventDemultiplexer(String packet) {
        this.packet = packet;
	}
	
	public void run() {

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
	}
}