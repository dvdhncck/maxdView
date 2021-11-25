//package uk.ac.man.bioinf.maxdLoad2;

import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.event.*;
import javax.swing.border.*;

// -----------------------------------------------------------------------------------------------------------------
// -----------------------------------------------------------------------------------------------------------------
//
//    GUI that maintains a list of connections (doesn't actually do any connecting itself.....)
//
// -----------------------------------------------------------------------------------------------------------------
// -----------------------------------------------------------------------------------------------------------------

public class ConnectionManager_m2
{
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  gui
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public ConnectionManager_m2( Controller cntrl_, Properties props_ )
    {
	props = props_;
	cntrl = cntrl_;
    }


    
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public String getHost()         { return host_jtf.getText();   }
    public String getDriverFile()   { return driver_location_jtf.getText(); }
    public String getDriverName()   { return driver_name_jtf.getText();   }
    public String getAccount()      { return acct_jtf.getText();   }
    public String getPassword()     { return passwd_jtf.getText(); }

    public void setConnectAction( ActionListener al ) { connect_action = al; }
    public void setCloseAction(   ActionListener al ) { close_action = al; }
    public void setHelpAction(    ActionListener al ) { help_action = al; }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  


    private class Database
    {
	public String driver_name, driver_location, name, host, account;
	
	public Database(String n, String h, String dl, String dn, String a )
	{
	    name = n; host = h; driver_name = dn; driver_location = dl; account = a;
	}
	
	public String toString() 
	{ 
	    return "[" + host + ":" + driver_name + ":" + driver_location + ":" + 
		name + ":" + account + "]"; 
	}
    }
	
    private int findUsingName( String name )
    {
	for(int d=0; d < db_list_v.size(); d++)
	{
	    if( ((Database) db_list_v.elementAt(d)).name.equals( name ) )
		return d;
	}
	return -1;
    }

    private void setupDatabaseList()
    {
	if( cntrl.debug_startup )
	    System.out.println("ConnectionManager(): setupDatabaseList() starts");

	DefaultListModel db_list_model = new DefaultListModel();

	for(int db=0; db < db_list_v.size(); db++)
	{
	    db_list_model.addElement( ((Database) db_list_v.elementAt(db)).name );
	}
	
	db_saves_list.setModel(db_list_model);

	if( cntrl.debug_startup )
	    System.out.println("ConnectionManager(): setupDatabaseList() ends");
    }

    private void addNewDatabase()
    {
	try
	{
	    String name = cntrl.mview.getString("Please choose a name for the new entry", "" );
	    
	    if( ( name != null ) && ( name.length() > 0 ) )
	    {
		// TODO:: check that the name doesn't already exist
		// .....
		if( findUsingName( name ) >= 0 )
		{
		    cntrl.alertMessage( "That name is already used for an entry in the list.\n" + 
					"Names must be unique." );
		}
		else
		{
		    db_list_v.addElement(new Database(name, 
						      host_jtf.getText(), 
						      driver_location_jtf.getText(), 
						      driver_name_jtf.getText(), 
						      acct_jtf.getText()));
		
		    setupDatabaseList();
		    
		    db_saves_list.setSelectedIndex( db_list_v.size() - 1 );
		    
		    updateAndSaveDatabaseListInCommonFormat();
		}
	    }
	    else
	    {
		cntrl.alertMessage( "The entry must have a name" );
	    }
	}
	catch( UserInputCancelled uic )
	{

	}

    }

    private void deleteDatabase()
    {
	int index = findUsingName( (String) db_saves_list.getSelectedValue() );

	if(index >= 0)
	{
	    Database db = (Database) db_list_v.elementAt(index);

	    if( cntrl.mview.infoQuestion("Really delete the entry '" + db.name + "' from the connection list ?", "Yes", "No") == 1)
		return;

	    db_list_v.removeElementAt(index);

	    setupDatabaseList();
	    
	    host_jtf.setText("");
	    driver_name_jtf.setText("");
	    driver_location_jtf.setText("");
	    acct_jtf.setText("");
	    passwd_jtf.setText("");
	    
	    updateAndSaveDatabaseListInCommonFormat();
	}
    }

    private void renameDatabase()
    {
	int index = findUsingName( (String) db_saves_list.getSelectedValue() );
	
	try
	{
	    if(index >= 0)
	    {
		Database db = (Database) db_list_v.elementAt(index);
		
		String nm = cntrl.mview.getString("New name for '" + db.name + "'", db.name );
		
		if(nm != null)
		{
		    db.name = nm;
		    setupDatabaseList();
		    
		    updateAndSaveDatabaseListInCommonFormat();
		}
	    }
	}
	catch( UserInputCancelled uic )
	{
	}
    }

