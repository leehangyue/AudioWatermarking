package gui;

public class AudioDuration {
	private double seconds;
	
	public AudioDuration(double seconds) {
		setDuration(seconds);
	}
	
	public void setDuration(double seconds) {
		if(seconds < 0) seconds = 0.0;
		this.seconds = seconds;
	}
	
	public double getDuration() { return seconds;}
	
	@Override
	public String toString() {
		double secondsLeft = seconds;
		long hour = (long)secondsLeft / (long)3600;//Automatically trunked to integer
		secondsLeft -= hour*(long)3600;
		long min = (long)secondsLeft / (long)60;
		secondsLeft -= min*(long)60;
		secondsLeft = ((int)(secondsLeft*1000)) / 1000;
		String ret;
		if(hour > (long)0) ret = (hour+"h "+min+"\'"+secondsLeft+"\"");
		else if (min > (long)0) ret = (min+"\'"+secondsLeft+"\"");
		else ret = (secondsLeft+"\"");
		return ret;
	}
}
