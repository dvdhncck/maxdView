//package uk.ac.man.bioinf.maxdLoad2;

import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.sql.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.util.jar.*;



public class Controller extends JPanel
{
    public static final String  preset_dir = "presets";

    private java.util.Date application_start_time;

    public  Dimension pref_window_size = new Dimension(800, 600);

    public int window_width, window_height, window_pos_x, window_pos_y;

    public final Color background_colour = new Color(45, 67, 123);
   
    final public String[] encoding_names  = { "US-ASCII", "ISO-LATIN-1", "ISO-8859-1", "UTF-8", "UTF-16BE", "UTF-16LE", "UTF-16" };


    //public boolean debug = false;   // deprecated
    //public boolean test = false;    // deprecated

    public boolean debug_driver  = false;
    public boolean debug_connect = false;
    public boolean debug_login   = false;
    public boolean debug_startup = false;

    public final boolean debug_all_sql = false;

    public boolean debug_stress_test = false;

    public maxdView mview;

    public Controller(maxdView mview_,
		      String window_geometry,
		      boolean debug_startup_ )
    {
	mview = mview_;

	System.out.println("Controller(): hello!");

	debug_startup = debug_startup_;

	//debug = debug_;
	//test = test_;

	//System.setErr( new SpecialisedPrintStream ( System.err ) );

	if(!is_applet)
	{
	    if( debug_startup )
		System.out.println("Controller(): loading configuration");

	    loadPrefs();
	    // openLogFile();
	}
	else
	{
	    System.out.println("This is an applet, trying to be secure...");
	}

	



//	logo_icon = getImageIconFromJAR("images/maxdLoad-blue.gif");

//	removeAll();
	
//	panel_gridbag = new GridBagLayout();

//	setLayout(panel_gridbag);

//	application_start_time = new java.util.Date();
	
//	decorateFrame(parent_frame);

/*
	if( getProperty("interface.type_in_history_mode", "Between Sessions" ).equals( "No" ) )
	{
	    MemoryTextField.keepHistory( false );
	    MemoryInstancePicker.keepHistory( false );
	}
*/

//	Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
	
//	final int screen_width  = (int) screen_size.getWidth();
//	final int screen_height = (int) screen_size.getHeight();
	
	/*
	if( window_geometry != null ) 
	{
	    setGeometry( window_geometry );
	}
	else
	{
	    if( getBooleanProperty( "interface.remember_window_size_and_position", true ) )
	    {
		if( window_pos_x < 0 )
		    window_pos_x = 0;
		if( window_pos_y < 0 )
		    window_pos_y = 0;
		
		if(  window_width > screen_width )
		    window_width = screen_width;
		if(  window_height > screen_height)
		    window_height = screen_height;
		
		if(( window_pos_x + window_width ) > screen_width )
		    window_pos_x = screen_width - window_width;
		if(( window_pos_y + window_height ) > screen_height)
		    window_pos_y = screen_height - window_height;
		
		parent_frame.setLocation(  window_pos_x, window_pos_y );
	    }
	}

	if( window_width == 0 )
	    window_width = screen_width / 3;
	if( window_height == 0 )
	    window_height = screen_height / 3;

	pref_window_size = new Dimension( window_width, window_height );

	pref_window_size = clipWindowSizeToScreenSize( window_pos_x, window_pos_y, pref_window_size );

	
	if( debug_startup )
	    System.out.println("Controller(): configured ok, creating connect panel");
	*/


	
//	cman = new ConnectionManager( this, application_props );

	
	//connect_panel = cman.getConnectPanel( null );


	//setupConnectionActions();
	
	

	if( debug_startup )
	    System.out.println("Controller(): connect panel constructed");

	//connect_panel.setPreferredSize( pref_window_size );
	//connect_panel.setMinimumSize( pref_window_size );


	if( debug_startup )
	    System.out.println( "Controller(): window size is " + window_width + "x" +  window_height );


	if( debug_startup )
	    System.out.println("Controller(): adding connect panel to stack");


	//addPanelToStack("Connect", connect_panel );


	// passwd_jtf.requestFocus();

/*
	String lic = getProperty("artistic_licence.agreed");
	if((lic == null) || (!lic.equals("true")))
	{
	    JPanel lp = createLicencePanel();
	    lp.setPreferredSize( pref_window_size );
	    lp.setMinimumSize( pref_window_size );
	    
	    positionWindowAtCenter( parent_frame );

	    addPanelToStack("Licence", lp);
	}
*/

	// if we appear to be running a new version, then unpack the help documentation...

/*
	String last_app_title = getProperty( "last_app_title", "[none]" );
	{
	    if( ! last_app_title.equals( maxdLoadApplication.app_title ) )
	    {
		extractHelpDocsFromJarFile();
	    }
	}

	
	putProperty( "last_app_title", maxdLoadApplication.app_title );
*/



//	if( cman.getNumberOfEntries() == 1)
//	    cman.selectFirstListEntry();

	if( debug_startup )
	    System.out.println("Controller(): initialised ok");

//	updateFonts();


       
	//cman.selectFirstListEntry();
	//attemptConnect();

    }

    // ====================================================================================


    public final boolean debugSilent()
    {
	return getProperty( "debug.verbosity" ).equals( "Silent" );
    }
    
    public final boolean debugVerbose()
    {
	return getProperty( "debug.verbosity" ).equals( "Silent" ) == false;
    }

    public final boolean debugMedium()
    {
	return getProperty( "debug.verbosity" ).equals( "Medium" );
    }
    
    

    // ====================================================================================


    public JFrame getFrame() { return mview.getMainFrame(); }

    //public maxdLoadApplication getApplication() { return parent_app; }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private String top_directory;
    private String user_directory;
    private boolean directories_identified = false;


    public String getConfigDirectory()
    { 
	if( directories_identified == false )
	{
	    identifySharedDirectory( null );
	    identifyConfigDirectory( null );
	    directories_identified = true;
	}

	return user_directory == null ? top_directory : user_directory; 
    }

    public String getSharedDirectory() 
    { 
	if( directories_identified == false )
	{
	    identifySharedDirectory( null );
	    identifyConfigDirectory( null );
	    directories_identified = true;
	}

	return top_directory; 
    }

    //
    // the installation directory (and hence the shared components such as 'standard' plugins and help documents) is identified as follows
    //
    //     using the optional command line arg "-shared_directory"  (e.g. /usr/local/software/maxdView) 
    //
    //       which overrides
    //
    //     the "MAXDLOAD_HOME" system property (i.e. the $MAXDLOAD_HOME environment variable)
    //
    //       which overrides
    //
    //     the "user.dir" system property (i.e. the current working directory, most likely the maxdView installation directory)
    //

    private void identifySharedDirectory( final String[] command_line_args )
    {
	try
	{
	    top_directory = new File( "." ).getCanonicalPath();

	    //if( top_directory.endsWith( File.separator ) == false )
	    //	top_directory += File.separator;

	}
	catch( java.io.IOException ioe )
	{
	    System.out.println("Information: unable to establish where the shared (installation) directory is");
	    top_directory = "";
	}



	String envvar_dir = System.getProperty( "MAXDLOAD_HOME", null );

	if( envvar_dir != null )
	{
	    top_directory = envvar_dir;
	}


	if( command_line_args != null)
	{
	    for( int a=0; a < command_line_args.length; a++ )
	    {
		if( command_line_args[ a ].toLowerCase().equals("-shared_directory") )
		{
		    String command_line_top_directory = ( a+1 < command_line_args.length ) ? command_line_args[ a + 1 ] : null;

		    if( ( command_line_top_directory != null ) && ( command_line_top_directory.length() > 0 ) )
		    {
			top_directory = command_line_top_directory;	

			// check that the specified directory actually exists...

			// ::TODO::
		    }
		    else
		    {
			mview.alertMessage( "The '-shared_directory' should be followed by a directory path\n" + 
					    "for example \"-shared_directory /usr/local/config/maxd\"\n" );
			System.exit( -1 );
		    }
		}
	    }
	}


	// trim off any trailing path separator(s)
	while( top_directory.charAt( top_directory.length() - 1 ) == File.separatorChar )
	     top_directory = top_directory.substring( 0, top_directory.length() - 1 );


	System.out.println("Information: shared directory is '" + top_directory + "'" );

    }
   
  

    //
    // the per-user configuration directory is identified as follows
    //
    //     using the optional command line arg "-user_directory"  (e.g. ~/.maxd/config) 
    //
    //       which overrides
    //
    //     the "user.home" system property (i.e. the $HOME environment variable)
    //
    //       which overrides
    //
    //     the "user.dir" system property (i.e. the current working directory, most likely the maxdLoad2 installation directory)
    //
    
    private void identifyConfigDirectory( final String[] command_line_args )
    {
	user_directory = System.getProperty( "user.dir", "." );

	String user_home_dir = System.getProperty( "user.home", null );

	if( user_home_dir != null )
	{
	    user_directory = user_home_dir + File.separatorChar + ".maxd" + File.separatorChar;
	    
	    //createUserConfigDirectoryIfItDoesntAlreadyExist();
	}

	if( command_line_args != null)
	{
	    for( int a=0; a < command_line_args.length; a++ )
	    {
		if( command_line_args[ a ].toLowerCase().equals("-user_directory") )
		{
		    String command_line_user_directory = ( a+1 < command_line_args.length ) ? command_line_args[ a + 1 ] : null;

		    if( ( command_line_user_directory != null ) && ( command_line_user_directory.length() > 0 ) )
		    {
			user_directory = command_line_user_directory;	

			// check that the specified directory actually exists...

			// ::TODO::
		    }
		    else
		    {
			mview.alertMessage( "The '-user_directory' argument should be followed by a directory path\n" + 
				    "for example \"-user_directory /usr/local/config/fred\"\n" );

			System.exit( -1 );
		    }
		}
	    }
	}

	if( ( user_directory == null ) || ( user_directory.length() == 0 ) )
	{
	    mview.alertMessage( "The configuration directory (where maxdLoad2 stores it's settings) could not be determined\n" +
				"This makes it impossible to run the application properly.\n\n" + 
				"See the help documentation for information about how\n" + 
				"to resolve this issue.");

	    System.exit( -1 );
	}

	// trim off any trailing path separator(s)
	while( user_directory.charAt( user_directory.length() - 1 ) == File.separatorChar )
	    user_directory = user_directory.substring( 0, user_directory.length() - 1 );
	

	System.out.println("Information: user-specific directory is '" + user_directory + "'" );
    }


/*
    public String getConfigDirectory()
    {
	return parent_app.getConfigDirectory();
    }
*/

    public String getBuiltInPresetsDirectory()
    {
	return getSharedDirectory() + File.separatorChar + "presets"; 
    }

    public String getPresetsDirectory()
    {
	return getConfigDirectory() + File.separatorChar + "maxdLoad2-presets"; 
    }



    public String getHelpDocumentRootDirectory() 
    { 
	//return parent_app.getSharedDirectory() + File.separatorChar + "docs";
	return getConfigDirectory() + File.separatorChar + "maxdLoad2-docs"; 
    }


    public String getJarFilePath( )
    {
	return getSharedDirectory() + File.separatorChar + jar_file_name;
    }


    public void updateDisplay() 
    { 
	/*
	if( current_panel != null )
	{
	    current_panel.revalidate();
	    current_panel.repaint();
	}

	((JPanel)parent_frame.getContentPane()).revalidate(); 

	parent_frame.repaint(); 
	*/

	// System.out.println("display has been updated...");
    }

    public void exitGracefully()
    {
//	System.out.println("exitGracefully()...");

	// make sure the user really wants to exit...
/*	
	JPanel current_panel = (JPanel) panel_stack_v.elementAt(0);

	if( ( current_panel != top_menu_panel ) && ( current_panel != connect_panel ) )
	{
	    if( schema_mode == InstanceHandler.CreateMode )
	    {
		if( infoQuestion("Really exit the program ?\n\n(Data on this form will be discarded)", "Yes", "No" ) == 1 )
		    return;
	    }
	    else
	    {
		if( infoQuestion("Really exit the program ?", "Yes", "No" ) == 1 )
		    return;
	    }
	}
	else
	{
	    if( getBooleanProperty( "general.warn_before_exit", false ) )
	    {
		System.out.println("3 way...");
		if( infoQuestion("Really exit the program ?", "Yes", "Not sure", "No" ) > 0 )
		    return;
	    }
	}


	if(mconn != null)
	{
	    if( bar != null )
	    {
		bar.setProgressVisible( true );
		bar.setWibbling( true );
	    }

	    mconn.commit();
	    
	    if( bar != null )
	    {
		bar.setWibbling( false );
		bar.setProgressVisible( false );
	    }
	}
	
	setVisible(false);

	if(mconn != null)
	    mconn.disconnect();

	if( getProperty("interface.type_in_history_mode", "No" ).startsWith( "Within"  ) )
	{
	    MemoryTextField.forgetAllData( this );
	    MemoryInstancePicker.forgetAllData( this );
	}

	if(!is_applet)
	{
	    // closeLogFile();
	    savePrefs();
	}

	System.exit(0);
*/
   }
 
    
    public maxdConnection_m2 getConnection() { return mconn; }

    public ConnectionManager_m2 getConnectionManager() { return cman; }

    public void setConnectionManager( ConnectionManager_m2 c ) { cman = c; }

//    public int getSchemaMode() { return schema_mode; }
 
//    int current_message = -1;


    private void setupConnectionActions()
    {
	cman.setConnectAction( new ActionListener()
	    {
		public void actionPerformed(ActionEvent ae)
		{
		    attemptConnect();
		}
	    });

	cman.setCloseAction( new ActionListener()
	    {
		public void actionPerformed(ActionEvent ae)
		{
		    exitGracefully();
		}
	    });

	cman.setHelpAction( new ActionListener()
	    {
		public void actionPerformed(ActionEvent ae)
		{
		    //HelpViewer hv = getHelpViewer();
		    //hv.reset();
		    //hv.addTextFromFile( "docs/Connect.html" );  
		    //hv.showFile( "Connect.html" );  
		}
	    });

	if( debug_startup )
	    System.out.println("setupConnectionActions(): actions added to connect panel ");
	
    }

    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

 



    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --


    
    //public  JPanel  top_menu_panel = null;
    private JButton run_batch_jobs_jb = null;

    private JList browse_list;
    private JList create_list;
    private JList find_list;

//    private CustomListSelectionListener list_sel_listener;

    private ImageIcon on_lamp_icon;
    private ImageIcon off_lamp_icon;

    public ImageIcon logo_icon;

    //private int schema_mode = InstanceHandler.CreateMode;


    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

    
    public void reportStatus( String message )
    {
	//if( message != null )
	//    System.out.println( message );

	if( status_panel == null )
	    return;

	Graphics g = status_panel.getGraphics();

	if( g == null )
	    return;

	if( ComponentFactory.useAntialiasing() )
	    ComponentFactory.setRenderingHints( (Graphics2D) g );

	g.setColor( background_colour );
	g.fillRect( 0, 0, status_panel.getWidth(), status_panel.getHeight() );

	if( message != null )
	{
	    g.setColor( Color.white );
	    
	    FontMetrics fm = g.getFontMetrics();
	    
	    int px = ( status_panel.getWidth() - fm.stringWidth( message ) ) / 2;
	    
	    int lh = ( fm.getAscent() + fm.getDescent() );
	    
	    int py = ( status_panel.getHeight() - lh ) / 2;
	    
	    
	    g.drawString( message, px, py + lh );
	}
    }




    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

    
/*
    public AttrDesc getAttrDesc( String table_name ) 
    { 
	return mconn.getAttrDesc( table_name ); 
    }
*/


    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

    
    final private String[] user_visible_table_names =
    {
	"Source", 
	"SamplingProtocol",
	//"SamplingAction",
	"Sample", 
	"TreatmentProtocol",
	//"TreatmentAction",
	"TreatedSample", 
	"ExtractionProtocol",
	//"ExtractionAction",
	"Extract", 
	"LabellingProtocol",
	//"LabellingAction",
	"LabelledExtract",
	"Array", 
	"ArrayType",
	"Feature",
	"Reporter", 
	"Gene", 
	"HybridisationProtocol", 
	//"HybridisationAction",
	"Hybridisation",
	"ScanningProtocol",
	//"ScanningAction",
	"Image", 
	"ImageAnalysisProtocol", 
	//"ImageAnalysisAction",
	"Measurement",
	"Submitter",
	"Experiment",

	// should the Action tables be included here? YES! - not any more....

    };

    private final String[] all_data_table_names =
    // note the ordering which ensures any FK dependancies will be avoided
    { 
	"IntegerProperty",
	"DoubleProperty", 
	"CharProperty", 
	"StringProperty", 
	"Experiment", 
	"Submitter", 
	"MeasurementList", 
	"Measurement", 
	"PropertyList", 
	"Property",
	"Type", 
	//"ImageAnalysisAction",
	"ImageAnalysisProtocol", 
	"Image", 
	//"ScanningAction",
	"ScanningProtocol",
	"Hybridisation", 
	//"HybridisationAction",
	"HybridisationProtocol", 
	"Array", 
	"ArrayType", 
	"Feature", 
	"Reporter", 
	"GeneList", 
	"Gene", 
	"LabelledExtractList", 
	"LabelledExtract",
	//"LabellingAction",
	"LabellingProtocol",
	"Extract", 
	//"ExtractionAction",
	"ExtractionProtocol", 
	"TreatedSampleList", 
	"TreatedSample", 
	//"TreatmentAction",
	"TreatmentProtocol", 
	"Sample", 
	//"SamplingAction",
	"SamplingProtocol", 
	"Source", 
	"AttributeText", 
    };
    
