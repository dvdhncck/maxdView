import java.io.*;

import javax.swing.*;
import javax.swing.event.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

import java.awt.font.*;
import java.awt.geom.*;

import java.awt.dnd.*;
import java.awt.datatransfer.*;

import javax.swing.border.*;
import javax.swing.JTree;
import javax.swing.tree.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreeSelectionModel;

//
//
//  groups spots with similar profiles together
//  (a clusterer by any other name)
//
//
///////////////
//
// basic idea;
//
//     user selects number of profiles N
// 
//     pick N initial random target profiles
//
//     iterate
//
//        put each spot into the group with the target profile most similar to the spot
//
//        adjust the target profiles to equalise the distribution
//
//      until stable
//
//
//
///////////////
//
//  its the "adjust the profiles" bit that is hard....
//
//   
//
//

public class SuperGrouper implements ExprData.ExprDataObserver, Plugin, ExprData.ExternalSelectionListener
{
    public SuperGrouper(maxdView mview_)
    {
	mview = mview_;

	
	//System.out.println("++ a new SuperGrouper is alive, mview is "  + mview);
    }

    final String[] adjust_what_opts = new  String[] 
    { "Smallest", "Biggest", "Best", "Worst", "Random", "All" };

    final String[] distance_opts = new  String[] 
    { "Distance", "Slope", "Direction" };

    final String[] adjust_opts = new  String[] 
    { 
	"Mean", "Mean Spot", "Random"
    };

