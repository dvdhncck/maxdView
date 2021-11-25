import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.util.*;

public class SortByNameOrValue implements Plugin, ExprData.ExprDataObserver
{
 
    public SortByNameOrValue(maxdView m_viewer)
    {
	mview = m_viewer;
	edata = mview.getExprData();
	dplot = mview.getDataPlot();	
    }

    public void cleanUp()
    {
	//edata.removeObserver(this);
	if(frame != null)
	    frame.setVisible(false);
	edata.removeObserver(this);
    }

    public void stopPlugin()
    {
	cleanUp();
    }
  
    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("Sort by Name or Value", "transform", 
					 "Sorts Spots by values, names or name attributes", "",
					 1, 0, 0);
	return pinf;
    }

    public PluginCommand[] getPluginCommands()
    {
	PluginCommand[] com = new PluginCommand[1];

	String[] args = new String[] 
	{ 
	    // name   // type      //default     // flag   // comment
    	    "value",  "string",    "",           "",       "specify which Measurement to sort",
	    "name",   "string",    "",           "",       "specify which Name or Name Attribute to sort",
	    "order",  "string",    "descending", "",       "one of 'ascending', 'descending', 'a-z', 'z-a', '0-9' or '9-0'",
	};
	
	com[0] = new PluginCommand("sort", args);
	
	return com;
     }

    public void runCommand(String name, String[] args, CommandSignal done) 
    { 
	if(name.equals("sort"))
	{
	    String direction = mview.getPluginStringArg("order", args, null);

	    int dir = 1;

	    if((direction != null) && 
	       ((direction.toLowerCase().startsWith("asc")) || (direction.toLowerCase().startsWith("a-z")) ) )
		dir = 0;

	    // is it a measurement name ?

	    String m_target = mview.getPluginStringArg("value", args, null);
	    
	    if(m_target != null)
	    {
		
		int m_id = edata.getMeasurementFromName( m_target );
		if(m_id >= 0)
		{
		    edata.sortSpots(m_id, dir, null);
		}
		
	    }

	    String n_target = mview.getPluginStringArg("name", args, null);
	    
	    if(n_target != null)
	    {
		// construct a NameTagSelection which matches this name

		ExprData.NameTagSelection nts = edata.new NameTagSelection();
		nts.setNames( n_target );
		
		if(name_nts == null)
		    name_nts = new NameTagSelector(mview);

		name_nts.setNameTagSelection( nts );
		
		doSort( 0, dir );
		
	    }

	}
	
	if(done != null)
	    done.signal();
    } 

    public void startPlugin()
    {
	frame = new JFrame("Sort by Name or Value");

	mview.decorateFrame( frame );

	frame.addWindowListener(new WindowAdapter() 
			  {
			      public void windowClosing(WindowEvent e)
			      {
				  cleanUp();
			      }
			  });


	JPanel sort_options = new JPanel();
	//sort_options.setPreferredSize(new Dimension(350, 250));
	sort_options.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	GridBagLayout gridbag = new GridBagLayout();
	sort_options.setLayout(gridbag);

	Dimension fillsize = new Dimension(8,8);

	// ========================================================================

	{
	    JPanel inner_panel = new JPanel();
	    GridBagLayout inner_gridbag = new GridBagLayout();
  	    inner_panel.setLayout(inner_gridbag);
	    TitledBorder title    = BorderFactory.createTitledBorder(" Sort By Name ");
	    Border space          = BorderFactory.createEmptyBorder(10, 10, 10, 10);
	    CompoundBorder border = BorderFactory.createCompoundBorder(title, space);
	    inner_panel.setBorder(border);
	    
	    // -------------------

	    name_nts = new NameTagSelector(mview);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.gridwidth = 3;
	    c.weighty = 1.0;
	    c.weightx = 2.0;
	    inner_gridbag.setConstraints(name_nts, c);
	    inner_panel.add(name_nts);
	     
	    addFiller( inner_panel, inner_gridbag, 1, 0, 8 );

	    JButton jb = new JButton("A...Z");
	    jb.setFont(mview.getSmallFont());
	    jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					 {
					     doSort(0, 0);
					 }
				     });
		
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 2;
	    c.anchor = GridBagConstraints.EAST;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    inner_gridbag.setConstraints(jb, c);
	    inner_panel.add(jb);
	    
	    addFiller( inner_panel, inner_gridbag, 2, 1, 8 );

	    jb = new JButton("Z...A");
	    jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					 {
					     doSort(0, 1);
					 }
				     });
	    jb.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = 2;
	    c.anchor = GridBagConstraints.WEST;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    inner_gridbag.setConstraints(jb, c);
	    inner_panel.add(jb);
	    
	    addFiller( inner_panel, inner_gridbag, 3, 0, 8 );

	    jb = new JButton("0...9");
	    jb.setFont(mview.getSmallFont());
	    jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					 {
					     doSort(0, 2);
					 }
				     });
		
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 4;
	    c.anchor = GridBagConstraints.EAST;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    inner_gridbag.setConstraints(jb, c);
	    inner_panel.add(jb);
	    
	    jb = new JButton("9...0");
	    jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					 {
					     doSort(0, 3);
					 }
				     });
	    jb.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = 4;
	    c.anchor = GridBagConstraints.WEST;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    inner_gridbag.setConstraints(jb, c);
	    inner_panel.add(jb);

	    // -------------------

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    //c.gridwidth = 3;
	    c.fill = GridBagConstraints.BOTH;
	    c.weighty = c.weightx = 1.0;
	    gridbag.setConstraints(inner_panel, c);
	    sort_options.add(inner_panel);
	}

	// ========================================================================

	{
	    JPanel inner_panel = new JPanel();
	    GridBagLayout inner_gridbag = new GridBagLayout();
	    inner_panel.setLayout(inner_gridbag);
	    TitledBorder title    = BorderFactory.createTitledBorder(" Sort By Value ");
	    Border space          = BorderFactory.createEmptyBorder(10, 10, 10, 10);
	    CompoundBorder border = BorderFactory.createCompoundBorder(title, space);
	    inner_panel.setBorder(border);
	    
	    // -------------------

	    meas_and_attr_jcb = new JComboBox();
	    populateList();
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridwidth = 3;
	    c.weighty = 1.0;
	    c.weightx = 2.0;
	    inner_gridbag.setConstraints(meas_and_attr_jcb, c);
	    inner_panel.add(meas_and_attr_jcb);
 
	    addFiller( inner_panel, inner_gridbag, 1, 0, 8 );

	    JButton jb = new JButton("A...Z");
	    jb.setFont(mview.getSmallFont());
	    jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					 {
					     doSort(1, 0);
					 }
				     });
		
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 2;
	    c.anchor = GridBagConstraints.EAST;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    inner_gridbag.setConstraints(jb, c);
	    inner_panel.add(jb);
	    
	    addFiller( inner_panel, inner_gridbag, 2, 1, 8 );

	    jb = new JButton("Z...A");
	    jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					 {
					     doSort(1, 1);
					 }
				     });
	    jb.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = 2;
	    c.anchor = GridBagConstraints.WEST;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    inner_gridbag.setConstraints(jb, c);
	    inner_panel.add(jb);
	    
	    addFiller( inner_panel, inner_gridbag, 3, 0, 8 );

	    jb = new JButton("0...9");
	    jb.setFont(mview.getSmallFont());
	    jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					 {
					     doSort(1, 2);
					 }
				     });
		
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 4;
	    c.anchor = GridBagConstraints.EAST;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    inner_gridbag.setConstraints(jb, c);
	    inner_panel.add(jb);
	    
	    jb = new JButton("9...0");
	    jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					 {
					     doSort(1, 3);
					 }
				     });
	    jb.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = 4;
	    c.anchor = GridBagConstraints.WEST;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    inner_gridbag.setConstraints(jb, c);
	    inner_panel.add(jb);


