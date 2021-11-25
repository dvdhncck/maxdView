import java.io.*;

import javax.swing.*;
import javax.swing.event.*;

import java.util.*;

import javax.swing.tree.*;
import javax.swing.event.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;

import java.awt.font.*;
import java.awt.geom.*;
import java.awt.dnd.*;

public class SpotPicker extends JPanel
{
    // ========================================================================================
    
    public Selection getSelection() { return selection; }

    // Spot Picker
    
    public SpotPicker( final maxdView mview, final ExprData edata, final NewProfileViewer viewer )
    {
	this.mview = mview;
	this.edata = edata;
	this.viewer = viewer;

	GridBagLayout main_bag = new GridBagLayout();
	setLayout( main_bag );
	GridBagConstraints c;

	
	// ========================================================================================
	//
	// controls for selecting either Individuals or Clusters of Spots
	//
	// ========================================================================================

	
	JPanel pickwrap = new JPanel();
	GridBagLayout pickbag = new GridBagLayout();
	pickwrap.setLayout(pickbag);
	
	ButtonGroup bg = new ButtonGroup();
	
	selecting_individuals = true;

	JRadioButton jchkb = new JRadioButton("Individuals");
	jchkb.setSelected( true );
	bg.add(jchkb);
	jchkb.setFont(mview.getSmallFont());
	c = new GridBagConstraints();
	c.gridx = 0;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.NORTHWEST;
	pickbag.setConstraints(jchkb, c);
	pickwrap.add(jchkb);
	jchkb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    JRadioButton es = (JRadioButton) e.getSource();

		    selecting_individuals = true;

		    spot_picker_nts.setVisible( es.isSelected() );
		    spot_list_scrollpane.setVisible( es.isSelected() );
		    spot_cluster_scrollpane.setVisible( ! es.isSelected() );

		    revalidate();

		    spotListSelectionHasChanged();
		}
	    });


	jchkb = new JRadioButton("Clusters");
	jchkb.setSelected( false );
	bg.add(jchkb);
	jchkb.setFont(mview.getSmallFont());
	c = new GridBagConstraints();
	c.gridx = 1;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.NORTHWEST;
	pickbag.setConstraints(jchkb, c);
	pickwrap.add(jchkb);
	jchkb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    JRadioButton es = (JRadioButton) e.getSource();
		    
		    selecting_individuals = false;

		    spot_picker_nts.setVisible( ! es.isSelected() );
		    spot_list_scrollpane.setVisible( ! es.isSelected() );
		    spot_cluster_scrollpane.setVisible( es.isSelected() );

		    revalidate();

		    treeSelectionHasChanged();
		}
	    });



	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	main_bag.setConstraints( pickwrap, c);
	add( pickwrap );


	// ========================================================================================
	//
	// a NameTagSelector for controlling what is displayed in the list
	//
	// ========================================================================================

	spot_picker_nts = new NameTagSelector( mview );
	spot_picker_nts.setFont( mview.getSmallFont() );
	spot_picker_nts.loadSelection("ProfileViewer.spot_label");
	spot_picker_nts.addActionListener(new ActionListener() 
	    { 
		public void actionPerformed(ActionEvent e) 
		{
		    spot_picker_nts.saveSelection("ProfileViewer.spot_label");
			
		    populateListWithSpots( spot_list );
		}
	    });


	c = new GridBagConstraints();
	c.gridy = 1;
	c.weightx = 10.0;
	c.anchor = GridBagConstraints.NORTH;
	c.fill = GridBagConstraints.HORIZONTAL;
	main_bag.setConstraints( spot_picker_nts, c);
	add( spot_picker_nts );



	// ========================================================================================
	//
	// the Spot List
	//
	// ========================================================================================


	spot_list = new DragAndDropList();

	populateListWithSpots( spot_list );
	    
	spot_list.addListSelectionListener(new ListSelectionListener() 
	    {
		public void valueChanged(ListSelectionEvent e)
		{
		    spotListSelectionHasChanged();
		}
	    });
	    

	spot_list.setDropAction( new DragAndDropList.DropAction()
	    {
		public void dropped(DragAndDropEntity dnde)
		{
		    try
		    {
			int[] sids = dnde.getSpotIds();
			if(sids != null)
			{
			    setSpotListSelection( sids );
			}
		    }
		    catch(DragAndDropEntity.WrongEntityException wee)
		    {
			try
			{
			    ExprData.Cluster cl = dnde.getCluster();
			    if(cl != null)
			    {
				int[] sids = cl.getAllClusterElements();
				if(sids != null)
				    setSpotListSelection( sids );
			    }
				
			}
			catch(DragAndDropEntity.WrongEntityException wee2)
			{
			}
		    }
		}
	    });
	
	spot_list_scrollpane = new JScrollPane( spot_list );
	c = new GridBagConstraints();
	c.gridy = 2;
	c.weightx = 9.0;
	c.weighty = 9.0;
	c.fill = GridBagConstraints.BOTH;

	main_bag.setConstraints( spot_list_scrollpane, c);
	add( spot_list_scrollpane );
	   

	

	// ========================================================================================
	//
	// the Cluster Tree
	//
	// ========================================================================================

	//CustomKeyListener ckl = new CustomKeyListener();
	//cluster_picker_panel.addKeyListener( ckl );
	
	//GridBagLayout cluster_picker_bag = new GridBagLayout();
	//cluster_picker_panel.setLayout(cluster_picker_bag);
	    
	
	spot_cluster_tree = new DragAndDropTree();

	populateTreeWithClusters( spot_cluster_tree, edata.getRootCluster() );

	addTreeDragAndDropActions( spot_cluster_tree );
	    
	spot_cluster_tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

	spot_cluster_tree.addTreeSelectionListener(new TreeSelectionListener() 
	    {
		public void valueChanged(TreeSelectionEvent e) 
		{
		    treeSelectionHasChanged();
		}
	    });

	spot_cluster_scrollpane = new JScrollPane( spot_cluster_tree );

	c = new GridBagConstraints();
	c.gridy = 2;
	c.weightx = 10.0;
	c.weighty = 9.0;
	c.anchor = GridBagConstraints.NORTH;
	c.fill = GridBagConstraints.BOTH;

	main_bag.setConstraints( spot_cluster_scrollpane, c );
	add( spot_cluster_scrollpane );
	 
	spot_cluster_scrollpane.setVisible( false );
    }

    
    // ========================================================================================
    //
    // handlers for the Spot List
    //
    // ========================================================================================


    private void populateListWithSpots( JList list )
    {

	// save existing selection if any
	HashSet sels = new HashSet();
	ListSelectionModel lsm = list.getSelectionModel();
	if(lsm != null)
	{
	    for(int s=lsm.getMinSelectionIndex(); s <= lsm.getMaxSelectionIndex(); s++)
	    {
		if(lsm.isSelectedIndex(s))
		    sels.add( list.getModel().getElementAt(s) );
	    }
	}



	final ExprData.NameTagSelection nts = spot_picker_nts.getNameTagSelection();
	final int ns = edata.getNumSpots();

	// build a hashtable of names to use as the list data
	// (to uniqify the names.....)
	
	java.util.HashSet data = new java.util.HashSet();
	
	final boolean af = false; // ((apply_filter_jchkb != null) && (apply_filter_jchkb.isSelected()));

	sel_spot_back_map = new Hashtable();

	for(int s=0; s < ns; s++)
	{
	    if( !af || ( !edata.filter( s ) ) )
	    {
		final String n = nts.getNameTag( s );

		if(n != null)
		{
		    Vector indices = (Vector) sel_spot_back_map.get( n );

		    if(indices == null)
		    {
			data.add( n );
			indices = new Vector();
			sel_spot_back_map.put(n, indices);
		    }

		    indices.addElement( new Integer(s) );
		}
	    }
	}
	
	String[] names = new String[ data.size() ];
	int np = 0;

	for(java.util.Iterator it = data.iterator() ; it.hasNext();  )
	{
	    names[np] = (String) it.next();
	    
	    np++;
	}
	
	java.util.Arrays.sort(names);

	// build a reverse map from name to index in the list
	// (needed to handle dropping of spots onto the list)
	//
	sel_spot_name_map = new Hashtable();
	for(int nps=0; nps < names.length; nps++)
	    sel_spot_name_map.put( names[nps], new Integer( nps ));
	

	// and install the new list data

	list.setListData( names );
	


	// and restore the previous selection if there was one
	if(sels.size() > 0)
	{
	    final Vector sels_v = new Vector();

	    // check each of the new elements 
	    for(int o=0; o < names.length; o++)
	    {
		if( sels.contains( names[ o ] ) )
		{
		    sels_v.addElement( new Integer( o ) );
		}
	    }

	    final int[] sel_cl_ids = new int[ sels_v.size() ];
	    for( int s=0; s <  sels_v.size(); s++ )
	    {
		sel_cl_ids[s] = ( (Integer) sels_v.elementAt(s)).intValue();
	    }

	    list.setSelectedIndices( sel_cl_ids );
	}		

    }

    private void setSpotListSelection( int[] sids )
    {
	// System.out.println("setSpotListSelection(): got " + sids.length + " spots");
	
	// we get a bunch of spot ids, want to convert to a bunch of list indices

	// check each spot id against indices in the back_map

	final ExprData.NameTagSelection nts = spot_picker_nts.getNameTagSelection();

	final Vector hits = new Vector();

	for(int s=0; s < sids.length; s++)
	{
	    // get the name tag(s) for this spot

	    final String n = nts.getNameTag( sids[s] );
	    
	    if(n != null)
	    {
		final Integer lid_i = (Integer) sel_spot_name_map.get( n );
		
		if(lid_i != null)
		    hits.addElement(lid_i);
	    }
	    
	}
	
	int[] sels = new int[ hits.size() ];

	for(int s=0; s <  hits.size(); s++)
	    sels[s] = ((Integer) hits.elementAt(s)).intValue();

	// System.out.println("setSpotListSelection(): " + hits.size() + " hits");
	
	spot_list.setSelectedIndices( sels );

    }


    private void spotListSelectionHasChanged()
    {
	System.out.println("spotListSelectionHasChanged()");

	if( selecting_individuals )
	{
	    ListSelectionModel lsm = spot_list.getSelectionModel();

	    if(lsm != null)
	    {
		Vector result = new Vector();
		
		for(int s=lsm.getMinSelectionIndex(); s <= lsm.getMaxSelectionIndex(); s++)
		{
		    if(lsm.isSelectedIndex(s))
		    {
			Vector indices = (Vector) sel_spot_back_map.get( spot_list.getModel().getElementAt(s) );

			if( indices != null )
			{
			    for( int i = 0 ; i < indices.size(); i++ )
				result.add( indices.elementAt( i ) );
			}
		    }
		}

		if( result.size() == 0 )
		{
		    selection = null;
		}
		else
		{
		    int[] ids = new int[ result.size() ];
		    
		    for (int i = 0 ; i < result.size(); i++ )
			ids[ i ] = ( (Integer) result.elementAt( i )).intValue();
		    
		    String[] titles = new String[ 1 ];
		    
		    titles[ 0 ] = "Spots";
		    
		    System.out.println( ids.length + " spots ( in one group )" );
		    
		    selection = new Selection( titles, ids, null );
		}
	    }
	    else
	    {
		selection = null;
	    }
	}
	
	viewer.selectionHasChanged();
    }
    

 
    // ========================================================================================
    //
    // handlers for the Cluster Tree
    //
    // ========================================================================================


    private void populateTreeWithClusters( JTree tree, ExprData.Cluster cluster )
    {
	// System.out.println("making tree for " + cluster.getName() );
	
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
	if(dmtn == null)
	    return;

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
	    //System.out.println(sels.size() + " saved selections");

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
	}
	
    }

    private DefaultMutableTreeNode generateTreeNodes(DefaultMutableTreeNode parent, ExprData.Cluster clust )
    {
	if( clust.getIsSpot() )
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
    }

    private void treeSelectionHasChanged()
    {
	System.out.println("spotTreeSelectionHasChanged()");

	if( selecting_individuals == false )
	{
	    Vector sel_cls = new Vector();
	    
	    // how many selected things?
	    
	    TreeSelectionModel tsm = (TreeSelectionModel) spot_cluster_tree.getSelectionModel();
	    
	    int mi = tsm.getMinSelectionRow();
	    int ma = tsm.getMaxSelectionRow();
	    
	    int total_spots = 0;

	    for(int s=mi; s <= ma; s++)
	    {
		if(tsm.isRowSelected(s))
		{
		    TreePath tp = spot_cluster_tree.getPathForRow(s);

		    DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) tp.getLastPathComponent();

		    ExprData.Cluster cl = (ExprData.Cluster) dmtn.getUserObject();
		    
		    if( cl.getNumElements() > 0 )
		    {
			sel_cls.addElement( cl );
			total_spots += cl.getNumElements();
		    }
		}
	    }

	    if( sel_cls.size() == 0 )
	    {
		// selection is empty,
		
		selection = null;
	    }
	    else
	    {
		//  convert those selected clusters into a single list
		//  (but keep an 'grouping index' mapping each entry
		//  in the list back onto the cluster which owns it)
		
		final String[] group_names = new String[ sel_cls.size() ];

		final int[] spot_ids   = new int[ total_spots ];

		final int[] spot_group = new int[ total_spots ];  

		int index = 0;

		for( int c = 0 ; c < sel_cls.size(); c++ )
		{
		    ExprData.Cluster cl = (ExprData.Cluster) sel_cls.elementAt( c );

		    group_names[ c ] = cl.getName();

		    int[] elements = cl.getElements();    // this is always a set of Spot IDs
		    
		    for( int e = 0; e < elements.length; e++ )
		    {
			spot_ids[ index ] = elements[ e ];
			
			spot_group[ index ] = c;
		    }
		    
		    index++;
		}
		
		System.out.println( spot_ids.length + " spots ( in " + sel_cls.size() + " groups )" );
		
		selection = new Selection( group_names, spot_ids, spot_group );
		
	    }


	    viewer.selectionHasChanged();
	}
    }


    // ========================================================================================
    //
    // state
    //
    // ========================================================================================


    private maxdView mview;
    private ExprData edata;
    private NewProfileViewer viewer;
    
    private Selection selection = null;

    private boolean selecting_individuals = true;

    private int[] sel_spot_ids = null;
    private Hashtable sel_spot_back_map = null;
    private Hashtable sel_spot_name_map = null;

    private boolean apply_filter;
    private boolean include_children;

    private DragAndDropTree spot_cluster_tree;
    private DragAndDropList spot_list;
    private NameTagSelector spot_picker_nts;

    private JScrollPane spot_list_scrollpane;
    private JScrollPane spot_cluster_scrollpane;

    private ExprData.NameTagSelection nt_sel;
    private NameTagSelector nts;

}