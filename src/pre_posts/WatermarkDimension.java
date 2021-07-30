package pre_posts;

public class WatermarkDimension {
	private short width;
	private short height;
	
	public WatermarkDimension(short width, short height) {
		this.width = width;
		this.height = height;
	}
	
	public short getWidth() {return width;}
	public short getHeight() {return height;}
	
	public void setWidth(short width) {this.width = width;}
	public void setHeight(short height) {this.height = height;}
	
	public boolean equals(WatermarkDimension dim) {
		if(this.width == dim.getWidth() && this.height == dim.getHeight()) return true;
		else return false;
	}
}
