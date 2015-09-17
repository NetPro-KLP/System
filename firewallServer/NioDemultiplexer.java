package firewallServer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class NioDemultiplexer implements CompletionHandler<Integer, ByteBuffer> {
	
	private AsynchronousSocketChannel channel;
	private NioHandleMap handleMap;
	
	public NioDemultiplexer(AsynchronousSocketChannel channel, NioHandleMap handleMap) {
		this.channel = channel;
		this.handleMap = handleMap;
	}

	public void completed(Integer result, ByteBuffer buffer) {
		
		if (result == -1) {
			try {
				channel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (result > 0) {
			buffer.flip();

			String header = new String(buffer.array());
			
			try {
				NioEventHandler handler = (NioEventHandler)Class.forName( handleMap.get(header) ).newInstance();

				ByteBuffer newBuffer = ByteBuffer.allocate(handler.getDataSize());
				handler.initialize(channel);
				channel.read(newBuffer, newBuffer, handler);
				
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	public void failed(Throwable exc, ByteBuffer buffer) {
	}
	
}
