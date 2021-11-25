import javax.swing.*;
import java.io.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

// import ExprData.*;

import javax.swing.filechooser.*;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.zip.*;

import java.lang.reflect.*;

import org.xml.sax.AttributeList;
import org.xml.sax.Configurable;
import org.xml.sax.HandlerBase;
import org.xml.sax.Parser;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.InputSource;

// --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
// --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
// --- --- ---  
// --- --- ---   ReadNative
// --- --- ---  
// --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
// --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  


public class ReadNative implements Plugin
{
    
    public ReadNative(maxdView mview_)
    {
	mview = mview_;
	edata = mview.getExprData();
	dplot = mview.getDataPlot();
    }

    public void cleanUp()
    {
	// remove the class loader path

	saveProps();
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  


    private void loadProps()
    {
	
	keep_existing_clusters_jrb.setSelected(mview.getBooleanProperty("ReadNative.keep_existing_clusters",false));
	keep_existing_attributes_jrb.setSelected(mview.getBooleanProperty("ReadNative.keep_existing_attributes",false)); 
	keep_existing_colourisers_jrb.setSelected(mview.getBooleanProperty("ReadNative.keep_existing_colourisers",false)); 
	
    }

    private void saveProps()
    {
	String cur_path = jfc.getCurrentDirectory().getPath();
	mview.putProperty("ReadNative.data_load_directory",  cur_path);

	mview.putBooleanProperty( "ReadNative.keep_existing_clusters",  
				  keep_existing_clusters_jrb.isSelected() );
	mview.putBooleanProperty( "ReadNative.keep_existing_attributes_jrb",  
				  keep_existing_attributes_jrb.isSelected() );
	mview.putBooleanProperty( "ReadNative.keep_existing_colourisers_jrb",  
				  keep_existing_colourisers_jrb.isSelected() );
	

    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  plugin implementation
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("Read Native", "importer", 
					 "Read data in maxdView native format", 
					 "Requires the Xerces XML parser,<BR>" + 
					 "Copyright 1999 The Apache Software Foundation.  All rights reserved.<BR>" + 
					 "see <A HREF=\"http://www.apache.org/\">http://www.apache.org/</A>",
					 1, 1, 0);
	return pinf;
    }

    public PluginCommand[] getPluginCommands()
    {
	PluginCommand[] com = new PluginCommand[2];

	String[] args = new String[] 
	{ 
	    // name           // type       //default   // flag   // comment
	    "file",          "file",        "",         "m", 	   "source file name",
	    "mode",          "string",      "merge",    "", 	   "either 'replace' or 'merge'",

	    "really_replace", "boolean",    "false",    "",        "override the confirmation dialog when replacing existing data",

	    "keep_existing_clusters", "boolean",    "false",    "",  "retain existing Clusters when loading new data",

	    "keep_existing_colourisers", "boolean",    "false",    "",    "retain existing colourisers when loading new data",
	    "keep_existing_name_attrs",  "boolean",    "false",    "",    "retain existing Name Attributes when loading new data"
	};

	com[0] = new PluginCommand("start", null);
	com[1] = new PluginCommand("load", args);

	return com;
    }
    
    public void  runCommand(String name, String[] args, CommandSignal done) 
    { 
	//System.out.println( "ReadNative.runCommand '" + name + "'" );
	
	if(name.equals("start"))
	{
	    startPlugin();
	    if(done != null)
		done.signal();
	    return;
	}

	if(name.equals("load"))
	{
	    
	    String fname = mview.getPluginArg("file", args);

	    if(fname != null)
	    {
		if( loadSAXParser() )
		{	    
		    //System.out.println("loading from '" + fname + "'....");
		    
		    int mode =  MergeMode;
		    
		    String arg = mview.getPluginStringArg("mode", args, "replace");
		    if(arg.equals("replace"))
			mode = ReplaceMode;
		    if(arg.equals("merge"))
			mode = MergeMode;
		    
		    really_replace_meas     = mview.getPluginBooleanArg("really_replace", args, false );
		   
		    //really_replace_clusters = mview.getPluginBooleanArg("really_replace_clusters", args, false );
		    //really_keep_clusters    = mview.getPluginBooleanArg("really_keep_clusters", args, false );

		    keep_existing_clusters    = mview.getPluginBooleanArg("keep_existing_clusters", args, false );
		    keep_existing_name_attrs  = mview.getPluginBooleanArg("keep_existing_name_attrs", args, false );
		    keep_existing_colourisers = mview.getPluginBooleanArg("keep_existing_colourisers", args, false );
		    
		    readData(fname, mode, done);
		}
		else
		{
		    if(done != null)
			done.signal();
		}
		
	    }
	    else
	    {
		// no file name argument
		startPlugin();
		if(done != null)
		    done.signal();
	    }
	}
    }

    public void stopPlugin()
    {
	cleanUp();
    }

    public String pluginType() { return "importer"; }


    public class CustomFileFilter extends javax.swing.filechooser.FileFilter 
    {
	
	public  String getExtension(String s) 
	{
	    String ext = null;
	    int i = s.lastIndexOf('.');
	    
	    if (i > 0 &&  i < s.length() - 1) 
	    {
		ext = s.substring(i+1).toLowerCase();
		
		if(ext.equals("gz"))
		{
		    return getExtension(s.substring(0,i));
		}
	    }

	    return ext;
	}

	public boolean accept(File f) 
	{
	    if (f.isDirectory()) 
	    {
		return true;
	    }
	    
	    String extension = getExtension(f.getName());

	    if(extension != null) 
	    {
		if (extension.equals("maxd"))
		    return true;
	    } 
	    
	    return false;
	}
	
	// The description of this filter
	public String getDescription() 
	{
	    return ".maxd Files";
	}
    }

    private boolean loadSAXParser()
    {
	if( parser != null )
	    // already loaded....
	    return true;

	
	mview.setMessage("Initialising Xerces classes");

	Thread.yield();

	
	sax_path = mview.getProperty("ReadNative.sax_path", null);

	if(sax_path == null)
	{
	    sax_path = mview.getTopDirectory() + File.separator + "external" + File.separator + "xml4j" + File.separator + "xerces.jar";
	}

	Class sax_class = null;
	

	maxdView.CustomClassLoader ucl = (maxdView.CustomClassLoader) getClass().getClassLoader();

		
	while(sax_class == null)
	{
	    try
	    {
		ucl.addPath( sax_path );
		
		sax_class = ucl.findClass( "org.apache.xerces.parsers.SAXParser" );
	    }
	    catch( java.net.MalformedURLException murle )
	    {
		if( pm != null )
		    pm.stopIt();

		mview.alertMessage( "Unable to load '" + sax_path + "'" );
	    }

	    if(sax_class == null)
	    {
		try
		{
		    String msg = "Unable to find Xerces classes\n";
		    msg += (sax_path == null)  ? "\n" : ("in '" + sax_path + "'\n");
		    msg += "Press \"Find\" to specify the location of the JAR file,\n" + 
		    "  or\nPress \"Cancel\" to stop the plugin.\n";
		    
		    if( pm != null )
			pm.stopIt();

		    if(mview.alertQuestion(msg, "Find", "Cancel") == 1)
			return false;
		    
		    sax_path = mview.getFile("Location of 'xerces.jar'", sax_path);
		}
		catch(UserInputCancelled uic)
		{
		    // don't start the plugin
		    return false;
		}
	    }
	    else
	    {

		mview.putProperty("ReadNative.sax_path", sax_path);

		//org.apache.xerces.parsers.SAXParser tmp = new org.apache.xerces.parsers.SAXParser();

		try
		{
		    parser = (org.apache.xerces.parsers.SAXParser) sax_class.newInstance();
		    
		    // org.apache.xerces.framework.XMLParser test = (org.apache.xerces.framework.XMLParser) object;
		    
		} 
		catch (InstantiationException e) 
		{
		    if( pm != null )
			pm.stopIt();
		    System.out.println(e);
		    return false;
		} 
		catch (IllegalAccessException e) 
		{
		    if( pm != null )
			pm.stopIt();
		    System.out.println(e);
		    return false;
		} 
		catch (Exception e) 
		{
		    if( pm != null )
			pm.stopIt();
		    System.out.println(e);
		    return false;
		} 

	    }
	}

	return true;
    }

    public void startPlugin()
    {
	// ======================================================================

	//if( !loadSAXParser() )
	//    return;

	// System.out.println("xerces class loaded: " + sax_class);
	
	// ======================================================================

	int oline = 0;

	JPanel accessory = new JPanel();
	
	accessory.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	
	GridBagLayout acc_gridbag = new GridBagLayout();
	accessory.setLayout(acc_gridbag);
	
	//	accessory.setLayout(new BoxLayout(accessory, BoxLayout.Y_AXIS));
	
	/*
	JLabel label = new JLabel("Load how?");
	accessory.add(label);
	GridBagConstraints c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = oline++;
	c.gridwidth = 2;
	//c.weighty = 1.0;
	gridbag.setConstraints(label, c);
	*/

	JLabel label;
	GridBagConstraints c;
	Font small_font = mview.getSmallFont();

	ButtonGroup bgroup1 = new ButtonGroup();
	ButtonGroup bgroup2 = new ButtonGroup();

	// -----------------------


	JPanel accessory_1 = new JPanel();
	accessory_1.setBorder(BorderFactory.createTitledBorder("  Load how?  "));
	
	GridBagLayout gridbag = new GridBagLayout();
	accessory_1.setLayout(gridbag);
	
	replace_jrb = new JRadioButton("Replace");
	replace_jrb.setSelected(true);
	accessory_1.add(replace_jrb);
	//replace_jrb.setEnabled(false);
	bgroup1.add(replace_jrb);
	replace_jrb.setFont(small_font);
	
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = oline;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(replace_jrb, c);

	merge_jrb = new JRadioButton("Merge");
	accessory_1.add(merge_jrb);
	//merge_jrb.setEnabled(false);
	merge_jrb.setFont(small_font);
	bgroup1.add(merge_jrb);
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = oline++;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(merge_jrb, c);
	

	keep_existing_clusters_jrb = new JCheckBox("Keep existing Clusters");
	accessory_1.add(keep_existing_clusters_jrb);
	keep_existing_clusters_jrb.setFont(small_font);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = oline++;
	c.gridwidth = 2;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(keep_existing_clusters_jrb, c);
	keep_existing_clusters_jrb.setEnabled( edata.getNumClusters() > 1 );
	
	keep_existing_colourisers_jrb = new JCheckBox("Keep existing Colourisers");
	accessory_1.add(keep_existing_colourisers_jrb);
	keep_existing_colourisers_jrb.setFont(small_font);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = oline++;
	c.gridwidth = 2;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(keep_existing_colourisers_jrb, c);

	keep_existing_attributes_jrb = new JCheckBox("Keep existing Name Attrs.");
	accessory_1.add(keep_existing_attributes_jrb);
	keep_existing_attributes_jrb.setFont(small_font);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = oline++;
	c.gridwidth = 2;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(keep_existing_attributes_jrb, c);
	// keep_existing_attributes_jrb.setEnabled( false );
	
	
	loadProps();

	// -----------------------

	JPanel accessory_2 = new JPanel();
	accessory_2.setBorder(BorderFactory.createTitledBorder("  Load what?  "));
	
	gridbag = new GridBagLayout();
	accessory_2.setLayout(gridbag);
	
	int ctop = 0;

	measurements_jrb = new JCheckBox("Measurements");
	accessory_2.add(measurements_jrb);
	//bgroup2.add(measurement_jrb);
	measurements_jrb.setFont(small_font);
	measurements_jrb.setSelected(true);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = ctop++;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(measurements_jrb, c);
	
	spot_attrs_jrb = new JCheckBox("Spot Attrs.");
	accessory_2.add(spot_attrs_jrb);
	//bgroup2.add(spot_attrs_jrb);
	spot_attrs_jrb.setFont(small_font);
	spot_attrs_jrb.setSelected(true);
	spot_attrs_jrb.addActionListener(new ActionListener() 
	    { 
		public void actionPerformed(ActionEvent e) 
		{
		    if(spot_attrs_jrb.isSelected())
			measurements_jrb.setSelected(true);
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = ctop++;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(spot_attrs_jrb, c);

	name_attrs_jrb = new JCheckBox("Name Attrs.");
	accessory_2.add(name_attrs_jrb);
	//bgroup2.add(clusters_jrb);
	name_attrs_jrb.setFont(small_font);
	name_attrs_jrb.setSelected(true);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = ctop++;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(name_attrs_jrb, c);

	clusters_jrb = new JCheckBox("Clusters");
	accessory_2.add(clusters_jrb);
	//bgroup2.add(clusters_jrb);
	clusters_jrb.setFont(small_font);
	clusters_jrb.setSelected(true);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = ctop++;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(clusters_jrb, c);

	ctop = 0;

	layout_jrb = new JCheckBox("Layout");
	accessory_2.add(layout_jrb);
	//bgroup2.add(layout_jrb);
	layout_jrb.setFont(small_font);
	layout_jrb.setSelected(true);
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = ctop++;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(layout_jrb, c);
	
	colours_jrb = new JCheckBox("Colourisers");
	accessory_2.add(colours_jrb);
	//bgroup2.add(colours_jrb);
	colours_jrb.setFont(small_font);
	colours_jrb.setSelected(true);
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = ctop++;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(colours_jrb, c);

	app_props_jrb = new JCheckBox("App.Props.");
	accessory_2.add(app_props_jrb);
	//bgroup2.add(app_props_jrb);
	app_props_jrb.setFont(small_font);
	app_props_jrb.setSelected(true);
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = ctop++;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(app_props_jrb, c);

	ctop = 4;

	JPanel wrapper = new JPanel();
	GridBagLayout wrapbag = new GridBagLayout();
	wrapper.setLayout(wrapbag);
	JButton jb = new JButton("Nothing");
	wrapper.add(jb);
	Insets ins = new Insets(1,4,1,4);
	jb.setMargin(ins);
	jb.addActionListener(new ActionListener() 
	    { 
		public void actionPerformed(ActionEvent e) 
		{
		    setParts(false);
		}
	    });
	//bgroup2.add(clusters_jrb);
	jb.setFont(small_font);
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.EAST;
	wrapbag.setConstraints(jb, c);
	jb = new JButton("All");
	jb.setMargin(ins);
	jb.addActionListener(new ActionListener() 
	    { 
		public void actionPerformed(ActionEvent e) 
		{
		    setParts(true);
		}
	    });
	wrapper.add(jb);
	//bgroup2.add(clusters_jrb);
	jb.setFont(small_font);
	c = new GridBagConstraints();
	c.gridx = oline++;
	c.anchor = GridBagConstraints.WEST;
	wrapbag.setConstraints(jb, c);
	
	accessory_2.add(wrapper);
	//bgroup2.add(clusters_jrb);
	jb.setFont(small_font);
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = ctop;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(wrapper, c);
	
	

	c = new GridBagConstraints();
	c.weightx = 7.0;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	acc_gridbag.setConstraints(accessory_1, c);
	accessory.add(accessory_1);

	c = new GridBagConstraints();
	c.weightx = 7.0;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.gridy = 1;
	acc_gridbag.setConstraints(accessory_2, c);
	accessory.add(accessory_2);

	jfc = new JFileChooser();
	
	jfc.setAccessory(accessory);

	
	final javax.swing.filechooser.FileFilter all_ff = jfc.getFileFilter();
	final CustomFileFilter cff = new CustomFileFilter();
	//final CustomFileFilter cff = null;

	jfc.addChoosableFileFilter(cff);

	int filter_mode = mview.getIntProperty("ReadNative.filename_filter_mode", 0);

	// System.out.println("in fnfm=" + ((filter_mode == 1) ? "maxd" : "*.*"));

	jfc.setFileFilter( filter_mode == 1 ? cff : all_ff);
       

	jfc.setPreferredSize( new Dimension( 680, 480 ));

	//jfc.updateUI();
	//jfc.pack();
	//jfc.revalidate();

	// look for the data load directory in the saved prefs
	//
	String dld = mview.getProperty("ReadNative.data_load_directory", ".");
	
	if(dld != null)
	{
	    File ftmp = new File(dld);
	    jfc.setCurrentDirectory(ftmp);
	}
	
	int returnVal =  jfc.showOpenDialog(mview.getDataPlot()); 
	if(returnVal == JFileChooser.APPROVE_OPTION) 
	{
	    File file = jfc.getSelectedFile();
	    if(file != null)
	    {
		load_mode = (replace_jrb.isSelected() ? ReplaceMode : MergeMode);

		load_measurements = measurements_jrb.isSelected();
		load_spot_attrs = spot_attrs_jrb.isSelected();
		load_layout = layout_jrb.isSelected();
		load_colours = colours_jrb.isSelected();
		load_app_props = app_props_jrb.isSelected();
		load_clusters = clusters_jrb.isSelected();
		load_name_attrs = name_attrs_jrb.isSelected();

		keep_existing_clusters    = keep_existing_clusters_jrb.isSelected();
		keep_existing_colourisers = keep_existing_colourisers_jrb.isSelected();
		keep_existing_name_attrs  = keep_existing_attributes_jrb.isSelected();

		readData(file.getPath(), load_mode, null);

		cleanUp();
		
	    }
	    else
	    {
		mview.errorMessage("No file selected");
	    }
	
	}

	// System.out.println("out fnfm=" + ((jfc.getFileFilter() == cff) ? "maxd" : "*.*"));

	mview.putIntProperty("ReadNative.filename_filter_mode", ((jfc.getFileFilter() == cff) ? 1 : 0));
    }

    private void setParts(boolean en)
    {
	measurements_jrb.setSelected(en);
	spot_attrs_jrb.setSelected(en);
	layout_jrb.setSelected(en);
	colours_jrb.setSelected(en);
	app_props_jrb.setSelected(en);
	clusters_jrb.setSelected(en);
	name_attrs_jrb.setSelected(en);
    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   read data
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private ProgressOMeter pm;
    
    public void readData(String file_name, int mode, CommandSignal done)
    {
	// give it a few goes in case something else is locking the class loader?

	//int attempts = 0;
	//while(attempts < 5)
	{
	    //try
	    {
		// Class.forName("org.xml.sax.HandlerBase");
		
		//new SAXRead().doLoad(parserName, (file_name), validate);

		boolean remove_the_clusters = true;

		if(mode == ReplaceMode)
		{
		    if((edata.getNumMeasurements() > 0))
		    {
			if(!really_replace_meas)
			    if(mview.infoQuestion("Really replace existing data?", "Yes", "No") == 1)
			    {
				return;
			    }
		    }

		    //  System.out.println("there are " + edata.getNumClusters() + " clusters");

		   
		}
		
		pm = new ProgressOMeter("Loading", 3);
		pm.startIt();

		if(!keep_existing_clusters)
		    edata.removeAllClusters();
		
		if(mode == ReplaceMode)
		{
		    dplot.removeAllNameCols();

		    edata.removeAllMeasurements(!keep_existing_name_attrs);

		    if(!keep_existing_colourisers)
			dplot.removeAllColourisers();
		}

		new ReaderThread(file_name, mode, done).start();
		
		return;
	    }

	    //catch(java.lang.ClassNotFoundException cnfe)
	    //{
	    //}
	    
	    /*
	    try
	    {
		Thread.sleep(250);
	    }
	    catch(java.lang.InterruptedException ie)
	    {
	    }

	    attempts++;
	    */
	}

	/*
	if(mview.alertQuestion("ReadNative: unable to load parser class from 'org.xml.sax.HandlerBase'\n" + 
			      "Make sure the Xerces JAR file is in the classpath and restart maxdView",
			      "OK", "More details") == 1)
	{ 
	    mview.infoMessage("The classpath is currently:\n  '" + System.getProperty("java.class.path") + "'");
	}
	return;
	*/
    }

    public class ReaderThread extends Thread
    {
	public ReaderThread(String file_name, int mode, CommandSignal done)
	{
	    fn = file_name;
	    lm = mode;
	    dn = done;
	}
	public void run()
	{
	    String  parserName = "org.apache.xerces.parsers.SAXParser";
	    boolean validate = false;

	    final String error_mesg = "whilst loading xerces from JAR: \n";

	    // make sure the file exists!
	    File test = new File(fn);
	    if(test.exists())
	    {
		try 
		{
		    /*
		    Class[] ctor_arg_types = new Class[1];
		    ctor_arg_types[0] = Integer.TYPE;
		    */

		    /*
		    Constructor[] srcons = srclass.getConstructors(  );
		    
		    System.out.println(srcons.length + "c'tors found");
		    
		    Class[] ripoff = null;

		    for(int c=0; c < srcons.length; c++)
		    {
			Class[] ptypes = srcons[c].getParameterTypes();
			System.out.println("   #" + c + " has " + ptypes.length + " args");

			if(ptypes.length == 1)
			{
			    ripoff  = ptypes;

			    System.out.println(" same? " + (ptypes[0] == (ReadNative.this).getClass()));
			}

			for(int p=0; p < ptypes.length; p++)
			{
			    System.out.println("  " + c + "." + p + " = " + ptypes[p]);
			}
		    }

		    Class[] ctor_arg_types = new Class[1];
		    ctor_arg_types[0] =  (ReadNative.this).getClass();
		    // ctor_arg_types[1] = int.class;

		    Constructor srcon = srclass.getConstructor( ctor_arg_types );
		    
		    System.out.println("correct c'tor found");

		    Object[] ctor_args = new Object[1];
		    ctor_args[0] = ReadNative.this;
		    //ctor_args[1] = new Integer(0);  // how to represent an int??

		    sr = (SAXRead) (srcons[0].newInstance(ctor_args));
		    

		    System.out.println("creating object....");
		    */

		    // need to make sure we use the CustomClassLoader.....
		    // (which is what the above code does)

		    try
		    {
			sr = new SAXRead( lm );
		    }
		    catch( java.lang.NoClassDefFoundError e )
		    {
		    	pm.stopIt();
		    	mview.alertMessage("Unable to instantiate the SaxReader class, no class definition could be found.");
		    }

		}
		/*
		catch (InstantiationException e)
		{
		    System.out.println(error_mesg + e);
		} 
		catch (IllegalAccessException e) 
		{
		    System.out.println(error_mesg + e);
		} 
		catch (IllegalArgumentException e)
		{
		    System.out.println(error_mesg + e);
		} 
		catch (InvocationTargetException e) 
		{
		    System.out.println(error_mesg + e.getTargetException().toString());
		}
		catch (java.lang.NoSuchMethodException e)
		{
		    System.out.println(error_mesg + e);
		} 
		*/
		catch (Exception e)
		{
		    pm.stopIt();
		    System.out.println(error_mesg);
		    e.printStackTrace();
		}

		if(sr != null)
		    sr.doLoad( fn, validate);


	    }
	    else
	    {
		pm.stopIt();
		mview.alertMessage("File '" + fn + "' not found");
	    }

	    if(dn != null)
		dn.signal();
	}

/*
	public void OLD_run()
	{
	    String  parserName = "org.apache.xerces.parsers.SAXParser";
	    boolean validate = false;

	    final String error_mesg = "whilst creating SAX parser: \n";

	    // make sure the file exists!
	    File test = new File(fn);
	    SAXRead sr = null;
	    if(test.exists())
	    {
		try 
		{
		    sr = new SAXRead(lm);

		}
		catch (Exception e)
		{
		    pm.stopIt();
		    System.out.println(error_mesg);
		    e.printStackTrace();
		}
		
		if(sr != null)
		    sr.doLoad( fn, validate);
		
	    }
	    else
	    {
		pm.stopIt();
		mview.alertMessage("File '" + fn + "' not found");
	    }
	}
*/

	private int lm;    // load mode
	private String fn; // file name
	private CommandSignal dn;
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   parser
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    /*
     * The Apache Software License, Version 1.1
     *
     *
     * Copyright (c) 1999 The Apache Software Foundation.  All rights 
     * reserved.
     *
     * Redistribution and use in source and binary forms, with or without
     * modification, are permitted provided that the following conditions
     * are met:
     *
     * 1. Redistributions of source code must retain the above copyright
     *    notice, this list of conditions and the following disclaimer. 
     *
     * 2. Redistributions in binary form must reproduce the above copyright
     *    notice, this list of conditions and the following disclaimer in
     *    the documentation and/or other materials provided with the
     *    distribution.
     *
     * 3. The end-user documentation included with the redistribution,
     *    if any, must include the following acknowledgment:  
     *       "This product includes software developed by the
     *        Apache Software Foundation (http://www.apache.org/)."
     *    Alternately, this acknowledgment may appear in the software itself,
     *    if and wherever such third-party acknowledgments normally appear.
     *
     * 4. The names "Xerces" and "Apache Software Foundation" must
     *    not be used to endorse or promote products derived from this
     *    software without prior written permission. For written 
     *    permission, please contact apache@apache.org.
     *
     * 5. Products derived from this software may not be called "Apache",
     *    nor may "Apache" appear in their name, without prior written
     *    permission of the Apache Software Foundation.
     *
     * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
     * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
     * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
     * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
     * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
     * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
     * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
     * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
     * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
     * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
     * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
     * SUCH DAMAGE.
     * ====================================================================
     *
     * This software consists of voluntary contributions made by many
     * individuals on behalf of the Apache Software Foundation and was
     * originally based on software copyright (c) 1999, International
     * Business Machines, Inc., http://www.apache.org.  For more
     * information on the Apache Software Foundation, please see
     * <http://www.apache.org/>.
     */
    
    public class SAXRead extends HandlerBase 
    {
	
	/** Elements. */
	private long elements;
	
	/** Attributes. */
	private long attributes;

	/** Characters. */
	private long characters;
	
	/** Ignorable whitespace. */
	private long ignorableWhitespace;
	
	private long spots_l;
	private int spots;
	private long probes_l;
	private int probes;
	private long genes_l;
	private int genes;

	private int measurements;

	private Vector spot_name_v = null;
	private Vector spot_comment_v = null;
	//private Vector spot_probe_v = null;
	private Vector probe_name_v = null;
	private Vector measurement_v = null;

	private Hashtable gene_name_ht = null;

	private Hashtable spot_name_to_probe_name_ht = null;

	private boolean in_data_block = false;
	private boolean in_spot_attrs = false;

	private int values_in_this_data_block;

	private String cur_spot_attr_name = null;
	private String cur_spot_attr_unit = null;
	private String cur_spot_attr_type_str = null;
	private int    cur_data_chunk_type = -1;
	//private DataPlot.ColourRamp current_cr =  null;

	private String cur_probe_name = null;

	private String cur_ordering_name = null;
	private int[] spot_ordering = null;
	private int[] meas_ordering = null;
	
	private String cur_selection_name = null;
	private int[] spot_selection = null;
	private int[] meas_selection = null;

		
	private ExprData.Cluster cur_clust;
	private ExprData.Cluster new_root;
	private int n_clusters;
	private Vector cur_clust_elements = null;
	private int cur_clust_name_mode;

	private double[] data_block_double_values = null;
	private int[]    data_block_int_values = null;
	private String[] data_block_text_values = null;
	private char[]   data_block_char_values = null;

	private ExprData.Measurement cur_measurement = null;
	private ExprData.DataTags data_tags = null;

	private final int IntDataType    = 0;
	private final int DoubleDataType = 1;
	private final int CharDataType   = 2;
	private final int TextDataType = 3;
	
	// the following flags are used for reporting progress
	private boolean in_genes = false;
	private boolean in_probes = false;
	private boolean in_spots = false;
	private boolean in_measurements = false;
	private boolean in_clusters = false;
	private boolean in_ordering = false;
	private boolean in_selection = false;

	private int load_mode;

	// temporary store for Measurements if using MergeMode
	private Vector new_meas_v = null;


	public SAXRead()
	{
	    load_mode = 0;
	}

	public SAXRead(int load_mode_)
	{
	    load_mode = load_mode_;
	}
	
       
	public void doLoad( String uri, boolean validate ) 
	{
	    
	    try 
	    {
		if( loadSAXParser() == false )
		    return;
		

		long before = System.currentTimeMillis();

		//Class classDefinition = Class.forName("org.apache.xerces.parsers.SAXParser");

		//System.out.println("doLoad(): class name is good....");

		//Parser parser = new org.apache.xerces.parsers.SAXParser(); //  org.xml.sax.helpers.ParserFactory.makeParser(parserName);

		//System.out.println( "doLoad(): parser object is " + parser );

		if( parser == null )
		{
		    mview.alertMessage( "Unable to instantiate the SAX Parser needed to read the file." );
		    return;
		}
		
		
		parser.setDocumentHandler(this);
		parser.setErrorHandler(this);

		/*
		try 
		{
		    if (validate && parser instanceof Configurable)
			((Configurable)parser).setFeature("http://xml.org/sax/features/validation", true);
		} 
		catch(Exception ex) {}
		*/

		// check for a .gz extension...
		int ext_pos = uri.lastIndexOf('.');
		boolean compressed = false;
		if(ext_pos > 0)
		{
		    String ext = uri.substring(ext_pos).toLowerCase();
		    if(ext.equals(".gz") || ext.equals(".zip"))
		    {
			compressed = true;
		    }
		}

		InputStreamReader isr = null;
		File file = new File(uri);
		FileInputStream fis = new FileInputStream(file);
		BufferedInputStream bis = new BufferedInputStream(fis);

		if(compressed)
		{
		    GZIPInputStream gis = new GZIPInputStream(bis);
		    parser.parse(new InputSource(gis));
		}
		else
		{
		    parser.parse(new InputSource(bis));
		}

		
		if(load_mode == MergeMode)
		{
		    ExprData.Measurement[] new_data = (ExprData.Measurement[]) (new_meas_v.toArray(new ExprData.Measurement[0]));
		    
		    if(new_data.length > 0)
		    {
			if(edata.mergeMeasurements( new_data ) == false)
			{
			    System.out.println("merge cancelled....");
			    
			    pm.stopIt();

			    // the merge has been cancelled
			    return;
			}
		    }

		    pm.stopIt();
		}
		
		if(load_mode != MergeMode)
		{
			// install the traversal orderings if they were found
			
			if(spot_ordering != null)
			{
			    edata.setSpotOrder(spot_ordering);
			    
			    //System.out.println("found spot order, length= " + spot_ordering.length);
			}
			
			if(meas_ordering != null)
			{
			    edata.setMeasurementOrder(meas_ordering);
			    
			    //System.out.println("found meas order, length= " + meas_ordering.length);
			}
		}
		
		if(new_root != null)
		{
		    // don't add the root itself, but do add each of it's children
		    
		    Vector ch = new_root.getChildren();
		    if(ch != null)
			for(int c=0; c < ch.size(); c++)
			    edata.addCluster((ExprData.Cluster) ch.elementAt(c));
		}
		

		
		
		pm.stopIt();

		long after = (System.currentTimeMillis() - before) / 1000;
		
		mview.setMessage("Read Native: loaded in " + mview.niceTime(after));

		//printResults(uri, after - before);
		if(load_mode != MergeMode)
		{
		    // when we do this, the ISYS data packager will be invoked.
		    // want to do so after the load status message has been displayed.
		    if((spot_selection != null) && (spot_selection.length > 0))
			edata.setSpotSelection(spot_selection);
		    
		    if((meas_selection != null) && (meas_selection.length > 0))
			edata.setMeasurementSelection(meas_selection);
		}

	    }
	    catch (java.lang.NoClassDefFoundError ncdfe) 
	    {
		pm.stopIt();
		ncdfe.printStackTrace();
	    }
	    catch (org.xml.sax.SAXParseException spe) 
	    {
		pm.stopIt();
		spe.printStackTrace(System.err);
	    }
	    catch (org.xml.sax.SAXException se) 
	    {
		pm.stopIt();
		if (se.getException() != null)
		    se.getException().printStackTrace(System.err);
		else
		    se.printStackTrace(System.err);
	    }
	    catch (Exception e) 
	    {
		pm.stopIt();
		e.printStackTrace(System.err);
	    }
	}
	
	//
	// DocumentHandler methods
	//
	
	/** Start document. */
	public void startDocument() 
	{
	    elements            = 0;
	    attributes          = 0;
	    characters          = 0;
	    ignorableWhitespace = 0;

	    in_data_block = false;
	    in_spot_attrs = false;

	    measurements = 0;
	    measurement_v = new Vector();

	    gene_name_ht = new Hashtable();

	    cur_measurement = null;

	    new_root = null;
	    n_clusters = 0;
	    cur_clust_elements = new Vector();
	    
	    if(load_mode == MergeMode)
		new_meas_v = new Vector();

	    // only report errors once (rather than 20,000 times...)
	    file_has_errors = false;
	}
	
	/** Start element. */
	public void startElement(String name, AttributeList attrs) 
	{
	    elements++;
	    if (attrs != null) 
	    {
		attributes += attrs.getLength();
	    }
	    if(name.equals("Probe"))
	    {
		if(in_clusters)
		{
		    cur_clust_name_mode = ExprData.ProbeName;
		    cur_clust_elements.addElement(attrs.getValue(0));
		}
		else
		{
		    if(!in_probes && !in_genes)
		    {
			in_probes = true;
			pm.setMessage(1, "Probes");
		    }
		    probes_l++;
		    cur_probe_name = attrs.getValue("NAME");

		    // --- update TagAttrs ---
		    final ExprData.TagAttrs pta = edata.getProbeTagAttrs();
		    final String pname = attrs.getValue("NAME");

		    updateTagAttrs( pname, pta, attrs, "" );

		    /*
		    for(int p_att=0; p_att < attrs.getLength(); p_att++)
		    {
			String key = attrs.getName(p_att);
			if(!key.equals("NAME"))
			    pta.setTagAttr( pname, attrs.getName(p_att), attrs.getValue(p_att) );
		    }
		    */

		    // -----------------------

		    probe_name_v.addElement( pname );  // the 0th attr is the name
		    
		}
		return;
	    }

	    if(name.equals("Gene"))
	    {
		if(in_clusters)
		{
		    cur_clust_name_mode = ExprData.GeneName;
		    cur_clust_elements.addElement(attrs.getValue(0));
		}
		else
		{
		    if(!in_genes)
		    {
			in_genes = true;
			pm.setMessage(1, "Probes and Genes");
		    }
		    
		    // --- update TagAttrs ---
		    final ExprData.TagAttrs gta = edata.getGeneTagAttrs();
		    final String gname = attrs.getValue("NAME");
		    
		    updateTagAttrs( gname, gta, attrs, "" );

		    /*
		    for(int g_att=0; g_att < attrs.getLength(); g_att++)
		    {
			String key = attrs.getName(g_att);
			if(!key.equals("NAME"))
			    gta.setTagAttr( gname, attrs.getName(g_att), attrs.getValue(g_att) );
		    }
		    */

		    // -----------------------
		    
		    genes_l++;

		    // attempt to retrieve the list of gene names linked to the current probe name
		    Vector gnv = (Vector) gene_name_ht.get(cur_probe_name);
		    if(gnv == null)
		    {
			// this is the first gene name linked to the current probe name
			gnv = new Vector();
			gnv.addElement( gname );
			gene_name_ht.put(cur_probe_name, gnv);
		    }
		    else
		    {
		    // add this gene name to this list of existing gene names for the current probe name 
			gnv.addElement( gname );
		    }
		}
		return;
	    }

	    if(name.equals("Spot"))
	    {
		if(in_clusters)
		{
		    // is it an index or a name?
		    if(attrs.getName(0).equals("NAME"))
		    {
			cur_clust_name_mode = ExprData.SpotName;
			cur_clust_elements.addElement(attrs.getValue(0));
		    }
		    else
		    {
			cur_clust_name_mode = ExprData.SpotIndex;
			Integer sii = new Integer(attrs.getValue(0));
			cur_clust_elements.addElement(sii);
		    }

		    
		    // System.out.println( attrs.getValue(0) + " added to " + cur_clust.getName() );
		}
		else
		{
		    if(!in_spots)
		    {
			in_spots = true;
			pm.setMessage(1, "Spots");
		    }
		    
		    // --- update TagAttrs ---
		    final ExprData.TagAttrs sta = edata.getSpotTagAttrs();
		    final String sname = attrs.getValue("NAME");
		    
		    updateTagAttrs( sname, sta, attrs, "PROBE" );

		    // -----------------------

		    spots_l++;
		    spot_name_v.addElement( attrs.getValue(0) );  // the 0th attr is the name
		    
		    // the 1th attr is the probe
		    spot_name_to_probe_name_ht.put(attrs.getValue(0), attrs.getValue(1) );

		    

		}
		return;

	    }

	    if(name.equals("Cluster"))
	    {
		if(!in_clusters)
		{
		    in_clusters = true;
		    pm.setMessage(1, load_clusters ? "Clusters" : "ignoring Clusters");
		}

		if(load_clusters)
		{
		    // attrs: NAME COLOUR GLYPH SIZE 

		    // save any existing elements
		    if((cur_clust != null) && (cur_clust_elements.size() > 0))
		    {
			cur_clust.setElements(cur_clust_name_mode, cur_clust_elements);
		    }

		    if(new_root == null)
		    {
			new_root = cur_clust = edata.new Cluster("tmp-root"); // temporary root that will be thrown away later
		    }

		    ExprData.Cluster new_child = edata.new Cluster( attrs.getValue(0) );
		    
		    /*
		    if(new_child_elements.size() > 0)
		    {
			new_child.setElements(cur_clust_name_mode, cur_clust_elements);
			new_child_elements = new Vector();
		    }
		    */
		    cur_clust_elements = new Vector();
		    
		    cur_clust.addCluster(new_child);
		    cur_clust = new_child;
		    
		    int ci = new Integer(attrs.getValue(1)).intValue();
		    Color c = new Color(ci);
		    
		    cur_clust.setColour(c);
		    
		    int gi = new Integer(attrs.getValue(2)).intValue();
		    
		    cur_clust.setGlyph(gi);
		    
		    cur_clust_name_mode = -1;

		    int si = new Integer(attrs.getValue(3)).intValue();

		    String sh = attrs.getValue(4);
		    if((sh != null) && (sh.equals("FALSE")))
		    {
			cur_clust.setShow(false);
		    }

		    n_clusters++;
		}
		return;
	    }

	    if(name.equals("DataBlock"))
	    {
		//System.out.println("new datablock starting, name is " + attrs.getValue(0) );
		data_block_double_values = new double[spots];
		values_in_this_data_block = 0;
		return;
	    }

	    if(name.equals("DataChunk"))
	    {
		//System.out.println("new datachunk starting, start index is " + attrs.getValue(0) );
		in_data_block = true;
		data_block_sbuf = new StringBuffer();
		return;
	    }

	    if(name.equals("Measurement"))
	    {
		if(in_clusters)
		{
		    // is it an index or a name?
		    if(attrs.getName(0).equals("NAME"))
		    {
			cur_clust_name_mode = ExprData.MeasurementName;
			cur_clust_elements.addElement(attrs.getValue(0));
		    }
		    else
		    {
			cur_clust_name_mode = ExprData.MeasurementIndex;
			Integer sii = new Integer(attrs.getValue(0));
			cur_clust_elements.addElement(sii);
		    }
		}
		else
		{
		    if(!in_measurements)
		    {
			in_measurements = true;
			pm.setMessage(2, load_measurements ? 
				      (spots_l + " Spots, " + probes_l + " Probes") : "ignore Measurements");
		    }
		    
		    if(load_measurements)
		    {
			
			cur_measurement = edata.new Measurement();
			cur_measurement.setName(attrs.getValue(0));
			
			pm.setMessage(1, "Measurement: " + attrs.getValue(0));
			
			String mtype = attrs.getValue("DATATYPE");
			if(mtype == null)
			{
			    cur_measurement.setDataType(ExprData.UnknownDataType);
			}
			else
			{
			    cur_measurement.setDataTypeString(mtype);
			}
			
			// we expect the first data chunk to be double's for
			// the Expression Levels
			//
			cur_data_chunk_type = DoubleDataType;
			
			/*
			  for(int a=0; a < attrs.getLength(); a++)
			  {
			  System.out.println(attrs.getName(a) + "..." + attrs.getValue(a));
			  }
			*/
			
			measurements++;
		    }
		}
		return;
	    }

	    if(name.equals("Attribute") && load_measurements)
	    // this is an attribute of the current measurement
	    {
		//System.out.println("adding Attribute to " + cur_measurement.getName() + " ...");
		if( attrs.getLength() > 2 )
		    cur_measurement.setAttribute( attrs.getValue(0),                      // name
						  attrs.getValue(2), attrs.getValue(1),   // source, value
						  attrs.getValue(3), attrs.getValue(4) ); // created, modified
		else
		    cur_measurement.setAttribute( attrs.getValue(0), "(unknown)", attrs.getValue(1),
						  "(unknown)", "(unknown)");   // created, modified not known
		return;
	    }

	    if(name.equals("SpotAttribute"))
	    {
		in_spot_attrs = true;
		
		if(load_spot_attrs)
		{
		    
		    //System.out.println("adding SpotAttribute to " + cur_measurement.getName() + " ...");
		    
		    cur_spot_attr_name     = attrs.getValue(0);
		    cur_spot_attr_unit     = attrs.getValue(1);
		    cur_spot_attr_type_str = attrs.getValue(2);
		    
		    pm.setMessage(1, "SpotAttribute: " + cur_spot_attr_name);
		    
		    /*
		      System.out.println("  expecting " + spots + " '" + cur_spot_attr_type_str + "'s for " + cur_spot_attr_name +
		      " (unit: " + cur_spot_attr_unit + ")");
		    */
		    
		    if(cur_spot_attr_type_str.equals("INTEGER"))
		    {
			cur_data_chunk_type = IntDataType;
			data_block_int_values = new int[spots];
		    }
		    else
		    {
			if(cur_spot_attr_type_str.equals("DOUBLE"))
			{
			    cur_data_chunk_type = DoubleDataType;
			    data_block_double_values = new double[spots];
			}
			else
			{
			    if(cur_spot_attr_type_str.equals("CHAR"))
			    {
				cur_data_chunk_type = CharDataType;
				data_block_char_values = new char[spots];
			    }
			    else
			    {
				if(cur_spot_attr_type_str.equals("TEXT"))
				{
				    cur_data_chunk_type = TextDataType;
				    data_block_text_values = new String[spots];
				}
				else
				{
				    mview.alertMessage("Unrecognised DataType '" + cur_spot_attr_type_str + "' in SpotAttributes");
				    cur_data_chunk_type = -1;
				}
			    }
			}
		    }
		}
		
		return;
	    }

	    if(name.equals("ArrayType"))
	    {
		pm.setMessage(1, "ArrayType");

		spot_name_v = new Vector();
		spot_name_to_probe_name_ht = new Hashtable();
		probe_name_v = new Vector();
		spots = 0;
		probes = 0;
		return;
	    }

	    if(name.equals("Colouriser") && (load_colours))
	    {
		// build a hashtable to pass the DataPlot which will dispatch it
		// any known Colouriser classes
		//
		
		Hashtable attrs_ht = new Hashtable();
		for(int a=0; a < attrs.getLength(); a++)
		{
		    attrs_ht.put(attrs.getName(a), attrs.getValue(a));
		}
		dplot.addColouriser(attrs_ht);
	    }

	    if(name.equals("ColouriserUser") && (load_colours))
	    {
		// assume the Measurement and Colouriser names are valid by now...

		String mname = (String) attrs.getValue(0);
		String cname = (String) attrs.getValue(1);
		
		dplot.setColouriserForMeasurement(mname, cname);
	    }

	    if(name.equals("DisplayOrder") && (load_layout))
	    {
		// the next data block is an ordering....
		try
		{
		    int things = new Integer(attrs.getValue(1)).intValue();
		    cur_ordering_name = attrs.getValue(0);
		    data_block_int_values = new int[things];
		    cur_data_chunk_type = IntDataType;
		    in_ordering = true;

		    // System.out.println("loading order...name=" + cur_ordering_name + " size=" + things);
		}
		catch(NumberFormatException nfe)
		{
		}
	    }

	    if(name.equals("Selection") && (load_layout))
	    {
		// the next data block is the spot or measurement selection....
		try
		{
		    int things = new Integer(attrs.getValue(1)).intValue();
		    cur_selection_name = attrs.getValue(0);
		    data_block_int_values = new int[things];
		    cur_data_chunk_type = IntDataType;
		    in_selection = true;

		    // System.out.println("loading order...name=" + cur_ordering_name + " size=" + things);
		}
		catch(NumberFormatException nfe)
		{
		}
	    }

	    if(name.equals("DisplayZoom") && (load_layout))
	    {
		int z = new Integer(attrs.getValue(0)).intValue();
		dplot.setZoom(z);
		return;
	    }

	    if(name.equals("ClusterDisplay") && (load_layout))
	    {
		try
		{
		    int bs = new Integer(attrs.getValue(0)).intValue();
		    boolean orc = new Boolean(attrs.getValue(1)).booleanValue();
		    boolean sb = new Boolean(attrs.getValue(2)).booleanValue();
		    boolean sg = new Boolean(attrs.getValue(3)).booleanValue();
		    boolean ag = new Boolean(attrs.getValue(4)).booleanValue();
		    
		    dplot.setClusterLayout(0, sg, sb, orc, ag, bs);
		    dplot.setClusterLayout(1, sg, sb, orc, ag, bs);

		    /*
		    dplot.setBranchScale(0,bs);
		    dplot.setOverlayRootChildren(0,orc);
		    dplot.setShowBranches(0,sb);
		    dplot.setShowGlyphs(0,sg);
		    dplot.setAlignGlyphs(0,ag);

		    dplot.setBranchScale(1,bs);
		    dplot.setOverlayRootChildren(1,orc);
		    dplot.setShowBranches(1,sb);
		    dplot.setShowGlyphs(1,sg);
		    dplot.setAlignGlyphs(1,ag);
		    */
		}
		catch(NullPointerException npe)
		{
		}
		return;
	    }

	    if(name.equals("SpotClusterDisplay") && (load_layout))
	    {
		try
		{
		    int bs = new Integer(attrs.getValue(0)).intValue();
		    boolean orc = new Boolean(attrs.getValue(1)).booleanValue();
		    boolean sb = new Boolean(attrs.getValue(2)).booleanValue();
		    boolean sg = new Boolean(attrs.getValue(3)).booleanValue();
		    boolean ag = new Boolean(attrs.getValue(4)).booleanValue();
		    
		    dplot.setClusterLayout(0, sg, sb, orc, ag, bs);
		    
		}
		catch(NullPointerException npe)
		{
		}
		return;
	    }

	    if(name.equals("MeasurementClusterDisplay") && (load_layout))
	    {
		try
		{
		    int bs = new Integer(attrs.getValue(0)).intValue();
		    boolean orc = new Boolean(attrs.getValue(1)).booleanValue();
		    boolean sb = new Boolean(attrs.getValue(2)).booleanValue();
		    boolean sg = new Boolean(attrs.getValue(3)).booleanValue();
		    boolean ag = new Boolean(attrs.getValue(4)).booleanValue();
		    
		    dplot.setClusterLayout(1, sg, sb, orc, ag, bs);
		    
		}
		catch(NullPointerException npe)
		{
		}
		return;
	    }

	    if(name.equals("BoxGeometry") && (load_layout))
	    {
		int bw = new Integer(attrs.getValue(0)).intValue();
		int bh = new Integer(attrs.getValue(1)).intValue();
		int cg = new Integer(attrs.getValue(2)).intValue();
		int rg = new Integer(attrs.getValue(3)).intValue();
		int bg = new Integer(attrs.getValue(4)).intValue();

		dplot.setBoxGeometry(bw,bh,cg,rg);
		dplot.setBorderGap(bg);
		return;
	    }
	    //if(name.equals("DisplayOptions") && (load_layout))
	    //{
		//int rls = new Integer(attrs.getValue(0)).intValue();
		//int rla = new Integer(attrs.getValue(1)).intValue();
		
		//dplot.setRowLabelSource(rls);
		//dplot.setRowLabelAlign(rla);

		//return;
	     //}
	    if(name.equals("Font") && (load_layout))
	    {
		String fname = attrs.getValue(0);
		
		int fsize   = attrs.getLength() > 1 ? new Integer(attrs.getValue(1)).intValue() : 12;
		int ffam    = attrs.getLength() > 2 ? new Integer(attrs.getValue(2)).intValue() : 0;
		int fsty    = attrs.getLength() > 3 ? new Integer(attrs.getValue(3)).intValue() : 0;
		boolean faa = attrs.getLength() > 4 ? new Boolean(attrs.getValue(4)).booleanValue() : true;
		
		if(fname.equals("DEFAULT") || fname.equals("SPOT"))
		{
		    dplot.setSpotFontFamily(ffam);
		    dplot.setSpotFontSize(fsize);
		    dplot.setSpotFontStyle(fsty);
		    dplot.setSpotFontAntialiasing(faa);
		    return;
		}
		if(fname.equals("MEASUREMENT"))
		{
		    dplot.setMeasurementFontFamily(ffam);
		    dplot.setMeasurementFontSize(fsize);
		    dplot.setMeasurementFontStyle(fsty);
		    dplot.setMeasurementFontAntialiasing(faa);
		    return;
		}
	    }

	    if(name.equals("MeasurementFont") && (load_layout))
	    {
		String fname = attrs.getValue(0);
		
		int fsize = attrs.getLength() > 1 ? new Integer(attrs.getValue(1)).intValue() : 12;
		int ffam  = attrs.getLength() > 2 ? new Integer(attrs.getValue(2)).intValue() : 0;
		int fsty  = attrs.getLength() > 3 ? new Integer(attrs.getValue(3)).intValue() : 0;

		dplot.setMeasurementFontFamily(ffam);
		dplot.setMeasurementFontSize(fsize);
		dplot.setMeasurementFontStyle(fsty);
		return;
	    }

	    if(name.equals("NameColumn") && (load_layout))
	    {
		dplot.addNameCol();
		int c = dplot.getNumNameCols() - 1;
		
		try
		{
		    dplot.getNameColSelection(c).setNames( attrs.getValue(0) );
		    dplot.setNameColTrimEnabled(c, new Boolean(attrs.getValue(1)).booleanValue() ); 
		    dplot.setNameColTrimLength(c,  new Integer(attrs.getValue(2)).intValue());
		    dplot.setNameColAlign(c,       new Integer(attrs.getValue(3)).intValue());
		}
		catch(NumberFormatException nfe)
		{
		}

		return;
	    }

	    if(name.equals("<MeasurementLabelDisplay") && (load_layout))
	    {
		int align = attrs.getLength() > 1 ? new Integer(attrs.getValue(0)).intValue() : 1;
		dplot.setMeasurementLabelAlign( align );
	    }
	    
	    if(name.equals("Environment") && (load_app_props))
	    {
		pm.setMessage(1, "maxdView environment");
	    }
	} 
	
	// -------------------------------------------------------------------
	// -------------------------------------------------------------------

	private void updateTagAttrs( String name, ExprData.TagAttrs ta, AttributeList attrs, String avoid)
	{
	    for(int s_att=0; s_att < attrs.getLength(); s_att++)
	    {
		String key = attrs.getName(s_att);
		if((!key.equals("NAME")) && (!key.equals(avoid)))
		{
		    if(key.startsWith("_ATTR"))
		    {
				
			// new (post 0.8.7) format
			//
			// used to avoid avoid problems with identifers 
                        // that contain spaces or HTML chars
			//
			//  .... _ATTR0="name1" _VAL0="value1"  _ATTR1="name2" _VAL1="value2" ...
			//

			try
			{
			    String attr_name = attrs.getValue(s_att);
			    String attr_val  = attrs.getValue( "_VAL" +  key.substring(5) );
			    ta.setTagAttr( name, attr_name, attr_val );
			}
			catch( NumberFormatException nfe)
			{
			    
			}
		    }
		    else
		    {
			// old (pre 0.8.8) format
			//
			//   ... NAME1="value1" NAME2="value2" ...
			if(!key.startsWith("_ATTR") && !key.startsWith("_VAL"))
			    ta.setTagAttr( name, attrs.getName(s_att), attrs.getValue(s_att) );
		    }
		}
	    }
	}
	
	
	// -------------------------------------------------------------------
	// -------------------------------------------------------------------
	// -------------------------------------------------------------------

	public void endElement(java.lang.String name) throws SAXException
	{
	    if(name.equals("DataChunk"))
	    {
		in_data_block = false;

		// attempt to parse the numbers....
		
		StringReader sr = new StringReader(data_block_sbuf.toString());

		//System.out.println(data_block_sbuf.toString().length() + " chars in buffer");
		
		StreamTokenizer st = new StreamTokenizer(sr);
		st.resetSyntax();
		st.wordChars('!','~');  // this covers all printable ASCII chars

		boolean done = false;

		int insert_p = values_in_this_data_block;

		try
		{
		    while(!done)
		    {
			int result = st.nextToken();
			
			if(result == java.io.StreamTokenizer.TT_WORD)
			{
			    switch(cur_data_chunk_type)
			    {
			    case  DoubleDataType:
				try
				{
				    data_block_double_values[insert_p++] = NumberParser.tokenToDouble(st.sval);
				}
				catch(TokenIsNotNumber tinn)
				{
				    if(!file_has_errors)
				    {
					System.out.println("FILE BAD!! (expecting DOUBLE, found '" + st.sval + "')");
					if(cur_measurement != null)
					    System.out.println("  in Measurement '" + cur_measurement.getName() + "'");
					file_has_errors = true;
				    }
				}
				break;

			    case IntDataType:
				try
				{
				    data_block_int_values[insert_p++] = (new Integer(st.sval)).intValue();
				}
				catch(NumberFormatException nfe)
				{
				    if(!file_has_errors)
				    {
					System.out.println("FILE BAD!! (expecting INTEGER, found '" + st.sval + "')");
					if(cur_measurement != null)
					    System.out.println("  in Measurement '" + cur_measurement.getName() + "'");
					file_has_errors = true;
				    }
				    
				}
				break;

			    case CharDataType:
				if( st.sval.equals("\\0") ) // special encoding for blank
				    data_block_char_values[insert_p++] = '\0';
				else
				    data_block_char_values[insert_p++] = st.sval.charAt(0);
				break;

			    case TextDataType:
				if( st.sval.equals("\\0") ) // special encoding for blank
				    data_block_text_values[insert_p++] = null;
				else
				    data_block_text_values[insert_p++] = new String(st.sval);
				break;

			    }
			}
			if(result == java.io.StreamTokenizer.TT_EOF)
			    done = true;
		    }
		    
		}
		catch (IOException e) 
		{ 
		    mview.alertMessage("File broken");
		} 
		
		values_in_this_data_block += (insert_p - values_in_this_data_block);

		//System.out.println("found " + p + " values");
		return;
	    }     // end if(name.equals("DataBlock"))

	    if(name.equals("Cluster") && (load_clusters))
	    {
		// end of cluster....

		if(cur_clust_elements.size() > 0)
		{
		    cur_clust.setElements(cur_clust_name_mode, cur_clust_elements);
		    cur_clust_elements = new Vector();
		}

		// return to the parent ready for the next sibling (if any)
		
		ExprData.Cluster parent = cur_clust.getParent();
		if(parent != null)
		    cur_clust = parent;
		
		// and reset the vector for next time (if any)
		
	    }

	    if(name.equals("Measurement") && (load_measurements))
	    {
		if(in_clusters)
		{
		    // handled at tag start 
		}
		else
		{
		    if(cur_measurement != null)
		    {
			// System.out.println(cur_measurement.getName() + " (#" + measurements + ") is finished");
			
			cur_measurement.setData(data_block_double_values);
			cur_measurement.setDataTags(data_tags);
			
			if(load_mode == ReplaceMode)
			{
			    if(measurements == 1)
			    {
				edata.addMeasurement(cur_measurement);
			    }
			    else
			    {
				edata.addOrderedMeasurement(cur_measurement);
			    }
			}
			else
			{
			    new_meas_v.addElement(cur_measurement);
			}
		    }
		}
		return;
	    }		   

	    if(name.equals("SpotAttribute") && (load_spot_attrs)) // this is an attribute of the current measurement
	    {
		if(cur_measurement != null)
		{
		    Object data_block_values = null;

		    switch(cur_data_chunk_type)
		    {
		    case IntDataType:
			data_block_values = (Object) data_block_int_values;
			break;
		    case DoubleDataType:
			data_block_values = (Object) data_block_double_values;
			break;
		    case CharDataType:
			data_block_values = (Object) data_block_char_values;
			break;
		    case TextDataType:
			data_block_values = (Object) data_block_text_values;
			break;
		    }
		    cur_measurement.addSpotAttribute(cur_spot_attr_name, 
						     cur_spot_attr_unit, 
						     cur_spot_attr_type_str, 
						     data_block_values);

		}

		// reset the data block chunk type to be ready for the expression levels
		// which might be the next data block (unless another SpotAttrbiute occurs,
		// but that will specify it's data type anyhow....)
		//
		cur_data_chunk_type = DoubleDataType;
		
		in_spot_attrs = false;
		return;
	    }

	    if(name.equals("DisplayOrder") && (load_layout))
	    {
		// the int data block data is loaded....
		if(in_ordering)
		{
		    if(cur_ordering_name.equals("SPOT"))
		    {
			spot_ordering = (int[]) data_block_int_values.clone();
		    }
		    else
		    {
			meas_ordering = (int[]) data_block_int_values.clone();
		    }
		    in_ordering = false;
		}
	    }

	    if(name.equals("Selection") && (load_layout))
	    {
		// the int data block data is loaded....
		if(in_selection)
		{
		    if(cur_selection_name.equals("SPOT"))
		    {
			spot_selection = (int[]) data_block_int_values.clone();
		    }
		    else
		    {
			meas_selection = (int[]) data_block_int_values.clone();
		    }
		    in_selection = false;
		}
	    }

	    if(name.equals("ArrayType") && ((load_spot_attrs) || load_measurements))
	    {
		if(spots_l > Integer.MAX_VALUE)
		{
		    mview.alertMessage("Too many Spots.");
		}
		else
		{
		    probes = (int) probes_l;
		    spots = (int) spots_l;
		    genes = (int) genes_l;

		    String[]   s_names = new String[(int) spots];
		    String[]   p_names = new String[(int) spots];
		    String[][] g_names = new String[(int) spots][];

		    for(int s=0; s < spots; s++)
		    {
			s_names[s] = (String) spot_name_v.elementAt(s);
			p_names[s] = (String) spot_name_to_probe_name_ht.get( s_names[s] );

			Vector gnv = (Vector) gene_name_ht.get(p_names[s]);
			if(gnv != null)
			{
			    g_names[s] = new String[gnv.size()];
			    for(int gn=0; gn < gnv.size(); gn++)
				g_names[s][gn] = (String) gnv.elementAt(gn);
			}
			else
			{
			    g_names[s] = null;
			}
		    }
		    
		    data_tags = edata.new DataTags( s_names, p_names, g_names );
		}
		return;
	    }
	}

	/** Characters. */
	StringBuffer data_block_sbuf = null;
	//StringBuffer cluster_data_sbuf = null;

	public void characters(char ch[], int start, int length) 
	{

	    characters += length;

	    if(in_data_block)
	    {
		//System.out.println(length + " untagged characters read in DataBlock");
		//System.out.println("buffer: length=" + ch.length + " chunk: start=" + start + " length=" + length);

		// TODO:: this is a bit crap.....
		//
		data_block_sbuf.append(ch, start, length);

		//for(int c=0; c < length; c++)
		//    data_block_sbuf.append(ch[start+c]);
	    }
	    /*
	      else
	      {
		if(in_clusters)
		{
		    cluster_data_sbuf.append(ch, start, length);
		}
	    }
	    */
	} // characters(char[],int,int);
	
	/** Ignorable whitespace. */
	public void ignorableWhitespace(char ch[], int start, int length) 
	{
	    ignorableWhitespace += length;
	} // ignorableWhitespace(char[],int,int);
	
	//
	// ErrorHandler methods
	//
	
	/** Warning. */
	public void warning(SAXParseException ex) 
	{
	    System.err.println("[Warning] "+
			       getLocationString(ex)+": "+
			       ex.getMessage());
	}
	
	/** Error. */
	public void error(SAXParseException ex) 
	{
	    pm.stopIt();
	    mview.errorMessage("Error! " + ex.getMessage() + "\n" + 
			       "Line: " + ex.getLineNumber() + ", Column:" + ex.getColumnNumber() + "\n" + 
			       "Is this really a native file?");

	    //System.err.println("[Error] "+
	    //		       getLocationString(ex)+": "+
	    //		       ex.getMessage());
	}
	
	/** Fatal error. */
	public void fatalError(SAXParseException ex) throws SAXException 
	{
	    pm.stopIt();
	    mview.errorMessage("Error! " + ex.getMessage() + "\n" + 
			       "Line: " + ex.getLineNumber() + ", Column:" + ex.getColumnNumber() + "\n" + 
			       "Is this really a native file?");

	   //System.err.println("[Fatal Error] "+
	    //		       getLocationString(ex)+": "+
	    //	       ex.getMessage());

	}
	
	/** Returns a string of the location. */
	private String getLocationString(SAXParseException ex) 
	{
	    StringBuffer str = new StringBuffer();
	    
	    String systemId = ex.getSystemId();
	    if (systemId != null) 
	    {
		int index = systemId.lastIndexOf('/');
		if (index != -1) 
		    systemId = systemId.substring(index + 1);
		str.append(systemId);
	    }
	    str.append(':');
	    str.append(ex.getLineNumber());
	    str.append(':');
	    str.append(ex.getColumnNumber());
	    
	    return str.toString();
	    
	} // getLocationString(SAXParseException):String
	
	//
	// Public methods
	//
	
	/** Prints the results. */
	public void printResults(String uri, long time) 
	{
	    System.out.print(uri);
	    System.out.print(": ");
	    System.out.print(time);
	    System.out.print(" ms (");
	    System.out.print(elements);
	    System.out.print(" elems, ");
	    System.out.print(attributes);
	    System.out.print(" attrs, ");
	    System.out.print(ignorableWhitespace);
	    System.out.print(" spaces, ");
	    System.out.print(characters);
	    System.out.print(" chars)");
	    System.out.println();

	    System.out.print(measurements);
	    System.out.print(" measurements of ");
	    if(genes > 0)
	    {
		System.out.print(genes);
		System.out.print(" genes using ");
	    }
	    System.out.print(probes);
	    System.out.print(" probes in ");
	    System.out.print(spots);
	    System.out.print(" spots");
	    if(n_clusters > 0)
	    {
		System.out.print(" with " + n_clusters + " clusters");
	    }
	    System.out.print("\n");
	    

	} // printResults(String,long)
    }

    
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   intestines
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public final static int ReplaceMode = 0;
    public final static int MergeMode   = 1;

    private boolean file_has_errors;
    
    private org.apache.xerces.parsers.SAXParser parser;

    private maxdView mview;
    private ExprData edata;
    private DataPlot dplot;
    private JFileChooser jfc;

    // used when the plugin is not started, but sent a command instead
    private int load_mode = ReplaceMode;
    private boolean load_measurements = true;
    private boolean load_spot_attrs = true;
    private boolean load_layout = true;
    private boolean load_colours = true;
    private boolean load_app_props = true;
    private boolean load_clusters = true;
    private boolean load_name_attrs = true;


    private SAXRead sr = null;

    private String sax_path = null;
 
    private boolean really_replace_meas;

    private boolean keep_existing_clusters;
    private boolean keep_existing_colourisers;
    private boolean keep_existing_name_attrs;
    

    private JRadioButton replace_jrb, merge_jrb;
    private JCheckBox keep_existing_clusters_jrb, keep_existing_colourisers_jrb, keep_existing_attributes_jrb, 
	measurements_jrb,  spot_attrs_jrb, layout_jrb, colours_jrb, app_props_jrb, 
	clusters_jrb, name_attrs_jrb;
}
