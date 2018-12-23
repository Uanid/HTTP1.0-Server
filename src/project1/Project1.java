package project1;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Project1 {

	public static int port = 80;
	public static boolean running = true;

	public static void main(String[] args) {
		System.out.println("Starting server...");

		ServerSocket server;
		try {
			server = new ServerSocket(port);
		} catch (IOException e) {
			System.out.println("서버소켓을 열 수 없음");
			e.printStackTrace();
			return;
		}

		while (running) {
			Socket socket;
			try {
				socket = server.accept();
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("클라이언트와의 컨넥션 소켓을 열 수 없음");
				continue;
			}
			
			Connection conn = new Connection(socket);
			new Thread(conn).start();
		}

		try {
			server.close();
		} catch (IOException e) {
			System.out.println("서버 소켓을 닫을 수 없음");
			e.printStackTrace();
		}
	}
}
