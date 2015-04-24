public class ThreadWeb extends Thread {

	public String message;
	public String message_location;
	public int port;
	
	public ThreadWeb(String message, String message_location, int port) {
		this.message = message;
		this.message_location = message_location;
		this.port = port;
	}

	public void run() {
		WebServer ws = new WebServer(this.port, this.message, this.message_location);
		ws.start();
	}

}
