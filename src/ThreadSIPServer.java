import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

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
							+ "\nContent-Length: 0");
					//System.out.println(trying);

					byte[] btrying = trying.getBytes();

					// send the response to the client at "address" and "port"
					InetAddress address = packet.getAddress();
					int port = packet.getPort();

					packet = new DatagramPacket(btrying, btrying.length,
							address, port);
					System.out.println(packet);
					socket.send(packet);

					// write the 200 OK response and convert the response in
					// byte[]
					String ok = new String("SIP/2.0 200 OK\n" + via + "\n"
							+ from + "\n" + to + "\nCall-ID: " + callID
							+ "\nCseq: " + numb + " INVITE"
							+ "\nContent-Length: 0");
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
}
