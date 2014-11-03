package hr.fer.dstank;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

/**
 * FTP connection. Can send 1 file.
 * @author Domagoj Stanković
 * @version 1.0
 */
public class SendWorker implements Callable<Void> {

	public static final int FTP_PORT = 21;
	private static final int BUFFER_SIZE = 1024 * 1024;

	private String user;
	private String pass;
	private Socket controlSocket;
	private String serverIP;
	private String file;

	private InputStream input;
	private OutputStream output;

	public SendWorker(String user, String pass, String serverIP, String file) {
		super();
		this.user = user;
		this.pass = pass;
		this.serverIP = serverIP;
		this.file = file;
	}

	private void send() throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.ISO_8859_1));
		PrintWriter pw = new PrintWriter(output);
		
		String message = null;
		
		pw.write("USER " + user + "\n");
		pw.flush();
		do {
			message = reader.readLine();
		} while (!message.startsWith("331"));
		
		pw.write("PASS " + pass + "\n");
		pw.flush();
		message = reader.readLine();
		if (message.charAt(0) != '2') {
			System.err.println("Invalid username or password!");
			System.exit(0);
		}
		
		pw.write("CWD /\n");
		pw.flush();
		reader.readLine();
		
		pw.write("TYPE I\n");
		pw.flush();
		reader.readLine();
		
		pw.write("PASV\n");
		pw.flush();
		message = reader.readLine();
		
		int index1 = message.indexOf('(');
		int index2 = message.indexOf(')');
		String[] parts = message.substring(index1 + 1, index2).split(",");
		String ip = parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3];
		int port = Integer.parseInt(parts[4]) * 256 + Integer.parseInt(parts[5]);
		
		index1 = file.lastIndexOf('/');
		index2 = file.lastIndexOf('\\');
		int index = index1 > index2 ? index1 : index2;
		String fileName = file.substring(index + 1);
		
		pw.write("STOR " + fileName + "\n");
		pw.flush();
		
		Socket dataSocket = new Socket(ip, port);
		OutputStream os = dataSocket.getOutputStream();
		
		InputStream is = new FileInputStream(file);
		byte[] bytes = new byte[BUFFER_SIZE];
		int len;
		long start = System.currentTimeMillis();
		do {
			len = is.read(bytes);
			if (len < 0) {
				break;
			}
			os.write(bytes, 0, len);
		} while (len > 0);
		os.flush();
		long finish = System.currentTimeMillis();
		long diff = finish - start;
		is.close();
		dataSocket.close();
		
		reader.readLine();
		message = reader.readLine();
		if (message.charAt(0) != '2') {
			System.err.println("File transfer failed!");
			return;
		}
		
		double sec = (double) diff / 1000;
		File f = new File(file);
		long fileLength = f.length();
		
		double average = fileLength / sec / 1024;
		String out = fileName + " : " + sec + " s : " + (average) + " KB/s";
		synchronized (System.out) {
			System.out.println(out);
		}
	}

	public Void call() throws Exception {
		try {
			controlSocket = new Socket(serverIP, FTP_PORT);
			input = new BufferedInputStream(controlSocket.getInputStream());
			output = new BufferedOutputStream(controlSocket.getOutputStream());

			send();

			controlSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}
