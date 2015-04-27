import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

public class ThreadSIPServer extends Thread {

	public int port;
	public String sAdd;
	public InetAddress addr;
	public String sipUser;
	public String messageLocation;
	public Vector<Integer> rtpPort_SIP_sessions;
	public HashMap<String,ThreadSIPSession> sessions;

	public DatagramSocket socket = null;

	public ThreadSIPServer(int port, String addrSIP, String sipUser,
			String messageLocation) {
		this.port = port;
		this.sipUser = sipUser;
		this.messageLocation = messageLocation;
		this.sAdd = addrSIP;
		this.sessions = new HashMap<String,ThreadSIPSession>();
		this.rtpPort_SIP_sessions = new Vector<Integer>(1);

		try {
			this.addr = InetAddress.getByName(this.sAdd);
		} catch (UnknownHostException e) {
			System.out
					.println("Error while getting the datagram socket's address: "
							+ e);
			return;
		}

		// creation of the socket on which the server will listen and wait for
		// SIP request.
		try {
			this.socket = new DatagramSocket(this.port, this.addr);
		} catch (IOException e) {
			System.out.println("Error while launching the datagram socket: "
					+ e);
			return;
		}
	}

	public void run() {
		while (true) {

			// If the buf size is too short, the request is cut.
			byte[] buf = new byte[2048];
			// To generate RTP port.
			Random rand = new Random();
			
			InetAddress addr_dest = null;
			int port_dest = 0;

			// receive request
			DatagramPacket packet = new DatagramPacket(buf, buf.length);

			try {
				System.out.println("Listenning for packet.");
				this.socket.receive(packet);
				addr_dest = packet.getAddress();
				port_dest = packet.getPort();
			} catch (IOException e) {
				System.out.println("Error while receiving a packet: " + e);
				return;
			}

			String request = new String(packet.getData());

			// If the request begins with BYE, we check if it corresponds to one of our calls with the Call-ID. If so, we call the method client_hangup().
			if (request.startsWith("BYE")){
				String[] string_request = request.split("[ \r\n]");
				int k = 0;
				String callID = "";
				for (String part : string_request) {

					if (part.equals("Call-ID:")) {
						callID = string_request[k + 1];
					}
					k++;
				}
				if (this.sessions.containsKey(callID)){
					System.out.println("Client has hanged up!");
					this.sessions.get(callID).client_hangup();
					String Via = "";
					String From = "";
					String To = "";
					String Call_ID = "";
					String CSeq = "";
					
					String[] split_request = request.split("\r\n");
					for (String part : split_request){
						if (part.startsWith("Via")){
							Via = part;
						}
						if (part.startsWith("From")){
							From = part;
						}
						if (part.startsWith("To")){
							To = part;
						}
						if (part.startsWith("Call-ID")){
							Call_ID  = part;
						}
						if (part.startsWith("CSeq")){
							CSeq = part;
						}
					}
					
					// TODO tester champs (de même que pour toutes les autres messages à envoyer.
					String ack_bye = "SIP/2.0 200 OK\r\n";
					ack_bye += Via + "\r\n";
					ack_bye += From + "\r\n";
					ack_bye += To + "\r\n";
					ack_bye += Call_ID + "\r\n";
					ack_bye += CSeq + "\r\n";
					ack_bye += "Content-Length: 0\r\n";
					ack_bye += "\r\n";
					
					byte[] back_bye = ack_bye.getBytes();
					
					packet = new DatagramPacket(back_bye, back_bye.length,
							addr_dest, port_dest);
					
					try {
						socket.send(packet);
						System.out.println("ACK for BYE message sends.");
					} catch (IOException e1) {
						System.out.println("Problem while sending ACK-BYE message: " + e1);
					}
					
					request = "";
				}
			}
			
			// If the request starts with INVITE, we check if it has already been answered thanks to the Call-ID.
			if (request.startsWith("INVITE")) {
				String first_line_res = request.split("\r\n")[0];

				// Check if the caller calls the correct user. If not, send an Error 404.
				if (first_line_res.contains(this.sipUser)) {
					String[] string_request = request.split("[ \n]");
					int k = 0;
					String callID = "";
					for (String part : string_request) {

						if (part.equals("Call-ID:")) {
							callID = string_request[k + 1];
						}
						k++;
					}
					
					// If the INVITE's Call-ID is not known (not in the hashmap), we start a response and add the couple (Call-id, ThreadSIP) to the hashmap.
					if (!this.sessions.containsKey(callID)) {

						// Generate a random port for the RTP session and stores it in a vector.
						int rtpPort = 7078;
						synchronized (rtpPort_SIP_sessions) {
							do {
								rtpPort = rand.nextInt((8500 - 6972) + 1) + 6972;
								if (!(rtpPort % 2 == 0)) {
									rtpPort = rtpPort - 1;
								}
							} while ((rtpPort_SIP_sessions.contains(rtpPort)));
							rtpPort_SIP_sessions.addElement(rtpPort);
						}

						ThreadSIPSession thread_SIP = new ThreadSIPSession(this.socket, request, rtpPort,
								this, this.sipUser, this.addr, this.port,
								addr_dest, port_dest, callID, this.messageLocation);
						
						synchronized (this.sessions) {
							this.sessions.put(callID,thread_SIP);
						}
						
						try {
							thread_SIP.start();
						} catch (Exception e) {
							System.out
									.println("Error while launching the SIP session thread: "
											+ e);
						}

						// Create the SIP session and start the sending (Trying
						// + OK + RTP + BYE).
						/*
						 * SessionSIP sess = new SessionSIP(this.sipUser,
						 * request, socket, this.addr, this.port, addr_dest,
						 * port_dest); if (sess.start()) {
						 * System.out.println("Sending ok."); } else {
						 * System.out.println("Problem while sending."); }
						 */
					}
					else {
						// The INVITE has already been answered. We reset the request string.
						request = "";
					}

				} else {
					System.out.println("A client tried to call an unnkown user.");
					
					// Making of the response message "404 Not found".
					String Via = "";
					String From = "";
					String To = "";
					String Call_ID = "";
					String CSeq = "";
					
					String[] split_request = request.split("\r\n");
					for (String part : split_request){
						if (part.startsWith("Via")){
							Via = part;
						}
						if (part.startsWith("From")){
							From = part;
						}
						if (part.startsWith("To")){
							To = part;
						}
						if (part.startsWith("Call-ID")){
							Call_ID  = part;
						}
						if (part.startsWith("CSeq")){
							CSeq = part;
						}
					}
					
					String not_found = "SIP/2.0 404 Not Found\r\n";
					not_found += Via + "\r\n";
					not_found += From + "\r\n";
					not_found += To + ";tag=" + SessionSIP.generateTag() + "\r\n";
					not_found += Call_ID + "\r\n";
					not_found += CSeq + "\r\n";
					not_found += "Allow: INVITE, ACK, CANCEL, OPTIONS, BYE, REFER, SUBSCRIBE, NOTIFY\r\n";
					not_found += "Contact: <" + this.sipUser + "@" + this.sAdd +":"+ this.port + ">\r\n";
					not_found += "Content-Length: 0\r\n";
					not_found += "\r\n";
					
					byte[] bnot_found = not_found.getBytes();
					
					packet = new DatagramPacket(bnot_found, bnot_found.length,
							addr_dest, port_dest);
					
					try {
						socket.send(packet);
						System.out.println("404 Not Found message sent.");
					} catch (IOException e1) {
						System.out.println("Problem while sending 404 Not Found message: " + e1);
					}
					
					request = "";
				}
			}
		}
	}

}
