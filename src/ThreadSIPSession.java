import java.net.DatagramSocket;
import java.net.InetAddress;

public class ThreadSIPSession extends Thread {

	DatagramSocket socket;
	String request;
	int rtp_port;
	ThreadSIPServer server;
	String sip_user;
	InetAddress addr_socket;
	int port_socket;
	InetAddress addr_caller;
	int port_caller;
	String call_ID;
	SessionSIP session_SIP;
	String message_location;

	public ThreadSIPSession(DatagramSocket socket, String request,
			int rtp_port, ThreadSIPServer server, String sip_user,
			InetAddress addr_socket, int port_socket, InetAddress addr_caller,
			int port_caller, String call_ID, String message_location) {
		this.socket = socket;
		this.request = request;
		this.rtp_port = rtp_port;
		this.server = server;
		this.sip_user = sip_user;
		this.addr_socket = addr_socket;
		this.port_socket = port_socket;
		this.addr_caller = addr_caller;
		this.port_caller = port_caller;
		this.call_ID = call_ID;
		this.message_location = message_location;
	}

	public void run() {
		
		// Create the SIP session and start the sending (Trying
		// + OK + RTP + BYE).
		this.session_SIP = new SessionSIP(this.sip_user, request,
				socket, this.addr_socket, this.port_socket, this.addr_caller,
				this.port_caller, this.rtp_port, this.message_location);
		if (this.session_SIP.start()) {
			System.out.println("Sending ok.");
		} else {
			System.out.println("Problem while sending.");
		}
		/*
		synchronized (this.server.CallID_SIP_sessions) {
			this.server.CallID_SIP_sessions.removeElement(this.call_ID);
		}
		*/
		synchronized (this.server.rtpPort_SIP_sessions) {
			this.server.rtpPort_SIP_sessions.removeElement(this.rtp_port);
		}
		
		this.interrupt();
	}
	
	public void client_hangup(){
		this.session_SIP.session_RTP.end = true;
		this.session_SIP.hangup = true;
		this.interrupt();
	}
}
