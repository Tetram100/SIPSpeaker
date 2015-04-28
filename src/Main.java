import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class Main {

	//Default value used in case of failure of the default config file
	public static int http_port = 8080;
	public static int sip_port = 5060;
	public static String sip_user = "sipspeaker";
	public static String sip_interface = "0.0.0.0";
	public static String http_interface = "127.0.0.1";
	public static String default_message = "Welcome to SIP Speaker. This is my own answering machine. You have no new message.";
	public static String message_location = "message";
	public static String fileName = "sipspeaker.cfg";

	public static void main(String[] args) throws Exception{

		//First we read the default one
		try {
			readConf(fileName);
		} catch (Exception e) {
			System.out.println("Can't read the default val.");
		}
		//We try to read the arguments
		try {
			//We read the input from the command line interface
			for (int i = 0; i < args.length; i = i+2) {

				switch (args[i])
				{
				case "-c":
					String config_file_name = args[i+1];
					readConf(config_file_name);
					break;
				case "-user":
					String sip_uri = args[i+1];
					sip_user = sip_uri.substring(0, sip_uri.indexOf("@"));
					if (sip_uri.indexOf(":") == -1){
						sip_interface = sip_uri.substring(sip_uri.indexOf("@")+1);
					} else {
						sip_interface = sip_uri.substring(sip_uri.indexOf("@")+1, sip_uri.indexOf(":"));
						sip_port = Integer.parseInt(sip_uri.substring(sip_uri.indexOf(":")+1));
					}
					break;
				case "-http":
					String http_bind_address = args[i+1];
					if (http_bind_address.indexOf(":") != -1){
						http_interface = http_bind_address.substring(0, http_bind_address.indexOf(":"));
						http_port = Integer.parseInt(http_bind_address.substring(http_bind_address.indexOf(":")+1));
					} else if (http_bind_address.indexOf(".") != -1) {
						http_interface = http_bind_address;
					} else {
						http_port = Integer.parseInt(http_bind_address);
					}
					break;
				default:
					System.out.println("No input from the command line interface, the default value will be used.");
				}
			}
		} catch (Exception e) {
			System.out.println("Bad line command line interface input, the default value will be used.");
		}

		System.out.println("The parameters of the app are:");
		System.out.println("SIP port: " + sip_port);
		System.out.println("HTTP port: " + http_port);
		System.out.println("SIP user: " + sip_user);
		System.out.println("SIP interface: " + sip_interface);
		System.out.println("HTTP interface: " + http_interface);
		System.out.println("Default message: " + default_message);
		System.out.println("Message location: " + message_location);

		//We write the default message
		writeDefaultMessage(default_message);
		
		Thread web_server = new ThreadWeb(default_message, message_location, http_port);
		web_server.start();


		// Create the thread for the sip server and launch it.
		System.out.println("Server sip started on: " + sip_interface + ":" + sip_port);
		Thread sip_server = new ThreadSIPServer(sip_port, sip_interface, sip_user, message_location, default_message);
		sip_server.start();
	}

	public static void readConf(String fileName) throws Exception{

		Properties prop = new Properties();
		InputStream is = new FileInputStream(fileName);

		prop.load(is);

		try {
			http_port = Integer.parseInt(prop.getProperty("http_port").trim());
			sip_port = Integer.parseInt(prop.getProperty("sip_port").trim());
			sip_user = prop.getProperty("sip_user");
			sip_interface = prop.getProperty("sip_interface");
			http_interface = prop.getProperty("http_interface");
			default_message = prop.getProperty("default_message_text");
			message_location = prop.getProperty("message_wav");
		} catch (Exception e) {
			System.out.println("The config file is not correct, default values will be used.");
		}
	}

	public static void writeDefaultMessage(String message){
		FreeTTS freetts = new FreeTTS(message);
		String response = freetts.writeMessage(message_location);

		System.out.println("The default SIP message has been created.");
		System.out.println("Default message: " + message);

		System.out.println("Response of the default wav creation:" + response);
	}

}