    private void databaseSelected( )
    {
	resetStatus();

	int index = findUsingName( (String) db_saves_list.getSelectedValue() );
	
	if(index >= 0)
	{
	    disable_list_updates = true;

	    Database db = (Database) db_list_v.elementAt(index);

	    host_jtf.setText( db.host );
	    driver_name_jtf.setText( db.driver_name );
	    driver_location_jtf.setText( db.driver_location );
	    acct_jtf.setText( db.account );
	    passwd_jtf.setText("");

	    disable_list_updates = false;
	}
    }

    private Database findDatabase( String name )
    {
	for(int dbi=0; dbi < db_list_v.size(); dbi++)
	{
	    Database db = (Database) db_list_v.elementAt( dbi );

	    if( db.name.equals( name ) )
		return db;
	}
	
	
	return null;
    }

    private void updateCurrent()
    {
	int index = findUsingName( (String) db_saves_list.getSelectedValue() );
	
	if(index >= 0)
	{
	    Database db = (Database) db_list_v.elementAt(index);
	    
	    db.host            = host_jtf.getText();
	    db.driver_name     = driver_name_jtf.getText();
	    db.driver_location = driver_location_jtf.getText();
	    db.account         = acct_jtf.getText();
	    
	    //	    System.out.println("db " + index + " updated : " + db.toString() );
	}
    }

    public void selectDatabase( String name )
    {
	for(int dbi=0; dbi < db_list_v.size(); dbi++)
	{
	    Database db = (Database) db_list_v.elementAt( dbi );

	    if( db.name.equals( name ) )
	    {
		db_saves_list.setSelectedIndex( dbi );
		return;
	    }
	}

	System.err.println("Unable to find entry '" + name + "' in the connection list.");
	return;
    }

/*
    private void updateAndSaveDatabaseList()
    {
	updateCurrent();

	props.put("DBConMan.account",     acct_jtf.getText());
	props.put("DBConMan.host",        host_jtf.getText());
	props.put("DBConMan.driver",      driver_name_jtf.getText());
	props.put("DBConMan.driver_loc",  driver_location_jtf.getText());

	//System.out.println(db_list_v.size() + " db entries in catalog");						     
	props.put("DBConMan.n_saved_dbs",  String.valueOf( db_list_v.size() ));

	DefaultListModel dlm = (DefaultListModel) db_saves_list.getModel( );

	for(int dbi=0; dbi < dlm.size(); dbi++)
	{
	    String name = (String) dlm.elementAt( dbi );
	    
	    Database db = findDatabase( name );

	    if(db != null)
	    {
		
		if( db.host == null ) 
		    db.host = "";
		if( db.jdbc == null ) 
		    db.jdbc = "";
		if( db.driver == null ) 
		    db.driver = "";
		if( db.account == null ) 
		    db.account = "";

		//System.out.println( "saving " + db.toString() + " ...." );
		
		props.put("DBConMan.db" + dbi + ".name",       db.name);
		props.put("DBConMan.db" + dbi + ".host",       db.host);
		props.put("DBConMan.db" + dbi + ".driver",     db.jdbc);
		props.put("DBConMan.db" + dbi + ".driver_loc", db.driver);
		props.put("DBConMan.db" + dbi + ".acct",       db.account);
	    }
	}

	props.put("DBConMan.new_format_active", "true");

	//	System.out.println(db_list_v.size() + " db entries saved");
    }
*/
 
