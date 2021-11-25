import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.io.*;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreeSelectionModel;

/*

   task:  given 2 clusters, each for example k-means with k=6, measure
          how similar the 2 clusters are to one another


  strategy:


    possible comparisons:
      
      cluster A is _like_ cluster B if it contains mostly the same
      elements and A's children are _like_ B's children


    slacker definition:

      cluster A is like cluster B if A and its children are mostly the
      same as cluster B and its children.

      
    things:

      don't want to make comparison too sensitive to structural differences
      in the trees - should do the comparing at each branch of the tree?

                 --D                            --C               
           --B--|         is the same as    A--|      --E
       A--|      --E   			        --B--|   
           --C         			              --D  
                                            


       and similar to 


           --C      
       A--|      --E    
           --B--|               
                |   --F                
		 --|
                    --G

      (assuming F and G are 'like' D)



   method 1:
   
      ** assume each spot occurs exactly once in each cluster
   
      compare A to B :=

         for each cluster B
	   find smallest set of clusters in A which have the same spots as B



   method 2

     what can be done using a reverse mapping from spot->cluster ?

            sp         c1          c2
            0         a d e       p u w
	    1         a d e       q u w
	    2         b d e       r u w
	    3         b d e       r v w
	    4         c   e       s v w 
	    5         c   e       t v w



   method 3

     foreach cluster A' in A

       find N clusters in B which make up A'
       or
       find 1 cluster in B which contains A'

        - maybe these operations can be made easier by doing 'A compare B' then 'B compare A' 
          and only implementing the second test? 
       
       
	- how about?

	     foreach cluster A' in A
	       find smallest cluster B' in B where A' is a subset of B'
	         insert A' -> B' to link map

	     foreach unique key B' in link map
	     ...?
	       

   method 4

     separate 'coverage' calculation....
     
     the 'coverage' records how well clusters of B are 'covered'
     (i.e. matched with) by clusters in A.  and, more importantly,
     which clusters of B are not covered at all.

 */
	    
public class CompareClusters implements Plugin, ExprData.ExprDataObserver
{
    final boolean debug = false;
    
    public CompareClusters(maxdView mview_)
    {
	mview = mview_;
	edata = mview.getExprData();
    }

