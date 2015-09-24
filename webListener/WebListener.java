package webListener;

import com.nhncorp.mods.socket.io.SocketIOServer;
import com.nhncorp.mods.socket.io.SocketIOSocket;
import com.nhncorp.mods.socket.io.impl.DefaultSocketIOServer;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

public class WebListener extends Verticle {

    public void start() {

        int port = 8888;

        HttpServer server = vertx.createHttpServer();
        SocketIOServer ioServer = new DefaultSocketIOServer(vertx, server);

        ioServer.sockets().onConnection(new Handler<SocketIOSocket>() {
            public void handle(final SocketIOSocket socket) {
                socket.on("msg", new Handler<JsonObject>() {
                    public void handle(JsonObject data) {
                        System.out.println(data);
                        socket.emit(data);
                    }
                });
            }
        });

        server.listen(port);

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
    }
}
