package webListener;

import com.nhncorp.mods.socket.io.SocketIOSocket;

import java.lang.Math;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.JsonArray;

public class MysqlHandler {

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
      JsonObject reply = null;

      if (this.isConnected) {
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

      this.socket.emit(emitTo, reply);
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
            String trafficQuery = "SELECT u.ip, u.connectedAt, u.status, p.starttime, p.endtime"
              + ", SUM(p.totalbytes), SUM(p.danger), SUM(p.warn)"
              + " FROM users u JOIN packets p ON u.ip = p.source_ip"
              + " OR u.ip = p.destination_ip GROUP BY u.ip, p.starttime, p.endtime"
              + " ORDER BY u.ip, p.starttime, p.endtime DESC";

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
                String preIp = null;

                while(rs.next()) {
                  JsonObject trafficObject = null;

                  String ip = rs.getString(1);
                  String connectedAt = rs.getString(2);
                  int status = rs.getInt(3);
                  String starttime = rs.getString(4);
                  String endtime = rs.getString(5);
                  int traffic = rs.getInt(6);
                  int danger = rs.getInt(7);
                  int warn = rs.getInt(8);

                  if (preIp == null) {
                    preIp = ip;
                    reply.putNumber("code", 200);
                  }

                  if (preIp != ip) {
                    cnt = 1;
                    reply.putArray(preIp, trafficArray);
                    trafficArray = null;
                    preIp = ip;
                  }
                  else
                    cnt++;

                  if (cnt <= 20) {
                    trafficObject = new JsonObject().putString("ip", ip);
                    trafficObject.putString("connectedAt", connectedAt);
                    trafficObject.putNumber("status", status);
                    trafficObject.putString("starttime", starttime);
                    trafficObject.putString("endtime", endtime);
                    trafficObject.putNumber("traffic", traffic);
                    trafficObject.putNumber("danger", danger);
                    trafficObject.putNumber("warn", warn);
                    trafficObject.putNumber("trafficPercentage",
                        ((double)traffic / maxTraffic) * 100);
                    trafficArray.addObject(trafficObject);
                  }
                }

                reply.putArray(preIp, trafficArray);

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
                  Thread.sleep(4000);
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

    public void insertLogHandler(String emitTo, String admin_idx,
        String action, String date) {

      if (this.isConnected) {
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
      } else {
        // 에러날 경우
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
