public class ThreadWeb extends Thread {

	public String message;
	public int port;
	public ThreadWeb(String message, int port) {
		this.message = message;
		this.port = port;
	}

	public void run() {
		WebServer ws = new WebServer(this.port, this.message);
		ws.start();
	}

}
