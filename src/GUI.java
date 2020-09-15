import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.util.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.awt.FlowLayout;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.LineBorder;
import javax.swing.UIManager;
import java.awt.CardLayout;
import javax.swing.JLayeredPane;

public class GUI {

	private JFrame frame;
	
	private JPanel menuPanel;
	private JPanel gamePanel;
	private JPanel endPanel;
	
	private static boolean playSounds =  false;
	
	private static GUI window;
	
	//SETTINGS
	public String nation = "Random";
	public int aiDifficulty = 1;
	
	public static int chosenPid; //set after a pid is chosen so info can be passed back
	public static int borderSize = 5;
	
	private static GameBoard board;
	private JPanel playerHand;
	private JPanel playerBoards[];
	private JPanel eraDecks[];
	private JPanel[] eraDiscards;
	public JLabel cardInspector;
	private JLabel instructionLabel;
	private JLabel BackgroundImage;
	private JLabel middleBackgroundImage;
	private JPanel[] playerNationPanels;
	private JPanel[] playerScorePanels;
	private JLabel endImage;
	private JLabel playRating;
	
	private Clip soundtrack;
	
	//0 = draw, 1 = play, 2 = discard, 3 = wait
	public static int mode = 0; 
	public static boolean inDialog = false;

	@FunctionalInterface //used to define lambda functions relating to cards
	public interface CardPredicate {
	   boolean test(Card card);
	} 
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					window = new GUI();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	//calc image path simply
	public static URL getImagePath(String folder, String name) {
		return GUI.class.getResource("/images/" + folder + "/" + name);
	}

	public JFrame getFrame() {
		return frame;
	}
	
	public void endGame(boolean victory, double score) {
		
		playSounds = false;
		
		if(!victory) {
			endImage.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/DefeatScreen.png")));
		}
		else {
			endImage.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/VictoryScreen.png")));
		}
		
		endSoundtrack();
		playRating.setText("Play Rating: " + Math.round(score));
		
		menuPanel.setVisible(false);
		gamePanel.setVisible(false);
		endPanel.setVisible(true);
	}
		
public void aiTurns() throws InterruptedException {
		
		board.stack.push(//after cards are done resolving, discard to hand size
				new StackEffect(2, true,
				()->{
					BackgroundImage.setIcon(new ImageIcon(getImagePath("AuxImages", "Final_Board_0.png")));
					board.newTurn(0);
				}
				));
		
		board.stack.push(//after cards are done resolving, discard to hand size
				new StackEffect(2, true,
				()->{
					board.discardCardsAI(2);
					board.endTurn(2);
				}
				));

		
		board.stack.push(//after cards are done resolving, discard to hand size
				new StackEffect(2,
				()->{
					mode = 0;
					board.newTurn(2);
					board.drawCards(2);
					mode = 1;
					board.playCardAI(2);
				}
				));
		
		board.stack.push(//after cards are done resolving, discard to hand size
				new StackEffect(1, true,
				()->{
					board.currentPlayer = 2;
					BackgroundImage.setIcon(new ImageIcon(getImagePath("AuxImages", "Final_Board_2.png")));
				}
				));
		
		board.stack.push(//after cards are done resolving, discard to hand size
				new StackEffect(1, true,
				()->{
					mode = 2;
					board.discardCardsAI(1);
					board.endTurn(1);
				}
				));

		board.stack.push(//after cards are done resolving, discard to hand size
				new StackEffect(1,
				()->{
					board.currentPlayer = 1;
					BackgroundImage.setIcon(new ImageIcon(getImagePath("AuxImages", "Final_Board_1.png")));
					mode = 0;
					board.newTurn(1);
					board.drawCards(1);
					mode = 1;
					board.playCardAI(1);
				}
				));
		
		board.stack.push(//after cards are done resolving, discard to hand size
				new StackEffect(0,
				()->{
					board.endTurn(0);
				}
				));
		
		board.stack.push(//after cards are done resolving, discard to hand size
				new StackEffect(0,
				()->{
					frame.repaint();
					mode = 2;
					int n = board.playerHands.get(0).size() - board.calcMaxHandSize(0);
					if(n > 0) {
						board.discardCards(0, "Discard Step", n);
					}
				}
				));
		
		board.stack.push(
				new StackEffect(0,
				()->{
					mode = 1;
					board.chooseCard(0, 
							 "Play Step: Play a Card",
							 (Card card) -> {return true;},
							(int id, String chosenName, GameBoard board) -> {
								board.play(chosenName, id);
								return true;
							});
				}
				));
		
		board.stack.push(
				new StackEffect(0,
				()->{
					board.currentPlayer = 0;
					BackgroundImage.setIcon(new ImageIcon(getImagePath("AuxImages", "Final_Board_0.png")));
					board.newTurn(0);
					mode = 0;
					board.drawCards(0, "Draw Step", (int i)->{return i <= board.playerEra[0];}, 2);
				}
				));
		
		doNext();
	}
	
	public void reset() {
		//make sure Swing detects a change
		instructionLabel.setText("CHANGE");
		instructionLabel.setText("");
		
		playerHand.repaint();

		//reset hand/board displays
		playerHand.removeAll();
		for(int i = 0; i < 3; i++) {
			playerBoards[i].removeAll();
			playerScorePanels[i].removeAll();
			playerNationPanels[i].removeAll();
		}
		for(int era = 0; era < 5; era++) {
			eraDecks[era].removeAll();
			eraDiscards[era].removeAll();
		}
		
		middleBackgroundImage.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/middle_" + board.playerEra[0] + ".png")));

	}
	
