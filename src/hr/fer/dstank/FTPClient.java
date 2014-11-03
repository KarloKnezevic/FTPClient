package hr.fer.dstank;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * FTP client.
 * @author Domagoj Stanković
 * @version 1.0
 */
public class FTPClient {
	
	/**
	 * FTP client start point. 5 files can be sent concurrently.
	 * @param args Flags: username (-u), password (-p), server ip address (-server), paths to files delimited with ';' (-files)
	 */
	public static void main(String[] args) {
		int len = args.length;
		String user = "user";
		String pass = "pass";
		String serverIP = "127.0.0.1";
		String filesStr = "";
		
		for (int i = 0; i < len; i += 2) {
			String flag = args[i];
			String content = args[i + 1];
			if (flag.equals("-u")) {
				user = content;
			} else if (flag.equals("-p")) {
				pass = content;
			} else if (flag.equals("-server")) {
				serverIP = content;
			} else if (flag.equals("-files")) {
				filesStr = content;
			} else {
				error();
			}
		}
		
		String[] files = filesStr.split(";");
		send(user, pass, serverIP, files);
	}
	
	/**
	 * Sends given files.
	 * @param user Username
	 * @param pass Password
	 * @param serverIP Server IP address
	 * @param files Paths to files
	 */
	private static void send(String user, String pass, String serverIP, String[] files) {
		try {
			int size = files.length;
			int procNum = Runtime.getRuntime().availableProcessors();
			long length = 0;
			
			ExecutorService es = Executors.newFixedThreadPool(procNum);
			List<SendWorker> tasks = new ArrayList<SendWorker>(size);
			for (int i = 0; i < size; i++) {
				tasks.add(new SendWorker(user, pass, serverIP, files[i]));
				length += new File(files[i]).length();
			}
			long start = System.currentTimeMillis();
			
			es.invokeAll(tasks);
			
			long finish = System.currentTimeMillis();
			
			es.shutdown();
			
			long diff = finish - start;
			double sec = (double) diff / 1000;
			double average = length / sec / 1024;
			System.out.println("Total: " + sec + " s : " + average + " KB/s");
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}

	private static void error() {
		System.err.println("Error!");
		System.exit(0);
	}

}
