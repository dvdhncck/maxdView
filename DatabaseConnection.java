import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.sql.*;
import javax.swing.event.*;
import java.util.Date;

//
// uses JDBC to retrieve data from a maxdSQL database
//

public class DatabaseConnection
{
    public DatabaseConnection(maxdView mview_)
    {
	connection = null;
	mview = mview_;
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public String getName()
    {
	if(connection == null)
	    return "unconnected";
	else
	    return maxd_props.database_name + " at " + maxd_props.database_location;
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    int connect_dialog_result = -1;

    public boolean attemptConnection()
    {
	cframe = new JDialog( mview.getMainFrame() );

	cframe.setTitle("Connect to Database");

	connect_dialog_result = -1;

	cman = new ConnectionManager( mview, mview.getProperties() );

	cman.setConnectAction( new ActionListener()
	    {
		public void actionPerformed(ActionEvent ae)
		{
		    if(attemptConnect())
		    {
			connect_dialog_result = 1;
			cframe.setVisible(false);
		    }
		    else
		    {
			System.out.println("DBCon: problem...");
		    }
		}
	    });

	cman.setCloseAction( new ActionListener()
	    {
		public void actionPerformed(ActionEvent ae)
		{
		    exitGracefully();
		    connect_dialog_result = 2;
		}
	    });

	cman.setHelpAction( new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    mview.getHelpTopic("DatabaseConnection");
		}
	    });
	
	JPanel cpanel =  cman.createConnectPanel( null );


	cframe.getContentPane().add(cpanel);
	cframe.pack();
	cframe.setModal(true);
	cframe.setVisible(true);

	
	if(connect_dialog_result == 1)
	{
	    System.out.println("DBCon: good");
	    return true;
	}
	else
	{
	    System.out.println("DBCon: bad");
	    return false;
	}
    }
    

    private void exitGracefully()
    {
	/*
	if(ccl != null)
	{
	    // the old-style custom class loader was used,
	    // now we should remove the custom path that had been installed

	    if(driver_location.toLowerCase().endsWith(".jar"))
		mview.removeClassSearchJarFile( driver_location );
	    else
		mview.removeClassSearchPath( driver_location );

	}
	*/

	cframe.setVisible(false);
    }

    // ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ 
    // ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ 
    // ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ 

    

