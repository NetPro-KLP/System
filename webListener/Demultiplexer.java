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
        this.mysqlHandler = new MysqlHandler("localhost", "root", "klpsoma123");
    }

    public void run() {

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
                mysqlHandler.deleteHandler(table, key, value);

                // delete 성공
                JsonObject reply = new JsonObject().putString("code", "204");

                // 에러날 경우
                // 404: 리소스가 존재하지 않을 경우
                // JsonObject reply = new JsonObject().putString("code","404");
                // reply.putString("body", "your data was this");

                // 400: 그 외의 에러는 에러 이유를 "body"에 삽입
                // JsonObject reply = new JsonObject().putString("code","400");
                // reply.putString("body", "somethings were error");

                socket.emit("delete res", reply);
            }
        });

        socket.on("insert log", new Handler<JsonObject>() {
            public void handle(JsonObject json) {
                String admin_idx = jsonToString(json, "admin_idx");
                String action = jsonToString(json, "action");
                String date = jsonToString(json, "date");
                date.trim();

                boolean res = mysqlHandler.insertLogHandler(admin_idx, action, date);

                JsonObject reply;

                // insert 성공(201)
                if (res)
                  reply = new JsonObject().putString("code", "201");
                // 에러날 경우(400)
                else {
                  reply = new JsonObject().putString("code","400");
                  reply.putString("body", "somethings were error");
                }

                socket.emit("insert log res", reply);
            }
        });

        socket.on("info", new Handler<JsonObject>() {
            public void handle(JsonObject json) {
              String table = jsonToString(json, "table");
            }
        });
    }
    
    private String jsonToString (JsonObject json, String element) {
      return json.getString(element);
    }
}
