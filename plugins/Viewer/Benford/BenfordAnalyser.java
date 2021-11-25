import javax.swing.*;
import javax.swing.event.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;


public class BenfordAnalyser implements ExprData.ExprDataObserver, Plugin
{
    
    public BenfordAnalyser( maxdView mview_ )
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
   
    double chisq_1stDigit;
    
    public double[] get_Benford_Distribution( double[] data )
    {
	int[] digit_count = new int[9];

	int count = 0;
	    
	boolean apply_filter = apply_filter_jchkb.isSelected();

	for( int i = 0; i < data.length; i++ )
	{
	    if(!apply_filter || (!edata.filter(i)))
	    {
		if(!Double.isNaN( data[i] ))
		{
		    final String str = String.valueOf( Math.abs( data[i] ) );
		    
		    final int len = str.length();
		    
		    int pos = 0;
		    boolean done = false;
		    
		    while( !done )
		    {
			final int digit = Character.digit( str.charAt( pos ), 10 );
			
			if((digit >= 1) && (digit <= 9))
			{
			    digit_count[ digit - 1 ]++;
			    done = true;
			}
			
			if(++pos == len)
			    done = true;
		    }
		    
		    count++;
		}
	    }
	}
	
	double[] benford_dist = new double[9];

	if(count > 0)
	{
	    for( int i = 0; i < 9; i++ )
	    {
		benford_dist[i] = (double) digit_count[i] / (double) count;
	    }
	}
	
	
	//Calculate First digit chi-squared statistic
	chisq_1stDigit = 0.0;
	for( int i = 0; i < 9; i++ )
	{
	    double temp1 = (double) ( i + 1 );
	    temp1 = 1.0 + ( 1.0 / temp1 );
	    temp1 = Math.log( temp1 );
	    temp1 /= Math.log( 10.0 );
	    
	    chisq_1stDigit += Math.pow( ( benford_dist[i] - temp1 ), 2.0 ) / temp1;
	}

	return benford_dist;
    }
    
    public double[] get_Benford_Distribution_theory()
    {
	int i;
	double temp1; 
	double[] bdist_theory = new double[9];
	
	for( i =0; i < 9; i++ )
	{
	    temp1 = (double) ( i + 1 );
	    temp1 = 1.0 + ( 1.0 / temp1 );
	    temp1 = Math.log( temp1 );
	    temp1 /= Math.log( 10.0 );
	    bdist_theory[i] = temp1;
	}
	
	return bdist_theory;
    }    
    
    public double get_chisquared_1stDigit()
    {
	return chisq_1stDigit;
    }
    
    //
    // ------------- GUI ---------------------------------------------------
    //
    
