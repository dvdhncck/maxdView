import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

import java.io.*;

import java.awt.font.*;
import java.awt.geom.*;

public class DecorationManager
{
    public final String[] font_family_names = { "Helvetica", "Courier", "Times-Roman" };

    public DecorationManager(maxdView mview_, String prop_name_)
    {
	mview = mview_;
	prop_name = prop_name_;
	dplot = mview.getDataPlot();

	listeners = new Vector();

	decs = new  Decoration[0];

	default_font = mview.getDataPlot().getFont();

	left_ii = new ImageIcon(mview.getImageDirectory() + "moveleft.gif");
	right_ii = new ImageIcon(mview.getImageDirectory() + "moveright.gif");
	up_ii = new ImageIcon(mview.getImageDirectory() + "moveup.gif");
	down_ii = new ImageIcon(mview.getImageDirectory() + "movedown.gif");
	
	big_left_ii = new ImageIcon(mview.getImageDirectory() + "movebigleft.gif");
	big_right_ii = new ImageIcon(mview.getImageDirectory() + "movebigright.gif");
	big_up_ii = new ImageIcon(mview.getImageDirectory() + "movebigup.gif");
	big_down_ii = new ImageIcon(mview.getImageDirectory() + "movebigdown.gif");
    }

    public final static int LABEL     = 0;
    public final static int LEGEND    = 1;
    public final static int ARROW     = 2;
    public final static int IMAGE     = 3;
    public final static int RECTANGLE = 4;

    public final String[] deco_type_name = { "Label", "Legend", "Arrow", "Image", "Rectangle" };
    public final String[] align_mode_names  = { "Center", "North", "South", "East", "West" };
    public final String[] orient_mode_names = { "Horizontal", "Downwards", "Upwards" };

    protected int        n_decs;
    protected Decoration[] decs;
    protected Vector listeners;

    private void notifyListeners()
    {
	for(int l=0; l <listeners.size(); l++)
	    ((DecoListener) listeners.elementAt(l)).decosChanged();
    }

    public interface DecoListener
    {
	public void decosChanged();
    }

    public void addDecoListener(DecoListener al)
    {
	listeners.addElement(al);
    }

    public void setNumDecos(int n_decs)
    {
	decs = new Decoration[n_decs];

	// assign default values...

	for(int d=0; d < n_decs; d++)
	{
	    decs[d] = new Decoration(mview, LABEL);

	    decs[d].d_loc = new Point( 50, 50 );

	    decs[d].d_col  = mview.getDataPlot().getTextColour();
	    decs[d].d_back = mview.getDataPlot().getBackgroundColour();
	    
	    decs[d].d_font_fam  = fontToFamily(default_font);
	    decs[d].d_font_sty  = fontToStyle(default_font);
	    decs[d].d_font_size = default_font.getSize();
	    
	    updateFont(d);

	}
    }

    public void setDecoType(int d, int t)          { decs[d].d_type = t; }
    public void setDecoText(int d, String t)       { decs[d].d_text = t; }
    public void setDecoLocation(int d, Point l)    { decs[d].d_loc = l; }
    public void setDecoOrientation(int d, int o)   { decs[d].d_orient = o; }
    public void setDecoAlign(int d, int a)         { decs[d].d_align = a; }
    public void setDecoColour(int d, Color c)      { decs[d].d_col = c; }
    public void setDecoBackground(int d, Color c)  { decs[d].d_back = c; }
    public void setDecoFont(int d, Font f)         { decs[d].d_font = f; }

    // =================================================================================================
    // the editor
    // =================================================================================================

    private JFrame editor_frame = null;

    public void startEditor()
    {
	//System.out.println("startEditor()...");

	if(editor_frame == null)
	    makeEditor();

	editor_frame.setVisible(true);
    }

