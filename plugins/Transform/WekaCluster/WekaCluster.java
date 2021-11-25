import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

import weka.core.*;
import weka.clusterers.*;

//
// interface to Weka
//
// 17/08 - uses Weka 3-4, added new clustering methods
//

public class WekaCluster implements ExprData.ExprDataObserver, Plugin
{
    public final static int COBWEB_METHOD   = 0;
    public final static int EM_METHOD       = 1;
    public final static int SIMPLE_K_METHOD = 2;
    public final static int F_FIRST_METHOD  = 3;

    public WekaCluster(maxdView mview_)
    {
	mview = mview_;
	edata = mview.getExprData();
	dplot = mview.getDataPlot();

    }

    public void cleanUp()
    {
	edata.removeObserver(this);

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
	maxdView.CustomClassLoader ccl = (maxdView.CustomClassLoader) getClass().getClassLoader();
	
	String jarfile = mview.getProperty("WekaCluster.weka_path", null);
	
	mview.setMessage("Initialising Weka classes");
	Thread.yield();

	boolean found = false;

	while(!found)
	{
	    if( jarfile != null )
	    {
		try
		{
		    ccl.addPath( jarfile );
		}
		catch( java.net.MalformedURLException murle )
		{
		    String msg = 
			"Unable to load the Weka JAR file from '" + jarfile + "'\n" +
			"\nPress \"Find\" to specify an alternate location for the file,\n" + 
			"  or\nPress \"Cancel\" to stop the plugin.\n" +
			"\n(see the help page for more information)\n";

		    if(mview.alertQuestion(msg, "Find", "Cancel") == 1)
			return;
		    
		    try
		    {
			jarfile = mview.getFile("Location of 'weka.jar'", jarfile);
		    }
		    catch( UserInputCancelled uic )
		    {
			return;
		    }
		}
	    }

	    Class wc = ccl.findClass("weka.clusterers.Cobweb");
	    
	    found = (wc != null);
	    
	    if(!found)
	    {
		try
		{
		    String msg = "Unable to find Weka JAR file\n";
		    msg += (jarfile == null)  ? "\n" : ("in '" + jarfile + "'\n");
		    msg += "\nPress \"Find\" to specify the location of the file,\n" + 
		    "  or\nPress \"Cancel\" to stop the plugin.\n" +
		    "\n(see the help page for more information)\n";
		    if(mview.alertQuestion(msg, "Find", "Cancel") == 1)
			return;
		    
		    jarfile = mview.getFile("Location of 'weka.jar'", jarfile);
		    
		}
		catch(UserInputCancelled uic)
		{
		    // don't start the plugin
		    return;
		}
	    }
	    else
	    {
		mview.putProperty("WekaCluster.weka_path", jarfile);
	    }
	}

	Class cob = ccl.findClass("weka.clusterers.Cobweb");
	Class em  = ccl.findClass("weka.clusterers.EM");

	//System.out.println("++ WekaCluster has been started ++");
	frame = new JFrame("Weka Clustering");
	mview.decorateFrame( frame );
	frame.addWindowListener(new WindowAdapter() 
	    {
		public void windowClosing(WindowEvent e)
		{
		    cleanUp();
		}
	    });

	addComponents(frame);
	frame.pack();
	frame.setVisible(true);
	edata.addObserver(this);
    }

