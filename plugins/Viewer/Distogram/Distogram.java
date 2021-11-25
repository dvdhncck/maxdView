import javax.swing.*;
import javax.swing.event.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.Hashtable;
import java.awt.print.*;

import javax.imageio.*;


//
// shows the distribution of values in one or more measurements
//

public class Distogram implements ExprData.ExprDataObserver, Plugin
{
    public Distogram(maxdView mview_)
    {
	mview = mview_;

	
	//System.out.println("++ a new Distogram is alive, mview is "  + mview);
    }

    private void buildGUI()
    {
	frame = new JFrame ("Distogram");

	mview.decorateFrame(frame);

	frame.addWindowListener(new WindowAdapter() 
	    {
		public void windowClosing(WindowEvent e)
		{
		    cleanUp();
		}
	    });
	
	background_col = mview.getBackgroundColour();
	text_col       = mview.getTextColour();

	GridBagLayout gridbag = new GridBagLayout();
	frame.getContentPane().setLayout(gridbag);

	/*
	{
	    JToolBar tool_bar = new JToolBar();
	    tool_bar.setFloatable(false);
	    addTopToolbarStuff(tool_bar);
	    getContentPane().add(tool_bar);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = c.weighty = 0.0;
	    //c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(toolbar, c);
	}
	*/

	{
	    control_panel = new JPanel();
	    control_panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

	    addControls( control_panel );

	    frame.getContentPane().add(control_panel);

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = 0.0;
	    c.weighty = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(control_panel, c);
	}

	{
	    data_panel = new DistogramPanel();
	    
	    data_panel.setLayout( new BoxLayout( data_panel, BoxLayout.X_AXIS ) );

	    hist_panel = new GraphPanel[2];

	    hist_panel[0] = new GraphPanel();
	    hist_panel[0].getContext().setBackgroundColour( mview.getBackgroundColour() );
	    hist_panel[0].getContext().setForegroundColour( mview.getTextColour() );
	    hist_panel[0].getContext().setCopyData(false);
	    hist_panel[0].setPreferredSize(new Dimension(250, 350));

	    hist_panel[0].getContext().setLegendAlignment( 2, 2 );

	    hist_panel[1] = new GraphPanel();
	    hist_panel[1].getContext().setBackgroundColour( mview.getBackgroundColour() );
	    hist_panel[1].getContext().setForegroundColour( mview.getTextColour() );
	    hist_panel[1].getContext().setCopyData(false);
	    hist_panel[1].setPreferredSize(new Dimension(250, 350));

	    hist_panel[0].getContext().setLegendAlignment( 2, 2 );

	    data_panel.add( hist_panel[0] );
	    data_panel.add( hist_panel[1] );

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    c.weightx = c.weighty = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    //c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints( data_panel, c);
	    frame.getContentPane().add( data_panel );

	    
	}

	{ 
	    JPanel button_panel = new JPanel();
	    button_panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	    frame.getContentPane().add(button_panel);

	    addButtons( button_panel );

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.gridwidth = 3;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(button_panel, c);
	}

	// ===== axis =====================================================================================

	/*
	axis_man = new AxisManager(mview);

	axis_man.addAxesListener( new AxisManager.AxesListener()
	    {
		public void axesChanged() 
		{
		    repaintGraph();
		}
	    });

	axis_man.addAxis(new PlotAxis(mview, "Value"));
	axis_man.addAxis(new PlotAxis(mview, "Count"));
	*/

	// ===== decorations ===============================================================================

	/*
	deco_man = new DecorationManager(mview, "Distogram");

	deco_man.addDecoListener( new DecorationManager.DecoListener()
	    {
		public void decosChanged() 
		{
		    repaintGraph();
		}
	    });
	*/
 
	// =================================================================================================

    }

    // =================================================================================================
    

    public class DistogramPanel extends JPanel implements Printable
    {
	public int print(Graphics g, PageFormat pf, int pg_num) throws PrinterException 
	{
	    g.translate( (int)pf.getImageableX(), 
			 (int)pf.getImageableY() );
	    
	    paint( g );

	    return ( pg_num > 0 ) ? NO_SUCH_PAGE : PAGE_EXISTS;
	}
    }


    // =================================================================================================


