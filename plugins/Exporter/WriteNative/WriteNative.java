import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.zip.*;

//
// writes some or all of the expression data in some unspecified XML format
//

public class WriteNative implements Plugin
{
    
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  plugin implementation
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void startPlugin()
    {
	cur_path = mview.getProperty("WriteNative.data_directory", ".");

	constructGUI();
	frame.pack();
	frame.setVisible(true);
    }

    public void stopPlugin()
    {
	cleanUp();
    }

    public PluginCommand[] getPluginCommands()
    {
	PluginCommand[] com = new PluginCommand[4];
	
	String[] args = new String[] 
	{ 
	    // name                    // type      //default   // flag   // comment
	    "file",                    "file",      "",         "",      "destination file name", 
	    "which_measurements",      "string",    "all",      "",      "either 'all' or 'currently visible'", 
	    "include_spot_attributes", "boolean",   "true",     "",      "",  
	    "apply_filter",            "boolean",   "false",    "",      "",  
	    "which_clusters",          "string",    "all",      "",      "one of 'do not include', 'all' or 'only visible'", 
	    "compress",                "boolean",   "false",    "",      "", 
	    
	    "force_overwrite",         "boolean",   "false",    "",      "overwrite file of the same name if present",
	    "report_status",           "boolean",   "true",     "",      "show either success or failure message after saving",
	};
	
	com[0] = new PluginCommand("start", args);
	
	com[1] = new PluginCommand("stop", null);
	
	com[2] = new PluginCommand("set", args);

	com[3] = new PluginCommand("save", args);
	
	return com;
    }
    
