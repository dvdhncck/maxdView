import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.lang.reflect.*;
import java.util.Vector;
import java.util.Date;
import java.util.Properties;

import java.rmi.*;
import java.rmi.server.*;

import java.util.jar.*;

import java.util.Hashtable;
import java.util.HashSet;

import java.net.*;

import java.util.Enumeration;
import javax.swing.tree.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreeSelectionModel;

public class maxdView
{
    public final int    version_series  = 1;
    public final int    version_major   = 0;
    public final int    version_minor   = 5;

    // the 'prerelease_variant' string should be in the form:  'alpha' 'digit', eg 'a1', 'b2' etc

    public final String version_prerelease_variant = ""; 

    private String app_title;
    
    public String getAppTitle() { return app_title; }

    public ExprData         getExprData()         { return expr_data; }
    public DataPlot         getDataPlot()         { return first_dplot_panel; }
    public AnnotationLoader getAnnotationLoader() { return annotation_loader; }
    public CustomMenu       getCustomMenu()       { return custom_menu; } 

    public JFrame getMainFrame() { return frame; }

    public AnnotationViewer av = null;
    
    private CustomMenu custom_menu;

    private ExprData expr_data;
    private static int n_views;
    private boolean is_first_view;

    private String user_directory;

    private JPanel pane;
    private DataPlot dplot_panel;
    private static DataPlot first_dplot_panel;
	
    private JLabel message_label;

    private JFrame frame;

    private JLabel filter_alert_label;
    private JLabel filter_info_label;
    private boolean filter_alert = false;
    private JLabel filter_status_label;

    private Hashtable menus; // JMenu system_menu, file_menu, transform_menu, plugin_menu, filter_menu;

    private JMenuBar menu_bar;

    private int import_menu_items;     // needed for positioning of plugin items in the menus
    private int export_menu_items;     // needed for positioning of plugin items in the menus
    private int import_insert_pos;

    private JLabel zoom_label;

    private JCheckBoxMenuItem apply_filter_menu_item;

    private HelpPanel help_panel = null;
    private final String help_directory = "docs";
    
    // private boolean scrollbars_on_left = true;

    private AnnotationLoader annotation_loader;
    
    private String[] command_line_args;

    private JFileChooser fc;
    private JColorChooser col_chooser;

    private Timer timer;
  
    private boolean is_java_1_4;

    // public String toString() { return "maxdView <" + expr_data + ">"; }
    
    public String getApplicationTitle() { return app_title; }

    
    public maxdView() throws RemoteException
    {
	this(null, null);
    }

    public maxdView(String[] args) throws RemoteException
    {
	this(args, null);
    }

    public maxdView(String[] args, ExprData src) throws RemoteException
    {
	//super();
	    
	try
	{
	    
	    app_title = "maxdView " + version_series + "." + version_major + "." + version_minor;
	    if((version_prerelease_variant != null) && (version_prerelease_variant.length() > 0))
		app_title += ("/" + version_prerelease_variant);
	    
	    command_line_args = args;
	    
	    //
	    // the configuration directory is identified as follows
	    //
	    //     using the optional command line arg "-user_directory" 
	    //
	    //       which overrides
	    //
	    //     the "user.home" system property (i.e. the $HOME environment variable)
	    //
	    //       which overrides
	    //
	    //     the "user.dir" system property (i.e. the current working directory, most likely the maxdView installation directory)
	    //
	    
	    identifySharedDirectory();

	    identifyConfigDirectory();



	    // do we need a a custom classpath to handle the user plugins directory?


	    
	    initOptions();
	    
	    is_java_1_4 = System.getProperty("java.version").startsWith("1.4");
	
	   
	    if(src == null)
	    {
		expr_data = new ExprData();
		n_views = 1;
		is_first_view = true;
		
		
		
	    }
	    else
	    {
		expr_data = src;
		n_views++;
		is_first_view = false;
		// System.out.println("maxdView clone created");
	    }
	    
	    //custom_class_loader = new NewCustomClassLoader( this.getClass() );
	    
	    
	    /*
	      
	    // testing the version number comparision routines
	    
	    VersionCheckerThread vct = new VersionCheckerThread();
	    
	    System.out.println( "0.9.7 : " + vct.isNewer("0.9.7"));
	    System.out.println( "0.9.4 : " + vct.isNewer("0.9.4"));
	    System.out.println( "0.9.5/b2 : " + vct.isNewer("0.9.5/b2"));
	    System.out.println( "0.9.5/b4 : " + vct.isNewer("0.9.5/b4"));
	    System.out.println( "0.9.5/a1 : " + vct.isNewer("0.9.5/a1"));
	    System.out.println( "0.9.5/a6 : " + vct.isNewer("0.9.5/a6"));
	    System.out.println( "0.9.5/c1 : " + vct.isNewer("0.9.5/c1"));
	    System.out.println( "1.0.0 : " + vct.isNewer("1.0.0"));
	    */
	    
	}
	catch( Exception ex )
	{
	    ex.printStackTrace();
	}
    }

    /*
    public void send(String thing_type, Object thing)
    {
	System.out.println( thing_type + ":" + thing + " received");
    }
    */

    //
    //	background_col = new Color(50,0,50);
    //  text_col       = new Color(254, 204, 254);
    //

    public Color getBackgroundColour()             { return background_col; }
    public void setBackgroundColour(Color new_col) 
    {
	background_col = new_col; 
	expr_data.generateEnvironmentUpdate(ExprData.ColourChanged);
    }
    private Color background_col = new Color(101,0,101);

    public Color getTextColour()             
    {
	return text_col; 
    }

    public void setTextColour(Color new_col) 
    { 
	text_col = new_col; 
	expr_data.generateEnvironmentUpdate(ExprData.ColourChanged);
    }
    private Color text_col = new Color(247, 205, 247);

    public Font getSmallFont()
    {
	if(small_font == null)
	{
	    Font f = new JLabel().getFont();
	    small_font = new Font(f.getName(), f.getStyle(), f.getSize() - 2);
	}
	return small_font;
    }
    private Font small_font = null;

    public void cleanExit()
    {
	closeDown();
	if(system_options[CanExit])
	    System.exit(0);
    }

    public void closeDown()
    {
	notifyExitListeners();
	
	saveProperties();

	if(dbcon != null)
	    dbcon.reallyDisconnect();

	/*
	if(custom_menu != null)
	    custom_menu.writeCustomMenuTree();
	*/

	// TO DO: should visit all the plugins and call stopPlugin on them....

	timer.stop();
	frame.setVisible(false);
	dplot_panel.closeDown();
	dplot_panel = null;
	frame = null;
    }

    void getBusy( )
    {
	if(command_line_args != null)
	    for(int a=0; a < command_line_args.length; a++)
	    {
		if(command_line_args[a].toLowerCase().equals("-allow_rmi"))
		    system_options[AllowRMI] = true;
	    }
	

	//if(system_options[LogErrorsToFile])
	//    System.setErr( new SpecialisedPrintStream ( System.err ) );
		
	if(is_first_view)
	{
	    if(system_options[AllowRMI])
	    {
		if(System.getSecurityManager() == null) 
		{
		    System.setSecurityManager(new RMISecurityManager());
		}
		
		String name = "//localhost/maxdViewRemoteExprData";
		
		final String emsg = "RMI connection failed.\n" + 
		" - is the 'rmiregistry' running?\n" + 
		" - is the security policy file in place?\n" + 
		" - is the java.rmi.server.codebase set?\n\n";
		try 
		{
		    Naming.rebind(name, getExprData());
		    System.out.println("server bound as " + name);
		}
		catch (java.security.AccessControlException ace) 
		{
		    System.err.println("RMI bind failed.\n" + emsg + ace.getMessage());
		    errorMessage("RMI bind failed.\n" + emsg + ace.getMessage());
		    if(system_options[CanExit])
			System.exit(-1);
		    
		}
		catch (Exception e) 
		{
		    System.err.println(emsg + e.getMessage());
		    errorMessage(emsg + e.getMessage());
		    if(system_options[CanExit])
			System.exit(-1);
		}
		catch (Throwable th) 
		{
		    System.err.println(emsg + th.getMessage());
		    errorMessage(emsg + th.getMessage());
		    if(system_options[CanExit])
			System.exit(-1);
		}
	    }
	}

	loadProperties();
	custom_menu = new CustomMenu(this);
	createComponents();
	 
	if(isLicenceAgreed())
	{
	    frame.pack();
	    frame.setVisible(true);

	    if(is_first_view)
	    {
		checkIsLatestVersion();
		
		checkVersionHasChanged();
		
		makeConfigDirIfMissing();
	    }
	    
	    readPluginDescriptionFile( is_first_view, true );
	    
	    readPluginCommandInfoFile( is_first_view );

	    buildMenus();
	
	    annotation_loader = new AnnotationLoader(this); 
	    
	    decorateFrame( frame );
	
	    if( small_logo_image != null )
		ProgressOMeter.setLogoImage( small_logo_image );

	    if((app_runs == 1) && is_first_view)
		getHelpTopic("FirstTime");

	    if((command_line_args != null) && (command_line_args.length > 0))
	    {
		for(int a=0; a < command_line_args.length; a++)
		{
		    if((command_line_args[a].endsWith(".maxd")) ||(command_line_args[a].endsWith(".maxd.gz")))
		    {
			String[] fcommand_line_args = { "file", command_line_args[a] };
			runCommand("Read Native", "load", fcommand_line_args);
		    }
		    if((command_line_args[a].endsWith(".xml")) ||(command_line_args[a].endsWith(".xml.gz")))
		    {
			String[] fargs = { "file", command_line_args[a], "where", "root" };
			runCommand("Cluster Manager", "load", fargs);
		    }
		}
	    }
	}
	else
	{
	    // agreement licence declined or text file not found....
	    if(system_options[CanExit])
		System.exit(-1);
	}

    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  optional things
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void setOption( int option, boolean enabled )
    {
	system_options[option] = enabled;
    }
    public boolean getOption( int option )
    {
	return system_options[ option ];
    }

    public final static int NamesAreEditable  = 0;
    public final static int CanSpawnNewView   = 1;
    public final static int CanExit           = 2;
    public final static int LogErrorsToFile   = 3;
    public final static int AllowRMI          = 4;
    public final static int DebugPlugins      = 5;

    private boolean[] system_options;

    private void initOptions()
    {
	system_options = new boolean[6];
	system_options[NamesAreEditable] = true;
	system_options[CanSpawnNewView]  = true;
	system_options[CanExit]          = true;
	system_options[LogErrorsToFile]  = true;
	system_options[AllowRMI]         = false;
	system_options[DebugPlugins]     = false;
    }


    private void copyStuffFrom( maxdView src_mview )
    {
	for(int so=0; so < 6; so++)
	    system_options[ so ] = src_mview.system_options[ so ];
	setTopDirectory( src_mview.getTopDirectory() );
	if(src_mview.exit_listeners == null)
	    exit_listeners = null;
	else
	    exit_listeners = (Vector) src_mview.exit_listeners.clone();
    }
   

 
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // 
    // variable configuration directory technology
    //
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // this is all new and exciting in version 1.0.5
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  


    //
    // the installation directory (and hence the shared components such as 'standard' plugins and help documents) is identified as follows
    //
    //     using the optional command line arg "-shared_directory"  (e.g. /usr/local/software/maxdView) 
    //
    //       which overrides
    //
    //     the "MAXDVIEW_HOME" system property (i.e. the $MAXDVIEW_HOME environment variable)
    //
    //       which overrides
    //
    //     the "user.dir" system property (i.e. the current working directory, most likely the maxdView installation directory)
    //

    private void identifySharedDirectory( )
    {
	try
	{
	    top_directory = new File( "." ).getCanonicalPath();

	    //if( top_directory.endsWith( File.separator ) == false )
	    //	top_directory += File.separator;

	}
	catch( java.io.IOException ioe )
	{
	    System.out.println("Information: unable to establish where shared directory is");
	    top_directory = "";
	}



	String envvar_dir = System.getProperty( "MAXDVIEW_HOME", null );

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
			System.err.println( "FATAL: the '-shared_directory' should be followed by a directory path [ERROR 106]" );
			alertMessage( "FATAL ERROR!\n\nThe '-shared_directory' should be followed by a directory path" );
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
    //     the "user.dir" system property (i.e. the current working directory, most likely the maxdView installation directory)
    //
    
    private void identifyConfigDirectory( )
    {
	user_directory = System.getProperty( "user.dir", "." );

	String user_home_dir = System.getProperty( "user.home", null );

	if( user_home_dir != null )
	{
	    user_directory = user_home_dir + File.separatorChar + ".maxd" + File.separatorChar;
	    
	    createUserConfigDirectoryIfItDoesntAlreadyExist();
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
			System.err.println( "FATAL: the '-user_directory' should be followed by a directory path [ERROR 102]" );
			alertMessage( "FATAL ERROR!\n\nThe '-user_directory' should be followed by a directory path" );
			System.exit( -1 );
		    }
		}
	    }
	}

	if( ( user_directory == null ) || ( user_directory.length() == 0 ) )
	{
	    System.err.println( "FATAL: the configuration directory could not be determined [ERROR 101]" );
	    alertMessage( "FATAL ERROR!\n\nThe configuration directory (where maxdView stores it's settings) could not be determined.\n" + 
			  "This makes it impossible to run the application properly.\n\n" + 
			  "See the help documentation for information about how\n" + 
			  "to resolve this issue." );
	    System.exit( -1 );
	}

	// trim off any trailing path separator(s)
	while( user_directory.charAt( user_directory.length() - 1 ) == File.separatorChar )
	    user_directory = user_directory.substring( 0, user_directory.length() - 1 );
	

