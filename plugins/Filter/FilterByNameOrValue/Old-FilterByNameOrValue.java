import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.*;

public class FilterByNameOrValue implements Plugin, ExprData.Filter, ExprData.ExprDataObserver
{
    public FilterByNameOrValue(maxdView mview_)
    {
	mview = mview_;
	edata = mview.getExprData();
    }

    public void cleanUp()
    {
	edata.removeObserver(this);
	edata.removeFilter(this);

	// remove any 'old' application props
	// (to avoid a build up of redundant measurement filter rules over time....)
	//
	Properties props = mview.getProperties();

	for(Enumeration e = props.keys(); e.hasMoreElements() ;) 
	{
	    String key = (String) e.nextElement();

	    if(key.startsWith("FilterByNameOrValue."))
		props.remove(key);
	}

	// and save the current rules as application props
	//
	nt_sel.saveSelection("FilterByNameOrValue.source");

	mview.putIntProperty("FilterByNameOrValue.filter_mode", filter_mode);
	mview.putIntProperty("FilterByNameOrValue.filter_logic", filter_logic);
	mview.putIntProperty("FilterByNameOrValue.filter_op", filter_op);
	mview.putProperty("FilterByNameOrValue.filter_value", String.valueOf(filter_value));

	mview.putIntProperty("FilterByNameOrValue.name_mode", filter_by_name_mode);
	mview.putProperty("FilterByNameOrValue.name_string", filter_by_name_str);

	for(int m=0; m < measurement_filter_value.length; m++)
	{
	    mview.putProperty("FilterByNameOrValue.meas_rule.meas." + m, measurement_filter_name[m]);
	    mview.putProperty("FilterByNameOrValue.meas_rule.value." + m, String.valueOf(measurement_filter_value[m]));
	    mview.putIntProperty("FilterByNameOrValue.meas_rule.op." + m, measurement_filter_op[m]);
	    mview.putBooleanProperty("FilterByNameOrValue.meas_rule.enabled." + m, measurement_filter_enabled[m]);
	}
	

	if(frame != null)
	    frame.setVisible(false);
    }
    
