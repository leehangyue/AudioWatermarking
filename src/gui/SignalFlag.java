package gui;

public class SignalFlag {
	private boolean go_on = false;
	
	public SignalFlag(boolean go_on) {
		setGoOn(go_on);
	}
	
	public void setGoOn(boolean go_on) {
		this.go_on = go_on;
	}
	
	public boolean isGoOn() {
		return this.go_on;
	}
}