	System.out.println("Information: user-specific directory is '" + user_directory + "'" );
    }


    private void createUserConfigDirectoryIfItDoesntAlreadyExist()
    {
	try
	{
	    File test = new File( user_directory );
	   
	    if( test.exists() == true )
	    {
		if( test.canWrite() == false )
		{
		    // this is a serious problem becuase the config directory is non-readble to the application
		    System.err.println( "FATAL: the config directory is not writable [ERROR 103]" );
		    alertMessage( "FATAL ERROR!\n\nThe configuration directory (where maxdView stores it's settings) is not writable.\n" + 
				  "This makes it impossible to run the application properly.\n\n" + 
				  "See the help documentation for information about how\n" + 
				  "to resolve this issue." );
		    System.exit( -1 );
		}
		else
		{
		    // everything is hunky and/or dory

		    return;
		}
	    }
	    else
	    {
		// config dir doesn't exist, try to create it.....

		if( test.mkdirs() == true )
		{
		    // ok, made it properly, copy anything in the 'shared' config directory to this newly created one...
		    
		    copySharedConfigToPerUserConfig();
		}
		else
		{
		    System.err.println( "FATAL: the config directory could not be created  [ERROR 104]" );
		    alertMessage( "FATAL ERROR!\n\nThe configuration directory (where maxdView stores it's settings) could not be created.\n" + 
				  "This makes it impossible to run the application properly.\n\n" + 
				  "See the help documentation for information about how\n" + 
				  "to resolve this issue." );
		    System.exit( -1 );
		    
		}

	    }


	}
	catch( Exception unexpected )
	{
	    System.err.println( "FATAL: An unexpected exception occured when setting up the config directory  [ERROR 105]" );
	    System.err.println( "       (the target directory was '" + user_directory + "')" );
	    unexpected.printStackTrace();
	    alertMessage( "FATAL ERROR!\n\nAn unexpected exception occured when trying to set up the\n" + 
			  "configuration directory (where maxdView stores it's settings).\n" + 
			  "This makes it impossible to run the application properly.\n\n" + 
			  "See the help documentation for information about how\n" + 
			  "to resolve this issue." );
	    System.exit( -1 );
	}
    }


    private void copySharedConfigToPerUserConfig()
    {
	// this method is called when the user's configuration directory has been freshly created
	// it copies anything in the 'shared config' directory (i.e. in the 'config' directory in the 
	// maxdView installation directory) into the newly created per-user config directory

	
	final String shared_config_path = getTopDirectory() + File.separatorChar + "config" + File.separatorChar;
	final String user_config_path   = user_directory + File.separatorChar;

	final File shared_config = new File( shared_config_path );

	final String[] contents = shared_config.list();

	if( contents != null )
	{
	    for( int c = 0; c < contents.length; c++ )
	    {
		File src =  new File( shared_config_path + contents[ c ] );
		File dest = new File( user_config_path   + contents[ c ] );
		
		tryToCopyFile( src, dest );
	    }
	}
	
	System.out.println("Information: copied existing configuration files from '" + shared_config_path + "' to '" + user_config_path + "'" );

    }

    
    private void tryToCopyFile( File src, File dest )
    {
	try
	{
	    if( src.isDirectory() )
	    {
		String[] list = src.list();

		if( dest.exists() == false )
		{
		    dest.mkdirs();
		    
		    System.out.println("Information: created directory '" + dest.getCanonicalPath() + "'" );
		}
			
		if( list != null )
		{
		    for(int d=0; d < list.length; d++)
		    {
			File src_sub  = new File( src,  list[ d ] );
			File dest_sub = new File( dest, list[ d ] );
			
			tryToCopyFile( src_sub, dest_sub );
		    }
		}
	    }
	    else
	    {
		BufferedInputStream cpy_reader  = new BufferedInputStream( new FileInputStream( src ) );
		BufferedOutputStream cpy_writer = new BufferedOutputStream( new FileOutputStream( dest ) );
		
		boolean done = false;
		
		while( cpy_reader.available() > 0 )
		{
		    cpy_writer.write( (byte) cpy_reader.read() );
		}
	    
		cpy_reader.close();
		cpy_writer.close();
		
		
		//System.out.println("Information: copied '" + src.getCanonicalPath() + "' to '" + dest.getCanonicalPath() + "'" );
	    }

	}
	catch( java.io.IOException ioe )
	{
	    System.err.println("Warning: unable to copy '" + src.getPath() + "' to '" + dest.getPath() + "'" );

	    ioe.printStackTrace();

	    System.exit( -1 );
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  abstraction for install directory
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    //
    //
    // the 'shared' directory is the installation location (read-only) 
    //   the 'user' directory is the per-user location (user-writable)
    //
    //
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public String getTopDirectory()
    {
	return top_directory;
    }

    // this is used by the ISYS client version
    // (or potentially by other applications which wish to wrap maxdView up
    //  and run it from somewhere other than it's default installation directory)
    public void setTopDirectory( String new_top )
    {
	if(!new_top.endsWith( String.valueOf(File.separatorChar) ))
	    new_top += File.separatorChar;

	// infoMessage("Top-level directory set to '" + new_top + "'");

	System.out.println("Information: setTopDirectory() top dir = "  + new_top);

	top_directory = new_top;
    }
    
    private String top_directory = "." + File.separatorChar;
    
    public String getConfigDirectory()
    {
	//return (getTopDirectory() + "config" + File.separatorChar);
	return user_directory + File.separatorChar + "config" + File.separatorChar;
    }

    public String getImageDirectory()
    {
	return (getTopDirectory() + File.separatorChar + "images" + File.separatorChar);
    }

    public String getHelpDirectory()
    {
	return (getTopDirectory() + File.separatorChar + "docs" + File.separatorChar);
    }

    public String getSharedPluginDirectory()
    {
	return "plugins" + File.separatorChar;	
    }

    public String getUserSpecificPluginDirectory()
    {
	return user_directory + File.separatorChar + "plugins" + File.separatorChar;
    }

    public String getTemporaryDirectory()
    {
	final String tmp_dir_path = user_directory + File.separatorChar + "tmp";

	File tmp_dir_file = new File( tmp_dir_path );

	if( tmp_dir_file.exists() == false )
	    tmp_dir_file.mkdirs();

	return tmp_dir_path;
    }

    public String getUserSpecificDirectory()
    {
	return user_directory + File.separatorChar;
    }

    
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  other features for wrapping maxdView
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void addExitListener( ActionListener al )
    {
	// System.out.println("INFO: addExitListener() listener added");

	if(exit_listeners == null)
	    exit_listeners = new Vector();
	exit_listeners.addElement(al);
    }

    private Vector exit_listeners = null;

    private void notifyExitListeners()
    {
	if(exit_listeners == null)
	    return;
	for(int el=0; el < exit_listeners.size(); el++)
	{
	    ActionListener al = (ActionListener) exit_listeners.elementAt(el);
	    al.actionPerformed(null);
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  


    public void addPopupMenuEntry( String name, ActionListener al )
    {
	//System.out.println("INFO: addPopupMenuEntry() entry added");

	if(popup_menu_entry_al == null)
	{
	    popup_menu_entry_al = new Vector();
	    popup_menu_entry_name= new Vector();
	}

	popup_menu_entry_name.addElement(name);
	popup_menu_entry_al.addElement(al);
    }

    public int addExternalPopupEntries(JPopupMenu menu)
    {
	if(popup_menu_entry_name == null)
	    return 0;
	for(int e=0; e < popup_menu_entry_name.size(); e++)
	{
	    JMenuItem mi = new JMenuItem( (String) popup_menu_entry_name.elementAt(e) );
	    mi.addActionListener( (ActionListener) popup_menu_entry_al.elementAt(e) );
	    menu.add(mi);
	}
	return popup_menu_entry_name.size();
    }

    private Vector popup_menu_entry_name = null;
    private Vector popup_menu_entry_al = null;


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  top level GUI
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void createComponents() 
    {
	if(!is_first_view)
	{
	    String view_str = " <" + n_views + ">";

	    app_title +=  view_str;
	}
	else
	{
	    input_icon = new ImageIcon(getImageDirectory() + "open-hand.gif");
	    info_icon = new ImageIcon(getImageDirectory() + "flat-palm.gif");
	    error_icon = new ImageIcon(getImageDirectory() + "thumbs-down.gif");
	    success_icon = new ImageIcon(getImageDirectory() + "thumbs-up.gif");
	}

/*
	try
	{
	    frame = new CustomFrame(app_title);
	}
	catch( java.lang.NoClassDefFoundError ncdfe )
	{
	    // it's ok, it just means that we aren't running Java 1.4

	    frame = new JFrame( app_title );

	    frame.addKeyListener(new CustomKeyListener());
	}
*/
	//frame = new JFrame( app_title );

	frame = new CustomFrame(app_title);

	frame.addWindowListener(new WindowAdapter() 
				{
				    public void windowClosing(WindowEvent e)
				    {
					System.out.println("window is closing!");
					cleanExit();
				    }
				});

	// --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  
	//    data plot panel
	// --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  

	dplot_panel = new DataPlot(this, expr_data, is_first_view);
	dplot_panel.setPreferredSize(new Dimension(500, 350));


	//InputMap imap = dplot_panel.getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW );
	//imap.put( "pressed", new KeyAction() );
	
	//ActionMap amap = dplot_panel.getActionMap( 

	if(is_first_view)
	    first_dplot_panel = dplot_panel;

	int bw = getIntProperty("dplot.box_width", 60);
	int bh = getIntProperty("dplot.box_height",20);
	int cg = getIntProperty("dplot.col_gap",1);
	int rg = getIntProperty("dplot.row_gap",1);
	
	dplot_panel.setBoxGeometry(bw, bh, cg, rg);
	
	dplot_panel.setBorderGap(getIntProperty("dplot.border_gap",0));
	dplot_panel.setFontSize(getIntProperty("dplot.font_size",12));
	dplot_panel.setFontFamily(getIntProperty("dplot.font_family",0));
	dplot_panel.setFontStyle(getIntProperty("dplot.font_style",0));
	
	dplot_panel.setNameColGap(getIntProperty("dplot.name_col_gap",8));
	
	try
	{
	    int cnum = new Integer(application_props.getProperty("eview.background_colour")).intValue();
	    setBackgroundColour(new Color(cnum));
	}
	catch(java.lang.NumberFormatException nfe)
	{ }
	
	try
	{
	    int cnum = new Integer(application_props.getProperty("eview.text_colour")).intValue();
	    setTextColour(new Color(cnum));
	}
	catch(java.lang.NumberFormatException nfe)
	{ }
	
	dplot_panel.removeAllNameCols();
	
	final int nc = getIntProperty("dplot.n_name_cols", 1);
	for(int c=0; c < nc; c++)
	{
	    dplot_panel.addNameCol();
	    dplot_panel.getNameColSelection(c).setNames(getProperty("dplot.name_col_" + c + ".display", "Gene name(s)"));
	    dplot_panel.setNameColTrimEnabled(c, getBooleanProperty("dplot.name_col_" + c + ".trim", true));
	    dplot_panel.setNameColTrimLength(c, getIntProperty("dplot.name_col_" + c + ".trim", 32));
	    dplot_panel.setNameColAlign(c, getIntProperty("dplot.name_col_" + c + ".align", 2));
	}
	
	/*
	  dplot_panel.setRowLabelSource(getIntProperty("dplot.row_label_source", 1));
	  dplot_panel.setRowLabelAlign(getIntProperty("dplot.row_label_align", 0));
	  
	  dplot_panel.setTrimEnabled(getBooleanProperty("dplot.trim_enabled", true));
	  dplot_panel.setTrimLength(getIntProperty("dplot.trim_len", 32));
	*/

	for(int p=0; p < 2; p++)
	{
	    String thing = (p == 0) ? "spot" : "meas";

	    dplot_panel.setAlignGlyphs(         p, getBooleanProperty( "dplot.align_"   + thing + "_glyphs",   false));
	    dplot_panel.setOverlayRootChildren( p, getBooleanProperty( "dplot.overlay_" + thing + "_root",     false));
	    dplot_panel.setShowBranches(        p, getBooleanProperty( "dplot.show_"    + thing + "_branches", true));
	    dplot_panel.setShowGlyphs(          p, getBooleanProperty( "dplot.show_"    + thing + "_glyphs",   true));
	    dplot_panel.setBranchScale(         p, getIntProperty    ( "dplot.branch_"  + thing + "_scale",    2));
	}

	int ww =  getIntProperty("dplot.window_width", 500);
	int wh = getIntProperty("dplot.window_height", 400);
	dplot_panel.setPreferredSize(new Dimension(ww, wh));
	dplot_panel.setMinimumSize(new Dimension(ww, wh));
	
	// --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  
	//    button panel
	// --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  

	//Create a file chooser which will be kept alive throughout the run
	fc = new JFileChooser();
	fc.setMinimumSize(new Dimension(450, 550));

	col_chooser = new JColorChooser();

	// create the menu bar.
	menu_bar = new JMenuBar();

	frame.setJMenuBar(menu_bar);

	
	// --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  
	//    status bar
	// --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  

	JPanel bottom_toolbar_panel = new JPanel();
	bottom_toolbar_panel.setBorder(BorderFactory.createEmptyBorder(2,0,0,0));
	{
	    GridBagLayout gridbag = new GridBagLayout();
	    bottom_toolbar_panel.setLayout(gridbag);
	    GridBagConstraints c = null;
	    Dimension but_size = new Dimension(18,18);

	    zoom_label =  new JLabel("1:1");
	    zoom_label.setFont(getSmallFont());
	    zoom_label.setPreferredSize(new Dimension(24, 16));
	    bottom_toolbar_panel.add(zoom_label);
	    c = new GridBagConstraints();
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    c.gridx = 0;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.EAST;
	    //c.weightx = 1.0;
	    //c.weighty = 1.0;
	    gridbag.setConstraints(zoom_label, c);

	    JButton zoom_out_but = new JButton(new ImageIcon(getImageDirectory() + "zoom-out.gif"));
	    zoom_out_but.setFont(getSmallFont());
	    zoom_out_but.setPreferredSize(but_size);
	    zoom_out_but.setMargin(new Insets(0,0,0,0));
	    zoom_out_but.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e)
		    {
			changeZoom( 1 );
		    }
		});
	    c = new GridBagConstraints();
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    c.gridx = 1;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.WEST;
	    //c.weightx = 1.0;
	    //c.weighty = 1.0;
	    gridbag.setConstraints(zoom_out_but, c);
	    bottom_toolbar_panel.add(zoom_out_but);



	    JButton zoom_in_but = new JButton(new ImageIcon(getImageDirectory() + "zoom-in.gif"));
	    zoom_in_but.setFont(getSmallFont());
	    zoom_in_but.setPreferredSize(but_size);
	    zoom_in_but.setMargin(new Insets(0,0,0,0));
	    zoom_in_but.addActionListener(new ActionListener() 
				   {
				       public void actionPerformed(ActionEvent e)
				       {
					   changeZoom(-1);
				       }
				   });
	    c = new GridBagConstraints();
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    c.gridx = 2;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.WEST;
	    //c.weightx = 1.0;
	    //c.weighty = 1.0;
	    gridbag.setConstraints(zoom_in_but, c);
	    bottom_toolbar_panel.add(zoom_in_but);

	    
	    but_size = new Dimension(9,18);
	    Box.Filler filler = new Box.Filler(but_size, but_size, but_size);
	    c = new GridBagConstraints();
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    c.gridx = 3;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.WEST;
	    //c.weightx = 1.0;
	    //c.weighty = 1.0;
	    gridbag.setConstraints(filler, c);
	    bottom_toolbar_panel.add(filler);


	    message_label = new JLabel("Starting....");
	    bottom_toolbar_panel.add(message_label);
	    
	    c = new GridBagConstraints();
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.gridx = 4;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.WEST;
	    c.weightx = 5.0;
	    //c.weighty = 1.0;
	    gridbag.setConstraints(message_label, c);


	   filter_alert_label = new JLabel("OK");
	    filter_alert_label.setFont(getSmallFont());
	    filter_alert_label.setForeground(Color.red);
	    bottom_toolbar_panel.add(filter_alert_label);
	    
	    c = new GridBagConstraints();
	    c.gridx = 5;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.EAST;
      	    c.fill = GridBagConstraints.REMAINDER;
	    //c.weightx = c.weighty = 1.0;
	    gridbag.setConstraints(filter_alert_label, c);

	    
	    filter_info_label = new JLabel("---");
	    filter_info_label.setFont(getSmallFont());
	    bottom_toolbar_panel.add(filter_info_label);
	    
	    c = new GridBagConstraints();
	    c.gridx = 6;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.EAST;
      	    c.fill = GridBagConstraints.REMAINDER;
	    //c.weightx = c.weighty = 1.0;
	    gridbag.setConstraints(filter_info_label, c);


	    


	    timer = new Timer(750, new ActionListener() 
			      {
				  public void actionPerformed(ActionEvent evt) 
				  {
				      if(apply_filter_menu_item != null)
				      {
					  apply_filter_menu_item.setEnabled(expr_data.filterIsOn());
				      }

				      if(filter_alert == true) 
				      { 
					  if((expr_data.filterIsOn()== true) && (dplot_panel.getUseFilter()==true))
					  {
					      filter_alert_label.setText("FILTER");
					      //filter_alert_label.setBackground(Color.white);
					      filter_alert = false;
					  }
				      }
				      else
				      {
					  filter_alert_label.setText("");
					  filter_alert = true;
				      }
				  }    
			      });
	    timer.start();

	}

	// --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  
	//    top-level gridbag panel
	// --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  --  

        GridBagLayout gridbag = new GridBagLayout();
	pane = new JPanel();
	pane.setPreferredSize(new Dimension(700, 400));
        pane.setLayout(gridbag);
        pane.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));

	{  
	    pane.add(dplot_panel);
	    GridBagConstraints c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.CENTER;
	    c.fill = GridBagConstraints.BOTH;
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = c.weighty = 1.0;
	    gridbag.setConstraints(dplot_panel, c);
	}  
	{ 
	    pane.add(bottom_toolbar_panel);
	    GridBagConstraints c = new GridBagConstraints();
	    c.fill = GridBagConstraints.BOTH;
	    c.gridx = 0;
	    c.gridy = 1;
	    gridbag.setConstraints(bottom_toolbar_panel, c);
	}
	
	/*
	for(int c=0; c < plugin_command_v.size(); c++)
	{
	    PluginCommand pc = (PluginCommand) plugin_command_v.elementAt(c);
	    System.out.println(pc.plugin_name + " ... " + pc.name);
	}
	*/

	frame.getContentPane().add(pane, BorderLayout.CENTER);

	
	
    }

 
    // ---------------- --------------- --------------- ------------- ------------
    // keyboard shortcut handling
    // ---------------- --------------- --------------- ------------- ------------
    
    // required to overcome a behaviour change in JDK1.4 in which
    // key events are no longer dispatched to the frame
    //
    private class CustomFrame extends JFrame implements AWTEventListener
    { 
	public CustomFrame( String title ) 
	{ 
	    super( title );
	    
	    Toolkit.getDefaultToolkit().addAWTEventListener(this,  KeyEvent.KEY_EVENT_MASK);
	} 


	public void eventDispatched(AWTEvent e) 
	{
	    KeyEvent ke = (KeyEvent) e;
	    
	    //System.out.println("key event:" + ke.getKeyCode() );
		
	    if( (( ke.getID() & KeyEvent.KEY_RELEASED )) == KeyEvent.KEY_RELEASED )
	    {
		//System.out.println("  released event:" + ke.getKeyCode() );

		if( is_java_1_4 )
		{
		    if( anyChildHasFocus( this ) )
		    {
			if( custom_menu.handleKeyEvent( ke ) == false )
			    handleKeyEvent( (KeyEvent) ke );
		    }
		}
		else
		{
		    if( ke.getComponent() == this )
		    {
			if( custom_menu.handleKeyEvent( ke ) == false )
			    handleKeyEvent( (KeyEvent) ke );
		    }
		}
	    }
	}

    }

    private boolean anyChildHasFocus( final Component comp )
    {
	if( comp.hasFocus() )
	    return true;

	if( comp instanceof Container )
	{
	    Component[] children = ( (Container)comp ).getComponents();
	    
	    if( children == null )
		return false;
	    
	    for(int c=0; c < children.length; c++)
		if( anyChildHasFocus( children[ c ] ) )
		    return true;
	}

	return false;
    }


    private void handleKeyEvent(KeyEvent e) 
    {
	switch(e.getKeyCode())
	{
	case KeyEvent.VK_SPACE:
	    //System.out.println("custom menu...");
	    dplot_panel.showMenu(null,-1,-1);
	    break;
	case KeyEvent.VK_PAGE_UP:
	    //System.out.println("page me up");
	    dplot_panel.scrollDisplayVert(1, false);
	    break;
	case KeyEvent.VK_PAGE_DOWN:
	    //System.out.println("page me down");
	    dplot_panel.scrollDisplayVert(1, true);
	    break;
	case KeyEvent.VK_UP:  // up-arrow
	    //System.out.println("line me up");
	    dplot_panel.scrollDisplayVert(0, false);
	    break;
	case KeyEvent.VK_DOWN: 
	    //System.out.println("line me down");
	    dplot_panel.scrollDisplayVert(0, true);
	    break;
	case KeyEvent.VK_LEFT:  // up-arrow
	    //System.out.println("line me up");
	    dplot_panel.scrollDisplayHor(0, true);
	    break;
	case KeyEvent.VK_RIGHT: 
	    //System.out.println("line me down");
	    dplot_panel.scrollDisplayHor(0, false);
	    break;
	case KeyEvent.VK_HOME:
	    dplot_panel.positionDisplayAtFrac(.0);
	    break;
	case KeyEvent.VK_END:
	    dplot_panel.positionDisplayAtFrac(1.0);
	    break;
	}
    }
 
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- --- zoom buttons
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void changeZoom( int delta )
    {
	dplot_panel.zoom( delta );
	zoom_label.setText("1:" +  dplot_panel.getZoomScale());
    }
    
    public void setZoom( int value )
    {
	dplot_panel.setZoom( value );
	zoom_label.setText("1:" +  dplot_panel.getZoomScale());
	
    }
    
    
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- --- apply filter can be controlled externally
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void setApplyFilter( boolean af )
    {
	apply_filter_menu_item.setSelected( af );
	dplot_panel.setUseFilter( af );
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    // window decorations

    private Image small_logo_image = null;
 
    public void decorateFrame( JFrame a_frame )
    {
	if( small_logo_image == null )
	{
	    ImageIcon ii = new ImageIcon( getImageDirectory() + "small-logo.gif" );
	    
	    small_logo_image = ii.getImage();
	    
	}

	a_frame.setIconImage( small_logo_image );
    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  menus
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  



    public void buildMenus()
    {
	
	JMenuItem menuItem;

	menu_bar.removeAll();

	// menus = new Hashtable();

	MenuListener li = new TopLevelListener();
	
	//
	// system menu
	//
	JMenu system_menu = new JMenu("System");
	system_menu.setMnemonic(KeyEvent.VK_S);
	
	//menus.put("System", system_menu);
	
	menuItem = new JMenuItem("Plugin Manager", KeyEvent.VK_P);
	menuItem.setToolTipText("Add,remove and update plugins");
	menuItem.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e)
		{
		    if(plugin_manager == null)
			plugin_manager = new PluginManager(maxdView.this);
		    
		    plugin_manager.showPluginManager();
		}
	    });
	system_menu.add(menuItem);
	system_menu.insertSeparator(1);
	    
	if(is_first_view)
	{
	    menuItem = new JMenuItem("Exit");
	    menuItem.setToolTipText("Exit the application");
	    menuItem.setMnemonic(KeyEvent.VK_X);
	    menuItem.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			cleanExit();
		    }
		});
	    system_menu.add(menuItem);
	}
	else
	{
	    menuItem = new JMenuItem("Close");
	    menuItem.setToolTipText("Close this window");
	    menuItem.setMnemonic(KeyEvent.VK_C);
	    menuItem.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			closeDown();
		    }
		});
	    system_menu.add(menuItem);
	}
	
	menu_bar.add(system_menu);


	
	//
	// display options menu
	//
	JMenu display_menu = new JMenu("Display");
	display_menu.setMnemonic(KeyEvent.VK_D);
	display_menu.getAccessibleContext().setAccessibleDescription("Settings for this data and viewer");
	menu_bar.add(display_menu);

	menuItem = new JMenuItem("Find",
				 KeyEvent.VK_F);
	menuItem.getAccessibleContext().setAccessibleDescription("Set the layout of this viewer");
	menuItem.getAccessibleContext().setAccessibleDescription("Goto a Gene, Probe or Spot name");
	menuItem.setToolTipText("Find Gene, Probe or Spot name or attributes");
	
	menuItem.addActionListener(new ActionListener() 
				   {
				       public void actionPerformed(ActionEvent e)
				       {
					   createFindDialog();
				       }
				   });
	display_menu.add(menuItem);

	menuItem = new JMenuItem("Layout",
				 KeyEvent.VK_L);
	//menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
	menuItem.getAccessibleContext().setAccessibleDescription("Set the layout of this viewer");
	menuItem.setToolTipText("Set the layout of this viewer");
	menuItem.addActionListener(new ActionListener() 
				   {
				       public void actionPerformed(ActionEvent e)
				       {
					   new DataPlotLayoutOptions(maxdView.this, dplot_panel);
				       }
				   });
	display_menu.add(menuItem);

	menuItem = new JMenuItem("Colours",
				 KeyEvent.VK_C);
	//menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
	menuItem.getAccessibleContext().setAccessibleDescription("Adjust the colours");
	menuItem.setToolTipText("Adjust the colours");
	menuItem.addActionListener(new ActionListener() 
				   {
				       public void actionPerformed(ActionEvent e)
				       {
					   new DataPlotColourOptions(maxdView.this);
				       }
				   });

	display_menu.add(menuItem);

	if(getOption(CanSpawnNewView))
	{
	    menuItem = new JMenuItem("New view",
				     KeyEvent.VK_N);
	    //menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
	    menuItem.getAccessibleContext().setAccessibleDescription("Create another view of this data");
	    menuItem.setToolTipText("Create another view of this data");
	    menuItem.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e)
		    {
			openNewView();
		    }
		});

	    display_menu.add(menuItem);
	}

	menuItem = new JMenuItem("Print",
				 KeyEvent.VK_P);
	//menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
	menuItem.getAccessibleContext().setAccessibleDescription("Print this display");
	menuItem.setToolTipText("Print this display");
	menuItem.addActionListener(new ActionListener() 
				   {
				       public void actionPerformed(ActionEvent e)
				       {
					   dplot_panel.printDisplay();
				       }
				   });

	display_menu.add(menuItem);

	display_menu.addSeparator();

	//a group of radio button menu items

	apply_filter_menu_item = new JCheckBoxMenuItem("Apply filter");
	apply_filter_menu_item.setSelected(dplot_panel.getUseFilter());
	apply_filter_menu_item.setToolTipText("Whether or not to apply the filter to this view");
	apply_filter_menu_item.setMnemonic(KeyEvent.VK_A);
	apply_filter_menu_item.addItemListener(new ItemListener() 
					       {
						   public void itemStateChanged(ItemEvent ite) 
						   {
						       dplot_panel.setUseFilter(ite.getStateChange() == ItemEvent.SELECTED);
						   }
					       });
	
	display_menu.add(apply_filter_menu_item);

	menu_bar.add(display_menu);





	//
	// now add menus as defined by the plugin manager
	//

	if(plugin_manager == null)
	    plugin_manager = new PluginManager(maxdView.this);

	DefaultMutableTreeNode menus = plugin_manager.getMenuRoot();

	if( menus != null )
	{
	    for(int m=0; m < menus.getChildCount(); m++)
	    {
		DefaultMutableTreeNode menu_node = (DefaultMutableTreeNode) menus.getChildAt(m);
		String menu_name = (String) menu_node.getUserObject();
		
		// System.out.println( "doing menu " + menu_name  );
		
		JMenu menu = new JMenu( menu_name );
		
		menu.addMenuListener(li);
		
		//menus.put( (String) menu.getUserObject(), menu );
		
		for(int c=0; c < menu_node.getChildCount(); c++)
		{
		    DefaultMutableTreeNode plugin_node = (DefaultMutableTreeNode) menu_node.getChildAt(c);
		    
		    // System.out.println( "  plugin " +  (String) plugin_node.getUserObject() );
		    
		    PluginInfo pinf = getPluginInfoFromName( (String) plugin_node.getUserObject() );
		    
		    
		    menuItem = new JMenuItem(pinf.name);
		    menuItem.setToolTipText(new String("Plugin: " + pinf.short_description));
		    menuItem.addActionListener(new ActionListener() 
			{
			    public void actionPerformed(ActionEvent e)
			    {
				quickrunPlugin(((AbstractButton)e.getSource()).getText());
			    }
			});
		    
		    menu.add(menuItem);
		}
	
		//
		// special handling for the filter menu
		//
		
		if( menu_name.equals("Filter") )
		{
		    filter_status_label = new JLabel("    No filters enabled");
		    int n_filters = menu.getMenuComponentCount();
		    if(n_filters > 1)
			menu.insertSeparator( n_filters  );
		    menu.add(filter_status_label);
		}
		
		
		menu_bar.add(menu);
	    }
	}


	//
	// help menu
	//
	JMenu help_menu = new JMenu("Help");
	help_menu.setMnemonic(KeyEvent.VK_H);

	menuItem = new JMenuItem("About", KeyEvent.VK_A);
	menuItem.addActionListener(new ActionListener() 
				   {
				       public void actionPerformed(ActionEvent e)
				       {
					   getUserSpecificHelpTopic("About");
				       }
				   });
	help_menu.add(menuItem);
	menuItem = new JMenuItem("Contents", KeyEvent.VK_C);
	menuItem.addActionListener(new ActionListener() 
				   {
				       public void actionPerformed(ActionEvent e)
				       {
					   getUserSpecificHelpTopic("Contents");  //Top
				       }
				   });

	help_menu.add(menuItem);

	menuItem = new JMenuItem("Menu Commands", KeyEvent.VK_M);
	menuItem.addActionListener(new ActionListener() 
				   {
				       public void actionPerformed(ActionEvent e)
				       {
					   getUserSpecificHelpTopic("Commands");
				       }
				   });
	help_menu.add(menuItem);

	menuItem = new JMenuItem("Popup Menu", KeyEvent.VK_O);
	menuItem.addActionListener(new ActionListener() 
				   {
				       public void actionPerformed(ActionEvent e)
				       {
					   getHelpTopic("Popup");
				       }
				   });
	help_menu.add(menuItem);

	menuItem = new JMenuItem("Plugins", KeyEvent.VK_P);
	menuItem.addActionListener(new ActionListener() 
				   {
				       public void actionPerformed(ActionEvent e)
				       {
					   getUserSpecificHelpTopic("PluginList");
				       }
				   });
	help_menu.add(menuItem);

	menuItem = new JMenuItem("Plugin Commands", KeyEvent.VK_L);
	menuItem.addActionListener(new ActionListener() 
				   {
				       public void actionPerformed(ActionEvent e)
				       {
					   getUserSpecificHelpTopic("PluginCommandsList");
				       }
				   });
	help_menu.add(menuItem);

	menuItem = new JMenuItem("Tutorials", KeyEvent.VK_T);
	menuItem.addActionListener(new ActionListener() 
				   {
				       public void actionPerformed(ActionEvent e)
				       {
					   getHelpTopic("Tutorial");
				       }
				   });
	help_menu.add(menuItem);

	menuItem = new JMenuItem("Method Reference", KeyEvent.VK_R);
	menuItem.addActionListener(new ActionListener() 
				   {
				       public void actionPerformed(ActionEvent e)
				       {
					   getHelpTopic("MethodRef");
				       }
				   });
	help_menu.add(menuItem);

	menuItem = new JMenuItem("What's New?", KeyEvent.VK_N);
	menuItem.addActionListener(new ActionListener() 
				   {
				       public void actionPerformed(ActionEvent e)
				       {
					   getHelpTopic("WhatsNew");
				       }
				   });
	help_menu.add(menuItem);

	menuItem = new JMenuItem("Web Site", KeyEvent.VK_W);
	menuItem.addActionListener(new ActionListener() 
				   {
				       public void actionPerformed(ActionEvent e)
				       {
					   helpPanelDisplayURL("http://www.bioinf.man.ac.uk/microarray/maxd/");
				       }
				   });
	help_menu.add(menuItem);

	/*
	menuItem = new JMenuItem("debug trigger");
	menuItem.addActionListener(new ActionListener() 
				   {
				       public void actionPerformed(ActionEvent e)
				       {
					   expr_data.countInVisibleClusters();
					   dplot_panel.repaint();
					   //double[] tmp = new double[expr_data.getNumSpots()];
					   //expr_data.addSet("test", tmp);
				       }
				   });
	menu.add(menuItem);
	*/

	menu_bar.add(Box.createHorizontalGlue());
	menu_bar.add(help_menu);

	system_menu.addMenuListener(li);
	help_menu.addMenuListener(li);
	display_menu.addMenuListener(li);
	

    }

    private class TopLevelListener implements MenuListener 
    {

	public void menuDeselected(MenuEvent e) {}
	{
	    //System.out.println("wobble!");
	}
	public void menuCanceled(MenuEvent e) {}
	{
	    //System.out.println("wobble!");
	}
	public void menuSelected(MenuEvent e) 
	{
	    dplot_panel.stopAnimation();
	    //System.out.println("wibble!");
	}
    }



 
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  menu handlers
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void openNewView()
    {
	try
	{
	    maxdView new_mv = new maxdView(null, expr_data);
	    new_mv.copyStuffFrom(maxdView.this);
	    new_mv.getBusy();
	}
	catch(java.rmi.RemoteException rmie)
	{
	}
    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  properties
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    long app_runs = 0;

    public long getAppRunCount() { return app_runs; }

    private Properties application_props = null;
    
    public Properties getProperties() { return application_props; }

    public String getProperty(String name, String def) { return application_props.getProperty(name, def); }
    public void   putProperty(String name, String val) { if(val != null) application_props.put(name, val); }

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

    public void putDoubleProperty(String name, double val)
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
	    double d = (Double.valueOf(s)).doubleValue();
	    return d;
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
    
    public boolean getBooleanProperty(String name, boolean def) 
    { 
	String s = application_props.getProperty(name);
	if(s == null)
	    return def;
	try
	{
	    return s.equals("true");
	}
	catch(NumberFormatException nfe)
	{
	    return def;
	}
    }
    public void putBooleanProperty(String name, boolean val)
    { 
	application_props.put(name, (val ? "true" : "false"));
    }

    
    public int colorToInt(Color c)
    {
	return (c.getRed() << 16) | (c.getGreen() << 8) | (c.getBlue());
    }
    public Color intToColor(int i)
    {
	return new Color(i);
    }
    

    public void loadProperties()
    {
	application_props = new Properties();
	try
	{
	    // alertMessage("looking for props file in '" + (getConfigDirectory() + "maxdView.config") + "'");
	    
	    FileInputStream in = new FileInputStream(getConfigDirectory() + "maxdView.config");
	    
	    if(in != null)
	    {
		application_props.load(in);
		in.close();

		
		try
		{
		    app_runs = new Long(application_props.getProperty("eview.runs")).longValue();
		}
		catch(java.lang.NumberFormatException nfe)
		{
		    app_runs = 0;
		}
		
		
		if(is_first_view)
		    app_runs++;
	    }
	}
	catch(java.io.IOException  ioe)
	{
	    app_runs = 1;
	}
    }

    private void makeConfigDirIfMissing()
    {
	try
	{
	    File ftest = new File(getConfigDirectory());
	    if(ftest.exists() == false)
	    {
		if(ftest.mkdir() != true)
		{
		    errorMessage("Unable to create configuration directory.\nMake sure you have write permission in the maxdView directory.\nmaxdView will still run, but your preferences and settings will not be saved.");
		}
	    }
	}
	catch(SecurityException  se)
	{
	    errorMessage("Unable to create configuration directory for security reaons\n  " + se);
	}
    }

    public void saveProperties()
    {
	if(is_first_view)
	    putProperty("maxdView.version", getAppTitle() );
		
	Color c = getBackgroundColour();
	int cnum = (c.getRed() << 16) | (c.getGreen() << 8) | (c.getBlue());
	application_props.put("eview.background_colour", String.valueOf(cnum));

	c = getTextColour();
	cnum = (c.getRed() << 16) | (c.getGreen() << 8) | (c.getBlue());
	application_props.put("eview.text_colour", String.valueOf(cnum));

	// application_props.put("eview.scrollbars", scrollbars_on_left ? "left" : "right");

	application_props.put("eview.runs", String.valueOf(app_runs));

	application_props.put("dplot.box_width",  String.valueOf(dplot_panel.getBoxWidth()));
	application_props.put("dplot.box_height", String.valueOf(dplot_panel.getBoxHeight()));
	application_props.put("dplot.col_gap",    String.valueOf(dplot_panel.getColGap()));
	application_props.put("dplot.name_col_gap",    String.valueOf(dplot_panel.getNameColGap()));
	application_props.put("dplot.row_gap",    String.valueOf(dplot_panel.getRowGap()));
	application_props.put("dplot.border_gap", String.valueOf(dplot_panel.getBorderGap()));
	application_props.put("dplot.window_width",  String.valueOf(dplot_panel.getWindowWidth()));
	application_props.put("dplot.window_height", String.valueOf(dplot_panel.getWindowHeight()));
	
	putIntProperty("dplot.font_size",  dplot_panel.getFontSize());
	putIntProperty("dplot.font_family",  dplot_panel.getFontFamily());
	putIntProperty("dplot.font_style",  dplot_panel.getFontStyle());
	
	final int nc = dplot_panel.getNumNameCols();
	putIntProperty("dplot.n_name_cols", nc);
	for(int co=0; co < nc; co++)
	{
	    putProperty("dplot.name_col_" + co + ".display", dplot_panel.getNameColSelection(co).getNames());
	    putBooleanProperty("dplot.name_col_" + co + ".trim", dplot_panel.getNameColTrimEnabled(co));
	    putIntProperty("dplot.name_col_" + co + ".len", dplot_panel.getNameColTrimLength(co));
	    putIntProperty("dplot.name_col_" + co + ".align", dplot_panel.getNameColAlign(co));
	}

	for(int p=0; p < 2; p++)
	{
	    String thing = (p == 0) ? "spot" : "meas";

	    putBooleanProperty("dplot.align_" + thing + "_glyphs",  dplot_panel.getAlignGlyphs(p));
	    putBooleanProperty("dplot.overlay_" + thing + "_root",  dplot_panel.getOverlayRootChildren(p));
	    putBooleanProperty("dplot.show_" + thing + "_branches", dplot_panel.getShowBranches(p));
	    putBooleanProperty("dplot.show_" + thing + "_glyphs",   dplot_panel.getShowGlyphs(p));
	    putIntProperty("dplot.branch_" + thing + "_scale",      dplot_panel.getBranchScale(p));
	}

	try
	{
	    // maybe the config directory doesn't exist
	    makeConfigDirIfMissing();

	    FileOutputStream out = new FileOutputStream(getConfigDirectory() + "maxdView.config");
	    application_props.save(out, app_title);

	    // for JDK 1.2 previous line should be;
	    //
	    // application_props.store(out, app_title);
	    //

	    out.close();
	}
	catch (java.io.IOException ioe)
	{
	    errorMessage("Unable to save application preferences");
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  version checker
    // --- --- ---  
    // --- --- ---    check the web site (in the background) to see if a new version is available
    // --- --- ---    
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void checkVersionHasChanged()
    {
	String last_ver = getProperty("maxdView.version", null);

	if(last_ver == null)
	    return;
	
	if(!last_ver.equals(app_title))
	{
	    if(infoQuestion("You are running a different version of maxdView than last time.\n" + 
			    "Rescanning the plugins is recommended.",
			    "Recan plugins", "Continue without rescan") == 0)
	    {
		scanPlugins();
	    }
	}
    }

    public void checkIsLatestVersion()
    {
	// don't bother with the check if this is the first time maxdView has run....

	String last_ver = getProperty("maxdView.version", null);

	if(last_ver == null)
	    return;
	
	// System.out.println("checkIsLatestVersion() state=" + vcd.vc_state);
	if(vcd == null)
	{
	    vcd = new VersionCheckDetails();
	}
	
	if(vcd.vc_state == VersionCheckNotStarted)
	{
	    
	    vcd.vc_state = VersionCheckRunning;
	    new VersionCheckerThread().start();
	    vcd.vc_timer = new Timer(1000, new ActionListener() 
		{
		    public void actionPerformed(ActionEvent evt) 
		    {
			checkIsLatestVersion();
		    }
		});
	    vcd.vc_timer.start();
	    return;
	}

	if(vcd.vc_state == VersionCheckRunning)
	{
	    return;
	}

	if(vcd.vc_state == VersionCheckDone)
	{
	    vcd.vc_timer.stop();

	    if(vcd.has_alert)
	    {
		// System.out.println("checkIsLatestVersion() alert id = " + vcd.alert_id_s);
		
		if(getProperty("maxdView.alert_message_" + vcd.alert_id_s, null) == null)
		{
		    if(errorMessage(vcd.alert_s, true))
		    {
			putProperty("maxdView.alert_message_" + vcd.alert_id_s, "dontshow");
		    }
		}
	    }

	    if(vcd.is_newer)
	    {
		infoMessage("A newer version of maxdView is available\n\n" + 
			    "Version: " + vcd.latest_s + "\n" +
			    "Released: " + vcd.released_s + "\n" +
			    ((vcd.descr_s == null) ? "" : vcd.descr_s) + "\n" + 
			    "\nThe installer application 'maxdSetup'\n" + 
			    "(also available on the 'maxd' website) can be\n" +
			    "used to simplify upgrading to the latest version.");
	    }

	    vcd.vc_state = VersionCheckDoneAndUserInformed;

	    return;
	}
    }

    private final static int VersionCheckNotStarted            = 0;
    private final static int VersionCheckRunning               = 1;
    private final static int VersionCheckDone                  = 2;
    private final static int VersionCheckDoneAndUserInformed   = 3;
    private final static int VersionCheckFailed                = 4;

    private class VersionCheckDetails
    {
	int vc_state;
	Timer vc_timer;
	String vc_new_info;

	boolean has_alert;
	boolean is_newer;

	String latest_s;
	String released_s;
	String descr_s;
	String alert_s;
	String alert_id_s;

	public VersionCheckDetails()
	{
	    vc_state = VersionCheckNotStarted;
	    has_alert = is_newer = false;
	    latest_s = alert_s = alert_id_s =released_s = descr_s = null;
	}

    }

    private VersionCheckDetails vcd = null;
   
    private class VersionCheckerThread extends Thread
    {
	public void run()
	{
	    boolean result = test("http://www.bioinf.man.ac.uk/microarray/maxd/dist/maxdView_version.dat");

	    if(!result)
		result = test("http://localhost/maxd/dist/maxdView_version.dat");
	    
	    if(!result)
	    {
		vcd.vc_state = VersionCheckFailed;
		if(vcd.vc_timer != null)
		    vcd.vc_timer.stop();
		System.out.println("WARNING: Version Check :: unable to contact home\n");
	    }
	    else
	    {
		vcd.vc_state = VersionCheckDone;
	    }
	}

	private boolean test( String loc )
	{
	    try
	    {
		URL version = new URL(loc);
		
		BufferedReader in = new BufferedReader(new InputStreamReader(version.openStream()));
		
		String input_line;

		while ((input_line = in.readLine()) != null)
		{
		    // System.out.println(input_line);

		    try
		    {
			if(input_line.startsWith("latest:"))
			{
			    vcd.latest_s = input_line.substring(7).trim();
			    
			    if(isNewer(vcd.latest_s))
				vcd.is_newer = true;
			}
			
			if(input_line.startsWith("released:"))
			    vcd.released_s = input_line.substring(9).trim();
			
			if(input_line.startsWith("description:"))
			{
			    if(vcd.descr_s == null)
				vcd.descr_s = input_line.substring(12);
			    else
				vcd.descr_s += ("\n" + input_line.substring(12));
			}
			
			if(input_line.startsWith("alert: "))
			{
			    String alert_string = input_line.substring(7);
			    int index = alert_string.indexOf(" ");
			    if(index > 0)
			    {
				String alert_version = alert_string.substring(0, index);
			    
				// System.out.println(":::: av='" + alert_version + "'");
				
				if(app_title.startsWith("maxdView " + alert_version))
				{
				    String text = alert_string.substring(index);
				    
				    if(vcd.alert_id_s == null)
				    {
					vcd.alert_id_s = text;
				    }
				    else
				    {
					if(vcd.alert_s == null)
					    vcd.alert_s = text;
					else
					    vcd.alert_s += "\n" + text;
				    }
				    
				    vcd.has_alert = true;
				}
			    }
			}
		    }
		    catch(IndexOutOfBoundsException iooe)
		    {
			// dont look at me...
		    }
		    catch(NullPointerException npe)
		    {
			// shrug....
		    }
		}
		in.close();
		return true;
	    }
	    catch(java.net.MalformedURLException murle)
	    {
	    }
	    catch(java.io.IOException ioe)
	    {
	    }
	    return false;
	}

	private boolean isNewer( String version_str )
	{
	    int i1     = version_str.indexOf('.');
	    int i2     = version_str.indexOf('.',i1+1);
	    int islash = version_str.indexOf('/',i2+1);

	    final boolean debug_version_check = false;

	    if((i1 < 0) || (i2 < 0))
	    {
		System.out.println("isNewer(): cannot parse version number '" + version_str + "'");
	    }
	    else
	    {
		try
		{
		    int v_v1 = (new Integer(version_str.substring(0,i1))).intValue();
		    int v_v2 = (new Integer(version_str.substring(i1+1,i2))).intValue();

		    int v3_end = (islash > 0) ? islash : version_str.length();

		    int v_v3 = (new Integer(version_str.substring(i2+1,v3_end))).intValue();

		    String v_var = (islash > 0) ? version_str.substring(islash+1) : "";

		    // debug

		    if(debug_version_check)
		    {
			String current = version_series + "." + version_major + "." + version_minor;
			if((  version_prerelease_variant != null)&& ( version_prerelease_variant.length() > 0 ))
			    current += "/" + version_prerelease_variant;
			System.out.println("isNewer(): comparing " + current + 
					   " (current) with " + version_str + " (test)");
			System.out.println("isNewer(): parsed:" + 
					   " se=" + v_v1 + " ma=" + v_v2 + 
					   " mi=" + v_v3 + " pv=" + v_var);
			
		    }

		    //end debug

		    if(v_v1 > version_series)
			return true;
		    if(v_v1 < version_series)
			return false;
		    
		    if(v_v2 > version_major)
			return true;
		    if(v_v2 < version_major)
			return false;
		    
		    if(v_v3 > version_minor)
			return true;
		    if(v_v3 < version_minor)
			return false;

		    // now check the variant string

		    if( v_var.length() == 0 )  // the 'empty' variant is newer than any variant
		    {
			if( version_prerelease_variant.length() == 0 )
			    return false;
			else
			    return true;
		    }
		    else
		    {
			if( version_prerelease_variant.length() == 0 )
			    return false;
			else
			{
			    // now check variant info:

			    // first the inital 'letter'  (which is 'a' for alpha versions and 'b' for beta)

			    char code_letter_test = v_var.charAt(0);
			    char code_letter_this = version_prerelease_variant.charAt(0);

			    if(code_letter_test > code_letter_this)    
				return true;

			    if(code_letter_test < code_letter_this) 
				return false;

			    // now it's down to the digits...

			    int code_digit_test =  (new Integer(v_var.substring(1))).intValue();
			    int code_digit_this =  (new Integer(version_prerelease_variant.substring(1))).intValue();
			    
			    if(code_digit_test > code_digit_this)
				return true;
			    if(code_digit_test < code_digit_this)
				return false;
			    
			}
		    }
		    
		}
		catch(NumberFormatException nfe)
		{
		    System.out.println("isNewer(): cannot parse version number '" + version_str + "'");
		}
	    }

	    return false;
	}

    }
    
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  plugin launcher
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  


    //
    // modifications in 0.8.9
    //
    //   in order to integrate with ISYS the plugin directory needs to be made
    //   relative rather than absolute
    //
    //   the getPluginsDirectory() method specifies the top level path, this
    //   must be able change between runs without having to update the plugin config file
    //
    //   (i.e. the same plugins should be available running under ISYS as when running 
    //    in standalone mode _without_ having to 'rescan plugins')
    //
    //
    //   the plugins config file now stores the location of the plugin class(es)
    //   relative to the "plugins/" directory
    //
    //   this directory can now be changed without the plugins config file breaking
    //
    //   when plugins are loaded the getPluginsDirectory() is prefixed to the location
    //   found in the plugins config file
    //
    //   when rescanning, the scan now starts at getPluginsDirectory() 
    //

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  plugin launcher
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public PluginInfo getPluginInfoFromName( String plugin_name )
    {
	for(int p=0; p < plugin_info.size(); p++)
	{
	    try
	    {
		PluginInfo pinf = (PluginInfo) plugin_info.elementAt(p);
		if( pinf.name.equals( plugin_name ) )
		{
		    return pinf;
		}
	    }
	    catch( Exception ex )
	    {
		return null;
	    }
	}

	return null;
    }


    public PluginInfo getPluginInfoFromClassName( String class_name )
    {
	for(int p=0; p < plugin_info.size(); p++)
	{
	    try
	    {
		PluginInfo pinf = (PluginInfo) plugin_info.elementAt(p);

		if( pinf.class_name.equals( class_name ) )
		{
		    return pinf;
		}
	    }
	    catch( Exception ex )
	    {
		return null;
	    }
	}

	return null;
    }


    private void pluginReportError( String root_path, String class_name, String message )
    {
	{
	    String e_msg = "Error: \"" + message + "\"\n  while trying to loading class \"" + 
	                   class_name + "\"\n  from \"" + root_path +
	                   "\"\n\nPlugin directory is \"plugins/\"\n";
	    
	    String s_msg = "\nMake sure the class file is in the right place\n" +
	                   "and then try the \"Rescan plugins\" option\n";

	    String full_msg = e_msg + s_msg;

	    if(errorQuestion(full_msg, "Rescan plugins", "OK") == 0)
		scanPlugins();
	}
    }

    
    // move the plugin lauching code to a thread in case it is the source
    // of problems starting the SpacePlot ?
    //
    
    private void quickrunPlugin(String class_name)
    {
	new PluginRunnerThread(class_name).start();
    }

    private class PluginRunnerThread extends Thread
    {
	private String class_name;
	public PluginRunnerThread(String cn)
	{
	    class_name = cn;
	}
	public void run()
	{
	    if(plugin_debug_run)
		System.out.println("+++ trying to quickrun " + class_name + "...");
	    
	    PluginInfo pinf = getPluginInfoFromName( class_name );

	    try
	    {
		if( pinf != null )
		{
		    loadAndStartPlugin( pinf, true );
		}
		else
		{
		    System.err.println("WARNING: unable to locate the plugin '" + class_name );
		}
	    }
	    catch( OutOfMemoryError oome )
	    {
		System.err.println( "PluginRunnerThread.run(): OutOfMemoryError" );
	    }

	    if(plugin_debug_run)
		System.out.println("+++ quickrun thread for " + class_name + " finished");
	    
	}
    }

    public Plugin runPlugin(String class_name)
    {
	return  runPlugin(class_name, true);
    }

    public Plugin loadPlugin(String class_name)
    {
	return  runPlugin(class_name, false);
    }

    public Plugin runPlugin(String class_name, boolean start_it)
    {
	if(plugin_debug_run)
	    System.out.println("trying to run " + class_name + "...");

	PluginInfo pinf = getPluginInfoFromName( class_name );
	
	if( pinf != null )
	{
	    if(plugin_debug_run)
		System.out.println("executing " + pinf.class_name );
	    
	    return loadAndStartPlugin( pinf, start_it );
		    
	    // how do we detect when they go away?
	    //
	    //int count = ((Integer) plugin_is_running.elementAt(p)).intValue();
	    //plugin_is_running.setElementAt(p, new Integer(count+1));
	}

	// if we got here, then the name was not recognised...

	// find the plugin with the closest name
	
	String[] opts = new String[ plugin_info.size() ];
	for(int o=0; o < plugin_info.size(); o++)
	{
	    opts[o] = ((PluginInfo) plugin_info.elementAt(o)).name;
	}

	String match = bestMatch( class_name, opts );

	alertMessage("Plugin '" + class_name + "' not found\n\n" +
		     "(did you mean '" + match + "' ?)" );
	
	return null;
    }

    public Plugin loadAndStartPlugin( PluginInfo pinf, boolean start_it )
    {
	// attempt to load the class dynamically from the
	// specified directory (this is what makes it hard,
	// as the default loader will only load from the
	// CLASSPATH)
	//

	Plugin plugin = null;
	Class plugin_definition;
	
	final String plugin_sig = "Plugin loader: loadAndStartPlugin(): class "  + pinf.class_name  + ": ";
	
	Class[]  plugin_args_class = new Class[] { maxdView.class };
	Object[] plugin_args       = new Object[] { this };

	Constructor plugin_args_constructor;
	
	try 
	{

	    CustomClassLoader enccl = new CustomClassLoader( pinf.root_path );
	    
	    plugin_definition = enccl.findClass( pinf.class_name );

			
/*
	    // extract the path to the plugin directory from the full
	    // class name of the principal class of this plugin
	    //
	    int path_end = full_name.lastIndexOf(File.separatorChar);

	    plugin_current_directory = full_name.substring(0, path_end+1);

	    // CustomClassLoader loader = new CustomClassLoader();
	    custom_class_loader = new NewCustomClassLoader( this.getClass() );

	    plugin_definition = custom_class_loader.loadClass( full_name, class_name);
	    
	    if(plugin_debug_run)
		System.out.println("using " + plugin_current_directory + " as local plugin directory");
*/

	    if(plugin_definition != null)
	    {
 		// get a handle to the constructor method of the plugin
		plugin_args_constructor =  plugin_definition.getConstructor(plugin_args_class);

		if(plugin_args_constructor != null)
		{
		    // plugin_definition.getConstructor(plugin_args_class);

		    // calls newInstance()
		    plugin = (Plugin) createObject(plugin_args_constructor, plugin_args);
		    
		    if(plugin != null)
		    {
			// initialise the plugin....
			
			setMessage("Starting plugin " + pinf.class_name);
			
			if(start_it)
			    plugin.startPlugin();

			//System.out.println("plugin loaded ok");
		    }
		    else
			pluginReportError( pinf.root_path, pinf.class_name,
					  "couldn't instantiate object (whilst trying to load plugin)");
		}
		else
		    pluginReportError( pinf.root_path, pinf.class_name,
				      "couldn't locate correctly formed constructor method (whilst trying to load plugin)");
	    }
	    else
		pluginReportError( pinf.root_path, pinf.class_name,
				  "couldn't load class (whilst trying to load plugin)");

	    
	} 
	catch (OutOfMemoryError e)
	{
	    e.printStackTrace();
	    pluginReportError( pinf.root_path, pinf.class_name, "Out of memory! (use Java's -Xmx flag to allocate more)" ); 
	    
	}
	catch (NullPointerException e)
	{
	    e.printStackTrace();
	    pluginReportError( pinf.root_path, pinf.class_name, "couldn't start plugin"); 
	    
	}
	catch (NoSuchMethodException e) 
	{
	   pluginReportError( pinf.root_path, pinf.class_name, "couldn't locate correctly formed method\n(" + e + ")");
	}
	catch (Exception e)
	{
	    e.printStackTrace();
	    errorMessage("Exception thrown by plugin " + pinf.class_name + "\n  " + e);
	}
	catch (Error err)
	{
	    err.printStackTrace();
	    errorMessage("Error thrown by plugin " + pinf.class_name + "\n  " + err);
	}

	return plugin;
    }

    public void sendCommandToPlugin( String target, String command, String[] args )
    {
	// find the full name corresponding to this class name
	//
	String class_name = null;
	String full_name = null;
	
	PluginInfo pinf = getPluginInfoFromName( target );

	/*
	for(int p=0; p < plugin_full_names.size(); p++)
	{
	    PluginInfo pinf = (PluginInfo) plugin_info.elementAt(p);
	    if(pinf.name.equals(target))
	    {
		if(plugin_debug_run)
		    System.out.println("executing " + (String)plugin_full_names.elementAt(p)  + 
				       " (" + (String)plugin_class_names.elementAt(p) + ")");
		
		class_name = (String) plugin_class_names.elementAt(p);
		full_name  = (String) plugin_full_names.elementAt(p);
		break;
	    }
	}
	*/

	if( pinf == null )
	{
	    alertMessage("Unable to locate plugin '" + target + "'");
	    return;
	}
	//
	// attempt to load the class dynamically from the
	// specified directory (this is what makes it hard,
	// as the default loader will only load from the
	// CLASSPATH)
	//

	Plugin plugin;
	Class plugin_definition;
	
	final String plugin_sig = "Plugin loader: sendCommandToPlugin(): class "  + full_name  + ": ";

	Class[]  plugin_args_class = new Class[] { maxdView.class };
	Object[] plugin_args       = new Object[] { this };

	Constructor plugin_args_constructor;
	
	try 
	{
	    CustomClassLoader enccl = new CustomClassLoader( pinf.root_path );
	    
	    plugin_definition = enccl.findClass( pinf.class_name );

	    if(plugin_definition != null)
	    {
		// get a handle to the constructor method of the plugin
		plugin_args_constructor =  plugin_definition.getConstructor(plugin_args_class);

		if(plugin_args_constructor != null)
		{
		    // plugin_definition.getConstructor(plugin_args_class);

		    // calls newInstance()
		    plugin = (Plugin) createObject(plugin_args_constructor, plugin_args);
		    
		    if(plugin != null)
		    {
			// send the command....
			
			// plugin.runCommand(command, args, new CommandSignal() { public void signal() { /*System.out.println("command done signal....");*/ } } );
			
			runCommand( plugin, command, args, true );
		    }
		    else
			pluginReportError( pinf.root_path, pinf.class_name, "couldn't instantiate object");
		}
		else
		    pluginReportError( pinf.root_path, pinf.class_name, "couldn't locate correctly formed constructor method");
	    }
	    else
		pluginReportError( pinf.root_path, pinf.class_name, "couldn't load class");

	} 
	catch (NullPointerException e)
	{
	    e.printStackTrace();
	    pluginReportError( pinf.root_path, pinf.class_name, "couldn't start plugin"); 
	    
	}
	catch (NoSuchMethodException e) 
	{
	   pluginReportError( pinf.root_path, pinf.class_name, "couldn't locate correctly formed method\n(" + e + ")");
	}
	catch (Exception e)
	{
	    e.printStackTrace();
	    errorMessage("Exception thrown by plugin " +  pinf.class_name + "\n  " + e);
	}
    }

    public static Object createObject( Constructor constructor, Object[] arguments ) 
    {
	
	//System.out.println ("Constructor: " + constructor.toString());

	Object object = null;

	final String plugin_sig = "Plugin loader: createObject(): ";

	try 
	{
	    object = constructor.newInstance(arguments);
	    //System.out.println ("Object: " + object.toString());
	    return object;
	} 
	catch (InstantiationException e)
	{
	    System.out.println(plugin_sig + e);
	} 
	catch (IllegalAccessException e) 
	{
	    System.out.println(plugin_sig + e);
	} 
	catch (IllegalArgumentException e)
	{
	    System.out.println(plugin_sig + e);
	} 
	catch (InvocationTargetException e) 
	{
	    System.out.println(plugin_sig + e.getTargetException().toString());
	}
	return object;
    }


 



    // ==================================================================================
    // ==  new class loader  ============================================================
    // ==================================================================================

    //private NewCustomClassLoader custom_class_loader;

    private Vector custom_classpaths = null;
    private Vector custom_jarfiles = null;
    
    private HashSet cache_jarfiles = new HashSet();

    // ==========================================================
/*
    public void showClassSearchLocations(  )
    {
	
	System.out.println( "custom loader locations:");
	
	if(custom_classpaths != null)
	    for(int s=0; s < custom_classpaths.size(); s++)
		System.out.println( "path:"  + (String) custom_classpaths.elementAt( s ));
	else
	    System.out.println( "no paths");

	if(custom_jarfiles != null)
	    for(int s=0; s < custom_jarfiles.size(); s++)
		System.out.println( "jarfile:"  + (String) custom_jarfiles.elementAt( s ));
	else
	    System.out.println( "no jarfiles");
    }
*/  
    // ==========================================================
/*
    public void addClassSearchPath( String path )
    {
	if(custom_classpaths == null)
	    custom_classpaths = new Vector();

	//make sure the path isn't already in the list
	for(int s=0; s < custom_classpaths.size(); s++)
	{
	    if(path.equals( (String) custom_classpaths.elementAt( s )))
		return;
	}
	
	custom_classpaths.insertElementAt( path, 0 );

	// showClassSearchLocations(  );
    }

    public void removeClassSearchPath( String path )
    {
	if(custom_classpaths == null)
	    return;
	int s=0;
	while(s < custom_classpaths.size())
	{
	    if(path.equals( (String) custom_classpaths.elementAt( s )))
		custom_classpaths.removeElementAt(s);
	    else
		s++;
	}
    }
*/
    // ==========================================================
    /*  
    public void addClassSearchJarFile( String jarfile )
    {
	addClassSearchJarFile( jarfile, true );
    }

    public void addClassSearchJarFile( String jarfile, boolean cache_it )
    {
	if(custom_jarfiles == null)
	    custom_jarfiles = new Vector();

	if(cache_it)
	    cache_jarfiles.add( jarfile );
	else
	    cache_jarfiles.remove( jarfile );

	//make sure the path isn't already in the list
	for(int s=0; s < custom_jarfiles.size(); s++)
	{
	    if( jarfile.equals( (String) custom_jarfiles.elementAt( s )))
		return;
	}

	custom_jarfiles.insertElementAt( jarfile, 0 );

	// showClassSearchLocations(  );
    }

    public void removeClassSearchJarFile( String jarfile )
    {
	if(custom_jarfiles == null)
	    return;
	int s=0;
	while(s < custom_jarfiles.size())
	{
	    if(jarfile.equals( (String) custom_jarfiles.elementAt( s )))
		custom_jarfiles.removeElementAt(s);
	    else
		s++;
	}
    }
    */
    // ==========================================================
/*
    public void setJarFileCacheMode( String jarfile, boolean cache_it )
    {
	
    }
*/  
     
    // ==========================================================

    // ==========================================================

/*
    public Class loadClassFromPath( String path, String class_name ) throws MalformedURLException, java.lang.ClassNotFoundException
    {
	// Create an array of URLs 
	// From directory name specified as args[0]
	URL[] urlList = { new File( path ).toURL() };
	
	// Create a URLClassLoader
	ClassLoader loader = new URLClassLoader( urlList, this.getClass().getClassLoader() );
	
	// Load class from class loader
	// Use args[1] as class name
	return loader.loadClass ( class_name );
    }
*/

    // ==========================================================


    public class CustomClassLoader extends java.net.URLClassLoader
    {
	public CustomClassLoader( final String path, final ClassLoader parent ) throws MalformedURLException
	{
	    super( new URL[] { new File( path ).toURL() }, parent );

	    //System.out.println( "CustomClassLoader(): creating loader with path '" + path + "'" );

	}


	public CustomClassLoader( final String path ) throws MalformedURLException
	{
	    super( new URL[] { new File( path ).toURL() }, (maxdView.this).getClass().getClassLoader() );

	    //System.out.println( "CustomClassLoader(): creating loader with path '" + path + "' and parent..." );
	}
	
	public void addPath( String path ) throws MalformedURLException
	{
	    URL new_path = new File( path ).toURL();

	    URL[] paths = getURLs();
	    
	    for( int u=0; u < paths.length; u++ )
		if( paths[ u ].equals( new_path ) )
		{
		    System.out.println( "CustomClassLoader(): path '" + path + "' already exists..." );
		    return;
		}

	    addURL( new_path );
	}

	public Class findClass( String class_name )
	{
	    try
	    {
		//if(plugin_debug_load)
		//    System.out.println( "CustomClassLoader(): trying to load " + class_name + " with path " + getURLs()[0] );
		
		return super.findClass( class_name );
	    }
	    catch( java.lang.LinkageError le )
	    {
		System.out.println( "CustomClassLoader(): failed to load " + class_name + "\n" + le  );
		return null;
	    }
	    catch( ClassNotFoundException cnfe )
	    {
		if(plugin_debug_load)
		{

		    System.out.println( "CustomClassLoader(): failed to load " + class_name + ", paths: "  );
		    describe();

		    cnfe.printStackTrace();
		}

		return null;
	    }
	}


	public void describe( )
	{
	    ClassLoader cl = this;
	    int parentage = 1;

	    while( cl != null )
	    {
		if( cl instanceof URLClassLoader )
		{
		    URL[] paths = ((URLClassLoader)cl).getURLs();
		    
		    for( int u=0; u < paths.length; u++ )
			System.out.println( "     " + parentage + "-" + (u+1) + "/" + (paths.length) + " : " + paths[ u ] );
		}
		else
		{
		    System.out.println( "     " + parentage + "-" + cl );
		}

		cl = cl.getParent();

		parentage++;
	    }
		
	}

    }
    
    
    // ==================================================================================
    // ==  plugin scanning  =============================================================
    // ==================================================================================

    //
    //
    // modified 24/06/2004 to be more friendly in multi-user shared installations
    //
    //
    //  first of all scan the '$INSTALL/plugins' subdir
    //   then scan the '$USER/plugins' and override any
    //   references to plugins with names that have already
    //   been seen.
    //
    // ==================================================================================



    // get the directory that the name plugin is found in
    //
    public String getPluginDirectory( String plugin_name )
    {
	PluginInfo pinf = getPluginInfoFromName( plugin_name );

	if( pinf != null )
	{
	    return pinf.root_path;
	}
	else
	{
	    return null;
	}
    }
        
   // determine whether the specified class implements the Plugin interface
    //
    // create an object, and call the getPluginInfo method to get the info
    // object for this plugin 
    //
    public PluginInfo testIsPlugin( String root_path, String class_name, Class plugin_definition )
    {
	Plugin plugin;
	
	//final String plugin_sig = "Plugin loader: testIsPlugin(): class "  + class_name  + ": ";

	Class[]  plugin_args_class = new Class[] { maxdView.class };
	Object[] plugin_args       = new Object[] { this };

	Constructor plugin_args_constructor;
	
	final String plugin_interface_name = "Plugin";
	
	// CustomClassLoader loader = new CustomClassLoader();
	
	// try 
	{
	    //if(plugin_debug_load)
	    // {
	    //	System.out.println("      ... testing potential plugin '" + class_name + "'  [class name]" );
	    //	System.out.println("      ...     '" + root_path+ "'  [root path]" );
	    //}

	    
//	    plugin_definition = custom_class_loader.loadClass( root_path + full_name, class_name );

	     
	    if(plugin_definition != null)
	    {
		if(plugin_debug_load)
		    System.out.println("       ... loaded as (" + plugin_definition.getName() + ")");

		try
		{
		    Class[] interfaces = plugin_definition.getInterfaces();
		    
		    if( ( interfaces != null ) && ( interfaces.length > 0 ) )
		    {
			if(plugin_debug_load)
			    System.out.println( "       ... interfaces found" );
			
			for(int i=0; i< interfaces.length; i++)
			{
			    if(plugin_debug_load)
				System.out.println( "         ... " + " implements " + interfaces[i].getName() );

			    if(interfaces[i].getName().equals(plugin_interface_name))
			    {
				// call the pluginType() method to find out what sort
				// of plugin this is (i.e. where in the menu
				// hierarchy it should go)
				
				Method type_method;

				try
				{
				    // get a handle to the constructor method of the plugin
				    plugin_args_constructor =  plugin_definition.getConstructor(plugin_args_class);
				    
				    // calls newInstance() and makes an object
				    plugin = (Plugin) createObject(plugin_args_constructor, plugin_args);
				    
				    if(plugin != null)
				    {
					// get a handle to the pluginType method of the class
					type_method = plugin_definition.getMethod("getPluginInfo", null);
					
					// and call this method
					PluginInfo result = (PluginInfo) type_method.invoke(plugin, null);
					
					// System.out.println("plugin " + class_name + " is " +  result);
					return result;
				    }
				    else
				    {
					// the object couldn't be constructed for some reason...
					//
					logBrokenPlugin( class_name, "Unable to construct object");
					return null;
				    }
				}
				catch (NoClassDefFoundError e)
				{
				    logBrokenPlugin(class_name, "Class not found: " + e.toString());
				    //pluginReportError(full_name, class_name, e.toString());
				}
				catch (NoSuchMethodException e) 
				{
				    logBrokenPlugin(class_name, "Method not found: " + e.toString());
				    e.printStackTrace();
				    //pluginReportError(full_name, class_name, e.toString());
				}
				catch (java.lang.reflect.InvocationTargetException e)
				{
				    logBrokenPlugin(class_name, "Plugin threw exception:: " + 
						    e.getTargetException().toString());
				    e.printStackTrace();
				    //pluginReportError(full_name, class_name, e.getTargetException().toString());
				}
				catch (java.lang.IllegalAccessException e)
				{
				    logBrokenPlugin(class_name, "Illegal Access: " + e.toString());
				    e.printStackTrace();
				    //pluginReportError(full_name, class_name, e.toString());
				}
				catch (java.lang.Exception e)
				{
				    logBrokenPlugin(class_name, "Unhandled exception: " + e.toString());
				    e.printStackTrace();
				    //pluginReportError(full_name, class_name, e.toString());
				}
				catch (java.lang.Error e)
				{
				    logBrokenPlugin(class_name, "Unhandled error: " + e.toString());
				    e.printStackTrace();
				    //pluginReportError(full_name, class_name, e.toString());
				}
			    }
			}
		    }
		}
		catch(java.lang.IncompatibleClassChangeError e)
		{
		    logBrokenPlugin(class_name, "Unimplemented interface method");
		    //pluginReportError(full_name, class_name, "plugin has Unimplemented interface method");
		}
		catch(Exception e)
		{
		    logBrokenPlugin(class_name, "Unhandled exception");
		    //pluginReportError(full_name, class_name, "plugin threw an exception: " + e.toString());
		}
	    }
	    else
	    {
		logBrokenPlugin(class_name, "Unable to load class (name=\"" + class_name + "\")");

		/*
		if(plugin_debug_load)
		    pluginReportError(full_name, class_name, "unable to load");
		*/

	    }
	    return null;
	} 
    }

    private void logBrokenPlugin(String name, String reason)
    {
	if(name.indexOf('$') < 0)
	{
	    plugin_plugins_that_would_not_start.addElement(name);
	    plugin_plugins_why_it_would_not_start.addElement(reason);
	}
    }

    public PluginCommand[] getPluginCommands( Class plugin_definition, PluginInfo pinf )
    {
	Plugin plugin;
	
	Class[]  plugin_args_class = new Class[] { maxdView.class };
	Object[] plugin_args       = new Object[] { this };

	Constructor plugin_args_constructor;
	
	final String plugin_interface_name = "maxdView$Plugin";
	
	//
	// it would be very helpful if getPluginDirectory() could return tyhe correct
	// result at this point so that plugins such as Normalise can figure out
	// their commands dynamically (i.e. based on the contents of their directory)
	//

	//CustomClassLoader loader = new CustomClassLoader();
	
	final String state = "Whilst checking the Plugin commands:\n";

	if(plugin_definition != null)
	{
	    Method type_method;
	    try
	    {
		// get a handle to the constructor method of the plugin
		plugin_args_constructor =  plugin_definition.getConstructor(plugin_args_class);
		
		// calls newInstance() and makes an object
		plugin = (Plugin) createObject(plugin_args_constructor, plugin_args);
		
		if(plugin != null)
		{
		    // get a handle to the pluginType method of the class
		    type_method = plugin_definition.getMethod("getPluginCommands", null);
		    
		    // and call this method
		    PluginCommand[] result = (PluginCommand[]) type_method.invoke(plugin, null);
		    
		    // fill in the missing command details...
		    if(result != null)
		    {
			for(int c=0; c < result.length; c++)
			{
			    result[c].plugin_name = pinf.name; 
			}
		    }

		    // System.out.println("plugin " + class_name + " is " +  result);
		    return result;
		}
	    }

	    // need to defer these error messages until the scan has completed

	    catch (NoClassDefFoundError e)
	    {
		logBrokenPlugin( pinf.class_name, state + "Class not found: " + e.toString());
		e.printStackTrace();
		// pluginReportError(full_name, class_name, e.toString() + state);
	    }
	    catch (NoSuchMethodException e) 
	    {
		logBrokenPlugin( pinf.class_name, state + "Method not found: " + e.toString());
		e.printStackTrace();
		// pluginReportError(full_name, class_name, e.toString() + state);
	    }
	    catch (java.lang.reflect.InvocationTargetException e)
	    {
		logBrokenPlugin( pinf.class_name, state + "InvocationTargetException: " + e.toString());
		e.printStackTrace();
		// pluginReportError(full_name, class_name, e.getTargetException().toString() + state);
	    }
	    catch (java.lang.IllegalAccessException e)
	    {
		logBrokenPlugin( pinf.class_name, state + "IllegalAccessException: " + e.toString());
		e.printStackTrace();
		// pluginReportError(full_name, class_name, e.toString() + state);
	    }
	}
	return null;
    }

    // as of 1.0.5 :
    //
    //   modified  again such that it ignores repeats of the same plugin name in 
    //   subdirectories from places which already contains a version of the plugin
    //   (so it is possible to maintain 'old' versions of plugins in subdirs without
    //   them conflicting, or being detected) but it is also possible to have
    //   different (i.e. newer) versions of the plugin available in totally
    //   unrelated places (i.e. in the user-specific plugins directory)
    //
    // 
    //   the 'root_path' is used to construct a full path to the plugin classes
    //   which is stored for later use when the plugin is called.
    //
    public boolean recursivelyScanPlugins( final String root_path, final String active_path, final java.util.HashSet plugin_names_to_ignore )
    {
	boolean result = true;

	File file = new File( root_path + File.separatorChar + active_path );

	if( file.isDirectory() )
	{	    
	    if(plugin_debug_load)
		System.out.println( file.getPath() + " is a directory" );

	    plugin_dirs_scanned++;

	    String[] list = file.list();

	    // record the current directory to use as as sort of local classpath

	    // String name = file.getPath();

	    // this should reflect 'root_path' rather than TopDir....

	    // name = name.substring( root_path.length() );


	    // as of 0.9.2 :
	    //
	    // modified to handle the case where other versions of the plugin
	    // are found in subdirs (i.e. when there are CVS subdirs as in NCGR's ISYS environment)
	    //
	    
	    

	    //String parent_dir = name;
            //
	    //String this_dir   = name;
	    //int lp = name.lastIndexOf(file.separatorChar);
	    //if(lp >= 0)
	    //{
	    //	parent_dir = name.substring(0, lp);
	    //	this_dir   = name.substring(lp+1);
	    //}

	    

	    //plugin_current_directory = root_path;
	    //if( root_path.endsWith( File.separator ) == false )
	    //	plugin_current_directory += File.separator;
	    //plugin_current_directory += active_path;



	    plugin_scan_pm.setMessage(3,  active_path );


	    //String saved_plugin_current_directory = plugin_current_directory;


	    // as of 1.0.5
	    //
	    // modified so that the _files_ in each directory are 
	    // considered before any of the _sub-directories_ are examined
	    //
	    // this ensures a predictable behaviour so that 'old' versions of
	    // the plugin can live in subdirectories of the plugin's real directory
	    // without causing conflicts

	    //
	    // ::TODO::  the 'plugin_current_directory' should reflect whether we are 
	    //           'shared' directory or the 'user-specific' directory
	    //

	    // firstly, scan the non-directories
	    for(int d=0; d < list.length; d++)
	    {
		File sub = new File( file, list[ d ] );

		if( sub.isDirectory() == false )
		{
		    if( recursivelyScanPlugins( root_path + File.separatorChar + active_path, 
						list[ d ], 
						plugin_names_to_ignore ) == false )
			result = false;
		}
	    }

	    // then, scan the sub-directories
	    for(int d=0; d < list.length; d++)
	    {
		File sub = new File( file, list[d] );

		if( sub.isDirectory() == true )
		{
		    if( recursivelyScanPlugins( root_path + File.separatorChar + active_path, 
						list[ d ], 
						plugin_names_to_ignore ) == false )
			result = false;
		}
	    }



	    //plugin_current_directory = saved_plugin_current_directory;


	    if(plugin_debug_load)
		System.out.println("finished in directory " + file.getPath());
	}
	else
	{
	    String full_name = active_path;

	    PluginInfo pinf = null;

	    plugin_classes_scanned++;
		    
	    if( full_name.indexOf('$') < 0 ) // names containing a '$' cannot be a plugin (because they are an inner class)
	    {
		
		if( (full_name.endsWith(".class")) || (full_name.endsWith(".CLASS")) )
		{
		    final String class_name = (new File( full_name )).getName();

		    final String plugin_path = root_path;

		    final String class_name_without_extension = class_name.substring( 0, class_name.length() - 6 );
		    
		    // make sure there already isn't a plugin with this name

		    // NOTE: this check disabled in 1.0.5, replaced with the 'plugin_names_to_ignore' directory-specific mechanism
		    //if( ! checkIfPluginKnown( name_without_extension ) )
		    if( plugin_names_to_ignore.contains( class_name_without_extension ) == false )
		    {
			try
			{
			    if(plugin_debug_scan)
			    {
				System.out.println("Checking " + class_name_without_extension + " in " + plugin_path );
			    }
			    
			    
			    CustomClassLoader enccl = new CustomClassLoader( plugin_path );
			    
			    Class plugin_definition = enccl.findClass( class_name_without_extension );
			
			    if( plugin_definition != null )
			    { 
				if( ( pinf = testIsPlugin( plugin_path, class_name_without_extension, plugin_definition )) != null )
				{
				    pinf.root_path  = plugin_path;
				    pinf.class_name = class_name_without_extension;

				    
				    // (A) begins
				    installPlugin( pinf );
				    plugin_info.addElement( pinf );
				    plugin_plugins_found++;
				    // (A) ends
				    
				    
				    // recall the fact that we've added a plugin with this name, so no further instances
				    // will be considered in any subdirectories from here downwards
				    plugin_names_to_ignore.add( class_name_without_extension );
				    
				    
				    // now we load it again using the same class loader
				    PluginCommand[] coms = getPluginCommands( plugin_definition, pinf );
				    
				    if(coms != null)
				    {
					for(int c=0; c < coms.length; c++)
					{
					    //System.out.println("plugin " + coms[c].plugin_name + " has command : " + coms[c].name);
					    
					    plugin_command_v.addElement(coms[c]);
					}
				    }
				}
				// (A) moved from here 
			    }
			    else
			    {
				if(plugin_debug_scan)
				    System.out.println("  -- " + file.getPath() + " is a class but not a plugin");
			    }
			}
			catch( MalformedURLException murle )
			{
			    System.err.println( "WARNING: bad path '" + plugin_path + "' for class loader." );
			}
			catch(Exception excp)
			{
			    excp.printStackTrace();
			    logBrokenPlugin( class_name_without_extension, "Unhandled exception, plugin could not be loaded" );
			}
			catch(Error e)
			{
			    e.printStackTrace();
			    logBrokenPlugin( class_name_without_extension, "Unexpected error, plugin could not be loaded" );
			}
		    }
		}
	    }
	}
	return result;
    }

/*
    private boolean checkIfPluginKnown(String class_name)
    {
	for(int p=0; p < plugin_class_names.size(); p++)
	{
	    String pcn = ( String)  plugin_class_names.elementAt(p);
	    if( class_name.equals( pcn ))
	    {
		if(plugin_debug_scan)
		    System.out.println("  -- " + class_name + " has already been seen");
		return true;
	    }
	}
	return false;
    }
*/

    // the location of the plugin (which is not recorded in the PluginInfo)
    //public Vector plugin_full_names = null;

    // the name of the plugin class (which is not recorded in the PluginInfo)
    //public Vector plugin_class_names = null;

    // the root path for plugin class (which is not recorded in the PluginInfo)
    //public Vector plugin_root_paths = null;

    public Vector getPluginInfoObjects() { return plugin_info; }

    // all other data about the plugin
    private Vector plugin_info = null;

    private int plugin_dirs_scanned;
    private int plugin_classes_scanned;
    private int plugin_plugins_found;

    private boolean plugin_debug_load = false;
    private boolean plugin_debug_scan = false;
    private boolean plugin_debug_run  = false;

    public Vector plugin_plugins_that_would_not_start = null;
    public Vector plugin_plugins_why_it_would_not_start = null;
    public int plugin_plugins_that_are_disabled =  0;

    private PluginManager plugin_manager = null;

    //private String plugin_current_directory = null;

    private boolean scanning_plugins = false;

    ProgressOMeter plugin_scan_pm = null;

    public void scanPlugins()
    {
	//custom_class_loader = new NewCustomClassLoader( this.getClass() );
	
	plugin_scan_pm = new ProgressOMeter("Scanning plugins",4);
	plugin_scan_pm.startIt();
	plugin_scan_pm.setMessage(1, "Please wait...");
	
	
	new pluginScannerThread().start();

	// now wait for it to finish....
	
	final int timer_freq = 500;  // 500ms
	int timer_ticks = 0;
	scanning_plugins = true;

	while( (timer_ticks < 50) && ( scanning_plugins ) )
	{
	    try
	    {
		Thread.sleep( timer_freq );
	    }
	    catch( InterruptedException ie)
	    {
	    }
	    timer_ticks++;
	}
    }

    public class pluginScannerThread extends Thread
    {
	public void run()
	{
	    try
	    {
		//plugin_full_names = new Vector();
		//plugin_class_names = new Vector();
		//plugin_root_paths = new Vector();

		
		plugin_dirs_scanned = 1;
		plugin_classes_scanned = 0;
		plugin_plugins_found = 0;
		
		// record any potentially interesting errors
		//
		plugin_plugins_that_would_not_start = new Vector();
		plugin_plugins_why_it_would_not_start = new Vector();
		plugin_plugins_that_are_disabled = 0;

		// throw away any old plugin menu entries
		//
		plugin_info = new Vector();
		plugin_command_v = new Vector();
		
		// each name can only be used once...
		//
		java.util.HashSet names_of_plugins_that_have_been_installed = new java.util.HashSet();
		


		// first, scan the user-specific plugins directory (if it exists...)
		//
		plugin_scan_pm.setMessage(2,  "Local plugins" );

		recursivelyScanPlugins( user_directory, "plugins", names_of_plugins_that_have_been_installed  );
		

		// then search the shared plugins directory
		//
		// ( any plugins which have already been defined in the user-specific directory
		//   will overpower any new definitions seen in the shared directory... )
		//
		plugin_scan_pm.setMessage(2,  "Global plugins" );

		//recursivelyScanPlugins( "/C:/Documents and Settings/dave/bio/maxdView", "plugins", names_of_plugins_that_have_been_installed );
		recursivelyScanPlugins( top_directory, "plugins", names_of_plugins_that_have_been_installed );
		
		

		// and update the new plugin description file 
		//
		writePluginDescriptionFile();
		
		// and the command info file
		//
		writePluginCommandInfoFile();
		
		// and generate the various help pages
		//
		updateHelpPages();
		
		// and also the "About.html" help page
		//
		//writeAboutHelpPageFile();
		
		scanning_plugins = false;

		plugin_scan_pm.stopIt();
		
		// report what was found
		//
		String i_msg = 
		"Scanned " + plugin_classes_scanned + " classes in " + 
		plugin_dirs_scanned + " directories\n" + plugin_plugins_found + " plugins found\n";
		
		if(plugin_plugins_that_are_disabled > 0)
		{
		    if(plugin_plugins_that_are_disabled == 1)
			i_msg += "\nOne plugin is"; 
		    else
			i_msg += (plugin_plugins_that_are_disabled + " plugins are"); 
		    
		    i_msg += " not enabled\n(the Plugin Manager can be used to re-enable them).\n";
		}

		if(plugin_plugins_that_would_not_start.size() > 0)
		{
		    if(plugin_plugins_that_would_not_start.size() > 1)
			i_msg += plugin_plugins_that_would_not_start.size() + " classes could not be loaded.";
		    else
			i_msg += "One class could not be loaded.";
		    
		    if(infoQuestion(i_msg, "More details", "OK") == 0)
		    {
			String failed = "The following classes might be plugins,\nbut have some problem:\n\n";
			for(int f=0; f < plugin_plugins_that_would_not_start.size(); f++)
			{
			    failed += "  " + (String)plugin_plugins_that_would_not_start.elementAt(f) + "\n";
			    failed += "     error: " + (String)plugin_plugins_why_it_would_not_start.elementAt(f) + "\n\n";
			}
			informationMessage(failed);
		    }
		}
		else
		{
		    informationMessage(i_msg);
		}
		
		custom_menu.commandsHaveBeenUpdated();

		buildMenus();

	    }
	    catch(Exception e)
	    {
		scanning_plugins = false;
		plugin_scan_pm.stopIt();
		errorMessage("Unexpected exception whilst scanning\n  " + e);
		e.printStackTrace();
	    }
	    catch(Error e)
	    {
		scanning_plugins = false;
		plugin_scan_pm.stopIt();
		errorMessage("Unexpected error whilst scanning\n  " + e);
		e.printStackTrace();
	    }
	    
	}
    }

    /*
    private boolean registerPlugin(String[] v)
    {
	// v[0] ... class-name 
	// v[1] ... file-path 
	// v[2] ... plugin-name 
	// v[3] ... short-plugin-description 
	// v[4] ... long-plugin-description 
	// v[5] ... plugin-type
	// v[6] ... version 
		
	// version format = Maj'.'Min'.'Bld

	// System.out.println("version str is " + v[5]);
	
	// v[5] = "1.2.3";

	int first_dot = v[6].indexOf('.');
	int second_dot = v[6].lastIndexOf('.');
	
	Integer vmaj, vmin, bld;

	if((first_dot <= 0) || (second_dot <= 0))
	{
	    vmin = vmaj = bld = new Integer(-1);
	}
	else
	{
	    vmaj = new Integer(v[6].substring(0,first_dot));
	    vmin = new Integer(v[6].substring(first_dot+1, second_dot));
	    bld  = new Integer(v[6].substring(second_dot+1));
	}

	PluginInfo pinf = new PluginInfo(v[2], v[5], v[3], v[4],vmaj.intValue(), vmin.intValue(), bld.intValue());
 
	installPluginInMenus(pinf, v[0], v[1]);

	return true;
    }
    */

    public void readPluginDescriptionFile( boolean first_time, boolean new_format )
    {
	try
	{

	    FileInputStream fis = new FileInputStream(new File(getConfigDirectory() + "plugin-descriptions.dat"));
	    ObjectInputStream ois = new ObjectInputStream(fis);
	    
	    // if(first_time)
	    {
		/*
		plugin_class_names = (Vector) ois.readObject();
		plugin_full_names  = (Vector) ois.readObject();

		if( new_format )
		{
		    plugin_root_paths  = (Vector) ois.readObject();
		}
		else
		{
		    plugin_root_paths = new java.util.Vector( plugin_class_names.size() );
		    for( int prp=0; prp < plugin_class_names.size(); prp++ )
			plugin_root_paths.add( "." + File.separatorChar );
		}
		*/

		plugin_info        = (Vector) ois.readObject();
	    }

	    final int np = plugin_info.size();

	    // System.out.println("installing " + plugin_info.size() + " plugins...");

	    for(int p=0 ; p < np; p++)
	    {
		try
		{
		    installPlugin( (PluginInfo) plugin_info.elementAt(p) );
		}
		catch(ArrayIndexOutOfBoundsException aioobe)
		{
		}
		catch(Exception ex )
		{
		    // something has gone wrong with the file format
		    return;
		}
	    }

	    ois.close();

	    
	}
	catch(FileNotFoundException fnfe)
	{ 
	    //plugin_full_names  = new Vector();
	    //plugin_class_names = new Vector();
	    plugin_info        = new Vector();

	    
	    if(app_runs == 0)
	    {
		makeConfigDirIfMissing();
		scanPlugins();
	    }
	    else
	    {
		// plugin descrption file not found,
		// initate a plugin rescan...
		
		//informationMessage("No Plugin Description file found in config directory");
		scanPlugins();
	    }
	}
	catch(ClassNotFoundException cnfe)
	{ 
	    informationMessage("Cannot understand plugin description file. The plugins will be re-scanned.");
	    makeConfigDirIfMissing();
	    scanPlugins();
	}

	catch(IOException ioe)
	{
	    informationMessage("Cannot read plugin description file. The plugins will be re-scanned.");
	    makeConfigDirIfMissing();
	    scanPlugins();
	}
    }

    public void writePluginDescriptionFile()
    {
	try
	{
	    // maybe the config directory doesn't exist
	    makeConfigDirIfMissing();

	    FileOutputStream fos = new FileOutputStream(new File(getConfigDirectory() + "plugin-descriptions.dat"));
	    ObjectOutputStream oos = new ObjectOutputStream(fos);
	    //oos.writeObject(plugin_class_names);
	    //oos.writeObject(plugin_full_names);
	    //oos.writeObject(plugin_root_paths);
	    oos.writeObject(plugin_info);
	    oos.flush();
	    oos.close();
	}
	catch(FileNotFoundException fnfe)
	{ 
	    informationMessage("Cannot write Plugin Description File\n  " + fnfe);
	}
	catch(IOException ioe)
	{
	    informationMessage("Cannot write Plugin Description File\n  " + ioe);
	}

 
    }
/*
    public PluginInfo getPluginInfo(String name)
    {
	try
	{
	    for(int pi=0; pi < plugin_class_names.size(); pi++)
	    {
		PluginInfo plinf = (PluginInfo) plugin_info.elementAt(pi);
		if( plinf.name.equals( name ) )
		    return plinf;
	    }
	    return null;
	}
	catch(ArrayIndexOutOfBoundsException aioobe)
	{
	    return null;
	}
	catch(NullPointerException npe)
	{
	    return null;
	}
	
    }
*/
    /*
    public String getPluginFullName(String name)
    {
	try
	{
	    for(int pi=0; pi < plugin_class_names.size(); pi++)
	    {
		PluginInfo plinf = (PluginInfo) plugin_info.elementAt(pi);
		if(plinf.name.equals(name))
		    return (String) plugin_full_names.elementAt(pi);
	    }
	    return null;
	}
	catch(ArrayIndexOutOfBoundsException aioobe)
	{
	    return null;
	}
    }
    */

    private void installPlugin( PluginInfo pinf )
    {
	boolean ok = false;
	
	if(pinf.type.equals("viewer") || 
	   pinf.type.equals("importer") || 
	   pinf.type.equals("exporter") || 
	   pinf.type.equals("transform") || 
	   pinf.type.equals("filter"))
	{
	    ok = true;
	}
	
	
	if(ok == false)
	{
	    String msg = new String("'" + pinf.type + "' is not a recognised plugin type");

	    pluginReportError( pinf.root_path, pinf.class_name, msg);
	}
	else
	{
	    //System.out.println("installing plugin '" + pinf.name + "'" );
	    //System.out.println("  class_name: '" + pinf.class_name + 
	    //		       "'\n  type: '" + pinf.type + 
	    //		       "'\n  root_path: '" + pinf.root_path + "'\n");
	   
	}
    }


    public PluginManager getPluginManager()
    {
	if( plugin_manager == null )
	    plugin_manager = new PluginManager(this);
	return plugin_manager;
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  command runner ....
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private class CommandSignalBlocker implements CommandSignal
    {
	private int count;

	public CommandSignalBlocker()
	{
	    count = 0;
	}

	public void signal()
	{
	    atomicUpdate(-1);

	    // System.out.println("+++ command done +++");
	}
	
	public void await()
	{
	    atomicUpdate(+1);
	    
	    while( count > 0 )
	    {
		try
		{
		    Thread.yield();
		    Thread.sleep(250);
		}
		catch(InterruptedException ie)
		{
		}
	    }
	}

	public synchronized void atomicUpdate(final int delta)
	{
	    count += delta;
	}
    }

    public Vector getAllCommands() { return plugin_command_v; }

    private Vector plugin_command_v = null;

    // launches a new instance of 'target' and sends the command to it
    //
    public void runCommand(final String target, final String command, final String[] args)
    {
	Plugin pl = loadPlugin(target);
	if(pl != null)
	    runCommand(pl, command, args);
	else
	    alertMessage("Unknown plugin '" + target  + "'");
    }
    // same as above, but with no args
    public void runCommand(final String target, final String command)
    {
	runCommand( target, command, null );
    }

    // sends a command to a plugin that is already running...
    //
    public void runCommand(final Plugin target, final String command, final String[] args)
    {
	runCommand(target, command, args, false);
    }
    // same as above, but with no args
    public void runCommand(final Plugin target, final String command)
    {
	runCommand( target, command );
    }

    
    private void runCommand(final Plugin target, final String command, final String[] args, boolean async)
    {
	if(target != null)
	{
	    // send the command....
	    
	    if( validateCommand(target,command,args) )
	    {
		CommandSignalBlocker blocker = async ? null : new CommandSignalBlocker();
		
		// System.out.println("sending command...");
		
		new PluginCommandThread( target, command, args, blocker ).start();
		
		if(!async)
		{
		    //System.out.println("starting blocker...");
		    
		    blocker.await();
		    
		    //System.out.println("blocker finished...");
		}
	    }
	}
	else
	{
	    alertMessage("runCommand(): Plugin argument 'target' is null");
	}
    }
    
    private class PluginCommandThread extends Thread
    {
	public PluginCommandThread( final Plugin target_, 
				    final String command_, final String[] args_, 
				    final CommandSignalBlocker blocker_)
	{
	    target = target_;
	    command = command_;
	    args = args_;
	    blocker = blocker_;
	}
	
	public void run()
	{
	    target.runCommand( command, args, blocker );
	}

	private Plugin target;
	private String command;
	private String[] args;
	private CommandSignalBlocker blocker;
    }



    // uses the 'nice' plugin names (i.e. not their class names, but the names
    // they use in their menu entries)
    //
    public Plugin startPlugin(final String name) 
    { 
	return runPlugin(name);
    }

    public void stopPlugin(final Plugin plugin) 
    { 
	plugin.stopPlugin();
    }
    
    // --- --- ---  --- --- ---  --- --- ---  


    private boolean validateCommand(final Plugin target, final String command, final String[] args )
    {
	// get the info for the plugin

	PluginInfo pi = target.getPluginInfo();
	
	boolean plugin_exists  = false;
	boolean command_exists = false;
	boolean args_are_good  = false;

	// check against all possible commands 

	String bad_arg_message = null;

	for(int p=0; p < plugin_command_v.size(); p++)
	{
	    PluginCommand pc = (PluginCommand)plugin_command_v.elementAt(p);
	    
	    if( ((String)pc.plugin_name).equals(pi.name)  )
	    {
		plugin_exists = true;
		
		// is this the right command?

		if(pc.name.equals( command ))
		{
		    command_exists = true;

		    // are the arguments ok?

		    if(pc.args == null)
		    {
			if(args == null)
			    args_are_good = true;
			else
			    bad_arg_message = "Plugin '" + pi.name + "' command '" + command + "' expects no arguments";
		    }
		    else
		    {
			bad_arg_message = checkArgs( args, pc.args );
			if(bad_arg_message == null)
			    args_are_good = true;
		    }
		}
	    }

	}

	if(!plugin_exists)
	{
	    // this can't actually happen (hopefully!)

	    return false;
	}
	else
	{
	    if(!command_exists)
	    {
		// find the command with the closest name

		Vector opts = new Vector();
		for(int p=0; p < plugin_command_v.size(); p++)
		{
		    PluginCommand pc = (PluginCommand)plugin_command_v.elementAt(p);
		    
		    if( ((String)pc.plugin_name).equals(pi.name)  )
		    {
			opts.addElement( pc.name );
		    }
		}
		
		String match = bestMatch( command, (String[]) opts.toArray(new String[0]) );
		
		alertMessage("Command '" + command + "' not found in plugin '" + pi.name + "'\n\n" +
			     "(did you mean '" + match + "' ?)" );

		return false;
	    }
	    else
	    {
		if(!args_are_good)
		{
		    alertMessage(bad_arg_message);

		    return false;
		}
		else
		{
		    // everything is ok....

		    return true;
		}
	    }
	}

    }
 
    private String bestMatch( String target, String[] options )
    {
	if((options == null) || (options.length < 1))
	    return null;

	int best_score = matchScore( target, options[0] );
	int best_option = 0;

	for(int o=1; o < options.length; o++)
	{
	    int s = matchScore( target, options[o] );
	    if(s > best_score)
	    {
		best_score = s;
		best_option = o;
	    }
	}
	
	return options[ best_option ];
    }

    private int matchScore( String  s1, String s2 )
    {
	int score = 0;
	
	if(s2 == null)
	    return 0;

	for(int c=0; c < s1.length(); c++)
	{
	    if(s2.indexOf( s1.charAt(c) ) >= 0)
		score++;
	}
	
	return score;
    }

    private String checkArgs( String[] spld, String[] allwd )
    {
	if((spld == null) || (spld.length == 0))
	    return null;

	if((spld.length % 2) != 0)
	{
	    return ("Illegal arguments, name or value missing");
	}

	java.util.Hashtable ht = new java.util.Hashtable();
	
	for(int a=0; a < allwd.length; a += 5)
	{
	    ht.put( allwd[a],  allwd[a+1] );
	    
	    // System.out.println( "a: " + allwd[a] + "\t" +  allwd[a+1] );
	}

	// spld = supplied args, allwd = allowed args
	
	// make sure all supplied argument names are recognised

	for(int a=0; a < spld.length; a+=2)
	{
	    String atype = (String) ht.get( spld[a] );
	    
	    
	    // System.out.println( "s: " + spld[a] + "\t" +  spld[a+1] );

	    if(atype == null)
	    {
		// find the closest match
		
		int nc = allwd.length/5;
		String[] opts = new String[ nc ];
		for(int o=0; o < nc; o++)
		    opts[o] = allwd[o*5];
		    
		String match = bestMatch( spld[a], opts );
		
		return( "Argument '" + spld[a] + "' not expected\n\n" + 
			"(did you mean '" + match + "' ? )" );
	    }
	    else
	    {
		if(spld[a+1] != null)
		{
		    String sval = spld[a+1].toLowerCase();
		    
		    // type check argument value
		    
		    if(atype.equals("integer"))
		    {
			try
		    {
			Integer i = Integer.valueOf(sval);
		    }
		    catch(NumberFormatException nfe)
		    {
			return("Argument '" + spld[a] + "' must be an integer value");
		    }
		    }
		    if(atype.equals("double"))
		    {
			try
			{
			    Double d = Double.valueOf(sval);
			}
			catch(NumberFormatException nfe)
			{
			    return("Argument '" + spld[a] + "' must be a double value");
			}
		    }
		    if(atype.equals("boolean"))
		    {
			if(!sval.equals("true") && !sval.equals("false"))
			    return("Argument '" + spld[a] + "' must have the value 'true' or 'false'");
		    }
		}
	    }
	}

	return null;
    }

    // --- --- ---  --- --- ---  --- --- ---  

    //
    // ---------------------------------
    // utils for parsing argument arrays
    // ---------------------------------
    //
    // supported plugin arg types:
    //
    //
    // double
    // integer
    // boolean
    // string
    // file
    // name_tag_selection
    // measurement_names
    // spot_names
    // measurement_or_spot_attr_names    : a mixed list of Measurement names and/or Spot Attribute names
    //
    //
    //
    // note: the 'measurement_or_spot_attr_names' type are of the form:
    //
    //      ex2.call ex2.size ex3 "ex7 b" "ex7 a.call" 
    //
    //

    public int findPluginArg(final String name, final String[] args)
    {
	try
	{
	    for(int a=0; a < args.length; a+=2)
		if(args[a].equals(name))
		    return a+1;
	}
	catch(NullPointerException npe)
	{
	}

	return -1;
    }

    public String getPluginArg(final String name, final String[] args, final String missing)
    {
	try
	{
	    for(int a=0; a < args.length; a+=2)
	    {
		if(args[a].equals(name))
		{
		    String res = args[a+1];
		    return ((res != null) && (res.length() > 0)) ? res : null;
		}
	    }
	}
	catch(Exception npe)
	{
	}
	
	return missing;
    }

    public String getPluginStringArg(final String name, final String[] args, final String missing)
    {
	return getPluginArg(name, args, missing);
    }

    public int getPluginIntegerArg(final String name, final String[] args, final int missing)
    {
	String arg = getPluginStringArg(name, args, null);
	try
	{
	    return (new Integer(arg)).intValue();
	}
	catch(Exception nfe)
	{
	}
	return missing;
    }

    public double getPluginDoubleArg(final String name, final String[] args, final double missing)
    {
	String arg = getPluginStringArg(name, args, null);
	try
	{
	    return (new Double(arg)).doubleValue();
	}
	catch(Exception nfe)
	{
	}
	return missing;
    }

    public boolean getPluginBooleanArg(final String name, final String[] args, final boolean missing)
    {
	String arg = getPluginStringArg(name, args, null);
	try
	{
	    if(arg.toLowerCase().equals("true"))
		return true;
	    if(arg.toLowerCase().equals("false"))
		return false;
	}
	catch(Exception nfe)
	{
	}
	return missing;
    }

    public File getPluginFileArg(final String name, final String[] args, final File missing)
    {
	String arg = getPluginStringArg(name, args, null);
	try
	{
	    return new File(arg);
	}
	catch(Exception nfe)
	{
	}
	return missing;
    }
    
    
    public ExprData.NameTagSelection getPluginNameTagSelectionArg(final String name, final String[] args, 
								  final ExprData.NameTagSelection missing)
    {
	String arg = getPluginStringArg(name, args, null);

	ExprData.NameTagSelection nts = expr_data.new NameTagSelection();

	if(arg != null)
	{
	    nts.setNames( arg );

	    return nts;
	}
	else
	    return missing;
    }


    public String[] getPluginMeasurementListArg( final String name, final String[] args, String[] missing )
    {
	String arg = getPluginStringArg(name, args, null);
	if(arg == null)
	    return missing;

	Vector tok = new Vector();

	final int nm = expr_data.getNumMeasurements();

	String[] mnames = new String[nm];
	for(int m=0; m < nm; m++)
	    mnames[m] = expr_data.getMeasurementName(m);;

	// sort the names by descending length 
	// so that 'Ex11' is seen before 'Ex1'
	// and 'meas(t1,t2)' is seen before 't1' or 't2' etc
	//

	java.util.Arrays.sort( mnames, new SortByLength() );

	boolean[] found = new boolean[nm];
	int n_found = 0;

	for(int m=0; m < nm; m++)
	{
	    found[m] = false;
	    int pos = arg.indexOf( mnames[m] );
	    if(pos >= 0)
	    {
		final int len = mnames[m].length();

		String before = (pos > 0) ? arg.substring(0, pos-1) : "";
		String after  = arg.substring(pos+len);

		// System.out.println( "[" + arg + "]" + " - [" + mnames[m] + "] = [" + before + "][" + after + "]");

		arg = before + after;
		
		found[m] = true;
		n_found++;
	    }
	}

	String[] result = new String[ n_found ];
	int rp = 0;
	for(int m=0; m < nm; m++)
	    if(found[m])
		result[rp++] = mnames[m];
	
	return result;
    }
    private class SortByLength implements java.util.Comparator
    {
	public int compare(Object o1, Object o2)
	{
	    int s1l = (o1 == null) ? 0 : ((String) o1).length();
	    int s2l = (o2 == null) ? 0 : ((String) o2).length();
	    
	    return (s2l - s1l);
	}
	
	public boolean equals(Object o) { return false; }
    }

    //
    // note: based on the convention that SpotAttr names are in the form:  MeasurementName "." SpotAttrName
    //
    public MeasSpotAttrID[] getPluginMeasurementOrSpotAttrListArg( final String name, final String[] args, MeasSpotAttrID[] missing )
    {
	String arg = getPluginStringArg(name, args, null);
	if(arg == null)
	    return missing;

	final int nm = expr_data.getNumMeasurements();

	Vector names = new Vector();

	Hashtable name_to_id = new Hashtable();

	String m_str, sa_str;

	for(int m=0; m < nm; m++)
	{
	    m_str = expr_data.getMeasurementName(m);
	    names.add( m_str );
	    name_to_id.put( m_str, new MeasSpotAttrID(m) );
	    
	    final int sa = expr_data.getMeasurement(m).getNumSpotAttributes();
	    for(int s=0; s < sa; s++)
	    {
		sa_str = m_str + "." + expr_data.getMeasurement(m).getSpotAttributeName(s);
		names.add( sa_str );
		name_to_id.put( sa_str, new MeasSpotAttrID(m,s) );
	    }
	}

	String[] names_a = (String[]) names.toArray( new String[0] );

	// sort the names by descending length 
	// so that 'Ex11.att1' is seen before 'Ex11' and before 'Ex1'
	// and 'meas(t1,t2)' is seen before 't1' or 't2' etc
	//

	java.util.Arrays.sort( names_a, new SortByLength() );
	
	Vector matched = new Vector();

	for(int n=0; n <  names_a.length; n++)
	{
	    //System.out.println( "check:" + names_a[n]);

	    int pos = arg.indexOf( names_a[n] );
	    if(pos >= 0)
	    {
		final int len = names_a[n].length();

		String before = (pos > 0) ? arg.substring(0, pos-1) : "";
		String after  = arg.substring(pos+len);

		//System.out.println( "[" + arg + "]" + " - [" + names_a[n] + "] = [" + before + "][" + after + "]");

		arg = before + after;

		matched.add( name_to_id.get(names_a[n]) );
	    }
	}

	
	return (MeasSpotAttrID[]) matched.toArray( new MeasSpotAttrID[0] );
    }



    // deprecated
    public String getPluginArg(final String name, final String[] args)
    {
	try
	{
	    for(int a=0; a < args.length; a+=2)
		if(args[a].equals(name))
		    return args[a+1];
	}
	catch(NullPointerException npe)
	{
	}

	return null;
    }


    // deprecated
    public boolean parseBooleanArg(final String b)
    {
	return (b== null) ? false : (b.toLowerCase().equals("true"));
    }
    // deprecated
    public int parseIntArg(final String i)
    {
	try
	{
	    return (new Integer(i)).intValue();

	}
	catch(NumberFormatException nfe)
	{
	    return 0;
	}
    }
    //
    // -------------------------------------
    // persistance is done via serialisation
    // -------------------------------------
    //
    private void writePluginCommandInfoFile()
    {
	try
	{
	    FileOutputStream fos = new FileOutputStream(new File(getConfigDirectory() + "plugin-command-info.dat"));
	    ObjectOutputStream oos = new ObjectOutputStream(fos);
	    oos.writeObject(plugin_command_v);
	    oos.flush();
	    oos.close();
	}
	catch(IOException ioe)
	{
	    errorMessage("Cannot save definitions file\n  " + ioe);
	}

    }

    private void readPluginCommandInfoFile(boolean first_time)
    {
	try
	{
	    FileInputStream fis = new FileInputStream(new File(getConfigDirectory() + "plugin-command-info.dat"));
	    ObjectInputStream ois = new ObjectInputStream(fis);
	    plugin_command_v = (Vector) ois.readObject();
	    ois.close();
	}
	catch(FileNotFoundException ioe)
	{
	    // no problem, it just means the plugin has not been run before
	    plugin_command_v = new Vector();

	}
	catch(ClassNotFoundException fnfe)
	{
	    infoMessage("The Plugin Commands format has changed, " + 
			 "use \"System -> Plugin Manager -> Rescan plugins\"\n to update things.");
	    plugin_command_v = new Vector();

	}
	catch(IOException ioe) // other than FileNotFound
	{
	    errorMessage("Cannot load plugin commands file\n  " + ioe);
	    plugin_command_v = new Vector();
	}

    }
     
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  interface to data plot
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void setFilterStatus( String stat1, String stat2 )
    {
	filter_status_label.setText( "    " + stat1 );

	if( stat2 == null )
	    filter_info_label.setText( " " + stat1 + " " );
	else
	    filter_info_label.setText( " " + stat1 + " " + stat2 );
    }
 
    public void setFilterStatus(String stat)
    {
	setFilterStatus( stat, null );
    }
    
    private Vector ann_viewer_list = new Vector();

    public void clusterSelected(ExprData.Cluster c)
    {
	//System.out.println("cluster " + c.getName() + " selected");
    }

    public void geneSelected(String gene_name)
    {
	geneSelected(gene_name, false);
    }

    public void geneSelected(String gene_name, boolean lock_it)
    {
	// find the first spot name that corresponds to this gene name
	final int nspots = expr_data.getNumSpots();
	for(int s=0; s < nspots; s++)
	{
	    if(expr_data.getGeneName(s).equals(gene_name))
	    {
		spotSelected(expr_data.getSpotName(s));
		return;
	    }
	}
    }

    // special version for multiple selections
    //
    public void genesSelected(String[] genes)
    {
	for(int g=0; g< genes.length; g++)
	{
	    // lock each viewer 
	    geneSelected(genes[g], true); 
	}
    }

    public void spotSelected(String spot_name, boolean lock_it)
    {
	AnnotationViewer target_av = av;

	// look for any unlocked viewers in the list
	if(ann_viewer_list.size() > 0)
	{
	    int pos = 0;
	    while(pos < ann_viewer_list.size())
	    {
		if(((AnnotationViewer) ann_viewer_list.elementAt(pos)).isLocked() == false)
		{
		    //System.out.println("found unlocked one");
		    target_av = (AnnotationViewer) ann_viewer_list.elementAt(pos);
		    break;
		}
		//else
		//   System.out.println("found locked one");

		pos++;
	    }
	}

	// if none were found, or the only one there is locked, create a new one
	if((target_av == null) || (target_av.isLocked()))
	{
	    target_av = av = new AnnotationViewer(this, annotation_loader);
	    ann_viewer_list.insertElementAt((Object)av, 0);
	    //System.out.println("created new one");
	}

	target_av.setSpot(spot_name);
	target_av.toFront();
	if(lock_it)
	    target_av.lock();
    }

    public void spotSelected(String spot_name)
    {
	spotSelected(spot_name, false);
    }

    // special version for multiple selections
    //
    public void spotsSelected(String[] spots)
    {
	for(int g=0; g< spots.length; g++)
	{
	    // lock each viewer 
	    spotSelected(spots[g], true); 
	}
    }
    
    public void annotationViewerHasClosed(AnnotationViewer the_av)
    {
	if(av == the_av)
	    av = null;
	for(int a=0; a < ann_viewer_list.size(); a++)
	{
	    if((AnnotationViewer) ann_viewer_list.elementAt(a) == the_av)
	    {
		ann_viewer_list.removeElementAt(a);
	    }
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  command handlers
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    /*
    public void exportData()
    {
	DataExporter de = new DataExporter(this);
	de.pack();
	de.setVisible(true);
    }
    */

    /*
    public void openFile()
    {
	DataImporter di = new DataImporter(this);
	di.pack();
	di.setVisible(true);
    }
    */
    
    /*
    public void openAboutWindow()
    {
	AboutPanel ap = new AboutPanel(this);
    }
    */


    public void createFindDialog()
    {
	new Finder(this);
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  information and alert messages, the status bar
    // --- --- ---  and the parameter getting dialogs
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private ImageIcon info_icon = null;
    private ImageIcon input_icon = null;
    private ImageIcon error_icon = null;
    private ImageIcon success_icon = null;

    public void setMessage(String str)
    {
	setMessage(str, true);
    }
    public void setMessage(String str, boolean do_lock)
    {
	if(do_lock)
	{
	    // System.out.println("blank out...");

	    // start a timer to lock the message in the display for a while

	    message_label.setText((str == null) ? app_title : str);

	    if(message_lock_ticker != null)
		message_lock_ticker.stop();

	    message_lock = true;

	    message_lock_ticker = new javax.swing.Timer(2500, new ActionListener() 
		{
		    public void actionPerformed(ActionEvent evt) 
		    {
			message_lock = false;
			message_lock_ticker.stop();
			message_lock_ticker = null;
			message_label.setText(app_title);
		    }
		});
	    message_lock_ticker.start();
	}
	else
	{
	    // display the message unless there is already a locked message

	    if(!message_lock)
	    {
		if(message_lock_ticker != null)
		    message_lock_ticker.stop();
		
		

		message_label.setText((str == null) ? app_title : str);
	    }
	    else
	    {
		// System.out.println("message is locked...");
	    }

	}
	
    }
    private boolean message_lock = false;
    private javax.swing.Timer message_lock_ticker = null;

    
    // =============================================================================
    // =============================================================================

    int n_dialogs_up = 0;
    CustomMessageDialog active_cmd = null;

    public void addMessageToLog(String str)
    {
	Date dt = new Date();
	System.out.println("LOG: " + dt.toString() + "\t" + str);
    }

    public void infoMessage(String str)  { informationMessage(str); }

    public void alertMessage(String str) { errorMessage(str); }

    public boolean alertMessage(String str, boolean dont_show) { return errorMessage(str, dont_show); }

    public void informationMessage(String str)
    {
	showPotentiallyLongMesage("Information", str, JOptionPane.INFORMATION_MESSAGE, info_icon); 
    }

    public void successMessage(String str)
    {
	showPotentiallyLongMesage("Success", str, JOptionPane.INFORMATION_MESSAGE, success_icon); 
    }

    public void errorMessage(String str)
    {
	showPotentiallyLongMesage("Error", str, JOptionPane.ERROR_MESSAGE, error_icon, false); 

    }

    // this version adds a "Dont show this message again" check box to the message
    // and returns true if the checkbox was selected when the dialog box closed.
    // note: this form of message dialog is modal.
    public boolean errorMessage(String str, boolean dont_show)
    {
	return showPotentiallyLongMesage("Error", str, JOptionPane.ERROR_MESSAGE, error_icon, dont_show); 
    }

    private int guessHeight( String str )
    {
	int line_h = 18;
	
	try
	{
	    FontMetrics fm = (new JPanel()).getGraphics().getFontMetrics();
	    
	    line_h = fm.getAscent() + fm.getDescent();
	}
	catch(NullPointerException npe)
	{
	}

	// how many lines in the message?
	int line = 1;
	for(int ch=0; ch < str.length(); ch++)
	    if(str.charAt(ch) == '\n')
		line++;

	//System.out.println(line  + " lines of height " + line_h);

	int height_guess = ((line * line_h) * 3) / 2;
	
	if(height_guess > 400)
	    height_guess = 400;

	return height_guess;
    }
    
    class CustomMessageDialog extends JDialog 
    {
	public CustomMessageDialog(JFrame frame, ImageIcon icon, String str, boolean dont_show) 
	{
	    super(frame, true);

	    addWindowListener(new WindowAdapter() 
		{
		    public void windowClosing(WindowEvent e)
		    {
			n_dialogs_up--;
		    }
		});
	
	    dont_show_ticked = new boolean[1];
	    dont_show_ticked[0] = false;
	    
	    addComponents(this, icon, str, dont_show);
	}

	public void addMessage( String addition )
	{
	    msg = addition + "\n\n" + msg;

	    text_area.setText(msg);
	}

	private void addComponents(final JDialog dialog, ImageIcon icon, String str, boolean dont_show)
	{
	    
	    JPanel panel = new JPanel();
	    GridBagConstraints c = null;
	    GridBagLayout gbag = new GridBagLayout();
	    panel.setLayout(gbag);
	    panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

	    setTitle("maxdView: Information");

	    if(dont_show)
		setModal(true);

	    JLabel icon_label = new JLabel(icon);
	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.NORTH;
	    //c.fill = GridBagConstraints.BOTH;
	    c.gridx = 0;
	    c.gridy = 0;
	    gbag.setConstraints(icon_label, c);
	    panel.add(icon_label);

	    int line = 0;
	    msg = str;
	    text_area = new JTextArea(msg);
	    text_area.setEditable(false);
	    text_area.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

	    jsp = new JScrollPane(text_area);
	    jsp.setPreferredSize(new Dimension(350, guessHeight( str )));

	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.BOTH;
	    c.gridx = 1;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.weighty = 5.0;

	    gbag.setConstraints(jsp, c);
	    panel.add(jsp);

	    
	    if(dont_show)
	    {
		final JCheckBox jcb = new JCheckBox("Don't show this message again");

		jcb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			dont_show_ticked[0] = jcb.isSelected();
		    }
		});

		jcb.setFont( getSmallFont() );
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 1;
		c.gridy = 1;
		gbag.setConstraints(jcb, c);
		panel.add(jcb);
	    }

	    JPanel butt_panel = new JPanel();
	    GridBagLayout butt_gbag = new GridBagLayout();
	    butt_panel.setLayout(butt_gbag);
	    butt_panel.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));

	    
	    JButton jb = new JButton("OK");
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			n_dialogs_up--;
			dialog.hide();
		    }
		});
	    c = new GridBagConstraints();
	    //c.anchor = GridBagConstraints.WEST;
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    c.weightx = 1.0;
	    butt_gbag.setConstraints(jb, c);
	    butt_panel.add(jb);
	    

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 2;
	    c.gridwidth = 2;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gbag.setConstraints(butt_panel, c);
	    panel.add(butt_panel);


	    getContentPane().add(panel);
	    validate();
	    pack();
	    
	}

	public void setSize( Component comp, String str )
	{
	    // attempt to set the dialog window to a suitable size for this message....
	    
	    Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
	    int max_pw = (int)(screen_size.getWidth() * .66);
	    int max_ph = (int)(screen_size.getHeight() * .5);
	    
	    if( comp != null )
	    {
		FontMetrics fm = comp.getGraphics().getFontMetrics();
	    
		if( fm != null )
		{
		    // find the maximum width of a line in the string
		    
		    int pw = fm.stringWidth( extractLongestLine( str ) ) + 32;
		    if(pw > max_pw)
			pw = max_pw;
		    
		    // and the total number of lines in the message
		    
		    int ph = getNumLines( str ) * (fm.getAscent() + fm.getDescent()) + 32;
		    if(ph > max_ph)
			ph = max_ph;
		    
		    //System.out.println("font height=" + (fm.getAscent() + fm.getDescent()));
		    //System.out.println("there are " + getNumLines( str ) + " lines, ph=" + ph);
		    //System.out.println("longest line=" + extractLongestLine( str ) + ", pw=" + pw);
		    
		    jsp.setPreferredSize( new Dimension(pw, ph));
		    jsp.setMinimumSize( new Dimension(pw, ph));
		}
	    }

	    // ensure scrollpane is fully-up

	    text_area.setCaretPosition(0);
	}

	private boolean[] dont_show_ticked;
	private JTextArea text_area;
	private JScrollPane jsp;
	private String msg;

	public boolean getDontShow() { return dont_show_ticked[0]; }
    }

 
    private void showPotentiallyLongMesage(String title, String str, int mode, ImageIcon icon)
    {
	showPotentiallyLongMesage(title, str, mode, icon, false);
    }
    
    private boolean showPotentiallyLongMesage(String title, String str, int mode, ImageIcon icon, boolean dont_show)
    {
	if(str == null)
	    return false;

	//if(dont_show || (str.length() > 300))

	if(n_dialogs_up == 0)
	{
	    active_cmd = new CustomMessageDialog(frame, icon, str, dont_show);
	    
	    active_cmd.setSize( frame , str);

	    active_cmd.pack();
	    
	    locateWindowAtCenter(active_cmd);
	    
	    active_cmd.setVisible(true);

	    n_dialogs_up++;

	    return active_cmd.getDontShow();
	}

	else

	{
	    // already an active CustomMessageDialog.... add the message to that
	    
	    active_cmd.addMessage( str );

	    active_cmd.setSize( frame , str);

	    active_cmd.setVisible( true );

	    return active_cmd.getDontShow();
	}

	//else
	//{
	//    JOptionPane.showMessageDialog(null, str, "maxdView: " + title, mode, icon);
	//    return false;
	//}
    }

    class CustomQuestionDialog extends JDialog 
    {
	private int result;

	private JScrollPane jsp;
	private JTextArea text_area;

	public CustomQuestionDialog(JFrame frame, ImageIcon icon, String str, String opts1, String opts2) 
	{
	    super(frame, true);
	    
	    JPanel panel = new JPanel();
	    GridBagConstraints c = null;
	    GridBagLayout gbag = new GridBagLayout();
	    panel.setLayout(gbag);
	    panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

	    setTitle("maxdView: Question");

	    JLabel icon_label = new JLabel(icon);
	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.NORTH;
	    //c.fill = GridBagConstraints.BOTH;
	    c.gridx = 0;
	    c.gridy = 0;
	    gbag.setConstraints(icon_label, c);
	    panel.add(icon_label);

	    int line = 0;
	    text_area = new JTextArea(str);
	    text_area.setEditable(false);
	    text_area.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

	    jsp = new JScrollPane(text_area);

	    jsp.setPreferredSize(new Dimension(350, guessHeight( str )));

	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.BOTH;
	    c.gridx = 1;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.weighty = 5.0;

	    gbag.setConstraints(jsp, c);
	    panel.add(jsp);
	    
	    JPanel butt_panel = new JPanel();
	    GridBagLayout butt_gbag = new GridBagLayout();
	    butt_panel.setLayout(butt_gbag);
	    butt_panel.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));


	    Insets ins = new Insets(2,15,2,15);

	    JButton jb = new JButton(opts1);
	    jb.setMargin(ins);
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			result = 0;
			hide();
		    }
		});
	    c = new GridBagConstraints();
	    c.weightx = 1.0;
	    butt_gbag.setConstraints(jb, c);
	    butt_panel.add(jb);

	    jb = new JButton(opts2);
	    jb.setMargin(ins);
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			result = 1;
			hide();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.weightx = 1.0;
	    butt_gbag.setConstraints(jb, c);
	    butt_panel.add(jb);

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.gridwidth = 2;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gbag.setConstraints(butt_panel, c);
	    panel.add(butt_panel);

	    getContentPane().add(panel);
	    pack();
	}

	public void setSize( Component comp, String str )
	{
	    // attempt to set the dialog window to a suitable size for this message....
	    
	    Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
	    int max_pw = (int)(screen_size.getWidth() * .66);
	    int max_ph = (int)(screen_size.getHeight() * .5);
	    
	    FontMetrics fm = comp.getGraphics().getFontMetrics();
	    
	    // find the maximum width of a line in the string

	    int pw = fm.stringWidth( extractLongestLine( str ) ) + 32;
	    if(pw > max_pw)
		pw = max_pw;
	    
	    // and the total number of lines in the message

	    int ph = getNumLines( str ) * (fm.getAscent() + fm.getDescent()) + 32;
	    if(ph > max_ph)
		ph = max_ph;

	    //System.out.println("font height=" + (fm.getAscent() + fm.getDescent()));
	    //System.out.println("there are " + getNumLines( str ) + " lines, ph=" + ph);
	    //System.out.println("longest line=" + extractLongestLine( str ) + ", pw=" + pw);
	    
	    jsp.setPreferredSize( new Dimension(pw, ph));
	    jsp.setMinimumSize( new Dimension(pw, ph));

	    // ensure scrollpane is fully-up

	    text_area.setCaretPosition(0);
	}

    }

    private String extractLongestLine( String src )
    {
	int longest_l = 0;
	String longest_s = null;
	
	java.util.StringTokenizer st = new java.util.StringTokenizer(src, "\n");
	while (st.hasMoreTokens()) 
	{
	    String t = st.nextToken();
	    if(t.length() > longest_l)
	    {
		longest_l = t.length();
		longest_s = t;
	    }
	}
	    return longest_s == null ? src : longest_s;
    }
    
    private int getNumLines( String src )
    {
	int n_lines = 1;
	
	int c = 0;
	while(c < src.length())
	    if(src.charAt(c++) == '\n')
		n_lines++;
	
	return n_lines;
    }


    private int askQuestion(ImageIcon icon, String str, String opt1, String opt2)
    {
	Object[] opt_buts = { opt1, opt2 };

	CustomQuestionDialog cqd = new CustomQuestionDialog(frame, icon, str, opt1, opt2);
	
	cqd.setSize( frame , str);

	locateWindowAtCenter(cqd);

	cqd.pack();
	cqd.setVisible(true);
	
	return cqd.result;
    }

    public int infoQuestion(String str, String opt1, String opt2)
    {
	return askQuestion( info_icon, str, opt1, opt2 );
    }

    public int alertQuestion(String str, String opt1, String opt2)
    {
	return askQuestion( error_icon, str, opt1, opt2 );
    }

    public int errorQuestion(String str, String opt1, String opt2)
    {
	return askQuestion( error_icon, str, opt1, opt2 );
    }
    
    public int getInteger(String name, int min, int max) throws UserInputCancelled
    {
	return getInteger(name,  min,  max, false, 0);
    }

    public int getInteger(String name, int min, int max, int init) throws UserInputCancelled
    {
	return getInteger(name,  min,  max, true, init);
    }
    
    public int getInteger(String name, int min, int max, boolean has_init, int init)  throws UserInputCancelled
    {
	String stint  = null;
	
	String initstr = has_init ? String.valueOf(init) : null;

	stint = (String) JOptionPane.showInputDialog(frame,
						     "Input value for `" + name + "'\n" + 
						     "(range " + min + " to " + max + ")", 
						     "Input : " + name, 
						     JOptionPane.PLAIN_MESSAGE,
						     input_icon,
						     null,
						     initstr);
	
	if(stint != null)
	{
	    Integer intint = new Integer(stint);
	    int ival  = intint.intValue();
	    
	    if((ival < min) || (ival > max))
	    {
		errorMessage(name + " must be in the range " + min + " to " + max);
		return getInteger(name, min, max, has_init, init);
	    }
	    
	    return ival;
	}
	throw new UserInputCancelled(name);
    }

    public final double getDouble(String name, double min, double max) throws UserInputCancelled
    {
	return getDouble(name,  min,  max, false, .0);
    }

    public final double getDouble(String name, double min, double max, double init) throws UserInputCancelled
    {
	return getDouble(name,  min,  max, true, init);
    }

    public final String niceDouble(double d, int len)
    {
	return niceDouble(d, len, -1);
    }

    // 
    // len is the desired _total_ lengeth on the string 
    //    (decimal point will be moved to accomodate this)
    //
    // 'dp' is the maximum number of digits after the decimal point,
    //    (or -1 if any number can be used)
    //
    public final String niceDouble(double d, int len, int dp)
    {
	String d_s = new Double(d).toString();

	// special handling for exponential values
	int exp_pt = d_s.indexOf('E');
	if(exp_pt > 0)
	{
	    
	    // trim the part before the 'E'
	    String np = trimString( d_s.substring(0, exp_pt), len, dp );
	    
	    // System.out.println( d_s + "  ->  " + (np+d_s.substring(exp_pt)));
	    
	    return np + d_s.substring(exp_pt);
	}
	else
	{
	    return trimString( d_s, len, dp );
	}
    }

    private final String trimString( String d_s, int len, int dp)
    {
	// dont trim numbers without a decimal point
	int dec_pt = d_s.indexOf('.');
	if(dec_pt < 0)
	    return d_s;

	int actual_decimals = d_s.length() - dec_pt;

	if(dp > actual_decimals)
	    return d_s;

	int trim_pt = dec_pt + dp;
	
	if(trim_pt > d_s.length())
	{
	    trim_pt = d_s.length();
	}

	return d_s.substring(0, trim_pt);
    }
    
    public double getDouble(String name, double min, double max, boolean has_init, double init) throws UserInputCancelled
    {
	String initstr = has_init ? String.valueOf(init) : null;

	String  stdoub = (String) JOptionPane.showInputDialog(frame,
							      "Input value for `" + name + "'\n" + 
							      "(range " + min + " to " + max + ")", 
							      "Input : " + name, 
							      JOptionPane.PLAIN_MESSAGE,
							      input_icon,
							      null,
							      initstr);
	
	if(stdoub != null)
	{
	    Double doubdoub = new Double(stdoub);
	    double dval  = doubdoub.doubleValue();
	    
	    if((dval < min) || (dval > max))
	    {
		errorMessage(name + " must be in the range " + min + " to " + max);
		return getDouble(name, min, max, has_init, init);
	    }
	    
	    return dval;
	}
	throw new UserInputCancelled(name);
    }

    public String getString(String name) throws UserInputCancelled
    {
	return  getString(name, false, null);
    }
    public String getString(String name, String init) throws UserInputCancelled
    {
	return  getString(name, true, init);
    }

    public String getString(String name, boolean has_init, String init) throws UserInputCancelled
    {
	String stint  = null;
	
	stint = (String) JOptionPane.showInputDialog(frame,
						     name, 
						     "Input : " + name, 
						     JOptionPane.PLAIN_MESSAGE,
						     input_icon,
						     null,
						     init);
	
	if(stint == null)
	    throw new UserInputCancelled(name);
	else
	    return stint;
    }

    public int[] getChoice(String message, String[] options, String[] switches) throws UserInputCancelled
    {
	return getChoice(message, options, switches, -1, false);
    }

    public int getChoice(String message, String[] options) throws UserInputCancelled
    {
	return getChoice(message, options, null,  -1, false)[0];
    }

    public int getChoice(String message, String[] options, int init) throws UserInputCancelled
    {
	return getChoice(message, options, null, init, false)[0];
    }


    //
    //  returns int[] with 1 int for each switch and 1 or more int for the selected options
    //  
    //   (e.g. if no switches and not multi then returns a single int)
    //   
    public int[] getChoice(String message, String[] options, String[] switches, int init, boolean multi) throws UserInputCancelled
    {
	String init_str = null;

	if((options == null) || (options.length < 1))
	{
	    if((switches == null) || (switches.length < 1))
		throw new UserInputCancelled(message);
	}
	else
	{
	    init_str = (init >= 0) ? options[init] : options[0];
	}

	/*
	if((multi == false) &&  (options.length > 5))
	{
	    String res =  (String) JOptionPane.showInputDialog(null, message, "Choose", 
							       JOptionPane.PLAIN_MESSAGE, 
							       input_icon, 
							       options, init_str);
	    
	    if(options != null)
	    {
		for(int so=0;so < options.length; so++)
		    if(options[so] == res)
		    {
			System.out.println("matched with " + res);
			return so;
		    }
	    }

	    throw new UserInputCancelled(message);
	}
	else
	*/

	{
	    Object[] but_opts = new Object[2];	  

	    but_opts[0] = "OK";
	    but_opts[1] = "Cancel";

	    JPanel panel = new JPanel();
	    GridBagLayout gridbag = new GridBagLayout();
	    panel.setLayout(gridbag);

	    JLabel label = new JLabel(message);
	    panel.add(label);
	    label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

	    GridBagConstraints con = new GridBagConstraints();
	    con.gridx = 0;
	    con.gridy = 0;
	    //con.weighty = 1.0;
	    //con.weightx = 9.0;
	    //con.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(label, con);

	    ButtonGroup bg = new ButtonGroup();

	    final int nbuts = (options == null) ? 0 :options.length;
	    final int nswitches = (switches == null) ? 0 : switches.length;

	    final AbstractButton[] buts = new AbstractButton[nbuts+nswitches];

	    JPanel subpanel = new JPanel();
	    GridBagLayout subbag = new GridBagLayout();
	    subpanel.setLayout(subbag);

	    // the buttons
	    for(int o=0; o < nbuts; o++)
	    {
		AbstractButton ab = null;
		if(multi)
		{
		    JCheckBox jchkb = new JCheckBox(options[o]);
		    ab = jchkb;
		}
		else
		{
		    JRadioButton jrb = new JRadioButton(options[o]);
		    bg.add(jrb);
		    ab = jrb;
		}

		if(nbuts == 1)
		    ab.setSelected(true);

		if((init >= 0) && (init == o))
		    ab.setSelected(true);
		    
		buts[o] = ab;

		con = new GridBagConstraints();
		con.gridx = 0;
		con.gridy = o;
		con.weighty = con.weightx = 1.0;
		con.anchor = GridBagConstraints.WEST;
		con.fill = GridBagConstraints.BOTH;
		subbag.setConstraints(ab, con);
		
		subpanel.add(ab);
	    }

	    // and the switches
	    if(switches != null)
	    {
		for(int s=0; s < switches.length; s++)
		{
		    AbstractButton ab = null;

		    JCheckBox jchkb = new JCheckBox(switches[s]);
		    ab = jchkb;
		
		    buts[nbuts + s] = ab;

		    con = new GridBagConstraints();
		    con.gridx = 0;
		    con.gridy = nbuts + s;
		    con.weighty = con.weightx = 1.0;
		    con.anchor = GridBagConstraints.WEST;
		    con.fill = GridBagConstraints.BOTH;
		    subbag.setConstraints(ab, con);
		    
		    subpanel.add(ab);
		}
	    }
	    
	    con = new GridBagConstraints();
	    con.gridx = 0;
	    con.gridy = 1;
	    con.weighty = 4.0;
	    con.weightx = 9.0;
	    con.anchor = GridBagConstraints.WEST;
	    con.fill = GridBagConstraints.BOTH;

	    
	    if((nbuts + nswitches) > 3)
	    {
		JScrollPane jsp = new JScrollPane(subpanel);
		
		jsp.setPreferredSize(new Dimension(300,270));
		// jsp.setMinimumSize(new Dimension(300,270));
	   
		gridbag.setConstraints(jsp, con);
		panel.add(jsp);
	    }
	    else
	    {
		gridbag.setConstraints(subpanel, con);
		panel.add(subpanel);
	    }

	    //gridbag.setConstraints(jsp, con);
	    //panel.add(jsp);

	    //choice_radio_but_set = -1;

    	    int res = JOptionPane.showOptionDialog(frame, panel, "Choose", 
						   JOptionPane.YES_NO_CANCEL_OPTION,
						   JOptionPane.PLAIN_MESSAGE, 
						   input_icon, 
						   but_opts, 
						   but_opts[1]);
	    //System.out.println("res is " + res);

	    if(res == 0)
	    {
		int c = 0;

		for(int o=0; o < nbuts; o++)
		{
		    if(buts[o].isSelected())
		    {
			c++;
		    }
		}

		
		if((nbuts > 0) && (c == 0))
		{
		    errorMessage("You must choose one of the options or press \"Cancel\"");
		    return getChoice(message, options, switches, init, multi);
		}
		else
		{
		    int[] ret = new int[nswitches + c];

		    for(int s=0; s < nswitches; s++)
			ret[s] = buts[nbuts + s].isSelected() ? 1 : 0;

		    c = nswitches;
		    
		    for(int o=0; o < nbuts; o++)
		    {
			if(buts[o].isSelected())
			{
			    ret[c++] = o;
			}
		    }

		    return ret;
		}
	    }
	    else
		throw new UserInputCancelled(message);

	}
    }

    /*
    private int choice_radio_but_set = 0;
    
    private class ChoiceActionListener implements ActionListener
    {
	public ChoiceActionListener(int o)
	{
	    opt = o;
	}

	public void actionPerformed(ActionEvent e) 
	{
	    choice_radio_but_set = opt;
	}
	private int opt;
    }
    */

    /*
    private String[] getMeasurementNameList()
    {
	
	String[] mnl = new String[expr_data.getNumMeasurements()];
	for(int m=0;m<expr_data.getNumMeasurements();m++)
	{
	    int mi = expr_data.getMeasurementAtIndex(m);
	    mnl[m] = expr_data.getMeasurementName(mi);
	}
	return mnl;
    }
    */

    private final String no_meas_msg = "No Measurements available to choose from";
    
    public int getMeasurementChoice( ) throws UserInputCancelled
    {
	return  getMeasurementChoice( "Pick one Measurement" );
    }
    public int[] getMeasurementsChoice( ) throws UserInputCancelled
    {
	return  getMeasurementsChoice( "Pick one or more Measurements" );
    }

    public int getMeasurementChoice( String msg ) throws UserInputCancelled
    {
	if(expr_data.getNumMeasurements() == 0)
	{
	    alertMessage(no_meas_msg );
	    throw new UserInputCancelled(no_meas_msg );
	}
	else
	{	   
	    String[] opt = expr_data.getMeasurementNames();
	    return expr_data.getMeasurementAtIndex(getChoice(msg, opt, -1));
	}
    }

    public int[] getMeasurementsChoice( String msg ) throws UserInputCancelled
    {
	if(expr_data.getNumMeasurements() == 0)
	{
	    alertMessage(no_meas_msg);
	    throw new UserInputCancelled(no_meas_msg);
	}
	else
	{
	    String[] opt = expr_data.getMeasurementNames();
	    int[] res = getChoice(msg, opt, null, -1, true);
	    for(int m=0; m < res.length; m++)
		res[m] =  expr_data.getMeasurementAtIndex(res[m]);
	    
	    return res;
	}
    }
	
    // ====== getNameTagSelection() =======================================================================

    public ExprData.NameTagSelection getNameTagSelection(String message) throws UserInputCancelled
    {
	JPanel panel = new JPanel();
	panel.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
	GridBagLayout gridbag = new GridBagLayout();
	panel.setLayout(gridbag);
	
	JLabel label = new JLabel(message);
	label.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
	GridBagConstraints con = new GridBagConstraints();
	con.gridx = 0;
	con.gridy = 0;
	gridbag.setConstraints(label, con);
	panel.add(label);
	
	final NameTagSelector ntsel = new NameTagSelector(this);
	con = new GridBagConstraints();
	con.gridx = 0;
	con.gridy = 1;
	con.weighty = 2.0;
	con.weightx = 10.0;
	con.fill = GridBagConstraints.BOTH;
	gridbag.setConstraints(ntsel, con);
	panel.add(ntsel);
	
	Object[] but_opts = new Object[2];	  
	
	but_opts[0] = "OK";
	but_opts[1] = "Cancel";
	
	int res = JOptionPane.showOptionDialog(frame, panel, "Choose", 
					       JOptionPane.YES_NO_CANCEL_OPTION,
					       JOptionPane.PLAIN_MESSAGE, 
					       /*input_icon*/null, 
					       but_opts, 
					       but_opts[1]);
	
	if(res == 0)
	{
	    return ntsel.getNameTagSelection();
	}
	else
	{
	    throw new UserInputCancelled(message);
	}
    }
    
    // ====== getDirectory(), getFile() ==============================================================

    public final String getDirectory( String message, String init ) throws UserInputCancelled
    {
	return getFile( message, init, JFileChooser.DIRECTORIES_ONLY );

    }

    public final String getFile( String message, String init ) throws UserInputCancelled
    {
	return getFile( message, init, JFileChooser.FILES_ONLY );  // FILES_AND_DIRECTORIES
    }

    public final String getFileOrDirectory( String message, String init ) throws UserInputCancelled
    {
	return getFile( message, init, JFileChooser.FILES_AND_DIRECTORIES );  // FILES_AND_DIRECTORIES
    }


    public final String getFile( String message, String init, int sel_mode ) throws UserInputCancelled
    {
	JFileChooser jfc = new JFileChooser(init);
	jfc.setFileSelectionMode(sel_mode);
	jfc.setDialogTitle(message);
	jfc.setPreferredSize( new Dimension( 440, 400 ));
	
	int returnVal =  jfc.showOpenDialog(getDataPlot());
	if(returnVal == JFileChooser.APPROVE_OPTION) 
	{
	    return jfc.getSelectedFile().getPath();
	}
	else
	{
	    throw new UserInputCancelled(message);
	}

    }

    // ====== getCluster() =======================================================================


    public final ExprData.Cluster getCluster(String message) throws UserInputCancelled
    {
	return getCluster(message, 0, null);
    }
    public final ExprData.Cluster getCluster(String message,  ExprData.Cluster init_cl) throws UserInputCancelled
    {
	return getCluster(message, 0, init_cl);
    }
    public final ExprData.Cluster getSpotCluster(String message) throws UserInputCancelled
    {
	return getCluster(message, 1, null);
    }
    public final ExprData.Cluster getSpotCluster(String message,  ExprData.Cluster init_cl) throws UserInputCancelled
    {
	return getCluster(message, 1, init_cl);
    }
    public final ExprData.Cluster getMeasurementCluster(String message) throws UserInputCancelled
    {
	return getCluster(message, 2, null);
    }
    public final ExprData.Cluster getMeasurement(String message,  ExprData.Cluster init_cl) throws UserInputCancelled
    {
	return getCluster(message, 2, init_cl);
    }

    private final ExprData.Cluster getCluster(final String message, 
					      final int mode, // 0==any, 1==spot only, 2==measurement only
					      final ExprData.Cluster init_cl) throws UserInputCancelled
    {
	JPanel panel = new JPanel();
	panel.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
	GridBagLayout gridbag = new GridBagLayout();
	panel.setLayout(gridbag);
	
	final JTree cl_tree = new JTree();
	populateTreeWithClusters(cl_tree, mode, expr_data.getRootCluster() );

	// expand the tree to display the initial selection if provided
	if(init_cl != null)
	{
	    System.out.println( "opening " + init_cl.getName() );
	    
	    DefaultTreeModel model      = (DefaultTreeModel)       cl_tree.getModel();
	    DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
	    for (Enumeration e = root.depthFirstEnumeration(); e.hasMoreElements() ;) 
	    {
		DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) e.nextElement();
		ExprData.Cluster cli = (ExprData.Cluster) dmtn.getUserObject();
		
		if(cli == init_cl)
		{
		    TreeNode[] tn_path = model.getPathToRoot(dmtn);
		    TreePath tp = new TreePath(tn_path);
		    cl_tree.expandPath(tp);
		    cl_tree.scrollPathToVisible(tp);
		    cl_tree.setSelectionPath(tp);
		    break;
		}
	    }
	}
	JScrollPane jsp = new JScrollPane(cl_tree);

	
	JLabel label = new JLabel(message);
	label.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
	GridBagConstraints con = new GridBagConstraints();
	con.gridx = 0;
	con.gridy = 0;
	gridbag.setConstraints(label, con);
	panel.add(label);
	
	con = new GridBagConstraints();
	con.gridx = 0;
	con.gridy = 1;
	con.weighty = 8.0;
	con.weightx = 10.0;
	con.fill = GridBagConstraints.BOTH;
	gridbag.setConstraints(jsp, con);
	panel.add(jsp);
	
	Object[] but_opts = new Object[2];	  
	
	but_opts[0] = "OK";
	but_opts[1] = "Cancel";
	
	int res = JOptionPane.showOptionDialog(frame, panel, "Choose", 
					       JOptionPane.YES_NO_CANCEL_OPTION,
					       JOptionPane.PLAIN_MESSAGE, 
					       input_icon, 
					       but_opts, 
					       but_opts[1]);
	
	if(res == 0)
	{
	    DefaultMutableTreeNode node = (DefaultMutableTreeNode) cl_tree.getLastSelectedPathComponent();
	    
	    if(node == null)
	    {
		alertMessage("You must selecte a Cluster or press \"Cancel\"");
		return getCluster(message, mode, init_cl);
	    }
	    else
		return (ExprData.Cluster) node.getUserObject();
	}
	else
	{
	    throw new UserInputCancelled(message);
	}
    }

    private void populateTreeWithClusters(final JTree tree, final int mode, final ExprData.Cluster cluster)
    {
	// System.out.println("making tree for " + cluster.getName() );

	DefaultMutableTreeNode dmtn = generateTreeNodes( null, mode, cluster );
	tree.setModel( new DefaultTreeModel( dmtn ));

	
	tree.putClientProperty("JTree.lineStyle", "Angled");
	
	tree.setCellRenderer(new CustomTreeCellRenderer() );
    }

    // mode:  0==any, 1==spot only, 2==measurement only
    private DefaultMutableTreeNode generateTreeNodes(final DefaultMutableTreeNode parent, 
						     final int mode, 
						     final ExprData.Cluster clust)
    {
	boolean ignore = false;
	if(mode == 1)
	    if(!clust.getIsSpot())
		ignore = true;
	if(mode == 2)
	    if(clust.getIsSpot())
		ignore = true;

	// dont ignore the Root....
	if(ignore && (parent == null))
	    ignore = false;

	DefaultMutableTreeNode node = ignore ? parent : new DefaultMutableTreeNode( clust );
	
	Vector ch = clust.getChildren();
	if(ch != null)
	{
	    for(int c=0; c < ch.size(); c++)
		generateTreeNodes( node, mode, (ExprData.Cluster) ch.elementAt(c) );
	}
	
	if(!ignore && (parent != null))
	{
	    parent.add(node);
	    return parent;
	}
	else
	{
	    return node;
	}
    }

    public class CustomTreeCellRenderer extends DefaultTreeCellRenderer
    {
	//private ImageIcon leaf_icon;
	private Font italic_font = null;
	private Font normal_font = null;
	private Font bold_font   = null;

	private Polygon[] glyph_poly = null;
	private int glyph_poly_height;

	private int draw_glyph;
	private boolean is_shown;

	private boolean draw_edge;  // to indicate selected node

	public CustomTreeCellRenderer()
	{
	    // leaf_icon = new ImageIcon("images/cluster.gif");
	    // System.out.println("CustomTreeCellRenderer() constructed");

	}

	public void paintComponent(Graphics g)
	{
	    int h = getHeight();

	    int off = 0;
	    
	    if(draw_glyph >= 0)
	    {
		if((glyph_poly == null) || (glyph_poly_height != h))
		{
		    // generate (or re-generate at a new size) the glyphs
		    glyph_poly = first_dplot_panel.getScaledClusterGlyphs(h-2);
		    glyph_poly_height = h;
		    
		    // System.out.println("^v^v^v^v^ CustomTreeCellRenderer(): glyphs rescaled");
		}
		
		off += h;

		Polygon poly = new Polygon(glyph_poly[draw_glyph].xpoints, 
					   glyph_poly[draw_glyph].ypoints,
					   glyph_poly[draw_glyph].npoints);
		
		if(is_shown)
		    g.fillPolygon(poly);
		else
		    g.drawPolygon(poly);

	    }
	    else
		off = 0;

	    g.drawString(getText(), off, h-4);

	    if(draw_edge)
		g.drawRect(0, 0, getWidth()-1, getHeight()-1);
	}

	public Component getTreeCellRendererComponent(JTree tree,
						      Object value,
						      boolean sel,
						      boolean expanded,
						      boolean leaf,
						      int row,
						      boolean hasFocus) 
	{
	    
	    super.getTreeCellRendererComponent(tree, value, sel,
					       expanded, leaf, row,
					       hasFocus);

	    backgroundNonSelectionColor = backgroundSelectionColor = borderSelectionColor = Color.white;

	    if(italic_font == null)
	    {		
		Graphics g = tree.getGraphics();
		if(g != null)
		{
		    normal_font = g.getFont();
		    if(normal_font != null)
		    {
			italic_font = new Font(normal_font.getName(), Font.ITALIC, normal_font.getSize());
			bold_font   = new Font(normal_font.getName(), Font.BOLD,   normal_font.getSize());
		    }
		}
	    }

	    
	    DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
	    if(node != null)
	    {
		ExprData.Cluster cl = (ExprData.Cluster) node.getUserObject();
	    
		draw_edge = sel;

		// System.out.println("rendering " + cl.getName());
		
		// changed to draw all nodes as if they were leafs except the root

		//if(cl.getElements() != null) 
		if(cl.getParent() != null)
		{
		    //ExprData.Cluster cl = (ExprData.Cluster) node_info;
		    
		    draw_glyph = cl.getGlyph();
		    is_shown = cl.getShow();
		    
		    setFont(normal_font);
		    setForeground(cl.getColour());
		}
		else 
		{
		    //System.out.println("this is a leaf not");

		    // this is the root
		    
		    draw_glyph = -1;
		    setFont(bold_font);
		    setIcon(null);
		    setForeground(Color.black);
		    // setToolTipText(null); //no tool tip
		    
		    //setFont(normal_font);
		} 
	    }
	    setBackground(Color.white);
	    
	    // System.out.println("treecell component is " + this);

	    return this;
	}
    }


    // ====================================================================================

    public void locateWindowAtCenter(Window win)
    {
	Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
   
	win.setLocation((int)((screen_size.getWidth() - win.getWidth()) / 2.0),
			(int)((screen_size.getHeight() - win.getHeight()) / 2.0));
    }

    public JFileChooser     getFileChooser()      { return fc; }

    public JColorChooser     getColourChooser()      { return col_chooser; }
    
    public DatabaseConnection getDatabaseConnection() 
    { 
	if(dbcon == null)
	{
	    dbcon = new DatabaseConnection(this);
	}
	       

	return dbcon;
    }
    private DatabaseConnection dbcon;

    // convenience version which set the current directory of the file chooser
    // to the specified string
    //
    public JFileChooser     getFileChooser(String def_dir)
    { 
	File dd = new File(def_dir);
	fc.setCurrentDirectory(dd);
	return fc; 
    }

    //
    // handy utils related to user input...
    //
    public String[] getListOfMeasurementNames()
    {
	if(expr_data.getNumMeasurements() == 0)
	    return null;

	String[] mnames = new String[expr_data.getNumMeasurements()];
	for(int nm=0; nm < expr_data.getNumMeasurements(); nm++)
	    mnames[nm] = expr_data.getMeasurementName(nm);
	
	return mnames;
    }

    public String[] getListOfSpotAttributeNames(int meas_id)
    {
	ExprData.Measurement m = expr_data.getMeasurement(meas_id);
	if(m == null)
	    return null;

	if(m.getNumSpotAttributes() == 0)
	    return null;

	String[] sanames = new String[m.getNumSpotAttributes()];
	for(int nm=0; nm < m.getNumSpotAttributes(); nm++)
	    sanames[nm] = m.getSpotAttributeName(nm);

	return sanames;
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  timing
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    
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
    
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  help panel
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void getPluginHelpTopic(String plugin_class_name, String topic)
    {
	// this version uses the docs directory of the specified plugin
	// rather than the global docs directory which only contains
	// help pages for the built-in functions
	//

	// find the directory that this plugin lives in...
	//
	//String plg = null;
	//String dir = getPluginDirectory(plugin_name);

	/*
	String s = "file:" 
		      + System.getProperty("user.dir")
		      + System.getProperty("file.separator")
		      + dir
		      + topic
		      + ".html";  
	*/
	
	PluginInfo pinf = getPluginInfoFromClassName( plugin_class_name );

	if( pinf == null )
	{
	    System.err.println("WARNING: unable to locate the plugin '" + plugin_class_name );
	    return;
	}
	
	String s = "file:" + pinf.root_path + File.separatorChar + topic + ".html";  
	
	if(help_panel == null)
	    help_panel = new HelpPanel(this, s);
	else
	    help_panel.gotoTopic(s);
    }

    public void getPluginHelpTopic(String plugin_class_name, String topic, String sub_topic)
    {
	// this version uses the directory of the specified plugin
	// rather than the global docs directory which only contains
	// help pages for the built-in functions
	//

	// find the directory that this plugin lives in...
	//
	//String plg = null;
	//String dir = getPluginDirectory(plugin_name);

	/*
	String s = 
	"file:" 
	+ System.getProperty("user.dir")
	+ System.getProperty("file.separator")
	+ dir
	+ topic
	+ ".html"
	+ sub_topic;
	*/

	PluginInfo pinf = getPluginInfoFromClassName( plugin_class_name );

	if( pinf == null )
	{
	    System.err.println("WARNING: unable to locate the plugin '" + plugin_class_name );
	    return;
	}

	String s = "file:" + pinf.root_path + File.separatorChar  + topic + ".html" + sub_topic;;  

	if(help_panel == null)
	    help_panel = new HelpPanel(this, s);
	else
	    help_panel.gotoTopic(s);
    }

    // adds the "file:" protocol and directory name as a prefix, and ".html" as a suffix
    //
    public String getHelpTopicFileName(String topic)
    {
	String s = "file:" + getHelpDirectory() + topic + ".html";  
	return s;
    }
    public String getHelpTopicFileName(String topic, String substopic)
    {
	String s = "file:" + getHelpDirectory() + topic + ".html" + substopic;  
	return s;
    }

    public void getUserSpecificHelpTopic(String topic)
    {
	String s = "file:" + getUserSpecificDirectory() + File.separatorChar + "docs" + File.separatorChar + topic + ".html";  

	if(help_panel == null)
	{
	    help_panel = new HelpPanel(this, s);
	}
	else
	{
	    help_panel.toFront();
	    help_panel.gotoTopic(s);
	}
    }

    public void getHelpTopic(String topic)
    {
	getHelpTopic(topic, null);
    }

    public void getHelpTopic(String topic, String subtopic)
    {
	String s = (subtopic == null) ?  getHelpTopicFileName(topic) : getHelpTopicFileName(topic, subtopic);

	if(help_panel == null)
	{
	    help_panel = new HelpPanel(this, s);
	}
	else
	{
	    help_panel.toFront();
	    help_panel.gotoTopic(s);
	}
    }

    public void helpPanelDisplayURL(String url_str)
    {
	// String s =  getHelpTopicFileName(topic) + subtopic;

	if(help_panel == null)
	{
	    help_panel = new HelpPanel(this, url_str);
	}
	else
	{
	    help_panel.toFront();
	    help_panel.gotoTopic(url_str);
	}
    }

    public void getHelpTopicFile(String topic)
    {
	String s = getHelpTopicFileName(topic);

	if(help_panel == null)
	{
	    help_panel = new HelpPanel(this, s);
	}
	else
	{
	    help_panel.toFront();
	    help_panel.gotoTopic(s);
	}
    }

    // called by the HelpPanel when it closes
    public void helpPanelHasClosed()
    {
	help_panel = null;
    }


    public void updateHelpPages()
    {
	new HelpMaker().writePages(maxdView.this);  // writeCommandHelpPageFile();
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  Licence panel
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    protected boolean agreed = false;
	

    public boolean isLicenceAgreed()
    {
	if((!is_first_view) || (getBooleanProperty("licence_agreed.perl", false) == true))
	    return true;
	
	String lic_str = null;
	File file = new File( getTopDirectory() + File.separatorChar + "LICENCE");

	try
	{
	    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
	    
	    int file_length = (int) file.length();
	    byte b[] = new byte[file_length];
	    
	    if(bis.read(b, 0, file_length) == file_length)
	    {
		lic_str = new String(b);
	    }
	    else
	    {
		alertMessage("Unable to load LICENCE file\n(from '" + file.getPath() + "')");
		return false;
	    }
	}
	catch (IOException e) 
	{
	    alertMessage("Unable to find LICENCE file\n(looking for '" + file.getPath() + "')");
	    return false;
	}
	    

	int result = infoQuestion( lic_str, "Decline", "Accept" );
	
	if( result == 1 )
	{
	    putBooleanProperty("licence_agreed.perl", true);
	    return true;
	}
	else
	{
	    return false;
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  SpecialisedPrintStream : a kludge to capture info about exceptions thrown
    // --- --- ---                           in the awt event handler thread by intercepting
    // --- --- ---                           the standard error stream
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    class SpecialisedPrintStream extends java.io.PrintStream
    {
	public SpecialisedPrintStream(OutputStream out) 
	{
	    super(out, true);  // enable autoflush

	    try
	    {
		pw = new PrintWriter(new BufferedWriter(new FileWriter( getTopDirectory() + File.separatorChar + log_file_name ) ), true);

		System.out.println("Information: error log file opened in \"" +  log_file_name + "\"");

		pw.println( getApplicationTitle() + "\n");
		
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
			   
	    }
	    catch(java.io.IOException ioe)
	    {
		pw = null;
		System.out.println("WARNING: unable to open error log in \"" +  log_file_name + "\"");
	    }
	}
	
	public void finalize() { pw.flush(); pw.close(); }

	public void print(boolean d)            { if(pw != null) pw.print(d); super.print(d);  }
	public void print(char d)               { if(pw != null) pw.print(d); super.print(d);  }
	public void print(int d)                { if(pw != null) pw.print(d); super.print(d);  }
	public void print(long d)               { if(pw != null) pw.print(d); super.print(d);  }
	public void print(float d)              { if(pw != null) pw.print(d); super.print(d);  }
	public void print(double d)             { if(pw != null) pw.print(d); super.print(d);  }

	public void println()                   { if(pw != null) pw.println(); super.println(); } 

	public void println(boolean d)          { if(pw != null) pw.println(d); super.println(d); } 
	public void println(char d)             { if(pw != null) pw.println(d); super.println(d); } 
	public void println(int d)              { if(pw != null) pw.println(d); super.println(d); } 
	public void println(long d)             { if(pw != null) pw.println(d); super.println(d); } 
	public void println(float d)            { if(pw != null) pw.println(d); super.println(d); } 
	public void println(double d)           { if(pw != null) pw.println(d); super.println(d); }
	public void println(char[] d)           { if(pw != null) pw.println(d); super.println(d); grabSpecial(String.valueOf(d)); } 
	public void println(java.lang.String d) { if(pw != null) pw.println(d); super.println(d); grabSpecial(d);  }
	public void println(java.lang.Object d) { if(pw != null) pw.println(d); super.println(d); grabSpecial(d);  } 

	//  other PrintStream methods not needed....(hopefully)

	//  public void print(char[] d)           {  }
	//  public void print(java.lang.String d) {  }
	//  public void print(java.lang.Object d) {  }
  
	//  public void write(int);
	//  public void write(byte[], int, int);

	
	public void grabSpecial(String s)
	{
	    if(state == 0)
	    {
		//if(s.startsWith("Exception") || s.startsWith("java."))
		{
		    state = 1;
		}
	    }
	}

	public void grabSpecial(Object o)
	{
	    //if(state == 1)
	    {
		state = 2;
		
		errorMessage("Unhandled Exception:\n  " + o.toString() + "\n(see file \"" +  log_file_name + "\" for details)\n");
		
		state = 0;
	    }
	}

	private final String log_file_name = "error.log";

	private int state = 0;
	private PrintWriter pw;
    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  main
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public static void main(String[] args) 
    {
	// System.out.println("started");
	try
	{

	    try 
	    {
		UIManager.setLookAndFeel( 
		    //"com.sun.java.swing.plaf.windows.WindowsLookAndFeel" 
		    "javax.swing.plaf.metal.MetalLookAndFeel" 
		    //"com.sun.java.swing.plaf.motif.MotifLookAndFeel" 
		    //UIManager.getCrossPlatformLookAndFeelClassName() 
		    );
	    } 
	    catch (Exception e) 
	    { 
	    }
	    
	    //identifySharedDirectory( );
	    //identifyConfigDirectory( );

	    maxdView app = new maxdView(args, null);
	    
	    try
	    {
		app.getBusy();
	    }
	    catch(Exception ex) 
	    {
		System.err.println("*** EXCEPTION CAUGHT ***\n");
		ex.printStackTrace();
		//app.errorMessage("Unexpected exception. Save your data and run like hell.\n" + ex);
	    }
	    catch(Error er) 
	    {
		System.err.println("*** ERROR CAUGHT ***\n");
		er.printStackTrace();
		//app.errorMessage("Unexpected error. Save your data and run like hell.\n" + er);
	    }

	    //System.exit( -1 );
	}
	catch(java.rmi.RemoteException rmie)
	{
	    System.out.println("*** java.rmi.RemoteException:\n" +  rmie);
	    rmie.printStackTrace();
	}
	catch(Throwable th) 
	{
	    System.out.println("*** THROWABLE CAUGHT ***\n" + th);
	    th.printStackTrace();
	}
	
	//catch(java.net.MalformedURLException murle)
	//{
	//    System.out.println("*** java.net.MalformedURLException:\n" +  murle);
	//}
	
    }
}
