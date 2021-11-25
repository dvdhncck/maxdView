//package uk.ac.man.bioinf.maxdLoad2;

import java.awt.*;
import javax.swing.*;

//
//
// This is null-ed out verion of the ComponentFactory which
// doesn't provided any anti-aliaised components....
//
//
class ComponentFactory
{
    public final static boolean useAntialiasing()
    {
	return false;
    }

    public final static void setRenderingHints( final Graphics2D g2 )
    {
    }

    public final static JFrame makeFrame( final String t )
    {
	    return new JFrame( t );
    }


    public final static JList makeList()
    {
	    return new JList( );
    }


    public final static JLabel makeLabel( final String s )
    {
	    return new JLabel( s );
    }

    public final static JLabel makeLabel( final ImageIcon i )
    {
	    return new JLabel( i );
    }

    public final static JComboBox makeComboBox( final String[] sa )
    {
	    return new JComboBox( sa );
    }

    public final static JComboBox makeComboBox(  )
    {
	    return new JComboBox( );
    }

    public final static JTextField makeTextField( final int length )
    {
	    return new JTextField( length );
    }

    public final static JPasswordField makePasswordField( final int length )
    {
	    return new JPasswordField( length );
    }

    public final static JButton makeButton( )
    {
	    return new JButton( );
    }

    public final static JButton makeButton( String s )
    {
	    return new JButton( s );
    }

    public final static JButton makeButton( ImageIcon i )
    {
	    return new JButton( i );
    }

    public final static JButton makeButton( String s, ImageIcon i )
    {
	    return new JButton( s, i );
    }

    public final static JToggleButton makeToggleButton( String s, ImageIcon i )
    {
	    return new JToggleButton( s, i  );
    }

    public final static JRadioButton makeRadioButton( String s )
    {
	    return new JRadioButton( s );
    }

    public final static JCheckBox makeCheckBox( String s, Icon i )
    {
	    return new JCheckBox( s, i );
    }

    public final static JCheckBox makeCheckBox( String s )
    {
	    return new JCheckBox( s );
    }

    public final static JCheckBox makeCheckBox()
    {
	    return new JCheckBox();
    }

   public final static JToolTip makeToolTip()
    {
	    return new JToolTip();
    }


    public final static JTextArea makeTextArea( String s )
    {
	    return new JTextArea( s  );
    }

    public final static JTextPane makeTextPane( javax.swing.text.DefaultStyledDocument dsd )
    {
	    return new JTextPane( dsd  );
    }

    public final static JTextArea makeTextArea( int c, int r, boolean wrap )
    {
	{
	    JTextArea jta = new JTextArea( c, r  );
	    jta.setLineWrap( wrap );
	    return jta; 
	}
    }

    public final static JPopupMenu makePopupMenu( )
    {
	    return new JPopupMenu( );
    }

    public final static JPopupMenu makePopupMenu( String title )
    {
	    return new JPopupMenu( title );
    }

    public final static JMenuItem makeMenuItem( String title )
    {
	    return new JMenuItem( title );
    }

}
