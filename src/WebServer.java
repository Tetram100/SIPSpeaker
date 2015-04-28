import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;


public class WebServer {

	int port;
	String message;
	String message_location;

	public WebServer(int port, String message, String message_location) {
		this.port = port;
		this.message = message;
		this.message_location = message_location;
	}

	protected void start() {
		String www = "Web/";
		ServerSocket s;

		System.out.println("The Webserver is starting up on port " + port);
		System.out.println("(press ctrl-c to exit java program)");
		try {
			// create the main server socket
			s = new ServerSocket(port);
		} catch (Exception e) {
			System.out.println("Error: " + e);
			return;
		}

		System.out.println("Waiting for connection");

		while (true) {
			Socket connection = null;
			try {
				//We wait for a connection
				connection = s.accept();	
				System.out.println("Connection, sending data.");

				//We read the input and prepare the output
				BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				OutputStream output = new BufferedOutputStream(connection.getOutputStream());
				PrintStream poutput = new PrintStream(output);

				//We interpret the input
				boolean error = false;
				String request = input.readLine();

				//We check the header of the request
				if (request == null) {
					error = true;
					writeError(poutput, connection, 400, "Bad Request", "Your http request in incorrect.");
					//We check the method
				} else if (!(request.startsWith("GET") || request.startsWith("POST"))){
					error = true;
					writeError(poutput, connection, 405, "Method Not Allowed", "The server supports only GET and POST methods.");
					// We check the HTTP version
				} else if (!(request.endsWith("HTTP/1.0") || request.endsWith("HTTP/1.1"))){
					error = true;
					writeError(poutput, connection, 505, "HTTP Version not supported", "The server supports only HTTP/1.0 and HTTP/1.1."); 
					//The GET method for sending a page
				} else if (request.startsWith("GET")){
					//We extract the url asked and manage the root page case
					String arr[] = request.split(" ");
					String url = arr[1];
					if (url.equals("/")){
						url = "index.html";
					}
					System.out.println("The user ask for the page " + url);
					//We check if the user is trying to access to a page outside the java server
					if (url.indexOf("..")!=-1 || url.indexOf("/.ht")!=-1 || url.endsWith("~")) {
						error = true;
						writeError(poutput, connection, 403, "Forbidden", "You don't have access to this page.");
					} else if (url.equals("index.html")){
						sendPage(poutput, this.message);
						output.flush();
						connection.close();	
					} else {
						//We delete the "/" in the url
						if (url.startsWith("/")){
							url = url.substring(1);
						}
						url = www + url;
						String filePath = url;
						File f = new File(filePath);
						//We check if the page exist
						if(!(f.exists() && !f.isDirectory())){
							System.out.println("Page doesn't exist");
							error = true;
							writeError(poutput, connection, 404, "Not Found", "This page doesn't exist. Please check the url.");
						} else {
							System.out.println("sending the page");
							//We send the page
							InputStream page = new FileInputStream(f);
							//We write the HTTP header
							poutput.println("HTTP/1.1 200 OK");
							String extension = url.replaceAll("^.*\\.([^.]+)$", "$1");
							poutput.println("Content-Type: " + contentType(extension) + "; ; charset=utf-8");
							//With the blank line we finish the header
							poutput.println("");
							//The rest of the page
							sendFile(page, output); // send raw file
							output.flush();
							connection.close();
						}
					}
				} else if (request.startsWith("POST")){
					//We find the url
					String head[] = request.split(" ");
					String url = head[1];
					if (url.startsWith("/")){
						url = url.substring(1);
					}
					System.out.println("The user post on the page " + url);

					//We download the post body with the exact number of byte from the Content-Length field
					String temp;
					int contentLength = 0;
					while (!(temp = input.readLine()).equals("")){
						if (temp.startsWith("Content-Length:")){
							String arr[] = temp.split(" ");
							contentLength = Integer.parseInt(arr[1]);
						}
					}
					StringBuilder requestContent = new StringBuilder();
					for (int i = 0; i < contentLength; i++)
					{
						requestContent.append((char) input.read());
					}
					String body = requestContent.toString();
					String request_cut[] = body.split("&");

					//The case it's for update the SIP message
					if (url.equals("update_message")){
						boolean false_form = false;

						String message = "";

						//For each field we decode the post request and change the default value if it's not empty
						for(String param : request_cut){
							if (param.startsWith("message")){
								String block_message[] = param.split("=",2);
								if (block_message.length == 2){
									message = java.net.URLDecoder.decode(block_message[1], "UTF-8").trim();
								} else {
									false_form = true;
								}
							}
						}
						//If the user didn't fill in every field
						if (false_form == true){
							error = true;
							writeError(poutput, connection, 400, "Bad request", "Please fill in a right way the field in the form.");
						} else {
							//We update the message
							this.message = message;
							//We create the WAVE file if the message is not empty
							String response;
							if (!(message.equals(""))) {
								synchronized(ThreadSIPServer.message_content){
									ThreadSIPServer.message_content = message;
								}
								FreeTTS freetts = new FreeTTS(message);
								response = freetts.writeMessage(this.message_location);
							} else {
								synchronized(ThreadSIPServer.message_content){
									ThreadSIPServer.message_content = "";
								}
								response = "OK";
							}
							System.out.println("The SIP message has been updated.");
							System.out.println("New message: " + message);

							System.out.println("Response of the wav creation:" + response);
							//We send the response
							writeUpdateReponse(poutput, response);

							output.flush();
							connection.close();	
						}
					} else {
						System.out.println("Page doesn't exist");
						error = true;
						writeError(poutput, connection, 404, "Not Found", "This page doesn't exist. Please check the url.");           		
					}
				}  
				if (error == true){
					output.flush();
					connection.close();	            	
				}     
			} catch (Exception e) {
				System.out.println("Error: " + e);
			}
		}

	}

