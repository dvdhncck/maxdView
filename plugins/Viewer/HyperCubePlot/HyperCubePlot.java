import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import javax.swing.event.*;
import java.awt.dnd.*;

import java.awt.print.*;

//
//

public class HyperCubePlot implements ExprData.ExprDataObserver, Plugin
{
    final double Pi = 3.14159265358979323846;
	
    public final boolean debug = false;

    public HyperCubePlot(maxdView mview_)
    {
	mview = mview_;
	edata = mview.getExprData();
	dplot = mview.getDataPlot();
    }

    private void buildGUI()
    {

	axis_col = new Color[8];
	axis_col[0] = Color.red;
	axis_col[1] = Color.green;
	axis_col[2] = Color.blue;
	axis_col[3] = Color.white;
	axis_col[4] = Color.black;
	axis_col[5] = Color.yellow;
	axis_col[6] = Color.magenta;
	axis_col[7] = Color.cyan;

	proj_params = new double[3];

	proj_params[0] = mview.getDoubleProperty("HyperCubePlot.persp", 5.0);   // amount of perpective
	proj_params[1] = mview.getDoubleProperty("HyperCubePlot.scale", 0.2);   // zoom factor
	proj_params[2] = mview.getDoubleProperty("HyperCubePlot.eye_sep", 0.6); // eye sep
	
	use_filter     = mview.getBooleanProperty("HyperCubePlot.use_filter", true);
	show_spots     = mview.getBooleanProperty("HyperCubePlot.show_spots", true);
	show_grid      = mview.getBooleanProperty("HyperCubePlot.show_grid", false);
	show_origin    = mview.getBooleanProperty("HyperCubePlot.show_origin", false);
	show_glyphs    = mview.getBooleanProperty("HyperCubePlot.show_glyphs", false);
	show_edges     = mview.getBooleanProperty("HyperCubePlot.show_edges", false);
	show_axes      = mview.getBooleanProperty("HyperCubePlot.show_axes", true);
	show_axes_labels = mview.getBooleanProperty("HyperCubePlot.show_axes_labels", true);
	show_limits    = mview.getBooleanProperty("HyperCubePlot.show_limits", true);
	uniform_scale  = mview.getBooleanProperty("HyperCubePlot.uniform_scale", true);
	depth_sort     = mview.getBooleanProperty("HyperCubePlot.depth_sort", true);
	stereo         = mview.getBooleanProperty("HyperCubePlot.stereo", false);
	perftime       = mview.getBooleanProperty("HyperCubePlot.perftime", false);

	use_wireframe_for_move  = mview.getBooleanProperty("HyperCubePlot.use_wireframe_for_move", true);


	frame = new JFrame("HyperCube Projection Plot");

	mview.decorateFrame(frame);

	text_col       = mview.getTextColour();
	background_col = mview.getBackgroundColour();

	frame.getContentPane().setLayout(new BorderLayout());

	// ===== axis =====================================================================================

	axis_man = new AxisManager(mview);

	axis_man.addAxesListener( new AxisManager.AxesListener()
	    {
		public void axesChanged() 
		{
		    bg_image = null;
		    panel.repaint();
		}
	    });

	// ===== decorations ===============================================================================

	deco_man = new DecorationManager(mview, "HyperCubePlot");

	deco_man.addDecoListener( new DecorationManager.DecoListener()
	    {
		public void decosChanged() 
		{
		    bg_image = null;
		    panel.repaint();
		}
	    });

	// ===== top =====================================================================================

	JToolBar tool_bar = new JToolBar();
	tool_bar.setFloatable(false);

	{
	    int tcol = 0;

	    JPanel wrapper = new JPanel();
	    wrapper.setBorder(BorderFactory.createEtchedBorder());
	    GridBagLayout igridbag = new GridBagLayout();
	    wrapper.setLayout(igridbag);
	    GridBagConstraints c = null;


	    sel_jb = new JButton(" Sweep Select ");
	    sel_jb.setFont(mview.getSmallFont());
	    sel_jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			startSelection();
		    }
		});
	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.gridx = tcol++;
	    c.weightx = 1.0;
	    igridbag.setConstraints(sel_jb, c);
	    wrapper.add(sel_jb);

	    zoom_jb = new JButton(" Sweep Zoom ");
	    zoom_jb.setFont(mview.getSmallFont());
	    zoom_jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			startZoom();
		    }
		});
	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.gridx = tcol++;
	    c.weightx = 1.0;
	    igridbag.setConstraints(zoom_jb, c);
	    wrapper.add(zoom_jb);

	    JButton reset_jb = new JButton("Reset zoom");
	    reset_jb.setFont(mview.getSmallFont());
	    reset_jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			resetZoom();
		    }
		});
	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.gridx = tcol++;
	    c.weightx = 1.0;
	    igridbag.setConstraints(reset_jb, c);
	    wrapper.add(reset_jb);


	    // -------------------------------

	    final ButtonMenu bm = new ButtonMenu(mview, " Labels ");
	    JMenuItem jmi = null;
	    
	    jmi = new JMenuItem("Show all");
	    bm.add(0, jmi);

	    jmi = new JMenuItem("Clear all");
	    bm.add(1, jmi);

	    jmi = new JMenuItem("Label selection");
	    bm.add(2, jmi);

	    bm.addButtonMenuListener( new ButtonMenu.Listener()
		    {
			public void menuItemSelected( int id, JMenuItem comp )
			{
			    boolean is_sel = bm.isSelected( id );

			    // System.out.println( id + "=" + is_sel );

			    switch(id)
			    {
			    case 0:
				labelAllSpots();
				break;
			    case 1:
				clearAllSpotLabels();
				break;
			    case 2:
				labelSelection();
				break;
			    }

			    panel.repaint();
			}
		    });


	    c = new GridBagConstraints();
	    c.gridx = tcol++;
	    //c.anchor = GridBagConstraints.WEST;
	    //c.weightx = 1.0;
	    igridbag.setConstraints(bm, c);
	    wrapper.add(bm);
	
	    // -----------------

	    JLabel label = new JLabel(" use ");
	    c = new GridBagConstraints();
	    igridbag.setConstraints(label, c);
	    c.gridx = tcol++;
	    wrapper.add(label);
	    	    
	    nts = new NameTagSelector(mview);
	    nts.loadSelection("HyperCubePlot.name_tags");
	    nt_sel = nts.getNameTagSelection();
	    nts.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			nts.saveSelection("HyperCubePlot.name_tags");
			nt_sel = nts.getNameTagSelection();
			refresh( true );
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = tcol++;
	    igridbag.setConstraints(nts, c);
	    wrapper.add(nts);

	    

	    /*
	    JButton ljb = new JButton("Show All");
	    ljb.setEnabled(false);
	    ljb.setFont(mview.getSmallFont());
	    ljb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = 3;
	    igridbag.setConstraints(ljb, c);
	    wrapper.add(ljb);
	    
	    ljb = new JButton("Clear All");
	    ljb.setFont(mview.getSmallFont());
	    ljb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			clearAllSpotLabels();
			panel.repaint();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = 4;
	    igridbag.setConstraints(ljb, c);
	    wrapper.add(ljb);
	    */

	
	    tool_bar.add(wrapper);
	}
    
	    // --------


	{
	    JPanel wrapper = new JPanel();
	    wrapper.setBorder(BorderFactory.createEtchedBorder());
	    GridBagLayout igridbag = new GridBagLayout();
	    wrapper.setLayout(igridbag);
	    GridBagConstraints c = null;
	    
	    JButton dec_jb = new JButton("Decorations");
	    dec_jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			deco_man.startEditor();
		    }
		});
	    
	    wrapper.add(dec_jb);
	    
	    JButton prn_jb = new JButton("Print");
	    prn_jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			new PrintManager( mview, panel, panel ).openPrintDialog();
		    }
		});
	    
	    wrapper.add(prn_jb);

	    tool_bar.add(wrapper);
	}

	frame.getContentPane().add(tool_bar, BorderLayout.NORTH);


	// ===== panel ====================================================================================

	panel = new DrawPanel();

	final int w = mview.getIntProperty("HyperCubePlot.width",  450);

	final int h = mview.getIntProperty("HyperCubePlot.height", 350);
	
	final Dimension dim = new Dimension(w, h);

	panel.setPreferredSize(dim);


	panel.setDropAction(new DragAndDropPanel.DropAction()
	    {
		public void dropped(DragAndDropEntity dnde)
		{
		    try
		    {
			int[] sids = dnde.getSpotIds();
			if(sids != null)
			{
			    for(int s=0;s < sids.length; s++)
				toggleSpotLabel(sids[s]);
			    panel.repaint();
			}
		    }
		    catch(DragAndDropEntity.WrongEntityException wee)
		    {
			try
			{
			    ExprData.Cluster clu = dnde.getCluster();
			    if(clu != null)
			    {
				setSpotLabel(clu);
				panel.repaint();
			    }
			}
			catch(DragAndDropEntity.WrongEntityException wee2)
			{
			    
			}
			
		    }
		}
	    });
	panel.setDragAction(new DragAndDropPanel.DragAction()
	    {
		public DragAndDropEntity getEntity(DragGestureEvent event)
		{
		    if(root_spot_picker != null)
		    {
			Point pt = event.getDragOrigin();
			int sid = root_spot_picker.findSpot(pt.x, pt.y, 5);
			if(sid >= 0)
			{
			    DragAndDropEntity.createSpotNameEntity(sid);
			}
		    }
		    return null;
		}
	    });


	//frame.getContentPane().add(panel);

	// ===== measurement picker =====================================================================

	// JPanel meas_pick_panel = new JPanel();
	
	meas_list = new DragAndDropList();
	
	// meas_list.setPreferredSize(new Dimension(75, 400));

	populateListWithMeasurements(meas_list);
	
	// meas_list.setSelectionInterval( 0, meas_ids.length - 1);
	
	meas_list.addListSelectionListener(new ListSelectionListener() 
	    {
		public void valueChanged(ListSelectionEvent e)
		{
		    listSelectionHasChanged();
		}
	    });
	
	JScrollPane jsp = new JScrollPane(meas_list);
	    

	l_jsplp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	l_jsplp.setLeftComponent(jsp);
	l_jsplp.setRightComponent(panel);

	
	// ===== knobs ==================================================================================

	{
	    JPanel wrapper = new JPanel();
	    wrapper.setBorder(BorderFactory.createEtchedBorder());
	    GridBagLayout igridbag = new GridBagLayout();
	    wrapper.setLayout(igridbag);
	    
	    rotate_panel = new KnobPanel( mview.getImageDirectory(), true );
	    rotate_panel.setMinMax(0,360);
	    //rotate_panel.setPreferredSize(new Dimension(75, 250));
	    JScrollPane rp_jsp = new JScrollPane(rotate_panel);
	    GridBagConstraints c = new GridBagConstraints();
	    c.fill = GridBagConstraints.BOTH;
	    c.weightx = 1.0;
	    c.weighty = 7.0;
	    igridbag.setConstraints(rp_jsp, c);
	    wrapper.add(rp_jsp);
	    
	    proj_params_panel = new KnobPanel( mview.getImageDirectory(), false );
	    //proj_params_panel.setPreferredSize(new Dimension(75, 150));

	    final String[] proj_params_labels = { "Persp.", "Scale", "Eye Sep." };
	    proj_params_panel.setLabels( proj_params_labels );
	    JScrollPane pp_jsp = new JScrollPane(proj_params_panel);
	    c = new GridBagConstraints();
	    c.gridy = 1;
	    c.weightx = 1.0;
	    c.weighty = 3.0;
	    c.fill = GridBagConstraints.BOTH;
	    igridbag.setConstraints(pp_jsp, c);
	    wrapper.add(pp_jsp);

	    // =====
	    
	    listSelectionHasChanged();
	    
	    bounds = makeHCube( n_meas );
	    axes   = makeAxes( n_meas );

	    rotate_panel.update( axis_rot_angle );
	    proj_params_panel.update( proj_params );

	    rotate_panel.addKnobListener( new KnobPanel.KnobListener()
		{
		    public void update(int knob, double value)
		    {
			//System.out.println("knob  " + knob + " update done, value = " + value);
			refresh( true );
		    }
		});

	    rotate_panel.addKnobMotionListener( new KnobPanel.KnobMotionListener()
		{
		    public void update(int knob, double value)
		    {
			//System.out.println("knob " + knob + " = " + value);
			refresh( false );
		    }
		});

	    proj_params_panel.addKnobListener( new KnobPanel.KnobListener()
		{
		    public void update(int knob, double value)
		    {
			//System.out.println("knob  " + knob + " update done, value = " + value);
			refresh( true );
		    }
		});
	    proj_params_panel.addKnobMotionListener( new KnobPanel.KnobMotionListener()
		{
		    public void update(int knob, double value)
		    {
			//System.out.println("knob " + knob + " = " + value);
			refresh( false );
		    }
		});

	    // JScrollPane wrap_jsp = new JScrollPane(wrapper);
	    
	    r_jsplp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	    r_jsplp.setLeftComponent(l_jsplp);
	    r_jsplp.setRightComponent(wrapper);

	    frame.getContentPane().add(r_jsplp);
	

	    // frame.getContentPane().add(wrap_jsp, BorderLayout.EAST);
	}


	
	// ===== bottom ==================================================================================

	JToolBar bottom_tool_bar = new JToolBar();
	bottom_tool_bar.setFloatable(false);

	{
	    bottom_tool_bar.addSeparator();
	    GridBagLayout gridbag = new GridBagLayout();
	    bottom_tool_bar.setLayout(gridbag);
	    
	    int col = 0;

	    {
		JPanel wrapper = new JPanel();
		wrapper.setBorder(BorderFactory.createEtchedBorder());
		GridBagLayout igridbag = new GridBagLayout();
		wrapper.setLayout(igridbag);
	    
		JLabel label = new JLabel(" colour ");
		GridBagConstraints c = new GridBagConstraints();
		//c.gridx = 0;
		//c.gridy = 0;
		//c.gridheight = 2;
		igridbag.setConstraints(label, c);
		wrapper.add(label);

		fixed_col_jb = new JButton();
		Insets ins = new Insets(3,3,3,3);
		fixed_col_jb.setMargin(ins);
		// fixed_col_jb.addActionListener(); 
		c = new GridBagConstraints();
		c.gridx = 1;
		//c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		igridbag.setConstraints(fixed_col_jb, c);
		wrapper.add(fixed_col_jb);
	  
		col_jcb = new JComboBox();

		populateComboBoxWithMeasurements(col_jcb);
		col_jcb.addActionListener(new ActionListener()
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    colour_meas_index = -1;
			    
			    String new_name = (String)col_jcb.getSelectedItem();
			    if(new_name != "[fixed]")
				colour_meas_index = edata.getMeasurementFromName(new_name);
			    
			    refresh( true );
			}
		    });

		c = new GridBagConstraints();
		c.gridx = 2;
		//c.gridy = 1;
		//c.gridwidth  = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		igridbag.setConstraints(col_jcb, c);
		wrapper.add(col_jcb);

		c = new GridBagConstraints();
		c.gridx = col++;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		//c.weightx = c.weighty = 1.0;
		c.gridheight = 2;
		c.fill = GridBagConstraints.VERTICAL;
		bottom_tool_bar.add(wrapper);
		gridbag.setConstraints(wrapper, c);
	    }

	    // ==================================

	    {
		JPanel wrapper = new JPanel();
		wrapper.setBorder(BorderFactory.createEtchedBorder());
		GridBagLayout igridbag = new GridBagLayout();
		wrapper.setLayout(igridbag);
	    
		JLabel label = new JLabel(" size ");
		GridBagConstraints c = new GridBagConstraints();
		//c.gridx = 0;
		//c.gridy = 0;
		//c.gridheight = 2;
		igridbag.setConstraints(label, c);
		wrapper.add(label);


		final String[] spot_size_opts_strs = { "Tiny", "Small", "Medium", "Large" };

		spot_size_jcb = new JComboBox( spot_size_opts_strs );
		spot_size_jcb.setSelectedIndex(mview.getIntProperty("HyperCubePlot.spot_size", 0));
		spot_size_jcb.addActionListener(new ActionListener()
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    refresh( true );
			}
		    });
		c = new GridBagConstraints();
		c.gridx = 1;
		///c.gridy = 1;
		//c.gridheight = 2;
		igridbag.setConstraints(spot_size_jcb, c);
		wrapper.add(spot_size_jcb);

		c = new GridBagConstraints();
		c.gridx = col++;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		//c.weightx = c.weighty = 1.0;
		c.gridheight = 2;
		c.fill = GridBagConstraints.VERTICAL;
		bottom_tool_bar.add(wrapper);
		gridbag.setConstraints(wrapper, c);
	    }

	    // ========================================================

	    JPanel cwrapper = new JPanel();
	    cwrapper.setBorder(BorderFactory.createEtchedBorder());
	    GridBagLayout cwgridbag = new GridBagLayout();
	    cwrapper.setLayout(cwgridbag);

	    int ccol = 0;

	    {
		final ButtonMenu bm = new ButtonMenu(mview, " Display ");
		JCheckBoxMenuItem jmi = null;


		jmi = new JCheckBoxMenuItem("Spots");
		jmi.setSelected(show_spots);
		bm.add(0, jmi);

		jmi = new JCheckBoxMenuItem("Cluster glyphs");
		jmi.setSelected(show_glyphs);
		bm.add(1, jmi);

		jmi = new JCheckBoxMenuItem("Cluster edges");
		jmi.setSelected(show_edges);
		bm.add(2, jmi);
		
		jmi = new JCheckBoxMenuItem("Axes");
		jmi.setSelected(show_axes);
		bm.add(3, jmi);

		jmi = new JCheckBoxMenuItem("Axes labels");
		jmi.setSelected(show_axes_labels);
		bm.add(4, jmi);

		jmi = new JCheckBoxMenuItem("Limits");
		jmi.setSelected(show_limits);
		bm.add(5, jmi);

		jmi = new JCheckBoxMenuItem("Grid points");
		jmi.setSelected(show_grid);
		bm.add(6, jmi);

		jmi = new JCheckBoxMenuItem("Origin");
		jmi.setSelected(show_origin);
		bm.add(7, jmi);
	
		bm.addButtonMenuListener( new ButtonMenu.Listener()
		    {
			public void menuItemSelected( int id, JMenuItem comp )
			{
			    boolean is_sel = bm.isSelected( id );

			    // System.out.println( id + "=" + is_sel );

			    switch(id)
			    {
			    case 0:
				show_spots = is_sel;
				break;
			    case 1:
				show_glyphs = is_sel;
				break;
			    case 2:
				show_edges = is_sel;
				break;
			    case 3:
				show_axes = is_sel;
				break;
			    case 4:
				show_axes_labels = is_sel;
				break;
			    case 5:
				show_limits = is_sel;
				break;
			    case 6:
				show_grid = is_sel;
				break;
			    case 7:
				show_origin = is_sel;
				break;
			    }
			    
			    refresh( true );

			}
		    });

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = ccol;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = c.weighty = 1.0;
		cwgridbag.setConstraints(bm, c);
		cwrapper.add(bm);
	    }

	    ccol++;
	    
	    {
		final ButtonMenu bm = new ButtonMenu(mview, " Options ");
		JCheckBoxMenuItem jmi = null;

		jmi = new JCheckBoxMenuItem("Uniform scale");
		jmi.setSelected(uniform_scale);
		bm.add(1, jmi);

		jmi = new JCheckBoxMenuItem("Use wireframe");
		jmi.setSelected(use_wireframe_for_move);
		bm.add(2, jmi);

		jmi = new JCheckBoxMenuItem("Stereo");
		jmi.setSelected(stereo);
		bm.add(3, jmi);

		jmi = new JCheckBoxMenuItem("Depth sort");
		jmi.setSelected(depth_sort);
		bm.add(4, jmi);

		jmi = new JCheckBoxMenuItem("Timing");
		jmi.setSelected(perftime);
		bm.add(5, jmi);

		bm.addButtonMenuListener( new ButtonMenu.Listener()
		    {
			public void menuItemSelected( int id, JMenuItem comp )
			{
			    boolean is_sel = bm.isSelected( id );

			    // System.out.println( id + "=" + is_sel );

			    switch(id)
			    {
			    case 1:
				uniform_scale = is_sel;
				break;
			    case 2:
				use_wireframe_for_move = is_sel;
				break;
			    case 3:
				stereo = is_sel;
				break;
			    case 4:
				depth_sort = is_sel;
				break;
			    case 5:
				perftime = is_sel;
				break;
			    }

			    refresh( true );

			}
		    });


		GridBagConstraints c = new GridBagConstraints();
		c.gridx = ccol;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = c.weighty = 1.0;
		cwgridbag.setConstraints(bm, c);
		cwrapper.add(bm);
		
	    }

	    ccol++;
	    

	    {
		JCheckBox jcb= new JCheckBox("Apply filter");
		cwrapper.add(jcb);
		jcb.setSelected(use_filter);
		jcb.setHorizontalTextPosition(AbstractButton.RIGHT);
		jcb.addActionListener(new ActionListener()
				      {
					  public void actionPerformed(ActionEvent e) 
					  {
					      JCheckBox source = (JCheckBox) e.getSource();
					      use_filter = source.isSelected();
					      refresh( true );
					  }
				      });
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = ccol;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = c.weighty = 1.0;
		cwgridbag.setConstraints(jcb, c);
	    }

	    // ===================

	    {
		JScrollPane cwjsp = new JScrollPane(cwrapper);

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = col++;
		c.gridy = 0;
		c.gridheight = 2;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 8.0;
		c.weighty = 2.0;
		c.fill = GridBagConstraints.BOTH;
		gridbag.setConstraints(cwjsp, c);
		bottom_tool_bar.add(cwjsp);
	    }

	    // ==================================================

	    {
		final JButton jb = new JButton("Help");
		bottom_tool_bar.add(jb);
		jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					 {
					     mview.getPluginHelpTopic("HyperCubePlot", "HyperCubePlot");
					 }
				     });
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = col;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		gridbag.setConstraints(jb, c);
	    }
	    {
		final JButton jb = new JButton("Close");
		bottom_tool_bar.add(jb);
		jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					 {
					     cleanUp();
					 }
		    });
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = col+1;
		//c.gridy = 1;
		c.anchor = GridBagConstraints.WEST;
	        c.weightx = c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		gridbag.setConstraints(jb, c);
	    }
	    
	}

	frame.getContentPane().add(bottom_tool_bar, BorderLayout.SOUTH);
	// panel.setPreferredSize(new Dimension(400, 400));




	/*
	Timer timer = new Timer(750, 
				new ActionListener() 
				{
				    public void actionPerformed(ActionEvent evt) 
				    {
					if(filter_alert == true) 
					{ 
					    if(edata.filterIsOn())
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
	*/
    }

    public void cleanUp()
    {
	mview.putIntProperty("HyperCubePlot.width", panel.getWidth());
	mview.putIntProperty("HyperCubePlot.height", panel.getHeight());
	
	if(l_jsplp != null)
	{
	    mview.putIntProperty("HyperCubePlot.lsplit", l_jsplp.getDividerLocation());
	    mview.putIntProperty("HyperCubePlot.rsplit", r_jsplp.getDividerLocation());
	}
	
	mview.putBooleanProperty("HyperCubePlot.show_limits",      show_limits);
	mview.putBooleanProperty("HyperCubePlot.show_axes",        show_axes);
	mview.putBooleanProperty("HyperCubePlot.show_axes_labels", show_axes_labels);
	
	mview.putBooleanProperty("HyperCubePlot.uniform_scale",   uniform_scale);

	mview.putBooleanProperty("HyperCubePlot.use_filter",   use_filter);
	mview.putBooleanProperty("HyperCubePlot.show_spots",   show_spots);

	mview.putBooleanProperty("HyperCubePlot.show_grid",    show_grid);
	mview.putBooleanProperty("HyperCubePlot.show_origin",  show_origin);

	mview.putBooleanProperty("HyperCubePlot.show_glyphs",  show_glyphs);
	mview.putBooleanProperty("HyperCubePlot.show_edges",   show_edges);

	mview.putBooleanProperty("HyperCubePlot.depth_sort",  depth_sort);
	mview.putBooleanProperty("HyperCubePlot.stereo",  stereo);
	
	mview.putBooleanProperty("HyperCubePlot.perftime",  perftime);

	mview.putDoubleProperty("HyperCubePlot.persp",   proj_params[0]);
	mview.putDoubleProperty("HyperCubePlot.scale",   proj_params[1]);
	mview.putDoubleProperty("HyperCubePlot.eye_sep", proj_params[2]);
	
	mview.putIntProperty("HyperCubePlot.spot_size", spot_size_jcb.getSelectedIndex());

	mview.putBooleanProperty("HyperCubePlot.use_wireframe_for_move", use_wireframe_for_move);

	frame.setVisible(false);
	edata.removeObserver(this);

	// System.out.println("removing knob panels...");

	// using the finaliser did not work which suggests that
	// a reference to the panel is surviving somehow.
	
	// instead forced to kill things explictly:

	rotate_panel.shutdown();      // kill the thread....
	proj_params_panel.shutdown(); // kill the thread....

	System.gc();
    }

    // ============= colour measurement combo box ===============================


    private void populateComboBoxWithMeasurements(JComboBox jcb)
    {
	String cur_sel = (String) jcb.getSelectedItem();

	jcb.removeAllItems();

	jcb.addItem("[fixed]");
	
	if(edata.getNumMeasurements() > 0)
	{
	    for(int m=0;m<edata.getNumMeasurements();m++)
	    {
		int mi = edata.getMeasurementAtIndex(m);
		if(edata.getMeasurementShow(mi) == true)
		{
		    jcb.addItem(edata.getMeasurementName(mi));
		}
	    }
	}

	if(cur_sel != null)
	    jcb.setSelectedItem(cur_sel);
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

    private void listSelectionHasChanged()
    {
	final int max_dims = 6;

	int[] sel_inds = meas_list.getSelectedIndices();

	int allowed_dims = sel_inds.length;
	if(allowed_dims > max_dims)
	    allowed_dims = max_dims;

	meas_ids = new int[ allowed_dims ];

	meas_min = new double[ allowed_dims ];
	meas_max = new double[ allowed_dims ];

	int mp = 0;
	
	for(int m=0; m < edata.getNumMeasurements(); m++)
	{
	    int mi = edata.getMeasurementAtIndex(m);
	    if(meas_list.isSelectedIndex( m ) && (mp < allowed_dims))
	    {
		meas_ids[mp] = mi;

		meas_min[mp] = edata.getMeasurementMinEValue( mi );
		meas_max[mp] = edata.getMeasurementMaxEValue( mi );

		mp++;

		//System.out.println( edata.getMeasurementName( mi ) );
	    }
	}

	final int n_d = meas_ids.length;

	axis_rot_angle = new double[ n_d ];

	for(int a=0; a < n_d; a++)
	    axis_rot_angle[a] = 0.5;

	// updateProfiles();
	rotate_panel.update( axis_rot_angle );
	
	bounds = makeHCube( n_d );
	axes   = makeAxes( n_d );

	if(bounds != null)
	{
	    // bounds.dump();
	}

	panel.repaint();
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
	buildGUI();

	initialise();
	
	frame.addWindowListener(new WindowAdapter() 
	    {
		public void windowClosing(WindowEvent e)
		{
		    cleanUp();
		}
	    });
	
	frame.pack();
	frame.setVisible(true);

	l_jsplp.setDividerLocation(mview.getIntProperty("HyperCubePlot.lsplit", 100));
	r_jsplp.setDividerLocation(mview.getIntProperty("HyperCubePlot.rsplit", 220));

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
	PluginInfo pinf = new PluginInfo("HyperCube Plot", "viewer", 
					 "Projects multi-dimensional data into a N-cube", 
					 "", 
					 1, 0, 0);
	return pinf;
    }

    public void runCommand(String name, String[] args, CommandSignal done) 
    { 
	if(name.equals("start"))
	{
	    startPlugin();
	}
	if(name.equals("stop"))
	{
	    cleanUp();
	}
	if(done != null)
	    done.signal();
    } 

    public PluginCommand[] getPluginCommands()
    {
	PluginCommand[] com = new PluginCommand[2];
	com[0] = new PluginCommand("start", null);
	com[1] = new PluginCommand("stop", null);
	return com;
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
	    panel.repaint();
	    break;
	case ExprData.SizeChanged:
	case ExprData.ValuesChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	case ExprData.RangeChanged:
	case ExprData.VisibilityChanged:
	    bg_image = null;
	    panel.repaint();
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
	    bg_image = null;
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
	case ExprData.NameChanged:
	case ExprData.OrderChanged:
	    populateComboBoxWithMeasurements(col_jcb);
	    populateListWithMeasurements(meas_list);
	    panel.repaint();
	    break;
	}
    }
    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
	text_col       = mview.getTextColour();
	background_col = mview.getBackgroundColour();

	bg_image = null;
	panel.repaint();
    }


    public void initialise()
    {
	
	edata = mview.getExprData();
	dplot = mview.getDataPlot();
    }

    // ======================================================================================
    // zooming

    private void startZoom()
    {
	zooming = true;

	sel_start = new java.awt.Point();
	sel_end   = new java.awt.Point();

	frame.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
	zoom_jb.setEnabled(false);
    }

    private void endZoom()
    {
	zooming = false;

	int[] result = getSpotsInRubberBox();

	if(result != null)
	{
	    //System.out.println( result.length + " spots in sweep");

	    // get the min/max for the chosen spots
	    
	    for(int m=0; m < meas_ids.length; m++)
	    {
		meas_min[m] = Double.MAX_VALUE;
		meas_max[m] = -Double.MAX_VALUE;
	    }

	    for(int s=0; s < result.length; s++)
	    {
		for(int m=0; m < meas_ids.length; m++)
		{
		    double e = edata.eValue( meas_ids[m], result[s] );

		    if(e < meas_min[m])
			meas_min[m] = e;
		    if(e > meas_max[m])
			meas_max[m] = e;
		}
	    }
	    
	    //for(int m=0; m < meas_ids.length; m++)
	    //{
	    //System.out.println( meas_min[m] + "\t" + meas_max[m]);
	    //}
	    
	}
	
	frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	zoom_jb.setEnabled(true);

	panel.repaint();
    }
    
    private void resetZoom()
    {
	for(int m=0; m < meas_ids.length; m++)
	{
	    int mi = meas_ids[m];
	    
	    meas_min[m] = edata.getMeasurementMinEValue( mi );
	    meas_max[m] = edata.getMeasurementMaxEValue( mi );
	}
	panel.repaint();
    }

    // ======================================================================================
    // selecting

    private void startSelection()
    {
	selecting = true;

	sel_start = new java.awt.Point();
	sel_end   = new java.awt.Point();

	frame.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
	sel_jb.setEnabled(false);
    }

    private void endSelection()
    {
	selecting = false;

	int[] result = getSpotsInRubberBox();

	if(result != null)
	{
	    edata.setSpotSelection( result );
	}

	frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	sel_jb.setEnabled(true);
    }

    // ====================================

    private int[] getSpotsInRubberBox()
    {
	final int ns = edata.getNumSpots();
	Primitive p = null;
	
	// java.awt.Point sel_end = new java.awt.Point( sel_start.x+sel_size.x, sel_start.y+sel_size.y );
	
	int[] poss_sels = new int[ns];
	int nsel = 0;
	for(int s=0; s < ns; s++)
	{
	    p = left_depth_sorter.getSpot( s );
	    if(p != null)
	    {
		if((p.sx >= sel_start.x) && (p.sx <= sel_end.x) && (p.sy >= sel_start.y) && (p.sy <= sel_end.y))
		{
		    poss_sels[nsel++] = s;
		}
	    }
	}

	// rebuild the array..

	if(nsel > 0)
	{
	    int[] real_sels = new int[nsel];
	    for(int s=0; s < nsel; s++)
		real_sels[s] = poss_sels[s];
	    
	    return real_sels;
	}
	else
	    return null;
    }

    // ======================================================================================
    // 
    // rotate a point
    //
    Point2D.Double rotatePoint(Point2D.Double p, double angle)
    {
	double rx, ry;

	final double ang = angle * 0.017452;   // deg -> rad

	final double c_ang = Math.cos(ang);
	final double s_ang = Math.sin(ang);

	rx  = (p.x * c_ang) - (p.y * s_ang);
	ry  = (p.x * s_ang) + (p.y * c_ang); 
	
	//System.out.println(p.x + "," + p.y + " -> " + rx + "," + ry);

	return new Point2D.Double( rx, ry );
    }

    // generates an angle (0..359) between the 2 points
    //   with 0 degs at 12 o'clock, 90 degs at 3 o'clock, 180 degs at 6 o'clock and so on...
    //
    double angleAt(Point p, Point o)
    {
	Point vec = new Point(p.x - o.x, p.y - o.y);
	if(vec.x == 0)
	{
	    return (vec.y > 0) ? 180 : 0;
	}
	else
	{
	    double o_over_a = (double) vec.y / (double) vec.x;
	    double ang = (Math.atan(o_over_a) * 57.29578); // rad -> deg
	    return (vec.x >= 0) ? (ang + 90.0) : (ang + 270.0);
	}
    }
    
    // ======================================================================================

    private double[][] makeRotMatrix(int dim, double[] angles)
    {
	// ===============
	//
	// taken from
	//
	//
	// http://www.flowerfire.com/ferrar/java/hypercuber/HyperCuber.html
	//
	// ===============

	//  Here is the algorithm used to build the matrix from scratch.  This algorith was
	//  derived by examining correct rotation matrices up to size 7x7 (as derived by
	//  Mathematica) and looking for patterns.  The patterns were very obvious, but since
	//  the analysis was not mathematical, this algorithm has not yet been formally PROVEN.
	//  See Mathematica notebook RotMatrix Derivation for more information (available from Greg Ferrar).
	//  
	//  M[i,j] is the (i,j)th element of the rotation matrix, and rows and columns both
	//  start at 0.  s[i] and c[i] are the ith
	//  element of the sines and cosines matrices, respectively.  s[0] is defined to be 1.0
	//  (important for proper execution of step 5).
	//
	//  1. prod = 1; loop c from dim-1 to 1 step -1 { M[0,c] = -prod*s[c-1]; prod *= c[c-1]; }
	//  2. M[0, 0] = prod;
	//  3. loop r from 1 to dim-1 { M[r, r] = c[r-1] }
	//  4. loop r from 1 to dim-2 { loop c from r+1 to dim-1 { M[r, c] = 0 } }
	//  5. loop c from 0 to dim-2 { if (c==0) then prod = 1 else prod = -s[c-1];
	//                              loop r from c+1 to dim-1 { M[r, c] = prod*s[r-1]; prod *= c[r-1] } }
	//
	//  The matrix below is a sample 7x7 rotation matrix.  The entries indicate which element
	//  is generated by which step of the above algorithm.  For instance, element (0,0) is 2,
	//  indicating that the element in the 0th row and 0th column is generated by step 2 of
	//  the algorithm.
	//
	// column = 0           column = dim-1
	//     |                       |
	//
	//  [  2   1   1   1   1   1   1  ]   row = 0
	//  [  5   3   4   4   4   4   4  ]
	//  [  5   5   3   4   4   4   4  ]
	//  [  5   5   5   3   4   4   4  ]
	//  [  5   5   5   5   3   4   4  ]
	//  [  5   5   5   5   5   3   4  ]
	//  [  5   5   5   5   5   0   3  ]   row = dim-1
	//
	

	final double[][] matrix = new double[dim][dim];

	final double ANGLE_OFFSET = .0; // Pi/2000;
	
	double prod = 1.0;
	int r, c;
	for (c = dim-1; c >= 1; c--)			//  STEP 1
	{
	    matrix[0][c] = -prod*Math.sin(angles[c-1] + ANGLE_OFFSET);
	    prod *= Math.cos(angles[c-1] + ANGLE_OFFSET);
	}
	
	matrix[0][0] = prod;							//  STEP 2
	
	for (r = 1; r <= dim-1; r++)			//  STEP 3
	    matrix[r][r] = Math.cos(angles[r-1] + ANGLE_OFFSET);
	
	for (r = 1; r <= dim-2; r++)			//  STEP 4
	    for (c = r+1; c <= dim-1; c++)
		matrix[r][c] = 0;
	
	for (c = 0; c <= dim-2; c++)			//  STEP 5
	{
	    if (c == 0)
		prod = 1.0;
	    else
		prod = -Math.sin(angles[c-1] + ANGLE_OFFSET);
	    for (r = c+1; r<= dim-1; r++)
	    {
		matrix[r][c] = prod * Math.sin(angles[r-1] + ANGLE_OFFSET);
		prod *= Math.cos(angles[r-1] + ANGLE_OFFSET);
	    }
	}

	return matrix;
    }
    
    public final void vecTranslate(final int space, final double[] in, final double delta, final double[] out)
    {
	for(int p=0; p < space; p++)
	    out[p] = in[p] + delta;
    }

    public final double[] vecMatMul( final int space, final double[] in, final double[][] matrix )
    {
	final double[] out = new double[space];

	for(int row=0; row < space; row++)
	{
	    double sum = 0;
	    
	    for (int column = 0; column < space; column++)
	    {
		sum += matrix[row][column] * in[column];
	    }
	    
	    out[row] = sum;
	}
	return out;
    }

    // projects 'in' from 'dim' space into 'out' in 'dim-1' space
    //
    // leaves the distance in the final element of the 'out' array
    //
    public final double[] projectPoint( final int dim, final double[] in )
    {
	final double[] out = new double[dim];
	
	double t = (persp_factor == 0) ? 1.0 : (persp_factor / (persp_factor - in[0]));
	
	if (t<0) 
	    t = -t;
	
	for (int p=1; p < dim; p++)
	    out[p-1] = in[p] * t;

	out[dim-1] = in[0];    // this was the distance used to calculate perspective

	return out;
    }

    // ======================================================================================
 
    public void refresh(boolean use_wireframe)
    {
	wireframe = !use_wireframe;

	// panel.drawAxes(mouse_tracking, panel.getWidth(), panel.getHeight());
	panel.repaint();
    }
	

    // ======================================================================================
    // 
    //  DrawPanel does most of the work
    //
    // ======================================================================================

    public class DrawPanel extends DragAndDropPanel implements MouseListener, MouseMotionListener, Printable
    {
	private Point last_pt = null;

	public DrawPanel()
	{
	    super(true);
	    addMouseListener(this);
	    addMouseMotionListener(this);
	}


	Point2D drag_start;
	boolean dragging = false;

	public void mouseMoved(MouseEvent e) 
	{
	    if(root_spot_picker != null)
	    {
		int sid = root_spot_picker.findSpot(e.getX(), e.getY(), 5);
		if(sid >= 0)
		{
		    nt_sel = nts.getNameTagSelection();

		    String str = nt_sel.getNameTag(sid);
		   
		    setToolTipText(str);
		    
		    // System.out.println("nearest sid=" + sid);
		}
	    }

	}

	public void mouseDragged(MouseEvent e) 
	{
	    if(selecting || zooming)
	    {
		drawRubberBox();

		sel_end.x = e.getX();
		sel_end.y = e.getY();

		drawRubberBox();

		return;
	    }

	    if(e.isShiftDown())
	    {
		double delta = 0.001 / proj_params[1];
		trans_x += ((double) (e.getX() - drag_start.getX()) * delta);
		trans_y += ((double) (e.getY() - drag_start.getY()) * delta);
	    }
	    else
	    {
		double delta = 0.0005 / proj_params[1];

		int knob_off = e.isControlDown() ? 2 : 0;

		int k = (knob_off+0);
		if(n_meas > k)
		{
		    axis_rot_angle[k] += ((double) (e.getX() - drag_start.getX()) * delta);
		    
		    while(axis_rot_angle[k] < .0)
			axis_rot_angle[k] += 1.0;
		    while(axis_rot_angle[k] > .0)
			axis_rot_angle[k] -= 1.0;
		}

		k = (knob_off+1);
		if(n_meas > k)
		{
		    axis_rot_angle[k] += ((double) (e.getY() - drag_start.getY()) * delta);

		    while(axis_rot_angle[k] < .0)
			axis_rot_angle[k] += 1.0;
		    while(axis_rot_angle[k] > .0)
			axis_rot_angle[k] -= 1.0;
		}

		rotate_panel.repaint();
	    }
	    drag_start = new Point(e.getX(), e.getY());
	    
	    // System.out.println(trans_x + "," + trans_y + " x " + zoom);

	    if(use_wireframe_for_move)
	    {
		dragging = true;
		
		refresh( false );
	    }
	    else
	    {
		refresh( true );
	    }
	} 

	
	public void mousePressed(MouseEvent e) 
	{
	    if(selecting || zooming)
	    {
		sel_end.x = sel_start.x = e.getX();
		sel_end.y = sel_start.y = e.getY();
		
		drawRubberBox();

		return;
	    }


	    dragging = false;
	    //System.out.println("mouse press");
	    drag_start = new Point(e.getX(), e.getY());
	}
	
	public void mouseReleased(MouseEvent e) 
	{
	    if(selecting || zooming)
	    {
		drawRubberBox();
		
		if(selecting)
		    endSelection();
		if(zooming)
		    endZoom();

		return;
	    }

	    dragging = false;

	    refresh( true );
	}
	
	public void mouseEntered(MouseEvent e) 
	{
	}

	public void mouseExited(MouseEvent e) 
	{
	}

	public void mouseClicked(MouseEvent e) 
	{
	    // System.out.println("click");
	    if(root_spot_picker != null)
	    {
		int sid = root_spot_picker.findSpot(e.getX(), e.getY(), 5);
		if(sid >= 0)
		{
		    toggleSpotLabel(sid);
		    repaint();
		}
	    }

	}

	// ===== transformation for display ======================

	public void setupTransformationAndAxes(int pw, int ph)
	{
	    n_spots = edata.getNumSpots();
	    n_meas  = meas_ids.length;

	    //axis_vec_x = new double[n_meas];
	    //axis_vec_y = new double[n_meas];
	    
	    e_scale = new double[n_meas];
	    e_trans = new double[n_meas];

	    a_scale = new double[n_meas];

	    int sw = stereo ? (pw/2) : pw;    // 'screen' width

	    if(stereo)
		translate_right = sw;

	    to_mid_x = (sw/2);
	    to_mid_y = (ph/2);

	    axis_scale = (sw < ph) ? (double)sw / 2. : (double)ph / 2.;

	    axis_scale   *= (proj_params[1] * 3.0);      // the user controlled zoom factor in 'proj_params[1]'
	    persp_factor = (1.0-proj_params[0]) * 10.0;  // the user controlled perspective factor
	    eye_sep = (proj_params[2] - 0.5) * 0.5;      // the user controlled eye separation
	    half_eye_sep = eye_sep * 0.5;

	    for(int m = 0; m < n_meas; m++)
	    {
		final int m_id = meas_ids[m];
		
		double range = (meas_max[m] - meas_min[m]);
		e_scale[m] = (range == .0) ? .0 : (1.0 / range);
		e_trans[m] = -meas_min[m];
	    }
	    
	    if(uniform_scale)
	    {
		double min_scale = Double.MAX_VALUE;

		for(int m = 0; m < n_meas; m++)
		    if(e_scale[m] < min_scale)
			min_scale = e_scale[m];

		for(int m = 0; m < n_meas; m++)
		    e_scale[m] = min_scale;
	    }

	    //length_normalise = (n_meas == 0) ? 1.0 : 1.0 /  max_axis_len;


	}

	private final int toDeviceX( double nx, int eye )
	{
	    return (int) ((nx + trans_x) * axis_scale ) + to_mid_x + ((eye == 1) ? translate_right : 0);
	}
	private final int toDeviceY( double ny, int eye )
	{
	    return (int) ((ny + trans_y) * axis_scale ) + to_mid_y;
	}
	
	// ===== drawing ======================

	public void drawRubberBox()
	{
	    // swap the start and end x coords if the width becomes negative

	    /*
	    if(sel_end.x < sel_start.x)
	    {
		int tmp = sel_end.x;
		sel_end.x = sel_start.x;
		sel_start.x = tmp;
	    }

	    // likewise for the y coords
	    if(sel_end.y < sel_start.y)
	    {
		int tmp = sel_end.y;
		sel_end.y = sel_start.y;
		sel_start.y =tmp;
	    }
	    */

	    Graphics graphic = getGraphics();
	    graphic.setXORMode(text_col);
	    graphic.drawRect(sel_start.x, sel_start.y, sel_end.x-sel_start.x, sel_end.y-sel_start.y);
	}

	public final double[][][] makeRotationMatrices(HCube bounds)
	{
	    final int n_v = bounds.vertex.length;
	    final int n_e = bounds.edge.length;
	    final int n_d = bounds.dim;

	    final double[] real_axis_rot_angle = new double[axis_rot_angle.length];

	    for(int a=0; a < axis_rot_angle.length; a++)
	    {
		real_axis_rot_angle[a] = (axis_rot_angle[a] - 0.5) * (2 * Pi);
		// System.out.print(mview.niceDouble(real_axis_rot_angle[a], 6,4) + "\t");
	    }

	    // System.out.println();
	    
	    final double[][][] rot_mat = new double[n_d][][];
	    final double[] rot_dir = new double[n_d];


	    // need one rotation matrice for each of the axes (i.e. each dimension)
	    for(int d=0; d < n_d; d++)
	    {
		for(int a=0; a < n_d; a++)
		    rot_dir[a] = (a == d) ?  real_axis_rot_angle[a] : .0;

		rot_mat[d] = makeRotMatrix( n_d, rot_dir );

		/*
		System.out.print("ang" + d + "= ");
		for(int i=0;i<rot_dir.length;i++)
		    System.out.print(mview.niceDouble(rot_dir[i],6,4) + "\t");
	        System.out.println();

		
		System.out.println("rot" + d + "=");
		for(int i=0;i<rot_mat[d].length;i++)
		{
		    for(int j=0;j<rot_mat[d][i].length;j++)
			System.out.print(mview.niceDouble(rot_mat[d][i][j],6,4) + "\t");
		    System.out.println();
		}
		*/
	    }

	    return rot_mat;
	}

	// if stereo out[0] is left eye and out[1] is right eye
	// else out[0] is middle eye

	// each eye is double[3] : eye[0] = screen_x, eye[1] = screen_y, eye[2] = distance
	//
	public final void transformPoint( final int n_d, final double[] in, final double[][][] rot_mat, final double[][] out )
	{

	    final double[] rot   = new double[ n_d ];
	    double[] transf = new double[ n_d ];
	    
	    
	    // shift normalised coords to origin
	    //
	    vecTranslate( n_d, in, -0.5, transf);
		

	    // rotate about each of the axes
	    for(int d=0; d < n_d; d++)
	    {
		transf = vecMatMul( n_d, transf, rot_mat[d] );
	    }

	    double[] lproj = transf;
	    double[] rproj = null;

	    
	    if(stereo)
	    {
		rproj = (double[]) lproj.clone();
		
		//vecTranslate( n_d, lproj, -half_eye_sep, lproj);
		//vecTranslate( n_d, rproj,  half_eye_sep, rproj);
	    }

	    // repeatedly project until in 3-space
	    for(int d=n_d; d>3; d--)
	    {
		lproj = projectPoint( d, lproj );
	    }

	    if(stereo)
	    {
		for(int d=n_d; d>3; d--)
		{
		    rproj = projectPoint( d, rproj );
		}
	    
		// offset points in dimension 1 (which is Y for some reason, Z is in 0) by eye separation

		lproj[1] -= half_eye_sep;
		rproj[1] += half_eye_sep;
	    }

	    // project to 2-space and store
	    if(n_d > 2)
	    {
		lproj = projectPoint( 3, lproj );

		out[0][0] = lproj[0];
		out[0][1] = lproj[1];
		out[0][2] = lproj[2];
	    }
	    else
	    {
		out[0][0] = lproj[0];
		out[0][1] = lproj[1];
		out[0][2] = .0;
	    }
	    
	    if(stereo)
	    {
		// project to 2-space and store
		if(n_d > 2)
		{
		    rproj = projectPoint( 3, rproj );
		    
		    out[1][0] = rproj[0];
		    out[1][1] = rproj[1];
		    out[1][2] = rproj[2];
		}
		else
		{
		    out[1][0] = rproj[0];
		    out[1][1] = rproj[1];
		    out[1][2] = .0;
		}
	    }

	}

	public void drawHCube(Graphics graphic, HCube hcube)
	{
	    if(hcube == null)
		return;
	    
	    final int n_v = hcube.vertex.length;
	    final int n_e = hcube.edge.length;
	    final int n_d = hcube.dim;
	    
	    // System.out.println("drawHCube(): dim=" + n_d + " n.v=" + n_v + " v.len=" + hcube.vertex[0].length);
	    
	    double[][] proj = new double[2][3];
	    
	    final int[][][]  s_hcube = new int[2][ n_v ][ n_d ];
	    final double[][] depth = new double[2][ n_v ];

	    for(int v=0; v < n_v; v++)
	    {
		transformPoint( n_d, hcube.vertex[v], rot_mat, proj );
		
		// convert to device coords for display
		//
		s_hcube[0][v][0] = toDeviceX( proj[0][0], 0 );
		s_hcube[0][v][1] = toDeviceY( proj[0][1], 0 );
		depth[0][v] = proj[0][2];

		if(stereo)
		{
		    s_hcube[1][v][0] = toDeviceX( proj[1][0], 1 );
		    s_hcube[1][v][1] = toDeviceY( proj[1][1], 1 );
		    depth[1][v] = proj[1][2];
		}

		// System.out.println( s_hcube[v][0] + " , " + s_hcube[v][1] );
	    }

	    for(int e=0; e < n_e; e++)
	    {
		final int e0 = hcube.edge[e][0];
		final int e1 = hcube.edge[e][1];

		final double mid_depth = depth[0][e0] > depth[0][e1] ? depth[0][e0] : depth[0][e1]; // (depth[0][e0] + depth[0][e1]) * 0.5;
 
		int ac = hcube.edge_dim[e];

		while(ac >= axis_col.length)
		{
		    ac -= axis_col.length;
		}
		
		left_depth_sorter.addEdge( s_hcube[0][e0][0], s_hcube[0][e0][1], 
					   mid_depth, 
					   s_hcube[0][e1][0], s_hcube[0][e1][1], 
					   axis_col[ac] );

		//graphic.drawLine( s_hcube[0][e0][0], s_hcube[0][e0][1], s_hcube[0][e1][0], s_hcube[0][e1][1] );
	    }

	    if(stereo)
	    {
		for(int e=0; e < n_e; e++)
		{
		    final int e0 = hcube.edge[e][0];
		    final int e1 = hcube.edge[e][1];

		    final double mid_depth = depth[0][e0] > depth[0][e1] ? depth[0][e0] : depth[0][e1]; // (depth[0][e0] + depth[0][e1]) * 0.5;
		    
		    int ac = hcube.edge_dim[e];
		    
		    while(ac >= axis_col.length)
		    {
			ac -= axis_col.length;
		    }
		    
		    right_depth_sorter.addEdge(s_hcube[1][e0][0], s_hcube[1][e0][1], 
					       mid_depth, 
					       s_hcube[1][e1][0], s_hcube[1][e1][1], 
					       axis_col[ac] );
		    
		    //graphic.drawLine( s_hcube[1][e0][0], s_hcube[1][e0][1], s_hcube[1][e1][0], s_hcube[1][e1][1] );
		}
	    }
	}

	public void drawLimits(Graphics graphic, int pw, int ph)
	{
	    if(axes == null)
		return;

	    /*
	    final int n_d = bounds.dim;

	    Color bgcol = background_col.darker();

	    for(int d=0; d < n_d; d++)
	    {
		drawHCube( graphic, axes[d] );
	    }
	    */

	    drawHCube( graphic, bounds );
	}

	public void drawAxes(Graphics graphic, int pw, int ph)
	{
	    if(axes == null)
		return;
	    
	    final int n_d = bounds.dim;

	    double[][] proj  = new double[2][3];
	    double[]   point = new double[n_d];

	    // project [0,0,0] first 

	    for(int d1=0; d1<n_d; d1++)
		point[d1] = .0;
	    transformPoint( n_d, point, rot_mat, proj );

	    int ox = toDeviceX( proj[0][0], 0 );
	    int oy = toDeviceY( proj[0][1], 0 );
	    int oz = (int) proj[0][2];
	    
	    final FontMetrics fm = graphic.getFontMetrics();
	    String lab = null;
	    int labw = 0;

	    for(int d=0; d < n_d; d++)
	    {
		int ac = d;
		while(ac >= axis_col.length)
		{
		    ac -= axis_col.length;
		}

		for(int d1=0; d1<n_d; d1++)
		    point[d1] = .0;
		point[d] = 1.0;

		transformPoint( n_d, point, rot_mat, proj );

		int sx = toDeviceX( proj[0][0], 0 );
		int sy = toDeviceY( proj[0][1], 0 );
		int depth = (int) proj[0][2];

		left_depth_sorter.addEdge( ox, oy, (oz + depth) / 2, sx, sy, axis_col[ ac ] );

		if(show_axes_labels)
		{
		    lab = edata.getMeasurementName( meas_ids[ d ] );
		    labw = fm.stringWidth(lab);

		    left_depth_sorter.addLabel( sx, sy, depth, lab, labw, axis_col[ ac ] );
		}
	
		if(stereo)
		{
		    sx = toDeviceX( proj[1][0], 0 );
		    sy = toDeviceY( proj[1][1], 0 );
		    depth = (int) proj[1][2];
		    
		    right_depth_sorter.addEdge( ox, oy, (oz + depth) / 2, sx, sy, axis_col[ ac ] );

		    if(show_axes_labels)
			right_depth_sorter.addLabel( sx, sy, depth, lab, labw, axis_col[ ac ]);
 		}
	    }
	}

	final int n_grid_pts = 5;
	final double p_delta = 1.0 / (double) (n_grid_pts-1);
	final Color grid_col = Color.black;

	public void drawGridPoints(Graphics graphic, int pw, int ph)
	{
	    final int n_d = bounds.dim;

	    double[]  origin = new double[n_d];
	    for(int d1=0; d1<n_d; d1++)
		origin[d1] = .0;

	    // System.out.println( "draw grid dim=" + n_d);

	    drawGridPoints( n_d, 0 , origin );

	    //drawOrigin( graphic, pw, ph );
	}

	private void drawGridPoints( int n_dim, int dim, double[] src_point )
	{
	    if(dim == n_dim)
	    {
		double[][] proj  = new double[2][3];
	    
		transformPoint( n_dim, src_point, rot_mat, proj );
		
		int sx = toDeviceX( proj[0][0], 0 );
		int sy = toDeviceY( proj[0][1], 0 );
		
		left_depth_sorter.addPoint( sx, sy, proj[0][2], grid_col );
	    }
	    else
	    {
		// a dim-d grid of points 

		double[] point = new double[n_dim];

		double p = .0;
		    
		for(int pt=0; pt < n_grid_pts; pt++)
		{
		    for(int d=0; d < n_dim; d++)
			point[d] = src_point[d];
		    
		    point[dim] = p;
		    
		    drawGridPoints( n_dim, dim+1, point );
		    
		    p += p_delta;
		}
	    }
	}


	public void drawOrigin(Graphics graphic, int pw, int ph)
	{
	    
	    final int n_dim = bounds.dim;
	    double[] point = new double[n_dim];
	    double[][] proj  = new double[2][3];
	    
	    for(int d=0; d < n_dim; d++)
	    {
		for(int pt=0; pt < n_dim; pt++)
		    point[pt] = .5;
		
		point[d] = .2;

		transformPoint( n_dim, point, rot_mat, proj );
		
		int sx = toDeviceX( proj[0][0], 0 );
		int sy = toDeviceY( proj[0][1], 0 );
		double depth = proj[0][2];
		
		for(int pt=0; pt < n_dim; pt++)
		    point[pt] = .5;
		
		point[d] = .8;

		transformPoint( n_dim, point, rot_mat, proj );
		
		int ex = toDeviceX( proj[0][0], 0 );
		int ey = toDeviceY( proj[0][1], 0 );
		depth +=  proj[0][2];
		
		left_depth_sorter.addEdge( sx, sy, depth*.5, ex, ey, grid_col );
	    }

	}


	public void drawPoints(Graphics graphic, int pw, int ph)
	{
	    double[][] proj = new double[2][3];
	    final int n_d = bounds.dim;
	    
	    final double[] vec = new double[n_meas];

	    Color colour = text_col;

	    for(int s=0; s < n_spots; s++)
	    {
		if(!use_filter || !edata.filter(s))
		{
		    // compute the position of this spot....

		    boolean failed = false;

		    for(int m = 0; m < n_meas; m++)
		    {
			if(!failed)
			{
			    double e = edata.eValue( meas_ids[m], s );
			    
			    if((e < meas_min[m]) || (e > meas_max[m]))
			    {
				failed = true;
			    }
			    else
			    {
				e += e_trans[m];
				e *= e_scale[m];
				
				vec[m] = e;
				
				if(debug)
				{
				    if((e < .0) || (e > 1.))
				    {
					System.out.println( "spot=" + s + " meas=" + meas_ids[m] + " e=" + e);
				    }
				}		
			    }
			}
		    }
		    
		    if(!failed)
		    {
			transformPoint( n_d, vec, rot_mat, proj );
			
			final int sx = toDeviceX( proj[0][0], 0 );
			final int sy = toDeviceY( proj[0][1], 0 );
			
			if(root_spot_picker != null)
			    root_spot_picker.storeSpot(sx, sy, s);
			
			if(colour_meas_index >= 0)
			    colour = dplot.getDataColour( edata.eValue( colour_meas_index, s ), colour_meas_index);
			
			left_depth_sorter.addSpot( sx, sy, proj[0][2], s, colour );
			
			if(stereo)
			{
			    right_depth_sorter.addSpot( toDeviceX( proj[1][0], 1 ), toDeviceY( proj[1][1], 1 ), proj[1][2], s, colour );
			}
		    }
		}
	    }
	}
	

	public void drawClusters(Graphics graphic, int pw, int ph)
	{
	    drawCluster( graphic, edata.getRootCluster(), null, left_depth_sorter );
	    if(stereo)
		drawCluster( graphic, edata.getRootCluster(), null, right_depth_sorter );
	}

	// returns the mid-point of all the elements
	// or if there are no elements, the mid-point of all the children
	// or null if cluster has no elements and no children
	//
	public Point drawCluster(Graphics graphic, ExprData.Cluster clust, Point parent_mid_pt, DepthSorter depth_sorter)
	{
	    Point mid_pt = null;
	    
	    cluster_offset = cluster_size / 2;
	    
	    if(clust != null)
	    {
		if(clust.getShow() && clust.getIsSpot())
		{
		    Integer i;
		    int iv, gene_index;

		    mid_pt = drawGlyphs( graphic, clust, depth_sorter);

		    if(show_edges)
		    {
			if(mid_pt != null)
			{
			    drawElementEdges(graphic, clust, mid_pt, depth_sorter );
			}
		    }
		}
		
		final int nc = clust.getNumChildren();
		if(nc > 0)
		{
		    int acc_x = 0;
		    int acc_y = 0;
		    int n_vis_ch = 0;

		    final Vector ch = clust.getChildren();
		    Point last_child_mid_pt = null;
		    for(int c=0; c < nc; c++)
		    {
			Point child_mid_pt = drawCluster( graphic, (ExprData.Cluster)ch.elementAt(c), mid_pt, depth_sorter );
			
			if(mid_pt == null)
			{
			    // as the parent cluster has no elements, join each of it's children with a line
			    // and compute a new mid point from the children
			    
			    if(child_mid_pt != null)
			    {
				acc_x += child_mid_pt.x;
				acc_y += child_mid_pt.y;
				n_vis_ch++;
				
				if(show_edges)
				{
				    if(last_child_mid_pt != null)
				    {
					depth_sorter.addEdge( last_child_mid_pt.x, last_child_mid_pt.y, 
							      .0,  // depth unknown
							      child_mid_pt.x, child_mid_pt.y,
							      clust.getColour() );
					
				    }
				    last_child_mid_pt = child_mid_pt;
				}
			    }
			}
			
		    }
		    
		    if((mid_pt == null) && (n_vis_ch > 0))
		    {
			// if there were no elements, then use the mid_pt of the children
			// as the midpoint of this cluster
			mid_pt = new Point( acc_x / n_vis_ch, acc_y / n_vis_ch );
		    }
		}

		if(show_edges)
		{
		    if(mid_pt != null)
		    {
			drawParentEdge(graphic, parent_mid_pt, mid_pt, clust.getColour(), depth_sorter );
		    }
		}

	    }
	    return mid_pt;
	}
	
	// draws from mid_pt to each of the visible elements
	public void drawParentEdge(Graphics graphic, Point parent_mid_pt, Point mid_pt, Color colour, DepthSorter depth_sorter)
	{
	    if((parent_mid_pt != null) && (mid_pt != null))
		depth_sorter.addEdge(parent_mid_pt.x, parent_mid_pt.y, 
				     .0,   // depth unknown
				     mid_pt.x, mid_pt.y, colour);
	}

	// draws from mid_pt to each of the visible elements
	public void drawElementEdges(Graphics graphic, ExprData.Cluster clust, Point mid_pt, DepthSorter depth_sorter)
	{
	    final int[] ce = clust.getElements();
	    
	    if(ce == null)
	    {
		return;
	    }
	    
	    for(int s=0; s < ce.length; s++)
	    {
		int spot_id = ce[s];
		if(!use_filter || !edata.filter(spot_id))
		{
		    // compute the position of this spot....

		    Primitive prim = depth_sorter.getSpot( spot_id );
		    if(prim != null)
		    {
			depth_sorter.addEdge( mid_pt.x, mid_pt.y, 
					      prim.depth, 
					      prim.sx, prim.sy, 
					      clust.getColour() );
		    }
		}
	    }
	}
	
	// returns the mid-point of all the elements
	// or null is no elements
	public Point drawGlyphs(Graphics graphic, ExprData.Cluster clust, DepthSorter depth_sorter)
	{
	    int acc_x = 0;
	    int acc_y = 0;
	    
	    final int[] ce = clust.getElements();
	    
	    if(ce == null)
	    {
		return null;
	    }

	    // graphic.setColor(clust.getColour());
	    final int glyph = clust.getGlyph();
	    
	    int n_vis = 0;  // how many glyphs are actually drawn
	    
	    for(int s=0; s < ce.length; s++)
	    {
		final int spot_id = ce[s];
		if(!use_filter || !edata.filter(spot_id))
		{
		    // lookup the position of this spot....
		    Primitive prim = depth_sorter.getSpot( spot_id );
		    if(prim != null)
		    {
			if(show_glyphs)
			    depth_sorter.addGlyph( prim.sx, prim.sy, prim.depth, clust );
			
			// keep track of the midpoint
			acc_x += prim.sx;
			acc_y += prim.sy;
			n_vis++;
		    }
		}
	    }
	    
	    return (n_vis == 0) ? null : ( new Point((acc_x / n_vis), (acc_y / n_vis)) );
	}
	

	private void drawLabels(Graphics graphic)
	{
	    final Color fg = mview.getTextColour();
	    final Color bg = mview.getBackgroundColour();

	    final FontMetrics fm = graphic.getFontMetrics();
	    final int laba = fm.getAscent();
	    final int labh = laba + fm.getDescent();

	    final double[][] proj = new double[2][3];
	    final double[] vec = new double[n_meas];

	    nt_sel = nts.getNameTagSelection();

	    for (Enumeration enum = show_spot_label.keys(); enum.hasMoreElements() ;) 
	    {
		Integer id = (Integer) enum.nextElement();

		int spot_id = id.intValue();

		// System.out.println("drawing label " + spot_id);

		String lab = nt_sel.getNameTag(spot_id);
		if(lab != null)
		{
		    // compute the position of this spot....
		    
		    Primitive prim = left_depth_sorter.getSpot( spot_id );
		    if(prim != null)
		    {
			final int labw = fm.stringWidth(lab);
			
			left_depth_sorter.addLabel( prim.sx, prim.sy, prim.depth, lab, labw, text_col );

			if(stereo)
			{
			    Primitive r_prim = right_depth_sorter.getSpot( spot_id );
			    right_depth_sorter.addLabel( r_prim.sx, r_prim.sy, r_prim.depth, lab, labw, text_col );
			}
		    }
		}
	    }
	}
	
	public void paintComponent(Graphics graphic)
	{
	    if(graphic == null)
		return;

	    paintIntoRegion(graphic, getWidth(), getHeight());
	}
	
	private void paintIntoRegion(Graphics graphic, int pw, int ph)
	{
	    // compute transformation from normalised coords to device coords
	    
	    long start_t =  new java.util.Date().getTime();

	    graphic.setColor(background_col);
	    graphic.fillRect(0, 0, pw, ph);
	    
	    if(meas_ids == null)
		return;
	    if(meas_ids.length < 2)
		return;
	    if(bounds == null)
		return;
	    
	    if(left_depth_sorter == null)
	    {
		left_depth_sorter = new DepthSorter();
	    }
	    else
	    {
		left_depth_sorter.reset();
	    }

	    if(stereo)
	    {
		if(right_depth_sorter == null)
		{
		    right_depth_sorter = new DepthSorter();
		}
		else
		{
		    right_depth_sorter.reset();
		}
	    }

	    setupTransformationAndAxes(pw, ph);

	    rot_mat = makeRotationMatrices(bounds);
	    
	    if(debug)
	    {
		System.out.println(meas_ids.length + "-gon");
	    }
	    
	    int ps = (spot_size_jcb.getSelectedIndex() * 2) + 1;
	    if(ps < 1)
		ps = 1;
	    spot_size = ps;
	    spot_offset = ps / 2;
	    
	    // drawLimits(graphic, pw, ph);
	    long init_t =  new java.util.Date().getTime();
	    long init_msecs = init_t - start_t;
	    
	    boolean draw_all = true;

	    if(use_wireframe_for_move)
	    {
		if(wireframe || dragging)
		    draw_all = false;
	    }
	    
	    if(show_axes)
		drawAxes(graphic, pw, ph);
	    if(show_limits)
		drawLimits(graphic, pw, ph);
	    if(show_grid)
		drawGridPoints(graphic, pw, ph);
	    if(show_origin)
		drawOrigin(graphic, pw, ph);

	    // draw_all = false;

	    if(draw_all)
	    {
		root_spot_picker = new SpotPickerNode(0, 0, pw, ph);

		drawPoints(graphic, pw, ph);
		
		if(show_glyphs || show_edges)
		    drawClusters(graphic, pw, ph);

		drawLabels(graphic);

		// root_spot_picker.dumpStats("");
		//root_spot_picker.drawNode(graphic);

	    }
	    else
	    {
		root_spot_picker = null;
	    }
	    
	    long proj_t =  new java.util.Date().getTime();
	    long proj_msecs = proj_t - init_t;

	    if(depth_sort)
	    {
		left_depth_sorter.sort();
		if(stereo)
		    right_depth_sorter.sort();
	    }
	    
	    long sort_t =  new java.util.Date().getTime();
	    long sort_msecs = sort_t - proj_t;

	    drawPrimitives(left_depth_sorter, graphic);

	    if(stereo)
		drawPrimitives(right_depth_sorter, graphic);

	    long draw_t =  new java.util.Date().getTime();
	    long draw_msecs = draw_t - sort_t;
	    
	    // long msecs = new java.util.Date().getTime() - start_time.getTime();
	    
	    // double fps = 1.0 / (((double) msecs) * 0.001);
	    
	    // String fps_str = mview.niceDouble(fps, 7, 3);

	    if(perftime)
	    {
		String times = 
		"init: " + init_msecs + 
		"ms  proj: " + proj_msecs + 
		"ms  sort: " + sort_msecs + 
		"ms  draw: " + draw_msecs + 
		"ms  total: " + (draw_t-start_t) + "ms";
		
		graphic.setColor(text_col);
		graphic.drawString( times, 10, ph - 5 );
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
	    int pw = (int)pf.getImageableWidth();
	    int ph = (int)pf.getImageableHeight();
	    
	    // System.out.println("PRINT REQUEST for page " + pg_num + " size=" + pw + "x" + ph); 
	    
	    panel.paintIntoRegion(g, pw, ph);
	    
	    return (pg_num > 0) ? NO_SUCH_PAGE : PAGE_EXISTS;
	}
	
    }
    
    // ================================================================================
    // ===== spot labels ======================
    // ================================================================================

    private Hashtable show_spot_label = new Hashtable();
    final String dummy = "x";

    public void showSpotLabel(int id)
    {
	// if(show_spot_label.get(i) == null)
	{
	   Integer i = new Integer(id);
	   show_spot_label.put(i, dummy);
	}
    }

    public void clearAllSpotLabels()
    {
	show_spot_label = new Hashtable();
    }

    public void labelAllSpots()
    {
	final int ns = edata.getNumSpots();
	Primitive p = null;
	for(int s=0; s < ns; s++)
	{
	    p = left_depth_sorter.getSpot( s );
	    if(p != null)
	    {
		show_spot_label.put(new Integer(s), dummy);
	    }
	}
    }

    public void labelSelection()
    {
	int[] sels = edata.getSpotSelection();

	if(sels == null)
	    return;

	for(int s=0; s < sels.length; s++)
	{
	    show_spot_label.put(new Integer(sels[s]), dummy);
	}
    }

    public void toggleSpotLabel(int id)
    {
	Integer i = new Integer(id);
	
	if(show_spot_label.get(i) == null)
	{
	    show_spot_label.put(i, dummy);
	    // System.out.println(id + " added");
	}
	else
	{
	    show_spot_label.remove(i);
	    // System.out.println(id + " removed");
	}
	
	panel.repaint();
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

    // ================================================================================
    // ===== hypercubes ======================
    // ================================================================================
    
    public final static boolean debug_hcube = false;

    public HCube makeHCube( int dim )
    {
	if(dim < 1)
	    return null;

	if(debug_hcube)
	    System.out.println("making " + dim + "-cube");

	if(dim == 1)   // base case
	{
	    HCube one_d = new HCube(1, 2, 1);
	    
	    one_d.vertex[0][0] = .0;   // origin
	    one_d.vertex[1][0] = 1;    // x=1

	    one_d.edge[0][0] = 0;
	    one_d.edge[0][1] = 1;

	    one_d.edge_dim[0] = 0;

	    if(debug_hcube)
		System.out.println("..." + dim + "-cube made");

	    return one_d;
	}
	else
	{
	    return ( extrudeHCube( dim, makeHCube( dim - 1 )));
	}
    }

    private HCube extrudeHCube( int dim, HCube input )
    {
	final int input_dim = input.vertex.length;

	final int input_n_v = input.vertex.length;
	final int input_n_e = input.edge.length;
	
	if(debug_hcube)
	{
	    System.out.println("...extruding from " + input.dim + "-cube");
	    System.out.println("   with " + input_n_e + " edges and " + input_n_v + " verts");
	}
	
	// make new shape by copying all verts of existing shape and
	// translating them along the new dimension

	// new shape will have twice as many verts as previous one

	// will have twice the existing 'edges' plus a 
	// new 'edge' for each of the existing vertices
	
	// an 'edge' in a HCube of dim D is a HCube of dim D-1
	// (eg edges of square are lines,  edges of cube are squares, etc)
	
	int new_n_edges = (input_n_e * 2) + input_n_v;
	
	int new_n_verts = 2 * input_n_v;

	HCube output = new HCube( input.dim+1, new_n_verts, new_n_edges);

	
	if(debug_hcube)
	    System.out.println("...extrusion will have " + new_n_edges + " edges and " + new_n_verts + " verts");
	
	// copy existing verts
	// set new dimension value to .0
	for(int v=0; v < input_n_v ; v++)
	{
	    for(int od=0; od < input.dim; od++)
		output.vertex[v][od] = input.vertex[v][od];
	    output.vertex[v][input.dim] = .0;
	}
	
	if(debug_hcube)
	    System.out.println("..." + input_n_v + " verts copied");

	// copy 'shadow' of existing verts
	// set new dimension value to 1.0
	for(int v=0; v < input_n_v; v++)
	{
	    for(int od=0; od < input.dim; od++)
		output.vertex[input_n_v + v][od] = input.vertex[v][od];
	    output.vertex[input_n_v + v][input.dim] = 1.0;
	}

	if(debug_hcube)
	    System.out.println("..." + input_n_v + " 'shadow' verts made");
	
	// copy input edges
	for(int e=0; e < input_n_e; e++)
	{
	    output.edge[e][0] = input.edge[e][0];
	    output.edge[e][1] = input.edge[e][1];

	    output.edge_dim[e] = input.edge_dim[e];
	}
	
	if(debug_hcube)
	    System.out.println("..." + input_n_e + " edges copied");

	// create edges for the copy (shadow) of input vertices
	int e_p =  input_n_e;
	for(int e=0; e < input_n_e; e++)
	{
	    output.edge[e_p + e][0] = input.edge[e][0] + input_n_v ;
	    output.edge[e_p + e][1] = input.edge[e][1] + input_n_v;

	    output.edge_dim[e_p + e] = input.edge_dim[e];
	}

	if(debug_hcube)
	    System.out.println("..." + input_n_e + " new 'shadow' edges made");

	// and create new edges between the input vertices and the copy vertices
	e_p =  2 * input_n_e;
	for(int e=0; e < input_n_v; e++)
	{
	    output.edge[e_p + e][0] = e;
	    output.edge[e_p + e][1] = e + input_n_v;

	    output.edge_dim[e_p + e] = dim-1;
	}
	
	if(debug_hcube)
	{
	    System.out.println("..." + input_n_v + " new joining edges made");
	    System.out.println("..." + output.dim + "-cube made by extrusion");
	}

	return output;
    }

    private class HCube
    {
	int dim;
	double[][] vertex;
	int[][] edge;

	int[] edge_dim;   // which dimension is this edge in?

	public HCube( int dim_, int n_verts, int n_edges )
	{
	    dim = dim_;
	    vertex   = new double[n_verts][dim];
	    edge     = new int[n_edges][2];
	    edge_dim = new int[n_edges];
	}

	public void dump()
	{
	    System.out.println( "dim=" + dim + ": " + vertex.length + " verts, " + edge.length + " edges");

	    for(int v=0; v < vertex.length; v++)
	    {
		for(int d=0; d < dim; d++)
		{
		    System.out.print( vertex[v][d] + ((d+1<dim) ? ",\t" : "\n"));
		}
	    }
	    for(int e=0; e < edge.length; e++)
	    {
		System.out.println( edge[e][0] + " -> " + edge[e][1] + " (" + edge_dim[e] + ")" );
	    }
	}

	public void setCorners(double[][] corners)
	{
	    // edges stay the same, but many vertices need to be adjusted...

	    final int n_verts = 1 << dim;
	    
	    // System.out.println("setCorners(): dim=" + dim + " : predicting " + n_verts + " has " + vertex.length + " verts");

	    for(int v=0; v < n_verts; v++)
	    {
		// use the bit pattern of 'v' to work out which corner this is

		for(int d=0; d < dim; d++)
		{
		    vertex[v][d] = ((v & (1 << d)) > 0) ? corners[1][d] : corners[0][d];
		}
	    }
	}

    }

    // ================================================================================
    // ===== axes (made of hypercubes)  ======================
    // ================================================================================
    
    private HCube[] makeAxes(int dim)
    {
	if(dim < 1)
	    return null;
	
	HCube[] axis = new HCube[dim];
	
	double[][] corners = new double[2][dim];
	
	// System.out.println( "making axes for " + dim + "-space");

	for(int a=0; a < dim; a++)
	{
	    axis[a] = makeHCube(dim);
	    
	    for(int a2=0; a2 < dim; a2++)
	    {
		corners[0][a2] = .0;   // origin 0,0,...,0
		corners[1][a2] = 1.0;  // other 'end' of axis e.g. 1,1,1,0,1
	    }
	    // set coord for relevant dimension of this axis to .0
	    corners[1][a] = .0;
	    
	    // System.out.println( "   corners are " + corners[0].length + "-dim");

	    axis[a].setCorners( corners );
	}
	
	return axis;
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
    // =====  DepthSorter  ======================
    // ================================================================================
    //
    //  a data structure used to depth sort of the elements for display
    //
    //  need to draw things in the right order, i.e. the stuff at the back first
    //
    //  don't want to allocate n_spots objects on every rendering,
    //    try to reuse the container object(s)
    //
    //
    //   must be sortable on depth,
    //
    //   must handle: spot, coloured glyph, coloured edge
    //
    //
    // ================================================================================

    class Primitive
    {
	int mode;
	
	int    sx, sy;   // screen position
	int    sid;      // spot id (used for retrieval)
	double depth;
	
	int   glyph;
	Color colour;
	int   edge_x, edge_y;

	String str;
	int    str_w;   // width in pixels, used for clearing background of label

	public static final int Unused = 0;
	public static final int Spot   = 1;
	public static final int Glyph  = 2;
	public static final int Edge   = 3;
	public static final int Label  = 4;
	public static final int Point  = 5;
    }

    // want to sort an array from -depth to depth
    // (so drawing from 0.....length goes from back to front)
    //
    class PrimitiveComparator  implements java.util.Comparator
    {
	public int compare(Object o1, Object o2)
	{
	    Primitive p1 = (Primitive) o1;
	    Primitive p2 = (Primitive) o2;

	    // -1 == p1 closer, +1 == p1 further

	    if(p1.mode == Primitive.Unused)
	    {
		return (p2.mode == Primitive.Unused) ? 0 : +1;
	    }
	    else
	    {
		// p1 is used...
		
		if(p2.mode == Primitive.Unused)
		{
		    return -1;
		}
		else
		{
		    return (p1.depth < p2.depth) ? -1 : +1;
		}
	    }
	}
	
	public boolean equals(Object o) { return false; }
    }

    class DepthSorter
    {
	public final int InitialPrimitives = 1024;
	public final int PrimitivesGrowthFactor = 2;

	public Primitive[] prims;
	public int n_used_prims;

	public int[] sid_to_pid;

	public DepthSorter()
	{
	    sid_to_pid = null;
	    prims = null;
	    allocateMorePrimitives();
	    reset();
	}

	public final void reset()
	{
	    n_used_prims = 0;
	    for(int p=0; p < prims.length; p++)
	    {
		prims[p].mode = Primitive.Unused;
	    }
	}

	public final void addPoint( int sx, int sy, double d, Color col )
	{
	    int pid = getNextUnusedPrimitive();
	    
	    prims[pid].mode = Primitive.Point;
	    prims[pid].sx = sx;
	    prims[pid].sy = sy;
	    prims[pid].depth = d;
	    prims[pid].colour = col;
	}

	public final void addSpot( int sx, int sy, double d, int sid, Color col )
	{
	    int pid = getNextUnusedPrimitive();
	    
	    prims[pid].mode = Primitive.Spot;
	    prims[pid].sx = sx;
	    prims[pid].sy = sy;
	    prims[pid].sid = sid;
	    prims[pid].depth = d;
	    prims[pid].colour = col;

	    if(sid >= 0)
		sid_to_pid[ sid ] = pid;
	}

	public final void addGlyph( int sx, int sy, double d, ExprData.Cluster cl )
	{
	    int pid = getNextUnusedPrimitive();
	    
	    prims[pid].mode = Primitive.Glyph;
	    prims[pid].depth = d;
	    prims[pid].sx = sx;
	    prims[pid].sy = sy;
	    prims[pid].glyph = cl.getGlyph();
	    prims[pid].colour = cl.getColour();
	}

	public final void addEdge( int sx, int sy, double d, int ex, int ey, Color col )
	{
	    int pid = getNextUnusedPrimitive();
	    
	    prims[pid].mode = Primitive.Edge;
	    prims[pid].depth = d;
	    prims[pid].sx = sx;
	    prims[pid].sy = sy;
	    prims[pid].edge_x = ex;
	    prims[pid].edge_y = ey;
	    prims[pid].colour = col;
	}
	public final void addLabel( int sx, int sy, double d, String str, int str_w, Color col )
	{
	    int pid = getNextUnusedPrimitive();
	    
	    prims[pid].mode  = Primitive.Label;
	    prims[pid].depth = d;
	    prims[pid].sx    = sx;
	    prims[pid].sy    = sy;
	    prims[pid].str   = str;
	    prims[pid].str_w = str_w;   // width in pixels, used for clearing background of label
	    prims[pid].colour = col;
	}

	public void sort()
	{
	    java.util.Arrays.sort( prims, new PrimitiveComparator() );
	    mapSidsToPids();

	    /*
	    double min_d = prims[0].depth;
	    double max_d = prims[0].depth;

	    for(int p=1; p < prims.length; p++)
	    {
		if(prims[p].depth < min_d)
		    min_d = prims[p].depth;
		if(prims[p].depth > max_d)
		    max_d = prims[p].depth;
	    }

	    System.out.println(prims.length + " primitives, depth range:" + min_d + " ... " + max_d);
	    */

	}

	public Primitive getSpot( int sid )
	{
	    final int pid = sid_to_pid[ sid ];
	    
	    return (pid >= 0) ? prims[pid] : null;
	}
	
	// === internals... ==========

	private void mapSidsToPids()
	{
	    final int ns = edata.getNumSpots();
	    if((sid_to_pid == null) || (sid_to_pid.length != ns))
		sid_to_pid = new int[ns];
	    for(int s=0; s < ns; s++)
		sid_to_pid[s] = -1;

	    final int np = prims.length;
	    for(int p=0; p < np; p++)
		if(prims[p].mode == Primitive.Spot)
		    sid_to_pid[ prims[p].sid ] = p;
	}

	private final void allocateMorePrimitives()
	{
	    if(prims == null)
	    {
		prims = new Primitive[ InitialPrimitives ];
		for(int p=0; p < prims.length; p++)
		{
		    prims[p] = new Primitive();
		    prims[p].mode = Primitive.Unused;
		}
	    }
	    else
	    {
		Primitive[] new_prims  = new Primitive[ prims.length * PrimitivesGrowthFactor];
		for(int p=0; p < prims.length; p++)
		{
		    new_prims[p] = prims[p];
		}
		for(int pp=prims.length; pp < new_prims.length; pp++)
		{
		    new_prims[pp] = new Primitive();
		    new_prims[pp].mode = Primitive.Unused;
		}
		prims = new_prims;
	    }
	    mapSidsToPids();
	    // System.out.println("primitive store is now of size " + prims.length);
	}

	private final int getNextUnusedPrimitive()
	{
	    if(n_used_prims < prims.length)
	    {
		int unused_id = n_used_prims;
		n_used_prims++;
		return unused_id;
	    }
	    else
	    {
		allocateMorePrimitives();
		return getNextUnusedPrimitive();
	    }
	}
	

    }

	

    private void drawPrimitives(DepthSorter depth_sorter, Graphics graphic)
    {
	if((glyph_poly == null) || (glyph_poly_height != cluster_size))
	{
	    // generate (or re-generate at a new size) the glyphs
	    glyph_poly = dplot.getScaledClusterGlyphs(cluster_size);
	    glyph_poly_height = cluster_size;
	}
	
	final Color fg = mview.getTextColour();
	final Color bg = mview.getBackgroundColour();
	final FontMetrics fm = graphic.getFontMetrics();
	final int laba = fm.getAscent();
	final int labh = laba + fm.getDescent();

	final int point_offset = 0;
	final int point_size = 1;

	// need to check all primitives in the list because the sorting
	// process may have introduced gaps of unused primitives 
	//
	final int n_p = depth_sorter.prims.length;

	// System.out.println("there are " + n_p + " primitives to draw...");

	for(int p=0; p < n_p; p++)
	{
	    final Primitive prim = depth_sorter.prims[p];
	    
	    switch(prim.mode)
	    {
	    case Primitive.Spot:
		if(show_spots)
		{
		    graphic.setColor( prim.colour );
		    graphic.fillRect( prim.sx - spot_offset, prim.sy - spot_offset, spot_size, spot_size );
		}
		break;
	    case Primitive.Glyph:
		Polygon poly = new Polygon(glyph_poly[prim.glyph].xpoints, 
					   glyph_poly[prim.glyph].ypoints,
					   glyph_poly[prim.glyph].npoints);
		
		poly.translate( prim.sx - cluster_offset, prim.sy - cluster_offset);
		
		graphic.setColor( prim.colour );
		graphic.fillPolygon(poly);
		break;
	    case Primitive.Point:
		graphic.setColor( prim.colour );
		graphic.fillRect( prim.sx, prim.sy, 1, 1 );
		break;
	    case Primitive.Edge:
		graphic.setColor( prim.colour );
		graphic.drawLine(  prim.sx , prim.sy, prim.edge_x , prim.edge_y );
		break;
	    case Primitive.Label:
		graphic.setColor( bg );
		graphic.fillRect( prim.sx, prim.sy-laba, prim.str_w, labh);
		graphic.setColor( prim.colour );
		graphic.drawString(prim.str, prim.sx, prim.sy);
		break;
	    }
	}
    }

    // ===== state ======================
   
    private maxdView mview;
    private ExprData edata;
    private DataPlot dplot;

    private boolean wireframe;

    private boolean stereo;
    private double  eye_sep;
    private double  half_eye_sep = 0.05;
    private int translate_right;

    private ExprData.NameTagSelection nt_sel;
    private NameTagSelector nts;

    private int[]  meas_ids;

    private double[] axis_vec_x;
    private double[] axis_vec_y;

    private double[] axis_rot_angle;

    private double length_normalise;

    private double[] e_scale;
    private double[] e_trans;
    private double[] a_scale;
   
    private int to_mid_x, to_mid_y;
    private double axis_scale;

    private int spot_size, spot_offset;

    //private double max_axis_len;

    private double persp_factor;

    private Color[] axis_col;

    // the translation and zoom are done in normalised coords
    private double trans_x, trans_y;   

    private int proj_mode = 1;
    private double[] proj_params;


    private int n_spots;
    private int n_meas;     // number of Measurements that are selected for display

	    
    private DragAndDropList meas_list;
    private JButton fixed_col_jb;

    private JComboBox col_jcb ;
    private JComboBox spot_size_jcb;

    private JSplitPane l_jsplp;
    private JSplitPane r_jsplp;

    private JButton sel_jb;

    private JButton zoom_jb;

    private java.awt.Point sel_start = new Point();
    private java.awt.Point sel_end   = new Point();

    private boolean selecting = false;
    private boolean zooming = false;

    private Polygon[] glyph_poly = null;
    private int glyph_poly_height;
    private int cluster_size   = 10;
    private int cluster_offset = cluster_size / 2;

    private JFrame frame;
    private DrawPanel panel;

    private KnobPanel rotate_panel;

    private KnobPanel proj_params_panel;

    private SpotPickerNode root_spot_picker;

    private DepthSorter left_depth_sorter = null;
    private DepthSorter right_depth_sorter = null;

    /*
    private boolean mouse_is_valid = false;    // true when the mouse is near a edge
    private int   mouse_edge;                 // which edge is the mouse near?
    private Point mouse_pt;                    // what the actual position of the mouse
    private int   mouse_spot;                  // the spot nearest the mouse_pt
    */

    private boolean show_spots      = true;
    private boolean show_glyphs      = false;
    private boolean show_edges       = false;
    private boolean use_filter         = true;

    private boolean show_grid        = false;
    private boolean show_origin      = false;
    private boolean show_axes        = false;
    private boolean show_axes_labels = false;
    private boolean show_limits      = true;

    private boolean uniform_scale   = true;

    private boolean depth_sort = false;

    private boolean perftime = false;

    private boolean use_wireframe_for_move = true;

    private int colour_meas_index = -1;

    private HCube bounds;

    private HCube[] axes;

    private double[] meas_min;
    private double[] meas_max;

    //private double[] axis_scale;

    private double global_min;
    private double global_max;

    /*
    private Vector near_pt_lines = null;

    private int      n_visible_meas = 0;
    private double   edge_angle = .0;
    private Point    edge_center = new Point(0,0);

    private int      edge_off = 0;
    private int      edge_gap = 0;
    private double   edge_len = .0;

    private Point2D.Double[] edge_start;
    private Point2D.Double[] edge_delta;
    private double[] edge_iscale;

    private int[] meas_in_edge = null;    // which set is in which edge
    
    private boolean filter_alert = false;

    private JLabel status_label;
    */

    private Color background_col;
    private Color text_col;

    private Image bg_image = null;

    private double[][][]  rot_mat;

    private AxisManager axis_man;
    private DecorationManager deco_man;

}