    private void addControls( JPanel panel )
    {
	GridBagLayout gridbag = new GridBagLayout();
	panel.setLayout(gridbag);
	GridBagConstraints c;

	int line = 0;

	{
	    JLabel label = new JLabel("Display mode");
	    panel.add(label);
	    
	    c = new GridBagConstraints();
	    c.gridy = line++;
	    c.weightx = 1.0;
	    c.gridwidth = 2;
	    c.anchor = GridBagConstraints.CENTER;
	    gridbag.setConstraints(label, c);

	    display_mode_jcb = new JComboBox( display_mode_options );
	    display_mode_jcb.addActionListener(new ActionListener()
		{
		    public void actionPerformed(ActionEvent evt) 
		    {
			/*
			// clear all the hist_data arrays
			//
			for(int s = 0; s< edata.getNumMeasurements(); s++)
			{
			    meas_hist_data[s] = null;
			}
			
			computeDistograms();
			repaintGraph();
			*/
			updateGraph();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    c.gridwidth = 2;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(display_mode_jcb, c);
	    panel.add(display_mode_jcb);
	}

	Dimension fillsize = new Dimension(10,10);
	Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.WEST;
	c.gridx = 0;
	c.gridy = line++;
	gridbag.setConstraints(filler, c);
	panel.add(filler);

	{
	    
	    bins_label = new JLabel(n_bins + " bins");
	    panel.add(bins_label);
	    
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    c.weightx = 1.0;
	    c.gridwidth = 2;
	    c.anchor = GridBagConstraints.CENTER;
	    gridbag.setConstraints(bins_label, c);

	    final JButton mjb = new JButton("More");
	    panel.add(mjb);
	    mjb.addActionListener(new ActionListener()
		{
		    public void actionPerformed(ActionEvent evt) 
		    {
			setNumBins(n_bins * 2);
			updateGraph();
			//repaintGraph();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(mjb, c);
	    
	    final JButton ljb = new JButton("Less");
	    panel.add(ljb);
	    ljb.addActionListener(new ActionListener()
		{
		    public void actionPerformed(ActionEvent evt) 
		    {
			setNumBins(n_bins / 2);
			updateGraph();
			//repaintGraph();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line++;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    gridbag.setConstraints(ljb, c);

	    final JCheckBox auto_bins_jchkb = new JCheckBox("Auto");
	    panel.add(auto_bins_jchkb);
	    auto_bins_jchkb.addActionListener(new ActionListener()
		{
		    public void actionPerformed(ActionEvent evt) 
		    {
			ljb.setEnabled( !auto_bins_jchkb.isSelected() );
			mjb.setEnabled( !auto_bins_jchkb.isSelected() );
			setAutoBins( auto_bins_jchkb.isSelected() );
			updateGraph();
			//repaintGraph();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridy = line++;
	    c.weightx = 1.0;
	    c.gridwidth = 2;
	    gridbag.setConstraints(auto_bins_jchkb, c);

	    ljb.setEnabled( !auto_bins_jchkb.isSelected() );
	    mjb.setEnabled( !auto_bins_jchkb.isSelected() );
			
	}
	
	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.WEST;
	c.gridx = 0;
	c.gridy = line++;
	gridbag.setConstraints(filler, c);
	panel.add(filler);

	{
	    meas_list = new DragAndDropList();
	    populateMeasurementList();
	    meas_list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	    meas_list.addListSelectionListener(new ListSelectionListener() 
		{
		    public void valueChanged(ListSelectionEvent e) 
		    {
			measurementSelectionChanged();
		    }
		});

	    JScrollPane jsp = new JScrollPane(meas_list);

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.weightx = 10.0;
	    c.weighty = 5.0;
	    c.gridwidth = 2;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(jsp, c);
	    panel.add(jsp);
	}

	line++;
	
	{
	    apply_filter_jcb = new JCheckBox("Apply filter");
	    
	    panel.add(apply_filter_jcb);
	    apply_filter_jcb.addActionListener(new ActionListener()
		{
		    public void actionPerformed(ActionEvent evt) 
		    {
			emptyCache();
			updateGraph();
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    c.gridwidth = 2;
	    //c.anchor = GridBagConstraints.EAST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(apply_filter_jcb, c);
	}
	
	line++;


	// force a repaint
	//
	// control_panel.updateUI();
    }

    private void populateMeasurementList()
    {
	String current_meas_name = (String) meas_list.getSelectedValue();
	int m_id = (current_meas_name == null) ? -1 : edata.getMeasurementFromName( current_meas_name );
	
	meas_list.setModel( new MeasListModel() );
	
	if(m_id >= 0)
	{
	    meas_list.setSelectedValue( current_meas_name, true );
	}
    }
 
   
    public class MeasListModel extends DefaultListModel
    {
	public Object getElementAt(int index) 
	{
	    int m_id = (index >= 0) ? edata.getMeasurementAtIndex(index) : -1;
	    return ((m_id >= 0) && (m_id < edata.getNumMeasurements())) ? edata.getMeasurementName( m_id ) : null;
	}
	public int getSize() 
	{
	    return edata.getNumMeasurements();
	}
    }
    
    public void addButtons( JPanel panel )
    {
	GridBagLayout gridbag = new GridBagLayout();
	panel.setLayout(gridbag);
	GridBagConstraints c;
	JButton jb;

	/*
	jb = new JButton("Decorations");
	panel.add(jb);
	jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent evt) 
		{
		    deco_man.startEditor();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 0;
	//c.weightx = 1.0;
	//c.weighty = 0.2;
	c.anchor = GridBagConstraints.SOUTH;
	gridbag.setConstraints(jb, c);
	*/

	jb = new JButton("Print");
	panel.add(jb);
	jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent evt) 
		{
		    new PrintManager( mview, data_panel, data_panel ).openPrintDialog();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 1;
	//c.weightx = 1.0;
	//c.weighty = 0.2;
	c.anchor = GridBagConstraints.SOUTH;
	gridbag.setConstraints(jb, c);
	
	Dimension fillsize = new Dimension(32,10);
	Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.WEST;
	c.gridx = 2;
	gridbag.setConstraints(filler, c);
	panel.add(filler);

	jb = new JButton("Help");
	panel.add(jb);
	jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent evt) 
		{
		    mview.getPluginHelpTopic("Distogram", "Distogram");
		}
	    });
	 c = new GridBagConstraints();
	 c.gridx = 3;
	 //c.weightx = 1.0;
	 //c.weighty = 1.0;
	 c.anchor = GridBagConstraints.SOUTH;
	 gridbag.setConstraints(jb, c);

	 fillsize = new Dimension(32,10);
	 filler = new Box.Filler(fillsize, fillsize, fillsize);
	 c = new GridBagConstraints();
	 c.anchor = GridBagConstraints.WEST;
	 c.gridx = 4;
	 gridbag.setConstraints(filler, c);
	 panel.add(filler);

	 jb = new JButton("Close");
	 panel.add(jb);
	 jb.addActionListener(new ActionListener()
	     {
		 public void actionPerformed(ActionEvent evt) 
		 {
		     cleanUp();
		 }
	     });
	 c = new GridBagConstraints();
	 c.gridx = 5;
	 //c.weightx = 1.0;
	 //c.weighty = 1.0;
	 c.anchor = GridBagConstraints.SOUTH;
	 gridbag.setConstraints(jb, c);
    }


    public void cleanUp()
    {
	edata.removeObserver(this);
	frame.setVisible(false);
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    final int n_colours = 32;
    Color[] colours;
    int next_free_colour = -1;

    private void resetColours()
    {
	next_free_colour = -1;
    }
    private Color nextFreeColour()
    {
	if(++next_free_colour >= n_colours)
	    next_free_colour = 0;
	return colours[next_free_colour];
    }

    private void allocateColours()
    {
	colours = new Color[ n_colours ];
	
	colours[0] = Color.red;
	colours[1] = Color.green;
	colours[2] = Color.blue;
	colours[3] = Color.gray;
	colours[4] = Color.pink;
	colours[5] = Color.cyan;
	
	float hue     = (float)0.0;
	float hue_inc = ((float)0.8 / (float)(n_colours-6));
	float sat     = (float) 1.0;
	float brt     = (float) 1.0;
	
	for(int ci=6; ci < n_colours; ci++)
	{
	    switch(ci % 3)
	    {
	    case 0:
		sat = (float) 1.0;
		brt = (float) 1.0;
		break;
	    case 1:
		sat = (float) 0.75;
		brt = (float) 1.0;
		break;
	    case 2:
		sat = (float) 1.0;
		brt = (float) 0.75;
		break;
	    }
	    colours[ci] = Color.getHSBColor(hue, sat, brt);
	    hue += hue_inc;
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

	resetDistogramData();

	buildGUI();

	allocateColours();

	// computeDistograms();
	
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
	PluginInfo pinf = new PluginInfo("Distogram", 
					 "viewer", 
					 "Shows distribution of values in one or more Measurements", "",
					 1, 0, 0);
	return pinf;
    }
    public PluginCommand[] getPluginCommands()
    {
	return null;
    }
    
    public void   runCommand(String name, String[] args, CommandSignal done) 
    { 
	if(done != null)
	    done.signal();
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
	    break;
	case ExprData.SizeChanged:
	case ExprData.ValuesChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	case ExprData.RangeChanged:
	    emptyCache();
	    updateGraph();
	    //computeDistograms();
	    //repaintGraph();
	    break;
	case ExprData.VisibilityChanged:
	    break;
	}
    }

    public void clusterUpdate(ExprData.ClusterUpdateEvent cue)
    {
	switch(cue.event)
	{
	case ExprData.ColourChanged:
	case ExprData.OrderChanged:
	    break;
	case ExprData.VisibilityChanged:
	case ExprData.SizeChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
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
	    resetDistogramData();
	    //computeDistograms();
	    populateMeasurementList();
	    updateGraph();
	    
	    break;
	case ExprData.NameChanged:
	case ExprData.OrderChanged:
	    populateMeasurementList();
	    updateGraph();
	    //meas_list.setModel(new MeasListModel());
	    //populateMeasurementList();
	    break;
	}
    }

    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
	background_col = mview.getBackgroundColour();
	text_col       = mview.getTextColour();
	repaintGraph();
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  handlers
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private void measurementSelectionChanged()
    {
	updateGraph();
	//computeDistograms();
	//repaintGraph();
    }


    private void setAutoBins(boolean auto_bins_)
    {
	auto_bins = auto_bins_;

	
    }

    private void setNumBins(int new_n_bins)
    {
	if(new_n_bins < 10)
	    return;

	if(new_n_bins > edata.getNumSpots())
	    return;

	n_bins = new_n_bins;

	// clear all the hist_data arrays
	//
	for(int s = 0; s< edata.getNumMeasurements(); s++)
	{
	    meas_hist_data[s] = null;
	}
	
	bins_label.setText(n_bins + " bins");

	emptyCache();
    }

    public int pickNumBins()
    {
	// .... to do ....

	return hist_panel[0].getWidth() / 2;

    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  maths
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public class CachedData
    {
	int m_id;

	double bin_gap;
	double min, max;
	double[] bin_start;
	int biggest_bin;
	int n_spots;
	int[] bin_count;
	int[] cum_count;

	double[] x_pt;

	double[] y_pt;
	double[] cum_y_pt;
	double[] pdf_y_pt;

	double[] qq_cum_x_pt;
	double[] qq_cum_y_pt;
	double[] qq_abs_x_pt;
	double[] qq_abs_y_pt;
    }

    public Hashtable data_cache;

    public CachedData getCachedData( int m_id )
    {
	if(data_cache == null)
	    data_cache = new Hashtable();

	CachedData cd = (CachedData) data_cache.get( new Integer(m_id) );
	if(cd == null)
	{
	    cd = new CachedData();
	    data_cache.put( new Integer(m_id), cd );

	    cd.m_id = m_id;
	}
	
	return cd;

    }

    public void emptyCache()
    {
	if(data_cache != null)
	    data_cache.clear();
    }

    
    public void repaintGraph()
    {
	for(int g=0; g < hist_panel.length; g++)
	    hist_panel[g].repaint();
    }

    public void updateGraph()
    {
	frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

	if(auto_bins)
	{
	    int new_n_bins = pickNumBins();
	    if(new_n_bins != n_bins)
	    {
		n_bins = new_n_bins;
		bins_label.setText(n_bins + " bins");
		emptyCache();
	    }
	}

	GraphContext gc0 = hist_panel[0].getContext();
	gc0.removeAllPlots();

	GraphContext gc1 = hist_panel[1].getContext();
	gc1.removeAllPlots();
	

	if( display_mode_jcb.getSelectedIndex() == 3 )
	{
	    // 2 graphs required:

	    hist_panel[0].setPreferredSize(new Dimension(250, 350));
	    hist_panel[1].setPreferredSize(new Dimension(250, 350));
	    hist_panel[1].setVisible(true);
	    // frame.pack();
	}
	else
	{
	    // 1 graph required

	    hist_panel[0].setPreferredSize(new Dimension(500, 350));
	    hist_panel[1].setVisible(false);
	    // frame.pack();
	}

	resetColours();

	hist_panel[0].getHorizontalAxis().setTitle("Value");

	hist_panel[0].getVerticalAxis().setTickMode(GraphAxis.MIN_MAX_MODE);
	    
	switch( display_mode_jcb.getSelectedIndex() )
	{
	case 0:
	    hist_panel[0].getVerticalAxis().setTitle("Count");
	    hist_panel[0].getVerticalAxis().setTickSigDigits(0);
	    break;
	case 1:
	    hist_panel[0].getVerticalAxis().setTitle("Cumulative Count");
	    hist_panel[0].getVerticalAxis().setTickSigDigits(0);
	    break;
	case 2:
	    hist_panel[0].getVerticalAxis().setTitle("Probability Density");
	    hist_panel[0].getVerticalAxis().setTickSigDigits(2);
	    break;
	case 3:
	    hist_panel[0].getHorizontalAxis().setTitle("Probability");
	    hist_panel[0].getVerticalAxis().setTitle("Cumulative");
	    hist_panel[0].getVerticalAxis().setTickSigDigits(2);

	    hist_panel[1].getHorizontalAxis().setTitle("Value");
	   hist_panel[1].getVerticalAxis().setTickMode(GraphAxis.MIN_MAX_MODE);
	
	    hist_panel[1].getVerticalAxis().setTitle("Abscissae");
	    hist_panel[1].getVerticalAxis().setTickSigDigits(2);
	    break;
	}
	
	int[] sel_ids = meas_list.getSelectedIndices();
	for(int i=0; i < sel_ids.length; i++)
	{
	    int m_id = edata.getMeasurementAtIndex( sel_ids[i] );
	    String mname = edata.getMeasurementName( m_id );

	    CachedData cd = getCachedData( m_id );

	    if( cd.bin_count == null )
	    {
		computeDistogram( cd );
	    }

	    switch( display_mode_jcb.getSelectedIndex() )
	    {
	    case 0:
		if(cd.y_pt != null)
		    gc0.addLinePlot( mname, cd.x_pt, cd.y_pt, nextFreeColour() );
		break;

	    case 1:
		if(cd.cum_y_pt == null)
		    getCumulativeCounts( cd );

		if(cd.cum_y_pt != null)
		    gc0.addLinePlot( mname, cd.x_pt, cd.cum_y_pt, nextFreeColour() );
		break;

	    case 2:
		if(cd.pdf_y_pt == null)
		    getProbDensityFunction( cd );

		if(cd.pdf_y_pt != null)
		    gc0.addLinePlot( mname, cd.x_pt, cd.pdf_y_pt, nextFreeColour() );
		break;

	    case 3:
		if(cd.qq_cum_x_pt == null)
		    getQQFunctions( cd );

		Color col = nextFreeColour();

		if(cd.qq_cum_y_pt != null)
		{
		    gc0.addLinePlot( mname, cd.qq_cum_x_pt, cd.qq_cum_y_pt, col );
		    gc1.addLinePlot( mname, cd.qq_abs_x_pt, cd.qq_abs_y_pt, col );
		}
		break;
	    }
	}

	if( display_mode_jcb.getSelectedIndex() == 2 )
	{
	    // the standard normal
	    
	    final double normalisation_of_standard_normal = 1.0 / Math.sqrt( 2.0 * Math.PI );
	    
	    final int n_pts = hist_panel[0].getWidth();
	    final double x_delta = (gc0.xmax - gc0.xmin) / (double) n_pts;
	    double x_pos =  gc0.xmin;

	    double[] y_vals = new double[n_pts];
	    double[] x_vals = new double[n_pts];
	    
	    for(int p=0; p < n_pts; p++)
	    {
		x_vals[p] = x_pos;
		x_pos += x_delta;
		y_vals[p] =  normalisation_of_standard_normal * Math.exp( -0.5 * Math.pow( x_vals[p], 2.0 ) );
	    }

	    gc0.addLinePlot( "std. norm",  x_vals, y_vals, Color.black, GraphPlot.NO_GLYPH );

	}

	repaintGraph();

	frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    public final String[] display_mode_options = { "Count", "Cumulative Count", "Prob. Density" , "Q;Q" };


    // ====================================================================================


    private void computeDistogram(CachedData cd)
    {

	cd.min = edata.getMeasurementMinEValue(cd.m_id);
	cd.max = edata.getMeasurementMaxEValue(cd.m_id);

	cd.bin_gap = (cd.max - cd.min) / (double) (n_bins-1);

	// allocate the bin space
	//
	cd.bin_count = new int[n_bins];
	
	// bin each of the values
	//
	cd.n_spots = 0;

	if(cd.bin_gap == .0)
	    return;

	boolean use_filter = apply_filter_jcb.isSelected();

	for(int s=0; s < edata.getNumSpots(); s++)
	{
	    if((use_filter == false) || (!edata.filter(s)))
	    {
		int bin = (int)((edata.eValue(cd.m_id, s) - cd.min) / cd.bin_gap);
		cd.bin_count[bin]++;
		cd.n_spots++;
	    
	
	    }
	}

	/*
	// and record the biggest value
	//
	cd.biggest_bin = 0;
	for(int b=0; b < n_bins; b++)
	    if(cd.bin_count[b] > cd.biggest_bin)
		cd.biggest_bin = cd.bin_count[b];
	*/


	// then convert to a wiggly line for display
	
	cd.x_pt = new double[n_bins];
	cd.y_pt = new double[n_bins];
	double xp = cd.min; //  - (cd.bin_gap / 2);
	for(int b=0; b < n_bins; b++)
	{
	    cd.x_pt[b] = xp;
	    xp += cd.bin_gap;
	    cd.y_pt[b] = (double) cd.bin_count[b];
	}
	
    }

    // ============================================

    private void getCumulativeCounts(CachedData cd)
    {
	int total = 0;
	cd.cum_count = new int[n_bins];
	for(int b=0; b < n_bins; b++)
	{
	    total += cd.bin_count[b];
	    cd.cum_count[b] = total;
	}

	// then convert to a wiggly line for display
	
	cd.cum_y_pt = new double[n_bins];
	for(int b=0; b < n_bins; b++)
	    cd.cum_y_pt[b] = (double) cd.cum_count[b];
	
    }

    // ============================================

    private void getProbDensityFunction(CachedData cd)
    {
	if(cd.n_spots > 0)
	{
	    cd.pdf_y_pt = new double[n_bins];
	    
	    final double nspots_in_bin_inv = 1.0 / (cd.bin_gap * (double)cd.n_spots);
	    
	    for(int b=0; b < n_bins; b++)
		cd.pdf_y_pt[b] = (double)cd.bin_count[b] * nspots_in_bin_inv ;
	}
    }
	
    // ============================================

    private boolean hasNaNs( double[] data )
    {
	for(int c=0; c < data.length; c++)
	    if(Double.isNaN(data[c]))
		return true;
	return false;
    }


    private void getQQFunctions(CachedData cd)
    {
	QQcalculator qqc = new QQcalculator();
	
	double[] data = apply_filter_jcb.isSelected() ? edata.getFilteredMeasurementData( cd.m_id ) : edata.getMeasurementData( cd.m_id );

	if(hasNaNs(data))
	{
	    mview.alertMessage( "Measurement '" + edata.getMeasurementName(cd.m_id) + "' contains NaN values\n" + 
				"which cannot be handled in this calculation.\n" +  
				"You can use a filter to hide these values" );
	    return;
	}
	
	if(data.length < 2)
	    return;

	double[][] result = qqc.calculate_cumulative( data );
	cd.qq_cum_x_pt = result[0];
	cd.qq_cum_y_pt = result[1];

	result = qqc.calculate_abscissae( data );
	cd.qq_abs_x_pt = result[0];
	cd.qq_abs_y_pt = result[1];
	
    }

    public class QQcalculator
    {
	// Class to provide ordinate and abscissa values from Normal distribution 
	// N(0,1) that correspond to cumulative distribution points of 
	// sample data. 
	// abscissae[i][0] should be plotted againts abscissae[i][1] over all index values i.
	// cumulative[i][0] should be plotted against cumulative[i][1] over all index values i.
	//
	// The methods are supposed to be applied to normalized data although
	// nothing drastic would happen if the data wasn't normalized.
	//
	// D. Hoyle 20/9/01
	//
	// Last modified 1/11/01
	//
	
	// modified 6/11/01 by DJH to return the result arrays the other way round
	// (i.e. '2 arrays of length N' rather than 'N arrays of length 2' for N points)

	public double[][] calculate_cumulative( double[] data )
	{
	    int i;
	    double[] data_copy = new double[data.length];
	    double[][] cumulative = new double[2][data.length];
	    final double sqr2 = Math.sqrt( 2.0 );
	    double temp1;    
	    
	    //Make copy of array data
	    for( i = 0; i < data.length; i++ )
	    {
		data_copy[i] = data[i];
	    }
	    
	    //Sort array data copy
	    java.util.Arrays.sort( data_copy );
	    
	    //Calculate cumulative    
	    for( i = 0; i < data_copy.length; i++ )
	    {
		cumulative[0][i] = ((double) (i+1)) / ((double) data_copy.length);
		temp1 = erf( Math.abs( data_copy[i] ) / sqr2 );
		if( data_copy[i] > 0.0 )
		{
		    cumulative[1][i] = 0.5 * ( 1.0 + temp1 );
		}
		else
		{
		    cumulative[1][i] = 0.5 * ( 1.0 - temp1 );
		}
	    }
	    data_copy = null;
	    
	    return cumulative;
	}

	/*
	public double[][] calculate_cumulative_ORIGINAL( double[] data )
	{
	    int i;
	    double[] data_copy = new double[data.length];
	    double[][] cumulative = new double[data.length][2];
	    final double sqr2 = Math.sqrt( 2.0 );
	    double temp1;    
	    
	    //Make copy of array data
	    for( i = 0; i < data.length; i++ )
	    {
		data_copy[i] = data[i];
	    }
	    
	    //Sort array data copy
	    java.util.Arrays.sort( data_copy );
	    
	    //Calculate cumulative    
	    for( i = 0; i < data_copy.length; i++ )
	    {
		cumulative[i][0] = ((double) (i+1)) / ((double) data_copy.length);
		temp1 = erf( Math.abs( data_copy[i] ) / sqr2 );
		if( data_copy[i] > 0.0 )
		{
		    cumulative[i][1] = 0.5 * ( 1.0 + temp1 );
		}
		else
		{
		    cumulative[i][1] = 0.5 * ( 1.0 - temp1 );
		}
	    }
	    data_copy = null;
	    
	    return cumulative;
	}
	*/

	// modified to return the result arrays the other way round
	// (i.e. '2 arrays of length N' rather than 'N arrays of length 2' for N points)

	public double[][] calculate_abscissae( double[] data )
	{
	    int i;
	    double[] data_copy = new double[data.length];
	    double[][] abscissae = new double[2][data.length - 1];
	    
	    //Make copy of array data
	    for( i = 0; i < data.length; i++ )
	    {
		data_copy[i] = data[i];
	    }
	    
	    //Sort array data copy
	    java.util.Arrays.sort( data_copy );
	    
	    //Calculate abscissae
	    double Q = 0.0;    
	    for( i = 0; i < data_copy.length - 1; i++ )
	    {
		abscissae[0][i] = data_copy[i];
		Q = ((double) (i+1)) / ((double) data_copy.length);
		abscissae[1][i] = normalQ( Q );
	    }
	    data_copy = null;
	    
	    return abscissae;
	}
	
	/*
	public double[][] calculate_abscissae_ORIGINAL( double[] data )
	{
	    int i;
	    double[] data_copy = new double[data.length];
	    double[][] abscissae = new double[data.length - 1][2];
	    
	    //Make copy of array data
	    for( i = 0; i < data.length; i++ )
	    {
		data_copy[i] = data[i];
	    }
	    
	    //Sort array data copy
	    java.util.Arrays.sort( data_copy );
	    
	    //Calculate abscissae
	    double Q = 0.0;    
	    for( i = 0; i < data_copy.length - 1; i++ )
	    {
		abscissae[i][0] = data_copy[i];
		Q = ((double) (i+1)) / ((double) data_copy.length);
		abscissae[i][1] = normalQ( Q );
	    }
	    data_copy = null;
	    
	    return abscissae;
	}
	*/
	
	private double erf( double  x )
	{
	    //Method to provide rational approximation to 
	    //error function. 
	    //Taken from Handbook of Mathematical Functions, Abramowitz, M.  & Stegun, I.A. p299.
	    
	    
	    int i;
	    double t;
	    final double p = 0.3275911;
	    final double[] a = {0.254829592, -0.284496736, 1.421413741, -1.453152027, 1.061405429};
	    
	    double sum = 0.0;
	    double answer = 0.0;
	    t = 1.0 / (1.0 + ( p * x ));
	    
	    for( i = 4; i >= 0; i-- )
	    {
		sum += a[i];
		sum *= t;
	    }
	    
	    answer = sum * Math.exp( -Math.pow( x, 2.0 ) );
	    answer = 1.0 - answer;
	    
	    return answer;
	}
	
	private double inverf( double y )
	{
	    //Method to find inverse of error function
	    
	    int i;
	    double x = 0.0;
	    double xlow = 0.0;
	    double xhigh = 0.0;
	    double erf_high;
	    double y_out;
	    double temp1;
	    
	    x = xlow = xhigh = 0.0;
	    
	    if( y < 0.0 || y > 1.0 )
	    {
		System.out.println( "Invalid value of argument - should be between 0 and 1" );
		return -1.0;
	    }
	    
	    //Check whether in asymptotic regime. If so use asymptotic 
	    //expansion to invert error function. A value of x = 8.0/sqrt(2) is 
	    //arbitrarily chosen as defining asymptotic regime
	    
	    erf_high = erf( 8.0 / Math.sqrt( 2.0 ) );
	    if( y > erf_high )
	    {
		temp1 = 1.0 - y;
		temp1 *= Math.sqrt( Math.PI );
		x = -Math.log( temp1 );
		x -= 0.5 * Math.log( x );
		x = Math.sqrt( x );
	    }
	    else
	    {
		xlow = 0.0;
		xhigh = 8.0 / Math.sqrt( 2.0 );
		
		for( i = 0; i < 15; i++ )
		{
		    x = 0.5 * ( xlow + xhigh );
		    y_out = erf( x );
		    
		    if( y_out < y )
		    {
			xlow = x;
		    }
		    else
		    {
			xhigh = x;
		    }
		}
	    }
	    
	    return x;
	}   
	
	private double normalQ( double Q )
	{
	    //Method to calculate values of Normally distributed 
	    //variable x ~N(0,1), such that cumulative distribution up to x equals Q.
	    //x = sqrt(2) inverf( 2Q - 1 )
	    
	    double answer = 0.0;
	    
	    if( Q < 0.5 )
	    {
		answer = -inverf( 1.0 - (2.0 * Q) );
	    }
	    else    
	    {
		answer = inverf( (2.0 * Q) - 1.0 );
	    }
	    answer *= Math.sqrt( 2.0 );
	    
	    return answer;
	}
	
    }


    // ============================================

    private void resetDistogramData()
    {
	int n_meas = edata.getNumMeasurements();
	
	meas_display = new boolean[n_meas];
	meas_bin_min = new double[n_meas] ;        
	meas_bin_max = new double[n_meas];
	meas_bin_gap = new double[n_meas];
	meas_hist_data = new int[n_meas][];
	meas_hist_biggest_bin  = new int[n_meas];
	meas_colour = new Color[n_meas];

	float hue = (float)0.0;
	float hue_inc = ((float)0.8 / (float)n_meas);
	float sat = (float) 1.0;
	float brt = (float) 1.0;
	
	for(int m=0; m< n_meas; m++)
	{
	    //int mi = edata.getMeasurementAtIndex(m);
	    
	    meas_hist_data[m] = null;
	    meas_display[m] = (m > 0) ? false : true;
	    
	    // increase the contrast between adjacent colours
	    //  by modulating the saturation and brightness
	    
	    switch(m % 3)
	    {
	    case 0:
		sat = (float) 1.0;
		brt = (float) 1.0;
		break;
	    case 1:
		sat = (float) 0.75;
		brt = (float) 1.0;
		break;
	    case 2:
		sat = (float) 1.0;
		brt = (float) 0.75;
		break;
	    }

	    meas_colour[m] = Color.getHSBColor(hue, sat, brt);
	    hue += hue_inc;
	}
    }

    // ================================================================================================
  


/*
    public void saveImage()
    {

    }

    public void saveImage()
    {
	// create a suitably sized Image and draw into it

	try
	{
	    JFileChooser fc = mview.getFileChooser();

	    int returnVal = fc.showSaveDialog( frame );

	    if (returnVal == JFileChooser.APPROVE_OPTION) 
	    {
		File file = fc.getSelectedFile();

		//JPanel image_panel = new JPanel();

		//image_panel.setMinimumSize( new Dimension( hist_panel[0].getWidth() * 2, hist_panel[0].getHeight() ) );
		
		java.awt.image.BufferedImage image = (java.awt.image.BufferedImage) data_panel.createImage( data_panel.getWidth(),  data_panel.getHeight() );
		
		Graphics g = image.getGraphics();
		
		data_panel.paint( g );
		
		ImageIO.write( image, "png", file );
	    }
	}
	catch( java.io.IOException ioe )
	{
	    mview.alertMessage( "Unable to write the image\n\n" + ioe.getMessage() );
	}
	//catch( UserInputCancelled uic )
	//{
        //
	//}
	
    }

*/

    // ================================================================================================
  
 

    private boolean debug_hist = false;

    private int n_bins = 50;

    private Color[]   meas_colour = null;
    private boolean[] meas_display = null;
    private double[]  meas_bin_min = null;
    private double[]  meas_bin_max = null;
    private double[]  meas_bin_gap = null;
    private int[][]   meas_hist_data = null;
    private int[]     meas_hist_biggest_bin = null;

    private boolean auto_bins;

    private maxdView mview;
    private ExprData edata;
    private DataPlot dplot;

    private boolean filter_alert = false;

    private JFrame frame;
    private DistogramPanel data_panel;

    private JLabel bins_label;
    private JCheckBox[] meas_jcb;
    private JCheckBox apply_filter_jcb;
    private JPanel control_panel;
    private GraphPanel[] hist_panel;
    private JComboBox display_mode_jcb;
    private DragAndDropList meas_list;

    private JLabel status_label;

    private Color background_col;
    private Color text_col;

    private PrintManager print_man;
    private AxisManager axis_man;
    private DecorationManager deco_man;
 
}