    private void buildGUI()
    {
	sel_cls = new Vector();
	sel_ids = new Vector();
	meas_ids = new int[0];

	frame = new JFrame ("Super Grouper");
	mview.decorateFrame( frame );
	frame.addWindowListener(new WindowAdapter() 
	    {
		public void windowClosing(WindowEvent e)
		{
		    cleanUp();
		}
	    });

	
	JPanel panel = new JPanel();
	GridBagLayout gridbag = new GridBagLayout();
	panel.setLayout(gridbag);

	panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	
	frame.getContentPane().add(panel);
	
	background_col = mview.getBackgroundColour();
	text_col       = mview.getTextColour();

	
	GridBagConstraints c = null;


	// ========================================================================================

	{
	    JPanel wrapper = new JPanel();
	    wrapper.setBorder(BorderFactory.createEmptyBorder(0,0,3,0));
	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrapper.setLayout(w_gridbag);

	    int col = 0;
	    JButton jb = null;


	    JLabel label = new JLabel("Labels ");
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    c.anchor = GridBagConstraints.EAST;
	    w_gridbag.setConstraints(label, c);
	    wrapper.add(label);


	    nts = new NameTagSelector(mview);
	    nts.loadSelection("SuperGrouper.name_tags");
	    nt_sel = nts.getNameTagSelection();
	    nts.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			nts.saveSelection("SuperGrouper.name_tags");
			nt_sel = nts.getNameTagSelection();
			pro_panel.repaint( );
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    c.weightx = 2.0;
	    c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints(nts, c);
	    wrapper.add(nts);

	    jb = new JButton("Clear All");
	    jb.setFont(mview.getSmallFont());
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			clearAllSpotLabels();
			pro_panel.repaint( );
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    c.fill = GridBagConstraints.VERTICAL;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    c.anchor = GridBagConstraints.EAST;
	    w_gridbag.setConstraints(jb, c);
	    wrapper.add(jb);

	    jb = new JButton("Label selection");
	    jb.setFont(mview.getSmallFont());
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			setAllSpotLabels();
			pro_panel.repaint( );
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    c.fill = GridBagConstraints.VERTICAL;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints(jb, c);
	    wrapper.add(jb);


	    ImageIcon icon = new ImageIcon(mview.getImageDirectory() + "f-up.gif");
	    Insets ins = new Insets(0,0,0,0);
	    jb = new JButton(icon);
	    jb.setMargin(ins);
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			spot_font_scale += 0.1;
			pro_panel.repaint( );
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    c.fill = GridBagConstraints.VERTICAL;
	    c.weighty = 1.0;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.EAST;
	    w_gridbag.setConstraints(jb, c);
	    wrapper.add(jb);

	    icon = new ImageIcon(mview.getImageDirectory() + "f-down.gif");
	    jb = new JButton(icon);
	    jb.setMargin(ins);
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			if(spot_font_scale > .0)
			{
			    spot_font_scale -= 0.1;
			    pro_panel.repaint( );
			}
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    c.fill = GridBagConstraints.VERTICAL;
	    c.weighty = 1.0;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints(jb, c);
	    wrapper.add(jb);

	    
	    final JCheckBox auto_space_jchkb = new JCheckBox("Auto space");
	    auto_space = mview.getBooleanProperty("SuperGrouper.auto_space", true);
	    auto_space_jchkb.setSelected(auto_space);
	    //auto_space_jchkb.setFont(mview.getSmallFont());
	    auto_space_jchkb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			auto_space = auto_space_jchkb.isSelected();
			pro_panel.repaint( );
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    //c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints(auto_space_jchkb, c);
	    wrapper.add(auto_space_jchkb);


	    final JCheckBox col_sel_jchkb = new JCheckBox("Colour");
	    col_sel = mview.getBooleanProperty("SuperGrouper.col_sel", true);
	    col_sel_jchkb.setSelected(col_sel);
	    //col_sel_jchkb.setFont(mview.getSmallFont());
	    col_sel_jchkb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			col_sel = col_sel_jchkb.isSelected();
			pro_panel.repaint( );
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    //c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints(col_sel_jchkb, c);
	    wrapper.add(col_sel_jchkb);

	    // 
	    // top button panel
	    //
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    //c.gridwidth = 2;
	    //c.weighty = 2.0;
	    c.weightx = 10.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(wrapper, c);
	    panel.add(wrapper);


	}

	// ========================================================================================
	
	{

	    JPanel control_panel = new JPanel();
	    GridBagLayout control_gridbag = new GridBagLayout();
	    control_panel.setLayout(control_gridbag);

	    JButton jb = null;
	    int line =0 ;


	    // \\ // \\ // \\ // \\ // \\ // \\ // \\ // \\ 


	    JPanel but_wrapper = new JPanel();
	    GridBagLayout but_gridbag = new GridBagLayout();
	    but_wrapper.setLayout(but_gridbag);
	    TitledBorder title = BorderFactory.createTitledBorder(" Similarity Metric ");
	    //title.setTitleColor(title_colour);
	    but_wrapper.setBorder(title);
	   

	    
	    distance_metric_jcb = new JComboBox( distance_opts );
	    c = new GridBagConstraints();
	    but_gridbag.setConstraints(distance_metric_jcb, c);
	    but_wrapper.add(distance_metric_jcb);
	    int metric = mview.getIntProperty( "SuperGrouper.distance_metric", 0 );
	    if(metric > 2)
		metric = 2;
	    distance_metric_jcb.setSelectedIndex( metric );
	    distance_metric_jcb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			updateGroups();
		    }
		});

	    

	    c = new GridBagConstraints();
	    c.weightx = 1;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.gridy = line++;
	    control_gridbag.setConstraints(but_wrapper, c);
	    control_panel.add(but_wrapper);

	    // \\ // \\ // \\ // \\ // \\ // \\ // \\ // \\ 

	    but_wrapper = new JPanel();
	    but_gridbag = new GridBagLayout();
	    but_wrapper.setLayout(but_gridbag);
	    title = BorderFactory.createTitledBorder(" Groups ");
	    //title.setTitleColor(title_colour);
	    but_wrapper.setBorder(title);
	   

	    n_groups_jtf = new JTextField(4);
	    n_groups_jtf.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			setupGroups();
			updateGroups();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    but_gridbag.setConstraints(n_groups_jtf, c);
	    but_wrapper.add(n_groups_jtf);


	    jb = new JButton("Set");
	    jb.setFont(mview.getSmallFont());
	    jb.setMargin( new Insets(0,0,0,0 ) );
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    but_gridbag.setConstraints(jb, c);
	    but_wrapper.add(jb);
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			setupGroups();
			updateGroups();
		    }
		});


	    

	    
	    jb = new JButton("Use selection");
	    jb.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridy = 1;
	    c.gridwidth = 2;
	    c.weightx = 2.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    //c.anchor = GridBagConstraints.WEST;
	    but_gridbag.setConstraints(jb, c);
	    but_wrapper.add(jb);
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			setGroupsFromSelection();
			updateGroups();
		    }
		});


	    jb = new JButton("Permutations");
	    jb.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridy = 2;
	    c.gridwidth = 2;
	    c.weightx = 2.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    //c.anchor = GridBagConstraints.WEST;
	    but_gridbag.setConstraints(jb, c);
	    but_wrapper.add(jb);
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			setGroupsByPermutation();
			updateGroups();
		    }
		});

	    jb = new JButton("Randomise");
	    jb.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridy = 3;
	    c.gridwidth = 2;
	    c.weightx = 2.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    //c.anchor = GridBagConstraints.WEST;
	    but_gridbag.setConstraints(jb, c);
	    but_wrapper.add(jb);
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			randomiseGroupProfiles();
			updateGroups();
		    }
		});


	    jb = new JButton("Remove empty");
	    jb.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridy = 4;
	    c.gridwidth = 2;
	    c.weightx = 2.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    //c.anchor = GridBagConstraints.WEST;
	    but_gridbag.setConstraints(jb, c);
	    but_wrapper.add(jb);
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			removeEmptyGroups();
			updateGroups();
		    }
		});



	    c = new GridBagConstraints();
	    c.gridy = line++;
	    c.weightx = 1;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    control_gridbag.setConstraints(but_wrapper, c);
	    control_panel.add(but_wrapper);

	    // \\ // \\ // \\ // \\ // \\ // \\ // \\ // \\ 

	    but_wrapper = new JPanel();
	    but_gridbag = new GridBagLayout();
	    but_wrapper.setLayout(but_gridbag);
	    title = BorderFactory.createTitledBorder(" Options ");
	    //title.setTitleColor(title_colour);
	    but_wrapper.setBorder(title);
	   

	    apply_filter_jchkb = new JCheckBox("Apply filter");
	    apply_filter = mview.getBooleanProperty("SuperGrouper.apply_filter", false);
	    apply_filter_jchkb.setSelected(apply_filter);
	    apply_filter_jchkb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			apply_filter = apply_filter_jchkb.isSelected();
			updateGroups();
		    }
		});
	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    but_gridbag.setConstraints(apply_filter_jchkb, c);
	    but_wrapper.add(apply_filter_jchkb);


	    edit_profiles_jchkb = new JCheckBox("Edit profiles");
	    edit_profiles_jchkb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			pro_panel.repaint();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridy = 1;
	    c.anchor = GridBagConstraints.WEST;
	    but_gridbag.setConstraints(edit_profiles_jchkb, c);
	    but_wrapper.add(edit_profiles_jchkb);

	    c = new GridBagConstraints();
	    c.gridy = line++;
	    c.weightx = 1;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    control_gridbag.setConstraints(but_wrapper, c);
	    control_panel.add(but_wrapper);


	    // \\ // \\ // \\ // \\ // \\ // \\ // \\ // \\ 


	    but_wrapper = new JPanel();
	    but_gridbag = new GridBagLayout();
	    but_wrapper.setLayout(but_gridbag);
	    title = BorderFactory.createTitledBorder(" Auto-adjust ");
	    //title.setTitleColor(title_colour);
	    but_wrapper.setBorder(title);

	    iterate_jchkb = new JCheckBox("Enable");
	    c = new GridBagConstraints();
	    c.gridy = 0;
	    c.weightx = 1;
	    c.anchor = GridBagConstraints.WEST;
	    but_gridbag.setConstraints(iterate_jchkb, c);
	    but_wrapper.add(iterate_jchkb);
	    
	    iterate_jchkb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			adjust_label.setText("  ");
			startStopIterate();
		    }
		});

	    adjust_label = new JLabel("  ");
	    adjust_label.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    c.weightx = 1;
	    c.anchor = GridBagConstraints.EAST;
	    but_gridbag.setConstraints(adjust_label, c);
	    but_wrapper.add(adjust_label);


	    
	    adjust_what = mview.getIntProperty( "SuperGrouper.adjust_what", 5);

	    adjust_what_jcb = new JComboBox( adjust_what_opts );
	    c = new GridBagConstraints();
	    adjust_what_jcb.setSelectedIndex(adjust_what);
	    c = new GridBagConstraints();
	    c.gridy = 1;
	    c.weightx = 1;
	    c.gridwidth = 2;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    but_gridbag.setConstraints(adjust_what_jcb, c);
	    but_wrapper.add(adjust_what_jcb);
	    adjust_what_jcb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			adjust_what = adjust_what_jcb.getSelectedIndex();
		    }
		});



	    
	    adjust_mode = mview.getIntProperty( "SuperGrouper.adjust_mode", 1);

	    adjust_mode_jcb = new JComboBox( adjust_opts );
	    c = new GridBagConstraints();
	    adjust_mode_jcb.setSelectedIndex(adjust_mode);
	    c = new GridBagConstraints();
	    c.gridy = 2;
	    c.weightx = 1;
	    c.gridwidth = 2;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    but_gridbag.setConstraints(adjust_mode_jcb, c);
	    but_wrapper.add(adjust_mode_jcb);
	    adjust_mode_jcb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			adjust_mode = adjust_mode_jcb.getSelectedIndex();
		    }
		});


	    JLabel label = new JLabel("Rate");
	    label.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridy = 3;
	    c.anchor = GridBagConstraints.WEST;
	    but_gridbag.setConstraints(label, c);
	    but_wrapper.add(label);

	    adjust_rate = mview.getDoubleProperty( "SuperGrouper.adjust_rate",  .1);

	    rate_label = new JLabel( String.valueOf(adjust_rate) );
	    rate_label.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridy = 3;
	    c.gridx = 1;
	    c.anchor = GridBagConstraints.EAST;
	    but_gridbag.setConstraints(rate_label, c);
	    but_wrapper.add(rate_label);


	    rate_slider = new JSlider(JSlider.HORIZONTAL, 1, 100, 1);
	    rate_slider.setPreferredSize(new Dimension(100,20));
	    //slider.setPaintTicks(true);
	    //slider.setMajorTickSpacing(10);
	    rate_slider.setValue( (int)(adjust_rate * 100.0) );
	    rate_slider.addChangeListener(new ChangeListener()
		{
		    public void stateChanged(ChangeEvent e) 
		    {
			JSlider source = (JSlider)e.getSource();
			// if (!source.getValueIsAdjusting()) 
			{
			    adjust_rate = ((double) source.getValue()) * 0.01;

			    rate_label.setText( mview.niceDouble( adjust_rate, 6, 3 ) );
			}
		    }
		});
	    c = new GridBagConstraints();
	    c.gridy = 4;
	    c.weightx = 1;
	    c.gridwidth = 2;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    but_gridbag.setConstraints(rate_slider, c);
	    but_wrapper.add(rate_slider);
	  

	    label = new JLabel("Noise");
	    label.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridy = 5;
	    c.anchor = GridBagConstraints.WEST;
	    but_gridbag.setConstraints(label, c);
	    but_wrapper.add(label);

	    adjust_noise_level = mview.getDoubleProperty( "SuperGrouper.adjust_noise_level",  .01);

	    noise_label = new JLabel( String.valueOf(adjust_noise_level) );
	    noise_label.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridy = 5;
	    c.gridx = 1;
	    c.anchor = GridBagConstraints.EAST;
	    but_gridbag.setConstraints(noise_label, c);
	    but_wrapper.add(noise_label);

	    noise_slider = new JSlider(JSlider.HORIZONTAL, 0, 100, 1);
	    noise_slider.setPreferredSize(new Dimension(100,20));
	    //slider.setPaintTicks(true);
	    //xslider.setMajorTickSpacing(10);
	    noise_slider.setValue( (int)(adjust_noise_level * 1000.0) );
	    noise_slider.addChangeListener(new ChangeListener()
		{
		    public void stateChanged(ChangeEvent e) 
		    {
			JSlider source = (JSlider)e.getSource();
			// if (!source.getValueIsAdjusting()) 
			{
			    adjust_noise_level = ((double) source.getValue()) * 0.001;
			    
			    noise_label.setText( mview.niceDouble( adjust_noise_level, 6, 4 ) );

			}
		    }
		});
	    c = new GridBagConstraints();
	    c.gridy = 6;
	    c.weightx = 1;
	    c.gridwidth = 2;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    but_gridbag.setConstraints(noise_slider, c);
	    but_wrapper.add(noise_slider);
	  

	    
	    auto_remove_empty_jchkb = new JCheckBox("Auto remove empty");
	    adjust_auto_remove_empty = mview.getBooleanProperty("SuperGrouper.adjust_auto_remove_empty", false);
	    auto_remove_empty_jchkb.setSelected( adjust_auto_remove_empty );
	    auto_remove_empty_jchkb.setFont(mview.getSmallFont());
	    auto_remove_empty_jchkb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			adjust_auto_remove_empty = auto_remove_empty_jchkb.isSelected();
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridy = 7;
	    c.weightx = 1;
	    c.gridwidth = 2;
	    c.anchor = GridBagConstraints.WEST;
	    but_gridbag.setConstraints(auto_remove_empty_jchkb, c);
	    but_wrapper.add(auto_remove_empty_jchkb);
	    

	    c = new GridBagConstraints();
	    c.gridy = line++;
	    c.weightx = 1;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    control_gridbag.setConstraints(but_wrapper, c);
	    control_panel.add(but_wrapper);


	    JScrollPane jsp1 = new JScrollPane( control_panel );
	    
	    // =========================================================

	    meas_list = new DragAndDropList();

	    populateListWithMeasurements(meas_list);

	    meas_list.setSelectionInterval( 0, meas_ids.length - 1);

	    meas_list.addListSelectionListener(new ListSelectionListener() 
		{
		    public void valueChanged(ListSelectionEvent e)
		    {
			listSelectionHasChanged();
		    }
		});

	    meas_list.setDropAction( new DragAndDropList.DropAction()
	    {
		public void dropped(DragAndDropEntity dnde)
		{
		    try
		    {
			String[] meas_n = dnde.getMeasurementNames(edata);
			int[] cur_sel = meas_list.getSelectedIndices();
			int n_cur = (cur_sel == null) ? 0 : cur_sel.length ;
			int[] new_sel = new int[meas_n.length];
			int n_new = 0;

			for(int n=0; n < meas_n.length; n++)
			{
			    ListModel lm = meas_list.getModel();
			    int i = -1;
			    for(int o=0; o < lm.getSize(); o++)
				if(meas_n[n].equals( (String) lm.getElementAt(o)))
				    i = o;
			    if(i >= 0)
			    {
				new_sel[n_new] = i;
				n_new++;
			    }
			}
			if(n_new > 0)
			{
			    int[] mix_sel = new int[n_cur + n_new];

			    for(int s=0; s <  n_cur; s++)
				mix_sel[s] = cur_sel[s];

			    for(int s=0; s < n_new; s++)
				mix_sel[n_cur+s] = new_sel[s];

			    meas_list.setSelectedIndices(mix_sel);
			}
			
		    }
		    catch(DragAndDropEntity.WrongEntityException wee)
		    {
		    }
		}
	    });
	
	    JScrollPane jsp2 = new JScrollPane(meas_list);
	    
	    //panel.add(jsp2);

	    JSplitPane jsplt_pane1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	    jsplt_pane1.setBottomComponent(jsp1);
	    jsplt_pane1.setTopComponent(jsp2);
	    jsplt_pane1.setOneTouchExpandable(true);

	    /*
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = 2.0;
	    c.weighty = 10.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(jsplt_pane1, c);
	    */

	    
	    // =========================================================

	    pro_panel = new ProfilePanel();

	    // pro_panel.addKeyListener(new CustomKeyListener());
	    frame.addKeyListener(new CustomKeyListener());
	    
	    //pro_panel.setPreferredSize(new Dimension(400, 350));
	    
	    JScrollPane jsp3 = new JScrollPane(pro_panel);
	    /*
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    c.weightx = 8.0;
	    c.weighty = 10.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(jsp3, c);
	    */
	    //panel.add(jsp3);

	    JSplitPane jsplt_pane2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	    jsplt_pane2.setLeftComponent(jsplt_pane1);
	    jsplt_pane2.setRightComponent(jsp3);
	    jsplt_pane2.setOneTouchExpandable(true);

	    // ------------

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.weightx = 10.0;
	    c.weighty = 10.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(jsplt_pane2, c);
	    panel.add(jsplt_pane2);


	}

	// ========================================================================================

	{
	    // 
	    // bottom button panel
	    //
	    JButton button = null;
	    
	    JPanel wrapper = new JPanel();
	    wrapper.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrapper.setLayout(w_gridbag);

	    int col = 0;

	    // ------------------
	
	    button = new JButton("Print");
	    button.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			new PrintManager( mview, pro_panel, pro_panel ).openPrintDialog();
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    w_gridbag.setConstraints(button, c);
	    wrapper.add(button);

	    button = new JButton("Make clusters");
	    button.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			makeGroupsIntoClusters(-1, null, null);
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    w_gridbag.setConstraints(button, c);
	    wrapper.add(button);

	    {
		Dimension fillsize = new Dimension(8,8);
		Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
		c = new GridBagConstraints();
		c.gridx = col++;
		w_gridbag.setConstraints(filler, c);
		wrapper.add(filler);
	    }

	    button = new JButton("Help");
	    button.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			mview.getPluginHelpTopic("SuperGrouper", "SuperGrouper");
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    w_gridbag.setConstraints(button, c);
	    wrapper.add(button);

	    button = new JButton("Close");
	    button.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			 cleanUp();
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    w_gridbag.setConstraints(button, c);
	    wrapper.add(button);
	    	
	    
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 2;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(wrapper, c);
	    panel.add(wrapper);
	}

	// ===== axis =====================================================================================

	axis_man = new AxisManager(mview);

	axis_man.addAxesListener( new AxisManager.AxesListener()
	    {
		public void axesChanged() 
		{
		    pro_panel.repaint();
		}
	    });

	axis_man.addAxis(new PlotAxis(mview, "Value"));
	axis_man.addAxis(new PlotAxis(mview, "Count"));

	// ===== decorations ===============================================================================

	deco_man = new DecorationManager(mview, "SuperGrouper");

	deco_man.addDecoListener( new DecorationManager.DecoListener()
	    {
		public void decosChanged() 
		{
		    pro_panel.repaint();
		}
	    });

	// =================================================================================================

    }

    private void savePrefs()
    {
	mview.putBooleanProperty("SuperGrouper.auto_space", auto_space );
	mview.putBooleanProperty("SuperGrouper.col_sel", col_sel );

	mview.putIntProperty( "SuperGrouper.distance_metric", distance_metric_jcb.getSelectedIndex() );

	mview.putBooleanProperty("SuperGrouper.apply_filter", apply_filter_jchkb.isSelected());
	    
	mview.putIntProperty( "SuperGrouper.adjust_what", adjust_what_jcb.getSelectedIndex() );
	mview.putIntProperty( "SuperGrouper.adjust_mode", adjust_mode_jcb.getSelectedIndex() );

	mview.putDoubleProperty( "SuperGrouper.adjust_rate", adjust_rate );
	mview.putDoubleProperty( "SuperGrouper.adjust_noise_level", adjust_noise_level );
	mview.putBooleanProperty("SuperGrouper.adjust_auto_remove_empty", adjust_auto_remove_empty);
    }

    public void cleanUp()
    {
	savePrefs();
	
	kill_action_thread = true;

	edata.removeObserver(this);
	frame.setVisible(false);
    }

    // ============= text field ===============================

    class TextChangeListener implements DocumentListener 
    {
	public void insertUpdate(DocumentEvent e)  { propagate(e); }
	public void removeUpdate(DocumentEvent e)  { propagate(e); }
	public void changedUpdate(DocumentEvent e) { propagate(e); }
	
	private void propagate(DocumentEvent e)
	{
	    setupGroups();
	    updateGroups();
	}
    }

    // ============= measurement list ===============================

    private void populateListWithMeasurements(JList list)
    {
	// save existing selection if any
	Hashtable sels = new Hashtable();
	ListSelectionModel lsm = list.getSelectionModel();
	if(lsm != null)
	{
	    for(int s=lsm.getMinSelectionIndex(); s <= lsm.getMaxSelectionIndex(); s++)
	    {
		if(lsm.isSelectedIndex(s))
		    sels.put( list.getModel().getElementAt(s) , "x");
	    }
	}
	
	// build a vector of names to use as the list data
	Vector data = new Vector();

	for(int m=0; m < edata.getNumMeasurements(); m++)
	{
	    int mi = edata.getMeasurementAtIndex(m);
	    if(edata.getMeasurementShow(mi))
	    {
		data.addElement(edata.getMeasurementName(mi));
	    }
	}
	list.setListData( data );
	
	// update the meas_id map
	meas_ids = new int[ data.size() ];
	int mp = 0;
	for(int m=0; m < edata.getNumMeasurements(); m++)
	{
	    int mi = edata.getMeasurementAtIndex(m);
	    if(edata.getMeasurementShow(mi))
	    {
		meas_ids[mp++] = mi;
	    }
	}

	getMinMax();

	// and restore the selection if there was one
	if(sels.size() > 0)
	{
	    Vector sels_v = new Vector();

	    // check each of the new elements 
	    for(int o=0; o < data.size(); o++)
	    {
		String name = (String) data.elementAt(o);
		if(sels.get(name) != null)
		{
		    sels_v.addElement(new Integer(o));
		}
	    }

	    int[] sel_ids = new int[ sels_v.size() ];
	    for(int s=0; s <  sels_v.size(); s++)
	    {
		sel_ids[s] = ((Integer) sels_v.elementAt(s)).intValue();
	    }

	    list.setSelectedIndices(sel_ids);

	}		
    }



    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  plugin implementation
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void startPlugin()
    {
	edata = mview.getExprData();
	dplot = mview.getDataPlot();

	buildGUI();

	frame.pack();
	frame.setVisible(true);

	// register ourselves with the data
	//
	edata.addObserver(this);

	// sel_handle = edata.addExternalSelectionListener(this);

	new ActionThread().start();
    }

    public void stopPlugin()
    {
	cleanUp();
    }

    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("Super Grouper", "transform", 
					 "Groups and clusters spots by similarity of profile", "",
					 1, 0, 0);
	return pinf;
    }
    public PluginCommand[] getPluginCommands()
    {
	PluginCommand[] com = new PluginCommand[8];
	

	String[] args = new String[] 
	{ 
	    // name                    // type                 //default     // flag   // comment
	    "select",                  "string",               "all",        "",     "either 'all' or 'list'",
	    "measurements",            "measurement_list",     "",          "",     "which Measurements to use for profile",

	    "n_groups",                "integer",   		"9",         "",     "number of groups", 
	    "apply_filter",            "boolean",   		"false",     "",     "", 
	    "metric",                  "string",    		"distance",  "",     "one of 'distance', 'slope' or 'direction'", 
	    "adjust_rate",             "double",    		"0.001",     "",      "", 
	    "adjust_towards",          "string",    		"mean spot", "",      "one of 'mean spot', 'mean profile' or 'random'", 
	    "noise_level",              "double",    		"0.0005",    "",      "", 
	    "adjust_what",             "string",    		"random",    "",      "one of 'random','biggest',smallest',etc", 
	    "auto_remove_empty",       "boolean",   		"false",     "",      "", 
	};

	com[0] = new PluginCommand("start", args);
	com[1] = new PluginCommand("stop",  null);

	com[2] = new PluginCommand("set",   args);
	
	args = new String[] 
	{ 
	    // name                    // type      //default          // flag   // comment
	    "mode",                    "string",   "most accurate",    "m",      "one of 'quickest', 'most accurate' or 'flat'", 
	    "new_name",                "string",   "",                 "",       "name for root of new clusters", 
	};
     	com[3] = new PluginCommand("makeClusters", args);

	args = new String[] 
	{ 
	    // name                    // type      //default   // flag   // comment
	    "iterations",              "integer",   "100",       "",      "how many iterations to perform", 
	};
	com[4] = new PluginCommand("autoAdjust", args);

	com[5] = new PluginCommand("randomise", null);

	com[6] = new PluginCommand("permutations", null);

	com[7] = new PluginCommand("removeEmpty", null);

	return com;
    }
    
    public void   runCommand(String name, String[] args, CommandSignal done) 
    { 
	if(name.equals("stop"))
	{
	    cleanUp();
	    if(done != null)
		done.signal();
	}

	if(name.equals("start") || name.equals("set"))
	{
	    if(frame == null)
		startPlugin();

	    String sel =  mview.getPluginStringArg( "select", args, null );
	    if(sel != null)
	    {
		if(sel.startsWith("li"))
		{
		    String[] m_names = mview.getPluginMeasurementListArg( "measurements", args, null );
		    
		    meas_list.selectItems( m_names);
		}
		else
		{
		    meas_list.selectAll();
		}
	    }
	    
	    if(mview.getPluginIntegerArg( "n_groups", args, -1 ) > 0)
	    {
		n_groups_jtf.setText(mview.getPluginArg( "n_groups", args ));
		setupGroups();
		updateGroups();
	    }

	    String mode = mview.getPluginStringArg( "metric", args, null );
	    if(mode != null)
	    {
		mode = mode.toLowerCase();
		for(int m=0; m < distance_opts.length; m++)
		{
		    String test = distance_opts[m].substring(0,4).toLowerCase();
		    
		    if(mode.startsWith( test ))
			distance_metric_jcb.setSelectedIndex(m);
		}
	    }
	    if(mview.getPluginArg( "apply_filter", args) != null)
		apply_filter_jchkb.setSelected( mview.getPluginBooleanArg( "apply_filter", args, false));
	    
	    String awhat = mview.getPluginStringArg( "adjust_what", args, null );
	    if(awhat != null)
	    {
		awhat = awhat.toLowerCase();

		for(int a=0; a < adjust_what_opts.length; a++)
		{
		    String test = (adjust_what_opts[a].substring(0,2)).toLowerCase();
		    
		    if(awhat.startsWith(test))
			adjust_what_jcb.setSelectedIndex(a);
		}		
		adjust_what = adjust_what_jcb.getSelectedIndex();
	    }
	    
	    String ato = mview.getPluginStringArg( "adjust_towards", args, null );
	    if(ato != null)
	    {
		ato = awhat.toLowerCase();

		for(int a=0; a < adjust_opts.length; a++)
		{
		    String test = adjust_opts[a].toLowerCase();
		    
		    if(ato.equals(test))
			adjust_mode_jcb.setSelectedIndex(a);
		}		
		adjust_mode = adjust_mode_jcb.getSelectedIndex();
	    }
	    

	    if(mview.getPluginArg( "adjust_rate", args) != null)
	    {
		adjust_rate = mview.getPluginDoubleArg( "adjust_rate", args, .0);
		
		rate_slider.setValue( (int)(adjust_rate * 100.0) );
	    }

	    if(mview.getPluginArg( "noise_level", args) != null)
	    {
		adjust_noise_level = mview.getPluginDoubleArg( "noise_level", args, .0);
		
		noise_slider.setValue( (int)(adjust_noise_level * 1000.0) );

		// noise_label.setText( mview.niceDouble( adjust_noise_level, 6, 4 ) );
	    }
	    	    
	    if(mview.getPluginArg( "auto_remove_empty", args) != null)
	    {
		adjust_auto_remove_empty = mview.getPluginBooleanArg( "auto_remove_empty", args, false);
		auto_remove_empty_jchkb.setSelected( adjust_auto_remove_empty );
	    }

	    if(done != null)
		done.signal();
	}
	
	if(name.equals("removeEmpty"))
	{
	    removeEmptyGroups();
	    updateGroups();
	    if(done != null)
		done.signal();
	}


	if(name.equals("randomise"))
	{
	    randomiseGroupProfiles();
	    updateGroups();
	    if(done != null)
		done.signal();
	}


	if(name.equals("permutations"))
	{
	    setGroupsByPermutation();
	    updateGroups();
	    if(done != null)
		done.signal();
	}


	if(name.equals("autoAdjust"))
	{
	    if(frame == null)
		startPlugin();

	    iterate_command_counter = mview.getPluginIntegerArg( "iterations", args, 100);

	    iterating_by_command = true;
	    
	    iterate_command_done = done;

	    startStopIterate();
	}

	if(name.equals("makeClusters"))
	{
	    String mstr = mview.getPluginStringArg( "mode", args, "");

	    int mode = 0; // flat
	    
	    if(mstr != null)
	    {
		if(mstr.startsWith("mo"))
		    mode = 1;

		if(mstr.startsWith("qu"))
		    mode = 2;
	    }
	    
	    String new_name = mview.getPluginStringArg( "new_name", args, null);
	    
	    makeGroupsIntoClusters( mode, new_name, done );
	}
	
    } 

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  observer 
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // the observer interface of ExprData notifies us whenever something
    // interesting happens to the data as a result of somebody (including us) 
    // manipulating it
    //
    public void dataUpdate(ExprData.DataUpdateEvent due)
    {
	switch(due.event)
	{
	case ExprData.ColourChanged:
	case ExprData.OrderChanged:
	case ExprData.VisibilityChanged:
	    break;

	case ExprData.SizeChanged:
	case ExprData.ValuesChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	case ExprData.RangeChanged:
	    allocateSpotsToGroups( group_profile, group_allocation, group_count, group_score );
	    //listSelectionHasChanged();
	    pro_panel.repaint();
	    break;
	}
    }

    public void clusterUpdate(ExprData.ClusterUpdateEvent cue)
    {
    }

    public void measurementUpdate(ExprData.MeasurementUpdateEvent mue)
    {
	switch(mue.event)
	{
	case ExprData.VisibilityChanged:
	case ExprData.SizeChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	case ExprData.NameChanged:
	case ExprData.OrderChanged:
	    populateListWithMeasurements(meas_list);
	    listSelectionHasChanged();
	    break;
	}
    }

    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
	background_col = mview.getBackgroundColour();
	text_col       = mview.getTextColour();
	pro_panel.repaint();
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private void listSelectionHasChanged()
    {
	int[] sel_inds = meas_list.getSelectedIndices();

	meas_ids = new int[ sel_inds.length ];
	int mp = 0;

	for(int m=0; m < edata.getNumMeasurements(); m++)
	{
	    int mi = edata.getMeasurementAtIndex(m);
	    if(meas_list.isSelectedIndex( m ))
	    {
		meas_ids[mp++] = mi;
	    }
	}

	getMinMax();

	// updateProfiles();
	
	setupGroups(); // pro_panel.repaint();

	allocateSpotsToGroups( group_profile, group_allocation, group_count, group_score );
		
	pro_panel.repaint( );
    }

    /*
    private void treeSelectionHasChanged()
    {
	
	// how many selected things?
	
	TreeSelectionModel tsm = (TreeSelectionModel) cl_tree.getSelectionModel();

	int mi = tsm.getMinSelectionRow();
	int ma = tsm.getMaxSelectionRow();

	int count = 0;

	storePosition();

	sel_cls.clear();

	for(int s=mi; s <= ma; s++)
	{
	    if(tsm.isRowSelected(s))
	    {
		count++;
		TreePath tp = cl_tree.getPathForRow(s);
		DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) tp.getLastPathComponent();
		ExprData.Cluster cl = (ExprData.Cluster) dmtn.getUserObject();
		sel_cls.addElement(cl);
	    }
	}

	n_cols = 1;
	n_rows = 0;

	if(count > 0)
	{
	    // find the nicest factors of 'count'
	    
	    // pick the two factors that are closest to one another
	    
	    double best_rc_diff = Double.MAX_VALUE;
	    
	    for(int m=1; m < count; m++)
	    {
		double c = (double) m;
		double r = ((double) count) / c;
		
		double rc_diff = Math.abs(r - c);
		if(rc_diff <  best_rc_diff)
		{
		    best_rc_diff= rc_diff;
		    n_cols = (int) c;
		}
	    }
	    
	    n_rows = (n_cols > 0) ? (int)(Math.ceil((double)count / (double)n_cols)) : count;
	}

	// System.out.println("count=" + count + " layout=" + n_cols + "x" + n_rows);
	
	updateProfiles();

	pro_panel.repaint();
    }
    */

    private void updateProfiles()
    {
	sel_ids.clear();

	for(int s=0; s < sel_cls.size(); s++)
	{
	    ExprData.Cluster cl = (ExprData.Cluster) sel_cls.elementAt(s);
	    int[] ids = include_children ? cl.getAllClusterElements() :  cl.getElements();
	    sel_ids.addElement(ids);
	}
	
	
    }

    /*
    private void selectClusterInTree(final JTree tree, final ExprData.Cluster cl)
    {
	DefaultTreeModel model      = (DefaultTreeModel)       tree.getModel();
	DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
	for (Enumeration en =  root.depthFirstEnumeration(); en.hasMoreElements() ;) 
	{
	    DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) en.nextElement();
	    ExprData.Cluster ncl = (ExprData.Cluster) dmtn.getUserObject();
	    if(cl == ncl)
	    {
		TreeNode[] tn_path = model.getPathToRoot(dmtn);
		TreePath tp = new TreePath(tn_path);
		
		tree.scrollPathToVisible(tp);
		tree.setSelectionPath(tp);
	    }
	}
    }

    private void selectClustersInTree(final JTree tree, final Hashtable clusters)
    {
	Vector tree_paths_v = new Vector();
	DefaultTreeModel model      = (DefaultTreeModel)       tree.getModel();
	DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();

	for (Enumeration en =  root.depthFirstEnumeration(); en.hasMoreElements() ;) 
	{
	    
	    DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) en.nextElement();
	    ExprData.Cluster ncl = (ExprData.Cluster) dmtn.getUserObject();

	    ExprData.Cluster cl = (ExprData.Cluster) clusters.get(ncl);

	    if(cl != null)
	    {
		TreeNode[] tn_path = model.getPathToRoot(dmtn);
		TreePath tp = new TreePath(tn_path);
		
		tree_paths_v.addElement(tp);
	    }
	}
	if(tree_paths_v.size() > 0)
	{
	    tree.setSelectionPaths((TreePath[]) tree_paths_v.toArray( new TreePath[tree_paths_v.size()] ));
	    tree.scrollPathToVisible((TreePath) tree_paths_v.elementAt(0));
	    treeSelectionHasChanged();
	}
    }

    private void expandClusterInTree(final JTree tree, final ExprData.Cluster cl)
    {
	// select cluster 'cl' and "some" of its children....
	int max = 10;
	
	if(cl.getNumChildren() >= 10)
	    max = cl.getNumChildren() + 1;

	Vector start = new Vector();
	start.addElement(cl);

	Vector clusters = new Vector();

	addClusters( 0, max, start, clusters );

	System.out.println("decided to add " + clusters.size() + " clusters...");

	Hashtable cl_ht = new Hashtable();

	for(int c=0; c < clusters.size(); c++)
	    cl_ht.put( clusters.elementAt(c), clusters.elementAt(c));

	selectClustersInTree( cl_tree, cl_ht );
    }

    private void addClusters( final int depth, final int space_left, Vector cl_v, final Vector vec )
    {
	//System.out.println("depth:" + depth + ": has "+  cl_v.size() + " clusters");

	// is there enough space to add all the clusters in the vector 'cl_v'?
	final int size = cl_v.size();
	if(size <  space_left)
	{
	    //System.out.println("depth:" + depth + ": adding these "+  cl_v.size() + " clusters");

	    for(int c=0; c < size; c++)
		vec.addElement( cl_v.elementAt( c ));
	}
	else
	{
	    //System.out.println("depth:" + depth + ": no room for these "+  cl_v.size() + " clusters");

	    return;
	}

	// might there be any room for the children?
	if((space_left-size) > 0)
	{
	    //System.out.println("depth:" + depth + ": checking for next depth....");

	    // now compose a vector of the children of all of the clusters in the vector 'cl_v'
	    Vector all_ch_v = new Vector();
	    for(int c=0; c < size; c++)
	    {
		Vector ch_v = ((ExprData.Cluster)cl_v.elementAt(c)).getChildren();
		if(ch_v != null)
		    for(int cc=0; cc < ch_v.size(); cc++)
			all_ch_v.addElement( ch_v.elementAt( cc ));
	    }
	    
	    //System.out.println("depth:" + depth + ": next depth will have "+  all_ch_v.size() + " clusters");
	    
	    // and try to add this
	    if(all_ch_v.size() > 0)
		addClusters( depth+1, space_left - size, all_ch_v, vec );
	}
    }
    */

    // ----------------------------------------------------------------------------------------------
    //
    //  tree expander
    //
    // ----------------------------------------------------------------------------------------------

    /*
    private final void openTreeToDepth(final int depth)
    {
	final JTree target = cl_tree;
	if(target == null)
	    return;
	final DefaultTreeModel model      = (DefaultTreeModel)       target.getModel();
	if(model == null)
	    return;
	final DefaultMutableTreeNode node = (DefaultMutableTreeNode) model.getRoot();

	// fully collapse the tree...
	int rc = target.getRowCount();
	while(rc > 0)
	{
	    if(target.isExpanded(rc))
		target.collapseRow(rc);
	    rc--;
	}
	
	if(depth > 0)
	    openNodeToDepth(target, model, node, depth);


    }

    private final void openNodeToDepth(final JTree target, final DefaultTreeModel model, 
				       final DefaultMutableTreeNode node, final int depth)
    {
	TreeNode[] tn_path = model.getPathToRoot(node);
	TreePath tp = new TreePath(tn_path);
	
	if(depth > 0)
	{
	    target.expandPath(tp);
	    final int n_c = node.getChildCount();
	    for(int c=0; c < n_c; c++)
	    {
		openNodeToDepth(target, model, (DefaultMutableTreeNode) node.getChildAt(c), depth-1);
	    }
	}
	else
	{
	    target.collapsePath(tp);
	}
    }


    private final void openTreeToDepth(final JTree target, final int depth)
    {
	if(target == null)
	    return;
	final DefaultTreeModel model      = (DefaultTreeModel)       target.getModel();
	if(model == null)
	    return;
	final DefaultMutableTreeNode node = (DefaultMutableTreeNode) model.getRoot();

	// fully collapse the tree...
	int rc = target.getRowCount();
	while(rc > 0)
	{
	    if(target.isExpanded(rc))
		target.collapseRow(rc);
	    rc--;
	}
	
	if(depth > 0)
	    openNodeToDepth(target, model, node, depth);


    }
    */

    // ----------------------------------------------------------------------------------------------
    //
    //  keyboard shortcuts
    //
    // ----------------------------------------------------------------------------------------------


    public class CustomKeyListener implements KeyListener
    {
	public void keyTyped(KeyEvent e) 
	{
	}
	
	public void keyPressed(KeyEvent e) 
	{
	}
	
	public void keyReleased(KeyEvent e) 
	{
	    handleKeyEvent(e);
	}
	public void keyAction(KeyEvent e) 
	{
	}

	protected void handleKeyEvent(KeyEvent e)
	{
	    
	    int group = (pro_panel.last_pos == null) ? -1 :  pro_panel.last_pos[0];
	    
	    // System.out.println("Key! in group " + group);

	    if(group < 0)
		return;

	    switch(e.getKeyCode())
	    {
	    case KeyEvent.VK_S:
		selectAllInGroup(group);
		break;


	    case KeyEvent.VK_C:
		copyGroup(group);
		
		break;
	    case KeyEvent.VK_P:
		pasteGroup(group);
		allocateSpotsToGroups( group_profile, group_allocation, group_count, group_score );
		pro_panel.repaint( );
		break;


	    case KeyEvent.VK_L:
		group_lock[ group ] = true;
		pro_panel.repaint( );
		break;

	    case KeyEvent.VK_U:
		group_lock[ group ] = false;
		pro_panel.repaint( );
		break;

	    case KeyEvent.VK_R:  // randomise
		randomiseGroupProfile(group);
		allocateSpotsToGroups( group_profile, group_allocation, group_count, group_score );
		pro_panel.repaint( );
		break;
		
	    case KeyEvent.VK_I:  // invert
		final double min = current_min; //edata.getMinEValue();
		final double max = current_max; //edata.getMaxEValue();
		final double range = max - min;
    
		for(int m = 0; m < n_meas; m++)
		{
		    double p  = group_profile[group][m] - min;   // p is 0....range
		    double ip = range - p;
		    group_profile[group][m] = ip + min;
		}

		allocateSpotsToGroups( group_profile, group_allocation, group_count, group_score );

		pro_panel.repaint( );
		break;


	    }
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   ExternalSelectionListener
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void spotSelectionChanged(int[] spot_ids)
    {
	pro_panel.repaint();
    }

    public void clusterSelectionChanged(ExprData.Cluster[] clusters) { }
    public void spotMeasurementSelectionChanged(int[] spot_ids, int[] meas_ids) { }
    
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   ProfilePanel
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public DragGestureRecognizer drag_gesture_recogniser = null;

    public class ProfilePanel extends JPanel
	                      implements DropTargetListener,DragSourceListener,DragGestureListener,
					 MouseListener, MouseMotionListener, Printable
	                      
    {
	public DragSource drag_source = null;
	public DropTarget drop_target = null;

	private Point last_pt = null;

	private double scale = 1.0;

	private Polygon[] glyph_poly = null;
	private int glyph_poly_height;

	private String tool_tip_text = null;

	private boolean dragging_profile = false;
	private int[] drag_data = null;

	private int[] last_pos = null;

	public ProfilePanel()
	{
	    drop_target = new DropTarget (this, this);
	    drag_source = DragSource.getDefaultDragSource();
	    drag_gesture_recogniser = drag_source.createDefaultDragGestureRecognizer( this, DnDConstants.ACTION_MOVE, this);
	    
	    addMouseListener(this);
	    addMouseMotionListener(this);

	    sel_colour = text_col.brighter();
	    unsel_colour = text_col.darker();
	}

	// --------------------------------------------------------------------------------------

	//
	// === drop target listener ==============================================
	//
	
	public void dragEnter (DropTargetDragEvent event) 
	{
	    //System.out.println( "DropTarget:dragEnter");
	    event.acceptDrag (DnDConstants.ACTION_MOVE);
	}
	
	public void dragExit (DropTargetEvent event) 
	{
	    //System.out.println( "DropTarget:dragExit");
	}
	
	public void dragOver (DropTargetDragEvent event) 
	{
	    //System.out.println( "DropTarget:dragOver");
	    //event.acceptDrag (DnDConstants.ACTION_COPY_OR_MOVE);
	}
	
	public void drop (DropTargetDropEvent event) 
	{
	    // System.out.println( "DropTarget:drop attempted");

	    // System.out.println("Drop!");
	    
	    try // is it a cluster?
	    {
		Transferable transferable = event.getTransferable();
		
		DragAndDropEntity dnde = null;
		String text = null;
		
		if(event.isLocalTransfer() == false)
		{
		    System.out.println( "transfer between apps...");
		    event.rejectDrop();
		}
		else
		{
		    if(transferable.isDataFlavorSupported (DragAndDropEntity.DragAndDropEntityFlavour))
		    {
			event.acceptDrop(DnDConstants.ACTION_MOVE);
			
			dnde = (DragAndDropEntity) transferable.getTransferData(DragAndDropEntity.DragAndDropEntityFlavour);
			
			try
			{
			    ExprData.Cluster drop_c = dnde.getCluster();
			    //System.out.println("cluster dropped");
			    int[] sids = drop_c.getAllClusterElements();
			    if(sids != null)
			    {
				// show the labels for each spot in the cluster
				
				for(int s=0;s < sids.length; s++)
				    showSpotLabel(sids[s]);
				
				repaint();
			    }
			}
			catch(DragAndDropEntity.WrongEntityException wee)
			{
			    try // or a spot?
			    {
				int[] sids = dnde.getSpotIds();
				if(sids != null)
				{

				    if(sids.length == 1)
				    {
					//	System.out.println("1 spot drop");
					
					int[] loc = pro_panel.locateMouse(  event.getLocation() );
					
					if(loc != null)
					{
					    // System.out.println("1 spot drop on group " + loc[0] );

					    setGroupProfile( loc[0], sids[0] );

					    allocateSpotsToGroups( group_profile, group_allocation, group_count, group_score );
		
					    repaint( );
					}
				    }
				    else
				    {
					for(int s=0;s < sids.length; s++)
					    showSpotLabel(sids[s]);
					repaint();
				    }
				   
				}
				//System.out.println("spot dropped");
				// showSpotLabel(sid);
				
				//repaint();
				
				
			    }
			    catch(DragAndDropEntity.WrongEntityException wee2)
			    {
				// not a cluster or spot, ignore it
			    }
			}
		    }
		}
	    }
	    catch (IOException exception) 
	    {
		exception.printStackTrace();
		System.err.println( "Exception" + exception.getMessage());
	    } 
	    catch (UnsupportedFlavorException ufException ) 
	    {
		ufException.printStackTrace();
		System.err.println( "Exception" + ufException.getMessage());
	    }
	}
	
	public void dropActionChanged ( DropTargetDragEvent event ) 
	{
	    //System.out.println( "DropTarget:dropActionChanged");
	}
 
	//
	// === DragGesture listener ==============================================
	//

	public void dragGestureRecognized( DragGestureEvent event) 
	{
	    DragAndDropEntity dnde = null;

	    if((root_profile_picker != null) && (!edit_profiles_jchkb.isSelected()))
	    {
		Point pt = event.getDragOrigin();
		int sid = root_profile_picker.findProfile(pt.x, pt.y);
		if(sid >= 0)
		{
		    dnde = DragAndDropEntity.createSpotNameEntity(sid);
		}
	    }
	    
	    if(dnde != null) 
	    {
		//System.out.println( " drag start....(7)");
		
		try
		{
		    drag_source.startDrag (event, DragSource.DefaultMoveDrop, dnde, this);
		}
		catch(java.awt.dnd.InvalidDnDOperationException ide)
		{
		    System.out.println( " BAD drag!\n" + ide);
		    
		    System.out.println( " trying to restart DnD system");

		    drag_gesture_recogniser.resetRecognizer();
		}
	    }
	    else
	    {
		System.out.println( " null drag....");
	    }
	}
	
	//
	// === DragSource listener ==============================================
	//

	public void dragDropEnd (DragSourceDropEvent event) 
	{   
	    //System.out.println( "DragSource:dragDropEnd");
	}
	
	public void dragEnter (DragSourceDragEvent event) 
	{
	    //System.out.println( "CustomDragListener:DragSource:dragEnter");
	}
	
	public void dragExit (DragSourceEvent event) 
	{
	    //System.out.println( "CustomDragListener:DragSource:dragExit");
	}
	
	public void dragOver (DragSourceDragEvent event) 
	{
	    //System.out.println( "CustomDragListener:DragSource:dragOver");
	}
	
	public void dropActionChanged ( DragSourceDragEvent event) 
	{
	    //System.out.println( "CustomDragListener:DragSource:dropActionChanged");
	}
	
	
	//
	// === MouseMotion listener ==============================================
	//
    	
	public void mouseMoved(MouseEvent e) 
	{
	    Point pt = new Point();
	    double xval, yval, xval_t, yval_t;

	    pt.x  = e.getX();
	    pt.y  = e.getY();

	    last_pos = locateMouseEvent( e );

	    if(root_profile_picker != null)
	    {
		int sid = root_profile_picker.findProfile(pt.x, pt.y);
		if(sid >= 0)
		{
		    nt_sel = nts.getNameTagSelection();
		    
		    String str = nt_sel.getNameTag(sid);
		   
		    tool_tip_text = str;
		    
		    setToolTipText(str);
		}
		else
		{
		    tool_tip_text = null;
		}
	    }
	}

	public String getToolTipText(MouseEvent event)
	{
	    return tool_tip_text;
	}

	public void mouseDragged(MouseEvent e) 
	{
	    if(dragging_profile)
	    {
		// System.out.println("dragging....");

		double new_val = getDragValue( drag_data, e );

		if( drag_data[1] >= 0 )
		{
		    group_profile[ drag_data[0] ][ drag_data[1] ] = new_val;
		    
		    repaint( );
		}
	    }
	} 

	private void addMenuEntry(JPopupMenu menu, String name, int group, int code)
	{
	    JMenuItem jmi = new JMenuItem(name);
	    jmi.addActionListener(new CustomMenuListener(group, code));
	    menu.add(jmi);
		        
	}

	public void mousePressed(MouseEvent e) 
	{
	    if(!is_iterating)
	    {
		if(e.isPopupTrigger() || e.isAltDown() || e.isMetaDown() || e.isAltGraphDown()) 
		{
		    if(last_pos == null)
			return;
		    int group = last_pos[0];
		    if(group < 0)
			return;

		    JPopupMenu group_popup = new JPopupMenu();

		    addMenuEntry(group_popup, "Select All", group, 0);
		    group_popup.addSeparator();
		    addMenuEntry(group_popup, "Lock", group, 1);
		    addMenuEntry(group_popup, "Copy", group, 2);
		    addMenuEntry(group_popup, "Paste", group, 3);
		    group_popup.addSeparator();
		    addMenuEntry(group_popup, "Remove", group, 4);
		    addMenuEntry(group_popup, "Randomise", group, 5);
		    
		    group_popup.show(e.getComponent(), e.getX(), e.getY());
		}
		else
		{
		    if( edit_profiles_jchkb.isSelected() )
		    {
			if(!dragging_profile)
			{
			    drag_data = locateMouseEvent( e );
			    
			    if(drag_data != null)
				dragging_profile = true;
			}
		    }
		}
	    }
	}
	
	public void mouseReleased(MouseEvent e) 
	{
	    if(e.isPopupTrigger() || e.isAltDown() || e.isMetaDown() || e.isAltGraphDown()) 
	    {
		
		
	    }
	    else
	    {
		if(dragging_profile)
		{
		    dragging_profile = false;
		    
		    allocateSpotsToGroups( group_profile, group_allocation, group_count, group_score );
		    
		    repaint( );
		}
	    }
	}
	
	public void mouseEntered(MouseEvent e) 
	{
	}

	public void mouseExited(MouseEvent e) 
	{
	}

	public void mouseClicked(MouseEvent e) 
	{
	    if(e.isPopupTrigger() || e.isAltDown() || e.isMetaDown() || e.isAltGraphDown()) 
	    {
	    }
	    else
	    {
		if( ! edit_profiles_jchkb.isSelected() )
		{
		    if(root_profile_picker != null)
		    {
			int sid = root_profile_picker.findProfile( e.getX(), e.getY() );
			if(sid >= 0)
			{
			    toggleSpotLabel(sid);
			}
		    }
		}
	    }
	}

	private int[] locateMouseEvent(MouseEvent e) 
	{
	    return locateMouse( e.getX(), e.getY() );
	}
	private int[] locateMouse(java.awt.Point p) 
	{
	    return locateMouse( p.x, p.y );
	}

	private int[] locateMouse(int x, int y) 
	{
	    if(!is_iterating)
	    {
		//if( edit_profiles_jchkb.isSelected() )
		{
		    //System.out.println("click");

		    if(( graph_sx == 0 ) || ( graph_sy == 0 ))
			return null;

		    int gx = x / graph_sx;
		    int gy = y / graph_sy;
		    
		    if((gx <= n_cols) && (gy <= n_rows))
		    {
			int group = (gy * n_cols) + gx;
			
			if(group < n_groups)
			{
			    // System.out.println("click in group " + group);

			    int[] result = new int[2];
			    result[0] = group;
			    result[1] = -1;

			    // was it near a group profile handle ?
			    
			    final int yp = graph_y[group] + graph_h;
			    int exp = graph_x[group];
			    
			    for(int m=0; m < n_meas; m++)
			    {
				final double eval = group_profile[group][m];
				final int eyp = yp - (int)((eval - graph_y_axis_min) * graph_y_axis_scale);
				
				final int dist_x = (x - exp);
				final int dist_y = (y - eyp);
				final int dist_sq = (dist_x*dist_x) + (dist_y*dist_y);
				if(dist_sq < 9)
				{
				    // System.out.println("click in group profile, m=" + m);

				    result[1] = m;

				    return result;
				}
				exp += graph_x_axis_step;
			    }
			    return result;
			}
		    }
		}

		
	    }
	    return null;
	}

	private double getDragValue( int[] drag_data, MouseEvent e )
	{
	    int group = drag_data[0];
	    
	    if(group < n_groups)
	    {
		
		final int yp =  graph_h - (e.getY() - graph_y[group]);
		
		double dyp = ((double) yp / graph_y_axis_scale) + graph_y_axis_min;
		
		if(dyp < current_min /*edata.getMinEValue()*/)
		    dyp = current_min  /*edata.getMinEValue()*/;
		if(dyp > current_max /*edata.getMaxEValue()*/)
		    dyp = current_max /*edata.getMaxEValue()*/;
		
		return dyp;
	    }

	    return .0;
	}

	// // // // // // // // // // // // // // // // // // // // // // // // 

	public class CustomMenuListener implements ActionListener
	{
	    private int item, group;
	    
	    public CustomMenuListener(int group_, int item_)
	    {
		group = group_;
		item = item_;
	    }
	    
	    public void actionPerformed(ActionEvent e) 
	    {    
		switch(item)
		{
		case 0: // Select All
		    selectAllInGroup(group);

		    break;
		case 1: // Lock/Unlock
		    break;


		case 2: // Copy
		    copyGroup(group);
		    break;
		case 3: // Paste
		    pasteGroup(group);
		
		    allocateSpotsToGroups( group_profile, group_allocation, group_count, group_score );
		    
		    pro_panel.repaint( );
		    break;


		case 4: // Remove
		    removeGroup(group);
		    allocateSpotsToGroups( group_profile, group_allocation, group_count, group_score );
		    pro_panel.repaint( );
		    break;

		case 5: // Randomise
		    randomiseGroupProfile(group);
		    allocateSpotsToGroups( group_profile, group_allocation, group_count, group_score );
		    pro_panel.repaint( );
		    break;

		}
	    }
	}
	

	// // // // // // // // // // // // // // // // // // // // // // // // 

	public void paintComponent(Graphics graphic)
	{
	    if(graphic == null)
		return;

	    if(sel_colours == null)
	    {
		sel_colours = new Color[n_sel_colours];
		sel_colours[0] = Color.red;
		sel_colours[1] = Color.green;
		sel_colours[2] = Color.blue;
		sel_colours[3] = Color.yellow;
		sel_colours[4] = Color.magenta;
		sel_colours[5] = Color.cyan;
		sel_colours[6] = Color.pink;
		sel_colours[7] = Color.white;
	    }

	    drawProfiles(graphic, getWidth(), getHeight());

	    
	}

	public int print(Graphics g, PageFormat pf, int pg_num) throws PrinterException 
	{
	    // margins
	    //
	    g.translate((int)pf.getImageableX(), 
			(int)pf.getImageableY());
	    
	    // area of one page
	    //
	    // ??  area seems to be too small, c.f. ScatterPlot...
	    //
	    int pw = (int)(pf.getImageableWidth() - pf.getImageableX());   
	    int ph = (int)(pf.getImageableHeight() - pf.getImageableY());
	    
	    //System.out.println("PRINT REQUEST for page " + pg_num + " size=" + pw + "x" + ph); 
	    
	    frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

	    drawProfiles(g, pw, ph );

	    frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		
	    // panel.paintIntoRegion(g, pw, ph);

	    return (pg_num > 0) ? NO_SUCH_PAGE : PAGE_EXISTS;
	}

	public void drawProfiles(Graphics graphic, int width, int height)
	{
	    try
	    {
		
		graphic.setColor(background_col);
		graphic.fillRect(0, 0, width, height);
		graphic.setColor(text_col);
		graphic.setFont(dplot.getFont());
		
		root_profile_picker = new ProfilePicker();
		root_profile_picker.setupPicker( width, height );

		if((n_cols > 0) && (n_rows > 0))
		{
		    
		    graph_sx = (int)((double)width * scale)  / n_cols;
		    graph_sy = (int)((double)height * scale) / n_rows;
		    
		    graph_w = (int)((double)graph_sx * 0.8 * scale);
		    graph_h = (int)((double)graph_sy * 0.8 * scale);
		    
		    ticklen = graph_h / 20;
		    
		    int col = 0;
			
		    xoff = (int)((double)graph_sx * 0.1 * scale);
		    yoff = (int)((double)graph_sy * 0.1 * scale);
		    
		    int xp = xoff;
		    int yp = yoff;
		    
		    if(meas_ids.length < 2)
			return;
		    
		    int[] ssel =  edata.getSpotSelection();
		    coloured = (ssel.length > 0);

		    double min = current_min /* edata.getMinEValue() */ ;
		    double max = current_max /* edata.getMaxEValue() */ ;
		    if(!dragging_profile)
		    {
			font = new Font("Helvetica", 1, (int)( (double)ticklen * scale * spot_font_scale));
			frc = new FontRenderContext(null, false, false);
			
			String str = mview.niceDouble(current_min, 9, 4);
			min_label = new TextLayout( str, font, frc);
			min_label_o = (int)(min_label.getBounds().getWidth() / 2.0);
			
			zero_label = new TextLayout( "0.0", font, frc);
			zero_label_o = (int)(zero_label.getBounds().getWidth() / 2.0);
		    
			str = mview.niceDouble(current_max, 9, 4);
			max_label = new TextLayout( str, font, frc);
			max_label_o = (int)(max_label.getBounds().getWidth() / 2.0);
			
			draw_zero = current_min < .0;
			
			// apply_filter = apply_filter_jchkb.isSelected();
			
			meas_labels   = new TextLayout[meas_ids.length];
			meas_labels_o = new int[meas_ids.length];
			
			for(int m=0; m < meas_ids.length; m++)
			{
			    str = edata.getMeasurementName( meas_ids[m] );
			    meas_labels[m] = new TextLayout( str, font, frc);
			    meas_labels_o[m] = (int)(meas_labels[m].getBounds().getWidth() / 2.0);
			}
			
			
			graph_x_axis_step = graph_w / (meas_ids.length-1);
			
			
			//if(uniform_scale)
			{
			    graph_y_axis_min   = min;
			    graph_y_axis_scale = (double) graph_h / (max - min);
			}
			
			if((glyph_poly == null) || (glyph_poly_height != ticklen))
			{
			    // generate (or re-generate at a new size) the glyphs
			    glyph_poly = mview.getDataPlot().getScaledClusterGlyphs(ticklen);
			    glyph_poly_height = ticklen;
			}
		    
			String name = null;
			Graphics2D g2 = (Graphics2D) graphic;
			
			resetLabelMap(width, height);
			colour_alloc = 0;
		    }

			
		    //for(int s=0; s < sel_cls.size(); s++)
		    for(int g=0; g < n_groups; g++)
		    {
			graphic.setColor( text_col );
			
			graph_x[g] = xp;
			graph_y[g] = yp;
			
			drawAxes( graphic, group_lock[g], xp, yp, graph_w, graph_h, min, max );
			
			xp += graph_sx;
			
			if(++col == n_cols)
			{
			    col = 0;
			    yp += graph_sy;
			    xp = xoff;
			}
		    }
		    
		    if(!dragging_profile)
		    {
			graphic.setColor( text_col.darker().darker() );
			drawSpotProfiles(graphic, width, height);
		    }
		    
		    graphic.setColor( text_col.brighter().brighter() );
		    drawGroupProfiles(graphic, width, height);
		}
		

		deco_man.drawDecorations(graphic, width, height);
	    }
	    catch(Throwable th)
	    {
		th.printStackTrace();
	    }
	}

	private void drawGroupProfiles(Graphics graphic,  int width, int height)
	{
	    if(group_allocation == null)
		return;
	    
	    // System.out.println( " drawGroupProfiles() : min="+ graph_y_axis_min + " scale=" + graph_y_axis_scale);


	    final boolean draw_handles = !is_iterating && (edit_profiles_jchkb.isSelected());

	    for(int g=0; g < n_groups; g++)
	    {
		final int yp = graph_y[g] + graph_h;
		int exp = graph_x[g];
		
		int last_eyp = 0;
		int last_exp = 0;
		
		for(int m=0; m < meas_ids.length; m++)
		{
		    final double eval = group_profile[g][m];
		    int eyp = yp - (int)((eval - graph_y_axis_min) * graph_y_axis_scale);
		 
		    if(m > 0)
		    {
			graphic.drawLine( last_exp, last_eyp, exp, eyp);
		    }

		    if(draw_handles)
		    {
			graphic.drawRect( exp-1, eyp-1, 3, 3 );
		    }

		    last_exp = exp;
		    last_eyp = eyp;
		    
		    exp += graph_x_axis_step;
		}
	    }

	}

	private void drawSpotProfiles(Graphics graphic,  int width, int height)
	{
	    if(group_allocation == null)
		return;

	    final int ns = group_allocation.length;

	    Graphics2D g2 = null;
	    int spot_label_w = 0;
	    int spot_label_h = 0;
	    int spot_label_o = 0;
	    TextLayout spot_label = null;

	    Color normal_colour = text_col.darker().darker();
	    Color draw_colour;

	    for(int s = 0 ; s < ns; s++)
	    {
		final int g =  group_allocation[s];
		
		if((g >= 0) && (g < n_groups))
		{
		    final int yp = graph_y[g] + graph_h;
		    int exp = graph_x[g];
		    
		    int last_eyp = 0;
		    int last_exp = 0;
		    
		    final int label_col = getSpotLabel(s);
		    boolean show_label = (label_col >= 0);
		    draw_colour = normal_colour;

		    if(show_label)
		    {
			String label = nt_sel.getNameTag(s);
			if((label == null) || (label.length() == 0))
			{
			    show_label = false;
			}
			else
			{
			    spot_label = new TextLayout( label, font, frc);
			    spot_label_w = (int)spot_label.getBounds().getWidth();
			    spot_label_h = (int)spot_label.getBounds().getHeight();
			    spot_label_o = spot_label_w / 2;
			    
			    if(g2 == null)
				g2 = (Graphics2D) graphic;

			    draw_colour = sel_colours[ label_col ];
			}
		    }


		    graphic.setColor( draw_colour );

		    
		    for(int m=0; m < meas_ids.length; m++)
		    {
			final double eval = edata.eValue(meas_ids[m], s);
			int eyp = yp - (int)((eval - graph_y_axis_min) * graph_y_axis_scale);
			
			
			if(m > 0)
			{
			    if(root_profile_picker != null)
				root_profile_picker.addSegment(last_exp, last_eyp, exp, eyp, s);
			    
			    graphic.drawLine( last_exp, last_eyp, exp, eyp);
			}
			
			if(show_label)
			{
			    // put the label underneath when the value is rising
			    int label_y = eyp;
			    int label_x = exp-spot_label_o;
			    
			    if((m+1) < meas_ids.length)
			    {
				if( eval < edata.eValue(meas_ids[m+1], s) )
				    label_y += spot_label_h;
			    }
			    else
			    {
				// put the last label underneath when the value is falling
				if( eval < edata.eValue(meas_ids[m-1], s) )
				    label_y += spot_label_h;
			    }
			    
			    if(spaceForLabel( 18, spot_label_w, spot_label_h, label_x, label_y ))
			    {
				graphic.setColor( background_col );
				graphic.fillRect( label_x, label_y-spot_label_h, spot_label_w, spot_label_h);
				graphic.setColor( draw_colour );
				
				AffineTransform new_at = new AffineTransform();
				new_at.translate(label_x, label_y);
				Shape shape = spot_label.getOutline(new_at);
				g2.fill(shape);
				
				storeLabelExtent( spot_label_w, spot_label_h, label_x, label_y );
			    }
			}
			
			last_exp = exp;
			last_eyp = eyp;
			
			exp += graph_x_axis_step;
		    }
		}
	    }
	}

	// ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 

	private int colour_alloc = 0;
	private Color[] sel_colours = null;

	private void drawElementsInto( Graphics graphic,  ExprData.Cluster cl, int[] ids, 
				       boolean do_sel, boolean is_sel, 
				       int xp, int yp, int w, int h )
	{
	    if(cl.getIsSpot())
	    {
		Graphics2D g2 = null;

		int spot_label_w = 0;
		int spot_label_h = 0;
		int spot_label_o = 0;
		TextLayout spot_label = null;

		
		for(int e=0; e < ids.length; e++)
		{
		    final int sid = ids[e];
		    final int sel_colour_id = getSpotLabel(sid);
		    final boolean spot_is_sel = (sel_colour_id >= 0);

		    if((do_sel == false) || (spot_is_sel == is_sel))
		    {
			boolean show_label = spot_is_sel;
			String label = spot_is_sel ? nt_sel.getNameTag(sid) : null;
			if((label == null) || (label.length() == 0))
			    show_label = false;
			
			if(show_label)
			{
			    spot_label = new TextLayout( label, font, frc);
			    spot_label_w = (int)spot_label.getBounds().getWidth();
			    spot_label_h = (int)spot_label.getBounds().getHeight();
			    spot_label_o = spot_label_w / 2;
			    
			}

			if((apply_filter == false) || (!edata.filter(sid)))
			{
			    int exp = xp;
			    int last_eyp = 0;
			    int last_exp = 0;
			    
			    Color draw_col = text_col;

			    if(do_sel)
			    {
				if(col_sel)
				{
				    //graphic.setColor( is_sel ? sel_colour : unsel_colour ); 
				    if(is_sel)
				    {
					draw_col = sel_colours[ sel_colour_id ];
				    }
				    else
					draw_col = unsel_colour; 
				}
				else
				{
				    draw_col = is_sel ? sel_colour : unsel_colour ; 
				}
			    }

			    graphic.setColor( draw_col );
			    if(g2 == null)
				g2 = (Graphics2D) graphic;

			    for(int m=0; m < meas_ids.length; m++)
			    {
				final double eval = edata.eValue(meas_ids[m], sid);
				int eyp = yp + h - (int)((eval - graph_y_axis_min) * graph_y_axis_scale);
			    
				
				// System.out.println("m=" + m + " s=" + ids[e] + " y=" + eyp);
				
				
				if(m > 0)
				{
				   if(root_profile_picker != null)
				       root_profile_picker.addSegment(last_exp, last_eyp, exp, eyp, sid);
				   
				   graphic.drawLine( last_exp, last_eyp, exp, eyp);
				}
				
				if(show_label)
				{
				    // put the label underneath when the value is rising
				    int label_y = eyp;
				    int label_x = exp-spot_label_o;

				    if((m+1) < meas_ids.length)
				    {
					if( eval < edata.eValue(meas_ids[m+1], sid) )
					    label_y += spot_label_h;
				    }
				    else
				    {
					// put the last label underneath when the value is falling
					if( eval < edata.eValue(meas_ids[m-1], sid) )
					    label_y += spot_label_h;
				    }

				    if(spaceForLabel( 18, spot_label_w, spot_label_h, label_x, label_y ))
				    {
					graphic.setColor( background_col );
					graphic.fillRect( label_x, label_y-spot_label_h, spot_label_w, spot_label_h);
					graphic.setColor( draw_col );

					AffineTransform new_at = new AffineTransform();
					new_at.translate(label_x, label_y);
					Shape shape = spot_label.getOutline(new_at);
					g2.fill(shape);
					
					storeLabelExtent( spot_label_w, spot_label_h, label_x, label_y );
				    }
				}

				last_exp = exp;
				last_eyp = eyp;
			    
				
				exp += graph_x_axis_step;
			    }
			}
		    }
		}
	    }
	}


	// ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 

	private void drawMeanElementsInto( Graphics graphic,  ExprData.Cluster cl, int[] ids, 
					   boolean do_sel, boolean is_sel, 
					   int xp, int yp, int w, int h )
	{
	    if(cl.getIsSpot())
	    {
		Graphics2D g2 = null;

		int spot_label_w = 0;
		int spot_label_h = 0;
		int spot_label_o = 0;
		TextLayout spot_label = null;

		final int n_spots = ids.length;
		final int n_meas  = meas_ids.length;
		
		if(n_spots == 0)
		    return;

		final double[] emean = new double[n_meas];
		final double[] emin = new double[n_meas];
		final double[] emax = new double[n_meas];

		for(int m=0; m < n_meas; m++)
		{
		    emin[m] = Double.MAX_VALUE; 
		    emax[m] = -Double.MAX_VALUE; 
		}
		
                // get mean, min and max for each Measurement
							  
		for(int s=0; s < n_spots; s++)
		{
		    for(int m=0; m < n_meas; m++)
		    {
			final double eval = edata.eValue(meas_ids[m], ids[s]);
			if( eval > emax[m] )
			    emax[m] = eval;
			if( eval < emin[m] )
			    emin[m] = eval;
			emean[m] += eval;
		    }
		}
		for(int m=0; m < n_meas; m++)
		{
		    emean[m] /= (double) n_spots;
		}
		
		
		
		{
		    int exp = xp;
		    int last_eyp = 0;
		    int last_exp = 0;
		    
		    Color draw_col = text_col;
		    
		    graphic.setColor( draw_col );
		    
		    for(int m=0; m < n_meas; m++)
		    {
			int eyp = yp + h - (int)((emean[m] - graph_y_axis_min) * graph_y_axis_scale);

			// System.out.println("m=" + m + " s=" + ids[e] + " y=" + eyp);
			
			if(m > 0)
			{
			    graphic.drawLine( last_exp, last_eyp, exp, eyp);
			}
			
			int min_yp = yp + h - (int)((emin[m] - graph_y_axis_min) * graph_y_axis_scale);
			int max_yp = yp + h - (int)((emax[m] - graph_y_axis_min) * graph_y_axis_scale);
			
			// draw the error bars
			graphic.drawLine( exp, min_yp, exp, max_yp );
			graphic.drawLine( exp-1, min_yp, exp+1, min_yp );
			graphic.drawLine( exp-1, max_yp, exp+1, max_yp );
			
			// and update for the next meas
			last_exp = exp;
			last_eyp = eyp;
			
			exp += graph_x_axis_step;
		    }
		}
	    }
	}
	
	// ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 
	
	private void drawAxes( Graphics graphic, boolean locked, int xp, int yp, int w, int h, double min, double max )
	{
	    graphic.drawRect( xp, yp, w, h );
	    
	    if(locked)
		graphic.drawRect( xp-2, yp-2, w+4, h+4 );

	    Shape shape = null;
	    TextLayout tl = null;
	    
	    //int eyp = yp + h - (int)((edata.eValue(meas_ids[m], ids[e]) - graph_y_axis_min) * graph_y_axis_scale);
	    int miny = yp + h;
	    int maxy = yp;
	    int zeroy = yp + h - (int)((0 - graph_y_axis_min) * graph_y_axis_scale);
	    
	    graphic.drawLine( xp, miny, xp-ticklen, miny );
	    
	    //if(uniform_scale)
	    {
		tl = min_label;
	    }
	    //else
	    {
		//tl = min_label;
		//tl = new TextLayout( mview.niceDouble( min, 9, 4 ), font, frc );
		//min_label_o = (int)(tl.getBounds().getWidth() / 2.0);
	    }
	    AffineTransform new_at = new AffineTransform();
	    new_at.translate(xp-ticklen, miny+min_label_o);
	    new_at.rotate(Math.toRadians(-90), 0, 0);
	    shape = tl.getOutline(new_at);
	    Graphics2D g2 = (Graphics2D) graphic;
	    g2.fill(shape);
	    
	    // if(uniform_scale)
	    {
		tl = max_label;
	    }
	    //else
	    {
		//tl = max_label;
		//tl = new TextLayout( mview.niceDouble( max, 9, 4 ), font, frc );
		//max_label_o = (int)(tl.getBounds().getWidth() / 2.0);
	    }
	    new_at = new AffineTransform();
	    new_at.translate(xp-ticklen, maxy+max_label_o);
	    new_at.rotate(Math.toRadians(-90), 0, 0);
	    shape = tl.getOutline(new_at);
	    g2.fill(shape);
	    graphic.drawLine( xp, maxy, xp-ticklen, maxy );

	    if(draw_zero)
	    {
		//if(spaceForLabel( ticklen, (zero_label_o*2), xp-ticklen, zeroy+zero_label_o ))
		{
		    new_at = new AffineTransform();
		    new_at.translate(xp-ticklen, zeroy+zero_label_o);
		    new_at.rotate(Math.toRadians(-90), 0, 0);
		    shape = zero_label.getOutline(new_at);
		    g2.fill(shape);
		    graphic.drawLine( xp, zeroy, xp-ticklen, zeroy );
		    //storeLabelExtent( ticklen, (zero_label_o*2), xp-ticklen, zeroy+zero_label_o );
		}
	    }

	    // measurement ticks & labels 
	    int exp = xp;
	    int eyp = yp+h+ticklen;
	    int etyp = eyp+ticklen;

	    for(int m=0; m < meas_ids.length; m++)
	    {
		graphic.drawLine( exp, yp+h, exp, eyp );

		if(spaceForLabel( 18, meas_labels_o[m]*2, ticklen, exp-meas_labels_o[m], etyp ))
		{
		    new_at = new AffineTransform();
		    new_at.translate(exp-meas_labels_o[m], etyp);
		    shape = meas_labels[m].getOutline(new_at);
		    g2.fill(shape);
		    storeLabelExtent( meas_labels_o[m]*2, ticklen, exp-meas_labels_o[m], etyp );
		}
		
		exp += graph_x_axis_step;
	    }
	}

	// drawing with selection
	private Color sel_colour, unsel_colour;
	private boolean coloured = false;
	
	
    }
	    
    // ================================================================================
    // ===== spot picker ======================
    // ================================================================================
    //
    //  stores and retrieves ids based on their positions
    //   uses a simle quadtree subdivision of the space
    //
    //   (used to accelerate for mouse tracking)
    //
    //  (shared with from HyperCubePlot)
    // ================================================================================

    class SpotPickerNode
    {
	final int max_spots_per_box = 64;
	
	// each node is either a split point or a spot container
	
	boolean is_split;
	
	// things for split node
	SpotPickerNode[] children;
	int x_split, y_split;
	
	// things for container node
	int entries;
	
	int x, y;
	int w, h;
	
	int[] px;
	int[] py;
	int[] id;
	
	// construct a container
	public SpotPickerNode(int x_, int y_, int w_, int h_)
	{
	    x = x_; 
	    y = y_;
	    w = w_;
	    h = h_;

	    is_split = false;
	    
	    px = new int[max_spots_per_box];
	    py = new int[max_spots_per_box];
	    id = new int[max_spots_per_box];
	    
	    //System.out.println("created node[" + w + "x" + h + " @ " + x + "," + y + "]");

	    entries = 0;
	}
	
	public int findSpot(int sx, int sy, int range)
	{
	
	    //System.out.println("checking " + sx + "," + sy +" in node[" + 
	    //	       w + "x" + h + " @ " + x + "," + y + "]");

	    if(is_split)
	    {
		// delegate the search to the correct child
		int child = 0;
		if(sx >= x_split)
		    child += 1;
		if(sy >= y_split)
		    child += 2;
		return children[child].findSpot(sx, sy, range);
	    }
	    else
	    {
		// search these entries
		int best_id = -1;
		int best_dist_sq = (range * range) + 1;
		for(int e=0; e < entries; e++)
		{
		    int dist_sq = ((px[e] - sx) * (px[e] - sx)) + ((py[e] - sy) * (py[e] - sy));
		    if(dist_sq < best_dist_sq)
		    {
			best_dist_sq = dist_sq;
			best_id = id[e];
		    }
		}
		return best_id;
	    }
	}
	
	public void storeSpot(int sx, int sy, int sid)
	{
	    if(is_split)
	    {
		// delegate the storing to the correct child
		int child = 0;
		if(sx >= x_split)
		    child += 1;
		if(sy >= y_split)
		    child += 2;
		children[child].storeSpot(sx, sy, sid);
	    }
	    else
	    {
		if(entries == max_spots_per_box)
		{
		    // this container is full,  convert it into a split point
		    
		    if((w < 2) && (h < 2))
		    {
			// too small for splitting, ignore this spot

			// System.out.println("  container is full, but too small to split....");
		    }
		    else
		    {
			//System.out.println("  splitting node[" + w + "x" + h + 
			//		   " @ " + x + "," + y + ":" + entries + "]");
			
			
			// convert it into a split point and distribute the children
			//
			splitNode();

			// and try the storeSpot metho again
			storeSpot(sx, sy, sid);
		    }
		}
		else
		{
		    // store this entry
		    px[entries] = sx;
		    py[entries] = sy;
		    id[entries] = sid;
		    entries++;

		    /*
		    System.out.println(sid + " @ " + sx + "," + sy + 
				       " stored in node[" + w + "x" + h + 
				       " @ " + x + "," + y + ":" + entries + "]");
		    */

		}
	    }
	}
	
	private void splitNode()
	{
	    is_split = true;
	    
	    // pick x_split and y_split
	    
	    x_split = (w / 2);     // relative to 0,0
	    y_split = (h / 2);
	    
	    int w_1 = x_split;
	    int w_2 = w-x_split;
	    int h_1 = y_split;
	    int h_2 = h-y_split;

	    if(w == 1)
		x_split = 0;
	    if(h == 1)
		y_split = 0;
	    
	    x_split += x;          // make relative to x,y
	    y_split += y;

	    // create the children
	    
	    children = new SpotPickerNode[4];
	    
	    children[0] = new SpotPickerNode(x, y, w_1, h_1);
	    children[1] = new SpotPickerNode(x_split, y, w_2, h_1);
	    children[2] = new SpotPickerNode(x, y_split, w_1, h_2);
	    children[3] = new SpotPickerNode(x_split, y_split, w_2, h_2);
	    
	    // distribute the entries of this node to the children
	    
	    for(int e=0; e < entries; e++)
	    {
		int child = 0;
		if(px[e] >= x_split)
		    child += 1;
		if(py[e] >= y_split)
		    child += 2;
		children[child].storeSpot(px[e], py[e], id[e]);
	    }
	    
	    // free up the now unused arrays of this node
	    
	    px = py = id = null;
	    entries = 0;
	}

	public void dumpStats(final String pad)
	{
	    if(is_split)
	    {
		final String ipad = " " + pad;
		for(int c=0; c < 4; c++)
		    children[c].dumpStats(ipad);
	    }
	    else
	    {
		System.out.println(pad + " [" + 
				   w + "x" + h + " @ " + 
				   x + "," + y + ":" + 
				   entries + "]");
	    }
	}

	public void drawNode(Graphics g)
	{

	    g.setColor(Color.white);
	    
	    if(is_split)
	    {
		for(int c=0; c < 4; c++)
		    children[c].drawNode(g);
	    }
	    else
		g.drawRect(x, y, w-1, h-1);
	}
	
    }

    // ================================================================================
    // ===== spot picker II ======================
    // ================================================================================

  
    public class LineSeg
    {
	public LineSeg( int sx_, int sy_, int ex_, int ey_, int id_ )
	{
	    sx = sx_; sy = sy_; ex = ex_; ey = ey_; id = id_;

	    m_inf = (sx == ex);

	    if(!m_inf)
	    {
		m = (double)( ey-sy ) / (double)( ex-sx );
		c = ((double) sy) - (m * ((double) sx));

		m_intersect = 1.0 / m;
	    }
	}

	int sx, sy, ex, ey, id;
	double m, c;
	boolean m_inf;    // vertical lines have an infinite slope

	double m_intersect;

	public int distanceFrom( int x, int y )
	{
	    if(m_inf)
	    {
		return (x - sx) * (x - sx);
	    }
	    else
	    {
		// find the equation of the line that 
		//  (a) is perpendicular to this segment
		//  (b) passes through (x,y)
		//
		
		// slope is known: m_intersect
		
		double c_intersect = ((double) y) - (m_intersect * ((double) x));

		// from:
		//
		//   1.this segment:    y1 = m1 x1 + c1;
		//   2.the intersect:   y2 = m2 x2 + c2;
		//
		//   the intersection point is (p,q)
		//
		//   y1, x1, y2, x2, m1, m2 and c1 are known
		//
		//   solving intersect for p: 
		//      m1 p + c1 = m2 p + c2
		//      p = (c2 - c1) / (m1 - m2);
		//
		
		double p = (c_intersect - c) / (m - m_intersect);

		double q = (m * p) + c;

		int pi = (int) p;
		int qi = (int) q;

		return ((x-pi) * (x-pi)) + ((y-qi) * (y-qi));
	    }
	}
    }


    public class ProfilePicker
    {
	private final int map_x = 20;
	private final int map_y = 20;
	
	private double scale_x = 1.0;
	private double scale_y = 1.0;
	
	private Vector[][] label_map;

	public ProfilePicker()
	{
	    label_map = new Vector[map_x][map_y];
	}

	public void addSegment( int sx, int sy, int ex, int ey, int spot_id )
	{
	    LineSeg ls = new LineSeg( sx, sy, ex, ey, spot_id );
	    
	    // rasterise this segment into the bins

	    if(sx > ex)
	    {
		int tmp = sx; sx = ex; ex = tmp;
	    }
	    if(sy > ey)
	    {
		int tmp = sy; sy = ey; ey = tmp;
	    }
	    
	    int msx = (int)((double) sx * scale_x);
	    int msy = (int)((double) sy * scale_y);
	
	    int mex = (int)((double) ex * scale_x);
	    int mey = (int)((double) ey * scale_y);

	    for(int mx=msx; mx <= mex; mx++)
		for(int my=msy; my <= mey; my++)
		{
		    Vector vec = label_map[mx][my];
		    if(vec == null)
			vec = (label_map[mx][my] = new Vector());
		    vec.addElement( ls );
		}
	}

	public int findProfile( int x, int y )
	{
	    
	    
	    int dist;
	    int best_id = -1;
	    int min_dist = Integer.MAX_VALUE;

	    // which bin is the pointer in?
	    int mx = (int)((double) x * scale_x);
	    int my = (int)((double) y * scale_y);
	    
	    if((mx >=0 ) && (mx < map_x) && (my >=0 ) && (my < map_y))
	    {
		// find the segment in this bin that is nearest to the pointer
		Vector vec = label_map[mx][my];
		if(vec != null)
		{
		    for(int seg=0; seg < vec.size(); seg++)
		    {
			LineSeg ls = (LineSeg) vec.elementAt(seg);
			dist = ls.distanceFrom( x, y );
			if(dist < min_dist)
			{
			    min_dist = dist;
			    best_id = ls.id;
			}
		    }
		}
	    }
	    return best_id;
	}

	public void setupPicker( int w, int h )
	{
	    // width = w; height = h;
	    scale_x = (double) map_x / (double) w;
	    scale_y = (double) map_y / (double) h;
	    
	    for(int x=0; x < map_x; x++)
		for(int y=0; y < map_y; y++)
		    if(label_map[x][y] != null)
			label_map[x][y].clear();
	}
    }

    

    // ============================================================================
    // ============================================================================
    // spot labels
    // ============================================================================

    private Hashtable show_spot_label = new Hashtable();
    private int cycled_data = 0;
    final static int n_sel_colours = 8;

    private Integer nextData()
    {
	int res = cycled_data++;
	if(cycled_data >=  n_sel_colours)
	    cycled_data = 0;

	return new Integer( res );
    }

    // -1 if not found, >=0 otherwise
    public int getSpotLabel(int id)
    {
	Integer res = (Integer) show_spot_label.get(new Integer(id));
	if(res != null)
	    return res.intValue();
	else
	    return -1;
    }

    public void showSpotLabel(int id)
    {
	Integer key = new Integer(id);
	if(show_spot_label.get(key) == null)
	{
	    show_spot_label.put( key, nextData() );
	}
    }

    public void clearAllSpotLabels()
    {
	show_spot_label = new Hashtable();
    }

    public void toggleSpotLabel(int id)
    {
	Integer i = new Integer(id);
	
	if(show_spot_label.get(i) == null)
	{
	    show_spot_label.put(i, nextData() );
	    //System.out.println(id + " added");
	}
	else
	{
	    show_spot_label.remove(i);
	    //System.out.println(id + " removed");
	}
	
	pro_panel.repaint();
    }

    public void setSpotLabel(ExprData.Cluster cl)
    {
	if(cl.getIsSpot())
	{
	    final int[] els = cl.getElements();
	    if(els != null)
		for(int e=0; e < els.length; e++)
		    showSpotLabel(els[e]);
	}
    }   
   
    public void setAllSpotLabels()
    {
	/*
	for(int s=0; s < sel_cls.size(); s++)
	{
	    ExprData.Cluster cl = (ExprData.Cluster) sel_cls.elementAt(s);
	    int[] ids = include_children ? cl.getAllClusterElements() :  cl.getElements();
	    if(ids != null)
		for(int e=0; e < ids.length; e++)
		    showSpotLabel(ids[e]);
	}
	*/
	int[] ssel = edata.getSpotSelection();
	if(ssel == null)
	    return;
	for(int s=0; s < ssel.length; s++)
	    showSpotLabel( ssel[ s ] );
    }

 

    // ============================================================================
    // ============================================================================
    //
    // label space allocator
    //
    // ============================================================================
    // ============================================================================

    private void storeLabelExtent( int lw, int lh, int lx, int ly )
    {
	int mx = (int)((double) lx * scale_x);
	int my = (int)((double) ly * scale_y);

	//System.out.println("storeLabelExtent: " + lw + "x" + lh + " @ " + lx + "," + ly + " ... " + mx + "," + my);

	if((mx >=0 ) && (mx < map_x) && (my >=0 ) && (my < map_y))
	{
	    if(label_map[mx][my] == null)
		label_map[mx][my] = new Vector();
	    label_map[mx][my].addElement(new LabelExtent( lw, lh, lx, ly ));
	}
    }
    
    // returns true if the specified label will fit into the map without overlapping anything
    //
    private boolean spaceForLabel( int scale ,int lw, int lh, int lx, int ly )
    {
	if(!auto_space)
	    return true;
	
	int mx = (int)((double) lx * scale_x);
	int my = (int)((double) ly * scale_y);

	//System.out.println("spaceForLabel: " + lw + "x" + lh + " @ " + lx + "," + ly + " ... " + mx + "," + my);

	if((mx >=0 ) && (mx < map_x) && (my >=0 ) && (my < map_y))
	{
	    //System.out.println("  spaceForLabel: searching from " + my);

	    int tp = my;
	    int bp = my + 1;

	    while((tp >= 0) || (bp < map_y))
	    {
		if(tp >= 0)
		{
		    if(searchMapLine( scale, mx, tp, lw, lh, lx, ly ))
			return false;
		    tp--;
		}
		if(bp < map_y)
		{
		    if(searchMapLine( scale, mx, bp, lw, lh, lx, ly ))
			return false;
		    bp++;
		}
	    }
	    
	    //System.out.println("  spaceForLabel: onscreen and there is space!");
	    return true;
	}
	return false;
    }
    
    // returns true if the specified label will overlap a label in this cell of the map
    //
    private boolean searchMapCell( int scale, int col, int row, int lw, int lh, int lx, int ly )
    {
	Vector lm = label_map[col][row];
	if(lm == null)
	    return false;

	for(int le=0; le < lm.size(); le++)
	{
	    LabelExtent lex = (LabelExtent) lm.elementAt(le);
	    if(labelsOverlap( scale, lw, lh, lx, ly, lex.lw, lex.lh, lex.lx, lex.ly ))
		return true;
	}
	return false;
    }

    // returns true if the specified label will overlap a label in the map on this line
    //
    private boolean searchMapLine( int scale, int col, int row, int lw, int lh, int lx, int ly )
    {
	//System.out.println("  searchMapLine:  col=" + col  + " row=" + row);

	int lp = col;
	int rp = col + 1;
	while((lp >= 0) || (rp < map_x))
	{
	    if(lp >= 0)
	    {
		if(searchMapCell( scale, lp, row, lw, lh, lx, ly ))
		    return true;
		lp--;
	    }
	    if(rp < map_x)
	    {
		if(searchMapCell( scale, rp, row, lw, lh, lx, ly ))
		    return true;
		rp++;
	    }
	}
	//System.out.println("  searchMapLine: not found");

	return false;
    }

    private void resetLabelMap(int w, int h)
    {
	// width = w; height = h;
	scale_x = (double) map_x / (double) w;
	scale_y = (double) map_y / (double) h;

	for(int x=0; x < map_x; x++)
	    for(int y=0; y < map_y; y++)
		if(label_map[x][y] != null)
		    label_map[x][y].clear();
    }

    private double scale_x = 1.0;
    private double scale_y = 1.0;

    private final int map_x = 10;
    private final int map_y = 10;

    private Vector[][] label_map = new Vector[map_x][map_y];
    
    private class LabelExtent
    {
	public int lw, lh, lx, ly;
	public LabelExtent(int lw_, int lh_, int lx_, int ly_)
	{
	    lw=lw_; lh=lh_; lx=lx_; ly=ly_;
	}
    }

    // scale should be a number in the range 16...32
    // (higher numbers give a large overlap)
    //
    private boolean labelsOverlap( int scale, 
				   int lw1, int lh1, int lx1, int ly1,
				   int lw2, int lh2, int lx2, int ly2 )
    {
	final int hlw1 = (lw1 * scale) / 32;
	final int hlh1 = (lh1 * scale) / 32;
	
	final int hlw2 = (lw2 * scale) / 32;
	final int hlh2 = (lh2 * scale) / 32;

	/*
	  final int hlw1 = lw1 / 2;
	  final int hlh1 = lh1 / 2;
	  
	  final int hlw2 = lw2 / 2;
	  final int hlh2 = lh2 / 2;
	*/

	final int tlx1 = lx1 - hlw1;
	final int tly1 = ly1 - hlh1;
	final int brx1 = lx1 + hlw1;
	final int bry1 = ly1 + hlh1;
	
	final int tlx2 = lx2 - hlw2;
	final int tly2 = ly2 - hlh2;
	final int brx2 = lx2 + hlw2;
	final int bry2 = ly2 + hlh2;
	
	// check the corners
	boolean h_ok = (((tlx1 >= tlx2) && (tlx1 <= brx2)) || ((brx1 >= tlx2) && (brx1 <= brx2)));
	boolean v_ok = (((tly1 >= tly2) && (tly1 <= bry2)) || ((bry1 >= tly2) && (bry1 <= bry2)));
	
	// check for containment
	if(!h_ok)
	    h_ok = ((tlx1 < tlx2) && (brx1 > brx2));
	if(!v_ok)
	    v_ok = ((tly1 < tly2) && (bry1 > bry2));
	
	return (h_ok && v_ok);
    }

    // ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 


    // ============================================================================
    // ============================================================================
    //
    //  the grouper
    //
    // ============================================================================
    // ============================================================================

    private int n_groups = 0;
    private int n_meas   = 0;

    private boolean is_iterating = false;

    private double[][] group_profile;

    private double[] profile_clipboard; // for cut&paste

    private boolean[] group_lock;

    private short[] group_allocation;
    
    private int[] group_count;
    private double[] group_score;

    private double[][] new_group_profile;
    private int[] new_group_count;
    private short[] new_group_allocation;

    private int[] graph_x;
    private int[] graph_y;

    private int distance_metric;

    private synchronized void setIterating( boolean is_iterating_ )
    {
	is_iterating = is_iterating_;
	if(iterate_jchkb.isSelected() != is_iterating)
	    iterate_jchkb.setSelected( is_iterating );
    }

    private void startStopIterate()
    {
	is_iterating = iterate_jchkb.isSelected() || iterating_by_command;
    }


    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

    private void getMinMax()
    {
	// System.out.println( "getMinMax()");
	
	current_min = Double.MAX_VALUE;
	current_max = -Double.MAX_VALUE;

	for(int m=0; m < meas_ids.length; m++)
	{
	    double tmp = edata.getMeasurementMinEValue( meas_ids[m] );
	    if(tmp < current_min)
		current_min = tmp;

	    tmp = edata.getMeasurementMaxEValue( meas_ids[m] );
	    if(tmp > current_max)
		current_max = tmp;
	}
    }

    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

    private void updateGroups()
    {
	distance_metric = distance_metric_jcb.getSelectedIndex();

	allocateSpotsToGroups( group_profile, group_allocation, group_count, group_score );
		
	pro_panel.repaint( );
	
    }

    private void layoutGroups()
    {
	// work out rows/cols
	n_cols = 1;
	n_rows = 0;

	if(n_groups > 0)
	{
	    // find the nicest factors of 'count'
	    
	    // pick the two factors that are closest to one another
	    
	    double best_rc_diff = Double.MAX_VALUE;
	    
	    for(int m=1; m < n_groups; m++)
	    {
		double c = (double) m;
		double r = ((double) n_groups) / c;
		
		double rc_diff = Math.abs(r - c);
		if(rc_diff <  best_rc_diff)
		{
		    best_rc_diff= rc_diff;
		    n_cols = (int) c;
		}
	    }
	    
	    n_rows = (n_cols > 0) ? (int)(Math.ceil((double)n_groups / (double)n_cols)) : n_groups;
	}

	graph_x = new int[ n_groups ];
	graph_y = new int[ n_groups ];

    }

    private void setupGroups()
    {
	int old_n_groups = n_groups ;
	int old_n_meas   = n_meas ;
	// how many groups?
	n_groups = 0;

	distance_metric = distance_metric_jcb.getSelectedIndex();

	try
	{
	    String ngs = n_groups_jtf.getText();
	    n_groups = Integer.valueOf(ngs).intValue();
	}
	catch(NumberFormatException nfe)
	{
	    return;
	}
	
	// how many measurements?
	
	n_meas = meas_ids.length;

	layoutGroups();

	// System.out.println( n_groups  + " groups = " + n_cols + " x " + n_rows );

	// setup random profiles

	if((group_profile != null) && (old_n_meas == n_meas))
	{
	    // System.out.println( "reusing old profiles...." );
	}

	group_profile     = new double[n_groups][];
	//new_group_profile = new double[n_groups][];
	
	group_lock = new boolean[n_groups];
	for(int g=0; g < n_groups; g++)
	    group_lock[g] = false;

	final int n_spots = edata.getNumSpots();

	group_allocation = new short[ n_spots ];
	group_count      = new int[ n_groups ];
	group_score      = new double[ n_groups ];

	//new_group_allocation = new short[ n_spots ];
	//new_group_count      = new int[ n_groups ];

	
	initActualProfiles();
	//initMeanProfiles();
	//initRandomProfiles();
    }

    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

    private boolean warnIfTooManyGroups( int ng)
    {
	if(ng > 30)
	{
	    if(mview.infoQuestion("There will be " + ng + " groups.\n" + 
				  "Do you really want that many?",
				  "Yes", "No") == 1)
		return false;
	}
	
	if(ng > 243)
	{
	    if(mview.infoQuestion(ng + " is really quite a lot.\n" + 
				  "Are you absolutely positive you\n" + 
				  "want to go ahead with this?",
				  "Yes", "No") == 1)
		return false;
	}

	if(ng > 1000)
	{
	    if(mview.infoQuestion(ng + " is going to take a very long time to process.\n" + 
				  "Maybe a smaller number might be better?\n" + 
				  "Really continue?",
				  "Yes", "No") == 1)
		return false;
	}

	return true;
    }

    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

    private void setGroupsFromSelection()
    {
	final int[] spot_sel = edata.getSpotSelection();
	if((spot_sel == null) || (spot_sel.length == 0))
	{
	    mview.alertMessage("There are no selected Spots");
	    return;
	}
	
	if( warnIfTooManyGroups( spot_sel.length  ) == false)
	    return;

	n_groups = spot_sel.length;
	
	n_meas = meas_ids.length;

	n_groups_jtf.setText( String.valueOf( n_groups ));

	layoutGroups();

	group_profile     = new double[n_groups][];
	
	group_lock = new boolean[n_groups];
	for(int g=0; g < n_groups; g++)
	    group_lock[g] = false;

	final int n_spots = edata.getNumSpots();

	group_allocation = new short[ n_spots ];
	group_count      = new int[ n_groups ];
	group_score      = new double[ n_groups ];

	for(int g=0; g < n_groups; g++)
	{
	    group_profile[g] = new double[n_meas];
	    setGroupProfile( g, spot_sel[ g ] );
	}

    }

    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

    private void setGroupsByPermutation()
    {
	
	// n_meas must be > 1...

	n_meas = meas_ids.length;
	if(n_meas < 2)
	{
	    mview.alertMessage("You must select two or more Measurements first");
	    return;
	}

	final int huge = Integer.MAX_VALUE / 3;

	int n_perms = 3;
	for(int p=1; p < (n_meas-1); p++)
	{
	    if(  (n_perms < huge ) )
	    {
		n_perms *= 3;
	    }
	    else
	    {
		mview.alertMessage("Too many permutations.\nSelect fewer Measurements and try again");
		return;
	    }
	}
	
	if( warnIfTooManyGroups( n_perms ) == false)
	    return;

	n_groups = n_perms;
	
	n_groups_jtf.setText( String.valueOf( n_groups ));

	layoutGroups();

	group_profile     = new double[n_groups][];

	//	new_group_profile = new double[n_groups][];
	
	group_lock = new boolean[n_groups];
	for(int g=0; g < n_groups; g++)
	    group_lock[g] = false;

	final int n_spots = edata.getNumSpots();

	group_allocation = new short[ n_spots ];
	group_count      = new int[ n_groups ];
	group_score      = new double[ n_groups ];


	int[] dir_for_dim = new int[n_meas];

	final double min = current_min;
	final double max = current_max;
	final double hrange = (max - min) * 0.5;
	final double shift = hrange / (double)(n_meas-1);
	final double zero_pt = min + hrange;

	for(int g=0; g < n_groups; g++)
	{
	    group_profile[g] = new double[n_meas];

	    group_profile[g][0] = zero_pt;

	    for(int m=1; m < n_meas; m++)
	    {
		switch(dir_for_dim[m])
		{
		case 0:  // up
		    group_profile[g][m] = group_profile[g][m-1] + shift;
		    break;
		case 1:  // 0
		    group_profile[g][m] = group_profile[g][m-1];
		    break;
		case 2:  // down
		    group_profile[g][m] = group_profile[g][m-1] - shift;
		    break;
		}
	    }

	    int cacade = n_meas-1;
	    while((cacade >= 0) && ((++dir_for_dim[cacade]) == 3))
	    {
		dir_for_dim[cacade] = 0;
		cacade--;
	    }
	}


    }

    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

    private void removeGroup( int group )
    {
	if(n_groups < 2)
	    return;
	
	int n_used = n_groups - 1;
	
	double[][] shortened_group_profile = new double[n_used][];
	boolean[]  shortened_group_lock    = new boolean[n_used];
	
	int g_pos = 0;

	for(int g=0; g < n_groups; g++)
	{
	    if(g != group)
	    {
		shortened_group_profile[g_pos] = group_profile[g];
		shortened_group_lock[g_pos]    = group_lock[g];
		
		g_pos++;
	    }
	}
	
	group_profile = shortened_group_profile;
	group_lock    = shortened_group_lock;
	
	n_groups = n_used;
	
	n_groups_jtf.setText( String.valueOf( n_groups ));
	
	layoutGroups();
    }


    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\


    private void removeEmptyGroups()
    {
	// how many be non empty?

	int n_empty = 0;
	for(int g=0; g < n_groups; g++)
	{
	    if(group_count[g] == 0)
		n_empty++;
	}
	
	if(n_empty > 0)
	{
	    int n_used = n_groups - n_empty;

	    double[][] shortened_group_profile = new double[n_used][];
	    boolean[]  shortened_group_lock    = new boolean[n_used];

	    int g_pos = 0;

	    for(int g=0; g < n_groups; g++)
	    {
		if(group_count[g] > 0)
		{
		    shortened_group_profile[g_pos] = group_profile[g];
		    shortened_group_lock[g_pos]    = group_lock[g];
		    
		    g_pos++;
		}
	    }

	    group_profile = shortened_group_profile;
	    group_lock    = shortened_group_lock;

	    n_groups = n_used;

	    n_groups_jtf.setText( String.valueOf( n_groups ));

	    layoutGroups();

	    System.out.println("removed " + n_empty + " empty groups");

	}

    }

    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

    private void copyGroup(int group)
    {
	
	profile_clipboard = new double[ n_meas ];
	
	for(int m = 0; m < n_meas; m++)
	    profile_clipboard[m] = group_profile[group][m];
    }

    private void pasteGroup(int group)
    {
	for(int m = 0; m < n_meas; m++)
	    group_profile[group][m] = profile_clipboard[m];
    }

    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
     
    private void selectAllInGroup(int group)
    {
	int[] ids = new int[ group_count[ group ] ];

	int sp = 0;
	for(int s=0; s < group_allocation.length; s++)
	{
	    if(group_allocation[s] == group)
	    {
		ids[ sp++ ] = s;
	    }
	}
	edata.setSpotSelection( ids );
    }

    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\


    private void randomiseGroupProfiles()
    {
	for(int g=0; g < n_groups; g++)
	{
	    randomiseGroupProfile( g );
	}

    }

    private void randomiseGroupProfile( int g )
    {
	if(!group_lock[ g ])
	{
	    int s = (int) (Math.random() * (group_allocation.length));
	    
	    if(s == group_allocation.length)
		s = 0;
	    
	    for(int m=0; m < n_meas; m++)
	    {
		group_profile[g][m] = edata.eValue( meas_ids[m], s);
	    }
	}
    }


    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

    private void initRandomProfiles()
    {
	double[] min   = new double[n_meas];
	double[] scale = new double[n_meas];

	for(int m=0; m < n_meas; m++)
	{
	    min[m] = current_min;
	    
	    scale[m] = current_max - min[m];
	}
	
	for(int g=0; g < n_groups; g++)
	{
	    group_profile[g] = new double[n_meas];

	    for(int m=0; m < n_meas; m++)
	    {
		double val = min[m] + (Math.random() * scale[m]);

		group_profile[g][m] = val;
	    }
	}
	
    }

    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

    private void setGroupProfile( int group, int spot_id )
    {
	for(int m=0; m < n_meas; m++)
	{
	    group_profile[group][m] = edata.eValue( meas_ids[m], spot_id );
	}
    }

    private void initActualProfiles()
    {
	// use actual spot profiles from the data

	if(n_groups == 0)
	    return;

	if(group_allocation.length == 0)
	    return;

	int step = group_allocation.length / n_groups;
	if(step < 1)
	    step = 1;

	int s = (int) (Math.random() * (group_allocation.length-1));
	for(int g=0; g < n_groups; g++)
	{
	    // System.out.println( "group " + g + " gets spot " + s );

	    group_profile[g] = new double[n_meas];

	    for(int m=0; m < n_meas; m++)
	    {
		group_profile[g][m] = edata.eValue( meas_ids[m], s);
	    }

	    s += step;

	    while(s >= group_allocation.length)
		s -= group_allocation.length;
	}
    }

    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

    private void initMeanProfiles()
    {
	// allocate in round-robin fashion

	if(n_groups == 0)
	    return;

	short group = 0;
	for(int s=0; s < group_allocation.length; s++)
	{
	    group_allocation[s] = group;
	    group_count[group]++;
	    if(++group >= n_groups)
		group = 0;
	}

	// then compute mean profiles for each group

	for(int g=0; g < n_groups; g++)
	{
	    double g_scale = 1.0 / (double) group_count[g];

	    group_profile[g] = new double[n_meas];

	    for(int s=0; s < group_allocation.length; s++)
	    {
		if(group_allocation[s] == g)
		{
		    for(int m=0; m < n_meas; m++)
		    {
			group_profile[g][m] += (edata.eValue( meas_ids[m], s) * g_scale);
		    }
		}
	    }
	}


    }

    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
    ////\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//
    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

    private void adjustGroupProfiles()
    {
	if(n_groups > 1)
	{
	    switch(adjust_mode)
	    {
	    case 0:
		adjustGroupProfiles_to_mean_profile();
		break;
	    case 1:
		adjustGroupProfiles_to_mean_spot_profile();
		break;
	    case 2:
		adjustGroupProfiles_to_random_profile();
		break;
	    }

	    if(adjust_auto_remove_empty)
	    {
		removeEmptyGroups();
	    }
	    if(adjust_noise_level > .0)
	    {
		// randomly perturb all group profiles

		final double range = (current_max-current_min) * adjust_noise_level;    // +/- 10%

		for(int g=0; g < n_groups; g++)
		{
		    for(int m=0; m < n_meas; m++)
		    {
			double delta = Math.random() * range;

			if(Math.random() >= 0.5)
			    delta = -delta;
			
			group_profile[g][m] += delta;
		    }
		}

	    }
	}

    }

    private int[] getGroupsToAdjust()
    {
	int[] res = null;

	switch(adjust_what)
	{
	case 0:  // smallest
	    res = new int[1];

	    int least_g = 0;
	    int least_c = group_count[0];
	    
	    for(int g=1; g < n_groups; g++)
	    {
		if( group_count[g] > least_c )
		{
		    least_c = group_count[g];
		    least_g = g;
		}
	    }
	    res[0] = least_g;
	    return res;

	case 1:  // biggest
	    res = new int[1];

	    int most_g = 0;
	    int most_c = group_count[0];
	    
	    for(int g=1; g < n_groups; g++)
	    {
		if( group_count[g] > most_c )
		{
		    most_c = group_count[g];
		    most_g = g;
		}
	    }
	    res[0] = most_g;
	    return res;

	case 2:  // best
	    res = new int[1];

	    int best_g = 0;
	    double best_s = group_score[0];
	    
	    for(int g=1; g < n_groups; g++)
	    {
		if( group_score[g] < best_s )
		{
		    best_s = group_score[g];
		    best_g = g;
		}
	    }
	    res[0] = best_g;
	    return res;

	case 3:  // worst
	    res = new int[1];

	    int worst_g = 0;
	    double worst_s = group_score[0];
	    
	    for(int g=1; g < n_groups; g++)
	    {
		if( group_score[g] > worst_s )
		{
		    worst_s = group_score[g];
		    worst_g = g;
		}
	    }
	    res[0] = worst_g;
	    return res;

	case 4:  // random
	    res = new int[1];
	    res[0] = (int)(Math.random() * (n_groups-1));
	    return res;

	case 5:  // all
	    res = new int[n_groups];
	    for(int g=0; g < n_groups; g++)
		res[g] = g;
	    return res;
	}

	return res;
    }

    private void adjustGroupProfiles_smallest_from_other( boolean from_biggest )
    {
	//  find the smallest group(s)
	//
	//  in each group, use the profile of either a random spot from the biggest group or any random spot
	//
	//  find G1, the biggest group
	//  and S1 the size of the smallest group
	//

	int most_g = 0;
	int most_c = group_count[0];
	int least_c = most_c;

	for(int g=1; g < n_groups; g++)
	{
	    if( group_count[g] > most_c )
	    {
		most_c = group_count[g];
		most_g = g;
	    }
	    if( group_count[g] < least_c )
	    {
		least_c = group_count[g];
	    }
	}

	int tweaked = 0;

	for(int g=0; g < n_groups; g++)
	{
	    if( group_count[g] == least_c)  // if this group is (one of) the smallest
	    {
		tweaked++;

		int spot_id = -1;

		if( from_biggest )
		{
		    // pick a random spot from G1
		    
		    int spot_c = (int)(Math.random() * (double) (group_count[most_g]-1));
		    
		    for(int s=0; s < group_allocation.length; s++)
		    {
			if(group_allocation[s] == most_g)
			{
			    /*
			      if(spot_id == -1) // just to make sure....
			      spot_id = s;
			    */
			    
			if(--spot_c == 0)
			{
			    spot_id = s;
			    break;
			}
			}
		    }
		}
		else
		{
		    // TODO: should consider filter....
		    
		    spot_id = (int)(Math.random() * (double) group_allocation.length);
		}

		// System.out.println( "fill_empty: tweaking group " + g + " to be spot " + spot_id);
		
		for(int m = 0; m < n_meas; m++)
		{
		    group_profile[g][m] = edata.eValue( meas_ids[m], spot_id);
		}
	    }
	}
	
	// and update
	
       
	allocateSpotsToGroups( group_profile, group_allocation, group_count, group_score );
	
    }

    private void adjustGroupProfiles_to_random_profile()
    {

	int[] groups = getGroupsToAdjust();

	for(int gi=0; gi < groups.length; gi++)
	{
	    final int g = groups[gi];
	    final int s = (int) (Math.random() * (group_allocation.length-1));
	    
	    for(int m=0; m < n_meas; m++)
	    {
		double s_data = edata.eValue( meas_ids[m], s);
		group_profile[g][m] = ((1.0-adjust_rate) * group_profile[g][m]) + (adjust_rate * s_data);
	    }
	}

	allocateSpotsToGroups( group_profile, group_allocation, group_count, group_score );
    }

    private void adjustGroupProfiles_to_mean_profile()
    {
	// find the group with the most spots,
	//
	// in that group find the dimension with the biggest variance
	// 
	// adjust the profile in that dimension so it is closer to the mean

	int[] groups  = getGroupsToAdjust();

	for(int gi=0; gi < groups.length; gi++)
	{
	    final int g = groups[gi];

	    double[] mean_profile = new double[n_meas];
	    double[] variance     = new double[n_meas];

	    double g_scale = 1.0 / (double) group_count[g];
	    
	    for(int s=0; s < group_allocation.length; s++)
	    {
		if(group_allocation[s] == g)
		{
		    for(int m=0; m < n_meas; m++)
		    {
			mean_profile[m] += (edata.eValue( meas_ids[m], s) * g_scale);
		    }
		}
	    }
	    for(int s=0; s < group_allocation.length; s++)
	    {
		if(group_allocation[s] == g)
		{
		    for(int m=0; m < n_meas; m++)
		    {
			double diff = edata.eValue( meas_ids[m], s) - mean_profile[m];
			variance[m] += ((diff * diff) * g_scale);
		    }
		}
	    }
	    
	    double most_v = variance[0];
	    int most_m = 0;

	    for(int m=1; m < n_meas; m++)
	    {
		if(variance[m] > most_v)
		{
		    most_v = variance[m];
		    most_m = m;
		}
	    }
	    
	    //System.out.println( "adjusting group=" + g + " dim=" + most_m + " count is " + group_count[g]);

	    group_profile[g][most_m] = ((1.0-adjust_rate) * group_profile[g][most_m]) + (adjust_rate * mean_profile[most_m]);
	}

	allocateSpotsToGroups( group_profile, group_allocation, group_count, group_score );
    }

    private void adjustGroupProfiles_to_mean_spot_profile()
    {
	// find the group with the most spots,
	//
	// in that group find the mean profile
	//
	// then find the spot with the profile closest to the mean
	//
	// and use the profile of that spot as the group profile

	int[] groups  = getGroupsToAdjust();

	for(int gi=0; gi < groups.length; gi++)
	{
	    final int g = groups[gi];

	    double g_scale = 1.0 / (double) group_count[g];
	    
	    double[] mean_profile = new double[n_meas];

	    for(int s=0; s < group_allocation.length; s++)
	    {
		if(group_allocation[s] == g)
		{
		    for(int m=0; m < n_meas; m++)
		    {
			mean_profile[m] += (edata.eValue( meas_ids[m], s) * g_scale);
		    }
		}
	    }
	    
	    int best_s = -1;
	    double best_d = 0;

	    double[] s_profile = new double[ n_meas ];
	    
	    // find the spot with the profile closest to the mean

	    for(int s=0; s < group_allocation.length; s++)
	    {
		if(group_allocation[s] == g)
		{
		    for(int m = 0; m < n_meas; m++)
		    {
			s_profile[ m ] = edata.eValue( meas_ids[m], s);
		    }

		    if(best_s == -1)
		    {
			// first spot in the group
			best_s = s;
			best_d = distance_sq( n_meas, s_profile, mean_profile ); 
		    }
		    else
		    {
			double dist =  distance_sq( n_meas, s_profile, mean_profile );
			if( dist < best_d )
			{
			    best_d = dist;
			    best_s = s;
			}
		    }

		}
	    }
	    
	    if(best_s >= 0)
	    {
		// use the profile of the chosen spot
		for(int m = 0; m < n_meas; m++)
		{
		    double val = edata.eValue( meas_ids[m], best_s);
		    group_profile[g][m] = ((1.0-adjust_rate) * group_profile[g][m]) + (adjust_rate * val );
		}
	    }

	    //System.out.println( "mean_spot_profile: adjusting group=" + g + 
	    //		" mean spot=" + best_s + " count is " + group_count[g]);
	}

	// and update
	   
	allocateSpotsToGroups( group_profile, group_allocation, group_count, group_score );
    }


    private void adjustGroupProfiles_2()
    {

	// randomly adjust the group with the most spots until it has less spots....

	if(n_groups > 1)
	{

	    int most_g = 0;
	    int most_c = group_count[0];
	    
	    for(int g=1; g < n_groups; g++)
	    {
		if( group_count[g] > most_c )
		{
		    most_c = group_count[g];
		    most_g = g;
		}
	    }

	    
	    final int shuffle_m = (int)(Math.random() * n_meas);
	    
	    
	    // create 'new_group_profile', exactly the same
	    // as 'group_profile' except for 1 entry [most_g][shuffle_m]
	    //
	    
	    for(int g=0; g < n_groups; g++)
		new_group_profile[g] = group_profile[g];
	    
	    new_group_profile[most_g] = new double[n_meas];
	    
	    for(int m=0; m < n_meas; m++)
		new_group_profile[most_g][m] = group_profile[most_g][m];
	    
	    final double range = current_max - current_min;

	    // can have temperature here 

	    double delta = Math.random() * range * 0.1;    // +/- 10%

	    if(Math.random() >= 0.5)
		delta = -delta;

	    new_group_profile[most_g][shuffle_m] = group_profile[most_g][shuffle_m] + delta;

	    // does this change improve things?

	    allocateSpotsToGroups( new_group_profile, new_group_allocation, new_group_count, group_score );

	    if( new_group_count[most_g] < group_count[most_g] )
	    {
		// +ve result, the group is now smaller
		
		group_profile = new_group_profile;
		group_allocation = new_group_allocation;
		group_count = new_group_count;

		// System.out.println( "adjusting group=" + most_g + " dim=" + shuffle_m + " delta=" + delta + " -> better!");
	    }
	    else
	    {
		// System.out.println( "adjusting group=" + most_g + " dim=" + shuffle_m + " delta=" + delta + " -> same or worse!");
	    }
	}

    }


    private void adjustGroupProfiles_1()
    {
	// System.out.println( "adjusting....");


	// 
	//  make the group with the most spots more like the group with the least spots
	// 
	
	if(n_groups > 1)
	{
	    
	    int most_g = 0;
	    int least_g = 0;
	    int most_c = group_count[0];
	    int least_c = group_count[0];

	    for(int g=1; g < n_groups; g++)
	    {
		if( group_count[g] > most_c )
		{
		    most_c = group_count[g];
		    most_g = g;
		}
		if( group_count[g] < least_c )
		{
		    least_c = group_count[g];
		    least_g = g;
		}
	    }

	    if(most_g != least_g)
	    {
		
		// in which dimension is there the most difference ? 

		int    most_m = 0;
		double most_d = group_profile[most_g][0] - group_profile[least_g][0];
		if(most_d < 0)
		    most_d = -most_d;
		
		for(int m=1; m < n_meas; m++)
		{
		    double diff = group_profile[most_g][m] - group_profile[least_g][m];
		    if(diff < 0)
			diff = -diff;
		    if(diff > most_d)
		    {
			most_m = m;
			most_d = diff;
		    }
		}

		// System.out.println( "making " + most_g + " more like " + least_g + " in dimension " + most_m);

		// move 'most_m' towards 'least_m'

		double diff = group_profile[most_g][most_m] - group_profile[least_g][most_m];
		
		group_profile[most_g][most_m] -= (diff * 0.5);

		/*
		if(diff > 0)
		{
		    // most > least
		    group_profile[most_c][most_m] -= (diff * 0.1);
		}
		else
		{
		    // most < least
		    group_profile[most_c][most_m] += -(diff * 0.1);
		}
		*/
	    }
	    
	} 
    }

    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

    private void allocateSpotsToGroups( double[][] profile, short[] alloc, int[] count, double[] score )
    {
	if(alloc == null)
	    return;

	double[] s_profile = new double[ n_meas ];
	
	final int ns = alloc.length;

	for(int g=0; g < n_groups; g++)
	{
	    count[ g ] = 0;
	}

	for(int s = 0; s < ns; s++)
	{
	    if((apply_filter == false) || (!edata.filter(s)))
	    {
		// get the profile of this spot
		
		for(int m = 0; m < n_meas; m++)
		{
		    s_profile[ m ] = edata.eValue( meas_ids[m], s);
		}
		
		// compare it with each of the group profiles

		short best_g = 0;
		double best_d = distance_sq( n_meas, s_profile, profile[ 0 ] );
		
		for(short g=1; g < n_groups; g++)
		{
		    final double d = distance_sq( n_meas, s_profile, profile[ g ] );
		    if(d < best_d)
		    {
			best_d = d;
			best_g = g;
		    }
		}
		
		alloc[ s ] = best_g;
		
		count[ best_g ] ++;
		
		score[ best_g ] += best_d;
	    }
	    else
	    {
		alloc[ s ] =  -1;
	    }

	}


	// normalise the scores

	for(int g=0; g < n_groups; g++)
	{
	    if(count[ g ] > 0)
		score[ g ] /= count[ g ];
	}

	// System.out.println(ns + " spots into " + n_groups + " groups");

	/*
	System.out.println("group\tcount\n");
	for(int g=0; g < n_groups; g++)
	{
	    System.out.println(g + "\t" + count[ g ]);
	}
	*/
    }

    private double distance_sq( final double n_dim, final double[] p1, final double[] p2 )
    {

	double acc = .0;

	switch( distance_metric )
	{
	case 0:   // distance

	    //final double scale = 1.0 / (double) n_dim;  // is this needed?

	    for(int d=0; d < n_dim; d++)
	    {
		final double tmp = p1[d] - p2[d];
		//acc += ((tmp * tmp) * scale);
		acc += (tmp * tmp);
	    }
	    break;

	case 1: // slope
	    for(int d=1; d < n_dim; d++)
	    {
		final double d1 = p1[d] - p1[d-1];
		final double d2 = p2[d] - p2[d-1];
		
		final double tmp = d2-d1;
		acc += (tmp * tmp);
	    }
	    break;

	case 2:  // direction
	    for(int d=1; d < n_dim; d++)
	    {
		final boolean up1 = (p1[d] >= p1[d-1]);
		final boolean up2 = (p2[d] >= p2[d-1]);
		
		if(up1 != up2)
		    acc += 1.0;
	    }
	    break;
	}	
	
	return acc;
    }

    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\

    private boolean kill_action_thread = false;

    private class ActionThread extends Thread
    {
	public void run()
	{
	    setPriority( Thread.MIN_PRIORITY );

	    java.util.Date last_paint = new java.util.Date();
	 
	    int updates = 0;
	    int total_updates = 0;

	    while(!kill_action_thread)
	    {
		Thread.yield();


		if( is_iterating )
		{
		    adjustGroupProfiles();

		    updates++;

		    if(iterating_by_command)
		    {
			if(--iterate_command_counter <= 0)
			{
			    System.out.println("commanded iteration completed");
			    iterating_by_command = false;
			    if(iterate_command_done != null)
			    {
				iterate_command_done.signal();
			    }
			    is_iterating = iterate_jchkb.isSelected();
			}

		    }

		    adjust_label.setText( String.valueOf(++total_updates) );

		    // dont want to repaint all of the time...
		    java.util.Date now = new java.util.Date();
		    if((now.getTime() - last_paint.getTime()) > 2000)
		    {
			last_paint = now;

			pro_panel.repaint();
			
			// System.out.println("paint after " + updates + " updates");

			updates = 0;
		    }

		    /*
		    try
		    {
			Thread.sleep(100);
		    }
		    catch(InterruptedException ie)
		    {
		    }
		    */
		    
		}
		else
		{
		    updates = 0;
		    total_updates = 0;
		    adjust_label.setText( " " );
		    try
		    {
			Thread.sleep(300);
		    }
		    catch(InterruptedException ie)
		    {
		    }
		}
	    }
	    // System.out.println("ActionThread has died....");
	}	
    }

    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
    
    ProgressOMeter pm = null;
    boolean abort_clustering = false;

    private void makeGroupsIntoClusters( int mode, String new_name, CommandSignal done )
    {
	if(n_groups == 0)
	    return;
	
	abort_clustering = false;

	pm = new ProgressOMeter("Clustering", 3);

	pm.setCancelAction( new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    System.out.println( "clustering aborted by user..." );
		    abort_clustering = true;
		}
	    });
	
	try
	{
	    if(mode < 0)
	    {
		final String[] alg_opts = { "One cluster per group", 
					    "Hierarchical, most accurate", 
					    "Hierarchical, quickest",
					    "Hierarchical, experimental"	};
		
		mode = mview.getChoice( "Which method ?", alg_opts );
	    }
	    
	    if(new_name == null)
	    {
		String init_name = "sg" + n_groups;
		
		new_name = mview.getString("Parent cluster name", init_name);
	    }

	    pm.startIt();
	    
	    new ClusterThread( new_name, mode, done ).start();
	}
	catch(UserInputCancelled uic)
	{
	}
	
    }


    private class ClusterThread extends Thread
    {
	private String cname;
	private int alg;
	private CommandSignal done;

	public ClusterThread( String cname_, int alg_, CommandSignal done_ )
	{
	    cname = cname_;
	    alg = alg_;
	    done = done_;
	}

	public void run()
	{
	    long before = System.currentTimeMillis();

	    ExprData.Cluster parent = edata.new Cluster( cname );
	    
	    float hue = .0f;
	    float sat = 0.6f;
	    float hue_d = 1.0f / (float)n_groups;
	    
	    for(int g=0; g < n_groups; g++)
	    {
		if(abort_clustering)
		    break;

		try
		{
		    pm.setMessage(1,"Group " + (g+1) + " : " + group_count[ g ] + " spots");
		    
		    Thread.yield();
		    
		    int[] sids = new int[ group_count[ g ] ];
		    
		    int sp = 0;
		    for(int s=0; s < group_allocation.length; s++)
		    {
			if(group_allocation[s] == g)
			{
			    sids[sp++] = s;
			}
		    }
		    
		    Color colour = Color.getHSBColor(hue, sat, 1.f);
		    sat += 0.2f;
		    if(sat > 1.f)
			sat = 0.6f;
		    
		    hue += hue_d;
		    
		    switch(alg)
		    {
		    case 0:
			convertGroupToCluster( cname + ".G"+(g+1), colour, sids, parent );
			break;
		    case 1:
			makeHierarchicalCluster( cname + ".G"+(g+1), colour, sids, parent );
			break;
		    case 2:
			quickMakeHierarchicalCluster( cname + ".G"+(g+1), colour, sids, parent );
			break;
		    case 3:
			makeHierarchicalClusterUsingNearestNeighbourHeuristic( cname + ".G"+(g+1), colour, sids, parent );
			break;
		    }
		}
		catch(Exception ex)
		{
		    ex.printStackTrace();
		}

		System.gc();
	    }

	    if(!abort_clustering)
		edata.addCluster( parent );
	    
	    long after = (System.currentTimeMillis() - before) / 1000;
	    
	    System.out.println("clustering done in " + mview.niceTime(after));

	    pm.stopIt();
	    
	    if(done != null)
	       done.signal();
	}

    }

    //\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\
    ////\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//\\//

    private double[] distances;
    private int[]    offsets;

    //
    //  the distance store is a triangular space packed into a 1d array
    //
    //  for example, when there are 4 spots, the distance matrix would be
    //
    //        0 1 2 3 4
    //      0   a b c d
    //      1     f g h
    //      2       j k
    //      3         m
    //      4
    //      
    //   where a...m represent the distance between a pair of spots
    //
    //   to save on calls to new[],  this matrix is stored as an
    //   array, like this:
    //   
    //      a b c d e f g h i j k l m
    //
    //   the array is indexed by an offset table, 
    //   which in this example would be:
    //
    //      0 4 7 9
    //
    //   where each element in the offset table stores 
    //   the start index of the corresponding row
    //
    //   e.g, data for the third row starts at index 7 in the array
    //
    //  the expression to get index for the distance from S1 to S2 is
    //
    //     offset[ S1 ] + ( S2 - ( S1 + 1 ) )
    //

    private boolean setupDistanceStore( final int ns )
    {
	int count = 0;
	for(int s=0; s < ns; s++)
	    for(int s2=(s+1); s2 < ns; s2++)
		count++;

	// System.out.println("setupDistanceStore( ): " + ns + " spots gives " + count + " distances");

	try
	{
	    distances = new double[ count ];
	    offsets = new int [ ns ];

	    int p = 0;
	    for(int s=0; s < ns; s++)
	    {
		offsets[s] = p;
		p += (ns - (s+1));
	    }
	    
	    return true;
	}
	catch(java.lang.OutOfMemoryError oome)
	{
	    reportMemoryProblem();
	    return false;
	}
    }

    //
    // quickie implementation:
    //
    //    find two nearest things
    //     merge them
    //      update distances to reflect the merge
    //       and repeat until only 1 thing left
    //
    // 
    //  optimisation:
    //    various tweaks to exploit the addressing scheme 
    //    and use incremental calculations where possible
    //
    private void makeHierarchicalCluster( final String name, final Color colour, 
					  int[] spot_ids, ExprData.Cluster parent )
    {
	final int ns = spot_ids.length;

	if(ns == 0)
	    return;

	// System.out.println("clustering " + ns + " spots");

	pm.setMessage(2,"Setup...");

	if(setupDistanceStore( ns ) == false)
	    return;

	// store a local copy of the expression profiles
	
	try
	{
	    double[][] profiles = new double[ ns ][];
	    
	    for(int s=0; s < ns; s++)
	    {
		final int sid = spot_ids[ s ];
		double[] line = new double[ n_meas ];
		
		for(int m=0; m < n_meas; m++)
		{
		    line[ m ] = edata.eValue( meas_ids[ m ], sid );
		}
		
		profiles[s] = line;
	    }
	
	    // System.out.println("profiles ok");
	    
	    // compute initial distances for all pairs of spots
	    
	    int pos = 0;
	    for(int s=0; s < ns; s++)
	    {
		for(int s2=s+1; s2 < ns; s2++)
		{
		    
		    // in a 2d traversal of the triangular space, the indexing is linear,
		    // so the "offsets[s] + (s2-(s+1))" expression can be replaced by "pos++"		    
		    // was: distances[ offsets[s] + (s2-(s+1)) ] = ....
		    
		    distances[ pos++ ] = distance_sq( n_meas, profiles[s], profiles[s2] );
		}
	    }
	    
	    // no longer need the profiles...
	    profiles = null;
	    System.gc();
	    
	    // System.out.println("init " + pos + " distances ok");
	    
	    boolean[] used = new boolean[ ns ];
	    ExprData.Cluster[] nodes = new ExprData.Cluster[ns];
	    
	    boolean finished = false;
	    
	    ExprData.Cluster last_cl = null;
	    ExprData.Cluster cl = null;
	    
	    int count = 0;
	    int last_score = -1;
	    final double c_scale = (1.0 / (double) ns) * 100.0;
	    
	    final String dummy_name = "x";
	    
	    while(!finished)
	    {
		if(abort_clustering)
		    return;
		
		// find the two closest spots
		
		double best_dist = Double.MAX_VALUE;
		int best_s = -1;
		int best_s2 = -1;
		
		for(int s=0; s < ns; s++)
		{
		    if(!used[s])
		    {
			pos = offsets[s];
			for(int s2=s+1; s2 < ns; s2++)
			{
			    if(!used[s2])
			    {
				// WAS: double dist = distances[ offsets[s] + (s2-(s+1)) ];
				double dist = distances[ pos ];
				
				if(dist <= best_dist)
				{
				    best_dist = dist;
				    best_s = s;
				    best_s2 = s2;
				}
			    }
			    
			    pos++;   // this simulates (s2-(s+1))
			}
		    }
		}
		
		if((best_s < 0) || (best_s2 < 0))
		{
		    // did't find a pair to match
		    finished = true;
		}
		else
		{
		    
		    count++;
		    
		    if((count & 2) > 0)  // check 1 in 4 times
		    {
			int score = (int)((double) count * c_scale);
			if(score != last_score) 
			{
			    last_score = score;
			    pm.setMessage(2, score + "%");
			}
		    }
		
		    // join these two things (which can each be either a leaf or a nodes)
		    
		    // are either of the pair of things leaf elements (rather than nodes) ?
		    //
		    final boolean has_kids = ( nodes[ best_s ] == null) || ( nodes[ best_s2 ] == null);
		    
		    if(has_kids)
		    {
			Vector el_data = new Vector();
			if( nodes[ best_s ] == null)
			    el_data.addElement( edata.getSpotName( spot_ids[ best_s ] ) );
			
			if( nodes[ best_s2 ] == null)
			    el_data.addElement( edata.getSpotName( spot_ids[ best_s2 ] ) );
			
			cl = edata.new Cluster(dummy_name, ExprData.SpotName, el_data );
		    }
		    else
		    {
			// both things are nodes, this node has no element data
			
			cl = edata.new Cluster(dummy_name );
		    }
		    
		    last_cl = cl;
		    
		    // are either of the pair of things nodes ?
		    //
		    if( nodes[ best_s ] != null)
			cl.addCluster( nodes[ best_s ] );
		    
		    if( nodes[ best_s2 ] != null)
			cl.addCluster( nodes[ best_s2 ] );
		    
		    
		    // important: the following relies on the fact that best_s2 is always > best_s

		    
		    // replace the distance for all pairs (X, best_s) 
		    // with the average of distances (X, best_s) and (X, best_s2) 
		    
		    
		    // first all spots up to best_s (all in one column)
		    //
		    
		    {
			for(int sm=0; sm < best_s; sm++)
			{
			    if(!used[sm])
			    {
				final int i1 = offsets[sm] + (best_s-(sm+1));
				final int i2 = offsets[sm] + (best_s2-(sm+1));
				
				distances[i1] = (distances[i1] + distances[i2]) * 0.5;
			    }
			    
			}
			
			// now all spots from best_s onwards (all in one row)
			//
			int offset1 = offsets[best_s]  /* + ((best_s+1)-(best_s+1) )*/ ;   // sm==(best_s+1)
			int offset2 = offsets[best_s2] + ((best_s+1)-(best_s2+1));  // sm==(best_s+1)
			
			for(int sm=best_s+1; sm < ns; sm++)
			{
			    if(!used[sm])
			    {
				// the only variable here is 'sm', can replace with 2 offsets
				
				//final int i1 = offsets[best_s]  + (sm-(best_s+1));
				//final int i2 = offsets[best_s2] + (sm-(best_s2+1));
				
				//distances[i1] = (distances[i1] + distances[i2]) * 0.5;
				
				distances[offset1] = (distances[offset1] + distances[offset2]) * 0.5;
			    }
			    offset1++;
			    offset2++;
			}
		    }
		    
		    /*
		      System.out.println("joined " + best_s + 
		      ((nodes[ best_s ] == null) ? "(S)" : "(N)") + 
		      " and " + best_s2 + 
		      ((nodes[ best_s2 ] == null) ? "(S)" : "(N)") );
		    */
		    
		    // store the new cluster
		    nodes[best_s] = cl;
		    
		    // and mark 'best_s2' as "used" so it will not be considered again
		    used[best_s2] = true;
		}
	    }
	    
	    // System.out.println( ns + " spots put into " + count + " clusters");
	    
	    if(last_cl != null)
	    {
		// it is easier to colour and name the cluster once it has been built
		// (top down rather than bottom up)
	    
		nameAndColourCluster( last_cl, name, colour );
		
		parent.addCluster( last_cl );
	    }
	}
	catch(java.lang.OutOfMemoryError oome)
	{
	    reportMemoryProblem();
	    return;
	}

    }


    private void nameAndColourCluster( final ExprData.Cluster cl, final String name, final Color colour )
    {
	cl.setName( name );
	cl.setColour(colour);
	Vector chs = cl.getChildren();
	if(chs == null)
	    return;
	for(int c=0; c < chs.size(); c++)
	    nameAndColourCluster( ((ExprData.Cluster)chs.elementAt(c)), (name+"."+(c+1)), colour );
    }


    // ============================================================================

    // clustering v2:

    // looking for the O(n) solution, rather than the O(n^2) one above
    //
    // keep the distances sorted at all times to avoid having to search the
    // distance matrix

    // (hopefully) faster, but uses lots more memory


    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- 
    //
    // required ops:
    //
    //         find pair (s1, s2) with smallest distance
    //
    //         alter distance for (s1, s2)    [which is done quite a lot]
    //
    //
    //
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- 
 
    // idea 1
    //
    // how about keeping the N smallest distances in a cache,
    // and rescanning the array only when the cache is emptied.
    //
    // when distances are altered, the cached distanes must also
    // be altered (or removed)
    //

    // want to avoid having to re-sort the whole distance matrix every time
    // a distance is updated
    //
    // i.e. build the list once and keep it sorted using incremental operations
    //
    

    // idea 2
    //
    // keep a sorted distance list for each spot:
    //
    //        
    //        0 1 2 3 4
    //      0   a b c d
    //      1     f g h
    //      2       j k
    //      3         m
    //      4
    //
    //   where a...m represent the distance between a pair of spots
    // 
    //   sort distances for each spot:
    //
    //        0   3:c 2:b 4:d 1:a             // c < b < d < a
    //        1   3:g 4:h 2:f                 // g < h < f
    //        2   4:k 3:j                     // k < j
    //        3   4:m
    //        4 
    //
    //    each list of composed  of tuples of the form 'spot':'dist', 
    //
    // op1: shortest dist
    //
    //    overall shortest dist is found by checking the head of each distance list
    //    which has complexity O(n)
    //

    // op2: update dist
    //
    //    updating dist for ( i, j )
    //  
    //    because lists are sorted, can use binary search of dist list for spot 'i'
    //    to locate tuple for 'j' -- assuming we know the old distace
    //
    //    after the update, the list must be resorted
    //    (although only one entry may need to be moved, so could use a
    //     remove-and-re-insert-in-the-right-place operation)
    //
    //    (given infinite memory) could also record the tuple ordering for each
    //    list, i.e.
    // 
    //    for  3:c 2:b 4:d 1:a  the ordering is a:4 b:2 c:1 d:3, 
    // 	
    //    now finding the position of any 'j' is easy 
    //    but the tuple ordering needs to be rebuilt every time there is a
    //    distance update
    //
    //    
    

    private class DistRec
    {
	public int s1, s2;
	public double dist;
	
	//public DistRec next;

	public DistRec( int s1_, int s2_, double dist_)
	{
	    s1 = s1_; s2 = s2_; dist = dist_;
	}
    }

    // ==========================

    private interface DistanceMatrix
    {
	// must be called before any DistRecs are stored
	//
	public void initialise( int n_spots );

	// add a new entry
	//
	public void storeDistRec( DistRec dr );

	// remove an entry
	//
	public void removeDistRec( DistRec dr );


	// retrieve the entry with the smallest 'dist'
	//
	// returns null when no 'DistRec's remain
	//
	public DistRec getSmallestDistance();

	// signals that the 'dist' of the specified 'DistRec' has been changed
	//
	// (the old distance is not available)
	//
	// pre: 'dr' must previously have been passed to storeDistance()
	//
	public void updateDistRec( DistRec dr );

    }

    // ==========================

    /*
    private class DistCache implements DistanceMatrix
    {
	public void initialise( int n_spots )
	{

	}
	

	public void storeDistance( DistRec dr )
	{

	}
	

	public void updateDistance( DistRec dr )
	{
	    // 'dr' may or may not be in the cached data,
	    
	    // if not, should it be?

	    // if it is, reposition based on new 'dist' value

	    int pos = java.util.Arrays.binarySearch( data, dr, new DistRecComparator() );

	    if(pos < 0)
	    {
		// not in the cache, should it be?
		if( dr.dist < max_dist )
		{

		}
	    }
	}

	public DistRec getSmallestDistance()
	{
	    if(data.length == 0)
	    {
		fillCache();
	    }
	    
	    DistRec top = data[0];
	    
	    DistRec[] new_data = new DistRec[ data.length - 1];
	    for(int d=1; d < data.length; d++)
		new_data[d-1] = data[d];
	    
	    data = new_data;

	    return top;
	}

	private void sort()
	{
	    // DistRec[] dr_a = (DistRec[]) data.toArray( new DistRec[0] );

	    java.util.Arrays.sort( data, new DistRecComparator() );
	    
	    max_dist = data[ data.length - 1 ].dist;

	    System.out.println( data.length + " distances sorted, from " + data[0].dist +
				" to " + max_dist);
	    
	    // data.removeAll();

	    //data = new Vector( dr_a.asList() );

	    //for(int i=0; i < dr_a.length; i++)
	    //data.addElement( dr_a[i] );
	}

	private void fillCache()
	{
	    
	}


	private final int cache_size = 32;

	// private Vector data;
	private double max_dist;

	private double[] dist_a;

	private DistRec[] data;
    }
    */

   // ==========================


    /*
    // inserts the record at the correct place, and returns
    // the new head of the list
    private DistRec insertDistRec( DistRec head, DistRec dr )
    {
	if(head == null)
	{
	    // insert as new head
	    return dr;
	}

	if(dr.dist < head.dist)
	{
	    // insert as head
	    dr.next = head;
	    return dr;
	}

	DistRec insertp = head;
	DistRec nextp = null;

	while( true )
	{
	    if(insertp.next == null)
	    {
		// insert at the end of the list...
		insertp.next = dr;
		return head;
	    }
	    if(insertp.next.dist > dr.dist)
	    {
		// insert here...
		dr.next = insertp.next;
		insertp.next = dr;
		return head;
	    }
	    
	    insertp = insertp.next;
	}
    }
    */

    private class DistRecComparator implements java.util.Comparator
    {
	public int compare(Object o1, Object o2)
	{
	    DistRec dr1 = (DistRec) o1;
	    DistRec dr2 = (DistRec) o2;
	    
	    return (dr1.dist < dr2.dist) ? -1 : 1;
	}
	
	public boolean equals(Object o) { return false; }
    }

    private void quickMakeHierarchicalCluster( final String name, final Color colour, 
					       int[] spot_ids, ExprData.Cluster parent )
    {
	final int ns = spot_ids.length;

	if(ns == 0)
	    return;

	// System.out.println("clustering " + ns + " spots");

	pm.setMessage(2,"Setup...");

	// store a local copy of the expression profiles
	
	try
	{
	    double[][] profiles = new double[ ns ][];
	    
	    for(int s=0; s < ns; s++)
	    {
		final int sid = spot_ids[ s ];
		double[] line = new double[ n_meas ];
		
		for(int m=0; m < n_meas; m++)
		{
		    line[ m ] = edata.eValue( meas_ids[ m ], sid );
		}
		
		profiles[s] = line;
	    }
	
	    // System.out.println("profiles ok");
	    
	    // compute initial distances for all pairs of spots
	    
	    Vector dr_vec = new Vector();
	       
	    for(int s=0; s < ns; s++)
	    {
		int score = (int)(((double) s * 100) / (double) ns);
		pm.setMessage(2, score + "%");

		for(int s2=s+1; s2 < ns; s2++)
		{
		    
		    // in a 2d traversal of the triangular space, the indexing is linear,
		    // so the "offsets[s] + (s2-(s+1))" expression can be replaced by "pos++"
		    
		    // was: distances[ offsets[s] + (s2-(s+1)) ] = ....
		    
		    // System.out.println(s + "--" + s2 + "...");

		    DistRec dr = new DistRec( s, s2, distance_sq( n_meas, profiles[s], profiles[s2] ));
		    dr_vec.addElement( dr );
		}
	    }
	    
	    // no longer need the profiles...
	    profiles = null;
	    System.gc();
	    
	    DistRec[] dr_a = (DistRec[]) dr_vec.toArray( new DistRec[0] );
	    java.util.Arrays.sort( dr_a, new DistRecComparator() );
	    
	    System.out.println( dr_a.length + " distances");
	    
	    
	    // work along the array, pairing up nodes....

	    int pos = 0;

	    boolean[] used = new boolean[ ns ];
	    ExprData.Cluster[] nodes = new ExprData.Cluster[ns];
	    
	    boolean finished = false;
	    
	    ExprData.Cluster last_cl = null;
	    ExprData.Cluster cl = null;
	    
	    int count = 0;
	    int last_score = -1;
	    final double c_scale = (1.0 / (double) ns) * 100.0;
	    
	    final String dummy_name = "x";
	    
	    while(!finished)
	    {
		if(abort_clustering)
		    return;
		
		// find the next smallest distance...
		
		boolean good = false;
		while((!good) && (pos < dr_a.length))
		{
		    good = ( used[dr_a[pos].s1] == false ) && ( used[dr_a[pos].s2] == false );
		    if(!good)
			pos++;
		}

		if(pos < dr_a.length)
		{
		    count++;
		    
		    int best_s  = dr_a[pos].s1;
		    int best_s2 = dr_a[pos].s2;

		    if((count & 2) > 0)  // check 1 in 4 times
		    {
			int score = (int)((double) count * c_scale);
			if(score != last_score) 
			{
			    last_score = score;
			    pm.setMessage(2, score + "%");
			}
		    }
		
		    // join these two things (which can each be either a leaf or a nodes)
		    
		    // are either of the pair of things leaf elements (rather than nodes) ?
		    //
		    final boolean has_kids = ( nodes[ best_s ] == null) || ( nodes[ best_s2 ] == null);
		    
		    if(has_kids)
		    {
			Vector el_data = new Vector();
			if( nodes[ best_s ] == null)
			    el_data.addElement( edata.getSpotName( spot_ids[ best_s ] ) );
			
			if( nodes[ best_s2 ] == null)
			    el_data.addElement( edata.getSpotName( spot_ids[ best_s2 ] ) );
			
			cl = edata.new Cluster( dummy_name, ExprData.SpotName, el_data );
		    }
		    else
		    {
			// both things are nodes, this node has no element data
			
			cl = edata.new Cluster( dummy_name );
		    }
		    
		    last_cl = cl;
		    
		    // are either of the pair of things nodes ?
		    //
		    if( nodes[ best_s ] != null)
			cl.addCluster( nodes[ best_s ] );
		    
		    if( nodes[ best_s2 ] != null)
			cl.addCluster( nodes[ best_s2 ] );
		    
		    // store the new cluster
		    nodes[best_s] = cl;
		    
		    // and mark 'best_s2' as "used" so it will not be considered again
		    used[best_s2] = true;
		}
		else
		{
		    finished = true;
		}
	    }
	    
	    // System.out.println( ns + " spots put into " + count + " clusters");
	    
	    if(last_cl != null)
	    {
		// it is easier to colour and name the cluster once it has been built
		// (top down rather than bottom up)
	    
		nameAndColourCluster( last_cl, name, colour );
		
		parent.addCluster( last_cl );
	    }
	}
	catch(java.lang.OutOfMemoryError oome)
	{
	    reportMemoryProblem();
	    return;
	}

    }



    // ============================================================================


    private void convertGroupToCluster( final String name, final Color colour, 
					int[] spot_ids, ExprData.Cluster parent )
    {
	final int ns = spot_ids.length;

	try
	{
	    Vector el_data = new Vector();
	
	    
	    for(int s=0; s < ns; s++)
		el_data.addElement( edata.getSpotName( spot_ids[s] ) );
	    
	    ExprData.Cluster cl = edata.new Cluster( name, ExprData.SpotName, el_data );
	    
	    nameAndColourCluster( cl, name, colour );
	    
	    parent.addCluster( cl );
	}
	catch(java.lang.OutOfMemoryError oome)
	{
	    reportMemoryProblem();
	    return;
	}
    }





    // ============================================================================

    private class NNHNode
    {
	int source_id;

	double[] data;

	double distance_to_nearest_node;
	NNHNode nearest_node;

	int index_in_node_list;

	NNHNode paired_node;

	public NNHNode( int source_id_, double[] data_ )
	{
	    paired_node = null;
	    nearest_node = null;

	    source_id = source_id_;
	    data = data_;
	}

	public NNHNode( NNHNode n1, NNHNode n2 )
	{
	    data = new double[ n1.data.length ];

	    for(int d=0; d < n1.data.length; d++)
		data[ d ] = ( n1.data[ d ] + n2.data[ d ] ) * 0.5;
	    
	    paired_node = n2;

	    source_id = n1.source_id;

	    nearest_node = null;
	}
    }

    private class NearestNeighbourHeuristic
    {
	public NearestNeighbourHeuristic( int[] source_ids, double[][] data )
	{
	    n_dim = data[0].length;
	    
	    // build the node array

	    n_nodes = data.length;

	    nodes = new NNHNode[ n_nodes ];

	    for(int n=0; n < n_nodes; n++)
	    {
		nodes[ n ] = new NNHNode( source_ids[ n ], data[ n ] );
		nodes[ n ].index_in_node_list = n;
	    }

	    // compute initial nearest nodes

	    for(int n=0; n < n_nodes; n++)
	    {
		findNearestNeighbour( nodes[ n ] );
	    }

	    free_slots_v = new java.util.Vector();
	}


	private void findNearestNeighbour( final NNHNode node )
	{
	    node.distance_to_nearest_node = Double.MAX_VALUE;
	    
	    for(int n=0; n < nodes.length; n++)
	    {
		if( n != node.index_in_node_list )
		{
		    if( nodes[ n ] != null )
		    {
			double dist_sq = distance_sq( n_dim, node.data, nodes[ n ].data );
			
			if( dist_sq < node.distance_to_nearest_node )
			{
			    node.distance_to_nearest_node = dist_sq;
			    node.nearest_node = nodes[ n ];
			}
		    }
		}
	    }
	}


	public void insert( NNHNode node )
	{
	    // find a free index in the list

	    if( free_slots_v.size() == 0 )
	    {
		System.err.println("NearestNeighbourHeuristic.insert(): unable to insert node, list is full");
		return;
	    }
	    Integer free_slot_i = (Integer) free_slots_v.elementAt( 0 );
	    free_slots_v.removeElementAt( 0 );

	    final int n = free_slot_i.intValue();
	    
	    if( nodes[ n ] == null )
	    {
		// insert this node:

		nodes[ n ] = node;
		node.index_in_node_list = n;

		// find it's nearest neighbour

		findNearestNeighbour( node );

		// and check all other nodes to see if they are nearer to
		// this new node than they are to their current nearest node

		for(int n2=0; n2 < nodes.length; n2++)
		{
		    if( n2 != n )
		    {
			if( nodes[ n2 ] != null )
			{
			    double dist_sq = distance_sq( n_dim, node.data, nodes[ n ].data );
			    
			    if( dist_sq < nodes[ n2 ].distance_to_nearest_node )
			    {
				// yes indeed, the new node is now the nearest neighbour to n2
				
				nodes[ n2 ].distance_to_nearest_node = dist_sq;
				nodes[ n2 ].nearest_node = node;
			    }
			}
		    }
		}
	    }
	    else
	    {
		System.err.println("NearestNeighbourHeuristic.insert(): unable to insert node, list is broken");
	    }
	}


	public void delete( NNHNode node )
	{
	    nodes[ node.index_in_node_list ] = null;
	    n_nodes--;

	    // check whether any other nodes considered this node
	    // to be their nearest neighbour

	    for(int n=0; n < nodes.length; n++)
	    {
		if( nodes[ n ] != null )
		{
		    if( nodes[ n ].nearest_node == node )
		    {
			// find the new nearest neighbour for this node
			findNearestNeighbour( nodes[ n ] );
		    }
		}
	    }
	}


	// it is slightly more efficient to delete 2 things at once
	//
	public void delete( NNHNode n1, NNHNode n2 )
	{
	    nodes[ n1.index_in_node_list ] = null;
	    nodes[ n2.index_in_node_list ] = null;

	    if( free_slots_v.size() < 5 )
	    {
		free_slots_v.addElement( new Integer( n1.index_in_node_list ) );
		free_slots_v.addElement( new Integer( n2.index_in_node_list ) );
	    }

	    n_nodes -= 2;

	    // check whether any other nodes considered this node
	    // to be their nearest neighbour

	    for(int n=0; n < nodes.length; n++)
	    {
		if( nodes[ n ] != null )
		{
		    if( ( nodes[ n ].nearest_node == n1 ) || ( nodes[ n ].nearest_node == n2 ) )
		    {
			// find the new nearest neighbour for this node
			findNearestNeighbour( nodes[ n ] );
		    }
		}
	    }
	}


	public NNHNode[] getNearestPair()
	{
	    double shortest_distance_between_nodes = Double.MAX_VALUE;

	    NNHNode closest_node = null;

	    for(int n=0; n < nodes.length; n++)
	    {
		if( nodes[ n ] != null )
		{
		    if( nodes[ n ].distance_to_nearest_node < shortest_distance_between_nodes )
		    {
			shortest_distance_between_nodes = nodes[ n ].distance_to_nearest_node;
			closest_node = nodes[ n ];
		    }
		}
	    }

	    if( closest_node == null )
		return null;

	    NNHNode[] result = new NNHNode[ 2 ];

	    result[0] = closest_node;
	    result[1] = closest_node.nearest_node;
	    
	    return result ;
	}


	public NNHNode getFirstNode()
	{
	    for(int n=0; n < nodes.length; n++)
	    {
		if( nodes[ n ] != null )
		{
		    return nodes[ n ];
		}
	    }

	    return null;
	}


	public int n_dim;

	public NNHNode[] nodes;
	public int n_nodes;

	public java.util.Vector free_slots_v;
    }


    private void makeHierarchicalClusterUsingNearestNeighbourHeuristic( final String name, final Color colour, 
									int[] spot_ids, ExprData.Cluster parent )
    {
	final int n_spots = spot_ids.length;

	if( n_spots == 0 )
	{
	    return;
	}

	double[][] profiles = new double[ n_spots ][];
	
	for(int s=0; s < n_spots; s++)
	{
	    final int sid = spot_ids[ s ];
	    double[] line = new double[ n_meas ];
	    
	    for(int m=0; m < n_meas; m++)
	    {
		line[ m ] = edata.eValue( meas_ids[ m ], sid );
	    }
	    
	    profiles[s] = line;
	}

	System.out.println("data prepared");


	NearestNeighbourHeuristic nnh = new NearestNeighbourHeuristic( spot_ids, profiles );
	

	System.out.println("node list initialised");

	int cluster_node_id = 1;
	
	while( nnh.n_nodes > 1 )
	{
	    if(abort_clustering)
		return;
	    
	    NNHNode[] pair = nnh.getNearestPair();

	    nnh.delete( pair[0], pair[1] );
	    
	    NNHNode new_cluster = new NNHNode( pair[0], pair[1] );

	    nnh.insert( new_cluster );
	}

	System.out.println("clustering done");

	// now build a cluster tree
	 
	ExprData.Cluster root = makeClusterTree( name, nnh.getFirstNode() );
	
	if( root != null )
	    parent.addCluster( root );
    }

    private ExprData.Cluster makeClusterTree( final String name, final NNHNode node )
    {
	if(abort_clustering)
	    return null;

	Vector el_data = new Vector();
	el_data.addElement( edata.getSpotName( node.source_id ) );

	ExprData.Cluster cl = edata.new Cluster( name + "." + node.source_id, ExprData.SpotName, el_data );
	    
	if( node.paired_node != null )
	{
	    cl.addCluster( makeClusterTree( name, node.paired_node ) );
	}
	

	return cl;
    }


    // ============================================================================


    private void reportMemoryProblem()
    {
	System.gc();

	if( pm != null )
	    pm.stopIt();

	mview.alertMessage("Not enough memory available for clustering this group.\n\n" +
			   "Try using the one of the other clustering methods, or allocate more memory\n" + 
			   "to the Java virtual machine using the \"-Xmx\" command line option." );
	
	if( pm != null )
	    pm.startIt();
    }



    // ============================================================================
    //
    //  doings
    //
    // ============================================================================
    // ============================================================================

    private TextLayout min_label, zero_label, max_label;
    private TextLayout[] meas_labels;
    private int  min_label_o, zero_label_o, max_label_o;
    private int[] meas_labels_o;
    private Font font;
    private FontRenderContext frc;

    private double spot_font_scale = 1.0;

    private boolean draw_zero = false;

    private maxdView mview;
    private ExprData edata;
    private DataPlot dplot;

    private JFrame frame;

    private ProfilePanel pro_panel;
    private DragAndDropTree cl_tree;
    private DragAndDropList meas_list;

    private boolean auto_space;
    private boolean col_sel;

    private JCheckBox iterate_jchkb;
    private JTextField n_groups_jtf;
    private JCheckBox edit_profiles_jchkb;
    private JComboBox distance_metric_jcb;
    private JCheckBox auto_remove_empty_jchkb;
    private JCheckBox apply_filter_jchkb;
    private JLabel adjust_label;
    private JSlider rate_slider;
    private JSlider noise_slider;

    private JLabel noise_label, rate_label;

    private JComboBox adjust_what_jcb;
    private int adjust_what;
    private double adjust_rate;

    private JComboBox adjust_mode_jcb;
    private int adjust_mode;

    private double  adjust_noise_level;
    private boolean adjust_auto_remove_empty;

    private ExprData.NameTagSelection nt_sel;
    private NameTagSelector nts;

    private Vector sel_cls;     // the selected clusters
    private Vector sel_ids;     // the element_ids for the selected clusters

    private int[]  meas_ids;

    private boolean apply_filter;
    private boolean include_children;

    private int n_cols;
    private int n_rows;

    private int graph_w;
    private int graph_h;

    private int ticklen;

    private double current_min = .0;
    private double current_max = .1;

    private int graph_sx;   // step size between graphs
    private int graph_sy;

    private int xoff, yoff;

    private int graph_x_axis_step;
    private int graph_y_axis_step;

    private double graph_y_axis_scale;
    private double graph_y_axis_min;

    private Color background_col;
    private Color text_col;

    // private SpotPickerNode root_spot_picker;

    private ProfilePicker root_profile_picker;

    private AxisManager axis_man;
    private DecorationManager deco_man;
 
    private int sel_handle;

    private Timer timer;

    private boolean iterating_by_command = false;
    private int iterate_command_counter;
    private CommandSignal iterate_command_done;

}
