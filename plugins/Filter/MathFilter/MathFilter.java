import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.event.KeyEvent;
import java.io.*;

public class MathFilter implements Plugin, ExprData.Filter, ExprData.ExprDataObserver
{
    public final boolean debug_minor = false;
    public final boolean debug_major = false;

    String[] func_list = { "min", "max", "mean", "sum" };

    public MathFilter(maxdView mview_)
    {
	mview = mview_;
	edata = mview.getExprData();
	anlo = mview.getAnnotationLoader();
	
	parser = new Parser();

	parser.setBinaryOperatorSymbols( new String[] 
	    { 
		"*", "/", "+", "-", ">=", "<=", ">", "<", "!=", "=", "and", "or"
	    } );

	parser.setUnaryOperatorSymbols( new String[] 
	    { 
		"-", "!"
	    } );
	
	parser.setFunctionSymbols( func_list );

	parser.setIdentifiers( getIdentifiers() );
    }

    public void cleanUp()
    {
	edata.removeFilter(this);

	writeHistory();

	if(frame != null)
	{
	    mview.putProperty("MathFilter.rule", rule_jta.getText());
	    mview.putBooleanProperty("MathFilter.scale", scale_jchkb.isSelected());
	    
	    frame.setVisible(false);
	}
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
	init_rule = mview.getProperty("MathFilter.rule", "");
	//init_rule = "";

	initialise( null );
    }

    // -----------------------------------------------

    private void initialise(CommandSignal done)
    {
	if(debug_minor)
	    System.out.println("got rule....'" + init_rule + "'");

	addComponents();

	if(debug_minor)
	    System.out.println("startPlugin(): 1");

	frame.pack();
	frame.setVisible(true);

	rule_and_graph_jsp.setDividerLocation( 0.5 );
	rule_graph_vars_and_ops_jsp.setDividerLocation( 0.6 );

	if(debug_minor)
	    System.out.println("startPlugin(): 2");

	edata.addFilter(this);

	new DelayedLoaderThread( done ).start();
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
    }
    
    public void measurementUpdate(ExprData.MeasurementUpdateEvent mue)
    {
	switch(mue.event)
	{
	case ExprData.SizeChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	case ExprData.NameChanged:
	    parser.setIdentifiers( getIdentifiers() );
	    break;
	}	
    }
    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  start up
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    //
    // for some reason the plugin sometimes does not start properly if the  
    // init_rule is set from within startPlugin()
    //
    // this is assumed to be some form of race condition with the rule panel
    // being updated before it has been initialised in addComponents()
    //
    // to avoid this, the DelayedLoaderThread waits for 250ms before doing the load....
    //
    //
    //  c.f. SimpleMaths
    //

    private class DelayedLoaderThread extends Thread
    {
	public DelayedLoaderThread( CommandSignal done_ )
	{
	    done = done_;
	}
	public void run()
	{
	    try
	    {
		Thread.yield();
		Thread.sleep(250);
		Thread.yield();


		// System.out.println( "startPlugin(): loading...");
		
		if(debug_minor)
		    System.out.println("startPlugin(): 3");
		
		rule_jta.setText(init_rule);
		
		if(debug_minor)
		    System.out.println("startPlugin(): 4");

		if(done != null)
		    done.signal();

	    }
	    catch(InterruptedException ie)
	    {
	    }
	}
	private CommandSignal done;
    }

