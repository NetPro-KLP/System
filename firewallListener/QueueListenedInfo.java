package firewallListener;

import java.net.Socket;

public class QueueListenedInfo {
    private Socket socket;
    private String firewallIp;
    private String code;
    private String packet;

    public QueueListenedInfo(Socket socket, String firewallIp, String code,
        String packet) {
        this.socket = socket;
        this.firewallIp = firewallIp;
        this.code = code;
        this.packet = packet;
    }

    public QueueListenedInfo(Socket socket, String firewallIp, String code) {
      this.socket = socket;
      this.firewallIp = firewallIp;
      this.code = code;
    }

    public Socket getSocket() {
        return this.socket;
    }

    public String getFirewallIp() {
      return this.firewallIp;
    }

    public String getCode() {
      return this.code;
    }

    public String getPacket() {
        return this.packet;
    }
}
