package pre_posts;

public class WatermarkException extends Exception{

	private static final long serialVersionUID = 3685507488583258741L;
	private int i = 0;  
	public WatermarkException() {  
	};  
	
	public WatermarkException(String msg) {  
		super(msg);  
	}
	
	public WatermarkException(String msg, int x) {  
		super(msg);  
		i = x;  
	}
	
	public int val() {  
		return i;  
		//if i = 0 as default, no option is provided to the user
		//if i = 1, the class that handles the exception should provide at least 2 options to the user
	} 
}
