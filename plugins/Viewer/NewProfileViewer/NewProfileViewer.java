import java.io.*;

import javax.swing.*;
import javax.swing.event.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

import java.awt.font.*;
import java.awt.geom.*;
import java.awt.dnd.*;

import javax.swing.JTree;
import javax.swing.tree.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreeSelectionModel;

//
//
//  profile viewer displays the profile of one or more spots for one or more measurements  
//
//  spots can be picked based on names, attrs or by clusters
//  
//  likewise, measurements can be picked based on names, name-attrs, spot-attrs or by clusters
//
//      eg 
//
//       meas1       meas2       meas3       meas4       meas5       meas6
//       --------------------------------------------------------------------
//       strain=A    strain=A    strain=A    strain=B    strain=B    strain=B   
//       time=0      time=1      time=2      time=0      time=1      time=2
//
//
//          
//   if we also know that 'strain' is an organising factor, we could plot a profile graph:
//
//                                   ____________spotX in strainB
//              |            _____  /
//              |           /     \/
//              |          /      /\
//              | ________/      /  \
//              |               /    \__________ spotX in strainA
//              | _____________/
//              |____________________________________
//                    |           |             |
//                    t0          t1            t2
//
//
//    how do we know to go <m1,m2,m3> and <m4,m5,m6>  and not  <m1,m5,m3> and  <m4,m2,m6>  ??
//  
//    answer; there would have to be another attribute which matched
//    said <m1,m2,m3> are grouped as "healthy" and <m4,m5,m6> are
//    "borked"
//            
    
//   or alternately as a bar chart (showing the same thing)
//
//              | 
//              |      0                1 
//              |      0   2            1
//              |      0   2            1 2 
//              |      0 1 2            1 2
//              |      0 1 2          0 1 2
//              |____________________________________
//                       |               |       
//                    strainA         strainB
//

//
//      without grouping, we know we need to have one profile for each
//      possible set of meaasurements with (t0, t1, t2) namely (m1,
//      m2, m3) and (m4, m5, m6)... but what about other possible
//      attrs - eg data type or something - how do we decide how to 
//      arrange the measurements?
//
//      
//      also might like to have 2 adjacent graphs , one for A one for B
//
// 
//    1. pick a set of spots to profile...
//
//    2. pick a set of measurements to place along axis (possibly pick an attribute and derive the measurements from that)
//
//    3. pick a an attribute to group by (optional, the set of possible groups will be determined by the choice made in step 2)
//
//      
//
//
//      
//
//
//      
//
//
//
//      
//
//

public class NewProfileViewer implements ExprData.ExprDataObserver, Plugin, ExprData.ExternalSelectionListener
{
    public NewProfileViewer( final maxdView mview )
    {
	this.mview = mview;
    }

