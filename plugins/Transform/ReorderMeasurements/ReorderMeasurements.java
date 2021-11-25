import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.*;

//import ExprData;

//
// distribute genes into a 2d array of bins based on the values in
//   two user controlled Measurements
//

public class ReorderMeasurements implements ExprData.ExprDataObserver, Plugin
{
    public ReorderMeasurements(maxdView mview_)
    {
	//System.out.println("++ ReorderMeasurements is constructed ++");

	mview = mview_;
	edata = mview.getExprData();
	dplot = mview.getDataPlot();

	initialiseOrder();

    }

    public void finalize()
    {
	//edata.removeObserver(this);
    }

    public void cleanUp()
    {
	edata.removeObserver(this);
	if(frame != null)
	    frame.setVisible(false);
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
	//System.out.println("++ ReorderMeasurements has been started ++");
	buildGUI();
	frame.pack();
	frame.setVisible(true);
	edata.addObserver(this);
    }

    public void stopPlugin()
    {
	//System.out.println("++ ReorderMeasurements has been stopped ++");
	cleanUp();
    }
  
    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("Reorder Measurements", "transform", 
							"Change the order of the columns", "",
								1, 1, 0);
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
	case ExprData.SizeChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	case ExprData.NameChanged:
	case ExprData.OrderChanged:
	    initialiseOrder();
	    thumbnails = null;
	    panel.repaint();
	    break;
	}	
    }
    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  command handlers
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void apply()
    {
	/*
	for(int s=0;s<edata.getNumMeasurements();s++)
	{
	    System.out.print(edata.getSetName(new_order[s]));
	    System.out.print(",");
	}
	System.out.println();
	*/
	edata.setMeasurementOrder(new_order);
	stopPlugin();
    }
    
    public void cancel()
    {
	cleanUp();
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  stuff
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

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

    private void buildGUI()
    {
	frame = new JFrame("Reorder Measurements");
	
	mview.decorateFrame( frame );

	frame.addWindowListener(new WindowAdapter() 
	    {
		public void windowClosing(WindowEvent e)
		{
		    cleanUp();
		}
	    });
	//	frame.getContentPane().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

	JPanel outer = new JPanel();
	outer.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	GridBagConstraints c = null;
	GridBagLayout gridbag = new GridBagLayout();
	outer.setLayout(gridbag);

	frame.getContentPane().add(outer);

	{
	    panel = new ReorderPanel();
	    panel.setPreferredSize(new Dimension(400, 100));

	    JScrollPane jsp = new JScrollPane(panel);

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weighty = c.weightx = 10.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(jsp, c);

	    outer.add(jsp);
	}

	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrapper.setLayout(w_gridbag);
	    wrapper.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));

	    {
		JButton button = new JButton("Sort by Name");
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    sortByName();
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 0;
		w_gridbag.setConstraints(button, c);
		wrapper.add(button);
	    }

	    addFiller( wrapper, w_gridbag, 0, 1, 16 );

	    {
		JButton button = new JButton("Sort by Attribute");
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    sortByAttribute();
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 2;
		w_gridbag.setConstraints(button, c);
		wrapper.add(button);
	    }


	    addFiller( wrapper, w_gridbag, 0, 3, 16 );
	    addFiller( wrapper, w_gridbag, 0, 4, 16 );
	    addFiller( wrapper, w_gridbag, 0, 5, 16 );
	    addFiller( wrapper, w_gridbag, 0, 6, 16 );

	    {
		JButton button = new JButton("Apply");
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    apply();
			}
		    });
		
		 c = new GridBagConstraints();
		c.gridx = 7;
		w_gridbag.setConstraints(button, c);
		wrapper.add(button);
	    }

	    addFiller( wrapper, w_gridbag, 0, 8, 16 );
	    addFiller( wrapper, w_gridbag, 0, 9, 16 );
	    addFiller( wrapper, w_gridbag, 0, 10, 16 );
	    addFiller( wrapper, w_gridbag, 0, 11, 16 );

	    {
		JButton button = new JButton("Cancel");
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    cancel();
			}
		    });
		
	        c = new GridBagConstraints();
		c.gridx = 12;
		w_gridbag.setConstraints(button, c);
		wrapper.add(button);
	    }

	    addFiller( wrapper, w_gridbag, 0, 13, 16 );

	    {
		JButton button = new JButton("Help");
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    mview.getPluginHelpTopic("ReorderMeasurements", "ReorderMeasurements");
			}
		    });
		
		 c = new GridBagConstraints();
		c.gridx = 14;
		w_gridbag.setConstraints(button, c);
		wrapper.add(button);
	    }

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.weightx = 10.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(wrapper, c);
	    outer.add(wrapper);
	}

    }

    private void initialiseOrder()
    {
	new_order = new int[edata.getNumMeasurements()];
	for(int m=0;m<edata.getNumMeasurements();m++)
	{
	    int mi = edata.getMeasurementAtIndex(m); //edata.getIndexOfMeasurement(m);
	    new_order[m] = mi;
	}
    }

    //
    // thumbnails are stored in the natural measurement order (i.e. the original order from the file)
    //
    private void generateThumbNails()
    {
	thumbnails = new Image[edata.getNumMeasurements()];

	double y_scale = (double)edata.getNumSpots() / (double)box_h;

	for(int s=0;s<edata.getNumMeasurements();s++)
	{
	    int[] pix = new int[box_w * box_h];
	    int index = 0;
	    for (int y = 0; y < box_h; y++) 
	    {
		int line = (int)((double)y * y_scale);

		if(line >= edata.getNumSpots())
		    line = edata.getNumSpots() - 1;

		//System.out.println("line " + line);

		Color c = dplot.getDataColour(edata.eValueAtIndex(s, line), s);
		int p = c.getRGB();
		for (int x = 0; x < box_w; x++) 
		{
		    pix[index++] = p;
		}
	    }
	    thumbnails[s] = frame.createImage(new MemoryImageSource(box_w, box_h, pix, 0, box_w));
	}
    }

    public class ReorderPanel extends JPanel implements MouseListener, MouseMotionListener
    {
	public ReorderPanel()
	{
	    super();
	    addMouseListener(this);
	    addMouseMotionListener(this);
	}

	public void mouseMoved(MouseEvent e) 
	{
	    int mouse_pos = findIndex(e.getX());
	    if(( mouse_pos >= 0 )  && ( mouse_pos < edata.getNumMeasurements() ))
		setToolTipText(edata.getMeasurementName(new_order[mouse_pos]));
	}

	public void mouseDragged(MouseEvent e) 
	{
	    if(dragging)
	    {
		// remove the drag set from the order...
		int[] new_new_order = new int[edata.getNumMeasurements()-1];
		int s;
		int p = 0;
		for(s=0;s<drag_cur_pos;s++)
		{
		    new_new_order[p++] = new_order[s];
		}
		for(s=drag_cur_pos+1;s<edata.getNumMeasurements();s++)
		{
		    new_new_order[p++] = new_order[s];
		}

		// choose the new insertion point
		drag_cur_pos = findIndex(e.getX());

		setToolTipText("Move to: " + edata.getMeasurementName(new_order[drag_cur_pos]));

		// and place back in the list
		new_order = new int[edata.getNumMeasurements()];
		p = 0;
		for(s=0;s<drag_cur_pos;s++)
		{
		    new_order[p++] = new_new_order[s];
		}
		new_order[p++] = drag_set;
		for(s=drag_cur_pos;s<(edata.getNumMeasurements()-1);s++)
		{
		    new_order[p++] = new_new_order[s];
		}

		repaint();
	    }
	} 
	public void mousePressed(MouseEvent e) 
	{
	    dragging = true;
	    drag_cur_pos = findIndex(e.getX());

	    // which set have we actually picked up?
	    drag_set = new_order[drag_cur_pos];

	    repaint();
	}
	public void mouseReleased(MouseEvent e) 
	{
	    dragging = false;
	    int end_pos = findIndex(e.getX());
	    new_order[end_pos] = drag_set;
	    repaint();
	}

	public void mouseEntered(MouseEvent e) 
	{
	}
	public void mouseExited(MouseEvent e) 
	{
	}
	public void mouseClicked(MouseEvent e) 
	{
	}

	// returns the 'local' index (i.e. counting sequentially from 0 at the left)
	//
	private int findIndex(int x)
	{
	    int m = x / (box_w + gap);

	    // m = new_order[m];

	    //int m = edata.getIndexOfMeasurement(mi);
	    if(m >= edata.getNumMeasurements())
		m = (edata.getNumMeasurements() - 1);
	    if(m < 0)
		m = 0;

	    return m;
	}

	public void setSize(Dimension d)
	{
	    super.setSize(d);

	    gap = 2;

	    box_h = getHeight() - (2 * gap);

	    double steps = (double)(getWidth() - gap) / ((double)edata.getNumMeasurements());

	    box_w = (int)steps - gap;
	    
	    if(box_w < 3)
		box_w = 3;

	    generateThumbNails();

	    System.gc();
	    // System.out.println("resized....");
	}

	public void paintComponent(Graphics graphic)
	{
	    if(graphic == null)
		return;

	    int font_height = graphic.getFontMetrics().getAscent();
	    
	    graphic.setColor(getBackground());
	    graphic.fillRect(0, 0, getWidth(), getHeight());
	    
	    int yp = gap;
	    int xp = gap;

	    if(thumbnails == null)
		generateThumbNails();

	    // draw the thumbnail images

	    for(int s=0;s<edata.getNumMeasurements();s++)
	    {
		//int mi = edata.getMeasurementAtIndex(s);
		graphic.drawImage(thumbnails[new_order[s]], xp, yp, this);

		if((dragging == true) && (new_order[s] == drag_set))
		{
		    graphic.setColor(Color.black);
		    graphic.drawRect(xp, yp, box_w, box_h);
		    graphic.setColor(Color.white);
		    graphic.drawRect(xp+1, yp+1, box_w-2, box_h-2);
		}

		xp += (box_w + gap);

		
	    }
	    
	    // now draw the strings....

	    xp = gap;
	    
	    graphic.setColor(Color.black);

	    int text_p = yp + font_height;

	    for(int s=0;s<edata.getNumMeasurements();s++)
	    {
		//int mi = edata.getMeasurementAtIndex(s);

		graphic.drawString(edata.getMeasurementName(new_order[s]), xp, text_p);

		xp += (box_w + gap);
		
		text_p += font_height;
		if(text_p > box_h)
		    text_p = yp + font_height;
	    }

	}
		
    }

    private void sortByName()
    {
	final int n_meas = edata.getNumMeasurements();

	final StringAndIndex[] data = new StringAndIndex[ n_meas ];

	for(int m=0; m < n_meas; m++)
	    data[ m ] = new StringAndIndex( edata.getMeasurementName( m ), m );

	sortUsingStringAndIndexArray( data );
    }

    private void sortByAttribute()
    {
	try
	{
	    final String[] attr_names = getAttributeNameList();

	    if( ( attr_names == null ) || ( attr_names.length == 0 ) )
	    {
		mview.alertMessage("None of the Measurements in this data have any Attributes defined.");
		return;
	    }

	    final int choice = mview.getChoice( "Pick an attribute", attr_names, -1 );

	    final String attr_name = attr_names[ choice ];

	    final int n_meas = edata.getNumMeasurements();

	    final StringAndIndex[] data = new StringAndIndex[ n_meas ];
	    
	    for(int m=0; m < n_meas; m++)
	    {
		data[ m ] = new StringAndIndex( edata.getMeasurement( m ).getAttribute( attr_name ), m );
	    }

	    sortUsingStringAndIndexArray( data );
	}
	catch( UserInputCancelled uic )
	{

	}
    }

    private void sortUsingStringAndIndexArray( final StringAndIndex[] data )
    {
	Arrays.sort( data, new StringAndIndexComparator() );

	final  int[] name_order = new int[ data.length ];

	for(int m=0; m < data.length; m++)
	    name_order[ m ] = data[ m ].index;

	new_order = name_order;

	panel.repaint();
    }

    private String[] getAttributeNameList()
    {
	Vector attr_n_v = new Vector();
	Hashtable uniq = new Hashtable();

	for(int m=0;m<edata.getNumMeasurements();m++)
	{
	    ExprData.Measurement ms = edata.getMeasurement(m);
	    for (Enumeration e = ms.getAttributes().keys(); e.hasMoreElements() ;) 
	    {
		final String name = (String) e.nextElement();
		if(uniq.get(name) == null)
		{
		    attr_n_v.addElement(name);
		}
		uniq.put(name, name);
	    }
	}
	
	String[] all_attr_names = (String[]) attr_n_v.toArray( new String[0] );

	Arrays.sort( all_attr_names );
	
	return all_attr_names;
    }


    private class StringAndIndex
    {
	public String string;
	public int index;
	public StringAndIndex( String s, int i ) { string = s; index = i; }
    }

    private class StringAndIndexComparator implements Comparator
    {
	final static String blank = "";

	public int compare( Object o1, Object o2 )
	{
	    String s1 = ( o1 == null ) ? blank : ((StringAndIndex) o1).string;
	    String s2 = ( o2 == null ) ? blank : ((StringAndIndex) o2).string;
	    
	    // StringAndIndex.string might be null for either operand
	    if( s1 == null ) 
		s1 = blank;
	    if( s2 == null ) 
		s2 = blank;

	    return s1.compareTo( s2 );
	}
	
	public boolean equals(Object o) { return false; }
    }


    private JFrame frame = null;

    private int[] new_order      = null;
    private int[] spandau_ballet = null;

    private Image[] thumbnails = null;

    private boolean dragging = false;
    private int drag_set;
    private int drag_cur_pos;

    private int box_w, box_h, gap;

    private ReorderPanel panel;
    private maxdView mview;
    private DataPlot dplot;
    private ExprData edata;

}
