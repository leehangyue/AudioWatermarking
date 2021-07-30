package pre_posts;

/**
 * Interior class, not engaged in UI unless exception occurs
 * 
 * Reference:
 * 
 * https://www.dyclassroom.com/image-processing-project/how-to-read-and-write-image-file-in-java
 * 
 * File: ReadWriteImage.java
 *
 * Description:
 * Read and write image.
 * author Yusuf Shakeel
 * Date: 26-01-2014 sun
 *
 * www.github.com/yusufshakeel/Java-Image-Processing-Project
 */

//SEARCH "ATTENTION" for suggestions on modifying!!
//SEARCH "to be removed" for debugging statements

import java.io.*;
import java.util.ArrayList;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import gui.VisWmBlur;
import gui.VisWmColor;
import processing.FFT;
import processing.KeySettings;

import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;

public class Watermark{
	
	/**
	 * Explanations
	 * smooth: Use larger STFT frame to suppress tone/vowel trebling, slower
	 * robust: Duplicate watermark image vertically along the frequency domain,
	 *         decreased watermark image resolution
	 *         
	 *         Prerequisite: height =< audioFrameSize/4
	 *  
	 * smooth, robust = 0, 0:
	 * STFT window width = 2048, audio frame width = 1024
	 * angular freq (w = watermark image) (0-pi and pi-2pi are symmetric around pi)
	 * pi
	 *      w  w  w  ... w = (width: per ori)
	 * 0
	 * decoded into w
	 * 
	 * smooth, robust = 0, 1:
	 * STFT window width = 2048, audio frame width = 1024
	 * angular freq (w = watermark image half size)
	 * pi
	 *      w  w  w  ... w = (width: per ori/2)
	 *      w  w  w  ... w
	 * 0
	 * 
	 *      w_applied =   w
	 *                    w
	 * decoded into w
	 *              w
	 * 
	 * 
	 * smooth, robust = 1, 0:
	 * STFT window width = 8192, audio frame width = 4096
	 * angular freq (w = watermark image)
	 * pi
	 *      w  w  w  ... w
	 *      w  w  w  ... w = (width: per ori)
	 *      w  w  w  ... w
	 *      w  w  w  ... w
	 * 0
	 * 
	 * 		w_applied =   w (4 "w"s in a column)
	 *                    w
	 *                    w
	 *                    w
	 * decoded into w by averaging the four "w"s
	 * 
	 * smooth, robust = 1, 1:
	 * STFT window width = 8192, audio frame width = 4096
	 * angular freq (w = watermark image half size)
	 * pi
	 * 		w_applied w_applied w_applied ... w_applied
	 * 0
	 * 
	 * 		w_applied = (8 "w"s in a column, width: per ori/2)
	 * 
	 * decoded into w by averaging the four pairs of "w"s
	 *              w                                 w
	 */
	public static final int maxWidth = 1024;
	private static final int maxWidthLog2 = 10;
	public static final int minWidth = 8;
	private static final int minWidthLog2 = 3;
	public static final int maxHeight = 400;
	public static final int minHeight = 8;
	public static final int widthStep = 8;//no need to modify this.SizeCorrect(..) if log2(widthStep) <= minWidthLog2
	public static final int heightStep = 4;
	
    private WatermarkDimension dimension = new WatermarkDimension((short)-1, (short)-1);
    private double offset = 0.0;
    private boolean smooth = false;
    private boolean robust = false;
    private int folds = 2;//Encryption folding level, horizontal (temporal) fold counts
    
    private BufferedImage oriImage = null;
	private BufferedImage image = null;
	private double[][] mask = null;//The coefs with which the STFT spectrum is modified
	private double[][] mask_blur = null;//The blurred mask
    
    private boolean isImgReadied = false;
    private boolean isKeyReadied = false;//whether or not the key is loaded or generated
    private boolean isNewKeySet = false;//whether the key parameters are all set or not
    private boolean isKeySaved = false;//whether or not the key is saved as a file
    private boolean isMaskReadied = false;
    private boolean encrypted;
    
    private short[] keyByWidth;
    private short[] keyByHeight;
    
    public Watermark(short width, short height, boolean smooth, boolean robust) {
    	//Only for applying a watermark
    	dimension = new WatermarkDimension(width, height);
    	this.smooth = smooth;
    	this.robust = robust;
		dimension = sizeCorrect(dimension, true);
		isImgReadied = false;
		isKeyReadied = false;
		isNewKeySet = true;
		isKeySaved = false;
		isMaskReadied = false;
    }
    
    public Watermark(File f) throws IOException {//input watermark image
    	//Only for applying a watermark
    	loadImg(f);
    	isKeySaved = false;
    }
    
    public Watermark(boolean isModeDecode) {//When used for decoding, next step should be loading a key file
    	//For both applying (with package gui) and reading a watermark
    	encrypted = isModeDecode;
    	dimension = new WatermarkDimension((short)-1, (short)-1);
    	isImgReadied = false;
		isKeyReadied = false;
		isNewKeySet = false;
		isKeySaved = false;
		isMaskReadied = false;
    }
    
    public void loadImg(File f) throws IOException {
    	BufferedImage ori_backup = oriImage;
    	try {
    		oriImage = ImageIO.read(f);
    	}
    	catch (IOException e){
    		oriImage = ori_backup;
    		throw e;
    	}
    	dimension = new WatermarkDimension((short) oriImage.getWidth(), (short) oriImage.getHeight());
    	dimension = sizeCorrect(dimension, true);//This may modify width and height
    	if(!dimension.equals(new WatermarkDimension((short)oriImage.getWidth(), (short)oriImage.getHeight()))) {
    		image = resize(oriImage, dimension, RenderingHints.VALUE_INTERPOLATION_BICUBIC, true);
    	}
    	else image = oriImage;
    	encrypted = false;
    	isImgReadied = true;
    	isNewKeySet = true;
    	isKeyReadied = false;//the watermark dimensions can be modified
    	isMaskReadied = false;
    }
    
    public void loadImgAndResize(File f) throws IOException, WatermarkException {
    	//Only for applying a watermark
    	if(isNewKeySet) {
    		oriImage = ImageIO.read(f);//throws IOException
        	if(dimension.getWidth() != oriImage.getWidth() || dimension.getHeight() != oriImage.getHeight()) {
        		image = resize(oriImage, dimension, 
            			RenderingHints.VALUE_INTERPOLATION_BICUBIC, true);
        	}
        	else image = oriImage;
        	isImgReadied = true;
        	//the key is not modified, therefore isKeyReadied should remain unchanged
        	isMaskReadied = false;
    	} else throw new WatermarkException("The key is not yet set!");
    }
    
    public void loadKey(File keyInFile) throws WatermarkException {
		try {
			FileInputStream binIn = new FileInputStream(keyInFile);
			DataInputStream keyIn = new DataInputStream(binIn);
			short widthInFile = keyIn.readShort();
			short heightInFile = keyIn.readShort();
			smooth = keyIn.readBoolean();
			robust = keyIn.readBoolean();
			offset = keyIn.readDouble();
			offset = Math.max(offset, -1.0);
	    	offset = Math.min(offset, 1.0);
	    	
			if((dimension.getWidth() != -1 || dimension.getHeight() != -1) && 
					(widthInFile != dimension.getWidth() || heightInFile != dimension.getHeight())) {
				//width or height has been set, and they does not match those in the key
				System.out.println("The new key has overwritten the old dimensions");
				setSize(widthInFile, heightInFile);
				/*int ans_INT;
				ans_INT = JOptionPane.showConfirmDialog (null, 
						"Press Yes to resize current watermark, No for choosing another key.",
						"Key and current watermark mismatch", JOptionPane.YES_NO_OPTION);
				if(ans_INT == JOptionPane.YES_OPTION) {
					(setsize...)
				}
				else {
					keyIn.close();
	    			throw new WatermarkException("Please specify the key file again.\n");
				}*/
			}
			if(dimension.getWidth() == -1 && dimension.getHeight() == -1) {//width and height not yet set
				dimension = new WatermarkDimension(widthInFile, heightInFile);
			}
			this.keyByWidth = new short[dimension.getWidth()];
			this.keyByHeight = new short[dimension.getHeight()];
			for(short i=0; i<dimension.getWidth(); i++) {
				keyByWidth[i] = keyIn.readShort();
			}
			for(short i=0; i<dimension.getHeight(); i++) {
				keyByHeight[i] = keyIn.readShort();
			}
			keyIn.close();
			isKeyReadied = true;
			isNewKeySet = true;
			isKeySaved = true;//the key is loaded means that the key is already saved a priori
			isMaskReadied = false;//because the key can be different and the mask can be out-dated
		}
		catch (FileNotFoundException e) {
			JOptionPane.showMessageDialog(null, 
					"Error: Binary key file " + keyInFile + " is not found!\n"
							+ "Exception msg: " + e + "\n");
			throw new WatermarkException("Please specify the key file again.\n");
		}
		catch (IOException eIO) {
			JOptionPane.showMessageDialog(null, 
					"Error reading key file " + keyInFile + "\n"
							+ "Exception msg: " + eIO + "\n");
			throw new WatermarkException("Please specify the key file again.\n");
		}
	}

