import javax.swing.JPanel;



public class JPanelFixed extends JPanel {
	@Override
	public java.awt.Dimension getPreferredSize() {

	    int h = super.getPreferredSize().height;
	    int w = getParent().getSize().width;
	    return new java.awt.Dimension(w, h);
	}
}
