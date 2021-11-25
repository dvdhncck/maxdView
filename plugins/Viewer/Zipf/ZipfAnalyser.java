import javax.swing.*;
import javax.swing.event.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;


public class ZipfAnalyser implements ExprData.ExprDataObserver, Plugin
{
    
    public ZipfAnalyser( maxdView mview_ )
    {
	mview = mview_;
    }
    
    private void cleanUp()
    {
	edata.removeObserver(this);
	frame.setVisible(false);
	frame = null;
    }

     //
    // ------------- maths bits (written by David Hoyle) ----------------------------------
    //
   
    private double nu = 0.0;           //Zipf's law exponent valu

    private double[] data_copy;        //Temporary copy of the input data
        
    public double[] do_zipf_analysis( double[] data, int max_spots )
    {
	if((data == null) || (data.length == 0))
	    return null;
	if(max_spots < 1)
	    return null;

	//  Method to extract Zipf's law exponent nu
	//  from microarray data set. 
	//
	//  Input array 'data' is mRNA abundances.
	//
	//  Exponent value 'nu' is extracted from top 'max_spots' values.
	//
	//  Primarily intended to be applied to log mRNA abundances but can be 
	//  applied to any data set.
	//
	//  D. Hoyle 21/8/01
	//
	//  Last modified 10/10/01 by DJH
	//
	//  this version reordered so that only the top 'max_spots' values
	//  are 'log'ed rather than the whole data set
	//

	int ngene;                 //No. of values used to extract exponent
	int i;
	
	// System.out.println("checking top " + max_spots + " values");
	
	//Make copy of the raw data

	// (can we use 'data_copy = (double[]) data.clone())' ?)

	data_copy = new double[data.length];
	for( i = 0; i < data.length; i++ )
	{
	    data_copy[i] = data[i];
	}
	
	//Check no. of specified genes is valid
	
	if( max_spots > data_copy.length ) 
	    max_spots = data_copy.length;
	
	//Rank raw array values
	java.util.Arrays.sort( data_copy );
	
	//Log the top values

	final int array_size = data_copy.length;
	
	int bad = 0;
	int nans = 0;

	boolean values_already_logged  = values_already_logged_jchkb.isSelected();

	for( i = array_size - 1 ; i >= array_size - max_spots; i-- )
	{
	    // System.out.println( i +  "\t" + data_copy[i] );

	    if(Double.isNaN( data_copy[i] ))
		nans++;
	    else
	    {
		if(!values_already_logged)
		{
		    if(data_copy[i] <= 0)
			bad++;
		}
	    }
	}
	if(nans > 0)
	{
	    String str = (nans == 1) ? "a NaN value" : (nans + " NaN values");
	    mview.alertMessage("The Zipf analysis cannot be performed because this Measurement\n" + 
			       "contains " + str + " which cannot be converted to logarithmic form\n");
	    return null;
	}

	double min = .0;

	if(bad > 0)
	{
	    String str = (bad == 1) ? "a value" : (bad + " values");
	    if(mview.infoQuestion("This Measurement contains " + str + " which cannot be converted to logarithmic form\n" + 
				  "Apply linear transformation to overcome this?", "Yes", "No") == 1)
		return null;
	    
	    // shift all the values up so the minimum is 1.0
	    min = data_copy[array_size - 1];
	    for( i = array_size - 1 ; i >= array_size - max_spots; i-- )
		if(data[i] < min)
		    min = data_copy[i];
	    
	    min -= 1.0;
	}

	for( i = array_size - 1 ; i >= array_size - max_spots; i-- )
	    data_copy[i] = Math.log( data_copy[i] - min );

	//Fit Zipf's law to top max_spots values
	double temp1, temp2, temp3;
	temp1 = temp2 = temp3 = .0;

	for( i = array_size - 1 ; i >= array_size - max_spots; i-- )
	{
	    double rank = (double) ( array_size - i );
	    temp1 += Math.log( rank );
	    temp2 += Math.pow( Math.log( rank ), 2.0 );
	    temp3 += data_copy[i] * Math.log( rank );
	}
	nu = temp3 / temp2;
	nu -= ( data_copy[array_size - 1] * temp1 / temp2 );
	
	return get_zipf_law_fit( max_spots );

    }
 
