/***
 *
 *
 * filtering using the OROMatcher regular expression package
 *
 *
 * OROMatcher is Copyright 1997 ORO, Inc.  All rights reserved.
 *
 ***/

//
// 11.05.2001; revised to be threaded
//
import com.oroinc.text.regex.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.awt.event.*;
import javax.swing.event.*;

public class RegExpFilter implements Plugin, ExprData.Filter, ExprData.ExprDataObserver
{
    public RegExpFilter(maxdView mview_)
    {
	mview = mview_;
	edata = mview.getExprData();
	anlo = mview.getAnnotationLoader();
    }

    public void cleanUp()
    {
	mview.putProperty("RegExpFilter.pattern", regexp_jtf.getText());
	mview.putBooleanProperty("RegExpFilter.case_sensitive", case_sens_jchkb.isSelected());
	mview.putBooleanProperty("RegExpFilter.invert_logic", invert_logic_jchkb.isSelected());
	nt_sel.saveSelection("RegExprFilter.source");

	edata.removeFilter(this);
	edata.removeObserver(this);
	
	deleteMatcher();

	shutDownCache();

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
	// try
	{

	    maxdView.CustomClassLoader ucl = (maxdView.CustomClassLoader) getClass().getClassLoader();
	    
	    oromatch_path = mview.getProperty("RegExpFilter.oromatch_path", null);
	    
	    if(oromatch_path == null)
	    {
		oromatch_path = mview.getTopDirectory() + File.separator + "external" + File.separator + 
                               "OROMatcher-1.1.0a" + File.separator;
	    }

	    Class pat_class = null;

	    while(pat_class == null)
	    {
		try
		{
		    if( oromatch_path != null )
		    {
			ucl.addPath( oromatch_path );
		    }

		    pat_class = ucl.findClass("com.oroinc.text.regex.Pattern");
		    
		    if(pat_class == null)
		    {
			try
			{
			    String msg = "Unable to find OROMatcher classes\n";
			    msg += (oromatch_path == null)  ? "\n" : ("in '" + oromatch_path + "'\n");
			    msg += "\nPress \"Find\" to specify the location of the directory,\n" + 
				"  or\nPress \"Cancel\" to stop the plugin.\n" + 
				"\n(see the help page for more information)\n";			
			    
			    
			    if(mview.alertQuestion(msg, "Find", "Cancel") == 1)
				return;
			    
			    oromatch_path = mview.getDirectory("Location of OROMatcher-1.1.0", oromatch_path);
			    oromatch_path += File.separator;
			}
			catch(UserInputCancelled uic)
			{
			    // don't start the plugin
			    return;
			}
		    }
		    else
		    {
			mview.putProperty("RegExpFilter.oromatch_path", oromatch_path);
			
			Class cc = ucl.findClass("com.oroinc.text.regex.Perl5Compiler");
			Class mc = ucl.findClass("com.oroinc.text.regex.Perl5Matcher");
		    }
		}
		catch( java.net.MalformedURLException murle )
		{
		    String msg = 
			"Unable to load OROMatcher classes from '" + oromatch_path + "'\n" +
			"\nPress \"Find\" to specify an alternate location,\n" + 
			"  or\nPress \"Cancel\" to stop the plugin.\n" + 
			"\n(see the help page for more information)\n";			
		    
		    try
		    {
			if(mview.alertQuestion(msg, "Find", "Cancel") == 1)
			{
			    return;
			}
			else
			{
			    oromatch_path = mview.getDirectory("Location of OROMatcher-1.1.0", oromatch_path);
			    oromatch_path += File.separator;
			}
		    }
		    catch( UserInputCancelled uic )
		    {
			return;
		    }


		}
	    }
	    
	    // System.out.println("oromatch path is '" + oromatch_path + "'");

	    // Class cc = ccl.findClass("com.oroinc.text.regex.Perl5Compiler");
	    // Class mc = ccl.findClass("com.oroinc.text.regex.Perl5Matcher");

	    setUpMatcher();
	    addComponents();
	    
	    regexp_jtf.setText(mview.getProperty("RegExpFilter.pattern", ""));
	    case_sens_jchkb.setSelected(mview.getBooleanProperty("RegExpFilter.case_sensitive", false));
	    invert_logic_jchkb.setSelected(mview.getBooleanProperty("RegExpFilter.invert_logic", false));
	    
	    frame.pack();
	    frame.setVisible(true);
	    edata.addFilter(this);
	    edata.addObserver(this);
	    
	    initCache();
	    updateCache();

	}
	/*
	catch(ClassNotFoundException cnfe)
	{
	    if(mview.alertQuestion("RegExpFilter: unable to load Pattren Matcher class from 'OROMatcher'\n" + 
				   "Make sure the OROMatcher classesd visible from the classpath and restart maxdView",
				   "OK", "More details") == 1)
	    { 
		mview.infoMessage("The classpath is currently:\n  '" + System.getProperty("java.class.path") + "'");
	    }
	}
	*/

    }