/*
	    Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    //gbc.fill = GridBagConstraints.HORIZONTAL;
	    inner_gridbag.setConstraints(filler, c);
	    inner_panel.add(filler);

	    JButton jb = new JButton("Ascending");
	    jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					 {
					     doSort(1, 0);
					 }
				     });
	    jb.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 2;
	    c.anchor = GridBagConstraints.EAST;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    inner_gridbag.setConstraints(jb, c);
	    inner_panel.add(jb);
	    
	   
	    jb = new JButton("Descending");
	    jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					 {
					     doSort(1, 1);
					 }
				     });
	    jb.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 2;
	    c.weighty = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    c.weightx = 1.0;
	    inner_gridbag.setConstraints(jb, c);
	    inner_panel.add(jb);
*/
	    
	    // -------------------

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    //c.gridwidth = 3;
	    c.fill = GridBagConstraints.BOTH;
	    c.weighty = c.weightx = 1.0;
	    gridbag.setConstraints(inner_panel, c);
	    sort_options.add(inner_panel);
	}
	

	// ========================================================================

	{
	    JPanel inner_panel = new JPanel();
	    GridBagLayout inner_gridbag = new GridBagLayout();
	    inner_panel.setLayout(inner_gridbag);
	    TitledBorder title    = BorderFactory.createTitledBorder(" Other Orderings ");
	    Border space          = BorderFactory.createEmptyBorder(10, 10, 10, 10);
	    CompoundBorder border = BorderFactory.createCompoundBorder(title, space);
	    inner_panel.setBorder(border);
	    
	    // -------------------

	    JButton jb = new JButton("Restore original order");
	    jb.setToolTipText("Arrange the Spots in the order in which they were originally loaded");

	    jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					 {
					     setDefaultOrder();
					 }
				     });
	    //jb.setFont(mview.getSmallFont());
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.WEST;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    inner_gridbag.setConstraints(jb, c);
	    inner_panel.add(jb);
	    
	   
	    Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    //gbc.fill = GridBagConstraints.HORIZONTAL;
	    inner_gridbag.setConstraints(filler, c);
	    inner_panel.add(filler);

	    jb = new JButton("Shuffle randomly");
	    jb.setToolTipText("Arrange the Spots into a random order");
	    jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					 {
					     setRandomOrder();
					 }
				     });
	    //jb.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 2;
	    c.weighty = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    c.weightx = 1.0;
	    inner_gridbag.setConstraints(jb, c);
	    inner_panel.add(jb);
		    
	    // -------------------

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 2;
	    //c.gridwidth = 3;
	    c.fill = GridBagConstraints.BOTH;
	    c.weighty = c.weightx = 1.0;
	    gridbag.setConstraints(inner_panel, c);
	    sort_options.add(inner_panel);
	}

	// ========================================================================

	{
	    JPanel buttons_panel = new JPanel();
	    buttons_panel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

	    GridBagLayout inner_gridbag = new GridBagLayout();
	    buttons_panel.setLayout(inner_gridbag);
	    {   
		
		final JButton jb = new JButton("Close");
		buttons_panel.add(jb);
		
		jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					 {
					     cleanUp();
					 }
				     });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		gridbag.setConstraints(jb, c);
	    }
	    {   
		final JButton jb = new JButton("Help");
		buttons_panel.add(jb);
		
		jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					 {
					     mview.getPluginHelpTopic("SortByNameOrValue", "SortByNameOrValue");
					 }
				     });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		gridbag.setConstraints(jb, c);
	    }
	    sort_options.add(buttons_panel);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 3;
	    //c.gridwidth = 2;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(buttons_panel, c);
	}
	
	// setMeasNames();

	edata.addObserver(this);

	frame.getContentPane().add(sort_options, BorderLayout.CENTER);
	frame.pack();
	frame.setVisible(true);
	
    }


    private void populateList()
    {
	Vector name_str_v = new Vector();
	Vector meas_spot_attr_v = new Vector();

	
	for(int mi=0; mi < edata.getNumMeasurements(); mi++)
	{
	    int m = edata.getMeasurementAtIndex(mi);

	    ExprData.Measurement ms = edata.getMeasurement(m);
	    
	    name_str_v.add( ms.getName() );
	    meas_spot_attr_v.add( new MeasSpotAttr( m, -1 ) );

	    for(int sa=0; sa < ms.getNumSpotAttributes(); sa++)
	    {
		String full_name = ms.getName() + "." + ms.getSpotAttributeName(sa);
		
		name_str_v.add( "  " + full_name );
		meas_spot_attr_v.add( new MeasSpotAttr( m, sa ) );
	    }
	}


	if( meas_and_attr_jcb != null)
	{
	    meas_and_attr_jcb.setModel( new DefaultComboBoxModel( name_str_v ) );
	    meas_and_attr_jcb.setSelectedIndex( -1 );
	}
 
	meas_spot_attr_a = (MeasSpotAttr[])  meas_spot_attr_v.toArray( new MeasSpotAttr[ meas_spot_attr_v.size() ] );
    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  observer implementation
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

    public void measurementUpdate(ExprData. MeasurementUpdateEvent mue)
    {
	switch(mue.event)
	{
	case ExprData.VisibilityChanged:
	case ExprData.SizeChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	case ExprData.NameChanged:
	    populateList();
	    break;
	}
    }
    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  doSort
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private abstract class TagEntry
    {
	public int spot_id;

	public abstract int compare( TagEntry te );
    }

    private class IntTagEntry extends TagEntry
    {
	public int int_val;

	public IntTagEntry( int s, int  i) 
	{ 
	    spot_id = s; 
	    int_val = i;
	}
	
	public int compare( TagEntry te )
	{
	    return ((IntTagEntry)te).int_val - int_val;
	}
    }

    private class DoubleTagEntry extends TagEntry
    {
	public double double_val;

	public DoubleTagEntry( int s, double d ) 
	{ 
	    spot_id = s; 
	    double_val = d;
	}
	
	public int compare( TagEntry te )
	{
	    
	    final double compare_value = ( ( DoubleTagEntry )te ).double_val;
	    if( compare_value > double_val )
		return 1;
	    if( compare_value < double_val )
		return -1;
	    return  0;
	}
    }

    private class StringTagEntry extends TagEntry
    {
	public String string_val;

	public StringTagEntry( int s, String ss ) 
	{ 
	    spot_id = s; 
	    string_val = ss;
	}
	public StringTagEntry( int s, char c ) 
	{ 
	    spot_id = s; 
	    string_val = String.valueOf( c );
	}
	
	public int compare( TagEntry te )
	{
	    String compare_str = ( (StringTagEntry) te ).string_val;
	    
	    if( string_val == null )
	    {
		return compare_str == null ? 0 : 1;
	    }
	    else
	    {
		if( compare_str == null )
		    return -1;
		else
		    return compare_str.compareTo( string_val );
	    }
	}
    }
    
    
    private class TagEntryComparator implements Comparator
    {
	public TagEntryComparator(int dir_)
	{
	    dir = dir_; // 0 ==ascending, !0 == descending
	}
	public int compare(Object o1, Object o2) 
	{ 
	    int result;

	    TagEntry t1 = (TagEntry) o1;
	    TagEntry t2 = (TagEntry) o2;
	    
	    return ( dir == 0 ) ? t2.compare( t1 ) :  t1.compare( t2 );
	}
	
	public boolean equals(Object obj)        { return false; }
	
	private int dir;
    }
    
    //
    // mode: 0=name,  1=value
    //
    //  dir: 0=a..z,  1=z..a,  2=0..9;  3=9..0
    //
    private void doSort( final int mode, final int dir ) 
    {
	if(mode == 0) // sort by name (including TagAttrs)
	{
	    final int n_spots = edata.getNumSpots();
	    final ExprData.NameTagSelection nts = name_nts.getNameTagSelection();
	    
	    final TagEntry[] tags = new TagEntry[n_spots];

	    for(int s=0; s < n_spots; s++)
	    {
		if( dir > 1 )
		{
		    tags[s] = new DoubleTagEntry( s, convertStringToNumber( nts.getNameTag(s) ) );
		}
		else
		{
		    tags[s] = new StringTagEntry( s, nts.getNameTag(s) );
		}
	    }

	    Arrays.sort(tags, new TagEntryComparator( dir > 1 ? (dir - 2) : dir ) );
	    
	    int[] new_order = new int[n_spots];
	    
	    for(int s=0; s < n_spots; s++)
		new_order[s] = tags[s].spot_id;
	    
	    edata.setSpotOrder(new_order);
	    
	    return;
	}

	if( mode == 1 ) // sort by value
	{

	    int sel = meas_and_attr_jcb.getSelectedIndex();

	    if( ( sel == -1 ) || ( sel >= meas_spot_attr_a.length ) )
	    {
		mview.alertMessage("Please select either a Measurement or a SpotAttribute to sort"); // 
		return;
	    }

	    final MeasSpotAttr msa = meas_spot_attr_a[ sel ];

	    final ExprData.Measurement ms = edata.getMeasurement( msa.meas_id );

	    TagEntry[] tags  = null;

	    if( msa.spot_attr_id == -1 )
	    {
		System.out.println("doSort(): Source is Measurement '" + ms.getName() + "', dir=" + dir);

		tags = makeTagArray( ms.getData() );
	    }
	    else
	    {
		System.out.println("doSort(): Source is Attribute '" +  ms.getSpotAttributeName( msa.spot_attr_id ) +
				   "' of Measurement '" + ms.getName()  +
				   "', dir=" + dir);

		Object spot_attr_data = ms.getSpotAttributeData( msa.spot_attr_id );
		    
		switch( ms.getSpotAttributeDataTypeCode( msa.spot_attr_id ) )
		{
		case ExprData.Measurement.SpotAttributeDoubleDataType:
		    tags = makeTagArray( (double[]) ms.getSpotAttributeData( msa.spot_attr_id ) );
		    break;

		case ExprData.Measurement.SpotAttributeIntDataType:
		    tags = makeTagArray( (int[]) ms.getSpotAttributeData( msa.spot_attr_id ) );
		    break;

		case ExprData.Measurement.SpotAttributeCharDataType:
		    tags = makeTagArray( (char[]) ms.getSpotAttributeData( msa.spot_attr_id ), dir );
		    break;

		case ExprData.Measurement.SpotAttributeTextDataType:
		    tags = makeTagArray( (String[]) ms.getSpotAttributeData( msa.spot_attr_id ),  dir );
		    break;
		}
	    }

	    if( tags == null )
	    {
		System.err.println("doSort(): Unable to make a TagArray...");
		return;
	    }

	    Arrays.sort( tags, new TagEntryComparator( dir > 1 ? (dir - 2) : dir ) );
	    
	    int[] new_order = new int[ tags.length ];
	    
	    for(int s=0; s < tags.length; s++)
		new_order[s] = tags[s].spot_id;
	    
	    edata.setSpotOrder( new_order );
	    
	    return;
	}
    }
    

    private TagEntry[] makeTagArray( final double[] data )
    {
	final int n_spots = data.length;
	
	final TagEntry[] tags = new TagEntry[ n_spots];
	
	for(int s=0; s < n_spots; s++)
	    tags[s] = new DoubleTagEntry( s, data[ s ] );

	return tags;
    }

    private TagEntry[] makeTagArray( final int[] data )
    {
	final int n_spots = data.length;
	
	final TagEntry[] tags = new TagEntry[ n_spots];
	
	for(int s=0; s < n_spots; s++)
	    tags[s] = new IntTagEntry( s, data[ s ] );

	return tags;
    }
    
    private TagEntry[] makeTagArray( final char[] data, final int dir )
    {
	final int n_spots = data.length;
	
	final TagEntry[] tags = new TagEntry[ n_spots];
	
	for(int s=0; s < n_spots; s++)
	{
	    if( dir > 1 ) // 2 = 0..9, 3 = 9..0
		tags[s] = new DoubleTagEntry( s, convertStringToNumber( String.valueOf( data[ s ] ) ) );
	    else          // 0 = a..z, 1 = z..a
		tags[s] = new StringTagEntry( s, data[ s ] );
	}

	return tags;
    }
    
    private TagEntry[] makeTagArray( final String[] data, final int dir )
    {
	final int n_spots = data.length;
	
	final TagEntry[] tags = new TagEntry[ n_spots];
	
	for(int s=0; s < n_spots; s++)
	{
	    if( dir > 1 )
		tags[s] = new DoubleTagEntry( s, convertStringToNumber( data[ s ] ) );
	    else
		tags[s] = new StringTagEntry( s, data[ s ] );
	}
	
	return tags;
    }

    //
    // extract any digit portion of a string, eg. "Spot00043" becomes 43
    //
    // return 0 for any string which cannot be converted
    //
    private double convertStringToNumber( String s )
    {
	if( s == null )
	    return 0;
	if( s.length() == 0 )
	    return 0;

	StringBuffer digits = new StringBuffer();

	for(int si=0; si < s.length(); si++)
	{
	    char ch = s.charAt( si );

	    if( Character.isDigit( ch ) || ( ch == '.' ) || ( ch == '-' ) )
	    {
		digits.append( s.charAt( si ) );
	    }
	}

	if( digits.length() == 0 )
	    return 0;

	try
	{
	    return ( Double.valueOf( digits.toString() ) ).doubleValue();
	}
	catch( NumberFormatException nfe )
	{
	    return 0;
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private void setDefaultOrder()
    {
	int[] order = new int[edata.getNumSpots()];
	for(int s=0;s< edata.getNumSpots(); s++)
	    order[s] = s;
	edata.setRowOrder(order);
    }

    private void setRandomOrder()
    {
	final int n_spots = edata.getNumSpots();

	final int[] order = new int[ n_spots ];

	for(int s=0; s < n_spots; s++)
	    order[s] = s;

	for(int shuffle=0; shuffle < n_spots; shuffle++)
	{
	    final int a = (int) ( Math.random() * (double) n_spots );
	    final int b = (int) ( Math.random() * (double) n_spots );
	    
	    final int temp = order[ a ];
	    order[ a ] = order[ b ];
	    order[ b ] = temp;
	}

	edata.setRowOrder(order);
    }
    
    private void addFiller( JPanel panel, GridBagLayout gb, int line, int col, int size )
    {
	Dimension fillsize = new Dimension(size,size);
	Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
	GridBagConstraints c = new GridBagConstraints();
	c.gridx = col;
	c.gridy = line;
	gb.setConstraints(filler, c);
	panel.add(filler);
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  stuff 
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  


    private class MeasSpotAttr 
    {
	public int meas_id;
	public int spot_attr_id;

	public MeasSpotAttr(int m, int a) 
	{
	    meas_id = m; spot_attr_id = a;
	}
	
    }
    

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  


    private MeasSpotAttr[] meas_spot_attr_a;

    private JComboBox meas_and_attr_jcb;

    private NameTagSelector name_nts;

    //private JProgressBar sort_progress;

    private maxdView mview;
    private ExprData edata;
    private DataPlot dplot;

    private JFrame frame;

}
