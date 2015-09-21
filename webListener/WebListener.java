package webListener;

import com.nhncorp.mods.socket.io.SocketIOServer;
import com.nhncorp.mods.socket.io.SocketIOSocket;
import com.nhncorp.mods.socket.io.impl.DefaultSocketIOServer;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class WebListener extends Verticle {

    public void start() {

        int port = 20000;

        HttpServer server = vertx.createHttpServer();
        SocketIOServer ioServer = new DefaultSocketIOServer(vertx, server);

        ioServer.sockets().onConnection(new Handler<SocketIOSocket>() {
            public void handle(final SocketIOSocket socket) {
                socket.emit("Hello");
                socket.on("/news", new Handler<JsonObject>() {
                    public void handle(JsonObject data) {
                        System.out.println(data);
                        socket.emit(data);
                    }
                });
            }
        });

        server.listen(port);
    }
}
