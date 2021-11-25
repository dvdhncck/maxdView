import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.*;
import java.io.*;

public class Normalise implements ExprData.ExprDataObserver,Plugin
{
    public Normalise(maxdView mview_)
    {
	mview = mview_;
	edata = mview.getExprData();
    }

    private void initialiseNormalisers()
    {
	plugin_dir = mview.getPluginDirectory("Normalise");

	normalisers = loadNormalisers( plugin_dir );

	normaliser_names = new String[normalisers.length];
	for(int n=0; n < normalisers.length; n++)
	    normaliser_names[n] = normalisers[n].getName();
    }

    public void cleanUp()
    {
	edata.removeObserver(this);

	mview.putBooleanProperty( "Normalise.apply_filter", apply_filter_jchkb.isSelected() );

	try
	{
	    if(method_jcb != null)
	    {
		String method = (String) method_jcb.getSelectedItem();
		
		Normaliser normaliser = getNormaliserByName( method );

		if(normaliser != null)
		    normaliser.saveProperties(mview);
		
		if(normaliser != null)
		    mview.putProperty("Normalise.normaliser", normaliser.getName());
	    }
	}
	catch(NullPointerException npe)
	{
	    // in case any normalisers were initialised but never had their UI constructed
	}

	if(frame != null)
	    frame.setVisible(false);
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  plugin implementation
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void startPlugin()
    {
	initialiseNormalisers();

	// pick the initial normaliser using the app.props.

	String nname = mview.getProperty("Normalise.normaliser", normaliser_names[0]);

	selected_normaliser = getNormaliserByName( nname );

	addComponents();
	
	edata.addObserver(this);

	frame.pack();
	frame.setVisible(true);

	split_pane.setDividerLocation( split_panel_divider_loc );

    }

    public void stopPlugin()
    {
	cleanUp();
    }
  
    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("Normalise", 
					 "transform", 
					 "Normalise data (several methods supported)", 
					 "Portions of this code developed by David Hoyle, Magnus Rattray and Norman Morrisson<BR>" +
					 "(c) 2001 Manchester Bioinformatics",
					 1, 1, 0);
	return pinf;
    }
    
    public PluginCommand[] getPluginCommands()
    {
	int n_norm_commands = 0;
	String g_opts = "";

	if(normaliser_names == null)
	    initialiseNormalisers();

	if(normaliser_names == null)
	    return null;

	PluginCommand[] norm_pc_a = new PluginCommand[normaliser_names.length];
	
	for(int n=0; n < normalisers.length; n++)
	{
	    if((norm_pc_a[n] = normalisers[n].getCommand()) != null)
		n_norm_commands++;

	    if(n > 1)
		g_opts += ((n+1) < normaliser_names.length) ? ", " : " or ";
	    g_opts += "'" + normaliser_names[n] + "'";

	}
	
	PluginCommand[] com = new PluginCommand[3 + n_norm_commands];

	final String[] common_args = new String[] 
	{ 
	    // name             // type               //default   // flag   // comment
	    "select",          "string",              "all",        "m",     "either 'all' or 'list'",
	    "measurements",    "measurement_list",    "",           "",      "used when 'select'='list'",
	    "apply_filter",    "boolean",             "false",      "",      "", 
	    "mode",            "string",              "create_new", "m",     "either 'in_place' or 'create_new'",
	    "new_name_prefix", "string",              "",       "",          "name prefix for new Measurements when 'mode'='create_new'"
	};

	final String[] args = new String[] 
	{ 
	    // name             // type               //default     // flag   // comment
	    "select",          "string",              "all",        "m",     "either 'all' or 'list'",
	    "measurements",    "measurement_list",    "",           "",      "used when 'select'='list'",
	    "method",          "string",              "",           "m",     "any installed method: " + g_opts,
	    "apply_filter",    "boolean",             "false",      "",      "", 
	    "mode",            "string",              "create_new", "m",     "either 'in_place' or 'create_new'",
	    "new_name_prefix", "string",              "",       "",          "name prefix for new Measurements when 'mode'='create_new'"
	};

	com[0] = new PluginCommand("start",args);
     	com[1] = new PluginCommand("set",  args);
	com[2] = new PluginCommand("stop", null);

	int np = 3;
	for(int n=0; n < normaliser_names.length; n++)
	{
	    if(norm_pc_a[n] != null)
	    {
		com[np] = norm_pc_a[n];

		com[np].args = mergeArgs( com[np].args, common_args );

		np++;
	    }
	}

	return com;
    }

