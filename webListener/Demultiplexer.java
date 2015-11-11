package webListener;

import com.nhncorp.mods.socket.io.SocketIOServer;
import com.nhncorp.mods.socket.io.SocketIOSocket;

import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

public class Demultiplexer implements Runnable {

    private SocketIOServer ioServer;
    private SocketIOSocket socket;
    private MysqlHandler mysqlHandler;

    public Demultiplexer(SocketIOServer ioServer, SocketIOSocket socket) {
        this.ioServer = ioServer;
        this.socket = socket;
        this.mysqlHandler = new MysqlHandler("localhost", "root",
            "klpsoma123", socket);
    }

    public void run() {

        socket.on("realtimeOn", new Handler<JsonObject>() {
            public void handle(JsonObject json) {
                String emitTo = "realtimeOn res";
                mysqlHandler.realtimeOn(emitTo);
            }
        });

        socket.on("tcpudp hour", new Handler<JsonObject>() {
            public void handle(JsonObject json) {
                String emitTo = "tcpudp hour res";
                mysqlHandler.tcpudp(emitTo, "traffic", "hour");
            }
        });

        socket.on("dashboard", new Handler<JsonObject>() {
            public void handle(JsonObject json) {
                String emitTo = "dashboard res";
                mysqlHandler.dashboard(emitTo);
            }
        });

        socket.on("tomorrow security", new Handler<JsonObject>() {
            public void handle(JsonObject json) {
              String emitTo = "tomorrow security res";
              mysqlHandler.predictSecurity(emitTo, "tomorrow");
            }
        });

        socket.on("protocol statistics", new Handler<JsonObject>() {
            public void handle(JsonObject json) {
              String emitTo = "protocol statistics res";
              mysqlHandler.protocolStatistics(emitTo, "traffic");
            }
        });

        socket.on("inoutbound week", new Handler<JsonObject>() {
            public void handle(JsonObject json) {
              String emitTo = "inoutbound week res";
              mysqlHandler.inoutBound(emitTo, "week");
            }
        });

        socket.on("barStatistics dangerWarn", new Handler<JsonObject>() {
            public void handle(JsonObject json) {
              String emitTo = "barStatistics dangerWarn res";
              mysqlHandler.barStatistics(emitTo, "dangerWarn");
            }
        });

        socket.on("insert log", new Handler<JsonObject>() {
            public void handle(JsonObject json) {
                String admin_idx = jsonToString(json, "admin_idx");
                String action = jsonToString(json, "action");
                String date = jsonToString(json, "date");
                date.trim();

                String emitTo = "insert log res";
                mysqlHandler.insertLogHandler(emitTo, admin_idx, action, date);
            }
        });

        socket.on("info", new Handler<JsonObject>() {
            public void handle(JsonObject json) {
              String admin_idx = jsonToString(json, "admin_idx");
              String action = jsonToString(json, "action");
              String contents = jsonToString(json, "contents");
              String date = jsonToString(json, "date");

              String emitTo = "info res";
              mysqlHandler.notifyToFirewall(emitTo, admin_idx, action,
                  contents, date);
            }
        });
    }
    
    private String jsonToString (JsonObject json, String element) {
      return json.getString(element);
    }
}
