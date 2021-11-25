import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.*;

public class CenteringNormaliser implements Normaliser
{
    public String getName() { return "Centering" ; }

    public boolean canHandleNaNs() { return false; }

    public int getMinimumNumMeasurements() { return 1; }
    public int getMaximumNumMeasurements() { return 0; }
    public int getNumberOfReturnedMeasurements( int number_of_measurements_given ) { return number_of_measurements_given; }
    public boolean isOneToOne() { return true; }

    public String getHelpDocumentName() { return "CenteringNormaliser"; }

    public JPanel  getUI(final maxdView mview, final NormaliserInput input )
    { 
	return makeUserInterface(mview, input); 
    }

    public String getInfo() 
    {  return "Mean or median centering.\n\nNormalize the data by setting mean/median of the logged data to zero.\n\nWhen applied to ratios of intensities from microarray data this corresponds to an assumption of no overall differential expression between the two mRNA populations\n"; }
 
    public String getSettings() 
    {  return new String(
	(median_center ? "Centering=Median"  : "Centering=Mean" )+
	" Noise Threshold=" + noise_thresh_ls.getValue()); }
    
    public void   saveProperties(maxdView mview) 
    { 
	mview.putDoubleProperty("CenteringNormaliser.noise_threshold", noise_thresh_ls.getValue() );
	mview.putBooleanProperty("CenteringNormaliser.median_center", median_center);
	mview.putBooleanProperty("CenteringNormaliser.show_stats", capture_stats);

    }

    public NormaliserOutput normalise( NormaliserInput input )
    {
	
	double[][] result = new double[input.data.length][];

	// scale the result from the LabelSlider
	// (sliders operate on Ints, so everything is scaled by 1000)

	noise_thresh = noise_thresh_ls.getValue();

	capture_stats = show_stats_jchkb.isSelected( );

	median_center  = use_median_jrb.isSelected();

	StringBuffer report = new StringBuffer();
       
	report.append("noise threshold = " + noise_thresh + "\n\n");

	for(int m=0; m < input.data.length; m++)
	{
	    double[] data = input.data[m];

	    boolean[] not_num = new boolean[ data.length ];
	    
	    double[] tmp  = new double[data.length];
	    
	    // Apply Noise Thresholding
	    
	    for(int g=0; g < data.length; g++)
	    {
		tmp[g] = data[g];
		if (data[g] < noise_thresh)
		{
		    tmp[g] = noise_thresh;
		    not_num[g] = true;
		}
	    }
	    
	    // Log Data (could do this in above loop, but keep seperate for now
	    // as may need to add a log data option)
	    
	    for(int g=0; g < data.length; g++)
	    {
		if( !Double.isNaN(tmp[g] ) )
		    tmp[g] = Math.log(tmp[g]);
	    }
	    
	    // Variable Declaration for Mean and Standard Deviation Calculations
	    double center = .0;
	    int count = 0;
	
	    if( median_center )
	    {
		// find the median value in array 'tmp' disregarding any NaN values

		// make a copy of the array and sort it...
		double[] tmp_tmp = new double[ tmp.length ];
		for( int tt=0; tt < tmp.length; tt++)
		    tmp_tmp[ tt ] = tmp[ tt ];

		java.util.Arrays.sort( tmp_tmp );

		// how many non-NaNs are there in the array ?

		int non_nans = 0;
		for(int g=0; g < data.length; g++)
		    if( !Double.isNaN( tmp_tmp[ g ] ) )
			non_nans++;
		
		// find the array index of the N/2 th value

		int middle_non_nan = non_nans / 2;

		for(int g=0; g < data.length; g++)
		    if( !Double.isNaN( tmp_tmp[ g ] ) )
			if(--middle_non_nan == 0)
			    center = tmp_tmp[g];

		count += non_nans;
	    }
	    else
	    {
		// Calculate Mean
		double mean = 0.0;
		for (int g=0; g < data.length; g++)
		{
		    if (!not_num[g])
		    {
			mean += tmp[g];
			count++;
		    }
		}
		mean /= (double) count;
		center = mean;
	    }

	    
	    for (int g=0; g < data.length; g++)
	    {
		tmp[g] = (tmp[g] - center); 
	    }
	    
	    if(capture_stats)
	    {
		report.append("  [ " + input.measurement_names[m] + "]\n");
		report.append("  center  = " + center + "\n");
		report.append("  count = " + count + "\n");
	    }
	    
	    result[m] = tmp;
	}

	if(capture_stats && (mview != null))
	    mview.infoMessage( report.toString() );

       return new NormaliserOutput( result, input.measurement_names );
       
    }

