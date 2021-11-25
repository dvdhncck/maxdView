import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.*;

public class LSNormaliser implements Normaliser
{    

    public String getName() { return "Least Squares" ; }

    public boolean canHandleNaNs() { return false; }

    public int getMinimumNumMeasurements() { return 2; }
    public int getMaximumNumMeasurements() { return 0; }
    public int getNumberOfReturnedMeasurements( int number_of_measurements_given ) { return number_of_measurements_given; }
    public boolean isOneToOne() { return true; }

    public String getHelpDocumentName() { return "LSNormaliser"; }

    public JPanel  getUI(final maxdView mview, final NormaliserInput input )
    { 
	return makeUserInterface(mview); 
    }


    public String getInfo() 
    { 
	return "Least Squares Normalization.\n\nTotal Least Squares regression of logged data values from multiple Measurements.\n\nA linear transformation (location and scale) is applied to each Measurement.\n\nBased on research by Magnus Rattray, Norman Morrison, David Hoyle & Andy Brass\n"; 
    }

    public String getSettings() 
    { 
	return new String("Noise Threshold=" + noise_thresh_ls.getValue() ); 
    }

    public NormaliserOutput normalise( NormaliserInput input )
    {
        noise_thresh = noise_thresh_ls.getValue();


	//
	// Maximum Likelihood normalization of several microarray experiments.
	// Closed form solution to least squares and latent variable normalization models
	//


	//
	// based upon research notes provided by Magnus Rattray, Dept. Computer Science, The University of Manchester.
	//
	//        author: D. Hoyle 
	//        date: 20/2/2002
	// last modified: 05/03/2002
	//


	
       double[][] beta;
       int number_parameters = 0;
       int total_datapoints_present = 0;
       double ratio_parameters_to_datapoints = 0.0;
       int reference_experiment;


       //Copy data into array normalized_data and take logs

       // scale the result from the LabelSlider
       // (sliders operate on Ints, so everything is scaled by 1000)

       double[][] norm_data = new double[input.data.length][input.data[0].length];
       
       for( int i = 0; i < input.data.length; i++)
       {
         for( int j = 0; j < input.data[0].length; j++ )
         {
           norm_data[i][j] = input.data[i][j];
            
           // Apply Noise Thresholding
           if( norm_data[i][j] < noise_thresh )
           {
             norm_data[i][j] = noise_thresh;
           }

	   norm_data[i][j] = Math.log( norm_data[i][j] );
	 }
       }
           

       //Calculate covariance maxtrix
       double[] mean = new double[norm_data.length];
       double[][] covariance = new double[norm_data.length][norm_data.length];

       for( int i = 0; i < norm_data.length; i++ )
       {
	   mean[i] = 0.0;
	   for( int j = 0; j < norm_data[0].length; j++ )
	   {
	       mean[i] += norm_data[i][j];
	   }
	   mean[i] /= ((double) norm_data[0].length);
       }

       for( int i = 0; i < norm_data.length; i++ )
       {
	   for( int j = 0; j < norm_data.length; j++ )
	   {
	       covariance[i][j] = 0.0;
	       for( int k = 0; k < norm_data[0].length; k++ )
	       {
		   covariance[i][j] += (norm_data[i][k] - mean[i]) * (norm_data[j][k] - mean[j]);
	       }
	       covariance[i][j] /= ((double) norm_data[0].length);
           }
       }

       //Calculate SVD of covariance matrix and normalize data
       try
       {
         Svdcmp svd = new Svdcmp(covariance);
	 svd.orderEigenvalues();

	 // get V   (eigenvectors)

	 double[][] eigenvectors = svd.getV();
	 double[] a = new double[eigenvectors.length];
	 //Check orientation of vector a
	 int isum = 0;
	 for( int i = 0; i < eigenvectors.length; i++ )
	 {
	     a[i] = eigenvectors[i][0];
	     if( a[i] >= 0.0 )
	     {
		 isum++;
	     }
	     else
	     {
		 isum--;
	     }
	 }

	 //If necessary flip vector a so that majority of its components are +Ve
	 if( isum < 0 )
	 {
	     for( int i = 0; i < a.length; i++ )
	     {
		 a[i] = -a[i];
	     }
	 }

	 
         //Calculate normalized data
         for( int i = 0; i < norm_data.length; i++ )
         {
	     for( int j = 0; j < norm_data[0].length; j++ )
	     {
	         norm_data[i][j] -= mean[i];
	         norm_data[i][j] /= a[i];
	     }
         }

	 //Rescale normalized data so that average variance is 1
	 double sum_variances = 0.0;
	 double temp_mean = 0.0;
	 double temp_var = 0.0;

	 for( int i = 0; i < norm_data.length; i++ )
	 {
	     //First calculate mean and variance
	     temp_mean = temp_var = 0.0;
	     for( int j = 0; j < norm_data[0].length; j++ )
	     {
		 temp_mean += norm_data[i][j];
		 temp_var += Math.pow( norm_data[i][j], 2.0 );
	     }
	     temp_mean /= ((double) norm_data[0].length);
	     temp_var /= ((double) norm_data[0].length);
	     temp_var -= Math.pow( temp_mean, 2.0 );

	     sum_variances += temp_var;
	 }

	 double scale_factor = ((double) norm_data.length) / sum_variances;
	 scale_factor = Math.sqrt( scale_factor );

	 for( int i = 0; i < norm_data.length; i++ )
	 {
	     for( int j = 0; j < norm_data[0].length; j++ )
	     {
		 norm_data[i][j] *= scale_factor;
             }
	 }
       }   
       catch (IllegalArgumentException e) 
       {
	 System.out.println( e.toString() );
       } 
       catch (Exception e)
       {
	 System.out.println( e.toString() );
       }


       return new NormaliserOutput( norm_data, input.measurement_names );
    }

    double noise_thresh = 0.1;

    public void   saveProperties(maxdView mview) 
    {
	mview.putDoubleProperty("LSNormaliser.noise_threshold", noise_thresh_ls.getValue() );
    }

    // ===== plugin command ======================================================

    public PluginCommand getCommand() 
    {
	String[] args = 
	{
	    // name             // type            //default   // flags   // comment
	    "noise_threshold",  "double",          "1.0e-4",          "",      "minimum cutoff level",
	};
	return new PluginCommand( "LSNormalise", args );
    }
    public void parseArguments( final maxdView mview, final String args[] )
    {
	double nt = mview.getPluginDoubleArg( "noise_threshold", args, Double.NaN );
	if(!Double.isNaN( nt ))
	    noise_thresh_ls.setValue( nt );
     }

    // ===== user interface ======================================================

    private LabelSlider noise_thresh_ls;

    private maxdView mview = null;

    private JPanel makeUserInterface(final maxdView mview_)
    {
	mview = mview_;
	
	JPanel ui = new JPanel();
	GridBagLayout gridbag = new GridBagLayout();
	ui.setLayout(gridbag);

	JLabel label = new JLabel("Noise Threshold");
	GridBagConstraints c = new GridBagConstraints();
	c.weightx = 1.0;
	gridbag.setConstraints(label, c);
	ui.add(label);
	
	noise_thresh = mview.getDoubleProperty("LSNormaliser.noise_threshold", 1.0e-1);

	noise_thresh_ls = new LabelSlider( null, JSlider.VERTICAL, JSlider.HORIZONTAL, 0, 100, noise_thresh );
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.gridy = 1;
	gridbag.setConstraints(noise_thresh_ls, c);
	ui.add(noise_thresh_ls);
	

	return ui;
    }



}