    private void buildGUI()
    {
	//sel_cls = new Vector();
	//sel_cl_ids = new Vector();
	//meas_ids = new int[0];

	frame = new JFrame ("New Profile Viewer");

	mview.decorateFrame(frame);

	frame.addWindowListener(new WindowAdapter() 
	    {
		public void windowClosing(WindowEvent e)
		{
		    cleanUp();
		}
	    });


	
	JPanel panel = new JPanel();
	GridBagLayout gridbag = new GridBagLayout();
	panel.setLayout(gridbag);

	panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	
	frame.getContentPane().add(panel);
	
	GridBagConstraints c = null;


	// =========================================================


	spot_picker = new SpotPicker( mview, edata, this );

	meas_picker = new MeasPicker( mview, edata, this );

	group_picker = new MeasGroupPicker( mview, edata, this );

	layout_panel = new GraphLayoutPanel();

	options_panel = new OptionsPanel();

	selector = new PanelSelector( mview );

	selector.add( "Spots",         spot_picker );
	selector.add( "Measurements",  meas_picker );
	selector.add( "Grouping",      group_picker );
	selector.add( "Title & Axes",  layout_panel );
	selector.add( "Options",       options_panel );
	
	pro_panel = new ProfilePanel( mview, edata, this );

	pro_panel.setPreferredSize( new Dimension( 640, 480 ) );

	JSplitPane jsplt_pane1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

	jsplt_pane1.setLeftComponent( selector );

	jsplt_pane1.setRightComponent( pro_panel );

	jsplt_pane1.setOneTouchExpandable(true);
	
	
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	c.weighty = 9.0;
	c.weightx = 10.0;
	c.fill = GridBagConstraints.BOTH;
	gridbag.setConstraints(jsplt_pane1, c);
	panel.add( jsplt_pane1 );


	// =========================================================
	

	/*
	pro_panel.setDropAction(new DragAndDropPanel.DropAction()
	    {
		public void dropped(DragAndDropEntity dnde)
		{
		    
		}
	    });
	
	pro_panel.setDragAction(new DragAndDropPanel.DragAction()
	    {
		public DragAndDropEntity getEntity(DragGestureEvent event)
		{
		    return null;
		}
	    });;
	*/   


	// ========================================================================================


	{
	    // 
	    // bottom button panel
	    //
	    JButton button = null;
	    
	    JPanel wrapper = new JPanel();
	    wrapper.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrapper.setLayout(w_gridbag);

	    int col = 0;

	    // ------------------

	    status_label = new JTextField( 20 );


	    status_label.setEditable( false );

	    c = new GridBagConstraints();
	    c.gridx = col++;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.BOTH;
	    c.weightx = 1.0;
	    w_gridbag.setConstraints( status_label, c);
	    
	    wrapper.add( status_label );


	    // ------------------


	    //addFiller( wrapper, w_gridbag, 0, col++, 10 );
	    addFiller( wrapper, w_gridbag, 0, col++, 10 );
	    

	    // ------------------
	
	    button = new JButton("Print");
	    button.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			new PrintManager( mview, pro_panel, pro_panel ).openPrintDialog();
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.EAST;
	    c.weightx = 0.1;
	    c.gridx = col++;
	    w_gridbag.setConstraints(button, c);
	    wrapper.add(button);

	    addFiller( wrapper, w_gridbag, 0, col++, 10 );
	    
	    button = new JButton("Help");
	    button.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			mview.getPluginHelpTopic("ProfileViewer", "ProfileViewer");
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    w_gridbag.setConstraints(button, c);
	    wrapper.add(button);

	    addFiller( wrapper, w_gridbag, 0, col++, 10 );

	    button = new JButton("Close");
	    button.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			cleanUp();
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = col++;
	    w_gridbag.setConstraints(button, c);
	    wrapper.add(button);
	    	
	    
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    //c.gridwidth = 2;
	    //c.weighty = 2.0;
	    //c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(wrapper, c);
	    panel.add(wrapper);
	}

    }


    private void addFiller( JPanel panel, GridBagLayout bag, int row, int col, int size )
    {
	Dimension fillsize = new Dimension( size, size);
	Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
	GridBagConstraints c = new GridBagConstraints();
	c.gridy = row;
	c.gridx = col;
	bag.setConstraints(filler, c);
	panel.add(filler);
    }


    public void cleanUp()
    {
	mview.putBooleanProperty("ProfileViewer.auto_space", auto_space );

	/*
	mview.putBooleanProperty("ProfileViewer.include_children", include_children_jchkb.isSelected() );
	mview.putBooleanProperty("ProfileViewer.uniform_scale", uniform_scale_jchkb.isSelected() );
	mview.putBooleanProperty("ProfileViewer.apply_filter", apply_filter_jchkb.isSelected() );
	mview.putBooleanProperty("ProfileViewer.show_mean", show_mean_jchkb.isSelected() );
	*/

	// edata.removeExternalSelectionListener(sel_handle);

	edata.removeObserver(this);
	frame.setVisible(false);
    }


 
    // ========================================================================================
    //
    // called by either of pickers whenever their selection has changed
    //
    // ========================================================================================

    
    public void selectionHasChanged()
    {
	System.out.println("NewProfileViewer.selectionHasChanged()...");

	pro_panel.update( spot_picker, meas_picker );
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
	edata = mview.getExprData();
	dplot = mview.getDataPlot();

	buildGUI();

	frame.pack();
	frame.setVisible(true);

	// force one of the panels to be visible
	//
	selector.displayPanel( meas_picker );

	// register ourselves with the data
	//
	edata.addObserver(this);

	// sel_handle = edata.addExternalSelectionListener(this);

    }

