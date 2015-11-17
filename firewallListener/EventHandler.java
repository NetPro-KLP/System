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
import java.text.DateFormat;

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
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    }

    public void checkBanUser() {
      if (this.isConnected) {
        Thread thread = new Thread() {
          public void run() {
            try {
              java.sql.Statement st = firewallConn.createStatement();
              ResultSet rs = null;
              String query = "SELECT ip FROM users WHERE status = 1 ORDER BY "
                      + "createdAt ASC";

              Queue<Long> banUserIpQueue = new LinkedList<Long>();
              Queue<Long> banUserIpCopyQueue = null;
              Queue<Long> banUserIpCurQueue = null;
              Queue<Long> banUserIpCurCopyQueue = null;

              boolean isSame = true;
              Long existData;
              Long curData;
              String diff = null;
              String header = null;
              String payload = null;

              rs = st.executeQuery(query);

              if(st.execute(query))
                rs = st.getResultSet();

              while(rs.next()) {
                banUserIpQueue.offer(rs.getLong(1));
              }

              while(isConnected) {
                banUserIpCopyQueue = new LinkedList<Long>(banUserIpQueue);
                banUserIpCurQueue = new LinkedList<Long>();

                query = "SELECT ip FROM users WHERE status = 1 ORDER BY "
                        + "createdAt ASC";

                rs = st.executeQuery(query);

                if(st.execute(query))
                  rs = st.getResultSet();

                while(rs.next()) {
                  banUserIpCurQueue.offer(rs.getLong(1));
                }

                banUserIpCurCopyQueue = new LinkedList<Long>(banUserIpCurQueue);

                isSame = true;
                while (true) {
                  existData = banUserIpCopyQueue.poll();
                  if (existData == null) {
                    // 추가된 경우
                    if (banUserIpCurCopyQueue.size() >= 1) {
                      isSame = false;
                      diff = "add";
                    }
                    break;
                  }

                  curData = banUserIpCurCopyQueue.poll();

                  // 삭제된 경우
                  if (curData == null) {
                    isSame = false;
                    diff = "del";
                    break;
                  }

                  if (!existData.equals(curData)) {
                    isSame = false;
                    diff = "strange";
                    break;
                  }
                }

                if (!isSame) {
                  if (diff.equals("add")) {
                    header = "usr|" + banUserIpCurCopyQueue.size();
                    //outputStream.write(header.getBytes());

                    while (true) {
                      curData = banUserIpCurCopyQueue.poll();

                      if (curData == null)
                        break;
                      else {
                        payload = Long.toString(curData);
                        //outputStream.write(payload);
                      }
                    }
                  } else if (diff.equals("del")) {
                    header = "urm|" + Integer.toString(banUserIpCopyQueue.size() + 1);
                    //outputStream.write(header.getBytes());
                    payload = Long.toString(existData);
                    //outputStream.write(payload.getbytes());

                    while (true) {
                      existData = banUserIpCopyQueue.poll();

                      if (existData == null)
                        break;
                      else {
                        payload = Long.toString(existData);
                        //outputStream.write(payload.getBytes());
                      }
                    }
                  } else {
                    System.out.println("strange");
                  }
                }

                try {
                  Thread.sleep(1000);
                } catch (InterruptedException e) {
                  e.printStackTrace();
                }
              }
            } catch (SQLException sqex) {
              System.out.println("SQLException: " + sqex.getMessage());
              System.out.println("SQLState: " + sqex.getSQLState());
            }
          }
        };

        thread.start();
      } else {}
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

              boolean isSame = true;
              String existData = null;
              String curData = null;
              String diff = null;
              String header = null;
              String payload = null;
              long from_ip = 0;
              long to_ip = 0;
              int insertNum = 0;
/*
              Socket firewallSocket = new Socket("172.16.101.12", 30001);
              OutputStream outputStream = firewallSocket.getOutputStream();*/

              String query = "SELECT country_code FROM GeoIP_Blacklist ORDER BY "
                + "createdAt ASC";

              rs = st.executeQuery(query);

              if(st.execute(query))
                rs = st.getResultSet();

              while(rs.next()) {
                blacklistQueue.offer(rs.getString(1));
              }

              while (isConnected) {
                blacklistCurQueue = new LinkedList<String>();

                query = "SELECT country_code FROM GeoIP_Blacklist ORDER BY "
                  + "createdAt ASC";

                rs = st.executeQuery(query);

                if(st.execute(query))
                  rs = st.getResultSet();

                while(rs.next()) {
                  blacklistCurQueue.offer(rs.getString(1));
                }

                blacklistCopyQueue = new LinkedList<String>(blacklistQueue);
                blacklistCurCopyQueue = new
                  LinkedList<String>(blacklistCurQueue);

                isSame = true;
                while (true) {
                  existData = blacklistCopyQueue.poll();
                  if (existData == null) {
                    // 추가된 경우
                    if (blacklistCurCopyQueue.size() >= 1) {
                      isSame = false;
                      diff = "add";
                    }
                    break;
                  }

                  curData = blacklistCurCopyQueue.poll();

                  // 삭제된 경우
                  if (curData == null) {
                    isSame = false;
                    diff = "del";
                    break;
                  }

                  if (!existData.equals(curData)) {
                    isSame = false;
                    diff = "strange";
                    break;
                  }
                }

                if (!isSame) {
                  if (diff.equals("add")) {
                    while (true) {
                      curData = blacklistCurCopyQueue.poll();

                      if(curData == null)
                        break;
                      else {
                        query = "SELECT DISTINCT from_ip_int, to_ip_int FROM GeoIP WHERE "
                          + "country_code = '" + curData + "'";

                        rs = st.executeQuery(query);

                        if (st.execute(query))
                          rs = st.getResultSet();

                        rs.last();
                        insertNum = rs.getRow();
                        rs.beforeFirst();

                        header = "geo|" + curData + "|" + Integer.toString(insertNum);
                        //outputStream.write(header.getBytes());

                        while (rs.next()) {
                          from_ip = rs.getLong(1);
                          to_ip = rs.getLong(2);
                          payload = Long.toString(from_ip) + "|" + Long.toString(to_ip);
                          //outputStream.write(payload.getBytes());
                        }
                      }
                    }
                  } else if (diff.equals("del")) {
                    header = "grm|" + existData;
                    //outputStream.write(header.getBytes());
                    while (true) {
                      existData = blacklistCopyQueue.poll();

                      if(existData == null)
                        break;
                      else {
                        header = "grm|" + existData;
                        //outputStream.write(header.getBytes());
                      }
                    }
                  } else {
                    System.out.println("strange");
                  }

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
            } /*catch (IOException e) {
              e.printStackTrace();
            }*/
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

              DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
              Date date = null;

              String query = "SELECT remove_packet FROM system WHERE 1";
              String packetQuery = null;
              String timeString = null;
              String deleteQuery = null;
              String insertQuery = null;

              long source_ip = 0;
              String source_port = null;
              long destination_ip = 0;
              String destination_port = null;
              int tcpudp = 0;
              int packet_count = 0;
              int totalbytes = 0;
              String starttime = null;
              String endtime = null;
              int danger = 0;
              int warn = 0;

              while(isConnected) {
                rs = st.executeQuery(query);

                if(st.execute(query))
                  rs = st.getResultSet();

                while (rs.next()) {
                  date = new Date();
                  date = new Date((date.getTime()/1000) - rs.getLong(1));
                  timeString = df.format(date);

                  packetQuery = "SELECT source_ip, source_port, destination_ip, "
                    + "destination_port, tcpudp, packet_count, totalbytes, starttime, "
                    + "endtime, danger, warn FROM packets WHERE endtime <= '"
                    + timeString + "' ORDER BY endtime ASC";

                  java.sql.Statement st2 = firewallConn.createStatement();
                  rs = st2.executeQuery(packetQuery);

                  if(st2.execute(packetQuery))
                    rs = st2.getResultSet();

                  while (rs.next()) {
                      try {
                        source_ip = rs.getLong(1);
                        source_port = rs.getString(2);
                        destination_ip = rs.getLong(3);
                        destination_port = rs.getString(4);
                        tcpudp = rs.getInt(5);
                        packet_count = rs.getInt(6);
                        totalbytes = rs.getInt(7);
                        starttime = (rs.getString(8)).substring(0,19);
                        endtime = (rs.getString(9)).substring(0,19);
                        danger = rs.getInt(10);
                        warn = rs.getInt(11);

                        deleteQuery = "DELETE FROM `packets` WHERE source_ip = '"
                          + source_ip + "' AND source_port = '" + source_port + "'"
                          + " AND destination_ip = '" + destination_ip + "' AND "
                          + "destination_port = '" + destination_port + "' AND tcpudp = '"
                          + tcpudp + "' AND packet_count = '" + packet_count + "'"
                          + " AND totalbytes = '" + totalbytes + "' AND starttime = '"
                          + starttime + "' AND endtime = '" + endtime + "' AND danger = '"
                          + danger + "' AND warn = '" + warn + "'";

                        java.sql.Statement updateSt = firewallConn.createStatement();
                        updateSt.executeUpdate(deleteQuery);

                        insertQuery = "INSERT INTO `backup_packets`(`source_i"
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

          OutputStream outputStream = socket.getOutputStream();
          String query = "SELECT data FROM rules_data WHERE 1";

          rs = st.executeQuery(query);

          if(st.execute(query))
            rs = st.getResultSet();

          boolean isShap = false;
          int contentPattern = 0;
          int outPattern = 0;
          int outPattern2 = 0;
          int outPattern3 = 0;
          int outPattern4 = 0;
          int outPattern5 = 0;
          int outPattern6 = 0;
          int outPattern7 = 0;
          int outPattern8 = 0;
          int outPattern9 = 0;
          int outPattern10 = 0;
          int rowNum = 0;
          int i = 0;
          long from_ip = 0;
          long to_ip = 0;

          Queue<String> ruleset = new LinkedList<String>();
          Queue<String> country_code = new LinkedList<String>();
          StringTokenizer token = null;
          String country = null;
          String header = null;
          String payload = null;

          while(rs.next()) {
            String rule = rs.getString(1);
            isShap = false;
            if (rule.substring(0,1).equals("#"))
              isShap = true;

            contentPattern = rule.indexOf("content");
            rule = rule.substring(contentPattern + 8, rule.length());
            token = new StringTokenizer(rule, ";");
            if (token.hasMoreTokens())
              rule = token.nextToken();

            outPattern = rule.indexOf("|");
            outPattern2 = rule.indexOf("{");
            outPattern3 = rule.indexOf("tcp");
            outPattern4 = rule.indexOf("udp");
            outPattern5 = rule.indexOf("msg:");
            outPattern6 = rule.indexOf("POST");
            outPattern7 = rule.indexOf("GET");
            outPattern8 = rule.indexOf("UPDATE");
            outPattern9 = rule.indexOf("X-Forwarded-For");
            outPattern10 = rule.indexOf("SIP/2.0");

            if (outPattern >= 0 || outPattern2 >= 0 || outPattern3 >= 0
                    || outPattern4 >= 0 || outPattern5 >= 0 || outPattern6 >= 0
                    || outPattern7 >= 0 || outPattern8 >= 0 || outPattern9 >= 0
                    || outPattern10 >= 0) {}
            else {
              rule = rule.substring(1, rule.length() - 1);
              if (isShap)
                ruleset.offer(rule + "|0");
              else
                ruleset.offer(rule + "|1");
            }
          }
          header = "rul|" + Integer.toString(ruleset.size()) + "|";
          for(; i < 254 - header.length(); i++)
            header = header + "-";
          System.out.println("rul header: " + header);
          outputStream.write(header.getBytes());
          while(ruleset.peek() != null) {
            payload = ruleset.poll() + "|";
            for (i = 0; i < 254 - payload.length(); i++)
              payload = payload + "-";
            outputStream.write(payload.getBytes());
          }

          System.out.println("rules_data finished");
          query = "SELECT country_code FROM GeoIP_Blacklist WHERE 1";

          rs = st.executeQuery(query);

          if(st.execute(query))
            rs = st.getResultSet();

          while(rs.next()) {
            country_code.offer(rs.getString(1));
          }

          header = "geo|" + Integer.toString(country_code.size()) + "|";
          for(i = 0; i < 254 - header.length(); i++)
            header = header + "-";
          outputStream.write(header.getBytes());
          System.out.println("geo header: " + header);

          while(country_code.peek() != null) {
            country = country_code.poll();
            query = "SELECT DISTINCT from_ip_int, to_ip_int FROM GeoIP WHERE "
                    + "country_code = '" + country + "'";

            rs = st.executeQuery(query);

            if (st.execute(query))
              rs = st.getResultSet();

            rs.last();
            rowNum = rs.getRow();
            rs.beforeFirst();

            header = "geo|" + country + "|" + Integer.toString(rowNum) + "|";
            for(i = 0; i < 254 - header.length(); i++)
              header = header + "-";
            outputStream.write(header.getBytes());
            System.out.println(country + " header: " + header);

            while(rs.next()) {
              from_ip = rs.getLong(1);
              to_ip = rs.getLong(2);

              payload = Long.toString(from_ip) + "|" + Long.toString(to_ip) + "|";
              for(i = 0; i < 254 - payload.length(); i++)
                payload = payload + "-";
              outputStream.write(payload.getBytes());
            }
            System.out.println(country + " ip finished");
          }

          query = "SELECT ip FROM users WHERE status = 1";

          rs = st.executeQuery(query);

          if(st.execute(query))
            rs = st.getResultSet();

          rs.last();
          rowNum = rs.getRow();
          rs.beforeFirst();

          header = "ban|" + Integer.toString(rowNum);
          //outputStream.write(header.getBytes());

          while(rs.next()) {
            payload = Long.toString(rs.getLong(1));
            //outputStream.write(payload.getBytes());
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
          long saddrLong = Long.parseLong(saddr);
          long daddrLong = Long.parseLong(daddr);

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

          java.sql.Statement st2 = this.firewallConn.createStatement();
          ResultSet rs2 = st2.executeQuery(query);
          String country_code = null;
          String country = null;

          while(rs.next()) {
            country_code = rs.getString(1);
            country = rs.getString(2);

            query = "SELECT country_code FROM GeoIP_Traffic WHERE country_code"
              + " = '" + country_code + "'";

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
