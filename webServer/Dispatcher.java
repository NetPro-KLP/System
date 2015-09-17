package webServer;

import javax.websocket.Session;

public interface Dispatcher {
	public void dispatch(Session serverSession, HandleMap handlers);
}
