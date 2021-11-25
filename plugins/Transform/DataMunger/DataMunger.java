import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.event.KeyEvent;
import java.lang.reflect.*;
import java.io.*;

public class DataMunger implements ExprData.ExprDataObserver, Plugin
{
    public DataMunger(maxdView mview_)
    {
	mview = mview_;
	edata = mview.getExprData();

    }

    public void cleanUp()
    {
	edata.removeObserver(this);

	if(frame != null)
	{
	    //Dimension dim = frame.getContentPane().getSize();

	    //mview.putIntProperty("DataMunger.panel_width", (int) dim.getWidth());
	    //mview.putIntProperty("DataMunger.panel_height", (int) dim.getHeight());
	    
	    savePrefs();
	
	    frame.setVisible(false);
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- -- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  plugin implementation
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void startPlugin()
    {
	addComponents();

	loadPrefs();

	edata.addObserver(this);

	frame.setVisible(true);

	// loadRule( new File(mview.getConfigDirectory() + "simple-maths.rule"), true );
    }

    public void stopPlugin()
    {
	cleanUp();
    }
  
    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("Data Munger", "transform", 
					 "Create a new Measurement or Spot Attribute based on an existing one", "",
					 1, 0, 0);
	return pinf;
    }

    public PluginCommand[] getPluginCommands()
    {
	/*
	PluginCommand[] com = new PluginCommand[3];
	
	com[0] = new PluginCommand("start", null);
	com[1] = new PluginCommand("stop", null);
	
	String[] args = new String[] 
	{
	    // name         // type     //default   // flag   // comment
	    "expr",         "string",   "",         "m",      "the expression to execute",
	    "apply_filter", "boolean",  "false",    "",       "", 
	    "new_name",     "string",   "",         "m",      "name for new Measurement" 
	};
	
	//com[2] = new PluginCommand("fish", null);
	
	com[2] = new PluginCommand("execute", args);

	return com;
	*/

	return new PluginCommand[0];
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
	
/*
	if(name.equals("execute"))
	{
	    String expr  = mview.getPluginStringArg( "expr", args, null );
	    String nname = mview.getPluginStringArg( "new_name", args, null );
	    boolean af   = mview.getPluginBooleanArg( "apply_filter", args, false );
	    
	    if(expr == null)
	    {
		mview.alertMessage("No expression specified");
	    }
	    else
	    {
		// System.out.println("running '" + expr + "'");
		
		try
		{
		    setupMethods();
		    
		    buildList();
		    
		    execute( expr, af, nname );
		}
		catch( Parser.ParseError pe )
		{
		    mview.alertMessage("Parse error: " + pe.toString() );
		}
	    }
	}
*/

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
	    populateList();
	    break;
	}
    }

    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  


    private class MeasSpotAttr 
    {
	public int meas_id;
	public int spot_attr_id;

	public MeasSpotAttr(int m, int a) 
	{
	    meas_id = m; spot_attr_id = a;
	}
	
    }
    

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  


    private void addFiller( JPanel panel, GridBagLayout bag, int row, int col, int size )
    {
	Dimension fillsize = new Dimension( size, size);
	Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
	GridBagConstraints c = new GridBagConstraints();
	c.gridy = row;
	c.gridx = col;
	bag.setConstraints(filler, c);
	panel.add(filler);
    }