    public void stopPlugin()
    {
	cleanUp();
    }
  
    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("Weka Clustering", "transform", 
					 "Interface to the Weka clustering algorithms", 
					 "Requires the WEKA Machine Learning package<BR>" + 
					 "Copyright 1998, 1999  Eibe Frank, Leonard Trigg, Mark Hall<BR>" + 
					 "See <A HREF=\"http://cs.waikato.ac.nz/ml/weka/\">" + 
					 "http://cs.waikato.ac.nz/ml/weka/</A>",
					 1, 0, 0);
	return pinf;
    }

    public PluginCommand[] getPluginCommands()
    {
	return null;
    }

    public void   runCommand(String name, String[] args, CommandSignal done) 
    { 
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

    public void cluster()
    {
	ClusteringThread ct = new ClusteringThread();
	ct.start();
    }

    // ---------------- --------------- --------------- ------------- ------------

    public void cancel()
    {
	cleanUp();
    }

    public class ClusteringThread extends Thread
    {
	public void run()
	{
	    cluster();
	}

	//
	// create a cluster hierarchy
	//
	public void createClustersFromIndices(String cname, int[] clust_allocs, int[] weka_to_maxd, int index_type)
	{
	    //
	    //System.out.println("exepecting " +  clust_allocs.length + " clusters...");
	    //System.out.println("       for " +  weka_to_maxd.length + " things...");
	    
	    ExprData.Cluster local_root = edata.new Cluster(cname);
	    
	    Hashtable clust_ids = new Hashtable();  // maps Weka name -> new cluster name
	    
	    for(int s=0; s < clust_allocs.length; s++)
	    {
		int weka_id = clust_allocs[s];
		String wname = "Weka-"  + String.valueOf(weka_id);
		Vector cl_elems = (Vector) clust_ids.get(wname);
		if(cl_elems == null)
		{
		    // this is a new Weka cluster id
		    // create a new list of cluster elements 
		    //
		    Vector new_elems = new Vector();
		    new_elems.addElement(new Integer(s));
		    clust_ids.put(wname, new_elems);
		}
		else
		{
		    // already seen this Weka cluster id
		    // add this element to the list of cluster elements for this id
		    //
		    cl_elems.addElement(new Integer(s));
		}
	    }
	    // now we have a table full of Vectors, each containing all the elements
	    // of one cluster, indexed by the id Weka gave it
	    //
	    // make a maxd cluster from each one...
	    //
	    // remembering to translate id's back to their proper positions
	    //
	    for (Enumeration e = clust_ids.keys() ; e.hasMoreElements() ;) 
	    {
		String clust_id = (String) e.nextElement();
		//System.out.println(clust_id);

		Vector weka_ids = (Vector)clust_ids.get(clust_id);
		Vector maxd_ids = new Vector();
		for(int s=0; s < weka_ids.size(); s++)
		{
		    int t = ((Integer) weka_ids.elementAt(s)).intValue();
		    int t2 = weka_to_maxd[t];

		    if(index_type == ExprData.SpotName)
			maxd_ids.addElement(edata.getSpotName(t2));
		    else
			maxd_ids.addElement(edata.getMeasurementName(t2));
		}
		
		ExprData.Cluster child = edata.new Cluster(clust_id, index_type, maxd_ids);
		local_root.addCluster(child);
	    }
	    
	    edata.addCluster(local_root);
	}
	
	public void cluster()
	{
	    int[] selected_measurement_indices = getSelectedMeasurements();

	    int n_meas_out = selected_measurement_indices.length;
	    
	    if(n_meas_out == 0)
	    {
		mview.errorMessage("Cannot cluster: No Measurements are selected");
		return;
	    }
	    
	    int n_spots_out = 0;

	    for(int s=0; s < edata.getNumSpots(); s++)
	    {
		if((!apply_filter_jchkb.isSelected()) || (!edata.filter(s)))
		{
		    {
			n_spots_out++;
		    }
		}
	    }
	    
	    if(n_spots_out == 0)
	    {
		mview.errorMessage("Cannot cluster: No Spots are selected (they have all be filtered out)");
		return;
	    }

	    ProgressOMeter pm = new ProgressOMeter("Clustering");
	    pm.startIt();
	    pm.setMessage("Preparing...");
	    
	    int total_elems = 0;

	    // convert (filtered) spots indices to a contiguous ordering 
	    //
	    int[] cluster_to_spot = new int[n_spots_out];

	    if(apply_filter_jchkb.isSelected())
	    {
		int c2s = 0;
		for(int s=0; s < edata.getNumSpots(); s++)
		{
		    if((!apply_filter_jchkb.isSelected()) || (!edata.filter(s)))
		    {
			{
			    cluster_to_spot[c2s++] = s;
			}
		    }
		}
	    }
	    else
	    {
		// no filter, use all spots in their current order
		for(int s=0; s < n_spots_out; s++)
		    cluster_to_spot[s] = s;
	    }

	    int[] cluster_to_meas = selected_measurement_indices;

	    // now we know which data we are dealing with....build the Weka Instance objects
	    //
	    int[] back_convert = null;
	    int thing_count = 0;
	    int att_count = 0;
	    Instances insts = null;
	    int index_type = -1;

	    if(cluster_spots_jrb.isSelected())
	    {
		// clustering by spots

		FastVector atts = new FastVector(n_meas_out);
	    
		for(int m =0; m < n_meas_out; m++)
		{
		    atts.addElement( new Attribute( edata.getMeasurementName( cluster_to_meas[ m ] ) ) );
		}

		insts = new Instances("spot", atts,  n_spots_out);

		for(int s=0; s < n_spots_out; s++)
		{
		    Instance i = new Instance(n_meas_out);

		    for(int m =0; m < n_meas_out; m++)
		    {
			total_elems++;
			i.setValue( m, edata.eValue( cluster_to_meas[ m ], cluster_to_spot[ s ]) );
		    }
		    
		    insts.add(i);
		}

		back_convert = cluster_to_spot;
		thing_count = n_spots_out;
		att_count = n_meas_out;
		index_type = ExprData.SpotName;

		//System.out.println("clustering " + n_spots_out + " spots with " + n_meas_out + " values each");

	    }
	    else
	    {
		// clustering by measurements

		FastVector atts = new FastVector(n_spots_out);

		for(int s =0; s < n_spots_out; s++)
		    atts.addElement( new Attribute( edata.getSpotName( cluster_to_spot[s] ) ) );
		
		insts = new Instances("meas", atts, n_meas_out);

		for(int m = 0; m < n_meas_out; m++)
		{
		    Instance i = new Instance(n_spots_out);
		    
		    for(int s=0; s < n_spots_out; s++)
		    {
			total_elems++;
			i.setValue(s, edata.eValue( cluster_to_meas[m], cluster_to_spot[s] ) );
		    }

		    insts.add(i);
		}

		back_convert = cluster_to_meas;
		thing_count = n_meas_out;
		index_type = ExprData.MeasurementName;
		att_count = n_spots_out;
		//System.out.println("clustering " + n_meas_out + " measurements with " + n_spots_out + " values each");

	    }
	    
	    //System.out.println("expecting results for " + thing_count + " things");
	    
	    pm.setMessage("Clustering..." + thing_count + "x" + att_count);
	    

	    int[] clust_allocs = new int[ thing_count ];    // whereabouts each of the instances ended up

	    String cname       = null;   // the name used for the root cluster

	    try
	    {
		switch( tab.getSelectedIndex() )
		{
		    case COBWEB_METHOD:
			
			Cobweb cw = new Cobweb();
			
			cw.setAcuity(cobweb_acuity.getValue());
			cw.setCutoff(cobweb_cutoff.getValue());
			
			cw.buildClusterer(insts);
			
			//System.out.println(" Cobweb clusterer created");
			//System.out.println("  acuity: " + cw.getAcuity() + "%, cutoff: " + cw.getCutoff() + "%");
			
			//
			// create a cluster hierarchy (albeit a very flat one...)
			//
			
			for(int t=0; t < thing_count; t++)
			{
			    pm.setMessage(t + " of " + thing_count);
			    
			    clust_allocs[t] = cw.clusterInstance(insts.instance(t));
			}
			
			cname = "Weka Cobweb: acuity " + cobweb_acuity.getValue() + ", cutoff " + cobweb_cutoff.getValue();
			
			break;
			
		    case EM_METHOD:
			
			EM em = new EM();
			
			em.setNumClusters(em_num_clusts.getValue());
			em.setMaxIterations(em_max_iters.getValue());
			
			em.buildClusterer(insts);
			
			//System.out.println(" EM clusterer created");
			//System.out.println("  num clusters: " + em.getNumClusters() + ", max iters: " + em.getMaxIterations());
			
			for(int t=0; t < thing_count; t++)
			{
			    
			    double[] tmp = em.distributionForInstance(insts.instance(t));
			    
			    /*
			      System.out.println(s + ":\t");
			      for(int t=0; t < tmp.length; t++)
			      System.out.print(tmp[t] + "\t");
			      System.out.println("");
			    */
			    
			    // very simple processing: pick the most likely cluster
			    // (i.e. the one with the highest prob)
			    //
			    int most_likely = 0;
			    double likelyhood = tmp[0];
			    if(tmp.length > 1)
			    {
				for(int c=1; c<tmp.length; c++)
				{
				    if(tmp[c] > likelyhood)
				    {
					likelyhood = tmp[c];
					most_likely = c;
				    }
				}
			    }
			    clust_allocs[t] = most_likely;
			    
			}
			
			cname = "Weka EM: " + em_num_clusts.getValue() + " clusters, " + em_max_iters.getValue() + " iterations";
			
			break;
			
		    case SIMPLE_K_METHOD:
			
			SimpleKMeans skm = new SimpleKMeans();
			
			
			if( skm_auto_n_clusters.isSelected() == false )
			{
			    skm.setNumClusters( skm_n_clusters.getValue() );
			}
			
			skm.buildClusterer( insts );
			
			for(int t=0; t < thing_count; t++)
			{
			    pm.setMessage(t + " of " + thing_count);

			    clust_allocs[t] = skm.clusterInstance( insts.instance(t) );
			}
			
			cname = "Weka Simple K-Means";
			
			break;
			
		    case F_FIRST_METHOD:
			
			FarthestFirst ff = new FarthestFirst();
			
			if( ff_auto_n_clusters.isSelected() == false )
			{
			    ff.setNumClusters( ff_n_clusters.getValue() );
			}
			
			ff.buildClusterer( insts );
			
			
			for(int t=0; t < thing_count; t++)
			{
			    pm.setMessage(t + " of " + thing_count);

			    clust_allocs[t] = ff.clusterInstance( insts.instance(t) );
			}
			
			cname = "Weka Farthest First";
			
			break;
			
		}
		
		createClustersFromIndices(cname, clust_allocs, back_convert, index_type);
	    
		pm.stopIt();
	    }
	    catch(Exception e)
	    {
		e.printStackTrace();
		pm.stopIt();
		mview.alertMessage( "Unexpected exception '" + e.toString() + "' during clustering" );
		return;
	    }
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  stuff
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private JTabbedPane tab;
    private NicerSlider cobweb_acuity;
    private NicerSlider cobweb_cutoff;
    private NicerSlider em_num_clusts;
    private NicerSlider em_max_iters;

    private JCheckBox apply_filter_jchkb;

    private JRadioButton cluster_spots_jrb, cluster_meas_jrb;

    private NicerSlider skm_n_clusters;
    private JCheckBox skm_auto_n_clusters;
    private NicerSlider ff_n_clusters;
    private JCheckBox ff_auto_n_clusters;

    private void setupSlider( NicerSlider ns, int major_tick_space, int minor_tick_space )
    {
	JSlider js = ns.getSlider();
	js.setMajorTickSpacing(major_tick_space);
	js.setMinorTickSpacing(minor_tick_space);
	js.setPaintTicks(true);
	js.setPaintLabels(true);
    }

    private void addComponents(JFrame frame)
    {
	GridBagLayout gridbag = new GridBagLayout();
	JPanel panel = new JPanel();
	GridBagConstraints c = null;

	panel.setLayout(gridbag);
	panel.setPreferredSize(new Dimension(640, 450));

	/*
	{
	    JLabel label = new JLabel("Weka Cluster Algorithms");
	    panel.add(label);
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.gridwidth = 2;
	    //c.anchor = GridBagConstraints.NORTH;
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(label, c);
	}
	*/

	{
	    JPanel meas_sel_panel = new JPanel();
	    
	    GridBagLayout ms_gridbag = new GridBagLayout();

	    meas_sel_panel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
	    meas_sel_panel.setLayout(ms_gridbag);

	    meas_list = new DragAndDropList();
	    JScrollPane jsp = new JScrollPane(meas_list);

	    meas_list.setModel(new MeasListModel());

	    meas_list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

/*	    
	    for(int m=0; m < edata.getNumMeasurements(); m++)
	    {
		int mi = edata.getMeasurementAtIndex(m);

		meas_sel_jchkb[m] = new JCheckBox(edata.getMeasurementName(mi));
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 0;
		c.gridy = m;
		ms_gridbag.setConstraints(meas_sel_jchkb[m], c);
		meas_sel_panel.add(meas_sel_jchkb[m]);

	    }
*/
	    
	    jsp = new JScrollPane( meas_list );
	    c = new GridBagConstraints();
	    //c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.BOTH;
	    c.gridx = 0;
	    c.gridy = 1;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    gridbag.setConstraints(jsp, c);
	    panel.add(jsp);

	    //int line = edata.getNumMeasurements() + 1;

	    Font small_font = null;

	    //if(line > 3)
	    {
		JPanel innerp = new JPanel();
		{
		    JButton jb = new JButton("All");

		    Font f = jb.getFont();
		    small_font = new Font(f.getName(), Font.PLAIN, f.getSize() - 2);
		    jb.setFont(small_font);

		    jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    meas_list.setSelectionInterval(0, edata.getNumMeasurements()-1);
			}
		    });
		    innerp.add(jb);
		}
		{
		    JButton jb = new JButton("None");
		    jb.setFont(small_font);
		    jb.addActionListener(new ActionListener() 
			{
			    public void actionPerformed(ActionEvent e) 
			    {
				meas_list.clearSelection();
			    }
			});
		    innerp.add(jb);
		}

		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 0;
		c.gridy = 2;
		gridbag.setConstraints(innerp, c);
		panel.add(innerp);	
	    }

	    /*
	    {
		only_clusters_jchkb = new JCheckBox("Only clusters");
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 0;
		c.gridy = line++;
		ms_gridbag.setConstraints(only_clusters_jchkb, c);
		meas_sel_panel.add(only_clusters_jchkb);
	    }
	    */


	}

	// -----------------------------------------------------------------------------------------
	//
	// options for the 'Cobweb' method
	//
	// -----------------------------------------------------------------------------------------

	tab = new JTabbedPane();
	//JPanel outer_panel = new JPanel;
	tab.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	//tab.setPreferredSize(new Dimension(400, 250));

	{
	    GridBagLayout innerbag = new GridBagLayout();
	    JPanel tab_panel = new JPanel();
	    
	    tab_panel.setLayout(innerbag);
	    //panel.setPreferredSize(new Dimension(350, 200));


	    {
		cobweb_acuity = new NicerSlider( "Acuity (%) ", 0, 100, 50 );
		setupSlider( cobweb_acuity, 25, 5 );
		tab_panel.add(cobweb_acuity);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		innerbag.setConstraints(cobweb_acuity, c);
	    }
	    {
		cobweb_cutoff = new NicerSlider( "Cutoff (%) ", 0, 100, 50 );
		setupSlider( cobweb_cutoff, 25, 5 );
		tab_panel.add(cobweb_cutoff);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.weighty = c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		innerbag.setConstraints(cobweb_cutoff, c);
	    }

	    tab.addTab(" Cobweb ", tab_panel);
	}

	// -----------------------------------------------------------------------------------------
	//
	// options for the 'Expectation Maximisation' method
	//
	// -----------------------------------------------------------------------------------------
	
	{
	    GridBagLayout innerbag = new GridBagLayout();
	    JPanel tab_panel = new JPanel();
	    
	    tab_panel.setLayout(innerbag);
	    //panel.setPreferredSize(new Dimension(350, 200));


	    {
		em_max_iters = new NicerSlider( "Max. Iterations ", 0, 10000, 5000 );
		setupSlider( em_max_iters, 2500, 200 );
		tab_panel.add(em_max_iters);
		 c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		innerbag.setConstraints(em_max_iters, c);
	    }
	    {
		int n_spots = mview.getExprData().getNumSpots();
		int start_val = n_spots / 5;
		em_num_clusts = new NicerSlider( "Clusters ", 0, n_spots, n_spots > 5 ? 5 : n_spots );
		setupSlider( em_num_clusts, n_spots/5, n_spots/20 );

		tab_panel.add(em_num_clusts);
		 c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.weighty = c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		innerbag.setConstraints(em_num_clusts, c);
	    }

	    tab.addTab(" Expectation Maximisation ", tab_panel);
	}


	// -----------------------------------------------------------------------------------------
	//
	// options for the 'Simple K-Means' method
	//
	// -----------------------------------------------------------------------------------------

	{
	    GridBagLayout innerbag = new GridBagLayout();
	    JPanel tab_panel = new JPanel();
	    
	    tab_panel.setLayout(innerbag);
	    //panel.setPreferredSize(new Dimension(350, 200));


	    {
		int n_spots = mview.getExprData().getNumSpots();
		
		skm_n_clusters = new NicerSlider( "Clusters ", 0, n_spots, n_spots > 5 ? 5 : n_spots );
		setupSlider( skm_n_clusters, n_spots/5, n_spots/20 );

		tab_panel.add( skm_n_clusters );
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		innerbag.setConstraints( skm_n_clusters, c);
	    }
	    {
		skm_auto_n_clusters = new JCheckBox( "Automatically select number of clusters" );

		tab_panel.add( skm_auto_n_clusters );
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.weighty = c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		innerbag.setConstraints( skm_auto_n_clusters, c );
	    }

	    tab.addTab(" Simple K-Means ", tab_panel);
	}


	
	// -----------------------------------------------------------------------------------------
	//
	// options for the 'Farthest First' method
	//
	// -----------------------------------------------------------------------------------------

	{
	    GridBagLayout innerbag = new GridBagLayout();
	    JPanel tab_panel = new JPanel();
	    
	    tab_panel.setLayout(innerbag);
	    //panel.setPreferredSize(new Dimension(350, 200));


	    {
		int n_spots = mview.getExprData().getNumSpots();
		
		ff_n_clusters = new NicerSlider( "Clusters ", 0, n_spots, n_spots > 5 ? 5 : n_spots );
		setupSlider( ff_n_clusters, n_spots/5, n_spots/20 );

		tab_panel.add( ff_n_clusters );
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		innerbag.setConstraints( ff_n_clusters, c);
	    }
	    {
		ff_auto_n_clusters = new JCheckBox( "Automatically select number of clusters" );

		tab_panel.add( ff_auto_n_clusters );
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.weighty = c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		innerbag.setConstraints( ff_auto_n_clusters, c );
	    }

	    tab.addTab(" Farthest First ", tab_panel);
	}



	{
	    panel.add(tab);
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 1;
	    c.weighty = 1.0;
	    c.weightx = 5.0;
	    //c.anchor = GridBagConstraints.NORTH;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(tab, c);
	}
	
	{
	    ButtonGroup bg = new ButtonGroup();
	    
	    JPanel wrapper = new JPanel();
	    wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.X_AXIS));

	    apply_filter_jchkb = new JCheckBox("Apply filter");
	    wrapper.add(apply_filter_jchkb);
	    
	    cluster_spots_jrb = new JRadioButton("Cluster by Spots");
	    cluster_spots_jrb.setSelected(true);
	    wrapper.add(cluster_spots_jrb);
	    bg.add(cluster_spots_jrb);
	    cluster_meas_jrb  = new JRadioButton("Cluster by Measurements");
	    wrapper.add(cluster_meas_jrb);
	    bg.add(cluster_meas_jrb);
	    

	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 2;
	    //c.weighty = 1.0;
	    c.weightx = 5.0;
	    //c.anchor = GridBagConstraints.NORTH;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(wrapper, c);
	    panel.add(wrapper);

	}

	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrapper.setLayout(w_gridbag);
	    wrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

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
		
		 c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		//c.weighty = c.weightx = 1.0;
		//c.anchor = GridBagConstraints.EAST;
		w_gridbag.setConstraints(button, c);
	    }
	    {
		JButton button = new JButton("Cluster");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    cluster();
			}
		    });
		
		 c = new GridBagConstraints();
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
			    mview.getPluginHelpTopic("WekaCluster", "WekaCluster");
			}
		    });
		
		 c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 0;
		//c.weighty = c.weightx = 1.0;
		w_gridbag.setConstraints(button, c);
	    }

	    c = new GridBagConstraints();
	    c.gridwidth = 2;
	    c.gridx = 0;
	    c.gridy = 4;
	    //c.weighty = c.weightx = 1.0;
	    gridbag.setConstraints(wrapper, c);

	    panel.add(wrapper);	    
	}

	frame.getContentPane().add(panel);
    }


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


    public int[] getSelectedMeasurements()
    {
	int n_meas = 0;

	for(int m=meas_list.getMinSelectionIndex(); m <= meas_list.getMaxSelectionIndex(); m++)
	{
	    if(meas_list.getSelectionModel().isSelectedIndex(m))
	    {
		n_meas++;
	    }
	}

	int[] meas_id = new int[ n_meas ];
	
	int nm = 0;

	for(int m=meas_list.getMinSelectionIndex(); m <= meas_list.getMaxSelectionIndex(); m++)
	{
	    if(meas_list.getSelectionModel().isSelectedIndex(m) )
	    {
		meas_id [ nm ] = edata.getMeasurementAtIndex( m );

		nm++;
	    }
	}

	return meas_id;
    }

    // ----------------------------------------------------------------------------------------------------------


    public class NicerSlider extends JPanel
    {
	public NicerSlider( String label_string, int min, int max, int init )
	{
	    slider = new JSlider( min, max, init );

	    slider.addChangeListener( new javax.swing.event.ChangeListener()
		{
		    public void	stateChanged( javax.swing.event.ChangeEvent e ) 
		    {
			if( dont_update_yourself == false )
			    input.setText( String.valueOf( slider.getValue() ) );
		    }
		} );

	    GridBagLayout gridbag = new GridBagLayout();
	    GridBagConstraints c = null;
	    setLayout(gridbag);
	    

	    label = new JLabel( label_string );
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.EAST;
	    c.weightx = 1.0;
	    gridbag.setConstraints( label, c );
	    add( label );

	    
	    input = new JTextField( 8 );
	    input.setHorizontalAlignment( JTextField.RIGHT );
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.anchor = GridBagConstraints.EAST;
	    c.weightx = 1.0;
	    gridbag.setConstraints( input, c );
	    add( input );


	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.WEST;
	    c.weightx = 9.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.gridheight = 2;
	    gridbag.setConstraints( slider, c );
	    add( slider );


	    input.setText( String.valueOf( init ) );

	    input.getDocument().addDocumentListener( new UserInputChangeListener() );
	}

	public int getValue()
	{
	    return slider == null ? 0 : slider.getValue();
	}

	public void setValue( int v )
	{
	    if( slider != null ) 
		slider.setValue( v );
	}


	
	class UserInputChangeListener implements javax.swing.event.DocumentListener 
	{
	    public void insertUpdate(javax.swing.event.DocumentEvent e)  { propagate(e); }
	    public void removeUpdate(javax.swing.event.DocumentEvent e)  { propagate(e); }
	    public void changedUpdate(javax.swing.event.DocumentEvent e) { propagate(e); }
	    
	    private void propagate(javax.swing.event.DocumentEvent e)
	    {
		try
		{
		    String val = e.getDocument().getText( 0, e.getDocument().getLength() );
		    
		    dont_update_yourself = true;

		    slider.setValue( new Integer( val ).intValue() );
		}
		catch (javax.swing.text.BadLocationException ble )
		{
		}
		catch ( NumberFormatException nfe )
		{
		}
		dont_update_yourself = false;
	    }
	}

	private boolean dont_update_yourself = false;    // stop recursive update misery

	public JSlider getSlider() { return slider; }

	private JSlider slider;
	private JLabel label;
	private JTextField input;

	
    }


    // ----------------------------------------------------------------------------------------------------------


    private JFrame frame = null;

    private DragAndDropList meas_list;

    private maxdView mview;
    private DataPlot dplot;
    private ExprData edata;

}
