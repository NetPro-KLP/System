package firewallListener;

import java.net.Socket;

public class QueueListenedInfo {
    private Socket socket;
    private String packet;

    public QueueListenedInfo(Socket socket, String packet) {
        this.socket = socket;
        this.packet = packet;
    }

    public Socket getSocket() {
        return this.socket;
    }

    public String getPacket() {
        return this.packet;
    }
}
