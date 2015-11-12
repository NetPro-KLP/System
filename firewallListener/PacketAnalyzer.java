package firewallListener;

import java.util.StringTokenizer;

public class PacketAnalyzer {

    private int rowNum;
    private String saddr;
    private String src;
    private String daddr;
    private String dst;

    private String tcpudp;
    private String warn;
    private String danger;
    private String packetCount;
    private String totalbytes;
    private String starttime;
    private String endtime;

    private String name;
    private String hazard;
    private String payload;
    private String createdAt;

    public PacketAnalyzer (String code, String packet) {
        StringTokenizer token = new StringTokenizer(packet, "|");

        if (code.equals("exp") || code.equals("alm")) {
          if (token.hasMoreTokens())
              this.saddr = token.nextToken();
          if (token.hasMoreTokens())
              this.src = token.nextToken();
          if (token.hasMoreTokens())
              this.daddr = token.nextToken();
          if (token.hasMoreTokens())
              this.dst = token.nextToken();
          if (token.hasMoreTokens())
              this.tcpudp = token.nextToken();
          if (token.hasMoreTokens())
              this.warn = token.nextToken();
          if (token.hasMoreTokens())
              this.danger = token.nextToken();
          if (token.hasMoreTokens())
              this.packetCount = token.nextToken();
          if (token.hasMoreTokens())
              this.totalbytes = token.nextToken();
          if (token.hasMoreTokens())
              this.starttime = token.nextToken();
          if (token.hasMoreTokens())
              this.endtime = token.nextToken();

          if (code.equals("alm")) {
            if (token.hasMoreTokens())
                token.nextToken();
            if (token.hasMoreTokens())
                this.name = token.nextToken();
            if (token.hasMoreTokens())
                this.hazard = token.nextToken();
            if (token.hasMoreTokens())
                this.payload = token.nextToken();
            if (token.hasMoreTokens())
                this.createdAt = token.nextToken();
          }
        }
    }

    public int getRowNum() {
      return rowNum;
    }

    public String getSaddr() {
        return saddr;
    }

    public String getSrc() {
        return src;
    }

    public String getDaddr() {
        return daddr;
    }

    public String getDst() {
        return dst;
    }

    public String getTcpudp() {
        return tcpudp;
    }

    public String getWarn() {
        return warn;
    }

    public String getDanger() {
        return danger;
    }

    public String getPacketCount() {
        return packetCount;
    }

    public String getTotalbytes() {
        return totalbytes;
    }

    public String getStarttime() {
      return starttime;
    }

    public String getEndtime() {
      return endtime;
    }

    public String getName() {
      return name;
    }

    public String getHazard() {
      return hazard;
    }

    public String getPayload() {
      return payload;
    }

    public String getCreatedAt() {
      return createdAt;
    }
}
