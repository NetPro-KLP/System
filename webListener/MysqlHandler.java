package webListener;

import com.nhncorp.mods.socket.io.SocketIOSocket;

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
  private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd "
      + "HH:mm:ss");
  private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public MysqlHandler(String host, String userId, String pw,
        SocketIOSocket socket) {

      this.isConnected = false;

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

      if (this.isConnected) {
        Thread thread = new Thread() {
          public void run() {
            java.sql.Statement st = getSt();

            String trafficQuery = "SELECT u.idx, u.ip, u.status, p.endtime"
              + ", SUM(p.totalbytes), SUM(p.danger), SUM(p.warn)"
              + " FROM users u JOIN packets p ON u.ip = p.source_ip"
              + " OR u.ip = p.destination_ip GROUP BY u.idx, p.endtime"
              + " ORDER BY u.idx, p.endtime DESC";

            String realtimeQuery = "SELECT endtime, SUM(totalbytes) FROM "
              + "packets GROUP BY endtime ORDER BY endtime DESC";

            try {
                JsonObject reply = new JsonObject();
                JsonArray trafficArray = new JsonArray();
                JsonObject trafficObject = null;
                ResultSet rs = null;

                rs = st.executeQuery(trafficQuery);

                if(st.execute(trafficQuery))
                  rs = st.getResultSet();

                int preIdx = -1;
                int idx = 0;
                long ip = 0;
                long time = 0;
                int status = 0;
                long traffic = 0;
                double totalbytesEachUser = 0;
                double totalbytes = 0;
                int totalWarnEachUser = 0;
                int warn = 0;
                int totalDangerEachUser = 0;
                int danger = 0;
                int cnt = 0;
                String endtime = null;

                while(rs.next()) {
                  idx = rs.getInt(1);
                  ip = rs.getLong(2);
                  status = rs.getInt(3);
                  endtime = (rs.getString(4)).substring(0,19);
                  traffic = rs.getLong(5);
                  danger = rs.getInt(6);
                  warn = rs.getInt(7);

                  if (preIdx == -1) {
                    preIdx = idx;
                  }

                  if (preIdx != idx) {
                    trafficObject = new JsonObject().putNumber("totalbytes",
                        totalbytesEachUser);
                    trafficObject.putNumber("totalwarn", totalWarnEachUser);
                    trafficObject.putNumber("totaldanger",
                        totalDangerEachUser);

                    trafficArray.addObject(trafficObject);
                    reply.putArray(Integer.toString(preIdx), trafficArray);

                    trafficArray = new JsonArray();
                    preIdx = idx;
                    totalbytesEachUser = 0;
                    totalWarnEachUser = 0;
                    totalDangerEachUser = 0;
                    cnt = 0;
                  }

                  cnt = cnt + 1;
                  if (cnt <= 20) {
                    trafficObject = new JsonObject().putNumber("ip", ip);
                    trafficObject.putNumber("status", status);
                    trafficObject.putString("endtime", endtime);
                    trafficObject.putNumber("danger", danger);
                    trafficObject.putNumber("warn", warn);
                    trafficObject.putNumber("trafficPercentage", traffic);
                    trafficArray.addObject(trafficObject);
                  }

                  totalbytesEachUser = totalbytesEachUser + traffic;
                  totalWarnEachUser = totalWarnEachUser + warn;
                  totalDangerEachUser = totalDangerEachUser + danger;
                }

                if (preIdx != -1) {
                  trafficObject = new JsonObject().putNumber("totalbytes",
                    totalbytesEachUser);
                  trafficObject.putNumber("totalwarn", totalWarnEachUser);
                  trafficObject.putNumber("totaldanger",
                    totalDangerEachUser);

                  trafficArray.addObject(trafficObject);
                  reply.putArray(Integer.toString(preIdx), trafficArray);
                }

                rs = st.executeQuery(realtimeQuery);

                if(st.execute(realtimeQuery))
                  rs = st.getResultSet();

                cnt = 0;
                trafficArray = new JsonArray();
                double first = 0;
                Date javatime = new Date();
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH"
                    + ":mm:ss");

                String curTimeString = (df.format(javatime)).substring(0,19);
                Date curTimeDate = df.parse(curTimeString);
                long curTime = curTimeDate.getTime() / 1000;
                long curTime2 = curTime - 240;

                while (rs.next()) {
                  trafficObject = new JsonObject();

                  endtime = (rs.getString(1)).substring(0,19);
                  totalbytes = rs.getDouble(2);
                  double[] data = new double[2];

                  Date date = dateFormat.parse(endtime);
                  time = date.getTime() / 1000;

                  if (first == 0) {
                    first = totalbytes;
                    curTime = time;
                  }

                  if (time <= curTime && time >= curTime2) {}
                  else
                    break;

                  data[0] = (double)cnt;
                  data[1] = totalbytes/100000000 * 2;
                  trafficArray.add(data);
                  cnt = cnt + 1;
                }

                reply.putArray("statistics", trafficArray);
                reply.putNumber("code", 200);
                socket.emit(emitTo, reply);

            } catch (SQLException sqex) {
              System.out.println("SQLException: " + sqex.getMessage());
              System.out.println("SQLState: " + sqex.getSQLState());
              resToWeb(emitTo, "400", "realtimeOn: somethings were error");
            } catch (ParseException e) {
              e.printStackTrace();
            }
          } // Thread: run()
        }; // Thread thread = new Thread() {
        thread.start();
      } else {
        resToWeb(emitTo, "400", "Database connection failed");
      }
    }

    public void dashboard(String emitTo) {
      if (this.isConnected) {
        Thread thread = new Thread() {
          public void run() {
            try {
              java.sql.Statement st = getSt();
              JsonObject reply = new JsonObject();
              Date date = new Date();
              String today = sdf.format(date);
              today = today.substring(0,11) + "00:00:00";

              String todayTcpQuery = "SELECT totalbytes, endtime FROM packets WHERE "
                + "(endtime >= '" + today + "') AND (tcpudp = 0) ORDER BY"
                + " endtime ASC";

              String todayUdpQuery = "SELECT totalbytes, endtime FROM packets "
                + "WHERE (endtime >= '" + today + "') AND (tcpudp = 1) ORDER "
                + "BY endtime ASC";

              date = new Date(date.getTime() - ((long)(1000*60*60*24)));
              String yesterday = sdf.format(date);
              yesterday = yesterday.substring(0,11) + "00:00:00";

              String yesterdayTcpQuery = "SELECT totalbytes, endtime FROM backup_"
                + "packets WHERE (endtime >= '" + yesterday + "') AND ("
                + "tcpudp = 0) ORDER BY endtime ASC";

              String yesterdayUdpQuery = "SELECT totalbytes, endtime FROM backup_"
                + "packets WHERE (endtime >= '" + yesterday + "') AND ("
                + "tcpudp = 1) ORDER BY endtime ASC";

              String thisMonth = today.substring(0,8) + "01 00:00:00";
              String thisMonthBackupTcpQuery = "SELECT totalbytes, "
                + "endtime FROM backup_packets WHERE (endtime >= '"
                + thisMonth + "') AND (tcpudp = 0) ORDER BY endtime ASC";

              String thisMonthBackupUdpQuery = "SELECT totalbytes, "
                + "endtime FROM backup_packets WHERE (endtime >= '"
                + thisMonth + "') AND (tcpudp = 1) ORDER BY endtime ASC";

              String thisMonthTcpQuery = "SELECT SUM(totalbytes), "
                + "endtime FROM packets WHERE (endtime >= '"
                + today + "') AND (tcpudp = 0)";

              String thisMonthUdpQuery = "SELECT SUM(totalbytes), "
                + "endtime FROM packets WHERE (endtime >= '"
                + today + "') AND (tcpudp = 1)";

              ResultSet rs = st.executeQuery(todayTcpQuery);

              if (st.execute(todayTcpQuery))
                rs = st.getResultSet();

              String preDate = null;
              JsonArray jsonArray = new JsonArray();
              double totalbytesEach = 0;
              double totalbytes = 0;
              String endtime = null;
              long time = 0;

              while (rs.next()) {
                totalbytes = rs.getDouble(1);
                endtime = (rs.getString(2)).substring(0,14) + "00:00";

                if (preDate == null)
                  preDate = endtime;

                if (!preDate.equals(endtime)) {
                  date = dateFormat.parse(preDate);
                  time = date.getTime() / 1000;

                  double[] array = new double[2];
                  array[0] = (double)time;
                  array[1] = totalbytesEach;
                  jsonArray.add(array);

                  preDate = endtime;
                  totalbytesEach = 0;
                }

                totalbytesEach = totalbytesEach + totalbytes;
              }

              if (preDate != null) {
                date = dateFormat.parse(preDate);
                time = date.getTime() / 1000;

                double[] array = new double[2];
                array[0] = (double)time;
                array[1] = totalbytesEach;
                jsonArray.add(array);
                reply.putArray("todayTcp", jsonArray);
              }

              rs = st.executeQuery(todayUdpQuery);

              if (st.execute(todayUdpQuery))
                rs = st.getResultSet();

              preDate = null;
              jsonArray = new JsonArray();
              totalbytesEach = 0;
              time = 0;

              while (rs.next()) {
                totalbytes = rs.getDouble(1);
                endtime = (rs.getString(2)).substring(0,14) + "00:00";

                if (preDate == null)
                  preDate = endtime;

                if (!preDate.equals(endtime)) {
                  date = dateFormat.parse(preDate);
                  time = date.getTime() / 1000;

                  double[] array = new double[2];
                  array[0] = (double)time;
                  array[1] = totalbytesEach;
                  jsonArray.add(array);

                  preDate = endtime;
                  totalbytesEach = 0;
                }

                totalbytesEach = totalbytesEach + totalbytes;
              }

              if (preDate != null) {
                date = dateFormat.parse(preDate);
                time = date.getTime() / 1000;

                double[] array = new double[2];
                array[0] = (double)time;
                array[1] = totalbytesEach;
                jsonArray.add(array);
                reply.putArray("todayUdp", jsonArray);
              }

              rs = st.executeQuery(yesterdayTcpQuery);

              if (st.execute(yesterdayTcpQuery))
                rs = st.getResultSet();

              preDate = null;
              jsonArray = new JsonArray();
              totalbytesEach = 0;
              time = 0;

              while (rs.next()) {
                totalbytes = rs.getDouble(1);
                endtime = (rs.getString(2)).substring(0,14) + "00:00";

                if (preDate == null)
                  preDate = endtime;

                if (!preDate.equals(endtime)) {
                  date = dateFormat.parse(preDate);
                  time = date.getTime() / 1000;

                  double[] array = new double[2];
                  array[0] = (double)time;
                  array[1] = totalbytesEach;
                  jsonArray.add(array);

                  preDate = endtime;
                  totalbytesEach = 0;
                }

                totalbytesEach = totalbytesEach + totalbytes;
              }

              if (preDate != null) {
                date = dateFormat.parse(preDate);
                time = date.getTime() / 1000;

                double[] array = new double[2];
                array[0] = (double)time;
                array[1] = totalbytesEach;
                jsonArray.add(array);
                reply.putArray("yesterdayTcp", jsonArray);
              }

              rs = st.executeQuery(yesterdayUdpQuery);

              if (st.execute(yesterdayUdpQuery))
                rs = st.getResultSet();

              preDate = null;
              jsonArray = new JsonArray();
              totalbytesEach = 0;
              time = 0;

              while (rs.next()) {
                totalbytes = rs.getDouble(1);
                endtime = (rs.getString(2)).substring(0,14) + "00:00";

                if (preDate == null)
                  preDate = endtime;

                if (!preDate.equals(endtime)) {
                  date = dateFormat.parse(preDate);
                  time = date.getTime() / 1000;

                  double[] array = new double[2];
                  array[0] = (double)time;
                  array[1] = totalbytesEach;
                  jsonArray.add(array);

                  preDate = endtime;
                  totalbytesEach = 0;
                }

                totalbytesEach = totalbytesEach + totalbytes;
              }

              if (preDate != null) {
                date = dateFormat.parse(preDate);
                time = date.getTime() / 1000;

                double[] array = new double[2];
                array[0] = (double)time;
                array[1] = totalbytesEach;
                jsonArray.add(array);
                reply.putArray("yesterdayUdp", jsonArray);
              }

              rs = st.executeQuery(thisMonthBackupTcpQuery);

              if (st.execute(thisMonthBackupTcpQuery))
                rs = st.getResultSet();

              preDate = null;
              jsonArray = new JsonArray();
              totalbytesEach = 0;
              time = 0;

              while (rs.next()) {
                totalbytes = rs.getDouble(1);
                endtime = (rs.getString(2)).substring(0,11) + "00:00:00";

                if (preDate == null)
                  preDate = endtime;

                if (!preDate.equals(endtime)) {
                  date = dateFormat.parse(preDate);
                  time = date.getTime() / 1000;

                  double[] array = new double[2];
                  array[0] = (double)time;
                  array[1] = totalbytesEach;
                  jsonArray.add(array);

                  preDate = endtime;
                  totalbytesEach = 0;
                }

                totalbytesEach = totalbytesEach + totalbytes;
              }

              if (preDate != null) {
                date = dateFormat.parse(preDate);
                time = date.getTime() / 1000;

                double[] array = new double[2];
                array[0] = (double)time;
                array[1] = totalbytesEach;
                jsonArray.add(array);
              }

              rs = st.executeQuery(thisMonthTcpQuery);

              if (st.execute(thisMonthTcpQuery))
                rs = st.getResultSet();

              while (rs.next()) {
                totalbytes = rs.getDouble(1);
                endtime = (rs.getString(2)).substring(0,11) + "00:00:00";
                date = dateFormat.parse(endtime);
                time = date.getTime() / 1000;

                double[] array = new double[2];
                array[0] = (double)time;
                array[1] = totalbytesEach;
                jsonArray.add(array);
                reply.putArray("thisMonthTcp", jsonArray);
              }

              rs = st.executeQuery(thisMonthBackupUdpQuery);

              if (st.execute(thisMonthBackupUdpQuery))
                rs = st.getResultSet();

              preDate = null;
              jsonArray = new JsonArray();
              totalbytesEach = 0;
              time = 0;

              while (rs.next()) {
                totalbytes = rs.getDouble(1);
                endtime = (rs.getString(2)).substring(0,11) + "00:00:00";

                if (preDate == null)
                  preDate = endtime;

                if (!preDate.equals(endtime)) {
                  date = dateFormat.parse(preDate);
                  time = date.getTime() / 1000;

                  double[] array = new double[2];
                  array[0] = (double)time;
                  array[1] = totalbytesEach;
                  jsonArray.add(array);

                  preDate = endtime;
                  totalbytesEach = 0;
                }

                totalbytesEach = totalbytesEach + totalbytes;
              }

              if (preDate != null) {
                date = dateFormat.parse(preDate);
                time = date.getTime() / 1000;

                double[] array = new double[2];
                array[0] = (double)time;
                array[1] = totalbytesEach;
                jsonArray.add(array);
              }

              rs = st.executeQuery(thisMonthUdpQuery);

              if (st.execute(thisMonthUdpQuery))
                rs = st.getResultSet();

              while (rs.next()) {
                totalbytes = rs.getDouble(1);
                endtime = (rs.getString(2)).substring(0,11) + "00:00:00";
                date = dateFormat.parse(endtime);
                time = date.getTime() / 1000;

                double[] array = new double[2];
                array[0] = (double)time;
                array[1] = totalbytesEach;
                jsonArray.add(array);
                reply.putArray("thisMonthUdp", jsonArray);
              }
              
              reply.putNumber("code", 200);
              socket.emit(emitTo, reply);
            } catch(SQLException sqex) {
              System.out.println("SQLException: " + sqex.getMessage());
              System.out.println("SQLState: " + sqex.getSQLState());
              resToWeb(emitTo, "400", "dashboard: somethings were error");
            } catch(ParseException e) {
              e.printStackTrace();
            }
          }
        };

        thread.start();
      } else {
      }
    }

    public void tcpudp(String emitTo, String code, String unit) {

      if (this.isConnected) {
        Thread thread = new Thread() {
          public void run() {
            try {
              java.sql.Statement st = getSt();

              String barTcpQuery = "SELECT p.starttime, p.endtime, "
                  + "SUM(p.totalbytes), SUM(p.danger), SUM(p.warn) FROM packets p"
                  + " WHERE p.tcpudp = 0 GROUP BY p.endtime ORDER BY p.endtime DESC";

              String barUdpQuery = "SELECT p.starttime, p.endtime, "
                  + "SUM(p.totalbytes), SUM(p.danger), SUM(p.warn) FROM packets p"
                  + " WHERE p.tcpudp = 1 GROUP BY p.endtime ORDER BY p.endtime DESC";

              JsonObject reply = new JsonObject();
              ResultSet rs = null;

              rs = st.executeQuery(barTcpQuery);

              if(st.execute(barTcpQuery))
                rs = st.getResultSet();

              String preTime = "init";
              long time = 0;
              double totalbytesEachTime = 0;
              double totalbytes = 0;
              int dangerEachTime = 0;
              int danger = 0;
              int warnEachTime = 0;
              int warn = 0;
              String starttime = null;
              String endtime = null;
              String preDate = null;
              Date date = null;

              JsonArray jsonArray = new JsonArray();
              JsonObject jsonGroup = new JsonObject();

              int unitCnt = 0;

              while (rs.next()) {
                JsonObject tcpObject = new JsonObject();

                starttime = rs.getString(1);
                endtime = rs.getString(2);
                totalbytes = rs.getDouble(3);
                danger = rs.getInt(4);
                warn = rs.getInt(5);

                starttime = starttime.substring(0,19);
                endtime = endtime.substring(0,19);

                String curtime = endtime.substring(11,13) + ":00:00";
                String curdate = endtime.substring(0,11);

                if (preTime.equals("init")) {
                  preTime = curtime;
                  preDate = curdate;
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

                  date = dateFormat.parse(preDate + preTime);
                  time = date.getTime() / 1000;

                  jsonGroup.putArray(Long.toString(time), jsonArray);

                  jsonArray = new JsonArray();
                  unitCnt = unitCnt + 1;
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

                date = dateFormat.parse(preDate + preTime);
                time = date.getTime() / 1000;

                jsonGroup.putArray(Long.toString(time), jsonArray);

                reply.putObject("tcpTraffic", jsonGroup);
                unitCnt = unitCnt + 1;
              }

              if (unitCnt < 24) {
                date = new Date();
                date = new Date(date.getTime() - ((long)(1000*60*60*24)));
                String yesterday = sdf.format(date);
                yesterday = yesterday.substring(0,11) + "00:00:00";

                String backupTcpQuery = "SELECT starttime, endtime, SUM(totalbytes"
                  + "), SUM(danger), SUM(warn) FROM backup_packets WHERE "
                  + "(tcpudp = 0) AND (endtime >= '" + yesterday + "') GROUP"
                  + " BY endtime ORDER BY endtime DESC";

                String backupUdpQuery = "SELECT starttime, endtime, SUM(totalbytes"
                  + "), SUM(danger), SUM(warn) FROM backup_packets WHERE "
                  + "(tcpudp = 1) AND (endtime >= '" + yesterday + "') GROUP"
                  + " BY endtime ORDER BY endtime DESC";

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

                while (rs.next()) {
                  JsonObject udpObject = new JsonObject();

                  starttime = rs.getString(1);
                  endtime = rs.getString(2);
                  totalbytes = rs.getDouble(3);
                  danger = rs.getInt(4);
                  warn = rs.getInt(5);

                  starttime = starttime.substring(0,19);
                  endtime = endtime.substring(0,19);

                  String curtime = endtime.substring(11,13) + ":00:00";
                  String curdate = endtime.substring(0,11);

                  if (preTime.equals("init")) {
                    preTime = curtime;
                    preDate = curdate;
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

                    date = dateFormat.parse(preDate + preTime);
                    time = date.getTime() / 1000;

                    jsonGroup.putArray(Long.toString(time), jsonArray);

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

                  date = dateFormat.parse(preDate + preTime);
                  time = date.getTime() / 1000;

                  jsonGroup.putArray(Long.toString(time), jsonArray);

                  if (code.equals("traffic"))
                    reply.putObject("udpTraffic", jsonGroup);
                }

                if (unitCnt < 24) {
                  int preUnitCnt = unitCnt;

                  rs = st.executeQuery(backupTcpQuery);

                  if(st.execute(backupTcpQuery))
                    rs = st.getResultSet();

                  preTime = "init";
                  totalbytesEachTime = 0;
                  dangerEachTime = 0;
                  warnEachTime = 0;
                  preDate = null;

                  jsonArray = new JsonArray();
                  jsonGroup = new JsonObject();

                  while (rs.next() && unitCnt < 24) {
                    JsonObject tcpObject = new JsonObject();

                    starttime = rs.getString(1);
                    endtime = rs.getString(2);
                    totalbytes = rs.getDouble(3);
                    danger = rs.getInt(4);
                    warn = rs.getInt(5);

                    starttime = starttime.substring(0,19);
                    endtime = endtime.substring(0,19);

                    String curtime = endtime.substring(11,13) + ":00:00";
                    String curdate = endtime.substring(0,11);

                    if (preTime.equals("init")) {
                      preTime = curtime;
                      preDate = curdate;
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

                      date = dateFormat.parse(preDate + preTime);
                      time = date.getTime() / 1000;

                      jsonGroup.putArray(Long.toString(time), jsonArray);

                      jsonArray = new JsonArray();
                      unitCnt = unitCnt + 1;
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

                  if (!preTime.equals("init") && unitCnt <= 24) {
                    if (unitCnt != 24) {
                      JsonObject insertObject = new JsonObject().putNumber("totalbytes", 
                          totalbytesEachTime);
                      jsonArray.addObject(insertObject);

                      insertObject = new JsonObject().putNumber("totaldanger",
                          dangerEachTime);
                      jsonArray.addObject(insertObject);

                      insertObject = new JsonObject().putNumber("totalwarn",
                          warnEachTime);
                      jsonArray.addObject(insertObject);

                      date = dateFormat.parse(preDate + preTime);
                      time = date.getTime() / 1000;

                      jsonGroup.putArray(Long.toString(time), jsonArray);
                    }

                    JsonObject preGroup = reply.getObject("tcpTraffic");
                    preGroup.mergeIn(jsonGroup);
                    reply.removeField("tcpTraffic");
                    reply.putObject("tcpTraffic", preGroup);
                  }

                  unitCnt = preUnitCnt;

                  rs = st.executeQuery(backupUdpQuery);

                  if(st.execute(backupUdpQuery))
                    rs = st.getResultSet();

                  preTime = "init";
                  totalbytesEachTime = 0;
                  dangerEachTime = 0;
                  warnEachTime = 0;
                  preDate = null;

                  jsonArray = new JsonArray();
                  jsonGroup = new JsonObject();

                  while (rs.next() && unitCnt < 24) {
                    JsonObject udpObject = new JsonObject();

                    starttime = rs.getString(1);
                    endtime = rs.getString(2);
                    totalbytes = rs.getDouble(3);
                    danger = rs.getInt(4);
                    warn = rs.getInt(5);

                    starttime = starttime.substring(0,19);
                    endtime = endtime.substring(0,19);

                    String curtime = endtime.substring(11,13) + ":00:00";
                    String curdate = endtime.substring(0,11);

                    if (preTime.equals("init")) {
                      preTime = curtime;
                      preDate = curdate;
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

                      date = dateFormat.parse(preDate + preTime);
                      time = date.getTime() / 1000;

                      jsonGroup.putArray(Long.toString(time), jsonArray);

                      jsonArray = new JsonArray();
                      unitCnt = unitCnt + 1;
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

                  if (!preTime.equals("init") && unitCnt <= 24) {
                    if (unitCnt != 24) {
                      JsonObject insertObject = new JsonObject().putNumber("totalbytes", 
                          totalbytesEachTime);
                      jsonArray.addObject(insertObject);

                      insertObject = new JsonObject().putNumber("totaldanger",
                          dangerEachTime);
                      jsonArray.addObject(insertObject);

                      insertObject = new JsonObject().putNumber("totalwarn",
                          warnEachTime);
                      jsonArray.addObject(insertObject);

                      date = dateFormat.parse(preDate + preTime);
                      time = date.getTime() / 1000;

                      jsonGroup.putArray(Long.toString(time), jsonArray);
                    }

                    JsonObject preGroup = reply.getObject("udpTraffic");
                    preGroup.mergeIn(jsonGroup);
                    reply.removeField("udpTraffic");
                    reply.putObject("udpTraffic", preGroup);
                  }
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
              JsonArray jsonArray = new JsonArray();

              Date date = new Date();
              date = new Date(date.getTime() - ((long)(1000*60*60*24*6)));
              String week = sdf.format(date);
              week = week.substring(0,11) + "00:00:00";

              String packetsQuery = "SELECT SUM(danger), SUM(warn), endtime FROM packets WHERE 1";
              String backupQuery = "SELECT SUM(danger), SUM(warn), endtime FROM "
                + "backup_packets WHERE endtime >= '" + week + "' GROUP BY "
                + "endtime ORDER BY endtime DESC";

              rs = st.executeQuery(packetsQuery);

              if (st.execute(packetsQuery))
                rs = st.getResultSet();

              int i = -1;
              long time = 0;
              int weekStart = 0;
              int eachUnit = 0;
              double traffic = 0;
              String endtime = null;
              DateFormat dateFormat2 = new SimpleDateFormat("yyyy-MM-dd");
              
              while (rs.next()) {
                eachUnit = rs.getInt(1) + rs.getInt(2);
                endtime = (rs.getString(3)).substring(0,10);

                date = dateFormat2.parse(endtime);
                time = date.getTime() / 1000;

                Long[] data = new Long[2];
                data[0] = time;
                data[1] = (long)eachUnit;
                jsonArray.add(data);
              }

              rs = st.executeQuery(backupQuery);

              if (st.execute(backupQuery))
                rs = st.getResultSet();

              int totalUnit = 0;
              double totalTraffic = 0;
              String day = null;
              boolean flag = true;

              while (rs.next()) {
                if (i == 6)
                  break;

                eachUnit = rs.getInt(1) + rs.getInt(2);
                endtime = (rs.getString(3)).substring(0,10);
                
                if (i == -1) {
                  day = endtime;
                  i = 0;
                }

                if (!day.equals(endtime)) {
                  date = dateFormat2.parse(day);
                  time = date.getTime() / 1000;

                  Long[] data = new Long[2];
                  data[0] = time;
                  data[1] = (long)totalUnit;
                  jsonArray.add(data);

                  day = endtime;
                  totalUnit = 0;
                  totalTraffic = 0;
                  i = i + 1;
                }

                totalUnit = totalUnit + eachUnit;
                totalTraffic = totalTraffic + traffic;
              }

              if (i <= 5) {
                date = dateFormat2.parse(day);
                time = date.getTime() / 1000;

                Long[] data = new Long[2];
                data[0] = time;
                data[1] = (long)totalUnit;
                jsonArray.add(data);
              }

              reply.putArray("dangerWarn", jsonArray);
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

              String query = "SELECT COUNT(p.destination_port), pr.name FROM packets p "
                + "JOIN protocol pr JOIN users u "
                + "ON (p.destination_port = pr.port) AND (u.ip = p.source_ip) GROUP BY "
                + "pr.name ORDER BY COUNT(p.source_port) DESC";

              rs = st.executeQuery(query);

              if(st.execute(query))
                rs = st.getResultSet();

              JsonObject userObject = new JsonObject();
              int i = 0;
              int count = 0;
              String protocol = null;

              while(rs.next()) {
                if (i++ > 3)
                  break;

                count = rs.getInt(1);
                protocol = rs.getString(2);

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

    public void inoutBound(String emitTo, String unit) {
      if (this.isConnected) {
        Thread thread = new Thread() {
          public void run() {
            try {
              java.sql.Statement st = getSt();
              ResultSet rs = null;
              JsonObject reply = new JsonObject();

              Date date = new Date();
              date = new Date(date.getTime() - ((long)(1000*60*60*24*6)));
              String week = sdf.format(date);
              week = week.substring(0,11) + "00:00:00";

              String outboundQuery = "SELECT SUM(p.totalbytes), SUM(p.warn), "
                + "SUM(p.danger), p.starttime, p.endtime FROM packets p "
                + "JOIN users u ON u.ip = p.source_ip GROUP BY p.endtime "
                + "ORDER BY p.endtime DESC";

              String inboundQuery = "SELECT SUM(p.totalbytes), SUM(p.warn), "
                + "SUM(p.danger), p.starttime, p.endtime FROM packets p "
                + "JOIN users u ON u.ip = p.destination_ip GROUP BY p.endtime "
                + "ORDER BY p.endtime DESC";
              String backupOutboundQuery = "SELECT SUM(p.totalbytes), SUM(p.warn), "
                + "SUM(p.danger), p.starttime, p.endtime FROM backup_packets p "
                + "JOIN users u ON (u.ip = p.source_ip) AND (p.endtime >= '"
                + week + "') GROUP BY p.endtime ORDER BY p.endtime DESC";

              String backupInboundQuery = "SELECT SUM(p.totalbytes), SUM(p.warn), "
                + "SUM(p.danger), p.starttime, p.endtime FROM backup_packets p "
                + "JOIN users u ON (u.ip = p.destination_ip) AND (p.endtime >="
                + " '" + week + "') GROUP BY p.endtime ORDER BY p.endtime DESC";

              rs = st.executeQuery(inboundQuery);

              if(st.execute(inboundQuery))
                rs = st.getResultSet();

              String preTime = "init";
              double totalbytesEachTime = 0;
              double totalbytes = 0;
              int dangerEachTime = 0;
              int danger = 0;
              int warnEachTime = 0;
              int warn = 0;
              int i = 7;
              String starttime = null;
              String endtime = null;
              String preDate = null;

              String[] weekDate = new String[7];
              double[] inbound = new double[7];
              double[] outbound = new double[7];

              while (rs.next()) {
                totalbytes = rs.getDouble(1);
                warn = rs.getInt(2);
                danger = rs.getInt(3);
                starttime = (rs.getString(4)).substring(0,19);
                endtime = (rs.getString(5)).substring(0,19);

                String curtime = endtime.substring(8,10) + " 00:00:00";
                String curdate = endtime.substring(0,8);

                if (preTime.equals("init")) {
                  preTime = curtime;
                  preDate = curdate;
                  i = i - 1;
                }

                if (!preTime.equals(curtime) || !preDate.equals(curdate)) {
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
                date = dateFormat.parse(preDate + preTime);
                weekDate[i] = preDate + preTime.substring(0,2);
                inbound[i] = totalbytesEachTime / (1024*1024);
              }

              rs = st.executeQuery(outboundQuery);

              if(st.execute(outboundQuery))
                rs = st.getResultSet();

              preTime = "init";
              totalbytesEachTime = 0;
              dangerEachTime = 0;
              warnEachTime = 0;
              preDate = null;

              while (rs.next()) {
                JsonObject outObject = new JsonObject();

                totalbytes = rs.getDouble(1);
                warn = rs.getInt(2);
                danger = rs.getInt(3);
                starttime = (rs.getString(4)).substring(0,19);
                endtime = (rs.getString(5)).substring(0,19);

                String curtime = endtime.substring(8,10) + " 00:00:00";
                String curdate = endtime.substring(0,8);

                if (preTime.equals("init")) {
                  preTime = curtime;
                  preDate = curdate;
                }

                if (!preTime.equals(curtime) || !preDate.equals(curdate)) {
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

              if (!preTime.equals("init"))
                outbound[i] = -totalbytesEachTime / (1024*1024);

              rs = st.executeQuery(backupInboundQuery);

              if(st.execute(backupInboundQuery))
                rs = st.getResultSet();

              int preI = i;
              preTime = "init";
              totalbytesEachTime = 0;
              dangerEachTime = 0;
              warnEachTime = 0;
              preDate = null;

              while (rs.next() && i > 0) {
                totalbytes = rs.getDouble(1);
                warn = rs.getInt(2);
                danger = rs.getInt(3);
                starttime = (rs.getString(4)).substring(0,19);
                endtime = (rs.getString(5)).substring(0,19);

                String curtime = endtime.substring(8,10) + " 00:00:00";
                String curdate = endtime.substring(0,8);

                if (preTime.equals("init")) {
                  preTime = curtime;
                  preDate = curdate;
                }

                if (!preTime.equals(curtime) || !preDate.equals(curdate)) {
                  inbound[i-1] = totalbytesEachTime / (1024*1024);
                  weekDate[i-1] = preDate + preTime.substring(0,2);

                  totalbytesEachTime = 0;
                  dangerEachTime = 0;
                  warnEachTime = 0;
                  preTime = curtime;
                  preDate = curdate;
                  i = i - 1;
                }

                totalbytesEachTime = totalbytesEachTime + totalbytes;
                dangerEachTime = dangerEachTime + danger;
                warnEachTime = warnEachTime + warn;
              }

              if (!preTime.equals("init")) {
                JsonObject insertObject = new JsonObject();
                if (i > 0) {
                  inbound[i-1] = totalbytesEachTime / (1024*1024);
                  weekDate[i-1] = preDate + preTime.substring(0,2);
                }
              }

              rs = st.executeQuery(backupOutboundQuery);

              if(st.execute(backupOutboundQuery))
                rs = st.getResultSet();

              i = preI;
              preTime = "init";
              totalbytesEachTime = 0;
              dangerEachTime = 0;
              warnEachTime = 0;
              preDate = null;

              while (rs.next() && i > 0) {
                totalbytes = rs.getDouble(1);
                warn = rs.getInt(2);
                danger = rs.getInt(3);
                starttime = (rs.getString(4)).substring(0,19);
                endtime = (rs.getString(5)).substring(0,19);

                String curtime = endtime.substring(8,10) + " 00:00:00";
                String curdate = endtime.substring(0,8);

                if (preTime.equals("init")) {
                  preTime = curtime;
                  preDate = curdate;
                }

                if (!preTime.equals(curtime) || !preDate.equals(curdate)) {
                  outbound[i-1] = -totalbytesEachTime / (1024*1024);

                  totalbytesEachTime = 0;
                  dangerEachTime = 0;
                  warnEachTime = 0;
                  preTime = curtime;
                  preDate = curdate;
                  i = i - 1;
                }

                totalbytesEachTime = totalbytesEachTime + totalbytes;
                dangerEachTime = dangerEachTime + danger;
                warnEachTime = warnEachTime + warn;
              }

              if (!preTime.equals("init")) {
                JsonObject insertObject = new JsonObject();
                if (i > 0)
                  outbound[i-1] = -totalbytesEachTime / (1024*1024);
              }

              JsonArray jsonArray = new JsonArray();
              jsonArray.add(weekDate);
              reply.putArray("weekDate", jsonArray);

              jsonArray = new JsonArray();
              jsonArray.add(inbound);
              reply.putArray("inbound", jsonArray);

              jsonArray = new JsonArray();
              jsonArray.add(outbound);
              reply.putArray("outbound", jsonArray);

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
}

