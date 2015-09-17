package xmlReader;

import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

// HandlerList데이터에서 handler에 관한 데이터들을 리스트형태로 반환한다.
public class HandlerListData {
	
	@ElementList(entry="handler", inline=true)
	private List<HandlerData> handler;
	
	@Attribute
	private String name;
	
	
	public List<HandlerData> getHandler() {
		return handler;
	}
	
	public String getName() {
		return name;
	}
	
}
