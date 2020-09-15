import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.Timer;


//class for the buttons which show user points
public class PointButton extends JButton {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int type;
	private PointButton button;
	
	public PointButton(int type) {
		this.type = type;
		this.button = this;
	}
	
	public void blink() {
		Icon defaultImage = this.getIcon();
		ImageIcon mFlash = new ImageIcon(GUI.getImagePath("AuxImages", "Military Flash.png"));	
		ImageIcon cFlash = new ImageIcon(GUI.getImagePath("AuxImages", "Culture Flash.png"));	
		ImageIcon eFlash = new ImageIcon(GUI.getImagePath("AuxImages", "Economy Flash.png"));	

		
		int step =  65;
		for(int i = 1; i <= 10; i++) {
			int j = i;
			Timer timer = new Timer(step * (i+1), new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if(j % 2 == 0) {//evens
						button.setIcon(defaultImage);
					}
					else {
						if(type == 0) {//if its a nation
							button.setIcon(mFlash);
						}
						else if(type == 1) {//if its a nation
							button.setIcon(cFlash);
						}
						else if(type == 2) {//if its a nation
							button.setIcon(eFlash);
						}
						
					}
					repaint();
				}
			});
			timer.setRepeats(false);
			timer.start();
		}
	}
	
	public void blinkNeg() {
		Icon defaultImage = this.getIcon();
		ImageIcon mFlash = new ImageIcon(GUI.getImagePath("AuxImages", "Military Flash Neg.png"));	
		ImageIcon cFlash = new ImageIcon(GUI.getImagePath("AuxImages", "Culture Flash Neg.png"));	
		ImageIcon eFlash = new ImageIcon(GUI.getImagePath("AuxImages", "Economy Flash Neg.png"));	

		
		int step =  65;
		for(int i = 1; i <= 10; i++) {
			int j = i;
			Timer timer = new Timer(step * (i+1), new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if(j % 2 == 0) {//evens
						button.setIcon(defaultImage);
					}
					else {
						if(type == 0) {//if its a nation
							button.setIcon(mFlash);
						}
						else if(type == 1) {//if its a nation
							button.setIcon(cFlash);
						}
						else if(type == 2) {//if its a nation
							button.setIcon(eFlash);
						}
						
					}
					repaint();
				}
			});
			timer.setRepeats(false);
			timer.start();
		}
	}
	
}
