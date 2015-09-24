package firewallListener;

import java.util.StringTokenizer;

public class PacketAnalyzer {

    private String packet;
    //private int headerSize;
    private int payloadSize;
    //private String header;
    private String code;
    private String reqRes;
    private String firewall;
    private String contents;

    public PacketAnalyzer (String packet) {
        this.packet = packet;
        StringTokenizer token = new StringTokenizer(packet, "|");

        /*if (token.hasMoreTokens())
            this.headerSize = Integer.parseInt(token.nextToken());*/
        if (token.hasMoreTokens())
            this.payloadSize = Integer.parseInt(token.nextToken());
        /*if (token.hasMoreTokens())
            this.header = token.nextToken();*/
        if (token.hasMoreTokens()) {
            String payload = token.nextToken();

            this.code = payload.substring(0,4);
            this.reqRes = payload.substring(4,8);
            this.firewall = payload.substring(8, 32);
            this.contents = payload.substring(32, payload.length());
        }
    }
/*
    public String getHeader() {
        return header;
    }*/

    public int getPayloadSize() {
      return payloadSize;
    }

    public String getCode() {
        return code;
    }

    public String getReqRes() {
        return reqRes;
    }

    public String getFirewall() {
        return firewall;
    }

    public String getContents() {
        return contents;
    }
}
