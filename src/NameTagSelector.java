import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import javax.swing.event.*;

public class NameTagSelector extends JPanel
{
    public final static int SINGLE_SELECTION   = 1;
    public final static int MULTIPLE_SELECTION = 2;
    public final static int POPUP_MENU         = 4;
    public final static int BUTTON_PANEL       = 8;
 
    public NameTagSelector(maxdView mview_)
    {
	this(mview_, SINGLE_SELECTION | BUTTON_PANEL, null);
    }
    
    public NameTagSelector(maxdView mview_, int mode_)
    {
	this(mview_, mode_, null);
    }

    public NameTagSelector(maxdView mview_, String user_opt_str_)
    {
	this(mview_, SINGLE_SELECTION | BUTTON_PANEL, user_opt_str_);
    }
    
    public NameTagSelector(maxdView mview_, int mode_, String user_opt_str_)
    {
	mview = mview_;
	mode = mode_;

	user_opt_str = user_opt_str_;
	user_opt_sel = false;

	single_sel_name_t = -1;

	font = null;

	nts = mview.getExprData().new NameTagSelection();

	GridBagLayout gbag = new GridBagLayout();
	setLayout(gbag);
	    
	ImageIcon ii = new ImageIcon(mview.getImageDirectory() + "down-arrow.gif");

	setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
	
	jb = new JButton("Names & Tags", ii);
	jb.setHorizontalTextPosition(AbstractButton.LEFT);
	GridBagConstraints c = new GridBagConstraints();
	
	c.anchor = GridBagConstraints.WEST;
	c.gridx = 0;
	c.gridy = 0;
	c.weightx = c.weighty = 1.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	//c.fill = GridBagConstraints.BOTH;
	gbag.setConstraints(jb, c);
	
	add(jb);
	
	jb.addActionListener(new ActionListener() 
	    { 
		public void actionPerformed(ActionEvent e) 
		{
		    showPopup();
		}
	    });

	popup_menu = null;
	popup_frame = null;
    }

    // =============================================================================================

    public void setEnabled(boolean en)
    {
	jb.setEnabled(en);
    }

    public void setFont(Font fo)
    {
	font = fo;

	if(jb != null)
	    jb.setFont(fo);
	
	if(popup_menu != null)
	    popup_menu.setFont(fo);
    }


    // =============================================================================================


    public void addActionListener(ActionListener al_)
    {
	al = al_;
    }

    public JPopupMenu makePopupMenu()
    {
	JPopupMenu menu= new JPopupMenu();
	if(font != null)
	    menu.setFont(font);
	addCheckBoxItemsToComponent( menu );
	return menu;
    }