    private final String[] all_system_table_names =
    // note the ordering which ensures any FK dependancies will be avoided
    { 
	"maxdTranslations",
	"maxdProperties"
    };

    public String[] getAllUserVisibleTables( )
    {
	return user_visible_table_names;
    }
    public String[] getAllTables( )
    {
	return all_data_table_names;
    }
    public String[] getAllSystemTables( )
    {
	return all_system_table_names;
    }

    public String[] getAllUserVisibleTablesExcept( String exclude )
    {
	Vector all_except = new Vector();

	for(int t=0; t < user_visible_table_names.length; t++)
	    if( ! user_visible_table_names[ t ].equals( exclude ) )
		all_except.add( user_visible_table_names[ t ] );

	return (String[]) all_except.toArray( new String[0] );
    }


    public String[] getAllIndexTableNames()
    {
	return new String[] { "StrPropIndex", "ChrPropIndex", "IntPropIndex", "DblPropIndex" };
    }


    public boolean tableHasRecursiveLink( String table_name )
    {
	return table_name.equals( "TreatedSample" );
    }

    // this is essentially the same as 'user visible tables'
    public boolean tableCanBeUsedDuringFindLinked( String table_name )
    {

	if( table_name.equals("Property") )
	    return false;
	if( table_name.equals("IntegerProperty") )
	    return false;
	if( table_name.equals("DoubleProperty") )
	    return false;
	if( table_name.equals("StringProperty") )
	    return false;
	if( table_name.equals("CharProperty") )
	    return false;

	return true;
    }
 
    private Hashtable createForeignKeyTableData()
    {
	Hashtable ht = new Hashtable();
	
	// note that references to instance list lists are NOT counted as FKs

	ht.put( "Experiment", new String[] { "Submitter", "Measurement" } );
	ht.put( "Sample", new String[] { "Source", "SamplingProtocol" } );
	ht.put( "TreatedSample", new String[] { "TreatedSample", "Sample", "TreatmentProtocol" } );
	ht.put( "TreatedSampleList", new String[] { "TreatedSample" } );
	ht.put( "Extract", new String[] { "TreatedSample", "ExtractionProtocol" } );
	ht.put( "LabelledExtract", new String[] { "Extract", "LabellingProtocol" } );
	ht.put( "LabelledExtractList", new String[] { "LabelledExtract" } );
	ht.put( "Array", new String[] { "ArrayType" } );
	ht.put( "Feature", new String[] { "ArrayType", "Reporter" } );
	ht.put( "GeneList", new String[] { "Gene" } );
	ht.put( "Reporter", new String[] { "Gene" } );
	ht.put( "Hybridisation", new String[] { "Array", "LabelledExtract", "HybridisationProtocol" } );
	ht.put( "Image", new String[] { "Hybridisation", "ScanningProtocol" } );

	// 13-10-2003 added 'Property' to FK list for 'Measurement' 

	// removed again - it confuses the hell out of findPathToTable()....
	ht.put( "Measurement", new String[] { "Image", "ImageAnalysisProtocol" /*, "Property"*/ } );

 	ht.put( "MeasurementList", new String[] { "Measurement" } );

 	ht.put( "PropertyList", new String[] { "Property" } );

	ht.put( "Property", new String[] { "Type", "LabelledExtract" } );

	// hopefully this won't break anything.....but it's needed for ExportNative
	ht.put( "IntegerProperty", new String[] { "Measurement", "Feature", "Type" } );
	ht.put( "DoubleProperty", new String[] { "Measurement", "Feature", "Type" } );
	ht.put( "CharProperty", new String[] { "Measurement", "Feature", "Type" } );
	ht.put( "StringProperty", new String[] { "Measurement", "Feature", "Type" } );

	return ht;
    }

    Hashtable fk_table_ht = null;

    public String[] getForeignKeys( String table_name )
    {
	if( fk_table_ht == null )
	    fk_table_ht = createForeignKeyTableData();

	if( table_name == null )
	{
	    System.out.println("getForeignKeys(): Odd! null table name");
	    return null;
	}

	return (String[]) fk_table_ht.get( table_name );
    }

    public boolean hasAttributeLink( String table_name )
    {
	if( table_name.equals("Property") )
	    return false;
	if( table_name.equals("AttributeText") )
	    return false;
	if( table_name.equals("IntegerProperty") )
	    return false;
	if( table_name.equals("DoubleProperty") )
	    return false;
	if( table_name.equals("StringProperty") )
	    return false;
	if( table_name.equals("CharProperty") )
	    return false;
	if( isLinkTableName( table_name ) )
	    return false;

	return true;
    }

    public boolean isPropertyTable( String table_name )
    {
	if( table_name.equals("IntegerProperty") )
	    return true;
	if( table_name.equals("DoubleProperty") )
	    return true;
	if( table_name.equals("StringProperty") )
	    return true;
	if( table_name.equals("CharProperty") )
	    return true;
	
	return false;
    }


    public Hashtable getForeignKeys(  )
    {
	return fk_table_ht;
    }

    public boolean isForeignKeyRequired( String table_name, String field_name )
    {
	if( table_name.equals("Experiment") && field_name.equals("Measurement") )
	    return false;

	if( table_name.equals("Measurement") && field_name.equals("Property") )
	    return false;

	if( table_name.equals("Reporter") && field_name.equals("Gene") )
	    return false;

	if( table_name.equals("Feature") && field_name.equals("Reporter") )
	    return false;

	if( table_name.equals("TreatedSample") && field_name.equals("Sample") )
	    return false;

	if( table_name.equals("TreatedSample") && field_name.equals("TreatedSample") )
	    return false;

	return true;
    }

    // this version can handle the contraints, such as "one or other of these keys must exist"
    //
    // ::TODO:: unify the validation of FKs and AttrDescs using the 'SCL' (snazzy constraint language)
    //
    public boolean isForeignKeyRequired( String table_name, String field_name, java.util.HashSet fks_which_have_values )
    {
	// only 'TreatedSample' needs special handling at the moment
	if( table_name.equals("TreatedSample") )
	{
	    if( field_name.equals("Sample") )
	    {
		return ( fks_which_have_values.contains( "TreatedSample" ) == false );
	    }
	    if( field_name.equals("TreatedSample") )
	    {
		return ( fks_which_have_values.contains( "Sample" ) == false );
	    }
	    return true;
	}
	else
	{
	    return isForeignKeyRequired( table_name, field_name );
	}
    }

    // this version can handle the contraints, such as "only one or other of these keys must exist"
    public boolean isForeignKeyPermitted( String table_name, String field_name, java.util.HashSet fks_which_have_values )
    {
	// only 'TreatedSample' needs special handling at the moment
	if( table_name.equals("TreatedSample") )
	{
	    if( field_name.equals("Sample") )
	    {
		return ( fks_which_have_values.contains( "TreatedSample" ) == false );
	    }
	    if( field_name.equals("TreatedSample") )
	    {
		return ( fks_which_have_values.contains( "Sample" ) == false );
	    }
	    return true;
	}
	else
	{
	    return true;
	}
    }
    

    private Hashtable createForeignKeyFieldData()
    {
	Hashtable ht = new Hashtable();
	
	// note: that references to instance list tables _are_ included here,
	//       and that the foreign key field is the field which identifies the list

	ht.put( "Experiment.Submitter", "Submitter_ID" );
	ht.put( "Experiment.Measurement", "Measurement_List_ID" );

	ht.put( "Sample.SamplingProtocol", "Sampling_Protocol_ID" );
	ht.put( "Sample.Source", "Source_ID" );

	ht.put( "TreatedSample.TreatmentProtocol", "Treatment_Protocol_ID" );
	ht.put( "TreatedSample.Sample", "Sample_ID" );
	ht.put( "TreatedSample.TreatedSample", "Treated_Sample_ID" );

	ht.put( "TreatedSampleList.TreatedSample", "Treated_Sample_ID" );

	ht.put( "Extract.ExtractionProtocol", "Extraction_Protocol_ID" );
	ht.put( "Extract.TreatedSample", "Treated_Sample_List_ID" );

	ht.put( "LabelledExtract.LabellingProtocol", "Labelling_Protocol_ID" );
	ht.put( "LabelledExtract.Extract", "Extract_ID" );

	ht.put( "LabelledExtractList.LabelledExtract", "Labelled_Extract_ID" );

	ht.put( "Array.ArrayType", "Array_Type_ID" );

	ht.put( "Feature.Reporter", "Reporter_ID" );
	ht.put( "Feature.ArrayType", "Array_Type_ID" );

	ht.put( "GeneList.Gene", "Gene_ID" );

	ht.put( "Reporter.Gene", "Gene_List_ID" );

	ht.put( "Hybridisation.HybridisationProtocol", "Hybridisation_Protocol_ID" );
	ht.put( "Hybridisation.LabelledExtract", "Labelled_Extract_List_ID" );
	ht.put( "Hybridisation.Array", "Array_ID" );

	ht.put( "Image.Hybridisation", "Hybridisation_ID" );
	ht.put( "Image.ScanningProtocol", "Scanning_Protocol_ID" );

	ht.put( "PropertyList.Property", "Property_ID" );

	ht.put( "Property.Type", "Type_ID" );
	ht.put( "Property.LabelledExtract", "Labelled_Extract_ID" );

	ht.put( "Measurement.Image", "Image_ID" );
	ht.put( "Measurement.ImageAnalysisProtocol", "Image_Analysis_Protocol_ID" );

	// NOTE: although there the FK link Measurement->Property is not declared (see above) there
	//       is still an entry here so that the link can be followed if required
	//  
	//       this is just a kludge to overcome the problem with findPathToTable() not being
	//       able to find the 'best' (i.e. shortest) path
	//
	ht.put( "Measurement.Property", "Property_List_ID" );

	ht.put( "MeasurementList.Measurement", "Measurement_ID" );


	// hopefully this won't break anything.....but it's needed for ExportNative
	ht.put( "IntegerProperty.Measurement", "Measurement_ID" );
	ht.put( "IntegerProperty.Feature", "Feature_ID" );
	ht.put( "IntegerProperty.Type", "Type_ID"  );

	ht.put( "DoubleProperty.Measurement", "Measurement_ID" );
	ht.put( "DoubleProperty.Feature", "Feature_ID" );
	ht.put( "DoubleProperty.Type", "Type_ID"  );

	ht.put( "StringProperty.Measurement", "Measurement_ID" );
	ht.put( "StringProperty.Feature", "Feature_ID" );
	ht.put( "StringProperty.Type", "Type_ID"  );

	ht.put( "CharProperty.Measurement", "Measurement_ID" );
	ht.put( "CharProperty.Feature", "Feature_ID" );
	ht.put( "CharProperty.Type", "Type_ID"  );


	return ht;
    }

    Hashtable fk_field_ht = null;

    public String getForeignKeyField( String table_name, String fk_table_name )
    {
	if( fk_field_ht == null )
	    fk_field_ht = createForeignKeyFieldData();
	
	return (String) fk_field_ht.get( table_name + "." +  fk_table_name );
    }


    public String[] getReversedForeignKeys( String table_name )
    {
	if( fk_table_ht == null )
	    fk_table_ht = createForeignKeyTableData();

	if( table_name == null )
	{
	    System.out.println("getForeignKeys(): Odd! null table name");
	    return null;
	}

	Vector result = new Vector();

	for( Enumeration en=fk_table_ht.keys(); en.hasMoreElements();  )
	{
	    String   key     = (String) en.nextElement();
	    String[] fk_refs = (String[]) fk_table_ht.get(key);

	    for(int k=0; k < fk_refs.length; k++)
		if( fk_refs[k].equals( table_name ) )
		    result.addElement( key );
	}

	return (String[]) result.toArray( new String[0] );
    }


    public String[] getReverseLinkedTables( String table_name )
    {
	Vector rev_v = new Vector();
	
	addReverseLinkedTables( table_name, rev_v );

	return (String[]) rev_v.toArray( new String[0] );
    }

    private void addReverseLinkedTables( String table_name, Vector rev_v )
    {
	String[] tables = getReversedForeignKeys( table_name );

	if( tables == null )
	    return;

	for(int t=0; t < tables.length; t++)
	{
	    rev_v.add( tables[t] );
	    
	    addReverseLinkedTables( tables[t], rev_v );
	}
    }

    public static final int OneToOneLink  = 0;
    public static final int OneToManyLink = 1;
    public static final int ManyToOneLink = 1;
    public static final int RecursiveLink = 2;


    //
    // true if the link is a 'normal' foreign key link from one table to another
    //
    public boolean isForwardLink( String from_table, String to_table )
    {
	return ( getForeignKeyField( from_table, to_table ) != null );
    }

    //
    // true if the link is a reversed key link from one table to another ( i.e. isForwardLink(A,B) implies isReverseLink(B,A) )
    //
    public boolean isReverseLink( String from_table, String to_table )
    {
	return isForwardLink( to_table, from_table );
    }


    public int getLinkType( String table_name, String fk_table_name )
    {
	if( table_name.equals("Hybridisation") && fk_table_name.equals("LabelledExtract") )
	    return OneToManyLink;

	if( table_name.equals("Reporter") && fk_table_name.equals("Gene") )
	    return OneToManyLink;

	if( table_name.equals("Measurement") && fk_table_name.equals("Property") )
	    return OneToManyLink;
	
	if( table_name.equals("Experiment") && fk_table_name.equals("Measurement") )
	    return OneToManyLink;

	if( table_name.equals("Extract") && fk_table_name.equals("TreatedSample") )
	    return OneToManyLink;

	if( table_name.equals("TreatedSample") && fk_table_name.equals("TreatedSample") )
	    return RecursiveLink;

	return OneToOneLink;
    }

    public String getLinkTableName( String table_name, String fk_table_name )
    {
	return fk_table_name + "List";
    }

    public boolean isLinkTableName( String table_name )
    {
	return ( table_name.equals( "MeasurementList" ) || 
		 table_name.equals( "PropertyList" ) || 
		 table_name.equals( "LabelledExtractList" ) || 
		 table_name.equals( "TreatedSampleList" ) || 
		 table_name.equals( "GeneList" ) );
    }



    private java.util.Hashtable numeric_fields_for_table_ht;
    private java.util.Hashtable text_fields_for_table_ht;

    public String[] getNumericFields( String table_name )
    {
	if( numeric_fields_for_table_ht == null )
	{
	    numeric_fields_for_table_ht = new java.util.Hashtable();
	    numeric_fields_for_table_ht.put( "AttributeText",   new String[] { "Index" } );
	    numeric_fields_for_table_ht.put( "StringProperty",  new String[] { "Index" } );
	    numeric_fields_for_table_ht.put( "IntegerProperty", new String[] { "Value" } );
	    numeric_fields_for_table_ht.put( "DoubleProperty",  new String[] { "Value" } );

	}
	return (String[]) numeric_fields_for_table_ht.get( table_name );
    }

    public String[] getTextFields( String table_name )
    {
	if( text_fields_for_table_ht == null )
	{
	    text_fields_for_table_ht = new java.util.Hashtable();
	    text_fields_for_table_ht.put( "AttributeText",  new String[] { "Value" } );
	    text_fields_for_table_ht.put( "StringProperty", new String[] { "Value" } );
	    text_fields_for_table_ht.put( "CharProperty",   new String[] { "Value" } );
	}
	return (String[]) text_fields_for_table_ht.get( table_name );
    }




    public boolean isBulkLoadingAllowed( String table_name )
    {
	if( table_name.equals("Feature") )
	    return true;
	if( table_name.equals("Reporter") )
	    return true;
	if( table_name.equals("Gene") )
	    return true;

	return false;
    }

    public boolean isBulkLoadingAllowedForField( String table_name, String field_name )
    {
	/*
	if( table_name.equals("ArrayType") && field_name.equals( "Feature" ) )
	    return true;
	*/

	return false;
    }

    Hashtable bulk_fk_field_ht = null;

