import java.io.*;

import javax.swing.*;
import javax.swing.event.*;

import java.util.*;

import javax.swing.tree.*;
import javax.swing.event.*;

public class GraphLayoutPanel extends JPanel
{
     public GraphLayoutPanel()
    {
	setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );

	add( new JLabel( "Horizontal Axis" ) );

	add( new JButton( "this is a button" ) );

	add( new JCheckBox( "this is an option" ) );

	add( new JLabel( "Vertical Axis" ) );

	add( new JButton( "this is a button" ) );

	add( new JCheckBox( "this is an option" ) );
 
    }
}