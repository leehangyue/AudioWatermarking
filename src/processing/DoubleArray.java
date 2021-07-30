package processing;

public class DoubleArray {
	// so that arrays of doubles can be modified in a function as parameters
	private double[][] mask1 = null;
	private double[][] mask2 = null;
	
	public double[][] getMask(int which) {
		if(which == 1) return mask1;
		if(which == 2) return mask2;
		return null;
	}
	
	public void setMast(int which, double[][] mask) {
		if(which == 1) mask1 = mask;
		if(which == 2) mask2 = mask;
	}
}
