import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntPredicate;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.UIManager;

public class GameBoard{
	
	//Constants to assign when the next era is reached
	static int[] ERA = {0, 0, 3, 9, 18, 30, 45, 75};
	
	//constants used in AI calculations
	static double DWEIGHT = 1.2;
	
	//ID of the current  player (human is P0)
	public int currentPlayer;
	
	public boolean gameOver = false;
	public boolean testing;
	public int aiDifficulty;
	
	//a deck of cards for each era
	public Deck[] EraDecks;
	
	//deck of nation cards
	public Deck nationDeck;
	public Card[] playerNations;
	
	//2D array containing player scores in format [players][military/culture/money]
	public int[][] scores;
	
	//array containing each player's current era
	public int[] playerEra;
	
	//array containing the player's hands as lists of cards
	public ArrayList<LinkedList<Card>> playerHands;
	
	//array containing the player's system cards as a lists
	public ArrayList<LinkedList<Card>> playerBoards;
	
	public LinkedList<StackEffect> stack; //keeps track of what cards must be played next
	public LinkedList<Card> triggered; //keep track of what systems have been triggered
	public LinkedList<Card> triggeredNations; //keep track of what systems have been triggered
	public int[][] triggeredPoints;
	public LinkedList<Procedure> alerts;

	
	//variable for if a player lost points since last turn
	public boolean[] lostPoints;
	public boolean lostPointsThisTurn;
	public boolean[] discardedSystem;
	public boolean discardedSystemThisTurn;
	public int[] osmanBeyPlayed;
	public boolean[] religionPlayed;
	public boolean[] movementPlayed;
	public boolean[] versaillePlayed;
	public boolean[][] attackMatrix;
	public boolean voltairePlayed; //played Voltaire this turn?
	
	public int[] playedThisTurn;
	public int drawnThisTurn;
	public int lastPlayedEra;
	
	
	//num cards to draw this turn
	int toDraw;
	
	//GUI elements for setting up output
	private GUI gui;
	private JFrame frame;
	private JTextArea txtrGameLog;
	
	private Random rand;
	
	private Icon icon;
	
	@FunctionalInterface //used to define lambda functions relating to cards
	public interface CardPredicate {
	   boolean test(Card card);
	}
	
	@FunctionalInterface //used to define lambda functions relating to scores
	public interface ScorePredicate {
	   boolean test(int player, int score);
	}
	
	@FunctionalInterface //done = true
	public interface playerFunction {
	   boolean execute(int pid, int chosenPid, GameBoard board);
	} 
	
	@FunctionalInterface //done = true
	public interface cardFunction {
	   boolean execute(int pid, String chosenName, GameBoard board);
	} 
	
	@FunctionalInterface //done = true
	public interface deckFunction {
	   boolean execute(int pid, int era, GameBoard board);
	}
	
	@FunctionalInterface //done = true
	public interface scoreFunction {
	   boolean execute(int pid, int chosenPlayer, int chosenScore, GameBoard board);
	}
	
	@FunctionalInterface //done = true
	public interface Procedure
	{
	    void invoke();
	}
	
	
	//constructor for new a gameboard
	public GameBoard(GUI gui, JFrame frame, JTextArea txtrGameLog, int aiDifficulty, String nation) {
				
		//setup GUI elements for message output
		this.gui = gui;
		this.frame = frame;
		this.txtrGameLog = txtrGameLog;
		this.rand = new Random();
		this.icon = new ImageIcon();
		
		//set look and feel
		UIManager UI=new UIManager();
		UI.put("OptionPane.background", Color.BLACK);
		UI.put("Panel.background", Color.BLACK);
		UI.put("OptionPane.messageForeground", Color.WHITE);
		UI.put("Button.background", Color.BLACK);
		UI.put("Button.foreground", Color.WHITE);
		UI.put("ComboBox.foreground", Color.WHITE);
		UI.put("ComboBox.background", Color.BLACK);
		UI.put("ComboBox.selectionForeground", Color.WHITE);
				
		//metadata
		this.aiDifficulty = aiDifficulty;
		
		this.testing = false; //start out not testing
		
		//human starts
		this.currentPlayer = 0;
		
		//initialize each player's starting era to 1
		this.playerEra = new int[3];
		for(int pid = 0; pid < 3; pid++) {
			this.playerEra[pid] = 1;
		}
		//create array to store Era decks
		this.EraDecks = new Deck[6];
		
		//initialize decks for each Era
		LinkedList<Card> Cards1 = CreateEra1Cards();
		EraDecks[1] = new Deck(Cards1); //create era deck 1
		
		LinkedList<Card>Cards2 = CreateEra2Cards();
		EraDecks[2] = new Deck(Cards2);
		
		LinkedList<Card>Cards3 = CreateEra3Cards();
		EraDecks[3] = new Deck(Cards3);
		
		LinkedList<Card>Cards4 = CreateEra4Cards();
		EraDecks[4] = new Deck(Cards4);
		
		LinkedList<Card>Cards5 = CreateEra5Cards();
		EraDecks[5] = new Deck(Cards5);
		
		//initialize a deck for the nation cards
		LinkedList<Card> nationCards = CreateNationCards();
		nationDeck = new Deck(nationCards);
		
		
		//DEAL NATIONS
		playerNations = new Card[3];
		if(nation == "Random") {
			//pull a random nation card for each player
			for(int pid = 0; pid < 3; pid++) {
				playerNations[pid] = nationDeck.draw();
			}
		}
		//give user the requested nation
		else {
			playerNations[0] = nationDeck.getCard(nation);
			for(int pid = 1; pid < 3; pid++) {
				playerNations[pid] = nationDeck.draw();
			}
		}
		
		//create empty hands and boards for each player
		this.playerHands = new ArrayList<LinkedList<Card>>();
		for (int pid = 0; pid < 3; pid++) {
			playerHands.add(new LinkedList<Card>());
		}
		this.playerBoards = new ArrayList<LinkedList<Card>>();
		for (int pid = 0; pid < 3; pid++) {
			playerBoards.add(new LinkedList<Card>());
		}
		
		this.stack = new LinkedList<StackEffect>();
		this.triggered = new LinkedList<Card>();
		this.triggeredNations = new LinkedList<Card>();
		this.triggeredPoints = new int[3][3];
		for(int id = 0; id < 3; id++) {
			for(int i = 0; i < 3; i++) {
				triggeredPoints[id][i] = 0;
			}
		}
		this.alerts = new LinkedList<Procedure>();
		
		
		//deal each player hands
		for(int pid = 0; pid < 3; pid++) {//for each player
			for(int i = 0; i < 5; i++) {//repeat five times
				playerHands.get(pid).add( EraDecks[1].draw() ); //add a new card to their hand
			}
		}

		//initialize game board to each player having 0 score
		this.scores = new int[3][3];
		for(int pid = 0; pid < 3; pid++) {
			for(int i = 0; i < 3; i ++) {
				scores[pid][i]= 0;
			}
		}
		
		//initialize so no player has lost points or played Osman Bey
		lostPoints = new boolean[3];
		osmanBeyPlayed = new int[3];
		religionPlayed = new boolean[3];
		movementPlayed = new boolean[3];
		discardedSystem = new boolean[3];
		versaillePlayed = new boolean[3];
		for(int pid = 0; pid < 3; pid++) {
			lostPoints[pid] = false;
			osmanBeyPlayed[pid] = 0;
			religionPlayed[pid] = false;
			movementPlayed[pid] = false;
			versaillePlayed[pid] = false;
			discardedSystem[pid] = false;
		}
		attackMatrix = new boolean[3][3]; //who has attacked who this round?
		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 3; j++) {
				attackMatrix[i][j] = false;
			}
		}
		
		playedThisTurn = new int[3];
		for(int i = 0; i < 3; i++) {
			playedThisTurn[i] = 0;
		}
		drawnThisTurn = 0;
		lastPlayedEra = 0;

		newTurn(0);
		
		//give first era bonuses
		for(int pid = 0; pid < 3; pid++) {
			newEra(pid);
		}
		
	}
	
	//given a player, return an int array with the predicted values of the cards in their hand from the list options
	public double[] rankCards(int pid, LinkedList<Card> options) {
		
		int length = options.size();
		double[] cardScores = new double[length];
		
		//save game state
		Deck[] saveEraDecks = EraDecks;
		int[][] saveScores = scores;
		int[] savePlayerEra = playerEra;
		ArrayList<LinkedList<Card>> savePlayerHands = playerHands;
		ArrayList<LinkedList<Card>> savePlayerBoards = playerBoards;
		LinkedList<StackEffect> saveStack  = stack;
		int[] savePlayedThisTurn = playedThisTurn;
		int saveLastPlayedEra = lastPlayedEra;
		boolean oldTestingState;
		if(testing) {
			oldTestingState = true;
		}
		else {
			oldTestingState = false;
		}

		for(int i = 0; i < length; i++) {
							
			this.testing = true;//play in our fake gamestate
			
			//set up dummies
			this.EraDecks = new Deck[6];
			for(int era = 1; era < 6; era++) {
				EraDecks[era] = saveEraDecks[era].copy();
			}
			this.scores = new int[3][3];
			for(int id = 0; id < 3; id++) {
				for(int s = 0; s < 3; s++) {
					scores[id][s] = saveScores[id][s];
				}
			}
			this.playerEra = new int[3];
			for(int id = 0; id < 3; id++) {
				playerEra[id] = savePlayerEra[id];
			}
			this.playerHands = new ArrayList<LinkedList<Card>>();
			for(int id = 0; id < 3; id++) {//add cards to dummy player hands
				LinkedList<Card> cPlayerHand = savePlayerHands.get(id);
				playerHands.add(new LinkedList<Card>());
				for(int k = 0; k < cPlayerHand.size(); k++) {
					playerHands.get(id).add(cPlayerHand.get(k));
				}
			}
			this.playerBoards = new ArrayList<LinkedList<Card>>();
			for(int id = 0; id < 3; id++) {//add cards to dummy player hands
				LinkedList<Card> cPlayerBoard = savePlayerBoards.get(id);
				playerBoards.add(new LinkedList<Card>());
				for(int k = 0; k < cPlayerBoard.size(); k++) {
					playerBoards.get(id).add(cPlayerBoard.get(k));
				}
			}
			this.stack = new LinkedList<StackEffect>();
			this.playedThisTurn = new int[3];
			for(int id = 0; id < 3; id++) {
				playedThisTurn[id] = savePlayedThisTurn[id];
			}
			lastPlayedEra = Integer.valueOf(saveLastPlayedEra);
			
			
			String testedCard = options.get(i).getCardName();
			
			play(testedCard, pid);
			while(!stack.isEmpty()) {
				stack.pop().getEffect().invoke();
			}
			endTurn(pid);
			
			double cScore = scoreBoard(pid); //get the score after playing our card
			cardScores[i] = cScore;
			System.out.println("Player " + pid + " ranks " + testedCard + " as " + cScore);
			
		}
		
		//Restore game state
		this.EraDecks =  saveEraDecks;
		this.scores =  saveScores;
		this.playerEra =  savePlayerEra;
		this.playerHands = savePlayerHands;
		this.playerBoards =  savePlayerBoards;
		this.stack = saveStack;
		this.playedThisTurn = savePlayedThisTurn;
		this.lastPlayedEra = saveLastPlayedEra;
		this.testing = oldTestingState;
		
		return cardScores;
	
	}
	
	//decide which cards to play for the AI's turn, return its name
	public void playCardAI(int pid) {
		
		String playedCard = "NULL";
		
		if(aiDifficulty == 0) {//decide randomly
			int index = 0;
			while(!playable(pid, playerHands.get(pid).get(index))) {
				index++; //find the first playable card
			}
			playedCard = playerHands.get(pid).get(index).getCardName();
		}
		
		else if(aiDifficulty > 0) {//decide based on local optimum
			
			//index of highest scoring play
			int bestCard = 0;
			
			double[] cardScores = rankCards(pid, playerHands.get(pid));
			
			for(int i = 0; i < cardScores.length; i++) {
				if(cardScores[i] > cardScores[bestCard]) {
					bestCard = i;
				}
			}
			
			//return locally optimal card and display it
			playedCard = playerHands.get(pid).get(bestCard).getCardName();			
		}
		
		play(playedCard, pid);
		
	}
	
	public void discardCardsAI(int pid) {
		
		double[] cardScores = rankCards(pid, playerHands.get(pid));
		LinkedList<Card> toDiscard = new LinkedList<Card>();
		int numToDiscard = Math.max(playerHands.get(pid).size() - calcMaxHandSize(pid), 0);
		
		for(int k = 0; k < numToDiscard; k++) {
			
			int worstCard = 0;

			for(int i = 0; i < cardScores.length; i++) {
				if(cardScores[i] < cardScores[worstCard]) {
					worstCard = i;
				}
			}
			cardScores[worstCard] = 50000; //don't pick this card again
			toDiscard.add(playerHands.get(pid).get(worstCard));

		}
		
		while(toDiscard.size() > 0) {
			discard(playerHands.get(pid).get(playerHands.get(pid).indexOf(toDiscard.get(0))).getCardName(), pid);
			toDiscard.remove(0);
		}
		
	}
	
	
	
	//given a set of options to choose from, choose the player which results in best score for player pid
	//return chosen player ID
	public int choosePlayerAI(int pid, playerFunction onClick, LinkedList<Integer> options) {
		
		//save game state
		Deck[] saveEraDecks = EraDecks;
		int[][] saveScores = scores;
		int[] savePlayerEra = playerEra;
		ArrayList<LinkedList<Card>> savePlayerHands = playerHands;
		ArrayList<LinkedList<Card>> savePlayerBoards = playerBoards;
		LinkedList<StackEffect> saveStack  = stack;
		int[] savePlayedThisTurn = playedThisTurn;
		int saveLastPlayedEra = lastPlayedEra;
		boolean oldTestingState;
		if(testing) {
			oldTestingState = true;
		}
		else {
			oldTestingState = false;
		}
		
		double bestScore = -1000;
		int bestPlayer = 0;
		
		
		for(int i = 0; i < options.size(); i++) {
			
			testing = true;
			
			//set up dummies
			this.EraDecks = new Deck[6];
			for(int era = 1; era < 6; era++) {
				EraDecks[era] = saveEraDecks[era].copy();
			}
			this.scores = new int[3][3];
			for(int id = 0; id < 3; id++) {
				for(int s = 0; s < 3; s++) {
					scores[id][s] = saveScores[id][s];
				}
			}
			this.playerEra = new int[3];
			for(int id = 0; id < 3; id++) {
				playerEra[id] = savePlayerEra[id];
			}
			this.playerHands = new ArrayList<LinkedList<Card>>();
			for(int id = 0; id < 3; id++) {//add cards to dummy player hands
				LinkedList<Card> cPlayerHand = savePlayerHands.get(id);
				playerHands.add(new LinkedList<Card>());
				for(int k = 0; k < cPlayerHand.size(); k++) {
					playerHands.get(id).add(cPlayerHand.get(k));
				}
			}
			this.playerBoards = new ArrayList<LinkedList<Card>>();
			for(int id = 0; id < 3; id++) {//add cards to dummy player hands
				LinkedList<Card> cPlayerBoard = savePlayerBoards.get(id);
				playerBoards.add(new LinkedList<Card>());
				for(int k = 0; k < cPlayerBoard.size(); k++) {
					playerBoards.get(id).add(cPlayerBoard.get(k));
				}
			}
			this.stack = new LinkedList<StackEffect>();
			this.playedThisTurn = new int[3];
			for(int id = 0; id < 3; id++) {
				playedThisTurn[id] = savePlayedThisTurn[id];
			}
			lastPlayedEra = Integer.valueOf(saveLastPlayedEra);
			
			onClick.execute(pid, options.get(i), this);//test out choosing that player
			while(!stack.isEmpty()) {
				stack.pop().getEffect().invoke();
			}
			endTurn(pid);
			
			double cScore = scoreBoard(pid);//score my position
			System.out.println("Choosing Player " + options.get(i) + " ranked " + cScore);
			
			
			if(cScore > bestScore) {//if we've found a new best option
				bestScore = cScore;
				bestPlayer = options.get(i);
			}
		}
		
		//Restore game state
		this.EraDecks =  saveEraDecks;
		this.scores =  saveScores;
		this.playerEra =  savePlayerEra;
		this.playerHands = savePlayerHands;
		this.playerBoards =  savePlayerBoards;
		this.stack = saveStack;
		this.playedThisTurn = savePlayedThisTurn;
		this.testing = oldTestingState;
		this.lastPlayedEra = saveLastPlayedEra;

		return bestPlayer;
		
	}
	
