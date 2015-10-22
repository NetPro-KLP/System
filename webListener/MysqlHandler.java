package webListener;

import com.nhncorp.mods.socket.io.SocketIOSocket;

import java.lang.Math;

import java.io.IOException;
import java.io.OutputStream;
import java.io.DataOutputStream;

import java.nio.ByteBuffer;

import java.net.Socket;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.JsonArray;

public class MysqlHandler {

  private Socket firewallSocket;
  private SocketIOSocket socket;
  private Connection firewallConn = null;
  private boolean isConnected;
  private boolean isRealtime;

    public MysqlHandler(String host, String userId, String pw,
        SocketIOSocket socket) {

      this.isConnected = false;
      this.isRealtime = false;

      while (!this.isConnected) {
        try {
            this.socket = socket;
            this.firewallConn =
              DriverManager.getConnection("jdbc:mysql://" + host +
                  "/KLP-Firewall", userId, pw);
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

    private java.sql.Statement getSt() {
      java.sql.Statement st = null;

      try {
          st = this.firewallConn.createStatement();
      } catch (SQLException sqex) {
          System.out.println("SQLException: " + sqex.getMessage());
          System.out.println("SQLState: " + sqex.getSQLState());
      }

      return st;
    }

    private void resToWeb(String emitTo, String code, String body) {
      Thread thread = new Thread() {
        public void run() {
          JsonObject reply = null;

          if (isConnected) {
            if (code != null) {
              reply = new JsonObject().putNumber("code", Integer.parseInt(code));
              if (body != null) {
                reply.putString("body", body);
              }
            }
          } else {
            reply = new JsonObject().putString("code", "400");
            reply.putString("body", "Database connection failed");
          }

          socket.emit(emitTo, reply);
        }
      };

      thread.start();
    }

    public void realtimeOn(String emitTo) {

      this.isRealtime = true;

      if (this.isConnected) {

        Thread thread = new Thread() {
          public void run() {
            java.sql.Statement st = getSt();

            /*
            // inboud, outbound packet 정보를 모두 불러오는 쿼리 : A
            String outboundPacketQuery = "SELECT u.ip, p.source_ip, p.source_port,"
              + "p.destination_ip, p.destination_port, p.protocol, p.tcpudp,"
              + "p.packet_count, p.totalbytes, p.starttime, p.endtime, p.danger,"
              + "p.warn FROM packets p JOIN users u ON u.ip = p.source_ip";
            String inboundPacketQuery = "SELECT u.ip, p.source_ip, p.source_port,"
              + "p.destination_ip, p.destination_port, p.protocol, p.tcpudp,"
              + "p.packet_count, p.totalbytes, p.starttime, p.endtime, p.danger,"
              + "p.warn FROM packets p JOIN users u ON u.ip = p.destination_ip";
            */
            String trafficQuery = "SELECT u.idx, u.ip, u.connectedAt, u.status, p.starttime, p.endtime"
              + ", SUM(p.totalbytes), SUM(p.danger), SUM(p.warn)"
              + " FROM users u JOIN packets p ON u.ip = p.source_ip"
              + " OR u.ip = p.destination_ip GROUP BY u.idx, p.endtime"
              + " ORDER BY u.idx, p.endtime DESC";

            double maxTraffic = Math.pow(2, 30);

            try {
              while(isRealtime) {

                JsonObject reply = new JsonObject();
                JsonArray trafficArray = new JsonArray();
                ResultSet rs = null;

                rs = st.executeQuery(trafficQuery);

                if(st.execute(trafficQuery))
                  rs = st.getResultSet();

                int cnt = 0;
                int percentage = 0;
                int preIdx = -1;

                while(rs.next()) {
                  JsonObject trafficObject = null;

                  int idx = rs.getInt(1);
                  String ip = rs.getString(2);
                  String connectedAt = rs.getString(3);
                  int status = rs.getInt(4);
                  String starttime = rs.getString(5);
                  String endtime = rs.getString(6);
                  int traffic = rs.getInt(7);
                  int danger = rs.getInt(8);
                  int warn = rs.getInt(9);

                  connectedAt = connectedAt.substring(0,19);
                  starttime = starttime.substring(0,19);
                  endtime = endtime.substring(0,19);

                  if (preIdx == -1) {
                    preIdx = idx;
                    reply.putNumber("code", 200);
                  }

                  if (preIdx != idx) {
                    cnt = 1;
                    reply.putArray(Integer.toString(preIdx), trafficArray);
                    trafficArray = new JsonArray();
                    preIdx = idx;
                  }
                  else
                    cnt++;

                  if (cnt <= 20) {
                    percentage = (int)(((double)traffic / maxTraffic) * 100);
                    if (percentage > 100)
                      percentage = 100;

                    trafficObject = new JsonObject().putString("ip", ip);
                    trafficObject.putString("connectedAt", connectedAt);
                    trafficObject.putNumber("status", status);
                    trafficObject.putString("starttime", starttime);
                    trafficObject.putString("endtime", endtime);
                    trafficObject.putNumber("traffic", traffic);
                    trafficObject.putNumber("danger", danger);
                    trafficObject.putNumber("warn", warn);
                    trafficObject.putNumber("trafficPercentage", percentage);
                    trafficArray.addObject(trafficObject);
                  }
                }

                reply.putArray(Integer.toString(preIdx), trafficArray);

                /*  A 내용
                ResultSet rs = null;
                JsonArray inboundPacketArray = new JsonArray();
                JsonArray outboundPacketArray = new JsonArray();
                rs = st.executeQuery(inboundPacketQuery);
                if(st.execute(inboundPacketQuery))
                  rs = st.getResultSet();
                while(rs.next()) {
                  JsonObject inboundObject = null;
                  String ip = rs.getString(1);
                  String source_ip = rs.getString(2);
                  String source_port = rs.getString(3);
                  String destination_ip = rs.getString(4);
                  String destination_port = rs.getString(5);
                  String protocol = rs.getString(6);
                  int tcpudp = rs.getInt(7);
                  int packet_count = rs.getInt(8);
                  int totalbytes = rs.getInt(9);
                  String starttime = rs.getString(10);
                  String endtime = rs.getString(11);
                  int danger = rs.getInt(12);
                  int warn = rs.getInt(13);
                  inboundObject = new JsonObject().putString("ip", ip);
                  inboundObject.putString("source_ip", source_ip);
                  inboundObject.putString("source_port", source_port);
                  inboundObject.putString("destination_ip", destination_ip);
                  inboundObject.putString("destination_port", destination_port);
                  inboundObject.putString("protocol", protocol);
                  inboundObject.putNumber("tcpudp", tcpudp);
                  inboundObject.putNumber("packet_count", packet_count);
                  inboundObject.putNumber("totalbytes", totalbytes);
                  inboundObject.putString("starttime", starttime);
                  inboundObject.putString("endtime", endtime);
                  inboundObject.putNumber("danger", danger);
                  inboundObject.putNumber("warn", warn);
                  inboundPacketArray.addObject(inboundObject);
                }
                rs = null;
                rs = st.executeQuery(outboundPacketQuery);
                if(st.execute(outboundPacketQuery))
                  rs = st.getResultSet();
                while(rs.next()) {
                  JsonObject outboundObject = null;
                  String ip = rs.getString(1);
                  String source_ip = rs.getString(2);
                  String source_port = rs.getString(3);
                  String destination_ip = rs.getString(4);
                  String destination_port = rs.getString(5);
                  String protocol = rs.getString(6);
                  int tcpudp = rs.getInt(7);
                  int packet_count = rs.getInt(8);
                  int totalbytes = rs.getInt(9);
                  String starttime = rs.getString(10);
                  String endtime = rs.getString(11);
                  int danger = rs.getInt(12);
                  int warn = rs.getInt(13);
                  outboundObject = new JsonObject().putString("ip", ip);
                  outboundObject.putString("source_ip", source_ip);
                  outboundObject.putString("source_port", source_port);
                  outboundObject.putString("destination_ip", destination_ip);
                  outboundObject.putString("destination_port", destination_port);
                  outboundObject.putString("protocol", protocol);
                  outboundObject.putNumber("tcpudp", tcpudp);
                  outboundObject.putNumber("packet_count", packet_count);
                  outboundObject.putNumber("totalbytes", totalbytes);
                  outboundObject.putString("starttime", starttime);
                  outboundObject.putString("endtime", endtime);
                  outboundObject.putNumber("danger", danger);
                  outboundObject.putNumber("warn", warn);
                  outboundPacketArray.addObject(outboundObject);
                }
                reply.putNumber("code", 200);
                reply.putArray("inboundPacket", inboundPacketArray);
                reply.putArray("outboundPacket", outboundPacketArray);
                // A 내용
                */

                socket.emit(emitTo, reply);

                try {
                  Thread.sleep(1000);
                } catch (InterruptedException e) {
                  e.printStackTrace();
                }
              } // while: isRealtime
            } catch (SQLException sqex) {
              System.out.println("SQLException: " + sqex.getMessage());
              System.out.println("SQLState: " + sqex.getSQLState());
              resToWeb(emitTo, "400", "realtimeOn: somethings were error");
            }
          } // Thread: run()
        }; // Thread thread = new Thread() {
        thread.start();
      } else {
        resToWeb(emitTo, "400", "Database connection failed");
      }
    }

    public void realtimeClose(String emitTo) {

      this.isRealtime = false;

      if (this.isConnected) {
        resToWeb(emitTo, "204", null);
      } else {
        resToWeb(emitTo, "400", "Database connection failed");
      }
    }

    public void trafficStatistics(String emitTo) {

      if (this.isConnected) {
        Thread thread = new Thread() {
          public void run() {
            try {
              java.sql.Statement st = getSt();
              String barTcpQuery = "SELECT p.starttime, p.endtime, "
                + "SUM(p.totalbytes), SUM(p.danger), SUM(p.warn) FROM packets p"
                + " WHERE p.tcpudp = 0 GROUP BY p.endtime DESC";

              String barUdpQuery = "SELECT p.starttime, p.endtime, "
                + "SUM(p.totalbytes), SUM(p.danger), SUM(p.warn) FROM packets p"
                + " WHERE p.tcpudp = 1 GROUP BY p.endtime DESC";

              String lineQuery = "";
              String piQuery = "";
              String realtimeChartQuery = "";
              String axisLineQuery = "";

              JsonObject reply = new JsonObject();
              ResultSet rs = null;

              rs = st.executeQuery(barTcpQuery);

              if(st.execute(barTcpQuery))
                rs = st.getResultSet();

              String preTime = "init";
              int totalbytesEachTime = 0;
              String preDate = null;

              JsonArray jsonArray = new JsonArray();
              JsonObject jsonGroup = new JsonObject();

              while (rs.next()) {
                JsonObject tcpObject = null;

                String starttime = rs.getString(1);
                String endtime = rs.getString(2);
                int totalbytes = rs.getInt(3);
                int danger = rs.getInt(4);
                int warn = rs.getInt(5);

                starttime = starttime.substring(0,19);
                endtime = endtime.substring(0,19);
                String curtime = endtime.substring(11,13);
                String curdate = endtime.substring(0,11);

                if (preTime.equals("init")) {
                  preTime = curtime;
                  preDate = curdate;
                  reply.putNumber("code", 200);
                }

                if (!preTime.equals(curtime) || !preDate.equals(curdate)) {
                  tcpObject = new JsonObject().putNumber("totalbytes",
                      totalbytesEachTime);
                  jsonArray.addObject(tcpObject);
                  jsonGroup.putArray(preDate + preTime, jsonArray);

                  jsonArray = new JsonArray();
                  totalbytesEachTime = 0;
                  preTime = curtime;
                  preDate = curdate;
                }

                tcpObject = new JsonObject().putString("starttime", starttime);
                tcpObject.putString("endtime", endtime);
                tcpObject.putNumber("danger", danger);
                tcpObject.putNumber("warn", warn);
                tcpObject.putNumber("eachbytes", totalbytes);
                totalbytesEachTime = totalbytesEachTime + totalbytes;
                jsonArray.addObject(tcpObject);
              }

              JsonObject insertObject = new JsonObject().putNumber("totalbytes", 
                  totalbytesEachTime);
              jsonArray.addObject(insertObject);
              jsonGroup.putArray(preDate + preTime, jsonArray);
              reply.putObject("tcpTraffic", jsonGroup);

              rs = st.executeQuery(barUdpQuery);

              if(st.execute(barUdpQuery))
                rs = st.getResultSet();

              preTime = "init";
              totalbytesEachTime = 0;
              preDate = null;

              jsonArray = new JsonArray();
              jsonGroup = new JsonObject();

              while (rs.next()) {
                JsonObject udpObject = null;

                String starttime = rs.getString(1);
                String endtime = rs.getString(2);
                int totalbytes = rs.getInt(3);
                int danger = rs.getInt(4);
                int warn = rs.getInt(5);

                starttime = starttime.substring(0,19);
                endtime = endtime.substring(0,19);
                String curtime = endtime.substring(11,13);
                String curdate = endtime.substring(0,11);

                if (preTime.equals("init")) {
                  preTime = curtime;
                  preDate = curdate;
                  reply.putNumber("code", 200);
                }

                if (!preTime.equals(curtime) || !preDate.equals(curdate)) {
                  udpObject = new JsonObject().putNumber("totalbytes",
                      totalbytesEachTime);
                  jsonArray.addObject(udpObject);
                  jsonGroup.putArray(preDate + preTime, jsonArray);

                  jsonArray = new JsonArray();
                  totalbytesEachTime = 0;
                  preTime = curtime;
                  preDate = curdate;
                }

                udpObject = new JsonObject().putString("starttime", starttime);
                udpObject.putString("endtime", endtime);
                udpObject.putNumber("danger", danger);
                udpObject.putNumber("warn", warn);
                udpObject.putNumber("eachbytes", totalbytes);
                totalbytesEachTime = totalbytesEachTime + totalbytes;
                jsonArray.addObject(udpObject);
              }

              insertObject = new JsonObject().putNumber("totalbytes", 
                  totalbytesEachTime);
              jsonArray.addObject(insertObject);
              jsonGroup.putArray(preDate + preTime, jsonArray);
              reply.putObject("udpTraffic", jsonGroup);
/*
              rs = st.executeQuery(lineQuery);

              if(st.execute(lineQuery))
                rs = st.getResultSet();

              rs = st.executeQuery(piQuery);

              if(st.execute(piQuery))
                rs = st.getResultSet();

              rs = st.executeQuery(realtimeChartQuery);

              if(st.execute(realtimeChartQuery))
                rs = st.getResultSet();

              rs = st.executeQuery(axisLineQuery);

              if(st.execute(axisLineQuery))
                rs = st.getResultSet();*/

              socket.emit(emitTo, reply);

            } catch (SQLException sqex) {
              System.out.println("SQLException: " + sqex.getMessage());
              System.out.println("SQLState: " + sqex.getSQLState());
              resToWeb(emitTo, "400", "trafficStatistics: somethings were error");
            }
          }
        };

        thread.start();
      } else {
        resToWeb(emitTo, "400", "Database connection failed");
      }
    }

    public void notifyToFirewall(String emitTo, String admin_idx,
        String action, String contents, String date) {
      if (this.isConnected) {
        Thread thread = new Thread() {
          public void run() {
            try {
              firewallSocket = new Socket("172.16.101.12", 30001);
              OutputStream outputStream = firewallSocket.getOutputStream();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        };

        thread.start();
      } else {
        resToWeb(emitTo, "400", "Database connection failed");
      }
    }



    public void insertLogHandler(String emitTo, String admin_idx,
        String action, String date) {

      if (this.isConnected) {
        Thread thread = new Thread() {
          public void run() {
            try {
              java.sql.Statement st = getSt();

              String query = "INSERT INTO `log`(`admin_idx`,`action`,`date`) VALUES(" + admin_idx
                + ", '" + action + "', '" + date + "')";

              st.executeUpdate(query);

              // insert 성공(201)
              resToWeb(emitTo, "201", null);

            } catch (SQLException sqex) {
              System.out.println("SQLException: " + sqex.getMessage());
              System.out.println("SQLState: " + sqex.getSQLState());
              resToWeb(emitTo, "400", "insert log: somethings were error");
            }
          }
        };

        thread.start();
      } else {
        resToWeb(emitTo, "400", "Database connection failed");
      }
    }
/*
    public void insertHandler() {
      if (this.isConnected) {
        try {
        } catch (SQLException sqex) {
          System.out.println("SQLException: " + sqex.getMessage());
          System.out.println("SQLState: " + sqex.getSQLState());
        }
      } else {
      }
    }
*/
    public void deleteHandler(String emitTo, String table, String key, String value) {

      if (this.isConnected) {
        try {
          java.sql.Statement st = getSt();

          String query = "DELETE FROM " + table + " WHERE " + key + " = '"
            + value + "'";

          st.executeUpdate(query);

          resToWeb(emitTo, "204", null);
        } catch (SQLException sqex) {
          System.out.println("SQLException: " + sqex.getMessage());
          System.out.println("SQLState: " + sqex.getSQLState());
          resToWeb(emitTo, "400", "delete: somethings were error");
        }
      } else {
        resToWeb(emitTo, "400", "Database connection failed");
      }
    }
/*
    public void updateHandler() {
      if (this.isConnected) {
        try {
        } catch (SQLException sqex) {
          System.out.println("SQLException: " + sqex.getMessage());
          System.out.println("SQLState: " + sqex.getSQLState());
        }
      } else {
      }
    }
    public void readHandler() {
      if (this.isConnected) {
        try {
        } catch (SQLException sqex) {
          System.out.println("SQLException: " + sqex.getMessage());
          System.out.println("SQLState: " + sqex.getSQLState());
        }
      } else {
      }
    }
*/
}

