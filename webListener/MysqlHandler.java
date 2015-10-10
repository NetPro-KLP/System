package webListener;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MysqlHandler {

  private Connection firewallConn = null;
  private boolean isConnected;

    public MysqlHandler(String host, String userId, String pw) {

      this.isConnected = false;
      
      while (!this.isConnected) {
        try {
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

    public java.sql.Statement getSt() {
      java.sql.Statement st = null;

      try {
          st = this.firewallConn.createStatement();
      } catch (SQLException sqex) {
          System.out.println("SQLException: " + sqex.getMessage());
          System.out.println("SQLState: " + sqex.getSQLState());
      }

      return st;
    }

    public void show() {
      if (this.isConnected) {
        try {

          java.sql.Statement st = getSt();
          ResultSet rs = null;
          /*
          rs = st.executeQuery("SHOW DATABASES");

          if (st.execute("SHOW DATABASES")) {
            rs = st.getResultSet();
          }

          while (rs.next()) {
            String str = rs.getNString(1);
            System.out.println(str);
          }
*/
          rs = st.executeQuery("SELECT data FROM rules_data WHERE 1");

          if (st.execute("SELECT data FROM rules_data WHERE 1")) {
            rs = st.getResultSet();
          }

          while(rs.next()) {
            String str = rs.getNString(1);
            //System.out.println(str);
          }

          firewallConn.close();
        } catch (SQLException sqex) {
          System.out.println("SQLException: " + sqex.getMessage());
          System.out.println("SQLState: " + sqex.getSQLState());
        }
      } else {
      }
    }

    public boolean insertLogHandler(String admin_idx,
        String action, String date) {
      if (this.isConnected) {
        try {
          java.sql.Statement st = getSt();

          String query = "INSERT INTO `log`(`admin_idx`,`action`,`date`) VALUES(" + admin_idx
            + ", '" + action + "', '" + date + "')";

          st.executeUpdate(query);

          return true;
        } catch (SQLException sqex) {
          System.out.println("SQLException: " + sqex.getMessage());
          System.out.println("SQLState: " + sqex.getSQLState());

          return false;
        }
      } else {
        return false;
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
      if (this.isConnected) {
        try {
          java.sql.Statement st = getSt();

          String query = "DELETE FROM " + table + " WHERE " + key + " = '"
            + value + "'";

          st.executeUpdate(query);

        } catch (SQLException sqex) {
          System.out.println("SQLException: " + sqex.getMessage());
          System.out.println("SQLState: " + sqex.getSQLState());
        }
      } else {
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