    private Hashtable createPredefinedBulkLoadingFieldData()
    {
	Hashtable ht = new Hashtable();

	ht.put( "Experiment", new String[] { "Name", "Submitter", "Measurement" } );
	ht.put( "Submitter", new String[] { "Name" } );
	ht.put( "Measurement", new String[] { "Name","Image","ImageAnalysisProtocol" } );
	ht.put( "Image", new String[] { "Name","ScanningProtocol","Hybridisation" } );

	ht.put( "Measurement.Data", new String[] { "Feature" } );
	ht.put( "ArrayType.Feature", new String[] { "Name", "Reporter" } );

	ht.put( "Array",  new String[] { "Name", "ArrayType" } );
	ht.put( "ArrayType", new String[] { "Name" } );
	ht.put( "Feature", new String[] { "Name", "Reporter", "ArrayType" } );
	ht.put( "Reporter", new String[] { "Name", "Gene" } );
	ht.put( "Gene", new String[] { "Name" } );

	ht.put( "Source",  new String[] { "Name" } );
	ht.put( "Sample",  new String[] { "Name", "Source", "SamplingProtocol" } );
	ht.put( "TreatedSample",  new String[] { "Name", "Sample", "TreatedSample", "TreatmentProtocol" } );
	ht.put( "Extract",  new String[] { "Name", "TreatedSample", "ExtractionProtocol" } );
	ht.put( "LabelledExtract",  new String[] { "Name", "Extract", "LabellingProtocol" } );
	ht.put( "Hybridisation",  new String[] { "Name", "HybridisationProtocol", "LabelledExtract", "Array" } );

	/*
	ht.put( "SamplingAction",  new String[] { "Name", "SamplingProtocol" } );
	ht.put( "TreatmentAction",  new String[] { "Name", "TreatmentProtocol" } );
	ht.put( "ExtractionAction",  new String[] { "Name", "ExtractionProtocol"  } );
	ht.put( "LabellingAction",  new String[] { "Name", "LabellingProtocol" } );
	ht.put( "HybridisationAction",  new String[] { "Name", "HybridisationProtocol" } );
	ht.put( "ScanningAction",  new String[] { "Name", "ScanningProtocol" } );
	ht.put( "ImageAnalysisAction",  new String[] { "Name", "ImageAnalysisProtocol" } );
	*/

	ht.put( "SamplingProtocol",  new String[] { "Name" } );
	ht.put( "TreatmentProtocol",  new String[] { "Name" } );
	ht.put( "ExtractionProtocol",  new String[] { "Name" } );
	ht.put( "LabellingProtocol",  new String[] { "Name" } );
	ht.put( "HybridisationProtocol",  new String[] { "Name" } );
	ht.put( "ScanningProtocol",  new String[] { "Name" } );
	ht.put( "ImageAnalysisProtocol",  new String[] { "Name" } );

	// ht.put( "",  new String[] { " Name" } );

	return ht;
    }



    public String[] getPredefinedBulkLoadingFields( String table_name )
    {
	if( bulk_fk_field_ht == null )
	    bulk_fk_field_ht = createPredefinedBulkLoadingFieldData();

	return (String[]) bulk_fk_field_ht.get( table_name );
    }


    Hashtable bulk_fk_field_name_ht = null;

    private Hashtable createPredefinedBulkLoadingFieldNameData()
    {
	Hashtable ht = new Hashtable();

	ht.put( "Experiment", new String[] { "Name", "Submitter_ID", "Measurement_ID" } );
	ht.put( "Submitter", new String[] { "Name" } );
	ht.put( "Measurement", new String[] { "Name", "Image_ID", "Image_Analysis_Protocol_ID" } );
	ht.put( "Image", new String[] { "Name","Scanning_Protocol_ID","Hybridisation_ID" } );

	ht.put( "Measurement.Data", new String[] { "Name" } );
	ht.put( "ArrayType.Feature", new String[] { "Name", "Reporter_ID" } );

	ht.put( "Array",  new String[] { "Name", "Array_Type_ID" } );
	ht.put( "ArrayType", new String[] { "Name" } );
	ht.put( "Feature", new String[] { "Name", "Reporter_ID", "Array_Type_ID" } );
	ht.put( "Reporter", new String[] { "Name", "Gene_ID" } );
	ht.put( "Gene", new String[] { "Name" } );

	ht.put( "Source",  new String[] { "Name" } );
	ht.put( "Sample",  new String[] { "Name", "Source_ID", "Sampling_Protocol_ID" } );
	ht.put( "TreatedSample",  new String[] { "Name", "Sample_ID", "Treated_Sample_ID", "Treatment_Protocol_ID" } );
	ht.put( "Extract",  new String[] { "Name", "Treated_Sample_ID", "Extraction_Protocol_ID" } );
	ht.put( "LabelledExtract",  new String[] { "Name", "Extract_ID", "Labelling_Protocol_ID" } );
	ht.put( "Hybridisation",  new String[] { "Name", "Hybridisation_Protocol_ID", "Labelled_Extract_ID", "Array_ID" } );

	/*
	ht.put( "SamplingAction",  new String[] { "Name", "Sampling_Protocol_ID" } );
	ht.put( "TreatmentAction",  new String[] { "Name", "Treatment_Protocol_ID" } );
	ht.put( "ExtractionAction",  new String[] { "Name", "Extraction_Protocol_ID" } );
	ht.put( "LabellingAction",  new String[] { "Name", "Labelling_Protocol_ID" } );
	ht.put( "HybridisationAction",  new String[] { "Name", "Hybridisation_Protocol_ID" } );
	ht.put( "ScanningAction",  new String[] { "Name", "Scanning_Protocol_ID" } );
	ht.put( "ImageAnalysisAction",  new String[] { "Name", "Image_Analysis_Protocol_ID" } );
	*/

	ht.put( "SamplingProtocol",  new String[] { "Name" } );
	ht.put( "TreatmentProtocol",  new String[] { "Name" } );
	ht.put( "ExtractionProtocol",  new String[] { "Name" } );
	ht.put( "LabellingProtocol",  new String[] { "Name" } );
	ht.put( "HybridisationProtocol",  new String[] { "Name" } );
	ht.put( "ScanningProtocol",  new String[] { "Name" } );
	ht.put( "ImageAnalysisProtocol",  new String[] { "Name" } );

	// ht.put( "",  new String[] { " Name" } );

	return ht;
    }

    public String[] getPredefinedBulkLoadingFieldNames( String table_name )
    {
	if( bulk_fk_field_name_ht == null )
	    bulk_fk_field_name_ht = createPredefinedBulkLoadingFieldNameData();

	return (String[]) bulk_fk_field_name_ht.get( table_name );
    }


    public String[] getPredefinedBulkLoadingFieldComments( String table_name )
    {
	
	return null;
    }

    public boolean isPredefinedBulLoadingFieldRequired( String table_name, String field_name )
    {
	if( table_name.equals("Reporter") && field_name.equals("Gene") )
	    return false;

	return true;
    }

    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

    boolean debug_find_linked = false; // true;

    /*
        retrieveLinkedInstances():

	  follows the link from one table to another 

            - but only a single link, and only in the correct link direction
	
	    - for more advanced link following, use the findLinkedInstances method

    */


    //
    // ::TODO:: why are there two versions of this? - the other version is unrelated, and is really part of findLinkedInstances()
    //
    // ::TODO:: integrate with version in InstanceHandler - done
    // 
    // ::TODO:: add support for RecursiveLink - done
    // 
    // ::TODO:: maybe integrate with findLinkedInstances() - make this code a special case of the generic link following method?
    //

    
    public Instance[] retrieveLinkedInstances( final String from_table, final Instance[] insts, final String to_table )
    {
	if(( insts == null ) || ( insts.length == 0 ))
	    return null;
	    
//	maxdConnection mconn = cntrl.getConnection();

	Vector insts_v = new Vector();
	
	if( getLinkType( from_table, to_table ) == Controller.OneToManyLink )
	{
	    if( debug_find_linked )
		System.out.println("  from=" + from_table + " to " + to_table + " via a List, " + 
				   "starting with " + insts.length + " instances" );

	    /*
                e.g.  from Hybridisation to LabelledExtract (via LabelledExtractList)
	    
	    	    foreach Hybridisation 'h'
		       get 'lel', the LabelledExtractList linked to 'h'
		       get the LabelledExtracts in 'lel'

	    */

	    String link_table_name     = getLinkTableName( from_table, to_table );        // LabelledExtractList
	    String instance_fk_name    = getForeignKeyField( link_table_name, to_table ); // Labelled_Extract_ID
	    String instance_table_name = to_table;                                        // LabelledExtract

	    String link_fk_name        = getForeignKeyField( from_table, to_table );      // Labelled_Extract_List_ID


	    // System.out.println("   ltn=" + link_table_name );
	    // System.out.println("   itn="  + instance_table_name + "\nifn=" + instance_fk_name);
	    // System.out.println("   lfn=" + link_fk_name );

	    
	    for(int i=0; i < insts.length; i++ )
	    {
		String link_fk_value = mconn.getFieldFromID( insts[i].id, link_fk_name, from_table );
		
		Instance[] insts_2 = mconn.retrieveInstanceList( link_fk_value,
								 link_table_name, 
								 instance_fk_name, 
								 instance_table_name );
		if( insts_2.length > 0 )
		{
		    //System.out.println("     " + insts_2.length + " found");

//                  int things_to_print = insts_2.length;
//			things_to_print = things_to_print > 20 ? 20 : things_to_print;
//		    for(int i2=0; i2 < things_to_print; i2++)
//			System.out.println("       " + insts_2[i2].name );
		}
		
		for(int i2=0; i2 < insts_2.length; i2++)
		    insts_v.add( insts_2[ i2 ] );
	     } 
	}
	else
	{
	    // not a OneToManyLink
	    
	    String fk_field = getForeignKeyField( from_table, to_table );
	    
	    if( debug_find_linked )
		System.out.println("  from=" + from_table + " to " + to_table + " using " + fk_field + 
				   ", starting with " + insts.length + " instances" );
	    
	    for(int i=0; i < insts.length; i++ )
	    {
		String fk_id = mconn.getFieldFromID( insts[i].id, fk_field, from_table );

		Instance[] insts_2 = mconn.getInstancesUsingFKField( fk_id, "ID", to_table );
		
		if( insts_2 != null )
		{
		    if( insts_2.length > 0 )
		    {
			//System.out.println("     " + insts_2.length + " found");

//			int things_to_print = insts_2.length;
//			things_to_print = things_to_print > 20 ? 20 : things_to_print;
//			for(int i2=0; i2 < things_to_print; i2++)
//			    System.out.println("       " + insts_2[i2].name );
		    }
		    
		    for(int i2=0; i2 < insts_2.length; i2++)
			insts_v.add( insts_2[ i2 ] );
		}
	    }
	}
	
	return makeInstancesUnique( (Instance[]) insts_v.toArray( new Instance[ insts_v.size() ] ) );
    }
    
    private Instance[] makeInstancesUnique( Instance[] insts )
    {
	if(( insts == null ) || ( insts.length == 0 ))
	    return null;

	HashSet insts_hs = new HashSet();
	Vector   unique   = new Vector();

	for( int i = 0; i < insts.length; i++ )
	{
	    if( ! insts_hs.contains( insts[i].id ) )
	    {
		insts_hs.add( insts[i].id );
		unique.add( insts[i] );
	    }
	}

	return ( Instance[] ) unique.toArray( new Instance[ unique.size() ] );
    }


    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

   /*
        retrieveLinkedInstancesLinkedTo() 

	    e.g.:

              to_table: Extract     
            from_table: TreatedSample
            from_insts: 1 or more TreatedSamples

	    finds all Extracts linked to any TreatedSamples in 'from_insts'

    */

    //
    //
    //
    public Instance[] retrieveInstancesLinkedTo( final String to_table, final String from_table, final Instance from_inst )
    {
	Instance[] inst_a = new Instance[ 1 ];
	inst_a[ 0 ] = from_inst;

	return retrieveInstancesLinkedTo( to_table, from_table, inst_a );
    }

    public Instance[] retrieveInstancesLinkedTo( final String to_table, final String from_table, final Instance[] from_insts )
    {
	if(( from_insts == null ) || ( from_insts.length == 0 ))
	    return null;
	    
	Vector to_insts_v = new Vector();
	
	if( getLinkType( to_table, from_table ) == Controller.OneToManyLink )
	{
	    /*
                e.g.  which Hybridisations are linked to the LabelledExtract(s) ? (via LabelledExtractList)
	    
	    	    foreach LabelledExtract 'le'
		       get 'lel', the LabelledExtractLists including 'le'
		       get any Hybridisation(s) using any of the lists 'lel'

	    */

	    String link_table_name     = getLinkTableName( to_table, from_table );            // LabelledExtractList
	    String instance_fk_name    = getForeignKeyField( link_table_name, from_table );   // Labelled_Extract_ID
	    String instance_table_name = from_table;                                          // LabelledExtract
	    String link_fk_name        = getForeignKeyField( to_table, from_table );          // Labelled_Extract_List_ID

	    if( debug_find_linked )
	    {
		System.out.println("retrieveInstancesLinkedTo()");

		System.out.println("  from=" + from_table + " to " + to_table + " via list " + link_table_name + 
				   " starting with " + from_insts.length + " instances" );

		System.out.println("   ltn=" + link_table_name );
		System.out.println("   itn=" + instance_table_name );
		System.out.println("   ifn=" + instance_fk_name);
		System.out.println("   lfn=" + link_fk_name );
	    }

	    
	    for(int i=0; i < from_insts.length; i++ )
	    {
		
		//String link_fk_value = mconn.getFieldFromID( from_insts[i].id, link_fk_name, from_table );
		
		Instance[] list_insts = mconn.retrieveInstanceListsContainingSomeInstance( from_insts[i].id ,
											   instance_fk_name, 
											   link_table_name );

		if( list_insts.length > 0 )
		{
		    // System.out.println("     " + list_insts.length + " lists found containing " + from_insts[i].name );

//                  int things_to_print = insts_2.length;
//			things_to_print = things_to_print > 20 ? 20 : things_to_print;
//		    for(int i2=0; i2 < things_to_print; i2++)
//			System.out.println("       " + insts_2[i2].name );
		}
		
		for(int i2=0; i2 < list_insts.length; i2++)
		{
		    // get any of the instances in 'to_table' which link to this list

		    String target_list_id = list_insts[i2].id;
		    
		    Instance[] users = mconn.getInstancesUsingFKField( target_list_id, link_fk_name, to_table );

		    if( users != null )
			for(int i3=0; i3 < users.length; i3++)
			    to_insts_v.add( users[ i3 ] );
		}
	     } 
	}
	else
	{
	    // this is a one-to-one link

	    /*
                e.g.  which Samples are linked to the Source(s) 
	    
	    	    foreach Source 'src'
		       get any Samples(s) linked to 'src'


		         to = Sample
		       from = Source
	    */

	    String fk_field = getForeignKeyField( to_table, from_table );
	    
	    if( debug_find_linked )
	    {
		System.out.println("retrieveInstancesLinkedTo()");
		System.out.println("  from=" + from_table + " to " + to_table + " using " + fk_field + 
				   ", starting with " + from_insts.length + " instances" );
	    }

	    for(int i=0; i < from_insts.length; i++ )
	    {
		Instance[] users = mconn.getInstancesUsingFKField( from_insts[i].id, fk_field, to_table );
		
		if( debug_find_linked )
		{
		    System.out.println("   " + 
				       from_insts[i].name + 
				       " is used by " + 
				       ( users == null ? 0 : users.length ) + 
				       " instances" );
		}

		if( users != null )
		{
		    for(int i2=0; i2 < users.length; i2++)
			to_insts_v.add( users[ i2 ] );
		}
	    }
	}

	
	return makeInstancesUnique( (Instance[]) to_insts_v.toArray( new Instance[0] ) );
    }
  

    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

    /*

              source <- sample <- extract -> extraction_protocol
    

	      reporter <- feature -> array_type <- array -> hybridisation

	      
	      
	      to get from reporter X to feature(s) S 
	      
                reverse link:

	          select id from feature where reporter_id = X;

                forward link:

	      to get from features F to array_type(s) A
	      
	          select array_type_id from feature where id = F;

    */


    private class Link
    {
	public boolean is_reverse;
	public String table_name;
	public boolean is_recursive;    // can this table have recursive links?

	public Link( String tn, boolean ir, boolean re )
	{
	    table_name = tn;
	    is_reverse = ir;
	    is_recursive = re;
	}
	public String toString()
	{
	    return ( is_reverse ? "[R]" : "[F]" ) + table_name + ( is_recursive ? "[rec]" : "" );
	}
    }


    //
    // capable of resolving the connection between 'inst' and anything linked to it in the specified 'target_table'
    //
    // the ButtonAndProgressBar is passed in so that the state of the 'cancel' button can be checked to see
    // if the user has tried to abort the operation...
    //
    public final Instance[] findLinkedInstances( final String start_table, 
						 final String target_table, 
						 final Instance inst )
    {
	// get a path from the current table to the target_table

	try
	{
	    // need to make sure the 'shortest' or 'best' path is used.
	    // (i.e. 'Measurement-[F]-Property' rather than 'Measurement-[R]-Image-[F]-Hybridisation-[F]-LabelledExtract-[R]-Property')

	    //
	    // how to correctly handle recursive links?
	    //
	    // e.g.  Extract-[F]-TreatedSample-[F]-TreatedSample-[F]-TreatedSample-[F]-Sample

	    Link[] path = findPathToTable( start_table, target_table );
	    
	    if( path != null )
	    {
		if( debug_find_linked )
		{
		    System.out.println( " path from " + start_table + " to " + target_table );
		    System.out.print( " -( " );
		    for(int p=0; p < path.length; p++ )
		    {
			System.out.print( path[p] + " - "  );
		    }
		    System.out.println( " )-" );
		}
		
		if( inst == null )
		{
		    return null;
		}
		
		Instance[] linked_insts = retrieveLinkedInstances( start_table, inst, path );
		
		if( linked_insts  == null )
		{
		    return null;
		}
		else
		{
		    return linked_insts;
		}
	    }
	    else
	    {
		System.err.println( "browseLinkedInstances(): WARNING! no path from " + start_table + " to " + target_table );

		return null;
	    }
	}
	catch( Exception ex )
	{
	    generateErrorReport( ex );
	    
	    //ex.printStackTrace();
	    //cntrl.alertMessage( "Unhandled exception whilst searching for linked instances\n\n" + ex);

	    return null;
	}

    }

    // ----------------------------------------------------------------------------------------------


    private java.util.Hashtable path_to_root_ht; // a result cache

