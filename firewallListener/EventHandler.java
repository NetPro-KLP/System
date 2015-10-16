package firewallListener;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.io.IOException;
import java.io.InputStream;

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

	public void nopeEvent() {
        System.out.println("Nope");
    }

    public void initEvent() {
      if (this.isConnected) {
        try {
          java.sql.Statement st = null;
          st = this.firewallConn.createStatement();
          ResultSet rs = null;

          String query = "SELECT data FROM rules_data WHERE 1";

          rs = st.executeQuery(query);

          if(st.execute(query))
            rs = st.getResultSet();

          while(rs.next()) {
            String data = rs.getString(1);
            System.out.println(data);
          }
        } catch (SQLException sqex) {
          System.out.println("SQLExeption: " + sqex.getMessage());
          System.out.println("SQLState: " + sqex.getSQLState());
        }

      } else {
      }
    }

    public void expiredEvent() {
      if (this.isConnected) {
        try {
          java.sql.Statement st = null;
          st = this.firewallConn.createStatement();

          String query = "INSERT INTO packets VALUES(1, '127.0.0.1', '16', '99.99.99.99', '1000', 'icmp', 0, 10000, 500000, '2015-10-10 07:30:15', '2015-10-10 07:50:30', 1)";

          st.executeUpdate(query);

        } catch (SQLException sqex) {
          System.out.println("SQLExeption: " + sqex.getMessage());
          System.out.println("SQLState: " + sqex.getSQLState());
        }
      } else {
      }
    }
}
