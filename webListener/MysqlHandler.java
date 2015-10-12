package webListener;

import com.nhncorp.mods.socket.io.SocketIOSocket;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.vertx.java.core.json.JsonObject;

public class MysqlHandler {

  private SocketIOSocket socket;
  private Connection firewallConn = null;
  private boolean isConnected;

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
      JsonObject reply = null;

      if (this.isConnected) {
        if (code != null) {
          reply = new JsonObject().putString("code", code);
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

    public void insertLogHandler(String admin_idx,
        String action, String date) {

      String emitTo = "insert log res";

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
    public void deleteHandler(String table, String key, String value) {

      String emitTo = "delete res";

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