    private String[] mergeArgs( String[] a1, String[] a2 )
    {
	if(a2 == null)
	    return a1;
	if(a1 == null)
	    return a2;

	String[] res = new String[ a1.length + a2.length ];
	
	int ap = 0;
	for(int a=0; a < a1.length; a++)
	    res[ap++] = a1[a];

	for(int a=0; a < a2.length; a++)
	    res[ap++] = a2[a];

	return res;
    }

    private void parseCommon(String[] args)
    {
	gotoPrevTab();
	
	String sel = mview.getPluginStringArg("select", args, "all");
	
	if(sel.startsWith("li"))
	{
	    String[] m_names = mview.getPluginMeasurementListArg( "measurements", args, null );
	    
	    meas_list.selectItems( m_names);
	}
	else
	{
	    meas_list.selectAll();
	}
	
	apply_filter_jchkb.setSelected( mview.getPluginBooleanArg("apply_filter", args, false) );
	
	gotoNextTab();
	
	String method = mview.getPluginStringArg("method", args, null);
	if(method != null)
	{
	    
	    for(int n=0; n <  normaliser_names.length; n++)
	    {
		if(normaliser_names[n].startsWith(method))
		{
		    method_jcb.setSelectedIndex( n );
		}
	    }
	}
	
	new_name_prefix = mview.getPluginArg("new_name_prefix", args, null);
	
	String mmode = mview.getPluginStringArg("mode", args, "create_new");
	create_mode = mmode.startsWith("in") ? 1 : 0;
    }

    public void runCommand(String name, String[] args, CommandSignal done) 
    {
	boolean started_this_time = false;

	if( name.equals("set") || name.equals("start") )
	{
	    if(frame == null)
		startPlugin();

	    parseCommon(args);
	}
	else
	{
	    if( name.equals("stop") )
	    {
		cleanUp();
	    }
	    else
	    {
		// check each normaliser...
		if(frame == null)
		{
		    started_this_time = true;
		    startPlugin();
		}

		if(normaliser_names == null)
		    initialiseNormalisers();

		for(int n=0; n < normaliser_names.length; n++)
		{
		    // Normaliser norm = loadNormaliser(plugin_dir, normaliser_names[n]);

		    PluginCommand pc = normalisers[n].getCommand();

		    if((pc != null) && (pc.name.equals( name )))
		    {
			selected_normaliser = normalisers[n];

			parseCommon(args);
			
			method_jcb.setSelectedIndex( n );
			
			if(selected_normaliser != null)
			{
			    // setup the parameters
			    selected_normaliser.parseArguments( mview, args );

			    // and invoke the normalisation....
			    doNormalise( create_mode == 1 );
			}
		    }
		}
	    }
	}

	if(started_this_time)
	    cleanUp();

	if(done != null)
	    done.signal();
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
    }

    public void clusterUpdate(ExprData.ClusterUpdateEvent cue)
    {
	// reset the label?
    }

    public void measurementUpdate(ExprData.MeasurementUpdateEvent mue)
    {
	switch(mue.event)
	{
	case ExprData.SizeChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	case ExprData.NameChanged:
	case ExprData.OrderChanged:
	    meas_list.setModel(new MeasListModel());
	    // updateDisplay();
	    
	    break;
	}	
    }
    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  gooey
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private int mode = 0;

