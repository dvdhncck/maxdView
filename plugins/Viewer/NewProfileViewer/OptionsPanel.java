import java.io.*;

import javax.swing.*;
import javax.swing.event.*;

import java.util.*;

import javax.swing.tree.*;
import javax.swing.event.*;

public class OptionsPanel extends JPanel
{
    public OptionsPanel()
    {
	setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );

	add( new JCheckBox( "Apply filter" ) );

	add( new JCheckBox( "Show means" ) );

	add( new JCheckBox( "Include children" ) );

	add( new JCheckBox( "Mouse tracking" ) );

	add( new JCheckBox( "Smooth lines" ) );


	    // ------------------

	/*
	    apply_filter_jchkb = new JCheckBox("Apply filter");
	    apply_filter = mview.getBooleanProperty("ProfileViewer.apply_filter", false);
	    apply_filter_jchkb.setSelected(apply_filter);
	    apply_filter_jchkb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			//populateListWithSpots( spot_list );

			//pro_panel.repaint();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints(apply_filter_jchkb, c);
	    wrapper.add(apply_filter_jchkb);

	    // ------------------

	    
	    show_mean_jchkb = new JCheckBox("Show mean");
	    show_mean = mview.getBooleanProperty("ProfileViewer.show_mean", false);
	    show_mean_jchkb.setSelected(show_mean);
	    show_mean_jchkb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			show_mean = show_mean_jchkb.isSelected();

			// updateProfiles();
			//pro_panel.repaint();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    // c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints(show_mean_jchkb, c);
	    wrapper.add(show_mean_jchkb);

	    // ------------------

	    
	    include_children_jchkb = new JCheckBox("Include children");
	    include_children_jchkb.setToolTipText( "Include all of the child clusters with their selected parent" );
	    include_children = mview.getBooleanProperty("ProfileViewer.include_children", true);
	    include_children_jchkb.setSelected(include_children);
	    include_children_jchkb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			include_children = include_children_jchkb.isSelected();
			//updateProfiles();
			//pro_panel.repaint();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    // c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints(include_children_jchkb, c);
	    wrapper.add(include_children_jchkb);
	    

	    // ------------------
	    
	    
	    uniform_scale_jchkb = new JCheckBox("Uniform scale");
	    uniform_scale_jchkb.setToolTipText( "Force each component to use the same vertical scale" );
	    uniform_scale = mview.getBooleanProperty("ProfileViewer.uniform_scale", true);
	    uniform_scale_jchkb.setSelected(uniform_scale);
	    uniform_scale_jchkb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			uniform_scale = uniform_scale_jchkb.isSelected();
			// updateProfiles();
			//pro_panel.repaint();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints(uniform_scale_jchkb, c);
	    wrapper.add(uniform_scale_jchkb);
	*/
    }
}