package processing;

import java.io.File;

/*
 * Current progress:
 * 
 * FirstCodingCompleted = 
 * 		pre_posts.WatermarkException
 * 		         .WaveFile
 * 		         .WaveFileException
 * 		         .Watermark
 * 		processing.Complex
 * 		          .FFT
 * 		          .STFTException
 * 		          .STFTFrame
 *  		      .ApplyWatermark
 *   		      .ReadWatermark
 * 
 * ToBeOptimized = 
 * 		processing.ApplyWatermark and
 *      processing.ReadWatermark: Parallel processing
 * 
 * ToBeDefined = 
 *      pre_posts.FormatNormalize: should sample rate be stored in key files? Use 44100Hz or 48000Hz?
 * 		processing.GUI: Drag and drop, windows instead of the java console, 
 *                      sliders for brightness, contrast, colortemp and tint,
 *                      audio preview (streaming), may have to MODIFY the following:
 *                          pre_posts.Watermark
 *                          processing.ApplyWatermark
 * 
 * Nov.19,2017
 * 
 */
public class Testing_main {
	public static void main(String[] args) {
		File wavInFile = new File(".\\res\\TEST.wav");
		File watermarkImageFile = new File(".\\res\\watermark-Àï°Â×÷Æ·.bmp");
		File keyFile = new File(".\\res\\TEST.bin");
//		ApplyWatermark operation1 = new ApplyWatermark(wavInFile, wavInFile, watermarkImageFile);
//		keyFile = operation1.encode();//keyFile, 0.3);
		
		File markedWavFile = new File(".\\res\\TEST-marked.wav");
		ReadWatermark operation2 = new ReadWatermark(markedWavFile, keyFile, true);
		operation2.decode();
	}
}
