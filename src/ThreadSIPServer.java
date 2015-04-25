import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ThreadSIPServer extends Thread {

	public int port;
	public String sAdd;
	public InetAddress addr;
	public String sipUser;
	public String messageLocation;

	public DatagramSocket socket = null;

	public ThreadSIPServer(int port, String addrSIP, String sipUser,
			String messageLocation) {
		this.port = port;
		this.sipUser = sipUser;
		this.messageLocation = messageLocation;
		this.sAdd = addrSIP;

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
			this.socket = new DatagramSocket(port, addr);
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

				
				InetAddress addr_dest = packet.getAddress();
				// TODO stringAddr_dest useful?
				String stringAddr_dest = addr_dest.getHostAddress();
				int port_dest = packet.getPort();

				// Create the SIP session and start the sending (Trying + OK + RTP + BYE).
				SessionSIP sess = new SessionSIP(request, socket, this.addr,
						this.port, addr_dest, port_dest);
				if (sess.start()) {
					System.out.println("Sending ok.");
				} else {
					System.out.println("Preoblem while sending.");
				}
			}
		}
	}

}
