
public class Main {

	public static int web_port = 8080;
	public static String default_message = "Hello world!";
	
	public static void main(String[] args) {
		Thread web_server = new ThreadWeb(default_message, web_port);
		web_server.start();
	}

}
