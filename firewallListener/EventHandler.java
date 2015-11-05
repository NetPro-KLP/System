package firewallListener;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.net.Socket;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.MalformedURLException;

import java.text.SimpleDateFormat;

import java.io.OutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

import java.nio.ByteBuffer;

import java.util.Queue;
import java.util.LinkedList;
import java.util.Date;

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

    public void checkGeoipBlacklist() {
      if (this.isConnected) {
        Thread thread = new Thread() {
          public void run() {
            try {
              java.sql.Statement st = firewallConn.createStatement();
              ResultSet rs = null;
              Queue<String> blacklistQueue = new LinkedList<String>();
              Queue<String> blacklistCurQueue = null;
              Queue<String> blacklistCopyQueue = null;
              Queue<String> blacklistCurCopyQueue = null;

              String query = "SELECT country_code FROM GeoIP_Blacklist ORDER BY "
                + "country_code DESC";

              rs = st.executeQuery(query);

              if(st.execute(query))
                rs = st.getResultSet();

              while(rs.next()) {
                blacklistQueue.offer(rs.getString(1));
              }

              while (isConnected) {
                /*Socket firewallSocket = new Socket("172.16.101.12", 30001);
                OutputStream outputStream = firewallSocket.getOutputStream(); */
                blacklistCurQueue = new LinkedList<String>();

                query = "SELECT country_code FROM GeoIP_Blacklist ORDER BY "
                  + "country_code DESC";

                rs = st.executeQuery(query);

                if(st.execute(query))
                  rs = st.getResultSet();

                while(rs.next()) {
                  blacklistCurQueue.offer(rs.getString(1));
                }

                blacklistCopyQueue = new LinkedList<String>(blacklistQueue);
                blacklistCurCopyQueue = new
                  LinkedList<String>(blacklistCurQueue);

                boolean isSame = true;
                while (true) {
                  String existData = blacklistCopyQueue.poll();
                  if (existData == null)
                    break;

                  if (!existData.equals(blacklistCurCopyQueue.poll())) {
                    isSame = false;
                    break;
                  }
                }

                if (!isSame) {
                  System.out.println("FUCKFUCKFUCK U!!!!!!!!!");
                  blacklistQueue = blacklistCurQueue;
                }
            
                try {
                  Thread.sleep(1000);
                } catch (InterruptedException e) {
                  e.printStackTrace();
                }
              }
            } catch (SQLException sqex) {
              System.out.println("SQLExeption: " + sqex.getMessage());
              System.out.println("SQLState: " + sqex.getSQLState());
            }
          }
        };

        thread.start();
      } else {
      }
    }

    public void checkPacketTable() {
      if (this.isConnected) {
        Thread thread = new Thread() {
          public void run() {
            try {
              java.sql.Statement st = firewallConn.createStatement();
              ResultSet rs = null;

              SimpleDateFormat simpleDateFormat = null;
              Date date = null;
              String currentDate = null;

              String query = "SELECT source_ip, source_port, destination_ip, "
                + "destination_port, tcpudp, packet_count, totalbytes, starttime, "
                + "endtime, danger, warn FROM packets ORDER BY endtime ASC";

              while(isConnected) {
                rs = st.executeQuery(query);

                if(st.execute(query))
                  rs = st.getResultSet();

                while (rs.next()) {
                  String endtime = rs.getString(9);
                  endtime = endtime.substring(0,10);

                  simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                  date = new Date();
                  currentDate = simpleDateFormat.format(date);

                  if (endtime.equals(currentDate))
                    break;
                  else {
                    long source_ip = rs.getLong(1);
                    String source_port = rs.getString(2);
                    long destination_ip = rs.getLong(3);
                    String destination_port = rs.getString(4);
                    int tcpudp = rs.getInt(5);
                    int packet_count = rs.getInt(6);
                    int totalbytes = rs.getInt(7);
                    String starttime = rs.getString(8);
                    int danger = rs.getInt(10);
                    int warn = rs.getInt(11);

                    String deleteQuery = "DELETE FROM packets WHERE source_ip = '"
                      + source_ip + "', source_port = '" + source_port + "', des"
                      + "tination_ip = '" + destination_ip + "', destination_port"
                      + " = '" + destination_port + "', tcpudp = '" + tcpudp
                      + "', packet_count = '" + packet_count + "', totalbytes = "
                      + "'" + totalbytes + "', starttime = '" + starttime + "'"
                      + ", endtime = '" + endtime + "', danger = '" + danger
                      + "', warn = '" + warn + "'";

                    java.sql.Statement updateSt = firewallConn.createStatement();
                    updateSt.executeUpdate(deleteQuery);

                    String insertQuery = "INSERT INTO `backup_packets`(`source_i"
                      + "p`, `source_port`, `destination_ip`, `destination_port`"
                      + ", `tcpudp`, `packet_count`, `totalbytes`, `starttime`, "
                      + "`endtime`, `danger`, `warn`) VALUES(" + source_ip + ", "
                      + "'" + source_port + "', '" + destination_ip + "', '"
                      + destination_port + "', '" + tcpudp + "', '"
                      + packet_count + "', '" + totalbytes + "', '" + starttime
                      + "', '" + endtime + "', '" + danger + "', '" + warn + "'"
                      + ")";

                    updateSt.executeUpdate(insertQuery);
                  }
                }

                try {
                  Thread.sleep(2000);
                } catch(InterruptedException e) {
                  e.printStackTrace();
                }
              }

            } catch(SQLException sqex) {
              System.out.println("SQLExeption: " + sqex.getMessage());
              System.out.println("SQLState: " + sqex.getSQLState());
            }
          }
        };

        thread.start();
      } else {
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
          String tcpudp = packetAnalyzer.getTcpudp();

          String warn = packetAnalyzer.getWarn();
          String danger = packetAnalyzer.getDanger();
          String packetCount = packetAnalyzer.getPacketCount();
          String totalbytes = packetAnalyzer.getTotalbytes();
          String starttime = packetAnalyzer.getStarttime();
          String endtime = packetAnalyzer.getEndtime();

          java.sql.Statement st = null;
          st = this.firewallConn.createStatement();

          String query = "INSERT INTO `packets`(`source_ip`, `source_port`,"
                 + "`destination_ip`, `destination_port`, `protocol`, `tcpudp`,"
                 + "`packet_count`, `totalbytes`, `starttime`, `endtime`,"
                 + "`danger`, `warn`) VALUES(" + saddr + ", " + src + ", "
                 + daddr + ", " + dst + ", " + tcpudp + ", "
                 + packetCount + ", " + totalbytes + ", " + starttime + ", "
                 + endtime + ", " + danger + ", " + warn + ")";

          st.executeUpdate(query);

        } catch (SQLException sqex) {
          System.out.println("SQLExeption: " + sqex.getMessage());
          System.out.println("SQLState: " + sqex.getSQLState());
        }
      } else {
      }
    }

    public void alarmEvent(PacketAnalyzer packetAnalyzer) {
      if (this.isConnected) {
        try {
          String saddr = packetAnalyzer.getSaddr();
          String src = packetAnalyzer.getSrc();
          String daddr = packetAnalyzer.getDaddr();
          String dst = packetAnalyzer.getDst();
          String tcpudp = packetAnalyzer.getTcpudp();

          String warn = packetAnalyzer.getWarn();
          String danger = packetAnalyzer.getDanger();
          String packetCount = packetAnalyzer.getPacketCount();
          String totalbytes = packetAnalyzer.getTotalbytes();
          String starttime = packetAnalyzer.getStarttime();
          String endtime = packetAnalyzer.getEndtime();

          String name = packetAnalyzer.getName();
          String hazard = packetAnalyzer.getHazard();
          String payload = packetAnalyzer.getPayload();
          String createdAt = packetAnalyzer.getCreatedAt();

          ResultSet rs = null;
          java.sql.Statement st = null;
          st = this.firewallConn.createStatement();

          String packetIdxQuery = "SELECT idx FROM packets WHERE source_ip = "
            + saddr + ", source_port = " + src + ", destination_ip = " + daddr
            + ", destination_port = " + dst + ", tcpudp = " + tcpudp
            + ", packet_count = " + packetCount + ", totalbytes = "
            + totalbytes + ", starttime = " + starttime + ", endtime = "
            + endtime + ", danger = " + danger + ", warn = " + warn;

          rs = st.executeQuery(packetIdxQuery);

          if(st.execute(packetIdxQuery))
            rs = st.getResultSet();

          int packetIdx = 1;

          while(rs.next()) {
            packetIdx = rs.getInt(1);
          }

          String packetLogQuery = "INSERT INTO `packet_log`(`packet_idx`,"
                 + "`name`, `hazard`, `payload`, `createdAt`) VALUES(" +
                 packetIdx + ", " + name + ", " + hazard + ", " + payload +
                 ", " + createdAt + ")";

          st.executeUpdate(packetLogQuery);

          String url = "http://172.16.100.61/alarm.php";

          try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            con.setRequestMethod("POST");
            String urlParameters = "name=" + name + "&hazard=" + hazard + 
              "&payload=" + payload + "&date=" + createdAt;

            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.flush();
            wr.close();

          } catch (MalformedURLException e) {
            e.printStackTrace();
          } catch (IOException e) {
            e.printStackTrace();
          }

        } catch (SQLException sqex) {
          System.out.println("SQLExeption: " + sqex.getMessage());
          System.out.println("SQLState: " + sqex.getSQLState());
        }
      } else {
      }
    }
}
