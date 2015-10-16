package firewallListener;

import java.io.IOException;
import java.net.Socket;

public class Demultiplexer implements Runnable {

    private Socket socket;
    private String firewallIp;
    private String code;
    private String packet;
	
	public Demultiplexer(QueueListenedInfo receivedInfo) {
        this.socket = receivedInfo.getSocket();
        this.firewallIp = receivedInfo.getFirewallIp();
        this.code = receivedInfo.getCode();
        if (this.code.equals("exp")) {
          this.packet = receivedInfo.getPacket();
        }
	}
	
	public void run() {

        try {
            EventHandler eventHandler = new EventHandler("localhost",
                "root", "klpsoma123");

            switch(code) {
                case "ini":
                    eventHandler.initEvent(socket);
                    break;
                case "exp":
                    PacketAnalyzer packetAnalyzer = new PacketAnalyzer(this.packet);
                    eventHandler.expiredEvent(packetAnalyzer);
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
