import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.event.KeyEvent;
import java.lang.reflect.*;
import java.io.*;

public class SimpleMaths implements ExprData.ExprDataObserver, Plugin
{
    public SimpleMaths(maxdView mview_)
    {
	mview = mview_;
	edata = mview.getExprData();

	parser = new Parser();

	parser.setBinaryOperatorSymbols( binary_op_symbols );

	parser.setUnaryOperatorSymbols( unary_op_symbols );
	
	parser.setFunctionSymbols( func_names );

	parser.setIdentifiers( getIdentifiers() );
    }

    public void cleanUp()
    {
	edata.removeObserver(this);

	if(frame != null)
	{
	    Dimension dim = frame.getContentPane().getSize();

	    mview.putIntProperty("SimpleMaths.panel_width", (int) dim.getWidth());
	    mview.putIntProperty("SimpleMaths.panel_height", (int) dim.getHeight());

	    mview.putIntProperty("SimpleMaths.rule_butts_dl", rule_butts_jspltp.getDividerLocation() );
	    mview.putIntProperty("SimpleMaths.rule_dl", rule_jspltp.getDividerLocation() );
	    mview.putIntProperty("SimpleMaths.butts_dl", butts_jspltp.getDividerLocation() );

	    saveRule( new File(mview.getConfigDirectory() + "simple-maths.rule"), true );
	    
	    mview.putBooleanProperty("SimpleMaths.apply_filter", apply_filter_jchkb.isSelected() );
	    
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
	addComponents();
	setupMethods();

	edata.addObserver(this);

	frame.setVisible(true);

	new DelayedLoaderThread().start();
    }

    //
    // for some reason the plugin sometimes does not start properly if the  
    //  loadRule() method is called from within startPlugin()
    //
    // this is assumed to be some form of race condition with the rule panel
    // being updated before it has been initialised in addComponents()
    //
    // to avoid this, the DelayedLoaderThread waits for 250ms before doing the load....
    //

    private class DelayedLoaderThread extends Thread
    {
	public void run()
	{
	    try
	    {
		Thread.yield();
		Thread.sleep(250);
		Thread.yield();

		// System.out.println( "startPlugin(): loading...");
		
		loadRule( new File(mview.getConfigDirectory() + "simple-maths.rule"), true );
	    }
	    catch(InterruptedException ie)
	    {
	    }
	}
    }

    public void stopPlugin()
    {
	cleanUp();
    }
  
    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("Simple Maths", "transform", 
					 "Combine Measurements using mathematical expressions", "",
					 1, 1, 0);
	return pinf;
    }
    public PluginCommand[] getPluginCommands()
    {
	PluginCommand[] com = new PluginCommand[3];
	
	com[0] = new PluginCommand("start", null);
	com[1] = new PluginCommand("stop", null);
	
	String[] args = new String[] 
	{
	    // name         // type     //default   // flag   // comment
	    "expr",         "string",   "",         "m",      "the expression to execute",
	    "apply_filter", "boolean",  "false",    "",       "", 
	    "new_name",     "string",   "",         "m",      "name for new Measurement" 
	};
	
	//com[2] = new PluginCommand("fish", null);
	
	com[2] = new PluginCommand("execute", args);

	return com;
    }

    public void runCommand(String name, String[] args, CommandSignal done) 
    { 
	if(name.equals("start"))
	{
	    startPlugin();
	}

	if(name.equals("stop"))
	{
	    cleanUp();
	}
	
	if(name.equals("execute"))
	{
	    String expr  = mview.getPluginStringArg( "expr", args, null );
	    String nname = mview.getPluginStringArg( "new_name", args, null );
	    boolean af   = mview.getPluginBooleanArg( "apply_filter", args, false );
	    
	    if(expr == null)
	    {
		mview.alertMessage("No expression specified");
	    }
	    else
	    {
		// System.out.println("running '" + expr + "'");
		
		try
		{
		    setupMethods();
		    
		    buildList();
		    
		    execute( expr, af, nname );
		}
		catch( Parser.ParseError pe )
		{
		    mview.alertMessage("Parse error: " + pe.toString() );
		}
	    }
	}

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
    }

    public void measurementUpdate(ExprData.MeasurementUpdateEvent mue)
    {
	switch(mue.event)
	{
	case ExprData.VisibilityChanged:
	case ExprData.SizeChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	case ExprData.NameChanged:
	case ExprData.OrderChanged:
	    parser.setIdentifiers( getIdentifiers() );
	    buildList();
	    parseExpressionAndDisplay();
	    break;
	}
    }

    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
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

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private class MeasSpotAttr 
    {
	public int meas;
	public int attr;

	public MeasSpotAttr(int m, int a) 
	{
	    meas = m; attr = a;
	}
	
    }
    
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

  
    DefaultListModel vars_list_model = null;
    Vector var_string = null;
    String[] token_a = null;
    JList vars_list = null;

    private void buildList()
    {
	vars_list_model = new DefaultListModel();
	var_string = new Vector();
	
	// build the hastable mapping Measurement.names to Measurement indexes
	// (used to detect names in the rule)
	//
	meas_name_to_id_ht = new Hashtable();

	// and also one for mapping Measurement.SpotAttr.names to a pair of indexes
	//
	meas_spot_attr_to_id_ht = new Hashtable();

	for(int mi=0; mi < edata.getNumMeasurements(); mi++)
	{
	    int m = edata.getMeasurementAtIndex(mi);

	    ExprData.Measurement ms = edata.getMeasurement(m);
	    
	    meas_name_to_id_ht.put(ms.getName(), new Integer(m));

	    vars_list_model.addElement(ms.getName());

	    if(isSafe(ms.getName()))
		var_string.addElement(ms.getName());
	    else
		var_string.addElement("\"" + ms.getName() + "\"");

	    for(int sa=0; sa < ms.getNumSpotAttributes(); sa++)
	    {
		int tc = ms.getSpotAttributeDataTypeCode(sa);
		
		if((tc == 0) || (tc == 1))
		{
		    String full_name = ms.getName() + "." + ms.getSpotAttributeName(sa);
		    
		    vars_list_model.addElement("  " + ms.getSpotAttributeName(sa));
		    
		    meas_spot_attr_to_id_ht.put(full_name, new MeasSpotAttr(m, sa));
		    
		    //System.out.println(full_name + " is " + m + "." + sa);
		    //System.out.println(full_name + " is type " + ms.getSpotAttributeDataTypeCode(sa));
		    
		    if(isSafe(full_name))
		    var_string.addElement(full_name);
		    else
			var_string.addElement("\"" + full_name + "\"");
		}
	    }
	}

	if(vars_list != null)
	    vars_list.setModel(vars_list_model);
    }