    private Class loadDriver( final String driver_file, final String driver_name )
    {
	Class dc = null;

	boolean debug_driver = false;

/*

	// ============================================================
	// 
	// special driver/JARfile checking
	//

	final boolean search_for_driver_name_in_jar_file = true;

	if(search_for_driver_name_in_jar_file)
	{
	    if((driver_file != null) && (driver_file.length() != 0))
	    {
		//if( driver_file.toLowerCase().endsWith(".jar") || (driver_file.toLowerCase().endsWith(".zip")) )
		{
		    System.out.println("\nSearching for the driver in the specified file...");
		    
		    
		    
		    try
		    {
			ccl = mview.new CustomClassLoader( driver_file );

			//Class wc = ccl.findClass("cern.jet.stat.Gamma");
			String match = ccl.findBestMatchInJARFile( driver_name + ".class" );
			
			if(match == null)
			{
			    System.out.println("...no match found");
			}
			else
			{
			    System.out.println("...best match is '" + match + "'");
			    
			    if( !match.equals(driver_name + ".class") )
				System.out.println("...which is NOT an exact match");
			}
		    }
		    catch(java.io.IOException ioe)
		    {
			System.out.println("...unable to load JAR file (" + ioe + ")");
		    }
		}
	    }
	}

	// ============================================================
	// first try the built-in class loader, which will only work if the JDBC driver
	// classes are in the CLASSPATH
	// ============================================================
	
	if(debug_driver)
	{
	    System.err.println("\nClassPathLoader: attempting to load driver from system classpath");
	    
	    String classpath = System.getProperty("java.class.path");
	    
	    if(classpath == null)
		System.err.println("...classpath is not set");
	    else
		System.err.println("...classpath is '" + classpath + "'");
	}

	try
	{
		    
	    dc = getClass().getClassLoader().loadClass( driver_name );

	    if(debug_driver)
	    {
		if(dc != null)
		    System.err.println("...found a potential driver class");
		else
		    System.err.println("...driver class not found");
	    }
	}
	catch(Exception e)
	{
	    if(debug_driver)
	    {
		System.err.println("...driver class not found in classpath");
	    }
	}
*/

/*
	// ============================================================
	// 
	// if that has failed, try a URLClassLoader.....
	// 
	// as of 1.8.0.b2, java.net.URLClassLoader is used, much better all round
	// as it doesn't make such a mess of loading JAR files and it works
	// properly with the Oracle drivers
	// ============================================================

	if(dc == null)
	{
	    if((driver_file == null) || (driver_file.length() == 0))
	    {
		mview.alertMessage("Unable to connect:\n  No 'Driver File' has been specified,\n" + 
				   "  and the driver is not in the classpath");
		return null;
	    }


	    if(debug_driver)
		System.err.println("ClassPathLoader: load failed, trying URLClassLoader...\n");


	    try
	    {
		String fname = "file:/" + driver_file;
		
		// append a trailing '\' if the file is a directory
		if(!fname.toLowerCase().endsWith(".jar") && !fname.toLowerCase().endsWith(".zip"))
		{
		    if(!fname.endsWith( File.separator ))
			fname += File.separator;
		}
		
		if(debug_driver)
		{
		    System.err.println("URLClassLoader: attempting to load driver from '" + fname + "'...");

		    File test_file = new File( driver_file );

		    if(!test_file.exists())
		    {
			System.err.println("...this file or directory does not exist!");
		    }
		    else
		    {
			String thing = test_file.isDirectory() ? "directory" : "file";

			System.err.println("...this " + thing + " exists");
			
			if(!test_file.canRead())
			    System.err.println("...this " + thing + " is not readable!");
			else
			    System.err.println("...this " + thing + " is readable");
		    }
		}

		java.net.URL[] urls = new java.net.URL[1];
		urls[0] = new java.net.URL( fname );
		CustomLoader cl = new CustomLoader( urls, getClass() );

		if(debug_driver)
		{
		    System.err.println("URLClassLoader: attempting to find class '" + driver_name + "'");
		}

		dc = cl.findClass( driver_name );

		if(debug_driver)
		{
		    if(dc != null)
			System.err.println("...found a potential driver class");
		    else
			System.err.println("...driver class not found");
		}
		
	    }
	    catch( java.net.MalformedURLException murle )
	    {
		if(debug_driver)
		    System.err.println("...driver file not found");
	    }
	}
*/
	
/*
	// ==================================================================
	// as a fallback, the old-style CustomClassLoader can still be used
	// (the URLClassLoader doesn't appear to work on Linux with JDK1.2.2)
	// ==================================================================
	
	if(dc == null)
	{
	    if(debug_driver)
		System.err.println("URLClassLoader: load failed, trying CustomClassLoader...\n");

	    maxdView.CustomClassLoader ccl = mview.new CustomClassLoader( getClass() );

	    if(driver_file.toLowerCase().endsWith(".jar"))
		ccl.addClassSearchJarFile( driver_file );
	    else
		ccl.addClassSearchPath( driver_file );
	    
	    if(debug_driver)
	    {
		System.err.println("CustomClassLoader: attempting to find class '" + driver_name + "'");
	    }
	    
	    dc = ccl.findClass( driver_name );

	    if(debug_driver)
	    {
		if(dc != null)
		    System.err.println("...found a potential driver class");
		else
		    System.err.println("...driver class not found");
	    }
	    
	}
*/

	if((driver_file == null) || (driver_file.length() == 0))
	{
	    mview.alertMessage( "Unable to connect:\n  No 'Driver File' has been specified,\n" + 
				"  and the driver is not in the classpath" );
	    return null;
	}
	
	try
	{
	    maxdView.CustomClassLoader ccl = mview.new CustomClassLoader( driver_file );

	    if((driver_name == null) || (driver_name.length() == 0))
	    {
		mview.alertMessage( "Unable to connect:\n  No 'Driver Name' has been specified" );
		return null;
	    }
	    
	    dc = ccl.findClass( driver_name );
	}
	catch( java.net.MalformedURLException murle )
	{
	    mview.alertMessage( "Unable to load the specified 'Driver File'." );
	    
	}
	
	
	return dc;
    }

    // ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ 
    // ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ 
    // ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ 

    public void disconnect()
    {
	users--;
	System.out.println("this connection has " + users + " users");
    }

    public void reallyDisconnect()
    {
	users = 0;
	try
	{
	    if(timeout_ticker != null)
	    {
		timeout_ticker.stop();
		timeout_ticker = null;
	    }

	    if(connection != null)
	    {
		System.out.println("connection closed...");
		connection.close();

/*
		if(cman.getDriverLocation().toLowerCase().endsWith(".jar"))
		    mview.removeClassSearchJarFile( driver_location );
		else
		    mview.removeClassSearchPath( driver_location );
*/

		connection = null;
	    }

	}
	catch(SQLException sqle)
	{
	    mview.alertMessage("Unable to close connection:\n" + sqle);
	}

	connected = false;
    }

