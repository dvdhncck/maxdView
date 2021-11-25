import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.util.Hashtable;
import java.util.Vector;

public class RampedColouriser implements Colouriser
{
    public RampedColouriser(String name_, int s, Color neg_, Color zero_, Color pos_)
    {
	name = name_;
	max = 0.0;
	min = 0.0;	
	neg_col = neg_;
	zero_col = zero_;
	pos_col   = pos_;
	steps = s;

	auto_min = auto_max = true;
	user_max = user_min = .0;

	initPoints();
	update();
    }

    public RampedColouriser()
    {
	name = null;
    }    

    // =============================================
    //
    // Colouriser interface
    //
    // =============================================

    public String getName() { return name; }

    public void setName(String newname) { name = newname; }

    public int getNumDiscreteColours() { return 3; }

    public double getDiscreteColourValue(int dc)
    {
	if(dc == 0)
	    return the_min;
	if(dc == 1)
	    return .0;
	return the_max;
    }

    public Color lookup(double val)
    {
	if(val > 0)
	{
	    if(val > the_max)
	    {
		return pos_ramp[steps-1];
	    }
	    else
	    {
		int ci = (int)(val * pos_i_scale);
		return pos_ramp[ci];
	    }
	}
	if(val < 0)
	{
	    if(val < the_min)
	    {
		return neg_ramp[steps-1];
	    }
	    else
	    {
		int ci = (int)(val * neg_i_scale);
		return neg_ramp[ci];
	    }
	}
	return zero_col;
    }
    
    public void setRange(double mi_, double ma_, Vector data_arrays)
    {
	// System.out.println("col " + name + ": set range....." + mi_ + " to " + ma_);
	
	min = mi_;
	max = ma_;

	dont_propagate = true;
	if(min_val_label != null)
	    min_val_label.setText(niceDouble( auto_min ? min : user_min, 8, 4));
	if(max_val_label != null)
	    max_val_label.setText(niceDouble( auto_max ? max : user_max, 8, 4));
	update();
	dont_propagate = false;
    }
    
    public JPanel getEditorPanel(maxdView mview, ExprData edata) 
    { 
	JPanel panel = new JPanel();
	addComponents(mview, edata, panel);
	return panel;
    }

    public double getMin() { return the_min; }
    public double getMax() { return the_max; }

    public Colouriser cloneColouriser()
    {
	RampedColouriser rc = new RampedColouriser(name, steps, neg_col, zero_col, pos_col);
	rc.min = min;
	rc.max = max;
	rc.user_min = user_min;
	rc.user_max = user_max;
	rc.auto_min = auto_min;
	rc.auto_max = auto_max;
	rc.neg_point = (double[]) neg_point.clone();
	rc.pos_point = (double[]) pos_point.clone();
	rc.update();
	return rc;
    }