	public void saveKey(File keyFile) throws WatermarkException {//Only for applying a watermark
		int ans_int;
		JFileChooser chooser = new JFileChooser();
		File newKeyFile;
		while(keyFile.exists()) {
			ans_int = JOptionPane.showConfirmDialog(null, "Output keyfile: " + keyFile 
					+ " already exists! Overwrite?\n"
					+ "Press YES to overwrite existing keyfile, press NO to give a new path and name.\n"
					+ "Press cancel to abort saving.\n", 
					"Key already exists", JOptionPane.YES_NO_CANCEL_OPTION);
			if(ans_int == JOptionPane.NO_OPTION) {
				chooser.setCurrentDirectory(new java.io.File(keyFile.getPath()));
				chooser.setDialogTitle("Save a new key");
				chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				chooser.showSaveDialog(null);
				if(chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION){
					newKeyFile = chooser.getSelectedFile();
					try {
						keyFile = newKeyFile.getCanonicalFile();
					} catch(IOException e) {
						JOptionPane.showMessageDialog(null, "The path name can not be resolved. "
								+ "Please choose again.",//choose again means to decide if to overwrite again
								"Unsupported path", JOptionPane.YES_OPTION);
					}
			    }
				else return;//That means to cancel saving
			}
			else if(ans_int == JOptionPane.YES_OPTION) break;//The "overwrite?" question received a "yes", then just go to the try-catch block below
			else return;
		}
		try {//write key file in a binary format
			FileOutputStream binOut = new FileOutputStream(keyFile);
			DataOutputStream keyOut = new DataOutputStream(binOut);
			keyOut.writeShort(dimension.getWidth());
			keyOut.writeShort(dimension.getHeight());
			keyOut.writeBoolean(smooth);
			keyOut.writeBoolean(robust);
			keyOut.writeDouble(offset);
			for(short i=0; i<keyByWidth.length; i++) {
				keyOut.writeShort(keyByWidth[i]);//if robust, only half of keyByWidth is used
			}
			for(short i=0; i<keyByHeight.length; i++) {
				keyOut.writeShort(keyByHeight[i]);
			}
			keyOut.close();
			isKeySaved = true;
		}
		catch (IOException eIO) {
			JOptionPane.showMessageDialog(null, 
					"Error writing key file " + keyFile + "\n"
							+ "Exception msg: " + eIO + "\n");
			throw new WatermarkException("Error writing key file.\n");
		}
	}

	public void saveDecodedWatermark(File outputfile) throws WatermarkException {
		if(image == null) maskToImage();
		try {
		    ImageIO.write(image, "png", outputfile);
		} catch (IOException e) {
		    throw new WatermarkException("Error saving decoded image!\n"
		    		+ "Error message: " + e);
		}
	}
	
	public static void saveImage(BufferedImage image, File outputfile) throws WatermarkException {
		try {
			outputfile.createNewFile();
		}
		catch(IOException e1) {
			System.out.println("Error creating png files!");
		}
		try {
		    ImageIO.write(image, "png", outputfile);
		} catch (IOException e) {
		    throw new WatermarkException("Error saving decoded image!\n"
		    		+ "Error message: " + e);
		}
	}
	
	public boolean isEncrypted() {//Getter, but "getEncrypted" sounds like "become encrypted"
    	return encrypted;
    }
	
	public boolean isImgReadied() {
		return isImgReadied;
	}
    
	public boolean isKeyReadied() {
		return isKeyReadied;
	}
	
	public boolean isNewKeySet() {
		return isNewKeySet;
	}
	
	public boolean isKeySaved() {
		return isKeySaved;
	}
	
	public boolean isMaskReadied() {
		return isMaskReadied;
	}
	
    public boolean isModeSmooth() {
    	return smooth;
    }
    
    public boolean isModeRobust() {
    	return robust;
    }
    
    public int getFold() {
    	return folds;
    }
    
    public double getOffset() {
		return offset;
	}

	public short getWidth() {
    	return dimension.getWidth();
    }
    
    public short getHeight() {
    	return dimension.getHeight();
    }
    
    public double[][] getMask(){
    	return mask;
    }
    
