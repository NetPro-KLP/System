package firewallServer;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public interface NioEventHandler extends CompletionHandler<Integer, ByteBuffer> {
    public String getHandle();
    public int getDataSize();
	public void initialize(AsynchronousSocketChannel channel);
}
