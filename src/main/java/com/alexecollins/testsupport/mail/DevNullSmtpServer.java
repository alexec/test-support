package com.alexecollins.testsupport.mail;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author alexec (alex.e.c@gmail.com)
 */
public class DevNullSmtpServer implements Runnable, Closeable {

	private ServerSocket server;

	public static void main(String[] args) throws Exception {
		new DevNullSmtpServer().run();
	}

	public void start() {
		new Thread(this, "DevNullSmtpServer").start();
	}

	@Override
	public void close() throws IOException {
		server.close();
	}

	@Override
	public void run() {
		log("running");
		try {
			loop();
		} catch (IOException e) {
			System.err.println(e);
		}
		log("closed");
	}

	private void log(String text) {
		System.out.println(this.getClass().getSimpleName() + " " + text);
	}

	private void loop() throws IOException {
		server = new ServerSocket(10587);
		while (!server.isClosed()) {
			final Socket socket = server.accept();
			final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			final PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
			println(out, "220 " + InetAddress.getLocalHost().getHostAddress() + " ESMTP Postfix");
			String line = null;
			boolean data = false;
			while (!"QUIT".equals(line) && (line = readLine(in)) != null) {
				if ("DATA".equals(line)) {
					println(out, "354 End data with <CR><LF>.<CR><LF>");
					data = true;
					continue;
				} else if ("QUIT".equals(line)) {
					println(out, "221 Bye");
					continue;
				} else if (".".equals(line))
					data = false;

				if (!data)
					println(out, "250 Ok");
			}

			socket.close();
		}
	}

	private String readLine(BufferedReader in) throws IOException {
		final String line = in.readLine();
		log("C: " + line);
		return line;
	}

	private void println(PrintWriter out, String text) {
		log("S: " + text);
		out.println(text);
		out.flush();
	}
}
