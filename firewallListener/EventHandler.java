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
import java.util.StringTokenizer;

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
                        try {
                        long source_ip = rs.getLong(1);
                        String source_port = rs.getString(2);
                        long destination_ip = rs.getLong(3);
                        String destination_port = rs.getString(4);
                        int tcpudp = rs.getInt(5);
                        int packet_count = rs.getInt(6);
                        int totalbytes = rs.getInt(7);
                        String starttime = (rs.getString(8)).substring(0,19);
                        endtime = (rs.getString(9)).substring(0,19);
                        int danger = rs.getInt(10);
                        int warn = rs.getInt(11);

                        String deleteQuery = "DELETE FROM `packets` WHERE source_ip = '"
                          + source_ip + "' AND source_port = '" + source_port + "'"
                          + " AND destination_ip = '" + destination_ip + "' AND "
                          + "destination_port = '" + destination_port + "' AND tcpudp = '"
                          + tcpudp + "' AND packet_count = '" + packet_count + "'"
                          + " AND totalbytes = '" + totalbytes + "' AND starttime = '"
                          + starttime + "' AND endtime = '" + endtime + "' AND danger = '"
                          + danger + "' AND warn = '" + warn + "'";

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
                        } catch (SQLException sqex) {
                          System.out.println("SQLExeption: " + sqex.getMessage());
                          System.out.println("SQLState: " + sqex.getSQLState());
                        }
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

          System.out.println("saddr: " + saddr + ", src: " + src + ", daddr: "
              + daddr + ", dst: " + dst + ", tcpudp: " + tcpudp + ", warn: "
              + warn + ", danger: " + danger + ", packetCount: " + packetCount
              + ", totalbytes: " + totalbytes + ", starttime: " + starttime
              + ", endtime: " + endtime);

          long long_from_ip = 0;
          long long_to_ip = 0;

          java.sql.Statement st = null;
          st = this.firewallConn.createStatement();

          String query = "SELECT bandwidth_from_ip, bandwidth_to_ip "
            + "FROM system WHERE 1";

          ResultSet rs = st.executeQuery(query);

          if(st.execute(query))
            rs = st.getResultSet();

          while(rs.next()) {
            String from_ip = rs.getString(1);
            String to_ip = rs.getString(2);

            long ip = 0;
            long ip2 = 0;
            long ip3 = 0;
            long ip4 = 0;

            StringTokenizer token = new StringTokenizer(from_ip, ".");
            
            if (token.hasMoreTokens())
              ip = Long.parseLong(token.nextToken());
            if (token.hasMoreTokens())
              ip2 = Long.parseLong(token.nextToken());
            if (token.hasMoreTokens())
              ip3 = Long.parseLong(token.nextToken());
            if (token.hasMoreTokens())
              ip4 = Long.parseLong(token.nextToken());

            long_from_ip = (ip & 0xff) << 24 | (ip2 & 0xff) << 16 |
              (ip3 & 0xff) << 8 | (ip4 & 0xff);

            token = new StringTokenizer(to_ip, ".");

            if (token.hasMoreTokens())
              ip = Long.parseLong(token.nextToken());
            if (token.hasMoreTokens())
              ip2 = Long.parseLong(token.nextToken());
            if (token.hasMoreTokens())
              ip3 = Long.parseLong(token.nextToken());
            if (token.hasMoreTokens())
              ip4 = Long.parseLong(token.nextToken());

            long_to_ip = (ip & 0xff) << 24 | (ip2 & 0xff) << 16 |
              (ip3 & 0xff) << 8 | (ip4 & 0xff);
          }

          query = "INSERT INTO `packets`(`source_ip`, `source_port`,"
              + "`destination_ip`, `destination_port`, `tcpudp`,"
              + "`packet_count`, `totalbytes`, `starttime`, `endtime`,"
              + "`danger`, `warn`) VALUES('" + saddr + "', '" + src + "', '"
              + daddr + "', '" + dst + "', '" + tcpudp + "', '"
              + packetCount + "', '" + totalbytes + "', '" + starttime + "', '"
              + endtime + "', '" + danger + "', '" + warn + "')";

          st.executeUpdate(query);

          Long saddrLong = Long.parseLong(saddr);
          Long daddrLong = Long.parseLong(daddr);

          Date curtime = new Date();
          SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
          String At = sdf.format(curtime);

          if (saddrLong >= long_from_ip && saddrLong <= long_to_ip) {
            query = "INSERT INTO `users`(`ip`, `createdAt`, `connectedAt`, "
              + "`status`) VALUES('" + saddrLong + "', '" + At + "', '" + At
              + "', '0')";

            st.executeUpdate(query);
          }
          if (daddrLong >= long_from_ip && daddrLong <= long_to_ip) {
            query = "INSERT INTO `users`(`ip`, `createdAt`, `connectedAt`, "
              + "`status`) VALUES('" + daddrLong + "', '" + At + "', '" + At
              + "', '0')";

            st.executeUpdate(query);
          }

          query = "SELECT country_code, country FROM GeoIP WHERE (from_ip_int <= " +
            saddrLong + ") AND (to_ip_int >= " + saddrLong + ")";

          rs = st.executeQuery(query);

          if(st.execute(query))
            rs = st.getResultSet();

          while(rs.next()) {
            String country_code = rs.getString(1);
            String country = rs.getString(2);

            query = "SELECT country_code FROM GeoIP_Traffic WHERE country_code"
              + " = '" + country_code + "'";

            java.sql.Statement st2 = this.firewallConn.createStatement();
            ResultSet rs2 = st2.executeQuery(query);

            if(st2.execute(query))
              rs2 = st2.getResultSet();

            if(rs2.next()) {
              query = "UPDATE GeoIP_Traffic SET totalbytes = totalbytes + "
                + totalbytes + " WHERE country_code = '" + country_code + "'";

              st2.executeUpdate(query);
            } else {
              query = "INSERT INTO `GeoIP_Traffic`(`country_code`, `country`, "
                + "`totalbytes`) VALUES('" + country_code + "', '" + country
                + "', " + totalbytes + ")";

              st2.executeUpdate(query);
            }
          }

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
