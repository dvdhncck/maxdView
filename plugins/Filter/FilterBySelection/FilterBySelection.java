import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;

public class FilterBySelection implements Plugin, ExprData.Filter, ExprData.ExternalSelectionListener
{
    public FilterBySelection(maxdView mview_)
    {
	mview = mview_;
	edata = mview.getExprData();
	anlo = mview.getAnnotationLoader();

	filter_mode = mview.getIntProperty("FilterBySelection.mode", NoFilter);
    }

    public void cleanUp()
    {
	edata.removeExternalSelectionListener(esl_handle);
	edata.removeFilter(this);

	if(frame != null)
	{
	    mview.putIntProperty("FilterBySelection.mode", filter_mode);
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
	esl_handle = edata.addExternalSelectionListener(this);
	startFilteredCounter();
    }

    public void stopPlugin()
    {
	cleanUp();
    }
  
    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("Filter By Selection", "filter", 
					 "Filtering based on the selected spots", "", 
					 1, 0, 0);
	return pinf;
    }
    public PluginCommand[] getPluginCommands()
    {
	PluginCommand[] com = new PluginCommand[2];
	
	String[] args = new String[] 
	{ 
	    "filter", "string", "", "m",  "one of 'no filter', 'selected' or 'unselected'" 
	};
 
	
	com[0] = new PluginCommand("start", args);
	com[1] = new PluginCommand("stop", null);
	
	return com;
    }

    public void  runCommand(String name, String[] args, CommandSignal done) 
    { 
	if(name.equals("start"))
	{
	    String val = mview.getPluginArg("filter", args);

	    if(val != null)
	    {
		val = val.toLowerCase();

		if(val.startsWith("no "))
		    filter_mode = NoFilter;
		if(val.startsWith("uns"))
		    filter_mode = RemoveUnselectedSpots;
		if(val.startsWith("sel"))
		    filter_mode = RemoveSelectedSpots;
	    }

	    startPlugin();
	}

	if(name.equals("stop"))
	    cleanUp();

	if(done != null)
	    done.signal();
    } 

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  ExternalSelectionListener implementation
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    
    public void spotSelectionChanged(int[] spots_ids)
    {
	edata.notifyFilterChanged((ExprData.Filter) FilterBySelection.this);
	startFilteredCounter();
    }

    public void clusterSelectionChanged(ExprData.Cluster[] clusters)
    {
    }

    public void spotMeasurementSelectionChanged(int[] spot_ids, int[] meas_ids)
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
	    
	case RemoveUnselectedSpots:
	    return ! edata.isSpotSelected(spot_index);
	    
	case RemoveSelectedSpots:
	    return edata.isSpotSelected(spot_index);
	}
	return false;
    }

    public boolean enabled()
    { 
	return (filter_mode != NoFilter);
    }

    public String  getName() { return "FilterBySelection"; }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  gooey
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private void addComponents()
    {
	frame = new JFrame("Filter By Selection");
	
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
	    JRadioButton jrb = new JRadioButton("Hide unselected Spots");
	    jrb.setSelected(filter_mode == RemoveUnselectedSpots);
	    jrb.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			setFilterMode(RemoveUnselectedSpots);
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
	    JRadioButton jrb = new JRadioButton("Hide selected Spots");
	    jrb.setSelected(filter_mode == RemoveSelectedSpots);
	    jrb.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			setFilterMode(RemoveSelectedSpots);
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
	    wrapper.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
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
			    mview.getPluginHelpTopic("FilterBySelection", "FilterBySelection");
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
	edata.notifyFilterChanged((ExprData.Filter) FilterBySelection.this);
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

    private int esl_handle;

    private JLabel miss_label, hit_label;

    public final static int NoFilter                       = 0;
    public final static int RemoveUnselectedSpots          = 1;
    public final static int RemoveSelectedSpots            = 2;


    private int filter_mode = NoFilter;
    
    private JFrame     frame = null;
}