    public Colouriser createFromAttrs(Hashtable attrs)
    {
	String type = (String) attrs.get("TYPE");
	if(type.equals("RampedColouriser"))
	{
	    try
	    {
		RampedColouriser rc = new RampedColouriser();
		
		//System.out.println("it's a RampedColouriser....");
		
		rc.name = (String) attrs.get("NAME");
		
		String v =  (String) attrs.get("NEGCOL");
		rc.neg_col = new Color(new Integer(v).intValue());
		
		v =  (String) attrs.get("POSCOL");
		if( v != null )
		    rc.pos_col = new Color(new Integer(v).intValue());
		
		v =  (String) attrs.get("ZEROCOL");
		if( v != null )
		    rc.zero_col = new Color(new Integer(v).intValue());
		
		v =  (String) attrs.get("NEGPTS");
		
		int neg_pts = ( v == null ) ? 0 : new Integer(v).intValue();
		
		v =  (String) attrs.get("POSPTS");
		int pos_pts = ( v == null ) ? 0 : new Integer(v).intValue();
		
		v =  (String) attrs.get("STEPS");
		rc.steps = ( v == null ) ? 0 : new Integer(v).intValue();
		

		v =  (String) attrs.get("USERMIN");
		if(v != null)
		    rc.user_min = new Double(v).doubleValue();
		
		v =  (String) attrs.get("USERMAX");
		if(v != null)
		    rc.user_max = new Double(v).doubleValue();
		
		v = (String) attrs.get("AUTOMIN");
		if(v != null)
		    rc.auto_min = v.equals("TRUE");

		v = (String) attrs.get("AUTOMAX");
		if(v != null)
		    rc.auto_max = v.equals("TRUE");


		rc.neg_point = new double[neg_pts];
		
		for(int n=0; n < neg_pts; n++)
		{
		    v =  (String) attrs.get("NEGPT" + String.valueOf(n));
		    rc.neg_point[n] = NumberParser.tokenToDouble(v);
		}
		
		rc.pos_point = new double[pos_pts];

		for(int n=0; n < pos_pts; n++)
		{
		    v =  (String) attrs.get("POSPT" + String.valueOf(n));
		    rc.pos_point[n] = NumberParser.tokenToDouble(v);
		}
		
		rc.update();
		return rc;
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
	attrs.put("TYPE", "RampedColouriser");
	attrs.put("STEPS", String.valueOf(steps));
	attrs.put("NEGCOL", String.valueOf(colorToInt(neg_col)));
	attrs.put("POSCOL", String.valueOf(colorToInt(pos_col)));
	attrs.put("ZEROCOL", String.valueOf(colorToInt(zero_col)));
	attrs.put("NEGPTS", String.valueOf(neg_point.length));
	for(int p=0; p < neg_point.length; p++)
	    attrs.put("NEGPT" + String.valueOf(p), String.valueOf(neg_point[p]));
	attrs.put("POSPTS", String.valueOf(pos_point.length));
	for(int p=0; p < pos_point.length; p++)
	    attrs.put("POSPT" + String.valueOf(p), String.valueOf(pos_point[p]));
	
	attrs.put("USERMIN", String.valueOf(user_min));
	attrs.put("USERMAX", String.valueOf(user_max));
	attrs.put("AUTOMIN", auto_min ? "TRUE" : "FALSE");
	attrs.put("AUTOMAX", auto_max ? "TRUE" : "FALSE");
	


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

    private RampEditor neg_re;
    private RampEditor pos_re;

    private boolean dont_propagate = false;

    private void addComponents(final maxdView mview, final ExprData edata, final JPanel panel)
    {
	GridBagLayout gridbag = new GridBagLayout();
	panel.setLayout(gridbag);
	GridBagConstraints c = null;

	//>>>>>>>
	//
	// the min, zero and max labels
	//
	//<<<<<<<

	Font sml = new Font("Helvetica", Font.PLAIN, 9);
 
	{
	    min_val_label = new JTextField(9);
	    min_val_label.setFont(sml);
	    min_val_label.setEnabled(!auto_min);
	    min_val_label.setText(niceDouble( auto_min ? min : user_min, 8, 4));
	    min_val_label.getDocument().addDocumentListener(new ValueChangeListener(edata, 0));
	    panel.add(min_val_label);
	    
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(min_val_label, c);
	}
	{
	    auto_min_jchkb = new JCheckBox("Auto");
	    auto_min_jchkb.setSelected(auto_min);
	    auto_min_jchkb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			dont_propagate = true;
			auto_min = auto_min_jchkb.isSelected();
			min_val_label.setEnabled(!auto_min);
			min_val_label.setText(niceDouble( auto_min ? min : user_min, 8, 4));
			update();
			buildRamps();
			neg_re.repaint();
			edata.generateDataUpdate(ExprData.ColourChanged);
			dont_propagate = false;
		    }
		});
	    auto_min_jchkb.setFont(sml);
	    
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.WEST;
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(auto_min_jchkb, c);
	    panel.add(auto_min_jchkb);
	}


	{
	    JLabel label  = new JLabel( " 0.0 " );
	    panel.add(label);
	    
	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(label, c);
	}

	{
	    auto_max_jchkb = new JCheckBox("Auto");
	    auto_max_jchkb.setFont(sml);
	    auto_max_jchkb.setSelected(auto_max);
	    
	    auto_max_jchkb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			dont_propagate = true;
			auto_max = auto_max_jchkb.isSelected();
			max_val_label.setText(niceDouble( auto_max ? max : user_max, 8, 4));
			max_val_label.setEnabled(!auto_max);
			update();
			buildRamps();
			neg_re.repaint();
			edata.generateDataUpdate(ExprData.ColourChanged);
			dont_propagate = false;
		    }
		});
	    auto_max_jchkb.setHorizontalTextPosition(SwingConstants.LEFT);

	    c = new GridBagConstraints();
	    c.gridx = 3;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.EAST;
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(auto_max_jchkb, c);
	    panel.add(auto_max_jchkb);
	}
	{
	    max_val_label = new JTextField(9);
	    max_val_label.setFont(sml);
	    max_val_label.setText(niceDouble( auto_max ? max : user_max, 8, 4));
	    max_val_label.setEnabled(!auto_max);
	    max_val_label.getDocument().addDocumentListener(new ValueChangeListener(edata, 1));
	    panel.add(max_val_label);
	    
	    c = new GridBagConstraints();
	    c.gridx = 4;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.EAST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(max_val_label, c);
	}
	
	//>>>>>>>
	//
	// colour buttons and RampEditors
	//
	//<<<<<<<
	{
	    final JButton jb = new JButton();
	    jb.setPreferredSize(new Dimension(50,20));
	    panel.add(jb);
	    
	    jb.setBackground(neg_col);
	    
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			Color new_colour = JColorChooser.showDialog(null,
								    "Choose Colour for Negative values",
								    neg_col);
			if (new_colour != null) 
			{
			    jb.setBackground(new_colour);
			    neg_col = new_colour;
			    buildRamps();
			    neg_re.repaint();
			    edata.generateDataUpdate(ExprData.ColourChanged);
			}
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.weighty = c.weightx = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(jb, c);
	}

	{
	    neg_re = new RampEditor(edata, this, true);
	    panel.add(neg_re);
	    
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 1;
	    c.weighty = c.weightx = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(neg_re, c);
	}

	{
	    final JButton jb = new JButton();
	    jb.setPreferredSize(new Dimension(50,20));
	    panel.add(jb);
	    jb.setBackground(zero_col);
	    
	    jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					 {
					     Color new_colour = JColorChooser.showDialog(null,
											 "Choose Zero Colour",
											 zero_col);
					     if (new_colour != null) 
					     {
						 jb.setBackground(new_colour);
						 
						 zero_col = new_colour;
						 
						 buildRamps();

						 neg_re.repaint();
						 pos_re.repaint();

						 edata.generateDataUpdate(ExprData.ColourChanged);
						 //dplot.setZeroExprColour(new_colour);
					     }
					 }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = 1;
	    c.weighty = c.weightx = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(jb, c);
	}

	{  
	    pos_re = new RampEditor(edata, this, false);
	    panel.add(pos_re);
		
	    c = new GridBagConstraints();
	    c.gridx = 3;
	    c.gridy = 1;
	    c.weighty = c.weightx = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(pos_re, c);
	}

	{   
	    final JButton jb = new JButton();
	    jb.setPreferredSize(new Dimension(50,20));
	    panel.add(jb);
	    jb.setBackground(pos_col);
	    
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
					 {
					     Color new_colour = JColorChooser.showDialog(null,
											 "Choose Colour for Positive Values",
											 pos_col);
					     if(new_colour != null) 
					     {
						 pos_col = new_colour;
						 buildRamps();
						 jb.setBackground(new_colour);
						 edata.generateDataUpdate(ExprData.ColourChanged);
						 pos_re.repaint();
					     }
					 }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = 4;
	    c.gridy = 1;
	    c.weighty = c.weightx = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(jb, c);
	}
	
	//>>>>>>>
	//
	// RampEditor node buttons
	//
	//<<<<<<<
	{
	    JPanel wrapper = new JPanel();
	    {
		ImageIcon ii  = new ImageIcon(mview.getImageDirectory() + "add_node.gif");
		JButton jb = new JButton(ii);
		jb.setPreferredSize(new Dimension(20,20));
		jb.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { neg_re.addNode(); } } );
		wrapper.add(jb);
		
		ii = new ImageIcon(mview.getImageDirectory() + "del_node.gif");
		jb = new JButton(ii);
		jb.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { neg_re.deleteNode(); } } );
		jb.setPreferredSize(new Dimension(20,20));
		wrapper.add(jb);
	    }
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 2;
	    //c.anchor = GridBagConstraints.NORTH;
	    gridbag.setConstraints(wrapper, c);
	    panel.add(wrapper);
	}
	
	{
	    JPanel wrapper = new JPanel();
	    {
		ImageIcon ii  = new ImageIcon(mview.getImageDirectory() + "add_node.gif");
		JButton jb = new JButton(ii);
		jb.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { pos_re.addNode(); } } );
		jb.setPreferredSize(new Dimension(20,20));
		wrapper.add(jb);
		
		ii = new ImageIcon(mview.getImageDirectory() + "del_node.gif");
		jb = new JButton(ii);
		jb.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { pos_re.deleteNode(); } } );
		jb.setPreferredSize(new Dimension(20,20));
		wrapper.add(jb);
	    }
	    c = new GridBagConstraints();
	    c.gridx = 3;
	    c.gridy = 2;
	    //c.anchor = GridBagConstraints.NORTH;
	    gridbag.setConstraints(wrapper, c);
	    panel.add(wrapper);
	}

	dont_propagate = false;
	update();
	
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
 
    // handles the name text field in the NameFilter controls on the Filter panel
    //
    class ValueChangeListener implements DocumentListener 
    {
	private int min_o_max;
	ExprData edata;

	public ValueChangeListener(ExprData edata_, int min_o_max_)
	{
	    edata = edata_;
	    min_o_max = min_o_max_;
	}

	public void insertUpdate(DocumentEvent e)  { propagate(e); }
	public void removeUpdate(DocumentEvent e)  { propagate(e); }
	public void changedUpdate(DocumentEvent e) { propagate(e); }

	private void propagate(DocumentEvent e)
	{
	    if(!dont_propagate)
	    {
		if(min_o_max == 0)
		{
		    if(!auto_min)
		    {
			try
			{
			    user_min = (new Double(min_val_label.getText())).doubleValue();
			}
			catch(NumberFormatException nfe)
			{
			}
		    }
		}
		else
		{
		    if(!auto_max)
		    {
			try
			{
			    user_max = (new Double(max_val_label.getText())).doubleValue();
			}
			catch(NumberFormatException nfe)
			{
			}
		    }
		}
		
		update();
		buildRamps();
		neg_re.repaint();
		edata.generateDataUpdate(ExprData.ColourChanged);
		
		// System.out.println("propagate(): user_min=" + user_min + " user max=" + user_max); 
	    }
  	}
    }
    
    private JTextField min_val_label = null;
    private JTextField max_val_label = null;

    private JCheckBox auto_min_jchkb = null;
    private JCheckBox auto_max_jchkb = null;

    // =============================================
    //
    // internals
    //
    // =============================================


    public Color neg_col, zero_col, pos_col;

    public int steps;

    public  double min, max;
    private double user_min, user_max;
    private double the_min, the_max;

    private boolean auto_min, auto_max;

    public double[] pos_point;
    public double[] neg_point;

    public Color[] pos_ramp;
    public Color[] neg_ramp;

    public double pos_i_scale;
    public double neg_i_scale;

    public String name;

    private void initPoints()
    {
	// by default, have no extra points other than the start and end point
	//   which are always present..
	//
	int n_points = 2;
	
	neg_point = new double[n_points];
	pos_point = new double[n_points];

	neg_point[0] = 0.0;
	neg_point[1] = 1.0;

	pos_point[0] = 0.0;
	pos_point[1] = 1.0;

    }
    
    public void setSteps(int new_steps) { steps = new_steps; buildRamps(); }
    
    /*    

	  // maybe useful for I/O ?

    public Color getPosColour()   { return pos_col; }
    public Color getNegColour() { return neg_col; }
    
    public int    getNumPoints()    { return n_points; }
    public double getPoints(int pt) { return point[pt]; }
    */

    public void setPoint(boolean pos, int pt, double val)
    {
	double[] point = (pos ? pos_point : neg_point);

	if(val < .0)
	    val = .0;
	if(val > 1.0)
	    val = 1.0;
	
	point[pt] = val;
	for(int p=pt-1; p>=0; p--)
	{
	    if(point[p] > point[pt])
		point[p] = point[pt];
	}
	for(int p=pt+1; p<point.length; p++)
	{
	    if(point[p] < point[pt])
		point[p] = point[pt];
	}

	buildRamps();
    }
    
    public void removeAllNodes() 
    {
	pos_point = new double[0];
	neg_point = new double[0];
	
	buildRamps();
    }

    public void removeAllNodes(boolean pos) 
    {
	if(pos)
	{
	    pos_point = new double[0];
	}
	else
	{
	    neg_point = new double[0];
	}

	buildRamps();
    }
    
    public void addNode(boolean pos)
    {
	double[] point = (pos ? pos_point : neg_point);

	// find the biggest gap, and put the new node in the middle...
	double bg = 0;
	int bg_p = 0;
	for(int pp=0; pp< (point.length-1); pp++)
	{
	    double gap = point[pp+1] - point[pp];
	    if(gap > bg)
	    {
		bg = gap;
		bg_p = pp;
	    }
	}
	double new_pt_pos =  point[bg_p] + ((point[bg_p+1] - point[bg_p]) * 0.5);
	
	// add the new node
	int new_p = 0;
	double[] new_pt = new double[point.length+1];
	for(int old_p=0; old_p < point.length; old_p++)
	{
	    new_pt[new_p++] = point[old_p];
	    
	    if(old_p == bg_p) // this is the insert point
	    {
		//System.out.println("new node " + new_p + " added at " + new_pt_pos);
		new_pt[new_p++] = new_pt_pos;
	    }
	}
	
	if(pos)
	    pos_point = new_pt;
	else
	    neg_point = new_pt;

	update();
    }
    
    public void deleteNode(boolean pos, int n)
    {
	double[] point = (pos ? pos_point : neg_point);

	if(point.length < 3)
	    return;
	
	int new_p = 0;
	double[] new_pt = new double[point.length-1];
	for(int old_p=0; old_p < point.length; old_p++)
	{
	    if(old_p != n) // this is the insert point
	    {
		new_pt[new_p++] = point[old_p];
	    }
	}

	if(pos)
	    pos_point = new_pt;
	else
	    neg_point = new_pt;

	update();
    }
    
    public void update()
    {
	dont_propagate = true;

	/*
	if(min_val_label != null)
	{
	    String min_v = String.valueOf(min);
	    if(min_v.length() > 8)
		min_v = min_v.substring(0,8);
	    min_val_label.setText(min_v);

	    String max_v = String.valueOf(max);
	    if(max_v.length() > 8)
		max_v = max_v.substring(0,8);
	    max_val_label.setText(max_v);
	}
	*/

	the_min = auto_min ? min : user_min;
	the_max = auto_max ? max : user_max;

	pos_i_scale = (steps > 0) ? (1.0 / ((the_max) / (double)(steps-1))) : 0.0;
	neg_i_scale = (steps > 0) ? (1.0 / ((the_min) / (double)(steps-1))) : 0.0;

	buildRamps();
	dont_propagate = false;
	//neg_re.repaint();
	//pos_re.repaint();
    }
    
    private void buildRamps()
    {
	pos_ramp = buildRamp(zero_col, pos_col, pos_point);
	neg_ramp = buildRamp(zero_col, neg_col, neg_point);
    }

    private Color[] buildRamp(Color neg_col, Color pos_col, double[] point)
    {
	double neg_d_r = (double)(neg_col.getRed());
	double neg_d_g = (double)(neg_col.getGreen());
	double neg_d_b = (double)(neg_col.getBlue());
	
	double pos_d_r = (double)(pos_col.getRed());
	double pos_d_g = (double)(pos_col.getGreen());
	double pos_d_b = (double)(pos_col.getBlue());
	
	double step_c = 1.0 / (double)(point.length-1);   // colour change per line (fixed 'height' step)
	    
	Color[] ramp = new Color[steps];
	    
	//System.out.println("buildRamp().... (" + steps + " steps)\n");
	    //System.out.println("neg " + neg.toString() + " to " + to.toString());

	for(int line=0; line < point.length-1; line++)
	{
	    int cstart = (int)(point[line]   * (double)steps);
	    int cend   = (int)(point[line+1] * (double)steps);

	    if(cstart < 0)
		cstart = 0;
	    if(cend > steps)
		cend = steps;
	    
	    if(cend > cstart)
	    {
		int cp = cstart;
		
		double step_start = step_c * (double) line;
		double step_c_per_colour = step_c / (double)(cend - cstart);

		//System.out.println("colour slots " + cstart + " to " + (cend-1));
		//System.out.println("normalised coords " + point[line] + " to " + point[line+1]);
		
		while(cp < cend)
		{
		    ramp[cp] = new Color((int)(((1.0-step_start) * neg_d_r) + (step_start * pos_d_r)),
					 (int)(((1.0-step_start) * neg_d_g) + (step_start * pos_d_g)),
					 (int)(((1.0-step_start) * neg_d_b) + (step_start * pos_d_b)));
		    
		    //System.out.println(cp + "\t" + ramp[cp].getRed() + "," + 
		    //                   ramp[cp].getGreen() + "," + ramp[cp].getBlue());
		    
		    step_start += step_c_per_colour;
		    
		    cp++;
		}
	    }
	}
	
	// just to be on the safe side...
	ramp[0] = neg_col;
	ramp[steps-1] = pos_col;

	return ramp;
    }
}
