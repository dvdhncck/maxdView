import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;


public class MeasurementManager implements ExprData.ExprDataObserver, Plugin, ExprData.ExternalSelectionListener
{
    final String[] dataTypeStrings = 
    { "Abs. Expression", 
      "Ratio Expression", 
      "Probability", 
      "Error Value", 
      "Unknown" 
    };
	
    public MeasurementManager(final maxdView m_viewer)
    {
	// super("Measurement Properties");

	mview = m_viewer;
	edata = mview.getExprData();
	dplot = mview.getDataPlot();

    }

    public void cleanUp()
    {
	edata.removeObserver(this);

	if(export_attr_frame != null)
	    export_attr_frame.setVisible(false);
	if(import_attr_frame != null)
	    import_attr_frame.setVisible(false);

	if(esl_handle >=0 )
	    edata.removeExternalSelectionListener(esl_handle);
	
	mview.putIntProperty( "MeasurementManager.tab", tabbed.getSelectedIndex() );
	
	mview.putBooleanProperty("MeasurementManager.search_case_sens", search_case_sens);
	mview.putBooleanProperty("MeasurementManager.search_substring", search_substring);
	mview.putProperty("MeasurementManager.search_string", search_string);

	mview.putBooleanProperty("MeasurementManager.orient_stats_by_meas", orient_stats_by_meas);
	mview.putBooleanProperty("MeasurementManager.lock_stats_table_width", lock_stats_table_width);

	frame.setVisible(false);
    }


    //
    // ========================================================================================
    // ------ plugin implementation -----------------------------------------------------------
    // ========================================================================================
    //

    public void startPlugin()
    {
	buildGUI();

	frame.addWindowListener(new WindowAdapter() 
	    {
		public void windowClosing(WindowEvent e)
		{
		    cleanUp();
		}
	    });

	
	frame.pack();
	frame.setVisible(true);


	edata.addObserver(this);
	esl_handle = edata.addExternalSelectionListener(this);
    }
   
