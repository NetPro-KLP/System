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
        //this.socket = receivedInfo.getSocket();
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

            PacketAnalyzer packetAnalyzer = null;
            byte[] expByte = new byte[4];
            expByte[0] = 101;
            expByte[1] = 120;
            expByte[2] = 112;
            expByte[3] = 0;

            String expString = new String(expByte);

            if(code.equals(expString)) {
              packetAnalyzer = new PacketAnalyzer(code, this.packet);
              eventHandler.expiredEvent(packetAnalyzer);
            }

            switch(code) {
                case "ini":
                    eventHandler.initEvent(socket);
                    break;
                case "alm":
                    packetAnalyzer = new PacketAnalyzer(code, this.packet);
                    eventHandler.alarmEvent(packetAnalyzer);
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
