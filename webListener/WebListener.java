package webListener;

import com.nhncorp.mods.socket.io.SocketIOServer;
import com.nhncorp.mods.socket.io.impl.DefaultSocketIOServer;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.platform.Verticle;

public class WebListener extends Verticle {

    public void start() {

        HttpServer server = vertx.createHttpServer();
        SocketIOServer ioServer = new DefaultSocketIOServer(vertx, server);

        Dispatcher dispatcher = new Dispatcher();
        dispatcher.dispatch(ioServer, server);
    }
}
