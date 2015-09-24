package webListener;

import com.nhncorp.mods.socket.io.SocketIOServer;
import com.nhncorp.mods.socket.io.impl.DefaultSocketIOServer;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.platform.Verticle;

/*
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
*/
public class WebListener extends Verticle {

    public void start() {

        HttpServer server = vertx.createHttpServer();
        SocketIOServer ioServer = new DefaultSocketIOServer(vertx, server);

        Demultiplexer demultiplexer = new Demultiplexer();
        demultiplexer.demultiplex(ioServer, server);
/*
        try {
          Connection con = null;

          con = DriverManager.getConnection("jdbc:mysql://localhost",
              "root", "klpsoma123");

          java.sql.Statement st = null;
          ResultSet rs = null;
          st = con.createStatement();
          rs = st.executeQuery("SHOW DATABASES");

          if (st.execute("SHOW DATABASES")) {
            rs = st.getResultSet();
          }

          while (rs.next()) {
            String str = rs.getNString(1);
            System.out.println(str);
          }
        } catch (SQLException sqex) {
          System.out.println("SQLException: " + sqex.getMessage());
          System.out.println("SQLState: " + sqex.getSQLState());
        }
        */
    }
}
