package webListener;

import com.nhncorp.mods.socket.io.SocketIOServer;
import com.nhncorp.mods.socket.io.SocketIOSocket;

import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

public class Demultiplexer implements Runnable {

    private SocketIOServer ioServer;
    private SocketIOSocket socket;

    public Demultiplexer(SocketIOServer ioServer, SocketIOSocket socket) {
        this.ioServer = ioServer;
        this.socket = socket;
    }

    public void run() {
        socket.on("insert", new Handler<JsonObject>() {
            public void handle(JsonObject data) {
                System.out.println(data);
                socket.emit(data);
            }
        });

        socket.on("read", new Handler<JsonObject>() {
            public void handle(JsonObject data) {
                System.out.println(data);
                socket.emit(data);
            }
        });

        socket.on("update", new Handler<JsonObject>() {
            public void handle(JsonObject data) {
                System.out.println(data);
                socket.emit(data);
            }
        });

        socket.on("delete", new Handler<JsonObject>() {
            public void handle(JsonObject data) {
                System.out.println(data);
                socket.emit(data);
            }
        });
    }
    
}
