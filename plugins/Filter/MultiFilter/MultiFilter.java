import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.Vector;

import java.io.*;

public class MultiFilter implements Plugin, ExprData.Filter, ExprData.ExprDataObserver
{
    public MultiFilter(maxdView mview_)
    {
	mview = mview_;
	edata = mview.getExprData();
	anlo = mview.getAnnotationLoader();

	//filter_mode = mview.getIntProperty("MultiFilter.mode", NoFilter);
    }

    public void cleanUp()
    {
	edata.removeObserver(this);
	edata.removeFilter(this);

	if(frame != null)
	{
	    //mview.putIntProperty("MultiFilter.mode", filter_mode);
	    frame.setVisible(false);
	}

	// shut any open editors...

	closeEditors(root_node);

	writeRule( mview.getConfigDirectory() + "multi-filter.rule" );
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
	root_node = new Leaf(0);
	
	buildPopups();
	getAllDataOperands();
	addComponents();

	frame.pack();
	frame.setVisible(true);

	edata.addFilter(this);
	edata.addObserver(this);

	readRule( mview.getConfigDirectory() + "multi-filter.rule", false );
    }

    public void stopPlugin()
    {
	cleanUp();
    }
  
    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("Multi Filter", "filter", 
					 "Filter rules for multiple Measurments", "", 
					 1, 0, 0);
	return pinf;
    }
    public PluginCommand[] getPluginCommands()
    {
	PluginCommand[] com = new PluginCommand[2];
	
	com[0] = new PluginCommand("start", null);
	com[1] = new PluginCommand("stop", null);
	
	return com;
    }

    public void  runCommand(String name, String[] args, CommandSignal done) 
    { 
	if(name.equals("start"))
	{
	    startPlugin();
	}

	if(name.equals("stop"))
	    cleanUp();

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
    }

    public void measurementUpdate(ExprData.MeasurementUpdateEvent mue)
    {
	switch(mue.event)
	{
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	case ExprData.OrderChanged:
	    if(root_node != null)
	    {
		closeEditors(root_node);
		removeEditors(root_node);
	    }

	}
    }
    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  filter implementation
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
	
    // should return TRUE  if this spot is not to be displayed
    //           and FALSE if the spot is to be displayed
    //
    public boolean filter(int spot_id)
    {
	if( type_mismatch_problem == true )
	    return false;

	if(root_node != null)
	{
	    // System.out.println( root_node.toString( spot_id ) );

	    try
	    {
		return ! root_node.filter(spot_id);
	    }
	    catch( TypeMismatch tm )
	    {
		// System.out.println("mismatch! (si=" + spot_id);
		
		if( type_mismatch_problem == false )
		{
		    type_mismatch_problem = true;
		    first_type_mismatch = tm;
		    System.out.println( tm.getMessage() );
		    rule_panel.repaint();
		}

		return false;
	    }

	    catch( ArrayIndexOutOfBoundsException aioobe )
	    {
		// almost certainly caused by a data update happening (spots or measurements
		// being added or removed) during the filtering test
		
		// not really a problem, things will sort themselves out as the event
		// is propagated

		return false;
	    }

	}
	else
	    return false;
    }

    public boolean enabled()
    { 
	return (root_node != null);
    }

    public String  getName() { return "MultiFilter"; }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  gooey
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    Color dark_green_col = new Color(0,255-32-16,0);
    Color dark_red_col   = new Color(255-32-16,0,0);
    Color red_shade_col  = new Color(255-32-16,64,64);
    private void addComponents()
    {
	frame = new JFrame("Multi Filter");
	
	mview.decorateFrame(frame);
	    
	frame.addWindowListener(new WindowAdapter() 
			  {
			      public void windowClosing(WindowEvent e)
			      {
				  cleanUp();
			      }
			  });

	GridBagLayout gridbag = new GridBagLayout();
	JPanel panel = new JPanel();
	panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	panel.setLayout(gridbag);
	//panel.setPreferredSize(new Dimension(350, 200));

	int line = 0;

	{
	    rule_panel = new RulePanel();

	    rule_panel.setPreferredSize(new Dimension(350, 300));

	    rule_panel_jsp = new JScrollPane(rule_panel);

	    panel.add(rule_panel_jsp);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    c.weighty = c.weightx = 9.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(rule_panel_jsp, c);
	}
	
	{
	    JPanel wrapper = new JPanel();
	    wrapper.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrapper.setLayout(w_gridbag);

	    {
		JButton button = new JButton("Load");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    readRule( );
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		w_gridbag.setConstraints(button, c);
	    }
	    {
		JButton button = new JButton("Save");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    writeRule( );
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		w_gridbag.setConstraints(button, c);
	    }
	    {
		Dimension fillsize = new Dimension(16,16);
		Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 2;
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
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 3;
		//c.weighty = c.weightx = 1.0;
		//c.anchor = GridBagConstraints.EAST;
		w_gridbag.setConstraints(button, c);
	    }

	    {
		JButton button = new JButton("Help");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    mview.getPluginHelpTopic("MultiFilter", "MultiFilter");
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 4;
		//c.weighty = c.weightx = 1.0;
		w_gridbag.setConstraints(button, c);
	    }

	    panel.add(wrapper);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.weightx = 1.0;
	    //c.anchor = GridBagConstraints.SOUTH;
	    gridbag.setConstraints(wrapper, c);
	    
	}

	frame.getContentPane().add(panel);
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  command handlers
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    /*
    private void setFilterMode(int new_mode)
    {
	filter_mode = new_mode;
	edata.notifyFilterChanged((ExprData.Filter) MultiFilter.this);
    }
    */

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  rule panel
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    final int node_h = 30;
    final int node_w = 100;

    final int leaf_h = 50;
    final int leaf_w = 180;

    final int operator_h = 20;
    final int operator_w = 60;

    final int operator_gap = 12;

    final int depth_offset = 12;
 
    final int border_gap = 6;

    private class RulePanel extends JPanel implements MouseListener
    {
	
	public RulePanel()
	{
	    addMouseListener(this);
	}

	public void paintComponent(Graphics g)
	{
	    // System.out.println("repaint!");
	    
	    fm = g.getFontMetrics();

	    int text_height = fm.getAscent();

	    g.setColor(Color.white);
	    g.fillRect(0,0, getWidth(), getHeight());

	    final int cur_w = getWidth();
	    final int cur_h = getHeight();
	    
	    int message_offset = 0;

	    if( type_mismatch_problem  )
	    {
		g.setColor( Color.red );
		g.drawString( first_type_mismatch.getMessage(), 4, 2 * text_height );
		message_offset = 3 * text_height;
	    }

	    if(root_node != null)
	    {
		Dimension rule_size = root_node.paint( g, 10, 10 + message_offset );

		// add some space for the border
		Dimension total_size = new Dimension( (int) rule_size.getWidth() + 20, 
						      (int) rule_size.getHeight() + 20 );
		
		if((cur_w != total_size.getWidth()) || (cur_h != total_size.getHeight()))
		{
		    // System.out.println("rule size has changed (" + total_size + ") resize the panel");
		    
		    setPreferredSize( total_size );
		    setMinimumSize( total_size );
		    rule_panel_jsp.revalidate();

		}

	    }
	    
	    
	}

	public void mousePressed(MouseEvent e) 
	{
	    if(!maybeShowPopup(e))
	    {
		root_node.unselect();
		Node selnode = root_node.findSelected();
	    }
	    /*
	    if(!maybeShowPopup(e))
	    {
		// locate which thing the click happened in...
		
	    }
	    */
	}
	
	public void mouseReleased(MouseEvent e) 
	{
	    maybeShowPopup(e);
	}
	
	public void mouseClicked(MouseEvent e) 
	{
	    if(!maybeShowPopup(e))
	    {
		root_node.unselect();
		root_node.handleClick(e);
	    }

	    /*
	    if(!maybeShowPopup(e))
	    {
		// locate which thing the click happened in...
		//root_node.unselect();
		//root_node.handleClick(e);
		
		Node selnode = root_node.findSelected();
		
		if(selnode != null)
		{
		    if(selnode.isBranch())
			System.out.println("branch selected");
		    //else
		    {
			
		    }
		}
	    }
	    */
	}

	private boolean maybeShowPopup(MouseEvent e) 
	{
	    if(e.isPopupTrigger() || e.isControlDown()) 
	    {
		root_node.unselect();
		root_node.handleClick(e);

		Node selnode = root_node.findSelected();
		
		if(selnode != null)
		{
		    //if(selnode.isLeaf())
		    leaf_popup.show(e.getComponent(), e.getX(), e.getY());
		}
		return true;
	    }
	    else
	    {
		return false;
	    }
	}

	
	public void mouseEntered(MouseEvent e) 
	{
	}

	public void mouseExited(MouseEvent e) 
	{
	}

	
    }

    JPopupMenu branch_popup, leaf_popup;

    public void buildPopups()
    {
	leaf_popup = new JPopupMenu();

	JMenuItem mi = new JMenuItem("Edit...");
	mi.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    editSelectedNode();
		}
	    });
	leaf_popup.add(mi);
	
	leaf_popup.addSeparator();

	mi = new JMenuItem("Expand backward");
	mi.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    expandSelectedNode(false);
		}
	    });
	leaf_popup.add(mi);
	
	mi = new JMenuItem("Expand forward");
	mi.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    expandSelectedNode(true);
		}
	    });
	leaf_popup.add(mi);

	leaf_popup.addSeparator();

	mi = new JMenuItem("Remove");
	mi.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    removeSelectedNode();
		}
	    });
	leaf_popup.add(mi);

	/*
	leaf_popup.addSeparator();

	JMenu type_popup = new JMenu("Type");
	leaf_popup.add(type_popup);

	mi = new JMenuItem("Values");
	mi.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		}
	    });
	type_popup.add(mi);
	mi = new JMenuItem("Names");
	mi.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		}
	    });
	type_popup.add(mi);
	mi = new JMenuItem("Clusters");
	mi.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		}
	    });
	type_popup.add(mi);
	*/
    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  rule tree
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  


    private void expandSelectedNode(boolean backward)
    {
	Node selnode = root_node.findSelected();

	if(selnode != null)
	{
	    Branch pnode = (Branch) root_node.getParentOf(selnode);
	    if(pnode != null)
	    {
		boolean left = (pnode.left == selnode);
		
		// replace either the left or right node with a new branch
		Node oldn = left ? pnode.left : pnode.right;
		Branch newb = backward ? new Branch( 0, new Leaf(0), oldn ) : new Branch( 0, oldn, new Leaf(0) );
		
		if(left)
		    pnode.left = newb;
		else
		    pnode.right = newb;
	    }
	    else
	    {
		// this leaf was the root node
		if(backward)
		    root_node =  new Branch( 0, selnode, new Leaf(0) );
		else
		    root_node =  new Branch( 0, new Leaf(0), selnode );
	    }
	    
	    rule_panel.repaint();
	}
    }

    private void editSelectedNode()
    {
	Node selnode = root_node.findSelected();

	if(selnode != null)
	{
	    if(selnode instanceof Leaf)
		((Leaf)selnode).edit();
	}
    }

    private void removeSelectedNode()
    {
	Node selnode = root_node.findSelected();

	if(selnode != null)
	{
	    Branch pnode = (Branch) root_node.getParentOf(selnode);
	    if(pnode != null)
	    {
		// find the parent-of-the-parent
		Branch ppnode = (Branch) root_node.getParentOf(pnode);
		
		closeEditors(selnode);
		
		if(ppnode != null)
		{
		    boolean pleft = (ppnode.left == pnode);
		    boolean left  = (pnode.left == selnode);
		    
		    // replace the parent-of-the-parent with either the left or right node
		    
		    if(pleft)
			ppnode.left  = left ? pnode.right : pnode.left;
		    else
			ppnode.right = left ? pnode.right : pnode.left;
		}
		else
		{
		    // this is leaf's parental branch is the root

		    boolean pleft = (pnode.left == selnode);
		    
		    root_node = pleft ? pnode.right : pnode.left;
		}
		rule_panel.repaint();
	    }
	    else
	    {
		// this node hs no parent so should not be deleted...
	    }
	}
    }
    
    private void updateNode(final Node node)
    {
	frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		    
	System.gc();
	rule_panel.repaint();
	edata.notifyFilterChanged((ExprData.Filter) MultiFilter.this);

	frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

	doCount();
    }

    public void closeEditors(final Node node)
    {
	if(node.isLeaf())
	{
	    JFrame editor = ((Leaf)node).editor;
	    if((editor != null) && (editor.isVisible()))
		editor.setVisible(false);
	}
	else
	{
	    closeEditors(((Branch) node).left);
	    closeEditors(((Branch) node).right);
	}
    }

    public void removeEditors(final Node node)
    {
	if(node.isLeaf())
	{
	    ((Leaf)node).editor = null;
	}
	else
	{
	    removeEditors(((Branch) node).left);
	    removeEditors(((Branch) node).right);
	}
    }

    // -----------------------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------------------

    private class TypeMismatch extends Throwable
    {
	public TypeMismatch( String s ) { super( s ); }
    }

    private class Operand
    {
	public String name;
	public int index;     // used for MeasOps and SpotAttrOps
	public int type;
	
	public int    getInt(int spot_id)    throws TypeMismatch  { return 0; }
	public double getDouble(int spot_id) throws TypeMismatch  { return .0; }
	public char   getChar(int spot_id)   throws TypeMismatch  { return 0; }
	public String getText(int spot_id)   throws TypeMismatch  { return null; }

	public boolean lessThan(int spot_id, Operand op)    throws TypeMismatch { return false; }
	public boolean greaterThan(int spot_id, Operand op) throws TypeMismatch { return false; }
	public boolean lessEquals(int spot_id, Operand op)    throws TypeMismatch { return false; }
	public boolean greaterEquals(int spot_id, Operand op) throws TypeMismatch { return false; }
	public boolean equals(int spot_id, Operand op)      throws TypeMismatch { return false; }
	public boolean notEquals(int spot_id, Operand op)   throws TypeMismatch { return false; }

	public String toString(int spot_id)
	{ 
	    try { return ((name == null) ? "const:" : (name + ":")) + getText(spot_id); }
	    catch(TypeMismatch tm) { return "TypeMismatch"; }

	}
    }

    public final int IntOperand    = 0;
    public final int DoubleOperand = 1;
    public final int CharOperand   = 2;
    public final int TextOperand   = 3;



    // -----------------------------------------------------------------------------------------------

    private class MeasOperand extends Operand
    {
	public int meas_id;
	public double[] data_v;

	public final int    getInt(int spot_id)    throws TypeMismatch 
	{ 
	    if( Double.isNaN( data_v[spot_id] ) )
		throw new TypeMismatch( "Cannot convert 'NaN' to INTEGER value (in Measurement " + edata.getMeasurementName( meas_id ) + ")" );
	    return (int) data_v[spot_id];
	    
	}
	public final double getDouble(int spot_id) throws TypeMismatch 
	{ 
	    return data_v[spot_id]; 
	}
	public final char   getChar(int spot_id)   throws TypeMismatch 
	{ 
	    throw new TypeMismatch( "Cannot convert numerical value to CHARACTER value (in Measurement " + edata.getMeasurementName( meas_id ) + ")" );
	}
	public final String getText(int spot_id)   throws TypeMismatch 
	{ 
	    if( Double.isNaN( data_v[spot_id] ) )
		return "NaN";
	    else
		return String.valueOf( data_v[spot_id] ); 
	}

	public final boolean lessThan(int spot_id, Operand op) throws TypeMismatch 
	{ 
	    return DoubleCompare.doubleLessThan( data_v[spot_id], op.getDouble(spot_id) );
	}
	public final boolean greaterThan(int spot_id, Operand op) throws TypeMismatch 
	{ 
	    return DoubleCompare.doubleGreaterThan( data_v[spot_id], op.getDouble(spot_id) );
	}
	public final boolean lessEquals(int spot_id, Operand op) throws TypeMismatch 
	{ 
	    return DoubleCompare.doubleLessEquals( data_v[spot_id], op.getDouble(spot_id) );
	}
	public final boolean greaterEquals(int spot_id, Operand op) throws TypeMismatch 
	{ 
	    return DoubleCompare.doubleGreaterEquals( data_v[spot_id], op.getDouble(spot_id) );
	}
	public final boolean equals( int spot_id, Operand op)  throws TypeMismatch 
	{ 
	    return DoubleCompare.doubleEquals( data_v[spot_id], op.getDouble(spot_id) );
	}
	public final boolean notEquals(int spot_id, Operand op)  throws TypeMismatch 
	{ 
	    return DoubleCompare.doubleNotEquals( data_v[spot_id], op.getDouble(spot_id) );
	}

	public MeasOperand(String name_, int meas_id_) 
	{ 
	    name = name_; 
	    meas_id = meas_id_; 
	    type = DoubleOperand;
	    data_v = edata.getMeasurementData(meas_id);
	}
    }

    private class SpotAttrOperand extends Operand
    {
	public int meas_id;
	public int sa_id;
    }

    private class SpotAttrIntOperand extends SpotAttrOperand
    {
	public int[] data_v;

	public final int    getInt(int spot_id)    throws TypeMismatch 
	{ 
	    return data_v[spot_id];
	}
	public final double getDouble(int spot_id) throws TypeMismatch
	{ 
	    return (double) data_v[spot_id]; 
	}
	public final char   getChar(int spot_id)   throws TypeMismatch 
	{ 
	    throw new TypeMismatch( "Cannot convert INTEGER value to a CHARACTER value" + 
				    " in " + edata.getMeasurementName( meas_id ) + "." + edata.getMeasurement(meas_id ).getSpotAttributeName( sa_id  ) );
	}
	public final String getText(int spot_id)   throws TypeMismatch
	{ 
	    return String.valueOf( data_v[spot_id] ); 
	}

	public final boolean lessThan(int spot_id, Operand op)    throws TypeMismatch 
	{ return data_v[spot_id] < op.getInt(spot_id); }
	public final boolean greaterThan(int spot_id, Operand op) throws TypeMismatch 
	{ return data_v[spot_id] > op.getInt(spot_id); }
	public final boolean lessEquals(int spot_id, Operand op)    throws TypeMismatch 
	{ return data_v[spot_id] <= op.getInt(spot_id); }
	public final boolean greaterEquals(int spot_id, Operand op) throws TypeMismatch 
	{ return data_v[spot_id] >= op.getInt(spot_id); }
	public final boolean equals(int spot_id, Operand op)      throws TypeMismatch 
	{ return data_v[spot_id] == op.getInt(spot_id); }
	public final boolean notEquals(int spot_id, Operand op)   throws TypeMismatch 
	{ return data_v[spot_id] != op.getInt(spot_id); }

	public SpotAttrIntOperand(String name_, int meas_id_, int sa_id_) 
	{ 
	    name= name_; 
	    meas_id = meas_id_; 
	    sa_id = sa_id_;
	    type = IntOperand;
	    data_v = (int[]) (edata.getMeasurement(meas_id).getSpotAttributeData(sa_id));
	}
    }
     
    private class SpotAttrDoubleOperand extends SpotAttrOperand
    {
	public double[] data_v;

	public final int    getInt(int spot_id)    throws TypeMismatch 
	{ 
	    if( Double.isNaN( data_v[spot_id] ) )
		throw new TypeMismatch("Cannot convert 'NaN' value to an INTEGER value" + 
				    " in " + edata.getMeasurementName( meas_id ) + "." + edata.getMeasurement( meas_id ).getSpotAttributeName( sa_id ) );
	    return (int) data_v[spot_id]; 
	}

	public final double getDouble(int spot_id) throws TypeMismatch { return data_v[spot_id]; }

	public final char   getChar(int spot_id)   throws TypeMismatch 
	{ 
	    throw new TypeMismatch("Cannot convert DOUBLE value to a CHARACTER value" + 
				    " in " + edata.getMeasurementName( meas_id ) + "." + edata.getMeasurement( meas_id ).getSpotAttributeName( sa_id )); 
	}

	public final String getText(int spot_id)   throws TypeMismatch 
	{ 
	    if( Double.isNaN( data_v[spot_id] ) )
		return "NaN";
	    else
		return String.valueOf(data_v[spot_id]); 
	}

	public final boolean lessThan(int spot_id, Operand op)    throws TypeMismatch 
	{ return DoubleCompare.doubleLessThan( data_v[spot_id], op.getDouble(spot_id) ); }
	public final boolean greaterThan(int spot_id, Operand op) throws TypeMismatch 
	{ return DoubleCompare.doubleGreaterThan( data_v[spot_id], op.getDouble(spot_id) ); }
	public final boolean lessEquals(int spot_id, Operand op)    throws TypeMismatch 
	{ return DoubleCompare.doubleLessEquals( data_v[spot_id], op.getDouble(spot_id) ); }
	public final boolean greaterEquals(int spot_id, Operand op) throws TypeMismatch 
	{ return DoubleCompare.doubleGreaterEquals( data_v[spot_id], op.getDouble(spot_id) ); }
	public final boolean equals(int spot_id, Operand op)      throws TypeMismatch 
	{ return DoubleCompare.doubleEquals( data_v[spot_id],  op.getDouble(spot_id) ); }
	public final boolean notEquals(int spot_id, Operand op)   throws TypeMismatch 
	{ return DoubleCompare.doubleNotEquals( data_v[spot_id], op.getDouble(spot_id) ); }

	public SpotAttrDoubleOperand(String name_, int meas_id_, int sa_id_) 
	{ 
	    name= name_; 
	    meas_id = meas_id_; 
	    sa_id = sa_id_;
	    type = DoubleOperand;
	    data_v = (double[]) (edata.getMeasurement(meas_id).getSpotAttributeData(sa_id));
	}
    }
     

    private class SpotAttrCharOperand extends SpotAttrOperand
    {
	public char[] data_v;

	public final int  getInt(int spot_id)  throws TypeMismatch 
	{ 
	    try
	    {
		Integer i = Integer.valueOf( String.valueOf( data_v[spot_id] ) );
		return i.intValue();
	    }
	    catch(NumberFormatException nfe)
	    {
		throw new TypeMismatch( "Cannot convert CHARACTER value to INTEGER value " + 
					"('" + data_v[spot_id] + "' in " + edata.getMeasurementName( meas_id ) + "." + 
					edata.getMeasurement( meas_id ).getSpotAttributeName( sa_id ) ); 
	    }
	}

	public final double getDouble(int spot_id) throws TypeMismatch 
	{ 
	    try
	    {
		Double d = Double.valueOf( String.valueOf( data_v[spot_id] ) );
		return d.doubleValue();
	    }
	    catch(NumberFormatException nfe)
	    {
		throw new TypeMismatch( "Cannot convert CHARACTER value to DOUBLE value " + 
					"('" + data_v[spot_id] + "' in " + edata.getMeasurementName( meas_id ) + "." + 
					edata.getMeasurement( meas_id ).getSpotAttributeName( sa_id ) ); 
	    }
	}

	public final char   getChar(int spot_id)   throws TypeMismatch { return data_v[spot_id]; }

	public final String getText(int spot_id)   throws TypeMismatch { return String.valueOf(data_v[spot_id]); }

	public final boolean lessThan(int spot_id, Operand op)    throws TypeMismatch { return data_v[spot_id] < op.getChar(spot_id); }
	public final boolean greaterThan(int spot_id, Operand op) throws TypeMismatch { return data_v[spot_id] > op.getChar(spot_id); }
	public final boolean lessEquals(int spot_id, Operand op)    throws TypeMismatch { return data_v[spot_id] <= op.getChar(spot_id); }
	public final boolean greaterEquals(int spot_id, Operand op) throws TypeMismatch { return data_v[spot_id] >= op.getChar(spot_id); }
	public final boolean equals(int spot_id, Operand op)      throws TypeMismatch { return data_v[spot_id] == op.getChar(spot_id); }
	public final boolean notEquals(int spot_id, Operand op)   throws TypeMismatch { return data_v[spot_id] != op.getChar(spot_id); }

	public SpotAttrCharOperand(String name_, int meas_id_, int sa_id_) 
	{ 
	    name= name_; 
	    meas_id = meas_id_; 
	    sa_id = sa_id_;
	    type = CharOperand;
	    data_v = (char[]) (edata.getMeasurement(meas_id).getSpotAttributeData(sa_id));
	}
    }

    private class SpotAttrTextOperand extends SpotAttrOperand
    {
	public String[] data_v;

	public final int  getInt(int spot_id)    throws TypeMismatch 
	{ 
	    try
	    {
		Integer i = Integer.valueOf( data_v[spot_id] );
		return i.intValue();
	    }
	    catch(NumberFormatException nfe)
	    {
		throw new TypeMismatch( "Cannot convert TEXT value to INTEGER value " + 
					"('" + data_v[spot_id] + "' in " + edata.getMeasurementName( meas_id ) + "." +
					edata.getMeasurement( meas_id ).getSpotAttributeName( sa_id ) ); 
	    }
	}

	public final double getDouble(int spot_id) throws TypeMismatch 
	{ 
	    try
	    {
		Double d = Double.valueOf( data_v[spot_id] );
		return d.doubleValue();
	    }
	    catch(NumberFormatException nfe)
	    {
		throw new TypeMismatch( "Cannot convert TEXT value to DOUBLE value " + 
					"('" + data_v[spot_id] + "' in " + edata.getMeasurementName( meas_id ) + "." + 
					edata.getMeasurement( meas_id ).getSpotAttributeName( sa_id ) ); 
	    }
	}
	
	public final char   getChar(int spot_id)   throws TypeMismatch 
	{ 
	    if( data_v[spot_id] == null )
		throw new TypeMismatch( "Cannot convert missing TEXT to CHARACTER value" + 
					" in " + edata.getMeasurementName( meas_id ) + "." + 
					edata.getMeasurement( meas_id ).getSpotAttributeName( sa_id ) ); 

	    if( data_v[spot_id].length() == 1 )
		return data_v[spot_id].charAt( 0 );

	    throw new TypeMismatch( "Cannot convert TEXT value to CHARACTER value " + 
				    "('" + data_v[spot_id] + "' in " + edata.getMeasurementName( meas_id ) + "." +  
				    edata.getMeasurement( meas_id ).getSpotAttributeName( sa_id ) ); 
	}
	
	public final String getText(int spot_id)   throws TypeMismatch 
	{ 
	    return data_v[spot_id]; 
	}
	
	public final boolean lessThan(int spot_id, Operand op)    throws TypeMismatch 
	{ return data_v[spot_id] == null ? true :  (data_v[spot_id].compareTo(op.getText(spot_id)) < 0); }

	public final boolean greaterThan(int spot_id, Operand op) throws TypeMismatch 
	{ return data_v[spot_id] == null ? true :  (data_v[spot_id].compareTo(op.getText(spot_id)) > 0); }

	public final boolean lessEquals(int spot_id, Operand op)    throws TypeMismatch 
	{ return data_v[spot_id] == null ? true :  (data_v[spot_id].compareTo(op.getText(spot_id)) <= 0); }

	public final boolean greaterEquals(int spot_id, Operand op) throws TypeMismatch 
	{ return data_v[spot_id] == null ? true :  (data_v[spot_id].compareTo(op.getText(spot_id)) >= 0); }

	public final boolean equals(int spot_id, Operand op)      throws TypeMismatch 
	{ 
	    if(data_v[spot_id] == null)
		return (op.getText(spot_id) == null);
	    else
		return data_v[spot_id].equals(op.getText(spot_id));
	}
	
	public final boolean notEquals(int spot_id, Operand op)   throws TypeMismatch 
	{ 
	    if(data_v[spot_id] == null)
		return (op.getText(spot_id) != null);
	    else
		return !(data_v[spot_id].equals(op.getText(spot_id)));
	}

	public SpotAttrTextOperand(String name_, int meas_id_, int sa_id_) 
	{ 
	    name= name_; 
	    meas_id = meas_id_; 
	    sa_id = sa_id_;
	    type = TextOperand;
	    data_v = (String[]) (edata.getMeasurement(meas_id).getSpotAttributeData(sa_id));
	}
    }

    // -----------------------------------------------------------------------------------------------
    
    private void getAllDataOperands()
    {
	data_operands = new Vector();

	for(int m=0; m < edata.getNumMeasurements(); m++)
	{
	    ExprData.Measurement meas = edata.getMeasurement(edata.getMeasurementAtIndex(m));

	    data_operands.addElement( new MeasOperand( meas.getName(), edata.getMeasurementAtIndex(m) ) );
	    
	    if(meas.getShow())
	    {
		for(int a = 0; a < meas.getNumSpotAttributes(); a++)
		{
		    final String aname = "  " + meas.getName() + "." + meas.getSpotAttributeName(a);

		    switch( meas.getSpotAttributeDataTypeCode( a ) )
		    {
		    case 0: // INTEGER
			data_operands.addElement( new SpotAttrIntOperand( aname, 
									  edata.getMeasurementAtIndex(m), 
									  a) );
			break;
		    case 1: // DOUBLE
			data_operands.addElement( new SpotAttrDoubleOperand(aname, 
									     edata.getMeasurementAtIndex(m), 
									     a) );
			break;
		    case 2: // CHAR
			data_operands.addElement( new SpotAttrCharOperand( aname, 
									   edata.getMeasurementAtIndex(m), 
									   a) );
			break;
		    case 3: // TEXT
			data_operands.addElement( new SpotAttrTextOperand( aname, 
									   edata.getMeasurementAtIndex(m), 
									   a) );
			break;
		    }
		}
	    }
	}

	data_operand_names = new String[ data_operands.size() ];
	
	for(int d=0; d < data_operands.size(); d++)
	{
	    data_operand_names[d] = ((Operand) data_operands.elementAt(d)).name;
	    ((Operand) data_operands.elementAt(d)).index = d;
	}
    }

   // -----------------------------------------------------------------------------------------------

    private class ConstIntOperand extends Operand
    {
	public int v;
	
	public final int    getInt(int spot_id)    throws TypeMismatch { return v; }
	public final double getDouble(int spot_id) throws TypeMismatch { return (double) v; }
	public final char   getChar(int spot_id)   throws TypeMismatch 
	{ 
	    if( v >= 0 && v <= 10 )
		return Character.forDigit( v, 10 );
	    else
		throw new TypeMismatch( "Cannot convert " + v + " to a CHARACTER" ); 
	}
	public final String getText(int spot_id)   throws TypeMismatch { return String.valueOf(v); }

	public final boolean lessThan(int spot_id, Operand op)    throws TypeMismatch 
	{ return v < op.getInt(spot_id); }
	public final boolean greaterThan(int spot_id, Operand op) throws TypeMismatch
	{ return v > op.getInt(spot_id); }
	public final boolean lessEquals(int spot_id, Operand op)    throws TypeMismatch 
	{ return v <= op.getInt(spot_id); }
	public final boolean greaterEquals(int spot_id, Operand op) throws TypeMismatch
	{ return v >= op.getInt(spot_id); }
	public final boolean equals(int spot_id, Operand op)      throws TypeMismatch 
	{ return v == op.getInt(spot_id); }
	public final boolean notEquals(int spot_id, Operand op)   throws TypeMismatch 
	{ return v != op.getInt(spot_id); }

	public ConstIntOperand(int i)  { v = i; type = IntOperand; name = String.valueOf(i); }
    }
    private class ConstDoubleOperand extends Operand
    {
	public double v;

	public final int    getInt(int spot_id)    throws TypeMismatch 
	{ 
	    if( Double.isNaN( v ) )
		throw new TypeMismatch("Cannot convert 'NaN' to an INTEGER value" );
	    return (int) v; 
	}
	public final double getDouble(int spot_id) throws TypeMismatch { return v; }
	public final char   getChar(int spot_id)   throws TypeMismatch 
	{ 
	    throw new TypeMismatch("Cannot convert DOUBLE value to a CHARACTER value" ); 
	}
	public final String getText(int spot_id)   throws TypeMismatch 
	{
	    if( Double.isNaN( v ) )
		return "NaN";
	    else
		return String.valueOf(v); 
	}

	public final boolean lessThan(int spot_id, Operand op)    throws TypeMismatch 
	{ 
	    return DoubleCompare.doubleLessThan( v , op.getDouble(spot_id) );
	}
	public final boolean greaterThan(int spot_id, Operand op) throws TypeMismatch 
	{ 
	    return DoubleCompare.doubleGreaterThan( v, op.getDouble(spot_id) );
	}
	public final boolean lessEquals(int spot_id, Operand op)    throws TypeMismatch 
	{ 
	    return DoubleCompare.doubleLessEquals( v , op.getDouble(spot_id) );
	}
	public final boolean greaterEquals(int spot_id, Operand op) throws TypeMismatch 
	{ 
	    return DoubleCompare.doubleGreaterEquals( v, op.getDouble(spot_id) );
	}
	public final boolean equals(int spot_id, Operand op)      throws TypeMismatch 
	{ 
	    return DoubleCompare.doubleEquals( v, op.getDouble(spot_id) );
	}
	public final boolean notEquals(int spot_id, Operand op)  throws TypeMismatch 
	{ 
	    return DoubleCompare.doubleNotEquals( v, op.getDouble(spot_id) );
	}

	public ConstDoubleOperand(double d) { v = d; type = DoubleOperand; name = String.valueOf(d); }
    }

    private class ConstTextOperand extends Operand
    {
	public String v;

	public final int    getInt(int spot_id)    throws TypeMismatch 
	{
	    try
	    {
		Integer i = Integer.valueOf( v );
		return i.intValue();
	    }
	    catch(NumberFormatException nfe)
	    {
		throw new TypeMismatch( "Cannot convert '" + v + "' to an INTEGER value"); 
	    }
	}

	public final double getDouble(int spot_id) throws TypeMismatch 
	{
	    if( v == null )
		throw new TypeMismatch( "Cannot convert missing value to a DOUBLE value"); 

	    if( v.equals("NaN" ) )
		return Double.NaN;

	    try
	    {
		Double d = Double.valueOf( v );
		return d.intValue();
	    }
	    catch(NumberFormatException nfe)
	    {
		throw new TypeMismatch( "Cannot convert '" + v + "' to a DOUBLE value"); 
	    }
	}
	
	public final char   getChar(int spot_id)   throws TypeMismatch 
	{ 
	    if( v == null )
		throw new TypeMismatch( "Cannot convert missing value to a CHARACTER"); 

	    if( v.length() == 1 )
		return v.charAt(0); 
	    
	    throw new TypeMismatch( "Cannot convert '" + v + "' to a CHARACTER value " );
	}
	public final String getText(int spot_id)   throws TypeMismatch { return v; }

	public final boolean lessThan(int spot_id, Operand op)    throws TypeMismatch { return (v.compareTo(op.getText(spot_id)) < 0); }
	public final boolean greaterThan(int spot_id, Operand op) throws TypeMismatch { return (v.compareTo(op.getText(spot_id)) > 0); }
	public final boolean lessEquals(int spot_id, Operand op)    throws TypeMismatch { return (v.compareTo(op.getText(spot_id)) <= 0); }
	public final boolean greaterEquals(int spot_id, Operand op) throws TypeMismatch { return (v.compareTo(op.getText(spot_id)) >= 0); }
	public final boolean equals(int spot_id, Operand op)      throws TypeMismatch { return v.equals(op.getText(spot_id)); }
	public final boolean notEquals(int spot_id, Operand op)   throws TypeMismatch { return !v.equals(op.getText(spot_id)); }

	public ConstTextOperand(String s) { v = s; type = TextOperand; name = s; }
    }

    private class ConstCharOperand extends Operand
    {
	public char v;

	public final int    getInt(int spot_id)    throws TypeMismatch 
	{ 
	    try
	    {
		Integer i = Integer.valueOf( String.valueOf( v ) );
		return i.intValue();
	    }
	    catch(NumberFormatException nfe)
	    {
		throw new TypeMismatch( "Cannot convert CHARACTER '" + v + "' to an INTEGER value" ); 
	    }
	}

	public final double getDouble(int spot_id) throws TypeMismatch
	{ 
	    try
	    {
		Double d = Double.valueOf( String.valueOf( v ) );
		return d.doubleValue();
	    }
	    catch(NumberFormatException nfe)
	    {
		throw new TypeMismatch( "Cannot convert CHARACTER '" + v + "' to a DOUBLE value" ); 
	    }
	}

	public final char   getChar(int spot_id)   throws TypeMismatch { return v; }
	public final String getText(int spot_id)   throws TypeMismatch { return String.valueOf(v); }

	public final boolean lessThan(int spot_id, Operand op)    throws TypeMismatch { return v < op.getChar(spot_id); }
	public final boolean greaterThan(int spot_id, Operand op) throws TypeMismatch { return v > op.getChar(spot_id); }
	public final boolean lessEquals(int spot_id, Operand op)    throws TypeMismatch { return v <= op.getChar(spot_id); }
	public final boolean greaterEquals(int spot_id, Operand op) throws TypeMismatch { return v >= op.getChar(spot_id); }
	public final boolean equals(int spot_id, Operand op)      throws TypeMismatch { return v == op.getChar(spot_id); }
	public final boolean notEquals(int spot_id, Operand op)   throws TypeMismatch { return v != op.getChar(spot_id); }

	public ConstCharOperand(char c)  { v = c; type = CharOperand; name = String.valueOf(c); }
    }

    // -----------------------------------------------------------------------------------------------

    public Operand makeOperand(String str_in)
    {
	String str = str_in.trim();

	// System.out.println( " makeOperand() : " + str );

	if(str.toLowerCase().equals("nan"))
	{
	    // System.out.println("that's a NaN that is...");

	    return new ConstDoubleOperand(Double.NaN);
	}
	if(str.toLowerCase().equals("infinity"))
	{
	    // System.out.println("that's a NaN that is...");

	    return new ConstDoubleOperand( Double.POSITIVE_INFINITY );
	}
	if(str.toLowerCase().equals("-infinity"))
	{
	    // System.out.println("that's a NaN that is...");

	    return new ConstDoubleOperand( Double.NEGATIVE_INFINITY );
	}
	
	try
	{
	    int i = (Integer.valueOf(str)).intValue();
	    // System.out.println("that's an integer...");
	    return new ConstIntOperand(i);
	}
	catch(NumberFormatException i_nfe)
	{
	    try
	    {
		double d = (Double.valueOf(str)).doubleValue();
		// System.out.println("that's an double...");
		return new ConstDoubleOperand(d);
	    }
	    catch(NumberFormatException d_nfe)
	    {
		if(str.length() == 1)
		{
		    // System.out.println("that's a char...");
		    return new ConstCharOperand(str.charAt(0));
		}
		else
		{
		    // System.out.println("that's some text...");
		    return new ConstTextOperand(str);
		}
	    }
	}
    }


    // -----------------------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------------------

    private final static String[] operands_str   = { "", "None", "Any", "At least", "At most", "All" };
    private final static String[] operator_str   = { ">", "<", ">=", "<=", "=", "!=" };
    private final static String[] leaf_modes_str = { "Value", "Name", "Cluster" };

    private Vector data_operands;  
    private String[] data_operand_names;


    // -----------------------------------------------------------------------------------
    // ----- node ------------------------------------------------------------------------
    // ----- node ------------------------------------------------------------------------
    // ----- node ------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------

    private class Node
    {
	public Node()
	{
	    border_rect = new Rectangle();
	    is_selected = false;
	}

	// should return TRUE  if this spot is not to be displayed
	//           and FALSE if the spot is to be displayed
	//
	public boolean filter( int spot_id ) throws TypeMismatch { return false; }

	public boolean is_selected;
	public Rectangle border_rect;

	public boolean isLeaf()   { return false; }
	public boolean isBranch() { return false; }

	public Node findSelected() { return null; }
	public void unselect() { is_selected = false;  }
	
	public Node getParentOf(Node n) { return null; }

	public boolean handleClick(MouseEvent me) { return false; }

	public Dimension paint(Graphics g, int xp, int yp) { return null; }

	public String toString(int spot_id) { return null; }

	public int counter;

    }


    // -----------------------------------------------------------------------------------
    // ----- branch ----------------------------------------------------------------------
    // ----- branch ----------------------------------------------------------------------
    // ----- branch ----------------------------------------------------------------------
    // -----------------------------------------------------------------------------------

    final static public String[] opnames = { "and", "or" /*, "nand", "xor" */ };

    private class Branch extends Node
    {
	public int operator;    // AND, OR

	public Rectangle operator_rect;
	public Rectangle border_rect;

	public Node left, right;

	final public boolean isBranch() { return true; }

	final private void init(int o)
	{
	    operator = o; 
	    operator_rect = new Rectangle();
	    border_rect = new Rectangle();
	}

	public Branch(int o, Node l, Node r)
	{
	    init(o); left = l; right = r;
	}

	public String toString(int spot_id)
	{
	    if(operator == 0)
		return (" ( " + left.toString(spot_id) + " and " + right.toString(spot_id) + " )");
	    else
		return (" ( " + left.toString(spot_id) + " or " + right.toString(spot_id) + " ) ");
	    
	}

	final public boolean filter(int spot_id) throws TypeMismatch
	{ 
	    if(operator == 0)
		return (left.filter(spot_id) && right.filter(spot_id)); 
	    else
		return (left.filter(spot_id) || right.filter(spot_id));
	}
	
	final public void unselect() 
	{ 
	    is_selected = false; 

	    // System.out.println("branch " + operator + " unselected");

	    left.unselect();
	    right.unselect();
	}
	
	final public Node findSelected() 
	{
	    Node result = (is_selected) ? this : null;
	    if(result == null)
		result = left.findSelected();
	    if(result == null)
		result = right.findSelected();
	    return result;
	}

	final public Node getParentOf(Node n) 
	{ 
	    Node result = null;

	    if( (n == left) || (n == right) )
		result = this; 
	    if(result == null)
		result = left.getParentOf(n);
	    if(result == null)
		result = right.getParentOf(n);
	    return result;
	}

	final public boolean handleClick(MouseEvent me)
	{
	    is_selected = false;

	    if(operator_rect.contains(me.getPoint()))
	    {
		// System.out.println("hit!");
		
		if(me.getClickCount() == 2)
		{
		    operator = (operator == 0) ? 1 : 0;
		    updateNode(Branch.this);
		    
		}

		return true;
	    }
	    else
	    {
		// System.out.println("miss!");

		boolean res = left.handleClick(me);

		if(!res) // no hit in lhs
		{
		    res = right.handleClick(me);
		}

		if(!res)
		{
		     if(border_rect.contains(me.getPoint()))
		     {
			 // select the branch itself
			
			is_selected = true;
			
			rule_panel.repaint();
			
			return true;
		    }
		}

		return res;
	    }
	}

	// returns size of Branch
	final public Dimension paint(Graphics g, int xp, int yp)
	{
	    g.setColor(Color.black);
	    
	    int h = 0;
	    Dimension lhs, rhs;
	    
	    yp += border_gap;
	    
	    // paint lhs...
	    
	    int pos_l = 0;
	    int pos_r = 0;

	    int w_l = 0;


	    pos_l =  xp;
	    
	    if(left != null)
	    {
		if(left.isBranch())
		{
		   pos_l += depth_offset;
		   lhs = left.paint(g, pos_l, yp);
		   w_l += depth_offset;
		}
		else
		{
		    lhs = left.paint(g, pos_l, yp);
		}
	    }
	    else
	    {
		lhs = new Dimension(0,0);
	    }

	    w_l += lhs.width;
	    
	    // the line linking the lhs with the operator...

	    //	    int link_l = xp + (lhs.width/2);
	    int link_l = xp + depth_offset + (lhs.width/2);

	    h += lhs.height;
	    //g.drawLine( link_l, yp + h, link_l, yp + h + operator_gap);
	    h += operator_gap;
	    
	    // paint the operator...
	    
	    int op_y = yp + h;
	    
	    //g.drawRect( xp, yp + h, operator_w, operator_h);
	    //g.drawString( String.valueOf(branch.operator), xp, yp + h + operator_h);
	    
	    h += operator_h + operator_gap;
	    
	    // paint rhs...
	    
	    int branch_off_r = 0;
	    int w_r = 0;

	    pos_r = xp;
	    if(right != null)
	    { 
		if(right.isBranch())
		{
		    pos_r += depth_offset;
		    rhs = right.paint(g, pos_r, yp + h);
		    //branch_off_r = border_gap;
		    w_r += depth_offset;
		}
		else
		{
		    rhs = right.paint(g, pos_r, yp + h);
		}
	    }
	    else
	    {
		rhs = new Dimension(0,0);
	    }

	    w_r += rhs.width;
	    
	    
	    xp = (rule_panel.getWidth() - operator_w) / 2;

	    // the line linking the rhs with the operator...
	    int link_r = xp + depth_offset + (rhs.width/2);
	    //g.drawLine( link_r, yp + h -  operator_gap, link_r, yp + h - branch_off_r);
	    
	    h += rhs.height;
	    
	    // now we know the x pos of the lhs and rhs, the operator can be
	    // drawn centrally between them...
	    
	    g.setColor(Color.red);
	    

	    
	    int link_m = rule_panel.getWidth() / 2;  // (link_l + link_r) / 2;
	    
	    // the 2 linking lines
	    g.drawLine( link_m, op_y, link_m, op_y - operator_gap );
	    g.drawLine( link_m, op_y + operator_h, link_m, op_y + operator_h + operator_gap );
	    
	    // and the centered box
	    link_m -= (operator_w / 2);
	    g.drawRect( link_m, op_y, operator_w, operator_h);
	    
	    // and some shading....
	    g.setColor( red_shade_col );

	    g.drawLine( link_m+operator_w+2, op_y+2, link_m+operator_w+2, op_y+operator_h+2);
	    g.drawLine( link_m+2, op_y+2+operator_h, link_m+operator_w+2, op_y+2+operator_h);

	    
	    operator_rect.x = link_m;
	    operator_rect.y = op_y;
	    operator_rect.width = operator_w;
	    operator_rect.height = operator_h;
	    
	    if(fm != null)
	    {
		String opname = (operator == 0) ? "and" : "or";
		
		int ow = fm.stringWidth(opname);
		int oh = fm.getAscent(); //+ fm.getDescent();
		
		g.setColor(Color.red);
		//g.drawString( opname, link_m+((operator_w-ow)/2), op_y+((operator_h-oh)/2)+oh );
		g.drawString( opname, link_m+((operator_w-ow)/2), op_y+(operator_h/2)+(oh/2));
	    }
	    
	    // draw a box 'round the whole thing
	    
	    int w = w_l > w_r ? w_l : w_r;
	    
	    g.setColor(Color.blue);
	
	    border_rect.x     = ((rule_panel.getWidth() - w) / 2) - border_gap;
	    border_rect.y     = yp - border_gap;
	    border_rect.width = w + (border_gap*2);
	    border_rect.height =h + (border_gap*2);
	    
	    g.drawRect(border_rect.x, border_rect.y, border_rect.width, border_rect.height); 
	    // xp-border_gap, yp-border_gap, w+(border_gap*2),  h+(border_gap*2));
	    
	    if(is_selected)
	    {
		 g.drawRect(border_rect.x-1, border_rect.y-1, border_rect.width+2, border_rect.height+2); 
	    }
	    
	    if(counts_are_valid)
	    {
		
		double passed_d = ((double)counter * 100.0) /  (double)spots_counted;
		String passed_s = mview.niceDouble(passed_d, 6,3);
		
	        int line_h = fm.getAscent() + fm.getDescent();

		String str = passed_s + " %";
		int cw = fm.stringWidth(str);

		int label_x = 5;

		int label_y = op_y + line_h + ((operator_h - line_h) / 2);

		g.setColor(dark_green_col);
		g.drawString(str, label_x, label_y);

		if(this == root_node)
		{
		    g.drawLine( label_x, label_y+1, label_x+cw, label_y+1 );
		}

		g.setColor(dark_red_col);
		double stopped_d = 100.0 - passed_d;
		String stopped_s = mview.niceDouble(stopped_d, 6,3);

		str = stopped_s + " %";

		cw = fm.stringWidth(str);
		    
		label_x = rule_panel.getWidth() - (cw + 5); 

		g.drawString(str, label_x, label_y);

		if(this == root_node)
		{
		    g.drawLine( label_x, label_y+1, label_x+cw, label_y+1 );
		}

		
	    }

	    return new Dimension(w+(border_gap*2), h+(border_gap*2));
	    
	}
    }

    // -----------------------------------------------------------------------------------
    // ----- leaf ------------------------------------------------------------------------
    // ----- leaf ------------------------------------------------------------------------
    // ----- leaf ------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------

    private class Leaf extends Node
    {
	
	public int multi_mode;  // one, all, at-least, at-most...
	public int multi_count;

	public Vector lhs_do;  // vector of DataOperand

	//public int lhs_type;    // Measurement, SpotAttribute
	//public int lhs_id;
	
	public int operator;

	public boolean rhs_const;   // otherwise use the DataOperand..

	public Operand rhs_const_op; // keep both separate to make UI handling easier
	public Operand rhs_var_op;

	public boolean is_selected;

	public JFrame editor;

	public String[] desc;    // the description used for this leaf

	public int width, height;

	private JComboBox multi_mode_jcb;
	private JTextField multi_count_jtf;
	private JComboBox operator_jcb;
	private JComboBox var_value_jcb;
	private JTextField const_value_jtf;
	private JRadioButton rhs_const_jrb;
	private JList list;
		

	final public boolean isLeaf()   { return true; }

	public Leaf(int o)
	{
	    multi_mode = 0;
	    multi_count = 0;

	    operator = o;
	    editor = null;

	    lhs_do = new Vector();
	    rhs_const_op = new ConstIntOperand(0);
	    rhs_var_op = null;
	    rhs_const = true;

	    desc = new String[2];
	    desc[0] = "( No rule selected,";
	    desc[1] = "  double click to edit )";

	    width  = leaf_w;
	    height = leaf_h;

	}

	public String toString()
	{
	    return toString(0);
	}

	public String toString(int spot_id)
	{
	    String res = operands_str[multi_mode];
	    if(lhs_do != null)
		for(int o=0; o < lhs_do.size(); o++)
		    res += (((Operand) lhs_do.elementAt(o)).toString(spot_id)) + " ";

	    Operand rop = rhs_const ? rhs_const_op : rhs_var_op;

	    return( res  + 
		    operator_str[operator] + 
		    (rop == null ? "" : rop.toString(spot_id) ) );
	}
	
	public final boolean filter(int spot_id) throws TypeMismatch
	{ 
	    int passed = 0;

	    for(int o=0; o < lhs_do.size(); o++)
	    {
		Operand dto = (Operand) lhs_do.elementAt(o);

		switch(operator)
		{
		case 0:  // >
		    if(dto.greaterThan(spot_id, rhs_const ? rhs_const_op : rhs_var_op))
			passed++;
		    break;
		case 1:  // <
		    if(dto.lessThan(spot_id, rhs_const ? rhs_const_op : rhs_var_op))
			passed++;
		    break;   
		case 2:  // >=
		    if(dto.greaterEquals(spot_id, rhs_const ? rhs_const_op : rhs_var_op))
			passed++;
		    break;
		case 3:  // <=
		    if(dto.lessEquals(spot_id, rhs_const ? rhs_const_op : rhs_var_op))
			passed++;
		    break;   
		case 4:  // ==
		    if(dto.equals(spot_id, rhs_const ? rhs_const_op : rhs_var_op))
			passed++;
		    break;   
		case 5:  // !=
		    if(dto.notEquals(spot_id, rhs_const ? rhs_const_op : rhs_var_op))
			passed++;
		    break;   
		}
	    }
	    
	    //if(spot_id < 5)
	    //  System.out.println("si=" + spot_id + " mode=" +  multi_mode + ", checked=" + lhs_do.size() + ", passed=" + passed);

	    switch(multi_mode)
	    {
	    case 0:  // single match
		return passed == 1;
	    case 1:  // none-of
		return passed == 0;
	    case 2:  // any-of
		return passed > 0;
	    case 3:  // at-least
		return (passed >= multi_count);
	    case 4:  // at-most;
		return (passed <= multi_count);
	    case 5:  // all-of
		return passed == lhs_do.size();
	    }
	    return false;
	}

	final public void unselect()
	{
	    is_selected = false;
	    //System.out.println("leaf " + operator + " unselected"); 
	}
	final public Node findSelected() 
	{
	    return is_selected ? this : null;
	}

	final public boolean handleClick(MouseEvent me)
	{
	    if(border_rect.contains(me.getPoint()))
	    {
		is_selected = true;
		    
		if(me.getClickCount() == 2)
		{
		    edit();
		}
		
		rule_panel.repaint();		    

		return true;
	    }
	    else
	    {
		is_selected = false;
		return false;
	    }
	}

	// returns size of leaf
	final public Dimension paint(Graphics g, int xp, int yp)
	{
	    if(fm == null)
		return new Dimension(1,1);

	    xp = (rule_panel.getWidth() - width) / 2;

	    int oh = fm.getAscent() + fm.getDescent();

	    height = (desc.length + 1) * oh;

	    g.setColor(Color.gray);

	    /*
	    g.drawLine( xp+1, yp+height+1, xp+width+1, yp+height+1);
	    g.drawLine( xp+width+1, yp+1, xp+width+1, yp+height+1);
	    */

	    g.drawLine( xp+2, yp+height+2, xp+width+2, yp+height+2);
	    g.drawLine( xp+width+2, yp+2, xp+width+2, yp+height+2);

	    g.setColor(Color.black);
	    
	    g.drawRect( xp, yp, width, height);
	    
	    
	    if(is_selected)
		g.drawRect( xp-1, yp-1, width+2, height+2);
	    
	    int typ = yp + ((height - (oh * (desc.length))) /2) + oh;
	    
	    for(int l=0; l < desc.length; l++)
	    {
		int ow = fm.stringWidth(desc[l]);
		
		g.drawString( desc[l], xp+((width-ow)/2), typ);
		
		typ += oh;
	    }
	    
	    if(counts_are_valid)
	    {
		
		double passed_d = ((double)counter * 100.0) /  (double)spots_counted;
		String passed_s = mview.niceDouble(passed_d, 6,3);
		
	        int line_h = fm.getAscent() + fm.getDescent();
		
		int label_y = yp + line_h + ((height - line_h) / 2);

		FontMetrics font_metrics = g.getFontMetrics();

		String str = passed_s + " %";

		int cw = font_metrics.stringWidth(str);
		int label_x = 5;
		
		g.setColor(dark_green_col);
		g.drawString(str, label_x, label_y);
		
		g.setColor(dark_red_col);

		double stopped_d = 100.0 - passed_d;
		String stopped_s = mview.niceDouble(stopped_d, 6,3);

		str = stopped_s + " %";

		cw = font_metrics.stringWidth(str);

		label_x = rule_panel.getWidth() - (cw + 5);

		g.drawString(str, label_x, label_y);
		
	    }

	    // System.out.println("paint " + width + "x" + height + " @ " + xp + "," + yp);

	    border_rect.x = xp;
	    border_rect.y = yp;
	    border_rect.width  = width;
	    border_rect.height = height;
	    
	    return new Dimension(width, height);
	}

	private final void edit()
	{
	    if(editor == null)
	    {
		JFrame eframe = new JFrame("Rule Editor");

		mview.decorateFrame( eframe );

		eframe.addWindowListener(new WindowAdapter() 
		    {
			public void windowClosing(WindowEvent e)
			{
			    editor.setVisible(false);
			    editor = null;
			    System.gc();
			}
		});
		
		JPanel epanel = new JPanel();
		eframe.getContentPane().add(epanel);
		epanel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	
		GridBagLayout gridbag = new GridBagLayout();
		epanel.setLayout(gridbag);
		GridBagConstraints c = null;

		// -----------------------------------------------------------


		/*
		final JComboBox leaf_mode_jcb = new JComboBox( leaf_modes_str );
		//multi_mode_jcb.setSelectedIndex(node.multi_mode);
		leaf_mode_jcb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    //node.multi_mode = multi_mode_jcb.getSelectedIndex();
			    
			    //multi_count_jtf.setEnabled(((node.multi_mode == 2) || (node.multi_mode == 3)));
			    
			    updateNode(node);
			}
		    });

		JPanel lm_panel = new JPanel();
		// lm_panel.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
		lm_panel.add(leaf_mode_jcb);
		
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1.0;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(lm_panel, c);
		epanel.add(lm_panel);
		*/

		multi_mode_jcb = new JComboBox( operands_str );
		multi_count_jtf = new JTextField( 10 );
		operator_jcb = new JComboBox( operator_str );
		var_value_jcb = new JComboBox( data_operand_names );
		const_value_jtf = new JTextField( 10 );
		rhs_const_jrb = new JRadioButton ();
		list = new JList();

		// -----------------------------------------------------------

		//DefaultListModel db_list_model = new DefaultListModel();
	    
		//String[] mnames = mview.getListOfMeasurementNames();
		
		list.setListData(data_operand_names);
		
		int[] sel_indices = new int[lhs_do.size()];
		
		for(int d=0; d < lhs_do.size(); d++)
		{
		    Operand d_o = (Operand) lhs_do.elementAt(d);
		    sel_indices[d] = d_o.index;
		}
		
		/*
		list.setSelectionMode(multi_mode > 0 ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
		*/
		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		list.setSelectedIndices( sel_indices );
	    
		/*
		list.addListSelectionListener(new ListSelectionListener() 
		    {
			public void valueChanged(ListSelectionEvent e)
			{
			    lhs_do.removeAllElements();
			    
			    for(int s=list.getMinSelectionIndex(); s <= list.getMaxSelectionIndex(); s++)
			    {
				if(list.isSelectedIndex(s))
				    lhs_do.addElement( data_operands.elementAt( s ));
			    }
			    
			    setDesc();
			    updateNode(Leaf.this);
			}
		    });
		*/

		JScrollPane jsp = new JScrollPane(list);
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.gridwidth = 3;
		c.fill = GridBagConstraints.BOTH;
		gridbag.setConstraints(jsp, c);
		epanel.add(jsp);
		
		// -----------------------------------------------------------

		multi_mode_jcb.setSelectedIndex(multi_mode);
		
		multi_mode_jcb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    int t_multi_mode = multi_mode_jcb.getSelectedIndex();
			    
			    multi_count_jtf.setEnabled(((t_multi_mode == 3) || (t_multi_mode == 4)));
			    
			    /*
			    list.setSelectionMode(t_multi_mode > 0 ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
			    */

			    //setDesc();
			    //updateNode(Leaf.this);
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 1.0;
		//c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(multi_mode_jcb, c);
		epanel.add(multi_mode_jcb);
		
		multi_count_jtf.setText(String.valueOf(multi_count));
		multi_count_jtf.setEnabled( ( multi_mode == 3 ) || ( multi_mode == 4 ) );
		
		/*
		LeafEditorTextFieldListener letfl = new LeafEditorTextFieldListener( this, multi_count_jtf, const_value_jtf);
		
		multi_count_jtf.getDocument().addDocumentListener(letfl);
		*/

		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 1;
		c.weightx = 2.0;
		c.fill = GridBagConstraints.BOTH;
		//c.anchor = GridBagConstraints.SOUTH;
		gridbag.setConstraints(multi_count_jtf, c);
		epanel.add(multi_count_jtf);
		
		operator_jcb.setSelectedIndex(operator);

		/*
		operator_jcb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    operator = operator_jcb.getSelectedIndex();
			    setDesc();
			    updateNode(Leaf.this);
			}
		    });
		*/

		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 1;
		c.weightx = 1.0;
		//c.fill = GridBagConstraints.HORIZONTAL;
		//c.anchor = GridBagConstraints.SOUTH;
		gridbag.setConstraints(operator_jcb, c);
		epanel.add(operator_jcb);
		
		// -----------------------------------------------------------

		JPanel op_panel = new JPanel();
		GridBagLayout op_gridbag = new GridBagLayout();
		op_panel.setLayout(op_gridbag);
		
		ButtonGroup bg = new ButtonGroup();

		rhs_const_jrb.setSelected(rhs_const);
		rhs_const_jrb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    //rhs_const = true;
			    var_value_jcb.setEnabled(false);
			    const_value_jtf.setEnabled(true);
			    //setDesc();
			    //updateNode(Leaf.this);
			}
		    });
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		op_gridbag.setConstraints(rhs_const_jrb, c);
		op_panel.add(rhs_const_jrb);
		bg.add(rhs_const_jrb);
		
		try
		{
		    if(rhs_const_op != null)
			const_value_jtf.setText(rhs_const_op.getText(-1));
		}
		catch(TypeMismatch tm)
		{
		}

		const_value_jtf.setEnabled(rhs_const);
		//const_value_jtf.getDocument().addDocumentListener(letfl);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 1.0;
		//c.anchor = GridBagConstraints.SOUTH;
		c.fill = GridBagConstraints.HORIZONTAL;
		op_gridbag.setConstraints(const_value_jtf, c);
		op_panel.add(const_value_jtf);
		
		JRadioButton jrb = new JRadioButton ();
		jrb.setSelected(!rhs_const);
		jrb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    //rhs_const = false;
			    var_value_jcb.setEnabled(true);
			    const_value_jtf.setEnabled(false);
			    //setDesc();
			    //updateNode(Leaf.this);
			}
		    });
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		op_gridbag.setConstraints(jrb, c);
		op_panel.add(jrb);
		bg.add(jrb);

		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 1;
		c.weightx = 1.0;
		//c.anchor = GridBagConstraints.SOUTH;
		c.fill = GridBagConstraints.HORIZONTAL;
		op_gridbag.setConstraints(var_value_jcb, c);
		op_panel.add(var_value_jcb);
		var_value_jcb.setEnabled(!rhs_const);
		if(rhs_var_op != null)
		    var_value_jcb.setSelectedItem(rhs_var_op.name);

		/*
		var_value_jcb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    rhs_var_op = (Operand) data_operands.elementAt( var_value_jcb.getSelectedIndex() );
			    setDesc();
			    updateNode(Leaf.this);
			}
		    });
		*/

		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 3;
		c.gridwidth = 3;
		c.weightx = 1.0;
		//c.anchor = GridBagConstraints.SOUTH;
		c.fill = GridBagConstraints.BOTH;
		gridbag.setConstraints(op_panel, c);
		epanel.add(op_panel);

		// ----------------

		JPanel wrap = new JPanel();
		wrap.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
	
		GridBagLayout wbag = new GridBagLayout();
		wrap.setLayout(wbag);
		
		JButton jb = new JButton("Apply");
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    applyEdits();
			}
		    });
		c = new GridBagConstraints();
		wbag.setConstraints(jb, c);
		wrap.add(jb);

		jb = new JButton("Help");
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    mview.getPluginHelpTopic("MultiFilter", "MultiFilter");
			}
		    });
		c.gridx = 1;
		c = new GridBagConstraints();
		wbag.setConstraints(jb, c);
		wrap.add(jb);

		jb = new JButton("Close");
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    editor.setVisible(false);
			    editor = null;
			    System.gc();
			}
		    });
		c.gridx = 2;
		c = new GridBagConstraints();
		wbag.setConstraints(jb, c);
		wrap.add(jb);

		c.gridx = 0;
		c.gridy = 4;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(wrap, c);
		epanel.add(wrap);

		// -----------------------------------------------------------

		editor = eframe;
		
		editor.pack();
		Point loc = rule_panel.getLocationOnScreen();
		editor.setLocation(loc.x + border_rect.x + border_rect.width, loc.y + border_rect.y);
	    }
	    
	    editor.setVisible(true);
	    editor.toFront();
	}

	private void setDesc()
	{
	    Vector descs = new Vector();

	    String muname = operands_str[multi_mode];
	    if(multi_mode > 0)
	    {
		if((multi_mode == 3) || (multi_mode == 4))
		    muname += " " + String.valueOf(multi_count);
		if(multi_mode > 0)
		    muname += " of";
		
		descs.addElement( muname );
	    }
		
	    //String opname = "";
	    if( lhs_do.size() == 1)
	    {
		descs.addElement( ((Operand)lhs_do.elementAt(0)).name.trim() );
	    }
	    else
	    {
		String buf = "";
		
		for(int o=0; o < lhs_do.size(); o++)
		{
		    String start = (o==0) ? "( " : "  " ;
		    String end   = (o==(lhs_do.size()-1)) ? " )" : "," ;
		    
		    buf += ( start + ((Operand)lhs_do.elementAt(o)).name.trim() + end );
		    if(buf.length() > 15)
		    {
			descs.addElement( buf );
			buf = "";
		    }
		}
		if(buf.length() > 0)
		    descs.addElement( buf );
	    }
	
	    String opval  = operator_str[operator] + " ";

	    if(rhs_const)
	    {
		if(rhs_const_op != null)
		{
		    String top = rhs_const_op.name.trim();
		    switch( rhs_const_op.type )
		    { 
		    case IntOperand :
		    case DoubleOperand :
			opval +=top;
			break;
		    case CharOperand :
			opval += "'" + top + "'";
			break;
		    case TextOperand :
			opval += "\"" + top + "\"";
			break;
		    }
		}
	    }
	    else
	    {
		if(rhs_var_op != null)
		{
		    /*
		    switch( rhs_var_op.type )
		    {
		    case IntOperand :
		    case DoubleOperand :
			opval += rhs_var_op.name.trim();
			break;
		    case CharOperand :
			opval += "'" + rhs_var_op.name.trim() + "'";
			break;
		    case TextOperand :
			opval += "\"" + rhs_var_op.name.trim() + "\"";
			break;
		    }
		    */
		    opval += rhs_var_op.name.trim();
		}
	    }

	    descs.addElement( opval );

	    /*
	    // if(dline < n_lines)
	    {
		if(lhs_do.size() == 1)
		    descs.addElement( opname + opval );
		else
		{
		    descs.addElement( opname );
		    descs.addElement( opval );
		}
	    }
	    */

	    desc = (String[]) descs.toArray(new String[0]);
	}

	private void applyEdits()
	{
	     lhs_do.removeAllElements();
			    
	     for(int s=list.getMinSelectionIndex(); s <= list.getMaxSelectionIndex(); s++)
	     {
		 if(list.isSelectedIndex(s))
		     lhs_do.addElement( data_operands.elementAt( s ));
	     }
	     
	     multi_mode = multi_mode_jcb.getSelectedIndex();

	     multi_count = (Integer.valueOf(multi_count_jtf.getText())).intValue();
	     
	     operator = operator_jcb.getSelectedIndex();

	     rhs_const = rhs_const_jrb.isSelected();

	     if( rhs_const )
	     {
		 rhs_const_op = makeOperand(const_value_jtf.getText());
		 rhs_var_op = null;
	     }
	     else
	     {
		 rhs_const_op = null;
		 int index = var_value_jcb.getSelectedIndex();
		 if(index >= 0)
		     rhs_var_op = (Operand) data_operands.elementAt( index );
		 else
		     rhs_var_op = null;
	     }
    

	     setDesc();
	     updateNode(Leaf.this);
	}
    }
    
    /*
     // handles the text fields in the leaf editor controls
    //
    class LeafEditorTextFieldListener implements DocumentListener 
    {
	private Leaf node;
	private JTextField multi_count_jtf;
	private JTextField const_value_jtf;

	public LeafEditorTextFieldListener(Leaf n, JTextField mc, JTextField cv)
	{
	    node = n;
	    multi_count_jtf = mc;
	    const_value_jtf = cv;
	}
	
	public void insertUpdate(DocumentEvent e)  { propagate(e); }
	public void removeUpdate(DocumentEvent e)  { propagate(e); }
	public void changedUpdate(DocumentEvent e) { propagate(e); }

	private void propagate(DocumentEvent e)
	{
	    try
	    {
		node.multi_count = (Integer.valueOf(multi_count_jtf.getText())).intValue();
		node.rhs_const_op = makeOperand(const_value_jtf.getText());
		
	    }
	    catch(NumberFormatException nfe)
	    {
		//		e.getDocument().setText("[Nan]");
	    }
	    node.setDesc();
	    updateNode(node);

	}
    }
    */
    
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  counting hits/misses
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private boolean counts_are_valid = false;
    private boolean reset_counter = false;
    private CounterThread counter_thread = null;
    private int spots_counted;
    private boolean type_mismatch_problem = false;
    private TypeMismatch first_type_mismatch = null;

    public void doCount()
    {
	if(counter_thread != null)
	{
	    reset_counter = true;
	}
	else
	{
	    reset_counter = false;
	    counter_thread = new CounterThread();
	    counter_thread.start();
	}
    }


    public class CounterThread extends Thread
    {
	public void run()
	{
	    if(root_node != null)
	    {
		counts_are_valid = false;
		
		type_mismatch_problem = false;
		first_type_mismatch = null;
		
		resetCounters(root_node);
		
		spots_counted = edata.getNumSpots();
		
		for(int s=0; ( s < spots_counted ) && ( type_mismatch_problem == false ); s++)
		{
		    if( reset_counter )
		    {
			reset_counter = false;
			resetCounters(root_node);
			s = 0;
		    }

		    countHitForNode(root_node, s);
		}

		if( type_mismatch_problem )
		    counts_are_valid = false;
		else
		    counts_are_valid = true; 
	    }

	    rule_panel.repaint();
	    
	    counter_thread = null;
	}
    }
    
    private void resetCounters(Node node)
    {
	node.counter = 0;

	if(node instanceof Branch)
	{
	    Branch b = (Branch) node;
	    resetCounters( b.left );
	    resetCounters( b.right );
	}
    }
    
    private void printCounters(Node node)
    {
	
	if(node instanceof Branch)
	{
	    Branch b = (Branch) node;
	    System.out.println(b.toString() + " : " + node.counter);
	    printCounters( b.left );
	    printCounters( b.right );
	}
	else
	{
	    System.out.println( ((Leaf)node).toString() + " : " + node.counter);
	}

    }

    private int countHitForNode(Node node, int s_id)
    {
	if( node instanceof Branch )
	{
	    Branch b = (Branch) node;

	    int total = countHitForNode( b.left, s_id );
	    total += countHitForNode( b.right, s_id );

	    if( b.operator == 0 )
	    {
		// AND

		if(total == 2)
		{
		    node.counter++;
		    return 1; 
		}
	    }
	    else
	    {
		// OR
		
		if(total > 0)
		{
		    node.counter++;
		    return 1; 
		}
	    }

	}
	else
	{
	    // NOTE: logic is reversed internally compared to normal...
	    // (for some unknown reason...)
	    
	    try
	    {
		if( node.filter( s_id ) == true ) 
		{
		    node.counter++;
		    return 1;
		}
	    }
	    catch( TypeMismatch tm )
	    {
		if( type_mismatch_problem == false )
		{
		    type_mismatch_problem = true;
		    first_type_mismatch = tm;
		    System.out.println( tm.getMessage() );
		}
	    }
	}
	return 0;
    }
    

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  read/write of rules
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    //
    // to keep things nice and simple for reading, have only one token per line
    // (avoids all sorts of nasty problems with delimiters)
    //
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private void readRule( )
    {
	JFileChooser fc = mview.getFileChooser();
	if(fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
	{
	    readRule( fc.getSelectedFile().getPath(), true );
	    
	    edata.notifyFilterChanged((ExprData.Filter) MultiFilter.this);
	}
    }
    
    private void readRule( String fname, boolean report_error )
    {
	try
	{
	    File file = new File(fname);
	    BufferedReader reader = new BufferedReader(new FileReader(file));

	    Node new_node =  readNode( reader );

	    if(new_node != null)
	    {
		root_node = new_node;
		rule_panel.repaint();
		doCount();
	    }
	}
	catch( java.io.IOException ioe )
	{
	    if(report_error) 
		mview.alertMessage( "Error whilst reading rule.\n\n" + ioe.toString() );
	}
	catch( Exception e )
	{
	    if(report_error) 
		mview.alertMessage( "Unable to parse rule.\n\n" + e.toString() );
	}
    }

    /*
    private String[] readAndTokeniseLine(  BufferedReader reader ) throws java.io.IOException
    {
	String line = reader.readLine();
	
    }
    */

    private int getInt( String str )
    {
	try
	{
	    return ( new Integer(str).intValue() );
	}
	catch(NumberFormatException nfe)
	{
	    System.out.println("expecting an integer, but found '" + str + "'");
	    return -1;
	}
    }

    private Node readNode( BufferedReader reader ) throws java.io.IOException
    {
	// System.out.println("readNode()");

	String keyword = reader.readLine();
	
	if(keyword.equals("BRANCH"))
	{
	    // System.out.println("readNode() branch");

	    String str = reader.readLine();
	    int op = str.equals("AND") ? 0 : 1;
	    
	    Branch br = new Branch( op, readNode( reader ), readNode( reader ) );

	    str = reader.readLine();

	    if(!str.equals("/BRANCH"))
	    {
		System.out.println("expecting \"/BRANCH\", but found " + str);
		return null;
	    }

	    // System.out.println("branch ok?");

	    return br;
	}
	if(keyword.equals("LEAF"))
	{
	    // System.out.println("readNode() leaf");
	    
	    int mm = getInt( reader.readLine() );
	    int mc = getInt( reader.readLine() );
	    int opcode = getInt( reader.readLine() );

	    Leaf lf = new Leaf( opcode );

	    lf.multi_mode  = mm;
	    lf.multi_count = mc;

	    String lop = reader.readLine();

	    if(lop.equals("LEFT"))
	    {
		int n_op = getInt( reader.readLine() );
		Vector op_vec = new Vector();
		
		for(int o=0; o < n_op; o++)
		{
		    Operand op = readOperand( reader );
		    if(op != null)
			op_vec.addElement( op );
		}
		
		if(op_vec.size() > 0)
		    lf.lhs_do = op_vec;

		lop = reader.readLine();

		if(!lop.equals("/LEFT"))
		{
		    System.out.println("expecting \"/LEFT\", but found " + lop);
		    return null;
		}

		lop = reader.readLine();

		if(lop.equals("RIGHT"))
		{
		    Operand op =  readOperand( reader );
		    if( (op instanceof MeasOperand) || (op instanceof SpotAttrOperand) )
		    {
			lf.rhs_const = false;
			lf.rhs_var_op = op;
			lf.rhs_const_op = null;
		    }
		    else
		    {
			lf.rhs_const = true;
			lf.rhs_const_op = op;
			lf.rhs_var_op = null;
		    }
		    
		    lop = reader.readLine();
		    if(!lop.equals("/RIGHT"))
		    {
			System.out.println("expecting \"/RIGHT\", but found " + lop);
			return null;
		    }
		    
		}
	    }

	    lop = reader.readLine();
	    if(!lop.equals("/LEAF"))
	    {
		System.out.println("expecting \"/LEAF\", but found " + lop);
		return null;
	    }

	    // System.out.println("leaf ok?" + lf.toString() );

	    lf.setDesc();

	    return lf;
	}

	return null;
    }

    private Operand readOperand( BufferedReader reader ) throws java.io.IOException
    {
	String keyword = reader.readLine();
	
	// System.out.println("readOperand() op=" + keyword);
	    
	if(keyword.equals("CONST_OP"))
	{
	    String val = reader.readLine();
	    
	    Operand c_op = makeOperand( val );

	    return c_op;
	}
	if(keyword.equals("MEAS_OP"))
	{
	    String m_name = reader.readLine();
	    int m_id = edata.getMeasurementFromName( m_name );
	    
	    if(m_id >= 0)
	    {
		MeasOperand mo =  new MeasOperand( m_name, m_id );
		mo.index = findMeasOpIndex( mo );
		return mo;
	    }
	    else
	    {
		return null;
	    }
	}

	if(keyword.equals("SPOTATTR_OP"))
	{
	    String m_name = reader.readLine();
	    String a_name = reader.readLine();
	    String a_type = reader.readLine();

	    int m_id = edata.getMeasurementFromName( m_name );
	    
	    if(m_id >= 0)
	    {
		ExprData.Measurement meas = edata.getMeasurement(m_id);

		int a_id = -1;

		for(int a=0; a < meas.getNumSpotAttributes(); a++)
		    if(meas.getSpotAttributeName(a).equals( a_name ))
			a_id = a;

		String name = "  " + m_name + "." + a_name;

		if(a_id >= 0)
		{
		    SpotAttrOperand sao = null;

		    if(a_type.equals("INT"))
			sao = new SpotAttrIntOperand( name, m_id, a_id );
		    if(a_type.equals("DOUBLE"))
			sao = new SpotAttrDoubleOperand( name, m_id, a_id );
		    if(a_type.equals("CHAR"))
			sao = new SpotAttrCharOperand( name, m_id, a_id );
		    if(a_type.equals("TEXT"))
			sao = new SpotAttrTextOperand( name, m_id, a_id );
		    
		    if(sao != null)
		    {
			sao.index = findSpotAttrOpIndex( sao );
			return sao;
		    }
		}
	    }
	}
	return null;
    }

    private int findMeasOpIndex( MeasOperand mo )
    {
	if(data_operands != null)
	{
	    for(int d=0; d < data_operands.size(); d++)
	    {
		Operand op = ((Operand) data_operands.elementAt(d));
		if(op instanceof MeasOperand)
		{
		    if(mo.name.equals(op.name))
			return op.index;
		}
	    }
	}
	return -1;
    }
    
    private int findSpotAttrOpIndex( SpotAttrOperand sao )
    {
	if(data_operands != null)
	{
	    for(int d=0; d < data_operands.size(); d++)
	    {
		Operand op = ((Operand) data_operands.elementAt(d));
		if(op instanceof SpotAttrOperand)
		{
		    if(sao.name.equals(op.name))
			return op.index;
		}
	    }
	}
	return -1;
    }

    // --- - --- - --- - --- - --- - --- - --- - --- 

    private void writeRule( )
    {
	JFileChooser fc = mview.getFileChooser();

	int ret_val =  fc.showSaveDialog(mview.getDataPlot()); 

	if(ret_val == JFileChooser.APPROVE_OPTION) 
	{
	    writeRule( fc.getSelectedFile().getPath() );
	}
    }
 
    private void writeRule( String fname )
    {
	try
	{
	    File file = new File(fname);
	    PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
	    writeNode( writer, root_node );
	    writer.flush();
	    writer.close();
	}
	catch( java.io.IOException ioe )
	{
	    mview.alertMessage( "Error whilst writing rule.\n\n" + ioe.toString() );
	}
	
    }

    private void writeNode( Writer writer, Node node ) throws java.io.IOException
    {
	if(node instanceof Branch) 
	{
	    Branch b = (Branch) node;
	    
	    // System.out.println("branch");
	    
	    writer.write("BRANCH\n" + ((b.operator == 0) ? "AND" : "OR") + "\n");
	    
	    writeNode( writer, b.left);
	    
	    writeNode( writer, b.right);
	    
	    writer.write("/BRANCH\n");
	}
	else
	{
	    Leaf l = (Leaf) node;
	    
	    // System.out.println("leaf");
	    
	    writer.write("LEAF\n" + l.multi_mode + 
			 "\n" + l.multi_count + 
			 "\n" + l.operator + "\n");
	    
	    if(l.lhs_do != null)
	    {
		writer.write("LEFT\n");
		writer.write( l.lhs_do.size() + "\n");

		for(int d=0; d < l.lhs_do.size(); d++)
		{
		    Operand data_op = (Operand) l.lhs_do.elementAt(d);
		    writeOperand( writer, data_op );
		}
		writer.write("/LEFT\n");
	    }
	    if(l.rhs_const_op != null)
	    {
		writer.write("RIGHT\nCONST_OP\n" + 
			     l.rhs_const_op.name + 
			     "\n/RIGHT\n");
		
	    }
	    if(l.rhs_var_op != null)
	    {
		writer.write("RIGHT\n");
		writeOperand( writer, l.rhs_var_op );
		writer.write("/RIGHT\n");
	    }
	    
	    writer.write("/LEAF\n");
	}

    }

    private void writeOperand( Writer writer, Operand op ) throws java.io.IOException
    {
	if(op instanceof MeasOperand)
	{
	    writer.write("MEAS_OP\n" + op.name + "\n");
	}
	if(op instanceof SpotAttrOperand)
	{
	    SpotAttrOperand saop = (SpotAttrOperand) op;
	    ExprData.Measurement meas = edata.getMeasurement( saop.meas_id );
	    String aname = meas.getSpotAttributeName( saop.sa_id );
	    String atype = meas.getSpotAttributeDataType( saop.sa_id );

	    writer.write("SPOTATTR_OP\n" + meas.getName() + "\n" + aname + "\n" + atype + "\n");
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
    private AnnotationLoader anlo;
    
    private Node root_node;

    /*
    public final static int NoFilter                         = 0;
    public final static int RemoveSpotsInNoCluster           = 1;
    public final static int RemoveSpotsInOneOrMoreClusters   = 2;
    public final static int RemoveSpotsInMoreThanOneCluster  = 3;
    public final static int RemoveSpotsInLessThanTwoClusters = 4;
    private int filter_mode = NoFilter;
    */
    
    private FontMetrics fm;

    private JFrame     frame = null;

    private RulePanel  rule_panel;
    private JScrollPane rule_panel_jsp;
}