    public void stopPlugin()
    {
	cleanUp();
    }
  
    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("RegExp Filter", "filter", 
					 "Filter using Regular Expression pattern matching", 
					 "Requires the OROMatcher regular expression package<BR>" +
					 "Copyright 1997 ORO, Inc. All rights reserved.<BR>" +
					 "see <A HREF=\"http://www.oro.com/\">http://www.oro.com/</A>",
					 1, 1, 0);
	return pinf;
    }

    public PluginCommand[] getPluginCommands()
    {
	PluginCommand[] com = new PluginCommand[3];
	
	String[] args = new String[] 
	{
	    // name           // type      //default   // flag   // comment
	    
	    "filter",         "string",        "",        "",          "", 
	    "target",         "name_tag_list", "",        "",          "", 
	    "case_sensitive", "boolean",       "false",   "",          "", 
	    "invert_logic",   "boolean",       "false",   "",          "", 
	};
	
	com[0] = new PluginCommand("set", args);
	
	com[1] = new PluginCommand("start", args);

	com[2] = new PluginCommand("stop", null);

	return com;
    }

    public void   runCommand(String name, String[] args, CommandSignal done) 
    {
	//System.out.println("goodness me, the command '" + name  + "' shall be run....");	

	if(name.equals("set") || name.equals("start"))
	{
	    if(name.equals("start"))
	    {
		startPlugin();
	    }
	    else
	    {
		if(frame == null)
		    startPlugin();
	    }
	    
	    String arg = mview.getPluginArg("filter", args);
	    if(arg != null)
	    {
		if(regexp_jtf != null)
		    regexp_jtf.setText(arg);
		else
		    filter_string = arg;
	    }
	    
	    case_sens_jchkb.setSelected( mview.getPluginBooleanArg("case_sensitive", args, false)); 
	    invert_logic_jchkb.setSelected( mview.getPluginBooleanArg("invert_logic", args, false)); 

	    ExprData.NameTagSelection nts = mview.getPluginNameTagSelectionArg("target", args, null);
	    if(nts != null)
		nt_sel.setNameTagSelection( nts );
	}

	if(name.equals("stop"))
	{
	    cleanUp();
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
	switch(due.event)
	{
	case ExprData.NameChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	    flushCache();
	    break;
	}
    }

    public void clusterUpdate(ExprData.ClusterUpdateEvent cue)
    {
    }

    public void measurementUpdate(ExprData.MeasurementUpdateEvent mue)
    {
    }
    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  result cache
    // --- --- ---  
    // --- --- ---  
    // --- --- ---  stores the filtering results 
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    boolean[] cache_results = null;

    public void flushCache()
    {

	final int ns = edata.getNumSpots();
	if(cache_results == null)
	{
	    cache_results = new boolean[ns];
	}
	else
	{
	    if(cache_results.length != ns)
		cache_results = new boolean[ns];
	}
	for(int s=0; s < ns; s++)
	    cache_results[s] = true;

    }

    public void initCache()
    {
	flushCache();
    }

    public void updateCache()
    {
	
	if(cache_updater_thread != null)
	{
	    //System.out.println("updateCache() killing old");

	   cache_updater_thread.stop = true;
	   
	   // wait for it to stop (or for a certain maximum time)
	   
	   int count = 50; // gives a time-out of 5 seconds
	   while((cache_updater_thread.is_updating) && (count-- > 0))
	   {
	       try
	       {
		   Thread.sleep(100);
	       }
	       catch(InterruptedException ie)
	       {
	       }
	   }

	   if(cache_updater_thread.is_updating)
	       System.out.println("updateCache() timeout - thread didn't stop as requested");
	}

	//System.out.println("updateCache() starting new");
	
	cache_updater_thread  = new CacheUpdater();
	cache_updater_thread.start();
    }
    
    public void shutDownCache()
    {
	if(cache_updater_thread != null)
	{
	    cache_updater_thread.stop = true;
	}
    }

    private CacheUpdater cache_updater_thread = null;
 
    public class CacheUpdater extends Thread
    {
	public int filtered_count;
	public int check_spot;
	public boolean stop;
	public boolean is_updating;

	private int update_event_freq;
	private int update_label_freq;

	public CacheUpdater()
	{
	    //setPriority((Thread.MIN_PRIORITY+Thread.MAX_PRIORITY) / 2);
	    //setPriority(Thread.MIN_PRIORITY);
	    reset();
	}

	public void reset()
	{
	    final int n_spots = edata.getNumSpots();

	    stop = false;
	    filtered_count = 0;
	    check_spot = 0;
	    
	    // System.out.println("filter counting reset...");
	    
	    update_event_freq = (n_spots / 4);
	    if(update_event_freq < 1)
		update_event_freq = 1;
	    update_label_freq = (n_spots / 50);
	    if(update_label_freq < 1)
		update_label_freq = 1;
	}

	public void run()
	{
	    frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	    is_updating = true;

	    flushCache();
	    
	    final int n_spots = edata.getNumSpots();

	    hit_label.setText("Busy...");
	    
	    while(!stop && (check_spot < n_spots))
	    {
		if((cache_results[check_spot] = doFilter(check_spot)) == true)
		{
		    filtered_count++;
		}

		check_spot++;
		
		if((check_spot % update_label_freq) == 0)
		{
		    String pc =  mview.niceDouble( (double)check_spot*100.0 / (double) n_spots, 7, 3) + "%";
		    // System.out.println(pc);
		    miss_label.setText(pc);
		    yield();
		}

		if((check_spot % update_event_freq) == 0)
		{
		    // System.out.println("sending filter update event");
		   edata.notifyFilterChanged((ExprData.Filter) RegExpFilter.this);
		   yield();
		}
		
		
	    }

	    if(!stop)
	    {
		edata.notifyFilterChanged((ExprData.Filter) RegExpFilter.this);

		//System.out.println("filter counting done, " + filtered_count + " Spots filtered");
		
		String hit_pc  = mview.niceDouble((double) (filtered_count * 100) / (double) n_spots, 7, 3);
		String miss_pc = mview.niceDouble((double) ((n_spots-filtered_count) * 100) / (double) n_spots, 7, 3);
		
		hit_label.setText("Fail: " + hit_pc + "%");
		miss_label.setText("Pass: " + miss_pc + "%");
	    }

	    //System.out.println("counting finished...");

	    is_updating = false;
	    frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	    
	}

	private int check_gene;
    }
 
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  filter implementation
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
	
    public boolean filter(int spot_index)
    {
	if((cache_results == null) || (spot_index >= cache_results.length))
	    return false;
	else
	    return cache_results[spot_index];

    }
    // should return TRUE  if this spot is not to be displayed
    //           and FALSE if the spot is to be displayed
    //
    public boolean doFilter(int spot_index)
    {
	if(pattern == null)
	    return invert_logic;  // false;

	if(nt_sel.userOptionSelected())
	{
	    String ann = anlo.loadAnnotationFromCache(spot_index);
	    
	    if(ann != null)
	    {
		if(matcher.contains(ann, pattern)) 
		{
		    return invert_logic;  // false;
		}
	    }
	}
	else
	{
	    String nts = nt_selection.getNameTag(spot_index);
	    if(nts != null)
	    {
		if(matcher.contains(nts, pattern)) 
		{
		    return invert_logic;  // false;
		}
	    }
	}
	
	{
	    // no match, spot should not be displayed
	    return !invert_logic;  // true;
	}
    }

    public boolean enabled()
    { 
	//return (gene_jchkb.isSelected() || ann_jchkb.isSelected() || spot_com_jchkb.isSelected() || probe_jchkb.isSelected() || spot_jchkb.isSelected());
	
	return ( nt_sel.userOptionSelected() || (nt_selection.getNames() != null));
    }

    public String  getName() { return "RegExp"; }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  gooey
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private void addComponents()
    {
	frame = new JFrame("RegExp Filter");
	
	mview.decorateFrame(frame);

	frame.addWindowListener(new WindowAdapter() 
			  {
			      public void windowClosing(WindowEvent e)
			      {
				  cleanUp();
			      }
			  });

	GridBagLayout gridbag = new GridBagLayout();

	DragAndDropPanel panel = new DragAndDropPanel();
	
	/*
	panel.setEntityAdaptor(new DragAndDropPanel.EntityAdaptor()
	    {
		public String getName(DragAndDropEntity dnde)
		{
		    return dnde.getName(edata);
		}
	    });
	*/
	panel.setDropAction(new DragAndDropPanel.DropAction()
	    {
		public void dropped(DragAndDropEntity dnde)
		{
		    regexp_jtf.setText(dnde.getName(edata));
		}
	    });

	panel.setDragAction(new DragAndDropPanel.DragAction()
	    {
		public DragAndDropEntity getEntity(java.awt.dnd.DragGestureEvent dge)
		{
		    if(edata.getNumSpots() > 0)
			return DragAndDropEntity.createSpotNameEntity(edata.getSpotAtIndex(0));
		    else
			return null;
		}
	    });

	panel.setLayout(gridbag);
	panel.setPreferredSize(new Dimension(350, 200));

	int n_cols = 2;
	int line = 0;

	{
	    JLabel label = new JLabel("Regular Expression Matcher (Perl5 syntax)");
	    panel.add(label);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.gridwidth = n_cols;
	    c.weighty = c.weightx = 1.0;
	    //c.anchor = GridBagConstraints.SOUTH;
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(label, c);
	}
	line++;

	{
	    nt_sel = new NameTagSelector(mview, "Annotation");
	    nt_sel.loadSelection("RegExprFilter.source");
	    nt_selection = nt_sel.getNameTagSelection();
	    
	    nt_sel.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			nt_selection = nt_sel.getNameTagSelection();
			updateCache();
		    }
		});
	    panel.add(nt_sel);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.gridwidth = n_cols;
	    c.weightx = 1.0;
	    gridbag.setConstraints(nt_sel, c);
	}
	line++;

	{
	    regexp_jtf = new JTextField(20);

	    regexp_jtf.getDocument().addDocumentListener(new CustomChangeListener());

	    panel.add(regexp_jtf);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.gridwidth = n_cols;
	    c.weighty = c.weightx = 1.0;
	    //c.anchor = GridBagConstraints.NORTH;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(regexp_jtf, c);
	}
	line++;
	
	{
	    error_label = new JLabel("");
	    panel.add(error_label);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.gridwidth = n_cols;
	    c.anchor = GridBagConstraints.NORTH;
	    c.weighty = c.weightx = 1.0;
	    gridbag.setConstraints(error_label, c);
	}
	line++;

	{
	    case_sens_jchkb = new JCheckBox("Case sensitive");
	    case_sens_jchkb.setHorizontalTextPosition(SwingConstants.LEFT);
	    panel.add(case_sens_jchkb);
	    case_sens_jchkb.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			setString(filter_string);
			updateCache();
		    }
		});
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.anchor = GridBagConstraints.NORTHEAST;
	    c.weighty = c.weightx = 1.0;
	    gridbag.setConstraints(case_sens_jchkb, c);
	}

	
	{
	    invert_logic_jchkb = new JCheckBox("Invert logic");
	    panel.add(invert_logic_jchkb);
	    invert_logic_jchkb.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			invert_logic = invert_logic_jchkb.isSelected();
			updateCache();
		    }
		});
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.anchor = GridBagConstraints.NORTHWEST;
	    c.weighty = c.weightx = 1.0;
	    gridbag.setConstraints(invert_logic_jchkb, c);
	}

	line++;

	
	{
	    hit_label = new JLabel("Hit%");
	    hit_label.setForeground(new Color(255-48-16,0,0));
	    panel.add(hit_label);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.weightx = 1.0;
	    //c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(hit_label, c);
	}

	{
	    miss_label = new JLabel("Miss%");
	    miss_label.setForeground(new Color(0,255-32-16,0));
	    panel.add(miss_label);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    //c.anchor = GridBagConstraints.WEST;
	    c.weightx = 1.0;
	    gridbag.setConstraints(miss_label, c);
	}

	line++;

	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrapper.setLayout(w_gridbag);

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
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		//c.anchor = GridBagConstraints.EAST;
		w_gridbag.setConstraints(button, c);
	    }
	    /*
	    {
		JButton button = new JButton("Filter");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		w_gridbag.setConstraints(button, c);
	    }
	    */
	    {
		JButton button = new JButton("Help");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    mview.getPluginHelpTopic("RegExpFilter", "RegExpFilter");
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		w_gridbag.setConstraints(button, c);
	    }

	    panel.add(wrapper);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.gridwidth = n_cols;
	    c.weighty = c.weightx = 1.0;
	    gridbag.setConstraints(wrapper, c);
	    
	}

	frame.getContentPane().add(panel);
    }

    // handles any changes in text field
    //
    class CustomChangeListener implements DocumentListener 
    {
	public void insertUpdate(DocumentEvent e)  { propagate(e); }
	public void removeUpdate(DocumentEvent e)  { propagate(e); }
	public void changedUpdate(DocumentEvent e) { propagate(e); }

	private void propagate(DocumentEvent e)
	{
	    try
	    {
		filter_string = e.getDocument().getText(0, e.getDocument().getLength());
		setString(filter_string);
		
		if(enabled())
		{
		    //System.out.println("filter: " + filter_string);
		    
		    // notify the filter control that we have changed
		    
		    updateCache();
		}
	    }
	    catch (javax.swing.text.BadLocationException ble)
	    {
		System.out.println("wierd string....\n");
	    }
	}
    }

    // ---------------- ---------------- --------------- --------------- ------------- ------------
    // ---------------- ---------------- --------------- --------------- ------------- ------------

    /*
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
    */

    // ---------------- --------------- --------------- ------------- ------------
    // filter counting thread 
    // - runs at a low priority and counts how many spots are removed by
    //   _this_ filter
    //
    /*
    private boolean       filtered_count_is_updating = false;
    private FilterCounter filter_counter_thread = null;
    private int           filtered_count = 0;


    public class FilterCounter extends Thread
    {
	public FilterCounter()
	{
	    super();
	    //System.out.println("FilterCounter created");
	    setPriority(Thread.MIN_PRIORITY);
	    restart();
	}
	public synchronized void restart()
	{
	    filtered_count = 0;
	    check_spot = 0;
	    //System.out.println("filter counting restarts...");
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
	    //System.out.println("filter counting done, " + filtered_count + " Spots filtered");

	    String hit_pc  = mview.niceDouble((double) (filtered_count * 100) / (double) n_spots, 7, 3);
	    String miss_pc = mview.niceDouble((double) ((n_spots-filtered_count) * 100) / (double) n_spots, 7, 3);
	    
	    hit_label.setText("Fail: " + hit_pc + "%");
	    miss_label.setText("Pass: " + miss_pc + "%");

	}

	private int check_gene;
    }
    */

    // ---------------- ---------------- --------------- --------------- ------------- ------------
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  command handlers
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private Perl5Compiler compiler = null;
    private Perl5Matcher  matcher = null;
    private Pattern       pattern = null;

    private void setUpMatcher()
    {
	compiler = new Perl5Compiler();
	matcher  = new Perl5Matcher();

    }

    private void deleteMatcher()
    {
	compiler = null;
	matcher  = null;
    }

    private void setString(String s)
    {
	try 
	{
	    if(case_sens_jchkb.isSelected())
		pattern = compiler.compile(s);
	    else
		pattern = compiler.compile(s, Perl5Compiler.CASE_INSENSITIVE_MASK);
	    error_label.setText("");
	} 
	catch(Exception e) 
	{
	    error_label.setForeground(Color.red);
	    error_label.setText("Bad pattern:" + e.getMessage());
	}
	
    }

    /*
    public static final void main(String[] argv) 
    {
	RegExpFilter ref = new RegExpFilter(null);
	ref.startPlugin();
    }
    */

    /*
    public void test() 
    {
    int matches = 0;
    String numberExpression = "\\d+";
    String exactMatch = "2010";
    String containsMatches = "  2001 was the movie before 2010, which takes place before 2069 the book ";
    Pattern pattern   = null;
    PatternMatcherInput input;
    PatternCompiler compiler;
    PatternMatcher matcher;
    MatchResult result;

    // Create Perl5Compiler and Perl5Matcher instances.
    compiler = new Perl5Compiler();
    matcher  = new Perl5Matcher();

    // Attempt to compile the pattern.  If the pattern is not valid,
    // report the error and exit.
    try {
      pattern = compiler.compile(numberExpression);
    } catch(MalformedPatternException e) {
      System.err.println("Bad pattern.");
      System.err.println(e.getMessage());
      System.exit(1);
    }

    // Here we show the difference between the matches() and contains()
    // methods().  Compile the program and study the output to reinforce
    // in your mind what the methods do.

    System.out.println("Input: " + exactMatch);

    // The following should return true because exactMatch exactly matches
    // numberExprssion.

    if(matcher.matches(exactMatch, pattern))
      System.out.println("matches() Result: TRUE, EXACT MATCH");
    else
      System.out.println("matches() Result: FALSE, NOT EXACT MATCH");

    System.out.println("\nInput: " + containsMatches);

    // The following should return false because containsMatches does not
    // exactly match numberExpression even though its subparts do.

    if(matcher.matches(containsMatches, pattern))
      System.out.println("matches() Result: TRUE, EXACT MATCH");
    else
      System.out.println("matches() Result: FALSE, NOT EXACT MATCH");


    // Now we call the contains() method.  contains() should return true
    // for both strings.

    System.out.println("\nInput: " + exactMatch);

    if(matcher.contains(exactMatch, pattern)) {
      System.out.println("contains() Result: TRUE");

      // Fetch match and print.
      result = matcher.getMatch();
      System.out.println("Match: " + result);
    } else
      System.out.println("contains() Result: FALSE");

    System.out.println("\nInput: " + containsMatches);

    if(matcher.contains(containsMatches, pattern)) {
      System.out.println("contains() Result: TRUE");
      // Fetch match and print.
      result = matcher.getMatch();
      System.out.println("Match: " + result);
    } else
      System.out.println("contains() Result: FALSE");


    // In the previous example, notice how contains() will fetch only first 
    // match in a string.  If you want to search a string for all of the
    // matches it contains, you must create a PatternMatcherInput object
    // to keep track of the position of the last match, so you can pick
    // up a search where the last one left off.

    input   = new PatternMatcherInput(containsMatches);

    System.out.println("\nPatternMatcherInput: " + input);
    // Loop until there are no more matches left.
    while(matcher.contains(input, pattern)) 
    {
	// Since we're still in the loop, fetch match that was found.
	result = matcher.getMatch();  
	
	++matches;
	
	System.out.println("Match " + matches + ": " + result);
    }
    }
    */

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

    private String oromatch_path = null;

    private String filter_string = null;
    private boolean invert_logic = false;

    private JFrame     frame = null;
    private JTextField regexp_jtf;
    private JCheckBox  case_sens_jchkb;
    private JLabel     hit_label, miss_label, error_label;

    private NameTagSelector nt_sel;
    private ExprData.NameTagSelection nt_selection;

    private JCheckBox nts_jchkb;

    private JCheckBox invert_logic_jchkb;

}