    public void stopPlugin()
    {
	cleanUp();
    }
  
    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("Math Filter", "filter", 
							"Filtering using mathematical inequalities", "",
							2, 2, 0);
	return pinf;
    }
    public PluginCommand[] getPluginCommands()
    {
	PluginCommand[] com = new PluginCommand[3];
	
	String[] args = new String[] 
	{ 
	    "filter", "string", "", "m",  "the filtering rule" 
	};
	
	com[0] = new PluginCommand("start", args);
	com[1] = new PluginCommand("set",   args);
	com[2] = new PluginCommand("stop", null);

	// com[3] = new PluginCommand("load", null);
	// com[4] = new PluginCommand("save", null);
	
	return com;
    }

    public void  runCommand(String name, String[] args, CommandSignal done) 
    { 
	if(name.equals("set"))
	{
	    String val = mview.getPluginArg("filter", args);
	    
	    if(rule_jta != null)
		rule_jta.setText( val );

	    if(done != null)
		done.signal();
	}

	if(name.equals("start"))
	{
	    String val = mview.getPluginArg("filter", args);

	    if(val != null)
		init_rule = val;

	   initialise( done );
	}

	if(name.equals("stop"))
	{
	    cleanUp();

	    if(done != null)
		done.signal();
	}
    } 
    
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private boolean         rule_has_changed = false;
    private boolean         plugin_done = false;
    private boolean         tab_completion_needed = false;
    private int             tab_completion_offset = 0;


    public void ruleHasChanged()
    {
	// System.out.println("ruleHasChanged(): 1");

	counts_are_valid = false;
	
	String error = null;
	
	String rule = rule_jta.getText();
	
	// System.out.println("ruleHasChanged(): 2");

	try
	{
	    Parser.TermNode term_node = parser.parse( rule );

	    if(debug_major)
		System.out.println( "Term node: " + describeTermNode( term_node ) );

	    compileDecisionTree( term_node );
	    
	    if(debug_major)
		System.out.println( "compiled ok...." );

	    typeCheckDecisionTree();
	    
	    if(debug_major)
		System.out.println( "typechecked ok...." );

	    edata.notifyFilterChanged((ExprData.Filter) MathFilter.this);
	}
	catch(Parser.ParseError pe)
	{
	    //System.out.println("Parser.ParseError: " + rse.toString());
	    error = pe.toString();
	}

	// System.out.println("ruleHasChanged(): 3");

	graph_panel.setErrorMsg(error);
	
	doLayoutAndDisplayGraph();

	doCount();


    }

    private void doLayoutAndDisplayGraph()
    {
	// do the initial layout with a fixed sized font
	// and then later scale the font based on this size
	
	Graphics graphic = graph_panel.getGraphics();
	
	Font f = graphic.getFont();
	
	Font font12 = new Font(f.getName(), f.getStyle(), 12);
	
	graphic.setFont(font12);
	
	font_metrics = graphic.getFontMetrics();
	font_height  = font_metrics.getAscent();
	
	positionNodes();
	
	// System.out.println("ruleHasChanged(): 4");
	
	NodeExtent ne = findExtent();
	
	int xr = (ne.maxx - ne.minx);
	int yr = (ne.maxy - ne.miny) + node_h + font_height;
	
	//System.out.println("fullsized extent is " + xr + "x" + yr);
	//System.out.println("jsp is " + graph_jsp.getWidth() + "x" + graph_jsp.getHeight());
	
	// add a bit of a border (5% on each side)
	
	// xr = (xr * 110) / 100;
	// yr = (yr * 110) / 100;
	
	int aw = graph_jsp.getWidth()-5;
	int ah = graph_jsp.getHeight()-5;
	
	if(scale_jchkb.isSelected())
	{
	    Dimension available_space = new Dimension( aw, ah );
	    
	    graph_panel.setPreferredSize(available_space);
	    graph_panel.setMinimumSize(available_space);
	    
	    // n2s = node-to-screen scale, <1.0 means the diagram will by shrunk to fit into a smaller space
	    
	    n2s_sx = (double) aw / (double) xr;
	    n2s_sy = (double) ah / (double) yr;
	    
	    
	}
	else
	{
	    // setPreferredSize(new Dimension( xr, xy ));
	    graph_panel.setPreferredSize(new Dimension( xr, yr ));
	    graph_panel.setMinimumSize(new Dimension( xr, yr ));

	    n2s_sx = n2s_sy = 1.0;
	}
	
	if(n2s_sx > 1.0)
	    n2s_sx = 1.0;
	if(n2s_sy > 1.0)
	    n2s_sy = 1.0;
	
	// preserve the aspect ratio
	//
	if(n2s_sx < n2s_sy)
	    n2s_sy = n2s_sx;
	else
	    n2s_sx = n2s_sy;
	
	//n2s_tx = (int)( (((double) getWidth()  - (double) xr) * n2s_sx) / 2.0);
	//n2s_ty = (int)( (((double) getHeight() - (double) yr) * n2s_sy) / 2.0);
	
	// need to allow a bit of extra space above and below for the counts
	
	n2s_tx = -ne.minx;
	n2s_ty = -(ne.miny - node_h); // extra height for the top-most joining line (so the pass-count is visible)
		
	//n2s_tx += (getWidth()  - n2slX(xr)) / 2;
	//n2s_ty += (getHeight() - n2slY(yr)) / 2;
	
	n2s_cx = 0;
	n2s_cy = 0;
	
	double sxr = (double) xr * n2s_sx; 
	double syr = (double) yr * n2s_sy; 
	
	// and (if small enough) center the diagram in the available space
	
	if(sxr < aw)
	    n2s_cx += (int) ((aw - sxr) / 2);
	
	if(syr < ah)
	    n2s_cy += (int) ((ah - syr) / 2);
	
	try
	{
	    graph_panel.updateUI();
	    graph_jsp.updateUI();
	}
	catch(NullPointerException npe)
	{
	}

	// System.out.println("panel is " + graph_panel.getWidth() + "x" + graph_panel.getHeight());
	
	// graph_panel.repaint();

    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  filter implementation
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
	
    // should return TRUE  if this spot is not to be displayed
    //           and FALSE if the spot is to be displayed
    //
    public boolean filter(int spot_index)
    {
	if(root != null)
	{
	    OpVal opv = evaluateNode(root, spot_index);
	    if(opv != null)
		return !opv.bv;
	}
		
	return false;
    }

    public boolean enabled()
    { 
	return (root != null);
    }

    public String  getName() { return "Math Filter"; }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  gooey
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    protected JList vars_list = null;
    protected DefaultListModel vars_list_model = null;

    protected Vector var_string = null;

    private boolean isSafe(String s)
    {
	if(s.indexOf(' ') >= 0)
	    return false;
	if(s.indexOf('(') >= 0)
	    return false;
	if(s.indexOf(')') >= 0)
	    return false;
	return true;
    }

    private class MeasSpotAttr 
    {
	public int meas;
	public int attr;

	/*
	public int type;

	// also keep a pointer to the data array for the SpotAttr

	// (keep one of each type to avoid run-time type checking during casts)
	
        double[]  double_data;
	int[]     int_data;
	char[]    char_data;
	String[]  text_data;
	*/

	public MeasSpotAttr(int m, int a) 
	{
	    meas = m; attr = a;
	}
	
    }
    
    private void buildList()
    {
	final boolean arrange_list_by_measurement = mview.getBooleanProperty("MathFilter.arrange_list_by_measurment", true );

	vars_list_model = new DefaultListModel();
	var_string = new Vector();

	// build the hastable mapping Measurement.names to Measurement indexes
	// (used to detect names in the rule)
	//
	meas_name_to_id_ht = new Hashtable();

	// and also one for mapping Measurement.SpotAttr.names to a pair of indexes
	//
	meas_spot_attr_to_id_ht = new Hashtable();

	if( arrange_list_by_measurement )
	{
	    for(int mi=0; mi < edata.getNumMeasurements(); mi++)
	    {
		final int m_index = edata.getMeasurementAtIndex(mi);
		
		ExprData.Measurement ms = edata.getMeasurement( m_index );
		
		meas_name_to_id_ht.put( ms.getName(), new Integer( m_index ) );
		
		vars_list_model.addElement(ms.getName());
		
		if(isSafe(ms.getName()))
		    var_string.addElement(ms.getName());
		else
		    var_string.addElement("\"" + ms.getName() + "\"");
		
		for(int sa=0; sa < ms.getNumSpotAttributes(); sa++)
		{
		    String full_name = ms.getName() + "." + ms.getSpotAttributeName(sa);
		    
		    vars_list_model.addElement("  " + ms.getSpotAttributeName(sa));
		    
		    meas_spot_attr_to_id_ht.put(full_name, new MeasSpotAttr( m_index, sa));
		    
		    if(debug_major)
			System.out.println(full_name + " is type " + ms.getSpotAttributeDataTypeCode(sa));
		    
		    //System.out.println(full_name + " is " + m + "." + sa);
		    
		    if(isSafe(full_name))
			var_string.addElement(full_name);
		    else
			var_string.addElement("\"" + full_name + "\"");
		}
	    }
	}
	else
	{
	    java.util.HashSet attr_names = new java.util.HashSet();

	    // add the measurement names
	    for(int mi=0; mi < edata.getNumMeasurements(); mi++)
	    {
		ExprData.Measurement ms = edata.getMeasurement( edata.getMeasurementAtIndex(mi) );
		     
		meas_name_to_id_ht.put(ms.getName(), new Integer( edata.getMeasurementAtIndex( mi )));
		
		vars_list_model.addElement(ms.getName());
		
		if(isSafe(ms.getName()))
		    var_string.addElement(ms.getName());
		else
		    var_string.addElement("\"" + ms.getName() + "\"");
	
	    }
	    

	    // get the full set of SpotAttr names....
	    for(int mi=0; mi < edata.getNumMeasurements(); mi++)
	    {
		ExprData.Measurement ms = edata.getMeasurement(mi);
		for(int sa=0; sa < ms.getNumSpotAttributes(); sa++)
		    attr_names.add( ms.getSpotAttributeName( sa ) );
	    }
		

	    // add each of spot attrs
	    for (java.util.Iterator it = attr_names.iterator() ; it.hasNext() ;) 
	    {
		String attr_name = (String) it.next();
		
		 for(int mi=0; mi < edata.getNumMeasurements(); mi++)
		 {
		     ExprData.Measurement ms = edata.getMeasurement( edata.getMeasurementAtIndex(mi) );

		     for(int sa=0; sa < ms.getNumSpotAttributes(); sa++)
		     {
			 if( ms.getSpotAttributeName( sa ).equals( attr_name ) )
			 {
			     String full_name = ms.getName() + "." + ms.getSpotAttributeName(sa);
			     
			     vars_list_model.addElement( full_name );
			     
			     meas_spot_attr_to_id_ht.put(full_name, new MeasSpotAttr( edata.getMeasurementAtIndex( mi ), sa));
			     
			     if(debug_major)
				 System.out.println(full_name + " is type " + ms.getSpotAttributeDataTypeCode(sa));
			     
			     //System.out.println(full_name + " is " + m + "." + sa);
			     
			     if(isSafe(full_name))
				 var_string.addElement(full_name);
			     else
				 var_string.addElement("\"" + full_name + "\"");
			 }
		     }
		 }
	    }
	    
	}

	vars_list.setModel(vars_list_model);
    }

    //
    // generate a String[] containing all of the current Measurement names and SpotAttribute names
    //
    private String[] getIdentifiers()
    {
	Vector ident_v = new Vector();
	for( int mi = 0; mi < edata.getNumMeasurements(); mi++ )
	{
	    ExprData.Measurement ms = edata.getMeasurement(mi);
	    
	    ident_v.add( ms.getName() );
	    
	    for( int sa = 0; sa < ms.getNumSpotAttributes(); sa++ )
	    {
		ident_v.add( ms.getName() + "." + ms.getSpotAttributeName(sa) );
	    }
	}
	return (String[]) ident_v.toArray( new String[ ident_v.size() ] );
    }


    private void insertText(String s)
    {
	//int cp = rule_jta.getCaretPosition();

	//rule_jta.insert(s, cp);

	rule_jta.replaceSelection(s);

	rule_jta.requestFocus();
    }

    private void replaceText( String s )
    {
	rule_jta.setText( s );
	rule_jta.requestFocus();
    }

    private void tabComplete()
    {
/*
	String result = null;

	// use the current caret pos to work out where to do the insert....

	int ins_pt = rule_jta.getCaretPosition();

	// find the token containing the insert pos
	//
	if(debug_minor)
	    System.out.println("insert pt is " + ins_pt);
	
	int tok_num=0;
	while(((tok_num+1) < token_a.length) && (token_a[(tok_num+1)].str_pos < ins_pt))
	    tok_num++;
	
	if(tok_num < token_a.length)
	{
	    if(debug_minor)
		System.out.println("insert pt in token " + tok_num + " '" + token_a[tok_num].t_str + "'");
	    
	    String t_str = token_a[tok_num].t_str;
	    
	    if(debug_minor)
		System.out.println("TAB completing '" + t_str + "'");
	
	    int m_len = t_str.length();

	    // first establish which words it might be....
	    Vector candidates = new Vector();

	    for (Enumeration keys = meas_name_to_id_ht.keys() ; keys.hasMoreElements() ;) 
	    {
		String full_name = (String) keys.nextElement();
		if(full_name.length() >= m_len)
		{
		    String t_match   = full_name.substring(0, m_len);
		    
		    if(t_match.equals(t_str))
		    {
			if(debug_major)
			    System.out.println("possibly " + full_name + " ? ");
			candidates.addElement(full_name);
		    }
		}
	    }
	    for (Enumeration keys = meas_spot_attr_to_id_ht.keys() ; keys.hasMoreElements() ;) 
	    {
		String full_name = (String) keys.nextElement();
		if(full_name.length() >= m_len)
		{
		    String t_match   = full_name.substring(0, m_len);
		    
		    if(t_match.equals(t_str))
		    {
			if(debug_major)
			    System.out.println("possibly " + full_name + " ? ");
			candidates.addElement(full_name);
		    }
		}
	    }
	    
	    if(candidates.size() == 1)
	    {
		// exactly one match, it's a winner
		
		result = ((String) candidates.elementAt(0)).substring(m_len);
	    }

	    if(candidates.size() > 1)
	    {
		// find the longest common sequence of chars that
		// occurs at the start of all of the candidates

		// as it has to be at least as long as the length
		// of the target string we can skip the first 
		// (m_len-1) chars

		int longest_prefix = m_len-1;
		
		boolean is_common = true;
		
		while(is_common)
		{
		    if(debug_major)
			System.out.println("longest prefix: " + ((String)candidates.elementAt(0)).substring(0, longest_prefix));

		    longest_prefix++;

		    if(longest_prefix < ((String)candidates.elementAt(0)).length())
		    {
			char common_char = ((String)candidates.elementAt(0)).charAt(longest_prefix);
			
			for(int cc=1; cc < candidates.size(); cc++)
			{
			    String cs = (String)candidates.elementAt(cc);
			    
			    if((cs.length() <= longest_prefix) || (cs.charAt(longest_prefix) != common_char))
			    {
				is_common = false;
				break;
			    }
			}
		    }
		    else
		    {
			is_common = false;
		    }
    
		}
		result = ((String)candidates.elementAt(0)).substring(m_len, longest_prefix);
		Toolkit.getDefaultToolkit().beep();
	    }
	}
	if(debug_minor)
	    System.out.println("best match: '" + result + "'");

	if(result == null)
	    Toolkit.getDefaultToolkit().beep();

	// now remove the TAB char and replace it with the completion string
	//
	if(debug_major)
	    System.out.println("insert pt is " + ins_pt);

	// this will generate a document change event.....
	if(result != null)
	    rule_jta.insert(result, ins_pt);
*/
    }


    private void addComponents()
    {
	frame = new JFrame("Math Filter");
	
	mview.decorateFrame(frame);

	frame.addWindowListener(new WindowAdapter() 
			  {
			      public void windowClosing(WindowEvent e)
			      {
				  cleanUp();
			      }
			  });


	GridBagLayout gridbag = new GridBagLayout();
	JPanel panel = new JPanel();
	panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	panel.setLayout(gridbag);
	//panel.setPreferredSize(new Dimension(350, 200));

	int line = 0;

	rule_and_graph_jsp = new JSplitPane( JSplitPane.VERTICAL_SPLIT );

	{
	    
	    // rule_and_graph.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));

	    // GridBagLayout rg_gridbag = new GridBagLayout();
	    // rule_and_graph.setLayout(rg_gridbag);
	    
	    {
		rule_jta = new JTextArea(80, 5);
		rule_jta.getDocument().addDocumentListener(new CustomChangeListener());
		JScrollPane jsp = new JScrollPane(rule_jta);
		jsp.setPreferredSize(new Dimension(300, 50));

		//GridBagConstraints c = new GridBagConstraints();
		//c.gridx = 0;
		//c.gridy = 0;
		//c.fill = GridBagConstraints.BOTH;
		//c.weighty = 1.0;
		//c.weightx = 3.0;
		
		Keymap km = rule_jta.getKeymap();
		//km.addActionForKeyStroke(KeyStroke.getKeyStrokeForEvent(KeyEvent.VK_TAB), new TabAction());
		km.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_TAB,0), new TabAction());
		rule_jta.setKeymap(km);

		//rg_gridbag.setConstraints(jsp, c);

		rule_and_graph_jsp.setTopComponent( jsp );
	    }
	    {
		graph_panel = new GraphPanel();

		graph_panel.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));

		graph_panel.setPreferredSize(new Dimension(300, 150));

		graph_jsp = new JScrollPane(graph_panel);

		//GridBagConstraints c = new GridBagConstraints();
		//c.gridx = 0;
		//c.gridy = 1;
		//c.fill = GridBagConstraints.BOTH;
		//c.weighty = 2.0;
		//c.weightx = 3.0;
		
		//rg_gridbag.setConstraints(graph_jsp, c);
		//rule_and_graph.add(graph_jsp);

		rule_and_graph_jsp.setBottomComponent( graph_jsp );

	    }
	    