    //
    // work out how to get to the root table from the 'target_table'
    //
    // return a string of the form "grandparent.parent.table" which uniquely identifies
    // the first occurence of the 'target_table' in the schema.
    //
    // this method is expected to work corectly even when the schema is recursive.
    //
    // once a result has been found, cache it for future re-use
    //
    public String findPathToRoot( final String target_table )
    {
	if( path_to_root_ht == null )
	    path_to_root_ht = new java.util.Hashtable();

	String path = (String) path_to_root_ht.get( target_table );

	if( path == null )
	{
	    path = findPathToRoot( target_table, "Experiment", "", new java.util.HashSet() );

	    if( path != null )
		path_to_root_ht.put( target_table, path );
	}

	return path;
    }

    //
    // return a string of the form "grandparent.parent.table" which uniquely identifies
    // the first occurence of the 'target_table' in the schema.
    //
    // this method is expected to work corectly even when the schema is recursive.
    //
    private String findPathToRoot( final String current_table, final String root_table, final String path_so_far, final java.util.HashSet already_visisted )
    {
	if( current_table.equals( root_table ) )
	{
	    // it's a winner!

	    return concatenatePath( current_table, path_so_far );
	}

	// check each of the 'parent's of this table...

	String[] rev_fkeys = getReversedForeignKeys( current_table );
	
	if( rev_fkeys != null )
	{
	    final String updated_path_so_far = concatenatePath( current_table, path_so_far );

	    for( int f=0; f < rev_fkeys.length; f++ )
	    {
		if( rev_fkeys[ f ].equals( current_table ) == false ) // watch out for recursive links 
		{
		    if( already_visisted.contains( rev_fkeys[ f ] ) == false ) // watch out for cyclic paths
		    {
			already_visisted.add( rev_fkeys[ f ] );

			final String check_this_parent = findPathToRoot( rev_fkeys[ f ], root_table, updated_path_so_far, already_visisted );
			
			if( check_this_parent != null )
			{
			    return check_this_parent;
			}
		    }
		}
	    }
	}
	
	// nothing found (this is probably 'bad')
	return null;
    }

    private String concatenatePath( final String new_bit, final String old_bit )
    {
	return ( old_bit.length() > 0 ) ? ( new_bit + "." + old_bit ) : new_bit;
	
    }

    // ----------------------------------------------------------------------------------------------


    private java.util.Hashtable path_cache_ht = null;

    // find all possible paths, then choose the shortest...

    public Link[] findPathToTable( final String start_table, 
				   final String target )
    {

	// have we seen this before?
	//
	if( path_cache_ht != null )
	{
	    Link[] result = (Link[]) path_cache_ht.get( start_table + "." + target );
	    if ( result != null )
		return result;
	}
	else
	{
	    path_cache_ht = new java.util.Hashtable();
	}
	


	java.util.HashSet paths = new java.util.HashSet();

	findPathsBetweenTables( start_table, target, new Link[0], 0, paths );

	if( paths.size() == 0 )
	{
	    //System.out.println( "no path from " + start_table + " to " + target );
	    return null;
	}

	Iterator it = paths.iterator();
	boolean first = true;
	Link[] shortest_path = null;

	//System.out.println( paths.size() + " path(s) from " + start_table + " to " + target );
	    
	while( it.hasNext() )
	{
	    Link[] path = (Link[]) it.next();
	    
	    //if( paths.size() < 6 )
	    //	System.out.println( "   " + toString( path ) );
	    
	    if( first )
	    {
		first = false;
		shortest_path = path;
	    }
	    else
	    {
		if( path.length < shortest_path.length )
		    shortest_path = path;
	    }
	}

	//if( paths.size() > 1 ) 
	//    System.out.println( "shortest is : " + toString( shortest_path ) );


	// and save this result for next time....
	path_cache_ht.put( start_table + "." + target, shortest_path );


	return shortest_path;
    }


    private void findPathsBetweenTables( final String table, 
					 final String target, 
					 final Link[] path,
					 final int depth,
					 final java.util.HashSet paths )
    {

//	System.out.print( depth + ":" + table + "-->" + target + " :" );
//
//	for( int p=0; p < path.length; p++ )
//	    System.out.print( path[p].toString() );
//	System.out.println( "(" + path.length + ")" );



 	String[] fkeys = getForeignKeys( table );

	if( fkeys != null )
	{
//	    System.out.println( depth + ": checking forward links for " + table  );

	    for( int f=0; f < fkeys.length; f++ )
	    {
		if( tableCanBeUsedDuringFindLinked( fkeys[ f ] ) ) // avoid certain tables (i.e. those which don't contain instances)
		{
		    // have we already passed through this node?
		    if( notInPath( fkeys[ f ], path ) )
		    {
			final Link this_node = new Link( fkeys[f], false, tableHasRecursiveLink( fkeys[f] ) );
			final Link[] path_to_this_node = append( this_node, path );
			
			if( fkeys[ f ].equals( target ) ) // direct result!
			{
			    paths.add( path_to_this_node );
			}
			else
			{
			    // not a direct connection, try following other paths from this connection
			    
			    findPathsBetweenTables( fkeys[ f ], target, path_to_this_node, depth+1, paths );
			}
		    }
		}
	    }
	}

//	System.out.println( depth + ": checked all forward links"  );

	// now check any reverse links to this table

	String[] rev_fkeys = getReversedForeignKeys( table );
	
	if( rev_fkeys != null )
	{
//	    System.out.println( depth + ": checking reverse links for " + table  );

	    for( int f=0; f < rev_fkeys.length; f++ )
	    {
		if( tableCanBeUsedDuringFindLinked( rev_fkeys[ f ] ) ) // avoid certain tables (i.e. those which don't contain instances)
		{
		    // have we already passed through this node?
		    if( notInPath( rev_fkeys[ f ], path ) )
		    {
			final Link this_node = new Link( rev_fkeys[f], true, tableHasRecursiveLink( rev_fkeys[f] ) );
			final Link[] path_to_this_node = append( this_node, path );
			
			if( rev_fkeys[ f ].equals( target ) ) // direct result!
			{
			    paths.add( path_to_this_node );
			}
			else
			{
			    // not a direct connection, try following other paths from this connection
			    
			findPathsBetweenTables( rev_fkeys[ f ], target, path_to_this_node, depth+1, paths );
			}
		    }
		}
	    }
	}

//	System.out.println( depth + ": checked all links..."  ); 
    }



    private boolean notInPath( String table, Link[] path )
    {
	for(int p=0; p < path.length; p++ )
	    if( path[ p ].table_name.equals( table ) )
		return false;
	return true;
    }


    private Link[] append( Link s, Link[] sa )
    {
	Link[] new_sa = new Link[ sa.length + 1];
	for(int c=0; c < sa.length; c++ )
	    new_sa[ c ] = sa[ c ];
        new_sa[ sa.length ] = s;
	return new_sa;
    }

    private Link[] prepend( Link s, Link[] sa )
    {
	Link[] new_sa = new Link[ sa.length + 1];
	new_sa[ 0 ] = s;
	for(int c=0; c < sa.length; c++ )
	    new_sa[ c+1 ] = sa[ c ];
	return new_sa;
    }

    private Link[] tail( Link[] sa )
    {
	if(sa == null)
	    return null;
	if(sa.length == 0)
	    return null;
	Link[] new_sa = new Link[ sa.length - 1 ];
	for(int c=0; c < (sa.length - 1); c++ )
	    new_sa[ c ] = sa[ c + 1 ];
	return new_sa;
    }

    private String toString( Link[] sa )
    {
	StringBuffer sbuf = new StringBuffer();
	for(int c=0; c < sa.length; c++ )
	{
	    if( c > 0)
		sbuf.append( "." );
	    sbuf.append( sa[ c ] );
	}
	return sbuf.toString();
    }

    //
    // the ButtonAndProgressBar is passed in so that the state of the 'cancel' button can be checked to see
    // if the user has tried to abort the operation...
    //
    private Instance[] retrieveLinkedInstances( final String start_table_name, 
						final Instance inst,  
						Link[] path )
    {

	Instance[] insts = new Instance[ 1 ];
	insts[0] = inst;

	String current = start_table_name; 

	if( tableHasRecursiveLink( start_table_name ) )
	{
	    if( debug_find_linked )
		System.out.println(" retrieveLinkedInstances(): start table has a recursive link, expanding....");

	    insts = expandRecursiveLinks( insts, start_table_name );
	}

	while( path != null )
	{
	    if( path.length > 0 )
	    {
		Link path_head = path[ 0 ];
		
		//mlabel.setText( "Checking " + path_head.table_name + "..." );
	    
		if( path_head.is_reverse )
		{
		    insts = retrieveReverseLinkedInstances( insts, current, path_head );
		    
		    if( path_head.is_recursive )
		    {
			if( debug_find_linked )
			    System.out.println(" retrieveLinkedInstances(): checking reversed recursive links in " + path_head.table_name );

			insts = expandReverseRecursiveLinks( insts, path_head.table_name );
		    }
		}
		else
		{
		    insts = retrieveLinkedInstances( insts, current, path_head );

		    if( path_head.is_recursive )
		    {
			if( debug_find_linked )
			    System.out.println(" retrieveLinkedInstances(): checking forward recursive links in " + path_head.table_name );
			
			insts = expandRecursiveLinks( insts, path_head.table_name );
		    }
		}

		// check periodically for a user 'cancel' 


		insts = makeInstancesUnique( insts );

		current = path_head.table_name;

		try
		{
		    Thread.sleep(5);
		}
		catch( InterruptedException ie )
		{
		}

	    }

	    path = tail( path );
	}
 
	if( ( insts == null ) || ( insts.length == 0 ) )
	{
	    if( debug_find_linked )
		System.out.println(" retrieveLinkedInstances(): nothing found");
	    return null;
	}
	else
	{
	    if( debug_find_linked )
		System.out.println(" retrieveLinkedInstances(): " + insts.length + " things found");
	    return insts;
	}
    }


    //
    // the ButtonAndProgressBar is passed in so that the state of the 'cancel' button can be checked to see
    // if the user has tried to abort the operation...
    //
    private final Instance[] retrieveLinkedInstances( final Instance[] insts, 
						      final String from_table, 
						      final Link link )
    {
	if(( insts == null ) || ( insts.length == 0 ))
	    return null;

	String to_table = link.table_name;

	Vector insts_v = new Vector();

	switch( getLinkType( from_table, to_table ) )
	{
	case Controller.OneToManyLink:
		
	    if( debug_find_linked )
		System.out.println("  from=" + from_table + " to " + to_table + " via a List, " + 
				   "starting with " + insts.length + " instances" );

	    /*
                e.g.  from Hybridisation to LabelledExtract (via LabelledExtractList)
	    
	    	    foreach Hybridisation 'h'
		       get 'lel', the LabelledExtractList linked to 'h'
		       get the LabelledExtracts in 'lel'

	    */

	    String link_table_name     = getLinkTableName( from_table, to_table );        // LabelledExtractList
	    String instance_fk_name    = getForeignKeyField( link_table_name, to_table ); // Labelled_Extract_ID
	    String instance_table_name = to_table;                                              // LabelledExtract

	    String link_fk_name        = getForeignKeyField( from_table, to_table );      // Labelled_Extract_List_ID


	    // System.out.println("   ltn=" + link_table_name );
	    // System.out.println("   itn="  + instance_table_name + "\nifn=" + instance_fk_name);
	    // System.out.println("   lfn=" + link_fk_name );

	    
	    for(int i=0; i < insts.length; i++ )
	    {


		String link_fk_value = mconn.getFieldFromID( insts[i].id, link_fk_name, from_table );
		
		Instance[] insts_2 = mconn.retrieveInstanceList( link_fk_value,
								 link_table_name, 
								 instance_fk_name, 
								 instance_table_name );
		if( insts_2.length > 0 )
		{
		    //System.out.println("     " + insts_2.length + " found");

//                  int things_to_print = insts_2.length;
//			things_to_print = things_to_print > 20 ? 20 : things_to_print;
//		    for(int i2=0; i2 < things_to_print; i2++)
//			System.out.println("       " + insts_2[i2].name );
		}
		
		for(int i2=0; i2 < insts_2.length; i2++)
		    insts_v.add( insts_2[ i2 ] );

/*
		try
		{
		    Thread.sleep(5);
		}
		catch( InterruptedException ie )
		{
		}
*/
	    }
	    break;

	case Controller.OneToOneLink:

	    String fk_field = getForeignKeyField( from_table, to_table );
	    
	    if( debug_find_linked )
		System.out.println("  from=" + from_table + " to " + to_table + " using " + fk_field + 
				   ", starting with " + insts.length + " instances" );
	    
	    for(int i=0; i < insts.length; i++ )
	    {



		String fk_id = mconn.getFieldFromID( insts[i].id, fk_field, from_table );

		Instance[] insts_2 = mconn.getInstancesUsingFKField( fk_id, "ID", to_table );
		
		if( insts_2 != null )
		{
		    if( insts_2.length > 0 )
		    {
			//System.out.println("     " + insts_2.length + " found");

//			int things_to_print = insts_2.length;
//			things_to_print = things_to_print > 20 ? 20 : things_to_print;
//			for(int i2=0; i2 < things_to_print; i2++)
//			    System.out.println("       " + insts_2[i2].name );
		    }
		    
		    for(int i2=0; i2 < insts_2.length; i2++)
			insts_v.add( insts_2[ i2 ] );

/*
		    try
		    {
			Thread.sleep(5);
		    }
		    catch( InterruptedException ie )
		    {
		    }
*/
		}

	    }
	    break;

	case Controller.RecursiveLink:
	    // handled elsewhere
	    
	    break;
	}
	
	return (Instance[]) insts_v.toArray( new Instance[0] );
    }


    //
    // the ButtonAndProgressBar is passed in so that the state of the 'cancel' button can be checked to see
    // if the user has tried to abort the operation...
    //

    private Instance[] retrieveReverseLinkedInstances( final Instance[] insts, 
						       final String from_table, 
						       final Link link )
    {
	if(( insts == null ) || ( insts.length == 0 ))
	    return null;

	String to_table = link.table_name;

	Vector insts_v = new Vector();

	if( getLinkType( to_table, from_table ) == Controller.OneToManyLink )
	{
	    if( debug_find_linked )
		System.out.println("  reversed link from=" + from_table + " to " + to_table + " via a List," +
				   " starting with " + insts.length + " instances" );

	    /*
	      e.g. from LabelledExtract to Hybridisation (via LabelledExtractList)
	      
	      foreach LabelledExtract 'le'
		          get 'lel', the LabelledExtractLists which include 'le'
			  foreach LabelledExtractList 'leli' in 'lel'
			     get the Hybridisations which link to 'leli'

			     
                    
	    */

	    String link_table_name = getLinkTableName( to_table, from_table  );
	    String link_fk_field   = getForeignKeyField( link_table_name, from_table  );

	    // System.out.println("   ltn=" + link_table_name + " lff=" + link_fk_field );

	    for(int i=0; i < insts.length; i++ )
	    {



		Instance[] insts_2 = mconn.retrieveInstanceListsContainingSomeInstance( insts[i].id, 
											link_fk_field,
											link_table_name );

		if( insts_2 != null )
		{
		    String fk_field =  getForeignKeyField( to_table, from_table );
		    
		    //System.out.println("   " + insts_2.length + " instance lists found");
		    //System.out.println("   fkf=" + fk_field );

		    for(int i2=0; i2 < insts_2.length; i2++ )
		    {


			//System.out.println("   checking id=" + insts_2[i2].id );
		    
			Instance[] insts_3 = mconn.getInstancesUsingFKField( insts_2[ i2 ].id, fk_field, to_table );
			
			if( insts_3 != null )
			{
			    //System.out.println("   " + insts_3.length + " things found using this instance list");
		    
			     for(int i3=0; i3 < insts_3.length; i3++)
				 insts_v.add( insts_3[ i3 ] );
			}
/*
			try
			{
			    Thread.sleep(5);
			}
			catch( InterruptedException ie )
			{
			}
*/
		    }
		}
	    }
	}
	else
	{
	    String fk_field = getForeignKeyField( to_table, from_table  );
	    
	    if( debug_find_linked )
		System.out.println("  reversed link from=" + from_table + " to " + to_table + " using " + fk_field +
				   " starting with " + insts.length + " instances" );
	    
	    for(int i=0; i < insts.length; i++ )
	    {


		Instance[] insts_2 = mconn.getInstancesUsingFKField( insts[i].id, fk_field, to_table );
		
		if( insts_2 != null )
		{
		    if( insts_2.length > 0 )
		    {
			//System.out.println("     " + insts_2.length + " found");

//			int things_to_print = insts_2.length;
//			things_to_print = things_to_print > 20 ? 20 : things_to_print;
//			for(int i2=0; i2 < things_to_print; i2++)
//			    System.out.println("       " + insts_2[i2].name );
		    }
		    
		    for(int i2=0; i2 < insts_2.length; i2++)
			insts_v.add( insts_2[ i2 ] );

/*
		    try
		    {
			Thread.sleep(5);
		    }
		    catch( InterruptedException ie )
		    {
		    }
*/
		}
	    }
	}
	
	return (Instance[]) insts_v.toArray( new Instance[0] );
  
    }