    private String saved_text  = null;

    private void insertText(String s)
    {
	//int cp = rule_jta.getCaretPosition();

	//rule_jta.insert(s, cp);

	saved_text = rule_jta.getText();

	rule_jta.replaceSelection(s);
	
	rule_jta.requestFocus();
    }

    private void clearText()
    {
	saved_text = rule_jta.getText();
	rule_jta.setText("");
	rule_jta.requestFocus();
    }

    private void undoLastInsert()
    {
	if(saved_text != null)
	{
	    rule_jta.setText(saved_text);
	    rule_jta.requestFocus();
	}
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

   // -------------------------------------------------

    Method[] func_methods = null;

    private void setupMethods()
    {
	func_handler = new FuncHandler();

	func_methods = new Method[ func_names.length ];

	Class[] func_args = new Class[1];
	func_args[0] = FuncData.class;
	
	final Class fh_class = FuncHandler.class;

	for(int f=0; f < func_names.length; f++)
	{
	    try
	    {
		func_methods[f] = fh_class.getMethod( func_names[f], func_args );
	    }
	    catch(NoSuchMethodException nsme)
	    {
		System.out.println("unable to find '" + func_names[f] + "'");
	    }
	}
    }

    public class FuncData  // used as both input and output from functions
    {
	public int spot_id;
	public double result;
	public Expression[] args;
    }

    public class FuncHandler
    {
	public FuncData mean( FuncData fd )
	{
	    if(fd.args.length == 0)
		fd.result = .0;
	    else
	    {
		double acc = .0;
		double count = .0;

		for(int a=0; a < fd.args.length; a++)
		{
		    double tmp =  fd.args[a].evaluate( fd.spot_id );
		    if(!Double.isNaN( tmp ))
		    {
			acc += tmp;
			count += 1.0;
		    }
		}
		fd.result = (count > .0) ? (acc / count) : Double.NaN;
	    }
	    return fd;
	}
	public FuncData min( FuncData fd )
	{
	    boolean first_good = false;
	    fd.result = Double.NaN;

	    for(int a=0; a < fd.args.length; a++)
	    {
		double tmp = fd.args[a].evaluate( fd.spot_id );
		if(!Double.isNaN( tmp ))
		{
		    if(!first_good)
		    {
			first_good = true;
			fd.result = tmp;
		    }
		    else
		    {
			fd.result = (tmp < fd.result) ? tmp : fd.result;
		    }
		}
	    }
	    return fd;
	}

	public FuncData max( FuncData fd )
	{
	    boolean first_good = false;
	    fd.result = Double.NaN;

	    for(int a=0; a < fd.args.length; a++)
	    {
		double tmp = fd.args[a].evaluate( fd.spot_id );
		if(!Double.isNaN( tmp ))
		{
		    if(!first_good)
		    {
			first_good = true;
			fd.result = tmp;
		    }
		    else
		    {
			fd.result = (tmp > fd.result) ? tmp : fd.result;
		    }
		}
	    }
	    return fd;
	}

	public FuncData range( FuncData fd )
	{
	    if(fd.args.length == 0)
		fd.result = .0;
	    else
	    {
		boolean first_good = false;

		double min = .0;
		double max = .0;
		
		for(int a=0; a < fd.args.length; a++)
		{
		    double tmp = fd.args[a].evaluate( fd.spot_id );
		    
		    if(!Double.isNaN( tmp ))
		    {
			if(!first_good)
			{
			    first_good = true;
			    max = min = tmp;
			}
			else
			{
			    min = (tmp < min) ? tmp : min;
			    max = (tmp > max) ? tmp : max;
			}
		    }
		}
		
		fd.result = (max - min);
	    }
	    return fd;
	}
	public FuncData var( FuncData fd )
	{
	    if(fd.args.length == 0)
		fd.result = .0;
	    else
	    {
		double mean = .0;
		double sum_x_sqrd = .0;
		double n = .0;

		for(int a=0; a < fd.args.length; a++)
		{
		    double x    = fd.args[a].evaluate( fd.spot_id );
		    
		    if(!Double.isNaN( x ))
		    {
			mean       += x;
			sum_x_sqrd += (x*x);
			n          += 1.0;
		    }
		}
		
		if(n > .0)
		{
		    mean /= n;
		    
		    double sum_x_sqrd_over_n = sum_x_sqrd  / n;
		    double mean_sqr = mean * mean;
		    
		    fd.result =  sum_x_sqrd_over_n - mean_sqr;
		}
		else
		{
		    fd.result = Double.NaN;
		}
	    }
	    return fd;
	}
	public FuncData stddev( FuncData fd )
	{
	    if(fd.args.length == 0)
		fd.result = .0;
	    else
	    {
		FuncData fd_var = var( fd );
		fd.result = Math.sqrt( fd_var.result );
	    }
	    return fd;
	}
	public FuncData sum( FuncData fd )
	{
	    double acc = .0;

	    for(int a=0; a < fd.args.length; a++)
	    {
		double tmp = fd.args[a].evaluate( fd.spot_id );
		
		if(!Double.isNaN( tmp ))
		{
		    acc += tmp;
		}
	    }

	    fd.result = acc;
	    return fd;
	}
	public FuncData cbrt( FuncData fd )
	{
	    fd.result = ( Math.sqrt( Math.sqrt( fd.args[0].evaluate( fd.spot_id ) ) ) );
	    return fd;
	}
	public FuncData sqrt( FuncData fd )
	{
	    fd.result = ( Math.sqrt( fd.args[0].evaluate( fd.spot_id ) ));
	    return fd;
	}
	public FuncData sqr( FuncData fd )
	{
	    double tmp = fd.args[0].evaluate( fd.spot_id );
	    fd.result = tmp * tmp;
	    return fd;
	}
	public FuncData abs( FuncData fd )
	{
	    fd.result = ( Math.abs( fd.args[0].evaluate( fd.spot_id ) ));
	    
	    return fd;
	}
	public FuncData inv( FuncData fd )
	{
	    fd.result = 1.0 / ( fd.args[0].evaluate( fd.spot_id ) );

	    return fd;
	}
	public FuncData neg( FuncData fd )
	{
	    fd.result = ( - fd.args[0].evaluate( fd.spot_id ) );
	    
	    return fd;
	}
	public FuncData log10( FuncData fd )
	{
	    fd.result = ( Math.log( fd.args[0].evaluate( fd.spot_id ) ) / ln_to_log10_scale );
	    
	    return fd;
	}
	public FuncData alog10( FuncData fd )
	{
	    fd.result = ( Math.exp( fd.args[0].evaluate( fd.spot_id ) * ln_to_log10_scale ) );
	    
	    return fd;
	}
	public FuncData log2( FuncData fd )
	{
	    fd.result = ( Math.log( fd.args[0].evaluate( fd.spot_id ) ) / ln_to_log2_scale );
	    
	    return fd;
	}
	public FuncData alog2( FuncData fd )
	{
	    fd.result = ( Math.exp( fd.args[0].evaluate( fd.spot_id ) * ln_to_log2_scale ) );
	    
	    return fd;
	}
	public FuncData ln( FuncData fd )
	{
	    fd.result = ( Math.log( fd.args[0].evaluate( fd.spot_id ) ));
	    
	    return fd;
	}
	public FuncData exp( FuncData fd )
	{
	    fd.result = ( Math.exp( fd.args[0].evaluate( fd.spot_id ) ));
	    
	    return fd;
	}
	public FuncData pow( FuncData fd )
	{
	    fd.result = ( Math.pow( fd.args[0].evaluate( fd.spot_id ), fd.args[1].evaluate( fd.spot_id ) ));
	    
	    return fd;
	}
	public FuncData sin( FuncData fd )
	{
	    fd.result = ( Math.sin( fd.args[0].evaluate( fd.spot_id ) ));
	    
	    return fd;
	}
	public FuncData cos( FuncData fd )
	{
	    fd.result = ( Math.cos( fd.args[0].evaluate( fd.spot_id ) ));
	    
	    return fd;
	}
	public FuncData tan( FuncData fd )
	{
	    fd.result = ( Math.tan( fd.args[0].evaluate( fd.spot_id ) ));
	    
	    return fd;
	}
	public FuncData ceil( FuncData fd )
	{
	    fd.result = ( Math.ceil( fd.args[0].evaluate( fd.spot_id ) ));
	    
	    return fd;
	}
	public FuncData floor( FuncData fd )
	{
	    fd.result = ( Math.floor( fd.args[0].evaluate( fd.spot_id ) ));
	    
	    return fd;
	}
	public FuncData round( FuncData fd )
	{
	    fd.result = (double) ( Math.round( fd.args[0].evaluate( fd.spot_id ) ));
	    
	    return fd;
	}
	public FuncData asin( FuncData fd )
	{
	    fd.result = ( Math.asin( fd.args[0].evaluate( fd.spot_id ) ));
	    
	    return fd;
	}
	public FuncData acos( FuncData fd )
	{
	   fd.result =  ( Math.acos( fd.args[0].evaluate( fd.spot_id ) ));
	    
	   return fd;
	}
	public FuncData atan( FuncData fd )
	{
	    fd.result = ( Math.atan( fd.args[0].evaluate( fd.spot_id ) ));
	    
	    return fd;
	}
	public FuncData rand( FuncData fd )
	{
	    fd.result = ( Math.random(  ));
	    
	    return fd;
	}
    }

    // -------------------------------------------------

    private void addComponents()
    {
	frame = new JFrame("Simple Maths");
	
	mview.decorateFrame( frame );

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

	{
	    rule_jspltp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

	    {
		rule_jta = new JTextArea(80, 5);
		rule_jta.getDocument().addDocumentListener(new CustomChangeListener());
		rule_jta.setPreferredSize(new Dimension(300, 150));
		JScrollPane jsp = new JScrollPane(rule_jta);
	
		Keymap km = rule_jta.getKeymap();
		//km.addActionForKeyStroke(KeyStroke.getKeyStrokeForEvent(KeyEvent.VK_TAB), new TabAction());
		km.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_TAB,0), new TabAction());
		rule_jta.setKeymap(km);

		rule_jspltp.setTopComponent( jsp );

	    }

	    {
		rule_panel = new RulePanel();

		rule_panel.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));