    public void runCommand(String name, String[] args, CommandSignal done) 
    { 
	if(name.equals("stop"))
	{
	    if(frame != null)
		cleanUp();
	}
	else
	{
	    boolean started_this_time = false;

	    if(frame == null)
	    {
		startPlugin();
		started_this_time = true;
	    }

	    String mmode = mview.getPluginStringArg("which_measurements", args, null);
	    if(mmode != null)
	    {
		mmode = mmode.toLowerCase();
		if(mmode.startsWith("all"))
		    meas_jcb.setSelectedIndex(0);
		if(mmode.startsWith("cur"))
		    meas_jcb.setSelectedIndex(1);
	    }
	    
	    String cmode = mview.getPluginStringArg("which_clusters", args, null);
	    if(cmode != null)
	    {
		cmode = cmode.toLowerCase();
		if(cmode.startsWith("do"))
		    clust_jcb.setSelectedIndex(0);
		if(cmode.startsWith("all"))
		    clust_jcb.setSelectedIndex(1);
		if(cmode.startsWith("only"))
		    clust_jcb.setSelectedIndex(2);
	    }
	
	    apply_filter_jchkb.setSelected( mview.getPluginBooleanArg("apply_filter", args, false) );
	    
	    compress_jchkb.setSelected( mview.getPluginBooleanArg("compress", args, false) );

	    incl_attrs_jchkb.setSelected( mview.getPluginBooleanArg("include_spot_attributes", args, true) );

	    if(name.equals("save"))
	    {
		String fname = mview.getPluginStringArg("file", args, null);
		if(fname != null)
		{
		    report_status   =  mview.getPluginBooleanArg("report_status", args, true);
		    force_overwrite =  mview.getPluginBooleanArg("force_overwrite", args, false);
		    
		    exportData( fname, false );
		}

		if(started_this_time)
		    cleanUp();
	    }
	}

	if(done != null)
	    done.signal();
    } 

    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("Write Native", "exporter", "Write data in maxdView native format", "", 1, 2, 0);
	return pinf;
    }

    public String pluginType() { return "exporter"; }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   WriteNative
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public WriteNative(maxdView mview_)
    {
	mview = mview_;
	edata = mview.getExprData();
    }

    public void cleanUp()
    {
	mview.putProperty("WriteNative.data_directory", cur_path);
	mview.putIntProperty("WriteNative.measurement",meas_jcb.getSelectedIndex());
	mview.putBooleanProperty("WriteNative.include_attribs",incl_attrs_jchkb.isSelected());
	mview.putBooleanProperty("WriteNative.apply_filter", apply_filter_jchkb.isSelected());
	mview.putIntProperty("WriteNative.clusters", clust_jcb.getSelectedIndex());
	mview.putBooleanProperty("WriteNative.compress", compress_jchkb.isSelected());

	frame.setVisible(false);
	frame = null;
    }
    
    private void constructGUI()
    {
	
	frame = new JFrame("Write Native");

	mview.decorateFrame( frame );

	frame.addWindowListener(new WindowAdapter() 
			  {
			      public void windowClosing(WindowEvent e)
			      {
				  cleanUp();
			      }
			  });

	export_panel = new JPanel();
	export_panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	GridBagLayout gridbag = new GridBagLayout();
	export_panel.setPreferredSize(new Dimension(400, 300));
	export_panel.setLayout(gridbag);

	int line = 0;

	{
	    JLabel label = new JLabel("Measurements ");
	    export_panel.add(label);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    //c.weightx = c.weighty = 1.0;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	}
	{
	    String[] format_str = { "All of them", "Currently enabled" };
	    meas_jcb = new JComboBox(format_str);

	    meas_jcb.setSelectedIndex(mview.getIntProperty("WriteNative.measurement", 0));

	    //jcb.setSelectedIndex( edata.getMeasurementDataType(s) );
	    //jcb.addActionListener(new SetDataTypeListener(s) );

	    meas_jcb.setToolTipText("Which Measurements to include");

	    export_panel.add(meas_jcb);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.anchor = GridBagConstraints.WEST;
	    //c.weightx = c.weighty = 1.0;
	    gridbag.setConstraints(meas_jcb, c);
	}
	line++;

	{
	    incl_attrs_jchkb = new JCheckBox("Include Spot Attributes");

	    incl_attrs_jchkb.setSelected(mview.getBooleanProperty("WriteNative.include_attribs", true));
	    export_panel.add(incl_attrs_jchkb);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    //c.weightx = c.weighty = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    gridbag.setConstraints(incl_attrs_jchkb, c);
	}
	line++;

	{
	    JLabel label = new JLabel("Spots ");
	    export_panel.add(label);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    //c.weightx = c.weighty = 1.0;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	}
	{
	    apply_filter_jchkb = new JCheckBox("Apply filter");
	    apply_filter_jchkb.setSelected(mview.getBooleanProperty("WriteNative.apply_filter", false));
	    apply_filter_jchkb.setToolTipText("Use the filter to remove unwanted spots");
     
	    export_panel.add(apply_filter_jchkb);

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    //c.weightx = c.weighty = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    gridbag.setConstraints(apply_filter_jchkb, c);
	}
	line++;
	
	{
	    JLabel label = new JLabel("Clusters ");
	    export_panel.add(label);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    //c.weightx = c.weighty = 1.0;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	}
	{
	    String[] format_str = { "Do not include", "All of them", "Only visible clusters" };

	    clust_jcb = new JComboBox(format_str);

	    clust_jcb.setSelectedIndex(mview.getIntProperty("WriteNative.clusters", 1));

	    //jcb.setSelectedIndex( edata.getMeasurementDataType(s) );
	    //jcb.addActionListener(new SetDataTypeListener(s) );

	    clust_jcb.setToolTipText("Which Clusters to include");

	    export_panel.add(clust_jcb);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.anchor = GridBagConstraints.WEST;
	    //c.weightx = c.weighty = 1.0;
	    gridbag.setConstraints(clust_jcb, c);
	}
	line++;

	{
	    JLabel label = new JLabel("Other ");
	    export_panel.add(label);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    //c.weightx = c.weighty = 1.0;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	}

	{
	    JCheckBox save_cols_jchkb = new JCheckBox("Save colour scheme");
	    save_cols_jchkb.setEnabled(false);
	    //apply_filter_save_cols_jchkb.setSelected(mview.getBooleanProperty("WriteNative.apply_filter", true));
	    save_cols_jchkb.setToolTipText("Include the current colour scheme in the file");
     
	    export_panel.add(save_cols_jchkb);

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    //c.weightx = c.weighty = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    gridbag.setConstraints(save_cols_jchkb, c);
	}
	line++;
	{
	    JCheckBox save_layout_jchkb = new JCheckBox("Save layout parameters");
	    save_layout_jchkb.setEnabled(false);
	    //apply_filter_save_cols_jchkb.setSelected(mview.getBooleanProperty("WriteNative.apply_filter", true));
	    save_layout_jchkb.setToolTipText("Include the current layout parameters in the file");
     
	    export_panel.add(save_layout_jchkb);

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    //c.weightx = c.weighty = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    gridbag.setConstraints(save_layout_jchkb, c);
	}
	line++;
	{
	    JCheckBox save_app_props_jchkb = new JCheckBox("Save applicaton properties");
	    save_app_props_jchkb.setEnabled(false);
	    //apply_filter_save_cols_jchkb.setSelected(mview.getBooleanProperty("WriteNative.apply_filter", true));
	    save_app_props_jchkb.setToolTipText("Include the current applicaton properties in the file");
     
	    export_panel.add(save_app_props_jchkb);

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    //c.weightx = c.weighty = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    gridbag.setConstraints(save_app_props_jchkb, c);
	}
	line++;

	{
	    JLabel label = new JLabel("Comments ");
	    export_panel.add(label);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    //c.weightx = c.weighty = 1.0;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	}
	{
	    comment_jta = new JTextArea(80, 20);
	    JScrollPane jsp = new JScrollPane(comment_jta);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.weightx = 1.0;
	    c.weighty = 2.0;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(jsp, c);

	    export_panel.add(jsp);
	}
	line++;
	{
	    compress_jchkb = new JCheckBox("Compress with GZIP");
	    compress_jchkb.setSelected(mview.getBooleanProperty("WriteNative.compress", true));
	    compress_jchkb.setToolTipText("Use GZIP compression to make the file smaller");
     
	    export_panel.add(compress_jchkb);

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.anchor = GridBagConstraints.WEST;
	    gridbag.setConstraints(compress_jchkb, c);
	}
	line++;
	{
	    JPanel buttons_panel = new JPanel();
	    GridBagLayout inner_gridbag = new GridBagLayout();
	    buttons_panel.setLayout(inner_gridbag);
	    buttons_panel.setBorder(BorderFactory.createEmptyBorder(20, 10, 0, 0));

	    {   
		final JButton jb = new JButton("Close");
		buttons_panel.add(jb);
		
		jb.setToolTipText("Close this dialog...");
		
		jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					 {
					     cleanUp();
					 }
				     });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		inner_gridbag.setConstraints(jb, c);
	    }
	    {   
		final JButton jb = new JButton("Export");
		buttons_panel.add(jb);
		
		jb.setToolTipText("Ready to choose a filename...");
		
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    JFileChooser jfc = new JFileChooser();
			    CustomFileFilter cff = new CustomFileFilter();
			    jfc.addChoosableFileFilter(cff);
			    jfc.setFileFilter(cff);
			    jfc.setCurrentDirectory(new File(cur_path));

			    int returnVal = jfc.showSaveDialog(export_panel);

			    if (returnVal == JFileChooser.APPROVE_OPTION) 
			    {
				File file = jfc.getSelectedFile();
				cur_path = file.getPath();

				exportData(cur_path, true);

				cleanUp();
				/*
				  if(file.canWrite() == true)
				  exportData(file);
				  else
				  JOptionPane.showMessageDialog(null, 
				  "Unable to write", 
				  "Unable to write", 
				  JOptionPane.ERROR_MESSAGE); 
				*/
			    }
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		inner_gridbag.setConstraints(jb, c);
	    }

	    {   
		final JButton jb = new JButton("Help");
		buttons_panel.add(jb);
		
		jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					 {
					     mview.getPluginHelpTopic("WriteNative", "WriteNative");
					 }
				     });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 0;
		inner_gridbag.setConstraints(jb, c);
	    }

	    export_panel.add(buttons_panel);

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.gridwidth = 2;
	    //c.weighty = 2.0;
	    c.weightx = 1.0;
	    gridbag.setConstraints(buttons_panel, c);

	}
	
	frame.getContentPane().add(export_panel, BorderLayout.CENTER);
    }
    
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

	// Accept all directories and all gif, jpg, or tiff files.
	public boolean accept(File f) 
	{
	    if (f.isDirectory()) 
	    {
		return true;
	    }
	    
	    String extension = getExtension(f.getName());

	    // System.out.println("testing: "  + f.getName() + " ext=" + extension);

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

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   exportData()
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private boolean spotIsVisible(int s) 
    {
	boolean result = true;
	if(apply_filter_jchkb.isSelected() && (edata.filter(s) == true))
	    result = false;
	return result;
    }

    private boolean measIsVisible(int m) 
    {
	boolean result = true;

	int mode = meas_jcb.getSelectedIndex();

	if(mode == 0)
	    result = true;
	if(mode == 1)
	    result = edata.getMeasurementShow(m);

	return result;
    }

    private boolean clustIsVisible(ExprData.Cluster c) 
    {
	boolean result = true;

	int mode = clust_jcb.getSelectedIndex();

	if(mode == 0)
	    result = false;
	if(mode == 1)
	    result = true;
	if(mode == 2)
	    result = c.getShow();

	return result;
    }

    //
    // replace '<', '>', '&' and '"' with their HTML entity names
    //
    private String makeSafeHTML(String s)
    {
	if(s == null)
	    return "";

	StringBuffer sbuf = new StringBuffer(s.length());
	for(int sc=0; sc < s.length(); sc++)
	{
	    final char ch = s.charAt( sc );

	    // check for wierd non-printing characers
	    if( Character.getType( ch ) == Character.CONTROL )
	    {
		// do nothing - or output a space or what?
	    }
	    else
	    {
		switch( ch )
		{
		    case '"':
			sbuf.append("&quot;");
			break;
		    case '>':
			sbuf.append("&gt;");
			break;
		    case '<':
			sbuf.append("&lt;");
			break;
		    case '&':
			sbuf.append("&amp;");
			break;
		    default:
			sbuf.append(s.charAt(sc));
			break;
		}
	    }
	}
	return sbuf.toString();

    }

    public void exportData( String name, boolean async )
    {
	String new_name = new String(name);
	    
	if(compress_jchkb.isSelected())
	{
	    // check for a .gz extension...
	    int ext_pos = name.lastIndexOf('.');
	    if(ext_pos <= 0)
	    {
		new_name += ".gz";
	    }
	    else
	    {
		String ext = name.substring(ext_pos).toLowerCase();
		if(ext.equals(".gz") || ext.equals(".zip"))
		{
		    new_name = name;
		}
		else
		{
		    new_name += ".gz";
		}
	    }
	}

	File file = new File( new_name );

	if(!force_overwrite)
	{
	    if(file.exists())
	    {
		if(mview.infoQuestion("File exists, overwrite?", "No", "Yes") == 0)
		    return;
	    }
	}

	
	if(async)
	{
	    ProgressOMeter pm = new ProgressOMeter("Saving", 3);
	    pm.startIt();
	    
	    (new ExportThread(pm, file)).start();
	}
	else
	{
	    try
	    {
		doExport(file);
	    }
	    catch (java.io.IOException ioe)
	    {
		mview.alertMessage("Unable to write " + file.getName() + "\n\n" + ioe);
	    }
	}
    }

    public class ExportThread extends Thread
    {
	public ExportThread(ProgressOMeter pm_, File file_)
	{
	    file = file_;
	    pm = pm_;
	}

	public void run()
	{
	    try
	    {
		String result = doExport(file);
		pm.stopIt();
		mview.successMessage(result);
	    }
	    catch (java.io.IOException ioe)
	    {
		if(pm != null)
		    pm.stopIt();
		mview.alertMessage("Unable to write " + file.getName() + "\n\n" + ioe);
		return;
	    }
	}
	
	private ProgressOMeter pm;
	private File file;
    }

    public String doExport(File file) throws java.io.IOException
    {
	BufferedWriter writer = null;
	
	if(compress_jchkb.isSelected())
	{
	    FileOutputStream fos = new FileOutputStream(file);
	    BufferedOutputStream bos = new BufferedOutputStream(fos);
	    GZIPOutputStream gos = new GZIPOutputStream(bos);
	    OutputStreamWriter osw = new OutputStreamWriter(gos);
	    writer = new BufferedWriter(osw);
	}
	else
	{
	    writer = new BufferedWriter(new FileWriter(file));
	    }
	
	//ExprData edata = mview.getExprData();
	DataPlot dplot = mview.getDataPlot();
	
	int data_pts = 0;
	int p_out = 0;
	int s_out = 0;
	int g_out = 0;
	
	final int n_spots = edata.getNumSpots();
	
	meas_ignored = 0;
	clust_ignored = 0;
	spot_ignored = 0;
	
	// write the header information
	writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
	// writer.write("<!DOCTYPE maxd SYSTEM \"maxd.dtd\">\n");
	
	writer.write("\n<!-- " + mview.getApplicationTitle() + " -->\n\n");
	writer.write("\n<!-- see http://www.bioinf.man.ac.uk/microarray/maxd/  -->\n\n");
	writer.write("\n<!-- this file generated by maxdView's WriteNative plugin -->\n\n");
	
	
	writer.write("<MeasurementGroup>\n");
	
	String comment_d = comment_jta.getText();
	if((comment_d != null) && (comment_d.length() > 0))
	{
	    writer.write("\n <Comment VALUE=\"" + makeSafeHTML(comment_d) + "\" />\n\n");
	}

	writer.write(" <ArrayType NAME=\"" + "unknown" + "\">\n\n");
	
	//
	    // first write the probe to gene mappings for any probes used
	// (keep probes unique)
	    //
	Hashtable probe_names = new Hashtable();
	
	final ExprData.TagAttrs pta = edata.getProbeTagAttrs();
	final ExprData.TagAttrs gta = edata.getGeneTagAttrs();
	
	int[] src_spot_id_to_dest_spot_id = null;
	int[] src_meas_id_to_dest_meas_id = null;

	for(int s=0; s< n_spots; s++)
	{
	    if(spotIsVisible(s))
	    {
		final String pname = edata.getProbeName(s);
		String res = (pname == null) ? null : (String) probe_names.get(pname);
		if((pname != null) && (res == null))
		{
		    String[] genes = edata.getGeneNames(s);
		    
		    writer.write("  <Probe NAME=\"" + makeSafeHTML(pname) + "\"");
		    
		    for(int a=0; a < pta.getNumAttrs(); a++)
		    {
			String v = pta.getTagAttr(pname, a);
			if(v != null)
			    writer.write(" _ATTR" + (a+1) + "=\"" + 
					 makeSafeHTML(pta.getAttrName(a)) + 
					 "\" _VAL" + (a+1) + "=\"" + 
					 makeSafeHTML(pta.getTagAttr(pname, a)) + "\"");
			
			//writer.write(" " + makeSafeHTML(pta.getAttrName(a)) + 
			//		     "=\"" + makeSafeHTML(pta.getTagAttr(pname, a)) + "\"");
		    }
		    
		    writer.write(">\n");
		    
		    //data_pts++;
		    if(genes != null)
		    {
			for(int g=0;g<genes.length;g++)
			{
				//if(g == 0)
				//    writer.write("\n");
			    writer.write("   <Gene NAME=\"" + makeSafeHTML(genes[g]) + "\"");
			    for(int a=0; a < gta.getNumAttrs(); a++)
			    {
				String v = gta.getTagAttr(genes[g], a);
				if(v != null)
				    writer.write(" _ATTR" + (a+1) + "=\"" + 
						 makeSafeHTML(gta.getAttrName(a)) + 
						 "\" _VAL" + (a+1) + "=\"" + 
						 makeSafeHTML(gta.getTagAttr(genes[g], a)) + "\"");
				
				//writer.write(" " + makeSafeHTML(gta.getAttrName(a)) +
				//	     "=\"" + makeSafeHTML(gta.getTagAttr(genes[g], a)) + "\"");
			    }
			    writer.write("/>\n");
			    g_out++;
			}
		    }
		    writer.write("  </Probe>\n");
		    
		    // flag the fact that this probe has been output...
		    probe_names.put(pname, pname);
		    
		    p_out++;
		    
		}
	    }
	    else
	    {
		spot_ignored++;
	    }
	}
	writer.write("\n");
	
	//
	// now write the list of spots and their contents
	//
	// use the 'natural' order, not the current display order
	// otherwise the cluster's spot_id's will be buggered up.
	//
	
	final ExprData.TagAttrs sta = edata.getSpotTagAttrs();
	
	for(int s=0; s< n_spots; s++)
	{
	    if(spotIsVisible(s))
	    {
		final String sname = edata.getSpotName(s);
		
		writer.write("  <Spot NAME=\"" + makeSafeHTML(sname) + 
			     "\" PROBE=\"" + makeSafeHTML(edata.getProbeName(s)) + "\"");
		
		for(int a=0; a < sta.getNumAttrs(); a++)
		{
		    String v = sta.getTagAttr(sname, a);
		    if(v != null)
			writer.write(" _ATTR" + (a+1) + "=\"" + 
				     makeSafeHTML(sta.getAttrName(a)) + 
				     "\" _VAL" + (a+1) + "=\"" + 
				     makeSafeHTML(sta.getTagAttr(sname, a)) + "\"");
		}
		    writer.write(" />\n");
		    s_out++;
		    //data_pts++;
	    }
	    
	}
	
	
	writer.write(" </ArrayType>\n");
	
	//
	// now the environment parameters (colours etc)
	//
	{
	    writer.write("\n <Environment>\n");
	    
	    
	    Colouriser[] col_a = dplot.getColouriserArray();
	    
	    if(col_a != null)
	    {
		for(int c=0; c < col_a.length; c++)
		{
		    Hashtable attrs = col_a[c].createAttrs();
		    
		    writer.write("\n  <Colouriser ");
		    
		    for (Enumeration e = attrs.keys(); e.hasMoreElements() ;) 
		    {
			String key = (String) e.nextElement();
			writer.write(makeSafeHTML(key) + "=\"" + makeSafeHTML((String) attrs.get(key)) + "\" ");
		    }
		    writer.write(" />\n");
		}
	    }
	    
	    //
	    // the mapping from Measurement name to Colouriser
	    //
	    //
	    for(int m=0; m < edata.getNumMeasurements(); m++)
	    {
		Colouriser col = dplot.getColouriserForMeasurement(m);
		if((col != null) && (measIsVisible(m)))
		{
		    writer.write("\n  <ColouriserUser MEASUREMENT=\"" + makeSafeHTML(edata.getMeasurementName(m)) + 
				 "\" COLOURISER=\"" + makeSafeHTML(col.getName()) + "\" />\n");
		}
	    }
	    
	    writer.write("\n  <BoxGeometry WIDTH=\"" + dplot.getBoxWidth() + "\" HEIGHT=\"" + dplot.getBoxHeight() + 
			 "\" COL_GAP=\"" + dplot.getColGap() + "\" ROW_GAP=\"" + dplot.getRowGap() + 
			 "\" BORDER_GAP=\"" +  dplot.getBorderGap() + "\" />\n");
	    
	    writer.write("  <Font NAME=\"SPOT\"" + 
			 " SIZE=\""  + dplot.getSpotFontSize() + "\"" +
			 " FAMILY=\"" + dplot.getSpotFontFamily() + "\"" +
			 " STYLE=\"" + dplot.getSpotFontStyle() + "\"" +
			 " ANTIALIAS=\"" + dplot.getSpotFontAntialiasing() + "\"/>\n");
	    
	    writer.write("  <Font NAME=\"MEASUREMENT\"" + 
			 " SIZE=\""  + dplot.getMeasurementFontSize() + "\"" +
			 " FAMILY=\"" + dplot.getMeasurementFontFamily() + "\"" +
			 " STYLE=\"" + dplot.getMeasurementFontStyle() + "\"" +
			 " ANTIALIAS=\"" + dplot.getMeasurementFontAntialiasing() + "\" />\n");
	    

	    writer.write("  <DisplayZoom LEVEL=\"" + dplot.getZoom() + "\" />\n");
	    
	    writer.write("  <SpotClusterDisplay BRANCH_SCALE=\"" + dplot.getBranchScale(0) + "\" " + 
			 " OVERLAY_ROOT_CHILDREN=\"" + dplot.getOverlayRootChildren(0) + "\" " + 
			 " SHOW_BRANCHES=\"" + dplot.getShowBranches(0) + "\" " + 
			 " SHOW_GLYPHS=\"" + dplot.getShowGlyphs(0) + "\" " +
			 " ALIGN_GLYPHS=\"" + dplot.getAlignGlyphs(0) + "\" />\n");
	    
	    writer.write("  <MeasurementClusterDisplay BRANCH_SCALE=\"" + dplot.getBranchScale(1) + "\" " + 
			 " OVERLAY_ROOT_CHILDREN=\"" + dplot.getOverlayRootChildren(1) + "\" " + 
			 " SHOW_BRANCHES=\"" + dplot.getShowBranches(1) + "\" " + 
			 " SHOW_GLYPHS=\"" + dplot.getShowGlyphs(1) + "\" " +
			 " ALIGN_GLYPHS=\"" + dplot.getAlignGlyphs(1) + "\" />\n");	    

	    writer.write("  <MeasurementLabelDisplay ALIGN=\"" + dplot.getMeasurementLabelAlign() + "\" />\n");

	    /*
	      writer.write("  <DisplayOptions ROW_LABEL_SOURCE=\"" + dplot.getRowLabelSource() + 
	      "\" ROW_LABEL_ALIGN=\"" + dplot.getRowLabelAlign() + "\" />\n");
	    */
	    
	    
	    //
	    // name columns
	    //
	    for(int nc=0; nc < dplot.getNumNameCols(); nc++)
	    {
		ExprData.NameTagSelection nts = dplot.getNameColSelection(nc);
		writer.write("  <NameColumn DISPLAY=\"" + makeSafeHTML(nts.getNames()) + "\" TRIM=\"" +
			     dplot.getNameColTrimEnabled(nc) + "\" LENGTH=\"" + 
			     dplot.getNameColTrimLength(nc) + "\" ALIGN=\"" + 
			     dplot.getNameColAlign(nc) + "\" />\n");
	    }
	    
	    
		//
		// spot and measurement orderings......
		//
		
		// what to do if one or more Spots aren't being written??

		/*
		int spots_out = 0;
		for(int s=0; s< n_spots; s++)
		    if(spotIsVisible(s))
			spots_out++;
			
			int[] sorder = new int[spots_out];
			spots_out = 0;
			for(int s=0; s< n_spots; s++)
			if(spotIsVisible(s))
			sorder[spots_out] = edata.getSpotAtIndex++;
		*/
	    

	    // FIXED in v1.2
	    //       need to take into account any filtering when writing the spot ordering
	    //       and the spot selection if the "apply filter" option is selected...

	    
	    if( apply_filter_jchkb.isSelected() )
	    {
		int n_spots_out = 0;
		int n_spots_sel = 0;

		for(int s=0; s< n_spots; s++)
		{
		    if( spotIsVisible( s ) )
		    {
			n_spots_out++;

			if( edata.isSpotSelected( s ) )
			    n_spots_sel++;
		    }
		}

		// need to keep track of how spot indices in the 'complete' set map to 
		// indices in the 'filtered' set

		src_spot_id_to_dest_spot_id = new int[ n_spots ];
		int dest_spot_id = 0;
		for(int s=0; s < n_spots; s++)
		{
		    if( spotIsVisible( s ) )
			src_spot_id_to_dest_spot_id[ s ] = dest_spot_id++;
		    else
			src_spot_id_to_dest_spot_id[ s ] = -1;
		}

		int[] spot_order = new int[ n_spots_out ];
		int[] spot_sel   = new int[ n_spots_sel ];

		n_spots_out = 0;
		n_spots_sel = 0;

		for(int si=0; si < n_spots; si++)
		{
		    int s = edata.getSpotAtIndex( si );

		    if( spotIsVisible( s ) )
		    {
			spot_order[ n_spots_out++ ] = src_spot_id_to_dest_spot_id[ s ];

			if( edata.isSpotSelected( s ) )
			    spot_sel[ n_spots_sel++ ] = src_spot_id_to_dest_spot_id[ s ];
		    }
		}

		writeNamedOrdering(writer, "DisplayOrder", "SPOT",  spot_order );

		writeNamedOrdering(writer, "Selection", "SPOT", spot_sel );
	    }
	    else
	    {
		// construct a null map ('null' because src_id maps to the same dest_id) 
		//

		src_spot_id_to_dest_spot_id = new int[ n_spots ];
		for(int s=0; s < n_spots; s++)
		    src_spot_id_to_dest_spot_id[ s ] = s;
		
		writeNamedOrdering(writer, "DisplayOrder", "SPOT", edata.getSpotOrder());

		writeNamedOrdering(writer, "Selection", "SPOT", edata.getSpotSelection());
	    }


	    // FIXED in v1.2
	    //     need to take into account any hidden measurements when writing ordering
	    //     if the "currently enabled" option is selected...

	    final int n_meas = edata.getNumMeasurements();

	    if( meas_jcb.getSelectedIndex() == 1 )
	    {
		// we are only outputting some Measurements....

		
		int n_meas_out = 0;
		int n_meas_sel = 0;

		for(int m=0; m < n_meas; m++)
		{
		    if( edata.getMeasurementShow( m ) )
		    {
			n_meas_out++;

			if( edata.isMeasurementSelected( m ) )
			    n_meas_sel++;
		    }
		}

		int[] meas_order = new int[ n_meas_out ];
		int[] meas_sel   = new int[ n_meas_sel ];



		// need to keep track of how measurement indices in the 'complete' set map to 
		// indices in the 'only_enabled' set

		src_meas_id_to_dest_meas_id = new int[ n_meas ];
		int dest_meas_id = 0;
		for(int m=0; m < n_meas; m++)
		{
		    if( edata.getMeasurementShow( m ) )
			src_meas_id_to_dest_meas_id[ m ] = dest_meas_id++;
		    else
			src_meas_id_to_dest_meas_id[ m ] = -1;
		}


		n_meas_out = 0;
		n_meas_sel = 0;

		for(int mi=0; mi < n_meas; mi++)
		{
		    int m = edata.getMeasurementAtIndex( mi );

		    if( edata.getMeasurementShow( m ) )
		    {
			meas_order[ n_meas_out++ ] = src_meas_id_to_dest_meas_id[ m ];

			if( edata.isMeasurementSelected( m ) )
			    meas_sel[ n_meas_sel++ ] = src_meas_id_to_dest_meas_id[ m ];
		    }
		}

		writeNamedOrdering( writer, "DisplayOrder", "MEASUREMENT", meas_order );

		writeNamedOrdering( writer, "Selection", "MEASUREMENT", meas_sel );
	    }
	    else
	    {
		// construct a null map ('null' because src_id maps to the same dest_id) 
		//

		src_meas_id_to_dest_meas_id = new int[ n_meas ];
		for(int m=0; m < n_meas; m++)
		    src_meas_id_to_dest_meas_id[ m ] = m;

		writeNamedOrdering( writer, "DisplayOrder", "MEASUREMENT", edata.getMeasurementOrder() );

		writeNamedOrdering( writer, "Selection", "MEASUREMENT", edata.getMeasurementSelection() );
	    }
	    
	    //
	    // now the current application properties data
	    //
	    
	    writer.write("\n  <ApplicationProperties>");
	    
	    Properties prop = mview.getProperties();
	    Enumeration prop_names = prop.propertyNames();
	    while(prop_names.hasMoreElements())
	    {
		String name = (String) prop_names.nextElement();
		String val  = (String) prop.get(name);
		
		writer.write("\n   <StringProp NAME=\"" + makeSafeHTML(name) + 
			     "\" VALUE=\"" + makeSafeHTML(val) + "\" />");
	    }
	    
	    writer.write("\n  </ApplicationProperties>\n");
	    
	    writer.write("\n </Environment>\n");
	}
	
	//
	// now write the data values themselves
	// one measurement at a time
	//
	for(int m=0; m < edata.getNumMeasurements(); m++)
	{
	    ExprData.Measurement ms = edata.getMeasurement(m);
	    if(measIsVisible(m))
	    {
		writer.write("\n <Measurement NAME=\"" + 
			     makeSafeHTML( ms.getName() ) + "\" DATATYPE=\"" + 
			     ms.getDataTypeString() + "\">\n\n");
		
		//
		// write any attributes that this measurement has...
		//
		
		for (Enumeration e = ms.getAttributes().keys() ; e.hasMoreElements() ;) 
		{
		    String key = (String) e.nextElement();
		    ExprData.Measurement.MeasurementAttr ma = (ExprData.Measurement.MeasurementAttr) (ms.getAttributes().get( key ));
		    
		    //data_pts++;
		    writer.write("  <Attribute NAME=\"" + makeSafeHTML( ma.name ) + 
				 "\"\n    VALUE=\"" + makeSafeHTML(ma.value) + 
				 "\"\n    SOURCE=\"" + makeSafeHTML(ma.source) + 
				 "\"\n    CREATED=\"" + makeSafeHTML(ma.time_created) + 
				 "\"\n    MODIFIED=\"" + makeSafeHTML(ma.time_last_modified) + "\" />\n");
		}
		if(ms.getAttributes().size() > 0)
		    writer.write("\n");
		
		
		if(incl_attrs_jchkb.isSelected())
		{
		    //
		    // and now any spot attributes 
		    // 
		    // which must come before the DataBlock for the
		    // Measurement's actual values because that was the
		    // DataBlocks can be parsed in 'immediate' mode
		    // which makes writing the reader much easier....
		    //
		    int n_spt_atts = ms.getNumSpotAttributes();
		    for(int sa=0; sa < n_spt_atts; sa++)
		    {
			writer.write("\n  <SpotAttribute NAME=\"" + makeSafeHTML(ms.getSpotAttributeName(sa)) + 
				     "\" UNIT=\"" + makeSafeHTML(ms.getSpotAttributeUnit(sa)) + 
				     "\" TYPE=\"" + makeSafeHTML(ms.getSpotAttributeDataType(sa)) + "\" >\n");
			
			writer.write("   <DataBlock NAME=\"SpotAttr: " + makeSafeHTML(ms.getSpotAttributeName(sa)) + "\">\n"); 
			
			int s = 0;
			int sc = 0;
			writer.write("\n    <DataChunk START=\"" + s + "\" >\n");

			int dt =  ms.getSpotAttributeDataTypeCode(sa);

			while(s < n_spots)
			{
			    if(spotIsVisible(s))
			    {
				String str = ms.getSpotAttributeDataValueAsString(sa, s);

				if(dt == 2) // char
				{
				    char[] cvec = (char[])  ms.getSpotAttributeData(sa);
				    if( cvec[s] == '\0' ) // missing value
					str = "\\0";
				}
				if(dt == 3) // text
				{
				    String[] svec = (String[])  ms.getSpotAttributeData(sa);
				    if( svec[s] == null ) // missing value
					str = "\\0";
				}

				writer.write(str + " ");
				data_pts++;
				if(++sc == nums_per_chunk)
				{
				    sc = 0;
				    writer.write("\n    </DataChunk>");
				    writer.write("\n    <DataChunk START=\"" + (s+1) + "\" >\n");
				}
			    }
			    s++;
			}
			writer.write("\n    </DataChunk>");
			writer.write("\n   </DataBlock>\n");
			
			/*
			  for(int s=0; s< n_spots; s++)
			  {
			  if(spotIsVisible(s))
			  {
			  String str = ms.getSpotAttributeDataValueAsString(sa, s);
			  data_pts++;
			  writer.write("   <Spot ID=" + s + " VAL=\"" + str + "\" />\n");
			  }
			  }
			*/
			
			writer.write("  </SpotAttribute>\n");
		    }
		}
		
		//
		// then the data values themselves
		//
		{
		    writer.write("  <DataBlock NAME=\"ExprLevel\">\n"); 
		    
		    int s = 0;
		    int sc = 0;
		    writer.write("\n    <DataChunk START=\"" + s + "\" >\n");
		    while(s < n_spots)
		    {
			if(spotIsVisible(s))
			{
			    writer.write(String.valueOf(edata.eValue(m,s) + " "));
			    data_pts++;
			    if(++sc == nums_per_chunk)
			    {
				sc = 0;
				writer.write("\n    </DataChunk>");
				writer.write("\n    <DataChunk START=\"" + (s+1) + "\" >\n");
			    }
			}
			s++;
		    }
		    writer.write("\n    </DataChunk>");
		    writer.write("\n   </DataBlock>\n");
		}
		writer.write(" </Measurement>\n");
	    }
	    else
	    {
		meas_ignored++;
	    }
	}
	
	
	int clusters = 0;
	int cmode = clust_jcb.getSelectedIndex();
	if(cmode > 0)
	{
	    clusters = recursivelyWriteCluster(writer, edata.getRootCluster(), 
					       cmode, 1, 
					       src_spot_id_to_dest_spot_id, 
					       src_meas_id_to_dest_meas_id);
	}
	
	writer.write("</MeasurementGroup>\n");
	
	writer.close();
	
	String msg = s_out + " Spots (with " + p_out + " Probes and " + g_out + " Genes)\n";
	
	msg += (data_pts + " data values");
	
	if((cmode > 0) && (clusters > 0))
	    msg += ", " + clusters + " clusters";
	
	msg += " written to " + file.getName();
	
	if(meas_ignored > 0)
	{
	    msg += "\n" + (meas_ignored == 1 ? "One Measurement was " : (meas_ignored + " Measurements were "));
	    msg += "not saved";
	}
	
	if(spot_ignored > 0)
	{
	    msg += "\n" + (spot_ignored == 1 ? "One Spot was " : (spot_ignored + " Spots were "));
	    msg += "not saved";
	}
	
	if(clust_ignored > 0)
	{
	    msg += "\n" + (clust_ignored == 1 ? "One Cluster was " : (clust_ignored + " Clusters were "));
	    msg += "not saved";
	}
	
	// mview.successMessage( msg );
	
	//System.out.println(lines + " lines written to " + file.getName());

	return msg; 
    }
    
    // returns a count of the number of clusters written
    //
    private int recursivelyWriteCluster(final Writer writer, final ExprData.Cluster cl, 
					final int mode,      final int indent,
					final int[] spot_id_map,   final int[] meas_id_map ) 
	throws java.io.IOException
    {
	int n_cls = 0;

	//final ExprData edata = mview.getExprData();

	StringBuffer ind_sb = new StringBuffer(indent);
	for(int c=0;c<indent;c++)
	    ind_sb.append(' ');
	String ind = ind_sb.toString();
	
	boolean skipit = false;

	int next_ind = indent;
	// don't write the top-level root cluster to the file, 
	// it is implicit in the load
	//
	if( cl.getParent() != null )
	{
	    if( clustIsVisible( cl ) )
	    {
		n_cls = 1;

		Color c = cl.getColour();
		int cnum = (c.getRed() << 16) | (c.getGreen() << 8) | (c.getBlue());

		Vector eln = cl.getElementNames();
		int[] eli = cl.getElements();
		
		final int n_elems = (eln == null) ? 0 : eln.size();

		
		/*
		System.out.println(ind + "<Cluster NAME=\"" + makeSafeHTML(cl.getName()) + "\" COLOUR=\""+ cnum + 
				   "\" GLYPH=\"" + cl.getGlyph() + "\" SIZE=\"" + n_elems + "\">\n");

		if(eln == null)
		    System.out.println(ind + " ElementNames is null");
		else
		    System.out.println(ind + " ElementNames is " + eln.size());
		*/

		writer.write(ind + "<Cluster NAME=\"" + makeSafeHTML(cl.getName()) + "\" COLOUR=\""+ cnum + 
			     "\" GLYPH=\"" + cl.getGlyph() + "\" SIZE=\"" + n_elems + "\" SHOW=\"" + 
			     (cl.getShow() ? "TRUE" : "FALSE") + "\">\n");
		
		final int name_mode = cl.getElementNameMode();

		for(int e=0; e < n_elems; e++)
		{
		    switch(name_mode)
		    {
		    case ExprData.ProbeName:
			writer.write(ind + " <Probe NAME=\"" + makeSafeHTML((String)eln.elementAt(e)) + "\" />\n");
			break;

		    case ExprData.SpotName:
			writer.write(ind + " <Spot NAME=\"" + makeSafeHTML((String)eln.elementAt(e)) + "\" />\n");
			break;

		    case ExprData.SpotIndex:
			// check whether this SpotIndex still exists (in the case where 'apply_filter' is enabled....
			if( spot_id_map[ eli[ e ] ] >= 0 )
			    writer.write(ind + " <Spot INDEX=\"" + String.valueOf( spot_id_map[ eli[ e ] ] ) + "\" />\n");
			break;

		    case ExprData.MeasurementIndex:
			// todo: check whether this MeasurementIndex still exists....
			if( meas_id_map[ eli[ e ] ] >= 0 )
			    writer.write(ind + " <Measurement INDEX=\"" + String.valueOf( meas_id_map[ eli[ e ] ] ) + "\" />\n");
			break;

		    case ExprData.MeasurementName:
			writer.write(ind + " <Measurement NAME=\"" + makeSafeHTML((String)eln.elementAt(e)) + "\" />\n");
			break;
			
		    }
		}
		next_ind ++;
	    }
	    else
	    {
		clust_ignored++;
	    }
	}

	Vector ch = cl.getChildren();
	if(ch != null)
	{
	    //writer.write(ind + "\n");

	    for(int c=0; c < ch.size(); c++)
	    { 
		n_cls += recursivelyWriteCluster(writer, (ExprData.Cluster)ch.elementAt(c), mode, next_ind, spot_id_map, meas_id_map );

		//writer.write(ind + "\n");
	    }
	
	}

	if((cl.getParent() != null) && (!skipit))
	    writer.write(ind + "</Cluster>\n");

	return (n_cls);
    }


    private void writeNamedOrdering(final Writer writer, final String elname, 
				    final String name, final int[] data) throws java.io.IOException
    {
	if(data != null)
	{
	    writer.write("\n   <" + elname + " NAME=\"" + name + "\" SIZE=\"" + data.length + "\">\n");
	    writer.write("    <DataBlock NAME=\"" + name + "\">\n"); 
	    
	    int s = 0;
	    int sc = 0;
	    writer.write("     <DataChunk START=\"" + s + "\" >\n");
	    while(s < data.length)
	    {
		writer.write(String.valueOf(data[s]) + " ");
		if(++sc == nums_per_chunk)
		{
		    sc = 0;
		    writer.write("\n     </DataChunk>");
		    writer.write("\n     <DataChunk START=\"" + (s+1) + "\" >\n");
		}
		s++;
	    }
	    writer.write("\n     </DataChunk>");
	    writer.write("\n    </DataBlock>");
	    writer.write("\n   </" + elname + ">\n");
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   intestines
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
	
    public final int nums_per_chunk = 256;
    
    String cur_path = null;
    
    private int meas_ignored;
    private int clust_ignored;
    private int spot_ignored;

    private boolean report_status = true;
    private boolean force_overwrite = false;

    private JFrame frame;
    private maxdView mview;
    private ExprData edata;
    private JPanel export_panel;
    private JComboBox meas_jcb, clust_jcb;
    private JTextArea comment_jta;
    private JCheckBox compress_jchkb, incl_attrs_jchkb, apply_filter_jchkb;
    private JProgressBar export_progress;
}
