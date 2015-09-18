package firewallListener;

import java.io.IOException;
import java.io.InputStream;

public class EventHandler {

    private static final int DATA_SIZE = 512;

	public void NopeEvent(InputStream inputStream) {
        try {
            byte[] buffer = new byte[DATA_SIZE];
            inputStream.read(buffer);
            String data = new String(buffer);

            System.out.println(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