    private void loadDatabaseList()
    {
	//if( cntrl.debug_startup )
	    System.out.println("ConnectionManager(): loadDatabaseList() start");

	db_list_v = new Vector();

	// firstly, try to load things from the common format file (i.e. the one that can be shared with maxdView)

//	if( loadCommonFormatDatabaseList() > 0 )
//	    return;

	loadCommonFormatDatabaseList();

	// then merge any entries found in the 'old skool' format, i.e. those listed in the maxdLoad2 saved properties 
	// (but make sure that entries aren't duplicated)


	int dbi = 0;
	boolean found = true;

	String max_db_s = (String) props.get("DBConMan.n_saved_dbs");
	
	int max_db = 0;
	try
	{
	    max_db = Integer.valueOf(max_db_s).intValue();
	}
	catch(NumberFormatException nfe)
	{
	}

	if( cntrl.debug_startup )
	    System.out.println("ConnectionManager(): expecting " + max_db + " connections in list");

	int loaded = 0;
	int ignored = 0;

	while((found) && (dbi < max_db))
	{
	    String dbn = (String) props.get("DBConMan.db" + dbi + ".name");

	    if(dbn == null)
	    {
		if( cntrl.debug_startup )
		{
		    System.out.println("ConnectionManager(): curious: entry " + dbi + " has no name");
		    dbn = "[missing-name-" + dbi + "]";

		    // found = false;
		}
	    }
	    else
	    {
		Database db = new Database( (String) props.get("DBConMan.db" + dbi + ".name"),
					    (String) props.get("DBConMan.db" + dbi + ".host"),
					    (String) props.get("DBConMan.db" + dbi + ".driver_loc"),
					    (String) props.get("DBConMan.db" + dbi + ".driver"),
					    (String) props.get("DBConMan.db" + dbi + ".acct") );
		
		// check for duplicated entries
		
		if( notInList( db_list_v, db ) )
		{
		    db_list_v.addElement( db );
		    
		    loaded++;
		}
		else
		{
		    ignored++;
		}
		
		// and also remove this info from the properties so it won't confuse things in the future...
		
		props.remove("DBConMan.db" + dbi + ".name");
		props.remove("DBConMan.db" + dbi + ".host");
		props.remove("DBConMan.db" + dbi + ".driver_loc");
		props.remove("DBConMan.db" + dbi + ".driver");
		props.remove("DBConMan.db" + dbi + ".acct");
	    }

	    
	    dbi++;
	}


	// and finally remove the entry counter from the properties so it won't confuse things in the future...
	props.remove("DBConMan.n_saved_dbs");

	if( cntrl.debug_startup )
	    System.out.println( loaded + " database entries loaded from old-skool format, " + ignored + " duplicates ignored" );

	// is this the first time the new format has been loaded?
	
	String check = (String) props.get("DBConMan.new_format_active");

	if(check == null)
	{
	    // System.out.println("converting from old saved connection format");
	    
	    String old_exists = (String) props.get("connect.database_name.0");
	    if((old_exists != null) && (old_exists.length() > 0))
	    {
		db_list_v.addElement( new Database( (String) props.get("connect.database_name.0"),
						    (String) props.get("connect.host.0"),
						    (String) props.get("connect.driver_path.0"),
						    (String) props.get("connect.driver.0"),
						    (String) props.get("connect.account.0") ) );
	    }
	}

	if( cntrl.debug_startup )
	    System.out.println("ConnectionManager(): loadDatabaseList() ends");
	
    }
 
    private boolean notInList( java.util.Vector list, Database db )
    {
	for( int i=0; i < list.size(); i++ )
	{
	    Database test = (Database) list.elementAt( i );
	    
	    if( ( test.name.equals( db.name ) ) &&
		( test.driver_name.equals( db.driver_name ) ) &&
		( test.driver_location.equals( db.driver_location ) ) &&
		( test.host.equals( db.host ) ) &&
		( test.account.equals( db.account ) ) )
		return false;
	}
	return true;
    }


    // ---------------------------------------------------------
    //
    // as of version 1.0.4, the database connection list is saved in a separate file
    // which can be more easily shared with other applications, namely maxdView
    // 
    //  ---------------------------------------------------------


    private void updateAndSaveDatabaseListInCommonFormat()
    {
	updateCurrent();

	Properties shared_props = new Properties();

	cntrl.putProperty("maxd_database_connection.selected.account",         acct_jtf.getText());
	cntrl.putProperty("maxd_database_connection.selected.host",            host_jtf.getText());
	cntrl.putProperty("maxd_database_connection.selected.driver_name",     driver_name_jtf.getText());
	cntrl.putProperty("maxd_database_connection.selected.driver_location", driver_location_jtf.getText());

//	System.out.println(db_list_v.size() + " db entries in catalog");
						     
	shared_props.put("maxd_database_connection.n_saved_dbs",  String.valueOf( db_list_v.size() ));

	for(int dbi=0; dbi < db_list_v.size(); dbi++)
	{
	   Database db = (Database) db_list_v.elementAt(dbi);
	   if(db != null)
	   {
	       
//	       System.out.println( "saving " + db.toString() + " ...." );
	       
	       shared_props.put("maxd_database_connection.db" + dbi + ".name",            db.name);
	       shared_props.put("maxd_database_connection.db" + dbi + ".host",            db.host);
	       shared_props.put("maxd_database_connection.db" + dbi + ".driver_name",     db.driver_name);
	       shared_props.put("maxd_database_connection.db" + dbi + ".driver_location", db.driver_location);
	       shared_props.put("maxd_database_connection.db" + dbi + ".account",         db.account);
	     }
	}


	

	try
	{
	    FileOutputStream out = new FileOutputStream( cntrl.getConfigDirectory() + File.separatorChar + "maxd_database_list.config" );

	    shared_props.save( out, "maxd database list" );

	    out.close();
	}
	catch (java.io.IOException ioe)
	{
	    //
	}

 
	
    }
 