    public boolean isConnected()
    {
	return(connected);
    }

    public void useExisting( DatabaseConnection existing_dbcon )
    {

    }

    private class CustomLoader extends java.net.URLClassLoader
    {
	public CustomLoader( java.net.URL[] urls, Class parent )
	{
	    super( urls, parent.getClassLoader() );
	}
	public Class findClass(String name) 
	{
	    try
	    {
		return super.findClass( name );
	    }
	    catch(ClassNotFoundException cfne)
	    {
		return null;
	    }
	}
    }
    
    public boolean attemptConnect()
    {
	try
	{
	    if(connection != null)
	    {
		connection.close();
		connection = null;
	    }
	}
	catch(SQLException sqle)
	{
	    mview.alertMessage("Unable to close connection:\n" + sqle);
	}

	connected = false;

	if(debug_connect)
	    System.out.println("attempt to connect to " + cman.getHost() + 
			       "(account: " + cman.getAccount() + ")");

	
	
	if(cman.getDriverName().length() == 0)
	{
	    mview.alertMessage("No JDBC driver specified");
	    return false;
	}
	
	//driver_location = cman.getDriverFile();
	
	/*	
	*/

	// ============================================================
	// as of 0.9.6, java.net.URLClassLoader is used, much better all round
	// as it doesnt make such a mess of loading JAR files and it works
	// properly with the Oracle drivers
	// ============================================================

/*
	Class dc = null;

	try
	{
	    String fname = "file:\\" + driver_location;

	    // append a trailing '\' if the file is a directory
	    if(!fname.toLowerCase().endsWith(".jar") && !fname.toLowerCase().endsWith(".zip"))
	    {
		if(!fname.endsWith( File.separator ))
		    fname += File.separator;
	    }

	    java.net.URL[] urls = new java.net.URL[1];
	    urls[0] = new java.net.URL( fname );
	    CustomLoader cl = new CustomLoader( urls, getClass() );

	    dc = cl.findClass( cman.getDriverName() );
	}
	catch( java.net.MalformedURLException murle )
	{
	    System.err.println("** driver file not found");
	}

	// use the olld method as a fallback if the above doesn't work

	if(dc == null)
	{
	    System.out.println("URLClassLoader failed, trying built-in loader...");

	    if(driver_location.toLowerCase().endsWith(".jar"))
		mview.addClassSearchJarFile( driver_location );
	    else
		mview.addClassSearchPath( driver_location );
	    
	    ccl = mview.new NewCustomClassLoader( mview.getClass() );
	    
	    dc = ccl.findClass( cman.getDriverName() );

	    
	}


	if(dc == null)
	{
	    String msg = null;
	    File testfile = new File( driver_location );
	    if(testfile.exists())
	    {
		msg = "Unable to load JDBC driver '" + cman.getDriverName() + "'\n";
		msg += (driver_location == null)  ? "\n" : ("(in '" + driver_location + "')\n");
	    }
	    else
	    {
		msg = "Specified 'Driver File' does not exist";
	    }
	    mview.alertMessage(msg);
	    return false;
	}
*/

	Class dc = loadDriver( cman.getDriverFile(), cman.getDriverName() );

	// make an instance of this class
	java.sql.Driver driver = null;
	
	try
	{
	    driver = (java.sql.Driver) dc.newInstance();
	    
	    //DriverManager.registerDriver( driver );
	    
	    System.out.println("driver " + driver.getMajorVersion() +"." +  driver.getMinorVersion() +
	    		   " has been registered");
	    
	}
	catch(Exception e)
	{
	    System.err.println("unable to instantiate Driver");
	    mview.alertMessage("Unable to instantiate JDBC Driver");
	    return false;
	}
	
	    
	Properties acct_props = new Properties();
	
	final String ac = new String( cman.getAccount() );
	if(ac.length() > 0)
	    acct_props.setProperty("user",  ac);
	
	final String pw = new String( cman.getPassword() );
	if(pw.length() > 0)
	    acct_props.setProperty("password", pw);
	
	
	
	try
	{
	    connection = driver.connect( cman.getHost(), acct_props) ;
	}
	catch(Exception e)
	{
	    String msg = "Unable to connect\n";
	    if( ac.length() == 0 )
		msg += "\n  Missing account name?\n";
	    if( pw.length() == 0 )
		msg += "\n  Missing password?\n";
	    msg += "\nError was: " + e;
	    mview.alertMessage(msg);
	    e.printStackTrace();
	    return false;
	}

	/*
	  connection = DriverManager.getConnection(host_jtf.getText(), 
	  acct_jtf.getText(), 
	  new String(passwd_jtf.getPassword()));
	  
	*/
	
	if(connection != null)
	{
	    if(debug_connect)
		System.out.println("connected to " + cman.getHost() );
	    
	    getDatabaseProperties();
	    
	    timeout_counter = 0;
	    
	    timeout_ticker = new javax.swing.Timer(timeout_timer_freq * 1000, new ActionListener() 
		{
		    public void actionPerformed(ActionEvent evt) 
		    {
			if(((++timeout_counter) * timeout_timer_freq) > timeout_limit)
			{
				// make sure nobody is still using the connection
			    if(users == 0)
			    {
				System.out.println("connection timeout!");
				if(timeout_ticker != null)
				    timeout_ticker.stop();
				    reallyDisconnect();
			    }
			    else
			    {
				// reset
				timeout_counter = 0;
			    }
			}
			// System.out.println((timeout_counter * timeout_timer_freq) + " ..." + timeout_limit);
		    }
		    
		});
	    timeout_ticker.start();
	    
	    connected = true;
	}
	
	return connected;
    }