    public void cleanUp()
    {
	edata.removeObserver(this);

	if(compare_thread != null)
	{
	    compare_thread.please_die = true;
	}

	//edata.removeObserver(this);
	if(frame != null)
	{
	    frame.setVisible(false);

	    //mview.putBooleanProperty("CompareClusters.detect_substr", substr_jchkb.isSelected());
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
	mview.getExprData().addObserver(this);
    }

    public void stopPlugin()
    {
	cleanUp();
    }
  
    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("Compare Clusters", "viewer", 
					 "Find commonalities between clusters", "",
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
    //
    public void dataUpdate(ExprData.DataUpdateEvent due)
    {
    }

    public void clusterUpdate(ExprData.ClusterUpdateEvent cue)
    {
	switch(cue.event)
	{
	case ExprData.ColourChanged:
	case ExprData.VisibilityChanged:
	    break;
	case ExprData.OrderChanged:
	case ExprData.SizeChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	    if(cl1_tree != null)
	    {
		populateTreeWithClusters(cl1_tree, edata.getRootCluster() );
		populateTreeWithClusters(cl2_tree, edata.getRootCluster() );
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
    // --- --- ---  gooey
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    JTable common_table = null;

    Vector clusters = new Vector();
    JList cluster_list = null;

    JTabbedPane tabbed = null;
    JButton next_but = null;
    JTextArea results_jta = null;
    JTextField min_pc_jtf = null;
    JTextField delim_jtf = null;
    // JComboBox words_jcb = null;

    private void addComponents()
    {
	frame = new JFrame("Compare Clusters");
	
	mview.decorateFrame(frame);

	frame.addWindowListener(new WindowAdapter() 
			  {
			      public void windowClosing(WindowEvent e)
			      {
				  cleanUp();
			      }
			  });

	JPanel panel = new JPanel();
	panel.setPreferredSize(new Dimension(600, 400));
	GridBagLayout gridbag = new GridBagLayout();
	panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	panel.setLayout(gridbag);
	GridBagConstraints c = null;

	int line = 0;

	JSplitPane jsplt_pane, jsplt_pane2;
	
	tabbed = new JTabbedPane();

	// ------------------------------------------------------------------
	// == pick cluster ==================================================
	// ------------------------------------------------------------------

	{
	    JPanel pick_cluster = new JPanel();
	    GridBagLayout p_gridbag = new GridBagLayout();
	    pick_cluster.setLayout(p_gridbag);
	    
	    
	    cl1_tree = new DragAndDropTree();
	    populateTreeWithClusters(cl1_tree, edata.getRootCluster() );
	    addTreeDragAndDropActions(cl1_tree);

	    JScrollPane jsp = new JScrollPane(cl1_tree);

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = 5.0;
	    c.weighty = 10.0;
	    c.fill = GridBagConstraints.BOTH;
	    p_gridbag.setConstraints(jsp, c);
	    pick_cluster.add(jsp);

	    cl2_tree = new DragAndDropTree();
	    populateTreeWithClusters(cl2_tree, edata.getRootCluster() );
	    addTreeDragAndDropActions(cl2_tree);

	    jsp = new JScrollPane(cl2_tree);

	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    c.weightx = 5.0;
	    c.weighty = 10.0;
	    c.fill = GridBagConstraints.BOTH;
	    p_gridbag.setConstraints(jsp, c);
	    pick_cluster.add(jsp);


	    tabbed.add(" Pick clusters ", pick_cluster);
	}

	// ------------------------------------------------------------------
	// == select rules ==================================================
	// ------------------------------------------------------------------

	{
	    JPanel select_rules = new JPanel();
	    
	    int sline = 0;

	    GridBagLayout r_gridbag = new GridBagLayout();
	    select_rules.setLayout(r_gridbag);

	    decompose_jcb = new JCheckBox("Decompose imperfect matches");
	    decompose_jcb.setSelected(true);
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.WEST;
	    r_gridbag.setConstraints(decompose_jcb, c);
	    select_rules.add(decompose_jcb);

	    coverage_jcb = new JCheckBox("Calculate total coverage");
	    coverage_jcb.setSelected(true);
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.anchor = GridBagConstraints.WEST;
	    r_gridbag.setConstraints( coverage_jcb, c);
	    select_rules.add( coverage_jcb);

	    tabbed.add(" Set options ", select_rules);
	}

	// ------------------------------------------------------------------
	// == view results ==================================================
	// ------------------------------------------------------------------

	{
	    JPanel view_results = new JPanel();
	    GridBagLayout v_gridbag = new GridBagLayout();
	    view_results.setLayout(v_gridbag);

	    // result_tree 

	    result_tree = new DragAndDropTree();
	    JScrollPane jsp1 = new JScrollPane(result_tree);

	    result_tree.setDragAction(new DragAndDropTree.DragAction()
		{
		    public DragAndDropEntity getEntity()
		    {
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) result_tree.getLastSelectedPathComponent();
			
			if(node != null)
			{
			    MatchRecord mr = (MatchRecord) node.getUserObject();
			    
			    DragAndDropEntity dnde = DragAndDropEntity.createClusterEntity(mr.c1.src);
			    
			    return dnde;
			}
			else
			    return null;
		    }
		});
	    
	    result_tree.setDropAction( new DragAndDropTree.DropAction()
		{
		    public void dropped(DragAndDropEntity dnde)
		    {
			try
			{
			    ExprData.Cluster cl = dnde.getCluster();
			    
			    DefaultTreeModel model      = (DefaultTreeModel)       result_tree.getModel();
			    DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
			    
			    for (Enumeration e =  root.depthFirstEnumeration(); e.hasMoreElements() ;) 
			    {
				DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) e.nextElement();
				MatchRecord mr = (MatchRecord) dmtn.getUserObject();

				// ****************
				// TODO:::: needs to handle MatchRecords with multiple FlatClusters (i.e. not using c2)
				// ****************
				
				if((cl == mr.c1.src) || ((mr.c2 != null) && (mr.c2.src == cl)))
				{
				    TreeNode[] tn_path = model.getPathToRoot(dmtn);
				    TreePath tp = new TreePath(tn_path);
				    
				    result_tree.expandPath(tp);
				    result_tree.scrollPathToVisible(tp);
				    result_tree.setSelectionPath(tp);
				    return;
				}
			    }
			}
			catch(DragAndDropEntity.WrongEntityException wee)
			{
			}
		    }
		});
	    // result_table

	    result_table = new DragAndDropTable();
	    result_table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	    result_table.setColumnSelectionAllowed(false); 

	    MouseAdapter lml = new MouseAdapter() 
		{
		    public void mouseClicked(MouseEvent e) 
		    {
			TableColumnModel tcm = result_table.getColumnModel();
			int vc = tcm.getColumnIndexAtX(e.getX()); 
			int column = result_table.convertColumnIndexToModel(vc); 
			if (e.getClickCount() == 1 && column != -1) 
			{
			    //System.out.println("Sorting " + column + "..."); 
			    
			    sortMatchRecords( column );
			}
		    }
		};
	    JTableHeader th = result_table.getTableHeader(); 
	    th.addMouseListener(lml); 
	    
	    lml = new MouseAdapter() 
		{
		    public void mouseClicked(MouseEvent e) 
		    {
			if (e.getClickCount() == 2) 
			{
			    int row = result_table.getSelectedRow();
			    int col = result_table.getSelectedColumn();
			    
			    System.out.println("viewing match record " + row);
			    displayMatchRecord(row);
			}
		    }
		};
	    result_table.addMouseListener(lml);

	    result_table.setDropAction( new DragAndDropTable.DropAction() 
		{
		    public void dropped(DragAndDropEntity dnde)
		    {
			try
			{
			    ExprData.Cluster cl = dnde.getCluster();
			    
			    int[] matches = findClusterInMatchRecords( cl );
			    
			    if(matches != null)
			    {
				DefaultListSelectionModel dlsm = new DefaultListSelectionModel();
				for(int m=0; m < matches.length; m++)
				    dlsm.addSelectionInterval(matches[m], matches[m]);
				result_table.setSelectionModel( dlsm );

				if(matches.length > 0)
				{
				    Rectangle r = result_table.getCellRect(result_table.getSelectedRow(), 0, true);
				    result_table.scrollRectToVisible(r);
				}
			    }
			}
			catch(DragAndDropEntity.WrongEntityException wee)
			{
			}
		    }
		});
	    result_table.setDragAction( new DragAndDropTable.DragAction() 
		{
		    public DragAndDropEntity getEntity()
		    {
			System.out.println("drag from " + result_table.getSelectedRow() + "," + 
					   result_table.getSelectedColumn());
			
			int row = result_table.getSelectedRow();
			int col = result_table.getSelectedColumn();
			MatchRecord[] mr_a = ((ResultTableModel)result_table.getModel()).getData();
			ExprData.Cluster cl = null;
			if(col == 0)
			{
			    cl = mr_a[row].c1.src;
			}
			if(col == 3)
			{
			    FlatCluster fc = mr_a[row].getFirstMatch();
			    if(fc != null)
				cl = fc.src;
			}
			
			return cl == null ? null : DragAndDropEntity.createClusterEntity(cl);
		    }
		});

	    JScrollPane jsp2 = new JScrollPane(result_table);


	    coverage_table = new  DragAndDropTable();

	    lml = new MouseAdapter() 
		{
		    public void mouseClicked(MouseEvent e) 
		    {
			TableColumnModel tcm = coverage_table.getColumnModel();
			int vc = tcm.getColumnIndexAtX(e.getX()); 
			int column = result_table.convertColumnIndexToModel(vc); 
			if (e.getClickCount() == 1 && column != -1) 
			{
			    //System.out.println("Sorting " + column + "..."); 
			    
			    sortCoverageRecords( column );
			}
		    }
		};
	    th = coverage_table.getTableHeader(); 
	    th.addMouseListener(lml); 
	
	    coverage_table.setDragAction( new DragAndDropTable.DragAction() 
		{
		    public DragAndDropEntity getEntity()
		    {
			System.out.println("drag from coverage: " + 
					   coverage_table.getSelectedRow() + "," + 
					   coverage_table.getSelectedColumn());
			
			int row =  coverage_table.getSelectedRow();
			int col =  coverage_table.getSelectedColumn();
			CoverageRecord[] cr_a = ((CoverageTableModel)coverage_table.getModel()).getData();
			ExprData.Cluster cl = null;
			if(col == 0)
			{
			    // the 'match target' column
			    cl = cr_a[row].src;
			}
			if(col == 2)
			{
			    // the 'covered_by' column
			    cl = cr_a[row].covered_by;
			}
			
			return cl == null ? null : DragAndDropEntity.createClusterEntity(cl);
		    }
		});
	    coverage_table.setDropAction( new DragAndDropTable.DropAction() 
		{
		    public void dropped(DragAndDropEntity dnde)
		    {
			try
			{
			    ExprData.Cluster cl = dnde.getCluster();
			    
			    int[] matches = findClusterInCoverageRecords( cl );
			    
			    if(matches != null)
			    {
				DefaultListSelectionModel dlsm = new DefaultListSelectionModel();
				for(int m=0; m < matches.length; m++)
				    dlsm.addSelectionInterval(matches[m], matches[m]);
				coverage_table.setSelectionModel( dlsm );

				if(matches.length > 0)
				{
				    Rectangle r =  coverage_table.getCellRect( coverage_table.getSelectedRow(), 0, true);
				    coverage_table.scrollRectToVisible(r);
				}
			    }

			}
			catch(DragAndDropEntity.WrongEntityException wee)
			{
			}
		    }
		});
	    JScrollPane jsp3 = new JScrollPane(coverage_table);

	    // -----------------------------

	    jsplt_pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	    jsplt_pane.setLeftComponent(jsp1);
	    jsplt_pane.setRightComponent(jsp2);
	    jsplt_pane.setOneTouchExpandable(true);

	    jsplt_pane2 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	    jsplt_pane2.setLeftComponent(jsplt_pane);
	    jsplt_pane2.setRightComponent(jsp3);
	    jsplt_pane2.setOneTouchExpandable(true);


	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = 10.0;
	    c.weighty = 10.0;
	    c.fill = GridBagConstraints.BOTH;
	    v_gridbag.setConstraints(jsplt_pane2, c);
	    view_results.add(jsplt_pane2);

	    	    

	    tabbed.add(" View results ", view_results);
	}


	// ------------------------------------------------------------------
	// == add the tabbed pane ===========================================
	// ------------------------------------------------------------------

	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
        c.weightx = 1.0;
	c.weighty = 8.0;
	c.fill = GridBagConstraints.BOTH;
	gridbag.setConstraints(tabbed, c);
	panel.add(tabbed);

	// ------------------------------------------------------------------
	// == buttons =======================================================
	// ------------------------------------------------------------------

	{
	    // 
	    // the Help & Cancel buttons
	    //

	    JPanel wrapper = new JPanel();
	    wrapper.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrapper.setLayout(w_gridbag);

	    {
		final JButton bbutton = new JButton("Back");
		final JButton nbutton = new JButton("Next");
		
		bbutton.setEnabled(false);
		bbutton.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    int tab_i = tabbed.getSelectedIndex();
			    if(tab_i > 0)
			    {
				tab_i--;
				tabbed.setSelectedIndex(tab_i);

				nbutton.setEnabled(true);

				match_button.setVisible(tab_i == 2);
				export_button.setVisible(tab_i == 2);
				status_label.setVisible(tab_i < 2);

				if(tab_i == 0)
				{
				    bbutton.setEnabled(false);
				}
			    }
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.anchor = GridBagConstraints.WEST;
		w_gridbag.setConstraints(bbutton, c);
		wrapper.add(bbutton);
		
		nbutton.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    int tab_i = tabbed.getSelectedIndex();
			    
			    /*
			      // debug: deliberately cause a null-ptr-exception...
			      DefaultMutableTreeNode dummy = null;
			      System.out.println(dummy.toString());
			    */
			    /*
			      // debug: deliberately cause an array out-of-bounds-exception...
			      int i = 101;
			      double[] dummy = new double[i-40];
			      dummy[i+40] = -1.0;
			    */
				    
			    if(tab_i == 0)
			    {
				DefaultMutableTreeNode n1 = (DefaultMutableTreeNode) cl1_tree.getLastSelectedPathComponent();
				DefaultMutableTreeNode n2 = (DefaultMutableTreeNode) cl2_tree.getLastSelectedPathComponent();
				
				if((n1 == null) || (n2 == null))
				{
				    mview.alertMessage("You must select two clusters for comparison");
				    return;
				}

			    }

			    if(tab_i < 3)
			    {
				tab_i++;
				tabbed.setSelectedIndex(tab_i);
				
				bbutton.setEnabled(true);

				if(tab_i == 2)
				{
				    doCompare();
				    nbutton.setEnabled(false);
				}
			    }
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 1;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		w_gridbag.setConstraints(nbutton, c);
		wrapper.add(nbutton);
	    }
	    {
		Dimension fillsize = new Dimension(16,16);
		Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
		c = new GridBagConstraints();
		c.gridx = 2;
		w_gridbag.setConstraints(filler, c);
		wrapper.add(filler);
						   
	    }

	    {
		status_label = new JLabel("Ready...");
		status_label.setHorizontalAlignment(SwingConstants.CENTER);
		c = new GridBagConstraints();
		c.gridx = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 2;
		c.weightx = 7.0;
		
		w_gridbag.setConstraints(status_label, c);
		wrapper.add(status_label);
	    }

	    {
		match_button = new JButton("Highlight Matches...");
		match_button.setVisible(false);
		match_button.setFont(mview.getSmallFont());
		match_button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    showMatched();
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 3;
		w_gridbag.setConstraints(match_button, c);
		wrapper.add(match_button);
	    }
	    {
		export_button = new JButton("Export...");
		export_button.setFont(mview.getSmallFont());
		export_button.setVisible(false);
		export_button.setEnabled(false);
		export_button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 4;
		w_gridbag.setConstraints(export_button, c);
		wrapper.add(export_button);
	    }

	    {
		Dimension fillsize = new Dimension(16,16);
		Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
		c = new GridBagConstraints();
		c.gridx = 5;
		w_gridbag.setConstraints(filler, c);
		wrapper.add(filler);
						   
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
		
		c = new GridBagConstraints();
		c.gridx = 6;
		c.anchor = GridBagConstraints.EAST;
		c.weightx = 1.0;
		w_gridbag.setConstraints(button, c);
	    }
	    {
		JButton button = new JButton("Help");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    mview.getPluginHelpTopic("CompareClusters", "CompareClusters");
			}
		    });
		
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 7;
		w_gridbag.setConstraints(button, c);
	    }

	    c = new GridBagConstraints();
	    c.gridy = line++;
	    //c.gridwidth = 2;
	    //c.weighty = 2.0;
	    c.weightx = 10.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    //c.anchor = GridBagConstraints.SOUTH;
	    gridbag.setConstraints(wrapper, c);
	    panel.add(wrapper);
	}

	// ------------------------------------------------------------------
	// ==================================================================
	// ------------------------------------------------------------------

	tabbed.setEnabledAt(0, false);
	tabbed.setEnabledAt(1, false);
	tabbed.setEnabledAt(2, false);

	frame.getContentPane().add(panel);

	frame.pack();

	frame.addKeyListener(new CustomKeyListener());

	jsplt_pane.setDividerLocation(0.5);
	jsplt_pane2.setDividerLocation(0.66);
	    
	frame.setVisible(true);

    }

