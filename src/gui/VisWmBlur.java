package gui;

public class VisWmBlur {//Visualize Watermark Blurring Effect Parameters
	private double gausBlurRad;
	private double coreWidth_BlurRad_Ratio;
	
	public static final double minGausBlurRad = 0.2;
	public static final double maxGausBlurRad = 5.0;
	public static final double minCoreWidth_BlurRad_Ratio = 0.0;
	public static final double maxCoreWidth_BlurRad_Ratio = 3.0;
	
	public VisWmBlur(double gausBlurRad, double coreWidth_BlurRad_Ratio) {
		setGausBlurRad(gausBlurRad);
		setCoreWidth_BlurRad_Ratio(coreWidth_BlurRad_Ratio);
	}
	
	public void setGausBlurRad(double gausBlurRad) {
		gausBlurRad = Math.min(gausBlurRad, maxGausBlurRad);
		gausBlurRad = Math.max(gausBlurRad, minGausBlurRad);
		this.gausBlurRad = gausBlurRad;
	}
	
	public void setCoreWidth_BlurRad_Ratio(double coreWidth_BlurRad_Ratio) {
		coreWidth_BlurRad_Ratio = Math.min(coreWidth_BlurRad_Ratio, maxCoreWidth_BlurRad_Ratio);
		coreWidth_BlurRad_Ratio = Math.max(coreWidth_BlurRad_Ratio, minCoreWidth_BlurRad_Ratio);
		this.coreWidth_BlurRad_Ratio = coreWidth_BlurRad_Ratio;
	}
	
	public double getGausBlurRad() { return gausBlurRad;}
	public double getCoreWidth_BlurRad_Ratio() { return coreWidth_BlurRad_Ratio;}
	
}