    public JFrame makePopupWindow()
    {
	if( ! isVisible() )
	    return null;

	final JFrame frame = new JFrame("Pick...");

	GridBagLayout gbag = new GridBagLayout();
	JPanel outer = new JPanel();
	outer.setLayout( gbag );
	outer.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));


	JPanel inner = new JPanel();
	inner.setLayout( new BoxLayout(inner, BoxLayout.Y_AXIS) );
	inner.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));

	addCheckBoxItemsToComponent( inner );
	JScrollPane jsp = new JScrollPane( inner );
	GridBagConstraints c = new GridBagConstraints();
	c.weightx = 1.0;
	c.weighty = 10.0;
	c.fill = GridBagConstraints.BOTH;
	gbag.setConstraints( jsp, c);
	outer.add( jsp );


	Dimension fill_size = new Dimension(3,3);
	Box.Filler filler = new Box.Filler(fill_size, fill_size, fill_size);
	c = new GridBagConstraints();
	//c.fill = GridBagConstraints.HORIZONTAL;
	c.gridy = 1;
	gbag.setConstraints(filler, c);
	outer.add(filler);


	JButton jb = new JButton("OK");
	jb.addActionListener( new ActionListener() 
	    { 
		public void actionPerformed(ActionEvent e) 
		{
		    if(al != null)
			al.actionPerformed(null);

		    frame.setVisible( false );
		}
	    });
	c = new GridBagConstraints();
	c.gridy = 2;
	c.weightx = 1.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	gbag.setConstraints( jb, c);
	outer.add( jb );


	frame.getContentPane().add( outer );	
	frame.pack();

	mview.decorateFrame( frame );

	//mview.locateWindowAtCenter( frame );

	// try to locate the popup near the button which activated it....
	try
	{
	    Point but_loc = getLocationOnScreen();
	    Dimension but_size = getSize();
	    
	    Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
	    
	    int px = but_loc.x + (but_size.width / 2);
	    int py = but_loc.y  + (but_size.height / 2);
	    
	    Dimension size = frame.getSize();
	    
	    if( ( px + size.width ) > screen_size.getWidth() )
		px = (int)screen_size.getWidth() - size.width;
	    if( px < 0 )
		px = 0;
	    
	    if( ( py + size.height ) > screen_size.getHeight() )
		py = (int)screen_size.getHeight() - size.height;
	    if( py < 0 )
		py = 0;
	    
	    frame.setLocation( px, py );
	}
	catch( java.awt.IllegalComponentStateException icse )
	{
	    // it's ok, it just means that the button doesn't exist
	    // (because the NTS is being activated from a menu)

	    mview.locateWindowAtCenter( frame );
	}

	return frame;
    }

    
    private void addCheckBoxItemsToComponent( JComponent component )
    {
	if(nts == null)
	    return;

	{
	    // SINGLE_SELECTION
	    ButtonGroup bg = null;
	    if((mode & MULTIPLE_SELECTION) == 0)
		bg= new ButtonGroup();
	    
	    {
		final javax.swing.AbstractButton jcb = getSuitableCheckBox( "Gene name(s)" );
		jcb.setSelected(nts.g_names);
		jcb.addActionListener(new SelActionListener( jcb, 0, -1 ));
		component.add(jcb);

		if(bg != null)
		    bg.add(jcb);
	    }

	    ExprData edata = mview.getExprData();
	    
	    ExprData.TagAttrs ta = edata.getGeneTagAttrs();
	    if(nts.g_attrs.length != ta.getNumAttrs())
		nts.g_attrs = new boolean[ta.getNumAttrs()];
	    for(int a=0; a < ta.getNumAttrs(); a++)
	    {
		final javax.swing.AbstractButton jcb = getSuitableCheckBox("  " + ta.getAttrName(a));
		jcb.setSelected(nts.g_attrs[a]);
		jcb.addActionListener(new SelActionListener( jcb, 0, a ));
		component.add(jcb);

		if(bg != null)
		    bg.add(jcb);
	    }
	    
	    {
		final javax.swing.AbstractButton jcb = getSuitableCheckBox("Probe name");
		jcb.setSelected(nts.p_name);
		jcb.addActionListener(new SelActionListener( jcb, 1, -1 ));
		component.add(jcb);

		if(bg != null)
		    bg.add(jcb);
	    }
	    
	    ta = edata.getProbeTagAttrs();
	    if(nts.p_attrs.length != ta.getNumAttrs())
		nts.p_attrs = new boolean[ta.getNumAttrs()];
	    for(int a=0; a < ta.getNumAttrs(); a++)
	    {
		final javax.swing.AbstractButton jcb = getSuitableCheckBox("  " + ta.getAttrName(a));
		jcb.setSelected(nts.p_attrs[a]);
		jcb.addActionListener(new SelActionListener( jcb, 1, a ));
		component.add(jcb);

		if(bg != null)
		    bg.add(jcb);		
	    }
	    
	    {
		final javax.swing.AbstractButton jcb = getSuitableCheckBox("Spot name");
		jcb.setSelected(nts.s_name);
		jcb.addActionListener(new SelActionListener( jcb, 2, -1 ));
		component.add(jcb);

		if(bg != null)
		    bg.add(jcb);		
	    }
	    
	    ta = edata.getSpotTagAttrs();
	    if(nts.s_attrs.length != ta.getNumAttrs())
		nts.s_attrs = new boolean[ta.getNumAttrs()];
	    for(int a=0; a < ta.getNumAttrs(); a++)
	    {
		final javax.swing.AbstractButton jcb = getSuitableCheckBox("  " + ta.getAttrName(a));
		jcb.setSelected(nts.s_attrs[a]);
		jcb.addActionListener(new SelActionListener( jcb, 2, a ));
		component.add(jcb);

		if(bg != null)
		    bg.add(jcb);		
	    }
	   
	    if(user_opt_str != null)
	    {
		/*
		if(is_popup)
		    ((JPopupMenu)popup).addSeparator();
		else
		    ((JMenu)popup).addSeparator();
		*/

		final javax.swing.AbstractButton jcb = getSuitableCheckBox(user_opt_str);
		jcb.setSelected(user_opt_sel);
		jcb.addActionListener(new SelActionListener( jcb, 3, -1 ));
		component.add(jcb);

		if(bg != null)
		    bg.add(jcb);		
	    }
	} 
    }

    private javax.swing.AbstractButton getSuitableCheckBox( String str )
    {
	if( mode == POPUP_MENU )
	    return new JCheckBoxMenuItem( str );
	else
	    return new JCheckBox( str );	    
    }


    // =============================================================================================

    public void setUserOptionSelected()
    {
	user_opt_sel = true;
	jb.setText(user_opt_str);
    }

    // =============================================================================================
    
    public ExprData.NameTagSelection getNameTagSelection()
    {
	return nts;
    }

    public void setNameTagSelection(ExprData.NameTagSelection nts_)
    {
	//nts = nts_.copy();
	nts = nts_;

	if((mode & SINGLE_SELECTION) > 0)
	{
	    // store the single selection data
	    if(userOptionSelected())
	    {
		single_sel_name_t = -1;
	    }
	    if(nts.isGeneNames())
	    {
		single_sel_name_t = 0;
		single_sel_attr_i = -1;
	    }
	    if(nts.isProbeName())
	    {
		single_sel_name_t = 1;
		single_sel_attr_i = -1;
	    }
	    if(nts.isSpotName())
	    {
		single_sel_name_t = 2;
		single_sel_attr_i = -1;
	    }
	    if(nts.isGeneNamesAttr())
	    {
		for(int b=0; b < nts.g_attrs.length; b++)
		    if(nts.g_attrs[b])
		    {
			single_sel_name_t = 0;
			single_sel_attr_i = b;
		    }
	    }
	    if(nts.isProbeNameAttr())
	    {
		for(int b=0; b < nts.p_attrs.length; b++)
		    if(nts.p_attrs[b])
		    {
			single_sel_name_t = 1;
			single_sel_attr_i = b;
		    }
	    }
	    if(nts.isSpotNameAttr())
	    {
		for(int b=0; b < nts.s_attrs.length; b++)
		    if(nts.s_attrs[b])
		    {
			single_sel_name_t = 2;
			single_sel_attr_i = b;
		    }
	    }
	}

	updateLabel();
	
	if( mode == POPUP_MENU )
	    popup_menu  = makePopupMenu();
	else
	    popup_frame = makePopupWindow();
    }

    public boolean hasUserOption() { return user_opt_str != null; }

    public boolean userOptionSelected() { return user_opt_sel; }

    // which name type is selected? 0==gene, 1==probe, 2==spot
    // NOTE: only makes sense for SINGLE_SELECTION mode
    public int getSelectedNameType()
    {
	return single_sel_name_t;
    }

    // which name tag attr is selected? -1==name, >=0 == attr_id
    // NOTE: only makes sense for SINGLE_SELECTION mode
    public int getSelectedAttrID()
    {
	return single_sel_attr_i;
    }

    public void setSingleSelection( int name_t, int attr_i )
    {
	if(name_t >= 0)
	{
	    nts.setSingleSelection( name_t, attr_i );
	}
    }

    // =============================================================================================

     // expects strings in the form: 1[ 2][ 3][ 4]...
    //  returns an array of integers, using -1 to signal 'illegal'
    //
    private int[] parseIndexList(String str)
    {
	if((str == null) || (str.length() == 0))
	   return null;

	Vector int_v = new Vector();
	StringTokenizer st = new StringTokenizer(str);
	while (st.hasMoreTokens()) 
	{
	    try
	    {
		Integer i = new Integer( st.nextToken() );
		int_v.addElement(i);
	    }
	    catch(NumberFormatException nfe)
	    {
		int_v.addElement( new Integer(-1) );
	    }
	}

	if(int_v.size() == 0)
	    return null;

	int[] res = new int[ int_v.size() ];
	for(int i=0; i < int_v.size(); i++)
	    res[i] = ((Integer) int_v.elementAt(i)).intValue();
	
	// System.out.println(str + " -> " + res);

	return res;

    } 
	
    private void setBoolArray( boolean[] ba, String str)
    {
	for(int b=0; b < ba.length; b++)
	    ba[b] = false;

	int[] il = parseIndexList(str);

	if(il != null)
	{
	    for(int i=0; i < il.length; i++)
	    {
		if(il[i] < ba.length)
		    ba[il[i]] = true;
	    }
	}
    }

    private String boolArrayToString( boolean[] ba )
    {
	String res = "";
	for(int i=0; i < ba.length; i++)
	{
	    if(ba[i])
	    {
		if(res.length() > 0)
		    res += " ";
		res += String.valueOf(i);
	    }
	}
	return res;
    }

    public void saveSelection(String prop_name)
    {
	int names_bf = 0;  // make a bit-field of names
	if(nts.g_names)
	    names_bf += 1;
	if(nts.p_name)
	    names_bf += 2;
	if(nts.s_name)
	    names_bf += 4;
	if(user_opt_sel)
	    names_bf += 8;

	mview.putIntProperty(prop_name+".Name", names_bf);
	
	mview.putProperty(prop_name+".GeneAttr",  boolArrayToString( nts.g_attrs ));
	mview.putProperty(prop_name+".ProbeAttr", boolArrayToString( nts.p_attrs ));
	mview.putProperty(prop_name+".SpotAttr",  boolArrayToString( nts.s_attrs ));
	
    }
 
    public void loadSelection(String prop_name)
    {
	String check_it = mview.getProperty(prop_name+".Name", null);
	if(check_it == null)
	{
	    nts.s_name = true;
	}
	else
	{
	    int names_bf = mview.getIntProperty(prop_name+".Name", 1);
	    
	    nts.g_names  = ((names_bf & 1) > 0);
	    nts.p_name   = ((names_bf & 2) > 0);
	    nts.s_name   = ((names_bf & 4) > 0);
	    user_opt_sel = ((names_bf & 8) > 0);
	    
	    String attrs = mview.getProperty(prop_name+".GeneAttr", null);
	    if(attrs != null)
		setBoolArray(nts.g_attrs, attrs);
	    attrs = mview.getProperty(prop_name+".ProbeAttr", null);
	    if(attrs != null)
		setBoolArray(nts.p_attrs, attrs);
	    attrs = mview.getProperty(prop_name+".SpotAttr", null);
	    if(attrs != null)
		setBoolArray(nts.s_attrs, attrs);
	}

	updateLabel();
    }

    // =============================================================================================

    private void updateLabel()
    {
	if(user_opt_sel)
	{
	    jb.setText( user_opt_str );
	}
	else
	{
	    String txt = nts.getNames();
	    
	    if((txt == null) || (txt.length() == 0))
		txt = "[none]";
	    
	    if(txt.length() > 17)
		txt = txt.substring(0,16) + "...";
	    
	    jb.setText( txt );
	}
    }

    // =============================================================================================

    private class SelActionListener implements ActionListener
    {
	private String name;
	private AbstractButton jab;
	private int name_t, attr_i;
	
	public SelActionListener(final AbstractButton jab_, final int name_t_, final int attr_i_)
	{
	    this(null, jab_, name_t_, attr_i_);
	}
	public SelActionListener(final String name_, final AbstractButton jab_, final int name_t_, final int attr_i_)
	{
	    name = name_; jab = jab_; name_t = name_t_; attr_i = attr_i_;
	}
	
	public void actionPerformed(ActionEvent e) 
	{
	    boolean sel = jab.isSelected();

	    single_sel_name_t = name_t;
	    single_sel_attr_i = attr_i;

	    if((mode & MULTIPLE_SELECTION) == 0)
	    {
		user_opt_sel = nts.g_names = nts.p_name = nts.s_name = false;
		for(int a=0; a < nts.g_attrs.length; a++)
		    nts.g_attrs[a] = false;
		for(int a=0; a < nts.p_attrs.length; a++)
		    nts.p_attrs[a] = false;
		for(int a=0; a < nts.s_attrs.length; a++)
		    nts.s_attrs[a] = false;
	    }

	    // System.out.println(name_t + ":" + attr_i + " = " + sel);

	    switch(name_t)
	    {
	    case 0:
		if(attr_i < 0)
		{
		    nts.g_names = sel;
		}
		else
		{
		    nts.g_attrs[attr_i] = sel;
		}
		break;
		
	    case 1:
		if(attr_i < 0)
		{
		    nts.p_name = sel;
		}
		else
		{
		    nts.p_attrs[attr_i] = sel;
		}
	    break;
	    
	    case 2:
		if(attr_i < 0)
		{
		    nts.s_name = sel;
		}
		else
		{
		    nts.s_attrs[attr_i] = sel;
		}
		break;

	    case 3:
		user_opt_sel = sel;
	    }
	    

	    if((mode & MULTIPLE_SELECTION) == 0)
	    {
		if(sel)
		{
		    //System.out.println(name + " selected");
		}
		else
		{
		    //System.out.println(name + " unselected");
		}
	    }

	    updateLabel();

	    if(al != null)
		al.actionPerformed(null);
	}
    }
    

    public void showPopup()
    {
	// System.out.println("show popup....");

	/*
	{
	    System.out.println("  hiding...");
	    popup.setVisible(false);
	}
	else
	*/

	if( mode == POPUP_MENU )
	{
	    if((popup_menu != null) && popup_menu.isVisible())
		return;
	    
	    popup_menu = makePopupMenu();
	    
	    // System.out.println("  showing...");
	    
	    Point cur_pos = getLocationOnScreen();
	    
	    popup_menu.setLocation(cur_pos);
	    //popup.setWidth(jb.getWidth());
	    popup_menu.show(this, 0, jb.getHeight());
	}
	else
	{
	    if((popup_frame != null) && popup_frame.isVisible())
	    {
		popup_frame.setVisible( true );
		return;
	    }

	    popup_frame = makePopupWindow();
	    
	    if( popup_frame != null )
		popup_frame.setVisible( true );
	}

    }

    // =============================================================================================

    private ExprData.NameTagSelection nts;

    // the component's user can add one extra option (e.g. "Annotation" or "Nothing") 

    private String  user_opt_str;
    private boolean user_opt_sel;

    private int single_sel_name_t;
    private int single_sel_attr_i;

    private ActionListener al;

    private JPopupMenu popup_menu;
    private JFrame     popup_frame;

    private JButton jb;


    private Font font;

    private maxdView mview;
    private int mode;
}
