import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.*;

public class IDNormaliser implements Normaliser
{    
    public IDNormaliser()
    {
    }

    public String getName() { return "Intensity Dependant" ; }

    public boolean canHandleNaNs() { return false; }

    public int getMinimumNumMeasurements() { return 2; }
    public int getMaximumNumMeasurements() { return 2; }
    public int getNumberOfReturnedMeasurements( int number_of_measurements_given ) { return 1; }
    public boolean isOneToOne() { return false; }

    public String getHelpDocumentName() { return "IDNormaliser"; }

    public JPanel  getUI(final maxdView mview, final NormaliserInput input )
    { 
	loadProperties( mview );

	return makeUserInterface( mview, input ); 
    }


    public String getInfo() 
    { 
	return "Intensity Dependant Normalization.\n\nMethods for removing intensity dependent bias in two colour microarray data.\n\nThe Loess (Lowess) method of Yang et al 2002. (Nucl. Acids. Res. volume 30:e15) is provided as the default option.\n\nOther scatter plot smoothing methods will be added in the future.\n\nThe mean log-ratio is set zero"; 
    }

    public String getSettings() 
    { 
//	return new String("Noise Threshold=" + noise_thresh_ls.getValue() ); 
	return "method=Loess";
    }

    public void   saveProperties(maxdView mview) 
    {
	syncStateFromGUI();

	//mview.putDoubleProperty("IDNormaliser.noise_threshold", noise_thresh );
	mview.putBooleanProperty("IDNormaliser.use_grid", use_grid );
	mview.putDoubleProperty("IDNormaliser.grid_points", grid_points );
	mview.putDoubleProperty("IDNormaliser.span_percent", span_percent );
	mview.putBooleanProperty("IDNormaliser.log_data", log_data );
	mview.putBooleanProperty("IDNormaliser.show_ma_plot", show_ma_plot );
	mview.putProperty("IDNormaliser.method", method_strs[method] );
	mview.putProperty("IDNormaliser.kernel", kernel_strs[kernel] );
    }

    public void   loadProperties(maxdView mview) 
    {
	//noise_thresh = mview.getDoubleProperty("IDNormaliser.noise_threshold", 0.001 );
	use_grid     = mview.getBooleanProperty("IDNormaliser.use_grid", true );
	//grid_points  = mview.getDoubleProperty("IDNormaliser.grid_points", 33 );
	span_percent = mview.getDoubleProperty("IDNormaliser.span_percent", 30 );
	log_data     = mview.getBooleanProperty("IDNormaliser.log_data", false );
	show_ma_plot = mview.getBooleanProperty("IDNormaliser.show_ma_plot", true );

	method = stringToCode( method_strs, mview.getProperty("IDNormaliser.method", method_strs[0] ) );
	if(method < 0)
	    method = 0;

	kernel = stringToCode( kernel_strs,  mview.getProperty("IDNormaliser.kernel", kernel_strs[0] ) );
	if(kernel < 0)
	    kernel = 0;
    }

    // ===== plugin command ======================================================

    public PluginCommand getCommand() 
    {
	String[] args = 
	{
	    // name             // type            //default   // flags   // comment
	    "log_data",        "boolean",          "false",    "",        "log the normalised data",
	    "show_ma_plot",    "boolean",          "false",    "",        "display a plot of M against A",

	    "red_data",        "measurement_list", "",         "",        "which Measurement to use as the 'green' data",
	    "green_data",      "measurement_list", "",         "",        "which Measurement to use as the 'red' data",
	};
	return new PluginCommand( "IDNormalise", args );
    }

    public void parseArguments( final maxdView mview, final String args[] )
    {
	log_data     = mview.getPluginBooleanArg( "log_data",     args, false );
        show_ma_plot = mview.getPluginBooleanArg( "show_ma_plot", args, true );

	String[] red_data   = mview.getPluginMeasurementListArg( "red_data",   args, null );
	String[] green_data = mview.getPluginMeasurementListArg("green_data",  args, null );

	if(( red_data != null ) && ( red_data.length > 0 ))
	    data1_jcb.setSelectedItem( red_data[0] );

	if(( green_data != null ) && ( green_data.length > 0 ))
	    data2_jcb.setSelectedItem( green_data[0] );

	syncGUIFromState();
    }

    
    // ===== user interface ======================================================

