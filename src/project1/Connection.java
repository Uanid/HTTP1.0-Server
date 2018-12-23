package project1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Base64.Decoder;
import java.util.Locale;
import java.util.Map;

public class Connection implements Runnable {

	private static final String SP = " ";
	private static final String CR = "\r";
	private static final String Lf = "\n";
	private static final String CRLF = "\r\n";
	private static final String RFC_1123_PATTERN = "EEE, dd MMM yyyy HH:mm:ss z";
	private static final String RFC_1036_PATTERN = "EEEE, dd-MMM-yy HH:mm:ss z";
	private static final String ASCTIME_PATTERN = "EEE MMM d HH:mm:ss yyyyy";
	private static final DateTimeFormatter RFC_1123_FORMAT;
	private static final DateTimeFormatter RFC_1036_FORMAT;
	private static final DateTimeFormatter ASCTIME_FORMAT;
	private static final Map<String, String> STATUS_MESSAGE;
	private static final ZonedDateTime EXPIRES_DATETIME;

	static {
		ZoneId zone = ZoneId.of("GMT");
		RFC_1123_FORMAT = DateTimeFormatter.ofPattern(RFC_1123_PATTERN, Locale.ENGLISH).withZone(zone);
		RFC_1036_FORMAT = DateTimeFormatter.ofPattern(RFC_1036_PATTERN, Locale.ENGLISH).withZone(zone);
		ASCTIME_FORMAT = DateTimeFormatter.ofPattern(ASCTIME_PATTERN, Locale.ENGLISH).withZone(zone);
		EXPIRES_DATETIME = ZonedDateTime.now();
		STATUS_MESSAGE = new HashMap<>();
		STATUS_MESSAGE.put("200", "OK");
		STATUS_MESSAGE.put("201", "Created");
		STATUS_MESSAGE.put("202", "Accepted");
		STATUS_MESSAGE.put("204", "No Content");
		STATUS_MESSAGE.put("301", "Moved Permanently");
		STATUS_MESSAGE.put("302", "Moved Temporarily");
		STATUS_MESSAGE.put("304", "Not Modified");
		STATUS_MESSAGE.put("400", "Bad Request");
		STATUS_MESSAGE.put("401", "Unauthorized");
		STATUS_MESSAGE.put("403", "Forbidden");
		STATUS_MESSAGE.put("404", "Not Found");
		STATUS_MESSAGE.put("500", "Internal Server Error");
		STATUS_MESSAGE.put("501", "Not implemented");
		STATUS_MESSAGE.put("502", "Bad Gateway");
		STATUS_MESSAGE.put("503", "Service Unavailable");
	}

	private Socket socket;
	private RequestServlet request;
	private ResponseServlet response;
	private long connectionId;

	private InputStream is;
	private BufferedReader br;
	private OutputStream os;

