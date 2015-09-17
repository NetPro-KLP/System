package firewallServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FirewallServer {
    private int port = 30000;
    private static int threadPoolSize = 10;
    private static int initialSize = 5;
    private static int backlog = 200;

    private NioHandleMap handleMap;

    public FirewallServer (int port) {
        this.port = port;
        handleMap = new NioHandleMap();
    }

    public void startServer() {
        // 고정 스레드 풀 생성(threadPoolSize만큼만 사용)
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);

        try {
            // 캐시 스레드 풀 생성
            // 초기에 initialSize만큼 스레드를 만들고 새 스레드는 필요한 만큼 생성한다.
            AsynchronousChannelGroup group = AsynchronousChannelGroup.withCachedThreadPool(executor, initialSize);

            // 모니터링 스레드 생성(2번째 인자는 콘솔 출력 delay(초))
            MyMonitorThread monitor = new MyMonitorThread(executor, 10);
            Thread monitorThread = new Thread(monitor);
            monitorThread.start();

            // 스트림 지향의 리스닝 소켓(비동기 채널)
            AsynchronousServerSocketChannel listener = AsynchronousServerSocketChannel.open(group);
            listener.bind(new InetSocketAddress(port), backlog);

            //
            listener.accept(listener, new NioDispatcher(handleMap));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void registerHandler(String header, String handler) {
        handleMap.put(header, handler);
    }
}
