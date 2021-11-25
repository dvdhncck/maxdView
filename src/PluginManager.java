import javax.swing.*;
import javax.swing.event.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.table.*;
import javax.swing.tree.*;

import java.util.jar.*;
import java.util.zip.*;
import java.net.*;

public class PluginManager
{
    /*
      features:

      +	display a list of all known plugins (and their version numbers)
  
      + scan 'plugins' directory for anything that looks like a plugin

      +	hide (a.k.a. uninstall) unwanted plugins
  
      +	install and update plugins from a central repository
  
      +	support for redefining the menu hierarchy



      design:

        the 'master' tree contains all plugins, whether they are disabled or not.

	from this, the 'menu' tree is built, this tree only contains the plugins which are not disabled.

	the reason for this is so that menu items stay in the 'right place' when they are
	enabled/disabled without the user having to reposition them explictly.

    */

    public PluginManager( maxdView mview_ )
    {
	mview = mview_;

	disabled_plugin_hs = new HashSet();
	temp_disabled_plugin_hs = new HashSet();
	
	loadMenus();
    }

    public void showPluginManager( )
    {
	//menu_names_hs = new HashSet();

	// getPluginInfo();

	if(frame == null)
	{
	    frame = new JFrame("Plugin Manager");

	    mview.decorateFrame(frame);
	
	    frame.addWindowListener(new WindowAdapter() 
		{
		    public void windowClosing(WindowEvent e)
		    {
			// 
		    }
		});
    
	    buildGUI(frame);

	    frame.pack();
	}

	frame.setVisible(true);	
    }


    public void closePluginManager()
    {
	boolean pc = checkForPluginChanges();
	boolean mc = checkForMenuChanges();
	if( pc || mc )
	{
	    if(mview.infoQuestion( "Apply changes before closing?", "Yes", "No" ) == 0)
	    {
		if( pc )
		    applyPluginChanges();
		if( mc )
		    applyMenuChanges();
		
		saveMenus();
	    }
	    else
	    {
		setPluginsToMaster();
		copyTempMenuFromActiveMenu();
	    }
	}
	
	frame.setVisible(false);
    }

    // --------------------------------------------------------------------------------------

    private void checkTabChanging( )
    {
	if (tabbed.getSelectedIndex() == 0)
	{
	    // delay the tab change until any data changes have been applied or cancelled....

	    if( checkForMenuChanges() )
	    {
		if(mview.infoQuestion("Some changes have not been applied", "Apply now", "Discard") == 0)
		{
		    applyMenuChanges();
		}
		else
		{
		    copyTempMenuFromActiveMenu();
		}
	    }
	}

	if (tabbed.getSelectedIndex() == 1)
	{
	    // delay the tab change until any data changes have been applied or cancelled....

	    if( checkForPluginChanges() )
	    {
		if(mview.infoQuestion("Some changes have not been applied", "Apply now", "Discard") == 0)
		{
		    applyPluginChanges();
		}
		else
		{
		    setPluginsToMaster();
		}
	    }
	}
	updateTabControls();
    }

    private void updateTabControls()
    {
	update_button.setVisible( tabbed.getSelectedIndex() == 0 );
	scan_button.setVisible(   tabbed.getSelectedIndex() == 0 );
	
	add_button.setVisible(    tabbed.getSelectedIndex() == 1 );
	delete_button.setVisible( tabbed.getSelectedIndex() == 1 );
	rename_button.setVisible( tabbed.getSelectedIndex() == 1 );
	reset_button.setVisible(  tabbed.getSelectedIndex() == 1 );
    }

    // --------------------------------------------------------------------------------------

