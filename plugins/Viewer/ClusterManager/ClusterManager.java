import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import javax.swing.tree.*;

import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreeSelectionModel;

public class ClusterManager implements ExprData.ExprDataObserver, Plugin
{
    public ClusterManager(maxdView mview_)
    {
	mview = mview_;
	edata = mview.getExprData();

    }

    public void cleanUp()
    {
	edata.removeObserver(this);

	if(timer != null)
	{
	    timer.stop();
	    timer = null;
	}
	
	

	mview.putIntProperty("clustman.width", panel.getWidth());
	mview.putIntProperty("clustman.height", panel.getHeight());
	mview.putIntProperty("clustman.hsplit", h_split_pane.getDividerLocation());
	mview.putIntProperty("clustman.vsplit", v_split_pane.getDividerLocation());
	mview.putIntProperty("clustman.colour", mview.colorToInt(tree_bg_col));

	//System.out.println("bye!\nv split was " + mview.getIntProperty("clustman.vsplit", 150));
	//System.out.println("h split was " + mview.getIntProperty("clustman.hsplit", 150));


	frame.setVisible(false);
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  toolbar controls
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    final private int[] grey_levels = { 0, 51, 2*51, 3*51, 4*51, 5*51 };

    class BackgroundButtonActionListener implements ActionListener
    {
	private int gl;

	BackgroundButtonActionListener(int gl_)
	{
	    gl = gl_;
	}
	public void actionPerformed(ActionEvent e) 
	{
	    setTreeBackground(grey_levels[gl]);
	}
    }

    protected void addMenus(JFrame frame) 
    {
	JMenuBar menu_bar = new JMenuBar();
	
	JMenu menu = new JMenu("Manager");

	JMenuItem jmi = new JMenuItem("Load");
	menu.add(jmi);
	jmi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.ALT_MASK));
	jmi.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    loadCluster();
		}
	    });
	
	jmi = new JMenuItem("Save");
	menu.add(jmi);
	jmi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));
	jmi.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    createSaveClustersDialog();
		}
	    });
	
	menu.addSeparator();
	
	jmi = new JMenuItem("Close");
	jmi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.ALT_MASK));
	menu.add(jmi);
	jmi.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    cleanUp();
		}
	    });

	menu_bar.add(menu);

	//  ====================================================================

	menu = new JMenu("Cluster");

	jmi = new JMenuItem("Find");
	menu.add(jmi);
	jmi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.ALT_MASK));
	jmi.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    findCluster();
		}
	    });
	menu.addSeparator();

	jmi = new JMenuItem("Create");
	menu.add(jmi);
	jmi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, ActionEvent.ALT_MASK));
	jmi.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    createCluster();
		}
	    });

	jmi = new JMenuItem("Delete");
	menu.add(jmi);
	jmi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.ALT_MASK));
	jmi.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    deleteCluster();
		}
	    });
	menu.addSeparator();

	jmi = new JMenuItem("Re-parent");
	menu.add(jmi);
	jmi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.ALT_MASK));
	jmi.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    reparentCluster();
		}
	    });

	jmi = new JMenuItem("Collapse");
	menu.add(jmi);
	jmi.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    collapseCluster();
		}
	    });

	menu_bar.add(menu);

	//  ====================================================================
	
	menu = new JMenu("Tree");
	jmi = new JMenuItem("Expand fully");
	jmi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.ALT_MASK));
	jmi.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    expandTree();
		}
	    });
	menu.add(jmi);

	jmi = new JMenuItem("Collapse");
	jmi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.ALT_MASK));
		jmi.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    collapseTree();
		}
	    });
	menu.add(jmi);

	JMenu submenu = new JMenu("Background");
	ButtonGroup group = new ButtonGroup();

	for(int bgb = 0; bgb < 6; bgb++)
	{
	    JRadioButtonMenuItem jrbmi = new JRadioButtonMenuItem("  ");

	    jrbmi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1 + bgb, ActionEvent.ALT_MASK));
	    jrbmi.addActionListener(new BackgroundButtonActionListener(bgb));

	    group.add(jrbmi);
	    submenu.add(jrbmi);

	    Color c = new Color(grey_levels[bgb], grey_levels[bgb], grey_levels[bgb]);
	    jrbmi.setBackground(c);
	}

	menu.add(submenu);
	
	menu_bar.add(menu);
	
	//tool_bar.add(menu_bar);

	menu = new JMenu("Cycle");
	JCheckBoxMenuItem jcbmi = new JCheckBoxMenuItem("Enable");
	jcbmi.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK));
	jcbmi.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    startStopCycleClusters();
		}
	    });
	menu.add(jcbmi);

	jcbmi = new JCheckBoxMenuItem("Descend");
	jcbmi.setSelected(cycle_descend);
	jcbmi.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    cycle_descend = ((JCheckBoxMenuItem)e.getSource()).isSelected();
		}
	    });
	menu.add(jcbmi);

	submenu = new JMenu("Speed");

	group = new ButtonGroup();
	slow_jrbmi = new JRadioButtonMenuItem("Slow");
	slow_jrbmi.setSelected(true);
	slow_jrbmi.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    setCycleSpeed(3000);
		}
	    });
	group.add(slow_jrbmi);
	submenu.add(slow_jrbmi);

	med_jrbmi = new JRadioButtonMenuItem("Medium");
	med_jrbmi.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    setCycleSpeed(1500);
		}
	    });
	group.add(med_jrbmi);
	submenu.add(med_jrbmi);

	fast_jrbmi = new JRadioButtonMenuItem("Fast");
	fast_jrbmi.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    setCycleSpeed(250);
		}
	    });
	group.add(fast_jrbmi);
	
	submenu.add(fast_jrbmi);

	menu.add(submenu);
	menu_bar.add(menu);
	
	//tool_bar.add(menu_bar);

	menu = new JMenu("Help");
	jmi = new JMenuItem("Help");
	menu.add(jmi);
	jmi.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    mview.getPluginHelpTopic("ClusterManager", "ClusterManager");
		}
	    });
	menu_bar.add(Box.createHorizontalGlue());
	menu_bar.add(menu);

	frame.setJMenuBar(menu_bar);
    }
    
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  tree controllers
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    void setTreeBackground(int grey_level)
    {
	Color c = new Color(grey_level, grey_level, grey_level);

	tree_bg_col = c;
	tree_grey_level = grey_level;
	tree.setBackground(tree_bg_col);
	//repaint();
	
    }


    private void createNodes() 
    {
	// System.out.println("createNodes()");

	tree_top = null;
	ExprData.Cluster cluster = edata.getRootCluster();

	if(cluster.getParent() == null) // this really is the root
	    tree_top = new DefaultMutableTreeNode(cluster);
	
	recursivelyAddClustersToTree(tree_top, cluster);

	// tree.expandRow(0);
    }

    private void installNodes()
    {
	// System.out.println("installNodes()");

	if(tree_top != null)
	    tree_model.setRoot(tree_top);

	/*
	else
	{
	    // generate a made-up node to keep the tree happy
	    // (trees have to have at least 1 node...)
	    DefaultMutableTreeNode made_up = new DefaultMutableTreeNode("Made up");
	    tree_top = made_up;
	}
	*/

    }

    private void recursivelyAddClustersToTree(DefaultMutableTreeNode top, ExprData.Cluster cluster)
    {
	if(cluster == null)
	{
	    //tree_top = new DefaultMutableTreeNode("No data");
	    
	}
	else
	    if((cluster.getParent() == null) || (cluster.getChildren() != null))
	    {
		// this is a group of clusters, or the root cluster, 
		// create a new category, and add each of the children to it
		//
		DefaultMutableTreeNode category = new DefaultMutableTreeNode(cluster);
		
	        //System.out.println("*>*>* adding new category, " + cluster.getName());
		
		if(top == null)
		    // this is the first node being inserted...
		    tree_top = category;
		else
		    // this is being added as a child of an existing category
		    top.add(category);

		//int rc = tree.getRowCount();

		//if(cluster.getParent() == edata.getRootCluster())
		//    tree.expandRow(rc);

		for(int i=0; i< cluster.getNumChildren(); i++)
		    recursivelyAddClustersToTree(category, (ExprData.Cluster) cluster.getChildren().elementAt(i));
		
	    }
	    else
	    {
		//System.out.println("*>*>* adding new cluster, " + cluster.getName());

		if((top != null) && (cluster != null))
		    top.add(new DefaultMutableTreeNode(cluster));
	    }
  
	
    }

    public void rebuildTree()
    {
	//System.out.println("rebuilding tree....");
	
	// record the open/closed state of the branches in the old tree
	// and use this to build a new tree with the same state
	// (hopefully this is easier than updating the tree in situ)

	DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree_model.getRoot();
	
	Vector present_tree_state = new Vector();

	//System.out.println("-----------------\n- Tree State ----");
	final int tr = tree.getRowCount();
	for(int tri=0; tri < tr; tri++)
	{
	    if(tree.isExpanded(tri))
	    {
		TreePath tp = tree.getPathForRow(tri);
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) tp.getLastPathComponent();
		ExprData.Cluster clust = (ExprData.Cluster) node.getUserObject();
		// System.out.println(clust.getName() + " is open");
		present_tree_state.add(clust);
	    }
	}
	//System.out.println("-------------\n");

	createNodes();
	installNodes();

	// now open any of the nodes which were open before....
	// 
	while(present_tree_state.size() > 0)
	{
	    ExprData.Cluster open_clust = (ExprData.Cluster) present_tree_state.elementAt(0);
	    present_tree_state.removeElementAt(0);
	    
	    // go backwards up the tree from the bottom
	    // so that the indices don't change as we open things...
	    //
	    int tri = 0;
	    final int trc = tree.getRowCount();

	    //ExprData.Cluster root_cluster = edata.getRootCluster();

	    while(tri < trc)
	    {
		TreePath tp = tree.getPathForRow(tri);
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) tp.getLastPathComponent();
		ExprData.Cluster clust = (ExprData.Cluster) node.getUserObject();
		
		if((clust == open_clust) /* || (clust.getParent() == root_cluster)*/)
		{
		    tree.expandRow(tri);
		    break;
		}
		tri++;
	    }
	}
    }

    public void updateControls()
    {
	if(current_cluster != null)
	{
	    clust_name_jtf.setText(current_cluster.getName());

	    show_jchb.setSelected(current_cluster.getShow());

	    colour_jb.setBackground(current_cluster.getColour());

	    glyph_jcb.removeActionListener(glyph_al);
	    if(current_cluster.getParent() != null)
	    {
		if(current_cluster.getChildren() == null)
		{
		    // this is a child cluster, glyph indices are normal
		    glyph_jcb.setSelectedIndex(current_cluster.getGlyph());
		    
		    //colour_rv.useColour(current_cluster.getColour());
		}
		else
		{
		    // this is a parent, cluster, must add 1 to indices to
		    // cover the extra "Cycle" element at the top
		    //
		    if(current_cluster.getGlyphCycle() == true)
			glyph_jcb.setSelectedIndex(0);
		    else
			glyph_jcb.setSelectedIndex(current_cluster.getGlyph() + 1);
		    
		    //colour_rv.useColour(current_cluster.getColour());
		    //colour_rv.useRamp();
		    //colour_rv.setRamp(current_cluster.getColourRamp(current_cluster.getColourSet(), 20));
		    
		    //colour_jb.setBackground(current_cluster.getColour());
		}
	    }
	    else
	    {
		// this is the root cluster, do nothing....
	    }
	    glyph_jcb.addActionListener(glyph_al);

	}
    }

    public void clusterHasBeenToggled(DefaultMutableTreeNode node)
    {
	Object node_info = node.getUserObject();
	
	//if (node.isLeaf()) 
	{
	    ExprData.Cluster cluster = (ExprData.Cluster)node_info;
	    if((cluster != null) && (cluster.getChildren() == null))
	    {
		edata.toggleClusterShow(cluster);
	    }
	}
    }

    public void clusterHasBeenSelected(DefaultMutableTreeNode node)
    {
	/*
	hide_all_jb.setEnabled(true);
	show_all_jb.setEnabled(true);
	hide_group_jb.setEnabled(true);
	show_group_jb.setEnabled(true);
	glyph_jcb.setEnabled(true);
	colour_rv.setEnabled(true);
	*/

	Object node_info = node.getUserObject();
	
	ExprData.Cluster last_cluster = current_cluster;

	current_cluster = (ExprData.Cluster)node_info;


	//StringBuffer cluster_info = new StringBuffer();

	updateClusterInfo();

	if(current_cluster.getChildren() == null)
	{
	    // check to see wether we actually need to change the 
	    // controls to the other mode (if the last cluster was 
	    // also a leaf, the no major change need to be made)
	    //
	    if((last_cluster == null) || (last_cluster.getChildren() != null))
	    {
		// enable and update the attribute controls
		//
		//show_jchb.setEnabled(true);

		glyph_jcb.removeActionListener(glyph_al);
		glyph_jcb.removeAllItems();
		for(int i=0;i<shown_glyph_images.length;i++)
		{
		    glyph_jcb.addItem(shown_glyph_images[i]);
		}
		glyph_jcb.addActionListener(glyph_al);

		//colour_label.setText("Colour  ");
		//glyph_label.setText("Glyph  ");
	    }

	    updateControls();
	    
	    //current_group = current_cluster.getParent();
	    
	} 
	else
	{
	    final int nc = current_cluster.getNumChildren();
	    
	    /*
	    String str = "cluster `" + current_cluster.getName();
	    
	    if(current_cluster.getIsSpot() == false)
		str += " [M]";

	    str += "' has " + nc + ((nc == 1) ? " child" : " children") + "\n";

	    //	    cluster_info.append(str);
	    */

	    //if((last_cluster == null) || (last_cluster.getElements() != null))
	    {
		// change the attribute controls to non-leaf mode
		//
		
		glyph_jcb.removeActionListener(glyph_al);
		glyph_jcb.removeAllItems();
		glyph_jcb.addItem(cycle_glyph_image);
		
		for(int i=0;i<shown_glyph_images.length;i++)
		{
		    glyph_jcb.addItem(shown_glyph_images[i]);
		}
		glyph_jcb.addActionListener(glyph_al);

		//colour_label.setText("Colours  ");
		//glyph_label.setText("Glyphs  ");
	    }
	    
	    updateControls();
	    
	    //current_group = current_cluster;
 	}
	
	// view_panel.setText(cluster_info.toString());

	//System.out.println("group=" + current_group);
	
	if(current_cluster != null)
	    group_label.setText(current_cluster.getName());


    }

    private ClusterInfoUpdateThread ciut = null;

    private void updateClusterInfo()
    {
	
	//if(ciut != null)
	{
	    ciut = new ClusterInfoUpdateThread();
	    ciut.setPriority(Thread.MIN_PRIORITY);
	    ciut.start();
	}
    }

    private class ClusterInfoUpdateThread extends Thread
    {
	public void run()
	{
	    StringBuffer cluster_info = new StringBuffer();
	    
	    if(current_cluster == null)
	    {
		cluster_info.append("[no selection]");
	    }
	    else
	    {
		//System.out.println("updating cluster info...");

		int nc = current_cluster.getNumChildren();
		int ne = current_cluster.getNumElements();
		
		String child_info = ((nc == 1) ? " 1 child" : (nc + " children"));
		if(nc == 1)
		    child_info += ( " ('" + ((ExprData.Cluster)current_cluster.getChildren().elementAt(0)).getName() + "')" );
		if(nc == 2)
		    child_info += ( " ('" + ((ExprData.Cluster)current_cluster.getChildren().elementAt(0)).getName() + " and " +
				    ((ExprData.Cluster)current_cluster.getChildren().elementAt(1)).getName() + "')" );
		
		cluster_info.append((current_cluster.getIsSpot() ? "Spot" : "Measurement") +
				    " Cluster\n  '" + current_cluster.getName() + "'\nhas\n  " +
				    child_info + 
				    "\nand\n  " +
				    ((ne == 1) ? " 1 element" : (ne + " elements")));;
		
		
		final String[] en0 = { "Spot index", "Spot indices" };
		final String[] en1 = { "Spot name", "Spot names" };
		final String[] en2 = { "Probe name", "Probe names" };
		final String[] en3 = { "Gene name", "Gene names" };
		final String[] en4 = { "Measurement name", "Measurement names" };
		final String[] en5 = { "Measurement index", "Measurement indices" };
		final String[][] el_name_strs = { en0, en1, en2, en3, en4, en5 };

		String el_names = null;
		
		if(ne > 0)
		{
		    final int enm = current_cluster.getElementNameMode();
		    final Vector elns = current_cluster.getElementNames();
		    final int nelnames = (elns == null) ? 0 : elns.size();
		    
		    int plural = (ne > 1) ? 1 : 0;
		    
		    el_names = el_name_strs[enm][plural];

		    switch(current_cluster.getElementNameMode())
		    {
		    case ExprData.SpotName:
			el_names = "Spot names";
			break;
		    case ExprData.ProbeName:
			el_names = "Probe names";
			break;
		    case ExprData.GeneName:
			el_names = "Gene names";
			break;
		    case ExprData.MeasurementName:
			el_names = "Measurement names";
			break;
		    case ExprData.MeasurementIndex:
			el_names = "Measurement indices";
			break;
		    }
		    

		    cluster_info.append(" defined by " + nelnames + " " + el_names);
		}
		
		if(nc > 0)
		{
		    int[] ids = current_cluster.getAllClusterElements();
		    
		    cluster_info.append("\n\nTotal of " + 
					(ids.length == 1 ? "1 element" : (ids.length + " elements")) +
					" in this cluster and it's descendants");
		}

		int depth = 0;
		ExprData.Cluster cl = current_cluster.getParent();
		String pad = "    ";
		while(cl != null)
		{
		    depth++;
		    pad += " ";
		    cl = cl.getParent();
		}
		
		final StringBuffer ppath = new StringBuffer();
		ExprData.Cluster parent = current_cluster;
		while(parent != null)
		{
		    if(parent.getParent() != null)
			ppath.insert(0, (pad.substring(0, depth) + parent.getName() + "\n"));
		    parent = parent.getParent();
		    depth--;
		}
		
		if(current_cluster.getParent() != null)
		    cluster_info.append("\n\nPath:\n" + ppath.toString());

		
		if(ne > 0)
		{
		    cluster_info.append("\n\n" + el_names + " in this cluster:\n");
		    
		    final Vector en = current_cluster.getElementNames();
		    if(en != null)
		    {
			int lim = en.size();
			if(lim > 505)
			{
			    lim = 500;
			}
			
			for(int e=0; e < lim; e++)
			{
			    if(e > 0)
				cluster_info.append((((e+1) < en.size()) ? ", " : " and "));
			    cluster_info.append(en.elementAt(e));
			    
			    yield();
			}
			if(lim <  en.size())
			    cluster_info.append(" and " + (en.size()-100) + " others.");
		    }
		}
	    }

	    view_panel.setText(cluster_info.toString());
	    view_panel.scrollRectToVisible(new Rectangle(20,20));

	    //System.out.println("...done");
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  observer 
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    //
    // the observer interface of ExprData notifies us whenever something
    // interesting happens to the data as a result of somebody (including us) 
    // manipulating it
    //
    // surprisingly enough, the cluster manager is only interested in
    // cluster events
    //
    public void clusterUpdate(ExprData.ClusterUpdateEvent cue)
    {
	switch(cue.event)
	{
	case ExprData.ColourChanged:
	case ExprData.VisibilityChanged:
	    updateControls();
	    frame.repaint();
	    break;
	    
	case ExprData.OrderChanged:
	case ExprData.SizeChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	case ExprData.NameChanged:
	    
	    rebuildTree();

	    break;
	}
    }

    public void dataUpdate(ExprData.DataUpdateEvent due)
    {
	switch(due.event)
	{
	case ExprData.SizeChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	    //System.out.println("+++ data elems have changed, cluster elems need updating");
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
    // --- --- ---  plugin implementation
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    JFrame frame = null;

    boolean started = false;

    public void startPlugin()
    {
	started = true;

	//System.out.println("*>*>* startPlugin()");
	frame = new JFrame("Cluster Manager");

	mview.decorateFrame(frame);

	frame.addWindowListener(new WindowAdapter() 
	    {
		public void windowClosing(WindowEvent e)
		{
		    //System.out.println("window closed...");
		    cleanUp();
		}
	    });


	frame.getContentPane().setLayout(new BorderLayout());

	//JToolBar tool_bar = new JToolBar();
	//tool_bar.setFloatable(false);
	addMenus(frame);
	//frame.getContentPane().add(tool_bar, BorderLayout.NORTH);

	panel = new JPanel();
	
	int w = mview.getIntProperty("clustman.width",  500);
	int h = mview.getIntProperty("clustman.height", 400);
	panel.setPreferredSize(new Dimension(w, h));

	current_cluster = edata.getRootCluster();

	// col_set_cho = new ColourSetChooser(mview, edata, current_cluster);

	createNodes();

	buildComponents(panel);

	installNodes();

	tree.expandRow(0);

	frame.getContentPane().add(panel);

	frame.pack();

	//System.out.println("v split at " + mview.getIntProperty("clustman.vsplit", 250));
	//System.out.println("h split at " + mview.getIntProperty("clustman.hsplit", 150));

	/*
	att_panel.setPreferredSize(new Dimension(mview.getIntProperty("clustman.hsplit", 150),
						 mview.getIntProperty("clustman.vsplit", 250)));
	*/

	v_split_pane.setDividerLocation(mview.getIntProperty("clustman.vsplit", 250));
	h_split_pane.setDividerLocation(mview.getIntProperty("clustman.hsplit", 150));

	frame.setVisible(true);

	clusterHasBeenSelected(tree_top);
	setCycleSpeed(1500);

	// register ourselves with the data
	//
	edata.addObserver(this);
    }

    public void stopPlugin()
    {
	cleanUp();
    }

    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("Cluster Manager", "viewer", 
							"Load, save and manipulate cluster information", "", 1, 1, 0);
	return pinf;
    }

    public PluginCommand[] getPluginCommands()
    {
	PluginCommand[] com = new PluginCommand[9];
       
	String[] load_args = new String[] 
	{ "file",  "file", "", "m", "source file name", 
	  "where", "string", "root", "m", "either 'root', 'selection'" 
	};
	com[0] = new PluginCommand("load", load_args);


	String[] goto_args = new String[] { "name", "string", "", "m", "name of Cluster to display" };
	com[1] = new PluginCommand("gotoName", goto_args);
	goto_args = new String[] { "id", "string", "", "m", "ID of cluster to display" };
	com[2] = new PluginCommand("gotoID", goto_args);
	
	com[3] = new PluginCommand("createFromFilter", null);

	com[4] = new PluginCommand("createFromSelection", null);  // both does spots and measurments
	com[5] = new PluginCommand("createFromSpotSelection", null);
	com[6] = new PluginCommand("createFromMeasurementSelection", null);

	com[7] = new PluginCommand("createFromIntersection", null);
	com[8] = new PluginCommand("createFromUnion", null);

	return com;
    }
 
    public void   runCommand(String name, String[] args, CommandSignal done) 
    {
	// System.out.println("hello from cluster command " + name);

	if(name.equals("gotoName"))
	{
	    String cname = mview.getPluginArg("name", args);

	    //System.out.println("hello, goto cluster " + cname);

	    if(!started)
		startPlugin();

	    displayCluster(cname);
	}
	if(name.equals("gotoID"))
	{
	   int cid = mview.parseIntArg(mview.getPluginArg("id", args));
	    
	    //System.out.println("hello, goto cluster " + cname);
	   
	   // System.out.println("runCommand(): 'gotoID' arg=" + cid);
	   
	   if(!started)
	       startPlugin();
	   
	   displayCluster( edata.getClusterByID( cid ) );
	}

	if(name.equals("load"))
	{
	    String fname = mview.getPluginArg("file", args);
	    if((fname != null) && (fname.length() > 0))
	    {
		String where = mview.getPluginStringArg("where", args, "root");

		ExprData.Cluster insert_pt = edata.getRootCluster();

		if(!where.equals("root") && (current_cluster != null))
		    insert_pt = current_cluster;

		// System.out.println("loading from '" + fname + "'....");

		File file = new File(fname);
		loadClusterDataFromNativeFile(file, insert_pt);
	    }
	    else
	    {
		loadCluster();
	    }

	    if(done != null)
		done.signal();
	}

	if(name.equals("createFromFilter"))
	{
	    createClusterFromFilter();
	}
	if(name.equals("createFromIntersection"))
	{
	    createClusterFromIntersection();
	}
	if(name.equals("createFromUnion"))
	{
	    createClusterFromUnion();
	}
	if(name.equals("createFromSelection"))
	{
	    createClusterFromSelection(2);
	}
	if(name.equals("createFromSpotSelection"))
	{
	    createClusterFromSelection(0);
	}
	if(name.equals("createFromMeasurementSelection"))
	{
	    createClusterFromSelection(1);
	}
	
	if(done != null)
	    done.signal();
    }



    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  file i/o for clusters 
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void recursivelyWriteClusterNative(Writer writer, ExprData.Cluster cl, 
					      int mode,  boolean children, int indent) throws java.io.IOException
    {
	ExprData edata = mview.getExprData();

	StringBuffer ind_sb = new StringBuffer(indent);
	for(int c=0;c<indent;c++)
	    ind_sb.append(' ');
	String ind = ind_sb.toString();
	
	boolean skipit = false;

	// System.out.println("   writing " + cl.getName() + " native " + (children ? "with" : "without") + " children...");

	int next_ind = indent;
	// don't write the top-level root cluster to the file, 
	// it is implicit in the load
	//
	if(cl.getParent() != null)
	{
	    if(mode == 1) // only visible clusters
		if(cl.getShow() == false)
		    skipit = true;
	    
	    if(!skipit)
	    {

		writer.write(ind + "<CLUSTER>\n");
		writer.write(ind + " <NAME> " + cl.getName() + " </NAME>\n");

		final Color c = cl.getColour();
		final int cnum = (c.getRed() << 16) | (c.getGreen() << 8) | (c.getBlue());
		writer.write(ind + " <COLOUR> " + cnum + " </COLOUR>\n");
		writer.write(ind + " <GLYPH> " + cl.getGlyph() + " </GLYPH>\n");

		final Vector eln = cl.getElementNames();
		final int[] eli = cl.getElements();
		final int n_els = cl.getNumElements();

		if(n_els > 0)
		{
		    writer.write(ind + " <ELEMENTS>\n");
		    writer.write(ind + "  <COUNT> " + eln.size() + " </COUNT>\n");
		    for(int e=0; e < eln.size(); e++)
		    {
			switch(cl.getElementNameMode())
			{
			case ExprData.ProbeName:
			    writer.write(ind + "  <PROBE> ");
			    writer.write((String)eln.elementAt(e));
			    writer.write(" </PROBE>\n");
			    break;
			case ExprData.SpotName:
			    writer.write(ind + "  <SPOT> ");
			    writer.write((String)eln.elementAt(e));
			    writer.write(" </SPOT>\n");
			    break;
			case ExprData.SpotIndex:
			    writer.write(ind + "  <SpotIndex> " + String.valueOf(eli[e]) + " </SpotIndex>\n");
			    break;
			case ExprData.MeasurementIndex:
			    writer.write(ind + "  <MeasurementIndex> " + String.valueOf(eli[e]) + " </MeasurementIndex>\n");
			    break;
			case ExprData.MeasurementName:
			    writer.write(ind + "  <MeasurementName> " + (String)eln.elementAt(e) + " </MeasurementName>\n");
			    break;
			
			}
		    }
		    writer.write(ind + " </ELEMENTS>\n");
		}

		next_ind += 2;
	    }
	}

	if(children)
	{
	    Vector ch = cl.getChildren();
	    if(ch != null)
	    {
		for(int c=0; c < ch.size(); c++)
		{ 
		    recursivelyWriteClusterNative(writer, (ExprData.Cluster)ch.elementAt(c), mode,  children, next_ind);
		}
	    }
	}

	if((cl.getParent() != null) && (!skipit))
	    writer.write(ind + "</CLUSTER>\n");
    }
    
    public void saveClustersNative(File file, int mode, boolean children)
    {
	// System.out.println("saving  native " + (children ? "with" : "without") + " children...");

	try
	{
	    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
	    
	    writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
	    writer.write("<!DOCTYPE maxd SYSTEM \"maxd.dtd\">\n");
	    
	    writer.write("\n<!-- this file generated by maxdView's ClusterManager plugin -->\n\n");
	    
	    if(mode < 2)
	    {
		ExprData.Cluster clust = edata.getRootCluster();
		recursivelyWriteClusterNative(writer, clust, mode, children, 0);
	    }
	    else
	    {
		recursivelyWriteClusterNative(writer, current_cluster, mode, children, 0);
	    }
	    
	    
	    writer.close();
	    // System.out.println("...done");
	}
	catch (java.io.IOException ioe)
	{
	    mview.errorMessage("Unable to write to '" + file.getName() + "'");
		
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void saveClustersListOfNames(File file, int mode, boolean children)
    {
	try
	{
	    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
	    
	    if(mode < 2)
	    {
		ExprData.Cluster clust = edata.getRootCluster();
		recursivelyWriteClusterListOfNames(writer, clust, mode, children, 0);
	    }
	    else
	    {
		recursivelyWriteClusterListOfNames(writer, current_cluster, mode, children, 0);
	    }
	    
	    writer.close();
	    // System.out.println("...done");
	}
	catch (java.io.IOException ioe)
	{
	    mview.errorMessage("Unable to write to '" + file.getName() + "'");
	    
	}
    }

    public void recursivelyWriteClusterListOfNames(Writer writer, ExprData.Cluster cl, 
						   int mode,  boolean children, int indent) throws java.io.IOException
    {
	StringBuffer ind_sb = new StringBuffer(indent);
	for(int c=0;c<indent;c++)
	    ind_sb.append(' ');
	String ind = ind_sb.toString();
	
	boolean skipit = false;

	if(mode == 1) // only visible clusters
	    if(cl.getShow() == false)
		skipit = true;
	

	if(!skipit)
	{
	    // System.out.println("  list-of-names for " +  cl.getName());

	    int n_els = cl.getNumElements();

	    if(n_els > 0)
	    {
		// System.out.println("  " + n_els + " elements");

		switch(cl.getElementNameMode())
		{
		case ExprData.SpotIndex:
		    {
			int[] els = cl.getElements();
			for(int e=0; e < els.length; e++)
			    writer.write(ind + edata.getSpotName(els[e]) + "\n");
		    }
		    break;
		case ExprData.MeasurementIndex:
		    {
			int[] els = cl.getElements();
			for(int e=0; e <  els.length; e++)
			    writer.write(ind + edata.getMeasurementName(els[e]) + "\n");
		    }
		    break;
		case ExprData.MeasurementName:
		case ExprData.ProbeName:
		case ExprData.GeneName:
		case ExprData.SpotName:
		    {
			Vector eln = cl.getElementNames();
			for(int e=0; e < eln.size(); e++)
			    writer.write(ind + (String) eln.elementAt(e) + "\n");
		    }
		    break;
		}
		
		writer.write("\n");
	    }
	}
	else
	{
	    // System.out.println("  skipping list-of-names for " +  cl.getName());
	}

	if(children)
	{
	    Vector ch = cl.getChildren();
	    if(ch != null)
	    {
		for(int c=0; c < ch.size(); c++)
		{ 
		    recursivelyWriteClusterListOfNames(writer, (ExprData.Cluster)ch.elementAt(c), mode,  children, indent + 2);
		}
	    }
	}

    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    
    private final boolean debug_xml_load = true;
    

    public ExprData.Cluster recursivelyReadCluster(StreamTokenizer st)
    {
	boolean done = false;
	ExprData.Cluster new_clust = edata.new Cluster("[unnamed-so-far]");

	new_clusters_read++;
	pm.setMessage(1, new_clusters_read + " clusters");

	try 
	{ 
	    boolean in_cluster = false;
	    boolean in_name = false;
	    boolean in_elements = false;
	    boolean in_probe = false;
	    boolean in_spot = false;
	    boolean in_spot_index = false;
	    boolean in_attr = false;

	    boolean in_meas_name  = false;
	    boolean in_meas_index = false;
	    
	    int name_mode = -1;
	    String accum = null;
	    String new_clust_name = null;
	    Vector tmp_cluster_set = null;
	    
	    int next_glyph = 0;
	    
	    if(debug_xml_load)
		System.out.println("recursive load starts...");
	    
	    while(!done)
	    { 
		int token = st.nextToken();
		
		switch(token)
		{ 
		case java.io.StreamTokenizer.TT_EOF:
		    if(in_cluster)
		    {
			mview.errorMessage("Error in cluster file: missing </CLUSTER>");
			return null;
		    }
		    else
		    {
			done = true;
			break;
		    }
		case java.io.StreamTokenizer.TT_EOL:
		    break;
		case java.io.StreamTokenizer.TT_WORD:
		    
		    boolean is_tag = false;
		    
		    /*
		    if(debug_xml_load)
		    {
			System.out.print("[`");
			System.out.print(st.sval);
			System.out.print("']");
		    }
		    */

		    // check for the _closing_ tags before interpreting anything else...
		    
		    if(st.sval.equals("/COLOUR"))
		    {
			is_tag = true;
			in_attr = false;
			try
			{
			    int col_int = new Integer(accum).intValue();
			    new_clust.setColour(new Color(col_int));
			}
			catch(NumberFormatException nfe)
			{
			}
		    }

		    if(st.sval.equals("/GLYPH"))
		    {
			is_tag = true;
			in_attr = false;
			try
			{
			    int g_int = new Integer(accum).intValue();
			    new_clust.setGlyph(g_int);
			}
			catch(NumberFormatException nfe)
			{
			}
		    }

		    if(st.sval.equals("/ELEMENTS"))
		    {
			is_tag = true;
			in_elements = false;
			if(tmp_cluster_set.size() > 0)
			{
			    new_clust.setElements(name_mode, tmp_cluster_set);

			    if(debug_xml_load)
				System.out.println("[ " + tmp_cluster_set.size() + 
					       " elements in '" + new_clust.getName() + "' ]");
			}
		    }
		    if(st.sval.equals("/NAME"))
		    {
			is_tag = true;
			in_name = false;
			new_clust.setName(accum);
			
			if(debug_xml_load)
			    System.out.println("[ name detected as '" + new_clust.getName() + "' ]");
		    }

		    if(st.sval.equals("/PROBE"))
		    {
			tmp_cluster_set.addElement(accum);
			if(debug_xml_load)
			    System.out.println("probe '" + accum + "' added");

			is_tag = true;
			in_probe = false;
		    }
		    if(st.sval.equals("/SpotIndex"))
		    {
			try
			{
			    tmp_cluster_set.addElement(new Integer(accum));
			    
			    if(debug_xml_load)
				System.out.println("spot '" + accum + "' added");
			}
			catch(NumberFormatException nfe)
			{
			    
			}
			is_tag = true;
			in_spot = false;
		    }

		    if(st.sval.equals("/MeasurementName"))
		    {
			tmp_cluster_set.addElement(accum);
			
			if(debug_xml_load)
			    System.out.println("meas name '" + accum + "' added");

			is_tag = true;
			in_meas_name = false;
		    }

		    if(st.sval.equals("/MeasurementIndex"))
		    {
			tmp_cluster_set.addElement(accum);
			
			if(debug_xml_load)
			    System.out.println("meas index '" + accum + "' added");

			is_tag = true;
			in_meas_index = false;
		    }

		    if(st.sval.equals("/SPOT"))
		    {
			tmp_cluster_set.addElement(accum);

			if(debug_xml_load)
			    System.out.println("spot '" + accum + "' added");

			is_tag = true;
			in_spot = false;
		    }
		    
		    if(st.sval.equals("/CLUSTER"))
		    {
			is_tag = true;
			in_cluster = false;

			// because the tokenizer is shared between all the recursive
			// calls to this method, when we return, we don't want the
			// enclosing instance to see the "/CLUSTER" tag 
			// 
			st.sval = "dummy";
			done = true;

			if(debug_xml_load)
			    System.out.println("cluster '" + new_clust.getName() + "' finished");
		    }

		    if(st.sval.equals("CLUSTER"))
		    {
			is_tag = true;
			ExprData.Cluster child = recursivelyReadCluster(st);
			if(child != null)
			{
			    new_clust.addCluster(child);
			    if(debug_xml_load)
				System.out.println("child " + child.getName() +
						   " added to cluster " + 
						   new_clust.getName());
			    
			    // System.out.println("-----\n" + new_clust + "\n------");
			}
			else
			{
			    // if an error has occurred in the child, go immediatley back up to the top
			    return null;
			}
		    }
		    
		    // -----------------------------

		    if(st.sval.equals("NAME"))
		    {
			is_tag = true;
			in_name = true;
			accum = null;
		    }
		    
		    if(st.sval.equals("COLOUR"))
		    {
			is_tag = true;
			in_attr = true;
			accum = null;
		    }
	
		    if(st.sval.equals("GLYPH"))
		    {
			is_tag = true;
			in_attr = true;
			accum = null;
		    }
	
		    // -----------------------------

		    if(st.sval.equals("PROBE"))
		    {
			if(in_elements)
			{
			    if(name_mode == -1)
			    {
				name_mode = ExprData.ProbeName;
			    }
			    else
			    {
				if(name_mode != ExprData.ProbeName)
				{
				    mview.errorMessage("Unexpected <PROBE>, this cluster starts with <SPOT>s");
				    return null;
				}
			    }
			    is_tag = true;
			    in_probe = true;
			    accum = null;
			}
			else
			{
			    mview.errorMessage("Unexpected <PROBE> (not within an <ELEMENT>...</ELEMENT>)");
			    return null;
			}
		    }
		    if(st.sval.equals("SpotIndex"))
		    {
			if(in_elements)
			{
			    if(name_mode == -1)
			    {
				name_mode = ExprData.SpotIndex;
			    }
			    else
			    {
				if(name_mode != ExprData.SpotIndex)
				{
				    mview.errorMessage("Unexpected <SpotIndex> in " + new_clust.getName());
				    return null;
				}
			    }
			    is_tag = true;
			    in_spot_index = true;
			    accum = null;
			}
			else
			{
			    mview.errorMessage("Unexpected <SpotIndex> (not within an <ELEMENT>...</ELEMENT>)");
			    return null;
			}
		    }

		    if(st.sval.equals("SPOT"))
		    {
			if(in_elements)
			{
			    if(name_mode == -1)
			    {
				name_mode = ExprData.SpotName;
			    }
			    else
			    {
				if(name_mode != ExprData.SpotName)
				{
				    mview.errorMessage("Unexpected <SPOT>, this cluster starts with <PROBE>s");
				    return null;
				}
			    }
			    is_tag = true;
			    in_spot = true;
			    accum = null;
			}
			else
			{
			    mview.errorMessage("Unexpected <SPOT> (not within an <ELEMENT>...</ELEMENT>)");
			    return null;
			}
		    }

		    // -----------------------------

		    if(st.sval.equals("MeasurementName"))
		    {
			if(in_elements)
			{
			    if(name_mode == -1)
			    {
				name_mode = ExprData.MeasurementName;
			    }
			    else
			    {
				if(name_mode != ExprData.MeasurementName)
				{
				    mview.errorMessage("Unexpected <MeasurementName> in " + new_clust.getName());
				    return null;
				}
			    }
			    is_tag = true;
			    in_meas_name = true;
			    accum = null;
			}
			else
			{
			    mview.errorMessage("Unexpected <MeasurementName> (not within an <ELEMENT>...</ELEMENT>)");
			    return null;
			}
		    }

		    if(st.sval.equals("MeasurementIndex"))
		    {
			if(in_elements)
			{
			    if(name_mode == -1)
			    {
				name_mode = ExprData.MeasurementIndex;
			    }
			    else
			    {
				if(name_mode != ExprData.MeasurementIndex)
				{
				    mview.errorMessage("Unexpected <MeasurementIndex> in " + new_clust.getName());
				    return null;
				}
			    }
			    is_tag = true;
			    in_meas_index = true;
			    accum = null;
			}
			else
			{
			    mview.errorMessage("Unexpected <MeasurementIndex> (not within an <ELEMENT>...</ELEMENT>)");
			    return null;
			}
		    }


		    // -----------------------------

		    if(st.sval.equals("ELEMENTS"))
		    {
			is_tag = true;
			in_elements = true;
			accum = null;
			tmp_cluster_set = new Vector();
		    }
		    
		    
		    if( (in_name) || (in_spot) || (in_probe) || (in_spot_index) || 
		        (in_meas_index) || (in_meas_name) || 
		        (in_attr) )
		    {
			if(!is_tag)
			{
			    if(accum == null)
				accum = st.sval;
			    else
				accum += (" " + st.sval);
			}
		    }
		    
		    break;
		}
	    }
	}
	catch (java.io.IOException e)
	{
	    mview.errorMessage("File reading error\n  " + e);
	    return null;
	}
	
	return new_clust;
    }
    
    private ProgressOMeter pm;
    private int new_clusters_read;

    public boolean loadClusterDataFromNativeFile(File file, ExprData.Cluster parent)
    {
	pm = new ProgressOMeter("Loading...", 2);
	pm.startIt();

	new_clusters_read = 0;
    
	new NativeClusterLoaderThread(file, parent).start();

	return true;
    }

    public class NativeClusterLoaderThread extends Thread
    {
	private File file;
	private ExprData.Cluster parent;
	
	public NativeClusterLoaderThread(File f, ExprData.Cluster p)
	{
	    file = f;
	    parent = p;
	}

	public void run() //boolean loadClusterDataFromTextFile(File file)
	{
	    try 
	    { 
		BufferedReader br = new BufferedReader(new FileReader(file));
		
		StreamTokenizer st = new StreamTokenizer(br);
		
		st.resetSyntax();
		st.eolIsSignificant(true);
		st.wordChars('!','~');  // this covers all printable ASCII chars
		st.whitespaceChars('<','<');
		st.whitespaceChars('>','>');
		
		ExprData.Cluster new_cl = recursivelyReadCluster(st);
		
		if(new_cl != null)
		{
		    new_cl.setName(file.getName());
		    edata.addChildToCluster(parent, new_cl);
		}
	    }
	    catch(FileNotFoundException fnfe)
	    { 
		pm.stopIt();
		mview.errorMessage("Cannot open file ' " + file.getName() + "' (not found)\n  " + fnfe);
		return;
	    }
	    catch (IOException ioe) 
	    { 
		pm.stopIt();
		mview.errorMessage("File ' " + file.getName() + "' broken\n  " + ioe);
		return;
	    }
	    
	    pm.stopIt();
	}
    }
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   command handlers
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void expandTree()
    {
	int i = 0;
	while(i < tree.getRowCount())
	{
	    tree.expandRow(i);
	    i++;
	}
    }

    public void collapseTree()
    {
	boolean any_open = true;

	// go backwards up the tree, closing as we go...
	//
	int rc = tree.getRowCount();
	while(rc > 0)
	{
	    if(tree.isExpanded(rc))
		tree.collapseRow(rc);
	    rc--;
	}

	/*
	while(any_open)
	{
	    any_open = false;
	    int ii = 0;
	    System.out.println(tree.getRowCount() + " rows in tree");
	    int shut_c = 0;
	    while(ii < tree.getRowCount())
	    {
		if(tree.isExpanded(ii))
		{
		   any_open = true; 
		   tree.collapseRow(ii);	
		   shut_c++;
		}
		ii++;
	    }
	    if(shut_c > 0)
		System.out.println(shut_c + " nodes closed");
	}
	*/
    }
    
    public void loadCluster()
    {
	final JLabel label1 = new JLabel("Insert where?");
	final JRadioButton root_jrb = new JRadioButton("Root");
	final JRadioButton sel_jrb = new JRadioButton("Selection");

	final JLabel label2 = new JLabel("Format?");
	final JRadioButton native_jrb = new JRadioButton("Native");
	final JRadioButton stanford_jrb = new JRadioButton("Stanford (.gtr)");
	final JRadioButton namelist_jrb = new JRadioButton("List of names");

	Font font = mview.getSmallFont();
	root_jrb.setFont(font);
	sel_jrb.setFont(font);
	label1.setFont(font);
	native_jrb.setFont(font);
	stanford_jrb.setFont(font);
	namelist_jrb.setFont(font);
	label2.setFont(font);
	
	if(current_cluster == null)
	    sel_jrb.setEnabled(false);

	JPanel opts_pan = new JPanel();
	opts_pan.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	opts_pan.setLayout(new BoxLayout(opts_pan, BoxLayout.Y_AXIS));
	opts_pan.add(label1);

	ButtonGroup bg = new ButtonGroup();
	
	root_jrb.setSelected(mview.getBooleanProperty("clustman.insert_root", true));
	bg.add(root_jrb);
	opts_pan.add(root_jrb);
	
	bg.add(sel_jrb);
	sel_jrb.setSelected(mview.getBooleanProperty("clustman.insert_selection", false));
	opts_pan.add(sel_jrb);
	
	bg = new ButtonGroup();
	opts_pan.add(label2);

	native_jrb.setSelected(true);
	native_jrb.setSelected(mview.getBooleanProperty("clustman.load_native", true));
	bg.add(native_jrb);
	opts_pan.add(native_jrb);
	
	bg.add(stanford_jrb);
	stanford_jrb.setSelected(mview.getBooleanProperty("clustman.load_stanford", false));
	opts_pan.add(stanford_jrb);
	
	bg.add(namelist_jrb);
	namelist_jrb.setSelected(mview.getBooleanProperty("clustman.load_lon", false));
	opts_pan.add(namelist_jrb);

	//In response to a button click:
	JFileChooser fc = new JFileChooser();
	fc.setCurrentDirectory(new File(mview.getProperty("clustman.load_path", System.getProperty("user.dir"))));
	
	int where = mview.getIntProperty("clustman.load_dest", 0);
	if((where == 1) && (current_cluster != null))
	    sel_jrb.setSelected(true); 
	
	fc.setAccessory(opts_pan);

	int returnVal = fc.showOpenDialog(frame);
	
	mview.putProperty("clustman.load_path", fc.getCurrentDirectory().getPath());
	mview.putBooleanProperty("clustman.insert_root", root_jrb.isSelected());
	mview.putBooleanProperty("clustman.insert_selection", sel_jrb.isSelected());
	mview.putBooleanProperty("clustman.load_native", native_jrb.isSelected());
	mview.putBooleanProperty("clustman.load_stanford", stanford_jrb.isSelected());
	mview.putBooleanProperty("clustman.load_lon", namelist_jrb.isSelected());


	if (returnVal == JFileChooser.APPROVE_OPTION) 
	{
	    File file = fc.getSelectedFile();
	    
	    ExprData.Cluster parent = edata.getRootCluster();
	    if(sel_jrb.isSelected())
	    {
		if(current_cluster == null)
		{
		    mview.alertMessage("No selected cluster");
		    return;
		}
		parent = current_cluster;
	    }
	    
	    if(stanford_jrb.isSelected())
		loadClusterDataFromStanfordFormatFile(file, parent);
	    else
	    {
		if(namelist_jrb.isSelected())
		    loadClusterDataFromTextFile(file, parent);
		else
		    loadClusterDataFromNativeFile(file, parent);
	    }

	}
    }

    public void createSaveClustersDialog()
    {
	final JLabel cl_label = new JLabel("Which Clusters?");
	final JRadioButton all_jrb = new JRadioButton("All of them");
	final JRadioButton vis_jrb = new JRadioButton("Visible ones");
	final JRadioButton cur_jrb = new JRadioButton("Selection");
	final JCheckBox chi_jchkb = new JCheckBox("Include children");

	final JRadioButton nat_jrb = new JRadioButton("Native");
	final JRadioButton lon_jrb = new JRadioButton("List of Names");
	final JLabel fo_label = new JLabel("Format?");

	Font font = mview.getSmallFont();
	cl_label.setFont(font);
	all_jrb.setFont(font);
	vis_jrb.setFont(font);
	cur_jrb.setFont(font);
	chi_jchkb.setFont(font);
	nat_jrb.setFont(font);
	lon_jrb.setFont(font);
	fo_label.setFont(font);

	JPanel opts_pan = new JPanel();
	opts_pan.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	opts_pan.setLayout(new BoxLayout(opts_pan, BoxLayout.Y_AXIS));
	opts_pan.add(cl_label);

	ButtonGroup bg = new ButtonGroup();
	
	all_jrb.setSelected(true);
	bg.add(all_jrb);
	opts_pan.add(all_jrb);
	
	bg.add(vis_jrb);
	opts_pan.add(vis_jrb);
	
	bg.add(cur_jrb);
	opts_pan.add(cur_jrb);
	
	chi_jchkb.setSelected(mview.getBooleanProperty("clustman.save_children", true));
	opts_pan.add(chi_jchkb);
	
	int what = mview.getIntProperty("clustman.save_src", 0);
	if(what == 1)
	    vis_jrb.setSelected(true); 
	if((what == 2) && (current_cluster != null))
	    cur_jrb.setSelected(true); 
	
	opts_pan.add(fo_label);

	bg = new ButtonGroup();
	nat_jrb.setSelected(mview.getBooleanProperty("clustman.native_format", true));
	bg.add(nat_jrb);
	opts_pan.add(nat_jrb);
	
	lon_jrb.setSelected(mview.getBooleanProperty("clustman.lon_format", true));
	bg.add(lon_jrb);
	opts_pan.add(lon_jrb);

	JFileChooser jfc = new JFileChooser();
	jfc.setAccessory(opts_pan);
	String dld = mview.getProperty("clustman.save_path", null);
	if(dld != null)
	{
	    File ftmp = new File(dld);
	    jfc.setCurrentDirectory(ftmp);
	}
	
	int ret_val =  jfc.showSaveDialog(mview.getDataPlot()); 

	mview.putProperty("clustman.save_path",jfc.getCurrentDirectory().getPath());
	int mode = 0;
	if(vis_jrb.isSelected())
	    mode = 1;
	if(cur_jrb.isSelected())
	    mode = 2;
	mview.putIntProperty("clustman.save_src", mode);

	mview.putBooleanProperty("clustman.save_children", chi_jchkb.isSelected());

	mview.putBooleanProperty("clustman.native_format", nat_jrb.isSelected());
	mview.putBooleanProperty("clustman.lon_format", lon_jrb.isSelected());

	if(ret_val == JFileChooser.APPROVE_OPTION) 
	{
	    File file = jfc.getSelectedFile();
	    if(nat_jrb.isSelected())
		saveClustersNative(file, mode, chi_jchkb.isSelected());
	    else
		saveClustersListOfNames(file, mode, chi_jchkb.isSelected());
	}
    }


    // ============================================================
 
    // collapse means move all of the spots from of it's children into itself
    //
    public void collapseCluster()
    {
	if(current_cluster == null)
	{
	    mview.alertMessage("No cluster selected");
	    return;
	}
	if(current_cluster.getParent() == null)
	{
	    mview.alertMessage("Cannot collapse the root cluster");
	    return;
	}
	// make sure all element name types are the same
	
	int new_n_spots = getNumSpots( current_cluster );

	// System.out.println(current_cluster.getName() + " has " + new_n_spots + " total spots");

	Vector new_spot_id = new Vector();

	addSpotsToVector( current_cluster, new_spot_id );

	// System.out.println(current_cluster.getName() + " found " +  new_spot_id.size() + " spots");

	if(current_cluster.getNumChildren() > 0)
	{
	    Vector clc = (Vector) current_cluster.getChildren().clone();
	    for(int c=0; c < clc.size(); c++)
		edata.deleteCluster( (ExprData.Cluster) clc.elementAt(c) );
	}
	
	edata.setClusterElements(current_cluster, ExprData.SpotIndex,  new_spot_id);
    }

    public void reparentCluster()
    {
	if(current_cluster == null)
	{
	    mview.alertMessage("No cluster selected");
	    return;
	}
	if(current_cluster.getParent() == null)
	{
	    mview.alertMessage("Cannot re-parent the root cluster");
	    return;
	}
	try
	{
	    ExprData.Cluster cl = mview.getCluster("New parent for '" + current_cluster.getName() + "'", current_cluster);

	    if(cl == current_cluster)
	    {
		mview.alertMessage("Cannot re-parent a cluster to itself");
		return;
	    }
	    if(cl != null)
	    {
		// System.out.println( "reparenting " + current_cluster.getName() + " to " + cl.getName() );
	    
		current_cluster.getParent().removeCluster(current_cluster);
		cl.addCluster( current_cluster );
		edata.generateClusterUpdate( ExprData.OrderChanged );
	    }
	}
	catch(UserInputCancelled uic)
	{

	}
    }


    /*
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
	private Color glyph_colour;

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
		ExprData.Cluster cl =  (ExprData.Cluster) node.getUserObject();

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
    */


    // -------------------------------------------------------------------

    private void addSpotsToVector( ExprData.Cluster cl, Vector spot_id )
    {
	if(cl.getElements() != null)
	{
	    int[] s_ids =  cl.getElements();
	    for(int s = 0; s < s_ids.length; s++)
		spot_id.addElement( new Integer( s_ids[s] ));
	}
	
	if(cl.getNumChildren() > 0)
	{
	    Vector clc = cl.getChildren();
	    for(int c=0; c < clc.size(); c++)
		addSpotsToVector( ( (ExprData.Cluster) clc.elementAt(c) ), spot_id );
	}
    }

    private int getNumSpots( ExprData.Cluster cl )
    {
	int total_spots = 0;

	if(cl.getElements() != null)
	{
	    total_spots += cl.getElements().length;
	}
	
	if(cl.getNumChildren() > 0)
	{
	    Vector clc = cl.getChildren();
	    for(int c=0; c < clc.size(); c++)
		total_spots += getNumSpots( ( (ExprData.Cluster) clc.elementAt(c) ) );
	}

	return total_spots;
    }

    // ============================================================
 
    public void mergeClusters()
    {

    }

    public void findCluster()
    {
	try
	{
	    String name = mview.getString("Cluster name");
	    
	    displayCluster(name);
	}
	catch(UserInputCancelled e)
	{
	}
    }

    public void displayCluster(String name)
    {
	// System.out.println("displayCluster() name search...");
	
	if((name != null) && (name.length() > 0))
	{
	    // search all of the DefaultMutableTreeNodes...
	    //
	    for (Enumeration e = tree_top.depthFirstEnumeration(); e.hasMoreElements() ;) 
	    {
		DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) e.nextElement();
		ExprData.Cluster cli = (ExprData.Cluster) dmtn.getUserObject();
		
		if(cli.getName().startsWith(name))
		{
		    // get the path to this node
		    //
		    
		    TreeNode[] tn_path = tree_model.getPathToRoot(dmtn);
		    
		    TreePath tp = new TreePath(tn_path);
		    
		    tree.expandPath(tp);
		    tree.scrollPathToVisible(tp);
		    tree.setSelectionPath(tp);

		    if(cli.getName().equals(name.trim()))
			// this is an exact match, don't display any more...
			return;
		}
	    }
	}
    }

    public void displayCluster(ExprData.Cluster cl)
    {
	if(cl == null)
	    return;

	// System.out.println("displayCluster() exact search...");

	// search all of the DefaultMutableTreeNodes...
	//
	for (Enumeration e = tree_top.depthFirstEnumeration(); e.hasMoreElements() ;) 
	{
	    DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) e.nextElement();
	    ExprData.Cluster cli = (ExprData.Cluster) dmtn.getUserObject();
	    
	    if(cli == cl)
	    {
		// get the path to this node
		//
		// System.out.println("displayCluster() found...");

		TreeNode[] tn_path = tree_model.getPathToRoot(dmtn);
		
		TreePath tp = new TreePath(tn_path);
		
		tree.expandPath(tp);
		tree.scrollPathToVisible(tp);
		tree.setSelectionPath(tp);

		return;
	    }
	}

	// System.out.println("displayCluster() NOT found...");
    }
    
    public void deleteCluster()
    {
	if(current_cluster == null)
	    return;
	edata.deleteCluster(current_cluster);
    }
 
    public void createClusterFromFilter()
    {
	Vector spots = new Vector();

	String name = null;
	
	for(int s=0; s < edata.getNumSpots(); s++)
	{
	    if(!edata.filter(s))
	    {
		spots.addElement(edata.getSpotName(s));
	    }
	}
	name = (spots.size() + " Filtered Spots");

	if(spots.size() > 0)
	{
		ExprData.Cluster cl = edata.new Cluster(name, ExprData.SpotName, spots);
		edata.addCluster(cl);
	}
	else
	{
	    mview.alertMessage("New cluster has no elements (not created)");
	}
    }

    public void createEmptyRootCluster()
    {
	try
	{
	    String name = mview.getString("Name for new cluster");
	    ExprData.Cluster cl = edata.new Cluster(name, ExprData.SpotName, null);
	    edata.addCluster(cl);
	}
	catch(UserInputCancelled uic)
	{
	}
    }

    public void createClusterFromIntersection()
    {
	Vector spots = new Vector();

	String name = null;

	// visit each of the visible clusters that have spots or measurements
	//
	//   start by assuming the answer is 'all the E in the first cluster'
	//   then remove any E in which not also present in all other clusters
	//   (where is spot or measurement)
	
	ExprData.ClusterIterator clit = edata.new ClusterIterator();
	ExprData.Cluster clust = clit.getCurrent();
	
	
	boolean first = true;
	boolean first_is_spot = true;
	int[] current_set = null;
	int n_vis = 0;
	String names = "";
	while(clust != null)
	{
	    if(clust.getShow() && (clust.getParent() != null))
	    {
		int[] els = clust.getElements();
		if(els != null)
		{
		    if(n_vis > 0)
			names += ", ";
		    if(n_vis < 6)
			names += clust.getName();
		    n_vis++;
		    
		    if(first)
		    {
			first = false;
			current_set = (int[]) els.clone();
			first_is_spot = clust.getIsSpot();
				//System.out.println("first is " + 
				//		   clust.getName() + " with " + 
				//		   current_set.length + " spots");
		    }
		    else
		    {
			// if this cluster is the same type as the first cluster,
			// remove any elements in current_set which are
			// not present in this cluster
			
			if(clust.getIsSpot() == first_is_spot)
			{
			    for(int t=0; t < current_set.length; t++)
			    {
				if(current_set[t] >= 0) // -1 used to indicate 'gone'
				{
				    if(containsElement(current_set[t], els) == false)
				    {
					current_set[t] = -1;
				    }
				}
			    }
			}
		    }
		} // ...if(el != null)
	    }
	    clust = clit.getNext();
	}
	
	// is anything left in current_set?
	
	if(current_set == null)
	{
	    mview.alertMessage("No visible clusters in intersection, new cluster not created");
	    return;
	}

	for(int t=0; t < current_set.length; t++)
	{
	    if(current_set[t] >= 0)
	    {
		spots.addElement(edata.getSpotName( current_set[t] ));
	    }
	}
	
	name = "Intersection of ";
	if(n_vis < 6)
	{
	    name += names;
	}
	else
	{
	    name += (n_vis + " other clusters");
	}
	

	if(spots.size() > 0)
	{
		ExprData.Cluster cl = edata.new Cluster(name, ExprData.SpotName, spots);
		edata.addCluster(cl);
	}
	else
	{
	    mview.alertMessage("New cluster has no elements (not created)");
	}
    }

    public void createClusterFromUnion()
    {
	
	String name = null;

	boolean[] union_els = null;

	boolean first = true;
	boolean first_is_spot = true;
	
	//spots = new boolean[edata.getNumSpots()];
	//for(int s=0; s < edata.getNumSpots(); s++)
	//    union_spots[s] = false;
	
	ExprData.ClusterIterator clit = edata.new ClusterIterator();
	ExprData.Cluster clust = clit.getCurrent();

	int n_vis = 0;
	String names = "";

	while(clust != null)
	{
	    boolean valid = false;
	    
	    if(clust.getShow() && (clust.getParent() != null))   // remember to ignore the root....
	    {
		//		System.out.println(clust.getName() + " visible");

		if(first)
		{
		    // System.out.println(clust.getName() + " first visible");
		
		    first_is_spot = clust.getIsSpot();
		    first = false;
		    if(first_is_spot)
		    {
			union_els= new boolean[edata.getNumSpots()];
		    }
		    else
		    {
			union_els= new boolean[edata.getNumMeasurements()];
		    }
		    for(int u=0; u < union_els.length; u++)
			union_els[u] = false;

		    valid = true;
		}
		else
		{
		    valid = (first_is_spot == clust.getIsSpot());
		}

		if(valid)
		{
		    // System.out.println(clust.getName() + " valid");

		    int[] els = clust.getElements();
		    if(els != null)
		    {
			if(n_vis > 0)
			    names += ", ";
			if(n_vis < 6)
			    names += clust.getName();
			n_vis++;
		    
			for(int el=0; el < els.length; el++)
			    union_els[els[el]] = true;
		    }
		}
	    }
	    clust = clit.getNext();
	}

	if(union_els == null)
	{
	    mview.alertMessage("No visible clusters in union, new cluster not created");
	    return;
	}

	Vector els = new Vector();

	for(int u=0; u < union_els.length; u++)
	{
	    if(union_els[u] == true)
		els.addElement( new Integer(u) );
	}
	name = "Union of ";
	if(n_vis < 6)
	{
	    name += names;
	}
	else
	{
	    name += (n_vis + " other clusters");
	}

	if(els.size() > 0)
	{
		ExprData.Cluster cl = edata.new Cluster(name, 
							first_is_spot ? ExprData.SpotIndex :  ExprData.MeasurementIndex, 
							els);
		edata.addCluster(cl);
	}
	else
	{
	    mview.alertMessage("New cluster has no elements (not created)");
	}
    }

    //
    // mode 0 -> spots
    //      1 -> measurements
    //      2 -> both

    public void createClusterFromSelection( int mode )
    {
	if((mode == 0) || (mode == 2))
	    createClusterFromSpotSelection();
	if((mode == 1) || (mode == 2))
	    createClusterFromMeasurementSelection();
    }
    public void createClusterFromSpotSelection()
    {
	// only spots can be selected....
	int[] s_ids_a = edata.getSpotSelection();
	if((s_ids_a == null) || (s_ids_a.length == 0))
	{
	    mview.alertMessage("No Spots are selected, new Cluster not created");
	}    
	else
	{
	    Vector s_ids_v = new Vector();
	    for(int s=0; s < s_ids_a.length; s++)
		s_ids_v.addElement( edata.getSpotName(  s_ids_a[s] ) );
	    
	    String name = (s_ids_a.length > 1) ? (s_ids_a.length + " selected Spots") : "1 selected Spot";
	    ExprData.Cluster cl = edata.new Cluster(name, ExprData.SpotName, s_ids_v );
	    edata.addCluster(cl);
	    // System.out.println("new cl has " + s_ids_a.length + " spots");
	}

    }

    public void createClusterFromMeasurementSelection()
    {
	
	int[] m_ids_a = edata.getMeasurementSelection();
	if((m_ids_a == null) || (m_ids_a.length == 0))
	{
	    mview.alertMessage("No Measurements are selected, new Cluster not created");
	}    
	else
	{
	    Vector m_ids_v = new Vector();
	    for(int m=0; m < m_ids_a.length; m++)
		m_ids_v.addElement( edata.getMeasurementName( m_ids_a[m] ) );
	    String name = (m_ids_a.length > 1) ? (m_ids_a.length + " selected Measurements") : "1 selected Measurement";
	    ExprData.Cluster cl = edata.new Cluster(name, ExprData.MeasurementName, m_ids_v );
	    edata.addCluster(cl);
	    // System.out.println("new cl has " + s_ids_a.length + " spots");
	}
    }


    public void createCluster()
    {
	try
	{
	    String[] opts = { "... new empty child of Root",
			      "... from filtered Spots", 
			      "... from selected Spots",
			      "... from selected Measurements",
			      "... from the union of visible clusters",
			      "... from the intersection of visible clusters" };
	    int c = mview.getChoice("Create cluster ...", opts);

	    // ====================================
	    // empty

	    if(c == 0)
	    {
		createEmptyRootCluster();
	    }

	    // ====================================
	    // filter

	    if(c == 1)
	    {
		createClusterFromFilter();
	    }

	    // ====================================
	    // selected spots

	    if(c == 2)
	    {
		createClusterFromSelection(0);
	    }

	    // ====================================
	    // selected meas

	    if(c == 3)
	    {
		createClusterFromSelection(1);
	    }

	    // ====================================
	    // union

	    if(c == 4)
	    {
		createClusterFromUnion();
	    }

	    // ====================================
	    // intersection

	    if(c == 5)
	    {
		createClusterFromIntersection();
	    }
	}
	catch(UserInputCancelled uic)
	{
	}
    }

    private boolean containsElement(int id, int[] e_a )
    {
	for(int e=0; e < e_a.length; e++)
	{
	    if(e_a[e] == id)
		return true;
	}
	return false;
    }

    public void setClusterGlyph(int g)
    {
	if(current_cluster != null)
	{
	    if(current_cluster.getChildren() == null)
	    {
		// this is a leaf, no cycle mode to worry about
		//
		edata.setClusterGlyph(current_cluster, g);
	    }
	    else
	    {
		// is the selection cycle mode?
		if(g == 0)
		    edata.setClusterGlyphCycle(current_cluster, true);
		else
		    edata.setClusterGlyph(current_cluster, g-1);
	    }
	}
    }

    public void setClusterColour(Color c)
    {
	if(current_cluster != null)
	{
	    edata.setClusterColour(current_cluster, c);
	}

    }

    public void toggleClusterShow()
    {
	if(current_cluster != null)
	{
	    //if(current_cluster.getChildren() == null)
	    {
		edata.toggleClusterShow(current_cluster);
	    }
	    //else
	    {
		//edata.clusterShowGroup(current_cluster, !current_cluster.getShow());
	    }
	}
    }
    
    public void setShowAll(boolean spots, boolean show)
    {
	if(current_cluster != null)
	{
	    if(spots)
		edata.clusterShowAllSpots(current_cluster, show);
	    else
		edata.clusterShowAllMeasurements(current_cluster, show);
	}
    }

    public void setShowAll(boolean show)
    {
	if(current_cluster != null)
	    edata.clusterShowAll(current_cluster, show);
    }

    public void setShowGroup(boolean show)
    {
	if(current_cluster != null)
	{
	    edata.clusterShowGroup(current_cluster, show);
	}
    }

    public void setCycleSpeed(int delay_)
    {
	timer_delay = delay_;
	if(timer == null)
	{	    
	    timer = new Timer(timer_delay, new ActionListener() 
		{
		    public void actionPerformed(ActionEvent evt) 
		    {
			cycleNextCluster();
		    }    
		});
	}
	else
	    timer.setDelay(timer_delay);
    }

    public void startStopCycleClusters()
    {
	if(timer == null)
	{	    
	    timer = new Timer(timer_delay, new ActionListener() 
			      {
				  public void actionPerformed(ActionEvent evt) 
				  {
				      cycleNextCluster();
				  }    
			      });
	}
	
	if(timer.isRunning())
	{
	    timer.stop();
	    //auto_cycle_button.setText("Start");
	}
	else
	{
	    if(cycle_descend)
	    {
		if(cycle_iterator == null)
		{	
		    cycle_iterator = edata.new ClusterIterator(false);
		    cycle_clust = cycle_iterator.getCurrent();
		}
		/*
	    else
	    cycleNextCluster();
		*/
		
		if(current_cluster != null)
		{
		    // move the iterator to the user's selection
		    cycle_iterator.positionAt(current_cluster);
		}
	    }
	    else
	    {
		cycle_current_child = 0;
		if(current_cluster.getNumChildren() > 0)
		    cycle_clust = (ExprData.Cluster) current_cluster.getChildren().elementAt(cycle_current_child);
		else
		    cycle_clust = null;

		// System.out.println("start at child " + cycle_current_child + " of " + current_cluster.getName());
	    }

	    // System.out.println("startStopCycleClusters(): ready to start from "  + current_cluster);

	    timer.start();
	    //auto_cycle_button.setText("Stop");

	    setShowAll(false);

	    if(cycle_clust != null)
	    {
		if(cycle_clust.getShow() != true)
		{
		    edata.setClusterShow(cycle_clust, true);
		}
	    }
	}
    }
    public void cycleNextCluster()
    {
	// advance forward, ignoring any group clusters
	//

	//while((cycle_clust != null) && (cycle_clust.getElements() == null))
	//    cycle_clust = cycle_iterator.getNext();
	
	if(cycle_descend)
	{
	    cycle_clust = cycle_iterator.getNextLeaf();

	    // when we get to the end, start again
	    //
	    if(cycle_clust == null)
		cycle_clust = cycle_iterator.reset();
	}
	else
	{
	    if(cycle_clust != null)
	    {
		ExprData.Cluster parent = cycle_clust.getParent();
		if(parent != null)
		{
		    //System.out.println("current at child " + cycle_current_child + " of " + parent.getName());

		    if((cycle_current_child+1) < parent.getNumChildren())
		    {
			cycle_current_child++;
		    }
		    else
		    {
			cycle_current_child = 0;
		    }
		    
		    cycle_clust = (ExprData.Cluster) parent.getChildren().elementAt(cycle_current_child);
		}
	    }
	}

	if(timer.isRunning() == false)
	    timer.start();
	
	setShowAll(false);

	if(cycle_clust != null)
	{
	    if(cycle_clust.getShow() != true)
	    {
		// System.out.println("called showGroup for " + cycle_clust.getName());

		edata.clusterShowGroup(cycle_clust, true);
		// edata.setClusterShow(cycle_clust, true);
	    }
	}
    }

    private void setColourRamp()
    {
	
    }

    private void startColorChooser()
    {
	if(current_cluster == null)
	    return;

	if(current_cluster.getNumChildren() == 0)
	{
	    Color new_colour = JColorChooser.showDialog(frame,
							"Choose Colour",
							current_cluster.getColour());
	    if (new_colour != null) 
	    {
		edata.setClusterColour(current_cluster, new_colour);
	    }
	}
	else
	{
	    new ColourSetChooser(mview, edata, current_cluster);
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   buildComponents()
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  


    public void buildComponents(JPanel top_panel)
    {

	GridBagLayout gridbag = new GridBagLayout();
	top_panel.setLayout(gridbag);

	// --- === --- === --- === --- === --- === --- === --- === --- === --- === --- === 
	//
	// first, the cluster selection tree
	//
	// --- === --- === --- === --- === --- === --- === --- === --- === --- === --- === 

	if(tree_top != null)
	{
	    tree_model = new DefaultTreeModel(tree_top);
	    
	    tree = new DragAndDropTree(tree_model);
	}
	else
	    tree = new DragAndDropTree();

	/*
	tree.setEntityAdaptor(new DragAndDropTree.EntityAdaptor()
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
	*/

	tree.setDropAction(new DragAndDropTree.DropAction()
	    {
		public void dropped(DragAndDropEntity dnde)
		{
		    try
		    {
			displayCluster(dnde.getCluster());
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
		    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
		    
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

	//tree_bg_col = top_panel.getBackground();

	tree_bg_col = mview.intToColor(mview.getIntProperty("clustman.colour", 100));

	tree.setBackground(tree_bg_col);
	tree_grey_level = tree_bg_col.getRed();

 	tree.setRootVisible(false);
	tree.setEditable(false);
	
	tree.setCellRenderer(new CustomTreeCellRenderer());

	// listen for when the selection changes.
	tree.addTreeSelectionListener(new TreeSelectionListener() 
	    {
		public void valueChanged(TreeSelectionEvent e) 
		{
		    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
		    
		    if (node == null) 
			return;
		    
		    clusterHasBeenSelected(node);
		}
	    });
	
	MouseListener ml = new MouseAdapter() 
	    {
		public void mouseClicked(MouseEvent e) 
		{
		    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
		    if(node != null) 
		    {
			if(e.getClickCount() == 2) 
			{
			    // System.out.println("DoubleClick at " + selRow + " - " + selPath);
			    clusterHasBeenToggled(node);
			}
		    }	
		}
	    };
	tree.addMouseListener(ml);
	
	tree.putClientProperty("JTree.lineStyle", "Angled");

	//DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
	//renderer.setLeafIcon(new ImageIcon("images/cluster.gif"));
	//tree.setCellRenderer(renderer);
	
	// embed the tree in a scroll pane
	JScrollPane tree_view = new JScrollPane(tree);
	
	ToolTipManager.sharedInstance().registerComponent(tree);

	// --- === --- === --- === --- === --- === --- === --- === --- === --- === --- === 
	//
	// the cluster viewing area 
	//   (shows the names of things in a cluster)
	//
	// --- === --- === --- === --- === --- === --- === --- === --- === --- === --- === 
	
	view_panel = new JTextPane();
	view_panel.setBackground(top_panel.getBackground());

	//view_panel.setText("this is a text");
	view_panel.setEditable(false);
	// embed the text viewer in a scroll pane
	JScrollPane view_scroller = new JScrollPane(view_panel);

	// --- === --- === --- === --- === --- === --- === --- === --- === --- === --- === 
	//
	// the attribute control panel area
	//   (controls for colours, symbols etc associated with a cluster)
	//
	// --- === --- === --- === --- === --- === --- === --- === --- === --- === --- === 

	{
	    att_panel = new JPanel();

	    att_panel.setMinimumSize(new Dimension(50, 50));
	    att_panel.setPreferredSize(new Dimension(150, 300));

	    GridBagLayout inner_gridbag = new GridBagLayout();
	    att_panel.setLayout(inner_gridbag);

	    shown_glyph_images = new ImageIcon[edata.n_glyph_types];
	    //hidden_glyph_images = new ImageIcon[edata.n_glyph_types];
	    for (int i = 0; i < edata.n_glyph_types; i++) 
	    {
		shown_glyph_images[i]  = new ImageIcon(mview.getImageDirectory() + "shown-glyph-" + i + ".gif");
		//hidden_glyph_images[i] = new ImageIcon("images/hidden-glyph-" + i + ".gif");
		
		// System.out.println(shown_glyph_images[i]);
	    }
	    cycle_glyph_image = new ImageIcon(mview.getImageDirectory() + "cycle.gif");
	    
	    int line = 0;

	    {
		int iline = 0;
		JPanel wrapper = new JPanel();
		wrapper.setBorder(BorderFactory.createEtchedBorder());
		GridBagLayout igridbag = new GridBagLayout();
		wrapper.setLayout(igridbag);
		
		/*
		JLabel label = new JLabel("Name");
		GridBagConstraints c2 = new GridBagConstraints();
		c2.gridx = 0;
		c2.gridy = iline++;
		c2.gridwidth = 2;
		c2.anchor = GridBagConstraints.SOUTH;
		igridbag.setConstraints(label, c2);
		wrapper.add(label);
		*/

		clust_name_jtf = new JTextField(20);
		clust_name_jtf.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    if(current_cluster != null)
				current_cluster.setName(clust_name_jtf.getText());
			    tree.repaint();
			    updateControls();
			}
		    });
		GridBagConstraints c2 = new GridBagConstraints();
		c2.gridx = 0;
		c2.gridy = iline++;
		c2.gridwidth = 2;
		c2.weightx = 4.0; 
		c2.fill = GridBagConstraints.HORIZONTAL;
		igridbag.setConstraints(clust_name_jtf, c2);
		wrapper.add(clust_name_jtf);


		JLabel label = new JLabel("Show  ");
		c2 = new GridBagConstraints();
		c2.gridx = 0;
		c2.gridy = iline;
		c2.weightx = 2.0;
		c2.anchor = GridBagConstraints.EAST;
		igridbag.setConstraints(label, c2);
		wrapper.add(label);
		

		show_jchb = new JCheckBox();
 		show_jchb.setToolTipText("Show or hide this cluster");
		show_jchb.addActionListener(new ActionListener()
					  {
					      public void actionPerformed(ActionEvent e) 
					      {
						  toggleClusterShow();
					      }
					  });

		c2 = new GridBagConstraints();
		c2.gridx = 1;
		c2.gridy = iline++;
		c2.weightx = 2.0;
		c2.anchor = GridBagConstraints.WEST;
		igridbag.setConstraints(show_jchb, c2);
		wrapper.add(show_jchb);
		

		colour_label = new JLabel("Colour  ");
		c2 = new GridBagConstraints();
		c2.gridx = 0;
		c2.gridy = iline;
		c2.weightx = 2.0;
		c2.anchor = GridBagConstraints.EAST;
		igridbag.setConstraints(colour_label, c2);
		wrapper.add(colour_label);

		
		colour_jb = new JButton();
		colour_jb.addActionListener(new ActionListener()
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    startColorChooser();
			}
		    });
		
		c2 = new GridBagConstraints();
		c2.gridx = 1;
		c2.gridy = iline++;
		c2.weightx = 2.0;
		c2.anchor = GridBagConstraints.WEST;
		igridbag.setConstraints(colour_jb, c2);
		wrapper.add(colour_jb);
		

		glyph_label = new JLabel("Glyph  ");
		c2 = new GridBagConstraints();
		c2.gridx = 0;
		c2.gridy = iline;
		c2.weightx = 2.0;
		c2.anchor = GridBagConstraints.EAST;
		igridbag.setConstraints(glyph_label, c2);
		wrapper.add(glyph_label);
		

		glyph_jcb = new JComboBox();  // shown_glyph_images
		glyph_jcb.setToolTipText("Select the glyph for this cluster");
		glyph_al = new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			JComboBox cb = (JComboBox)e.getSource();
			int new_val = cb.getSelectedIndex();
			setClusterGlyph(new_val);
		    }
		};
     		glyph_jcb.addActionListener(glyph_al);
		c2 = new GridBagConstraints();
		c2.gridx = 1;
		c2.gridy = iline++;
		c2.weightx = 2.0;
		c2.anchor = GridBagConstraints.WEST;
		igridbag.setConstraints(glyph_jcb, c2);
		wrapper.add(glyph_jcb);

		c2 = new GridBagConstraints();
		c2.gridx = 0;
		c2.gridy = line;
		c2.fill = GridBagConstraints.HORIZONTAL;
		c2.weightx = 5.0;
		inner_gridbag.setConstraints(wrapper, c2);
		att_panel.add(wrapper);
	    }
	    line++;

	    // ========================================================
	    
	    {
		JPanel wrapper = new JPanel();
		wrapper.setBorder(BorderFactory.createEtchedBorder());
		GridBagLayout igridbag = new GridBagLayout();
		wrapper.setLayout(igridbag);
		
		{
		    JLabel label = new JLabel("Selection");
		    label.setFont(mview.getSmallFont());
		    GridBagConstraints c2 = new GridBagConstraints();
		    c2.gridx = 0;
		    c2.gridy = 0;
		    c2.gridwidth = 0;
		    //c2.anchor = GridBagConstraints.SOUTH;
		    igridbag.setConstraints(label, c2);
		    wrapper.add(label);


		    group_label = new JLabel("[root]");
		    group_label.setForeground(label.getForeground().darker());
		    c2 = new GridBagConstraints();
		    c2.gridx = 0;
		    c2.gridy = 1;
		    c2.gridwidth = 2;
		    c2.weightx = 1.0;
		    //c2.anchor = GridBagConstraints.SOUTH;
		    igridbag.setConstraints(group_label, c2);
		    wrapper.add(group_label);
		

		    hide_group_jb = new JButton("Hide");
		    //hide_group_jb.setEnabled(false);
		    hide_group_jb.setToolTipText("Hide the selected cluster and it's children");
		    hide_group_jb.addActionListener(new ActionListener()
					    {
						public void actionPerformed(ActionEvent e) 
						{
						    setShowGroup(false);
						}
					    });
		    c2 = new GridBagConstraints();
		    c2.gridx = 0;
		    c2.gridy = 2;
		    c2.anchor = GridBagConstraints.EAST;
		    c2.weightx = 1.0;
		    igridbag.setConstraints(hide_group_jb, c2);
		    wrapper.add(hide_group_jb);

		    show_group_jb = new JButton("Show");
		    //jb.setPreferredSize(new Dimension(50,20));
		    //show_group_jb.setEnabled(false);
		    //show_group_jb.setVisible(false);
		    show_group_jb.setToolTipText("Show the selected cluster and it's children");
		    show_group_jb.addActionListener(new ActionListener()
			{
			    public void actionPerformed(ActionEvent e) 
			    {
				setShowGroup(true);
			    }
			});
		    
		    c2 = new GridBagConstraints();
		    c2.gridx = 1;
		    c2.gridy = 2;
		    c2.anchor = GridBagConstraints.WEST;
		    c2.weightx = 1.0;
		    igridbag.setConstraints(show_group_jb, c2);
		    wrapper.add(show_group_jb);
		}

		GridBagConstraints c2 = new GridBagConstraints();
		c2.gridx = 0;
		c2.gridy = line;
		c2.fill = GridBagConstraints.HORIZONTAL;
		c2.weightx = 5.0;
		inner_gridbag.setConstraints(wrapper, c2);
		att_panel.add(wrapper);
	    }
	    line++;

	    // ========================================================

	    {
		JPanel wrapper = new JPanel();
		wrapper.setBorder(BorderFactory.createEtchedBorder());
		GridBagLayout igridbag = new GridBagLayout();
		wrapper.setLayout(igridbag);
		

		JLabel label = new JLabel("Spot Clusters");
		label.setFont(mview.getSmallFont());
		GridBagConstraints c2 = new GridBagConstraints();
		c2.gridx = 0;
		c2.gridy = 0;
		c2.gridwidth = 2;
		c2.anchor = GridBagConstraints.SOUTH;
		//c2.weightx = c2.weighty = 1.0;
		igridbag.setConstraints(label, c2);
		wrapper.add(label);
		

		JButton hide_all_jb = new JButton("Hide");
		hide_all_jb.setFont(mview.getSmallFont());
		hide_all_jb.setToolTipText("Hide all Spot clusters (except the selected one)");
		hide_all_jb.addActionListener(new ActionListener()
					    {
						public void actionPerformed(ActionEvent e) 
						{
						    setShowAll(true, false);
						}
					    });
		
		c2 = new GridBagConstraints();
		c2.gridx = 0;
		c2.gridy = 1;
		c2.anchor = GridBagConstraints.EAST;
		c2.weightx = 1.0;
		igridbag.setConstraints(hide_all_jb, c2);
		wrapper.add(hide_all_jb);


		JButton show_all_jb = new JButton("Show");
		show_all_jb.setFont(mview.getSmallFont());
		show_all_jb.setToolTipText("Show all Spot clusters");
		show_all_jb.addActionListener(new ActionListener()
					    {
						public void actionPerformed(ActionEvent e) 
						{
						    setShowAll(true, true);
						}
					    });
		
		c2 = new GridBagConstraints();
		c2.gridx = 1;
		c2.gridy = 1;
		c2.anchor = GridBagConstraints.WEST;
		c2.weightx = 1.0;
		igridbag.setConstraints(show_all_jb, c2);
		wrapper.add(show_all_jb);

		
		c2 = new GridBagConstraints();
		c2.gridx = 0;
		c2.gridy = line;
		c2.fill = GridBagConstraints.HORIZONTAL;
		c2.weightx = 5.0;
		inner_gridbag.setConstraints(wrapper, c2);
		att_panel.add(wrapper);
	    }
	    line++;

	    // ========================================================

	    {
		JPanel wrapper = new JPanel();
		wrapper.setBorder(BorderFactory.createEtchedBorder());
		GridBagLayout igridbag = new GridBagLayout();
		wrapper.setLayout(igridbag);
		

		JLabel label = new JLabel("Measurement Clusters");
		label.setFont(mview.getSmallFont());
		GridBagConstraints c2 = new GridBagConstraints();
		c2.gridx = 0;
		c2.gridy = 0;
		c2.gridwidth = 2;
		c2.anchor = GridBagConstraints.SOUTH;
		//c2.weightx = c2.weighty = 1.0;
		igridbag.setConstraints(label, c2);
		wrapper.add(label);
		

		hide_all_jb = new JButton("Hide");
		hide_all_jb.setToolTipText("Hide all Measurement clusters (except the selected one)");
		hide_all_jb.addActionListener(new ActionListener()
					    {
						public void actionPerformed(ActionEvent e) 
						{
						    setShowAll(false, false);
						}
					    });
		
		c2 = new GridBagConstraints();
		c2.gridx = 0;
		c2.gridy = 1;
		c2.anchor = GridBagConstraints.EAST;
		c2.weightx = 1.0;
		igridbag.setConstraints(hide_all_jb, c2);
		wrapper.add(hide_all_jb);


		show_all_jb = new JButton("Show");
		show_all_jb.setToolTipText("Show all Measurement clusters");
		show_all_jb.addActionListener(new ActionListener()
					    {
						public void actionPerformed(ActionEvent e) 
						{
						    setShowAll(false, true);
						}
					    });
		
		c2 = new GridBagConstraints();
		c2.gridx = 1;
		c2.gridy = 1;
		c2.anchor = GridBagConstraints.WEST;
		c2.weightx = 1.0;
		igridbag.setConstraints(show_all_jb, c2);
		wrapper.add(show_all_jb);

		
		c2 = new GridBagConstraints();
		c2.gridx = 0;
		c2.gridy = line;
		c2.fill = GridBagConstraints.HORIZONTAL;
		c2.weightx = 5.0;
		inner_gridbag.setConstraints(wrapper, c2);
		att_panel.add(wrapper);
	    }
	    line++;


	    Font small_font = mview.getSmallFont();

	    show_group_jb.setFont(small_font);
	    hide_group_jb.setFont(small_font);
	    show_all_jb.setFont(small_font);
	    hide_all_jb.setFont(small_font);
	    

	}

	//updateControls();
	
	// --- === --- === --- === --- === --- === --- === --- === --- === --- === --- === 
	//
	// now compose the three elements together into 2 split panes
	//
	// --- === --- === --- === --- === --- === --- === --- === --- === --- === --- === 

	// add tree and control panel to a H split pane
	//
	h_split_pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	h_split_pane.setLeftComponent(att_panel);
	h_split_pane.setRightComponent(tree_view);

	// add H split pane and text view pane to a V split pane
	//
	v_split_pane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	//view_scroller.setPreferredSize(new Dimension(300, 50));
	v_split_pane.setTopComponent(h_split_pane);
	v_split_pane.setBottomComponent(view_scroller);

	// --- === --- === --- === --- === --- === --- === --- === --- === --- === --- === 
	//
	// and pop it into the top level panel
	//
	// --- === --- === --- === --- === --- === --- === --- === --- === --- === --- === 

	// add the split pane to the panel
	//
	top_panel.add(v_split_pane);
	//top_panel.setMinimumSize(new Dimension(400, 450));
	GridBagConstraints c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	c.weighty = c.weightx = 1.0;
	c.fill = GridBagConstraints.BOTH;
	gridbag.setConstraints(v_split_pane, c);
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   Custom TreeCell Renderer
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

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

	private boolean draw_edge;  // to indicate selected node

	public CustomTreeCellRenderer()
	{
	    // leaf_icon = new ImageIcon("images/cluster.gif");
	    // System.out.println("CustomTreeCellRenderer() constructed");

	}

	public void paintComponent(Graphics g)
	{
	    int h = getHeight();

	    int off = 0;
	    
	    if(draw_glyph >= 0)
	    {
		if((glyph_poly == null) || (glyph_poly_height != h))
		{
		    // generate (or re-generate at a new size) the glyphs
		    glyph_poly = mview.getDataPlot().getScaledClusterGlyphs(h-2);
		    glyph_poly_height = h;
		    
		    // System.out.println("^v^v^v^v^ CustomTreeCellRenderer(): glyphs rescaled");
		}
		
		off += h;

		Polygon poly = new Polygon(glyph_poly[draw_glyph].xpoints, 
					   glyph_poly[draw_glyph].ypoints,
					   glyph_poly[draw_glyph].npoints);
		
		if(is_shown)
		    g.fillPolygon(poly);
		else
		    g.drawPolygon(poly);

	    }
	    else
		off = 0;

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

	    backgroundNonSelectionColor = tree_bg_col;
	    backgroundSelectionColor    = tree_bg_col;

	    //borderSelectionColor = (tree_grey_level > (2*51)) ? Color.black : Color.white;
	    borderSelectionColor =tree_bg_col;

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
		ExprData.Cluster cl = (ExprData.Cluster) node.getUserObject();
	    
		draw_edge = false;

		if(sel)
		{
		    draw_edge = true;
		    //System.out.println( "painting tree cell: " + cl.getName() + " is selected");  
		}
		if(hasFocus)
		{
		    //draw_edge = true;
		    //System.out.println( "painting tree cell: " + cl.getName() + " has focus");  
		}
	    
		// System.out.println("rendering " + cl.getName());
		
		// changed to draw all nodes as if they were leafs except the root

		//if(cl.getElements() != null) 
		if(cl.getParent() != null)
		{
		    //ExprData.Cluster cl = (ExprData.Cluster) node_info;
		    
		    draw_glyph = cl.getGlyph();
		    is_shown = cl.getShow();
		    
		    setFont(normal_font);
		    setForeground(cl.getColour());
		}
		else 
		{
		    //System.out.println("this is a leaf not");

		    // this is the root

		    draw_glyph = -1;
		    setFont(bold_font);
		    setIcon(null);
		    if(tree_grey_level > (2*51))
			setForeground(Color.black);
		    else
			setForeground(Color.white);
		    
		    // setToolTipText(null); //no tool tip
		    
		    //setFont(normal_font);
		} 
	    }

	    
	    // System.out.println("treecell component is " + this);

	    return this;
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // load a list of names from a file
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public boolean loadClusterDataFromTextFile(File file, ExprData.Cluster parent)
    {
	Vector new_elem_names   = new Vector();
	//Vector new_elem_vectors = new Vector();
	    
	Vector input_strs = new Vector();
	
	try
	{
	    ExprData.NameTagSelection nt_sel = mview.getNameTagSelection("Match which name or name attribute?");
	    
	    try
	    {
		BufferedReader br = new BufferedReader(new FileReader(file));
		
		String str = br.readLine();
		while(str != null)
		{
		    //String tstr = str.trim();
		    input_strs.addElement(str);
		    str = br.readLine();
		}
	    }
	    catch(java.io.IOException ioe)
	    {
		mview.alertMessage("Unable to read name.\nError: " + ioe);
	    }

	    // make the input_names list unique because multiple matches may have lead to duplicate names
	    Hashtable uniq_ht = new Hashtable();
	    for(int n=0; n < input_strs.size(); n++)
	    {
		final String nm = (String) input_strs.elementAt(n);
		uniq_ht.put( nm, nm );
	    }
	    input_strs = new Vector();
	    for (Enumeration e = uniq_ht.keys(); e.hasMoreElements() ;) 
	    {
		final String nm = (String) e.nextElement();
		input_strs.addElement(nm);
	    }
	    
	    // build a map from all the possible names or name attrs 
	    // (used to detect matches when scanning the input)

	    Hashtable name_table = new Hashtable();

	    final int ns = edata.getNumSpots();
	    for(int s=0; s < ns; s++)
	    {
		String n = nt_sel.getNameTag(s);
		if(n != null)
		{
		    Vector names = (Vector) name_table.get(n);
		    if(names == null)
		    {
			names = new Vector();
			name_table.put(n, names);
		    }
		    if(nt_sel.isSpotName() || nt_sel.isSpotNameAttr())
			names.addElement( edata.getSpotName(s) );
		    if(nt_sel.isProbeName() || nt_sel.isProbeNameAttr())
			names.addElement( edata.getProbeName(s) );
		    if(nt_sel.isGeneNames() || nt_sel.isGeneNamesAttr())
			names.addElement( edata.getGeneNames(s) );
		    
		}
	    }

	    boolean is_name_attr = false;
	    if(nt_sel.isSpotNameAttr() || nt_sel.isProbeNameAttr() || nt_sel.isGeneNamesAttr())
		is_name_attr = true;
	    
	    // count matches and store either the actual names or the vector of spot_ids 
	    //
	    int matched = 0;
	    int multi_matched = 0;
	    int ignored = 0;

	    for(int n=0; n < input_strs.size(); n++)
	    {
		String input_name = (String) input_strs.elementAt(n);
		Vector names = (Vector) name_table.get(input_name);

		if(names != null) 
		{
		    matched++;

		    if(names.size() > 1)
			multi_matched++;

		    if(is_name_attr)
		    {
			for(int nn=0; nn < names.size(); nn++)
			    new_elem_names.addElement( (String) names.elementAt(nn) );
		    }
		    else
		    {
			new_elem_names.addElement(input_name);		    
		    }
		}
		else
		{
		    ignored++;
		}
	    }
	    
	    // make this list unique because multiple matches may have lead to duplicate names
	    uniq_ht.clear();
	    for(int n=0; n < new_elem_names.size(); n++)
	    {
		final String nm = (String) new_elem_names.elementAt(n);
		uniq_ht.put( nm, nm );
	    }
	    new_elem_names = new Vector();
	    for (Enumeration e = uniq_ht.keys(); e.hasMoreElements() ;) 
	    {
		String nm = (String) e.nextElement();
		new_elem_names.addElement(nm);
	    }

	    // report status
	    //

	    // 	    System.out.println("matched: " + matched + " multiply matched: " + multi_matched);
	    
	    String report = input_strs.size() + " names in the file, assuming one name per line.\n";
	    
	    if(input_strs.size() == 0)
	    {
		mview.alertMessage("File appears to be empty");
		return false;
	    }
	    
	    if(matched == 0)
	    {
		mview.alertMessage("No names were recognised.");
		return false;
	    }
	    
	    if(input_strs.size() == matched)
	    {
		report += "All names were recognised.";
		
		mview.infoMessage(report); 
	    }
	    else
	    {
		report += matched + " name" + ((matched != 1) ? "s" : "") + " were recognised.\n";
		
		report += ignored + " name" + ((ignored != 1) ? "s" : "") + " not recognised\n";
		
		if(multi_matched > 0)
		    report += (multi_matched + " name" + 
			       ((multi_matched != 1) ? "s" : "") + 
			       " matched multiple instances in current data");
		
		report +="\nContinue?";
		
		if(mview.infoQuestion(report, "Yes", "No") == 1)
		    return false;
	    }
	    
	    // create the cluster
	    //

	    String new_name = mview.getString("New Cluster name:", file.getName());
	    
	    ExprData.Cluster new_clust = edata.new Cluster(new_name);

	    Vector converted_attr_names = null;

	    /*
	    // possibly convert s_id vectors into a single vector of SpotNames
	    //
	    if(nt_sel.isSpotNameAttr())
	    {
		for(int v=0; v < new_elem_vectors.size(); v++)
		{
		    Vector s_ids = (Vector) new_elem_vectors.elementAt(v);
		    for(int s=0; s < s_ids.size(); s++)
		    {
			int s_id = ((Integer) s_ids.elementAt(s)).intValue();
			String name = edata.getSpotName( s_id );
			converted_attr_names.addElement(name);
		    }
		}
		new_clust.setElements(ExprData.SpotName, converted_attr_names);
	    }

	    // possibly convert s_id vectors into a single vector of ProbeNames
	    //
	    if(nt_sel.isProbeNameAttr())
	    {
		for(int v=0; v < new_elem_vectors.size(); v++)
		{
		    Vector s_ids = (Vector) new_elem_vectors.elementAt(v);
		    
		    // only interested in the first occurrence of the Probe name
		    // as other instances of the name will share the same name attribute
		    int s_id = ((Integer) s_ids.elementAt(0)).intValue();
		    String name = edata.getProbeName( s_id );
		    converted_attr_names.addElement(name);
		}
		new_clust.setElements(ExprData.ProbeName, converted_attr_names);
	    }

	    // possibly convert s_id vectors into a single vector of GeneNames
	    //
	    if(nt_sel.isGeneNamesAttr())
	    {
		for(int v=0; v < new_elem_vectors.size(); v++)
		{
		    Vector s_ids = (Vector) new_elem_vectors.elementAt(v);
		    
		    // only interested in the first occurrence of the Gene name
		    // as other instances of the name will share the same name attribute
		    // 
		    // but we dont know which of the genes for this spot that it matches
		    // but the getNameTag() method concatenates all tags together...
		    int s_id = ((Integer) s_ids.elementAt(0)).intValue();
		    
		    String matched_word = (String) new_elem_names.elementAt(v);

		    String[] tags = nt_sel.getNameTagArray(s_id);

		    converted_attr_names.addElement(edata.getGeneName(s_id, 0));
		}
		new_clust.setElements(ExprData.GeneName, converted_attr_names);
	    }
	    
	    // otherwise just use the names....
	    //
	    */
	    
	    if(nt_sel.isSpotName() || nt_sel.isSpotNameAttr())
	    {
		//  names were spot names...
		new_clust.setElements(ExprData.SpotName, new_elem_names);
	    }
	    else
	    {
		if(nt_sel.isGeneNames() || nt_sel.isGeneNamesAttr() )
		{
		    //  names were genes names...
		    new_clust.setElements(ExprData.GeneName, new_elem_names);
		}
		else
		{
		    if(nt_sel.isProbeName()|| nt_sel.isProbeNameAttr())
		    {
			//  names were probe names...
			new_clust.setElements(ExprData.ProbeName, new_elem_names);
		    }
		    
		}
	    }
	    
	    edata.addChildToCluster(parent, new_clust);
	    
	    return true;
	}
	catch(UserInputCancelled e)
	{
	    return false;
	}
    }

    public boolean OLD_loadClusterDataFromTextFile(File file, ExprData.Cluster parent)
    {
	Vector names = new Vector();
	    
	Vector input_strs = new Vector();
	
	try
	{
	    BufferedReader br = new BufferedReader(new FileReader(file));
	    
	    String str = br.readLine();
		while(str != null)
		{
		    //String tstr = str.trim();
		    input_strs.addElement(str);
		    str = br.readLine();
		}
	}
	catch(java.io.IOException ioe)
	{
	    mview.alertMessage("Unable to read name.\nError: " + ioe);
	}
	
	// how many of the names are recognised?
	
	int matched_pnames = 0;
	int matched_gnames = 0;
	int matched_snames = 0;

	Vector new_pnames = new Vector();
	Vector new_gnames = new Vector();
	Vector new_snames = new Vector();

	int dupls = 0;
	String dupls_name = null;

	Hashtable pnames = edata.getProbeNameHashtable();
	Hashtable gnames = edata.getGeneNameHashtable();
	
	Vector s_ids = new Vector();

	int recognised = 0;
        int ignored_count = 0;
	String ignored = "\nIgnored:\n";

	for(int n=0; n < input_strs.size(); n++)
	{
	    String name = (String) input_strs.elementAt(n);
	    
	    int matched = 0;

	    // is it a spot name?
	    int s_id = edata.getIndexBySpotName(name);
	    if(s_id >= 0)
	    {
		s_ids.addElement(new Integer(s_id));
		matched++;
		matched_snames++;
		new_snames.addElement(name);
	    }
	    
	    // is it a gene name?
	    Vector g_s_ids = (Vector) gnames.get(name);
	    if(g_s_ids != null)
	    {
		s_ids.addAll(g_s_ids);
		matched++;
		matched_gnames++;
		new_gnames.addElement(name);
	    }
	    
	    // finally, is it a probe name?
	    Vector p_s_ids = (Vector) pnames.get(name);
	    if(p_s_ids != null)
	    {
		s_ids.addAll(p_s_ids);
		matched++;
		matched_pnames++;
		new_pnames.addElement(name);
	    }	    
	    
	    if(matched > 0)
	    {
		recognised++;

		if(matched > 1)
		{
		    dupls++;
		    dupls_name = name;
		}
	    }
	    else
	    {
		ignored_count++;

		if(ignored_count < 5)
		    ignored += "  " + name + "\n";
		if(ignored_count == 5)
		    ignored += "  (and more...)\n";
	    }
	}
	
	String report = input_strs.size() + " names in the file, assuming one name per line.\n";

	if(input_strs.size() == 0)
	{
	    mview.alertMessage("File appears to be empty");
	    return false;
	}

	if(recognised == 0)
	{
	    mview.alertMessage("No names were recognised.");
	    return false;
	}
	
	if(input_strs.size() == recognised)
	{
	    report += "All names were recognised.";

	    mview.infoMessage(report); 
	}
	else
	{
	    report += recognised + " name" + ((recognised != 1) ? "s" : "") + " were recognised.\n";
	
	    report += ignored;

	    report += "\nContinue?";

	    if(mview.infoQuestion(report, "Yes", "No") == 1)
		return false;
	}

	if(dupls > 0)
	{
	    String error = "One name, '" + dupls_name + "', matched more than one of Spot, Gene or Probe names";

	    if(dupls > 1)
	    {
		error = dupls + " names, for example '" + dupls_name + 
		        "', matched more than one of Spot, Gene or Probe names";
	    }

	    if(mview.infoQuestion(error + "\nContinue?" , "Yes", "No") == 1)
		return false;
	}
	
	try
	{
	    String new_name = mview.getString("New Cluster name:", file.getName());
	    
	    // attempt to preserve the typage of the name if possible, otherwise convert to spot ids
	    
	    int total = matched_pnames + matched_gnames + matched_snames;
	    
	    Vector elements = null;
	    
	    ExprData.Cluster new_clust = edata.new Cluster(new_name);
	    
	    if(total == matched_snames)
	    {
		// all names were spot names...
		new_clust.setElements(ExprData.SpotName, new_snames);
	    }
	    else
	    {
		if(total == matched_gnames)
		{
		    // all names were probe names...
		    new_clust.setElements(ExprData.GeneName, new_gnames);
		}
		else
		{
		    if(total == matched_pnames)
		    {
			// all names were probe names...
			new_clust.setElements(ExprData.ProbeName, new_pnames);
		    }
		    else
		    {
			// it was a hotch potch of names, use spot ids instead
			new_clust.setElements(ExprData.SpotIndex, s_ids);
		    }
		}
	    }
	    
	    edata.addChildToCluster(parent, new_clust);

	    return true;
	}
	catch(UserInputCancelled e)
	{
	    return false;
	}
	
    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   parsing Stanford format cluster data
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    //
    // the .gtr file:
    //
    //	  NODE1X	GENE2326X	GENE1806X	0.99981689453125
    //	  NODE2X	GENE2983X	GENE2445X	0.99945068359375
    //	  ....
    //	  NODE76X	NODE4X	GENE1323X	0.99688720703125
    //	  ...
    //	  NODE242X	NODE88X	NODE150X	0.994384765625
    //
    //
    // nodes dependancies are ok, each node is defined before being referenced in some other node
    //
    // assume GENExxx and GENEyyyy are the extents of the cluster using the ordering in the .cdt file
    // 
    // assume real names are the PROBE names.....
    //
    //

    private class EisenClust
    {
	public String node_name;
	public String first_name;
	public String last_name;

	public EisenClust(String n, String f, String l)
	{
	    node_name = n;
	    first_name = f;
	    last_name = l;
	}

    }

    public boolean loadClusterDataFromStanfordFormatFile(File gtr_file, ExprData.Cluster parent)
    {
	// also need the .cdt file which maps gene name to eisen-codes
	
	// attempt to find it automagically
	File cdt_file = null;

	String gtr_name = gtr_file.getPath();
	if((gtr_name.endsWith(".gtr")) || (gtr_name.endsWith(".GTR")))
	{
	    String root_name = gtr_name.substring(0, gtr_name.length() - 4);
	    String cdt_name = root_name + (gtr_name.endsWith(".gtr") ? ".cdt" : ".CDT");
	    
	    cdt_file = new File(cdt_name);
	}
	else
	{
	    mview.infoMessage("Unable to locate corresponding '.cdt' file.\nUsing manual file browsing technology...");
	    
	    JFileChooser fc = new JFileChooser();
	    fc.setCurrentDirectory(new File(mview.getProperty("clustman.load_path", System.getProperty("user.dir"))));
	    
	    //JFileChooser fc = mview.getFileChooser(mview.getProperty("clustman.load_path", System.getProperty("user.dir")));
	    
	    int returnVal = fc.showOpenDialog(frame);
	    
	    if (returnVal != JFileChooser.APPROVE_OPTION) 
		return false;
	    
	    cdt_file = fc.getSelectedFile();
	}	    
	
	// -----------------------------------------------------------------------
	//
	// the .cdt file specifies the order of the spots and gives the 
	// mapping from 'real' name to 'code' name
	// ( code names are of the form GENEnnnX )
	//
	// read each of the NODEnnnX \t GENEzzzX mappings
	//
	// watch out for multiple occurences of the same GENExxxX name
	// and attempt to map them to multiple ocurrences of the same
	// probe name in the current data
	//
	// -----------------------------------------------------------------------
	
	if(cdt_file != null)
	{

	    Vector eisen_clusts = new Vector();
	    
	    Vector input_strs = new Vector();

	    try
	    {
		BufferedReader br = new BufferedReader(new FileReader(cdt_file));
		
		String str = br.readLine();
		while(str != null)
		{
		    //String tstr = str.trim();
		    input_strs.addElement(str);
		    str = br.readLine();
		}
	    }
	    catch(java.io.IOException ioe)
	    {
		mview.alertMessage("Unable to read name.\nError: " + ioe);
	    }

	    int found = 0;
	    int recog = 0;

	    // -----------------------------------------------------------------------
	    // -----------------------------------------------------------------------

	    // build a table of vectors giving the spot index(es) for each probe name

	    Hashtable probe_name_to_spot_ids_ht = new Hashtable();
	    
	    {
		final int n_spots = edata.getNumSpots();

		for(int p=0; p < n_spots; p++)
		{
		    String pname = edata.getProbeName(p);
		    
		    Vector spot_vec = (Vector) probe_name_to_spot_ids_ht.get(pname);
		    if(spot_vec == null)
		    {
			// first time this probe name has been seen, build a new vector
			Vector new_vec = new Vector();
			new_vec.addElement(new Integer(p));
			probe_name_to_spot_ids_ht.put(pname, new_vec);
		    }
		    else
		    {
			// add this index to the vector of indices for this probe name
			spot_vec.addElement(new Integer(p));
		    }
		}
	    }
	    
	    // -----------------------------------------------------------------------
	    // -----------------------------------------------------------------------

	    Hashtable code_to_spot_index_ht = new Hashtable();
	    Hashtable code_name_to_index_ht = new Hashtable();
	    Vector    code_name_sequence_v = new Vector();

	    Hashtable real_name_freq_ht = new Hashtable();
	    int dupl_real_name = 0;

	    int missing_real_name = 0;
	    int too_many_dupls = 0;

	    // the first two lines are header to be skipped...
	    for(int en=2; en < input_strs.size(); en++)
	    {
		String str = (String) input_strs.elementAt(en);

		// extract the eisen-code as the chars up to the first tab
		int tab_p = str.indexOf('\t');
		if(tab_p >= 0)
		{
		    String eisen_name = (str.substring(0, tab_p)).trim();
		    String remainder =  str.substring(tab_p+1);
		    int tab_p_2 = remainder.indexOf('\t');
		    if(tab_p_2 >= 0)
		    {
			String real_name = (remainder.substring(0, tab_p_2)).trim();
			
			int occur = 1;

			// keep track of duplication within the real names
			{
			    Integer cnt = (Integer) real_name_freq_ht.get(real_name);
			    if(cnt == null)
			    {
				// first timer
				real_name_freq_ht.put(real_name, new Integer(1));
			    }
			    else
			    {
				int cnt_i = cnt.intValue();
				if(cnt_i == 1)
				    dupl_real_name++;
				real_name_freq_ht.put(real_name, new Integer(++cnt_i));
				occur = cnt_i;
			    }
			}

			// find the spot index of Nth probe name which matches this real name
			
			Vector spot_vec = (Vector) probe_name_to_spot_ids_ht.get(real_name);
			if(spot_vec == null)
			{
			    missing_real_name++;
			}
			else
			{
			    if(spot_vec.size() == 0)
			    {
				too_many_dupls++;
			    }
			    else
			    {
				Integer spot_id = (Integer) spot_vec.elementAt(0);
				spot_vec.removeElementAt(0);

				code_to_spot_index_ht.put(eisen_name, spot_id);

				code_name_to_index_ht.put(eisen_name, new Integer(en-2));
				
				code_name_sequence_v.addElement(eisen_name);
				
				found++;
			    }
			}

			if(probe_name_to_spot_ids_ht.get(real_name) != null)
			    recog++;
		    }
		}
	    }

	    // -----------------------------------------------------------------------
	    // report progress
	    // -----------------------------------------------------------------------

	    if(dupl_real_name == 0)
		mview.infoMessage(found + " names found, " + recog + " matched with current Probe names.");
	    else
	    {
		if(mview.infoQuestion(found + " names found, " + recog + " matched with current Probe names.\n" + 
				      dupl_real_name + " names were duplicated", "Details", "OK") == 0)
		{
		    String details = "Duplicated names:\n";
		    for (Enumeration e = real_name_freq_ht.keys(); e.hasMoreElements() ;) 
		    {
			String rname = (String) e.nextElement();
			int cnt_i = ((Integer) real_name_freq_ht.get(rname)).intValue();
			if(cnt_i > 1)
			    details += (" " + cnt_i + " x " + rname + "\n");
		    }
		    mview.infoMessage(details);
		}
	    }    

	    // -----------------------------------------------------------------------
	    // -----------------------------------------------------------------------

	    Hashtable eisen_clust_by_name_ht = new Hashtable();

	    // now parse the .gtr file and build the clusters
	    //
	    input_strs = new Vector();
	    
	    try
	    {
		BufferedReader br = new BufferedReader(new FileReader(gtr_file));
		
		String str = br.readLine();
		while(str != null)
		{
		    //String tstr = str.trim();
		    input_strs.addElement(str);
		    str = br.readLine();
		}
	    }
	    catch(java.io.IOException ioe)
	    {
		mview.alertMessage("Unable to read name.\nError: " + ioe);
	    }

	    int dupls = 0;
	    
	    for(int en=0; en < input_strs.size(); en++)
	    {
		String str = (String) input_strs.elementAt(en);

		// extract the eisen-code as the chars up to the first tab
		int tab_p = str.indexOf('\t');
		if(tab_p >= 0)
		{
		    String eisen_name = (str.substring(0, tab_p)).trim();
		    String part_2 =  str.substring(tab_p+1);
		    int tab_p_2 = part_2.indexOf('\t');
		    if(tab_p_2 >= 0)
		    {
			String clust_first = (part_2.substring(0, tab_p_2)).trim();
			
			String part_3 =  part_2.substring(tab_p_2+1);

			int tab_p_3 = part_3.indexOf('\t');
			if(tab_p_3 >= 0)
			{
			    String clust_last = (part_3.substring(0, tab_p_3)).trim();

			    // now we have the cluster details for this node
			    EisenClust ec = new EisenClust(eisen_name, clust_first, clust_last);
			    
			    if(eisen_clust_by_name_ht.get(eisen_name) != null)
				dupls++;

			    eisen_clust_by_name_ht.put(eisen_name, ec);
			    eisen_clusts.addElement(ec);
			}
		    }
		}
	    }
	    if(dupls > 0)
	    {
		mview.alertMessage("Warning: " + dupls + " node names were duplicated. Possibly bad?");
	    }
	    
	    int spots_used = 0;
	    	    
	    mview.infoMessage(eisen_clusts.size() + " nodes found in the file.");
	    
	    // now build a cluster for each of the nodes

	    //ExprData.Cluster new_root = edata.new Cluster("Stanford test");

	    final int nn = eisen_clusts.size();

	    int leaf_clusters   = 0;
	    int branch_clusters = 0;

	    Hashtable cluster_by_name_ht = new Hashtable();

	    int prog_done = 0;
	    int prog_update = (nn / 10);
	    int prog_tick = 0;

	    for(int n=0; n < nn; n++)
	    {
		if(++prog_tick == prog_update)
		{
		    prog_done += 10;
		    prog_tick = 0;
		    // System.out.println(prog_done + "%");
		}

		EisenClust ec = (EisenClust) eisen_clusts.elementAt(n);

		boolean first_is_node = ec.first_name.startsWith("NODE");
		boolean last_is_node  = ec.last_name.startsWith("NODE");

		//System.out.println(ec.node_name + " is " + ec.first_name + " to " + ec.last_name);

		if(first_is_node)
		{
		    if(last_is_node)
		    {
			// both are nodes...

			// get the cluster representing the left-hand side
			ExprData.Cluster left_node = (ExprData.Cluster) cluster_by_name_ht.get(ec.first_name);

			if(left_node == null)
			{
			    System.out.println("WARNING: undefined reference to " + ec.first_name + " in " + ec.node_name);
			}
			else
			{
			    // get the cluster representing the right-hand side
			    ExprData.Cluster right_node = (ExprData.Cluster) cluster_by_name_ht.get(ec.last_name);
			    if(right_node == null)
			    {
				System.out.println("WARNING: undefined reference to " + ec.last_name + " in " + ec.node_name);
			    }
			    else
			    {
				// create the new node containing no probes,
				ExprData.Cluster new_cl = edata.new Cluster(ec.node_name, ExprData.ProbeName, null);

				// and add the other clusters as children
				new_cl.addCluster(left_node);
				new_cl.addCluster(right_node);

				// and put this cluster in the table
				cluster_by_name_ht.put(ec.node_name, new_cl);
			    }
			}
		    }
		    else
		    {
			// node, !node

			// get the cluster representing the left-hand side
			ExprData.Cluster left_node = (ExprData.Cluster) cluster_by_name_ht.get(ec.first_name);

			if(left_node == null)
			{
			    System.out.println("WARNING: undefined reference to " + ec.first_name + " in " + ec.node_name);
			}
			else
			{
			    Vector maxd_spot_ids = new Vector();

			    maxd_spot_ids.addElement((Integer) code_to_spot_index_ht.get(ec.last_name));
			    
			    spots_used++;

			    // create the new node containing the single probe
			    ExprData.Cluster new_cl = edata.new Cluster(ec.node_name, ExprData.SpotIndex, maxd_spot_ids);
			    // and add the other cluster as a child
			    new_cl.addCluster(left_node);
			    
			    // and put this cluster in the table
			    cluster_by_name_ht.put(ec.node_name, new_cl);
			}
		    }
		}
		else
		{
		    if(last_is_node)
		    {
			// !node, node

			// get the cluster representing the right-hand side
			ExprData.Cluster right_node = (ExprData.Cluster) cluster_by_name_ht.get(ec.last_name);

			if(right_node == null)
			{
			    System.out.println("WARNING: undefined reference to " + ec.last_name + " in " + ec.node_name);
			}
			else
			{
			    Vector maxd_spot_ids = new Vector();

			    maxd_spot_ids.addElement((Integer) code_to_spot_index_ht.get(ec.first_name));
			    
			    spots_used++;

			    // create the new node containing the single probe
			    ExprData.Cluster new_cl = edata.new Cluster(ec.node_name, ExprData.SpotIndex, maxd_spot_ids);
			    // and add the other cluster as a child
			    new_cl.addCluster(right_node);
			    
			    // and put this cluster in the table
			    cluster_by_name_ht.put(ec.node_name, new_cl);
			}
		    }
		    else
		    {
			// neither are nodes, both are 'genes'
			
			// generate the list of names between 'first' and 'last'
			int fi = ((Integer) code_name_to_index_ht.get(ec.first_name)).intValue();
			int li = ((Integer) code_name_to_index_ht.get(ec.last_name)).intValue();

			spots_used += 2;

			Vector maxd_spot_ids = new Vector();
			
			for(int cn=fi; cn <= li; cn++)
			{
			    String code_name = (String) code_name_sequence_v.elementAt(cn);
			    Integer spot_id  = (Integer) code_to_spot_index_ht.get(code_name);

			    maxd_spot_ids.addElement(spot_id);

			    //System.out.println(ec.node_name + " has " + ((li-fi)+1) + " elements");

			    leaf_clusters++;
			}
			
			ExprData.Cluster new_cl = edata.new Cluster(ec.node_name, ExprData.SpotIndex, maxd_spot_ids);

			// store a reference to this cluster for later usage
			cluster_by_name_ht.put(ec.node_name, new_cl);

			//new_root.addCluster(new_cl);
		    }
		}
		
	    }
	    
	    if(eisen_clusts.size() > 0)
	    {
		// assume that the last cluster defined is the root....
		
		mview.infoMessage(spots_used + " Spots placed in " + cluster_by_name_ht.size() + " clusters.\n");
		
		EisenClust ec = (EisenClust) eisen_clusts.elementAt(eisen_clusts.size() - 1);
		
		ExprData.Cluster root_node = (ExprData.Cluster) cluster_by_name_ht.get(ec.node_name);
		
		edata.addChildToCluster(parent, root_node);

		//edata.addCluster(new_root);
	    }
	}
	    
	return true;
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   intestines
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private JPanel panel;
    
    private DragAndDropTree tree = null;

    private DefaultMutableTreeNode tree_top = null;
    private DefaultTreeModel tree_model;
    private Color tree_bg_col;
    private int tree_grey_level;

    // timer and iterator for auto-cycling the clusters
    private Timer timer;
    private int timer_delay;

    private ExprData.ClusterIterator cycle_iterator = null;
    private ExprData.Cluster         cycle_clust    = null;

    private boolean cycle_descend = false;
    private int cycle_current_child = 0;

    JTextPane view_panel = null;

    private ExprData.Cluster current_cluster = null;
    // private ExprData.Cluster current_group   = null;

    private JLabel colour_label, glyph_label;

    private JComboBox glyph_jcb;
    private JPanel att_panel;
    private JCheckBox show_jchb;
    //private JButton colour_jb;
    private JButton colour_jb;
    private JButton hide_all_jb;
    private JButton hide_group_jb;
    private JButton show_all_jb;
    private JButton show_group_jb;
    private JTextField clust_name_jtf;

    private ActionListener glyph_al;

    //private JButton auto_cycle_button;
 
    private JRadioButtonMenuItem fast_jrbmi, med_jrbmi, slow_jrbmi;
    
    private ImageIcon[] shown_glyph_images;
    private ImageIcon cycle_glyph_image;

    private ColourSetChooser col_set_cho;

    ///private ImageIcon[] hidden_glyph_images;

    private JSplitPane h_split_pane, v_split_pane;

    private Color[] colour_ramp;
    //private RampViewer colour_rv;
    
    private JLabel group_label;

    private maxdView mview;
    private ExprData edata;

    //private JLabel status_label;

    private Color background_col;
    private Color text_col;
}
