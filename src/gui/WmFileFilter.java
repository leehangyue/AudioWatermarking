package gui;

import java.io.File;
import java.io.FileFilter;

import javax.imageio.ImageIO;

public class WmFileFilter implements FileFilter {
	private final String[] imgFileExtensions = ImageIO.getReaderFileSuffixes();
	private final String[] keyFileExtensions = new String[] { "bin" };
	private final String[] wavFileExtensions = new String[] { "wav" };
	public enum FilterType {
			Image, Key, Wav
	};
	private FilterType ft;
	public void setFilterType(FilterType ft) {
		this.ft = ft;
	}
	@Override
	public boolean accept(File file) {
		//Reference: 
		//https://alvinalexander.com/source-code/java/java-filefilter-example-image-files
		String[] okFileExtensions;
		if(ft == FilterType.Image) okFileExtensions = imgFileExtensions;
		else if(ft == FilterType.Key) okFileExtensions = keyFileExtensions;
		else okFileExtensions = wavFileExtensions;
        for (String extension : okFileExtensions) {
            if (file.getName().toLowerCase().endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

}
