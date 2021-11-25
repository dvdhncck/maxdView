import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.*;

public class SampleNormaliser implements Normaliser
{
    public String getName() { return "Sample" ; }

    //
    // the result of this method defines whether this 
    // Normaliser can handle data containing NaN values
    //
    // if false, the Normalise plugin will not allow data
    // with NaN values to be used with this Normaliser
    //
    public boolean canHandleNaNs() { return false; }

    
    //
    // should return the minimum number of Measurements that
    // this Normaliser needs to operate correctly
    //
    public int getMinimumNumMeasurements() { return 1; }
    public int getMaximumNumMeasurements() { return 0; }
    public int getNumberOfReturnedMeasurements( int number_of_measurements_given ) { return number_of_measurements_given; }
    public boolean isOneToOne() { return true; }
    //
    public String getHelpDocumentName() { return "SampleNormaliser"; }

    // this method should return a JPanel containing the user interface 
    // components that this Normaliser needs.
    //
    // the maxdView object is passed so that the application properties
    // can be queried for saved parameters (see below)
    //
    // the data is passed in case the Normaliser wants to adjust any
    // of it's parameter values to reflect the data
    //
    public JPanel  getUI(final maxdView mview, final NormaliserInput input )
    { return makeUserInterface(mview); }

    //
    // this method should return a String describing how the
    // Normaliser operates.
    //
    public String getInfo() 
    { 
	return "This is an example implementation of Normaliser " +
	       "that doesn't actually do anything\n\n" + 
	       "You can use this code as a template for your " +
	       "own algorithms."; 
    }
    
    //
    // !! NEW METHOD !!
    //
    // this method should return a String describing the current
    // settings of the Normaliser
    //
    // this string will be saved as an attribute in any Measurements that are created
    // 
    public String getSettings() 
    {  
	return new String("Param 1 =  " + slider_1.getValue() + 
			  " Param 2 = " + slider_2.getValue() + 
			  " Param 3 = " + (checkbox_1.isSelected() ? "yes" : "no") ); 
    }


    //
    // this method will be called when the Normalise plugin thinks that this
    // Normaliser should save its properties.
    //
    // this method should save any properties that you want to be able to
    // retrieve next time.
    //
    // (you don't have to do anything in this method if you don't want to)
    //
    public void   saveProperties(final maxdView mview) 
    { 
	mview.putDoubleProperty(  "SampleNormaliser.parameter_1", slider_1.getValue() );
	mview.putDoubleProperty(  "SampleNormaliser.parameter_2", slider_2.getValue() );
	mview.putBooleanProperty( "SampleNormaliser.parameter_3", checkbox_1.isSelected() );
    }

    //
    // this method contains the actual normalisation code
    //
    // the data is organised by Measurements:
    //
    //    data[i][j] is the expression in Measurement 'i' of Spot 'j'
    // 
    //    the Measurements are those that were selected in the plugin's Measurement list
    //
    //    the Spots have already been filtered (if any filter was applied).
    //
    //    this method should return a 2d array of doubles with the same dimensions
    //    as the input array
    //
    //    the Measurement names are provided in case you wish to report
    //    any extra information to the user (e.g. statistics about each Measurement)
    //
    public NormaliserOutput normalise( NormaliserInput input )
    {
	System.out.println( "There are " + input.data.length + " Measurements and " + input.data[0].length + " Spots");

	System.out.println( "p1 = " + slider_1.getValue() );
	System.out.println( "p2 = " + slider_2.getValue() );
	System.out.println( "p3 = " + (checkbox_1.isSelected() ? "yes" : "no") );

	double[][] result = new double[input.data.length][input.data[0].length];

	// for example, normalise all values to the range 0...1
	
	double max = -Double.MAX_VALUE;
	double min = Double.MAX_VALUE;
	
	for(int i=0; i < input.data.length; i++)
	    for(int j=0; j < input.data[i].length; j++)
	    {
		if(input.data[i][j] > max)
		    max = input.data[i][j];
		if(input.data[i][j] < min)
		    min = input.data[i][j];
	    }

	double scale = (min == max) ? 1.0 : (1.0 / (max-min));
	
	for(int i=0; i < input.data.length; i++)
	    for(int j=0; j < input.data[i].length; j++)
		result[i][j] = (input.data[i][j] - min) * scale;

	return new NormaliserOutput( result, input.measurement_names );
    }
    
