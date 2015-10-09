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

    public void show() {
      if (this.isConnected) {
        try {

          java.sql.Statement st = null;
          ResultSet rs = null;
          st = this.firewallConn.createStatement();
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
/*
    public void insertHandler() {
      if (this.isConnected) {
        try {

          String query = "";

        } catch (SQLException sqex) {
          System.out.println("SQLException: " + sqex.getMessage());
          System.out.println("SQLState: " + sqex.getSQLState());
        }
      } else {
      }
    }

    public void deleteHandler() {
      if (this.isConnected) {
        try {

        } catch (SQLException sqex) {
          System.out.println("SQLException: " + sqex.getMessage());
          System.out.println("SQLState: " + sqex.getSQLState());
        }
      } else {
      }
    }

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
