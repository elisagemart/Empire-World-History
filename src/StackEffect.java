
public class StackEffect {

	private GameBoard.Procedure effect;
	private int pid;
	private boolean delay; //should we delay this effect if AI plays it on their turn?
	
	public GameBoard.Procedure getEffect() {
		return effect;
	}

	public void setEffect(GameBoard.Procedure effect) {
		this.effect = effect;
	}

	public int getPid() {
		return pid;
	}

	public void setPid(int pid) {
		this.pid = pid;
	}

	public boolean isDelay() {
		return delay;
	}

	public void setDelay(boolean delay) {
		this.delay = delay;
	}

	public StackEffect(int pid, GameBoard.Procedure effect) {
		this.pid = pid;
		this.effect = effect;
		this.delay = false;
	}
	public StackEffect(int pid, boolean delay,  GameBoard.Procedure effect) {
		this.pid = pid;
		this.effect = effect;
		this.delay = delay;
	}
	
}
