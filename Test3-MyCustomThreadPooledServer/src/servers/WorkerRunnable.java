package servers;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.StringTokenizer;

public class WorkerRunnable implements Runnable {

	protected Socket clientSocket = null;
	protected String serverText = null;
	BufferedReader input = null; 
	// We used a Buffered Reader for the INPUT as it is easier to read HTTP Requests line by line. 
	// This implies some limitations: in fact we cannot upload non-text file
	// The method DataInputStream.readLine() is in fact deprecated now.
	DataOutputStream output = null;

	static final String HTML_START = "<html>" + "<title>HTTP Server in java</title>" + "<body>";
	static final String HTML_END = "</body>" + "</html>"; //Useful Strings to speed up HTML output writes

	public WorkerRunnable(Socket clientSocket, String serverText) {
		this.clientSocket = clientSocket;
		this.serverText = serverText;
	}

	public void run() {

		try {

			input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			output = new DataOutputStream(clientSocket.getOutputStream());

			// Let's read the FIRST LINE of the HTTP Request
			String requestString = input.readLine();
			String headerLine = requestString;

			//We split the first line in TOKENS, to be able to parse which METHOD was used
			StringTokenizer tokenizer = new StringTokenizer(headerLine);
			String httpMethod = tokenizer.nextToken();
			String httpQueryString = tokenizer.nextToken();

			StringBuffer responseBuffer = new StringBuffer();
			responseBuffer.append("<h1>My Thread-Pooled JAVA Web Server </h1>");
			responseBuffer.append("<h2>This is the HTTP Server Home Page.... </h2><BR>");
			responseBuffer.append("<b>Request handled by " + Thread.currentThread().getName() + "</b><BR><BR><HR>");
			responseBuffer.append("<h3>Usage:</h3>");
			responseBuffer.append("<a href=\"http://localhost:9000\">locahost:9000/</a>: The server home page<BR>");
			responseBuffer.append("<a href=\"http://localhost:9000/img.bmp\">locahost:9000/img.bmp</a>: Get file img.bmp <BR>");
			responseBuffer.append("<a href=\"http://localhost:9000/upload\">locahost:9000/upload</a>: The server upload page<BR><BR><HR>");
			responseBuffer.append("<b>The HTTP Client request is ....</b><BR>");

			System.out.println("The HTTP request string is ....");

			// GET Requests :
			// If we request home page ( i.e. localhost:9000/ ), the server then respond by showing a title and the GET Request

			// Anything else (i.e. localhost:9000/xxxx ) is interpreted as a file. 
			// Try to put some files, either text, images, or binary inside the root directory to be able to download them via browser
			
			// localhost:9000/upload will return a FORM by which you will be able to upload a text file (max size 2MB, only text-file)
			// The SUBMIT button inside the FORM will send a POST request to "localhost:9000/" with the attached text file to upload
			// The POST Handler will read the Content-Length and Content-Type fields, then read the file line by line to write it inside the server root directory

			if (httpMethod.equals("GET")) {
				System.out.println("GET request");
				while (input.ready()) {
					// Read the HTTP complete HTTP Query
					responseBuffer.append(requestString + "<BR>");
					System.out.println(requestString);
					requestString = input.readLine();
				}
				if (httpQueryString.equals("/")) {
					// The default home page
					sendResponse(200, responseBuffer.toString(), false);
				} else if (httpQueryString.equals("/upload")) {
					String responseString = WorkerRunnable.HTML_START
							+ "<h1> Welcome to the Upload page </h1>"
							+ "<h2> Please select a text file to upload </h2>"
							+ "<form action=\"http://127.0.0.1:9000\" enctype=\"multipart/form-data\""
							+ "method=\"post\">" + "Browse computer <input name=\"file\" type=\"file\"><br>"
							+ "<br><input value=\"Upload\" type=\"submit\"></form>"
							+ "<br><br>Back to <a href=\"http://localhost:9000\">home</a> page"
							+ WorkerRunnable.HTML_END;
					sendResponse(200, responseString, false);

				} else {
					// This is interpreted as a file name
					String fileName = httpQueryString.replaceFirst("/", "");
					fileName = URLDecoder.decode(fileName, "UTF-8");
					if (new File(fileName).isFile()) {
						sendResponse(200, fileName, true);
					} else {
						sendResponse(404, "<b>The Requested resource not found ...."
								+ "Usage: http://127.0.0.1:9000 or http://127.0.0.1:9000/</b>", false);
					}
				}
			} else { // POST request
				System.out.println("POST request");

				boolean ready = false;
				String boundary = null;
				PrintWriter fout = null;
				String contentLength = null;
				String uploadFilename = null;

				do {

					if (ready == false) {
						requestString = input.readLine();
						if (requestString.indexOf("Content-Type: multipart/form-data") != -1) {
							boundary = requestString.split("boundary=")[1];
							// The POST boundary
						}

						if (requestString.indexOf("Content-Length:") != -1) {
							contentLength = requestString.split(" ")[1];
							System.out.println("Content Length = " + contentLength);
							// Content length should be < 2MB
							if (Long.valueOf(contentLength) > 2000000L) {
								sendResponse(200, "File size should be < 2MB", false);
							}

						}


						if (requestString.indexOf("--" + boundary) != -1) {
							uploadFilename = input.readLine().split("filename=")[1].replaceAll("\"", "");
							String[] filelist = uploadFilename.split("\\" + System.getProperty("file.separator"));
							uploadFilename = filelist[filelist.length - 1];
							System.out.println("File to be uploaded = " + uploadFilename);
							ready = true;
						}

					} else {
						String fileContentType = input.readLine().split(" ")[1];
						System.out.println("File content type = " + fileContentType);

						input.readLine(); // assert(inFromClient.readLine().equals(""))
											// : "Expected line in POST request
											// is "" ";

						fout = new PrintWriter(uploadFilename);
						String prevLine = input.readLine();
						requestString = input.readLine();

						// Here we upload the actual file contents
						while (true) {
							if (requestString.equals("--" + boundary + "--")) {
								fout.print(prevLine);
								break;
							} else {
								fout.println(prevLine);
							}
							prevLine = requestString;
							requestString = input.readLine();
						}

						sendResponse(200, "File " + uploadFilename + " Uploaded..", false);
						fout.close();
					}
					// if
				} while (input.ready()); // End of do-while
			} // else

			long time = System.currentTimeMillis();
			System.out.println("Processing request n°" + Thread.currentThread().getName());

			/*
			 * try { Thread.sleep(1000 * 4); } catch (InterruptedException e) {
			 * e.printStackTrace(); }
			 */

			// input.close();
			System.out.println("Request n°" + Thread.currentThread().getName() + " processed: " + time);
		} catch (Exception e) {
			// report exception somewhere.
			e.printStackTrace();
		}
	}