    private void buildGUI( JFrame frame )
    {
	JPanel panel = new JPanel();

	panel.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
	GridBagLayout gridbag = new GridBagLayout();
	panel.setLayout(gridbag);


	update_button = new JButton("Check for updates");
	update_button.setToolTipText("Query the maxdView website for new or updated plugins");
	scan_button   = new JButton("Scan 'plugins' directory");
	scan_button.setToolTipText("Search the plugins directory for newly installed plugins");

	add_button    = new JButton("New");
	delete_button = new JButton("Remove");
	rename_button = new JButton("Rename");
	reset_button  = new JButton("Reset");
	reset_button.setToolTipText("Restore the default menu configuration");

	tabbed = new JTabbedPane();

	add_button.setVisible( false );
	delete_button.setVisible( false );

	add_button.setEnabled( false );
	rename_button.setEnabled( false );
	delete_button.setEnabled( false );
	
	//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 

	plugin_table = new JTable( new PluginTableModel() );

	setPluginsToMaster();

	// column sorting ....
	MouseAdapter listMouseListener = new MouseAdapter() 
	    {
		public void mouseClicked(MouseEvent e) 
		{
		    PluginTableModel tableModel  = (PluginTableModel) plugin_table.getModel();
		    TableColumnModel columnModel = plugin_table.getColumnModel();
		    int viewColumn = columnModel.getColumnIndexAtX(e.getX()); 
		    int column = plugin_table.convertColumnIndexToModel(viewColumn); 
		    //if (e.getClickCount() == 1 && column != -1) 
		    if (column >= 0)
		    {
			tableModel.sortColumn(column); 
		    }
		}
	    };
        JTableHeader th = plugin_table.getTableHeader(); 
        th.addMouseListener(listMouseListener); 



	// make the first column twice the width of second, which is twice the width of the others
	for(int i = 0; i < 4; i++) 
	{
	    plugin_table.getColumnModel().getColumn(i).setPreferredWidth( i==0 ? 200 : (i == 1 ? 100 : 50)); 
	}
	

	JScrollPane jsp = new JScrollPane( plugin_table );

	tabbed.add( "  Plugins  ", jsp );
	tabbed.setToolTipTextAt(0, "View the complete list of all plugins" );

	//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 

	
	menu_tree = new DraggableTree(); 
	menu_tree.setAllowReparenting( false );

	// menu_tree.setRootVisible(false);

	menu_tree.setEditable(false);

	menu_tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
	
	//Listen for when the selection changes.
	menu_tree.addTreeSelectionListener(new TreeSelectionListener() 
	    {
		public void valueChanged(TreeSelectionEvent e) 
		{
		    treeSelectionChanged();
		}
	    });

	((DefaultTreeCellRenderer)menu_tree.getCellRenderer()).setLeafIcon( null );
	
	if(master_root == null)
	    master_root = makeDefaultMenuTree();
	
	if(master_root != null)
	{
	    menu_root = makeMenuTreeFromMasterTree();
	    
	    copyTempMenuFromActiveMenu();

	    menu_tree.setModel( new DefaultTreeModel( temp_menu_root ));
	}

	
	jsp = new JScrollPane( menu_tree );
	
	tabbed.add( "  Menus  ", jsp );
	tabbed.setToolTipTextAt(1, "View and adjust menu layout");


	//  - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - 

	GridBagConstraints c = new GridBagConstraints();
	c.gridx = 0; 
	c.gridy = 0;
	c.weighty = 8.0;
	c.weightx = 10.0;
	c.fill = GridBagConstraints.BOTH;
	gridbag.setConstraints(tabbed, c);
	panel.add(tabbed);



	JPanel buttons_panel = new JPanel();
	buttons_panel.setBorder(BorderFactory.createEmptyBorder(3,0,0,0));
	GridBagLayout inner_gridbag = new GridBagLayout();
	buttons_panel.setLayout(inner_gridbag);

	{   
	    JButton jb;
	    
	    
	    jb = update_button;
	    buttons_panel.add(jb);
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			startUpdateCheck();
		    }
		});

	    
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    inner_gridbag.setConstraints(jb, c);
	    
	    
	    jb = scan_button;
	    buttons_panel.add(jb);
	    
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			startPluginScan();
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    inner_gridbag.setConstraints(jb, c);



	    jb = add_button;
	    buttons_panel.add(jb);
	    
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			addMenu();
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    inner_gridbag.setConstraints(jb, c);

	    
	    jb = delete_button;
	    buttons_panel.add(jb);
	    
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			removeMenu();
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    inner_gridbag.setConstraints(jb, c);

	    jb = rename_button;
	    buttons_panel.add(jb);
	    
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			renameMenu();
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = 2;
	    inner_gridbag.setConstraints(jb, c);

	    jb = reset_button;
	    buttons_panel.add(jb);
	    
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			resetMenuHierarchy();
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = 3;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    inner_gridbag.setConstraints(jb, c);


	    Dimension fillsize = new Dimension(16,16);
	    Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
	    c = new GridBagConstraints();
	    c.gridx = 4;
	    c.weightx = 1.0;
	    // c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    inner_gridbag.setConstraints(filler, c);
	    buttons_panel.add(filler);


	    jb = new JButton("Help");
	    buttons_panel.add(jb);
	    
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			if(tabbed.getSelectedIndex() == 0)
			    mview.getHelpTopic("PluginManager", "#table");
			else
			    mview.getHelpTopic("PluginManager", "#tree");
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = 5;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.EAST;
	    inner_gridbag.setConstraints(jb, c);
	    
	    jb = new JButton("Apply");
	    buttons_panel.add(jb);
	    jb.setToolTipText("Apply any changes that have been made");

	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			if(tabbed.getSelectedIndex() == 0)
			    applyPluginChanges();
			else
			    applyMenuChanges();
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = 6;
	    inner_gridbag.setConstraints(jb, c);
	    
	    jb = new JButton("Close");
	    jb.setToolTipText("Close this window");

	    buttons_panel.add(jb);
	    
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			closePluginManager();
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = 7;
	    inner_gridbag.setConstraints(jb, c);
	}
	
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 1;
	c.fill = GridBagConstraints.HORIZONTAL;
	gridbag.setConstraints(buttons_panel, c);
	panel.add(buttons_panel);

	// important not to add this listener until all other components have been set up
	tabbed.addChangeListener(new ChangeListener ()
	    {
		public void stateChanged(ChangeEvent e) 
		{
		    checkTabChanging( );
		}
	    });

	updateTabControls();

	frame.getContentPane().add( panel );
    }

    // --------------------------------------------------------------------------------------
    
    public class PluginTableModel extends AbstractTableModel
    {
	// columns:  name, type, version_number, enable?

	public boolean isCellEditable(int row, int column) 
	{
	    return (column == 3);
	}

	public String getColumnName(int col) 
	{ 
	    return columnNames[col].toString(); 
	}

	public int getRowCount() { return ( plugin_info == null ) ? 0 : plugin_info.size(); }

	public int getColumnCount() { return 4; }

	public Object getValueAt( int row, int col ) 
	{ 
	    int real_row = (sort_order == null) ? row : sort_order[row];

	    PluginInfo pi = (PluginInfo) plugin_info.elementAt( real_row );

	    if(col == 3)
	    {
		return new Boolean( ! temp_disabled_plugin_hs.contains( pi.name ) );
	    }
	    else
	    {
		if(col == 0)
		    return pi.name;

		if(col == 1)
		    return pi.type;

		return String.valueOf(pi.version_major) + "." + 
		       String.valueOf(pi.version_minor) + "." +
		       String.valueOf(pi.build);
	    }
	}

	public void setValueAt(Object value, int row, int col) 
	{
	    int real_row = (sort_order == null) ? row : sort_order[row];

	    if(col == 3)
	    {
		PluginInfo pi = (PluginInfo) plugin_info.elementAt( real_row );
		if(const_true.equals( (Boolean) value ))
		    temp_disabled_plugin_hs.remove( pi.name );
		else
		    temp_disabled_plugin_hs.add( pi.name );
	    }

	    fireTableCellUpdated(row, col);
	}
	

	public Class getColumnClass(int c) 
	{
	    return getValueAt(0, c).getClass();
	}
	

	public void sortColumn( int c )
	{
	    final int row_count = getRowCount();

	    sort_order = null; 

	    SortTuple[] sta = new SortTuple[ plugin_info.size() ];
	    
	    for(int r=0; r < row_count; r++)
		// because sort_order is null, this array will be constructed in the 'real' order
		sta[r] = new SortTuple( getValueAt( r, c ), r ); 
	    
	    if(invert_column == null)
	    {
		invert_column = new boolean[ getColumnCount() ];
		for(int ci=0; ci < getColumnCount(); ci++)
		    invert_column[ci] = false;
	    }

	    java.util.Arrays.sort( sta, new SortTupleComparator( invert_column[ c ]) );
	    invert_column[c] = !invert_column[c];

	    sort_order = new int[ row_count ];
	    for(int r=0; r < row_count; r++)
		sort_order[r] = sta[r].index;
	}
	
	private class SortTuple 
	{ 
	    public Object value; 
	    public int index;
	    public SortTuple( Object v, int i ) { value = v; index = i; }
	}
	
	private class SortTupleComparator implements java.util.Comparator
	{
	    public SortTupleComparator( boolean invert_ )
	    {
		invert = invert_;
	    }
	    public final int compare(Object o1, Object o2)
	    {
		SortTuple st1 = (SortTuple) o1;
		SortTuple st2 = (SortTuple) o2;
		
		try
		{
		    return possiblyInvert(((String) st1.value).compareTo( (String) st2.value ));
		}
		catch(ClassCastException cce)
		{
		    // must be Boolean instead....
		    boolean b1 = ((Boolean) st1.value).booleanValue();
		    boolean b2 = ((Boolean) st2.value).booleanValue();
		    int cval = 0;  // assume (b1 == b2)
		    if(b1 == b2)
		    {
			return 0;
		    }
		    else
		    {
			if(b1 && !b2)
			    return  possiblyInvert(1);
			else
			    return  possiblyInvert(-1);
		    }
		}
	    }

	    public final int possiblyInvert( int input )
	    {
		if(invert)
		{
		    if(input == 0)
			return 0;
		    else
			return (input < 0) ? 1 : -1;
		}
		else
		    return input;
	    }

	    boolean invert;
	}
	
	final String[] columnNames = { "Name", "Type", "Version", "Enable?" };

	private int[]     sort_order = null;
	private boolean[] invert_column = null;
    }



    private void setColumnWidths( JTable table )
    {
	TableColumnModel tcm = table.getColumnModel();
	TableColumn tc = tcm.getColumn(0);

	int one_sixth = table.getWidth() / 6;
	
	tc.setWidth( 3 * one_sixth );
	tc.setPreferredWidth( 3 * one_sixth );
    }

	
    // --------------------------------------------------------------------------------------

    // --------------------------------------------------------------------------------------

    private void applyPluginChanges()
    {
	boolean something_has_changed = false;
	
	int enabled = 0;
	int disabled = 0;

	// figure out whether the current state of the gui is the same as the master state
	
	
	for(int p=0; p < plugin_info.size(); p++)
	{
	    PluginInfo pi = (PluginInfo) plugin_info.elementAt(p);
	    
	    boolean currently_disabled     = disabled_plugin_hs.contains( pi.name );
	    boolean desired_to_be_disabled = temp_disabled_plugin_hs.contains( pi.name );

	    if( currently_disabled != desired_to_be_disabled )
	    {
		// synchronise the value
		if( desired_to_be_disabled )
		    disabled_plugin_hs.add( pi.name );
		else
		    disabled_plugin_hs.remove( pi.name );

		//System.out.println( pi.name + " is now " + ( desired_to_be_disabled ? "disabled" : "enabled" ));

		// and record the fact that something has changed

		something_has_changed = true;
		
		if( desired_to_be_disabled )
		    disabled++;
		else
		    enabled++;
	    }
	}

	if( something_has_changed )
	{
	    // update the tree in the 'menu' tab 
	    menu_root = makeMenuTreeFromMasterTree();
	    copyTempMenuFromActiveMenu();
	    menu_tree.setModel( new DefaultTreeModel( temp_menu_root ));

	    // and save the results back to the config file
	    saveMenus();

	    // rebuild the menu hierarchy....
	    mview.buildMenus();
	    
	    // and report
	    String msg = "";
	    if(enabled > 0)
		msg += pluralise(enabled) + " enabled.\n";
	    if(disabled > 0)
		msg += pluralise(disabled) + " disabled.\n";
	    
	    mview.infoMessage( msg );
	}
	else
	{
	    mview.infoMessage( "There are no changes to apply." );
	}
    }

    private String pluralise( int val )
    {
	return (val == 1) ? "One plugin was" : (val + " plugins were");
    }

    // true iff the current 'enabled' state is not the same as the state of the table`
    private boolean checkForPluginChanges()
    {
	
	for(int p=0; p < plugin_info.size(); p++)
	{
	    PluginInfo pi = (PluginInfo) plugin_info.elementAt(p);
	    
	    boolean currently_disabled     = disabled_plugin_hs.contains( pi.name );

	    boolean desired_to_be_disabled = temp_disabled_plugin_hs.contains( pi.name );

	    if( currently_disabled != desired_to_be_disabled)
	    {
		//System.out.println( pi.name + " needs updating" );

		return true;
	    }
	}

	return false;
    }

    // synchronise the GUI state with that of the master
    private void setPluginsToMaster()
    {
	// make 'temp_disabled_plugin_hs' the same as 'disabled_plugin_hs'

	temp_disabled_plugin_hs = new HashSet();

	Iterator things = disabled_plugin_hs.iterator();
	while(things.hasNext())
	    temp_disabled_plugin_hs.add( things.next() );
	
	// and update the table model
	plugin_table.setModel( new PluginTableModel() );

	setColumnWidths(plugin_table);
    }

    // --------------------------------------------------------------------------------------
    // --------------------------------------------------------------------------------------
    //
    //    p l u g i n  s c a n n i n g
    //
    // --------------------------------------------------------------------------------------
    // --------------------------------------------------------------------------------------

    private void startPluginScan()
    {
	frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

	new ScanThread().start();
    }

    private class ScanThread extends Thread
    {
	public void run()
	{
	    mview.scanPlugins();
	    
	    // need to rebuild the plugin table at this point
	    // (in case any existing plugins have gone, or new ones have arrived)
	    
	    plugin_info = mview.getPluginInfoObjects();
	    
	    // System.out.println("there are " + plugin_info.size() + " plugins...");
	    
	    // update the version numbers and disabled state in the GUI
	    setPluginsToMaster();
	    
	    // and likewise for the menu tree
	    
	    addNewPluginsToMasterRoot();
	    removeMissingPluginsFromMasterRoot();
	    
	    //reconfigureMasterRoot( menu_root );
	    
	    menu_root = makeMenuTreeFromMasterTree();
	    copyTempMenuFromActiveMenu();
	    menu_tree.setModel( new DefaultTreeModel( temp_menu_root ));
	    
	    saveMenus();
	    
	    applyMenuChanges();

	    frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

	}
    }


    // --------------------------------------------------------------------------------------
    // --------------------------------------------------------------------------------------
    //
    //    m e n u    c u s t o m i s a t i o n
    //
    // --------------------------------------------------------------------------------------
    // --------------------------------------------------------------------------------------

    private void applyMenuChanges()
    {
	// copy temp to master....

	// this is quite hard as there are nodes in 'master' which are not in the 'temp',
	// (but they should remain where they are in 'master', whilst the other
	//  nodes should assume the same positions that they have in 'temp'....)


	// this bit is easy, update the active state from the temporary state....
	menu_root = deepCopyWithIgnore( temp_menu_root ); 

	// now rebuild the master state based on this new active state
	reconfigureMasterRoot( menu_root );
	   
	// and debug
	dumpMenuTree();
	
	saveMenus();

	// and make maxdView rebuild the menu hierarchy....
	mview.buildMenus();

	mview.updateHelpPages();

    }    

    // true iff the current menu tree is not the same as the state of the temporary tree (i.e. the gui state)
    private boolean checkForMenuChanges()
    {
	// is the temporary state the same as the active state?
	
	return( !deepCompare( menu_root, temp_menu_root ));
    }



    private void resetMenuHierarchy()
    {
	if( mview.infoQuestion("Really reset the menus to the default arrangement?", "Yes", "No") == 0)
	{
	    // completely reset the master tree

	    master_root = makeDefaultMenuTree();
	    
	    if(master_root != null)
	    {
		menu_root = makeMenuTreeFromMasterTree();
		
		copyTempMenuFromActiveMenu();

		saveMenus();

		applyMenuChanges();
	    }
	}
    }

    // synchronise the temporary state (i.e. that in the GUI) with that of the active state
    private void copyTempMenuFromActiveMenu()
    {
	temp_menu_root = makeMenuTreeFromMasterTree();
	menu_tree.setModel( new DefaultTreeModel( temp_menu_root ));
    }

    private DefaultMutableTreeNode makeDefaultMenuTree()
    {
	DefaultMutableTreeNode top = new DefaultMutableTreeNode("Menus");

	DefaultMutableTreeNode import_menu = new DefaultMutableTreeNode("Import");
	addEntries( import_menu, "importer" );

	DefaultMutableTreeNode export_menu = new DefaultMutableTreeNode("Export");
	addEntries( export_menu, "exporter" );
	
	DefaultMutableTreeNode transform_menu = new DefaultMutableTreeNode("Transform");
	addEntries( transform_menu, "transform" );

	DefaultMutableTreeNode filter_menu = new DefaultMutableTreeNode("Filter");
	addEntries( filter_menu, "filter" );
	
	DefaultMutableTreeNode viewer_menu = new DefaultMutableTreeNode("View");
	addEntries( viewer_menu, "viewer" );

	top.add( import_menu );
	top.add( export_menu );
	top.add( transform_menu );
	top.add( filter_menu );
	top.add( viewer_menu );

	return top;
    }

    
    private void addEntries( DefaultMutableTreeNode node, String plugin_type )
    {
	for(int p=0; p < plugin_info.size(); p++)
	{
	    PluginInfo pi = (PluginInfo) plugin_info.elementAt(p);
	    
	    if(pi.type.equals( plugin_type ))
		node.add( new DefaultMutableTreeNode( pi.name ));
	}
    }
    
    // convert the 'master' tree (which contains all plugins) to the 'menu' tree
    // which only contains plugins which are not disabled
    private DefaultMutableTreeNode makeMenuTreeFromMasterTree()
    {
	return deepCopyWithIgnore( master_root );
    }

    private void reconfigureMasterRoot( DefaultMutableTreeNode active_root )
    {
	// firstly, save the positions of all nodes which represent disabled plugins....

	Hashtable disabled_plugins_by_menu = new Hashtable();

	Enumeration nodes = master_root.breadthFirstEnumeration();
	while( nodes.hasMoreElements() )
	{
	    DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
	    String pname = (String) node.getUserObject();

	    if( disabled_plugin_hs.contains( pname ))
	    {
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();

		if(parent != null)
		{
		    String menu_name = (String) parent.getUserObject();
		    
		    Vector disabled_plugins_in_this_menu = (Vector) disabled_plugins_by_menu.get( menu_name );
		    if(disabled_plugins_in_this_menu == null)
		    {
			disabled_plugins_in_this_menu = new Vector();
			disabled_plugins_by_menu.put( menu_name, disabled_plugins_in_this_menu );
		    }
		    disabled_plugins_in_this_menu.addElement(pname);
		}
	    }
	}

	// now we have a vector of disabled plugin names for each (sub)menu

	// rebuild the master by copying the active menu

	master_root = deepCopy( active_root );
	
	// go through the nodes and add the data we collected above

	if( master_root == null )
	    return;

	nodes = master_root.breadthFirstEnumeration();

	while( nodes.hasMoreElements() )
	{
	    DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();

	    if(node.getChildCount() > 0)
	    {
		String menu_name = (String) node.getUserObject();
		
		Vector disabled_plugins_in_this_menu = (Vector) disabled_plugins_by_menu.get( menu_name );

		if(disabled_plugins_in_this_menu != null)
		{
		    for(int n=0; n < disabled_plugins_in_this_menu.size(); n++)
		    {
			String chld = (String) disabled_plugins_in_this_menu.elementAt(n);
			node.add( new DefaultMutableTreeNode(chld) );
		    }
		}
	    }
	}
	
    }

    // scan through the master tree and insert any plugins which are not disabled,
    // and not present (called after a plugin scan to insert any new plugins into
    // the tree)

    private void addNewPluginsToMasterRoot()
    {
	// pre: plugin_info vector has been updated to the latest state


	// build a collection of all of the plugins currently in the tree

	HashSet exists = new HashSet();

	
	Enumeration nodes = master_root.breadthFirstEnumeration();
	while( nodes.hasMoreElements() )
	{
	   DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
	   exists.add( node.getUserObject() );
	}

	// now scan through the plugin_info vector looking for anything that should be added

	for(int p=0; p < plugin_info.size(); p++)
	{
	    PluginInfo pi = (PluginInfo) plugin_info.elementAt(p);
	    
	    if( ! exists.contains( pi.name ))
	    {
		System.out.println( pi.name + " should be added (type=" + pi.type);

		addPluginEntryToMasterRoot( pi.name, pi.type );
	    }

	}
    }

    // scan through the master tree and remove any plugins which no longer exist
    // (called after a plugin scan to synchronize the tree with reality)

    private void removeMissingPluginsFromMasterRoot()
    {
	

    }


    // add a new entry called 'pname' into the (sub)menu specified by 'ptype'

    private void addPluginEntryToMasterRoot( String pname, String ptype )
    {


	// locate the correct menu

	String ptype_lc = ptype.toLowerCase();

	// convert to 'revised naming scheme'
	if(ptype.equals("viewer"))
	    ptype_lc = "view";
	if(ptype.equals("importer"))
	    ptype_lc = "import";
	if(ptype.equals("exporter"))
	    ptype_lc = "export";

	

	Enumeration nodes = master_root.breadthFirstEnumeration();
	while( nodes.hasMoreElements() )
	{
	    DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();

	    if(!node.isLeaf())
	    {
		String mname = (String) node.getUserObject();
		if(mname.toLowerCase().equals(ptype_lc))
		{
		    node.add( new DefaultMutableTreeNode(pname) );
		    return;
		}
	    }
	}
	
	// we didn't find the menu with a suitable name, create one a a new top-level

	DefaultMutableTreeNode new_menu  = new DefaultMutableTreeNode(ptype);
	new_menu.add(  new DefaultMutableTreeNode( pname ));

	master_root.add(new_menu);

    }

    // ------ gui handling ------

    private void treeSelectionChanged()
    {
	DefaultMutableTreeNode node = (DefaultMutableTreeNode) menu_tree.getLastSelectedPathComponent();
	
	if (node == null) 
	    return;
	
	/*
	add_button.setEnabled( node.isLeaf() == false );

	rename_button.setEnabled( node.isLeaf() == false );

	delete_button.setEnabled( node.isLeaf() == false );
	*/

	add_button.setEnabled( false );
	rename_button.setEnabled( false );
	delete_button.setEnabled( false );

    }

    private void renameMenu()
    {

	try
	{
	    DefaultMutableTreeNode node = (DefaultMutableTreeNode) menu_tree.getLastSelectedPathComponent();

	    String current_name = (String) node.getUserObject();
	    String name = mview.getString("New name for new menu", current_name);
	    
	    // TODO:: check that there isn't already a node with this name....
	    
	    node.setUserObject( new String( name ));

	    DefaultTreeModel tmodel = (DefaultTreeModel) menu_tree.getModel();
	    tmodel.nodeChanged(node);

	}
	catch(UserInputCancelled uic)
	{
	}

    }

    private void removeMenu()
    {
	DefaultMutableTreeNode node = (DefaultMutableTreeNode) menu_tree.getLastSelectedPathComponent();

	if(node.isLeaf())
	    return;

	int num_children = node.getChildCount();

	if(num_children > 0)
	{
	    mview.alertMessage("This menu cannot be removed as it is not empty");
	}
	else
	{
	    DefaultTreeModel tmodel = (DefaultTreeModel) menu_tree.getModel();
	    tmodel.removeNodeFromParent( node );
			
	}
    }

    private void addMenu()
    {
	try
	{
	    String name = mview.getString("Name for new menu");
	    
	    // check that there isn't already a node with this name....

	    DefaultMutableTreeNode node = (DefaultMutableTreeNode) menu_tree.getLastSelectedPathComponent();

	    DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();

	    DefaultTreeModel tmodel = (DefaultTreeModel) menu_tree.getModel();
	    
	    int position = 0;

	    if(node == null)
	    {
		node = (DefaultMutableTreeNode) tmodel.getRoot();
	    }
	    else
	    {
		position = tmodel.getIndexOfChild( parent, node ) - 1;
		if(position < 0)
		    position = 0;
	    }
	    
	    DefaultMutableTreeNode new_node = new DefaultMutableTreeNode( name );

	    tmodel.insertNodeInto( parent, new_node, position );
	}
	catch(UserInputCancelled uic)
	{
	}
    }

    // --------------------------------------------------------------------------------------
    // various tree maintenance stuff....

    private DefaultMutableTreeNode deepCopyWithIgnore( DefaultMutableTreeNode node )
    {
	if( node == null )
	    return null;

	String uo = (String) node.getUserObject();

	// if this is a leaf node (i.e. a plugin name) then check whether it has been disabled
	if( node.getChildCount() == 0)
	{
	    if( disabled_plugin_hs.contains( uo ) )
		return null;
	    
	    // mkake sure it is still known to the system (i.e. it hasn't been deleted)
	    if( mview.getPluginInfoFromName( uo ) == null )
		return null;
	}

	DefaultMutableTreeNode copy = new DefaultMutableTreeNode();
	
	copy.setUserObject( uo );
	
	if(node.getChildCount() > 0)
	{
	    for(int c=0; c < node.getChildCount(); c++)
	    {
		DefaultMutableTreeNode chld = deepCopyWithIgnore((DefaultMutableTreeNode) node.getChildAt( c ) ) ;
		if (chld != null ) 
		    copy.add( chld );
	    }
	}
	
	return copy;
    }

    private DefaultMutableTreeNode deepCopy( DefaultMutableTreeNode node )
    {
	if( node == null )
	    return null;

	String uo = (String) node.getUserObject();

	DefaultMutableTreeNode copy = new DefaultMutableTreeNode();
	
	copy.setUserObject( uo );
	
	if(node.getChildCount() > 0)
	{
	    for(int c=0; c < node.getChildCount(); c++)
	    {
		DefaultMutableTreeNode chld =   deepCopyWithIgnore((DefaultMutableTreeNode) node.getChildAt( c ) ) ;
		if (chld != null ) 
		    copy.add( chld );
	    }
	}
	
	return copy;
    }

    // true iff 'a' and 'b' are exactly the same
    private boolean deepCompare( DefaultMutableTreeNode a , DefaultMutableTreeNode b )
    {
	String ua = (String) a.getUserObject();
	String ub = (String) b.getUserObject();

	if(!ua.equals(ub))
	    return false;
	
	if(a.getChildCount() != b.getChildCount())
	    return false;

	if(a.getChildCount() > 0)
	{
	    for(int c=0; c < a.getChildCount(); c++)
	    {
		boolean ok = deepCompare( (DefaultMutableTreeNode) a.getChildAt( c ), 
					  (DefaultMutableTreeNode) b.getChildAt( c ) );
		if(!ok)
		    return false;
	    }
	}

	return true;
    }

    // --------------------------------------------------------------------------------------

    private void loadMenus()
    {
	menu_root = new DefaultMutableTreeNode("Menus");
	
	plugin_info = mview.getPluginInfoObjects();
	
	disabled_plugin_hs.clear();
	
	try 
	{ 
	    File file = new File( mview.getConfigDirectory() + "mainmenu.dat" );
	    BufferedReader br = new BufferedReader(new FileReader(file));
		
	    String format = br.readLine();

	    if( format == null )
	    {
		// file missing, or knackered
		master_root = makeDefaultMenuTree();
		return;
	    }

	    if(format.startsWith("maxdView menu format 1.0"))
	    {
		master_root = readNode( br );
	    }
	    else
	    {
		mview.alertMessage("Unable to read menu arrangement file\n(wrong format?)");
		System.out.println( "format:'" + format + "' ?" );
	    }

	}
	catch(FileNotFoundException fnfe)
	{ 
	    // no problem, just use default values....
	    master_root = makeDefaultMenuTree();

	}
	catch (IOException ioe) 
	{ 
	    mview.errorMessage("Unable to read menu arrangement file\n\n" + ioe);

	    // use default values
	    master_root = makeDefaultMenuTree();

	}
	
	if(master_root != null)
	{
	    menu_root = makeMenuTreeFromMasterTree();
	}
    }
    
    private DefaultMutableTreeNode readNode( BufferedReader reader ) throws java.io.IOException
    {
	DefaultMutableTreeNode top_node = null;
	DefaultMutableTreeNode parent = top_node;   // always points at the current 'parent'

	boolean done = false;

	int current_depth = 0;

	while(!done)
	{
	   String data = reader.readLine();

	   if(data == null)
	   {
	       done = true;
	   }
	   else
	   {
	       if( data.charAt(0)=='+' ) // start a new submenu
	       {
		   DefaultMutableTreeNode new_menu = new DefaultMutableTreeNode( data.substring(1) );
		   if(top_node == null)
		   {
		       top_node = new_menu;
		       parent = top_node;
		   }
		   else
		   {
		       parent.add(new_menu);
		       parent = new_menu;
		   }
	       }
	       else
	       {
		   if( data.charAt(0)=='-' ) // end of this list of entries
		   {
		       parent = (DefaultMutableTreeNode)parent.getParent();
		   }
		   else // its an entry in the current list
		   {
		       DefaultMutableTreeNode new_entry;
		       
		       if( data.charAt(0)=='!' )
		       {
			   // when the name is prefixed by '!', the plugin is disabled

			   String name = data.substring(1);

			   new_entry = new DefaultMutableTreeNode( name ); 

			   // update both the master and temporary disabled lists
			   disabled_plugin_hs.add( name );
		       }
		       else
		       {
			   // its a normal (i.e. not disabled) entry
			   new_entry = new DefaultMutableTreeNode( data );
		       }

		       parent.add(new_entry);
		   }

	       }
	   }
	}

	return top_node;
    }


    // --------------------------------------------------------------------------------------
    
    private void saveMenus()
    {
	try
	{
	    File file = new File( mview.getConfigDirectory() + "mainmenu.dat" );
	    
	    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
	    
	    writer.write("maxdView menu format 1.0\n");
	    
	    // the menu hierarchy
	    writeNode( writer, master_root );
	    
	    writer.close();
	}
	catch(IOException ioe)
	{
	    mview.alertMessage("Unable to write menu arrangement file\n\n" + ioe);
	}
    }

    private void writeNode( Writer writer, DefaultMutableTreeNode node ) throws IOException
    {
	if( node == null )
	    return;

	writer.write( "+" + (String) node.getUserObject() + "\n");

	for(int c=0; c < node.getChildCount(); c++)
	{
	   DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt( c );
	   
	   if(child.getChildCount() > 0)
	   {
	       // a recursive call to handle the submenus
	       
	       writeNode( writer, child );
	   }
	   else
	   {
	       // otherwise write the list of child entries

	       String pname = (String) child.getUserObject();

	       if ( disabled_plugin_hs.contains( pname ) )
		   writer.write("!");

	       writer.write( pname + "\n" );
	   }
	}

	writer.write( "-\n");
    }

    // --------------------------------------------------------------------------------------

    // this interface used by maxdView to contruct it's JMenuBar 
    //

    public int getTopLevelMenuCount()
    {
	return menu_root.getChildCount();
    }

    public String[] getTopLevelMenuNames( )
    {
	final String[] names = new String[ menu_root.getChildCount() ];

	for(int m=0; m < menu_root.getChildCount(); m++ )
	{
	    DefaultMutableTreeNode node = (DefaultMutableTreeNode) menu_root.getChildAt(m);
	    names[m] = (String) node.getUserObject();
	}
	return names;
    }
    
    public String getTopLevelMenuName( int menu_number )
    {
	DefaultMutableTreeNode node = (DefaultMutableTreeNode) menu_root.getChildAt( menu_number );
	return (String) node.getUserObject();
    }

    public DefaultMutableTreeNode getMenuRoot( )
    {
	return menu_root;
    }

    public boolean isPluginDisabled( String name )
    {
	return disabled_plugin_hs.contains( name );
    }

    // --------------------------------------------------------------------------------------

    private void dumpMenuTree()
    {

    }


    // --------------------------------------------------------------------------------------
    // --------------------------------------------------------------------------------------
    //
    //    automatic plugin updating
    //
    // --------------------------------------------------------------------------------------
    // --------------------------------------------------------------------------------------

    /*
    private void checkForPluginUpdates()
    {
        // list of plugins which are newer than the installed version

	// list of plugins which are availablem but not installed

	PluginData[] pdata = getLatestInfo();

	if(pdata == null)
	{
	    mview.alertMessage("Unable to contact website to get\ninformation about the latest plugins");
	    return;
	}

	for(int p=0;  p < pdata.length; p++)
	    System.out.println( pdata[p] );
    }
    */
    /*
    private PluginData[] getLatestInfo()
    {
	final String loc = "file://C:\\Documents and Settings\\dave\\maxdView_plugin_library.dat";

	PluginData pd = null;
	Vector data_v = new Vector();

	try
	{
	    // java.net.URL plugin_info = new java.net.URL(loc);

	    File tmp = new File( "C:\\Documents and Settings\\dave\\plugin_library.dat" );
	    java.net.URL plugin_info = tmp.toURL();
	    
	    BufferedReader in = new BufferedReader(new InputStreamReader(plugin_info.openStream()));
	    
	    if(in == null)
		return null;

	    String input_line;
	    
	    while ((input_line = in.readLine()) != null)
	    {
		// System.out.println( input_line );

		if( input_line.startsWith( "[" ))
		{
		    // entry for a new plugin
		    
		    pd = new PluginData();

		    pd.name = input_line.substring(1, input_line.length()-2).trim();
		    
		    data_v.addElement( pd );
		}
		
		if( input_line.startsWith( "short_desc:" ))
		{
		    pd.short_desc = input_line.substring(11).trim();
		}
		
		if( input_line.startsWith( "author:" ))
		{
		    pd.author = input_line.substring(7).trim();
		}

		if( input_line.startsWith( "version:" ))
		{
		    pd.short_desc = input_line.substring(8).trim();
		}

		if( input_line.startsWith( "release_date:" ))
		{
		    pd.release_date = input_line.substring(13).trim();
		}

		if( input_line.startsWith( "jar_url:" ))
		{
		    pd.release_date = input_line.substring(8).trim();
		}
	    }	
	    in.close();

	    return (PluginData[]) data_v.toArray( new PluginData[0] );
	}
	catch(java.net.MalformedURLException murle)
	{
	}
	catch(java.io.IOException ioe)
	{
	}
	return null;
    }
    */

	//
	// contents of the 'latest-plugin-info' file:
	//
	//   for each available plugin 
	//     -  the newest version number, 
	//     -  the URL of the corresponding JAR file
	//     -  a description string
	//     -  a 'whats new' string
	//     -  a (list of) maxdView version numbers known to work 
	//     -  an optional (list of) maxdView version numbers known not to work 
	//

    /*
    private class PluginData
    {
	String name;
	String version;
	String author;
	String release_date;
	String short_desc;
	String known_to_work_with;
	String not_known_to_work_with;
	String jar_url;

	public String toString() 
	{
	    return
		"name:" + name + "\n" + 
		"version:" + version + "\n" + 
		"author:" + author + "\n" + 
		"release_date:" + release_date  + "\n" + 
		"short_desc:" + short_desc + "\n" + 
		"known_to_work_with:" + known_to_work_with + "\n" + 
		"not_known_to_work_with:" + not_known_to_work_with + "\n";
	}
    }
    */

    // --------------------------------------------------------------------------------------
    // --------------------------------------------------------------------------------------
    //
    //    automatic plugin updating
    //
    // --------------------------------------------------------------------------------------
    // --------------------------------------------------------------------------------------

    private class PluginData
    {
	public String name;
	public String type;
	public int    version_major, version_minor, version_build;
	public String author;
	public String release_date;
	public Vector short_desc_v;

	public VerNum[] compatible_with;
	public VerNum[] not_compatible_with;

	public String jar_url;

	public String toString() 
	{
	    return
		"name:         " + name + "\n" + 
		"version:      " + version_major + "." + version_minor + "." + version_build + "\n" + 
		"author:       " + author + "\n" + 
		"release_date: " + release_date  + "\n";
	}
    }

    private void startUpdateCheck()
    {
	pm = new ProgressOMeter("Contacting website");
	
	pm.startIt();
	frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	    
	new PluginUpdateThread().start();
    }


    private class PluginUpdateThread extends Thread
    {
	public void run()
	{
	   PluginData[] pdata = getLatestInfo();

	   pm.stopIt();
	   frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	   
	   checkForPluginUpdates( pdata) ;
	}
    }

    private void checkForPluginUpdates( PluginData[] pdata )
    {
        // list of plugins which are newer than the installed version
	// a and
	// list of plugins which are available but not installed

	
	if(pdata == null)
	{
	    mview.alertMessage("Unable to contact website to get\ninformation about the latest plugins");
	    return;
	}

	System.out.println("information about " + pdata.length + " plugins retreived");

	VerNum this_mv = new VerNum();
	this_mv.series  = mview.version_series;
	this_mv.major   = mview.version_major;
	this_mv.minor   = mview.version_minor;
	this_mv.variant = mview.version_prerelease_variant;

	Vector new_compatible_plugins   = new Vector();
	Vector new_incompatible_plugins = new Vector();
	Vector updated_compatible_plugins   = new Vector();
	Vector updated_incompatible_plugins = new Vector();

	for(int p=0;  p < pdata.length; p++)
	{
	    PluginInfo pinf = mview.getPluginInfoFromName( pdata[p].name );

	    if(pinf == null)
	    {
		// this plugin which is not known to the system

		//System.out.println( "'" +  pdata[p].name + "' is NEW");

		if( isCompatible( pdata[p] ) )
		{
		    new_compatible_plugins.add( pdata[p] );
		}
		else
		{
		    new_incompatible_plugins.add( pdata[p] );
		}

	    }
	    else
	    {
		// some version of this plugin is already installed

		if( isNewerPluginVersion( pdata[p].version_major,pdata[p].version_minor, pdata[p].version_build, 
					  pinf.version_major, pinf.version_minor, pinf.build ))
		{
		    // this plugin is newer than the version currently installed
		    
		    // System.out.println( "'" +  pdata[p].name + "' is UPDATED");

		    if( isCompatible( pdata[p] ) )
		    {
			 updated_compatible_plugins.add( pdata[p] );
		    }
		    else
		    {
			 updated_incompatible_plugins.add( pdata[p] );
		    }
		} 
	    }
	}
	
	//System.out.println("Compatible new plugins: " + new_compatible_plugins.size());
	//System.out.println("Incompatible new plugins: " + new_incompatible_plugins.size());
	//System.out.println("Updated plugins: " + updated_compatible_plugins.size());
	//System.out.println("Incompatible updated plugins: " + updated_incompatible_plugins.size());
	
	if( ( new_compatible_plugins.size() +  new_incompatible_plugins.size() +
	      updated_compatible_plugins.size() + updated_incompatible_plugins.size() ) == 0 )
	{
	    mview.infoMessage("There are no new or updated plugins");
	}
	else
	{
	    displayPluginUpdateResults( pdata,
					new_compatible_plugins, new_incompatible_plugins, 
					updated_compatible_plugins, updated_incompatible_plugins);
	}
    }


    private void displayPluginUpdateResults( final PluginData[] pdata,
					     final Vector new_compatible_plugins, 
					     final Vector new_incompatible_plugins, 
					     final Vector updated_compatible_plugins, 
					     final Vector updated_incompatible_plugins)
    {
	final JFrame frame = new JFrame("Plugin Update List");
	
	mview.decorateFrame(frame);

	JPanel panel = new JPanel();
	panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	
	JLabel label;
	GridBagConstraints c;

	GridBagLayout gridbag = new GridBagLayout();
	panel.setLayout(gridbag);

	Object[][] data = null;
	String[]   col_names = { "Name", "Type", "Released", "Version", "Status" };
	int pos=0;
	JScrollPane jsp = null;

	int line = 0;

	final int total_adds = new_compatible_plugins.size() + updated_compatible_plugins.size();

	final JTable add_table = new JTable();
	    
	//  - - - - - - - - - - - 
	
	if(total_adds > 0)
	{
	    label = new JLabel("The following plugins can be installed or updated");
	    c = new GridBagConstraints();
	    c.gridy = line++;
	    c.weightx = 10.0;
	    gridbag.setConstraints(label, c);
	    panel.add(label);
	    
	    label = new JLabel("Select one or more items from this list");
	    label.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridy = line++;
	    c.weightx = 10.0;
	    gridbag.setConstraints(label, c);
	    panel.add(label);
	
	    label = new JLabel("Double-click on an item to see a brief description");
	    label.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridy = line++;
	    c.weightx = 10.0;
	    gridbag.setConstraints(label, c);
	    panel.add(label);
	
	    add_table.setColumnSelectionAllowed(false);
	    
	    data = new String[ total_adds ][];
	    
	    pos=0;
	    for(int item=0; item < new_compatible_plugins.size(); item++)
	    {
		data[pos] = new String[ 5 ];
		PluginData pd = (PluginData) new_compatible_plugins.elementAt(item);
		data[pos][0] = pd.name;
		data[pos][1] = pd.type;
		data[pos][2] = pd.release_date;
		data[pos][3] = pd.version_major + "." +  pd.version_minor + "." +  pd.version_build;
		data[pos][4] = "New"; 	
		pos++;
	    }

	    for(int item=0; item < updated_compatible_plugins.size(); item++)
	    {
		data[pos] = new String[ 5 ];
		PluginData pd = (PluginData) updated_compatible_plugins.elementAt(item);
		data[pos][0] = pd.name;
		data[pos][1] = pd.type;
		data[pos][2] = pd.release_date;
		data[pos][3] = pd.version_major + "." +  pd.version_minor + "." +  pd.version_build;
		data[pos][4] = "Updated"; 	
		pos++;
	    }
	    
	    add_table.setModel( new ImmutableTableModel( data, col_names ));
	    
	    add_table.addMouseListener( new MouseAdapter()
		{
		    public void mouseClicked(MouseEvent e) 
		    {
			if (e.getClickCount() == 2) 
			{
			    showPluginDescription( pdata, add_table );
			}
		    }
		});


	    jsp = new JScrollPane(add_table);
	    c = new GridBagConstraints();
	    c.gridy = line++;
	    c.weightx = 10.0;
	    c.weighty = 4.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(jsp, c);
	    panel.add(jsp);
	}

	//  - - - - - - - - - - - 

	final int total_info = new_incompatible_plugins.size() + updated_incompatible_plugins.size();
	
	if(total_info > 0)
	{
	    label = new JLabel("The following plugins cannot be installed");
	    c = new GridBagConstraints();
	    c.gridy = line++;
	    c.weightx = 10.0;
	    gridbag.setConstraints(label, c);
	    panel.add(label);
	    
	    label = new JLabel("as they require a newer version of maxdView");
	    c = new GridBagConstraints();
	    c.gridy = line++;
	    c.weightx = 10.0;
	    gridbag.setConstraints(label, c);
	    panel.add(label);
	    
	    
	    label = new JLabel("Double-click on an item to see a brief description");
	    label.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridy = line++;
	    c.weightx = 10.0;
	    gridbag.setConstraints(label, c);
	    panel.add(label);
	
	    
	    final JTable info_table = new JTable();
	    info_table.setRowSelectionAllowed(false);
	    info_table.setColumnSelectionAllowed(false);
	    
	    
	    data = new String[ total_info ][];
	    
	    pos=0;
	    for(int item=0; item < new_incompatible_plugins.size(); item++)
	    {
		data[pos] = new String[ 5 ];
		PluginData pd = (PluginData) new_incompatible_plugins.elementAt(item);
		data[pos][0] = pd.name;
		data[pos][1] = pd.type;
		data[pos][2] = pd.release_date;
		data[pos][3] = pd.version_major + "." +  pd.version_minor + "." +  pd.version_build;
		data[pos][4] = "New"; 	
		pos++;
	    }
	    
	    for(int item=0; item < updated_incompatible_plugins.size(); item++)
	    {
		data[pos] = new String[ 5 ];
		PluginData pd = (PluginData) updated_incompatible_plugins.elementAt(item);
		data[pos][0] = pd.name;
		data[pos][1] = pd.type;
		data[pos][2] = pd.release_date;
		data[pos][3] = pd.version_major + "." +  pd.version_minor + "." +  pd.version_build;
		data[pos][4] = "Updated"; 	
		pos++;
	    }
	    
	    info_table.setModel( new ImmutableTableModel( data, col_names ));
	    
	    info_table.addMouseListener( new MouseAdapter()
		{
		    public void mouseClicked(MouseEvent e) 
		    {
			if (e.getClickCount() == 2) 
			{
			    showPluginDescription( pdata, info_table );
			}
		    }
		});



	    jsp = new JScrollPane(info_table);
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    c.weightx = 10.0;
	    c.weighty = 4.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(jsp, c);
	    panel.add(jsp);
	}

	// - - - - - - - - - - - 

	JButton button;
	JPanel wrapper = new JPanel();
	wrapper.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
	
	if( total_adds > 0 )
	{
	    button = new JButton("Install");
	    button.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			//compile the list of selected plugins:
			
			int rows = add_table.getRowCount();
			
			Vector install_v = new Vector();
			ListSelectionModel lsm = add_table.getSelectionModel();

			for(int r=0; r < rows; r++)
			{
			    if( lsm.isSelectedIndex(r))
			    {
				String     pl_name = (String) add_table.getValueAt( r, 0 );
				PluginData pl_data  = getDataFromName( pdata, pl_name );
				install_v.add( pl_data );
			    }
			}
			if( install_v.size() == 0)
			{
			    mview.alertMessage("No plugins have been selected");
			}
			else
			{
			    frame.setVisible(false);
			    startInstallerThread( install_v );
			}
		    }
		});
	    wrapper.add(button);
	}

	button = new JButton("Cancel");
	button.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    frame.setVisible(false);
		}
	    });
	wrapper.add(button);
	
	c = new GridBagConstraints();
	c.gridy = line++;
	c.weightx = 10.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	gridbag.setConstraints(wrapper, c);
	panel.add(wrapper);
	
	// - - - - - - - - - - - 
	
	panel.setPreferredSize( new Dimension( 500, 400 ));
	frame.getContentPane().add(panel);
	frame.pack();
	frame.setVisible(true);
    }

    public class ImmutableTableModel extends DefaultTableModel
    {
	public ImmutableTableModel(Object[][] data, Object [] cols)
	{
	    super(data,cols);
	}
	public boolean isCellEditable(int row, int column) 
	{
	    return false;
	}
    }  
  
    private PluginData getDataFromName(  final PluginData[] pdata, final String name )
    {
	for(int p=0; p < pdata.length; p++)
	    if(pdata[p].name.equals(name))
		return pdata[p];
	return null;
    }

    private void showPluginDescription( final PluginData[] pdata_a, final JTable table )
    {

	int row = ((ListSelectionModel)table.getSelectionModel()).getAnchorSelectionIndex();
	
	if(row >= 0)
	{
	    String pname = (String) table.getValueAt( row, 0 );

	    PluginData pdata = getDataFromName( pdata_a, pname );

	    String desc = pdata.name + "\n  Version: " + pdata.version_major + "." + pdata.version_minor + "." + pdata.version_build + "\n";

	    if(( pdata.author != null )  && ( pdata.author != null ) )
	    {
		desc += "  Author: " + pdata.author + "\n\n";
	    }
	    else
	    {
		desc += "\n";
	    }

	    for(int d=0; d < pdata.short_desc_v.size(); d++)
	    {
		if(d > 0)
		    desc += "\n";
		
		desc += (String) pdata.short_desc_v.elementAt(d);
	    }

	    if( pdata.short_desc_v.size() == 0)
	    {
		desc += "No description available for this plugin";
	    }
	    
	    mview.infoMessage(desc);

	}
    }

    // ==============================
    // website communications
    // ==============================


    private PluginData[] getLatestInfo()
    {
	final String loc = "http://www.bioinf.man.ac.uk/microarray/maxd/dist/maxdView_plugin_library.dat";

	
	PluginData pd = null;
	Vector data_v = new Vector();

	try
	{
	    java.net.URL plugin_info = new java.net.URL(loc);

	    URLConnection uc = plugin_info.openConnection();
	    uc.setUseCaches( false );

	    //File tmp = new File( "C:\\Documents and Settings\\dave\\maxdView_plugin_library.dat" );
	    //java.net.URL plugin_info = tmp.toURL();
	    
	    BufferedReader in = new BufferedReader(new InputStreamReader( uc.getInputStream() ) );
	    
	    if(in == null)
		return null;

	    String input_line;
	    
	    while ((input_line = in.readLine()) != null)
	    {
		pm.setMessage("Receiving information");


		boolean good_line = false;

		// System.out.println( input_line );

		String data_line = input_line.trim();
		
		if( data_line.startsWith( "[" ))
		{
		    // entry for a new plugin
		    
		    pd = new PluginData();
		    pd.short_desc_v = new Vector();

		    pd.name = data_line.substring(1, data_line.length()-1);
		    
		    data_v.addElement( pd );

		    good_line = true;
		}
		
		
		if( data_line.startsWith( "type:" ))
		{
		    pd.type = data_line.substring(5).trim();
		    good_line = true;
		}
		
		if( data_line.startsWith( "short_desc:" ))
		{
		    pd.short_desc_v.addElement(data_line.substring(11).trim());
		    good_line = true;
		}
		
		if( data_line.startsWith( "author:" ))
		{
		    pd.author = data_line.substring(7).trim();
		    good_line = true;
		}
		
		if( data_line.startsWith( "version:" ))
		{
		    int[] vdat = parsePluginVersion( data_line.substring(8).trim() );

		    pd.version_major  = vdat[0];
		    pd.version_minor  = vdat[1];
		    pd.version_build  = vdat[2];

		    good_line = (vdat != null);
		}

		if( data_line.startsWith( "release_date:" ))
		{
		    pd.release_date = data_line.substring(13).trim();
		    good_line = true;
		}

		if( data_line.startsWith( "jar_url:" ))
		{
		    pd.jar_url = data_line.substring(8).trim();
		    good_line = true;
		}

		if( data_line.startsWith( "compatible_with:" ))
		{
		    pd.compatible_with = parseCompatibilityLine( data_line.substring(16).trim() );
		    good_line = (pd.compatible_with != null);
		}

		if(data_line.startsWith( "#" ))
		{
		    good_line = true;
		}
		if(data_line.trim().length() == 0)
		{
		    good_line = true;
		}


		if(!good_line)
		{
		    System.err.println("WARNING: unable to parse the following line:\n  " + data_line);
		}
	    }

	    in.close();

	    return (PluginData[]) data_v.toArray( new PluginData[0] );
	}
	catch(java.net.MalformedURLException murle)
	{
	    System.err.println("WARNING: getLatestInfo(): " + murle);
	}
	catch(java.io.IOException ioe)
	{
	    System.err.println("WARNING: getLatestInfo(): " + ioe);
	}
	return null;
    }



    // ==============================
    // plugin version number checking
    // ==============================

    // returns [maj,min,bld] in an int[3]
    //
    private int[] parsePluginVersion( String vstr )
    {
	try
	{
	    int[] result = new int[3];

	    int dot1 = vstr.indexOf('.');
	    
	    result[0] =  new Integer(vstr.substring(0,dot1)).intValue();

	    int dot2 = vstr.indexOf('.', dot1+1);

	    result[1] =  new Integer(vstr.substring(dot1+1,dot2)).intValue();
	    
	    result[2] =  new Integer(vstr.substring(dot2+1)).intValue();
	    
	    return result;
	}
	catch(Exception e)
	{
	    System.err.println("parsePluginVersion(): cannot parse: '" + vstr + "'");
	    return null;
	}
    }

    // true if 1 is newer than 2
    //
    private boolean isNewerPluginVersion( int maj1, int min1, int bld1,
					  int maj2, int min2, int bld2 )
    {
	if(maj1 < maj2)
	    return false;
	if(maj1 > maj2)
	    return true;

	// maj must be the same, check min....
	
	if(min1 < min2)
	    return false;
	if(min1 > min2)
	    return true;

	// min must be the same, check bld....

	if(bld1 > bld2)
	    return true;
	
	// bld must be same or lower....
	
	return false;
    }


    // ==============================
    // compatibility testing
    // ==============================

    private VerNum[] parseCompatibilityLine( String vnums )
    {
	// tokenise the line
	Vector result = new Vector();
	String[] toks = tokenise( vnums );

	for(int t=0; t < toks.length; t++)
	{
	    try
	    {
		VerNum vn = new VerNum();
		
		String vstr = toks[t];

		if(vstr.charAt(0) == '>')
		{
		    vn.great_equals = true;
		    vstr = vstr.substring(1);
		}
		if(vstr.charAt(0) == '<')
		{
		    vn.less_equals = true;
		    vstr = vstr.substring(1);
		}

		int dot1 = vstr.indexOf('.');
		
		vn.series = new Integer(vstr.substring(0,dot1)).intValue();
		
		int dot2 = vstr.indexOf('.', dot1+1);
		
		vn.major = new Integer(vstr.substring(dot1+1,dot2)).intValue();
	    
		int slash = vstr.indexOf('/', dot2+1);
		int d_end = (slash > 0) ? slash : vstr.length();
		
		vn.minor = new Integer(vstr.substring(dot2+1, d_end)).intValue();

		if(slash > 0)
		    vn.variant = vstr.substring(slash+1);
		else
		    vn.variant = "";

		result.addElement( vn );
		
	    }
	    catch(Exception e)
	    {
		System.err.println("parseCompatibilityLine(): cannot parse: '" + toks[t] + "'");
	    }
	    
	}

	return (VerNum[]) result.toArray( new VerNum[0] );
    }

    private class VerNum
    {
	int series, major, minor;
	String variant;

	boolean great_equals;
	boolean less_equals;

	public String toString() 
	{ 
	    String var_str = (variant.length() > 0) ? ("/" + variant) : "";

	    String prefix = "";
	    if(great_equals)
		prefix = ">:";
	    if(less_equals)
		prefix = "<:";

	    return prefix + series + "." +  major + "." +  minor + var_str;
	}
    } 

    private String[] tokenise( String str )
    {
	 Vector vec = new Vector();
	 StringTokenizer st = new StringTokenizer( str );
	 while (st.hasMoreTokens()) 
	 {
	     vec.addElement( st.nextToken() );
	 }
	 return (String[]) vec.toArray( new String[0] );
    }

    private boolean isCompatible( PluginData pdata )
    {
	// is it known to be compatible?

	boolean compatible = false;
	
	VerNum this_version = new VerNum();
	this_version.series  = mview.version_series;
	this_version.major   = mview.version_major;
	this_version.minor   = mview.version_minor;
	this_version.variant = mview.version_prerelease_variant;

	//System.out.println("checking compatibility for '" + pdata.name + "':");

	for(int c=0; c < pdata.compatible_with.length; c++)
	{
	    //System.out.println("  testing:" + pdata.compatible_with[c]);
	    
	    if( isCompatible( this_version, pdata.compatible_with[c] ))
	    {
		//System.out.println("  OK!");
		return true;
	    }
	}
	//System.out.println("  not compatible!");
	return false;
    }

    private boolean isCompatible( VerNum mv_version, VerNum vnum )
    {
	if((!vnum.great_equals) && (!vnum.less_equals))
	{
	    return (compare(mv_version, vnum) == 0);
	}
	
	if(vnum.great_equals)
	{
	    return (compare(mv_version, vnum) >= 0);
	}

	if(vnum.less_equals)
	{
	    return (compare(mv_version, vnum) <= 0);
	}

	return false;
    }

    // 0==same, -1==v1 older than v2, +1==v1 newer than v2
    //
    private int compare( VerNum v1, VerNum v2 ) 
    {
	if ((v1.series == v2.series) &&
	    (v1.major == v2.major) &&
	    (v1.minor == v2.minor) &&
	    (v1.variant.equals(v2.variant)))
	    return 0;
	
	if (v1.series < v2.series)
	    return -1;
	if (v1.series > v2.series)
	    return +1;

	if (v1.major < v2.major)
	    return -1;
	if (v1.major > v2.major)
	    return +1;

	if (v1.minor < v2.minor)
	    return -1;
	if (v1.minor > v2.minor)
	    return +1;

	if( v1.variant.length() == 0 )  // the 'empty' variant is newer than any variant
	{
	    if( v2.variant.length() == 0 )
		return 0;
	    else
		return +1;   // empty varient is newer than any other variant
	}
	else
	{
	    if( v2.variant.length() == 0 )
	    {
		return -1;   // v1 must be older than v2
	    }
	    else
	    {
		// now check both variants:
		
		// first the inital 'letter'  (which is 'a' for alpha versions and 'b' for beta)
		
		char v1_code_letter = v1.variant.charAt(0);
		char v2_code_letter = v2.variant.charAt(0);
		
		if(v1_code_letter > v2_code_letter)    
		    return +1;
		
		if(v1_code_letter < v2_code_letter) 
		    return -1;
		
		// now it's down to the digits...
		try
		{
		    int v1_code_digit =  (new Integer(v1.variant.substring(1))).intValue();
		    int v2_code_digit =  (new Integer(v2.variant.substring(1))).intValue();
		    
		    if(v1_code_digit > v2_code_digit)
			return +1;
		    if(v1_code_digit < v2_code_digit)
			return -1;

		    return 0;
		}
		catch(NumberFormatException nfe)
		{
		    return 0;
		}
	    }
	}
    }

    // ==============================
    // installation routines
    // ==============================

    private boolean abort_download = false;

    private void startInstallerThread( Vector pdata_v )
    {
	new PluginInstallerThread( pdata_v ).start();
    }

    private class PluginInstallerThread extends Thread
    {
	public PluginInstallerThread( Vector pdata_v_ )
	{
	    pdata_v = pdata_v_;
	}
	public void run()
	{
	    frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

	    pm = new ProgressOMeter("Installing plugin" + ((pdata_v.size() > 1) ? "s" : ""), 3);

	    pm.setCancelAction( new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			abort_download = true;
		    }
		});
	    
	    pm.startIt();

	    doInstall( pdata_v );
	}

	private Vector pdata_v;
    }
    
    private void doInstall( Vector pdata_v )
    {
	int success = 0;
	int fail    = 0;


	String success_report = "";
	String fail_report = "";

	String fail_details = "";

	for(int p=0; p < pdata_v.size(); p++ )
	{
	    if(!abort_download)
	    {
		PluginData pdata = (PluginData) pdata_v.elementAt(p);
		
		pm.setMessage( 0, "Plugin: " + pdata.name);
		pm.setMessage( 1, "Downloading JAR..." );
		pm.setMessage( 2, "[ connecting ]" );

		try
		{
		    // System.out.println("getting jar for " + pdata.name + "...");
		    
		    final String tmp_file_name = mview.getTemporaryDirectory() + File.separatorChar + "tmp.jar";

		    System.out.println("downloading Jar to " + tmp_file_name + "...");
		    
		    int size  = downloadToFile( pdata.jar_url, tmp_file_name );
		    
		    //System.out.println("  ....done (" + size + " bytes)");
		    
		    //System.out.println("unpacking jar...");
		    
		    if( !abort_download )
		    {
			int nfiles = unpackJAR( tmp_file_name );
			
			pm.setMessage( 1, "Unpacking JAR..." );
			pm.setMessage( 2, "" );

			//System.out.println("  ...done (" + nfiles + " files)");
			
			removeFile( tmp_file_name );
			
			success++;
			
			if(success_report.length() > 0)
			    success_report += "\n";
			
			success_report += ("\"" + pdata.name + "\" downloaded & unpacked.");
		    }
		}
		catch(Exception ex)
		{
		    fail++;
		    
		    if(fail_report.length() > 0)
			fail_report += "\n";
		    
		    fail_report += ("\"" + pdata.name + "\" not installed.");
		    
		    if(fail_details.length() > 0)
			fail_details += "\n";
		    
		    fail_details +=  pdata.name + ":\n  " + ex;

		    //System.err.println("A problem was encountered whilst installing '" + pdata.name + "'\n\n" + ex);
		}
	    }
	}

		
	/*
	String success_report = 
	    ((success > 1) ? (success + " plugins were") : "One plugin was") +
	    " successfully installed or updated";

	String fail_report = 
	    ((fail > 1) ? (fail + " plugins") : "One plugin") +
	    " could not be installed or updated";
	*/

	String report = "";

	//System.out.println("good=" + success + " bad=" + fail);

	if(success > 0)
	{
	    report += success_report;

	    if(fail > 0)
		report += "\n";
	}

	if(fail > 0)
	{
	    report += fail_report;
	}

	pm.stopIt();
	frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	    
	if(abort_download)
	{
	    mview.infoMessage("Installation was aborted");
	}
	else
	{
	    if( fail > 0 )
	    {
		if(mview.infoQuestion( report, "More details", "OK" ) == 0 )
		{
		    mview.infoMessage( fail_details );
		}
	    }
	    else
	    {
		mview.infoMessage( report + "\n\nThe plugin directory will now be re-scanned" );

		startPluginScan();
	    }
	}
    }

 
    private void removeFile( String name  ) throws Exception
    {
	File file = new File( name );
	
	if(file.exists())
	    file.delete();
    }

    private int downloadToFile( String url_str, String file ) throws Exception
    {
	
	long start_time = ( new java.util.Date() ).getTime();
	
	java.util.Date last_change = new java.util.Date();

	int tick = 0;
	
	int bytes = 0;
	
	// System.out.println("expecting " + latest_size_i + " bytes from " + new_jar_url);

	BufferedInputStream bis;
	
	if( url_str.startsWith("http") )
	{
	    java.net.URL in_url = new java.net.URL( url_str );
	    bis = new BufferedInputStream( in_url.openStream() );
	}
	else
	{
	    File in_file = new File( url_str.substring(6) );
	    bis = new BufferedInputStream( new FileInputStream(in_file) );
	}
	
	BufferedOutputStream bos = new BufferedOutputStream( new FileOutputStream( file ));
		    
	boolean done = false;

	final int read_rate = 8096;
	byte[] data_buf = new byte[ read_rate ];
	
	while(!done && !abort_download)
	{
	    int res = bis.read( data_buf, 0, read_rate );
	    
	    // System.out.println("got " + res);
	    
	    if(res == -1)
	    {
		done = true;
	    }
	    else
	    {
		bos.write( data_buf, 0, res );
		bytes += res;
	    }

	    int k_bytes = (int)((double) bytes / 1024.0);

	    pm.setMessage( 2, "[ received " + k_bytes + " Kbytes ]" );
	}
		    
	bis.close();
	bos.close();

	return bytes;
    }

    private int unpackJAR( String file_name ) throws Exception
    {
	int count = 0;
	int bad_files = 0;
	
	JarFile jarfile = new JarFile( file_name );
	
	// first pass, count files and create directories
	
	for (Enumeration e = jarfile.entries() ; e.hasMoreElements() ;) 
	{
	    ZipEntry ze = (ZipEntry) e.nextElement();
	    count++;
	    
	    final int nbytes = (int) ze.getSize();
	    if(nbytes == 0)
	    {
		// it's a directory
		
		File dir = new File( ze.getName() );

		if(!dir.exists())
		{
		    System.out.println("creating directory " + dir.getPath());
		    dir.mkdirs();
		}
	    }
	}
	
	System.out.println("jar file has " + count + " entries");
		    
	int bad_writes = 0;
	int total_size = 0;
	
	final double total_d = (double) (count-1);
	final int total_files = count;
	count = 0;
	
	for (Enumeration e = jarfile.entries() ; e.hasMoreElements() ;) 
	{
	    if(!abort_download)
	    {
		int progress_pc = (int)(((double) (count++) / total_d) * 100.0);
			
		pm.setMessage( 3, progress_pc + " %" );

		ZipEntry ze = (ZipEntry) e.nextElement();
		
		BufferedInputStream bis = new BufferedInputStream( jarfile.getInputStream( ze ));
		
		final int nbytes = (int) ze.getSize();
		
		total_size += nbytes;
		
		if(nbytes > 0)
		{
		    byte data_bytes[] = new byte[ nbytes ];
				
		    int len = bis.read(data_bytes, 0, nbytes);
				
		    if(len != nbytes)
			System.err.println("unpackJAR(): BAD FILE READ, only got " + len + " of " + nbytes + " bytes");
		    
		    final String dest_file_name = mview.getUserSpecificDirectory() + File.separatorChar + ze.getName();

		    System.out.println( "writing to " + dest_file_name );

		    File file = new File( dest_file_name );
		    
		    File path = file.getParentFile();

		    if( path != null )
		    {
			// System.out.println("creating directory " + path.getPath());
			path.mkdirs();
		    }

		    BufferedOutputStream bos = new BufferedOutputStream( new FileOutputStream( file ));
		    
		    bos.write( data_bytes, 0, nbytes );
		    
		    bos.close();
		}
	    }
	}

	return count;
    }


    // --------------------------------------------------------------------------------------
    // --------------------------------------------------------------------------------------

    final private Boolean const_true  = new Boolean(true);

    private maxdView mview;
    
    // the complete list of all menus and all plugins (even the disabled ones)
    private DefaultMutableTreeNode master_root;

    // a reference to the complete (but unstructured by menus) list of plugins generated by maxdView
    private Vector                 plugin_info;


    // current active state
    private HashSet                disabled_plugin_hs;
    private DefaultMutableTreeNode menu_root;


    // temporary state of GUI (before the 'apply' button is pressed)
    private HashSet                temp_disabled_plugin_hs;
    private DefaultMutableTreeNode temp_menu_root;


    // gui elements
    private JTabbedPane tabbed;
    private JButton update_button, scan_button, add_button, delete_button, reset_button, rename_button;

    private DraggableTree menu_tree;

    private JFrame frame;
    private JTable plugin_table;

    private ProgressOMeter pm;
    
}