    public void stopPlugin()
    {
	cleanUp();
    }

    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("Measurement Manager", "viewer", 
					 "View and manipulate Measurement properties and attributes", 
					 "", 1, 0, 0);
	return pinf;
    }

    public PluginCommand[] getPluginCommands()
    {
	PluginCommand[] com = new PluginCommand[2];
       
	String[] show_args = new String[] { "name", "string", "", "m", "name of Measurement to display" };

	com[0] = new PluginCommand("showProperties", show_args);

	com[1] = new PluginCommand("showAttributes", show_args);

	return com;
    }
 
    public void   runCommand(String name, String[] args, CommandSignal done) 
    {
	if(name.equals("showProperties"))
	{
	    String mname = mview.getPluginArg("name", args);
	    System.out.println("sp: name=" + mname);

	    startPlugin();
	    selectMeasurement(mname);
	    selectPropertiesTab();
	}

	if(name.equals("showAttributes"))
	{
	    String mname = mview.getPluginArg("name", args);
	    System.out.println("sa: name=" + mname);

	    startPlugin();
	    selectMeasurement(mname);
	    selectAttributesTab();
	}
    }

    //
    // ========================================================================================
    // ------ command handlers ----------------------------------------------------------------
    // ========================================================================================
    //


    public void selectMeasurement(final String mname)
    {
	if((meas_list != null) && (mname != null))
	{
	    meas_list.setSelectedValue(mname, true);
	    updatePropertyDisplay();
	}
    }

    public void selectPropertiesTab()
    {
	tabbed.setSelectedIndex(0);
    }
    public void selectAttributesTab()
    {
	tabbed.setSelectedIndex(1);
    }
    public void selectStatisticsTab()
    {
	tabbed.setSelectedIndex(2);
    }

    //
    // ========================================================================================
    // ------ new gui -------------------------------------------------------------------------
    // ========================================================================================
    //

    JList meas_list, attr_list;
    JTextField name_jtf;
    JComboBox colouriser_jcb, datatype_jcb;
    JRadioButton show_jrb, hide_jrb;
    JButton delete_jb;
    JTable attr_table;
    JTable stats_table;
    JTabbedPane tabbed;
    
    JTextField attr_search_jtf;

    JLabel sa_label;
    

    private void buildGUI()
    {
	JPanel panel = new JPanel();
	panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	GridBagLayout gridbag = new GridBagLayout();
	panel.setLayout(gridbag);
	GridBagConstraints c;
	JLabel label;
	JButton jb;
	Box.Filler filler;

	Dimension fillsize = new Dimension(10,10);
	
	// =============================================================
	// on the left-hand side: the measurement selection list:


	JPanel wrap = new JPanel();
	wrap.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
	GridBagLayout wrapbag = new GridBagLayout();
	wrap.setLayout(wrapbag);
	
	meas_list = new JList();
	
	meas_list.addListSelectionListener( new ListSelectionListener()
	    {
		public void valueChanged(ListSelectionEvent e) 
		{
		    if(sync_meas_sel)
			exportMeasurementSelection();
		    
		    updateExportPreview();

		    updatePropertyDisplay();
		    updateAttributeDisplay();
		}
	    });
	
	JScrollPane meas_jsp = new JScrollPane(meas_list);
	
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	c.weighty = 1.0;
	c.weightx = 9.0;
	c.fill = GridBagConstraints.BOTH;
	wrapbag.setConstraints(meas_jsp, c);
	wrap.add(meas_jsp);
	
	jb = new JButton("Select All");
	jb.setToolTipText("Select all Measurements in the list");
	jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    meas_list.setSelectionInterval(0, edata.getNumMeasurements()-1);
		    updateAttributeDisplay();
		    updatePropertyDisplay();
		}
	    });
	jb.setFont( mview.getSmallFont() );
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 1;
	c.fill = GridBagConstraints.HORIZONTAL;
	wrapbag.setConstraints(jb, c);
	wrap.add(jb);

	/*
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	c.weighty = 9.0;
	c.weightx = 4.0;
	c.fill = GridBagConstraints.BOTH;
	gridbag.setConstraints(wrap, c);
	panel.add(wrap);
	*/

	JSplitPane jspltp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

	jspltp.setLeftComponent( wrap );

	// =============================================================
	// on the right-hand side: a tab pane 

	
	tabbed = new JTabbedPane();

	jspltp.setRightComponent( tabbed );

	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	c.weighty = 9.0;
	c.weightx = 10.0;
	c.fill = GridBagConstraints.BOTH;
	gridbag.setConstraints(jspltp, c);
	panel.add(jspltp);

	

	// =============================================================
	// along the bottom: the help and close buttons

	wrap = new JPanel();
	wrap.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
	wrapbag = new GridBagLayout();
	wrap.setLayout(wrapbag);


	int col = 0;

	delete_jb = new JButton("Delete");
	delete_jb.setToolTipText("Delete the selected Measurement(s)");
	delete_jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    deleteSelectedMeasurements();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = col++;
	c.anchor = GridBagConstraints.WEST;
	wrapbag.setConstraints(delete_jb, c);
	wrap.add(delete_jb);


	JButton cluster_jb = new JButton("Cluster");
	cluster_jb.setToolTipText("Create a Cluster from the selected Measurement(s)");
	cluster_jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    clusterSelectedMeasurements();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = col++;
	c.anchor = GridBagConstraints.WEST;
	wrapbag.setConstraints(cluster_jb, c);
	wrap.add(cluster_jb);

	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.NORTHWEST;
	c.gridx = col++;
	wrapbag.setConstraints(filler, c);
	wrap.add(filler);

	final JCheckBox sync_sel_jchkb = new JCheckBox("Sync. Selection");
	sync_sel_jchkb.setToolTipText("Lock the selection in the Measurement list to the selection in the main display");
	sync_meas_sel = mview.getBooleanProperty("MeasurementManager.sync_selection", true);
	sync_sel_jchkb.setSelected(sync_meas_sel);
	sync_sel_jchkb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    sync_meas_sel = sync_sel_jchkb.isSelected();
		    exportMeasurementSelection();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = col++;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.WEST;
	wrapbag.setConstraints(sync_sel_jchkb, c);
	wrap.add(sync_sel_jchkb);


	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.gridx = col++;
	wrapbag.setConstraints(filler, c);
	wrap.add(filler);


	jb = new JButton("Help");
	jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    mview.getPluginHelpTopic("MeasurementManager", "MeasurementManager");
		}
	    });
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.EAST;
	c.weightx = 1.0;
	c.gridx = col++;
	wrapbag.setConstraints(jb, c);
	wrap.add(jb);
	
	 
	jb = new JButton("Close");
	jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    cleanUp();
		}
	    });
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.EAST;
	c.gridx = col++;
	wrapbag.setConstraints(jb, c);
	wrap.add(jb);
	
	
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 1;
	c.weightx = 10.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	gridbag.setConstraints(wrap, c);
	panel.add(wrap);


	// =============================================================
	// tab #1 : measurement properties

	wrap = new JPanel();
	wrapbag = new GridBagLayout();
	wrap.setLayout(wrapbag);
	wrap.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	
	int aline = 0;
	    	
	//String[] colouriser_names =  dplot.getColouriserNameArray();

	label = new JLabel("Name ");
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.EAST;
	c.gridy = aline;
	wrapbag.setConstraints(label, c);
	wrap.add(label);

	name_jtf = new JTextField(32);
	name_jtf.addMouseListener(new CustomMouseListener());
	name_jtf.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    updateMeasurementName();
		}
	    });
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.WEST;
	c.gridx = 1;
	c.gridy = aline;
	c.weightx = 8.0;
	c.gridwidth = 2;
	c.fill = GridBagConstraints.HORIZONTAL;
	wrapbag.setConstraints(name_jtf, c);
	wrap.add(name_jtf);

	aline++;

	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.WEST;
	c.gridy = aline;
	wrapbag.setConstraints(filler, c);
	wrap.add(filler);

	aline++;

	label = new JLabel(" Visibility ");
	c = new GridBagConstraints();
	c.gridx = 0;
	c.anchor = GridBagConstraints.EAST;
	c.gridy = aline;
	wrapbag.setConstraints(label, c);
	wrap.add(label);
 
	ButtonGroup bgroup = new ButtonGroup();

	show_jrb = new JRadioButton("Show");
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.WEST;
	c.gridx = 1;
	c.gridy = aline;
	wrapbag.setConstraints(show_jrb, c);
	wrap.add(show_jrb);
	bgroup.add(show_jrb);

	hide_jrb = new JRadioButton("Hide");
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.WEST;
	c.gridx = 2;
	c.gridy = aline;
	wrapbag.setConstraints(hide_jrb, c);
	wrap.add(hide_jrb);
	bgroup.add(hide_jrb);

	aline++;

	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.WEST;
	c.gridy = aline;
	wrapbag.setConstraints(filler, c);
	wrap.add(filler);

	aline++;

	label = new JLabel(" Spot Attributes ");
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.EAST;
	c.gridy = aline;
	wrapbag.setConstraints(label, c);
	wrap.add(label);

	sa_label = new JLabel("None");
	sa_label.setForeground(Color.black);
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.WEST;
	c.gridwidth = 2;
	c.gridx = 1;
	c.gridy = aline;
	wrapbag.setConstraints(sa_label, c);
	wrap.add(sa_label);

	aline++;

	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.WEST;
	c.gridy = aline;
	wrapbag.setConstraints(filler, c);
	wrap.add(filler);

	aline++;

	label = new JLabel(" Colouriser ");
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.EAST;
	c.gridy = aline;
	wrapbag.setConstraints(label, c);
	wrap.add(label);

	colouriser_jcb = new JComboBox();
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.WEST;
	c.gridwidth = 2;
	c.gridx = 1;
	c.gridy = aline;
	wrapbag.setConstraints(colouriser_jcb, c);
	wrap.add(colouriser_jcb);

	aline++;

	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.WEST;
	c.gridx = 0;
	c.gridy = aline;
	wrapbag.setConstraints(filler, c);
	wrap.add(filler);
	
	aline++;

	label = new JLabel(" Data Type ");
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.EAST;
	c.gridy = aline;
	wrapbag.setConstraints(label, c);
	wrap.add(label);

	datatype_jcb = new JComboBox(dataTypeStrings);
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.WEST;
	c.gridwidth = 2;
	c.gridx = 1;
	c.gridy = aline;
	wrapbag.setConstraints(datatype_jcb, c);
	wrap.add(datatype_jcb);

	aline++;

	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.NORTHWEST;
	c.weighty = 1.0;
	c.gridy = aline;
	wrapbag.setConstraints(filler, c);
	wrap.add(filler);

	tabbed.add(" Properties ", wrap);



	// =============================================================
	// tab #2 : measurement attributes

	wrap = new JPanel();
	wrapbag = new GridBagLayout();
	wrap.setLayout(wrapbag);
	wrap.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
	
	int cline = 0;

	attr_list = new JList();
	JScrollPane jsp = new JScrollPane(attr_list);
	c = new GridBagConstraints();
	c.fill = GridBagConstraints.BOTH;
	c.gridy = cline++;
	c.weightx = 3.0;
	c.weighty = 10.0;
	wrapbag.setConstraints(jsp, c);
	wrap.add(jsp);
	
	jb = new JButton("Select All");
	jb.setToolTipText("Select all Attributes in the list");
	jb.setFont(mview.getSmallFont());
	c = new GridBagConstraints();
	c.gridy = cline++;
	c.fill = GridBagConstraints.HORIZONTAL;
	wrapbag.setConstraints(jb, c);
	wrap.add(jb);
	jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    attr_list.setSelectionInterval(0, all_attr_names.length - 1);
		    updateAttributeDisplay();
		}
	    });

	/*
	jb = new JButton("Find text");
	jb.setToolTipText("Select Measurements and Attributes by searching for text");
	jb.setFont(mview.getSmallFont());
	c = new GridBagConstraints();
	c.gridy = cline++;
	c.fill = GridBagConstraints.HORIZONTAL;
	wrapbag.setConstraints(jb, c);
	wrap.add(jb);
	jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    openFindTextFrame();
		}
	    });
	*/

	jb = new JButton("Delete");
	jb.setToolTipText("Delete the selected Attribute(s)");
	jb.setFont(mview.getSmallFont());
	c = new GridBagConstraints();
	c.gridy = cline++;
	c.fill = GridBagConstraints.HORIZONTAL;
	wrapbag.setConstraints(jb, c);
	wrap.add(jb);
	jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    deleteSelectedAttributes();
		}
	    });
	jb = new JButton("Add new");
	jb.setToolTipText("Create a new Attribute in the selected Measurement(s)");
	jb.setFont(mview.getSmallFont());
	c = new GridBagConstraints();
	c.gridy = cline++;
	c.fill = GridBagConstraints.HORIZONTAL;
	wrapbag.setConstraints(jb, c);
	wrap.add(jb);
	jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    addNewAttribute();
		}
	    });
	jb = new JButton("Rename");
	jb.setToolTipText("Rename the selected Attribute in the selected Measurement(s)");
	jb.setFont(mview.getSmallFont());
	c = new GridBagConstraints();
	c.gridy = cline++;
	c.fill = GridBagConstraints.HORIZONTAL;
	wrapbag.setConstraints(jb, c);
	wrap.add(jb);
	jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    renameSelectedAttribute();
		}
	    });
	jb = new JButton("Export");
	jb.setToolTipText("Save the data in the table to a plain text file");
	jb.setFont(mview.getSmallFont());
	c = new GridBagConstraints();
	c.gridy = cline++;
	c.fill = GridBagConstraints.HORIZONTAL;
	wrapbag.setConstraints(jb, c);
	wrap.add(jb);
	jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    exportSelectedAttributes();
		}
	    });
	jb = new JButton("Import");
	jb.setToolTipText("Load attributes from a plain text file");
	jb.setFont(mview.getSmallFont());
	c = new GridBagConstraints();
	c.gridy = cline++;
	c.fill = GridBagConstraints.HORIZONTAL;
	wrapbag.setConstraints(jb, c);
	wrap.add(jb);
	jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    importAttributes();
		}
	    });

	
	attr_list.addListSelectionListener( new ListSelectionListener()
	    {
		public void valueChanged(ListSelectionEvent e) 
		{
		    updateExportPreview();

		    updateAttributeDisplay();
		}
	    });

	
	jspltp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	jspltp.setLeftComponent( wrap );

	// - - - - - - - - - - - 

	wrap = new JPanel();
	wrapbag = new GridBagLayout();
	wrap.setLayout(wrapbag);
	wrap.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
	
	attr_table = new JTable();
	attr_table.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
	attr_table.setColumnSelectionAllowed(false);
	attr_table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	//attr_table.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);

	MouseAdapter lml = new MouseAdapter() 
	    {
		public void mouseClicked(MouseEvent e) 
		{
		    TableColumnModel tcm = attr_table.getColumnModel();
		    int vc = tcm.getColumnIndexAtX(e.getX()); 
		    int column = attr_table.convertColumnIndexToModel(vc); 
		    if (e.getClickCount() == 1 && column != -1) 
		    {
			//System.out.println("Sorting " + column + "..."); 
			
			sortColumn( column );
		    }
		}
	    };
	JTableHeader th = attr_table.getTableHeader(); 
	th.addMouseListener(lml); 

	jsp = new JScrollPane(attr_table);
	
	c = new GridBagConstraints();
	c.weightx = 10.0;
	c.weighty = 9.0;
	c.fill = GridBagConstraints.BOTH;
	wrapbag.setConstraints(jsp, c);
	wrap.add(jsp);

	// --------------

	JPanel search_wrap = new JPanel();
	GridBagLayout search_bag = new GridBagLayout();
	search_wrap.setLayout(search_bag);
	search_wrap.setBorder(BorderFactory.createEmptyBorder(3,0,0,0));


	label = new JLabel("Find ");
	c = new GridBagConstraints();
	search_bag.setConstraints( label, c);
	search_wrap.add(label);


	search_string= mview.getProperty("MeasurementManager.search_string", "");
	attr_search_jtf = new JTextField(20);
	attr_search_jtf.getDocument().addDocumentListener( new SearchStringDocumentListener() );
	attr_search_jtf.setText(search_string);
	c = new GridBagConstraints();
	c.gridx = 1;
	c.weightx = 6.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.anchor = GridBagConstraints.WEST;
	search_bag.setConstraints( attr_search_jtf, c);
	search_wrap.add(attr_search_jtf);
	attr_search_jtf.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    doFindText();
		}
	    });

	search_case_sens = mview.getBooleanProperty("MeasurementManager.search_case_sens", false);
	final JCheckBox cs_jchkb = new JCheckBox("Case sensitive");
	cs_jchkb.setFont( mview.getSmallFont() );
	cs_jchkb.setSelected(search_case_sens);
	c = new GridBagConstraints();
	c.gridx = 3;
	search_bag.setConstraints(cs_jchkb, c);
	search_wrap.add(cs_jchkb);
	cs_jchkb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    search_case_sens = cs_jchkb.isSelected();
		    doFindText();
		}
	    });


	search_substring = mview.getBooleanProperty("MeasurementManager.search_substring", true);
	final JCheckBox ms_jchkb = new JCheckBox("Match substrings");
	ms_jchkb.setFont( mview.getSmallFont() );
	ms_jchkb.setSelected(search_substring);
	c = new GridBagConstraints();
	c.gridx = 4;
	search_bag.setConstraints(ms_jchkb, c);
	search_wrap.add(ms_jchkb);
	ms_jchkb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    search_substring = ms_jchkb.isSelected();
		    doFindText();
		}
	    });


	JButton select_matches_jb = new JButton("Select");
	select_matches_jb.setFont( mview.getSmallFont() );
	c = new GridBagConstraints();
	c.gridx = 5;
	search_bag.setConstraints(select_matches_jb, c);
	search_wrap.add(select_matches_jb);
	select_matches_jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    selectMatchedText();
		}
	    });


	c = new GridBagConstraints();
	c.gridy = 1;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.anchor = GridBagConstraints.WEST;
	wrapbag.setConstraints( search_wrap, c);
	wrap.add(search_wrap);
	
	// --------------

	jspltp.setRightComponent( wrap );

	// - - - - - - - - - - - 

	// attr_table.setDefaultEditor(Object.class,   new CustomCellEditor());
	attr_table.setDefaultRenderer(Object.class, new CustomCellRenderer());

	tabbed.add(" Attributes ", jspltp);




	// =============================================================
	// tab #3 : measurement statistics

	wrap = new JPanel();
	wrapbag = new GridBagLayout();
	wrap.setLayout(wrapbag);
	wrap.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

	

	stats_table = new JTable();
	stats_table.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
	stats_table.setCellSelectionEnabled( false );
	stats_table.setRowSelectionAllowed( true );
	stats_table.setDefaultRenderer(Object.class, new StatisticsCellRenderer());
	stats_table.setAutoResizeMode( lock_stats_table_width ? 
				       JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS : 
				       JTable.AUTO_RESIZE_OFF );
	
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	c.weightx = 8.0;
	c.weighty = 8.0;
	c.gridwidth = 3;
	c.anchor = GridBagConstraints.WEST;
	c.fill   = GridBagConstraints.BOTH;
	JScrollPane stats_jsp = new JScrollPane( stats_table );
	wrapbag.setConstraints(stats_jsp, c);
	wrap.add(stats_jsp); 
	
	ButtonGroup stats_bg = new ButtonGroup();
	
	orient_stats_by_meas = mview.getBooleanProperty("MeasurementManager.orient_stats_by_meas", true );

	final JRadioButton orient_by_meas_jrb = new JRadioButton("One Measurement per row");
	orient_by_meas_jrb.setSelected( orient_stats_by_meas );
	orient_by_meas_jrb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    orient_stats_by_meas = orient_by_meas_jrb.isSelected();
		    updatePropertyDisplay();
		}
	    });
	orient_by_meas_jrb.setFont( mview.getSmallFont() );
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 1;
	c.anchor = GridBagConstraints.WEST;
	wrapbag.setConstraints(orient_by_meas_jrb, c);
	wrap.add(orient_by_meas_jrb);
	stats_bg.add(orient_by_meas_jrb);
	
	final JRadioButton orient_by_stat_jrb = new JRadioButton("One statistic per row");
	orient_by_stat_jrb.setSelected( !orient_stats_by_meas );
	orient_by_stat_jrb.setFont( mview.getSmallFont() );
	orient_by_stat_jrb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    orient_stats_by_meas = !orient_by_stat_jrb.isSelected();
		    updatePropertyDisplay();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 1;
	c.weightx = 0.5;
	c.anchor = GridBagConstraints.WEST;
	wrapbag.setConstraints(orient_by_stat_jrb, c);
	wrap.add(orient_by_stat_jrb);
	stats_bg.add(orient_by_stat_jrb);
	
	lock_stats_table_width = mview.getBooleanProperty("MeasurementManager.lock_stats_table_width", true );

	final JCheckBox fixed_table_width_jcb = new JCheckBox("Lock table width to window width");
	fixed_table_width_jcb.setSelected( lock_stats_table_width );
	fixed_table_width_jcb.setFont( mview.getSmallFont() );
	fixed_table_width_jcb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    lock_stats_table_width = fixed_table_width_jcb.isSelected();

		    stats_table.setAutoResizeMode( lock_stats_table_width ? 
						   JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS : 
						   JTable.AUTO_RESIZE_OFF );
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 1;
	c.weightx = 0.5;
	c.anchor = GridBagConstraints.EAST;
	wrapbag.setConstraints(fixed_table_width_jcb, c);
	wrap.add(fixed_table_width_jcb);
	

	tabbed.add(" Statistics ", wrap);


	// =============================================================
	// finally, synchronise gui elements and create JFrame

	updateMeasList();
	updatePropertyDisplay();
	updateAttributeNameList();
	updateAttributeDisplay();
	updateColouriserList();

	importMeasurementSelection( edata.getMeasurementSelection() );

	tabbed.setSelectedIndex( mview.getIntProperty( "MeasurementManager.tab",0 ) );

	frame = new JFrame("Measurement Manager");

	mview.decorateFrame( frame );

	frame.getContentPane().add(panel, BorderLayout.CENTER);

    }
    
    ActionListener show_al, hide_al, colouriser_al, datatype_al, name_al;

    // =======================================================================
    // ------ Properties and Statistics --------------------------------------
    // =======================================================================

    private boolean ignore_selection_updates = false;

    // set the list selection based on the main display selection 
    private void importMeasurementSelection( int[] msel )
    {
	if(ignore_selection_updates)
	    return;

	if(!sync_meas_sel)
	    return;

	// convert actual measurement id's to traversal order indices

	int[] msel_t = new int[ msel.length ];
	for(int m=0; m < msel.length; m++)
	{
	    msel_t[m] = edata.getIndexOfMeasurement( msel[m] );
	}

	meas_list.setSelectedIndices( msel_t );
    }

    // send the current list selection to the main display
    private void exportMeasurementSelection()
    {
	// avoid the infinite loop!
	ignore_selection_updates = true;
	
	int[] msel_t = meas_list.getSelectedIndices();

	// convert traversal order indices to actual measurement id's


	int[] msel = new int[ msel_t.length ];
	for(int m=0; m < msel.length; m++)
	{
	    msel[m] = edata.getMeasurementAtIndex( msel_t[m] );
	}
	
	edata.setMeasurementSelection( msel );

	ignore_selection_updates = false;
    }


    // called when the list needs to be updated (doh!)
    //
    private void updateMeasList()
    {
	DefaultListModel meas_list_model = new DefaultListModel();
	
	final int nm = edata.getNumMeasurements();
	
	for(int m=0; m < nm; m++)
	{
	    //PluginCommand pc = (PluginCommand) commands.elementAt(c);
	    //String cname = pc.plugin_name + "." + pc.name;
	    
	    meas_list_model.addElement( edata.getMeasurementName( edata.getMeasurementAtIndex(m) ) );
	}

	//System.out.println("updateMeasLists(): there are " + commands.size() + " commands in the list");

	meas_list.setModel(meas_list_model);
 	
    }

    private void updateColouriserList()
    {
	String[] colouriser_names =  dplot.getColouriserNameArray();
	//System.out.println("updateColouriserList(): there are " + colouriser_names.length + " colourisers");

	// disable the action listener whilst the data model is changing
	if(colouriser_al != null)
	    colouriser_jcb.removeActionListener(colouriser_al);
	else
	{
	    colouriser_al = new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			updateMeasurements(3);
		    }
		}; 
	}

	// and save the current selection
	String cur_sel = (String) colouriser_jcb.getSelectedItem();

	// set the new list
	colouriser_jcb.setModel( new DefaultComboBoxModel(colouriser_names) );

	// restore the previous selection
	if(cur_sel ==null)
	    colouriser_jcb.setSelectedIndex(-1);
	else
	    colouriser_jcb.setSelectedItem(cur_sel);

	// and re-enable the action litener
	colouriser_jcb.addActionListener(colouriser_al);
    }

    // called when the list selection changes
    //
    private void updatePropertyDisplay()
    {
	final String[] stat_names = 
	{ "# Spots", "Min. Value", "Max. Value", "Mean Value", "% +ve", "% -ve", "# NaNs", "# Infs." };

	// remove the action listeners if they exist, if not create them (but don't add them)
	
	if(show_al != null)
	    show_jrb.removeActionListener(show_al);
	else
	{
	    show_al = new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			updateMeasurements(1);
		    }
		};
	}
	
	if(hide_al != null)
	    hide_jrb.removeActionListener(hide_al);
	else
	{
	    hide_al = new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			updateMeasurements(2);
		    }
		};
	}

	if(colouriser_al != null)
	    colouriser_jcb.removeActionListener(colouriser_al);
	else
	{
	    colouriser_al = new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			updateMeasurements(3);
		    }
		}; 
	}
	
	if(datatype_al != null)
	    datatype_jcb.removeActionListener(datatype_al);
	else
	{
	    datatype_al = new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			updateMeasurements(4);
		    }
		}; 
	}
	
	Object[] selobj = meas_list.getSelectedValues();

	if((selobj == null) || (selobj.length == 0))
	{
	    name_jtf.setEnabled(false);
	    show_jrb.setEnabled(false);
	    hide_jrb.setEnabled(false);
	    colouriser_jcb.setEnabled(false);
	    datatype_jcb.setEnabled(false);
	    delete_jb.setEnabled(false);
	    
	    name_jtf.setText("");
	    colouriser_jcb.setSelectedIndex(-1);
	    datatype_jcb.setSelectedIndex(-1);
	    show_jrb.setSelected(false);
	    hide_jrb.setSelected(false);

	    Object[][] null_data = new Object[0][];

	    stats_table.setModel( new DefaultTableModel( null_data, stat_names ));
	}
	else
	{
	    String name = (String) selobj[0];
	    int m_id = edata.getMeasurementFromName(name);

	    // 
	    if( m_id < 0 )
	    {
		// the list is out of date because one or more measurement names have changed
		// updateMeasList();
		return;
	    }

	    ExprData.Measurement m0 = edata.getMeasurement(m_id);
	    Colouriser col0 = dplot.getColouriserForMeasurement(m_id); 
	    
	    name_jtf.setEnabled(true);
	    show_jrb.setEnabled(true);
	    hide_jrb.setEnabled(true);
	    colouriser_jcb.setEnabled(true);
	    datatype_jcb.setEnabled(true);

	    if(selobj.length == 1)
	    {
		// single selection, set each field accordingly

		name_jtf.setEnabled(true);
		name_jtf.setText(m0.getName());
		
		if(col0 != null)
		{
		    String cname =  col0.getName();
		    if(cname.equals("(default)"))
			colouriser_jcb.setSelectedIndex(-1);
		    else
			colouriser_jcb.setSelectedItem(cname);
		}

		show_jrb.setSelected(m0.getShow());
		hide_jrb.setSelected(!(m0.getShow()));
		
		datatype_jcb.setSelectedIndex(m0.getDataType());
		
		delete_jb.setEnabled(true);
		
		double[] data = m0.getData();

		if(data == null)
		{
		    Object[][] null_data = new Object[0][];
		    String[] null_cols = new String[0];

		    stats_table.setModel( new ImmutableTableModel( null_data, null_cols ));
		}
		else
		{ 
		    int n_nans = 0;
		    int n_infs = 0;
		    
		    int n_pos = 0;
		    int n_neg = 0;

		    final int ns= data.length;
		    
		    for(int s=0; s < ns; s++)
		    {
			if(Double.isNaN(data[s]))
			    n_nans++;
			else
			{
			    if(Double.isInfinite(data[s]))
				n_infs++;
			    else
			    {
				if( data[s] > .0 )
				    n_pos++;
				else
				{
				    if( data[s] < .0 )
					n_neg++;
				}
			    }
				
			}
			
		    }
		    
		    double mean_val = (edata.getMeasurementMinEValue(m_id) + edata.getMeasurementMaxEValue(m_id)) * 0.5;
		   
		    double percent_positive = ((double)n_pos * 100.0) / (double) ns;
		    double percent_negative = ((double)n_neg * 100.0) / (double) ns;
		    
		    // two columns (name, value), one value per row
		    //
		    // rows are:
		    // 
		    //     "# Spots",
		    // 	   "Min. Value", 
		    // 	   "Max. Value", 
		    // 	   "Mean Value",
		    // 	   "% +ve",
		    // 	   "% -ve", 
		    // 	   "# NaNs",
		    // 	   "# Infs."


		    if( orient_stats_by_meas )
		    {
			Object[][] stats_data = new Object[ 1 ][];
			stats_data[0] = new String[ stat_names.length + 1 ];

			stats_data[0][0] = edata.getMeasurementName(m_id);
			stats_data[0][1] = String.valueOf(ns);
			stats_data[0][2] = mview.niceDouble( edata.getMeasurementMinEValue(m_id), 10, 6 );
			stats_data[0][3] = mview.niceDouble( edata.getMeasurementMaxEValue(m_id), 10, 6 );
			stats_data[0][4] = mview.niceDouble( mean_val, 10, 6 );
			stats_data[0][5] = mview.niceDouble( percent_positive, 6, 2 );
			stats_data[0][6] = mview.niceDouble( percent_negative, 6, 2 );
			stats_data[0][7] = String.valueOf(n_nans);
			stats_data[0][8] = String.valueOf(n_infs);
			
			final String[] col_names = new String[ stat_names.length + 1 ];

			col_names[0] = " ";
			for(int s=0; s < stat_names.length; s++)
			    col_names[s+1] = stat_names[s];

			stats_table.setModel( new ImmutableTableModel( stats_data, col_names ));
		    }
		    else
		    {
			Object[][] stats_data = new Object[ stat_names.length ][];
			for(int v=0; v < stat_names.length; v++)
			{
			    stats_data[v] = new String[2];
			    stats_data[v][0] = stat_names[v];
			}
			stats_data[0][1] = String.valueOf(ns);
			stats_data[1][1] = mview.niceDouble( edata.getMeasurementMinEValue(m_id), 10, 6 );
			stats_data[2][1] = mview.niceDouble( edata.getMeasurementMaxEValue(m_id), 10, 6 );
			stats_data[3][1] = mview.niceDouble( mean_val, 10, 6 );
			stats_data[4][1] = mview.niceDouble( percent_positive, 6, 2 );
			stats_data[5][1] = mview.niceDouble( percent_negative, 6, 2 );
			stats_data[6][1] = String.valueOf(n_nans);
			stats_data[7][1] = String.valueOf(n_infs);
			
			final String[] col_names = { " ", edata.getMeasurementName(m_id) };

			stats_table.setModel( new ImmutableTableModel( stats_data, col_names ));
		    }
		}

	    }
	    else
	    {
		// multiple selection, set the GUI for any atts which are common
		// to all selected Measurements
		
		//System.out.println("multi sel...");

		name_jtf.setText("");
		name_jtf.setEnabled(false);
		delete_jb.setEnabled(true);
		
		//delete_jb.setText("Delete these Measurements");
		
		boolean same_show = true;
		boolean same_colouriser = true;
		boolean same_datatype = true;
		
		name = (String) selobj[0];
		m_id = edata.getMeasurementFromName(name);
		
		double overall_min = edata.getMeasurementMinEValue( m_id );
		double overall_max = edata.getMeasurementMaxEValue( m_id );

		for(int m=1; m < selobj.length; m++)
		{
		    name = (String) selobj[m];
		    m_id = edata.getMeasurementFromName(name);
		    ExprData.Measurement mN = edata.getMeasurement(m_id);
		    Colouriser colN = dplot.getColouriserForMeasurement(m_id); 
		    
		    if(m0.getShow() != mN.getShow())
			same_show = false;
		    
		    if((col0 == null) || !((col0.getName().equals(colN.getName()))))
			same_colouriser = false;
		    
		    if(m0.getDataType() != mN.getDataType())
			same_datatype = false;
		}

		// in 'orient_stats_by_meas' mode, there is one measurement per row
		// with the statistics in the columns
		// 
		// otherwise, one statistic per row, one one column per selected measurement
		//
		// statistics are:
		// 
		//     "# Spots",
		//     "Min. Value", 
		//     "Max. Value", 
		//     "Mean Value",
		//     "% +ve",
		//     "% -ve", 
		//     "# NaNs",
		//     "# Infs."

		final int n_cols = orient_stats_by_meas ? stat_names.length + 1 : selobj.length + 1;
		final int n_rows = orient_stats_by_meas ? selobj.length : stat_names.length;
		
		Object[][] stats_data = new Object[ n_rows ][];

		for(int r=0; r < n_rows; r++)
		{
		    stats_data[r] = new String[ n_cols ];
		}

		final String[] col_names = new String[ n_cols ];

		col_names[0] = " ";
		    
		if( orient_stats_by_meas )
		{
		    for(int c=1; c < n_cols; c++)
			col_names[c] = (String) stat_names[c-1];
		}
		else
		{
		    for(int c=1; c < n_cols; c++)
			col_names[c] = (String) selobj[c-1];

		    for(int r=0; r < n_rows; r++)
			stats_data[r][0] = stat_names[r];
		}
		
		/*
		    double this_min = edata.getMeasurementMinEValue(m_id);

		    if(this_min < overall_min)
			overall_min = this_min;
		    
		    double this_max = edata.getMeasurementMaxEValue(m_id);

		    if(this_max > overall_max)
			overall_max = this_max;
		    
		    data = mN.getData();

		    if(data != null)
		    {
			final int ns = data.length;
			
			for(int s=0; s < ns; s++)
			{
			    if(Double.isNaN(data[s]))
				n_nans++;
			    else
				if(Double.isInfinite(data[s]))
				    n_infs++;
			}
		    }
		*/

		// =============================================

		for(int m=0; m < selobj.length; m++)
		{
		    name = (String) selobj[m];
		    m_id = edata.getMeasurementFromName(name);
		    ExprData.Measurement mN = edata.getMeasurement(m_id);

		    double[] data = mN.getData();

		    int n_nans = 0;
		    int n_infs = 0;
		    
		    int n_pos = 0;
		    int n_neg = 0;

		    final int ns= data.length;
		    
		    for(int s=0; s < ns; s++)
		    {
			if(Double.isNaN(data[s]))
			    n_nans++;
			else
			{
			    if(Double.isInfinite(data[s]))
				n_infs++;
			    else
			    {
				if( data[s] > .0 )
				    n_pos++;
				else
				{
				    if( data[s] < .0 )
					n_neg++;
				}
			    }
				
			}
			
		    }
		    
		    double min_val = edata.getMeasurementMinEValue(m_id);
		    double max_val = edata.getMeasurementMaxEValue(m_id);
		    double mean_val = (min_val + max_val) * 0.5;
		   
		    double percent_positive = ((double)n_pos * 100.0) / (double) ns;
		    double percent_negative = ((double)n_neg * 100.0) / (double) ns;

		    if( orient_stats_by_meas )
		    {
			stats_data[m][0] = name;
			stats_data[m][1] = String.valueOf(ns);
			stats_data[m][2] = mview.niceDouble( min_val, 10, 6 );
			stats_data[m][3] = mview.niceDouble( max_val, 10, 6 );
			stats_data[m][4] = mview.niceDouble( mean_val, 10, 6 );
			stats_data[m][5] = mview.niceDouble( percent_positive, 6, 2 );
			stats_data[m][6] = mview.niceDouble( percent_negative, 6, 2 );
			stats_data[m][7] = String.valueOf(n_nans);
			stats_data[m][8] = String.valueOf(n_infs);
		    }
		    else
		    {
			stats_data[0][m+1] = String.valueOf(ns);
			stats_data[1][m+1] = mview.niceDouble( min_val, 10, 6 );
			stats_data[2][m+1] = mview.niceDouble( max_val, 10, 6 );
			stats_data[3][m+1] = mview.niceDouble( mean_val, 10, 6 );
			stats_data[4][m+1] = mview.niceDouble( percent_positive, 6, 2 );
			stats_data[5][m+1] = mview.niceDouble( percent_negative, 6, 2 );
			stats_data[6][m+1] = String.valueOf(n_nans);
			stats_data[7][m+1] = String.valueOf(n_infs);
		    }
		
		}

		stats_table.setModel( new ImmutableTableModel( stats_data, col_names ));

		// =============================================

		if(same_show)
		{
		    show_jrb.setSelected(m0.getShow());
		    hide_jrb.setSelected(!(m0.getShow()));
		}
		else
		{
		    show_jrb.setSelected(false);
		    hide_jrb.setSelected(false);
		}
		
		if(same_colouriser)
		{
		    if(col0 != null)
		    {
			String cname =  col0.getName();
			if(cname.equals("(default)"))
			    colouriser_jcb.setSelectedIndex(-1);
			else
			    colouriser_jcb.setSelectedItem(cname);
		    }
		}
		else
		{
		    colouriser_jcb.setSelectedIndex(-1);
		}
		
		if(same_datatype)
		{
		    datatype_jcb.setSelectedIndex(m0.getDataType());
		}
		else
		{
		    datatype_jcb.setSelectedIndex(-1);
		}

		//details_jb.setEnabled(false);

	    }
	}
	

	// =============================================

	// update the list of SpotAttrs 
	{
	    HashSet all_spot_attr_names_hs = new HashSet();
	    

	    for(int m=0; m < selobj.length; m++)
	    {
		String name = (String) selobj[m];
		int m_id = edata.getMeasurementFromName(name);
		ExprData.Measurement meas = edata.getMeasurement(m_id);

		for(int a=0; a < meas.getNumSpotAttributes(); a++)
		{
		    all_spot_attr_names_hs.add( meas.getSpotAttributeName( a ) );
		}
	    }
	    
	    if( all_spot_attr_names_hs.size() > 0 )
	    {
		String[] names_a = (String[]) all_spot_attr_names_hs.toArray( new String[0] );
		
		Arrays.sort( names_a );
		
		String result = "";
		
		for(int a=0; a < names_a.length; a++)
		{
		    if(a > 0)
			result += ", ";
		    
		    result += names_a[a];
		}
		
		sa_label.setText( result );
	    }
	    else
	    {
		sa_label.setText( "None" );
	    }
	}
	
	// (re)install the action listeners
	show_jrb.addActionListener(show_al);
	hide_jrb.addActionListener(hide_al);
	datatype_jcb.addActionListener(datatype_al);
	colouriser_jcb.addActionListener(colouriser_al);

    }


    class StatisticsCellRenderer extends JLabel implements TableCellRenderer 
    {
	public StatisticsCellRenderer() 
	{
	    super();

	    Font cur_font = getFont();
	    normal_font = new Font( cur_font.getName(), Font.PLAIN, cur_font.getSize() );
	    bold_font   = new Font( cur_font.getName(), Font.BOLD,  cur_font.getSize() );
	}
	
	public Component getTableCellRendererComponent(JTable table, Object object, 
						       boolean isSelected, boolean hasFocus,
						       int row, int col) 
	{
	    setForeground( Color.black );

	    setFont( col == 0 ? bold_font : normal_font );

	    setHorizontalAlignment( col == 0 ? LEFT : RIGHT );

	    if( object != null )
	    {
		String value = (String) object.toString();
		setText( value );
	    }

	    return this;
	}

	private Font  normal_font, bold_font;
    }

 

    private boolean ignore_measurement_updates = false;

    // called when the properties are changed by the user and one or more Measurements need updating
    //
    private void updateMeasurements(int att)
    {
	//System.out.println("updateMeasurements()");
	Object[] selobj = meas_list.getSelectedValues();

	if((selobj == null) || (selobj.length == 0))
	{
	    return;
	}
	
	ignore_measurement_updates = true;
	
	for(int m=0; m < selobj.length; m++)
	{
	    String name = (String) selobj[m];
	    int m_id = edata.getMeasurementFromName(name);
	    if(m_id >= 0)
	    {
		ExprData.Measurement meas = edata.getMeasurement(m_id);
		
		// System.out.println("updating " + meas.getName());

		switch(att)
		{
		case 1: // show
		    meas.setShow(true);
		    break;
		case 2: // hide
		    meas.setShow(false);
		    break;
		case 3: // colouriser
		    dplot.setColouriserForMeasurement(m_id, (String) colouriser_jcb.getSelectedItem());
		    break;
		case 4: // data type
		    meas.setDataType(datatype_jcb.getSelectedIndex());
		    break;
		}
	    }

	    // generate the right sort of update event
	    switch(att)
	    {
	    case 1: // show
	    case 2: // hide
	    case 4: // data type
		edata.generateMeasurementUpdate(ExprData.VisibilityChanged);
		break;
	    case 3:
		edata.generateMeasurementUpdate(ExprData.ColouriserChanged);
		break;
	    }
	}
	

	ignore_measurement_updates = false;
	
	// make sure the GUI reflect the new values...

	// this is required?

	// updatePropertyDisplay();
    }

    // called when the atts change and one or more Measurements need updating
    //
    private void deleteSelectedMeasurements()
    {

	Object[] selobj = meas_list.getSelectedValues();

	if((selobj == null) || (selobj.length == 0))
	{
	    return;
	}

	int delc = selobj.length;
	if(mview.infoQuestion("Really delete " + (selobj.length == 1 ? "this Measurement" : "these Measurements") + " ?",
			      "Yes", "No") == 1)
	    return;

	if(selobj.length == edata.getNumMeasurements())
	{
	    edata.removeAllMeasurements();
	    return;
	}
	
	for(int m=0; m < selobj.length; m++)
	{
	    String name = (String) selobj[m];
	    System.out.println("deleting '" + name + "'");
	    int m_id = edata.getMeasurementFromName(name);
	    edata.removeMeasurement(m_id);
	}
    }

    private void clusterSelectedMeasurements()
    {
	Object[] sel_meas = meas_list.getSelectedValues();
	if((sel_meas == null) || (sel_meas.length == 0))
	{
	    mview.alertMessage("No Measurements are selected");
	    return;
	}
	
	try
	{
	    String name = mview.getString("Name for new Cluster");
	    
	    Vector m_names_v = new Vector();
	    for(int m=0; m < sel_meas.length; m++)
		m_names_v.addElement( (String) sel_meas[m] );
	   
	    ExprData.Cluster cl = edata.new Cluster( name, ExprData.MeasurementName, m_names_v );

	    edata.addCluster(cl);
	}
	catch(UserInputCancelled uic)
	{
	}
    }


    // called when return is pressed in the name JTF, or the mouse exits the name JTF
    public void updateMeasurementName()
    {
	Object[] selobj = meas_list.getSelectedValues();

	if((selobj != null) && (selobj.length == 1))
	{
	    String name = (String) selobj[0];
	    String new_name =  name_jtf.getText();
	    if((new_name != null) && (new_name.length() > 0) && (!new_name.equals(name)))
	    {
		// System.out.println("renaming '" + name + "'");
		int m_id = edata.getMeasurementFromName(name);

		if( m_id >= 0 )
		{
		    // update the entry in the list as it will be needed when
		    // the update event is recieved as a result of making the
		    // following call:
		    
		    DefaultListModel dlm = (DefaultListModel) meas_list.getModel();
		    int sel_ind = meas_list.getSelectedIndex();
		    dlm.setElementAt( new_name, sel_ind );

		    edata.setMeasurementName(m_id, new_name);

		    meas_list.setSelectedValue(new_name, true);
		}
	    }
	}
	
    }

    public class CustomMouseListener implements MouseListener
    {
	public void mousePressed(MouseEvent e) 
	{
	}
	
	public void mouseReleased(MouseEvent e) 
	{
	}
	
	public void mouseClicked(MouseEvent e) 
	{
	}
	
	public void mouseEntered(MouseEvent e) 
	{
	}

	public void mouseExited(MouseEvent e) 
	{
	    updateMeasurementName();
	}
    }

    // =======================================================================
    // ------ Attributes -----------------------------------------------------
    // =======================================================================

    private void deleteSelectedAttributes()
    {
	Object[] sel_meas = meas_list.getSelectedValues();
	int n_sel_meas = (sel_meas == null) ? 0 : sel_meas.length;

	if( n_sel_meas == 0 )
	{
	    mview.alertMessage("No Measurements are selected");
	    return;
	}

	Object[] sel_attr = attr_list.getSelectedValues();
	int n_sel_attr = (sel_attr == null) ? 0 : sel_attr.length;

	if( n_sel_attr == 0 )
	{
	    mview.alertMessage("No Attributes are selected");
	    return;
	}

	// inform the user of what will happen and request confirmation
	//
	int mhit = 0;

	for(int m=0; m < n_sel_meas; m++)
	{
	    int m_id = edata.getMeasurementFromName( (String) sel_meas[m] );
	    ExprData.Measurement ms = edata.getMeasurement(m_id);

	    int ahit = 0;

	    for(int a=0; a < n_sel_attr; a++)
	    {
		if(ms.getAttributes().get( (String)sel_attr[a] ) != null)
		    ahit++;
	    }

	    if(ahit > 0)
		mhit++;
	}

	String attr_desc = (n_sel_attr == 1) ? 
	    ("Attribute '" + (String)sel_attr[0] +"'") : ("the " + n_sel_attr + " selected Attributes");
	
	String meas_desc = (n_sel_meas == 1) ? 
	    ("Measurement '" + (String)sel_meas[0] + "'") : ("the " + n_sel_meas + " selected Measurements");
	
	if(mview.alertQuestion("Really remove " + attr_desc + "\nfrom " + meas_desc + " ?", "Yes", "No") == 1)
	    return;
	
	
	// remove these Attributes from each of the selected Measurements
	//
	for(int m=0; m < n_sel_meas; m++)
	{
	    int m_id = edata.getMeasurementFromName( (String) sel_meas[m] );
	    ExprData.Measurement ms = edata.getMeasurement(m_id);
	    for(int a=0; a < n_sel_attr; a++)
	    {
		ms.getAttributes().remove( (String)sel_attr[a] );
	    }
	}

	// and synchronise the gui
	//
	updateAttributeNameList();
	updateAttributeDisplay();
	
    }
    

    public void addNewAttribute()
    {
	Object[] sel_meas = meas_list.getSelectedValues();
	int n_sel_meas = (sel_meas == null) ? 0 : sel_meas.length;

	if( n_sel_meas == 0 )
	{
	    mview.alertMessage("No Measurements are selected");
	    return;
	}

	try
	{
	    String aname = mview.getString("Name for new Attribute");
	    
	    // what if there is already an attribute with this name??

	    for(int m=0; m < n_sel_meas; m++)
	    {
		int m_id = edata.getMeasurementFromName( (String) sel_meas[m] );
		ExprData.Measurement ms = edata.getMeasurement(m_id);

		ExprData.Measurement.MeasurementAttr new_ma = ms.new MeasurementAttr();
		new_ma.source = "User";
		new_ma.value  = "";
		new_ma.name   = aname;
		new_ma.time_created = (new Date()).toString();
		new_ma.time_last_modified = "";

		ms.getAttributes().put( aname, new_ma);
	    }

	    updateAttributeNameList();
	    updateAttributeDisplay();
	}
	catch(UserInputCancelled uic)
	{
	}
    }

    private void renameSelectedAttribute()
    {
	Object[] sel_meas = meas_list.getSelectedValues();
	int n_sel_meas = (sel_meas == null) ? 0 : sel_meas.length;

	if( n_sel_meas == 0 )
	{
	    mview.alertMessage("No Measurements are selected");
	    return;
	}

	Object[] sel_attr = attr_list.getSelectedValues();
	int n_sel_attr = (sel_attr == null) ? 0 : sel_attr.length;

	if( n_sel_attr == 0 )
	{
	    mview.alertMessage("No Attributes are selected");
	    return;
	}

	if( n_sel_attr > 1 )
	{
	    mview.alertMessage("More than one Attribute is selected.\n" + 
			       "Only one Attribute can be renamed at a time");
	    return;
	}

	// how many of the selected Measurements actually have this attr?
	
	final String attr_name = (String) sel_attr[0];

	int count = 0;
	ExprData.Measurement.MeasurementAttr ma = null;
	
	for(int m=0; m < n_sel_meas; m++)
	{
	    int m_id = edata.getMeasurementFromName( (String) sel_meas[m] );
	
	    ExprData.Measurement ms = edata.getMeasurement(m_id);
	    
	    ma = (ExprData.Measurement.MeasurementAttr) ms.getAttributes().get( attr_name );
		
	    if(ma != null)
		count++;
	}
	
	if(( count != n_sel_meas ) && ( n_sel_meas == 1 ))
	{
	    mview.alertMessage("The selected Attribute is not present in the selected Measurement");
	    return;
	}
	if( count == 0 )
	{
	    mview.alertMessage("The selected Attribute is not present in any of selected Measurements");
	    return;
	}
	
	String msg = null;

	if(( count == n_sel_meas ) && ( n_sel_meas == 1 ))
	    msg = null;  // probably dont need confirmation for the simplest case 

	if(( count == n_sel_meas ) && ( n_sel_meas > 1 ))
	    msg = "in all of the selected Measurments";

	if(( count != n_sel_meas ) && ( n_sel_meas > 1 ))
	    msg = "(which occurs in " + count + " of the selected Measurments)";

	if(msg != null)
	{
	    if(mview.infoQuestion("Really rename '" + attr_name + "'\n" + msg, "Yes", "No") == 1)
		return;
	}


	String new_name = null;

	try
	{
	    new_name = mview.getString( "New name for '" + attr_name + "'", attr_name );
	}
	catch(UserInputCancelled uic)
	{
	    return;
	}

	if((new_name == null) || (new_name.length() == 0))
	{
	    mview.alertMessage("Attribute names cannot be blank");
	    return;
	}
	
	// now do the actual renaming operation

	for(int m=0; m < n_sel_meas; m++)
	{
	    int m_id = edata.getMeasurementFromName( (String) sel_meas[m] );
	
	    ExprData.Measurement ms = edata.getMeasurement(m_id);
	    
	    ma = (ExprData.Measurement.MeasurementAttr) ms.getAttributes().get( attr_name );
		
	    if(ma != null)
	    {
		ms.getAttributes().remove( attr_name );
		
		ma.name = new_name;
		
		ms.getAttributes().put( new_name, ma );
	    }
	}

	updateAttributeNameList();
	updateAttributeDisplay();
    }

    private void updateAttributeDisplay()
    {
	if( all_attr_names == null )
	    updateAttributeNameList();


	Object[] sel_meas = meas_list.getSelectedValues();
	int n_sel_meas = (sel_meas == null) ? 0 : sel_meas.length;

	Object[] sel_attr = attr_list.getSelectedValues();
	int n_sel_attr = (sel_attr == null) ? 0 : sel_attr.length;

	if(n_sel_meas == 0)
	    return;

	int[] m_ids = new int[ n_sel_meas ];
	for(int m=0; m < n_sel_meas; m++)
	    m_ids[m] = edata.getMeasurementFromName( (String) sel_meas[m] );
	
	String[] attr_names = (n_sel_attr == 0) ? null : new String[ sel_attr.length ];
	for(int a=0; a < n_sel_attr; a++)
	    attr_names[a] = (String)  sel_attr[a];

	// displayAttributesOfMeasurements( m_ids, attr_names );

	if(n_sel_meas == 1)
	{
	    // a single selected measurement: 2 possible configurations of attrs, either 1 or !1

	    if(n_sel_attr == 0)
	    {
		displaySomeAttributesOfOneMeasurement( m_ids[0], all_attr_names );
	    }
	    else
	    {
		if(n_sel_attr == 1)
		{
		    displayOneAttributeOfOneMeasurement( m_ids[0], attr_names[0] );
		}
		else
		{
		    displaySomeAttributesOfOneMeasurement( m_ids[0], attr_names );
		}
	    }
	    
	}
	else
	{
	    
	    if(n_sel_attr == 1)
	    {
		displayOneAttributeOfSomeMeasurements( m_ids, attr_names[0] );
	    }
	    else
	    {
		if(n_sel_attr == 0)
		{
		    displaySomeAttributesOfSomeMeasurements( m_ids, all_attr_names );
		}
		else
		{
		    displaySomeAttributesOfSomeMeasurements( m_ids, attr_names );
		}
	    }

	}
        
	adjustTableColumnWidths();
    }


    //
    // one meas and one attr == detailed display of all fields
    //
    // one meas and >1 attr  == table: col1=attr_name col2=attr_val
    //
    // >1 meas and one attr  == table: col1=meas_name col2=attr_val
    //
    // >1 meas and >1 attr   == table: 
    //

    private void displayOneAttributeOfOneMeasurement( int meas_id, String attr_name )
    {
	//System.out.println("1a 1m");

	ExprData.Measurement ms = edata.getMeasurement(meas_id);
	ExprData.Measurement.MeasurementAttr ma = (ExprData.Measurement.MeasurementAttr) ms.getAttributes().get(attr_name);
	if(ma == null)
	{
	    // an empty table
	    saveTableColumnWidths();
	    attr_table.setModel(new DefaultTableModel( ));
	    table_mode = EmptyTable;
	    return;
	}

	String[] col_names = { "Field", "Value" };
	String[] row_names = { "Measurement", "Attribute", "Value", "Source", "Created", "Last Modified" };

	Object[][] table_data = new Object[6][];

	for(int row=0; row < 6; row++)
	{
	    table_data[row] = new String[2];

	    table_data[row][0] = row_names[row];
	}
	
	table_data[0][1] = edata.getMeasurementName( meas_id );
	table_data[1][1] = attr_name;
	table_data[2][1] = ma.value;
	table_data[3][1] = ma.source;
	table_data[4][1] = ma.time_created;
	table_data[5][1] = ma.time_last_modified;

	saveTableColumnWidths();
	table_mode = OneMeasOneAttrTable;
	attr_table.setModel(new CustomTableModel( table_data, col_names ));
    }


    private void displaySomeAttributesOfSomeMeasurements( int[] meas_ids, String[] attr_names )
    {
	//System.out.println("Sa Sm");

	Object[][] table_data = new Object[ meas_ids.length ][];


	String[] col_names = new String[ attr_names.length + 1 ];
	for(int c=0; c < attr_names.length; c++)
	    col_names[c+1] = attr_names[c];
	col_names[0] = "Measurement";

	int displayed = 0;

	for(int row=0; row < meas_ids.length; row++)
	{
	    table_data[row] = new String[ attr_names.length + 1 ];
	    
	    ExprData.Measurement ms = edata.getMeasurement( meas_ids[row] );
	    Hashtable attrs = ms.getAttributes();

	    table_data[row][0] = ms.getName();

	    for(int col=0; col <  attr_names.length; col++)
	    {
		ExprData.Measurement.MeasurementAttr ma = (ExprData.Measurement.MeasurementAttr) attrs.get(attr_names[col]);
		if(ma != null)
		{
		    table_data[row][col + 1] =  ma.value;
		    displayed++;
		}
	    }
	}

	saveTableColumnWidths();

	if(displayed > 0)
	{
	    table_mode = SomeMeasSomeAttrTable;
	    attr_table.setModel(new CustomTableModel( table_data, col_names ));
	}
	else
	{
	    table_mode = EmptyTable;
	    attr_table.setModel(new DefaultTableModel( ));
	}
	
    }
    

    private void displayOneAttributeOfSomeMeasurements( int[] meas_ids, String attr_name )
    {
	//System.out.println("1a Sm" );

	final int n_meas = meas_ids.length;

	if(n_meas == 0)
	{
	    saveTableColumnWidths();
	    table_mode = EmptyTable;
	    attr_table.setModel(new DefaultTableModel( ));
	    return;
	}

	String[] col_names = new String[5];
	
	col_names[0] = "Measurement";
	col_names[1] = attr_name;
	col_names[2] = "Source";
	col_names[3] = "Created";
	col_names[4] = "Last Modified";
	
	Object[][] table_data = new Object[n_meas][];

	int displayed = 0;

	for(int m=0; m < n_meas; m++)
	{
	    ExprData.Measurement ms = edata.getMeasurement(meas_ids[m]);
	    
	    table_data[m] = new Object[5];
	    table_data[m][0] = ms.getName();
	    
	    Hashtable attrs = ms.getAttributes();
	    ExprData.Measurement.MeasurementAttr ma  = (ExprData.Measurement.MeasurementAttr) attrs.get(attr_name);

	    if(ma != null)
	    {
		table_data[m][1] = ma.value;
		table_data[m][2] = ma.source;
		table_data[m][3] = ma.time_created;
		table_data[m][4] = ma.time_last_modified;

		displayed++;
	    }
	    
	}
	
	// Arrays.sort(table_data, new ArrayComparator(sort_col, false));
	saveTableColumnWidths();

	if(displayed > 0)
	{
	    table_mode = SomeMeasOneAttrTable;
	    attr_table.setModel(new CustomTableModel( table_data, col_names ));
	}
	else
	{
	    table_mode = EmptyTable;
	    attr_table.setModel(new DefaultTableModel( ));
	}

    }

    private void displaySomeAttributesOfOneMeasurement( int meas_id, String[] attr_names )
    {
	//System.out.println("Sa 1m");

	ExprData.Measurement ms = edata.getMeasurement(meas_id);
	
	if(ms == null)
	{
	    saveTableColumnWidths();
	    table_mode = EmptyTable;
	    attr_table.setModel(new DefaultTableModel( ));
	    return;
	}

	String[] col_names = new String[5];

	col_names[0] = "Name";
	col_names[1] = "Value";
	col_names[2] = "Source";
	col_names[3] = "Created";
	col_names[4] = "Last Modified";

	Object[][] table_data = null;

	table_data = new Object[ attr_names.length ][];

	Hashtable attrs = ms.getAttributes();

	int displayed = 0;

	for (int n=0; n < attr_names.length; n++)
	{
	   ExprData.Measurement.MeasurementAttr ma = (ExprData.Measurement.MeasurementAttr) attrs.get(attr_names[n]); 
	
	   table_data[n] = new String[5];
	   
	   if(ma != null)
	   {
	       table_data[n][0] = ma.name;
	       table_data[n][1] = ma.value;
	       table_data[n][2] = ma.source;
	       table_data[n][3] = ma.time_created;
	       table_data[n][4] = ma.time_last_modified;

	       displayed++;
	   }
	}

	saveTableColumnWidths();

	if(displayed > 0)
	{
	    table_mode = OneMeasSomeAttrTable;
	    attr_table.setModel(new CustomTableModel( table_data, col_names ));
	}
	else
	{
	    attr_table.setModel(new DefaultTableModel( ));
	    table_mode = EmptyTable;
	}

    }

    private final static int EmptyTable            = 0;
    private final static int OneMeasOneAttrTable   = 1;
    private final static int SomeMeasOneAttrTable  = 2;
    private final static int SomeMeasSomeAttrTable = 3;
    private final static int OneMeasSomeAttrTable  = 4;

    private int table_mode = EmptyTable;


    private boolean isTableCellEditable(int row, int col) 
    { 
	switch( table_mode )
	{
	case OneMeasOneAttrTable:
	    return ((col == 1) && (( row==2) || (row==3)));
	case SomeMeasOneAttrTable:
	    return ((col == 1 ) || (col == 2));
	case OneMeasSomeAttrTable:
	    return ((col == 1 ) || (col == 2));
	case SomeMeasSomeAttrTable:
	    return (col > 0);
	}
	return false; 
    }
    
    private void handleCellEdit(int row, int col, Object value) 
    {
	if ( isTableCellEditable(row, col) )
	{
	    String mname = null;
	    String aname = null;
	    
	    int mod_r = -1;
	    int mod_c = -1;

	    CustomTableModel ctm = (CustomTableModel) attr_table.getModel();
	    
	    // things:  0==value, 1==source
	    int thing = -1;
	    
	    // figure out which Attribute of which Measurement  is being edited?
	    switch( table_mode )
	    {
	    case OneMeasOneAttrTable:
		mname = (String) meas_list.getSelectedValue();
		aname = (String) attr_list.getSelectedValue();
		thing = (row == 2) ? 0 : 1;
		mod_r = 5;
		mod_c = 1;
		break;
		
	    case SomeMeasOneAttrTable:
		mname = (String) ctm.getValueAt(row, 0);
		aname = (String) attr_list.getSelectedValue();
		thing = (col == 1) ? 0 : 1;
		mod_r = row;
		mod_c = 4;
		break;
		
	    case OneMeasSomeAttrTable:
		mname = (String) meas_list.getSelectedValue();
		aname = (String) ctm.getValueAt(row, 0);
		thing = (col == 1) ? 0 : 1;
		mod_r = row;
		mod_c = 4;
		break;
		
	    case SomeMeasSomeAttrTable:
		mname = (String) ctm.getValueAt(row, 0);
		aname = (String) ctm.getColumnName(col);
		thing = 0;
		break;
	    }
	    
	    if((mname == null) || (aname == null))
		return;
	    
	    // System.out.println("setting: m=" + mname + " a=" + aname);
	    
	    ExprData.Measurement ms = edata.getMeasurement( edata.getMeasurementFromName( mname ));
	    ExprData.Measurement.MeasurementAttr ma = (ExprData.Measurement.MeasurementAttr) ms.getAttributes().get(aname);
	    
	    if(ma == null)
	    {
		// this Attribute doesn't exist yet for this Measurement,
		// create it
		//
		ms.setAttribute( aname, "User", "" );
		ma = (ExprData.Measurement.MeasurementAttr) ms.getAttributes().get(aname);
		
	    }

	    boolean same = true;
	    String new_value = (String) value;
	    
	    if(thing == 0) // changing the value
	    {
		same = new_value.equals( ma.value );
		ma.value = new_value;
	    }
	    if(thing == 1) // changing the source
	    {
		same = new_value.equals( ma.source );
		ma.source = new_value;
	    }
	    if(!same)
	    {
		ma.time_last_modified = (new Date()).toString();
		
		if((mod_r >= 0) && (mod_c >= 0))
		    ctm.setValueAt( ma.time_last_modified, mod_r, mod_c );
	    }
	}
    }

    private class CustomTableModel extends DefaultTableModel
    {
	public CustomTableModel( Object[][] data, Object[] cols ) 
	{
	    super(data, cols);
	}
	
	public boolean isCellEditable(int row, int col) 
	{ 
	    return isTableCellEditable(row, col);
	}

	public void setValueAt(Object value, int row, int col) 
	{
	    // alter the local data model to reflect the edit
	    super.setValueAt(value, row, col);

	    // System.out.println("edit at " + row + "," + col + " = " + value);

	    handleCellEdit( row, col, value );
	    
	    fireTableCellUpdated(row, col);
	}



    }


    class CustomCellRenderer extends JTextField implements TableCellRenderer 
    {
	public CustomCellRenderer() 
	{
	    super();

	    setOpaque(true); //MUST do this for background to show up.

	    setBorder(null);
	    
	    normal_background       = Color.white;
	    search_match_background = new Color(230,230,230);

	    setSelectedTextColor( Color.green );

	    Font cur_font = getFont();
	    normal_font = new Font( cur_font.getName(), Font.PLAIN, cur_font.getSize() );
	    bold_font   = new Font( cur_font.getName(), Font.BOLD,  cur_font.getSize() );
	}
	
	public Component getTableCellRendererComponent(JTable table, Object object, 
						       boolean isSelected, boolean hasFocus,
						       int row, int col) 
	{
	    setBackground(normal_background);

	    if( isTableCellEditable(row, col))
	    {
		setForeground( Color.black );
		setFont( bold_font );
	    }
	    else	    {
		setForeground( Color.darkGray );
		setFont( normal_font );
	    }

	    if( object != null )
	    {
		String value = (String) object.toString();
		setText( value );
		
		if(col > 0)
		{
		    Dimension match_pos = matchSearchString( value );
		    if(match_pos != null)
		    {
			//System.out.println( "match in " + value + " " +
			//      		  match_pos.getWidth() + "..." + match_pos.getHeight() );
			
			// TODO: why doesn't this work??
			setCaretPosition( (int) match_pos.getWidth() );
			moveCaretPosition( (int) match_pos.getHeight() );

			// or this??
			select( (int) match_pos.getWidth(), (int) match_pos.getHeight() );
			
			setBackground( search_match_background );
			setForeground( Color.red );
			
		    }   
		}
	    }
	    else
	    {
		setText("");
	    }

	    return this;
	}

	private Font  normal_font, bold_font;
	private Color normal_background, search_match_background;

    }

    private void adjustTableColumnWidths()
    {
	JViewport jvp = (JViewport) attr_table.getParent();
	int table_width = jvp.getWidth();

	int cc = attr_table.getColumnCount();

	if(cc == 0)
	    return;

	try
	{
	    // have the column widths been previously set by the user?
	    if( table_mode == previous_table_mode )
	    {
		if( table_width != previous_table_width )
		{
		    
		}

		// reinstate the previous widths
		
		TableColumn column = null;
		for(int c=0; c < cc; c++)
		{
		    column = attr_table.getColumnModel().getColumn(c);
		    column.setPreferredWidth(previous_column_widths[c]);
		}
	    }
	    else
	    {
		
		setInitialTableColumnWidths();
	
	    }
	}
	catch( Exception ex )
	{
	    setInitialTableColumnWidths();
	}
    }

    private void setInitialTableColumnWidths()
    {
	JViewport jvp = (JViewport) attr_table.getParent();
	int table_width = jvp.getWidth();
	int cc = attr_table.getColumnCount();
	int col_width = (int)((double)table_width / (double)cc);
	
	TableColumn column = null;
	for(int c=0; c < cc; c++)
	{
	    column = attr_table.getColumnModel().getColumn(c);
	    column.setPreferredWidth(col_width);
	}
    }

    private void saveTableColumnWidths()
    {
	int cc = attr_table.getColumnCount();

	JViewport jvp = (JViewport) attr_table.getParent();
	previous_table_width = jvp.getWidth();

	previous_table_mode = table_mode;
	previous_column_widths = new int[cc];

	TableColumn column = null;
	for(int c=0; c < cc; c++)
	{
	    column = attr_table.getColumnModel().getColumn(c);
	    previous_column_widths[c] = column.getWidth();
	}
    }

    private int previous_table_mode = -1;
    private int previous_table_width = -1;
    private int[] previous_column_widths = null;

    // = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = = 

    private void updateAttributeNameList()
    {
	Vector attr_n_v = new Vector();
	Hashtable uniq = new Hashtable();
	for(int m=0;m<edata.getNumMeasurements();m++)
	{
	    ExprData.Measurement ms = edata.getMeasurement(m);
	    for (Enumeration e = ms.getAttributes().keys(); e.hasMoreElements() ;) 
	    {
		final String name = (String) e.nextElement();
		if(uniq.get(name) == null)
		{
		    attr_n_v.addElement(name);
		}
		uniq.put(name, name);
	    }
	}
	all_attr_names = (String[]) attr_n_v.toArray( new String[0] );
	Arrays.sort(all_attr_names);
	
	attr_list.setListData( all_attr_names );

	/*
	display_jcb.removeActionListener(meas_al);
	display_jcb.removeAllItems();

	for(int a=0; a<all_attr_names.length; a++)
	{
	    display_jcb.addItem( all_attr_names[a] );
	}

	display_jcb.addActionListener(meas_al);
	*/

    }


    private void sortColumn(int sort_col_)
    {
	sort_col = sort_col_;
	
	// displayData();
    }

    private String[] all_attr_names = null;
    private int sort_col = 0;

    private class ArrayComparator implements java.util.Comparator
    {
	public ArrayComparator(int mode_, boolean ascend_)
	{
	    mode = mode_;
	    ascend = ascend_;
	}
	public int compare(Object o1, Object o2)
	{
	    Object[] ma1 = (Object[]) o1;
	    Object[] ma2 = (Object[]) o2;

	    String n1 = (String) ma1[ mode ];
	    String n2 = (String) ma2[ mode ];

	    int res;

	    if(n1 == null)
		res = (n2 == null) ? 0 : 1;
	    else
		res = (n2 == null) ? -1 : (n1.compareTo(n2));
		
	    return ascend ? -res : res;
	}
	
	public boolean equals(Object o) { return false; }

	private boolean ascend;
	private int mode;
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  T e x t   S e a r ch i n g 
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  


    /* 
      Interesting functionality:

      
         1.  find text in any attrbiute and select the attributes and measurements
	     containing the text

	 2a. find text in only the selected attributes, select the measurements 
	     containing the text

	 2b. find text in only the selected measurements, select the attributes 
	     containing the text  (possibly not as useful as 2a?)

	 3.  suport for regexps

    */

    private boolean search_case_sens = false;
    private boolean search_substring = true;
    private String  search_string    = "";
    private String  search_string_lc = "";
  
    private void doFindText()
    {
	search_string    = attr_search_jtf.getText();
	search_string_lc = search_string.toLowerCase();
	attr_table.repaint();
    }

    private Dimension matchSearchString( final String value )
    {
	if(( search_string == null ) || ( search_string.length() == 0 ))
	{
	    return null;
	}

	if( search_substring )
	{
	    int pos = search_case_sens ? value.indexOf(search_string) : 
		                         value.toLowerCase().indexOf(search_string_lc);
	    if(pos < 0)
		return null;
	    else
		return new Dimension( pos, pos+search_string.length() );
	}
	else
	{
	    boolean hit = search_case_sens ? value.equals(search_string) : 
		                             value.toLowerCase().equals(search_string_lc);

	    if(hit)
		return new Dimension(0, search_string.length());
	    else
		return null;
	}
    }

    // handles any changes in text fields
    //
    private class SearchStringDocumentListener implements DocumentListener 
    {
	public void insertUpdate(DocumentEvent e)  
	{ 
	    propagate(e); 
	}

	public void removeUpdate(DocumentEvent e)  { propagate(e); }
	public void changedUpdate(DocumentEvent e) { propagate(e); }

	private void propagate(DocumentEvent e)
	{
	    doFindText();
	}
    }

    // select any attributes and measurements which match the 'find' text
    // (only consider those that are currently selected)
    //
    private void selectMatchedText()
    {
	Hashtable meas_ht = new Hashtable();
	ListModel lm = meas_list.getModel();
	for(int m=0; m < lm.getSize(); m++)
	{
	    if(meas_list.isSelectedIndex(m))
	       meas_ht.put( lm.getElementAt(m), new Integer(m) );
	}
	Hashtable attr_ht = new Hashtable();
	lm = attr_list.getModel();
	for(int a=0; a < lm.getSize(); a++)
	{
	    if(attr_list.isSelectedIndex(a))
	       attr_ht.put( lm.getElementAt(a), new Integer(a) );
	}

	ExprData.Measurement.MeasurementAttr ma = null;

	HashSet matched_meas_names = new HashSet();
	HashSet matched_attr_names = new HashSet();

	for(Enumeration me = meas_ht.keys(); me.hasMoreElements() ;) 
	{
	    String meas_name = (String) me.nextElement();

	    int m_id = edata.getMeasurementFromName( meas_name );
	    Hashtable atts = edata.getMeasurement(m_id).getAttributes();
	    
	    for(Enumeration ae = attr_ht.keys(); ae.hasMoreElements() ;) 
	    {
		String attr_name = (String) ae.nextElement();

		ma = (ExprData.Measurement.MeasurementAttr) atts.get(attr_name);
		
		if( (matchSearchString(ma.value) != null) ||
		    (matchSearchString(ma.source) != null) ||
		    (matchSearchString(ma.time_created) != null) ||
		    (matchSearchString(ma.time_last_modified) != null) )
		{
		    matched_meas_names.add( meas_name );
		    matched_attr_names.add( attr_name );
		}
	    }
	}
	
	// generate list indices for the matched measurements and attributes

	int[] sel_meas = new int[ matched_meas_names.size() ];
	Iterator i =  matched_meas_names.iterator();
	int p = 0;
	while( i.hasNext() ) 
	{
	    Integer index = (Integer) meas_ht.get( (String) i.next() );
	    sel_meas[p++] = index.intValue();
	}
	meas_list.setSelectedIndices(sel_meas);


	int[] sel_attr = new int[ matched_attr_names.size() ];
	i =  matched_attr_names.iterator();
	p = 0;
	while( i.hasNext() ) 
	{
	    Integer index = (Integer) attr_ht.get( (String) i.next() );
	    sel_attr[p++] = index.intValue();
	}
	attr_list.setSelectedIndices(sel_attr);

	
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  A t t r i b u t e   E x p o r t i n g
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  


    /*
      Possible Export Formats:
      
         MeasName1   AttrName1   AttrVal
	 MeasName1   AttrName2   AttrVal
	 MeasName1   AttrName3   AttrVal
	 MeasName2   AttrName1   AttrVal
	 MeasName2   AttrName2   AttrVal
	 MeasName2   AttrName3   AttrVal


         AttrName1   MeasName1   AttrVal
	 AttrName1   MeasName2   AttrVal
	 AttrName2   MeasName1   AttrVal
	 AttrName2   MeasName2   AttrVal
	 AttrName3   MeasName1   AttrVal
	 AttrName3   MeasName2   AttrVal


                    AttrName   AttrName  AttrName 
	 MeasName   AttrVal    AttrVal   AttrVal
	 MeasName   AttrVal    AttrVal   AttrVal
	 MeasName   AttrVal    AttrVal   AttrVal

	 
	            MeasName   MeasName   MeasName
	 AttrName   AttrVal    AttrVal    AttrVal 
	 AttrName   AttrVal    AttrVal    AttrVal 
	 AttrName   AttrVal    AttrVal    AttrVal 
	 AttrName   AttrVal    AttrVal    AttrVal 

     */
    
    private final static String[] export_format_names = 
    { "One per line, grouped by Measurement",
      "One per line, grouped by Attribute",
      "Tabular, one Measurement per row",
      "Tabular, one Measurement per column"
    };

    private int export_format_code = 0;

    private final static String[] delim_names =
    {
	"TAB", "Comma", "Space"
    };
    private final static char[] column_delim_chars = { '\t', ',', ' ' };
    private int export_delim_char_code = 0;


    private boolean export_include_headers   = true;
    private boolean export_include_source    = true;
    private boolean export_include_timestamp = true;

    private void exportSelectedAttributes()
    {
	if(export_attr_frame != null)
	{
	    export_attr_frame.setVisible(true);
	    return;
	}

	export_attr_frame = new JFrame("Export Attributes");

	mview.decorateFrame( export_attr_frame );

	export_attr_table = new JTable();

	export_attr_table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	
	export_attr_table.setCellSelectionEnabled( false );

	export_attr_table.setRowSelectionAllowed( true );
	export_attr_table.setColumnSelectionAllowed( true );

	export_attr_table.setTableHeader( null );

	JPanel panel = new JPanel();
	panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	GridBagLayout bag = new GridBagLayout();
	panel.setLayout(bag);
	GridBagConstraints c;
	JLabel label;
	JButton jb;
	Dimension fillsize = new Dimension(10,10);
	Box.Filler filler;

	JScrollPane jsp = new JScrollPane(export_attr_table);
	c = new GridBagConstraints();
	c.gridy = 1;
	c.weighty = 8.0;
	c.weightx = 10.0;
	c.fill = GridBagConstraints.BOTH;
	bag.setConstraints(jsp, c);
	panel.add(jsp);
	
	// --------------------

	export_include_headers   = mview.getBooleanProperty("MeasurementManager.export_include_headers", true);
	export_include_source    = mview.getBooleanProperty("MeasurementManager.export_include_source", true);
	export_include_timestamp = mview.getBooleanProperty("MeasurementManager.export_include_timestamp", true);

	export_delim_char_code   = mview.getIntProperty("MeasurementManager.export_delim_char_code", 0 );

	export_format_code       = mview.getIntProperty("MeasurementManager.export_format_code", 0 );

	// --------------------

	JPanel wrap = new JPanel();
	wrap.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
	GridBagLayout wrapbag = new GridBagLayout();
	wrap.setLayout(wrapbag);

	label = new JLabel("Format ");
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	c.weightx = 0.1;
	//c.fill = GridBagConstraints.HORIZONTAL;
	c.anchor = GridBagConstraints.EAST;
	wrapbag.setConstraints(label, c);
	wrap.add(label);

	final JComboBox format_jcb = new JComboBox( export_format_names );
	format_jcb.setSelectedIndex( export_format_code );
	format_jcb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    export_format_code = format_jcb.getSelectedIndex();
		    updateExportPreview( export_attr_table );
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 0;
	c.weightx = 9.0;
	//c.fill = GridBagConstraints.HORIZONTAL;
	c.anchor = GridBagConstraints.WEST;
	wrapbag.setConstraints(format_jcb, c);
	wrap.add(format_jcb);


	label = new JLabel("Delimiter ");
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 1;
	c.weightx = 0.1;
	//c.fill = GridBagConstraints.HORIZONTAL;
	c.anchor = GridBagConstraints.EAST;
	wrapbag.setConstraints(label, c);
	wrap.add(label);


	final JComboBox delim_jcb = new JComboBox( delim_names );
	delim_jcb.setSelectedIndex( export_delim_char_code );
	delim_jcb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    export_delim_char_code = delim_jcb.getSelectedIndex();
		    updateExportPreview( export_attr_table );
		}
	    });

	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 1;
	c.weightx = 9.0;
	//c.fill = GridBagConstraints.HORIZONTAL;
	c.anchor = GridBagConstraints.WEST;
	wrapbag.setConstraints(delim_jcb, c);
	wrap.add(delim_jcb);


	final JCheckBox incl_hdrs_jchkb = new JCheckBox("Include Headers");
	incl_hdrs_jchkb.setSelected(export_include_headers);
	incl_hdrs_jchkb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    export_include_headers = incl_hdrs_jchkb.isSelected();
		    updateExportPreview( export_attr_table );
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 2;
	c.weightx = 9.0;
	//c.fill = GridBagConstraints.HORIZONTAL;
	c.anchor = GridBagConstraints.WEST;
	wrapbag.setConstraints(incl_hdrs_jchkb, c);
	wrap.add(incl_hdrs_jchkb);
	

	final JCheckBox incl_src_jchkb = new JCheckBox("Include Source");
	incl_src_jchkb.setSelected(export_include_source);
	incl_src_jchkb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    export_include_source = incl_src_jchkb.isSelected();	
		    updateExportPreview( export_attr_table );
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 3;
	c.weightx = 9.0;
	//c.fill = GridBagConstraints.HORIZONTAL;
	c.anchor = GridBagConstraints.WEST;
	wrapbag.setConstraints(incl_src_jchkb, c);
	wrap.add(incl_src_jchkb);
	

	final JCheckBox incl_tstmp_jchkb = new JCheckBox("Include Timestamps");
	incl_tstmp_jchkb.setSelected(export_include_timestamp);
	incl_tstmp_jchkb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    export_include_timestamp = incl_tstmp_jchkb.isSelected();	
		    updateExportPreview( export_attr_table );
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 4;
	c.weightx = 9.0;
	//c.fill = GridBagConstraints.HORIZONTAL;
	c.anchor = GridBagConstraints.WEST;
	wrapbag.setConstraints(incl_tstmp_jchkb, c);
	wrap.add(incl_tstmp_jchkb);
	

	c = new GridBagConstraints();
	c.gridy = 0;
	c.weightx = 10.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	bag.setConstraints(wrap, c);
	panel.add(wrap);

	// --------------------


	wrap = new JPanel();
	wrap.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
	wrapbag = new GridBagLayout();
	wrap.setLayout(wrapbag);

	jb = new JButton( "Export" );
	jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    
		    JFileChooser fc = new JFileChooser();
		    fc.setCurrentDirectory(new File(mview.getProperty("MeasurementManager.export_path", 
								      System.getProperty("user.dir"))));
		    
		    int returnVal = fc.showSaveDialog(frame);
		    
		    if (returnVal != JFileChooser.APPROVE_OPTION) 
			return;
		    
		    File attr_file = fc.getSelectedFile();

		    if( attr_file.exists() )
		    {
			if ( mview.alertQuestion("File exists, overwrite?", "Yes", "No" ) == 1 )
			    return;
		    }

		    mview.putProperty( "MeasurementManager.export_path", attr_file.getPath() );

		    mview.putIntProperty("MeasurementManager.export_delim_char_code", export_delim_char_code );
		    mview.putIntProperty("MeasurementManager.export_format_code", export_format_code );

		    mview.putBooleanProperty("MeasurementManager.export_include_headers",   export_include_headers);
		    mview.putBooleanProperty("MeasurementManager.export_include_source",    export_include_source);
		    mview.putBooleanProperty("MeasurementManager.export_include_timestamp", export_include_timestamp);
		    
		    if ( doExport( attr_file, export_attr_table ) )
		    {
			export_attr_frame.setVisible(false);
			export_attr_table = null;
			export_attr_frame = null;
		    }

		}
	    });

	c = new GridBagConstraints();
	c.gridx = 0;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.EAST;
	wrapbag.setConstraints(jb, c);
	wrap.add(jb);

	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.gridx = 1;
	wrapbag.setConstraints(filler, c);
	wrap.add(filler);

	jb = new JButton( "Help" );
	jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    mview.getPluginHelpTopic( "MeasurementManager",  "MeasurementManager", "#exportattr" );
				       
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 2;
	wrapbag.setConstraints(jb, c);
	wrap.add(jb);

	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.gridx = 3;
	wrapbag.setConstraints(filler, c);
	wrap.add(filler);

	jb = new JButton( "Cancel" );
	c = new GridBagConstraints();
	c.gridx = 4;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.WEST;
	wrapbag.setConstraints(jb, c);
	wrap.add(jb);
	jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    export_attr_frame.setVisible(false);
		    export_attr_table = null;
		    export_attr_frame = null;
		}
	    });

	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 2;
	c.weightx = 10.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	bag.setConstraints(wrap, c);
	panel.add(wrap);

	// --------------------
	

	updateExportPreview( export_attr_table );

	panel.setPreferredSize( new Dimension(450, 400 ));

	export_attr_frame.addWindowListener(new WindowAdapter() 
	    {
		public void windowClosing(WindowEvent e)
		{
		    export_attr_table = null;
		    export_attr_frame = null;
		}
	    });
	
	export_attr_frame.getContentPane().add(panel);
	export_attr_frame.pack();
	export_attr_frame.setVisible(true);

    }

    private void updateExportPreview()
    {
	if(( export_attr_frame != null ) && ( export_attr_table != null ))
	    updateExportPreview( export_attr_table );
    }

    private void updateExportPreview( JTable table )
    {
	Vector data_v = constructExportData();

	int max_r = data_v.size();
	int max_c = 0;
	for(int r=0; r < max_r; r++)
	{
	    int c = ((Vector) data_v.elementAt(r)).size();
	    if(c > max_c)
		max_c = c;
	}

	ImmutableTableModel itm = new ImmutableTableModel( 0, max_c );

	for(int r=0; r < max_r; r++)
	    itm.addRow( (Vector) data_v.elementAt(r) );

	table.setModel( itm );
    }

    private Vector constructExportData()
    {
	Vector data_v = new Vector();
	Vector line_v = null;
	ExprData.Measurement.MeasurementAttr ma = null;

	final Object[] sel_meas = meas_list.getSelectedValues();
	final int n_sel_meas = (sel_meas == null) ? 0 : sel_meas.length;

	final Object[] sel_attr = attr_list.getSelectedValues();
	final int n_sel_attr = (sel_attr == null) ? 0 : sel_attr.length;
	    
	switch( export_format_code )
	{
	case 0:  // One per line, grouped by Measurement

	    if(export_include_headers)
	    {
		line_v = new Vector();
		line_v.addElement( "MeasurementName" );
		line_v.addElement( "AttributeName" );
		line_v.addElement( "Value"  );
		if(export_include_source)
		    line_v.addElement( "Source" );
		if(export_include_timestamp)
		{
		    line_v.addElement( "TimeCreated" );
		    line_v.addElement( "TimeLastModified" );
		}
		data_v.addElement( line_v );
	    }
	    
	    for(int m=0; m < n_sel_meas; m++)
	    {
		int m_id = edata.getMeasurementFromName( (String) sel_meas[m] );

		Hashtable atts = edata.getMeasurement(m_id).getAttributes();
		
		for(int a=0; a < n_sel_attr; a++)
		{
		    String name = (String) sel_attr[a];

		    ma = (ExprData.Measurement.MeasurementAttr) atts.get(name);
		    
		    if((ma != null) && (ma.value != null))
		    {
			line_v = new Vector();
			line_v.addElement( (String) sel_meas[m] );
			line_v.addElement( ma.name   );
			line_v.addElement( ma.value  );
			if(export_include_source)
			    line_v.addElement( ma.source );
			if(export_include_timestamp)
			{
			    line_v.addElement( ma.time_created );
			    line_v.addElement( ma.time_last_modified );
			}
			    
			data_v.addElement( line_v );
		    }
		}
	    }
	    break;

	case 1:  // One per line, grouped by Attribute
	    
	    if(export_include_headers)
	    {
		line_v = new Vector();
		line_v.addElement( "AttributeName" );
		line_v.addElement( "MeasurementName"   );
		line_v.addElement( "Value"  );
		if(export_include_source)
		    line_v.addElement( "Source" );
		if(export_include_timestamp)
		{
		    line_v.addElement( "TimeCreated" );
		    line_v.addElement( "TimeLastModified" );
		}
		data_v.addElement( line_v );
	    }

	    for(int a=0; a < n_sel_attr; a++)
	    {
		for(int m=0; m < n_sel_meas; m++)
		{
		    int m_id = edata.getMeasurementFromName( (String) sel_meas[m] );

		    Hashtable atts = edata.getMeasurement(m_id).getAttributes();

		    ma = (ExprData.Measurement.MeasurementAttr) atts.get(sel_attr[a]);

		    if((ma != null) && (ma.value != null))
		    {
			line_v = new Vector();
			line_v.addElement( ma.name   );
			line_v.addElement( edata.getMeasurementName(m_id)   );
			line_v.addElement( ma.value  );
			if(export_include_source)
			    line_v.addElement( ma.source );
			if(export_include_timestamp)
			{
			    line_v.addElement( ma.time_created );
			    line_v.addElement( ma.time_last_modified );
			}
			data_v.addElement( line_v );
		    }
		}
		

	    }
	    break;

	case 2:  // Tabular, one Measurement per row

	    if(export_include_headers)
	    {
		line_v = new Vector();
		line_v.addElement( null );
		for(int a=0; a < n_sel_attr; a++)
		    line_v.addElement( sel_attr[a] );
		data_v.addElement( line_v );
	    }

	    for(int m=0; m < n_sel_meas; m++)
	    {
		int m_id = edata.getMeasurementFromName( (String) sel_meas[m] );
		
		Hashtable atts = edata.getMeasurement(m_id).getAttributes();

		line_v = new Vector();

		line_v.addElement( edata.getMeasurementName(m_id) );
		
		for(int a=0; a < n_sel_attr; a++)
		{
		    ma = (ExprData.Measurement.MeasurementAttr) atts.get(sel_attr[a]);
		    
		    line_v.addElement( ma == null ? null : ma.value );
		}
		data_v.addElement( line_v );
	    }
	    break;

	case 3:  // Tabular, one Measurement per column

	    if(export_include_headers)
	    {
		line_v = new Vector();
		line_v.addElement( null );
		for(int m=0; m < n_sel_meas; m++)
		    line_v.addElement( (String) sel_meas[m] );
		data_v.addElement( line_v );
	    }

	    for(int a=0; a < n_sel_attr; a++)
	    {
		line_v = new Vector();

		line_v.addElement( sel_attr[a] );

		for(int m=0; m < n_sel_meas; m++)
		{
		    int m_id = edata.getMeasurementFromName( (String) sel_meas[m] );

		    Hashtable atts = edata.getMeasurement(m_id).getAttributes();

		    ma = (ExprData.Measurement.MeasurementAttr) atts.get(all_attr_names[a]);

		    line_v.addElement( ma == null ? null : ma.value );
		}
		data_v.addElement( line_v );
	    }
	    break;

	}

	return data_v;
    }

    private boolean doExport( File file, JTable table )
    {
	try
	{
	    PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));

	    Vector data_v = constructExportData();
	    
	    int lines = 0;

	    char delim = column_delim_chars[ export_delim_char_code ];

	    for(int r=0; r < data_v.size(); r++)
	    {
		Vector line_v = (Vector) data_v.elementAt(r);

		for(int c=0; c < line_v.size(); c++)
		{
		    String word = (String) line_v.elementAt(c);

		    if(c > 0)
			writer.write( delim );

		    if(word != null)
			writer.write(word);
		}
		
		lines++;

		writer.write("\n");
	    }

	    writer.close();
	    
	    mview.infoMessage( lines + " line" + 
			       ((lines == 1) ? "" : "s") +
			       " saved to " + file.getName() );

	    return true;
	    
	}
	catch(java.io.IOException e)
	{
	    mview.errorMessage("Unable to write to " + file.getName() + "\nerror: " + e);
	    return false;
	}

    }

    private void writeAttrsToFile( File file, boolean headers, boolean all )
    {
	try
	{
	    PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
	    
	    AbstractTableModel atm = (AbstractTableModel) attr_table.getModel();
	    ListSelectionModel lsm = attr_table.getSelectionModel();

	    int cc = atm.getColumnCount();
	    int rc = atm.getRowCount();

	    if(headers)
	    {
		for(int c=0; c < cc; c++)
		{
		    if(c > 0)
			writer.write( "\t" );
		    
		    writer.write( atm.getColumnName(c) );
		}
		writer.write( "\n" );
	    }

	    int lines = 0;

	    for(int r=0; r < rc; r++)
	    { 
		boolean is_selected = lsm.isSelectedIndex(r);

		if(all || is_selected)
		{
		    for(int c=0; c < cc; c++)
		    {
			if(c > 0)
			    writer.write( "\t" );
			
			String value = (String) atm.getValueAt( r, c );
			writer.write( value == null ? "" : value  );
		    }
		    writer.write( "\n" );

		    lines++;
		}
	    }

	    writer.close();

	    mview.infoMessage( lines + " Attribute" + 
			       ((lines == 1) ? "" : "s") +
			       " saved to " + file.getName() );

	}
	catch(java.io.IOException e)
	{
	    mview.errorMessage("Unable to write to " + file.getName() + "\nerror: " + e);
	}

    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  A t t r i b u t e   I m p o r t i n g
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    
    private boolean meas_names_tagged = false;
    private boolean attr_names_tagged = false;
    private boolean attr_vals_tagged  = false;

    private int meas_name_start_col;
    private int meas_name_end_col;
    private int meas_name_start_row;
    private int meas_name_end_row;

    private int attr_name_start_col;
    private int attr_name_end_col;
    private int attr_name_start_row;
    private int attr_name_end_row;

    private int attr_val_start_col;
    private int attr_val_end_col;
    private int attr_val_start_row;
    private int attr_val_end_row;

    private final static char[] import_delim_chars = { '\t', ',', ' ' };
    private int import_delim_char_code = 0;

    private void importAttributes()
    {
	JFileChooser fc = new JFileChooser();

	fc.setCurrentDirectory(new File(mview.getProperty("MeasurementManager.import_path", 
							  System.getProperty("user.dir"))));
	
	int returnVal = fc.showOpenDialog(frame);
	
	if (returnVal != JFileChooser.APPROVE_OPTION) 
	    return;
	
	
	File attr_file = fc.getSelectedFile();

	mview.putProperty( "MeasurementManager.import_path", attr_file.getPath() );


	displayImportAttrTable( attr_file  );
    }

    private Vector tokeniseFile( File file, int delim_char_code )
    {
	Vector data = new Vector();

	final char delim_ch = column_delim_chars[ delim_char_code  ];

	try
	{
	    BufferedReader br = new BufferedReader(new FileReader(file));

	    String line = br.readLine();
		
	    while(line != null)
	    {
		Vector tokens = new Vector();
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
		
		// add these tokens to the data

		// System.out.println( tokens.size() +  " tokens read from line");

		data.addElement(tokens);

		line = br.readLine();
	    }
	}
	catch(FileNotFoundException fnfe)
	{
	    //pm.stopIt();
	    mview.errorMessage("File not found");
	    return null;
	}
	catch(IOException ioe)
	{
	    mview.errorMessage("File cannot be read\n" + ioe);
	    return null;
	}

	return data;
    }


    private void updateImportPreview( final File source_file, final JTable table, int delim_char_code )
    {
	Vector data = tokeniseFile( source_file, delim_char_code );

	if(data == null)
	    return;

	// System.out.println( data.size() +  " lines read from file");

	// how may columns?
	int max_cols = 0;
	for(int row=0; row < data.size(); row++)
	{
	    int cols = ((Vector)data.elementAt(row)).size();
	    if(cols > max_cols)
		max_cols = cols;
	}
	// create column names
	final Vector col_names = new Vector();
	for(int col=0; col < max_cols; col++)
	    col_names.addElement( String.valueOf(col+1) );

	// rectangularise the data matrix (i.e. pad any short lines)
	for(int row=0; row < data.size(); row++)
	{
	    int cols = ((Vector)data.elementAt(row)).size();
	    while(cols < max_cols)
	    {
		((Vector)data.elementAt(row)).addElement( null );
		cols++;
	    }
	}


	table.setModel( new ImmutableTableModel( data, col_names ));
	table.setDefaultRenderer(Object.class, new ImportAttrCellRenderer());

	
    }


    private void displayImportAttrTable( final File source_file )
    {
	if(import_attr_frame != null)
	{
	    import_attr_frame.setVisible(false);
	}

	meas_names_tagged = false;
	attr_names_tagged = false;
	attr_vals_tagged = false;
	    
	import_attr_frame = new JFrame("Import Attributes");

	import_delim_char_code = mview.getIntProperty("MeasurementManager.import_delim_char_code", 0 );

	mview.decorateFrame( import_attr_frame );

	final JTable table = new JTable();

	JPanel panel = new JPanel();
	panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	GridBagLayout bag = new GridBagLayout();
	panel.setLayout(bag);
	GridBagConstraints c;
	JLabel label;
	JButton jb;
	Dimension fillsize = new Dimension(10,10);
	Box.Filler filler;

	final JButton import_jb = new JButton("Import");
	
	
	final JButton tag_meas_name_jb = new JButton("Measurement Name(s)");
	final JButton tag_attr_name_jb = new JButton("Attribute Name(s)");
	final JButton tag_attr_val_jb  = new JButton("Attribute Value(s)");
	

	final JComboBox delim_jcb = new JComboBox( delim_names );

	updateImportPreview( source_file, table, import_delim_char_code );

	JScrollPane jsp = new JScrollPane(table);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 1;
	c.weighty = 9.0;
	c.weightx = 10.0;
	c.fill = GridBagConstraints.BOTH;
	bag.setConstraints(jsp, c);
	panel.add(jsp);
	
	ListSelectionModel lsm = table.getSelectionModel();
	lsm.addListSelectionListener(new ListSelectionListener()
	    {
		public void valueChanged(ListSelectionEvent e) 
		{
		    if (e.getValueIsAdjusting()) 
			return;
		    
		    armTagButtons( table,
				   tag_meas_name_jb, 
				   tag_attr_name_jb,
				   tag_attr_val_jb );
		}
	    });
	

	table.setColumnSelectionAllowed(true);
	table.setRowSelectionAllowed(true);
	table.setCellSelectionEnabled(true);


	// = = = = = = = = = 

	JPanel wrap = new JPanel();
	wrap.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
	GridBagLayout wrapbag = new GridBagLayout();
	wrap.setLayout(wrapbag);
	
	int cline = 0;

	label = new JLabel("Delimiter ");
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = cline;
	c.weightx = 0.5;
	c.anchor = GridBagConstraints.EAST;
	wrapbag.setConstraints(label, c);
	wrap.add(label);


	delim_jcb.setSelectedIndex( import_delim_char_code );
	delim_jcb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    import_delim_char_code = delim_jcb.getSelectedIndex();
		    updateImportPreview( source_file, table, import_delim_char_code );
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = cline++;
	c.gridwidth = 2;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.WEST;
	//c.fill = GridBagConstraints.HORIZONTAL;
	wrapbag.setConstraints(delim_jcb, c);
	wrap.add(delim_jcb);


	label = new JLabel("Tag cells as ");
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = cline;
	c.weightx = 0.5;
	c.anchor = GridBagConstraints.EAST;
	wrapbag.setConstraints(label, c);
	wrap.add(label);

	//tag_meas_name_jb.setFont(mview.getSmallFont());
	tag_meas_name_jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    tagCells( table, 0 );
		    //table.setModel( new ImmutableTableModel( data, col_names ));
		    table.repaint();
		    import_jb.setEnabled( checkCellsAndLoad( table, false ) > 0 );
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = cline;
	c.weightx = 1.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	wrapbag.setConstraints(tag_meas_name_jb, c);
	wrap.add(tag_meas_name_jb);
	
	label = new JLabel(" (shown in Black)");
	//label.setFont(mview.getSmallFont());
	c = new GridBagConstraints();
	c.gridx = 2;
	c.gridy = cline++;
	c.weightx = 0.5;
	c.anchor = GridBagConstraints.WEST;
	wrapbag.setConstraints(label, c);
	wrap.add(label);

	//tag_attr_name_jb.setFont(mview.getSmallFont());
	tag_attr_name_jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    tagCells( table, 1 );
		    //table.setModel( new ImmutableTableModel( data, col_names ));
		    table.repaint();
		    import_jb.setEnabled( checkCellsAndLoad( table, false ) > 0 );
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = cline;
	c.weightx = 1.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	wrapbag.setConstraints(tag_attr_name_jb, c);
	wrap.add(tag_attr_name_jb);

	label = new JLabel(" (shown in blue)");
	//label.setFont(mview.getSmallFont());
	c = new GridBagConstraints();
	c.gridx = 2;
	c.gridy = cline++;
	c.weightx = 0.5;
	c.anchor = GridBagConstraints.WEST;
	wrapbag.setConstraints(label, c);
	wrap.add(label);

	//tag_attr_val_jb.setFont(mview.getSmallFont());
	tag_attr_val_jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    tagCells( table, 2 );
		    //table.setModel( new ImmutableTableModel( data, col_names ));
		    table.repaint();
		    import_jb.setEnabled( checkCellsAndLoad( table, false ) > 0 );
	
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = cline;
	c.weightx = 1.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	wrapbag.setConstraints(tag_attr_val_jb, c);
	wrap.add(tag_attr_val_jb);
	
	label = new JLabel(" (shown in green)");
	//label.setFont(mview.getSmallFont());
	c = new GridBagConstraints();
	c.gridx = 2;
	c.gridy = cline++;
	c.weightx = 0.5;
	c.anchor = GridBagConstraints.WEST;
	wrapbag.setConstraints(label, c);
	wrap.add(label);



	


	c = new GridBagConstraints();
	c.fill = GridBagConstraints.HORIZONTAL;
	c.weightx = 10.0;
	c.gridx = 0;
	c.gridy = 0;
	bag.setConstraints(wrap, c);
	panel.add(wrap);

	// = = = = = = = = = 

	wrap = new JPanel();
	wrap.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
	wrapbag = new GridBagLayout();
	wrap.setLayout(wrapbag);

	import_jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    if( checkCellsAndLoad( table, true ) > 0)
		    {
			mview.putIntProperty("MeasurementManager.import_delim_char_code", import_delim_char_code );
			
			import_attr_frame.setVisible(false);
			import_attr_frame = null;
		    }
		}
	    });
	import_jb.setEnabled(false);
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.EAST;
	c.gridx = 0;
	c.gridy = 2;
	c.weightx = 1.0;
	wrapbag.setConstraints(import_jb, c);
	wrap.add(import_jb);

	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 2;
	wrapbag.setConstraints(filler, c);
	wrap.add(filler);

	jb = new JButton("Help");
	jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    mview.getPluginHelpTopic( "MeasurementManager",  "MeasurementManager", "#importattr" );
				       
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 2;
	c.gridy = 2;
	wrapbag.setConstraints(jb, c);
	wrap.add(jb);

	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.gridx = 3;
	c.gridy = 2;
	wrapbag.setConstraints(filler, c);
	wrap.add(filler);

	jb = new JButton("Cancel");
	jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    import_attr_frame.setVisible(false);
		    import_attr_frame = null;
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 4;
	c.gridy = 2;
	c.anchor = GridBagConstraints.WEST;
	c.weightx = 1.0;
	wrapbag.setConstraints(jb, c);
	wrap.add(jb);


	c = new GridBagConstraints();
	c.fill = GridBagConstraints.HORIZONTAL;
	c.weightx = 10.0;
	c.gridx = 0;
	c.gridy = 2;
	bag.setConstraints(wrap, c);
	panel.add(wrap);

	// = = = = = = = = = 

	armTagButtons( table,
		       tag_meas_name_jb, 
		       tag_attr_name_jb,
		       tag_attr_val_jb );

	panel.setPreferredSize( new Dimension(450, 400 ));

	import_attr_frame.addWindowListener(new WindowAdapter() 
	    {
		public void windowClosing(WindowEvent e)
		{
		    import_attr_frame = null;
		}
	    });

	import_attr_frame.getContentPane().add(panel, BorderLayout.CENTER);
	import_attr_frame.pack();
	import_attr_frame.setVisible(true);
	
    }

    private void armTagButtons( JTable table, JButton mn_jb, JButton an_jb, JButton av_jb )
    {
	int[] sel_cols = table.getSelectedColumns();
	int[] sel_rows = table.getSelectedRows();
	
	boolean is_block = ( sel_cols.length > 1 ) && ( sel_rows.length > 1 );
	boolean is_line = ((( sel_cols.length >= 1 ) && ( sel_rows.length == 1 )) ||
			   (( sel_cols.length == 1 ) && ( sel_rows.length >= 1 )));

	av_jb.setEnabled( is_line || is_block );
	mn_jb.setEnabled( is_line );
	an_jb.setEnabled( is_line );
    }

    class ImmutableTableModel extends DefaultTableModel
    {
	public ImmutableTableModel( Vector data, Vector cnames )
	{
	    super( data,cnames );
	}
	public ImmutableTableModel( Object[][] data, Object[] cnames )
	{
	    super( data,cnames );
	}
	public ImmutableTableModel( int n_cols, int n_rows )
	{
	    super( n_cols, n_rows  );
	}
	public boolean isCellEditable(int row, int col) 
	{ 
	    return false;
	}
    }

    class ImportAttrCellRenderer extends JLabel implements TableCellRenderer 
    {
	public ImportAttrCellRenderer() 
	{
	    super();
	    setOpaque(true); //MUST do this for background to show up.

	    sel_colour = SystemColor.textHighlight;
	}
	
	public Component getTableCellRendererComponent(JTable table, Object object, 
						       boolean isSelected, boolean hasFocus,
						       int row, int col) 
	{
	    setForeground( Color.lightGray );

	    if( isSelected )
		setBackground( sel_colour );
	    else
		setBackground( Color.white );

	    if(attr_vals_tagged)
	    {
		if(inside( row, col, 
			   attr_val_start_row, attr_val_end_row, 
			   attr_val_start_col, attr_val_end_col ) )
		{
		    setForeground( Color.green ); 
		}
	    }

	    if(attr_names_tagged)
	    {
		if(inside( row, col, 
			   attr_name_start_row, attr_name_end_row, 
			   attr_name_start_col, attr_name_end_col ) )
		{
		    setForeground( Color.blue ); 
		}
	    }

	    if(meas_names_tagged)
	    {
		if(inside( row, col, 
			   meas_name_start_row, meas_name_end_row, 
			   meas_name_start_col, meas_name_end_col ) )
		{
		    if( known_meas_names_hs == null )
			known_meas_names_hs = makeMeasNamesHashSet();
		    
		    setForeground( Color.black ); 

		    if( known_meas_names_hs.contains( (String) object ) == false )
			setBackground( Color.red );
		}
	    }

	    
	    setText( (object == null) ? "" : object.toString() );

	    return this;
	}

	private boolean inside( int r, int c, int start_r, int end_r, int start_c, int end_c )
	{
	    if((r < start_r) || (r > end_r))
		return false;
	    if((c < start_c) || (c > end_c))
		return false;

	    return true;
	}

	private Color sel_colour;
    }


    private java.util.HashSet makeMeasNamesHashSet()
    {
	java.util.HashSet names = new java.util.HashSet();

	final int nm = edata.getNumMeasurements();
	
	for(int m=0; m < nm; m++)
	    names.add( edata.getMeasurementName( m ) );

	return names;
    }


    private void tagCells( JTable table, int thing )
    {
	try
	{
	    int start_c = -1;
	    int end_c   = -1;
	    int start_r = -1;
	    int end_r   = -1;
	    
	    int[] sel_cols = table.getSelectedColumns();
	    int[] sel_rows = table.getSelectedRows();
	    
	    boolean good = false;

	    if( sel_cols.length == 1)
	    {
		start_c = end_c = sel_cols[0];
		start_r = sel_rows[0];
		end_r   = sel_rows[ sel_rows.length - 1 ];
		
		good = true;
	    }
	    if( sel_rows.length == 1)
	    {
		start_r = end_r = sel_rows[0];
		start_c = sel_cols[0];
		end_c   = sel_cols[ sel_cols.length - 1 ];
		
		good = true;
	    }

	    if( thing == 2 )  // values can be a block
	    {
		start_c = sel_cols[0];
		end_c   = sel_cols[ sel_cols.length - 1 ];
		
		start_r = sel_rows[0];
		end_r   = sel_rows[ sel_rows.length - 1 ];

		good = true;
	    }

	    if(!good)
	    {
		String msg = "Select a single cell, or an unbroken line of cells,";
		if( thing == 0 )
		    msg += "\nto tag as Measurement names";
		if( thing == 1 )
		    msg += "\nto tag as Attribute names";
		if( thing == 2 )
		    msg = "Select a single cell, an unbroken line of cells,\n" + 
			  " or a block of cellsto tag as Attribute values";

		mview.alertMessage(msg);
	    }

	    if(thing == 0)
	    {
		meas_name_start_col = start_c;
		meas_name_end_col = end_c;
		meas_name_start_row = start_r;
		meas_name_end_row = end_r;
		meas_names_tagged = true;
	    }
	    if(thing == 1)
	    {
		attr_name_start_col = start_c;
		attr_name_end_col = end_c;
		attr_name_start_row = start_r;
		attr_name_end_row = end_r;
		attr_names_tagged = true;
	    }
	    if(thing == 2)
	    {
		attr_val_start_col = start_c;
		attr_val_end_col = end_c;
		attr_val_start_row = start_r;
		attr_val_end_row = end_r;
		attr_vals_tagged = true;
	    }

	    // work out the area covered by the meas_name and attr_name tags
	    if( !attr_vals_tagged && meas_names_tagged && attr_names_tagged )
	    {
		if(( meas_name_start_col == meas_name_end_col ) && 
		   ( attr_name_start_row != meas_name_end_row ))
		{
		    // meas names are row oriented, attr names are col oriented
		    attr_val_start_col = attr_name_start_col;
		    attr_val_end_col   = attr_name_end_col;
		    attr_val_start_row = meas_name_start_row;
		    attr_val_end_row   = meas_name_end_row;
		    attr_vals_tagged = true;
		}
		
		if(( meas_name_start_row == meas_name_end_row ) && 
		   ( attr_name_start_col != meas_name_end_col ))
		{ 
		    // meas names are column oriented
		    attr_val_start_col = meas_name_start_col;
		    attr_val_end_col   = meas_name_end_col;
		    attr_val_start_row = attr_name_start_row;
		    attr_val_end_row   = attr_name_end_row;
		    attr_val_end_row   = meas_name_end_row;
		    attr_vals_tagged = true;
		}
	    }

	}
	catch(Exception ex)
	{
	}
	
    }


    private int checkCellsAndLoad( JTable table, boolean load ) 
    {
	if( !meas_names_tagged )
	{
	    if(load)
		mview.alertMessage("No cells have been tagged as containing Measurement names");
	    return -1;
	}
	if( !attr_names_tagged )
	{
	    if(load)
		mview.alertMessage("No cells have been tagged as containing Attribute names");
	    return -1;
	}
	if( !attr_vals_tagged )
	{
	    if(load)
	       mview.alertMessage("No cells have been tagged as containing Attribute values");
	    return -1;
	}

	int meas_name_format = getFormat( meas_name_start_row, meas_name_end_row,
					  meas_name_start_col, meas_name_end_col );
	int attr_name_format = getFormat( attr_name_start_row, attr_name_end_row,
					  attr_name_start_col, attr_name_end_col );
	int attr_val_format  = getFormat( attr_val_start_row, attr_val_end_row,
					  attr_val_start_col, attr_val_end_col );

	int n_meas_names = getSize( meas_name_start_row, meas_name_end_row,
				    meas_name_start_col, meas_name_end_col );
	int n_attr_names = getSize( attr_name_start_row, attr_name_end_row,
				    attr_name_start_col, attr_name_end_col );
	int n_attr_vals  = getSize( attr_val_start_row, attr_val_end_row,
				    attr_val_start_col, attr_val_end_col );

	/*
	System.out.println("meas_name_format=" + meas_name_format + 
			   " attr_name_format=" + attr_name_format+
			   " attr_val_format="  + attr_val_format);
	
	System.out.println("n_meas_names=" + n_meas_names + 
			   " n_attr_names=" + n_attr_names + 
			   " n_attr_vals="  + n_attr_vals);
	*/

	Vector import_data_v = null;
	if(load)
	   import_data_v = new Vector();

	if( ( meas_name_format == attr_name_format ) &&
	    ( meas_name_format == attr_val_format ) )
	{
	    // can be that all 3 are [row|col|singleton]
	    // should be the same number of each type of thing
	    if (( n_meas_names != n_attr_names ) || ( n_meas_names != n_attr_vals ))
		return -1;

	    // should be in a 'straight line'
	    if( meas_name_format == ColOrientedFormat )
	    {
		// revised in 1.0.4 so that starts & don't have to line up, as long as the length of the run is the same...

		if((  n_meas_names != n_attr_names ) || ( n_meas_names != n_attr_vals ) )
		    return -1;
		
/*
		if( ( meas_name_start_col != attr_name_start_col ) ||
		    ( meas_name_start_col != attr_val_start_col ) )
		    return -1;

		if( ( meas_name_end_col != attr_name_end_col ) ||
		    ( meas_name_end_col != attr_val_end_col ) )
		    return -1;
*/


		if(load)
		{
		   for(int m = 0; m < n_meas_names; m++)
		   {
		       String[] data_a = new String[3];
		       data_a[0] = (String) table.getValueAt( meas_name_start_row, meas_name_start_col + m );
		       data_a[1] = (String) table.getValueAt( attr_name_start_row, attr_name_start_col + m );
		       data_a[2] = (String) table.getValueAt( attr_val_start_row,  attr_val_start_col + m );
		       import_data_v.addElement( data_a );
		   } 

		   if( doMeasAttrImport( import_data_v ) < 0 )
		       return -1;
		}

		return n_meas_names;
	    }

	    if( meas_name_format == RowOrientedFormat )
	    {
		// revised in 1.0.4 so that starts & don't have to line up, as long as the length of the run is the same...

		if((  n_meas_names != n_attr_names ) || ( n_meas_names != n_attr_vals ) )
		    return -1;
		
		/*
		// all row_starts and row_ends should line up
		if( ( meas_name_start_row != attr_name_start_row ) ||
		    ( meas_name_start_row != attr_val_start_row ) )
		    return -1;
		if( ( meas_name_end_row != attr_name_end_row ) ||
		    ( meas_name_end_row != attr_val_end_row ) )
		    return -1;
		*/

		if(load)
		{
		    for(int m = 0; m < n_meas_names; m++)
		    {
			String[] data_a = new String[3];
			data_a[0] = (String) table.getValueAt( m + meas_name_start_row, meas_name_start_col );
			data_a[1] = (String) table.getValueAt( m + attr_name_start_row, attr_name_start_col );
			data_a[2] = (String) table.getValueAt( m + attr_val_start_row, attr_val_start_col  );
			import_data_v.addElement( data_a );
		    } 
		    
		    if( doMeasAttrImport( import_data_v ) < 0 )
			return -1;
		}

		return n_meas_names;
	    }

	    if( meas_name_format == SingletonFormat )
	    {
		
		// allow any positioning if there is exactly one of each thing
		if(load)
		{
		    String[] data_a = new String[3];
		    data_a[0] = (String) table.getValueAt( meas_name_start_row, meas_name_start_col );
		    data_a[1] = (String) table.getValueAt( attr_name_start_row, attr_name_start_col );
		    data_a[2] = (String) table.getValueAt( attr_val_start_row, attr_val_start_col  );
		    import_data_v.addElement( data_a );
		    if ( doMeasAttrImport( import_data_v ) < 0 )
			return -1;
		}

		return n_meas_names;

	    }

	    return -1;
	    
	}
	 
	if( ( meas_name_format == RowOrientedFormat ) && ( attr_name_format == ColOrientedFormat ) )
	{
	    // should be a rectangular layout
	    if( attr_val_format != BlockFormat )
		return -1;
	    
	    if( n_attr_vals != ( n_meas_names * n_attr_names ))
		return -1;
	    
	    
	    // revised in 1.0.4 so that starts & don't have to line up, as long as the length of the run is the same...

	    int n_attr_rows = ( attr_val_end_row - attr_val_start_row ) + 1;
	    int n_attr_cols  = ( attr_val_end_col - attr_val_start_col ) + 1;
	    
	    if( n_attr_names != n_attr_cols )
		return -1;
	    if( n_meas_names != n_attr_rows )
		return -1;
	    
	    if(load)
	    {
		for(int m = 0; m <  n_meas_names; m++)
		{
		    for(int a = 0; a < n_attr_names; a++)
		    {
			String[] data_a = new String[3];

			data_a[0] = (String) table.getValueAt( meas_name_start_row + m, meas_name_start_col ); // meas name
			data_a[1] = (String) table.getValueAt( attr_name_start_row, attr_name_start_col + a  ); // attr name
			data_a[2] = (String) table.getValueAt( attr_val_start_row + m, attr_val_start_col + a );   // attr value

			import_data_v.addElement( data_a );
		    }
		}

		
		if( doMeasAttrImport( import_data_v ) < 0 )
		    return -1;
	    }

	    return n_attr_vals;
	}

	
	if( ( meas_name_format == ColOrientedFormat ) && ( attr_name_format == RowOrientedFormat ) )
	{
	    // should be a rectangular layout
	    if( attr_val_format != BlockFormat )
		return -1;
	    
	    if( n_attr_vals != ( n_meas_names * n_attr_names ))
		return -1;
	    

	    // revised in 1.0.4 so that starts & don't have to line up, as long as the length of the run is the same...

	    int n_attr_rows = ( attr_val_end_row - attr_val_start_row ) + 1;
	    int n_attr_cols  = ( attr_val_end_col - attr_val_start_col ) + 1;
	    

	    if( n_attr_names != n_attr_rows )
		return -1;
	    if( n_meas_names != n_attr_cols )
		return -1;


	    if(load)
	    {
		for(int m = 0; m <  n_meas_names; m++)
		{
		    for(int a = 0; a < n_attr_names; a++)
		    {
			String[] data_a = new String[3];
			
			data_a[0] = (String) table.getValueAt( meas_name_start_row, meas_name_start_col + m  ); // meas name
			data_a[1] = (String) table.getValueAt( attr_name_start_row + a, attr_name_start_col ); // attr name
			data_a[2] = (String) table.getValueAt( attr_val_start_row + a, attr_val_start_col + m );   // attr value

			import_data_v.addElement( data_a );
		    }
		}
		if( doMeasAttrImport( import_data_v ) < 0 )
		    return -1;
	    }
	    return n_attr_vals;	    
	}


	if( meas_name_format == SingletonFormat ) 
	{
	    if( attr_name_format == ColOrientedFormat )
	    {
		if( attr_val_format != ColOrientedFormat )
		    return -1;

		if( n_attr_names != n_attr_vals )
		    return -1;

		if(load)
		{
		    for(int a = 0; a < n_attr_names; a++)
		    {
			String[] data_a = new String[3];
			data_a[0] = (String) table.getValueAt( meas_name_start_row, meas_name_start_col );
			data_a[1] = (String) table.getValueAt( attr_name_start_row, attr_name_start_col + a );
			data_a[2] = (String) table.getValueAt( attr_val_start_row,  attr_val_start_col + a );
			import_data_v.addElement( data_a );
		    } 
		    
		    if( doMeasAttrImport( import_data_v ) < 0 )
			return -1;
		}

		return n_attr_names;
	    }

	    if( attr_name_format == RowOrientedFormat )
	    {
		if( attr_val_format != RowOrientedFormat )
		    return -1;

		if( n_attr_names != n_attr_vals )
		    return -1;

		if(load)
		{
		    for(int a = 0; a < n_attr_names; a++)
		    {
			String[] data_a = new String[3];
			data_a[0] = (String) table.getValueAt( meas_name_start_row, meas_name_start_col );
			data_a[1] = (String) table.getValueAt( attr_name_start_row + a, attr_name_start_col );
			data_a[2] = (String) table.getValueAt( attr_val_start_row + a, attr_val_start_col  );
			import_data_v.addElement( data_a );
		    } 
		    
		    if( doMeasAttrImport( import_data_v ) < 0 )
			return -1;
		}


		return n_attr_names;
	    }

	   
	}

	if( attr_name_format == SingletonFormat ) 
	{
	   if( meas_name_format == ColOrientedFormat )
	   { 
	       if( attr_val_format != ColOrientedFormat )
		   return -1;
	       
	       if( n_meas_names != n_attr_vals )
		   return -1;
	       
	       if(load)
	       {
		   for(int m = 0; m <  n_meas_names; m++)
		   {
		       String[] data_a = new String[3];
		       data_a[0] = (String) table.getValueAt( meas_name_start_row, meas_name_start_col + m );
		       data_a[1] = (String) table.getValueAt( attr_name_start_row, attr_name_start_col );
		       data_a[2] = (String) table.getValueAt( attr_val_start_row, attr_val_start_col + m  );
		       import_data_v.addElement( data_a );
		   } 
		   if( doMeasAttrImport( import_data_v ) < 0 )
		       return -1;
	       }

	       return n_meas_names;
	   }

	   if( meas_name_format == RowOrientedFormat )
	   { 
	       if( attr_val_format != RowOrientedFormat )
		   return -1;
	       
	       if( n_meas_names != n_attr_vals )
		   return -1;
	       
	       if(load)
	       {
		   for(int m = 0; m <  n_meas_names; m++)
		   {
		       String[] data_a = new String[3];
		       data_a[0] = (String) table.getValueAt( meas_name_start_row + m, meas_name_start_col );
		       data_a[1] = (String) table.getValueAt( attr_name_start_row, attr_name_start_col );
		       data_a[2] = (String) table.getValueAt( attr_val_start_row + m, attr_val_start_col  );
		       import_data_v.addElement( data_a );
		   } 
		   if( doMeasAttrImport( import_data_v ) < 0 )
		       return -1;
	       }

	       return n_meas_names;
	   }

	}

	return -1;
    }


    final static int SingletonFormat    = 0;    // 1r x 1c
    final static int RowOrientedFormat  = 1;    // Nr x 1c
    final static int ColOrientedFormat  = 2;    // 1r x Nc
    final static int BlockFormat        = 3;    // Nr x Nc
    
    private int doMeasAttrImport( Vector import_data_v )
    {

	// debugging
	//
	System.out.println( "================================================");
	for(int d=0; d < import_data_v.size(); d++)
	{
	    String[] data_a = (String[]) import_data_v.elementAt(d);

	    System.out.println( "meas=" +  data_a[0] +
				" attr=" + data_a[1] +
				" val=" +  data_a[2] );
	}
	System.out.println( "================================================");
	
	
	
	// check whether all measurement names are recognised
	//

	if( known_meas_names_hs == null )
	    known_meas_names_hs = makeMeasNamesHashSet();


	int unknown_meas_name = 0;
	for(int d=0; d < import_data_v.size(); d++)
	{
	    String[] data_a = (String[]) import_data_v.elementAt(d);

	    if( known_meas_names_hs.contains( data_a[0] ) == false )
		unknown_meas_name++;
	}

	if( unknown_meas_name > 0)
	{
	    if( mview.infoQuestion( "One or more Measurement names were not recognised. Continue importing data for those names that were recongised?",
				    "Yes", "No" ) == 1 ) 
		return -1;
	}
	
	// check whether any existing data will be overwritten
	//
	int value_exists = 0;
	for(int d=0; d < import_data_v.size(); d++)
	{
	    String[] data_a = (String[]) import_data_v.elementAt(d);

	    int m_id = edata.getMeasurementFromName(data_a[0]);

	    if( m_id >= 0 )
	    {
		ExprData.Measurement ms = edata.getMeasurement(m_id);
		
		ExprData.Measurement.MeasurementAttr ma = (ExprData.Measurement.MeasurementAttr) ms.getAttributes().get(data_a[1]);
		
		if(ma != null)
		    value_exists++;
	    }
	}

	int replace_mode = -1;

	if(value_exists > 0)
	{
	    replace_mode = mview.alertQuestion( "One or more Attributes with these names are already defined\n" + 
						"The existing values can be replaced, or new\n" +
						"names can be generated for the data being imported",
						"Replace", "Rename");
	}
	
	// now do the actual import....
	//
	int new_values      = 0;
	int replaced_values = 0;
	int renamed_values  = 0;

	final String src_name = "Measurement Manager 1.0";

	for(int d=0; d < import_data_v.size(); d++)
	{
	    String[] data_a = (String[]) import_data_v.elementAt(d);

	    int m_id = edata.getMeasurementFromName(data_a[0]);

	    if( m_id >= 0 )
	    {
		ExprData.Measurement ms = edata.getMeasurement(m_id);
		
		ExprData.Measurement.MeasurementAttr ma = (ExprData.Measurement.MeasurementAttr) ms.getAttributes().get(data_a[1]);
		
		if(ma != null)
		{
		    // value already exists, either replace or rename
		    
		    if(replace_mode == 0)
		    {
			// replace existing value
			//
			ms.setAttribute( data_a[1], src_name, data_a[2] );
			replaced_values++;
		    }
		    else
		    {
			// rename attribute
		    //
			String new_name = generateUniqueName( data_a[1], ms.getAttributes() );
			
			ms.setAttribute( new_name, src_name, data_a[2] );
			renamed_values++;
		    }
		}
		else
		{
		    // value doesn't already exist, add it
		    //
		    ms.setAttribute( data_a[1], src_name, data_a[2] );
		    new_values++;
		}
	    }
	}
	
	String report = "";

	if(new_values == 1)
	    report += "\nOne new attribute name/value pair was imported";
	if(new_values > 1)
	    report += "\n" + new_values + " new attribute name/value pairs were imported";

	if(replaced_values == 1)
	    report += "\nOne existing attribute name/value pair was replaced";
	if(replaced_values > 1)
	    report += "\n" + replaced_values + " existing attribute name/value pairs were replaced";

	if(renamed_values == 1)
	    report += "\nOne attribute name/value pair was renamed and imported";
	if(renamed_values > 1)
	    report += "\n" + renamed_values + " attribute name/value pairs were renamed and imported";
	
	updateAttributeNameList();

	mview.infoMessage( report );

	return import_data_v.size();
    }

    private String generateUniqueName( String input, Hashtable existing )
    {
	boolean unique = false;
	String new_name = input;
	int count = 1;
	while(!unique)
	{
	    new_name =  input + "(" + String.valueOf(++count) + ")";
	    unique = (existing.get(new_name) == null);
	}
	return new_name;
    }

    private int getFormat( int start_r, int end_r, int start_c, int end_c )
    {
	if( ( start_r == end_r ) &&  ( start_c == end_c ) )
	    return SingletonFormat;
	if( ( start_r == end_r ) &&  ( start_c != end_c ) )
	    return ColOrientedFormat;
	if( ( start_r != end_r ) &&  ( start_c == end_c ) )
	    return RowOrientedFormat;
	
	return BlockFormat;
    }

    private int getSize( int start_r, int end_r, int start_c, int end_c )
    {
	return ((end_r - start_r)+1) * ((end_c - start_c)+1);
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  ExternalSelectionListener implementation
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    int esl_handle;

    public void spotSelectionChanged(int[] spot_ids) {}
    public void clusterSelectionChanged(ExprData.Cluster[] clusters) {}
    public void spotMeasurementSelectionChanged(int[] spot_ids, int[] meas_ids)
    {
	importMeasurementSelection( meas_ids);
    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  observer implementation
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

    public void measurementUpdate(ExprData.MeasurementUpdateEvent sue)
    {
	// guard against infinte loops where updates trigger updates....
	if(ignore_measurement_updates)
	    return;

	
	switch(sue.event)
	{
	case ExprData.SizeChanged:
	    break;

	case ExprData.NameChanged:
	case ExprData.VisibilityChanged:
	    known_meas_names_hs = null;
	    updateMeasList();
	    updatePropertyDisplay();
	    break;

	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	case ExprData.OrderChanged:
	    known_meas_names_hs = null;
	    updateMeasList();
	    updatePropertyDisplay();
	    updateAttributeNameList();
	    updateAttributeDisplay();
	    break;

	case ExprData.ColouriserAdded:
	case ExprData.ColouriserRemoved:
	case ExprData.ColouriserChanged:
	    updatePropertyDisplay();
	    updateColouriserList();
	    break;
	}
    }
    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private boolean sync_meas_sel;
    private boolean orient_stats_by_meas = true;
    private boolean lock_stats_table_width = true;

    private JFrame export_attr_frame = null;
    private JFrame import_attr_frame = null;

    private JTable export_attr_table = null;

    private java.util.HashSet known_meas_names_hs = null;

    private maxdView mview;
    private ExprData edata;
    private DataPlot dplot;
    private JFrame   frame;

}