    private void addComponents()
    {
	frame = new JFrame("Normalise");

	mview.decorateFrame(frame);
	
	frame.addWindowListener(new WindowAdapter() 
			  {
			      public void windowClosing(WindowEvent e)
			      {
				  cleanUp();
			      }
			  });

	JPanel o_panel = new JPanel();
	o_panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	GridBagLayout o_gridbag = new GridBagLayout();
	o_panel.setLayout(o_gridbag);

	tabbed = new JTabbedPane();
	tabbed.setEnabled(false);

	o_panel.setMinimumSize(new Dimension(640, 480));
	o_panel.setPreferredSize(new Dimension(640, 480));
	
	GridBagConstraints c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	c.weightx = 10.0;
	c.weighty = 9.0;
	c.fill = GridBagConstraints.BOTH;
	o_gridbag.setConstraints(tabbed, c);
	o_panel.add(tabbed);

	{
	    // the measurement picking panel
	    
	    GridBagLayout gridbag = new GridBagLayout();
	    
	    DragAndDropPanel panel = new DragAndDropPanel();
	    
	    panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	    
	    panel.setLayout(gridbag);
	    //panel.setMinimumSize(new Dimension(500, 350));
	    //panel.setPreferredSize(new Dimension(500, 350));

	    int n_cols = 2;
	    int line = 0;
	    
	    meas_list = new DragAndDropList();
	    JScrollPane jsp = new JScrollPane(meas_list);
	    meas_list.setModel(new MeasListModel());
	    meas_list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.gridwidth = 2;
	    c.weighty = c.weightx = 8.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(jsp, c);
	    panel.add(jsp);
	    
	    line++;

	    JButton sel_all_jb = new JButton("Select All");
	    sel_all_jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			meas_list.setSelectionInterval(0, edata.getNumMeasurements()-1);
		    }
		});

	    sel_all_jb.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    gridbag.setConstraints(sel_all_jb, c);
	    panel.add(sel_all_jb);


	    tabbed.add(" Pick Measurements ", panel);
	    
	}
	    
	// ===== options ======================================================

	{
	    int line = 0;
	    GridBagLayout gridbag = new GridBagLayout();
	    
	    JPanel panel = new JPanel();

	    panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	    
	    panel.setLayout(gridbag);


	    // --------------------------------------------------------------


	    JPanel meth_panel = new JPanel();
	    meth_panel.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
	    GridBagLayout meth_gridbag = new GridBagLayout();
	    meth_panel.setLayout(meth_gridbag);


	    apply_filter_jchkb = new JCheckBox("Apply filter");
	    apply_filter_jchkb.setSelected( mview.getBooleanProperty( "Normalise.apply_filter", false ));

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.EAST;
	    meth_gridbag.setConstraints(apply_filter_jchkb, c);
	    meth_panel.add(apply_filter_jchkb);

	    
	    Dimension fillsize = new Dimension(48,16);
	    Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    meth_gridbag.setConstraints(filler, c);
	    meth_panel.add(filler);

	    
	    JLabel label = new JLabel("Method ");
	    c = new GridBagConstraints();
	    c.gridx = 2;
	    //c.anchor = GridBagConstraints.EAST;
	    meth_gridbag.setConstraints(label, c);
	    meth_panel.add(label);

	    method_jcb = new JComboBox(normaliser_names);
	    
	    if(selected_normaliser != null)
	    {
		// work out which one...
		method_jcb.setSelectedItem( selected_normaliser.getName() );
	    }

	    method_jcb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			// save the parameters of the currently selected normaliser

			if(selected_normaliser != null)
			{
			    try
			    {
				selected_normaliser.saveProperties(mview);
			    }
			    catch(NullPointerException npe)
			    {
				// in case it never had its UI constructed
			    }
			}

			// and setup the newly selected one
			String method = (String) method_jcb.getSelectedItem();
			selected_normaliser = getNormaliserByName( method );
			setupNormaliser();
			updateLabels();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = 3;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    meth_gridbag.setConstraints(method_jcb, c);
	    meth_panel.add(method_jcb);


	    c.gridx = 0;
	    c.gridy = line++;
	    c.weightx = 10.0;
	    //c.gridwidth = 3;
	    c.anchor = GridBagConstraints.NORTH;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(meth_panel, c);
	    panel.add(meth_panel);

	    // --------------------------------------------------------------


	    ui_panel = new JPanel();
	    ui_panel.setBorder(BorderFactory.createEmptyBorder());


	    info_text_area = new JTextPane();
	    info_text_area.setBorder(BorderFactory.createEmptyBorder());
	    info_text_area.setMinimumSize(new Dimension(400, 300));
	    info_text_area.setEditable(false);
	    info_text_area.setFont(mview.getSmallFont());
	    info_text_area.setBackground(ui_panel.getBackground());
	    
	    split_pane = new JSplitPane();
	    //split_pane.setBorder(BorderFactory.createEmptyBorder());
	    
	    JScrollPane left_jsp = new JScrollPane(ui_panel);
	    left_jsp.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
	    split_pane.setLeftComponent( left_jsp );

	    JScrollPane right_jsp = new JScrollPane(info_text_area);
	    right_jsp.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
	    split_pane.setRightComponent( right_jsp );

	    //split_pane.setDividerLocation( 0.65 );

	    /*
	    GridBagLayout infobag = new GridBagLayout();
	    split_pane.setLayout(infobag);
	    c = new GridBagConstraints();
	    c.weightx = 9.0;
	    c.weighty = 9.0;
	    c.fill = GridBagConstraints.BOTH;
	    infobag.setConstraints(jsp, c);
	    split_pane.add(jsp);
	    */

	    c = new GridBagConstraints();
	    //c.gridx = 0;
	    c.gridy = line;
	    c.weightx = 10.0;
	    c.weighty = 7.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(split_pane, c);
	    panel.add(split_pane);

	    line++;

	    // ============================================

	    JPanel wrapper = new JPanel();
	    wrapper.setBorder(BorderFactory.createEmptyBorder(5,5,0,5));
	    GridBagLayout wrapbag = new GridBagLayout();
	    wrapper.setLayout(wrapbag);

	    in_place_jb = new JButton("In place");
	    c = new GridBagConstraints();
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    c.anchor = GridBagConstraints.SOUTH;
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    in_place_jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			doNormalise(true);
		    }
		});
	    wrapbag.setConstraints(in_place_jb, c);
	    wrapper.add(in_place_jb);
	    
	    label = new JLabel("Changes the data values");
	    label.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridy = 1;
	    c.weightx = 1.0;
	    c.weighty = 0.1;
	    c.anchor = GridBagConstraints.NORTH;
	    wrapbag.setConstraints(label, c);
	    wrapper.add(label);

	    ip_label = new JLabel("in the selected Measurements");
	    ip_label.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridy = 2;
	    c.weightx = 1.0;
	    c.weighty = 0.1;
	    c.anchor = GridBagConstraints.NORTH;
	    wrapbag.setConstraints(ip_label, c);
	    wrapper.add(ip_label);

	    fillsize = new Dimension(16,16);
	    filler = new Box.Filler(fillsize, fillsize, fillsize);
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    wrapbag.setConstraints(filler, c);
	    wrapper.add(filler);

	    JButton jb = new JButton("Create new");
	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    c.anchor = GridBagConstraints.SOUTH;
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    wrapbag.setConstraints(jb, c);
	    wrapper.add(jb);
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			doNormalise(false);
		    }
		});

	    cn_label = new JLabel("Makes new Measurements");
	    cn_label.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = 1;
	    c.weightx = 1.0;
	    c.weighty = 0.1;
	    c.anchor = GridBagConstraints.NORTH;
	    wrapbag.setConstraints(cn_label, c);
	    wrapper.add(cn_label);

	    label = new JLabel("containing normalised values");
	    label.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = 2;
	    c.weightx = 1.0;
	    c.weighty = 0.1;
	    c.anchor = GridBagConstraints.NORTH;
	    wrapbag.setConstraints(label, c);
	    wrapper.add(label);

	    // =============

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    //c.weightx = 1.0;
	    //c.weighty = 0.1;
	    //c.gridwidth = 3;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(wrapper, c);
	    panel.add(wrapper);

	    // ============================================


	    tabbed.add(" Normalise ", panel);
	}

	// ======= buttons =============================================================

	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrapper.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
	    wrapper.setLayout(w_gridbag);
	    nbutton  = new JButton("Next");
	    bbutton  = new JButton("Back");
	    bbutton.setEnabled(false);

	    {
		wrapper.add(bbutton);
		
		bbutton.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    gotoPrevTab();
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		//c.anchor = GridBagConstraints.EAST;
		w_gridbag.setConstraints(bbutton, c);
	    }
	    {
		wrapper.add(nbutton);
		
		nbutton.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    gotoNextTab();
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		//c.anchor = GridBagConstraints.EAST;
		w_gridbag.setConstraints(nbutton, c);
	    }
	    {
		Dimension fillsize = new Dimension(16,16);
		Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 0;
		w_gridbag.setConstraints(filler, c);
		wrapper.add(filler);
						   
	    }
	    {
		JButton button = new JButton("Close");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    cleanUp();
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 3;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		//c.anchor = GridBagConstraints.EAST;
		w_gridbag.setConstraints(button, c);
	    }
	    {
		JButton button = new JButton("Help");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    if( tabbed.getSelectedIndex() == 0 )
			    {
				mview.getPluginHelpTopic("Normalise", "Normalise");
			    }
			    else
			    {
				String method = (String) method_jcb.getSelectedItem();
		
				Normaliser normaliser = getNormaliserByName( method );
				
				if( normaliser != null )
				    mview.getPluginHelpTopic("Normalise", normaliser.getHelpDocumentName() );
			    }
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 4;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		w_gridbag.setConstraints(button, c);
	    }

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.weightx = 10.0;
	    o_gridbag.setConstraints(wrapper, c);
	    o_panel.add(wrapper);
	    
	}

	// setupNormaliser();

	frame.getContentPane().add(o_panel);
    }

    private void gotoPrevTab()
    {
	nbutton.setEnabled(true);
	if(--mode <= 0)
	{
	    mode = 0;
	    bbutton.setEnabled(false);
	}
	
	tabbed.setSelectedIndex(mode);
	
	//if(mode == 0)
	//doNothing...();
    }

    private void gotoNextTab()
    {
	bbutton.setEnabled(true);
	
	if(mode == 0)
	{
	    getData();
	    
	    setupNormaliser();
	    
	    final int[] sel_ind = meas_list.getSelectedIndices();
	    final int n_in = (sel_ind == null) ? 0 : sel_ind.length;

	    final int n_out = selected_normaliser.getNumberOfReturnedMeasurements( n_in );

	    if(n_in == 0)
	    {
		mview.alertMessage("You must select one or more Measurements");
		return;
	    }
	    
	    updateLabels();

	    if( n_in < selected_normaliser.getMinimumNumMeasurements() )
	    {
		mview.alertMessage("You must select at least one Measurement");
		return;
	    }
	}
	
	if(++mode >= 1)
	{
	    mode = 1;
	    nbutton.setEnabled(false);
	}
	
	tabbed.setSelectedIndex(mode);
	
    }

    private void updateLabels()
    {
	    final int[] sel_ind = meas_list.getSelectedIndices();
	    final int n_in = (sel_ind == null) ? 0 : sel_ind.length;

	    final int n_out = selected_normaliser.getNumberOfReturnedMeasurements( n_in );

	    String nm = (n_in == 1) ? "selected Measurement" : (n_in + " selected Measurements");
	    ip_label.setText("in the " + nm);
	    
	    nm = (n_out == 1) ? "a new Measurement" : (n_out + " new Measurements");
	    cn_label.setText("Make " + nm);
    }

    public class MeasListModel extends DefaultListModel
    {
	public Object getElementAt(int index) 
	{
	    return edata.getMeasurementName( edata.getMeasurementAtIndex(index) );
	}
	public int getSize() 
	{
	    return edata.getNumMeasurements();
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    

    private final void setupNormaliser()
    {
	//	final int method = method_jcb.getSelectedIndex();

	String method = (String) method_jcb.getSelectedItem();

	selected_normaliser = getNormaliserByName( method );

	if(selected_normaliser == null)
	{
	    mview.alertMessage("Unable to load the normaliser '" + method + "'");
	    return;
	}

	ui_panel.removeAll();

	if(selected_normaliser == null)
	{
	    info_text_area.setText("");
	}
	else
	{
	    info_text_area.setText(selected_normaliser.getInfo());
	
	    JPanel uip = selected_normaliser.getUI(mview, new NormaliserInput(data, measurement_names, spot_attr_names, data_id_to_spot_id));

	    if(uip != null)
	    {
		JScrollPane jsp = new JScrollPane(uip);
		jsp.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
		
		GridBagLayout ui_bag = new GridBagLayout();
		ui_panel.setLayout(ui_bag);
		
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 9.0;
		c.weighty = 9.0;
		c.anchor = GridBagConstraints.NORTH;
		c.fill = GridBagConstraints.BOTH;
		ui_bag.setConstraints(jsp, c);
		ui_panel.add(jsp);
	    }

	    in_place_jb.setEnabled( selected_normaliser.isOneToOne() );
	}

	ui_panel.revalidate();
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    int[] data_id_to_spot_id = null;
    int[] data_id_to_meas_id = null;

    String[] measurement_names = null;
    String[] spot_attr_names = null;

    private final void getData()
    {
	int unfiltered = 0;
	data_id_to_spot_id = null;

	if(apply_filter_jchkb.isSelected())
	{
	    // filtered, contruct new double[]s containing just the unfiltered spots
	    //
	    final int real_n_spots = edata.getNumSpots();
	    
	    for(int s=0; s < real_n_spots; s++)
		if(!edata.filter(s))
		    unfiltered++;
	    
	    data_id_to_spot_id = new int[unfiltered];
	    
	    // build an array mapping filtered index to 'real' index
	    //
	    
	    int spot_n = 0;
	    for(int s=0; s < real_n_spots; s++)
		if(!edata.filter(s))
		    data_id_to_spot_id[spot_n++] = s;
	    
	    // System.out.println(unfiltered + " unfiltered (from " + real_n_spots + ")");
	}    
		    
	// ===============================================================
	//
	// build a double[][] containing the filtered spots in the selected measurements
	//
	int n_meas = 0;
	for(int m=meas_list.getMinSelectionIndex(); m <= meas_list.getMaxSelectionIndex(); m++)
	{
	    if(meas_list.getSelectionModel().isSelectedIndex(m))
	    {
		n_meas++;
	    }
	}
	data_id_to_meas_id = new int[n_meas];
	
	data = new double[n_meas][];

	measurement_names = new String[n_meas];

	int nm = 0;
	for(int m=meas_list.getMinSelectionIndex(); m <= meas_list.getMaxSelectionIndex(); m++)
	{
	    if(meas_list.getSelectionModel().isSelectedIndex(m))
	    {
		int meas_id = edata.getMeasurementAtIndex(m);
		data_id_to_meas_id[ nm ] = meas_id;
		measurement_names[ nm ] = edata.getMeasurementName(meas_id);

		if(apply_filter_jchkb.isSelected())
		{
		    double[] raw_data = edata.getMeasurementData( meas_id );
		    data[nm] = new double[ unfiltered ];
		    
		    for(int s=0; s < unfiltered; s++)
			data[nm][s] = raw_data[data_id_to_spot_id[s]];
		}
		else
		{
		    // not filtered, just grab the actual double[] s
		    //
		    data[nm] = edata.getMeasurementData( meas_id );
		}
		
		nm++;
	    }
	}

	
	// generate a list of all of the Spot Attributes that are in one or more Measurements

	java.util.HashSet all_spot_attr_names_hs = new java.util.HashSet();

	for(int m=0; m < measurement_names.length; m++)
	{
	    int m_id = edata.getMeasurementFromName( measurement_names[m] );
	    ExprData.Measurement meas = edata.getMeasurement( m_id );
	    
	    for(int a=0; a < meas.getNumSpotAttributes(); a++)
	    {
		all_spot_attr_names_hs.add( meas.getSpotAttributeName( a ) );
	    }
	}

	spot_attr_names = (String[]) all_spot_attr_names_hs.toArray( new String[0] );
	
	Arrays.sort( spot_attr_names );
	

    }

    private final boolean doNormalise(boolean in_place)
    {
	String method = (String) method_jcb.getSelectedItem();

	selected_normaliser = getNormaliserByName( method );

	if(selected_normaliser == null)
	{
	    mview.alertMessage("Unable to load the normaliser '" + method + "'");
	    return false;
	}
	
	if( data.length < selected_normaliser.getMinimumNumMeasurements())
	{
	    mview.alertMessage("You must select at least " + 
			       selected_normaliser.getMinimumNumMeasurements() + 
			       " Measurements for this method");
	    return false;
	}

	frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

	if(!selected_normaliser.canHandleNaNs() && containsNaNs(data))
	{
	    frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	    mview.alertMessage("The selected data contains NaN values\n" + 
			       "which cannot be handled at this time.\n" +
			       "(You can use a Filter to remove these values)");
	    return false;
	}

	final boolean one_to_one = selected_normaliser.isOneToOne();

	NormaliserOutput output = selected_normaliser.normalise( new NormaliserInput( data, 
										      measurement_names, 
										      spot_attr_names,
										      data_id_to_spot_id ) );
	
	if (output == null )
	{
	    frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	    return false;
	}

	String norm_desc_str =  selected_normaliser.getName() + ":" + selected_normaliser.getSettings();
	
	if(output.data != null)
	{
	    // possibly convert back to unfiltered...
	    if(apply_filter_jchkb.isSelected())
	    {
		final int real_n_spots = edata.getNumSpots();
		final int n_spots = data_id_to_spot_id.length;
		
		for(int m=0; m < output.data.length; m++)
		{
		    double[] unfiltered_data = new double[ real_n_spots ];
		    
		    for(int s=0; s < real_n_spots; s++)
			unfiltered_data[s] = Double.NaN;
		    for(int s=0; s < n_spots; s++)
		    unfiltered_data[ data_id_to_spot_id[s] ] = output.data[m][s];
		    
		    output.data[m] = unfiltered_data;
		}
	    }
		

	    ExprData.Measurement meas = null;
	    
	    if(in_place)
	    {
		for(int m=0; m < data_id_to_meas_id.length; m++)
		{
		    meas = edata.getMeasurement( data_id_to_meas_id[m] );
		    meas.addAttribute( "Normalisation" , "Normalise v1.0", norm_desc_str );
		    edata.setMeasurementData( data_id_to_meas_id[m], output.data[m] );
		}
	    }
	    else
	    {
		String name_prefix = (new_name_prefix == null) ? (selected_normaliser.getName()+":") : new_name_prefix;
		String name;

		for(int m=0; m < output.data.length; m++)
		{
		    if( one_to_one )
			name = name_prefix + edata.getMeasurementName( data_id_to_meas_id[ m ] );
		    else
		    {
			
			name = ( output.data.length == 1 ) ? name_prefix : (name_prefix + ":" + String.valueOf( m+1 ));
		    }

		    meas = edata.new Measurement(name, ExprData.ExpressionAbsoluteDataType, output.data[m] );
		    
		    meas.addAttribute( "Normalisation" , "Normalise v1.0", norm_desc_str );
		    
		    edata.addOrderedMeasurement( meas );
		    
		    if(m==0)
			mview.getDataPlot().displayMeasurement(meas);
		}
	    }

	}
	else
	{
	    frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	    mview.alertMessage("The normalisation could not be performed using this method");
	    return false;
	}

	// ===============================================================
	//
	// finished....
	//

	frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

	return true;
    }

    private boolean containsNaNs(double[][] data)
    {
	final int n = data.length;
	
	if(n == 0)
	    return false;
	
	for(int i=0; i < n; i++)
	{
	    final int m = data[i].length;
	    
	    for(int j=0; j < m; j++)
		if(Double.isNaN( data[i][j] ))
		    return true;
	}
	return false;
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  witchcraft to locate potential normaliser classes
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private Normaliser[] loadNormalisers( String directory )
    {
	if(( directory == null ) || (directory.length() == 0))
	    return new Normaliser[0];


	File directory_file = new File(directory);

	String[] class_names = scanForClasses( directory_file, "Normaliser" );
	
	Normaliser[] normalisers = new Normaliser[ class_names.length ];

	for(int c=0; c < class_names.length; c++)
	{
	    normalisers[ c ] = loadNormaliser( directory_file, class_names[c] );
	}

	return normalisers;
    }

    // locates classes in 'directory' which implement 'interface'
    // returns an array of class names
    //
    private String[] scanForClasses( File directory_file, String interface_name )
    {
	Vector results = new Vector();

	try
	{
	    String[] directory_contents = directory_file.list();
	
	    java.net.URL[] directory_url = { directory_file.toURL() };
	    java.net.URLClassLoader ucl = new java.net.URLClassLoader( directory_url, getClass().getClassLoader() );
	    
	    for(int d=0; d < directory_contents.length; d++)
	    {
		File item = new File(directory_file, directory_contents[d]);
		
		String iname = item.getName();
		
		if(iname.toLowerCase().endsWith(".class"))
		{
		    if(iname.indexOf("$") < 0)  // ignore inner classes
		    {
			// this is a potential class so load it and check what interfaces it has
			
			try
			{
			    String cname = iname.substring(0, iname.length() - 6);  // strip the .class from the end
			    
			    //System.out.println("checking '" + cname + "'");
			    
			    Class cl = ucl.loadClass( cname );
			    
			    // get a list of the interfaces supported by this class
			    
			    Class[] interfaces = cl.getInterfaces();
		    
			    if((interfaces != null) && (interfaces.length > 0))
			    {
				for(int i=0; i< interfaces.length; i++)
				{
				    //System.out.println(".. " + " implements " + interfaces[i].getName());

				    if(interfaces[i].getName().equals(interface_name))
				    {
					results.add( cname );
				    }
				}
			    }
			}
			catch(Exception ex)
			{
			    System.out.println("scanForClasses(): inner:" + ex);
			}
			
		    }
		}
		
	    }
	}
	catch(Exception ex)
	{
	    System.out.println("scanForClasses(): outer:" + ex);
	}

	return (String[]) results.toArray( new String[0] );
    }

    // loads and instantiates a class

    private Normaliser loadNormaliser( File directory_file, String name )
    {
	//System.out.println("loadNormaliser() dir=" + directory_file.getPath() + " name=" + name);

	try
	{
	    java.net.URL[] directory_url = { directory_file.toURL() };
	    java.net.URLClassLoader ucl = new java.net.URLClassLoader( directory_url, getClass().getClassLoader() );
	    //System.out.println("loadNormaliser() classLoader ok");
	    Class cl = ucl.loadClass( name );
	    //System.out.println("loadNormaliser() class ok");
	    Object object = cl.newInstance();
	    //System.out.println("loadNormaliser() object ok");
	    return (Normaliser) object;
	}
	catch(Exception ex)
	{
	    System.out.println("loadNormaliser:" + ex);
	    return null;
	}

    }

    private Normaliser getNormaliserByName( String target )
    {
	if((normaliser_names == null) || (normalisers == null))
	    return null;

	for(int n=0; n < normaliser_names.length; n++)
	    if( normaliser_names[n].equals( target ))
		return normalisers[n];
	return null;
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  gubbins
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private Normaliser[]  normalisers = null;
    private String[]      normaliser_names = null;
    private String        plugin_dir = null;

    private Normaliser    selected_normaliser = null;

    private JCheckBox apply_filter_jchkb;
    private JComboBox method_jcb;
    private JTextPane info_text_area;
    private JLabel ip_label, cn_label;
    private JButton in_place_jb;

    private double split_panel_divider_loc = 0.65;

    private JPanel ui_panel;
    private JSplitPane split_pane;

    private double[][] data = null;

    private maxdView mview;
    private DataPlot dplot;
    private ExprData edata;
 
    private int create_mode;
    private String new_name_prefix;

    private JFrame frame = null;
    private JTabbedPane tabbed;
    private JButton nbutton, bbutton;

    private DragAndDropList meas_list;
}
