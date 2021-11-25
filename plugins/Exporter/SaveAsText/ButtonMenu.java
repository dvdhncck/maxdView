import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class ButtonMenu extends JButton implements ActionListener 
{
    public ButtonMenu( maxdView mview, String name )
    {
	super(name);

	items = new Hashtable();
	listeners = new Vector();

	ImageIcon down_ii = new ImageIcon( mview.getImageDirectory() + "down-arrow.gif" );
	setIcon( down_ii );
	setHorizontalTextPosition( SwingConstants.LEFT );

	//setBorder(BorderFactory.createEtchedBorder());
	//add(menu);

	addActionListener(new ActionListener() 
	    { 
		public void actionPerformed(ActionEvent e) 
		{
		    showPopup();
		}
	    });
    }

    public void setTitle( String title )
    {
	menu.setText( title );
    }

    public void add( int id, JMenuItem comp )
    {
	items.put( new Integer(id) , comp );
	
	comp.addActionListener(this);

	// menu.add( comp );
    }

    public JMenuItem get( int id )
    {
	return (JMenuItem) items.get( new Integer(id) );
    }
    
    /*
    public void addButtonMenuListener( ButtonMenu.Listener bl )
    {
	listeners.addElement( bl );
    }
    public void removeButtonMenuListener( ButtonMenu.Listener bl )
    {
	listeners.removeElement( bl );

    }
    */

    /*
    public boolean isSelected( int id )
    {
	JMenuItem jmi = (JMenuItem) items.get( new Integer(id) );
	return (jmi != null) ? jmi.isSelected() : false;
    }
    */

    public interface Listener
    {
	public void menuItemSelected( int id, JMenuItem comp );
    }

    public void actionPerformed( ActionEvent ae )
    {
	JMenuItem src = (JMenuItem) ae.getSource();

	//System.out.println("1");

	for (Enumeration enum = items.keys(); enum.hasMoreElements() ; ) 
	{
	    Integer id     = (Integer)   enum.nextElement();
	    JMenuItem comp = (JMenuItem) items.get( id );
	    
	    if(comp == src)
	    {
		int idi = id.intValue();
		for(int l=0; l < listeners.size(); l++)
		{
		    // System.out.println("2");
		    
		    ((ButtonMenu.Listener) listeners.elementAt(l)).menuItemSelected( idi, comp );
		}
	    }
	}
    }

    public JPopupMenu makePopup()
    {
	JPopupMenu popup = new JPopupMenu();
	if(font != null)
	    popup.setFont(font);
	addToMenu(popup, true);
	return popup;
    }

    private void addToMenu(JComponent popup, boolean is_popup)
    {

    }

     private void showPopup()
    {
	// System.out.println("show popup....");

	/*
	{
	    System.out.println("  hiding...");
	    popup.setVisible(false);
	}
	else
	*/

	if((popup != null) && popup.isVisible())
	    return;

	popup = makePopup();
	
	// System.out.println("  showing...");

	Point cur_pos = getLocationOnScreen();
	
	popup.setLocation(cur_pos);
	//popup.setWidth(jb.getWidth());
	popup.show(this, 0, jb.getHeight());
    }
   
    private JMenu menu;
    private Vector listeners;
    private Hashtable items;
    
    private ActionListener al;
    private JPopupMenu popup;
    private JButton jb;

}