    public double[][] getMask2(double amp, int windowWidth) {//Only for applying a watermark
		//getMask"2" has this 2 because the returned mask_ret is stretched along its height by 2
		//Due to the hanning window overlapping, the window size is halved when reading watermark
		// to avoid the overlap, therefore the frequency resolution is halved, so the mask must be
		// stretched to avoid watermark data loss.
    	if(mask == null) return null;
		amp = Math.max(amp, 0.0);
		amp = Math.min(amp, 1.0);
		offset = Math.max(offset, -1.0);
		offset = Math.min(1.0, offset);//Determines in which frequency band should the watermark be
		double[][] mask_ret;
		double temp;
		if(!smooth) {
			mask_ret = new double[mask.length][windowWidth];
			for(short i=0; i<mask_ret.length; i++) {
				for(short j=0; j<mask_ret[0].length; j++) {
					mask_ret[i][j] = 1.0;//Initialize
				}
			}
	    	if(windowWidth/4 <= dimension.getHeight()+1) {//which shall never happen unless bugs exist
	    		System.out.println("Warning from Watermark.getMask(): "
	    				+ "half windowWidth is less than height!"
	    				+ "The returned mask will be trimmed.");
	    		for(short i=0; i<mask.length; i++) {
	        		for(short j=2; j<windowWidth/2; j++) {//zero frequency mask_ret[i][0] should not be modified
	        			temp = 1.0 - mask[i][j]*amp;
	        			mask_ret[i][windowWidth-j] = temp;
	        			mask_ret[i][j] = temp;
	        		}
	        	}
	    	}
	    	else {//Normal situation
	    		short maskStartHeight = (short)((windowWidth/2 - 2*dimension.getHeight())/4);
	    		maskStartHeight *= 2;
	    		short maskEndHeight = (short)(maskStartHeight + 2*dimension.getHeight());
	    		short offsetShort = (short)((maskStartHeight)*offset);
	    		offsetShort -= Math.signum(offset) * (offsetShort%2);
	    		for(short i=0; i<mask.length; i++) {
	        		for(short j=maskStartHeight; j<maskEndHeight; j++) {
	        			temp = 1.0 - mask[i][(int)((j-maskStartHeight)/2)]*amp;
	        			mask_ret[i][windowWidth-j-offsetShort] = temp;
	        			mask_ret[i][j+offsetShort] = temp;
	        		}
	        	}
	    	}
		}
		else {//smooth
			mask_ret = new double[mask.length][windowWidth];
			for(short i=0; i<mask_ret.length; i++) {
				for(short j=0; j<mask_ret[0].length; j++) {
					mask_ret[i][j] = 1.0;//Initialize
				}
			}
	    	if(windowWidth/16 <= dimension.getHeight()+1) {//which shall never happen unless bugs exist
	    		System.out.println("Warning in Watermark.getMask(): "
	    				+ "half windowWidth is less than height!"
	    				+ "The returned mask will be trimmed.");
	    		int maskHeight = mask[0].length, maskLength = mask.length;
	    		int ofst1 = maskLength / 4, ofst2 = 2 * ofst1, ofst3 = 3 * ofst1;
	    		for(short i=0; i<maskLength; i++) {
	        		for(short j=2; j<windowWidth/2; j++) {//zero frequency mask_ret[i][0] should not be modified
	        			temp = 1.0 - mask[i][j]*amp;
	        			mask_ret[(i+ofst3)%maskLength][windowWidth-j-6*maskHeight] = temp;
	        			mask_ret[(i+ofst3)%maskLength][j+6*maskHeight] = temp;
	        			mask_ret[(i+ofst2)%maskLength][windowWidth-j-4*maskHeight] = temp;
	        			mask_ret[(i+ofst2)%maskLength][j+4*maskHeight] = temp;
	        			mask_ret[(i+ofst1)%maskLength][windowWidth-j-2*maskHeight] = temp;
	        			mask_ret[(i+ofst1)%maskLength][j+2*maskHeight] = temp;
	        			mask_ret[i][windowWidth-j] = temp;
	        			mask_ret[i][j] = temp;
	        		}
	        	}
	    	}
	    	else {//Normal situation
	    		short maskStartHeight = (short)((windowWidth/2 - 8*dimension.getHeight())/4);
	    		maskStartHeight *= 2;
	    		short maskEndHeight = (short)(maskStartHeight + 2*dimension.getHeight());
	    		//maskEndHeight is the end height of the lowest mask (as there are four in a column)
	    		short offsetShort = (short)((maskStartHeight)*offset);
	    		offsetShort -= Math.signum(offset) * (offsetShort%2);
	    		for(short i=0; i<mask.length; i++) {
	        		for(short j=maskStartHeight; j<maskEndHeight; j++) {
	        			temp = 1.0 - mask[i][(int)((j-maskStartHeight)/2)]*amp;
	        			mask_ret[i][windowWidth-j-6*dimension.getHeight()-offsetShort] = temp;
	        			mask_ret[i][j+6*dimension.getHeight()+offsetShort] = temp;
	        			mask_ret[i][windowWidth-j-4*dimension.getHeight()-offsetShort] = temp;
	        			mask_ret[i][j+4*dimension.getHeight()+offsetShort] = temp;
	        			mask_ret[i][windowWidth-j-2*dimension.getHeight()-offsetShort] = temp;
	        			mask_ret[i][j+2*dimension.getHeight()+offsetShort] = temp;
	        			mask_ret[i][windowWidth-j-offsetShort] = temp;
	        			mask_ret[i][j+offsetShort] = temp;
	        		}
	        	}
	    	}
		}
		return mask_ret;
	}

	public BufferedImage getImage() {//Only for applying a watermark
		return image;
	}

	public BufferedImage getOriImage() {//Only for applying a watermark
		return oriImage;
	}
	
	public void setKey(KeySettings keySettings) {
		setOffset(keySettings.getOffset());
		setSize((short) keySettings.getWidth(), (short) keySettings.getHeight());
		setMode(keySettings.isSmooth(), keySettings.isRobust());
		setFold(keySettings.getFold());
	}
	
	public void setMode(boolean smooth, boolean robust) {
		this.smooth = smooth;
		this.robust = robust;
		isKeyReadied = false;
		//this has nothing to do with isNewKeySet, because the default value can be used
		isKeySaved = false;
		isMaskReadied = false;
	}

	public void setFold(int folds) {
		this.folds = folds;
		isKeyReadied = false;
		//this has nothing to do with isNewKeySet, because the default value can be used
		isKeySaved = false;
		isMaskReadied = false;
	}

	public void setOffset(double offset) {//Only for applying a watermark
		offset = Math.max(offset, -1.0);
		offset = Math.min(offset, 1.0);
		this.offset = offset;
		isKeyReadied = false;
		//this has nothing to do with isNewKeySet, because the default value can be used
		isKeySaved = false;
		isMaskReadied = false;
	}

	public void setSize(WatermarkDimension dim) {//Only for applying a watermark
    	setSize(dim.getWidth(), dim.getHeight());
    }
    
    public void setSize(short width, short height) {//Only for applying a watermark
    	dimension = new WatermarkDimension(width, height);
    	dimension = sizeCorrect(dimension, true);
    	if(image != null) {
    		if(width != image.getWidth() || height != image.getHeight()) {
        		image = resize(oriImage, dimension, RenderingHints.VALUE_INTERPOLATION_BICUBIC, true);
        	}
    	}
    	isKeyReadied = false;
    	isNewKeySet = true;
    	isKeySaved = false;
    	isMaskReadied = false;
    }
    
    public void setMask(double[][] decodedMask) {//Only for decoding or reading a watermark
		
		int windowWidth = decodedMask[0].length;
		int decodedWidth = decodedMask.length;
		double[][] mask_temp = new double[decodedWidth][dimension.getHeight()];
		WatermarkDimension temp_dim;
		
		//convert decodedMask to mask
		if(!smooth) {
			short maskStartHeight = (short)((windowWidth/2 - dimension.getHeight())/2);
			short offsetShort = (short)((maskStartHeight)*offset);
			maskStartHeight = (short)Math.max(maskStartHeight, 1);//zero frequency decoded[i][0] should not be modified, as described in this.getMask2()
			for(int i=0; i<decodedWidth; i++) {
				for(int j=0; j<dimension.getHeight(); j++) {
					mask_temp[i][j] = (decodedMask[i][j + maskStartHeight + offsetShort]
							+ decodedMask[i][windowWidth - j - maskStartHeight - offsetShort]) / 2 ;
				}
			}
		}
		else {//smooth
			short maskStartHeight = (short)((windowWidth/2 - 4*dimension.getHeight())/2);
			short offsetShort = (short)((maskStartHeight)*offset);
			short maskStartHeight_temp = maskStartHeight;
			maskStartHeight = (short)Math.max(maskStartHeight, 1);
			//zero frequency decoded[i][0] should not be modified, as described in this.getMask2()
			int[] ofst = new int[4];
			ofst[0] = 0; ofst[1] = decodedWidth/4; ofst[2] = 2* ofst[1]; ofst[3] = 3 * ofst[1];
			for(int i=0; i<decodedWidth; i++) {//pick out the mask from the spectrum (decoded mask from the audio)
				maskStartHeight_temp = maskStartHeight;
				for(int fourx=0; fourx<4; fourx++) {//Four times
					for(int j=0; j<dimension.getHeight(); j++) {
	    				mask_temp[i][j] = (decodedMask[(i+ofst[fourx])%decodedWidth][j + maskStartHeight_temp + offsetShort]
	    						+ decodedMask[(i+ofst[fourx])%decodedWidth][windowWidth - j - maskStartHeight_temp - offsetShort]) / 8 ;
	    			}
					maskStartHeight_temp += dimension.getHeight();//to read the next window at a higher (STFT)frequency
				}
			}
		}
		
		mask_temp = cancelAverage(mask_temp);
		
		temp_dim = new WatermarkDimension((short)decodedWidth, dimension.getHeight());
		if(sizeCheck(temp_dim)) {
			dimension = temp_dim;
			this.mask = new double[decodedWidth][dimension.getHeight()];
			for(int i=0; i<decodedWidth; i++) {
				for(int j=0; j<dimension.getHeight(); j++) {
					this.mask[i][j] = mask_temp[i][j];//copy the values instead of copy the pointer
				}
			}
			encrypted = true;
			isMaskReadied = true;
		}
		else {//the key file contains an illegal dimension. Decryption is not possible
			System.out.println("Input mask has an illegal size!");
			System.out.println("Failed setting mask.");
		}
		//This method is only used for decoding, therefore it is irrelevant to isKeyReadied
	}
    
