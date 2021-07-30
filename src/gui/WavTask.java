package gui;

import java.io.File;
import java.io.IOException;

public class WavTask {
	private File wavFile = null;
	private boolean selected = true;//is selected in the WAV file table in GUI
	
	public WavTask(File wavFile, boolean selected) {
		this.wavFile = wavFile;
	}
	
	public File getFile() {return wavFile;}
	public boolean isSelected() {return selected;}
	
	public void setSelected(boolean selected) { this.selected = selected;}
	
	public boolean isSameFile(WavTask wavTask) {
		try {
			return wavTask.wavFile.getCanonicalPath().equalsIgnoreCase(
					this.wavFile.getCanonicalPath());
		} catch (IOException e) {
			System.err.println("WavTask.isSameFile: unresolvable path!"+wavTask.wavFile.getPath()+", "+this.wavFile.getPath());
			e.printStackTrace();
			return false;
		}
	}
}
