import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.Hashtable;
import java.util.Vector;

public class BlenderColouriser implements Colouriser
{
    public BlenderColouriser()
    {
	name = null;
	colours = 0;
	steps = 0;
    }

    public BlenderColouriser(String name_, int s, int c)
    {
	name = name_;
	max = 1.0;
	min = 0.0;	
	colours = c;
	steps = s;

	initColours();
	buildRamp();
    }
    
    // =============================================
    //
    // Colouriser interface
    //
    // =============================================

    public int getNumDiscreteColours() { return colours; }

    public double getDiscreteColourValue(int dc)
    {
	return point[dc];
    }

    public String getName() { return name; }
    public void setName(String newname) { name = newname; }

    public Color lookup(double val)
    {
	int b = 0;

	// convert from real range (min...max) to normalised range (0...1)

	double norm = (val - min ) * to_normalised;
	//System.out.println("val= " + val + " norm=" +norm);


	// convert to a ramp index...

	// by finding the which chunk this number falls into
	
	int rp = 0;
	while(((rp+1)  < colours) && (norm > point[rp+1]))
	{
	    rp++;
	}

	if(rp == colours)
	    rp--;

	// and then scaling to the range of that chunk

	//if(rp  < colours)
	{
	    final int ind = ramp_start[rp] + (int) ((norm - point[rp]) * scale[rp]);
	    if(ind < 0)
		return ramp[ramp_start[rp]];
	    return  (ind < ramp.length) ? ramp[ind] : ramp[ramp.length-1];
	    
	}
	//else
	//    return Color.black;
    }
    
    public void setRange(double mi_, double ma_, Vector data_arrays)
    {
	//System.out.println("col " + name + ": set range....." + mi_ + " to " + ma_);
	
	min = mi_;
	max = ma_;

	range = max - min;

	if(range != 0)
	{
	    to_normalised =  1.0 / range;
	}

	/*
	int rp = 0;
	while(rp  < colours)
	{
	    System.out.println("  ramp " + rp + " starts " + point[rp] + " scale=" + scale[rp]);
	    rp++;
	}
	*/

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
	BlenderColouriser dc = new BlenderColouriser(name, steps, colours);
	dc.min = min;
	dc.max = max;
	dc.colour = (Color[]) colour.clone();
	dc.point  = (double[]) point.clone();
	return dc;
    }
    public Colouriser createFromAttrs(Hashtable attrs)
    {
	String type = (String) attrs.get("TYPE");
	if(type.equals("BlenderColouriser"))
	{
	    // System.out.println("it's a BlenderColouriser....");
	    try
	    {
		BlenderColouriser bc = new BlenderColouriser();
		
		bc.name = (String) attrs.get("NAME");
		
		String v =  (String) attrs.get("COLOURS");
		if( v != null )
		    bc.colours = new Integer(v).intValue();

		v =  (String) attrs.get("STEPS");
		if( v != null )
		    bc.steps = new Integer(v).intValue();

		bc.colour = new Color[bc.colours];
		bc.point =  new double[bc.colours];
		
		for(int c=0; c < bc.colours; c++)
		{
		    v =  (String) attrs.get("POINT" + String.valueOf(c));
		    if( v != null )
			bc.point[c] = NumberParser.tokenToDouble(v);

		    v =  (String) attrs.get("COLOUR" + String.valueOf(c));
		    if( v != null )
			bc.colour[c] = new Color(new Integer(v).intValue());
		}

		bc.buildRamp();

		return bc;
	    }
	    catch(TokenIsNotNumber e)
	    {
		return null;
	    }
	    catch(NumberFormatException e)
	    {
		return null;
	    }
	    
	}
	return null;
    }

    public Hashtable createAttrs()
    {
	Hashtable attrs = new Hashtable();
	attrs.put("NAME", name);
	attrs.put("TYPE", "BlenderColouriser");
	attrs.put("STEPS", String.valueOf(steps));
	attrs.put("COLOURS", String.valueOf(colours));
	for(int c=0; c < colours; c++)
	{
	    attrs.put("COLOUR" + String.valueOf(c), String.valueOf(colorToInt(colour[c])));
	    attrs.put("POINT" + String.valueOf(c), String.valueOf(point[c]));
	}
	
	return attrs;
    }

