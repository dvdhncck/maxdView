import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import javax.swing.table.*;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.BitSet;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreeSelectionModel;

//
// distribute genes into a 2d array of bins based on the values in
//   two user controlled sets
//

public class SortClusters implements ExprData.ExprDataObserver, Plugin
{
    public SortClusters(maxdView mview_)
    {
	mview = mview_;
	edata = mview.getExprData();
	dplot = mview.getDataPlot();

    }

    public void cleanUp()
    {
	edata.removeObserver(this);

	if(frame != null)
	{
	    frame.setVisible(false);
	    
	    mview.putBooleanProperty("SortClusters.sort_spots", sort_spots);
	    mview.putBooleanProperty("SortClusters.sort_meas", sort_meas);
	    mview.putBooleanProperty("SortClusters.show_progress", show_progress);
	    mview.putBooleanProperty("SortClusters.apply_filter", apply_filter);
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
	sort_spots = mview.getBooleanProperty("SortClusters.sort_spots", true);
	sort_meas = mview.getBooleanProperty("SortClusters.sort_meas", true);
	show_progress = mview.getBooleanProperty("SortClusters.show_progress", false);
	apply_filter = mview.getBooleanProperty("SortClusters.apply_filter", false);

	frame = new JFrame("Sort Clusters");
	mview.decorateFrame( frame );
	addComponents();
	frame.pack();
	frame.setVisible(true);

	frame.addWindowListener(new WindowAdapter() 
	    {
		public void windowClosing(WindowEvent e)
		{
		    cleanUp();
		}
	    });

	edata.addObserver(this);
    }

    public void stopPlugin()
    {
	cleanUp();
    }
  
    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("Sort Clusters", "transform", 
							"Order the data so that the contents of each cluster are near one another", "",
							1, 0, 0);
	return pinf;
    }
    
    public PluginCommand[] getPluginCommands()
    {
	PluginCommand[] com = new PluginCommand[3];
	
	String[] args = new String[] 
	{ 
	    "sortSpots",        "boolean",  "false",   "",    "sort all Sort clusters",
	    "sortMeasurements", "boolean",  "false",   "",    "sort all Measurement clusters"
	};
	
	com[0] = new PluginCommand("sort", args);
	
	com[1] = new PluginCommand("sortSpots", null);
	
	com[2] = new PluginCommand("sortMeasurements", null);
	
	return com;
    }
    
