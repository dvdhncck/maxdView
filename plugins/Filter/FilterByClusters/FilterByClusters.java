import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;

public class FilterByClusters implements Plugin, ExprData.Filter, ExprData.ExprDataObserver
{
    public FilterByClusters(maxdView mview_)
    {
	mview = mview_;
	edata = mview.getExprData();
	anlo = mview.getAnnotationLoader();

	filter_mode = mview.getIntProperty("FilterByClusters.mode", NoFilter);
    }

    public void cleanUp()
    {
	edata.removeFilter(this);
	edata.removeObserver(this);

	if(frame != null)
	{
	    mview.putIntProperty("FilterByClusters.mode", filter_mode);
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
	frame.pack();
	frame.setVisible(true);
	edata.addFilter(this);
	edata.addObserver(this);
	startFilteredCounter();
    }

    public void stopPlugin()
    {
	cleanUp();
    }
  
    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("Filter By Clusters", "filter", 
							"Filtering based on cluster elements", "", 
							1, 0, 0);
	return pinf;
    }
    public PluginCommand[] getPluginCommands()
    {
	PluginCommand[] com = new PluginCommand[3];
	
	String[] args = new String[] 
	{ 
	    "filter", "string", "", "m",  "one of 'no filter', 'not in any', 'in one or more', 'in more than one' or 'in less than two'" 
	};
    
	com[0] = new PluginCommand("start", args);
	com[1] = new PluginCommand("set", args);
	com[2] = new PluginCommand("stop", null);
	
	return com;
    }

