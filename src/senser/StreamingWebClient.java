package senser;

// java platform imports
import java.io.BufferedInputStream;
import java.net.*;
import java.net.URLConnection;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.*;

public class StreamingWebClient {
	private String url;
	private int size;
	private LinkedBlockingQueue<Byte> q;
	private URLConnection con;
	private BufferedInputStream bis;

	public StreamingWebClient(String webResource, int expectedBufferSize) {
		url = webResource;
		size = expectedBufferSize;
		// realization of the observable pattern via queue
		q = new LinkedBlockingQueue<Byte>();
		byte[] buffer = new byte[size];
		System.err.println(this.getClass().getName() + ": Connecting to " + url + "...");
		// make a producer thread that fills a queue
		Runnable producer = new Runnable() {
			public void run() {
				try {
					con = new URL(url).openConnection();
					
					con.setConnectTimeout(20);
					con.setReadTimeout(200);
					bis = new BufferedInputStream(con.getInputStream());
					int bytesRead = 0;
					while ((bytesRead = bis.read(buffer)) != -1) {
						String message = new String(buffer, 0, bytesRead);
						for (byte b : message.getBytes()) {
							q.add(b);
						}
					}
				} catch (Exception e) {
					System.err.println("No connection Exception");
					Path path = Paths.get("adscapt.txt");
					try {
						byte[] data = Files.readAllBytes(path);
						System.err.println("Using file:" + path);
						for (byte b : data) {
							Thread.sleep(10);
							q.add(b);
						}
					} catch (Exception io) {}
					
				}
			} // run
		}; // producer thread
		new Thread(producer).start();
	} // constructor

	public Byte read() {
		Byte b = null;
		// consumes the queue
		try {
			b = q.take();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return b;
	} // readln

	public String readChunk(String filter) {
		String chunk = "";
		Pattern pattern = Pattern.compile(filter);
		Matcher matcher;
		do {
			chunk = chunk + (char) read().byteValue();
			matcher = pattern.matcher(chunk);
		} while (!matcher.find());
		return matcher.group();
	}

	public void finalize() {
		System.err.println("\n" + this.getClass().getName() + ": Disconnecting from " + url + "...");
		try {
			bis.close();
		} catch (Exception e) {
		}
	}

} // class