    public void stopPlugin()
    {
	cleanUp();
    }

    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("New Profile Viewer", "viewer", 
					 "Shows the profile of elements within one or more clusters", "",
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
    // the observer interface of ExprData notifies us whenever something
    // interesting happens to the data as a result of somebody (including us) 
    // manipulating it
    //
    public void dataUpdate(ExprData.DataUpdateEvent due)
    {
	switch(due.event)
	{
	case ExprData.ColourChanged:
	    break;
	case ExprData.OrderChanged:
	case ExprData.ValuesChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	case ExprData.RangeChanged:
	    //spotListSelectionHasChanged();
	    //pro_panel.repaint();
	    break;
	case ExprData.VisibilityChanged:
	    break;
	case ExprData.NameChanged:
	case ExprData.SizeChanged:
	    //populateListWithSpots(spot_list);
	    //spotListSelectionHasChanged();
	    //pro_panel.repaint();
	    break;
	}
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
	    //if(cl_tree != null)
	    //{
	    //	populateTreeWithClusters(cl_tree, edata.getRootCluster() );
	    //	treeSelectionHasChanged();
	    //}
	    break;
	}
    }

    public void measurementUpdate(ExprData.MeasurementUpdateEvent mue)
    {
	switch(mue.event)
	{
	case ExprData.VisibilityChanged:
	case ExprData.SizeChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	case ExprData.NameChanged:
	case ExprData.OrderChanged:
	    // populateListWithMeasurements(meas_list);
	    //measurementListSelectionHasChanged();
	    break;
	}
    }

    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
	//background_col = mview.getBackgroundColour();
	//text_col       = mview.getTextColour();

	pro_panel.repaint();
    }



    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

/*
    private void setPickerPanel(boolean display_clusters)
    {
	// to update the graph layout....
	
	
	// ------------

	JComponent rjc = (picker_pick_spots) ? spot_picker_panel : cluster_picker_panel;

	pickwrap.remove( rjc );

	picker_pick_spots = !display_clusters;

	JComponent jc = (picker_pick_spots) ? spot_picker_panel : cluster_picker_panel;

	GridBagConstraints c = new GridBagConstraints();
	c.gridy = 1;
	c.gridwidth = 2;
	c.weightx = 9.0;
	c.weighty = 9.0;
	c.fill = GridBagConstraints.BOTH;
	pickbag.setConstraints(jc, c);
	pickwrap.add(jc);
	
        pickwrap.revalidate();
	pickwrap_parent.revalidate();
	pickwrap_parent.repaint();

	if(display_clusters)
	{
	    treeSelectionHasChanged();
	}
	else
	{
	    n_cols = 1;
	    n_rows = 1;
	
	    layoutGraphs();

	    if(pro_panel != null)
	    {
		updateProfiles();
		
		pro_panel.repaint();
	    }
	}

    }

*/

