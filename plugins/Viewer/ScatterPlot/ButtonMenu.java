import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;

public class ButtonMenu extends JMenuBar implements ActionListener 
{
    public ButtonMenu( maxdView mview, String name )
    {
	items = new Hashtable();
	listeners = new Vector();

	menu = new JMenu(name, true);
	
	ImageIcon down_ii = new ImageIcon( mview.getImageDirectory() + "down-arrow.gif" );
	menu.setIcon( down_ii );
	menu.setHorizontalTextPosition( SwingConstants.LEFT );

	setBorder(BorderFactory.createEtchedBorder());
	add(menu);
    }

    public void setFont( Font font )
    {
	super.setFont( font );

	if( menu != null )
	{
	    menu.setFont( font );
	}
    }
 
    public void add( int id, JMenuItem comp )
    {
	items.put( new Integer(id) , comp );
	
	comp.addActionListener(this);

	menu.add( comp );
    }

    public JMenuItem get( int id )
    {
	return (JMenuItem) items.get( new Integer(id) );
    }
    
    public void addButtonMenuListener( ButtonMenu.Listener bl )
    {
	listeners.addElement( bl );
    }
    public void removeButtonMenuListener( ButtonMenu.Listener bl )
    {
	listeners.removeElement( bl );

    }

    public boolean isSelected( int id )
    {
	JMenuItem jmi = (JMenuItem) items.get( new Integer(id) );
	return (jmi != null) ? jmi.isSelected() : false;
    }
    
    public interface Listener
    {
	public void menuItemSelected( int id, JMenuItem comp );
    }

    private JMenu menu;
    private Vector listeners;
    private Hashtable items;

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

    

}
