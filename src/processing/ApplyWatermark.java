package processing;

/**
 * Exterior class, engaged in UI
 */

import pre_posts.*;
import java.io.*;
import java.util.ArrayList;

import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import gui.SignalFlag;

public class ApplyWatermark {
	
	private Watermark watermark;
	private File wavInFile = null, wavOutFile = null;
	
	public ApplyWatermark() {//Use GUI to obtain the wav and watermark files
		/*
		 * From: https://stackoverflow.com/questions/10621687/how-to-get-full-path-directory-from-file-chooser
		 * http://www.java2s.com/Code/Java/Swing-JFC/SelectadirectorywithaJFileChooser.htm
		 * Answered by https://stackoverflow.com/users/381161/malcolm-smith at May 16 '12 at 15:27, 2017
		 */
		wavInFile = chooseAudioInWithUI(".");
		watermark = chooseWatermarkImageWithUI(wavInFile.getPath());
		prepareOutputFile(wavInFile, wavInFile);
	}
	
	//For directory or multi-file processing (call this in an outside loop)
	public ApplyWatermark(File wavInFile, File wavOutFile, Watermark watermark) {
		this.watermark = watermark;
		this.wavInFile = wavInFile;
		prepareOutputFile(wavInFile, wavOutFile);
	}
	
	//For directory or multi-file processing (call this in an outside loop)
	public ApplyWatermark(File wavInFile, File wavOutFile, File watermarkImageFile) {
		try {
			watermark = new Watermark(watermarkImageFile);
		}
		catch(IOException eIO) {
			JOptionPane.showMessageDialog(null, "Error reading watermark image file!\n"
					+ "Please choose another.");
			watermark = chooseWatermarkImageWithUI(wavInFile.getPath());
		}
		this.wavInFile = wavInFile;
		prepareOutputFile(wavInFile, wavOutFile);
	}
	
	private void prepareOutputFile(File wavInFile, File wavOutFile) {
		//Output path and name cannot be modified... maybe this can be optimized in later versions
		String wavOutFilePath = wavOutFile.getParent() + "\\" + wavInFile.getName();
		if(wavInFile == wavOutFile) {
			wavOutFilePath = wavOutFilePath.substring(0, wavOutFilePath.length()-4);
			wavOutFilePath += "-marked.wav";
		}
		wavOutFile = new File(wavOutFilePath);
		try {
			wavOutFile.createNewFile();
			this.wavOutFile = wavOutFile;
		} catch (IOException e) {
			System.out.println("Error creating output wav file!");
			JOptionPane.showMessageDialog(null, "Error", "Error creating output file: " 
			+ wavOutFile.getAbsolutePath(), JOptionPane.YES_OPTION);
		}
		System.out.println("Wav input and watermark image loaded. Output file readied");
	}
	
	private void genNewKeyWithUI(File keyFile) {//Do not use in the package GUI
		//Only use this method when applying watermark, otherwise generating a new key is not possible
		// due to invalid watermark dimension (initialized as -1 x -1)
		boolean smooth, robust;
		int folds = 0;
		double offset = 0.0;
		folds = Integer.parseInt(JOptionPane.showInputDialog(
				"Please set the encryption folding level", "2"));
		offset = Double.parseDouble(JOptionPane.showInputDialog(
				"Please set the watermark position offset", "-0.75"));
		int ans_INT = JOptionPane.showConfirmDialog (null, 
				"Enable smooth watermarking? This may take longer.",
				"Choose an option", JOptionPane.YES_NO_OPTION);
		if(ans_INT == JOptionPane.YES_OPTION) smooth = true; else smooth = false;
		ans_INT = JOptionPane.showConfirmDialog (null, 
				"Enable robust watermarking? This may decrease watermark resolution.",
				"Choose an option", JOptionPane.YES_NO_OPTION);
		if(ans_INT == JOptionPane.YES_OPTION) robust = true; else robust = false;
		
		genNewKeyWithKeySettings(keyFile, new KeySettings(offset, watermark.getWidth(), watermark.getHeight(), smooth, robust), folds);
	}
	
