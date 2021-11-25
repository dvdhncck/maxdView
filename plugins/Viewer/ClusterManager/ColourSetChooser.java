import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import javax.swing.tree.*;

// --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
// --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
// --- --- ---  
// --- --- ---   ColourSetChooser
// --- --- ---  
// --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
// --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

class ColourSetChooser extends JFrame 
{
    /*
    private int n_presets = 8;

    private final static Color preset_from[] = 
    { Color.white, 
      Color.red, 
      Color.green,
      Color.blue,
      Color.yellow,
      Color.magenta,
      Color.pink,
      Color.orange
    };

    private final static Color preset_to[] = 
    { Color.black, 
      Color.black, 
      Color.black,
      Color.black,
      Color.white,
      Color.white,
      Color.white,
      Color.green
    };
    */

    public ColourSetChooser(maxdView mview_, ExprData edata_, ExprData.Cluster clust)
    {
	super("Cluster Colour Chooser");

	cluster = clust;
	mview = mview_;
	edata = edata_;
	
	addWindowListener(new WindowAdapter() 
			  {
			      public void windowClosing(WindowEvent e)
			      {
				  saveSettings();
				  setVisible(false);
			      }
			  });

	getContentPane().setLayout(new BorderLayout());

	top_panel = new JPanel();
	top_panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

	//panel.setPreferredSize(new Dimension(250, 300));

	buildComponents(top_panel);
	getContentPane().add(top_panel);


	//new_from = cluster.getFromColour();
	//new_to   = cluster.getToColour();

	pack();
	setVisible(true);

	updateOptions();
	// blend_panel.setVisible(false);

    }

    private JPanel top_panel;

    public void setCluster(ExprData.Cluster clust)
    {
	
    }

    public void buildComponents(JPanel top_panel)
    {
	GridBagLayout gridbag = new GridBagLayout();
	top_panel.setLayout(gridbag);

	int line = 0;

	Dimension fillsize = new Dimension(16,16);

	{
	    JPanel wrapper = new JPanel();
	    wrapper.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));

	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrapper.setLayout(w_gridbag);
	    wrapper.setBorder(BorderFactory.createEtchedBorder());

	    {
		JLabel label = new JLabel(cluster.getName());
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		c.weightx = c.weighty = 1.0;
		w_gridbag.setConstraints(label, c);
		wrapper.add(label);
	    }
	
	    {
		JLabel label = new JLabel("Currently  ");
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = c.weighty = 1.0;
		c.anchor = GridBagConstraints.EAST;
		w_gridbag.setConstraints(label, c);

		wrapper.add(label);
	    }

	    {
		current_jb = new JButton(); 
		// RampViewer(cluster.getColourRamp(cluster.getColourSet(), ramp_steps), false);
		//current_jb.setPreferredSize(new Dimension(50,30));
		current_jb.setBackground(cluster.getColour());

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 1;
		c.weightx = c.weighty = 1.0;
		c.anchor = GridBagConstraints.WEST;
		//c.fill = GridBagConstraints.HORIZONTAL;
		w_gridbag.setConstraints(current_jb, c);
		
		wrapper.add(current_jb);
	    }

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    c.weightx = c.weighty = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(wrapper, c);
	    