    public void runCommand(String name, String[] args, CommandSignal done) 
    { 
	show_progress = false;
	sort_spots = false;
	sort_meas = false;
	apply_filter = false;

	if(name.equals("sort"))
	{
	    // check args
	    String ss = mview.getPluginArg("sortSpots", args);
	    if(ss != null)
		sort_spots = ss.equals("true");
	    
	    String sm = mview.getPluginArg("sortMeasurements", args);
	    if(ss != null)
		sort_meas = sm.equals("true");
	}
	   
	if(name.equals("sortMeasurements"))
	{
	    sort_meas = true;
	}
	if(name.equals("sortSpots"))
	{
	    sort_spots = true;
	}

	SorterThread st = new SorterThread( done );
	st.start();
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
	switch(cue.event)
	{
	case ExprData.ColourChanged:
	case ExprData.OrderChanged:
	case ExprData.VisibilityChanged:
	    break;
	case ExprData.SizeChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	    if(cluster_tree != null)
	    {
		populateTreeWithClusters(cluster_tree, edata.getRootCluster() );
	    }
	    break;
	}
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

    /*
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
    */

    /*
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
    */

    private class MutlipleEntryException extends Exception
    {
    }

    public class SorterThread extends Thread
    {
	ProgressOMeter pm;
	CommandSignal done;

	public SorterThread( CommandSignal done_ )
	{
	    done = done_;
	}

	public void run()
	{
	    if(show_progress)
	    {
		pm = new ProgressOMeter("Sorting...");
		pm.startIt();
	    }

	    DefaultMutableTreeNode node = null;
	    if(cluster_tree != null)
		node = (DefaultMutableTreeNode) cluster_tree.getLastSelectedPathComponent();

	    if(node != null)
	    {
		ExprData.Cluster cluster = (ExprData.Cluster) node.getUserObject();
		if(!sortUsingTree(cluster.getIsSpot(), cluster, false))
		    sortByComparing(cluster.getIsSpot(), cluster, false);
	    }
	    if(sort_spots)
	    {
		if(!sortUsingTree(true))
		    sortByComparing(true);
	    }
	    if(sort_meas)
	    {
		sortUsingTree(false);
		//if(!sortUsingTree(false))
		//    sortByComparing(false);
	    }
	    
	     if(show_progress)
		 pm.stopIt();
	    
	     if(done != null)
		 done.signal();
	}

	private int allocateThingsToArray( ExprData.Cluster clust, boolean spots, 
					   int pos, int n_things, 
					   int[] order, boolean[] used, 
					   boolean ignore_hidden) throws MutlipleEntryException
	{
	    int things = 0;
	    int iv = 0;

	    Vector chl = clust.getChildren();
	    if(chl != null)
	    {
		for(int c=0; c < chl.size(); c++)
		{
		    things += allocateThingsToArray( (ExprData.Cluster) chl.elementAt(c), spots,
						     pos + things, n_things,
						     order, used , ignore_hidden);
		    
		}
	    }
	    
	    if(!ignore_hidden || clust.getShow())
	    {
		
		if(spots == clust.getIsSpot())
		{

		    //System.out.print(clust.getName() + ":");
		    int[] ce = clust.getElements();
			
		    if(ce != null)
		    {
			final int n_els = ce.length;
			
			// els_in_clust_cnt += n_els;
			
			for(int gn=0; gn < n_els; gn++)
			{
			    iv = ce[gn];
			    
			    if((iv < n_things) && ( (pos + things) < n_things))
			    {
				// has this row already been placed somwhere?
				if(used[iv] == false)
				{
				    order[pos + things] = iv;
				    used[iv] = true;
				    //System.out.print("(" + edata.getProbeName(iv) + ")");
				}
				else
				{
				    throw new MutlipleEntryException();
				}
			    }
			    things++;
			}
		    }
		}
	    }

	    //if(things > 0)
	    //System.out.println(clust.getName() + ": " + things + " things from " + pos + " to " + (pos+things));

	    return things;
	}

	// sort using knowledge of the cluster hierarchy
	// to generate the ordering which mostly closely
	// matches the dendrogram.
	//
	// (very good when all rows are in exactly one cluster, but
	// cannot cope with situations where one or more elements are 
	// in more than one cluster)
	//
	public boolean sortUsingTree(boolean sort_spots)
	{
	    return sortUsingTree(sort_spots, edata.getRootCluster(), true);
	}

	public boolean sortUsingTree(boolean sort_spots, ExprData.Cluster cluster, boolean ignore_hidden)
	{
	    boolean is_sorted = false;
	    
	    boolean one_for_one = true; // assume each row in zero or one clusters

	    int n_things = 0;

	    if(sort_spots)
	    {
		n_things = edata.getNumSpots();
	    }
	    else
	    {
		n_things = edata.getNumMeasurements();		
	    }
	    
	    int[] new_trav = new int[n_things];
	    boolean[] used = new boolean[n_things];

	    for(int t=0; t < n_things; t++)
	    {
		new_trav[t] = -1;
		used[t]     = false;
	    }

	    //ExprData.ClusterIterator clit = edata.new ClusterIterator();
	    
	    //ExprData.Cluster clust = clit.getCurrent();

	    int iv;

	    //int vis_clust_cnt = 0;
	    //int els_in_clust_cnt = 0;

	    try
	    {
		int allocated = allocateThingsToArray( cluster, sort_spots, 0, n_things, new_trav, used, ignore_hidden );

	    
	    /*
	    while(clust != null)
	    {
		if(clust.getShow())
		{
		    if(sort_spots == clust.getIsSpot())
		    {
			
			vis_clust_cnt++;
			
			//System.out.print(clust.getName() + ":");
			int[] ce = clust.getElements();
			
			if(ce != null)
			{
			    final int n_els = ce.length;
			    
			    els_in_clust_cnt += n_els;
			    
			    for(int gn=0; gn < n_els; gn++)
			    {
				iv = ce[gn];
				
				if(iv < n_things)
				{
				    // has this row already been placed somwhere?
				    if(used[iv] == true)
				    {
					// row in in more than one cluster
					one_for_one = false;
				    }
				    else
				    {
					new_trav[posn++] = iv;
					used[iv] = true;
					//System.out.print("(" + edata.getProbeName(iv) + ")");
				    }
				}
				
			    }
			}
		    }
		}
		//System.out.print("");
		
		yield();
		clust = clit.getNext();
	    }
	    */

		// are there are things left over (i.e. those not in any clusters)
		
		//System.out.println("sortUsingTree(): " + allocated + " things placed using tree");

		for(int s=0; s < n_things; s++)
		{
		    if(used[s] == false)
		    {
			//System.out.println("sortUsingTree(): thing " + s + " not used...");
			new_trav[allocated++] = s;
		    }
		}
		
		if(allocated < n_things)
		{
		    System.out.println("sortUsingTree(): WIERD! placed " + allocated + " of " + n_things + " things");
		}
		
		// updateLabels(vis_clust_cnt, els_in_clust_cnt);
		
		if(sort_spots)
		{
		    edata.setRowOrder(new_trav);
		}
		else
		{
		    edata.setMeasurementOrder(new_trav);
		}
		
		return true;
	    }
	    catch( MutlipleEntryException mle )
	    {
		System.out.println("sortUsingTree(): failed to generate simple order");
		return false;
	    }
	}


	// sort without using knowledge of the cluster hierarchy,
	// instead compare order rows by comparing which
	// cluster leaves each one is one and grouping ones
	// in similar sets of clusters
	//
	// (not very good when all rows are in exactly one
	//  cluster, but can cope with situations where 
	//  one or more rows are in more than one cluster)
	//
	public boolean sortByComparing(boolean sort_spots)
	{
	    return sortByComparing(sort_spots, edata.getRootCluster(), true);
	}


	public boolean sortByComparing(boolean sort_spots, ExprData.Cluster cluster, boolean ignore_hidden)
	{

	    int n_spots = edata.getNumSpots();
	    int vis_clust_cnt = 0;
	    int els_in_clust_cnt = 0;

	    if(show_progress)
		pm.setMessage("Scanning clusters...");

	    // group spots that are in exactly the same clusters together
	    
	    // build a vector for all spots that are in 1 or more clusters.
	    // the vector contains the Cluster IDs for any cluster that spot is in

	    Vector in_clusters[] = new Vector[n_spots];
	    
	    for(int i=0; i < n_spots; i++)
		in_clusters[i] = null;
	    
	    ExprData.ClusterIterator clit = edata.new ClusterIterator();
	    
	    ExprData.Cluster clust = clit.getFirstLeaf();

	    while(clust != null)
	    {
		if(clust.getShow() && clust.getIsSpot())
		{
		    vis_clust_cnt++;
		    
		    int[] ce = clust.getElements();
		    for(int si=0; si < ce.length; si++)
		    {
			int s_id = ce[si];
			
			boolean add = true;

			if(apply_filter && (edata.filter(s_id)))
			   add = false;
			if(s_id >= n_spots)
			    add = false;
			
			if(add)
			{
			    if(in_clusters[s_id] == null)
				in_clusters[s_id] = new Vector();
			    
			    in_clusters[s_id].addElement(new Integer(clust.getId()));
			    
			    els_in_clust_cnt++;
			}
		    }
		}
		clust = clit.getNextLeaf();
	    }
	    
	    if(show_progress)
		pm.setMessage("Matching clusters...");

	    // now convert these Vectors into Strings for easy comparison
	    // and store lists of spots which same the same clusters
	    //
	    Hashtable same_clusters_ht = new Hashtable();

	    for(int si=0; si < n_spots; si++)
	    {
		if(in_clusters[si] != null)
		{
		    String cluster_desc = new String();
		    final int nc = in_clusters[si].size();
		    for(int c=0; c < nc; c++)
		    {
			Integer i = (Integer) in_clusters[si].elementAt(c);
			cluster_desc += (String.valueOf(i.intValue()) + ".");
		    }
		    
		    SameClusters sc = (SameClusters) same_clusters_ht.get(cluster_desc);
		    Vector s_ids = null;
		    if(sc == null)
		    {
			s_ids = new Vector();
			sc = new SameClusters(in_clusters[si], s_ids);
			same_clusters_ht.put(cluster_desc, sc);
		    }
		    else
		    {
			s_ids = sc.s_ids;
		    }
		    s_ids.addElement(new Integer(si));
		}
	    }

	    if(show_progress)
		pm.setMessage("Ordering spots...");

	    // same_clusters_ht now contains groups of spots, where all the spots
	    // in the group are in exactly the same set of clusters
	    //
	    // now find an ordering for the groups which keeps similar groups
	    // together....

	    int[] traversal_order = new int[n_spots];
	    
	    int sp = 0;

	    // start with the biggest cluster....

	    String big_key = findBiggest(same_clusters_ht);
	    SameClusters big_sc = (SameClusters) same_clusters_ht.get(big_key);
	    same_clusters_ht.remove(big_key);
	    Vector s_ids = big_sc.s_ids;
	    for(int s=0; s < s_ids.size(); s++)
	    {
		Integer s_id = (Integer) s_ids.elementAt(s);
		traversal_order[sp++] = s_id.intValue();
	    }
	    
	    // repeatedly add the set of clusters which is closest to the last one
	    // until there are none left
	    
	    SameClusters last_sc = big_sc;
	    while(same_clusters_ht.size() > 0)
	    {
		String       next_key = findClosest(same_clusters_ht, last_sc);
		SameClusters next_sc = (SameClusters) same_clusters_ht.get(next_key);
		same_clusters_ht.remove(next_key);

		// System.out.println(next_key + ", " + same_clusters_ht.size() + " remain");
		
		s_ids = next_sc.s_ids;
		for(int s=0; s < s_ids.size(); s++)
		{
		    Integer s_id = (Integer) s_ids.elementAt(s);
		    traversal_order[sp++] = s_id.intValue();
		}


		last_sc = next_sc;
	    }

	    // finally add any spots which are not in any clusters

	    for(int s=0; s < n_spots; s++)
	    {
		int si = edata.getSpotAtIndex(s);
		if(in_clusters[si] == null)
		{
		    traversal_order[sp++] = si;
		}
	    }

	    if(sp != n_spots)
	    {
		mview.errorMessage("sortByComparing(): odd, " + sp + " spots processed, should be " + n_spots);
		return false;
	    }
	    else
	    {
		edata.setRowOrder(traversal_order);
		return true;
	    }

	}

	// contains the Spot IDs of all Spots which are in the 
	// same set of clusters. The set of Cluster ID's is also stored.
	//
	private class SameClusters
	{
	    public Vector c_ids; // both are Vectors of Integers
	    public Vector s_ids;

	    public SameClusters(Vector c,  Vector s)
	    {
		c_ids = c;
		s_ids = s;
	    }
	}

	// return the SameClusters object with the biggest set of clusters
	//
	private String findBiggest(Hashtable ht)
	{
	    int    biggest_size = 0;
	    String key_for_biggest_sc = null;

	    for (Enumeration e = ht.keys(); e.hasMoreElements() ;) 
	    {
		String cluster_desc = (String) e.nextElement();
		SameClusters sc = (SameClusters) ht.get(cluster_desc);

		if(sc.s_ids.size() > biggest_size)
		{
		    key_for_biggest_sc = cluster_desc;
		    biggest_size = sc.s_ids.size();
		}
	    }
	    
	    return key_for_biggest_sc;
	}

	// return the SameClusters object which is most like the argument 'sc_in'
	//
	private String findClosest(Hashtable ht, SameClusters sc_in)
	{
	    int   closest_dist = Integer.MAX_VALUE;
	    String key_for_closest_sc = null;

	    for (Enumeration e = ht.keys(); e.hasMoreElements() ;) 
	    {
		String cluster_desc = (String) e.nextElement();
		SameClusters sc = (SameClusters) ht.get(cluster_desc);

		int dist = distanceBetween(sc, sc_in);

		if(dist < closest_dist)
		{
		    key_for_closest_sc = cluster_desc;
		    closest_dist = dist;
		}
	    }
	    
	    return key_for_closest_sc;
	    
	}

	// returns a distance metric 
	// the smaller the number (including <0), the closer the match
	//
	private int distanceBetween(SameClusters sc1, SameClusters sc2)
	{
	    Vector cl1 = sc1.c_ids;
	    Vector cl2 = sc2.c_ids;
	    
	    int matched = 0;
	    int not_matched = 0;

	    final int cl1s = cl1.size();
	    final int cl2s = cl2.size();

	    for(int outer=0; outer < cl1s; outer++)
	    {
		Integer oc = (Integer) cl1.elementAt(outer);

		for(int inner=0; inner < cl2s; inner++)
		{
		    Integer ic = (Integer) cl2.elementAt(inner);
		    
		    if(oc.equals(ic))
		    {
			matched += 2;
		    }
		    else
		    {
			not_matched++;
		    }
		}
	    }

	    // <0 for close (more matches than mismatchs), >0 for far (more mismatches than matches)
	    
	    return (not_matched - matched);

	}


	// sort without using knowledge of the cluster hierarchy,
	// instead compare order rows by comparing which
	// cluster leaves each one is one and grouping ones
	// in similar sets of clusters
	// (not very good when all rows are in exactly one
	//  cluster, but can cope with situations where 
	//  one or more rows are in more than one cluster)
	//
	//
	//  this version isn't very good at keeping similar things together
	//  and also takes forever to run....
	/*
	public boolean OLD_sortByComparing(boolean sort_spots)
	{
	    int n_spots = edata.getNumSpots();
	    
	    BitSet in_clusters[] = new BitSet[n_spots];
	    
	    for(int i=0; i < n_spots; i++)
		in_clusters[i] = null;
	    
	    // find out which visible clusters each gene is in...
	    //
	    int vis_clust_cnt = 0;
	    int els_in_clust_cnt = 0;
	    
	    ExprData.ClusterIterator clit = edata.new ClusterIterator();
	    
	    ExprData.Cluster clust = clit.getFirstLeaf();
	    int iv;
	    while(clust != null)
	    {
		if(clust.getShow() && clust.getIsSpot())
		{
		    vis_clust_cnt++;
		    
		    int[] ce = clust.getElements();
		    for(int gn=0; gn < ce.length; gn++)
		    {
			iv = ce[gn];
			
			if(iv < n_spots)
			{
			    if(in_clusters[iv] == null)
				in_clusters[iv] = new BitSet();
			    
			    in_clusters[iv].set(clust.getId());
			    
			    els_in_clust_cnt++;
			}
		    }
		}
		clust = clit.getNextLeaf();
	    }
	    
	    updateLabels(vis_clust_cnt, els_in_clust_cnt);

	    Thread.yield();

	    // vis_clust_cnt_label.update();
	    
	    //System.out.println("There are " + vis_clust_cnt + " visible clusters");
	    //System.out.println("with a total of " + genes_in_clust_cnt + " genes in them");
	    
	    
	    int progress_step = n_spots / 10;
	    int progress_done = 0;
	    int progress_tick = 0;
	    
	    int[] current_traversal = edata.getRowOrder();

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

			edata.setRowOrder(tmp_progress_order);
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
		    mview.addMessageToLog("SortClusters: BAD! run out of stuff on the list");
		    break;
		}

		yield();
	    }
	    
	    edata.setRowOrder(traversal_order);

	    return false;
	}
	*/
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
	panel.setPreferredSize(new Dimension(450, 250));
	panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

	int line = 0;

	
	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout wrapbag = new GridBagLayout();
	    wrapper.setLayout(wrapbag);
	    GridBagConstraints c = null;
	    
	    /*
	    {
		vis_clust_cnt_label = new JLabel("There are ?? visible clusters");
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.SOUTH;
		c.gridwidth = 2;
		wrapbag.setConstraints(vis_clust_cnt_label, c);
		wrapper.add(vis_clust_cnt_label);
	   
		els_in_clust_cnt_label = new JLabel("containing a total of ??? elements");
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 1.0;
		c.gridwidth = 2;
		wrapbag.setConstraints(els_in_clust_cnt_label, c);
		wrapper.add(els_in_clust_cnt_label);
	    }
	    */

	    {
		sort_spots_jchkb = new JCheckBox("Sort all Spot clusters");
		sort_spots_jchkb.setSelected(sort_spots);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 2;
		c.anchor = GridBagConstraints.WEST;
		c.weighty = c.weightx = 1.0;
		wrapbag.setConstraints(sort_spots_jchkb, c);
		wrapper.add(sort_spots_jchkb);
	    }
	    {
		sort_meas_jchkb = new JCheckBox("Sort all Measurement clusters");
		sort_meas_jchkb.setSelected(sort_meas);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 3;
		c.anchor = GridBagConstraints.WEST;
		c.weighty = c.weightx = 1.0;
		wrapbag.setConstraints(sort_meas_jchkb, c);
		wrapper.add(sort_meas_jchkb);
	    }
	    
	    {
		apply_filter_jchkb = new JCheckBox("Apply filter");
		apply_filter_jchkb.setSelected(apply_filter);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 4;
		c.anchor = GridBagConstraints.WEST;
		c.weighty = c.weightx = 1.0;
		wrapbag.setConstraints(apply_filter_jchkb, c);
		wrapper.add(apply_filter_jchkb);
	    }

	    {
		show_progress_jchkb = new JCheckBox("Show progress");
		show_progress_jchkb.setSelected(show_progress);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 5;
		c.anchor = GridBagConstraints.WEST;
		c.weighty = c.weightx = 1.0;
		wrapbag.setConstraints(show_progress_jchkb, c);
		wrapper.add(show_progress_jchkb);
	    }

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.weighty = 5.0;
	    c.weightx = 5.0;
	    c.anchor = GridBagConstraints.EAST;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(wrapper, c);
	    panel.add(wrapper);
	    
	}

	// =========================================================

	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout wrapbag = new GridBagLayout();
	    wrapper.setLayout(wrapbag);
	    GridBagConstraints c = null;
	    
	    cluster_tree = new DragAndDropTree();

	    cluster_tree.setDropAction( new DragAndDropTree.DropAction()
		{
		    public void dropped(DragAndDropEntity dnde)
		    {
			try
			{
			    ExprData.Cluster cl = dnde.getCluster();
			    
			    DefaultTreeModel model      = (DefaultTreeModel)       cluster_tree.getModel();
			    DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
			    
			    for (Enumeration e =  root.depthFirstEnumeration(); e.hasMoreElements() ;) 
			    {
				DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) e.nextElement();
				ExprData.Cluster ncl = (ExprData.Cluster) dmtn.getUserObject();
				if(cl == ncl)
				{
				    TreeNode[] tn_path = model.getPathToRoot(dmtn);
				    TreePath tp = new TreePath(tn_path);
				    
				    cluster_tree.expandPath(tp);
				    cluster_tree.scrollPathToVisible(tp);
				    cluster_tree.setSelectionPath(tp);
				    return;
				}
			    }
			}
			catch(DragAndDropEntity.WrongEntityException wee)
			{
			}
		    }
		});
	    
