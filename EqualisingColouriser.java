import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.Hashtable;
import java.util.Vector;

public class EqualisingColouriser implements Colouriser
{
    public EqualisingColouriser()
    {
	this(null);
    }

    public EqualisingColouriser(String name_)
    {
	name = name_;
	max = 1.0;
	min = 0.0;	

	from_c = Color.white;
	to_c   = Color.black;

	n_levels = 2;

	partitionSpace();
    }
    
    // =============================================
    //
    // Colouriser interface
    //
    // =============================================

    public int getNumDiscreteColours() { return 2; }

    public double getDiscreteColourValue(int dc)
    {
	return .0;
    }

    public String getName() { return name; }
    public void setName(String newname) { name = newname; }

    public Color lookup(double val)
    {
	int b = (int)((val - min) * scale_to_bin);
	if((bin_to_colour != null) && (b < bin_to_colour.length) && (b >= 0))
	{
	    return  colours[bin_to_colour[b]];
	}
	else
	{
	    //System.out.println("bin " + b + "?");
	    return Color.black;
	}
    }
    
    public void setRange(double mi_, double ma_, Vector data_arrays_)
    {
	min = mi_;
	max = ma_;
	data_arrays = data_arrays_;
	
	partitionSpace();
	if(equal_edit != null)
	    equal_edit.repaint();
    }
    
    public JPanel getEditorPanel(maxdView mview, ExprData edata) 
    { 
	JPanel panel = new JPanel();
	addComponents(mview, edata, panel);
	return panel;
    }

    public double getMin() { return min; }
    public double getMax() { return max; }

    public Colouriser cloneColouriser()
    {
	EqualisingColouriser dc = new EqualisingColouriser(name);
	dc.min = min;
	dc.max = max;
	dc.data_arrays = data_arrays;
	dc.from_c = from_c;
	dc.to_c = to_c;
	dc.n_levels = n_levels;
	return dc;
    }

    public Colouriser createFromAttrs(Hashtable attrs)
    {
	String type = (String) attrs.get("TYPE");
	if(type.equals("EqualisingColouriser"))
	{
	    // System.out.println("it's a EqualisingColouriser....");
	    try
	    {
		EqualisingColouriser ec = new EqualisingColouriser();
		
		ec.name = (String) attrs.get("NAME");
		
		String v =  (String) attrs.get("FROM");
		if( v != null )
		    ec.from_c = new Color(new Integer(v).intValue());

		v =  (String) attrs.get("TO");
		if( v != null )
		    ec.to_c = new Color(new Integer(v).intValue());
		
		v =  (String) attrs.get("LEVELS");
		if( v != null )
		    ec.n_levels = new Integer(v).intValue();
		
		ec.partitionSpace();

		return ec;
	    }
	    catch(NumberFormatException e)
	    {
		System.out.println("NumberFormatException whilst creating EqualisingColouriser");
		return null;
	    }
	    
	}
	else
	{
	    System.out.println("wrong ColouriserType for EqualisingColouriser");
	}

	return null;
    }

    public Hashtable createAttrs()
    {
	Hashtable attrs = new Hashtable();
	attrs.put("NAME", name);
	attrs.put("TYPE", "EqualisingColouriser");

	attrs.put("LEVELS", String.valueOf(n_levels));
	attrs.put("FROM",   String.valueOf(colorToInt(from_c)));
	attrs.put("TO",     String.valueOf(colorToInt(to_c)));

	return attrs;
    }


    public int colorToInt(Color c)
    {
	return (c.getRed() << 16) | (c.getGreen() << 8) | (c.getBlue());
    }

    // =============================================
    //
    // partition space based on data distribution
    //
    // =============================================

    int[] bins;
    double[] bounds;
    int[] ibounds;
    int[] bin_to_colour;
    Color[] colours;
    String[] colour_ranges;   // used for ToolTips
    Vector data_arrays;

    Color from_c, to_c;

    int n_colours = 0;
    double scale_to_bin;

    int n_levels = 3;