/*
    private void measurementListSelectionHasChanged()
    {
	int[] sel_inds = meas_list.getSelectedIndices();

	meas_ids = new int[ sel_inds.length ];
	int mp = 0;

	for(int m=0; m < edata.getNumMeasurements(); m++)
	{
	    int mi = edata.getMeasurementAtIndex(m);
	    if(meas_list.isSelectedIndex( m ))
	    {
		meas_ids[mp++] = mi;
	    }
	}

	// updateProfiles();
	
	pro_panel.repaint();
    }
*/


    private void layoutGraphs( )
    {
/*
	if(picker_pick_spots)
	{
	    n_cols = 1;
	    n_rows = 1;
	}
	else
	{
	    TreeSelectionModel tsm = (TreeSelectionModel) cl_tree.getSelectionModel();
	    
	    int mi = tsm.getMinSelectionRow();
	    int ma = tsm.getMaxSelectionRow();
	    
	    int count = 0;
	    
	    for(int s=mi; s <= ma; s++)
	    {
		if(tsm.isRowSelected(s))
		{
		    count++;
		}
	    }
	    

	    //System.out.println( "layoutGraphs(): " + count + " things selected..." );


	    if(count > 0)
	    {
		// find the nicest factors of 'count'
		
		// pick the two factors that are closest to one another
		
		double best_rc_diff = Double.MAX_VALUE;
		
		n_cols = count;

		for(int m=1; m < count; m++)
		{
		    double c = (double) m;
		    double r = ((double) count) / c;
		    
		    double rc_diff = Math.abs(r - c);
		    if(rc_diff <  best_rc_diff)
		    {
			best_rc_diff= rc_diff;
			n_cols = (int) c;
		    }
		}
		
		n_rows = (n_cols > 0) ? (int)(Math.ceil((double)count / (double)n_cols)) : count;

		if( n_rows < 1 )
		    n_rows = 1;
		if( n_cols < 1 )
		    n_cols = 1;

		//System.out.println( "layoutGraphs(): layout is " + n_rows + "x" + n_cols );

	    }
	}
*/
    }


    /**
     * repoulates the sel_cl_ids vector after a selection change
     * ( sel_cl_ids is a vector of int[] for each selected cluster )
     *  
     */
    private void updateProfiles()
    {
/*
	if( picker_pick_spots )
	{

	}
	else
	{
	    sel_cl_ids.clear();
	    
	    for(int s=0; s < sel_cls.size(); s++)
	    {
		ExprData.Cluster cl = (ExprData.Cluster) sel_cls.elementAt(s);
		int[] ids = include_children ? cl.getAllClusterElements() :  cl.getElements();
		sel_cl_ids.addElement(ids);
	    }
	    }
*/

    }

