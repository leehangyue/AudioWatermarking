package processing;

/**
 * Exterior class, engaged in UI
 */

import pre_posts.*;

import java.awt.image.BufferedImage;
import java.io.*;

import javax.swing.DefaultBoundedRangeModel;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import gui.SignalFlag;
import gui.VisWmBlur;
import gui.VisWmColor;


public class ReadWatermark {
	
	Watermark watermark;//contains the key
	File wavInFile = null, imageOutFile1 = null, imageOutFile2 = null, keyFile = null;
	double[][] mask1 = new double[1][1];
	double[][] mask2 = new double[1][1];
	boolean saveWatermark = false;
	
	public ReadWatermark() {//Use GUI to obtain the wav and watermark files
		/*
		 * From: https://stackoverflow.com/questions/10621687/how-to-get-full-path-directory-from-file-chooser
		 * http://www.java2s.com/Code/Java/Swing-JFC/SelectadirectorywithaJFileChooser.htm
		 * Answered by https://stackoverflow.com/users/381161/malcolm-smith at May 16 '12 at 15:27, 2017
		 */
		
		wavInFile = chooseAudioInWithUI(".");
		
		String keyFilePath = wavInFile.toString();
		keyFilePath = keyFilePath.substring(0, keyFilePath.length()-4);
		keyFilePath += ".bin";//default key
		File keyFile = new File(keyFilePath);
		if(keyFile.exists()) {
			int ans_INT = JOptionPane.showConfirmDialog (null, 
					"Use the default key?", 
					"Default key found", JOptionPane.YES_NO_OPTION);
			if(ans_INT == JOptionPane.NO_OPTION)//chose not to use default key
				keyFile = chooseKeyFileWithUI(wavInFile.getPath());
		}
		if(!keyFile.exists()) {//default key file not found
			keyFile = chooseKeyFileWithUI(wavInFile.getPath());
		}
		
		int ans_INT = JOptionPane.showConfirmDialog (null, 
				"Do you wish to save decoded watermark as an image file?", 
				"Choose an option", JOptionPane.YES_NO_OPTION);
		if(ans_INT == JOptionPane.NO_OPTION) saveWatermark = false;
		else saveWatermark = true;
		
		initialize(wavInFile, keyFile, saveWatermark);
	}
	
	public ReadWatermark(String defaultWavFilePath, String defaultKeyFilePath, boolean saveWatermark) {
		this(new File(defaultWavFilePath), new File(defaultKeyFilePath), saveWatermark);
	}
	
	//For directory or multi-file processing (call this in an outside loop)
	public ReadWatermark(File wavInFile, File keyFile, boolean saveWatermark) {
		initialize(wavInFile, keyFile, saveWatermark);
	}
	
	private void initialize(File wavInFile, File keyFile, boolean saveWatermark) {
		this.wavInFile = wavInFile;
		watermark = new Watermark(true);
		while(true) {
			try {
				watermark.loadKey(keyFile);
				break;
			}
			catch(WatermarkException e) {
				int ans_INT = JOptionPane.showConfirmDialog (null, 
						"Press Yes to choose, press No to exit", 
						"Invalid key file! Please choose another.", JOptionPane.YES_NO_OPTION);
				if(ans_INT == JOptionPane.NO_OPTION) System.exit(0);
				keyFile = chooseKeyFileWithUI(wavInFile.getPath());
			}
		}//Now the key is loaded
		System.out.println("Watermark key loaded.");
		this.keyFile = keyFile;
		
		if(saveWatermark == true) {
			//Output path and name cannot be modified... maybe this can be optimized in later versions
			String imageOutFilePath = wavInFile.toString();
			imageOutFilePath = imageOutFilePath.substring(0, imageOutFilePath.length()-4);
			if(imageOutFilePath.substring(imageOutFilePath.length()-7, imageOutFilePath.length())
					== "-marked")
				imageOutFilePath = imageOutFilePath.substring(0, imageOutFilePath.length()-7);
			imageOutFile1 = new File(imageOutFilePath + "1.png");
			imageOutFile2 = new File(imageOutFilePath + "2.png");
			try {
				imageOutFile1.createNewFile();
				imageOutFile2.createNewFile();
			} catch (IOException e) {
				try {
					imageOutFile1 = new File(imageOutFilePath + "1_new.png");
					imageOutFile1.createNewFile();
					imageOutFile2 = new File(imageOutFilePath + "2_new.png");
					imageOutFile2.createNewFile();
				}
				catch(IOException e1) {
					System.out.println("Error creating png files!");
					saveWatermark = false;
				}
			}
		}
		this.saveWatermark = saveWatermark;
	}
	