	public void genNewKeyWithKeySettings(File keyFile, KeySettings keySettings, int folds) {
		watermark.setFold(folds);
		watermark.setOffset(keySettings.getOffset());
		watermark.setMode(keySettings.isSmooth(), keySettings.isRobust());
		
		try {
			watermark.keyGen();
		}
		catch(WatermarkException e) {
			JOptionPane.showMessageDialog(null, "Error! Inappropriate watermark dimensions!");
			return;
		}
		
		try {
			watermark.saveKey(keyFile);
		}
		catch(WatermarkException e) {
			JOptionPane.showMessageDialog(null, "Error saving key!");
			return;
		}
	}
	
	private File loadExistingKeyWithUI(File keyFile) {//Do not use in the package GUI
		boolean notUsingDefaultKey = false;
		int ans_INT;
		if(keyFile.exists()) {
			ans_INT = JOptionPane.showConfirmDialog (null, 
					"Use the default key?", 
					"Default key found", JOptionPane.YES_NO_OPTION);
			if(ans_INT == JOptionPane.NO_OPTION)//chose not to use default key
				notUsingDefaultKey = true;
		}
		if(!keyFile.exists() || notUsingDefaultKey) {//If no default key file is found or not used
			keyFile = null;
			JFileChooser chooser = new JFileChooser();
			chooser.setCurrentDirectory(new java.io.File(wavInFile.getPath()));
			chooser.setDialogTitle("Select the key file to encrypt with");
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			chooser.setAcceptAllFileFilterUsed(false);
			FileNameExtensionFilter filter = new FileNameExtensionFilter("Key File","bin");
			chooser.setFileFilter(filter);//chooser.addChoosableFileFilter(filter);
			while(keyFile == null)
			{
				if( chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION ){
					keyFile = chooser.getSelectedFile();
			    }
				if(keyFile == null) {//keyFile not chosen
					ans_INT = JOptionPane.showConfirmDialog (null, 
							"If no, choose a key file or generate new key to continue", 
							"Do you wish to exit the program?", JOptionPane.YES_NO_OPTION);
					if(ans_INT == JOptionPane.YES_OPTION) {
						System.exit(0);
					}
					else {//if not to exit
						ans_INT = JOptionPane.showConfirmDialog (null, 
								"If no, choose a key file to continue", 
								"Do you wish generate a new key?", JOptionPane.YES_NO_OPTION);
						if(ans_INT == JOptionPane.YES_OPTION) {
							genNewKeyWithUI(keyFile);
							break;
						}
						else continue;//Not to generate a new key
					}
				}
			}
		}
		return keyFile;
	}
	
	public File encode() {
		
		String keyFilePath = wavInFile.toString();
		keyFilePath = keyFilePath.substring(0, keyFilePath.length()-4);
		keyFilePath += ".bin";
		File keyFile = new File(keyFilePath);
		double amp = 0.0;//Relative amplitude/intensity/energy of the watermark
		amp = Double.parseDouble(JOptionPane.showInputDialog(null, 
				"Please specify watermark amplitude coefficient, ranging from 0 to 1.", "0.3"));
		
		//Choose whether to load a key or generate a new key
		int ans_INT = JOptionPane.showConfirmDialog (null, 
				"Press Yes to generate new key, No for openning key file.",
				"Choose an option", JOptionPane.YES_NO_OPTION);
		if(ans_INT == JOptionPane.YES_OPTION) { genNewKeyWithUI(keyFile);}
		else { keyFile = loadExistingKeyWithUI(keyFile);}
		//Now keyFile as a File is readied
		try { watermark.loadKey(keyFile);} 
		catch(WatermarkException we) {
			JOptionPane.showMessageDialog(null, "Encode(): Failed loading keyFile!\n" + 
					"Detail: " + we.getMessage());
		}
		System.out.println("The key for encryprtion is "+keyFile.toString());
		
		if(amp > 1.0) amp = 1.0;//if amp is too great, audio may sound broken
		if(amp < 0.01) amp = 0.01;//if amp is too small, watermark is hardly detectable
		
		int resEncode;
		// resEncode: 0=success, 1=input error, 2=output error/aborted, 3=watermark error, 4=stft error
		resEncode = encode(amp);
		if(resEncode == 0) return keyFile;
		else return null;
	}
	
	public int encode(double amp) {
		DefaultBoundedRangeModel brModel = new DefaultBoundedRangeModel();
		return encode(amp, brModel, new SignalFlag(true));
	}
	