	    cluster_tree.setDragAction(new DragAndDropTree.DragAction()
		{
		    public DragAndDropEntity getEntity()
		    {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) cluster_tree.getLastSelectedPathComponent();
			
			if(node != null)
			{
			    ExprData.Cluster cluster = (ExprData.Cluster) node.getUserObject();
			    
			    DragAndDropEntity dnde = DragAndDropEntity.createClusterEntity(cluster);
			    
			    return dnde;
			}
			else
			    return null;
		    }
		});
	    
	    DefaultTreeSelectionModel dtsm = new DefaultTreeSelectionModel();
	    dtsm.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
	    cluster_tree.setSelectionModel(dtsm);

	    populateTreeWithClusters(cluster_tree, edata.getRootCluster() );
	    
	    
	    JScrollPane jsp = new JScrollPane(cluster_tree);
	    jsp.setPreferredSize(new Dimension(180, 200));
	    jsp.setMinimumSize(new Dimension(180, 200));

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = c.weighty = 10.0;
	    c.fill = GridBagConstraints.BOTH;
	    wrapbag.setConstraints(jsp, c);
	    wrapper.add(jsp);

	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.weighty = 5.0;
	    c.weightx = 5.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(wrapper, c);
	    panel.add(wrapper);
	    
	}
	line++;


	// =========================================================

	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrapper.setLayout(w_gridbag);
	    wrapper.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
	    
	    {
		JButton button = new JButton("Close");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    sort_spots    = sort_spots_jchkb.isSelected();
			    sort_meas     = sort_meas_jchkb.isSelected();
			    show_progress = show_progress_jchkb.isSelected();
			    apply_filter  = apply_filter_jchkb.isSelected();

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
		JButton button = new JButton("Sort");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    sort_spots    = sort_spots_jchkb.isSelected();
			    sort_meas     = sort_meas_jchkb.isSelected();
			    show_progress = show_progress_jchkb.isSelected();
			    apply_filter  = apply_filter_jchkb.isSelected();

			    SorterThread st = new SorterThread(null);
			    st.start();
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		//c.weighty = c.weightx = 1.0;
		w_gridbag.setConstraints(button, c);
	    }
	    {
		JButton button = new JButton("Help");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    mview.getPluginHelpTopic("SortClusters", "SortClusters");
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 0;
		c.gridwidth = 2;
		w_gridbag.setConstraints(button, c);
	    }

	    panel.add(wrapper);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    c.gridwidth = 2;
	    c.weightx = 1.0;
	    gridbag.setConstraints(wrapper, c);
	    
	}

	frame.getContentPane().add(panel);
    }

    private void updateLabels(int vis_clust_cnt, int els_in_clust_cnt)
    {
	if(vis_clust_cnt_label != null)
	{
	    if(vis_clust_cnt == 0)
		vis_clust_cnt_label.setText("There are no visible clusters");
	    if(vis_clust_cnt == 1)
		vis_clust_cnt_label.setText("There is one clusters");
	    if(vis_clust_cnt > 1)
		vis_clust_cnt_label.setText("There are " + vis_clust_cnt + " visible clusters");	
	    
	    if(els_in_clust_cnt == 0)
		els_in_clust_cnt_label.setText("containing no elements");
	    if(els_in_clust_cnt == 1)
		els_in_clust_cnt_label.setText("containing but one element");
	    if(els_in_clust_cnt > 1)
		els_in_clust_cnt_label.setText("containing a total of " + els_in_clust_cnt + " elements");
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
 
    private void populateTreeWithClusters(JTree tree, ExprData.Cluster cluster)
    {
	// record the selected items
	TreeSelectionModel tsm = tree.getSelectionModel();
	Hashtable sels = new Hashtable();
	if(tsm != null)
	{
	    TreePath[] tpaths = tsm.getSelectionPaths();
	    if(tpaths != null)
	    {
		for(int tp=0; tp < tpaths.length; tp++)
		{
		    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tpaths[tp].getLastPathComponent();
		    ExprData.Cluster clust = (ExprData.Cluster) node.getUserObject();
		    sels.put(String.valueOf(clust.getId()), clust);
		}
	    }
	}

	// System.out.println("making tree for " + cluster.getName() );

	DefaultMutableTreeNode dmtn = generateClusterTreeNodes( null, cluster );
	DefaultTreeModel model =  new DefaultTreeModel( dmtn );
	tree.setModel(model);

	tree.putClientProperty("JTree.lineStyle", "Angled");
	
	DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
	renderer.setLeafIcon(null);
	renderer.setOpenIcon(null);
	renderer.setClosedIcon(null);
	tree.setCellRenderer(renderer);

	if(sels.size() > 0)
	{
	    System.out.println(sels.size() + " saved selections");

	    /*
	    for (Enumeration e =  sels.keys(); e.hasMoreElements() ;) 
	    {
		String id = e.nextElement();
		ExprData.Cluster clust = (ExprData.Cluster) sels.get( id );
		
	    }
	    */
	    Vector tp_vec = new Vector();

	    for (Enumeration e = dmtn.depthFirstEnumeration(); e.hasMoreElements() ;) 
	    {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
		ExprData.Cluster clu = (ExprData.Cluster) node.getUserObject();
		String id = String.valueOf(clu.getId());
		if(sels.get(id) != null)
		{
		    // make a path for this node
		    TreeNode[] tn_path = model.getPathToRoot(node);
		    TreePath tp = new TreePath(tn_path);
		    // and record it for later
		    tp_vec.addElement(tp);
		}
	    }
	    // add all of the Selection paths in one go
	    TreePath[] tp_a = (TreePath[]) tp_vec.toArray(new TreePath[0]);
	    tree.setSelectionPaths( tp_a );

	    // panel.repaint();
	}

    }

    private DefaultMutableTreeNode generateClusterTreeNodes(DefaultMutableTreeNode parent, ExprData.Cluster clust)
    {
	{
	    DefaultMutableTreeNode node = new DefaultMutableTreeNode( clust );
	    
	    Vector ch = clust.getChildren();
	    if(ch != null)
	    {
		for(int c=0; c < ch.size(); c++)
		    generateClusterTreeNodes( node, ( ExprData.Cluster) ch.elementAt(c) );
	    }
	    
	    if(parent != null)
	    {
		parent.add(node);
		return parent;
	    }
	    else
	    {
		return node;
	    }
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private maxdView mview;
    private DataPlot dplot;
    private ExprData edata;

    private JFrame frame;

    private DragAndDropTree cluster_tree = null;

    private boolean sort_spots = true;
    private boolean sort_meas = true;
    private boolean show_progress = false;
    private boolean apply_filter = false;

    protected JCheckBox show_progress_jchkb, apply_filter_jchkb;
    protected JCheckBox sort_spots_jchkb, sort_meas_jchkb;


    private JLabel vis_clust_cnt_label = null;
    private JLabel els_in_clust_cnt_label = null;
}