/*
    private void selectClusterInTree(final JTree tree, final ExprData.Cluster cl)
    {
	DefaultTreeModel model      = (DefaultTreeModel)       tree.getModel();
	DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
	for (Enumeration en =  root.depthFirstEnumeration(); en.hasMoreElements() ;) 
	{
	    DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) en.nextElement();
	    ExprData.Cluster ncl = (ExprData.Cluster) dmtn.getUserObject();
	    if(cl == ncl)
	    {
		TreeNode[] tn_path = model.getPathToRoot(dmtn);
		TreePath tp = new TreePath(tn_path);
		
		tree.scrollPathToVisible(tp);
		tree.setSelectionPath(tp);
	    }
	}
    }

    private void selectClustersInTree(final JTree tree, final Hashtable clusters)
    {
	Vector tree_paths_v = new Vector();
	DefaultTreeModel model      = (DefaultTreeModel)       tree.getModel();
	DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();

	for (Enumeration en =  root.depthFirstEnumeration(); en.hasMoreElements() ;) 
	{
	    
	    DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) en.nextElement();
	    ExprData.Cluster ncl = (ExprData.Cluster) dmtn.getUserObject();

	    ExprData.Cluster cl = (ExprData.Cluster) clusters.get(ncl);

	    if(cl != null)
	    {
		TreeNode[] tn_path = model.getPathToRoot(dmtn);
		TreePath tp = new TreePath(tn_path);
		
		tree_paths_v.addElement(tp);
	    }
	}
	if(tree_paths_v.size() > 0)
	{
	    tree.setSelectionPaths((TreePath[]) tree_paths_v.toArray( new TreePath[tree_paths_v.size()] ));
	    tree.scrollPathToVisible((TreePath) tree_paths_v.elementAt(0));
	    treeSelectionHasChanged();
	}
    }

    private void expandClusterInTree(final JTree tree, final ExprData.Cluster cl)
    {
	// select cluster 'cl' and "some" of its children....
	int max = 10;
	
	if(cl.getNumChildren() >= 10)
	    max = cl.getNumChildren() + 1;

	Vector start = new Vector();
	start.addElement(cl);

	Vector clusters = new Vector();

	addClusters( 0, max, start, clusters );

	//System.out.println("decided to add " + clusters.size() + " clusters...");

	Hashtable cl_ht = new Hashtable();

	for(int c=0; c < clusters.size(); c++)
	    cl_ht.put( clusters.elementAt(c), clusters.elementAt(c));

	selectClustersInTree( cl_tree, cl_ht );
    }

    private void addClusters( final int depth, final int space_left, Vector cl_v, final Vector vec )
    {
	//System.out.println("depth:" + depth + ": has "+  cl_v.size() + " clusters");

	// is there enough space to add all the clusters in the vector 'cl_v'?
	final int size = cl_v.size();
	if(size <  space_left)
	{
	    //System.out.println("depth:" + depth + ": adding these "+  cl_v.size() + " clusters");

	    for(int c=0; c < size; c++)
		vec.addElement( cl_v.elementAt( c ));
	}
	else
	{
	    //System.out.println("depth:" + depth + ": no room for these "+  cl_v.size() + " clusters");

	    return;
	}

	// might there be any room for the children?
	if((space_left-size) > 0)
	{
	    //System.out.println("depth:" + depth + ": checking for next depth....");

	    // now compose a vector of the children of all of the clusters in the vector 'cl_v'
	    Vector all_ch_v = new Vector();
	    for(int c=0; c < size; c++)
	    {
		Vector ch_v = ((ExprData.Cluster)cl_v.elementAt(c)).getChildren();
		if(ch_v != null)
		    for(int cc=0; cc < ch_v.size(); cc++)
			all_ch_v.addElement( ch_v.elementAt( cc ));
	    }
	    
	    //System.out.println("depth:" + depth + ": next depth will have "+  all_ch_v.size() + " clusters");
	    
	    // and try to add this
	    if(all_ch_v.size() > 0)
		addClusters( depth+1, space_left - size, all_ch_v, vec );
	}
    }
*/

    // ----------------------------------------------------------------------------------------------
    //
    //  tree expander
    //
    // ----------------------------------------------------------------------------------------------

    private final void openTreeToDepth(final int depth)
    {
/*
	final JTree target = cl_tree;
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
*/

    }

    private final void openNodeToDepth(final JTree target, final DefaultTreeModel model, 
				       final DefaultMutableTreeNode node, final int depth)
    {
/*
	TreeNode[] tn_path = model.getPathToRoot(node);
	TreePath tp = new TreePath(tn_path);
	
	if(depth > 0)
	{
	    target.expandPath(tp);
	    final int n_c = node.getChildCount();
	    for(int c=0; c < n_c; c++)
	    {
		openNodeToDepth(target, model, (DefaultMutableTreeNode) node.getChildAt(c), depth-1);
	    }
	}
	else
	{
	    target.collapsePath(tp);
	}
*/
    }


    private final void openTreeToDepth(final JTree target, final int depth)
    {
/*
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
*/

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
	    /*
	    int od = -1;

	    switch(e.getKeyCode())
	    {
	    case KeyEvent.VK_0:
		od = 0;
		break;
	    case KeyEvent.VK_1:
		od = 1;
		break;
	    case KeyEvent.VK_2:
		od = 2;
		break;
	    case KeyEvent.VK_3:
		od = 3;
		break;
	    case KeyEvent.VK_4:
		od = 4;
		break;
	    case KeyEvent.VK_5:
		od = 5;
		break;
	    case KeyEvent.VK_6:
		od = 6;
		break;
	    case KeyEvent.VK_7:
		od = 7;
		break;
	    case KeyEvent.VK_8:
		od = 8;
		break;
	    }
	    
	    if(od >= 0)
	    {
		//System.out.println(" key_event: od=" + od );

		DefaultMutableTreeNode node = (DefaultMutableTreeNode) cl_tree.getLastSelectedPathComponent();
		if(node != null)
		{
		    final DefaultTreeModel model = (DefaultTreeModel) cl_tree.getModel();
		    if(model == null)
			return;
		    openNodeToDepth(cl_tree, model, node, od);
		}
		else
		{
		    openTreeToDepth(od);
		}
	    }
	    */
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   ExternalSelectionListener
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void spotSelectionChanged(int[] spot_ids)
    {
	pro_panel.repaint();
    }

    public void clusterSelectionChanged(ExprData.Cluster[] clusters) { }

    public void spotMeasurementSelectionChanged(int[] spot_ids, int[] meas_ids) { }
    
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   ProfileViewerPanel
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    // ================================================================================
    // ===== spot picker ======================
    // ================================================================================
    //
    //  stores and retrieves ids based on their positions
    //   uses a simle quadtree subdivision of the space
    //
    //   (used to accelerate for mouse tracking)
    //
    //   (shared with HyperCubePlot)
    //
    // ================================================================================


    // ================================================================================
    // ===== spot picker II ======================
    // ================================================================================

  
    

    // ============================================================================
    // ============================================================================
    // spot labels
    // ============================================================================

    public Vector getLabelledSpots() { return new Vector(); }

    public String getSpotLabel( int sid )
    {
	// return (nt_sel.getNameTag.getSpotlabel() );
	
	return "spot:" +  String.valueOf( sid );
    }
    
    /*
    private Hashtable show_spot_label = new Hashtable();
    private int cycled_data = 0;

    private Integer nextData()
    {
	int res = cycled_data++;
	if(cycled_data >=  n_sel_colours)
	    cycled_data = 0;

	return new Integer( res );
    }

    // -1 if not found, >=0 otherwise
    public int getSpotLabel(int id)
    {
	Integer res = (Integer) show_spot_label.get(new Integer(id));
	if(res != null)
	    return res.intValue();
	else
	    return -1;
    }

    public void showSpotLabel(int id)
    {
	Integer key = new Integer(id);
	if(show_spot_label.get(key) == null)
	{
	    show_spot_label.put( key, nextData() );
	}
    }

    public void clearAllSpotLabels()
    {
	show_spot_label = new Hashtable();
    }

    public void toggleSpotLabel(int id)
    {
	Integer i = new Integer(id);
	
	if(show_spot_label.get(i) == null)
	{
	    show_spot_label.put(i, nextData() );
	    //System.out.println(id + " added");
	}
	else
	{
	    show_spot_label.remove(i);
	    //System.out.println(id + " removed");
	}
	
	pro_panel.repaint();
    }

    public void setSpotLabel(ExprData.Cluster cl)
    {
	if(cl.getIsSpot())
	{
	    final int[] els = cl.getElements();
	    if(els != null)
		for(int e=0; e < els.length; e++)
		    showSpotLabel(els[e]);
	}
    }   
   
    public void setAllSpotLabels()
    {
	if( picker_pick_spots )
	{
	    for(int s=0; s < sel_spot_ids.length; s++)
	    {
		showSpotLabel( sel_spot_ids[s] );
	    }
	}
	else
	{
	    for(int s=0; s < sel_cls.size(); s++)
	    {
		ExprData.Cluster cl = (ExprData.Cluster) sel_cls.elementAt(s);
		int[] ids = include_children ? cl.getAllClusterElements() :  cl.getElements();
		if(ids != null)
		for(int e=0; e < ids.length; e++)
		    showSpotLabel(ids[e]);
	    }
	}
    }
    */
 

    // ============================================================================
    // ============================================================================
    //
    // label space allocator
    //
    // ============================================================================
    // ============================================================================

    // ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 


    // ============================================================================
    // ============================================================================
    //
    //  history list
    //
    // ============================================================================
    // ============================================================================

    private void goBack()
    {
	/*
	System.out.println("goBack(): " + history_v.size() + " things in history");

	if(history_v.size() == 0)
	    return;


	// history_v.removeElementAt( 0 );

	Vector back = (Vector) history_v.elementAt( 0 );
	history_v.removeElementAt( 0 );

	//System.out.println("goBack(): got old selection with size=" + back.size());
	
	Hashtable cl_ht = new Hashtable();

	for(int c=0; c < back.size(); c++)
	    cl_ht.put( back.elementAt(c), back.elementAt(c));

	enable_stores = false;

	selectClustersInTree( cl_tree, cl_ht );

	//System.out.println("goBack(): done");

	enable_stores = true;
	*/
    }

    private void goForward()
    {
	//System.out.println("goForward(): " + history_v.size() + " things in history");

    }

    private void storePosition()
    {
	
	if(enable_stores)
	{
	    //System.out.println("storePosition(): saving, sel size=" + sel_cls.size() );
	    
	    //history_v.insertElementAt( sel_cls.clone(), 0 );

	    //System.out.println("storePosition(): " + history_v.size() + " things in history");
	    
	    /*
	    System.out.print("  history: ");
	    for(int h=0; h < history_v.size(); h++)
	    {
		Vector back = (Vector) history_v.elementAt( h );
		System.out.print( " [");
		for(int b=0; b < back.size(); b++)
		    System.out.print( ((ExprData.Cluster)back.elementAt(b)).getName() + " ");
		System.out.print( "] " );
	    }
	    System.out.println();
	    */
	}
    }

    private Vector history_v = new Vector(); 
    private int pos = -1;
    private boolean enable_stores = true;


    // ============================================================================
    // ============================================================================
    //
    //  doings
    //
    // ============================================================================
    // ============================================================================


    public void setMessage( String message ) { status_label.setText( message ); }




    private JFrame frame;

    
    private SpotPicker spot_picker;
    private MeasPicker meas_picker;
    private MeasGroupPicker group_picker;
    private ProfilePanel pro_panel;
    private GraphLayoutPanel layout_panel;
    private OptionsPanel options_panel;

    private boolean include_children;
    private boolean apply_filter;
    private boolean auto_space;
    private boolean uniform_scale;
    private boolean show_mean;

    private maxdView mview;
    private ExprData edata;
    private DataPlot dplot;

    private JTextField status_label;

    private PanelSelector selector;

    /*
      private JCheckBox apply_filter_jchkb;
      private JCheckBox show_mean_jchkb;
      private JCheckBox uniform_scale_jchkb;
      private JCheckBox include_children_jchkb;

 
    private TextLayout min_label, zero_label, max_label;
    private TextLayout[] meas_labels;
    private int  min_label_o, zero_label_o, max_label_o;
    private int[] meas_labels_o;
    private Font font;
    private FontRenderContext frc;

    private double spot_font_scale = 1.0;

    private boolean draw_zero = false;


    private ProfilePanel pro_panel;
    private DragAndDropList meas_list;



    private JPanel pickwrap;  // can contain either a spot list or a cluster tree
    private GridBagLayout pickbag;
	    
    private JPanel spot_picker_panel;
    private JPanel cluster_picker_panel;

    private JComponent pickwrap_parent;

    private boolean picker_pick_spots = false;

    private ExprData.NameTagSelection nt_sel;
    private NameTagSelector nts;

    private Vector sel_cls;       // the selected clusters
    private Vector sel_cl_ids;    // the element_ids for the selected clusters

    private int[]  meas_ids;

    private boolean apply_filter;
    private boolean include_children;

    private int n_cols;
    private int n_rows;

    private int graph_w;
    private int graph_h;

    private int ticklen;

    private int graph_sx;   // step size between graphs
    private int graph_sy;

    private int xoff, yoff;

    private int graph_x_axis_step;
    private int graph_y_axis_step;

    private double graph_y_axis_scale;
    private double graph_y_axis_min;


    // private SpotPickerNode root_spot_picker;

    private ProfilePicker root_profile_picker;

    private AxisManager axis_man;
    private DecorationManager deco_man;

    private int sel_handle;
    */
}