    boolean capture_stats = true;

    double noise_thresh = 0.1;

    boolean median_center = true;

    // ===== plugin command ======================================================

    public PluginCommand getCommand() 
    {
	String[] args = 
	{
	    // name             // type            //default   // flags   // comment
	    "noise_threshold",  "double",          "",          "",      "minimum cutoff level",
	    "show_statistics",  "boolean",         "false",     "",      "display statistical information",
	    "median_center",    "boolean",         "true",      "",      "use the median centering algorithm",
	    "mean_center",      "boolean",         "false",     "",      "use the mean centering algorithm",
	};
	return new PluginCommand( "CenterNormalise", args );
    }

    public void parseArguments( final maxdView mview, final String args[] )
    {
	double nt = mview.getPluginDoubleArg( "noise_threshold", args, Double.NaN );
	if(!Double.isNaN( nt ))
	    noise_thresh_ls.setValue( nt );

	boolean mea = mview.getPluginBooleanArg( "mean_center", args, true );
	median_center = !mea;

	boolean med = mview.getPluginBooleanArg( "median_center", args, true );
	median_center = med;

	boolean ss = mview.getPluginBooleanArg( "show_statistics", args, false );
	show_stats_jchkb.setSelected( ss );
    }

    // ===== user interface ======================================================

    private LabelSlider noise_thresh_ls;
    private JCheckBox   show_stats_jchkb;

    private JRadioButton use_median_jrb, use_mean_jrb;

    private maxdView mview = null;

    private JPanel makeUserInterface(final maxdView mview_, final NormaliserInput input )
    {
	mview = mview_;
	
	double max = -Double.MAX_VALUE;
	for(int m=0; m < input.data.length; m++)
	{
	    final int ns = input.data[m].length;
	    for(int s=0; s < ns; s++)
	    {
		final double d = input.data[m][s];
		if(!Double.isNaN(d))
		{
		    if(d > max)
			max = d;
		}
	    }
	}

	JPanel ui = new JPanel();
	GridBagLayout gridbag = new GridBagLayout();
	ui.setLayout(gridbag);

	JLabel label = new JLabel("Noise Threshold");
	GridBagConstraints c = new GridBagConstraints();
	c.weightx = 1.0;
	gridbag.setConstraints(label, c);
	ui.add(label);
	
	
	noise_thresh = mview.getDoubleProperty("CenteringNormaliser.noise_threshold", 1.0);
	median_center = mview.getBooleanProperty("CenteringNormaliser.median_center", true);
	capture_stats = mview.getBooleanProperty("CenteringNormaliser.show_stats", false);

	
	noise_thresh_ls = new LabelSlider( null, JSlider.VERTICAL, JSlider.HORIZONTAL, 0, max, noise_thresh );
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.gridy = 1;
	//c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(noise_thresh_ls, c);
	ui.add(noise_thresh_ls);
	

	use_median_jrb = new JRadioButton("Median center the logged data");

	use_median_jrb.setSelected( median_center );
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.gridy = 2;
	//c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(use_median_jrb, c);
	ui.add(use_median_jrb);


	use_mean_jrb = new JRadioButton("Mean center the logged data");

	use_mean_jrb .setSelected( !median_center );
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.gridy = 3;
	//c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(use_mean_jrb , c);
	ui.add(use_mean_jrb );


	ButtonGroup bg = new ButtonGroup();
	bg.add( use_median_jrb );
	bg.add( use_mean_jrb );


	show_stats_jchkb = new JCheckBox("Show statistics");
	show_stats_jchkb.setSelected( capture_stats );
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.gridy = 4;
	//c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(show_stats_jchkb, c);
	ui.add(show_stats_jchkb);


	return ui;
    }
}