    javax.swing.Timer timeout_ticker = null;
    
    final int timeout_timer_freq = 60;  // called every N seconds
    
    final int timeout_limit      = 10 * 60;  // limit in seconds
    int timeout_counter = 0;

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  wrapper for batch operation
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public final void startBatch()
    {
	try
	{
	    connection.setAutoCommit(false);
	    timeout_counter = 0;
	}
	catch(SQLException sqle)
	{
	    //alertMessage("Unable to disable autocommit mode");
	}
	
    }

    public final void endBatch()
    {
	try
	{
	    connection.commit();
	    timeout_counter = 0;
	}
	catch(SQLException sqle)
	{
	    mview.alertMessage("Unable to commit batch transaction. Data not uploaded.");
	}
	try
	{
	    connection.setAutoCommit(true);
	}
	catch(SQLException sqle)
	{
	}
    }

    public final void cancelBatch()
    {
	try
	{
	    connection.rollback();
	    mview.infoMessage("Batch transaction cancel, rollback successful");
	    timeout_counter = 0;
	}
	catch(SQLException sqle)
	{
	    mview.alertMessage("Unable to cancel batch transaction. Data not uploaded.");
	}
	try
	{
	    connection.setAutoCommit(true);
	}
	catch(SQLException sqle)
	{
	}
    }
    
		   
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  queries and statements
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public ResultSet executeQuery(String sql)
    {
	timeout_counter = 0;
	
	try
	{
	    // System.out.println("executeQuery(): exec:\n  " + sql);
	    
	    Statement stmt = connection.createStatement();
	    stmt.setEscapeProcessing(false);
	    ResultSet rs = stmt.executeQuery(sql);
	    
	    //System.out.println("executeQuery(): ResultSet retrieved ok");
	    
	    /*
	      ResultSetMetaData rsmd = rs.getMetaData();
	      
	      int cc = rsmd.getColumnCount();
	      System.out.println("               " + cc + " columns");
	      for(int ccc=0;ccc<cc;ccc++)
	      System.out.println("                " + rsmd.getColumnName(ccc+1));
	    */
	    
	    return rs;
	}
	catch(SQLException sqle)
	{
	    String emesg = (sqle.toString().length() > 256) ? sqle.toString().substring(0,255) : sqle.toString();
	    String sql_short = (sql.length() > 256) ? sql.substring(0,255) : sql;
	    
	    //mview.alertMessage("executeQuery(): Unable to execute SQL query:\n  '" + sql_short + "'\nerror: " + emesg);
	    System.out.println("executeQuery(): Unable to execute SQL query:\n  '" + sql_short + "'\nerror: " + emesg);
	}
	return null;
    }


