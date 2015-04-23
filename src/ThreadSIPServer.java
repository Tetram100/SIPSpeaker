import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;

public class ThreadSIPServer extends Thread {

	public int port;
	public DatagramSocket socket = null;

	public ThreadSIPServer(int port) {
		this.port = port;

		// creation of the socket on which the server will listen and wait for
		// SIP request.
		try {
			this.socket = new DatagramSocket(port);
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
					// byte[]
					String trying = new String("SIP/2.0 100 Trying\n" + via
							+ "\n" + from + "\n" + to + "\nCall-ID: " + callID
							+ "\nCseq: " + numb + " INVITE"
							+ "\nContent-Length: 0\n\n");
					//System.out.println(trying);

					byte[] btrying = trying.getBytes();

					// send the response to the client at "address" and "port"
					InetAddress address = packet.getAddress();
					int port = packet.getPort();

					packet = new DatagramPacket(btrying, btrying.length,
							address, port);
					
					socket.send(packet);

					// write the 200 OK response and convert the response in
					// byte[]
					// we need to add a tag to the "To" field
					
					String toTag = this.randomString(10);
					
					String ok = new String("SIP/2.0 200 OK\n" + via + "\n"
							+ from + "\n" + to.substring(0,to.length()-1) +";tag="+toTag+ "\nCall-ID: " + callID
							+ "\nCseq: " + numb + " INVITE\n"
							+ "Supported: replaces\n"
							+ "Allow: INVITE, ACK, CANCEL, OPTIONS, BYE, REFER, SUBSCRIBE, NOTIFY, INFO\n"
							+ "Contact: \"Thomas\" <sip:127.0.0.1:40000>\n"
							+ "Content-Type: application/sdp\n");
							
					String sdp = new String("v=0\n"
							+ "o=georges 4535 4535 IN IP4 192.168.0.101\n"
							+ "s=messageAuto\n"
							+ "c=IN IP4 127.0.0.1\n"
							+ "t=0 0\n"
							+ "a=rtcp-xr:rcvr-rtt=all:10000 stat-sumary=loss,dup,jitt,TTL voip-metrics\n"
							+ "m=audio 20000 RTP/AVP 124 111 110 0 8 101\n"
							+ "a=rtpmap:101 telephone-event/8000\n"
							+ "a=fmtp:101 0-16\n"
							/*
							+ "a=rtpmap:0 PCMU/8000\n"
							+ "a=rtpmap:8 PCMA/8000\n"
							+ "a=rtpmap:101 telephone-event/8000\n"
							+ "a=fmtp:101 0-16\n"
							+ "a=silenceSupp:off\n\n");
							*/
							);
					
					byte[] bsdp = sdp.getBytes();
					int cLength = bsdp.length;
					
					ok = ok + "Content-Length: " + String.valueOf(cLength) + "\n\n" + sdp;
					
					System.out.println(ok);

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
	public String randomString(int lengthString){
		char[] chars = "0123456789".toCharArray();
		StringBuilder sb = new StringBuilder();
		Random random = new Random();
		for (int i = 0; i < lengthString; i++) {
		    char c = chars[random.nextInt(chars.length)];
		    sb.append(c);
		}
		String output = sb.toString();
		
		return output;
	}
}
