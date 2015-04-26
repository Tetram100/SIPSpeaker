import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.Vector;

public class ThreadSIPServer extends Thread {

	public int port;
	public String sAdd;
	public InetAddress addr;
	public String sipUser;
	public String messageLocation;
	public Vector<String> CallID_SIP_sessions;
	public Vector<Integer> rtpPort_SIP_sessions;

	public DatagramSocket socket = null;

	public ThreadSIPServer(int port, String addrSIP, String sipUser,
			String messageLocation) {
		this.port = port;
		this.sipUser = sipUser;
		this.messageLocation = messageLocation;
		this.sAdd = addrSIP;
		this.CallID_SIP_sessions = new Vector<String>(1);
		this.rtpPort_SIP_sessions = new Vector<Integer>(1);

		try {
			addr = InetAddress.getByName(this.sAdd);
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
			byte[] buf = new byte[1000];
			// To generate RTP port.
			Random rand = new Random();

			// receive request
			DatagramPacket packet = new DatagramPacket(buf, buf.length);

			try {
				this.socket.receive(packet);
			} catch (IOException e) {
				System.out.println("Error while receiving a packet: " + e);
				return;
			}

			String request = new String(packet.getData());

			if (request.startsWith("INVITE")) {
				String[] string_request = request.split("[ \n]");
				int k = 0;
				String callID = "";
				for (String part : string_request) {

					if (part.equals("Call-ID:")) {
						callID = string_request[k + 1];
					}
					k++;
				}

				if (!CallID_SIP_sessions.contains(callID)) {
					
					synchronized (CallID_SIP_sessions) {
						CallID_SIP_sessions.addElement(callID);
					}
					int rtpPort = 7078;
					synchronized (rtpPort_SIP_sessions) {
						do{
							rtpPort = rand.nextInt((8500 - 6972) + 1) + 6972;
							if (!(rtpPort % 2 == 0)) {
								rtpPort = rtpPort - 1;
							}
						}while((rtpPort_SIP_sessions.contains(rtpPort)));
						rtpPort_SIP_sessions.addElement(rtpPort);
					}
					
					
					InetAddress addr_dest = packet.getAddress();
					int port_dest = packet.getPort();
					
					try {
						
						new ThreadSIPSession(this.socket, request, rtpPort, this, this.sipUser, this.addr, this.port, addr_dest, port_dest, callID).run();
					} catch (Exception e) {
						System.out.println("Error while launching the SIP session thread: " + e);
						return;
					}

					// Create the SIP session and start the sending (Trying
					// + OK + RTP + BYE).
					/*
					SessionSIP sess = new SessionSIP(this.sipUser, request,
							socket, this.addr, this.port, addr_dest,
							port_dest);
					if (sess.start()) {
						System.out.println("Sending ok.");
					} else {
						System.out.println("Problem while sending.");
					}
					*/
				}
			}
		}
	}

}
