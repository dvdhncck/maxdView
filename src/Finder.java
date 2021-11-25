import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;


public class Finder
{
    public Finder(maxdView mview_)
    {
	mview = mview_;
	edata = mview.getExprData();
	dplot = mview.getDataPlot();

        final JFrame find_f = new JFrame("maxdView: Find");
	final JPanel find_p = new JPanel();
	
	mview.decorateFrame(find_f);

	final int text_field_width = 14;

	final TimedLabel status_l = new TimedLabel();

	final DragAndDropTextField s_jtf = new DragAndDropTextField(text_field_width);

	s_jtf.setEntityAdaptor(new DragAndDropTextField.EntityAdaptor()
	    {
		public String getName(DragAndDropEntity dnde)
		{
		    try
		    {
			//System.out.println("adapting for spot name.." + dnde.getSpotID());
			ExprData.NameTagSelection nt_sel = nts.getNameTagSelection();
			return nt_sel.getNameTag( dnde.getSpotId() );
		    }
		    catch(DragAndDropEntity.WrongEntityException wee)
		    {
			return null;
		    }
		}
	    });

	final DragAndDropTextField c_jtf = new DragAndDropTextField(text_field_width);
	c_jtf.setEntityAdaptor(new DragAndDropTextField.EntityAdaptor()
	    {
		public String getName(DragAndDropEntity dnde)
		{
		    try
		    {
			return dnde.getCluster().getName();
		    }
		    catch(DragAndDropEntity.WrongEntityException wee)
		    {
			return null;
		    }
		}
	    });

	s_jtf.setText(mview.getProperty("FindDialog.name",  ""));
	c_jtf.setText(mview.getProperty("FindDialog.clust_name",  ""));

	final JCheckBox exact_jchkb = new JCheckBox("Exact match");
	final JCheckBox case_jchkb  = new JCheckBox("Case sensitive");

	exact_jchkb.setSelected(mview.getBooleanProperty("FindDialog.exact", false));
	case_jchkb.setSelected(mview.getBooleanProperty("FindDialog.case", false));

	find_p.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));

	find_f.getContentPane().add(find_p, BorderLayout.CENTER);

	GridBagLayout gbag = new GridBagLayout();
	find_p.setLayout(gbag);


	int line = 0;

	{
	    nts = new NameTagSelector(mview);
	    nts.loadSelection("FindDialog.name_tag_sel");

	    nts.addActionListener(new ActionListener() 
		{ 
			public void actionPerformed(ActionEvent e) 
		    {
			nts.saveSelection("FindDialog.name_tag_sel");
			
			doFind(s_jtf.getText(), nts.getNameTagSelection(),  
			       exact_jchkb.isSelected(), case_jchkb.isSelected(), 
			       status_l);
		    }
		});

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridy = line;
	    c.anchor =  GridBagConstraints.EAST;
	    gbag.setConstraints(nts, c);
	    find_p.add(nts);
	}
	{
	    find_p.add(s_jtf);
	    s_jtf.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			doFind(s_jtf.getText(), nts.getNameTagSelection(),
			       exact_jchkb.isSelected(), case_jchkb.isSelected(), 
			       status_l);
			
			// doFind(s_jtf.getText(), 0, exact_jchkb.isSelected(), case_jchkb.isSelected(), status_l);
		    }
		});

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.fill =  GridBagConstraints.BOTH;
	    c.weightx = c.weighty = 1.0;
	    gbag.setConstraints(s_jtf, c);
	}
	{
	    JButton find_jb = new JButton("Go");
	    find_p.add(find_jb);
	    find_jb.setFont(mview.getSmallFont());
	    find_jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			doFind(s_jtf.getText(), nts.getNameTagSelection(),
			       exact_jchkb.isSelected(), case_jchkb.isSelected(), 
			       status_l);
			//doFind(s_jtf.getText(), 0, exact_jchkb.isSelected(), case_jchkb.isSelected(), status_l);
		    }
		});

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = line++;
	    c.fill =  GridBagConstraints.VERTICAL;
	    gbag.setConstraints(find_jb, c);

	}

	{
	    JLabel label = new JLabel("Cluster ");
	    find_p.add(label);

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.anchor =  GridBagConstraints.EAST;
	    gbag.setConstraints(label, c);

	}
	{
	    find_p.add(c_jtf);
	    c_jtf.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			doFindCluster(c_jtf.getText(), exact_jchkb.isSelected(), case_jchkb.isSelected(), status_l);
		    }
		});
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.weighty = c.weightx = 1.0;
	    c.fill =  GridBagConstraints.BOTH;
	    gbag.setConstraints(c_jtf, c);
	}
	{
	    JButton find_jb = new JButton("Go");
	    find_jb.setFont(mview.getSmallFont());
	    find_jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			doFindCluster(c_jtf.getText(), exact_jchkb.isSelected(), case_jchkb.isSelected(), status_l);
		    }
		});
	    find_p.add(find_jb);

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = line++;
	    c.fill =  GridBagConstraints.VERTICAL;
	    gbag.setConstraints(find_jb, c);

	}
	
	
	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout wbag = new GridBagLayout();
	    wrapper.setLayout(wbag);

	    // ------------
	    
	    exact_jchkb.setHorizontalTextPosition(AbstractButton.LEFT);
	    exact_jchkb.setFont(mview.getSmallFont());

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.fill =  GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.EAST;
	    wbag.setConstraints(exact_jchkb, c);

	    wrapper.add(exact_jchkb);

	    // ------------

	    case_jchkb.setHorizontalTextPosition(AbstractButton.RIGHT);
	    case_jchkb.setFont(mview.getSmallFont());

	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.fill =  GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    wbag.setConstraints(case_jchkb, c);

	    wrapper.add(case_jchkb);

	    // ------------

	    status_l.setHorizontalAlignment(JLabel.CENTER);
	    status_l.setFont(mview.getSmallFont());
	    status_l.setForeground(new Color(140, 100, 200));

	    status_l.addComponentToHide( exact_jchkb );
	    status_l.addComponentToHide( case_jchkb );

	    c = new GridBagConstraints();
	    c.gridwidth = 2;
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.fill =  GridBagConstraints.VERTICAL;
	    wbag.setConstraints(status_l, c);

	    wrapper.add(status_l);

	    
	    // ------------

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridwidth = 3;
	    c.gridy = line++;
	    gbag.setConstraints(wrapper, c);

	    find_p.add(wrapper);
	}

	{   
	    JPanel wrapper = new JPanel();
	    GridBagLayout wbag = new GridBagLayout();
	    wrapper.setLayout(wbag);

	    // ------------

	    final JButton hjb = new JButton("Help");
	    hjb.setFont(mview.getSmallFont());
	    hjb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			mview.getHelpTopic("ViewerFind");
		    }
		});

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    //c.fill =  GridBagConstraints.HORIZONTAL;
	    gbag.setConstraints(hjb, c);
	    wrapper.add(hjb);

	    /*
	    final JButton ojb = new JButton("Options");
	    ojb.setFont(mview.getSmallFont());
	    ojb.setEnabled(false);

	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    //c.fill =  GridBagConstraints.HORIZONTAL;
	    gbag.setConstraints(ojb, c);
	    wrapper.add(ojb);
	    */

	    final JButton jb = new JButton("Close");
	    jb.setFont(mview.getSmallFont());
    
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			Point pt = find_f.getLocation();

			mview.putIntProperty("FindDialog.width",  find_p.getWidth());
			mview.putIntProperty("FindDialog.height", find_p.getHeight());

			mview.putBooleanProperty("FindDialog.exact", exact_jchkb.isSelected());
			mview.putBooleanProperty("FindDialog.case",  case_jchkb.isSelected());
			
			mview.putProperty("FindDialog.name",   s_jtf.getText());
			/*
			mview.putProperty("FindDialog.probe_name",  p_jtf.getText());
			mview.putProperty("FindDialog.gene_name",   g_jtf.getText());
			mview.putProperty("FindDialog.clust_name",  c_jtf.getText());
			*/

			find_f.setVisible(false);
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    gbag.setConstraints(jb, c);
	    wrapper.add(jb);
	    
	    // ------------

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    c.gridwidth = 3;
	    c.fill =  GridBagConstraints.HORIZONTAL;
	    c.weightx = 1.0;
	    gbag.setConstraints(wrapper, c);

	    find_p.add(wrapper);
	}

	int w = mview.getIntProperty("FindDialog.width",  250);
	int h = mview.getIntProperty("FindDialog.height", 150);

	find_p.setPreferredSize(new Dimension(w, h));

	find_f.pack();
	
	Point pt = dplot.getLocationOnScreen();
	
	find_f.setLocation(pt.x + dplot.getWidth() - find_f.getWidth(), pt.y);
	
	find_f.setVisible(true);

    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  F i n d i n g     t h i n g s     b y     n a m e
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private boolean findMatches(String s1, String s2, boolean exact, boolean case_sens)
    {
	if(!exact)
	{
	    if(!case_sens)
	    {
		return (s2.toLowerCase().indexOf(s1.toLowerCase()) >= 0);
	    }
	    else
	    {
		return (s2.indexOf(s1) >= 0);
	    }	    
	}
	else
	{
	    if(!case_sens)
	    {
		return s2.toLowerCase().equals(s1.toLowerCase());
	    }
	    else
	    {
		return s2.equals(s1);
	    }
	}
    }

    // cur is a spot_index in the current traversal order
    // and so is the returned value
    //
    // returns '>= num_spots' when the end of the list is reached
    //
    private int getNextSpot(final int cur)
    {
	if(cur >= edata.getNumSpots())
	    return edata.getNumSpots();

	int cur_spot = edata.getSpotAtIndex(cur);
	if(++cur_spot < edata.getNumSpots())
	    return edata.getIndexOf(cur_spot);
	else
	    return edata.getNumSpots();

    }

    final int SpotName = 0;
    final int ProbeName = 1;
    final int GeneName = 2;
    final int ClusterName = 3;

    public final boolean debug_find = false;

    private final void doFind(final String s, final ExprData.NameTagSelection nt_sel, 
			      final boolean exact, final boolean case_sens, 
			      final TimedLabel status_label)
    {
	if((s == null) || (s.length() == 0) || (edata.getNumSpots() == 0))
	{
	    status_label.setText("ready...");
	    return;
	}
	
	boolean apply_filter = dplot.getUseFilter();
	
	current_find_pos_i = edata.getIndexOf(current_find_pos);

	// are we still searching for the same thing?
	if(!s.equals(current_string) || (current_case != case_sens) || (current_exact != exact))
	{
	    // no, reset all of the counters and begin a new search from the current position
	    
	    current_string = s;
	    current_match_count = 0;
	    hidden_match_count = 0;
	    current_case = case_sens;
	    current_exact = exact;

	    current_find_start_pos = current_find_pos;
	}

	// System.out.println(s + " " + name_mode + " " + exact + " " + case_sens);

	if(current_find_start_pos > edata.getNumSpots())
	    current_find_start_pos = 0;

	// work through the spots current traversal order so the
	// search matches the order of the spots in the main display
	//

	// but if the spots have been reordered between searches, this 
	// position will be knackered, so keep track of the real position too

	current_find_pos_i = getNextSpot(current_find_pos_i);

	if(current_find_pos_i >= edata.getNumSpots())
	    current_find_pos_i = edata.getIndexOf(0);

	while(true)
	{
	    if(debug_find)
		System.out.println("find: i=" + current_find_pos_i + " s=" + edata.getSpotAtIndex(current_find_pos_i));

	    String name = nt_sel.getNameTag( current_find_pos_i );

	    /*
	    String name = null;
	    switch(name_mode)
	    {
	    case SpotName:
		name = edata.getSpotName(current_find_pos_i);
		break;
	    case ProbeName:
		name = edata.getProbeName(current_find_pos_i);
		break;
	    case GeneName:
		name = edata.getGeneName(current_find_pos_i);
		break;
	    }
	    */

	    if((name != null) && findMatches(s, name, exact, case_sens))
	    {
		if(/*!apply_filter ||*/ !edata.filter(current_find_pos_i))
		{
		    current_match_count++;
		    status_label.setText("found... " + current_match_count);
		    dplot.displaySpot(current_find_pos_i);
		    return;
		}
		else
		{
		    hidden_match_count++;
		    status_label.setText("found, hidden by filter(s)...");
		    return;
		}
	    }

	    
	    // work through the spots current traversal order so the
	    // search matches the order of the spots in the main display
	    //
	    current_find_pos_i = getNextSpot(current_find_pos_i);
	    
	    if(current_find_pos_i >= edata.getNumSpots())
	    {
		// fallen off the bottom...
		//
		status_label.setText("restarting at top...");
		
		current_find_pos_i = edata.getIndexOf(0);

		if(debug_find)
		    System.out.println("restarting at top...");
	    }
	    
	    current_find_pos = edata.getSpotAtIndex(current_find_pos_i);


	    if(edata.getSpotAtIndex(current_find_pos_i) == current_find_start_pos)
	    {
		// we've got back to where we started from

		if(current_match_count == 0)
		{
		    if(hidden_match_count == 0)
			status_label.setText("search finished, not found");
		    else
			status_label.setText("search finished, " + hidden_match_count + " hidden");
		}
		else
		    status_label.setText("search finished, " + current_match_count + " found");
		
		current_match_count = 0;
		hidden_match_count = 0;

		//System.out.println("back to the start....");
		return;
	    }
	    
	}
    }

    // special version for clusters as the searching method is completely different
    //
    // work through each cluster using a ClusterIterator until we find a matching
    // name, then visit each of the elements of that cluster, then resume searching
    // through the clusters looking for another name match
    //
    private ExprData.Cluster current_cluster = null;
    private ExprData.Cluster current_find_start_cluster = null;
    private ExprData.ClusterIterator current_clit = null;
    private int current_cluster_el_id = 0;  // which element of the current cluster?

    private void doFindCluster(final String s, final boolean exact, final boolean case_sens, final TimedLabel status_label)
    {
	if((s == null) || (s.length() == 0) || (edata.getNumClusters() == 0))
	{
	    status_label.setText("ready...");
	    return;
	}
	
	boolean apply_filter = dplot.getUseFilter();

	// are we still searching for the same thing?
	// are we still searching for the same thing?
	if(!s.equals(current_string) || (current_case != case_sens) || (current_exact != exact))
	{
	    // no, reset all of the counters and begin a new search from the current position

	    current_string = s;
	    current_match_count = 0;
	    hidden_match_count = 0;
	    current_case = case_sens;
	    current_exact = exact;

	    current_find_start_cluster = current_cluster;
	}

	while(true)
	{
	    String name = null;

	    if(current_cluster == null)
	    {
		if(current_clit == null)
		{
		    current_clit = edata.new ClusterIterator();
		}
		current_cluster = current_clit.reset();

		current_cluster_el_id = 0;

		// and store this as the start point of the search
		current_find_start_cluster = current_cluster;
		
		//System.out.println("starting...");
	    }

	    if(current_cluster != null)
	    {
		name = current_cluster.getName();
	    }
	    else
	    {
		// there aren't any clusters....
		status_label.setText("no clusters...");
		return;
	    }

	    //System.out.println("searching " + name);

	    if(findMatches(s, name, exact, case_sens))
	    {
		// the name matches, visit each of the elements (if any) in turn
		//

		//System.out.println("matched... current el is" + current_cluster_el_id);


		// what to do if the matching cluster has no elements? (i.e. there
		// are now rows which are actuallly 'in' the cluster directly. 
		// Check whether this cluster has any descendant which contains
		// a spot and, if so, display that spot.

		while(current_cluster.getSize() == 0)
		{
		    // the name matches, but this cluster has no elements
		    // of it's own, go to the first child of this cluster...
		    if(current_cluster.getNumChildren() > 0)
		    {
			//System.out.println("no elements in " + name + " , descending to first child...");
			
			ExprData.Cluster first_child = (ExprData.Cluster) current_cluster.getChildren().elementAt(0);
			
			current_cluster = first_child;
			current_clit.positionAt(first_child);
			
			current_cluster_el_id = 0;
		    }
		    else
		    {
			// cannot be displayed, has no elements and none of it's
			// children have any elements...
			status_label.setText("found, but has no elements...");
			return;
		    }
		}

		if(current_cluster_el_id < current_cluster.getSize())
		{
		    int spot_id = (current_cluster.getElements())[current_cluster_el_id];

		    //System.out.println("    spot id is " + spot_id);

		    // move to next element ready for the next find
		    //
		    if(++current_cluster_el_id >= current_cluster.getSize())
		    {
			// run out of elements, move to next cluster
			//System.out.println("moving to next cluster");
			
			current_cluster_el_id = 0;
			
			current_cluster = current_clit.getNext();
			
			if(current_cluster == null)
			{
			    //System.out.println("resetting to top of tree");
			    
			    /// run out of clusters, start again at the root
			    current_cluster = current_clit.reset();
			}
		    }

		    // and (possibly) display the result of this match
		    //
		    if(/*!apply_filter ||*/ !edata.filter(spot_id))
		    {
			current_match_count++;
			status_label.setText("found... " + current_match_count);
			dplot.displaySpot(spot_id);
			return;
		    }
		    else
		    {
			hidden_match_count++;
			status_label.setText("found, hidden by filter(s)...");
			return;
		    }
		}
		else
		{
		    // run out of elements, move to next cluster
		    //System.out.println("moving to next cluster");
		    
		    current_cluster_el_id = 0;
		    
		    current_cluster = current_clit.getNext();
		    
		    if(current_cluster == null)
		    {
			//System.out.println("resetting to top of tree");
			
			/// run out of clusters, start again at the root
			current_cluster = current_clit.reset();
		    }
		}
	    }
	    else
	    {
		//System.out.println("no match, moving to next cluster");

		// this cluster name, doesn't match, move to next cluster
		current_cluster = current_clit.getNext();
		current_cluster_el_id = 0;

		if(current_cluster == null)
		{
		    //System.out.println("resetting to top of tree");

		    /// run out of clusters, start again at the root
		    current_cluster = current_clit.reset();
		}
	    }

	    if(current_cluster == current_find_start_cluster)
	    {
		//System.out.println("search completed");
		
		// we've got back to where we started from

		if(current_match_count == 0)
		{
		    if(hidden_match_count == 0)
			status_label.setText("search finished, not found");
		    else
			status_label.setText("search finished, " + hidden_match_count + " hidden");
		}
		else
		    status_label.setText("search finished, " + current_match_count + " found");
		
		current_match_count = 0;
		hidden_match_count = 0;
		current_cluster_el_id = 0;
		current_cluster = null;
		return;
	    }
	    
	}

    }

    private class TimedLabel extends JLabel
    {
	private Timer timer = null;
	private Vector hide_these = null;

	public TimedLabel()
	{
	    super("");
	}

	public void addComponentToHide( Component com )
	{
	    if(hide_these == null)
		hide_these = new Vector();

	    hide_these.addElement(com);
	}


	public void setText(String s)
	{
	    if(timer != null)
		timer.stop();

	    if(hide_these != null)
	    {
		for(int c=0; c < hide_these.size(); c++)
		{
		    ((Component) hide_these.elementAt(c)).setVisible(false);
		}
	    }

	    super.setText(s);

	    timer = new Timer(2000, new ActionListener() 
		{
		    public void actionPerformed(ActionEvent evt) 
		    {
			// return things to normal

			TimedLabel.super.setText("");

			if(hide_these != null)
			{
			    for(int c=0; c < hide_these.size(); c++)
			    {
				((Component) hide_these.elementAt(c)).setVisible(true);
			    }
			}
		    }
		});
	    timer.setRepeats(false);
	    timer.start();
	}
    }

    private String current_string = null;
    private boolean current_case = false;
    private boolean current_exact = false;

    private int    current_match_count = 0;
    private int    hidden_match_count = 0;
    private int    current_find_pos_i = 0;      // in current traversal order
    private int    current_find_pos = 0;        // actual spot numbers
    private int    current_find_start_pos = 0;  // actual spot numbers
    
    private NameTagSelector nts;

    private maxdView mview;
    private ExprData edata;
    private DataPlot dplot;

}
