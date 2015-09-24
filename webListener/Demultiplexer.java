package webListener;

import com.nhncorp.mods.socket.io.SocketIOServer;
import com.nhncorp.mods.socket.io.SocketIOSocket;

import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

public class Demultiplexer {

  private int port = 8888;

  public void demultiplex (SocketIOServer ioServer, HttpServer server) {
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

    server.listen(this.port);
  }
}
