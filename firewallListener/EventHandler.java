package firewallListener;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.net.Socket;

import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.nio.ByteBuffer;

public class EventHandler {

    private Connection firewallConn = null;
    private boolean isConnected;

    public EventHandler(String host, String userId, String pw) {

      this.isConnected = false;

      while (!this.isConnected) {
        try {
          this.firewallConn = DriverManager.getConnection("jdbc:mysql://"
              + host + "/KLP-Firewall", userId, pw);
          this.isConnected = true;
        } catch (SQLException sqex) {
          System.out.println("SQLException: " + sqex.getMessage());
          System.out.println("SQLState: " + sqex.getSQLState());
          try {
            Thread.sleep(100);
          } catch(InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    }

    public void initEvent(Socket socket) {
      if (this.isConnected) {
        try {
          java.sql.Statement st = null;
          st = this.firewallConn.createStatement();
          ResultSet rs = null;

          String query = "SELECT data FROM rules_data WHERE 1";

          rs = st.executeQuery(query);

          if(st.execute(query))
            rs = st.getResultSet();

          rs.last();
          int rulesNum = rs.getRow();
          rs.beforeFirst();

          OutputStream outputStream = socket.getOutputStream();
          byte[] rulesNumByte = ByteBuffer.allocate(4).putInt(rulesNum).array();
          outputStream.write(rulesNumByte);

          while(rs.next()) {
            String rule = rs.getString(1);
            
            byte[] ruleLengthByte = ByteBuffer.allocate(4).putInt(rule.length()).array();
            outputStream.write(ruleLengthByte);
            outputStream.write(rule.getBytes());
          }

        } catch (SQLException sqex) {
          System.out.println("SQLExeption: " + sqex.getMessage());
          System.out.println("SQLState: " + sqex.getSQLState());
        } catch (IOException e) {
          e.printStackTrace();
        }

      } else {
      }
    }

    public void expiredEvent(PacketAnalyzer packetAnalyzer) {
      if (this.isConnected) {
        try {
          String saddr = packetAnalyzer.getSaddr();
          String src = packetAnalyzer.getSrc();
          String daddr = packetAnalyzer.getDaddr();
          String dst = packetAnalyzer.getDst();
          String protocol = packetAnalyzer.getProtocol();

          String tcpudp = packetAnalyzer.getTcpudp();
          String warn = packetAnalyzer.getWarn();
          String danger = packetAnalyzer.getDanger();
          String packetCount = packetAnalyzer.getPacketCount();
          String totalbytes = packetAnalyzer.getTotalbytes();

          java.sql.Statement st = null;
          st = this.firewallConn.createStatement();

          String query = "INSERT INTO `packets`(`source_ip`, `source_port`,"
                 + "`destination_ip`, `destination_port`, `protocol`, `tcpudp`,"
                 + "`packet_count`, `totalbytes`,"/* `starttime`, `endtime`,"*/
                 + "`danger`, `warn`) VALUES(" + saddr + ", " + src + ", "
                 + daddr + ", " + dst + ", " + protocol + ", " + tcpudp + ", "
                 + packetCount + ", " + totalbytes + ", " /* + starttime + ", "
                 + endtime + ", " */ + danger + ", " + warn + ")";

          st.executeUpdate(query);

        } catch (SQLException sqex) {
          System.out.println("SQLExeption: " + sqex.getMessage());
          System.out.println("SQLState: " + sqex.getSQLState());
        }
      } else {
      }
    }
}
