import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

public class ThreadSIPServer extends Thread {

	public int port;
	public String sAdd;
	public InetAddress addr;
	
	
	public DatagramSocket socket = null;

	public ThreadSIPServer(int port, String addrSIP) {
		this.port = port;
		
		//TODO changer cette ligne et ajouter un paramètre au constructeur
		this.sAdd = "192.168.0.101";

		try {
			addr = InetAddress.getByName(this.sAdd);
		} catch (UnknownHostException e1) {
			System.out.println("Error while getting the datagram socket's address: "
					+ e1);
			return;
		}

		// creation of the socket on which the server will listen and wait for
		// SIP request.
		try {
			this.socket = new DatagramSocket(port, addr);
		} catch (IOException e) {
			System.out.println("Error while launching the datagram socket: "
					+ e);
			return;
		}
	}

	public void run() {
		while (true) {

			try {
				byte[] buf = new byte[256];

				// receive request
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				this.socket.receive(packet);

				String request = new String(packet.getData());

				//System.out.println(request);

				if (request.startsWith("INVITE")) {

					String[] req = request.split("\n");
					String via = "";
					String from = "";
					String to = "";
					String callID = "";
					String numb = "";

					for (String part : req) {
						if (part.startsWith("Via")) {
							via = part;
						}

						if (part.startsWith("From")) {
							from = part;
						}

						if (part.startsWith("To")) {
							to = part;
						}
					}

					req = request.split("[ \n]");
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
					String trying = new String("SIP/2.0 100 Trying\n" 
							+ via
							+ "\n" + from 
							+ "\n" + to 
							+ "\nCall-ID: " + callID
							+ "\nCseq: " + numb + " INVITE"
							+ "\nContent-Length: 0\n\n");


					byte[] btrying = trying.getBytes();

					// send the response to the client at "address" and "port".
					InetAddress address = packet.getAddress();
					int port = packet.getPort();

					packet = new DatagramPacket(btrying, btrying.length,
							address, port);
					
					socket.send(packet);

					// write the 200 OK response and convert the response in
					// byte[].
					// we need to add a tag to the "To" field.
					
					String toTag = this.generateTag();
					
					String ok = new String("SIP/2.0 200 OK\n" 
							+ via + "\n"
							+ from + "\n" 
							+ to.substring(0,to.length()-1) +";tag="+toTag
							+ "\nCall-ID: " + callID
							+ "\nCseq: " + numb + " INVITE\n"
							+ "Supported: replaces\n"
							+ "Allow: INVITE, ACK, CANCEL, OPTIONS, BYE, REFER, SUBSCRIBE, NOTIFY, INFO\n"
							+ "Contact: \"Thomas\" <sip:"+this.sAdd+":40000>\n"
							+ "Content-Type: application/sdp\n");
							
					String sdp = new String("v=0\n"
							+ "o=georges 4535 4535 IN IP4 "+this.sAdd+"\n"
							+ "s=messageAuto\n"
							+ "c=IN IP4 127.0.0.1\n"
							+ "t=0 0\n"
							+ "a=rtcp-xr:rcvr-rtt=all:10000 stat-sumary=loss,dup,jitt,TTL voip-metrics\n"
							
							//TODO change the port to allow multiple RTP sessions
							+ "m=audio 20000 RTP/AVP 0 101/n" //124 111 110 0 8 101\n"
							+ "a=rtpmap:101 telephone-event/8000\n"
							//+ "a=fmtp:101 0-16\n"
							/*
							+ "a=rtpmap:0 PCMU/8000\n"
							+ "a=rtpmap:8 PCMA/8000\n"
							+ "a=rtpmap:101 telephone-event/8000\n"
							+ "a=fmtp:101 0-16\n"
							+ "a=silenceSupp:off\n\n");
							*/
							);
					
					ok = ok + "Content-Length: " + String.valueOf(sdp.length()) + "\n\n" + sdp;

					byte[] bok = ok.getBytes();

					// send the response to the client at "address" and "port"
					packet = new DatagramPacket(bok, bok.length, address, port);
					socket.send(packet);
				}

			} catch (IOException e) {
				System.out.println("Error while receiving a packet: " + e);
				return;
			}
		}
	}
	
	
	// generate a string of length "lengthString" to generate tags for SIP
	public String generateTag(){
		UUID tagUUID = UUID.randomUUID();
		String tagString = tagUUID.toString();
		String[] tagStringArray = tagString.split("-");
		String output = tagStringArray[0];
		
		return output;
	}
}