    public double get_zipf_exponent()
    {
	//Method returns value of Zipf law exponent
	return nu;
    }
    
    public double[] get_zipf_law_fit( int n1 )
    {
	//Method returns Zipf's law fit for first n1 
	//ranks
	
	int i;
	double[] zipf_fit = new double[n1];

	zipf_fit[0] = data_copy[data_copy.length - 1];

	for( i = 1; i < n1; i++ )
	{
	    zipf_fit[i] = zipf_fit[0] + ( nu *  Math.log( ((double) (i+1)) ) );
	    zipf_fit[i] = Math.exp( zipf_fit[i] );
	}
	zipf_fit[0] = Math.exp( zipf_fit[0] );
	
	return zipf_fit;

    }


    
    //
    // ------------- GUI ---------------------------------------------------
    //
    
    private void buildGUI()
    {
	GridBagConstraints c;

	frame = new JFrame ("Zipf Analysis");
	
	mview.decorateFrame(frame);

	frame.addWindowListener(new WindowAdapter() 
	    {
		public void windowClosing(WindowEvent e)
		{
		    cleanUp();
		}
	    });
	
	GridBagLayout gridbag = new GridBagLayout();
	//frame.getContentPane().setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
	frame.getContentPane().setLayout(gridbag);

	meas_list = new DragAndDropList();
	meas_list.setModel(new MeasListModel());
	meas_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	meas_list.addListSelectionListener(new ListSelectionListener() 
	    {
		public void valueChanged(ListSelectionEvent e) 
		{
		    measurementSelectionChanged();
		}
	    });
	JScrollPane meas_list_jsp = new JScrollPane(meas_list);
	
	n_values_to_test = ( edata.getNumSpots() / 20 );
	if( n_values_to_test < 2 )
	    n_values_to_test = 2;
	n_spots_ls = new LabelSlider("Top spots", JSlider.VERTICAL, JSlider.HORIZONTAL, 2, edata.getNumSpots(), n_values_to_test );
	n_spots_ls.setMode( LabelSlider.INTEGER );
	
	n_spots_ls.addChangeListener( new ChangeListener()
	    {
		public void stateChanged(ChangeEvent e) 
		{
		    n_values_to_test = (int) n_spots_ls.getValue();
		    measurementSelectionChanged();
		}
	    });

	
	JPanel control_panel = new JPanel();
	GridBagLayout control_panel_gridbag = new GridBagLayout();
	control_panel.setLayout(control_panel_gridbag);

	c = new GridBagConstraints();
	c.fill = GridBagConstraints.BOTH;
	c.weighty = 8.0;
	c.weightx = 10.0;
	control_panel_gridbag.setConstraints(meas_list_jsp, c);
	control_panel.add(meas_list_jsp);

	c = new GridBagConstraints();
	c.gridy = 1;
	c.weighty = 1.0;
	c.weightx = 10.0;
	c.fill = GridBagConstraints.BOTH;
	control_panel_gridbag.setConstraints(n_spots_ls, c);
	control_panel.add(n_spots_ls);
	
	values_already_logged_jchkb = new JCheckBox("Use Log/Log scale");
	values_already_logged_jchkb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    measurementSelectionChanged();
		}
	    });
	values_already_logged_jchkb.setSelected(true);
	c = new GridBagConstraints();
	c.gridy = 2;
	//c.fill = GridBagConstraints.BOTH;
	c.weighty = 1.0;
	c.weightx = 10.0;
	c.anchor = GridBagConstraints.CENTER;
	control_panel_gridbag.setConstraints(values_already_logged_jchkb, c);
	control_panel.add(values_already_logged_jchkb);
	
	
	graph_panel = new GraphPanel();
	
	graph_panel.getContext().setBackgroundColour( mview.getBackgroundColour() );
	graph_panel.getContext().setForegroundColour( mview.getTextColour() );
	    
	JSplitPane jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	jsp.setLeftComponent(control_panel);
	jsp.setRightComponent(graph_panel);

	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	c.weighty = c.weightx = 1.0;
	c.fill = GridBagConstraints.BOTH;
	gridbag.setConstraints(jsp, c);
	frame.getContentPane().add(jsp);
	
	JPanel wrapper = new JPanel();
	GridBagLayout w_gridbag = new GridBagLayout();
	wrapper.setBorder(BorderFactory.createEmptyBorder(3,0,3,0));
	wrapper.setLayout(w_gridbag);

	JButton button;

	apply_filter_jchkb = new JCheckBox("Apply filter");
	c = new GridBagConstraints();
	w_gridbag.setConstraints(apply_filter_jchkb, c);
	wrapper.add(apply_filter_jchkb);
	
	button = new JButton("Print");
	wrapper.add(button);
	button.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    new PrintManager( mview, graph_panel, graph_panel ).openPrintDialog();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 1;
	w_gridbag.setConstraints(button, c);

	button = new JButton("Close");
	wrapper.add(button);
	
	button.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    cleanUp();
		}
	    });
	
	c = new GridBagConstraints();
	c.gridx = 2;
	w_gridbag.setConstraints(button, c);

	
	button = new JButton("Help");
	wrapper.add(button);
	
	button.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    mview.getPluginHelpTopic("ZipfAnalyser", "ZipfAnalyser");
		}
	    });
	
	c = new GridBagConstraints();
	c.gridx = 3;
	w_gridbag.setConstraints(button, c);

	
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 1;
	c.gridwidth = 2;
	c.weightx = 10.0;
	gridbag.setConstraints(wrapper, c);
	frame.getContentPane().add(wrapper);

	populateMeasurementList();
  }
    
    private void populateMeasurementList()
    {
	meas_list.setModel( new MeasListModel() );
    }
    
    
    private void measurementSelectionChanged()
    {
	int selid = meas_list.getSelectedIndex();
	if(selid < 0)
	{
	    graph_panel.getContext().removeAllPlots();
	    graph_panel.repaint();
	    return;
	}

	int m_id = edata.getMeasurementAtIndex(selid);

	double[] data = apply_filter_jchkb.isSelected() ? edata.getFilteredMeasurementData( m_id ) : edata.getMeasurementData( m_id );


	double[] y2_axis = do_zipf_analysis( data , n_values_to_test );
	double[] y1_axis = new double[n_values_to_test];
	for( int i = 0; i < n_values_to_test; i++ )
	{
	    y1_axis[i] = Math.exp( data_copy[data_copy.length -(i+1)] );
	}

	if(y1_axis  != null)
	{
	    graph_panel.getContext().setBackgroundColour( mview.getBackgroundColour() );
	    graph_panel.getContext().setForegroundColour( mview.getTextColour() );
	    
	    graph_panel.getContext().removeAllPlots();
	    
	    if(values_already_logged_jchkb.isSelected())
	    {
		graph_panel.getVerticalAxis().setLog();
		graph_panel.getHorizontalAxis().setLog();
	    }
	    else
	    {
		graph_panel.getVerticalAxis().setLinear();
		graph_panel.getHorizontalAxis().setLinear();
	    }
	
	    graph_panel.getVerticalAxis().setTickMode(GraphAxis.MIN_MAX_MODE);
	    graph_panel.getHorizontalAxis().setTickMode(GraphAxis.MIN_MAX_MODE);
	    
	    graph_panel.getVerticalAxis().setTickSigDigits( 3 );
	    graph_panel.getHorizontalAxis().setTickSigDigits( 3 );
	    
	    graph_panel.getVerticalAxis().setTitle( "Spot Intensity" );
	    graph_panel.getHorizontalAxis().setTitle( "Rank" );

	    //graph_panel.getContext().setBackgroundColour( mview.getBackgroundColour() );
	    //graph_panel.getContext().setForegroundColour( mview.getTextColour() );
	    
	    String tstr = ("nu = " + mview.niceDouble( get_zipf_exponent(), 10, 3));
	    
	    graph_panel.getContext().setTitle( tstr );
	    
	    final int array_size = data_copy.length;
	    double[] x_axis = new double[n_values_to_test];
	    for( int i = 0 ; i < n_values_to_test; i++ )
		x_axis[i] = (double) (i+1);
	    
	    
	    graph_panel.getContext().addLinePlot( "Original", x_axis, y1_axis, mview.getTextColour() );
	    graph_panel.getContext().addLinePlot( "Zipf's Law", x_axis, y2_axis, Color.red );
	    graph_panel.getContext().setAutoGrid(8);
	    
	    graph_panel.repaint();
	}
	
    }

    private void OLD_measurementSelectionChanged()
    {
	int selid = meas_list.getSelectedIndex();
	if(selid < 0)
	{
	    graph_panel.getContext().removeAllPlots();
	    graph_panel.repaint();
	    return;
	}

	int m_id = edata.getMeasurementAtIndex(selid);

	double[] data = apply_filter_jchkb.isSelected() ? edata.getFilteredMeasurementData( m_id ) : edata.getMeasurementData( m_id );

	double[] y_axis = do_zipf_analysis( data , n_values_to_test );

	if(y_axis  != null)
	{
	    graph_panel.getContext().setBackgroundColour( mview.getBackgroundColour() );
	    graph_panel.getContext().setForegroundColour( mview.getTextColour() );
	    
	    graph_panel.getContext().removeAllPlots();
	    
	    if(values_already_logged_jchkb.isSelected())
	    {
		graph_panel.getVerticalAxis().setLog();
		graph_panel.getHorizontalAxis().setLog();
	    }
	    else
	    {
		graph_panel.getVerticalAxis().setLinear();
		graph_panel.getHorizontalAxis().setLinear();
	    }
	
	    graph_panel.getVerticalAxis().setTickMode(GraphAxis.MIN_MAX_MODE);
	    graph_panel.getHorizontalAxis().setTickMode(GraphAxis.MIN_MAX_MODE);
	    
	    graph_panel.getVerticalAxis().setTickSigDigits( 3 );
	    graph_panel.getHorizontalAxis().setTickSigDigits( 3 );
	    
	    graph_panel.getVerticalAxis().setTitle( "Zipf" );
	    graph_panel.getHorizontalAxis().setTitle( "Original" );

	    //graph_panel.getContext().setBackgroundColour( mview.getBackgroundColour() );
	    //graph_panel.getContext().setForegroundColour( mview.getTextColour() );
	    
	    String tstr = ("nu = " + mview.niceDouble( get_zipf_exponent(), 10, 3));
	    
	    graph_panel.getContext().setTitle( tstr );
	    
	    final int array_size = data_copy.length;
	    double[] x_axis = new double[n_values_to_test];
	    for( int i = array_size - 1 ; i >= array_size - n_values_to_test; i-- )
		x_axis[array_size - (i+1)] = data_copy[i];
	    
	    
	    graph_panel.getContext().addLinePlot( null, x_axis, y_axis, mview.getTextColour() );
	    
	    graph_panel.getContext().setAutoGrid(8);
	    
	    graph_panel.repaint();
	}
	
    }

    public class MeasListModel extends DefaultListModel
    {
	public Object getElementAt(int index) 
	{
	    return edata.getMeasurementName( edata.getMeasurementAtIndex(index) );
	}
	public int getSize() 
	{
	    return edata.getNumMeasurements();
	}
    }
    
    //
    // ------------- GraphPanel ---------------------------------
    //
    
    //
    // ------------- plugin implementation ---------------------------------
    //
    
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
    }

    public void stopPlugin()
    {
	cleanUp();
    }

    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("Zipf Analyser", 
					 "viewer", 
					 "Analyses the tail of a distribution", "",
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
  
    //
    // ------------- observer implementation ---------------------------------
    //

    public void dataUpdate(ExprData.DataUpdateEvent due)
    {
	switch(due.event)
	{
	case ExprData.SizeChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	    populateMeasurementList();
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
	    populateMeasurementList();
	    break;
	}
    }
    
    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
	graph_panel.getContext().setBackgroundColour( mview.getBackgroundColour() );
	graph_panel.getContext().setForegroundColour( mview.getTextColour() );
	graph_panel.repaint();

    }


    //
    // ------------- state -------------------------------------------------
    //

    private int n_values_to_test;
    private JFrame frame;
    private DragAndDropList meas_list;
    private GraphPanel graph_panel;
    private JCheckBox apply_filter_jchkb;
    private JCheckBox values_already_logged_jchkb;
    private LabelSlider n_spots_ls;
    private maxdView mview;
    private ExprData edata;
    private DataPlot dplot;
    
}