    public void keyGen() throws WatermarkException {
    	if(isNewKeySet) {
    		keyGenWidth();
        	keyGenHeight();
        	isKeyReadied = true;
        	isKeySaved = false;
        	isMaskReadied = false;//Due to the random permutation, the key is almost surely different
    	} else throw new WatermarkException("Cannot gen key: the key is not set");
    }
    
	public void encrypt() throws WatermarkException {
    	
    	if(encrypted == true) {
    		throw new WatermarkException("The mask is already encrypted!\n");
    	}
    	if(keyByWidth.length != dimension.getWidth() || keyByHeight.length != dimension.getHeight()) {
    		throw new WatermarkException("Encrypt: The key has different "
    				+ "dimensions from those of the watermark image!\n");
    	}
    	imageToMask();
    	double[][] buf = new double[keyByWidth.length][keyByHeight.length];
    	short widthLoop = dimension.getWidth();
    	if(robust) widthLoop /= 2;
    	for(short i=0; i<widthLoop; i++) {
    		for(short j=0; j<dimension.getHeight(); j++) {
    			buf[i][j] = mask[i][j];//copy
    		}//Efficiency: https://i.stack.imgur.com/9JiYq.png
    	}
    	for(short i=0; i<widthLoop; i++) {
    		for(short j=0; j<dimension.getHeight(); j++) {
    			mask[i][j] = buf[keyByWidth[i]][j];
    		}//Efficiency: https://i.stack.imgur.com/9JiYq.png
    	}
    	for(short i=0; i<widthLoop; i++) {
    		for(short j=0; j<dimension.getHeight(); j++) {
    			buf[i][j] = mask[i][j];//copy
    		}//Efficiency: https://i.stack.imgur.com/9JiYq.png
    	}
    	for(short i=0; i<widthLoop; i++) {
    		for(short j=0; j<dimension.getHeight(); j++) {
    			mask[i][j] = buf[i][keyByHeight[j]];
    			//System.out.println(keyByHeight[j]);
    		}//Efficiency: https://i.stack.imgur.com/9JiYq.png
    	}
    	encrypted = true;
    	isMaskReadied = true;
    }
    
    public void decrypt() throws WatermarkException {//Only for decoding or reading a watermark
    	//DEBUG
    	if(isMaskReadied == false) {
    		throw new WatermarkException("The mask is not yet readied! "
    				+ "Please set a mask before decrypting.\n");
    	}
    	if(encrypted == false) {
    		throw new WatermarkException("The mask is not yet encrypted "
    				+ "or already decrypted!\n");
    	}
    	if(keyByWidth.length != dimension.getWidth() || keyByHeight.length != dimension.getHeight()) {
//    		System.out.println("Error: " + keyByWidth.length + ", " + dimension.getWidth() + ", " + keyByHeight.length + ", " + dimension.getHeight());
    		boolean throwException = true;
    		if(robust) {
    			if(keyByWidth.length == 2 * dimension.getWidth() && keyByHeight.length == dimension.getHeight()) {
    				throwException = false;
    			}
    		}
    		if(throwException) {
    			throw new WatermarkException("Encrypt: The key has different "
        				+ "dimensions from those of the watermark image!\n");
    		}
    	}
    	
    	short widthLoop = dimension.getWidth();
    	short heightLoop = dimension.getHeight();
    	double[][] buf = new double[keyByWidth.length][keyByHeight.length];
    	for(short i=0; i<widthLoop; i++) {
    		for(short j=0; j<heightLoop; j++) {
    			buf[i][j] = mask[i][j];//copy
    		}//Efficiency: https://i.stack.imgur.com/9JiYq.png
    	}
    	for(short i=0; i<widthLoop; i++) {
    		for(short j=0; j<heightLoop; j++) {
    			mask[keyByWidth[i]][j] = buf[i][j];
    		}//Efficiency: https://i.stack.imgur.com/9JiYq.png
    	}
    	
    	for(short i=0; i<widthLoop; i++) {
    		for(short j=0; j<heightLoop; j++) {
    			buf[i][j] = mask[i][j];//copy
    		}//Efficiency: https://i.stack.imgur.com/9JiYq.png
    	}
    	for(short i=0; i<widthLoop; i++) {
    		for(short j=0; j<heightLoop; j++) {
    			mask[i][keyByHeight[j]] = buf[i][j];
    			//System.out.println(keyByHeight[j]);
    		}//Efficiency: https://i.stack.imgur.com/9JiYq.png
    	}
    	
    	encrypted = false;
    	
//    	System.out.println("Binary distribution confidence of decoded watermark = "+calcBinConfidence(mask));
    	//ShowHistogram(mask, 10);//DEBUG
    }
    
