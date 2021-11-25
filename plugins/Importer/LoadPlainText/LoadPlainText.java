import javax.swing.*;
import javax.swing.event.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.zip.*;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
//import ExprData.*;
import javax.swing.table.*;


import javax.swing.table.AbstractTableModel;
import javax.swing.DefaultCellEditor;
import javax.swing.table.TableCellRenderer;

//
// recent changes:
//
//   22/06/04:    + Added support for +/- Infinity
//
//
//   20/02/04:    + Added support for different character encodings
//
//
//   23/05/02:    + "Really replace?" dialog fixed (yes/no were the wrong way 'round)
//
//                + plugins_commands udpated to match functionality
//
//
//   26/04/02:    + Row parsing altered  (to include MeasurementAttrs etc)
//
//                + Table layout improved 
//
// 
//   30/11/01:    + fixed behaviour of drop-downs under 1.2.2
//
//                + auto-parse sets 'from' and 'to' line values
//
//                + values from text fields are updated on the fly
//

public class LoadPlainText implements Plugin
{
    static final boolean debug = false;

    static final String plugin_name = "Load Plain Text v2.2";

    final String[] load_mode_opts = { "Replace", "Merge"};
    final String[] delim_opts     = { "TAB", "Space", "Comma" };
    final String[] encoding_opts  = { "US-ASCII", "ISO-LATIN-1", "UTF-8", "UTF-16BE", "UTF-16LE", "UTF-16" };

    public LoadPlainText(maxdView mview_)
    {
	mview = mview_;
	edata = mview.getExprData();
    }

    public void cleanUp()
    {
	if( fd != null )
	    if( ( fd.lines > 0 ) && ( fd.cols > 0 ) )
		frame.quickSetSave( new File( previous_settings_file ), false );


	if(jfc != null)
	{
	    // save the current path in the app properties

	    String cur_path = jfc.getCurrentDirectory().getPath();
	    
	    mview.getProperties().put("LoadPlainText.data_load_directory", cur_path);
	}

	if( quick_set_frame != null )
	    quick_set_frame.setVisible(false);

	if(frame != null)
	    frame.setVisible(false);


	fd.data = null;
	pd = null;
	fd = null;

	System.gc();
    }

    public void startPlugin()
    {
	previous_settings_file = mview.getConfigDirectory() + "load-plain-text-default-settings.dat";

	System.out.println( "previous_settings_file = " + previous_settings_file );

	frame = new LoaderFrame();

	frame.addWindowListener(new WindowAdapter() 
	    {
		public void windowClosing(WindowEvent e)
		{
		    cleanUp();
		}
	    });

    }
    