    private void addComponents()
    {
	frame = new JFrame("Data Munger");
	
	mview.decorateFrame( frame );

	frame.addWindowListener(new WindowAdapter() 
			  {
			      public void windowClosing(WindowEvent e)
			      {
				  cleanUp();
			      }
			  });

	GridBagLayout gridbag = new GridBagLayout();
	JPanel panel = new JPanel();
	panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	panel.setLayout(gridbag);

	//panel.setPreferredSize(new Dimension(350, 200));

	GridBagConstraints c;
	JLabel label;

	int line = 0;
	
	// -------------------------------------------------------------------------------------

	{
	    GridBagLayout w_gridbag = new GridBagLayout();
	    JPanel w_panel = new JPanel();
	    w_panel.setBorder(BorderFactory.createEmptyBorder(24,0,24,0));
	    w_panel.setLayout(w_gridbag);
	    w_panel.setBorder(BorderFactory.createTitledBorder( BorderFactory.createLineBorder(Color.black),
								" Select the source 'Measurement' or 'Spot Attribute' "  ) );

	    int w_line = 0;

	    meas_and_attr_jcb = new JComboBox();
	    c = new GridBagConstraints();
	    c.gridy = w_line++;
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    c.weightx = 10.0;
	    w_gridbag.setConstraints( meas_and_attr_jcb, c );
	    w_panel.add( meas_and_attr_jcb );
	    
	    populateList();

	    addFiller( w_panel, w_gridbag, w_line++, 0, 8 );

	    label = new JLabel( "  Selecting a 'Measurement' will result a new 'Measurement' being generated. " );
	    label.setFont( mview.getSmallFont() );
	    c = new GridBagConstraints();
	    c.gridy = w_line++;
	    c.anchor = GridBagConstraints.WEST;
	    //c.weightx = 10.0;
	    w_gridbag.setConstraints( label, c);
	    w_panel.add( label );

	    addFiller( w_panel, w_gridbag, w_line++, 0, 8 );

	    label = new JLabel( "  Selecting a 'Spot Attribute' will result a new 'Spot Attribute' being added " );
	    label.setFont( mview.getSmallFont() );
	    c = new GridBagConstraints();
	    c.gridy = w_line++;
	    c.anchor = GridBagConstraints.WEST;
	    //c.weightx = 10.0;
	    w_gridbag.setConstraints( label, c);
	    w_panel.add( label );

	    label = new JLabel( "  to the same 'Measurement' as the source 'Spot Attribute'. " );
	    label.setFont( mview.getSmallFont() );
	    c = new GridBagConstraints();
	    c.gridy = w_line++;
	    c.anchor = GridBagConstraints.WEST;
	    //c.weightx = 10.0;
	    w_gridbag.setConstraints( label, c);
	    w_panel.add( label );

	    addFiller( w_panel, w_gridbag, w_line++, 0, 16 );

	    // ----------------------

	    c = new GridBagConstraints();
	    c.gridy = line++;
	    c.weightx = 10.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints( w_panel, c);
	    panel.add( w_panel );

	    addFiller( panel, gridbag, line++, 0, 12 );

	}

	{
	    GridBagLayout w_gridbag = new GridBagLayout();
	    JPanel w_panel = new JPanel();
	    w_panel.setLayout(w_gridbag);
	    w_panel.setBorder(BorderFactory.createTitledBorder( BorderFactory.createLineBorder(Color.black),
								" Choose which Spots to affect "  ) );

	    int w_line = 0;

	    mung_filtered_jrb = new JRadioButton( "Change the value in all filtered Spots" );
	    c = new GridBagConstraints();
	    c.gridy = w_line++;
	    c.gridwidth = 2;
	    //c.weightx = 10.0;
	    c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints( mung_filtered_jrb, c);
	    w_panel.add( mung_filtered_jrb );
	    
	    label = new JLabel( "   This option causes the value in Spots which are hidden by the current filter(s)" );
	    label.setFont( mview.getSmallFont() );
	    c = new GridBagConstraints();
	    c.gridy = w_line++;
	    c.gridwidth = 2;
	    c.anchor = GridBagConstraints.WEST;
	    //c.weightx = 10.0;
	    w_gridbag.setConstraints( label, c);
	    w_panel.add( label );

	    label = new JLabel( "   to be changed, but leaves the value in the other Spots unchanged." );
	    label.setFont( mview.getSmallFont() );
	    c = new GridBagConstraints();
	    c.gridy = w_line++;
	    c.gridwidth = 2;
	    c.anchor = GridBagConstraints.WEST;
	    //c.weightx = 10.0;
	    w_gridbag.setConstraints( label, c);
	    w_panel.add( label );

	    addFiller( w_panel, w_gridbag, w_line++, 0, 8 );

	    mung_unfiltered_jrb = new JRadioButton( "Change the value in all unfiltered Spots" );
	    c = new GridBagConstraints();
	    c.gridy = w_line++;
	    c.gridwidth = 2;
	    //c.weightx = 10.0;
	    c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints( mung_unfiltered_jrb, c);
	    w_panel.add( mung_unfiltered_jrb );
	    
	    label = new JLabel( "   This option causes the value in Spots which are still visible using the current filter(s)" );
	    label.setFont( mview.getSmallFont() );
	    c = new GridBagConstraints();
	    c.gridy = w_line++;
	    c.gridwidth = 2;
	    c.anchor = GridBagConstraints.WEST;
	    //c.weightx = 10.0;
	    w_gridbag.setConstraints( label, c);
	    w_panel.add( label );

	    label = new JLabel( "   to be changed, but leaves the value in other Spots unchanged." );
	    label.setFont( mview.getSmallFont() );
	    c = new GridBagConstraints();
	    c.gridy = w_line++;
	    c.gridwidth = 2;
	    c.anchor = GridBagConstraints.WEST;
	    //c.weightx = 10.0;
	    w_gridbag.setConstraints( label, c);
	    w_panel.add( label );

	    addFiller( w_panel, w_gridbag, w_line++, 0, 16 );

	    // ----------------------

	    c = new GridBagConstraints();
	    c.gridy = line++;
	    //c.weightx = 10.0;
	    c.weightx = 10.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints( w_panel, c);
	    panel.add( w_panel );

	    addFiller( panel, gridbag, line++, 0, 12 );

	}

	{
	    GridBagLayout w_gridbag = new GridBagLayout();
	    JPanel w_panel = new JPanel();
	    w_panel.setLayout(w_gridbag);
	    w_panel.setBorder(BorderFactory.createTitledBorder( BorderFactory.createLineBorder(Color.black),
								" Specify how the values are to be changed "  ) );

	    int w_line = 0;

	    set_to_nan_jrb = new JRadioButton( "Change values to 'NaN'" );
	    c = new GridBagConstraints();
	    c.gridy = w_line++;
	    c.gridwidth = 2;
	    c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints( set_to_nan_jrb, c);
	    w_panel.add( set_to_nan_jrb );
	    
	    addFiller( w_panel, w_gridbag, w_line++, 0, 8 );

	    set_to_other_jrb = new JRadioButton( "Change values to" );
	    c = new GridBagConstraints();
	    c.gridy = w_line;
	    c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints( set_to_other_jrb, c);
	    w_panel.add( set_to_other_jrb );
	    
	    other_value_jtf = new JTextField( 10 );
	    c = new GridBagConstraints();
	    c.gridy = w_line++;
	    c.gridx = 1;
	    c.weightx = 3.0;
	    c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints( other_value_jtf, c);
	    w_panel.add( other_value_jtf );
	    	    
	    addFiller( w_panel, w_gridbag, w_line++, 0, 16 );

	    ButtonGroup bg = new ButtonGroup();
	    bg.add( mung_filtered_jrb );
	    bg.add( mung_unfiltered_jrb );

	    bg = new ButtonGroup();
	    bg.add( set_to_nan_jrb );
	    bg.add( set_to_other_jrb );

	    // ----------------------

	    c = new GridBagConstraints();
	    c.gridy = line++;
	    c.weightx = 10.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints( w_panel, c);
	    panel.add( w_panel );

	    addFiller( panel, gridbag, line++, 0, 12 );

	}

	// -------------------------------------------------------------------------------------

	{
	    JPanel w_panel = new JPanel();
	    GridBagLayout w_gridbag = new GridBagLayout();
	    w_panel.setLayout(w_gridbag);

	    w_panel.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
	    
	    int b_col = 0;
	    
	    
	    execute_jb = new JButton("Execute");
	    //execute_jb.setEnabled(false);
	    execute_jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			execute();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = b_col++;
	    w_gridbag.setConstraints(execute_jb, c);
	    w_panel.add(execute_jb);

		
	    addFiller( w_panel, w_gridbag, 0, b_col++, 8 );
	    addFiller( w_panel, w_gridbag, 0, b_col++, 8 );
	    addFiller( w_panel, w_gridbag, 0, b_col++, 8 );
	    addFiller( w_panel, w_gridbag, 0, b_col++, 8 );
	    
	    
	    JButton button = new JButton("Help");
	    button.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			mview.getPluginHelpTopic("DataMunger", "DataMunger");
		    }
		    });
	    c = new GridBagConstraints();
	    c.gridx = b_col++;
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    //c.weightx = 3.0;
	    w_gridbag.setConstraints(button, c);
	    w_panel.add(button);

		
	    addFiller( w_panel, w_gridbag, 0, b_col++, 8 );
	    
	    
	    button = new JButton("Close");
	    button.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			cleanUp();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = b_col++;
	    w_gridbag.setConstraints( button, c );
	    w_panel.add(button);

	    
	    c = new GridBagConstraints();
	    c.gridy = line;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.weightx = 10.0;
	    gridbag.setConstraints( w_panel, c );
	    panel.add( w_panel );
	}

	// -------------------------------------------------------------------------------------


	//int iw = mview.getIntProperty("DataMunger.panel_width", 400);
	//int ih = mview.getIntProperty("DataMunger.panel_height", 450);
	//
	//panel.setPreferredSize(new Dimension( iw, ih ));
	
	frame.getContentPane().add(panel);

	frame.pack();

    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private void populateList()
    {
	Vector name_str_v = new Vector();
	Vector meas_spot_attr_v = new Vector();

	
	for(int mi=0; mi < edata.getNumMeasurements(); mi++)
	{
	    int m = edata.getMeasurementAtIndex(mi);

	    ExprData.Measurement ms = edata.getMeasurement(m);
	    
	    name_str_v.add( ms.getName() );
	    meas_spot_attr_v.add( new MeasSpotAttr( m, -1 ) );

	    for(int sa=0; sa < ms.getNumSpotAttributes(); sa++)
	    {
		int tc = ms.getSpotAttributeDataTypeCode(sa);
		
		if((tc == 0) || (tc == 1))
		{
		    String full_name = ms.getName() + "." + ms.getSpotAttributeName(sa);
		    
		    name_str_v.add( "  " + full_name );
		    meas_spot_attr_v.add( new MeasSpotAttr( m, sa ) );
		}
	    }
	}


	if( meas_and_attr_jcb != null)
	{
	    meas_and_attr_jcb.setModel( new DefaultComboBoxModel( name_str_v ) );
	    meas_and_attr_jcb.setSelectedIndex( -1 );
	}
 
	meas_spot_attr_a = (MeasSpotAttr[])  meas_spot_attr_v.toArray( new MeasSpotAttr[ meas_spot_attr_v.size() ] );
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  


    private void execute( )
    {
	try
	{
	    if( edata.getEnabledFilterCount() == 0 )
	    {
		if( mview.errorQuestion("No filters are active.\n\n" + 
					"Either all of the Spots, or none of them, will be affected.\n\n" + 
					"Continue ?", "Yes", "No" ) == 1 )
		    return;
	    }

	    int sel = meas_and_attr_jcb.getSelectedIndex();

	    if( sel == -1 )
	    {
		mview.alertMessage("Please select either a Measurement or a SpotAttribute to base the new values on"); // 
		return;
	    }

	    if( sel >= meas_spot_attr_a.length )
	    {
		mview.alertMessage("No existing data available to modify.");
		return;
	    }

	    if( ( ! mung_unfiltered_jrb.isSelected() ) && ( ! mung_filtered_jrb.isSelected() ) )
	    {
		mview.alertMessage("Please choose which Spots to affect, either the filtered or unfiltered ones.");
		return;
	    }

	    if( ( ! set_to_nan_jrb.isSelected() ) && ( ! set_to_other_jrb.isSelected() ) )
	    {
		mview.alertMessage("Please specify whether to change values to 'NaN' or to some other value");
		return;
	    }

	    if( ( set_to_other_jrb.isSelected() ) && 
		( ( other_value_jtf.getText() == null ) || ( other_value_jtf.getText().length() == 0 ) ) )
	    {
		mview.alertMessage("Please provide a value to change the values to");
		return;
	    }

	    final MeasSpotAttr msa = meas_spot_attr_a[ sel ];

	    final ExprData.Measurement ms = edata.getMeasurement( msa.meas_id );
	    
	    final int n_spots = edata.getNumSpots();
	    
	    double[] data = new double[ n_spots ];
	    
	    double[] source_data = null;

	    if( msa.spot_attr_id == -1 )
	    {
		System.out.println(" Source is Measurement '" + ms.getName() + "'");

		source_data = ms.getData();
	    }
	    else
	    {
		System.out.println(" Source is Attribute '" +  ms.getSpotAttributeName( msa.spot_attr_id ) + "' of Measurement '" + ms.getName() + "'");

		Object spot_attr_data = ms.getSpotAttributeData( msa.spot_attr_id );
		    
		int dt = ms.getSpotAttributeDataTypeCode( msa.spot_attr_id );
		
		if( dt == 0 )
		{
		    // convert integer data to doubles....

		    int[] int_data = (int[]) spot_attr_data;

		    source_data = new double[ n_spots ];

		    for(int s=0; s < n_spots; s++)
			source_data[ s ] = (double) int_data[ s ];
		}
		else
		{
		    if( dt == 1 )
		    {
			// use the double data directly
			source_data = (double[]) spot_attr_data;
		    }
		    else
		    {
			mview.alertMessage( "Unable to handle '" + ms.getSpotAttributeName( msa.spot_attr_id ) + "'" +
					    "\n(the SpotAttribute must be of data-type Integer or Double)" );
			return;
		    }
		}
	    }


	    if( source_data == null )
	    {
		mview.alertMessage( "Unable to access the source data" );
		return;
	    }
	    
	    //
	    // generate a new column of doubles
	    //

	    final boolean use_nans = set_to_nan_jrb.isSelected();

	    final double new_value = use_nans ? Double.NaN : stringToDouble( other_value_jtf.getText() );

	    final boolean mung_filtered = mung_filtered_jrb.isSelected();

	    if( ( use_nans == false ) && ( new_value == Double.NaN ) )
	    {
		mview.alertMessage( "Unable to parse the value '" + other_value_jtf.getText() + "'" );
		return;
	    }

	    for(int s=0; s < n_spots; s++)
	    {
		boolean is_filtered = ( edata.filter( s ) == false );

		if( mung_filtered )
		{
		    data[ s ] = is_filtered ? source_data[ s ]: new_value;
		}
		else
		{
		    data[ s ] = is_filtered ? new_value : source_data[ s ];
		}
	    }

	    if( msa.spot_attr_id == -1 )
	    {
		System.out.println(" Source is Measurement '" + ms.getName() + "'");
	    }
	    else
	    {
		System.out.println(" Source is Attribute '" +  ms.getSpotAttributeName( msa.spot_attr_id ) + "' of Measurement '" + ms.getName() + "'");
	    }


	    // now either create a new Measurement or add a new SpotAttr to an existing Measurement 


	    String transform_str = "";

	    if( mung_filtered_jrb.isSelected() )
		transform_str = "Set filtered values to ";
	    else
		transform_str = "Set unfiltered values to ";

	    if( set_to_nan_jrb.isSelected() )
		transform_str += "'NaN'";
	    else
		transform_str += other_value_jtf.getText();
	    

	    if( msa.spot_attr_id == -1 )
	    {
		try
		{
		    String new_name = mview.getString( "Choose a name for the new Measurement", ms.getName() );

		    ExprData.Measurement meas = ms.cloneMeasurement(); // edata.new Measurement( new_name, ExprData.ExpressionAbsoluteDataType, data );
		    
		    meas.setName( new_name );
		    meas.setData( data );
		    meas.setAttribute( "Transform", "DataMunger plugin", transform_str );
		    
		    edata.addOrderedMeasurement(meas);
		    
		    // make the new Measurement visible
		    mview.getDataPlot().displayMeasurement( meas );
		}
		catch( UserInputCancelled uic )
		{
		}
	    }
	    else
	    {
		String new_name = mview.getString( "Choose a name for the new Spot Attribute", 
						   ms.getSpotAttributeName( msa.spot_attr_id ) );

		String old_unit_type =  ms.getSpotAttributeUnit( msa.spot_attr_id );

		ms.addSpotAttribute( new_name, old_unit_type, "DOUBLE", data );

	    }

	}
	catch( Exception ex )
	{
	}
    }
    

    private double stringToDouble( String str )
    {
	try
	{
	    return (new Double( str )).doubleValue();
	}
	catch( NumberFormatException nfe )
	{
	    return Double.NaN;
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  


    private void savePrefs()
    {
    }
    
    private void loadPrefs()
    {
    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  gubbins
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private maxdView mview;
    private DataPlot dplot;
    private ExprData edata;

    private JRadioButton mung_filtered_jrb, mung_unfiltered_jrb;
    private JRadioButton set_to_other_jrb, set_to_nan_jrb;
    private JTextField other_value_jtf;

    private MeasSpotAttr[] meas_spot_attr_a;

    private JButton execute_jb;

    private JComboBox meas_and_attr_jcb;

    private JFrame      frame = null;
}
