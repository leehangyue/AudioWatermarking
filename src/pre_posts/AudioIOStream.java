package pre_posts;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.*;

public class AudioIOStream {
	
	protected File inputAudio;
	protected File outputAudio;
	
	protected AudioFileFormat inFormat = null;
	protected AudioFileFormat outFormat = null;
	
	protected AudioInputStream inStream = null;
	//For output: reference keyword = port, line, mixer
	//https://docs.oracle.com/javase/tutorial/sound/accessing.html
	
	public AudioIOStream(File inputAudio, File outputAudio) {
		try {
			inFormat = AudioSystem.getAudioFileFormat(inputAudio);
		}
		catch (UnsupportedAudioFileException e_unsup) {
			//DEBUG
		}
		catch (IOException e_io) {
			//DEBUG
		}
		
		try {
			outFormat = AudioSystem.getAudioFileFormat(outputAudio);
		}
		catch (UnsupportedAudioFileException e_unsup) {
			//DEBUG
		}
		catch (IOException e_io) {
			//DEBUG
		}
	}
	
	public static void main(String[] args) {
		
	}
	
}
