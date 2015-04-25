import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SessionSIP {

	String request;
	DatagramSocket socket;
	InetAddress addr_socket;
	int port_socket;
	InetAddress addr_caller;
	int port_caller;

	public SessionSIP(String request, DatagramSocket socket,
			InetAddress addr_socket, int port_socket, InetAddress addr_caller,
			int port_caller) {
		this.socket = socket;
		this.request = request;
		this.addr_socket = addr_socket;
		this.port_socket = port_socket;
		this.addr_caller = addr_caller;
		this.port_caller = port_caller;
	}

	public boolean start() {

		String[] req = this.request.split("\n");
		String via = "";
		String from = "";
		String caller_id = "";
		String to = "";
		String socket_id = "";
		String callID = "";
		String numb = "";
		String id_caller = "";
		int port_socket = 0;

		for (String part : req) {
			if (part.startsWith("Via")) {
				via = part;
			}

			if (part.startsWith("From")) {
				from = part;
				caller_id = part.split(" ")[1];
				System.out.println(caller_id);
			}

			if (part.startsWith("To")) {
				to = part;
				socket_id = part.split(" ")[1];
				System.out.println(socket_id);
			}

			if (part.startsWith("Contact")) {
				id_caller = part.split(":")[2];
			}

			if (part.startsWith("m=")) {
				port_socket = Integer.parseInt(part.split(" ")[1]);
			}
		}

		req = this.request.split("[ \n]");
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

		// write the 100 trying response and convert the response in
		// byte[].
		String trying = new String("SIP/2.0 100 Trying\n" + via + "\n" + from
				+ "\n" + to + "\nCall-ID: " + callID + "\nCseq: " + numb
				+ " INVITE" + "\nContent-Length: 0\n\n");

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

		String toTag = this.generateTag();
		System.out.println(to);
		to = to.substring(0, to.length() - 1) + ";tag=" + toTag;

		// SIP header of the OK message. The Content-length is missing and is
		// added after.
		String ok = new String(
				"SIP/2.0 200 OK\n"
						+ via
						+ "\n"
						+ from
						+ "\n"
						+ to
						+ "\nCall-ID: "
						+ callID
						+ "\nCseq: "
						+ numb
						+ " INVITE\n"
						+ "Supported: replaces\n"
						+ "Allow: INVITE, ACK, CANCEL, OPTIONS, BYE, REFER, SUBSCRIBE, NOTIFY, INFO\n"
						+ "Contact: \"Thomas\" <sip:"
						+ this.addr_socket.getHostAddress() + ":"
						+ Integer.toString(this.port_socket) + ">\n"
						+ "Content-Type: application/sdp\n");

		// SDP header + calculation of its length.
		String sdp = new String(
				"v=0\n"
						+ "o=georges 4535 4535 IN IP4 "
						+ this.addr_caller.getHostAddress()
						+ "\n"
						+ "s=messageAuto\n"
						+ "c=IN IP4 127.0.0.1\n"
						+ "t=0 0\n"
						+ "a=rtcp-xr:rcvr-rtt=all:10000 stat-sumary=loss,dup,jitt,TTL voip-metrics\n"

						// TODO change the port to allow multiple RTP sessions
						+ "m=audio 20000 RTP/AVP 0 101/n" // 124 111 110 0 8
															// 101\n"
						+ "a=rtpmap:101 telephone-event/8000\n"
		// + "a=fmtp:101 0-16\n"
		/*
		 * + "a=rtpmap:0 PCMU/8000\n" + "a=rtpmap:8 PCMA/8000\n" +
		 * "a=rtpmap:101 telephone-event/8000\n" + "a=fmtp:101 0-16\n" +
		 * "a=silenceSupp:off\n\n");
		 */
		);

		// Making of the OK message and conversion in byte[].
		ok = ok + "Content-Length: " + String.valueOf(sdp.length()) + "\n\n"
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

		// TODO port variable for the RTP session.
		/*
		 * SessionAudio session = new SessionAudio(20000, this.addr_socket,
		 * this.port_caller, this.addr_caller.getHostAddress(),
		 * "/home/tom/Documents/SIP_Speaker/SIPSpeaker/message.wav");
		 * session.sendFile();
		 */

		// TODO make the BYE message and send it.
		String bye = new String("BYE sip:" + id_caller + " SIP/2.0\n"
				+ "Via: SIP/2.0/UDP " + this.addr_socket.getHostAddress() + ":"
				+ Integer.toString(this.port_socket) + ";rport;branc=z9hG4bk"
				+ this.generateTag() + "\n" + from + "\n" + to + "\n"
				+ "Call-ID: " + callID + "\n" + "Cseq: 21 BYE\n"
				+ "Contact: \"Thomas\" <sip:"
				+ this.addr_socket.getHostAddress() + ":"
				+ Integer.toString(this.port_socket) + ">\n"
				+ "Max-Forwards: 70\n" + "Content-Length: 0\n" + "\n");

		byte[] bbye = bye.getBytes();
		
		try {
		    TimeUnit.SECONDS.sleep(5);
		} catch (InterruptedException e) {
		    System.out.println("Problem while sleeping: " + e);
		}
		
		System.out.println(bye);
		// send the BYE request to the client at "addr_caller" and "port_caller"
		packet = new DatagramPacket(bbye, bbye.length, this.addr_caller,
				this.port_caller);
		System.out.println(packet);
		try {
			this.socket.send(packet);
			System.out.println("Inside try");
		} catch (IOException e) {
			System.out.println("Problem while sending BYE message: " + e);
			return false;
		}

		return true;
	}

	// generate a string of length "lengthString" to generate tags for SIP
	public String generateTag() {
		UUID tagUUID = UUID.randomUUID();
		String tagString = tagUUID.toString();
		String[] tagStringArray = tagString.split("-");
		String output = tagStringArray[0];

		return output;
	}
}
