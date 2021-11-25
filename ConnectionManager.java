import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.event.*;

// -----------------------------------------------------------------------------------------------------------------
// -----------------------------------------------------------------------------------------------------------------
//
//    GUI that maintains a list of connections (doesn't actually do any connecting itself.....)
//
// -----------------------------------------------------------------------------------------------------------------
// -----------------------------------------------------------------------------------------------------------------

public class ConnectionManager
{
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  gui
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public ConnectionManager( maxdView mview_, Properties props_ )
    {
	props = props_;
	mview = mview_;
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
	public String driver_location, name, host, driver_name, account;
	
	public Database(String n, String h, String dl, String dn, String a )
	{
	    name = n; host = h; driver_location = dl; driver_name = dn; account = a;
	}
      
      public String toString() 
      { 
	return "[" + host + ":" + driver_name + ":" + driver_location + ":" + 
	       name + ":" + account + "]"; 
      }
    }
	
    private void setupDatabaseList()
    {
	DefaultListModel db_list_model = new DefaultListModel();

	for(int db=0; db < db_list_v.size(); db++)
	{
	    db_list_model.addElement( ((Database) db_list_v.elementAt(db)).name );
	}
	
	db_saves_list.setModel(db_list_model);
    }

    private void addNewDatabase()
    {
	String name = getString("Name for new connection", false, null );
	
	if(name != null)
	{
	    db_list_v.addElement(new Database(name, 
					      host_jtf.getText(), 
					      driver_location_jtf.getText(), 
					      driver_name_jtf.getText(), 
					      acct_jtf.getText()));
	    
	    setupDatabaseList();
	    
	    db_saves_list.setSelectedIndex( db_list_v.size() - 1 );

	    //updateAndSaveDatabaseList();
	    updateAndSaveDatabaseListInCommonFormat();
	}
    }

    private void deleteDatabase()
    {
	int index = db_saves_list.getSelectedIndex();
	if(index >= 0)
	{
	    Database db = (Database) db_list_v.elementAt(index);

	    // if(mview.infoQuestion("Really delete '" + db.name + "' ?", "Yes", "No") == 0)
	    {
		db_list_v.removeElementAt(index);
		setupDatabaseList();

		host_jtf.setText("");
		driver_name_jtf.setText("");
		driver_location_jtf.setText("");
		acct_jtf.setText("");

		//updateAndSaveDatabaseList();
		updateAndSaveDatabaseListInCommonFormat();		
	    }
	}
    }

    private void renameDatabase()
    {
	int index = db_saves_list.getSelectedIndex();
	if(index >= 0)
	{
	    Database db = (Database) db_list_v.elementAt(index);

	    String nm = getString("New name for '" + db.name + "'", false, null);
	
	    if(nm != null)
	    {
		db.name = nm;
		setupDatabaseList();

		//updateAndSaveDatabaseList();
		updateAndSaveDatabaseListInCommonFormat();
	    }
	}
    }
    private void databaseSelected( int index)
    {
	//System.out.println("db sel=" + index);
	if(index >= 0)
	{
	    disable_list_updates = true;

	    Database db = (Database) db_list_v.elementAt(index);
	    
	    host_jtf.setText(db.host);
	    driver_name_jtf.setText(db.driver_name);
	    driver_location_jtf.setText(db.driver_location);
	    acct_jtf.setText(db.account);
	    passwd_jtf.setText("");

	    disable_list_updates = false;
	}
    }

