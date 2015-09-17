package xmlReader;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;

// HandlerList데이터에서 각각 handler의 헤더 속성값과 jar파일 경로를 반환한다.

public class HandlerData {
	@Attribute(required=false)  
    private String header;
	
	@Text 
	private String text;
	
	public String getHeader() {
		return header;
	}
	
	public String getHandler() {
		return text;
	}
}