    private maxdView mview = null;

    private JComboBox data1_jcb, data2_jcb;
    private JComboBox flags1_jcb, flags2_jcb;
    private JComboBox method_jcb;
    private JCheckBox log_data_jchkb;
    private JComboBox kernel_jcb;
    private LabelSlider grid_points_ls;
    private LabelSlider span_percent_ls;
    private LabelSlider noise_thresh_ls;
    private JRadioButton use_grid_jrb;
    private JRadioButton use_span_jrb;
    
    private JCheckBox show_plot_jchkb;

    private double noise_thresh = 0.1;
    private boolean log_data;
    private boolean show_ma_plot;
    private int method;
    private int kernel;
    private double grid_points;
    private double span_percent;
    private boolean use_grid;

    public final String[] kernel_strs = { "TriCube", "Epanechnikov"  };
    public final String[] method_strs = { "Loess", "LUD Dye Correction", "SVD Dye Correction" }; 

    
    private int stringToCode( String[] ss, String s ) 
    {
	for( int i=0; i < ss.length; i++ )
	    if( ss[i].equals(s) )
		return i;
	return -1;
    }

    private JPanel makeUserInterface(final maxdView mview_, final NormaliserInput input )
    {
	mview = mview_;

	JLabel label;
	GridBagConstraints c;
	Box.Filler filler;

	final JPanel basic_opts    = new JPanel();
	final JPanel advanced_opts = new JPanel();
	final JPanel ui            = new JPanel();
	
	Dimension fillsize = new Dimension(10,10);
	
	// ==================================================

	int line = 0;

	GridBagLayout gridbag = new GridBagLayout();
	basic_opts.setLayout(gridbag);


	label = new JLabel("'Red' data ");
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.EAST;
	c.gridy = line;
	gridbag.setConstraints(label, c);
	basic_opts.add(label);
	
	data1_jcb = new JComboBox( input.measurement_names );
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.gridx = 1;
	c.anchor = GridBagConstraints.WEST;
	c.gridy = line;
	gridbag.setConstraints(data1_jcb, c);
	basic_opts.add(data1_jcb);
	
	line++;

	label = new JLabel("'Red' flags ");
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.EAST;
	c.gridy = line;
	//gridbag.setConstraints(label, c);
	//basic_opts.add(label);
	
	flags1_jcb = new JComboBox( input.spot_attr_names );
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.gridx = 1;
	c.anchor = GridBagConstraints.WEST;
	c.gridy = line;
	//gridbag.setConstraints(flags1_jcb, c);
	//basic_opts.add(flags1_jcb);
	
	line++;

	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.gridy = line;
	gridbag.setConstraints(filler, c);
	basic_opts.add(filler);

	line++;

	label = new JLabel("'Green' data ");
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.EAST;
	c.gridy = line;
	gridbag.setConstraints(label, c);
	basic_opts.add(label);
	
	data2_jcb = new JComboBox( input.measurement_names );
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.gridx = 1;
	c.anchor = GridBagConstraints.WEST;
	c.gridy = line;
	gridbag.setConstraints(data2_jcb, c);
	basic_opts.add(data2_jcb);
	
	if( input.measurement_names.length > 1 )
	    data2_jcb.setSelectedIndex(1);

	line++;

	label = new JLabel("'Green' flags ");
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.EAST;
	c.gridy = line;
	//gridbag.setConstraints(label, c);
	//basic_opts.add(label);
	
	flags2_jcb = new JComboBox( input.spot_attr_names );
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.gridx = 1;
	c.anchor = GridBagConstraints.WEST;
	c.gridy = line;
	//gridbag.setConstraints(flags2_jcb, c);
	//basic_opts.add(flags2_jcb);

	line++;

	
	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.gridy = line;
	gridbag.setConstraints(filler, c);
	basic_opts.add(filler);

	line++;

	
	label = new JLabel("Method ");
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.EAST;
	c.gridy = line;
	gridbag.setConstraints(label, c);
	basic_opts.add(label);
	
	method_jcb = new JComboBox( method_strs );
	method_jcb.setEnabled(false);
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.gridx = 1;
	c.anchor = GridBagConstraints.WEST;
	c.gridy = line;
	gridbag.setConstraints(method_jcb, c);
	basic_opts.add(method_jcb);
	
	line++;

	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.gridy = line;
	gridbag.setConstraints(filler, c);
	basic_opts.add(filler);

	line++;

/*
	label = new JLabel("Noise threshold ");
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.EAST;
	c.gridy = line;
	gridbag.setConstraints(label, c);
	basic_opts.add(label);
	
	noise_thresh_ls = new LabelSlider( null, JSlider.VERTICAL, JSlider.HORIZONTAL, 0, 100, noise_thresh );
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.gridx = 1;
	c.gridy = line;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(noise_thresh_ls, c);
	basic_opts.add(noise_thresh_ls);
	
	line++;
*/

	log_data_jchkb = new JCheckBox( "LOG data after normalisation" );
	log_data_jchkb.setEnabled( false );
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.gridx = 1;
	c.gridy = line;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(log_data_jchkb, c);
	basic_opts.add(log_data_jchkb);
	

	line++;
	
	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.gridy = line;
	gridbag.setConstraints(filler, c);
	basic_opts.add(filler);
	
	line++;

	show_plot_jchkb = new JCheckBox( "Show 'MA' plot after normalisation" );

	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.gridx = 1;
	c.gridy = line;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(show_plot_jchkb, c);
	basic_opts.add(show_plot_jchkb);
	
	line++;

	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.gridy = line;
	gridbag.setConstraints(filler, c);
	basic_opts.add(filler);
	
	line++;

	JButton options_jb = new JButton( "Advanced options" );
	options_jb.setEnabled( false );
	options_jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    basic_opts.setVisible( false );
		    advanced_opts.setVisible( true );
		    ui.revalidate();
		}
	    });
	options_jb.setFont( mview.getSmallFont() );
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.gridx = 1;
	c.gridy = line;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(options_jb, c);
	basic_opts.add(options_jb);
	
	line++;


	// ==================================================
	

	line = 0;

	gridbag = new GridBagLayout();
	advanced_opts.setLayout(gridbag);


	label = new JLabel("Kernel ");
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.EAST;
	c.gridy = line;
	gridbag.setConstraints(label, c);
	advanced_opts.add(label);
	
	kernel_jcb = new JComboBox( kernel_strs );
	kernel_jcb.setEnabled(false);
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.gridx = 1;
	c.anchor = GridBagConstraints.WEST;
	c.gridy = line;
	gridbag.setConstraints(kernel_jcb, c);
	advanced_opts.add(kernel_jcb);
	
	line++;

	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.gridy = line;
	gridbag.setConstraints(filler, c);
	advanced_opts.add(filler);
	
	line++;

	use_grid_jrb = new JRadioButton( "Use discrete grid" );
	use_grid_jrb.setEnabled(false);
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.gridx = 1;
	c.gridy = line;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(use_grid_jrb, c);
	//advanced_opts.add(use_grid_jrb);

	line++;

	use_span_jrb = new JRadioButton( "Use spanning" );
	use_span_jrb.setEnabled(false);
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.gridx = 1;
	c.gridy = line;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(use_span_jrb, c);
	//advanced_opts.add(use_span_jrb);
	
	ButtonGroup bg = new ButtonGroup();
	bg.add( use_grid_jrb );
	bg.add( use_span_jrb );

	line++;

	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.gridy = line;
	gridbag.setConstraints(filler, c);
	advanced_opts.add(filler);

	line++;

	label = new JLabel("Grid points ");
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.EAST;
	c.gridy = line;
	gridbag.setConstraints(label, c);
	advanced_opts.add(label);
	
	grid_points_ls = new LabelSlider( null, JSlider.VERTICAL, JSlider.HORIZONTAL, 5, 95, 33 );
	grid_points_ls.setMode( LabelSlider.INTEGER );
	grid_points_ls.setEnabled(false);
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.gridx = 1;
	c.gridy = line;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(grid_points_ls, c);
	//advanced_opts.add(grid_points_ls);

	line++;

	label = new JLabel("Span %");
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.EAST;
	c.gridy = line;
	gridbag.setConstraints(label, c);
	advanced_opts.add(label);
	
	span_percent_ls = new LabelSlider( null, JSlider.VERTICAL, JSlider.HORIZONTAL, 1, 99, 33 );
	span_percent_ls.setEnabled(false);
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.gridx = 1;
	c.gridy = line;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(span_percent_ls, c);
	advanced_opts.add(span_percent_ls);

	
	line++;


	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.gridy = line;
	gridbag.setConstraints(filler, c);
	advanced_opts.add(filler);
	
	line++;

	JButton hide_options_jb = new JButton( "Hide advanced options" );
	hide_options_jb.setFont( mview.getSmallFont() );
	hide_options_jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    advanced_opts.setVisible( false );
		    basic_opts.setVisible( true );
		    ui.revalidate();
		}
	    });
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.gridx = 1;
	c.gridy = line;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(hide_options_jb, c);
	advanced_opts.add(hide_options_jb);
	
	line++;

	// ==================================================
	

	ui.add( basic_opts );
	ui.add( advanced_opts );

	advanced_opts.setVisible( false );

	syncGUIFromState();

	return ui;
    }


    private void syncStateFromGUI()
    {
        //noise_thresh = noise_thresh_ls.getValue();
	log_data     = log_data_jchkb.isSelected();
	method       = method_jcb.getSelectedIndex();
	kernel       = kernel_jcb.getSelectedIndex();
	grid_points  = grid_points_ls.getValue();
	span_percent    = span_percent_ls.getValue();
	use_grid     = use_grid_jrb.isSelected();
    }

    private void syncGUIFromState()
    {
        //noise_thresh_ls.setValue(noise_thresh);
	log_data_jchkb.setSelected(log_data);
	show_plot_jchkb.setSelected(show_ma_plot);
	method_jcb.setSelectedItem( method_strs[method] );
	kernel_jcb.setSelectedItem( kernel_strs[kernel] );
	grid_points_ls.setValue(grid_points);
	span_percent_ls.setValue(span_percent);
	use_grid_jrb.setSelected(use_grid);
	use_span_jrb.setSelected(!use_grid);
    }


    // ===== preview mode ========================================================

    private void showMAPlot( final double[][] points )
    {
	GraphFrame gframe = new GraphFrame( mview, "Normaliser Preview", 2 );
	mview.decorateFrame( gframe );

	GraphPanel gpanel = gframe.getGraphPanel(0);
	
	gpanel.getContext().setTitle( "'MA' Plot : Uncorrected Log Ratios" );
	
	gpanel.getHorizontalAxis().setTitle( "A (log intensity)" );
	gpanel.getVerticalAxis().setTitle( "M (log ratio)" );

	gpanel.getContext().setBackgroundColour( mview.getBackgroundColour() );
	gpanel.getContext().setForegroundColour( mview.getTextColour() );

	gpanel.getContext().addScatterPlot( points[0], points[1], mview.getTextColour().brighter() );

	gpanel.getContext().addScatterPlot( points[0], points[3], Color.black, GraphPlot.CIRCLE_GLYPH );


	// plot  0 against 2 as scatter plot in adjacent panel called "'MA' Plot : Corrected Log Ratios"
	gpanel = gframe.getGraphPanel(1);
	
	gpanel.getContext().setTitle( "'MA' Plot : Corrected Log Ratios" );
	
	gpanel.getHorizontalAxis().setTitle( "A (log intensity)" );
	gpanel.getVerticalAxis().setTitle( "M (log ratio)" );

	gpanel.getContext().setBackgroundColour( mview.getBackgroundColour() );
	gpanel.getContext().setForegroundColour( mview.getTextColour() );

	gpanel.getContext().addScatterPlot( points[0], points[2], mview.getTextColour().brighter() );
    }


    // ===========================================================================


    private double[] getNumericSpotAttributeData( String meas_name, String spot_attr_name )
    {
	final int m_id = mview.getExprData().getMeasurementFromName( meas_name );

	ExprData.Measurement meas = mview.getExprData().getMeasurement( m_id );
	
	final int sa_id = meas.getSpotAttributeFromName( spot_attr_name );
	
	final int sa_dt = meas.getSpotAttributeDataTypeCode( sa_id );

	System.out.println("  checking sa=" + spot_attr_name + " for meas=" + meas_name );

	if( sa_dt == ExprData.Measurement.SpotAttributeDoubleDataType )
	{
	    return (double[]) meas.getSpotAttributeData( sa_id );
	}

	if( sa_dt == ExprData.Measurement.SpotAttributeIntDataType )
	{
	    int[] data_i = (int[]) meas.getSpotAttributeData( sa_id );
	    double[] data_d = new double[ data_i.length ];
	    for(int d=0; d < data_i.length; d++)
		data_d[d] = (double) data_i[d];

	    return data_d;
	}

	System.out.println("  ....dt not INT or DBL");

	return null;
    }


    // ===========================================================================

    public NormaliserOutput normalise( NormaliserInput input )
    {
	syncStateFromGUI();

	int red_m   = data1_jcb.getSelectedIndex();
	int green_m = data2_jcb.getSelectedIndex();

	String red_f   = (String) flags1_jcb.getSelectedItem();
	String green_f = (String) flags2_jcb.getSelectedItem();

	double[][] selected_pair = new double[2][];

	selected_pair[0] = input.data[ red_m   ];
	selected_pair[1] = input.data[ green_m ];


	double[][] points = MicroArrayIntensityLowess.normalize( selected_pair[0], selected_pair[1] );

	if( show_plot_jchkb.isSelected() )
	{
	    showMAPlot( points );
	}


	String[] new_name = { "Lowess:" + input.measurement_names[ red_m ] + ":" +  input.measurement_names[ green_m ] };
	
	double[][] new_data = new double[1][];
	new_data[0] = points[2];


	return new NormaliserOutput( new_data, new_name );


    }


    // ===========================================================================