/*
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.fill = GridBagConstraints.BOTH;
	    c.weighty = 4.0;
	    c.weightx = 4.0;
	    
	    gridbag.setConstraints(rule_and_graph, c);
	    panel.add(rule_and_graph);
*/

	}

	{
	    JPanel vars_and_ops = new JPanel();
	    GridBagLayout vo_gridbag = new GridBagLayout();
	    vars_and_ops.setLayout(vo_gridbag);

	    vars_and_ops.setBorder(BorderFactory.createEmptyBorder(0,4,0,0));

	    {
		JPanel order_panel = new JPanel();
		GridBagLayout order_gridbag = new GridBagLayout();
		order_panel.setLayout( order_gridbag );
		
		order_panel.setBorder(BorderFactory.createEmptyBorder(0,0,4,0));

		JLabel label = new JLabel( "Arrange by: " );
		label.setFont( mview.getSmallFont() );
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		order_gridbag.setConstraints( label, c );
		order_panel.add( label );

		ButtonGroup bg = new ButtonGroup();

		boolean arrange_list_by_measurement = mview.getBooleanProperty("MathFilter.arrange_list_by_measurment", true );

		final JRadioButton meas_jrb = new JRadioButton("Measurement");

		meas_jrb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    mview.putBooleanProperty("MathFilter.arrange_list_by_measurment", meas_jrb.isSelected() );
			    buildList();
			}
		    });

		meas_jrb.setSelected( arrange_list_by_measurement );
		bg.add( meas_jrb );
		meas_jrb.setFont( mview.getSmallFont() );
		c = new GridBagConstraints();
	        c.gridx = 1;
		c.anchor = GridBagConstraints.WEST;
		order_gridbag.setConstraints( meas_jrb, c );
		order_panel.add( meas_jrb );

		final JRadioButton attr_jrb = new JRadioButton("Spot Attribute");

		attr_jrb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    mview.putBooleanProperty("MathFilter.arrange_list_by_measurment", ( attr_jrb.isSelected() == false ) );
			    buildList();
			}
		    });

		attr_jrb.setSelected( ! arrange_list_by_measurement );
		bg.add( attr_jrb );
		attr_jrb.setFont( mview.getSmallFont() );
		c = new GridBagConstraints();
	        c.gridx = 2;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		order_gridbag.setConstraints( attr_jrb, c );
		order_panel.add( attr_jrb );


		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
	        c.weightx = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;

		vo_gridbag.setConstraints(order_panel, c);
		vars_and_ops.add(order_panel);
	    }

	    {
		vars_list = new JList();
		
		vars_list.addListSelectionListener(new ListSelectionListener()
		    {
			public void valueChanged(ListSelectionEvent e) 
			{
			    int index = vars_list.getSelectedIndex();
			    if(index >= 0)
			    {
				insertText((String)var_string.elementAt(index));
				vars_list.clearSelection();
			    }
			}
		    });


		buildList();
		
		JScrollPane jsp = new JScrollPane(vars_list);
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 4.0;
	        c.weightx = 1.0;
		
		vo_gridbag.setConstraints(jsp, c);
		vars_and_ops.add(jsp);
	    }




	    {
		JTabbedPane jtp = new JTabbedPane();

		jtp.addTab( "Operators", makeOpsPanel() );
		jtp.addTab( "Functions", makeFuncsPanel() );
		jtp.addTab( "History",   makeHistoryPanel() );

		// --------------------------------

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 2;
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 0.1;
		c.weightx = 10.0;
		
		vo_gridbag.setConstraints( jtp, c);
		vars_and_ops.add( jtp );
	    }	    

	    vars_and_ops.setPreferredSize(new Dimension(300, 400));


	    rule_graph_vars_and_ops_jsp = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT );

	    rule_graph_vars_and_ops_jsp.setLeftComponent( rule_and_graph_jsp );

	    rule_graph_vars_and_ops_jsp.setRightComponent( vars_and_ops );

	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridy = line;
	    c.fill = GridBagConstraints.BOTH;
	    c.weighty = 4.0;
	    c.weightx = 10.0;
	    
	    gridbag.setConstraints( rule_graph_vars_and_ops_jsp, c);
	    panel.add( rule_graph_vars_and_ops_jsp );
	}
	
	line++;

	
	{
	    JPanel wrapper = new JPanel();
	    wrapper.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrapper.setLayout(w_gridbag);
	    
	    {
		scale_jchkb = new JCheckBox("Scale view to fit window");
		scale_jchkb.setSelected(mview.getBooleanProperty("MathFilter.scale", true));
		scale_jchkb.addActionListener(new ActionListener() 
		    { public void actionPerformed(ActionEvent e) { ruleHasChanged(); } } );
		
		scale_jchkb.setFont(mview.getSmallFont());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		w_gridbag.setConstraints(scale_jchkb, c);
		wrapper.add(scale_jchkb);
	    }
	    
	    /*
	    {
		JButton button = new JButton("Count");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    doCount();
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		//c.weighty = c.weightx = 1.0;
		//c.anchor = GridBagConstraints.EAST;
		w_gridbag.setConstraints(button, c);
	    }
	    */
	    
	    
	    {
		JButton button = new JButton("Clear");
		button.setToolTipText("Remove the current expression");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    replaceText("");
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		//c.weighty = c.weightx = 1.0;
		//c.anchor = GridBagConstraints.EAST
		w_gridbag.setConstraints(button, c);
	    }
	    
	    addFiller( wrapper, w_gridbag, 0, 2 );

	    {
		JButton button = new JButton("Load");
		button.setToolTipText("Load an expression from a file");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    readRule( );
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 3;
		c.gridy = 0;
		//c.weighty = c.weightx = 1.0;
		//c.anchor = GridBagConstraints.EAST
		w_gridbag.setConstraints(button, c);
	    }
	    
	    addFiller( wrapper, w_gridbag, 0, 4 );

	    {
		JButton button = new JButton("Save");
		button.setToolTipText("Save the current expression to a file");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    writeRule( );
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 5;
		c.gridy = 0;
		//c.weighty = c.weightx = 1.0;
		//c.anchor = GridBagConstraints.EAST
		w_gridbag.setConstraints(button, c);
	    }

	    addFiller( wrapper, w_gridbag, 0, 6 );

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
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 7;
		c.gridy = 0;
		c.anchor = GridBagConstraints.EAST;
		c.weightx = 1.0;
		//c.anchor = GridBagConstraints.EAST;
		w_gridbag.setConstraints(button, c);
	    }

	    addFiller( wrapper, w_gridbag, 0, 8 );

	    {
		JButton button = new JButton("Help");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    mview.getPluginHelpTopic("MathFilter", "MathFilter");
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 9;
		c.gridy = 0;
		c.anchor = GridBagConstraints.EAST;
		//c.weighty = c.weightx = 1.0;
		w_gridbag.setConstraints(button, c);
	    }


	    panel.add(wrapper);
	    GridBagConstraints c = new GridBagConstraints();
	    //c.gridx = 0;
	    c.gridy = line;
	    c.weightx = 10.0;
	    //c.gridwidth = 2;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(wrapper, c);
	    
	}

	readHistory();

	frame.getContentPane().add(panel);
    }

    static Dimension tinyfillsize = new Dimension(4,4);
    static Dimension fillsize = new Dimension(10,10);

    private void addTinyFiller( JPanel panel, GridBagLayout gbl, int row, int col )
    {
	Box.Filler filler = new Box.Filler( tinyfillsize, tinyfillsize, tinyfillsize );
	GridBagConstraints c = new GridBagConstraints();
	c.gridx = col;
	c.gridy = row;
	gbl.setConstraints(filler, c);
	panel.add(filler);
    }
    private void addFiller( JPanel panel, GridBagLayout gbl, int row, int col )
    {
	Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
	GridBagConstraints c = new GridBagConstraints();
	c.gridx = col;
	c.gridy = row;
	gbl.setConstraints(filler, c);
	panel.add(filler);
    }
    

    private JPanel makeOpsPanel()
    {
	JPanel ops_panel = new JPanel();
	
	GridBagLayout ops_gridbag = new GridBagLayout();
	ops_panel.setLayout(ops_gridbag);
	
	Font f = ops_panel.getFont();
	Font small_font = new Font(f.getName(), f.getStyle(), f.getSize() - 2);
	
	JButton jb = new JButton("<");
	jb.setFont(small_font);
	jb.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { insertText(" < "); } } );
	GridBagConstraints c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	c.weightx = 2.0;
	c.fill = GridBagConstraints.BOTH;
	ops_gridbag.setConstraints(jb, c);
	ops_panel.add(jb);
	
	addTinyFiller( ops_panel, ops_gridbag, 0, 1 );

	jb = new JButton("=");
	jb.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { insertText(" = "); } } );
	jb.setFont(small_font);
	c = new GridBagConstraints();
	c.gridx = 2;
	c.gridy = 0;
	c.weightx = 2.0;
	c.fill = GridBagConstraints.BOTH;
	ops_gridbag.setConstraints(jb, c);
	ops_panel.add(jb);
	
	addTinyFiller( ops_panel, ops_gridbag, 0, 3 );

	jb = new JButton("!=");
	jb.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { insertText(" != "); } } );
	jb.setFont(small_font);
	c = new GridBagConstraints();
	c.gridx = 4;
	c.gridy = 0;
	c.weightx = 2.0;
	c.fill = GridBagConstraints.BOTH;
	ops_gridbag.setConstraints(jb, c);
	ops_panel.add(jb);
	
	addTinyFiller( ops_panel, ops_gridbag, 0, 5 );

	jb = new JButton("/");
	jb.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { insertText(" / "); } } );
	jb.setFont(small_font);
	c = new GridBagConstraints();
	c.gridx = 6;
	c.gridy = 0;
	c.weightx = 2.0;
	c.fill = GridBagConstraints.BOTH;
	ops_gridbag.setConstraints(jb, c);
	ops_panel.add(jb);
	
	addTinyFiller( ops_panel, ops_gridbag, 1, 0 );

	jb = new JButton(">");
	jb.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { insertText(" > "); } } );
	jb.setFont(small_font);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 2;
	c.fill = GridBagConstraints.BOTH;
	ops_gridbag.setConstraints(jb, c);
	ops_panel.add(jb);
	
	jb = new JButton("(");
	jb.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { insertText(" ( "); } } );
	jb.setFont(small_font);
	c = new GridBagConstraints();
	c.gridx = 2;
	c.gridy = 2;
	c.fill = GridBagConstraints.BOTH;
	ops_gridbag.setConstraints(jb, c);
	ops_panel.add(jb);
	
	jb = new JButton(")");
	jb.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { insertText(" ) "); } } );
	jb.setFont(small_font);
	c = new GridBagConstraints();
	c.gridx = 4;
	c.gridy = 2;
	c.fill = GridBagConstraints.BOTH;
	ops_gridbag.setConstraints(jb, c);
	ops_panel.add(jb);
	
	jb = new JButton("*");
	jb.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { insertText(" * "); } } );
	jb.setFont(small_font);
	c = new GridBagConstraints();
	c.gridx = 6;
	c.gridy = 2;
	c.fill = GridBagConstraints.BOTH;
	ops_gridbag.setConstraints(jb, c);
	ops_panel.add(jb);
	
	// -------------
	
	addTinyFiller( ops_panel, ops_gridbag, 3, 0 );

	jb = new JButton("<=");
	jb.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { insertText(" <= "); } } );
	jb.setFont(small_font);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 4;
	c.fill = GridBagConstraints.BOTH;
	ops_gridbag.setConstraints(jb, c);
	ops_panel.add(jb);
	
	jb = new JButton("and");
	jb.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { insertText(" and "); } } );
	jb.setFont(small_font);
	c = new GridBagConstraints();
	c.gridx = 2;
	c.gridy = 4;
	c.fill = GridBagConstraints.BOTH;
	c.gridwidth=3;
	ops_gridbag.setConstraints(jb, c);
	ops_panel.add(jb);
	
	jb = new JButton("-");
	jb.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { insertText(" - "); } } );
	jb.setFont(small_font);
	c = new GridBagConstraints();
	c.gridx = 6;
	c.gridy = 4;
	c.fill = GridBagConstraints.BOTH;
	ops_gridbag.setConstraints(jb, c);
	ops_panel.add(jb);
	
	addTinyFiller( ops_panel, ops_gridbag, 5, 0 );

	jb = new JButton(">=");
	jb.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { insertText(" >= "); } } );
	jb.setFont(small_font);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 6;
	c.fill = GridBagConstraints.BOTH;
	ops_gridbag.setConstraints(jb, c);
	ops_panel.add(jb);
	
	jb = new JButton("or");
	jb.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { insertText(" or "); } } );
	jb.setFont(small_font);
	c = new GridBagConstraints();
	c.gridx = 2;
	c.gridy = 6;
	c.fill = GridBagConstraints.BOTH;
	c.gridwidth=3;
	ops_gridbag.setConstraints(jb, c);
	ops_panel.add(jb);
	
	jb = new JButton("+");
	jb.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { insertText(" + "); } } );
	jb.setFont(small_font);
	c = new GridBagConstraints();
	c.gridx = 6;
	c.gridy = 6;
	c.fill = GridBagConstraints.BOTH;
	ops_gridbag.setConstraints(jb, c);
	ops_panel.add(jb);

	return ops_panel;
    }

    private JPanel makeFuncsPanel()
    {
	JPanel panel = new JPanel();

	GridBagLayout gridbag = new GridBagLayout();
	panel.setLayout( gridbag );
	

	Font font = panel.getFont();
	Font small_font = new Font(font.getName(), font.getStyle(), font.getSize() - 2);
	
	int row = 0;
	int col = 0;

	for( int fn=0; fn < func_list.length; fn++ )
	{
	    final String fn_str = func_list[ fn ];

	    JButton jb = new JButton( func_list[ fn ] );
	    jb.setFont(small_font);

	    jb.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    { 
			insertText( fn_str ); 
		    } 
		} );

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = col;
	    c.gridy = row;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.weightx = 1.0;
	    gridbag.setConstraints( jb, c);
	    panel.add( jb );

	    row++;

	    addTinyFiller( panel, gridbag, row, col );

	    row++;
	}
	
	return panel;
    }


    private JPanel makeHistoryPanel()
    {
	JPanel panel = new JPanel();
	GridBagConstraints c;

	GridBagLayout gridbag = new GridBagLayout();
	panel.setLayout( gridbag );
	
	history_list = new JList();
	history_list.addListSelectionListener(new ListSelectionListener()
	    {
		public void valueChanged(ListSelectionEvent e) 
		{
		    String str = (String) history_list.getSelectedValue();
		    if(( str == null ) || ( str.length() == 0 ))
		       return;
		    replaceText( str );
		    vars_list.clearSelection();
		}
	    });

	JScrollPane jsp = new JScrollPane( history_list );
	c = new GridBagConstraints();
	c.gridy = 0;
	c.gridwidth = 3;
	c.fill = GridBagConstraints.BOTH;
	c.weighty = 9.0;
	c.weightx = 9.0;
	gridbag.setConstraints( jsp, c);
	panel.add( jsp );
	
	history_list.setModel( new DefaultListModel() );

	addTinyFiller( panel, gridbag, 1, 0 );

	JButton jb = new JButton( "Add current" );
	jb.setToolTipText("Add the current expression to the history list");
	jb.addActionListener( new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{ 
		    DefaultListModel lm = (DefaultListModel) history_list.getModel();
		    if( rule_jta.getText().trim().length() > 0 )
			lm.addElement( rule_jta.getText().trim() );
		    
		} 
	    } );

	jb.setFont( mview.getSmallFont() );
	c = new GridBagConstraints();
	c.gridy = 2;
	c.gridx = 0;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(jb, c);
	panel.add(jb);

	jb = new JButton( "Remove selected" );
	jb.addActionListener( new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{ 
		    int index = history_list.getSelectedIndex();
		    if( index >= 0 )
		    {
			DefaultListModel lm = (DefaultListModel) history_list.getModel();
			lm.remove( index );
		    }		    
		} 
	    } );
	jb.setToolTipText("Remove the selected item from the history list");
	jb.setFont( mview.getSmallFont() );
	c = new GridBagConstraints();
	c.gridy = 2;
	c.gridx = 2;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.EAST;
	gridbag.setConstraints(jb, c);
	panel.add(jb);

	
	return panel;
    }

    

    // handles any changes in text fields
    //
    class CustomChangeListener implements DocumentListener 
    {
	public void insertUpdate(DocumentEvent e)  
	{ 
	    propagate(e); 
	}

	public void removeUpdate(DocumentEvent e)  { propagate(e); }
	public void changedUpdate(DocumentEvent e) { propagate(e); }

	private void propagate(DocumentEvent e)
	{
	    ruleHasChanged();
	}
    }

    public class TabAction extends AbstractAction
    {
	public void actionPerformed(ActionEvent ae) 
	{
	    //System.out.println("TAB");
	    tabComplete();
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  read/write of history
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private void readHistory()
    {
	//System.out.println("readHistory(): starts... " );

	try
	{
	    File file = new File( mview.getConfigDirectory() + "math-filter-history.txt" );

	    BufferedReader reader = new BufferedReader(new FileReader(file));

	    DefaultListModel dlm = new DefaultListModel();
	    
	    String str = reader.readLine();

	    while(str != null)
	    {
		//System.out.println("readHistory(): " + str );

		dlm.addElement( str );

		str = reader.readLine();
	    }

	    history_list.setModel( dlm  );
	}
	catch( java.io.IOException ioe )
	{
	    
	}

	//System.out.println("readHistory(): ends... " );
    }

    private void writeHistory()
    {
	try
	{
	    File file = new File( mview.getConfigDirectory() + "math-filter-history.txt" );

	    PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));

	    DefaultListModel lm = (DefaultListModel) history_list.getModel();

	    for(int l=0; l < lm.getSize(); l++ )
	    {
		writer.write( stripCarriageReturnsFrom( (String) lm.elementAt( l ) ) );
		writer.write( "\n" );
	    }
	    
	    writer.flush();
	    writer.close();

	}
	catch( java.io.IOException ioe )
	{
	    
	}
    }

    private String stripCarriageReturnsFrom( String in )
    {
	final StringBuffer out = new StringBuffer();

	if( in != null )
	{
	    for( int c=0; c < in.length(); c++ )
	    {
		if( in.charAt( c ) == '\n' )
		    out.append( ' ' );
		else
		    out.append( in.charAt( c ) );
	    }
	}
	return out.toString();
    }
    
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  read/write of rules
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private void readRule( )
    {
	JFileChooser fc = mview.getFileChooser();
	if(fc.showOpenDialog( frame ) == JFileChooser.APPROVE_OPTION)
	{
	    String rule_text = "";

	    try
	    {
		File file = new File( fc.getSelectedFile().getPath() );
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String str = reader.readLine();
		while(str != null)
		{
		    rule_text += str + "\n";
		    str = reader.readLine();
		}
	    }
	    catch( java.io.IOException ioe )
	    {
		mview.alertMessage("Unable to write this rule to a file\n\n" + ioe);
	    }

	    rule_jta.setText( rule_text );
	    
	    edata.notifyFilterChanged((ExprData.Filter) MathFilter.this);
	}
    }

    private void writeRule( )
    {
	JFileChooser fc = mview.getFileChooser();

	int ret_val =  fc.showSaveDialog( frame ); 

	if(ret_val == JFileChooser.APPROVE_OPTION) 
	{
	    String rule_text =  rule_jta.getText(  );

	    try
	    {
		File file = new File( fc.getSelectedFile().getPath() );
		PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
		
		writer.write(rule_text);
		
		writer.flush();
		writer.close();
	    }
	    catch( java.io.IOException ioe )
	    {
		mview.alertMessage("Unable to write this rule to a file\n\n" + ioe);
	    }

	}
    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  parser
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    // ----------------------------
    // 
    // the tree is constructed from Operators and Operands
    //
    // ----------------------------
    //
    //  grammar:
    //
    //
    //    Filter      :=   Operand Operator Operand
    // 
    // 
    //    Operator    :=   BoolOp  |  RelOp  |  MathOp
    //	  
    //    BoolOp      :=   'and'  |  'or'
    //	  
    //    RelOp       :=   '<'  |  '>'  |  '>='  |  '<='  |  '='  |  '!='  
    //	  
    //    MathOp      :=   '+'  |  '-'  |  '*'  |  '/'
    //	  
    //    Operand     :=   Constant  |  Variable |  Operand  Operator  Operand
    // 
    //    Constant    :=   DoubleConstant  |  IntConstant  |  CharConstant  |  TextConstant
    // 
    //    Variable    :=   MeasurementName  |  MeasurementSpotAttributeName   
    //
    //
    //    presidence:   BoolOp ... RelOp ... MathOp ... 
    //
    //    type checking and some additional constraints are also applied,
    //    eg the top-level Operator in a Filter must be a BoolOp...
    //
    // ----------------------------
    



    public class Operator     // models BoolOps, RelOps and MathOps
    {
	int x, y;
	int w, h;
	
	int op_t;             // operator type
	
	boolean is_binary;

	Operand[]  operand;   //  one for each branch, currently all Operators have 2 branches
	Operator[] op_node;   //  (each branch is either an operand or an op_node)

	int counter;          //  used to calculating stop/go percents
	
	public Operator( boolean is_binary_ )
	{
	    op_t = OpNone;
	    operand = new Operand[2];
	    op_node = new Operator[2];
	    is_binary = is_binary_;
	    makeEmpty();
	}

	public void makeEmpty()
	{
	    op_t = OpNone;
	    operand[0] = operand[1] = null;
	    op_node[0] = op_node[1] = null;
	}	

	public void copyFrom(Operator opn)
	{
	    op_t = opn.op_t;
	    for(int b=0; b < 2; b++)
	    {
		operand[b] = opn.operand[b];
		op_node[b] = opn.op_node[b];
	    }
	}


	public boolean isValid()
	{
	    return false; // return((operator_t != TokNone) && (left_t != OpNone) && (right_t != OpNone));
	}

	public boolean isReallyAnOperand()
	{
	    return((op_t == OpNone) && 
	       (operand[0] != null) && 
		   (operand[1] == null));
	}

	public Operand toOperand()
	{
	    return operand[0];
	}

	public boolean typeCheck()
	{
	    /*
	    if(left_t == right_t)
		return true;
	    if((left_t == OpMeasurementID) && (right_t == OpConstantDouble))
		return true;
	    if((left_t == OpConstantDouble) && (right_t ==  OpMeasurementID))
		return true;

	    // this is a hack, needs proper type-check for SpotAttrs...
	    if((left_t == OpMeasurementSpotAttrID) && (right_t == OpConstantDouble))
		return true;
	    if((left_t == OpMeasurementSpotAttrID) && (right_t == OpConstantDouble))
		return true;
	    */

	    return false;
	}

	public String branchToString(int b)
	{
	    if(operand[b] != null)
	    {
		return operand[b].toString();
	    }
	    else
	    {
		if(op_node[b] == null)
		{
		    return (" ? ");
		}
		else
		{
		    return op_node[b].toString();
		}
	    }
	}

	public String toStringOp()
	{
	    switch(op_t)
	    {
	    case OpNone:
		return " ? ";

	    default:
		return op_symbols[op_t];
	    }
	}
	
	public String toString()
	{
	    String res = " ( " + branchToString(0);
	    
	    res += " " + toStringOp() + " ";

	    res += branchToString(1) + " ) ";

	    return res;
	}

    }

    Operator root;

    //  /\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\

    public class Function
    {
	int x, y;
	int w, h;
	
	int fn_t;             // function type code

 	Object[]  argument;   //  arguments can be either Operators or OpNodes

	int counter;          //  used to calculating stop/go percents
   }

    //  /\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\

    public class OpVal
    {
	public int iv;
	public double dv;
	public char cv;
	public String tv;
	public boolean bv;

	public double getDouble()  { return Double.NaN; }
	public int    getInteger() { return 0; }
	public char   getChar()    { return 0; }
	public String getString()  { return null; }

	public OpVal() {}

	public boolean isDouble() { return false; }

	/*
	public OpVal(int i)     { OpVal(i); }
	public OpVal(double d)  { dv = d; }
	public OpVal(char c)    { cv = c; }
	public OpVal(String s)  { tv = s; }
	*/

	public OpVal(boolean b) { bv = b; }

	public OpVal add(OpVal o) { return null; }
	public OpVal sub(OpVal o) { return null; }
	public OpVal div(OpVal o) { return null; }
	public OpVal mul(OpVal o) { return null; }

	public OpVal lt(OpVal o) { return null; }
	public OpVal gt(OpVal o) { return null; }
	public OpVal le(OpVal o) { return null; }
	public OpVal ge(OpVal o) { return null; }
	public OpVal eq(OpVal o) { return null; }
	public OpVal ne(OpVal o) { return null; }

    }

    public class IntegerOpVal extends OpVal
    {
	public IntegerOpVal(int i) { iv = i; dv = (double) i; }

	public final double getDouble() { return (double) iv; }
	public final int    getInteger() { return iv; }

	// promote result to double if other operand is a double
	//

	public final OpVal add(OpVal o) 
	{ 
	    if(o.isDouble())
		return new DoubleOpVal((double)iv + o.getDouble()); 
	    else
		return new IntegerOpVal(iv + o.getInteger()); 
	}

	public final OpVal sub(OpVal o)
	{ 
	    if(o.isDouble())
		return new DoubleOpVal((double)iv - o.getDouble()); 
	    else
		return new IntegerOpVal(iv - o.getInteger()); 
	}

	public final OpVal div(OpVal o)
	{ 
	    if(o.isDouble())
		return new DoubleOpVal((double)iv / o.getDouble()); 
	    else
		return new IntegerOpVal(iv / o.getInteger()); 
	}
	public final OpVal mul(OpVal o)
	{ 
	    if(o.isDouble())
		return new DoubleOpVal((double)iv * o.getDouble()); 
	    else
		return new IntegerOpVal(iv * o.getInteger()); 
	}

	public final OpVal lt(OpVal o)
	{ 
	    if(o.isDouble())
		return new OpVal((double)iv < o.getDouble()); 
	    else
		return new OpVal(iv < o.getInteger()); 
	}
	public final OpVal gt(OpVal o)
	{ 
	    if(o.isDouble())
		return new OpVal((double)iv > o.getDouble()); 
	    else
		return new OpVal(iv > o.getInteger()); 
	}
	public final OpVal le(OpVal o)
	{ 
	    if(o.isDouble())
		return new OpVal((double)iv <= o.getDouble()); 
	    else
		return new OpVal(iv <= o.getInteger()); 
	}
	public final OpVal ge(OpVal o)
	{ 
	    if(o.isDouble())
		return new OpVal((double)iv >= o.getDouble()); 
	    else
		return new OpVal(iv >= o.getInteger()); 
	}
	public final OpVal eq(OpVal o)
	{ 
	    if(o.isDouble())
		return new OpVal((double)iv == o.getDouble()); 
	    else
		return new OpVal(iv == o.getInteger()); 
	}
	public final OpVal ne(OpVal o)
	{ 
	    if(o.isDouble())
		return new OpVal((double)iv != o.getDouble()); 
	    else
		return new OpVal(iv != o.getInteger()); 
	}
    }

    public class DoubleOpVal extends OpVal
    {
	public DoubleOpVal(double d) { dv = d; }

	public final boolean isDouble() { return true; }

	public final double getDouble()  { return dv; }
	public final int    getInteger() { return (int) dv; }

	public OpVal add(OpVal o) { return new DoubleOpVal(dv + o.getDouble()); }
	public OpVal sub(OpVal o) { return new DoubleOpVal(dv - o.getDouble()); }
	public OpVal div(OpVal o) { return new DoubleOpVal(dv / o.getDouble()); }
	public OpVal mul(OpVal o) { return new DoubleOpVal(dv * o.getDouble()); }

	public OpVal lt(OpVal o) { return new OpVal( DoubleCompare.doubleLessThan( dv, o.getDouble() ) );  }
	public OpVal gt(OpVal o) { return new OpVal( DoubleCompare.doubleGreaterThan( dv, o.getDouble() ) );  }
	public OpVal le(OpVal o) { return new OpVal( DoubleCompare.doubleLessEquals( dv, o.getDouble() ) ); }
	public OpVal ge(OpVal o) { return new OpVal( DoubleCompare.doubleGreaterEquals( dv, o.getDouble() ) ); }
	public OpVal eq(OpVal o) { return new OpVal( DoubleCompare.doubleEquals( dv, o.getDouble() ) ); }
	public OpVal ne(OpVal o) { return new OpVal( DoubleCompare.doubleNotEquals( dv, o.getDouble() ) ); }
    }


    public class CharOpVal extends OpVal
    {
	public CharOpVal(char c) { cv = c; tv = String.valueOf(c); }

	public OpVal add(OpVal o) { return new CharOpVal(cv); }
	public OpVal sub(OpVal o) { return new CharOpVal(cv); }
	public OpVal div(OpVal o) { return new CharOpVal(cv); }
	public OpVal mul(OpVal o) { return new CharOpVal(cv); }

	public OpVal lt(OpVal o) { return new OpVal(cv < o.cv);  }
	public OpVal gt(OpVal o) { return new OpVal(cv > o.cv);  }
	public OpVal le(OpVal o) { return new OpVal(cv <= o.cv); }
	public OpVal ge(OpVal o) { return new OpVal(cv >= o.cv); }
	public OpVal eq(OpVal o) { return new OpVal(cv == o.cv); }
	public OpVal ne(OpVal o) { return new OpVal(cv != o.cv); }
    }
    public class TextOpVal extends OpVal
    {
	public TextOpVal(String t) { tv = t; }

	public OpVal add(OpVal o) { return new TextOpVal(tv + o.tv); }
	public OpVal sub(OpVal o) { return new TextOpVal(tv); }
	public OpVal div(OpVal o) { return new TextOpVal(tv); }
	public OpVal mul(OpVal o) { return new TextOpVal(tv); }

	public OpVal lt(OpVal o) { return new OpVal(tv.compareTo(o.tv) < 0);  }
	public OpVal gt(OpVal o) { return new OpVal(tv.compareTo(o.tv) > 0);  }
	public OpVal le(OpVal o) { return new OpVal(tv.compareTo(o.tv) <= 0); }
	public OpVal ge(OpVal o) { return new OpVal(tv.compareTo(o.tv) >= 0); }
	public OpVal eq(OpVal o) { return new OpVal(tv.equals(o.tv)); }
	public OpVal ne(OpVal o) { return new OpVal(!tv.equals(o.tv)); }
    }

    //  /\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/

    public class Operand
    {
	int op_t;       // operand type
	
	String name;    // used for display
	
	int x, y;       // used for layout
	int w, h;

	public Operand(int o, String n) { op_t = o; name = n; }
	
	// constant
	//
	OpVal the_const;
	OpVal the_var;

	public OpVal getOpVal(int spot_id)
	{
	    if(op_t == OpMeasurementID)
	    {
		return new DoubleOpVal(dv[spot_id]);
	    }
	    else
	    {
		if(op_t == OpMeasurementSpotAttrID)
		{
		   switch(meas_spot_attr_type)
		   {
		   case 0:
		       return new IntegerOpVal(iv[spot_id]);
		   case 1:
		       return new DoubleOpVal(dv[spot_id]);
		   case 2:
		       return new CharOpVal(cv[spot_id]);
		   default:
		       return new TextOpVal(tv[spot_id]);
		   }
		}
		else
		{
		    return the_const;
		}
	    }
	}
	
	// variable
	//
	int meas_id;
	int meas_spot_attr_id;
	int meas_spot_attr_type;

	// and for efficency, references direct to the data arrays
	//
	double[] dv;
	int[]    iv;
	char[]   cv;
	String[] tv;
	
	public String toString()
	{
	    switch(op_t)
	    {
	    case OpConstantDouble:
		//return (the_const.dv + "d");
		return (String.valueOf(the_const.dv));
	    case OpConstantInteger:
		//return (the_const.iv + "i");
		return (String.valueOf(the_const.iv));
	    case OpConstantText:
		return ("\"" + the_const.tv + "\"");
	    case OpConstantChar:
		return ("'" + the_const.cv + "'");
		/*
	    case OpMeasurementID:
		//return "[" + name + "]d";
		return name;
	    case OpMeasurementSpotAttrID:
		switch(meas_spot_attr_type)
		   {
		   case 0:
		       return "[" + name + "]i";
		   case 1:
		       return "[" + name + "]d";
		   case 2:
		       return "[" + name + "]c";
		   default:
		       return "[" + name + "]t";
		   }
		*/
	    default:
		return name;
	    }
	}


	public Operand( final String str ) throws Parser.ParseError
	{
	    final int len = str.length();
	    
	    name = str;

	    //
	    // don't just remove quotes for the hell of it, the measurement names might actually be in quotes
	    //
	     String unquoted_version_of_name = null;

	    if(( len > 2 ) && 
	       ( str.charAt(0)=='\"' ) && 
	       ( str.charAt( len-1 )=='\"' ) )
		unquoted_version_of_name = str.substring(1, len-1);

	    
	    // is the token a Measurement name?
	    Integer m_id = (Integer) meas_name_to_id_ht.get( name );

	    // if the name wasn't recognised, try the unquoted version of the name (if one exists)
	    if(( unquoted_version_of_name != null ) && ( m_id == null ) )
		m_id = (Integer) meas_name_to_id_ht.get( unquoted_version_of_name );

	    if(m_id != null)
	    {
		op_t = OpMeasurementID;
		meas_id = m_id.intValue();

		// store the vector....
		dv = edata.getMeasurementData(meas_id);
		return;
	    }


	    // is it a Measurement.SpotAttribute name?
	    MeasSpotAttr msa = (MeasSpotAttr) meas_spot_attr_to_id_ht.get(name);

	    if(( unquoted_version_of_name != null ) && ( msa == null ) )
		msa = (MeasSpotAttr) meas_spot_attr_to_id_ht.get( unquoted_version_of_name );
	    
	    if( msa != null )
	    {
		op_t = OpMeasurementSpotAttrID;
		meas_id           = msa.meas;
		meas_spot_attr_id = msa.attr;
		ExprData.Measurement m = edata.getMeasurement(meas_id);
		meas_spot_attr_type = m.getSpotAttributeDataTypeCode(meas_spot_attr_id);
		
		// store a reference to the relevant array type....
		switch(meas_spot_attr_type)
		{
		case 0:
		    iv = (int[]) m.getSpotAttributeData(meas_spot_attr_id);
		    //op_t = OpConstantInteger;
		    return;
		case 1:
		    dv = (double[]) m.getSpotAttributeData(meas_spot_attr_id);
		    //op_t = OpConstantDouble;
		    return;
		case 2:
		    cv = (char[]) m.getSpotAttributeData(meas_spot_attr_id);
		    //op_t = OpConstantChar;
		    return;
		case 3:
		    tv = (String[]) m.getSpotAttributeData(meas_spot_attr_id);
		    //op_t = OpConstantText;
		    return;
		}
		return;
	    }

	    try
	    {
		the_const = new IntegerOpVal(Integer.valueOf(name).intValue());
		op_t =  OpConstantInteger;
		return;
	    }
	    catch(NumberFormatException infe)
	    {
		try
		{
		    if( name.toLowerCase().equals("nan" ) )
			the_const = new DoubleOpVal( Double.NaN);
		    else
			if( name.toLowerCase().equals("infinity" ) )
			    the_const = new DoubleOpVal( Double.POSITIVE_INFINITY );
			else
			    the_const = new DoubleOpVal(Double.valueOf(name).doubleValue());
		    op_t = OpConstantDouble;
		    return;
		}
		catch(NumberFormatException dnfe)
		{
		    if( len == 3)
		    {
			// a single character must be enclosed in 'quotes'
			if( (str.charAt(0)=='\'') && (str.charAt(2)=='\'') )
			{
			    the_const = new CharOpVal(str.charAt(1));
			    op_t = OpConstantChar;
			}
			else
			{
			    throw new Parser.ParseError("Illegal operand: " + str );
			}
		    }
		    else
		    {
			// is it a string (it must be enclosed in double quotes?)
			
			if( ( len > 1 ) && 
			    ( str.charAt(0)=='\"' ) && 
			    ( str.charAt( len-1 )=='\"' ) )
			{
			    the_const = new TextOpVal(str.substring(1, len-1));
			    op_t = OpConstantText;
			}
			else
			{
			    // it's illegal!
			    throw new Parser.ParseError("Illegal operand: " + str );
			}
		    }
		}
	    }
	}

    }

    //  /\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\


    // codes for the operand types
    //
    public final static int OpNone            = -1;

    public final static int OpIsNode           = 0;  // operand is another node

    public final static int OpConstantDouble        = 1;
    public final static int OpConstantInteger       = 2;
    public final static int OpConstantChar          = 3;
    public final static int OpConstantText          = 4;
    public final static int OpMeasurementID         = 5;
    public final static int OpMeasurementSpotAttrID = 6;

    public boolean isConstant( int op )
    {
	return( (op == OpConstantDouble) || 
		(op == OpConstantInteger) || 
		(op == OpConstantChar) || 
		(op == OpConstantText) );
    }

    // codes for the possible tokens found the in the rule
    // (and their corresponding operators)

    public final static int OpLessThan     =  0;
    public final static int OpGreaterThan  =  1;
    public final static int OpLessEqual    =  2;
    public final static int OpGreaterEqual =  3;
    public final static int OpNotEqual     =  4;
    public final static int OpEqual        =  5;

    public final static int OpLogicalAnd   =  6;
    public final static int OpLogicalOr    =  7;
    public final static int OpLogicalNot   =  8;
    
    public final static int OpAdd          =  9;
    public final static int OpSubtract     = 10;
    public final static int OpMultiply     = 11;
    public final static int OpDivide       = 12;

    //
    // important: the order of synbols in the following array must match the order of the ID codes above,
    //
    // ( note that this is NOT the precedence order, which is actually determined in the
    //   calls to Parser.set..Symbol() in the c'tor for this plugin )
    //

    final String[] op_symbols = { "<", ">", "<=", ">=", "!=", "=", "and", "or", "!", "+", "-", "*", "/",  };

    private Hashtable meas_name_to_id_ht;
    private Hashtable meas_spot_attr_to_id_ht;

    private int getOperatorType( String operator_str )
    {
	for(int os=0; os < op_symbols.length; os++)
	{
	    if( op_symbols[ os ].equals( operator_str ) )
		return os;
	}
	return OpNone;
    }

    public boolean isMathOp(int op_t)
    {
	switch(op_t)
	{
	case OpAdd:
	case OpSubtract:
	case OpMultiply:
	case OpDivide:
	    return true;
	default:
	    return false;
	}
    }
    
    public boolean isBoolOp(int op_t)
    {
	return ((op_t == OpLogicalAnd) || (op_t == OpLogicalOr));
    }

    public boolean isRelOp(int op_t)
    {
	switch(op_t)
	{
	case OpLessThan:
	    return true;
	case OpGreaterThan:
	    return true;
	case OpLessEqual:
	    return true;
	case OpGreaterEqual:
	    return true;
	case OpNotEqual:
	    return true;
	case OpEqual:
	    return true;
	}
	return false;
    }
    

    //  /\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\

 
    public void compileDecisionTree( Parser.TermNode term_node ) throws Parser.ParseError
    {
	Object root_object =  makeOperatorOrOperand( term_node );

	if( root_object instanceof Operator )
	{
	    root = (Operator) root_object;
	}
	else
	{
	    root = null;
	    throw new Parser.ParseError("The expression must evaluate to a boolean value (i.e. true or false)");
	}
    }
    
    private Object makeOperatorOrOperand( Parser.TermNode term_node ) throws Parser.ParseError
    {
	if( term_node == null )
	    return null;


	if( term_node instanceof Parser.IdentifierTermNode )
	{
	    Parser.IdentifierTermNode itn = (Parser.IdentifierTermNode) term_node;

	    return new Operand( itn.identifier );
	}
	

	if( term_node instanceof Parser.BinaryOperatorTermNode )
	{
	    Parser.BinaryOperatorTermNode botn = (Parser.BinaryOperatorTermNode) term_node;

	    Operator op = new Operator( true );

	    op.op_t = getOperatorType( botn.operator );

	    Object lhs = makeOperatorOrOperand( botn.left_operand );

	    if( lhs instanceof Operator )
		op.op_node[0] = (Operator) lhs;
	    else
		op.operand[0] = (Operand) lhs;

	    Object rhs = makeOperatorOrOperand( botn.right_operand );

	    if( rhs instanceof Operator )
		op.op_node[1] = (Operator) rhs;
	    else
		op.operand[1] = (Operand) rhs;
	    
	    return op;
	}


	if( term_node instanceof Parser.UnaryOperatorTermNode )
	{
	    Parser.UnaryOperatorTermNode uotn = (Parser.UnaryOperatorTermNode) term_node;

	    Operator op = new Operator( false );

	    op.op_t = getOperatorType( uotn.operator );

	    Object rhs = makeOperatorOrOperand( uotn.operand );

	    if( rhs instanceof Operator )
		op.op_node[1] = (Operator)rhs;
	    else
		op.operand[1] = (Operand)rhs;
	    
	    return op;
	}


	if( term_node instanceof Parser.FunctionTermNode )
	{
	    // Parser.FunctionTermNode ftn = (Parser.FunctionTermNode) term_node;

	    throw new Parser.ParseError("Sorry, functions are not implemented yet");
	}

	
	throw new Parser.ParseError("Unhandled species of Parser.TermNode");
    }

    
    private String describeTermNode( Parser.TermNode term_node )
    {
	if( term_node == null )
	    return "";
	
	if( term_node instanceof Parser.IdentifierTermNode )
	{
	    return ((Parser.IdentifierTermNode) term_node).identifier + "[i]";
	}
	if( term_node instanceof Parser.BinaryOperatorTermNode )
	{
	    Parser.BinaryOperatorTermNode botn = (Parser.BinaryOperatorTermNode) term_node;
	    return " ( " + describeTermNode( botn.left_operand ) + botn.operator + "[b]" + describeTermNode( botn.right_operand ) + " ) ";
	}
	if( term_node instanceof Parser.UnaryOperatorTermNode )
	{
	    Parser.UnaryOperatorTermNode uotn = (Parser.UnaryOperatorTermNode) term_node;
	    return " ( " + uotn.operator + "[u]" + describeTermNode( uotn.operand ) + " ) ";
	}
	if( term_node instanceof Parser.FunctionTermNode )
	{
	    return "[f]";
	}

	return "[?]";
    }

    //  \/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/\/

	
    private final int TypeUnknown = 0;
    private final int TypeInt     = 1;
    private final int TypeDouble  = 2;
    private final int TypeChar    = 3;
    private final int TypeText    = 4;
    private final int TypeBoolean = 5;

    public void typeCheckDecisionTree() throws Parser.ParseError
    {
	if(root == null)
	    return;

	int root_t = recursivelyTypeCheckOperator(root);
	
	if(root_t != TypeBoolean)
	    throw new Parser.ParseError("Type mismatch: the expression must evaluate to a boolean");
    }
    
    private int recursivelyTypeCheckOperator(Operator op) throws Parser.ParseError
    {
	int[] op_type = new int[2];

	for(int b=0; b< 2; b++)
	{
	    op_type[b] = TypeUnknown;

	    if(op.operand[b] != null)
	    {
		switch(op.operand[b].op_t)
		{
		case OpMeasurementID:
		    op_type[b] = TypeDouble;
		    break;
		case OpMeasurementSpotAttrID:
		    switch(op.operand[b].meas_spot_attr_type)
		    {
		    case 0:
			op_type[b] = TypeInt;
			break; 
		    case 1:
			op_type[b] = TypeDouble;
			break; 
		    case 2:
			op_type[b] = TypeChar;
			break; 
		    default:
			op_type[b] = TypeText;
			break; 
		    }
		    break;    
		case OpConstantInteger:
		    op_type[b] = TypeInt;
		    break;
		case OpConstantDouble:
		    op_type[b] = TypeDouble;
		    break;
		case OpConstantChar:
		    op_type[b] = TypeChar;
		    break;
		case OpConstantText:
		    op_type[b] = TypeText;
		    break;
		}
	    }
	    else
	    {
		if(op.op_node[b] != null )
		{
		    op_type[b] = recursivelyTypeCheckOperator(op.op_node[b]);
		}
		else
		{
		    if( op.is_binary )
			throw new Parser.ParseError( "Syntax error: expecting operand" );
		}
	    }
	}
	
	final String[] tnames = { "?", "I", "D", "C" ,"T", "B" };

	
	String tstr = "B";
	if(isMathOp(op.op_t))
	    tstr = "M";
	if(isRelOp(op.op_t))
	    tstr = "R";

	
	if(debug_major)
	    System.out.println("[" + op.toString() + "]-[" + 
			       tnames[op_type[0]] + ":" + tstr + ":" + tnames[op_type[1]] + "] ");

	/*
	if(op_type[0] != op_type[1])
	{
	    // make sure at both one of the operands are variables or constants
	    if(op.op_node[0] != null || op.op_node[1] != null)
		throw new Parser.ParseError("type mismatch", -1, op);

	    // promote ints to doubles where neccessary and possible
	    if((op_type[0] == TypeDouble) && (op_type[1] == TypeInt))
	    {
		// rhs is constant Int, lhs is double.... 
		if((op.operand[1] != null) && (op.operand[1].op_t == OpConstantInteger))
		{
		    // promote the constant
		    op.operand[1].op_t = OpConstantDouble;
		    op.operand[1].the_const.dv = (double) op.operand[1].the_const.iv;
		    op_type[1] = TypeDouble;
		}
	    }
	    else
	    {
		// now try the same but with lhs and rhs reversed..
		if((op_type[0] == TypeInt) && (op_type[1] == TypeDouble))
		{
		    // rhs is constant Int, lhs is double.... 
		    if((op.operand[0] != null) && (op.operand[0].op_t == OpConstantInteger))
		    {
			// promote the constant
			op.operand[0].op_t = OpConstantDouble;
			op.operand[0].the_const.dv = (double) op.operand[0].the_const.iv;
			op_type[0] = TypeDouble;
		    }
		}
		else
		{
		    // promotion not possible,  a type mismatch then...
		    //
		    throw new Parser.ParseError("type mismatch", -1, op);
		}
	    }
	}
	*/

	// check at least one of lhs or rhs is a variable
	boolean has_a_variable = true;
	if( (op.operand[0] != null) && (op.operand[1] != null) )
	{
	    if(isConstant(op.operand[0].op_t) && isConstant(op.operand[1].op_t))
		has_a_variable = false;
	}
	
	
	if(isBoolOp(op.op_t))
	{
	    if(!has_a_variable)
		throw new Parser.ParseError( "Syntax error: expecting variable" ); 

	    return TypeBoolean;
	}

	if(isRelOp(op.op_t))
	{
	    if(!has_a_variable)
		throw new Parser.ParseError( "Syntax error: expecting variable" ); 
	  
	    return TypeBoolean;
	}

	if(isMathOp(op.op_t))
	    return op_type[1];

	return TypeUnknown;
    }
	
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  do the actual logic test
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private final DoubleOpVal zero = new DoubleOpVal(0);

    private OpVal evaluateNode(Operator opn, int s_id)
    {
	try
	{
	    
	    if(isRelOp(opn.op_t))
	    {
		OpVal lhs = (opn.operand[0] == null) ? evaluateNode(opn.op_node[0], s_id) : opn.operand[0].getOpVal(s_id);
		OpVal rhs = (opn.operand[1] == null) ? evaluateNode(opn.op_node[1], s_id) : opn.operand[1].getOpVal(s_id);
		
		switch(opn.op_t)
		{
		case OpLessThan:
		    return lhs.lt(rhs);
		case OpGreaterThan:
		    return lhs.gt(rhs);
		case OpLessEqual:
		    return lhs.le(rhs);
		case OpGreaterEqual:
		    return lhs.ge(rhs);
		case OpNotEqual:
		    return lhs.ne(rhs);
		case OpEqual:
		    return lhs.eq(rhs);
		} 
		
		return null;
	    }
	    
	    if(isMathOp(opn.op_t))
	    {
		if( opn.is_binary )
		{
		    OpVal lhs = (opn.operand[0] == null) ? evaluateNode(opn.op_node[0], s_id) : opn.operand[0].getOpVal(s_id);
		    OpVal rhs = (opn.operand[1] == null) ? evaluateNode(opn.op_node[1], s_id) : opn.operand[1].getOpVal(s_id);
		    
		    try
		    {
			switch(opn.op_t)
			{
			case OpAdd:
			    return lhs.add(rhs);
			case OpSubtract:
			    return lhs.sub(rhs);
			case OpMultiply:
			    return lhs.mul(rhs);
			case OpDivide:
			    return lhs.div(rhs);
			}
		    }
		    catch( java.lang.ArithmeticException ae )
		    {
			if( graph_panel != null )
			{
			    graph_panel.setErrorMsg( ae.toString() );
			    graph_panel.repaint(); 
			}
			
			return null;
		    }
		    return null;
		}
		else
		{
		    OpVal rhs = (opn.operand[1] == null) ? evaluateNode(opn.op_node[1], s_id) : opn.operand[1].getOpVal(s_id);

		    // must be the unary operator '-'

		    //if( opn.op_t == OpSubtract )
		    return zero.sub( rhs );
			//else
			//	return null;
		}
	    }
	    
	    if(isBoolOp(opn.op_t))
	    {
		// short circuit the logic if possible
		//
		if(opn.op_t == OpLogicalAnd)
		{
		    OpVal lhs = evaluateNode(opn.op_node[0], s_id);
		    if(lhs.bv==true)
		    {
			OpVal rhs = evaluateNode(opn.op_node[1], s_id);
			return new OpVal(rhs.bv==true);
		    }
		    else
		    {
			return new OpVal(false);
		    }
		}
		else
		{
		    OpVal lhs = evaluateNode(opn.op_node[0], s_id);
		    if(lhs.bv==true)
		    {
			return new OpVal(true);
		    }
		    else
		    {
			OpVal rhs = evaluateNode(opn.op_node[1], s_id);
			return new OpVal(rhs.bv==true);
		    }
		}
	    }
	    
	    return null;
	}
	catch(NullPointerException npe)
	{
	    return null;
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  drawing the graph
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private final int node_h = 25;
    private final int node_w = 50;
    
    private final int node_h_gap = 20;
    private final int node_w_gap = 50;

    // place the nodes onto a plane, with the root at 0,0
    //
    protected int x_offset = 0;
    protected int y_offset = 0;


    protected int n2s_tx = 0;        // translate graph coords to 0,0
    protected int n2s_ty = 0;

    protected double n2s_sx = .0;    // scale to screen coords
    protected double n2s_sy = .0;    

    protected int n2s_cx = 0;        // offset for centering
    protected int n2s_cy = 0;

    // map node coords to screen coords
    //
    // n2spX ... node position to screen position (in X dimension)
    //
    protected int n2spX(int nx)
    {
	return (int)(((double)(nx + n2s_tx) * n2s_sx)) + n2s_cx ;
    }
    protected int n2spY(int ny)
    {
	return (int)(((double)(ny + n2s_ty) * n2s_sy)) + n2s_cy ;
    }
    //
    // n2slX ... node length   to screen length (in X dimension)
    // (i.e don't add the post-scale centering translation)
    //
    protected int n2slX(int nx)
    {
	//return (int)((double)(nx + n2s_tx) * n2s_sx);
	return (int)((double)nx * n2s_sx);
    }
    protected int n2slY(int ny)
    {
	//return (int)((double)(ny + n2s_ty) * n2s_sy);
	return (int)((double)ny * n2s_sy);
    }

    public void positionNodes()
    {
	if(root != null)
	    positionNode(root, 0, node_h);
	
	//x_offset = findMinXPos();
	//x_offset = -x_offset + node_h;     //(graph_panel.getWidth() / 2);

    }
    
    public int textWidth(Operator opn)
    {
	String op_str = opn.toStringOp();

	int w = font_metrics.stringWidth(op_str);

	if(w < node_h)
	    w = node_h;

	return w;
    }

    public int widthOfNode( Operand opr )
    {
	int w = font_metrics.stringWidth( "  " + opr.name + "  " );
	
	if(w < node_h)
	    w = node_h;

	return w;
    }


    public int widthOfNode( Operator opn )
    {
	int w = textWidth(opn);  // node_w;
	
	for(int b=0; b < 2; b++)
	{
	    if(opn.op_node[b] != null)
	    {
		w += widthOfNode(opn.op_node[b]);
	    }
	    if(opn.operand[b] != null)
	    {
		w += widthOfNode(opn.operand[b]);
	    }
	}
	
	return w;
    }


    public void positionNode(Operand opr, int x, int y)
    {
	opr.x = x;
	opr.y = y;
	
	opr.w = widthOfNode( opr );
	opr.h = node_h;
	
    }

    public void positionNode(Operator opn, int x, int y)
    {
	opn.x = x;
	opn.y = y;
	
	opn.w = textWidth(opn); // node_w;

	int l_ext = 0;

	if(opn.op_node[0] != null)
	{
	    l_ext = widthOfNode(opn.op_node[0]); // ,  x - node_w, y + (node_h*2));
	}
	if(opn.operand[0] != null)
	{
	    l_ext = widthOfNode(opn.operand[0]); // ,  x - node_w, y + (node_h*2));
	}

	opn.x += l_ext;

	if(opn.op_node[0] != null)
	{
	    positionNode(opn.op_node[0],  opn.x - l_ext, y + node_h + node_h_gap);
	    
	}
	if(opn.op_node[1] != null)
	{
	    positionNode(opn.op_node[1], opn.x + opn.w, y + node_h + node_h_gap);
	}

	if(opn.operand[0] != null)
	{
	    positionNode(opn.operand[0],  opn.x - l_ext, y + node_h + node_h_gap);
	    
	}
	if(opn.operand[1] != null)
	{
	    positionNode(opn.operand[1], opn.x + opn.w, y + node_h + node_h_gap);
	}
    }
    
    // find the extents of the nodes
    // (used to translate the positions for display)
    //
    class NodeExtent
    {
	public int minx, maxx, miny , maxy;

	public void mergeWith(NodeExtent ne)
	{
	    if(ne.minx < minx)
		minx = ne.minx;
	    if(ne.maxx > maxx)
		maxx = ne.maxx;
	    if(ne.miny < miny)
		miny = ne.miny;
	    if(ne.maxy > maxy)
		maxy = ne.maxy;
	}
    }

    public NodeExtent findExtent()
    {
	NodeExtent ne = new NodeExtent();
	
	ne.minx = ne.miny = Integer.MAX_VALUE;
	ne.maxx = ne.maxy = -Integer.MAX_VALUE;

	if(root == null)
	    return ne;
	else
	    return recursivelyfindExtent(ne, root);
    }


    private NodeExtent findExtent(NodeExtent ne, Operand opr)
    {
	if(opr.x < ne.minx)
	    ne.minx = opr.x;
	if((opr.x+opr.w) > ne.maxx)
	    ne.maxx = (opr.x+opr.w);
	if(opr.y < ne.miny)
	    ne.miny = opr.y;
	if((opr.y+node_h) > ne.maxy)
	    ne.maxy = (opr.y+node_h);	

	return ne;
    }


    private NodeExtent recursivelyfindExtent(NodeExtent ne, Operator opn)
    {
	if(opn.x < ne.minx)
	    ne.minx = opn.x;
	if((opn.x+opn.w) > ne.maxx)
	    ne.maxx = (opn.x+opn.w);
	if(opn.y < ne.miny)
	    ne.miny = opn.y;
	if((opn.y+node_h) > ne.maxy)
	    ne.maxy = (opn.y+node_h);

	for(int b=0; b < 2; b++)
	{
	    if(opn.op_node[b] != null)
	    {
		NodeExtent lne = recursivelyfindExtent(ne, opn.op_node[b]);
		ne.mergeWith(lne);
	    }
	    if(opn.operand[b] != null)
	    {
		NodeExtent lne = findExtent( ne, opn.operand[b] );
		ne.mergeWith(lne);
	    }
	}

	return ne;
    }

    FontMetrics font_metrics;
    int font_height;

    // ==============================================================================

    public class CustomListener implements ComponentListener 
    {
	public void componentHidden(ComponentEvent e) {}
	
	public void componentMoved(ComponentEvent e) {}
	
	public void componentResized(ComponentEvent e) {  doLayoutAndDisplayGraph(); }
	
	public void componentShown(ComponentEvent e) {}
    }

    // ==============================
    

    public class GraphPanel extends JPanel
    {
	public GraphPanel()
	{
	    addComponentListener( new CustomListener() );
	}

	private String error_msg = null;

	private Color dark_green_col = new Color(0,255-32-16,0);
	private Color dark_red_col   = new Color(255-32-16,0,0);


	public void paintComponent(Graphics graphic)
	{
	    if(graphic == null)
		return;

	    graphic.setColor(Color.white);
	    graphic.fillRect(0, 0, getWidth(), getHeight());
	    graphic.setColor(Color.black);

	    if(error_msg != null)
	    {
		Font f = graphic.getFont();
		
		Font error_font = new Font(f.getName(), Font.BOLD, 12);
		
		graphic.setFont(error_font);
		
		graphic.setColor(Color.blue);
		graphic.drawString(error_msg, 10, font_height+10);

		return;
	    }

	    if(root != null)
	    {
		//System.out.println(ne.minx + "," + ne.miny + " to " + ne.maxx + "," + ne.maxy);
		
		//System.out.println("scale=" + n2s_sx + "," + n2s_sy + " trans=" + n2s_tx + "," + n2s_ty);

		
		// scale the font based on the original 12pt used for the layout

		int font_size = n2slY(12);

		Font f = graphic.getFont();
		
		Font graph_font = new Font(f.getName(), f.getStyle(), font_size);
		
		// x_offset = -ne.minx + ((getWidth()  - xr) / 2);
		// y_offset = -ne.miny + ((getHeight() - yr) / 2);
		
		//System.out.println("font size " + font_size);

		graphic.setFont(graph_font);
		
		font_metrics = graphic.getFontMetrics();
		font_height  = font_metrics.getAscent();
		
		// draw a line from the top into the root node
		graphic.setColor(Color.lightGray);

		graphic.drawLine(n2spX(root.x + (root.w/2)), n2spY(root.y + (node_h/2)),
				 n2spX(root.x + (root.w/2)), n2spY(root.y - node_h));

		drawNode(graphic, root);

	    }

	}

	public void setErrorMsg(String msg)
	{
	    error_msg = msg;
	}


	public void drawNode(Graphics g, Operand opr)
	{
	    g.setColor(graph_node_col);
	    g.fillRect(n2spX(opr.x), n2spY(opr.y), n2slX(opr.w), n2slY(node_h));
	    
	    g.setColor(colourForOperand(opr.op_t));

	    int t_w = font_metrics.stringWidth( opr.name );

	    int t_o = ( n2slX(opr.w) - t_w ) / 2;

	    int t_y = n2spY(opr.y  + ((node_h + font_height)/2));

	    g.drawString( opr.name, t_o + n2spX(opr.x), t_y );
	}


	public void drawNode(Graphics g, Operator opn)
	{
	    g.setColor(graph_node_col);

	    //System.out.println("node '" + opn.toStringOp() + "' at " + opn.x + "," + opn.y + ", offset is " + x_offset);

	    for(int b=0; b < 2; b++)
	    {
		if( opn.op_node[b] != null )
		{
		    // H part
		    g.drawLine(n2spX(opn.x + (opn.w/2)), n2spY(opn.y + (node_h/2)),
			       n2spX(opn.op_node[b].x + (opn.op_node[b].w/2)), n2spY(opn.y + (node_h/2)));
		    // V part
		    g.drawLine(n2spX(opn.op_node[b].x + (opn.op_node[b].w/2)), n2spY(opn.y + (node_h/2)),
			       n2spX(opn.op_node[b].x + (opn.op_node[b].w/2)), n2spY(opn.op_node[b].y));
		}

		if( opn.operand[b] != null )
		{
		    // H part
		    g.drawLine(n2spX(opn.x + (opn.w/2)), n2spY(opn.y + (node_h/2)),
			       n2spX(opn.operand[b].x + (opn.operand[b].w/2)), n2spY(opn.y + (node_h/2)));
		    // V part
		    g.drawLine(n2spX(opn.operand[b].x + (opn.operand[b].w/2)), n2spY(opn.y + (node_h/2)),
			       n2spX(opn.operand[b].x + (opn.operand[b].w/2)), n2spY(opn.operand[b].y));
		}
	    }
	    
	    if(isBoolOp(opn.op_t))
		g.fillOval(n2spX(opn.x), n2spY(opn.y), n2slX(opn.w), n2slY(node_h));
	    else
		g.fillRect(n2spX(opn.x), n2spY(opn.y), n2slX(opn.w), n2slY(node_h));

	    for(int b=0; b < 2; b++)
	    {
		if(opn.op_node[b] != null)
		{
		    drawNode(g, opn.op_node[b]);
		}
		if(opn.operand[b] != null)
		{
		    drawNode(g, opn.operand[b]);
		}
	    }

	    // flag any errors in this node
	    if(opn.isValid())
	    {
		g.setColor(Color.red);
	    }
	    else
	    {
		g.setColor(Color.black);
	    }

	    // work out where to draw the various parts of the label

	    String op_str = " " + opn.toStringOp() + " ";

	    int    op_w   = font_metrics.stringWidth(op_str);

	    int    ttw_s   = op_w;     // total text width (screen coords)

	    int[]    side_w = new int[2];
	    String[] side_s = new String[2];
	   
	   // now we know the total text width for this node
	    // work out where to start drawing the first bit of text

	    int nw_s = n2slX(opn.w);                          // node width (screen coords)
	    int to_s = (nw_s - ttw_s) / 2;                    // text offset (screen coords)

	    font_metrics.stringWidth(op_str); 

	    int label_x = n2spX(opn.x) + to_s;
	    int label_y = n2spY(opn.y  + ((node_h + font_height)/2));

	    g.setColor(colourForOperator(opn.op_t));
	    g.drawString(op_str, label_x, label_y);
	    label_x += op_w;

	    if(counts_are_valid && (!isMathOp(opn.op_t)))
	    {
		g.setColor(dark_green_col);
		double passed_d = ((double)opn.counter * 100.0) /  (double)counts_for_n_spots;

		String passed_s = mview.niceDouble(passed_d, 6,3) + " %";

	        label_y = n2spY(opn.y) - 1;

		int cw = font_metrics.stringWidth(passed_s);
		label_x = n2spX(opn.x) + (n2slX(opn.w) - cw) / 2;

		g.drawString(passed_s, label_x, label_y);
		
		g.setColor(dark_red_col);
		double stopped_d = 100.0 - passed_d;
		String stopped_s = mview.niceDouble(stopped_d, 6,3) + " %";

		label_y += n2slY(node_h) + font_height + 1;
		
		cw = font_metrics.stringWidth(stopped_s);
		label_x = n2spX(opn.x) + (n2slX(opn.w) - cw) / 2;

		g.drawString(stopped_s, label_x, label_y);
		
	    }
	}
    }

    final static Color graph_node_col = new Color(235,235,230);

    final static Color salmon = new Color(230, 121, 121);
    final static Color warmgrey = new Color(136, 105, 105);
    final static Color terracotta = new Color(202, 35, 35);
    final static Color orangeyred = new Color(188, 42, 5);

    final static Color nicegreen  = new Color(15,124,50);
    final static Color niceblue  = new Color(51, 145, 185);
    final static Color washeygreen  = new Color(90,131,60);

    private Color colourForOperator(int op_t)
    {
	if(isBoolOp(op_t))
	    return niceblue;
	if(isRelOp(op_t))
	    return nicegreen;
	if(isMathOp(op_t))
	    return washeygreen;

	return Color.black;
    }
    private Color colourForOperand(int op_t)
    {
	switch(op_t)
	{
	case OpMeasurementID:
	case OpConstantDouble:
	    return warmgrey;
	case OpConstantInteger:
	    return salmon;
	case OpConstantChar:
	    return terracotta;
	case OpConstantText:
	    return orangeyred;
	case OpMeasurementSpotAttrID:
	    return Color.black;
	}
	
	return Color.black;
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  counting hits/misses
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private boolean counts_are_valid = false;
    private int counts_for_n_spots = 0;        // how many spots were there when the counts were done?
    private boolean reset_counter = false;
    private CounterThread counter_thread = null;

    public void doCount()
    {
	if(counter_thread != null)
	{
	    reset_counter = true;
	}
	else
	{
	    reset_counter = false;
	    counter_thread = new CounterThread();
	    counter_thread.start();
	}
    }


    public class CounterThread extends Thread
    {
	public void run()
	{
	    if(root != null)
	    {
		counts_are_valid = false;
		
		resetCounters(root);
		
		counts_for_n_spots = edata.getNumSpots();
		
		try
		{
		    for(int s=0; s < counts_for_n_spots; s++)
		    {
			if(reset_counter)
			{
			    reset_counter = false;
			    resetCounters(root);
			    s = 0;
			}
			
			countHitForNode(root, s);
		    }

		    counts_are_valid = true;
		    
		}
		catch(NullPointerException npe)
		{
		    
		}
		
		//System.out.println( spots_counted  + "checked");
		//printCounters(root_node);
		
		graph_panel.repaint();
	    }
	    counter_thread = null;
	}
    }


    private void resetCounters(Operator opn)
    {
	opn.counter = 0;
	for(int b=0; b < 2; b++)
	    if(opn.op_node[b] != null)
		resetCounters(opn.op_node[b]); 
    }
    
    private void printCounters(Operator opn)
    {
	System.out.println(opn.toString() + " : " + opn.counter);
	for(int b=0; b < 2; b++)
	    if(opn.op_node[b] != null)
		printCounters(opn.op_node[b]); 
    }

    // kept separate from evaluateNode so as not to slow the
    // normal filtering operations down
    //
    // (but this is basically the same code as evaluateNode except
    //  that each decision updates the counter for it's node)
    //
    private OpVal countHitForNode(Operator opn, int s_id)
    {
	if(isRelOp(opn.op_t))
	{
	    if(opn.operand == null)
		return null;

	    OpVal lhs = (opn.operand[0] == null) ? countHitForNode(opn.op_node[0], s_id) : opn.operand[0].getOpVal(s_id);
	    OpVal rhs = (opn.operand[1] == null) ? countHitForNode(opn.op_node[1], s_id) : opn.operand[1].getOpVal(s_id);
	    
	    switch(opn.op_t)
	    {
	    case OpLessThan:
		if(lhs.lt(rhs).bv == true)
		{
		    opn.counter++;
		    return new OpVal(true);
		}
		else
		    return new OpVal(false);
	    case OpGreaterThan:
		if(lhs.gt(rhs).bv == true)
		{
		    opn.counter++;
		    return new OpVal(true);
		}
		else
		    return new OpVal(false);
	    case OpLessEqual:
		if(lhs.le(rhs).bv == true)
		{
		    opn.counter++;
		    return new OpVal(true);
		}
		else
		    return new OpVal(false);
	    case OpGreaterEqual:
		if(lhs.ge(rhs).bv == true)
		{
		    opn.counter++;
		    return new OpVal(true);
		}
		else
		    return new OpVal(false);
	    case OpNotEqual:
		if(lhs.ne(rhs).bv == true)
		{
		    opn.counter++;
		    return new OpVal(true);
		}
		else
		    return new OpVal(false);
	    case OpEqual:
		if(lhs.eq(rhs).bv == true)
		{
		    opn.counter++;
		    return new OpVal(true);
		}
		else
		    return new OpVal(false);
	    } 
	    
	    return null;
	}
	
	if(isMathOp(opn.op_t))
	{
	    // make sure the logic is not short-circuited, force both
	    // branches to be evaluated
	    //
	    opn.counter++;    // count all additions as passing through this part of the filter

	    if( opn.is_binary )
	    {
		OpVal lhs = (opn.operand[0] == null) ? countHitForNode(opn.op_node[0], s_id) : opn.operand[0].getOpVal(s_id);
		OpVal rhs = (opn.operand[1] == null) ? countHitForNode(opn.op_node[1], s_id) : opn.operand[1].getOpVal(s_id);
		
		try
		{
		    switch(opn.op_t)
		    {
		    case OpAdd:
			return lhs.add(rhs);
		    case OpSubtract:
			return lhs.sub(rhs);
		    case OpMultiply:
			return lhs.mul(rhs);
		    case OpDivide:
			return lhs.div(rhs);
		    }
		}
		catch( java.lang.ArithmeticException ae )
		{
		    if( graph_panel != null )
		    {
			graph_panel.setErrorMsg( ae.toString() );
			graph_panel.repaint(); 
		    }
		    
		    return null;
		}
	    }
	    else
	    {
		OpVal rhs = (opn.operand[1] == null) ? evaluateNode(opn.op_node[1], s_id) : opn.operand[1].getOpVal(s_id);
		
		// must be the unary operator '-'
		
		//if( opn.op_t == OpSubtract )
		return zero.sub( rhs );
		//else
		//	return null;
	    }
	    
	    return null;
	}

	if(isBoolOp(opn.op_t))
	{
	    // make sure the logic is not short-circuited, force both
	    // branches to be evaluated
	    //

	    OpVal lhs = countHitForNode(opn.op_node[0], s_id);
	    OpVal rhs = countHitForNode(opn.op_node[1], s_id);
	
	    if(opn.op_t == OpLogicalAnd)
	    {
		if((lhs.bv==true) && (rhs.bv==true))
		{
		    opn.counter++;
		    return new OpVal(true);
		}
		else
		    return new OpVal(false);
	    }
	    else
	    {
		if((lhs.bv==true) || (rhs.bv==true))
		{
		    opn.counter++;
		    return new OpVal(true);
		}
		else
		    return new OpVal(false);
	    }
	}
	
	return null;
    }
    

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  gubbins
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private maxdView mview;
    private DataPlot dplot;
    private ExprData edata;
    private AnnotationLoader anlo;

    private String init_rule;
    
    private Parser parser;

    private JSplitPane rule_and_graph_jsp;
    private JSplitPane rule_graph_vars_and_ops_jsp;

    private JList history_list;

    private JCheckBox   scale_jchkb;
    private JScrollPane graph_jsp;
    private JTextArea   rule_jta;
    private GraphPanel  graph_panel = null;
    private JFrame      frame = null;
}