    private void buildGUI()
    {
	GridBagConstraints c;

	frame = new JFrame ("Benford Analysis");
	
	mview.decorateFrame(frame);

	frame.addWindowListener(new WindowAdapter() 
	    {
		public void windowClosing(WindowEvent e)
		{
		    cleanUp();
		}
	    });
	
	GridBagLayout gridbag = new GridBagLayout();
	frame.getContentPane().setLayout(gridbag);
	//frame.getContentPane().setBorder(BorderFactory.createEmptyBorder(3,3,3,3));

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


	graph_panel = new GraphPanel();
	graph_panel.getContext().setBackgroundColour( mview.getBackgroundColour() );
	graph_panel.getContext().setForegroundColour( mview.getTextColour() );
	JScrollPane meas_list_jsp = new JScrollPane(meas_list);
	
	JSplitPane jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	jsp.setLeftComponent(meas_list_jsp);
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
	apply_filter_jchkb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    measurementSelectionChanged();
		}
	    });
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
		    mview.getPluginHelpTopic("BenfordAnalyser", "BenfordAnalyser");
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
	String current_meas_name = (String) meas_list.getSelectedValue();
	int m_id = (current_meas_name == null) ? -1 : edata.getMeasurementFromName( current_meas_name );
	
	meas_list.setModel( new MeasListModel() );
	
	if(m_id >= 0)
	{
	    meas_list.setSelectedValue( current_meas_name, true );
	}
    }
    
    
    private void measurementSelectionChanged()
    {
	int selid = meas_list.getSelectedIndex();
	int m_id = -1;

	if(selid < 0)
	{
	    graph_panel.getContext().removeAllPlots();
	    graph_panel.repaint();
	    return;
	}
	m_id = edata.getMeasurementAtIndex(selid);

	/*
	if(selid < 0)
	{
	    if(current_meas_name != null)
	    {
		m_id = edata.getMeasurementFromName(current_meas_name);
		if(m_id < 0)
		{
		    graph_panel.getContext().removeAllPlots();
		    graph_panel.repaint();
		    return;
		}
	    }
	    else
	    {
		graph_panel.getContext().removeAllPlots();
		graph_panel.repaint();
		return;
	    }
	}
	else
	{
	    m_id = edata.getMeasurementAtIndex(selid);
	}
	
	// remember the name for next time....
	current_meas_name = edata.getMeasurementName(m_id);
	*/


	//double[] test_data = { 1.1111, 2.2221, 3.3331, 4.4441, 5.5551, 7.7772, 8.8882, 9.9992 };
	//double[] test_data = { 0.1, 0.2, 0.3, 0.000004, 0.5, 0.0007, 0.008, 0.09 };
	//double[] test_data = { 0.1, 0.2, Double.NaN, 0.3E-9, 51234, 0.0007, 0.008, 0.09 };
	//double[] result = get_Benford_Distribution( test_data );

	double[] result = get_Benford_Distribution( edata.getMeasurementData(m_id) );

	double[] x_axis = new double[9];
	for(int x=1; x < 10; x++)
	    x_axis[x-1] = x;

	graph_panel.getContext().removeAllPlots();

	graph_panel.getContext().setBackgroundColour( mview.getBackgroundColour() );
	graph_panel.getContext().setForegroundColour( mview.getTextColour() );

	String tstr = ("chi squared = " + mview.niceDouble(get_chisquared_1stDigit(), 10, 5));
	
	graph_panel.getContext().setTitle( tstr );

	//graph_panel.getXAxis().setTicks( x_axis );

	graph_panel.getContext().setLegendAlignment( GraphPlot.ALIGN_RIGHT, GraphPlot.ALIGN_TOP );

	graph_panel.getVerticalAxis().setTickMode(GraphAxis.MIN_MAX_MODE);
	graph_panel.getVerticalAxis().setTickSigDigits( 3 );
	
	// graph_panel.getHorizontalAxis().setTickMode(GraphAxis.CUSTOM_MODE);
	graph_panel.getHorizontalAxis().setTicks( x_axis );
	graph_panel.getHorizontalAxis().setTickSigDigits( 0 );
	
	graph_panel.getHorizontalAxis().setTitle( "Digit" );
	graph_panel.getVerticalAxis().setTitle( "Frequency" );

	graph_panel.getHorizontalAxis().setMouseTracking( false );
	
	graph_panel.getContext().addBarChart( x_axis, result, mview.getTextColour().brighter() );

	graph_panel.getContext().addLinePlot( "Normal", 
					      x_axis, get_Benford_Distribution_theory(), 
					      mview.getTextColour().darker(), GraphPlot.FILLED_CIRCLE_GLYPH );
	
	graph_panel.repaint();

	
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
	PluginInfo pinf = new PluginInfo("Benford Analyser", 
					 "viewer", 
					 "Shows the distribution of first significant digits", "",
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

    
    private JFrame frame;
    private DragAndDropList meas_list;
    private GraphPanel graph_panel;

    private JCheckBox apply_filter_jchkb;

    private maxdView mview;
    private ExprData edata;
    private DataPlot dplot;
    
}
