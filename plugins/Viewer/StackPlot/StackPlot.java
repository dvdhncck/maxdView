import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import javax.swing.event.*;
import java.awt.print.*;

//
// distribute genes into a 2d array of bins based on the values in
//   two user controlled sets
//

public class StackPlot implements ExprData.ExprDataObserver, Plugin
{
    public StackPlot(maxdView mview_)
    {
	mview = mview_;

	//System.out.println("++ a new StackPlot is alive, mview is "  + mview);
    }

    public void initialise()
    {
	frame = new JFrame("Stack Plot");

	mview.decorateFrame(frame);

	frame.addWindowListener(new WindowAdapter() 
			  {
			      public void windowClosing(WindowEvent e)
			      {
				  cleanUp();
			      }
			  });

	edata = mview.getExprData();
	dplot = mview.getDataPlot();

	nt_sel = edata.new NameTagSelection();

	height_scale = 50;

	//System.out.println("++ edata.getNumMeasurements is "  + edata.getNumMeasurements());
	//System.out.println("++ dplot.getFontSize is " + dplot.getFontSize());

	int first_good_m = -1;
	int first_good_mi = 0;

	if(edata.getNumMeasurements() > 0)
	{
	    do
	    {
		first_good_m++;
		first_good_mi = edata.getMeasurementAtIndex(first_good_m);
	    } while((first_good_m < edata.getNumMeasurements()) && 
		    (edata.getMeasurementDataType(first_good_mi) == ExprData.UnknownDataType));
	}
	
	if((first_good_m >= 0) && (first_good_m < edata.getNumMeasurements()))
	    x_meas = first_good_mi;
	else
	    x_meas = -1;

	y_meas = x_meas;

	x_box_mode = EquallySpaced;
	y_box_mode = EquallySpaced;

	n_x_boxes = 8;
	n_y_boxes = 8;

	box_width = 10;
	box_height = 10;

	setGridLayout(GRID_SINGLE);
    }

    private void buildGUI()
    {
	background_col = mview.getBackgroundColour();
	text_col       = mview.getTextColour();

	frame.getContentPane().setLayout(new BorderLayout());

	JToolBar tool_bar = new JToolBar();
	tool_bar.setFloatable(false);
	addTopToolbarStuff(tool_bar);
	frame.getContentPane().add(tool_bar, BorderLayout.NORTH);

	panel = new StackDrawPanel();
	frame.getContentPane().add(panel);

	panel.setDropAction(new DragAndDropPanel.DropAction()
	    {
		public void dropped(DragAndDropEntity dnde)
		{
		    try
		    {
			int sid = dnde.getSpotId();
			System.out.println("spot dropped");

			panel.highlightSpot(sid);
		    }
		    catch(DragAndDropEntity.WrongEntityException wee2)
		    {
		    }
		}
	    });
	

	// ===== axis =====================================================================================

	axis_man = new AxisManager(mview);

	axis_man.addAxesListener( new AxisManager.AxesListener()
	    {
		public void axesChanged() 
		{
		    panel.repaintDisplay();
		}
	    });

	axis_man.addAxis(new PlotAxis(mview, "X"));
	axis_man.addAxis(new PlotAxis(mview, "Y"));

	// ===== decorations ===============================================================================

	deco_man = new DecorationManager(mview, "ScatterPlot");

	deco_man.addDecoListener( new DecorationManager.DecoListener()
	    {
		public void decosChanged() 
		{
		    panel.repaintDisplay();
		}
	    });

 
	// =================================================================================================

	JToolBar bottom_tool_bar = new JToolBar();
	bottom_tool_bar.setFloatable(false);
	addBottomToolbarStuff(bottom_tool_bar);

	JToolBar side_bar = new JToolBar();
	side_bar.setFloatable(false);
	frame.getContentPane().add(side_bar, BorderLayout.WEST);
	{
	    JSlider height_scale_slider = new JSlider(JSlider.VERTICAL, 0, 100, 1);
	    height_scale_slider.setPreferredSize(new Dimension(20, 100));
	    height_scale_slider.setValue(height_scale);
	    side_bar.add(height_scale_slider);

	    height_scale_slider.addChangeListener(new ChangeListener()
						  {
						      public void stateChanged(ChangeEvent e) 
						      {
							  JSlider source = (JSlider)e.getSource();
							  //if (!source.getValueIsAdjusting()) 
							  {
							      height_scale = source.getValue();
							      bg_image = null;
							      panel.repaint();
							  }
						      }
						  });
	    
	}

	frame.getContentPane().add(bottom_tool_bar, BorderLayout.SOUTH);
	panel.setPreferredSize(new Dimension(300, 300));

	Timer timer = new Timer(750, 
				new ActionListener() 
				{
				    public void actionPerformed(ActionEvent evt) 
				    {
					if(filter_alert == true) 
					{ 
					    if(edata.filterIsOn() && use_filter)
					    {
						panel.drawFilterAlert(true);
						filter_alert = false;
					    }
					}
					else
					{
					    panel.drawFilterAlert(false);
					    filter_alert = true;
					}
					
					if(auto_levelize == true)
					{
					    levelize();
					    
					}
				    }    
				});
	timer.start();

	//System.out.println("initialised ok, dplot is " + dplot + " edata is "  + edata);

	
    }

    public void cleanUp()
    {
	box_hits = null;
	bin_contents = null;
	frame.setVisible(false);
	edata.removeObserver(this);
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
	initialise();
	buildGUI();
	resetView();
	setBoxLayout();
	setBoundaryValues();
	frame.pack();
	frame.setVisible(true);

	// register ourselves with the data
	//
	edata.addObserver(this);
    }

