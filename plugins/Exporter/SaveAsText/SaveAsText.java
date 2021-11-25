import javax.swing.*;
import javax.swing.event.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.zip.*;
import java.util.Hashtable;
import java.util.Vector;
import javax.swing.border.*;
import javax.swing.table.*;
//
//
public class SaveAsText implements ExprData.ExprDataObserver, Plugin
{

    public SaveAsText(maxdView mview_)
    {
	mview = mview_;
	edata = mview.getExprData();
	dplot = mview.getDataPlot();
    }

    public void startPlugin()
    {
	frame = new JFrame("Save As Text");
	
	mview.decorateFrame( frame );

	frame.addWindowListener(new WindowAdapter() 
	    {
		public void windowClosing(WindowEvent e)
		{
		    cleanUp();
		}
	    });

	export_panel = new JPanel();
	export_panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	frame.getContentPane().add(export_panel, BorderLayout.CENTER);
	GridBagLayout gridbag = new GridBagLayout();
	export_panel.setPreferredSize(new Dimension(500, 400));
	export_panel.setLayout(gridbag);

	// analyseColumnWidths();

	Color title_colour = new JLabel().getForeground().brighter();	    
	int line = 0;
	JLabel label;
	GridBagConstraints c;

	JTabbedPane tabbed = new JTabbedPane();

	// ==========================================================================
	// the main options:
	// ==========================================================================
		
	{
	    line = 0;

	    JPanel panel = new JPanel();
	    panel.setBorder(BorderFactory.createEmptyBorder(3,5,3,5));
	    GridBagLayout bag = new GridBagLayout();
	    panel.setLayout(bag);

	    
	    label = new JLabel("Column delimiter  ");
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    c.anchor = GridBagConstraints.EAST;
	    bag.setConstraints(label, c);
	    panel.add( label );


	    String[] delim_str = { "Tab", "Space", "Comma" };
	    delim_jcb = new JComboBox(delim_str);
	    delim_jcb.setSelectedIndex(mview.getIntProperty("saveastext.delim", 0));

	    delim_jcb.addActionListener(new ActionListener()
				       {
					   public void actionPerformed(ActionEvent e) 
					   {
					       updateSample();
					   }
				       });

	    //jcb.setSelectedIndex( edata.getMeasurementDataType(s) );
	    //jcb.addActionListener(new SetDataTypeListener(s) );

	    delim_jcb.setToolTipText("Which character to put between columns");

	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.anchor = GridBagConstraints.WEST;
	    c.weightx = 1.0;
	    bag.setConstraints(delim_jcb, c);
	    panel.add(delim_jcb);
	    
	    
	    line++;

	    label = new JLabel("Significant digits  ");
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.weighty = 1.0;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.EAST;
	    bag.setConstraints(label, c);
	    panel.add(label);

	    
	    sig_dig_slider = new LabelSlider( null, JSlider.VERTICAL, JSlider.HORIZONTAL, 0, 32, 6);
	    sig_dig_slider.setMode(LabelSlider.INTEGER);
	    sig_dig_slider.setValue(mview.getIntProperty("saveastext.sig_digs", 6));
	    sig_dig_slider.addChangeListener(new ChangeListener()
					     {
						 public void stateChanged(ChangeEvent e) 
						 {
						     updateSample();
						 }
					     });
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    // c.fill = GridBagConstraints.HORIZONTAL;
	    bag.setConstraints(sig_dig_slider, c);
	    panel.add(sig_dig_slider);
	    
	    line++;
	    
	    label = new JLabel("Missing value  ");
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    c.anchor = GridBagConstraints.EAST;
	    bag.setConstraints(label, c);
	    panel.add( label );


	    blank_jtf = new JTextField(10);
	    blank_jtf.setText(mview.getProperty("saveastext.missing_value", "") );
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.weightx = 1.0;
	    // c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    bag.setConstraints( blank_jtf, c );
	    panel.add( blank_jtf );
	    blank_jtf.addActionListener(new ActionListener()
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			updateSample();
		    }
		});

