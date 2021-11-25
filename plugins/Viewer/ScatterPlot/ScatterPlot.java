import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.event.*;
import java.awt.dnd.*;
import java.awt.print.*;

//
// display spots in a 2d plane based on the values in one or more a pairs of Measurements
//

public class ScatterPlot implements ExprData.ExprDataObserver, Plugin
{
    public ScatterPlot(maxdView mview_)
    {
	mview = mview_;
	
	final Range dummy = new Range();
    }
    
    public void initialise()
    {
	frame = new JFrame("Scatter Plot");
	
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

	cluster_size = 12;

	nt_sel = edata.new NameTagSelection();
	
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

	// -------------------------

	PlotDetails pd = new PlotDetails();

	/*
	if(first_good_m < edata.getNumMeasurements())
	    pd.x_meas = first_good_mi;
	else
	    pd.x_meas = -1;
	
	pd.y_meas = pd.x_meas;

	*/
	int[] m_sel = edata.getMeasurementSelection();

	if( ( m_sel != null ) && ( m_sel.length == 2 ) )
	{
	    pd.x_meas = m_sel[ 0 ];
	    pd.y_meas = m_sel[ 1 ];

	    pd.x_thing_name = edata.getMeasurementName( m_sel[ 0 ] );
	    pd.y_thing_name = edata.getMeasurementName( m_sel[ 1 ] );
	}
	else
	{
	    pd.x_meas = pd.y_meas = -1;
	}

	pd_list = new Vector();
	pd_list.addElement(pd);

	// -------------------------

	/*
	pd = new PlotDetails();

	if(first_good_m < edata.getNumMeasurements())
	    pd.x_meas = first_good_mi;
	else
	    pd.x_meas = -1;
	
	pd.y_meas = pd.x_meas;

	pd.x_scale = pd.y_scale = 1.0;

	pd.x_col = -1;
	pd.x_size = pd.x_meas;
	pd.y_size = pd.x_meas;


	pd.fixed_col = Color.red;

	pd_list.addElement(pd);
	*/

    }

    private void buildGUI()
    {
	background_col = mview.getBackgroundColour();
	text_col       = mview.getTextColour();

	JPanel gui_wrapper = new JPanel();
	GridBagLayout gui_gridbag = new GridBagLayout();
	gui_wrapper.setLayout( gui_gridbag );

	gui_wrapper.setPreferredSize(new Dimension(640, 600));
	

	//frame.getContentPane().setLayout( new BoxLayout( frame.getContentPane(), BoxLayout.Y_AXIS ) );


	pd_panel = new JPanel();
	buildPlotDetailControls();
	JScrollPane pd_panel_jsp = new JScrollPane( pd_panel );



	panel = new ScatterPlotPanel();
	panel.setDropAction(new DragAndDropPanel.DropAction()
	    {
		public void dropped(DragAndDropEntity dnde)
		{
		    // System.out.println("Drop!");
		    
		    try // is it a cluster?
		    {
			ExprData.Cluster drop_c = dnde.getCluster();
			//System.out.println("cluster dropped");
			int[] elems = drop_c.getAllClusterElements();
			if(elems != null)
			{
			    // shw the labels for each spot in the cluster
			    for(int s=0; s < elems.length; s++)
				showSpotLabel(elems[s]);
			    panel.repaintDisplay();
			}
		    }
		    catch(DragAndDropEntity.WrongEntityException wee)
		    {
			try // or one or more spots?
			{
			    int[] sids = dnde.getSpotIds();
			    //System.out.println("spot dropped");
			    if(sids != null)
			    {
				for(int s=0; s < sids.length; s++)
				    showSpotLabel(sids[s]);
				panel.repaintDisplay();
			    }
			}
			catch(DragAndDropEntity.WrongEntityException wee2)
			{
			    // not a cluster or spot(s), ignore it
			}
		    }
		}
	    });

	panel.setDragAction(new DragAndDropPanel.DragAction()
	    {
		public DragAndDropEntity getEntity(DragGestureEvent event)
		{
		    if(nearest_spot != null)
		    {
			if(nearest_spot.is_meas)
			    return DragAndDropEntity.createMeasurementNameEntity(nearest_spot.id);
			else
			    return DragAndDropEntity.createSpotNameEntity(nearest_spot.id);
		    }
		    else
			return null;
		}
	    });

	JSplitPane jspp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	jspp.setTopComponent(pd_panel_jsp);
	jspp.setBottomComponent(panel);
	jspp.revalidate();


	GridBagConstraints c = new GridBagConstraints();
	c.gridy = 0;
	c.weighty = 9.0;
	c.weightx = 10.0;
	c.fill = GridBagConstraints.BOTH;
	c.anchor = GridBagConstraints.NORTH;
	gui_gridbag.setConstraints( jspp, c );
	gui_wrapper.add( jspp );



	JPanel toolbar_panel = makeToolbars();
	c = new GridBagConstraints();
	c.gridy = 1;
	c.weightx = 10.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.anchor = GridBagConstraints.SOUTH;
	gui_gridbag.setConstraints( toolbar_panel, c );
	gui_wrapper.add( toolbar_panel );



	frame.getContentPane().add( gui_wrapper ); //, BorderLayout.SOUTH);



	// ===== axis =====================================================================================

	axis_man = new AxisManager(mview);

	axis_man.addAxis( new PlotAxis(mview, "X") );
	axis_man.addAxis( new PlotAxis(mview, "Y") );

	axis_man.addAxesListener( new AxisManager.AxesListener()
	    {
		public void axesChanged() 
		{
		    axesHaveChanged();
		}
	    });


	// ===== decorations ===============================================================================


	deco_man = new DecorationManager(mview, "ScatterPlot");

	deco_man.addDecoListener( new DecorationManager.DecoListener()
	    {
		public void decosChanged() 
		{
		    updateDisplay();
		}
	    });


    }

    Timer timer = null;
    

    public void cleanUp()
    {
	// 	System.out.println("cleanUp....");

	nts.saveSelection("ScatterPlot.name_tags");

	if( panel != null )
	{
	    panel.abort_draw = true;
	    panel = null;
	}

	edata.removeObserver(this);

	closeAnyOpenPickerListFrames();

	if(axis_man != null)
	    axis_man.stopEditor();
	
	if(timer != null)
	    timer.stop();

	if(frame != null)
	    frame.setVisible(false);

	frame = null;
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
	
	frame.pack();
	frame.setVisible(true);

	resetView();
	setBoundaryValues();
	updateDisplay();
	
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
	PluginInfo pinf = new PluginInfo("Scatter Plot", "viewer", 
					 "Shows the corelation between pairs of Measurements or Spots", "", 1, 2, 1);
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

    private void buildMeasurementNameList(JComboBox jcb, int cur_val, ActionListener al, String bonus)
    {
	
	if(jcb == null)
	    return;	
	    
	// temporarily disable the action listeners
	if(al != null)
	    jcb.removeActionListener(al);
	
	String cur_str = (String) jcb.getSelectedItem();

	jcb.removeAllItems();
	
	if(bonus != null)
	    jcb.addItem(bonus);
	
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

	    if(cur_str != null)
		jcb.setSelectedItem(cur_str);

	    /*
	    if(bonus != null)
	    {
		int id = (cur_val == -1) ? 0 : (edata.getIndexOfMeasurement(cur_val) + 1);
		jcb.setSelectedIndex(id);
	    }
	    else
	    {
		jcb.setSelectedIndex(edata.getIndexOfMeasurement(cur_val));
		}
	    */
	}
	
	// re-enable the action listener
	if(al != null)
	    jcb.addActionListener(al);
    }

    protected void buildMeasurementNameLists(PlotDetails pd)
    {
	//buildMeasurementNameList(pd.x_meas_jcb, pd.x_meas, pd.x_meas_al, null);
	//buildMeasurementNameList(pd.y_meas_jcb, pd.y_meas, pd.y_meas_al, null);

	buildMeasurementNameList(pd.x_col_jcb,  pd.x_col,  pd.x_col_al,  "[fixed]");
	buildMeasurementNameList(pd.x_size_jcb, pd.x_size, pd.x_size_al, "[fixed]");
	buildMeasurementNameList(pd.y_size_jcb, pd.y_size, pd.y_size_al, "[fixed]");

    }
    protected void resyncMeasurementNames()
    {
	for(int p=0; p < pd_list.size(); p++)
	{
	    PlotDetails pd = (PlotDetails) pd_list.elementAt(p);

	    /*
	    pd.x_meas = edata.getMeasurementFromName( (String) pd.x_meas_jcb.getSelectedItem() );
	    pd.y_meas = edata.getMeasurementFromName( (String) pd.y_meas_jcb.getSelectedItem() );
	    */

	    try
	    {
		pd.x_size = edata.getMeasurementFromName( (String) pd.x_size_jcb.getSelectedItem() );
		pd.y_size = edata.getMeasurementFromName( (String) pd.y_size_jcb.getSelectedItem() );
		pd.x_col  = edata.getMeasurementFromName( (String) pd.x_col_jcb.getSelectedItem() );
	    }
	    catch( java.lang.NullPointerException npe )
	    {
	    }

	}
    }

    protected void buildMeasurementNameLists()
    {
	for(int p=0; p < pd_list.size(); p++)
	{
	    PlotDetails pd = (PlotDetails) pd_list.elementAt(p);
	    buildMeasurementNameLists(pd);
	}
    }

