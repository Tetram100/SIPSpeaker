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
			System.out.println("Error while launching the datagram socket: " + e);
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
				
				String callID = "";
				
				
				if(request.startsWith("INVITE")){
					
					System.out.println("INVITE INVITE INVITE");
					String[] req = request.split("[ \n]");
					int k = 0;
					 for(String part : req)
					 {
						 if(part.equals("Call-ID:")){
							callID = req[k+1];
						 }
						 k++;
					 }
					
					String rep = new String("SIP/2.0 100 Trying\nCall-ID: "+ callID + "\nCseq: 1 INVITE\nContent-Length: 0");
					System.out.println(rep);
					
					byte[] brep = rep.getBytes();
					// send the response to the client at "address" and "port"
			
					InetAddress address = packet.getAddress();
					int port = packet.getPort();
					
					System.out.println(address);
					System.out.println(port);
					packet = new DatagramPacket(brep, brep.length, address, port);
					socket.send(packet);
				}
					
				
				
			} catch (IOException e) {
				System.out.println("Error while receiving a packet: "+ e);
				return;
			}
		}
	}
}