	private static void writeError(PrintStream poutput, Socket connection, int code_error, String error_title, String error_message){
		poutput.println("HTTP/1.1 " + code_error + " " + error_title);
		poutput.println("Content-Type: text/html; ; charset=utf-8");
		poutput.println("");
		poutput.println("<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Strict//EN' 'http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd'>");
		poutput.println("<html xmlns='http://www.w3.org/1999/xhtml'>");
		poutput.println("<head>");
		poutput.println("<meta http-equiv='Content-Type' content='text/html; charset=utf-8' />");
		poutput.println("<title> Error " + code_error + " - Webmail</title>");
		poutput.println("</head>");
		poutput.println("<body>");
		poutput.println("<h1> Error " + code_error + " - " + error_title + "</h1>");
		poutput.println(error_message);
		poutput.println("</body>");
		poutput.println("</html>");
	}

	private static void writeUpdateReponse(PrintStream poutput, String response){
		poutput.println("HTTP/1.1 200 OK");
		poutput.println("Content-Type: text/html; ; charset=utf-8");
		poutput.println("");
		poutput.println("<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Strict//EN' 'http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd'>");
		poutput.println("<html xmlns='http://www.w3.org/1999/xhtml'>");
		poutput.println("<head>");
		poutput.println("<meta http-equiv='Content-Type' content='text/html; charset=utf-8' />");
		poutput.println("<link rel='icon' type='image/x-icon' href='favicon.ico' />");
		poutput.println("<script src='jquery-1.11.2.js'></script>");
		poutput.println("<link href='bootstrap.min.css' rel='stylesheet'>");
		poutput.println("<script src='bootstrap.min.js'></script>");
		poutput.println("<title> Webmail</title>");
		poutput.println("</head>");
		poutput.println("<body>");
		poutput.println("<div class = 'container well'>");
		if (response.equals("OK")){
			poutput.println("<div class='alert alert-success'>");
			poutput.println("The message has been successfully updated.");
		} else {
			poutput.println("<div class='alert alert-danger'>");
			poutput.println("ERROR : " + response);
		}
		poutput.println("</div>");
		poutput.println("</div>");
		poutput.println("</body>");
		poutput.println("</html>");	
	}

	private void sendPage(PrintStream poutput, String message){
		poutput.println("HTTP/1.1 200 OK");
		poutput.println("Content-Type: text/html; ; charset=utf-8");
		poutput.println("");
		poutput.println("<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Strict//EN' 'http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd'>");
		poutput.println("<html xmlns='http://www.w3.org/1999/xhtml'>");
		poutput.println("<head>");
		poutput.println("<meta http-equiv='Content-Type' content='text/html; charset=utf-8' />");
		poutput.println("<link rel='icon' type='image/x-icon' href='favicon.ico' />");
		poutput.println("<script src='perso.js'></script>");
		poutput.println("<script src='jquery-1.11.2.js'></script>");
		poutput.println("<link href='bootstrap.min.css' rel='stylesheet'>");
		poutput.println("<script src='bootstrap.min.js'></script>");
		poutput.println("<title> SIP Message</title>");
		poutput.println("</head>");
		poutput.println("<body>");
		poutput.println("<div class = 'container well'>");
		poutput.println("<div class = 'panel panel-default'>");
		poutput.println("<div class='panel-heading'>");
		poutput.println("<div class='btn-group pull-right'>");
		poutput.println("<button class='btn btn-warning btn-xs' style='display: inline-block;' onclick='reset_form();'>Reset</button>");
		poutput.println("</div>");
		poutput.println("<h4 class='panel-title'>Update the SIP message</h4>");
		poutput.println("</div>");
		poutput.println("<div class='panel-body'>");
		poutput.println("<form accept-charset='UTF-8' id='mail_form' action='/update_message' class='form-horizontal' method='post'>");
		poutput.println("<div class='form-group'>");
		poutput.println("<label class='col-lg-2 control-label' for='message'>Message</label>");
		poutput.println("<div class='col-lg-10'>");
		poutput.println("<textarea class='form-control' id='message' name='message' placeholder='Enter your message' type='text' rows='10'/>");
		poutput.println(message + "</textarea>");
		poutput.println("</div>");
		poutput.println("</div>");
		poutput.println("<hr>");
		poutput.println("<div class='form-actions text-center'>");
		poutput.println("<input class='btn btn-primary' name='Submit' type='submit' value='Update the message' />");
		poutput.println("</div>");
		poutput.println("</form>");
		poutput.println("</div>");
		poutput.println("</div>");
		poutput.println("</div>");
		poutput.println("<p class='text-center'>Guillaume Dhainaut - Thomas Fouqueray</p>");
		poutput.println("</body>");
		poutput.println("</html>");
	}


	//To find the right content type for the file extension
	private String contentType(String extension) {
		if (extension.equals("js")){
			return "application/javascript";
		} else if (extension.equals("css")){
			return "text/css";
		} else if (extension.equals("ico")){
			return "image/jpeg";
		} else {
			return "text/html";
		}
	}

	//To write the file in the outputstream
	private static void sendFile(InputStream file, OutputStream out)
	{
		try {
			byte[] buffer = new byte[1000];
			while (file.available()>0) 
				out.write(buffer, 0, file.read(buffer));
		} catch (IOException e) { System.err.println(e); }
	}	

}