    public void reportBusy(boolean is_busy)
    {
	frame.setCursor(Cursor.getPredefinedCursor(is_busy ?  Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
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

	DefaultMutableTreeNode dmtn = generateTreeNodes( null, cluster );
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
	}
    }

    private DefaultMutableTreeNode generateTreeNodes(DefaultMutableTreeNode parent, ExprData.Cluster clust)
    {
	if(clust.getIsSpot())
	{
	    DefaultMutableTreeNode node = new DefaultMutableTreeNode( clust );
	    
	    Vector ch = clust.getChildren();
	    if(ch != null)
	    {
		for(int c=0; c < ch.size(); c++)
		    generateTreeNodes( node, ( ExprData.Cluster) ch.elementAt(c) );
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
	return null;
    }

    private void addTreeDragAndDropActions(final DragAndDropTree tree)
    {
	tree.setDropAction( new DragAndDropTree.DropAction()
	    {
		public void dropped(DragAndDropEntity dnde)
		{
		    try
		    {
			ExprData.Cluster cl = dnde.getCluster();
			
			DefaultTreeModel model      = (DefaultTreeModel)       tree.getModel();
			DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
			
			for (Enumeration e =  root.depthFirstEnumeration(); e.hasMoreElements() ;) 
			{
				DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) e.nextElement();
				ExprData.Cluster ncl = (ExprData.Cluster) dmtn.getUserObject();
				if(cl == ncl)
				{
				    TreeNode[] tn_path = model.getPathToRoot(dmtn);
				    TreePath tp = new TreePath(tn_path);
				    
				    tree.expandPath(tp);
				    tree.scrollPathToVisible(tp);
				    tree.setSelectionPath(tp);
				    return;
				}
			}
		    }
		    catch(DragAndDropEntity.WrongEntityException wee)
		    {
		    }
		}
	    });
	
	tree.setDragAction(new DragAndDropTree.DragAction()
	    {
		public DragAndDropEntity getEntity()
		{
		    DefaultMutableTreeNode node = (DefaultMutableTreeNode) cl1_tree.getLastSelectedPathComponent();
		    
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
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ===== getAllClusterElements  ===========================================
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    // returns the spot_id's of the cluster and it's children
    private int[] getAllClusterElements(ExprData.Cluster cl)
    {
	Vector s_ids_v = new Vector();
	
	addAllClusterElements(s_ids_v, cl);
	
	int vc = 0;
	for(int v=0; v < s_ids_v.size(); v++)
	    vc += ((int[]) s_ids_v.elementAt(v)).length;
	
	int p = 0;
	int[] s_ids_a = new int[vc];
	
	for(int v=0; v < s_ids_v.size(); v++)
	{
	    int[] va = (int[]) s_ids_v.elementAt(v);
	    for(int v2=0; v2 < va.length; v2++)
		s_ids_a[p++] = va[v2];
	}
	
	return  s_ids_a;
    }
    // adds to 's_ids_v' the elements int[] of the cluster 'cl' and all it's children
    private void addAllClusterElements(Vector s_ids_v, ExprData.Cluster cl)
    {
	int[] c_s_ids = cl.getElements();
	
	if(c_s_ids != null)
	    s_ids_v.addElement(c_s_ids);
	
	for(int c=0; c < cl.getNumChildren(); c++)
	{
	    ExprData.Cluster ch = (ExprData.Cluster)(cl.getChildren().elementAt(c));
	    addAllClusterElements(s_ids_v, ch);
	}
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ===== the actual comparison algorithms  ================================
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private CompareThread compare_thread;

    private void doCompare()
    {
	System.gc(); // likely to need all available store!

	compare_thread = new CompareThread();
	compare_thread.please_die = false;
	compare_thread.start();
    }
    
    public class CompareThread extends Thread
    {
	public boolean please_die;

	public void run()
	{
	    reportBusy(true);
	    
	    DefaultMutableTreeNode node1 = (DefaultMutableTreeNode) cl1_tree.getLastSelectedPathComponent();
	    DefaultMutableTreeNode node2 = (DefaultMutableTreeNode) cl2_tree.getLastSelectedPathComponent();
	    
	    if( (node1 != null)  || (node2 != null) )
	    {
		ExprData.Cluster cluster1 = (ExprData.Cluster) node1.getUserObject();
		ExprData.Cluster cluster2 = (ExprData.Cluster) node2.getUserObject();
		
		// System.out.println("comparing " + cluster1.getName() + " and " + cluster2.getName());
		
		compareCluster( cluster1, cluster2 );
		
	    }
	    
	    match_button.setVisible(true);
	    export_button.setVisible(true);
	    status_label.setVisible(false);
				
	    reportBusy(false);

	    compare_thread = null;
	}
    }
    
    
    int biggest_flat_cluster_size;
    Hashtable uncovered_ht = null;

    private void compareCluster(ExprData.Cluster c1, ExprData.Cluster c2)
    {
	FlatCluster fc = new FlatCluster(c1, 0);

	Vector fcv = makeFlatClusters( c2 );

	// Hashtable result = new Hashtable();

	match_record_v = new Vector();      // will be filled in findSupersetLinks()
	coverage_record_v = new Vector();   // likewise

	result_tree.setModel( null );
	result_table.setModel( new DefaultTableModel() );
	coverage_table.setModel( new DefaultTableModel() );

	biggest_flat_cluster_size = 0;

	// 
	if(coverage_jcb.isSelected())
	{
	    uncovered_ht = new Hashtable();
	    populateHashTableWithClusters( uncovered_ht, c2 );

	    System.out.println("there are " +  uncovered_ht.size() + " clusters to cover");
	}

	DefaultMutableTreeNode dmtn = findSupersetLinks( null,  fc, fcv );

	System.out.println("biggest flat cluster was " + biggest_flat_cluster_size);

	// find any clusters in c2 which are not covered....

	if(coverage_jcb.isSelected())
	{
	    addUncovered( uncovered_ht, fcv, coverage_record_v );
	}

	// update the tree

	result_tree.setModel( new DefaultTreeModel( dmtn ));
	result_tree.putClientProperty("JTree.lineStyle", "Angled");

	result_tree.setCellRenderer(new CustomTreeCellRenderer());

	// sort the contents of match_record_v

	MatchRecord[] mr_a = (MatchRecord[]) match_record_v.toArray(new MatchRecord[0]);
	java.util.Arrays.sort(mr_a, new MatchRecordSizeComparator(1, false));

	// print out the results...

	System.out.println(mr_a.length + " MatchRecords");
	
	result_table.setModel( new ResultTableModel( mr_a ) );

	if(coverage_jcb.isSelected())
	{
	    // and optionally the coverage
	    
	    CoverageRecord[] cr_a = (CoverageRecord[]) coverage_record_v.toArray(new CoverageRecord[0]);
	    // java.util.Arrays.sort(mr_a, new CoverageRecordComparator(1, false));
	    coverage_table.setModel( new CoverageTableModel( cr_a ) );
	}
	else
	{
	    coverage_table.setModel( new CoverageTableModel( null ) );
	}

	// dumpMatches(c1, result, "");

	/*
	for (Enumeration e = result.keys(); e.hasMoreElements() ;) 
	{
	    FlatCluster key = (FlatCluster) e.nextElement();
	    FlatCluster val = (FlatCluster) result.get(key);
	    
	    System.out.println(key.src.getName() + "(" + key.size() + ")" + " -> " + 
			       val.src.getName() + "(" + val.size() + ")");
	}
	*/

    }

    private void dumpMatches(ExprData.Cluster cl, Hashtable result, String pad)
    {
	FlatCluster match = (FlatCluster) result.get( new FlatCluster(cl, 0) );
	if(match != null)
	{
	    
	}
	else
	{
	    System.out.println(pad + cl.getName() + " NO MATCH");
	}

	System.out.println(pad + cl.getName() + " -> " + match);
	Vector ch = cl.getChildren();
	if(ch != null)
	    for(int c=0; c < ch.size(); c++)
	    {
		dumpMatches((ExprData.Cluster) ch.elementAt(c), result, pad + " ");
	    }
    }

    // ------------------------------------

    private class MatchRecord
    {
	public String toString()
	{
	    return name;
	}
	
	public MatchRecord(String n, int s,  FlatCluster c1_, FlatCluster c2_)
	{
	    name  = n;
	    score = s;
	    c1    = c1_;
	    c2    = c2_;
	    width = 0;
	    fcv   = null;
	}

	public MatchRecord(String n, int s,  FlatCluster c1_, Vector v_)
	{
	    name  = n;
	    score = s;
	    c1    = c1_;
	    c2    = null;
	    fcv   = v_;    // Vector of FlatClusters
	    
	    final int nc = fcv.size();
	    width = 0;
	    for(int i=0; i < nc; i++)
	    {
		final ExprData.Cluster cl = ((FlatCluster) fcv.elementAt(i)).src;
		for(int j=i; j < nc; j++)
		{
		    int dist = findShortestPathDistanceBetween( cl, ((FlatCluster) fcv.elementAt(j)).src);
		    if(dist > width)
			width = dist;
		}		
	    }
	}

	public int getNumMatches()
	{
	    if(fcv != null)
	    {
		return fcv.size();
	    }
	    else
	    {
		return (c2 == null) ? 0 : 1;
	    }
	}

	public FlatCluster getFirstMatch()
	{
	    if(fcv != null)
	    {
		return (fcv.size() > 0) ? (FlatCluster) fcv.elementAt(0) : null;
	    }
	    else
	    {
		return c2;
	    }
	}

	public String getMatchNames()
	{
	    if(fcv != null)
	    {
		String name = "";
		for(int m=0; m < fcv.size(); m++)
		    name += ((FlatCluster)fcv.elementAt(m)).src.getName() + " ";
		return name;
	    }
	    else
	    {
		return c2 == null ? null : c2.src.getName();
	    }
	}

	public int getMatchSize()
	{
	    if(fcv != null)
	    {
		int len = 0;
		for(int m=0; m < fcv.size(); m++)
		    len += ((FlatCluster)fcv.elementAt(m)).sorted_spot_ids.length;
		return len;
	    }
	    else
	    {
		return c2 == null ? 0 : c2.sorted_spot_ids.length;
	    }
	}

	// finds the shortest path between two Clusters in the same tree
	// or -1 if there is no path
	private int findShortestPathDistanceBetween(ExprData.Cluster cl1, ExprData.Cluster cl2)
	{
	    ExprData.Cluster[] pa1 = getParents(cl1);
	    ExprData.Cluster[] pa2 = getParents(cl2);
	    
	    for(int p2i=0; p2i < pa2.length; p2i++)
	    {
		for(int p1i=0; p1i < pa1.length; p1i++)
		{
		    if(pa1[p1i] == pa2[p2i])
			return p1i + p2i;
		}
	    }
	    return -1; 
	}

	private ExprData.Cluster[] getParents(ExprData.Cluster cl)
	{
	    Vector v = new Vector();
	    ExprData.Cluster parent = cl.getParent();
	    while(parent != null)
	    {
		v.addElement(parent);
		parent = parent.getParent();
	    }
	    return (ExprData.Cluster[]) v.toArray(new ExprData.Cluster[0]);
	}

	// ===== state =========================================================================

	public String name;
	public int score;
	public int width;   // the 'width' of either c2 or the contents of fcv
	public FlatCluster c1, c2;
	public Vector fcv;
    }

    private class MatchRecordSizeComparator implements java.util.Comparator
    {
	public MatchRecordSizeComparator(int mode_, boolean ascend_)
	{
	    mode = mode_;
	    ascend = ascend_;
	}
	public int compare(Object o1, Object o2)
	{
	    MatchRecord m1 = (MatchRecord) o1;
	    MatchRecord m2 = (MatchRecord) o2;

	    int res;

	    if(m2.getFirstMatch() == null)
	    {
		res = (m1.getFirstMatch() == null) ? 0 : -1;
	    }
	    else
	    {
		if(m1.getFirstMatch() == null)
		{
		    res = 1;  // m2 can't be null
		}
		else
		{
		    switch(mode)  // mode is the same as cols in the table
		    {
		    case 0:
			res = (m2.c1.src.getName().compareTo(m1.c1.src.getName()));
			break;
		    case 1:
			res = (m2.c1.sorted_spot_ids.length - m1.c1.sorted_spot_ids.length);
			break;
		    case 2:
			res = (m2.c1.depth - m1.c1.depth);
			break;
		    case 3:
			res = (m2.getMatchNames().compareTo(m1.getFirstMatch().src.getName()));
			break;
		    case 4:
			res = (m2.width - m1.width);
			break;
		    case 5:
			res = (m2.getMatchSize() - m1.getMatchSize());
			break;
		    case 6:
			res = (m2.getFirstMatch().depth - m1.getFirstMatch().depth);
			break;
		    default:
			res = (m2.score - m1.score);
			break;
		    }
		}
	    }
	    return ascend ? -res : res;
	}
	
	public boolean equals(Object o) { return false; }

	private boolean ascend;
	private int mode;
    }


    private final DefaultMutableTreeNode findSupersetLinks(final DefaultMutableTreeNode parent, 
							   final FlatCluster fc, 
							   final Vector fcv)
    {
	String result;    
	int score ;

	if(parent != null)
	    if(fc.sorted_spot_ids.length > biggest_flat_cluster_size)
		biggest_flat_cluster_size = fc.sorted_spot_ids.length;

	
	// ----------------------------------------------------------------------------
	// show progress in status label
	// ----------------------------------------------------------------------------

	status_label.setText(fc.src.getName());
	
	// ----------------------------------------------------------------------------
	// create a TreeNode for this entry 
	// ----------------------------------------------------------------------------

	FlatCluster ss = findSmallestSuperset( fc, fcv );
	MatchRecord mr;

	if(ss != null)
	{
	    result = fc.src.getName() + "(" + fc.size() + ")" + " -> ";
	             
	    score = (int) (((double) fc.size() / (double) ss.size()) * 360.0);  // score in degrees

	    if( (score < 100) && decompose_jcb.isSelected() )
	    {
		// under-fit, this result should be decomposed to make the fit better
		
		Vector v = new Vector();
		
		int subsize = findBestSubset(v, fc, ss ); // 'v' will be filled with zero or more FlatClusters
		
		result += v.size() + " children of " + ss.src.getName() + "(" + subsize + " of " + ss.size() + ")            "; 
		
		score = (int) (((double) fc.size() / (double) subsize) * 360.0);  // score in degrees
		
		// TODO:: should possibly store 'ss' in the MatchRecord for subset matches?
		mr = new MatchRecord(result, score, fc, v);

		for(int ssfci=0; ssfci < v.size(); ssfci++)
		{
		    FlatCluster ssfc = (FlatCluster) v.elementAt(ssfci);
		    coverage_record_v.addElement( new CoverageRecord( ssfc.src,
								      fc.src,
								      ssfc.sorted_spot_ids.length, 
								      countOverlap( fc, ssfc )));

		    // remove the matched cluster from the uncovered hashtable
		    if(coverage_jcb.isSelected())
			uncovered_ht.remove( ssfc.src.getName() );
		}
	    }
	    else
	    {
		// perfect match with other cluster(s)
		
		result += ss.src.getName() + "(" + ss.size() + ")            "; 
		mr = new MatchRecord(result, score, fc, ss);
		
		coverage_record_v.addElement( new CoverageRecord( ss.src, 
								  fc.src,
								  ss.sorted_spot_ids.length, 
								  fc.size()) );

		// remove the matched cluster from the uncovered hashtable
		if(coverage_jcb.isSelected())
		    uncovered_ht.remove( ss.src.getName() );
	    }

	}
	else
	{
	    // doesn't cover any other cluster....

	    score = -1;
	    result = fc.src.getName() + "(" + fc.size() + ")            ";
	    mr = new MatchRecord(result, score, fc, ss);
	}
	
	match_record_v.addElement(mr);

	DefaultMutableTreeNode node = new DefaultMutableTreeNode( mr );

	// ----------------------------------------------------------------------------
	// recurse to each of the children
	// ----------------------------------------------------------------------------

	Vector ch = fc.src.getChildren();
	if(ch != null)
	    for(int c=0; c < ch.size(); c++)
	    {
		findSupersetLinks( node, new FlatCluster((ExprData.Cluster) ch.elementAt(c), fc.depth+1), fcv );
	    }

	// ----------------------------------------------------------------------------
	// add the newly created node to the parent and return
	// ----------------------------------------------------------------------------
	
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

    // returns the smallest of the FlatClusters from 'fcv' which contains the input 'fc' 
    // or null if no FlatClusters from 'fcv' contain 'fc'
    //
    private final FlatCluster findSmallestSuperset( final FlatCluster fc, final Vector fcv)
    {
	final int fcvs = fcv.size();

	int smallest_size = Integer.MAX_VALUE;
	FlatCluster smallest_fc = null;

	for(int c=0; c < fcvs; c++)
	{
	    FlatCluster fci = (FlatCluster) fcv.elementAt(c);
	    if(fci.contains(fc))
	    {
		final int fcis = fci.size();
		if(fcis < smallest_size)
		{
		    smallest_fc = fci;
		    smallest_size = fcis;
		}
	    }
	}
	return smallest_fc;
    }

    // return the number of spots in fc2 that also occur in fc1
    //
    private final int countOverlap( FlatCluster fc1, FlatCluster fc2 )
    {
	int count = 0;
	final int[] s_ids = fc2.sorted_spot_ids;
	if(s_ids != null)
	{
	    for(int s=0; s < s_ids.length; s++)
		if(fc1.contains( s_ids[ s ] ))
		{
		    count++;
		}
	}
	return count;
    }

    // -------------------------------------------
    
    // find the smallest set of clusters in 'ss' that cover 'fc' completely
    // (pre: 'ss' must contain all of 'fc')
    //
    // returns the total size of the set
    //
    private final int findBestSubset(final Vector cover, final FlatCluster fc, final FlatCluster ss)
    {
	int size_of_cover = 0;
	cover.clear(); 

	addCoveringClusters(cover, fc, ss.src, fc.depth);

	if(debug)
	{
	    System.out.println( fc.src.getName() + " [" + fc.sorted_spot_ids.length + "] :");
	    System.out.println( "  naive match = " + ss.src.getName() + " [" + ss.sorted_spot_ids.length + "] :");
	}

	final int cs = cover.size();
	for(int c=0; c < cs; c++)
	{
	    final FlatCluster cfc = (FlatCluster) cover.elementAt(c);
	    //size_of_cover += cfc.src.getSize();
	    size_of_cover += cfc.sorted_spot_ids.length;
	    
	    if(debug)
		System.out.println( "  " + cfc.src.getName() + " (" + cfc.src.getSize() + ")");
	}

	// FlatCluster fc = new FlatCluster( cover );

	return size_of_cover;
    }

    private final void addCoveringClusters(final Vector v, final FlatCluster fc, final ExprData.Cluster cl, final int depth)
    {
	// if 'cl' contains any of 'fc's spots then add it to 'v'
	final int[] s_ids = cl.getElements();
	if(s_ids != null)
	{
	    final int s_ids_l = s_ids.length;
	    for(int s=0; s <  s_ids_l; s++)
		if(fc.contains( s_ids[ s ] ))
		{
		    //v.addElement( new FlatCluster(cl, depth, false) );
		    
		    // should include all descendants in the size count....
		    // (maybe should be be an option?)
		    
		    v.addElement( new FlatCluster(cl, depth, true) );
		    break;
		}
	}

	// and likewise for 'c2's children
	Vector ch = cl.getChildren();
	final int nc = cl.getNumChildren();
	if(ch != null)
	    for(int c=0; c < nc; c++)
		addCoveringClusters( v, fc, (ExprData.Cluster) ch.elementAt(c), depth+1 );
    }

    // ----------------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------------
    //
    // ResultTableModel
    //
    // ----------------------------------------------------------------------------------------------
    //
    // displays the MatchRecord statistics
    //
    // ----------------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------------

    private class ResultTableModel extends javax.swing.table.AbstractTableModel
    {
	public ResultTableModel(MatchRecord[] mr_a_)
	{
	    mr_a = mr_a_;
	}

	public MatchRecord   getMatchRecord(int i) { return mr_a[i]; }
	public MatchRecord[] getMatchRecords()     { return mr_a; }

	public int getRowCount()    { return mr_a.length; }
	public int getColumnCount() { return 8; } 
	
	public Object getValueAt(int row, int col)
	{
	    if(mr_a == null)
		return null;
	    if(row >= mr_a.length)
		return null;

	    try
	    {
		switch(col)
		{
		case 0:
		    return mr_a[row].c1.src == null ? "?" : mr_a[row].c1.src.getName();
		case 1:
		    return String.valueOf(mr_a[row].c1.sorted_spot_ids.length);
		case 2:
		    return String.valueOf(mr_a[row].c1.depth);
		case 3:
		    return mr_a[row].getFirstMatch() == null ? null : mr_a[row].getMatchNames();
		case 4:
		    return String.valueOf(mr_a[row].width);
		case 5:
		    return mr_a[row].getFirstMatch() == null ? null : String.valueOf(mr_a[row].getMatchSize());
		case 6:
		    return mr_a[row].getFirstMatch() == null ? null : String.valueOf(mr_a[row].getFirstMatch().depth);
		default:
		    return mr_a[row].getFirstMatch() == null ? null : mview.niceDouble( (((double) mr_a[row].score / 3.6)), 7, 3 );
		}
	    }
	    catch(NullPointerException npe)
	    {
		System.out.println("hmmmm...npe row=" + row + " col=" + col);
		return "?";
	    }
	}
	public String getColumnName(int col) 
	{
	    switch(col)
	    {
	    case 0:
		return "Cluster";
	    case 1:
		return "(size)";
	    case 2:
		return "(depth)";
	    case 3:
		return "matches";
	    case 4:
		return "(width)";
	    case 5:
		return "(size)";
	    case 6:
		return "(depth)";
	    default:
		return "Score";
	    }
	}

	public MatchRecord[] getData() { return mr_a; }

	private MatchRecord[] mr_a;
    }

    private int last_click_col = -1;
    private boolean last_dir = true;

    // instantiates and invokes a comparator
    // above state used to implement toggling between ascending and descending sort
    
    private void sortMatchRecords( int col )
    {
	
	reportBusy(true);

	if(last_click_col == col)
	    last_dir = !last_dir;
	last_click_col = col;

	MatchRecord[] data = ((ResultTableModel) result_table.getModel()).getData();
	
	java.util.Arrays.sort( data, new MatchRecordSizeComparator( col, last_dir ));

	// clear the selection
	result_table.setSelectionModel( new DefaultListSelectionModel() );

	result_table.revalidate(); 

	reportBusy(false);
    }
    
    // ****************
    // TODO:::: needs to handle MatchRecords with  multiple FlatClusters
    // ****************
    //
    // used by drag-n-drop, finds indices of MatchRecords which refer to 'cl'
    //
    private int[] findClusterInMatchRecords( ExprData.Cluster cl)
    {
	Vector hits = new Vector();
	
	MatchRecord[] data = ((ResultTableModel) result_table.getModel()).getData();

	if(data == null)
	    return null;

	for(int d=0; d < data.length; d++)
	{
	    if((data[d].c1.src == cl) || ((data[d].getFirstMatch() != null) && (data[d].getFirstMatch().src == cl)))
		hits.addElement(new Integer(d));
	}

	int[] hits_a = new int[hits.size()];

	for(int h=0; h < hits_a.length; h++)
	    hits_a[h] = ((Integer) hits.elementAt(h)).intValue();

	return hits_a;
    }

    // ----------------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------------
    //
    // MatchRecordViewer
    //
    // ----------------------------------------------------------------------------------------------
    //
    // displays details of one MatchRecord
    //
    // ----------------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------------

    /*
    public class CustomJFrame extends JFrame
    {
	public CustomJFrame(String name) { super(name); }

	protected void processEvent(AWTEvent e)
	{
	    try
	    {
		super.processEvent(e);
	    }
	    catch(Throwable exc)
	    {
		System.out.println(" Ha! got you you little bugger");
	    }
	}
    }
    */

    private void displayMatchRecord(int mri)
    {
	final ResultTableModel rtm = (ResultTableModel) result_table.getModel();
	final MatchRecord mr = rtm.getMatchRecord(mri);
	
	if(mr.fcv == null)
	    // only one FlatCluster matches in this MatchRecord, popup not needed
	    return;

	/*
	System.out.println("match: " + mr.name + " has " + ((mr.fcv == null) ? " 1 hit" : mr.fcv.size() + " hits"));
	for(int h=0; h < mr.fcv.size(); h++)
	    System.out.println("      " + ((FlatCluster)mr.fcv.elementAt(h)).src.getName());

	System.out.println("   width=" + mr.width);
	*/

	final JFrame cframe = new JFrame( "Matches for " + mr.c1.src.getName());
	
	mview.decorateFrame(cframe);

	cframe.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	
	final DragAndDropTable ctable = new DragAndDropTable();
	ctable.setFont( new Font( "Courier", Font.PLAIN, 12 ));

	// ====================================

	// ctable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	ctable.setColumnSelectionAllowed(false); 

	ctable.setDragAction( new DragAndDropTable.DragAction() 
	    {
		public DragAndDropEntity getEntity()
		{
		    int row = ctable.getSelectedRow();
		    int col = ctable.getSelectedColumn();
		    if(col == 0)
		    {
			System.out.println("drag from " + col + "," + row);
			
			ExprData.Cluster cl = (ExprData.Cluster) (((DefaultTableModel)ctable.getModel()).getValueAt(row, col));
			if(cl != null )
			    return DragAndDropEntity.createClusterEntity(cl);
		    }
		    return null;
		}
	    });
	
	// ====================================

	//	final NameTagSelector label_nts = new NameTagSelector(mview);

	cframe.addWindowListener(new WindowAdapter() 
			  {
			      public void windowClosing(WindowEvent e)
			      {
				  cleanUp();
			      }
			  });

	JPanel panel = new JPanel();
	// panel.setPreferredSize(new Dimension(550, 300));
	GridBagLayout gridbag = new GridBagLayout();
	panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
	panel.setLayout(gridbag);
	GridBagConstraints c = null;

	int line = 0;

	JLabel title = new JLabel("Matches for " + mr.c1.src.getName());	
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	//c.gridwidth = 2;
	//c.weighty = 2.0;
	//c.fill = GridBagConstraints.BOTH;
	//c.anchor = GridBagConstraints.SOUTH;
	gridbag.setConstraints(title, c);
	panel.add(title);


	JScrollPane jsp = new JScrollPane(ctable);
	jsp.setPreferredSize( new Dimension( 550, 300 ));
	
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	//c.gridwidth = 2;
	//c.weighty = 2.0;
	c.weightx = 10.0;
	c.weighty = 9.0;
	c.fill = GridBagConstraints.BOTH;
	//c.anchor = GridBagConstraints.SOUTH;
	gridbag.setConstraints(jsp, c);
	panel.add(jsp);
	
	// ------------------------------------------------------------------
	// == buttons =======================================================
	// ------------------------------------------------------------------

	{
	    JPanel wrapper = new JPanel();
	    wrapper.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrapper.setLayout(w_gridbag);

	    
	    // 
	    // the Help & Cancel buttons
	    //

	    {
		JButton button = new JButton("Close");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    cframe.setVisible(false);
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 3;
		c.gridy = 0;
		w_gridbag.setConstraints(button, c);
	    }
	    {
		JButton button = new JButton("Help");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    mview.getPluginHelpTopic("CompareClusters", "CompareClusters", "#matchviewer");
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 4;
		c.gridy = 0;
		w_gridbag.setConstraints(button, c);
	    }

	    panel.add(wrapper);
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    //c.gridwidth = 2;
	    //c.weighty = 2.0;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    //c.anchor = GridBagConstraints.SOUTH;
	    gridbag.setConstraints(wrapper, c);
	    
	}

	
	cframe.getContentPane().add(panel);
	cframe.pack();
	cframe.setVisible(true);

	// ====================================

	populateMatchRecordTable( ctable, mr);
	
	cframe.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    // returns an array of spot ids which indicate which spot has been used in each row
    // of the table
    public void populateMatchRecordTable( JTable ctable, MatchRecord mr)
    {
	// cluster_name, spot_tag, context_text

	Vector data = new Vector();

	if(mr.fcv != null)
	    for(int h=0; h < mr.fcv.size(); h++)
	    {
		Vector line  = new Vector();
		FlatCluster fc = (FlatCluster)mr.fcv.elementAt(h);
		line.addElement(fc.src);
		line.addElement(String.valueOf( fc.sorted_spot_ids.length ));
		data.addElement(line);
	    }

	Vector colnames = new Vector();
	colnames.addElement("Cluster");
	colnames.addElement("Size");

	ctable.setModel( new DefaultTableModel( data, colnames ));

	int tw = ctable.getWidth();
	
	TableColumn column = null;
	column = ctable.getColumnModel().getColumn(0);
	column.setWidth((5 * tw) / 6);
	column.setPreferredWidth((5 * tw) / 6);

	column = ctable.getColumnModel().getColumn(1);
	column.setWidth(tw / 6);
	column.setPreferredWidth(tw / 6);
	ctable.revalidate();
    }

    // ----------------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------------
    //
    // CoverageTableModel
    //
    // ----------------------------------------------------------------------------------------------
    //
    // displays the coverage statistics
    //
    // ----------------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------------

    private class CoverageTableModel extends javax.swing.table.AbstractTableModel
    {
	public CoverageTableModel(CoverageRecord[] cr_a_)
	{
	    cr_a = cr_a_;
	}

	public int getRowCount()    { return cr_a == null ? 0 : cr_a.length; }
	public int getColumnCount() { return 4; } 
	
	public Object getValueAt(int row, int col)
	{
	    if(cr_a == null)
		return null;
	    if(row >= cr_a.length)
		return null;

	    try
	    {
		switch(col)
		{
		case 0:
		    return cr_a[row].src.getName();
		    
		case 1:
		    return String.valueOf(cr_a[row].total_n_spots);

		case 2:
		    return (cr_a[row].covered_by == null) ? null : String.valueOf(cr_a[row].covered_by.getName());

		default:
		    return String.valueOf(cr_a[row].total_n_spots - cr_a[row].match_count);
		}
	    }
	    catch(NullPointerException npe)
	    {
		System.out.println("hmmmm...npe row=" + row + " col=" + col);
		return "?";
	    }
	}
	public String getColumnName(int col) 
	{
	    switch(col)
	    {
	    case 0:
		return "Cluster";
	    case 1:
		return "size";
	    case 2:
		return "covered by";
	    default:
		return "unmatched";
	    }
	}

	public CoverageRecord[] getData() { return cr_a; }
	
	private CoverageRecord[] cr_a;
    }

    public class CoverageRecord
    {
	ExprData.Cluster src;
	ExprData.Cluster covered_by;
	int total_n_spots;
	int match_count;
	int matched_spots;

	public CoverageRecord(ExprData.Cluster src_, ExprData.Cluster cb_,
			      int total_n_spots_, int match_count_)
	{
	    src = src_;
	    covered_by = cb_;
	    total_n_spots = total_n_spots_;
	    match_count = match_count_;
	}

    }

    // instantiates and invokes a comparator
    // state used to implement toggling between ascending and descending sort
    
    private int     last_cr_click_col = -1;
    private boolean last_cr_dir       = true;

    private void sortCoverageRecords( int col )
    {
	
	reportBusy(true);

	if(last_cr_click_col == col)
	    last_cr_dir = !last_cr_dir;
	last_cr_click_col = col;

	CoverageRecord[] data = ((CoverageTableModel) coverage_table.getModel()).getData();
	
	java.util.Arrays.sort( data, new CoverageRecordComparator( col, last_cr_dir ));

	// clear the selection
	coverage_table.setSelectionModel( new DefaultListSelectionModel() );
	
	coverage_table.revalidate(); 
	
	reportBusy(false);
    }
  
    private class CoverageRecordComparator implements java.util.Comparator
    {
	public CoverageRecordComparator(int mode_, boolean ascend_)
	{
	    mode = mode_;
	    ascend = ascend_;
	}
	public int compare(Object o1, Object o2)
	{
	    CoverageRecord c1 = (CoverageRecord) o1;
	    CoverageRecord c2 = (CoverageRecord) o2;

	    int res;

	    switch(mode)  // mode is the same as cols in the table
	    {
	    case 0: // "Cluster"
		res = (c1.src.getName().compareTo(c2.src.getName()));
		break;
	    case 1:  // "size"
		res = (c1.total_n_spots - c2.total_n_spots);
		break;
	    case 2:
		if(c1.covered_by == null)
		    res = (c2.covered_by == null) ? 0 : 1;
		else
		    res = (c2.covered_by == null) ? -1 : (c1.covered_by.getName().compareTo(c2.covered_by.getName()));
		break;
	    default:  // "covered by"
		res = (c1.total_n_spots - c1.match_count) - (c2.total_n_spots - c2.match_count);
		break;
	    }
	    return ascend ? -res : res;
	}
	
	public boolean equals(Object o) { return false; }

	private boolean ascend;
	private int mode;
    }

    private int[] findClusterInCoverageRecords( ExprData.Cluster cl)
    {
	Vector hits = new Vector();
	
	CoverageRecord[] data = ((CoverageTableModel) coverage_table.getModel()).getData();

	if(data == null)
	    return null;

	for(int d=0; d < data.length; d++)
	{
	    if((data[d].src == cl) || (data[d].covered_by == cl))
		hits.addElement(new Integer(d));
	}

	int[] hits_a = new int[hits.size()];

	for(int h=0; h < hits_a.length; h++)
	    hits_a[h] = ((Integer) hits.elementAt(h)).intValue();

	return hits_a;
    }

    // ----------------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------------
    //
    // CustomTreeCellRender
    //
    // ----------------------------------------------------------------------------------------------
    //
    // draws a cluster tree with glyphs and graphical representations
    // of the MatchRecord for each cluster
    //
    // ----------------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------------

    public class CustomTreeCellRenderer extends DefaultTreeCellRenderer
    {
	//private ImageIcon leaf_icon;
	private Font italic_font = null;
	private Font normal_font = null;
	private Font bold_font   = null;

	private Polygon[] glyph_poly = null;
	private int glyph_poly_height;

	private int draw_glyph;
	private boolean is_shown;
	private MatchRecord mr;
	private Color glyph_colour;
	private int dial_grey_level;

	private boolean draw_edge;  // to indicate selected node

	public CustomTreeCellRenderer()
	{
	    setLeafIcon(null);
	    setOpenIcon(null);
	    setClosedIcon(null);
	}

	public void paintComponent(Graphics g)
	{
	    final int h = getHeight();
	    final int gap = h / 2;
	    final int cd = h-1;

	    int off = 1;
	    
	    if(mr.score > 0)
	    {
		g.setColor( new Color( dial_grey_level, dial_grey_level, dial_grey_level));
		g.drawOval( off, 0, cd, cd);
		g.fillArc( off, 0, cd, cd, 0, mr.score);
	    }
	    else
	    {
		g.setColor(Color.red);
		g.fillRect( off, 0, cd, cd);
	    }

	    off += (h + gap);

	    g.setColor(glyph_colour);

	    if(draw_glyph >= 0)
	    {
		
		if((glyph_poly == null) || (glyph_poly_height != h))
		{
		    // generate (or re-generate at a new size) the glyphs
		    glyph_poly = mview.getDataPlot().getScaledClusterGlyphs(h-2);
		    glyph_poly_height = h;
		    
		    // System.out.println("^v^v^v^v^ CustomTreeCellRenderer(): glyphs rescaled");
		}
		
		
		Polygon poly = new Polygon(glyph_poly[draw_glyph].xpoints, 
					   glyph_poly[draw_glyph].ypoints,
					   glyph_poly[draw_glyph].npoints);
		
		poly.translate(off, 0);

		if(is_shown)
		    g.fillPolygon(poly);
		else
		    g.drawPolygon(poly);

		off += h + 1;
	    }
	    
	    g.drawString(getText(), off, h-4);

	    if(draw_edge)
		g.drawRect(0, 0, getWidth()-1, getHeight()-1);
	}

	public Component getTreeCellRendererComponent(JTree tree,
						      Object value,
						      boolean sel,
						      boolean expanded,
						      boolean leaf,
						      int row,
						      boolean hasFocus) 
	{
	    super.getTreeCellRendererComponent(tree, value, sel,
					       expanded, leaf, row,
					       hasFocus);

	    backgroundNonSelectionColor = backgroundSelectionColor = Color.white;
	    borderSelectionColor = Color.black;

	    // borderSelectionColor = (tree_grey_level > (2*51)) ? Color.black : Color.white;

	    if(italic_font == null)
	    {		
		Graphics g = tree.getGraphics();
		if(g != null)
		{
		    normal_font = g.getFont();
		    if(normal_font != null)
		    {
			italic_font = new Font(normal_font.getName(), Font.ITALIC, normal_font.getSize());
			bold_font   = new Font(normal_font.getName(), Font.BOLD,   normal_font.getSize());
		    }
		}
	    }

	    
	    DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
	    if(node != null)
	    {
		draw_edge = sel;

		mr = (MatchRecord) node.getUserObject();
		ExprData.Cluster cl = mr.c1.src;

		// biggest cluster is darkest grey level...
		// generate grey levels 10...180
		//
		double clust_rel_size = (double) mr.c1.sorted_spot_ids.length / (double)biggest_flat_cluster_size;
		
		dial_grey_level = 10 + (int)((1. - clust_rel_size) * 170);
		if(dial_grey_level < 10)
		    dial_grey_level = 10;

		if(cl.getParent() != null)
		{
		    // this is a leaf
		    //
		    draw_glyph = cl.getGlyph();
		    is_shown = cl.getShow();
		    setFont(normal_font);
		    glyph_colour = cl.getColour();
		}
		else 
		{
		    // this is the root
		    //
		    draw_glyph = -1;
		    setFont(bold_font);
		    setIcon(null);
		} 
	    }

	    setBackground(Color.white);
	    
	    return this;
	}
    }

    // ----------------------------------------------------------------------------------------------
    //
    //  tree expander
    //
    // ----------------------------------------------------------------------------------------------

    private final void openTreeToDepth(final int depth)
    {
	final JTree target = result_tree;
	if(target == null)
	    return;
	final DefaultTreeModel model      = (DefaultTreeModel)       target.getModel();
	if(model == null)
	    return;
	final DefaultMutableTreeNode node = (DefaultMutableTreeNode) model.getRoot();

	// fully collapse the tree...
	int rc = target.getRowCount();
	while(rc > 0)
	{
	    if(target.isExpanded(rc))
		target.collapseRow(rc);
	    rc--;
	}
	
	if(depth > 0)
	    openNodeToDepth(target, model, node, depth);


    }

    private final void openNodeToDepth(final JTree target, final DefaultTreeModel model, 
				       final DefaultMutableTreeNode node, final int depth)
    {
	TreeNode[] tn_path = model.getPathToRoot(node);
	TreePath tp = new TreePath(tn_path);
	target.expandPath(tp);
	
	if(depth > 0)
	{
	    final int n_c = node.getChildCount();
	    for(int c=0; c < n_c; c++)
	    {
		openNodeToDepth(target, model, (DefaultMutableTreeNode) node.getChildAt(c), depth-1);
	    }
	}
    }



    public class CustomKeyListener implements KeyListener
    {
	public void keyTyped(KeyEvent e) 
	{
	}
	
	public void keyPressed(KeyEvent e) 
	{
	}
	
	public void keyReleased(KeyEvent e) 
	{
	    handleKeyEvent(e);
	}
	public void keyAction(KeyEvent e) 
	{
	}

	protected void handleKeyEvent(KeyEvent e)
	{
	    if(tabbed.getSelectedIndex() != 2)
		return;

	    switch(e.getKeyCode())
	    {
	    case KeyEvent.VK_0:
		openTreeToDepth(0);
		break;
	    case KeyEvent.VK_1:
		openTreeToDepth(1);
		break;
	    case KeyEvent.VK_2:
		openTreeToDepth(2);
		break;
	    case KeyEvent.VK_3:
		openTreeToDepth(3);
		break;
	    case KeyEvent.VK_4:
		openTreeToDepth(4);
		break;
	    case KeyEvent.VK_5:
		openTreeToDepth(5);
		break;
	    }
	}
    }

    // ----------------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------------
    //
    //  FlatCluster
    //
    // ----------------------------------------------------------------------------------------------
    //
    //  FlatCluster stores a flattened representation of a cluster and its children
    //
    // ----------------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------------

    private class FlatCluster
    {
	ExprData.Cluster src;
	int   depth;
	int[] sorted_spot_ids;
	
	public int size() { return sorted_spot_ids == null ? 0 : sorted_spot_ids.length; }

	public FlatCluster( ExprData.Cluster src_, int d )
	{
	    this(src_, d, true);
	}
	
	public FlatCluster( ExprData.Cluster src_, int d, boolean include_children )
	{
	    src = src_;
	    depth = d;

	    if(include_children)
	    {
		sorted_spot_ids = getAllClusterElements(src);
		java.util.Arrays.sort(sorted_spot_ids);
	    }
	    else
	    {
		int[] els = src.getElements();
		if(els == null)
		{
		    sorted_spot_ids = null;
		}
		else
		{
		    sorted_spot_ids = els;
		    java.util.Arrays.sort(sorted_spot_ids);
		}
	    }
	}

	public boolean contains(int spot_id)
	{
	    int ss = 0;
	    while(ss < sorted_spot_ids.length)
	    {
		if(sorted_spot_ids[ss] == spot_id)
		    return true;
		if(sorted_spot_ids[ss] > spot_id)
		    return false;
		ss++;
	    }
	    return false;
	}
	public boolean contains(FlatCluster fc)
	{
	    // all elements of 'fc.sorted_spot_ids' must be in 'this.sorted_spot_ids'
	    final int fcsl = fc.sorted_spot_ids.length;

	    if(fcsl > sorted_spot_ids.length)  // short-cut, can't possibly contain a bigger set
		return false;

	    for(int i=0; i < fcsl; i++)
	    {
		if(!contains(fc.sorted_spot_ids[i]))
		    return false;
	    }
	    return true;
	}

	
	
    }

    // ------------------------------------

    private Vector makeFlatClusters(ExprData.Cluster cl)
    {
	Vector v = new Vector();

	addFlatCluster(v, cl, 0);

	return v;
    }

    private void addFlatCluster(Vector v, ExprData.Cluster cl, int depth)
    {
	v.addElement( new FlatCluster( cl, depth ));
	Vector ch = cl.getChildren();
	if(ch != null)
	    for(int c=0; c < ch.size(); c++)
	    {
		addFlatCluster( v, (ExprData.Cluster) ch.elementAt(c), depth+1 );
	    }
    }
    

    // ----------------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------------
    //
    //  extra bits for detecting uncovered clusters
    //
    // ----------------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------------
 
    public void addUncovered( Hashtable uncovered_ht, Vector flat_cluster_v, Vector coverage_record_v )
    {
	System.out.println("there are " +  uncovered_ht.size() + " uncovered clusters");

	final int nfc = flat_cluster_v.size();
	int count = 0;
	for(int f=0; f < nfc; f++)
	{
	    FlatCluster fc = (FlatCluster) flat_cluster_v.elementAt(f);
	    ExprData.Cluster cl = (ExprData.Cluster) uncovered_ht.get(fc.src.getName());
	    if(cl != null)
	    {
		int size = fc.sorted_spot_ids == null ? 0 : fc.sorted_spot_ids.length;
		coverage_record_v.addElement( new CoverageRecord( cl, null, size, 0 ));
		count++;
	    }
	}
	System.out.println("  ..." + count + " found");
    }

    public void populateHashTableWithClusters( Hashtable uncovered_ht, ExprData.Cluster cl )
    {
	// System.out.println( cl.getName() );
	uncovered_ht.put(cl.getName(), cl);

	final int nc = cl.getNumChildren();
	final Vector ch = cl.getChildren();
	for(int c=0; c < nc; c++)
	    populateHashTableWithClusters(  uncovered_ht, (ExprData.Cluster) ch.elementAt(c) );
    }
	
    // ----------------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------------
    //
    //  showMatched
    //
    // ----------------------------------------------------------------------------------------------
    //
    // sets the visibilty of the clusters such that only matches with a minimum score 
    // and maximum width are displayed
    //
    // ----------------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------------
 
    private int min_size = 1;
    private int max_width = 1;
    private double min_score = 50.0;

    public void showMatched()
    {
	try
	{
	    ResultTableModel rtm = (ResultTableModel) result_table.getModel();
	    MatchRecord[] mr_a = rtm.getMatchRecords();
	    
	    // retreive the old parameter values
	    min_score = mview.getDoubleProperty("CompareClusters.min_match_score",  95.);
	    min_size = mview.getIntProperty("CompareClusters.min_clust_size",  1);
	    max_width = mview.getIntProperty("CompareClusters.max_clust_width", 0);

	    // prompt the user to change the parameter values
	    min_score = mview.getDouble("Minimum match score:", 0, 100, min_score);
	    min_size = mview.getInteger("Minimum Cluster size:", 0, edata.getNumSpots(), min_size);
	    max_width = mview.getInteger("Maximum Cluster width:", 0, edata.getNumSpots(), max_width);
	    
	    // save the parameter values for next time
	    mview.putDoubleProperty("CompareClusters.min_match_score",  min_score);
	    mview.putIntProperty("CompareClusters.min_clust_size",      min_size);
	    mview.putIntProperty("CompareClusters.max_clust_width",     max_width);

	    // hide all clusters
	    edata.clusterShowAll(edata.getRootCluster(), false);
	    
	    // now visit each MatchRecord and show clusters in the MatchRecords
	    // which fit into the users parameter values
	    //
	    for(int mri=0; mri < mr_a.length; mri++)
	    {
		MatchRecord mr = mr_a[mri];
		double score_pc = (double) mr.score * (1.0 / 3.6);
		
		if(score_pc >= min_score)
		{
		    if((mr.c1.src.getNumElements() >= min_size) && (mr.width <= max_width))
		    {
			if(mr.c1.src.getNumElements() > 0)
			    mr.c1.src.setShow( true );
		    }
		    
		    // and also show the clusters that matched it....
		    if(mr.c2 != null)
		    {
			if(mr.c2.src.getNumElements() > 0)
			    mr.c2.src.setShow( true );
		    }
		    else
		    {
			for(int c=0; c < mr.fcv.size(); c++)
			{
			    FlatCluster cfc = (FlatCluster) mr.fcv.elementAt(c);
			    if(cfc.src.getNumElements() > 0)
				cfc.src.setShow( true );
			}
		    }
		}
	    }
	    // generate an event
	    //
	    edata.generateClusterUpdate(ExprData.VisibilityChanged);
	}
	catch(UserInputCancelled uic)
	{
	    
	}
	

    }

    // ----------------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------------
    //
    //  export
    //
    // ----------------------------------------------------------------------------------------------
    //
    // writes some or all of the match and coverage data to a text file
    //
    // ----------------------------------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------------

    public void exportData()
    {
	try
	{
	    final String[] save_opts = { "Matching data", "Coverage data" };

	    int[] save_what = mview.getChoice("Save what?", null, save_opts );
	    

	}
	catch(UserInputCancelled uic)
	{
	    
	}
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

    private DragAndDropTree cl1_tree;
    private DragAndDropTree cl2_tree;

    private DragAndDropTree  result_tree;
    private DragAndDropTable result_table;
    private DragAndDropTable coverage_table;

    private JLabel status_label;
    private JButton match_button, export_button;

    private JCheckBox decompose_jcb, coverage_jcb;

    private Vector match_record_v;
    private Vector coverage_record_v;

    private JFrame     frame = null;
}
