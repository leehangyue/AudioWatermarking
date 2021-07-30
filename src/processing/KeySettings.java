package processing;

public class KeySettings {
	private double offset;
	private int width;
	private int height;
	private boolean smooth;
	private boolean robust;
	private boolean isDefault = true;
	//If settings come from constructor, it's default, if from setters, it's not default
	private int fold = 2;//default
	
	public KeySettings(double offset, int width, int height, boolean smooth, boolean robust) {
		this.offset = offset;
		this.width = width;
		this.height = height;
		this.smooth = smooth;
		this.robust = robust;
		isDefault = true;//constructed keySettings are default
	}
	
	public double getOffset() {return offset;}
	public int getWidth() {return width;}
	public int getHeight() {return height;}
	public boolean isSmooth() {return smooth;}
	public boolean isRobust() {return robust;}
	public boolean isDefault() {return isDefault;}
	public int getFold() {return fold;}
	
	public void setOffset(double offset) {
		offset = Math.min(1.0, offset);
		offset = Math.max(-1.0, offset);
		this.offset = offset;
		isDefault = false;
	}
	public void setWidth(int width) {this.width = width; isDefault = false;}
	public void setHeight(int height) {this.height = height; isDefault = false;}
	public void setSmooth(boolean smooth) {this.smooth = smooth; isDefault = false;}
	public void setRobust(boolean robust) {this.robust = robust; isDefault = false;}
	public void setDefault(boolean isDefault) {this.isDefault = isDefault;}
	public void setFold(int fold) {this.fold = fold; isDefault = false;}
	
	public void copyAs(KeySettings referenceKeySettings) {
		this.offset = referenceKeySettings.getOffset();
		this.width = referenceKeySettings.getWidth();
		this.height = referenceKeySettings.getHeight();
		this.smooth = referenceKeySettings.isSmooth();
		this.robust = referenceKeySettings.isRobust();
		this.isDefault = referenceKeySettings.isDefault();
	}
	
}
