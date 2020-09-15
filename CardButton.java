import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.Timer;

public class CardButton extends JButton {


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String cardName;
	private String folder;
	
	private Card card;
	
	CardButton cardButton;
		
	public String getCardName() {
		return cardName;
	}

	public void setCardName(String cardName) {
		this.cardName = cardName;
	}
	
	public Card getCard() {
		return card;
	}

	//construct a new cardButton based on a card
	public CardButton(Card card, JLabel lblCardInspector) {
		
		this.card = card;
		this.cardName = card.getCardName();
		this.folder ="Era"+card.getEra();
		
		this.setBounds(10, 11, 62, 84);
		
		this.setContentAreaFilled(false);
		
		this.setMargin(new Insets(0,0,0,0));
		
		this.cardButton = this;
	
		this.setIcon(new ImageIcon(GUI.getImagePath("Era"+card.getEra(), card.getCardName()+"_small.png")));
		
		this.addMouseListener(new MouseListener() {

            @Override
            public void mouseReleased(MouseEvent e) { }

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseExited(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                lblCardInspector.setIcon(new ImageIcon(GUI.getImagePath("Era"+card.getEra(), card.getCardName()+"_med.png")));
            }

            @Override
            public void mouseClicked(MouseEvent e) {

            }
        });
    
	}
	
	//construct a button based on the name and folder
	public CardButton(String cardName, String folder, JLabel lblCardInspector) {
		this.cardName = cardName;
		this.folder = folder;

		this.setBounds(10, 11, 62, 84);

		this.setContentAreaFilled(false);

		this.setMargin(new Insets(2,2,2,2));
		
		this.cardButton = this;

		this.setIcon(new ImageIcon(GUI.getImagePath(folder, cardName+"_small.png")));

		this.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) { }

			@Override
			public void mousePressed(MouseEvent e) {}

			@Override
			public void mouseExited(MouseEvent e) {
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				lblCardInspector.setIcon(new ImageIcon(GUI.getImagePath(folder, cardName+"_med.png")));
			}

			@Override
			public void mouseClicked(MouseEvent e) {

			}
		});

	}
	
	//blink the border of the button
	public void blinkBorder() {
		if(folder.contains("Nation")) {
			GUI.playSound("NationDrums.wav");
		}
		else {
			GUI.playSound("Gong.wav");		
		}
		ImageIcon defaultImage = new ImageIcon(GUI.getImagePath(folder, cardName+"_small.png"));
		ImageIcon flash = new ImageIcon(GUI.getImagePath("AuxImages", "System Flash.png"));
		ImageIcon nationFlash = new ImageIcon(GUI.getImagePath("AuxImages", "Nation Flash.png"));
		
		int step =  65;
		
		for(int i = 1; i <= 10; i++) {
			int j = i;
			Timer timer = new Timer(step * (i+1), new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if(j % 2 == 0) {//evens
						cardButton.setIcon(defaultImage);
					}
					else {
						if(folder.contains("Nations")) {//if its a nation
							cardButton.setIcon(nationFlash);
						}
						else {//else use system flash
							cardButton.setIcon(flash);
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