    public void stopPlugin()
    {
	cleanUp();
    }
 
    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("Filter By Name Or Value", "filter", 
					 "Filter spots by their names or values", "",
					 1, 0, 0);
	return pinf;
    }

    public PluginCommand[] getPluginCommands()
    {
	PluginCommand[] com = new PluginCommand[2];
	
	com[0] = new PluginCommand("start", null);

	com[1] = new PluginCommand("stop", null);

	return com;
    }

    public void   runCommand(String name, String[] args, CommandSignal done) 
    {
	if(name.equals("start"))
	{
	    startPlugin();
	}
	if(name.equals("stop"))
	{
	    cleanUp();
	}
	if(done != null)
	    done.signal();
    }

    public void startPlugin()
    {
	// final String[] name_modes = { "No filter", "Gene name(s)", "Probe name", "Spot name", "Spot comment" };
	final String[] match_modes = {  "Containing", "Not containing", "Starting with", "Ending with" };
	final String[] filter_modes = { "No filter", "Expression", "Probability", "Error" };
	final String[] filter_logic = { "No value", "Any values", "All values" };

	rebuildMeasurementFilter(); // will create arrays and set all default values 

	readAppProps();  // find rules for any matching measurement names

	frame = new JFrame("Filter By Name or Value");

	mview.decorateFrame(frame);

	JPanel filter_options = new JPanel();
	filter_options.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
	GridBagLayout gridbag = new GridBagLayout();
	filter_options.setLayout(gridbag);
	
	frame.getContentPane().add(filter_options, BorderLayout.CENTER);
	
	JPanel panel_1 = new JPanel();
	panel_1.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

	GridBagLayout gridbag_1 = new GridBagLayout();
	panel_1.setLayout(gridbag_1);

	{
	    JLabel label  = new JLabel("Names  ");
	    panel_1.add(label);
		
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag_1.setConstraints(label, c);
	}
	{
	    nt_sel = new NameTagSelector(mview, "No filter");
	    nt_sel.loadSelection("FilterByNameOrValue.source");
	    nt_selection = nt_sel.userOptionSelected() ? null : nt_sel.getNameTagSelection();
	    nt_sel.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			setFilterByNameSource(nt_sel.userOptionSelected(), nt_sel.getNameTagSelection());
		    }
		});
	    panel_1.add(nt_sel);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag_1.setConstraints(nt_sel, c);
	}	       
	{

	    JComboBox jcb = new JComboBox(match_modes);
	    jcb.setSelectedIndex(getFilterByNameMode());
	    panel_1.add(jcb);

	    jcb.addActionListener(new ActionListener() 
				  { 
				      public void actionPerformed(ActionEvent e) 
				      {
					  JComboBox cb = (JComboBox)e.getSource();
					  int new_val = cb.getSelectedIndex();
					  setFilterByNameMode(new_val);

					  //boolean enabler = (new_val == 0) ? false : true;
					  //filter_name_jtf.setEnabled(enabler);
				      }
				  });

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = 0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag_1.setConstraints(jcb, c);
	}	       
	{
	    filter_name_jtf = new JTextField(20);

	    panel_1.add(filter_name_jtf);
		
	    filter_name_jtf.setText( String.valueOf(getFilterByNameString() ) );

	    filter_name_jtf.getDocument().addDocumentListener(new FilterByNameChangeListener());

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 3;
	    c.gridy = 0;
	    c.weightx = 2.0;
	    c.gridwidth = GridBagConstraints.REMAINDER;
	    c.fill = GridBagConstraints.BOTH;
	    c.weightx = 1.0;
	    gridbag_1.setConstraints(filter_name_jtf, c);
	}
    
	    
	{
	    JLabel label  = new JLabel("Values  ");
	    panel_1.add(label);
		
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag_1.setConstraints(label, c);
	}

	{

	    JComboBox jcb = new JComboBox(filter_modes);
	    jcb.setSelectedIndex(getFilterMode());
	    panel_1.add(jcb);

	    jcb.addActionListener(new ActionListener() 
				  { 
				      public void actionPerformed(ActionEvent e) 
				      {
					  JComboBox cb = (JComboBox)e.getSource();
					  int new_val = cb.getSelectedIndex();
					  setFilterMode(new_val);

					  //boolean enabler = (new_val == 0) ? false : true;
					  //filter_logic_jcb.setEnabled(enabler);
					  //filter_ops_jcb.setEnabled(enabler);
					  //filter_val_jtf.setEnabled(enabler);
						  
				      }
				  });

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 1;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    gridbag_1.setConstraints(jcb, c);
	}	       

	{

	    filter_logic_jcb = new JComboBox(filter_logic);
	    filter_logic_jcb.setSelectedIndex(getFilterLogic());
	    panel_1.add(filter_logic_jcb);

	    filter_logic_jcb.addActionListener(new ActionListener() 
					       { 
						   public void actionPerformed(ActionEvent e) 
						   {
						       JComboBox cb = (JComboBox)e.getSource();
						       int new_val = cb.getSelectedIndex();
						       setFilterLogic(new_val);
						   }
					       });

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = 1;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.CENTER;
	    gridbag_1.setConstraints(filter_logic_jcb, c);
	}	       

	{

	    filter_ops_jcb = new JComboBox(filter_ops);
	    filter_ops_jcb.setSelectedIndex(getFilterOp());
	    panel_1.add(filter_ops_jcb);

	    filter_ops_jcb.addActionListener(new ActionListener() 
					     { 
						 public void actionPerformed(ActionEvent e) 
						 {
						     JComboBox cb = (JComboBox)e.getSource();
						     int new_val = cb.getSelectedIndex();
						     setFilterOp(new_val);
						 }
					     });

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 3;
	    c.gridy = 1;
	    c.anchor = GridBagConstraints.CENTER;
	    gridbag_1.setConstraints(filter_ops_jcb, c);
	}	       
     
	{
	    filter_val_jtf = new JTextField(6);
	    panel_1.add(filter_val_jtf);
	
	    filter_val_jtf.setText( String.valueOf(getFilterValue() ) );

	    filter_val_jtf.getDocument().addDocumentListener(new FilterValueChangeListener());

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 4;
	    c.gridy = 1;
	    //c.gridwidth = GridBagConstraints.REMAINDER;
	    c.fill = GridBagConstraints.BOTH;
	    c.anchor = GridBagConstraints.WEST;
	    c.weightx = 1.0;
	    //c.weighty = 1.0;
	    gridbag_1.setConstraints(filter_val_jtf, c);

	}

	meas_filter_panel = new JPanel();
	meas_filter_panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
	meas_filter_scroller = new JScrollPane(meas_filter_panel);

	addMeasurementFilters();

	JPanel buttons_panel = new JPanel();
	buttons_panel.setBorder(BorderFactory.createEmptyBorder(10,10,0,10));
	{
	    GridBagLayout inner_gridbag = new GridBagLayout();
	    buttons_panel.setLayout(inner_gridbag);	
	    {   
		miss_label = new JLabel("-");
		miss_label.setForeground(new Color(0,255-32-16,0));
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 1.0;
	        inner_gridbag.setConstraints(miss_label, c);

		buttons_panel.add(miss_label);
	    }


	    {   
		final JButton jb = new JButton("Close");
		jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					 {
					     cleanUp();
					 }
				     });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.anchor = GridBagConstraints.EAST;
		c.weightx = 1.0;
		inner_gridbag.setConstraints(jb, c);
		buttons_panel.add(jb);
	    }
	    {   
		final JButton jb = new JButton("Help");
		
		jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					 {
					     mview.getPluginHelpTopic("FilterByNameOrValue", "FilterByNameOrValue");
					 }
				     });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 1.0;
		inner_gridbag.setConstraints(jb, c);
		buttons_panel.add(jb);
	    }

	    {   
		hit_label = new JLabel("-");
		hit_label.setForeground(new Color(255-48-16,0,0));
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 3;
		c.gridy = 0;
		c.anchor = GridBagConstraints.EAST;
		c.weightx = 1.0;
		inner_gridbag.setConstraints(hit_label, c);

		buttons_panel.add(hit_label);

	    }

	}

	filter_options.add(panel_1);
	GridBagConstraints c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	c.gridwidth = GridBagConstraints.REMAINDER;
	c.fill = GridBagConstraints.HORIZONTAL;
	//c.weightx = c.weighty = 1.0;
	gridbag.setConstraints(panel_1, c);

	filter_options.add(meas_filter_scroller);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 1;
	c.gridwidth = GridBagConstraints.REMAINDER;
	c.fill = GridBagConstraints.BOTH;
	c.weightx = c.weighty = 1.0;
	gridbag.setConstraints(meas_filter_scroller, c);

	filter_options.add(buttons_panel);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 2;
	c.weightx = 1.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	gridbag.setConstraints(buttons_panel, c);

	edata.addFilter(this);
	edata.addObserver(this);
	startFilteredCounter();

	frame.pack();
	
	/*
	int w = meas_filter_scroller.getWidth();
	int h = meas_filter_scroller.getHeight();
	
	if(w > 700)
	    w = 700;
	if(h > 500)
	    h = 500;

	meas_filter_scroller.setPreferredSize(new Dimension(w,h));

	frame.pack();
	*/
	
	frame.setVisible(true);
    }

    private void addMeasurementFilters()
    {
	meas_filter_panel.removeAll();
	GridBagLayout meas_filter_gbag = new GridBagLayout();
	meas_filter_panel.setLayout(meas_filter_gbag);
	
	// how many columns to use?

	final int n_meas = edata.getNumMeasurements();
	
	if(n_meas == 0)
	    return;

	// pick the two factors that are closest to one another
	
        double best_rc_diff = Double.MAX_VALUE;
	int n_cols = 1;
	
	for(int m=1; m < n_meas; m++)
	{
	    double c = (double) m;
	    double r = ((double) n_meas) / c;
	    
	    double rc_diff = Math.abs(r - c);
	    if(rc_diff <  best_rc_diff)
	    {
		best_rc_diff= rc_diff;
		n_cols = (int) c;
	    }
	}

	if(n_cols > 6)
	    n_cols = 6;

	int meas_per_col = (n_cols > 0) ? (int)(Math.floor((double)n_meas / (double)n_cols)) : n_meas;
	
	int current_col = 0;
	int current_row = 0;
	

	for(int m=0;m<edata.getNumMeasurements();m++)
	{
	    int mi = edata.getMeasurementAtIndex(m);

	    int xpos = 4 * current_col;
	    int ypos = current_row;
	    {
		String prefix = (current_col == 0) ? "" : "  ";
		JLabel label  = new JLabel(prefix + edata.getMeasurementName(mi) + "  ");
		meas_filter_panel.add(label);
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = xpos + 0;
		c.gridy = ypos;
		c.anchor = GridBagConstraints.EAST;
		meas_filter_gbag.setConstraints(label, c);
	    }

	    {
		JCheckBox jchb = new JCheckBox();
		    
		jchb.setSelected(getMeasurementFilterEnabled(mi));
		meas_filter_panel.add(jchb);
		    
		jchb.addItemListener(new MeasFilterListener(mi));
		    
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = xpos + 1;
		c.gridy = ypos;
		meas_filter_gbag.setConstraints(jchb, c);
	    }

	    {
		    
		filter_ops_jcb = new JComboBox(filter_ops);
		filter_ops_jcb.setSelectedIndex(getMeasurementFilterOp(mi));
		meas_filter_panel.add(filter_ops_jcb);

		filter_ops_jcb.addActionListener(new MeasFilterLogicListener(mi));
		    
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = xpos + 2;
		c.gridy = ypos;
		c.anchor = GridBagConstraints.CENTER;
		meas_filter_gbag.setConstraints(filter_ops_jcb, c);
	    }	       
		
	    {
		filter_val_jtf = new JTextField(6);
		meas_filter_panel.add(filter_val_jtf);
		    
		filter_val_jtf.setText( String.valueOf(getMeasurementFilterValue(mi) ) );
		    
		filter_val_jtf.getDocument().addDocumentListener( new MeasFilterValueChangeListener(mi) );
		    
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = xpos + 3;
		c.gridy = ypos;
		//c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 2.0;
		c.weighty = 1.0;
		meas_filter_gbag.setConstraints(filter_val_jtf, c);
	    }

	    if(++current_row == meas_per_col)
	    {
		current_row = 0;
		current_col++;
	    }

	}

	
	meas_filter_panel.updateUI();

	int w = meas_filter_scroller.getWidth();
	int h = meas_filter_scroller.getHeight();
	
	if(w > 700)
	    w = 700;
	if(h > 500)
	    h = 500;

	meas_filter_scroller.setPreferredSize(new Dimension(w,h));

	meas_filter_scroller.updateUI();
    }

    private void readAppProps()
    {
	// get the name filter and global filter

	filter_mode = mview.getIntProperty("FilterByNameOrValue.filter_mode", 0);
	filter_logic = mview.getIntProperty("FilterByNameOrValue.filter_logic", 0);
	filter_op = mview.getIntProperty("FilterByNameOrValue.filter_op", 0);
	filter_value = mview.getDoubleProperty("FilterByNameOrValue.filter_value", .0);

	filter_by_name_mode = mview.getIntProperty("FilterByNameOrValue.name_mode", 0);
	filter_by_name_str = mview.getProperty("FilterByNameOrValue.name_string", "");
	

	Properties props = mview.getProperties();

	// build a map of known names...
	Hashtable meas_name_to_index = new Hashtable();

	for(int m=0; m < measurement_filter_name.length; m++)
	    meas_name_to_index.put(measurement_filter_name[m], new Integer(m));

	// int m_set = 0;

	for(Enumeration e = props.keys(); e.hasMoreElements() ;) 
	{
	    String key = (String) e.nextElement();

	    if(key.startsWith("FilterByNameOrValue.meas_rule.meas"))
	    {
		String last = key.substring(key.lastIndexOf('.')+1);

		// get the other parts of this rule....
		String mrn  = mview.getProperty("FilterByNameOrValue.meas_rule.meas." + last, null);
		double mrv  = mview.getDoubleProperty("FilterByNameOrValue.meas_rule.value." + last, .0);
		int mro     = mview.getIntProperty("FilterByNameOrValue.meas_rule.op." + last, 0);
		boolean mre = mview.getBooleanProperty("FilterByNameOrValue.meas_rule.enabled." + last, false);
		
		// is this name known?
		Integer id = (Integer) meas_name_to_index.get(mrn);
		if(id != null)
		{
		    int iid = id.intValue();

		    measurement_filter_value[iid] = mrv;
		    measurement_filter_op[iid] = mro;
		    measurement_filter_enabled[iid] = mre;

		    // System.out.println(mrn + ":" + mrv_d + "\t" + mro + "\t" + mre);

		    //m_set++;
		}
	    }
	}
    }

    // ---------------- ---------------- --------------- --------------- ------------- ------------
    // ---------------- ---------------- --------------- --------------- ------------- ------------

    private maxdView mview;
    private ExprData edata;
    //private DataPlot dplot;

    private JComboBox filter_logic_jcb;
    private JComboBox filter_ops_jcb;
    private JTextField filter_val_jtf;
    private JTextField filter_name_jtf;

    // handles the 'Enabled?' toggle buttons in the Clusters panel
    //
    class MeasFilterListener implements ItemListener
    { 
	public MeasFilterListener(int which_set) 
	{ super();
	  set = which_set;
	}
	public void itemStateChanged(ItemEvent ite) 
	{
	    setMeasurementFilterEnabled(set, ite.getStateChange() == ItemEvent.SELECTED);
	}

	private int set;
    }   
    class MeasFilterLogicListener implements ActionListener
    {
	public MeasFilterLogicListener(int which_set) 
	{ super();
	  set = which_set;
	}
	public void actionPerformed(ActionEvent e) 
	{
	    JComboBox cb = (JComboBox)e.getSource();
	    int new_val = cb.getSelectedIndex();
	    setMeasurementFilterOp(set, new_val);
	}
	private int set;
    }

    // handles the value text field in the MeasFilter controls on the Filter panel
    //
    class MeasFilterValueChangeListener implements DocumentListener 
    {
	public MeasFilterValueChangeListener(int which_set) 
	{ super();
	  set = which_set;
	}

	public void insertUpdate(DocumentEvent e)  { propagate(e); }
	public void removeUpdate(DocumentEvent e)  { propagate(e); }
	public void changedUpdate(DocumentEvent e) { propagate(e); }
	
	private void propagate(DocumentEvent e)
	{
	    try
	    {
		String d_str = e.getDocument().getText(0, e.getDocument().getLength());
		if(d_str.equals("NaN"))
		    setMeasurementFilterValue(set, Double.NaN);
		else
		    setMeasurementFilterValue(set, new Double(d_str).doubleValue());
	    }
	    catch (javax.swing.text.BadLocationException ble)
	    {
		System.out.println("wierd string....\n");
	    }
	    catch(NumberFormatException nfe)
	    {
	    }
	}
	private int set;
    }
 
    // handles the value text field in the Filter controls on the Filter panel
    //
    class FilterValueChangeListener implements DocumentListener 
    {
	public void insertUpdate(DocumentEvent e)  { propagate(e); }
	public void removeUpdate(DocumentEvent e)  { propagate(e); }
	public void changedUpdate(DocumentEvent e) { propagate(e); }

	private void propagate(DocumentEvent e)
	{
	    try
	    {
		String d_str = e.getDocument().getText(0, e.getDocument().getLength());
		if(d_str.equals("NaN"))
		    setFilterValue(Double.NaN);
		else
		    setFilterValue(new Double(d_str).doubleValue());

	    }
	    catch (javax.swing.text.BadLocationException ble)
	    {
		System.out.println("wierd string....\n");
	    }
	    catch(NumberFormatException nfe)
	    {
		//		e.getDocument().setText("[Nan]");
	    }
	}
    }
    // handles the name text field in the NameFilter controls on the Filter panel
    //
    class FilterByNameChangeListener implements DocumentListener 
    {
	public void insertUpdate(DocumentEvent e)  { propagate(e); }
	public void removeUpdate(DocumentEvent e)  { propagate(e); }
	public void changedUpdate(DocumentEvent e) { propagate(e); }

	private void propagate(DocumentEvent e)
	{
	    try
	    {
		setFilterByNameString(e.getDocument().getText(0, e.getDocument().getLength()));
	    }
	    catch (javax.swing.text.BadLocationException ble)
	    {
		System.out.println("wierd string....\n");
	    }
	}
    }

    // ---------------- ---------------- --------------- --------------- ------------- ------------
    // ---------------- ---------------- --------------- --------------- ------------- ------------

    public boolean enabled() 
    { 
	if(frame == null)
	    return false;
    
	return((m_filter_enabled_count > 0) || 
	       (filter_mode > 0) || 
	       (nt_selection != null));
    }

    public String  getName() { return "Filter By Name Or Value"; }

    public boolean filter(int spot)
    { 
	boolean result = false;

	final int n_measurements = edata.getNumMeasurements();

	// first the Filter which considers all sets of one type 
	//
	if(filter_mode > 0)
	{
	    // count how many cols of this row pass the (op,value) test
	    int valid_cols = 0;
	    
	    int possible_valid_cols = 0;
	    
	    for(int m=0; m < n_measurements; m++) 
	    {
		boolean ok = false;
		
		ExprData.Measurement meas = edata.getMeasurement(m);

		// should this column be counted as a valid
		//   to be valid it must:
		//    -  be switched on 
		//    -  match the type of the filter_mode
		//
		if(meas.getShow())
		{   
		    switch(meas.getDataType())
		    {
		    case ExprData.ExpressionAbsoluteDataType:
		    case ExprData.ExpressionRatioDataType:
			if(filter_mode == 1) 
			    ok = true;
			break;
		    case ExprData.ProbabilityDataType:
			if(filter_mode == 2) 
			    ok = true;
		    case ExprData.ErrorDataType:
			if(filter_mode == 3) 
			    ok = true;
			break;
		    }
		}
		
		if(ok)
		{ 
		    possible_valid_cols++;

		    // do the logic operation on the value in the this column
		    //
		    
		    double v = edata.eValue(m, spot);
		    
		    boolean v_is_nan = Double.isNaN(v);
		    boolean f_is_nan = Double.isNaN(filter_value);

		    // special handling for NaNs
		    //
		    if(f_is_nan || v_is_nan)
		    {
			switch(filter_op)
			{
			case 0: // only test for the '=' operator
			    if(v_is_nan == f_is_nan)
				valid_cols++;
			    break;
			case 1:
			    if(v_is_nan != f_is_nan)
				valid_cols++;
			    break;
			default:
			    valid_cols++;
			    break;
			}
			//else
			// all other operators are true w.r.t. NaNs
			//valid_cols++; 
		    }
		    else
		    {
			switch(filter_op)
			{
			case 0: // equals
			    if(v == filter_value)
				valid_cols++;
			    break;
			case 1: // !=
			    if(v != filter_value)
				valid_cols++;
			    break;
			case 2: // >=
			    if(v >= filter_value)
				valid_cols++;
			    break;
			case 3: // <=
			    if(v <= filter_value)
				valid_cols++;
			    break;
			case 4: // >
			    if(v > filter_value)
				valid_cols++;
			    break;
			case 5: // <
			    if(v < filter_value)
				valid_cols++;
			    break;
			case 6: // < > (range)
			    if(Math.abs(v) <= filter_value)
				valid_cols++;
			    break;
			case 7: // !<> (not in range)
			    if(Math.abs(v) > filter_value)
				valid_cols++;
			    break;
			}
		    }
		    
		}
	    }
	    
	    switch(filter_logic)
	    {
	    case 0: // no value
		result = (valid_cols > 0);
	        break;
	    case 1: // any value
		result = (valid_cols == 0);
	        break;
	    default: // all values
		result = (valid_cols < possible_valid_cols);
	        break;
	    }

	}

	
	boolean any_measurement_filter_on = false;
	int mf=0;
	while(mf<n_measurements) 
	{
	    if(measurement_filter_enabled[mf] == true)
	    {
		any_measurement_filter_on = true;
		break;
	    }
	    mf++;
	}

	boolean filter_by_measurement = false;

	if(any_measurement_filter_on == true)
	{
	    for(int ms=0; ms < n_measurements; ms++) 
	    {
		if(/*(measurement[ms].show) &&*/ (measurement_filter_enabled[ms] == true))
		{   
		    double v = edata.eValue(ms, spot);
		    
		    boolean v_is_nan = Double.isNaN(v);
		    boolean f_is_nan = Double.isNaN(measurement_filter_value[ms]);

		    // special handling for NaNs
		    //
		    if(f_is_nan || v_is_nan)
		    {
			// note: the sense of all operators is reversed
			//       in these tests
			//
			switch(measurement_filter_op[ms])
			{
			case 0: // only test for the '=' operator
			    if(v_is_nan != f_is_nan)  // no, they are not the same
				filter_by_measurement = true;
			    break;
			case 1: // and for '!='
			    if(v_is_nan == f_is_nan)   // yes, they are the same
				filter_by_measurement = true;
			    break;
			}
		    }
		    else
		    {
			
			// note: the sense of all operators is reversed
			//       in these tests
			//
			switch(measurement_filter_op[ms])
			{
			case 0: // equals
			    if(v != measurement_filter_value[ms])
				filter_by_measurement = true;
			    break;
			case 1: // not equals
			    if(v == measurement_filter_value[ms])
				filter_by_measurement = true;
			    break;
			case 2: // >=
			    if(v < measurement_filter_value[ms])
				filter_by_measurement = true;
			    break;
			case 3: // <=
			    if(v > measurement_filter_value[ms])
				filter_by_measurement = true;
			    break;
			case 4: // >
			    if(v <= measurement_filter_value[ms])
				filter_by_measurement = true;
			    break;
			case 5: // <
			    if(v >= measurement_filter_value[ms])
				filter_by_measurement = true;
			    break;
			case 6: // < > (range)
			    if(Math.abs(v) >  measurement_filter_value[ms])
				filter_by_measurement = true;
			    break;
			case 7: // !<> (not in range)
			    if(Math.abs(v) <=  measurement_filter_value[ms])
				filter_by_measurement = true;
			    break;
			}
		    }
		}
	    }
	}

	if(filter_by_measurement == true)
	    result = true;
	    
	boolean filter_by_name = false;

	if(nt_selection != null)
	{
	    // search all gene names associated with this spot
	    //
	    String row_name = nt_selection.getNameTag(spot);
	    
	    if(row_name == null)
	    {
		if(filter_by_name_mode == 0)
		    filter_by_name = true;
		if(filter_by_name_mode == 1)
		    filter_by_name = false;
		if(filter_by_name_mode == 2)
		    filter_by_name = true;
		if(filter_by_name_mode == 3)
		    filter_by_name = true;
	    }
	    else
	    {
		if((filter_by_name_mode == 0) && (row_name.indexOf(filter_by_name_str) == -1))
		    // doesn't contain the specified string, was supposed to, ditch it
		    filter_by_name = true;
		if((filter_by_name_mode == 1) && (row_name.indexOf(filter_by_name_str) >= 0))
		    // does indeed contain the specified string, wasn't supposed to, ditch it
		    filter_by_name = true;
		if((filter_by_name_mode == 2) && (!row_name.startsWith(filter_by_name_str)))
		    // doesn't start with the specified string, was supposed to, ditch it
		    filter_by_name = true;
		if((filter_by_name_mode == 3) && (!row_name.endsWith(filter_by_name_str)))
		    // doesn't  end with the specified string, was supposed to, ditch it
		    filter_by_name = true;
	    }
	}
	
	if(filter_by_name == true)
	    result = true;

	return result;

    }

    // the NameFilter operates on gene names
    //
    public int getFilterByNameMode()         { return filter_by_name_mode; }
    public void setFilterByNameMode(int fm) 
    { 
	filter_by_name_mode = fm; 
	startFilteredCounter(); 
	edata.notifyFilterChanged((ExprData.Filter) FilterByNameOrValue.this);
    }
    private int filter_by_name_mode = 0;

    public ExprData.NameTagSelection getFilterByNameSource()         { return nt_selection; }
    public void setFilterByNameSource(boolean nothing, ExprData.NameTagSelection nt_selection_) 
    { 
	nt_selection = nothing ? null : nt_selection_;
	startFilteredCounter();
	System.out.println("name/attr updated");
	edata.notifyFilterChanged((ExprData.Filter) FilterByNameOrValue.this);
    }

    public String getFilterByNameString()        { return filter_by_name_str; }
    public void setFilterByNameString(String s)  
    { 
	filter_by_name_str = s; 
	startFilteredCounter(); 
	if(nt_selection != null)
	    edata.notifyFilterChanged((ExprData.Filter) FilterByNameOrValue.this);
    }
    private String filter_by_name_str = "";

    // the Filter operates on all Measurements simultaneously
    //
    public int getFilterMode()        { return filter_mode; }
    public void setFilterMode(int fm) 
    {
	filter_mode = fm; 
	startFilteredCounter();
	edata.notifyFilterChanged((ExprData.Filter) FilterByNameOrValue.this);
    }

    private int filter_mode = 0;

    public int getFilterLogic()        { return filter_logic; }
    public void setFilterLogic(int fl) 
    {
	filter_logic = fl;
	startFilteredCounter();
	if(filter_mode > 0)
	    edata.notifyFilterChanged((ExprData.Filter) FilterByNameOrValue.this);
    }
    private int filter_logic = 0;

    public int getFilterOp()        { return filter_op; }
    public void setFilterOp(int fo) 
    { 
	filter_op = fo;
	startFilteredCounter();
	if(filter_mode > 0)
	     edata.notifyFilterChanged((ExprData.Filter) FilterByNameOrValue.this);
    }
    private int filter_op = 0;

    public double getFilterValue()        { return filter_value; }
    public void setFilterValue(double fv) 
    {
	filter_value = fv;
	startFilteredCounter();
	if(filter_mode > 0)
	    edata.notifyFilterChanged((ExprData.Filter) FilterByNameOrValue.this);
    }
    private double filter_value = 0.0;

    // the SetFilter operates on the values in individual Sets (i.e. columns)
    //
    public  boolean   getMeasurementFilterEnabled(int m)             { return measurement_filter_enabled[m]; }
    public  void      setMeasurementFilterEnabled(int m, boolean fo) 
    { 
	measurement_filter_enabled[m] = fo; 

	if(fo == true)
	    m_filter_enabled_count++;
	else
	    m_filter_enabled_count--;

	startFilteredCounter(); 

	edata.notifyFilterChanged((ExprData.Filter) FilterByNameOrValue.this);
    }
    private int m_filter_enabled_count = 0;

    public  int   getMeasurementFilterOp(int m)          {  return measurement_filter_op[m]; }
    public  void  setMeasurementFilterOp(int m, int fo) 
    {
	measurement_filter_op[m] = fo;
	startFilteredCounter();
	if(measurement_filter_enabled[m] == true)
	    edata.notifyFilterChanged((ExprData.Filter) FilterByNameOrValue.this);
    }
 
    //private int[] set_filter_op = null;
    
    public  double   getMeasurementFilterValue(int m)            { return measurement_filter_value[m]; }
    public  void     setMeasurementFilterValue(int m, double fv)
    { 
	measurement_filter_value[m] = fv; 
	startFilteredCounter();
	if(measurement_filter_enabled[m] == true)
	    edata.notifyFilterChanged((ExprData.Filter) FilterByNameOrValue.this);
    }

    private void rebuildMeasurementFilter()
    {
	// save the existing ordering...

	Hashtable old_order = new Hashtable();
	    
	if(measurement_filter_value != null)
	{
	    for(int m=0; m < measurement_filter_value.length; m++)
	    {
		old_order.put(measurement_filter_name[m], new Integer(m));
	    }
	}

	final int n_measurements = edata.getNumMeasurements();

	measurement_filter_name    = new String[n_measurements];

	double[]  new_measurement_filter_value   = new double[n_measurements];
	int[]     new_measurement_filter_op      = new int[n_measurements];
	boolean[] new_measurement_filter_enabled = new boolean[n_measurements];

	for(int m=0; m < n_measurements; m++)
	{
	    measurement_filter_name[m] = edata.getMeasurementName(m);
	    Integer i = (Integer) old_order.get(measurement_filter_name[m]);
	    if(i != null)
	    {
		int old_i = i.intValue();
		new_measurement_filter_value[m]   = measurement_filter_value[old_i];
		new_measurement_filter_op[m]      = measurement_filter_op[old_i];
		new_measurement_filter_enabled[m] = measurement_filter_enabled[old_i];
	    }
	}

	measurement_filter_value = new_measurement_filter_value;
	measurement_filter_op = new_measurement_filter_op;
	measurement_filter_enabled = new_measurement_filter_enabled;

    }

    private double[]  measurement_filter_value = null;
    private int[]     measurement_filter_op = null;
    private boolean[] measurement_filter_enabled = null;
    private String[]  measurement_filter_name = null;

    // ---------------- ---------------- --------------- --------------- ------------- ------------
    // ---------------- ---------------- --------------- --------------- ------------- ------------

    public void startFilteredCounter()
    {
	if(filtered_count_is_updating == false)
	{
	    filtered_count_is_updating = true;
	    filter_counter_thread  = new FilterCounter();
	    filter_counter_thread.start();
	}
	else
	   filter_counter_thread.restart(); 
    }

    // ---------------- --------------- --------------- ------------- ------------
    // filter counting thread 
    // - runs at a low priority and counts how many genes are removed by
    //   _this_ filter
    //
    private boolean       filtered_count_is_updating = false;
    private FilterCounter filter_counter_thread = null;
    private int           filtered_count = 0;

    private int  check_spot;

    public class FilterCounter extends Thread
    {
	public FilterCounter()
	{
	    super();
	    System.out.println("FilterCounter created");
	    setPriority(Thread.MIN_PRIORITY);
	    restart();
	}
	public synchronized void restart()
	{
	    filtered_count = 0;
	    check_spot = 0;
	    System.out.println("filter counting restarts...");
	}
	public synchronized void run()
	{
	    final int n_spots = edata.getNumSpots();
	    while(check_spot < n_spots)
	    {
		if(filter(check_spot))
		    filtered_count++;

		check_spot++;
		
		yield();
	    }
	    filtered_count_is_updating = false;
	    System.out.println("filter counting done, " + filtered_count + " Spots filtered");

	    String hit_pc  = mview.niceDouble((double) (filtered_count * 100) / (double) n_spots, 7, 3);
	    String miss_pc = mview.niceDouble((double) ((n_spots-filtered_count) * 100) / (double) n_spots, 7, 3);
	    
	    hit_label.setText("Fail: " + hit_pc + "%");
	    miss_label.setText("Pass: " + miss_pc + "%");

	}

	private int check_gene;
    }
    
    // ---------------- ---------------- --------------- --------------- ------------- ------------
    // ---------------- ---------------- --------------- --------------- ------------- ------------

    public void dataUpdate(ExprData.DataUpdateEvent due)
    {
    }
    public void clusterUpdate(ExprData.ClusterUpdateEvent cue)
    {
    }
    public void measurementUpdate(ExprData.MeasurementUpdateEvent mue)
    {
	switch(mue.event)
	{
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	case ExprData.OrderChanged:
	    rebuildMeasurementFilter();
	    addMeasurementFilters();
	}
    }
    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
    }
    // ---------------- ---------------- --------------- --------------- ------------- ------------
    // ---------------- ---------------- --------------- --------------- ------------- ------------

    final String[] filter_ops = { "=", "!=", ">=", "<=", ">", "<", "<>", "!<>" };

    private NameTagSelector nt_sel;
    private ExprData.NameTagSelection nt_selection;

    private JFrame frame = null;

    private JLabel hit_label, miss_label;

    private JScrollPane meas_filter_scroller;
    private JPanel meas_filter_panel;
}