	    top_panel.add(wrapper);
	}

	{
	    Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    gridbag.setConstraints(filler, c);
	    
	    top_panel.add(filler);
	}

	/*
	{
	    JLabel label = new JLabel("Change to  ");
	    top_panel.add(label);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.weightx = c.weighty = 1.0;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	}
	{
	    new_choice_rv = new RampViewer(cluster.getColourRamp(cluster.getColourSet(), ramp_steps));
	    new_choice_rv.setPreferredSize(new Dimension(50,30));
	    top_panel.add(new_choice_rv);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.weightx = c.weighty = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    gridbag.setConstraints(new_choice_rv, c);
	}
	*/

	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrapper.setLayout(w_gridbag);
	    wrapper.setBorder(BorderFactory.createEtchedBorder());

	    {
		children_jchkb = new JCheckBox("Apply to children");

		children_jchkb.setSelected(mview.getBooleanProperty("clustman.colour_apply_to_children", false));
		
		children_jchkb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    updateOptions();
			}
		    });

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		w_gridbag.setConstraints(children_jchkb, c);
		
		wrapper.add(children_jchkb);
	    }

	    ButtonGroup bg = new ButtonGroup();
	    
	    {
		depth_jrb = new JRadioButton("Depth first");

		depth_jrb.setSelected(mview.getBooleanProperty("clustman.colour_depth_first", false));
		
		depth_jrb.setHorizontalTextPosition(AbstractButton.LEFT);
		depth_jrb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    updateOptions();
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		w_gridbag.setConstraints(depth_jrb, c);
		
		wrapper.add(depth_jrb);
		bg.add(depth_jrb);
	    }
	    {
		breadth_jrb = new JRadioButton("Breadth first");

		breadth_jrb.setSelected(mview.getBooleanProperty("clustman.colour_breadth_first", false));

		breadth_jrb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    updateOptions();
			}
		    });
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 1;
		w_gridbag.setConstraints(breadth_jrb, c);
		
		wrapper.add(breadth_jrb);
		bg.add(breadth_jrb);
	    }

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    c.weightx = c.weighty = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(wrapper, c);
	    
	    top_panel.add(wrapper);
	}

	{
	    Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    gridbag.setConstraints(filler, c);
	    
	    top_panel.add(filler);
	}

	new_to   = mview.intToColor(mview.getIntProperty("clustman.colour_to", 0x000000));
	new_from = mview.intToColor(mview.getIntProperty("clustman.colour_from", 0xFFFFFF));

	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrapper.setLayout(w_gridbag);
	    wrapper.setBorder(BorderFactory.createEtchedBorder());

	    ButtonGroup bg = new ButtonGroup();
	    
	    int blend_mode = mview.getIntProperty("clustman.colour_blend_mode", 0);
	    
	    {
		solid_jrb = new JRadioButton("Fixed");

		solid_jrb.setSelected(blend_mode == 0);
		
		solid_jrb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    updateOptions();
			}
		    });
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		c.anchor = GridBagConstraints.EAST;
		w_gridbag.setConstraints(solid_jrb, c);
		
		wrapper.add(solid_jrb);
		bg.add(solid_jrb);
	    }

	    {
		blend_hsb_jrb = new JRadioButton("Blend HSB");

		blend_hsb_jrb.setSelected(blend_mode == 1);

		blend_hsb_jrb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    updateOptions();
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		w_gridbag.setConstraints(blend_hsb_jrb, c);
		
		wrapper.add(blend_hsb_jrb);
		bg.add(blend_hsb_jrb);
	    }
	    
	    {
		blend_rgb_jrb = new JRadioButton("Blend RGB");

		blend_rgb_jrb.setSelected(blend_mode == 2);

		blend_rgb_jrb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    updateOptions();
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		w_gridbag.setConstraints(blend_rgb_jrb, c);
		
		wrapper.add(blend_rgb_jrb);
		bg.add(blend_rgb_jrb);
	    }
	
	    {
		blend_panel = new JPanel();
		GridBagLayout b_gridbag = new GridBagLayout();
		blend_panel.setLayout(b_gridbag);
		
		{
		    JLabel label = new JLabel("From  ");
		    
		    GridBagConstraints c = new GridBagConstraints();
		    c.gridx = 0;
		    c.gridy = 0;
		    //c.weightx = c.weighty = 1.0;
		    c.anchor = GridBagConstraints.EAST;
		    w_gridbag.setConstraints(label, c);
		    
		    blend_panel.add(label);
		}
		{   
		    from_jb = new JButton();
		    from_jb.setPreferredSize(new Dimension(50,25));
		    from_jb.setBackground(new_from);
		
		    from_jb.addActionListener(new ActionListener() 
			{
			    public void actionPerformed(ActionEvent e) 
			    {
				Color newColour = JColorChooser.showDialog(ColourSetChooser.this,
									   "Choose Down Regulated Colour",
									   cluster.getFromColour());
				if (newColour != null) 
				{
				    new_from = newColour;
				    setNewChoice();
				}
			    }
			});
		    GridBagConstraints c = new GridBagConstraints();
		    c.gridx = 1;
		    c.gridy = 0;
		    c.fill = GridBagConstraints.HORIZONTAL;
		    //c.weightx = c.weighty = 1.0;
		    c.anchor = GridBagConstraints.WEST;
		    b_gridbag.setConstraints(from_jb, c);
		    
		    blend_panel.add(from_jb);
		}
		
		{
		    JLabel label = new JLabel(" To ");
		    
		    GridBagConstraints c = new GridBagConstraints();
		    c.gridx = 2;
		    c.gridy = 0;
		    //c.weightx = c.weighty = 1.0;
		    c.anchor = GridBagConstraints.EAST;
		    b_gridbag.setConstraints(label, c);
		    
		    blend_panel.add(label);
		}
		{   
		    to_jb = new JButton();
		    to_jb.setPreferredSize(new Dimension(50,25));
		    to_jb.setBackground(new_to);
		    
		    to_jb.addActionListener(new ActionListener() 
			{
			    public void actionPerformed(ActionEvent e) 
			    {
				Color newColour = JColorChooser.showDialog(ColourSetChooser.this,
									   "Choose Down Regulated Colour",
									   cluster.getToColour());
				if (newColour != null) 
				{
				    new_to = newColour;
				    setNewChoice();
				}
			    }
			});
		    
		    GridBagConstraints c = new GridBagConstraints();
		    c.gridx = 3;
		    c.gridy = 0;
		    c.fill = GridBagConstraints.HORIZONTAL;
		    //c.weightx = c.weighty = 1.0;
		    c.anchor = GridBagConstraints.WEST;
		    b_gridbag.setConstraints(to_jb, c);
		    
		    blend_panel.add(to_jb);
		}

		{
		    GridBagConstraints c = new GridBagConstraints();
		    c.gridx = 0;
		    c.gridy = 1;
		    c.weightx = 1.0; //c.weighty = 1.0;
		    c.gridwidth = 3;
		    c.fill = GridBagConstraints.HORIZONTAL;
		    w_gridbag.setConstraints(blend_panel, c);
		    wrapper.add(blend_panel);
		    
		}

		{
		    solid_panel = new JPanel();

		    GridBagLayout s_gridbag = new GridBagLayout();
		    solid_panel.setLayout(s_gridbag);
		    
		    JLabel label = new JLabel(" Colour ");
		    
		    GridBagConstraints c = new GridBagConstraints();
		    c.gridx = 0;
		    c.gridy = 0;
		    //c.weightx = c.weighty = 1.0;
		    c.anchor = GridBagConstraints.EAST;
		    s_gridbag.setConstraints(label, c);
		    
		    solid_panel.add(label);
		
		    solid_col_jb = new JButton();
		    solid_col_jb.setPreferredSize(new Dimension(50,25));
		    solid_col_jb.setBackground(new_to);
		    
		    solid_col_jb.addActionListener(new ActionListener() 
			{
			    public void actionPerformed(ActionEvent e) 
			    {
				Color newColour = JColorChooser.showDialog(ColourSetChooser.this,
									   "Choose Down Regulated Colour",
									   cluster.getToColour());
				if (newColour != null) 
				{
				    new_to = newColour;
				    setNewChoice();
				}
			    }
			});
		    
		    c = new GridBagConstraints();
		    c.gridx = 1;
		    c.gridy = 0;
		    c.fill = GridBagConstraints.HORIZONTAL;
		    //c.weightx = c.weighty = 1.0;
		    c.anchor = GridBagConstraints.WEST;
		    s_gridbag.setConstraints(solid_col_jb, c);
		    
		    solid_panel.add(solid_col_jb);
		}

		{
		    GridBagConstraints c = new GridBagConstraints();
		    c.gridx = 0;
		    c.gridy = 1;
		    c.weightx = 1.0; //c.weighty = 1.0;
		    c.gridwidth = 3;
		    c.fill = GridBagConstraints.HORIZONTAL;
		    w_gridbag.setConstraints(solid_panel, c);
		    wrapper.add(solid_panel);
		    
		}

	    }

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    c.weightx = c.weighty = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(wrapper, c);
	    top_panel.add(wrapper);
	}

	{
	    Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    gridbag.setConstraints(filler, c);
	    
	    top_panel.add(filler);
	}

	/*
	{
	    JLabel label = new JLabel("Presets");
	    top_panel.add(label);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.gridwidth = 2;
	    c.weightx = c.weighty = 1.0;
	    c.anchor = GridBagConstraints.SOUTH;
	    gridbag.setConstraints(label, c);
	}

	line++;

	{
	    JPanel preset_panel = new JPanel();
	    GridBagLayout preset_gridbag = new GridBagLayout();
	    preset_panel.setLayout(preset_gridbag);

	    int presets_per_row = 3;
	    int row = 0;
	    int in_this_row = 0;
	    
	    RampViewer preset_rv = null;

	    for(int pre=0; pre < (n_presets+1); pre++)
	    {
		{
		    
		    if(pre > 0)
			preset_rv = new RampViewer(RampViewer.makeRamp(preset_from[pre-1], preset_to[pre-1], ramp_steps));
		    else
			// preset 0 is the special hue spectrum preset
			preset_rv = new RampViewer(cluster.getColourRamp(0, ramp_steps));

		    preset_rv.setPreferredSize(new Dimension(50,30));
		    preset_panel.add(preset_rv);
		    
		    preset_rv.addActionListener(new PresetActionListener(pre));
		    
		    GridBagConstraints c = new GridBagConstraints();

		    c.gridx = in_this_row;
		    c.gridy = row;

		    if(++in_this_row == presets_per_row)
		    {
			row++;
			in_this_row = 0;
		    }

		    c.weightx = c.weighty = 1.0;
		    preset_gridbag.setConstraints(preset_rv, c);
		}
	    }

	    top_panel.add(preset_panel);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.gridwidth = 2;
	    c.weightx = c.weighty = 1.0;
	    c.anchor = GridBagConstraints.NORTH;
	    gridbag.setConstraints(preset_panel, c);
	}
	
	line += 2;
	 
	{
	    JLabel label = new JLabel("Custom");
	    top_panel.add(label);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.gridwidth = 2;
	    c.weightx = c.weighty = 1.0;
	    c.anchor = GridBagConstraints.SOUTH;
	    gridbag.setConstraints(label, c);
	}

	line++;
	*/

	{
	    JPanel inner_panel = new JPanel();
	    GridBagLayout inner_gridbag = new GridBagLayout();
	    inner_panel.setLayout(inner_gridbag);
	    inner_panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));

	    {
		final JButton jb = new JButton("Close");
		inner_panel.add(jb);
		jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					 {
					     saveSettings();
					     setVisible(false);
					 }
				     });
	    }
	    
	    {
		final JButton jb = new JButton("Apply");
		inner_panel.add(jb);
		jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					 {
					     colourCluster();
					     saveSettings();
					 }
				     });
	    }

	    {
		final JButton jb = new JButton("Help");
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    mview.getPluginHelpTopic("ClusterManager", "ClusterManager", "#colsel");
			}
		    });
		inner_panel.add(jb);
	    }
	    
	    top_panel.add(inner_panel);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(inner_panel, c);
	}
	
	// updateOptions();

    }

    private void saveSettings()
    {
	mview.putBooleanProperty("clustman.colour_apply_to_children", children_jchkb.isSelected());
	mview.putBooleanProperty("clustman.colour_depth_first", depth_jrb.isSelected());
	mview.putBooleanProperty("clustman.colour_breadth_first", breadth_jrb.isSelected());

	if(solid_jrb.isSelected())
	    mview.putIntProperty("clustman.colour_blend_mode", 0);
	if(blend_hsb_jrb.isSelected())
	    mview.putIntProperty("clustman.colour_blend_mode", 1);
	if(blend_rgb_jrb.isSelected())
	    mview.putIntProperty("clustman.colour_blend_mode", 2);

	mview.putIntProperty("clustman.colour_to", mview.colorToInt(new_to));
	mview.putIntProperty("clustman.colour_from", mview.colorToInt(new_from));
    }

    private void updateOptions()
    {
	boolean mode = children_jchkb.isSelected();

	depth_jrb.setEnabled(mode);
	breadth_jrb.setEnabled(mode);
	//solid_jrb.setSelected(!mode);
	if(!mode)
	    solid_jrb.setSelected(true);
	blend_hsb_jrb.setEnabled(mode);
	blend_rgb_jrb.setEnabled(mode);
	from_jb.setEnabled(mode);

	mode = solid_jrb.isSelected();
	depth_jrb.setEnabled(!mode);
	breadth_jrb.setEnabled(!mode);

	solid_panel.setVisible(mode);
	blend_panel.setVisible(!mode);
    }

    private void setNewChoice()
    {
	/*
	if(new_preset == 0)
	{
	    new_choice_rv.setRamp(cluster.getColourRamp(0,ramp_steps));
	}
	else
	{
	    new_choice_rv.setRamp(RampViewer.makeRamp(new_from, new_to, ramp_steps));
	}
	*/
	from_jb.setBackground(new_from);
	to_jb.setBackground(new_to);
	solid_col_jb.setBackground(new_to);
    }

    //
    // =================================================================
    // improved colouring routines.......
    // =================================================================
    // 

    private void colourCluster()
    {
	if(children_jchkb.isSelected())
	{
	    if(breadth_jrb.isSelected())
		colourClusterByBreadth(cluster, 0.0, 1.0);
	    else
		colourClusterByDepth(cluster);

	    edata.generateClusterUpdate(ExprData.ColourChanged);
	}
	else
	{
	    edata.setClusterColour(cluster, new_to);
	}
    }

    // --------------------------------------------------------
    // depth-first
    // --------------------------------------------------------

    private void colourClusterByDepth(ExprData.Cluster cl)
    {
	// what is the max depth from this node?

	int max_depth = findDeepestChild(cl);

	recursivelyColourByDepth(cluster, 0, max_depth);
    }

    private int findDeepestChild(ExprData.Cluster cl)
    {
	int local_max = 0;
	
	Vector ch = cl.getChildren();
	if(ch != null)
	{
	    for(int c=0; c< ch.size(); c++)
	    {
		int child_depth = findDeepestChild( (ExprData.Cluster) ch.elementAt(c) );
		if(child_depth > local_max)
		    local_max = child_depth;
	    }
	}
	return local_max + 1;
    }

    private void recursivelyColourByDepth(ExprData.Cluster cl, int depth, int max_depth)
    {
	double depth_scale = 1.0 / max_depth;
	
	double this_depth = (double) depth * depth_scale;

	if(solid_jrb.isSelected())
	{
	    cl.setColour(new_to);
	}
	else
	{
	    if(blend_hsb_jrb.isSelected())
		cl.setColour(getBlendHSB(this_depth));
	    else
		cl.setColour(getBlendRGB(this_depth));
	}
	

	Vector chld = cl.getChildren();
	if(chld != null)
	{
	    depth++;

	    for(int ci=0; ci < chld.size(); ci++)
	    {
		ExprData.Cluster ch = (ExprData.Cluster) chld.elementAt(ci);
		
		recursivelyColourByDepth(ch, depth, max_depth);
	    }
	}
    }

    // --------------------------------------------------------
    // breadth-first
    // --------------------------------------------------------

    private void colourClusterByBreadth(ExprData.Cluster cl, double s, double e)
    {
	// colour this cluster using the midpoint of the range
	    
	if(solid_jrb.isSelected())
	{
	    cl.setColour(new_to);
	}
	else
	{
	    double bval = (s+e) * 0.5;

	    if(blend_hsb_jrb.isSelected())
		cl.setColour(getBlendHSB(bval));
	    else
		cl.setColour(getBlendRGB(bval));
	}
	
	// get the total number of descendants in each child...
	
	int total_des = 0;
	Vector chld = cl.getChildren();
	if(chld != null)
	{
	    int[] chld_des = new int[chld.size()];
	    
	    for(int ci=0; ci < chld.size(); ci++)
	    {
		ExprData.Cluster ch = (ExprData.Cluster) chld.elementAt(ci);
		
		chld_des[ci] = getDescendentCount(ch);
		
		total_des += chld_des[ci];
	    }
	    
	    // subdivide the range between the children based on the number of descendants in each one
	    
	    double range = (e - s);
	    double range_scale = range / (double) total_des;
	    
	    if(range > .0)
	    {
		double pos = s;
		
		for(int ci=0; ci < chld.size(); ci++)
		{
		    double child_range = range_scale * (double) chld_des[ci];
		    
		    ExprData.Cluster ch = (ExprData.Cluster) chld.elementAt(ci);
		    
		    colourClusterByBreadth(ch, pos, (pos + child_range)-range_scale);
		    
		    pos += child_range;
		}
	    }
	}
    }

    private Color getBlendHSB(double dd)
    {
	float d = (float) dd;
	float[] from = Color.RGBtoHSB(new_from.getRed(), new_from.getGreen(), new_from.getBlue(), null);
	float[] to   = Color.RGBtoHSB(new_to.getRed(), new_to.getGreen(), new_to.getBlue(), null);
	
	return Color.getHSBColor((((1.0f-d) * from[0]) + (d * to[0])),
				 (((1.0f-d) * from[1]) + (d * to[1])),
				 (((1.0f-d) * from[2]) + (d * to[2])));
    }

    private Color getBlendRGB(double d)
    {
	double from_d_r = (double)(new_from.getRed());
	double from_d_g = (double)(new_from.getGreen());
	double from_d_b = (double)(new_from.getBlue());
	
	double to_d_r = (double)(new_to.getRed());
	double to_d_g = (double)(new_to.getGreen());
	double to_d_b = (double)(new_to.getBlue());
	
	return new Color((int)(((1.0-d) * from_d_r) + (d * to_d_r)),
			 (int)(((1.0-d) * from_d_g) + (d * to_d_g)),
			 (int)(((1.0-d) * from_d_b) + (d * to_d_b)));
    }

    private int getDescendentCount(ExprData.Cluster cl)
    {
	int count = 1;
	
	Vector chld = cl.getChildren();
	if(chld != null)
	{
	    for(int ci=0; ci < chld.size(); ci++)
	    {
		count += getDescendentCount( (ExprData.Cluster) chld.elementAt(ci) );
	    }
	}

	return count;
    }


    //    private final int ramp_steps = 20;

    private Color new_to, new_from;

    private JButton solid_col_jb, current_jb, from_jb, to_jb;
    private JCheckBox children_jchkb;
    private JRadioButton depth_jrb, breadth_jrb, blend_rgb_jrb, blend_hsb_jrb, solid_jrb;
    private JPanel solid_panel, blend_panel;

    private ExprData.Cluster cluster;

    private ExprData edata;
    private maxdView mview;

	//private RampViewer current_rv;
	//private RampViewer new_choice_rv;

}
