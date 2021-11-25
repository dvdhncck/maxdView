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

public class MeasPicker extends JPanel
{
    public Selection getSelection() 
    { 
	return selection; 
    }

    
    public int getNumDataPointsPerProfile()
    {
	if( selecting_individuals )
	{
	    //final boolean name_mode = ( meas_individual_names_jcb.getSelectedIndex() <= 0 );
	    
	    int count = 0;
	    
	    /*
	    final ListSelectionModel lsm = meas_list.getSelectionModel();
	    for( int s = lsm.getMinSelectionIndex(); s <= lsm.getMaxSelectionIndex(); s++ )
		if( lsm.isSelectedIndex( s ) )
		    count++;
	    */

	    return count;
	}
	else
	{
	    // selecting clusters....
	    
	    return selection == null ? 0 : selection.getSize();
	}
    }



    public MeasPicker( final maxdView mview, final ExprData edata, final NewProfileViewer viewer )
    {
	this.mview = mview;
	this.edata = edata;
	this.viewer = viewer;


	GridBagLayout main_bag = new GridBagLayout();
	setLayout( main_bag );
	GridBagConstraints c;


	// ========================================================================================
	//
	// controls for selecting either Individuals or Clusters of Measurements
	//
	// ========================================================================================

	
	JPanel pickwrap = new JPanel();
	GridBagLayout pickbag = new GridBagLayout();
	pickwrap.setLayout(pickbag);

	ButtonGroup bg = new ButtonGroup();
	

	JRadioButton jchkb = new JRadioButton("Individuals");
	jchkb.setSelected( true );
	bg.add(jchkb);
	jchkb.setFont(mview.getSmallFont());
	c = new GridBagConstraints();
	c.gridx = 0;
	c.anchor = GridBagConstraints.NORTHWEST;
	pickbag.setConstraints(jchkb, c);
	pickwrap.add(jchkb);
	jchkb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    JRadioButton es = (JRadioButton) e.getSource();

		    selecting_individuals = true;

		    meas_individual_names_jcb.setVisible( es.isSelected() );
		    meas_indiv_scrollpane.setVisible( es.isSelected() );
		    meas_cluster_scrollpane.setVisible( ! es.isSelected() );

		    //individualTreeSelectionHasChanged();  // is this neccessary?

		    revalidate();
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
	//c.fill = GridBagConstraints.HORIZONTAL;
	pickbag.setConstraints(jchkb, c);
	pickwrap.add(jchkb);
	jchkb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    JRadioButton es = (JRadioButton) e.getSource();
		    
		    selecting_individuals = false;

		    meas_individual_names_jcb.setVisible( ! es.isSelected() );
		    meas_indiv_scrollpane.setVisible( ! es.isSelected() );
		    meas_cluster_scrollpane.setVisible( es.isSelected() );

		    //clusterTreeSelectionHasChanged(); // is this neccessary?

		    revalidate();
		}
	    });



	c = new GridBagConstraints();
	c.weightx = 10.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.anchor = GridBagConstraints.NORTHWEST;
	main_bag.setConstraints( pickwrap, c);
	add( pickwrap );


	// ========================================================================================
	//
	// the Measurement individual tree
	//
	// ========================================================================================

	meas_indiv_tree = new DragAndDropTree();
	
	populateIndividualTree( meas_indiv_tree );
	
	addIndividualTreeDragAndDropActions( meas_indiv_tree );

	meas_indiv_tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
	
	meas_indiv_tree.addTreeSelectionListener(new TreeSelectionListener() 
	    {
		public void valueChanged( TreeSelectionEvent e ) 
		{
		    individualTreeSelectionHasChanged( meas_indiv_tree, e );
		}
	    });
	
	meas_indiv_scrollpane = new JScrollPane( meas_indiv_tree );

	c = new GridBagConstraints();
	c.gridy = 2;
	c.weightx = 10.0;
	c.weighty = 9.0;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.fill = GridBagConstraints.BOTH;
	main_bag.setConstraints( meas_indiv_scrollpane, c);
	add( meas_indiv_scrollpane );


	// ========================================================================================
	//
	// the Measurement Cluster Tree
	//
	// ========================================================================================

	meas_cluster_tree = new DragAndDropTree();

	populateClusterTree( meas_cluster_tree, edata.getRootCluster() );

	addClusterTreeDragAndDropActions( meas_cluster_tree );
	    
	meas_cluster_tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

	meas_cluster_tree.addTreeSelectionListener(new TreeSelectionListener() 
	    {
		public void valueChanged(TreeSelectionEvent e) 
		{
		    clusterTreeSelectionHasChanged();
		}
	    });


	meas_cluster_scrollpane = new JScrollPane( meas_cluster_tree );

	c = new GridBagConstraints();
	c.gridy = 2;
	c.weightx = 10.0;
	c.weighty = 9.0;
	c.anchor = GridBagConstraints.NORTH;
	c.fill = GridBagConstraints.BOTH;

	main_bag.setConstraints( meas_cluster_scrollpane, c );
	add( meas_cluster_scrollpane );

	meas_cluster_scrollpane.setVisible( false );
 

	    
   }


    // ===========================================================================


    /**
     *  identify the unique set of measurement attributes
     */
    private String[] getUniqueMeasurementAttributeNames( )
    {
	HashSet unique_attr_names = new HashSet();
	
	Vector data = new Vector();

	for(int m=0; m < edata.getNumMeasurements(); m++)
	{
	    int mi = edata.getMeasurementAtIndex(m);
	    if(edata.getMeasurementShow(mi))
	    {
		Hashtable all_attr_names_for_this_meas = edata.getMeasurement(mi).getAttributes();
		
		for(Enumeration ae = all_attr_names_for_this_meas.keys(); ae.hasMoreElements() ;) 
		{
		    String  attr_name_for_this_meas = (String) ae.nextElement();
		    
		    if( unique_attr_names.contains( attr_name_for_this_meas ) == false )
		    {
			unique_attr_names.add( attr_name_for_this_meas );

			data.add( attr_name_for_this_meas );
		    }
		}
		
	    }
	}

	return (String[]) data.toArray( new String[ data.size()] );
    }

    /**
     * the unique set of measurement attribute values for a given attribute
     */
    private String[] getUniqueMeasurementAttributeValues( final String attribute_name )
    {
	HashSet unique = new HashSet();
	
	Vector data = new Vector();
	
	for( int m=0; m < edata.getNumMeasurements(); m++ )
	{
	    int mi = edata.getMeasurementAtIndex(m);

	    if( edata.getMeasurementShow( mi ) )
	    {
		final String value = edata.getMeasurement( mi ).getAttribute( attribute_name );
		
		if( value != null )
		{
		    if( unique.contains( value) == false )
		    {
			unique.add( value );
			
			data.add( value );
		    }
		}
		
	    }
	}
	
	return (String[]) data.toArray( new String[ data.size()] );
    }


    /**
     * returns a unique list of the measurement ids which have any of the 'allowed_values' for the specified 'attr_name'
     */
    private int[] getMeasurementsBasedOnAttributeValues( final String attr_name, final HashSet allowed_values )
    {
	Vector data = new Vector();
	
	for( int m=0; m < edata.getNumMeasurements(); m++ )
	{
	    int mi = edata.getMeasurementAtIndex(m);

	    if( edata.getMeasurementShow( mi ) )
	    {
		final String value = edata.getMeasurement( mi ).getAttribute( attr_name );
		
		if( allowed_values.contains( value ) )
		{
		    data.add( new Integer( mi ) );
		}
	    }
	}
       
	int[] result = new int[ data.size() ];

	for( int i = 0 ; i < data.size() ; i++ )
	    result[ i ] = ((Integer) data.elementAt(i)).intValue();

	return result;
    }

   
    /**
     * the unique set of all known spot attribute names
     */
    private String[] getUniqueMeasurementSpotAttributeNames()
    {
	HashSet unique = new HashSet();
	
	Vector data = new Vector();
	
	for( int m=0; m < edata.getNumMeasurements(); m++ )
	{
	    int mi = edata.getMeasurementAtIndex(m);

	    if( edata.getMeasurementShow( mi ) )
	    {
		ExprData.Measurement meas = edata.getMeasurement( mi );
		
		for( int sa = 0; sa < meas.getNumSpotAttributes(); sa++ )
		{
		    String spot_attr_name = meas.getSpotAttributeName( sa );

		    if( unique.contains( spot_attr_name ) == false )
		    {
			data.add( spot_attr_name );
			unique.add( spot_attr_name );
		    }
		}
		
	    }
	}
	
	return (String[]) data.toArray( new String[ data.size()] );
    }

    
    /**
     * returns a unique list of the measurement ids which have a spot attr defined with the specified 'spot_attr_name'
     */
    private int[] getMeasurementsHavingSpotAttribute( final String spot_attr_name )
    {
	Vector data = new Vector();
	
	for( int m=0; m < edata.getNumMeasurements(); m++ )
	{
	    if( edata.getMeasurementShow( m ) )
	    {
		ExprData.Measurement meas = edata.getMeasurement( m );
		
		if( meas.getSpotAttributeFromName( spot_attr_name ) >= 0 )
		{
		    data.add( new Integer( m ) );
		}
	    }
	}
	
	int[] result = new int[ data.size() ];

	for( int i = 0 ; i < data.size() ; i++ )
	    result[ i ] = ((Integer) data.elementAt(i)).intValue();

	return result;

    }


    // ===========================================================================



    private void addIndividualTreeDragAndDropActions( final DragAndDropTree list )
    {
    }


    // ========================================================================================
    //
    // handlers for the Measurement Individual Tree
    //
    // ========================================================================================


    /**
     *
     * stores information pertinant to a Measurement selection node,
     * such as the type of the node, whether selection should be
     * propagated and so on
     *
     */

    private class NodeInfo
    {
	public NodeInfo( String label, int node_type )
	{
	    this( label, node_type, -1, -1 );
	}

	public NodeInfo( String label, int node_type, int meas_id )
	{
	    this( label, node_type, meas_id, -1 );

	}

	public NodeInfo( String label, int node_type, int meas_id, int spot_attr_id )
	{
	    this.label        = label;
	    this.node_type    = node_type;
	    this.meas_id      = meas_id;
	    this.spot_attr_id = spot_attr_id;
	}

	public String toString() { return label; }

	public String label;

	public int node_type;

	public boolean can_select;

	public boolean propogate;   // should a selection 

	public int meas_id;
	public int spot_attr_id;

    }

    public final static int MeasNameRoot      = 0;
    public final static int MeasNameNode      = 1;
    public final static int MeasNameAttrNode  = 2;
    
    public final static int SpotAttrRoot      = 3;
    public final static int SpotAttrNode      = 4;
    public final static int SpotAttrMeasNode  = 5;
    
    public final static int MeasAttrRoot      = 6;
    public final static int MeasAttrNode      = 7;
    public final static int MeasAttrValueNode = 8;
    

    private void populateIndividualTree( final JTree tree )
    {
	//
	// ====================================================================
	//
	
	DefaultMutableTreeNode name_root = new DefaultMutableTreeNode( new NodeInfo( "Names", MeasNameRoot ) );
	
	for(int m=0; m < edata.getNumMeasurements(); m++)
	{
	    int mi = edata.getMeasurementAtIndex(m);

	    if( edata.getMeasurementShow( mi ) )
	    {
		ExprData.Measurement this_meas = edata.getMeasurement( mi );

		DefaultMutableTreeNode meas_node = new DefaultMutableTreeNode( this_meas.getName() );
		
		for( int sa = 0; sa < this_meas.getNumSpotAttributes(); sa++ )
		{
		    DefaultMutableTreeNode attr_node = new DefaultMutableTreeNode( new NodeInfo( this_meas.getSpotAttributeName( sa ),
												 MeasNameNode, mi ) );
		    
		    meas_node.add( attr_node );
		}
		
		name_root.add( meas_node );
	    }
	}

	//
	// ====================================================================
	//

	DefaultMutableTreeNode spot_attr_root = new DefaultMutableTreeNode( new NodeInfo( "Spot Attrs", SpotAttrRoot ) );
	
	String[] spot_attr_names = getUniqueMeasurementSpotAttributeNames();
	
	if( spot_attr_names != null )
	{
	    for( int san = 0; san < spot_attr_names.length; san++ )
	    {
		DefaultMutableTreeNode spot_attr_node = new DefaultMutableTreeNode( new NodeInfo( spot_attr_names[ san ], SpotAttrNode ) );
		
		int[] meas_with_spot_attr = getMeasurementsHavingSpotAttribute( spot_attr_names[ san ] );

		if( meas_with_spot_attr != null )
		{
		    for( int mwsa = 0; mwsa < meas_with_spot_attr.length; mwsa++ )
		    {
			final String mname = edata.getMeasurementName( meas_with_spot_attr[ mwsa ] );

			DefaultMutableTreeNode meas_attr_node = new DefaultMutableTreeNode( new NodeInfo( mname, 
													  SpotAttrMeasNode, 
													  meas_with_spot_attr[ mwsa ] ) );
			
			spot_attr_node.add( meas_attr_node );
		    }
		}
		
		spot_attr_root.add( spot_attr_node );
		
	    }
	}

	//
	// =====================================================================
	//

	DefaultMutableTreeNode meas_attr_root = new DefaultMutableTreeNode( new NodeInfo( "Measurement Attrs", MeasAttrRoot ) );
		
	final String[] meas_attr_names = getUniqueMeasurementAttributeNames();

	if( meas_attr_names != null )
	{
	    for( int man = 0; man < meas_attr_names.length; man++ )
	    {
		final DefaultMutableTreeNode meas_attr_node = new DefaultMutableTreeNode( new NodeInfo( meas_attr_names[ man ], 
													MeasAttrNode ) );

		final String[] meas_attr_values = getUniqueMeasurementAttributeValues( meas_attr_names[ man ] );
		
		if( meas_attr_values != null ) 
		{
		   for( int mav = 0; mav < meas_attr_values.length; mav++ )
		   {
		       final DefaultMutableTreeNode meas_attr_value_node = new DefaultMutableTreeNode( new NodeInfo( meas_attr_values[ mav ], 
														     MeasAttrValueNode )  );
		       
		       meas_attr_node.add( meas_attr_value_node );

		   } 
		}
		meas_attr_root.add( meas_attr_node );
	    }
	}
	
	//
	// =====================================================================
	//

	DefaultMutableTreeNode root = new DefaultMutableTreeNode( "All" );
	root.add( name_root );
	root.add( spot_attr_root );
	root.add( meas_attr_root );


        DefaultTreeModel model =  new DefaultTreeModel( root );
	tree.setModel(model);

	tree.putClientProperty("JTree.lineStyle", "Angled");
	
	DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
	renderer.setLeafIcon(null);
	renderer.setOpenIcon(null);
	renderer.setClosedIcon(null);
	tree.setCellRenderer(renderer);

		
    }

    private void individualTreeSelectionHasChanged( final JTree jtree, final TreeSelectionEvent event )
    {
	// get user object(s) for the node(s) that have just changed state...

	disable_recusive_individual_tree_selection = true;

	TreePath[] changed_paths = event.getPaths();

	for( int p = 0; p < changed_paths.length; p++ )
	{
	    if( event.isAddedPath( p ) )
		possiblyAddToIndividualTreeSelection( jtree, changed_paths[ p ] );
	}
	
	selection = null;

	viewer.selectionHasChanged();

	disable_recusive_individual_tree_selection = false;

    }

    private void possiblyAddToIndividualTreeSelection( final JTree jtree, final TreePath added_path )
    {
	DefaultMutableTreeNode node = (DefaultMutableTreeNode) added_path.getLastPathComponent();
	
	if( node == null )
	    return;

	viewer.setMessage( "" );

	if( node.getUserObject() instanceof NodeInfo )
	{
	    NodeInfo node_info = (NodeInfo) node.getUserObject();
	    
	    if( node_info == null )
		return;
	    
	    switch( node_info.node_type )
	    {
		case MeasNameRoot:
		    viewer.setMessage( "Auto-selecting all Measurements" );
		    // select all the immediate child nodes...
		    addAllChildrenToSelection( jtree, node );
		    break;
		case MeasNameNode:
		    // clear any user-level SpotAttr nodes or MeasAttr nodes
		    break;
		case MeasNameAttrNode:
		    break;
		    
		case SpotAttrRoot:
		    break;
		case SpotAttrNode:
		    // select all the immediate child nodes...
		    addAllChildrenToSelection( jtree, node );
		    break;
		case SpotAttrMeasNode:
		    break;
		    
		case MeasAttrRoot:
		    break;
		case MeasAttrNode:
		    // select all the immediate child nodes...
		    addAllChildrenToSelection( jtree, node );
		    break;
		case MeasAttrValueNode:
		    viewer.setMessage( "Auto-selecting all Measurements with this value" );
		    break;
		    
	    }
	}
    }


    private void addAllChildrenToSelection( final JTree jtree, final DefaultMutableTreeNode node )
    {
	if( node.getChildCount() == 0 )
	    return;
	
	final Vector paths_v = new Vector();
	
	for( int c = 0; c < node.getChildCount(); c++ )
	{
	    DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt( c );
	    paths_v.add( new TreePath( child.getPath() ) );
	}
	
	final DefaultTreeSelectionModel model = (DefaultTreeSelectionModel) jtree.getSelectionModel();
	
	model.addSelectionPaths( (TreePath[]) paths_v.toArray( new TreePath[ paths_v.size() ] ) );
    }



    // ========================================================================================
    //
    // handlers for the Cluster Tree
    //
    // ========================================================================================


    private void populateClusterTree( JTree tree, ExprData.Cluster cluster )
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

	DefaultMutableTreeNode dmtn = generateClusterTreeNodes( null, cluster );
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
	
	// restore the selection is there was one

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
    

    private DefaultMutableTreeNode generateClusterTreeNodes(DefaultMutableTreeNode parent, ExprData.Cluster clust )
    {
	if( ( clust.getParent() == null ) || ( clust.getIsSpot() == false ) )
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


    private void addClusterTreeDragAndDropActions(final DragAndDropTree tree)
    {
    }

    private void clusterTreeSelectionHasChanged()
    {
	System.out.println("clusterTreeSelectionHasChanged()");

	if( selecting_individuals == false )
	{
	    Vector sel_cls = new Vector();
	    
	    // how many selected things?
	    
	    TreeSelectionModel tsm = (TreeSelectionModel) meas_cluster_tree.getSelectionModel();
	    
	    int mi = tsm.getMinSelectionRow();
	    int ma = tsm.getMaxSelectionRow();
	    
	    int total_meas = 0;

	    for(int s=mi; s <= ma; s++)
	    {
		if(tsm.isRowSelected(s))
		{
		    TreePath tp = meas_cluster_tree.getPathForRow(s);

		    DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) tp.getLastPathComponent();

		    ExprData.Cluster cl = (ExprData.Cluster) dmtn.getUserObject();
		    
		    if( cl.getNumElements() > 0 )
		    {
			sel_cls.addElement(cl);
			total_meas += cl.getNumElements();
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
		//  convert those selected clusters into lists of Spot IDs
		//  (but keep an 'grouping index' mapping each entry
		//  in the list back onto the cluster which owns it)
		
		final String[] group_names = new String[ sel_cls.size() ];
		final int[]  meas_ids      = new int[ total_meas ];
		final int[] meas_group     = new int[ total_meas ];  

		int index = 0;

		for(int c = 0 ; c < sel_cls.size(); c++ )
		{
		    ExprData.Cluster cl = (ExprData.Cluster) sel_cls.elementAt( c );
		    
		    group_names[ c ] = cl.getName();
		    
		    int[] elements = cl.getElements();    // this is always a set of Spot IDs
		    
		    for( int e = 0; e < elements.length; e++ )
		    {
			meas_ids[ index ] = elements[ e ];
			
			meas_group[ index ] = c;
		    }
		    
		    index++;

		}
		
		System.out.println( meas_ids.length + " Measurement selections..." );

		selection = new Selection( group_names, meas_ids, meas_group );

	    }


	    viewer.selectionHasChanged();
	}

    }



    // ========================================================================================
    //
    // state
    //
    // ========================================================================================


    private int[][] sel_meas_ids = null;

    private maxdView mview;
    private ExprData edata;
    private NewProfileViewer viewer;

    private Selection selection = null;

    private boolean selecting_individuals = true;

    private boolean disable_recusive_individual_tree_selection = false;

    private DragAndDropTree meas_indiv_tree;
    private DragAndDropTree meas_cluster_tree;

    private JComboBox meas_individual_names_jcb;
    
    private JScrollPane meas_cluster_scrollpane;
    private JScrollPane meas_indiv_scrollpane;

    private JPanel meas_indiv_pane;
    private JPanel meas_cluster_pane;

}