    public void stopPlugin()
    {
	cleanUp();
    }

    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("Stack Plot", "viewer", 
					 "Shows the corelation between two or more Measurements", "", 1, 2, 0);
	return pinf;
    }

    public void   runCommand(String name, String[] args, CommandSignal done) 
    { 
	if(done != null)
	    done.signal();
    } 

    public PluginCommand[] getPluginCommands()
    {
	return null;
    }
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  toolbar controls
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private void buildMeasurementNameList(JComboBox jcb, ActionListener al)
    {
	// save the current selection if there is one

	String meas_name = (String) jcb.getSelectedItem();

	//System.out.println("current x set is " + x_meas + ", name is "  + x_meas_name);
	//System.out.println("        y set is " + y_meas + ", name is "  + y_meas_name);


	// temporarily disable the action listeners
	if(al != null)
	    jcb.removeActionListener(al);

	jcb.removeAllItems();

	for(int m=0;m<edata.getNumMeasurements();m++)
	{
	    int mi = edata.getMeasurementAtIndex(m);
	    if(edata.getMeasurementShow(mi) == true)
	    {
		jcb.addItem(edata.getMeasurementName(mi));
	    }
	}

	if(meas_name != null)
	{
	    int meas = edata.getMeasurementFromName(meas_name);
	    if(meas < 0)
	    {
		// this one no longer exists...
		meas_name =  edata.getMeasurementName(meas);
		jcb.setSelectedItem(meas_name);
	    }
	}
	else
	{
	    jcb.setSelectedItem(edata.getMeasurementName(0));
	}

	//System.out.println("new x set is " + x_meas + ", name is "  + x_meas_name);
	//System.out.println("    y set is " + y_meas + ", name is "  + y_meas_name);

	// re-enable the action listener
	if(al != null)
	    jcb.addActionListener(al);
    }

    protected void buildMeasurementNameLists()
    {
	buildMeasurementNameList(x_meas_jcb, x_meas_al);
	buildMeasurementNameList(y_meas_jcb, y_meas_al);

	/*
	  // save the current selection if there is one

	String x_meas_name = (String) x_meas_jcb.getSelectedItem();
	String y_meas_name = (String) y_meas_jcb.getSelectedItem();

	//System.out.println("current x set is " + x_meas + ", name is "  + x_meas_name);
	//System.out.println("        y set is " + y_meas + ", name is "  + y_meas_name);


	// temporarily disable the action listeners
	x_meas_jcb.removeActionListener(x_meas_al);
	y_meas_jcb.removeActionListener(y_meas_al);

	x_meas_jcb.removeAllItems();
	y_meas_jcb.removeAllItems();

	for(int s=0;s<edata.getNumMeasurements();s++)
	{
	    if(edata.getMeasurementShow(s) == true)
	    {
		x_meas_jcb.addItem(edata.getMeasurementName(s));
		y_meas_jcb.addItem(edata.getMeasurementName(s));
	    }
	}

	if(x_meas_name != null)
	{
	    x_meas = edata.getMeasurementFromName(x_meas_name);
	    if(x_meas < 0)
	    {
		// this set no longer exists...
		x_meas = 0;
		x_meas_name =  edata.getMeasurementName(x_meas);
	    }
	    x_meas_jcb.setSelectedItem(x_meas_name);

	}
	else
	{
	    x_meas = 0;
	    x_meas_jcb.setSelectedItem(edata.getMeasurementName(x_meas));
	}

	if(y_meas_name != null)
	{
	    y_meas = edata.getMeasurementFromName(y_meas_name);
	    if(y_meas < 0)
	    {
		// this set no longer exists...
		y_meas = 0;
		y_meas_name =  edata.getMeasurementName(y_meas);
	    }
	    y_meas_jcb.setSelectedItem(y_meas_name);
	}
	else
	{
	    y_meas = 0;
	    y_meas_jcb.setSelectedItem(edata.getMeasurementName(y_meas));
	}

	//System.out.println("new x set is " + x_meas + ", name is "  + x_meas_name);
	//System.out.println("    y set is " + y_meas + ", name is "  + y_meas_name);


	// re-enable the action listeners
	x_meas_jcb.addActionListener(x_meas_al);
	y_meas_jcb.addActionListener(y_meas_al);
	*/

    }


    protected void addBottomToolbarStuff(JToolBar tool_bar) 
    {
	GridBagLayout gridbag = new GridBagLayout();
	tool_bar.setLayout(gridbag);

	{
	    back_jb = new JButton("Back");
	    tool_bar.add(back_jb);
	    back_jb.setEnabled(false);
	    
	    back_jb.addActionListener(new ActionListener() 
				      {
					  public void actionPerformed(ActionEvent e) 
					  {
					      goBack();
					      setBoundaryValues();
					      panel.repaint();
					  }
				      });
	    GridBagConstraints c = new GridBagConstraints();
	    //c.anchor = GridBagConstraints.CENTER;
	    //c.fill = GridBagConstraints.BOTH;
	    c.anchor = GridBagConstraints.WEST;
	    c.gridx = 0;
	    c.gridy = 0;
	    //c.weightx = c.weighty = 1.0;
	    gridbag.setConstraints(back_jb, c);
	}

	{
	    final JButton jb = new JButton("Reset");
	    tool_bar.add(jb);
	    jb.addActionListener(new ActionListener() 
				 {
				     public void actionPerformed(ActionEvent e) 
				     {
					 resetView();
					 setBoundaryValues();
					 panel.repaint();
				     }
				 });

	    GridBagConstraints c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.gridx = 1;
	    c.gridy = 0;
	    //c.weightx = c.weighty = 1.0;
	    gridbag.setConstraints(jb, c);
	}
	
	{
	    status_label = new JLabel("Mmmmm, pixels.");
	    tool_bar.add(status_label);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 3;
	    c.gridy = 0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.weightx = c.weighty = 1.0;
	    gridbag.setConstraints(status_label, c);

	}
	
	{
	    JButton jb = new JButton("Print");
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 4;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(jb, c);
	    tool_bar.add(jb);
	    
	    jb.addActionListener(new ActionListener()
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			new PrintManager( mview, panel, panel ).openPrintDialog();
		    }
		});
	}

	{
	    final JButton jb = new JButton("Help");
	    tool_bar.add(jb);
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			mview.getPluginHelpTopic("StackPlot", "StackPlot");
		    }
		});
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 5;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.EAST;
	    //c.weightx = c.weighty = 1.0;
	    gridbag.setConstraints(jb, c);
	}
	{
	    final JButton jb = new JButton("Close");
	    tool_bar.add(jb);
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			cleanUp();
		    }
		});
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 6;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.EAST;
	    //c.weightx = c.weighty = 1.0;
	    gridbag.setConstraints(jb, c);
	}
	
    }
    
    protected void addTopToolbarStuff(JToolBar tool_bar) 
    {
	String[] mode_names = { "Regular", "Equalised" };
	
	String[] bin_names = { "5", "8", "10", "12", "15", "20" };

	GridBagLayout gridbag = new GridBagLayout();
	tool_bar.setLayout(gridbag);

	{
	    JPanel wrapper = new JPanel();
	    wrapper.setBorder(BorderFactory.createEtchedBorder());
	    GridBagLayout igridbag = new GridBagLayout();
	    wrapper.setLayout(igridbag);

	    
	    JLabel label = new JLabel("Bins");
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = 0;
	    igridbag.setConstraints(label, c);
	    wrapper.add(label);

	    label = new JLabel(" Mode");
	    c = new GridBagConstraints();
	    c.gridx = 3;
	    c.gridy = 0;
	    igridbag.setConstraints(label, c);
	    wrapper.add(label);

	    label = new JLabel("x ");

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    igridbag.setConstraints(label, c);
	    wrapper.add(label);

	    x_meas_jcb = new JComboBox();
	    x_meas_al  = new MeasurementNameListener(true);
	   
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 1;
	    igridbag.setConstraints(x_meas_jcb, c);
	    wrapper.add(x_meas_jcb);
	    
	    n_x_boxes_jcb = new JComboBox(bin_names);
	    n_x_boxes_jcb.addActionListener(new BinNameListener(true));
	    n_x_boxes_jcb.setSelectedIndex(1);
	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = 1;
	    igridbag.setConstraints(n_x_boxes_jcb, c);
	    wrapper.add(n_x_boxes_jcb);
    
	    x_box_mode_jcb = new JComboBox(mode_names);
	    x_box_mode_jcb.addActionListener(new ModeNameListener(true));
	    x_box_mode_jcb.setSelectedIndex(0);
	    c = new GridBagConstraints();
	    c.gridx = 3;
	    c.gridy = 1;
	    igridbag.setConstraints(x_box_mode_jcb, c);
	    wrapper.add(x_box_mode_jcb);
	    
	    label = new JLabel("y ");
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 2;
	    igridbag.setConstraints(label, c);
	    wrapper.add(label);
    
	    y_meas_jcb = new JComboBox();
	    y_meas_al  = new MeasurementNameListener(false);
    
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 2;
	    igridbag.setConstraints(y_meas_jcb, c);
	    wrapper.add(y_meas_jcb);
    
	    n_y_boxes_jcb = new JComboBox(bin_names);
	    n_y_boxes_jcb.addActionListener(new BinNameListener(false));
	    n_y_boxes_jcb.setSelectedIndex(1);
	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = 2;
	    igridbag.setConstraints(n_y_boxes_jcb, c);
	    wrapper.add(n_y_boxes_jcb);
    
	    y_box_mode_jcb = new JComboBox(mode_names);
	    y_box_mode_jcb.addActionListener(new ModeNameListener(false));
	    y_box_mode_jcb.setSelectedIndex(0);
	    c = new GridBagConstraints();
	    c.gridx = 3;
	    c.gridy = 2;
	    igridbag.setConstraints(y_box_mode_jcb, c);
	    wrapper.add(y_box_mode_jcb);
   
	    // ---------

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.fill = GridBagConstraints.BOTH;
	    //c.anchor = GridBagConstraints.EAST;
	    //c.weightx = c.weighty = 1.0;
	    gridbag.setConstraints(wrapper, c);
	    tool_bar.add(wrapper);
    
	}

	{
	    JPanel wrapper = new JPanel();
	    wrapper.setBorder(BorderFactory.createEtchedBorder());

	    GridBagLayout bgridbag = new GridBagLayout();
	    wrapper.setLayout(bgridbag);

	    JLabel label = new JLabel(" Labels ");
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.gridwidth = 2;
	    bgridbag.setConstraints(label, c);
	    wrapper.add(label);
	    
	    {
		final NameTagSelector nts = new NameTagSelector(mview);
		nts.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    nt_sel = nts.getNameTagSelection();
			    updateDisplay();
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 2;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		bgridbag.setConstraints(nts, c);
		wrapper.add(nts);
	    }

	    final JButton cjb = new JButton("Clear all");
		
	    Font f = cjb.getFont();
	    Font small_font = new Font(f.getName(), f.getStyle(), f.getSize() - 2);
	    
	    {
		cjb.setFont(small_font);
		cjb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			}
		    });
		 c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.5;
		//c.gridwidth = 2;
		c.anchor = GridBagConstraints.WEST;
		bgridbag.setConstraints(cjb, c);
		wrapper.add(cjb);
	    }
	    {
		final JButton jb = new JButton("Label all");
		jb.setFont(small_font);
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			}
		    });
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.5;
		//c.gridwidth = 2;
		c.anchor = GridBagConstraints.WEST;
		bgridbag.setConstraints(jb, c);
		wrapper.add(jb);
	    }

	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    c.fill = GridBagConstraints.BOTH;
	    //c.anchor = GridBagConstraints.EAST;
	    //c.weightx = c.weighty = 1.0;
	    gridbag.setConstraints(wrapper, c);
	    tool_bar.add(wrapper);
	}

	{
	    JPanel wrapper = new JPanel();
	    wrapper.setBorder(BorderFactory.createEtchedBorder());

	    GridBagLayout bgridbag = new GridBagLayout();
	    wrapper.setLayout(bgridbag);

	    JLabel label = new JLabel(" Colours ");
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    bgridbag.setConstraints(label, c);
	    wrapper.add(label);
	   
	    {
		JButton jb = new JButton("Layout");
	   
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		bgridbag.setConstraints(jb, c);
		wrapper.add(jb);
		
		jb.addActionListener(new ActionListener()
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    openLayoutEditor();
			}
		    });
	    }
	    {
		JButton jb = new JButton("Cycle");
	   
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		bgridbag.setConstraints(jb, c);
		wrapper.add(jb);
		
		jb.addActionListener(new ActionListener()
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    // cycleNextLayout();
			}
		    });
	    }

	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = 0;
	    c.fill = GridBagConstraints.BOTH;
	    //c.anchor = GridBagConstraints.EAST;
	    //c.weightx = c.weighty = 1.0;
	    gridbag.setConstraints(wrapper, c);
	    tool_bar.add(wrapper);
	}

	{
	    JPanel wrapper = new JPanel();
	    wrapper.setBorder(BorderFactory.createEtchedBorder());

	    GridBagLayout bgridbag = new GridBagLayout();
	    wrapper.setLayout(bgridbag);

	    {
		JButton jb = new JButton("Axes");
	   
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		bgridbag.setConstraints(jb, c);
		wrapper.add(jb);
		
		jb.addActionListener(new ActionListener()
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    axis_man.startEditor();
			}
		    });
	    }
	    {
		JButton jb = new JButton("Decs");
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		bgridbag.setConstraints(jb, c);
		wrapper.add(jb);
		
		jb.addActionListener(new ActionListener()
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    deco_man.startEditor();
			}
		    });
	    }
	    /*
	    {
		JButton jb = new JButton("Print");
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 2;
		bgridbag.setConstraints(jb, c);
		wrapper.add(jb);
		
		jb.addActionListener(new ActionListener()
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    new PrintManager( mview, panel, panel ).openPrintDialog();
			}
		    });
	    }
	    */
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 3;
	    c.gridy = 0;
	    c.fill = GridBagConstraints.BOTH;
	    //c.anchor = GridBagConstraints.EAST;
	    //c.weightx = c.weighty = 1.0;
	    gridbag.setConstraints(wrapper, c);
	    tool_bar.add(wrapper);
	}

	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrapper.setLayout(w_gridbag);
	    wrapper.setBorder(BorderFactory.createEtchedBorder());

	    {
		JCheckBox jcb= new JCheckBox("Apply filter");
		wrapper.add(jcb);
		jcb.setSelected(use_filter);
		//jcb.setHorizontalTextPosition(AbstractButton.LEFT);
		jcb.addActionListener(new ActionListener()
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    JCheckBox source = (JCheckBox) e.getSource();
			    use_filter = source.isSelected();
			    // System.out.println("filter is " + (use_filter ? "on" : "off"));
			    binGenes();
			    panel.repaint();
			}
		    });
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = c.weighty = 1.0;
		w_gridbag.setConstraints(jcb, c);
	    }
	    {
		JCheckBox jcb= new JCheckBox("Show clusters...");
		wrapper.add(jcb);
		jcb.setSelected(show_clusters);
		//jcb.setHorizontalTextPosition(AbstractButton.LEFT);
		jcb.addActionListener(new ActionListener()
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    JCheckBox source = (JCheckBox) e.getSource();
			    show_clusters = source.isSelected();
			    // System.out.println("filter is " + (use_filter ? "on" : "off"));
			    //binGenes();
			    panel.repaint();
			}
		    });
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = c.weighty = 1.0;
		w_gridbag.setConstraints(jcb, c);
	    }
	    {
		JCheckBox jcb= new JCheckBox("...and edges");
		wrapper.add(jcb);
		jcb.setSelected(show_edges);
		//jcb.setHorizontalTextPosition(AbstractButton.LEFT);
		jcb.addActionListener(new ActionListener()
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    JCheckBox source = (JCheckBox) e.getSource();
			    show_edges = source.isSelected();
			    // System.out.println("filter is " + (use_filter ? "on" : "off"));
			    //binGenes();
			    panel.repaint();
			}
		    });
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 2;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = c.weighty = 1.0;
		w_gridbag.setConstraints(jcb, c);
	    }
	    {
		JCheckBox jcb= new JCheckBox("Show axes");
		wrapper.add(jcb);
		//jcb.setSelected(auto_reset);
		//jcb.setHorizontalTextPosition(AbstractButton.LEFT);
		jcb.addActionListener(new ActionListener()
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    JCheckBox source = (JCheckBox) e.getSource();
			    show_axes = source.isSelected();
			    updateDisplay();
			    
			}
		    });
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		//c.anchor = GridBagConstraints.EAST;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = c.weighty = 1.0;
		w_gridbag.setConstraints(jcb, c);
	    }
	    {
		JCheckBox jcb= new JCheckBox("Auto reset view");
		wrapper.add(jcb);
		jcb.setSelected(auto_reset);
		//jcb.setHorizontalTextPosition(AbstractButton.LEFT);
		jcb.addActionListener(new ActionListener()
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    JCheckBox source = (JCheckBox) e.getSource();
			    auto_reset = source.isSelected();
			    if(auto_reset)
			    {
				setBoundaryValues();
				panel.repaint();
			    }
			}
		    });
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 1;
		//c.anchor = GridBagConstraints.EAST;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = c.weighty = 1.0;
		w_gridbag.setConstraints(jcb, c);
	    }
	    {
		al_jchkb = new JCheckBox("Auto levelize");
		wrapper.add(al_jchkb);
		al_jchkb.setEnabled(false);
		al_jchkb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    JCheckBox source = (JCheckBox) e.getSource();
			    auto_levelize = source.isSelected();
			}
		    });
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 1;
		c.gridy = 2;
		//c.weightx = c.weighty = 1.0;
		w_gridbag.setConstraints(al_jchkb, c);
	    }

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 4;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.EAST;
	    c.fill = GridBagConstraints.BOTH;
	    //c.weightx = c.weighty = 1.0;
	    gridbag.setConstraints(wrapper, c);
	    tool_bar.add(wrapper);
	}


	
	buildMeasurementNameLists();
    }
    
    // handles the Measurement choice combo boxen
    class MeasurementNameListener implements ActionListener
    { 
	public MeasurementNameListener(boolean is_x_) 
	{ 
	    super();
	    is_x = is_x_;
	}

	public void actionPerformed(ActionEvent e) 
	{
	    JComboBox cb = (JComboBox)e.getSource();
	    String new_name = (String)cb.getSelectedItem();
	    int new_set_i = edata.getMeasurementFromName(new_name);

	    // System.out.println("set name "  + new_name + " is index " + new_set_i);

	    if(auto_reset)
	    {
		resetView();
		x_saved_traversal = null;
		y_saved_traversal = null;
	    }

	    if(is_x)
	    {
		x_meas = new_set_i;
		
		setBoundaryValues();
		if(x_box_mode == EquallyFilled)
		    computeHistograms(true);
	    }
	    else
	    {
		y_meas = new_set_i;
		
		setBoundaryValues();
		if(y_box_mode == EquallyFilled)
		    computeHistograms(false);
	    }
	    
	    // System.out.println("x set is " + x_meas + ", y_meas is " + y_meas);
 
	    if(panel != null)
		panel.repaint();

	}

	private boolean is_x;
    }

    // handles the Bin count combo boxen
    //
    class BinNameListener implements ActionListener
    { 
	public BinNameListener(boolean is_x_) 
	{ 
	    super();
	    is_x = is_x_;
	}

	public void actionPerformed(ActionEvent e) 
	{
	    JComboBox cb = (JComboBox)e.getSource();
	    int new_val = cb.getSelectedIndex();
	    if(is_x)
	    {
		n_x_boxes = bin_sizes[new_val];
	    }
	    else
	    {
		n_y_boxes = bin_sizes[new_val];
	    }

	    setBoxLayout();    // reallocate the bin_hits matrix for the new size
	    setBoundaryValues();

	    if(panel != null)
		panel.repaint();
	}

	private boolean is_x;
    }
    // handles the Mode choice combo boxen
    class ModeNameListener implements ActionListener
    { 
	public ModeNameListener(boolean is_x_) 
	{ 
	    super();
	    is_x = is_x_;
	}

	public void actionPerformed(ActionEvent e) 
	{
	    JComboBox cb = (JComboBox)e.getSource();
	    int new_val = cb.getSelectedIndex();
	    if(is_x)
	    {
		x_box_mode = new_val;
		if(x_box_mode == EquallyFilled)
		    computeHistograms(true);
	    }
	    else
	    {
		y_box_mode = new_val;
		if(y_box_mode == EquallyFilled)
		    computeHistograms(false);
	    }
	    
	    if(al_jchkb != null)
		al_jchkb.setEnabled((x_box_mode == EquallyFilled) && (y_box_mode == EquallyFilled));
		
	    setBoundaryValues();
	    if(panel != null)
		panel.repaint();
	}

	private boolean is_x;
    }
  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  layout editor
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    // force a repaint
    private void updateDisplay()
    {
	bg_image = null;
	panel.repaint();
    }

    private void updateLayoutEditor()
    {
	cols_panel.removeAll();
	
	int r = 0;
	int c = 0;
	
	GridBagLayout gridbag = new GridBagLayout();
	cols_panel.setLayout(gridbag);

	cols_jcb = new JComboBox[ grid_col.length ];

	String[] m_names = new String[ edata.getNumMeasurements() ];
	for(int m=0; m < edata.getNumMeasurements(); m++)
	    m_names[m] = edata.getMeasurementName( edata.getMeasurementAtIndex(m) );

	
	for(int m=0; m < grid_col.length; m++)
	{
	    cols_jcb[m] = new JComboBox(m_names);
   
	    cols_jcb[m].setSelectedItem( edata.getMeasurementName( grid_col[m] ));;

	    GridBagConstraints con = new GridBagConstraints();
	    con.gridx = c;
	    con.gridy = r;
	    gridbag.setConstraints(cols_jcb[m], con);
	    cols_panel.add(cols_jcb[m]);

	    if(++c == grid_c)
	    {
		c = 0;
		    r++;
	    }
	}

	cols_panel.updateUI();
    }

    private JPanel cols_panel;
    private JComboBox box_size_jcb = null;
    private JComboBox[] cols_jcb;
    
    class CustomLayoutListener implements ActionListener
    {
	private int id;

	public CustomLayoutListener(int id_)
	{
	    id = id_;
	}
	public void actionPerformed(ActionEvent e) 
	{
	    
	}
    }

	

    private void openLayoutEditor()
    {
	final JFrame frame = new JFrame("Stack Plot Layout");
	
	mview.decorateFrame(frame);

	frame.getContentPane().setLayout(new BorderLayout());

	JPanel panel = new JPanel();
	panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
	GridBagLayout gridbag = new GridBagLayout();
	panel.setLayout(gridbag);

	GridBagConstraints c = null;
	int line = 0;
	
	ButtonGroup bg = new ButtonGroup();


	{
	    JPanel wrapper = new JPanel();
	    wrapper.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrapper.setLayout(w_gridbag);
    
	    {
		JLabel label = new JLabel("Mode ");
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0;
		c.gridy = 0;
		w_gridbag.setConstraints(label, c);
		wrapper.add(label);
		
		
		box_size_jcb = new JComboBox(grid_mode_names);
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 0;
		w_gridbag.setConstraints(box_size_jcb, c);
		wrapper.add(box_size_jcb);
		
		
		box_size_jcb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    setGridLayout(  box_size_jcb.getSelectedIndex() );
			    updateLayoutEditor();
			}
		    });

		
		cols_panel = new JPanel();
		cols_panel.setPreferredSize(new Dimension(350, 200));
	
		JScrollPane jsp = new JScrollPane(cols_panel);
		jsp.setBorder(BorderFactory.createEmptyBorder(10,0,0,0));
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 0;
		c.gridwidth = 2;
		c.gridy = 1;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		w_gridbag.setConstraints(jsp, c);
		wrapper.add(jsp);
	    }

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    c.weightx = c.weighty = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(wrapper, c);
	    panel.add(wrapper);
	}

	{
	    JPanel inner_panel = new JPanel();
	    //GridBagLayout inner_gridbag = new GridBagLayout();
	    //inner_panel.setLayout(inner_gridbag);
	    inner_panel.setBorder(BorderFactory.createEmptyBorder(10,0,0,0));

	    {
		final JButton jb = new JButton("Close");
		inner_panel.add(jb);
		jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					 {
					     frame.setVisible(false);
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
					     // setBoxLayout(  box_size_jcb.getSelectedIndex() );

					     for(int g=0; g < cols_jcb.length; g++)
					     {
						 //int mi = cols_jcb[g].getSelectedIndex();
						 //int m = edata.getIndexOfMeasurement(mi);

						 String mn = (String) cols_jcb[g].getSelectedItem();
						 int m = edata.getMeasurementFromName(mn);
						 grid_col[g] = m;
					     }

					     // frame.setVisible(false);
					     updateDisplay();
					 }
				     });
	    }

	    {
		final JButton jb = new JButton("Help");
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    mview.getPluginHelpTopic("StackPlot", "StackPlot", "#layout");
			}
		    });
		inner_panel.add(jb);
	    }
	    
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(inner_panel, c);
	    panel.add(inner_panel);
	}

	updateLayoutEditor();

	frame.getContentPane().add(panel);
	frame.pack();
	frame.setVisible(true);
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
	case ExprData.RangeChanged:
	    if(use_filter)
	    {
		//y_saved_traversal = null;
		//x_saved_traversal = null;
		//setBoundaryValues();
		binGenes();
	    }
	    panel.repaint();
	    break;
	case ExprData.SizeChanged:
	case ExprData.ValuesChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	    int nmeas =  edata.getNumMeasurements();
	    if(x_meas >= nmeas)
		x_meas = nmeas - 1;
	    if(y_meas >= nmeas)
		y_meas = nmeas - 1;
	    y_saved_traversal = null;
	    x_saved_traversal = null;
	    //setBoundaryValues();
	    binGenes();
	    panel.repaint();
	    break;
	case ExprData.VisibilityChanged:
	    binGenes();
	    panel.repaint();
	    break;
	case ExprData.OrderChanged:
	    // do nothing, it was probably us doing a sort anyhow...
	    break;
	}
    }

    public void clusterUpdate(ExprData.ClusterUpdateEvent cue)
    {
	switch(cue.event)
	{
	case ExprData.ColourChanged:
	case ExprData.OrderChanged:
	case ExprData.VisibilityChanged:
	case ExprData.SizeChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	    panel.repaint();
	    break;
	}
    }

    public void measurementUpdate(ExprData.MeasurementUpdateEvent mue)
    {
	switch(mue.event)
	{
	case ExprData.VisibilityChanged:
	case ExprData.SizeChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:

	    int nmeas =  edata.getNumMeasurements();
	    if(x_meas >= nmeas)
		x_meas = nmeas - 1;
	    if(y_meas >= nmeas)
		y_meas = nmeas - 1;
	    x_saved_traversal = y_saved_traversal = null;
	    buildMeasurementNameLists();
	    panel.repaint();
	    break;
	case ExprData.NameChanged:
	    x_saved_traversal = y_saved_traversal = null;
	    buildMeasurementNameLists();
	    break;
	case ExprData.OrderChanged:
	    initialise();
	    x_saved_traversal = y_saved_traversal = null;
	    buildMeasurementNameLists();
	    binGenes();
	    panel.repaint();
	    break;
	case ExprData.ValuesChanged:
	    // indicates a change of data_type
	    //
	    panel.repaint();
	}	
    }
    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
	background_col = mview.getBackgroundColour();
	text_col       = mview.getTextColour();
	
	panel.repaint();
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  layout
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    // allocates space for counters and other things which depend
    // on the numbers of bins in x and y
    //
    public void setBoxLayout()
    {
	if((x_meas < 0) || (y_meas < 0))
	    return;

	box_hits     = new int[n_x_boxes][];
	bin_contents = new Vector[n_x_boxes][];

	for(int i=0;i<n_x_boxes;i++)
	{
	    box_hits[i] = new int[n_y_boxes];
	    bin_contents[i] = new Vector[n_y_boxes];

	    for(int j=0;j < n_y_boxes; j++)
		bin_contents[i][j] = new Vector();
	}
	
	// System.out.println("hit matrix is " + n_x_boxes + "x" + n_y_boxes + " boxes");

    }

    public void resetView()
    {
	x_meas_min = y_meas_min = -(Double.MAX_VALUE);
	x_meas_max = y_meas_max = Double.MAX_VALUE;
    }

    public void goBack()
    {
	if(history.size() > 0)
	{
	    Range range = (Range) history.elementAt(0);
	    history.removeElementAt(0);
	    x_meas_min = range.x_meas_min;
	    y_meas_min = range.y_meas_min;
	    x_meas_max = range.x_meas_max;
	    y_meas_max = range.y_meas_max;
	}
	if(history.size() == 0)
	{
	    back_jb.setEnabled(false);
	    //fore_jb.setEnabled(false);
	}
    }

    public void setBoundaryValues()
    {
	if((x_meas < 0) || (x_meas < 0))
	    return; 

	if(x_meas_min < edata.getMeasurementMinEValue(x_meas))
	    x_meas_min = edata.getMeasurementMinEValue(x_meas);
	if(y_meas_min < edata.getMeasurementMinEValue(y_meas))
	    y_meas_min = edata.getMeasurementMinEValue(y_meas);
	
	if(x_meas_max > edata.getMeasurementMaxEValue(x_meas))
	    x_meas_max = edata.getMeasurementMaxEValue(x_meas);
	if(y_meas_max > edata.getMeasurementMaxEValue(y_meas))
	    y_meas_max = edata.getMeasurementMaxEValue(y_meas);

	/*
	System.out.println("X set, " + n_x_boxes + " boxes, range: " + 
			   String.valueOf(edata.getMeasurementMinEValue(x_meas)) + " -> " +
			   String.valueOf(edata.getMeasurementMaxEValue(x_meas)));
	System.out.println("Y set " + n_y_boxes + "boxes, range: " + 
			   String.valueOf(edata.getMeasurementMinEValue(y_meas)) + " -> " + 
			   String.valueOf(edata.getMeasurementMaxEValue(y_meas)));
	*/

	if(x_box_mode == EquallyFilled)
	    computeHistograms(true);
	if(y_box_mode == EquallyFilled)
	    computeHistograms(false);

	bin_x_e_gap = (x_meas_max - x_meas_min) / (double) (n_x_boxes-1);
	bin_y_e_gap = (y_meas_max - y_meas_min) / (double) (n_y_boxes-1);
	
	binGenes();
    }

    // generate a histogram for the set currently begin displayed 
    // along the given dimension. use the same number of bins as are
    // used for display
    public void computeHistograms(boolean is_x)
    {

	int set = is_x ? x_meas : y_meas;

	if(set < 0)
	    return; 

	int n_bins = is_x ? n_x_boxes : n_y_boxes;

	double[] boundary = null;

	double bin_gap = (edata.getMeasurementMaxEValue(set)-edata.getMeasurementMinEValue(set)) / (double) (n_bins-1);

	boundary = new double[n_bins+1];
	
	//System.out.println("computing " + (n_bins+1) + " box boundaries for "  + (is_x ? "X" : "Y") );

	// sort the values into order, then choose boundaries
	// to equally partition the set of visible values
	//
	boolean traversal_known = false;
	int[] local_traversal = null;

	if(is_x)
	{
	    if((x_saved_traversal != null) && (x_meas == x_saved_traversal_meas))
	    {
		traversal_known = true;
		local_traversal =  x_saved_traversal;
	    }
	}
	else
	{
	    if((y_saved_traversal != null) && (y_meas == y_saved_traversal_meas))
	    {
		traversal_known = true;
		local_traversal =  y_saved_traversal;
	    }
	}
	
	if(traversal_known == false)
	{
	    local_traversal = edata.getTraversal(set, 0, null);

	    if(is_x)
	    {
		x_saved_traversal = local_traversal;
		x_saved_traversal_meas = x_meas;
	    }
	    else
	    {
		y_saved_traversal = local_traversal;
		y_saved_traversal_meas = y_meas;
	    }
	}

	// how many genes are visible?
	//
	int count = 0;
	for(int g=0; g < edata.getNumSpots(); g++)
	{
	    double xval = edata.eValue(x_meas, local_traversal[g]);
	    double yval = edata.eValue(y_meas, local_traversal[g]);
	    
	    if(spotIsVisible(xval, yval))
	    {
		count++;
	    }
	}

	int genes_per_bin = (int)((double)count / (double)(n_bins));

	// System.out.println(count + " genes currently visible");
	// System.out.println("target is " + genes_per_bin + " genes per bin");

	int gene = 0;
	int visible_so_far = 0;
	int bin = 0;
	int in_this_bin = 0;

	// System.out.println("making boundries:");
 
	for(int g=0; g < edata.getNumSpots(); g++)
	{
	    double xval = edata.eValue(x_meas, local_traversal[g]);
	    double yval = edata.eValue(y_meas, local_traversal[g]);
	    double val = (is_x) ? xval : yval;

	    if(spotIsVisible(xval, yval))
	    {
		if(visible_so_far == 0)
		{
		    boundary[0] = val;
		    // System.out.println(0 + "\t" + boundary[0]);
		}

		visible_so_far++;
		in_this_bin++;

		if(in_this_bin >= genes_per_bin)
		{
		    if(bin < (n_bins-1))
		    {
			bin++;
			boundary[bin] = val;
			in_this_bin = 0;
			//System.out.println(bin + "\t" + boundary[bin]);
		    } 
		}
	    }
	}
	boundary[bin] = edata.getMeasurementMaxEValue(set);


	if(is_x)
	    x_bin_minima = boundary;
	else
	    y_bin_minima = boundary;
    }

    private void levelize_one()
    {
	// find the worst bin
       
    }

    private void levelize_two()
    {
	// chose the worst bin in and adjust

	// sum the rows and columns
	
	int[] x_bin_hits = new int[n_x_boxes];
	int[] y_bin_hits = new int[n_y_boxes];
	

	for(int x=0;x < n_x_boxes; x++)
	{
	    for(int y=0;y < n_y_boxes; y++)
	    {
		x_bin_hits[x] += box_hits[x][y];
		y_bin_hits[y] += box_hits[x][y];
	    }
	}

	int biggest_bin = 0;
	int biggest_hits = 0;
	boolean biggest_is_x = true;

	for(int x=0;x < n_x_boxes; x++)
	{
	    if(x_bin_hits[x] > biggest_hits)
	    {
		biggest_hits = x_bin_hits[x];
		biggest_bin = x;
	    }
	}
	for(int y=0;y < n_y_boxes; y++)
	{
	    if(y_bin_hits[y] > biggest_hits)
	    {
		biggest_hits = y_bin_hits[y];
		biggest_is_x = false;
		biggest_bin = y;
	    }
	}
	
	/*
        System.out.println("worst bin is " + 
			   (biggest_is_x ? "X" : "Y") + 
			   " bin number " + biggest_bin + 
			   " with " + biggest_hits + " elements");
	*/
	int max_bin = biggest_is_x ? (n_x_boxes-1) : (n_y_boxes-1);

	double[] minima_vec = biggest_is_x ? x_bin_minima : y_bin_minima;

	double scale = 0.5;

	if(biggest_bin == max_bin)
	{
	    // redistribute to the left only
	    
	    // increment this min value....
	    if(minima_vec[biggest_bin] > 0)
		minima_vec[biggest_bin] *= 1.1;
	    else
		minima_vec[biggest_bin] *= ((1.0)/(1.1));
	}
	else
	{
	    if(biggest_bin == 0)
	    {
		// redistribute to the right only

		// reduce the value of the bin to the right
		if(minima_vec[1] > 0)
		    minima_vec[1] *= 0.9;
		else
		    minima_vec[1] *= ((1.0)/(0.9));
	    }
	    else
	    {
		// redistribute in both direction
		
		// increment this min value....
		if(minima_vec[biggest_bin] > 0)
		    minima_vec[biggest_bin] *= 1.1;
		else
		    minima_vec[biggest_bin] *= ((1.0)/(1.1));
		
		// reduce the value of the bin to the right
		if(minima_vec[biggest_bin+1] > 0)
		    minima_vec[biggest_bin+1] *= 0.9;
		else
		    minima_vec[biggest_bin+1] *= ((1.0)/(0.9));
	    }

	}

	/*
	if(biggest_bin > (max_bin/2))
	{
	    // reduce number of things in this bin 
	    // by increasing the min value...
	    minima_vec[biggest_bin] *= 1.5;
	    
	    // to the left make everything corresponding smaller
	    for(int b=biggest_bin-1; b > 0; b--)
	    {
		//minima_vec[b] = (minima_vec[b] * (1.0-scale)) + (minima_vec[b+1] * scale);
		minima_vec[b] *= (1.0+scale);
		scale *= 0.5;
	    }
	}
	else
	{
	    if(biggest_bin > 0)
	    {
		// reduce number of things in this bin 
		// by increasing the min value...
		minima_vec[biggest_bin] *= 1.5;
	    }

	    // to the right make everything smaller
	    for(int b=biggest_bin+1; b <= max_bin; b++)
	    {
		minima_vec[b] = (minima_vec[b] * (1.0-scale)) + (minima_vec[b-1] * scale);
		minima_vec[b] *= (1.0+scale);
		scale *= 0.5;
	    }
	}
	*/

	/*
	if(biggest_bin == max_bin)
	{
	    // redistribute to the left only
	    for(int b=max_bin; b > 0; b--)
	    {
		minima_vec[b] = minima_vec[b] * (1.0+scale);
		scale *= 0.5;
	    }
	}
	else
	{
	    if(biggest_bin == 0)
	    {
		// redistribute to the right only
		for(int b=0; b < max_bin; b++)
		{
		    minima_vec[b] = minima_vec[b] * (1.0-scale);
		    scale *= 0.5;
		}
	    }
	    else
	    {
		
		if(biggest_bin > (max_bin/2))
		{
		    // to the left make everything smaller
		    for(int b=biggest_bin; b > 0; b--)
		    {
			minima_vec[b] = (minima_vec[b] * (1.0-scale)) + (minima_vec[b-1] * scale);
			scale *= 0.5;
		    }
		}
		else
		{
		    // to the right make everything smaller
		    for(int b=biggest_bin+1; b < max_bin; b++)
		    {
			minima_vec[b] = (minima_vec[b] * (1.0-scale)) + (minima_vec[b-1] * scale);
			scale *= 0.5;
		    }
		}
	    }

	}
	*/

	/*
	System.out.println("levelized to:");
	for(int b=0; b <= max_bin; b++)
	{
	    System.out.println(b + "\t" + minima_vec[b]);
	}
	*/

	binGenes();
	panel.repaint();
    }

    private void makeBinSmaller(double[] bins, int max_bin, int bin)
    {
	double scale = 0.5;

	if(bin == max_bin)
	{
	    // redistribute to the left only
	    
	    // increment this min value....
	    if(bins[bin] > 0)
		bins[bin] *= 1.1;
	    else
		bins[bin] *= ((1.0)/(1.1));
	}
	else
	{
	    if(bin == 0)
	    {
		// redistribute to the right only

		// reduce the value of the bin to the right
		if(bins[1] > 0)
		    bins[1] *= 0.9;
		else
		    bins[1] *= ((1.0)/(0.9));
	    }
	    else
	    {
		// redistribute in both direction
		
		// increment this min value....
		if(bins[bin] > 0)
		    bins[bin] *= 1.1;
		else
		    bins[bin] *= ((1.0)/(1.1));
		
		// reduce the value of the bin to the right
		if(bins[bin+1] > 0)
		    bins[bin+1] *= 0.9;
		else
		    bins[bin+1] *= ((1.0)/(0.9));
	    }

	}

    }

    private void levelize()
    {
	// chose the worst individual bin and adjust its 'row' and 'column' bins


	// sum the rows and columns
	/*
	int[] x_bin_hits = new int[n_x_boxes];
	int[] y_bin_hits = new int[n_y_boxes];
	
	for(int x=0;x < n_x_boxes; x++)
	    x_bin_hits[x] = 0;
	for(int y=0;y < n_y_boxes; y++)
	    y_bin_hits[y] = 0;
	   
	for(int x=0;x < n_x_boxes; x++)
	{
	    for(int y=0;y < n_y_boxes; y++)
	    {
		x_bin_hits[x] += box_hits[x][y];
		y_bin_hits[y] += box_hits[x][y];
	    }
	}
	*/

	int biggest_x_bin = 0;
	int biggest_y_bin = 0;
	int biggest_hits = 0;

	for(int x=0;x < n_x_boxes; x++)
	{
	    for(int y=0;y < n_y_boxes; y++)
	    {
		if(box_hits[x][y] > biggest_hits)
		{
		    biggest_hits = box_hits[x][y];
		    biggest_x_bin = x;
		    biggest_y_bin = y;
		}
	    }
	}

        //System.out.println("worst bin is in X col " + biggest_x_bin);
        //System.out.println("worst bin is in Y row " + biggest_y_bin);

	makeBinSmaller(x_bin_minima, n_x_boxes-1, biggest_x_bin);
	makeBinSmaller(y_bin_minima, n_y_boxes-1, biggest_y_bin);

	/*
	System.out.println("levelized to:");

	int max_bin = (n_x_boxes > n_y_boxes) ? n_x_boxes : n_y_boxes;
	for(int b=0; b < max_bin; b++)
	{
	    System.out.println(b + "\t" + x_bin_minima[b] + "\t" + y_bin_minima[b]);
	}
	*/

	binGenes();
	panel.repaint();

	//System.out.println("levelised...");

    }
 
    // returns the minimum value for genes allocated to this X box
    //
    public double getXBoxMinima(int box)
    {
	if(x_box_mode == EquallySpaced)
	{
	    return x_meas_min + (box * bin_x_e_gap);
	}
	else
	{
	    if(box < n_x_boxes)
		return x_bin_minima[box];
	    else
		return x_bin_minima[n_x_boxes-1];
	}	
    }

    // returns the minium value for genes allocated to this Y box
    //
    public double getYBoxMinima(int box)
    {
	if(y_box_mode == EquallySpaced)
	{
	    return y_meas_min + (box * bin_y_e_gap);
	}
	else
	{
	    if(box < n_y_boxes)
		return y_bin_minima[box];
	    else
		return y_bin_minima[n_y_boxes-1];
	}
	
    }

    // returns the X box for this value
    //
    public int getXBox(double value)
    {
	int xbox = 0;
	if(x_box_mode == EquallySpaced)
	{
	    xbox = (int)((value - x_meas_min)  / bin_x_e_gap);
	}
	else
	{
	    if(x_bin_minima == null)
		return 0;

	    // find the lowest boundary...
	    while((xbox < (n_x_boxes-1)) && (value > x_bin_minima[xbox+1]))
	    {
		//System.out.println(xbox + "\t" + value  + " < " + x_bin_minima[xbox]);
		xbox++;
	    }
	}

	if(xbox >= n_x_boxes)
	    System.out.println("DAMN! xbox is too big (" + xbox + ")");

	return xbox;
    }
    // returns the Y box for this value
    //
    public int getYBox(double value)
    {
	int ybox = 0;
	if(y_box_mode == EquallySpaced)
	{
	    ybox = (int)((value - y_meas_min)  / bin_y_e_gap);
	}
	else
	{
	    if(y_bin_minima == null)
		return 0;

	    // find the lowest boundary...
	    while((ybox <  (n_y_boxes-1)) && (value > y_bin_minima[ybox+1]))
		ybox++;
	}

	if(ybox >= n_y_boxes)
	    System.out.println("DAMN! ybox is too big (" + ybox + ")");

	return ybox;
    }

    // returns the X box for this value, even if the box
    //  doesnt' exist (ie. it's offscreen)
    //
    public int getXBoxNoClip(double value)
    {
	int xbox = 0;
	if(x_box_mode == EquallySpaced)
	{
	    xbox = (int)((value - x_meas_min)  / bin_x_e_gap);
	}
	else
	{
	    if(x_bin_minima == null)
		return 0;

	    if(value < x_bin_minima[0])
	    {
	       return -5;
	    }
	    else
	    {
		// find the lowest boundary...
		while((xbox < (n_x_boxes-1)) && (value > x_bin_minima[xbox+1]))
		    xbox++;
	    }
	    
	    //if(value > x_bin_minima[n_x_boxes-1])
	    //	xbox = n_x_boxes+5;
	    if(value > x_meas_max)
		xbox = n_x_boxes+5;
	}
	return xbox;
    }
    // returns the Y box for this value
    //
    public int getYBoxNoClip(double value)
    {
	int ybox = 0;
	if(y_box_mode == EquallySpaced)
	{
	    ybox = (int)((value - y_meas_min)  / bin_y_e_gap);
	}
	else
	{
	    if(y_bin_minima == null)
		return 0;

	    if(value < y_bin_minima[0])
	    {
	       return -5;
	    }
	    else
	    {
		// find the lowest boundary...
		while((ybox <  (n_y_boxes-1)) && (value > y_bin_minima[ybox+1]))
		    ybox++;
	    }
	   //if(value > y_bin_minima[n_y_boxes-1])
	   //    ybox = n_y_boxes+5;

	   if(value > y_meas_max)
		ybox = n_y_boxes+5;
	}
	return ybox;
    }

    public boolean spotIsVisible(double xv, double yv)
    {
	return ((xv >= x_meas_min) &&
		(yv >= y_meas_min) &&
		(xv <= x_meas_max) &&
		(yv <= y_meas_max));
    }

    public class Range extends Object
    {
	public double x_meas_min, x_meas_max;
	public double y_meas_min, y_meas_max;
    }

    private Vector history = new Vector();

    public void binGenes()
    {
	if((x_meas < 0) || (y_meas < 0))
	    return;
	
	// System.out.println("binning for Measurements " + x_meas + " and " + y_meas);

	for(int x=0;x<n_x_boxes;x++)
	    for(int y=0;y<n_y_boxes;y++)
	    {
		box_hits[x][y] = 0; 
		bin_contents[x][y].removeAllElements();
	    }
	
	int total_count = 0;

	for(int g=0; g < edata.getNumSpots(); g++)
	{
	    double xval = edata.eValue(x_meas, g);
	    double yval = edata.eValue(y_meas, g);
	    
	    if(spotIsVisible(xval, yval))
	    {
		if((use_filter == false) || (!edata.filter(g)))
		{
		    {
			int xbox = getXBox(xval);
			int ybox = getYBox(yval);
			
			//System.out.println(xval + " -> " + xbox);
			
			if((xbox  >= 0) && (ybox  >= 0))
			{
			    box_hits[xbox][ybox]++;
			    bin_contents[xbox][ybox].addElement(new Integer(g));
			    
			    total_count++;
			}
		    }
		}
	    }
	}

	bg_image = null;
	/*
	System.out.println(total_count + " genes put into "  + 
			   (n_x_boxes * n_y_boxes) + " bins");

	
	for(int y=0;y<n_y_boxes;y++)
	{
	    for(int x=0;x<n_x_boxes;x++)
	    {
		System.out.print(box_hits[x][y] + "\t");
	    }
	    System.out.println();
	} 
	*/

    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   box layout
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    final static int GRID_NONE = -1;
    final static int GRID_SINGLE = 0;
    final static int GRID_DOUBLE = 1;
    final static int GRID_QUAD = 2;
    final static int GRID_3x2 = 3;
    final static int GRID_3x3 = 4;
    final static int GRID_CUSTOM = 5;
    final static int GRID_ALL = 6;
    
    final static String[] grid_mode_names = { "Single (1x1)", "Double (2x1)", "Quad (2x2)", "3x2", "3x3", "Custom", "All" };

    private void setGridLayout(int new_mode)
    {
	if(edata.getNumMeasurements() == 0)
	{
	    grid_mode = GRID_NONE;
	    grid_col = new int[0];
	    return;
	}

	grid_mode = new_mode;
	
	int old_nm = (grid_col == null) ? 0 : grid_col.length;
	int nm = 0;
	
	switch(grid_mode)
	{
	    case GRID_ALL:
		nm = edata.getNumMeasurements();
		final double ar = (double) box_width / (double) box_height;
		
		//System.out.println("box aspect ratio=" + ar + " (w=" + box_width + " h=" + box_height + ")");
		
		// find the layout with the aspect ratio closest to the aspect
		// ratio of the box the grid will be drawn into
		
		double best_d = 0;
		grid_r = -1;
		
		for(int c=1; c <= nm; c++)
		{
		    int r = (nm / c);
		    if((nm - (r * c)) > 0)
			r++;
		    
		    double g_ar = (double) c / (double) r;
		    
		    double diff = Math.abs(ar - g_ar);
		    
		    //System.out.println("  " + c + "," + r + " = " + g_ar);
		    
		    if((diff < best_d) || (grid_r < 0))
		    {
			best_d = diff;
			
			grid_c = c;
			grid_r = r;
		    }
		}
		break;
		
	case GRID_SINGLE:
	    nm = 1;
	    grid_r = grid_c = 1;
	    break;
	    
	case GRID_DOUBLE:
	    nm = 2;
	    grid_c = 2;
	    grid_r = 1;
	    break;
	    
	case GRID_QUAD:
	    nm = 4;
	    grid_c = 2;
	    grid_r = 2;
	    break;
	    
	case GRID_3x2:
	    nm = 6;
	    grid_c = 3;
	    grid_r = 2;
	    break;
	    
	case GRID_3x3:
	    nm = 9;
	    grid_c = 3;
	    grid_r = 3;
	    break;
	    
	default:
	    nm = 1;
	    grid_r = grid_c = 1;
	    break;
	}
	
	boolean mode_changed = (nm != old_nm);

	// now that rows,cols is known, figure out the grid coords 
	//
	
	//System.out.println("best layout is " + grid_c + " cols, " + grid_r + " rows");
	
	// only use 90% of the available space to leave a border between boxes
	
	double grid_w_d = (((double) box_width) * 0.95)  / (double) grid_c;	
	double grid_h_d = (((double) box_height) * 0.95) / (double) grid_r;
	
	grid_w = (int)grid_w_d;
	grid_h = (int)grid_h_d;
	
	if(mode_changed)
	{
	    grid_col = new int[nm];
	    grid_x   = new int[nm];
	    grid_y   = new int[nm];
	}
	
	int r = 0;
	int c = 0;
	
	for(int m=0; m < nm; m++)
	{
	    if(mode_changed)
		grid_col[m] = edata.getMeasurementAtIndex(m);
	    
	    grid_x[m] = grid_w * c; //(int)Math.floor(((double) c * grid_w_d));
	    grid_y[m] = grid_h * r; //(int)Math.floor(((double) r * grid_h_d));
	    
	    if(++c == grid_c)
	    {
		c = 0;
		r++;
	    }
	}

	grid_w--;
	grid_h--;
    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   StackDrawPanel
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public class StackDrawPanel extends DragAndDropPanel implements MouseListener, MouseMotionListener, Printable
    {
	private Point last_pt = null;

	public StackDrawPanel()
	{
	    super();
	    addMouseListener(this);
	    addMouseMotionListener(this);
	}

	// --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
	// --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
	
	public synchronized void repaintDisplay()
	{
	    if(drawing)
	    {
		// stop existing thread
		int counter = 0;
		abort_draw = true;
		//System.out.println("waiting for existing thread to stop");
		while((counter < 30) && (drawing == true))
		{
		    try
		    {
			Thread.sleep(100);
		    }
		    catch(InterruptedException tie)
		    {
		    }
		    counter++;
		}
	    }
	    abort_draw = false;
	    drawing = true;
	    rt = new RedrawThread();
	    rt.start();
	}
	
	private boolean abort_draw = false;
	private boolean drawing = false;
	private RedrawThread rt = null;
	
	public class RedrawThread extends Thread
	{
	    public void run()
	    {
		frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		
		repaint();

		//System.out.println("starting draw thread");
		
		/*
		Image new_total_image = createImage(getWidth(), getHeight());
		
		
		if(spot_image == null)
		{		
		    Image new_spot_image = drawSpots(getWidth(), getHeight());
		    
		    if(new_spot_image == null)
		    {
			drawing = false;
			//System.out.println("draw thread finished early...");
			frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			return;
		    }

		    
		    spot_image = new_spot_image;
		    
		}
		
		Graphics ntig = new_total_image.getGraphics();

		// paint spot image
		//
		if(!abort_draw)
		    ntig.drawImage(spot_image, 0, 0, null  );

		// and add the overlays
		
		if(!abort_draw)
		    drawClusters(ntig, getWidth(), getHeight());
		
		if(!abort_draw)
		    drawLabels(ntig, getWidth(), getHeight());
		
		if(!abort_draw)
		    deco_man.drawDecorations(ntig, getWidth(), getHeight() );
		    
		ntig = null;

		//if(abort_draw)
		//    System.out.println("draw thread aborted");
		//else
		//    System.out.println("draw thread finished");

		if(!abort_draw)
		{
		    total_image = new_total_image;
		    getGraphics().drawImage(total_image, 0, 0, null);
		    }
		*/

		frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		
		System.gc();

		drawing = false;
	    }
	}
    
	
	// --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
	// --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
	
	public void highlightSpot(int spot_id)
	{
	    // work out which box the spot is in...
	    double xval = edata.eValue(x_meas, spot_id);
	    double yval = edata.eValue(y_meas, spot_id);

	    int xbox = getXBox(xval);
	    int ybox = getYBox(yval);
		
	    if((xbox  >= 0) && (ybox  >= 0))
	    {
		System.out.println("highlight " + xbox + "," + ybox);

		int xp = xbox * (box_width);
		int yp = getHeight() - ((ybox+2) * box_height);
		
		double height_scale_d = ((double) height_scale / 100.0);
		
		double twice_height_scale_d = height_scale_d * 2.0;
		double half_height_scale_d  = height_scale_d * 0.5;
		
		double box_h = (double) (bin_contents[xbox][ybox].size());

		int xt = xp + (int)((half_height_scale_d * box_h));
		int yt = yp - (int)((twice_height_scale_d * box_h));

		Graphics g = getGraphics();

		g.setColor(text_col);

		g.drawLine(xp, yp, xp, yp+grid_h);
		g.drawLine(xp+1, yp, xp+1, yp+grid_h);

		g.drawLine(xp,yp+grid_h, xp+grid_w,yp+grid_h);
		g.drawLine(xp,yp+grid_h+1, xp+grid_w,yp+grid_h+1);

		g.drawRect(xt, yt, grid_w, grid_h);
		g.drawRect(xt+1, yt+1, grid_w-2, grid_h-2);

		g.drawLine(xp+grid_w,yp+grid_h, xt+grid_w,yt+grid_h);
		g.drawLine(xp,yp, xt,yt);
		g.drawLine(xp,yp+grid_h, xt,yt+grid_h);
		
		g = null;
		System.gc();

		Timer timer = new Timer(4000, new ActionListener() 
		    {
			public void actionPerformed(ActionEvent evt) 
			{
			    repaint();
			}
		    });
		timer.start();
	    }
	}

	public void mouseMoved(MouseEvent e) 
	{
	    if(box_hits == null)
		return;

	    Point pt = new Point();
	    double xval, yval, xval_t, yval_t;

	    pt.x  = e.getX() / (box_width);
	    pt.y  = (getHeight() - e.getY()) / (box_height) - 1; //pt.y  = e.getY() / (box_height);

	    
	    // don't  need to change the label if the mouse
	    // is in the same box as last time
	    if(last_pt != null)
		if((pt.x == last_pt.x) && (pt.y == last_pt.y))
		    return;

	    if((pt.x >=0) && (pt.y >=0) && (pt.x < n_x_boxes) && (pt.y < n_y_boxes))
	    {

		// figure out the values corresponding to this box

		try
		{
		    xval   = getXBoxMinima(pt.x);
		    xval_t = getXBoxMinima(pt.x+1);
		    
		    yval = getYBoxMinima(pt.y);
		    yval_t = getYBoxMinima(pt.y+1);
		}
		catch(NullPointerException npe)
		{
		    status_label.setText("No data");
		   return;
		}
		
		String str = null;

		try
		{
		    String xstr   = String.valueOf(xval);
		    String xstr_t = String.valueOf(xval_t);
		    String ystr = String.valueOf(yval);
		    String ystr_t = String.valueOf(yval_t);

		    xstr   =   xstr.substring(0, ((  xstr.length() > 7) ? 7 :   xstr.length()));
		    xstr_t = xstr_t.substring(0, ((xstr_t.length() > 7) ? 7 : xstr_t.length()));
		    ystr   =   ystr.substring(0, ((  ystr.length() > 7) ? 7 :   ystr.length()));
		    ystr_t = ystr_t.substring(0, ((ystr_t.length() > 7) ? 7 : ystr_t.length()));

		    str = new String(" [ " + xstr + " ... " + xstr_t + ", " + 
				     ystr + " ... " + ystr_t + ", " );
		    
		    if(box_hits[pt.x][pt.y] == 1)
		    {
			int gene = ((Integer)bin_contents[pt.x][pt.y].elementAt(0)).intValue();
			String n_str = nt_sel.getNameTag(gene);
			if(n_str == null)
			    n_str = "(no label)";
			str += (n_str + " ]");
		    }
		    else
		    {
			if(box_hits[pt.x][pt.y] > 1)
			{
			    str += (box_hits[pt.x][pt.y] + " ]");
			}
			else
			{
			    str += " ]";
			}
		    }
		}
		catch(NullPointerException npe)
		{
		    str = "No data";
		}
		catch (StringIndexOutOfBoundsException siobe)
		{
		    str = "String problem..."; // System.out.println("ah, " + siobe.toString());
		}

		status_label.setText(str);
	    }
	    else
		status_label.setText("Feh");
	    
	    last_pt = pt;

	}

	private Point drag_start = new Point();
	private Point drag_size = new Point();

	private boolean dragging = false;

	public void mouseDragged(MouseEvent e) 
	{
	    if(dragging)
	    {
		drawRubberBox();

		// swap the start and end x coords if the width becomes negative
		int x = e.getX();
		if(x < drag_start.x)
		{
		    drag_size.x = drag_start.x - x;
		    drag_start.x = x;
		}
		else
		    drag_size.x = x - drag_start.x;

		// likewise for the y coords
		int y = e.getY();
		if(y < drag_start.y)
		{
		    drag_size.y = drag_start.y - y;
		    drag_start.y = y;
		}
		else
		    drag_size.y = y - drag_start.y;

		// constrain the aspect ratio to match the window shape
		
		double screen_aspect_ratio = (double) getWidth() / (double) getHeight();

		if(drag_size.y >= drag_size.x)
		{
		    drag_size.x = (int)((double)drag_size.y * screen_aspect_ratio);
		}
		else
		{
		    drag_size.y = (int)((double)drag_size.x / screen_aspect_ratio);
		}

		drawRubberBox();
	    }

	} 

	class customMenuListener implements ActionListener
	{
	    public void actionPerformed(ActionEvent e) 
	    {
		JMenuItem source = (JMenuItem)(e.getSource());
		{
		    System.out.println("menu event for " + source.getText());
		    
		    if((source.getText().equals("[ All ]")) || 
		       (source.getText().equals("[ Both ]")))
		    {
			//System.out.println("what is all???");
			mview.genesSelected(selected_spots);
		    }
		    else
		    {
			//mview.getAnnotationLoader().loadAnnotation(source.getText());
			mview.geneSelected(source.getText());
		    }
		}
	    }
	}

	String[] selected_spots = null;

	public void mousePressed(MouseEvent e) 
	{
	    if (e.isPopupTrigger() || e.isControlDown()) 
	    {
		if(box_hits == null)
		    return;

		JPopupMenu popup = new JPopupMenu();
		customMenuListener menu_listener = new customMenuListener();
		Point pt = new Point();

		pt.x  = e.getX() / (box_width);
		pt.y  = ((getHeight() - e.getY()) / box_height) - 1;
		
		if(box_hits[pt.x][pt.y] < 100)
		{
		    selected_spots = new String[ box_hits[pt.x][pt.y] ];

		    for(int i=0; i < box_hits[pt.x][pt.y]; i++)
		    {
			int gene = ((Integer)bin_contents[pt.x][pt.y].elementAt(i)).intValue();
			String n_str = nt_sel.getNameTag(gene);
			if(n_str == null)
			    n_str = "(no label)";
			selected_spots[i] = n_str;
			JMenuItem item = new JMenuItem(selected_spots[i]);
			item.addActionListener(menu_listener);
			popup.add(item);
		    }		    
		    
		    if(box_hits[pt.x][pt.y] > 1)
		    {
			popup.addSeparator();
			JMenuItem item = new JMenuItem();
			item.setText((box_hits[pt.x][pt.y] == 2) ? "[ Both ]" : "[ All ]");
			item.addActionListener(menu_listener);
			popup.add(item);
			
		    }
		}
		else
		{
		    // no action listener, so it doesn't do anything
		    //
		    JMenuItem item = new JMenuItem("[ too many ]");
		    popup.add(item);
		    selected_spots = null;
		}
		popup.show(e.getComponent(),
			   e.getX(), e.getY());

	    }
	    else
	    {
		dragging = true;
		
		drag_start.x = e.getX();
		drag_start.y = e.getY();
		drag_size.x = 0;
		drag_size.y = 0;
		
		drawRubberBox();
	    }
	}
	
	public void mouseReleased(MouseEvent e) 
	{
	    if(dragging)
	    {
		drawRubberBox();
		dragging = false;

		// ignore requests for very thin or very small zooms,
		// they have most likely occured by mistake
		//
		if((drag_size.x < box_width) || (drag_size.y < box_height))
		    return;

		// save the current range
		//
		Range range = new Range();
		range.x_meas_min = x_meas_min;
		range.y_meas_min = y_meas_min;
		range.x_meas_max = x_meas_max;
		range.y_meas_max = y_meas_max;
		history.insertElementAt(range, 0);
		back_jb.setEnabled(true);
		// fore_jb.setEnabled(false);

		// set the new range to that specified by the rubber box
		//
		int min_x_box = drag_start.x / (box_width);
		//int min_y_box = drag_start.y / (box_height);
		int max_y_box = ((getHeight() - drag_start.y) / box_height) - 1; 

		int max_x_box = (drag_start.x+drag_size.x) / (box_width);
		//int max_y_box = (drag_start.y+drag_size.y) / (box_height);
		int min_y_box = ((getHeight() - (drag_start.y+drag_size.y)) / box_height) - 1;
		
		x_meas_min = getXBoxMinima(min_x_box);
		y_meas_min = getYBoxMinima(min_y_box);

		x_meas_max = getXBoxMinima(max_x_box);
		y_meas_max = getYBoxMinima(max_y_box);

		setBoundaryValues();
		panel.repaint();

		/*
		System.out.println(String.valueOf(x_meas_min) + " , " + 
				   String.valueOf(y_meas_min) + " -> " +
				   String.valueOf(x_meas_max) + " , " + 
				   String.valueOf(y_meas_max));
		*/
	    }
	}
	
	public void mouseEntered(MouseEvent e) 
	{
	}

	public void mouseExited(MouseEvent e) 
	{
	    status_label.setText("Mmmmmmmmmm");
	    last_pt = null;
	}

	public void mouseClicked(MouseEvent e) {}

	public void drawRubberBox()
	{
	    Graphics graphic = getGraphics();
	    graphic.setXORMode(text_col);
	    graphic.drawRect(drag_start.x, drag_start.y, drag_size.x, drag_size.y);
	}

	public void drawFilterAlert(boolean draw_it)
	{
	    Graphics gr = getGraphics();
	    if(gr != null)
	    {
		gr.setColor(draw_it ? Color.red : background_col);
		gr.fillRect(getWidth() - 20, 10, 10, 10);
	    }
	}


	public int print(Graphics g, PageFormat pf, int pg_num) throws PrinterException 
	{
	    // margins
	    //
	    g.translate((int)pf.getImageableX(), 
			(int)pf.getImageableY());
	    
	    // area of one page
	    //
	    // ??  area seems to be the right size, c.f. ScatterPlot...
	    //
	    int pw = (int)(pf.getImageableWidth());   
	    int ph = (int)(pf.getImageableHeight());
	    
	    // System.out.println("PRINT REQUEST for page " + pg_num + " size=" + pw + "x" + ph); 
	    
	    frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

	    bg_image = null;
	    
	    drawBoxes( pw, ph );
	    g.drawImage(bg_image, 0, 0, null );
	    if(show_clusters)
		drawClusters(g, pw, ph);
	    deco_man.drawDecorations(g, pw, ph);

	    /*
	    Image new_spot_image = drawSpots(pw, ph);
	    g.drawImage(new_spot_image, 0, 0, null );
	    drawClusters(g, pw, ph);
	    drawLabels(g, pw, ph);
	    deco_man.drawDecorations(g, pw, ph);
	    */

	    frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		
	    // panel.paintIntoRegion(g, pw, ph);

	    return (pg_num > 0) ? NO_SUCH_PAGE : PAGE_EXISTS;
	}

	public void drawBoxes(int width, int height)
	{
	    bg_image = createImage(width, height);
	    if(bg_image == null)
		return;
	    Graphics graphic = bg_image.getGraphics();

	    //for(int g=0; g < grid_col.length; g++)
	    {
		//System.out.println(g + "\tm=" + grid_col[g]);
	    }

	    int set, gene;
	    
	    graphic.setColor(background_col);
	    graphic.fillRect(0, 0, width, height);
	    
	    graphic.setFont(dplot.getFont());
	    label_height = dplot.getFontHeight();

	    double height_scale_d = ((double) height_scale / 100.0);

	    double twice_height_scale_d = height_scale_d * 2.0;
	    double half_height_scale_d  = height_scale_d * 0.5;

	    if((x_meas < 0) || (y_meas < 0))
		return;
	    
	    box_width  = width / ((n_x_boxes)+1); // add an extra one in each dimension for the axes
	    box_height = height / ((n_y_boxes)+1);    
	    
	    setGridLayout( grid_mode );

	    int xbox, ybox;

	    max_labels_fit = (box_height / label_height);

	    int box_fill_w_m_1 = box_width-1;
	    int box_fill_w_m_2 = box_width-2;
	    int box_fill_h_m_1 = box_height-1;

	    for(int x=(n_x_boxes-1);x>=0;x--)
		//for(int y=0;y<n_y_boxes;y++)
		for(int y=(n_y_boxes-1);y>=0;y--)
		{
		    xbox = x * (box_width);

		    // invert y axis so that 0 is at the bottom
		    // (actually, 0 is at the box minus 2 box heights,
		    //  one for the last box, and one for the x axis)
		    //
		    ybox = height - ((y+2) * box_height);
		    
		    for(int g=0;g < bin_contents[x][y].size(); g++)
		    {
		        gene = ((Integer)bin_contents[x][y].elementAt(g)).intValue();

			double xval = edata.eValue(x_meas, gene);
			double yval = edata.eValue(y_meas, gene);
			
			// move diagonally upwards and to the right
			int xb = xbox + (int)((half_height_scale_d * (double)g));
			int yb = ybox - (int)((twice_height_scale_d * (double)g));
			   
			/* 
			if(g > 0)
			{
			    graphic.setColor(background_col);
			    graphic.fillRect(xb, yb-1, box_fill_w_m_1, box_height);
			}
			*/

			/*
			graphic.setColor(dplot.getDataColour(xval, x_meas));
			graphic.fillRect(xb, yb,  box_fill_w_m_2, box_fill_h_m_1);
			
			graphic.setColor(dplot.getDataColour(yval, y_meas));
			graphic.fillRect(xb+(box_fill_w_m_1), yb, box_fill_w_m_1, box_fill_h_m_1);
			*/

			for(int grid=0; grid < grid_col.length; grid++)
			{
			    double val = edata.eValue(grid_col[grid], gene);
			    
			    graphic.setColor(dplot.getDataColour(val, grid_col[grid])); 
			    
			    graphic.fillRect(xb + grid_x[grid], yb + grid_y[grid], grid_w, grid_h);   
			}
		    }
		    
		    // now all graphical elements have been plotted, draw the names 
		    //
		    graphic.setColor(text_col);
		    graphic.setXORMode(background_col);

		    // move diagonally upwards and to the right
		    xbox += (int)((half_height_scale_d * (double)(bin_contents[x][y].size())));
		    ybox -= (int)((twice_height_scale_d * (double)(bin_contents[x][y].size())));
		    xbox += 2;
		    ybox += (label_height + 2);

		    int in_this_bin = bin_contents[x][y].size();
		    if(in_this_bin <= max_labels_fit)
		    {
			for(int g=0;g < in_this_bin; g++)
			{
			    int yb = (ybox + (g * label_height));
			    
			    gene = ((Integer)bin_contents[x][y].elementAt(g)).intValue();
			    
			    String n_str = nt_sel.getNameTag(gene);
			    if(n_str == null)
				n_str = "(no label)";
			    graphic.drawString(n_str, xbox, yb); 
			}
		    }
		    else
		    {
			int yb = ybox;
			if(max_labels_fit > 2)
			{
			    // just draw the first and last labels
			    gene = ((Integer)bin_contents[x][y].elementAt(0)).intValue();
			    String n_str = nt_sel.getNameTag(gene);
			    if(n_str == null)
				n_str = "(no label)";
			    graphic.drawString(n_str, xbox, yb); 
			    yb += label_height;
			    graphic.drawString("-- [ " + (in_this_bin-2) + " spots ] --", xbox, yb); 
			    yb += label_height;
			    gene = ((Integer)bin_contents[x][y].elementAt(bin_contents[x][y].size()-1)).intValue();
			    n_str = nt_sel.getNameTag(gene);
			    if(n_str == null)
				n_str = "(no label)";
			    graphic.drawString(n_str, xbox, yb); 
			}
			else
			{
			    graphic.drawString("[ " + in_this_bin + " ]", xbox, yb); 
			}
		    }

		    graphic.setPaintMode();
		}
	    //System.out.println("painted " + 
	    //String.valueOf(x_meas_min) + " , " + String.valueOf(y_meas_min) + " -> " +
	    //String.valueOf(x_meas_max) + " , " + String.valueOf(y_meas_max));

	    if(show_axes)
		drawAxes(graphic, width, height);

	}
	
	private void drawAxes(Graphics graphic, int width, int height)
	{

	    // --------------------------------------------- /
	    // --------------------------------------------- /
	    // draw the axes                                 /
	    // --------------------------------------------- /
	    graphic.setColor(text_col);
	    
	    int ticklen = 3;

	    // horizontal
	    //
	    int x_axis_y_pos = height - (box_height - 1);
	    int x_axis_label_y_pos = height - ((box_height - label_height)/2);
	    int x_axis_label_x_pos = 0;
	    for(int x=0; x < n_x_boxes;x++)
	    {
		double xval = getXBoxMinima(x);
		String xstr   = String.valueOf(xval);
		if(xstr.length() > 7)
		    xstr = xstr.substring(0, 7);
		graphic.drawString(xstr, x_axis_label_x_pos, x_axis_label_y_pos);
		graphic.drawLine(x_axis_label_x_pos, x_axis_y_pos, x_axis_label_x_pos, x_axis_y_pos + ticklen);
		x_axis_label_x_pos += (box_width * 1);
	    }
	    int x_end = (n_x_boxes * (box_width * 1));
	    
	    graphic.drawLine(0, x_axis_y_pos, x_end, x_axis_y_pos);
	    graphic.drawLine(x_end, x_axis_y_pos, x_end, x_axis_y_pos + ticklen);
	    
	    // vertical
	    //
	    int y_axis_x_pos = width - box_width;
	    int y_axis_label_y_pos = label_height+1;
	    int y_axis_label_x_pos = y_axis_x_pos + ticklen;
	    for(int y=0; y < n_y_boxes;y++)
	    {
		y_axis_label_y_pos = height - ((y+2) * box_height);
		double yval = getYBoxMinima(y);
		String ystr   = String.valueOf(yval);
		if(ystr.length() > 7)
		    ystr = ystr.substring(0, 7);
		graphic.drawString(ystr, y_axis_label_x_pos + label_height, y_axis_label_y_pos);
		graphic.drawLine(y_axis_x_pos, y_axis_label_y_pos, y_axis_x_pos + ticklen, y_axis_label_y_pos);
		//y_axis_label_y_pos += (box_height);
	    }
	    graphic.drawLine(y_axis_x_pos, 0, y_axis_x_pos, (n_y_boxes*box_height));
	    
	    //System.out.println("StackPlot: boxes drawn....");
	}

	private void drawClusters(Graphics graphic, int width, int height)
	{
	    // --------------------------------------------- /
	    // --------------------------------------------- /
	    // draw the clusters                             /
	    // --------------------------------------------- /

	    int xbox, ybox;
	    int glyph_offset = box_height / 4;

	    double height_scale_d = ((double) height_scale / 100.0);
	    
	    double twice_height_scale_d = height_scale_d * 2.0;
	    double half_height_scale_d  = height_scale_d * 0.5;

	    if((glyph_poly == null) || (glyph_poly_height != box_height))
	    {
		// generate (or re-generate at a new size) the glyphs
		glyph_poly = dplot.getScaledClusterGlyphs(box_height / 2);
		glyph_poly_height = box_height;
	    }

	    ExprData.ClusterIterator clit = edata.new ClusterIterator();
	    
	    ExprData.Cluster clust = clit.getFirstLeaf();
		
	    while(clust != null)
	    {
		if(clust.getShow() && clust.getIsSpot())
		{
		    Integer i;
		    int iv, gene_index;
		    int[] ce = clust.getElements();
		    
		    if(ce != null)
		    {
			graphic.setColor(clust.getColour());
			
			int last_x = 0;
			int last_y = 0;
			int vis_count = 0;
			int size;
			
			//System.out.print("cl " + cl + ": ");
			
			for(int gn=0; gn < ce.length; gn++)
			{
			    iv = ce[gn];
			    
			    //System.out.print(".." + iv );
			    
			    if(iv < edata.getNumSpots())
			    {
				//gene_index = edata.getIndexOf(iv);
				
				double xval = edata.eValue(x_meas, iv);
				double yval = edata.eValue(y_meas, iv);
				
				if((use_filter == false) || (!edata.filter(iv)))
				{
				    {
					
					xbox = getXBoxNoClip(xval);
					ybox = getYBoxNoClip(yval);
					
					if(((xbox  >= 0) && (ybox  >= 0)) && (xbox < n_x_boxes) && (ybox < n_y_boxes))
					    size = bin_contents[xbox][ybox].size();
					else
					    size = 0;
					
					xbox *= (box_width);
					
					//				    ybox *= box_height;
					
					// invert y axis so that 0 is at the bottom
					// (actually, 0 is at the box minus 2 box heights,
					//  one for the last box, and one for the x axis)
					//
					ybox = height - ((ybox+2) * box_height);
					
					// move to the top of the stack
					xbox += (int)((half_height_scale_d * (double)(size)));
					ybox -= (int)((twice_height_scale_d * (double)(size)));
					
					// move to the middle of the boxes
					xbox += (box_width/2);
					ybox += (box_height/2);
					
					if(vis_count == 0)
					{
					}
					else
					{
					    if(show_edges)
						graphic.drawLine(last_x, last_y, xbox, ybox);

					    //graphic.drawLine(last_x+1, last_y, xbox+1, ybox);
					    //graphic.drawLine(last_x, last_y+1, xbox, ybox+1);
					}
					
					if(spotIsVisible(xval, yval))
					{
					    int gly = clust.getGlyph();
					    Polygon poly = new Polygon(glyph_poly[gly].xpoints, 
								       glyph_poly[gly].ypoints,
								       glyph_poly[gly].npoints);
					    
					    poly.translate(xbox-glyph_offset, ybox-glyph_offset);
					    graphic.fillPolygon(poly);
					}
					
					vis_count++;
					last_x = xbox;
					last_y = ybox;
				    }
				}
			    }
			}
		    }
		    //System.out.println(" .");
		}
		clust = clit.getNextLeaf();
	    }

	}
	public void resizeComponent(Graphics graphic)
	{
	    
	}

	public void paintComponent(Graphics graphic)
	{
	    if(graphic == null)
		return;

	    if(box_hits == null)
		return;

	    if(bg_image == null)
	    {		
		drawBoxes(getWidth(), getHeight());
	    }
	    else
	    {
		// has the image resized since the boxes were last drawn?
		if((bg_image.getWidth(null) != getWidth()) || 
		   (bg_image.getHeight(null) != getHeight()))
		{
		    bg_image = null;

		    drawBoxes(getWidth(), getHeight());
		}
	    }
	    
	    if(bg_image == null)
		return;
	    
	    // paint the back buffer in
	    
	    graphic.drawImage(bg_image, 0, 0, null /* ImageObserver */ );
	    
	    if(show_clusters)
		drawClusters(graphic, getWidth(), getHeight());

	    deco_man.drawDecorations(graphic, getWidth(), getHeight());
	}
    }

    public final int EquallySpaced = 0;
    public final int EquallyFilled = 1;

    final int[] bin_sizes = { 5, 8, 10, 12, 15, 20 };
	
    private double x_meas_min, x_meas_max;
    private double y_meas_min, y_meas_max;

    private double bin_x_e_gap;
    private double bin_y_e_gap;

    private int[] x_saved_traversal = null;
    private int[] y_saved_traversal = null;
    private int x_saved_traversal_meas = -1;
    private int y_saved_traversal_meas = -1;
    
    private double[] x_bin_minima;
    private double[] y_bin_minima;

    private int[][]    box_hits;
    private Vector[][] bin_contents;

    private int height_scale;

    private int box_n_x;
    private int box_n_y;
    private int box_col_meas_n;    // n = (y * box_n_y) + x

    private boolean show_clusters = true;
    private boolean show_edges = true;
    private boolean show_axes = true;

    private boolean use_filter = true;
    private boolean auto_reset = true;
    
    private ExprData.NameTagSelection nt_sel;

    private JCheckBox al_jchkb;           // autolevelise
    private boolean auto_levelize = false;

    private int n_x_boxes, n_y_boxes;
    private int box_width, box_height;
    private int x_meas, y_meas;
    private int x_box_mode, y_box_mode;

    private int grid_mode = GRID_NONE;
    private int grid_r, grid_c;
    private int[] grid_col;
    private int[] grid_x;
    private int[] grid_y;
    private int grid_h, grid_w;
    
    private int label_height;
    private int max_labels_fit;

    private Polygon[] glyph_poly = null;
    private int glyph_poly_height;

    private maxdView mview;
    private ExprData edata;
    private DataPlot dplot;

    private boolean filter_alert = false;

    private JFrame frame = null;

    private JComboBox x_meas_jcb;
    private JComboBox y_meas_jcb;

    private MeasurementNameListener x_meas_al;
    private MeasurementNameListener y_meas_al;
     
    private JComboBox n_x_boxes_jcb;
    private JComboBox n_y_boxes_jcb;

    private JComboBox x_box_mode_jcb;
    private JComboBox y_box_mode_jcb;

    private JButton back_jb, fore_jb;

    private JLabel status_label;

    private Color background_col;
    private Color text_col;
 
    private Image bg_image = null;

    private StackDrawPanel panel;

    private AxisManager axis_man;
    private DecorationManager deco_man;

}