    private void updateCurrent()
    {
	int index = db_saves_list.getSelectedIndex();
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

/*
    private void updateAndSaveDatabaseList()
    {
	updateCurrent();

	props.put("DBConMan.account", acct_jtf.getText());
	props.put("DBConMan.host",    host_jtf.getText());
	props.put("DBConMan.driver",    driver_name_jtf.getText());
	props.put("DBConMan.driver_loc",  driver_location_jtf.getText());

//	System.out.println(db_list_v.size() + " db entries in catalog");						     
	props.put("DBConMan.n_saved_dbs",  String.valueOf( db_list_v.size() ));

	for(int dbi=0; dbi < db_list_v.size(); dbi++)
	{
	   Database db = (Database) db_list_v.elementAt(dbi);
	   if(db != null)
	     {

//	       System.out.println( "saving " + db.toString() + " ...." );

	       props.put("DBConMan.db" + dbi + ".name",   db.name);
	       props.put("DBConMan.db" + dbi + ".host",   db.host);
	       props.put("DBConMan.db" + dbi + ".driver",   db.jdbc);
	       props.put("DBConMan.db" + dbi + ".driver_loc", db.driver);
	       props.put("DBConMan.db" + dbi + ".acct",   db.account);
	     }
	}

	props.put("DBConMan.new_format_active", "true");

//	System.out.println(db_list_v.size() + " db entries saved");
    }
*/


    private void loadDatabaseList()
    {
	db_list_v = new Vector();

	// firstly, try to load things from the common format file (i.e. the one that can be shared with maxdView)

	loadCommonFormatDatabaseList();

	// then merge any entries found in the 'old skool', i.e. those listed in the maxdLoad2 saved properties 
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

	while((found) && (dbi < max_db))
	{
	    String dbn = (String) props.get("DBConMan.db" + dbi + ".name");
	    if(dbn == null)
	    {
		found = false;
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
		}
		
		// and also remove this info from the properties so it won't confuse things in the future...
		
		props.remove("DBConMan.db" + dbi + ".name");
		props.remove("DBConMan.db" + dbi + ".host");
		props.remove("DBConMan.db" + dbi + ".driver_loc");
		props.remove("DBConMan.db" + dbi + ".driver");
		props.remove("DBConMan.db" + dbi + ".acct");

		dbi++;
	    }
	}

//	System.out.println(db_list_v.size() + " db entries loaded");


	// and finally remove the entry counter from the properties so it won't confuse things in the future...
	props.remove("DBConMan.n_saved_dbs");


	// is this the first time the new format has been loaded?
	
	String check = (String) props.get("DBConMan.new_format_active");
	if(check == null)
	{
//	    System.out.println("converting from old saved connection format");
	    
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


    private void updateAndSaveDatabaseListInCommonFormat()
    {
	updateCurrent();

	Properties shared_props = new Properties();

	mview.putProperty("maxd_database_connection.selected.account",         acct_jtf.getText());
	mview.putProperty("maxd_database_connection.selected.host",            host_jtf.getText());
	mview.putProperty("maxd_database_connection.selected.driver_name",     driver_name_jtf.getText());
	mview.putProperty("maxd_database_connection.selected.driver_location", driver_location_jtf.getText());

//	System.out.println(db_list_v.size() + " db entries in catalog");
						     
	shared_props.put("maxd_database_connection.n_saved_dbs",  String.valueOf( db_list_v.size() ));

	for(int dbi=0; dbi < db_list_v.size(); dbi++)
	{
	   Database db = (Database) db_list_v.elementAt(dbi);
	   if(db != null)
	     {

//	       System.out.println( "saving " + db.toString() + " ...." );

	       shared_props.put( "maxd_database_connection.db" + dbi + ".name",            db.name );
	       shared_props.put( "maxd_database_connection.db" + dbi + ".host",            db.host );
	       shared_props.put( "maxd_database_connection.db" + dbi + ".driver_name",     db.driver_name );
	       shared_props.put( "maxd_database_connection.db" + dbi + ".driver_location", db.driver_location );
	       shared_props.put( "maxd_database_connection.db" + dbi + ".acct",            db.account );
	     }
	}


	try
	{
	    FileOutputStream out = new FileOutputStream( mview.getConfigDirectory() + "maxd_database_list.config" );

	    shared_props.save( out, "maxd database list" );

	    out.close();
	}
	catch (java.io.IOException ioe)
	{
	    //
	}

 
	
	props.put("DBConMan.new_format_active", "true");

//	System.out.println(db_list_v.size() + " db entries saved");
    }
 


    private int loadCommonFormatDatabaseList()
    {
	Properties shared_props = new Properties();

	try
	{
	    FileInputStream in = new FileInputStream( mview.getConfigDirectory() + "maxd_database_list.config");
	    
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
		db_list_v.addElement(new Database( dbn,
						   (String) shared_props.get( "maxd_database_connection.db" + dbi + ".host" ),
						   (String) shared_props.get( "maxd_database_connection.db" + dbi + ".driver_location" ),
						   (String) shared_props.get( "maxd_database_connection.db" + dbi + ".driver_name" ),
						   (String) shared_props.get( "maxd_database_connection.db" + dbi + ".account" )));
		
		dbi++;
	    }
	}


	if( shared_props.get( "maxd_database_connection.selected.account" ) != null )
	    mview.putProperty( "maxd_database_connection.account", (String) shared_props.get( "maxd_database_connection.selected.account" ) );
	
	if( shared_props.get( "maxd_database_connection.selected.host" ) != null )
	    mview.putProperty( "maxd_database_connection.host", (String) shared_props.get( "maxd_database_connection.selected.host" ) );
	
	if( shared_props.get( "maxd_database_connection.selected.driver_location" ) != null )
	    mview.putProperty( "maxd_database_connection.driver_location", (String) shared_props.get( "maxd_database_connection.selected.driver_location" ) );
	
	if( shared_props.get( "maxd_database_connection.selected.driver_name" ) != null )
	    mview.putProperty( "maxd_database_connection.driver_name", (String) shared_props.get( "maxd_database_connection.selected.driver_name" ) );

	    
/*
	acct_jtf.setText( (String) shared_props.get( "maxd_database_connection.selected.account" ) );
	host_jtf.setText( (String) shared_props.get( "maxd_database_connection.selected.host" ) );
	driver_name_jtf.setText( (String) shared_props.get( "maxd_database_connection.selected.driver") );
	driver_location_jtf.setText( (String) shared_props.get( "maxd_database_connection.selected.driver_loc" ) );
*/

//	System.out.println(db_list_v.size() + " db entries loaded");
	
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

    public JPanel createConnectPanel(final JDialog parent /*, final DatabaseConnection existing_dbcon */ )
    {
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
	connect_panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

	GridBagLayout gridbag = new GridBagLayout();
	connect_panel.setPreferredSize(new Dimension(w, h));
	connect_panel.setLayout(gridbag);

	int line = 0;

	JPanel  wrapper = new JPanel();
	GridBagLayout wrapbag = new GridBagLayout();
	wrapper.setLayout(wrapbag);

	int wline = 0;
	{
	    JLabel label = new JLabel("Database ");
	    wrapper.add(label);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = wline;
	    c.weightx = c.weighty = 0.0;
	    c.anchor = GridBagConstraints.EAST;
	    wrapbag.setConstraints(label, c);
	}
	{
	    host_jtf = new JTextField(20);
	    wrapper.add(host_jtf);
	    
	    host_jtf.setText( mview.getProperty( "maxd_database_connection.selected.host", "" ) );
	    host_jtf.getDocument().addDocumentListener(new TextChangeListener());

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = wline++;
	    c.gridwidth = 2;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    c.weightx = 2.0;
	    c.weighty = 1.0;
	    wrapbag.setConstraints(host_jtf, c);
	    
	}
	{
	    JLabel label = new JLabel(" Driver File ");
	    wrapper.add(label);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = wline;
	    c.weightx = c.weighty = 0.0;
	    c.anchor = GridBagConstraints.EAST;
      	    wrapbag.setConstraints(label, c);
	}
	{
	    driver_location_jtf = new JTextField(20);
	    wrapper.add(driver_location_jtf);
	    
	    driver_location_jtf.setText( mview.getProperty( "maxd_database_connection.selected.driver_location", "" ) );
	    driver_location_jtf.getDocument().addDocumentListener(new TextChangeListener());

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = wline;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    c.weightx = 2.0;
	    c.weighty = 1.0;
	    wrapbag.setConstraints(driver_location_jtf, c);
	    
	}
	{
	    JButton jb = new JButton("...");
	    jb.setMargin(new Insets(0,0,0,0));
	    jb.setFont(small_font);

	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			 JFileChooser fc = new JFileChooser();
				    
			 fc.setSelectedFile(new File(driver_location_jtf.getText()));
			 
			 int returnVal = fc.showOpenDialog(null);
			 
			 if (returnVal == JFileChooser.APPROVE_OPTION) 
			     {
				 File file = fc.getSelectedFile();
				 if(file != null)
				     driver_location_jtf.setText(file.getPath());
			     }
			 
			 /*try
			{
			    // driver_location_jtf.setText( mview.getFileOrDirectory( "Driver location" , driver_location_jtf.getText() ) );
			}
			catch(UserInputCancelled uic)
			{
			}*/
		    }
		});

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = wline++;
	    c.anchor = GridBagConstraints.WEST;
      	    wrapbag.setConstraints(jb, c);
	    wrapper.add(jb);
	}
	{
	    JLabel label = new JLabel(" Driver Name ");
	    wrapper.add(label);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = wline;
	    c.weightx = c.weighty = 0.0;
	    c.anchor = GridBagConstraints.EAST;
      	    wrapbag.setConstraints(label, c);
	}
	{
	    driver_name_jtf = new JTextField(20);
	    wrapper.add(driver_name_jtf);
	    
	    driver_name_jtf.setText( mview.getProperty( "maxd_database_connection.selected.driver_name", "" ) );
	    driver_name_jtf.getDocument().addDocumentListener(new TextChangeListener());

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = wline++;
	    c.gridwidth = 2;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    c.weightx = 2.0;
	    c.weighty = 1.0;
	    wrapbag.setConstraints(driver_name_jtf, c);
	    
	}

