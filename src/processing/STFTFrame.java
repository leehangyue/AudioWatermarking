package processing;

import java.util.ArrayList;

/*
 * Interior class, not engaged in UI unless exception occurs
 * 
 * A frame of STFT data that implements FFT and IFFT operation
 * 
 */

import javax.swing.JOptionPane;

public class STFTFrame {

	private int n;//frame size, positive integer power of 2
	private double[] frame;
	private double[] FRAME_real;
	private double[] FRAME_imag;
	private FFT frameFFT;
	public STFTFrame(int n) {
		this.n = n;
		frameFFT = new FFT(n);
	}
	
	public void setSize(int n) {
		this.n = n;
	}
	
	public int getSize() {
		return n;
	}
	
	public double[] hanningWindow(int n) {
	    // Make a hanning window with window width n:
		double[] window = new double[n];
	    for(int i = 0; i < n; i++) {
	    	window[i] = 0.5 * (1 - Math.cos(2*Math.PI*i/(n-1)));//Symmetric for even length
	    }
	    return window;
	}
	
	public void loadAudioFrame(double[] buffer) {//Correct and copy the input to "frame"
		frame = new double[n];
		int bufferLength = buffer.length;
		if(bufferLength != n) {
			JOptionPane.showMessageDialog(null, "class:STFT_Frame, input array of loadAudio must have "
					+ "the same length as framesize!\nTruncation or padding with zero is "
					+ "done to retain STFT frame lenth.\n");//Warning. The program continues
		}
		bufferLength = Math.min(n,bufferLength);
		for(int i=0; i<bufferLength; i++) {
			frame[i] = buffer[i];//the default value of an array of double is zero
		}
	}
	
	public double[] getAudioFrame() {
		return frame;
	}
	
	public void calcFFT() {
		FRAME_real = frame;
		FRAME_imag = new double[n];
		frameFFT.fft(FRAME_real, FRAME_imag);
	}
	
	public void calcIFFT() {//Could yield problems because the imag of generated audio is simply discarded!
		//https://www.dsprelated.com/showarticle/800.php
		double[] frame_imag;
		frame = FRAME_real;
		frame_imag = FRAME_imag;
		frameFFT.fft(frame_imag, frame);
		frame_imag = frame;//frame_imag is just a temp var and has nothing to do with imag
		for(int i=0; i<n; i++) {
			frame[i] /= n;
		}
	}
	
	public ArrayList<Complex> getFrameComplex() {
		ArrayList<Complex> ComplexFrame = new ArrayList<>();
		for(int i=0; i<n; i++){
			ComplexFrame.add(new Complex(FRAME_real[i], FRAME_imag[i]));;
		}
		//System.out.println("In STFTFrame: ComplexFrame.size() = " +  ComplexFrame.size());//DEBUG
		return ComplexFrame;
	}
	
	public void setFrameComplex (ArrayList<Complex> ComplexFrame) throws STFTException {
		if(ComplexFrame.size() != n) {
			throw new STFTException("Inproper use of getFrameComplex! The input array "
					+ "must have the same length as frame size.\n");
		}
		for(int i=0; i<n; i++) {
			FRAME_real[i] = ComplexFrame.get(i).real();
			FRAME_imag[i] = ComplexFrame.get(i).imag();
		}
	}
	
	public void setFrameComplex (double[] FrameReal, double[] FrameImag) throws STFTException {
		if(FrameReal.length != n || FrameImag.length != n) {
			throw new STFTException("Inproper use of getFrameComplex! The input array "
					+ "must have the same length as frame size.\n");
		}
			FRAME_real = FrameReal;
			FRAME_imag = FrameImag;
	}
	
	public double[] getFrameAmp() {
		double[] amp = new double[n];
		for(int i=0; i<n; i++) {
			amp[i] = Math.sqrt(FRAME_real[i]*FRAME_real[i] + 
					FRAME_imag[i]*FRAME_imag[i]);
		}
		return amp;
	}
	
	public double[] getFrameArg() {
		double[] arg = new double[n];
		for(int i=0; i<n; i++) {
			arg[i] = Math.atan2(FRAME_real[i], FRAME_imag[i]);
		}
		return arg;
	}
	
	public double[] getFramePower() {
		double[] power = new double[n];
		for(int i=0; i<n; i++) {
			power[i] = FRAME_real[i]*FRAME_real[i] + 
					FRAME_imag[i]*FRAME_imag[i];
		}
		return power;
	}
	/*
	private void ShowRMS(double[] inputArray, int id) {//DEBUG
		int n = inputArray.length;
		double rms = 0.0;
		double buf;
		for(int i=0; i<n; i++) {
			buf = inputArray[i];
			rms += buf*buf;
		}
		rms = Math.sqrt(rms/n);
		System.out.println("RMS = " + rms + ", id = " + id);
	}
	*/
}
