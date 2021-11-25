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

//
//
// new idea:
//
//    set master_hit_rate to 0 for all words
//
//    traverse tree,
//
//       get hit_rate of all words in all elements of this cluster and it's children
//
//       merge hit_rate with master_hit_rate by picking highest hit rate for each word
//

public class JustOClust implements Plugin, ExprData.ExprDataObserver
{
    public JustOClust(maxdView mview_)
    {
	mview = mview_;
	edata = mview.getExprData();
	anlo = mview.getAnnotationLoader();
    }

    public void cleanUp()
    {
	edata.removeObserver(this);
	if(frame != null)
	{
	    frame.setVisible(false);

	    mview.putBooleanProperty("JustOClust.detect_substr", substr_jchkb.isSelected());
	    mview.putBooleanProperty("JustOClust.case_sens", case_sens_jchkb.isSelected());
	    
	    mview.putProperty("JustOClust.min_word_len", min_wlen_jtf.getText());
	    mview.putProperty("JustOClust.min_repeat_pc", min_repeat_pc_jtf.getText());
	    mview.putProperty("JustOClust.delims", delim_jtf.getText());
	    source_nts.saveSelection("JustOClust.source");
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
	PluginInfo pinf = new PluginInfo("Just-o-Clust", "viewer", 
					 "Find common features in the annotation of clusters", "",
					 1, 0, 0);
	return pinf;
    }

    public PluginCommand[] getPluginCommands()
    {
	return null;
    }

    public void runCommand(String name, String[] args, CommandSignal done)
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
	frame = new JFrame("Just-o-Clust");
	
	mview.decorateFrame(frame);

	frame.addWindowListener(new WindowAdapter() 
			  {
			      public void windowClosing(WindowEvent e)
			      {
				  cleanUp();
			      }
	    });
	
	JPanel panel = new JPanel();
	panel.setPreferredSize(new Dimension(500, 300));
	GridBagLayout gridbag = new GridBagLayout();
	panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	panel.setLayout(gridbag);
	GridBagConstraints c = null;

	int line = 0;

	tabbed = new JTabbedPane();

	// ------------------------------------------------------------------
	// == pick cluster ==================================================
	// ------------------------------------------------------------------

	{
	    JPanel pick_cluster = new JPanel();
	    GridBagLayout p_gridbag = new GridBagLayout();
	    pick_cluster.setLayout(p_gridbag);
	    
	    
	    /*
	    cluster_list = new DragAndDropList();

	    cluster_list.setDropAction(new DragAndDropList.DropAction()
		{
		    public void dropped(DragAndDropEntity dnde)
		    {
			try
			{
			    ExprData.Cluster cl = dnde.getCluster();
			    System.out.println(cl.getName() + " dropped");
			    
			    if(pick_clusters)
			    {
				// this wont work because the names have been
				// artificially padded with " "s
				//
				cluster_list.setSelectedValue( cl.getName(), true );

			    }
			}
			catch(DragAndDropEntity.WrongEntityException another_wee)
			{
			    System.out.println("not a cluster");
			}
		    }
		});
	    */

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

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = c.weighty = 10.0;
	    c.fill = GridBagConstraints.BOTH;
	    p_gridbag.setConstraints(jsp, c);
	    pick_cluster.add(jsp);


	    tabbed.add(" Pick cluster ", pick_cluster);
	}

	// ------------------------------------------------------------------
	// == select rules ==================================================
	// ------------------------------------------------------------------

	{
	    JPanel select_rules = new JPanel();
	    
	    int sline = 0;

	    GridBagLayout r_gridbag = new GridBagLayout();
	    select_rules.setLayout(r_gridbag);

	    JLabel label = new JLabel("Word source ");
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = sline;
	    c.anchor = GridBagConstraints.EAST;
	    r_gridbag.setConstraints(label, c);
	    select_rules.add(label);

	    // String[] word_srcs = { "Annotation", "Gene name(s)", "Probe name", "Spot comment" };

	    source_nts = new NameTagSelector(mview, "Annotation");
	    source_nts.loadSelection("JustOClust.source");
	    source_nts.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			
		    }
		});
	    
	    /*
	      words_jcb = new JComboBox(word_srcs);
	      c = new GridBagConstraints();
	    */
	    c.gridx = 1;
	    c.gridy = sline++;
	    c.anchor = GridBagConstraints.WEST;
	    r_gridbag.setConstraints(source_nts, c);
	    select_rules.add(source_nts);
	   
	    label = new JLabel("Delimiters ");
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = sline;
	    c.anchor = GridBagConstraints.EAST;
	    r_gridbag.setConstraints(label, c);
	    select_rules.add(label);
	    
	    delim_jtf = new JTextField(20);
	    delim_jtf.setText(mview.getProperty("JustOClust.delims", delims));
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = sline++;
	    c.anchor = GridBagConstraints.WEST;
	    r_gridbag.setConstraints(delim_jtf, c);
	    select_rules.add(delim_jtf);
	    
	    label = new JLabel("Min word length ");
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = sline;
	    c.anchor = GridBagConstraints.EAST;
	    r_gridbag.setConstraints(label, c);
	    select_rules.add(label);
	    
	    min_wlen_jtf = new JTextField(3);
	    min_wlen_jtf.setText(mview.getProperty("JustOClust.min_word_len",  "3"));
	    // delim_jtf.setText(delims);
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = sline++;
	    c.anchor = GridBagConstraints.WEST;
	    r_gridbag.setConstraints(min_wlen_jtf, c);
	    select_rules.add(min_wlen_jtf);

	    label = new JLabel("Min repeat percent ");
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = sline;
	    c.anchor = GridBagConstraints.EAST;
	    r_gridbag.setConstraints(label, c);
	    select_rules.add(label);
	    
	    min_repeat_pc_jtf = new JTextField(3);
	    min_repeat_pc_jtf.setText(mview.getProperty("JustOClust.min_repeat_pc", "25"));
	    // delim_jtf.setText(delims);
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = sline++;
	    c.anchor = GridBagConstraints.WEST;
	    r_gridbag.setConstraints(min_repeat_pc_jtf, c);
	    select_rules.add(min_repeat_pc_jtf);


	    substr_jchkb = new JCheckBox("Detect words as substrings");
	    substr_jchkb.setSelected(mview.getBooleanProperty("JustOClust.detect_substr", true));
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = sline++;
	    c.anchor = GridBagConstraints.WEST;
	    r_gridbag.setConstraints(substr_jchkb, c);
	    select_rules.add(substr_jchkb);

	    case_sens_jchkb = new JCheckBox("Case sensitive");
	    case_sens_jchkb.setSelected(mview.getBooleanProperty("JustOClust.case_sens", true));
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = sline++;
	    c.anchor = GridBagConstraints.WEST;
	    r_gridbag.setConstraints(case_sens_jchkb, c);
	    select_rules.add(case_sens_jchkb);


	    /*
	    common_jchkb = new JCheckBox("Display common words");
	    common_jchkb.setSelected(mview.getBooleanProperty("JustOClust.display_common", true));
	    common_jchkb.setEnabled(false);
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = sline++;
	    c.anchor = GridBagConstraints.WEST;
	    r_gridbag.setConstraints(common_jchkb, c);
	    select_rules.add(common_jchkb);
	    */

	    
	    // -------------------
	    
	    label = new JLabel("Stop words ");
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = sline;
	    c.anchor = GridBagConstraints.EAST;
	    r_gridbag.setConstraints(label, c);
	    select_rules.add(label);
	    
	    JButton wjb = new JButton("Edit");
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = sline++;
	    c.anchor = GridBagConstraints.WEST;
	    r_gridbag.setConstraints(wjb, c);
	    select_rules.add(wjb);
	    wjb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			editStopWords();
		    }
		});

	    label = new JLabel("Translations ");
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = sline;
	    c.anchor = GridBagConstraints.EAST;
	    r_gridbag.setConstraints(label, c);
	    select_rules.add(label);
	    
	    wjb = new JButton("Edit");
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = sline++;
	    c.anchor = GridBagConstraints.WEST;
	    r_gridbag.setConstraints(wjb, c);
	    select_rules.add(wjb);
	    wjb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			editTranslations();
		    }
		});

	    /*
	    JPanel pc_panel = new JPanel();
	    GridBagLayout pc_gridbag = new GridBagLayout();
	    pc_panel.setLayout(pc_gridbag);

	    label = new JLabel(" occuring at least ");
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    //c.anchor = GridBagConstraints.EAST;
	    pc_gridbag.setConstraints(label, c);
	    pc_panel.add(label);

	    min_pc_jtf = new JTextField(3);
	    min_pc_jtf.setText(String.valueOf(min_common_word_pc));
	    // delim_jtf.setText(delims);
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    //c.anchor = GridBagConstraints.WEST;
	    pc_gridbag.setConstraints(min_pc_jtf, c);
	    pc_panel.add(min_pc_jtf);

	    label = new JLabel(" % of the time");
	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = 0;
	    //c.anchor = GridBagConstraints.EAST;
	    pc_gridbag.setConstraints(label, c);
	    pc_panel.add(label);

	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = sline++;
	    c.anchor = GridBagConstraints.WEST;
	    r_gridbag.setConstraints(pc_panel, c);
	    select_rules.add(pc_panel);

	    // -------------------

	    groups_jchkb = new JCheckBox("Display common word groups");
	    groups_jchkb.setEnabled(false);
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = sline++;
	    c.anchor = GridBagConstraints.WEST;
	    r_gridbag.setConstraints(groups_jchkb, c);
	    select_rules.add(groups_jchkb);
	    */

	    tabbed.add(" Set options ", select_rules);
	}

	// ------------------------------------------------------------------
	// == view results ==================================================
	// ------------------------------------------------------------------

	{
	    JPanel view_results = new JPanel();

	    JPanel picker_panel = new JPanel();
	    GridBagLayout v_gridbag = new GridBagLayout();
	    picker_panel.setLayout(v_gridbag);

	    Insets ins = new Insets(0,2,0,2);
	    ButtonGroup bg = new ButtonGroup();
	    
	    JRadioButton jb = new JRadioButton("Clusters");
	    bg.add(jb);
	    jb.setSelected(pick_clusters);
	    jb.setFont(mview.getSmallFont());
	    jb.setMargin(ins);
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.WEST;
	    v_gridbag.setConstraints(jb, c);
	    picker_panel.add(jb);
	    
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			pick_clusters = true;
			updatePickerPanel();
		    }
		});

	    jb = new JRadioButton("Words");
	    bg.add(jb);
	    jb.setSelected(!pick_clusters);
	    jb.setMargin(ins);
	    jb.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.WEST;
	    v_gridbag.setConstraints(jb, c);
	    picker_panel.add(jb);

	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			pick_clusters = false;
			updatePickerPanel();
		    }
		});

	    
	    picker_jsp = new JScrollPane();
	    picker_jsp.setPreferredSize( new Dimension( 100, 300));
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.gridwidth = 2;
	    c.weightx = 1.0;
	    c.weighty = 10.0;
	    c.fill = GridBagConstraints.BOTH;
	    v_gridbag.setConstraints(picker_jsp, c);
	    picker_panel.add(picker_jsp);
	           
	    	      
	    // results_jta = new JTextArea(200,80);
	    word_list_panel = new WordListPanel();
	    word_list_panel_jsp = new JScrollPane(word_list_panel);
	    
	    JSplitPane jsplt_pane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	    jsplt_pane.setLeftComponent(picker_panel);
	    jsplt_pane.setRightComponent(word_list_panel_jsp);

	    GridBagLayout o_gridbag = new GridBagLayout();
	    view_results.setLayout(o_gridbag);
	    
	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = 0;
	    c.gridheight = 2;
	    c.weightx = 9.0;
	    c.weighty = 10.0;
	    c.fill = GridBagConstraints.BOTH;
	    o_gridbag.setConstraints(jsplt_pane, c);
	    view_results.add(jsplt_pane);
	    
	    scale_slider = new JSlider(JSlider.VERTICAL, 0, 100, (int)(scale_factor * 100.0));
	    scale_slider.setPreferredSize(new Dimension(16, 300));
	    scale_slider.setToolTipText("Magnification");
	    scale_slider.addChangeListener(new ChangeListener()
		{
		    public void stateChanged(ChangeEvent e) 
		    {
			JSlider source = (JSlider)e.getSource();
			word_list_panel.setScale(((double) source.getValue()) * 0.01);
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = 3;
	    c.gridy = 0;
	    //c.gridheight = 2;
	    c.weighty = 10.0;
	    c.fill = GridBagConstraints.VERTICAL;
	    o_gridbag.setConstraints(scale_slider, c);
	    view_results.add(scale_slider);

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
			public void actionPerformed(ActionEvent ae) 
			{
			    int tab_i = tabbed.getSelectedIndex();
			    if(tab_i > 0)
			    {
				tab_i--;

				tabbed.setSelectedIndex(tab_i);

				nbutton.setEnabled(true);

				if(tab_i == 0)
				{
				     populateTreeWithClusters( cluster_tree, edata.getRootCluster() );
				     // buildClusterList();

				     if(selected_cluster != null)
				     {
					 // find the coresponding tree node and expand the tree to that path
					 DefaultTreeModel model      = (DefaultTreeModel)         cluster_tree.getModel();
					 DefaultMutableTreeNode root = ( DefaultMutableTreeNode ) model.getRoot();
					 
					 for (Enumeration e =  root.depthFirstEnumeration(); e.hasMoreElements() ;) 
					 {
					     DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) e.nextElement();
					     ExprData.Cluster ncl = (ExprData.Cluster) dmtn.getUserObject();
					     if(selected_cluster == ncl)
					     {
						 TreeNode[] tn_path = model.getPathToRoot(dmtn);
						 TreePath tp = new TreePath(tn_path);
						 
						 cluster_tree.expandPath(tp);
						 cluster_tree.scrollPathToVisible(tp);
						 cluster_tree.setSelectionPath(tp);
						 break;
					     }
					 } 
				     }

				     bbutton.setEnabled(false);
				}
			    }
			}
		    });
		
		c = new GridBagConstraints();
		w_gridbag.setConstraints(bbutton, c);
		wrapper.add(bbutton);
		
		nbutton.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    int tab_i = tabbed.getSelectedIndex();
			    if(tab_i < 2)
			    {
				if(tab_i == 0)
				{
				    DefaultMutableTreeNode node = (DefaultMutableTreeNode) cluster_tree.getLastSelectedPathComponent();
				    if(node == null)
				    {
					mview.alertMessage("You must select a cluster first.");
					return;
				    }
				    else
				    {
					selected_cluster = (ExprData.Cluster) node.getUserObject();
				    }
				}
				
				tab_i++;
				tabbed.setSelectedIndex(tab_i);
				
				bbutton.setEnabled(true);

				if(tab_i == 2)
				{
				    applyRules();
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
		Dimension fillsize = new Dimension(16,4);
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
		Dimension fillsize = new Dimension(16,4);
		Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
		c = new GridBagConstraints();
		c.gridx = 4;
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
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 5;
		w_gridbag.setConstraints(button, c);
	    }
	    {
		JButton button = new JButton("Help");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    mview.getPluginHelpTopic("JustOClust", "JustOClust");
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 6;
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

	// ------------------------------------------------------------------
	// ==================================================================
	// ------------------------------------------------------------------

	tabbed.setEnabledAt(0, false);
	tabbed.setEnabledAt(1, false);
	tabbed.setEnabledAt(2, false);

	frame.getContentPane().add(panel);

	frame.pack();
	frame.setVisible(true);

	frame.addKeyListener(new CustomKeyListener());

	// buildClusterList();
	updatePickerPanel();
    }

    public void resetReporter()
    {
	results_jta.setText("");
    }
    public void reportBusy(boolean is_busy)
    {
	results_jta.setCursor(Cursor.getPredefinedCursor(is_busy ?  Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    public void report(String msg)
    {
	results_jta.append(msg + "\n");
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

    private DefaultMutableTreeNode generateClusterTreeNodes(DefaultMutableTreeNode parent, ExprData.Cluster clust)
    {
	if(clust.getIsSpot())
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
	return null;
    }

     // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private void populateTreeWithWords(final JTree tree, final DisplayableWordList dwl)
    {
	// System.out.println("making tree for " + cluster.getName() );

	DefaultMutableTreeNode parent = new DefaultMutableTreeNode( null);
	
	if(dwl != null)
	{
	    Vector v = word_list_panel.root_dwl.makeWordVector();

	    String[] va = (String[]) v.toArray(new String[0]);
	    java.util.Arrays.sort(va);
	    
	    for(int w=0; w < va.length; w++)
	    {
		final String word = va[w];
		final DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode( va[w] );
		
		// add a child for each cluster which has this word repeated
		addClustersToWordTreeNode( word, dmtn, dwl );
		
		// if any children were added, store the words node in the parent 
		if(dmtn.getChildCount() > 0)
		    parent.add(dmtn);
	    }
	}

	tree.putClientProperty("JTree.lineStyle", "Angled");
	
	DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
	renderer.setLeafIcon(null);
	renderer.setOpenIcon(null);
	renderer.setClosedIcon(null);
	tree.setCellRenderer(renderer);

	tree.setModel( new DefaultTreeModel( parent ));
    }

    // scan all word lists looking for clusters which have repeats for the specified word
    //
    private void addClustersToWordTreeNode(final String word, final DefaultMutableTreeNode node, final DisplayableWordList dwl)
    {
	int min_repeat_count = (int)((double)(dwl.max_count) * (double)min_repeat_pc * 0.01);

	// need to scan all word lists.....
	// TODO:: this data should be stored as the DWLs are built
	//
	DefaultMutableTreeNode dmtn = node;
	if(dwl.ordered != null)
	{
	    for(int o=0; o < dwl.ordered.length; o++)
	    {
		if(dwl.ordered[o].word.equals(word))
		{
		    if(dwl.ordered[o].count_pc_i > min_repeat_count)
		    {
			dmtn = new DefaultMutableTreeNode( dwl.cluster );
			node.add(dmtn);
		    }
		}
	    }
	}
	if(dwl.children != null)
	{
	    for(int c=0; c < dwl.children.length; c++)
		addClustersToWordTreeNode(word, dmtn, dwl.children[c]);
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private int sort_mode = 0;

    private class CommonToken implements Comparable
    {
	public int count;
	public String str;

	public CommonToken(int c, String s)
	{
	    count = c;
	    str = s;
	}

	public int compareTo(Object o)
	{
	    CommonToken ct = (CommonToken) o;
	    return str.compareTo(ct.str);
	}
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // =====  ======================================================
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public void updateRules()
    {
	delim_jtf.setText(delims);
	min_pc_jtf.setText(String.valueOf(min_common_word_pc));
	min_wlen_jtf.setText(String.valueOf(min_word_len));
	min_repeat_pc_jtf.setText(String.valueOf(min_repeat_pc));
    }

    public void updatePickerPanel()
    {
	// picker_jsp.removeAll();

	if(pick_clusters)
	{
	    status_label.setText("Building cluster tree");

	    // populateTreeWithClusters()...

	    if(cluster_picker_tree == null)
	    {
		cluster_picker_tree = new DragAndDropTree();
		
		// mouse and drag'n'drop handling....

		MouseListener tree_listener = new MouseAdapter() 
		    {
			public void mouseClicked(MouseEvent e) 
			{
			    DefaultMutableTreeNode node = (DefaultMutableTreeNode) cluster_picker_tree.getLastSelectedPathComponent();

			    if(node != null)
			    {
				ExprData.Cluster cluster = (ExprData.Cluster) node.getUserObject();
				
				DisplayableWordList dwl = word_list_panel.root_dwl.matchByCluster( cluster );
				
				if(dwl != null)
				    word_list_panel.scrollToList( dwl );
			    }
			}
		    };
		cluster_picker_tree.addMouseListener(tree_listener);

		cluster_picker_tree.setDropAction( new DragAndDropTree.DropAction()
		    {
			public void dropped(DragAndDropEntity dnde)
			{
			    try
			    {
				ExprData.Cluster cl = dnde.getCluster();
				
				DefaultTreeModel model      = (DefaultTreeModel)         cluster_picker_tree.getModel();
				DefaultMutableTreeNode root = ( DefaultMutableTreeNode ) model.getRoot();

				for (Enumeration e =  root.depthFirstEnumeration(); e.hasMoreElements() ;) 
				{
				    DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) e.nextElement();
				    ExprData.Cluster ncl = (ExprData.Cluster) dmtn.getUserObject();
				    if(cl == ncl)
				    {
					TreeNode[] tn_path = model.getPathToRoot(dmtn);
					TreePath tp = new TreePath(tn_path);
					
					cluster_picker_tree.expandPath(tp);
					cluster_picker_tree.scrollPathToVisible(tp);
					cluster_picker_tree.setSelectionPath(tp);
					return;
				    }
				}
			    }
			    catch(DragAndDropEntity.WrongEntityException wee)
			    {
			    }
			}
		    });
		
		cluster_picker_tree.setDragAction(new DragAndDropTree.DragAction()
		    {
			public DragAndDropEntity getEntity()
			{
			    DefaultMutableTreeNode node = (DefaultMutableTreeNode) cluster_picker_tree.getLastSelectedPathComponent();
			    
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
	    
	    if(word_list_panel.root_dwl != null)
		populateTreeWithClusters(cluster_picker_tree, word_list_panel.root_dwl.cluster );
	    else
	    {
		// cluster_picker_tree.setModel( new DefaultTreeModel( null ));
		// System.out.println("no root WordList");
	    }

	    // System.out.println("adding cluster tree");

	    picker_jsp.setViewportView(cluster_picker_tree);

	    //picker_jsp.revalidate();
	}
	else
	{
	    status_label.setText("Building word tree");

	    if(word_picker_tree == null)
	    {
		word_picker_tree = new DragAndDropTree();
		
		// mouse  handling....

		MouseListener tree_listener = new MouseAdapter() 
		    {
			public void mouseClicked(MouseEvent e) 
			{
			    DefaultMutableTreeNode node = (DefaultMutableTreeNode) word_picker_tree.getLastSelectedPathComponent();
			    
			    if(node != null)
			    {
				try
				{
				    ExprData.Cluster cluster = (ExprData.Cluster) node.getUserObject();
				    
				    DisplayableWordList dwl = word_list_panel.root_dwl.matchByCluster( cluster );
				    
				    if(dwl != null)
					word_list_panel.scrollToList( dwl );
				}
				catch(ClassCastException cce)
				{
				    // wasn't a cluster then, must have been a word
				    String word = (String) node.getUserObject();
				    word_list_panel.selected_words.clear();
				    word_list_panel.selected_words.put(word, word);
				    word_list_panel.repaint();
				}
			    }

			}
		    };
		word_picker_tree.addMouseListener(tree_listener);

		word_picker_tree.setDragAction(new DragAndDropTree.DragAction()
		    {
			public DragAndDropEntity getEntity()
			{
			    DefaultMutableTreeNode node = (DefaultMutableTreeNode) word_picker_tree.getLastSelectedPathComponent();
			    
			    if(node != null)
			    {
				try
				{
				    ExprData.Cluster cluster = (ExprData.Cluster) node.getUserObject();
				    
				    DragAndDropEntity dnde = DragAndDropEntity.createClusterEntity(cluster);
				    
				    return dnde;
				}
				catch(ClassCastException cce)
				{
				    return null;
				}
			    }
			    else
				return null;
			}
		    });
	    }
	    
	    if(word_list_panel.root_dwl != null)
		populateTreeWithWords(word_picker_tree, word_list_panel.root_dwl );
	    
	    picker_jsp.setViewportView(word_picker_tree);
    
            // ===================================
	}
	picker_jsp.revalidate();
	status_label.setText("");
    }

    
    private final void openTreeToDepth(final int depth)
    {
	final JTree target = (pick_clusters) ? cluster_picker_tree :  word_picker_tree;
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
	private boolean custom_menu_hotkey_armed = false;

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


    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ===== apply rules ======================================================
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    public void applyRules()
    {
	// ==== reset viewer components ====

	word_list_panel.setWordList(null);

	if(cluster_picker_tree != null)
	{
	    cluster_picker_tree.setModel( null );
	}

	if(word_picker_tree != null)
	{
	    word_picker_tree.setModel( null );
	}

	picker_jsp.revalidate();

	// ==== read user options ====

	try
	{
	    min_word_len = new Integer(min_wlen_jtf.getText()).intValue();
	}
	catch(NumberFormatException nfe)
	{
	}

	try
	{
	    min_repeat_pc = new Integer(min_repeat_pc_jtf.getText()).intValue();
	}
	catch(NumberFormatException nfe)
	{
	}

	case_sens = case_sens_jchkb.isSelected();

	delims = unescapeDelimiters(delim_jtf.getText());

	// System.out.println("delims are '" + delims  + "'");
	
	new FindMatchThread().start();
    }
    
    private String escapeDelimiters(String d)
    {
	boolean armed = false;
	StringBuffer sbuf = new StringBuffer();
	for(int c=0; c < d.length(); c++)
	{
	    char ch  = d.charAt(c);
	    if(ch == 'n') 
		sbuf.append('\n');
	    else
	    {
		if(ch == '\t') 
		    sbuf.append('\t');
		else
		{
		    if(ch == '\\') 
		    {
			if(armed)
			{
			    sbuf.append('\\');
			    armed = false;
			}
			else
			{
			    armed = true;
			}
		    }
		    else
		    {
			sbuf.append(ch);
		    }
		}
	    }
	}
	return sbuf.toString();
    }

    private String unescapeDelimiters(String d)
    {
	boolean armed = false;
	StringBuffer sbuf = new StringBuffer();

	for(int c=0; c < d.length(); c++)
	{
	    char ch  = d.charAt(c);
	    if((ch == '\\') && (!armed))
	    {
		armed = true;
	    }
	    else
	    {
		if(armed)
		{
		    if(ch == 'n') 
			sbuf.append('\n');
		    if(ch == '\\') 
			sbuf.append('\\');
		    if(ch == 't') 
			sbuf.append('\t');
		    armed = false;
		}
		else
		{
		    sbuf.append(ch);
		}

	    }
	}
	
	// System.out.println("\"" + d + "\"" + " -> \"" + sbuf.toString() + "\"");

	return sbuf.toString();
    }

    public class FindMatchThread extends Thread
    {

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

	public void run()
	{
	    // int cl_i = cluster_list.getSelectedIndex();
	    // int cl_i = cluster_tree.getSelectedIndex();

	    DefaultMutableTreeNode node = (DefaultMutableTreeNode) cluster_tree.getLastSelectedPathComponent();

	    if(node != null)
	    {
		// ExprData.Cluster cluster = (ExprData.Cluster) clusters.elementAt(cl_i);
		ExprData.Cluster cluster = (ExprData.Cluster) node.getUserObject();
		
		//final FontMetrics fm  = word_list_panel.getGraphics().getFontMetrics();
		//final int text_height = fm.getAscent() /*+ fm.getDescent()*/;
	    
	    
		DisplayableWordList dwl = makeWordList(cluster);

		status_label.setText("Building graph");

		dwl.layout(10, 10, word_list_panel.text_height);
		dwl.translate();

		word_list_panel.setWordList(dwl);

		status_label.setText("");

	    }
	}
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ===== Translations and Stop Words ======================================
    //
    //
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    Hashtable stop_words_ht  = new Hashtable();
    Hashtable translation_ht = new Hashtable();
 
    JFrame stop_word_editor = null;
    JFrame translation_editor = null;

    public boolean isStopWord( String word )
    {
	return stop_words_ht.get(word) == null ? false : true;
    }

    public void editStopWords()
    {
	if(stop_word_editor == null)
	{
	    stop_word_editor = createHashkeysEditor( "Stop Words", stop_words_ht, false);
	    stop_word_editor.addWindowListener(new WindowAdapter() 
		{
		    public void windowClosing(WindowEvent e)
		    {
			stop_word_editor = null;
		    }
		});
	}
	//	if(!stop_word_editor.isVisible())
	//    stop_word_editor.setVisible(true);
	stop_word_editor.show();
    }

    public String getTranslation( String word )
    {
	String tr = (String) translation_ht.get(word);
	return (tr == null) ? word : tr;
    }

    public void editTranslations()
    {
	if(translation_editor == null)
	{
	    translation_editor = createHashkeysEditor( "Translations", translation_ht, true );
	    translation_editor.addWindowListener(new WindowAdapter() 
		{
		    public void windowClosing(WindowEvent e)
		    {
			translation_editor = null;
		    }
		});
	    translation_editor.addWindowListener(new WindowAdapter() 
		{
		    public void windowClosing(WindowEvent e)
		    {
			System.out.println("translations panel shut");
			translation_editor = null;
		    }
		});
	}
	translation_editor.setVisible(true);
    }

    public JFrame createHashkeysEditor(final String title, final Hashtable ht, final boolean edit_vals)
    {
	final JFrame kframe = new JFrame(title);
	
	//kframe.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	
	JPanel panel = new JPanel();
	// panel.setPreferredSize(new Dimension(550, 300));
	GridBagLayout gridbag = new GridBagLayout();
	panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	panel.setLayout(gridbag);
	GridBagConstraints c = null;

	int line = 0;

	final DragAndDropTable ktable = new DragAndDropTable();

	updateHashkeysTable(ktable, ht, edit_vals);

	JScrollPane jsp = new JScrollPane(ktable);
	jsp.setPreferredSize( new Dimension( 300, 500 ));
	
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

	{
	    JPanel wrapper = new JPanel();
	    wrapper.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrapper.setLayout(w_gridbag);

	    {
		JButton button = new JButton("Add");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    try
			    {
				if(edit_vals)
				{
				    String new_from = mview.getString("Translate from");

				    if(ht.get(new_from) != null)
				    {
					mview.alertMessage("There is already a translation for '" + new_from + "'");
					return;
				    }
				    String new_to   = mview.getString("Translate to");
				    ht.put(new_from, new_to);
				}
				else
				{
				    String new_word = mview.getString("Add new stop word");
				    ht.put(new_word, new_word);
				}
				updateHashkeysTable(ktable, ht, edit_vals);
			    }
			    catch(UserInputCancelled uic)
			    {
			    }
			}
		    });
		
		c = new GridBagConstraints();
		c.weightx = 1.0;
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		w_gridbag.setConstraints(button, c);
	    }
	    {
		JButton button = new JButton("Remove");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    if(ktable.getSelectedRow() == -1)
				mview.alertMessage("No entries seleected");
			    else
			    {
				int[] selrows = ktable.getSelectedRows();
				
				DefaultTableModel model = (DefaultTableModel) ktable.getModel();
				
				for(int d=0; d < selrows.length; d++)
				{
				    String key_str = (String) model.getValueAt(selrows[d], 0);
				    ht.remove(key_str);
				}
				updateHashkeysTable(ktable, ht, edit_vals);
			    }
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		w_gridbag.setConstraints(button, c);
	    }

	    {
		JButton button = new JButton("Load");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    loadHashkeys(ht, edit_vals);
			    updateHashkeysTable(ktable, ht, edit_vals);
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.weightx = 1.0;
		c.gridy = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		w_gridbag.setConstraints(button, c);
	    }
	    {
		JButton button = new JButton("Save");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    saveHashkeys(ht, edit_vals);
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		w_gridbag.setConstraints(button, c);
	    }

	    {
		Dimension fillsize = new Dimension(16,16);
		Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 0;
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
			    //kframe.setVisible(false);
			    kframe.hide();
			    // need to fire a WindowClosing event here
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 3;
		c.gridy = 1;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		w_gridbag.setConstraints(button, c);
	    }
	    {
		JButton button = new JButton("Help");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    mview.getPluginHelpTopic("JustOClust", "JustOClust", "#context");
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 3;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
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
	
	kframe.getContentPane().add(panel);
	kframe.pack();

	return kframe;
    }

    private void updateHashkeysTable(final JTable table, final Hashtable ht, final boolean edit_vals)
    {
	Vector data = new Vector();
	for (Enumeration e = ht.keys(); e.hasMoreElements() ;) 
	{
	    String key = (String) e.nextElement();
	    Vector line_data = new Vector();

	    if(edit_vals)
	    {
		line_data.addElement(key);
		String val = (String) ht.get(key);
		line_data.addElement(val);
	    }
	    else
	    {
		line_data.addElement(key);
	    }
	    data.addElement(line_data);
	}

	Vector colkeys = new Vector();

	if(edit_vals)
	{
	    colkeys.addElement("From");
	    colkeys.addElement("To");
	}
	else
	{
	    String ct = " Stop Word";
	    if(data.size() != 1)
		ct += "s";
	    
	    colkeys.addElement( data.size() + ct );
	}
	
	table.setModel( new DefaultTableModel( data, colkeys ));
    }

    private void loadHashkeys(final Hashtable ht, final boolean edit_vals)
    {
	try
	{
	    JFileChooser jfc = new JFileChooser();
	    jfc.setCurrentDirectory(new File(mview.getProperty("JustOClust.load_path", System.getProperty("user.dir"))));
	
	    int ret_val = jfc.showOpenDialog(frame); 
	    File file;
	
	    if(ret_val == JFileChooser.APPROVE_OPTION) 
	    {
		boolean merge = false;
		if(ht.size() > 0)
		{
		    if(mview.infoQuestion("Replace the current data or merge with the contents of the file?", "Replace", "Merge") == 0)
		    {
			ht.clear();
		    }
		    else
			merge = true;
		}
		
		file = jfc.getSelectedFile();
		
		mview.putProperty("JustOClust.load_path", file.getPath());

		BufferedReader br = new BufferedReader(new FileReader(file));
		
		String str = br.readLine();
		int valid_lines = 0;
		while(str != null)
		{
		    int dpos = str.indexOf('\t');
		    if(dpos > 0)
		    {
			String lhs = str.substring(0, dpos).trim();
			String rhs = str.substring(dpos+1).trim();
			
			boolean valid = (lhs != null);
			if(edit_vals && (rhs == null))
			    valid = false;
			if(valid)
			{
			    ht.put(lhs, rhs);
			    valid_lines++;
			    // System.out.println("'" + lhs + "' -> '" + rhs + "'");
			}
		    }
		    str = br.readLine();
		}
		if(valid_lines == 0)
		{
		    mview.alertMessage("No valid data found in the file");
		}
		else
		{
		    String msg = (valid_lines == 1) ? "One entry" : (valid_lines + " entries");
		    msg += (merge ? " merged" : " loaded");
		    mview.infoMessage(msg);
		}
	    }
	}
	catch (java.io.IOException ioe)
	{
	    mview.errorMessage("Unable to read data\n" + ioe);
	}

    }

    // returns a Hashtable of String:String mappings
    //
    private Hashtable loadTuplesFromFile(String fname)
    {
	File file = new File(fname); //fc.getSelectedFile();

	Hashtable ht = new Hashtable();
	loadHashkeys(ht, true);
	return ht;
    }

    private void saveHashkeys(final Hashtable ht, final boolean edit_vals)
    {
	    
	try
	{
	    JFileChooser jfc = new JFileChooser();
	    jfc.setCurrentDirectory(new File(mview.getProperty("JustOClust.save_path", System.getProperty("user.dir"))));
	
	    int ret_val = jfc.showSaveDialog(frame); 
	    File file;
	
	    if(ret_val == JFileChooser.APPROVE_OPTION) 
	    {
		file = jfc.getSelectedFile();
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		
		for (Enumeration e = ht.keys(); e.hasMoreElements() ;) 
		{
		    String key = (String) e.nextElement();

		    if(edit_vals)
		    {
			String val = (String) ht.get(key);
			writer.write(key + "\t" + val + "\n");
		    }
		    else
		    {
			writer.write(key + "\n");
		    }
		}
		
		writer.close();
		System.out.println("...done");

		mview.putProperty("JustOClust.save_path", jfc.getCurrentDirectory().getPath());
	    }
	}
	catch (java.io.IOException ioe)
	{
	    mview.errorMessage("Unable to write data\n" + ioe);
	}
    }


    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ===== Word Index =======================================================
    //
    // for each unique word in a set of annotations
    //   store all of the positions of that word in each of the annotations
    //
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private class WordIndexHeader
    {
	public String  word;

	public int     word_id;  // allocated automatically

	public Vector  entries;  // Vector of WordIndexEntry's

	public Vector  has_substrings;   // Vector of WordIndexEntry's

	public Vector  is_substring_of;  // Vector of WordIndexEntry's

	public WordIndexHeader(String w, int wid)
	{
	    word = w;
	    word_id = wid;
	    entries = new Vector();

	    has_substrings = new Vector();
	    is_substring_of = new Vector();
	}

	public int getNumEntries() { return entries.size(); }
    }


    private class WordIndexEntry
    {
	int[]   index;    // the index of where the word occur(s) in 'anno_id'
	int     anno_id;
    }
    
    private class WordIndex
    {
	private Hashtable word_to_wih; // maps 'word' to WordIndexHeader
	private int next_word_id = 0;

	public WordIndex()
	{
	    word_to_wih = new Hashtable();
	}

	public Enumeration getNames() 
	{
	    return word_to_wih.keys();
	}

	public WordIndexHeader getEntry(String name)
	{
	    return (WordIndexHeader) word_to_wih.get(name);
	}
	
	public int getNumWords() { return word_to_wih.size(); }

	// record the occurence of a word
	//
	public void addEntry(String name, int anno_id, int index)
	{
	    if(name.length() < min_word_len)
		return;

	    WordIndexHeader wih = getEntry(name);
	    
	    if(wih == null)
	    {
		// this word has not been seen before...
		wih = new WordIndexHeader(name, next_word_id++);
		
		// locate any of the existing words which are substrings of this word
		// 
		for (Enumeration e = word_to_wih.keys(); e.hasMoreElements() ;) 
		{
		    String oname = (String) e.nextElement();
		    if(name.indexOf(oname) >= 0)
		    {
			WordIndexHeader owih = getEntry(oname);
			wih.has_substrings.addElement(owih);
			owih.is_substring_of.addElement(wih);
		    }
		    
		}		
		
		// locate any of the existing words which this word is a substring of
		//
		for (Enumeration e = word_to_wih.keys(); e.hasMoreElements() ;) 
		{
		    String oname = (String) e.nextElement();
		    if(oname.indexOf(name) >= 0)
		    {
			WordIndexHeader owih = getEntry(oname);
			owih.has_substrings.addElement(wih);
			wih.is_substring_of.addElement(owih);
		    }
		    
		}	

		// and store the header into the hashtable
		word_to_wih.put(name, wih);

	    }

	    Vector wiev = wih.entries;
	    WordIndexEntry wie = null;

	    // find the entry corresponding to this anno_id (if any)
	    for(int e=0; e < wiev.size(); e++)
	    {
		if(((WordIndexEntry) wiev.elementAt(e)).anno_id == anno_id)
		{
		    wie = (WordIndexEntry) wiev.elementAt(e);
		    break;
		}
	    }

	    // if we didn't find an existing entry, create one
	    if(wie == null)
	    {
		wie = new WordIndexEntry();
		wie.anno_id = anno_id;
		wie.index   = new int[0];

		wiev.addElement(wie);
	    }

	    // add the index to the existing array...
	    
	    int[] tmp = new int[wie.index.length + 1];
	    for(int t=0; t < wie.index.length; t++)
		tmp[t] = wie.index[t];
	    tmp[wie.index.length] = index;

	    wie.index = tmp;

	    // System.out.println("word '" + wih.word + "' in anno " + wie.anno_id + " at index " + index);
	}
	
	// merges all of the WordIndexEntry's in 'entries' with the existing entries for 'name'
	//
	public void addEntries(String name, Vector entries)
	{
	    for(int e=0; e < entries.size(); e++)
	    {
		WordIndexEntry wie = (WordIndexEntry) entries.elementAt(e);
		for(int i=0; i < wie.index.length; i++)
		    addEntry(name, wie.anno_id, wie.index[i]);
	    }
	}

	// returns a vector of headers for words which occur more than 'min_pc'  time
	// the headers are sorted by the repeat frequency (highest first)
	//
	public WordIndexHeader[] getRepeats(int min_count)
	{
	    Vector reps = new Vector();

	    for (Enumeration e = word_to_wih.keys(); e.hasMoreElements() ;) 
	    {
		String name = (String) e.nextElement();
		WordIndexHeader wih = getEntry(name);
		Vector wiev = wih.entries;
		if(wiev != null)
		{
		    if(wiev.size() > min_count)
		    {
			
			
			reps.addElement( wih );
		    }
		}
	    }

	    WordIndexHeader[] reps_a = (WordIndexHeader[]) reps.toArray(new WordIndexHeader[0]);
	    
	    java.util.Arrays.sort(reps_a, new SortByRepeatFreq());
	    
	    return reps_a;
	}

	// returns a vector of headers for all words, sorted by the frequency (most first)
	//
	public WordIndexHeader[] getWords()
	{
	    Vector reps = new Vector();

	    for (Enumeration e = word_to_wih.keys(); e.hasMoreElements() ;) 
	    {
		String name = (String) e.nextElement();
		WordIndexHeader wih = getEntry(name);
		Vector wiev = wih.entries;
		if(wiev != null)
		{
			reps.addElement( wih );
		}
	    }

	    WordIndexHeader[] reps_a = (WordIndexHeader[]) reps.toArray(new WordIndexHeader[0]);
	    
	    java.util.Arrays.sort(reps_a, new SortByRepeatFreq());
	    
	    return reps_a;
	}


	// debugging
	public void listWords()
	{
	    for (Enumeration w = word_to_wih.keys(); w.hasMoreElements() ;) 
	    {
		String name = (String) w.nextElement();
		WordIndexHeader wih = getEntry(name);
		int total_occ = 0;
		for(int e=0; e < wih.entries.size(); e++)
		{
		    total_occ += ((WordIndexEntry) wih.entries.elementAt(e)).index.length;
		}
		System.out.println(wih.word + " x " + wih.entries.size() + " (" + total_occ + ")");
	    }

	}

	// merge the WordIndexEntry's for any word with those of the word of which it is a substring
	// 
	// i.e. if  'human' and 'subhuman' are both words, then copy all elements in
	//      the vector recording the occurences of 'subhuman' into the vector recording the occurences of 'human'
	//
	public void mergeSubstrings()
	{
	    for (Enumeration e = word_to_wih.keys(); e.hasMoreElements() ;) 
	    {
		String name = (String) e.nextElement();
		WordIndexHeader wih = getEntry(name);
	
		if(wih.has_substrings.size() > 0)
		{
		    for(int ss=0; ss < wih.has_substrings.size(); ss++)
		    {
			WordIndexHeader swih = (WordIndexHeader) wih.has_substrings.elementAt(ss);

			addEntries(swih.word, wih.entries);
		    }
		}

	    }
	}

	private class SortByRepeatFreq implements java.util.Comparator
	{
	    public int compare(Object o1, Object o2)
	    {
		WordIndexHeader wih1 = (WordIndexHeader) o1;
		WordIndexHeader wih2 = (WordIndexHeader) o2;

		return (wih2.entries.size() - wih1.entries.size());
	    }

	    public boolean equals(Object o) { return false; }
	}

    }

   // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ===== the tokeniser ====================================================
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private boolean stopOrTranslateWordThenStore(WordIndex wi, String word, int anno_id, int w_count)
    {
	//String word = case_sens ? sbuf.toString() : (sbuf.toString()).toLowerCase();
	if(!isStopWord(word))
	{
	    String tword = getTranslation( word );
	    wi.addEntry(tword, anno_id, w_count);
	    return true;
	}
	else
	    return false;
    }

    private void addWordsToIndex(WordIndex wi, String text, int anno_id)
    {
	if(text == null)
	    return;

	// text = removeTags(text);

	StringBuffer sbuf = new StringBuffer();

	final int len = text.length();

	int c = 0;
	boolean intok = true;
	int w_count = 0;

	while(c < len)
	{
	    char ch = text.charAt(c++);
	    
	    if(delims.indexOf(ch) >= 0)
	    {
		if(sbuf.length() > 0)
		{
		    String word = case_sens ? sbuf.toString() : (sbuf.toString()).toLowerCase();
		    if(stopOrTranslateWordThenStore(wi, word, anno_id, w_count))
		    {
			w_count++;
		    }
		    sbuf = new StringBuffer();
		}
	    }
	    else
	    {
		sbuf.append(ch);
	    }
	    
	}
	// add the last token (if any)
	if(sbuf.length() > 0)
	{
	    String word = case_sens ? sbuf.toString() : (sbuf.toString()).toLowerCase();
	    if(stopOrTranslateWordThenStore(wi, word, anno_id, w_count))
	    {
		w_count++;
	    }
	}
    }
    
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ===== utilities ========================================================
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private String removeTags(String src)
    {
	StringBuffer sbuf = new StringBuffer();
	final int len = src.length();
	boolean in_tag = false;
	int c = 0;
	while(c < len)
	{
	    char ch = src.charAt(c++);

	    if(ch == '<')
		in_tag = true;

	    if(!in_tag)
		sbuf.append(ch);

	    if(ch == '>')
		in_tag = false;
	}
	return sbuf.toString();
    }

    // ========================================================================================
    // ----------------------------------------------------------------------------------------
    // ---   new version   --------------------------------------------------------------------
    // ----------------------------------------------------------------------------------------
    // ========================================================================================

     
    // 
    //  how to make a WordList for a cluster:
    //
    //    make a WordList for the Spots in the cluster
    // 
    //    if the cluster has children 
    //      
    //        make a WordList for each child
    //      
    //        merge these WordLists with the WordList for this cluster
    // 
    //    endif
    //

    // records the words in common with all elements of a Cluster
    //
    private class DisplayableWordList
    {
	// =============================================================

	Hashtable words_ht; // maps 'token' to 'count'
	String name;
	int max_count;      // how many spots or annos were checked

	DisplayableWordList[] children;

	public int x,y,w,h;

	OrderedWordListEntry[] ordered;

	ExprData.Cluster cluster;   // which cluster is this a list for

	// =============================================================

	public DisplayableWordList( String name_, ExprData.Cluster cluster_)
	{
	    words_ht = new Hashtable();
	    name = name_;
	    cluster =  cluster_;
	    children = null;
	    ordered = new OrderedWordListEntry[0];
	}

	public String toString() { return name; }

	public void addWordsFromSpotIndices( int[] s_ids )
	{
	    final int n_things = s_ids.length;
	    max_count = n_things;

	    boolean substrs = substr_jchkb.isSelected();
		
	    // any words which occur in more than one annotation are stored

	    WordIndex word_index = new WordIndex();

	    //boolean substrs = substr_jchkb.isSelected();
	    //boolean common  = common_jchkb.isSelected();
	    
	    ExprData.NameTagSelection nt_sel = source_nts.getNameTagSelection();
	    
	    for(int s = 0; s < n_things; s++)
	    {
		//anno_source_name[s] = edata.getSpotName(s_ids[s]);
		
		// anno_source_name[s] = nt_sel.getNameTag(s_ids[s]);
		
		if(source_nts.userOptionSelected())
		{
		    String ce = anlo.loadAnnotationFromCache(s_ids[s]);
		    if(ce != null)
			addWordsToIndex(word_index, ce, s);
		}
		else
		{
		    addWordsToIndex(word_index, nt_sel.getNameTag(s_ids[s]), s);
		}
	    }
	    
	    if(substrs)
		word_index.mergeSubstrings();

	    // report(word_index.getNumWords() + " unique words found in annos for " + s_ids.length + " spots");

	    // now build the hashtable using the WordIndex

	    int min_repeat_count = (int)((double)(max_count) * (double)min_repeat_pc * 0.01);
	    WordIndexHeader[] repeats = word_index.getRepeats( min_repeat_count);

	    // System.out.println(name + " sp=" + n_things + " mrc=" + min_repeat_count + " n.reps=" + repeats.length);

	    // WordIndexHeader[] unique_words = word_index.getWords();
	    
	    for(int r=0; r < repeats.length; r++)
	    {
		WordIndexHeader wih = repeats[r];

		words_ht.put(wih.word, new Integer(wih.entries.size()));
		
		// System.out.println(wih.entries.size() + " x " + wih.word);
	    }

	    createOrderedCounts();
	}

	public void intersect(DisplayableWordList wl)
	{

	}

	// combine this data with another WordList

	public void merge(DisplayableWordList wl)
	{
	     // foreach entry X in wl
	    //    if X.name is known
	    //       add both 'count's and store in ht
	    //    else
	    //       store X in ht

	    max_count += wl.max_count;

	    for (Enumeration e = wl.words_ht.keys(); e.hasMoreElements() ;) 
	    {
		String token      = (String)  e.nextElement();
		Integer this_count = (Integer) words_ht.get( token );
		Integer that_count = (Integer) wl.words_ht.get( token );

		if(this_count == null)
		{
		    words_ht.put( token, that_count );
		}
		else
		{
		    int sum = this_count.intValue() + that_count.intValue();
		    words_ht.put( token, new Integer( sum ));
		}
	    }
	    createOrderedCounts();
	}

	
	private class OrderedWordListEntry
	{
	    String word;
	    String count_pc_str;
	    int count_pc_i;
	    int count_i;

	    public OrderedWordListEntry(String w, int c, String cps, int cpi)
	    {
		word = w;
		count_i = c;
		count_pc_str = cps;
		count_pc_i = cpi;
	    }

	}
	
	private class OrderedWordListEntryComparator implements java.util.Comparator
	{
	    public int compare(Object o1, Object o2)
	    {
		OrderedWordListEntry wle1 = (OrderedWordListEntry) o1;
		OrderedWordListEntry wle2 = (OrderedWordListEntry) o2;
		
		return wle2.count_pc_i - wle1.count_pc_i;
	    }
	    
	    public boolean equals(Object o) { return false; }
	}

	private void createOrderedCounts()
	{
	    final int n_words = words_ht.size();

	    Vector ordered_v = new Vector();

	    int nw = 0;
	    
	    // keep all entries where pc > min_repeat_pc
	    //
	    for (Enumeration e = words_ht.keys(); e.hasMoreElements() ;) 
	    {
		String token  = (String)  e.nextElement();
		Integer count = (Integer) words_ht.get(token);
		int count_i = count.intValue();
		if(count_i > 1)
		{
		    double pc_d = ((double) count_i * 100.0) / (double) max_count;
		    int pc_i = (int) pc_d;
		    if(pc_i >= min_repeat_pc)
			ordered_v.addElement(new OrderedWordListEntry( token, count_i, mview.niceDouble( pc_d, 5, 2 ), pc_i ));
		}
	    }

	    // convert to array and sort
	    //
	    ordered = (OrderedWordListEntry[]) ordered_v.toArray(new OrderedWordListEntry[0]);

	    if(ordered == null)
		ordered = new OrderedWordListEntry[0];
	    else
		java.util.Arrays.sort( ordered, new OrderedWordListEntryComparator());
	}

	public void dump()
	{
	    System.out.println("word list for " + name + ", max_count = " + max_count );

	    for (Enumeration e = words_ht.keys(); e.hasMoreElements() ;) 
	    {
		String token  = (String)  e.nextElement();
		Integer count = (Integer) words_ht.get(token);
		System.out.println(count + " x " + token);
	    }
	}

	// start at the first child of the first child of the first etc...
	// return the height of the children
	private int layout( int top, int left, int text_height )
	{
	    final int list_v_gap = 20;
	    final int list_h_gap = 30;

	    w = 200;
	    // h = 6 + (words_ht.size() * text_height);
	    h = 6 + (ordered == null ? 0 : (ordered.length * text_height));
	    
	    if(children != null)
	    {
		int c_top = top;

		for(int c = 0; c < children.length; c++)
		{
		    c_top += children[c].layout( c_top, left + w + list_h_gap, text_height ) + list_v_gap;
		}

		x = left;
		y = (children[0].y + children[children.length-1].y) / 2;

		// System.out.println(name + " is " + w + "x" + h + " @ " + x + "," + y);
		
		// return the max of this nodes height or its total childrens height
		int c_height = (c_top - top);
		int t_height = h > c_height ? h : c_height;
		return list_v_gap + t_height;
	    }
	    else
	    {
		x = left;
		y = top;
		
		// System.out.println(name + " is " + w + "x" + h + " @ " + x + "," + y);
		
		return list_v_gap + h;
	    }
	}

	// ====================================

	private WordListExtent findExtent()
	{
	    WordListExtent ne = new WordListExtent();
	    
	    ne.minx = ne.miny = Integer.MAX_VALUE;
	    ne.maxx = ne.maxy = -Integer.MAX_VALUE;
	    
	    return recursivelyfindExtent(ne);
	}
	
	private WordListExtent recursivelyfindExtent(WordListExtent e)
	{
	    if(x < e.minx)
		e.minx = x;
	    if((x+w) > e.maxx)
		e.maxx = (x+w);
	    if(y < e.miny)
		e.miny = y;
	    if((y+h) > e.maxy)
		e.maxy = (y+h);
	    
	    if(children != null)
	    {
		for(int c = 0; c < children.length; c++)
		{
		    WordListExtent ce = children[c].recursivelyfindExtent( e );
		    e.mergeWith( ce );
		}
	    }
	    return e;
	}

	// ===================================

	private void translate()
	{
	    WordListExtent wle = findExtent();

	    // System.out.println("shifting by " + wle.minx + "," + wle.maxx);

	    wle.minx -= 10;
	    wle.miny -= 20;

	    translate(-wle.minx, -wle.miny);

	    wle.maxx += 10;
	    wle.maxy += 10;
	    
	    // System.out.println("size is  " +  (wle.maxx - wle.minx) + "x" + (wle.maxy - wle.miny));

	    word_list_panel.setPreferredSize( new Dimension( wle.maxx - wle.minx, wle.maxy - wle.miny ) );
	    // word_list_panel.setMinimumSize( new Dimension( wle.maxx - wle.minx, wle.maxy - wle.miny ) );
	    // word_list_panel.setSize( new Dimension( wle.maxx - wle.minx, wle.maxy - wle.miny ) );

	    // this lets the scroll pane know to update itself and its scroll bars.
	    word_list_panel.revalidate();
	}

	private void translate(int dx, int dy)
	{
	    x += dx; 
	    y += dy;

	    if(children != null)
	    {
		for(int c = 0; c < children.length; c++)
		{
		    children[c].translate(dx,dy);
		}
	    }
	}

	/*
	private void scale(double s)
	{
	    x = (int)((double)x * s);
	    y = (int)((double)y * s);

	    w = (int)((double)w * s);
	    h = (int)((double)h * s);

	    if(children != null)
	    {
		for(int c = 0; c < children.length; c++)
		{
		    children[c].scale(s);
		}
	    }
	}
	*/

	// ====================================

	// returns the word that was clicked on in this list or
	// in any of its children or null if the click wasn't on a word
	//
	public String mouseClicked(int mex, int mey)
	{
	    if((mex > x) && (mex < (x+w)) &&
	       (mey > y) && (mey < (y+h)))
	    {
		// System.out.println("-> click in " + name);
		
		int word = (mey - y - 3) / word_list_panel.text_height;
		
		// System.out.println("   word =  " + ordered[word].word);

		return ordered[word].word;
	    }
	    else
	    {
		if(children != null)
		{
		    for(int c = 0; c < children.length; c++)
		    {
			String word = children[c].mouseClicked(mex,mey);
			if(word != null)
			    return word;
		    }
		}
		return null;
	    }
	}

	// returns true if the click occurs over any of the 'buttons'
	//
	public boolean mouseClickIsOverButton(int mex, int mey)
	{
	    if((mex > (x+w-11)) && (mex < (x+w)) &&
	       (mey > y) && (mey < (y+h)))
	    {
		return true;
	    }
	    else
	    {
		if(children != null)
		{
		    for(int c = 0; c < children.length; c++)
		    {
			if(children[c].mouseClickIsOverButton(mex,mey))
			    return true;
		    }
		}
		return false;
	    }
	}

	// returns the object which matches the name, or null if no match found
	//
	public DisplayableWordList matchByName(String n) 
	{
	    if(n.equals(name))
	    {
		return this;
	    }
	    else
	    {
		if(children != null)
		{
		    for(int c = 0; c < children.length; c++)
		    {
			DisplayableWordList dwl = children[c].matchByName(n);
			if(dwl != null)
			    return dwl;
		    }
		}
		return null;
	    }
	}
 
	// returns the object which matches the name, or null if no match found
	//
	public DisplayableWordList matchByCluster(ExprData.Cluster cl) 
	{
	    if(cl == cluster)
	    {
		return this;
	    }
	    else
	    {
		if(children != null)
		{
		    for(int c = 0; c < children.length; c++)
		    {
			DisplayableWordList dwl = children[c].matchByCluster(cl);
			if(dwl != null)
			    return dwl;
		    }
		}
		return null;
	    }
	}

	public DisplayableWordList matchByPosition(int mex, int mey)
	{
	    if((mex > x) && (mex < (x+w)) &&
	       (mey > y) && (mey < (y+h)))
	    {
		return this;
	    }
	    else
	    {
		if(children != null)
		{
		    for(int c = 0; c < children.length; c++)
		    {
			DisplayableWordList dwl = children[c].matchByPosition(mex, mey);
			if(dwl != null)
			    return dwl;
		    }
		}
		return null;
	    }
	}
	
	// ====================================

	//  generates a Vector containing the names of all of the WordLists (i.e. the cluster names)
	//
	public Vector makeClusterNameVector()
	{
	    Vector data = new Vector();
	    makeClusterNameVector(data, 0);
	    return data;
	}

	private void makeClusterNameVector(final Vector v, final int depth)
	{
	    String pad = "";
	    for(int p=0; p < depth; p++)
		pad += "  ";
	    v.addElement( pad + name );

	    if(children != null)
	    {
		for(int c = 0; c < children.length; c++)
		{
		    children[c].makeClusterNameVector(v, depth+1);
		}
	    }
	}
	
	//  generates a Vector containing all of the words in all of the lists
	//
	public Vector makeWordVector()
	{
	    Vector data = new Vector();
	    makeWordVector(data);

	    // make this data unique...
	    Hashtable ht = new Hashtable();
	    
	    for(int w=0; w < data.size(); w++)
	    {
		String wr = (String) data.elementAt(w);
		ht.put(wr, wr);
	    }
	    Vector unique_data = new Vector();

	    for (Enumeration e = words_ht.keys(); e.hasMoreElements() ;) 
	    {
		String token  = (String)  e.nextElement();
		unique_data.addElement(token);
	    }

	    return unique_data;
	}
	private void makeWordVector(final Vector v)
	{
	    for(int w=0; w < ordered.length; w++)
		v.addElement( ordered[w].word );
	    
	    if(children != null)
	    {
		for(int c = 0; c < children.length; c++)
		{
		    children[c].makeWordVector(v);
		}
	    }
	}
		
    }
	
    private class WordListExtent
    {
	public int minx, maxx, miny , maxy;
	
	public void mergeWith(WordListExtent ne)
	{
	    if(ne.minx < minx)
		minx = ne.minx;
	    if(ne.maxx > maxx)
		maxx = ne.maxx;
	    if(ne.miny < miny)
		miny = ne.miny;
	    if(ne.maxy > maxy)
		maxy = ne.maxy;
	}
    }
	
	
    private DisplayableWordList makeWordList( ExprData.Cluster cl)
    {
	// report progress
	status_label.setText(cl.getName());

	//  make a WordList for the Spots in the cluster
	// 
	DisplayableWordList wl = new DisplayableWordList( cl.getName(), cl );

	if( cl.getIsSpot() && (cl.getNumElements() > 0) )
	{
	    wl.addWordsFromSpotIndices( cl.getElements() );
	}
	
	//  if the cluster has children 
	//      
	//      make a WordList for each child
	//      
	//      merge these WordLists with the WordList for this cluster
	// 
	//  endif
	
	final int n_kids = cl.getNumChildren();

	if(n_kids > 0)
	{
	    wl.children = new DisplayableWordList[ n_kids ];
	    
	    for(int c=0; c < n_kids; c++)
	    {
		ExprData.Cluster ch = (ExprData.Cluster)(cl.getChildren().elementAt(c));
		
		wl.children[c] = makeWordList( ch );
		
		// c_top += wl.children[c].h + list_v_gap;
		
		wl.merge( wl.children[c] );
	    }
	}

	// and position this WordList
	// wl.layout( top, left, text_height );

	// wl.dump();
	
	return wl;
    }

    /*
    private void makeWordList(Vector msgs, String pname, ExprData.Cluster cl)
    {
	String lname   = ((pname == null) ? "" : (pname + ".")) + cl.getName();
	String message = lname + "\n";

	
	for(int c=0; c < cl.getNumChildren(); c++)
	{
	    ExprData.Cluster ch = (ExprData.Cluster)(cl.getChildren().elementAt(c));
	    reportCommonWords(msgs, lname, ch);
	}
	
	msgs.addElement( message );
    }
    */
    
    public class WordListPanel extends DragAndDropPanel
    {
	DisplayableWordList root_dwl;
	Hashtable selected_words;

	Color in_c, out_c;
	public int text_height;

	public WordListPanel()
	{
	    MouseListener mouse_listener = new MouseAdapter() 
		{
		    public void mouseClicked(MouseEvent e) 
		    {
			int mex = fromScaled(e.getX());
			int mey = fromScaled(e.getY());
			
			String word = root_dwl.mouseClicked(mex, mey);
			if(word != null)
			{
			    // was the click over the 'button' ?
			    //
			    if(root_dwl.mouseClickIsOverButton(mex, mey))
			    {
				// display more information about this word in this cluster
				DisplayableWordList dwl = root_dwl.matchByPosition(mex, mey);
				if(dwl != null)
				{
				    // System.out.println("context button click on '" + word + "' in '" + dwl.name + "'");
				    displayContext( word, dwl );
				}
			    }
			    else
			    {
				// toggle the word in the selection
				//
				if(selected_words.get(word) != null)
				{
				    selected_words.remove(word);
				}
				else
				{
				    selected_words.put(word, word);
				}
			    }
			}
			else // the click did not occur over any of the words
			{
			    selected_words.clear(); 
			}
			
			repaint();
		    }
		};

	    addMouseListener(mouse_listener);
	    

	    setDropAction(new DragAndDropPanel.DropAction()
		{
		    public void dropped(DragAndDropEntity dnde)
		    {
			try
			{
			    ExprData.Cluster cl = dnde.getCluster();
			    System.out.println(cl.getName() + " dropped");
			    
			    DisplayableWordList dwl = root_dwl.matchByName( cl.getName() );

			    if(dwl != null)
				scrollToList( dwl );
			}
			catch(DragAndDropEntity.WrongEntityException another_wee)
			{
			    System.out.println("not a cluster");
			}
		    }
		});

	    setDragAction(new DragAndDropPanel.DragAction()
		{
		    public DragAndDropEntity getEntity(java.awt.dnd.DragGestureEvent event)
		    {
			Point pt = event.getDragOrigin();
			DisplayableWordList dwl = root_dwl.matchByPosition( fromScaled(pt.x), fromScaled(pt.y) ); 
			if(dwl != null)
			{
			    return  DragAndDropEntity.createClusterEntity(dwl.cluster);
			}
			else
			    return null;  
		    }
		});

	    // ===================================

	    selected_words = new Hashtable();

	    in_c  = new Color(200, 200, 200);
	    out_c = new Color(230, 230, 230);

	    //final FontMetrics fm  = new JPanel().getGraphics().getFontMetrics();
	    //text_height = fm.getAscent()/* + fm.getDescent()*/;
	    text_height = 12;

	}

	public void paintComponent(Graphics g)
	{
	    
	    // System.out.println("repainting....");
	    int width  = word_list_panel_jsp.getWidth();
	    int height = word_list_panel_jsp.getHeight();

	    g.setColor(Color.white);
	    g.fillRect(0,0,getWidth(),getHeight());

	    if(root_dwl == null)
		return;

	    g.setFont(new Font("Helvetica", Font.PLAIN, toScaled(text_height)));
	    drawWordList(g, root_dwl, 0, 0, width, height);
	    g.setColor(Color.red);
	    linkSelectedWords(g, root_dwl);
	}

	private void scrollToList(DisplayableWordList dwl)
	{
	    //System.out.println("scrolling to " + dwl.name);
	    //	    scrollRectToVisible( new Rectangle(dwl.x - 32, dwl.y - 32, 64, 64) ); 
	    scrollRectToVisible( new Rectangle(toScaled(dwl.x - 32), toScaled(dwl.y - 32), 
					       toScaled(dwl.w + 64), toScaled(dwl.h + 64) ) ); 
	}

	private int toScaled(int v)
	{
	    return (int)((double) v * scale_factor);
	}
	
	private int fromScaled(int v)
	{
	    return (int)((double) v / scale_factor);
	}

	private void drawWordList(Graphics g, DisplayableWordList dwl, int xoff, int yoff, int width, int height)
	{
	    final int border_gap = 3;

	    // if((dwl.x < width) && (dwl.y < height))

	    g.setColor(Color.blue);
	    g.drawString(dwl.name + " ( " + String.valueOf(dwl.max_count) + " )", toScaled(dwl.x), toScaled(dwl.y-1));

	    g.setColor(Color.black);
	    g.drawRect(toScaled(dwl.x), toScaled(dwl.y), toScaled(dwl.w), toScaled(dwl.h));

	    final int nw = dwl.words_ht.size();
	    
	    
	    int wxp = dwl.x + border_gap;
	    int wyp = dwl.y + border_gap;
	    
	    // draw a bargraph of the percentages as a background 

	    for(int w=0; w < dwl.ordered.length; w++)
	    {
		int a_w =  dwl.w- (2 * border_gap);
		int pc_w =  (dwl.ordered[w].count_pc_i * a_w) / 100;
		int o_pc_w = a_w - pc_w;

		g.setColor(in_c);
		g.fillRect( toScaled(wxp), toScaled(wyp), toScaled(pc_w), toScaled(text_height-1));

		g.setColor(out_c);
		g.fillRect( toScaled(wxp + pc_w), toScaled(wyp), toScaled(o_pc_w), toScaled(text_height-1));

		wyp += text_height;
	    }

	    // now draw the words and their percentages

	    wyp = dwl.y + border_gap;
	    
	    for(int w=0; w < dwl.ordered.length; w++)
	    {
		g.setColor( (selected_words.get( dwl.ordered[w].word ) == null) ? Color.black : Color.red );
		
		// a little 'button' for each word
		g.drawRect( toScaled(wxp + dwl.w - 11), toScaled(wyp+3), toScaled(5), toScaled(5) );

		// and the words and percentages
		wyp += text_height;
		
		g.drawString( dwl.ordered[w].word,         toScaled(wxp), toScaled(wyp));
		g.drawString( dwl.ordered[w].count_pc_str, toScaled(wxp + dwl.w - 50), toScaled(wyp));

		//g.drawString( String.valueOf(dwl.ordered[w].count_i), wxp + dwl.w - 100, wyp);
	    }
	    
	    if(dwl.children != null)
	    {
		int sx = dwl.x + dwl.w;            // start point of joining line
		int sy = dwl.y + (dwl.h / 2);

		if(dwl.children.length > 1)
		{
		    int mx = (sx + dwl.children[0].x) / 2;  // mid-point of joining line
		    
		    g.setColor(Color.black);
		    g.drawLine(toScaled(sx), toScaled(sy), toScaled(mx), toScaled(sy)); // first horizontal segment
		    
		    for(int c=0; c < dwl.children.length; c++)
		    {
			int cx = dwl.children[c].x;
			int cy = dwl.children[c].y + (dwl.children[c].h / 2);
			
			// draw the line to this child
			g.setColor(Color.black);
			g.drawLine(toScaled(mx), toScaled(cy), toScaled(cx), toScaled(cy));    // second horizontal segment
			
			// and the vertical line encompasing all children
			if(c == 0)
			{
			    int last_c = dwl.children.length - 1;
			    int ey = dwl.children[last_c].y + (dwl.children[last_c].h / 2);
			    
			    g.drawLine(toScaled(mx), toScaled(cy), toScaled(mx), toScaled(ey));
			}
			
			drawWordList(g, dwl.children[c], xoff, yoff, width, height);
			
		    }
		}
		else
		{
		    // special case for joining only children
		    int cx = dwl.children[0].x;
		    int cy = dwl.children[0].y + (dwl.children[0].h / 2);
		    g.setColor(Color.black);
		    
		    // single joining segment
		    if(cy > sy)
			g.drawLine(toScaled(sx), toScaled(sy), toScaled(cx), toScaled(sy));  
		    else
			g.drawLine(toScaled(sx), toScaled(cy), toScaled(cx), toScaled(cy));
			
		    drawWordList(g, dwl.children[0], xoff, yoff, width, height);
		}
	    }
	    
	}

	private void linkSelectedWords(Graphics g, DisplayableWordList dwl)
	{
	    for (Enumeration e = selected_words.keys(); e.hasMoreElements() ;) 
	    {
		String word  = (String)  e.nextElement();

		// draw a line from this word in this list to the word in each
		// of the children lists
		
		int i = 0;

		try
		{
		    if(dwl.children != null)
		    {
			while( ! dwl.ordered[i].word.equals( word ))
			    i++;
			
			int px = dwl.x + 3;
			int py = dwl.y + (++i * text_height) /*+ (text_height/2)*/;
			
			for(int c = 0; c < dwl.children.length; c++)
			{
			    drawLinkFrom(g, px, py, word, dwl.children[c]);
			}
		    }
		}
		catch(ArrayIndexOutOfBoundsException aioobe)
		{
		}
	    }

	    // now recursively do the same thing to each of the children
	    if(dwl.children != null)
	    {
		for(int c = 0; c < dwl.children.length; c++)
		{
		    linkSelectedWords(g, dwl.children[c]);
		}
	    }
	}

	private void drawLinkFrom(Graphics g, int px, int py, String word, DisplayableWordList dwl)
	{
	    int i = 0;
	    
	    try
	    {
		while( ! dwl.ordered[i].word.equals( word ))
		    i++;

		int tx = dwl.x + 3;
		int ty = dwl.y + (++i * text_height)/* + (text_height/2)*/ ;

		g.drawLine(toScaled(px), toScaled(py), toScaled(tx), toScaled(ty));
	    }
	    catch(ArrayIndexOutOfBoundsException aioobe)
	    {
	    }
	}

	public double getScale()
	{
	    return scale_factor;
	}

	public void setScale(double sf)
	{
	    scale_factor = sf;
	    repaint();
	}

	public DisplayableWordList getWordList()
	{
	    return root_dwl;
	}

	public void setWordList(DisplayableWordList dwl_)
	{
	    frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

	    root_dwl = dwl_;
	    
	    updatePickerPanel();
	    
	    if( root_dwl != null )
		scrollToList( root_dwl );

	    frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

	    repaint();
	}
	
	/*
	private void updatePickerList()
	{
	    int things = 0;
	    String[] data;

	    if(pick_clusters)
	    {
		Vector v = root_dwl.makeClusterNameVector();

		// cluster_picker_tree.setSelectedValue( v );

		
	    }
	    else
	    {
		// pick words....
		
		
	    }
	}
        */


    }


    // ========================================================================================
    // ----------------------------------------------------------------------------------------
    // ---   context viewer   -----------------------------------------------------------------
    // ----------------------------------------------------------------------------------------
    // ========================================================================================

    public void displayContext( final String word, final DisplayableWordList dwl)
    {

	final JFrame cframe = new JFrame( "Context: '" + word + "' in " + dwl.name);

	cframe.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	
	final DragAndDropTable ctable = new DragAndDropTable();
	ctable.setFont( new Font( "Courier", Font.PLAIN, 12 ));

	// ====================================

	
	// ====================================

	final NameTagSelector label_nts = new NameTagSelector(mview);

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
	    // spot label mode selector
	    //
	    {
		label_nts.loadSelection("JustOClust.spot_label");
		label_nts.addActionListener(new ActionListener() 
		    { 
			public void actionPerformed(ActionEvent e) 
			{
			    // System.out.println("spot label changed");
			    populateContextTable( ctable, word, dwl, label_nts );
			    label_nts.saveSelection("JustOClust.spot_label");
			}
		    });
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		//c.weighty = 2.0;
		c.weightx = 10.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		//c.anchor = GridBagConstraints.SOUTH;
		w_gridbag.setConstraints(label_nts, c);
		wrapper.add(label_nts);
	    }

	    {
		Dimension fillsize = new Dimension(16,16);
		Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 0;
		w_gridbag.setConstraints(filler, c);
		wrapper.add(filler);
						   
	    }

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
			    mview.getPluginHelpTopic("JustOClust", "JustOClust", "#context");
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

	final int[] row_to_sid_map = populateContextTable( ctable, word, dwl, label_nts);

	ctable.setDragAction(new DragAndDropTable.DragAction()
	    {
		public DragAndDropEntity getEntity()
		{
		    int row = ctable.getSelectedRow();
		    int col = ctable.getSelectedColumn();
		    
		    if(col == 0)
		    {
			ExprData.Cluster cl = (ExprData.Cluster) (((DefaultTableModel)ctable.getModel()).getValueAt(row, col));
			if(cl != null )
			    return DragAndDropEntity.createClusterEntity(cl);
		    }
		    else
		    {
			if(col == 1)
			{
			    DragAndDropEntity dnde = DragAndDropEntity.createSpotNameEntity( row_to_sid_map[ row ]);
			    
			    return dnde;
			}
		    }
		    return null;
		}
	    });
	
	cframe.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    // returns an array of spot ids which indicate which spot has been used in each row
    // of the table
    public int[] populateContextTable( JTable ctable, String word, DisplayableWordList dwl, NameTagSelector label_nts)
    {
	// cluster_name, spot_tag, context_text

	Vector good_data = new Vector();
	Vector good_sids = new Vector();

	Vector bad_data = new Vector();
	Vector bad_sids = new Vector();

	ExprData.NameTagSelection label_nt_sel = label_nts.getNameTagSelection();
	
	addContextInfo( word, dwl.cluster, 0, good_data, good_sids, bad_data, bad_sids, label_nt_sel );
	
	// put the 'bad' (i.e. unmatched) data after the 'good' (i.e. matched) data
	//
	for(int b=0; b < bad_data.size(); b++)
	{
	    good_data.addElement( bad_data.elementAt(b) );
	    good_sids.addElement( bad_sids.elementAt(b) );
	}

	// this reverse mapping is needed for drag and drop
	int[] sids_a = new int[good_sids.size()];
	for(int s=0; s < sids_a.length; s++)
	    sids_a[s] = ((Integer)good_sids.elementAt(s)).intValue();
	
	Vector colnames = new Vector();
	colnames.addElement("Cluster");
	colnames.addElement(label_nt_sel.getNames());
	colnames.addElement("Context");

	ctable.setModel( new DefaultTableModel( good_data, colnames ));

	int tw = ctable.getWidth();
	
	TableColumn column = null;
	column = ctable.getColumnModel().getColumn(0);
	column.setWidth(tw / 6);
	column.setPreferredWidth(tw / 6);

	column = ctable.getColumnModel().getColumn(1);
	column.setWidth(tw / 6);
	column.setPreferredWidth(tw / 6);
	
	column = ctable.getColumnModel().getColumn(2);
	column.setWidth((2 * tw) / 3);
	column.setPreferredWidth((2 * tw) / 3);
	
	ctable.revalidate();

	return sids_a;
    }

    private void addContextInfo( String word, 
				 ExprData.Cluster cl, 
				 int depth,  
				 Vector good_data, Vector good_sids, 
				 Vector bad_data, Vector bad_sids,
				 ExprData.NameTagSelection label_nt_sel )
    {
	// check each of the spots in this dwl....

	ExprData.NameTagSelection anno_nt_sel = source_nts.getNameTagSelection();

	final int[] s_ids = cl.getElements();

	if(s_ids != null)
	{

	    System.out.println("addContextInfo() " + cl.getName() + " has " + s_ids.length + " elements");

	    // store the non-matches in a separate vector and
	    // then bung them all into 'data' at the end so they
	    // are together in the table
	    //
	    
	    for(int s=0; s < s_ids.length; s++)
	    {
		Vector data_line = new Vector();
		
		data_line.addElement( cl );
		
		data_line.addElement( label_nt_sel.getNameTag(s_ids[s]) );
		
		    
		// does the word occur in the anno for this spot?
		String anno = null;
		if(source_nts.userOptionSelected())
		{
		    anno = anlo.loadAnnotationFromCache(s_ids[s]);
		}
		else
		{
		    anno = anno_nt_sel.getNameTag(s_ids[s]);
		}
		
		if(anno != null)
		{
		    String anno_t = case_sens ? anno : anno.toLowerCase();
		    
		    int index = anno_t.indexOf( word );
		    
		    // System.out.println("checking '" + anno_t + "'");
		    
		    if(index >= 0)
		    {
			int endp   = index + word.length() + 20;
			int startp = index - 20;

			boolean end_trimmed = true;
			
			if(startp < 0)
			    startp = 0;
			if(endp >= anno.length())
			{
			    endp = anno.length() - 1;
			    end_trimmed = false;
			}
			
			String ctxt = anno.substring( startp, endp + 1);
			
			if(index < 20)
			{
			    String cpad = "";
			    for(int p=index; p < (20+3); p++)  // the extra 3 is for the "..."
				cpad += " ";
			    ctxt = cpad + ctxt;
			}
			else
			{
			    ctxt = "..." + ctxt;
			}
			
			if(end_trimmed)
			    ctxt = ctxt + "...";
			
			data_line.addElement( ctxt );
			good_data.addElement( data_line );
			good_sids.addElement( new Integer(s_ids[s]) );
		    }
		    else
		    {
			data_line.addElement( "[no match]" );
			bad_data.addElement( data_line );
			bad_sids.addElement( new Integer(s_ids[s]) );			
		    }
		    

		}
		else
		{
		    data_line.addElement( "[no text]" );
		    bad_data.addElement( data_line );
		    bad_sids.addElement( new Integer(s_ids[s]) );		
		}
	    }
	    
	    // add in the bad sids....
	}
	
	// now do the children

	for(int c=0; c < cl.getNumChildren(); c++)
	{
	    ExprData.Cluster ch = (ExprData.Cluster) cl.getChildren().elementAt(c);
	    addContextInfo( word, ch, depth+1, good_data, good_sids, bad_data, bad_sids, label_nt_sel );
	}

	/*
	if(dwl.children != null)
	{
	    for(int c = 0; c < dwl.children.length; c++)
	    {
		addContextInfo( word, dwl.children[c], depth+1, good_data, good_sids, bad_data, bad_sids, label_nt_sel );
	    }
	}
	*/
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  gubbins
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  


    String delims = " \"\n:;()-{}[],.<>";
    boolean case_sens = false;

    int min_common_word_pc = 30;
    int min_word_len = 3;
    int min_repeat_pc = 10;

    private ExprData.Cluster selected_cluster = null;

    String cur_path = null;

    int total_tokens = 0;

    private maxdView mview;
    private DataPlot dplot;
    private ExprData edata;
    private AnnotationLoader anlo;

    private double scale_factor = 1.0;

    private NameTagSelector source_nts;

    private JTextField top_N_tok_jtf, min_wlen_jtf, min_repeat_pc_jtf;
    private JLabel names_label;
    private JCheckBox groups_jchkb, substr_jchkb, common_jchkb, case_sens_jchkb;
    private JSlider scale_slider;

    private DragAndDropTree cluster_tree;

    private WordListPanel   word_list_panel;

    private DragAndDropTree word_picker_tree;

    private boolean         pick_clusters = true;
    private DragAndDropTree cluster_picker_tree = null;
    private DragAndDropList word_picker_list = null; 
    private JScrollPane     picker_jsp = null;
    private JLabel          status_label;

    private JScrollPane     word_list_panel_jsp;

    private JFrame     frame = null;
}
