package processing;

/**
Java Swing, 2nd Edition
By Marc Loy, Robert Eckstein, Dave Wood, James Elliott, Brian Cole
ISBN: 0-596-00408-7
Publisher: O'Reilly 

http://www.java2s.com/Code/Java/Swing-JFC/AdemonstrationoftheJProgressBarcomponent.htm
*/
// SwingProgressBarExample.java
// A demonstration of the JProgressBar component. The component tracks the
// progress of a for loop.
//

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

public class ProgressBar extends JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6906426553397907732L;

	JProgressBar pbar;

	static final int MY_MINIMUM = 0;

	static final int MY_MAXIMUM = 100;

	public ProgressBar() {
		pbar = new JProgressBar();
		pbar.setMinimum(MY_MINIMUM);
		pbar.setMaximum(MY_MAXIMUM);
		add(pbar);
	}

	public void updateBar(int newValue) {
		pbar.setValue(newValue);
	}

	public static void main(String args[]) {

		final ProgressBar it = new ProgressBar();

		JFrame frame = new JFrame("Progress Bar Example");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setContentPane(it);
		frame.pack();
		frame.setVisible(true);

		for (int i = MY_MINIMUM; i <= MY_MAXIMUM; i++) {
			final int percent = i;
			try {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						it.updateBar(percent);
					}
				});
				java.lang.Thread.sleep(100);
			} catch (InterruptedException e) {
				;
			}
		}
	}
}