	public void addScores() {
		
		String[] letters = {"m", "c", "e"};
		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 3; j++) {
				PointButton playerScoreLabel = new PointButton(j);
				playerScoreLabel.setIcon(new ImageIcon(getImagePath("AuxImages", board.scores[i][j]+letters[j]+".png")));
				playerScoreLabel.setContentAreaFilled(false);
				playerScoreLabel.setBorderPainted(false);
				playerScoreLabel.setPreferredSize(new Dimension(41, 41));
				playerScoreLabel.setFocusable(false);
				playerScoreLabel.setMargin(new Insets(0,0,0,0));
				if(board.triggeredPoints[i][j] == 1) {
					playerScoreLabel.blink();
					board.triggeredPoints[i][j] = 0;
				}
				if(board.triggeredPoints[i][j] == -1) {
					playerScoreLabel.blinkNeg();
					board.triggeredPoints[i][j] = 0;
				}
				playerScorePanels[i].add(playerScoreLabel);
			}
		}
	}
	
	public void addDecks() {
		//add decks to GUI
		for(int era = 1; era < 6; era++) {
			//create new image
			CardButton deck = new CardButton("cardback", "Era"+era, cardInspector);
			deck.setBorderPainted(false);
			deck.setFocusable(false);
			eraDecks[era-1].add(deck); //add deck to appropriate container
			
			if(board.EraDecks[era].hasDiscard()) {
				Card topDiscard = board.EraDecks[era].topDiscard();
				CardButton discard = new CardButton(topDiscard, cardInspector);
				discard.setBorderPainted(false);
				discard.setFocusable(false);
				eraDiscards[era-1].add(discard);
			}
		}
	}
	
	public void addHand() {
		//add cards to player hand
		//capture the human player (player 0)'s hand
		LinkedList<Card> personHand = board.playerHands.get(0);
		//generate a card image for each card in the player's hand
		for(int i = 0; i < personHand.size(); i++) {
			int j = i;
			CardButton newImage = new CardButton(personHand.get(i), cardInspector); //store the card name as text in the label
			newImage.setBorderPainted(false);
			newImage.setFocusable(false);
			playerHand.add(newImage);
		}
	}
	
	public void addBoards() {
		//add cards to player boards
		for(int i = 0; i <= 2; i++) {
			//get the board for this player
			LinkedList<Card> playerBoard = board.playerBoards.get(i);
			JPanel playerBoardPanel = playerBoards[i];
			//for each card in the player's board, create an image and add it to the correct container
			for(int j = 0; j < playerBoard.size(); j++) {
				CardButton newSystemImage = new CardButton(playerBoard.get(j), cardInspector);
				if(board.triggered.contains(playerBoard.get(j))) {//if this system was just triggered
					newSystemImage.blinkBorder();
					while(board.triggered.contains(playerBoard.get(j))) {
						board.triggered.remove(board.triggered.indexOf(playerBoard.get(j)));
					}
				}
				newSystemImage.setBorderPainted(false);
				playerBoardPanel.add(newSystemImage);
			}
		}
	}
	
	public void addNations() {
		//set up player nations
	    for(int pid = 0; pid < 3; pid++) {
	    	//set these cards up in the GUI
	    	playerNationPanels[pid].removeAll();
	    	CardButton nationCard = new CardButton(board.playerNations[pid].getCardName(), "Nations/Era" + board.playerNations[pid].getEra(), cardInspector);
	    	nationCard.setBounds(0, 0, 67, 66);
	    	nationCard.setMargin(new Insets(0,0,0,0));
	    	nationCard.setBorderPainted(false);
	    	nationCard.setFocusable(false);
	    	if(board.triggeredNations.contains(board.playerNations[pid])) {//if this system was just triggered
	    		nationCard.blinkBorder();
	    		while(board.triggeredNations.contains(board.playerNations[pid])) {
	    			board.triggeredNations.remove(board.triggeredNations.indexOf(board.playerNations[pid]));
	    		}
	    	}
	    	playerNationPanels[pid].add(nationCard);
	    }
	}
	
	//What should we do next to the GUI?
	public void doNext() {
		frame.repaint();
		if(!inDialog) {
			if(board.stack.isEmpty()) {//we've reached the bottom of the stack, start turns over again

				try {
					aiTurns();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			else {
				StackEffect nextEffect = board.stack.pop();
				if(nextEffect.getPid() != 0 && nextEffect.isDelay() && !board.testing) {//wait a beat before resolving things AI has put on the stack
					if(board.currentPlayer == 0) {
						updateGUI();
						instructionLabel.setText("Wait: The " + board.playerNations[nextEffect.getPid()].getCardName() + " Is Making Decisions");
					}
					else {
						updateGUI();
					}
					Timer timer = new Timer(2000, new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							nextEffect.getEffect().invoke();//do the top of the stack
							doNext();
						}
					});
					timer.setRepeats(false);
					timer.start();
				}
				else {
					nextEffect.getEffect().invoke();//do the top of the stack
					doNext();
				}
			}
		}
	}
	
	public void chooseSystem(GameBoard.cardFunction onClick, LinkedList<String> options, int pid, String message) {

		inDialog = true;
		
		reset();

		instructionLabel.setText(message);
		
		addNations();
		
		addScores();

		addDecks();

		addHand();

		//add cards to player boards
		for(int i = 0; i <= 2; i++) {
			//get the board for this player
			LinkedList<Card> playerBoard = board.playerBoards.get(i);
			JPanel playerBoardPanel = playerBoards[i];
			//for each card in the player's board, create an image and add it to the correct container
			for(int j = 0; j < playerBoard.size(); j++) {
				int k = j;
				CardButton newSystemImage = new CardButton(playerBoard.get(j), cardInspector);
				if(options.contains(playerBoard.get(j).getCardName())) {
					newSystemImage.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {		
							inDialog = false;
							onClick.execute(pid, playerBoard.get(k).getCardName(), board);
							doNext();

						}
					});

				}
				else {
					newSystemImage.setBorderPainted(false);
				}
				if(board.triggered.contains(playerBoard.get(j))) {//if this system was just triggered
					newSystemImage.blinkBorder();
					while(board.triggered.contains(playerBoard.get(j))) {//if this system was just triggered
						board.triggered.remove(board.triggered.indexOf(playerBoard.get(j)));
					}
				}
				playerBoardPanel.add(newSystemImage);
			}
		}
		
		frame.repaint();
		
		if(!board.alerts.isEmpty()) {//broadcast all alerts to the user
			board.alerts.pop().invoke();
			chooseSystem(onClick, options, pid, message);
		}
	}
	
	
	public void chooseScore(GameBoard.scoreFunction onClick, LinkedList<int[]> options, int pid, String message, int n) {
		inDialog = true;
		
		reset();
		
		instructionLabel.setText(message + " (" + n + ")");
		
		addNations();
		
		//add scores to panels
		String[] letters = {"m", "c", "e"};
		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 3; j++) {
				PointButton playerScoreLabel = new PointButton(j);
				playerScoreLabel.setIcon(new ImageIcon(getImagePath("AuxImages", board.scores[i][j]+letters[j]+".png")));
				playerScoreLabel.setContentAreaFilled(false);
				playerScoreLabel.setPreferredSize(new Dimension(41, 41));
				playerScoreLabel.setMargin(new Insets(0,0,0,0));
				if(board.triggeredPoints[i][j] == 1) {
					playerScoreLabel.blink();
					board.triggeredPoints[i][j] = 0;
				}
				if(board.triggeredPoints[i][j] == -1) {
					playerScoreLabel.blinkNeg();
					board.triggeredPoints[i][j] = 0;
				}
				int[] arr = {i, j};
				boolean found = false;
				for(int k = 0; k < options.size(); k++) {
					if(options.get(k)[0] == arr[0] && options.get(k)[1] == arr[1]) {
						found = true;
					}
				}
				if(found) {
					playerScoreLabel.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							inDialog = false;
							onClick.execute(pid, arr[0], arr[1], board);
							if(n > 1) {
								chooseScore(onClick, options, pid, message, n-1); //keep running until n is used up
							}
							else {
								doNext();
							}
						}
					});
				}
				else {
					playerScoreLabel.setFocusable(false);
					playerScoreLabel.setBorderPainted(false);
				}
				playerScorePanels[i].add(playerScoreLabel);
			}
		}

		addDecks();
		
		addHand();

		addBoards();
		
		frame.repaint();
		
		if(!board.alerts.isEmpty()) {//broadcast all alerts to the user
			board.alerts.pop().invoke();
			chooseScore(onClick, options, pid, message, n);
		}
		
	}
	
	public void chooseDeck(GameBoard.deckFunction onClick, LinkedList<Integer> options, int pid, String message) {

		inDialog = true;
		
		reset();

		instructionLabel.setText(message);
		
		addNations();
		
		addScores();

		//add decks to GUI
		for(int era = 1; era < 6; era++) {
			int i = era;
			//create new image
			CardButton deck = new CardButton("cardback", "Era"+era, cardInspector);
			if(options.contains(era)) {
				deck.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {

						inDialog = false;
						onClick.execute(pid, i, board);
						doNext();

					}
				});
			}
			else {
				deck.setBorderPainted(false);
			}
			eraDecks[era-1].add(deck); //add deck to appropriate container
			
			if(board.EraDecks[era].hasDiscard()) {
				Card topDiscard = board.EraDecks[era].topDiscard();
				CardButton discard = new CardButton(topDiscard, cardInspector);
				discard.setBorderPainted(false);
				discard.setFocusable(false);
				eraDiscards[era-1].add(discard);
			}
			
		}

		addHand();

		addBoards();
		
		frame.repaint();
		
		if(!board.alerts.isEmpty()) {//broadcast all alerts to the user
			board.alerts.pop().invoke();
			chooseDeck(onClick, options, pid, message);
		}
		
	}
	
	//same but repeat action n times
	public void chooseDeck(GameBoard.deckFunction onClick, LinkedList<Integer> options, int pid, String message, int n) {

		inDialog = true;
		
		reset();

		instructionLabel.setText(message + " (" + n + ")");
		
		addNations();
		
		addScores();

		//add decks to GUI
		for(int era = 1; era < 6; era++) {
			int i = era;
			//create new image
			CardButton deck = new CardButton("cardback", "Era"+era, cardInspector);
			if(options.contains(era)) {
				deck.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						
						inDialog = false;
						playSound("DrawCard.wav");
						onClick.execute(pid, i, board);
						if(n > 1) {
							chooseDeck(onClick, options, pid, message, n-1);
						}
						doNext();

					}
				});
			}
			else {
				deck.setBorderPainted(false);
			}
			eraDecks[era-1].add(deck); //add deck to appropriate container
			
			if(board.EraDecks[era].hasDiscard()) {
				Card topDiscard = board.EraDecks[era].topDiscard();
				CardButton discard = new CardButton(topDiscard, cardInspector);
				discard.setBorderPainted(false);
				discard.setFocusable(false);
				eraDiscards[era-1].add(discard);
			}
			
		}

		addHand();

		addBoards();
		
		frame.repaint();
		
		if(!board.alerts.isEmpty()) {//broadcast all alerts to the user
			board.alerts.pop().invoke();
			chooseDeck(onClick, options, pid, message, n);
		}
	}
	
	public void chooseDiscard(GameBoard.deckFunction onClick, LinkedList<Integer> options, int pid, String message, int n) {

		inDialog = true;
		
		reset();

		instructionLabel.setText(message + " (" + n + ")");
		
		addNations();
		
		addScores();

		boolean foundDiscard = false;
		//add decks to GUI
		for(int era = 1; era < 6; era++) {
			int i = era;
			//create new image
			CardButton deck = new CardButton("cardback", "Era"+era, cardInspector);
			deck.setBorderPainted(false);
			eraDecks[era-1].add(deck); //add deck to appropriate container
			if(board.EraDecks[era].hasDiscard()) {
				foundDiscard = true;
				Card topDiscard = board.EraDecks[era].topDiscard();
				CardButton discard = new CardButton(topDiscard, cardInspector);
				if(options.contains(era)) {
					
					discard.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							
							inDialog = false;
							onClick.execute(pid, i, board);
							if(n > 1) {
								chooseDiscard(onClick, options, pid, message, n-1);
							}
							else {
								doNext();	
							}
						}
					});
				}
				else {
					discard.setBorderPainted(false);
					discard.setFocusable(false);
				}
				eraDiscards[era-1].add(discard);
			}
		}
		if(!foundDiscard) {
			inDialog = false;
			doNext();
		}

		addHand();

		addBoards();
		
		frame.repaint();
		
		if(!board.alerts.isEmpty()) {//broadcast all alerts to the user
			board.alerts.pop().invoke();
			chooseDiscard(onClick, options, pid, message, n);
		}
	}
	
	public void chooseCard(GameBoard.cardFunction onClick, LinkedList<Card> options, int pid, String message) {

		GUI.playSound("Notification.wav");
		
		inDialog = true;
		
		reset();

		instructionLabel.setText(message);
		
		addNations();
		
		addScores();

		addDecks();

		//add cards to player hand
		//capture the human player (player 0)'s hand
		LinkedList<Card> personHand = board.playerHands.get(0);
		//generate a card image for each card in the player's hand
		for(int i = 0; i < personHand.size(); i++) {
			int j = i;
			CardButton newImage = new CardButton(personHand.get(i), cardInspector); //store the card name as text in the label
			if(options.contains(personHand.get(i))) {
				newImage.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						inDialog = false;
						onClick.execute(pid, personHand.get(j).getCardName(), board);
						if(!inDialog) { //do the action, check if done
							doNext();
						}
					}
				});
			}
			else {
				newImage.setBorderPainted(false);
				newImage.setFocusable(false);
			}
			playerHand.add(newImage);
		}

		addBoards();
		
		frame.repaint();
		
		if(!board.alerts.isEmpty()) {//broadcast all alerts to the user
			board.alerts.pop().invoke();
			chooseCard(onClick, options, pid, message);
		}
	}
	
	public void chooseCard(GameBoard.cardFunction onClick, LinkedList<String> options, int pid, String message, int n) {

		inDialog = true;
		
		reset();

		instructionLabel.setText(message + " (" + n + ")");
		
		addNations();
		
		addScores();

		addDecks();

		//add cards to player hand
		//capture the human player (player 0)'s hand
		LinkedList<Card> personHand = board.playerHands.get(0);
		//generate a card image for each card in the player's hand
		for(int i = 0; i < personHand.size(); i++) {
			int j = i;
			CardButton newImage = new CardButton(personHand.get(i), cardInspector); //store the card name as text in the label
			if(options.contains(personHand.get(i).getCardName())) {
				newImage.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						
						inDialog = false;
						onClick.execute(pid, personHand.get(j).getCardName(), board);
						if(n > 1) {
							board.chooseCard(pid, message, onClick, n-1);
						}
						if(!inDialog) { //do the action, check if done
							doNext();
						}
					}
				});
			}
			else {
				newImage.setBorderPainted(false);
				newImage.setFocusable(false);
			}
			playerHand.add(newImage);
		}

		addBoards();
		
		frame.repaint();
		
		if(!board.alerts.isEmpty()) {//broadcast all alerts to the user
			board.alerts.pop().invoke();
			chooseCard(onClick, options, pid, message, n);
		}
	}
	
	
	public void choosePlayer(GameBoard.playerFunction onClick, LinkedList<Integer> options, int pid, String message) {

		inDialog = true;
		
		reset();

		instructionLabel.setText(message);
		
		//set up player nations
		for(int i = 0; i < 3; i++) {
			int j = i;
			//set these cards up in the GUI
			playerNationPanels[i].removeAll();
	    	CardButton nationCard = new CardButton(board.playerNations[i].getCardName(), "Nations/Era" + board.playerNations[i].getEra(), cardInspector);
			nationCard.setBounds(0, 0, 67, 66);
			nationCard.setMargin(new Insets(0,0,0,0));
			if(options.contains(i)) {
				nationCard.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						
						inDialog = false;
						chosenPid = j;
						onClick.execute(pid, j, board);
						if(!inDialog) { //do the action, check if done
							doNext();
						}
					}
				});
			}
			else {
				nationCard.setBorderPainted(false);
				nationCard.setFocusable(false);
			}
			if(board.triggeredNations.contains(board.playerNations[pid])) {//if this system was just triggered
				nationCard.blinkBorder();
				while(board.triggeredNations.contains(board.playerNations[pid])) {//if this system was just triggered
					board.triggeredNations.remove(board.triggeredNations.indexOf(board.playerNations[pid]));
				}
			}
			playerNationPanels[i].add(nationCard);
		}
		
		addScores();

		addDecks();

		addHand();
		
		addBoards();
		
		frame.repaint();
		
		if(!board.alerts.isEmpty()) {//broadcast all alerts to the user
			board.alerts.pop().invoke();
			choosePlayer(onClick, options, pid, message);
		}
	}
	
	public void updateGUI() {
		
		reset();
		
		instructionLabel.setText("Wait for Your Turn");
				
		addNations();

		addScores();

		addDecks();

		addHand();

		addBoards();
		
		frame.repaint();
		
		playSounds = true;
		
		if(!board.alerts.isEmpty()) {//broadcast all alerts to the user
			board.alerts.pop().invoke();
			updateGUI();
		}
			
	}
	
	public void displayCard(Card card, JLabel cardInspector) {
		cardInspector.setIcon(new ImageIcon(getImagePath("Era"+card.getEra(), card.getCardName()+"_med.png")));
	}
	
	public static void playSound(String soundName) {
		if(playSounds && !board.testing) {
		AudioInputStream audioIn;
		try {
			audioIn = AudioSystem.getAudioInputStream(GUI.class.getResource("images/Extras/" + soundName));
			Clip clip = AudioSystem.getClip();
			clip.open(audioIn);
			clip.start();
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}
	}
	
	public void startSoundtrack() {
		AudioInputStream audioIn;
		try {
			audioIn = AudioSystem.getAudioInputStream(GUI.class.getResource("images/Extras/Soundtrack.wav"));
			soundtrack = AudioSystem.getClip();
			soundtrack.open(audioIn);
			soundtrack.loop(Clip.LOOP_CONTINUOUSLY);
			soundtrack.start();
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void endSoundtrack() {
		soundtrack.close();
	}

	/**
	 * Create the application.
	 */
	public GUI() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setResizable(false);
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(new Dimension(970, 715));
		frame.getContentPane().setLayout(new CardLayout(0, 0));
		
		JPanel menuPanel = new JPanel();
		frame.getContentPane().add(menuPanel, "name_1036669554767700");
		menuPanel.setLayout(null);
		this.menuPanel = menuPanel;
		
		JPanel tutorialPanel = new JPanel();
		frame.getContentPane().add(tutorialPanel, "name_17696760283400");
		tutorialPanel.setLayout(null);
		
		JLayeredPane layeredPane = new JLayeredPane();
		layeredPane.setBounds(0, 0, 1006, 691);
		tutorialPanel.add(layeredPane);
		
		
		JLabel tutorialGuy = new JLabel("");
		tutorialGuy.setBounds(-45, 214, 500, 500);
		layeredPane.add(tutorialGuy, new Integer(2));
		tutorialGuy.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/TutorialGuy.png")));
		
		JPanel tutorialScreen = new JPanel();
		tutorialScreen.setBounds(175, 110, 630, 419);
		layeredPane.add(tutorialScreen, new Integer(1));
		tutorialScreen.setLayout(new CardLayout(0, 0));
		
		JButton prevButton = new JButton("");
		prevButton.setBounds(346, 539, 121, 39);
		layeredPane.add(prevButton);
		prevButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				CardLayout layout = (CardLayout)tutorialScreen.getLayout();
				layout.previous(tutorialScreen);
			}
		});
		prevButton.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/PreviousButton.png")));
		
		JButton nextButton = new JButton("");
		nextButton.setBounds(490, 540, 121, 39);
		layeredPane.add(nextButton);
		nextButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				CardLayout layout = (CardLayout)tutorialScreen.getLayout();
				layout.next(tutorialScreen);
			}
		});
		nextButton.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/NextButton.png")));
		
		JButton returnToMenuButton = new JButton("");
		returnToMenuButton.setBounds(365, 600, 236, 59);
		layeredPane.add(returnToMenuButton);
		returnToMenuButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				menuPanel.setVisible(true);
				tutorialPanel.setVisible(false);
				
			}
		});	
		returnToMenuButton.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/ReturnToMenuButton.png")));
		
		JPanel tutPanel1 = new JPanel();
		tutorialScreen.add(tutPanel1, "name_18138826134600");
		
		JLabel lblNewLabel_1 = new JLabel("");
		tutPanel1.add(lblNewLabel_1);
		lblNewLabel_1.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/Tutorial1.png")));
		
		JPanel tutPanel2 = new JPanel();
		tutorialScreen.add(tutPanel2, "name_18229433164400");
		
		JLabel lblNewLabel_2 = new JLabel("");
		lblNewLabel_2.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/Tutorial2.png")));
		tutPanel2.add(lblNewLabel_2);
		
		JPanel tutPanel3 = new JPanel();
		tutorialScreen.add(tutPanel3, "name_18284714460300");
		
		JLabel lblNewLabel_3 = new JLabel("");
		lblNewLabel_3.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/Tutorial3.png")));
		tutPanel3.add(lblNewLabel_3);
		
		JPanel tutPanel4 = new JPanel();
		tutorialScreen.add(tutPanel4, "name_18339558218000");
		
		JLabel tutPanel4Label = new JLabel("");
		tutPanel4Label.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/Tutorial4.png")));
		tutPanel4.add(tutPanel4Label);
		
		JPanel tutPanel5 = new JPanel();
		tutorialScreen.add(tutPanel5, "name_18399453184600");
		
		JLabel lblNewLabel_4 = new JLabel("");
		lblNewLabel_4.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/Tutorial5.png")));
		tutPanel5.add(lblNewLabel_4);
		
		JPanel tutPanel6 = new JPanel();
		tutorialScreen.add(tutPanel6, "name_18452585227300");
		
		JLabel tutLabel6 = new JLabel("");
		tutLabel6.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/Tutorial6.png")));
		tutPanel6.add(tutLabel6);
		
		JPanel tutPanel7 = new JPanel();
		tutorialScreen.add(tutPanel7, "name_18626469086200");
		
		JLabel lblNewLabel_5 = new JLabel("");
		lblNewLabel_5.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/Tutorial7.png")));
		tutPanel7.add(lblNewLabel_5);
		
		JPanel tutPanel8 = new JPanel();
		tutorialScreen.add(tutPanel8, "name_18542742472000");
		
		JLabel lblNewLabel_6 = new JLabel("");
		lblNewLabel_6.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/Tutorial8.png")));
		tutPanel8.add(lblNewLabel_6);
		
		JLabel tutorialBackground = new JLabel("");
		tutorialBackground.setBounds(0, 0, 1006, 691);
		layeredPane.add(tutorialBackground);
		tutorialBackground.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/Tutorial Screen.png")));
		
		JPanel gamePanel = new JPanel();
		frame.getContentPane().add(gamePanel, "name_1036664049282000");
		gamePanel.setLayout(null);
		this.gamePanel = gamePanel;
		
		JButton tutorialButton = new JButton("");
		tutorialButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				menuPanel.setVisible(false);
				tutorialPanel.setVisible(true);
				
			}
		});
		tutorialButton.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/TutorialButton.png")));
		tutorialButton.setBounds(426, 421, 121, 39);
		menuPanel.add(tutorialButton);
		
		
		JLabel player0Name = new JLabel("Player 0");
		player0Name.setBounds(216, 32, 139, 28);
		gamePanel.add(player0Name);
		player0Name.setFont(new Font("Tahoma", Font.PLAIN, 15));
		player0Name.setForeground(Color.WHITE);
		
		JPanel player0ScorePanel = new JPanel();
		player0ScorePanel.setBounds(348, 19, 139, 51);
		gamePanel.add(player0ScorePanel);
		player0ScorePanel.setOpaque(false);
		player0ScorePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		JButton mScore0 = new PointButton(0);
		mScore0.setIcon(new ImageIcon( getClass().getResource("/images/AuxImages/0m.png") ));
		player0ScorePanel.add(mScore0);
		
		JButton cScore0 = new PointButton(1);
		cScore0.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/0c.png")));
		player0ScorePanel.add(cScore0);
		
		JButton eScore0 = new PointButton(2);
		eScore0.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/0e.png")));
		player0ScorePanel.add(eScore0);
		
		JLabel player1Name = new JLabel("Player 1");
		player1Name.setBounds(216, 82, 139, 28);
		gamePanel.add(player1Name);
		player1Name.setForeground(Color.WHITE);
		player1Name.setFont(new Font("Tahoma", Font.PLAIN, 15));
		
		JPanel player1ScorePanel = new JPanel();
		player1ScorePanel.setBounds(348, 72, 139, 51);
		gamePanel.add(player1ScorePanel);
		player1ScorePanel.setOpaque(false);
		player1ScorePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		JButton mScore1 = new PointButton(0);
		mScore1.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/0m.png")));
		player1ScorePanel.add(mScore1);
		
		JButton cScore1 = new PointButton(1);
		cScore1.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/0c.png")));
		player1ScorePanel.add(cScore1);
		
		JButton eScore1 = new PointButton(2);
		eScore1.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/0e.png")));
		player1ScorePanel.add(eScore1);
		
		JLabel player2Name = new JLabel("Player 2");
		player2Name.setBounds(216, 134, 139, 28);
		gamePanel.add(player2Name);
		player2Name.setForeground(Color.WHITE);
		player2Name.setFont(new Font("Tahoma", Font.PLAIN, 15));
		
		JPanel player2ScorePanel = new JPanel();
		player2ScorePanel.setBounds(348, 123, 139, 51);
		gamePanel.add(player2ScorePanel);
		player2ScorePanel.setOpaque(false);
		player2ScorePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		JButton mScore2 = new PointButton(0);
		mScore2.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/0m.png")));
		player2ScorePanel.add(mScore2);
		
		JButton cScore2 = new PointButton(1);
		cScore2.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/0c.png")));
		player2ScorePanel.add(cScore2);
		
		JButton eScore2 = new PointButton(2);
		eScore2.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/0e.png")));
		player2ScorePanel.add(eScore2);
		
		JLabel instructionLabel_1 = new JLabel("");
		instructionLabel_1.setBounds(682, 388, 266, 35);
		gamePanel.add(instructionLabel_1);
		instructionLabel_1.setHorizontalAlignment(SwingConstants.CENTER);
		instructionLabel_1.setText("Draw a Card");
		instructionLabel_1.setForeground(Color.WHITE);
		this.instructionLabel = instructionLabel_1;
		
		JLabel cardInspector_1 = new JLabel("");
		cardInspector_1.setBounds(682, 22, 266, 356);
		gamePanel.add(cardInspector_1);
		cardInspector_1.setIcon(new ImageIcon(GUI.class.getResource("/images/Era1/cardback_med.png")));
		cardInspector_1.setHorizontalAlignment(SwingConstants.CENTER);
		this.cardInspector = cardInspector_1;
		
		JScrollPane scrollPane_1 = new JScrollPane();
		scrollPane_1.setBounds(18, 22, 88, 428);
		gamePanel.add(scrollPane_1);
		scrollPane_1.setMaximumSize(new Dimension(92, 32767));
		scrollPane_1.setBorder(null);
		scrollPane_1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane_1.setHorizontalScrollBar(null);
		
		JPanel player1Board = new JPanelFixed();
		player1Board.setMaximumSize(new Dimension(92, 32767));
		player1Board.setBackground(new Color(54, 54, 54));
		scrollPane_1.setViewportView(player1Board);
		
		JScrollPane scrollPane_2 = new JScrollPane();
		scrollPane_2.setBounds(582, 22, 88, 428);
		gamePanel.add(scrollPane_2);
		scrollPane_2.setMaximumSize(new Dimension(92, 32767));
		scrollPane_2.setBorder(null);
		scrollPane_2.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane_2.setHorizontalScrollBar(null);
		
		JPanel player2Board = new JPanelFixed();
		player2Board.setMaximumSize(new Dimension(92, 32767));
		scrollPane_2.setViewportView(player2Board);
		player2Board.setBackground(new Color(54, 54, 54));
		
		JPanel player0Board = new JPanel();
		player0Board.setBounds(18, 463, 654, 100);
		gamePanel.add(player0Board);
		player0Board.setOpaque(false);
		
		JScrollPane scrollPane_3 = new JScrollPane();
		scrollPane_3.setBounds(18, 574, 654, 97);
		gamePanel.add(scrollPane_3);
		scrollPane_3.setBorder(null);
		scrollPane_3.setViewportBorder(null);
		scrollPane_3.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		
		JPanel playerHand_1 = new JPanel();
		playerHand_1.setBackground(new Color(54, 54, 54));
		scrollPane_3.setViewportView(playerHand_1);
		
			this.playerHand = playerHand_1;
		
		JPanel player0Nation = new JPanel();
		player0Nation.setBounds(685, 594, 76, 79);
		gamePanel.add(player0Nation);
		player0Nation.setBorder(null);
		player0Nation.setOpaque(false);
		
		JPanel player1Nation = new JPanel();
		player1Nation.setBounds(117, 16, 78, 79);
		gamePanel.add(player1Nation);
		player1Nation.setOpaque(false);
		
		JPanel player2Nation = new JPanel();
		player2Nation.setBounds(493, 15, 78, 79);
		gamePanel.add(player2Nation);
		player2Nation.setOpaque(false);
		
		JPanel eraDeck1 = new JPanel();
		eraDeck1.setBounds(124, 173, 68, 106);
		gamePanel.add(eraDeck1);
		eraDeck1.setOpaque(false);
		
		JPanel eraDeck2 = new JPanel();
		eraDeck2.setBounds(273, 173, 68, 106);
		gamePanel.add(eraDeck2);
		eraDeck2.setOpaque(false);
		
		JPanel eraDeck3 = new JPanel();
		eraDeck3.setBounds(425, 173, 68, 106);
		gamePanel.add(eraDeck3);
		eraDeck3.setOpaque(false);
		
		JPanel eraDeck4 = new JPanel();
		eraDeck4.setBounds(137, 310, 68, 106);
		gamePanel.add(eraDeck4);
		eraDeck4.setOpaque(false);
		
		JPanel eraDeck5 = new JPanel();
		eraDeck5.setBounds(280, 310, 68, 106);
		gamePanel.add(eraDeck5);
		eraDeck5.setOpaque(false);
		
		JPanel eraDiscard1 = new JPanel();
		eraDiscard1.setBounds(189, 184, 68, 106);
		gamePanel.add(eraDiscard1);
		eraDiscard1.setOpaque(false);
		
		JPanel eraDiscard2 = new JPanel();
		eraDiscard2.setBounds(338, 184, 68, 106);
		gamePanel.add(eraDiscard2);
		eraDiscard2.setOpaque(false);
		
		JPanel eraDiscard3 = new JPanel();
		eraDiscard3.setBounds(493, 184, 68, 106);
		gamePanel.add(eraDiscard3);
		eraDiscard3.setOpaque(false);
		
		JPanel eraDiscard4 = new JPanel();
		eraDiscard4.setBounds(202, 318, 68, 106);
		gamePanel.add(eraDiscard4);
		eraDiscard4.setOpaque(false);
		
		JPanel eraDiscard5 = new JPanel();
		eraDiscard5.setBounds(348, 318, 68, 106);
		gamePanel.add(eraDiscard5);
		eraDiscard5.setOpaque(false);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(688, 437, 254, 143);
		gamePanel.add(scrollPane);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
		JTextArea gameLog = new JTextArea();
		gameLog.setEditable(false);
		scrollPane.setViewportView(gameLog);
		gameLog.setForeground(Color.WHITE);
		gameLog.setBackground(Color.BLACK);
		gameLog.setText("Game log...");
				
		JLabel middleBackgroundImage_1 = new JLabel("");
		middleBackgroundImage_1.setBounds(118, 181, 451, 269);
		gamePanel.add(middleBackgroundImage_1);
		middleBackgroundImage_1.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/middle_1.png")));
		this.middleBackgroundImage = middleBackgroundImage_1;
		
		JLabel BackgroundImage_1 = new JLabel("");
		BackgroundImage_1.setBounds(0, 0, 1004, 691);
		gamePanel.add(BackgroundImage_1);
		BackgroundImage_1.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/Final_Board_0.png")));
		this.BackgroundImage = BackgroundImage_1;
		
		JPanel endPanel = new JPanel();
		frame.getContentPane().add(endPanel, "name_1039092513737200");
		endPanel.setLayout(null);
		this.endPanel = endPanel;
		
		JButton btnNewButton = new JButton("");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				menuPanel.setVisible(true);
				gamePanel.setVisible(false);
				endPanel.setVisible(false);
				tutorialPanel.setVisible(false);
				
			}
		});
		btnNewButton.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/ReturnToMenuButton.png")));
		btnNewButton.setBounds(364, 362, 236, 59);
		endPanel.add(btnNewButton);
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				menuPanel.setVisible(true);
				gamePanel.setVisible(false);
				endPanel.setVisible(false);
				tutorialPanel.setVisible(false);

			}
		});
		
		JLabel playRating = new JLabel("Play Rating: 1000");
		playRating.setFont(new Font("Adobe Caslon Pro", Font.PLAIN, 20));
		playRating.setHorizontalAlignment(SwingConstants.CENTER);
		playRating.setForeground(Color.WHITE);
		playRating.setBounds(344, 186, 276, 43);
		endPanel.add(playRating);
		this.playRating = playRating;
		
		JLabel endImage = new JLabel("");
		endImage.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/DefeatScreen.png")));
		endImage.setBounds(0, 0, 1004, 691);
		endPanel.add(endImage);
		this.endImage = endImage;
		
		
		
		JPanel[] playerBoards = {player0Board, player1Board, player2Board};
		JPanel[] eraDecks = {eraDeck1, eraDeck2, eraDeck3, eraDeck4, eraDeck5};
		JPanel[] eraDiscards = {eraDiscard1, eraDiscard2, eraDiscard3, eraDiscard4, eraDiscard5};
		JLabel[] playerNames = {player0Name, player1Name, player2Name};
		JPanel[] playerNationPanels = {player0Nation, player1Nation, player2Nation};
		JPanel[] playerScorePanels = {player0ScorePanel, player1ScorePanel, player2ScorePanel};
		this.playerBoards = playerBoards;
		this.eraDecks = eraDecks;
		this.eraDiscards = eraDiscards;
		this.cardInspector = cardInspector;
		this.instructionLabel = instructionLabel;
		this.BackgroundImage = BackgroundImage;
		this.middleBackgroundImage = middleBackgroundImage;
		this.playerNationPanels = playerNationPanels;
		this.playerScorePanels = playerScorePanels;
		
		//create a new gameboard
		//GameBoard board = new GameBoard(this, frame, gameLog, 1);
		
		GUI gui = this;
		
		JButton settingsButton = new JButton("");
		settingsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				
				//TODO: add settings selection
				SettingsWindow settings = new SettingsWindow(gui);
				settings.showSettings(gui);
				
				
			}
		});
		settingsButton.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/SettingsButton.png")));
		settingsButton.setBounds(800, 30, 121, 39);
		menuPanel.add(settingsButton);
		
		JButton enterGameButton = new JButton("");
		enterGameButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
								
				GameBoard board = new GameBoard(gui, frame, gameLog, aiDifficulty, nation);

				gui.board = board;
				
				for(int i = 0; i < 3; i++) {
					playerNames[i].setText(board.playerNations[i].getCardName());
				}
				
				gameLog.setText("Game Log...");
				cardInspector_1.setIcon(new ImageIcon(GUI.class.getResource("/images/Era1/cardback_med.png")));
				inDialog = false;
				
				startSoundtrack();
				
				menuPanel.setVisible(false);
				gamePanel.setVisible(true);
				tutorialPanel.setVisible(false);
				
				updateGUI();
				
				try {
					aiTurns();//start game
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	
				
			}
		});
		enterGameButton.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/EnterGameButton.png")));
		enterGameButton.setBounds(389, 351, 190, 59);
		menuPanel.add(enterGameButton);

		
		JLabel lblNewLabel = new JLabel("New label");
		lblNewLabel.setIcon(new ImageIcon(GUI.class.getResource("/images/AuxImages/Menu.png")));
		lblNewLabel.setBounds(0, 0, 1006, 691);
		menuPanel.add(lblNewLabel);

	}
}
