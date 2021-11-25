import java.awt.geom.*;
import java.awt.font.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
import javax.swing.border.*;

public class AxisManager
{
    public AxisManager(maxdView mview_)
    {
	mview = mview_;
	dplot = mview.getDataPlot();

	axes = new PlotAxis[0];
	listeners = new Vector();
    }

    public void addAxesListener(AxesListener al)
    {
	listeners.addElement(al);
    }

    public void addAxis(PlotAxis pa)
    {
	PlotAxis[] new_axes = new PlotAxis[axes.length+1];
	for(int d=0; d < axes.length; d++)
	    new_axes[d] = axes[d];
	new_axes[axes.length] = pa;
	axes = new_axes;
	populateAxisList();
	
	if(axis_list != null)
	    axis_list.setSelectedIndex(axes.length-1);
	
	notifyListeners();

	// System.out.println("axis added, there are now " + axes.length);
    }

    public PlotAxis getAxis( int index )
    { 
	return axes[ index ]; 
    }

    public void removeAllAxes()
    {
	axes = new PlotAxis[0];
	populateAxisList();
	notifyListeners();
    }

    private void notifyListeners()
    {
	for(int l=0; l <listeners.size(); l++)
	    ((AxesListener) listeners.elementAt(l)).axesChanged();
    }

    public interface AxesListener
    {
	public void axesChanged();
    }

    // =================================================================================================
    // wrapped interface to individual PlotAxis
    // =================================================================================================

    public void setComputedRange(int axis, double min, double max)
    {
	axes[axis].setComputedRange(min, max);

	if(editor_frame != null)
	{
	    populateAttsPanel(axis_list.getSelectedIndex());
	}
    }

    public double getMin(int axis)
    {
	return axes[axis].getMin();
    }
    public double getMax(int axis)
    {
	return axes[axis].getMax();
    }
    public double getLength(int axis)
    {
	return axes[axis].axis_len;
    }

    public double longestLength()
    {
	double ll = .0;
	for(int a=0; a < axes.length; a++)
	    if(axes[a].axis_len > ll)
		ll = axes[a].axis_len;
	return ll;
    }

    public double shortestLength()
    {
	double ll = Double.MAX_VALUE;
	for(int a=0; a < axes.length; a++)
	    if(axes[a].axis_len < ll)
		ll = axes[a].axis_len;
	return ll;
    }


    public final double toScale( int axis, double val, double total_len )
    {
	return axes[axis].toScale( val, total_len );
    }

    public final double fromScale( int axis, double val, double total_len )
    {
	return axes[axis].fromScale( val, total_len );
    }

    // =================================================================================================
    // the graphics
    // =================================================================================================

    public void drawAxis( Graphics g, int axis_id, Point start, Point end )
    {
	double amin = axes[ axis_id ].getMin();
	double amax = axes[ axis_id ].getMax();

	if(amin == amax)
	    return;

	// overall line length

	final double line_dx = (double)(end.x - start.x);
	final double line_dy = (double)(end.y - start.y);
	final double line_length = Math.sqrt( ( line_dx *  line_dx  ) + ( line_dy * line_dy ) );


	Graphics2D g2 = (Graphics2D) g;
	
	g2.setFont( axes[ axis_id ].font );
	g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, 
			     RenderingHints.VALUE_ANTIALIAS_ON );
	

	// ::TODO::

	double angle = .0;

	if( start.y == end.y )
	{
	    // angle is  .0 or .180
	    angle = ( start.x > end.x ) ? 180 : 0;
	}
	else
	{
	    if( start.x == end.x ) 
	    {
		// angle is 90 or 270		
		angle = ( start.y > end.y ) ? 270 : 90;
	    }
	    else
	    {
		angle =  Math.toDegrees ( Math.atan( (double)( start.y - end.y ) / (double)( start.x - end.x ) ) );

		if( start.x > end.x )
		{
		    angle += 180;
		}


	    }
	}

	/*
	if(start.x == end.x)
	{
	    if( start.y < end.y )
		angle = 90.;
	    else
		angle = -90.;
	}
	*/

	double tick_height = 0;

	// ===========  the ticks and labels  ============================== 


	switch( axes[ axis_id ].tick_mode )
	{
	case PlotAxis.MinMaxZeroTicks:
	case PlotAxis.MinMaxTicks:

	    if( axes[ axis_id ].tick_mode == PlotAxis.MinMaxZeroTicks )
		if( amin < .0 )
		    drawTickAndLabel( g2, axes[ axis_id ], start, .0, line_length, angle );
	    
	    drawTickAndLabel( g2, axes[ axis_id ], start, amin, line_length, angle );
	    
	    tick_height = drawTickAndLabel( g2, axes[ axis_id ], start, amax, line_length, angle );
	    
	    break;

	case PlotAxis.AutoTicks:
	case PlotAxis.ManualTicks:

	    if( axes[ axis_id ].tick_positions != null )
	    {
		for( int i=0; i < axes[ axis_id ].tick_positions.length; i++ )
		{
		    if(( axes[ axis_id ].tick_positions[ i ] >= amin ) && ( axes[ axis_id ].tick_positions[ i ] <= amax ) ) 
		    {
			tick_height = drawTickAndLabel( g2, 
							axes[ axis_id ], start, axes[ axis_id ].tick_positions[ i ], 
							line_length, angle );
		    }
		}
	    }
	    break;
	}