    //
    // this method is used when finding links along paths which have recursively linked tables,
    // for example Extract->TreatedSample->TreatedSample->Sample
    //
    // given a input array of instances in the recursively linked table, it produces a
    // an output array of instances which comprises all of the the input instances and any instances
    // which are recursively linked to any of the input instances
    //
    private Instance[] expandRecursiveLinks( final Instance[] insts, final String table_name )
    {
	final java.util.Vector output_v = new java.util.Vector();

	if( insts != null )
	{
	    for( int i=0; i < insts.length; i++)
	    {
		output_v.addElement( insts[ i ] );
		expandRecursiveLinks( insts[ i ], table_name, output_v );
	    }
	}
	
	if( debug_find_linked )
	    System.out.println("expandRecursiveLinks(): " + table_name + " : " + insts.length + " inputs result in " + output_v.size() + " outputs" );
	
	return ( Instance[] ) output_v.toArray( new Instance[ output_v.size() ] );
    }

    private void expandRecursiveLinks( final Instance inst, final String table_name, final java.util.Vector result_v )
    {
	String link_fk_field = getForeignKeyField( table_name, table_name  );

	String link_fk_value = mconn.getFieldFromID( inst.id, link_fk_field, table_name );
	
	if( debug_find_linked )
	    System.out.println("expandRecursiveLinks(): " + table_name + ":" + inst.name + " value in " + link_fk_field + " is " + link_fk_value );

	if( link_fk_value != null )
	{
	    Instance linked_inst =  mconn.getInstanceUsingID( link_fk_value, table_name );
	    
	    if( linked_inst != null )
	    {
		result_v.add( linked_inst );

		if( debug_find_linked )
		    System.out.println("expandRecursiveLinks(): " + table_name + " : " + inst.name + " recursively linked to " + linked_inst.name );

		// and now do the same thing again with the newly found instance
		
		expandRecursiveLinks( linked_inst, table_name, result_v );
	    }
	}
    }


    //
    // this method is used when finding reversed links along paths which have recursively linked tables,
    // for example Sample->TreatedSample->TreatedSample->Extract
    //
    // given a input array of instances in the recursively linked table, it produces a
    // an output array of instances which comprises all of the the input instances and any instances
    // which are recursively reverse linked to  any of the input instances
    //
    private Instance[] expandReverseRecursiveLinks( final Instance[] insts, final String table_name )
    {
	final java.util.Vector output_v = new java.util.Vector();

	if( insts != null )
	{
	    for( int i=0; i < insts.length; i++)
	    {
		output_v.addElement( insts[ i ] );
		expandReverseRecursiveLinks( insts[ i ], table_name, output_v );
	    }
	    
	    if( debug_find_linked )
		System.out.println("expandReverseRecursiveLinks(): " + table_name + " : " + insts.length + 
				   " inputs result in " + output_v.size() + " outputs" );
	    
	    return ( Instance[] ) output_v.toArray( new Instance[ output_v.size() ] );
	}
	else
	{
	    return null;
	}
    }

    private void expandReverseRecursiveLinks( final Instance inst, final String table_name, final java.util.Vector result_v )
    {
	String link_fk_field = getForeignKeyField( table_name, table_name  );

	// get all of the instances which recursively link to this instance 

	Instance[] linked_insts = mconn.getInstancesUsingFKField( inst.id, link_fk_field, table_name );

	if( linked_insts != null )
	{
	    if( debug_find_linked )
		System.out.println("expandReverseRecursiveLinks(): " + table_name + ":" + inst.name + 
				   " is recursively linked to by " + linked_insts.length + " instances");

	    for( int i=0; i < linked_insts.length; i++)
	    {
		result_v.add( linked_insts[ i ] );

		if( debug_find_linked )
		    System.out.println("expandReverseRecursiveLinks(): " + table_name + " : " + inst.name + 
				       " recursively linked to by " + linked_insts[ i ].name );

		// and now do the same thing again with the newly found instance
		
		expandReverseRecursiveLinks( linked_insts[ i ], table_name, result_v );
	    }
	}
    }



    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --


    public String evaluateDefaultAttributeValue( String val )
    {
	if(val == null)
	    return val;

	if(val.indexOf("$") < 0)
	    return val;

	while( val.indexOf("$") >= 0 )
	{
	    // System.out.println("val=" + val);

	    boolean matched = false;

	    if( ! matched )
		if( val.indexOf("$USER") >= 0 )
		{
		    val = replaceDefaultAttributeTerm( val, "$USER", cman.getAccount() );
		    
		    matched = true;
		}

	    if( ! matched )
		if( val.indexOf("$TIME") >= 0 )
		{
		    String time_now = java.text.DateFormat.getTimeInstance().format( new java.util.Date() );
		    
		    val = replaceDefaultAttributeTerm( val, "$TIME", time_now );
		    
		    matched = true;
		}

	    if( ! matched )
		if( val.indexOf("$DATE.MONTH_NAME") >= 0 )
		{
		    String date_now = java.text.DateFormat.getDateInstance().format( new java.util.Date() );
		    
		    val = replaceDefaultAttributeTerm( val, "$DATE.MONTHNAME", date_now );
		    
		    matched = true;
		}

	    if( ! matched )
		if( val.indexOf("$DATE.MONTH") >= 0 )
		{
		    java.util.Calendar cal = Calendar.getInstance();
		    int number = cal.get( java.util.Calendar.MONTH );
		    
		    val = replaceDefaultAttributeTerm( val, "$DATE.MONTH", String.valueOf( number+1 ) );
		    
		    matched = true;
		}

	    if( ! matched )
		if( val.indexOf("$DATE.YEAR") >= 0 )
		{
		    java.util.Calendar cal = Calendar.getInstance();
		    int number = cal.get( java.util.Calendar.YEAR );
		    
		    val = replaceDefaultAttributeTerm( val, "$DATE.YEAR", String.valueOf( number ) );
		    
		    matched = true;
		}

	    if( ! matched )
		if( val.indexOf("$DATE.DAY_OF_MONTH") >= 0 )
		{
		    java.util.Calendar cal = Calendar.getInstance();
		    int number = cal.get( java.util.Calendar.DAY_OF_MONTH );
		    
		    val = replaceDefaultAttributeTerm( val, "$DATE.DAY_OF_MONTH", String.valueOf( number ) );
		    
		    matched = true;
		}

	    if( ! matched )
		if( val.indexOf("$DATE") >= 0 )
		{
		    String date_now = java.text.DateFormat.getDateInstance().format( new java.util.Date() );
		    
		    val = replaceDefaultAttributeTerm( val, "$DATE", date_now );
		    
		    matched = true;
		}
	
	    if(!matched)
	    {
		// the string contains a variable which does not match
		// any of the supported names; give up...
		
		return val;
	    }
	}

	return val;
    }

    private String replaceDefaultAttributeTerm( String input, String search, String replace )
    {
	final int pos = input.indexOf(search);

	final int endpos = pos + search.length();

	if(pos < 0)
	    return input;

	String pre = (pos > 0) ? input.substring(0,pos) : "";

	String post = (endpos < input.length()) ? input.substring(endpos) : "";
	
	return pre + replace + post;
    }

    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

    /* 
        'attr_desc' is the table-specific AttrDesc[]

	'values' should contain 1 or more name:value pairs.

            For tuples; the name should match one of the names in 'attr_desc'

	    For triples; names should match one of the names in 'attr_desc' suffixed with ".1" and ".2",
	            e.g. 'Mass.1':'100' , 'Mass.2':'Kg'
     */


/*
    public String storeAttributes( AttrDesc attr_desc, Hashtable values )
    {
	String attr_id = null;
	
	String value_str = attr_desc.serialiseValues( values );

	if( value_str != null )
	{
	    attr_id = mconn.generateDescriptionID( value_str );
	}

	return attr_id;
    }
*/


    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --


    public void doCommit()
    {
	if( mconn.executeStatement("COMMIT") == false)
	    System.err.println("Unable to execute COMMIT statement");
    }



    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    //
    //  e r r o r    r e p o r t i n g
    //
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --


    public void generateErrorReport( Throwable t )
    {
	try
	{
	    t.printStackTrace();

	    PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter( "error.log"  ) ), true);
	    
	    pw.println( "maxdView: LoadFromDatabase " );
	    
	    pw.println( "JVM: " + 
			System.getProperty("java.vm.version") +
			" from " +
			System.getProperty("java.vm.vendor") );
	    
	    pw.println("OS: " + 
		       System.getProperty("os.name") +
		       " " +
		       System.getProperty("os.version") +
		       " on " +
		       System.getProperty("os.arch"));
	    
	    pw.println("Host: " + cman.getHost() );

	    pw.println();

	    t.printStackTrace( pw );
	    
	    alertMessage( "Something bad has happened !\n\n" + 
			  "The error message was\n  " + t.toString() + "\n\n" + 
			  "An error log file has been created in a file called 'error.log'\n\n" + 
			  "Please forward this file to 'maxd_bugs@cs.man.ac.uk'");
	    
	}
	catch( Exception e )
	{
	    System.out.println("WARNING: unable to create an error log file");

	    t.printStackTrace( );

	    alertMessage( "Something bad has happened !\n\n" + 
			  "The error message was\n  " + t.toString() + "\n\n" + 
			  "It was not possible to create an error log." );
	    
	}
	
    }


    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

    public Font huge_font = null;
    public Font huge_bold_font = null;
    public Font big_font = null;
    public Font big_bold_font = null;
    public Font small_font = null;
    public Font small_bold_font = null;

    public Font field_font = null;
    public Font label_font = null;
    public Font underlined_label_font = null;
    public Font info_font  = null;

    public Color label_colour           = new Color( 33,3,3 );
    public Color form_background_colour = new Color( 187,200,183 );

    public void updateFonts()
    {
	//System.out.println("browse_list_font_size = " +      getIntProperty( "interface.browse_list_font_size", 12 ) );
	//System.out.println("label_field_font_size = " +      getIntProperty( "interface.label_field_font_size", 12 ) );
	//System.out.println("data_entry_field_font_size = " + getIntProperty( "interface.data_entry_field_font_size", 12 ) );

	label_colour           = new Color ( getIntProperty( "interface.label_field_font_colour",  Color.black.getRGB() ) );

	form_background_colour = new Color ( getIntProperty( "interface.form_background_colour",   Color.white.getRGB() ) );
	
	try
	{
	    Font f = getGraphics().getFont();

	    int label_font_size = getIntProperty( "interface.label_field_font_size",      f.getSize() );
	    int field_font_size = getIntProperty( "interface.data_entry_field_font_size", f.getSize() );
		
	    field_font            = new Font( f.getName(), f.getStyle(), field_font_size );
	    label_font            = new Font( f.getName(), f.getStyle(), label_font_size );
	    underlined_label_font = new Font( f.getName(), f.getStyle(), label_font_size );
	    info_font             = new Font( f.getName(), Font.ITALIC, label_font_size );
	}
	catch( NullPointerException npe )
	{
	    // System.err.println("updateFonts(): " + npe.toString() );
	}
    }

    public void getDifferentFonts()
    {
	Graphics g = getGraphics();

	if(g != null)
	{
	    Font f = g.getFont();
	    if(f != null)
	    {
		huge_font       = new Font( f.getName(), f.getStyle(), f.getSize() + 4);
		huge_bold_font  = new Font( f.getName(), Font.BOLD,    f.getSize() + 4);
		big_font        = new Font( f.getName(), f.getStyle(), f.getSize() + 2);
		big_bold_font   = new Font( f.getName(), Font.BOLD,    f.getSize() + 2);
		small_font      = new Font( f.getName(), f.getStyle(), f.getSize() - 2);
		small_bold_font = new Font( f.getName(), Font.BOLD,    f.getSize() - 2);

		
		field_font = new JLabel().getFont();
		label_font = new JLabel().getFont();

		updateFonts();
	    }
	    else
	    {
		System.err.println("Warning: Unable to get hold of the default font");
	    }

	}
	else
	{
	    System.err.println("Warning: Unable to get a Graphics object from which to determine the default font");
	}
    }
    
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --


    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --



    // expects strings in the form: 1[ 2][ 3][ 4]...
    //  returns an array of integers, using -1 to signal 'illegal'
    //
    //
    //
    //
    public int[] parseIndexList( final String str )
    {
	if((str == null) || (str.length() == 0))
	   return null;
	   
	Vector int_v = new Vector();
	StringTokenizer st = new StringTokenizer( str );
	while (st.hasMoreTokens()) 
	{
	    String tok = st.nextToken();

	    if(!tok.startsWith("$"))
	    {
		try
		{
		    Integer i = new Integer( tok );
		    int_v.addElement(i);
		}
		catch(NumberFormatException nfe)
		{
		    int_v.addElement( new Integer(-1) );
		}
	    }
	}

	if(int_v.size() == 0)
	    return null;

	int[] res = new int[ int_v.size() ];
	for(int i=0; i < int_v.size(); i++)
	    res[i] = (((Integer) int_v.elementAt(i)).intValue()) - 1;
	
	// System.out.println(str + " -> " + res);

	return res;

    } 
	
    public Character parseJoinChar(String str)
    {
	if((str == null) || (str.length() == 0))
	   return null;

	int i = str.indexOf('$');

	if(i < 0)
	    return null;

	if((i+1) < str.length())
	    return new Character(str.charAt(i+1));
	else
	    return null;
    }

    public String integerListToString( int[] ilist )
    {
	return integerListToString( ilist, null );
    }

    public String integerListToString( int[] ilist, Character ch )
    {
	String res = "";
	for(int i=0; i < ilist.length; i++)
	{
	    if((i > 0) && (ch != null))
		res += ch;
	    res += String.valueOf(ilist[i]);
	}
	return res;
    }

    public int getColumn(String c)
    {
	try
	{
	    return (new Integer(c)).intValue();
	}
	catch(NumberFormatException nfe)
	{
	    return -1;
	}
    }


    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

    private ImageIcon info_icon = null;
    private ImageIcon input_icon = null;
    private ImageIcon error_icon = null;
    private ImageIcon success_icon = null;

    private Image small_logo_image = null;
 
    private ImageIcon button_icon         = null;
    private ImageIcon pressed_button_icon = null;

    public void setGeometry( String geom_descr_str )
    {
	if( geom_descr_str == null )
	    return;

	// format is as that of X Window, eg  -g WxH+X+Y
	
	int d1 = geom_descr_str.indexOf('x');
	int d2 = geom_descr_str.indexOf('+');
	int d3 = (d2 >= 0) ? geom_descr_str.indexOf('+',d2+1) : -1;

	//System.out.println( geom_descr_str + " d1=" + d1 + " d2=" + d2 + " d3=" + d3 );

	int x = -1;
	int y = -1;
	int w = -1;
	int h = -1;

	if(d2 >=0)
	{
	    if(d1 >= 0)
	    {
		w = parseInt( geom_descr_str.substring(0,d1) );
		h = parseInt( geom_descr_str.substring(d1+1,d2) );
	    }

	    if(d3 >= 0)
	    {
		x = parseInt( geom_descr_str.substring(d2+1,d3) );
		y = parseInt( geom_descr_str.substring(d3+1) );
	    }
	    else
	    {
		x = parseInt( geom_descr_str.substring(d2+1) );
	    }
	}
	else
	{
	    if(d1 >= 0)
	    {
		w = parseInt( geom_descr_str.substring(0,d1) );
		h = parseInt( geom_descr_str.substring(d1+1) );
	    }
	}

	// System.out.println( " x=" + x + " y=" + y+ " w=" + w + " h=" + h );

	if(( w > 0 ) && ( h > 0 ))
	{
	    window_width = w;
	    window_height = h;

	    pref_window_size = new Dimension( window_width, window_height );
	}

	if(( x > 0 ) && ( y > 0 ))
	{
	    window_pos_x = x;
	    window_pos_y = y;

	    parent_frame.setLocation(  window_pos_x, window_pos_y );
	}
    }
    private int parseInt( String str )
    {
	try
	{
	    return Integer.valueOf( str ).intValue();
	}
	catch( NumberFormatException fe )
	{
	    return -1;
	}
    }



    public String[] splitLongLineOfText( final String line, final int max_line_length )
    {
	if( line.length() < max_line_length )
	{
	    String[] result = new String[1];
	    result[0] = line;
	    return result;
	}
	
	Vector tokens = new Vector();
	StringTokenizer st = new StringTokenizer( line, " " );

	while (st.hasMoreTokens()) 
	{
	    tokens.add( st.nextToken() );
	}
	
	Vector lines = new Vector();
	int current_line_length = 0;
	StringBuffer current_line = new StringBuffer();
	int token = 0;
	while( token < tokens.size() )
	{
	    String current_token = (String) tokens.elementAt( token );
	    int current_token_length = current_token.length();

	    if( current_token_length > max_line_length ) 
	    {
		// special case if the token is longer than the max line length
		// store the current line, add the over-long token as a line of it's own,
		// and create a new empty line ready for the next token

		lines.add( current_line.toString() );
		lines.add( current_token );
		current_line = new StringBuffer();
		current_line_length = 0;
	    }
	    else
	    {
		
		if( ( current_line_length + current_token_length ) < max_line_length )
		{
		    // add this token to the current line
		    
		    if( current_line_length > 0 )
		    {
			// a bit naughty: sometimes this will result in lines that are 1 character longer longer than max_line_length....

			current_line.append( " " );
			current_line_length++;
		    }

		    current_line.append( current_token );
		    current_line_length += current_token_length;
		}
		else
		{
		    // save the current line

		    lines.add( current_line.toString() );

		    // create a new line and add the current token to it

		    current_line = new StringBuffer();
		    current_line.append( current_token );
		    current_line_length = current_token_length;
		}
	    }
	    
	    token++;
	}

	if( current_line_length > 0 )
	    lines.add( current_line.toString() );

	return (String[]) lines.toArray( new String[ lines.size() ] );
    }


