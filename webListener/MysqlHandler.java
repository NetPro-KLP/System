package webListener;

import com.nhncorp.mods.socket.io.SocketIOSocket;

import java.lang.Math;

import java.util.Date;
import java.util.Queue;
import java.util.LinkedList;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

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

    public void predictSecurity(String emitTo, String unit) {
      if (this.isConnected) {
        Thread thread = new Thread() {
          public void run() {
            java.sql.Statement st = getSt();

            String query = "SELECT p.endtime"
              + ", SUM(p.totalbytes), SUM(p.danger), SUM(p.warn)"
              + " FROM users u JOIN packets p ON (u.ip = p.source_ip"
              + " OR u.ip = p.destination_ip) AND (u.status = 0) GROUP BY "
              + "p.endtime ORDER BY p.endtime DESC";

            int totalbytes = 0;
            int totaldanger = 0;
            int totalwarn = 0;

            try {
                JsonObject reply = new JsonObject();
                ResultSet rs = null;

                rs = st.executeQuery(query);

                if(st.execute(query))
                  rs = st.getResultSet();

                Queue<Integer> bytesQueue = new LinkedList<Integer>();
                Queue<Integer> dangerQueue = new LinkedList<Integer>();
                Queue<Integer> warnQueue = new LinkedList<Integer>();

                boolean flag = true;
                int startDay = 0;
                int startHour = 0;
                int startMin = 0;
                int startSec = 0;
                int startTime = 0;
                int curr = 0;
                int currbytes = 0;
                int currdanger = 0;
                int currwarn = 0;
                double bytesVariance = 0;
                double dangerVariance = 0;
                double warnVariance = 0;
                String endtime = null;

                while(rs.next()) {
                  endtime = rs.getString(1);
                  int bytes = rs.getInt(2);
                  int danger = rs.getInt(3);
                  int warn = rs.getInt(4);

                  endtime = endtime.substring(0,19);
                  int day = Integer.parseInt(endtime.substring(8,10));

                  if (flag) {
                    flag = !flag;
                    startDay = day;
                    startHour = Integer.parseInt(endtime.substring(11,13));
                    startMin = Integer.parseInt(endtime.substring(14,16));
                    startSec = Integer.parseInt(endtime.substring(17,19));
                    startTime = startHour * 3600 + startMin * 60 + startSec;
                  }

                  if (unit.equals("tomorrow")) {
                    if (startDay != day) {
                      curr = curr + 1;
                      if (curr == 1) {
                        if (startTime == 0)
                          startTime = 5;

                        double weight = (double)86400 / (double)startTime;
                        totalbytes = (int)((double)totalbytes * weight);
                        totaldanger = (int)((double)totaldanger * weight);
                        totalwarn = (int)((double)totalwarn * weight);

                        bytesQueue.offer(totalbytes);
                        dangerQueue.offer(totaldanger);
                        warnQueue.offer(totalwarn);
                      } else {
                        bytesQueue.offer(totalbytes - currbytes);
                        dangerQueue.offer(totaldanger - currdanger);
                        warnQueue.offer(totalwarn - currwarn);
                      }

                      if (curr >= 9) break;

                      currbytes = totalbytes;
                      currdanger = totaldanger;
                      currwarn = totalwarn;
                      startDay = day;
                    }
                  } else if (unit.equals("week")) {
                  }

                  totalbytes = totalbytes + bytes;
                  totaldanger = totaldanger + danger;
                  totalwarn = totalwarn + warn;
                }

                if (curr == 0) {
                  curr = 1;
                  if (startTime == 0)
                    startTime = 5;

                  double weight = (double)86400 / (double)startTime;
                  totalbytes = (int)((double)totalbytes * weight);
                  totaldanger = (int)((double)totaldanger * weight);
                  totalwarn = (int)((double)totalwarn * weight);

                  bytesQueue.offer(totalbytes);
                  dangerQueue.offer(totaldanger);
                  warnQueue.offer(totalwarn);
                } else if (curr <= 8) {
                  curr = curr + 1;

                  startHour = Integer.parseInt(endtime.substring(11,13));
                  startMin = Integer.parseInt(endtime.substring(14,16));
                  startSec = Integer.parseInt(endtime.substring(17,19));
                  startTime = startHour * 3600 + startMin * 60 + startSec;

                  if (startTime == 86400)
                    startTime = 86395;

                  double weight =(int)((double)86400 / ((double)86400 -
                        (double)startTime));
                  totalbytes = currbytes + (int)((double)(totalbytes - currbytes)
                      * weight);
                  totaldanger = currdanger + (int)((double)(totaldanger -
                        currdanger) * weight);
                  totalwarn = currwarn + (int)((double)(totalwarn - currwarn)
                      * weight);

                  bytesQueue.offer(totalbytes - currbytes);
                  dangerQueue.offer(totalbytes - currdanger);
                  warnQueue.offer(totalwarn - currwarn);
                }

                double bytesAvr = (double)totalbytes/(double)curr;
                double warnAvr = (double)totalwarn/(double)curr;
                double dangerAvr = (double)totaldanger/(double)curr;

                reply.putNumber("dangerAvr", dangerAvr);
                reply.putNumber("warnAvr", warnAvr);
                reply.putNumber("bytesAvr", bytesAvr);

                int divisor = bytesQueue.size();
                double dangerStandardDeviation = 0;
                double warnStandardDeviation = 0;
                double bytesStandardDeviation = 0;

                while (bytesQueue.peek() != null) {
                  bytesVariance = bytesVariance +
                    Math.pow((double)bytesQueue.poll() - bytesAvr, 2);
                }

                bytesVariance = bytesVariance / (double)divisor;
                bytesStandardDeviation = Math.pow(bytesVariance, 0.5);

                divisor = warnQueue.size();

                while (warnQueue.peek() != null) {
                  warnVariance = warnVariance +
                    Math.pow((double)warnQueue.poll() - warnAvr, 2);
                }

                warnVariance = warnVariance / (double)divisor;
                warnStandardDeviation = Math.pow(warnVariance, 0.5);

                divisor = dangerQueue.size();

                while (dangerQueue.peek() != null) {
                  dangerVariance = dangerVariance +
                    Math.pow((double)dangerQueue.poll() - dangerAvr, 2);
                }

                dangerVariance = dangerVariance / (double)divisor;
                dangerStandardDeviation = Math.pow(dangerVariance, 0.5);

                reply.putNumber("dangerVariance", dangerVariance);
                reply.putNumber("warnVariance", warnVariance);
                reply.putNumber("bytesVariance", bytesVariance);

                reply.putNumber("dangerStandardDeviation",
                    dangerStandardDeviation);
                reply.putNumber("warnStandardDeviation",
                    warnStandardDeviation);
                reply.putNumber("bytesStandardDeviation",
                    bytesStandardDeviation);

                double dangerLow = dangerAvr - (1.96 * dangerStandardDeviation);
                double dangerHigh = dangerAvr + (1.96 *
                    dangerStandardDeviation);

                double warnLow = warnAvr - (1.96 * warnStandardDeviation);
                double warnHigh = warnAvr + (1.96 * warnStandardDeviation);

                double bytesLow = bytesAvr - (1.96 * bytesStandardDeviation);
                double bytesHigh = bytesAvr + (1.96 * bytesStandardDeviation);

                reply.putNumber("dangerLow", dangerLow);
                reply.putNumber("dangerHigh", dangerHigh);
                reply.putNumber("warnLow", warnLow);
                reply.putNumber("warnHigh", warnHigh);
                reply.putNumber("bytesLow", bytesLow);
                reply.putNumber("bytesHigh", bytesHigh);
                reply.putNumber("reliability", 95);

                reply.putNumber("code", 200);
                socket.emit(emitTo, reply);
            } catch (SQLException sqex) {
              System.out.println("SQLException: " + sqex.getMessage());
              System.out.println("SQLState: " + sqex.getSQLState());
            }
          }
        };

        thread.start();
      } else {
        resToWeb(emitTo, "400", "Database connection failed");
      }
    }

    public void realtimeOn(String emitTo) {

      this.isRealtime = true;

      if (this.isConnected) {

        Thread thread = new Thread() {
          public void run() {
            java.sql.Statement st = getSt();

            String trafficQuery = "SELECT u.idx, u.ip, u.connectedAt, u.status, p.starttime, p.endtime"
              + ", SUM(p.totalbytes), SUM(p.danger), SUM(p.warn)"
              + " FROM users u JOIN packets p ON u.ip = p.source_ip"
              + " OR u.ip = p.destination_ip GROUP BY u.idx, p.endtime"
              + " ORDER BY u.idx, p.endtime DESC";

            try {
              while(isRealtime) {

                JsonObject reply = new JsonObject();
                JsonArray trafficArray = new JsonArray();
                ResultSet rs = null;

                rs = st.executeQuery(trafficQuery);

                if(st.execute(trafficQuery))
                  rs = st.getResultSet();

                int cnt = 0;
                int preIdx = -1;
                //int divisor = 0;

                while(rs.next()) {
                  JsonObject trafficObject = null;

                  int idx = rs.getInt(1);
                  long ip = rs.getLong(2);
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
                    //divisor = traffic / 6;
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

                    trafficObject = new JsonObject().putNumber("ip", ip);
                    trafficObject.putString("connectedAt", connectedAt);
                    trafficObject.putNumber("status", status);
                    trafficObject.putString("starttime", starttime);
                    trafficObject.putString("endtime", endtime);
                    trafficObject.putNumber("danger", danger);
                    trafficObject.putNumber("warn", warn);
                    trafficObject.putNumber("trafficPercentage", traffic/* /
                        divisor*/);
                    trafficArray.addObject(trafficObject);
                  }
                }

                reply.putArray(Integer.toString(preIdx), trafficArray);

                reply.putNumber("code", 200);
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

    public void tcpudp(String emitTo, String code, String unit) {

      if (this.isConnected) {
        Thread thread = new Thread() {
          public void run() {
            try {
              java.sql.Statement st = getSt();

              String barTcpQuery = null;
              String barUdpQuery = null;

              if (code.equals("traffic")) {
                barTcpQuery = "SELECT p.starttime, p.endtime, "
                  + "SUM(p.totalbytes), SUM(p.danger), SUM(p.warn) FROM packets p"
                  + " WHERE p.tcpudp = 0 GROUP BY p.endtime ORDER BY p.endtime DESC";

                barUdpQuery = "SELECT p.starttime, p.endtime, "
                  + "SUM(p.totalbytes), SUM(p.danger), SUM(p.warn) FROM packets p"
                  + " WHERE p.tcpudp = 1 GROUP BY p.endtime ORDER BY p.endtime DESC";
              } else if (code.equals("user")) {
                barTcpQuery = "SELECT u.idx, p.starttime, p.endtime, "
                  + "SUM(p.totalbytes), SUM(p.danger), SUM(p.warn) FROM packets"
                  + " p JOIN users u ON (u.ip = p.source_ip OR u.ip = p.destination_ip)"
                  + " AND (p.tcpudp = 0) GROUP BY u.idx, p.endtime ORDER BY "
                  + "u.idx, p.endtime DESC";

                barUdpQuery = "SELECT u.idx, p.starttime, p.endtime, "
                  + "SUM(p.totalbytes), SUM(p.danger), SUM(p.warn) FROM packets"
                  + " p JOIN users u ON (u.ip = p.source_ip OR u.ip = p.destination_ip)"
                  + " AND (p.tcpudp = 1) GROUP BY u.idx, p.endtime ORDER BY "
                  + "u.idx, p.endtime DESC";
              }

              JsonObject reply = new JsonObject();
              ResultSet rs = null;

              rs = st.executeQuery(barTcpQuery);

              if(st.execute(barTcpQuery))
                rs = st.getResultSet();

              String preTime = "init";
              double totalbytesEachTime = 0;
              int dangerEachTime = 0;
              int warnEachTime = 0;
              String preDate = null;

              JsonArray jsonArray = new JsonArray();
              JsonObject jsonGroup = new JsonObject();
              JsonObject jsonUser = new JsonObject();

              int preIdx = -1;

              while (rs.next()) {
                JsonObject tcpObject = new JsonObject();

                int idx = 0;
                String starttime = null;
                String endtime = null;
                double totalbytes = 0;
                int danger = 0;
                int warn = 0;

                if (code.equals("traffic")) {
                  starttime = rs.getString(1);
                  endtime = rs.getString(2);
                  totalbytes = (double)rs.getFloat(3);
                  danger = rs.getInt(4);
                  warn = rs.getInt(5);
                } else if (code.equals("user")) {
                  idx = rs.getInt(1);
                  starttime = rs.getString(2);
                  endtime = rs.getString(3);
                  totalbytes = (double)rs.getFloat(4);
                  danger = rs.getInt(5);
                  warn = rs.getInt(6);
                }

                starttime = starttime.substring(0,19);
                endtime = endtime.substring(0,19);

                String curtime = null;
                String curdate = null;

                if (unit.equals("hour")) {
                  curtime = endtime.substring(11,13) + ":00:00";
                  curdate = endtime.substring(0,11);
                } else if (unit.equals("min")) {
                  curtime = endtime.substring(14,16) + ":00";
                  curdate = endtime.substring(0,14);
                } else if (unit.equals("sec")) {
                  curtime = endtime.substring(17,19);
                  curdate = endtime.substring(0,17);
                } else if (unit.equals("day")) {
                  curtime = endtime.substring(8,10) + " 00:00:00";
                  curdate = endtime.substring(0,8);
                } else if (unit.equals("mon")) {
                  curtime = endtime.substring(5,7) + "-01 00:00:00";
                  curdate = endtime.substring(0,5);
                } else if (unit.equals("year")) {
                  curtime = endtime.substring(0,4) + "-01-01 00:00:00";
                  curdate = "";
                }

                if (preTime.equals("init")) {
                  preTime = curtime;
                  preDate = curdate;
                  preIdx = idx;
                }

                if (!preTime.equals(curtime) || !preDate.equals(curdate)) {
                  tcpObject = new JsonObject().putNumber("totalbytes",
                      totalbytesEachTime);
                  jsonArray.addObject(tcpObject);

                  tcpObject = new JsonObject().putNumber("totaldanger",
                      dangerEachTime);
                  jsonArray.addObject(tcpObject);

                  tcpObject = new JsonObject().putNumber("totalwarn",
                      warnEachTime);
                  jsonArray.addObject(tcpObject);

                  DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd "
                      + "HH:mm:ss");
                  Date date = dateFormat.parse(preDate + preTime);
                  long time = date.getTime() / 1000;

                  jsonGroup.putArray(Long.toString(time), jsonArray);

                  if (code.equals("user") && preIdx != idx) {
                    jsonUser.putObject(Integer.toString(preIdx), jsonGroup);
                    jsonGroup = new JsonObject();
                    preIdx = idx;
                  }

                  jsonArray = new JsonArray();
                  totalbytesEachTime = 0;
                  dangerEachTime = 0;
                  warnEachTime = 0;
                  preTime = curtime;
                  preDate = curdate;
                }

                totalbytesEachTime = totalbytesEachTime + totalbytes;
                dangerEachTime = dangerEachTime + danger;
                warnEachTime = warnEachTime + warn;
              }

              if (!preTime.equals("init")) {
                JsonObject insertObject = new JsonObject().putNumber("totalbytes", 
                    totalbytesEachTime);
                jsonArray.addObject(insertObject);

                insertObject = new JsonObject().putNumber("totaldanger",
                    dangerEachTime);
                jsonArray.addObject(insertObject);

                insertObject = new JsonObject().putNumber("totalwarn",
                    warnEachTime);
                jsonArray.addObject(insertObject);

                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd "
                     + "HH:mm:ss");
                Date date = dateFormat.parse(preDate + preTime);
                long time = date.getTime() / 1000;

                jsonGroup.putArray(Long.toString(time), jsonArray);
                if (code.equals("traffic"))
                  reply.putObject("tcpTraffic", jsonGroup);
                else if (code.equals("user")) {
                  jsonUser.putObject(Integer.toString(preIdx), jsonGroup);
                  reply. putObject("tcpTraffic", jsonUser);
                }
              }

              rs = st.executeQuery(barUdpQuery);

              if(st.execute(barUdpQuery))
                rs = st.getResultSet();

              preTime = "init";
              totalbytesEachTime = 0;
              dangerEachTime = 0;
              warnEachTime = 0;
              preDate = null;

              jsonArray = new JsonArray();
              jsonGroup = new JsonObject();
              jsonUser = new JsonObject();

              while (rs.next()) {
                JsonObject udpObject = new JsonObject();

                int idx = 0;
                String starttime = null;
                String endtime = null;
                double totalbytes = 0;
                int danger = 0;
                int warn = 0;

                if (code.equals("traffic")) {
                  starttime = rs.getString(1);
                  endtime = rs.getString(2);
                  totalbytes = (double)rs.getFloat(3);
                  danger = rs.getInt(4);
                  warn = rs.getInt(5);
                } else if (code.equals("user")) {
                  idx = rs.getInt(1);
                  starttime = rs.getString(2);
                  endtime = rs.getString(3);
                  totalbytes = (double)rs.getFloat(4);
                  danger = rs.getInt(5);
                  warn = rs.getInt(6);
                }

                starttime = starttime.substring(0,19);
                endtime = endtime.substring(0,19);

                String curtime = null;
                String curdate = null;

                if (unit.equals("hour")) {
                  curtime = endtime.substring(11,13) + ":00:00";
                  curdate = endtime.substring(0,11);
                } else if (unit.equals("min")) {
                  curtime = endtime.substring(14,16) + ":00";
                  curdate = endtime.substring(0,14);
                } else if (unit.equals("sec")) {
                  curtime = endtime.substring(17,19);
                  curdate = endtime.substring(0,17);
                } else if (unit.equals("day")) {
                  curtime = endtime.substring(8,10) + " 00:00:00";
                  curdate = endtime.substring(0,8);
                } else if (unit.equals("mon")) {
                  curtime = endtime.substring(5,7) + "-01 00:00:00";
                  curdate = endtime.substring(0,5);
                } else if (unit.equals("year")) {
                  curtime = endtime.substring(0,4) + "-01-01 00:00:00";
                  curdate = "";
                }

                if (preTime.equals("init")) {
                  preTime = curtime;
                  preDate = curdate;
                  preIdx = idx;
                }

                if (!preTime.equals(curtime) || !preDate.equals(curdate)) {
                  udpObject = new JsonObject().putNumber("totalbytes",
                      totalbytesEachTime);
                  jsonArray.addObject(udpObject);

                  udpObject = new JsonObject().putNumber("totaldanger",
                      dangerEachTime);
                  jsonArray.addObject(udpObject);

                  udpObject = new JsonObject().putNumber("totalwarn",
                      warnEachTime);
                  jsonArray.addObject(udpObject);

                  DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd "
                      + "HH:mm:ss");
                  Date date = dateFormat.parse(preDate + preTime);
                  long time = date.getTime() / 1000;

                  jsonGroup.putArray(Long.toString(time), jsonArray);

                  if (code.equals("user") && preIdx != idx) {
                    jsonUser.putObject(Integer.toString(preIdx), jsonGroup);
                    jsonGroup = new JsonObject();
                    preIdx = idx;
                  }

                  jsonArray = new JsonArray();
                  totalbytesEachTime = 0;
                  dangerEachTime = 0;
                  warnEachTime = 0;
                  preTime = curtime;
                  preDate = curdate;
                }

                totalbytesEachTime = totalbytesEachTime + totalbytes;
                dangerEachTime = dangerEachTime + danger;
                warnEachTime = warnEachTime + warn;
              }

              if (!preTime.equals("init")) {
                JsonObject insertObject = new JsonObject().putNumber("totalbytes", 
                    totalbytesEachTime);
                jsonArray.addObject(insertObject);

                insertObject = new JsonObject().putNumber("totaldanger",
                    dangerEachTime);
                jsonArray.addObject(insertObject);

                insertObject = new JsonObject().putNumber("totalwarn",
                    warnEachTime);
                jsonArray.addObject(insertObject);

                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd "
                    + "HH:mm:ss");
                Date date = dateFormat.parse(preDate + preTime);
                long time = date.getTime() / 1000;

                jsonGroup.putArray(Long.toString(time), jsonArray);

                if (code.equals("traffic"))
                  reply.putObject("udpTraffic", jsonGroup);
                else if (code.equals("user")) {
                  jsonUser.putObject(Integer.toString(preIdx), jsonGroup);
                  reply.putObject("udpTraffic", jsonUser);
                }
              }

              reply.putNumber("code", 200);
              socket.emit(emitTo, reply);

            } catch (SQLException sqex) {
              System.out.println("SQLException: " + sqex.getMessage());
              System.out.println("SQLState: " + sqex.getSQLState());
              resToWeb(emitTo, "400", "tcpudp: somethings were error");
            } catch (ParseException e) {
              e.printStackTrace();
            }
          }
        };

        thread.start();
      } else {
        resToWeb(emitTo, "400", "Database connection failed");
      }
    }

    public void barStatistics(String emitTo, String unit) {
      if (this.isConnected) {
        Thread thread = new Thread() {
          public void run() {
            try {
              java.sql.Statement st = getSt();
              ResultSet rs = null;
              JsonObject reply = new JsonObject();

              String packetsQuery = null;
              String backupQuery = null;

              if (unit.equals("danger")) {
                packetsQuery = "SELECT SUM(danger), endtime FROM packets WHERE 1";
                backupQuery = "SELECT SUM(danger), endtime FROM backup_packets GROUP BY "
                  + "endtime ORDER BY endtime DESC";
              } else if (unit.equals("warn")) {
                packetsQuery = "SELECT SUM(warn), endtime FROM packets WHERE 1";
                backupQuery = "SELECT SUM(warn), endtime FROM backup_packets GROUP BY "
                  + "endtime ORDER BY endtime DESC";
              } else if (unit.equals("traffic")) {
                packetsQuery = "SELECT SUM(totalbytes), endtime FROM packets WHERE 1";
                backupQuery = "SELECT SUM(totalbytes), endtime FROM backup_packets GROUP BY "
                  + "endtime ORDER BY endtime DESC";
              } else if (unit.equals("dangerWarn")) {
                packetsQuery = "SELECT SUM(danger), SUM(warn), endtime FROM packets WHERE 1";
                backupQuery = "SELECT SUM(danger), SUM(warn), endtime FROM backup_packets GROUP BY "
                  + "endtime ORDER BY endtime DESC";
              }

              rs = st.executeQuery(packetsQuery);

              if (st.execute(packetsQuery))
                rs = st.getResultSet();

              while (rs.next()) {
                int eachUnit = 0;
                double traffic = 0;

                if (unit.equals("traffic"))
                  traffic = (double)rs.getFloat(1);
                else if (unit.equals("dangerWarn"))
                  eachUnit = rs.getInt(1) + rs.getInt(2);
                else
                  eachUnit = rs.getInt(1);

                String endtime = null;

                if (unit.equals("dangerWarn"))
                  endtime = (rs.getString(3)).substring(0,10);
                else
                  endtime = (rs.getString(2)).substring(0,10);

                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date date = dateFormat.parse(endtime);
                long time = date.getTime() / 1000;

                if (unit.equals("traffic"))
                  reply.putNumber(Long.toString(time), traffic);
                else
                  reply.putNumber(Long.toString(time), eachUnit);
              }

              rs = st.executeQuery(backupQuery);

              if (st.execute(backupQuery))
                rs = st.getResultSet();

              int i = -1;
              int totalUnit = 0;
              double totalTraffic = 0;
              String day = null;

              while (rs.next()) {
                if (i == 6)
                  break;

                double traffic = 0;
                int eachUnit = 0;

                if (unit.equals("traffic"))
                  traffic = (double)rs.getFloat(1);
                else if (unit.equals("dangerWarn"))
                  eachUnit = rs.getInt(1) + rs.getInt(2);
                else
                  eachUnit = rs.getInt(1);

                String endtime = null;

                if (unit.equals("dangerWarn"))
                  endtime = (rs.getString(3)).substring(0,10);
                else
                  endtime = (rs.getString(2)).substring(0,10);
                
                if (i == -1) {
                  day = endtime;
                  i = 0;
                }

                if (!day.equals(endtime)) {
                  DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                  Date date = dateFormat.parse(day);
                  long time = date.getTime() / 1000;

                  if (unit.equals("traffic"))
                    reply.putNumber(Long.toString(time), totalTraffic);
                  else
                    reply.putNumber(Long.toString(time), totalUnit);

                  day = endtime;
                  totalUnit = 0;
                  totalTraffic = 0;
                  i = i + 1;
                }

                totalUnit = totalUnit + eachUnit;
                totalTraffic = totalTraffic + traffic;
              }

              reply.putNumber("code", 200);
              socket.emit(emitTo, reply);
            } catch (SQLException sqex) {
              System.out.println("SQLException: " + sqex.getMessage());
              System.out.println("SQLState: " + sqex.getSQLState());
              resToWeb(emitTo, "400", "bar statistics: somethings were error");
            } catch (ParseException e) {
              e.printStackTrace();
            }
          }
        };

        thread.start();
      } else {
        resToWeb(emitTo, "400", "Database connection failed");
      }
    }

    public void protocolStatistics(String emitTo, String code) {
      if (this.isConnected) {
        Thread thread = new Thread() {
          public void run() {
            try {
              java.sql.Statement st = getSt();
              ResultSet rs = null;
              JsonObject reply = new JsonObject();

              String query = null;

              if (code.equals("traffic")) {
                query = "SELECT COUNT(p.source_port), pr.name FROM packets p JOIN protocol pr "
                  + "ON p.source_port = pr.port GROUP BY "
                  + "pr.name ORDER BY COUNT(p.source_port) DESC";
              } else if (code.equals("user")) {
                query = "SELECT u.idx, p.source_port FROM packets p JOIN"
                  + " users u ON u.ip = p.source_ip OR u.ip = p.destination_ip "
                  + "GROUP BY u.idx, p.source_port ORDER BY u.idx DESC";
              }

              rs = st.executeQuery(query);

              if(st.execute(query))
                rs = st.getResultSet();

              JsonObject userObject = new JsonObject();
              int i = 0;

              while(rs.next()) {
                if (i++ > 3)
                  break;
                int count = 0;
                String protocol = null;

                if (code.equals("traffic")) {
                  count = rs.getInt(1);
                  protocol = rs.getString(2);
                }
                else if (code.equals("user")) {
                  count = rs.getInt(1);
                  protocol = rs.getString(2);
                }

                reply.putNumber(protocol, count);
              }

              reply.putNumber("code", 200);
              socket.emit(emitTo, reply);
            } catch (SQLException sqex) {
              System.out.println("SQLException: " + sqex.getMessage());
              System.out.println("SQLState: " + sqex.getSQLState());
              resToWeb(emitTo, "400", "protocol statistics: somethings were error");
            }
          }
        };

        thread.start();
      } else {
        resToWeb(emitTo, "400", "Database connection failed");
      }
    }

    public void inoutBound(String emitTo, String code, String unit) {
      if (this.isConnected) {
        Thread thread = new Thread() {
          public void run() {
            try {
              java.sql.Statement st = getSt();
              ResultSet rs = null;
              JsonObject reply = new JsonObject();

              String outboundQuery = null;
              String inboundQuery = null;

              if (code.equals("traffic")) {
                outboundQuery = "SELECT SUM(p.totalbytes), SUM(p.warn), "
                  + "SUM(p.danger), p.starttime, p.endtime FROM packets p "
                  + "JOIN users u ON u.ip = p.source_ip GROUP BY p.endtime "
                  + "ORDER BY p.endtime DESC";

                inboundQuery = "SELECT SUM(p.totalbytes), SUM(p.warn), "
                  + "SUM(p.danger), p.starttime, p.endtime FROM packets p "
                  + "JOIN users u ON u.ip = p.destination_ip GROUP BY p.endtime "
                  + "ORDER BY p.endtime DESC";
              } else if (code.equals("user")) {
                outboundQuery = "SELECT u.idx, SUM(p.totalbytes), SUM(p.warn), "
                  + "SUM(p.danger), p.starttime, p.endtime FROM packets p "
                  + "JOIN users u ON u.ip = p.source_ip GROUP BY u.idx, p.endtime "
                  + "ORDER BY u.idx, p.endtime DESC";

                inboundQuery = "SELECT u.idx, SUM(p.totalbytes), SUM(p.warn), "
                  + "SUM(p.danger), p.starttime, p.endtime FROM packets p "
                  + "JOIN users u ON u.ip = p.destination_ip GROUP BY u.idx, p.endtime "
                  + "ORDER BY u.idx, p.endtime DESC";
              }

              rs = st.executeQuery(inboundQuery);

              if(st.execute(inboundQuery))
                rs = st.getResultSet();

              String preTime = "init";
              double totalbytesEachTime = 0;
              int dangerEachTime = 0;
              int warnEachTime = 0;
              int preIdx = -1;
              String preDate = null;

              JsonArray jsonArray = new JsonArray();
              JsonObject jsonGroup = new JsonObject();
              JsonObject jsonUser = new JsonObject();

              while (rs.next()) {
                JsonObject inObject = new JsonObject();

                int idx = 0;
                double totalbytes = 0;
                int warn = 0;
                int danger = 0;
                String starttime = null;
                String endtime = null;

                if (code.equals("traffic")) {
                  totalbytes = (double)rs.getFloat(1);
                  warn = rs.getInt(2);
                  danger = rs.getInt(3);
                  starttime = rs.getString(4);
                  endtime = rs.getString(5);
                } else if (code.equals("user")) {
                  idx = rs.getInt(1);
                  totalbytes = (double)rs.getFloat(2);
                  warn = rs.getInt(3);
                  danger = rs.getInt(4);
                  starttime = rs.getString(5);
                  endtime = rs.getString(6);
                }

                starttime = starttime.substring(0,19);
                endtime = endtime.substring(0,19);

                String curtime = null;
                String curdate = null;

                if (unit.equals("hour")) {
                  curtime = endtime.substring(11,13) + ":00:00";
                  curdate = endtime.substring(0,11);
                } else if (unit.equals("min")) {
                  curtime = endtime.substring(14,16) + ":00";
                  curdate = endtime.substring(0,14);
                } else if (unit.equals("sec")) {
                  curtime = endtime.substring(17,19);
                  curdate = endtime.substring(0,17);
                } else if (unit.equals("day")) {
                  curtime = endtime.substring(8,10) + " 00:00:00";
                  curdate = endtime.substring(0,8);
                } else if (unit.equals("mon")) {
                  curtime = endtime.substring(5,7) + "-01 00:00:00";
                  curdate = endtime.substring(0,5);
                } else if (unit.equals("year")) {
                  curtime = endtime.substring(0,4) + "-01-01 00:00:00";
                  curdate = "";
                }

                if (preTime.equals("init")) {
                  preTime = curtime;
                  preDate = curdate;
                  preIdx = idx;
                }

                if (!preTime.equals(curtime) || !preDate.equals(curdate)) {
                  inObject = new JsonObject().putNumber("totalbytes",
                      totalbytesEachTime);
                  jsonArray.addObject(inObject);

                  inObject = new JsonObject().putNumber("totaldanger",
                      dangerEachTime);
                  jsonArray.addObject(inObject);

                  inObject = new JsonObject().putNumber("totalwarn",
                      warnEachTime);
                  jsonArray.addObject(inObject);

                  DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd "
                      + "HH:mm:ss");
                  Date date = dateFormat.parse(preDate + preTime);
                  long time = date.getTime() / 1000;

                  jsonGroup.putArray(Long.toString(time), jsonArray);

                  if (code.equals("user") && preIdx != idx) {
                    jsonUser.putObject(Integer.toString(preIdx), jsonGroup);
                    jsonGroup = new JsonObject();
                    preIdx = idx;
                  }

                  jsonArray = new JsonArray();
                  totalbytesEachTime = 0;
                  dangerEachTime = 0;
                  warnEachTime = 0;
                  preTime = curtime;
                  preDate = curdate;
                }

                totalbytesEachTime = totalbytesEachTime + totalbytes;
                dangerEachTime = dangerEachTime + danger;
                warnEachTime = warnEachTime + warn;
              }

              if (!preTime.equals("init")) {
                JsonObject insertObject = new
                  JsonObject().putNumber("totalbytes", totalbytesEachTime);
                jsonArray.addObject(insertObject);

                insertObject = new JsonObject().putNumber("totaldanger",
                    dangerEachTime);
                jsonArray.addObject(insertObject);

                insertObject = new JsonObject().putNumber("totalwarn",
                    warnEachTime);
                jsonArray.addObject(insertObject);

                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd "
                    + "HH:mm:ss");
                Date date = dateFormat.parse(preDate + preTime);
                long time = date.getTime() / 1000;

                jsonGroup.putArray(Long.toString(time), jsonArray);
                if (code.equals("traffic"))
                  reply.putObject("inboundTraffic", jsonGroup);
                else if (code.equals("user")) {
                  jsonUser.putObject(Integer.toString(preIdx), jsonGroup);
                  reply.putObject("inboundTraffic", jsonUser);
                }
              }

              rs = st.executeQuery(outboundQuery);

              if(st.execute(outboundQuery))
                rs = st.getResultSet();

              preTime = "init";
              totalbytesEachTime = 0;
              dangerEachTime = 0;
              warnEachTime = 0;
              preDate = null;

              jsonArray = new JsonArray();
              jsonGroup = new JsonObject();
              jsonUser = new JsonObject();

              while (rs.next()) {
                JsonObject outObject = new JsonObject();

                int idx = 0;
                double totalbytes = 0;
                int warn = 0;
                int danger = 0;
                String starttime = null;
                String endtime = null;

                if (code.equals("traffic")) {
                  totalbytes = (double)rs.getFloat(1);
                  warn = rs.getInt(2);
                  danger = rs.getInt(3);
                  starttime = rs.getString(4);
                  endtime = rs.getString(5);
                } else if (code.equals("user")) {
                  idx = rs.getInt(1);
                  totalbytes = (double)rs.getFloat(2);
                  warn = rs.getInt(3);
                  danger = rs.getInt(4);
                  starttime = rs.getString(5);
                  endtime = rs.getString(6);
                }

                starttime = starttime.substring(0,19);
                endtime = endtime.substring(0,19);

                String curtime = null;
                String curdate = null;

                if (unit.equals("hour")) {
                  curtime = endtime.substring(11,13) + ":00:00";
                  curdate = endtime.substring(0,11);
                } else if (unit.equals("min")) {
                  curtime = endtime.substring(14,16) + ":00";
                  curdate = endtime.substring(0,14);
                } else if (unit.equals("sec")) {
                  curtime = endtime.substring(17,19);
                  curdate = endtime.substring(0,17);
                } else if (unit.equals("day")) {
                  curtime = endtime.substring(8,10) + " 00:00:00";
                  curdate = endtime.substring(0,8);
                } else if (unit.equals("mon")) {
                  curtime = endtime.substring(5,7) + "-01 00:00:00";
                  curdate = endtime.substring(0,5);
                } else if (unit.equals("year")) {
                  curtime = endtime.substring(0,4) + "-01-01 00:00:00";
                  curdate = "";
                }

                if (preTime.equals("init")) {
                  preTime = curtime;
                  preDate = curdate;
                  preIdx = idx;
                }

                if (!preTime.equals(curtime) || !preDate.equals(curdate)) {
                  outObject = new JsonObject().putNumber("totalbytes",
                      -totalbytesEachTime);
                  jsonArray.addObject(outObject);

                  outObject = new JsonObject().putNumber("totaldanger",
                      -dangerEachTime);
                  jsonArray.addObject(outObject);

                  outObject = new JsonObject().putNumber("totalwarn",
                      -warnEachTime);
                  jsonArray.addObject(outObject);

                  DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd "
                      + "HH:mm:ss");
                  Date date = dateFormat.parse(preDate + preTime);
                  long time = date.getTime() / 1000;

                  jsonGroup.putArray(Long.toString(time), jsonArray);

                  if (code.equals("user") && preIdx != idx) {
                    jsonUser.putObject(Integer.toString(preIdx), jsonGroup);
                    jsonGroup = new JsonObject();
                    preIdx = idx;
                  }

                  jsonArray = new JsonArray();
                  totalbytesEachTime = 0;
                  dangerEachTime = 0;
                  warnEachTime = 0;
                  preTime = curtime;
                  preDate = curdate;
                }

                totalbytesEachTime = totalbytesEachTime + totalbytes;
                dangerEachTime = dangerEachTime + danger;
                warnEachTime = warnEachTime + warn;
              }

              if (!preTime.equals("init")) {
                JsonObject insertObject = new
                  JsonObject().putNumber("totalbytes", -totalbytesEachTime);
                jsonArray.addObject(insertObject);

                insertObject = new JsonObject().putNumber("totaldanger",
                    -dangerEachTime);
                jsonArray.addObject(insertObject);

                insertObject = new JsonObject().putNumber("totalwarn",
                    -warnEachTime);
                jsonArray.addObject(insertObject);

                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd " +
                    "HH:mm:ss");
                Date date = dateFormat.parse(preDate + preTime);
                long time = date.getTime() / 1000;

                jsonGroup.putArray(Long.toString(time), jsonArray);
                
                if (code.equals("traffic"))
                  reply.putObject("outboundTraffic", jsonGroup);
                else if (code.equals("user")) {
                  jsonUser.putObject(Integer.toString(preIdx), jsonGroup);
                  reply.putObject("outboundTraffic", jsonUser);
                }
              }

              reply.putNumber("code", 200);
              socket.emit(emitTo, reply);
            } catch (SQLException sqex) {
              System.out.println("SQLException: " + sqex.getMessage());
              System.out.println("SQLState: " + sqex.getSQLState());
              resToWeb(emitTo, "400", "inout bound: somethings were error");
            } catch (ParseException e) {
              e.printStackTrace();
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

