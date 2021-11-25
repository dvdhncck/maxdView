import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.sql.*;
import javax.swing.event.*;
import java.util.Date;
import javax.swing.table.*;

//
// uses JDBC to retrieve data from a maxdSQL database
//

public class LoadFromVersionOne
{
    public final static String plugin_name = "DatabaseLoader v1.1";


    public LoadFromVersionOne( maxdView mview_ )
    {
	mview = mview_;
    }


    public void cleanUp()
    {
	dbcon.disconnect();
    }


    public boolean getGoing()
    {
	dbcon = mview.getDatabaseConnection();
	
	if((dbcon.attemptConnection() == false) || (!dbcon.isConnected()))
	{
	    System.out.println("connection failed...");
	    return;
	}
	    
	System.out.println("connection made ok...");
    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  gui
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private JPanel createBrowsePanel()
    {
	// -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
	// -- the browse panel  -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
	// -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

	int w = mview.getIntProperty("dbload.width",  450);
	int h = mview.getIntProperty("dbload.height", 350);

	Dimension dim = new Dimension(w, h);

	JPanel browse_panel = new JPanel();
	
	browse_panel.setPreferredSize(dim);
	browse_panel.setMinimumSize(dim);

	browse_panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	GridBagLayout gridbag = new GridBagLayout();
	// browse_panel.setPreferredSize(new Dimension(w, h));
	browse_panel.setLayout(gridbag);

	int line = 0;

	JPanel sel_panel = new JPanel();
	sel_panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 10));
	GridBagLayout sel_gridbag = new GridBagLayout();
	sel_panel.setLayout(sel_gridbag);
	{

	    {
		JLabel label = new JLabel("Browse by  ");
	    
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.EAST;
		sel_gridbag.setConstraints(label, c);

		sel_panel.add(label);
	    }
	    {
		final String[] browse_table_options = 
		{ "Submitter", "Experiment", "ArrayType", "Hybridisation", 
		  "Source", "Sample", "Extract", "Array", "Image" };
		
		browse_jcb = new JComboBox(browse_table_options);
		browse_jcb.setSelectedIndex(-1);
		//jcb.setSelectedIndex( edata.getSetDataType(s) );
		browse_jcb.addActionListener(new ActionListener()
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    selectBrowseTable();
			}
		    });
		browse_jcb.setToolTipText("Which primary database table to browse on");
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		sel_gridbag.setConstraints(browse_jcb, c);