	public int encode(double amp, DefaultBoundedRangeModel brModel, SignalFlag flag) {
		int windowWidth, numOfChannels, maskIndex = 0, maskWidth;
		long numOfSamples;//This numOfSamples is WavFile.numOfSample if mono and WavFile.numOfFrames if multi-channel
		double[][] mask;
		WavFile wavIn = null;
		WavFile wavOut = null;
		STFTFrame stftFrame;
		final double clipSaftyCorrect = 0.0;//Prevent clipping DEBUG
		
		if(!watermark.isModeSmooth())	windowWidth = 1024;//Do not modify! Relevant to Watermark.getMask22!
		else {
			windowWidth = 4096;//Do not modify! Relevant to Watermark.getMask22!
			amp = amp * (1.8 - 0.8*amp);//DEBUG
			System.out.println("Actual watermark amp is increased to "+amp+
					" because smooth mode requires higher amp for the watermark to be detectable.");
		}
		stftFrame = new STFTFrame(2 * windowWidth);//Instantiate STFTFrame
		
		try { wavIn = new WavFile(wavInFile);}
		catch(IOException eIO) {
			JOptionPane.showMessageDialog(null, "Error reading file!\n"
					+ "Please put the default wav file in the default path!");
			return 1;//If so the program terminates
		}
		catch(WavFileException e) {
			JOptionPane.showMessageDialog(null, "Errors in wav format!\n"
					+ "Please choose another.");
			return 1;//If so the program terminates
		}
		
		numOfSamples = wavIn.getNumFrames();
		if(numOfSamples < windowWidth) {
			JOptionPane.showMessageDialog(null, "Encode(..): Input wav file" + 
					wavInFile.getAbsolutePath() + " is too short!");
		}
		numOfChannels = wavIn.getNumChannels();
		
		try {
			wavOut = new WavFile(wavOutFile, numOfChannels, numOfSamples, 
					wavIn.getValidBits(), wavIn.getSampleRate());
		}
		catch(IOException eIO) {
			JOptionPane.showMessageDialog(null, "Error creating file!\n");
			return 2;//If so the program terminates
		}
		catch(WavFileException e) {
			JOptionPane.showMessageDialog(null, "Errors with the wav format!\n"
					+ "Failed creating output.");
			return 2;//If so the program terminates
		}
		
		try { if(!watermark.isEncrypted()) watermark.encrypt();}
		catch(WatermarkException e) {
			JOptionPane.showMessageDialog(null, "Failed encrypting watermark!");
			return 3;//If so the program terminates
		}
		
		mask = watermark.getMask2(amp, 2*windowWidth);
		maskWidth = mask.length;
		
		System.out.println("Wav input stream and watermark key readied.");
		
		/**
		 * Explanation:
		 * 
		 * 1. The audio is loaded from file to bufferSTFT:
		 * 
		 *    bufferSTFT [0  0  0  ...  0  0  0  0  ...  0]
		 *               before...
		 *               after...
		 *               [0  0  0  ...  0 (a frame of audio)]
		 *               
		 * 2. The audio is windowed and copied to the central part of currentSTFT:
		 *    
		 *                         (a frame of audio)
		 *                                 *
		 *                            (windowFunc)
		 *   currentSTFT [0 0 ... 0 (windowed audio) 0 0 ... 0]
		 *   
		 * 3. currentSTFT is watermarked by stftFrame and thus modified:
		 * 
		 *   currentSTFT [0 ? ... ?  (marked audio)  ? ? ... 0] with both ends smoothed, "?"=spectrum leaks
		 *   
		 * 4. currentSTFT is added to resultSTFT:
		 * 
		 *   resultSTFT [0  0  0  ...  0  0  0  0  ...  0]
		 *              += currentSTFT;
		 *              
		 *   After this, clear currentSTFT (to zeros)
		 *              
		 * 5. For the first and second frame, due to frame overlapping,
		 *    the start of resultSTFT matches not yet the start of audio input
		 *    thus no data is sent to the output stream;
		 *    
		 *    For the frames after, the first windowWidth/2 size of data 
		 *    in resultSTFT would be sent to the output stream.
		 *    
		 *   resultSTFT [r  r  r  ...  r  r  r  r  ... ...  r]
		 *               [sent to oStrm] (windowWidth/2 of 2*windowWidth)
		 *    
		 * 6. The data in resultSTFT is moved forward by windowWidth/2
		 *   after this moving, the data of resultSTFT before moving are:
		 *   d = dismissed; r = moved; 0 = padded with zeros
		 *   resultSTFT [d  d  d  ...  d  r  r  r  ... ...  r] (0  0 ... 0)
		 *   new resultSTFT              [r  r  r  ... ...  r   0  0 ... 0]
		 *   
		 * 7. Repeat from 1 on until reaching the end of the input audio
		 * 
		 */
		
		if(numOfChannels >= 1) {
			double[][] bufferSTFT = new double[numOfChannels][2 * windowWidth];
			double[] currentSTFT = new double[2 * windowWidth];
			double[][] resultSTFT = new double[numOfChannels][2 * windowWidth];
			double[] windowFunc;// = new double[windowWidth];
			double[] smoothEnds = new double[2 * windowWidth];// = hanning rising half + rectangular + hanning falling half
			ArrayList<Complex> currentFRAMEComp = new ArrayList<>();//Complex[2 * windowWidth];
			windowFunc = stftFrame.hanningWindow(windowWidth);
			for(int i=1; i<windowWidth/2; i++) {
				smoothEnds[2*windowWidth - i - 1] = smoothEnds[i] = windowFunc[i];
			}
			for(int i=windowWidth/2; i<3*windowWidth/2; i++) {
				smoothEnds[i] = 1.0;
			}
			
			System.out.println("Applying watermark...");
			System.out.println("|__________________________________________________|");
			System.out.println("0%       20%       40%       60%       80%      100%");
			System.out.print("-");
			int progressMarkCount = 1, progressMark = (int)((numOfSamples+1) / 50);
			brModel.setMinimum(0);
			brModel.setMaximum(50);
			for(long sampleCounter=windowWidth; sampleCounter<numOfSamples; sampleCounter += windowWidth) {
				{//Read once and process two windows at a time
					//Step 1 starts
					try {
						wavIn.readFrames(bufferSTFT, windowWidth, windowWidth);
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
					//Step 1 ends
					
					for(int twice=0; twice<2; twice++) {
						for(int ch=0; ch<numOfChannels; ch++) {
							//Step 2 starts
							if(twice == 0) {//odd-numbered frames
								for(int i=0; i<windowWidth; i++) {
									currentSTFT[i + windowWidth/2] = bufferSTFT[ch][i + windowWidth/2]
											* windowFunc[i];
								}
							}
							else {//even-numbered frames
								for(int i=0; i<windowWidth; i++) {
									currentSTFT[i + windowWidth/2] = bufferSTFT[ch][i + windowWidth]
											* windowFunc[i];
								}
							}
							//Step 2 ends
							//Step 3 starts
							stftFrame.loadAudioFrame(currentSTFT);
							stftFrame.calcFFT();
							currentFRAMEComp = stftFrame.getFrameComplex();
							
							for(int i=0; i<2*windowWidth; i++) {
								currentFRAMEComp.set(i, currentFRAMEComp.get(i).times((1.0-clipSaftyCorrect*amp)*mask[maskIndex][i]));
							}
							
							try {
								stftFrame.setFrameComplex(currentFRAMEComp);
							}
							catch(STFTException e) {
								JOptionPane.showMessageDialog(null, "Error setting Complex frame!");
								return 4;
							}
							stftFrame.calcIFFT();
							currentSTFT = stftFrame.getAudioFrame();
							//Step 3 ends
							//Step 4 starts
							for(int i=0; i<3*windowWidth/2; i++) {//Move
								resultSTFT[ch][i] = resultSTFT[ch][i + windowWidth/2];
							}
							for(int i=3*windowWidth/2; i<2*windowWidth; i++) {//Pad zeros
								resultSTFT[ch][i] = 0.0;
							}
							
							//See "Instantiate STFTFrame" for stftFrame definition
							for(int i=0; i<2*windowWidth; i++) {
								resultSTFT[ch][i] += currentSTFT[i] * smoothEnds[i];
							}
							//Step 4 ends
							currentSTFT = new double[2 * windowWidth];
						}//end of channel loop
						
						//No sending to output stream for the first and second frames
						if(sampleCounter > windowWidth) {
							//Step 5 starts
							try {
								wavOut.writeFrames(resultSTFT, 0, windowWidth/2);
								//     writeFrames(buffer, offset_writing_buffer, write_how_many);
							}
							catch(IOException eIO) {
								JOptionPane.showMessageDialog(null, "Error writing wav file!");
								return 2;//If so the program terminates
							}
							catch(WavFileException eWav) {
								JOptionPane.showMessageDialog(null, "Error writing wav! Unexpected format.");
								return 2;//If so the program terminates
							}
							//Step 5 ends
						}
						if(twice > 0 || true) maskIndex++;//Overlapping DEBUG
						if(maskIndex >= maskWidth) maskIndex = 0;
					}//end of the "twice" loop
					//Step 6 starts
					for(int ch=0; ch<numOfChannels; ch++) {//Move bufferSTFT
						for(int i=0; i<windowWidth; i++) {
							bufferSTFT[ch][i] = bufferSTFT[ch][i + windowWidth];
						}
					}
					//Step 6 ends
				}//end of the "process" block
				if(sampleCounter > progressMarkCount * progressMark) {
					progressMarkCount++;
					brModel.setValue(progressMarkCount);
					System.out.print(">");
				}
				if(!flag.isGoOn()) { //if the go-on flag is false, abort and delete the output file.
					System.out.print("\n**Aborted**\n");
					try {
						wavIn.close();
					} catch(IOException e) {
						JOptionPane.showInternalMessageDialog(null, "Error closing input file!");
					}
					try {
						wavOut.close();
						wavOutFile.delete();
					}catch(IOException e) {
						JOptionPane.showInternalMessageDialog(null, "Error closing/deleting output file!");
					}
					return 2;
				}
			}//end of sample counter loop. Watermarking almost done...
			//Step 7 is triggered if there are more data to process
			
			long remainingSamples = wavIn.getFramesRemaining();
			if(remainingSamples > 0) {//Num of remaining frames must < windowWidth
				//Step 1 starts
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
					for(int i=windowWidth+(int)remainingSamples; i<windowWidth*2; i++){
						bufferSTFT[ch][i] = 0;
					}
				}
				//Step 1 ends
				
				{//Process the last two windows
					for(int twice=0; twice<2; twice++) {
						for(int ch=0; ch<numOfChannels; ch++) {
							//Step 2 starts
							if(twice == 0) {//odd-numbered frames
								for(int i=0; i<windowWidth; i++) {
									currentSTFT[i + windowWidth/2] = bufferSTFT[ch][i + windowWidth/2]
											* windowFunc[i];
								}
							}
							else {
								for(int i=0; i<windowWidth; i++) {
									currentSTFT[i + windowWidth/2] = bufferSTFT[ch][i + windowWidth]
											* windowFunc[i];
								}
							}
							//Step 2 ends
							//Step 3 starts
							stftFrame.loadAudioFrame(currentSTFT);
							stftFrame.calcFFT();
							currentFRAMEComp = stftFrame.getFrameComplex();
							if(maskIndex >= maskWidth) maskIndex = 0;
							for(int i=0; i<windowWidth; i++) {
								currentFRAMEComp.set(i, currentFRAMEComp.get(i).times((1.0-clipSaftyCorrect*amp)*mask[maskIndex][i]));
							}
							if(twice > 0 || true) maskIndex++;//Overlapping DEBUG
							
							try {
								stftFrame.setFrameComplex(currentFRAMEComp);
							}
							catch(STFTException e) {
								JOptionPane.showMessageDialog(null, "Error setting Complex frame!");
								return 4;
							}
							stftFrame.calcIFFT();
							currentSTFT = stftFrame.getAudioFrame();
							//Step 3 ends
							//Step 4 (adding currentSTFT to resultSTFT) is unnecessary for the last frame
							//  because the latter half of the last currentSTFT contains 
							//  only process derived data and extends the original audio length
							//Step 6 starts: the last frame of audio is written on a different occasion
							for(int i=0; i<3*windowWidth/2; i++) {//Move
								resultSTFT[ch][i] = resultSTFT[ch][i + windowWidth/2];
							}
							for(int i=3*windowWidth/2; i<2*windowWidth; i++) {//Pad zeros
								resultSTFT[ch][i] = 0;
							}
							
							//See "Instantiate STFTFrame" for stftFrame definition
							for(int i=0; i<2*windowWidth; i++) {
								resultSTFT[ch][i] += currentSTFT[i] * smoothEnds[i];
							}
							currentSTFT = new double[2 * windowWidth];
							//Step 6 ends
							//Step 5 starts
							try {
								wavOut.writeFrames(resultSTFT, windowWidth/2);
								//     writeFrames(buffer, offset_writing_buffer, write_how_many);
							}
							catch(IOException eIO) {
								JOptionPane.showMessageDialog(null, "Error writing wav file!");
								return 2;//If so the program terminates
							}
							catch(WavFileException eWav) {
								JOptionPane.showMessageDialog(null, "Error writing wav! Unexpected format.");
								return 2;//If so the program terminates
							}
							//Step 5 ends
						}
					}//end of the "twice" loop
					
				}//end of the processing block
				try {
					wavOut.writeFrames(resultSTFT, windowWidth/2, (int)remainingSamples);
					//     writeFrames(buffer, offset_writing_buffer, write_how_many);
				}
				catch(IOException eIO) {
					JOptionPane.showMessageDialog(null, "Error writing wav file!");
					return 2;//If so the program terminates
				}
				catch(WavFileException eWav) {
					JOptionPane.showMessageDialog(null, "Error writing wav! Unexpected format.");
					return 2;//If so the program terminates
				}
			}//end of "if the number of remaining samples > 0"
			
		}//end of data processing
		System.out.println(">|");
		brModel.setValue(brModel.getMaximum());
		
		System.out.println("Watermarking done. Result saved as " + wavOutFile.toString());
		
		try {
			wavIn.close();
		}
		catch(IOException e) {
			JOptionPane.showInternalMessageDialog(null, "Error closing input file!");
		}
		
		try {
			wavOut.close();
		}
		catch(IOException e) {
			JOptionPane.showInternalMessageDialog(null, "Error closing output file!");
		}
		
		return 0;
	}
	
