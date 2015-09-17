package firewallServer;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class NioDispatcher implements CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel> {

	private int HEADER_SIZE = 6;
	private NioHandleMap handleMap;
	
	public NioDispatcher(NioHandleMap handleMap) {
		this.handleMap = handleMap;
	}

	public void completed(AsynchronousSocketChannel channel, AsynchronousServerSocketChannel listener) {
		
		listener.accept(listener, this);

		// 버퍼를 만들고 비동기 콜백 방식으로 읽은 결과를 받아온다.
		ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE);
		channel.read(buffer, buffer, new NioDemultiplexer(channel, handleMap));
	}

	public void failed(Throwable exc, AsynchronousServerSocketChannel listener) {
	}
	
}