/*
    public void addPopupToTextComponent( Component comp )
    {
	if( comp instanceof javax.swing.text.JTextComponent )
	    comp.addMouseListener( new TextFieldPopup( (javax.swing.text.JTextComponent) comp ) );
	else
	    System.err.println( "Warning: unable to add popup to non-text componnent" );
    }
*/

/*
    public void decorateFrame( JFrame a_frame )
    {
	if( small_logo_image == null )
	{
	    ImageIcon ii = getImageIconFromJAR( "images/small-logo.gif" );
	    if( ii != null )
		small_logo_image = ii.getImage();
	}

	if( small_logo_image != null )
	{
	    a_frame.setIconImage( small_logo_image );
	}
    }
*/
/*
    public void decorateButton( JButton jb )
    {
	if(button_icon == null)
	    button_icon = getImageIconFromJAR("images/button.gif");
	
	if(pressed_button_icon == null)
	    pressed_button_icon = getImageIconFromJAR("images/button-pressed.gif");
	
	jb.setIcon( button_icon );
	jb.setPressedIcon( pressed_button_icon);
	
	jb.setHorizontalTextPosition( SwingConstants.CENTER );
	jb.setVerticalTextPosition( SwingConstants.CENTER );
	jb.setMargin( new Insets(0,0,0,0) );
	jb.setBorderPainted( false);
    }
*/
/*
    public ImageIcon getDefaultButtonBackground()
    {
	if(button_icon == null)
	    button_icon = getImageIconFromJAR("images/button.gif");
	return button_icon;
    }
*/

    public void infoMessage(String str)  {  mview.infoMessage( str ); }

    public void alertMessage(String str) { mview.alertMessage( str ); }

    public void successMessage(String str) { mview.successMessage( str ); }

    public void errorMessage(String str) { mview.errorMessage( str ); }


    public void addFiller( final int size_i, final JPanel panel, final GridBagLayout bag, final int row, final int col )
    {
	Dimension size = new Dimension( size_i, size_i );
	Box.Filler filler = filler = new Box.Filler(size, size, size);
	GridBagConstraints c = new GridBagConstraints();
	c.gridx = col;
	c.gridy = row;
	bag.setConstraints(filler, c);
	panel.add(filler);
    }




    public Color getSimilarColour( Color source )
    {
	int r = nearbyIntensity( source.getRed() );
	int g = nearbyIntensity( source.getGreen() );
	int b = nearbyIntensity( source.getBlue() );

	return new Color( r,g,b );
    }

    private int nearbyIntensity( int i )
    {
	return ( i < 128 ) ? ( i + 48 ) : ( i - 48 );
    }

    /*
    public Color getDisimilarColour( Color source )
    {
	int r = farawayIntensity( source.getRed() );
	int g = farawayIntensity( source.getGreen() );
	int b = farawayIntensity( source.getBlue() );
 
	return new Color( r,g,b );
   }
    */

    private int farawayIntensity( int i )
    {
	// return ( i < 128 ) ? ( i + 128 ) : ( i - 128 );
	return (255 - i);
    }

   // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

    public long secondsSince(java.util.Date start_time)
    {
	java.util.Date stop_time = new java.util.Date();
	long elapsed =  stop_time.getTime() - start_time.getTime();
	return (elapsed / 1000);
    }
    
    public String niceFloat(float fl, int dp)
    {
	String s = String.valueOf(fl);
	int dpt = s.indexOf('.');
	if(dpt >= 0)
	{
	    if((s.length() - dpt) > (dp+2))
	    {
		s = s.substring(0, dpt+2);
	    }
	}
	return s;
    }

    public String niceTime(long seconds)
    {
	long hrs = seconds / (60 * 60);
	seconds -= (hrs * (60 * 60));
	long mins = seconds / 60;
	seconds -= (mins * 60);

	if((hrs == 0) && (mins == 0))
	{
	    return seconds + "s";
	}
	/*
	if(hrs > 0)
	{
	    return (hrs + "h " + (mins<10 ? "0" : "")  + mins + "m");
	}
	else
	{
	    return ((mins<10 ? "0" : "") + mins + "m " + (seconds<10 ? "0" : "") + seconds + "s");
	}
	*/
	
	return hrs + ":" + (mins<10 ? "0" : "") + mins + ":" + (seconds<10 ? "0" : "") + seconds;

    }
    
    public String performanceData(int done, int expected, java.util.Date start_time)
    {
	return "boo";
    }

    public String maybePlural(long count, String word)
    {
	return (count == 1) ? word : (word + "s");
    }

    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

    void startBatch()
    {
	
	mconn.setAutoCommit(false);
	
    }

    void endBatch()
    {
	System.out.println( "Information: commiting any pending transactions");

	mconn.commit();

	mconn.setAutoCommit( getBooleanProperty( "database.auto_commit", true ) );
    }

    void cancelBatch()
    {
	System.out.println( "Information: rolling back any uncommited transactions");

	mconn.rollback();

	mconn.setAutoCommit( getBooleanProperty( "database.auto_commit", true ) );
    }
    
    public synchronized void disconnect()
    {
	if( mconn != null )
	    mconn.disconnect();

	//setupConnectionActions();

	//returnToPreviousPanel();
    }

    public synchronized int attemptConnect()
    {
	mconn = new maxdConnection_m2( Controller.this, "2.1" );
	
	if( cman == null )
	{
	    mview.alertMessage( "No ConnectionManager available" );
	    return -1;
	}

	return mconn.attemptConnect( cman.getHost(), 
				     cman.getDriverFile(), 
				     cman.getDriverName(), 
				     cman.getAccount(), 
				     cman.getPassword() );

    }

/*
    public synchronized void attemptConnect()
    {
	if(debug_driver)
	{
	    System.err.println("attemptConnect(): driver debugging mode enabled");
	}

	if(debug_connect)
	{
	    System.err.println("attemptConnect(): connection debugging mode enabled");
	}

	
	cman.resetStatus();


	final SwingWorker worker = new SwingWorker() 
	    {
		int result = -1;
		
		public Object construct() 
		{
		    mconn = new maxdConnection_m2( Controller.this, "2.1" );
		    
		    mconn.testTextEscaping();

		    result = mconn.attemptConnect( cman.getHost(), 
						   cman.getDriverFile(), 
						   cman.getDriverName(), 
						   cman.getAccount(), 
						   cman.getPassword() );
		    
		    // System.out.println( "Connection result is " + result );

		    return "done";
		}

		public void finished() 
		{
		    
		    if(result > 0)
		    {
			// check that the database version is up to date....
			
			
			mconn.setAutoCommit( getBooleanProperty("database.auto_commit", false ) );
			
			//updateNavPanelWindowingState();

		    }
		}
	    };

	
	worker.start();  //required for SwingWorker 3
    }
*/

/*
    public synchronized void attemptConnect()
    {
	   
	mconn = new maxdConnection( this, "2.0" );
	
	int result = mconn.attemptConnect( cman.getHost(), 
					   cman.getDriverFile(), cman.getDriverName(), 
					   cman.getAccount(), cman.getPassword() );

	if(result > 0)
	{
	    if(top_menu_panel == null)
	    {
		top_menu_panel = createTopMenuPanel();
	    }

	    
	    addPanelToStack("Top Level", top_menu_panel);

	    mconn.setAutoCommit( getBooleanProperty("database.auto_commit", false ) );
	    
	    if( getBooleanProperty("interface.show_navigation_tree", true ) )
		showNavPanel();

	}
    }
 */
   
    // ------------------------------------------------------------------------------------------
    //
    // is there a maxdSQL database on the current host?
    //

    public boolean checkDatabaseExists( String name )
    {
	// attempt to connect to this database
	
	String sql = "CONNECT " + name ;
	
	return ((mconn != null) && (mconn.executeStatement(sql) == false));
	
    }

    // ------------------------------------------------------------------------------------------
    // ------------------------------------------------------------------------------------------

    
    private Properties application_props = new Properties();
 
    public Properties getProperties() { return application_props; }

    public String getProperty(String name)             { return application_props.getProperty(name, ""); }
    public void   putProperty(String name, String val) { application_props.put(name, val); }

    public String getProperty(String name, String def) 
    { 
	String s = application_props.getProperty(name);
	if(s == null)
	    return def;
	else
	    return s;
    }
    public int getIntProperty(String name, int def) 
    { 
	String s = application_props.getProperty(name);
	if(s == null)
	    return def;
	try
	{
	    int i = (Integer.valueOf(s)).intValue();
	    return i;
	}
	catch(NumberFormatException nfe)
	{
	    return def;
	}
    }
    public void putIntProperty(String name, int val)
    { 
	application_props.put(name, String.valueOf(val));
    }
    public double getDoubleProperty(String name, double def) 
    { 
	String s = application_props.getProperty(name);
	if(s == null)
	    return def;
	try
	{
	    return (Double.valueOf(s)).doubleValue();
	}
	catch(NumberFormatException nfe)
	{
	    return def;
	}
    }
    public void putDoubleProperty(String name, double val)
    { 
	application_props.put(name, String.valueOf(val));
    }
 
    public boolean getBooleanProperty(String name, boolean def) 
    { 
	String s = application_props.getProperty(name);
	if(s == null)
	    return def;
	return s.equals("true");
    }
    public void putBooleanProperty(String name, boolean val)
    { 
	application_props.put(name, val ? "true" : "false");
    }


    // store the current selection of the list in the named property
    public void storeListSelectionInProperty( final String name, final JList list )
    {
	try
	{
	    // convert the contents of the list to a comma-delimited string
	    StringBuffer sbuf = new StringBuffer();
	    
	    int[] indices = list.getSelectedIndices( );
	    if( indices != null )
	    {
		DefaultListModel dlm = (DefaultListModel) list.getModel();
		for( int i=0; i < indices.length; i++ )
		{
		    if( i > 0 )
			sbuf.append(",");
		    sbuf.append( dlm.get( indices[ i ] ) );
		}
	    }
	    
	    putProperty( name, sbuf.toString() );
	}
	catch( ClassCastException cce )
	{
	}
	catch( NullPointerException cce )
	{
	}
    }

/*
    // set the current selection of the list using the value in the named property
    public void setListSelectionUsingProperty( final String name, final JList list )
    {
	try
	{
	    DefaultListModel dlm = (DefaultListModel) list.getModel();
	    
	    String[] tokens = AttrDesc.tokenise( getProperty( name, "" ), ',' );
	    
	    if( tokens != null )
	    {
		int[] indices = new int[ tokens.length ];
		
		for( int i=0; i < indices.length; i++ )
		    indices[ i ] = dlm.indexOf( tokens[ i ] );
		
	    list.setSelectedIndices( indices );
	    }
	}
	catch( ClassCastException cce )
	{
	}
	catch( NullPointerException cce )
	{
	}
    }
*/