    // ===== plugin command ======================================================

    public PluginCommand getCommand()
    {
	String[] args = 
	{
	    // name             // type            //default   // flags   // comment
	    "param1",          "double",          "",          "",       "parameter 1 does nothing", 
	    "param2",          "double",          "",          "",       "parameter 2 does nothing as well", 
	    "param3",          "boolean",         "false" ,    "",       "this is parameter 3, which does nothing"
	};
	return new PluginCommand( "SampleNormalise", args );
    }

    public void parseArguments( final maxdView mview, final String args[] )
    {
	double p1_val = mview.getPluginDoubleArg( "param1", args, Double.NaN );
	if(!Double.isNaN( p1_val ))
	    slider_1.setValue( p1_val );

	double p2_val = mview.getPluginDoubleArg( "param2", args, Double.NaN );
	if(!Double.isNaN( p2_val ))
	    slider_2.setValue( p2_val );
	
	boolean p3_val = mview.getPluginBooleanArg( "param3", args, false );
	
	checkbox_1.setSelected( p3_val );
    }
    
    // ===== user interface ======================================================

    private LabelSlider slider_1, slider_2;
    private JCheckBox checkbox_1;

    private JPanel makeUserInterface(final maxdView mview)
    {
	JPanel ui = new JPanel();
	GridBagLayout gridbag = new GridBagLayout();
	ui.setLayout(gridbag);

	//
	//  the first paramter
	//
	//  made of a LabelSlider which provides a label and an edit field
	//

	// get the previous value from the application properties
	double old_param_1_value = mview.getDoubleProperty("SampleNormaliser.parameter_1", 1.0);
	slider_1 = new LabelSlider( "Param1", JSlider.VERTICAL, JSlider.HORIZONTAL, 0., 1., old_param_1_value);
	GridBagConstraints c = new GridBagConstraints();
	c.gridy = 0;
	c.weightx = 10.0;
	c.gridwidth = 2;
	c.weighty = 1.0;
	gridbag.setConstraints(slider_1, c);
	ui.add(slider_1);


	//
	// the second parameter is made in exactly the same way as the first
	//

	// get the previous value from the application properties
	double old_param_2_value = mview.getDoubleProperty("SampleNormaliser.parameter_2", 1.0);
	slider_2 = new LabelSlider( "Param2", JSlider.VERTICAL, JSlider.HORIZONTAL, 0., 1., old_param_2_value);
	c = new GridBagConstraints();
	c.gridy = 1;
	c.weightx = 10.0;
	c.gridwidth = 2;
	c.weighty = 1.0;
	gridbag.setConstraints(slider_2, c);
	ui.add(slider_2);

	//
	// the third parameter is a JCheckBox
	//

	JLabel label = new JLabel("Parameter 3");
	c = new GridBagConstraints();
	c.gridy = 2;
	c.weightx = 2.0;
	c.weighty = 1.0;
	c.anchor = GridBagConstraints.EAST;
	gridbag.setConstraints(label, c);
	ui.add(label);

	checkbox_1 = new JCheckBox("Optional");
	// get the previous value from the application properties
	boolean old_param_3_value = mview.getBooleanProperty("SampleNormaliser.parameter_3", true);
	checkbox_1.setSelected(old_param_3_value);
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 2;
	c.weightx = 8.0;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(checkbox_1, c);
	ui.add(checkbox_1);
	

	return ui;
    }
}