    protected JPanel makeToolbars( ) 
    {
	final JPanel outer_panel = new JPanel();
	outer_panel.setBorder( BorderFactory.createEmptyBorder( 3,3,3,3 ) );
	final GridBagLayout outer_gridbag = new GridBagLayout();
	outer_panel.setLayout( outer_gridbag );


	int tool_line = 0;


	GridBagLayout gridbag;
	JPanel tool_panel;
	JButton jb;
	JLabel label;
	Box.Filler filler;
	GridBagConstraints c;
	int pos = 0;

	Font smaller_font = mview.getSmallFont();

	// there are 3 toolbars
	//
	//  [mouse mode controls: zoom,pan,select] [viewing controls: reset,back]
	//  [labelling controls: label type, add, clear etc]
	//  [status line] and [print|help|close] buttons
      


	//
	//
	// ====== [mouse mode controls: zoom,pan,select] [viewing controls: reset,back] ==========================
	//
	//


	gridbag = new GridBagLayout();
	tool_panel = new JPanel();
	tool_panel.setBorder( BorderFactory.createEmptyBorder( 0,0,3,0 ) );
	tool_panel.setLayout( gridbag );
	pos = 0;


	label = new JLabel( "Mouse mode : " );
	label.setFont( smaller_font );
	c = new GridBagConstraints();
	c.gridx = pos++;
	gridbag.setConstraints( label, c );
	tool_panel.add( label );

	ButtonGroup bg = new ButtonGroup();


	JRadioButton jrb = new JRadioButton( "Zoom" );
	jrb.setToolTipText("Zoom the view point");
	smallifyComponent( jrb );
	jrb.setFont( smaller_font );
	jrb.setSelected( mouse_mode == MOUSE_ZOOM );
	jrb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    mouse_mode = MOUSE_ZOOM;
		}
	    });
	c = new GridBagConstraints();
	c.gridx = pos++;
	gridbag.setConstraints( jrb, c );
	tool_panel.add( jrb );
	bg.add( jrb );

	jrb = new JRadioButton( "Pan" );
	jrb.setToolTipText("Scroll the view point");
	smallifyComponent( jrb );
	jrb.setFont( smaller_font );
	jrb.setSelected( mouse_mode == MOUSE_PAN );
	jrb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    mouse_mode = MOUSE_PAN;
		}
	    });
	c = new GridBagConstraints();
	c.gridx = pos++;
	gridbag.setConstraints( jrb, c );
	tool_panel.add( jrb );
	bg.add( jrb );

	jrb = new JRadioButton( "Select" );
	jrb.setToolTipText("Draw a box to set the selected items");
	smallifyComponent( jrb );
	jrb.setFont( smaller_font );
	jrb.setSelected( mouse_mode == MOUSE_SELECT );
	jrb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    mouse_mode = MOUSE_SELECT;
		}
	    });
	c = new GridBagConstraints();
	c.gridx = pos++;
	c.anchor = GridBagConstraints.WEST;
	c.weightx = 0.1;
	gridbag.setConstraints( jrb, c );
	tool_panel.add( jrb );
	bg.add( jrb );


	filler = getFiller( 10 );
	c = new GridBagConstraints();
	c.gridx = pos++;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.weightx = 1.0;
	gridbag.setConstraints( filler, c );
	tool_panel.add( filler );



	JCheckBox jcb= new JCheckBox("Auto reset view  ");
	smallifyComponent( jcb );
	jcb.setFont( smaller_font );
	jcb.setToolTipText( "Automatically reset the view whenever the data changes" );
	jcb.setSelected(auto_reset);
	//jcb.setHorizontalTextPosition(AbstractButton.LEFT);
	jcb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    JCheckBox source = (JCheckBox) e.getSource();
		    auto_reset = source.isSelected();
		    
		}
	    });
	c = new GridBagConstraints();
	c.gridx = pos++;
	c.anchor = GridBagConstraints.EAST;
	c.weightx = 0.1;
	gridbag.setConstraints( jcb, c );
	tool_panel.add( jcb );


	filler = getFiller( 10 );
	c = new GridBagConstraints();
	c.gridx = pos++;
	gridbag.setConstraints( filler, c );
	tool_panel.add( filler );


	jb = new JButton("Reset view");
	smallifyComponent( jb );
	jb.setFont( smaller_font );
	jb.setToolTipText( "Reset the view so that all data points are displayed" );
	jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    resetView();
		    setBoundaryValues();
		    updateDisplay();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = pos++;
	gridbag.setConstraints( jb, c );
	tool_panel.add( jb );

	
	filler = getFiller( 10 );
	c = new GridBagConstraints();
	c.gridx = pos++;
	gridbag.setConstraints( filler, c );
	tool_panel.add( filler );


	back_jb = new JButton("Back");
	smallifyComponent( back_jb );
	back_jb.setFont( smaller_font );
	back_jb.setToolTipText( "Go back to the previous view" );
	back_jb.setEnabled(false);
	back_jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    goBack();
		    updateDisplay();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = pos++;
	gridbag.setConstraints( back_jb, c );
	tool_panel.add( back_jb );




	c = new GridBagConstraints();
	c.gridy = tool_line++;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.weightx = 10.0;
	outer_gridbag.setConstraints( tool_panel, c );
	outer_panel.add( tool_panel );


	//
	//
	// ====== [labelling controls: label type, add, clear etc] ==========================
	//
	//


	gridbag = new GridBagLayout();
	tool_panel = new JPanel();
	tool_panel.setBorder( BorderFactory.createEmptyBorder( 0,0,3,0 ) );
	tool_panel.setLayout( gridbag );
	pos = 0;

	
	label = new JLabel( "Labels : " );
	label.setFont( smaller_font );
	c = new GridBagConstraints();
	c.gridx = pos++;
	gridbag.setConstraints( label, c );
	tool_panel.add( label );


	nts = new NameTagSelector(mview);
	smallifyComponent( nts );
	nts.setFont( smaller_font );
	nts.loadSelection("ScatterPlot.name_tags");
	nt_sel = nts.getNameTagSelection();
	nts.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    nt_sel = nts.getNameTagSelection();
		    updateDisplay();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = pos++;
	gridbag.setConstraints( nts, c );
	tool_panel.add( nts );

	

	filler = getFiller( 5 );
	c = new GridBagConstraints();
	c.gridx = pos++;
	gridbag.setConstraints( filler, c );
	tool_panel.add( filler );

	
	final ButtonMenu bm = new ButtonMenu(mview, "Commands");
	smallifyComponent( bm );
	bm.setFont( smaller_font );

	JMenuItem jmi = null;
	
	jmi = new JMenuItem("Label all visible items");
	jmi.setToolTipText( "Add labels to all of the items which are currently visible on the graph" );
	bm.add(0, jmi);
	
	jmi = new JMenuItem("Clear all labels");
	jmi.setToolTipText( "Removes all labels from all items" );
	bm.add(1, jmi);
	
	jmi = new JMenuItem("Label all items in the current selection");
	jmi.setToolTipText( "Add labels to all of the items which are currently in the master selection" );
	bm.add(2, jmi);
	
	jmi = new JMenuItem("Select all labelled items");
	jmi.setToolTipText( "Add all of the currently labelled items to the master selection" );
	bm.add(3, jmi);
	
	bm.addButtonMenuListener( new ButtonMenu.Listener()
	    {
		public void menuItemSelected( int id, JMenuItem comp )
		{
		    boolean is_sel = bm.isSelected( id );
		    
		    // System.out.println( id + "=" + is_sel );
		    
		    switch(id)
		    {
			case 0:
			    showAllVisibleLabels();
			    updateDisplay();
			    break;
			case 1:
			    clearAllLabels();
			    updateDisplay();
			    break;
			case 2:
			    labelSelection();
			    updateDisplay();
			    break;
			case 3:
			    selectLabelled();
			    break;
		    }
		    
		    
		}
	    });

	c = new GridBagConstraints();
	c.gridx = pos++;
	c.fill = GridBagConstraints.VERTICAL;
	c.weightx = 0.1;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints( bm, c );
	tool_panel.add( bm );


	filler = getFiller( 10 );
	c = new GridBagConstraints();
	c.gridx = pos++;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.weightx = 0.1;
	gridbag.setConstraints( filler, c );
	tool_panel.add( filler );


	jb = new JButton("Axis Options");
	smallifyComponent( jb );
	jb.setToolTipText( "Opens the control panel for the plot axes" );
	jb.setFont( smaller_font );
	jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    axis_man.startEditor();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = pos++;
	gridbag.setConstraints( jb, c );
	tool_panel.add( jb );


	filler = getFiller( 5 );
	c = new GridBagConstraints();
	c.gridx = pos++;
	gridbag.setConstraints( filler, c );
	tool_panel.add( filler );


	jb = new JButton("Decoration Options");
	smallifyComponent( jb );
	jb.setToolTipText( "Opens the control panel for the plot decorations ( such as the title or legend )" );
	jb.setFont( smaller_font );
	jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    deco_man.startEditor();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = pos++;
	gridbag.setConstraints( jb, c );
	tool_panel.add( jb );



	c = new GridBagConstraints();
	c.gridy = tool_line++;
	c.weightx = 10.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	outer_gridbag.setConstraints( tool_panel, c );
	outer_panel.add( tool_panel );


	//
	//
	// ====== [status line] and [print|help|close] buttons ==========================
	//
	//


	gridbag = new GridBagLayout();
	tool_panel = new JPanel();
	tool_panel.setBorder( BorderFactory.createEmptyBorder( 0,0,0,0 ) );
	tool_panel.setLayout( gridbag );
	pos = 0;