    public void visualizeAnyImage(BufferedImage imageToShow) {//Do not use in the package GUI
		if(encrypted == true) {
			//JOptionPane.showMessageDialog(null, "Watermark is not decoded and might be unreadable.\n");
			System.out.println("Watermark is not decoded and might be unreadable.");
		}
	    //Reference: https://stackoverflow.com/questions/14353302/displaying-image-in-java
	    //Reference written by https://stackoverflow.com/users/2009887/abdul-rasheed at Sep 18 '13 at 14:15, 2017
	    ImageIcon icon=new ImageIcon(imageToShow);
	    JFrame frame=new JFrame();
	    frame.setLayout(new FlowLayout());
	    frame.setSize(imageToShow.getWidth(), imageToShow.getHeight());
	    JLabel lbl=new JLabel();
	    lbl.setIcon(icon);
	    frame.add(lbl);
	    frame.setVisible(true);
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public BufferedImage visualizeResult(boolean popNewFrame) {
		if(encrypted == true) {
			//JOptionPane.showMessageDialog(null, "Watermark is not decoded and might be unreadable.\n");
			System.out.println("Watermark is not decoded and might be unreadable.");
		}
		maskToImage();
	    //Reference: https://stackoverflow.com/questions/14353302/displaying-image-in-java
	    //Reference written by https://stackoverflow.com/users/2009887/abdul-rasheed at Sep 18 '13 at 14:15, 2017
		
		if(popNewFrame) {//Do not use in the package GUI
			ImageIcon icon=new ImageIcon(image);
		    JFrame frame=new JFrame();
		    frame.setLayout(new FlowLayout());
		    frame.setSize(2*image.getWidth(), 2*image.getHeight());
		    JLabel lbl=new JLabel();
		    lbl.setIcon(icon);
		    frame.add(lbl);
		    frame.setVisible(true);
		    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		}
		return image;
	}

	/**
	 * Reference:
	 * 
	 * https://stackoverflow.com/questions/24745147/java-resize-image-without-losing-quality
	 * Answer by Macro 13, Jul 14 '14 at 21:21
	 * https://stackoverflow.com/users/3182664/marco13
	 * 
	 */
	public static BufferedImage resize(
			final BufferedImage img, WatermarkDimension targetDim, Object hint, 
			boolean higherQuality)
	{
		short targetWidth = targetDim.getWidth();
		short targetHeight = targetDim.getHeight();
		
		int type =
				(img.getTransparency() == Transparency.OPAQUE)
					? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
		BufferedImage ret = (BufferedImage) img;
		short w, h;
		if (higherQuality)
		{
			// Use multi-step technique: start with original size, then
			// scale down in multiple passes with drawImage()
			// until the target size is reached
			w = (short) img.getWidth();
			h = (short) img.getHeight();
		}
		else
		{
			// Use one-step technique: scale directly from original
			// size to target size with a single drawImage() call
			w = targetWidth;
			h = targetHeight;
		}
		
		do
		{
			if (higherQuality && w > targetWidth)
			{
				w /= 2;
				if (w < targetWidth)
				{
					w = targetWidth;
				}
			}
	
			if (higherQuality && h > targetHeight)
			{
				h /= 2;
				if (h < targetHeight)
				{
					h = targetHeight;
				}
			}
			
			BufferedImage tmp = new BufferedImage(w, h, type);
			Graphics2D g2 = tmp.createGraphics();
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
			g2.drawImage(ret, 0, 0, w, h, null);
			g2.dispose();
			
			ret = tmp;
		} while (w != targetWidth || h != targetHeight);
		
		return ret;
	}

	private void keyGenWidth() throws WatermarkException {//Only for applying a watermark
		if(dimension.getWidth() < 8) {
			throw new WatermarkException("Watermark mask width must not be less than 8.\n");
		}
		double log2Width = Math.log(dimension.getWidth())/Math.log(2.0);
		if(Math.floor(log2Width) != log2Width) {
			throw new WatermarkException("Watermark mask width must be integer exponents of 2.\n");
		}
		short[] keyByWidth = new short[dimension.getWidth()];
		short[] buf = new short[dimension.getWidth()];
		if(!robust) {
			folds = Math.floorMod(folds, (int)(log2Width));//Eliminate surplus folding processes
			
			keyByWidth = new short[dimension.getWidth()];
	    	for(short i=0; i < dimension.getWidth(); i++) {
	    		keyByWidth[i] = i;
	    	}
	    	for(int index=0; index<dimension.getWidth(); index++) buf[index] = keyByWidth[index];
	    	for(short i=0; i < folds; i++) {
	    		for(short j=0; j < dimension.getWidth()/2; j++) {
	    			keyByWidth[j] = buf[2*j];
	    			keyByWidth[j + dimension.getWidth()/2] = buf[2*j + 1];
	    		}
	    		for(int index=0; index<dimension.getWidth(); index++) buf[index] = keyByWidth[index];
	    	}
	    	//for(int index=0; index<dimension.getWidth(); index++) System.out.println(keyByWidth[index]);//DEBUG
		}
		else {//robust
			folds = Math.floorMod(Math.max(folds-1, 0), (int)(log2Width/2 - 1));
	    	if(folds == 0) folds = (int)(log2Width/2 - 1);//Eliminate surplus folding processes
			short[] keyByWidth_temp = new short[dimension.getWidth()/2];
	    	for(short i=0; i < dimension.getWidth()/2; i++) {
	    		keyByWidth_temp[i] = i;
	    	}
	    	for(int index=0; index<dimension.getWidth()/2; index++) buf[index] = keyByWidth_temp[index];
	    	for(short i=0; i < folds; i++) {
	    		for(short j=0; j < dimension.getWidth()/4; j++) {
	    			keyByWidth_temp[j] = buf[2*j];
	    			keyByWidth_temp[j + dimension.getWidth()/4] = buf[2*j + 1];
	    		}
	    		for(int index=0; index<dimension.getWidth()/2; index++) buf[index] = keyByWidth_temp[index];
	    	}
	    	short temp;
	    	for(int index=0; index<dimension.getWidth()/2; index++) {
	    		temp = keyByWidth_temp[index];
	    		keyByWidth[index] = temp;
	    		keyByWidth[index + dimension.getWidth()/2] = (short)(temp + dimension.getWidth()/2);
	    	}
		}
		this.keyByWidth = new short[dimension.getWidth()];
		for(int index=0; index<dimension.getWidth(); index++) {
			this.keyByWidth[index] = keyByWidth[index];
		}
	}

	private void keyGenHeight() throws WatermarkException {//Only for applying a watermark
		keyByHeight = new short[dimension.getHeight()];
		if(!robust) {
	    	try {
	    		keyByHeight = randPermutation(dimension.getHeight());
	    	}
	    	catch (WatermarkException e) {
	    		throw e;
	    	}
		}
		else {//robust
			short[] keyByHeight_temp = new short[dimension.getHeight()/2];
	    	try {
	    		keyByHeight_temp = randPermutation((short)(dimension.getHeight()/2));
	    	}
	    	catch (WatermarkException e) {
	    		throw e;
	    	}
	    	for(int i=0; i<dimension.getHeight()/2; i++) {
	    		keyByHeight[i] = keyByHeight_temp[i];
	    		keyByHeight[i+dimension.getHeight()/2] = (short) (keyByHeight_temp[i] + dimension.getHeight()/2);
	    	}
		}
	}

	private short[] randPermutation(short permSize) throws WatermarkException {
		ArrayList<Integer> choiceLeft = new ArrayList<>();
		if(permSize%heightStep != 0) {
			System.out.println("randPermutation: Warning! Input length "+
					permSize+" does not have the factor "+heightStep+", modified to "+(permSize+permSize%heightStep));
			permSize -= permSize%heightStep;
		}
		short[] perm = new short[permSize];//Initialize to zeros
		short randInt;
		if(permSize < 2) {
			throw new WatermarkException("private method randPermutation: "
					+ "the input short array length must be greater than 2\n");
		}
		for(short i=0; i<permSize/heightStep; i++) {
			choiceLeft.add(new Integer(i));
		}
		for(short i=0; i<permSize/heightStep; i++) {
			randInt = (short) (Math.random() * choiceLeft.size());
			perm[i] = choiceLeft.get(randInt).shortValue();
			choiceLeft.remove(randInt);
		}
		if(heightStep > 1) {
			short[] permBackup = new short[(int)(permSize/heightStep)];
			for(int i=0; i<permSize/heightStep; i++) permBackup[i] = perm[i];//copy
			for(int j=0; j<heightStep; j++) {
				for(int i=0; i<permSize/heightStep; i++) {
					perm[heightStep*i + j] = (short)(heightStep*permBackup[i] + j);
				}
			}
		}
		/*for(short i=0; i<permSize; i++) {//DEBUG
			perm[i] = i;//DEBUG
			System.out.println("perm[i] = " + perm[i]);//DEBUG
		}*/
		return perm;
	}

	private boolean sizeCheck(WatermarkDimension dim) {
		WatermarkDimension newDim = sizeCorrect(dim, false);
		return newDim.equals(dim);
	}

	private WatermarkDimension sizeCorrect(WatermarkDimension dim, boolean printMessage){
		short width = dim.getWidth();
		short height = dim.getHeight();
		if(width < minWidth) {
			width = minWidth;
		}
		if(width > maxWidth) {
			width = maxWidth;
		}
		double log2Width = Math.log(width)/Math.log(2.0);
		if(log2Width > maxWidthLog2) log2Width = maxWidthLog2;//prevent numerical error from causing further troubles if it does
		if(log2Width < minWidthLog2) log2Width = minWidthLog2;
		if(Math.floor(log2Width) != log2Width) {
			width = (short)Math.round(log2Width);
			width = (short) (1<<width);
		}
		/*
		//this is not necessary if log2(widthStep) <= minWidthLog2
		if(width % widthStep != 0) {
			width += widthStep - (width % widthStep);
		}
		 */
		if(height > maxHeight) {
			height = maxHeight;
		}
		if(height < minHeight) {
			height = minHeight;
		}
		if(height % heightStep != 0) {
			height += heightStep - (height % heightStep);
		}
		WatermarkDimension newDim = new WatermarkDimension(width, height);
		if(printMessage && !newDim.equals(dim)) System.out.println("Watermark.SizeCheck: " + 
				"Inappropriate dimensions. Suggested size: width = " + width + ", height = " + height + ".");
		return newDim;
	}

	private void imageToMask() {
    	if(!robust) {
    		mask = new double[dimension.getWidth()][dimension.getHeight()];
        	//int[] RGBrow = new int[height];//Alpha component is neglected (OPAQUE)
    		int argb;//alpha red green blue
        	int maxMask = 0, minMask = 0xFFFFFF, buf;
        	for(short i=0; i<dimension.getWidth(); i++) {
        		for(short j=0; j<dimension.getHeight(); j++) {
        			argb = image.getRGB(i, j) & 0x00FFFFFF;
        			buf = (argb >> 16) & 0xFF;//red
        			buf += (argb >> 8) & 0xFF;//green
        			buf += argb & 0xFF;//blue
        			mask[i][j] = (double)buf;
        			if(buf > maxMask) maxMask = buf;
        			if(buf < minMask) minMask = buf;
        		}
        	}
        	double bufdb = 1.0 / (double)(maxMask - minMask);//buffer double
        	for(short i=0; i<dimension.getWidth(); i++) {
        		for(short j=0; j<dimension.getHeight(); j++) {
        			mask[i][j] = (mask[i][j] - (double)minMask) * bufdb;//Normalize
        		}
        	}
    	}
    	else {//robust
        	int shrinkedWidth = (int)(dimension.getWidth()/2);
        	int shrinkedHeight = (int)(dimension.getHeight()/2);
        	mask = new double[shrinkedWidth][dimension.getHeight()];//Two shrinked masks in height direction
        	BufferedImage shrinkedImage;
        	shrinkedImage = resize(image, new WatermarkDimension((short)shrinkedWidth,
        			(short)shrinkedHeight), RenderingHints.VALUE_INTERPOLATION_BICUBIC, 
                    true);
        	
        	int argb;//alpha red green blue
        	int maxMask = 0, minMask = 0xFFFFFF, buf;
        	for(short i=0; i<shrinkedWidth; i++) {
        		for(short j=0; j<shrinkedHeight; j++) {
        			argb = shrinkedImage.getRGB(i, j) & 0x00FFFFFF;
        			buf = (argb >> 16) & 0xFF;//red
        			buf += (argb >> 8) & 0xFF;//green
        			buf += argb & 0xFF;//blue
        			mask[i][j] = (double)buf;
        			if(buf > maxMask) maxMask = buf;
        			if(buf < minMask) minMask = buf;
        		}
        	}
        	double bufdb = 1.0 / (double)(maxMask - minMask);//buffer double
        	int ofst_8 = dimension.getWidth()/8;
        	for(short i=0; i<shrinkedWidth; i++) {
        		for(short j=0; j<shrinkedHeight; j++) {
        			mask[(i+ofst_8)%shrinkedWidth][j+shrinkedHeight] = mask[i][j] = (mask[i][j] - (double)minMask) * bufdb;//Normalize
        		}
        	}//Mask width halved, height unchanged
    	}
    	
    	//Binarize
    	double[][] maskBinErr = new double[mask.length][mask[0].length];
    	double threshold = 0.5;
    	{//GUI required for choosing proper threshold if the RMS of maskBinErr is too great
	    	threshold = 0.5;//DEBUG
	    	for(int i=0; i<mask.length; i++) {
	    		for(int j=0; j<mask[0].length; j++) {
	    			if(mask[i][j] > threshold) maskBinErr[i][j] = mask[i][j] - 1.0;
	    			else maskBinErr[i][j] = mask[i][j];
	    		}
	    	}//Now maskBin contains the deviation values from the binarized values
	    	showRMS(maskBinErr, "Binarization-caused error");//DEBUG
    	}//DEBUG
    	for(int i=0; i<mask.length; i++) {
    		for(int j=0; j<mask[0].length; j++) {
    			if(mask[i][j] > threshold) mask[i][j] = 1.0;
    			else mask[i][j] = 0.0;
    		}
    	}//Now mask contains the binarized values
    }
    
    private void maskToImage() {
    	maskBlur(new VisWmBlur(0.65, 1.5));
    	maskColoring(new VisWmColor(0.0, 0.5, -0.2, -0.1));
    }
    
    public BufferedImage maskToImage(double[][] mask, VisWmBlur blur, VisWmColor color) {
    	double[][] mask_blur = maskBlur(mask, blur);
    	BufferedImage image = maskColoring(mask_blur, robust, dimension, color);
    	return image;
    }
    
    private void maskBlur(VisWmBlur vb) {
    	mask_blur = maskBlur(mask, vb);
    }
    
    public double[][] maskBlur(double[][] mask, VisWmBlur vb) {
    	
    	int maskWidth = mask.length, maskHeight = mask[0].length;
    	double[][] mask_blur = new double[maskWidth][maskHeight];
    	
    	for(int i=0; i<maskWidth; i++) {
    		for(int j=0; j<maskHeight; j++) {
    			mask_blur[i][j] = mask[i][j];//copy
    		}
    	}

    	if(robust) {
    		double[][] buf = new double[maskWidth][maskHeight];
    		for(short i=0; i<maskWidth; i++) {
        		for(short j=0; j<maskHeight; j++) {
        			buf[i][j] = mask_blur[i][j];//copy
        		}//Efficiency: https://i.stack.imgur.com/9JiYq.png
        	}
    		mask_blur = new double[maskWidth * 2][maskHeight];
    		
    		double buffer = 0.0;
    		int half_height = (int) (maskHeight / 2);
    		int row2_offset = (int)(maskWidth / 4);
    		for(short i=0; i<maskWidth; i++) {
        		for(short j=0; j<half_height; j++) {
        			int ii = (i + row2_offset) % maskWidth;
        			buffer = buf[i][j] + 
        					 buf[ii][j + half_height];
        			buffer /= 2.0;
        			mask_blur[2*i][2*j] = buffer;
        			mask_blur[2*i][2*j + 1] = buffer;
        			mask_blur[2*i + 1][2*j] = buffer;
        			mask_blur[2*i + 1][2*j + 1] = buffer;
        			//System.out.println(keyByHeight[j]);
        		}//Efficiency: https://i.stack.imgur.com/9JiYq.png
        	}
    		maskWidth *= 2;
    	}
    	
    	double maskAverage = 0;
    	for(int i=0; i<maskWidth; i++) {
    		for(int j=0; j<maskHeight; j++) {
    			maskAverage += mask_blur[i][j];
    		}
    	}
    	maskAverage /= (maskWidth * maskHeight);
    	
    	//Blur the mask_blur DEBUG
    	//  Convolution core generation
    	double gausBlurRad = vb.getGausBlurRad();
    	double coreWidth_BlurRad_Ratio = vb.getCoreWidth_BlurRad_Ratio();//(core width) / (Blur radius)
    	int blurRadius = (int)Math.ceil(coreWidth_BlurRad_Ratio * gausBlurRad);
    	double[] convCore = new double[4*blurRadius+1];
    	for(int k=-2*blurRadius; k<=2*blurRadius; k++) {
    		convCore[k+2*blurRadius] = Math.exp(- k*k / (2*gausBlurRad*gausBlurRad));
    	}
    	//Now gausBlurRad is used as a temp var and has nothing to do with blur radius!
    	gausBlurRad = 0;
    	for(int k=-2*blurRadius; k<=2*blurRadius; k++) {
    		gausBlurRad += convCore[k+2*blurRadius];
    	}
    	for(int k=-2*blurRadius; k<=2*blurRadius; k++) {
    		convCore[k+2*blurRadius] /= gausBlurRad;//normalize
    	}
    	//  Convolution core generated
    	
    	double[] backup = new double[maskWidth + 4*blurRadius];
    	for(int j=0; j<maskHeight; j++) {//convolve
    		for(int i=0; i<maskWidth; i++) {//Copy central pixels
    			backup[i+2*blurRadius] = mask_blur[i][j];
    		}
    		for(int i=0; i<2*blurRadius; i++) {//Pad edges
    			backup[maskWidth + 4*blurRadius - 1 - i] = backup[i] = maskAverage;
    		}
    		for(int i=0; i<maskWidth; i++) {
    			mask_blur[i][j] = 0;
    			for(int k=-2*blurRadius; k<=2*blurRadius; k++) {
    				mask_blur[i][j] += backup[i+k+2*blurRadius] * convCore[-k+2*blurRadius];
    			}
        	}
    	}
    	backup = new double[maskHeight + 4*blurRadius];
    	for(int i=0; i<maskWidth; i++) {//convolve
    		for(int j=0; j<maskHeight; j++) {//Copy central pixels
    			backup[j+2*blurRadius] = mask_blur[i][j];
    		}
    		for(int j=0; j<2*blurRadius; j++) {//Pad edges
    			backup[maskHeight + 4*blurRadius - 1 - j] = backup[j] = maskAverage;//DEBUG
    		}
    		for(int j=0; j<maskHeight; j++) {
    			mask_blur[i][j] = 0;
    			for(int k=-2*blurRadius; k<=2*blurRadius; k++) {
    				mask_blur[i][j] += backup[j+k+2*blurRadius] * convCore[-k+2*blurRadius];
    			}
        	}
    	}//Mask_visualize blurring done
    	
    	//Normalize the mask
    	double maskMin = 1e307, maskMax = -1e307, range;
    	for(int i=0; i<maskWidth; i++) {
    		for(int j=0; j<maskHeight; j++) {
    			if(mask_blur[i][j] < maskMin) maskMin = mask_blur[i][j];
    			if(mask_blur[i][j] > maskMax) maskMax = mask_blur[i][j];
    		}
    	}
    	range = 1 / Math.max(maskMax-maskMin, 1e-3);
    	for(int i=0; i<maskWidth; i++) {
    		for(int j=0; j<maskHeight; j++) {
    			mask_blur[i][j] = (mask_blur[i][j] - maskMin) * range;
    		}
    	}
    	//Mask_visualize normalization done
    	return mask_blur;
    }
    
    private void maskColoring(VisWmColor vc) {
    	image = maskColoring(mask_blur, robust, dimension, vc);
    }
    
    public static BufferedImage maskColoring(double[][] mask_blur, boolean robust, WatermarkDimension dimension, VisWmColor vc) {	
    	int fakeColorMask;
    	int widthLoop = dimension.getWidth();
    	double temp;
    	double bright, contrast;//Adjust brightness and contrast. Both range from -1 to 1
    	bright = vc.getBright();//[-1,1] slider DEBUG
    	contrast = vc.getContrast();//[-1,1] slider
    	bright = Math.exp(-2 * bright);
    	contrast = Math.exp(-2 * contrast);
    	double colorTemp, tint;//colorTemp: blue-yellow, tint: green-red, not LAB color!
    	colorTemp = vc.getColorTemp();//[-1,1] slider
    	tint = vc.getTint();//[-1,1] slider
    	double redExp, greenExp, blueExp;
    	blueExp = Math.exp(colorTemp);//temporary value for red and green calculations
    	redExp = Math.exp(-2 * tint)/blueExp;
    	blueExp *= blueExp;//the final value
    	greenExp = 1 / (redExp * blueExp);
//    	if(robust) widthLoop /= 2;  // width is doubled in maskBlur, width of mask_blur is twice as width of mask if robust
    	BufferedImage image = new BufferedImage(widthLoop*2, dimension.getHeight(), BufferedImage.TYPE_INT_RGB);
        for(short x = 0; x<widthLoop; x++) {
            for(short y = 0; y<dimension.getHeight(); y++) {
            	temp = Math.pow(mask_blur[x][y], bright);//range of temp [0,1]
            	temp = temp*2 - 1;//move to the range [-1,1]
            	temp = Math.signum(temp) * Math.pow(Math.abs(temp), contrast);//Odd symmetric power func
            	temp = (temp + 1.0) / 2;//move back to the range [0,1]
            	fakeColorMask = (int)(Math.pow(temp, redExp) * 256);
            	fakeColorMask = (int)(Math.pow(temp, greenExp) * 256) + (fakeColorMask<<8);
            	fakeColorMask = (int)(Math.pow(temp, blueExp) * 256) + (fakeColorMask<<8);
                image.setRGB(x, y, fakeColorMask);
                image.setRGB(x + widthLoop, y, fakeColorMask);
            }
        }
        return image;
    }
    
    private double[][] cancelAverage(double[][] maskIn){
    	int maskInWidth = maskIn.length;
    	int maskInHeight = maskIn[0].length;
    	
    	double[] maskAverage;
		
		maskAverage = new double[maskInWidth];
		for(int i=0; i<maskInWidth; i++) {
			for(int j=0; j<maskInHeight; j++) {
				maskAverage[i] += maskIn[i][j];
			}
		}
		double temp;
		for(int i=0; i<maskInWidth; i++) {
			temp = maskAverage[i] / maskInHeight;
			for(int j=0; j<maskInHeight; j++) {
				maskIn[i][j] -= temp;//cancel time variance
			}
		}
		
		maskAverage = new double[maskInHeight];
		for(int i=0; i<maskInWidth; i++) {
			for(int j=0; j<maskInHeight; j++) {
				maskAverage[j] += maskIn[i][j];
			}
		}
		for(int j=0; j<maskInHeight; j++) {
			maskAverage[j] /= maskInWidth;
		}
		for(int i=0; i<maskInWidth; i++) {
			for(int j=0; j<maskInHeight; j++) {
				maskIn[i][j] -= maskAverage[j];//cancel frequency variance
			}
		}
		return maskIn;
    }
    
    private double calcRMS(double[][] inputArray) {
    	int m = inputArray.length;
		int n = inputArray[0].length;
		double rms = 0.0;
		double buf;
		for(int i=0; i<m; i++) {
			for(int j=0; j<n; j++) {
				buf = inputArray[i][j];
				rms += buf*buf;
			}
		}
		rms = Math.sqrt(rms/(m*n));
		return rms;
    }
    
    private void showRMS(double[][] inputArray, String id) {//DEBUG
		System.out.println("RMS = " + calcRMS(inputArray) + ", id = " + id);
	}

    private double calcBinConfidence(double[][] inputArray) {
    	int m = inputArray.length, n = inputArray[0].length;
    	int me = (int)Math.ceil(Math.log(m)/Math.log(2));
    	int ne = (int)Math.ceil(Math.log(n)/Math.log(2));
    	me = 1<<me; ne = 1<<ne;
    	double[][] arr = new double[m][n];
    	double[] arrTemp1, arrTemp2;
    	double res = 0.0, filterLoFreq = 0.01, filterHiFreq = 0.125, 
    			filterLoTime = 0.05,  filterHiTime = 0.125;//DEBUG
		//Copy and pad zeros (zeros are originally there)
		for(int i=0; i<m; i++) { for(int j=0; j<n; j++) { arr[i][j] = inputArray[i][j];}}
		
		//Clear average and retain the padded zeros
		/*
		res = 0;
		for(int i=0; i<m; i++) { for(int j=0; j<n; j++) { res += arr[i][j];}}
		res /= (m*n);
		for(int i=0; i<m; i++) { for(int j=0; j<n; j++) { arr[i][j] -= res;}}
		*/
		
		//Apply 2d fft
		FFT fftCalc = new FFT(ne);
		arrTemp1 = new double[ne];
		for(int i=0; i<me; i++) {
			for(int j=0; j<ne; j++) arrTemp1[j] = arr[i][j];
			arrTemp2 = new double[ne];
			fftCalc.fft(arrTemp1, arrTemp2);
			for(int j=0; j<ne; j++) arr[i][j] = Math.sqrt(Math.pow(arrTemp1[j], 2) 
					+ Math.pow(arrTemp2[j], 2));
		}
		fftCalc = new FFT(me);
		arrTemp1 = new double[me];
		for(int j=0; j<ne; j++) {
			for(int i=0; i<me; i++) arrTemp1[i] = arr[i][j];
			arrTemp2 = new double[me];
			fftCalc.fft(arrTemp1, arrTemp2);
			for(int i=0; i<me; i++) arr[i][j] = Math.sqrt(Math.pow(arrTemp1[i], 2) 
					+ Math.pow(arrTemp2[i], 2));
		}
		
		res = calcRMS(arr);
		for(int i=0; i<me; i++) { for(int j=0; j<ne; j++) { arr[i][j] /= res; arr[i][j] *= (arr[i][j]/(me*ne));}}
		//Now arr is the power spectrum of the decoded watermark image
		res = 0;
		for(int i=(int)(me*filterLoTime); i<me*filterHiTime; i++) { for(int j=(int)(ne*filterLoFreq); j<ne*filterHiFreq; j++) 
			{ res += arr[i][j] + arr[me-i-1][j] + arr[me-i-1][ne-j-1] + arr[i][ne-j-1]; }}
		//Move the range linearly from 0.12 - 0.16 to 0 - 1
		//res = (res - 0.12) / (0.16 - 0.12);
		//res = Math.max(0.0, Math.min(1.0, res));
		
		return res;
    }
    
    
	/*private double CalcBinConfidence_Distribution(double[][] inputArray) {
    	//Assumption: inputArray elements obey such distribution of Z
    	//Z = X(Bi(-1/(2-2p), 1/(2p), p)) + Y(N(0, s^2)), 
    	//where X and Y are independent, and s is the noise level after normalization
		double aver = 0.0, devi = 0.0, skew = 0.0, kurt = 0.0, temp, temp2, 
				upAver = 0.0, dnAver = 0.0, max = Double.MIN_VALUE, min = Double.MAX_VALUE, r;
		int m = inputArray.length, n = inputArray[0].length, upCount = 0, dnCount = 0;
		double[] arr = new double[m*n];
		//Copy
		for(int i=0; i<m; i++) { for(int j=0; j<n; j++) { arr[i * n + j] = inputArray[i][j];}}
		//Find average and standardize
		for(int i=0; i<m*n; i++) { aver += arr[i];}
		aver /= (m*n);
		for(int i=0; i<m*n; i++) { arr[i] -= aver;}
		//Find deviation and normalize
		for(int i=0; i<m*n; i++) { devi += arr[i] * arr[i];}
		devi /= (m*n);
		temp = Math.sqrt(devi);
		for(int i=0; i<m*n; i++) { arr[i] /= temp;}
		//Find skewness and relative kurtosis (comparing to Standard Normal Distribution)
		for(int i=0; i<m*n; i++) {
			temp = arr[i];
			temp2 = temp*temp*temp;
			skew += temp2;
			kurt += temp2*temp;
		}
		skew /= (m*n);
		kurt /= (m*n*3);
		
		/*
		 * First, assume that the watermark component in the input array (or arr[][], the same)
		 *  obeys binary distribution X ~ Bi(-1, 1, p) where p is the probability that x = 1
		 *  and (assume) that the noise component in the input array 
		 *  obeys normal distribution Y ~ N(0, sigma^2) where sigma is the standard deviation
		 *  
		 * Then the input variable (arr[][]), let's say Z = X + Y, is normalized so that
		 *  its average equals 0 and variance equals 1, 
		 *  now that Z_s = X_s + Y_s, 
		 *   X_s ~ Bi(-k/(2p(1-p)), k/(2p), p) where k = 1 / sqrt(1 + sigma^2) is unknown
		 *   Y_s ~ N(0, s^2) where s = k * sigma / sqrt(4p(1-p)) (deduce with sigma = 0 if confused)
		 * 
		 * To find k, let's define a new variable "range".
		 *  Calculate the mid-range value m = (max+min)/2 (not the median!),
		 *  If we divide arr[][] into two parts, one no less than m and the other less than m
		 *  Find the averages of both parts: upAver and dnAver
		 *  then calculate their difference: range = (upAver+dnAver)/2
		 *  As X_s and Y_s are still independent, range = max(X_s) - min(X_s)
		 *  Therefore range = k / (2p(1-p)) (Similarly, deduce with sigma = 0 if confused)
		 * 
		 * Note that E(X_s) = 0, E(X_s^2) = k^2, 
		 *   E(X_S^3) = k^3 * (1-2p) / (2 * (2p(1-p))^2), 
		 *   E(X_s^4) = k^4 * (1-3p+3p^2) / (2 * (2p(1-p))^3)
		 *  and that E(Y_s) = 0, E(Y_s^2) = s^2, 
		 *   E(Y_s^3) = 0, E(Y_s^4) = 3*k^4
		 *  it can be derived that
		 *   E(Z_s^3) = E(X_s^3) and E(Z_s^4) = E(X_s^4) + 6 s^2 + 3 s^4
		 *  then
		 *   E(X_s^4) = 2*(E(X_s^3)^2) / range + range^3
		 *   E(X_s^4) = 2*(E(Z_s^3)^2) / range + range^3
		 *  therefore
		 *   3 s^4 + 6 s^2 = E(Z_s^4) - 2*(E(X_s^3)^2) / range - range^3
		 *  Let temp = 3 s^4 + 6 s^2 = E(Z_s^4) - 2*(E(X_s^3)^2)
		 *   then s = sqrt(sqrt(temp/3 + 1) - 1)
		 *  Note that s = k * sigma / sqrt(4p(1-p)) = sqrt(k) * sigma * sqrt(r/2) 
		 *    and k = 1 / sqrt(1 + sigma^2)
		 *   therefore 4*s^4 / r^2 = sigma^4 * (1 + sigma^2)
		 *   let t = sqrt(sqrt( sigma^4 * (1 + sigma^2) ))
		 *   then sigma can be approximated as t - t^2 / 7.8
		 *    when 0 <= sigma <= 1 with a maximal error of less than 1%
		 *    
		 *   Here, a result related to sigma is returned
		 */
		/*
		//Find denoised mid-range value
		for(int i=0; i<m*n; i++) {
			if(max < arr[i]) max = arr[i];
			if(min > arr[i]) min = arr[i];
		}
		r = (max + min) / 2;//Interim result
		for(int i=0; i<m*n; i++) {
			if(arr[i] < aver) {dnAver += arr[i]; dnCount++;}
			else {upAver += arr[i]; upCount++;}
		}
		if(upCount > 1) upAver /= upCount;
		if(dnCount > 1) dnAver /= dnCount;
		r = upAver - dnAver;//the range
		
		if(skew > 0.4 || kurt > 1.1) return 1.0;
		temp = kurt - 2*skew*skew/r - r*r*r;//temp
		temp = Math.max(0.0, temp);
		temp = Math.sqrt(temp/3 + 1) - 1;//s
		temp = Math.sqrt(4*temp*temp / (r*r));//t^2
		temp = Math.sqrt(temp) - temp / 7.8;//sigma
		System.out.println("temp, skew, kurt = "+temp+", "+skew+", "+kurt);//DEBUG
		temp = (3*Math.atan(skew*skew * 70 + 5*Math.abs(kurt - 1))) / (1.0 + 3*temp);
		return Math.min(1.0, Math.max(0.0, 1.5*temp - 0.5));//DEBUG
	}*/
    /*
    private void ShowHistogram(double[][] arrIn, int nbars) {//DEBUG
    	float[] bnds = new float[nbars+1];
	    int m = arrIn.length, n = arrIn[0].length;
	    int[] bndVal = new int[nbars];
	    double min = Double.MAX_VALUE, max = Double.MIN_VALUE, barWidth;
	    double[] arr = new double[m*n];
	    for(int i=0; i<m; i++) {
	    	for(int j=0; j<n; j++) {
	    		arr[i*n + j] = arrIn[i][j];
	    	}
	    }
	    for(int i=0; i<m*n; i++) {
	    	if(min > arr[i]) min = arr[i];
    		if(max < arr[i]) max = arr[i];
	    }
	    barWidth = (max - min) / nbars;
	    for(int i=0; i<nbars+1; i++) {
	    	bnds[i] = (float)(min + barWidth * i);
	    }
	    for(int i=0; i<m*n; i++) {
	    	arr[i] = (arr[i]-min) / barWidth;
	    	bndVal[Math.min((int)arr[i], nbars-1)]++;
	    }
	    for(int i=0; i<nbars; i++) {
	    	System.out.println("Bar "+(i+1)+" from "+bnds[i]+"	to "+bnds[i+1]+":	"+bndVal[i]);
	    }
    }
	*/
}