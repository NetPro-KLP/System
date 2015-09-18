package firewallListener;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class Demultiplexer implements Runnable {

	private final int HEADER_SIZE = 4;
	
	private Socket socket;
	
	public Demultiplexer(Socket socket) {
		this.socket = socket;
	}
	
	public void run() {
		try {
			InputStream inputStream = socket.getInputStream();
			
			byte[] buffer = new byte[HEADER_SIZE];
			inputStream.read(buffer);
			String header = new String(buffer);
            System.out.println(header);
            EventHandler eventHandler = new EventHandler();

            switch(header) {
                case "NULL":
                    eventHandler.NopeEvent(inputStream);
                    break;
                default:
                    break;
            }
			
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