/*
	nearest_label = new JLabel( "[ Pick a pair of Measurements or Spots ]" );
	c = new GridBagConstraints();
	c.gridx = pos++;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.anchor = GridBagConstraints.WEST;
	c.weightx = 6.0;
	gridbag.setConstraints( nearest_label, c );
	tool_panel.add( nearest_label );
	
	
	filler = getFiller( 5 );
	c = new GridBagConstraints();
	c.gridx = pos++;
	gridbag.setConstraints( filler, c );
	tool_panel.add( filler );
*/


	coords_label = new JLabel( "[ Pick a pair of Measurements or Spots ]" );
	c = new GridBagConstraints();
	c.gridx = pos++;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.anchor = GridBagConstraints.WEST;
	c.weightx = 3.0;
	gridbag.setConstraints( coords_label, c );
	tool_panel.add( coords_label );
 
   
	filler = getFiller( 10 );
	c = new GridBagConstraints();
	c.gridx = pos++;
	c.weightx = 1.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	gridbag.setConstraints( filler, c );
	tool_panel.add( filler );


	jb = new JButton("Print");
	jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    new PrintManager( mview, panel, panel ).openPrintDialog();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = pos++;
	c.anchor = GridBagConstraints.EAST;
	c.weightx = 0.1;
	gridbag.setConstraints( jb, c );
	tool_panel.add( jb );


	filler = getFiller( 10 );
	c = new GridBagConstraints();
	c.gridx = pos++;
	gridbag.setConstraints( filler, c );
	tool_panel.add( filler );


	jb = new JButton("Help");
	jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    mview.getPluginHelpTopic("ScatterPlot", "ScatterPlot");
		}
	    });
	c = new GridBagConstraints();
	c.gridx = pos++;
	c.anchor = GridBagConstraints.EAST;
	gridbag.setConstraints( jb, c );
	tool_panel.add( jb );


	filler = getFiller( 10 );
	c = new GridBagConstraints();
	c.gridx = pos++;
	gridbag.setConstraints( filler, c );
	tool_panel.add( filler );


	jb = new JButton("Close");
	jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    cleanUp();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = pos++;
	c.anchor = GridBagConstraints.EAST;
	gridbag.setConstraints( jb, c );
	tool_panel.add( jb );
	

	c = new GridBagConstraints();
	c.gridy = tool_line++;
	c.weightx = 10.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	outer_gridbag.setConstraints( tool_panel, c );
	outer_panel.add( tool_panel );


	return outer_panel;

    }
    

    private void smallifyComponent( JComponent jc )
    {
	if( jc instanceof AbstractButton ) 
	    (( AbstractButton) jc).setMargin( new Insets( 0,0,0,0 ) );
    }

    private Box.Filler getFiller( int size )
    {
	Dimension fillsize = new Dimension( size, size );
	return new Box.Filler(fillsize, fillsize, fillsize);
    }


    protected void buildPlotDetailControls() 
    {
	pd_panel.removeAll();

	GridBagConstraints c = null;
	GridBagLayout gridbag = new GridBagLayout();
	pd_panel.setLayout(gridbag);

	Font small_font = null;

	ImageIcon raise_ii = new ImageIcon(mview.getImageDirectory() + "raise.gif");
	ImageIcon lower_ii = new ImageIcon(mview.getImageDirectory() + "lower.gif");
	ImageIcon delete_ii = new ImageIcon(mview.getImageDirectory() + "delete.gif");
	ImageIcon clone_ii = new ImageIcon(mview.getImageDirectory() + "clone.gif");


	for(int p=0; p < pd_list.size(); p++)
	{
	    final PlotDetails pd = (PlotDetails) pd_list.elementAt(p);

	    // ---------

	    Insets ins = new Insets(1,1,1,1);

	    JPanel cntrls = new JPanel();
	    cntrls.setBorder(BorderFactory.createEtchedBorder());
	    GridBagLayout cgridbag = new GridBagLayout();
	    cntrls.setLayout(cgridbag);
	    JButton jb = null;
	    
	    boolean both = ((p > 0) && ((p+1) < pd_list.size()));

	    if(p > 0)
	    {
		jb = new JButton(raise_ii);
		jb.setMargin(ins);
		jb.addActionListener(new PlotDetailListener(pd, 30)); 
		jb.setToolTipText("Raise this plot");
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = both ? 1 : 2;
		cgridbag.setConstraints(jb, c);
		cntrls.add(jb);
	    }

	    if((p+1) < pd_list.size())
	    {
		jb = new JButton(lower_ii);
		jb.setMargin(ins);
		jb.setToolTipText("Lower this plot");
		jb.addActionListener(new PlotDetailListener(pd, 40)); 
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = both ? 1 : 0;
	        c.gridheight = both ? 1 : 2;
		cgridbag.setConstraints(jb, c);
		cntrls.add(jb);
	    }

	    jb = new JButton(clone_ii);
	    jb.setMargin(ins);
	    jb.addActionListener(new PlotDetailListener(pd, 50)); 
	    jb.setToolTipText("Add a new plot");
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    cgridbag.setConstraints(jb, c);
	    cntrls.add(jb);

	    if(pd_list.size() > 1)
	    {
		jb = new JButton(delete_ii);
		jb.setMargin(ins);
		jb.addActionListener(new PlotDetailListener(pd, 20)); 
		jb.setToolTipText("Remove this plot");
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 1;
		cgridbag.setConstraints(jb, c);
		cntrls.add(jb);
	    }

	    // ---------

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = p;
	    //c.weightx = 1.0;
	    //c.weighty = 1.0;
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    //c.weightx = c.weighty = 1.0;
	    gridbag.setConstraints(cntrls, c);
	    pd_panel.add(cntrls);


	    // --------- -------- ---------- --------- --------- ---------

	    JPanel wrapper = new JPanel();
	    wrapper.setBorder(BorderFactory.createEtchedBorder());
	    GridBagLayout igridbag = new GridBagLayout();
	    wrapper.setLayout(igridbag);

	    // if(pd.measurements)
	    ButtonGroup bg = new ButtonGroup();
	    pd.spot_mode = new JRadioButton("Spot");
	    pd.spot_mode.setSelected( !pd.measurements );
	    pd.spot_mode.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			pd.measurements = pd.meas_mode.isSelected(); //!(pd.spot_mode.isSelected());
			displayPickerLists(pd);
			//resetView();
			//setBoundaryValues();
			//updateDisplay();
		    }
		});
	    bg.add(pd.spot_mode);
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    igridbag.setConstraints(pd.spot_mode, c);
	    wrapper.add(pd.spot_mode);

	    pd.meas_mode = new JRadioButton("Meas");
	    pd.meas_mode.setSelected( pd.measurements );
	    pd.meas_mode.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			pd.measurements = pd.meas_mode.isSelected();
			displayPickerLists(pd);
			
		    }
		});
	    bg.add(pd.meas_mode);
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    igridbag.setConstraints(pd.meas_mode, c);
	    wrapper.add(pd.meas_mode);

	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = p;
	    //c.weightx = 9.0;
	    c.weighty = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    c.anchor = GridBagConstraints.WEST;
	    //c.weightx = c.weighty = 1.0;
	    gridbag.setConstraints(wrapper, c);
	    pd_panel.add(wrapper);
    

	    // --------- -------- ---------- --------- --------- ---------

	    int row = 0;

	    wrapper = new JPanel();
	    wrapper.setBorder(BorderFactory.createEtchedBorder());
	    igridbag = new GridBagLayout();
	    wrapper.setLayout(igridbag);

	    JLabel label = new JLabel(" x pos ");

	    c = new GridBagConstraints();
	    c.gridx = row++;
	    c.gridy = 0;
	    igridbag.setConstraints(label, c);
	    wrapper.add(label);

	    pd.x_thing_jb = new JButton( (pd.x_thing_name == null) ? "[click here to select a Measurement]" : pd.x_thing_name);
	    pd.x_thing_jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			displayPickerLists(pd);
		    }
		});
	    //pd.x_meas_jcb = new JComboBox();
	    //pd.x_meas_al  = new PlotDetailListener(pd, 0);
	   
	    c = new GridBagConstraints();
	    c.gridx = row++;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.fill =  GridBagConstraints.HORIZONTAL;
	    igridbag.setConstraints(pd.x_thing_jb, c);
	    wrapper.add(pd.x_thing_jb);
	    
	    
	    label = new JLabel(" size ");
	    c = new GridBagConstraints();
	    c.gridx = row++;
	    c.gridy = 0;
	    igridbag.setConstraints(label, c);
	    wrapper.add(label);
    
	    pd.x_size_jcb = new JComboBox();
	    pd.x_size_al  = new PlotDetailListener(pd, 2);
	    //x_size_jcb.addActionListener(new ModeNameListener(true));
	    c = new GridBagConstraints();
	    c.gridx = row++;
	    c.gridy = 0;
	    igridbag.setConstraints(pd.x_size_jcb, c);
	    wrapper.add(pd.x_size_jcb);
	    
	    label = new JLabel(" x ");
	    c = new GridBagConstraints();
	    c.gridx = row++;
	    c.gridy = 0;
	    igridbag.setConstraints(label, c);
	    wrapper.add(label);

	    pd.x_scale_jtf = new JTextField(3);
	    pd.x_scale_jtf.setText(String.valueOf(pd.x_scale));
	    pd.x_scale_jtf.addActionListener(new PlotDetailListener(pd, 6)); 
	    //x_size_jcb.setSelectedIndex(0);
	    c = new GridBagConstraints();
	    c.gridx = row++;
	    c.gridy = 0;
	    igridbag.setConstraints(pd.x_scale_jtf, c);
	    wrapper.add(pd.x_scale_jtf);

	    label = new JLabel(" colour ");
	    c = new GridBagConstraints();
	    c.gridx = row++;
	    c.gridy = 0;
	    //c.gridheight = 2;
	    igridbag.setConstraints(label, c);
	    wrapper.add(label);

	    pd.x_col_jcb = new JComboBox();
	    pd.x_col_al  = new PlotDetailListener(pd, 4);

	    //x_col_jcb.addActionListener(new BinNameListener(true));
	    //x_col_jcb.setSelectedIndex(1);
	    c = new GridBagConstraints();
	    c.gridx = row++;
	    c.gridy = 0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    //c.gridheight = 2;
	    igridbag.setConstraints(pd.x_col_jcb, c);
	    wrapper.add(pd.x_col_jcb);
    
	    
	    pd.fixed_col_jb = new JButton();
	    ins = new Insets(3,3,3,3);
	    pd.fixed_col_jb.setMargin(ins);
	    pd.fixed_col_jb.addActionListener(new PlotDetailListener(pd, 10)); 
	    c = new GridBagConstraints();
	    c.gridx = row++;
	    c.gridy = 0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    igridbag.setConstraints(pd.fixed_col_jb, c);
	    wrapper.add(pd.fixed_col_jb);
	    // pd.fixed_col_jb.setVisible(false);
	    
	    // ===================================================================

	    row = 0;

	    label = new JLabel(" y pos ");
	    c = new GridBagConstraints();
	    c.gridx = row++;
	    c.gridy = 1;
	    igridbag.setConstraints(label, c);
	    wrapper.add(label);
    
	    //pd.y_meas_jcb = new JComboBox();
	    //pd.y_meas_al  = new PlotDetailListener(pd, 1);
	    pd.y_thing_jb = new JButton( (pd.y_thing_name == null) ? "[click here to select a Measurement]" : pd.y_thing_name);
	    pd.y_thing_jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			displayPickerLists(pd);
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = row++;
	    c.gridy = 1;
	    c.weightx = 1.0;
	    c.fill =  GridBagConstraints.HORIZONTAL;
	    igridbag.setConstraints(pd.y_thing_jb, c);
	    wrapper.add(pd.y_thing_jb);

	    label = new JLabel(" size ");
	    c = new GridBagConstraints();
	    c.gridx = row++;
	    c.gridy = 1;
	    igridbag.setConstraints(label, c);
	    wrapper.add(label);
    
	    pd.y_size_jcb = new JComboBox();
	    pd.y_size_al  = new PlotDetailListener(pd, 3);
	    //y_size_jcb.addActionListener(new ModeNameListener(false));
	    //y_size_jcb.setSelectedIndex(0);
	    c = new GridBagConstraints();
	    c.gridx = row++;
	    c.gridy = 1;
	    igridbag.setConstraints(pd.y_size_jcb, c);
	    wrapper.add(pd.y_size_jcb);
    
	    label = new JLabel(" x ");
	    c = new GridBagConstraints();
	    c.gridx = row++;
	    c.gridy = 1;
	    igridbag.setConstraints(label, c);
	    wrapper.add(label);

	    pd.y_scale_jtf = new JTextField(3);
	    pd.y_scale_jtf.setText(String.valueOf(pd.y_scale));
	    pd.y_scale_jtf.addActionListener(new PlotDetailListener(pd, 7)); 
	    //x_size_jcb.addActionListener(new ModeNameListener(true));
	    //x_size_jcb.setSelectedIndex(0);
	    c = new GridBagConstraints();
	    c.gridx = row++;
	    c.gridy = 1;
	    igridbag.setConstraints(pd.y_scale_jtf, c);
	    wrapper.add(pd.y_scale_jtf);

	    label = new JLabel(" shape ");
	    c = new GridBagConstraints();
	    c.gridx = row++;
	    c.gridy = 1;
	    //c.gridheight = 2;
	    igridbag.setConstraints(label, c);
	    wrapper.add(label);

	    final String[] shapes = { "Oval", "Rectangle", "Diamond", "Crosshair" };
	    pd.shape_jcb = new JComboBox(shapes);
	    pd.shape_jcb.setSelectedIndex(pd.shape);
	    pd.shape_jcb.addActionListener( new PlotDetailListener(pd, 5) );

	    //x_col_jcb.addActionListener(new BinNameListener(true));
	    //x_col_jcb.setSelectedIndex(1);
	    c = new GridBagConstraints();
	    c.gridx = row++;
	    c.gridy = 1;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    //c.gridheight = 2;
	    igridbag.setConstraints(pd.shape_jcb, c);
	    wrapper.add(pd.shape_jcb);
	    
	    
	    pd.solid_jchkb = new JCheckBox("Solid");
	    pd.solid_jchkb.setSelected( pd.solid );
	    pd.fixed_col_jb.setBackground(pd.fixed_col);
	    pd.fixed_col_jb.setForeground(pd.fixed_col);
	    pd.solid_jchkb.addActionListener( new PlotDetailListener(pd, 8) );

	    c = new GridBagConstraints();
	    c.gridx = row++;
	    c.gridy = 1;
	    igridbag.setConstraints(pd.solid_jchkb, c);
	    wrapper.add(pd.solid_jchkb);

	    // ----------------------------

	    JPanel cb_panel = new JPanel();
	    cb_panel.setBorder(BorderFactory.createEmptyBorder(0,15,0,0));
	    GridBagLayout cb_gridbag = new GridBagLayout();
	    cb_panel.setLayout(cb_gridbag);

	    /*
	    label = new JLabel(" bins ");
	    c = new GridBagConstraints();
	    c.gridx = 6;
	    c.gridy = 1;
	    igridbag.setConstraints(label, c);
	    wrapper.add(label);
	    */

	    ins = new Insets(0,0,0,0);

	    JCheckBox jcb= new JCheckBox("Apply filter");
	    
	    if(small_font == null)
	    {
		Font font = jcb.getFont();
		small_font = new Font(font.getName(), Font.PLAIN, font.getSize()-1);
	    }

	    jcb.setFont(small_font);
	    jcb.setMargin(ins);
	    jcb.setSelected(pd.use_filter);
	    jcb.addActionListener(new PlotDetailListener(pd, 15));
	    
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.WEST;
	    cb_gridbag.setConstraints(jcb, c);
	    cb_panel.add(jcb);
	    
	    jcb = new JCheckBox("Show clusters");
	    jcb.setMargin(ins);
	    jcb.setFont(small_font);
	    jcb.setSelected(pd.show_clusters);
	    jcb.addActionListener(new PlotDetailListener(pd, 16));
	    
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.anchor = GridBagConstraints.WEST;
	    cb_gridbag.setConstraints(jcb, c);
	    cb_panel.add(jcb);

	    jcb = new JCheckBox("...and edges");
	    jcb.setMargin(ins);
	    jcb.setFont(small_font);
	    jcb.setSelected(pd.show_edges);
	    jcb.addActionListener(new PlotDetailListener(pd, 17));
	    
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 2;
	    c.anchor = GridBagConstraints.WEST;
	    cb_gridbag.setConstraints(jcb, c);
	    cb_panel.add(jcb);

	    // ---------

	    c = new GridBagConstraints();
	    c.gridx = 9;
	    c.gridy = 0;
	    c.gridheight = 2;
	    c.anchor = GridBagConstraints.EAST;
	    igridbag.setConstraints(cb_panel, c);
	    wrapper.add(cb_panel);

	    // ----------------------------

	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = p;
	    c.weightx = 9.0;
	    //c.weighty = 1.0;
	    c.fill = GridBagConstraints.BOTH; //HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    //c.weightx = c.weighty = 1.0;
	    gridbag.setConstraints(wrapper, c);
	    pd_panel.add(wrapper);
    
	    buildMeasurementNameLists(pd);

	}
	pd_panel.updateUI();
    }
    
    // handles the Measurement choice combo boxen
    class PlotDetailListener implements ActionListener
    { 
	public PlotDetailListener(PlotDetails pd_, int id_) 
	{ 
	    super();
	    pd = pd_;
	    id = id_;
	}

	public void actionPerformed(ActionEvent e) 
	{
	    int new_meas_index = -1;

	    
	    if(id < 5)
	    {
		JComboBox cb = (JComboBox)e.getSource();
		String new_name = (String)cb.getSelectedItem();
		if( new_name != "[fixed]" )
		{
		    new_meas_index = edata.getMeasurementFromName(new_name);
		}
	    }
	    
	    switch( id )
	    {
	    /*
	    case 0:
		pd.x_meas = new_meas_index;
		if(auto_reset)
		{
		    resetView();
		    setBoundaryValues();
		}
		break;

	    case 1:
		pd.y_meas = new_meas_index;
		if(auto_reset)
		{
		    resetView();
		    setBoundaryValues();
		}
		break;
		*/

	    case 2:
		/*
		if(new_meas_index > 0)
		    x_size = new_meas_index - 1;
		else
		    x_size = -1;
		*/
		pd.x_size = new_meas_index;
		break;

	    case 3:
		/*
		if(new_meas_index > 0)
		    y_size = new_meas_index - 1;
		else
		    y_size = -1;
		*/
		pd.y_size = new_meas_index;
		break;

	    case 4: // colour 

		if(new_meas_index >= 0)
		{
		    pd.x_col = new_meas_index;
		    pd.fixed_col_jb.setVisible(false);
		}
		else
		{
		    pd.x_col = -1;
		    pd.fixed_col_jb.setVisible(true);
		}
		break;

	    case 5: // shape
		pd.shape = pd.shape_jcb.getSelectedIndex();
		break;
	    case 6:
	    case 7:
		try
		{
		    pd.x_scale = new Double(pd.x_scale_jtf.getText()).doubleValue();
		    pd.y_scale = new Double(pd.y_scale_jtf.getText()).doubleValue();
		}
		catch(NumberFormatException nfe)
		{
		}
		break;

	    case 8:
		pd.solid = pd.solid_jchkb.isSelected();
		break;

	    case 10: // fixed colour selector...
		Color new_colour = JColorChooser.showDialog(frame,
							    "Choose Colour",
							    pd.fixed_col);
		if(new_colour != null) 
		{

		    pd.fixed_col = new_colour;
		    pd.fixed_col_jb.setBackground(pd.fixed_col);
		    pd.fixed_col_jb.setForeground(pd.fixed_col);
		}
		break;

	    case 15:   // apply _filter
		pd.use_filter = ((JCheckBox) e.getSource()).isSelected();
		setBoundaryValues();
		break;
	    case 16:
		pd.show_clusters = ((JCheckBox) e.getSource()).isSelected();
		break;
	    case 17:
		pd.show_edges = ((JCheckBox) e.getSource()).isSelected();
		break;

	    case 20: // delete plot
		removePlotDetails(pd);
		break;
	    case 30: // raise plot
		raisePlotDetails(pd);
		break;
	    case 40: // lower plot
		lowerPlotDetails(pd);
		break;
	    case 50: // clone plot
		clonePlotDetails(pd);
		break;
	    }

	    updateDisplay();

	}
	private PlotDetails pd;
	private int id;
    }

    public class PositionUpdater extends Thread
    {
	int x,y;
	boolean alive = true;
	Vector job_v = new Vector();

	public class UpdateJob { public int x, y; }

	public void run()
	{
	    while(alive)
	    {
		UpdateJob uj = getJob();

		if(uj == null)
		{
		    try
		    {
		       Thread.sleep(100);
		    }
		    catch(InterruptedException ie)
		    {
		    }
		}
		else
		{
		    update(uj.x, uj.y);
		}
	    }
	}
	
	public synchronized UpdateJob getJob()
	{
	    if(job_v.size() == 0)
		return null;
	    UpdateJob uj = (UpdateJob) job_v.elementAt(0);
	    job_v.removeElementAt(0);

	    //System.out.println("job removed (at " + uj.x + "," + uj.y + ")");

	    return uj;
	}
	public synchronized void addJob(int x, int y)
	{
	    UpdateJob uj = new UpdateJob();
	    uj.x = x; uj.y = y;
	    job_v.removeAllElements();
	    job_v.addElement(uj);

	    //System.out.println("job added (at " + x + "," + y + ")");

	}
	public synchronized int jobCount()
	{
	    return job_v.size();
	}

	public void update(int x, int y)
	{
	    //System.out.println("update mouse posn at " + x + "," + y);

	    // if there are any more jobs in the queue then this one can be ditched...
	    if(jobCount()  == 0)
	    {
		double xval = fromScreenX(x);
		double yval = fromScreenY(y);
		
		// System.out.println("x pos=" + x + " -> " + xval );
		
		String xstr = String.valueOf(xval);
		String ystr = String.valueOf(yval);
		
		xstr = xstr.substring(0, ((  xstr.length() > 7) ? 7 :   xstr.length()));
		ystr = ystr.substring(0, ((  ystr.length() > 7) ? 7 :   ystr.length()));
		
		coords_string = "(" + xstr + ", " +  ystr + ")";
		
		// find the nearest spot using  bin Vectors
		
		if((x_to_bin == 0) || (y_to_bin == 0))
		    return;

		int bx = x / x_to_bin;
		int by = y / y_to_bin;

		nearest_spot = null;

		String n_str = null;

		if((bx >= 0) && (by >= 0) &&
		   (bx < n_spot_bins) && (by < n_spot_bins))
		{
		    Vector v = spot_bins[by][bx];
		    //str += " size=" + ((v == null) ? "0" : String.valueOf(v.size()));
		    if(v != null)
		    {
			BlotData bd = findNearestBlot(x, y, v);

			if(bd != null)
			{
			    if(bd.is_meas)
				n_str = edata.getMeasurementName(bd.id);
			    else
				n_str = nt_sel.getNameTag(bd.id);

			    // System.out.println(e.getX() + "," +  e.getY() + " bin=" + bx + "," + by + " nearest=" + id );
			    
			    nearest_spot = bd;
			}
		    }
		    
		    if(nearest_spot == null)
		    {
			if( ignored_info != null )
			    coords_label.setText( coords_string + " : " + ignored_info + " not plotted (coordinates were 'Infinity' or 'NaN')");
			else
			    coords_label.setText( coords_string );

			panel.setToolTipText("");
		    }
		    else
		    {
			if(n_str == null)
			    n_str = "(no label)";

			coords_label.setText( coords_string + " : " + n_str);
			panel.setToolTipText( n_str );
		    }

		    //coords_label.setText( coords_string );
		}
	    }
	}
    }

    PositionUpdater pu = null;
    private BlotData nearest_spot = null;

    public synchronized void updatePosition(int x, int y)
    {
	if(pu == null)
	{
	    pu = new PositionUpdater();
	    pu.start();
	}
	pu.addJob(x,y);
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
	//System.out.println("it's a data event....: id=" + due.event);

	switch(due.event)
	{
	case ExprData.RangeChanged:
	case ExprData.SizeChanged:
	    sel_spot_back_map = null;
	    if(auto_reset)
		setBoundaryValues();
	    updateDisplay();
	    break;

	case ExprData.NameChanged:
	    sel_spot_back_map = null;
	    updateDisplay();
	    break;

	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	    if(auto_reset)
		setBoundaryValues();
	    sel_spot_back_map = null;
	    updateDisplay();
	    break;

	case ExprData.ColourChanged:
	case ExprData.ValuesChanged:
	    updateDisplay();
	    break;

	case ExprData.VisibilityChanged:
	    panel.repaintDisplay();
	    break;

	case ExprData.OrderChanged:
	    // nothink to do....
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
	    panel.repaintDisplay();
	    break;
	}
    }

    public void measurementUpdate(ExprData.MeasurementUpdateEvent mue)
    {
	switch(mue.event)
	{
	case ExprData.SizeChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	case ExprData.NameChanged:
	    buildMeasurementNameLists();
	    resyncMeasurementNames();
	    updateDisplay();
	    break;
	case ExprData.VisibilityChanged:
	case ExprData.ColourChanged:
	    updateDisplay();
	    break;
	case ExprData.OrderChanged:
	    initialise();
	    buildMeasurementNameLists();
	    resyncMeasurementNames();
	    panel.repaintDisplay();
	    break;
	case ExprData.ValuesChanged:
	    // indicates a change of data_type
	    //
	    panel.repaintDisplay();
	}	
    }
    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
	background_col = mview.getBackgroundColour();
	text_col       = mview.getTextColour();
	
	updateDisplay();
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  layout
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

   // force a repaint
    private void updateDisplay()
    {
	/*
	System.out.println("updateDisplay():" + 
			   String.valueOf(x_meas_min) + " , " + 
			   String.valueOf(y_meas_min) + " -> " +
			   String.valueOf(x_meas_max) + " , " + 
			   String.valueOf(y_meas_max));
	*/

	spot_image = null;

	if(panel != null)
	    panel.repaintDisplay();
    }

    //private double x_norm, y_norm;
    //private double x_d, y_d;
    //private double d_w, d_h;
    //private double i_d_w, i_d_h;
    
    private void axesHaveChanged(  )
    {
	// System.out.println("axesHaveChanged...");

	x_meas_min = axis_man.getMin(0);
	x_meas_max = axis_man.getMax(0);
	
	y_meas_min = axis_man.getMin(1);
	y_meas_max = axis_man.getMax(1);

	spot_image = null;

	panel.repaintDisplay();

	//System.out.println("axesHaveChanged(): x_min=" + x_meas_min + " x_max=" + x_meas_max + " y_min=" + y_meas_min + " y_max=" + y_meas_max );

    }

    private void updateTransform( int w, int h )
    {
	// TODO: most of this is not used now, having been moved to PlotAxis

	//x_d = x_meas_max - x_meas_min;
	//y_d = y_meas_max - y_meas_min;
	
	// x_norm = 1.0 / (x_d);
        //y_norm = 1.0 / (y_d);
        
        // scaled_width = (double) w  - (2 * gap);
        // scaled_height = (double) h - (2 * gap);

	scaled_width  = ((double) w) * axis_man.getLength(0);
	scaled_height = ((double) h) * axis_man.getLength(1);

	x_gap = (w - (int)scaled_width) / 2;
	y_gap = (h - (int)scaled_height) / 2;

	//System.out.println("scaled_width=" + scaled_width + " scaled_height=" + scaled_height +
	//		   " x_gap=" + x_gap + " y_gap=" + y_gap);

        //i_d_w = 1.0 / d_w;
        //i_d_h = 1.0 / d_h;
    }


    private final int toScreenX(double v)
    {
	return x_gap + (int) axis_man.toScale( 0, v, scaled_width );
    }
    private final int toScreenY(double v)
    {
	return y_gap + (int) (scaled_height - axis_man.toScale( 1, v, scaled_height ));
    }
    
    private final double fromScreenX(int p)
    {
	return axis_man.fromScale( 0, (double) (p -  x_gap), scaled_width );

    }
    private final double fromScreenY(int p)
    {
	return axis_man.fromScale( 1, scaled_height - (double) (p -  y_gap), scaled_height );
    }

    /*
    private int toScreenX(double v)
    {
        return x_gap + (int)(((v - x_meas_min) * x_norm) * d_w);
    }
    private int toScreenY(double v)
    {
        return y_gap + (int)(d_h - (((v - y_meas_min) * y_norm) * d_h));
    }
    
    private double fromScreenX(int p)
    {
        //if((p >= gap) && (p < ( panel.getWidth()-gap)))
        {
    	return (((double)(p - x_gap) * i_d_w) * x_d) + x_meas_min;
        }
        //else
    	//return Double.NaN;
    }
    private double fromScreenY(int p)
    {
        //if((p >= gap) && (p < ( panel.getHeight()-gap)))
        {
    	return ((1.0 - ((double)(p - y_gap) * i_d_h)) * y_d) + y_meas_min;
        }
        //else
	// return Double.NaN;
    }
    */

    final int n_spot_bins = 10;
    int x_to_bin = 1;
    int y_to_bin = 1;
    Vector[][] spot_bins = null;
	

    // --------------------------------------

    public class Range
    {
	public double x_meas_min, x_meas_max;
	public double y_meas_min, y_meas_max;
    }

    public void resetView()
    {
	x_meas_min = y_meas_min = (Double.MAX_VALUE);
	x_meas_max = y_meas_max = -Double.MAX_VALUE;
    }

    Vector history = new Vector();


    public void goBack()
    {
	System.out.println( history.size() + " things in buffer" );

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
	}
    }


    public String getTitle( int axis )
    {
	String name = "";

	for(int p=0; p < pd_list.size(); p++)
	{
	    PlotDetails pd = (PlotDetails) pd_list.elementAt( p );

	    if( name.length() > 0 )
		name += " ";

	    if( axis == 0 )
	    {
		if( pd.x_thing_name != null )
		    name += (String) pd.x_thing_name;
	    }
	    else
	    {
		if( pd.y_thing_name != null )
		    name += (String) pd.y_thing_name;
	    }
	}

	return name;
    }

    public void setBoundaryValues()
    {
	// resetView();

	for(int p=0; p < pd_list.size(); p++)
	{
	    PlotDetails pd = (PlotDetails) pd_list.elementAt(p);

	    boolean first_plot = false;

	    if(pd.measurements)
	    {
		if((pd.x_meas >= 0) && (pd.y_meas >= 0))
		{
		    double[] x_result, y_result;

		    if( pd.use_filter == false) 
		    {
			x_result = new double[] { edata.getMeasurementMinEValue(  pd.x_meas ), edata.getMeasurementMaxEValue( pd.x_meas ) };
			y_result = new double[] { edata.getMeasurementMinEValue(  pd.y_meas ), edata.getMeasurementMaxEValue( pd.y_meas ) };
		    }
		    else
		    {
			x_result = getFilteredMinMax( pd.x_meas );
			y_result = getFilteredMinMax( pd.y_meas );
		    }

		    if(  first_plot )
		    {
			x_meas_min = x_result[ 0 ];
			x_meas_max = x_result[ 1 ];

			y_meas_min = y_result[ 0 ];
			y_meas_max = y_result[ 1 ];

			first_plot = false;
		    }
		    else
		    {
			if( x_result[ 0 ] < x_meas_min )
			    x_meas_min = x_result[ 0 ];
			if( x_result[ 1 ] > x_meas_max )
			    x_meas_max = x_result[ 1 ];
			
			if( y_result[ 0 ] < y_meas_min )
			    y_meas_min = y_result[ 0 ];
			if( y_result[ 1 ] > y_meas_max )
			    y_meas_max = y_result[ 1 ];
		    }
		}
	    }
	    else
	    {
		double tmp = edata.getMaxEValue();
		if(tmp > x_meas_max)
		    x_meas_max = tmp;
		if(tmp > y_meas_max)
		    y_meas_max = tmp;

		tmp = edata.getMinEValue();
		if(tmp < x_meas_min)
		    x_meas_min = tmp;
		if(tmp < y_meas_min)
		    y_meas_min = tmp;
	    }
	}


	// update the two PlotAxis objects

	axis_man.setComputedRange(0, x_meas_min, x_meas_max);
	axis_man.setComputedRange(1, y_meas_min, y_meas_max);

	// and read back the values 
	// (which might be user specified, or the values we just inserted)
	//
	x_meas_min = axis_man.getMin(0);
	x_meas_max = axis_man.getMax(0);
	
	y_meas_min = axis_man.getMin(1);
	y_meas_max = axis_man.getMax(1);
	
    }


    
    private double[] getFilteredMinMax( int meas_id )
    {
	double min = Double.NaN;
	double max = Double.NaN;
	
	{
	    boolean first_spot = true;
	    final int ns = edata.getNumSpots();
	    final double[] data = edata.getMeasurement( meas_id ).getData();

	    for(int s=0; s < ns ; s++)
	    {
		if( edata.filter( s ) == false )
		{
		    final double value = data[ s ];

		    if( first_spot )
		    {
			if( ! Double.isNaN( value ) && ! Double.isInfinite( value ) )
			{
			    min = max = value;
			    first_spot = false;
			}
		    }
		    else
		    {
			if( ! Double.isNaN( value ) && ! Double.isInfinite( value ) )
			{
			    if( data[ s ] < min )
				min = value;
			    if( data[ s ] > max )
				max = value;
			}
		    }
		}
	    }
	}

	double[] result = new double[2];
	result[0] = min;
	result[1] = min;
	
	return new double[] { min, max };
    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   spot labels
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private class BlotData 
    { 
	public int x, y, id;
	public boolean is_meas;
	public BlotData(int x_, int y_, int id_, boolean is_meas_)
	{
	    x = x_; y = y_; id = id_;
	    is_meas = is_meas_;
	}
    }
    
    public BlotData findNearestBlot(int x, int y, Vector v)
    {
	
	final int dist_threshold = ( 4 * 4 );
	BlotData nearest = null;
	int n_dist = dist_threshold;
	
	for(int s=0; s < v.size(); s++)
	{
	    BlotData sd = (BlotData) v.elementAt(s);
	    
	    int dist = ((sd.x - x) * (sd.x - x)) + ((sd.y - y) * (sd.y - y));
	    
	    if(nearest == null)
	    {
		n_dist = dist;
		nearest = sd;
	    }
	    else
	    {
		if(dist < n_dist)
		{
		    n_dist = dist;
		    nearest = sd;
		}
	    }
	}

	if(n_dist < dist_threshold)
	    return nearest;
	else
	    return null;
    }
    
    private HashSet show_spot_label = new HashSet();
    private HashSet show_meas_label = new HashSet();
    
    final String dummy = "x";

    public void showAllVisibleLabels()
    {
	// use the hit bins....
	
	for(int x=0; x < n_spot_bins; x++)
	{
	    for(int y=0; y < n_spot_bins; y++)
	    {
		 Vector v = spot_bins[y][x];
		  if(v != null)
		  {
		      for(int z=0; z < v.size(); z++)
		      {
			  BlotData sd = (BlotData) v.elementAt(z);
			  if(!sd.is_meas)
			      showSpotLabel(sd.id);
			  else
			      showMeasLabel(sd.id);
		      }
		  }
	    }
	}
	    
    }


    public void showMeasLabel(int id)
    {
	// if(show_spot_label.get(i) == null)
	{
	   Integer i = new Integer(id);
	   show_meas_label.add(i);
	}
    }


    public void showSpotLabel(int id)
    {
	// if(show_spot_label.get(i) == null)
	{
	   Integer i = new Integer(id);
	   show_spot_label.add(i);
	}
    }

    public void clearAllLabels()
    {
	show_spot_label = new HashSet();
	show_meas_label = new HashSet();
    }

    public void labelSelection()
    {
	int[] sels = edata.getSpotSelection();
		
	if(sels != null)
	{		
	    for(int p=0; p < pd_list.size(); p++)
	    {
		PlotDetails pd = (PlotDetails) pd_list.elementAt(p);
		
		if(pd.meas_mode.isSelected())
		{
		    // System.out.println("beep");

		    for(int s=0; s < sels.length; s++)
		    {
			show_spot_label.add(new Integer(sels[s]));
		    }
		}
	    }
	}

	sels = edata.getMeasurementSelection();
		
	if(sels != null)
	{		
	    for(int p=0; p < pd_list.size(); p++)
	    {
		PlotDetails pd = (PlotDetails) pd_list.elementAt(p);
		
		if(pd.spot_mode.isSelected())
		{
		    // System.out.println("beep");

		    for(int m=0; m < sels.length; m++)
		    {
			show_meas_label.add(new Integer(sels[m]));
		    }
		}
	    }
	}

    }

    public void selectLabelled()
    {
	if( show_spot_label.size() > 0 )
	{
	    int[] sel_ids = new int[ show_spot_label.size() ];
	    
	    int index = 0;
	    
	    for (Iterator i = show_spot_label.iterator(); i.hasNext() ;) 
	    {
		Integer id = (Integer) i.next();
		
		sel_ids[ index++] = id.intValue();
	    }
	    
	    edata.addToSpotSelection( sel_ids );
	}
	if( show_meas_label.size() > 0 )
	{
	    int[] sel_ids = new int[ show_meas_label.size() ];
	    
	    int index = 0;
	    
	    for (Iterator i = show_meas_label.iterator(); i.hasNext() ;) 
	    {
		Integer id = (Integer) i.next();
		
		sel_ids[ index++] = id.intValue();
	    }
	    
	    edata.addToMeasurementSelection( sel_ids );
	}

    }

    public void toggleSpotLabel(int id)
    {
	Integer i = new Integer(id);
	final String dummy = "x";
	
	if(!show_spot_label.contains(i))
	{
	    show_spot_label.add(i);
	}
	else
	{
	    show_spot_label.remove(i);
	}
	
	panel.repaintDisplay();
    }
    
    public void toggleMeasLabel(int id)
    {
	Integer i = new Integer(id);
	final String dummy = "x";
	
	if(!show_meas_label.contains(i))
	{
	    show_meas_label.add(i);
	}
	else
	{
	    show_meas_label.remove(i);
	}
		
	panel.repaintDisplay();
    }
    
    // note: expects screen coords
    public void selectItemsInRectangle( double sx_min, double sy_min, double sx_max, double sy_max )
    {
	System.out.println("checking " + sx_min + "," + sy_min + " ..." + sx_max + "," + sy_max );

	// get the collection of things within this bounding box...

	Vector meas_hits_v = new Vector();
	Vector spot_hits_v = new Vector();
	
	for(int bx = 0; bx < n_spot_bins; bx++ )
	{
	    for(int by = 0; by < n_spot_bins; by++ )
	    {
		Vector v = spot_bins[by][bx];

		if( v != null )
		{
		    for(int s=0; s < v.size(); s++)
		    {
			BlotData bd = (BlotData) v.elementAt(s);
			
			if( ( bd.x >= sx_min ) &&
			    ( bd.x <= sx_max ) &&
			    ( bd.y >= sy_min ) &&
			    ( bd.y <= sy_max ) )
			{
			    if( bd.is_meas )
				meas_hits_v.addElement( new Integer( bd.id ) );
			    else
				spot_hits_v.addElement( new Integer( bd.id ) );
			}
		    }
		}
	    }
	}
	
	if( meas_hits_v.size() > 0 )
	{
	    for(int m = 0 ; m < meas_hits_v.size(); m++ )
	    {
		show_meas_label.add(  meas_hits_v.elementAt( m ) );
	    }
	}

	if( spot_hits_v.size() > 0 )
	{
	    for(int s = 0 ; s < spot_hits_v.size(); s++ )
	    {
		show_spot_label.add(  spot_hits_v.elementAt( s ) );
	    }
	}

	panel.repaintDisplay();

     }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   ScatterPlotPanel
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public class ScatterPlotPanel extends DragAndDropPanel implements MouseListener, MouseMotionListener, Printable
    {
	private Point last_pt = null;

	public ScatterPlotPanel()
	{
	    super();
	    addMouseListener(this);
	    addMouseMotionListener(this);
	}


	
	public void mouseMoved(MouseEvent e) 
	{
	    updatePosition( e.getX(), e.getY() );
	}

	private Point drag_start = new Point();
	private Point drag_size = new Point();

	private Point2D.Double drag_start_screen = new Point2D.Double();
	private Point2D.Double drag_end_screen   = new Point2D.Double();

	private boolean dragging = false;

	public void mouseDragged(MouseEvent e) 
	{
	    if(dragging)
	    {
		if( mouse_mode == MOUSE_PAN )
		    drawRubberLine();
		else
		    drawRubberBox();

		// swap the start and end x coords if the width becomes negative
		int x = e.getX();
		int y = e.getY();
		    
		if( ( mouse_mode == MOUSE_PAN ) || ( mouse_mode == MOUSE_SELECT ) )
		{
		    drag_size.x = x - drag_start.x;
		    drag_size.y = y - drag_start.y;
		}

		if( mouse_mode == MOUSE_ZOOM )
		{
		    if(x < drag_start.x)
		    {
			drag_size.x = drag_start.x - x;
			drag_start.x = x;
		    }
		    else
			drag_size.x = x - drag_start.x;
		    
		    // likewise for the y coords
		    
		    if(y < drag_start.y)
		    {
			drag_size.y = drag_start.y - y;
			drag_start.y = y;
		    }
		    else
			drag_size.y = y - drag_start.y;
		    
		    // constrain the aspect ratio to match the screen
		    
		    double screen_aspect_ratio = (double) getWidth() / (double) getHeight();
		
		    if(drag_size.y >= drag_size.x)
		    {
			drag_size.x = (int)((double)drag_size.y * screen_aspect_ratio);
		    }
		    else

		    {
			drag_size.y = (int)((double)drag_size.x / screen_aspect_ratio);
		    }
		}

		if( mouse_mode == MOUSE_PAN )
		    drawRubberLine();
		else
		    drawRubberBox();
	    }

	} 

	
	public void mousePressed(MouseEvent e) 
	{
	    if (e.isPopupTrigger() || e.isControlDown()) 
	    {
		
	    }
	    else
	    {
		dragging = true;
		
		drag_start.x = e.getX();
		drag_start.y = e.getY();
		drag_size.x = 0;
		drag_size.y = 0;
		
		drag_start_screen.x = fromScreenX( drag_start.x );
		drag_start_screen.y = fromScreenY( drag_start.y );

		if( mouse_mode == MOUSE_PAN )
		    drawRubberLine();
		else
		    drawRubberBox();
	    }
	}
	
	public void mouseReleased(MouseEvent e) 
	{
	    if(dragging)
	    {
		if( mouse_mode == MOUSE_PAN )
		    drawRubberLine();
		else
		    drawRubberBox();

		dragging = false;

		
		    
		drag_end_screen.x = fromScreenX( e.getX() );
		drag_end_screen.y = fromScreenY( e.getY() );


		if( mouse_mode == MOUSE_SELECT )
		{
		    // select the spots within this sweep
		
		    selectItemsInRectangle(drag_start.x, drag_start.y, drag_start.x+drag_size.x, drag_start.y+drag_size.y );

		    return;
		}

		// save the current view
		//
		
		Range range = new Range();
		range.x_meas_min = x_meas_min;
		range.y_meas_min = y_meas_min;
		range.x_meas_max = x_meas_max;
		range.y_meas_max = y_meas_max;
		history.insertElementAt(range, 0);
		back_jb.setEnabled(true);
		
		
		if( mouse_mode == MOUSE_ZOOM )
		{
		    // ignore requests for very thin or very small zooms,
		    // they have most likely occured by mistake
		    //
		    
		    if((drag_size.x < 4) && (drag_size.y < 4))
			return;
		
		    final double new_x_min = fromScreenX(drag_start.x);
		    final double new_x_max = fromScreenX(drag_start.x+drag_size.x);
		    
		    final double new_y_min = fromScreenY(drag_start.y+drag_size.y);
		    final double new_y_max = fromScreenY(drag_start.y);
		
		

		    // can't alter 'x_meas_min' until new 'x_meas_max' computed...

		    
		    x_meas_min = new_x_min;
		    x_meas_max = new_x_max;
		    
		    y_meas_min = new_y_min;
		    y_meas_max = new_y_max;
		    

		    axis_man.setComputedRange(0, x_meas_min, x_meas_max);
		    axis_man.setComputedRange(1, y_meas_min, y_meas_max);
		    

		    // and read back the values 
		    // (which might be user specified, or the values we just inserted)
		    //
		    x_meas_min = axis_man.getMin(0);
		    x_meas_max = axis_man.getMax(0);
		    
		    y_meas_min = axis_man.getMin(1);
		    y_meas_max = axis_man.getMax(1);
		}

		if( mouse_mode == MOUSE_PAN )
		{
		    // translate ...

		    System.out.println(" pan pixels: " + drag_size.x + " , " + drag_size.y );

		    //final double new_x_min = fromScreenX(drag_start.x);
		    //final double new_x_max = fromScreenX(drag_start.x+drag_size.x);
		    
		    //final double new_y_min = fromScreenY(drag_start.y+drag_size.y);
		    //final double new_y_max = fromScreenY(drag_start.y);
		

		    final double delta_x = drag_start_screen.x - drag_end_screen.x;
		    final double delta_y = drag_start_screen.y - drag_end_screen.y;


		    System.out.println(" pan coords: " + delta_x + " , " + delta_y );

		    x_meas_min += delta_x;
		    x_meas_max += delta_x;
		    
		    y_meas_min += delta_y;
		    y_meas_max += delta_y;
		   
		    axis_man.setComputedRange(0, x_meas_min, x_meas_max);
		    axis_man.setComputedRange(1, y_meas_min, y_meas_max);
		   
		    // and read back the values 
		    // (which might be user specified, or the values we just inserted)
		    //
		    x_meas_min = axis_man.getMin(0);
		    x_meas_max = axis_man.getMax(0);
		    
		    y_meas_min = axis_man.getMin(1);
		    y_meas_max = axis_man.getMax(1);
		}


		// setBoundaryValues();

		//System.out.println(String.valueOf(drag_start.x) + " , " + 
		//		   String.valueOf(drag_start.y) + " @ " +
		//		   String.valueOf(drag_size.x) + " x " + 
		//		   String.valueOf(drag_size.y));

		updateDisplay();

	    }
	}
	
	public void mouseEntered(MouseEvent e) 
	{
	}

	public void mouseExited(MouseEvent e) 
	{
	    if( ignored_info != null )
		coords_label.setText( coords_string + " : " + ignored_info + " not plotted (coordinates were 'Infinity' or 'NaN')");
	    else
		coords_label.setText( coords_string );
	    
	    last_pt = null;
	}

	public void mouseClicked(MouseEvent e) 
	{
	    // find the nearest spot using  bin Vectors

	    int bx = (e.getX()) / x_to_bin;
	    int by = (e.getY()) / y_to_bin;

	    if((bx >= 0) && (by >= 0) &&
	       (bx < n_spot_bins) && (by < n_spot_bins))
	    {
		Vector v = spot_bins[by][bx];
		//str += " size=" + ((v == null) ? "0" : String.valueOf(v.size()));
		if(v != null)
		{
		    BlotData bd = findNearestBlot(e.getX(), e.getY(), v);
		    
		    if(bd != null)
		    {
			//System.out.println("toggle " + id );
			if(bd.is_meas)
			    toggleMeasLabel(bd.id);
			else
			    toggleSpotLabel(bd.id);
		    }
		}
	    }
	}

	public void drawRubberBox()
	{
	    Graphics graphic = getGraphics();
	    graphic.setXORMode(text_col);
	    graphic.drawRect(drag_start.x, drag_start.y, drag_size.x, drag_size.y);
	}

	public void drawRubberLine()
	{
	    Graphics graphic = getGraphics();
	    graphic.setXORMode(text_col);
	    graphic.drawLine(drag_start.x, drag_start.y, drag_start.x +  drag_size.x, drag_start.y + drag_size.y);
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


	public Image drawBlots(int width, int height)
	{
	    spot_bins = new Vector[n_spot_bins][];     // used for mouse tracking.....
	    for(int sb=0; sb < n_spot_bins; sb++)
		spot_bins[sb] = new Vector[n_spot_bins];
	    
	    x_to_bin = width  / n_spot_bins;
	    y_to_bin = height / n_spot_bins;

	    Image new_spot_image = createImage(width, height);
	    if(new_spot_image == null)
		return null;
	    Graphics graphic = new_spot_image.getGraphics();


	    graphic.setColor(background_col);
	    graphic.fillRect(0, 0, width, height);
	    
	    //graphic.setClip( x_gap, y_gap, width - 2 * x_gap, height - 2 * y_gap );
	    
	    graphic.setFont(dplot.getFont());
	    label_height = dplot.getFontHeight();

	    updateTransform( width, height);

	    int[] pts_x = new int[4];
	    int[] pts_y = new int[4];

	    int spot_size_x = 0;
	    int spot_size_y = 0;


	    java.util.HashSet ignore_hs = new java.util.HashSet();

	    
	    //graphic.setClip( x_gap, y_gap, width - x_gap, height - y_gap );

	    final int x_max = width  - x_gap;
	    final int y_max = height - y_gap;

	    try
	    {
		for(int p=0; p < pd_list.size(); p++)
		{
		    final PlotDetails pd = (PlotDetails) pd_list.elementAt(p);
		    
		    if(pd.x_col == -1)
			graphic.setColor(pd.fixed_col);
		    
		    // 		System.out.println("drawBlots(): drawing plot " + p + " meas=" + pd.measurements);
		    
		    if(pd.measurements)
		    {
			final int ns = edata.getNumSpots();
			if((pd.x_meas >= 0) && (pd.y_meas >= 0))
			{
			     for(int s=0; s < ns; s++)
			    {
				if(abort_draw)
				    return null;
				
				int spot = edata.getSpotAtIndex(s); // is this really neccessary?
				
				if((pd.use_filter == false) || (!edata.filter(spot)))
				{
				    final double xval = edata.eValue(pd.x_meas, spot);
				    final double yval = edata.eValue(pd.y_meas, spot);
				    
				    final boolean ignore = 
					Double.isNaN( xval ) || 
					Double.isNaN( yval ) || 
					Double.isInfinite( xval ) || 
					Double.isInfinite( yval );

				    if( ! ignore )
				    {
					final int xp = toScreenX(xval);
					final int yp = toScreenY(yval);
					
					final boolean offscreen = ( xp < x_gap ) || ( yp <  y_gap) || ( xp > x_max ) || ( yp > y_max ) ;

					if( !offscreen )
					{
					    // store the spot in the correct bin Vector
					    int bx = xp / x_to_bin;
					    int by = yp / y_to_bin;
					    
					    //System.out.println(bx + "," + by);
					    
					    if((bx >= 0) && (by >= 0) &&
					       (bx < n_spot_bins) && (by < n_spot_bins))
					    {
						
						if(spot_bins[by][bx] == null)
						    spot_bins[by][bx] = new Vector();
						spot_bins[by][bx].addElement(new BlotData(xp, yp, spot, false));
						
						
						// get it's colour and size
						if(pd.x_col >= 0)
						{
						    double cval = edata.eValue(pd.x_col, spot);
						    graphic.setColor(dplot.getDataColour(cval, pd.x_col));
						}
						
						if(pd.x_size >= 0)
						{
						    double sxval = edata.eValue(pd.x_size, spot);
						    spot_size_x = (int) (Math.abs(sxval) * pd.x_scale );
						}
						else
						{
						    spot_size_x = (int)(pd.x_scale);
						    
						}
						
						if(pd.y_size >= 0)
						{
						    double syval = edata.eValue(pd.y_size, spot);
						    spot_size_y = (int) (Math.abs(syval) * pd.y_scale);
						}
						else
						{
						    spot_size_y = (int)(pd.y_scale);
						}
						if(spot_size_x < 1)
						    spot_size_x = 1;
						if(spot_size_y < 1)
						    spot_size_y = 1;
						
						switch(pd.shape)
						{
						    case 0:
							if(pd.solid)
							    graphic.fillOval(xp-spot_size_x, yp-spot_size_y, spot_size_x*2, spot_size_y*2);
							else
							    graphic.drawOval(xp-spot_size_x, yp-spot_size_y, spot_size_x*2, spot_size_y*2);
							break;
						    case 1:
							if(pd.solid)
							    graphic.fillRect(xp-spot_size_x, yp-spot_size_y, spot_size_x*2, spot_size_y*2);
							else
							    graphic.drawRect(xp-spot_size_x, yp-spot_size_y, spot_size_x*2, spot_size_y*2);
							break;
						    case 2:
							pts_x[0] = xp; pts_y[0] = yp-spot_size_y;
							pts_x[1] = xp-spot_size_x; pts_y[1] = yp;
							pts_x[2] = xp; pts_y[2] = yp+spot_size_y;
							pts_x[3] = xp+spot_size_x; pts_y[3] = yp;
							if(pd.solid)
							    graphic.fillPolygon(pts_x, pts_y, 4);
							else
							    graphic.drawPolygon(pts_x, pts_y, 4);
							break;
						    case 3:
							graphic.drawLine(xp-spot_size_x, yp, xp+spot_size_x, yp);
							graphic.drawLine(xp, yp-spot_size_y, xp, yp+spot_size_y);
							break;
						}
					    }
					} // !offscreen
				    }
				    else
				    {
					// ignore this spot, one of the coords is Infinity or NaN
					//
					// (need to keep track of which IDs were ignored rather than just a count,
					//  so that the same spot doesn't get counted multiple times when several
					//  plots are being overlayed)
					//
					ignore_hs.add( new Integer( s ) );
				    }
				}
			    }

			    if( ignore_hs.size() > 0 )
			    {
				ignored_info = ignore_hs.size() > 1 ? ( ignore_hs.size() + " Spots" ) : "One Spot";
			    }
			    else
			    {
				ignored_info = null;
			    }
			}
		    }
		    else
		    {
			// drawing measurements instead....
			
			if((pd.x_spot >= 0) && (pd.y_spot >= 0))
			{


			    for(int m=0; m < edata.getNumMeasurements(); m++)
			    {
				if(abort_draw)
				    return null;
				
				
				double xval = edata.eValue(m, pd.x_spot);
				double yval = edata.eValue(m, pd.y_spot);
				
				final boolean ignore = 
					Double.isNaN( xval ) || 
					Double.isNaN( yval ) || 
					Double.isInfinite( xval ) || 
					Double.isInfinite( yval );

				
				if( ! ignore ) 
				{
				    final int xp = toScreenX(xval);
				    final int yp = toScreenY(yval);
				    
				    final boolean offscreen = ( xp < x_gap ) || ( yp <  y_gap) || ( xp > x_max ) || ( yp > y_max ) ;

				    if( ! offscreen )
				    {
					// store the spot in the correct bin Vector
					int bx = xp / x_to_bin;
					int by = yp / y_to_bin;
					
					if((bx >= 0) && (by >= 0) &&
					   (bx < n_spot_bins) && (by < n_spot_bins))
					{
					    
					    if(spot_bins[by][bx] == null)
						spot_bins[by][bx] = new Vector();
					    spot_bins[by][bx].addElement(new BlotData(xp, yp, m, true));
					    
					    
					    // get it's colour and size
					    if(pd.x_col >= 0)
					    {
						double cval = edata.eValue(pd.x_col, pd.x_spot);
						graphic.setColor(dplot.getDataColour(cval, pd.x_col));
					    }
					    
					    if(pd.x_size >= 0)
					    {
						double sxval = edata.eValue(pd.x_size, pd.x_spot);
						spot_size_x = (int) (Math.abs(sxval) * pd.x_scale);
					    }
					    else
					    {
						spot_size_x = (int)(pd.x_scale);
					    }
					    
					    if(pd.y_size >= 0)
					    {
						double syval = edata.eValue(pd.y_size, pd.x_spot);
					    spot_size_y = (int) (Math.abs(syval) * pd.y_scale);
					    }
					    else
					    {
						spot_size_y = (int)(pd.y_scale);
					    }
					    
					    if(spot_size_x < 1)
						spot_size_x = 1;
					    if(spot_size_y < 1)
						spot_size_y = 1;
					    
					    switch(pd.shape)
					    {
						case 0:
						    if(pd.solid)
							graphic.fillOval(xp-spot_size_x, yp-spot_size_y, spot_size_x*2, spot_size_y*2);
						    else
							graphic.drawOval(xp-spot_size_x, yp-spot_size_y, spot_size_x*2, spot_size_y*2);
						    break;
						case 1:
						    if(pd.solid)
							graphic.fillRect(xp-spot_size_x, yp-spot_size_y, spot_size_x*2, spot_size_y*2);
						    else
							graphic.drawRect(xp-spot_size_x, yp-spot_size_y, spot_size_x*2, spot_size_y*2);
						    break;
						case 2:
						    pts_x[0] = xp; pts_y[0] = yp-spot_size_y;
						    pts_x[1] = xp-spot_size_x; pts_y[1] = yp;
						    pts_x[2] = xp; pts_y[2] = yp+spot_size_y;
						    pts_x[3] = xp+spot_size_x; pts_y[3] = yp;
						    if(pd.solid)
							graphic.fillPolygon(pts_x, pts_y, 4);
						    else
							graphic.drawPolygon(pts_x, pts_y, 4);
						    break;
						case 3:
						    graphic.drawLine(xp-spot_size_x, yp, xp+spot_size_x, yp);
						    graphic.drawLine(xp, yp-spot_size_y, xp, yp+spot_size_y);
						    break;
					    }
					    
					}

				    }  // !offscreen
				}
				else
				{
				    // ignore this Measurement, one of the coords is Infinity or NaN
				    //
				    // (need to keep track of which IDs were ignored rather than just a count,
				    //  so that the same Measurement doesn't get counted multiple times when several
				    //  plots are being overlayed)
				    //
				    ignore_hs.add( new Integer( m ) );
				}
			    }
			}

			if( ignore_hs.size() > 0 )
			{
			    ignored_info = ignore_hs.size() > 1 ? ( ignore_hs.size() + " Measurements" ) : "One Measurement";
			}
			else
			{
			    ignored_info = null;
			}
		
		    }
		}

		
		drawAxes(graphic, width, height);
	    }
	    catch( Exception ex )
	    {
		System.out.println( ex );
	    }

	    return new_spot_image;
	}

	public void drawClusters(Graphics graphic, int width, int height)
	{
	    // --------------------------------------------- /
	    // --------------------------------------------- /
	    // draw the clusters                             /
	    // --------------------------------------------- /

	    if((glyph_poly == null) || (glyph_poly_height != cluster_size))
	    {
		// generate (or re-generate at a new size) the glyphs
		glyph_poly = dplot.getScaledClusterGlyphs(cluster_size);
		glyph_poly_height = cluster_size;
	    }

	    int glyph_offset = cluster_size / 2;
	    
	    final int x_max = width  - x_gap;
	    final int y_max = height - y_gap;

	    for(int p=0; p < pd_list.size(); p++)
	    {
		final PlotDetails pd = (PlotDetails) pd_list.elementAt(p);
		
		 if(pd.show_clusters)
		 {
		     if(pd.measurements)
		     {
			 // drawing spots

			 ExprData.ClusterIterator clit = edata.new ClusterIterator();
			 
			 ExprData.Cluster clust = clit.getCurrent();
			 
			 while(clust != null)
			 {
			     if(abort_draw)
				 return;
			     
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
				     
				     // System.out.print("cl " + cl + ": ");
				     
				     for(int gn=0; gn < ce.length; gn++)
				     {
					 iv = ce[gn];
					 
					 
					 //System.out.print(".." + iv );
					 
					 if(iv < edata.getNumSpots())
					 {
					     // gene_index = edata.getIndexOf(iv);
					     
					     final int xp = toScreenX( edata.eValue(pd.x_meas, iv) );
					     final int yp = toScreenY( edata.eValue(pd.y_meas, iv) );
					     
					     final boolean offscreen = ( xp < x_gap ) || ( yp <  y_gap) || ( xp > x_max ) || ( yp > y_max ) ;
					     
					     if( !offscreen )
					     {
						 
						 if((pd.use_filter == false) || (!edata.filter(iv)))
						 {
						     if(vis_count == 0)
						     {
						     }
						     else
						     {
							 if(pd.show_edges)
							     graphic.drawLine(last_x, last_y, xp, yp);
						     }
						     
						     int gly = clust.getGlyph();
						     Polygon poly = new Polygon(glyph_poly[gly].xpoints, 
										glyph_poly[gly].ypoints,
										glyph_poly[gly].npoints);
						     
						     poly.translate(xp-glyph_offset, yp-glyph_offset);
						     graphic.fillPolygon(poly);
						     
						     vis_count++;
						     last_x = xp;
						     last_y = yp;
						 }
					     }
					 }
				     }
				     
				 }
				 
			     }  
			     clust = clit.getNext();
			     
			 } // while(clust != null)
		     }
		     else
		     {
			 // drawing measurements
			 
			 ExprData.ClusterIterator clit = edata.new ClusterIterator();
			 
			 ExprData.Cluster clust = clit.getCurrent();
			 
			 while(clust != null)
			 {
			     if(abort_draw)
				 return;
			     
			     if(clust.getShow() && !clust.getIsSpot())
			     {
				 Integer i;
				 int iv, gene_index;
				 int[] ce = clust.getElements();
				 int last_x = 0;
				 int last_y = 0;
				 int vis_count = 0;
				        
				 if(ce != null)
				 {
				     graphic.setColor(clust.getColour());

				     for(int mi=0; mi < ce.length; mi++)
				     {
					 iv = ce[mi];
					 
					 final double xval = edata.eValue(iv, pd.x_spot);
					 final double yval = edata.eValue(iv, pd.y_spot);

					 final int xp = toScreenX(xval);
					 final int yp = toScreenY(yval);

					 final boolean offscreen = ( xp < x_gap ) || ( yp <  y_gap) || ( xp > x_max ) || ( yp > y_max ) ;
					 
					 if( !offscreen )
					 {
					     
					     if(vis_count == 0)
					     {
					     }
					     else
					     {
						 if(pd.show_edges)
						     graphic.drawLine(last_x, last_y, xp, yp);
					     }
					     
					     final int gly = clust.getGlyph();

					     final Polygon poly = new Polygon(glyph_poly[gly].xpoints, 
									glyph_poly[gly].ypoints,
									glyph_poly[gly].npoints);
					     
					     poly.translate(xp-glyph_offset, yp-glyph_offset);
					     graphic.fillPolygon(poly);
					     
					     vis_count++;
					     last_x = xp;
					     last_y = yp;
					 }
				     }
   
				 }
			     }
			     clust = clit.getNext();
			 }
		     }
		 }     // if(pd.show_clusters)
	    }         // for(int p=0; ...
	}
	
	private void drawLabels(Graphics graphic, int width, int height)
	{
	    Color fg = mview.getTextColour();
	    Color bg = mview.getBackgroundColour();

	    FontMetrics fm = graphic.getFontMetrics();
	    int laba = fm.getAscent();
	    int labh = laba + fm.getDescent();


	    final int x_max = width  - x_gap;
	    final int y_max = height - y_gap;

	    
	    for (Iterator i = show_spot_label.iterator(); i.hasNext() ;) 
	    {
		Integer id = (Integer) i.next();
		
		int spot = id.intValue();

		 for(int p=0; p < pd_list.size(); p++)
		 {
		     if(abort_draw)
			 return;
		     
		     PlotDetails pd = (PlotDetails) pd_list.elementAt(p);
		     
		     if(pd.measurements)
		     {
			 final boolean af = ((pd.apply_filter_jchkb != null) && (pd.apply_filter_jchkb.isSelected()));
			 
			 if(!af || (!edata.filter(spot)))
			 {
			     
			     // System.out.println(dplot.getTrimmedSpotLabel(spot));
			     
			     double xval = edata.eValue(pd.x_meas, spot);
			     double yval = edata.eValue(pd.y_meas, spot);
			     
			     int xp = toScreenX(xval);
			     int yp = toScreenY(yval);
			     
			     final boolean offscreen = ( xp < x_gap ) || ( yp <  y_gap) || ( xp > x_max ) || ( yp > y_max ) ;

			     if( !offscreen )
			     {
				 if(pd.x_size >= 0)
				 {
				     double sxval = edata.eValue(pd.x_size, spot);
				     xp += (int) (Math.abs(sxval) * pd.x_scale);
				 }
				 else
				 {
				     xp += (int)(pd.x_scale);
				 }
				 
				 String lab = nt_sel.getNameTag(spot);
				 if(lab != null)
				 {
				     int labw = fm.stringWidth(lab);
				     
				     graphic.setColor(bg);
				     graphic.fillRect(xp, yp-laba, labw, labh);
				     graphic.setColor(fg);
				     graphic.drawString(lab, xp, yp);
				 }
			     }
			 }
		     }
		 }
	    }

	    for (Iterator i = show_meas_label.iterator(); i.hasNext() ;) 
	    {
		Integer id = (Integer) i.next();
		
		int meas = id.intValue();
		
		int label_y_off = fm.getAscent() / 2;

		for(int p=0; p < pd_list.size(); p++)
		{
		    if(abort_draw)
			return;
		    
		    PlotDetails pd = (PlotDetails) pd_list.elementAt(p);
		    
		    if(!pd.measurements)
		    {
			// if(!af || (!edata.filter(spot)))
			{
			    
			    // System.out.println(dplot.getTrimmedSpotLabel(spot));
			    
			    double xval = edata.eValue(meas, pd.x_spot);
			    double yval = edata.eValue(meas, pd.y_spot);
			
			    int xp = toScreenX(xval) + 1;
			    int yp = toScreenY(yval) + label_y_off;
			    
			    String lab = edata.getMeasurementName(meas);

			    if(lab != null)
			    {
				if(pd.x_size >= 0)
				{
				    double sxval = edata.eValue(meas, pd.x_size);
				    xp += (int) (Math.abs(sxval) * pd.x_scale);
				}
				else
				{
				    xp += (int)(pd.x_scale);
				}

				int labw = fm.stringWidth(lab);
				
				graphic.setColor(bg);
				graphic.fillRect(xp, yp-laba, labw, labh);
				graphic.setColor(fg);
				graphic.drawString(lab, xp, yp);
			    }
			}
		    }
		}
	    }
	    
	}

	private void drawAxes(Graphics graphic, int width, int height)
	{
	    
	    graphic.setColor(text_col);
	    
	    // ..... horizontal ..... 
	    //
	    int xs = x_gap;
	    int xe = width - x_gap;
	    
	    int x_axis_y_pos = height - y_gap;

	    axis_man.drawAxis( graphic, 0, new Point(xs, x_axis_y_pos), new Point(xe, x_axis_y_pos) );

	    int ye = y_gap;
	    int ys = height - y_gap;
	    
	    int y_axis_x_pos = width - x_gap;
	    
	    axis_man.drawAxis( graphic, 1, new Point(y_axis_x_pos, ys), new Point(y_axis_x_pos, ye) );
	}

	public void paintComponent(Graphics graphic)
	{
	    if(graphic == null)
		return;
	    
	    // has the panel resized?
		    
	    if(total_image != null)
	    {
		if((total_image.getWidth(null) != getWidth()) || 
		   (total_image.getHeight(null) != getHeight()))
		{
		    //System.out.println("panel has resized");
		    spot_image = null;
		    repaintDisplay();
		}
		else
		{
		    graphic.drawImage(total_image,0,0,null);
		}
	    }
	}

	public int print(Graphics g, PageFormat pf, int pg_num) throws PrinterException 
	{
	    ((Graphics2D)g).setRenderingHint( RenderingHints.KEY_ANTIALIASING, 
					      RenderingHints.VALUE_ANTIALIAS_ON );

	    // margins
	    //
	    g.translate((int)pf.getImageableX(), 
			(int)pf.getImageableY());
	    
	    // area of one page
	    //
	    // ??  area seems to be too big?
	    //
	    int pw = (int)(pf.getImageableWidth() * 0.9);   
	    int ph = (int)(pf.getImageableHeight() * 0.9);
	    
	    // System.out.println("PRINT REQUEST for page " + pg_num + " size=" + pw + "x" + ph); 
	    
	    frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

	    
	    Image new_spot_image = drawBlots(pw, ph);
	    g.drawImage(new_spot_image, 0, 0, null );
	    drawClusters(g, pw, ph);
	    drawLabels(g, pw, ph);
	    deco_man.drawDecorations(g, pw, ph);

	    frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		
	    // panel.paintIntoRegion(g, pw, ph);

	    return (pg_num > 0) ? NO_SUCH_PAGE : PAGE_EXISTS;
	}

	public synchronized void repaintDisplay()
	{
	    if(frame == null)
		return;

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
		if(frame == null)
		    return;

		frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

		//System.out.println("starting draw thread");

		if((getWidth() == 0) || (getHeight() == 0))
		    return;

		Image new_total_image = createImage(getWidth(), getHeight());

		
		if(spot_image == null)
		{		
		    Image new_spot_image = drawBlots( getWidth(), getHeight() );

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


		//ntig.setClip( x_gap, y_gap, getWidth() - x_gap, getHeight() - y_gap );

		// paint spot image
		//
		if(!abort_draw)
		    ntig.drawImage(spot_image, 0, 0, null /* ImageObserver */ );

		// and add the overlays
		
		if(!abort_draw)
		    drawClusters(ntig, getWidth(), getHeight());
		
		((Graphics2D)ntig).setRenderingHint( RenderingHints.KEY_ANTIALIASING, 
						     RenderingHints.VALUE_ANTIALIAS_ON );
		
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

		frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		
		System.gc();

		drawing = false;
	    }
	}
    }
    
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   plot details
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private class PlotDetails
    {
	private int x_spot, y_spot, x_meas, y_meas, x_col, y_col, x_size, y_size, shape;
	private Color fixed_col;
	private double x_scale, y_scale;
	private boolean solid;
	
	private boolean measurements;   // if not, then spots....

	private boolean use_filter;
	private boolean show_clusters;
	private boolean show_edges;

	private boolean spot_fixed_size;

	private JButton x_spot_button;
	private JButton y_spot_button;

	private JRadioButton spot_mode;
	private JRadioButton meas_mode;

	//private JComboBox x_meas_jcb;
	//private JComboBox y_meas_jcb;

	private JButton x_thing_jb;
	private JButton y_thing_jb;
	
	private String x_thing_name;
	private String y_thing_name;

	private JComboBox x_size_jcb;
	private JComboBox y_size_jcb;
	private JComboBox x_col_jcb;
	private JComboBox y_col_jcb;
	private JTextField x_scale_jtf;
	private JTextField y_scale_jtf;
	private JComboBox shape_jcb;
	private JCheckBox solid_jchkb;
	private JCheckBox apply_filter_jchkb;
	private JButton fixed_col_jb;
	
	private PlotDetailListener x_meas_al, x_size_al, x_col_al;
	private PlotDetailListener y_meas_al, y_size_al;

	public PlotDetails()
	{
	    x_scale = 1.0;
	    y_scale = 1.0;
	    shape = 3;
	    solid = true;
	    x_col = -1;
	    x_size = -1;
	    y_size = -1;
	    x_thing_name = null;
	    y_thing_name = null;
	    measurements = true;
	    spot_fixed_size = false;
	    fixed_col = mview.getTextColour();
	    use_filter = true;
	    show_clusters = false;
	    show_edges = false;
	}

	public PlotDetails(final PlotDetails src)
	{
	    x_meas = src.x_meas;
	    y_meas = src.y_meas;
	    x_size = src.x_size;
	    y_size = src.y_size;
	    measurements = src.measurements;
	    x_col = src.x_col;
	    x_scale = src.x_scale;
	    y_scale = src.y_scale;
	    shape = src.shape;
	    solid = src.solid;
	    spot_fixed_size = src.spot_fixed_size;
	    fixed_col = src.fixed_col;
	    use_filter = src.use_filter;
	    show_clusters = src.show_clusters;
	    show_edges = src.show_edges;

	    x_thing_name = src.x_thing_name;
	    y_thing_name = src.y_thing_name;
	}

    }

    Vector pd_list;

    private void removePlotDetails(PlotDetails pd)
    {
	if(mview.infoQuestion("Really delete this plot?", "Yes", "No") == 0)
	{
	    int i = pd_list.indexOf(pd);
	    if(i >= 0)
	    {
		pd_list.removeElementAt(i);
		buildPlotDetailControls();
	    }
	}
    }
    private void clonePlotDetails(PlotDetails pd)
    {
	int i = pd_list.indexOf(pd);
	if(i >= 0)
	{
	   PlotDetails new_pd = new PlotDetails(pd);
	   pd_list.insertElementAt(new_pd, i+ 1);
	   buildPlotDetailControls();
	}
    }
    private void raisePlotDetails(PlotDetails pd)
    {
	int i = pd_list.indexOf(pd);
	if(i > 0)
	    pd_list.removeElementAt(i);
	pd_list.insertElementAt(pd, 0);
	buildPlotDetailControls();
    }
    private void lowerPlotDetails(PlotDetails pd)
    {
	int i = pd_list.indexOf(pd);
	if(i >= 0)
	    pd_list.removeElementAt(i);
	pd_list.insertElementAt(pd, i+1);
	buildPlotDetailControls();
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   spot picker list
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private java.util.Hashtable open_picker_lists;

    private void displayPickerLists( final PlotDetails pd )
    {
	if( open_picker_lists == null )
	    open_picker_lists = new java.util.Hashtable();

	JFrame pframe = (JFrame) open_picker_lists.get( pd );

	if( pframe == null )
	{
	    pframe = makePickerListFrame( pd );
	    open_picker_lists.put( pd, pframe );
	}

	pframe.setVisible( true );
    }

    private void pickerListFrameHasClosed( JFrame pframe )
    {
	for (Enumeration keys = open_picker_lists.keys() ; keys.hasMoreElements() ;) 
	{
	    PlotDetails pd = (PlotDetails) keys.nextElement();
	    JFrame pf = (JFrame) open_picker_lists.get( pd );
	    if( pf == pframe )
	    {
		open_picker_lists.remove( pd );
		return;
	    }
	}
    }

    private void closeAnyOpenPickerListFrames()
    {
	if( open_picker_lists == null )
	    return;

	for (Enumeration keys = open_picker_lists.keys() ; keys.hasMoreElements() ;) 
	{
	    PlotDetails pd = (PlotDetails) keys.nextElement();
	    JFrame pf = (JFrame) open_picker_lists.get( pd );
	    pf.setVisible( false );
	}
    }

    private JFrame makePickerListFrame( final PlotDetails pd )
    {
	final JFrame pframe = new JFrame ( pd.measurements ? "Pick Measurements" : "Pick Spots");

	mview.decorateFrame(pframe);

	JPanel panel = new JPanel();
	GridBagConstraints c = null;

	GridBagLayout gridbag = new GridBagLayout();
	panel.setLayout(gridbag);
	panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	
	final JList list1 = new JList();
	list1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

	final JList list2 = new JList();
	list2.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

	int row = 1;

	if( !pd.measurements )
	{
	    pick_ntsel = new NameTagSelector(mview);
	    pick_ntsel.loadSelection("ScatterPlot.name_tags");
	    pick_ntsel.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			sel_spot_back_map = null;
			pick_ntsel.saveSelection("ScatterPlot.name_tags");
			populateListWithSpots( pd, list1 );
			populateListWithSpots( pd, list2 );
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridwidth = 2;
	    c.gridy = row++;
	    c.weightx = 5.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(pick_ntsel, c);
	    panel.add(pick_ntsel);
	    row++;
	}

	JLabel label = new JLabel("x pos");
	c = new GridBagConstraints();
	c.gridy = row;
	gridbag.setConstraints(label, c);
	panel.add(label);
   
	label = new JLabel("y pos");
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = row;
	gridbag.setConstraints(label, c);
	panel.add(label);

	row++;

	if( !pd.measurements )
	{
	    populateListWithSpots( pd, list1 );
	    populateListWithSpots( pd, list2 );
	}
	else
	{
	    populateListWithMeasurements( pd, list1 );
	    populateListWithMeasurements( pd, list2 );
	}

	list1.setSelectedValue( pd.x_thing_jb.getText(), true );
	list2.setSelectedValue( pd.y_thing_jb.getText(), true );
	
        
	JScrollPane jsp1 = new JScrollPane(list1);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = row;
	c.weightx = 5.0;
	c.weighty = 8.0;
	c.fill = GridBagConstraints.BOTH;
	gridbag.setConstraints(jsp1, c);
	panel.add(jsp1);


	JScrollPane jsp2 = new JScrollPane(list2);
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = row;
	c.weightx = 5.0;
	c.weighty = 8.0;
	c.fill = GridBagConstraints.BOTH;
	gridbag.setConstraints(jsp2, c);
	panel.add(jsp2);

	row++;

	{
	    JPanel wrapper = new JPanel();
	    wrapper.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));

	    JButton button = new JButton("OK");
	    wrapper.add(button);
	    button.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			pd.x_thing_name = (String) list1.getSelectedValue();
			pd.y_thing_name = (String) list2.getSelectedValue();

			pd.x_thing_jb.setText(pd.x_thing_name);
			pd.y_thing_jb.setText(pd.y_thing_name);
			
			if(pd.measurements)
			{
			    pd.x_meas = edata.getMeasurementFromName( pd.x_thing_name );
			    axis_man.getAxis( 0 ).setAutoTitle( getTitle( 0 ) );
			    pd.y_meas = edata.getMeasurementFromName( pd.y_thing_name);
			    axis_man.getAxis( 1 ).setAutoTitle( getTitle( 1 ) );
			}
			else
			{
			    Vector sids1 = (Vector) sel_spot_back_map.get( pd.x_thing_name );
			    pd.x_spot = ((Integer)sids1.elementAt(0)).intValue();
			    Vector sids2 = (Vector) sel_spot_back_map.get( pd.y_thing_name );
			    pd.y_spot = ((Integer)sids2.elementAt(0)).intValue();
			}

			if(auto_reset)
			{
			    resetView();
			    setBoundaryValues();
			}

			updateDisplay();

			pickerListFrameHasClosed( pframe );

			pframe.setVisible(false);
		    }
		});

	    button = new JButton("Cancel");
	    wrapper.add(button);
	    button.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			pframe.setVisible(false);
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridwidth = 2;
	    c.gridy = row;
	    c.weightx = 5.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(wrapper, c);
	    panel.add(wrapper);
	}


	pframe.getContentPane().add(panel);
	pframe.pack();
	mview.locateWindowAtCenter(pframe);

	return pframe;
    }


    //private int[] sel_spot_ids = null;

    private Hashtable sel_spot_back_map = null;
    private Hashtable sel_spot_name_map = null;
    private NameTagSelector pick_ntsel = null;
    private HashSet unique_names = null;
    private String[] unique_names_array = null;
    
    private void populateListWithSpots(PlotDetails pd, JList list)
    {
	if(pick_ntsel == null)
	    return;
	
	final ExprData.NameTagSelection nts = pick_ntsel.getNameTagSelection();
	final int ns = edata.getNumSpots();

	//System.out.println( "some spots (" + ns  + "?)...");
	
	// build a hashtable of names to use as the list data
	// (to uniqify the names.....)
	
	final boolean af = ((pd.apply_filter_jchkb != null) && (pd.apply_filter_jchkb.isSelected()));

	if(sel_spot_back_map == null)
	{
	    unique_names = new java.util.HashSet();

	    sel_spot_back_map = new Hashtable();

	    for(int s=0; s < ns; s++)
	    {
		if(!af || (!edata.filter(s)))
		{
		    final String n = nts.getNameTag( s );
		    if(n != null)
		    {
			Vector indices = (Vector) sel_spot_back_map.get( n );
			if(indices == null)
			{
			    unique_names.add( n );
			    indices = new Vector();
			    sel_spot_back_map.put(n, indices);
			}
			indices.addElement( new Integer(s) );
		    }
		}
	    }
	    
	    unique_names_array = new String[  unique_names.size() ];
	    int np = 0;
	    
	    for(java.util.Iterator it =  unique_names.iterator() ; it.hasNext();  )
	    {
		unique_names_array[np] = (String) it.next();
		
		np++;
	    }
	}

	java.util.Arrays.sort(unique_names_array);

	// build a reverse map from name to index in the list
	// (needed to handle dropping of spots onto the list)
	//

	/*
	sel_spot_name_map = new Hashtable();
	for(int nps=0; nps < names.length; nps++)
	    sel_spot_name_map.put( names[nps], new Integer( nps ));
	*/


	// and install the new list data

	// System.out.println( unique_names_array.length + " spots...");

	list.setListData( unique_names_array );
	
    }    

    private void populateListWithMeasurements(PlotDetails pd, JList list)
    {
	Vector data = new Vector();

	for(int m=0;m<edata.getNumMeasurements();m++)
	{
	    int mi = edata.getMeasurementAtIndex(m);
	    if(edata.getMeasurementShow(mi) == true)
	    {
		data.addElement(edata.getMeasurementName(mi));
	    }
	}

	// System.out.println( data.size() + " meas...");

	list.setListData( (String[]) data.toArray(new String[0])  );
	    
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   stuff...
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  


    private boolean auto_reset = true;
    
    private int cluster_size;

    private double x_meas_min, x_meas_max;
    private double y_meas_min, y_meas_max;

    private int x_gap, y_gap;

    private double scaled_width, scaled_height;

    private int label_height;
    private int max_labels_fit;

    private NameTagSelector nts;
    private ExprData.NameTagSelection nt_sel;

    private Polygon[] glyph_poly = null;
    private int glyph_poly_height;

    private String ignored_info = null;

    private maxdView mview;
    private ExprData edata;
    private DataPlot dplot;

    private boolean filter_alert = false;

    private JFrame frame = null;

    private JPanel pd_panel;

    private JButton back_jb, fore_jb;

    private final static int MOUSE_ZOOM   = 0;
    private final static int MOUSE_PAN    = 1;
    private final static int MOUSE_SELECT = 2;
    private int mouse_mode = MOUSE_ZOOM;

    private String coords_string = "";
    private JLabel coords_label;

    private Color background_col;
    private Color text_col;
 
    private Image spot_image  = null;
    private Image total_image = null;

    private ScatterPlotPanel panel;

    //private PrintManager print_man;
    private AxisManager axis_man;
    private DecorationManager deco_man;

}
