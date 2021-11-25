import java.io.*;

import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import java.awt.*;
import java.awt.event.*;

public class PanelSelector extends JPanel
{
    public PanelSelector( maxdView mview )
    {
	master_panel = new JPanel();
	master_gridbag = new GridBagLayout();
	master_panel.setLayout( master_gridbag );

	//JScrollPane jsp = new JScrollPane( master_panel );

	//jsp.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );

	GridBagLayout gridbag = new GridBagLayout();
	setLayout(gridbag);
	GridBagConstraints c = new GridBagConstraints();
	c.weightx = 10.0;
	c.weighty = 10.0;
	c.fill = GridBagConstraints.BOTH;
	c.anchor = GridBagConstraints.NORTHWEST;
	gridbag.setConstraints( master_panel, c );
	add( master_panel );
 
	button_to_panel = new Hashtable();

	right_ii = new ImageIcon(mview.getImageDirectory() + "moveright.gif");
	down_ii  = new ImageIcon(mview.getImageDirectory() + "movedown.gif");

	button_group = new ButtonGroup();
   }

    public void add( final String name, final JPanel panel )
    {
	GridBagConstraints c;

	panel.setVisible( false );

	if( next_row > 0 )
	{
	    LineSeparator lsep = new LineSeparator();
	    lsep.setBorder( BorderFactory.createEmptyBorder( 5,0,0,0 ) );
	    c = new GridBagConstraints();
	    c.gridy = next_row++;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.NORTHWEST;
	    master_gridbag.setConstraints( lsep, c );
	    master_panel.add( lsep );
	}

	
	final JToggleButton button = new JToggleButton( name );
	
	button_group.add( button );

	button.setBorder( BorderFactory.createEmptyBorder() );
	button.setHorizontalTextPosition( SwingConstants.LEFT );
	//button.setContentAreaFilled( false );
	button.setBackground( master_panel.getBackground().darker() );

	//button.setIcon( right_ii );
	//button.setPressedIcon( down_ii );
	//button.setSelectedIcon( down_ii );

	button.addActionListener( new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    for (Enumeration en =  button_to_panel.keys(); en.hasMoreElements() ;) 
		    {
			JPanel panel = (JPanel) button_to_panel.get( en.nextElement() );
			panel.setVisible( false );
		    }

		    JPanel panel = (JPanel) button_to_panel.get( button );

		    panel.setVisible( button.isSelected() );
		}
	    } );
	
	button_to_panel.put( button, panel );
	
	c = new GridBagConstraints();
	c.gridy = next_row++;
	c.weightx = 10.0;
	c.weighty = 0.001;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.anchor = GridBagConstraints.NORTHWEST;
	master_gridbag.setConstraints( button, c );
	master_panel.add( button );
 
	c = new GridBagConstraints();
	c.gridy = next_row++;
	c.weightx = 10.0;
	c.weighty = 9.0;
	c.fill = GridBagConstraints.BOTH;
	c.anchor = GridBagConstraints.NORTHWEST;
	master_gridbag.setConstraints( panel, c );
	master_panel.add( panel );
 

    }


    public void displayPanel( JPanel chosen_panel )
    {
	JToggleButton target_button = null;

	for ( Enumeration en =  button_to_panel.keys(); en.hasMoreElements() ;) 
	{
	    JToggleButton button = (JToggleButton)  en.nextElement();

	    JPanel panel = (JPanel) button_to_panel.get( button );
	    //
	    // panel.setVisible( false );

	    if( panel == chosen_panel )
		target_button = button;
	}
	
	if( target_button != null )
	    target_button.doClick( );
    }


    class LineSeparator extends JComponent
    {
	public void paintComponent( Graphics g )
	{
	    g.setColor( Color.black );
	    g.fillRect( 0,0, getWidth(), getHeight() );

	    //System.out.println( "LineSeparator: " + getWidth() + "x" +  getHeight() );
	}
    }


    private int next_row = 0;
    private GridBagLayout master_gridbag;
    private ButtonGroup button_group;
    private JPanel master_panel;
    private Hashtable button_to_panel;
    private ImageIcon right_ii;
    private ImageIcon down_ii;
}
