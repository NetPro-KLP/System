package firewallListener;

import java.net.Socket;

public class QueueListenedInfo {
    private Socket socket;
    private String firewallIp;
    private String code;
    private String packet;

    public QueueListenedInfo(Socket socket, long firewallIp, String code,
        String packet) {
        this.socket = socket;
        this.firewallIp = Long.toString(firewallIp);
        this.code = code;
        this.packet = packet;

        System.out.println("QueueListendinfo : " + packet);
    }

    public QueueListenedInfo(Socket socket, long firewallIp, String code) {
      this.socket = socket;
      this.firewallIp = Long.toString(firewallIp);
      this.code = code;

      System.out.println("QueueListenedInfo : " + code);
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
