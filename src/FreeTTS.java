import java.io.File;

import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;
import com.sun.speech.freetts.audio.SingleFileAudioPlayer;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFileFormat.Type;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public class FreeTTS{

	String voiceName = "kevin16"; // Voice type "alan","kevin","kevin16"
	String voiceMsg = "Hi How are you?"; // The text to Speech
	String fileLocation = "message"; // filename without extension

	public FreeTTS(String message){
		this.voiceMsg = message;
	}

	public String writeMessage(String message_location) {
		try {
			VoiceManager voiceManager = VoiceManager.getInstance();
			Voice voice = voiceManager.getVoice(voiceName);
			voice.setStyle("casual"); //Voice style "business", "casual", "robotic", "breathy"
			voice.allocate();            
			SingleFileAudioPlayer audioPlayer = new SingleFileAudioPlayer(
					message_location + "_temp", Type.WAVE);     //To Create the wav file for the text
			voice.setAudioPlayer(audioPlayer);
			voice.setVolume(100);
			voice.speak(voiceMsg); //Speaks the text
			voice.deallocate();
			audioPlayer.close();
			convertWav(message_location);
			return "OK";
		} catch (Exception e) { 
			System.err.println(e);
			return ""+e;
		}
	}

	public void convertWav(String message_location){
		File fileIn = new File(message_location + "_temp.wav");
		
		try {
			AudioInputStream ais = AudioSystem.getAudioInputStream(fileIn);
            
			// First we reduce the frequency
			AudioFormat newFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000, 16, 1, 2, 8000, false) ;
			AudioInputStream lowRestemp = AudioSystem.getAudioInputStream(newFormat, ais);
			//Then we change the WAV encoding type
			AudioFormat[] targets = AudioSystem.getTargetFormats(AudioFormat.Encoding.ULAW, lowRestemp.getFormat());
			AudioInputStream lowResAIS = AudioSystem.getAudioInputStream(targets[0], lowRestemp);
			//We write the output file
			File outFile = new File(message_location + ".wav");
			AudioSystem.write(lowResAIS, AudioFileFormat.Type.WAVE, outFile);
			//We delete the temporary file
			fileIn.delete();
			
			System.out.println("Successfully converted to the right WAV format.");
		} catch (Exception e) {
			System.out.println(e);
		}

	}

}
