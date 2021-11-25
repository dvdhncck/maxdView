import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import ExprData;
import java.util.BitSet;
import java.util.Vector;

//
// distribute genes into a 2d array of bins based on the values in
//   two user controlled sets
//

public class SortClusters extends JFrame implements ExprData.ExprDataObserver, ExpressionViewer.Plugin
{
    public SortClusters(ExpressionViewer eview_)
    {
	super("Sort Clusters");

	//System.out.println("++ SortClusters is constructed ++");

	eview = eview_;
	edata = eview.getExprData();
	dplot = eview.getDataPlot();

	addWindowListener(new WindowAdapter() 
			  {
			      public void windowClosing(WindowEvent e)
			      {
				  cleanUp();
			      }
			  });

    }

    public void cleanUp()
    {
	stopPlugin();
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
	//System.out.println("++ SortClusters has been started ++");
	addComponents();
	pack();
	setVisible(true);
	edata.addObserver(this);
    }

    public void stopPlugin()
    {
	//System.out.println("++ SortClusters has been stopped ++");
	edata.removeObserver(this);
	setVisible(false);
    }
  
    public ExpressionViewer.PluginInfo getPluginInfo()
    { 
	ExpressionViewer.PluginInfo pinf = eview.new PluginInfo("Sort Clusters", "transform", 
								"re-order the genes so that clusters are near one another", 
								1, 0, 0);
	return pinf;
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
    }
    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  command handlers
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public class MatchDistance
    {
	int matches;
	int mismatches;

	public MatchDistance(int m, int mm)
	{
	    matches = m;
	    mismatches = mm;
	}
	public String toString()
	{
	    return new String("(" + matches + " matches, " + mismatches + " mismatches)");
	}
	
	// is one MatchDistance better than another?
	// (a greater number of matches is always better, 
	//  with the same number of matches, fewer mismatches is better)
	//
	public boolean betterThan(MatchDistance t)
	{
	    return ((matches > t.matches) || ((matches == t.matches) && (mismatches < t.mismatches)));
	}
    }
    
    public class SortList
    {

	public int index;
	BitSet clusters;
	public SortList next;
	//public MatchDistance to_next;
	
	public SortList(BitSet cl, int i)
	{
	    index = i;
	    clusters = cl;
	    next = null;
	    //to_next = null;

	}

	public MatchDistance distanceTo(SortList t)
	{
	    int m = 0;    // matches
	    int mm = 0;   // mismatches
	    
	    // score +2 for each match
	    //   and -1 for missing one (in either cluster)
	    //
	    
	    // if either the source or the target are in no clusters,
	    // then return the biggest possible distance
	    //
	    if((clusters == null) || (t.clusters == null))
	    {
		return new MatchDistance(0, Integer.MAX_VALUE);
	    }

	    BitSet most  = null;
	    BitSet least = null;

	    if(t.clusters.size() > clusters.size())
	    {
		most  = t.clusters;
		least = clusters;
	    }
	    else
	    {
		most  = clusters;
		least = t.clusters;
	    }

	    // check all the clusters that 'least' is in
	    // to see whether 'most' is also in the same ones...
	    //
	    for(int c=0; c< least.size(); c++)
	    {
		if(least.get(c))
		{
		    if(most.get(c))
			// both genes are in the same cluster
			m++;
		    else
			// 'least' is in this cluster, 'most' is not
			mm++;
		}
		else
		{
		    if(most.get(c))
			// 'most' in in this cluster, 'least' is not
			mm++;
		}
	    }

	    // now check for any other clusters which 'most' is in
	    // that 'least' can't possibly be in
	    //
	    for(int c = least.size(); c < most.size(); c++)
	    {
		if(most.get(c))
		    mm++;
	    }

	    return new MatchDistance(m, mm);
	}
    }

    public class SorterThread extends Thread
    {
	int n_spots;
	SortList head;
	SortList tail;
	boolean show_progress;

	public SorterThread(boolean show_p)
	{
	    n_spots = edata.getNumSpots();
	    
	    head = null;
	    tail = null;
	    show_progress = show_p;
	}
	
