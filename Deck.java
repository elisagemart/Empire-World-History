import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedList;

public class Deck{

	//list to store the cards in the deck, as a stack
	private LinkedList<Card> deck;
	
	//list to store the discard piles for each deck
	public LinkedList<Card> discardPile;
	
	//construct a new deck based on a list of cards
	public Deck( LinkedList<Card> cards) {
		
		//loop through cards and create new card objects for deck
		this.deck = cards;
		
		this.discardPile = new LinkedList<Card>();
		
		//shuffle the deck
		shuffle();
		
	}
	
	public Deck() {

		//loop through cards and create new card objects for deck
		this.deck = new LinkedList<Card>();

		this.discardPile = new LinkedList<Card>();

		//shuffle the deck
		shuffle();

	}
	
	//randomize the deck
	public void shuffle() {
		GUI.playSound("Shuffle.wav");
		Collections.shuffle(deck);
	}
	
	//draw a card from the top of the deck
	public Card draw() {
		
		//if there are no cards to draw, reshuffle discard pile into deck
		if(deck.size() < 1) {
			deck = discardPile;
			discardPile = new LinkedList<Card>();
			shuffle();
		}
		
		return deck.pop();
	}
	
	//return true if this deck has cards left between its deck and discard pile
	public boolean hasCards() {
		return (deck.size() + discardPile.size() > 0);
	}
	
	public Card drawFromDiscard() {
			
		return discardPile.removeLast();
	}
	
	//discard until a political/social system is found
	public Card getPSSystem() {
		Card card = draw();
		while(card.getSubtype() != Card.subType.Political && card.getSubtype() != Card.subType.Social) {
			discard(card);
			if(deck.isEmpty()) {
				return null;
			}
			else {
				card = draw();
			}
		}
		return card;
	}
	
	//TODO: should this just search?
	public Card getCard(String cardName) {
		for(int i = 0; i < deck.size(); i++) {
			if(deck.get(i).getCardName() == cardName) {
				Card foundCard = deck.get(i);
				deck.remove(i);
				return foundCard;
			}
		}
		System.out.println("Not found: " + cardName);
		return null;
	}
	
	public Card topDiscard() {
		return discardPile.getLast();
	}
	
	public boolean hasDiscard() {
		return discardPile.size() > 0;
	}

	
	public void discard(Card card) {
		discardPile.add(card);
	}
	
	public void addCard(Card card) {
		deck.add(card);
	}
	
	public LinkedList<Card> getDeck(){
		return deck;
	}
	
	/*public LinkedList<Card> casteEffect(int n){//shuffle in top card of discard, then return discard
		Card topCard = discardPile.getLast();
		discardPile.removeLast();
		System.out.println(topCard.getCardName());
		deck.add(topCard);
		shuffle();
		LinkedList<Card> returnedDiscard = new LinkedList<Card>();
		int i = 0;
		while(i < n && discardPile.size() > 0) {
			returnedDiscard.add( discardPile.removeLast());
		}
		return returnedDiscard;
	}*/
	
	public void casteEffect(){//shuffle in last played card
		Card topCard = discardPile.getLast();
		discardPile.removeLast();
		deck.add(topCard);
		shuffle();
	}
	
	//create a new deck, with linked lists that point to the same cards but in a new list
	public Deck copy() {
		Deck copyDeck = new Deck();
		for(int i = 0; i < deck.size(); i++) {
			copyDeck.deck.add(this.deck.get(i));
		}
		for(int i = 0; i < discardPile.size(); i++) {
			copyDeck.discardPile.add(this.discardPile.get(i));
		}
		return copyDeck;
	}
	
	
}
