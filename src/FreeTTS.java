import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;
import com.sun.speech.freetts.audio.SingleFileAudioPlayer;
import javax.sound.sampled.AudioFileFormat.Type;

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
					message_location, Type.WAVE);     //To Create the wav file for the text
			voice.setAudioPlayer(audioPlayer);
			voice.setVolume(100);
			voice.speak(voiceMsg); //Speaks the text
			voice.deallocate();
			audioPlayer.close();
			return "OK";
		} catch (Exception e) { 
			System.err.println(e);
			return ""+e;
		}
	}

}