    private void partitionSpace()
    {
	//System.out.println("partitioning to " + n_levels + " levels");
	
	int total_els = 0;

	if(data_arrays == null)
	{
	    bins = bin_to_colour = null;
	    return;
	}
  
	final int n_d_a = data_arrays.size();

	for(int da=0; da < n_d_a; da++)
	{
	    total_els += ((double[]) data_arrays.elementAt(da)).length;
	}
	
	if(total_els == 0)
	{
	    bins = bin_to_colour = null;
	    return;
	}

	int n_bounds = 0;	// 4 levels=1 + 2 + 4 + 8; // ==15
	for(int l=0; l < n_levels; l++)
	{
	    n_bounds += (1<<l); 
	}
	int start_mid = n_bounds - (1<<(n_levels-1));
	
	//System.out.println("will have " + n_bounds + " bounds, mid pt=" + start_mid);
	int n_bins = (int)((double)total_els * 0.1);
	if(n_bins < (n_bounds*2))
	    n_bins = (n_bounds*2);
	if(n_bins > 1024)
	    n_bins = 1024;

	bins = new int[n_bins];

	// System.out.println(n_bins + " bins, " + n_bounds + " bounds");

	scale_to_bin = (double)(n_bins-1) / (max-min);
	for(int da=0; da < n_d_a; da++)
	{
	    double[] daa = (double[]) data_arrays.elementAt(da);
	    for(int d=0; d < daa.length; d++)
	    {
		int b = (int)((daa[d] - min) * scale_to_bin);
		bins[b]++;
	    }
	}
	
	ibounds = new int[n_bounds];
	for(int p=0; p < n_bounds; p++)
	    ibounds[p] = -23;

	findMidPoints(0, n_bins-1, bins, ibounds, 0, n_levels, start_mid);

	bin_to_colour = new int[n_bins];

	for(int p=1; p < n_bounds; p++)
	{
	    for(int range=ibounds[p-1];  range <= ibounds[p]; range++)
		bin_to_colour[range] = p;
	}
	int last = ibounds[n_bounds-1];
	for(int l=last; l <n_bins; l++)
	    bin_to_colour[l] = n_bounds;


	/*
	for(int b=0; b < n_bins; b++)
	    System.out.print(bin_to_colour[b] + ",");
	System.out.println();
	*/

	n_colours = (n_bounds + 1);

	double blend_d = 255.0 / (double)(n_bounds);

	colours = new Color[n_colours];

	double from_r = (double)(from_c.getRed());
	double from_g = (double)(from_c.getGreen());
	double from_b = (double)(from_c.getBlue());
	
	double to_r = (double)(to_c.getRed());
	double to_g = (double)(to_c.getGreen());
	double to_b = (double)(to_c.getBlue());

	colour_ranges = new String[ n_colours ];

	for(int c=0; c < n_colours ; c++)
	{
	    final double d = (double)c / (double)(n_colours-1);

	    colours[c] = new Color( (int) (d * to_r + ((1.-d) * from_r)),
				    (int) (d * to_g + ((1.-d) * from_g)),
				    (int) (d * to_b + ((1.-d) * from_b)) );

	    
	    int lowb  = c > 0 ? ibounds[c-1] : 0;
	    int highb = (c+1) < n_colours ? ibounds[c] : ibounds[c-1];
	    
	    double lowv  = (lowb  / scale_to_bin) + min;
	    double highv = (c+1) < n_colours ? (highb / scale_to_bin) + min : max;
	    
	    colour_ranges[c] = niceDouble(lowv,9,4) + "..." + niceDouble(highv,9,4);
	}
    }

    private void findMidPoints(final int min, final int max, final int[] bins, final int[] ibounds, 
			       int depth, final int max_depth, final int bound_pos)
    {
	//System.out.println("findMidPoints() " + min + "..." + max + " -> " + bound_pos);

	int sum = 0;
	for(int b=min; b<=max; b++)
	{
	    sum += bins[b];
	}
	final int mid_sum = sum / 2;

	int mid = min;
	sum = bins[min];
	while(sum < mid_sum)
	{
	    sum += bins[++mid];
	}
	
	try
	{
	    ibounds[ bound_pos ] = mid;
	}
	catch(ArrayIndexOutOfBoundsException aioobe)
	{
	    System.out.println("aioobe! index was:" + bound_pos);
	}

	if(++depth < max_depth)
	{
	    int bound_offset = 1 << (max_depth-(depth+1));

	    findMidPoints(min,   mid, bins, ibounds, depth, max_depth, bound_pos-bound_offset);
	    findMidPoints(mid+1, max, bins, ibounds, depth, max_depth, bound_pos+bound_offset);
	}
    }
    // =============================================
    //
    // GUI
    //
    // =============================================