/*  
    // get (in a HashSet) a previously saved list selection from the named property
    public java.util.HashSet getListSelection( final String name )
    {
	java.util.HashSet hs = new java.util.HashSet();

	String[] tokens = AttrDesc.tokenise( getProperty( name, "" ), ',' );
	
	if( tokens != null )
	{
	    for( int t=0; t < tokens.length; t++ )
	    {
		hs.add( tokens[ t ] );
	    }
	}
	
	return hs;
    }
*/



    public void putBrowseSelection(String name, int sel)
    {
	if(sel >= 0)
	    application_props.put("browse." + name, String.valueOf(sel));
    }

    public int getBrowseSelection(String name)
    {
	String val = application_props.getProperty("browse." + name, "");
	if(val != null)
	{
	    try
	    {
		int i = (Integer.valueOf(val)).intValue();
		return i;
	    }
	    catch(NumberFormatException nfe)
	    {
		return -1;
	    }
	}
	return -1;
    }


    // ------------------------------------------------------------------------------------------
    // ------------------------------------------------------------------------------------------


    public void setTableSelection(String name, JTable table)
    {
	table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

	int i = getBrowseSelection(name);
	if(i >=0)
	{
	    ListSelectionModel lsm = table.getSelectionModel();
	    lsm.clearSelection();
	    lsm.setSelectionInterval(i, i);
	    table.setSelectionModel(lsm);
	}
    }

    // this new improved version also positions the scrollpane
    // so that the selected row can be seen
    //
    public void setTableSelection(String name, JScrollPane jsp, JTable table)
    {
	table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

	int i = getBrowseSelection(name);
	if(i >=0)
	{
	    ListSelectionModel lsm = table.getSelectionModel();
	    lsm.clearSelection();
	    lsm.setSelectionInterval(i, i);
	    table.setSelectionModel(lsm);

	    Rectangle r = table.getCellRect(i, 0, true);

	    JViewport jvp = jsp.getViewport();
	    jvp.setViewPosition(new Point(0, r.y));
	    jsp.setViewport(jvp);
	}
    }


    // ------------------------------------------------------------------------------------------
    // ------------------------------------------------------------------------------------------



    private void loadPrefs()
    {

	boolean config_found = false;

	try
	{
	    FileInputStream in = new FileInputStream( getConfigDirectory() + File.separatorChar + "maxdLoad2.config" );

	    if(in != null)
	    {
		application_props.load(in);
		in.close();
		config_found = true;
		System.out.println("Information: preferences loaded from '" + 
				   getConfigDirectory() + File.separatorChar + "maxdLoad2.config'" );
	    }
	    else
	    {
		// try loading them from the 'old' location (i.e. the installation directory)

		in = new FileInputStream( "maxdLoad.config" );
		
		if(in != null)
		{
		    application_props.load(in);
		    in.close();
		    config_found = true;
		    System.out.println("Information: preferences loaded from 'maxdLoad2.config'" );
		}
	    }
	    
	    /*
	    if( debug_startup )
	    {

		for( Enumeration en=application_props.keys(); en.hasMoreElements();  )
		{
		    String prop = (String) en.nextElement();
		    System.out.println( prop + "=" + application_props.get( prop ) );
		}

	    }
	    */
	    
	}
 	catch(java.io.IOException  ioe)
	{
	}
    }

    public String getPref( String name )
    {
	return application_props.getProperty( name );
    }



    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- loading data directly from JAR file -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

    private String jar_file_name = null;
    private boolean jar_looked_for_but_not_found = false;


    public boolean findJarFile()
    {
	if(jar_looked_for_but_not_found)
	    return false;

	// attempt to extract the jar file name from the classpath....
	// (might not be there...)
	
	String cp       = System.getProperty("java.class.path");
	String cp_delim = System.getProperty("path.separator");
	
	int lastp = 0;
	boolean ended = false;
	
	// the string "maxdLoad" might occur several times, eg in directory names
	// try to find the one matching "maxdLoad*jar"
	
	while(!ended)
	{
	    int start = cp.indexOf( "maxdLoad", lastp );
	    
	    if(start >= 0)
	    {
		int end  = cp.indexOf(cp_delim, start);
		
		String match = (end >= 0) ? cp.substring(start, end) : cp.substring(start);
		
		if(debug_startup)
		    System.out.println("findJarFile(): maybe JAR file is '" + match + "' ?");
		
		if(match.toLowerCase().endsWith(".jar"))
		{
		    // System.out.println("maybe JAR file is '" + match + "' ?");
		    
		    if( isJarFile(match) )
		    {
			jar_file_name = match;
			if(debug_startup)
			    System.out.println("findJarFile(): using Jar file '" + jar_file_name + "'");
			ended = true;
		    }
		}
		
		if(end >= 0)
		    lastp = end+1;
		else
		    ended = true;
	    }
	    else
		ended = true;
	}
	
	if(jar_file_name == null)
	{
	    
	    // search the current directory for possible JAR files
	    
	    String cdir = getSharedDirectory(); // System.getProperty( "user.dir" );
	    
	    if(debug_startup)
		System.out.println("findJarFile(): checking " + cdir + " for a possible JAR file");
	    
	    File cfile = new File(cdir);
	    String[] contents = cfile.list();
	    Vector hits = new Vector();
	    for(int c=0; c < contents.length; c++)
	    {
		String flc = contents[c].toLowerCase();
		
		if( flc.endsWith(".jar") )
		    if( flc.startsWith("maxdload2") )
		    {
			if(debug_startup)
			    System.err.println(" findJarFile():  possibly: " +  contents[c] + " ?" );
			hits.addElement( contents[c] );
		    }
	    }
	    if(hits.size() == 1)
		jar_file_name = (String) hits.elementAt(0);
	    else
	    {
		if(hits.size() == 0)
		    System.err.println("No JAR files found in current directory");
		else
		{
		    jar_looked_for_but_not_found = true;
		    System.err.println("More than 1 potential maxdLoad JAR file found...");
		    alertMessage("More than one potential maxdLoad JAR file found in\n" +
				 "the current directory, not sure which one to use.\n" + 
				 "\nmaxdLoad will work, but some features will be missing.\n" +
				 "\nEither specify the desired file in the Java classpath\n" +
				 "or ensure that there is only one maxdLoad JAR file\n" +
				 "in this directory, then restart the application.");
		    
		    return false;
		}
	    }
	    
	    if(jar_file_name == null)
	    {
		
		jar_looked_for_but_not_found = true;
		alertMessage("Couldn't find a maxdLoad JAR file to load system data from.\n" + 
			     "maxdLoad will work, but some features will be missing.\n" +
			     "Make sure the correct name is specified in the classpath and restart.\n");
		return false;
	    }
	}

	return true;
    }
    
    
    private boolean isJarFile(String test)
    {
	try
	{
	    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(test)));
	    
	    JarInputStream jis = new JarInputStream(bis);
	    
	    //if(jf == null)
	    //  System.out.println("couldn't find Jar file");
	    
	    return jis != null;
	}
	catch(java.io.IOException ioe)
	{
	    return false;
	}
    }

    // finds the file(s) in the JAR file which start with the specified prefix
    // ( used by the 'presets' mechanism to retrieve the built-in presets )
    //
    public String[] getFilesFromJarFile( String file_name_prefix )
    {
	try
	{
	    if(jar_file_name == null)
	    {
		if(!findJarFile())
		    return null;
	    }

	    BufferedInputStream bis = new BufferedInputStream( new FileInputStream( new File( jar_file_name ) ) );

	    JarInputStream jis = new JarInputStream( bis );
	    
	    Vector file_names = new Vector();

	    if(jis == null)
	    {
		jar_looked_for_but_not_found = true;
		System.out.println("getFilesFromJarFile(): couldn't create input stream");
	    }
	    
	    //System.out.println( "getFilesFromJarFile(): checking for prefix '" + file_name_prefix + "'" );

	    boolean found = false;

	    JarEntry je = null;

	    while( true )
	    {
		je = jis.getNextJarEntry();
		
		if( je != null )
		{
		    //System.out.println( "getFilesFromJarFile(): checking '" + je.getName() + "'" );
		    
		    if( isInPath( je.getName(), file_name_prefix ) )
		    {
			System.out.println( "getFilesFromJarFile(): candidate '" + je.getName() + "'" );
			
			file_names.add( je.getName() ) ;
		    }
		}
		else
		{
		    break;
		}
	    }
	    
	    System.out.println( "getFilesFromJarFile(): found " + file_names.size() + " files" );
	    
	    return (String[]) file_names.toArray( new String[ file_names.size() ] );
	}
	catch(java.io.IOException ioe)
	{
	    System.out.println("couldn't find Jar file '" + jar_file_name + "'");

	    jar_looked_for_but_not_found = true;

	    return null;
	}
    }

    private boolean isInPath( String p1, String p2 )
    {
	// normalise both paths so that the file separators are the same
	String np1 = p1.replace('\\','/');
	String np2 = p2.replace('\\','/');

	return( np1.startsWith( np2 ) );
    }


    public byte[] getBytesFromJarFile(String fname)
    {
	final int chunk_size = 1024;

	if(jar_looked_for_but_not_found == true)
	    return null;

	try
	{
	    //JarFile jf = new JarFile("maxdLoad.jar");
	    if(jar_file_name == null)
	    {
		if(!findJarFile())
		    return null;
	    }

	    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(jar_file_name)));
	    
	    JarInputStream jis = new JarInputStream(bis);
	    
	    Vector data_chunks = new Vector();


	    //if(jf == null)
	    //  System.out.println("couldn't find Jar file");

	    if(jis == null)
	    {
		jar_looked_for_but_not_found = true;
		System.out.println("getBytesFromJarFile((): couldn't create input stream");
	    }
	    
	    boolean found = false;

	    JarEntry je = null;

	    while(!found)
	    {
		je = jis.getNextJarEntry();
		
		// System.out.println(je.getName());
		
		if((je != null) && (je.getName().equals(fname)))
		{
		    found = true;
		    // System.out.println("'" + fname + "' FOUND!");
		}
		if(je == null)
		    break;
	    }

	    if(!found)
	    {
		System.out.println("getBytesFromJarFile(): couldn't find entry for '" + fname + "'");
		return null;
	    }
	    else
	    {
		// probably dont know how long the file entry is...
		// (JAR files dont appear to store the uncompressed sizes...)
		
		int len = 0;

		int total_read = 0;

		while(len != -1)
		{
		    byte data_bytes[] = new byte[ chunk_size ];

		    len = jis.read(data_bytes, 0, chunk_size);

		    if(len > 0)
		    {
			// if the amount read is shorter than the chunk size, trim the array
			//
			if(len < chunk_size)
			{
			    byte shorter_data_bytes[] = new byte[ len ];
			    for(int c=0; c < len; c++)
				shorter_data_bytes[c] = data_bytes[c];

			    data_bytes = shorter_data_bytes;
			}
			
			// and store the array for subsequent concatenation
			//
			data_chunks.addElement(data_bytes);
			total_read += len;
		    }

		    
		}

		if(total_read == 0)
		{
		    // is this an error?
		    
		    //System.out.println("getBytesFromJarFile(): couldn't read data for '" + fname + 
		    //		       "' from Jar file '" + jar_file_name + "'");

		    return null;
		}
		else
		{
		    // now concatenate each of the chunks into a single array

		    byte[] concatenated_data_bytes = new byte[total_read];
 
		    int pos = 0;
		    for(int chunk=0; chunk < data_chunks.size(); chunk++)
		    {
			byte[] data = (byte[]) data_chunks.elementAt(chunk);

			for(int b=0; b < data.length; b++)
			    concatenated_data_bytes[pos++] = data[b];
		    }

		    return concatenated_data_bytes;
		}
	    }
	}
	catch(java.io.IOException ioe)
	{
	    System.out.println("getBytesFromJarFile(): couldn't find Jar file '" + jar_file_name + "'");
	    jar_looked_for_but_not_found = true;
	    return null;
	}

    }


    public ImageIcon getImageIconFromJAR(String fname)
    {
	//System.out.println(" seeking image " + fname + "..." );

	// how do we know the name of the file? (what about version numbers???)
	byte[] data_bytes = getBytesFromJarFile(fname);

	if(data_bytes != null)
	{
	    //System.out.println("  ...found");

	    return new ImageIcon(data_bytes);
	}
	else
	{
	    //System.err.println("WARNING: image '" + fname + "' not found");

	    return null;
	}
    }


    public BufferedInputStream getFileFromJar( String fname )
    {
	byte[] data_bytes = getBytesFromJarFile(fname);

	if(data_bytes != null)
	{
	    //System.out.println("  ...found");

	    return new BufferedInputStream( new ByteArrayInputStream( data_bytes ) );
	}
	else
	{
	    //System.err.println("WARNING: file '" + fname + "' not found");

	    return null;
	}
    }

    public ClassLoader getLoaderForJAR( String jarname )
    { 
	try
	{
	    // System.out.println("getLoaderForJAR()...");
	    
	    /*
	    //java.net.URL xerces_url = this.getClass().getClassLoader().getResource( jarname );
	    
	    //java.net.URL xerces_url = new java.net.URL("file://" + jarname);

	    // an explicit path appears to work
	    //java.net.URL xerces_url = new java.net.URL("file://C:/Documents and Settings/dave/bio/maxdLoad/" + jarname);

	    // try looking in the current directory
	    //java.net.URL xerces_url = new java.net.URL("file://./" + jarname);
	    
	    //String current_dir = System.getProperty("user.dir");
	    //java.net.URL xerces_url = new java.net.URL("file://" + current_dir + File.separator + jarname);

	    //System.out.println("xerces_url=" + xerces_url);
	    
	    //java.net.URL urls[] = new java.net.URL[1];
	    //urls[0] = xerces_url;
	    */

	    // as none of these methods appear to work (and although nested JAR files might be
	    // supported in the java-embedded-server, j2se doesn't appear to like them)
	    // then a kludgy method is called for.... ;)

	    // check to see if the required JAR is in the current directory,
	    // is not, extract it from the enclosing (i.e. maxdLoad) JAR file
	    // and write it into the current directory ready for next time.


	    File jarfile = new File( jarname );

	    if(!jarfile.exists())
	    {
		extractJARfromJAR(jarname);

		jarfile = new File( jarname );

		if(!jarfile.exists())
		{
		    alertMessage("Failed to extract '" + jarname + "'");
		}
	    }
	    
	    String current_dir = System.getProperty("user.dir");
	    java.net.URL url = new java.net.URL("file://" + current_dir + File.separator + jarname);

	    //System.out.println("jar url=" + url);
	    
	    java.net.URL urls[] = new java.net.URL[1];
	    urls[0] = url;

	    //CustomLoader cl = new CustomLoader( urls, this.getClass() );

	    return null;
	}
	catch(Exception e)
	{
	    e.printStackTrace();
	    return null;
	}
    }
    
    public boolean extractJARfromJAR( String jarname )
    { 
	try
	{
	    // System.out.println("extractJARfromJAR()...");

	    byte[] jardata = getBytesFromJarFile(jarname);

	    FileOutputStream fos = new  FileOutputStream( new File( jarname ));

	    fos.write( jardata );

	    return true;
	}
 	catch(Exception e)
	{
	    e.printStackTrace();
	    return false;
	}
    }


    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --


/*
    public void extractHelpDocsFromJarFile( )
    {
	if(jar_file_name == null)
	{
	    if( !findJarFile() )
		return;
	}
	
	new HelpViewer( this ).extractHelpDocsFromJarFile( jar_file_name );
    }
*/

 
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --


    public boolean makeSurePathExists( String path )
    {
	File test = new File( path );

	if( test.exists() )
	    return true;
	
	if( test.mkdirs() )
	    return true;
	
	return false;
    }


    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --




    public MeasDataProp[] findPropertiesForMeasurement( final Instance measurement )
    {

	// split it into two parts....

	final maxdConnection_m2 mconn = getConnection();
	
	final String property_list_id = mconn.getFieldFromID( measurement.id, "Property_List_ID", "Measurement" );

	if( property_list_id == null )
	    return null;

	// CHECK:
	// the following SQL appears to cause problems in MySQL 
	// possibly because it is too long, or because the
	// ORDER BY clause doest work when the result set is empty

	final String sql = 
	    "SELECT " + 
	    mconn.qTableDotField("Property","Labelled_Extract_ID")  + "," +
	    mconn.qTableDotField("Type","ID")  + "," +
	    mconn.qTableDotField("Type","Name")  + "," +
	    mconn.qTableDotField("Type","Attributes_ID") + 
	    "FROM " + 
	    mconn.qTable("PropertyList") + "," +
	    mconn.qTable("Property") + "," +
	    mconn.qTable("Type") +
	    " WHERE " +
	    mconn.qTableDotField("PropertyList","ID")  + "=" + mconn.qID( property_list_id ) +
	    " AND " + 
	    mconn.qTableDotField("PropertyList","Property_ID") + "=" +  mconn.qTableDotField("Property","ID") +
	    " AND " + 
	    mconn.qTableDotField("Property","Type_ID") + "=" +  mconn.qTableDotField("Type","ID") +
	    " ORDER BY " +
	    mconn.qTableDotField("PropertyList","Index");


	final Vector mdp_v = new Vector();

	ResultSet rs = mconn.executeQuery( sql );
	
	if(rs != null)
	{
	    try
	    {
		while (rs.next()) 
		{
		    
		    final String lex_id       = rs.getString(1);
		    final String type_id      = rs.getString(2);
		    final String type_name    = rs.getString(3);
		    final String type_attr_id = rs.getString(4);
		    
		    final Instance channel_instance = ( lex_id != null ) ? mconn.getInstanceUsingID( lex_id, "LabelledExtract" ) : null;
		    
		    final String type_attr_str = mconn.getDescription( type_attr_id );
		    
		    final java.util.Hashtable type_attr_vals_ht = unserialiseValues( type_attr_str );
		
		    final MeasDataProp mdp = new MeasDataProp( type_name, type_id, channel_instance, type_attr_vals_ht );

		    mdp_v.add( mdp );
		}
	    }
	    catch(SQLException sqle)
	    {
	    }
	    finally
	    {
		mconn.closeResultSet(rs);
	    }
	}

	return (MeasDataProp[]) mdp_v.toArray( new MeasDataProp[ mdp_v.size() ] );

    }


 
    public String[][] getMeasurementPropertyValues( final ArrayDescription array_description,
						    final Instance measurement, 
						    final MeasDataProp[] mdp )
    {
	String sql = null;

	boolean is_a_string = false;

	//EventTimer timer = new EventTimer();

	java.util.Vector all_data_v = new java.util.Vector();

	final String meas_id = measurement.id;

	for(int property = 0; property < mdp.length; property++ )
	{
	    if( mdp[ property ].data_type_code == MeasDataProp.IntegerDataType )
	    {
		sql = 
		    "SELECT " + mconn.qField("Feature_ID") + "," + mconn.qField("Value") + " FROM " +
		    mconn.qTable("IntegerProperty") + " WHERE " + mconn.qField("Measurement_ID") + " = " + 
		    mconn.qID(meas_id) + " AND " + mconn.qField("Type_ID") + " = " + mconn.qID(mdp[ property ].type_id);
		
		
	    }
	    
	    if( mdp[ property ].data_type_code == MeasDataProp.DoubleDataType )
	    {
		sql = 
		    "SELECT " + mconn.qField("Feature_ID") + "," + mconn.qField("Value") + " FROM " +
		    mconn.qTable("DoubleProperty") + " WHERE " + mconn.qField("Measurement_ID") + " = " + 
		    mconn.qID(meas_id) + " AND " + mconn.qField("Type_ID") + " = " + mconn.qID(mdp[ property ].type_id);
		
		
	    }
	    
	    if( mdp[ property ].data_type_code == MeasDataProp.CharDataType )
	    {
		sql = 
		    "SELECT " + mconn.qField("Feature_ID") + "," + mconn.qField("Value") + " FROM " +
		    mconn.qTable("CharProperty") + " WHERE " + mconn.qField("Measurement_ID") + " = " + 
		    mconn.qID(meas_id) + " AND " + mconn.qField("Type_ID") + " = " + mconn.qID(mdp[ property ].type_id);
		
		
	    }
	    
	    
	    if( mdp[ property ].data_type_code == MeasDataProp.StringDataType )
	    {
		sql = 
		    "SELECT " + mconn.qField("Feature_ID") + "," + mconn.qField("Value") + " FROM " +
		    mconn.qTable("StringProperty") + " WHERE " + mconn.qField("Measurement_ID") + " = " + 
		    mconn.qID(meas_id) + " AND " + mconn.qField("Type_ID") + " = " + mconn.qID(mdp[ property ].type_id) +
		    " ORDER BY " + mconn.qField("Index");
		
		is_a_string = true;
	    }
	    
	    
	    if( sql == null )
		return null;
	    
	    //	System.out.println( meas_id + " : name=" + mdp.name + " unit=" + mdp.unit + " type=" + mdp.data_type );
	    
	    // System.out.println( sql );
	    
	    ResultSet rs = mconn.executeQuery( sql );
	    
	    String[] data = new String[ array_description.features.length ];
	    
	    String feature_id;
	    
	    int warned = 0;
	    
	    if(rs != null)
	    {
		try
		{
		    while (rs.next()) 
		    {
			feature_id = rs.getString(1);
			
			Integer index_i = (Integer) array_description.feature_id_to_index.get( feature_id );
			
			if( index_i != null )
			{
			    final int index = index_i.intValue();
			    
			    if( is_a_string )
			    {
				if( data[ index ] == null )
				    data[ index ] = rs.getString(2);
				else
				    data[ index ] += rs.getString(2);
			    }
			    else
			    {
				data[ index ] = rs.getString(2);
			    }
			}
			else
			{
			    if(warned < 10)
			    {
				System.err.println( "retrieveMeasDataProp(): unrecognised FeatureID '" + feature_id + "'");
				warned++;
			    }
			}
			
//		    System.out.println( index + " (" + feature_id + ") = " + data[ index ] );
		    }
		}
		catch(SQLException sqle)
		{
		}
		finally
		{
		    mconn.closeResultSet(rs);
		}
	    }
	    
	    all_data_v.add( data );

	    //System.out.println( "Retrieved " + mdp.data_type + " Property '" + mdp.name + "' in: " + timer.elapsed() );
	}

	return (String[][]) all_data_v.toArray( new String[ all_data_v.size() ][0] );
    }

    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 
 

    /*

    ::TODO:: this could be simplified by collapsing the 3 separate searches (Features,Probe,Genes)
             into a single search

	     SELECT Feature.ID,Probe.ID,Gene.ID FROM Gene,Probe,Feature,ArayType where....


	     and then accumulating the various IDs in a single pass

	     IMPORTANT: make sure the JOIN is done in the optimal order


    */

    public ArrayDescription getArrayDescription( final Instance array_type_inst, final boolean retrieve_attrs )
    {
	System.out.println( "loading ArrayType: " + array_type_inst.id );
	  
	ArrayDescription ad = new ArrayDescription();

	//cached_array_type_id = array_type_id;	

	maxdConnection_m2 mconn = getConnection();

	//  ---- Spots (Features) ----

	{

	    String get_spots = 
		"SELECT " + 
		mconn.qTableDotField("Feature","ID") + "," +
		mconn.qTableDotField("Feature","Name") + "," +
		mconn.qTableDotField("Feature","Attributes_ID") + 
		" FROM " +
		mconn.qTable("Feature") + 
		" WHERE " +
		mconn.qTableDotField("Feature","Array_Type_ID") + "=" +  mconn.qID( array_type_inst.id );
	    
	    ResultSet rs = mconn.executeQuery( get_spots );
	    
	    java.util.Vector feature_v = new java.util.Vector();
	    
	    ad.feature_id_to_index = new java.util.Hashtable();

	    ad.feature_to_attribute_id = new java.util.Hashtable();

	    int index = 0;

	    if(rs != null)
	    {
		try
		{
		    while (rs.next()) 
		    {
			
			final Instance feature_inst = new Instance( rs.getString( 2 ), rs.getString( 1 ) );

			ad.feature_id_to_index.put( feature_inst.id, new Integer( index++ ) );

			ad.feature_to_attribute_id.put( feature_inst, rs.getString( 3 ) );

			feature_v.add( feature_inst );
		    }
		}
		catch( SQLException sqle )
		{
		}
		finally
		{
		}
	    }
	    
	    
	    System.out.println( index + " Spots...." );

	    ad.features = (Instance[]) feature_v.toArray( new Instance[ feature_v.size() ] );

	    // and create the index arrays ready for subsequent filling
	    //
	    ad.reporters = new Instance[ ad.features.length ];
	    ad.genes     = new Instance[ ad.features.length ][];
	}

	//  ---- Probes (Reporters) ----

	int count = 0;

	{
	    
	    String get_probes = 
		"SELECT " + 
		mconn.qTableDotField("Feature","ID") + "," +
		mconn.qTableDotField("Reporter","ID") + "," +
		mconn.qTableDotField("Reporter","Name") + "," +
		mconn.qTableDotField("Reporter","Attributes_ID") + 
		" FROM " +
		mconn.qTable("Reporter") + "," + 
		mconn.qTable("Feature") + 
		" WHERE " +
		mconn.qTableDotField("Feature","Reporter_ID") + "=" +  mconn.qTableDotField("Reporter","ID") + 
		" AND " +
		mconn.qTableDotField("Feature","Array_Type_ID") + "=" +  mconn.qID( array_type_inst.id );
	    
	    ResultSet rs = mconn.executeQuery( get_probes );
	    
	    ad.reporter_to_attribute_id = new java.util.Hashtable();

	    if(rs != null)
	    {
		try
		{
		    while (rs.next()) 
		    {
			final Instance reporter_inst = new Instance( rs.getString( 3 ), rs.getString( 2 ) );

			// figure out the Feature->Reporter indexing
			// (i.e. which Reporter is in which Feature )
			final Integer feature_index = (Integer) ad.feature_id_to_index.get( rs.getString( 1 ) );

			ad.reporters[ feature_index.intValue() ] = reporter_inst;

			final String attr_id = rs.getString( 4 );
			
			if( attr_id != null )
			    ad.reporter_to_attribute_id.put( reporter_inst, attr_id  );

			count++;
		    }
		}
		catch( SQLException sqle )
		{
		}
		finally
		{
		}
	    }
	    
	    
	    System.out.println( count + " Probes...." );
	}

	// ---- Genes ----

	count = 0;

	{
	    
	    //
	    // this should sort the man database servers from the boy database servers....
	    //
	    String get_genes = 
		"SELECT " + 
		mconn.qTableDotField("Feature","ID") + "," + 
		mconn.qTableDotField("Gene","ID") + "," + 
		mconn.qTableDotField("Gene","Name") + "," + 
		mconn.qTableDotField("Gene","Attributes_ID") + 
		" FROM " +
		mconn.qTable("Gene") + "," + 
		mconn.qTable("GeneList") + "," + 
		mconn.qTable("Reporter") + "," + 
		mconn.qTable("Feature") + 
		" WHERE " +
		mconn.qTableDotField("Feature","Array_Type_ID") + "=" +  mconn.qID( array_type_inst.id ) +
		" AND " + 
		mconn.qTableDotField("Feature","Reporter_ID") + "=" +  mconn.qTableDotField("Reporter","ID") + 
		" AND " +
		mconn.qTableDotField("Reporter","Gene_List_ID") + "=" +  mconn.qTableDotField("GeneList","ID") + 
		" AND " +
		mconn.qTableDotField("GeneList","Gene_ID") + "=" +  mconn.qTableDotField("Gene","ID");
	    
	    
	    ResultSet rs = mconn.executeQuery( get_genes );
	    
	    java.util.Vector gene_info_v = new java.util.Vector();

	    ad.gene_to_attribute_id = new java.util.Hashtable();

	    if(rs != null)
	    {
		try
		{
		    while (rs.next()) 
		    {
			final Instance gene_inst = new Instance( rs.getString( 3 ), rs.getString( 2 ) );

			// figure out the Feature->Gene indexing
			// (i.e. which Gene(s) are linked to which Feature )
			final Integer feature_index = (Integer) ad.feature_id_to_index.get( rs.getString( 1 ) );
			final int index = feature_index.intValue();
			
			if( ad.genes[ index ] == null )
			{
			    // no Gene seen for this index yet
			    ad.genes[ index ] = new Instance[ 1 ];
			    ad.genes[ index ][ 0 ] = gene_inst;
			}
			else
			{
			    // add it to the list of Genes for this index
			    ad.genes[ index ] = appendToArray( ad.genes[ index ], gene_inst );
			}

			final String attr_id = (String) rs.getString( 4 );

			if( attr_id != null )
			    ad.gene_to_attribute_id.put( gene_inst, rs.getString( 4 ) );

			count++;
		    }
		}
		catch( SQLException sqle )
		{
		}
		finally
		{
		}
	    }

	    System.out.println( count + " Genes...." );
	    
	}


	System.out.println( "ArrayType: " + array_type_inst.id + " loaded." );

	return ad;
    }


    private Instance[] appendToArray( final Instance[] ai, final Instance i )
    {
	final Instance[] new_ai = new Instance[ ai.length + 1 ];
	for( int copy=0; copy < ai.length; copy++ )
	    new_ai[ copy ] = ai[ copy ];
	new_ai[ ai.length ] = i;
	return new_ai;
    }


    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 
 
 