    private void makeEditor()
    {
	editor_frame  = new JFrame("Decorations");
	GridBagLayout gridbag = new GridBagLayout();
	GridBagConstraints c = null;
	JPanel panel = new JPanel();
	panel.setLayout(gridbag);
	
	panel.setPreferredSize(new Dimension(550, 400));

	// ================================
	
	JPanel dec_panel = new JPanel();
	GridBagLayout dec_gridbag = new GridBagLayout();
	dec_panel.setLayout(dec_gridbag);

	dec_list = new JList();
	JScrollPane jsp = new JScrollPane(dec_list);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	c.weighty = 2.0;
	c.weightx = 1.0;
	c.gridwidth = 2;
	c.fill = GridBagConstraints.BOTH;
	dec_gridbag.setConstraints(jsp, c);
	dec_panel.add(jsp);  
	dec_list.addListSelectionListener(new ListSelectionListener() 
	    {
		public void valueChanged(ListSelectionEvent e) 
		{
		    populateAttsPanel( dec_list.getSelectedIndex() );
		}
	    });
	
	Font small_font = mview.getSmallFont();
	Insets ins = new Insets(1,3,1,3);

	JButton add_jb = new JButton("Add");
	add_jb.setFont(small_font);
	add_jb.setMargin(ins);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 1;
	c.weightx = 1.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	dec_gridbag.setConstraints(add_jb, c);
	dec_panel.add(add_jb);
	add_jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    addDeco();
		}
	    });

	JButton del_jb = new JButton("Delete");
	del_jb.setFont(small_font);
	del_jb.setMargin(ins);
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 1;
	c.weightx = 1.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	dec_gridbag.setConstraints(del_jb, c);
	dec_panel.add(del_jb);
	del_jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    removeDeco();
		}
	    });

	JButton cln_jb = new JButton("Clone");
	cln_jb.setFont(small_font);
	cln_jb.setMargin(ins);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 2;
	c.weightx = 1.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	dec_gridbag.setConstraints(cln_jb, c);
	dec_panel.add(cln_jb);
	cln_jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    cloneDeco();
		}
	    });
	
	JButton raise_jb = new JButton("Raise");
	raise_jb.setFont(small_font);
	raise_jb.setMargin(ins);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 3;
	c.weightx = 1.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	dec_gridbag.setConstraints(raise_jb, c);
	dec_panel.add(raise_jb);
	raise_jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    raiseDeco();
		}
	    });
	
	JButton lower_jb = new JButton("Lower");
	lower_jb.setFont(small_font);
	lower_jb.setMargin(ins);
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 3;
	c.weightx = 1.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	dec_gridbag.setConstraints(lower_jb, c);
	dec_panel.add(lower_jb);
	lower_jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    lowerDeco();
		}
	    });

	// ------------

	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	c.weightx = 1.0;
	c.weighty = 1.0;
	//c.gridheight = 2;
	c.fill = GridBagConstraints.BOTH;
	gridbag.setConstraints(dec_panel, c);
	panel.add(dec_panel);  

	// ================================

	// ================================
	
	atts_panel = new JPanel();
	atts_panel.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 0;
	c.weightx = 2.0;
	c.weighty = 1.0;
	c.fill = GridBagConstraints.BOTH;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(atts_panel, c);
	panel.add(atts_panel);  

	// ================================

	populateDecoList();
	dec_list.setSelectedIndex(0);

	dec_list.addMouseListener(new MouseAdapter() 
	    {
		public void mouseClicked(MouseEvent e) 
		{
		    if(e.getClickCount() == 2)
		    {
			// 
			selectAllSimilar();
		    }
		}
	    });
	// ================================

	JPanel but_panel = new JPanel();
	but_panel.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
	GridBagLayout but_gridbag = new GridBagLayout();
	but_panel.setLayout(but_gridbag);
	
	JButton save_jb = new JButton("Save");
	c = new GridBagConstraints();
	c.gridx = 0;
	//c.weightx = 1.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	but_gridbag.setConstraints(save_jb, c);
	but_panel.add(save_jb);
	save_jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    saveDecos();
		}
	    });
	

	JButton load_jb = new JButton("Load");
	c = new GridBagConstraints();
	c.gridx = 1;
	//c.weightx = 1.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	but_gridbag.setConstraints(load_jb, c);
	but_panel.add(load_jb);
        load_jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    loadDecos();
		}
	    });
	
	JButton help_jb = new JButton("Help");
	c = new GridBagConstraints();
	c.gridx = 2;
	//c.weightx = 1.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	but_gridbag.setConstraints(help_jb, c);
	but_panel.add(help_jb);
        help_jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		   mview.getHelpTopic("Decorations");
		}
	    });

	JButton close_jb = new JButton("Close");
	c = new GridBagConstraints();
	c.gridx = 3;
	//c.weightx = 1.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	but_gridbag.setConstraints(close_jb, c);
	but_panel.add(close_jb);
        close_jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    editor_frame.setVisible(false);
		}
	    });

	// ---------

	c = new GridBagConstraints();
	c.gridy = 2;
	c.weightx = 1.0;
	//c.weighty = 1.0;
	c.gridwidth = 2;
	c.fill = GridBagConstraints.BOTH;
	gridbag.setConstraints(but_panel, c);
	panel.add(but_panel);  



	// ================================
	

	editor_frame.getContentPane().add(panel);
	editor_frame.pack();
    }

    private void populateDecoList()
    {
	Vector names = new Vector();
	for(int d=0; d < decs.length; d++)
	    names.addElement( deco_type_name[ decs[d].d_type ] );

	dec_list.setListData(names);

	//System.out.println("populateDecoList() " + names.size()  + " decos");
    }

    private void populateAttsPanel(final int deco_id)
    {
	atts_panel.removeAll();

	if((deco_id < 0) || (deco_id >= decs.length))
	{
	    atts_panel.updateUI();
	    return;
	}

	GridBagLayout gridbag = new GridBagLayout();
	GridBagConstraints c = null;
	JLabel label = null;
	atts_panel.setLayout(gridbag);

	final int deco_type = decs[deco_id].d_type;

	int line = 0;

	{
	    JPanel loc_panel = new JPanel();
	    GridBagLayout loc_gridbag = new GridBagLayout();
	    loc_panel.setLayout(loc_gridbag);
	    
	    JButton big_up_jb = new JButton(big_up_ii);
	    big_up_jb.setPreferredSize(butt_size);
	    big_up_jb.addActionListener(new NudgeActionListener(0, 10, 0, -1));
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    loc_gridbag.setConstraints(big_up_jb, c);
	    loc_panel.add(big_up_jb);
	    
	    JButton big_down_jb = new JButton(big_down_ii);
	    big_down_jb.setPreferredSize(butt_size);
	    big_down_jb.addActionListener(new NudgeActionListener(0, 10, 0, 1));
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 2;
	    loc_gridbag.setConstraints(big_down_jb, c);
	    loc_panel.add(big_down_jb);
	    
	    JButton big_left_jb = new JButton(big_left_ii);
	    big_left_jb.setPreferredSize(butt_size);
	    big_left_jb.addActionListener(new NudgeActionListener(0, 10, -1, 0));
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    loc_gridbag.setConstraints(big_left_jb, c);
	    loc_panel.add(big_left_jb);
	    
	    JButton big_right_jb = new JButton(big_right_ii);
	    big_right_jb.setPreferredSize(butt_size);
	    big_right_jb.addActionListener(new NudgeActionListener(0, 10, 1, 0));
	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = 1;
	    loc_gridbag.setConstraints(big_right_jb, c);
	    loc_panel.add(big_right_jb);
	    
	    // ------
	    
	    JLabel dummy = new JLabel();
	    dummy.setPreferredSize(butt_size);
	    c = new GridBagConstraints();
	    c.gridx = 3;
	    c.gridy = 1;
	    loc_gridbag.setConstraints(dummy, c);
	    loc_panel.add(dummy);
	    
	    // ------
	    
	    JButton up_jb = new JButton(up_ii);
	    up_jb.setPreferredSize(butt_size);
	    up_jb.addActionListener(new NudgeActionListener(0, 1, 0, -1));
	    c = new GridBagConstraints();
	    c.gridx = 5;
	    c.gridy = 0;
	    loc_gridbag.setConstraints(up_jb, c);
	    loc_panel.add(up_jb);
	    
	    JButton down_jb = new JButton(down_ii);
	    down_jb.setPreferredSize(butt_size);
	    down_jb.addActionListener(new NudgeActionListener(0, 1, 0, 1));
	    c = new GridBagConstraints();
	    c.gridx = 5;
	    c.gridy = 2;
	    loc_gridbag.setConstraints(down_jb, c);
	    loc_panel.add(down_jb);
	    
	    JButton left_jb = new JButton(left_ii);
	    left_jb.setPreferredSize(butt_size);
	    left_jb.addActionListener(new NudgeActionListener(0, 1, -1, 0));
	    c = new GridBagConstraints();
	    c.gridx = 4;
	    c.gridy = 1;
	    loc_gridbag.setConstraints(left_jb, c);
	    loc_panel.add(left_jb);
	    
	    JButton right_jb = new JButton(right_ii);
	    right_jb.setPreferredSize(butt_size);
	    right_jb.addActionListener(new NudgeActionListener(0, 1, 1, 0));
	    c = new GridBagConstraints();
	    c.gridx = 6;
	    c.gridy = 1;
	    loc_gridbag.setConstraints(right_jb, c);
	    loc_panel.add(right_jb);
	    
	    // ------
	    
	    JPanel loc_t_panel = new JPanel();
	    GridBagLayout loc_t_gridbag = new GridBagLayout();
	    loc_t_panel.setLayout(loc_t_gridbag);
	    
	    label = new JLabel("   x ");
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    loc_t_gridbag.setConstraints(label, c);
	    loc_t_panel.add(label);

	    x_jtf = new JTextField(5);
	    x_jtf.setText(String.valueOf( decs[deco_id].d_loc.x ));
	    x_jtf.addActionListener(new CustomActionListener(20));
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    loc_t_gridbag.setConstraints(x_jtf, c);
	    loc_t_panel.add(x_jtf);

	    label = new JLabel("   y ");
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    loc_t_gridbag.setConstraints(label, c);
	    loc_t_panel.add(label);
	    
	    y_jtf = new JTextField(5);
	    y_jtf.setText(String.valueOf( decs[deco_id].d_loc.y ));
	    y_jtf.addActionListener(new CustomActionListener(21));
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 1;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    loc_t_gridbag.setConstraints(y_jtf, c);
	    loc_t_panel.add(y_jtf);

	    // ------

	    c = new GridBagConstraints();
	    c.gridx = 7;
	    c.gridy = 0;
	    c.gridheight = 3;
	    c.weightx = 1.0;
	    //c.gridwidth = 2;
	    //c.weighty = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.BOTH;
	    loc_gridbag.setConstraints(loc_t_panel, c);
	    loc_panel.add(loc_t_panel);  

	    /*
	    dummy = new JLabel();
	    dummy.setPreferredSize(butt_size);
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 3;
	    loc_gridbag.setConstraints(dummy, c);
	    loc_panel.add(dummy);
	    */

	    // ------------
	    
	    label = new JLabel("Location ");
	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.EAST;
	    c.gridy = line;
	    gridbag.setConstraints(label, c);
	    atts_panel.add(label);
	    
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.weightx = 2.0;
	    //c.gridwidth = 2;
	    c.weighty = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(loc_panel, c);
	    atts_panel.add(loc_panel);  
	    
	    line++;
	}

	if(deco_type == LABEL)
	{
	    label = new JLabel("Text ");
	    c = new GridBagConstraints();
	    c.gridy = line;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	    atts_panel.add(label);

	    JTextField text_jtf = new JTextField(20);
	    text_jtf.setText( decs[deco_id].d_text );
	    text_jtf.addActionListener(new CustomActionListener(0));
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.gridwidth = 2;
	    c.weighty = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(text_jtf, c);
	    atts_panel.add(text_jtf);

	    line++;

	    label = new JLabel("Font ");
	    c = new GridBagConstraints();
	    c.gridy = line;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	    atts_panel.add(label);

	    JComboBox font_family_jcb = new JComboBox(font_family_names);
	    font_family_jcb.setSelectedIndex(fontToFamily(decs[deco_id].d_font));
	    font_family_jcb.addActionListener(new CustomActionListener(1));
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.weighty = 0.75;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(font_family_jcb, c);
	    atts_panel.add(font_family_jcb);

	    JComboBox font_style_jcb = new JComboBox(dplot.font_style_names);
	    font_style_jcb.setSelectedIndex(fontToStyle(decs[deco_id].d_font));
	    font_style_jcb.addActionListener(new CustomActionListener(2));
	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = line;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(font_style_jcb, c);
	    atts_panel.add(font_style_jcb);

	    line++;

	    JSlider font_size_js = new JSlider(JSlider.HORIZONTAL, 4, 72, 8);
	    font_size_js.setValue(decs[deco_id].d_font_size);
	    font_size_js.addChangeListener(new CustomChangeListener(0));
	    font_size_js.setPaintTicks(true);
	    font_size_js.setMajorTickSpacing(4);
	    //font_family_jcb.setSelectedIndex();
	    
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.gridwidth = 2;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(font_size_js, c);
	    atts_panel.add(font_size_js);

	    line++;

	    /*
	    label = new JLabel("Align ");
	    c = new GridBagConstraints();
	    c.gridy = line;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	    atts_panel.add(label);
	    
	    JComboBox align_jcb = new JComboBox(align_mode_names);
	    align_jcb.setSelectedIndex( decs[deco_id].d_align );
	    align_jcb.addActionListener(new CustomActionListener(4));
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(align_jcb, c);
	    atts_panel.add(align_jcb);
	    */

	    line++;
	    
	    label = new JLabel("Orient ");
	    c = new GridBagConstraints();
	    c.gridy = line;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	    atts_panel.add(label);
	    
	    JComboBox orient_jcb = new JComboBox(orient_mode_names);
	    orient_jcb.setSelectedIndex( decs[deco_id].d_orient );
	    orient_jcb.addActionListener(new CustomActionListener(5));
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.anchor = GridBagConstraints.WEST;
	    c.weighty = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(orient_jcb, c);
	    atts_panel.add(orient_jcb);

	    line++;
	    
	}

	if(deco_type == ARROW)
	{
	    label = new JLabel("Direction ");
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	    atts_panel.add(label);
	  
	    JSlider line_dir_js = new JSlider(JSlider.HORIZONTAL, -180, 180, 0);
	    line_dir_js.setValue((int) decs[deco_id].d_line_dir);
	    line_dir_js.addChangeListener(new CustomChangeListener(1));
	    line_dir_js.setPaintTicks(true);
	    line_dir_js.setMajorTickSpacing(45);
	    //font_family_jcb.setSelectedIndex();
	    
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.weighty = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(line_dir_js, c);
	    atts_panel.add(line_dir_js);

	    line++;

	    // ------
	    
	    label = new JLabel("Length ");
	    c = new GridBagConstraints();
	    c.gridy = line;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	    atts_panel.add(label);
	  
	    JSlider line_len_js = new JSlider(JSlider.HORIZONTAL, 0, 100, 10);
	    line_len_js.setValue((int) decs[deco_id].d_arrow_len);
	    line_len_js.addChangeListener(new CustomChangeListener(2));
	    line_len_js.setPaintTicks(true);
	    line_len_js.setMajorTickSpacing(4);
	    //font_family_jcb.setSelectedIndex();
	    
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.weighty = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(line_len_js, c);
	    atts_panel.add(line_len_js);

	    line++;

	    // ------

	    label = new JLabel("Thickness ");
	    c = new GridBagConstraints();
	    c.gridy = line;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	    atts_panel.add(label);
	  
	    JSlider arrow_width_js = new JSlider(JSlider.HORIZONTAL, 0, 100, 1);
	    arrow_width_js.setValue((int) (decs[deco_id].d_arrow_w * 10));
	    arrow_width_js.addChangeListener(new CustomChangeListener(3));
	    arrow_width_js.setPaintTicks(true);
	    arrow_width_js.setMajorTickSpacing(5);
	    //font_family_jcb.setSelectedIndex();
	    
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.weighty = 0.5;
	    c.gridy = line;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(arrow_width_js, c);
	    atts_panel.add(arrow_width_js);

	    line++;

	    // ------

	    label = new JLabel("Head width ");
	    c = new GridBagConstraints();
	    c.gridy = line;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	    atts_panel.add(label);
	  
	    JSlider arh_width_js = new JSlider(JSlider.HORIZONTAL, 1, 50, 10);
	    arh_width_js.setValue((int) (decs[deco_id].d_arrow_head_w * 5.0));
	    arh_width_js.addChangeListener(new CustomChangeListener(4));
	    arh_width_js.setPaintTicks(true);
	    arh_width_js.setMajorTickSpacing(5);
	    //font_family_jcb.setSelectedIndex();
	    
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.weighty = 0.5;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(arh_width_js, c);
	    atts_panel.add(arh_width_js);

	    line++;

	    // ------

	    label = new JLabel("Head length ");
	    c = new GridBagConstraints();
	    c.gridy = line;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	    atts_panel.add(label);
	  
	    JSlider arh_length_js = new JSlider(JSlider.HORIZONTAL, 1, 50, 10);
	    arh_length_js.setValue((int) (decs[deco_id].d_arrow_head_l * 5.0));
	    arh_length_js.addChangeListener(new CustomChangeListener(5));
	    arh_length_js.setPaintTicks(true);
	    arh_length_js.setMajorTickSpacing(5);
	    //font_family_jcb.setSelectedIndex();
	    
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.weighty = 0.5;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(arh_length_js, c);
	    atts_panel.add(arh_length_js);

	    line++;
	}

	if(deco_type == IMAGE)
	{
	    label = new JLabel("File ");
	    c = new GridBagConstraints();
	    c.gridy = line;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	    atts_panel.add(label);

	    JTextField img_jtf = new JTextField(20);
	    img_jtf.setText( decs[deco_id].d_img_fn );
	    img_jtf.addActionListener(new CustomActionListener(40));
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.gridwidth = 2;
	    c.weighty = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(img_jtf, c);
	    atts_panel.add(img_jtf);

	    line++;

	    label = new JLabel("Width ");
	    c = new GridBagConstraints();
	    c.gridy = line;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	    atts_panel.add(label);
	  
	    JSlider img_width_js = new JSlider(JSlider.HORIZONTAL, 0, 100, 20);
	    img_width_js.setValue((int) decs[deco_id].d_img_w);
	    img_width_js.addChangeListener(new CustomChangeListener(6));
	    img_width_js.setPaintTicks(true);
	    img_width_js.setMajorTickSpacing(4);
	    //font_family_jcb.setSelectedIndex();
	    
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.weighty = 1.0;
	    c.gridwidth = 2;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(img_width_js, c);
	    atts_panel.add(img_width_js);

	    line++;

	    label = new JLabel("Height ");
	    c = new GridBagConstraints();
	    c.gridy = line;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	    atts_panel.add(label);
	  
	    JSlider img_height_js = new JSlider(JSlider.HORIZONTAL, 0, 100, 10);
	    img_height_js.setValue((int) decs[deco_id].d_img_h);
	    img_height_js.addChangeListener(new CustomChangeListener(7));
	    img_height_js.setPaintTicks(true);
	    img_height_js.setMajorTickSpacing(4);
	    //font_family_jcb.setSelectedIndex();
	    
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.gridwidth = 2;
	    c.weighty = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(img_height_js, c);
	    atts_panel.add(img_height_js);

	    line++;

	}

	if(deco_type == LEGEND)
	{
	    label = new JLabel("Colouriser ");
	    c = new GridBagConstraints();
	    c.gridy = line;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	    atts_panel.add(label);

	    JComboBox col_jcb = new JComboBox(dplot.getColouriserNameArray());
	    if(decs[deco_id].d_colouriser != null)
		col_jcb.setSelectedItem(decs[deco_id].d_colouriser.getName());
	    col_jcb.addActionListener(new CustomActionListener(30));
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.weighty = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(col_jcb, c);
	    atts_panel.add(col_jcb);

	    line++;

	    label = new JLabel("Font ");
	    c = new GridBagConstraints();
	    c.gridy = line;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	    atts_panel.add(label);

	    JComboBox font_family_jcb = new JComboBox(font_family_names);
	    font_family_jcb.setSelectedIndex(fontToFamily(decs[deco_id].d_font));
	    font_family_jcb.addActionListener(new CustomActionListener(1));
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.weighty = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(font_family_jcb, c);
	    atts_panel.add(font_family_jcb);

	    JComboBox font_style_jcb = new JComboBox(dplot.font_style_names);
	    font_style_jcb.setSelectedIndex(fontToStyle(decs[deco_id].d_font));
	    font_style_jcb.addActionListener(new CustomActionListener(2));
	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = line;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(font_style_jcb, c);
	    atts_panel.add(font_style_jcb);

	    line++;

	    JSlider font_size_js = new JSlider(JSlider.HORIZONTAL, 4, 72, 8);
	    font_size_js.setValue(decs[deco_id].d_font_size);
	    font_size_js.addChangeListener(new CustomChangeListener(0));
	    font_size_js.setPaintTicks(true);
	    font_size_js.setMajorTickSpacing(4);
	    //font_family_jcb.setSelectedIndex();
	    
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.gridwidth = 2;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(font_size_js, c);
	    atts_panel.add(font_size_js);

	    line++;

	}
	    // ------------

	if((deco_type == LEGEND) || (deco_type == RECTANGLE))
	{
	    label = new JLabel("Width ");
	    c = new GridBagConstraints();
	    c.gridy = line;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	    atts_panel.add(label);
	  
	    JSlider leg_width_js = new JSlider(JSlider.HORIZONTAL, 0, 100, 20);
	    leg_width_js.setValue((int) decs[deco_id].d_leg_w);
	    leg_width_js.addChangeListener(new CustomChangeListener(8));
	    leg_width_js.setPaintTicks(true);
	    leg_width_js.setMajorTickSpacing(10);
	    //font_family_jcb.setSelectedIndex();
	    
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.weighty = 1.0;
	    c.gridwidth = 2;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(leg_width_js, c);
	    atts_panel.add(leg_width_js);

	    line++;

	    label = new JLabel("Height ");
	    c = new GridBagConstraints();
	    c.gridy = line;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	    atts_panel.add(label);
	  
	    JSlider leg_height_js = new JSlider(JSlider.HORIZONTAL, 0, 100, 10);
	    leg_height_js.setValue((int) decs[deco_id].d_leg_h);
	    leg_height_js.addChangeListener(new CustomChangeListener(9));
	    leg_height_js.setPaintTicks(true);
	    leg_height_js.setMajorTickSpacing(10);
	    //font_family_jcb.setSelectedIndex();
	    
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.gridwidth = 2;
	    c.weighty = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(leg_height_js, c);
	    atts_panel.add(leg_height_js);

	    line++;

	    if(deco_type == LEGEND)
	    {
		label = new JLabel("Orient ");
		c = new GridBagConstraints();
		c.gridy = line;
		c.anchor = GridBagConstraints.EAST;
		gridbag.setConstraints(label, c);
		atts_panel.add(label);
		
		JComboBox orient_jcb = new JComboBox(orient_mode_names);
		orient_jcb.setSelectedIndex( decs[deco_id].d_leg_orient );
		orient_jcb.addActionListener(new CustomActionListener(10));
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line;
		c.weighty = 1.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(orient_jcb, c);
		atts_panel.add(orient_jcb);
		
		line++;
	    }
	}

	if((deco_type != LEGEND) && (deco_type != IMAGE))
	{
	    label = new JLabel("F'ground ");
	    c = new GridBagConstraints();
	    c.gridy = line;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	    atts_panel.add(label);
	
	    Dimension bdim = new Dimension(50, 25);

	    JButton col_jb = new JButton();
	    col_jb.setMinimumSize(bdim);
	    col_jb.setPreferredSize(bdim);
	    col_jb.setBackground(decs[deco_id].d_col);
	    col_jb.addActionListener(new CustomActionListener(6));
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.weighty = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(col_jb, c);
	    atts_panel.add(col_jb);

	    line++;
	    
	    if((deco_type == ARROW) || (deco_type == RECTANGLE))
	    {
		JCheckBox jchkb = new JCheckBox("Fill");
		jchkb.setSelected(decs[deco_id].d_fill_mode == 1);
		jchkb.addActionListener(new CustomActionListener(50));
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line;
		//c.weighty = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(jchkb, c);
		atts_panel.add(jchkb);
		line++;
	    }

	    if((deco_type != ARROW) && (deco_type != RECTANGLE))
	    {
		label = new JLabel("B'ground ");
		c = new GridBagConstraints();
		c.gridy = line;
		c.anchor = GridBagConstraints.EAST;
		gridbag.setConstraints(label, c);
		atts_panel.add(label);
		
		JButton bak_jb = new JButton();
		bak_jb.setMinimumSize(bdim);
		bak_jb.setPreferredSize(bdim);
		bak_jb.setBackground(decs[deco_id].d_back);
		bak_jb.addActionListener(new CustomActionListener(7));
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weighty = 0.5;
		gridbag.setConstraints(bak_jb, c);
		atts_panel.add(bak_jb);

		line++;

		if(deco_type == LABEL)
		{
		    JCheckBox jchkb = new JCheckBox("Opaque");
		    jchkb.setSelected(decs[deco_id].d_back_mode == 1);
		    jchkb.addActionListener(new CustomActionListener(51));
		    c = new GridBagConstraints();
		    c.gridx = 1;
		    c.gridy = line;
		    //c.weighty = 1.0;
		    c.anchor = GridBagConstraints.NORTHWEST;
		    c.fill = GridBagConstraints.HORIZONTAL;
		    gridbag.setConstraints(jchkb, c);
		    atts_panel.add(jchkb);
		    
		    line++;
		}
	    }
	    else
	    {
		label = new JLabel("Line width ");
		c = new GridBagConstraints();
		c.gridy = line;
		c.anchor = GridBagConstraints.EAST;
		gridbag.setConstraints(label, c);
		atts_panel.add(label);
		
		JSlider line_width_js = new JSlider(JSlider.HORIZONTAL, 0, 100, 10);
		line_width_js.setValue((int) (decs[deco_id].d_line_w * (float) 10.0));
		line_width_js.addChangeListener(new CustomChangeListener(10));
		line_width_js.setPaintTicks(true);
		line_width_js.setMajorTickSpacing(4);
		//font_family_jcb.setSelectedIndex();
		
		c = new GridBagConstraints();
		c.gridx = 1;
		c.weighty = 0.5;
		c.gridy = line;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(line_width_js, c);
		atts_panel.add(line_width_js);
		
		line++;
		
		// ------

		label = new JLabel("Outline ");
		c = new GridBagConstraints();
		c.gridy = line;
		c.anchor = GridBagConstraints.EAST;
		gridbag.setConstraints(label, c);
		atts_panel.add(label);
		
		JButton outl_jb = new JButton();
		outl_jb.setMinimumSize(bdim);
		outl_jb.setPreferredSize(bdim);
		outl_jb.setBackground(decs[deco_id].d_outline_col);
		outl_jb.addActionListener(new CustomActionListener(8));
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line;
		c.anchor = GridBagConstraints.WEST;
		c.weighty = 0.5;
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(outl_jb, c);
		atts_panel.add(outl_jb);

		line++;
	    }
	}
	
	atts_panel.updateUI();
    }
    
    private int fontToFamily(Font f)
    {
	String name = f.getName();
	if(name.equals("Courier"))
	    return 1;
	if(name.equals("Times"))
	    return 2;
	return 0;
    }
 
    private int fontToStyle(Font f)
    {
	if(f.isBold())
	    return 1;
	if(f.isItalic())
	    return 2;
	return 0;
    }

    private int fontStyle(int s)
    {
	if(s == 1)
	    return Font.BOLD;
	if(s == 2)
	    return Font.ITALIC;
	return Font.PLAIN;
    }
     
    private void updateFont(int d)
    {
	// System.out.println("Font: " + decs[d].d_font_fam + "." + decs[d].d_font_sty + "." + decs[d].d_font_size);

	decs[d].d_font = new Font( font_family_names[ decs[d].d_font_fam ], fontStyle(  decs[d].d_font_sty ), decs[d].d_font_size );
    }

    private void updateImage(int deco_id)
    {
	if(decs[deco_id].d_type == IMAGE)
	{
	    decs[deco_id].d_img_i = Toolkit.getDefaultToolkit().getImage(decs[deco_id].d_img_fn);
	    MediaTracker tracker = new MediaTracker(editor_frame);
	    tracker.addImage(decs[deco_id].d_img_i, 0);
	    try 
	    {
		tracker.waitForAll();
	    } 
	    catch (InterruptedException ie) 
	    {
	    }
	}
    }

    private class NudgeActionListener implements ActionListener
    {
	int m, d,x,y;
	public NudgeActionListener(int m_, int d_, int x_, int y_)
	{
	    m = m_; d = d_; x = x_; y = y_;
	}
	public void actionPerformed(ActionEvent e) 
	{
	    for(int deco_id=dec_list.getMinSelectionIndex(); deco_id <= dec_list.getMaxSelectionIndex(); deco_id++)
	    {
		if(dec_list.isSelectedIndex(deco_id))
		{
		    
		    decs[deco_id].d_loc.x += (x * d);
		    decs[deco_id].d_loc.y += (y * d);
		    
		    x_jtf.setText(String.valueOf(decs[deco_id].d_loc.x));
		    y_jtf.setText(String.valueOf(decs[deco_id].d_loc.y));
		    
		}
	    }
	    notifyListeners();
	}
    }

    private class CustomChangeListener implements ChangeListener
    {
	private int code;
	public CustomChangeListener(int code_)
	{
	    code = code_;
	}
	public void stateChanged(ChangeEvent e)
	{
	    JSlider source = (JSlider)e.getSource();
	    if(!source.getValueIsAdjusting())
	    {
		for(int deco_id=dec_list.getMinSelectionIndex(); deco_id <= dec_list.getMaxSelectionIndex(); deco_id++)
		{
		    if(dec_list.isSelectedIndex(deco_id))
		    {
			switch(code)
			{
			case 0: // font size
			    decs[deco_id].d_font_size = (int) source.getValue();
			    updateFont(deco_id);
			    break;
			case 1: // arrow direction
			    decs[deco_id].d_line_dir = (float) source.getValue();
			    break;
			case 2: // arrow length
			    decs[deco_id].d_arrow_len = (float) source.getValue();
			    break;
			case 3: // arrow width
			    decs[deco_id].d_arrow_w = (float) source.getValue() * (float) 0.1;
			    break;
			case 4: // arrow head width
			    decs[deco_id].d_arrow_head_w = (float) source.getValue() * (float) 0.2;
			    break;
			case 5: // arrow head length
			    decs[deco_id].d_arrow_head_l = (float) source.getValue() * (float) 0.2;
			    break;
			case 6: // image w
			    decs[deco_id].d_img_w = source.getValue();
			    break;
			case 7: // image h
			    decs[deco_id].d_img_h = source.getValue();
			    break;
			case 8: // legend w
			    decs[deco_id].d_leg_w = source.getValue();
			    break;
			case 9: // legend h
			    decs[deco_id].d_leg_h = source.getValue();
			    break;
			case 10: // line w
			    decs[deco_id].d_line_w = ((float) source.getValue()) * (float) 0.1;

			}
		    }
		}
		notifyListeners();
	    }
	}
    }

    private class CustomActionListener implements ActionListener
    {
	public CustomActionListener(int code_)
	{
	    code = code_;
	}
	public void actionPerformed(ActionEvent e) 
	{
	    Color new_c = null;
	    
	    for(int deco_id=dec_list.getMinSelectionIndex(); deco_id <= dec_list.getMaxSelectionIndex(); deco_id++)
	    {
		if(dec_list.isSelectedIndex(deco_id))
		{
		    if((code >= 6) && (code <= 8))
		    {
			if(new_c == null)
			    new_c = mview.getColourChooser().showDialog(editor_frame,
									"Choose Background Colour",
									decs[deco_id].d_back);
		    }

		    switch(code)
		    {
		    case 0: // text
			{
			    JTextField source = (JTextField)e.getSource();
			    decs[deco_id].d_text = source.getText();
			    break;
			}
		    case 1: // family
			{
			    JComboBox source = (JComboBox)e.getSource();
			    decs[deco_id].d_font_fam = source.getSelectedIndex();
			    updateFont(deco_id);
			    break;
			}
		    case 2: // style
			{
			    JComboBox source = (JComboBox)e.getSource();
			    decs[deco_id].d_font_sty = source.getSelectedIndex();
			    updateFont(deco_id);
			    break;
			}
		    case 3: // size
			{
			    //JSlider source = (JSlider)e.getSource();
			    //d_font_size[ deco_id ] = (int) source.getValue();
			    //updateFont(deco_id);
			    break;
			}
			
		    case 4: // align
			{
			    JComboBox source = (JComboBox)e.getSource();
			    decs[deco_id].d_align = (int) source.getSelectedIndex();
			    break;
			}
			
		    case 5: // orient
			{
			    JComboBox source = (JComboBox)e.getSource();
			    decs[deco_id].d_orient = (int) source.getSelectedIndex();
			    break;
			}
			
		    case 6: // colour
			{
			    if (new_c != null) 
			    {
				JButton source = (JButton)e.getSource();
				source.setBackground(new_c);
				decs[deco_id].d_col = new_c;
			    }
			    
			    break;
			}
		    case 7: // background
			{
			    if (new_c != null) 
			    {
				JButton source = (JButton)e.getSource();
				source.setBackground(new_c);
				decs[deco_id].d_back = new_c;
			    }
			    
			    break;
			}


		    case 50: // solid f'ground
			decs[deco_id].d_fill_mode = (((JCheckBox)e.getSource()).isSelected()) ? 1 : 0;
			break;
		    case 51: // solid b'ground
			decs[deco_id].d_back_mode = (((JCheckBox)e.getSource()).isSelected()) ? 1 : 0;
			break;

		    case 8: // outline
			{
			    if (new_c != null) 
			    {
				JButton source = (JButton)e.getSource();
				source.setBackground(new_c);
				decs[deco_id].d_outline_col = new_c;
			    }
			    
			    break;
			}
			
		    case 10: // legend orient
			{
			    JComboBox source = (JComboBox)e.getSource();
			    decs[deco_id].d_leg_orient = (int) source.getSelectedIndex();
			    break;
			}
			
		    case 20: // location x (text field)
			{
			    JTextField source = (JTextField)e.getSource();
			    try
			    {
				decs[deco_id].d_loc.x = (int) (new Double(source.getText()).doubleValue());
			    }
			    catch(NumberFormatException nfe)
			    {
			    }
			    break;
			}
		    case 21: // location y (text field)
			{
			    JTextField source = (JTextField)e.getSource();
			    try
			    {
				decs[deco_id].d_loc.y = (int) (new Double(source.getText()).doubleValue());
			    }
			    catch(NumberFormatException nfe)
			    {
			    }
			    break;
			}
			
		    case 30: // colouriser
			{
			    JComboBox source = (JComboBox)e.getSource();
			    decs[deco_id].d_colouriser = dplot.getColouriserByName((String) source.getSelectedItem());
			    break;
			}
			
		    case 40: // image file name
			{
			    JTextField source = (JTextField)e.getSource();
			    decs[deco_id].d_img_fn = source.getText();
			    updateImage(deco_id);
			    break;
			}
		    }
		}
	    }

	    notifyListeners();
	    
	}
	private int code;
    }

    private void selectAllSimilar()
    {
	int ta = dec_list.getSelectedIndex();
	if(ta >= 0)
	{
	    int ta_t = decs[ta].d_type;
	    int mcount = 0;
	    for(int t2 = 0; t2 < decs.length; t2++)
		if(decs[t2].d_type == ta_t)
		    mcount++;
	    int[] sels = new int[mcount];
	    mcount = 0;
	    for(int t2 = 0; t2 < decs.length; t2++)
		if(decs[t2].d_type == ta_t)
		    sels[mcount++] = t2;
	    dec_list.setSelectedIndices(sels);
	}
    }

    // =================================================================================================
    // deco management
    // =================================================================================================

    protected void addDeco()
    {
	try
	{
	    int opt = mview.getChoice("What type of decoration?", deco_type_name);

	    addDeco( opt );
	}
	catch(UserInputCancelled uic)
	{
	}
    }
    
    public void addDeco(int new_d_type)
    {
	Decoration[] new_decs = new Decoration[decs.length+1];
	for(int d=0; d < decs.length; d++)
	    new_decs[d] = decs[d];
	new_decs[decs.length] = new Decoration(mview, new_d_type);
	decs = new_decs;
	populateDecoList();
	dec_list.setSelectedIndex(decs.length-1);
	notifyListeners();
    }

    public void addDeco(Decoration new_dec, boolean update)
    {
	Decoration[] new_decs = new Decoration[decs.length+1];
	for(int d=0; d < decs.length; d++)
	    new_decs[d] = decs[d];
	new_decs[decs.length] = new_dec;
	decs = new_decs;
	if(update)
	{
	    populateDecoList();
	    dec_list.setSelectedIndex(decs.length-1);
	    notifyListeners();
	}
    }

    private void removeDeco()
    {
	int deco_id = dec_list.getSelectedIndex();
	if(deco_id < 0)
	    return;

	Decoration[] new_decs = new Decoration[decs.length-1];
	int d_id = 0;
	for(int d=0; d < decs.length; d++)
	    if(d != deco_id)
		new_decs[d_id++] = decs[d];
	decs = new_decs;
	populateDecoList();
	
	int new_d = deco_id+1;
	if(new_d >= decs.length)
	    new_d = deco_id-1;
	if((new_d >= 0) && (new_d < decs.length))
	    dec_list.setSelectedIndex(new_d);
	
	notifyListeners();
    }

    public void cloneDeco()
    {
	int deco_id = dec_list.getSelectedIndex();
	if(deco_id < 0)
	    return;
	
	Decoration[] new_decs = new Decoration[decs.length+1];
	for(int d=0; d < decs.length; d++)
	    new_decs[d] = decs[d];
	new_decs[decs.length] = decs[deco_id].copyMe();
	decs = new_decs;
	populateDecoList();
	dec_list.setSelectedIndex(decs.length-1);
	notifyListeners();
    }

    public void raiseDeco()
    {
	int deco_id = dec_list.getSelectedIndex();
	if(deco_id < 1)
	    return;
	Decoration tmp = decs[deco_id - 1];
	decs[deco_id - 1] = decs[deco_id];
	decs[deco_id] = tmp;
	populateDecoList();
	dec_list.setSelectedIndex(deco_id-1);
	notifyListeners();
    }

    public void lowerDeco()
    {
	int deco_id = dec_list.getSelectedIndex();
	if(deco_id < 0)
	    return;
	if(deco_id == (decs.length-1))
	    return;
	Decoration tmp = decs[deco_id + 1];
	decs[deco_id + 1] = decs[deco_id];
	decs[deco_id] = tmp;
	populateDecoList();
	dec_list.setSelectedIndex(deco_id+1);
	notifyListeners();
    }

    private void loadDecos()
    {
	if(decs.length > 0)
	{
	    try
	    {
		String[] ostr = { "Relace existing decorations", "Append to existing decorations" };
		int opt = mview.getChoice("Load method", ostr);
		
		JFileChooser jfc = mview.getFileChooser();
		
		String dld = mview.getProperties().getProperty("DecoratedPanel.load_path");
		if(dld != null)
		{
		    File ftmp = new File(dld);
		    jfc.setCurrentDirectory(ftmp);
		}
		
		int ret_val =  jfc.showOpenDialog(editor_frame); 
		if(ret_val == JFileChooser.APPROVE_OPTION) 
		{
		    if(opt == 0)
			decs = new Decoration[0];

		    File file = jfc.getSelectedFile();
		    mview.putProperty("DecoratedPanel.load_path", file.getPath());
		    
		    try
		    {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			
			Decoration d = Decoration.read(mview, reader);
			while(d != null)
			{
			    addDeco(d, false);
			    updateFont(decs.length-1);
			    updateImage(decs.length-1);
			    d = Decoration.read(mview, reader);
			}
			reader.close();
			populateDecoList();
			dec_list.setSelectedIndex(decs.length-1);
			notifyListeners();
		    }
		    catch (java.io.IOException ioe)
		    {
			mview.errorMessage("Unable to read from '" + file.getName() + "'");
			
		    }
		    
		}
	    }
	    catch(UserInputCancelled uic)
	    {
	    }
	}
    }

    private void saveDecos()
    {
	JFileChooser jfc = mview.getFileChooser();
	
	String dld = mview.getProperties().getProperty("DecoratedPanel.save_path");
	if(dld != null)
	{
	    File ftmp = new File(dld);
	    jfc.setCurrentDirectory(ftmp);
	}
	
	int ret_val =  jfc.showSaveDialog(editor_frame); 
	if(ret_val == JFileChooser.APPROVE_OPTION) 
	{
	    File file = jfc.getSelectedFile();
	    mview.putProperty("DecoratedPanel.save_path", file.getPath());

	    try
	    {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		
		for(int d=0; d < decs.length; d++)
		    decs[d].write(writer);
		writer.flush();
		writer.close();
	    }
	    catch (java.io.IOException ioe)
	    {
		mview.errorMessage("Unable to write to '" + file.getName() + "'");
		
	    }
	}
    }

    // =================================================================================================
    // drawing
    // =================================================================================================

    final public void drawDecorations(Graphics g, int w, int h)
    {
	drawDecorations((Graphics2D) g, w, h);
    }

    final private int toWindowX(int vx, int w)
    {
	//System.out.println("x=" +vx + " -> "+ ((int)((double)(getWidth() * vx) / (double) 100.0)));

	return (int)((double)(w * vx) * (double) .01);
    }
    final private int toWindowY(int vy, int h)
    {
	//System.out.println("y=" +vy + " -> "+ ((int)((double)(getHeight() * vy) / (double) 100.0)));

	return (int)((double)(h * vy) * (double) .01);
    }

    final public void drawDecorations(Graphics2D g, int width, int height)
    {
	final int n_decs = decs.length;

	FontRenderContext frc = new FontRenderContext(null, false, false);
	
	double scale = (Math.sqrt( (width * width) + (height * height) ) * 0.01);

	for(int d=0; d < n_decs; d++)
	{
	    Decoration dec = decs[d];

	    int loc_x = toWindowX(dec.d_loc.x, width);
	    int loc_y = toWindowY(dec.d_loc.y, height);
	
	    switch(dec.d_type)
	    {
	    case LABEL:
		String str = dec.d_text;
		if((str != null) && (str.length() > 0))
		{
		    /*
		    g.setFont( d_font[d] );

		    FontMetrics fm = g.getFontMetrics();
		    int fa = fm.getAscent();
		    int fd = fm.getDescent();
		    int fh = fa + fd;
		    int fw = fm.stringWidth(d_text[d]);
		    
		    g.setColor( d_back[d] );
		    g.fillRect( d_loc[d].x, d_loc[d].y, fw, fh );
		    g.setColor( d_col[d] );
		    g.drawString( d_text[d], d_loc[d].x, d_loc[d].y + fa );
		    */
		    TextLayout textTl = new TextLayout(dec.d_text, dec.d_font, frc);
		    
		    AffineTransform new_at = new AffineTransform();
		    
		    double tw = textTl.getBounds().getWidth();
		    double th = textTl.getBounds().getHeight();

		    switch(dec.d_orient)
		    {
		    case 0:
			new_at.translate(loc_x - (tw/2), loc_y - (th/2)); 
			break;
		    case 1:
			new_at.translate(loc_x - (th/2), loc_y - (tw/2)); 
			new_at.rotate(Math.toRadians(90), 0, 0);
			break;
		    case 2:
			new_at.translate(loc_x + (th/2), loc_y + (tw/2)); 
			new_at.rotate(Math.toRadians(-90), 0, 0);
			break;
		    }
		    
		    if(dec.d_back_mode == 1)
		    {
			Shape back = new_at.createTransformedShape(textTl.getBounds());
			g.setColor( dec.d_back );
			g.fill(back);
		    }

		    Shape shape = textTl.getOutline(new_at);
		    g.setColor( dec.d_col );
		    g.fill(shape);

		    
		}
		break;

	    case ARROW:
		{
		    int len = (int) (dec.d_arrow_len * scale);
		    double dwid = (dec.d_arrow_w * scale);
		    int wid = (int) dwid;
		    
		    int hwid = (int) (dwid * dec.d_arrow_head_w);
		    int hlen = (int) (dwid * dec.d_arrow_head_l);

		    int x_pts[] = { 0-wid, 0+wid, 0+wid, 0+hwid, 0, 0-hwid, 0-wid };	
		    int y_pts[] = { 0, 0, 0+len-hlen, 0+len-hlen, 0+len, 0+len-hlen, 0+len-hlen };

		    GeneralPath polygon = new GeneralPath(GeneralPath.WIND_EVEN_ODD, x_pts.length);
		    polygon.moveTo(x_pts[0], y_pts[0]);
		
		    for (int index = 1; index < x_pts.length; index++) 
			polygon.lineTo(x_pts[index],
				       y_pts[index]);
		    polygon.closePath();
		    
		    AffineTransform new_at = new AffineTransform();
		    new_at.translate(loc_x, loc_y);
		    new_at.rotate(Math.toRadians(-dec.d_line_dir ), 0, 0);
		    Shape arrow = new_at.createTransformedShape(polygon);
		    
		    g.setColor( dec.d_col );
		    
		    if(dec.d_fill_mode == 1)
		    {
			g.setStroke( new BasicStroke( 1 ));
			g.fill( arrow );
		    }

		    if(dec.d_line_w > 0)
		    {
			g.setColor( dec.d_outline_col );
			g.setStroke( new BasicStroke( (int)(dec.d_line_w * scale) ) );
			g.draw( arrow );
		    }
		}
		break;

	    case LEGEND:
		{
		    int len = (int) dec.d_leg_w;
		    
		    int lw = toWindowX(dec.d_leg_w, width);
		    int lh = toWindowY(dec.d_leg_h, height);

		    int col_len = lw;
		    int sx = loc_x;
		    int sy = loc_y;
		    int ex = loc_x;
		    int ey = loc_y + lh;
		    int dir = 1;

		    if(dec.d_leg_orient == 0)
		    {
			g.fillRect( loc_x, loc_y, lw, lh);
		    }			
		    else
		    {
			col_len = lh;
			ex = loc_x + lw;
			if(dec.d_leg_orient == 1)
			{
			    dir = 1;
			    ey = sy;
			}
			else
			{
			    dir = -1;
			    sy = ey = loc_y + lh;
			}
		    }
		    
		    if(dec.d_colouriser != null)
		    {
			if(col_len > 0)
			{
			    g.setStroke( new BasicStroke( 1 ));
			    double min = dec.d_colouriser.getMin();
			    double max = dec.d_colouriser.getMax();
			    double cscale = (max - min) / (double) col_len;
			    
			    for(int p = 0; p < col_len; p++)
			    {
				double v = (((double) p) * cscale) + min;
				// 			    System.out.println(p + "\t" + v);
				g.setColor( dec.d_colouriser.lookup(v) );
				g.drawLine( sx,sy, ex,ey );
				if(dec.d_leg_orient == 0)
				{
				    sx++; ex++;
				}
				else
				{
				    sy += dir; ey += dir;
				}
			    }

			    g.setColor( dec.d_col );
			    g.setFont( dec.d_font );

			    final int ncols = dec.d_colouriser.getNumDiscreteColours();
			    for(int nc=0; nc < ncols; nc++)
			    {
				double v = dec.d_colouriser.getDiscreteColourValue(nc);
				int p = (int)((v - min) / cscale);
				
				g.drawString( String.valueOf(v), loc_x + p, loc_y);
			    }
			}
		    }
		    else
		    {
			g.setColor( Color.white );
			g.fillRect( loc_x, loc_y, lw, lh);
		    }
		}
		break;

	    case IMAGE:
		{
		    int lw = toWindowX(dec.d_img_w, width);
		    int lh = toWindowY(dec.d_img_h, height);

		    g.setColor( Color.white );

		    if(dec.d_img_i == null)
		    {
			g.fillRect( loc_x, loc_y, lw, lh);
		    }
		    else
		    {
			g.drawImage( dec.d_img_i, loc_x, loc_y, lw, lh, null );
		    }
		}
		break;

	    case RECTANGLE:
		{
		    g.setStroke( new BasicStroke( 1 ));

		    int lw = toWindowX(dec.d_leg_w, width);
		    int lh = toWindowY(dec.d_leg_h, height);

		    
		    if(dec.d_fill_mode == 1)
		    {
			g.setColor( dec.d_col );
			g.fillRect( loc_x, loc_y, lw, lh);
		    }

		    if(dec.d_line_w > 0)
		    {
			g.setStroke( new BasicStroke( (int)(dec.d_line_w * scale) ) );
			
			g.setColor( dec.d_outline_col );
			
			g.drawRect( loc_x, loc_y, lw, lh);
		    }
		}
		break;

	    }
	}
    }
    
    public void initAxes()
    {
    }

    private JTextField x_jtf, y_jtf;

    private Dimension butt_size = new Dimension(21,21);
    
    private ImageIcon left_ii;
    private ImageIcon right_ii;
    private ImageIcon up_ii;
    private ImageIcon down_ii;
    
    private ImageIcon big_left_ii;
    private ImageIcon big_right_ii;
    private ImageIcon big_up_ii;
    private ImageIcon big_down_ii;
    
    private maxdView mview;
    private DataPlot dplot;;
    private String prop_name;

    private JPanel atts_panel;
    private JList  dec_list;

    protected Font     default_font;
}
