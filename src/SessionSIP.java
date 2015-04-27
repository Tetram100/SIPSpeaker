import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.UUID;

public class SessionSIP {

	String sip_user;
	String request;
	DatagramSocket socket;
	InetAddress addr_socket;
	int port_socket;
	InetAddress addr_caller;
	int port_caller;
	int rtp_port;
	SessionAudio session_RTP;
	boolean hangup;

	// TODO changer construct, ajouter le champ "port_rtp".
	public SessionSIP(String sip_user, String request, DatagramSocket socket,
			InetAddress addr_socket, int port_socket, InetAddress addr_caller,
			int port_caller, int rtp_port) {
		this.sip_user = sip_user;
		this.socket = socket;
		this.request = request;
		this.addr_socket = addr_socket;
		this.port_socket = port_socket;
		this.addr_caller = addr_caller;
		this.port_caller = port_caller;
		this.rtp_port = rtp_port;
		this.hangup = false;
	}

	public boolean start() {

		String[] req = this.request.split("\r\n");
		String via = "";
		String from = "";
		String caller_id = "";
		String to = "";
		String socket_id = "";
		String callID = "";
		String numb = "";
		String id_caller = "";
		int port_rtp_caller = 0;

		for (String part : req) {
			if (part.startsWith("Via")) {
				via = part;
			}

			if (part.startsWith("From")) {
				from = part;
				caller_id = part.split(" ")[1];
			}

			if (part.startsWith("To")) {
				to = part;
				socket_id = part.split(" ")[1];
				socket_id = socket_id.substring(0, socket_id.length());
			}

			if (part.startsWith("Contact")) {
				id_caller = part.split(":")[2];
			}

			if (part.startsWith("m=")) {
				port_rtp_caller = Integer.parseInt(part.split(" ")[1]);
			}

		}

		req = this.request.split("[ \r\n]");
		int k = 0;
		for (String part : req) {

			if (part.equals("Call-ID:")) {
				callID = req[k + 1];
			}

			if (part.equals("CSeq:")) {
				numb = req[k + 1];
			}
			k++;
		}

		// TODO Tester si tous les champs sont corrects (au moins non vide).
		
		// write the 100 trying response and convert the response in
		// byte[].
		String trying = new String("SIP/2.0 100 Trying\r\n" + via + "\r\n" + from + "\r\n"
				+ to + "\r\n" + "Call-ID: " + callID + "\r\n" + "Cseq: " + numb
				+ " INVITE" + "\r\n" + "Content-Length: 0\r\n\r\n");

		byte[] btrying = trying.getBytes();

		// send the response to the client at "addr_caller" and "port_caller".
		DatagramPacket packet = new DatagramPacket(btrying, btrying.length,
				this.addr_caller, this.port_caller);

		try {
			socket.send(packet);
		} catch (IOException e1) {
			System.out.println("Problem while sending Trying message: " + e1);
			return false;
		}

		// write the 200 OK response and convert the response in
		// byte[].
		// we need to add a tag to the "To" field.

		String tag = this.generateTag();
		to = to.substring(0, to.length()) + ";tag=" + tag;

		// SIP header of the OK message. The Content-length is missing and is
		// added after.
		String ok = new String(
				"SIP/2.0 200 OK\r\n"
						+ via
						+ "\r\n"
						+ from
						+ "\r\n"
						+ to
						+ "\r\nCall-ID: "
						+ callID
						+ "\r\nCSeq: "
						+ numb
						+ " INVITE\r\n"
						+ "User-Agent: pipophone\r\n"
						+ "Supported: outboud\r\n"
						+ "Allow: INVITE, ACK, CANCEL, OPTIONS, BYE, REFER, SUBSCRIBE, NOTIFY, INFO\r\n"
						+ "Contact: <sip:" + this.sip_user + "@"
						+ this.addr_socket.getHostAddress() + ":"
						+ Integer.toString(this.port_socket) + ">\r\n"
						+ "Content-Type: application/sdp\r\n");

		// SDP header + calculation of its length.
		String sdp = new String(
				"v=0\r\n"
						+ "o=georges 4535 4535 IN IP4 "
						+ this.addr_socket.getHostAddress()
						+ "\r\n"
						+ "s=messageAuto\r\n"
						+ "c=IN IP4 "+addr_socket.getHostAddress()+"\r\n"
						+ "t=0 0\r\n"
						+ "a=rtcp-xr:rcvr-rtt=all:10000 stat-sumary=loss,dup,jitt,TTL voip-metrics\r\n"

						// TODO change the port to allow multiple RTP sessions
						+ "m=audio "+this.rtp_port+" RTP/AVP 0 101\r\n"
						+ "a=rtpmap:101 telephone-event/8000\r\n"
		);

		// Making of the OK message and conversion in byte[].
		ok = ok + "Content-Length: " + String.valueOf(sdp.length()) + "\r\n\r\n"
				+ sdp;

		byte[] bok = ok.getBytes();

		// send the OK response to the client at "addr_caller" and "port_caller"
		packet = new DatagramPacket(bok, bok.length, this.addr_caller,
				this.port_caller);
		
		try {
			this.socket.send(packet);
		} catch (IOException e) {
			System.out.println("Problem while sending OK message: " + e);
			return false;
		}
		
		// TODO port variable for the RTP session. Nom du fichier wav !!

		// Creation and sending of the SessionAudio that will send the RTP
		// messages.
		this.session_RTP = new SessionAudio(this.rtp_port, this.addr_socket,
				port_rtp_caller, this.addr_caller.getHostAddress(),
				"message.wav");

		try {
			this.session_RTP.sendFile();
		} catch (Exception e1) {
			System.out.println("Problem while launching the method sendFIle of a SessionAUdio: " + e1);
		}

		if (!this.hangup){
			String bye = new String("BYE sip:" + id_caller + " SIP/2.0\r\n"
					+ "Via: SIP/2.0/UDP " + this.addr_socket.getHostAddress() + ":"
					+ Integer.toString(this.port_socket) + ";rport;branc=z9hG4bk"
					+ this.generateTag() + "\r\n" + "From: " + socket_id + ";tag="
					+ tag + "\r\n" + "To: " + caller_id + "\r\n" + "Call-ID: " + callID
					+ "\r\n" + "Cseq: 21 BYE\r\n" + "Contact: \"Thomas\" <sip:"
					+ this.addr_socket.getHostAddress() + ":"
					+ Integer.toString(this.port_socket) + ">\r\n"
					+ "Max-Forwards: 70\r\n" + "Content-Length: 0\r\n" + "\r\n");

			byte[] bbye = bye.getBytes();

			// send the BYE request to the client at "addr_caller" and "port_caller"
			packet = new DatagramPacket(bbye, bbye.length, this.addr_caller,
					this.port_caller);

			try {
				this.socket.send(packet);
			} catch (IOException e) {
				System.out.println("Problem while sending BYE message: " + e);
				return false;
			}
		}

		return true;
	}

	// generate a string with the UUID library to get tags for SIP
	public String generateTag() {
		UUID tagUUID = UUID.randomUUID();
		String tagString = tagUUID.toString();
		String[] tagStringArray = tagString.split("-");
		String output = tagStringArray[0];

		return output;
	}
}