	// ===========  the axis line and title ==========================


	// System.out.println( start.x + "," + start.y + " len=" +  line_length + " angle=" +  angle );

	AffineTransform at = new AffineTransform();
	at.translate( start.x , start.y );
	at.rotate( Math.toRadians( angle ), 0, 0);
	g2.setTransform( at );
	g2.drawLine( 0, 0, (int) line_length, 0 );
	
	String title = axes[ axis_id ].getTitle();

	if( title != null )
	{
	    //System.out.println( "title=" + title );

	    final TextLayout text_layout = new TextLayout( title, axes[ axis_id ].title_font, new FontRenderContext( null, false, false ) );
	    
	    final double text_align  = ( line_length / 2 ) - ( text_layout.getBounds().getWidth() * 0.5);
	    
	    //final double tick_length  = ( axes[ axis_id ].tick_dir == 1 ) ? axes[ axis_id ].tick_len : -axes[ axis_id ].tick_len ;
	    //final double tick_offset = tick_length * 1.5;

	    final double text_height = text_layout.getBounds().getHeight() * 2;
	    final double text_offset = ( axes[ axis_id ].tick_dir == 1 ) ? ( text_height + tick_height ) : ( text_height - tick_height );
	    	    
	    at.translate( text_align, text_offset );
	    
	    g2.setTransform( at );
	    
	    Shape shape  = text_layout.getOutline( null );
	    
	    g2.fill(shape);
	    
	}

    }

    
    // returns the height used by the tick and the label
    //
    private double drawTickAndLabel( Graphics2D g2, 
				   PlotAxis pa, 
				   Point origin, double value, 
				   double axis_length, 
				   double angle )
    {

	
	int pos = (int)( pa.toScale( value, axis_length ) );
	
	//int py = origin.y + (int)( pa.toScale( value, axis_length ) * axis_dy );

	//g.drawLine( px, py, px + tick_dx, py + tick_dy );

	//FontMetrics fm = g.getFontMetrics();
	//int fh = fm.getAscent(); // + fd;
	//int fw = fm.stringWidth( str );

	//g.drawString( str, px + tick_dx -(fw / 2), py + tick_dy + fh );

	//Graphics2D g2 = (Graphics2D) graphic;

	
	AffineTransform at = new AffineTransform();

	at.translate( origin.x , origin.y );
	
	at.rotate( Math.toRadians( angle ), 0, 0);

	// move along the line to the tick position

	at.translate( pos, 0 );

	g2.setTransform( at );

	final double tick_length  = ( pa.tick_dir == 1 ) ? pa.tick_len : -pa.tick_len ;

	g2.drawLine( 0, 0, 0, (int) tick_length );

	// get the size of the label, and translate accordingly to align it with the tick

	String label = mview.niceDouble( value, 20, pa.decimals );

	TextLayout text_layout = new TextLayout( label, pa.font, new FontRenderContext( null, false, false ) );

	final double text_align  = text_layout.getBounds().getWidth() * -0.5;
	
	final double tick_offset = tick_length * 1.25;
	final double text_height = text_layout.getBounds().getHeight();
	final double text_offset = ( pa.tick_dir == 1 ) ? ( text_height + tick_offset ) : tick_offset;

	at.translate( text_align, text_offset );

	g2.setTransform( at );

	Shape shape  = text_layout.getOutline( null );

//	g2.setTransform( at );

	g2.fill(shape);

	return text_offset;
    }

    
    private void fillSpaceWithTicks( final Graphics graphic, final int axis_id, 
				     double sv, double ev,
				     double angle,
				     int sx, int sy, int ex, int ey, 
				     int tdx, int tdy )
    {
	
	if(sv >= ev)
	    return;
	
	if(Double.isNaN(sv))
	    return;
	if(Double.isNaN(ev))
	    return;

/*
	double mv = (sv+ev) *.5;

	FontRenderContext frc = new FontRenderContext(null, false, false);
	String str = mview.niceDouble( mv, 20, axes[ axis_id ].decimals );
	TextLayout text_layout = new TextLayout(str, axes[ axis_id ].font, frc);

	int len_sq = ((sx-ex) * (sx-ex)) + ((sy-ey) * (sy-ey));

	int scaled_w = (int) ( text_layout.getBounds().getWidth() * axes[ axis_id ].tick_density );
	
	
	System.out.println("from " + sv + " to " + ev + " str=" + str  + 
			   " td= " + axes[ axis_id ].tick_density + " size=" + scaled_w);
	
  
	if(scaled_w > 0)
	{
	    
	    if((scaled_w*scaled_w) < len_sq)
	    {
		int px = (sx + ex) / 2;
		int py = (sy + ey) / 2;
		
		graphic.drawLine( px, py, px + tdx, py + tdy );
		
		if( axes[ axis_id ].label_mode == 3)
		    drawRotatedLabel( graphic, axis_id, str, 0, angle, px, py, tdx, tdy );
		
		fillSpaceWithTicks( graphic, axis_id, sv, mv, angle, sx, sy, px, py, tdx, tdy );
		fillSpaceWithTicks( graphic, axis_id, mv, ev, angle, px, py, ex, ey, tdx, tdy );
	    }
	}
*/
    }
    

    // =================================================================================================
    // the editor
    // =================================================================================================

    private JFrame editor_frame = null;

    public void startEditor()
    {
	// System.out.println("startEditor()...");

	if(editor_frame == null)
	{
	    makeEditor();
	    mview.decorateFrame( editor_frame );
	}

	populateAxisList();
	
	axis_list.setSelectedIndex(0);

	editor_frame.setVisible(true);	
    }

    // can be safely called even if the editor has never been started
    public void stopEditor()
    {
	if(editor_frame != null)
	    editor_frame.setVisible(false);	
    }

    // should be called when the PlotAxis values are changed 'externally',
    // i.e. by the plot which is using them
    //
    public void updateEditor()
    {
	if(axis_list != null)
	    populateAttsPanel( axis_list.getSelectedIndex() );
    }


    private void makeEditor()
    {
	editor_frame  = new JFrame("Axes");

	editor_frame.addWindowListener(new WindowAdapter() 
	    {
		public void windowClosing(WindowEvent e)
		{
		    editor_frame = null;
		}
	    });

	GridBagLayout gridbag = new GridBagLayout();
	GridBagConstraints c = null;
	JPanel panel = new JPanel();
	panel.setLayout(gridbag);
	
	panel.setPreferredSize(new Dimension( 480, 540 ));

	// ================================
	
	JPanel axis_panel = new JPanel();
	GridBagLayout axis_gridbag = new GridBagLayout();
	axis_panel.setLayout(axis_gridbag);

	axis_list = new JList();
	JScrollPane jsp = new JScrollPane(axis_list);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	c.weighty = 2.0;
	c.weightx = 1.0;
	//c.gridwidth = 2;
	c.fill = GridBagConstraints.BOTH;
	axis_gridbag.setConstraints(jsp, c);
	axis_panel.add(jsp);  
	axis_list_sel_listener = new ListSelectionListener() 
	    {
		public void valueChanged(ListSelectionEvent e) 
		{
		    populateAttsPanel( axis_list.getSelectedIndex() );
		}
	    };
	
	axis_list.addListSelectionListener(axis_list_sel_listener);
	
	JButton add_jb = new JButton("All");
	add_jb.setFont(mview.getSmallFont());
	add_jb.setMargin(new Insets(1,10,1,10));
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 1;
	c.weightx = 1.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	axis_gridbag.setConstraints(add_jb, c);
	axis_panel.add(add_jb);
	add_jb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    if(axes.length > 0)
		    {
			axis_list.removeListSelectionListener(axis_list_sel_listener);
			
			int[] sels = new int[axes.length];
			for(int s=0; s < sels.length; s++)
			    sels[s] = s;
			axis_list.setSelectedIndices(sels);
			
			axis_list.addListSelectionListener(axis_list_sel_listener);
			
			populateAttsPanel( 0 );
		    }
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
	gridbag.setConstraints(axis_panel, c);
	panel.add(axis_panel);  

	// ================================

	axis_list.addMouseListener(new MouseAdapter() 
	    {
		public void mouseClicked(MouseEvent e) 
		{
		    if(e.getClickCount() == 2)
		    {
			// 
			// selectAllSimilar();
		    }
		}
	    });

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

	
	// ================================

	JPanel but_panel = new JPanel();
	but_panel.setBorder(BorderFactory.createEmptyBorder(5,10,5,10));
	GridBagLayout but_gridbag = new GridBagLayout();
	but_panel.setLayout(but_gridbag);
	
	/*
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
		    
		}
	    });
	*/

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
		   mview.getHelpTopic("AxisManager");
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
		    editor_frame = null;
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

    private void populateAxisList()
    {
	if(axis_list == null)
	    return;

	Vector names = new Vector();

	for(int d=0; d < axes.length; d++)
	    names.addElement( axes[d].name );

	axis_list.setListData(names);


    }


    private void populateAttsPanel(final int axis_id)
    {
	atts_panel.removeAll();

	if((axis_id < 0) || (axis_id >= axes.length))
	{
	    atts_panel.updateUI();
	    return;
	}

	int sels = 0;
	for(int a=axis_list.getMinSelectionIndex(); a <= axis_list.getMaxSelectionIndex(); a++)
	    if(axis_list.isSelectedIndex(a))
		sels++;

	GridBagLayout gridbag = new GridBagLayout();
	GridBagConstraints c = null;
	JLabel label = null;
	JRadioButton jrb   = null;
	JCheckBox jchkb = null;
	atts_panel.setLayout(gridbag);

	int line = 0;

	// ===== scale =====================================================

	JPanel        but_wrapper = new JPanel();
	GridBagLayout but_gridbag = new GridBagLayout();
	but_wrapper.setLayout(but_gridbag);

	Color title_colour = new JLabel().getForeground().brighter();
	Font f = new JLabel().getFont();
	Font title_font = new Font(f.getName(), Font.BOLD, f.getSize() + 2);
	Font bfont = new Font(f.getName(), Font.PLAIN, f.getSize() - 2);

	TitledBorder title = BorderFactory.createTitledBorder(" Range & Scale ");
	title.setTitleColor(title_colour);
	title.setTitleFont(title_font);
	but_wrapper.setBorder(title);
	
	int iline = 0;

	label = new JLabel("Scale ");
	c = new GridBagConstraints();
	c.gridy = iline;
	//c.weightx = 1.0;
	c.weighty = 1.0;
	c.anchor = GridBagConstraints.EAST;
	but_gridbag.setConstraints(label, c);
	but_wrapper.add(label);

	jrb = new JRadioButton("Linear");
	ButtonGroup bg = new ButtonGroup();
	if(sels == 1)
	    jrb.setSelected(axes[axis_id].scale == PlotAxis.LinearScale);
	jrb.addActionListener(new CustomActionListener(axis_id, 40));
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = iline;
	c.weighty = 1.0;
	//c.weightx = 1.0;
	c.anchor = GridBagConstraints.WEST;
	//c.fill = GridBagConstraints.HORIZONTAL;
	but_gridbag.setConstraints(jrb, c);
	but_wrapper.add(jrb);
	bg.add(jrb);

	jrb = new JRadioButton("Ln");
	jrb.setToolTipText("Natural log");
	if(sels == 1)
	    jrb.setSelected(axes[axis_id].scale == PlotAxis.LogScale);
	jrb.addActionListener(new CustomActionListener(axis_id, 41));
	c = new GridBagConstraints();
	c.gridx = 2;
	c.gridy = iline;
	c.weighty = 1.0;
	//c.weightx = 1.0;
	c.anchor = GridBagConstraints.WEST;
	//c.fill = GridBagConstraints.HORIZONTAL;
	but_gridbag.setConstraints(jrb, c);
	but_wrapper.add(jrb);
	bg.add(jrb);

	jrb = new JRadioButton("Exp");
	jrb.setToolTipText("Exponential");
	if(sels == 1)
	    jrb.setSelected(axes[axis_id].scale == PlotAxis.ExpScale);
	jrb.addActionListener(new CustomActionListener(axis_id, 42));
	c = new GridBagConstraints();
	c.gridx = 3;
	c.gridy = iline;
	c.weightx = 1.0;
	c.weighty = 1.0;
	//c.weightx = 1.0;
	c.anchor = GridBagConstraints.WEST;
	//c.fill = GridBagConstraints.HORIZONTAL;
	but_gridbag.setConstraints(jrb, c);
	but_wrapper.add(jrb);
	bg.add(jrb);


	iline++;

	label = new JLabel("Min ");
	c = new GridBagConstraints();
	c.gridy = iline;
	c.anchor = GridBagConstraints.EAST;
	but_gridbag.setConstraints(label, c);
	but_wrapper.add(label);
	
	min_jtf = new JTextField(14);
	if(sels == 1)
	{
	    min_jtf.setText( mview.niceDouble( axes[axis_id].getMin(), 12, 6 ));
	    min_jtf.setEnabled(axes[axis_id].use_auto_min == false);
	}
	min_jtf.addActionListener(new CustomActionListener(axis_id, 1));

	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridwidth = 3;
	c.gridy = iline;
	c.gridwidth = 3;
	//c.gridwidth = 2;
	c.weighty = 1.0;
	c.weightx = 2.0;
	c.anchor = GridBagConstraints.WEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	but_gridbag.setConstraints(min_jtf, c);
	but_wrapper.add(min_jtf);

	jchkb = new JCheckBox("Auto");
	if(sels == 1)
	    jchkb.setSelected(axes[axis_id].use_auto_min);
	jchkb.setFont(mview.getSmallFont());
	jchkb.addActionListener(new CustomActionListener(axis_id, 20));
	c = new GridBagConstraints();
	c.gridx = 4;
	c.gridy = iline;
	//c.weightx = 1.0;
	//c.anchor = GridBagConstraints.WEST;
	//c.fill = GridBagConstraints.HORIZONTAL;
	but_gridbag.setConstraints(jchkb, c);
	but_wrapper.add(jchkb);

	
	iline++;

	label = new JLabel("Max ");
	c = new GridBagConstraints();
	c.gridy = iline;
	c.anchor = GridBagConstraints.NORTHEAST;
	but_gridbag.setConstraints(label, c);
	but_wrapper.add(label);
	
	max_jtf = new JTextField(14);
	if(sels == 1)
	{
	    max_jtf.setText( mview.niceDouble( axes[axis_id].getMax(), 12, 6 ));
	    max_jtf.setEnabled(axes[axis_id].use_auto_max == false);
	}
	else
	{
	    max_jtf.setText( "" );
	}
	max_jtf.addActionListener(new CustomActionListener(axis_id, 3));

	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = iline;
	c.gridwidth = 3;
	//c.gridwidth = 2;
	c.weighty = 1.0;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	but_gridbag.setConstraints(max_jtf, c);
	but_wrapper.add(max_jtf);


	jchkb = new JCheckBox("Auto");
	if(sels == 1)
	    jchkb.setSelected(axes[axis_id].use_auto_max);
	jchkb.setFont(mview.getSmallFont());
	jchkb.addActionListener(new CustomActionListener(axis_id, 31));
	c = new GridBagConstraints();
	c.gridx = 4;
	c.gridy = iline;
	//c.weightx = 1.0;
	//c.anchor = GridBagConstraints.NORTHWEST;
	//c.fill = GridBagConstraints.HORIZONTAL;
	but_gridbag.setConstraints(jchkb, c);
	but_wrapper.add(jchkb);

	iline++;

	label = new JLabel("Length ");
	label.setToolTipText( "The proportion of the window that this axis should occupy" ); 
	c = new GridBagConstraints();
	c.gridy = iline;
	c.anchor = GridBagConstraints.EAST;
	but_gridbag.setConstraints(label, c);
	but_wrapper.add(label);
	
	JSlider axis_len_js = new JSlider(JSlider.HORIZONTAL, 1, 100, (int) (axes[axis_id].axis_len * 100.0));
	axis_len_js.setToolTipText( "The proportion of the window that this axis should occupy" ); 
	axis_len_js.addChangeListener(new CustomChangeListener(axis_id, 15));
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = iline;
	c.weighty = 1.0;
	c.gridwidth = 4;
	c.anchor = GridBagConstraints.WEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	but_gridbag.setConstraints(axis_len_js, c);
	but_wrapper.add(axis_len_js);

	
	
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridwidth = 4;
	c.gridy = line;
	c.weighty = 1.0;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	gridbag.setConstraints(but_wrapper, c);
	atts_panel.add(but_wrapper);
	
	line++;

	// ===== tick mode =====================================================

	but_wrapper = new JPanel();
	but_gridbag = new GridBagLayout();
	but_wrapper.setLayout(but_gridbag);
	title = BorderFactory.createTitledBorder(" Ticks ");
	title.setTitleColor(title_colour);
	title.setTitleFont(title_font);
	but_wrapper.setBorder(title);
	   
	int bline = 0;

	label = new JLabel("Mode ");
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.EAST;
	but_gridbag.setConstraints(label, c);
	but_wrapper.add(label);
	
	final String[] tick_mode_names = { "No Ticks", "Min/Max", "Min/Max/Zero", "Auto", "Manual" };
	
	JComboBox jcb = new JComboBox(tick_mode_names);
	if(sels == 1)
	    jcb.setSelectedIndex(axes[axis_id].tick_mode);
	else
	    jcb.setSelectedIndex(-1);

	jcb.addActionListener(new CustomActionListener(axis_id, 5));
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridwidth = 2;
	c.gridy = bline++;
	c.weighty = 1.0;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.WEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	but_gridbag.setConstraints(jcb, c);
	but_wrapper.add(jcb);
	

	final JTextField manual_ticks_jtf = new JTextField( 20 );
	if( sels == 1)
	    manual_ticks_jtf.setText( axes[axis_id].manual_tick_spec );
	manual_ticks_jtf.addActionListener( new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    setManualTicks( axis_id, manual_ticks_jtf.getText() );
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 1;
	c.weightx = 1.0;
	c.gridy = bline;
	c.anchor = GridBagConstraints.EAST;
	c.fill = GridBagConstraints.HORIZONTAL;
	but_gridbag.setConstraints(manual_ticks_jtf, c);
	but_wrapper.add(manual_ticks_jtf);


	JButton set_manual_ticks_jb = new JButton("Set");
	set_manual_ticks_jb.addActionListener( new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    setManualTicks( axis_id, manual_ticks_jtf.getText() );
		}
	    });

	set_manual_ticks_jb.setMargin(new Insets(1,1,1,1));
	c = new GridBagConstraints();
	c.gridx = 2;
	c.gridy = bline++;
	c.anchor = GridBagConstraints.EAST;
	but_gridbag.setConstraints(set_manual_ticks_jb, c);
	but_wrapper.add(set_manual_ticks_jb);
	

	label = new JLabel("Length ");
	c = new GridBagConstraints();
	c.gridy = bline;
	c.anchor = GridBagConstraints.EAST;
	but_gridbag.setConstraints(label, c);
	but_wrapper.add(label);
	
	int tick_len = (int) axes[axis_id].tick_len;
	if ( tick_len < 1 )
	    tick_len = 1;
	if ( tick_len > 20 )
	    tick_len = 20;
	JSlider tick_len_js = new JSlider(JSlider.HORIZONTAL, 1, 20, tick_len );
	tick_len_js.addChangeListener(new CustomChangeListener(axis_id, 11));
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = bline++;
	c.gridwidth = 2;
	c.weighty = 1.0;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	but_gridbag.setConstraints(tick_len_js, c);
	but_wrapper.add(tick_len_js);


	label = new JLabel("Number of Ticks ");
	c = new GridBagConstraints();
	c.gridy = bline;
	c.anchor = GridBagConstraints.EAST;
	but_gridbag.setConstraints(label, c);
	but_wrapper.add(label);

	JSlider tick_density_js = new JSlider(JSlider.HORIZONTAL, 2, 32, axes[axis_id].number_of_ticks );
	tick_density_js.addChangeListener(new CustomChangeListener(axis_id, 16));
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridwidth = 2;
	c.gridy = bline++;
	c.weighty = 1.0;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	but_gridbag.setConstraints(tick_density_js, c);
	but_wrapper.add(tick_density_js);


	label = new JLabel("Direction ");
	c = new GridBagConstraints();
	c.gridy = bline;
	c.anchor = GridBagConstraints.EAST;
	but_gridbag.setConstraints(label, c);
	but_wrapper.add(label);

	final String[] tick_dir_names = { "Inwards", "Outwards" };
	
	jcb = new JComboBox(tick_dir_names);
	if(sels == 1)
	    jcb.setSelectedIndex(axes[axis_id].tick_dir);
	else
	    jcb.setSelectedIndex(-1);
	jcb.addActionListener(new CustomActionListener(axis_id, 10));
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = bline++;
	c.gridwidth = 2;
	c.weighty = 1.0;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	but_gridbag.setConstraints(jcb, c);
	but_wrapper.add(jcb);
	
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridwidth = 4;
	c.gridy = line;
	c.weighty = 1.0;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	gridbag.setConstraints(but_wrapper, c);
	atts_panel.add(but_wrapper);
	

	line++;
	
	// ===== label mode =====================================================


	but_wrapper = new JPanel();
	but_gridbag = new GridBagLayout();
	but_wrapper.setLayout(but_gridbag);
	title = BorderFactory.createTitledBorder(" Tick Labels ");
	title.setTitleColor(title_colour);
	title.setTitleFont(title_font);
	but_wrapper.setBorder(title);
	
/*
	label = new JLabel("Mode ");
	c = new GridBagConstraints();
	c.gridy = 0;
	c.anchor = GridBagConstraints.EAST;
	but_gridbag.setConstraints(label, c);
	but_wrapper.add(label);
	
	final String[] label_mode_names = { "None", "Min/Max", "Min/Max/Zero", "All ticks" };
	
	jcb = new JComboBox(label_mode_names);
	if(sels == 1)
	    jcb.setSelectedIndex(axes[axis_id].label_mode);
	else
	    jcb.setSelectedIndex(-1);
	jcb.addActionListener(new CustomActionListener(axis_id, 6));
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridwidth = 2;
	c.gridy = 0;
	c.weighty = 1.0;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.WEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	but_gridbag.setConstraints(jcb, c);
	but_wrapper.add(jcb);
*/	

	label = new JLabel("Decimals ");
	c = new GridBagConstraints();
	c.gridy = 1;
	c.anchor = GridBagConstraints.EAST;
	but_gridbag.setConstraints(label, c);
	but_wrapper.add(label);
		
	JSlider decimals_js = new JSlider(JSlider.HORIZONTAL, 0, 20, axes[axis_id].decimals);
	decimals_js.addChangeListener(new CustomChangeListener(axis_id, 17));
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 1;
	c.weighty = 1.0;
	c.gridwidth = 2;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	but_gridbag.setConstraints(decimals_js, c);
	but_wrapper.add(decimals_js);


	label = new JLabel("Font ");
	c = new GridBagConstraints();
	c.gridy = 2;
	c.anchor = GridBagConstraints.EAST;
	but_gridbag.setConstraints(label, c);
	but_wrapper.add(label);
	
	JComboBox font_family_jcb = new JComboBox(dplot.font_family_names);
	if(sels == 1)
	    font_family_jcb.setSelectedIndex(dplot.fontToFamily(axes[axis_id].font));
	else
	    font_family_jcb.setSelectedIndex(-1);

	font_family_jcb.addActionListener(new CustomActionListener(axis_id, 7));
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 2;
	c.weighty = 1.0;
	c.weighty = 0.75;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	but_gridbag.setConstraints(font_family_jcb, c);
	but_wrapper.add(font_family_jcb);
	
	JComboBox font_style_jcb = new JComboBox(dplot.font_style_names);
	if(sels == 1)
	    font_style_jcb.setSelectedIndex(dplot.fontToStyle(axes[axis_id].font));
	else
	    font_style_jcb.setSelectedIndex(-1);
	font_style_jcb.addActionListener(new CustomActionListener(axis_id, 8));
	c = new GridBagConstraints();
	c.gridx = 2;
	c.gridy = 2;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	but_gridbag.setConstraints(font_style_jcb, c);
	but_wrapper.add(font_style_jcb);
	
	label = new JLabel("Text Size ");
	c = new GridBagConstraints();
	c.gridy = 3;
	c.anchor = GridBagConstraints.EAST;
	but_gridbag.setConstraints(label, c);
	but_wrapper.add(label);
		
	JSlider font_size_js = new JSlider(JSlider.HORIZONTAL, 2, 72, axes[axis_id].font_size);
	font_size_js.addChangeListener(new CustomChangeListener(axis_id, 9));

	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 3;
	c.weighty = 1.0;
	c.gridwidth = 2;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	but_gridbag.setConstraints(font_size_js, c);
	but_wrapper.add(font_size_js);
	
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridwidth = 4;
	c.gridy = line;
	c.weighty = 1.0;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	gridbag.setConstraints(but_wrapper, c);
	atts_panel.add(but_wrapper);
	

	line++;


	// ===== title info =====================================================


	but_wrapper = new JPanel();
	but_gridbag = new GridBagLayout();
	but_wrapper.setLayout(but_gridbag);
	title = BorderFactory.createTitledBorder(" Title ");
	title.setTitleColor(title_colour);
	title.setTitleFont(title_font);
	but_wrapper.setBorder(title);
	
	label = new JLabel("Text ");
	c = new GridBagConstraints();
	c.gridy = 1;
	c.anchor = GridBagConstraints.EAST;
	but_gridbag.setConstraints(label, c);
	but_wrapper.add(label);
		
	title_jtf = new JTextField( 20 );
	title_jtf.setEnabled( axes[axis_id].getUseAutoTitle() == false );
	title_jtf.setText( axes[axis_id].user_title );
	title_jtf.addActionListener(new CustomActionListener(axis_id, 100));
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 1;
	c.weighty = 1.0;
	c.gridwidth = 2;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	but_gridbag.setConstraints(title_jtf, c);
	but_wrapper.add(title_jtf);


	jchkb = new JCheckBox("Auto");
	if(sels == 1)
	    jchkb.setSelected( axes[axis_id].getUseAutoTitle() );
	jchkb.setFont(mview.getSmallFont());
	jchkb.addActionListener(new CustomActionListener(axis_id, 104));
	c = new GridBagConstraints();
	c.gridx = 3;
	c.gridy = 1;
	//c.weightx = 1.0;
	//c.anchor = GridBagConstraints.NORTHWEST;
	//c.fill = GridBagConstraints.HORIZONTAL;
	but_gridbag.setConstraints(jchkb, c);
	but_wrapper.add(jchkb);


	label = new JLabel("Font ");
	c = new GridBagConstraints();
	c.gridy = 2;
	c.anchor = GridBagConstraints.EAST;
	but_gridbag.setConstraints(label, c);
	but_wrapper.add(label);
	
	font_family_jcb = new JComboBox(dplot.font_family_names);
	if(sels == 1)
	    font_family_jcb.setSelectedIndex(dplot.fontToFamily(axes[axis_id].title_font));
	else
	    font_family_jcb.setSelectedIndex(-1);

	font_family_jcb.addActionListener(new CustomActionListener(axis_id, 101));
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 2;
	c.weighty = 1.0;
	c.weighty = 0.75;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	but_gridbag.setConstraints(font_family_jcb, c);
	but_wrapper.add(font_family_jcb);
	
	font_style_jcb = new JComboBox(dplot.font_style_names);
	if(sels == 1)
	    font_style_jcb.setSelectedIndex(dplot.fontToStyle(axes[axis_id].title_font));
	else
	    font_style_jcb.setSelectedIndex(-1);
	font_style_jcb.addActionListener(new CustomActionListener(axis_id, 102));
	c = new GridBagConstraints();
	c.gridx = 2;
	c.gridy = 2;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	but_gridbag.setConstraints(font_style_jcb, c);
	but_wrapper.add(font_style_jcb);
	
	label = new JLabel("Text Size ");
	c = new GridBagConstraints();
	c.gridy = 3;
	c.anchor = GridBagConstraints.EAST;
	but_gridbag.setConstraints(label, c);
	but_wrapper.add(label);
		
	font_size_js = new JSlider(JSlider.HORIZONTAL, 2, 72, axes[axis_id].title_font_size);
	font_size_js.addChangeListener(new CustomChangeListener(axis_id, 103));
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 3;
	c.weighty = 1.0;
	c.gridwidth = 2;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	but_gridbag.setConstraints(font_size_js, c);
	but_wrapper.add(font_size_js);
	
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridwidth = 4;
	c.gridy = line;
	c.weighty = 1.0;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	gridbag.setConstraints(but_wrapper, c);
	atts_panel.add(but_wrapper);
	

	line++;
	
	// ==========================================================================

	atts_panel.updateUI();
    }

    private void setManualTicks( final int axis_id, final String manual_tick_spec )
    {
	final double[] manual_tick_positions_a = parseTickSpec( manual_tick_spec );
	
	for(int axis=axis_list.getMinSelectionIndex(); axis <= axis_list.getMaxSelectionIndex(); axis++)
	{
	    if(axis_list.isSelectedIndex(axis))
	    {
		axes[axis].manual_tick_spec = manual_tick_spec;   
		axes[axis].tick_positions = manual_tick_positions_a;
	    }
	}
	notifyListeners();
    }


    private class CustomChangeListener implements ChangeListener
    {
	private int code;
	public CustomChangeListener(int axis_, int code_)
	{
	    code = code_;
	}
	public void stateChanged(ChangeEvent e) 
	{
	    JSlider source = (JSlider)e.getSource();
	    if(!source.getValueIsAdjusting())
	    {
		for(int axis_id=axis_list.getMinSelectionIndex(); axis_id <= axis_list.getMaxSelectionIndex(); axis_id++)
		{
		    if(axis_list.isSelectedIndex(axis_id))
		    {
			switch(code)
			{
			case 9:
			    axes[axis_id].font_size = source.getValue();
			    axes[axis_id].updateFonts();
			    break;
			case 11:
			    axes[axis_id].tick_len = ((double) source.getValue());
			    break;

			case 15:
			    axes[axis_id].axis_len = ((double) source.getValue()) * 0.01;
			    break;

			case 16:
			    axes[axis_id].number_of_ticks = (int) source.getValue();
			    axes[axis_id].updateTicks();
			    break;

			case 17:
			    axes[axis_id].decimals = (int) source.getValue();
			    break;
  
			case 103:
			    axes[axis_id].title_font_size = source.getValue();
			    axes[axis_id].updateFonts();
			    break;	
		
 
			}
		    }
		}
		notifyListeners();
	    }
	}
    }
    
    private class CustomActionListener implements ActionListener
    {
	private int code;
	public CustomActionListener(int axis_, int code_)
	{
	    code = code_;
	}
	public void actionPerformed(ActionEvent e) 
	{
	    for(int axis=axis_list.getMinSelectionIndex(); axis <= axis_list.getMaxSelectionIndex(); axis++)
	    {
		if(axis_list.isSelectedIndex(axis))
		{
		    switch(code)
		    {
		    case 0:
			axes[axis].name = ((JTextField)e.getSource()).getText();
			break;
		    case 1:
			try
			{
			    axes[axis].user_min = (new Double(((JTextField)e.getSource()).getText())).doubleValue();
			    axes[axis].updateRange();
			}
			catch(NumberFormatException nfe) 
			{ }
			break;

		    case 20:
			axes[axis].use_auto_min = ((JCheckBox)e.getSource()).isSelected();
			min_jtf.setEnabled( ! axes[axis].use_auto_min);
			min_jtf.setText(  axes[axis].use_auto_min ? mview.niceDouble( axes[axis].getMin(), 12, 6 ) : String.valueOf( axes[axis].user_min ) );
			axes[axis].updateRange();
			break;


		    case 3:
			try
			{
			    axes[axis].user_max = (new Double(((JTextField)e.getSource()).getText())).doubleValue();
			    axes[axis].updateRange();
			}
			catch(NumberFormatException nfe) 
			{ }
			break;

		    case 31:
			axes[axis].use_auto_max = ((JCheckBox)e.getSource()).isSelected();
			max_jtf.setEnabled( ! axes[axis].use_auto_max );
			max_jtf.setText( axes[axis].use_auto_max ? mview.niceDouble( axes[axis].getMax(), 12, 6 ) : String.valueOf( axes[axis].user_max ) );
			axes[axis].updateRange();
			break;


		    case 40:  // set linear
			axes[axis].scale = PlotAxis.LinearScale;
			axes[axis].updateRange();
			break;
		    case 41:  // set log
			axes[axis].scale = PlotAxis.LogScale;
			axes[axis].updateRange();
			break;
		    case 42:  // set exp
			axes[axis].scale = PlotAxis.ExpScale;
			axes[axis].updateRange();
			break;


		    case 5:
			axes[axis].tick_mode = ((JComboBox)e.getSource()).getSelectedIndex();
			axes[axis].updateTicks();
			break;
		    case 10:
			axes[axis].tick_dir = ((JComboBox)e.getSource()).getSelectedIndex();
			break;

		   
		    case 7:
			axes[axis].font_fam = ((JComboBox)e.getSource()).getSelectedIndex();
			axes[axis].updateFonts();
			break;
			
		    case 8:
			axes[axis].font_sty = ((JComboBox)e.getSource()).getSelectedIndex();
			axes[axis].updateFonts();
			break;

		    case 100:
			axes[axis].user_title = ((JTextField)e.getSource()).getText();
			break;

		    case 101:
			axes[axis].title_font_fam = ((JComboBox)e.getSource()).getSelectedIndex();
			axes[axis].updateFonts();
			break;
			
		    case 102:
			axes[axis].title_font_sty = ((JComboBox)e.getSource()).getSelectedIndex();
			axes[axis].updateFonts();
			break;
			
		    case 104:
			axes[axis].setUseAutoTitle( ((JCheckBox)e.getSource()).isSelected() );
			title_jtf.setEnabled( axes[axis].getUseAutoTitle() == false );
			break;
		    }
		}
	    }

	    notifyListeners();
	}
    }

    private void updateFont(int axis_id)
    {

    }

    // =================================================================================================
    // tick positioning
    // =================================================================================================

    private double[] parseTickSpec( final String tick_spec )
    {
	StringTokenizer st = new StringTokenizer( tick_spec );

	java.util.Vector tick_pos_v = new java.util.Vector();

	while (st.hasMoreTokens()) 
	{
	    try
	    {
		Double d = new Double( st.nextToken() );
		tick_pos_v.add( d );
	    }
	    catch(NumberFormatException nfe )
	    {

	    }
	}

	if( tick_pos_v.size() == 0 )
	    return null;

	double[] result = new double[ tick_pos_v.size() ];

	for( int d=0; d < tick_pos_v.size(); d++ )
	{
	    result[ d ] = ( (Double)tick_pos_v.elementAt( d ) ).doubleValue();
	}

	return result;
    }


    // =================================================================================================
    // state
    // =================================================================================================

    private ListSelectionListener axis_list_sel_listener;

    private JList axis_list;
    private JPanel atts_panel;

    private JTextField min_jtf, max_jtf, title_jtf;

    private maxdView mview;
    private DataPlot dplot;
    protected PlotAxis[] axes;
    protected Vector listeners;


}