	public int decode() {
		DefaultBoundedRangeModel brModel = new DefaultBoundedRangeModel();
		DoubleArray masks = new DoubleArray();
		int ret = decode(brModel, new SignalFlag(true), true, masks);
		mask1 = masks.getMask(1);
		mask1 = masks.getMask(2);
		return ret;
	}
	
	public int decode(DefaultBoundedRangeModel brModel, SignalFlag flag, boolean popNewFrame, DoubleArray decoded_masks) {
		int windowWidth, numOfChannels, maskIndex = 0, maskWidth;
		long numOfSamples;//This numOfSamples is WavFile.numOfSample if mono and WavFile.numOfFrames if multi-channel
		double[][] mask_temp1, mask_temp2;
		WavFile wavIn = null;
		STFTFrame stftFrame;
		
		boolean smooth = watermark.isModeSmooth();
		boolean robust = watermark.isModeRobust();
		
		if(!smooth)	windowWidth = 1024;//Do not modify! Relevant to Watermark.getMask22!
		else windowWidth = 4096;//Do not modify! Relevant to Watermark.getMask22!
		stftFrame = new STFTFrame(windowWidth);//Instantiate STFTFrame
		
		try {
			wavIn = new WavFile(wavInFile);
		}
		catch(IOException eIO) {
			JOptionPane.showMessageDialog(null, "Error reading file!\n"
					+ "Please put the default wav file in the default path!");
			return 1;//If so the program terminates
		}
		catch(WavFileException e) {
			JOptionPane.showMessageDialog(null, "Errors with the wav format!\n"
					+ "Please choose another. "+e);
			return 1;//If so the program terminates
		}
		System.out.println("Input stream is readied. Source = "+wavInFile.toString());
		
		numOfSamples = wavIn.getNumFrames();
		if(numOfSamples < windowWidth) {
			JOptionPane.showMessageDialog(null, "Input wav file is too short! Program terminates.");
			System.exit(-1);
		}
		numOfChannels = wavIn.getNumChannels();
		
		maskWidth = watermark.getWidth();
		if(robust) maskWidth /= 2;
		
		mask_temp1 = new double[maskWidth][windowWidth];
		mask_temp2 = new double[maskWidth][windowWidth];
		mask1 = new double[maskWidth][windowWidth];
		mask2 = new double[maskWidth][windowWidth];
		
		/**
		 * Explanation:
		 * 
		 * 1. The audio is loaded from file to bufferSTFT:
		 * 
		 *    bufferSTFT [0  0  ...  0  0  0  0  ...  0]
		 *               before...
		 *               after...
		 *               [0  0  ...  0 (a frame of audio)]
		 *               
		 * 2. The audio is windowed and copied to currentSTFT:
		 *    
		 *              (a frame of audio)
		 *                      *
		 *                 (windowFunc)
		 *   currentSTFT [windowed audio]
		 *   
		 * 3. currentSTFT is set as the amplitude spectrum of the windowed audio
		 * 
		 *   currentSTFT [amplitude spectrum]
		 *   
		 * 4. currentSTFT is written to a column of mask_temp
		 *    
		 * 5. Each time when a mask_temp is full, it is logged (Math.log())
		 *    and added to mask1 in case of odd-numbered frames 
		 *           or to mask2 in case of even-numbered frames
		 *    For the last frame which might be uncompleted, it is treated as if completed
		 * 	
		 * 6. Repeat from 1 on until reaching the end of the input audio
		 *  
		 * 7. Masks are normalized (ClearAverage-ed and scaled according to RMS)
		 *   
		 * 8. Masks are sent to Watermark via setMask and decoded and visualized (and saved if requested)
		 * 
		 */
		System.out.println("Reading watermark...");
		System.out.println("|__________________________________________________|");
		System.out.println("0%       20%       40%       60%       80%      100%");
		System.out.print("-");
		int progressMarkCount = 1, progressMark = (int)((numOfSamples+1) / 50);
		brModel.setMinimum(0);
		brModel.setMaximum(50);
		if(numOfChannels >= 1) {//Otherwise the audio file format is illegal
			double[][] bufferSTFT = new double[numOfChannels][3 * windowWidth / 2];
			double[] currentSTFT = new double[windowWidth];
			double[] windowFunc = new double[windowWidth];
			windowFunc = stftFrame.hanningWindow(windowWidth);
			
			for(long sampleCounter=windowWidth; sampleCounter<numOfSamples; sampleCounter+=windowWidth/2) {
				{//Read once and process two windows at a time
					//Step 1 starts
					if(sampleCounter == windowWidth) {//the first frame
						try {
							wavIn.readFrames(bufferSTFT, windowWidth/2, windowWidth);
							//    readFrames(buffer, offset_writing_buffer, write_how_many);
						}
						catch(IOException eIO) {
							JOptionPane.showMessageDialog(null, "Error reading wav file!");
							return 1;//If so the program terminates
						}
						catch(WavFileException eWav) {
							JOptionPane.showMessageDialog(null, "Unexpected wav file format!");
							return 1;//If so the program terminates
						}
					}
					else {//the frames after the first frame
						try {
							wavIn.readFrames(bufferSTFT, windowWidth, windowWidth/2);
							//    readFrames(buffer, offset_writing_buffer, write_how_many);
						}
						catch(IOException eIO) {
							JOptionPane.showMessageDialog(null, "Error reading wav file!");
							return 1;//If so the program terminates
						}
						catch(WavFileException eWav) {
							JOptionPane.showMessageDialog(null, "Unexpected wav file format!");
							return 1;//If so the program terminates
						}
					}
					//Step 1 ends
					
					for(int twice=0; twice<2; twice++) {
						for(int ch=0; ch<numOfChannels; ch++) {
							//Step 2 starts
							if(twice == 0) {//odd-numbered frames
								for(int i=0; i<windowWidth; i++) {
									currentSTFT[i] = bufferSTFT[ch][i]
											* windowFunc[i];
								}
							}
							else {//even-numbered frames
								for(int i=0; i<windowWidth; i++) {
									currentSTFT[i] = bufferSTFT[ch][i + windowWidth/4]
											* windowFunc[i];
								}
							}
							//Step 2 ends
							//Step 3 starts
							stftFrame.loadAudioFrame(currentSTFT);
							stftFrame.calcFFT();
							currentSTFT = stftFrame.getFrameAmp();
							//Step 3 ends
							//Step 4 starts
							if(twice == 0) {//odd-numbered frames
								if(ch == 0) {
									for(int index=0; index<windowWidth; index++) {
										mask_temp1[maskIndex][index] = currentSTFT[index];
									}
								}
								else {
									for(int index=0; index<windowWidth; index++) {
										mask_temp1[maskIndex][index] += currentSTFT[index];
									}
								}
							}
							else {//even-numbered frames
								if(ch == 0) {
									for(int index=0; index<windowWidth; index++) {
										mask_temp2[maskIndex][index] = currentSTFT[index];
									}
								}
								else {
									for(int index=0; index<windowWidth; index++) {
										mask_temp2[maskIndex][index] += currentSTFT[index];
									}
								}
							}
							//Step 4 ends
						}//end of channel loop
					}//end of the "twice" loop
					maskIndex++;//Two mask_temp-s are set within the "twice" loop
					//Step 5 starts
					if(maskIndex >= maskWidth) {
						maskIndex = 0;
						
						//Calculate the logarithms of the two mask_temp-s
						for(int i=0; i<maskWidth; i++) {
							for(int j=0; j<windowWidth; j++) {
								mask_temp1[i][j] = Math.log(mask_temp1[i][j] + 1e-6);
							}//Limit the minimum for logarithm calculation. P.s. 1e-6= -120dB
						}
						for(int i=0; i<maskWidth; i++) {
							for(int j=0; j<windowWidth; j++) {
								mask_temp2[i][j] = Math.log(mask_temp2[i][j] + 1e-6);
							}//Limit the minimum for logarithm calculation. P.s. 1e-6= -120dB
						}
						
						for(int i=0; i<maskWidth; i++) {
							for(int j=0; j<windowWidth; j++) {
								mask1[i][j] += mask_temp1[i][j];//add to result
								mask2[i][j] += mask_temp2[i][j];
							}
						}
					}
					//Step 5 ends
					
					for(int ch=0; ch<numOfChannels; ch++) {//Move bufferSTFT
						for(int i=0; i<windowWidth; i++) {
							bufferSTFT[ch][i] = bufferSTFT[ch][i + windowWidth/2];
						}
					}
					if(sampleCounter > progressMarkCount * progressMark) {
						progressMarkCount++;
						System.out.print(">");
						brModel.setValue(progressMarkCount);
					}
					if(!flag.isGoOn()) {
						System.out.print("\n**Aborted**\n");
						try {
							wavIn.close();
						} catch(IOException e) {
							JOptionPane.showInternalMessageDialog(null, "Error closing input file!");
						}
						return 2;
					}
				}//end of the "process" block
			}//end of sample counter loop. Watermark reading almost done...
			//Here step 6 is triggered if there are more audio data to process
			
			long remainingSamples = wavIn.getFramesRemaining();
			if(remainingSamples > 0) {//Num of remaining frames must < windowWidth/2
				//Step 1 starts for the last time
				try {
					wavIn.readFrames(bufferSTFT, windowWidth, (int)remainingSamples);
					//    readFrames(buffer, offset_writing_buffer, write_how_many);
				}
				catch(IOException eIO) {
					JOptionPane.showMessageDialog(null, "Error reading wav file!");
					return 1;//If so the program terminates
				}
				catch(WavFileException eWav) {
					JOptionPane.showMessageDialog(null, "Unexpected wav file format!");
					return 1;//If so the program terminates
				}
				//pad zeros for the rest
				for(int ch=0; ch<numOfChannels; ch++){
					for(int i=windowWidth+(int)remainingSamples; i<windowWidth*3/2; i++){
						bufferSTFT[ch][i] = 0;
					}
				}
				//Step 1 ends for the last time
				
				{//Process the last two windows
					for(int twice=0; twice<2; twice++) {
						for(int ch=0; ch<numOfChannels; ch++) {
							//Step 2 starts
							if(twice == 0) {//odd-numbered frames
								for(int i=0; i<windowWidth; i++) {
									currentSTFT[i] = bufferSTFT[ch][i]
											* windowFunc[i];
								}
							}
							else {//even-numbered frames
								for(int i=0; i<windowWidth; i++) {
									currentSTFT[i] = bufferSTFT[ch][i + windowWidth/4]
											* windowFunc[i];
								}
							}
							//Step 2 ends
							//Step 3 starts
							stftFrame.loadAudioFrame(currentSTFT);
							stftFrame.calcFFT();
							currentSTFT = stftFrame.getFrameAmp();
							//Step 3 ends
							//Step 4 starts
							if(twice == 0) {//odd-numbered frames
								if(ch == 0) {
									for(int index=0; index<windowWidth; index++) {
										mask_temp1[maskIndex][index] = currentSTFT[index];
									}
								}
								else {
									for(int index=0; index<windowWidth; index++) {
										mask_temp1[maskIndex][index] += currentSTFT[index];
									}
								}
							}
							else {//even-numbered frames
								if(ch == 0) {
									for(int index=0; index<windowWidth; index++) {
										mask_temp2[maskIndex][index] = currentSTFT[index];
									}
								}
								else {
									for(int index=0; index<windowWidth; index++) {
										mask_temp2[maskIndex][index] += currentSTFT[index];
									}
								}
							}
							//Step 4 ends
						}//end of channel loop
					}//end of the "twice" loop
					maskIndex++;//Two mask_temp-s are set within the "twice" loop
					//Step 5 starts
					if(maskIndex >= maskWidth) {
						maskIndex = 0;
						//Calculate the logarithms of the two mask_temp-s
						for(int i=0; i<maskWidth; i++) {
							for(int j=0; j<windowWidth; j++) {
								mask_temp1[i][j] = Math.log(mask_temp1[i][j] + 1e-6);
							}//Limit the minimum for logarithm calculation. P.s. 1e-6= -120dB
						}
						for(int i=0; i<maskWidth; i++) {
							for(int j=0; j<windowWidth; j++) {
								mask_temp2[i][j] = Math.log(mask_temp2[i][j] + 1e-6);
							}//Limit the minimum for logarithm calculation. P.s. 1e-6= -120dB
						}
						
						for(int i=0; i<maskWidth; i++) {
							for(int j=0; j<windowWidth; j++) {
								mask1[i][j] += mask_temp1[i][j];
								mask2[i][j] += mask_temp2[i][j];
							}
						}
					}
					//Step 5 ends
				}//end of the processing block
				
			}//end of "if the number of remaining samples > 0"
		}//end of the data processing
		System.out.println(">|");
		brModel.setValue(brModel.getMaximum());
		try {
			wavIn.close();
		}
		catch(IOException e) {
			JOptionPane.showInternalMessageDialog(null, "Error closing input file!");
		}
		System.out.println("Preparing decoded watermark for display...");
		
		//Step 7 starts
		mask1 = clearAverage(mask1);
		mask2 = clearAverage(mask2);
		
		double rms1 = calcRMS(mask1);
		double rms2 = calcRMS(mask2);
		//System.out.println("rms1, 2 = "+rms1+", "+rms2);//DEBUG
		for(int i=0; i<maskWidth; i++) {
			for(int j=0; j<windowWidth; j++) {
				mask1[i][j] = -mask1[i][j]/(rms1);//normalize
				//if(mask1[i][j] > 1.0) mask1[i][j] = 1.0;
				//if(mask1[i][j] < 0.0) mask1[i][j] = 0.0;
				//mask1[i][j] = Math.atan(mask1[i][j]) / Math.PI + 0.5;//DEBUG
				mask2[i][j] = -mask2[i][j]/(rms2);//normalize
				//if(mask2[i][j] > 1.0) mask2[i][j] = 1.0;
				//if(mask2[i][j] < 0.0) mask2[i][j] = 0.0;
				//mask2[i][j] = Math.atan(mask2[i][j]) / Math.PI + 0.5;
			}
		}
		//Step 7 ends
		//Step 8 starts
		watermark.setMask(mask1);
		try {
			watermark.decrypt();
		}
		catch(WatermarkException e){
			JOptionPane.showMessageDialog(null, "Failed decrypting watermark1! Cause: "+e);
		}
		if(popNewFrame) {
			watermark.visualizeResult(popNewFrame);
			if(saveWatermark) {
				try {
					watermark.saveDecodedWatermark(imageOutFile1);
				}
				catch(WatermarkException e) {
					JOptionPane.showMessageDialog(null, "Failed saving first decoded watermark!");
				}
			}
		}
		decoded_masks.setMast(1, watermark.getMask());
		
		//JOptionPane.showMessageDialog(null, "Paused");//DEBUG
		watermark.setMask(mask2);
		try {
			watermark.decrypt();
		}
		catch(WatermarkException e){
			JOptionPane.showMessageDialog(null, "Failed decrypting watermark2! Cause: "+e);
		}
		if(popNewFrame) {
			watermark.visualizeResult(popNewFrame);
			System.out.println("Decoded watermark displayed.");
			
			if(saveWatermark) {
				try {
					watermark.saveDecodedWatermark(imageOutFile2);
					System.out.println("Decoded watermark saved as "+imageOutFile1.toString()+
							" and "+imageOutFile2.toString());
				}
				catch(WatermarkException e) {
					JOptionPane.showMessageDialog(null, "Failed saving second decoded watermark!");
				}
			}
		}
		decoded_masks.setMast(2, watermark.getMask());
		//Step 8 ends
		
		return 0;
	}
	