    private int loadCommonFormatDatabaseList()
    {
	Properties shared_props = new Properties();

	final String db_list_file = cntrl.getConfigDirectory() + File.separatorChar + "maxd_database_list.config";

	System.out.println("looking in:" + db_list_file );

	try
	{
	    FileInputStream in = new FileInputStream( db_list_file );
	    
	    if( in != null )
	    {
		shared_props.load( in );
		in.close();
	    }
	}
	catch (java.io.IOException ioe)
	{
	    // file not found, or not readable or something else equally annoying
	    return 0;
	}


	db_list_v = new Vector();

	int dbi = 0;
	boolean found = true;

	String max_db_s = (String) shared_props.get("maxd_database_connection.n_saved_dbs");
	
	int max_db = 0;

	try
	{
	    max_db = Integer.valueOf(max_db_s).intValue();
	}
	catch(NumberFormatException nfe)
	{
	}

	while( found && (dbi < max_db) )
	{
	    final String dbn = (String) shared_props.get("maxd_database_connection.db" + dbi + ".name");

	    if(dbn == null)
	    {
		found = false;
	    }
	    else
	    {
		db_list_v.addElement( new Database( dbn,
						    (String) shared_props.get( "maxd_database_connection.db" + dbi + ".host" ),
						    (String) shared_props.get( "maxd_database_connection.db" + dbi + ".driver_location" ),
						    (String) shared_props.get( "maxd_database_connection.db" + dbi + ".driver_name" ),
						    (String) shared_props.get( "maxd_database_connection.db" + dbi + ".account" ) ) );
		
		dbi++;
	    }
	}

	// uses: host, driver_loc, driver, account ...

	// and override the 'selected database' values
	//
	
	if( shared_props.get( "maxd_database_connection.selected.account" ) != null )
	    props.put( "maxd_database_connection.account", shared_props.get( "maxd_database_connection.selected.account" ) );
	
	if( shared_props.get( "maxd_database_connection.selected.host" ) != null )
	    props.put( "maxd_database_connection.host",       shared_props.get( "maxd_database_connection.selected.host" ) );
	
	if( shared_props.get( "maxd_database_connection.selected.driver_location" ) != null )
	    props.put( "maxd_database_connection.driver_location", shared_props.get( "maxd_database_connection.selected.driver_location" ) );
	
	if( shared_props.get( "maxd_database_connection.selected.driver_name" ) != null )
	    props.put( "maxd_database_connection.driver_name",     shared_props.get( "maxd_database_connection.selected.driver_name" ) );

	
	if( cntrl.debug_startup )
	    System.out.println( dbi + " database entries loaded from common file...");
	
	return dbi;
    }
 
 
    // ---------------------------------------------------------

    public String getString(String name, boolean has_init, String init)
    {
	String stint  = null;
	
	stint = (String) JOptionPane.showInputDialog(null,
						     name, 
						     "Input : " + name, 
						     JOptionPane.PLAIN_MESSAGE,
						     null,
						     null,
						     init);
	
	return stint;
    }

    public JPanel getConnectPanel(final JFrame parent )
    {
	System.out.println("ConnectionManager(): getConnectPanel() starts");

	if( connection_panel == null )
	    connection_panel = createConnectPanel( parent );
	return connection_panel;
    }
    
    private JPanel createConnectPanel(final JFrame parent )
    {
	//if( cntrl.debug_startup )
	    System.out.println("ConnectionManager(): createConnectPanel() starts");

	loadDatabaseList();

	//int w = mview.getIntProperty("DatabaseConnection.width",  500);
	//int h = mview.getIntProperty("DatabaseConnection.height", 400);

	final int w = 550;
	final int h = 380;

	// -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
	// -- the connect panel -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
	// -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

	Insets ins = new Insets(1,1,1,1);
	Font f = (new JButton("x")).getFont();
	Font small_font = new Font(f.getName(), f.getStyle(), f.getSize() - 2);
	    
	JPanel connect_panel = new JPanel();

	connect_panel.setBorder(BorderFactory.createEmptyBorder(5,5,0,5));

	//connect_panel.setBackground( cntrl.background_colour );
	connect_panel.setPreferredSize(new Dimension(w, h));
	GridBagLayout gridbag = new GridBagLayout();
	connect_panel.setLayout(gridbag);

	int line = 0;

	JPanel  wrapper = new JPanel();

	//wrapper.setBackground( cntrl.background_colour );

	Border inner_border   = BorderFactory.createEmptyBorder(30,20,30,20);
	Border outer_border   = BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED);
	CompoundBorder border = BorderFactory.createCompoundBorder(outer_border, inner_border);
	wrapper.setBorder(border);

	GridBagLayout wrapbag = new GridBagLayout();
	wrapper.setLayout(wrapbag);

	Font label_font = new JLabel().getFont();
	Font big_bold_font = new Font( label_font.getName(), Font.BOLD, label_font.getSize() + 4 );
	Color label_colour = Color.blue.darker();

