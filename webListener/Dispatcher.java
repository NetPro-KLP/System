package webListener;

import com.nhncorp.mods.socket.io.SocketIOServer;
import com.nhncorp.mods.socket.io.SocketIOSocket;

import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.Handler;

public class Dispatcher {

  private int port = 8888;

  public void dispatch (SocketIOServer ioServer, HttpServer server) {
    ioServer.sockets().onConnection(new Handler<SocketIOSocket>() {
        public void handle(final SocketIOSocket socket) {
            Demultiplexer demultiplexer = new Demultiplexer(ioServer, socket);
            demultiplexer.run();
        }
    });

    server.listen(this.port);
  }
}
