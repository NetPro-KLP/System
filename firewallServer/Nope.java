package firewallServer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.StringTokenizer;

public class Nope implements NioEventHandler {
	private AsynchronousSocketChannel channel;
	
	public String getHandle() {
		return "NULL";
	}

	public int getDataSize() {
		return 512;
	}
	
	public void initialize(AsynchronousSocketChannel channel) {
		this.channel = channel;
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
			String msg = new String(buffer.array());
	        
            System.out.println(msg);

	        try {
	        	buffer.clear();
				channel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void failed(Throwable exc, ByteBuffer attachment) {
	}
}