	public BufferedImage visualizeResult(int which, VisWmBlur blur, VisWmColor color) {
		BufferedImage image = null;
		if(which == 1) image = watermark.maskToImage(mask1, blur, color);
		if(which == 2) image = watermark.maskToImage(mask2, blur, color);
		return image;
	}
	
	private File chooseAudioInWithUI(String defaultPath) {
		File wavInFile = null;
		final JFileChooser chooser = new JFileChooser();
		if(wavInFile == null)
		{
			chooser.setCurrentDirectory(new java.io.File(defaultPath));
			chooser.setDialogTitle("Select the audio file *.wav to mark");
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			chooser.setAcceptAllFileFilterUsed(false);
			FileNameExtensionFilter filter = new FileNameExtensionFilter("Wav File","wav");
			chooser.setFileFilter(filter);//chooser.addChoosableFileFilter(filter);
			
		    if( chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION ){
		    	wavInFile = chooser.getSelectedFile();
		    }
			/*
			if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			  System.out.println("getCurrentDirectory(): " + chooser.getCurrentDirectory());
			  System.out.println("getSelectedFile() : " + chooser.getSelectedFile());
			} else {
			  System.out.println("No Selection ");
			}
			*/
			
//			if(wavInFile == null) {
//				int ans_INT = JOptionPane.showConfirmDialog (null, 
//						"Do you wish to exit the program?",
//						"Wav File Not Selected", JOptionPane.YES_NO_OPTION);
//				if(ans_INT == JOptionPane.YES_OPTION) System.exit(0);
//			}
		}
		return wavInFile;
	}
	