		rule_panel.setPreferredSize(new Dimension(300, 150));

		rule_jsp = new JScrollPane(rule_panel);

		rule_jspltp.setBottomComponent( rule_jsp );
	    }
	    rule_butts_jspltp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	    rule_butts_jspltp.setTopComponent( rule_jspltp); 

	}

	{
	    butts_jspltp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);


	    vars_list = new JList();
	    
	    MouseListener mouseListener = new MouseAdapter() 
		{
		    public void mouseClicked(MouseEvent e) 
		    {
			    int index = vars_list.locationToIndex(e.getPoint());
			    vars_list.clearSelection();
			    if(index >= 0)
				insertText((String)var_string.elementAt(index) + " ");
		    }
		};
	    vars_list.addMouseListener(mouseListener);
	    
	    buildList();
	    
	    JScrollPane jsp = new JScrollPane(vars_list);
	    

	    butts_jspltp.setLeftComponent( jsp );

	    JPanel butt_wrap = new JPanel();
	    GridBagLayout butt_bag = new GridBagLayout();
	    butt_wrap.setLayout(butt_bag);
	    
	    // - - - - - - - - - - - - - - - - - - - - - - - - -

	    JPanel func_panel = new JPanel();
	    
	    func_panel.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
	    
	    GridBagLayout func_gridbag = new GridBagLayout();
	    func_panel.setLayout(func_gridbag);
	    
	    Font f = func_panel.getFont();
	    Font small_font = new Font(f.getName(), f.getStyle(), f.getSize() - 2);
	    JButton jb = null;
	    GridBagConstraints c = null;

	    int row = 0;
	    int col = 0;

	    Insets insets = new Insets(2,1,2,1);
	    
	    Color func_butt_col = new Color( 234, 224, 180 );
	    col = 0;
	    for(int fn=0; fn < func_names.length; fn++)
	    {
		final int fnf = fn;
		
		jb = new JButton(func_names[fn]);
		jb.setToolTipText( func_desc[ fn ] );
		jb.setMargin(insets);
		jb.addActionListener(new ActionListener() 
		    { 
			public void actionPerformed(ActionEvent e) 
			{ 
			    insertText(func_names[fnf] + "( "); 
			} 
		    } );
		jb.setFont(small_font);
		jb.setBackground(func_butt_col);
		c = new GridBagConstraints();
		c.gridx = col;
		c.gridy = row;
		c.weightx = 2.0;
		c.fill = GridBagConstraints.BOTH;
		func_gridbag.setConstraints(jb, c);
		func_panel.add(jb);
		
		if( ++col == 6 )
		{
		    row++;
		    col = 0;
		}
	    }

	    // --------------------------------
	    
	    c = new GridBagConstraints();
	    c.fill = GridBagConstraints.BOTH;
	    c.weighty = 5.00;
	    c.weightx = 10.0;
	    butt_bag.setConstraints(func_panel, c);
	    butt_wrap.add(func_panel);
	
	    // - - - - - - - - - - - - - - - - - - - - - - - - -

	    JPanel ops_panel = new JPanel();
	    
	    ops_panel.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
	    
	    GridBagLayout ops_gridbag = new GridBagLayout();
	    ops_panel.setLayout(ops_gridbag);
	    
	    row = 0;
	    col = 0;

	    {
		Dimension fillsize = new Dimension(5,5);
		Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
		c = new GridBagConstraints();
		c.gridy = row++;
		ops_gridbag.setConstraints(filler, c);
		ops_panel.add(filler);
	    }


	    final String[] butts = { "7","8","9","C","AC", 
				     "4","5","6", " * ", " / ",
				     "1", "2", "3", " + ", " -" , 
				     "0", ".", ",", " ( ", " ) " };
	    
	    col = 0;
	    int first = row;
	    insets = new Insets(5,2,5,2);

	    Color normal_butt_col = new Color( 219, 202, 177 );
	    Color op_butt_col = new Color( 175, 171, 167 );
	    Color other_butt_col = new Color( 142,142,141 );

	    for(int butt=0; butt < butts.length; butt++)
	    {
		jb = new JButton( butts[ butt ] );
		jb.setMargin(insets);
		final int buttf = butt;

		if(row == first)
		{
		    if(col == 4) // All Clear
		    {
			jb.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { clearText(); } } );
			jb.setBackground( new Color( 232, 101, 97) );
		    }
		    
		    if(col == 3) // Cancel
		    {
			jb.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { undoLastInsert(); } } );
			jb.setBackground( new Color( 232, 184, 97) );
		    }

		    if(col < 3)
		    {
			jb.addActionListener(new ActionListener() 
			    { public void actionPerformed(ActionEvent e) 
				{ insertText(butts[ buttf ]); } 
			    } );
			jb.setBackground( normal_butt_col );
		    }
		}
		else
		{
		    jb.addActionListener(new ActionListener() 
			{ public void actionPerformed(ActionEvent e) 
			    { insertText(butts[ buttf ]); } 
			} );

		    if(butts[ buttf ].equals(" ( ") || butts[ buttf ].equals(" ) "))
		    {
			jb.setBackground( other_butt_col );
		    }
		    else
		    {
			jb.setBackground( (col < 3) ? normal_butt_col : op_butt_col );
		    }
		}
		c = new GridBagConstraints();
		c.gridx = col;
		c.gridy = row;
		c.weightx = (col < 3) ? 3.0 : 1.0;
		c.fill = GridBagConstraints.BOTH;
		ops_gridbag.setConstraints(jb, c);
		ops_panel.add(jb);
		
		if(++col == 5)
		{
		    row++;
		    col = 0;
		}
	    }

	    // --------------------------------
	    
	    c = new GridBagConstraints();
	    c.gridy = 1;
	    c.fill = GridBagConstraints.BOTH;
	    c.weighty = 5.00;
	    c.weightx = 10.0;
	    
	    butt_bag.setConstraints(ops_panel, c);
	    butt_wrap.add(ops_panel);
	
	    
	    // --------------------------------
	    
	    JScrollPane bw_jsp = new JScrollPane(butt_wrap);

	    butts_jspltp.setRightComponent( bw_jsp );

	    rule_butts_jspltp.setBottomComponent( butts_jspltp );


	    c = new GridBagConstraints();
	    c.gridy = 2;
	    c.fill = GridBagConstraints.BOTH;
	    c.weighty = 4.0;
	    c.weightx = 10.0;
	    
	    gridbag.setConstraints(rule_butts_jspltp, c);
	    panel.add(rule_butts_jspltp);

	    
	}
	
	line++;

	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrapper.setLayout(w_gridbag);

	    wrapper.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
	    
	    {
		JButton button = new JButton("Load");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    loadRule();
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 3.0;
		w_gridbag.setConstraints(button, c);
	    }
	    {
		JButton button = new JButton("Save");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    saveRule();
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridy = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 3.0;
		w_gridbag.setConstraints(button, c);
	    }

	    {
		Dimension fillsize = new Dimension(16,16);
		Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		w_gridbag.setConstraints(filler, c);
		wrapper.add(filler);
	    }

	    {
		apply_filter_jchkb = new JCheckBox("Apply filter");
		apply_filter_jchkb.setFont(mview.getSmallFont());
		apply_filter_jchkb.setSelected( mview.getBooleanProperty("SimpleMaths.apply_filter", false) );
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 2;
		w_gridbag.setConstraints(apply_filter_jchkb, c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 4.0;
		wrapper.add(apply_filter_jchkb);
	    }

	    {
		execute_jb = new JButton("Execute");
		wrapper.add(execute_jb);
		execute_jb.setEnabled(false);
		execute_jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    String expr_s = rule_jta.getText();
			
			    try
			    {
				execute( expr_s, apply_filter_jchkb.isSelected(), null );
			    }
			    catch( Parser.ParseError pe )
			    {
				mview.alertMessage("Parse error: " + pe.toString() );
			    }
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 4.0;
		w_gridbag.setConstraints(execute_jb, c);
	    }

	    {
		Dimension fillsize = new Dimension(16,16);
		Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 3;
		w_gridbag.setConstraints(filler, c);
		wrapper.add(filler);
	    }

	    
	    {
		JButton button = new JButton("Help");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    mview.getPluginHelpTopic("SimpleMaths", "SimpleMaths");
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 4;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 3.0;
		w_gridbag.setConstraints(button, c);
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
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 4;
		c.gridy = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 3.0;
		w_gridbag.setConstraints(button, c);
	    }
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridy = 3;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.weightx = 10.0;
	    gridbag.setConstraints(wrapper, c);
	    panel.add(wrapper);
	}

	int iw = mview.getIntProperty("SimpleMaths.panel_width", 400);
	int ih = mview.getIntProperty("SimpleMaths.panel_height", 450);
	
	panel.setPreferredSize(new Dimension( iw, ih ));
	
	frame.getContentPane().add(panel);

	frame.pack();

	rule_butts_jspltp.setDividerLocation( mview.getIntProperty("SimpleMaths.rule_butts_dl", 200) );
	rule_jspltp.setDividerLocation( mview.getIntProperty("SimpleMaths.rule_dl", 100) );
	butts_jspltp.setDividerLocation( mview.getIntProperty("SimpleMaths.butts_dl", 160) );
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
	    parseExpressionAndDisplay();
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

    final Color rp_bg_color = new Color( 220,220,220 );
	    
    private class RulePanel extends JPanel
    {
	public void setExpression( Expression ex )
	{
	    expr = ex;

	    if(getGraphics() == null)
	    {
		System.out.println( "setExpression(): no Graphics object");
	    }

	    FontMetrics fm = getGraphics().getFontMetrics();
	    
	    Dimension psize = null;

	    if(expr != null)
	    {
		// System.out.println( "setExpression(): starting layout");

		try
		{
		    expr.layout( gap, gap, fm );
		
		    // System.out.println( "setExpression(): layout completed");
		    
		    psize = new Dimension(  expr.w + gap, expr.h + gap );
		}
		catch( Exception exception )
		{
		    exception.printStackTrace();
		    psize = new Dimension(300, 100 );
		}

		rule_panel.setPreferredSize( psize );
		rule_panel.setMinimumSize( psize );
		rule_panel.setMaximumSize( psize );
	    }
	    else
	    {
		// System.out.println( "setExpression(): null expression");

		//int sw = fm.stringWidth( ((pe == null) ? " " : pe.toString()) ) + (2*gap);
		//int sh = fm.getAscent() + fm.getDescent() + (2*gap);
		
		//System.out.println("error: " + sw + " x " + sh);

		//psize = new Dimension(  sw, sh );

		JViewport jvp = rule_jsp.getViewport();
		psize = new Dimension(  jvp.getWidth(), jvp.getHeight() );

		rule_panel.setPreferredSize( psize );
		rule_panel.setMinimumSize( psize );
		rule_panel.setMaximumSize( psize );
	    }

	   
	    
	}

	public void setParseError( Parser.ParseError pe_ )
	{
	    pe = pe_;
	}

	public void paintComponent( Graphics g )
	{
	    g.setColor( rp_bg_color );
	    g.fillRect( 0, 0, getWidth(), getHeight() );

	    if(pe != null)
	    {
		FontMetrics fm = getGraphics().getFontMetrics();
		int sw = fm.stringWidth(pe.toString());
		
		int sh = fm.getAscent() + fm.getDescent();

		g.setColor( rp_bg_color );
		g.fillRect( 3, getHeight()-3, sw, sh );

		g.setColor( Color.red );
		g.drawString( pe.toString(), 3,  getHeight()-3);
	    }
	    
	    if(expr != null)
	    {
		try
		{
		    expr.paint( g );
		}
		catch( Exception ex )
		{
		    ex.printStackTrace();
		}
	    }
	}

	private Expression expr;
	private Parser.ParseError pe;
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  



    // codes for the operand types
    //


    final String[] binary_op_symbols = { "*", "/", "+", "-",  };
    
    final String[] unary_op_symbols = { "-" };
    
/*
    final String[] func_names = { "mean", "min", "max", "sum", "range",
				  "sqrt", "sqr", "abs", "neg", "rand",
				  //"log10", "log10^-1", "ln",  "exp", "log2", "log2^-1",
				  "log", "exp", "pow", "ceil", "floor", 
				  "sin", "cos", "tan", "stddev", "var",
				  "asin", "acos", "atan", };
*/

    final String[] func_names = { "mean", "min", "max", 
				  "round", "ceil", "floor", 
				  "sum", "range", "stddev", 
				  "var", "abs", "rand",
				  "sqrt", "sqr", "cbrt", 
				  "pow", "inv", "neg",
				  "ln", "log10", "log2", 
				  "exp", "alog10", "alog2",
				  "sin", "cos", "tan", 
				  "asin", "acos", "atan" };

    final String[] func_desc = { "mean of two or more values", "minimum of two or more values", "maximum of two or more values", 
				 "convert to nearest integer", "convert upwards to integer", "convert downwards to integer", 
				 "sum of two or more values", "range of two or more values (i.e. max-min)", "standard deviation", 
				 "variance", "absolue value", "random value in the range 0...1",
				 "squared", "square root", "cube root", 
				 "power function, pow(a,b) is a to the power of b", "inverse (1/x)", "negative (-x)",
				 "natural log", "log to base 10", "log to base 2", 
				 "exponential", "inverse log to base 10", "inverse log to base 2",
				 "sin", "cos", "tan", 
				 "arcsin", "arccos", "arctan" };

    final int[] func_arg_count  = { -1, -1, -1,    // -1 means 2 or more arguments 
				    1, 1, 1, 
				    -1, -1, -1, 
				    -1, 1, 0,
				    1, 1, 1, 
				    2, 1, 1,
				    1, 1, 1, 
				    1, 1, 1,
				    1, 1, 1, 
				    1, 1, 1 };
    

    private Hashtable meas_name_to_id_ht;
    private Hashtable meas_spot_attr_to_id_ht;

   
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  


    final boolean debug = false;
    final int gap = 3;

    final Color border_col = new Color( 180, 180, 180 );
    final Color var_col    = new Color( 32, 32, 64 );
    final Color const_col  = new Color( 32, 64, 32 );
    final Color func_col   = new Color( 128, 48, 128 );
    final Color op_col     = new Color( 48, 48, 16 );

    private class Expression
    {
	int x,y,w,h,th;    // for layout;

	public void layout( int x_, int y_, FontMetrics fm ) 
	{ 
	    x = x_;
	    y = y_;
	    
	    th = fm.getAscent() /* + fm.getDescent()*/ ;
	    
	    w = fm.stringWidth( toString() ) + gap;
	    h = th /* + gap */;
	}
	
	public void paint( Graphics g ) 
	{ 
	    //g.drawString( toString(), x+(gap/2), y+th+(gap/2) );

	    g.drawString( toString(), x+(gap/2), y+th );

	    //g.setColor( border_col );
	    //g.drawRect( x, y, w, h );
	   
	}

	public String toString()
	{
	    return "?";
	}

	public double evaluate( int spot_id ) 
	{
	    return Double.NaN;
	}
    }

    private class ConstantExpression extends Expression
    {
	double data;   // always double
	
	public ConstantExpression( final String src ) throws Parser.ParseError
	{
	    try
	    {
		data = (double) ( (Integer.valueOf(src)).intValue());
	    }
	    catch(NumberFormatException nfe)
	    {
		try
		{
		    if( src.toLowerCase().equals("nan" ) )
			data = Double.NaN;
		    else
			if( src.toLowerCase().equals("infinity" ) )

			    data = Double.POSITIVE_INFINITY;
			else
			    data = ( (Double.valueOf(src)).doubleValue());
		}
		catch(NumberFormatException nfe2)
		{
		    throw new Parser.ParseError( "Unexpected '" + src + "'");
		}
	    }
	}

	public String toString()
	{
	    return String.valueOf( data );
	}

	public void paint( Graphics g ) 
	{ 
	    g.setColor( const_col );
	    super.paint(g);
	}
	

	public double evaluate( int spot_id )
	{
	    return data;
	}
    }

    private class VariableExpression extends Expression
    {
	public VariableExpression (int m_id )
	{
	    this( m_id, -1 );
	}

	public VariableExpression (int m_id, int sa_id )
	{
	    meas_id      = m_id;
	    spot_attr_id = sa_id;
	}

	public String toString()
	{
	    if(spot_attr_id == -1)
	    {
		return (meas_id >= 0) ? edata.getMeasurementName(meas_id) : "?";
	    }
	    else
	    {
		if(meas_id >= 0)
		    return edata.getMeasurementName(meas_id) + "." + edata.getMeasurement(meas_id).getSpotAttributeName(spot_attr_id);
		else
		    return "?";
	    }
	}

	public void paint( Graphics g ) 
	{ 
	    g.setColor( var_col );
	    super.paint(g);
	}


	public double evaluate( int spot_id )
	{
	    if(spot_attr_id == -1)
	    {
		return (meas_id >= 0) ? edata.eValue(meas_id, spot_id) : Double.NaN;
	    }
	    else
	    {
		if(meas_id >= 0)
		{
		    int dt = edata.getMeasurement(meas_id).getSpotAttributeDataTypeCode(spot_attr_id);
		    Object data = edata.getMeasurement(meas_id).getSpotAttributeData(spot_attr_id);
		    
		    switch(dt)
		    {
		    case 0:
			int[] data_i = (int[]) data;
			return (double) data_i[ spot_id ];
		    case 1:
			double[] data_d = (double[]) data;
			return data_d[ spot_id ];
		    case 2:
			return Double.NaN;
		    default:
			return Double.NaN;
		    }
		}
		else
		{
		    return Double.NaN;
		}
	    }
	}


	int meas_id;
	int spot_attr_id;   // -1 implies not a SpotAttr
    }

    private class FunctionExpression extends Expression
    {
	int function;
	Expression[] arg;
	
	int fnw;          // needed for layout of func with 0 arguments
	int arg_sep_gap;  // how much space between arguments

	public FunctionExpression( int function_, Expression[] arg_ )
	{
	    function = function_;
	    arg = arg_;
	}

	public String toString()
	{
	    String result = func_names[ function ];
	    
	    if( ( arg != null ) || ( arg.length == 0 ) )
	    {
		result += " ( ";
		
		for(int a=0; a < arg.length; a++)
		{
		    if ( a > 0 )
			result += ", ";

		    result += arg[a].toString();
		}
		result += " ) ";
	    }
	    else
	    {
		result += " () ";
	    }

	    return result;
	}

	public void layout( int x_, int y_, FontMetrics fm ) 
	{ 
	    x = x_;
	    y = y_;
	 
	    String prefix = func_names[ function ] + " ( " ;
	    
	    fnw = w = gap + fm.stringWidth( prefix );
	    
	    h = th = fm.getAscent()/* + fm.getDescent()*/;
	    
	    int xp = 0;

	    arg_sep_gap = fm.stringWidth( ", " );

	    if(arg != null)
	    {
		for(int a=0; a < arg.length; a++)
		{
		    if( a > 0 )
			xp += arg_sep_gap;

		    arg[a].layout( x + w + xp, y + gap, fm );

		    xp += arg[a].w;

		    if(arg[a].h > h)
			h = arg[a].h;
		}
	    }
	    else
	    {
		xp = gap;
	    }

	    String suffix = " )" ;
	    
	    w += fm.stringWidth(suffix);

	    h += (2 * gap);

	    w += (xp + gap);
	}

	public void paint( Graphics g ) 
	{ 
	    
	    String prefix = func_names[ function ] + " ( " ;
	    int xp = x + gap;
	    
	    int yp = y + th + gap;

	    g.setColor( func_col );
	    g.drawString( prefix , xp, yp );
	    
	    int last_x = xp + fnw + gap;

	    if(arg != null)
	    {
		for(int a=0; a < arg.length; a++)
		{
		    if( a > 0 )
		    {
			g.drawString( ", " , last_x, yp );
			last_x += arg_sep_gap;
		    }

		    arg[a].paint( g );
		    
		    last_x = (arg[a].x + arg[a].w);
		}
	    }
	    
	    g.setColor( func_col );
	    g.drawString( " )", last_x, yp );
	    
	    g.setColor( border_col );
	    g.drawRect( x, y, w, h );
	}

	public double evaluate( int spot_id ) throws ArithmeticException
	{
	    Object[] args = new Object[1];
	    
	    FuncData fd = new FuncData( );
	    fd.spot_id = spot_id;
	    fd.args = arg;
	    
	    args[0] = fd;
	    
	    try
	    {
		FuncData fd_r = (FuncData) (func_methods[ function ].invoke( func_handler, args ));
		return fd_r.result;
	    }
	    catch( java.lang.reflect.InvocationTargetException ite )
	    {
		return Double.NaN;
	    }
	    catch( IllegalAccessException iae )
	    {
		return Double.NaN;
	    }

	}
    }
    
    private class ComplexExpression extends Expression
    {
	Expression left, right;
	int op_code;

	public ComplexExpression( String operator, Expression lhs, Expression rhs ) throws Parser.ParseError
	{
	    op_code = -1;
	    
	    // note that this ordering must match that of 'binary_op_symbols'

	    if( operator.equals("*") )
		op_code = 0;
	    if( operator.equals("/") )
		op_code = 1;
	    if( operator.equals("+") )
		op_code = 2;
	    if( operator.equals("-") )
		op_code = 3;
	    
	    if( lhs == null ) // unary operator
	    {
		if( ! operator.equals("-") )
		{
		    throw new Parser.ParseError("Unexpected operator '" + operator + "'");
		}
	    }


	    if( op_code == -1 )
		throw new Parser.ParseError("Unexpected operator '" + operator + "'");

	    left  = lhs;
	    right = rhs;
	}


	public String toString()
	{
	    if( left == null )
	    {
		return " (" + binary_op_symbols[ op_code ] + " " + 
		    (right == null ? "?" : right.toString()) +  " )";
	    }
	    else
	    {
		return " (" + left.toString()  + " " + binary_op_symbols[ op_code ] + " " + 
		    (right == null ? "?" : right.toString()) + " )";
	    }
	}
	
	public void layout( int x_, int y_, FontMetrics fm ) 
	{ 
	    x = x_;
	    y = y_;
	    
	    th = fm.getAscent()/* + fm.getDescent()*/;

	    if( left != null )
	    {
		left.layout( x+gap, y+gap, fm );
		
		w = gap + left.w;
	    }

	    w += (gap + fm.stringWidth( binary_op_symbols[ op_code ] ) + gap);

	    right.layout( x + w, y+gap, fm );
	    
	    w += right.w + gap;

	    if( left == null ) // unary operator
	    {
		h = right.h;
	    }
	    else
	    {
		h = (left.h > right.h) ? left.h : right.h;
	    }

	    /*
	    if(left.h > right.h)
	    {
		right.y += ((left.h - right.h) / 2);
	    }
	    else
	    {
		left.y += ((right.h - left.h) / 2);
	    }
	    */

	    h += (2 * gap);
	}

	public void paint( Graphics g ) 
	{ 
	    if( left != null )
		left.paint( g );
	    
	    // int op_h = ( left.h + right.h ) / 2;

	    g.setColor( op_col );

	    int lgap = ( left == null ) ? 0 : left.w + gap;

	    g.drawString( binary_op_symbols[ op_code ], x + gap + lgap, y + gap + th );
	    
	    right.paint( g );

	    g.setColor( border_col );

	    g.drawRect( x, y, w, h );
	}

	public double evaluate( int spot_id )
	{
	    if( left == null )
	    {
		// unary operator: the only one supported is '-'
		switch( op_code )
		{
		case 3:
		    return - right.evaluate( spot_id);		    
		default:
		    return Double.NaN;
		}
	    }
	    else
	    {
		switch( op_code )
		{
		case 0:
		    return left.evaluate( spot_id) * right.evaluate( spot_id);
		case 1:
		    //try
		    //{
		    return left.evaluate( spot_id) / right.evaluate( spot_id);
		    //}
		    //catch( java.math.DivideByZeroException dvze )
		    //{
		    //	return Double.NaN;
		    //}
		case 2:
		    return left.evaluate( spot_id) + right.evaluate( spot_id);
		case 3:
		    return left.evaluate( spot_id) - right.evaluate( spot_id);
		default:
		    return Double.NaN;
		}

	    }
	}
    }

 
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
 

    /*
        take the TermNode generated by the Parser and convert it into an Expression
    */

    private Expression makeExpression( Parser.TermNode term_node ) throws Parser.ParseError
    {
	if( term_node == null )
	    return null;


	if( term_node instanceof Parser.IdentifierTermNode )
	{
	    // is it a known variable name ?


	    final String identifier = ((Parser.IdentifierTermNode) term_node).identifier;

	    final String unquoted_identifier = possiblyRemoveQuotes( identifier );

	    System.out.println("checking identifier " + identifier );

	    MeasSpotAttr msa = (MeasSpotAttr) meas_spot_attr_to_id_ht.get( identifier );

	    if( msa == null )
	    {
		System.out.println("..." + identifier + " not found" );

		System.out.println("also checking identifier " + unquoted_identifier );

		msa = (MeasSpotAttr) meas_spot_attr_to_id_ht.get( identifier );

		/*
		if( msa != null )
		{
		    identifier = unquoted_identifier;
		}
		else
		{
		    System.out.println( "damn, still not found." );
		}
		*/
	    }

	    if(msa == null)
	    {
		Integer m_id = (Integer) meas_name_to_id_ht.get( identifier );
	    
		if(m_id == null)
		{
		    // try the unquoted version
		    m_id = (Integer) meas_name_to_id_ht.get( unquoted_identifier );
		}
		
		if( m_id == null)
		{
		    // not a variable, must be a constant
		    
		    return new ConstantExpression( identifier );
		}
		else
		{
		    return new VariableExpression( m_id.intValue() );
		}
	    }
	    else
	    {
		return new VariableExpression( msa.meas, msa.attr );
	    }
	}


	if( term_node instanceof Parser.BinaryOperatorTermNode )
	{
	    Parser.BinaryOperatorTermNode botn = (Parser.BinaryOperatorTermNode) term_node;

	    return new ComplexExpression( botn.operator, 
					  makeExpression( botn.left_operand ),
					  makeExpression( botn.right_operand ) );
	}


	if( term_node instanceof Parser.UnaryOperatorTermNode )
	{
	    Parser.UnaryOperatorTermNode uotn = (Parser.UnaryOperatorTermNode) term_node;

	    return new ComplexExpression( uotn.operator, 
					  null,
					  makeExpression( uotn.operand ) );

	}


	if( term_node instanceof Parser.FunctionTermNode )
	{
	    Parser.FunctionTermNode ftn = (Parser.FunctionTermNode) term_node;

	    for(int f=0; f < func_names.length; f++)
	    {
		if( func_names[ f ].length() > 0 )
		{
		    if( ftn.function_name.equals( func_names[ f ] ) )
		    {
			if( ftn.argument == null )
			{
			    checkFunctionArguments( f, 0 );
			    
			    return new FunctionExpression( f, null );
			}
			else
			{
			    checkFunctionArguments( f, ftn.argument.length );
			    
			    Expression[] arg = new Expression[ ftn.argument.length ];
			    
			    for(int a=0; a < ftn.argument.length; a ++)
				arg[ a ] = makeExpression( ftn.argument[ a ] );
			    
			    return new FunctionExpression( f, arg );
			}
		    }
		}
	    }
	    
	    throw new Parser.ParseError("Unrecognised function name '" + ftn.function_name + "'");
	}

	throw new Parser.ParseError("Unhandled flavour of Parser.TermNode!");
    }


    private String possiblyRemoveQuotes( String identifier )
    {
	String tmp = identifier.trim();
	
	if( tmp.length() > 2 )
	    if( tmp.charAt(0) == '\"' )
		if( tmp.charAt( tmp.length() - 1 ) == '\"' )
		    return tmp.substring( 1, tmp.length() - 1 );
	
	return identifier;
    }

    private void checkFunctionArguments( int f_code, int n_args ) throws Parser.ParseError
    {
	final int expected_n_args = func_arg_count[ f_code ];

	if( expected_n_args < 0 )
	{
	    if( n_args > 0 )
		return;
	    else
		throw new Parser.ParseError("Function '" + 
					    func_names[ f_code ] + "' requires at least 1 argment" );	
	}

	if( expected_n_args == 0 )
	{
	    if( n_args == 0 )
		return;
	    else
		throw new Parser.ParseError("Function '" + 
					    func_names[ f_code ] + "' takes no arguments" );	

	}

	if( expected_n_args > 0 )
	{
	    if( n_args == expected_n_args )
		return;
	    if( n_args < expected_n_args )
	    {
		throw new Parser.ParseError("Function '" + 
					    func_names[ f_code ] + "' requires " + 
					    expected_n_args + " argment" +
					    ((expected_n_args != 1) ? "s" : ""));
	    }
	    else
	    {
		throw new Parser.ParseError("Function '" + 
					    func_names[ f_code ] + "' allows only " + 
					    expected_n_args + " argment" +
					    ((expected_n_args != 1) ? "s" : ""));
	    }
	}

	throw new Parser.ParseError("Unable to work out whether function arguments are correct");
    }
					    

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void parseExpressionAndDisplay()
    {
	try
	{
	    // System.out.println( "parseExpressionAndDisplay() starts");
	    
	    String expr_s = rule_jta.getText();
	    
	    System.out.println( "parseExpressionAndDisplay(): expr is '" + expr_s + "'" );
	    
	    Parser.TermNode term_node = parser.parse( expr_s );

	    System.out.println( "parseExpressionAndDisplay(): term node is '" + term_node + "'" );

	    Expression root = makeExpression( term_node );
	    
	    // System.out.println( "parseExpressionAndDisplay(): parse done");

	    rule_panel.setExpression( root );
	    
	    rule_panel.setParseError( null );

	    // System.out.println( "parseExpressionAndDisplay(): rule panel updated");

	    if(root == null)
		System.out.println( "root is null" );
		    
	    execute_jb.setEnabled( root != null );
	}
	catch(Parser.ParseError pe)
	{
	    System.out.println( "parseExpressionAndDisplay(): ParseError '" + pe + "'" );

	    rule_panel.setExpression( null );
	    
	    rule_panel.setParseError( pe );

	    execute_jb.setEnabled(false);
	}

	rule_jsp.revalidate();
	rule_jsp.updateUI();
	rule_panel.repaint();
  
	// System.out.println( root.w + "x" + root.h + " @ " + root.x + "," + root.y );
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private void execute(String expr_str, boolean apply_filter, String init_name) throws Parser.ParseError
    {
	try
	{
	    
	    Expression root = makeExpression( parser.parse( expr_str ) );
	    
	    // System.out.println("init_name=" + init_name);

	    if(root != null)
	    {
		String name = null;

		if(init_name == null)
		{
		    init_name = root.toString().trim();
		    
		    if(init_name.length() > 40)
			init_name = init_name.substring(0,40) + "...";
		    
		    name = mview.getString( "Name for new Measurement", init_name);
		}
		else
		{
		    name = init_name;
		}
		
		while(name.length() == 0)
		{
		    mview.alertMessage("You must specify a unique name");
		    name = mview.getString( "Name for new Measurement", init_name);
		}

		final int ns = edata.getNumSpots();

		double[] data = new double[ ns ];

		try
		{
		    for(int s=0; s < ns; s++)
		    {
			if(!apply_filter || !edata.filter(s))
			    data[s] = root.evaluate( s );
			else
			    data[s] = Double.NaN;
		    }

		    ExprData.Measurement meas = edata.new Measurement( name, ExprData.ExpressionAbsoluteDataType, data );
		    
		    meas.setAttribute( "Expression", "SimpleMaths plugin", expr_str );
		    meas.setAttribute( "Expression.ApplyFilter", "SimpleMaths plugin", (apply_filter ? "true" : "false") );
		    
		    edata.addOrderedMeasurement(meas);
		    
		    // make the new Measurement visible
		    mview.getDataPlot().displayMeasurement( meas );
		}
		catch(ArithmeticException ex)
		{
		    mview.alertMessage( ex.toString() + "\n\nNew Measurement not created." );
		}
		catch(Exception ex)
		{
		    // System.out.println( "FunctionExpression:evaluate() " + ex.toString() );
		    
		    mview.alertMessage( "Unexpected error.\n\n" + ex.toString() + "\n\nNew Measurement not created." );

		    ex.printStackTrace();
		}
		
		
		System.gc();
	    }
	}
	catch(UserInputCancelled uic)
	{
	}
    }
    
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private void saveRule()
    {

	JFileChooser fc = mview.getFileChooser();

	int ret_val =  fc.showSaveDialog(mview.getDataPlot()); 

	if(ret_val == JFileChooser.APPROVE_OPTION) 
	{
	    saveRule( fc.getSelectedFile(), false );
	}
    }
    
    private void saveRule( File file, boolean quiet )
    {
	
	try
	{
	    PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter( file )));
	    
	    writer.write( rule_jta.getText() );
	    writer.write( System.getProperty("line.separator") );
	    writer.flush();
	    writer.close();
	}
	catch(IOException ioe)
	{
	    if(!quiet)
		mview.alertMessage("Unable to write to " + file.getPath() );
	}
    }

    private void loadRule()
    {
	JFileChooser fc = mview.getFileChooser();
	
	if(fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
	{
	    loadRule( fc.getSelectedFile(), false );
	}
    }

    private void loadRule( File file, boolean quiet )
    {

	try
	{
	    BufferedReader reader = new BufferedReader(new FileReader(file));
	    
	    String rule = "";
	    
	    //System.out.println("read started");

	    try
	    {
		String str = reader.readLine();
		while((str != null) && (str.length() > 0))
		{
		    if(rule.length() > 0)
			rule += "\n";
		    
		    rule += str;
		    str = reader.readLine();
		}
	    }
	    catch(IOException ioe)
	    {
	    }

	    // System.out.println("read finished, read " +  rule.length() + " chars");

	    rule_jta.setText( rule.trim() );

	    // System.out.println("rule set");

	}
	catch(IOException ioe)
	{
	    if(!quiet)
		mview.alertMessage("Unable to read from " + file.getPath() );
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  gubbins
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  


    //
    //  log{a} x = ( log{b} x ) / ( log{b} a )
    //
    //  so log{2}x = ( log{e} x ) / ( log{e} 2 )
    // 
    //  calculate the constant log{e} 2 as 'ln_to_log2_scale'
    //  and do the same for 'ln_to_log10_scale'
    //
    
    public final static double ln_to_log10_scale = 2.3025850929940456840179914546844;
    public final static double ln_to_log2_scale = 0.69314718055994530941723212145818;


    private maxdView mview;
    private DataPlot dplot;
    private ExprData edata;

    private String init_rule;

    private JScrollPane rule_jsp;
    private RulePanel   rule_panel;
    private JTextArea   rule_jta;
    private JSplitPane  butts_jspltp;
    private JSplitPane  rule_jspltp;
    private JSplitPane  rule_butts_jspltp;
    
    private JCheckBox  apply_filter_jchkb;
    private JButton    execute_jb;

    private FuncHandler func_handler;

    private Parser     parser;

    private JFrame      frame = null;
}
