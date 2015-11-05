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

        socket.on("realtimeClose", new Handler<JsonObject>() {
            public void handle(JsonObject json) {
                String emitTo = "realtimeClose res";
                mysqlHandler.realtimeClose(emitTo);
            }
        });

        socket.on("tcpudp hour", new Handler<JsonObject>() {
            public void handle(JsonObject json) {
                String emitTo = "tcpudp hour res";
                mysqlHandler.tcpudp(emitTo, "traffic", "hour");
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

        socket.on("protocol user statistics", new Handler<JsonObject>() {
            public void handle(JsonObject json) {
              String emitTo = "protocol user statistics res";
              mysqlHandler.protocolStatistics(emitTo, "user");
            }
        });

        socket.on("inoutbound week", new Handler<JsonObject>() {
            public void handle(JsonObject json) {
              String emitTo = "inoutbound week res";
              mysqlHandler.inoutBound(emitTo, "week");
            }
        });

        socket.on("barStatistics danger", new Handler<JsonObject>() {
            public void handle(JsonObject json) {
              String emitTo = "barStatistics danger res";
              mysqlHandler.barStatistics(emitTo, "danger");
            }
        });

        socket.on("barStatistics traffic", new Handler<JsonObject>() {
            public void handle(JsonObject json) {
              String emitTo = "barStatistics traffic res";
              mysqlHandler.barStatistics(emitTo, "traffic");
            }
        });

        socket.on("barStatistics warn", new Handler<JsonObject>() {
            public void handle(JsonObject json) {
              String emitTo = "barStatistics warn res";
              mysqlHandler.barStatistics(emitTo, "warn");
            }
        });

        socket.on("barStatistics dangerWarn", new Handler<JsonObject>() {
            public void handle(JsonObject json) {
              String emitTo = "barStatistics dangerWarn res";
              mysqlHandler.barStatistics(emitTo, "dangerWarn");
            }
        });

        socket.on("barStatistics weekDangerWarn", new Handler<JsonObject>() {
            public void handle(JsonObject json) {
              String emitTo = "barStatistics weekDangerWarn res";
              mysqlHandler.barStatistics(emitTo, "weekDangerWarn");
            }
        });

        socket.on("insert", new Handler<JsonObject>() {
            public void handle(JsonObject json) {
                String target = jsonToString(json, "data");

                // insert 성공(201)
                JsonObject reply = new JsonObject().putString("code", "201");
                reply.putString("body", "your data was this");

                // 에러날 경우
                // 400
                // JsonObject reply = new JsonObject().putString("code","400");
                // reply.putString("body", "somethings were error");

                socket.emit("insert res", reply);
            }
        });

        socket.on("read", new Handler<JsonObject>() {
            public void handle(JsonObject json) {
                String target = jsonToString(json, "data");

                // read 성공
                JsonObject reply = new JsonObject().putString("code", "200");
                reply.putString("body", "somthings were generated");

                // 에러날 경우
                // 404: 리소스가 존재하지 않을 경우
                // JsonObject reply = new JsonObject().putString("code","404");
                // reply.putString("body", "your data was this");

                // 400: 그 외의 에러는 에러 이유를 "body"에 삽입
                // JsonObject reply = new JsonObject().putString("code","400");
                // reply.putString("body", "somethings were error");

                socket.emit("read res", reply);
            }
        });

        socket.on("update", new Handler<JsonObject>() {
            public void handle(JsonObject json) {
                String target = jsonToString(json, "data");

                // update 성공
                JsonObject reply = new JsonObject().putString("code", "200");
                reply.putString("body", "somethings were updated");

                // 에러날 경우
                // 404: 리소스가 존재하지 않을 경우
                // JsonObject reply = new JsonObject().putString("code","404");
                // reply.putString("body", "your data was this");

                // 400: 그 외의 에러는 에러 이유를 "body"에 삽입
                // JsonObject reply = new JsonObject().putString("code","400");
                // reply.putString("body", "somethings were error");

                socket.emit("update res", reply);
            }
        });

        socket.on("delete", new Handler<JsonObject>() {
            public void handle(JsonObject json) {
                String table = jsonToString(json, "table");
                String key = jsonToString(json, "key");
                String value = jsonToString(json, "value");

                String emitTo = "delete res";
                mysqlHandler.deleteHandler(emitTo, table, key, value);
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