    private void addComponents(final maxdView mview, final ExprData edata, final JPanel panel)
    {
	panel.removeAll();
	GridBagLayout gridbag = new GridBagLayout();
	panel.setLayout(gridbag);
	GridBagConstraints c = null;

	ImageIcon ii_add = new ImageIcon(mview.getImageDirectory() + "add-box.gif");
	ImageIcon ii_del = new ImageIcon(mview.getImageDirectory() + "del-box.gif");

	JLabel label = new JLabel("Levels");

	Font f = label.getFont();
	Font sml = new Font(f.getName(), Font.PLAIN, f.getSize() - 2);
	label.setFont(sml);

	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	gridbag.setConstraints(label, c);
	panel.add(label);

	JPanel wrapper = new JPanel();
	{
	    JButton jb = new JButton(ii_add);
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			if(n_levels < 7)
			{
			    n_levels++;
			    partitionSpace();
			    equal_edit.repaint();
			    edata.generateDataUpdate(ExprData.ColourChanged);
			}
		    }
		});
	    jb.setPreferredSize(new Dimension(20,20));
	    jb.setToolTipText("Insert a new blend colour");
	    wrapper.add(jb);
	    
	    jb = new JButton(ii_del);
	    jb.setToolTipText("Remove the current blend colour");
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			if(n_levels > 1)
			{
			    n_levels--;
			    partitionSpace();
			    equal_edit.repaint();
			    edata.generateDataUpdate(ExprData.ColourChanged);
			}
		    }
		});
	    jb.setPreferredSize(new Dimension(20,20));
	    wrapper.add(jb);
	}
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 1;
	gridbag.setConstraints(wrapper, c);
	panel.add(wrapper);

	label = new JLabel("From");
	label.setFont(sml);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 2;
	gridbag.setConstraints(label, c);
	panel.add(label);

	final JButton fjb = new JButton();
	fjb.setBackground(from_c);
	fjb.setPreferredSize(new Dimension(32,20));
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 3;
	gridbag.setConstraints(fjb, c);
	panel.add(fjb);
	fjb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    Color new_colour = JColorChooser.showDialog(null,
								"Choose 'From' Colour",
								from_c);
		    if (new_colour != null) 
		    {
			fjb.setBackground(new_colour);
			from_c = new_colour;
			partitionSpace();
			equal_edit.repaint();
			edata.generateDataUpdate(ExprData.ColourChanged);
		    }
		}
	    });
	    
	 
	label = new JLabel("To");
	label.setFont(sml);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 4;
	gridbag.setConstraints(label, c);
	panel.add(label);

	final JButton tjb = new JButton();
	tjb.setBackground(to_c);
	tjb.setPreferredSize(new Dimension(32,20));
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 5;
	gridbag.setConstraints(tjb, c);
	panel.add(tjb);
	tjb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    Color new_colour = JColorChooser.showDialog(null,
								"Choose 'From' Colour",
								to_c);
		    if (new_colour != null) 
		    {
			tjb.setBackground(new_colour);
			to_c = new_colour;
			partitionSpace();
			equal_edit.repaint();
			edata.generateDataUpdate(ExprData.ColourChanged);
		    }
		}
	    });
	
	/*
	JButton jb = new JButton("Presets");
	jb.setFont();
	Insets ins = new Insets(1,4,1,4);
	jb.setMargin(ins);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 1;
	gridbag.setConstraints(jb, c);
	panel.add(jb);
	*/

	equal_edit = new EqualiserEditor(edata);

	/*
	equal_edit.setDragAction(new DragAndDropPanel.DragAction()
	    {
		public DragAndDropEntity getEntity(java.awt.dnd.DragGestureEvent event)
		{
		    DragAndDropEntity dnde =  DragAndDropEntity.createColouriserEntity(EqualisingColouriser.this);
		    return dnde;
		}
	    });
	*/

	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 0;
	c.gridheight = 6;
	c.weightx = 5.0;
	c.weighty = 1.0;
	c.fill = GridBagConstraints.BOTH;
	gridbag.setConstraints(equal_edit, c);
	panel.add(equal_edit);
    }



    // =============================================
    //
    // GUI (inner class for panel)
    //
    // =============================================

    private class EqualiserEditor extends JPanel implements MouseListener, MouseMotionListener
    {
	public EqualiserEditor(final ExprData ed_)
	{
	    super();
	    edata = ed_;
	    //System.out.println("Blend is " + (ramp == null ? "null" : "ok") + "\n");
	    
	    setPreferredSize(new Dimension(300, 100));
	    
	    addMouseListener(this);
	    addMouseMotionListener(this);
	}
	
	private ExprData edata;

	public boolean dragging;
	public int selected_point = -1;


	// ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ----
	// ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ----

	private int half_knob_width;

	public void paintComponent(Graphics g)
	{
	    super.paintComponent(g);

	    //System.out.println("paint.... " + getWidth() + " x " +  getHeight());
	    
	    g.setColor(Color.white);
	    g.fillRect(0,0,getWidth(),getHeight());
	    g.setColor(Color.lightGray);
	    if(bins != null)
	    {
		// set scaling factors
		//
		final int width = getWidth();
		int max_bin  = bins[0];
		for(int b=1; b < bins.length; b++)
		    if(bins[b] > max_bin)
			max_bin = bins[b];
		
		final int height = getHeight();
		int colh = height / 10;
		if(colh < 2)
		    colh = 2;

		int yp = height - colh;
		int ye = height;
		double bscale = (double)(height - colh) / (double)max_bin;
		double xscale = (double)width / (double)bins.length;
		double i_xscale = 1.0 / xscale;
		int xd = (int)xscale;
		if(xd < 1)
		    xd = 1;
	    
		// draw the histogram bins
		//
		for(int b=0; b < bins.length; b++)
		{
		    int yh = (int) (((double) bins[b]) * bscale);
		    int xp = (int) (xscale * (double) b);
		    g.fillRect(xp, yp-yh, xd, yh);
		}

		// and the bounds and colour ramp
		//
		if(ibounds != null)
		{
		    
		    for(int x=0; x < width; x++)
		    {
			int b = (int)((double)x * i_xscale);
			g.setColor(colours[bin_to_colour[b]]);
			g.drawLine(x, yp, x, ye);
		    }
		    
		    g.setColor(Color.darkGray);
		    
		    g.drawLine(0,yp,width,yp);
		    g.drawLine(0,ye,width,ye);
		    
		    for(int b=0; b < ibounds.length; b++)
		    {
			int xp = (int) (xscale * (double) ibounds[b]) + xd/2;
			g.drawLine(xp,0, xp, height);
		    }
		}
	    }
	}
	
	// ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ----
	// ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ----
	
	public void mouseDragged(MouseEvent e)
	{
	    if(!dragging)
		return;
	}
	
	public void mouseMoved(MouseEvent e) 
	{
	    if(bins == null)
		return;

	    double i_xscale = (double)bins.length / (double)getWidth();
	    int b = (int)((double)e.getX() * i_xscale);
	    int c = bin_to_colour[b];
	    
	    double bin_val = (b / scale_to_bin) + min;
	    
	    if(colour_ranges == null)
		return;

	    if(e.getY() > ((getHeight() * 9) / 10 ))
		// show the colour ranges along the bottom 10%
		setToolTipText( colour_ranges[c] );
	    else
		// and the bin ranges everywhere else
		setToolTipText( niceDouble( bin_val, 10, 4 ));
		
	}
	
	public void mousePressed(MouseEvent e) 
	{
	    
	}
	
	public void mouseReleased(MouseEvent e) 
	{
	    
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
	

    }
    
    public final String niceDouble(double d, int len, int dp)
    {
	String d_s = new Double(d).toString();

	// don't trim exponential values
	int exp_pt = d_s.indexOf('E');
	if(exp_pt > 0)
	    return d_s;

	// or numbers without a decimal point
	int dec_pt = d_s.indexOf('.');
	if(dec_pt < 0)
	    return d_s;

	int actual_decimals = d_s.length() - dec_pt;

	if(dp > actual_decimals)
	    return d_s;

	int trim_pt = dec_pt + dp;
	
	if(trim_pt > d_s.length())
	{
	    trim_pt = d_s.length();
	}

	return d_s.substring(0, trim_pt);
    }

    // ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ----
    // ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ----
    // ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ----

    // =============================================
    //
    // internals
    //
    // =============================================

  
    public double min, max;
 
    public String name;

    // the following fields are derived and do not have to be saved..

    EqualiserEditor equal_edit = null;


}