	int wline = 0;
	{
	    JLabel label = ComponentFactory.makeLabel("  Database ");
	    label.setForeground( label_colour );
	    label.setFont( big_bold_font );
	    wrapper.add(label);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridy = wline++;
	    c.anchor = GridBagConstraints.WEST;
	    wrapbag.setConstraints(label, c);
	}
	{
	    host_jtf = ComponentFactory.makeTextField(20);
	    host_jtf.setFont( big_bold_font );
	    wrapper.add(host_jtf);
	    
	    if( db_list_v.size() == 0 )
		host_jtf.setText( cntrl.getProperty("maxd_database_connection.selected.host", "" ));

	    host_jtf.getDocument().addDocumentListener(new TextChangeListener());

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridy = wline;
	    c.gridwidth = 3;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    c.weightx = 9.0;
	    wrapbag.setConstraints(host_jtf, c);
	    
	}

	addFiller( 3, wrapper, wrapbag, wline, 3 );

	{
	    JButton jb = new JButton( "?" );
	    jb.setToolTipText("Display help on how to specify the database"); 
	    
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			displayHelp( "Database" );
		    }
		});

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 4;
	    c.gridy = wline++;
	    c.fill = GridBagConstraints.VERTICAL;
	    c.anchor = GridBagConstraints.WEST;
      	    wrapbag.setConstraints(jb, c);
	    wrapper.add(jb);
	}

	addFiller( 20, wrapper, wrapbag, wline++, 0 );
	
	{
	    JLabel label = ComponentFactory.makeLabel("  Driver File ");
	    label.setForeground( label_colour );
	    label.setFont( big_bold_font );
	    wrapper.add(label);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridy = wline++;
	    c.anchor = GridBagConstraints.WEST;
      	    wrapbag.setConstraints(label, c);
	}
	{
	    driver_location_jtf = ComponentFactory.makeTextField(20);
	    driver_location_jtf.setFont( big_bold_font );
	    wrapper.add(driver_location_jtf);
	    
	    if( db_list_v.size() == 0 )
		driver_location_jtf.setText( cntrl.getProperty("maxd_database_connection.selected.driver_location", "") );

	    driver_location_jtf.getDocument().addDocumentListener(new TextChangeListener());
		    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = wline;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    c.weightx = 9.0;
	    wrapbag.setConstraints(driver_location_jtf, c);
	    
	}

	addFiller( 3, wrapper, wrapbag, wline, 1 );

	{
	    JButton jb = ComponentFactory.makeButton(" Browse ");
	    jb.setMargin(new Insets(0,0,0,0));
	    jb.setFont(small_font);

	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			JFileChooser fc = cntrl.mview.getFileChooser();

			 // cntrl.decorateFrame( fc );
			 // cntrl.positionWindowAtCenter( fc );

			 fc.setSelectedFile(new File(driver_location_jtf.getText()));
			 
			 fc.setDialogTitle("Pick a file or directory containing the JDBC driver");

			 fc.setFileSelectionMode( JFileChooser.FILES_AND_DIRECTORIES );

			 int returnVal = fc.showDialog( cntrl.getFrame(), "Select");
			 
			 if (returnVal == JFileChooser.APPROVE_OPTION) 
			     {
				 File file = fc.getSelectedFile();
				 if(file != null)
				     driver_location_jtf.setText(file.getPath());
			     }
		    }
		});

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = wline;
	    c.fill = GridBagConstraints.VERTICAL;
	    c.anchor = GridBagConstraints.WEST;
      	    wrapbag.setConstraints(jb, c);
	    wrapper.add(jb);
	}

	addFiller( 3, wrapper, wrapbag, wline, 3 );

	{
	    JButton jb = new JButton( "?" );
	    jb.setToolTipText("Display help on how to specify the driver file"); 

	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			displayHelp( "DriverFile" );
		    }
		});

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 4;
	    c.gridy = wline++;
	    c.fill = GridBagConstraints.VERTICAL;
	    c.anchor = GridBagConstraints.WEST;
      	    wrapbag.setConstraints(jb, c);
	    wrapper.add(jb);
	}


	addFiller( 20, wrapper, wrapbag, wline++, 0 );

	{
	    JLabel label = ComponentFactory.makeLabel("  Driver Name ");
	    label.setForeground( label_colour );
	    label.setFont( big_bold_font );
	    wrapper.add(label);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridy = wline++;
	    c.anchor = GridBagConstraints.WEST;
      	    wrapbag.setConstraints(label, c);
	}
	{
	    driver_name_jtf = ComponentFactory.makeTextField(20);
	    driver_name_jtf.setFont( big_bold_font );
	    wrapper.add(driver_name_jtf);
	    
	    if( db_list_v.size() == 0 )
		driver_name_jtf.setText( cntrl.getProperty("maxd_database_connection.selected.driver_name", "" ) );

	    driver_name_jtf.getDocument().addDocumentListener(new TextChangeListener());

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridy = wline;
	    c.gridwidth = 3;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    c.weightx = 9.0;
	    wrapbag.setConstraints(driver_name_jtf, c);
	    
	}

	addFiller( 3, wrapper, wrapbag, wline, 3 );

	{
	    JButton jb = new JButton( "?" );
	    jb.setToolTipText("Display help on how to specify the driver name"); 

	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			displayHelp( "DriverName" );
		    }
		});

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 4;
	    c.gridy = wline++;
	    c.fill = GridBagConstraints.VERTICAL;
	    c.anchor = GridBagConstraints.WEST;
      	    wrapbag.setConstraints(jb, c);
	    wrapper.add(jb);
	}

	addFiller( 20, wrapper, wrapbag, wline++, 0 );

	{
	    JLabel label = ComponentFactory.makeLabel("  User Name ");
	    label.setForeground( label_colour );
	    label.setFont( big_bold_font );
	    wrapper.add(label);
	    
	    GridBagConstraints c = new GridBagConstraints();
	     c.gridy = wline++;
	    c.anchor = GridBagConstraints.WEST;
      	    wrapbag.setConstraints(label, c);
	}
	{
	    acct_jtf = ComponentFactory.makeTextField(20);
	    acct_jtf.setFont( big_bold_font );
	    wrapper.add(acct_jtf);

	    if( db_list_v.size() == 0 )
		acct_jtf.setText( cntrl.getProperty("maxd_database_connection.selected.account", "" ) );

	    acct_jtf.getDocument().addDocumentListener(new TextChangeListener());


	    GridBagConstraints c = new GridBagConstraints();
	    c.gridy = wline;
	    c.gridwidth = 3;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    c.weightx = 9.0;
	    wrapbag.setConstraints(acct_jtf, c);
	    
	}

	addFiller( 3, wrapper, wrapbag, wline, 3 );

	{
	    JButton jb = new JButton( "?" );
	    jb.setToolTipText("Display help on user names"); 

	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			displayHelp( "UserName" );
		    }
		});

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 4;
	    c.gridy = wline++;
	    c.fill = GridBagConstraints.VERTICAL;
	    c.anchor = GridBagConstraints.WEST;
      	    wrapbag.setConstraints(jb, c);
	    wrapper.add(jb);
	}

	addFiller( 20, wrapper, wrapbag, wline++, 0 );

	{
	    JLabel label = ComponentFactory.makeLabel("  Password ");
	    label.setForeground( label_colour );
	    label.setFont( big_bold_font );
	    wrapper.add(label);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridy = wline++;
	    c.anchor = GridBagConstraints.WEST;
	    wrapbag.setConstraints(label, c);
	}
	{
	    passwd_jtf = ComponentFactory.makePasswordField(20);
	    passwd_jtf.setFont( big_bold_font );
	    wrapper.add(passwd_jtf);

	    passwd_jtf.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			updateAndSaveDatabaseListInCommonFormat();

			if(connect_action != null)
			    connect_action.actionPerformed(null);
		    }
		});

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridy = wline;
	    c.gridwidth = 3;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    c.weightx = 9.0;
	    wrapbag.setConstraints(passwd_jtf, c);
	}

	addFiller( 3, wrapper, wrapbag, wline, 3 );

	{
	    JButton jb = new JButton( "?" );
	    jb.setToolTipText("Display help on passwords"); 

	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			displayHelp( "Password" );
		    }
		});

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 4;
	    c.gridy = wline++;
	    c.fill = GridBagConstraints.VERTICAL;
	    c.anchor = GridBagConstraints.WEST;
      	    wrapbag.setConstraints(jb, c);
	    wrapper.add(jb);
	}

	addFiller( 20, wrapper, wrapbag, wline++, 3 );

	{
	    JLabel dummy = new JLabel();
	    wrapper.add(dummy);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridy = wline++;
	    c.weighty = 1.0;
	    c.anchor = GridBagConstraints.NORTH;
      	    wrapbag.setConstraints(dummy, c);
	}

	{
	    JLabel label = ComponentFactory.makeLabel("  Status ");
	    label.setForeground( label_colour );
	    //label.setFont( bold_font );
	    wrapper.add(label);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridy = wline++;
	    c.gridwidth = 5;
	    c.anchor = GridBagConstraints.SOUTHWEST;
	    wrapbag.setConstraints(label, c);
	    

	    status_jtp = ComponentFactory.makeTextArea( 60, 6, false );
	    status_jtp.setEditable( false );
	    JScrollPane status_jsp = new JScrollPane( status_jtp );
	    c = new GridBagConstraints();
	    c.gridy = wline++;
	    c.weightx = 2.0;
	    c.weighty = 1.0;
	    c.gridwidth = 5;
	    c.fill = GridBagConstraints.BOTH;
	    c.anchor = GridBagConstraints.SOUTHWEST;
	    wrapbag.setConstraints( status_jsp, c );
	    wrapper.add( status_jsp );
	    
	}


	{
	    JPanel exwrap = new JPanel();
	    GridBagLayout exbag = new GridBagLayout();
	    exwrap.setLayout(exbag);
	    GridBagConstraints c = null;

	    /*
	    if((existing_dbcon != null) && (existing_dbcon.isConnected()) && (existing_dbcon.users == 0))
	    {
		JLabel label = new JLabel("There is an open connection to:");
		c = new GridBagConstraints();
		//c.anchor = GridBagConstraints.WEST;
		c.weightx = 2.0;
		c.weighty = 1.0;
	        exbag.setConstraints(label, c);
		exwrap.add(label);  

		label = new JLabel( existing_dbcon.getName() );
		c = new GridBagConstraints();
		//c.anchor = GridBagConstraints.WEST;
		c.gridy = 1;
		c.weightx = 2.0;
		c.weighty = 1.0;
	        exbag.setConstraints(label, c);
		exwrap.add(label);  

		JButton jb = new JButton(" Use ");
		c = new GridBagConstraints();
		c.gridy = 2;
		//c.anchor = GridBagConstraints.WEST;
		c.weightx = 2.0;
		c.weighty = 1.0;
	        exbag.setConstraints(jb, c);
		exwrap.add(jb);
		
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    // make sure the connection hasn't timed out since the connect panel was displayed
			    if(connection == null)
			    {
				mview.errorMessage("Sorry, this connection has since closed.\nPlease login again.");
			    }
			    else
			    {
				timeout_counter = 0;
				parent.setVisible(false);
				dialog_result = 1;
				users++;
				System.out.println("this connection has " + users + " users");
			    }
			    // useExisting( existing_dbcon );
			}
		    });

		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = wline++;
		c.gridwidth = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		//c.anchor = GridBagConstraints.WEST;
		c.weightx = 2.0;
		c.weighty = 2.0;
		wrapbag.setConstraints(exwrap, c);
		wrapper.add(exwrap);
	    }
	    */
	}


	/*
	{
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.fill = GridBagConstraints.BOTH;
	    c.weightx = 8.0;
	    c.weighty = 8.0;
	    gridbag.setConstraints(wrapper, c);
	    connect_panel.add(wrapper);

	}
	*/

	JSplitPane c_split_pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

	c_split_pane.setRightComponent(wrapper);

	// and the saves list

	wrapper = new JPanel();
	// wrapper.setBackground( cntrl.background_colour );
	wrapbag = new GridBagLayout();
	wrapper.setLayout(wrapbag);

	inner_border   = BorderFactory.createEmptyBorder(2,2,2,2);
	outer_border   = BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED);
	border = BorderFactory.createCompoundBorder(outer_border, inner_border);
	wrapper.setBorder(border);


	{
	    db_saves_list = new DragList();

	    db_saves_list.addListSelectionListener(new ListSelectionListener()
		{
		    public void valueChanged(ListSelectionEvent e) 
		    {
			if( e.getValueIsAdjusting() )
			    return;

			int index = db_saves_list.getSelectedIndex();

			{
			    if(index >= 0)
			    {
				databaseSelected();
				delete_jb.setEnabled( true );
				rename_jb.setEnabled( true );
			    }
			    else
			    {
				delete_jb.setEnabled( false );
				rename_jb.setEnabled( false );
			    }
			}
		    }
		});
	    
	    db_saves_list.addMouseListener(new java.awt.event.MouseAdapter()
		{
		    public void mousePressed(MouseEvent e)
		    {
			if( e.getClickCount() == 2 )
			{
			    // System.out.println("quick-connect....");a
			    
			    updateAndSaveDatabaseListInCommonFormat();
			    
			    if(connect_action != null)
				connect_action.actionPerformed(null);
			}
		    }
		});
						   
	    db_saves_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
	    JScrollPane jsp = new JScrollPane(db_saves_list);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.fill = GridBagConstraints.BOTH;
	    c.gridwidth = 3;
	    c.weightx = 1.0;
	    c.weighty = 8.0;
	    wrapbag.setConstraints(jsp, c);
	    wrapper.add(jsp);

	    addFiller( 3, wrapper, wrapbag, 1, 0 );

	    JButton jb = ComponentFactory.makeButton("Create a new entry");
	    jb.setToolTipText("Add a new entry to the connection list");
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
		        addNewDatabase();
		    }

		});
	    jb.setFont(small_font);
	    jb.setMargin(ins);

	    c = new GridBagConstraints();
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.weightx = 5.0;
	    c.gridx = 0;
	    c.gridy = 2;
	    c.gridwidth = 3;
	    wrapbag.setConstraints(jb, c);
	    wrapper.add(jb);

	    addFiller( 3, wrapper, wrapbag, 3, 0 );

	    rename_jb = ComponentFactory.makeButton("Rename");
	    rename_jb.setToolTipText("Change the name of the currently selected entry");
	    rename_jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			renameDatabase();
		    }

		});
	    rename_jb.setFont(small_font);
	    rename_jb.setMargin(ins);
	    c = new GridBagConstraints();
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.weightx = 1.0;
	    c.gridx = 0;
	    c.gridy = 4;
	    wrapbag.setConstraints(rename_jb, c);
	    wrapper.add(rename_jb);

	    addFiller( 3, wrapper, wrapbag, 4 , 1 );

	    delete_jb = ComponentFactory.makeButton("Delete");
	    delete_jb.setToolTipText("Remove the currently selected entry from the connection list");
	    delete_jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			deleteDatabase();
		    }

		});
	    delete_jb.setFont(small_font);
	    delete_jb.setMargin(ins);
	    c = new GridBagConstraints();
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.weightx = 1.0;
	    c.gridx = 2;
	    c.gridy = 4;
	    wrapbag.setConstraints(delete_jb, c);
	    wrapper.add(delete_jb);

	    delete_jb.setEnabled( false );
	    rename_jb.setEnabled( false );

	}

	c_split_pane.setLeftComponent(wrapper);

	{
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.fill = GridBagConstraints.BOTH;
	    c.weightx = 8.0;
	    c.weighty = 8.0;
	    gridbag.setConstraints(c_split_pane, c);
	    connect_panel.add(c_split_pane);

	}

	setupDatabaseList();

	line++;