/*
    public abstract class Kernel
    {
	//Scale parameter sets "width" of kernel 
	double scale;
	
	//Default constructor defined so that scale is automatically  set
	//to some non-zero value
	public Kernel()
	{
	    scale = 1.0;
	}
	
	//This Method returns kernel value
	//x0 is where center of kernel will be placed
	//x1 point where I wish to calculate kernel value given the center x0
	//Most kernels will be of form f( |x0 - x1| / scale )
	public abstract double value( double x0, double x1 );
	
	//Accessor methods
	public void setScale( double lambda )
	{
	    scale = lambda;
	}
	
	public double getScale()
	{
	    double lambda = scale;
	    return lambda;
	}
    }
    
    public class TriCube extends Kernel
    {
	public double value( double x0, double x1 )
	{
	    double s = Math.abs( x0 - x1 );
	    s /= scale;
	    
	    double ker = 0.0;
	    if( s <= 1.0 )
	    {
	    	ker = Math.pow( (1.0 - Math.pow( s, 3.0 ) ), 3.0 );
	    }

	    return ker;
	}
    }

    
    public class Epanechnikov extends Kernel
    {
	public double value( double x0, double x1 )
	{
	    double s = Math.abs( x0 - x1 );
	    s /= scale;
	    
	    double ker = 0.0;
	    if( s <= 1.0 )
	    {
	    	ker = (3/4)* (1.0 - Math.pow( s, 2.0 ) );
	    }
	    
	    return ker;
	}
    }
*/

    // ===========================================================================