	private File chooseAudioInWithUI(String defaultPath) {//Do not use in the package GUI
		File wavInFile = null;
		final JFileChooser chooser = new JFileChooser();
		while(wavInFile == null)
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
			
			if(wavInFile == null) {
				int ans_INT = JOptionPane.showConfirmDialog (null, 
						"Do you wish to exit the program?",
						"Wav File Not Selected", JOptionPane.YES_NO_OPTION);
				if(ans_INT == JOptionPane.YES_OPTION) System.exit(0);
			}
		}//Now wavInFile is readied
		return wavInFile;
	}
	
	private Watermark chooseWatermarkImageWithUI(String defaultPath) {//Do not use in the package GUI
		File watermarkImageFile = null;
		Watermark watermark = null;
		final JFileChooser chooser = new JFileChooser();
		while(watermarkImageFile == null)
		{
			chooser.setCurrentDirectory(new java.io.File(defaultPath));
			chooser.setDialogTitle("Select the watermark image to mark with");
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			chooser.setAcceptAllFileFilterUsed(false);
			FileNameExtensionFilter filter = new FileNameExtensionFilter("Image File",
					"bmp","jpg","jpeg","png");//DEBUG, only these formats are considered
			chooser.setFileFilter(filter);//chooser.addChoosableFileFilter(filter);
			
			if( chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION ){
		    	watermarkImageFile = chooser.getSelectedFile();
		    }
			if(watermarkImageFile == null) {
				int ans_INT = JOptionPane.showConfirmDialog (null, 
						"Do you wish to exit the program?",
						"Watermark File Not Selected", JOptionPane.YES_NO_OPTION);
				if(ans_INT == JOptionPane.YES_OPTION) System.exit(0);
			}
			else {
				try {
					watermark = new Watermark(watermarkImageFile);
				}
				catch(IOException eIO) {
					JOptionPane.showMessageDialog(null, "Error reading watermark image file!\n"
							+ "Please choose another.");
					continue;
				}
			}
		}//Now watermark as a Watermark is readied
		return watermark;
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