/*
    public Instance[] retrieveFeaturesForMeasurement( final maxdConnection mconn, final String meas_id )
    {
	java.util.Hashtable feature_id_to_index_ht = new java.util.Hashtable();
	
	String array_type_id = mconn.getFieldFromID( meas_id, "Array_Type_ID", "Measurement" );

	Instance[] features = retrieveFeatures( mconn, array_type_id );

	if( features != null )
	{
	    for(int f=0; f < features.length; f++ )
	    {
		feature_id_to_index_ht.put( features[ f ].id, new Integer( f ) );
	    }
	}

	return feature_id_to_index_ht;
    }
*/
/*
    private Instance[] retrieveFeatures( final maxdConnection mconn, final String array_type_id )
    {
	final Instance[] feature_insts = cntrl.getConnection().getInstancesUsingFKField( array_type_id, 
											 "Array_Type_ID", 
											 "Feature" );

	return feature_insts;
    }
*/
    private String getArrayTypeID( final maxdConnection_m2 mconn,  final String image_id )
    {
	if( image_id == null )
	    return null;

	final String get_array_type_sql =
	    "SELECT " + 
	    mconn.qTableDotField("ArrayType","ID") + 
	    " FROM " +
	    mconn.qTable("Image") + ", " +
	    mconn.qTable("Hybridisation") + ", " +
	    mconn.qTable("Array") + ", " +
	    mconn.qTable("ArrayType") +
	    " WHERE " +
	    mconn.qTableDotField("Image", "ID") + " = " +  mconn.qID( image_id ) +
	    " AND " + 
	    mconn.qTableDotField("Image", "Hybridisation_ID") + " = " + 
	    mconn.qTableDotField("Hybridisation","ID") +
	    " AND " + 
	    mconn.qTableDotField("Hybridisation","Array_ID") + " = " + 
	    mconn.qTableDotField("Array", "ID") +
	    " AND " + 
	    mconn.qTableDotField("Array", "Array_Type_ID") + " = " + 
	    mconn.qTableDotField("ArrayType","ID");
	
	ResultSet rs = mconn.executeQuery(get_array_type_sql);

	String array_type_id = null;

	if(rs != null)
	{
	    try
	    {
		while (rs.next()) 
		{
		    array_type_id = rs.getString(1);
		}
	    }
	    catch(SQLException sqle)
	    {
	    }
	    finally
	    {
		mconn.closeResultSet(rs);
	    }
	}
	

	// System.out.println( "getArrayTypeID(): image_id=" + image_id + " -> array_type_id=" + array_type_id );

	return array_type_id;
    }

/*
    //
    // note that data is returned as strings irrespective of the 'real' type
    //
    private String[][] PropertiesForMeasurement( final Instance inst )
    {
	if( inst == null )
	    return null;

	System.out.println( "getting data for " + inst.name );

//	EventTimer timer = new EventTimer();

//	if( bar!= null )
//	    bar.setPercent( 0 );

	// retrieve the Feature IDs for this ArrayType

	String image_id = cntrl.getConnection().getFieldFromID( inst.id, "Image_ID", "Measurement" );

	if( image_id == null )
	{
	    cntrl.alertMessage("Unable to identify the Image for this Measurement.");
	    return;
	}
	
	String array_type_id = getArrayTypeID( cntrl.getConnection(), image_id );

	if( array_type_id == null )
	{
	    cntrl.alertMessage("Unable to identify the ArrayType for this Measurement.");
	    return;
	}
	

	Instance[] feature_instances = retrieveFeatures( cntrl.getConnection(), array_type_id );
	
	System.out.println( "Features retrieved in: " + timer.elapsed() );

//	timer = new EventTimer();

	final int n_features = feature_instances == null ? 0 : feature_instances.length;

	//System.out.println( n_features + " Features on the ArrayType" );

	if( n_features == 0 )
	{
	    cntrl.infoMessage("No Features are defined for the ArrayType used by this Measurement.");
	    return;
	}

	Hashtable feature_id_to_index_ht = new Hashtable();

	for(int s=0; s < n_features; s++)
	    feature_id_to_index_ht.put( feature_instances[ s ].id, new Integer( s ) );

	Vector data_v      = new Vector();
	Vector col_names_v = new Vector();

	// retrieve all the Properties for this Measurement
	
	MeasDataProp[] mdp_list = findPropertiesForMeasurement( cntrl.getConnection(),  inst.id );

	if(( mdp_list == null ) || ( mdp_list.length == 0 ))
	{
	    cntrl.alertMessage("No data is available for this Measurement");
	    return;
	}

//	int percent_per_property = 100 / mdp_list.length;

	// load each of the Properties in turn

	for( int m=0; m <  mdp_list.length; m++ )
	{
	    String[] data = retrieveMeasDataProp( cntrl.getConnection(), inst.id, mdp_list[m], 
						  n_features, feature_id_to_index_ht  );
	    
	    if( data != null )
	    {
		data_v.add( data );

		col_names_v.add( mdp_list[m].name );
	    }
	    
//	    if( bar != null )
//		bar.setPercent( m * percent_per_property );
	}

//	System.out.println( "Properties retrieved in: " + timer.elapsed() );

//	timer = new EventTimer();

	// note: annoyingly, the data is loaded one Property at a 
	//       time so the orientation is wrong for the DataViewer
	//
	//       (i.e. rows are Properties, cols are Features so the
	//        data is wide and short which doesn't suit tablular
	//        display)
	//
	// so the data is rotated...
	//
	// ::TODO:: modify the DataViewer so it can cope with data in 
	//          either orientation to avoid this memory overhead


	return data_v.toArray( new String[0][] );
    }
    


    public Object[] loadProperty( final MeasDataProp measurement_data_property, 
				  final String measurement_id, 
				  final java.util.Hashtable feature_id_to_index )
    {


    }
*/

    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --


    public static java.util.Hashtable unserialiseValues( String str )
    {
	java.util.Hashtable ht = new java.util.Hashtable();

//	System.out.println( "unserialiseValues(): " + str );

	if( str != null)
	{
	    //System.out.println( "attr_str=" + str );


	    // tokenise() is clever enough to ignore escaped delimiter characters
	    //
	    String[] tokens = tokenise( str, ';' );
	    

	    if(tokens != null)
	    {
		for(int t=0; t < tokens.length; t++)
		{
		    //System.out.println( "token " + t + " = " + tokens[ t] );

		    String[] nv_pair = tokenise( tokens[t], '=' );
		    
		    if( ( nv_pair != null ) && ( nv_pair.length == 2 ) )
		    {
			//System.out.println( nv_pair[0] + " = " + nv_pair[1] );
			
			//
			// NOTE: 
			//  
			//  only characters that are significant to the serialisation process are escaped here,
			//  these characters are = (equals) and ; (semicolon)
			//  all escaping of special characters is handled by the maxdConnection (in the tidyText() method)
			// 
			
			ht.put( unescapeAttrText( nv_pair[0] ), unescapeAttrText( nv_pair[1] ) );

			//System.out.println( "value " + t + " = " + unescapeAttrText( nv_pair[1] ) );

			//
			// ::TODO:: these calls to unescapeAttrText() are messing things up!
			//          they appear to be removing more that they are supposed to
			//
			// fixed?
			//
			//ht.put( nv_pair[0], nv_pair[1] );

		    }
		}
	    }
	}

	return ht;
    }

    //
    // should convert '\;' to ';' and '\=' to '='
    //
    // but watch out for '\n\\;' that semicolon is not escaped!
    //
    private static String unescapeAttrText( String input )
    {
	
	if(input == null)
	    return null;

	final int input_length = input.length();

	boolean hit = false;

	//System.out.println("unescapeAttrText: in=" + input);

	for(int c=0; ( hit == false ) && ( c < input_length ); c++)
	{
	    final char ch = input.charAt(c);

	    if( ch == ';' || ch == '=' )
		hit = true;
	}

	if( hit == false )
	{
	    // nothing to do here, move along
	   
	    return input.trim();
	}


	StringBuffer sbuf = new StringBuffer();
	
	int escape_count = 0;


	for( int c=0; c < input_length; c++)
	{
	    final char ch = input.charAt(c);

	    if( ch == '\\' )
	    {
		escape_count++;
	    }

	    if( ( ch  == ';' ) || ( ch == '=' ) )
	    {
		if( ( c > 0 ) && ( input.charAt( c - 1 ) == '\\' ) )
		{
		    // the special character is preceeded by an escape char, 
		    // 
		    // is this escape affecting the special character ?
		    //
		    if( ( escape_count % 2 ) == 1 )
		    {
			// why yes it does
			//
			// replace the current 'last' character 
			// (which will be an escape char)
			// with the ; or = char
			
			sbuf.setCharAt( sbuf.length() - 1, ch );
		    }
		    else
		    {
			// not a live escape char (i.e. it's not affect the
			// special char, the escape char has been escaped itself)
			//
			sbuf.append( ch );
		    }
		}
		else
		{
		    // the special character is NOT preceeded by an escape char
		    //
		    sbuf.append( ch );
		}
	    }
	    else
	    {
		// not a special character, might be a normal char or an escape char
		// 
		sbuf.append( ch );
	    }

	    if( ch != '\\' )
		escape_count = 0;
	    
	}

	//System.out.println("unescapeAttrText:\n   in=" + input + "\n  out=" + sbuf.toString() );

	return sbuf.toString().trim();
    }


    public static String[] tokenise( String str, char delim )
    {
	if((str == null) || (str.length() == 0))
	    return null;

	java.util.Vector tokens = new java.util.Vector();
	
	int token_start = 0;

	int escape_count = 0;

	for(int c=0; c < str.length(); c++)
	{
	    char ch = str.charAt(c);

	    //
	    //    important! take care\\;the previous semicolon is really a delimiter and is not escaped
	    //

	    if( ch == '\\' )
	    {
		escape_count++;
	    }
	    else
	    {
		if( ch == delim )
		{
		    boolean prev_is_escape_char = ( c > 0 ) && ( str.charAt( c - 1 ) == '\\' );
		    boolean is_escaped = prev_is_escape_char && ( ( escape_count % 2 ) == 1 );
		    
		    if( is_escaped == false )
		    {
			String token = str.substring( token_start, c );
			tokens.addElement( token );
			token_start = c+1;
		    }
		}

		// it wasn't an escape char, so reset the escape char counter
		//
		escape_count = 0;
	    }
	}
	// anything left over?
	if( token_start < str.length() )
	{
	    String token = str.substring( token_start,  str.length() );
	    tokens.addElement( token );
	}
	// was the last character a delimiter? (in which case there is an empty token at the end)
	if( str.charAt( str.length()-1 ) == delim)
	{
	    tokens.addElement( new String("") );
	}

	// was there just one empty token ?
	if( tokens.size() == 1 )
	    if( ((String) tokens.elementAt(0)).trim().length() == 0 )
		// if so, pretend there was nothing
		return null;

	//for(int t=0; t < tokens.size(); t++)
	//    System.out.println("<token" + t + "><" + (String)tokens.elementAt(t) + ">" );

	return (String[]) tokens.toArray( new String[0] );
    }



    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --



    // TODO:: should be part of maxdConnection
    private boolean normal_escape_mode_for_single_quotes = true;
 
    private JPanel status_panel;

    private JPanel current_panel = null;
    //private Vector panel_list = new Vector();
    private GridBagLayout panel_gridbag;
    private int panels_shown = 0;
    public  JPanel connect_panel = null;
  
    private boolean is_applet;
 
    private JFrame parent_frame = null;
    private JSplitPane split_pane;  // used to store the Nav Panel when it is in 'share window' mode


    private maxdConnection_m2 mconn = null;

    public ConnectionManager_m2 cman  = null;

    public boolean auto_run_test = false;
}

