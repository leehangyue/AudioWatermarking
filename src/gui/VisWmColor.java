package gui;

public class VisWmColor {//Visualize Watermark Color Rendering Settings
	private double bright;
	private double contrast;
	private double colorTemp;
	private double tint;
	
	public VisWmColor(double bright, double contrast, double colorTemp, double tint) {
		setBright(bright);
		setContrast(contrast);
		setColorTemp(colorTemp);
		setTint(tint);
	}
	
	public void setBright(double bright) {
		bright = Math.min(bright, 1.0);
		bright = Math.max(bright, -1.0);
		this.bright = bright;
	}
	public void setContrast(double contrast) {
		contrast = Math.min(contrast, 1.0);
		contrast = Math.max(contrast, -1.0);
		this.contrast = contrast;
	}
	public void setColorTemp(double colorTemp) {
		colorTemp = Math.min(colorTemp, 1.0);
		colorTemp = Math.max(colorTemp, -1.0);
		this.colorTemp = colorTemp;
	}
	public void setTint(double tint) {
		tint = Math.min(tint, 1.0);
		tint = Math.max(tint, -1.0);
		this.tint = tint;
	}
	
	public double getBright() { return bright;}
	public double getContrast() { return contrast;}
	public double getColorTemp() { return colorTemp;}
	public double getTint() { return tint;}
	
}
