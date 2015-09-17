package xmlReader;

import java.util.List;
import org.simpleframework.xml.ElementList;

// HandlerList데이터에서 server에 관한 노드를 반환한다.
public class ServerListData {

	@ElementList(entry="server", inline=true)
	private List<HandlerListData> server;
	
	public List<HandlerListData> getServer() {
		return server;
	}
}
