package processing;

public class STFTException extends Exception {
	
	private static final long serialVersionUID = -1221333715072497774L;
	private int i = 0;  
	public STFTException() {  
	};  
	
	public STFTException(String msg) {  
		super(msg);  
	}
	
	public STFTException(String msg, int x) {  
		super(msg);  
		i = x;  
	}
	
	public int val() {  
		return i;  
	} 
}
