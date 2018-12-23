package project1;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

public class RequestServlet {
	
	public String requestURI = "";
	public String requestURL = "";
	public String httpVersion = "";
	public String method = "";
	public ZonedDateTime date = null;
	public ZonedDateTime ifModSince = null;
	public String uid = "";
	public String upw = "";
	public int contentLength = 0;
	
	public Map<String, String> parameters = new HashMap<>();
	public Map<String, String> headers = new HashMap<>();
	public Map<String, String> credentials = new HashMap<>();
	
	public String body = "";
	
	//PUT이 없기 때문에 안 쓰게 됨
	@Deprecated
	public byte[] rawBody = null;
}