	{
	    JLabel label = new JLabel("User Name ");
	    wrapper.add(label);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = wline;
	    c.weightx = c.weighty = 0.0;
	    c.anchor = GridBagConstraints.EAST;
      	    wrapbag.setConstraints(label, c);
	}
	{
	    acct_jtf = new JTextField(20);
	    wrapper.add(acct_jtf);

	    acct_jtf.setText( mview.getProperty( "maxd_database_connection.selected.account", "" ) );
	    acct_jtf.getDocument().addDocumentListener(new TextChangeListener());

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = wline++;
	    c.gridwidth = 2;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    c.weightx = 2.0;
	    c.weighty = 1.0;
	    wrapbag.setConstraints(acct_jtf, c);
	    
	}
	{
	    JLabel label = new JLabel("Password ");
	    wrapper.add(label);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = wline;
	    c.anchor = GridBagConstraints.EAST;
	    c.weightx = c.weighty = 0.0;
	    wrapbag.setConstraints(label, c);
	}
	{
	    passwd_jtf = new JPasswordField(20);
	    wrapper.add(passwd_jtf);

	    passwd_jtf.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			//updateAndSaveDatabaseList();
			updateAndSaveDatabaseListInCommonFormat();

			if(connect_action != null)
			    connect_action.actionPerformed(null);
		    }
		});

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = wline++;
	    c.gridwidth = 2;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    c.weightx = 2.0;
	    c.weighty = 1.0;
	    wrapbag.setConstraints(passwd_jtf, c);
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
	wrapbag = new GridBagLayout();
	wrapper.setLayout(wrapbag);

	{
	    db_saves_list = new JList();

	    //updateAndSaveDatabaseList();
	    updateAndSaveDatabaseListInCommonFormat();

	      db_saves_list.addListSelectionListener(new ListSelectionListener()
		{
		  public void valueChanged(ListSelectionEvent e) 
		  {
		    int index = db_saves_list.getSelectedIndex();
		    if(index >= 0)
		      databaseSelected( index );
		  }
		});
	    
	    /*
	    MouseListener mouse_listener = new MouseAdapter() 
		{
		    public void mousePressed(MouseEvent e) 
		    {
			int index = db_saves_list.locationToIndex(e.getPoint());
			databaseSelected(index);

		    }
		};

	    db_saves_list.addMouseListener(mouse_listener);
	    */

	    db_saves_list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
	    JScrollPane jsp = new JScrollPane(db_saves_list);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.fill = GridBagConstraints.BOTH;
	    c.gridwidth = 2;
	    c.weightx = 1.0;
	    c.weighty = 8.0;
	    wrapbag.setConstraints(jsp, c);
	    wrapper.add(jsp);

	    JButton jb = new JButton("New entry");
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
	    c.gridy = 1;
	    c.gridwidth = 2;
	    wrapbag.setConstraints(jb, c);
	    wrapper.add(jb);

	    jb = new JButton("Rename");
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			renameDatabase();
		    }

		});
	    jb.setFont(small_font);
	    jb.setMargin(ins);
	    c = new GridBagConstraints();
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.weightx = 1.0;
	    c.gridx = 0;
	    c.gridy = 2;
	    wrapbag.setConstraints(jb, c);
	    wrapper.add(jb);

	    jb = new JButton("Delete");
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			deleteDatabase();
		    }

		});
	    jb.setFont(small_font);
	    jb.setMargin(ins);
	    c = new GridBagConstraints();
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.weightx = 1.0;
	    c.gridx = 1;
	    c.gridy = 2;
	    wrapbag.setConstraints(jb, c);
	    wrapper.add(jb);
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

	{
	    JPanel buttons_panel = new JPanel();
	    buttons_panel.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
	    GridBagLayout inner_gridbag = new GridBagLayout();
	    buttons_panel.setLayout(inner_gridbag);
	    {   
		final JButton jb = new JButton("Close");
		buttons_panel.add(jb);
		
		jb.setToolTipText("Close this dialog box");

		jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					     {
						 if(close_action != null)
						     close_action.actionPerformed(null);
					     }
				     });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		gridbag.setConstraints(jb, c);
	    }
	    {
		
		final JButton jb = new JButton("Connect");
		buttons_panel.add(jb);
		
		jb.setToolTipText("Attempt to open a connection to this database");
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    //updateAndSaveDatabaseList();
			    updateAndSaveDatabaseListInCommonFormat();


			    if(connect_action != null)
				connect_action.actionPerformed(null);
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		gridbag.setConstraints(jb, c);
	    }
	    {
		
		final JButton jb = new JButton("Help");
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    if(help_action != null)
				help_action.actionPerformed(null);
			}
		    });
		buttons_panel.add(jb);
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		gridbag.setConstraints(jb, c);
	    }
	    connect_panel.add(buttons_panel);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    // c.gridwidth = 2;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(buttons_panel, c);
	}

	// System.out.println("connect panel ok");

	return connect_panel;
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

    protected maxdView mview;

    protected JTextField host_jtf, acct_jtf, driver_name_jtf, driver_location_jtf;
    protected JPasswordField passwd_jtf;
    
    protected ActionListener connect_action;
    protected ActionListener close_action;
    protected ActionListener help_action;

    protected Properties props;

    protected Vector db_list_v;
    protected JList db_saves_list;
    protected  boolean disable_list_updates = false;
    
}