	private File chooseKeyFileWithUI(String defaultPath) {
		File keyFile = null;
		final JFileChooser chooser = new JFileChooser();
		if(keyFile == null)
		{
			chooser.setCurrentDirectory(new java.io.File(defaultPath));
			chooser.setDialogTitle("Select the key file *.bin to use");
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			chooser.setAcceptAllFileFilterUsed(false);
			FileNameExtensionFilter filter = new FileNameExtensionFilter("Key File","bin");
			chooser.setFileFilter(filter);//chooser.addChoosableFileFilter(filter);
			
		    if( chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION ){
		    	keyFile = chooser.getSelectedFile();
		    }
			
//			if(keyFile == null) {
//				int ans_INT = JOptionPane.showConfirmDialog (null, 
//						"Do you wish to exit the program?",
//						"Wav File Not Selected", JOptionPane.YES_NO_OPTION);
//				if(ans_INT == JOptionPane.YES_OPTION) System.exit(0);
//			}
		}
		return keyFile;
	}
	
	private double[][] clearAverage(double[][] arrayIn){
		int widthIn = arrayIn.length;
		int heightIn = arrayIn[0].length;
		double average = 0;
		
		for(int i=0; i<widthIn; i++) {
			for(int j=0; j<heightIn; j++) {
				average += arrayIn[i][j];
			}
		}
		average /= (widthIn * heightIn);
		for(int i=0; i<widthIn; i++) {
			for(int j=0; j<heightIn; j++) {
				arrayIn[i][j] -= average;
			}
		}
		
		return arrayIn;
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
	
	/*
	private void ShowRMS(double[][] inputArray, int id) {//DEBUG
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
		System.out.println("RMS = " + rms + ", id = " + id);
	}
	
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
	
	private void ShowRMS(ArrayList<Complex> inputArray, int id) {//DEBUG
		int n = inputArray.size();
		double rms = 0.0;
		Complex buf;
		for(int i=0; i<n; i++) {
			buf = inputArray.get(i);
			rms += buf.real()*buf.real() + buf.imag()*buf.imag();
		}
		rms = Math.sqrt(rms/n);
		System.out.println("RMS = " + rms + ", id = " + id);
	}
	*/
}
