

import java.io.File;
import java.io.IOException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.lang.String;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;

import org.jlibrtp.*;


//this class create the RTPsession on a port and an address and send the file.
public class SessionAudio implements RTPAppIntf {

	public RTPSession rtp_session = null;
	public String message_path;
	public final int buffer_size = 1024;
	public Participant receiver;
	public boolean end;
	public boolean part_add = false;
	
	// The address of the sender and of the receiver are of different types.
	public SessionAudio(int port_sender, InetAddress add_sender, int port_receiver, String add_receiver, String message_path){
		
		this.message_path = message_path;
		
		this.end = false;
		
		// creation of the RTP session.
		try {
			
			System.out.println("Port sender: " + port_sender);
			DatagramSocket rtpSocket = new DatagramSocket(port_sender, add_sender);
			DatagramSocket rtcpSocket = new DatagramSocket(port_sender+1, add_sender);
			this.rtp_session = new RTPSession(rtpSocket, rtcpSocket);
			
			this.rtp_session.RTPSessionRegister(this,null, null);
			
		} catch (SocketException e) {
			System.out.println("Problem while creating the RTPSession: " + e);
			return;
		}
		
		try {
			TimeUnit.MILLISECONDS.sleep(300);
		} catch (InterruptedException e) {
			System.out.println("Problem while sleeping: " + e);
		}
		
		try {
			this.receiver = new Participant(add_receiver, port_receiver, port_receiver + 1);
			this.rtp_session.addParticipant(this.receiver);			
		} catch (Exception e) {
			System.out.println("Problem while adding a participant: " + e);
			e.printStackTrace();
			return;
		}
		
		try {
			TimeUnit.MILLISECONDS.sleep(2);
		} catch (InterruptedException e) {
			System.out.println("Problem while sleeping: " + e);
		}
	}

	// the three next methods are mandatory because if the implementation of RTPAPPintf, but are not useful in our situation
	@Override
	public int frameSize(int arg0) {
		return 0;
	}

	@Override
	public void receiveData(DataFrame arg0, Participant arg1) {
		// do nothing.
		
	}

	@Override
	public void userEvent(int arg0, Participant[] arg1) {
		// do nothing.
		
	}
	
	public void sendFile() {
		File audioFile = new File(this.message_path);
		if (!audioFile.exists()) {
			System.err.println("Wav file " + this.message_path + " doesn't exist.");
			return;
		}
		
		AudioInputStream audio_input_stream = null;
		
		try {
			audio_input_stream = AudioSystem.getAudioInputStream(audioFile);
		} catch (UnsupportedAudioFileException e) {
			System.err.println("The audio file is not supported: "+e);
			return;
		} catch (IOException e) {
			System.err.println("Problem with the audio input stream: "+e);
			return;
		}
		
		int nBytesRead = 0;
		byte[] data_to_send = new byte[buffer_size];
		
		if(true){
			try {
				while ((nBytesRead != -1) && (!end)) {
					
					nBytesRead = audio_input_stream.read(data_to_send, 0, data_to_send.length);
					
					if (nBytesRead >= 0) {
						
						try {
							TimeUnit.MILLISECONDS.sleep(130);
						} catch (InterruptedException e) {
							System.out.println("Problem while sleeping: " + e);
						}
						
						this.rtp_session.sendData(data_to_send);
						
					}
					
				}
			} catch (IOException e) {
				System.out.println("Problem while sending the audio: " + e);
				return;
			}
		}
		
		try {
			TimeUnit.MILLISECONDS.sleep(300);
		} catch (InterruptedException e) {
			System.out.println("Problem while sleeping: " + e);
		}
		
		
		try {
			this.rtp_session.endSession();
		} catch (Exception e) {
			System.out.println("Problem while ending the RTP session: " + e);
		}
		
		if (!end){
			try {
				TimeUnit.SECONDS.sleep(2);
			} catch (InterruptedException e) {
				System.out.println("Problem while sleeping: " + e);
			}
		}
	}
	
}