	public Connection(Socket socket) {
		this.socket = socket;
		this.request = new RequestServlet();
		this.response = new ResponseServlet();
		this.connectionId = this.hashCode();

		try {
			this.is = socket.getInputStream();
			this.br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			this.os = socket.getOutputStream();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void run() {
		System.out.println("[" + connectionId + "]" + "Starting connection local: " + socket.getLocalPort() + " port: " + socket.getPort());
		boolean isErrorHappened = false;
		try {
			this.readRequest();
		} catch (Exception e) {
			isErrorHappened = true;
			System.out.println("[" + connectionId + "]" + "파싱중 에러 발생함");
		}
		System.out.println("[" + connectionId + "]" + request.method);
		System.out.println("[" + connectionId + "]" + request.requestURI);
		System.out.println("[" + connectionId + "]" + request.httpVersion);
		for (String key : request.headers.keySet()) {
			System.out.println("[" + connectionId + "]" + key + ": " + request.headers.get(key));
		}
		System.out.println("[" + connectionId + "]Parameter: " + request.parameters.toString());
		System.out.println("[" + connectionId + "]Entity-Body: " + request.body);

		response.httpVersion = request.httpVersion.toUpperCase();

		// controller call
		String resultHtml = null;
		if (isErrorHappened) {
			// 이 때는 항상 400임
			response.statusCode = "400";
			resultHtml = Controller.error(request, response);

		} else if (request.requestURI.equalsIgnoreCase("/files")) {
			resultHtml = Controller.files(request, response);

		} else if (request.requestURI.startsWith("/files/")) {
			Controller.filesDownload(request, response);

		} else if (request.requestURI.equalsIgnoreCase("/favicon.ico")) {
			Controller.favicon(request, response);

		} else if (request.requestURI.equalsIgnoreCase("/login")) {
			resultHtml = Controller.login(request, response);

		} else if (request.requestURI.equalsIgnoreCase("/register")) {
			resultHtml = Controller.register(request, response);

		} else if (request.requestURI.equalsIgnoreCase("/")) {
			resultHtml = Controller.index(request, response);

		} else {
			response.statusCode = "404";
			resultHtml = Controller.error(request, response);
		}

		// html가지고 body 구성
		if (resultHtml == null) {
			// 파일 다운로드
			// 이것도 사실 컨트롤러에서 해야하는데...
			response.contentType = "application/attachments";// 수정해야함
		} else {
			// html파일 전송
			File htmlFolder = new File("html");
			File htmlFile = new File(htmlFolder, resultHtml + ".html");
			StringBuilder sb = new StringBuilder();
			try {
				BufferedReader br = new BufferedReader(new FileReader(htmlFile));
				br.lines().forEach(s -> sb.append(s).append(CRLF));
				br.close();
			} catch (IOException e) {
				System.out.println("[" + connectionId + "]" + "html파일을 찾지 못함");
			}

			response.rawBody = sb.toString().getBytes();
			response.contentLength = response.rawBody.length;
			response.contentType = "text/html";// 수정해야함
		}

		try {
			this.writeResponse();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("[" + connectionId + "]" + "response메시지 작성중 에러 발생함");
		}
		System.out.println("[" + connectionId + "]" + "연결 종료");

		try {
			br.close();
			is.close();

			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void writeResponse() throws IOException {

		// 0.9일땐 header가 없음
		if (!request.httpVersion.equals("HTTP/0.9")) {
			StringBuilder sb = new StringBuilder();

			// status line
			sb.append("HTTP/1.0").append(SP).append(response.statusCode).append(SP)
					.append(STATUS_MESSAGE.get(response.statusCode)).append(CRLF);

			// header lines
			sb.append("Location:").append(SP).append(request.requestURL).append(CRLF);
			sb.append("Server:").append(SP).append("Project1-Server/1.0").append(CRLF);
			sb.append("Date:").append(SP).append(ZonedDateTime.now().format(RFC_1123_FORMAT)).append(CRLF);
			sb.append("Connection:").append(SP).append("close").append(CRLF);

			if (response.statusCode.equals("401")) {
				sb.append("WWW-Authenticate:").append(SP).append("Basic realm=\"Project Users only\"").append(CRLF);
			}

			if (response.statusCode.equals("304")) {
				sb.append("Expires:").append(SP).append(response.expires.format(RFC_1123_FORMAT)).append(CRLF);
				sb.append("Last-Modified:").append(SP).append(response.lastModified.format(RFC_1123_FORMAT)).append(CRLF);
			} else if(response.expires != null){
				sb.append("Expires:").append(SP).append(response.expires.format(RFC_1123_FORMAT)).append(CRLF);
			}else {
				sb.append("Expires:").append(SP).append(EXPIRES_DATETIME.format(RFC_1123_FORMAT)).append(CRLF);
			}

			if (response.contentType != null) {
				sb.append("Content-Type:").append(SP).append(response.contentType).append(CRLF);
				sb.append("Content-Language:").append(SP).append("en-US").append(CRLF);
				// sb.append("Cache-Control:").append(SP).append("private").append(CRLF);
			}

			if (response.contentLength != 0) {
				sb.append("Content-Length:").append(SP).append(response.contentLength).append(CRLF);
			}

			sb.append(CRLF);

			byte[] head = sb.toString().getBytes(Charset.forName("UTF-8"));
			os.write(head);
			// os.flush();
		}

		// head가 아닐 때만 body첨부
		if (!request.method.equalsIgnoreCase("head")) {
			if (response.contentLength != 0 && response.rawBody.length != 0) {
				os.write(response.rawBody);
			}
		}
	}

	// 요청 메시지를 읽어들이는 부분!
	private void readRequest() throws Exception {
		// 헤더 raw로 가져오기
		ArrayList<String> rawHead = new ArrayList<>();
		String s;
		while ((s = br.readLine()) != null) {
			if (s.equals("")) {
				break;
			}
			// 요청 메시지 한 줄씩 읽어서 문자열 배열에 저장
			rawHead.add(s);
		}

		if (rawHead.size() == 0) { // 요청메시지가 없을 겅우
			// 소켓 연결만 해놓고 데이터 안보내고 튐
			// bad request... 보낼 소켓도 없지만
			System.out.println("[" + connectionId + "]" + " 클라이언트가 소켓 연결만 했다가 데이터 안보내고 튀었음");
			throw new Exception("400");

		}

		// 맨 첫줄 처리
		// 요청 메시지의 첫 줄 부분 구분하여 나누기(메소드, URI, http버전)
		s = rawHead.get(0);
		String[] requestLine = s.split(SP);
		if (requestLine.length == 2) { // 구분이 2개인 경우(메소드, URI만 있는 경우)
			this.request.method = requestLine[0].toUpperCase();
			this.request.requestURI = requestLine[1].toLowerCase();
			this.request.httpVersion = "HTTP/0.9"; // http버전은 0.9
			this.request.requestURL = "http://localhost" + request.requestURI;
		} else if (requestLine.length == 3) {
			this.request.method = requestLine[0].toUpperCase();
			this.request.requestURI = requestLine[1].toLowerCase();
			this.request.httpVersion = requestLine[2].toUpperCase();
			this.request.requestURL = "http://localhost" + request.requestURI;
		} else {
			// http0.9도 1.0도 아님
			// bad request
			System.out.println("[" + connectionId + "]" + " simple request도 full request도 이도저도 아님");
			throw new Exception("400");
		}

		// 요청메시지에 임의로 날짜 타입 3가지 헤더 삽입
		// rawHead.add("DATE: Sun, 06 Nov 1994 08:49:30 GMT");

		// 헤더 처리
		int contentLength = 0;
		Map<String, String> headers = this.request.headers;
		// 요청 메시지 첫 줄은 처리했으므로 2번째 부터 읽어서 구분하기
		for (int i = 1; i < rawHead.size(); i++) {
			String line = rawHead.get(i);
			int index = line.indexOf(':'); // 구분자로 헤더명과 값 나누기
			String name = line.substring(0, index).trim(); // 헤더명
			String value = line.substring(index + 1).trim(); // 값
			headers.put(name, value); // RequestServlet의 헤더 맵에 추가

			// 날짜 처리(일반 헤더의 날짜와 If-Modified-Since의 날짜)
			if (name.equalsIgnoreCase("date") || name.equalsIgnoreCase("if-modified-since")) {

				ZonedDateTime zdt = null;
				try {
					zdt = ZonedDateTime.from(RFC_1123_FORMAT.parse(value));
				} catch (Exception e) {
					try {
						zdt = ZonedDateTime.from(RFC_1036_FORMAT.parse(value));
					} catch (Exception e2) {
						try {
							zdt = ZonedDateTime.from(ASCTIME_FORMAT.parse(value));
						} catch (Exception e3) {
							// bad request
							// 날짜 파싱 불가능
							throw new Exception("400");
						}
					}
				}
				if (name.equalsIgnoreCase("date")) {
					this.request.date = zdt;
				} else if (name.equalsIgnoreCase("if-modified-since")) {
					this.request.ifModSince = zdt;
				}
			}

			// 사용자 인증에 대한 부분 디코딩
			if (name.equalsIgnoreCase("authorization")) {
				String[] vValue = value.split(SP);
				if (vValue.length > 1) {
					if (vValue[0].equalsIgnoreCase("basic")) {
						Decoder decoder = Base64.getDecoder();

						String credentials = new String(decoder.decode(vValue[1]));
						int dist = credentials.indexOf(':'); // 구분자로 이름과 암호 나누기
						this.request.uid = credentials.substring(0, dist).trim(); // 이름
						this.request.upw = credentials.substring(dist + 1).trim(); // 암호
					} else {
						System.out.println("올바르지 않은 Authrization 알고리즘");
						throw new Exception("400");
					}
				} else {
					System.out.println("부족한 Authorization 필드임");
					throw new Exception("400");
				}
			}

			if (name.equalsIgnoreCase("content-length")) {
				try {
					contentLength = Integer.parseInt(value);
				} catch (Exception e) {
					// 여기서 오류 발생시 bad request
					System.out.println("content-length value가 정수가 아님");
					throw new Exception("400");
				}
			}
		}

		// 바디 raw로 가져오기
		// ByteBuffer rawBody = ByteBuffer.allocate(1024);
		StringBuilder rawBody = new StringBuilder(1024);
		// byte[] buf = new byte[1024];
		char[] buf = new char[1024];
		int count = 0;
		int r = 0;
		System.out.println("avail " + is.available());
		while (count < contentLength) {
			if (count + buf.length < contentLength) {
				r = br.read(buf, 0, buf.length);
				// r = is.read(buf, 0, buf.length);
				count += r;
			} else {
				int remainLength = contentLength - count;
				System.out.println("remain: " + remainLength);
				r = br.read(buf, 0, remainLength);
				// r = is.read(buf, 0, remainLength);
				count += r;
			}
			if (r == -1) {
				for (int i = 0; i < 50; i++) {
					// System.out.println(buf[i] + ", " + (int)buf[i]);
				}
				System.out.println("[" + connectionId + "]" + " body 읽는데 content-length 불일치 발생");
				throw new Exception("400");
			}
			// rawBody.put(buf, 0, r);
			rawBody.append(buf, 0, r);
		}

		// 진짜 바디
		// this.request.rawBody = rawBody.array();

		// 원래 인코딩 알고리즘 타입보고 해야하는데, PUT같은걸 구현할게 아니라서 그냥 직접 씀
		// this.request.body = new String(this.request.rawBody,
		// Charset.forName("UTF-8"));
		this.request.body = rawBody.toString();

		// parameter 파싱
		String parameterLine = null;
		if (this.request.method.equalsIgnoreCase("get") || this.request.method.equalsIgnoreCase("head")) {
			int index = this.request.requestURI.indexOf('?');
			if (index != -1) {
				parameterLine = this.request.requestURI.substring(index + 1);
				this.request.requestURI = this.request.requestURI.substring(0, index);
			}
		} else if (this.request.method.equalsIgnoreCase("post")) {
			if (this.request.body.trim().length() != 0) {
				parameterLine = this.request.body.trim();
			}
		}

		if (parameterLine != null) {
			String[] args = parameterLine.split("&");
			for (String arg : args) {
				int index = arg.indexOf('=');
				String name = arg.substring(0, index).trim();
				String value = arg.substring(index + 1).trim();
				this.request.parameters.put(name, value);
			}
		}
	}

}
