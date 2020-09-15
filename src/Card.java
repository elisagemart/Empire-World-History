import java.io.Serializable;
import java.util.function.IntPredicate;

public class Card implements Serializable{
	
	//enum to define card types
	enum Type{
		
		Leader, Event, System, Nation;
		
		private static final Type[] types = Type.values();
		public static Type getType(int i) {
			return Type.types[i];
		}
		
	}
	
	//enum to define card subtypes
	enum subType{
		
		General, Ruler, Philosopher, Artist, Cleric, Explorer, Scientist, Rebel,
		Movement, Proclamation, Discovery, Entente, Conflict, 
		Religion, Political, Social,  Economic,
		None;
		
		
		private static final subType[] subtypes = subType.values();
		public static subType getSubType(int i) {
			return subType.subtypes[i];
		}
		
	}
	
	//card name
	String cardName;
	
	//card types
	private Type type;
	private subType subtype;
	
	//card scores
	private int military;
	private int culture;
	private int money;
	
	//card era
	private int era;
	
	private int player;
	
	//playable condition
	public IntPredicate playable;
	
	//additional effect
	@FunctionalInterface //used to define lambda functions relating to cards
	public interface Effect{
	   void execute(int pid);
	} 
	public Effect effect;

	public Card(String name, int era, Type type, subType subtype, int military, int culture, int money) {
		this.cardName = name;
		this.era = era;
		this.type = type;
		this.subtype = subtype;
		this.military = military;
		this.culture = culture;
		this.money = money;
		this.playable = (int pid) -> {return true;}; //default to playable
		this.effect = (int pid) -> {;}; //default to no additional effect
	}
	
	public Card(String name, int era, Type type, subType subtype, int military, int culture, int money, IntPredicate playable, Effect effect) {
		this.cardName = name;
		this.era = era;
		this.type = type;
		this.subtype = subtype;
		this.military = military;
		this.culture = culture;
		this.money = money;
		this.playable = playable;
		this.effect = effect;
	}
	
	public Card(String name, int era, Type type, subType subtype, int military, int culture, int money, Effect effect) {
		this.cardName = name;
		this.era = era;
		this.type = type;
		this.subtype = subtype;
		this.military = military;
		this.culture = culture;
		this.money = money;
		this.playable = (int pid) -> {return true;}; //default to playable
		this.effect = effect;
	}
	
	public Card(String name, int era, Type type, subType subtype, int military, int culture, int money, IntPredicate playable) {
		this.cardName = name;
		this.era = era;
		this.type = type;
		this.subtype = subtype;
		this.military = military;
		this.culture = culture;
		this.money = money;
		this.playable = playable;
		this.effect = (int pid) -> {;}; // no effect
	}
	

	public String getCardName() {
		return cardName;
	}

	public Type getType() {
		return type;
	}

	public subType getSubtype() {
		return subtype;
	}
	
	public boolean isSubtype(subType subtype) {
		if(this.subtype == subtype || 
				(cardName == "Leonardo da Vinci" && (subtype == subType.General || subtype == subType.Ruler || subtype == subType.Philosopher || subtype ==  subType.Artist || subtype == subType.Cleric || subtype == subType.Explorer || subtype ==  subType.Scientist || subtype ==  subType.Rebel) )) {
			return true;
		}
		else {
			return false;
		}
	}

	public int getMilitary() {
		return military;
	}

	public int getCulture() {
		return culture;
	}

	public int getMoney() {
		return money;
	}

	public int getEra() {
		return era;
	}

	public int getPlayer() {
		return player;
	}

	public void setPlayer(int player) {
		this.player = player;
	}
	
	
	
}
