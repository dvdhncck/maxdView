import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.Hashtable;
import java.util.Vector;

public class DiscreteColouriser implements Colouriser
{
    public DiscreteColouriser()
    {
	name = null;
	boxes = 0;
    }

    public DiscreteColouriser(String name_, int b)
    {
	name = name_;
	max = 1.0;
	min = 0.0;	
	boxes = b;

	initBoxes();
    }
    
    // =============================================
    //
    // Colouriser interface
    //
    // =============================================

    public int getNumDiscreteColours() { return boxes; }

    public String getName() { return name; }
    public void setName(String newname) { name = newname; }

    public double getDiscreteColourValue(int dc)
    {
	return point[dc];
    }

    public Color lookup(double val)
    {
	int b = 0;
	while(b < boxes)
	{
	    switch(logic[b])
	    {
	    case 0:
		if(val > point[b])
		    return colour[b];
		break;
	    case 1:
		if(val < point[b])
		    return colour[b];
		break;
	    case 2:
		if(val >= point[b])
		    return colour[b];
		break;
	    case 3:
		if(val <= point[b])
		    return colour[b];
		break;
	    }
	    b++;
	}
	return Color.black;
    }
    
    public void setRange(double mi_, double ma_, Vector data_arrays)
    {
	// System.out.println("col " + name + ": set range....." + mi_ + " to " + ma_);
	
	min = mi_;
	max = ma_;
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
	DiscreteColouriser dc = new DiscreteColouriser(name, boxes);
	dc.min = min;
	dc.max = max;
	dc.colour = (Color[]) colour.clone();
	dc.point = (double[]) point.clone();
	dc.logic = (int[]) logic.clone();
	return dc;
    }
    public Colouriser createFromAttrs(Hashtable attrs)
    {
	String type = (String) attrs.get("TYPE");
	if(type.equals("DiscreteColouriser"))
	{
	    // System.out.println("it's a DiscreteColouriser....");
	    try
	    {
		DiscreteColouriser dc = new DiscreteColouriser();
		
		dc.name = (String) attrs.get("NAME");
		
		String v =  (String) attrs.get("BOXES");
		dc.boxes = new Integer(v).intValue();

		dc.colour = new Color[dc.boxes];
		dc.point =  new double[dc.boxes];
		dc.logic =  new int[dc.boxes];
		
		for(int b=0; b < dc.boxes; b++)
		{
		    v =  (String) attrs.get("POINT" + String.valueOf(b));
		    dc.point[b] = NumberParser.tokenToDouble(v);

		    v =  (String) attrs.get("COLOUR" + String.valueOf(b));
		    dc.colour[b] = new Color(new Integer(v).intValue());
		    
		    v =  (String) attrs.get("LOGIC" + String.valueOf(b));
		    dc.logic[b] = new Integer(v).intValue();
		}

		return dc;
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
	attrs.put("TYPE", "DiscreteColouriser");
	attrs.put("BOXES", String.valueOf(boxes));
	for(int b=0; b < boxes; b++)
	{
	    attrs.put("COLOUR" + String.valueOf(b), String.valueOf(colorToInt(colour[b])));
	    attrs.put("LOGIC" + String.valueOf(b), String.valueOf(logic[b]));
	    attrs.put("POINT" + String.valueOf(b), String.valueOf(point[b]));
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

	point_jtf = new JTextField[boxes];
	logic_jcb = new JComboBox[boxes];

	String[] logic_opts = { ">", "<", ">=", "<=" };

	Font small_font = null;

	ImageIcon ii_add = new ImageIcon(mview.getImageDirectory() + "add-box.gif");
	ImageIcon ii_del = new ImageIcon(mview.getImageDirectory() + "del-box.gif");
	
	Dimension fillsize = new Dimension(10,10);

	Box.Filler filler = null;

	for(int b=0; b < boxes; b++)
	{
	    logic_jcb[b] = new JComboBox(logic_opts);

	    if(b == 0)
	    {
		Font f = logic_jcb[b].getFont();
		small_font = new Font(f.getName(), Font.PLAIN, f.getSize() - 2);
	    }
	    logic_jcb[b].setFont(small_font);
	    logic_jcb[b].setToolTipText("Select which operator to apply");

	    logic_jcb[b].setSelectedIndex(logic[b]);
	    logic_jcb[b].addActionListener(new LogicComboActionListener(edata, b));
	    c = new GridBagConstraints();
	    c.gridx = (b * 3);
	    c.gridy = 0;
	    //c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(logic_jcb[b], c);
	    panel.add(logic_jcb[b]);

	    point_jtf[b] = new JTextField(5);

	    point_jtf[b].setText(String.valueOf(point[b]));
	    
	    point_jtf[b].getDocument().addDocumentListener(new PointChangeListener(edata, b));

	    c = new GridBagConstraints();
	    c.gridx = (b * 3)+1;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(point_jtf[b], c);
	    panel.add(point_jtf[b]);

	    if((b+1) < boxes)
	    {
		filler = new Box.Filler(fillsize, fillsize, fillsize);
		c = new GridBagConstraints();
		c.gridx = (b * 3)+2;
		c.gridy = 0;
		//c.weighty = c.weightx = 1.0;
		//c.fill = GridBagConstraints.BOTH;
		gridbag.setConstraints(filler, c);
		panel.add(filler);
	    }

	    JButton jb = new JButton();
	    jb.setToolTipText("Press to change this colour");
	    jb.setMinimumSize(new Dimension(30,20));
	    jb.setBackground(colour[b]);
	    jb.addActionListener(new ColourButtonActionListener(edata, b));
    
	    c = new GridBagConstraints();
	    c.gridx = b * 3;
	    c.gridy = 1;
	    c.gridwidth = 2;
	    c.weighty = c.weightx = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(jb, c);
	    panel.add(jb);
	    
	    

	    JPanel wrapper = new JPanel();
	    {
		jb = new JButton(ii_add);
		jb.addActionListener(new CustomActionListener(mview, edata, panel, 0, b));
		jb.setPreferredSize(new Dimension(20,20));
		jb.setToolTipText("Insert a new box here");
		wrapper.add(jb);
		
		jb = new JButton(ii_del);
		jb.setToolTipText("Remove this box");
		jb.addActionListener(new CustomActionListener(mview, edata, panel, 1, b));
		jb.setPreferredSize(new Dimension(20,20));
		wrapper.add(jb);
	    }
	    c = new GridBagConstraints();
	    c.gridx = b * 3;
	    c.gridy = 2;
	    c.gridwidth = 2;
	    gridbag.setConstraints(wrapper, c);
	    panel.add(wrapper);

	}
    }

    private class CustomActionListener implements ActionListener
    {
	public  CustomActionListener(final maxdView m, final ExprData e, final JPanel p, final int a, final int b)
	{
	    mview = m;
	    edata = e;
	    box = b;
	    action = a;
	    panel = p;
	}
	public void actionPerformed(ActionEvent e) 
	{
	    switch(action)
	    {
	    case 0:
		{
		    // System.out.println("inserting, there are currently  " + boxes + " boxes");
		    
		    // insert box
		    int[] nlogic = new int[boxes+1];
		    Color[] ncolour = new Color[boxes+1];
		    double[] npoint = new double[boxes+1];
		    
		    int bpos = 0;
		    for(int b=0; b < boxes; b++)
		    {
			if(b == box)
			{
			    nlogic[bpos] = 0;
			    ncolour[bpos] = Color.white;
			    npoint[bpos] = .0;
			    bpos++;
			}
			
			nlogic[bpos] = logic[b]; 
			ncolour[bpos] = colour[b]; 
			npoint[bpos] = point[b]; 
			bpos++;
		    }
		    logic = nlogic;
		    colour = ncolour;
		    point = npoint;
		    boxes++;
		    
		    addComponents(mview, edata, panel);
		    panel.updateUI();
		    
		    edata.generateDataUpdate(ExprData.VisibilityChanged);
		}

		break;
	    case 1:
		// remove box
		
		if(boxes > 0)
		{
		    // System.out.println("removing, there are currently  " + boxes + " boxes");

		    int[] nlogic = new int[boxes-1];
		    Color[] ncolour = new Color[boxes-1];
		    double[] npoint = new double[boxes-1];
		    
		    int bpos = 0;
		    for(int b=0; b < boxes; b++)
		    {
			if(b != box)
			{
			    nlogic[bpos] = logic[b]; 
			    ncolour[bpos] = colour[b]; 
			    npoint[bpos] = point[b]; 
			    bpos++;
			}
		    }
		    logic = nlogic;
		    colour = ncolour;
		    point = npoint;
		    boxes--;
		    
		    addComponents(mview, edata, panel);
		    panel.updateUI();
		    
		    edata.generateDataUpdate(ExprData.VisibilityChanged);
		}
		break;
	    }
	    
	}
	private maxdView mview;
	private ExprData edata;
	private int box;
	private int action;
	private JPanel panel;

    }

    private class ColourButtonActionListener implements ActionListener
    {
	public  ColourButtonActionListener(final ExprData e, final int b)
	{
	    edata = e;
	    box = b;
	}
	public void actionPerformed(ActionEvent e) 
	{
	    Color new_colour = JColorChooser.showDialog(null,
							"Choose Colour",
							colour[box]);
	    if (new_colour != null) 
	    {
		((JButton)e.getSource()).setBackground(new_colour);
		colour[box] = new_colour;
		edata.generateDataUpdate(ExprData.VisibilityChanged);

	    }
	}
	private ExprData edata;
	private int box;
    }

    private class LogicComboActionListener implements ActionListener
    {
	public  LogicComboActionListener(final ExprData e, final int b)
	{
	    edata = e;
	    box = b;
	}
	public void actionPerformed(ActionEvent e) 
	{
	    logic[box] = ((JComboBox)e.getSource()).getSelectedIndex();
	    edata.generateDataUpdate(ExprData.VisibilityChanged);
	}
	private ExprData edata;
	private int box;
    }

    // handles the 'Name' text fields buttons in the Set panel
    //
    class PointChangeListener implements DocumentListener 
    {
	public PointChangeListener(final ExprData e, int which_box) 
	{ 
	    super();
	    edata = e;
	    box = which_box;
	}

	public void insertUpdate(DocumentEvent e)  { propagate(e); }
	public void removeUpdate(DocumentEvent e)  { propagate(e); }
	public void changedUpdate(DocumentEvent e) { propagate(e); }

	private void propagate(DocumentEvent e)
	{
	    // update the relevant string
	    try
	    { 
		point[box] = NumberParser.tokenToDouble(point_jtf[box].getText());
		edata.generateDataUpdate(ExprData.VisibilityChanged);
	    }
	    catch (TokenIsNotNumber tinn)
	    {
		//System.out.println("wierd string....\n");
	    }
	}
	
	private ExprData edata;
	private int box;
    }

    // =============================================
    //
    // internals
    //
    // =============================================

  
    public double min, max;
 
    public int boxes;

    public String name;

    Color[] colour;
    double[] point;
    int[] logic;

    JTextField[] point_jtf = null;
    JComboBox[] logic_jcb = null;

    private void initBoxes()
    {
	colour = new Color[boxes];
	point = new double[boxes];
	logic = new int[boxes];
	
	for(int b=0; b < boxes; b++)
	{
	    colour[b] = Color.white;
	    point[b] = .0;
	    logic[b] = 0;
	}
    }

    private void insertPoint(int p)
    {

    }

    private void deletePoint(int p)
    {

    }
}