	public void sendResponse(int statusCode, String responseString, boolean isFile) throws Exception {

		String statusLine = null;
		String serverdetails = "Server: Java HTTPServer";
		String contentLengthLine = null;
		String fileName = null;
		String contentTypeLine = "Content-Type: text/html" + "\r\n";
		FileInputStream fin = null;

		if (statusCode == 200)
			statusLine = "HTTP/1.1 200 OK" + "\r\n";
		else
			statusLine = "HTTP/1.1 404 Not Found" + "\r\n";

		if (isFile) {
			fileName = responseString;
			fin = new FileInputStream(fileName);
			contentLengthLine = "Content-Length: " + Integer.toString(fin.available()) + "\r\n";
			if (!fileName.endsWith(".htm") && !fileName.endsWith(".html"))
				contentTypeLine = "Content-Type: \r\n";
		} else {
			responseString = WorkerRunnable.HTML_START + responseString + WorkerRunnable.HTML_END;
			contentLengthLine = "Content-Length: " + responseString.length() + "\r\n";
		}

		output.writeBytes(statusLine);
		output.writeBytes(serverdetails);
		output.writeBytes(contentTypeLine);
		output.writeBytes(contentLengthLine);
		output.writeBytes("Connection: close\r\n");
		output.writeBytes("\r\n");

		if (isFile)
			sendFile(fin, output);
		else
			output.writeBytes(responseString);

		output.close();
	}

	public void sendFile(FileInputStream fin, DataOutputStream out) throws Exception {
		byte[] buffer = new byte[1024];
		int bytesRead;

		while ((bytesRead = fin.read(buffer)) != -1) {
			out.write(buffer, 0, bytesRead);
		}
		fin.close();
	}

}