    public int colorToInt(Color c)
    {
	return (c.getRed() << 16) | (c.getGreen() << 8) | (c.getBlue());
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

	JPanel wrapper = new JPanel();
	{
	    JButton jb = new JButton(ii_add);
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			addColour();
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
			deleteColour();
		    }
		});
	    jb.setPreferredSize(new Dimension(20,20));
	    wrapper.add(jb);
	}
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	gridbag.setConstraints(wrapper, c);
	panel.add(wrapper);

	JButton jb = new JButton("Presets");
	Font f = jb.getFont();
	jb.setFont(new Font(f.getName(), Font.PLAIN, f.getSize() - 2));
	Insets ins = new Insets(1,4,1,4);
	jb.setMargin(ins);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 1;
	gridbag.setConstraints(jb, c);
	panel.add(jb);


	blend_edit = new BlendEditor(edata);

	/*
	blend_edit.setDragAction(new DragAndDropPanel.DragAction()
	    {
		public DragAndDropEntity getEntity(java.awt.dnd.DragGestureEvent event)
		{
		    DragAndDropEntity dnde =  DragAndDropEntity.createColouriserEntity(BlenderColouriser.this);
		    return dnde;
		}
	    });
	*/

	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 0;
	c.gridheight = 2;
	c.weightx = 5.0;
	c.weighty = 1.0;
	c.fill = GridBagConstraints.BOTH;
	gridbag.setConstraints(blend_edit, c);
	panel.add(blend_edit);
    }


    // =============================================
    //
    // GUI (inner class for panel)
    //
    // =============================================

    private class BlendEditor extends JPanel implements MouseListener, MouseMotionListener
    {
	public BlendEditor(final ExprData ed_)
	{
	    super();
	    edata = ed_;
	    //System.out.println("Blend is " + (ramp == null ? "null" : "ok") + "\n");
	    
	    // setPreferredSize(new Dimension((colours * 3), 20));
	    
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

	    if(ramp == null)
		return;

	    //System.out.println("paint.... " + getWidth() + " x " +  getHeight());
	    
	    int gap = 1; // getHeight() / 5;
	    
	    half_knob_width = (int)(Math.ceil((double)getHeight() * 0.15));

	    if(half_knob_width < 1)
		half_knob_width = 1;

	    // int[] xp = new int[5];
	    // int[] yp = new int[5];
	    
	    final double w_d = (double)(getWidth()-1);

	    // int last_pp = 0;

	    int ys = half_knob_width * 2; // ten_pc_h * 2;
	    int ye = getHeight() - (half_knob_width * 2) + 1; // (ten_pc_h * 2);

	    // draw each of the blend chunks
	    //

	    for(int p=0; p < (point.length-1); p++)
	    {
		int pstart = (int)(point[p]   * w_d);
		int pend   = (int)(point[p+1] * w_d);
		
		//
		// for each x column in the chunk ending at this point,
		//
		//     get the colour for that x val draw a line
		//

		try
		{
		    if(pend > pstart)
		    {
			double local_p = .0;
			double local_d = (point[p+1] - point[p]) / (double)((pend - pstart)+1);
			for(int pline = pstart; pline < pend; pline++)
			{
			    int local_ramp_pos = (int)(local_p * scale[p]);
			    local_p += local_d;
			    
			    g.setColor( ramp[ramp_start[p] + local_ramp_pos] );
			
			    g.drawLine(pline, ys, pline, ye);
			}
		    }
		}
		catch(ArrayIndexOutOfBoundsException aioobe)
		{
		    // should try to avoid this really....
		}
	    }

	    // draw boxes for each of the colours including the start and end ones
	    //

	    ys = half_knob_width;
	    ye = getHeight() - (2 * half_knob_width);

	    int kw =  (half_knob_width*2)-1;

	    for(int p=0; p < point.length; p++)
	    {
		int pp = (int)(point[p] * w_d);

		if(p == selected_point)
		{
		    g.setColor(Color.black);
		    
		    g.drawRect((pp-half_knob_width), ys, kw, ye);
		}

		g.setColor(colour[p]);
		
		g.fillRect(pp-half_knob_width, ys, kw, ye-1);

		if(p == selected_point)
		{
		    g.setColor(Color.white);
		    
		    g.drawRect((pp-half_knob_width)-1, ys-1, kw, ye);

		    
		    g.setColor(Color.black);
		    
		    double v = (point[p] * range) + min;
		    String s = String.valueOf(v);
		    
		    g.drawString(s, pp + half_knob_width, getHeight());
		}

	    }
	}
	
	// ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ----
	// ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ----
	
	public void mouseDragged(MouseEvent e)
	{
	    if(!dragging)
		return;
	    
	    double pn = (double) e.getX() / (double)getWidth();
	    
	    if(pn > 1.0)
		pn = 1.0;
	    else
		if(pn < 0.0)
		    pn = 0.0;

	    // watch out for points moving over other points...

	    if(pn > point[selected_point+1])
	    {
		if((selected_point+1) < colours)
		{
		    // moved up one, swap them over....
		    Color tmpc = colour[selected_point+1];
		    colour[selected_point+1] = colour[selected_point];
		    colour[selected_point] = tmpc;
		    point[selected_point] = point[selected_point+1];
		    point[selected_point+1] = pn;
		    selected_point++;
		}
	    }
	    else
	    {
		if(pn < point[selected_point-1])
		{
		    if((selected_point-1) > 0)
		    {
			// moved down one, swap them over....
			Color tmpc = colour[selected_point-1];
			colour[selected_point-1] = colour[selected_point];
			colour[selected_point] = tmpc;
			point[selected_point] = point[selected_point-1];
			point[selected_point-1] = pn;
			selected_point--;
		    }
		}
		else
		{
		    // still within our chunk
		    point[selected_point] = pn;
		}
	    }
	    
	    buildRamp();

	    blend_edit.repaint();

	}
	
	public void mouseMoved(MouseEvent e) 
	{
	}
	
	public void mousePressed(MouseEvent e) 
	{
	    // find nearest point
	    //
	    //int np = -1;
	    //double nd = 1.0;
	    
	    /*
	    double pn = (double) e.getX() / (double)getWidth();
	    
	    //System.out.println("pick at " + pp);
	    
	    for(int p=1; p < (point.length-1); p++)
	    {
		double d = Math.abs(point[p] - pn);
		//System.out.println("dist to " + point[p] + " = " + d);	    
		if(d < nd)
		{
		    nd = d;
		    np = p;
		    //System.out.println("**best");
		}
	    }
	    
	    //System.out.println("nearest is " + np);
	    */

	    int np = pickNearest(e, half_knob_width);

	    if((np > 0) && (np < (point.length-1)))
	    {
		selected_point = np;
		dragging = true;
		blend_edit.repaint(); //repaint();
		//System.out.println("point " + np + " selected");
	    }
	}
	
	public void mouseReleased(MouseEvent e) 
	{
	    if(dragging)
	    {
		dragging = false;
		
		// notify the world that the update has finished
		edata.generateDataUpdate(ExprData.ColourChanged);
	    }
	}
	
	public void mouseEntered(MouseEvent e) 
	{
	}
	
	public void mouseExited(MouseEvent e) 
	{
	}
	
	public void mouseClicked(MouseEvent e) 
	{
	    if (e.getClickCount() == 2) 
	    {
		int p = pickNearest(e,  half_knob_width);

		if((p > 0) && (p < (point.length-1)))
		    selected_point = p;
		else
		    selected_point = -1;

		if((p >= 0) && (p < point.length))
		{
		    Color new_colour = JColorChooser.showDialog(null,
								"Choose new Colou",
								colour[p]);
			if (new_colour != null) 
			{
			    colour[p] = new_colour;
			    buildRamp();
			    blend_edit.repaint();
			    edata.generateDataUpdate(ExprData.ColourChanged);
			}
		}
		else
		{
		    // create a new colour at this point
		    
		    double pn = (double) e.getX() / (double)getWidth();

		    addColour(pn);
		}
	    }
	}
	
	private int pickNearest(MouseEvent e, int scope)
	{
	    int np = -1;
	    int nd = getWidth();
	    
	    double pn = (double) e.getX() / (double)getWidth();
	    
	    //System.out.println("pick at " + pp);
	    
	    for(int p=0; p < point.length; p++)
	    {
		int pp = (int)(point[p] * (double)getWidth());

		int d = e.getX() - pp;
		if(d < 0)
		    d = -d;

		if(d < scope)
		{
		    //System.out.println("dist to " + point[p] + " = " + d);	    
		    if(d < nd)
		    {
			nd = d;
			np = p;
			//System.out.println("**best");
		    }
		}
	    }
	    
	    return np;
	}

    }
    
    // ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ----
    // ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ----
    // ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ---- * ----

    public void addColour()
    {
	//addNode();
	blend_edit.repaint();
    }
    
    public void addColour(double new_p)
    {
	// where to insert thew new colour?
	int pp = 0;

	while((new_p > point[pp]) && (pp < colours))
	{
	    pp++;
	}

	if(pp == colours)
	    // new point position is > 1.0
	    return;
	
	// System.out.println("adding new Colour at pos " + new_p + " in index " + pp);

	// set the colour based on the current blend at this point
	
	double p_in_real_range = min + (new_p * range);

	Color new_color_at_this_point = lookup(p_in_real_range);
	
	Color[] ncolour = new Color[colours+1];
	double[] npoint = new double[colours+1];

	int cpos = 0;
	for(int c=0; c< colours; c++)
	{
	    if(c == pp)
	    {
		ncolour[cpos] = new_color_at_this_point;
		npoint[cpos] = new_p;
		cpos++;
	    }
	    ncolour[cpos] = colour[c];
	    npoint[cpos]  = point[c];
	    cpos++;
	}
	colour = ncolour;
	point = npoint;
	colours++;

	buildRamp();
	blend_edit.repaint();
    }

    public void deleteColour()
    {
	if(blend_edit.selected_point > 0)
	{
	    //deleteNode(selected_point);
	    Color[] ncolour = new Color[colours-1];
	    double[] npoint = new double[colours-1];
	    
	    int cpos = 0;
	    for(int c=0; c< colours; c++)
	    {
		if(c != blend_edit.selected_point)
		{
		    ncolour[cpos] = colour[c];
		    npoint[cpos]  = point[c];
		    cpos++;
		}
	    }
	    colour = ncolour;
	    point = npoint;
	    colours--;
	    
	    blend_edit.selected_point = -1;

	    buildRamp();
	    blend_edit.repaint();
	}
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
 
    public int steps;

    public int colours;

    public String name;

    Color[] colour;
    double[] point;

 
    // the following fields are derived and do not have to be saved..

    double range, to_normalised;

    Color[] ramp;

    double[] scale;      // one scale for each chunk between 2 colour points
    int[]    ramp_start; // likewise

    BlendEditor blend_edit = null;

    private void initColours()
    {
	double p_delta = .0;

	if(colours < 2)
	    colours = 2;

	colour = new Color[colours];
	point = new double[colours];
	for(int c=0; c < colours; c++)
	{
	    colour[c] = ((c % 2) == 0) ? Color.white : Color.black;
	    point[c] = p_delta * (double)c;
	}

    //colour[c] = ((c % 2) == 0) ? Color.white : Color.black;
	
	/*
	if(colours < 2)
	{
	    colours = 6;
	    p_delta = (1.0 / (double)(colours-1));
	}
	
	
	
	for(int c=0; c < colours; c++)
	{
	    //colour[c] = ((c % 2) == 0) ? Color.white : Color.black;
	    colour[c] = Color.getHSBColor((float)(p_delta * (double)c), 1.0f, 1.0f);
	    point[c] = p_delta * (double)c;
	}
	*/

	point[0] = 0.0;
	point[colours-1] = 1.0;
    }

    private void buildRamp()
    {
	if(colours < 1)
	    ramp = null;

	// the ramp always has a fixed number of steps

	ramp = new Color[steps];
	
	// and the steps are divided equally between each the chunks defined by 2 colours
	//
	// so if there are 3 colours, A B & C there are 2 chunks, A-B & B-C
	//
	// each chunk uses (steps/chunks) entries in the ramp[] array even if the
	// chunks represent different lengths in the normalised space (0.0 - 1.0)
	//
	final int steps_per_chunk = steps / (colours - 1);

	final double step_c = 1.0 / (double)(steps_per_chunk-1);   // colour change per line (fixed 'height' step)

	ramp_start = new int[colours];
	scale      = new double[colours];

	//System.out.println("buildRamp().... " + steps + " steps and " + 
       	//	   colours + " colours equals " + steps_per_chunk + " stesp/chunk");

	for(int c=0; (c+1) < colours; c++)
	{
	    ramp_start[c] = steps_per_chunk * c;
	    
	    double end_p = ((c+1) == colours) ? 1.0 : point[c+1];

	    double step_range = end_p - point[c];
	    
	    scale[c] = (1.0 / step_range) * (double) (steps_per_chunk-1);

	    double start_d_r = (double)(colour[c].getRed());
	    double start_d_g = (double)(colour[c].getGreen());
	    double start_d_b = (double)(colour[c].getBlue());
	    
	    double end_d_r = (double)(colour[c+1].getRed());
	    double end_d_g = (double)(colour[c+1].getGreen());
	    double end_d_b = (double)(colour[c+1].getBlue());
	    
	    double step_start = 0.0;
	    
	    int rp = ramp_start[c];
	    for(int co=0; co < steps_per_chunk; co++)
	    {
		ramp[rp++] = new Color((int)(((1.0-step_start) * start_d_r) + (step_start * end_d_r)),
				       (int)(((1.0-step_start) * start_d_g) + (step_start * end_d_g)),
				       (int)(((1.0-step_start) * start_d_b) + (step_start * end_d_b)));
		
		step_start += step_c;
	    }

	    //System.out.println("buildRamp().... chunk " + c + " with range " + step_range + " and scale " + scale[c]);
	    //System.out.println("buildRamp().... starts at ramp pos " + ramp_start[c]);
	}
	
    }

    private void insertPoint(int p)
    {

    }

    private void deletePoint(int p)
    {

    }
}