    public synchronized boolean executeStatement(String sql)
    {
	timeout_counter = 0;

	if((sql == null) || (sql.length() == 0))
	   return true;

	{
	    try
	    {
	        // System.out.println("executeStatement(): trying to exec:\n  " + sql);
	      
		Statement stmt = connection.createStatement();
		stmt.setEscapeProcessing(false);
		stmt.executeUpdate(sql);
		stmt.close();
		stmt = null;

		return true;
	    }
	    catch(SQLException sqle)
	    {
		String sql_short = (sql.length() > 256) ? sql.substring(0,255) : sql;
		String emesg = (sqle.toString().length() > 256) ? sqle.toString().substring(0,255) : sqle.toString();
		
		mview.alertMessage("executeQuery(): Unable to execute SQL update:\n  '" + sql_short + "'\nerror: " + emesg);
	    }
	    return false;
	}
    }


    // TODO: could keep a table of all currently not-closed ResultSets for debugging
    //
    //       could also test for multiple closings of the same ResultSet (/)
    //
    public final void closeResultSet( final ResultSet rs )
    {
	if(rs != null)
	{
	    try
	    {
		Statement stmt = null;

		try
		{
		    stmt = rs.getStatement();
		}
		catch( Error ex) // AbstractMethodError
		{
		}
 
		rs.close();

		if(stmt != null)
		    stmt.close();

		//  still_open_rs.remove( rs );
	    }
	    catch(Exception e)
	    {
		// System.err.println("warning: unable to close ResultSet");
	    }
	}
    }

  

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  utils: getFieldFromTable
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public String getFieldFromTable(String tname, String tid, String cname)
    {
	timeout_counter = 0;

	if(tid == null)
	    return null;

	String sql = 
	"SELECT " + qField(cname) + 
	" FROM " + qTable(tname) + 
	" WHERE " + qField("ID") + " = " + qID(tid);

	ResultSet rs = executeQuery(sql);

	String result = null;

	if(rs != null)
	{
	    try
	    {
		
		if(rs.next())
		{
		    //System.out.println(tname + ": id=" + tid + " " + cname + "=" +  rs.getString(1));
		    result = rs.getString(1);
		}
	    }
	    catch(SQLException sqle)
	    {
		mview.alertMessage("SQL error\n" + sqle);
	    }
	    finally
	    {
		closeResultSet( rs );
	    }
	}
	
	return result;
    }

    private int next_id = 0;

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  utils: generateUniqueID
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
 