	public void run()
	{
	    ProgressOMeter pm = new ProgressOMeter("Sorting", true);
	    pm.startIt();
	    
	    BitSet in_clusters[] = new BitSet[n_spots];
	    
	    for(int i=0; i < n_spots; i++)
		in_clusters[i] = null;
	    
	    
	    // find out which visible clusters each gene is in...
	    //
	    int vis_clust_cnt = 0;
	    int genes_in_clust_cnt = 0;
	    
	    ExprData.ClusterIterator clit = edata.new ClusterIterator();
	    
	    ExprData.Cluster clust = clit.getFirstLeaf();
	    int iv;
	    while(clust != null)
	    {
		if(clust.getShow())
		{
		    vis_clust_cnt++;
		    
		    Vector v = clust.getElements();
		    for(int gn=0; gn < v.size(); gn++)
		    {
			iv = ((Integer) v.elementAt(gn)).intValue();
			
			if(iv < n_spots)
			{
			    if(in_clusters[iv] == null)
				in_clusters[iv] = new BitSet();
			    
			    in_clusters[iv].set(clust.getId());
			    
			    genes_in_clust_cnt++;
			}
		    }
		}
		clust = clit.getNextLeaf();
	    }
	    
	    vis_clust_cnt_label.setText("There are " + vis_clust_cnt + " visible clusters");
	    genes_in_clust_cnt_label.setText("with a total of " + genes_in_clust_cnt + " genes in them");
	    
	    Thread.yield();

	    // vis_clust_cnt_label.update();
	    
	    //System.out.println("There are " + vis_clust_cnt + " visible clusters");
	    //System.out.println("with a total of " + genes_in_clust_cnt + " genes in them");
	    
	    
	    int progress_step = n_spots / 10;
	    int progress_done = 0;
	    int progress_tick = 0;
	    
	    int[] current_traversal = edata.getGeneOrder();

	    // now visit each gene in turn, and place it after the gene already in the list
	    // which has the most similar set of clusters to the gene being inserted...
	    //
	    for(int i=0; i < n_spots; i++)
	    { 
		
		if(++progress_tick > progress_step)
		{
		    progress_tick = 0;
		    progress_done += 10;
		    pm.setProgress(progress_done);

		    if(show_progress)
		    {
			int[] tmp_progress_order = new int[n_spots];
			SortList tmp_head = head;
			int s = 0;
			while(tmp_head != null)
			{
			    tmp_progress_order[s++] = tmp_head.index;
			    tmp_head = tmp_head.next;
			}
			// the remainder comes directly from the current order
			//
			for(int rem=s; rem < n_spots;rem++)
			    tmp_progress_order[rem] = current_traversal[rem];

			System.out.println(progress_done + "%");

			edata.setGeneOrder(tmp_progress_order);
		    }
		    Thread.yield();
		}
		
		int g = current_traversal[i];
		
		SortList elem = new SortList(in_clusters[g], g);
		
		SortList insert = null;
		
		// insert into the correct place
		
		if(head == null)
		{
		    // insert as the new head
		    elem.next = insert;
		    head = elem;
		    // and it's also the tail of this list
		    tail = elem;
		    
		    //System.out.println("inserted as new head");
		}
		else
		{
		    {
			// search for insertion point
			SortList insert_search = head;
			MatchDistance best_distance = new MatchDistance(0, Integer.MAX_VALUE);
			
			while((insert_search != null) && (insert_search.next != null))
			{
			    MatchDistance md = elem.distanceTo(insert_search.next);
			    
			    if(md.betterThan(best_distance))
			    {
				best_distance = md;
				insert = insert_search;
			    }
			    insert_search = insert_search.next;
			}
			
			if(insert == null)
			{ 
			    // add at the tail
			    tail.next = elem;
			    tail = elem;  
			    
			    //System.out.println("inserted as tail");
			}
			else
			{
			    
			    if(insert == head)
			    {
				// insert at head
				elem.next = head;
				head = elem;
				//System.out.println("inserted before head");
			    }
			    else
			    {
				// add somewhere in the list 
				
				elem.next = insert.next;
				insert.next = elem;
				
				//System.out.println("inserted in Nth pos");
			    }
			}
			
			yield();
			
		    }
		}
	    }

	    // now build a the traversal order using the list
	    //
	    int[] traversal_order = new int[n_spots];
	    
	    for(int g=0; g < n_spots; g++)
	    { 
		if(head != null)
		{
		    traversal_order[g] = head.index;
		    head = head.next;
		}
		else
		{
		    eview.addMessageToLog("SortClusters: BAD! run out of stuff on the list");
		    break;
		}

		yield();
	    }
	    
	    //System.out.println("calling setGeneOrder()");
	    
	    pm.stopIt();
	    
	    edata.setGeneOrder(traversal_order);
	}
    }

    // ---------------- --------------- --------------- ------------- ------------

    public void cancel()
    {
	cleanUp();
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  stuff
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private void addComponents()
    {
	GridBagLayout gridbag = new GridBagLayout();
	JPanel panel = new JPanel();
	
	panel.setLayout(gridbag);
	panel.setPreferredSize(new Dimension(350, 200));

	{
	    vis_clust_cnt_label = new JLabel("There are ?? visible clusters");
	    panel.add(vis_clust_cnt_label);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weighty = c.weightx = 1.0;
	    c.anchor = GridBagConstraints.SOUTH;
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(vis_clust_cnt_label, c);
	}

	{
	    genes_in_clust_cnt_label = new JLabel("with a total of ??? genes in them");
	    panel.add(genes_in_clust_cnt_label);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.weighty = c.weightx = 1.0;
	    c.anchor = GridBagConstraints.NORTH;
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(genes_in_clust_cnt_label, c);
	}
	
	{
	    show_progress_jchkb = new JCheckBox("Show progess");
	    panel.add(show_progress_jchkb);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 2;
	    c.weighty = c.weightx = 1.0;
	    gridbag.setConstraints(show_progress_jchkb, c);
	}


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
			    cancel();
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		//c.anchor = GridBagConstraints.EAST;
		w_gridbag.setConstraints(button, c);
	    }
	    {
		JButton button = new JButton("Sort");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    SorterThread st = new SorterThread(show_progress_jchkb.isSelected());
			    st.start();
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		w_gridbag.setConstraints(button, c);
	    }
	    {
		JButton button = new JButton("Help");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    eview.getPluginHelpTopic("SortClusters", "SortClusters");
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
	    c.gridy = 5;
	    c.weighty = c.weightx = 1.0;
	    gridbag.setConstraints(wrapper, c);
	    
	}

	getContentPane().add(panel);
    }

    private ExpressionViewer eview;
    private DataPlot dplot;
    private ExprData edata;

    protected JCheckBox show_progress_jchkb;

    private JLabel vis_clust_cnt_label;
    private JLabel genes_in_clust_cnt_label;
}
