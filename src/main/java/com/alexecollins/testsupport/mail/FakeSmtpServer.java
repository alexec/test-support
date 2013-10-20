package com.alexecollins.testsupport.mail;

import lombok.Data;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * @author alexec (alex.e.c@gmail.com)
 */
public class FakeSmtpServer implements Runnable, Closeable {

	@Data
	public static class Message {
		private String from;
		private String[] to;
		private String subject;
		private String text;
	}

	private ServerSocket server;
	private final List<Message> messages = new ArrayList<Message>();

	public static void main(String[] args) throws Exception {
		new FakeSmtpServer().run();
	}

	public Message lastMessage() {
		return messages.get(messages.size() - 1);
	}

	public void start() {
		new Thread(this, "FakeSmtpServer").start();
	}

	public void close() throws IOException {
		server.close();
		messages.clear();
	}

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
			boolean text = false;
			final Message message = new Message();
			while (!"QUIT".equals(line) && (line = readLine(in)) != null) {
				if ("DATA".equals(line)) {
					println(out, "354 End data with <CR><LF>.<CR><LF>");
					data = true;
					continue;
				} else if ("QUIT".equals(line)) {
					println(out, "221 Bye");
					continue;
				} else if (".".equals(line)) {
					data = false;
				}

				if (!data) {
					println(out, "250 Ok");
				} else {
					if (line.startsWith("From: ")) {
						message.setFrom(line.substring(6));
					}
					if (line.startsWith("To: ")) {
						message.setTo(line.substring(4).split(", "));
					}
					if (line.startsWith("Subject: ")) {
						message.setSubject(line.substring(9));
					}
					if (line.isEmpty()) {
						text = true;
					} else if (text) {
						message.setText((message.getText() != null ? message.getText() + "\n" : "") + line);
					}
				}
			}

			messages.add(message);

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