    public void  runCommand(String name, String[] args, CommandSignal done) 
    { 
	if(name.equals("start") || name.equals("set"))
	{
	    if(frame == null)
		startPlugin();
	    
	    String val = mview.getPluginArg("filter", args);

	    if(val != null)
	    {
		val = val.toLowerCase();

		if(val.startsWith("no "))
		    filter_mode = NoFilter;
		if(val.startsWith("not"))
		    filter_mode = RemoveSpotsInNoCluster;
		if(val.startsWith("in o"))
		    filter_mode = RemoveSpotsInOneOrMoreClusters;
		if(val.startsWith("in m"))
		    filter_mode = RemoveSpotsInMoreThanOneCluster;
		if(val.startsWith("in l"))
		    filter_mode = RemoveSpotsInLessThanTwoClusters;
	    }


	}

	if(name.equals("stop"))
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
	edata.notifyFilterChanged((ExprData.Filter) FilterByClusters.this);
	startFilteredCounter();
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
    // --- --- ---  filter implementation
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
	
    // should return TRUE  if this spot is not to be displayed
    //           and FALSE if the spot is to be displayed
    //
    public boolean filter(int spot_index)
    {
	switch(filter_mode)
	{
	case NoFilter:
	    return true;
	    
	case RemoveSpotsInNoCluster:
	    return (edata.inVisibleClusters(spot_index) < 1);
	    
	case RemoveSpotsInOneOrMoreClusters:
	    return (edata.inVisibleClusters(spot_index) > 0);
	    
	case RemoveSpotsInMoreThanOneCluster:
	    return (edata.inVisibleClusters(spot_index) > 1);

	case RemoveSpotsInLessThanTwoClusters:
	    return (edata.inVisibleClusters(spot_index) < 2);
	}
	return false;
    }

    public boolean enabled()
    { 
	return (filter_mode != NoFilter);
    }

    public String  getName() { return "FilterByClusters"; }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  gooey
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private void addComponents()
    {
	frame = new JFrame("Filter By Clusters");
	
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
	panel.setBorder(BorderFactory.createEmptyBorder(10,10,0,10));
	panel.setLayout(gridbag);
	//panel.setPreferredSize(new Dimension(350, 200));

	int line = 0;

	{
	    JLabel label = new JLabel("Filtering rule:");
	    panel.add(label);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.weighty = c.weightx = 1.0;
	    //c.anchor = GridBagConstraints.NORTH;
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(label, c);
	}
	line++;

	ButtonGroup bg = new ButtonGroup();

	{
	    JRadioButton jrb = new JRadioButton("No filter");
	    jrb.setSelected(filter_mode == NoFilter);
	    jrb.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			setFilterMode(NoFilter);
		    }
		});
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.anchor = GridBagConstraints.WEST;
	    gridbag.setConstraints(jrb, c);
	    bg.add(jrb);
	    panel.add(jrb);
	}
	line++;

	{
	    JLabel label = new JLabel("Hide spots..");
	    panel.add(label);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.anchor = GridBagConstraints.WEST;
	    //c.weighty = c.weightx = 1.0;
	    //c.anchor = GridBagConstraints.NORTH;
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(label, c);
	}
	line++;

	{
	    JRadioButton jrb = new JRadioButton(".. not in any cluster");
	    jrb.setSelected(filter_mode == RemoveSpotsInNoCluster);
	    jrb.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			setFilterMode(RemoveSpotsInNoCluster);
		    }
		});
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.anchor = GridBagConstraints.WEST;
	    gridbag.setConstraints(jrb, c);
	    bg.add(jrb);
	    panel.add(jrb);
	}
	line++;
	
	{
	    JRadioButton jrb = new JRadioButton(".. in one or more clusters");
	    jrb.setSelected(filter_mode == RemoveSpotsInOneOrMoreClusters);
	    jrb.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			setFilterMode(RemoveSpotsInOneOrMoreClusters);
		    }
		});
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.anchor = GridBagConstraints.WEST;
	    gridbag.setConstraints(jrb, c);
	    bg.add(jrb);
	    panel.add(jrb);
	}
	line++;

	{
	    JRadioButton jrb = new JRadioButton(".. in more than one cluster");
	    jrb.setSelected(filter_mode == RemoveSpotsInMoreThanOneCluster);
	    jrb.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			setFilterMode(RemoveSpotsInMoreThanOneCluster);
		    }
		});
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.anchor = GridBagConstraints.WEST;
	    gridbag.setConstraints(jrb, c);
	    bg.add(jrb);
	    panel.add(jrb);
	}
	line++;

	{
	    JRadioButton jrb = new JRadioButton(".. in less than two clusters");
	    jrb.setSelected(filter_mode == RemoveSpotsInLessThanTwoClusters);
	    jrb.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			setFilterMode(RemoveSpotsInLessThanTwoClusters);
		    }
		});
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.anchor = GridBagConstraints.WEST;
	    gridbag.setConstraints(jrb, c);
	    bg.add(jrb);
	    panel.add(jrb);
	}
	line++;

	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout w_gridbag = new GridBagLayout();	
	    wrapper.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
	    wrapper.setLayout(w_gridbag);

	    {
		hit_label = new JLabel("Hit%");
		hit_label.setForeground(new Color(255-48-16,0,0));
		wrapper.add(hit_label);
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0;
		w_gridbag.setConstraints(hit_label, c);

		Dimension fillsize = new Dimension(20,8);
		Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
		c = new GridBagConstraints();
		c.gridx = 1;
		w_gridbag.setConstraints(filler, c);
		wrapper.add(filler);

		miss_label = new JLabel("Miss%");
		miss_label.setForeground(new Color(0,255-32-16,0));
		wrapper.add(miss_label);
		c = new GridBagConstraints();
		c.gridx = 2;
		c.weightx = 1.0;
		w_gridbag.setConstraints(miss_label, c);
	    }

	    panel.add(wrapper);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.weighty = c.weightx = 1.0;
	    //c.anchor = GridBagConstraints.SOUTH;
	    gridbag.setConstraints(wrapper, c);
	    
	}
	line++;

	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout w_gridbag = new GridBagLayout();	
	    wrapper.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
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
		//c.weighty = c.weightx = 1.0;
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
			    mview.getPluginHelpTopic("FilterByClusters", "FilterByClusters");
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		//c.weighty = c.weightx = 1.0;
		w_gridbag.setConstraints(button, c);
	    }

	    panel.add(wrapper);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.weighty = c.weightx = 1.0;
	    //c.anchor = GridBagConstraints.SOUTH;
	    gridbag.setConstraints(wrapper, c);
	    
	}

	frame.getContentPane().add(panel);
    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  filter counting
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void startFilteredCounter()
    {
	if(filtered_count_is_updating == false)
	{
	    filtered_count_is_updating = true;
	    filter_counter_thread  = new FilterCounter();
	    filter_counter_thread.run();
	}
	else
	   filter_counter_thread.restart(); 
    }

    // ---------------- --------------- --------------- ------------- ------------
    // filter counting thread 
    // - runs at a low priority and counts how many spots are removed by
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
	    
	    if(filter_mode > 0)
	    {
		while(check_spot < n_spots)
		{
		    if(filter(check_spot))
			filtered_count++;
		    
		    check_spot++;
		    
		    yield();
		}
	    }

	    filtered_count_is_updating = false;
		
	    if(filter_mode > 0)
	    {
		//System.out.println("filter counting done, " + filtered_count + " Spots filtered");
		
		String hit_pc  = mview.niceDouble((double) (filtered_count * 100) / (double) n_spots, 7, 3);
		String miss_pc = mview.niceDouble((double) ((n_spots-filtered_count) * 100) / (double) n_spots, 7, 3);
		
		if(hit_label != null)
		{
		    hit_label.setText("Fail: " + hit_pc + "%");
		    miss_label.setText("Pass: " + miss_pc + "%");
		}
	    }
	    else
	    {
		hit_label.setText("-");
		miss_label.setText("-");
	    }
	}

	private int check_gene;
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  command handlers
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private void setFilterMode(int new_mode)
    {
	filter_mode = new_mode;
	edata.notifyFilterChanged((ExprData.Filter) FilterByClusters.this);
	startFilteredCounter();
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

    private JLabel miss_label, hit_label;

    public final static int NoFilter                         = 0;
    public final static int RemoveSpotsInNoCluster           = 1;
    public final static int RemoveSpotsInOneOrMoreClusters   = 2;
    public final static int RemoveSpotsInMoreThanOneCluster  = 3;
    public final static int RemoveSpotsInLessThanTwoClusters = 4;

    private int filter_mode = NoFilter;
    
    private JFrame     frame = null;
}
