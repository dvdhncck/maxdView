import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.*;

public class GGMNormaliser implements Normaliser
{
    public String getName() { return "Global Geometric Mean" ; }

    public boolean canHandleNaNs() { return false; }

    public int getMinimumNumMeasurements() { return 1; }
    public int getMaximumNumMeasurements() { return 0; }
    public int getNumberOfReturnedMeasurements( int number_of_measurements_given ) { return number_of_measurements_given; }
    public boolean isOneToOne() { return true; }

    public String getHelpDocumentName() { return "GGMNormaliser"; }

    public JPanel  getUI(final maxdView mview, final NormaliserInput input )
    { 
	return makeUserInterface(mview, input); 
    }

    public String getInfo() 
    {  return "Normalise by global geometric mean.\n\nThis is a location and scale transformation normalization method. The data supplied is logged (natural logarithms) and transformed to have a mean of zero and a trimmed standard deviation of 1. The transformed logged data is returned\n"; }
 
    public String getSettings() 
    {  return new String("Noise Threshold =  " + noise_thresh_ls.getValue() ); }
    
    public void   saveProperties(maxdView mview) 
    { 
	mview.putDoubleProperty("GGMNormaliser.noise_threshold", noise_thresh_ls.getValue() );
	mview.putBooleanProperty("GGMNormaliser.show_stats", show_stats_jchkb.isSelected() );
    }

    public NormaliserOutput normalise( NormaliserInput input )
    {
	
	double[][] result = new double[input.data.length][];

	// scale the result from the LabelSlider
	// (sliders operate on Ints, so everything is scaled by 1000)

	noise_thresh = noise_thresh_ls.getValue();

	capture_stats = show_stats_jchkb.isSelected( );

	StringBuffer report = new StringBuffer();
       
	report.append("noise threshold = " + noise_thresh + "\n\n");

	for(int m=0; m < input.data.length; m++)
	{
	    double[] data = input.data[m];

	    boolean[] not_num = new boolean[ data.length ];
	    
	    double[] tmp  = new double[data.length];
	    double[] tmp1 = new double[data.length];
	    
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
		tmp1[g] = Math.log(tmp[g]);
	    }
	    
	    // Variable Declaration for Mean and Standard Deviation Calculations
	    double mean = .0;
	    double std = .0;
	    int count = 0;
	
	    // Calculate Mean
	    for (int g=0; g < data.length; g++)
	    {
		if (!not_num[g])
		{
		    mean += tmp1[g];
		    count++;
		}
	    }
	    mean /= (double) count;
	    
	    // Calculate Standard Deviation
	    for (int g=0; g < data.length; g++)
	    {
		if (!not_num[g])
		{
		    std += Math.pow((tmp1[g] - mean), 2);
		} 
	    }
	    std = Math.sqrt(std /= (double) (count-1));
	    
	    // Flag Data Outside +/- 3sd's of the Mean
	    double bounds[] = new double[2];
	    bounds[0] = mean - (3 * std);
	    bounds[1] = mean + (3 * std);
	    
	    for (int g=0; g < data.length; g++)
	    {
		if (tmp1[g] <= bounds[0] || tmp1[g] >= bounds[1])
		{
		    not_num[g] = true;
		}
	    }
	    
	    // Reset Variables
	    mean = .0;
	    std = .0;
	    count = 0;
	    
	    // Calculate Mean
	    for (int g=0; g < data.length; g++)
	    {
		if (!not_num[g])
		{
		    mean += tmp1[g];
		    count++;
		}
	    }
	    mean /= (double) count;
	    
	    // Calculate Standard Deviation
	    for (int g=0; g < data.length; g++)
	    {
		if (!not_num[g])
		{
		    std += Math.pow((tmp1[g] - mean), 2);
		} 
	    }
	    std = Math.sqrt(std /= (double) (count-1));
	    
	    for (int g=0; g < data.length; g++)
	    {
		tmp1[g] = (tmp1[g] - mean) / std; 
	    }
	    
	    if(capture_stats)
	    {
		report.append("  [ " + input.measurement_names[m] + "]\n");
		report.append("  mean  = " + mean + "\n");
		report.append("  std = " + std + "\n");
		report.append("  count = " + count + "\n");
		report.append("  upper bound = " + bounds[0] + "\n");
		report.append("  lower bound = " + bounds[1] + "\n\n");
	    }

	    result[m] = tmp1;
	}

	if(capture_stats && (mview != null))
	    mview.infoMessage( report.toString() );

	
	return new NormaliserOutput( result, input.measurement_names );
    }

    boolean capture_stats = true;

    double noise_thresh = 0.1;

    // ===== plugin command ======================================================

    public PluginCommand getCommand() 
    {
	String[] args = 
	{
	    // name             // type            //default   // flags   // comment
	    "noise_threshold",  "double",          "",          "",      "minimum cutoff level",
	    "show_statistics",  "boolean",         "false",     "",      "display statistical information"
	};
	return new PluginCommand( "GGMNormalise", args );
    }
    public void parseArguments( final maxdView mview, final String args[] )
    {
	double nt = mview.getPluginDoubleArg( "noise_threshold", args, Double.NaN );
	if(!Double.isNaN( nt ))
	    noise_thresh_ls.setValue( nt );

	boolean ss = mview.getPluginBooleanArg( "show_statistics", args, false );
	show_stats_jchkb.setSelected( ss );
    }

    // ===== user interface ======================================================

    private LabelSlider noise_thresh_ls;
    private JCheckBox   show_stats_jchkb;

    private maxdView mview = null;

    private JPanel makeUserInterface( final maxdView mview_, final NormaliserInput input )
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
	
	noise_thresh = mview.getDoubleProperty("GGMNormaliser.noise_threshold", 1.0);

	noise_thresh_ls = new LabelSlider( null, JSlider.VERTICAL, JSlider.HORIZONTAL, 0, max, noise_thresh );
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.gridy = 1;
	gridbag.setConstraints(noise_thresh_ls, c);
	ui.add(noise_thresh_ls);
	
	capture_stats = mview.getBooleanProperty("LSNormaliser.show_stats", false);

	show_stats_jchkb = new JCheckBox("Show statistics");
	show_stats_jchkb.setSelected( capture_stats );
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.gridy = 2;
	gridbag.setConstraints(show_stats_jchkb, c);
	ui.add(show_stats_jchkb);

	return ui;
    }
}
