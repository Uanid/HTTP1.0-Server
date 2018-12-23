package project1;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

public class ResponseServlet {

	public String statusCode = "";
	public String httpVersion;
	
	//선택적 헤더
	public String contentType = null;
	public int contentLength = 0;
	public ZonedDateTime expires = null;
	public ZonedDateTime lastModified = null;
	
	//바디
	public byte[] rawBody;
}