	    line++;
	    
	 
	    tabbed.add( " Formatting options ", panel);
	} 


	// ==========================================================================
	// columns:
	// ==========================================================================

	{
	    line = 0;

	    JPanel panel = new JPanel();
	    panel.setBorder(BorderFactory.createEmptyBorder(3,5,3,5));
	    GridBagLayout bag = new GridBagLayout();
	    panel.setLayout(bag);
	    
	    meas_labels_jchkb = new JCheckBox("Include column labels");
	    meas_labels_jchkb.setSelected(mview.getBooleanProperty("saveastext.meas_label_headings", true));

	    meas_labels_jchkb.addActionListener(new ActionListener()
					 {
					     public void actionPerformed(ActionEvent e) 
					     {
						 updateSample();
					     }
					 });

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    bag.setConstraints(meas_labels_jchkb, c);
	    panel.add(meas_labels_jchkb);
	    
	    line++;


	    

	    tidy_col_names_jchkb = new JCheckBox("Remove whitespace from labels");
	    tidy_col_names_jchkb.setSelected(mview.getBooleanProperty("saveastext.remove_whitespace", true));
	    tidy_col_names_jchkb.addActionListener(new ActionListener()
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			updateSample();
		    }
		});

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.NORTHWEST;
	    bag.setConstraints(tidy_col_names_jchkb, c);
	    panel.add(tidy_col_names_jchkb);
	    
	    
	    line++;

	    label = new JLabel("Include which Measurements?");
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.anchor = GridBagConstraints.WEST;
	    bag.setConstraints(label, c);
	    panel.add(label);

	    line++;

	    meas_list = new DragAndDropList();
			
	    meas_list.addListSelectionListener( new ListSelectionListener()
		{
		    public void valueChanged(ListSelectionEvent e) 
		    {
			updateSample();
		    }
		});

	    meas_list.setToolTipText("Which Measurements to include");

	    JScrollPane jsp = new JScrollPane(meas_list);

	    panel.add(jsp);
	    
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.anchor = GridBagConstraints.WEST;
	    c.weightx = 10.;
	    c.weighty = 10.;
	    c.fill = GridBagConstraints.BOTH;
	    bag.setConstraints(jsp, c);


	    line++;

	    addAllAndNoneButtons( panel, bag, 0, line, meas_list );

	    line++;

	    include_spot_attrs_jchkb = new JCheckBox("Show Spot Attributes in list");
	    //include_spot_attrs_jchkb.setFont(mview.getSmallFont());
	    
	    include_spot_attrs_jchkb.addActionListener(new ActionListener()
		{
		    public void actionPerformed(ActionEvent e) 

		    {
			populateMeasList(null);
			updateSample();
		    }
		});
	    
	    
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.weightx = 10.0;
	    c.anchor = GridBagConstraints.WEST;
	    bag.setConstraints(include_spot_attrs_jchkb, c);
	    panel.add(include_spot_attrs_jchkb);

	    line++;

	    expand_spot_attrs_jchkb = new JCheckBox("Expand Spot Attribute names in output");
	    //expand_spot_attrs_jchkb.setFont(mview.getSmallFont());
	    
	    expand_spot_attrs_jchkb.addActionListener(new ActionListener()
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			populateMeasList(null);
			updateSample();
		    }
		});
	    
	    
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.weightx = 10.0;
	    c.anchor = GridBagConstraints.WEST;
	    bag.setConstraints(expand_spot_attrs_jchkb, c);
	    panel.add(expand_spot_attrs_jchkb);


	    line++;

	    
	    
	    tabbed.add( " Column contents ", panel);


	}

	// ==========================================================================
	// rows:
	// ==========================================================================

	{
	    line = 0;
	    
	    JPanel panel = new JPanel();
	    GridBagLayout bag = new GridBagLayout();
	    panel.setBorder(BorderFactory.createEmptyBorder(3,5,3,5));
	    panel.setLayout(bag);

	    
	    apply_filter_jchkb = new JCheckBox("Apply filter");
	    apply_filter_jchkb.setSelected(mview.getBooleanProperty("saveastext.apply_filter", true));
				     
	    apply_filter_jchkb.addActionListener(new ActionListener()
					 {
					     public void actionPerformed(ActionEvent e) 
					     {
						 updateSample();
					     }
					 });

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.weightx = 1.0;
	    c.gridwidth = 2;
	    c.anchor = GridBagConstraints.WEST;
	    bag.setConstraints(apply_filter_jchkb, c);
	    panel.add(apply_filter_jchkb);
	    
	    
	    line++;

	    meas_attr_labels_jchkb = new JCheckBox("Include row labels for Measurement Attributes");
	    meas_attr_labels_jchkb.setSelected(mview.getBooleanProperty("saveastext.meas_attr_label_headings", true));

	    meas_attr_labels_jchkb.addActionListener(new ActionListener()
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			updateSample();
		    }
		});

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.weightx = 1.0;
	    c.gridwidth = 2;
	    c.anchor = GridBagConstraints.WEST;
	    bag.setConstraints(meas_attr_labels_jchkb, c);
	    panel.add(meas_attr_labels_jchkb);


	    line++;


	    tidy_row_names_jchkb = new JCheckBox("Remove whitespace from labels");
	    tidy_row_names_jchkb.setSelected(mview.getBooleanProperty("saveastext.row_label_remove_whitespace", 
								      true));
	    tidy_row_names_jchkb.addActionListener(new ActionListener()
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			updateSample();
		    }
		});

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.weightx = 1.0;
	    c.gridwidth = 2;
	    c.anchor = GridBagConstraints.NORTHWEST;
	    bag.setConstraints(tidy_row_names_jchkb, c);
	    panel.add(tidy_row_names_jchkb);
	   

	    line++;


	    
	    label = new JLabel("Measurement Attributes");
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.weightx = 1.0;
	    //c.anchor = GridBagConstraints.WEST;
	    bag.setConstraints(label, c);
	    panel.add(label);
	    

	    label = new JLabel("Spot Names and Attributes");
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.weightx = 1.0;
	    //c.anchor = GridBagConstraints.WEST;
	    bag.setConstraints(label, c);
	    panel.add(label);
	    

	    line++;


	    meas_attr_labels_list = new JList();
	    
	    populateMeasAttrsList();
	    
	    meas_attr_labels_list.addListSelectionListener(new ListSelectionListener()
		{
		    public void valueChanged(ListSelectionEvent e) 
		    {
			updateSample();
		    }
		});

	    JScrollPane jsp = new JScrollPane(meas_attr_labels_list);
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.anchor = GridBagConstraints.WEST;
	    c.weightx = 5.;
	    c.weighty = 10.;
	    c.fill = GridBagConstraints.BOTH;
	    bag.setConstraints(jsp, c);
	    panel.add(jsp);
	    
	    spot_name_attr_labels_list = new JList();
	    populateSpotNameAttrsList();
	    spot_name_attr_labels_list.addListSelectionListener(new ListSelectionListener()
		{
		    public void valueChanged(ListSelectionEvent e) 
		    {
			updateSample();
		    }
		});

	    jsp = new JScrollPane(spot_name_attr_labels_list);
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.anchor = GridBagConstraints.WEST;
	    c.weightx = 5.;
	    c.weighty = 10.;
	    c.fill = GridBagConstraints.BOTH;
	    bag.setConstraints(jsp, c);
	    panel.add(jsp);
	    
	    
	    
	    line++;
	    
	    addAllAndNoneButtons( panel, bag, 0, line, spot_name_attr_labels_list );
	    addAllAndNoneButtons( panel, bag, 1, line, meas_attr_labels_list );


	    line++;
		    
	    // - - - - - - - - - - - - - - - - - -  - - 

	    
	    tabbed.add( " Row contents ", panel);

	}



	{
	    JPanel panel = new JPanel();
	    GridBagLayout bag = new GridBagLayout();
	    panel.setLayout(bag);

	    view_table = new JTable();
	    view_table.setBackground(export_panel.getBackground());
	    view_table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	    view_table.setFont(mview.getSmallFont());
	    view_table.setRowSelectionAllowed(false);
	    view_table.setColumnSelectionAllowed(false);
	    view_table.setCellSelectionEnabled(false);
	    view_table.setDefaultEditor(String.class, null);

	    // embed the text viewer in a scroll pane
	    JScrollPane view_scroller = new JScrollPane(view_table);
	     c = new GridBagConstraints();
	    c.fill = GridBagConstraints.BOTH;
	    c.weighty = 10.;
	    c.weightx = 10.;
	    bag.setConstraints(view_scroller, c);
	    panel.add(view_scroller);
	    

	    tabbed.add( " Preview ", panel);

	}

	 c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	c.fill = GridBagConstraints.BOTH;
	c.weightx = 10.0;
	c.weighty = 9.0;
	gridbag.setConstraints(tabbed, c);
	export_panel.add(tabbed);


	// ==========================================================================
	// and finally, the control buttons
	// ==========================================================================

	JPanel buttons_panel = new JPanel();
	GridBagLayout inner_gridbag = new GridBagLayout();
	buttons_panel.setLayout(inner_gridbag);
	buttons_panel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
	
	{
	    compress_jchkb = new JCheckBox("Compress with GZIP");
	    compress_jchkb.setSelected(mview.getBooleanProperty("saveastext.compress", true));
	    
	    compress_jchkb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			// view_panel.scrollRectToVisible(new Rectangle(20,20));
		    }
		});
	    
	    
	    buttons_panel.add(compress_jchkb);
	    
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.anchor = GridBagConstraints.WEST;
	    inner_gridbag.setConstraints(compress_jchkb, c);
	}
	{   
	    final JButton jb = new JButton("Save");
	    buttons_panel.add(jb);
	    
	    jb.setToolTipText("Ready to choose a filename...");
	    
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			
			File file = pickFileName( );
			
			if(file != null)
			    exportData( file );
			
			// save all settings 
			//
			saveProps();
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    inner_gridbag.setConstraints(jb, c);
	}
	{   
	    final JButton jb = new JButton("Help");
	    buttons_panel.add(jb);
	    
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			mview.getPluginHelpTopic("SaveAsText", "SaveAsText");
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = 3;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.EAST;
	    inner_gridbag.setConstraints(jb, c);
	}
	{   
	    final JButton jb = new JButton("Close");
	    buttons_panel.add(jb);
	    
	    jb.setToolTipText("Close this dialog...");
	    
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			saveProps();
			cleanUp();
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = 4;
	    c.anchor = GridBagConstraints.EAST;
	    inner_gridbag.setConstraints(jb, c);
	}
	
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 1;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.weightx = 10.0;
	gridbag.setConstraints(buttons_panel, c);
	export_panel.add(buttons_panel);
	

	
	populateMeasList(null);
	updateSample();
	mview.getExprData().addObserver(this);

	frame.pack();
	frame.setVisible(true);
    }
    
    public void stopPlugin()
    {
	cleanUp();
    }


    public void cleanUp()
    {
	mview.getExprData().removeObserver(this);
	frame.setVisible(false);
    }

    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("Save As Text", "exporter", 
							"Write data in a tabular ASCII format", "",
							1, 1, 0);
	return pinf;
    }

    private void saveProps()
    {
	mview.putIntProperty("saveastext.sig_digs", (int) sig_dig_slider.getValue());
	mview.putIntProperty("saveastext.delim", delim_jcb.getSelectedIndex());
	mview.putBooleanProperty("saveastext.measure_headings",meas_labels_jchkb.isSelected());
	mview.putBooleanProperty("saveastext.apply_filter", apply_filter_jchkb.isSelected());
	mview.putBooleanProperty("saveastext.col_labels_remove_whitespace", tidy_col_names_jchkb.isSelected());
	mview.putBooleanProperty("saveastext.row_labels_remove_whitespace", tidy_row_names_jchkb.isSelected());
	mview.putBooleanProperty("saveastext.meas_attr_label_headings", meas_attr_labels_jchkb.isSelected());
	mview.putBooleanProperty("saveastext.compress", compress_jchkb.isSelected());
    }
    
    public PluginCommand[] getPluginCommands()
    {
	PluginCommand[] com = new PluginCommand[3];
	
	String[] args = new String[] 
	{ 
	    // name                 // type           //default   // flag   // comment
	    "file",                 "file",           "",         "",       "destination file name",
	    "significant_digits",   "integer",        "10",       "",       "", 
	    "delimiter",            "string",         "tab",      "",       "one of 'tab', 'space' or 'comma'", 
	    "missing_value",        "string",         "",      "",          "the string to interpret as a blank or missing value", 
	    "apply_filter",         "boolean",        "false",    "",       "", 

	    "which_columns",         "string",                          "all",     "",       "either 'all', 'list' or 'selection'", 
	    "column_list",           "measurement_or_spot_attr_names",  "",        "",       "the Measurements or Spot Attributes to include when 'which_columns'='list'", 
	    "tidy_column_labels",    "boolean",                         "false",   "",       "replace spaces with underscores in column headings", 	

	    "row_labels",            "name_tag_selection",              "",         "",   "which Names or Name Attributes to include", 
	    "include_column_labels", "boolean",                         "true",     "",   "include headings for Names or Name Attributes", 
	    "tidy_row_labels",       "boolean",                         "false",    "",   "replace spaces with underscores in row labels (i.e. Names or Name Attributes)", 

	    "compress",              "boolean",        "false",    "",       "compress the output file with GZIP", 
	    "force_overwrite",       "boolean",        "false",    "",       "overwrite file of the same name if present",
	    "report_status",         "boolean",        "true",     "",       "show either success or failure message after saving"
	};
	
	//com[0] = new PluginCommand("start", "display the interface", null);
	//com[1] = new PluginCommand("set",  "set one or more parameters", args);
	//com[2] = new PluginCommand("save", "set one or more parameters then write file", args);
	
	com[0] = new PluginCommand("start", null);
	com[1] = new PluginCommand("set",   args);
	com[2] = new PluginCommand("save",  args);

	return com;
    }

    public void runCommand(String name, String[] args, CommandSignal done) 
    { 
	// 
	if(name.equals("start"))
	{
	    startPlugin();
	}

	
	if(name.equals("save") || name.equals("set"))
	{
	    if(frame == null)
		startPlugin();
	    
	    String dname = mview.getPluginStringArg("delimiter", args, "tab");

	    if(dname != null)
	    {
		dname = dname.toLowerCase();
		if(dname.equals("tab"))
		    delim_jcb.setSelectedIndex(0);
		if(dname.equals("space"))
		    delim_jcb.setSelectedIndex(1);
		if(dname.equals("comma"))
		    delim_jcb.setSelectedIndex(2);
	    }

	    sig_dig_slider.setValue( mview.getPluginIntegerArg("significant_digits", args, 10) );
	    
	    blank_jtf.setText( mview.getPluginStringArg("missing_value", args, "") );
	    
	     /*
	    String mmode = mview.getPluginStringArg("which_measurements", args, "all");
	    if(mmode != null)
	    {
		mmode = mmode.toLowerCase();
		if(mmode.startsWith("currently enabled"))
		    meas_jcb.setSelectedIndex(0);
		if(mmode.startsWith("all"))
		    meas_jcb.setSelectedIndex(1);
	    }
	    */

	    apply_filter_jchkb.setSelected( mview.getPluginBooleanArg("apply_filter", args, false) );
	    
	    // -------------------
	    
	    String n_mode = (mview.getPluginStringArg("which_columns", args, "all")).toLowerCase();
	    if(n_mode.equals("all"))
	    {
		//System.out.println("including all");
		meas_list.selectAll();
	    }
	    if(n_mode.equals("selection"))
	    {
		//System.out.println("including selection");
		
		int[] m_sel_ids = edata.getMeasurementSelection();

		MeasSpotAttrID[] m_ids = new MeasSpotAttrID[ m_sel_ids.length ];

		for(int m=0; m < m_sel_ids.length; m++)
		{
		    //System.out.println(" + " + edata.getMeasurementName(m_sel_ids[m]));
		    
		    m_ids[m] = new MeasSpotAttrID(m_sel_ids[m]);
		}

		populateMeasList( m_ids );
	    }
	    if(n_mode.equals("list"))
	    {
		System.out.println("including list of names");
		
		MeasSpotAttrID[] m_and_sa_ids =  mview.getPluginMeasurementOrSpotAttrListArg("column_list", args, null);
		
		boolean list_contains_spot_attrs = false;

		for(int n=0; n < m_and_sa_ids.length; n++)
		{
		    System.out.println(" +  " + m_and_sa_ids[n].meas_id + "," + m_and_sa_ids[n].spot_attr_id);
		    
		    if( m_and_sa_ids[n].isSpotAttr() )
			list_contains_spot_attrs = true;
		}
		
		include_spot_attrs_jchkb.setSelected( list_contains_spot_attrs );

		populateMeasList( m_and_sa_ids );
	    }
	    
	    meas_labels_jchkb.setSelected( mview.getPluginBooleanArg("include_column_labels", args, true) );

	    tidy_col_names_jchkb.setSelected( mview.getPluginBooleanArg("tidy_column_labels", args, false) );
	    
	    // -------------------

	    //ExprData.NameTagSelection nts = mview.getPluginNameTagSelectionArg("row_labels", args, null);
	    //if(nts != null)
	    //	spot_labels_nts.setNameTagSelection( nts );

	    tidy_row_names_jchkb.setSelected( mview.getPluginBooleanArg("tidy_row_labels", args, false) );

	    compress_jchkb.setSelected( mview.getPluginBooleanArg("compress", args, false) );

	    // -------------------

	    updateSample();

	    if(name.equals("save"))
	    {
		String fname = mview.getPluginStringArg("file", args, null);
		if(fname != null)
		{
		    report_status   =  mview.getPluginBooleanArg("report_status", args, true);
		    force_overwrite =  mview.getPluginBooleanArg("force_overwrite", args, false);
		    
		    File file = new File( fname );
		    exportData( file );
		}
		else
		{
		    // no file specified in the args; pop up a file chooser
		    
		    File file = pickFileName( );
		    if(file != null)
			exportData( file );
		}
		
		cleanUp();
	    }
	}
	
	if(done != null)
	    done.signal();
    } 

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private void addAllAndNoneButtons( JPanel panel, GridBagLayout bag, int col, int row, final JList list )
    {
	JPanel inner = new JPanel();
	inner.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
	JButton all = new JButton("All");
	all.setMargin(new Insets(0,2,0,2));
	all.setFont(mview.getSmallFont());
	inner.add(all);
	JButton none = new JButton("None");
	none.setFont(mview.getSmallFont());
	none.setMargin(new Insets(0,2,0,2));
	inner.add(none);

	GridBagConstraints c = new GridBagConstraints();
	c.gridx = col;
	c.gridy = row;
	c.anchor = GridBagConstraints.NORTHEAST;
	c.weightx = 1;
	bag.setConstraints(inner, c);
	panel.add(inner);

	all.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    list.addSelectionInterval(0, list.getModel().getSize()-1 );
		}
	    });
	none.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    list.clearSelection(); 
		}
	    });
   
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private File pickFileName()
    {
	JFileChooser fc = mview.getFileChooser();

	fc.setCurrentDirectory(new File(mview.getProperty("saveastext.current_directory", ".")));
	
	int returnVal = fc.showSaveDialog(export_panel);
	if (returnVal == JFileChooser.APPROVE_OPTION) 
	{
	    File file = mview.getFileChooser().getSelectedFile();
	    
	    if( file.getParent() != null)
		mview.putProperty("saveastext.current_directory", file.getParent() );
	    
	    // check whether file already exists....
	    if(file.exists())
	    {
		if(mview.infoQuestion("File exists, overwrite?", "No", "Yes") == 0)
		    return null;
	    }
	    
	    return file;
	}
	
	return null;
	
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- measurement ( & Spot attribute) list   --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private void populateMeasList( MeasSpotAttrID[] new_selection )
    {
	int count = 0;
	
	MeasSpotAttrID[] selection = null;

	Vector res = new Vector();

	int[] sel_ids = null;
	int sel_p = 0;
	
	if(new_selection == null)
	{
	    // save the current selection
	    int[] curr_sel_ids = meas_list.getSelectedIndices();
	    
 	    if(( curr_sel_ids != null ) && ( meas_list_index_to_data_ht != null ))
	    {
		sel_ids = new int[ curr_sel_ids.length ];
		selection = new MeasSpotAttrID[ curr_sel_ids.length ];

		for(int i=0; i < curr_sel_ids.length; i++)
		{
		    MeasSpotAttrID msa = (MeasSpotAttrID) meas_list_index_to_data_ht.get( new Integer( curr_sel_ids[i]) );
		    
		    selection[i] = msa;
		}
	    }
	}
	else
	{
	    selection = new_selection;
	    sel_ids = new int[ selection.length ];
	}

	meas_list_index_to_data_ht = new Hashtable();

	MeasSpotAttrID msa_id;
	
	boolean expand_spot_attr_names = expand_spot_attrs_jchkb.isSelected();

	for(int m=0; m < edata.getNumMeasurements(); m++)
	{
	    int m_id = edata.getMeasurementAtIndex(m);
	    
	    res.addElement( edata.getMeasurementName( m_id ) );
	    
	    msa_id = new MeasSpotAttrID( m_id );
	    
	    if( isSelected(msa_id, selection) )
		sel_ids[sel_p++] = count;

	    meas_list_index_to_data_ht.put( new Integer( count++),  msa_id );
	    
	    
	    if(include_spot_attrs_jchkb.isSelected())
	    {
		ExprData.Measurement meas = edata.getMeasurement( m_id );
		
		for(int a=0; a < meas.getNumSpotAttributes(); a++)
		{
		    if(expand_spot_attr_names)
			res.addElement( "  " + edata.getMeasurementName( m_id ) + "." + meas.getSpotAttributeName( a ) );
		    else
			res.addElement( "  " + meas.getSpotAttributeName( a ) );
		    
		    msa_id = new MeasSpotAttrID( m_id, a );
	    
		    if( isSelected(msa_id, selection) )
			sel_ids[sel_p++] = count;
		    
		    meas_list_index_to_data_ht.put( new Integer( count++), msa_id );
		}
	    }
	}

	meas_list.setListData( res );

	if(selection != null)
	{
	    meas_list.setSelectedIndices(sel_ids);
	}
    }

    private boolean isSelected( MeasSpotAttrID msa_id, MeasSpotAttrID[] selection )
    {
	if(selection == null)
	    return false;
	
	for(int i=0; i < selection.length; i++)
	    if((selection[i].meas_id == msa_id.meas_id) && (selection[i].spot_attr_id == msa_id.spot_attr_id))
		return true;

	return false;
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   exportData()
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void exportData(File file)
    {
	try
	{
	    BufferedWriter writer = null;

	    if(compress_jchkb.isSelected())
	    {
		// check for a .gz extension...
		String name = file.getPath();
		String new_name = new String(name);
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
		//System.out.println("file renamed to " + new_name);

		if(!force_overwrite)
		{
		    if(file.exists())
		    {
			if(mview.infoQuestion("File exists, overwrite?", "No", "Yes") == 0)
			    return;
		    }
		}
						 

		file = new File(new_name);

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

	    int lines = writeData(writer, -1);

	    writer.close();

	    if(report_status)
		mview.successMessage(lines + " lines written to " + file.getName());
	    
	    //System.out.println(lines + " lines written to " + file.getName());
	}
	catch (java.io.IOException ioe)
	{
	    mview.alertMessage("Unable to write\n"  + ioe);
	}
    }

    public void updateSample()
    {
	new SampleUpdater().start();
    }
    private class SampleUpdater extends Thread
    {
	public void run()
	{
	    //StringWriter sw = new StringWriter();
	    //BufferedWriter writer = new BufferedWriter(sw);

	    //	    int lines = writeData(sw, 20);

	    updateTable( 20 );

	    //int remain = (lines-20);
	    //if(remain > 0)
	    //sw.write("\n[ .." + ((remain==1) ? "one more line" : (remain + " more lines")) + ".. ]\n");


	    //System.out.println(lines  + " lines written to " + sw.toString());
	    
	    // view_panel.setText(sw.toString());
	    
	    // view_panel.scrollRectToVisible(new Rectangle(20,20));
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  generate the actual text
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    //
    // work out the maximum width of each column so everything can be suitably padded
    //
    private void analyseColumnWidths()
    {
	boolean skip;
	ExprData edata = mview.getExprData();

	// first, find the length of the longest number in each column
	//
	num_len = new int[edata.getNumMeasurements()];
	for(int m=0;m<edata.getNumMeasurements();m++)
	{
	   final int mi = edata.getMeasurementAtIndex(m);
		       
	   num_len[m] = edata.getMeasurementName(mi).length();
	   
	   for(int s=0;s<edata.getNumSpots();s++)
	   {
	       final int si = edata.getSpotAtIndex(s);
		
	       if((!use_filter) || (!edata.filter(si)))
	       {
		   final int len = String.valueOf(edata.eValueAtIndex(mi,si)).length();
	       
		   if(len > num_len[m])
		       num_len[m] = len;
	       }
	   }
	   num_len[m] += 1;
	}

	longest_name_len = 0;

	{
	    for(int s=0;s<edata.getNumSpots();s++)
	    {
		// if the filtering option is selected,
		// only examine spots
		//
		// .....
	    }
	}
    }

    public String tidyName(String n)
    {
	String t = n.trim();
	String r = t.replace('\t', '_');
	return r.replace(' ', '_');
    }



    // 
    //
    public Vector getLine(int line, ExprData.NameTagSelection nts)
    {
	Vector res = new Vector();

	final boolean tn = tidy_row_names_jchkb.isSelected();

	if(meas_attr_labels_jchkb.isSelected())
	{
	    Object[] meas_attr_sel = meas_attr_labels_list.getSelectedValues();
	    int count = (meas_attr_sel == null) ? 0 : meas_attr_sel.length;
	    
	    if(count > 0)
		res.addElement("");
	}

	if(nts.getNames() != null)
	{
	    String[] names = nts.getFullNameTagArray(line);
		
	    
	    if(names != null)
	    {
		for(int n=0; n < names.length; n++)
		{
		    if(tn && (names[n] != null))
			names[n] = tidyName(names[n]);
		    
		    res.addElement( names[n] );
		}
	    }
	}
	
	final int sig_digs = (int) sig_dig_slider.getValue();

	for(int m=meas_list.getMinSelectionIndex() ; m <= meas_list.getMaxSelectionIndex(); m++)
	{
	    if(meas_list.isSelectedIndex(m))
	    {
		MeasSpotAttrID msa = (MeasSpotAttrID) meas_list_index_to_data_ht.get( new Integer(m) );
			    
		String str;

		if(msa.isSpotAttr())
		{
		    // TODO: trim to sig.digits if spot attr type == double
		    str = edata.getMeasurement(msa.meas_id).getSpotAttributeDataValueAsString(msa.spot_attr_id, line);
		}
		else
		{
		    double d = edata.eValue(msa.meas_id,line);
		    if(Double.isNaN(d))
			str = blank_jtf.getText();
		    else
			str = mview.niceDouble(d, 100, sig_digs+1);
		}

		//if(tn && (str != null))
		//    str = tidyName(str);
		
		if(str == null)
		    str = blank_jtf.getText();

		res.addElement( str );
	    }
	}
	
	return res;
    }

    // 
    //  create a Vector containing the each values for the specified MeasurementAttribute
    //  in the order that the Measurements have been selected
    //
    public Vector getMeasurementAttrs( String name )
    {
	Vector result = new Vector();

	// System.out.println("m.attrs for " + name);

	for(int m=meas_list.getMinSelectionIndex() ; m <= meas_list.getMaxSelectionIndex(); m++)
	{
	    if(meas_list.isSelectedIndex(m))
	    {
		MeasSpotAttrID msa = (MeasSpotAttrID) meas_list_index_to_data_ht.get( new Integer(m) );
		
		ExprData.Measurement meas = edata.getMeasurement(msa.meas_id);

		String value = meas.getAttribute( name );

		result.addElement( value );

		// System.out.println("  " + meas.getName() + "=" + value);
	    }
	}

	return result;
    }

    // 
    //  create a Vector containing the column header labels
    //
    public Vector getHeader(ExprData.NameTagSelection nts)
    {
	Vector res = new Vector();

	boolean expand_spot_attr_names = expand_spot_attrs_jchkb.isSelected();

	if(meas_attr_labels_jchkb.isSelected())
	{
	    Object[] meas_attr_sel = meas_attr_labels_list.getSelectedValues();
	    int count = (meas_attr_sel == null) ? 0 : meas_attr_sel.length;

	    if(count > 0)
		res.addElement( tidy_col_names_jchkb.isSelected() ? "Measurement_Attribute" : "Measurement Attribute" );
	}
	
	if(nts.getNames() != null)
	{
	    String[] attrs_names = nts.getNamesArray();

	    for(int n=0; n < attrs_names.length; n++)
	    {
		if(meas_labels_jchkb.isSelected())
		{
		    res.addElement(tidy_col_names_jchkb.isSelected() ? tidyName(attrs_names[n]) : attrs_names[n]);
		}
	    }
	}
	for(int m=meas_list.getMinSelectionIndex() ; m <= meas_list.getMaxSelectionIndex(); m++)
	{
	    if(meas_list.isSelectedIndex(m))
	    {
		MeasSpotAttrID msa = (MeasSpotAttrID) meas_list_index_to_data_ht.get( new Integer(m) );
		
		// check for spaces in the set name as it is written
		// and replace them with '_'s
		//
		
		String clean_name;

		if(msa.isSpotAttr())
		{
		    if(expand_spot_attr_names)
			clean_name = edata.getMeasurementName(msa.meas_id) + "." + edata.getMeasurement(msa.meas_id).getSpotAttributeName(msa.spot_attr_id);
		    else
			clean_name = edata.getMeasurement(msa.meas_id).getSpotAttributeName(msa.spot_attr_id);
		}
		else
		{
		    clean_name = edata.getMeasurementName(msa.meas_id);
		}
		
		if(delim_jcb.getSelectedIndex() == 1)
		    clean_name = clean_name.replace(' ', '_');
		
		if(tidy_col_names_jchkb.isSelected())
		    clean_name = tidyName(clean_name);
		
		res.addElement(clean_name);
	    }
	}

	return res;
    }

    private int[] getColumnOrder( )
    {
	try
	{
	    TableColumnModel tcm = (TableColumnModel) view_table.getColumnModel();
	    TableModel        tm =       (TableModel) view_table.getModel();
	    
	    int[] order = new int[ tcm.getColumnCount() ];
	    for(int c=0; c < tcm.getColumnCount(); c++)
	    {
		//order[c] = tcm.getColumnIndexAtX(c);
		
		//order[c] = tcm.getColumnIndex( tcm.getColumn(c).getIdentifier() );
		
		int pos = tcm.getColumnIndex( tm.getColumnName(c) );
		
		//System.out.println( c + " = " + tm.getColumnName(c) + " -> " +  order[c] );
		
		order[ pos ] = c;
		
		//System.out.println( c + " : orig='" + 
		//		tm.getColumnName(c) + "' : actual='" +  
		//		tcm.getColumn(c).getIdentifier() + "'");
	    }

	    //for(int o=0; o < order.length; o++)
	    //  System.out.println( o + " : " + order[ o ] );
	    
	    return order;
	}
	catch(Exception ex)
	{
	    return null;
	}
    }

    private Vector reorderColumns( int[] order, Vector input )
    {
	Vector output = new Vector();

	for(int o=0; o < order.length; o++)
	{
	    output.addElement( input.elementAt( order[o] ));
	}

	return output;
    }

    // 
    //
    //public boolean hasHeader()
    //{
    //  // return (nts.getNames() != null);
    //}


    // 
    //
    public int updateTable(int max_lines)
    {
   
	//save the existing column ordering (if there is one)
	
	int[] current_order = getColumnOrder( );
	
	
	use_filter = apply_filter_jchkb.isSelected();
	
	Vector data = new Vector();

	ExprData.NameTagSelection nts =  makeNameTagSelection( spot_name_attr_labels_list );

	
	Vector line = null;
	
	// column labels
	
	if(meas_labels_jchkb.isSelected())
	    data.addElement( getHeader( nts ) );
	
	// measurement attributes

	Object[] meas_attr_sel = meas_attr_labels_list.getSelectedValues();
	if( meas_attr_sel != null )
	{
	    Object[] name_attrs_sel = spot_name_attr_labels_list.getSelectedValues();
	    int pad = (name_attrs_sel == null) ? 0 : name_attrs_sel.length;

	    for(int a=0; a < meas_attr_sel.length; a++)
	    {
		Vector m_attrs = getMeasurementAttrs( (String) meas_attr_sel[a] );

		// pad the elements to account for the columns containing Name and/or NameAttrs
		for(int p=0; p < pad; p++)
		    m_attrs.insertElementAt(null, 0);

		// and add the label for the MeasurementAttribute
		if(meas_attr_labels_jchkb.isSelected())
		    m_attrs.insertElementAt( (String) meas_attr_sel[a], 0 );

		data.addElement( m_attrs ); 
	    }
	}
	
	// data lines

	int l = 0;
	int data_lines = 0;

	while((data_lines < max_lines) && (l < edata.getNumSpots()))
	{
	    final int si = edata.getSpotAtIndex(l);
	    
	    if((!use_filter) || (!edata.filter(si)))
	    {
		line = getLine(  si, nts );
		data.addElement( line );
		
		data_lines++;
	    }

	    if(data_lines==max_lines)
	    {
		if(line != null)
		{
		    Vector ftr = new Vector();
		    for(int f=0; f < line.size(); f++)
			ftr.addElement("...");
		    data.addElement(ftr);
		}
	    }

	    l++;
	}

	if(line != null) // at least one line has been added
	{
	    Vector hdr = new Vector();
	    for(int h=0; h < line.size(); h++)
		hdr.addElement( String.valueOf(h+1) );
	    
	    view_table.setModel( new ImmutableTableModel( data, hdr ));

	    // re-install the previous column order
	    if(current_order != null)
	    {
		TableColumnModel tcm = (TableColumnModel) view_table.getColumnModel();
		TableModel        tm =       (TableModel) view_table.getModel();
 
		if(current_order.length == tcm.getColumnCount() )
		{
		    int iters = 0;
		    boolean correct = false;
		    while((iters < 10) && (!correct))
		    {
			correct = true;
			int[] order = getColumnOrder( );
			for(int o=0; o < order.length; o++)
			    if(order[o] != current_order[o])
				correct = false;

			if(!correct)
			{
			    System.out.println( "reordering...." + iters);
			    
			    for(int o=0; o < current_order.length; o++)
			    {
				
				// which column should be at this position ?
				
				System.out.println( current_order[ o ] + " -> " + o );
				
				int target = current_order[o];
				
				// where is this column now?
				
				int pos = tcm.getColumnIndex( tm.getColumnName(o) );
				
				// move it 
				
				tcm.moveColumn( pos, target );
			    }
			    
			    iters++;
			}
		    }
		}
	    }
	}

	return max_lines;
    }

    class ImmutableTableModel extends DefaultTableModel
    {
	public ImmutableTableModel( Vector data, Vector cnames )
	{
	    super( data,cnames );
	}

	public boolean isCellEditable(int row, int col) 
	{ 
	    return false;
	}
    }

    // write the data to a Writer 
    //
    public int writeData(Writer writer, int max_lines)
    {
	use_filter = apply_filter_jchkb.isSelected();
	
	// ExprData.NameTagSelection nts = spot_labels_nts.getNameTagSelection();

	Vector line;
	int good_lines = 0;  // the actual number of lines that would be written ignoring 'max_lines'


	try
	{

	    int[] order = getColumnOrder();

	    char delim_char;
	    switch(delim_jcb.getSelectedIndex())
	    {
		case 0:
		    delim_char = '\t';
		    break;
	        case 1:
		    delim_char = ' ';
		    break;
	        default:
		     delim_char = ',';
		    break;
	    }

	    ExprData.NameTagSelection nts = makeNameTagSelection( spot_name_attr_labels_list );

	    // column labels
	    
	    if(meas_labels_jchkb.isSelected())
	    {
		line = getHeader( nts );
		line = reorderColumns( order, line );
		for(int n=0; n < line.size(); n++)
		{
		    if(n > 0)
			writer.write(delim_char);

		    writer.write((String)line.elementAt(n));
		}
		writer.write('\n');
	    }

	    // measurement attributes
	    
	    Object[] meas_attr_sel = meas_attr_labels_list.getSelectedValues();
	    if( meas_attr_sel != null )
	    {
		Object[] name_attrs_sel = spot_name_attr_labels_list.getSelectedValues();
		int pad = (name_attrs_sel == null) ? 0 : name_attrs_sel.length;
		
		
		for(int a=0; a < meas_attr_sel.length; a++)
		{
		    Vector m_attrs = getMeasurementAttrs( (String) meas_attr_sel[a] );
		    
		    // pad the elements to account for the columns containing Name and/or NameAttrs
		    for(int p=0; p < pad; p++)
		    {
			m_attrs.insertElementAt( "", 0 );
		    }
		    
		    // and add the label for the MeasurementAttribute
		    if(meas_attr_labels_jchkb.isSelected())
		    {
			m_attrs.insertElementAt( (String) meas_attr_sel[a], 0 );
		    }

		    m_attrs = reorderColumns( order,  m_attrs );
		    
		    for(int n=0; n < m_attrs.size(); n++)
		    {
			if(n > 0)
			    writer.write(delim_char);
			
			if( m_attrs.elementAt(n) != null )
			    writer.write( (String) m_attrs.elementAt( n ) );
		    }
		    
		    writer.write('\n');
		}
	    }
	
	    // and the data lines

	    for(int l=0; l < edata.getNumSpots(); l++)
	    {
		final int si = edata.getSpotAtIndex(l);
		
		if((!use_filter) || (!edata.filter(si)))
		{
		    try
		    {
			line = getLine(  si, nts );
			line = reorderColumns( order, line );
			
			for(int n=0; n < line.size(); n++)
			{
			    if(n > 0)
				writer.write(delim_char);
			    
			    String val = (String)line.elementAt(n);

			    if(val != null)
				writer.write(val);
			}
		    }
		    catch(NullPointerException npe)
		    {
			System.out.println("warning: line not written");
			// npe.printStackTrace();
		    }

		    good_lines++;
		    writer.write('\n');
		}
	    }
	}
	catch (java.io.IOException ioe)
	{
	    mview.alertMessage("Unable to write file\n\n" + ioe);
	}
	
	return good_lines;
	
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  MeasurementAttributes
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
 
    
    Hashtable id_to_attr_name = null;   // converts ButtonMenu id's back into MeasAttr names

    public void populateMeasAttrsList()
    {
	// build a list of all possible attrs in all (selected?) Measurements

	java.util.HashSet uniq = new java.util.HashSet();

	for(int m=0;m<edata.getNumMeasurements();m++)
	{
	    ExprData.Measurement ms = edata.getMeasurement(m);
	    for (java.util.Enumeration e = ms.getAttributes().keys(); e.hasMoreElements() ;) 
	    {
		final String name = (String) e.nextElement();
		uniq.add(name);
	    }
	}
	
	Vector vec = new Vector();

	java.util.Iterator it = uniq.iterator();
	while(it.hasNext())
	    vec.addElement( it.next() );

	String[] all_attr_names = (String[]) vec.toArray( new String[0] );
	java.util.Arrays.sort(all_attr_names);
	
	meas_attr_labels_list.setListData( all_attr_names );

    }
	    
    public void populateSpotNameAttrsList()
    {
	Vector names = new Vector();
	ExprData.TagAttrs ta;

	names.addElement("Gene name(s)");

	ta = edata.getGeneTagAttrs();
	for(int a=0; a < ta.getNumAttrs(); a++)
	    names.addElement( "  " + ta.getAttrName(a) );

	names.addElement("Probe name");

	ta = edata.getProbeTagAttrs();
	for(int a=0; a < ta.getNumAttrs(); a++)
	    names.addElement( "  " + ta.getAttrName(a) );
 
	names.addElement("Spot name");

	ta = edata.getSpotTagAttrs();
	for(int a=0; a < ta.getNumAttrs(); a++)
	    names.addElement( "  " + ta.getAttrName(a) );

	spot_name_attr_labels_list.setListData( names );

   }
    
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private ExprData.NameTagSelection makeNameTagSelection( JList list )
    {
	StringBuffer buf = new StringBuffer();

	Object[] selection = list.getSelectedValues();
	
	ExprData.NameTagSelection nts = edata.new NameTagSelection();

	if( selection != null )
	{
	    String[] selection_s = new String[ selection.length ];
	    for( int s = 0 ; s < selection.length; s++ )
		selection_s[ s ] = (String) selection[ s ];

	    nts.setNames( selection_s );
	}

	return nts;
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
	populateMeasList(null);
	populateMeasAttrsList();
	updateSample();
    }

    public void measurementUpdate(ExprData.MeasurementUpdateEvent mue)
    {
	populateMeasList(null);
	populateSpotNameAttrsList();
	updateSample();
    }

    public void clusterUpdate(ExprData.ClusterUpdateEvent cue)
    {
    }

    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  stuff
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    private maxdView mview;
    private ExprData edata;
    private DataPlot dplot;

    private JPanel export_panel;
    // private JTextArea view_panel;
    private LabelSlider sig_dig_slider;

    private JFrame frame = null;

    private Hashtable meas_list_index_to_data_ht;

    private boolean use_filter = true;

    private JComboBox delim_jcb, spots_jcb;
    //     private NameTagSelector spot_labels_nts;
    private JCheckBox format_jcb, meas_labels_jchkb, meas_attr_labels_jchkb;
    private JProgressBar export_progress;
    private JCheckBox compress_jchkb, apply_filter_jchkb, tidy_col_names_jchkb, tidy_row_names_jchkb;
    private DragAndDropList meas_list;
    private JCheckBox include_spot_attrs_jchkb;
    private JCheckBox expand_spot_attrs_jchkb;
    private JTable view_table;
    private boolean report_status   = true;
    private boolean force_overwrite = false;
    private JTextField blank_jtf;

    private JList meas_attr_labels_list, spot_name_attr_labels_list;

    private int longest_name_len;
    private int[] num_len;
}
