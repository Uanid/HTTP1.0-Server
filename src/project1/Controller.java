package project1;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class Controller {

	public static String error(RequestServlet request, ResponseServlet response) {
		if (response.statusCode.equals("404")) {
			return "notfound";
		} else if (response.statusCode.equals("400")) {
			return "badrequest";
		}
		return "servererror";
	}

	public static void favicon(RequestServlet request, ResponseServlet response) {
		System.out.println("파비콘 호출됨");
		response.statusCode = "301";
		request.requestURL = "http://localhost/files/favicon.ico";
		response.expires = ZonedDateTime.now().plus(1, ChronoUnit.YEARS);
	}

	public static String files(RequestServlet request, ResponseServlet response) {
		if (users.containsKey(request.uid)) {
			String pw = users.get(request.uid);
			if (pw.equals(request.upw)) {
				response.statusCode = "200";
				return "files";
			} else {
				response.statusCode = "403";
				return "forbidden";
			}
		} else {
			response.statusCode = "403";
			return "forbidden";
		}
	}

	public static void filesDownload(RequestServlet request, ResponseServlet response) {
		String filename = request.requestURI.replace("/files/", "");
		File file = new File("files/" + filename);
		try {
			String mimeType = Files.probeContentType(file.toPath());

			response.statusCode = "200";
			response.contentType = mimeType;

			InputStream is = new FileInputStream(file);
			int size = is.available();
			byte[] arr = new byte[size];
			is.read(arr, 0, size);
			response.rawBody = arr;
			is.close();
			response.contentLength = size;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String login(RequestServlet request, ResponseServlet response) {
		if (users.containsKey(request.uid)) {
			String pw = users.get(request.uid);
			if (pw.equals(request.upw)) {
				response.statusCode = "200";
				return "login_success";
			} else {
				response.statusCode = "401";
				return "login";
			}
		} else {
			response.statusCode = "401";
			return "login";
		}
	}

	private static Map<String, String> users = new HashMap<>();

	public static String register(RequestServlet request, ResponseServlet response) {
		response.statusCode = "200";
		System.out.println("파라미터: " + request.parameters.toString());

		if (!request.parameters.containsKey("id") || !request.parameters.containsKey("pw")) {
			System.out.println("아무것도 인자로 입력하지 않음");
			return "register";
		}

		String id = request.parameters.get("id");
		String pw = request.parameters.get("pw");

		id = id.toLowerCase();

		if (users.containsKey(id)) {
			return "register_fail";
		}
		
		//회원가입 시 아이디, 암호 둘 중 하나를 입력하지 않은 경우
		if(id.equals("") || pw.equals(""))
			return "register";

		users.put(id, pw);
		System.out.println("id: " + id + ", pw: " + pw + " 회원가입 완료");
		return "register_success";
	}

	public static String index(RequestServlet request, ResponseServlet response) {
		// response.contentType = "text/plain";
		response.statusCode = "200";
		return "index";
	}

}