    public String generateUniqueID(String table_name, String column_name)
    {
	timeout_counter = 0;

	{
	    String result = null;

	    int max_id = 0;
	    
	    // NEW: try to use MAX(INT(ID)), and if that fails then try MAX(ID) instead
	    //  (to try to keep MySQL and PostgreSQL happy at the same time...
	    //    dont know which one is correct SQL92)
	    //
	    ResultSet rs = null;
	    
	    rs = executeQuery("SELECT MAX(INT(" + qField(column_name) + ")) FROM " + qTable(table_name));
	    if(rs == null)
	    {
		rs = executeQuery("SELECT MAX(" + qField(column_name) + ") FROM " + qTable(table_name));
	    }
	    
	    //System.out.println("SELECT MAX(INT(" + column_name + ")) FROM \"" + table_name + "\"");

	    if(rs != null)
	    {
		try
		{
		    while (rs.next()) 
		    {
			String str = rs.getString(1);
			if(str != null)
			{
			    try
			    {
				//System.out.println("generateUniqueID(): max id found, is " + str);
				max_id = (new Integer(str).intValue()) + 1;
			    }
			    catch (NumberFormatException nfe)
			    {
				max_id = 0;
				//alertMessage("generateUniqueID(): " + nfe);
			    }
			}
			//System.out.println("generateUniqueID(): max id in " + column_name + " is " + max_id);
		    }
		}
		catch(SQLException sqle)
		{
		    mview.alertMessage("generateUniqueID(): " + sqle);
		}
		finally
		{
		    closeResultSet(rs);
		}

		try
		{
		    result = new String(Integer.toString(max_id));
		}
		catch (NumberFormatException nfe)
		{
		    result = "0";
		}

	    }
	    else
	    {
		mview.alertMessage("generateUniqueID(): table " + qTable(table_name) + " missing?");  
	    }

	    return result;
	}
    }
 
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  utils: generateDescriptionID
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    //
    // add a new description row to the database
    // (if the description is longer than N chars, then split
    //  it into a number of chunks and store each of 
    //  them with the same ID, and sequential indices so they can be 
    //  joined together on retrieval)
    //
    //  call with 'desc' == null to just generate
    //  an empty Description entry, used for linking
    //  Property's to
    //
    public String generateDescriptionID(String desc)
    {
	timeout_counter = 0;

	String desc_id = generateUniqueID("Description", "ID");
	
	int n_desc_rows = 1;
	String[] desc_rows = null;

	if((desc != null) && (desc.length() > maxd_props.long_text_len))
	{
	    int start = 0;
	    n_desc_rows = (desc.length() / maxd_props.long_text_len) + 1;
	    desc_rows = new String[n_desc_rows];
	    for(int dr=0; dr < n_desc_rows; dr++)
	    {
		int end = start + maxd_props.long_text_len;
		if(end > desc.length())
		{
		    end = desc.length();
		}
		if(start < desc.length())
		{
		    desc_rows[dr] = desc.substring(start, end);
		}
		start += maxd_props.long_text_len;
	    }
	}
	else
	{
	    if((desc != null) && (desc.length() > 0))
	    {
		n_desc_rows = 1;
		desc_rows = new String[1];
		desc_rows[0] = desc;
	    }
	    else
	    {
		n_desc_rows = 0;
	    }
	}

	String sql = "INSERT INTO " + qTable("Description") + " (" + qField("ID") + ") VALUES (" + qDescID(desc_id) + ")\n";
	
	if(executeStatement(sql) == false)
	{
	    return null;
	}

	for(int dr=0; dr < n_desc_rows; dr++)
	{
	    StringBuffer sbuf = new StringBuffer();

	    sbuf.append("INSERT INTO ");
	    sbuf.append(qTable("TextProperty"));
	    
	    sbuf.append(" ( " + qField(index_field_name) + ", ");
	    sbuf.append(qField("Description_ID") + ", " + qField("Text") + ")\n");
	    
	    sbuf.append("VALUES ( " + dr + ", ");
	    sbuf.append(qDescID(desc_id));
	    sbuf.append(", ");
	    sbuf.append(qText(desc_rows[dr].trim()));
	    sbuf.append(" )\n");
	    
	    if(executeStatement(sbuf.toString()) == false)
	    {
		return null;
	    }
	}

	return desc_id;
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  maxdProperties
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public class maxdProperties
    {
	public String database_name;            
	public String database_version;     
	public String database_location;    
	public String database_is_copy_of;  
	public String database_copied_date; 
	
	public int identifier_len;
	public int long_text_len;

	public Hashtable translations;

	public maxdProperties() 
	{ 
	    translations = new Hashtable(); 
	}
	
    }

    public maxdProperties maxd_props;

    private boolean getDatabaseProperties()
    {
	timeout_counter = 0;

	// attempt to automatically figure out the quoting convention
	// 
	// (because MyMSQL is non-ansi compliant)
	//
	boolean found = false;

	maxd_props = new maxdProperties();
	
	for(int tmode=0; tmode < 3; tmode++)
	    for(int fmode=0; fmode < 3; fmode++)
		for(int cmode=0; cmode < 2; cmode++)   // added in 0.9.7
		{
		    if(!found)
		    {
			quote_table_mode = tmode;
			quote_field_mode = fmode;
			lower_case_mode = (cmode == 1) ? true : false;

			String test_sql = "SELECT " + qField("Database_Name") + " FROM " + qTable("maxdProperties");
			try
			{
			    Statement stmt = connection.createStatement();
			    stmt.setEscapeProcessing(false);
			    ResultSet rs = stmt.executeQuery(test_sql);
			    System.out.println("passed: " + test_sql);
			    
			    rs.close();
			    stmt.close();
			    
			    found = true;
			}
			catch(SQLException sqle)
			{
			    //System.out.println("failed: " + test_sql);
			}
			
		    }
		}
	
	if(!found)
	    mview.alertMessage("Unable to determine quoting convention.\n Please report this error");
	
	//
	// get then name and check the version number is ok
	//

	String get_props_sql = 
	"SELECT " + 
	qField("Database_Name") + ", " + 
	qField("Database_maxd_Version") + ", " + 
	qField("Database_Location") + ", " + 
	qField("Is_Copy_Of") + ", " + 
	qField("Copied_Date") + ", " + 
	qField("Identifer_Len") + ", " + 
	qField("Long_Text_Len") + 
	" FROM " + 
	qTable("maxdProperties");

	ResultSet rs = executeQuery(get_props_sql);
	
	try
	{
	   
	    while (rs.next()) 
	    {

		if(rs.getString(1) != null)
		{
		    maxd_props.database_name  = new String(rs.getString(1));
		    System.out.println("Name:" + maxd_props.database_name);
		}
		else
		{
		    mview.alertMessage("Unable to determine maxd database name. You should update to the latest version of the database");
		    return false;
		}

		if(rs.getString(2) != null)
		{
		    maxd_props.database_version = new String(rs.getString(2));
		    System.out.println("Version:" + maxd_props.database_version);
		    if(maxd_props.database_version.startsWith("1.2") == false)
		    {
			mview.alertMessage("Database is version '" + maxd_props.database_version 
				     + "'\nThis plugin can only operate properly with version 1.2.x databases.");
			return false;
		    }
		}
		else
		{
		    mview.alertMessage("Unable to determine maxd database version. You should update to the latest version of the database");
		    return false;
		}
		
		if(rs.getString(3) != null)
		{
		    maxd_props.database_location = new String(rs.getString(3));
		    System.out.println("Location:" + maxd_props.database_location);
		}		
		
		if(rs.getString(4) != null)
		{
		    maxd_props.database_is_copy_of = new String(rs.getString(4));
		    System.out.println("Is Copy Of:" + maxd_props.database_is_copy_of);
		}
		if(rs.getString(4) != null)
		{
		    maxd_props.database_copied_date = new String(rs.getString(5));
		    System.out.println("Copied Date:" + maxd_props.database_copied_date);
		}
		try
		{
		    if(rs.getString(6) != null)
		    {
			maxd_props.identifier_len = (Integer.valueOf(rs.getString(6))).intValue();
		    }
		    if(rs.getString(7) != null)
		    {
			maxd_props.long_text_len = (Integer.valueOf(rs.getString(7))).intValue();
		    }
		}
		catch(NumberFormatException nfe)
		{
		    mview.alertMessage("Warning: unable to determine maximum field sizes\nIs this really a maxdSQL 1.2 database");
		}
	    }
	}
	catch(java.sql.SQLException sqle)
	{
	    mview.alertMessage("Unable to find database properties. Is this really a maxd database?");
	    return false;
	}
	finally
	{
	    closeResultSet( rs );
	}
	//
	// get any translations that are used by this database
	//
	
	String get_names_sql = 
	"SELECT " + qField("maxd_Name") + ", " + qField("Actually_Called") + " FROM " + qTable("maxdTranslations");

	rs = executeQuery(get_names_sql);
	    
	    
	try
	{
	    while (rs.next()) 
	    {
		if((rs.getString(1) != null) && (rs.getString(2) != null))
		{
		    maxd_props.translations.put(rs.getString(1), rs.getString(2));

		    //System.out.println("Translate: " + rs.getString(1) + " to " +  rs.getString(2));
		}
	    }
	}
	catch(java.sql.SQLException sqle)
	{
	}
	finally
	{
	    closeResultSet( rs );
	}

	//
	// get the 'offical' quoting conventions for the database
	//
	String get_quotes_sql = 
	"SELECT " + 
	qField("Table_Quote_Mode") + ", " + qField("Field_Quote_Mode") + ", " + 
	qField("Text_Quote_Mode") + ", " + qField("ID_Quote_Mode") + 
	" FROM " + qTable("maxdProperties");
	
	rs = executeQuery(get_quotes_sql);
	    
	try
	{
	    while (rs.next()) 
	    {
		quote_table_mode = parseQuoteName(rs.getString(1));
		quote_field_mode = parseQuoteName(rs.getString(2));
		quote_text_mode  = parseQuoteName(rs.getString(3));
		quote_id_mode    = parseQuoteName(rs.getString(4));

		/*
		System.out.println("Quoting conventions for this database");
		System.out.println("Table: " + getQuoteName(quote_table_mode));
		System.out.println("Field: " + getQuoteName(quote_field_mode));
		System.out.println(" Text: " + getQuoteName(quote_text_mode));
		System.out.println("   ID: " + getQuoteName(quote_id_mode));
		*/
	    }
	}
	catch(java.sql.SQLException sqle)
	{
	    mview.alertMessage("Unable to find database properties. Is this really a maxd database?");
	    return false;
	}
	finally
	{
	    closeResultSet( rs );
	}

	return true;
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  quoting
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    //
    // special handling of different quoting conventions
    //

    public final static int QuoteNone = 0;
    public final static int QuoteSingle = 1;
    public final static int QuoteDouble = 2;

    private final char[] quote_char = { ' ', '\'', '\"' };

    // variable depending on database
    private int quote_table_mode = QuoteDouble;
    private int	quote_field_mode = QuoteNone;
    private int	quote_text_mode  = QuoteSingle;
    private int	quote_id_mode  = QuoteSingle;     // assume IDs are VARCHARS not INTEGERS

    private boolean lower_case_mode = false;

    // convert name to symbol
    private int parseQuoteName(String name)
    {
	if(name == null)
	    return QuoteNone;
	if(name.equals("single"))
	    return QuoteSingle;
	if(name.equals("double"))
	    return QuoteDouble;
	return QuoteNone;	
    }
    // convert symbol to name
    private String getQuoteName(int sym)
    {
	final String[] names = { "none", "single", "double" };
	return names[sym];
    }

    public String translate(String name)
    {
	try
	{
	    String t_name = (String) maxd_props.translations.get(name);
	    
	    if( t_name == null )
	    {
		return lower_case_mode ? name.toLowerCase() : name;
	    }
	    else
	    {
		return lower_case_mode ? t_name.toLowerCase() : t_name;
	    }
	}
	catch(java.lang.NullPointerException npe)
	{
	    return name;
	}
    }

    public String qTable(String name)
    {
	return quote_char[quote_table_mode] + translate(name) + quote_char[quote_table_mode];
    }

    public String qField(String name)
    {
	return quote_char[quote_field_mode] + translate(name) + quote_char[quote_field_mode];
    }
    
    public String qTableDotField(String tname, String fname)
    {
	return (quote_char[quote_table_mode] + translate(tname) + quote_char[quote_table_mode] + 
		"." +
	        quote_char[quote_field_mode] + translate(fname) + quote_char[quote_field_mode]);
    }

    public String qText(String name)
    {
	return quote_char[quote_text_mode] + name + quote_char[quote_text_mode];
    }

    public String qID(String id)
    {
	return quote_char[quote_id_mode] + id + quote_char[quote_id_mode];
    }

    public String qDescID(String id)
    {
	return id;
    }

    // remove any illegal characters from the string
    // by escaping them with a '\'
    //
    public String tidyText(String str)
    {
	if(str == null)
	    return null;
	
	str = str.trim();

	if((str.indexOf('\'') >= 0) ||
	   (str.indexOf('\\') >= 0))
	{
	    StringBuffer result = new StringBuffer(str.length());
	    
	    // have to be careful that the string isn't already tidied
	    str = untidyText(str);

	    int c = 0;
	    while(c < str.length())
	    {
		char ch = str.charAt(c);
		
		switch(ch)
		{
		case '\\':
		case '\'':
		case  '"':
		    result.append('\\');
		    result.append(ch);
		    break;

		case '\n':
		    result.append("\\n");
		    break;

		case '\t':
		    result.append("\\t");
		    break;

		default:
		    result.append(ch);
		}

		c++;
	    }

	    return result.toString();
	}
	else
	{
	    return str;
	}

    }

    // is the character at the specified index prefixed by the escape char '\'
    //
    private boolean isEscaped(String str, int index)
    {
	if(index == 0)
	    return false;
	return (str.charAt(index - 1) == '\\');
    }

    // expand any escaped chars into their internal rep
    //
    private String untidyText(String str)
    {
	if(str == null)
	    return null;

	StringBuffer result = new StringBuffer(str.length());
	
	int c = 0;
	boolean escaped = false;

	while(c < str.length())
	{
	    char ch = str.charAt(c);
	    
	    if(ch != '\\')
	    {
		if(escaped)
		{
		    switch(ch)
		    {
		    case 'n':
			result.append('\n');
			break;
		    case 't':
			result.append('\t');
			break;
		    default:
			result.append(ch);
			break;
		    }
		    escaped = false;
		}
		else
		{
		    result.append(ch);
		}
	    }
	    else
	    {
		// watch out for '\\'
		//
		if(!escaped)
		{
		    escaped = true;
		}
		else
		{
		   result.append(ch); 
		   escaped = false;
		}
	    }
	    
	    c++;
	}
	
	//System.out.println("untidy(): " + str + " --> " + result.toString());

	return result.toString();
    }
	
    public String row_field_name = new String("Row");
    public String col_field_name = new String("Column");
    public String index_field_name = new String("Index");


    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

    private ConnectionManager cman;
    private JDialog cframe;

    //maxdView.NewCustomClassLoader ccl = null;

    private boolean connected = false;
    private int users = 0;

    final boolean debug_connect = true; //false;
	
    protected Connection connection = null;
	
    protected maxdView mview;
    protected ExprData edata;

    //protected JTextField host_jtf, acct_jtf, jdbc_jtf, driver_jtf;
    //protected JPasswordField passwd_jtf;

    protected String driver_location = null;
}
