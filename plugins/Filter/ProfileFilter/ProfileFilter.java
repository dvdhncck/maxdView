import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.*;

public class ProfileFilter implements Plugin, ExprData.Filter, ExprData.ExprDataObserver
{
    final public boolean debug = false;

    public ProfileFilter(maxdView mview_)
    {
	mview = mview_;
	edata = mview.getExprData();
    }

    public void cleanUp()
    {
	edata.removeFilter(this);
	edata.removeObserver(this);

	nt_sel.saveSelection("ProfileFilter.source");

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
	n_similar_spots = 10;

	addComponents();
	
	frame.pack();
	frame.setVisible(true);
	edata.addFilter(this);
	edata.addObserver(this);
    }

    public void stopPlugin()
    {
	cleanUp();
    }
  
    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("Profile Filter", "filter", 
					 "Filter using expression profile matching", "",
					 1, 0, 0);
	return pinf;
    }

    public PluginCommand[] getPluginCommands()
    {
	PluginCommand[] com = new PluginCommand[2];
	
	String[] args = new String[] 
	{ 
	    // name                // type               //default   // flag   // comment

	    "select",             "string",              "all",       "",   "either 'all' or 'list'",
	    "measurements",       "measurement_list",     "",         "",   "used when 'select'='list'",
	    "target_spot_tag",    "string",               "",         "",   "which Name or Name Attrbiute",
	    "target_spot_value",  "string",               "",         "",   "the value", 
	    "n_spots",            "integer",              "10",       "",   "how many nearest matches",
	    "metric",             "string",               "distance", "",   "one of 'distance', 'slope' or 'direction'"
	};
	
	com[0] = new PluginCommand("start", args);
	com[1] = new PluginCommand("stop", null);
	
	return com;
    }

    public void runCommand(String name, String[] args, CommandSignal done) 
    {
	if(name.equals("start"))
	{
	    startPlugin();

	    String tspotn = mview.getPluginStringArg("target_spot_tag", args, null);
	    if(tspotn != null)
	    {
		nt_selection.setNames( tspotn );
		nt_sel.setNameTagSelection( nt_selection );
	    }
	    
	    String tspotv = mview.getPluginStringArg("target_spot_value", args, null);

	    if(tspotv != null)
		spot_list.setSelectedValue( tspotv, true );
	
	    String sel =  mview.getPluginStringArg( "select", args, null );
	    if(sel != null)
	    {
		if(sel.startsWith("li"))
		{
		    String[] m_names = mview.getPluginMeasurementListArg( "measurements", args, null );
		    
		    meas_list.selectItems( m_names);
		}
		else
		{
		    meas_list.selectAll();
		}
	    }


	    n_spots_jtf.setText( mview.getPluginArg("n_spots", args, "10") );
	    
	    String mets = mview.getPluginStringArg("metric", args, "distance");
	    if(mets.startsWith("dis"))
		distance_metric_jcb.setSelectedIndex(0);
	    if(mets.startsWith("slo"))
		distance_metric_jcb.setSelectedIndex(1);
	    if(mets.startsWith("dir"))
		distance_metric_jcb.setSelectedIndex(2);

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
	switch(due.event)
	{
	case ExprData.OrderChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	    if(spot_list != null)
	    {
		spot_list.setModel(new SpotListModel());
		startUpdate();
	    }
	    break;
		    
	}
    }

    public void clusterUpdate(ExprData.ClusterUpdateEvent cue)
    {
    }

    public void measurementUpdate(ExprData.MeasurementUpdateEvent mue)
    {
	switch(mue.event)
	{
	case ExprData.ColourChanged:
	case ExprData.VisibilityChanged:
	    break;
	case ExprData.OrderChanged:
	case ExprData.SizeChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	    if(meas_list != null)
	    {
		System.out.println("meas update!");
		populateListWithMeasurements(meas_list);
		meas_list.revalidate();
		startUpdate();
	    }
	    break;
	}
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
	if(filter_res == null)
	{
	    return false;
	}
	else
	{
	    if(spot_index < filter_res.length)
		return filter_res[spot_index];
	    else
		return false;
	}
    }

    public boolean enabled()
    { 
	return true;
    }

    public String  getName() { return "Profile"; }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  gooey
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private void addComponents()
    {
	frame = new JFrame("Profile Filter");
	
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
	
	panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	
	panel.setLayout(gridbag);
	panel.setPreferredSize(new Dimension(450, 300));

	int line = 0;
	
	{
	    GridBagConstraints c;
	    
	    {
		JLabel jb = new JLabel("Measurements");
		 c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.gridwidth = 3;
		c.weightx = 1.0;
		gridbag.setConstraints(jb, c);
		panel.add(jb);
	    }
	    {
		JLabel jb = new JLabel("Target Spot");
		c = new GridBagConstraints();
		c.gridx = 3;
		c.gridy = line;
		c.gridwidth = 2;
		c.weightx = 1.0;
		gridbag.setConstraints(jb, c);
		panel.add(jb);
	    }

	    line++;

	    {
		JButton jb = new JButton("All");
		jb.setFont(mview.getSmallFont());
		jb.addActionListener(new ActionListener() 
		    { 
			public void actionPerformed(ActionEvent e) 
			{
			    meas_list.setSelectionInterval(0, edata.getNumMeasurements()-1);
			}
		    });
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.EAST;
		gridbag.setConstraints(jb, c);
		panel.add(jb);
	    }
	    {
		JButton jb = new JButton("None");
		jb.setFont(mview.getSmallFont());
		jb.addActionListener(new ActionListener() 
		    { 
			public void actionPerformed(ActionEvent e) 
			{
			    meas_list.setSelectedIndices(new int[0]);
			}
		    });
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line;
		gridbag.setConstraints(jb, c);
		panel.add(jb);
	    }
	    {
		JButton jb = new JButton("Invert");
		jb.setFont(mview.getSmallFont());
		jb.addActionListener(new ActionListener() 
		    { 
			public void actionPerformed(ActionEvent e) 
			{
			    int[] ids = meas_list.getSelectedIndices();
			    final int nm = edata.getNumMeasurements();
			    
			    boolean[] inv = new boolean[ nm ];
			    for(int m=0; m < ids.length; m++)
				inv[ids[m]] = true;
			    
			    int[] inv_ids = new int[ nm - ids.length ];
			    int i = 0;
			    
			    for(int m=0; m < nm; m++)
				if(inv[m] == false)
				    inv_ids[i++] = m;
			    
			    meas_list.removeListSelectionListener(meas_list_al);
			    meas_list.setSelectedIndices(inv_ids);
			    meas_list.addListSelectionListener(meas_list_al);
			    startUpdate();
			}
		    });
		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = line;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		gridbag.setConstraints(jb, c);
		panel.add(jb);
	    }

	    {
		JLabel jb = new JLabel("Show ");
		jb.setFont(mview.getSmallFont());
		c = new GridBagConstraints();
		c.gridx = 3;
		c.gridy = line;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.EAST;
		gridbag.setConstraints(jb, c);
		panel.add(jb);
	    }
	    {
		nt_sel = new NameTagSelector(mview);
		nt_sel.setFont(mview.getSmallFont());
		nt_sel.loadSelection("ProfileFilter.source");
		nt_sel.addActionListener(new ActionListener() 
		    { 
			public void actionPerformed(ActionEvent e) 
			{
			    nt_selection = nt_sel.getNameTagSelection();
			    spot_list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
			    spot_list.setModel(new SpotListModel());
			}
		    });
		nt_selection = nt_sel.getNameTagSelection();
		c = new GridBagConstraints();
		c.gridx = 4;
		c.gridy = line;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		//c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(nt_sel, c);
		panel.add(nt_sel);
	    }

	    line++;
	}

	{
	    Dimension fill_size = new Dimension(10,10);
	    Box.Filler filler = new Box.Filler(fill_size, fill_size, fill_size);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.gridwidth = 5;
	    gridbag.setConstraints(filler, c);
	    panel.add(filler);
	    
	    line++;
	}

	{
	    meas_list = new DragAndDropList();
	    JScrollPane jsp = new JScrollPane(meas_list);
	    populateListWithMeasurements(meas_list); // setModel(new MeasListModel());
	    meas_list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	    meas_list_al = new CustomListSelectionListener() ;
	    meas_list.addListSelectionListener(meas_list_al);

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.gridwidth = 3;
	    c.weighty = c.weightx = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(jsp, c);
	    panel.add(jsp);
	    
	    // ========================

	    meas_list.setDropAction( new DragAndDropList.DropAction()
	    {
		public void dropped(DragAndDropEntity dnde)
		{
		    try
		    {
			String[] meas_n = dnde.getMeasurementNames(edata);
			int[] cur_sel = meas_list.getSelectedIndices();
			int n_cur = (cur_sel == null) ? 0 : cur_sel.length ;
			int[] new_sel = new int[meas_n.length];
			int n_new = 0;

			for(int n=0; n < meas_n.length; n++)
			{
			    ListModel lm = meas_list.getModel();
			    int i = -1;
			    for(int o=0; o < lm.getSize(); o++)
				if(meas_n[n].equals( (String) lm.getElementAt(o)))
				    i = o;
			    if(i >= 0)
			    {
				new_sel[n_new] = i;
				n_new++;
			    }
			}
			if(n_new > 0)
			{
			    int[] mix_sel = new int[n_cur + n_new];

			    for(int s=0; s <  n_cur; s++)
				mix_sel[s] = cur_sel[s];

			    for(int s=0; s < n_new; s++)
				mix_sel[n_cur+s] = new_sel[s];

			    meas_list.setSelectedIndices(mix_sel);
			}
			
		    }
		    catch(DragAndDropEntity.WrongEntityException wee)
		    {
		    }
		}
	    });

	    // ========================

	    spot_list = new DragAndDropList();
	    jsp = new JScrollPane(spot_list);
	    spot_list.setModel(new SpotListModel());
	    spot_list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
	    spot_list.addListSelectionListener(new ListSelectionListener() 
		{
		    public void valueChanged(ListSelectionEvent e) 
		    {
			if(!e.getValueIsAdjusting())
			    startUpdate();
		    }
	    });
	    c = new GridBagConstraints();
	    c.gridx = 3;
	    c.gridy = line;
	    c.gridwidth = 2;
	    c.weighty = c.weightx = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(jsp, c);
	    panel.add(jsp);

	    spot_list.setDropAction(new DragAndDropList.DropAction()
		{
		    public void dropped(DragAndDropEntity dnde)
		    {
			try
			{
			    int index = edata.getIndexOf(dnde.getSpotId());
			    spot_list.setSelectedIndex(index);
			    spot_list.ensureIndexIsVisible(index);
			    //startUpdate();
			}
			catch(DragAndDropEntity.WrongEntityException wee)
			{
			}
		    }
		});
	}
	line++;

	{
	    JPanel wrap = new JPanel();
	    wrap.setBorder(BorderFactory.createEmptyBorder(15,5,15,5));
	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrap.setLayout(w_gridbag);

	    JLabel label = new JLabel("Nearest ");
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    w_gridbag.setConstraints(label, c);
	    wrap.add(label);

	    n_spots_jtf = new JTextField(4);
	    n_spots_jtf.getDocument().addDocumentListener(new CustomListener());
	    n_spots_jtf.setText(String.valueOf(n_similar_spots));
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    w_gridbag.setConstraints(n_spots_jtf, c);
	    wrap.add(n_spots_jtf);

	    label = new JLabel(" spots, metric ");
	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = 0;
	    w_gridbag.setConstraints(label, c);
	    wrap.add(label);

	    distance_metric_jcb = new JComboBox(metric_strs);
	    c = new GridBagConstraints();
	    c.gridx = 3;
	    c.gridy = 0;
	    w_gridbag.setConstraints(distance_metric_jcb, c);
	    wrap.add(distance_metric_jcb);
	    distance_metric_jcb.setSelectedIndex(distance_metric);
	    distance_metric_jcb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			distance_metric = distance_metric_jcb.getSelectedIndex();
			startUpdate();
		    }
		});

	    // --------------

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.gridwidth = 5;
	    // c.weighty = c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(wrap, c);
	    panel.add(wrap);

	    
	}

	line++;

	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrapper.setLayout(w_gridbag);

	    {
		JButton button = new JButton("Store similarities");
		button.setToolTipText("Store the results of the similarity metric as a SpotAttribute");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    saveSimilarities();
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.weighty = c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
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
		c.gridx = 1;
		c.weighty = c.weightx = 1.0;
		c.anchor = GridBagConstraints.EAST;
		w_gridbag.setConstraints(button, c);
	    }

	    {
		JButton button = new JButton("Help");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    mview.getPluginHelpTopic("ProfileFilter", "ProfileFilter");
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 0;
		c.anchor = GridBagConstraints.EAST;
		w_gridbag.setConstraints(button, c);
	    }

	    panel.add(wrapper);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.gridwidth = 5;
	    c.weightx = 2.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(wrapper, c);
	    
	}

	frame.getContentPane().add(panel);
    }

    public class SpotListModel extends DefaultListModel
    {
	public Object getElementAt(int index) 
	{
	    return nt_selection.getNameTag( edata.getSpotAtIndex(index) );
	}
	public int getSize() 
	{
	    return edata.getNumSpots();
	}
    }

    public class CustomListSelectionListener implements ListSelectionListener
    {
	public void valueChanged(ListSelectionEvent e) 
	{
	    if(!e.getValueIsAdjusting())
		startUpdate();
	}
    }

    /*
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
    */

    // handles any changes in text field
    //
    class CustomListener implements DocumentListener 
    {
	public void insertUpdate(DocumentEvent e)  { propagate(e); }
	public void removeUpdate(DocumentEvent e)  { propagate(e); }
	public void changedUpdate(DocumentEvent e) { propagate(e); }

	private void propagate(DocumentEvent e)
	{
	    try
	    {
		n_similar_spots = (new Integer(n_spots_jtf.getText())).intValue();
		startUpdate();
	    }
	    catch (NumberFormatException nfe)
	    {
	    }
	}
    }

    // ==============================================================
    // ============= measurement list ===============================
    // ==============================================================

    private void populateListWithMeasurements(JList list)
    {
	// save existing selection if any
	Hashtable sels = new Hashtable();
	ListSelectionModel lsm = list.getSelectionModel();
	if(lsm != null)
	{
	    for(int s=lsm.getMinSelectionIndex(); s <= lsm.getMaxSelectionIndex(); s++)
	    {
		if(lsm.isSelectedIndex(s))
		    sels.put( list.getModel().getElementAt(s) , "x");
	    }
	}
	
	// build a vector of names to use as the list data
	Vector data = new Vector();

	for(int m=0; m < edata.getNumMeasurements(); m++)
	{
	    int mi = edata.getMeasurementAtIndex(m);
	    
	    data.addElement(edata.getMeasurementName(mi));
	}
	list.setListData( data );
	
	// update the meas_id map
	/*
	meas_ids = new int[ data.size() ];
	int mp = 0;
	for(int m=0; m < edata.getNumMeasurements(); m++)
	{
	    int mi = edata.getMeasurementAtIndex(m);
	    if(edata.getMeasurementShow(mi))
	    {
		meas_ids[mp++] = mi;
	    }
	}
	*/

	// and restore the selection if there was one
	if(sels.size() > 0)
	{
	    Vector sels_v = new Vector();

	    // check each of the new elements 
	    for(int o=0; o < data.size(); o++)
	    {
		String name = (String) data.elementAt(o);
		if(sels.get(name) != null)
		{
		    sels_v.addElement(new Integer(o));
		}
	    }

	    int[] sel_ids = new int[ sels_v.size() ];
	    for(int s=0; s <  sels_v.size(); s++)
	    {
		sel_ids[s] = ((Integer) sels_v.elementAt(s)).intValue();
	    }

	    list.setSelectedIndices(sel_ids);

	}		
    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  updater
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    
    private boolean abort_thread   = false;
    private boolean thread_running = false;

    public void startUpdate()
    {
	if(thread_running)
	{
	    abort_thread = true;
	    int counter = 0;
	    // System.out.println("waiting for existing thread to stop");
	    while((counter < 25) && (thread_running == true))
	    {
		try
		{
		    Thread.sleep(100);
		}
		catch(InterruptedException tie)
		{
		}
		counter++;
	    }
	}
	abort_thread = false;
	thread_running = true;
	new UpdateThread().start();
    }

    private double distance(double d1, double d2)
    {
	if(Double.isNaN(d1))
	{
	    if(Double.isNaN(d2))
		return .0;
	    else
		return Double.MAX_VALUE;
	}
	else
	{
	    if(Double.isNaN(d2))
		return Double.MAX_VALUE;
	    else
		return d1 - d2;
	}
    }

    private class UpdateThread extends Thread
    {
	public void run()
	{
	    frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	    
	    if(debug)
		System.out.println("updating profile...");
	    
	    
	    int[]     meas_ids = null;
	    double[]  target_e = null;
	    
	    boolean[] new_filter_res = null;

	    int n_meas = 0;
	    
	    DefaultListSelectionModel lm = (DefaultListSelectionModel) meas_list.getSelectionModel();
	    
	    for(int m=lm.getMinSelectionIndex(); m <= lm.getMaxSelectionIndex(); m++)
	    {
		if(meas_list.getSelectionModel().isSelectedIndex(m))
		{
		    n_meas++;
		}
	    }
		
	    if((!abort_thread) && (spot_list.getSelectedIndex() >= 0) && (n_meas > 0))
	    {
		final int n_spots = edata.getNumSpots();
	    
		new_filter_res = new boolean[n_spots];
		
		for(int s=0; s < n_spots; s++)
		{
		    new_filter_res[s]  = true;
		}
	    
		target_spot = edata.getSpotAtIndex(spot_list.getSelectedIndex());
		
		//System.out.println("profile: " + edata.getSpotName(target_spot));
		
		
		
		    
		meas_ids = new int[n_meas];
		int mi = 0;
		
		for(int m=lm.getMinSelectionIndex(); m <= lm.getMaxSelectionIndex(); m++)
		{
		    if(lm.isSelectedIndex(m))
		    {
			meas_ids[mi++] = edata.getMeasurementAtIndex(m);
		    }
		}
		
		//System.out.println(n_meas + " meas points");
		
		// store the profile of the selected spot....
		// and optionally store the directions
		
		target_e = new double[n_meas];
		
		
		for(int m=0; m < n_meas; m++)
		{
		    target_e[m] = edata.eValue(meas_ids[m], target_spot);
		}
		

		DistHit[] distance = new DistHit[n_spots - 1];

		// for efficiency in calculating deltas, cache the row of values for each spot
		double[] val_tmp = new double[n_meas];

		// likewise for the deltas of the target spot
		double[] target_deltas = new double[n_meas];
		for(int m=1; m < n_meas; m++)
		{
		    target_deltas[m] = target_e[m] - target_e[m-1];
		}

		// get distances for all other spot profiles
		
		int si = 0;
		for(int s=0; s < n_spots; s++)
		{
		    if(!abort_thread)
		    {
			if(s != target_spot)
			{
			    double sdist = .0;

			    switch( distance_metric )
			    {
			    case 0: // distance
				
				for(int m=0; m < n_meas; m++)
				{
				    final double tmp = distance( target_e[m] , edata.eValue(meas_ids[m], s) );
				    sdist += ( tmp * tmp );
				}
				break;

			    case 1: // slope

				for(int m=0; m < n_meas; m++)
				    val_tmp[m] = edata.eValue(meas_ids[m], s);
	
				for(int m=1; m < n_meas; m++)
				{
				    final double sl1 = target_deltas[m];
				    final double sl2 = val_tmp[m] - val_tmp[m-1]; 
				    
				    final double tmp = distance( sl1, sl2 );
				    sdist += ( tmp * tmp );
				}
				break;
				
			    case 2: // direction

				for(int m=0; m < n_meas; m++)
				    val_tmp[m] = edata.eValue(meas_ids[m], s);
				
				for(int m=1; m < n_meas; m++)
				{
				    final boolean up1 = (target_deltas[m] >= .0);
				    final boolean up2 = ((val_tmp[m] - val_tmp[m-1]) >= .0);
				    
				    sdist += ((up1 != up2) ? 1.0 : 0.0);
				}
				break; 
			    }
			    
			    distance[si++] = new DistHit(s, sdist);
			}
		    }
		}
		
		if(!abort_thread)
		    Arrays.sort(distance, new DistComparator());
		
		if(!abort_thread)
		{
		    new_filter_res[target_spot] = false;
		    
		    final int n_poss   = edata.getNumSpots() - 1;
		    final int do_spots = n_poss < n_similar_spots ? n_poss : n_similar_spots;

		    for(int s=0; s < do_spots; s++)
		    {
			new_filter_res[ distance[s].spot_id ] = false;
		    }
		}
	    }
	    
	    filter_res = new_filter_res;
	    edata.notifyFilterChanged((ExprData.Filter) ProfileFilter.this);
	    
	    frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	    
	    if(debug)
		System.out.println("...done");	
	    
	    thread_running = false;
	}
    }
    
    private class DistHit
    {
	public double dist;
	public int spot_id;

	public DistHit( int s, double d) { dist = d; spot_id = s; }
    }

    private class DistComparator implements Comparator
    {
	public int compare(Object o1, Object o2) 
	{ 
	    double d1 = ((DistHit)o1).dist;
	    double d2 = ((DistHit)o2).dist;
	    if(d1 == d2)
		return 0;
	    else
		return (d1 < d2) ? -1 : 1; 
	}
	public boolean equals(Object obj)        { return false; }
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    
    private void saveSimilarities()
    {
	final JFrame options_frame = new JFrame("Save Profile Similarity Data");
	mview.decorateFrame( options_frame );

	JPanel options_panel = new JPanel();
	options_panel.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
	GridBagLayout gridbag = new GridBagLayout();
	options_panel.setLayout( gridbag );

	final JRadioButton save_as_meas = new JRadioButton( "Store the similarities in a new Measurement" );
	final JTextField   meas_name    = new JTextField( 32 );
	
	final JRadioButton save_in_attr    = new JRadioButton( "Store the similarities in an existing SpotName attribute" );
	final NameTagSelector new_attr_nts  = new NameTagSelector(mview);

	final JRadioButton save_in_new_attr  = new JRadioButton( "Store the similarities in a new SpotName attribute" );
	final JTextField new_attr_name  = new JTextField( 32 );

	ButtonGroup bg = new ButtonGroup();
	bg.add( save_as_meas );
	bg.add( save_in_attr );
	bg.add( save_in_new_attr );

	GridBagConstraints c;

	int line = 0;

	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line;
	c.gridwidth = 2;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints( save_as_meas, c );
	options_panel.add( save_as_meas );

	line++;

	JLabel label = new JLabel("Name");
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line;
	//c.weightx = 1.0;
	c.anchor = GridBagConstraints.EAST;
	gridbag.setConstraints( label, c );
	options_panel.add( label );

	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = line;
	c.weightx = 1.0;
	gridbag.setConstraints( meas_name, c );
	options_panel.add( meas_name );

	line++;

	addFiller( options_panel, gridbag, line++, 6 );

	line++;

	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line;
	c.gridwidth = 2;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints( save_in_attr, c );
	options_panel.add( save_in_attr );

	line++;

	/*
	JLabel label = new JLabel("Name");
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line;
	//c.weightx = 1.0;
	gridbag.setConstraints( label, c );
	panel.add( label );
	*/

	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = line;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints( new_attr_nts, c );
	options_panel.add(new_attr_nts );

	line++;

	addFiller( options_panel, gridbag, line++, 6 );

	line++;

	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line;
	c.gridwidth = 2;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints( save_in_new_attr, c );
	options_panel.add( save_in_new_attr );

	line++;

	label = new JLabel("Name");
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line;
	//c.weightx = 1.0;
	gridbag.setConstraints( label, c );
	c.anchor = GridBagConstraints.EAST;
	options_panel.add( label );

	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = line;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints( new_attr_name, c );
	options_panel.add( new_attr_name );


	line++;

	addFiller( options_panel, gridbag, line++, 6 );

	line++;

	JPanel button_panel = new JPanel();
	button_panel.setLayout( new BoxLayout( button_panel, BoxLayout.X_AXIS ) );

	JButton store  = new JButton( "Store" );
	button_panel.add( store );
	JButton cancel = new JButton( "Cancel" );
	button_panel.add( cancel );
	

	store.addActionListener(new ActionListener() 
		    { 
			public void actionPerformed(ActionEvent e) 
			{
			    Object name = null;
			    int mode = -1; 

			    if ( save_as_meas.isSelected() )
			    {
				mode = 0;
				name = meas_name.getText();
			    }
			    if ( save_in_attr.isSelected() )
			    {
				mode = 1;
				name = new_attr_nts.getNameTagSelection();
			    }
			    if ( save_in_new_attr.isSelected() )
			    {
				mode = 2;
				name = new_attr_name.getText();
			    }
			    
			    if( name != null )
			    {
				saveSimilarities( mode, name, frame );
			    }
			    else
			    {
				mview.alertMessage("Choose one of the destinations in which to store the values");
			    }
			}
		    });

	cancel.addActionListener(new ActionListener() 
		    { 
			public void actionPerformed(ActionEvent e) 
			{
			    options_frame.setVisible( false );
			}
		    });

	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line;
	c.gridwidth = 2;
	c.weightx = 10.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	gridbag.setConstraints( button_panel, c );
	options_panel.add( button_panel );

	
	options_frame.getContentPane().add( options_panel );
	options_frame.pack();
	options_frame.setVisible( true );
    }


    private void saveSimilarities( final int mode, final Object name, final JFrame frame )
    {
	//final ExprData.NameTagSelection nt_sel;
	
	// 1. gather the similarity metric for each spot
	
	int n_meas = 0;
	
	DefaultListSelectionModel lm = (DefaultListSelectionModel) meas_list.getSelectionModel();
	
	for(int m=lm.getMinSelectionIndex(); m <= lm.getMaxSelectionIndex(); m++)
	{
	    if(meas_list.getSelectionModel().isSelectedIndex(m))
	    {
		n_meas++;
	    }
	}
	
	int[] meas_ids = new int[n_meas];
	int mi = 0;
	
	for(int m=lm.getMinSelectionIndex(); m <= lm.getMaxSelectionIndex(); m++)
	{
	    if(lm.isSelectedIndex(m))
	    {
		meas_ids[mi++] = edata.getMeasurementAtIndex(m);
	    }
	}
	
	final int n_spots = edata.getNumSpots();
	    
	final int target_spot = edata.getSpotAtIndex(spot_list.getSelectedIndex());
		
	final double[] target_e = new double[n_meas];
	
	for(int m=0; m < n_meas; m++)
	{
	    target_e[m] = edata.eValue(meas_ids[m], target_spot);
	}
		
	
	// for efficiency in calculating deltas, cache the row of values for each spot
	double[] val_tmp = new double[n_meas];
	
	// likewise for the deltas of the target spot
	double[] target_deltas = new double[n_meas];
	for(int m=1; m < n_meas; m++)
	{
	    target_deltas[m] = target_e[m] - target_e[m-1];
	}

	// get distances for all other spot profiles
	
	double[] distance = new double[ n_spots ];

	for(int s=0; s < n_spots; s++)
	{
	    double sdist = .0;
	    
	    switch( distance_metric )
	    {
		case 0: // distance
		    
		    for(int m=0; m < n_meas; m++)
		    {
			final double tmp = distance( target_e[m] , edata.eValue(meas_ids[m], s) );
			sdist += ( tmp * tmp );
		    }
		    break;
		    
		case 1: // slope
		    
		    for(int m=0; m < n_meas; m++)
			val_tmp[m] = edata.eValue(meas_ids[m], s);
		    
		    for(int m=1; m < n_meas; m++)
		    {
			final double sl1 = target_deltas[m];
			final double sl2 = val_tmp[m] - val_tmp[m-1]; 
			
			final double tmp = distance( sl1, sl2 );
			sdist += ( tmp * tmp );
		    }
		    break;
		    
		case 2: // direction
		    
		    for(int m=0; m < n_meas; m++)
			val_tmp[m] = edata.eValue(meas_ids[m], s);
		    
		    for(int m=1; m < n_meas; m++)
		    {
			final boolean up1 = (target_deltas[m] >= .0);
			final boolean up2 = ((val_tmp[m] - val_tmp[m-1]) >= .0);
			
			sdist += ((up1 != up2) ? 1.0 : 0.0);
		    }
		    break; 
	    }
	    
	    distance[ s ] = sdist;
	}
	

	switch( mode )
	{
	    case 0: // store in a Measurement

		ExprData.Measurement meas = edata.new Measurement( (String) name, ExprData.ExpressionAbsoluteDataType, distance );
		
		meas.setAttribute( "Source", "ProfileFilter plugin", "Distance Metric" );
		
		edata.addOrderedMeasurement( meas );
		
		// make the new Measurement visible
		mview.getDataPlot().displayMeasurement( meas );	

		break;

	    case 1: // store in an existing SpotName Attr
	    case 2: // store in a new SpotName Attr
		
		final ExprData.TagAttrs spot_ta = edata.getSpotTagAttrs();

		int tag_attr_id = -1;

		if( mode == 2 )
		{
		    tag_attr_id = spot_ta.addAttr( (String) name );
		}
		else
		{
		    ExprData.NameTagSelection nt_sel = ( ExprData.NameTagSelection ) name;
		    if( nt_sel.isSpotNameAttr() )
		    {
			tag_attr_id = spot_ta.getAttrID( nt_sel.getNames() );
		    }
		}

		if( tag_attr_id == -1 )
		{
		    mview.alertMessage("Unable to determine which SpotName attribute to use.");
		    return;
		}

		String[] spot_name = edata.getSpotName();
		
		for( int s=0; s < n_spots; s++) 
		    spot_ta.setTagAttr( spot_name[ s ], tag_attr_id, String.valueOf( distance[ s ] ) );

		// generate an event?

		break;

	}

	frame.setVisible( false );
    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  


    private void addFiller( JPanel panel, GridBagLayout gb, int line, int size )
    {
	Dimension fillsize = new Dimension(20,20);
	Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
	GridBagConstraints c = new GridBagConstraints();
	c.gridy = line;
	gb.setConstraints(filler, c);
	panel.add(filler);
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  gubbins
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private String[] metric_strs = { "Distance", "Slope", "Direction" };

    private int distance_metric = 0;

    private boolean[] filter_res = null;
    private int target_spot;

    private int[] meas_ids = null;

    private maxdView mview;
    private DataPlot dplot;
    private ExprData edata;

    private NameTagSelector nt_sel;
    private ExprData.NameTagSelection nt_selection;

    private JTextField n_spots_jtf;
    private JComboBox distance_metric_jcb;

    private int n_similar_spots;

    private JFrame     frame = null;

    private DragAndDropList spot_list;
    private DragAndDropList meas_list;
    private ListSelectionListener meas_list_al;
}