public int spendGoldAI(int pid) {
		
		//save game state
		Deck[] saveEraDecks = EraDecks;
		int[][] saveScores = scores;
		int[] savePlayerEra = playerEra;
		ArrayList<LinkedList<Card>> savePlayerHands = playerHands;
		ArrayList<LinkedList<Card>> savePlayerBoards = playerBoards;
		LinkedList<StackEffect> saveStack  = stack;
		int[] savePlayedThisTurn = playedThisTurn;
		int saveLastPlayedEra = lastPlayedEra;
		boolean oldTestingState;
		if(testing) {
			oldTestingState = true;
		}
		else {
			oldTestingState = false;
		}
		
		double bestScore = -1000;
		int bestPick = 0;
		
		
		for(int i = 0; i < 3; i++) {
			
			testing = true;
			
			//set up dummies
			this.EraDecks = new Deck[6];
			for(int era = 1; era < 6; era++) {
				EraDecks[era] = saveEraDecks[era].copy();
			}
			this.scores = new int[3][3];
			for(int id = 0; id < 3; id++) {
				for(int s = 0; s < 3; s++) {
					scores[id][s] = saveScores[id][s];
				}
			}
			this.playerEra = new int[3];
			for(int id = 0; id < 3; id++) {
				playerEra[id] = savePlayerEra[id];
			}
			this.playerHands = new ArrayList<LinkedList<Card>>();
			for(int id = 0; id < 3; id++) {//add cards to dummy player hands
				LinkedList<Card> cPlayerHand = savePlayerHands.get(id);
				playerHands.add(new LinkedList<Card>());
				for(int k = 0; k < cPlayerHand.size(); k++) {
					playerHands.get(id).add(cPlayerHand.get(k));
				}
			}
			this.playerBoards = new ArrayList<LinkedList<Card>>();
			for(int id = 0; id < 3; id++) {//add cards to dummy player hands
				LinkedList<Card> cPlayerBoard = savePlayerBoards.get(id);
				playerBoards.add(new LinkedList<Card>());
				for(int k = 0; k < cPlayerBoard.size(); k++) {
					playerBoards.get(id).add(cPlayerBoard.get(k));
				}
			}
			this.stack = new LinkedList<StackEffect>();
			this.playedThisTurn = new int[3];
			for(int id = 0; id < 3; id++) {
				playedThisTurn[id] = savePlayedThisTurn[id];
			}
			lastPlayedEra = Integer.valueOf(saveLastPlayedEra);
			
			int[] assignments = {0,0,0};
			assignments[i] = 1;
			
			updateScores(pid, assignments[0], assignments[1], assignments[2]);
			while(!stack.isEmpty()) {
				stack.pop().getEffect().invoke();
			}

			
			double cScore = scoreBoard(pid);//score my position
			System.out.println("Choosing Score " + i + " ranked " + cScore);
			
			
			if(cScore > bestScore) {//if we've found a new best option
				bestScore = cScore;
				bestPick = i;
			}
		}
		
		//Restore game state
		this.EraDecks =  saveEraDecks;
		this.scores =  saveScores;
		this.playerEra =  savePlayerEra;
		this.playerHands = savePlayerHands;
		this.playerBoards =  savePlayerBoards;
		this.stack = saveStack;
		this.playedThisTurn = savePlayedThisTurn;
		this.testing = oldTestingState;
		this.lastPlayedEra = saveLastPlayedEra;

		return bestPick;
		
	}
	
	//heuristic to score the current position of the player pid, without regard for opponent's boards
	//high score = good
	public double scoreMyPosition(int pid) {
		double score = 0;

		
		if(playerEra[pid] == 5) {//if we're already in the last era, heavily weight highest track
			int maxScore = 0;
			for(int i = 0; i < 3; i++) {
				if(scores[pid][i] > scores[pid][maxScore]) {
					maxScore = i;
				}
			}
			if(maxScore >= ERA[7]) {//this play will win...
				return 2000;
			}
			
			double pointsToWin = ERA[7] - scores[pid][maxScore];
			score = pointsToWin;
			for(int i = 0; i < 3; i++) {
				if(i != maxScore) {pointsToWin += (double)(ERA[6] - scores[pid][i]) / 5.0;} //add heavily discounted other tracks
			}
			
			double[] pointsLeft = new double[3]; //store points left to next era victory
			pointsLeft[0] = Math.max(ERA[playerEra[pid] + 1] - scores[pid][0], 0);
			pointsLeft[1] = Math.max(ERA[playerEra[pid] + 1] - scores[pid][1], 0);
			pointsLeft[2] = Math.max(ERA[playerEra[pid] + 1] - scores[pid][2], 0);
			
			for(int i = 0; i < 3; i++) {
				score += pointsLeft[i];
			}
			
			double bestPathToVictory = Math.min(pointsToWin, score);
			double otherPath = Math.max(pointsToWin, score);
			
			
			score = 600.0 + ((double)ERA[6] / bestPathToVictory) + (5.0 * (double)ERA[6] / otherPath);
			return score;
			
		}
		
		else if(playerEra[pid] == 4) {//if we're close
			int maxScore = 0;
			for(int i = 0; i < 3; i++) {
				if(scores[pid][i] > scores[pid][maxScore]) {
					maxScore = i;
				}
			}
			
			double pointsToWin = ERA[6] - scores[pid][maxScore];
			for(int i = 0; i < 3; i++) {
				if(i != maxScore) {pointsToWin += (double)(ERA[6] - scores[pid][i]) / 5.0;} //add heavily discounted other tracks
			}
			
			double[] pointsLeft = new double[3]; //store points left to next era
			pointsLeft[0] = Math.max(ERA[playerEra[pid] + 1] - scores[pid][0], 0);
			pointsLeft[1] = Math.max(ERA[playerEra[pid] + 1] - scores[pid][1], 0);
			pointsLeft[2] = Math.max(ERA[playerEra[pid] + 1] - scores[pid][2], 0);
			
			for(int i = 0; i < 3; i++) {
				score += pointsLeft[i];
			}
			
			score = score + (pointsToWin / 5.0);
			score = 500.0 + ((double)ERA[6] / score);
			return score;
		}
		else {//eras 1-3
			double[] pointsLeft = new double[3]; //store points left to next era
			pointsLeft[0] = Math.max(ERA[playerEra[pid] + 1] - scores[pid][0], 0);
			pointsLeft[1] = Math.max(ERA[playerEra[pid] + 1] - scores[pid][1], 0);
			pointsLeft[2] = Math.max(ERA[playerEra[pid] + 1] - scores[pid][2], 0);
			
			double[] pointsLeft2 = new double[3]; //store points left to next era
			pointsLeft2[0] = Math.max(ERA[playerEra[pid] + 2] - scores[pid][0], 0);
			pointsLeft2[1] = Math.max(ERA[playerEra[pid] + 2] - scores[pid][1], 0);
			pointsLeft2[2] = Math.max(ERA[playerEra[pid] + 2] - scores[pid][2], 0);
			
			double score1 = pointsLeft[0] + pointsLeft[1] + pointsLeft[2];
			double score2 = pointsLeft2[0] + pointsLeft2[1] + pointsLeft2[2];
			
			score = score1 + (score2 / 5.0);
			
			double systemBonuses = 0;
			for(int i = 0; i < playerBoards.get(pid).size(); i++) {
				if(playerBoards.get(pid).get(i).getSubtype() == Card.subType.Religion) {
					systemBonuses += 1.5;
				}
				else if(playerBoards.get(pid).get(i).getType() == Card.Type.System) {
					systemBonuses += 0.75;
				}
			}
			
			score = score - systemBonuses;
			
			score = (100.0 * playerEra[pid]) + ((double)ERA[6] / score);
			return score;
		}
		

		
	}
	
	//more advanced algorithm to score the AI's current position
	public double scoreMyPositionAdvanced(int pid) {
		
		double score = 0;

		
		if(playerEra[pid] == 5) {//if we're already in the last era, heavily weight highest track
			int maxScore = 0;
			for(int i = 0; i < 3; i++) {
				if(scores[pid][i] > scores[pid][maxScore]) {
					maxScore = i;
				}
			}
			if(maxScore >= ERA[7]) {//this play will win...
				return 2000;
			}
			
			double pointsToWin = Math.pow(ERA[7] - scores[pid][maxScore], DWEIGHT);
			double totalPointsToWin = Math.pow(ERA[7], DWEIGHT);
			double percentToWin = ((totalPointsToWin - pointsToWin) / totalPointsToWin)* 100;
			
			//calc raw points next era
			double[] pointsLeft = new double[3]; //store points left to next era
			pointsLeft[0] = Math.max(ERA[playerEra[pid] + 1] - scores[pid][0], 0);
			pointsLeft[1] = Math.max(ERA[playerEra[pid] + 1] - scores[pid][1], 0);
			pointsLeft[2] = Math.max(ERA[playerEra[pid] + 1] - scores[pid][2], 0);

			//add weight for distance
			for(int i = 0; i < 3; i++) {
				pointsLeft[i] = Math.pow(pointsLeft[i], DWEIGHT);
			}
			//calc sum of remaining points
			double sumPointsLeft = pointsLeft[0] + pointsLeft[1] + pointsLeft[2];
			//calc distance weighted total distance
			double totalPoints = Math.pow(3*(ERA[playerEra[pid] + 1]), DWEIGHT);
			double percentToNextEra = (Math.max(totalPoints - sumPointsLeft, 0) / totalPoints) * 100;
			
			double bestPathToVictory = Math.max(percentToWin, percentToNextEra);
			double otherPath = Math.min(percentToWin, percentToNextEra);
			
			
			score = 1000 + bestPathToVictory + (otherPath / 10.0);
			return score;
			
		}
		
		else {//eras 1-4
			
			double percentToNextEra = 0;
			double percentToReligion = 0;
			double percentSystems = 0;
			double percentToNextNextEra = 0;
			
			//PERCENT TO NEXT ERA
			//calc raw points next era
			double[] pointsLeft = new double[3]; //store points left to next era
			pointsLeft[0] = Math.max(ERA[playerEra[pid] + 1] - scores[pid][0], 0);
			pointsLeft[1] = Math.max(ERA[playerEra[pid] + 1] - scores[pid][1], 0);
			pointsLeft[2] = Math.max(ERA[playerEra[pid] + 1] - scores[pid][2], 0);
			if(pid == currentPlayer) {
				System.out.println("Mil Left: " + pointsLeft[0]);
				System.out.println("Cul Left: " + pointsLeft[1]);
				System.out.println("Eco Left: " + pointsLeft[2]);

			}
			//add weight for distance
			for(int i = 0; i < 3; i++) {
				pointsLeft[i] = Math.pow(pointsLeft[i], DWEIGHT);
			}
			//calc sum of remaining points
			double sumPointsLeft = pointsLeft[0] + pointsLeft[1] + pointsLeft[2];
			//calc distance weighted total distance
			double totalPoints = Math.pow(3*(ERA[playerEra[pid] + 1]), DWEIGHT);
			percentToNextEra = (Math.max(totalPoints - sumPointsLeft, 0) / totalPoints) * 100;
			
			//PERCENT TO RELIGION
			int religiousGoal = -1;
			if(hasSystem(pid, "Catholic Christianity", false)) {//+2 mil, +1 culture
				religiousGoal = 0;
			}
			else if(hasSystem(pid, "Hinduism", false)) {//+3 culture
				religiousGoal = 1;
			}
			else if(hasSystem(pid, "Islam", false)) {//+2 money, +1 military
				religiousGoal = 0;
			}
			else if(hasSystem(pid, "Orthodox Christianity", false)) {//+2 culture, +1 money
				religiousGoal = 2;
			}
			else if(hasSystem(pid, "Protestant Christianity", false)) {//+2 culture, +1 money
				if(scores[pid][0] > scores[pid][1]) {
					religiousGoal = 0;
				}
				else {
					religiousGoal = 1;
				}
			}
			else if(hasSystem(pid, "Polytheism", false)) {
				religiousGoal = 1;
			}
			else if(hasSystem(pid, "Judaism",false)) {
				religiousGoal = 2;
			}			
			else if(hasSystem(pid, "Shinto",false)) {//+2 culture, +1 money
				religiousGoal = 1;
			}
			if(religiousGoal >= 0) {
				int maxScore = -10;
				for(int i = 0; i < 3; i++) {
					if(scores[i][religiousGoal] > maxScore) {
						maxScore = scores[i][religiousGoal];
					}
				}
				if(maxScore == 0) {maxScore += 1;}
				//currently squaring to decrease importance of small increases...
				percentToReligion = Math.pow((((double)(scores[pid][religiousGoal])) / maxScore), 2) * 100;
			}
			else {
				percentToReligion = 0;
			}
			
			//PERCENT SYSTEMS
			double systemPts = 0;//number of "pts" assigned for the systems this player has
			for(int i = 0; i < playerBoards.get(pid).size(); i++) {
				if(playerBoards.get(pid).get(i).getSubtype() == Card.subType.Political) {
					systemPts += (1.0 + (playerBoards.get(pid).get(i).getEra() * 0.5));
				}
				else if(playerBoards.get(pid).get(i).getSubtype() == Card.subType.Social) {
					systemPts += 2.0;
				}
				else if(playerBoards.get(pid).get(i).getSubtype() == Card.subType.Economic) {
					systemPts += 2.0;
				}
			}
			percentSystems = ((systemPts) / 3.5) * 100; 
			
			//PERCENT TO NEXT NEXT ERA
			//calc raw points next era
			double[] pointsLeft2 = new double[3]; //store points left to next era
			pointsLeft2[0] = Math.max(ERA[playerEra[pid] + 2] - scores[pid][0], 0);
			pointsLeft2[1] = Math.max(ERA[playerEra[pid] + 2] - scores[pid][1], 0);
			pointsLeft2[2] = Math.max(ERA[playerEra[pid] + 2] - scores[pid][2], 0);
			//add weight for distance
			for(int i = 0; i < 3; i++) {
				pointsLeft2[i] = Math.pow(pointsLeft2[i], DWEIGHT);
			}
			//calc sum of remaining points
			double sumPointsLeft2 = pointsLeft2[0] + pointsLeft2[1] + pointsLeft2[2];
			//calc distance weighted total distance
			double totalPoints2 = Math.pow(3*(ERA[playerEra[pid] + 2]), DWEIGHT);
			percentToNextNextEra = (Math.max(totalPoints2 - sumPointsLeft2, 0) / totalPoints2) * 100;
			
			if(pid == currentPlayer) {
				System.out.println("Next Era: " + percentToNextEra);
				System.out.println("Religion Goal: " + (percentToReligion / (4 * playerEra[pid]))); //discount religions more as eras increase
				System.out.println("Systems: " + (percentSystems / 6));
				System.out.println("Next Next Era: " + percentToNextNextEra / 7);
			}
			
			score = 100 * playerEra[pid] + percentToNextEra + (percentToReligion / (4 * playerEra[pid])) + (percentSystems / 6) + (percentToNextNextEra / 7);
			return score;
		}
		

	}
	
	//score the entire board situation for the player, taking into account opp's boards
	public double scoreBoard(int pid) {
		
		if(aiDifficulty == 1) {//mid level (1)
		
			double minOppScore = 10000;
			double maxOppScore = 0;
			for(int i = 0; i < 3; i++) {
				if(i != pid) {
					double oppScore = scoreMyPosition(i);
					if(oppScore < minOppScore) {minOppScore = oppScore;}
					if(oppScore > maxOppScore) {maxOppScore = oppScore;}
				}
			}

			//weight max opp score much more heavily
			double totalScore = scoreMyPosition(pid) - (maxOppScore / 1.5) - (minOppScore / 5.0);

			return totalScore;
		}
		
		else {//advanced level (2-3)
			double minOppScore = 10000;
			double maxOppScore = 0;
			for(int i = 0; i < 3; i++) {
				if(i != pid) {
					double oppScore = scoreMyPositionAdvanced(i);
					if(i == 0 && aiDifficulty == 3) {
						oppScore = oppScore * 3.5;
					}
					if(oppScore < minOppScore) {minOppScore = oppScore; System.out.println("Player " + i + " is weaker foe");}
					if(oppScore > maxOppScore) {maxOppScore = oppScore; System.out.println("Player " + i + " is stronger foe");}
				}
			}
			//weight max opp score much more heavily
			double totalScore = scoreMyPositionAdvanced(pid) - (maxOppScore / 1.8) - (minOppScore / 14.0);
			return totalScore;
		}
	}
	
	public void newTurn(int pid) {//initilalize a new player's turn
		currentPlayer = pid;
		lostPointsThisTurn = false;
		discardedSystemThisTurn = false;
		religionPlayed[pid] = false;
		movementPlayed[pid] = false;
		if(versaillePlayed[pid]) {
			versaillePlayed[pid] = false;
			for(int i =0; i < 3; i++) {
				if(i != pid) {updateScores(i, 6, 0, 0);}
			}
		}
		for(int i = 0; i < 3; i++) {
			attackMatrix[pid][i] = false;
		}
		toDraw = 2;
		
		voltairePlayed = false;
		
		playedThisTurn[0] = 0;
		playedThisTurn[1] = 0;
		playedThisTurn[2] = 0;

		drawnThisTurn = 0;
		
	}
	
	public void endTurn(int pid) {
		if(!testing) {
			lostPoints[pid] = lostPointsThisTurn;
			discardedSystem[pid] = discardedSystemThisTurn;
		}
		
		//Bhuddism
		if(pid == hasLeast(0) && hasSystem(pid, "Buddhism", true)) {//+3 culture
			updateScores(pid, 0,1,0);
		}

		
		//Catholic Christianity
		if(pid == hasMost(0) && hasSystem(pid, "Catholic Christianity", true)) {//+2 mil, +1 culture
			updateScores(pid, 0,1,0);
		}
		
		//Atheism
		if(playedThisTurn[pid] > 1 && hasSystem(pid, "Atheism", true)) {
			stack.push( 
					new StackEffect(pid,
					() -> {distributeGold(pid, 2, "Atheism");}));
		}
		
		//Hinduism
		if(pid == hasMost(1) && hasSystem(pid, "Hinduism", true)) {//+3 culture
			updateScores(pid, 1,0,0);
		}
		
		//Islam
		if(pid == hasMost(0) && hasSystem(pid, "Islam", true)) {//+2 money, +1 military
			updateScores(pid, 0,0,1);
		}
		
		//Orthodox Christianity
		if(pid == hasMost(2) && hasSystem(pid, "Orthodox Christianity", true)) {//+2 culture, +1 money
			updateScores(pid, 0,1,0);
		}
		//Orthodox Christianity
		if((pid == hasMost(1) || pid == hasMost(0)) && hasSystem(pid, "Protestant Christianity", true)) {//+2 culture, +1 money
			updateScores(pid, 0,0,1);
		}
		//Polytheism
		if(pid == hasMost(1) && hasSystem(pid, "Polytheism", true)) {
			updateScores(pid, 0,0,1);
		}

		if(pid == hasMost(2) && hasSystem(pid, "Judaism",true)) {
			updateScores(pid, 1,0,0);
		}
		
		if(pid == hasMost(1) && hasSystem(pid, "Shinto",true)) {//+2 culture, +1 money
			drawCards(pid, "Shinto", (int i) -> {return i <= playerEra[pid];}, 1);
		}
		
		if(!testing) {
			System.out.println("Player " + pid + " position: " + scoreBoard(pid));
			System.out.println("_________________________________________________");
		}
		
	}
	
	//have the indicated player play the indicated card, and update scores accordingly. Return false if play is illegal.
	public void play(String cardName, int pid) {
		
		playedThisTurn[pid] += 1;
			
		stack.push(
				new StackEffect(pid, true,
						()->{
							if(!testing) {
								GUI.playSound("PlayCard.wav");
							}
							realPlay(cardName, pid);
						}
						)
				);
		
	}

	public void realPlay(String cardName, int pid) {

		//find the specified card in the hand of the correct player
		int index = -1;
		for(int i = 0; i < playerHands.get(pid).size(); i++) {
			if(playerHands.get(pid).get(i).getCardName() == cardName) {
				index = i;
			}
		}
		if(index < 0) { //if card was not found in your hand
			return; //then this play was illegal
		}
		
		Card playedCard = playerHands.get(pid).get(index);
		lastPlayedEra = playedCard.getEra();
		
		//check playability
		if(!playable(pid, playedCard)) {
			return;
		}
		
		if(!testing) {
			gui.displayCard(playedCard, gui.cardInspector);

			output("The " + playerNations[pid].getCardName() + " plays " + playedCard.getCardName());
		}
		
		if(playedThisTurn[pid] > 1 && hasSystem(pid, "Bureaucracy", true)) {
			updateScores(pid, 0, playedThisTurn[pid]-1, 0);
			drawCards(pid, "Bureaucracy", (int i )->{return i <= playerEra[pid];}, playedThisTurn[pid] -1);
		}

		
		//put systems onto playerBoard
		if(playedCard.getType() == Card.Type.System || playedCard.getCardName() == "National Seclusion Policy") {//national seclusion policy is special case
			
			//Sedentarianism gains culture for each system
			if(playedCard.getType() == Card.Type.System) {
				if(hasNation(pid, "Roman Empire", true)) {updateScores(pid, 1, 0, 0);} 
				if(hasNation(pid, "Achaemenid Empire", true)) {updateScores(pid, 0, 1, 0);} 
				if(hasNation(pid, "Inca Empire", true)) {updateScores(pid, 0, 0, 1);} 
				if(playedCard.getSubtype() == Card.subType.Religion) {
					if(!testing) {
						for(int i = 2; i >= 0; i--) {
							if(i != pid) {
								int k = i;
								stack.push(
										new StackEffect(k,
												()->{
													chooseCard(k, 
															"Play The Counter-Reformation",
															((Card c) -> {return c.getCardName() == "The Counter-Reformation";}), 
															(int j, String chosenName, GameBoard board) -> {
																stack.push(
																		new StackEffect(
																				k, false,
																				() -> {updateScores(pid, -1, -1, -1);}
																				)
																		);
																play(chosenName, k);
															return true;}
															);
												}
												));
							}
						}
					}
				}
			}

	
			//delete any system of that type from playerBoard
			//TODO: make sure this actually works in all cases
			for(int i = 0; i < playerBoards.get(pid).size(); i++) {
				if(		playedCard.getType() == Card.Type.System && //if they're systems
						playerBoards.get(pid).get(i).getSubtype() == playedCard.getSubtype() && //if they share a subtype
						!(playedCard.getCardName() == "Shinto" || playerBoards.get(pid).get(i).getCardName() == "Shinto") &&
						!(voltairePlayed && playedCard.getSubtype() == Card.subType.Religion) &&
						!((playedCard.getSubtype() == Card.subType.Social || playedCard.getSubtype() == Card.subType.Political) && hasNation(pid, "Han Dynasty", false)) &&
						!(playedCard.getSubtype() == Card.subType.Religion && hasNation(pid, "Mughal Empire", false)) ) 		
						 {
					//discard the old system
					discardFromBoard(playerBoards.get(pid).get(i).getCardName(), pid);
				}
			}
			//add playedCard to the correct playerBoard and remove from hand
			playerBoards.get(pid).add(playedCard);
			playerHands.get(pid).remove(index);
			
			int military = playedCard.getMilitary();
			int culture = playedCard.getCulture();
			int money = playedCard.getMoney();
			updateScores(pid, military, culture, money);
		}
		//get effect of leader/event card and discard it
		else {	
			boolean pointsLost = false; 
			for(int i = 0; i < 3; i++) {
				if(i != pid && lostPoints[i] == true) { //if another player lost points
					pointsLost = true;
				}
			}
			
			//Const. Monarchy gains card for each ruler
			if(playedCard.isSubtype(Card.subType.Philosopher) && hasSystem(pid, "Democracy", true)) {
				updateScores(pid, 0, 1, 0);
			}
			if(playedCard.isSubtype(Card.subType.Entente) && hasSystem(pid, "Republicanism", true)) {
				updateScores(pid, 0, 0, 1);
			}
			if((playedCard.getSubtype() == Card.subType.Conflict|| playedCard.isSubtype(Card.subType.General)) && hasSystem(pid, "Feudalism", true)) {
				updateScores(pid, 1, 0, 0);
			}
			if((playedCard.isSubtype(Card.subType.Ruler) || playedCard.getSubtype() == Card.subType.Movement) && hasSystem(pid, "Constitutional Monarchy", true)) {
				updateScores(pid, 0, 1, 0);
			}

			if((playedCard.isSubtype(Card.subType.Ruler) || playedCard.isSubtype(Card.subType.General) || playedCard.isSubtype(Card.subType.Conflict)) && hasSystem(pid, "Fascism", true)) {
				updateScores(pid, 2, 0, 0);
			}
			if(playedCard.isSubtype(Card.subType.Ruler) && hasSubtype(pid, Card.subType.Religion) && hasNation(pid, "Holy Roman Empire", true)) {
				updateScores(pid, 0, 1, 0);
			}
			if(playedCard.isSubtype(Card.subType.Cleric) && hasSubtype(pid, Card.subType.Political) && hasNation(pid, "Holy Roman Empire", true)) {
				updateScores(pid, 0, 1, 0);
			}
			if(playedCard.isSubtype(Card.subType.Cleric) && hasNation(pid, "Islamic Caliphate", true)) {
				updateScores(pid, 1, 0, 0);
			}
			if(playedCard.isSubtype(Card.subType.Rebel) && hasNation(pid, "Soviet Union", true)) {
				updateScores(pid, 1, 1, 0);
			}
			if(playedCard.isSubtype(Card.subType.Conflict) && hasNation(pid, "German Empire", true)) {
				updateScores(pid, 1, 0, 0);
			}
			if(playedCard.isSubtype(Card.subType.Philosopher) && hasNation(pid, "Athenian Empire", true)) {
				updateScores(pid, 0, 1, 0);
			}
			if(playedCard.isSubtype(Card.subType.Proclamation) && hasNation(pid, "French Empire", true)) {
				updateScores(pid, 0, 1, 0);
			}
			if((playedCard.getSubtype() == Card.subType.Discovery || playedCard.isSubtype(Card.subType.Explorer)) && hasNation(pid, "Portuguese Empire", true)) {
				updateScores(pid, 0, 0, 1);
			}
			if(playedCard.getSubtype() == Card.subType.Movement) {
				if(!testing) {
					for(int i = 2; i >= 0; i--) {
						if(i != pid) {
							int k = i;
							stack.push(
									new StackEffect(k,
											()->{
												chooseCard(k, 
														"Play The Counter-Reformation",
														((Card c) -> {return c.getCardName() == "The Counter-Reformation";}), 
														(int j, String chosenName, GameBoard board) -> {
															stack.push(
																	new StackEffect(
																			k, false,
																			() -> {updateScores(pid, -1, -1, -1);}
																			)
																	);
															play(chosenName, k);
														return true;}
														);
											}
											));
						}
					}
				}
			}
			
			
			//Congress of Vienna -- play after if a conflict is played
			if(playedCard.getSubtype() == Card.subType.Conflict) {
				stack.push(
						new StackEffect(pid, false,
						() -> {
							chooseCard(pid,
									playedCard.getCardName() + ": Play The Congress of Vienna",
									((Card c) -> c.getCardName() == "The Congress of Vienna"), 
									(int i, String chosenName, GameBoard board) -> {play(chosenName, pid);
																				return true;}
									);
						})
						);
				stack.push(
						new StackEffect(pid, false,
						() -> {
							chooseCard(pid, 
									playedCard.getCardName() + ": Play The Yalta Conference",
									((Card c) -> c.getCardName() == "The Yalta Conference"), 
									(int i, String chosenName, GameBoard board) -> {play(chosenName, pid);
																				return true;}
									);
						})
						);
				
			}
			
			//increase points
			int military = playedCard.getMilitary();
			int culture = playedCard.getCulture();
			int money = playedCard.getMoney();
			if(playedCard.getSubtype() == Card.subType.Religion && !testing) {
				religionPlayed[pid] = true;
			}
			else if(playedCard.getSubtype() == Card.subType.Movement && !testing) {
				movementPlayed[pid] = true;
			}
		
			
			updateScores(pid, military, culture, money);
			//discard the card
			if(playedCard.getCardName() != "The Monroe Doctrine") {
				EraDecks[playedCard.getEra()].discard(playedCard);
				playerHands.get(pid).remove(index);
			}
		
		//SPECIAL PLAY EFFECTS
		playedCard.effect.execute(pid);	
		
		}
	}
	
	//used for copying a card
	public void playCard(int pid, Card playedCard) {
				
		//check playability
		if(!playable(pid, playedCard)) {
			return;
		}
		
		if(!testing) {
			gui.displayCard(playedCard, gui.cardInspector);
		}
		
		//put systems onto playerBoard
		if(playedCard.getType() == Card.Type.System || playedCard.getCardName() == "National Seclusion Policy") {//national seclusion policy is special case
			
			//Sedentarianism gains culture for each system
			if(playedCard.getType() == Card.Type.System) {
				if(hasNation(pid, "Roman Empire", true)) {updateScores(pid, 1, 0, 0);} 
				if(hasNation(pid, "Achaemenid Empire", true)) {updateScores(pid, 0, 1, 0);} 
				if(hasNation(pid, "Inca Empire", true)) {updateScores(pid, 0, 0, 1);} 
				if(playedCard.getSubtype() == Card.subType.Religion) {
					if(!testing) {
						for(int i = 2; i >= 0; i--) {
							if(i != pid) {
								int k = i;
								stack.push(
										new StackEffect(k,
												()->{
													chooseCard(k, 
															"Opponent Played Religion/Movement: Play The Counter-Reformation",
															((Card c) -> {return c.getCardName() == "The Counter-Reformation";}), 
															(int j, String chosenName, GameBoard board) -> {
																stack.push(
																		new StackEffect(
																				k, false,
																				() -> {updateScores(pid, -1, -1, -1);}
																				)
																		);
																play(chosenName, k);
															return true;}
															);
												}
												));
							}
						}
					}
				}
			}

	
			//delete any system of that type from playerBoard
			//TODO: make sure this actually works in all cases
			for(int i = 0; i < playerBoards.get(pid).size(); i++) {
				if(		playedCard.getType() == Card.Type.System && //if they're systems
						playerBoards.get(pid).get(i).getSubtype() == playedCard.getSubtype() && //if they share a subtype
						!(playedCard.getCardName() == "Shinto" || playerBoards.get(pid).get(i).getCardName() == "Shinto") &&
						!(voltairePlayed && playedCard.getSubtype() == Card.subType.Religion) &&
						!((playedCard.getSubtype() == Card.subType.Social || playedCard.getSubtype() == Card.subType.Political) && hasNation(pid, "Han Dynasty", false))
								) {//and neither is Shinto
					//discard the old system
					EraDecks[playerBoards.get(pid).get(i).getEra()].discard(playerBoards.get(pid).get(i));
					playerBoards.get(pid).remove(i);
				}
			}
			//add playedCard to the correct playerBoard and remove from hand
			playerBoards.get(pid).add(playedCard);
		}
		//get effect of leader/event card and discard it
		else {	
			boolean pointsLost = false; 
			for(int i = 0; i < 3; i++) {
				if(i != pid && lostPoints[i] == true) { //if another player lost points
					pointsLost = true;
				}
			}
			
			if((playedCard.getSubtype() == Card.subType.Entente || playedCard.isSubtype(Card.subType.Philosopher)) && hasSystem(pid, "Democracy", true)) {
				updateScores(pid, 0, 0, 1);
			}
			if((playedCard.getSubtype() == Card.subType.Conflict|| playedCard.isSubtype(Card.subType.General)) && hasSystem(pid, "Feudalism", true)) {
				updateScores(pid, 1, 0, 0);
			}
			if((playedCard.isSubtype(Card.subType.Ruler) || playedCard.getSubtype() == Card.subType.Movement) && hasSystem(pid, "Constitutional Monarchy", true)) {
				updateScores(pid, 0, 1, 0);
			}
			if(playedThisTurn[pid] > 1 && hasSystem(pid, "Bureaucracy", true)) {
				updateScores(pid, 0, playedThisTurn[pid]-1, 0);
				drawCards(pid, "Bureaucracy", (int i )->{return i <= playerEra[pid];}, playedThisTurn[pid] -1);
			}

			if(playedCard.isSubtype(Card.subType.Ruler) && hasSubtype(pid, Card.subType.Religion) && hasNation(pid, "Holy Roman Empire", true)) {
				updateScores(pid, 0, 1, 0);
			}
			if(playedCard.isSubtype(Card.subType.Cleric) && hasSubtype(pid, Card.subType.Political) && hasNation(pid, "Holy Roman Empire", true)) {
				updateScores(pid, 0, 1, 0);
			}
			if(playedCard.isSubtype(Card.subType.Cleric) && hasNation(pid, "Islamic Caliphate", true)) {
				updateScores(pid, 1, 0, 0);
			}
			if(playedCard.isSubtype(Card.subType.Philosopher) && hasNation(pid, "Athenian Empire", true)) {
				updateScores(pid, 0, 1, 0);
			}
			if(playedCard.getSubtype() == Card.subType.Movement) {
				if(!testing) {
					for(int i = 2; i >= 0; i--) {
						if(i != pid) {
							int k = i;
							stack.push(
									new StackEffect(k,
											()->{
												chooseCard(k, 
														"Play The Counter-Reformation",
														((Card c) -> {return c.getCardName() == "The Counter-Reformation";}), 
														(int j, String chosenName, GameBoard board) -> {
															stack.push(
																	new StackEffect(
																			k, false,
																			() -> {updateScores(pid, -1, -1, -1);}
																			)
																	);
															play(chosenName, k);
														return true;}
														);
											}
											));
						}
					}
				}
			}
	
	
	//Congress of Vienna -- play after if a conflict is played
	if(playedCard.getSubtype() == Card.subType.Conflict) {
		stack.push(
				new StackEffect(pid, false,
				() -> {
					chooseCard(pid,
							playedCard.getCardName() + ": Play The Congress of Vienna",
							((Card c) -> c.getCardName() == "The Congress of Vienna"), 
							(int i, String chosenName, GameBoard board) -> {play(chosenName, pid);
																		return true;}
							);
				})
				);
		stack.push(
				new StackEffect(pid, false,
				() -> {
					chooseCard(pid, 
							playedCard.getCardName() + ": Play The Yalta Conference",
							((Card c) -> c.getCardName() == "The Yalta Conference"), 
							(int i, String chosenName, GameBoard board) -> {play(chosenName, pid);
																		return true;}
							);
				})
				);
		
	}
	
	//increase points
	int military = playedCard.getMilitary();
	int culture = playedCard.getCulture();
	int money = playedCard.getMoney();
	//double military of generals for nomadism
	if(playedCard.getSubtype() == Card.subType.Religion && !testing) {
		religionPlayed[pid] = true;
	}
	else if(playedCard.getSubtype() == Card.subType.Movement && !testing) {
		movementPlayed[pid] = true;
	}

	
	updateScores(pid, military, culture, money);
//SPECIAL PLAY EFFECTS
playedCard.effect.execute(pid);	

}
}
	
	public void chooseCard(int pid, String message, CardPredicate condition, cardFunction onClick) {
		LinkedList<Card> options = new LinkedList<Card>();
		for(int i = 0; i < playerHands.get(pid).size(); i++) {
			if(condition.test(playerHands.get(pid).get(i)) && playable(pid, playerHands.get(pid).get(i))) {//if the tested card meets the condition AND IS PLAYABLE, add it to options
				options.add(playerHands.get(pid).get(i));
			}
		}

		if(options.size() > 0) {
			if(pid == 0) {//present options to the player
				gui.chooseCard(onClick, options, pid, message);
			}
			else{//play random system for AI
				
				double[] cardScores = rankCards(pid, options);
				int maxScore = 0;
				for(int i = 0; i < cardScores.length; i++) {
					if(cardScores[i] > cardScores[maxScore]) {
						maxScore = i;
					}
				}
				String chosenCard = options.get(maxScore).getCardName();
				onClick.execute(pid, chosenCard, this);
			}
		}
	}
	
	public void chooseCard(int pid, String message, cardFunction onClick, int n) {
		LinkedList<String> options = new LinkedList<String>();
		for(int i = 0; i < playerHands.get(pid).size(); i++) {
				options.add(playerHands.get(pid).get(i).getCardName());
		}

		if(options.size() > 0) {
			if(pid == 0) {//present options to the player
				gui.chooseCard(onClick, options, pid, message, n);
			}
			else{//choose randomly for AI
				String chosenCard = options.get(rand.nextInt(options.size()));
				onClick.execute(pid, chosenCard, this);
				if(n > 1) {
					chooseCard(pid, message, onClick, n-1);
				}
			}
		}
	}
	
	
	public void chooseAllCards(int pid, String message, CardPredicate condition, cardFunction onClick) {
		LinkedList<Card> options = new LinkedList<Card>();
		for(int i = 0; i < playerHands.get(pid).size(); i++) {
			if(condition.test(playerHands.get(pid).get(i)) &&  playable(pid, playerHands.get(pid).get(i))) {//if the tested card meets the condition AND IS PLAYABLE, add it to options
				options.add(playerHands.get(pid).get(i));
			}
		}

		if(options.size() > 0) {
			if(pid == 0) {//present options to the player
				stack.push(
						new StackEffect(pid,
						() -> {chooseAllCards(pid, message, condition, onClick);}));
				gui.chooseCard(onClick, options, pid, message);
			}
			else{//play random system for AI
				stack.push(
						new StackEffect(pid,
						() -> {chooseAllCards(pid, message, condition, onClick);}));
				String chosenCard = options.get(rand.nextInt(options.size())).getCardName();
				onClick.execute(pid, chosenCard, this);
			}
		}
	}
	
	public void chooseDeck(int pid, String message, IntPredicate condition, deckFunction onClick) {
		LinkedList<Integer> options = new LinkedList<Integer>();
		for(int i = 1; i < 6; i++) {
			if(condition.test(i)) {//if the tested card meets the condition, add it to options
				options.add(i);
			}
		}

		if(options.size() > 0) {
			if(pid == 0) {//present options to the player
				gui.chooseDeck(onClick, options, pid, message);
			}
			else{//choose max era for AI
				int chosenDeck = options.getLast();
				onClick.execute(pid, chosenDeck, this);
			}
		}
	}
	
	public void chooseDiscard(int pid, String message, IntPredicate condition, deckFunction onClick, int n) {
		LinkedList<Integer> options = new LinkedList<Integer>();
		for(int i = 1; i < 6; i++) {
			if(condition.test(i) && EraDecks[i].hasDiscard()) {//if the tested deck meets condition and has discard pile
				options.add(i);
			}
		}

		if(options.size() > 0) {
			if(pid == 0) {//present options to the player
				gui.chooseDiscard(onClick, options, pid, message, n);
			}
			else{//choose max era for AI
				int chosenDeck = options.getLast();
				onClick.execute(pid, chosenDeck, this);
				if(n > 1) {//repeat n times, or until discard piles are exhausted
					chooseDiscard(pid, message, condition, onClick, n-1);
				}
			}
		}
	}
	
	//repeat n times
	public void chooseDeck(int pid, String message, IntPredicate condition, deckFunction onClick, int n) {
		LinkedList<Integer> options = new LinkedList<Integer>();
		for(int i = 1; i < 6; i++) {
			if(condition.test(i)) {//if the tested card meets the condition, add it to options
				options.add(i);
			}
		}

		if(options.size() > 0) {
			if(pid == 0) {//present options to the player
				gui.chooseDeck(onClick, options, pid, message, n);
			}
			else{//choose max era for AI
				int chosenDeck = options.getLast();
				onClick.execute(pid, chosenDeck, this);
				if(n > 1) {
					chooseDeck(pid, message, condition, onClick, n-1);
				}
			}
		}
	}
	
	
	//choose a score. n = times to repeat process
	public void chooseScore(int pid, String message, ScorePredicate condition, scoreFunction onClick, int n) {
		LinkedList<int[]> options = new LinkedList<int[]>();
		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < 3; j++) {
				if(condition.test(i,j)) {
					int[] newArray = {i,j};
					options.add(newArray);
				} 
			}
		}

		if(options.size() > 0) {
			if(pid == 0) {//present options to the player
				gui.chooseScore(onClick, options, pid, message, n);
			}
			else{//choose random score for AI
				int[] chosenScore = options.get(rand.nextInt(options.size()));
				onClick.execute(pid, chosenScore[0], chosenScore[1], this);
			}
		}
	}
	
	public boolean chooseSystem(int pid, String message, CardPredicate condition, cardFunction onClick) {
		LinkedList<String> options = new LinkedList<String>();
		for(int i = 0; i < playerBoards.get(pid).size(); i++) {
			if(condition.test(playerBoards.get(pid).get(i))) {//if the tested card meets the condition, add it to options
				options.add(playerBoards.get(pid).get(i).getCardName());
			}
		}

		if(options.size() > 0) {
			if(pid == 0) {//present options to the player
				gui.chooseSystem(onClick, options, pid, message);
			}
			else{//play random system for AI
				String chosenCard = options.get(rand.nextInt(options.size()));
				onClick.execute(pid, chosenCard, this);
			}
			return true;
		}
		return false;
	}
	
	public int choosePlayer(int pid, String mode, String message, IntPredicate condition, playerFunction onClick, boolean repeats) {	
		
		for(int id = 0; id < 3; id++) {//aztecs count as having 5 less mil for attacks
			if(id != pid && hasNation(id, "Aztec Empire", false)) {
				scores[id][0] -= 5;
			}
		}
		
		LinkedList<Integer> options = new LinkedList<Integer>();
		for(int i = 0; i < 3; i++) {
			if(condition.test(i) && 
					!(mode != "None" && hasSystem(i, "National Seclusion Policy", false)) &&
					!(mode != "None" && hasSystem(i, "The Monroe Doctrine", false) && pid != getCardFromBoard(i, "The Monroe Doctrine").getPlayer()) && 
					!(mode != "None" && hasNation(i, "Qing Dynasty", false)) &&	
					!(mode == "Attack" && hasNation(i, "Egyptian Empire", false))){//if this player meets the condition (no player can select someone with National Seclusion Policy
				options.add(i);
			}
		}
		
		for(int id = 0; id < 3; id++) {//give them back their mil
			if(id != pid && hasNation(id, "Aztec Empire", false)) {
				scores[id][0] += 5;
			}
		}
		
		if(options.size() > 0) {			
			
			int chosenPid;
			playerFunction newOnClick = onClick;
			
			if(mode == "Attack") {
				
				if(repeats == true && hasSystem(pid, "Nomadism", true)) {
					stack.push(
							new StackEffect(pid, true,
									()-> {
										choosePlayer(pid, mode, message, condition, onClick, false);
									}
									
									)
							);
				}
				
				newOnClick = (int id, int chosenID, GameBoard board) -> {
					output("The " + playerNations[id].getCardName() + " attacks the " + playerNations[chosenID].getCardName());
					if(!testing) {
						attackMatrix[chosenID][id] = true;
						if(hasNation(pid, "Assyrian Empire", true)) {//discard their hand
							while(playerHands.get(chosenID).size() > 0) {
								Card discardedCard = playerHands.get(chosenID).get(0);
								discard(discardedCard.getCardName(), chosenID);
							}
						}
					}
					if(hasSubtype(chosenID, Card.subType.Religion) && hasNation(pid, "Safavid Empire", true)) {
						updateScores(pid, 1, 0, 0);
					}
					if(hasNation(pid, "Spanish Empire", true)) {
						updateScores(chosenID, -1, 0, 0);
					}
					if(scores[chosenID][0] < scores[pid][0] && hasNation(pid, "British Empire", true)) {
						updateScores(pid, 0, 0, 1);
					}
					int lowest = 100;
					for(int i = 0; i < 3; i++) {
						if(scores[chosenID][i] < lowest) {lowest = scores[chosenID][i];}
					}
					for(int i = 0; i < 3; i++) {
						if(scores[chosenID][i] > lowest + 5 && hasNation(pid, "United States of America", true)) {scores[chosenID][i] = lowest+5;}
					}
					if(hasNation(chosenID, "Japanese Empire",true)) {
						updateScores(chosenID, 1, 0, 0);
					}
					
					if(!testing) {//Arab-Israeli War can be played when attacled
						stack.push(
								new StackEffect(chosenID,
										()->{
											chooseCard(chosenID, 
													"Play Arab-Israeli War",
													((Card c) -> {return c.getCardName() == "Arab-Israeli War";}), 
													(int j, String chosenName, GameBoard board2) -> {
														updateScores(currentPlayer, 0, 0, -2);
														play(chosenName, chosenID);
													return true;}
													);
										}
										));

					}
					if(!testing && hasNation(chosenID, "State of Israel", true)) {//State of Israel plays conflict when attacked
						stack.push(
								new StackEffect(chosenID,
										()->{
											chooseCard(chosenID, 
													"Play a General or Conflict",
													((Card c) -> {return c.isSubtype(Card.subType.General) || c.isSubtype(Card.subType.Conflict);}), 
													(int j, String chosenName, GameBoard board2) -> {play(chosenName, chosenID);
													return true;}
													);
										}
										));

					}
					if(!testing && chosenID != 0) {
						GUI.playSound("Attack.wav");
					}
					onClick.execute(id, chosenID, board);
					return true;
				};
				
			}//end Attack
			else if(mode == "Trade") {
				
				if(repeats == true && hasSystem(pid, "Stoicism", true)) {
					stack.push(
							new StackEffect(pid, true,
									()-> {
										choosePlayer(pid, mode, message, condition, onClick, false);
									}
									
									)
							);
				}
				
				newOnClick = (int id, int chosenID, GameBoard board) -> {
					output("The " + playerNations[id].getCardName() + " trades with the " + playerNations[chosenID].getCardName());
					if(hasNation(pid, "Phoenician Empire", true)) {
						updateScores(pid, 0, 0, 1);
					}
					if(scores[chosenID][2] < scores[pid][2] && hasNation(pid, "Dutch Empire", true)) {
						updateScores(pid, 0, 0, 1);
					}
					for(int i = 0; i < 3; i++) {
						if(i != pid && i != chosenID && hasNation(i, "Byzantine Empire", true)) {
							updateScores(i, 0, 0, 1);
						}
					}
					if(hasNation(chosenID, "Japanese Empire",true)) {
						updateScores(chosenID, 1, 0, 0);
					}
					if(!testing && chosenID != 0) {
						GUI.playSound("Trade.wav");
					}
					onClick.execute(id, chosenID, board);
					return true;
				};
				
			}
			else if(mode == "Negotiate") {
				
				if(repeats == true && hasSystem(pid, "Stoicism", true)) {
					stack.push(
							new StackEffect(pid, true,
									()-> {
										choosePlayer(pid, mode, message, condition, onClick, false);
									}
									
									)
							);
				}
				
				newOnClick = (int id, int chosenID, GameBoard board) -> {
					output("The " + playerNations[id].getCardName() + " negotiates with the " + playerNations[chosenID].getCardName());
					if(scores[chosenID][2] < scores[pid][2] && hasNation(pid, "Dutch Empire", true)) {
						updateScores(pid, 0, 0, 1);
					}
					for(int i = 0; i < 3; i++) {
						if(i != pid && i != chosenID && hasNation(i, "Byzantine Empire", true)) {
							updateScores(i, 0, 0, 1);
						}
					}
					if(!testing && chosenID != 0) {
						GUI.playSound("Negotiate.wav");
					}
					onClick.execute(id, chosenID, board);
					return true;
				};
				
			}
			
			
			if(pid == 0) {//give options to player

				gui.choosePlayer(newOnClick, options, pid, message);
				chosenPid = GUI.chosenPid;
			}
			else {//choose automatically for the AI
				chosenPid = choosePlayerAI(pid, newOnClick, options);
				playerFunction finalOnClick = newOnClick;
				stack.push( new StackEffect(pid, true, ()->{
					if(!testing && chosenPid == 0) {
						gui.inDialog = true;
						if(mode == "Attack") {//alert the player if they are attacked
							GUI.playSound("Attack.wav");
							JOptionPane.showMessageDialog(frame, "The " + playerNations[pid].getCardName() + " has attacked us!", "Attack", JOptionPane.PLAIN_MESSAGE, new ImageIcon(GUI.getImagePath("AuxImages", "AttackIcon.png")));
						}
						else if(mode == "Trade") {//alert the player if they are attacked
							GUI.playSound("Trade.wav");
							JOptionPane.showMessageDialog(frame, "The " + playerNations[pid].getCardName() + " has traded with us!", "Trade", JOptionPane.PLAIN_MESSAGE, new ImageIcon(GUI.getImagePath("AuxImages", "TradeIcon.png")));
						}
						else if(mode == "Negotiate") {//alert the player if they are attacked
							GUI.playSound("Negotiate.wav");
							JOptionPane.showMessageDialog(frame, "The " + playerNations[pid].getCardName() + " has negotiated with us!", "Negotiation", JOptionPane.PLAIN_MESSAGE, new ImageIcon(GUI.getImagePath("AuxImages", "NegotiateIcon.png")));
						}
						gui.inDialog = false;
					}
					finalOnClick.execute(pid, chosenPid, this);
					
				
				}
						));
			}
			
			return chosenPid;
		}
		return -1;
	}
	
	//discard a specified card from the hand of the indicated player
	public boolean discard(String cardName, int pid) {
		//find the specified card in the hand of the correct player
		int index = -1;
		for(int i = 0; i < playerHands.get(pid).size(); i++) {
			if(playerHands.get(pid).get(i).getCardName() == cardName) {
				index = i;
			}
		}
		if(index < 0) { //if card was not found in your hand
			return false; //then this play was illegal
		}
				
		Card discardedCard = playerHands.get(pid).get(index);
		output("The " + playerNations[pid].getCardName() + " discards " + discardedCard.getCardName());
		//discard the selected card
		EraDecks[discardedCard.getEra()].discard(discardedCard);
		playerHands.get(pid).remove(index);
		
		return true;
	}
	
	public boolean discardFromBoard(String cardName, int pid) {
		//find the specified card in the board of the correct player
		int index = -1;
		for(int i = 0; i < playerBoards.get(pid).size(); i++) {
			if(playerBoards.get(pid).get(i).getCardName() == cardName) {
				index = i;
			}
		}
		if(index < 0) { //if card was not found in your board
			return false; //then this play was illegal
		}

		Card discardedCard = playerBoards.get(pid).get(index);
		//discard the selected card
		EraDecks[discardedCard.getEra()].discard(discardedCard);
		playerBoards.get(pid).remove(index);
		if(!testing) {
			discardedSystemThisTurn = true;
		}
		
		for(int i = 0; i<3; i++) {
			if(hasNation(i, "People's Republic of China", true)) {
				updateScores(i, 0, 1, 0);
			}
		}
		
		return true;
	}
	
	//draw n cards into the indicated player's hand, from the indicated era deck.
	public void drawCards(int pid, int era, int n) {
		
		drawnThisTurn = drawnThisTurn + n;
		
		for(int i = 0; i < n && EraDecks[era].hasCards(); i++) {
			playerHands.get(pid).add(EraDecks[era].draw());
		}
	}
	
	//draw n cards into the indicated player's hand, from the indicated era deck.
	public void drawCards(int pid, String source, IntPredicate condition, int n) {
		
		if(GUI.mode != 0 && hasSystem(pid, "Confucianism", true)) {
			n = n+1;
			updateScores(pid, 0, 1, 0);
		}
		
		if(!testing) {
			int k = n;

			stack.push(
					new StackEffect(pid,
							() -> {
								chooseDeck(pid, 
										source + ": Draw Cards", 
										condition, 
										(int id, int chosenEra, GameBoard board) ->{
											drawCards(id, chosenEra, 1);
											return true;
										},
										k);
							}));
		}

	}
	
	//discard n cards from the indicated player's hand.
	public void discardCards(int pid, String source, int n) {
		
		if(GUI.mode == 1) {

			if(hasNation(pid, "Islamic Caliphate", true)) {
				updateScores(pid, 1, 0, 0);
			}

			//TODO: IS THIS THROWING ERRORS
			if(GUI.mode != 0 && currentPlayer == pid && hasSystem(pid, "Caste System", true)) {
				
				updateScores(pid, 1, 0, 0);
				EraDecks[lastPlayedEra].casteEffect();
				
				int k = n;
				stack.push(
						new StackEffect(pid,
								() -> {
									chooseDiscard(pid, 
											"Caste System: Draw From a Discard Pile", 
											(int i) -> {return true;}, 
											(int id, int chosenEra, GameBoard board) ->{
												playerHands.get(id).add(EraDecks[chosenEra].drawFromDiscard()); //get all but top card of discard pile
												return true;},
											k
											);
								}));
			}
			else {
				stack.push(
						new StackEffect(pid,
								() -> {
									chooseCard(pid, 
											source + ": Discard Cards", 
											(int id, String chosenName, GameBoard board) ->{
												discard(chosenName, id);
												return true;
											},
											n);
								}));
			}
		}
		
		else {
			stack.push(
					new StackEffect(pid,
							() -> {
								chooseCard(pid, 
										source + ": Discard Cards", 
										(int id, String chosenName, GameBoard board) ->{
											discard(chosenName, id);
											return true;
										},
										n);
							}));
		}

	}
	
	
	//player pid draws cards for the turn. The amount and era is computed automatically.
	public void drawCards(int pid) {
		int n = 2; //num cards to draw
		drawCards(pid, playerEra[pid], n);
	}
	
	//return a player's max hand size, calculated from their systems
	public int calcMaxHandSize(int pid) {
		return 5;
	}
	
	public void output(String txt) {
		if(!testing) {
			txtrGameLog.setText(txtrGameLog.getText() + "\n" + txt);
		}
	}

	//method to update a player's scores
	public void updateScores(int pid, int military, int culture, int money) {
		
		if(money >= 3 && hasNation(pid, "Mali Empire", true)) {
			money = money + 1;
		}
		if(military >= 3 && hasNation(pid, "Mongol Empire", true)) {
			military = military + 1;
		}
		if((military < 0 || culture < 0 || money < 0)) {//if this player lost points
			if(currentPlayer == pid && hasSystem(pid, "Legalism", true)) {//legalism can't lose points
				military = Math.max(military, 0);
				culture = Math.max(culture, 0);
				money = Math.max(money, 0);
			}
			if(currentPlayer != pid && hasSystem(currentPlayer, "Legalism", true)){//legalism doubles opps point loss
				if(military < 0) {military = military * 2;}
				if(culture < 0) {culture = culture * 2;}
				if(money < 0) {money = money * 2;}
			}
		}
		
		if((military < 0 || culture < 0 || money < 0)) {//if this player lost points
			if(money < 0) {
				for(int i = 0; i < 3; i++) {
					if(i != pid && hasNation(i, "Vikings", true)) {
						updateScores(i, 0, 0, 1);
					}
				}
			}
			
			if(military < 0) {
				for(int i = 0; i < 3; i++) {
					if(i != pid && hasNation(i, "Macedonian Empire", true)) {
						updateScores(i, 1, 0, 0);
					}
				}
			}
			
			if(!testing) {
				for(int i= 0; i < 3; i++) {
					if(i != pid) {
						int k = i;
						stack.push(
								new StackEffect(k,
										()->{
											chooseCard(k, 
													"Play Alexander the Great",
													((Card c) -> {return c.getCardName() == "Alexander the Great";}), 
													(int j, String chosenName, GameBoard board) -> {play(chosenName, k);
													return true;}
													);
										}
										));
					}
				}
			}//end Alex
			if(!testing) {
				for(int i = 2; i >= 0; i--) {
					if(i != pid) {
						int k = i;
						stack.push(
								new StackEffect(k,
										()->{
											chooseCard(k, 
													"Play Simon Bolivar",
													((Card c) -> {return c.getCardName() == "Simon Bolivar";}), 
													(int j, String chosenName, GameBoard board) -> {play(chosenName, k);
													return true;}
													);
										}
										));
					}
				}
			}//end Simon
		}
		
		
		if(money > 0 && hasSystem(pid, "Capitalism", true)) {
			money = money + 2;
			culture = culture - 1;
		}
		if(culture > 0 && hasSystem(pid, "Communism", true)) {
			money = money - 1;
			culture = culture + 2;
		}

		scores[pid][0] = scores[pid][0] + military;
		scores[pid][1] = scores[pid][1] + culture;
		scores[pid][2] = scores[pid][2] + money;
		
		//note if this player lost any points
		if((military < 0 || culture < 0 || money < 0) && !testing) {
			lostPointsThisTurn = true;
		}
		
		if(scores[pid][0] > ERA[7]) {scores[pid][0] = ERA[7];}
		if(scores[pid][1] > ERA[7]) {scores[pid][1] = ERA[7];}
		if(scores[pid][2] > ERA[7]) {scores[pid][2] = ERA[7];}

		
		
		/*//make sure no track increases too far if they have democracy
		int lowest = 100;
		for(int i = 0; i < 3; i++) {
			if(scores[pid][i] < lowest) {lowest = scores[pid][i];}
		}
		for(int i = 0; i < 3; i++) {
			if(scores[pid][i] > lowest + 6 && hasSystem(pid, "Democracy", true)) {scores[pid][i] = lowest+6;}
		}*/
		
		//FLASH ICONS ON BOARD IF CHANGE
		if(!testing) {
			if(military > 0) {triggeredPoints[pid][0] = 1;}
			else if(military < 0) {triggeredPoints[pid][0] = -1;}
			if(culture > 0) {triggeredPoints[pid][1] = 1;}
			if(culture < 0) {triggeredPoints[pid][1] = -1;}
			if(money > 0) {triggeredPoints[pid][2] = 1;}
			if(money < 0) {triggeredPoints[pid][2] = 1;}
		}
		
		//check if the player has won
		if(gameOver == false && playerEra[pid] >= 5 && testing == false && 
				scores[pid][0] >= ERA[6] && scores[pid][1] >= ERA[6] && scores[pid][2] >= ERA[6]) {
			gui.updateGUI();
			gameOver = true;
			JOptionPane.showMessageDialog(frame, "The " + playerNations[pid].getCardName() + " wins a New Era victory!", "Victory", JOptionPane.PLAIN_MESSAGE, new ImageIcon(GUI.getImagePath("AuxImages", "NewEraIcon.png")));
			gui.endGame(pid == 0, scoreBoard(0));
		}
		else if(gameOver == false && playerEra[pid] >= 5 && testing == false && 
				scores[pid][0] >= ERA[7]) {
			gui.updateGUI();
			gameOver = true;
			JOptionPane.showMessageDialog(frame, "The " + playerNations[pid].getCardName() + " wins a Military victory!", "Victory", JOptionPane.PLAIN_MESSAGE, new ImageIcon(GUI.getImagePath("AuxImages", "MilitaryIcon.png")));
			gui.endGame(pid == 0, scoreBoard(0));
		}
		else if(gameOver == false && playerEra[pid] >= 5 && testing == false && 
				scores[pid][1] >= ERA[7]) {
			gui.updateGUI();
			gameOver = true;
			JOptionPane.showMessageDialog(frame, "The " + playerNations[pid].getCardName() + " wins a Cultural victory!", "Victory", JOptionPane.PLAIN_MESSAGE, new ImageIcon(GUI.getImagePath("AuxImages", "CultureIcon.png")));
			gui.endGame(pid == 0, scoreBoard(0));
		}
		else if(gameOver == false && playerEra[pid] >= 5 && testing == false && 
				scores[pid][2] >= ERA[7]) {
			gui.updateGUI();
			gameOver = true;
			JOptionPane.showMessageDialog(frame, "The " + playerNations[pid].getCardName() + " wins an Economic victory!", "Victory", JOptionPane.PLAIN_MESSAGE, new ImageIcon(GUI.getImagePath("AuxImages", "EconomyIcon.png")));
			gui.endGame(pid == 0, scoreBoard(0));
		}
		
		//check if the player has reached a new era
		if(playerEra[pid] < 2 && scores[pid][0] >= ERA[2] && scores[pid][1] >= ERA[2]  && scores[pid][2] >= ERA[2]) {
				playerEra[pid] = 2;
				if(!testing) {//alert the user at next GUI update
					alerts.push(
							()->{
								GUI.playSound("Chime.wav");
								JOptionPane.showMessageDialog(frame, "The " + playerNations[pid].getCardName() + " has entered the Post-Classical Era!", "New Era", JOptionPane.PLAIN_MESSAGE, new ImageIcon(GUI.getImagePath("AuxImages", "Era2Icon.png")));
								newEra(pid);
							});
				}
				else {//just run newEra
					newEra(pid);
				}
		}
		if(playerEra[pid] < 3 && scores[pid][0] >= ERA[3] && scores[pid][1] >= ERA[3]  && scores[pid][2] >= ERA[3]) {
			playerEra[pid] = 3;
			if(!testing) {//alert the user at next GUI update
				alerts.push(
						()->{
							GUI.playSound("Chime.wav");
							JOptionPane.showMessageDialog(frame, "The " + playerNations[pid].getCardName() + " has entered the Pre-Modern Era!", "New Era", JOptionPane.PLAIN_MESSAGE, new ImageIcon(GUI.getImagePath("AuxImages", "Era3Icon.png")));
							newEra(pid);
						});
			}
			else {//just run newEra
				newEra(pid);
			}
		}
		if(playerEra[pid] < 4 && scores[pid][0] >= ERA[4] && scores[pid][1] >= ERA[4]  && scores[pid][2] >= ERA[4]) {
			playerEra[pid] = 4;
			if(!testing) {//alert the user at next GUI update
				alerts.push(
						()->{
							GUI.playSound("Chime.wav");
							JOptionPane.showMessageDialog(frame, "The " + playerNations[pid].getCardName() + " has entered the Modern Era!", "New Era", JOptionPane.PLAIN_MESSAGE, new ImageIcon(GUI.getImagePath("AuxImages", "Era4Icon.png")));
							newEra(pid);
						});
			}
			else {//just run newEra
				newEra(pid);
			}
		}
		if(playerEra[pid] < 5 && scores[pid][0] >= ERA[5] && scores[pid][1] >= ERA[5]  && scores[pid][2] >= ERA[5]) {
			playerEra[pid] = 5;
			if(!testing) {//alert the user at next GUI update
				alerts.push(
						()->{
							GUI.playSound("Chime.wav");
							JOptionPane.showMessageDialog(frame, "The " + playerNations[pid].getCardName() + " has entered the Contemporary Era!", "New Era", JOptionPane.PLAIN_MESSAGE, new ImageIcon(GUI.getImagePath("AuxImages", "Era5Icon.png")));
							newEra(pid);
						});
			}
			else {//just run newEra
				newEra(pid);
			}
		}
		
	}
	
	//function to update when players enter a new era
	public void newEra(int pid) {
		
		//handle nation bonuses
		if(playerNations[pid].getEra() == playerEra[pid]) {//if the player has entered the era on their card, give them their bonus
			if(!testing) {
				triggeredNations.add(playerNations[pid]);//flash nation
			}
			updateScores(pid, playerNations[pid].getMilitary(), playerNations[pid].getCulture(), playerNations[pid].getMoney());
			
		}
		
		//Russian Empire
		for(int i = 0; i < 3; i++) {
			if(i != pid && playerEra[i] < playerEra[pid] && hasNation(i, "Russian Empire", true)) {
				int maxType = 0;
				for(int k =0; k < 3; k++) {
					if(scores[pid][k] > scores[pid][maxType]) {
						maxType = k;
					}
				}
				if(maxType == 0) {updateScores(i, 1, 0, 0);}
				else if(maxType == 1) {updateScores(i, 0, 1, 0);}
				else if(maxType == 2) {updateScores(i, 0, 0, 1);}
			}
		}
		
		//Osman Bey
		if(osmanBeyPlayed[pid] > 0) {
			updateScores(pid, osmanBeyPlayed[pid], 0, osmanBeyPlayed[pid]);
			osmanBeyPlayed[pid] = 0;
		}
		
		
	}
	
	
	//function to check if a player has a named system on their board
	
	//return true if player pid has the named system, false otherwise
	//trigger indicates whether the card should blink on the board
	public boolean hasSystem(int pid, String cardName, boolean trigger) {
		LinkedList<Card> playerBoard = playerBoards.get(pid);
		for(int i = 0; i < playerBoard.size(); i++) {
			if(playerBoard.get(i).getCardName() == cardName) {
				if(trigger && !testing) {triggered.add(playerBoard.get(i));}
				return true;
			}
		}
		return false;
	}
	
	//true if has at least one system
	public boolean hasASystem(int pid) {
		LinkedList<Card> playerBoard = playerBoards.get(pid);
		for(int i = 0; i < playerBoard.size(); i++) {
			if(playerBoard.get(i).getType() == Card.Type.System) {
				return true;
			}
		}
		return false;
	}
	
	public boolean hasCard(int pid, String cardName) {
		LinkedList<Card> playerHand = playerHands.get(pid);
		for(int i = 0; i < playerHand.size(); i++) {
			if(playerHand.get(i).getCardName() == cardName) {

				return true;
			}
		}
		return false;
	}
	
	public boolean hasNation(int pid, String cardName, boolean trigger) {
		if(playerNations[pid].getCardName() == cardName) {
			if(trigger && !testing) {
				System.out.println(cardName + "triggered!");
				triggeredNations.add(playerNations[pid]);}
			return true;
		}
		return false;
	}
	
	//function to determine is a player has a certain subtype of system in front of them
	public boolean hasSubtype(int pid, Card.subType subtype) {
		for(int i = 0; i < playerBoards.get(pid).size(); i++) {
			if(playerBoards.get(pid).get(i).getSubtype() == subtype) {
				return true;
			}
		}
		return false;
	}
	
	//return a count of the religions on the board
	public int countReligions() {
		int count = 0;
		for(int i = 0; i < 3; i++) {
			for(int j = 0; j < playerBoards.get(i).size(); j++) {
				if(playerBoards.get(i).get(j).getSubtype() == Card.subType.Religion) {
					count++;
				}
			}
		}
		return count;
	}
	
	//function to get user to distribute their n gold points
	public void distributeGold(int pid, int n, String sender) {
		int[] choices = {0,0,0};//array to hold user split points
		if(hasNation(pid, "Duchy of Venice", true)) {
			n +=1;
		}
		int k = n;
		if(pid == 0) {//give options to player
			stack.push( 
					new StackEffect(pid,
							() -> {
								chooseScore(pid, 
										sender + ": Spend your Gold Point",
										((int player, int score) -> {return (player == pid);}), 
										(int i, int chosenPlayer, int chosenScore, GameBoard board) -> {
											int[] updates = {0,0,0};
											updates[chosenScore] = 1;
											updateScores(i, updates[0], updates[1], updates[2]);
											return true;},
										k
										);
							}));
		}
		else {//split randomly for AI
			stack.push(
					new StackEffect(pid,
							()-> {
								for(int i = 0; i < k; i++) {
									//TODO: choose lowest track?
									int choice = spendGoldAI(pid);
									int[] AIChoices = {0,0,0};
									AIChoices[choice] = 1;
									updateScores(pid, AIChoices[0], AIChoices[1], AIChoices[2]);
								}
							}));
		}

	}

	//return pid of player with the most of a specific resource: 0 = mil, 1 = culture, 2 = money
	public int hasMost(int type) {
		for(int pid = 0 ; pid < 3; pid++) {
			boolean max = true;
			for(int i = 0; i < 3; i++) {
				if(pid != i && scores[pid][type] <= scores[i][type]) {//if another player has equal or more
					max = false;
				}
			}
			if(max) {return pid;}
		}
		return -1;
	}
	
	//return pid of player with the least of a specific resource: 0 = mil, 1 = culture, 2 = money
		public int hasLeast(int type) {
			for(int pid = 0 ; pid < 3; pid++) {
				boolean min = true;
				for(int i = 0; i < 3; i++) {
					if(pid != i && scores[pid][type] >= scores[i][type]) {//if another player has equal or more
						min = false;
					}
				}
				if(min) {return pid;}
			}
			return -1;
		}
	
	
	//give a player, get a list of cards of a given subtype in their hand
	public LinkedList<String> getSubtypes(int pid, Card.subType subtype){
		LinkedList<Card> playerHand = playerHands.get(pid);
		LinkedList<String>cardNames = new LinkedList<String>();
		for(int i = 0; i < playerHand.size(); i++) {
			if(playerHand.get(i).isSubtype(subtype)) {
				cardNames.add(playerHand.get(i).getCardName());
			}
		}
		return cardNames;
	}
	
	//give a player, get a list of cards of a given type in their hand
	public LinkedList<String> getTypes(int pid, Card.Type type){
		LinkedList<Card> playerHand = playerHands.get(pid);
		LinkedList<String>cardNames = new LinkedList<String>();
		for(int i = 0; i < playerHand.size(); i++) {
			if(playerHand.get(i).getType() == type) {
				cardNames.add(playerHand.get(i).getCardName());
			}
		}
		return cardNames;
	}

	public Card getCardFromHand(int pid, String cardName) {
		for(int i = 0; i < playerHands.get(pid).size(); i++) {
			if(playerHands.get(pid).get(i).getCardName() == cardName) {
				return playerHands.get(pid).get(i);
			}
		}
		return null;
	}
	
	public Card getCardFromBoard(int pid, String cardName) {
		for(int i = 0; i < playerBoards.get(pid).size(); i++) {
			if(playerBoards.get(pid).get(i).getCardName() == cardName) {
				return playerBoards.get(pid).get(i);
			}
		}
		return null;
	}
	
	
	
	public String getImagePath(String folder, String name) {
		return "C:\\Users\\elisa\\eclipse-workspace\\Empire World History\\images\\" + folder + "\\" + name;
	}
	
	public void printGameState() {
		//loop through players and print hands and boards
		for(int pid = 0; pid <= 2; pid++) {
			System.out.println("Player " + pid + " Hand:");
			for(int i = 0; i < playerHands.get(pid).size(); i++) {
				System.out.println(playerHands.get(pid).get(i).getCardName());
			}
			System.out.println("Player " + pid + " Board:");
			for(int i = 0; i < playerBoards.get(pid).size(); i++) {
				System.out.println(playerBoards.get(pid).get(i).getCardName());
			}
		}
	}
	
	public void chooseCardFromDiscard(int pid, int era) {
		LinkedList<Card> discardPile = EraDecks[era].discardPile;
		LinkedList<String> options = new LinkedList<String>();
		for(int i = 0; i < discardPile.size(); i++) {
			options.add(discardPile.get(i).getCardName());
		}
		Object[] possibles = options.toArray();
		String res = (String) JOptionPane.showInputDialog(null, "Choose a Card:", "Caste System", JOptionPane.PLAIN_MESSAGE, null, possibles, possibles[0]);
		
		for(int i = 0; i < possibles.length; i++) {
			if(possibles[i] == res) {//if this is chosen card
				Card chosenCard = discardPile.get(i);
				discardPile.remove(i);
				playerHands.get(pid).add(chosenCard);
			}
		}
		
	}
	
	public boolean playable (int pid, Card card) {
		if(card.getSubtype() == Card.subType.Movement && hasNation(pid, "Ottoman Empire", false)) {
			return false;
		}
		return card.playable.test(pid);
	}
	
	
	
	private LinkedList<Card> CreateEra1Cards() {
		LinkedList<Card> Cards1 = new LinkedList<Card>();
		Cards1.add( new Card("The Twelve Tables of Rome", 1, Card.Type.Event, Card.subType.Proclamation, 0,1,0) );
		
		Cards1.add( new Card("The Neolithic Revolution", 1, Card.Type.Event, Card.subType.Discovery, -1,0,2) );
		
		Cards1.add( new Card("Alexander the Great", 1, Card.Type.Leader, Card.subType.General, 1,0,0) ); //elsewhere code: play when opp loses points
		
		Cards1.add( new Card("Aristotle", 1, Card.Type.Leader, Card.subType.Philosopher, 0,1,0, 
				(int pid) -> {chooseCard(pid, 
							"Aristotle: Play a Discovery",
							((Card c) -> c.getSubtype() == Card.subType.Discovery), 
							(int i, String chosenName, GameBoard board) -> {play(chosenName, pid);
																		return true;}
							);}
					) );
		
		Cards1.add( new Card("Augustus", 1, Card.Type.Leader, Card.subType.Ruler, 0,0,1) );
		
		Cards1.add( new Card("Bantu Migrations", 1, Card.Type.Event, Card.subType.Movement, 0,0,1,
				(int pid) -> {
					int handSize = playerHands.get(pid).size();
					
					stack.push( 
							new StackEffect(pid, 
									()->{
										drawCards(pid, "Bantu Migrations", (int i) -> {return i <= playerEra[pid];}, handSize);
									}
									)							
							);
					
					stack.push( 
							new StackEffect(pid, 
									()->{
										discardCards(pid, "Bantu Migrations", handSize);
									}
									)							
							);
					}
				));
		
		Cards1.add( new Card("Buddhism", 1, Card.Type.System, Card.subType.Religion, 0,0,0) );
		
		Cards1.add( new Card("Bureaucracy", 1, Card.Type.System, Card.subType.Political, 0,0,0) );
		
		Cards1.add( new Card("Caste System", 1, Card.Type.System, Card.subType.Social, 0,0,0) );
		
		Cards1.add( new Card("Catholic Christianity", 1, Card.Type.System, Card.subType.Religion, 0,0,0) );
		
		Cards1.add( new Card("Chandragupta Maurya", 1, Card.Type.Leader, Card.subType.Ruler, 0,0,1) );
		
		Cards1.add( new Card("The Code of Hammurabi", 1, Card.Type.Event, Card.subType.Proclamation, 0,1,0) );
		
		Cards1.add( new Card("Confucianism", 1, Card.Type.System, Card.subType.Social, 0,0,0) );
		
		Cards1.add( new Card("Constantine the Great", 1, Card.Type.Leader, Card.subType.Ruler, 0,0,1, 
				(int pid) -> {chooseCard(pid, 
							"Constantine: Play a Religion",
							((Card c) -> c.getSubtype() == Card.subType.Religion), 
							(int i, String chosenName, GameBoard board) -> {play(chosenName, pid);
																		return true;}
							);}
					) );
		
		Cards1.add( new Card("Cuneiform", 1, Card.Type.Event, Card.subType.Discovery, 0,1,0, 
				(int pid) -> {choosePlayer(pid, 
							"Trade",
							"Cuneiform: Trade with an Opponent",
							(i -> i != pid), 
							(int i, int chosenPid, GameBoard board) -> {
																	board.updateScores(pid, 0, 0, 1); //both get +1 money
																	board.updateScores(chosenPid, 0, 0, 1);
																	return true;}, 
							true
							);}
				) );
		
		Cards1.add( new Card("Democracy", 1, Card.Type.System, Card.subType.Political, 0,0,0) );
		
		Cards1.add( new Card("Republicanism", 1, Card.Type.System, Card.subType.Political, 0,0,0) );
		
		Cards1.add( new Card("Diocletian", 1, Card.Type.Leader, Card.subType.Ruler, -1,0,2) );
		
		Cards1.add( new Card("The Edict of Milan", 1, Card.Type.Event, Card.subType.Proclamation, 0,2,0, //play only if religion on board
				(int pid) -> {
					return (hasSubtype(pid, Card.subType.Religion));
				}
				) );
		
		Cards1.add( new Card("Hannibal", 1, Card.Type.Leader, Card.subType.General, 1,0,0) );
		
		Cards1.add( new Card("Hellenism", 1, Card.Type.Event, Card.subType.Movement, 0,0,0,
				(int pid) -> {
					
					drawCards(pid, "Hellenism", (int i)->{return i <= playerEra[pid];}, 1);
					
					int mostCulture = hasMost(1); //+1 money if most culture, +1 culture if most money
					int mostMoney = hasMost(2);
					if(pid == mostCulture) {
						updateScores(pid, 0, 0, 1);
					}
					if(pid == mostMoney) {
						updateScores(pid, 0,1,0);
					}}
				) );
		
		Cards1.add( new Card("Hinduism", 1, Card.Type.System, Card.subType.Religion, 0,0,0) );
		
		Cards1.add( new Card("Judaism", 1, Card.Type.System, Card.subType.Religion, 0,0,0) );
		
		Cards1.add( new Card("Homer", 1, Card.Type.Leader, Card.subType.Artist, 0,1,0) );
		
		Cards1.add( new Card("King Menes", 1, Card.Type.Leader, Card.subType.Ruler, 1,0,0) );
		
		Cards1.add( new Card("Legalism", 1, Card.Type.System, Card.subType.Social, 0,0,0) );
		
		Cards1.add( new Card("Nomadism", 1, Card.Type.System, Card.subType.Social, 0,0,0) );
		
		Cards1.add( new Card("Pax Romana", 1, Card.Type.Event, Card.subType.Entente, 0,0,2, //play only if most military
				(int pid) -> {
					return (hasMost(0) == pid);
				}
				) );
		
		Cards1.add( new Card("Peloponnesian War", 1, Card.Type.Event, Card.subType.Conflict, 2,0,-1) );
		
		Cards1.add( new Card("Pericles", 1, Card.Type.Leader, Card.subType.Philosopher, 0,1,0, 
				(int pid) -> {chooseCard(pid, 
							"Pericles: Play a System",
							((Card c) -> c.getType() == Card.Type.System), 
							(int i, String chosenName, GameBoard board) -> {play(chosenName, pid);
																		return true;}
							);}
					) );
		
		Cards1.add( new Card("Greco-Persian Wars", 1, Card.Type.Event, Card.subType.Conflict, 1,0,0) );
		
		Cards1.add( new Card("Plato", 1, Card.Type.Leader, Card.subType.Philosopher, 0,1,0, 
				(int pid) -> {chooseCard(pid, 
							"Plato: Play a Philosopher",
							((Card c) -> c.isSubtype(Card.subType.Philosopher)), 
							(int i, String chosenName, GameBoard board) -> {play(chosenName, pid);
																		return true;}
							);}
					) );		
		
		Cards1.add( new Card("Polytheism", 1, Card.Type.System, Card.subType.Religion, 0,0,0) );
		
		Cards1.add( new Card("Qin Shi Huang", 1, Card.Type.Leader, Card.subType.Ruler, 2,-1,0) );
		
		Cards1.add( new Card("Queen Hatshepsut", 1, Card.Type.Leader, Card.subType.Ruler, 0,1,0) );
		
		Cards1.add( new Card("Stoicism", 1, Card.Type.System, Card.subType.Social, 0,0,0) );
		
		Cards1.add( new Card("Socrates", 1, Card.Type.Leader, Card.subType.Philosopher, 0,1,0,
				(int pid)-> {
					
					drawCards(pid, "Socrates", (int i) -> {return i <= playerEra[pid];}, 2);

				}				
				) );
		
		Cards1.add( new Card("Wang Mang", 1, Card.Type.Leader, Card.subType.Rebel, 1,0,0, //discard all their systems
				(int pid) ->  {choosePlayer(pid,
								"Attack",
								"Wang Mang: Attack an Opponent",
								(int i) -> {return (i != pid) && hasASystem(i);}, 
								(int i, int j, GameBoard board) -> {
									updateScores(j, 0, -1, 0);
									return true;},
								true
						);}
				));
		
		Cards1.add( new Card("Xiongnu Invasion", 1, Card.Type.Event, Card.subType.Conflict, 1,0,0) );
		
		return Cards1;
	}
	
	
	//code to create the cards for the era 2 deck
	private LinkedList<Card> CreateEra2Cards() {
		LinkedList<Card> Cards2 = new LinkedList<Card>();
		
		Cards2.add( new Card("Bubonic Plauge", 2, Card.Type.Event, Card.subType.None, 0,0,0, 
				(int pid) -> {
					//all players get -1 military
					for(int i = 0; i < 3; i++) {
						updateScores(i, -1, 0, 0);
					}
					
					chooseSystem(pid, 
							"Bubonic Plauge: Renounce a System",
							((Card c) -> c.getType() == Card.Type.System), 
							(int i, String chosenName, GameBoard board) -> {
								updateScores(pid, 2, 0, 0); //+2 mil if discard system
								discardFromBoard(chosenName, pid);
								chooseDeck(pid, 
										"Bubonic Plauge: Choose a Deck", 
										(int k) -> {return k <= 3;}, 
										(int id, int chosenEra, GameBoard board2) ->{
											Card foundSystem = EraDecks[chosenEra].getPSSystem();
											if(foundSystem != null) {
												playerHands.get(pid).add(foundSystem);
												play(foundSystem.getCardName(), pid);
											}
											return true;
										});
								return true;}
							);

				}
				) );

		
		Cards2.add( new Card("Charlemagne", 2, Card.Type.Leader, Card.subType.Ruler, 1,1,1, //each other player draws a card
				(int pid) -> {
					for(int i = 0; i < 3; i++) {
						if(i != pid) {drawCards(i, playerEra[i], 1);}
					}
				}
				) );
		
		Cards2.add( new Card("Charles Martel", 2, Card.Type.Leader, Card.subType.General, 2,0,0) );
		
		Cards2.add( new Card("Code of Justinian", 2, Card.Type.Event, Card.subType.Proclamation, 0,2,0,
				(int pid) -> {
					drawCards(pid, "Code of Justinian", (int i) -> {return i <= playerEra[pid];}, 2);
				}
				) );
		
		Cards2.add( new Card("Emperor Justinian", 2, Card.Type.Leader, Card.subType.Ruler, 0,0,1,
				(int pid) -> {
					chooseCard(pid, 
							"Emperor Justinian: Play a Proclamation",
							((Card c) -> c.getSubtype() == Card.subType.Proclamation), 
							(int i, String chosenName, GameBoard board) -> {play(chosenName, pid);
																			updateScores(pid, 0, 1, 0);
																			return true;}
							);
				}
				) );
		
		Cards2.add( new Card("Feudalism", 2, Card.Type.System, Card.subType.Political, 0,0,0) );
		
		Cards2.add( new Card("Ghenghis Khan", 2, Card.Type.Leader, Card.subType.General, 3,-1,0) );
		
		Cards2.add( new Card("Indian Ocean Trade", 2, Card.Type.Event, Card.subType.Entente, 0,0,2, //extra money for each money-making religion
				(int pid) -> {
					choosePlayer(pid, 
							"Trade",
							"Indian Ocean: Trade with an Opponent",
							(int i) -> {return i != pid && hasSubtype(i, Card.subType.Religion);}, 
							(int i, int chosenPid, GameBoard board) -> {
																	board.updateScores(pid, 0, 0, 1); //both get +2 money
																	board.updateScores(chosenPid, 0, 0, 1);
																	return true;},
							true
							);
				}
				) );
		
		Cards2.add( new Card("Islam", 2, Card.Type.System, Card.subType.Religion, 0,0,0) );
		
		Cards2.add( new Card("Ivan the Terrible", 2, Card.Type.Leader, Card.subType.Ruler, 1,1,0) );
		
		Cards2.add( new Card("King Clovis", 2, Card.Type.Leader, Card.subType.Ruler, 1,1,0) );
		
		Cards2.add( new Card("Kublai Khan", 2, Card.Type.Leader, Card.subType.Ruler, 0,2,0) );
		
		Cards2.add( new Card("Mansa Musa", 2, Card.Type.Leader, Card.subType.Ruler, 0,0,2) );
		
		Cards2.add( new Card("Mohammed", 2, Card.Type.Leader, Card.subType.Cleric, 1,1,0) );
		
		Cards2.add( new Card("Orthodox Christianity", 2, Card.Type.System, Card.subType.Religion, 0,0,0) );
		
		Cards2.add( new Card("Pope Innocent III", 2, Card.Type.Leader, Card.subType.Cleric, 1,0,0,
				/*(int pid) -> {//play only if have religion
					return (hasSubtype(pid, Card.subType.Religion));
				},*/
				(int pid) -> {//get more gold for each player with different religion
					
					choosePlayer(pid, 
							"Attack",
							"Pope Innocent: Attack an Opponent",
							(i -> {return i != pid && hasSubtype(i, Card.subType.Religion);}), 
							(int i, int chosenPid, GameBoard board) -> {
								board.updateScores(pid, 0, 0, 1);
								board.updateScores(chosenPid, 0, 0, -1);
								return true;
							},
							true);
				}
				) );
		
		Cards2.add( new Card("Prince Shotoku", 2, Card.Type.Leader, Card.subType.Ruler, 0,0,0,
				(int pid) -> {
					int mil = 0;
					int cul = 0;
					int eco = 0;
					if(pid != hasMost(0)) {mil=1;}
					if(pid != hasMost(1)) {cul=1;}
					if(pid != hasMost(2)) {eco=1;}
					updateScores(pid, mil, cul, eco);
				}
				) );
		
		Cards2.add( new Card("Saint Cyril", 2, Card.Type.Leader, Card.subType.Cleric, 0,1,0,
				(int pid) -> {
					if(hasSubtype(pid, Card.subType.Religion)) {
					choosePlayer(pid, 
						"Negotiate",
						"Saint Cyril: Negotiate with an Opponent",
						(i -> i != pid), 
						(int i, int chosenPid, GameBoard board) -> {
																board.updateScores(pid, 0, 2, 0);
																board.updateScores(chosenPid, 0, 1, 0);
																return true;},
						true
						);}}
				) );
		
		Cards2.add( new Card("Scholasticism", 2, Card.Type.Event, Card.subType.Movement, 0,1,0,
				(int pid) ->{
					drawCards(pid, "Scholasticism", (int i) -> {return i == 3;}, 2);
				}
				) );
		
		Cards2.add( new Card("Shinto", 2, Card.Type.System, Card.subType.Religion, 0,0,0) );
		
		Cards2.add( new Card("Taika Reforms", 2, Card.Type.Event, Card.subType.Proclamation, 0,0,1,
				(int pid) -> {
					choosePlayer(pid, 
							"Negotiate",
							"Taika Reforms: Negotiate with an Opponent",
							(i -> i != pid), 
							(int i, int chosenPid, GameBoard board) -> {
																		int[] points = new int[3];//array to store points we will add
																		for(int j = 0; j < 3; j++) {
																			if(scores[pid][j] < scores[chosenPid][j]) {
																				points[j] = 1;
																			}
																			else {
																				points[j] = 0;
																			}
																		}
																		updateScores(pid, points[0], points[1], points[2]);
																		return true;
																	},
							true
							);
				}
				) );
		
		Cards2.add( new Card("Tamerlane", 2, Card.Type.Leader, Card.subType.General, 1,1,0) );
		
		Cards2.add( new Card("The Crusades", 2, Card.Type.Event, Card.subType.Conflict, 1,0,0, 
				(int pid) -> {
					choosePlayer(pid, 
							"Attack",
							"Crusades: Attack an Opponent",
							(i -> {return i != pid && hasSubtype(i, Card.subType.Religion);}), 
							(int i, int chosenPid, GameBoard board) -> {
								board.updateScores(pid, 0, 1, 0);
								board.updateScores(chosenPid, -1, 1, 0);
								return true;
							},
							true);}
							) );
		
		Cards2.add( new Card("The Hanseatic League", 2, Card.Type.Event, Card.subType.Entente, 0,0,2,	
				(int pid) -> {choosePlayer(pid, 
						"Trade",
						"Hanseatic League: Trade with an Opponent",
						(i -> i != pid), 
						(int i, int chosenPid, GameBoard board) -> {
																board.updateScores(pid, 0, 0, 1); //both get +2 money
																board.updateScores(chosenPid, 0, 0, 1);
																return true;},
						true
						);}
				) );
		
		Cards2.add( new Card("The Inquisition", 2, Card.Type.Event, Card.subType.Proclamation, 1,0,0,
				(int pid) -> {
					if(hasSubtype(pid, Card.subType.Religion)) {
						updateScores(pid, 2, 0, 0);//+2 mil if has religion
						discardCards(pid, "The Inquisition", 2);
					}
				}
				) );
		
		Cards2.add( new Card("The Magna Carta", 2, Card.Type.Event, Card.subType.Proclamation, 0,1,0,
				(int pid) -> {
					chooseCard(pid, 
							"Magna Carta: Play a Political System",
							((Card c) -> c.getSubtype() == Card.subType.Political), 
							(int i, String chosenName, GameBoard board) -> {
								updateScores(pid, 0, 1, 0);
								play(chosenName, pid);
								return true;}
							);
				}
				) );

		Cards2.add( new Card("The Silk Road", 2, Card.Type.Event, Card.subType.Entente, 0,1,1,
				(int pid) -> {choosePlayer(pid, 
						"Trade",
						"Silk Road: Trade with an Opponent",
						(i -> i != pid), 
						(int i, int chosenPid, GameBoard board) -> {
																board.updateScores(pid, 0, 0, 1); //both get +1 money
																board.updateScores(chosenPid, 0, 0, 1);
																return true;},
						true
						);}
				) );
		
		Cards2.add( new Card("The Treaty of Verdun", 2, Card.Type.Event, Card.subType.Entente, 0,0,0,
				(int pid) -> {
				chooseSystem(pid, 
						"Verdun: Discard a System",
						((Card c) -> c.getType() == Card.Type.System), 
						(int i, String chosenName, GameBoard board) -> {discardFromBoard(chosenName, pid);
																		distributeGold(pid, 3, "Verdun");
																		return true;}
						);
				}
				) );
		
		Cards2.add( new Card("Three Field System", 2, Card.Type.Event, Card.subType.Discovery, 0,0,2) );
		
		Cards2.add( new Card("Vladmir the Great", 2, Card.Type.Leader, Card.subType.Ruler, 1,0,0,
				(int pid) -> {
					chooseCard(pid, 
							"Vladmir the Great: Play a Religion",
							((Card c) -> c.getSubtype() == Card.subType.Religion), 
							(int i, String chosenName, GameBoard board) -> {play(chosenName, pid);
																			updateScores(pid, 0, 1, 0);
																			return true;}
							);
				}
				) );
		
		Cards2.add( new Card("William the Conqueror", 2, Card.Type.Leader, Card.subType.General, 1,0,0,
				(int pid) -> {
					chooseCard(pid, 
							"William the Conqueror: Play a Conflict",
							((Card c) -> c.getSubtype() == Card.subType.Conflict), 
							(int i, String chosenName, GameBoard board) -> {play(chosenName, pid);
																			updateScores(pid, 1, 0, 0);
																			return true;}
							);
				}
				) );
		
		return Cards2;
	}
	
	private LinkedList<Card> CreateEra3Cards() {
		LinkedList<Card> Cards3 = new LinkedList<Card>();
		
		
		Cards3.add( new Card("Akbar", 3, Card.Type.Leader, Card.subType.Ruler, 1,0,0,
				(int pid) -> {//play only if have a religion
					return hasSubtype(pid, Card.subType.Religion);
				},
				(int pid) -> {//get +1 cul for each other player with a religion
					int cul = 0;
					for(int i = 0; i < 3; i++) {
						if(i != pid && hasSubtype(pid, Card.subType.Religion)) {
							cul++;
						}
					}
					updateScores(pid, 0, cul, 0);
				}
				) );
		
		Cards3.add( new Card("Catherine the Great", 3, Card.Type.Leader, Card.subType.Ruler, 3,2,-2) );
		
		Cards3.add( new Card("Charles I", 3, Card.Type.Leader, Card.subType.Ruler, 2,0,0,
				(int pid) -> {
					chooseCard(pid, 
							"Charles I: Play a Conflict",
							((Card c) -> c.getSubtype() == Card.subType.Conflict), 
							(int i, String chosenName, GameBoard board) -> {play(chosenName, pid);
																			return true;}
							);
				}
				) );
		
		Cards3.add( new Card("Christopher Columbus", 3, Card.Type.Leader, Card.subType.Explorer, 0,0,1,
				(int pid) ->{
					choosePlayer(pid, 
							"Attack",
							"Columbus: Attack an Opponent",
							(i -> i != pid), 
							(int i, int chosenPid, GameBoard board) -> {
																	board.updateScores(chosenPid, -3, 0, 0);
																	return true;},
							true
							);
					}	
		));
		
		Cards3.add( new Card("Constitutional Monarchy", 3, Card.Type.System, Card.subType.Political, 0,0,0) );
		
		Cards3.add( new Card("Donatello", 3, Card.Type.Leader, Card.subType.Artist, 0,3,0) );

		Cards3.add( new Card("Elizabeth I", 3, Card.Type.Leader, Card.subType.Ruler, 0,0,1,
				(int pid) -> {
					
					stack.push(
							new StackEffect(pid, false,
									() -> {
										choosePlayer(pid, 
												"Trade",
												"Elizabeth I: Trade with an Opponent",
												(i -> i != pid), 
												(int i, int chosenPid, GameBoard board) -> {
													board.updateScores(i, 0,0,1);
													board.updateScores(chosenPid, 0, 0, 1);
													return true;},
												true
												);

									}
									)
							);

					stack.push(
							new StackEffect(pid, false,
									() -> {
										choosePlayer(pid, 
												"Attack",
												"Elizabeth I: Attack an Opponent",
												(i -> i != pid), 
												(int i, int chosenPid, GameBoard board) -> {
													board.updateScores(chosenPid, -2, 0, 0);
													return true;},
												true
												);
									}
							)
							);
				}) );

		Cards3.add( new Card("Francisco Pizarro", 3, Card.Type.Leader, Card.subType.General, 1,0,0,
				(int pid) -> {
					choosePlayer(pid, 
							"Attack",
							"Pizarro: Attack an Opponent",
							(int i) -> {return (i != pid && scores[i][0] < scores[pid][0]);}, 
							(int i, int chosenPid, GameBoard board) -> {
																	board.updateScores(chosenPid, -1, 0, 0);
																	board.updateScores(pid, 0, 0, 2);
																	return true;},
							true
							);
				}
				
				) );
		
		Cards3.add( new Card("Galileo Galilei", 3, Card.Type.Leader, Card.subType.Scientist, 0,0,0,
				(int pid) -> {
					
					int j = 0;
					while(j < playerBoards.get(pid).size()) {//discard all religions
						if(playerBoards.get(pid).get(j).getSubtype() == Card.subType.Religion  && playerBoards.get(pid).get(j).getCardName() != "Atheism") {
							playerBoards.get(pid).remove(j);
						}
						else {
							j++;
						}
					}

					drawCards(pid, "Galileo", (int i) -> {return i <= playerEra[pid];}, 2);		
					
					distributeGold(pid, 3, "Galileo");//+3 gold

				}
				));

		Cards3.add( new Card("Hernan Cortes", 3, Card.Type.Leader, Card.subType.General, 2,0,0,
				(int pid) -> {
					choosePlayer(pid, 
							"Attack",
							"Cortes: Attack an Opponent",
							(int i) -> {return (i != pid && (scores[i][0] < scores[pid][0]));}, 
							(int i, int chosenPid, GameBoard board) -> {
																	board.updateScores(chosenPid, -1, 0, 0);
																	board.updateScores(pid, 0, 0, 1);
																	return true;},
							true
							);
				}
				
				) );
				
		Cards3.add( new Card("James I", 3, Card.Type.Leader, Card.subType.Ruler, 1,3,1,
				(int pid) -> {
					updateScores(pid, 0, -1 * countReligions(), 0); //-1 culture for each religion on the table
				}
				
				) );
		
		
		Cards3.add( new Card("Jean Jacques Rousseau", 3, Card.Type.Leader, Card.subType.Philosopher, 0,2,0,
				(int pid) -> {
					chooseCard(pid, 
							"Rousseau: Play a Political or Social System",
							((Card c) -> c.getSubtype() == Card.subType.Political || c.getSubtype() == Card.subType.Social), 
							(int i, String chosenName, GameBoard board) -> {//play discovery
								play(chosenName, pid); 
								return true;}
							);
				}		
				) );
		
		Cards3.add( new Card("Johannes Gutenberg", 3, Card.Type.Leader, Card.subType.Scientist, 0,0,2,
				(int pid) -> {
					
					stack.push(
							new StackEffect(pid,
							() -> {
								chooseCard(pid, 
										"Gutenberg: Play a Discovery",
										((Card c) -> c.getSubtype() == Card.subType.Discovery), 
										(int i, String chosenName, GameBoard board) -> {
											play(chosenName, pid);
											return true;}
										);
							}
						));
					
					stack.push(
							new StackEffect(pid,
							() -> {
								drawCards(pid, "Johannes Gutenberg", (int i) -> {return i <= playerEra[pid];}, 1);
							}
						));
					
					
				}			
				) );

		Cards3.add( new Card("John Locke", 3, Card.Type.Leader, Card.subType.Philosopher, 0,3,0,
				(int pid) -> {
					chooseDeck(pid, 
							"Locke: Choose a Deck", 
							(int i) -> {return i <= 3;}, 
							(int id, int chosenEra, GameBoard board) ->{
								Card foundSystem = EraDecks[chosenEra].getPSSystem();
								if(foundSystem != null) {
									playerHands.get(pid).add(foundSystem);
								}
								return true;
							});
				}
				
				) );
		
		Cards3.add( new Card("Leonardo da Vinci", 3, Card.Type.Leader, Card.subType.Artist, 0,0,0,
				(int pid) -> {
					distributeGold(pid, 2, "da Vinci");
				}
				
				) );
		
		Cards3.add( new Card("Louis XIV", 3, Card.Type.Leader, Card.subType.Ruler, -1,4,0) );
		
		Cards3.add( new Card("Michaelangelo", 3, Card.Type.Leader, Card.subType.Artist, 0,3,0) );
		
		Cards3.add( new Card("National Seclusion Policy", 3, Card.Type.Event, Card.subType.Proclamation, 0,2,-1) ); //effect added in play function

		Cards3.add( new Card("Niccolo Machiavelli", 3, Card.Type.Leader, Card.subType.Philosopher, 3,-2,2) );
		
		Cards3.add( new Card("Nicolaus Copernicus", 3, Card.Type.Leader, Card.subType.Scientist, 0,0,0,
				(int pid) -> {
										
					drawCards(pid, "Copernicus", (int i) -> {return i <= playerEra[pid];}, 2);

					distributeGold(pid, 2, "Copernicus");//+1 gold
					
				}
				));
		
		Cards3.add( new Card("Oliver Cromwell", 3, Card.Type.Leader, Card.subType.General, 2,0,0, 
				(int pid) -> {
					//you may discard a system card from in front of you. If you do, play a system
					chooseSystem(pid, 
								"Oliver Cromwell: Renounce a System",
								((Card c) -> c.getType() == Card.Type.System), 
								(int i, String chosenName, GameBoard board) -> {
									updateScores(pid, 2, 0, 0); //+2 mil if discard system
									discardFromBoard(chosenName, pid);
									chooseCard(pid, 
											"Oliver Cromwell: Play a System",
											((Card c) -> c.getType() == Card.Type.System), 
											(int id, String chosenName2, GameBoard board2) -> {
																							play(chosenName2, pid);
																							return true;}
											);
									return true;}
								);
				}
				) );
		
		Cards3.add( new Card("Osman Bey", 3, Card.Type.Leader, Card.subType.General, 1,0,1, 
				(int pid) -> {
					if(!testing) {
						osmanBeyPlayed[pid]++;
					}
				}
				) );
		
		Cards3.add( new Card("Peter the Great", 3, Card.Type.Leader, Card.subType.Ruler, 0,0,0,
				(int pid) -> {
					choosePlayer(pid, 
							"Negotiate",
							"Peter the Great: Negotiate with an Opponent",
							(i -> i != pid), 
							(int i, int chosenPid, GameBoard board) -> {
																		int[] points = new int[3];//array to store points we will add
																		for(int j = 0; j < 3; j++) {
																			if(scores[pid][j] < scores[chosenPid][j]) {
																				points[j] = 1;
																			}
																			else {
																				points[j] = 0;
																			}
																		}
																		updateScores(pid, 2*points[0], 2*points[1], 2*points[2]);
																		return true;
																	},
							true
							);
				}
				) );
		
		Cards3.add( new Card("Pope Leo X", 3, Card.Type.Leader, Card.subType.Cleric, 2,0,0,
				(int pid) -> {
					choosePlayer(pid, 
							"Attack",
							"Pope Leo X: Attack an Opponent",
							(int i) -> {return (i != pid);}, 
							(int i, int chosenPid, GameBoard board) -> {
																	LinkedList<Card> playerBoard = playerBoards.get(chosenPid);
																	int k = 0;
																	while(k < playerBoard.size()) {
																		if(playerBoard.get(k).getSubtype() == Card.subType.Religion) {
																			discardFromBoard(playerBoard.get(k).getCardName(), chosenPid);
																		}
																		else {
																			k++;
																		}
																	}
																	return true;},
							true
							);
				}
				));
		
		Cards3.add( new Card("Prince Henry the Navigator", 3, Card.Type.Leader, Card.subType.Ruler, 2,0,1));
		
		Cards3.add( new Card("Protestant Christianity", 3, Card.Type.System, Card.subType.Religion, 0,0,0));

		Cards3.add( new Card("Sir Isaac Newton", 3, Card.Type.Leader, Card.subType.Scientist, 0,0,0,
				(int pid) -> {							
					drawCards(pid, "Newton", (int i) -> {return i <= playerEra[pid];}, 5);
					
					distributeGold(pid, 1, "Newton");//+2 gold

				}
				
				) );

		
		Cards3.add( new Card("The Commercial Revolution", 3, Card.Type.Event, Card.subType.Movement, 0,0,3));
		
		Cards3.add( new Card("The Counter-Reformation", 3, Card.Type.Event, Card.subType.Movement, 1,0,0));
		
		Cards3.add( new Card("The Edict of Nantes", 3, Card.Type.Event, Card.subType.Proclamation, 0,0,0, 
				(int pid) ->{
					updateScores(pid, 0, countReligions(), 0);
				}
				));
		
		Cards3.add( new Card("The English Bill of Rights", 3, Card.Type.Event, Card.subType.Proclamation, 0,3,0));
		
		Cards3.add( new Card("The English Civil War", 3, Card.Type.Event, Card.subType.Conflict, 4,0,0, 
				(int pid) ->{//play only if has a political system
					return hasSubtype(pid, Card.subType.Political);
				}
				));

		Cards3.add( new Card("The Enlightenment", 3, Card.Type.Event, Card.subType.Movement, 0,0,0, 
				(int pid)->{
					chooseAllCards(pid, 
							"Enlightenment: Play a Philosopher",
							((Card c) -> c.isSubtype(Card.subType.Philosopher)), 
							(int i, String chosenName, GameBoard board) -> {//play philosopher
										play(chosenName, pid);
										return true;}
							);
				}
					
				));

		Cards3.add( new Card("The Peace of Augsburg", 3, Card.Type.Event, Card.subType.Entente, 0,1,0,
				(int pid) -> {
					choosePlayer(pid, 
							"Negotiate",
							"Peace of Augsburg: Negotiate with an Opponent",
							(i -> i != pid), 
							(int i, int chosenPid, GameBoard board) -> {
																		updateScores(pid, 0, countReligions(), 0);
																		updateScores(chosenPid, 0, countReligions(), 0);
																		return true;
																	},
							true
							);
				}
				) );
		
		/*Cards3.add( new Card("The Peace of Westphalia", 3, Card.Type.Event, Card.subType.Entente, 0,1,0,
				(int pid) -> {
					choosePlayer(pid, 
							"Negotiate",
							"Peace of Westphalia: Negotiate with a Player",
							(i -> i != pid), 
							(int i, int chosenPid, GameBoard board) -> {
																		updateScores(pid, 0, countReligions(), 0);
																		updateScores(chosenPid, 0, countReligions(), 0);
																		return true;
																	},
							true
							);
				}
				) );*/
		
		
		Cards3.add( new Card("The Petition of Right", 3, Card.Type.Event, Card.subType.Proclamation, 0,-1,4) );

		Cards3.add( new Card("The Printing Press", 3, Card.Type.Event, Card.subType.Discovery, 0,1,1,
				(int pid) -> {
						chooseCard(pid, 
								"Printing Press: Play a Movement",
								((Card c) -> c.getSubtype() == Card.subType.Movement), 
								(int i, String chosenName, GameBoard board) -> {//play discovery
																			    play(chosenName, pid);
																				return true;}
								);

				}
				
				) );
		
		Cards3.add( new Card("The Protestant Reformation", 3, Card.Type.Event, Card.subType.Movement, 0,2,1,
				(int pid) -> {
					chooseCard(pid, 
							"Protestant Reformation: Play a Religion",
							((Card c) -> c.getSubtype() == Card.subType.Religion), 
							(int i, String chosenName, GameBoard board) -> {//play discovery
																		    play(chosenName, pid);
																			return true;}
							);
				}
				
				) );
		
		Cards3.add( new Card("The Renaissance", 3, Card.Type.Event, Card.subType.Movement, 0,0,0, 
				(int pid)->{
					chooseAllCards(pid, 
							"The Renaissance: Play an Artist or Classical Era Card",
							((Card c) -> {return c.isSubtype(Card.subType.Artist) || c.getEra() == 1;}), 
							(int i, String chosenName, GameBoard board) -> {//play discovery
										play(chosenName, pid);
										return true;}
							);
				}
				));
		
		Cards3.add( new Card("The Scientific Revolution", 3, Card.Type.Event, Card.subType.Movement, 0,0,0, 
				(int pid)->{
					
					stack.push(
							new StackEffect(pid,
							() -> {
								chooseAllCards(pid, 
										"Scientific Revolution: Play a Scientist",
										((Card c) -> c.isSubtype(Card.subType.Scientist)), 
										(int i, String chosenName, GameBoard board) -> {//play discovery
													play(chosenName, pid);
													return true;}
										);
							})
							);				
					
					stack.push(
							new StackEffect(pid,
							() -> {
								if(hasSubtype(pid, Card.subType.Religion)) {
									discardCards(pid, "The Scientific Revolution", 1);
								}
							})
							);
					}
					
				));

		
		Cards3.add( new Card("The Thirty Years War", 3, Card.Type.Event, Card.subType.Conflict, 1,0,-1, 
				(int pid) ->{//play only if has religion
					return hasSubtype(pid, Card.subType.Religion);
				},
				(int pid) ->{//get +1 mil for each religion
					updateScores(pid, countReligions(), 0, 0);
				}
				));
		
		Cards3.add( new Card("The Time of Troubles", 3, Card.Type.Event, Card.subType.Conflict, 2,0,0,
				(int pid) -> {
					choosePlayer(pid, 
							"Attack",
							"Time of Troubles: Attack an Opponent",
							(i -> i != pid && !hasSubtype(i, Card.subType.Political)), 
							(int i, int chosenPid, GameBoard board) -> {
																		updateScores(chosenPid, -1, -1, -1);
																		int k =0;
																		while(k < playerHands.get(chosenPid).size()) {
																			if(playerHands.get(chosenPid).get(k).isSubtype(Card.subType.Ruler)) {
																				discard(playerHands.get(chosenPid).get(k).getCardName(), chosenPid);
																			}
																			else {
																				k++;
																			}
																		}
																		return true;
																	},
							true
							);
				}
				) );
		
		Cards3.add( new Card("The War of Spanish Succession", 3, Card.Type.Event, Card.subType.Conflict, 0,0,0, 
				(int pid) -> {	
					choosePlayer(pid, 
							"Other",
							"Choose a Player to Get Military",
							(i -> {return true;}), 
							(int i, int chosenPid, GameBoard board) -> {
								board.updateScores(chosenPid, 2, 0, -1);
								
								stack.push( new StackEffect(pid,
										() -> {
										choosePlayer(pid, 
												"Other",
												"Choose a Player to Get Economy",
												(j -> j != chosenPid), 
												(int j, int chosenPid2, GameBoard board2) -> {
													board2.updateScores(chosenPid2, 0, 0, 3);
													return true;},
												true
												);
										}
										));
								
								return true;},
							true
							);
				}
				) );
		
		Cards3.add( new Card("Thomas Hobbes", 3, Card.Type.Leader, Card.subType.Philosopher, 2,-1,1,
				(int pid) -> {
					chooseCard(pid, 
							"Hobbes: Play a Political or Social System",
							((Card c) -> c.getSubtype() == Card.subType.Political || c.getSubtype() == Card.subType.Social), 
							(int i, String chosenName, GameBoard board) -> {//play discovery
																		    play(chosenName, pid);
																			return true;}
							);
				}
				
				) );
		
		Cards3.add( new Card("Vasco de Gama", 3, Card.Type.Leader, Card.subType.Explorer, 0,0,2,
				(int pid) -> {choosePlayer(pid, 
						"Trade",
						"Vasco de Gama: Trade with an Opponent",
						(i -> i != pid), 
						(int i, int chosenPid, GameBoard board) -> {
																board.updateScores(pid, 0, 0, 2); //both get +2 money
																board.updateScores(chosenPid, 0, 0, 2);
																return true;},
						true
						);}
				) );
		
		Cards3.add( new Card("Voltaire", 3, Card.Type.Leader, Card.subType.Philosopher, 0,2,0,
				(int pid) -> {
					chooseCard(pid, 
							"Voltaire: Play a Religion",
							((Card c) -> c.getSubtype() == Card.subType.Religion), 
							(int i, String chosenName, GameBoard board) -> {//play religion
																			if(!testing) {voltairePlayed = true;} //don't delete current religions
																		    play(chosenName, pid);
																			return true;}
							);
				}
				
				) );
		
		Cards3.add( new Card("William Shakespeare", 3, Card.Type.Leader, Card.subType.Artist, 0,2,0,
				(int pid) -> {//draw and play top card of classical era deck
					Card drawnCard = EraDecks[1].draw();
					playerHands.get(pid).add(drawnCard);
					play(drawnCard.getCardName(), pid);
				}
				
				) );
		
		Cards3.add( new Card("Zheng He", 3, Card.Type.Leader, Card.subType.Explorer, 0,0,3,
				(int pid) -> {
					
					choosePlayer(pid, 
							"Trade",
							"Zheng He: Trade With an Opponent",
							(i -> i != pid), 
							(int i, int chosenPid, GameBoard board) -> {
																	board.updateScores(pid, 0, 0, 1); 
																	board.updateScores(chosenPid, 0, 0, 1);
																	return true;},
							true
							);
					
					/*for(int i = 0; i < 3; i++) {
						if(i != pid) {
							updateScores(i, 0, 0, 1);
						}
					}*/
				}
				) );
		
		Cards3.add( new Card("Atheism", 3, Card.Type.System, Card.subType.Religion, 0,0,0) );


		
		return Cards3;
	}
	
	private LinkedList<Card> CreateEra4Cards() {
		LinkedList<Card> Cards4 = new LinkedList<Card>();

		Cards4.add( new Card("Adam Smith", 4, Card.Type.Leader, Card.subType.Philosopher, 0,0,3, 
				(int pid) -> {chooseCard(pid, 
							"Adam Smith: Play an Economic System",
							((Card c) -> c.getSubtype() == Card.subType.Economic), 
							(int i, String chosenName, GameBoard board) -> {play(chosenName, pid);
																		return true;}
							);}
					) );
		
		Cards4.add( new Card("Capitalism", 4, Card.Type.System, Card.subType.Economic, 0,0,0) );
		
		Cards4.add( new Card("Charles Darwin", 4, Card.Type.Leader, Card.subType.Scientist, 0,0,0,
				(int pid) -> {
					
					int j = 0;
					while(j < playerBoards.get(pid).size()) {//discard all religions
						if(playerBoards.get(pid).get(j).getSubtype() == Card.subType.Religion && playerBoards.get(pid).get(j).getCardName() != "Atheism") {
							playerBoards.get(pid).remove(j);
						}
						else {
							j++;
						}
					}
					drawCards(pid, "Darwin", (int i) -> {return i <= playerEra[pid];}, 3);
					distributeGold(pid, 3, "Darwin");//+2 gold
				}
				));
		
		Cards4.add( new Card("Commodore Matthew Perry", 4, Card.Type.Leader, Card.subType.Explorer, 2,0,1,
				(int pid) -> {choosePlayer(pid, 
						"Trade",
						"Perry: Trade with an Opponent",
						(i -> i != pid && scores[i][0] < scores[pid][0]), 
						(int i, int chosenPid, GameBoard board) -> {
																board.updateScores(pid, 0, 0, 2); //you get +1 money
																board.updateScores(chosenPid, 0, 0, 1);
																return true;},
						true
						);}
				) );
		
		Cards4.add( new Card("Communism", 4, Card.Type.System, Card.subType.Economic, 0,0,0) );

		Cards4.add( new Card("Democracy", 4, Card.Type.System, Card.subType.Political, 0,0,0) );

		Cards4.add( new Card("Eli Whitney", 4, Card.Type.Leader, Card.subType.Scientist, 0,0,3, 
				(int pid) -> {chooseCard(pid, 
							"Eli Whitney: Play a Discovery",
							((Card c) -> c.getSubtype() == Card.subType.Discovery), 
							(int i, String chosenName, GameBoard board) -> {play(chosenName, pid);
																		return true;}
							);}
					) );
		
		Cards4.add( new Card("The Industrial Revolution", 4, Card.Type.Event, Card.subType.Discovery, 0,0,0, 
				(int pid) -> {//each opp gets +1 money, +1 mil
					for(int i = 0; i < 3; i++) {
						if(i != pid) {updateScores(i, 1, 0, 1);}
					}
					int[] choices = {0,0,0};//array to hold user split points
					if(pid == 0) {//give options to player

						chooseScore(pid, 
								"Industrial Revolution: Spend your Gold Point",
								((int player, int score) -> {return (player == pid && score != 1);}), 
								(int i, int chosenPlayer, int chosenScore, GameBoard board) -> {
									int[] updates = {0,0,0};
									updates[chosenScore] = 1;
									updateScores(i, updates[0], updates[1], updates[2]);
									return true;},
								5
								);
					}

					else {//split randomly for AI
						for(int i = 0; i < 4; i++) {
							//TODO: choose lowest track?
							int choice = rand.nextInt(2);
							if(choice == 0) {choices[0] += 1;}
							else {choices[2]+=1;}
						}
						updateScores(pid, choices[0], choices[1], choices[2]);
					}
				}
				) );
		
		Cards4.add( new Card("Karl Marx", 4, Card.Type.Leader, Card.subType.Philosopher, 0,3,0, 
				(int pid) -> {
					chooseCard(pid, 
							"Karl Marx: Play an Economic System",
							((Card c) -> c.getSubtype() == Card.subType.Economic), 
							(int i, String chosenName, GameBoard board) -> {play(chosenName, pid);
																		return true;}
							);
					}
					) );
		
		Cards4.add( new Card("Maximilien Robespierre", 4, Card.Type.Leader, Card.subType.Ruler, 6,-2,0) );

		Cards4.add( new Card("Muhammad Ali", 4, Card.Type.Leader, Card.subType.General, 1,0,3) );

		Cards4.add( new Card("Napoleon Bonaparte", 4, Card.Type.Leader, Card.subType.General, 4,0,0) );

		Cards4.add( new Card("Otto von Bismark", 4, Card.Type.Leader, Card.subType.Ruler, 2,0,0,
				(int pid) -> {
					chooseCard(pid, 
							"Bismark: Play a Conflict",
							((Card c) -> c.getSubtype() == Card.subType.Conflict), 
							(int i, String chosenName, GameBoard board) -> {play(chosenName, pid);
																			updateScores(pid, 0,1,0);
																		return true;}
							);
				}
				) );
		
		Cards4.add( new Card("Pierre Toussaint L'Ouverture", 4, Card.Type.Leader, Card.subType.Rebel, 5,0,0,
				(int pid) -> {//play only if least of a type of point
					return (hasLeast(0) == pid || hasLeast(1) == pid || hasLeast(2) == pid);
				}
				) );
		
		Cards4.add( new Card("Simon Bolivar", 4, Card.Type.Leader, Card.subType.Rebel, 3,0,2) );//elsewhere code
		
		Cards4.add( new Card("The Boer Wars", 4, Card.Type.Event, Card.subType.Conflict, 0,0,2,
				(int pid) -> {choosePlayer(pid, 
						"Attack",
						"Boer Wars: Attack an Opponent",
						(i -> i != pid && scores[i][0] < scores[pid][0]), 
						(int i, int chosenPid, GameBoard board) -> {
																board.updateScores(pid, 0, 0, 2); //you get +1 money
																board.updateScores(chosenPid, 0, 0, -2);
																return true;},
						true
						);}
				) );

		Cards4.add( new Card("The Boxer Rebellion", 4, Card.Type.Event, Card.subType.Conflict, 3,0,0,
				(int pid) -> {//play only if not most military
					return !(pid == hasMost(0));
				},
				(int pid) -> {//each player with more military gets -2
					for(int i = 0; i < 3; i++) {
						if(scores[i][0] > scores[pid][0]) {updateScores(i, -2, 0, 0);}
					}
				}
				) );
		
		Cards4.add( new Card("The Chinese Exclusion Act", 4, Card.Type.Event, Card.subType.Proclamation, 0,5,-1,
				(int pid) -> {
					discardCards(pid, "Exclusion Act", 2);
				}
				) );
		
		Cards4.add( new Card("The Congress of Vienna", 4, Card.Type.Event, Card.subType.Entente, 1,1,1) );
		
		Cards4.add( new Card("The Cotton Gin", 4, Card.Type.Event, Card.subType.Discovery, 0,0,4) );
		
		Cards4.add( new Card("The Declaration of Independence", 4, Card.Type.Event, Card.subType.Proclamation, 0,3,0, 
				(int pid) -> {
					chooseCard(pid, 
							"Declaration of Independence: Play a Political System",
							((Card c) -> c.getSubtype() == Card.subType.Political), 
							(int i, String chosenName, GameBoard board) -> {play(chosenName, pid);
																		return true;}
							);
					}
					) );
		
		Cards4.add( new Card("The Declaration of the Rights of Man", 4, Card.Type.Event, Card.subType.Proclamation, 0,4,0) );

		Cards4.add( new Card("The Emancipation Edict", 4, Card.Type.Event, Card.subType.Proclamation, 1,2,1) );

		Cards4.add( new Card("The Franco-Prussian War", 4, Card.Type.Event, Card.subType.Conflict, 2,2,0) );

		Cards4.add( new Card("The French and Indian War", 4, Card.Type.Event, Card.subType.Conflict, 3,0,0,
				(int pid) -> {choosePlayer(pid, 
						"Attack",
						"French and Indian War: Attack an Opponent",
						(i -> i != pid && scores[i][0] < scores[pid][0]), 
						(int i, int chosenPid, GameBoard board) -> {
																board.updateScores(chosenPid, 0, 0, -1);
																return true;},
						true
						);}
				) );
		
		Cards4.add( new Card("The Meiji Restoration", 4, Card.Type.Event, Card.subType.Proclamation, 1,0,2, 
				(int pid) -> {
					chooseCard(pid, 
							"Meiji Restoration: Play a Ruler",
							((Card c) -> c.isSubtype(Card.subType.Ruler)), 
							(int i, String chosenName, GameBoard board) -> {play(chosenName, pid);
																		return true;}
							);
					}
					) );

		Cards4.add( new Card("The Monroe Doctrine", 4, Card.Type.Event, Card.subType.Proclamation, 0,0,4, 
				(int pid) -> {
					Card monroeDoctrine = getCardFromHand(pid, "The Monroe Doctrine");
					monroeDoctrine.setPlayer(pid);
					choosePlayer(pid, 
							"Other",
							"The Monroe Doctrine: Choose an Opponent",
							(i -> i != pid), 
							(int i, int chosenPid, GameBoard board) -> {
								int index = -1;
								for(int j = 0; j < playerHands.get(pid).size(); j++) {
									if(playerHands.get(pid).get(j).getCardName() == "The Monroe Doctrine") {
										index = j;
									}
								}
								playerBoards.get(chosenPid).add(playerHands.get(i).get(index));
								playerHands.get(i).remove(index);
								return true;},
							true
							);}
				));
		
		Cards4.add( new Card("The Napoleonic Code", 4, Card.Type.Event, Card.subType.Proclamation, 0,5,0,
				(int pid) -> {
					discardCards(pid, "Napoleonic Code", 3);
				}
				));

		Cards4.add( new Card("The Opium War", 4, Card.Type.Event, Card.subType.Conflict, 1,0,1,
				(int pid) -> {choosePlayer(pid, 
						"Attack",
						"Opium War: Attack an Opponent",
						(i -> i != pid && scores[i][0] < scores[pid][0]), 
						(int i, int chosenPid, GameBoard board) -> {
																board.updateScores(chosenPid, 0, -2, 0);
																board.updateScores(pid, 0, 0, 2);
																return true;},
						true
						);}
				) );
		
		Cards4.add( new Card("The Panama Canal", 4, Card.Type.Event, Card.subType.Entente, 0,0,3,
				(int pid) -> {choosePlayer(pid, 
						"Trade",
						"Panama Canal: Trade With an Opponent",
						(i -> i != pid && scores[i][0] < scores[pid][0]), 
						(int i, int chosenPid, GameBoard board) -> {
																board.updateScores(chosenPid, 0, 0, 1);
																board.updateScores(pid, 0, 0, 2);
																return true;},
						true
						);}
				) );
		
		Cards4.add( new Card("The Self-Strengthening Movement", 4, Card.Type.Event, Card.subType.Movement, 5,0,0,
				(int pid) -> {//play only if not most military
					return !(pid == hasMost(0));
				}
				) );
		
		Cards4.add( new Card("The Sino-Japanese War", 4, Card.Type.Event, Card.subType.Conflict, 3,0,0,
				(int pid) -> {choosePlayer(pid, 
						"Attack",
						"Sino-Japanese War: Attack an Opponent",
						(i -> i != pid && scores[i][0] < scores[pid][0]), 
						(int i, int chosenPid, GameBoard board) -> {
																board.updateScores(chosenPid, 0, 0, -1);
																board.updateScores(pid, 0, 0, 1);
																return true;},
						true
						);}
				) );
		
		Cards4.add( new Card("The Spanish-American War", 4, Card.Type.Event, Card.subType.Conflict, 3,0,0,
				(int pid) -> {
					int highest = hasMost(0);
					int lowest = hasLeast(0);
					if(highest >= 0) {updateScores(highest, 1, 0, 0);}
					if(lowest >= 0) {updateScores(lowest, 1, 0, 0);}
				}
				) );
		
		Cards4.add( new Card("The Suez Canal", 4, Card.Type.Event, Card.subType.Entente, 0,0,3,
				(int pid) -> {choosePlayer(pid, 
						"Trade",
						"Suez Canal: Trade With an Opponent",
						(i -> i != pid && scores[i][2] < scores[pid][2]), 
						(int i, int chosenPid, GameBoard board) -> {
																board.updateScores(chosenPid, 0, -1, 2);
																board.updateScores(pid, 0, 1, 1);
																return true;},
						true
						);}
				) );
		
		Cards4.add( new Card("The Treaty of Cordoba", 4, Card.Type.Event, Card.subType.Entente, 0,3,0,
				(int pid) -> {
					stack.push(
							new StackEffect(pid,
									() -> {
										drawCards(pid, "Treaty of Cordoba", (int i) -> {return i <= playerEra[pid];}, 2);
									}
									)
							);
					
					stack.push(
							new StackEffect(pid,
									() -> {
										choosePlayer(pid, 
												"Negotiate",
												"Cordoba: Negotiate With an Opponent",
												(i -> i != pid && scores[i][0] > scores[pid][0]), 
												(int i, int chosenPid, GameBoard board) -> {
																						board.updateScores(chosenPid, 0, 1, 0);
																						board.updateScores(pid, 0, 1, 0);
																						return true;},
												true
												);
									}
									)
							);
					
				}
				) );
		
		Cards4.add( new Card("Thomas Paine", 4, Card.Type.Leader, Card.subType.Philosopher, 0,3,0, 
				(int pid) -> {
					chooseCard(pid, 
							"Thomas Paine: Play a Proclamation",
							((Card c) -> c.getSubtype() == Card.subType.Proclamation), 
							(int i, String chosenName, GameBoard board) -> {play(chosenName, pid);
																		return true;}
							);
					}
					) );

		return Cards4;
	}
	
	private LinkedList<Card> CreateEra5Cards() {
		LinkedList<Card> Cards5 = new LinkedList<Card>();

		Cards5.add( new Card("Adolf Hitler", 5, Card.Type.Leader, Card.subType.General, 8,-3,0) );

		Cards5.add( new Card("Arab-Israeli War", 5, Card.Type.Event, Card.subType.Conflict, 2,0,2) );
		
		Cards5.add( new Card("Ayatollah Khomeini", 5, Card.Type.Leader, Card.subType.Cleric, 0,5,0,
				(int pid) -> {//play only if religion
					return hasSubtype(pid, Card.subType.Religion);
				},
				(int pid) -> {choosePlayer(pid, 
						"Attack",
						"Khomeini: Attack an Opponent",
						(i -> i != pid && scores[i][0] > scores[pid][0]), 
						(int i, int chosenPid, GameBoard board) -> {
																board.updateScores(chosenPid, 0, -2, 0);
																return true;},
						true
						);}
				) );
		
		Cards5.add( new Card("Chiang Kai-shek", 5, Card.Type.Leader, Card.subType.Ruler, -1,3,4,
				(int pid) -> {//play only if not communism
					for(int i = 0; i < playerBoards.get(pid).size(); i++) {
						if(playerBoards.get(pid).get(i).getCardName() == "Communism") {
							return false;
						}
					}
					return true;
				}
				) );
		
		Cards5.add( new Card("Cold War", 5, Card.Type.Event, Card.subType.Conflict, 4,0,0,
				(int pid) -> {//each player with economic system gets +2 mil
					for(int i = 0; i < 3; i++) {
						if(hasSubtype(i, Card.subType.Economic)) {
							updateScores(i, 2, 0, 0);
						}
					}
				}
				) );
		
		Cards5.add( new Card("Czar Nicholas II", 5, Card.Type.Leader, Card.subType.Ruler, 2,-1,-1, 
				(int pid) -> {
					stack.push(
							new StackEffect(pid,
							() -> {
								chooseCard(pid, 
										"Nicholas II: Play a Conflict",
										((Card c) -> c.getSubtype() == Card.subType.Conflict), 
										(int i, String chosenName, GameBoard board) -> {play(chosenName, pid);
																					return true;}
										);
							}
							));
					
								chooseCard(pid, 
										"Nicholas II: Play a Rebel",
										((Card c) -> c.isSubtype(Card.subType.Rebel)), 
										(int i, String chosenName, GameBoard board) -> {play(chosenName, pid);
																					return true;}
										);
							}

					) );
		
		Cards5.add( new Card("Fascism", 5, Card.Type.System, Card.subType.Political, 0,0,0) );
		
		Cards5.add( new Card("Fidel Castro", 5, Card.Type.Leader, Card.subType.Rebel, 2,3,0) );
		
		Cards5.add( new Card("Francisco Franco", 5, Card.Type.Leader, Card.subType.General, 6,-1,0) );

		Cards5.add( new Card("Franklin Roosevelt", 5, Card.Type.Leader, Card.subType.Ruler, 0,2,4,
				(int pid) -> {//play only if not most money
					return (pid != hasMost(2));
				}
				) );
		
		Cards5.add( new Card("Gamal Nasser", 5, Card.Type.Leader, Card.subType.General, 3,-1,3,
				(int pid) -> {//play only if not most military
					return (pid != hasMost(0));
				}
				) );

		Cards5.add( new Card("Gavrilo Princip", 5, Card.Type.Leader, Card.subType.Rebel, 1,0,0, 
				(int pid) -> {//play conflict and double it
					chooseCard(pid, 
							"Gavrilo Princip: Play a Conflict",
							((Card c) -> c.getSubtype() == Card.subType.Conflict), 
							(int i, String chosenName, GameBoard board) -> {
								Card chosenCard = getCardFromHand(i, chosenName);
								stack.push( 
										new StackEffect(pid,
										() -> {
											playCard(pid, chosenCard);
									}));
								
								play(chosenName, pid);
								return true;}
							);
				}
				) );
		
		Cards5.add( new Card("Ho Chi Minh", 5, Card.Type.Leader, Card.subType.Rebel, 3,2,0,
				(int pid) -> {//play only if not most military
					return (pid != hasMost(0));
				}
				) );
		
		Cards5.add( new Card("Iranian Revolution", 5, Card.Type.Event, Card.subType.Conflict, 1,3,0,
				(int pid) -> {choosePlayer(pid, 
						"Attack",
						"Iranian Revolution: Attack an Opponent",
						(i -> i != pid && scores[i][0] > scores[pid][0]), 
						(int i, int chosenPid, GameBoard board) -> {
																board.updateScores(chosenPid, 0, -3, 0);
																return true;},
						true
						);}
				) );
		
		Cards5.add( new Card("Josef Stalin", 5, Card.Type.Leader, Card.subType.Ruler, 4,-2,3) );
		
		Cards5.add( new Card("Leon Trotsky", 5, Card.Type.Leader, Card.subType.Rebel, 4,0,0,
				(int pid) -> {//play only if not most military
					if(pid == hasMost(1)) {
						updateScores(pid, 2, 0, 0);
					}
				}
				) );
		
		Cards5.add( new Card("Mao Zedong", 5, Card.Type.Leader, Card.subType.Rebel, 4,3,-2,
				(int pid) -> {
					discardCards(pid, "Mao", 3);
				}
				) );

		Cards5.add( new Card("Mikhail Gorbachev", 5, Card.Type.Leader, Card.subType.Ruler, 0,4,0,
				(int pid) -> {choosePlayer(pid, 
						"Negotiate",
						"Gorbachev: Negotiate With an Opponent",
						(i -> i != pid), 
						(int i, int chosenPid, GameBoard board) -> {
							board.updateScores(pid, 0, 2, 0);
							board.updateScores(chosenPid, 0, 2, 0);
							return true;},
						true
						);}
				) );
		
		Cards5.add( new Card("Mohandas Ghandi", 5, Card.Type.Leader, Card.subType.Rebel, -2,7,0,
				(int pid) -> {//play only if not most military
					return (pid != hasMost(0));
				}
				) );
		
		Cards5.add( new Card("Muhammed Ali Jinnah", 5, Card.Type.Leader, Card.subType.Cleric, 0,6,0,
				(int pid) -> {//play only if have religion
					return hasSubtype(pid, Card.subType.Religion);
				}
				) );
		
		Cards5.add( new Card("Munich Conference", 5, Card.Type.Event, Card.subType.Entente, 0,3,3,
				(int pid) -> {
					for(int i =0; i < 3; i++) {//each opponent gets +1 mil
						if(i != pid) {
							updateScores(i, 2, 0, 0);
						}
					}
				}
				) );
		
		Cards5.add( new Card("Mustafa Kemal Ataturk", 5, Card.Type.Leader, Card.subType.Ruler, 0,0,0,
				(int pid) -> {
					
					int k = 0;
					while(k < playerBoards.get(pid).size()) {//discard all religions
						if(playerBoards.get(pid).get(k).getSubtype() == Card.subType.Religion && playerBoards.get(pid).get(k).getCardName() != "Atheism") {
							playerBoards.get(pid).remove(k);
						}
						else {
							k++;
						}
					}
					
					choosePlayer(pid, 
							"Negotiate",
							"Ataturk: Negotiate With an Opponent",
							(i -> i != pid), 
							(int i, int chosenPid, GameBoard board) -> {
																		int[] points = new int[3];//array to store points we will add
																		for(int j = 0; j < 3; j++) {
																			if(scores[pid][j] < scores[chosenPid][j]) {
																				points[j] = 1;
																			}
																			else {
																				points[j] = 0;
																			}
																		}
																		updateScores(pid, 4*points[0], 4*points[1], 4*points[2]);
																		return true;
																	},
							true
							);
				}
				) );
		
		Cards5.add( new Card("NAFTA", 5, Card.Type.Event, Card.subType.Entente, 0,0,4,
				(int pid) -> {choosePlayer(pid, 
						"Trade",
						"NAFTA: Trade With an Opponent",
						(i -> i != pid), 
						(int i, int chosenPid, GameBoard board) -> {
							board.updateScores(pid, 0, 0, 2);
							board.updateScores(chosenPid, 0, 0, 2);
							return true;},
						true
						);}
				) );
		
		Cards5.add( new Card("Nelson Mandela", 5, Card.Type.Leader, Card.subType.Rebel, 1,4,0,
				(int pid) -> {
					drawCards(pid, "Mandela", (int i) -> {return i <= playerEra[pid];}, 2);
				}
				) );

		Cards5.add( new Card("Neville Chamberlain", 5, Card.Type.Leader, Card.subType.Ruler, 0,2,3, 
				(int pid) -> {//play entente, each opp gets +1 mil
					chooseCard(pid, 
							"Chamberlain: Play an Entente",
							((Card c) -> c.getSubtype() == Card.subType.Entente), 
							(int i, String chosenName, GameBoard board) -> {
								for(int i1 =0; i1 < 3; i1++) {//each opponent gets +1 mil
									if(i1 != pid) {
										updateScores(i1, 1, 0, 0);
									}
								}
								play(chosenName, pid);
								return true;}
							);
				}
				) );
		
		Cards5.add( new Card("Nonproliferation Treaty", 5, Card.Type.Event, Card.subType.Entente, 0,3,2,
				(int pid) -> {choosePlayer(pid, 
						"Negotiate",
						"Nonproliferation Treaty: Negotiate With an Opponent",
						(i -> i != pid), 
						(int i, int chosenPid, GameBoard board) -> {
							board.updateScores(pid, -4, 0, 0);
							board.updateScores(chosenPid, -4, 0, 0);
							return true;},
						true
						);}
				) );
		
		Cards5.add( new Card("Persian Gulf War", 5, Card.Type.Event, Card.subType.Conflict, 3,0,0,
				(int pid) -> {choosePlayer(pid, 
						"Attack",
						"Persian Gulf War: Attack an Opponent",
						(i -> i != pid && scores[i][2] < scores[pid][2]), 
						(int i, int chosenPid, GameBoard board) -> {
																board.updateScores(chosenPid, -2, 0, -2);
																board.updateScores(pid, 0, 0, 2);
																return true;},
						true
						);}
				) );
		
		Cards5.add( new Card("Saddam Hussein", 5, Card.Type.Leader, Card.subType.Ruler, 2,-2,2,
				(int pid) -> {
					discardCards(pid, "Saddam", 3);
				}
				) );

		Cards5.add( new Card("The Cuban Revolution", 5, Card.Type.Event, Card.subType.Conflict, 2,3,0,
				(int pid) -> {choosePlayer(pid, 
						"Attack",
						"Cuban Revolution: Attack an Opponent",
						(i -> i != pid && scores[i][0] > scores[pid][0]), 
						(int i, int chosenPid, GameBoard board) -> {
																board.updateScores(chosenPid, -2, 0, 0);
																return true;},
						true
						);}
				) );
		
		Cards5.add( new Card("The Cultural Revolution", 5, Card.Type.Event, Card.subType.Movement, 5,-2,0,
				(int pid) -> {
					for(int i =0; i < 3; i++) {//each opponent renounces all system except Communism
						if(i != pid) {
							int j =0;
							while(j < playerBoards.get(i).size()) {//discard all religions
								if(playerBoards.get(i).get(j).getCardName() != "Communism") {
									discardFromBoard(playerBoards.get(i).get(j).getCardName(), i);
								}
								else {
									j++;
								}
							}
						}
					}
				}
				) );
		
		Cards5.add( new Card("The Green Revolution", 5, Card.Type.Event, Card.subType.Discovery, 0,0,7,
				(int pid) -> {
					for(int i =0; i < 3; i++) {//each opponent gets +3 money
						if(i != pid) {
							updateScores(i, 0, 0, 3);
						}
					}
				}
				) );
		
		Cards5.add( new Card("The Manhatten Project", 5, Card.Type.Event, Card.subType.Discovery, 6,0,0,
				(int pid) -> {//play only if most money
					return (pid == hasMost(2));
				}
				) );
		
		Cards5.add( new Card("The Russian Revolution", 5, Card.Type.Event, Card.subType.Conflict, -1,2,0,
				(int pid) -> {//play only if not most money
					return (pid != hasMost(2));
				},
				(int pid) -> {
					chooseAllCards(pid, 
							"Russian Revolution: Play a Rebel",
							((Card c) -> c.isSubtype(Card.subType.Rebel)), 
							(int i, String chosenName, GameBoard board) -> {//play rebel
								play(chosenName, pid);
								return true;}
							);
				}
				) );
		
		Cards5.add( new Card("The Treaty of Versaille", 5, Card.Type.Event, Card.subType.Entente, 3,3,3,
				(int pid) -> {
					if(!testing) {
						versaillePlayed[pid] = true;
					}
				}
				) );
		
		Cards5.add( new Card("The Yalta Conference", 5, Card.Type.Event, Card.subType.Entente, 0,1,3) );

		Cards5.add( new Card("Vladmir Lenin", 5, Card.Type.Leader, Card.subType.Rebel, -1,6,0) );
		
		Cards5.add( new Card("Winston Churchill", 5, Card.Type.Leader, Card.subType.Ruler, 3,2,0) );
		
		Cards5.add( new Card("World War I", 5, Card.Type.Event, Card.subType.Conflict, 2,2,-1, 
				(int pid) -> {//play a conflict
					if(hasMost(0) != -1) {
						updateScores(hasMost(0), -2, -2, -2);
					}
					chooseCard(pid, 
							"WWI: Play a Conflict",
							((Card c) -> c.getSubtype() == Card.subType.Conflict), 
							(int i, String chosenName, GameBoard board) -> {
								play(chosenName, pid);
								return true;}
							);
				}
				) );
		
		Cards5.add( new Card("World War II", 5, Card.Type.Event, Card.subType.Conflict, 2,3,-1, 
				(int pid) -> {//play a conflict
					if(hasMost(0) != -1) {
						updateScores(hasMost(0), -3, -3, -3);
					}
				}
				) );

		
		return Cards5;
	}
	
	
	private LinkedList<Card> CreateNationCards(){
		LinkedList<Card> nationCards = new LinkedList<Card>();
		
		//ERA 1
		nationCards.add( new Card("Achaemenid Empire", 1, Card.Type.Nation, Card.subType.None, 0,0,1) );
		nationCards.add( new Card("Assyrian Empire", 1, Card.Type.Nation, Card.subType.None, 1,0,0) );
		nationCards.add( new Card("Athenian Empire", 1, Card.Type.Nation, Card.subType.None, 0,1,0) );
		nationCards.add( new Card("Egyptian Empire", 1, Card.Type.Nation, Card.subType.None, 0,1,0) );
		nationCards.add( new Card("Han Dynasty", 1, Card.Type.Nation, Card.subType.None, 0,1,0) );
		nationCards.add( new Card("Macedonian Empire", 1, Card.Type.Nation, Card.subType.None, 1,0,0) );
		nationCards.add( new Card("Phoenician Empire", 1, Card.Type.Nation, Card.subType.None, 0,0,1) );
		nationCards.add( new Card("Roman Empire", 1, Card.Type.Nation, Card.subType.None, 1,0,0) );
		
		//ERA 2
		nationCards.add( new Card("Aztec Empire", 2, Card.Type.Nation, Card.subType.None, 3,2,0) );
		nationCards.add( new Card("Byzantine Empire", 2, Card.Type.Nation, Card.subType.None, 0,1,1) );
		nationCards.add( new Card("Holy Roman Empire", 2, Card.Type.Nation, Card.subType.None, 1,1,0) );
		nationCards.add( new Card("Inca Empire", 2, Card.Type.Nation, Card.subType.None, 1,1,0) );
		nationCards.add( new Card("Islamic Caliphate", 2, Card.Type.Nation, Card.subType.None, 1,0,1) );
		nationCards.add( new Card("Mali Empire", 2, Card.Type.Nation, Card.subType.None, 0,0,2) );
		nationCards.add( new Card("Mongol Empire", 2, Card.Type.Nation, Card.subType.None, 2,0,0) );
		nationCards.add( new Card("Vikings", 2, Card.Type.Nation, Card.subType.None, 1,0,1) );
		
		//ERA 3
		nationCards.add( new Card("Duchy of Venice", 3, Card.Type.Nation, Card.subType.None, 0,0,3) ); //gold in enter era written in elsewhere code
		nationCards.add( new Card("Mughal Empire", 3, Card.Type.Nation, Card.subType.None, 0,2,1) );
		nationCards.add( new Card("Ottoman Empire", 3, Card.Type.Nation, Card.subType.None, 4,3,0) );
		nationCards.add( new Card("Portuguese Empire", 3, Card.Type.Nation, Card.subType.None, 1,0,2) );
		nationCards.add( new Card("Qing Dynasty", 3, Card.Type.Nation, Card.subType.None, 2,2,0) );
		nationCards.add( new Card("Safavid Empire", 3, Card.Type.Nation, Card.subType.None, 2,1,0) );
		nationCards.add( new Card("Spanish Empire", 3, Card.Type.Nation, Card.subType.None, 2,0,1) );
		
		//ERA 4
		nationCards.add( new Card("British Empire", 4, Card.Type.Nation, Card.subType.None, 3,0,1) );
		nationCards.add( new Card("Dutch Empire", 4, Card.Type.Nation, Card.subType.None, 0,0,4) );
		nationCards.add( new Card("French Empire", 4, Card.Type.Nation, Card.subType.None, 1,2,0) );
		nationCards.add( new Card("German Empire", 4, Card.Type.Nation, Card.subType.None, 1,0,1) );
		nationCards.add( new Card("Japanese Empire", 4, Card.Type.Nation, Card.subType.None, 2,0,2) );
		nationCards.add( new Card("Russian Empire", 4, Card.Type.Nation, Card.subType.None, 2,2,0) );
		
		//ERA 5
		nationCards.add( new Card("United States of America", 5, Card.Type.Nation, Card.subType.None, 2,1,2) );
		nationCards.add( new Card("Soviet Union", 5, Card.Type.Nation, Card.subType.None, 2,3,0) );
		nationCards.add( new Card("State of Israel", 5, Card.Type.Nation, Card.subType.None, 3,2,0) );
		nationCards.add( new Card("People's Republic of China", 5, Card.Type.Nation, Card.subType.None, 1,4,0) );
		


		return nationCards;
	}
	
}