		sel_panel.add(browse_jcb);		
	    }
	    {
		browse_table_label = new JLabel("and show  ");
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.anchor = GridBagConstraints.EAST;
		sel_gridbag.setConstraints(browse_table_label, c);

		sel_panel.add(browse_table_label);
	    }
	    {
		browse_items_jcb = new JComboBox();
		browse_items_jcb.setSelectedIndex(-1);
		browse_items_jcb.setToolTipText("Which key to use from the primary table");
		browse_items_al = new ActionListener()
		    { 
			public void actionPerformed(ActionEvent e) 
			{
			    loadListOfMeasurements();
			}
		    };
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 1;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		sel_gridbag.setConstraints(browse_items_jcb, c);

		sel_panel.add(browse_items_jcb);
	    }

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(sel_panel, c);
	    
	    browse_panel.add(sel_panel);

	}
	
	v_split_pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	
	{
	    h_split_pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	    {   
		browse_list = new JList();
		JScrollPane list_scroll_pane = new JScrollPane(browse_list);
		
		browse_list.addListSelectionListener(new ListSelectionListener()
		    {
			public void valueChanged(ListSelectionEvent e) 
			{
			    updateAttsPanel();
			}
		    });
		
		browse_list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		
		h_split_pane.setLeftComponent(list_scroll_pane);
	    }
	    
	    {
		details_table = new JTable();

		details_table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		details_table.setPreferredSize(new Dimension(200, 200));

		JScrollPane jsp = new JScrollPane(details_table);

		// jsp.setMinimumSize(new Dimension(100, 300));
		
		h_split_pane.setRightComponent(jsp);
	    }
	}

	v_split_pane.setTopComponent(h_split_pane);
	
	{
	    JPanel atts_and_opts = new JPanel();
	    GridBagLayout inner_gridbag = new GridBagLayout();
	    atts_and_opts.setLayout(inner_gridbag);

	    atts_wrapper = new JPanel();
	    
	    JScrollPane jsp = new JScrollPane(atts_wrapper);
	    jsp.setMinimumSize(new Dimension(150, 100));
		
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = 5.0;
	    c.weighty = 9.0;
	    c.fill = GridBagConstraints.BOTH;
	    inner_gridbag.setConstraints(jsp, c);
	    atts_and_opts.add(jsp);
	    
	    opts_wrapper = new JPanel();
	    
	    jsp = new JScrollPane(opts_wrapper);
	    jsp.setMinimumSize(new Dimension(150, 100));
		
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    c.weightx = 5.0;
	    c.weighty = 9.0;
	    c.fill = GridBagConstraints.BOTH;
	    inner_gridbag.setConstraints(jsp, c);
	    atts_and_opts.add(jsp);
	    
	    v_split_pane.setBottomComponent(atts_and_opts);
	    
	}

	h_split_pane.setDividerLocation(mview.getIntProperty("dbload.hsplit", 250));
	v_split_pane.setDividerLocation(mview.getIntProperty("dbload.vsplit", 150));


	//v_split_pane.setDividerLocation(100); 

	{
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    c.weighty = 1.0;
	    c.weightx = 1.0;
	    c.fill   = GridBagConstraints.BOTH;
	    gridbag.setConstraints(v_split_pane, c);
	    
	    browse_panel.add(v_split_pane);
	}

	{
	    JPanel buttons_panel = new JPanel();
	    GridBagLayout inner_gridbag = new GridBagLayout();
	    buttons_panel.setLayout(inner_gridbag);
	    buttons_panel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

	    {
		final JButton jb = new JButton("Merge");
		buttons_panel.add(jb);
		
		jb.setToolTipText("Merge the selection into the current data");

		jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					     {
						 loadSelection(false);
					     }
				     });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		gridbag.setConstraints(jb, c);
	    }
	    {
		final JButton jb = new JButton("Replace");
		buttons_panel.add(jb);

		jb.setToolTipText("Replace the current data with this selection");

		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    if(mview.getExprData().getNumMeasurements() > 0)
			    {
				if(mview.infoQuestion("Really replace existing data?", "Yes", "No") == 1)
				{
				    return;
				}
				mview.getExprData().removeAllMeasurements();
				// mview.getExprData().removeAllClusters();
			    }
			    loadSelection(true);
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		gridbag.setConstraints(jb, c);
	    }

	    {
		
		final JButton jb = new JButton("Options");
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    displayOptionsPanel();
			}
		    });
		buttons_panel.add(jb);
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 3;
		c.gridy = 0;
		gridbag.setConstraints(jb, c);
	    }

	    {
		
		final JButton jb = new JButton("Help");
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    mview.getPluginHelpTopic("DatabaseLoader", "DatabaseLoader");
			}
		    });
		buttons_panel.add(jb);
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 4;
		c.gridy = 0;
		gridbag.setConstraints(jb, c);
	    }
	    
	    {   
		final JButton jb = new JButton("Close");
		buttons_panel.add(jb);
		
		jb.setToolTipText("Close this window");

		jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					     {
						 cleanUp();
					     }
				     });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 5;
		c.gridy = 0;
		gridbag.setConstraints(jb, c);
	    }
	    browse_panel.add(buttons_panel);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    //c.weighty = 0.2;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(buttons_panel, c);
	}
	
	load_name_details[0] = mview.getBooleanProperty("dbload.load_spot_details",  false);
	load_name_details[1] = mview.getBooleanProperty("dbload.load_probe_details", false);
	load_name_details[2] = mview.getBooleanProperty("dbload.load_gene_details",  false);

	optimise_for_small_joins = mview.getBooleanProperty("dbload.optimise_for_small_joins",  true);

    	updateOptsPanel();

	return browse_panel;
    }

    // ---------------------------------------------------------

    private void displayOptionsPanel()
    {
	String[] opts = { "Optimise for fewer queries", "Optimise for smaller joins" };
	int cur_opt_mode = optimise_for_small_joins ? 1 : 0;
	try
	{
	    int optimise_mode = mview.getChoice( "Optimisation", opts, cur_opt_mode );
	    optimise_for_small_joins = (optimise_mode == 1);
	}
	catch(UserInputCancelled uic)
	{
	}
    }

    // ---------------------------------------------------------

    private JCheckBox[] atts_sel_jchkb;

    private void updateOptsPanel()
    {
	opts_wrapper.removeAll();

	int line = 0;

	GridBagLayout gridbag = new GridBagLayout();
	opts_wrapper.setLayout(gridbag);
	
	JLabel label= new JLabel("Spots");
	GridBagConstraints c = new GridBagConstraints();
	c.anchor = GridBagConstraints.CENTER;
	c.gridy = line++;
	gridbag.setConstraints(label, c);
	opts_wrapper.add(label);

	ButtonGroup bg = new ButtonGroup();
	JRadioButton jrb = new JRadioButton("Names only");
	jrb.setSelected(!load_name_details[0]);
	jrb.addActionListener(new OptsListener(0, false));
	c.anchor = GridBagConstraints.WEST;
	c.gridy = line++;
	gridbag.setConstraints(jrb, c);
	opts_wrapper.add(jrb);
	bg.add(jrb);

	jrb = new JRadioButton("All details");
	jrb.setSelected(load_name_details[0]);
	jrb.addActionListener(new OptsListener(0, true));
	c.gridy = line++;
	gridbag.setConstraints(jrb, c);
	opts_wrapper.add(jrb);
	bg.add(jrb);

	label= new JLabel("Probes");
	c.anchor = GridBagConstraints.CENTER;
	c.gridy = line++;
	gridbag.setConstraints(label, c);
	opts_wrapper.add(label);

	bg = new ButtonGroup();
	jrb = new JRadioButton("Names only");
	jrb.setSelected(!load_name_details[1]);
	jrb.addActionListener(new OptsListener(1, false));
	c.anchor = GridBagConstraints.WEST;
	c.gridy = line++;
	gridbag.setConstraints(jrb, c);
	opts_wrapper.add(jrb);
	bg.add(jrb);

	jrb = new JRadioButton("All details");
	jrb.setSelected(load_name_details[1]);
	jrb.addActionListener(new OptsListener(1, true));
	c.gridy = line++;
	gridbag.setConstraints(jrb, c);
	opts_wrapper.add(jrb);
	bg.add(jrb);

	label= new JLabel("Genes");
	c.anchor = GridBagConstraints.CENTER;
	c.gridy = line++;
	gridbag.setConstraints(label, c);
	opts_wrapper.add(label);
	
	bg = new ButtonGroup();
	jrb = new JRadioButton("Names only");
	jrb.setSelected(!load_name_details[2]);
	jrb.addActionListener(new OptsListener(2, false));
	c.anchor = GridBagConstraints.WEST;
	c.gridy = line++;
	gridbag.setConstraints(jrb, c);
	bg.add(jrb);
	opts_wrapper.add(jrb);
	
	jrb = new JRadioButton("All details");
	jrb.setSelected(load_name_details[2]);
	jrb.addActionListener(new OptsListener(2, true));
	c.gridy = line++;
	gridbag.setConstraints(jrb, c);
	opts_wrapper.add(jrb);
	bg.add(jrb);

	opts_wrapper.updateUI();
    }

    private boolean[] load_name_details = new boolean[3];
    private boolean load_name_details_has_changed;

    private class OptsListener implements ActionListener
    {
	int n; 
	boolean m;
	public OptsListener(int n_, boolean m_)
	{
	    n = n_; m = m_;
	}
	public void actionPerformed(ActionEvent e) 
	{
	    load_name_details_has_changed = true;
	    load_name_details[n] = m;
	}
    }

    private void updateAttsPanel()
    {
	// find the atts for the current selection
	//
	int[] sel = browse_list.getSelectedIndices(); // first selected line
	final int n_sel = sel.length;

	atts_wrapper.removeAll();

	GridBagLayout gridbag = new GridBagLayout();
	atts_wrapper.setLayout(gridbag);

	frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

	if(sel.length >= 0)
	{
	    MeasurementDetails[] md_a = new MeasurementDetails[n_sel];

	    Hashtable sa_count = new Hashtable();
	    Hashtable all_meas_details = new Hashtable();

	    for(int s=0; s < n_sel; s++)
	    {
		md_a[s] = getMeasurementDetails(sel[s]);
		
		//System.out.println(ms_a[s].getName() + " has " + ms_a[s].getNumSpotAttributes() + " SAs");

		//System.out.println();

		for(int sa=0; sa < md_a[s].spot_attr_name.size(); sa++)
		{
		    String san = (String) md_a[s].spot_attr_name.elementAt(sa);
		    Integer cnt = (Integer) sa_count.get(san);
		    if(cnt == null)
			cnt = new Integer(0);
		    sa_count.put(san, new Integer(cnt.intValue() + 1));

		    //System.out.println("    " + sa + "..." + san);
		}

		for (Enumeration e = md_a[s].details.keys() ; e.hasMoreElements() ;) 
		{
		    all_meas_details.put( e.nextElement() , "x" );
		}
		
	    }
	    
	    final int n_total_sa  = sa_count.size();

	    JCheckBox jcb = new JCheckBox("Significance");

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.NORTHWEST;
	    gridbag.setConstraints(jcb, c);

	    atts_wrapper.add(jcb);

	    int col = 0;
	    int row = 1;

	    atts_sel_jchkb = new JCheckBox[n_total_sa + 1];
	    atts_sel_jchkb[0] = jcb;

	    //System.out.println();

	    
	    int sa = 1;
	    for (Enumeration e = sa_count.keys() ; e.hasMoreElements() ;) 
	    {
		String san = (String) e.nextElement();
		int san_c = ((Integer) sa_count.get(san)).intValue();

		jcb = new JCheckBox( san );
		
		//System.out.println(san + " x " + san_c);

		atts_sel_jchkb[sa++] = jcb;

		c = new GridBagConstraints();
		c.gridx = col;
		c.gridy = row;
		c.anchor = GridBagConstraints.NORTHWEST;
		gridbag.setConstraints(jcb, c);

		atts_wrapper.add(jcb);
		
		// indicate how many of the Measurements have this SpotAttribute
		//
		if(san_c < sel.length)
		{
		    JLabel label = new JLabel("(" + san_c + ")");
		    c = new GridBagConstraints();
		    c.gridx = col+1;
		    c.gridy = row;
		    c.anchor = GridBagConstraints.NORTHWEST;
		    gridbag.setConstraints(label, c);
		    atts_wrapper.add(label);
		}
		//System.out.println("added: " + sa + " ... " + m.getSpotAttributeName(sa) + " at " + col + "," + row);

		/*
		  // two column layout
		if(++col == 2)
		{
		    col = 0;
		    row++;
		}
		*/
		row++;
	    }
	    
	    //
	    // and update the Measurement  details table
	    //
	    int n_atts = all_meas_details.size();
	    Object[][] atts_data = new Object[n_atts][];
	    
	    final String[] col_names = new String[1 + sel.length];
	    col_names[0] = "Name";
	    for(int m=0; m < sel.length; m++)
		col_names[m+1] = md_a[m].name;


	    int ma = 0;
	    for (Enumeration e = all_meas_details.keys() ; e.hasMoreElements() ;) 
	    {
		atts_data[ma] = new Object[1 + sel.length];
		atts_data[ma][0] = (String) e.nextElement();
		
		for(int s=0; s < sel.length; s++)
		{
		    atts_data[ma][s+1] = md_a[s].details.get((String) atts_data[ma][0]);
		}
		//System.out.println(atts_data[ma][0] + " ... " + atts_data[ma][1]);

		ma++;
	    }
	    

	    details_table.setModel(new DefaultTableModel(atts_data, col_names));
	    
	    details_table.setPreferredSize(new Dimension(200 + (100*col_names.length), 600));

	    TableColumn column = null;
	    
	    int att_w = 100; // details_table.getWidth() / (col_names.length+1);
	    int name_w = 2 * att_w;

	    // System.out.println("dtw =" + details_table.getWidth() + " aw=" +  att_w);

	    for(int cn=0; cn < col_names.length; cn++)
	    {
		column = details_table.getColumnModel().getColumn(cn);
		int width = (cn == 0) ? name_w : att_w;
		column.setWidth(width);
		// column.setPreferredWidth(width);
	    }

	}
	else
	{
	    // reset the details table if there is no selection
	    //
	    details_table.setModel(new DefaultTableModel() );
	}

	// update the SpotAttrs panel

	atts_wrapper.revalidate();
	atts_wrapper.repaint();
	
	v_split_pane.revalidate();

	// update the table layout
	//

	frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

	details_table.revalidate();
    }


    // ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ 
    // ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ 
    // ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ 
   
    public void selectBrowseTable()
    {
	// get the list of all names in the chosen table

	String sql = null;
	
	int t = browse_jcb.getSelectedIndex();
	if(t < 0) 
	    return;
	
	// now get the list of unique keys 
	switch(t)
	{
	case 8:  // Image
	    sql = 
	    "SELECT DISTINCT " + 
	    dbcon.qTableDotField("Image","Name") + 
	    " FROM " +  
	    dbcon.qTable("Measurement") + ", " +
	    dbcon.qTable("Image") +
	    " WHERE " + 
	    dbcon.qTableDotField("Measurement","Image_ID") + " = " +  dbcon.qTableDotField("Image","ID");
	    break;
	    

	case 7:  // Array
	    sql = 
	    "SELECT DISTINCT " + 
	    dbcon.qTableDotField("Array","Name") + 
	    " FROM " +  
	    dbcon.qTable("Measurement") + ", " +
	    dbcon.qTable("Image") + ", " +
	    dbcon.qTable("Hybridisation") + ", " +
	    dbcon.qTable("Array") + 
	    " WHERE " + 
	    dbcon.qTableDotField("Measurement","Image_ID") + " = " +  dbcon.qTableDotField("Image","ID") +
	    " AND " +
	    dbcon.qTableDotField("Image","Hybridisation_ID") + " = " + dbcon.qTableDotField("Hybridisation","ID") +
	    " AND " +
	    dbcon.qTableDotField("Hybridisation", "Array_ID") + " = " + dbcon.qTableDotField("Array","ID");
	    break;

	case 6:  // Extract
	    sql = 
	    "SELECT DISTINCT " + 
	    dbcon.qTableDotField("Extract","Name") + 
	    " FROM " +  
	    dbcon.qTable("Measurement") + ", " +
	    dbcon.qTable("Image") + ", " +
	    dbcon.qTable("Hybridisation") + ", " +
	    dbcon.qTable("Extract") + 
	    " WHERE " + 
	    dbcon.qTableDotField("Measurement","Image_ID") + " = " +  dbcon.qTableDotField("Image","ID") +
	    " AND " +
	    dbcon.qTableDotField("Image","Hybridisation_ID") + " = " + dbcon.qTableDotField("Hybridisation","ID") +
	    " AND " +
	    dbcon.qTableDotField("Hybridisation", "Extract_ID") + " = " + dbcon.qTableDotField("Extract","ID");
	    break;

	case 5:  // Sample 
	    sql = 
	    "SELECT DISTINCT " + 
	    dbcon.qTableDotField("Sample","Name") + 
	    " FROM " +  
	    dbcon.qTable("Measurement") + ", " +
	    dbcon.qTable("Image") + ", " +
	    dbcon.qTable("Hybridisation") + ", " +
	    dbcon.qTable("Extract") + ", " +
	    dbcon.qTable("Sample") + 
	    " WHERE " + 
	    dbcon.qTableDotField("Measurement","Image_ID") + " = " +  dbcon.qTableDotField("Image","ID") +
	    " AND " +
	    dbcon.qTableDotField("Image","Hybridisation_ID") + " = " + dbcon.qTableDotField("Hybridisation","ID") +
	    " AND " +
	    dbcon.qTableDotField("Hybridisation", "Extract_ID") + " = " + dbcon.qTableDotField("Extract","ID") +
	    " AND " +
	    dbcon.qTableDotField("Extract", "Sample_ID") + " = " + dbcon.qTableDotField("Sample","ID");
	    break;

	case 4:  // Source 
	    sql = 
	    "SELECT DISTINCT " + 
	    dbcon.qTableDotField("Source","Name") + 
	    " FROM " +  
	    dbcon.qTable("Measurement") + ", " +
	    dbcon.qTable("Image") + ", " +
	    dbcon.qTable("Hybridisation") + ", " +
	    dbcon.qTable("Extract") + ", " +
	    dbcon.qTable("Sample") + ", " +
	    dbcon.qTable("Source") + 
	    " WHERE " + 
	    dbcon.qTableDotField("Measurement","Image_ID") + " = " +  dbcon.qTableDotField("Image","ID") +
	    " AND " +
	    dbcon.qTableDotField("Image","Hybridisation_ID") + " = " + dbcon.qTableDotField("Hybridisation","ID") +
	    " AND " +
	    dbcon.qTableDotField("Hybridisation", "Extract_ID") + " = " + dbcon.qTableDotField("Extract","ID") +
	    " AND " +
	    dbcon.qTableDotField("Extract", "Sample_ID") + " = " + dbcon.qTableDotField("Sample","ID") +
	    " AND " +
	    dbcon.qTableDotField("Sample", "Source_ID") + " = " + dbcon.qTableDotField("Source","ID");
	    break;


	case 3:  // Hybridisation
	    sql = 
	    "SELECT DISTINCT " + 
	    dbcon.qTableDotField("Hybridisation","Name") + 
	    " FROM " +  
	    dbcon.qTable("Measurement") + ", " +
	    dbcon.qTable("Image") + ", " +
	    dbcon.qTable("Hybridisation") +
	    " WHERE " + 
	    dbcon.qTableDotField("Measurement","Image_ID") + " = " +  dbcon.qTableDotField("Image","ID") +
	    " AND " +
	    dbcon.qTableDotField("Image","Hybridisation_ID") + " = " + dbcon.qTableDotField("Hybridisation","ID");
	    break;

	case 2:  // ArrayType
	    sql = 
	    "SELECT DISTINCT " + 
	    dbcon.qTableDotField("ArrayType","Name") + 
	    " FROM " +  
	    dbcon.qTable("Measurement") + ", " +
	    dbcon.qTable("Image") + ", " +
	    dbcon.qTable("Hybridisation") + ", " +
	    dbcon.qTable("Array") + ", " +
	    dbcon.qTable("ArrayType") + 
	    " WHERE " + 
	    dbcon.qTableDotField("Measurement","Image_ID") + " = " +  dbcon.qTableDotField("Image","ID") +
	    " AND " +
	    dbcon.qTableDotField("Image","Hybridisation_ID") + " = " + dbcon.qTableDotField("Hybridisation","ID") +
	    " AND " +
	    dbcon.qTableDotField("Hybridisation", "Array_ID") + " = " + dbcon.qTableDotField("Array","ID") +
	    " AND " +
	    dbcon.qTableDotField("Array", "Array_Type_ID") + " = " + dbcon.qTableDotField("ArrayType","ID");
	    break;

	case 1:  // Experiment
	    sql = 
	    "SELECT DISTINCT " + 
	    dbcon.qTableDotField("Experiment","Name") + 
	    " FROM " +  
	    dbcon.qTable("Measurement") + ", " +
	    dbcon.qTable("Image") + ", " +
	    dbcon.qTable("Hybridisation") + ", " +
	    dbcon.qTable("Experiment") + 
	    " WHERE " + 
	    dbcon.qTableDotField("Measurement","Image_ID") + " = " +  dbcon.qTableDotField("Image","ID") +
	    " AND " +
	    dbcon.qTableDotField("Image","Hybridisation_ID") + " = " + dbcon.qTableDotField("Hybridisation","ID") +
	    " AND " +
	    dbcon.qTableDotField("Hybridisation", "Experiment_ID") + " = " + dbcon.qTableDotField("Experiment","ID");
	    break;
	
	case 0:  // submitter
	    sql = 
	    "SELECT DISTINCT " + 
	    dbcon.qTableDotField("Submitter","Name") + 
	    " FROM " +  
	    dbcon.qTable("Measurement") + ", " +
	    dbcon.qTable("Image") + ", " +
	    dbcon.qTable("Hybridisation") + ", " +
	    dbcon.qTable("Experiment") + ", " +
	    dbcon.qTable("Submitter") + 
	    " WHERE " + 
	    dbcon.qTableDotField("Measurement","Image_ID") + " = " +  dbcon.qTableDotField("Image","ID") +
	    " AND " +
	    dbcon.qTableDotField("Image","Hybridisation_ID") + " = " + dbcon.qTableDotField("Hybridisation","ID") +
	    " AND " +
	    dbcon.qTableDotField("Hybridisation", "Experiment_ID") + " = " + dbcon.qTableDotField("Experiment","ID") +
	    " AND " +
	    dbcon.qTableDotField("Experiment", "Submitter_ID") + " = " + dbcon.qTableDotField("Submitter","ID");
	    break;
		    

	}
	
	
	browse_items_jcb.removeActionListener(browse_items_al);
	browse_items_jcb.removeAllItems();
	
	boolean first = true;

	ResultSet rs = dbcon.executeQuery(sql);
	    
	try
	{
	    
	    while (rs.next()) 
	    {
		browse_items_jcb.addItem(new String(rs.getString(1)));

		if(first)
		{
		    first = false;
		    browse_items_jcb.setSelectedIndex(0);
		    browse_target_name = rs.getString(1);
		}
	    }
	}
	catch(SQLException sqle)
	{
	    mview.alertMessage("Unable to execute SQL:\n" + sqle);
	}
	finally
	{
	    dbcon.closeResultSet( rs );
	}

	browse_items_jcb.addActionListener(browse_items_al);
	//System.out.println("browse target is "  + browse_target_name);

	loadListOfMeasurements();
    }

    // attempt to retrieve a list of experiments
    // using the current primary table and table_key
    //
    public void loadListOfMeasurements()
    {
	int n_rows = 0;

	//table_model = new ExptDataTableModel();
	list_model = new DefaultListModel();
	
	browse_target_name = (String)browse_items_jcb.getSelectedItem();

	//System.out.println("browse target is "  + browse_target_name);

	expt_data.removeAllElements();

	measure_id_strs = new Vector();

	{

	    // get the measurements which match the browse string...
	    String sql = null;

	    switch(browse_jcb.getSelectedIndex())
	    {
	    case 8:  // Image
		sql = 
		"SELECT " + 
		dbcon.qTableDotField("Measurement","ID") + ", " + 
		dbcon.qTableDotField("Measurement","Name") + 
		" FROM " +  
		dbcon.qTable("Measurement") + ", " +
		dbcon.qTable("Image") + 
		" WHERE " + 
		dbcon.qTableDotField("Measurement","Image_ID") + " = " +  dbcon.qTableDotField("Image","ID") +
		" AND " +
		dbcon.qTableDotField("Image","Name") + " = " + dbcon.qText(browse_target_name);
		break;

	    case 7:  // Array
		sql = 
		"SELECT " + 
		dbcon.qTableDotField("Measurement","ID") + ", " + 
		dbcon.qTableDotField("Measurement","Name") + 
		" FROM " +  
		dbcon.qTable("Measurement") + ", " +
		dbcon.qTable("Image") + ", " +
		dbcon.qTable("Hybridisation") + ", " +
	        dbcon.qTable("Array") +
		" WHERE " + 
		dbcon.qTableDotField("Measurement","Image_ID") + " = " +  dbcon.qTableDotField("Image","ID") +
		" AND " +
		dbcon.qTableDotField("Image","Hybridisation_ID") + " = " + dbcon.qTableDotField("Hybridisation","ID") + 
		" AND " + 
		dbcon.qTableDotField("Hybridisation","Array_ID") + " = " + dbcon.qTableDotField("Array","ID") + 
		" AND " +
		dbcon.qTableDotField("Array","Name") + " = " + dbcon.qText(browse_target_name);
		break;

	    case 6:  // Extract
		sql = 
		"SELECT " + 
		dbcon.qTableDotField("Measurement","ID") + ", " + 
		dbcon.qTableDotField("Measurement","Name") + 
		" FROM " +  
		dbcon.qTable("Measurement") + ", " +
		dbcon.qTable("Image") + ", " +
		dbcon.qTable("Hybridisation") + ", " +
	        dbcon.qTable("Extract") + " " +
		" WHERE " + 
		dbcon.qTableDotField("Measurement","Image_ID") + " = " +  dbcon.qTableDotField("Image","ID") +
		" AND " +
		dbcon.qTableDotField("Image","Hybridisation_ID") + " = " + dbcon.qTableDotField("Hybridisation","ID") + 
		" AND " + 
		dbcon.qTableDotField("Hybridisation","Extract_ID") + " = " + dbcon.qTableDotField("Extract","ID") + 
		" AND " +
		dbcon.qTableDotField("Extract","Name") + " = " + dbcon.qText(browse_target_name);
		break;

	    case 5:  // Sample
		sql = 
		"SELECT " + 
		dbcon.qTableDotField("Measurement","ID") + ", " + 
		dbcon.qTableDotField("Measurement","Name") + 
		" FROM " +  
		dbcon.qTable("Measurement") + ", " +
		dbcon.qTable("Image") + ", " +
		dbcon.qTable("Hybridisation") + ", " +
	        dbcon.qTable("Extract") + ", " +
	        dbcon.qTable("Sample") + " " +
		" WHERE " + 
		dbcon.qTableDotField("Measurement","Image_ID") + " = " +  dbcon.qTableDotField("Image","ID") +
		" AND " +
		dbcon.qTableDotField("Image","Hybridisation_ID") + " = " + dbcon.qTableDotField("Hybridisation","ID") + 
		" AND " + 
		dbcon.qTableDotField("Hybridisation","Extract_ID") + " = " + dbcon.qTableDotField("Extract","ID") + 
		" AND " +
		dbcon.qTableDotField("Extract","Sample_ID") + " = " + dbcon.qTableDotField("Sample","ID") + 
		" AND " +
		dbcon.qTableDotField("Sample","Name") + " = " + dbcon.qText(browse_target_name);
		break;


	    case 4:  // Source
		sql = 
		"SELECT " + 
		dbcon.qTableDotField("Measurement","ID") + ", " + 
		dbcon.qTableDotField("Measurement","Name") + 
		" FROM " +  
		dbcon.qTable("Measurement") + ", " +
		dbcon.qTable("Image") + ", " +
		dbcon.qTable("Hybridisation") + ", " +
	        dbcon.qTable("Extract") + ", " +
	        dbcon.qTable("Sample") + ", " +
	        dbcon.qTable("Source") + " " +
		" WHERE " + 
		dbcon.qTableDotField("Measurement","Image_ID") + " = " +  dbcon.qTableDotField("Image","ID") +
		" AND " +
		dbcon.qTableDotField("Image","Hybridisation_ID") + " = " + dbcon.qTableDotField("Hybridisation","ID") + 
		" AND " + 
		dbcon.qTableDotField("Hybridisation","Extract_ID") + " = " + dbcon.qTableDotField("Extract","ID") + 
		" AND " +
		dbcon.qTableDotField("Extract","Sample_ID") + " = " + dbcon.qTableDotField("Sample","ID") + 
		" AND " +
		dbcon.qTableDotField("Sample","Source_ID") + " = " + dbcon.qTableDotField("Source","ID") + 
		" AND " +
		dbcon.qTableDotField("Source","Name") + " = " + dbcon.qText(browse_target_name);
		break;


	    case 3:  // Hybridisation
		sql = 
		"SELECT " + 
		dbcon.qTableDotField("Measurement","ID") + ", " + 
		dbcon.qTableDotField("Measurement","Name") + 
		" FROM " +  
		dbcon.qTable("Measurement") + ", " +
		dbcon.qTable("Image") + ", " +
		dbcon.qTable("Hybridisation") + 
		" WHERE " + 
		dbcon.qTableDotField("Measurement","Image_ID") + " = " +  dbcon.qTableDotField("Image","ID") +
		" AND " +
		dbcon.qTableDotField("Image","Hybridisation_ID") + " = " + dbcon.qTableDotField("Hybridisation","ID") + 
		" AND " + 
		dbcon.qTableDotField("Hybridisation","Name") + " = " + dbcon.qText(browse_target_name);
		break;



	    case 2:  // ArrayType
		sql = 
		"SELECT " + 
		dbcon.qTableDotField("Measurement","ID") + ", " + 
		dbcon.qTableDotField("Measurement","Name") + 
		" FROM " +  
		dbcon.qTable("Measurement") + ", " +
		dbcon.qTable("Image") + ", " +
		dbcon.qTable("Hybridisation") + ", " +
	        dbcon.qTable("Array") + ", " +
	        dbcon.qTable("ArrayType") + 
		" WHERE " + 
		dbcon.qTableDotField("Measurement","Image_ID") + " = " +  dbcon.qTableDotField("Image","ID") +
		" AND " +
		dbcon.qTableDotField("Image","Hybridisation_ID") + " = " + dbcon.qTableDotField("Hybridisation","ID") + 
		" AND " + 
		dbcon.qTableDotField("Hybridisation","Array_ID") + " = " + dbcon.qTableDotField("Array","ID") + 
		" AND " +
		dbcon.qTableDotField("Array","Array_Type_ID") + " = " + dbcon.qTableDotField("ArrayType","ID") + 
		" AND " +
		dbcon.qTableDotField("ArrayType","Name") + " = " + dbcon.qText(browse_target_name);
		break;

	    case 1:  // Experiment
		sql = 
		"SELECT " + 
		dbcon.qTableDotField("Measurement","ID") + ", " + 
		dbcon.qTableDotField("Measurement","Name") + 
		" FROM " +  
		dbcon.qTable("Measurement") + ", " +
		dbcon.qTable("Image") + ", " +
		dbcon.qTable("Hybridisation") + ", " +
	        dbcon.qTable("Experiment") + 
		" WHERE " + 
		dbcon.qTableDotField("Measurement","Image_ID") + " = " +  dbcon.qTableDotField("Image","ID") +
		" AND " +
		dbcon.qTableDotField("Image","Hybridisation_ID") + " = " + dbcon.qTableDotField("Hybridisation","ID") + 
		" AND " + 
		dbcon.qTableDotField("Hybridisation","Experiment_ID") + " = " + dbcon.qTableDotField("Experiment","ID") + 
		" AND " +
		dbcon.qTableDotField("Experiment","Name") + " = " + dbcon.qText(browse_target_name);
		break;

	    case 0:  // submitter
		sql = 
		"SELECT " + 
		dbcon.qTableDotField("Measurement","ID") + ", " + 
		dbcon.qTableDotField("Measurement","Name") + 
		" FROM " +  
		dbcon.qTable("Measurement") + ", " +
		dbcon.qTable("Image") + ", " +
		dbcon.qTable("Hybridisation") + ", " +
	        dbcon.qTable("Experiment") + ", " +
	        dbcon.qTable("Submitter") + 
		" WHERE " + 
		dbcon.qTableDotField("Measurement","Image_ID") + " = " +  dbcon.qTableDotField("Image","ID") +
		" AND " +
		dbcon.qTableDotField("Image","Hybridisation_ID") + " = " + dbcon.qTableDotField("Hybridisation","ID") + 
		" AND " + 
		dbcon.qTableDotField("Hybridisation","Experiment_ID") + " = " + dbcon.qTableDotField("Experiment","ID") + 
		" AND " +
		dbcon.qTableDotField("Experiment","Submitter_ID") + " = " + dbcon.qTableDotField("Submitter","ID") + 
		" AND " +
		dbcon.qTableDotField("Submitter","Name") + " = " + dbcon.qText(browse_target_name);
		break;

	    }
	    
	   ResultSet rs = dbcon.executeQuery(sql);
		
	   if(rs != null)
	   {
	       try
	       {
		   
		   while (rs.next()) 
		   {
		       measure_id_strs.addElement(rs.getString(1));
		       
		       list_model.addElement(rs.getString(2));
		       
		       n_rows++;
		   }
	       }
	       catch(SQLException sqle)
	       {
		   mview.alertMessage("Unable to execute SQL: " + sqle);
	       }
	       finally
	       {
		   dbcon.closeResultSet( rs );
	       }
	   }

	}
	
	//table.setModel(table_model);
	browse_list.setModel(list_model);

	//items_label.setText(n_rows + ((n_rows > 1) ? " rows" : " row") + " retrieved");
	//table_model.fireTableChanged(new TableModelEvent(table_model));

	updateAttsPanel();
    }


    public void showMeasurementDetailsOfSelection() 
    {
	int sel = browse_list.getSelectedIndex();
	MeasurementDetails md = getMeasurementDetails(sel);

	JFrame d_frame = new JFrame("Details: " + md.name);
	mview.decorateFrame(d_frame);
	JPanel d_panel = new JPanel();
	d_frame.getContentPane().add(d_panel);

	int n_atts = md.details.size();
	Object[][] t_data = new Object[n_atts][];
	int ma = 0;
	for (Enumeration e = md.details.keys() ; e.hasMoreElements() ;) 
	{
	    t_data[ma] = new Object[2];
	    t_data[ma][0] = (String) e.nextElement();
	    t_data[ma][1] = md.details.get((String) t_data[ma][0]);
	    ma++;
	}

	final String[] col_names = { "Name", "Value" };

	final JTable table = new JTable(t_data, col_names);
	//table.setEditable(false);
	//table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

	JScrollPane jsp = new JScrollPane(table);
	jsp.setMinimumSize(new Dimension(150, 100));
	d_panel.add(jsp);
	d_frame.pack();
	d_frame.setVisible(true);

    }

    // should really be saved somewhere...
    //
    /*
    private Vector spot_attr_type_id   = null;
    private Vector spot_attr_type_name = null;
    private Vector spot_attr_unit_name = null;
    private Vector spot_attr_type_type = null;
    private Vector spot_attr_name      = null;
    */

    private class MeasurementDetails
    {
	String name;
	String id;

	String image_id;
	String hyb_id;
	String image_anal_prot_id;

	Hashtable details = new Hashtable();

	private Vector spot_attr_name      = null;

	private Vector spot_attr_type_id   = null;
	private Vector spot_attr_type_name = null;
	private Vector spot_attr_unit_name = null;
	private Vector spot_attr_type_type = null;
    }

    public MeasurementDetails getMeasurementDetails(int sel) 
    {
	if(sel < 0)
	    return null;

	// we know the Measurement_ID of the target,
	//  ... find lots of associated stuff based on this key
	
	MeasurementDetails md = new MeasurementDetails();

	md.name = (String) list_model.elementAt(sel);
	md.id   = (String) measure_id_strs.elementAt(sel);
	
	
	String sql = null;

	try
	{
	    // first get the names of the scanning and image analysis protocols
	    
	    StringBuffer sbuf =  new StringBuffer();

	    sbuf.append("SELECT ");
	    sbuf.append(dbcon.qTableDotField("Image","ID") + ", ");
	    sbuf.append(dbcon.qTableDotField("Image","Name") + ", ");
	    sbuf.append(dbcon.qTableDotField("ScanningProtocol","Name") + ", ");
	    sbuf.append(dbcon.qTableDotField("ImageAnalysisProtocol","Name") + ", ");
	    sbuf.append(dbcon.qTableDotField("ImageAnalysisProtocol","ID"));
	    sbuf.append(" FROM ");
	    sbuf.append(dbcon.qTable("Measurement") + ", ");
	    sbuf.append(dbcon.qTable("Image") + ", ");
	    sbuf.append(dbcon.qTable("ScanningProtocol") + ", ");
	    sbuf.append(dbcon.qTable("ImageAnalysisProtocol"));
	    sbuf.append(" WHERE ");
	    sbuf.append(dbcon.qTableDotField("Measurement","ID") + " = " +  dbcon.qID(md.id));
	    sbuf.append(" AND ");
	    sbuf.append(dbcon.qTableDotField("Measurement","Image_ID") + " = " + dbcon.qTableDotField("Image","ID"));
	    sbuf.append(" AND ");
	    sbuf.append(dbcon.qTableDotField("Measurement","Image_Analysis_Protocol_ID") + " = ");
	    sbuf.append(dbcon.qTableDotField("ImageAnalysisProtocol","ID"));
	    sbuf.append(" AND ");
	    sbuf.append(dbcon.qTableDotField("Image","Scanning_Protocol_ID") + " = " + dbcon.qTableDotField("ScanningProtocol","ID"));

	    sql = sbuf.toString();

	    ResultSet rs = dbcon.executeQuery(sql);
	    if(rs != null)
	    {
		if(rs.next())
		{
		    md.image_id = new String(rs.getString(1));

		    md.details.put("Image ID",  md.image_id);
		    md.details.put("Image name",  rs.getString(2));
		    md.details.put("Scanning Protocol name", rs.getString(3));
		    md.details.put("Image Analysis Protocol name", rs.getString(4));

		    md.image_anal_prot_id = new String(rs.getString(5));
		}
		dbcon. closeResultSet(rs);
	    }
	    
	    // ---------------------------
	    // establish which SpotAttributes are present due to the
	    // chosen ImageAnalysisProtocol
	    //
	    sql = 
	    "SELECT " + 
	    dbcon.qField("Type_ID") + ", " + 
	    dbcon.qField("Name") + ", " + 
	    dbcon.qField("Unit") + ", " + 
	    dbcon.qField("Data_Type") + 
	    " FROM " +  
	    dbcon.qTable("ImageAnalysisProtocolSpotAttr") + ", " +
	    dbcon.qTable("Type") + 
	    " WHERE " + 
	    dbcon.qField("Image_Analysis_Protocol_ID") + " = " + dbcon.qID(md.image_anal_prot_id) + " AND " +
	    dbcon.qTableDotField("Type","ID") + " = " +  dbcon.qField("Type_ID");

	    md.spot_attr_type_id    = new Vector();
	    md.spot_attr_type_name  = new Vector();
	    md.spot_attr_unit_name  = new Vector();
	    md.spot_attr_type_type  = new Vector();
	    md.spot_attr_name       = new Vector();

	    rs = dbcon.executeQuery(sql);
	    if(rs != null)
	    {
		while(rs.next())
		{
		    // keep these seperately in case the Measurement
		    // does something odd to the ordering
		    //
		    md.spot_attr_type_id.addElement(rs.getString(1));
		    md.spot_attr_name.addElement(rs.getString(2));
		    
		    String unit_n = rs.getString(3);
		    String type_n = rs.getString(4);

		    //System.out.println("spot attr: name=" + rs.getString(2) + " unit=" + unit_n + " type=" + type_n);

		    if(type_n != null)
			type_n = type_n.toUpperCase();

		    int type_i = -1;

		    if(type_n.equals("INTEGER"))
			type_i = 0;
		    if(type_n.equals("DOUBLE"))
			type_i = 1;
		    if(type_n.equals("CHAR"))
			type_i = 2;
		    if(type_n.equals("TEXT"))
			type_i = 3;

		    if(type_n == null)
			type_n = "(untyped)";

		    if(type_i >= 0)
		    {
			//System.out.println(" attr added as type " + type_n);

			md.spot_attr_type_name.addElement(type_n);
			md.spot_attr_unit_name.addElement(unit_n);
			md.spot_attr_type_type.addElement(new Integer(type_i));
			
			// md.addSpotAttribute(rs.getString(2), unit_n, type_n, null);
		    }
		}
		dbcon. closeResultSet(rs);
	    }
	    
	    // ---------------------------
	    
	    // now get the names of the hybridisation, hybridisation protocol and experiment names
	    
	    sql = 
	    "SELECT " + 
	    dbcon.qTableDotField("Hybridisation","ID") + ", " + 
	    dbcon.qTableDotField("Hybridisation","Name") + ", " + 
	    dbcon.qTableDotField("HybridisationProtocol","Name") + ", " + 
	    dbcon.qTableDotField("Experiment","Name") + 
	    " FROM " +  
	    dbcon.qTable("Hybridisation") + ", " +
	    dbcon.qTable("Image") + ", " +
	    dbcon.qTable("HybridisationProtocol") + ", " +
	    dbcon.qTable("Experiment") + 
	    " WHERE " + 
	    dbcon.qTableDotField("Image","ID") + " = " +  dbcon.qID(md.image_id) + 
	     " AND " +
	    dbcon.qTableDotField("Image","Hybridisation_ID") + " = " + dbcon.qTableDotField("Hybridisation","ID") + 
	    " AND " + 
	    dbcon.qTableDotField("Hybridisation","Hybridisation_Protocol_ID") + " = " + dbcon.qTableDotField("HybridisationProtocol","ID") + 
	    " AND " +
	    dbcon.qTableDotField("Hybridisation","Experiment_ID") + " = " + dbcon.qTableDotField("Experiment","ID");
	    
	    rs = dbcon.executeQuery(sql);
	    if(rs != null)
	    {
		if(rs.next())
		{
		    md.hyb_id = new String(rs.getString(1));
		    
		    md.details.put("Hybridisation name", rs.getString(2));
		    md.details.put("Hybridisation Protocol name", rs.getString(3));
		    md.details.put("Experiment name",  rs.getString(4));
		}
		dbcon. closeResultSet(rs);
	    }

	    // ---------------------------

	    // now get the names of the array and array type

	    sql = 
	    "SELECT " + 
	    dbcon.qTableDotField("Array","Name") + ", " + 
	    dbcon.qTableDotField("ArrayType","Name") + ", " + 
	    dbcon.qTableDotField("ArrayType","Number_Spots") + 
	    " FROM " +  
	    dbcon.qTable("Hybridisation") + ", " +
	    dbcon.qTable("Array") + ", " +
	    dbcon.qTable("ArrayType") + 
	    " WHERE " + 
	    dbcon.qTableDotField("Hybridisation","ID") + " = " +  dbcon.qID(md.hyb_id) + 
	     " AND " +
	    dbcon.qTableDotField("Hybridisation","Array_ID") + " = " + dbcon.qTableDotField("Array","ID") + 
	    " AND " + 
	    dbcon.qTableDotField("Array","Array_Type_ID") + " = " + dbcon.qTableDotField("ArrayType","ID");
	    
	    rs = dbcon.executeQuery(sql);

	    if(rs != null)
	    {
		while(rs.next())
		{
		    md.details.put("Array Name", rs.getString(1));
		    md.details.put("ArrayType name", rs.getString(2));
		    md.details.put("Number of Spots", String.valueOf(rs.getInt(3)));
		}
		dbcon. closeResultSet(rs);
	    }
	    
	    // ---------------------------
	    
	    /*
	    
	    sbuf = new StringBuffer();
	    sbuf.append("SELECT ");
	    sbuf.append(dbcon.qTableDotField("Extract","Name") + ", ");
	    sbuf.append(dbcon.qTableDotField("Sample","Name") + ", ");
	    sbuf.append(dbcon.qTableDotField("Source","Name"));
	    sbuf.append(" FROM ");
	    sbuf.append(dbcon.qTable("Hybridisation") + ", ");
	    sbuf.append(dbcon.qTable("Extract") + ", ");
	    sbuf.append(dbcon.qTable("Sample") + ", ");
	    sbuf.append(dbcon.qTable("Source"));
	    sbuf.append(" WHERE ");
	    sbuf.append(dbcon.qTableDotField("Hybridisation","ID") + " = " + dbcon.qID(hyb_id));
	    sbuf.append(" AND ");
	    sbuf.append(dbcon.qTableDotField("Hybridisation","Extract_ID") + " = " + dbcon.qTableDotField("Extract","ID"));
	    sbuf.append(" AND ");
	    sbuf.append(dbcon.qTableDotField("Extract","Sample_ID") + " = " + dbcon.qTableDotField("Sample","ID"));
	    sbuf.append(" AND ");
	    sbuf.append(dbcon.qTableDotField("Sample","Source_ID") + " = " + dbcon.qTableDotField("Source","ID"));

	    sql = sbuf.toString();

	    rs = dbcon.executeQuery(sql);
	    if(rs != null)
	    {
		while(rs.next())
		{
		    m.setAttribute("Extract name", rs.getString(1));
		    m.setAttribute("Sample name", rs.getString(2));
		    m.setAttribute("Source name", rs.getString(3));
		}
		dbcon.closeResultSet( rs );
	    }
	    */

	    // ---------------------------
	    
	    // now get the names of the extract, sample and source and such
	    
	    String ext_id      = getFieldFromTable("Hybridisation", md.hyb_id, "Extract_ID");
	    String ext_n       = getFieldFromTable("Extract", ext_id, "Name");
	    md.details.put("Extract name", ext_n);

	    String ext_prot_id = getFieldFromTable("Extract", ext_id, "Extraction_Protocol_ID");
	    String ext_prot_n  = getFieldFromTable("ExtractionProtocol", ext_prot_id, "Name");
	    md.details.put("Extraction Protocol name", ext_prot_n);
	    
	    String sam_id      = getFieldFromTable("Extract", ext_id, "Sample_ID");
	    String sam_n       = getFieldFromTable("Sample",  sam_id, "Name");
	    md.details.put("Sample name", sam_n);

	    String sam_trt_id  = getFieldFromTable("Sample",  sam_id, "Sample_Treatment_ID");
	    String sam_trt_n   = getFieldFromTable("SampleTreatment",  sam_trt_id, "Name");
	    md.details.put("Sample Treatment name", sam_trt_n);
	    
	    String src_id      = getFieldFromTable("Sample", sam_id, "Source_ID");
	    String src_n       = getFieldFromTable("Source", src_id, "Name");
	    md.details.put("Source name", src_n);

	     // ---------------------------
	
	    // now get any ImageAttributes properties associated with the ImageAnalysisProtocol
	    //
	    sbuf = new StringBuffer();
	    sbuf.append("SELECT ");
	    sbuf.append(dbcon.qField("Image_Attribute_Description_ID"));
	    sbuf.append(" FROM ");
	    sbuf.append(dbcon.qTable("Measurement"));
	    sbuf.append(" WHERE ");
	    sbuf.append(dbcon.qField("ID") + " = " + dbcon.qID(md.id));

	    sql = sbuf.toString();
	    rs = dbcon.executeQuery(sql);

	    String i_a_desc_id = null;
	    if(rs != null)
	    {
		if(rs.next())
		{
		    i_a_desc_id =  rs.getString(1);
		}
		dbcon.closeResultSet(rs);
	    }

	    if(i_a_desc_id != null)
	    {
		// System.out.println("desc id is " + i_a_desc_id);
		
		sbuf = new StringBuffer();
		sbuf.append("SELECT ");
		sbuf.append(dbcon.qField("Name") + ", ");
		sbuf.append(dbcon.qField("Value"));
		sbuf.append(" FROM ");
		sbuf.append(dbcon.qTable("Property") + ", ");
		sbuf.append(dbcon.qTable("Type"));
		sbuf.append(" WHERE ");
		sbuf.append(dbcon.qTableDotField("Property", "Type_ID") + " = " + dbcon.qTableDotField("Type","ID"));
		sbuf.append(" AND ");
		sbuf.append(dbcon.qField("Description_ID") + " = " + dbcon.qDescID(i_a_desc_id));
		
		sql = sbuf.toString();
		rs = dbcon.executeQuery(sql);

		if(rs != null)
		{
		    while(rs.next())
		    {
			md.details.put(rs.getString(1), rs.getString(2));
		    }
		    dbcon.closeResultSet( rs );
		}

		sbuf = new StringBuffer();
		sbuf.append("SELECT ");
		sbuf.append(dbcon.qField("Index") + ", ");
		sbuf.append(dbcon.qField("Text"));
		sbuf.append(" FROM ");
		sbuf.append(dbcon.qTable("TextProperty"));
		sbuf.append(" WHERE ");
		sbuf.append(dbcon.qField("Description_ID") + " = " + dbcon.qDescID(i_a_desc_id));
		
		sql = sbuf.toString();
		rs = dbcon.executeQuery(sql);

		if(rs != null)
		{
		    while(rs.next())
		    {
			md.details.put("Text (" +  rs.getString(1) + ")", rs.getString(2));
		    }
		    dbcon. closeResultSet(rs);
		}
		
		sbuf = new StringBuffer();
		sbuf.append("SELECT ");
		sbuf.append(dbcon.qField("Name") + ", ");
		sbuf.append(dbcon.qField("Value"));
		sbuf.append(" FROM ");
		sbuf.append(dbcon.qTable("CharProperty") + ", ");
		sbuf.append(dbcon.qTable("Type"));
		sbuf.append(" WHERE ");
		sbuf.append(dbcon.qTableDotField("CharProperty","Type_ID") + " = ");
		sbuf.append(dbcon.qTableDotField("Type", "ID"));
		sbuf.append(" AND ");
		sbuf.append(dbcon.qField("Description_ID") + " = " + dbcon.qDescID(i_a_desc_id));
		
		sql = sbuf.toString();
		
		rs = dbcon.executeQuery(sql);
		if(rs != null)
		{
		    while(rs.next())
		    {
			md.details.put(rs.getString(1), rs.getString(2));
		    }
		    dbcon. closeResultSet(rs);
		}

		sbuf = new StringBuffer();
		sbuf.append("SELECT ");
		sbuf.append(dbcon.qField("Name") + ", ");
		sbuf.append(dbcon.qField("Value"));
		sbuf.append(" FROM ");
		sbuf.append(dbcon.qTable("NumericProperty") + ", ");
		sbuf.append(dbcon.qTable("Type"));
		sbuf.append(" WHERE ");
		sbuf.append(dbcon.qTableDotField("NumericProperty","Type_ID") + " = ");
		sbuf.append(dbcon.qTableDotField("Type", "ID"));
		sbuf.append(" AND ");
		sbuf.append(dbcon.qField("Description_ID") + " = " + dbcon.qDescID(i_a_desc_id));
		
		sql = sbuf.toString();
		rs = dbcon.executeQuery(sql);

		if(rs != null)
		{
		    while(rs.next())
		    {
			md.details.put(rs.getString(1), rs.getString(2));
		    }
		    dbcon. closeResultSet(rs);
		}

		sbuf = new StringBuffer();
		sbuf.append("SELECT ");
		sbuf.append(dbcon.qField("Name") + ", ");
		sbuf.append(dbcon.qField("Value"));
		sbuf.append(" FROM ");
		sbuf.append(dbcon.qTable("IntegerProperty") + ", ");
		sbuf.append(dbcon.qTable("Type"));
		sbuf.append(" WHERE ");
		sbuf.append(dbcon.qTableDotField("IntegerProperty","Type_ID") + " = ");
		sbuf.append(dbcon.qTableDotField("Type", "ID"));
		sbuf.append(" AND ");
		sbuf.append(dbcon.qField("Description_ID") + " = " + dbcon.qDescID(i_a_desc_id));
		
		sql = sbuf.toString();
		rs = dbcon.executeQuery(sql);

		if(rs != null)
		{
		    while(rs.next())
		    {
			md.details.put(rs.getString(1), rs.getString(2));
		    }
		    dbcon. closeResultSet(rs);
		}
	    }
	    
	}
	catch (SQLException sqle)
	{
	    mview.alertMessage("Unable to execute SQL: " + sqle);
	}
	
	return md;
    }

    public class MeasurementLoaderThread extends Thread
    {
	boolean replace;

	public MeasurementLoaderThread(boolean repl)
	{
	    replace = repl;
	}

	// load _all_ SpotAttributes associated with this Measurement_ID
	//
	private boolean loadSpotAttributes(String meas_id, MeasurementDetails md, 
					   ExprData.Measurement target, int n_spots,
					   Vector message_v)
	{
	    final boolean debug_spot_attrs = true;

	    final String[] table_names = { "IntegerProperty", "NumericProperty", "CharProperty", "Property" };
	    
	    
	    Hashtable selected_spot_attrs = new Hashtable();
	    for(int sao=1; sao < atts_sel_jchkb.length; sao++)
	    {
		if(atts_sel_jchkb[sao].isSelected())
		    selected_spot_attrs.put(atts_sel_jchkb[sao].getText(), "x");
	    }
	    
	    for(int a=0; a < md.spot_attr_type_id.size(); a++)
	    {
		int missing = 0;
		
		try
		{
		    if(selected_spot_attrs.get(md.spot_attr_name.elementAt(a)) != null)
		    {
			pm.setMessage(2, "Querying SpotAttributes: " + (String)md.spot_attr_name.elementAt(a));
			
			if(debug_spot_attrs)
			{
			    System.out.println("processing SpotAttr (number " + a + ") " +
					       (String)md.spot_attr_name.elementAt(a) + ", type = " + 
					       (String)md.spot_attr_type_name.elementAt(a));
			}
			
			String  tname = (String)  md.spot_attr_type_name.elementAt(a);
			String  uname = (String)  md.spot_attr_unit_name.elementAt(a);
			Integer ttype = (Integer) md.spot_attr_type_type.elementAt(a);
			String  tid   = (String)  md.spot_attr_type_id.elementAt(a);
			
			StringBuffer sql = new StringBuffer("SELECT ");
			
			sql.append(dbcon.qField("Spot_ID") + ", " + dbcon.qField("Value"));
			sql.append(" FROM ");
			sql.append(dbcon.qTable("SpotMeasurement") + ", ");
			
			sql.append(dbcon.qTable(table_names[ttype.intValue()]));
			
			sql.append(" WHERE ");
			sql.append(dbcon.qField("Measurement_ID") + " = ");
			sql.append(dbcon.qID(meas_id));
			sql.append(" AND ");
			sql.append(dbcon.qField("Output_Description_ID") + " = ");
			sql.append(dbcon.qField("Description_ID"));
			sql.append(" AND Type_ID = " + tid);
		    
			ResultSet rs = dbcon.executeQuery(sql.toString());
			
			pm.setMessage(2, "Receiving SpotAttributes: " + (String)md.spot_attr_name.elementAt(a));
		    
			if(rs != null)
			{
			    if(tname.equals("INTEGER"))
			    {
				int[] data = new int[n_spots];
				while(rs.next())
				{
				    Integer si = (Integer) spot_id_to_index_ht.get((String) rs.getString(1));
				    int sii = si.intValue();
				    
				    if(rs.getString(2) != null)
					data[sii] = rs.getInt(2);
				    else
				    {
					data[sii] = 0;
				    
					missing++;
				    }
				}
				target.addSpotAttribute((String)md.spot_attr_name.elementAt(a), uname, "INTEGER", (Object) data);
			    }
			    if(tname.equals("DOUBLE"))
			    {
				double[] data = new double[n_spots];
				
				while(rs.next())
				{
				    Integer si = (Integer) spot_id_to_index_ht.get((String) rs.getString(1));
				    int sii = si.intValue();
				    
				    if(rs.getString(2) != null)
					data[sii] =  rs.getDouble(2);
				    else
				    {
					data[sii] =  Double.NaN;
					missing++;
				    }
				}
				target.addSpotAttribute((String)md.spot_attr_name.elementAt(a), uname, "DOUBLE", (Object) data);
			    }
			    if(tname.equals("CHAR"))
			    {
				char[] data = new char[n_spots];
				while(rs.next())
				{
				    Integer si = (Integer) spot_id_to_index_ht.get((String) rs.getString(1));
				    int sii = si.intValue();

				    if((rs.getString(2) != null) && (rs.getString(2).length() > 0))
				    {
					data[sii] = rs.getString(2).charAt(0);
				    }					    
				    else
				    {
					data[sii] = 0;
					missing++;
				    }
				}
				target.addSpotAttribute((String)md.spot_attr_name.elementAt(a), uname, "CHAR", (Object) data);
			    }
			    if(tname.equals("TEXT"))
			    {
				String[] data = new String[n_spots];
				while(rs.next())
				{
				    Integer si = (Integer) spot_id_to_index_ht.get((String) rs.getString(1));
				    int sii = si.intValue();
				    
				    if(rs.getString(2) != null)
					data[sii] = rs.getString(2);
				    else
					missing++;

				}
			    target.addSpotAttribute((String)md.spot_attr_name.elementAt(a), uname, "TEXT", (Object) data);
			    }

			    dbcon.closeResultSet( rs );
			}
			else
			    return false;
		    }
		}
		catch (SQLException sqle)
		{
		    System.out.println(sqle);
		    return false;
		}
		catch (NullPointerException npe)
		{
		    System.out.println(npe);
		    return false;
		}
		catch (ArrayIndexOutOfBoundsException aioobe)
		{
		    System.out.println(aioobe);
		    return false;
		}
	    
		if(missing > 0)
		{
		    message_v.addElement( new String("Warning: in '" + md.name + "' there " + 
						     (missing == 1 ? "was 1 missing Spot Attribute value" : 
						      ("were " + missing + " missing Spot Attribute values")) +
						     " for '" + md.spot_attr_name.elementAt(a) + "'"));
		}
	    }

	    return true;
	}
	
	// load the Probe.Names (and other details if req'd) for all Spots in this Measurement
	// and then load the Genes that are linked to those Probes
	//
	// returns the number of Spots read
	//
	public int loadProbesAndGenesInSpots(String measure_id)
	{
	    String image_id = getFieldFromTable("Measurement", measure_id, "Image_ID");
	    
	    String hyb_id   = getFieldFromTable("Image", image_id, "Hybridisation_ID");
	    
	    String arr_id   = getFieldFromTable("Hybridisation", hyb_id ,"Array_ID");
	    
	    String arr_t_id = getFieldFromTable("Array", arr_id ,"Array_Type_ID");
	    
	    //System.out.println("array type is " + arr_t_id);
	    
	    if((current_array_type_id != null) && current_array_type_id.equals(arr_t_id))
	    {
		// has the user requested different name details than last time
		// the array was loaded?
		if(load_name_details_has_changed == false)
		{
		    System.out.println("spot & probe, gene data already loaded. doing nothing");

		    return current_n_spots;
		}
		else
		{
		    // remove the existing DataTags and TagAttrs
		    edata.removeAllMeasurements(true);
		}

	    }
	    
	    StringBuffer sbuf = new StringBuffer();
	    
	    
	    //
	    // now we know the Array_Type_ID, load all spots from this ArrayType
	    //
	    
	    pm.setMessage(2, "Querying Spots and Probes...");
	    
	    sbuf.append("SELECT ");
	    sbuf.append(dbcon.qTableDotField("Spot","ID") + ", ");
	    sbuf.append(dbcon.qTableDotField("Spot","Name") + ", ");
	    if(load_name_details[0] == true)
	    {
		sbuf.append(dbcon.qTableDotField("Spot","Row") + ", ");
		sbuf.append(dbcon.qTableDotField("Spot","Column") + ", ");
	    }
	    sbuf.append(dbcon.qTableDotField("Probe","Name") + ",");
	    sbuf.append(dbcon.qTableDotField("Probe","ID"));
	    if(load_name_details[1] == true)
	    {
		sbuf.append(", " + dbcon.qTableDotField("Probe","Short_Description"));
		sbuf.append(", " + dbcon.qTableDotField("Probe","Clone_Database_ID"));
		sbuf.append(", " + dbcon.qTableDotField("Probe","Clone_Entry_ID"));
		sbuf.append(", " + dbcon.qTableDotField("Probe","Sequence_Database_ID"));
		sbuf.append(", " + dbcon.qTableDotField("Probe","Sequence_Entry_ID"));
		sbuf.append(", " + dbcon.qTableDotField("Probe","Sequence_Verified"));
	    }
	    sbuf.append(" FROM ");
	    sbuf.append(dbcon.qTable("Spot") + ", ");
	    sbuf.append(dbcon.qTable("Probe"));
	    sbuf.append(" WHERE ");
	    sbuf.append(dbcon.qTableDotField("Spot","Probe_ID") + " = " + dbcon.qTableDotField("Probe","ID"));
	    sbuf.append(" AND ");
	    sbuf.append(dbcon.qTableDotField("Spot", "Array_Type_ID") + " = " + dbcon.qID(arr_t_id));
	    
	    // System.out.println("[[[" + sbuf.toString() + "]]]");

	    Vector  new_spot_v        = new Vector();

	    Vector  new_spot_row_v    = new Vector();
	    Vector  new_spot_col_v    = new Vector();

	    Vector  new_spot_id_v     = new Vector();
	    Vector  new_probe_v       = new Vector();
	    Vector  new_probe_id_v    = new Vector();
	    
	    Vector  new_probe_de_v    = new Vector();
	    Vector  new_probe_cd_v    = new Vector();
	    Vector  new_probe_ce_v    = new Vector();
	    Vector  new_probe_sd_v    = new Vector();
	    Vector  new_probe_se_v    = new Vector();
	    Vector  new_probe_sv_v    = new Vector();

	    Hashtable probe_name_to_spot_indices = new Hashtable();

	    ResultSet rs = dbcon.executeQuery(sbuf.toString());
	    
	    if(rs != null)
	    {
		try
		{
		    // Vector  new_gene_v  = new Vector();
		    
		    while(rs.next())
		    {
			int col = 1;
			
			new_spot_id_v.addElement(rs.getString(col++));
			new_spot_v.addElement(rs.getString(col++));
			
			if(load_name_details[0] == true)
			{
			    new_spot_row_v.addElement(rs.getString(col++));
			    new_spot_col_v.addElement(rs.getString(col++));
			}
			
			new_probe_v.addElement(rs.getString(col++));
			new_probe_id_v.addElement(rs.getString(col++));
						  
			if(load_name_details[1] == true)
			{
			    new_probe_de_v.addElement(rs.getString(col++));
			    new_probe_cd_v.addElement(rs.getString(col++));
			    new_probe_ce_v.addElement(rs.getString(col++));
			    new_probe_sd_v.addElement(rs.getString(col++));
			    new_probe_se_v.addElement(rs.getString(col++));
			    new_probe_sv_v.addElement(rs.getString(col++));
			}
			//new_gene_v.addElement(rs.getString(2));
		    }
		}
		catch (SQLException sqle)
		{
		    System.out.println("Problem with:\n" + sbuf.toString());
		    
		    mview.alertMessage("Unable to execute SQL: " + sqle);
		}
		finally
		{
		    dbcon.closeResultSet( rs );
		}
	    }
	    
	    System.out.println(new_spot_v.size()  + " spots loaded for this ArrayType\n");
	    
	    //
	    // build the DataTags
	    //
	    final int n_spots = new_spot_v.size();
	    
	    String[]   new_spot_name_a   = new String[n_spots];
	    String[]   new_probe_name_a  = new String[n_spots];
	    
	    // and the mapping from Spot.ID to DataTags index
	    //
	    spot_id_to_index_ht = new Hashtable();
	    
	    for(int sa=0; sa < n_spots; sa++)
	    {
		new_spot_name_a[sa]  = (String)new_spot_v.elementAt(sa);
		new_probe_name_a[sa] = (String)new_probe_v.elementAt(sa);
		
		if( !optimise_for_small_joins ) // see below....
		{
		    // store a mapping of Probe.Name to spot indices for use later when retrieving Genes
		    
		    int[] sids = (int[]) probe_name_to_spot_indices.get( new_probe_name_a[sa] );
		    
		    sids = appendIntToIntArray( sids, sa );

		    probe_name_to_spot_indices.put( new_probe_name_a[sa], sids );
		}
		

		spot_id_to_index_ht.put((String)new_spot_id_v.elementAt(sa), new Integer(sa));
	    }
	    
	    if(load_name_details[0] == true)
	    {
		// save spot details
		/*
		spot_details = new Vector();
		spot_details.addElement(new_spot_row_v);
		spot_details.addElement(new_spot_col_v);
		*/
		ExprData.TagAttrs sta = mview.getExprData().getSpotTagAttrs();
		
		int rid = sta.addAttr("ROW");
		int cid = sta.addAttr("COL");

		for(int s=0; s < n_spots; s++)
		{
		    sta.setTagAttr(new_spot_name_a[s], rid, (String) new_spot_row_v.elementAt(s));
		    sta.setTagAttr(new_spot_name_a[s], cid, (String) new_spot_col_v.elementAt(s));
		}
		
	    }
	    if(load_name_details[1] == true)
	    {
		ExprData.TagAttrs pta = mview.getExprData().getProbeTagAttrs();
		
		int[] aid = new int[6];
		
		aid[0] = pta.addAttr("DESCRIPTION");
		aid[1] = pta.addAttr("CLONE DB");
		aid[2] = pta.addAttr("CLONE ENTRY");
		aid[3] = pta.addAttr("SEQ DB");
		aid[4] = pta.addAttr("SEQ ENTRY");
		aid[5] = pta.addAttr("SEQ VERIFIED");

		for(int s=0; s < n_spots; s++)
		{
		    pta.setTagAttr(new_probe_name_a[s], aid[0], (String) new_probe_de_v.elementAt(s));
		    pta.setTagAttr(new_probe_name_a[s], aid[1], (String) new_probe_cd_v.elementAt(s));
		    pta.setTagAttr(new_probe_name_a[s], aid[2], (String) new_probe_ce_v.elementAt(s));
		    pta.setTagAttr(new_probe_name_a[s], aid[3], (String) new_probe_sd_v.elementAt(s));
		    pta.setTagAttr(new_probe_name_a[s], aid[4], (String) new_probe_se_v.elementAt(s));
		    pta.setTagAttr(new_probe_name_a[s], aid[5], (String) new_probe_sv_v.elementAt(s));
		}

		// save probe details
		/*
		probe_details = new Vector();
		probe_details.addElement(new_probe_de_v);
		probe_details.addElement(new_probe_cd_v);
		probe_details.addElement(new_probe_ce_v);
		probe_details.addElement(new_probe_sd_v);
		probe_details.addElement(new_probe_se_v);
		probe_details.addElement(new_probe_sv_v);
		*/
	    }

	    // ==============================================================
	    // now load the genes.....

	    pm.setMessage(2, "Querying Genes...");

	    Vector  new_gene_v      = new Vector(); // ordered per spot (can be multiple genes per spot)

	    Vector  all_gene_names  = new Vector(); // geneneral list of _all_ genes 
	    Vector  new_gene_db_v   = new Vector();
	    Vector  new_gene_de_v   = new Vector();

	    String[][] new_gene_name_a   = new String[n_spots][];

	    if ( optimise_for_small_joins )
	    {
		System.out.println("[retrieving genes using the multiple query method]");

		sbuf = new StringBuffer();
		sbuf.append("SELECT ");
		
		sbuf.append(dbcon.qTableDotField("Gene", "Name"));
		
		if(load_name_details[2] == true)
		{
		    sbuf.append("," + dbcon.qTableDotField("Gene", "Database_ID"));
		    sbuf.append("," + dbcon.qTableDotField("Gene", "Entry_ID"));
		}
		
		sbuf.append(" FROM " + dbcon.qTable("Gene") + "," + dbcon.qTable("GeneAsProbe") + " WHERE ");
		sbuf.append(dbcon.qTableDotField("Gene", "ID") + " = " + dbcon.qTableDotField("GeneAsProbe", "Gene_ID"));
		sbuf.append(" AND " + dbcon.qTableDotField("GeneAsProbe", "Probe_ID") + " = ");
	    
		String  sbuf_hdr = sbuf.toString();
	    
		try
		{
		    for(int p=0; p < new_spot_v.size(); p++)
		    {
			String pid = (String) new_probe_id_v.elementAt(p);
			
			sbuf = new StringBuffer();
			sbuf.append(sbuf_hdr);
			sbuf.append(dbcon.qID(pid));
			
			int hits = 0;
			
			rs = dbcon.executeQuery(sbuf.toString());
			
			if(rs != null)
			{
			    Vector gene_names_for_this_probe = new Vector();
			    
			    while(rs.next()) 
			    {
				hits++;
				String gname = rs.getString(1);
				gene_names_for_this_probe.addElement(gname);
				
				if(load_name_details[2] == true)
				{
				    all_gene_names.addElement(gname);
				    new_gene_db_v.addElement(rs.getString(2));
				    new_gene_de_v.addElement(rs.getString(3));
				}
			    }
			    
			    if(hits > 0)
				new_gene_v.addElement(gene_names_for_this_probe);
			    
			    dbcon.closeResultSet( rs );
			}
			
			if(hits == 0)
			    new_gene_v.addElement(null);
		    }
		}
		catch (SQLException sqle)
		{
		    System.out.println("Problem with:\n" + sbuf.toString());
		    
		    mview.alertMessage("Unable to execute SQL: " + sqle);
		}

		System.out.println(new_gene_v.size() + " genes found");
		
		for(int ga=0; ga < n_spots; ga++)
		{
		    Vector gns = (Vector) new_gene_v.elementAt(ga);
		    if(gns != null)
		    {
			new_gene_name_a[ga] = new String[gns.size()];
			for(int g=0; g < gns.size(); g++)
			    new_gene_name_a[ga][g] = (String) gns.elementAt(g);
		    }
		}
		
		if(load_name_details[2] == true)
		{
		    ExprData.TagAttrs gta = mview.getExprData().getGeneTagAttrs();
		    
		    int gene_db_name_aid  = gta.addAttr("DB_NAME");
		    int gene_db_entry_aid = gta.addAttr("DB_ENTRY");
		    
		    for(int g=0; g < all_gene_names.size(); g++)
		    {
			String gname = (String) all_gene_names.elementAt(g);
			
			if(gname != null)
			{
			    gta.setTagAttr(gname, gene_db_name_aid,  (String) new_gene_db_v.elementAt(g));
			    gta.setTagAttr(gname, gene_db_entry_aid, (String) new_gene_de_v.elementAt(g));
			}
		    }
		}

	    }
	    else
	    {
		// retrieve all the genes in a single query; this uses a much bigger
		// join than method (1) above, but only requires a single transaction
		// with the RDBMS and thus can be faster over a low bandwidth connection

		System.out.println("[retrieving genes using the single query method]");

sbuf = new StringBuffer();
		sbuf.append("SELECT ");
		
		sbuf.append(dbcon.qTableDotField("Gene", "Name") + "," + dbcon.qTableDotField("Probe", "Name"));
		
		if(load_name_details[2] == true)
		{
		    sbuf.append("," + dbcon.qTableDotField("Gene", "Database_ID"));
		    sbuf.append("," + dbcon.qTableDotField("Gene", "Entry_ID"));
		}

		sbuf.append(" FROM " + 
			    dbcon.qTable("Spot") + ", " + 
			    dbcon.qTable("Probe") + ", " + 
			    dbcon.qTable("Gene") + "," + 
			    dbcon.qTable("GeneAsProbe") + 
			    " WHERE ");

		sbuf.append(dbcon.qTableDotField("Gene", "ID") + " = " + dbcon.qTableDotField("GeneAsProbe", "Gene_ID") +" AND ");
		sbuf.append(dbcon.qTableDotField("GeneAsProbe", "Probe_ID") + " = " + dbcon.qTableDotField("Probe", "ID") + " AND ");
		sbuf.append(dbcon.qTableDotField("Spot","Probe_ID") + " = " + dbcon.qTableDotField("Probe","ID") + " AND ");
		sbuf.append(dbcon.qTableDotField("Spot", "Array_Type_ID") + " = " + dbcon.qID(arr_t_id));
	    
		int hits = 0;
		
		rs = dbcon.executeQuery(sbuf.toString());
		
		if(rs != null)
		{
		    ExprData.TagAttrs gta = mview.getExprData().getGeneTagAttrs();
		    
		    int gene_db_name_aid  = gta.addAttr("DB_NAME");
		    int gene_db_entry_aid = gta.addAttr("DB_ENTRY");
		    
		    try
		    {
			while(rs.next()) 
			{
			    hits++;
			    
			    String gname = rs.getString(1);

			    all_gene_names.addElement(gname);

			    // store the TagAttrs for this Gene
			    if(load_name_details[2] == true)
			    {
				gta.setTagAttr(gname, gene_db_name_aid,  rs.getString(3) );
				gta.setTagAttr(gname, gene_db_entry_aid, rs.getString(4) );
			    }
			    
			    // which spot indices does this gene correspond to?
			    
			    String pname = rs.getString(2);
			    
			    int[] sids = (int[]) probe_name_to_spot_indices.get( pname );
			    
			    if(sids != null)
			    {
				// and store the Gene name in the relevant indices

				for(int s=0; s< sids.length; s++)
				{
				    insertStringToStringArrayArray( new_gene_name_a, sids[s], gname );
				}
			    }
			}
			System.out.println(hits + " genes found");
		    }

		    catch(SQLException sqle)
		    {
			mview.alertMessage("SQL error\n" + sqle);
		    }
		    finally
		    {
			dbcon.closeResultSet( rs );
		    }
		}
		
		//if(hits == 0)
		//    new_gene_v.addElement(null);
	    }
	    
	    
	    // ==============================================================
	    // build the DataTags

	    
	    current_dtags = edata.new DataTags(new_spot_name_a, new_probe_name_a, new_gene_name_a);	
	    current_array_type_id = arr_t_id;
	    current_n_spots = n_spots;
	    
	    
	    // ==============================================================

	    pm.setMessage(3, n_spots + " Spots on this ArrayType");
	    
	    load_name_details_has_changed = false;
	    
	    return n_spots;
	}
	

	// array handling utils 
	private void insertStringToStringArrayArray( String[][] saa, int pos, String str )
	{
	    String[] cur = saa[pos];
	    if(cur == null)
	    {
		cur = new String[1];
		cur[0] = str;
	    }
	    else
	    {
		String[] cur_cpy = new String[ cur.length + 1 ];
		for(int cpy = 0; cpy < cur.length; cpy++)
		    cur_cpy[cpy] = cur[cpy];
		cur_cpy[cur.length] = str;
		cur = cur_cpy;
	    }
	    saa[pos ] = cur;
	}

	private int[] appendIntToIntArray( int[] src, int val )
	{
	    if(src == null)
	    {
		int[] new_array = new int[ 1 ];
		new_array[0] = val;
		return new_array;
	    }
	    else
	    {
		int[] new_array = new int[ src.length + 1 ];
		for(int cpy = 0; cpy < src.length; cpy++)
		    new_array[cpy] = src[cpy];
		new_array[src.length] = val;
		return new_array;
	    }
	}

	private Vector spot_details, probe_details;

	// load the Measurement specified by index 'sel' in the current list
	public void loadMeasurement(int sel, Vector warning_v)
	{
	    if(sel < 0)
		return;
	    
	    Date start_time = new Date();
	    
	    
	    // we know the Measurement_ID of the target,
	    //  ... find lots of associated stuff based on this key
	    
	    String measure_id = (String) measure_id_strs.elementAt(sel);
	    
	    double[] new_e_val_a = null;
	    double[] new_s_val_a = null;
	    
	    // System.out.println("loading ID " + measure_id);
	    
	    Hashtable selected_spot_attrs = new Hashtable();
	    for(int sao=1; sao < atts_sel_jchkb.length; sao++)
	    {
		if(atts_sel_jchkb[sao].isSelected())
		    selected_spot_attrs.put(atts_sel_jchkb[sao].getText(), "x");
	    }
	    
	    try
	    {
		// get lots of interesting Measurement details, i.e. name 
		//
		pm.setMessage(2, "Details...");
		
		MeasurementDetails m_details = getMeasurementDetails(sel);

		pm.setMessage(1, m_details.name);

		Vector spot_atts_v = new Vector();
		
		boolean sig_sel = atts_sel_jchkb[0].isSelected();
		//System.out.println("significance is " + (sig_sel ? "on" : "off"));
		
		/*
		  // debug SpotAttr selection

		if(atts_sel_jchkb != null)
		{
		    for(int sa=0; sa < m_details.spot_attr_name.size(); sa++)
		    {
			String att_name    = (String) m_details.spot_attr_name.elementAt(sa);
			String att_type_id = (String) m_details.spot_attr_type_id.elementAt(sa);
			
			if(selected_spot_attrs.get(att_name) != null)
			{
			    System.out.println( att_name + " ("+  att_type_id + ") is selected");
			}
			else
			{
			    System.out.println( att_name + " ("+  att_type_id + ") is available but not selected");
			}

		    }
		}
		*/

		// the Spot.ID -> Probe.Name mappings for this ArrayType
		
		final int n_spots = loadProbesAndGenesInSpots(measure_id);
				
		// get the <Spot, E_Level [,Sigificance]> tuples from SpotMeasurement
		//
		
		pm.setMessage(2, "Querying Expression Levels...");
		
		StringBuffer sbuf = new StringBuffer();

		/*
		sbuf.append("SELECT ");
		sbuf.append(dbcon.qTableDotField("Spot","ID") + ", ");
		sbuf.append(dbcon.qField("Expression_Level") + ", ");
		sbuf.append(dbcon.qField("Output_Description_ID"));
		if(sig_sel)
		{
		    sbuf.append(", " + dbcon.qField("Significance"));
		}
		sbuf.append(" FROM ");
		sbuf.append(dbcon.qTable("SpotMeasurement") + ", ");
		sbuf.append(dbcon.qTable("Spot"));
		sbuf.append(" WHERE ");
		sbuf.append(dbcon.qField("Measurement_ID") + " = " + dbcon.qID(measure_id));
		sbuf.append(" AND ");
		sbuf.append(dbcon.qTableDotField("SpotMeasurement","Spot_ID") + " = " + dbcon.qTableDotField("Spot","ID"));
		*/

		sbuf.append("SELECT ");
		sbuf.append(dbcon.qField("Spot_ID") + ", ");
		sbuf.append(dbcon.qField("Expression_Level") + ", ");
		sbuf.append(dbcon.qField("Output_Description_ID"));
		if(sig_sel)
		{
		    sbuf.append(", " + dbcon.qField("Significance"));
		}
		sbuf.append(" FROM ");
		sbuf.append(dbcon.qTable("SpotMeasurement"));
		sbuf.append(" WHERE ");
		sbuf.append(dbcon.qField("Measurement_ID") + " = " + dbcon.qID(measure_id));

		// we know the maximum number of spots to expect
		//
		new_e_val_a = new double[n_spots];
		new_s_val_a = new double[n_spots];
		
		// fill the arrays with NaN's to handle any missing Spots
		for(int ns=0; ns < n_spots; ns++)
		    new_e_val_a[ns] = Double.NaN;
		
		if(sig_sel)
		    for(int ns=0; ns < n_spots; ns++)
			new_s_val_a[ns] = Double.NaN;
		
		// System.out.println("exec: " + sbuf.toString());

		int e_count = 0;
		
		ResultSet rs = dbcon.executeQuery(sbuf.toString());

		pm.setMessage(2, "Receiving Expression Levels...");

		if(rs != null)
		{
		    while(rs.next())
		    {
			Integer spot_index = (Integer) spot_id_to_index_ht.get(rs.getString(1));
			
			// System.out.println(rs.getString(1) + " -> " + spot_index.toString());
			
			if( spot_index != null )
			{
			    int s_i_i = spot_index.intValue();
			    
			    if(rs.getString(2) != null)
				new_e_val_a[s_i_i] = rs.getDouble(2);
			    //else
			    //    System.out.println("missing eval in spot " + s_i_i);
			    
			    // String out_desc_id = rs.getString(3);  // this is not used to match SpotAttrs
			    
			    if(sig_sel)
			    {
				if(rs.getString(4) != null)
				    new_s_val_a[s_i_i] = rs.getDouble(4);
			    }
			    
			    e_count++;
			}
			
			if(n_spots > 10)
			{
			    if((e_count % (n_spots / 10)) == 0)
			    {
				double pc = ((double) e_count * 100.0) / (double) n_spots;
				pm.setMessage(3, mview.niceDouble(pc, 4, 2) + " %");
			    }
			}

			yield();
		    }

		    dbcon.closeResultSet( rs );
		}
		
		/*
		mview.addMessageToLog( e_count + " expression levels loaded from database\n" +
				       "Measurement name is " + m_details.getName() + "\n" + 
				       "ArrayType is " + m_details.getAttribute("ArrayType name") + " with " + 
				       m_details.getAttribute("Number of Spots") + " spots");
		*/

		pm.setMessage(3, e_count + " SpotMeasurements found");
		
		if(e_count > 0)
		{
		    //System.out.println("adding to " + edata);
		    
		    ExprData.Measurement m = edata.new Measurement();

		    m.setName( m_details.name );
		    
		    m.addAttribute("Database name", plugin_name, dbcon.maxd_props.database_name);
		    m.addAttribute("Database version", plugin_name, dbcon.maxd_props.database_version);
		    m.addAttribute("Database location", plugin_name, dbcon.maxd_props.database_location);

		    m.setShow(true);
		    m.setDataType(ExprData.ExpressionAbsoluteDataType);
		    m.setData(new_e_val_a);
		    m.setDataTags(current_dtags);
		    
		    // copy the attributes from the MeasurementDetails
		    for (Enumeration e = m_details.details.keys() ; e.hasMoreElements() ;) 
		    {
			String key = (String) e.nextElement();
			m.addAttribute(key, plugin_name, (String) m_details.details.get(key));
		    }

		    // load SpotAttrs....
		    if(atts_sel_jchkb != null)
		    {
			loadSpotAttributes(measure_id, m_details, m, n_spots, warning_v);
		    }
		    
		    // and install the data
		    mview.getExprData().addMeasurement(m);
		    
		    if(sig_sel)
		    {
			// and add the Significance data as a separate Measurement
			//
			m = edata.new Measurement();
			
			m.addAttribute("Database name", plugin_name, dbcon.maxd_props.database_name);
			m.addAttribute("Database version", plugin_name, dbcon.maxd_props.database_version);
			m.addAttribute("Database location", plugin_name, dbcon.maxd_props.database_location);
			
			for (Enumeration e = m_details.details.keys() ; e.hasMoreElements() ;) 
			{
			    String key = (String) e.nextElement();
			    m.addAttribute(key, plugin_name, (String) m_details.details.get(key));
			}

			m.setName(m_details.name + "_sig");
			m.setShow(true);
			m.setDataType(ExprData.ErrorDataType);
			m.setData(new_s_val_a);
			m.setDataTags(current_dtags);

			mview.getExprData().addMeasurement(m);
		    }
		    
		}
		else
		{
		    mview.addMessageToLog("no SpotMeasurements associated with selected Measurement '" +
					  m_details.name + "'");
		}
		// TODO; now look up the Gene's associated with the Probe names
		// 
	    }
	    catch (SQLException sqle)
	    {
		mview.alertMessage("Unable to execute SQL:\n\n  " + sqle);
	    }
	    catch (Exception e)
	    {
		mview.alertMessage("Serious error whilst loading data:\n\n  " + e);
	    }

	    long elapsed = new Date().getTime() -  start_time.getTime();
	    double elapsed_d = (double) elapsed / 1000.0;
	    
	    pm.setMessage(3, "(last Measurement loaded in " + mview.niceDouble(elapsed_d,8,2) + "s)");
	    //System.out.println("loaded in " + mview.niceDouble(elapsed_d,8,2) + "s");
	}
	
	public void run()
	{
	    int count = 0;
	    int total = 0;


	    Date start_time = new Date();

	    Vector warning_v = new Vector();

	    for(int s = browse_list.getMinSelectionIndex(); s <= browse_list.getMaxSelectionIndex(); s++)
	    {
		if(browse_list.isSelectedIndex(s))
		{
		    total++;
		}
	    }
	    for(int s = browse_list.getMinSelectionIndex(); s <= browse_list.getMaxSelectionIndex(); s++)
	    {
		if(browse_list.isSelectedIndex(s))
		{
		    if(total > 1)
		    {
			//System.out.println((count+1) + " of " + total);

			pm.setMessage(0,  (count+1) + " of " + total);
		    }
		    
		    loadMeasurement(s, warning_v);
		    count++;
		}
		
	    }
	    
	    String m_pl = (count > 1) ? (count + " Measurments") : "Measurement";

	    long elapsed = new Date().getTime() -  start_time.getTime();
	    double elapsed_d = (double) elapsed / 1000.0;

	    mview.addMessageToLog(m_pl + " loaded in " + mview.niceDouble(elapsed_d,8,2) + "s");
	    
	    pm.stopIt();

	    if(warning_v.size() > 0)
	    {
		String warning = "";
		for(int w=0; w < warning_v.size(); w++)
		    warning += ((String) warning_v.elementAt(w)) + "\n";
		mview.infoMessage(warning);
	    }
	}
    }

    public void loadSelection(boolean replace)
    {
	pm = new ProgressOMeter("Loading", 5);
	pm.startIt();

	new MeasurementLoaderThread(replace).start();
    }

    // ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ 
    // ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ 
    // ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ ------ 

    public String getFieldFromTable(String tname, String tid, String cname)
    {
	if(tid == null)
	    return null;

	String sql = 
	"SELECT " + dbcon.qField(cname) + 
	" FROM " + dbcon.qTable(tname) + 
	" WHERE " + dbcon.qField("ID") + " = " + dbcon.qID(tid);

	String result = null;

	ResultSet rs = dbcon.executeQuery(sql);
 
	if(rs != null)
	{
	    try
	    {
		while((result == null) && (rs.next()))
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
		dbcon.closeResultSet( rs );
	    }
	}

	return result;
    }

    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

    private boolean optimise_for_small_joins = true;

    private DatabaseConnection dbcon;

    boolean synthesise_connection = false;
    boolean debug_connect = false;
	
    private Connection connection = null;
	
    private Vector expt_data = new Vector();

    private ExprData.DataTags current_dtags = null;
    private String            current_array_type_id = null;
    private int               current_n_spots = 0;
    private Hashtable         spot_id_to_index_ht = null;

    private ProgressOMeter pm = null;

    String browse_target_name = null;
    private JLabel browse_table_label;
    private JComboBox browse_items_jcb, browse_jcb;
    private JList browse_list;
    private JTable details_table;
    private DefaultListModel list_model;
    private ActionListener browse_items_al;
    Vector measure_id_strs = null;

    private JPanel atts_wrapper;
    private JPanel opts_wrapper;

    private maxdView mview;
    private ExprData edata;

    private JFrame frame;

    private JSplitPane c_split_pane, v_split_pane, h_split_pane;

    private JList db_saves_list;
}
