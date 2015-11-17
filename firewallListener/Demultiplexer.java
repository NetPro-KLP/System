package firewallListener;

import java.io.IOException;
import java.io.OutputStream;

import java.net.Socket;

public class Demultiplexer implements Runnable {

    private Socket socket;
    private String firewallIp;
    private String code;
    private String packet;
	
    public Demultiplexer(QueueListenedInfo receivedInfo) {
        this.firewallIp = receivedInfo.getFirewallIp();
        this.code = receivedInfo.getCode();
        if (this.code.equals("exp")) {
          this.packet = receivedInfo.getPacket();
        } else
          this.socket = receivedInfo.getSocket();
    }
	
    public void run() {

        EventHandler eventHandler = new EventHandler("localhost",
            "root", "klpsoma123");

        PacketAnalyzer packetAnalyzer = null;

        switch(code) {
            case "ini":
                eventHandler.initEvent(socket);
                break;
            case "exp":
                packetAnalyzer = new PacketAnalyzer(code, this.packet);
                eventHandler.expiredEvent(packetAnalyzer);
                break;
            case "alm":
                packetAnalyzer = new PacketAnalyzer(code, this.packet);
                eventHandler.alarmEvent(packetAnalyzer);
                break;
            default:
                break;
        }
    }
}