/*
    // aggregate class and it's associated comparator 
    // ( used for sorting double values whilst keeping original indices )

    private class DoubleIntTuple
    {
	public double a;
	public int b;
	
	DoubleIntTuple()
	{
	    a = 0.0;
	    b = 0;
	}
	
	public void setValues( double atmp, int btmp )
	{
	    a = atmp;
	    b = btmp;
	}
    }

    private class DoubleIntTupleComparator implements java.util.Comparator
    {
	public int compare( Object o1, Object o2 )
	{

	    DoubleIntTuple my1 = (DoubleIntTuple) o1;
	    DoubleIntTuple my2 = (DoubleIntTuple) o2;

	    if( my1.a > my2.a )
	    {
		return 1;
	    }
	    else if( my1.a < my2.a )
	    {
		return -1;
	    }
	    else
	    {
		return 0;
	    }
	}
    }
*/

    // ===========================================================================

/*
    private double[][] normaliseNoGrid( double[][] data, Kernel kernel,double f )
    {
	//Array data consists of x and y values 
	//For microarray work data[i][0] = log(R), data[i][1] = log(G)
	
	//Sort intensity values
	//We need to sort on intensity values (A = 1/2[log(R) + log(G)]) 
	//whilst keeping track of ratio values (M = log(R/G)). To do this use DoubleIntTuple class and
	//MyDoublePairs_Comparator
	
	DoubleIntTuple[] intensity = new DoubleIntTuple[data.length];
	
	//Load intensity values and array indices into DoubleIntTuple objects 
	double temp1;
	for( int i = 0; i < data.length; i++ )
	{
	    temp1 = 0.5 * ( data[i][0] + data[i][1] );
	    intensity[i] = new DoubleIntTuple();
	    intensity[i].setValues( temp1, i );
	}
	
	//Sort A values whilst keeping track of array indices
	Arrays.sort( intensity, new DoubleIntTupleComparator() );
	
	//Create independent and dependent variables
	//x = A, y = M
	double[] x = new double[data.length];
	double[] y = new double[data.length];
	
	
	for( int i = 0; i < data.length; i++ )
	{
	    x[i] = intensity[i].a;
	    y[i] = data[intensity[i].b][0] - data[intensity[i].b][1];
	}
	
	
	//Calculate regression at data points
	double[][] corrected_channels = new double[data.length][2];   //Quantity to be returned
	double correction;   //Temporary variable to hold correction to applied to the data
	double x0;    		    //Center of kernel
	double k, kx, kx2, kxy, ky;    //Sums for calculating straight line at x0 (also called x[n])
	double detA;                   //determinant of 2x2 matrix used for straight line determination
	double ker = 0.0;              //kernel value
	double alpha;                  //intercept of straight line through x0
	double beta;                   //Slope of straight line through x0
	int n_low, n_upper;            //Array indices of data points defining span about x0
	double trial_scale1, trial_scale2, scale;   //Scale parameters for kernel
	
	//Calculate No. of data points corresponding to fraction f
	int span = (int) Math.floor( 0.5 * f * ((double)data.length));
	
	//Loop of all spots and do Lowess at each of them
	for( int n = 0; n < x.length; n++ )
	{
	    
	    //Initialize sums
	    k = kx = kx2 = ky = kxy = 0.0;
	    
	    //Calculate max and min data points to be included at x[n](i.e. x0)
	    n_upper = (n + span) < data.length ? (n + span):data.length;
	    n_low = (n - span ) > 0 ? (n - span) : 0;
	    
	    //Trial scales are distance from x[n] to right and left hand edges of kernel respectively
	    //Set with of kernel to larger of the two
	    trial_scale1 = x[n_upper -1] - x[n];
	    trial_scale2 = x[n] - x[n_low];
	    
	    //Set kernel scale appropriately
	    scale = trial_scale1 > trial_scale2 ? trial_scale1:trial_scale2;
	    kernel.setScale( scale );
	    
	    //Loop over data points allowed by kernel and weight appropriately
	    for( int i = n_low; i < n_upper; i++ )
	    {
		ker = kernel.value(x[n], x[i]);
		k += ker;
		kx += ker * x[i];
		kx2 += ker * Math.pow( x[i], 2.0 );
		ky += ker * y[i];
		kxy += ker * x[i] * y[i];
	    }
	    k /= (double) (x.length);
	    kx /= (double) (x.length);
	    kx2 /= (double) (x.length);
	    ky /= (double) (x.length);
	    kxy /= (double) (x.length);
	    
	    detA = (k * kx2) - Math.pow( kx, 2.0 );
	    alpha = (kx2 * ky) - (kx * kxy);
	    alpha /= detA;
	    
	    beta = (k * kxy) - (kx * ky);
	    beta /= detA;
	    
	    
	    
	    //Local trend is at x[n] is given by alpha + (beta*x[n])
	    //Corrected ratio should be residual between original ratioy[n] and 
	    //local trend, i.e. y[n] - (alpha + beta*x[n])
	    
	    //System.out.println( n + "\t\t" + alpha + "\t\t" + beta +"\t\t" + kx );
	    //Make corrections to data
	    
	    correction = -0.5 * ( alpha + (beta * x[n]) );
	    corrected_channels[intensity[n].b][0] = data[intensity[n].b][0] + correction;
	    corrected_channels[intensity[n].b][1] = data[intensity[n].b][1] - correction;
	    
	    // corrected_ratio[n][0] = y[n] -corrected_ratio[n][2];
	    //   corrected_ratio[n][1] = alpha + (beta * x[n]);
	    //   corrected_ratio[n][2] = x[n]; 
	}
	
	return corrected_channels;
    }    
*/

    /* 
    public double[][] normaliseOnGrid( double[][] data, Kernel kernel, int ngrid )
    {
	//Array data consists of x and y values 
	//For microarray work data[i][0] = log(R), data[i][1] = log(G)
	
	//Sort intensity values
	//We need to sort on intensity values (A = 1/2[log(R) + log(G)]) 
	//whilst keeping track of ratio values (M = log(R/G)). To do this use DoubleIntTuple class and 
	//MyDoublePairs_Comparator
	
	DoubleIntTuple[] intensity = new DoubleIntTuple[data.length];
	
	//Load intensity values and array indices into DoubleIntTuple objects
	double temp1;
	for( int i = 0; i < data.length; i++ )
	{
	    temp1 = 0.5 * ( data[i][0] + data[i][1] );
	    intensity[i] = new DoubleIntTuple();
	    intensity[i].setValues( temp1, i );
	}
	
	//Sort A values whilst keeping track of array indices
	Arrays.sort( intensity, new DoubleIntTupleComparator() );
	
	//Create independent and dependent variables
	//x = A, y = M
	double[] x = new double[data.length];
	double[] y = new double[data.length];
	
	
	for( int i = 0; i < data.length; i++ )
	{
	    x[i] = intensity[i].a;
	    y[i] = data[intensity[i].b][0] - data[intensity[i].b][1];
	}
	
	//Find range of data
	double xmin = Double.MAX_VALUE;
	double xmax = -Double.MAX_VALUE;
	for( int i = 0; i < x.length; i++ )
	{
	    if( x[i] < xmin )
	    {
		xmin = x[i];
	    }
	    
	    if( x[i] > xmax )
	    {
		xmax = x[i];
	    }
	}
	
	
	//Calculate grid spacing
	double delta_x = (xmax - xmin) / ((double) (ngrid -1));
	
	double[] alpha = new double[ngrid - 2];
	double[] beta = new double[ngrid - 2];
	
	//Variables for regression calculation
	double x0;
	double k, kx, kx2, kxy, ky;
	double detA;
	double ker = 0.0;
	
	
	//Variables to set scale of kernel
	int n_low, n_upper;            //Array indices of data points defining span about x0
	double trial_scale1, trial_scale2, scale;   //Scale  parameters for kernel
	
	//Calculate No. of data points corresponding to fraction f
	double f = 1;  //scale needs to be assigned
	int span = (int) Math.floor( 0.5 * f * ((double)data.length));
	
	
	for( int n = 1; n < ngrid-1; n++ )
	{
	    //Calculate intensity value at which lowess to be performed
	    x0 = xmin + ( ((double) n) * delta_x );
	    
	    //Find spot with intensity value closest to x0
	    //Call it xclosest, iclosest. For Geraint to do......
	    int iclosest = 1;
	    for (int i=0; i<x.length;i++)
	    {
		if (x0 <x[i])
		{
		    iclosest = i+1;
		    break;
		}
	    }

//	    System.out.println("xO: "+x0 +"\niclosest: " + x[iclosest]);
	    
	    //Calculate max and min data points to be included at x[n](i.e. x0)    
	    n_upper = (n + span) < data.length ? (n + span):data.length;
	    n_low = (n - span ) > 0 ? (n - span) : 0;
	    
	    //Trial scales are distance from x[n] to right and left hand edges of kernel respectively
	    //Set with of kernel to larger of the two
	    
	    trial_scale1 = x[n_upper -1] - x[iclosest];
	    trial_scale2 = x[iclosest] - x[n_low];
	    
	    //Set kernel scale appropriately
	    scale = trial_scale1 > trial_scale2 ? trial_scale1 :trial_scale2;
	    kernel.setScale( scale );
	    
	    
	    //Initialize sums
	    k = kx = kx2 = ky = kxy = 0.0;
	    
	    for( int i = n_low; i < n_upper; i++ )
	    {
		ker = kernel.value(x0, x[i]);
		k += ker;
		kx += ker * x[i];
		kx2 += ker * Math.pow( x[i], 2.0 );
		ky += ker * y[i];
		kxy += ker * x[i] * y[i];
	    }
	    k /= (double) (x.length);
	    kx /= (double) (x.length);
	    kx2 /= (double) (x.length);
	    ky /= (double) (x.length);
	    kxy /= (double) (x.length);
	    detA = (k * kx2) - Math.pow( kx, 2.0 );
	    alpha[n-1] = (kx2 * ky) - (kx * kxy);
	    alpha[n-1] /= detA;
	    
	    beta[n-1] = (k * kxy) - (kx * ky);
	    beta[n-1] /= detA;

//	    System.out.println( n + "\t\t" + alpha[n-1] + "\t\t" +beta[n-1] );
	    
	}
	
	//Make corrections to data
	double[][] corrected_channels = new  double[data.length][2];
	int igrid1;
	double correction;      //Temporary holding variable to store correction to be applied
	double  temp2;
	
	//Loop over data
	for( int i = 0; i < x.length; i++ )
	{
	    //Find closest grid point
	    //Geraint you will need to do a better interpolation of alpha and beta between 
	    //igrid1 and igrid1 + 1
	    
	    
	    
	    igrid1 = (int) Math.floor( (x[i] - xmin) / delta_x);
	    while( igrid1 >= alpha.length )
	    {
		int alpha1Closest = 1;
		int alpha2Closest = 1;
		for (int a=0; a<x.length;i++)
		{
		    if (alpha[igrid1] <x[a])
		    {
			alpha1Closest = a+1;
			break;
		    }
		}
		for (int a=0; a<x.length;a++)
		{
		    if (alpha[igrid1+1]<x[a])
		    {
			alpha2Closest = a+1;
			break;
		    }
		}
                
                //from the equation ((xspot-x1)alpha2 + (x2-xspot)alpha1) / x2-x1                
                //where xspot = x[i]; x1 = x[alpha1Closest]; x2  = x[alpha2Closest]; aplha1 @ x0 = alpha[igrid1]; alpha2 @ x0 + 1 = alpha[igrid1 + 1]
		alpha[igrid1] = (((x[i]-x[alpha1Closest])*(alpha[igrid1+1]))+((x[alpha2Closest] - x[i])*alpha[igrid1]))/(x[alpha2Closest] - x[alpha1Closest]); 
		
		
		igrid1--;
	    }
	    
	    
	    correction = -0.5 * ( alpha[igrid1] + (beta[igrid1]*x[i]) );
	    corrected_channels[intensity[i].b][0] =  data[intensity[i].b][0] + correction;
	    corrected_channels[intensity[i].b][1] =  data[intensity[i].b][1] -correction;
	}
	
	return corrected_channels;
    }
    */

    // ===========================================================================



}