/*
	{
	    JPanel buttons_panel = new JPanel();
	    buttons_panel.setBackground( cntrl.background_colour );
	    GridBagLayout buttons_bag = new GridBagLayout();
	    buttons_panel.setLayout( buttons_bag );
	    buttons_panel.setBorder( BorderFactory.createEmptyBorder(0,0,0,0) );

	    bar = cntrl.addButtonAndProgressBar( new String[] { "Close", "Connect", "Help" }, buttons_panel );

	    bar.getButton(0).addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			updateAndSaveDatabaseListInCommonFormat();
			if(close_action != null)
			    close_action.actionPerformed(null);
		    }
		});
	    bar.getButton(1).addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			updateAndSaveDatabaseListInCommonFormat();
			if(connect_action != null)
			    connect_action.actionPerformed(null);
			}
		});
	    bar.getButton(2).addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			if(help_action != null)
			    help_action.actionPerformed(null);
		    }
		});

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridy = line;
	    c.weightx = 10.0;
	    c.anchor = GridBagConstraints.SOUTH;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(buttons_panel, c);
	    connect_panel.add(buttons_panel);
	}
*/

	//System.out.println("connect panel ok");

	if( cntrl.debug_startup )
	    System.out.println("ConnectionManager(): connection panel manufactured");

	return connect_panel;
    }

    private void addFiller( final int size_i, final JPanel panel, final GridBagLayout bag, final int row, final int col )
    {
	Dimension size = new Dimension( size_i, size_i );
	Box.Filler filler = filler = new Box.Filler(size, size, size);
	GridBagConstraints c = new GridBagConstraints();
	c.gridx = col;
	c.gridy = row;
	bag.setConstraints(filler, c);
	panel.add(filler);
    }


    // handles all text fields
    //
    class TextChangeListener implements DocumentListener 
    {
	public void insertUpdate(DocumentEvent e)  { propagate(e); }
	public void removeUpdate(DocumentEvent e)  { propagate(e); }
	public void changedUpdate(DocumentEvent e) { propagate(e); }

	private void propagate(DocumentEvent e)
	{
	    if(!disable_list_updates)
		updateCurrent();
	}
    }

    public void selectFirstListEntry()
    {
	db_saves_list.setSelectedIndex(0);
    }

    public int getNumberOfEntries()
    {
	return db_list_v.size();
    }


    // ==================================================================================


    private void displayHelp( String thing )
    {
	//HelpViewer hv = cntrl.getHelpViewer();
	//hv.showFile( "Connection" + thing + ".html" );
    }


    // ==================================================================================

    //public ButtonAndProgressBar getButtonAndProgressBar() { return bar; }

    public void resetStatus( )
    {
	status_jtp.setText("");	
    }

    public void reportStatus( String msg )
    {
	status_jtp.append( msg + "\n" );

	System.out.println( msg );
    }

    // ==================================================================================


    protected JPanel connection_panel;

    //protected ButtonAndProgressBar bar;

    protected JTextArea status_jtp;

    protected Controller cntrl;

    protected JTextField host_jtf, acct_jtf, driver_name_jtf, driver_location_jtf;

    protected JPasswordField passwd_jtf;
    
    protected JButton delete_jb, rename_jb;

    protected ActionListener connect_action;
    protected ActionListener close_action;
    protected ActionListener help_action;

    protected Properties props;

    protected Vector db_list_v;
    protected JList db_saves_list;
    protected boolean disable_list_updates = false;
    
}