    public void stopPlugin()
    {
	cleanUp();
    }
  
    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("Load Plain Text", "importer", 
					 "Parses data from (compressed) plain text", "", 2, 2, 0);
	return pinf;
    }

    public PluginCommand[] getPluginCommands()
    {
	PluginCommand[] com = new PluginCommand[3];

	String[] args = new String[] 
	{ 
	    // name                 // type      //default   // flag   // comment
	    "file",                 "file",      "",         "",       "source file name",
	    "mode" ,                "string",    "merge",    "",       "either 'replace' or 'merge'",

	    "really_replace",       "boolean",   "false",    "",       "override the confirmation dialog when the file already exists",
	    	    
	    "apply_quickset_file",  "file",      "",         "",       "a file containing 'QuickSet' parser settings",

	    "delimiter" ,     	    "string",    "tab",      "",       "one of 'tab', 'space' or 'comma'",
	    "col_names_line", 	    "integer",   "", 	     "",       "", 
	    "missing_value",  	    "string",    "", 	     "",       "", 
	    "comment_prefix", 	    "string",    "", 	     "",       "", 
	    	    
	    "auto_parse",     	    "boolean",   "true",     "",       "runs AutoParse once the data is loaded", 
	    "report_status" , 	    "boolean",   "true",     "",       "show either success or failure message after loading"
	};

	com[0] = new PluginCommand("start", null);   // display the UI

	com[1] = new PluginCommand("set",  args);    // sets one or more arguments

	com[2] = new PluginCommand("load", args);    // display the UI, set one or more arguments & import file

	return com;
    }
    public void runCommand(String name, String[] args, CommandSignal done) 
    { 
	if(name.equals("start"))
	{
	    startPlugin();
	    if(done != null)
		done.signal();
	    return;
	}

	if((name.equals("load")) || (name.equals("set")))
	{
	    boolean started_this_time = false;

	    if(frame == null)
	    {
		started_this_time = true;
		startPlugin();
	    }

	    String dname = mview.getPluginArg("delimiter", args);
	    if(dname != null)
	    {

		if(dname.toLowerCase().equals("tab"))
		    frame.delim_jcb.setSelectedIndex(0);
		if(dname.toLowerCase().equals("space"))
		    frame.delim_jcb.setSelectedIndex(1);
		if(dname.toLowerCase().equals("comma"))
		    frame.delim_jcb.setSelectedIndex(2);
	    }

	    String arg = null;

	    arg = mview.getPluginStringArg("mode", args, "merge");
	    
	    frame.load_mode_jcb.setSelectedIndex( arg.equals( "merge" )  ? 1 : 0 );

	    //frame.merge_jrb.setSelected(arg.equals("merge"));
	    //frame.replace_jrb.setSelected(!arg.equals("merge"));
	    
	    frame.com_jtf.setText(mview.getPluginArg("comment_prefix", args, ""));

	    frame.missing_jtf.setText(mview.getPluginArg("missing_value", args, ""));

	    //frame.colname_jtf.setText( mview.getPluginArg("col_names_line", args, ""));

	    final String file_name = mview.getPluginStringArg("file", args, "");

	    fd.filename = file_name;
	    fd.file = new File( file_name );

	    // now load the file into the table
	    frame.loadAllTokensActual();


	    // give it a few moments to update things...
	    try
	    {
		Thread.sleep(1000);
	    }
	    catch(InterruptedException ie)
	    {
	    }

	    System.out.println( "data loaded..." );


	    silent_running = (mview.getPluginBooleanArg("report_status", args, true) == false);

	    override_really_replace = mview.getPluginBooleanArg("really_replace", args, false);

	    if(mview.getPluginBooleanArg("auto_parse", args, false))
		(frame.new AutoParseThread()).run();  // note: calling run() not start() to make it synchronous

	    if(name.equals("load"))
	    {
		// where there some QuickSet settings to apply now?

		arg = mview.getPluginStringArg( "apply_quickset_file", args, null );

		if( arg != null )
		{
		    frame.quickSetLoad( new File( arg ), false );

		}

		frame.importData(done);

		
	    }
	    else
	    {
		if(done != null)
		    done.signal();
	    }
	}
    } 

    public class ImportingException extends Exception
    {
	public ImportingException( String m )
	{
	    super( m );
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   L o a d e r F r a m e
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public class LoaderFrame extends JFrame
    {
	public final static short Ignore               = 0;
	       
	public final static short SpotName             = 1;
	public final static short SpotNameAttr         = 2;
	public final static short ProbeName            = 3;
	public final static short ProbeNameAttr        = 4;
	public final static short GeneName             = 5;
	public final static short GeneNameAttr         = 6;
	public final static short Spot                 = 7;
	public final static short SpotAttrPrev         = 8;
	public final static short SpotAttrNext         = 9;   
	public final static short MeasurementAttrName  = 10;
	
	public final String[] col_type_names = 
	{
	    "Ignore", 
	    "SpotName",
	    "SpotNameAttr", 
	    "ProbeName",     
	    "ProbeNameAttr", 
	    "GeneName",      
	    "GeneNameAttr",  
	    "Data",          
	    "DataAttrPrev",  
	    "DataAttrNext",
	    "MeasAttrName",
	};        


	public final static short ColumnHeader    = 1;
	public final static short MeasurementAttr = 2; 
	public final static short Data            = 3;

	public final String[] row_type_names = 
	{
	    "Ignore", 
	    "ColumnHeader",    // a.k.a. MeasurementName ?? 
	    "MeasurementAttr",
	    "Data"      
	};        

	// ==========================================================================

	public boolean importData( CommandSignal done )
	{
	    boolean replace_mode = (load_mode_jcb.getSelectedIndex() == 0);

	    if(replace_mode && (edata.getNumMeasurements() > 0))
	    {
		if(!override_really_replace)
		    if(mview.infoQuestion("Really replace existing Measurements?", "Yes", "No") == 1)
		    {
			if(done != null)
			    done.signal();
			
			return false;
		    }
	    }

	    if(replace_mode && (edata.getNumClusters() > 1))
	    {
		if(!override_really_replace)
		    if(mview.infoQuestion("Remove existing clusters?", "Yes", "No") == 0)
		    {
			edata.removeAllClusters();
		    }
	    }
	    
	    new ImporterThread(done).start();
	    
	    return true;
	}

	private class ImporterThread extends Thread
	{
	    private CommandSignal done;
		
	    public ImporterThread(CommandSignal done_)
	    {
		done = done_;
	    }

	    public void run()
	    {
		//pm = new ProgressOMeter("Checking format");
		
		//pm.startIt();
		boolean replace_mode = (load_mode_jcb.getSelectedIndex() == 0);
	    
		try
		{
		    if(checkParseDetails())
		    {
			if(replace_mode)
			    edata.removeAllMeasurements();
			
			ExprData.DataTags dtags = makeDataTags();
			
			if(dtags != null)
			{
			    try
			    {
				addMeasurements( dtags );
				cleanUp();
			    }
			    catch( ImportingException ie )
			    {
				mview.alertMessage( ie.getMessage() );
			    }


			}
		    }
		}
		catch(Exception ex)
		{
		    ex.printStackTrace();
		}

		if(done != null)
		    done.signal();
	    }
	}

	// ==========================================================================

	public LoaderFrame()
	{
	    setTitle("Load Plain Text");
	    mview.decorateFrame(this);
	    JPanel panel = new JPanel();
	    GridBagLayout gridbag = new GridBagLayout();
	    panel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
	    panel.setLayout(gridbag);
	    GridBagConstraints c = null;
	    
	    fd = new FileDetails();
	    pd = new ParseDetails();

	    //pm = new ProgressOMeter("Loading");

	    int line = 0;
	    
	    {
	      JPanel wrapper = new JPanel();
	      GridBagLayout w_gridbag = new GridBagLayout();
	      wrapper.setLayout(w_gridbag);
  
	      // -----------
  
	      {
	 	  JPanel i_wrapper = new JPanel();
	 	  i_wrapper.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createEtchedBorder(), 
									   BorderFactory.createEmptyBorder( 3,3,3,3 ) ) );
	 	  GridBagLayout i_gridbag = new GridBagLayout();
	 	  i_wrapper.setLayout(i_gridbag);
	 	  
	 	  JLabel label = new JLabel(" File ");
	 	  c = new GridBagConstraints();
	 	  c.gridx = 0;
	 	  c.anchor = GridBagConstraints.EAST;
	 	  i_gridbag.setConstraints(label, c);
	 	  i_wrapper.add(label);
	 	  
	 	  filename_jtf = new JTextField(20);
	 	  c = new GridBagConstraints();
	 	  c.gridx = 1;
	 	  c.weightx = 9.0;
	 	  c.fill = GridBagConstraints.HORIZONTAL;
	 	  c.anchor = GridBagConstraints.WEST;
	 	  i_gridbag.setConstraints(filename_jtf, c);
	 	  i_wrapper.add(filename_jtf);
	 	  
	 	  filename_jtf.addActionListener(new ActionListener() 
	 	  {
	 	      public void actionPerformed(ActionEvent e) 
	 	      {
	 		  filenameHasChanged();
	 	      }
	 	  });
	 	  

	 	  JButton jb = new JButton("Browse");
	 	  jb.setFont(mview.getSmallFont());
		  jb.setMargin(new Insets(0,0,0,0));
	 	  c = new GridBagConstraints();
	 	  c.gridx = 2;
	 	  //c.weightx = 1.0;
	 	  c.fill = GridBagConstraints.VERTICAL;
		  c.anchor = GridBagConstraints.WEST;
	 	  i_gridbag.setConstraints(jb, c);
	 	  i_wrapper.add(jb);
  
		  jb.addActionListener(new ActionListener() 
		      {
	 	      public void actionPerformed(ActionEvent e) 
			  {
			      filenameBrowse();
			  }
		      });
		  
	 	  c = new GridBagConstraints();
	 	  c.gridx = 0;
	 	  c.gridy = 0;
	 	  c.weightx = 10.0;
	 	  c.fill = GridBagConstraints.HORIZONTAL;
	 	  w_gridbag.setConstraints(i_wrapper, c);
	 	  wrapper.add(i_wrapper);

		  file_name_panel = i_wrapper;
	      }

	      {
		  status_panel = new JPanel();
		  
		  status_panel.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createEtchedBorder(), 
									   BorderFactory.createEmptyBorder( 3,3,3,3 ) ) );
	 	  GridBagLayout i_gridbag = new GridBagLayout();
	 	  status_panel.setLayout(i_gridbag);
	 	  
	 	  status_label = new JLabel(" Status ");
	 	  c = new GridBagConstraints();
	 	  c.gridx = 0;
	 	  c.anchor = GridBagConstraints.EAST;
	 	  c.fill = GridBagConstraints.BOTH;
		  c.weightx = 10.0;
	 	  i_gridbag.setConstraints(status_label, c);
	 	  status_panel.add(status_label);
	 	  

		  c = new GridBagConstraints();
	 	  c.gridx = 0;
	 	  c.gridy = 0;
	 	  c.weightx = 10.0;
	 	  c.fill = GridBagConstraints.HORIZONTAL;
	 	  w_gridbag.setConstraints(status_panel, c);
	 	  wrapper.add(status_panel);


		  status_panel.setVisible( false );
	      }

  
	      // -----------
  
	      {
	 	  JPanel i_wrapper = new JPanel();
		  javax.swing.border.Border inner = BorderFactory.createCompoundBorder( BorderFactory.createEtchedBorder(), 
											BorderFactory.createEmptyBorder( 3,3,3,3 ) );
	 	  i_wrapper.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createEmptyBorder( 3,0,3,0 ), inner ) );
	 	  GridBagLayout i_gridbag = new GridBagLayout();
	 	  i_wrapper.setLayout(i_gridbag);
  
		  int col = 0;

	 	  JLabel label = new JLabel("Mode ");
	 	  c = new GridBagConstraints();
	 	  c.gridx = col++;
	 	  c.gridy = 0;
	 	  c.anchor = GridBagConstraints.EAST;
	 	  i_gridbag.setConstraints(label, c);
	 	  i_wrapper.add(label);
	 	  
		  load_mode_jcb = new JComboBox( load_mode_opts );
		  c = new GridBagConstraints();
	 	  c.gridx = col++;
	 	  c.gridy = 0;
	 	  c.anchor = GridBagConstraints.WEST;
	 	  i_gridbag.setConstraints(load_mode_jcb, c);
	 	  i_wrapper.add(load_mode_jcb);


	 	  label = new JLabel(" Delimiter ");
	 	  c = new GridBagConstraints();
	 	  c.gridx = col++;
	 	  c.gridy = 0;
	 	  c.anchor = GridBagConstraints.EAST;
	 	  i_gridbag.setConstraints(label, c);
	 	  i_wrapper.add(label);
	 	  

		  delim_jcb = new JComboBox( delim_opts );
		  c = new GridBagConstraints();
	 	  c.gridx = col++;
	 	  c.gridy = 0;
	 	  c.anchor = GridBagConstraints.WEST;
	 	  i_gridbag.setConstraints(delim_jcb, c);
	 	  i_wrapper.add(delim_jcb);

	 	  delim_jcb.addActionListener(new ActionListener() 
		      {
			  public void actionPerformed(ActionEvent e) 
			  {
			      updateOptions();
			      loadAllTokens();
			  }
		      });
		  


		  label = new JLabel(" Encoding ");
	 	  c = new GridBagConstraints();
	 	  c.gridx = col++;
	 	  c.gridy = 0;
	 	  c.anchor = GridBagConstraints.EAST;
	 	  i_gridbag.setConstraints(label, c);
	 	  i_wrapper.add(label);
	 	  
		  encoding_jcb = new JComboBox( encoding_opts );
		  pd.encoding_name = mview.getProperty("LoadPlainText.encoding_name", encoding_opts[ 0 ] );
		  encoding_jcb.setSelectedItem( pd.encoding_name );
	 	  c.gridx = col++;
	 	  c.gridy = 0;
	 	  c.anchor = GridBagConstraints.WEST;
	 	  i_gridbag.setConstraints(encoding_jcb, c);
	 	  i_wrapper.add(encoding_jcb);

	 	  
		  encoding_jcb.addActionListener(new ActionListener() 
		      {
			  public void actionPerformed(ActionEvent e) 
			  {
			      updateOptions();
			      loadAllTokens();
			  }
		      });
		  
		  CustomMouseExitListener cmel = new CustomMouseExitListener();

	 	  label = new JLabel(" Comment prefix ");
	 	  c = new GridBagConstraints();
	 	  c.gridx = col++;
	 	  c.gridy = 0;
	 	  c.anchor = GridBagConstraints.EAST;
	 	  i_gridbag.setConstraints(label, c);
	 	  i_wrapper.add(label);
  
	 	  com_jtf = new JTextField(5);
		  pd.comment_prefix = mview.getProperty("LoadPlainText.comment_prefix", "");
		  if((pd.comment_prefix != null) && (pd.comment_prefix.length() == 0))
		      pd.comment_prefix = null;
		  else
		      com_jtf.setText(pd.comment_prefix);
	 	  c = new GridBagConstraints();
	 	  c.gridx = col++;
	 	  c.gridy = 0;
		  c.weightx = 5.0;
		  c.fill = GridBagConstraints.HORIZONTAL;
	 	  i_gridbag.setConstraints(com_jtf, c);
	 	  i_wrapper.add(com_jtf);
		  com_jtf.addActionListener(new ActionListener() 
		      {
			  public void actionPerformed(ActionEvent e) 
			  {
			      updateOptions();
			      loadAllTokens();
			  }
		      });
		  com_jtf.addMouseListener( cmel );

		  

		  label = new JLabel(" Missing value ");
	 	  c = new GridBagConstraints();
	 	  c.gridx = col++;
	 	  c.gridy = 0;
	 	  i_gridbag.setConstraints(label, c);
	 	  i_wrapper.add(label);
  
		  missing_jtf = new JTextField(7);
		  pd.missing_value = mview.getProperty("LoadPlainText.missing_value", "");
		  missing_jtf.setText( pd.missing_value );
		  missing_jtf.addActionListener(new ActionListener() 
		      {
			  public void actionPerformed(ActionEvent e) 
			  {
			      updateOptions();
			      updateTable();
			  }
		      });
		  missing_jtf.addMouseListener( cmel );

	 	  c = new GridBagConstraints();
	 	  c.gridx = col++;
	 	  c.gridy = 0;
	 	  c.weightx = 5.0;
		  c.fill = GridBagConstraints.HORIZONTAL;
		  i_gridbag.setConstraints(missing_jtf, c);
	 	  i_wrapper.add(missing_jtf);
		
		  // ================

	 	  c = new GridBagConstraints();
	 	  c.gridx = 0;
	 	  c.gridy = 1;
	 	  c.weightx = 10.0;
	 	  c.fill = GridBagConstraints.HORIZONTAL;
	 	  w_gridbag.setConstraints(i_wrapper, c);
	 	  wrapper.add(i_wrapper);
	      }
  
  
	      // -----------
  
	      c = new GridBagConstraints();
	      c.gridx = 0;
	      c.gridy = line++;
	      c.weightx = 10.0;
	      //c.weighty = 1.0;
	      c.fill = GridBagConstraints.HORIZONTAL;
	      gridbag.setConstraints(wrapper, c);
	      panel.add(wrapper);
	    }  
	    
	    table = new JTable();
	    table.setFont( new Font( "Courier", Font.PLAIN, 12 ));
	    JScrollPane jsp = new JScrollPane(table);
	    jsp.setPreferredSize( new Dimension( 550, 300 ));

	    table.addMouseListener( new TableMouseListener() );

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    c.weightx = 10.0;
	    c.weighty = 9.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(jsp, c);
	    panel.add(jsp);
	    
	    // --------------------------------

	    {  
		JButton button = null;
		JPanel wrapper = new JPanel();
		wrapper.setBorder(BorderFactory.createEmptyBorder(3,0,0,0));
		wrapper.setLayout( new BoxLayout( wrapper, BoxLayout.X_AXIS ) );
		
		int col = 0;


		wrapper.add(Box.createHorizontalGlue());

	
		button = new JButton("Import");
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    saveProps();
			    updateOptions();
			    updateTable();
			    importData(null);
			    
			    //if(importData())
			    //  cleanUp();
			}
		    });
		
		wrapper.add(button);
		
		wrapper.add(Box.createRigidArea(new Dimension(30, 0)));

		button = new JButton("AutoParse");
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    autoParse();
			}
		    });
		wrapper.add(button);
		
		wrapper.add(Box.createRigidArea(new Dimension(10, 0)));
	

		button = new JButton("QuickSet");
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    quickSet();
			}
		    });
		wrapper.add(button);

		wrapper.add(Box.createRigidArea(new Dimension(30, 0)));

		button = new JButton("Close");
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    saveProps();
			    cleanUp();
			}
		    });
		wrapper.add(button);
		
		wrapper.add(Box.createRigidArea(new Dimension(10, 0)));
	
		button = new JButton("Help");
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{			    mview.getPluginHelpTopic("LoadPlainText", "LoadPlainText");
			}
		    });
		wrapper.add(button);
		
		
		wrapper.add(Box.createHorizontalGlue());

		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line++;
		c.weightx = 10.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(wrapper, c);
		panel.add(wrapper);
		
	    }
	    
	    getContentPane().add(panel);
	    pack();
	    setVisible(true);
	}

	
	private void status(String msg)
	{
	    status_label.setText( msg == null ? "Ready." : msg );

	    //System.out.println( msg );
	}
	
	private void enableStatusDisplay( boolean enable )
	{
	    status_panel.setVisible( enable );
	    file_name_panel.setVisible( ! enable );
	}

	private BufferedReader getReader(File file, String text_encoding)
	{
	    try 
	    { 
		// check for a .gz extension...
		int ext_pos = file.getPath().lastIndexOf('.');
		boolean compressed = false;
		if(ext_pos > 0)
		{
		    String ext = file.getPath().substring(ext_pos).toLowerCase();
		    if(ext.equals(".gz") || ext.equals(".zip"))
		    {
			compressed = true;
		    }
		}

		InputStreamReader isr = null;
		FileInputStream fis = new FileInputStream(file);
		BufferedInputStream bis = new BufferedInputStream(fis);
		if(compressed)
		{
		    GZIPInputStream gis = new GZIPInputStream(bis);
		    isr = new InputStreamReader(gis, text_encoding);
		}
		else
		{
		    isr = new InputStreamReader(bis, text_encoding);
		}
		return new BufferedReader(isr);
	    }
	    catch(FileNotFoundException fnfe)
	    {
		return null;
	    }
	    catch(IOException ioe)
	    {
		mview.errorMessage("Unable to open this file.\n\n" + ioe );
		return null;
	    }
	}

	private void filenameBrowse()
	{
	    if(jfc == null)
		jfc = new JFileChooser();
	    jfc.setCurrentDirectory(new File(mview.getProperty("LoadPlainText.load_path", System.getProperty("user.dir"))));
	    int returnVal =  jfc.showOpenDialog( frame ); 
	    if(returnVal == JFileChooser.APPROVE_OPTION) 
	    {
		filename_jtf.setText(  jfc.getSelectedFile().getPath() );

		mview.putProperty("LoadPlainText.load_path", jfc.getSelectedFile().getPath());

		filenameHasChanged();
	    }
	}

	private void filenameHasChanged()
	{
	    if(debug)
		System.out.println("filenameHasChanged()...start");

	    //pm.startIt();

	    fd.filename = filename_jtf.getText();
	    fd.file = new File(fd.filename);
	    
	    loadAllTokens();

	    // updateTable();

	    //pm.stopIt();

	    if(debug)
		System.out.println("filenameHasChanged()...done");
	}

	// ==========================================================================
	
	public class CustomTableModel extends javax.swing.table.AbstractTableModel
	{
	    public int getRowCount()    { return (fd == null || fd.lines == 0) ? 0 : fd.lines + 6; }
	    public int getColumnCount() { return (fd == null || fd.lines == 0) ? 0 : fd.cols  + 4;  }

	    // TODO: insert a blank column at position 1
	    // TODO: re-instate the line number column


	    // ie.
	    //
	    //  row:
	    //      0  row# 
	    //      1  blank
	    //      2  meaning   (E)
	    //      3  blank
	    //      4  name      (E)
	    //      5  blank
	    //      6  data0
	    //      7  data1
	    //      .....
	    //
	    //
	    //  col:   0           1         2           3         4      5
	    //       col#   small_blank  meaning(E)  small_blank  data0   data1  ....
	    //                    
	    //
	    //
	    //  (E) == editable cell


	    public Object getValueAt(int row, int col)
	    {
		if((fd == null) || (fd.lines == 0))
		   return null;

		try
		{
		    if((col == 1) || (col==3)) // blank
			return null;

		    if((row == 1) || (row == 3))  // blank
			return null;

		    if((col > 3) && (row > 5))  // the data values
		    {
			Vector rv = (Vector) fd.data.elementAt(row-6);
			if((col-4) < rv.size())
			{
			    // check for tokens which match pd.missing_value
			    
			    String str = (String) rv.elementAt((col-4));
			    if((str != null) && (str.equals(pd.missing_value)))
				str = null;
			    return str;
			}
			else
			{
			    return null;
			}
		    }
		    
		    if(col == 0) // col 0 is the row number
		    {
			if(row < 6)
			    return null;
			else
			    return String.valueOf( row - 5 );
		    }

		    if(row == 0) // row 0 is the column number
		    {
			if(col < 4)
			    return null;
			else
			    return String.valueOf( col - 3 );
		    }

		    if(row == 2) // the 'col_meaning' row
		    {
			if(col < 4)
			    return null;
			else
			    return col_type_names[ pd.col_contents[ col-4 ]];
		    }

		    if(col == 2)  // the 'row_meaning' column
		    {
			if(row < 6)
			    return null;
			else
			    return row_type_names[ pd.row_contents[ row-6 ]];
		    }

		    if(row == 4)  // the 'col_name' row
		    {
			// depending on the col_contents, the col_name may not be relevant
			// e.g. SpotName columns don't need a column name
			
			final int data_col = col-4;

			if(pd.col_contents[data_col] == SpotName)
			    return null;
			if(pd.col_contents[data_col] == ProbeName)
			    return null;
			if(pd.col_contents[data_col] == GeneName)
			    return null;
			if(pd.col_contents[data_col] == Ignore)
			    return null;
			
			return pd.col_names[data_col];
		    }

		    return null;

		}
		catch(Exception exp)
		{
		    return "Exception!"; 
		} 
	    }

	    public String getColumnName(int col) 
	    {
		if((fd == null) || (fd.lines == 0))
		   return null;
		
		try
		{
		    if(col == 0)
		    {
			return null; // "Line";
		    }
		    else
		    {
			return String.valueOf("col" + col);
		    }
		}
		catch(Exception exp)
		{
		    return null;
		} 
	    }

	    public boolean isCellEditable(int row, int col)
	    { 
		return (((row == 2) || (row == 4)) && (col > 3)) || ((col == 2) && (row > 5)); 
	    }

	    public void setValueAt(Object value, int row, int col) 
	    {
		try
		{
		    // System.out.println("setValueAt(): " + value);
		    
		    String vs = (String) value;
		    
		    if(row == 2) // the 'col_meaning' row
		    {
			for(short n=0; n < (short)col_type_names.length; n++)
			    if(col_type_names[n].equals(vs))
				pd.col_contents[col-4] = n;
		    }

		    if(row == 4) // the 'col_name' row
		    {
			pd.col_names[col-4] = vs;
		    }

		    if(col == 2) // the 'row_meaning' column
		    {
			for(short n=0; n < (short)row_type_names.length; n++)
			    if(row_type_names[n].equals(vs))
				pd.row_contents[row-6] = n;
		    }

		    // data[row][col] = value;
		    fireTableCellUpdated(row, col);

		    if(col == 2)
			table.repaint();
		}
		catch(Exception exp)
		{
		    System.err.println("WARNING: illegal cell");
		}
		  
	    }
	}

	private void saveProps()
	{
	    // save the col_contents data in the app properties
	    if(pd.col_contents != null)
		for(int c=0; c < pd.col_contents.length; c++)
		    mview.putIntProperty("LoadPlainText.col_type_" + c, pd.col_contents[c]);

	    //mview.putIntProperty("LoadPlainText.start_line", pd.start_line);
	    //mview.putIntProperty("LoadPlainText.end_line", pd.end_line);
	    //mview.putIntProperty("LoadPlainText.col_names_line", pd.col_names_line);

	    int possible_col_hdr_line = findColumnHeaderline();
	    mview.putIntProperty("LoadPlainText.col_names_line", possible_col_hdr_line);

	    mview.putProperty("LoadPlainText.comment_prefix", pd.comment_prefix);

	    mview.putProperty("LoadPlainText.encoding_name", pd.encoding_name );

	    mview.putProperty("LoadPlainText.missing_value", pd.missing_value);
	}
	
	private void updateOptions()
	{
	    pd.comment_prefix = com_jtf.getText();
	    if(pd.comment_prefix.length() == 0)
		pd.comment_prefix = null;
	    
	    pd.missing_value = missing_jtf.getText();
	    
	    pd.encoding_name = (String) encoding_jcb.getSelectedItem();
	}
	
	//
	// install a new TableModel 
	//
	private void updateTable()
	{
	    updateOptions();


	    if(debug)
		System.out.println("updateTable()...start");

	    try
	    {
		if( fd.lines == 0)
		    table.setModel( new DefaultTableModel() ); 
		else
		{
		    table.setModel( new CustomTableModel() ); 

		    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		    
		    table.setDefaultEditor(Object.class, new CustomCellEditor());
		    
		    table.setDefaultRenderer(Object.class, new ColorRenderer());
		    
		    table.getTableHeader().setDefaultRenderer( new CustomHeaderRenderer() );
		    
		    setColumnWidths();
		}
	    }
	    catch(Throwable th)
	    {
		System.out.println("hmm, unexpected " + th.toString());
	    }

	    
	    if(debug)
		System.out.println("updateTable()...done");
	}

	private void setColumnWidths()
	{
	    TableColumn column = null;
	   
	    // line number
	    column = table.getColumnModel().getColumn(0);
	    column.setWidth(50);
	    column.setPreferredWidth(50);

	    // blank
	    column = table.getColumnModel().getColumn(1);
	    column.setWidth(10);
	    column.setPreferredWidth(10);

	    // line meaning
	    column = table.getColumnModel().getColumn(2);
	    column.setWidth(120);
	    column.setPreferredWidth(120);

	    // blank
	    column = table.getColumnModel().getColumn(3);
	    column.setWidth(10);
	    column.setPreferredWidth(10);


	    for(int c=0; c <= fd.cols; c++)
	    {
		try
		{
		    column = table.getColumnModel().getColumn(c+4);
		    column.setWidth(120);
		    column.setPreferredWidth(120);
		}
		catch(ArrayIndexOutOfBoundsException aioobe)
		{
		}
		catch(NullPointerException npe)
		{
		}
	    }

	    table.revalidate();

	}

	// ==============================

	class CustomHeaderRenderer extends JLabel implements TableCellRenderer 
	{
	    public Component getTableCellRendererComponent(JTable table, Object object, 
							   boolean isSelected, boolean hasFocus,
							   int row, int column) 
	    {
		setBackground(Color.white);
		setForeground(Color.red);
		return this;
	    }


	}


	class ColorRenderer extends JLabel implements TableCellRenderer 
	{
	    private boolean strikeout = false;

	    public ColorRenderer() 
	    {
		super();
		setOpaque(true); //MUST do this for background to show up.
	    }
	    
	    public void paintComponent( Graphics g )
	    {
		super.paintComponent( g );
		
		if( strikeout )
		{
		    int mx = getWidth() / 2;
		    int my = getHeight() / 2;
		    g.drawLine(mx-my, 0, mx+my, getHeight() );
		    g.drawLine(mx-my, getHeight(), mx+my, 0 );
		}

	    }

	    public Component getTableCellRendererComponent(JTable table, Object object, 
							   boolean isSelected, boolean hasFocus,
							   int row, int column) 
	    {
		try
		{
		    // System.out.println("ColorRenderer for " + row + "," + column + "...");
		    String token = (String) object;
		    
		    setBackground(Color.white);
		    strikeout = false;

		    Color fg = Color.black;
		    Color bg = Color.white;

		    String ttt = null;

		    if( ( row > 5 ) && ( column > 3 ) )
		    {
			try
			{
			    setToolTipText("Row is '" + row_type_names[ pd.row_contents[row - 6] ] + 
					   "', Column is '" + col_type_names[ pd.col_contents[column - 4] ] + "'");
			}
			catch( NullPointerException npe)
			{
			}
		    }
			
		    
		    if(row > 5)
		    {
			switch(pd.row_contents[row - 6])
			{
			case ColumnHeader:
			    fg = Color.green;
			    break;
			case MeasurementAttr:
			    fg = Color.magenta;
			    break;
			case Ignore:
			    if(column > 3)
			    {
				fg = Color.darkGray;
				bg = Color.lightGray;
			    }
			    break;
			case Data:
			    fg = Color.black;
			    break;
			}
			
			
		    }
		    

		    if(column > 3)
		    {
			switch(pd.col_contents[ column - 4 ])
			{
			    case Ignore:
			    {
				fg = Color.darkGray;
				bg = Color.lightGray;
			    }
			    break;
			}
		    }


		    // row/col numbers
		    //
		    if((column == 0) || (row == 0))
		    {
			setHorizontalAlignment(SwingConstants.CENTER);
			fg = Color.red;
		    }
		    else
		    {
			setHorizontalAlignment(SwingConstants.LEFT);
		    }

		    // row/col 'meanings'
		    if( ((column == 2) && (row > 5)) || ((row == 2) && (column > 3)) )
		    {
			fg = Color.white;
			bg = Color.blue;
		    }

		    // row 3 is the col_name editor 
		    //
		    if(row == 4)
		    {
			if(column > 3)
			{
			    fg = Color.gray;
			    bg = Color.pink;
			}
			else
			{
			    fg = Color.blue;
			}
		    }
		    
		    
		    // flag missing data values
		    //
		    if((column > 3) && (row > 5) && (token == null))
		    {
			fg = Color.darkGray;
			// setBackground(Color.white);
			setText(null);
			strikeout = true;
		    }
		    else
		    {
			setText(token);
		    }

		    if((column == 1) || (column == 3) || (row == 1) || (row == 3))  // blank
		    {
			bg = Color.white;
		    }
		    
		    setBackground( bg );
		    setForeground( fg );

		}
		catch(Exception ex)
		{
		    ex.printStackTrace();
		}

		return this;
	    }
	}
	
	// ==========================================================================

	class CustomCellEditor extends DefaultCellEditor
	{
	    public CustomCellEditor() 
	    {
                super(new JCheckBox());
		
		col_jcb = new JComboBox();
		row_jcb = new JComboBox();

		col_jtf = new JTextField(10);
		col_jtf.setBackground(Color.pink);
		col_jtf.setForeground(Color.gray);

		edit_mode = 0;

		//Must do this so that editing stops when appropriate.
		col_jcb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    fireEditingStopped();
			}
		    });
		
		row_jcb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    fireEditingStopped();
			}
		    });
		

		col_jtf.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    fireEditingStopped();
			}
		    });

		if(col_type_names != null)
		    for(int n=0; n < col_type_names.length; n++)
			if(col_type_names[n] != null)
			    col_jcb.addItem( col_type_names[n] );
		
		if(row_type_names != null)
		    for(int n=0; n < row_type_names.length; n++)
			if(row_type_names[n] != null)
			    row_jcb.addItem( row_type_names[n] );
		
			
		
		// setPreferredSize(new Dimension(100, 40));
	    }

	    public Component getTableCellEditorComponent(JTable table,
							 Object value,
							 boolean isSelected,
							 int row,
							 int column)
	    {
		// System.out.println("editor at " + row + "," + column);
		edit_col = column - 4;
		
		if(row == 4) // the 'col_name' row
		{
		    edit_mode = 1;
		    if(pd.col_names[edit_col] != null)
			col_jtf.setText( pd.col_names[edit_col] );
		    else
			col_jtf.setText( "" );
		    
		    return col_jtf;
		}
		else
		{
		    if(column == 2)  // the 'row_meaning' col
		    {
			edit_mode = 2;

			// row_jcb.setSelectedIndex( pd.row_contents[ row - 6 ] );
			
			return row_jcb;
		    }
		    else
		    {
			if(row == 2)   // the 'col_meaning' row
			{
			    edit_mode = 3;
			    
			    // col_jcb.setSelectedIndex( pd.col_contents[ column - 4 ] );

			    return col_jcb;
			}
		    }
		}

		return null;
	    }
	    
	    protected void fireEditingStopped() 
	    {
		// System.out.println("fireEditingStopped(): value is " + (text_mode ? jtf.getText() : (String)jcb.getSelectedItem()));
		
		if(edit_mode == 1)
		{
		    if((pd != null) && (pd.col_names != null) && (pd.col_names.length >= edit_col))
			pd.col_names[edit_col] = col_jtf.getText();
		}
		super.fireEditingStopped();
		edit_mode = 0;
	    }
	    
	    public Object getCellEditorValue() 
	    {
		switch(edit_mode) 
		{
		case 1:
		    return col_jtf.getText();
		
		case 2:
		    return row_jcb.getSelectedItem();

		case 3:
		    return col_jcb.getSelectedItem();

		default:
		    return null;
		}
	    }
	    
	    public boolean stopCellEditing()
	    {
		// System.out.println("stopCellEditing(): value is " + (text_mode ? jtf.getText() : (String)jcb.getSelectedItem()));

		if(edit_mode == 1)
		{
		    pd.col_names[edit_col] = col_jtf.getText();
		}
		return super.stopCellEditing();
	    }

	    private int edit_col;
	    private int edit_row;
	    private int edit_mode;
	    private JComboBox col_jcb;
	    private JComboBox row_jcb;
	    private JTextField col_jtf;
	}
	
	// ==========================================================================

	public class CustomMouseExitListener implements MouseListener
	{
	    public void mousePressed(MouseEvent e) {}
	    public void mouseReleased(MouseEvent e) {}
	    public void mouseClicked(MouseEvent e) {}
	    public void mouseEntered(MouseEvent e) {}
	    
	    public void mouseExited(MouseEvent e) 
	    {
		updateOptions();
		updateTable();
	    }
	}
    
	// ==========================================================================
	

	private void loadAllTokens()
	{
	    System.gc();

	    new TokenLoaderThread().start();
	}


	private class TokenLoaderThread extends Thread
	{
	    public void run()
	    {
		loadAllTokensActual();
	    }

	}


	private void loadAllTokensActual()
	{
	    if(fd == null)
		return;
	    if(fd.file == null)
		return;
	    if(!fd.file.exists())
		return;

	    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	    

	    // save the old settings
	    // (but only do this if there are some settings...)
	    if( ( fd.lines > 0 ) && ( fd.cols > 0 ) )
		quickSetSave( new File( previous_settings_file ), false );


	    status("Loading file...");

	    if(debug)
		System.out.println("loadAllTokens()...start");
		
	    // saveProps();
	    
	    table.setModel(new DefaultTableModel());
	    table.revalidate();

	    enableStatusDisplay( true );
	    
	    char delim_ch = '\t';
	    if(delim_jcb.getSelectedIndex() == 1)
		delim_ch = ' ';
	    if(delim_jcb.getSelectedIndex() == 2)
		delim_ch = ',';
	    

	    try
	    { 
		BufferedReader br = getReader(fd.file, (String) encoding_jcb.getSelectedItem() );
	    
		if(br == null)
		    return;

		// count number of lines in the file
		String line = br.readLine();
		int line_count = 0;
		while(line != null)
		{
		    line_count++;
		    line = br.readLine();
		}
		
		System.out.println( "there are " + line_count + " lines..." );


		br = getReader(fd.file, (String) encoding_jcb.getSelectedItem() );
		
		if(br == null)
		    return;
	    
		fd.lines = 0;
		fd.cols = 0;
		fd.data = new Vector();
		
		line = br.readLine();
		int lines_processed = 0;

		while(line != null)
		{
		    Vector tokens = new Vector();
		    
		    // detect delimiters and tokenise
		    
		    final int nc = line.length();
		    
		    int last = 0;
		    
		    for(int c=0; c < nc; c++)
			if(line.charAt(c) == delim_ch)
			{
			    String str = line.substring(last, c);
			    
			    last = c+1;
			    
			    tokens.addElement(str.trim());
			}
		    
		    // add the final token...
		    String last_t = line.substring(last).trim();
		    if(last_t.length() > 0)
			tokens.addElement(last_t);
		    
		    // add these tokens to the data and possibly update counts of cols
		    fd.data.addElement(tokens);
		    fd.lines++;
		    if(tokens.size() > fd.cols)
			fd.cols = tokens.size();
		    
		    //if((fd.lines % 100) == 0)
		    //	status(fd.lines + " lines read");

		    if( ( fd.lines % 100 ) == 0 )
		    {
			int percent_done = (int)( ( (double) fd.lines / (double) line_count ) * 100.0 );
			status( "Loading: " + percent_done + "%");
		    }

		    

		    // next line, if any
		    line = br.readLine();
		} 
		
		pd.col_contents = new short[fd.cols];
		pd.col_names    = new String[fd.cols];
		pd.row_contents = new short[fd.lines];

		for(int r=0; r < pd.row_contents.length; r++)
		    pd.row_contents[ r ] = Ignore;
		for(int c=0; c < pd.col_contents.length; c++)
		    pd.col_contents[ c ] = Ignore;

		
		System.out.println( "data loaded: " + fd.cols + " cols, " + fd.lines + " lines.");

		
		// restore the most recent row/col settings
		//
		quickSetLoad( new File( previous_settings_file ), false );

		// make an inital guess on the row and column contents...

		/*
		for(int r=0; r < pd.row_contents.length; r++)
		{
		    final Vector vec = (Vector)fd.data.elementAt(r);

		    if((vec != null) && (vec.size() > 0))
		    {
			final String col0 = (String)vec.elementAt(0);

			if((col0 != null) && (pd.comment_prefix != null) && (col0.startsWith(pd.comment_prefix)))
			{
			    pd.row_contents[r] = Ignore;
			}
			else
			{
			    if(vec.size() == fd.cols)
			    {
				pd.row_contents[r] = Data;
			    }
			    else
			    {
				pd.row_contents[r] = Ignore;   // line appears to be too short
			    }
			}
		    }
		    else
		    {
			pd.row_contents[r] = Ignore;
		    }

		}

		// retrieve col_contents data from the app properties
		for(int c=0; c < pd.col_contents.length; c++)
		{
		    int t = mview.getIntProperty("LoadPlainText.col_type_" + c, -1);
		    if(t >= 0)
			pd.col_contents[c] = (short)t; 
		}

		// and retrieve any stored information about row meanings from the app properties
		int cnl = mview.getIntProperty("LoadPlainText.col_names_line", -1);
		if((cnl >= 0) && (cnl < fd.lines))
		    pd.row_contents[cnl] = ColumnHeader;
		*/

		br = null;
		System.gc();
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	    }
	    catch(FileNotFoundException fnfe)
	    {
		enableStatusDisplay( false );
		//pm.stopIt();
		status(null);
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		mview.errorMessage("File not found");
		return;
	    }
	    catch(IOException ioe)
	    {
		enableStatusDisplay( false );
		status(null);
		//pm.stopIt();
		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		mview.errorMessage("File cannot be read\n" + ioe);
		return;
	    }
	    
	    // status( fd.lines + " lines, " + fd.cols + " columns" );

	    enableStatusDisplay( false );

	    if(debug)
		System.out.println("loadAllTokens()...done: " + fd.lines + " lines, " + fd.cols + " cols");
	    
	    updateTable();
	}

	// = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 

	private String[] getColumn( final int col_id, final int n_data_lines )
	{
	    try
	    {
		String[] data = new String[ n_data_lines ];
		int i = 0;
		
		for(int l=0; l < fd.lines; l++)
		{
		    if(pd.row_contents[l] == Data)
		    {
			Vector v = (Vector)fd.data.elementAt(l);
			String dstr = (col_id < v.size()) ? (String) v.elementAt(col_id) : null; 
			if((dstr != null) && (dstr.equals(pd.missing_value)))
			    dstr = null;
			data[i++] = dstr;
		    }
		}
		return data;
	    }
	    catch(Exception ex)
	    {
		return null;
	    }
	}

	private boolean hasBlanks( final String[] data )
	{
	    for(int d=0; d < data.length; d++)
	    {
		if(data[d] == null)
		    return true;
		if(data[d].length() == 0)
		    return true;
	    }
	    return false;
	}
	private boolean isUnique( final String[] data )
	{
	    java.util.HashSet ht = new java.util.HashSet();
	    for(int d=0; d < data.length; d++)
	    {
		if( ht.contains(data[d]) )
		    return false;
		ht.add( data[d] );
	    }
	    return true;
	}
	private String findSomeDuplicates( final String[] data, final int max_dups )
	{
	    String result = "";
	    int dups = 0;
	    java.util.HashSet ht = new java.util.HashSet();
	    for(int d=0; d < data.length; d++)
	    {
		if( ht.contains(data[d]) )
		{
		    if( dups < max_dups )
		    {
			if( result.length() > 0 )
			    result += ", ";
			
			result += data[ d ];
		    }

		    if( dups == max_dups )
		    {
			result += "....";
		    }

		    dups++;

		    if( dups > max_dups )
			return result;
		}
		ht.add( data[d] );
	    }
	    return result;
	}

	private boolean isBlanks( final String[] data )
	{
	    for(int d=0; d < data.length; d++)
	    {
		if(data[d] != null)
		{
		    if(data[d].length() > 0)
			return false;
		}
	    }
	    return true;
	}
	private int[] getNumberRange( final String[] data )
	{
	    int[] range = new int[2];

	    range[0] = range[1] = -1;

	    for(int d=0; d < data.length; d++)
	    {
		if(pd.row_contents[d] == Data)
		{
		    if(data[d] != null)
		    {
			if(range[0] < 0)
			    range[0] = d;
			
			range[1] = d;
		    }
		}
	    }
	    return range;
	}
	
	private double[] getDataAsDoubles( final String[] data )
	{
	    double[] data_d = new double[data.length];
	    try
	    {
		for(int i=0; i < data.length; i++)
		{
		    if( (data[i] == null) || (data[i].length() == 0) )
			data_d[i] = Double.NaN;
		    else
			data_d[i] = NumberParser.tokenToDouble(data[i]);
		}
		return data_d;
	    }
	    catch(TokenIsNotNumber tinn)
	    {
		// System.out.println("'" + tinn.str + "' is not a Double");
		return null;
	    }
	}

	private String getNonDoubleEntries( final String[] data, final int count )
	{
	    String res = "";
	    int c = 0;
	    for(int i=0; i < data.length; i++)
	    {
		try
		{
		    NumberParser.tokenToDouble(data[i]);
		}
		catch(TokenIsNotNumber tinn)
		{
		    // System.out.println("'" + tinn.str + "' is not a Double");
		    res += "'" + data[i] + "' (line " + (i+1) + ") ";
		    if(++c == count)
			return res;
		}
		catch(NullPointerException npe) // missing value
		{
		    res += "[missing] (line " + (i+1) + ") ";
		    if(++c == count)
			return res; 
		}
	    }
	    return res;
	}

	private int[] getDataAsInts( final String[] data )
	{
	    int[] data_i = new int[data.length];
	    try
	    {
		for(int i=0; i < data.length; i++)
		{
		    if((data[i] == null) || (data[i].length() == 0))
			data_i[i] = 0;
		    else
			data_i[i] = (new Integer(data[i])).intValue();
		}
		return data_i;
	    }
	    catch(NumberFormatException nfe)
	    {
		return null;
	    }
	}

	private char[] getDataAsChars( final String[] data )
	{
	    char[] data_c = new char[data.length];

	    for(int i=0; i < data.length; i++)
	    {
		final String dline = data[i];
		if(dline == null)
		{
		    data_c[i] = ' ';
		}
		else
		{
		    if(dline.length() == 1)
			data_c[i] = dline.charAt(0);
		    else
		    {
			if(dline.length() == 0)
			{
			    data_c[i] = ' ';
			}
			else
			{
			    // not a char
			    return null;
			}
		    }
		    
		}
	    }
	    return data_c;
	}
	
	private int getDataType( final String[] data_in )
	{
	    Object data = getDataAsInts( data_in );
	    if(data != null)
	    {
		return ExprData.Measurement.SpotAttributeIntDataType;
	    }
	    else
	    {
		data = getDataAsDoubles( data_in );
		if(data != null)
		{
		    return ExprData.Measurement.SpotAttributeDoubleDataType;
		}
		else
		{
		    data = getDataAsChars( data_in );
		    if(data != null)
		    {
			return ExprData.Measurement.SpotAttributeCharDataType;
		    }
		    else
		    {
			return ExprData.Measurement.SpotAttributeTextDataType;
		    }
		}
	    }
	}
	
	// ==========================================================================
	
	public boolean checkParseDetails()
	{
	    status("Checking...");

	    if(debug)
		System.out.println("checkParseDetails()...start");

	    Vector errors = new Vector();
	    Vector info = new Vector();

	    int[] counts = new int[11];  

	    pd.make_spot_names  = false;
	    pd.make_probe_names = false;

	    if(pd.col_contents == null)
	    {
		errors.addElement("No data has been loaded, use \"Browse\" to pick a file.");
	    }
	    else
	    {
		// how many of each type of column are there?
		for(int n=0; n < pd.col_contents.length; n++)
		{
		    counts[pd.col_contents[n]]++;
		}
		
		// a set of rules...

		if(counts[SpotName] == 0)
		{
		    info.addElement("Spot Names will be automatically generated.");
		    pd.make_spot_names = true;
		}

		if((counts[ProbeNameAttr] > 0) && (counts[ProbeName] == 0))
		{
		    info.addElement("Probe Names will be automatically generated.");
		    pd.make_probe_names = true;
		}

		if((counts[GeneName] > 0) && (counts[ProbeName] == 0))
		{
		    if(!pd.make_probe_names)
		    {
			info.addElement("Probe Names will be automatically generated.");
			pd.make_probe_names = true;
		    }
		}

		if(counts[SpotName] > 1)
		{
		    errors.addElement("Only one column may be interpreted as SpotName.");
		}
		if(counts[ProbeName] > 1)
		{
		    errors.addElement("Only one column may be interpreted as ProbeName.");
		}

		if(counts[MeasurementAttrName] > 1)
		{
		    errors.addElement("Only one column may be interpreted as MeasurementAttrName.");
		}

		/*
		if((counts[ProbeNameAttr] > 0) && (counts[ProbeName] == 0))
		{
		    errors.addElement("There can only be ProbeNameAttr columns when there is a ProbeName column.");
		}
		*/

		if((counts[GeneNameAttr] > 0) && (counts[GeneName] == 0))
		{
		    errors.addElement("There can only be GeneNameAttr columns when there is a GeneName column.");
		}
		/*
		if((counts[GeneNameAttr] > 1) && (counts[GeneName] == 0))
		{
		    errors.addElement("There can only be ProbeNameAttr columns when there is a ProbeName column.");
		}
		*/

		/* // probe names can be auto
		if((counts[GeneName] > 0) && (counts[ProbeName] == 0))
		{
		    errors.addElement("There can only be GeneName columns when there is a ProbeName column.");
		}
		*/


		if((counts[SpotAttrPrev] + counts[SpotAttrNext]) > 1)
		{
		    if(counts[Spot] == 0)
			errors.addElement("There are DataAttr column(s) but no Data column(s)");
		}
		else
		{
		    if(counts[Spot] == 0)
			errors.addElement("There are no Data columns");
		}
		
		for(int n=0; n < pd.col_contents.length; n++)
		{
		    if(pd.col_contents[n] == SpotAttrPrev)
		    {
			int m_col = findNearestPrevSpotColumn(n);
			if(m_col == -1)
			    errors.addElement("The DataAttr in column " + (n+1) + " cannot be matched to a Data column");
		    }
		    if(pd.col_contents[n] == SpotAttrNext)
		    {
			int m_col = findNearestNextSpotColumn(n);
			if(m_col == -1)
			    errors.addElement("The DataAttr in column " + (n+1) + " cannot be matched to a Data column");
		    }
		}
	    }
	    if(errors.size() > 0)
	    {
		//pm.stopIt();
		
		String report = (errors.size() == 1) ? "Error\n" : "Errors\n";
		for(int e=0; e < errors.size(); e++)
		    report += ("  " + (String)errors.elementAt(e) + "\n");
		mview.alertMessage(report);
		
		if(debug)
		    System.out.println("checkParseDetails()...done (there were reported errors)");

		status("Bad parse...");

		return false;
	    }
	    else
	    {
		if(info.size() > 0)
		{
		  String report = "Information\n";
		  for(int e=0; e < info.size(); e++)
		      report += ("  " + (String)info.elementAt(e) + "\n");

		  if(!silent_running)
		      mview.infoMessage(report);
		  
		  //System.out.println("checkParseDetails()...done (there was information)");

		}
		else
		{
		    //System.out.println("checkParseDetails()...done");
		}

		status("Parsed ok.");
		return true;
	    }
	}

	public String getColumnNameFromData( int col_id )
	{
	    StringBuffer buf = new StringBuffer();

	    for(int l=0;  l < fd.lines; l++)
	    {
		if( pd.row_contents[l] == ColumnHeader )
		{
		    Vector line = (Vector)fd.data.elementAt( l );
		    if(col_id < line.size())
			buf.append( (String) line.elementAt(col_id) );
		}
	    }
	    
	    return buf.length() == 0 ? null : buf.toString();
	}

	// ==========================================================================
	
	public boolean autoParse()
	{
	    new AutoParseThread().start();
	    return true;
	}    
	 
	public class AutoParseThread extends Thread
	{
	    public void run()
	    {
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	    
		boolean spot_name_allocated  = false;
		boolean probe_name_allocated = false;
		boolean gene_name_allocated = false;
		boolean data_seen = false;
		
		boolean is_blanks, has_blanks, is_unique;

		int data_count = 0;
		
		int min_line = -1;
		int max_line = -1;

		final int n_data_lines = countDataLines();

		for(int c=0; c < fd.cols; c++)
		{
		    status("Checking column " + (c+1) + "...");
		    
		    String cname = getColumnNameFromData(c);
		    pd.col_names[c] = cname;

		    String[] col_data = getColumn( c, n_data_lines );
		    
		    pd.col_contents[c] = Ignore;
			
		    if(col_data != null)
		    {
			int[] data_range = getNumberRange( col_data );
			
			if(c == 0)
			{
			    min_line = data_range[0];
			    max_line = data_range[1];
			}
			else
			{
			    if(data_range[0] > min_line)
				min_line = data_range[0];
			    if(data_range[1] < max_line)
				max_line = data_range[1];
			}
			
			System.out.println("column " + c + " has " + col_data.length + " els, " + data_range[0] + ":" + data_range[1] + " = " + min_line + " ... " + max_line);

			int dtype = getDataType( col_data );
			
			int attr_count = 0;
			
			is_blanks = (col_data == null) ? true : isBlanks( col_data );
			
			if(is_blanks)
			{
			    has_blanks = true;
			    is_unique = false;
			}
			else
			{
			    has_blanks = hasBlanks( col_data );

			    if(!has_blanks)
				is_unique = isUnique( col_data );
			    else
				is_unique = false;
			}
			
			
			switch(dtype)
			{
			case ExprData.Measurement.SpotAttributeTextDataType:

			    int guess = Ignore;

			    if(pd.col_names[c] != null)
			    {
				// make a guess based on the name
				
				
				String ccname = pd.col_names[c].toLowerCase().trim();
				
				if(ccname.startsWith("s") || (ccname.indexOf("sp") >= 0))
				    guess = SpotName;
				if(ccname.startsWith("pr") || (ccname.indexOf("probe") >= 0))
				    guess = ProbeName;
				if(ccname.startsWith("ge") || (ccname.indexOf("gene") >= 0))
				    guess = GeneName;
			    }

			    if(guess == GeneName)
			    {
				if(!is_blanks)
				{
				    if(!gene_name_allocated)
				    {
					gene_name_allocated = true;
					pd.col_contents[c] = GeneName;
				    }
				}
				break;
			    }
			    
			    if(guess == ProbeName)
			    {
				if(!is_blanks)
				{
				    if(!probe_name_allocated)
				    {
					probe_name_allocated = true;
					pd.col_contents[c] = ProbeName;
				    }
				    else
				    {
					pd.col_contents[c] = ProbeNameAttr;
				    }
				}
				break;
			    }

			    if(guess == SpotName)
			    {
				if(!has_blanks && is_unique)
				{
				    spot_name_allocated = true;
				    pd.col_contents[c] = SpotName;
				}
			    }
			    
			    if(guess == Ignore)
			    {
				if(!is_blanks)
				{
				    if(is_unique)
				    {
					if(!spot_name_allocated)
					{
					    spot_name_allocated = true;
					    pd.col_contents[c] = SpotName;
					    break;
					}
				    }
				    
				    if(!gene_name_allocated)
				    {
					gene_name_allocated = true;
					pd.col_contents[c] = GeneName;
				    }
				    else
				    {
					if(!data_seen)
					{
					    if(probe_name_allocated)
						pd.col_contents[c] = GeneNameAttr;
					    else
					    {
						probe_name_allocated = true;
						pd.col_contents[c] = ProbeName;
					    }
					}
					else
					{
					    pd.col_contents[c] = SpotAttrPrev;
					}
				    }
				}
			    }
			    break;
			    
			case ExprData.Measurement.SpotAttributeDoubleDataType:
			    if(!is_blanks)
			    {
				pd.col_contents[c] = Spot;
				
				if(pd.col_names[c] == null)
				{
				    pd.col_names[c] = ("Data" + (++data_count));
				}
				
				data_seen = true;
			    }
			    break;
			    
			case ExprData.Measurement.SpotAttributeCharDataType:
			    if(!data_seen && !is_blanks)
			    {
				if(probe_name_allocated)
				    pd.col_contents[c] = ProbeNameAttr;
				else
				    pd.col_contents[c] = SpotNameAttr;
			    }
			    else
			    {
				pd.col_contents[c] = SpotAttrPrev;
				if(pd.col_names[c] == null)
				    pd.col_names[c] = ("Attr" + data_count + "." + (++attr_count));
			    }
			    
			    break;
			    
			case ExprData.Measurement.SpotAttributeIntDataType:
			    if(!is_blanks)
			    {
				if(!data_seen)
				{
				    if(!spot_name_allocated)
				    {
					if(!has_blanks && is_unique)
					{
					    spot_name_allocated = true;
					pd.col_contents[c] = SpotName;
					}
				    }
				    else
				    {
					if(probe_name_allocated)
					    pd.col_contents[c] = ProbeNameAttr;
					else
					    pd.col_contents[c] = SpotNameAttr;
				    }
				}
				else
				{
				    pd.col_contents[c] = SpotAttrPrev;
				    if(pd.col_names[c] == null)
					pd.col_names[c] = ("Attr" + data_count + "." + (++attr_count));
				}
			    }
			    break;
			    }
			}
		}
		
		/*
		if(min_line >= 0)
		    pd.start_line = min_line;

		if(max_line >= 0)
		    pd.end_line = max_line;
		*/

		System.gc();
		status(null);

		
		if( fd.lines == 0)
		    table.setModel( new DefaultTableModel() ); 
		else
		{
		    table.setModel( new CustomTableModel() ); 
		    
		    setColumnWidths();
		}

		setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	    }
	}

	// == quick set =========================================================================
	
	public void quickSet()
	{
	    if( quick_set_frame == null )
	    {
		quick_set_frame = new JFrame("Quick Set");
		mview.decorateFrame( quick_set_frame );
		
		JPanel outer_panel = new JPanel();
		GridBagLayout outer_gridbag = new GridBagLayout();
		outer_panel.setLayout( new BoxLayout( outer_panel,BoxLayout.Y_AXIS  ) );
		outer_panel.setBorder( BorderFactory.createEmptyBorder(4,4,4,4) );
		
		JPanel panel = new JPanel();
		panel.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder( 5,5,5,5 ) ) );
		GridBagLayout gridbag = new GridBagLayout();
		panel.setLayout(gridbag);
		
		int line = 0;
		
		JLabel label;
		GridBagConstraints con;
		
		Dimension fillsize      = new Dimension(10,10);
		Dimension smallfillsize = new Dimension(5,5);
		Box.Filler filler;
		
		// -- load/save settings --
		
		label = new JLabel( "The current row and column settings can be", SwingConstants.LEFT );
		con = new GridBagConstraints();
		con.gridy = line++;
		con.gridwidth = 3;
		gridbag.setConstraints(label, con);
		panel.add(label);
		
		label = new JLabel( "saved and reused when importing data from", SwingConstants.LEFT );
		con = new GridBagConstraints();
		con.gridy = line++;
		con.gridwidth = 3;
		gridbag.setConstraints(label, con);
		panel.add(label);
		
		label = new JLabel( "another file which has the same format.", SwingConstants.LEFT );
		con = new GridBagConstraints();
		con.gridy = line++;
		con.gridwidth = 3;
		gridbag.setConstraints(label, con);
		panel.add(label);
		
		
		filler = new Box.Filler(smallfillsize, smallfillsize, smallfillsize);
		con = new GridBagConstraints();
		con.gridy = line++;
		gridbag.setConstraints(filler, con);
		panel.add(filler);
		
		
		JButton save_jb = new JButton(" Save settings ");
		save_jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    quickSetSave();
		    }
		    });
		con = new GridBagConstraints();
		con.gridy = line;
		con.weightx = 1.0;
		con.anchor = GridBagConstraints.EAST;
		gridbag.setConstraints(save_jb, con);
		panel.add(save_jb);
		
		filler = new Box.Filler(smallfillsize, smallfillsize, smallfillsize);
		con = new GridBagConstraints();
		con.gridx = 1;
		con.gridy = line;
		gridbag.setConstraints(filler, con);
		panel.add(filler);
		
		JButton load_jb = new JButton(" Load settings ");
		load_jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    quickSetLoad();
			    updateTable();
			}
		    });
		con = new GridBagConstraints();
		con.gridx = 2;
		con.gridy = line;
		con.weightx = 1.0;
		con.anchor = GridBagConstraints.EAST;
		gridbag.setConstraints(load_jb, con);
		panel.add(load_jb);
		
		
		outer_panel.add( panel );
		outer_panel.add( Box.createRigidArea( new Dimension( 0,5 ) ) );
		

		// -- the columns --
		
		panel = new JPanel();
		panel.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder( 5,5,5,5 ) ) );
		gridbag = new GridBagLayout();
		panel.setLayout(gridbag);
		line = 0;
		
		if(fd.cols == 1)
		    label = new JLabel("There is but 1 column )");
		else
		    label = new JLabel("There are " + fd.cols + " columns");
		label.setBorder(BorderFactory.createEmptyBorder(0,0,3,0));
		con = new GridBagConstraints();
		con.gridx = 0;
		con.gridy = line++;
		con.gridwidth = 4;
		gridbag.setConstraints(label, con);
		panel.add(label);
		
		label = new JLabel("from column ");
		con = new GridBagConstraints();
		con.gridx = 0;
		con.gridy = line;
		con.anchor = GridBagConstraints.EAST;
		gridbag.setConstraints(label, con);
		panel.add(label);
		
		final JTextField col_from_jtf = new JTextField(4);
		con = new GridBagConstraints();
		con.gridx = 1;
		con.gridy = line++;
		con.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(col_from_jtf, con);
		panel.add(col_from_jtf);
	    
	    
		label = new JLabel("to column ");
		con = new GridBagConstraints();
		con.gridx = 0;
		con.gridy = line;
		con.anchor = GridBagConstraints.EAST;
		gridbag.setConstraints(label, con);
		panel.add(label);
		
		final JTextField col_to_jtf = new JTextField(4);
		con = new GridBagConstraints();
		con.gridx = 1;
		con.gridy = line++;
		con.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(col_to_jtf, con);
		panel.add(col_to_jtf);
		
		filler = new Box.Filler(smallfillsize, smallfillsize, smallfillsize);
		con = new GridBagConstraints();
		con.gridx = 1;
		con.gridy = line;
		gridbag.setConstraints(filler, con);
		panel.add(filler);
		
		label = new JLabel("mode ");
		con = new GridBagConstraints();
		con.gridx = 0;
		con.gridy = line;
		con.anchor = GridBagConstraints.EAST;
		gridbag.setConstraints(label, con);
		panel.add(label);
		
		final JComboBox col_jcb = new JComboBox(col_type_names);
		con = new GridBagConstraints();
		con.gridx = 1;
		con.gridy = line;
		con.weighty = 1.0;
		con.weightx = 10.0;
		con.fill = GridBagConstraints.BOTH;
		gridbag.setConstraints(col_jcb, con);
		panel.add(col_jcb);
		
		/*
		filler = new Box.Filler(smallfillsize, smallfillsize, smallfillsize);
		con = new GridBagConstraints();
		con.gridx = 1;
		con.gridy = line++;
		gridbag.setConstraints(filler, con);
		panel.add(filler);
		*/

		JButton set_cols_jb = new JButton("Set");
		con = new GridBagConstraints();
		con.gridx = 3;
		con.gridy = line++;
		con.weighty = 1.0;
		con.weightx = 10.0;
		gridbag.setConstraints(set_cols_jb, con);
		panel.add(set_cols_jb);
		
		outer_panel.add( panel );
		outer_panel.add( Box.createRigidArea( new Dimension( 0,5 ) ) );
		
		// -- and the rows --
		
		panel = new JPanel();
		panel.setBorder( BorderFactory.createCompoundBorder( BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder( 0,5,5,5 ) ) );
		gridbag = new GridBagLayout();
		panel.setLayout(gridbag);
		line = 0;
		
		if(fd.cols == 1)
		    label = new JLabel("There is but 1 row )");
		else
		    label = new JLabel("There are " + fd.lines + " rows");
		label.setBorder(BorderFactory.createEmptyBorder(8,0,3,0));
		con = new GridBagConstraints();
		con.gridx = 0;
		con.gridy = line++;
		con.gridwidth = 4;
		gridbag.setConstraints(label, con);
		panel.add(label);

		label = new JLabel("from row ");
		con = new GridBagConstraints();
		con.gridx = 0;
		con.gridy = line;
		con.anchor = GridBagConstraints.EAST;
		gridbag.setConstraints(label, con);
		panel.add(label);
		
		final JTextField row_from_jtf = new JTextField(4);
		con = new GridBagConstraints();
		con.gridx = 1;
		con.gridy = line++;
		con.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(row_from_jtf, con);
		panel.add(row_from_jtf);
		
		label = new JLabel("to row ");
		con = new GridBagConstraints();
		con.gridx = 0;
		con.gridy = line;
		con.anchor = GridBagConstraints.EAST;
		gridbag.setConstraints(label, con);
		panel.add(label);

		final JTextField row_to_jtf = new JTextField(4);
		con = new GridBagConstraints();
		con.gridx = 1;
		con.gridy = line++;
		con.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(row_to_jtf, con);
		panel.add(row_to_jtf);
		
		filler = new Box.Filler(smallfillsize, smallfillsize, smallfillsize);
		con = new GridBagConstraints();
		con.gridx = 1;
		con.gridy = line;
		gridbag.setConstraints(filler, con);
		panel.add(filler);
		
		label = new JLabel("mode ");
		con = new GridBagConstraints();
		con.gridx = 0;
		con.gridy = line;
		con.anchor = GridBagConstraints.EAST;
		gridbag.setConstraints(label, con);
		panel.add(label);
		
		final JComboBox row_jcb = new JComboBox(row_type_names);
		con = new GridBagConstraints();
		con.gridx = 1;
		con.gridy = line;
		con.weighty = 1.0;
		con.weightx = 10.0;
		con.fill = GridBagConstraints.BOTH;
		gridbag.setConstraints(row_jcb, con);
		panel.add(row_jcb);
		
		/*
		filler = new Box.Filler(smallfillsize, smallfillsize, smallfillsize);
		con = new GridBagConstraints();
		con.gridx = 1;
		con.gridy = line++;
		gridbag.setConstraints(filler, con);
		panel.add(filler);
		*/

		JButton set_rows_jb = new JButton("Set");
		con = new GridBagConstraints();
		con.gridx = 3;
		con.gridy = line++;
		con.weighty = 1.0;
		con.weightx = 10.0;
		gridbag.setConstraints(set_rows_jb, con);
		panel.add(set_rows_jb);
		
		
		outer_panel.add( panel );
		outer_panel.add( Box.createRigidArea( new Dimension( 0,5 ) ) );
		
		
		// -- and close button --
		
		
		panel = new JPanel();
		//panel.setBorder( BorderFactory.createEmptyBorder( 9,0,0,0 ) );
		gridbag = new GridBagLayout();
		panel.setLayout(gridbag);
		
		
		JButton close_jb = new JButton("Close");
		con = new GridBagConstraints();
		con.anchor = GridBagConstraints.EAST;
		con.gridx = 0;
		con.weightx = 4.0;
		gridbag.setConstraints(close_jb, con);
		panel.add(close_jb);
		
		close_jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    quick_set_frame.setVisible(false);
			}
		    });
		
		
		filler = new Box.Filler(fillsize, fillsize, fillsize);
		con = new GridBagConstraints();
		con.gridx = 1;
		gridbag.setConstraints(filler, con);
		panel.add(filler);
		
		JButton help_jb = new JButton("Help");
		con = new GridBagConstraints();
		con.anchor = GridBagConstraints.WEST;
		con.gridx = 2;
		con.weightx = 4.0;
		gridbag.setConstraints(help_jb, con);
		panel.add( help_jb );
		
		help_jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    mview.getPluginHelpTopic("LoadPlainText", "LoadPlainText", "quickset");
			}
		    });
		
		
		outer_panel.add( panel );
		
		
		// ----------------------
	    

		quick_set_frame.getContentPane().add(outer_panel);
		quick_set_frame.pack();

	    
		set_cols_jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    int from_c = -1;
			    int to_c = -1;
			    try
			    {
				from_c = new Integer(col_from_jtf.getText()).intValue();
				to_c   = new Integer(col_to_jtf.getText()).intValue();
			    }
			    catch(NumberFormatException nfe)
			    {
				
			    }
			    int sel = col_jcb.getSelectedIndex();
			    
			    if(from_c < 1 || from_c > fd.cols )
			    {
				mview.alertMessage("Illegal range:\n  'from column' must be in the range 1..." + fd.cols);
				return;
			    }
			    if(to_c < 1 || to_c > fd.cols )
			    {
				mview.alertMessage("Illegal range:\n  'to column' must be in the range 1..." + fd.cols);
				return;
			    }
			    if(to_c < from_c )
			    {
				mview.alertMessage("Illegal range:\n  'to column' must be greater than 'from column'");
				return;
			    }
			    
			    for(int c=from_c; c <= to_c; c++)
			    {
				pd.col_contents[ c-1 ] = (short)sel;
				
			    }

			    updateTable(); // table.repaint();
			    
			}
		    });
		
		
		set_rows_jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    int from_r = -1;
			    int to_r = -1;
			    try
			    {
				from_r = new Integer(row_from_jtf.getText()).intValue();
				to_r   = new Integer(row_to_jtf.getText()).intValue();
			    }
			    catch(NumberFormatException nfe)
			    {
				
			    }
			    int sel = row_jcb.getSelectedIndex();
			    
			    if(from_r < 1 || from_r > fd.lines )
			    {
				mview.alertMessage("Illegal range:\n  'from row' must be in the range 1..." + fd.lines);
				return;
			    }
			    if(to_r < 1 || to_r > fd.lines )
			    {
				mview.alertMessage("Illegal range:\n  'to row' must be in the range 1..." + fd.lines);
				return;
			    }
			    if(to_r < from_r )
			    {
				mview.alertMessage("Illegal range:\n  'to row' must be greater than 'from row'");
				return;
			    }
			    
			    for(int r=from_r; r <= to_r; r++)
			    {
				pd.row_contents[ r-1 ] = (short)sel;
				
			    }
			updateTable(); // table.repaint();
			
			}
		    });
		
	    }
	    
	    quick_set_frame.setVisible(true);
	    
	}
	    

	private void quickSetSave()
	{
	    if(jfc == null)
		jfc = new JFileChooser();

	    jfc.setCurrentDirectory(new File(mview.getProperty("LoadPlainText.settings_path", System.getProperty("user.dir"))));

	    int returnVal =  jfc.showSaveDialog(mview.getDataPlot()); 
	    
	    if(returnVal == JFileChooser.APPROVE_OPTION) 
	    {
		mview.putProperty( "LoadPlainText.settings_path", jfc.getSelectedFile().getPath() );
		
	        File dest = jfc.getSelectedFile();
		
		if( dest.exists())
		{
		    if(mview.infoQuestion("File exists, overwrite?", "No", "Yes") == 0)
			return;
		}
		
		quickSetSave( dest, true );
	    }
	}
	private void quickSetSave( final File dest, final boolean report_errors )
	{
	    try
	    {
		BufferedWriter writer = new BufferedWriter( new FileWriter( dest ) );
		
		//System.out.println( "quickSetSave(): " + pd.col_contents.length + " cols, " + pd.row_contents.length + " rows." );
		
		if( pd != null )
		{
		    if( pd.col_contents != null )
		    {
			writeSequenceWithRunLengthEncoding( writer, "Column", pd.col_contents, col_type_names );
		    }
		    if( pd.row_contents != null )
		    {
			writeSequenceWithRunLengthEncoding( writer, "Row", pd.row_contents, row_type_names );
		    }
		}

		writer.close();

		System.out.println( "quickSetSave(): settings saved to '" + dest.getPath() + "'" ); 

	    }
	    catch( java.io.IOException ioe )
	    {
		if( report_errors )
		    mview.alertMessage("Unable to save the current settings.\n\n" + ioe );
	    }
	}
	

	private void writeSequenceWithRunLengthEncoding( BufferedWriter writer,  
							 String   sequence_type,
							 short[]  sequence,
							 String[] sequence_identifiers ) throws java.io.IOException
	{
	    short last_thing = sequence[ 0 ];
	    int last_change = 0;


/*
	    for( int s = 0; s < sequence.length; s++ )
	    {
	        writer.write( "[check] " +  sequence_type + " " + s + " " + sequence_identifiers [ sequence[ s ] ]  + "\n" );
	    }
*/


	    for( int s = 0; s < sequence.length; s++ )
	    {
		if( sequence[ s ] != last_thing )
		{
		    if( s > 0 )
		    {
			writeSequence( writer, last_change, s-1, sequence_type,  sequence_identifiers[ last_thing ] );
		    }

		    last_change = s;
		}
		
		last_thing = sequence[ s ];
	    }

	    if( last_change <= ( sequence.length - 1 )  )
	    {
		writeSequence( writer, last_change, sequence.length - 1, sequence_type,  sequence_identifiers[ sequence[ last_change ] ] );
	    }
	}

	private void writeSequence( BufferedWriter writer, int from, int to, String type, String identifier ) throws java.io.IOException
	{
	    if(( to - from ) > 1 )
	    {
		writer.write( type + "s " + ( from + 1 ) + " to " + ( to + 1 ) + " " + identifier + "\n" );
	    }
	    else
	    {
		writer.write( type + " " + ( from + 1 ) + " " + identifier + "\n" );
	    }
	}

	private void quickSetLoad()
	{
	    if(jfc == null)
		jfc = new JFileChooser();

	    jfc.setCurrentDirectory(new File(mview.getProperty("LoadPlainText.settings_path", System.getProperty("user.dir"))));

	    int returnVal =  jfc.showOpenDialog(mview.getDataPlot()); 

	    if( returnVal == JFileChooser.APPROVE_OPTION ) 
	    {
		quickSetLoad( jfc.getSelectedFile(), true );
	    }
	}

	private void quickSetLoad( final File source, final boolean report_errors )
	{
	    try 
	    { 
		BufferedReader br = new BufferedReader( new FileReader( source ) );
		
		String line = br.readLine();
		
		RowColInfo rci = new RowColInfo();
		
		int errors = 0;
		
		while(line != null)
		{
		    if( processQuickSetLine( rci, line ) == false )
		    {
			if( ++errors < 10 )
			    System.err.println("quickSetLoad(): unable to parse '" + line + "'" );
		    }
		    
		    line = br.readLine();
		}
		
		short[] rows = rci.get( true,  row_type_names );
		
		pd.row_contents = setSettings( pd.row_contents, rows, fd.lines );
		
		short[] cols = rci.get( false, col_type_names );
		
		pd.col_contents = setSettings( pd.col_contents, cols, fd.cols );
		
		System.out.println( "quickSetLoad(): settings loaded from '" + source.getPath() + "'" ); 
	    }
	    
	    catch( FileNotFoundException fnfe )
	    {
		return;
	    }
	    catch( IOException ioe )
	    {
		if( report_errors )
		    mview.errorMessage("Unable to open the settings file.\n\n" + ioe );

		return;
	    }
	}
	
	
	private boolean processQuickSetLine( final RowColInfo rci, final String line )
	{
	    java.util.StringTokenizer st = new java.util.StringTokenizer( line );

	    java.util.Vector tokens_v = new java.util.Vector();

	    while ( st.hasMoreTokens() ) 
		tokens_v.add( st.nextToken() );

	    if( tokens_v.size() < 3 )
		return false;

/*
	    for( int t = 0; t < tokens_v.size(); t++ )
	    {
		System.out.print( "[" + (String)tokens_v.elementAt( t ) + "]" );
	    }
	    System.out.println();
*/

	    String keyword = (String) tokens_v.elementAt( 0 );

	    try
	    {
		if( keyword.equals( "Rows" ) )
		{
		    int from_row_number  = new Integer( (String) tokens_v.elementAt( 1 ) ).intValue() - 1;
		    int to_row_number    = new Integer( (String) tokens_v.elementAt( 3 ) ).intValue() - 1;
		    String row_type      = (String) ( tokens_v.elementAt( 4 ) );
		    rci.set( from_row_number, to_row_number, row_type, true );
		    return true;
		}
		if( keyword.equals( "Row" ) )
		{
		    int row_number  = new Integer(( String) tokens_v.elementAt( 1 ) ).intValue() - 1;
		    String row_type = (String) tokens_v.elementAt( 2 );
		    rci.set( row_number, row_number, row_type, true );
		    return true;
		}
		if( keyword.equals( "Columns" ) )
		{
		    int from_col_number  = new Integer( (String) tokens_v.elementAt( 1 ) ).intValue() - 1;
		    int to_col_number    = new Integer( (String) tokens_v.elementAt( 3 ) ).intValue() - 1;
		    String col_type      = (String) ( tokens_v.elementAt( 4 ) );
		    rci.set( from_col_number, to_col_number, col_type, false );
		    return true;
		}
		if( keyword.equals( "Column" ) )
		{
		    int col_number  = new Integer( (String) tokens_v.elementAt( 1 ) ).intValue() - 1;
		    String col_type = (String) ( tokens_v.elementAt( 2 ) );
		    rci.set( col_number, col_number, col_type, false  );
		    return true;
		}
	    }
	    catch( NumberFormatException nfe )
	    {
		System.out.println("processQuickSetLine(): NumberFormatException...." );
		return false;
	    }
	    catch( Exception e )
	    {
		System.out.println("processQuickSetLine(): unexpected Exception...." + e );
		return false;
	    }

	    return false;
	}
	
	private short[] setSettings( short[] old_data, short[] new_data, int n_items )
	{
	    if( ( new_data == null ) || ( new_data.length == 0 ) )
	    {
		//System.out.println( "setSettings(): no new info");
		return old_data;
	    }
	    else
	    {
		//System.out.println( "setSettings(): info for items 0..." + new_data.length );
		
		if( new_data.length < n_items )
		{
		    // not enough info, just replace most of the old data with the new data

		    for(int i=0; i < new_data.length; i++)
			old_data[ i ] = new_data[ i ]; 

		    return old_data;
		}
		if( new_data.length > n_items )
		{
		    // more info than needed, trim the array
		    short[] trimmed_new_data = new short[ n_items ]; 
		    for(int i=0; i < n_items; i++)
			 trimmed_new_data[ i ] = new_data[ i ]; 

		    return trimmed_new_data;
		}
		
		// exactly the right amount!
		return new_data;
	    }
	}


	// intermediate format used whilst loading the row/col settings
	private class RowColInfo
	{
	    public int n_rows;
	    public int n_cols;
	    public java.util.Hashtable row_info;
	    public java.util.Hashtable col_info;

	    public RowColInfo() 
	    { 
		row_info = new java.util.Hashtable();
		col_info = new java.util.Hashtable();
	    }

	    public void set( final int from, final int to, final String thing, final boolean is_row )
	    {
		if( is_row )
		{
		    n_rows = from > n_rows ? from : n_rows;	
		    n_rows = to   > n_rows ? to   : n_rows;	
		}
		else
		{
		    n_cols = from > n_cols ? from : n_cols;	
		    n_cols = to   > n_cols ? to   : n_cols;	
		}

		for(int i=from; i <= to; i++)
		{
		    if( is_row )
		    {
			row_info.put( new Integer( i ), thing );
		    }
		    else
		    {
			col_info.put( new Integer( i ), thing );
		    }
		}
	    }

	    public short[] get( boolean is_row, String[] symbols )
	    {
		java.util.Hashtable symbol_codes = new java.util.Hashtable();
		for( short s=0; s < symbols.length; s++ )
		    symbol_codes.put( symbols[ s ] , new Short( s ) );

		int max = is_row ? n_rows : n_cols;
		
		short[] result = new short[ max + 1 ];

		for( int i=0; i <= max; i++ )
		{ 
		    String symbol  = (String) ( is_row ? row_info.get( new Integer(i) ) : col_info.get( new Integer(i) ) );

		    if( symbol != null )
		    {
			Short code = (Short) symbol_codes.get( symbol );

			if( code != null )
			{
			    result[ i ] = code.shortValue();
			    
			    //if( i < 20 )
			    //	System.out.println( ( is_row ? "Row" : "Col" ) + " " + (i+1) + "=" + symbols[ result[ i ] ]);
			}
		    }
		}
		
		return result;
	    }
	}

	// == the actual data loading stuff =====================================================

	private int countDataLines()
	{
	    int n_lines = 0;

	    for(int l=0; l < fd.lines; l++)
		if( pd.row_contents[l] == Data )
		    n_lines++;

	    return n_lines;
	}

	private int findColumnHeaderline()
	{
	    for(int l=0; l < fd.lines; l++)
		if( pd.row_contents[l] == ColumnHeader )
		    return l;
	    return -1;
	}

	// = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 

	private ExprData.DataTags makeDataTags()
	{
	    status("Extracting Names");

	    if(debug)
		System.out.println("makeDataTags()...start");
	    
	    String[] sn = null;
	    String[] pn = null; 
	    Vector gn_v = new Vector(); 
	    String[][] gn = null;

	    final int n_data_lines = countDataLines();

	    // --------------------------------------------------
	    // extract the relevant columns and perform some sanity checks
	    //
	    for(int n=0; n < pd.col_contents.length; n++)
	    {
		if(pd.col_contents[n] == SpotName)
		{
		    sn = getColumn(n, n_data_lines);

		    if(hasBlanks(sn))
		    {
			//.stopIt();
			mview.alertMessage("The 'SpotName' column must not contain missing entries");
			return null;
		    }

		    if(!isUnique(sn))
		    {
			String non_unique = findSomeDuplicates( sn, 5 );
			mview.alertMessage("All entries in the 'SpotName' column must be unique\n\n" + "Duplicates include: " + non_unique );
			return null;
		    }

		    if(debug)
			System.out.println( sn.length + " SpotNames found");
		    
		}
		if(pd.col_contents[n] == ProbeName)
		{
		    pn = getColumn(n, n_data_lines);
		}
		if(pd.col_contents[n] == GeneName)
		{
		    gn_v.addElement( getColumn (n, n_data_lines ));
		}
	    }

	    // need to re-order GeneName arrays
	    // (as more than GeneName might have been found per line)
	    //
	    final int n_gene_names = gn_v.size();

	    if(n_gene_names > 0)
	    {
		gn = new String[n_data_lines][];
		for(int n=0; n < n_data_lines; n++)
		{
		    gn[n] = new String[ n_gene_names ];
		    for(int g=0; g < n_gene_names; g++)
		    {
			gn[n][g] = ((String[]) gn_v.elementAt(g))[n];
		    }
		}
	    }
	    
	    if(sn == null)
	    {
		// spot names were not specified, generate automatically....
		sn = makeSyntheticSpotNames( n_data_lines );
	    }

	    if(pn == null)
	    {
		// probe names were not specified, generate automatically...
		if(pd.make_probe_names)
		    pn = makeSyntheticProbeNames( n_data_lines, gn );
	    }

	    
	    ExprData.DataTags dtags = edata.new DataTags(sn, pn, gn);
	    
	    
	    System.gc();
	    
	    status("Names are ok.");
	    
	    if(debug)
		System.out.println("makeDataTags()...done");
	    
	   return dtags;
	}

	// = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 

	private String makeNameUnique( String name, java.util.HashSet existing )
	{
	    int     suffix    = 2;
	    String  candidate = name;
	    boolean unique    = (existing.contains(candidate) == false);
	    while(!unique)
	    {
		candidate = name + "(" + (suffix++) + ")";
		unique    = (existing.contains(candidate) == false);
	    }
	    return candidate;
	}

	private String[] makeSyntheticSpotNames( final int n_spots )
	{
	    String[] sn = new String[n_spots];

	    int tmp = (n_spots+1);
	    int max_w = 1;
	    while(tmp >= 10)
	    {
		max_w++;
		tmp /= 10;
	    }
	    if(max_w > 12)
		max_w = -1;

	    // for efficiency, build this hashtable once only
	    String[] existing_spot_names = edata.getSpotName();
	    java.util.HashSet existing_spot_names_hs = new java.util.HashSet();
	    if(existing_spot_names != null)
		for(int s=0; s < existing_spot_names.length; s++)
		    existing_spot_names_hs.add( existing_spot_names[s] );


	    for(int s=0; s < n_spots; s++)
	    {
		String tmp_sn = makeSyntheticName("S", (s+1), max_w);
		
		sn[s] = makeNameUnique( tmp_sn, existing_spot_names_hs );
	    }

	    return sn;
	}


	private String[] makeSyntheticProbeNames( final int n_spots, final String[][] gnames )
	{
	    final int n_genes = (gnames == null) ? 0 : gnames.length;
	    String[] pnames = new String[ n_spots ];

	    int pi = 1;

	    //int max_w = (int)(Math.ceil(Math.log( n_spots )));  // damn! this is natural log, want log_base_10

	    int tmp = (n_spots+1);
	    int max_w = 1;
	    while(tmp >= 10)
	    {
		max_w++;
		tmp /= 10;
	    }
	    if(max_w > 12)
		max_w = -1;

	    // make sure duplicated genes are correctly matched
	    // with duplicated synthetic probe names
	    
	    Hashtable name_ht = new Hashtable();

	    for(int p=0; p < n_spots; p++)
	    {
		int id = -1;
		String gn = null;
		if(p < n_genes)
		{
		    gn = flatten( gnames[p] );
		    if(gn != null)
		    {
			Integer iid = (Integer) name_ht.get( gn );
			if(iid != null)
			{
			    id = iid.intValue();
			}
		    }
		}
		if(id == -1)
		{
		    id = pi++;
		    if(gn != null)
		    {
			name_ht.put( gn, new Integer( id ));
		    }
		}

		pnames[p] = makeSyntheticName( "P", id, max_w );
	    }
	    
	    return pnames;
	}

	private final String zero_pad_str = "0000000000000000000000";

	private final String makeSyntheticName( final String prefix, final int id, final int width )
	{
	    String id_s = String.valueOf(id);

	    if(width > 0)
	    {
		int pad = width - id_s.length();
		
		// System.out.println( "id=" + id + " with w=" + width + " needs pad=" + pad);
		
		if(pad > 0)
		    id_s = (zero_pad_str.substring(0,pad) + id_s);
	    }

	    return (prefix + id_s);
	}

	private final String flatten(final String[] sa)
	{
	    if((sa == null) || (sa.length == 0))
		return null;
	    StringBuffer sbuf = new StringBuffer();
	    for(int s=0; s < sa.length; s++)
	    {
		if((sa[s] != null) && (sa[s].length() > 0))
		{
		    sbuf.append(sa[s]);
		    sbuf.append("--");
		}
	    }
	    String res = sbuf.toString();
	    return (res.length() == 0) ? null : res;
	}


	// = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 

	private void installTagAttrs( final int n_data_lines, final ExprData.DataTags dtags )
	{

	    // now add the [Spot|Probe|Gene]_NameAttrs

	    final ExprData.TagAttrs snta = edata.getSpotTagAttrs();
	    final ExprData.TagAttrs pnta = edata.getProbeTagAttrs();
	    final ExprData.TagAttrs gnta = edata.getGeneTagAttrs();
	    
	    // what!! this removes all the attrs from the 'master' data, even if we are merging!

	    if( load_mode_jcb.getSelectedIndex() == 0 ) // == replace mode
	    {
		snta.removeAllAttrs();
		pnta.removeAllAttrs();
		gnta.removeAllAttrs();
	    }

	    for(int n=0; n < pd.col_contents.length; n++)
	    {
		String attr_name = null;

		if(attr_name == null)
		    attr_name = pd.col_names[n]; 
		
		if((attr_name == null) || (attr_name.length() == 0))
		    attr_name = getColumnNameFromData( n );
		
		if((attr_name == null) || (attr_name.length() == 0))
		    attr_name = ("Column_" + (n+1));
		
		if(pd.col_contents[n] == SpotNameAttr)
		{
		    final int snta_i = snta.addAttr( attr_name );
		    
		    final String[] data = getColumn( n, n_data_lines );
		    
		    for(int d=0; d < data.length; d++)
		    {
			if( dtags.spot_name[ d ] != null)
			    snta.setTagAttr( dtags.spot_name[ d ], snta_i, data[d]);
		    }
		}
		if(pd.col_contents[n] == ProbeNameAttr)
		{
		    final int pnta_i = pnta.addAttr( attr_name );
		    
		    final String[] data = getColumn( n, n_data_lines );
		    
		    for(int d=0; d < data.length; d++)
		    {
			if( dtags.probe_name[ d ] != null)
			    pnta.setTagAttr( dtags.probe_name[ d ], pnta_i, data[d]);
		    }
		}
		if(pd.col_contents[n] == GeneNameAttr)
		{
		    final int gnta_i = gnta.addAttr( attr_name );
		    
		    final String[] data = getColumn( n, n_data_lines );
		    
		    for(int d=0; d < data.length; d++)
		    {
			if( dtags.gene_names[ d ][0] != null)
			    gnta.setTagAttr( dtags.gene_names[ d ][0] , gnta_i, data[d]);
		    }
		}
	    }
	}
	
	// = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 


	private void setupNameColumns(ExprData.DataTags dtags)
	{
	    DataPlot dplot = mview.getDataPlot();
	    dplot.removeAllNameCols();
	    dplot.addNameCol();
	    dplot.addNameCol();
	}

	// = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 

	private int addMeasurements(ExprData.DataTags dtags) throws ImportingException 
	{
	    long before = System.currentTimeMillis();

	    if(debug)
		System.out.println("addMeasurements()...start");
	    
	    //pm.setMessage("Extracting Data");
	    
	    int count = 0;

	    Vector merge_v = new Vector();

	    for(int n=0; n < pd.col_contents.length; n++)
	    {
		if(debug)
		    System.out.println("checking column " + (n+1));
		
		if(pd.col_contents[n] == Spot)
		{
		    
		    if(addMeasurementFromColumn( n, count, dtags, merge_v ))
		    {
			count++;
		    }
		    else
		    {
			// this has gone wrong!
			
		    }

		    System.gc();
		}
	    }
	    
	    installTagAttrs( countDataLines(), dtags  );

	    // in merge mode, none of the data will have been installed yet, so initate the merge now

	    if( load_mode_jcb.getSelectedIndex() == 1 ) // == merge mode
	    {
		edata.mergeMeasurements( (ExprData.Measurement[]) (merge_v.toArray(new ExprData.Measurement[0])) );
	    }
	    
	    if(debug)
		System.out.println("addMeasurements()...done");

	    long after = (System.currentTimeMillis() - before) / 1000;
	    
	    mview.setMessage("Load Plain Text: loaded in " + mview.niceTime(after));

	    return count;
	}

	private boolean addMeasurementFromColumn( int col_id, int count, ExprData.DataTags dtags, Vector merge_v) throws ImportingException 
	{
	    
	    // locate any columns marked as containing SpotAttrs which are linked to this column
	    
	    Vector spot_attrs_v = new Vector();

	    String mname = pd.col_names[col_id]; 
		
	    if(mname == null)
		mname = getColumnNameFromData( col_id) ;
	    
	    if(mname == null)
		mname = ("Meas_" + (count+1));
	    
	    //pm.setMessage( mname );
	    status("Loading '" + mname + "...");

	    final int n_data_lines = countDataLines();

	    double[] data_d = getDataAsDoubles( getColumn( col_id, n_data_lines ) );
	    
	    if(data_d == null)
	    {
		//pm.stopIt();
		
		String baddies = getNonDoubleEntries( getColumn( col_id, n_data_lines ), 3 );

		throw new ImportingException("Spot data in column " + (col_id + 1) + " has illegal entries, for example:\n" + 
					     "  " + baddies + ",\n" + 
					     "Spot data entries must be a number, a blank, 'NaN', 'Infinity' or '-Infinity')");
	    }
	    else
	    {
		if(debug)
		    System.out.println(mname + " has " + data_d.length + " spots");
	    }

	    for(int n=0; n < pd.col_contents.length; n++)
	    {
		if(pd.col_contents[n] == SpotAttrNext)
		{
		    if(findNearestNextSpotColumn(n) == col_id)
		    {
			spot_attrs_v.addElement( new Integer( n ));
		    }
		    
		}
		if(pd.col_contents[n] == SpotAttrPrev)
		{
		    if(findNearestPrevSpotColumn(n) == col_id)
		    {
			spot_attrs_v.addElement( new Integer( n ));
		    }
		}
	    }
	    
	    ExprData.Measurement new_m = edata.new Measurement(mname, 
							       ExprData.ExpressionAbsoluteDataType,
							       data_d);

	    new_m.addAttribute("Source.File_Name",    plugin_name, fd.file.getPath());
	    new_m.addAttribute("Source.Column_Index", plugin_name, String.valueOf(col_id + 1));
	    //new_m.addAttribute("Source.From_Line",    plugin_name, String.valueOf(pd.start_line));
	    //new_m.addAttribute("Source.To_Line",      plugin_name, String.valueOf(pd.end_line));
	    
	    // System.out.println(mname + " has " + spot_attrs_v.size() + " SpotAttrs");

	    //
	    // add any SpotAttributes 
	    //

	    for(int sa=0; sa < spot_attrs_v.size(); sa++)
	    {
		final String[] data_type_name = { "INTEGER", "DOUBLE", "CHAR", "TEXT" };
		
		int sa_col_id = ((Integer) spot_attrs_v.elementAt(sa)).intValue();
		
		String sa_name = pd.col_names[sa_col_id]; 
		if(sa_name == null)
		    sa_name = getColumnNameFromData(sa_col_id) ;
		if(sa_name == null)
		    sa_name = ("Column_" + (sa_col_id+1));
		
		String   sa_unit     = "No_Unit";
		String[] sa_data_str = getColumn(sa_col_id, n_data_lines );
		int      sa_type     = getDataType( sa_data_str );
		Object   sa_data     = null;

		switch(sa_type)
		{
		case ExprData.Measurement.SpotAttributeIntDataType:
		    sa_data = getDataAsInts( sa_data_str );
		    break;
		case ExprData.Measurement.SpotAttributeDoubleDataType:
		    sa_data = getDataAsDoubles( sa_data_str );
		    break;
		case ExprData.Measurement.SpotAttributeCharDataType:
		    sa_data = getDataAsChars( sa_data_str );
		    break;
		case ExprData.Measurement.SpotAttributeTextDataType:
		    sa_data = sa_data_str;
		    break;
		}

		if(sa_data != null)
		{
		    if(debug)
			System.out.println("  " + sa_name + " type=" + 
					   data_type_name[sa_type] +
					   " (col " + (sa_col_id + 1) + ")");
		    
		    new_m.addSpotAttribute( sa_name, sa_unit, data_type_name[sa_type], sa_data );
		}
		else
		{
		    throw new ImportingException("Couldn't parse SpotAttr data in column " + (sa_col_id+1));
		}
	    }

	    //
	    // add any MeasurementAttributes
	    //

	    for(int l=0; l < fd.lines; l++)
	    {
		if( pd.row_contents[l] == MeasurementAttr)
		{
		    Vector v = (Vector)fd.data.elementAt(l);

		    if( col_id < v.size() )
		    {
			String ma_value = (String) v.elementAt( col_id );
			
			String ma_name = "Attr(Line:" + l + ")";   // synthetic name in case no real name is specified
			
			//System.out.println("checking for MeasurementAttrName for value=" + ma_value + " on line " + l);
			
			// is there a MeasAttrName volumn?
			
			for(int man_col=0; man_col < pd.col_contents.length; man_col++)
			{
			    if(pd.col_contents[man_col] == MeasurementAttrName)
			    {
				v = (Vector) fd.data.elementAt(l);
				
				String poss_name = (String) v.elementAt( man_col );
				
				//System.out.println("found '" + poss_name + "' in column " + man_col);
				
				if((poss_name != null) && (poss_name.length() > 0))
				    ma_name = poss_name;
			    }
			}
			
			if((ma_value != null) && (ma_value.length() > 0))
			    new_m.addAttribute( ma_name, plugin_name, ma_value );
		    }
		}
	    }

	    //
	    // and insert the data (or store for subsequent merging)
	    //

	    if( load_mode_jcb.getSelectedIndex() == 0 ) // == replace mode
	    {
		if(count == 0)
		{
		    new_m.setDataTags(dtags);
		    setupNameColumns(dtags);
		    edata.addMeasurement(new_m);
		}
		else
		{
		    edata.addOrderedMeasurement(new_m);
		}
	    }
	    else
	    {
		new_m.setDataTags(dtags);
		merge_v.addElement( new_m );
	    }
	    
	    if(debug)
		System.out.println(mname + " inserted");

	    return true;
	}

	private int findNearestNextSpotColumn(int pos)
	{
	    while(++pos < pd.col_contents.length)
	    {
		if(pd.col_contents[pos] == Spot)
		{
		    return pos;
		}
	    }
	    return -1; // signals not found
	}
	private int findNearestPrevSpotColumn(int pos)
	{
	    while(--pos >= 0)
	    {
		if(pd.col_contents[pos] == Spot)
		{
		    return pos;
		}
	    }
	    return -1; // signals not found
	}
	// ==========================================================================

	// ProgressOMeter pm;

	String filename;
	JTextField filename_jtf;

	JComboBox load_mode_jcb, delim_jcb, encoding_jcb;

	JTextField missing_jtf;
	JTextField com_jtf;

	JTable table;

	
	// --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
	// --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
	
	// quick set popup menu
	
	public void displayCellPopup( final int mouse_x, final int mouse_y, final int row, final int col )
	{
	    JPopupMenu popup = new JPopupMenu();
	    JMenuItem mi;
	    
	    JMenu row_menu = new JMenu("This row");
	    for(int r=0; r < row_type_names.length; r++)
	    {
		final short row_type = (short) r;

		mi = new JMenuItem( row_type_names[ r ] );

		mi.addActionListener( new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    pd.row_contents[ row ] = row_type;
			    updateTable();
			}
		    });

		row_menu.add( mi );
	    } 
	    popup.add( row_menu );
	    
	    
	    JMenu after_row_menu = new JMenu("Rows above this one");
	    for(int r=0; r < row_type_names.length; r++)
	    {
		final short row_type = (short) r;

		mi = new JMenuItem( row_type_names[ r ] );

		mi.addActionListener( new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    for(int n=0; n < row; n++)
				pd.row_contents[ n ] = row_type;
			    updateTable();
			}
		    });

		after_row_menu.add( mi );
	    } 
	    popup.add( after_row_menu );

	    JMenu before_row_menu = new JMenu("Rows below this one");
	    for(int r=0; r < row_type_names.length; r++)
	    {
		final short row_type = (short) r;

		mi = new JMenuItem( row_type_names[ r ] );

		mi.addActionListener( new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    for(int n=row+1; n < pd.row_contents.length; n++)
				pd.row_contents[ n ] = row_type;
			    updateTable();
			}
		    });

		before_row_menu.add( mi );
	    } 
	    popup.add( before_row_menu );


	    JMenu col_menu = new JMenu("This column");
	    for(int c=0; c < col_type_names.length; c++)
	    {
		final short col_type = (short) c;
		
		mi = new JMenuItem( col_type_names[ c ] );

		mi.addActionListener( new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    pd.col_contents[ col ] = col_type;
			    updateTable();
			}
		    });

		col_menu.add( mi );
	    } 
	    popup.add( col_menu );

	    JMenu before_col_menu = new JMenu("Columns before this one");
	    for(int c=0; c < col_type_names.length; c++)
	    {
		final short col_type = (short) c;
		
		mi = new JMenuItem( col_type_names[ c ] );

		mi.addActionListener( new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    for(int n=0; n < col; n++)
				pd.col_contents[ n ] = col_type;
			    updateTable();
			}
		    });

		before_col_menu.add( mi );
	    } 
	    popup.add( before_col_menu );

	    
	    JMenu after_col_menu = new JMenu("Columns after this one");
	    for(int c=0; c < col_type_names.length; c++)
	    {
		final short col_type = (short) c;
		
		mi = new JMenuItem( col_type_names[ c ] );

		mi.addActionListener( new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    for(int n=col+1; n < pd.col_contents.length; n++)
				pd.col_contents[ n ] = col_type;
			    updateTable();
			}
		    });

		after_col_menu.add( mi );
	    } 
	    popup.add( after_col_menu );

	    JMenu ignore_menu = new JMenu("Ignore");

	    popup.show( table, mouse_x, mouse_y );
	    
	}
	
	public class TableMouseListener implements MouseListener
	{
	    private boolean showPopupIfTrigger(MouseEvent e)
	    {
		if( e.isPopupTrigger() || e.isAltDown() || e.isMetaDown() || e.isAltGraphDown() ) 
		{
		    // figure out which cell we are in...
		    int data_row = table.rowAtPoint( e.getPoint() ) - 6;     // the -6 is so that the info rows are ignored
		    int data_col = table.columnAtPoint( e.getPoint() ) - 4;  // the -4 is so that the info cols are ignored
		    
		    System.out.println("TableMouseListener() cell: r=" + data_row + " c=" + data_col );
		    
		    if( ( data_row >= 0 ) && (  data_col >= 0 ) )
			displayCellPopup( e.getX(), e.getY(), data_row, data_col );
		    
		    return true;
		}
		else
		{
		    return false;
		}
	    }
	    
	    public void mousePressed(MouseEvent e) 
	    {
		showPopupIfTrigger(e);
	    }
	    
	    
	    public void mouseReleased(MouseEvent e) 
	    {
		// showPopupIfTrigger(e);
	    }
	    
	    public void mouseClicked(MouseEvent e) 
	    {
		showPopupIfTrigger(e);
	    }
	    
	    public void mouseEntered(MouseEvent e) {}
	    public void mouseExited(MouseEvent e) {}
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  state
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
 
    class FileDetails
    {
	File   file;
	String filename;
	
	Vector data;
	
	int    lines;
	int    cols;
    }

    FileDetails fd = null;
    
    class ParseDetails
    {
	String[] col_names;       // names to use for data and attr columns
 
	short[]  row_contents;    // 0 == comment, 1 == skip, 2 == data
	short[]  col_contents;    // how to interpret each column
	
	//int n_data_lines;         // lines containing data not comments, col.names or ignored
	
	String comment_prefix;    // ignore lines that start with this string

	String missing_value;     // the string that indicates a missing value e.g. "BLANK"

	String encoding_name;     // e.g. US-ASCII, UTF-1 etc

	//int[]  col_types;       //  not used

	boolean make_spot_names;
	boolean make_probe_names;

    }

    ParseDetails pd = null;

    private JFrame quick_set_frame = null;

    private JPanel status_panel;
    private JLabel status_label;
    private JPanel file_name_panel;

    private boolean silent_running = false;
    private boolean override_really_replace = false;

    private maxdView mview;
    private ExprData edata;

    private JPanel import_panel;
    private LoaderFrame frame  = null;
    private String[] header_title = null;

    private JFileChooser jfc = null;

    private String previous_settings_file;
}
