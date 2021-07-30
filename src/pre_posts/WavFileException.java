package pre_posts;

public class WavFileException extends Exception {
	
	private static final long serialVersionUID = 7709521305731333128L;
	private int i = 0;  
	public WavFileException() {  
	};  
	
	public WavFileException(String msg) {  
		super(msg);  
	}
	
	public WavFileException(String msg, int x) {  
		super(msg);  
		i = x;  
	}
	
	public int val() {  
		return i;  
	} 
}
