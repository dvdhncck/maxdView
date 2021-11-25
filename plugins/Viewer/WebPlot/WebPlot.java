import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Vector;
import javax.swing.event.*;
import java.awt.dnd.*;

import java.awt.print.*;

//
//  draw profiles along either radial or parallel axes
//

public class WebPlot implements ExprData.ExprDataObserver, Plugin
{
    public WebPlot(maxdView mview_)
    {
	mview = mview_;
    }

    private void buildGUI()
    {
	parallel_axes = mview.getBooleanProperty("WebPlot.parallel_axes", false);
	use_filter = mview.getBooleanProperty("WebPlot.use_filter", true);
	uniform_scaling  = mview.getBooleanProperty("WebPlot.uniform_scaling", false);
	mouse_tracking = mview.getBooleanProperty("WebPlot.mouse_tracking", false);

	frame = new JFrame("Web Plot");

	mview.decorateFrame(frame);

	text_col       = mview.getTextColour();
	background_col = mview.getBackgroundColour();

	frame.getContentPane().setLayout(new BorderLayout());

	// ===== axis =====================================================================================

	axis_man = new AxisManager(mview);

	axis_man.addAxesListener( new AxisManager.AxesListener()
	    {
		public void axesChanged() 
		{
		    bg_image = null;
		    panel.repaint();
		}
	    });

	// ===== decorations ===============================================================================

	deco_man = new DecorationManager(mview, "WebPlot");

	deco_man.addDecoListener( new DecorationManager.DecoListener()
	    {
		public void decosChanged() 
		{
		    bg_image = null;
		    panel.repaint();
		}
	    });

	// ===== top =====================================================================================

	JToolBar tool_bar = new JToolBar();
	tool_bar.setFloatable(false);
	JLabel label = new JLabel("Labels ");
	tool_bar.add(label);

	final NameTagSelector nts = new NameTagSelector(mview);
	tool_bar.add(nts);
	nts.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
			nt_sel = nts.getNameTagSelection();
			refresh();
		}
	    });
	

	JButton ax_jb = new JButton("Axes");
	ax_jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    axis_man.startEditor();
		}
	    });
	tool_bar.add(ax_jb);


	JButton dec_jb = new JButton("Decorations");
	dec_jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    deco_man.startEditor();
		}
	    });

	tool_bar.add(dec_jb);

	JButton prn_jb = new JButton("Print");
	prn_jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    new PrintManager( mview, panel, panel ).openPrintDialog();
		}
	    });

	tool_bar.add(prn_jb);
	
	frame.getContentPane().add(tool_bar, BorderLayout.NORTH);


	// ===== panel ====================================================================================

	panel = new WebDrawPanel();

	panel.setDropAction(new DragAndDropPanel.DropAction()
	    {
		public void dropped(DragAndDropEntity dnde)
		{
		    System.out.println("Drop!");

		    try
		    {
			int sid = dnde.getSpotId();
		    }
		    catch(DragAndDropEntity.WrongEntityException wee)
		    {
		    }
		}
	    });
	panel.setDragAction(new DragAndDropPanel.DragAction()
	    {
		public DragAndDropEntity getEntity(DragGestureEvent event)
		{
		    System.out.println("drag start!");
		    
		    if(mouse_tracking)
			if(mouse_is_valid)
			    return DragAndDropEntity.createSpotNameEntity(mouse_spot);

		    return null;
		}
	    });


	frame.getContentPane().add(panel);

	// ===== bottom ==================================================================================

	JToolBar bottom_tool_bar = new JToolBar();
	bottom_tool_bar.setFloatable(false);

	{
	    bottom_tool_bar.addSeparator();
	    GridBagLayout gridbag = new GridBagLayout();
	    bottom_tool_bar.setLayout(gridbag);
	    
	    
	    {
		JCheckBox jcb= new JCheckBox("Parallel axes");
		bottom_tool_bar.add(jcb);
		jcb.setSelected(parallel_axes);
		jcb.setHorizontalTextPosition(AbstractButton.RIGHT);
		jcb.addActionListener(new ActionListener()
				      {
					  public void actionPerformed(ActionEvent e) 
					  {
					      JCheckBox source = (JCheckBox) e.getSource();
					      parallel_axes = source.isSelected();
					      positionSpokes();
					      refresh();
					  }
				      });
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = c.weighty = 1.0;
		gridbag.setConstraints(jcb, c);

	    }
	    {
		JCheckBox jcb= new JCheckBox("Uniform scaling");
		bottom_tool_bar.add(jcb);
		jcb.setSelected(uniform_scaling);
		jcb.setHorizontalTextPosition(AbstractButton.RIGHT);
		jcb.addActionListener(new ActionListener()
				      {
					  public void actionPerformed(ActionEvent e) 
					  {
					      JCheckBox source = (JCheckBox) e.getSource();
					      uniform_scaling = source.isSelected();
					      computeScales();
					      positionSpokes();
					      refresh();
					  }
				      });
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = c.weighty = 1.0;
		gridbag.setConstraints(jcb, c);

	    }
	    {
		JCheckBox jcb= new JCheckBox("Apply filter");
		bottom_tool_bar.add(jcb);
		jcb.setSelected(use_filter);
		jcb.setHorizontalTextPosition(AbstractButton.RIGHT);
		jcb.addActionListener(new ActionListener()
				      {
					  public void actionPerformed(ActionEvent e) 
					  {
					      JCheckBox source = (JCheckBox) e.getSource();
					      use_filter = source.isSelected();
					      refresh();
					  }
				      });
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = c.weighty = 1.0;
		gridbag.setConstraints(jcb, c);
	    }

	    {
		JCheckBox jcb= new JCheckBox("Mouse tracking");
		bottom_tool_bar.add(jcb);
		jcb.setSelected(mouse_tracking);
		jcb.setHorizontalTextPosition(AbstractButton.RIGHT);
		jcb.addActionListener(new ActionListener()
				      {
					  public void actionPerformed(ActionEvent e) 
					  {
					      JCheckBox source = (JCheckBox) e.getSource();
					      mouse_tracking = source.isSelected();
					      refresh();
					  }
				      });
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 1;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = c.weighty = 1.0;
		gridbag.setConstraints(jcb, c);
	    }

	    {
		final JButton jb = new JButton("Help");
		bottom_tool_bar.add(jb);
		jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					 {
					     mview.getPluginHelpTopic("WebPlot", "WebPlot");
					 }
				     });
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 3;
		c.gridy = 0;
		c.anchor = GridBagConstraints.EAST;
		c.weightx = c.weighty = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(jb, c);
	    }
	    {
		final JButton jb = new JButton("Close");
		bottom_tool_bar.add(jb);
		jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					 {
					     cleanUp();
					 }
		    });
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 3;
		c.gridy = 1;
		c.anchor = GridBagConstraints.EAST;
		c.weightx = c.weighty = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		gridbag.setConstraints(jb, c);
	    }
	    
	}

	frame.getContentPane().add(bottom_tool_bar, BorderLayout.SOUTH);
	panel.setPreferredSize(new Dimension(400, 400));

	/*
	Timer timer = new Timer(750, 
				new ActionListener() 
				{
				    public void actionPerformed(ActionEvent evt) 
				    {
					if(filter_alert == true) 
					{ 
					    if(edata.filterIsOn())
					    {
						panel.drawFilterAlert(true);
						filter_alert = false;
					    }
					}
					else
					{
					    panel.drawFilterAlert(false);
					    filter_alert = true;
					}
					
					if(auto_levelize == true)
					{
					    levelize();
					}
				    }    
				});
	timer.start();
	*/
    }

    public void cleanUp()
    {
	mview.putBooleanProperty("WebPlot.parallel_axes",   parallel_axes);
	mview.putBooleanProperty("WebPlot.use_filter",      use_filter);
	mview.putBooleanProperty("WebPlot.uniform_scaling", uniform_scaling);
	mview.putBooleanProperty("WebPlot.mouse_tracking",  mouse_tracking);

	frame.setVisible(false);
	edata.removeObserver(this);
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
	buildGUI();

	initialise();
	
	frame.addWindowListener(new WindowAdapter() 
	    {
		public void windowClosing(WindowEvent e)
		{
		    cleanUp();
		}
	    });
	
	frame.pack();
	frame.setVisible(true);

	// register ourselves with the data
	//
	edata.addObserver(this);
    }

    public void stopPlugin()
    {
	cleanUp();
    }

    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("Web Plot", "viewer", "Display data on a spoked wheel", "", 1, 0, 0);
	return pinf;
    }

    public void runCommand(String name, String[] args, CommandSignal done) 
    { 
	if(name.equals("start"))
	{
	    startPlugin();
	}
	if(name.equals("stop"))
	{
	    cleanUp();
	}
	if(done != null)
	    done.signal();
    } 

    public PluginCommand[] getPluginCommands()
    {
	PluginCommand[] com = new PluginCommand[2];
	com[0] = new PluginCommand("start", null);
	com[1] = new PluginCommand("stop", null);
	return com;
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
	switch(due.event)
	{
	case ExprData.ColourChanged:
	case ExprData.OrderChanged:
	    panel.repaint();
	    break;
	case ExprData.SizeChanged:
	case ExprData.ValuesChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	case ExprData.RangeChanged:
	case ExprData.VisibilityChanged:
	    bg_image = null;
	    panel.repaint();
	    break;
	}
    }
    
    public void clusterUpdate(ExprData.ClusterUpdateEvent cue)
    {
	switch(cue.event)
	{
	case ExprData.ColourChanged:
	case ExprData.OrderChanged:
	case ExprData.VisibilityChanged:
	case ExprData.SizeChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	    bg_image = null;
	    panel.repaint();
	    break;
	}
    }
    
    public void measurementUpdate(ExprData.MeasurementUpdateEvent mue)
    {
	switch(mue.event)
	{
	case ExprData.VisibilityChanged:
	case ExprData.SizeChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	    initialise();
	    panel.repaint();
	    break;
	case ExprData.NameChanged:
	    bg_image = null;
	    panel.repaint();
	    break;
	case ExprData.OrderChanged:
	    initialise();
	    panel.repaint();
	    break;
	}
    }
    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
	text_col       = mview.getTextColour();
	background_col = mview.getBackgroundColour();

	bg_image = null;
	panel.repaint();
    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   WebDrawPanel
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void initialise()
    {
	
	edata = mview.getExprData();
	dplot = mview.getDataPlot();

	n_visible_meas = 0;

	nt_sel = edata.new NameTagSelection();
	
	bg_image = null;

	for(int s=0;s< edata.getNumMeasurements(); s++)
	{
	    if(edata.getMeasurementShow(s) == true)
		n_visible_meas++;
	}

	// work out which measurement is in which spoke
	meas_in_spoke = new int[n_visible_meas];
	int mis = 0;
	for(int m=0; m< edata.getNumMeasurements(); m++)
	{
	    int mi = edata.getMeasurementAtIndex(m);
	    if(edata.getMeasurementShow(mi) == true)
	    
		meas_in_spoke[mis++] = mi;
	}
	
	if(n_visible_meas > 0)
	{
	    spoke_angle = 360.0 / n_visible_meas;
	}

	axis_man.removeAllAxes();
	
	for(int a=0; a < n_visible_meas; a++)
	{
	    axis_man.addAxis(new PlotAxis(mview, ("A" + a)));
	}

	computeScales();

    }

    // ======================================================================================
    // 
    // ======================================================================================

    public void computeScales()
    {
	if(uniform_scaling == true)
	{
	    for(int m=0; m < meas_in_spoke.length; m++)
	    {
		axis_man.setComputedRange(m, edata.getMinEValue(), edata.getMaxEValue());
	    }
	}
	else
	{
	    for(int spoke=0; spoke < meas_in_spoke.length; spoke++)
	    {
		double min = edata.getMeasurementMinEValue(meas_in_spoke[spoke]);
		double max = edata.getMeasurementMaxEValue(meas_in_spoke[spoke]);

		axis_man.setComputedRange(spoke, min, max);
		
	    }
	}

	axis_man.updateEditor();
    }


    public void positionSpokes()
    {
	positionSpokes( panel.getWidth(), panel.getHeight() );
    }

    public void positionSpokes(int pw, int ph)
    {
	//System.out.println("doing layout for " + meas_in_spoke.length + " spokes...");
	//System.out.println("panel is " +  pw + "x" +  ph);
	
	double shortest_spoke = axis_man.shortestLength();
	double spoke_offset  = (1.0 - shortest_spoke) / 2;
	
	if(parallel_axes)
	{
	    // for parallel axes
	    spoke_gap = (int) (((double) pw * shortest_spoke) / (double)(meas_in_spoke.length-1));
	    spoke_off = (int) (((double) pw) * spoke_offset);
	    spoke_len = ph - (2 * spoke_off);
	}
	else
	{
	    // for radial axes
	    spoke_len = (double) (pw < ph ? pw/2 : ph/2);
	    spoke_len *= shortest_spoke;
	    spoke_angle = 360.0 / meas_in_spoke.length;
	}
	
	Point2D.Double end_pt = new Point2D.Double( .0, -spoke_len );

	Point2D.Double trans  = new Point2D.Double( (double)(pw / 2) , (double)(ph / 2));
	
	spoke_start = new Point2D.Double[ meas_in_spoke.length ];
	spoke_delta = new Point2D.Double[ meas_in_spoke.length ];

	spoke_iscale = new double[ meas_in_spoke.length ];

	double angle = .0;

	for(int spoke=0; spoke < meas_in_spoke.length; spoke++)
	{
	    spoke_iscale[spoke] = 1.0 / (axis_man.getMax(spoke) - axis_man.getMin(spoke));

	    if(parallel_axes)
	    {
		spoke_start[spoke] = new Point2D.Double(spoke_off + (spoke_gap * spoke), spoke_off);
		
		spoke_delta[spoke] = new Point2D.Double(.0, spoke_len);
	    }
	    else
	    {
		Point2D.Double spoke_end_pt = rotatePoint( end_pt, angle );
		
		spoke_start[spoke] = new Point2D.Double((spoke_end_pt.x * spoke_offset) + trans.x, 
							(spoke_end_pt.y * spoke_offset) + trans.y);
		
		spoke_delta[spoke] = new Point2D.Double(spoke_end_pt.x * shortest_spoke, 
							spoke_end_pt.y * shortest_spoke);
		
		angle += spoke_angle;
	    }

	    //System.out.println("spoke " + spoke + ": s=" + spoke_start[spoke].x + "," + spoke_start[spoke].y +
	    //         	         " d=" +  spoke_delta[spoke].x + "," + spoke_delta[spoke].y);
	}
    }

    private Point findPosOnSpoke( int spoke, double frac )
    {
	return new Point ( (int) (spoke_start[spoke].x + ( spoke_delta[spoke].x * frac ) ),
			   (int) (spoke_start[spoke].y + ( spoke_delta[spoke].y * frac ) ));
    }
    
    // rotate a point
    //
    Point2D.Double rotatePoint(Point2D.Double p, double angle)
    {
	double rx, ry;

	double ang = angle * 0.017452;   // deg -> rad

	rx  = (p.x * Math.cos(ang)) - (p.y * Math.sin(ang));
	ry  = (p.x * Math.sin(ang)) + (p.y * Math.cos(ang)); 
	
	//System.out.println(p.x + "," + p.y + " -> " + rx + "," + ry);

	return new Point2D.Double( rx, ry );
    }

    // generates an angle (0..359) between the 2 points
    //   with 0 degs at 12 o'clock, 90 degs at 3 o'clock, 180 degs at 6 o'clock and so on...
    //
    double angleAt(Point p, Point o)
    {
	Point vec = new Point(p.x - o.x, p.y - o.y);
	if(vec.x == 0)
	{
	    return (vec.y > 0) ? 180 : 0;
	}
	else
	{
	    double o_over_a = (double) vec.y / (double) vec.x;
	    double ang = (Math.atan(o_over_a) * 57.29578); // rad -> deg
	    return (vec.x >= 0) ? (ang + 90.0) : (ang + 270.0);
	}
    }
    
    // generates a normalised (0..1) position along the spoke  
    //
    private final double scaleEValue(double evalue, int spoke)
    {
	return (evalue - axis_man.getMin(spoke)) * spoke_iscale[spoke];
    }


    public void refresh()
    {
	panel.drawAxes(mouse_tracking, panel.getWidth(), panel.getHeight());
	panel.repaint();
    }
	

    // ======================================================================================
    // 
    // ======================================================================================

    public class WebDrawPanel extends DragAndDropPanel 
	implements MouseListener, MouseMotionListener, java.awt.print.Printable
    {
	private Point last_pt = null;

	public WebDrawPanel()
	{
	    super(true);
	    addMouseListener(this);
	    addMouseMotionListener(this);
	}

	public void mouseMoved(MouseEvent e) 
	{
	    if(!mouse_tracking)
		return;

	    Point m_pt = new Point(e.getX(), e.getY());
		
	    if(parallel_axes)
	    {
		int near_spoke = ((m_pt.x - spoke_off) / spoke_gap);
		if(near_spoke >= meas_in_spoke.length)
		    near_spoke = meas_in_spoke.length - 1;

		if((m_pt.x - ((spoke_off + (near_spoke * spoke_gap)))) < 5.0)
		{
		    // System.out.println("  nearest spoke is " + edata.getMeasurementName(meas_in_spoke[near_spoke]));
		    
		    mouse_spoke = near_spoke;
		    mouse_is_valid = true;
		    mouse_pt = m_pt;
		    repaint();
		}
	    }
	    else
	    {
		// which is the spoke nearest the mouse?
		
		// get the angle of the mouse position w.r.t the middle
		//
		Point o_pt = new Point(getWidth()/2, getHeight()/2);
		double ang = angleAt(m_pt, o_pt);
		
		// System.out.println("mouse angle is " + ang);
		
		int near_spoke = (int) ((ang + (spoke_angle / 2)) / spoke_angle);
		
		if(near_spoke >= meas_in_spoke.length)
		    near_spoke = meas_in_spoke.length - 1;
		
		int near_mi = meas_in_spoke[near_spoke]; //edata.getMeasurementAtIndex(near_spoke);
		
		// how far are we from the spoke?
		double ang_to_spoke = Math.abs(ang - (spoke_angle * (double)near_spoke));
		
		// System.out.println("  nearest spoke is " + edata.getMeasurementName(near_mi));
		
		if(ang_to_spoke < 5.0)
		{
		    
		    mouse_is_valid = true;
		    mouse_spoke = near_spoke;
		    mouse_pt = m_pt;
		    repaint();
		}
		else
		{
		    mouse_is_valid = false;
		}
	    }
	    
	    
	}

	public void mouseDragged(MouseEvent e) 
	{
	} 

	String[] selected_spots = null;

	class customMenuListener implements ActionListener
	{
	    public void actionPerformed(ActionEvent e) 
	    {
		JMenuItem source = (JMenuItem)(e.getSource());
		{
		    System.out.println("menu event for " + source.getText());
		    
		    if((source.getText().equals("[ All ]")) || 
		       (source.getText().equals("[ Both ]")))
		    {
			mview.genesSelected(selected_spots);
		    }
		    else
		    {
			mview.geneSelected(source.getText());
		    }
		}
	    }
	}

	public void mousePressed(MouseEvent e) 
	{
	    //System.out.println("mouse press");
	    
	    if (e.isPopupTrigger() || e.isControlDown()) 
	    {
		if(near_pt_lines != null)
		{
		    JPopupMenu popup = new JPopupMenu();
		    customMenuListener menu_listener = new customMenuListener();
		    Point pt = new Point();
		    
		    int n_lines = near_pt_lines.size();
		    
		    if(n_lines < 20)
		    {
			selected_spots = new String[ n_lines ];
			
			for(int i=0; i < n_lines; i++)
			{
			    int sid = ((Integer)near_pt_lines.elementAt(i)).intValue();
			    String n_str = nt_sel.getNameTag(sid);
			    if(n_str == null)
				n_str = "(no label for Spot '" + edata.getSpotName(sid) + "')";
			    selected_spots[i] = n_str;
			    JMenuItem item = new JMenuItem(selected_spots[i]);
			    item.addActionListener(menu_listener);
			    popup.add(item);
			}		    
			
			if( n_lines > 1)
			{
			    popup.addSeparator();
			    JMenuItem item = new JMenuItem();
			    item.setText((n_lines == 2) ? "[ Both ]" : "[ All ]");
			    item.addActionListener(menu_listener);
			    popup.add(item);
			    
			}
		    }
		    else
		    {
			// no action listener, so it doesn't do anything
			//
			JMenuItem item = new JMenuItem("[ too many ]");
			popup.add(item);
			selected_spots = null;
		    }
		    popup.show(e.getComponent(),
			       e.getX(), e.getY());
		    
		    System.out.println("there are " + near_pt_lines.size() + " lines here");
		}
		else
		{
		    System.out.println("no nearby lines");
		}
	    }
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

	public void mouseClicked(MouseEvent e) {}

	/*
	public void drawRubberBox()
	{
	    Graphics graphic = getGraphics();
	    graphic.setXORMode(text_col);
	    graphic.drawRect(drag_start.x, drag_start.y, drag_size.x, drag_size.y);
	}

	public void drawFilterAlert(boolean draw_it)
	{
	    Graphics gr = getGraphics();
	    if(gr != null)
	    {
		gr.setColor(draw_it ? Color.red : background_col);
		gr.fillRect(getWidth() - 20, 10, 10, 10);
	    }
	}
	*/

	/*
	private Vector drawLines(Graphics graphic, boolean near_val, int near_spoke, Point near_pt)
	{
	    if(parallel_axes)
		drawParallelLines( graphic,  near_val,  near_spoke,  near_pt);
	    else
		drawAngledLines( graphic,  near_val,  near_spoke,  near_pt);

	}
	*/

	//  lines are drawn in grey, unless
	//    near_val is true, and the line is near 'near_pt' on spoke 'near_spoke'
	//    near_val is true, and near_pt is null
	//
	//    if in near_pt mode, then a list of the lines near the point is returned
	//
	private Vector drawLines(Graphics graphic, boolean near_val, int near_spoke, Point near_pt)
	{
	    Vector near_lines = null;
	    
	    graphic.setColor(Color.gray);

	    double angle = .0;
	    double eval;
	    double scale;

	    Point tpt = null;

	    boolean mouse_hit = false;

	    Point last  = null;
	    Point first = null;
	    Point mid   = null;
	    
	    int first_spoke = 0;
	    int last_spoke = 0;
	    double last_eval = .0;
	    double first_eval = .0;
	    
	    //System.out.println(" repaint......");

	    // now draw the data lines
	    //
	    for(int spot=0; spot < edata.getNumSpots(); spot++)
	    {
		if((use_filter == false) || (!edata.filter(spot)))
		{
		    //System.out.println("    drawing spot " + spot + " = " + edata.getProbeName(spot));

		    
		    angle = .0;
		    last  = null;
		    first = null;
		    mid = new Point();
		    
		    first_spoke = 0;
		    last_spoke = 0;
		    last_eval = .0;
		    first_eval = .0;
		    
		    // test whether this spot passes near the specified point
		    //
		    mouse_hit = false;
		    if(near_val)
		    {
			if(near_pt == null)
			{
			    mouse_hit = true;
			}
			else
			{
			    if(mouse_spoke >= 0)
			    {
				angle = ((double) mouse_spoke) * spoke_angle;
				
				eval = edata.eValue(meas_in_spoke[mouse_spoke], spot);
				
				scale = scaleEValue(eval, mouse_spoke);
				
				tpt = findPosOnSpoke( mouse_spoke, scale );

				double dist = (double)(((tpt.x - near_pt.x) * (tpt.x - near_pt.x)) + 
						       ((tpt.y - near_pt.y) * (tpt.y - near_pt.y)));
				if(dist < 9.0)
				{
				    mouse_hit = true;
				    
				    if(near_lines == null)
				    {
					// this is the first nearest spot
					near_lines = new Vector();
					
					mouse_spot = spot;
				    }
				    near_lines.addElement(new Integer(spot));
				    
				//System.out.println(" -> hit in spot " + spot + " = " + edata.getProbeName(spot));
				}
			    }
			}
		    }
		    
		    angle = .0;
		    
		    //for(int set=0;set<edata.getNumMeasurements();set++)
		    for(int spoke=0;spoke<meas_in_spoke.length;spoke++)
		    {
			int mi = meas_in_spoke[spoke];
			
			{
			    eval = edata.eValue(mi, spot);
			    
			    scale = scaleEValue(eval, spoke);
			    
			    // System.out.println("spoke=" + spoke + " e=" + eval + " s=" + scale);

			    tpt = findPosOnSpoke( spoke, scale );

			    if(first == null)
			    {
				first = tpt;
				first_spoke = spoke;
				first_eval = eval;
			    }
			    if(last != null)
			    {
				// find the mid point....
				mid.x = (last.x + tpt.x) / 2;
				mid.y = (last.y + tpt.y) / 2;
				
				if(near_val)
				{
				    if(mouse_hit)
				    {
					// draw the first half using the last sets colour
					graphic.setColor(dplot.getDataColour(last_eval, last_spoke));
					graphic.drawLine(last.x,  last.y, mid.x, mid.y);
					// draw the second half using this set's colour
					graphic.setColor(dplot.getDataColour(eval, mi));
					graphic.drawLine(tpt.x,  tpt.y, mid.x, mid.y);
				    }
				}
				else
				{
				    // draw using the default colour
				    graphic.drawLine(last.x,  last.y, mid.x, mid.y);
				    graphic.drawLine(tpt.x,  tpt.y, mid.x, mid.y);
				}
			    }
			    angle += spoke_angle;
			    last = tpt;
			    last_spoke = mi;
			    last_eval = eval;
			}
		    }
		    
		    // now draw the final pair of line sgements 
		    //
		    if(!parallel_axes)
		    {
			mid.x = (last.x + first.x) / 2;
			mid.y = (last.y + first.y) / 2;
			
			if(near_val)
			{
			    if(mouse_hit)
			    {
				// the second half of the last set...
				//
				graphic.setColor(dplot.getDataColour(edata.eValue(last_spoke, spot), 
								     last_spoke));
				graphic.drawLine(last.x, last.y, mid.x,  mid.y);
				
				// the first half of the first set
				//
				graphic.setColor(dplot.getDataColour(edata.eValue(first_spoke, spot), 
								     first_spoke));
				graphic.drawLine(mid.x,  mid.y, first.x, first.y);
			    }
			}
			else
			{
			    graphic.drawLine(last.x, last.y, mid.x,  mid.y);
			    graphic.drawLine(mid.x,  mid.y, first.x, first.y);
			}
		    }
		}
	    }
	    
	    if(near_val && (near_pt != null))
	    {
		// draw a circle show the targeting area
		if(mouse_tracking)
		{
		    graphic.setColor(text_col);
		    graphic.drawOval(near_pt.x - 3, near_pt.y - 3, 6, 6);
		}

		if((near_lines != null) && (near_lines.size() > 0))
		{
		    // if there is enough space, write the spot names...
		    FontMetrics fm = graphic.getFontMetrics();
		    int font_height = fm.getAscent();
		    boolean more_not_shown = false;
		    graphic.setColor(text_col);
		    int max_lines = (getHeight() / font_height)-1;
		    if (near_lines.size() < max_lines)
			max_lines = near_lines.size();
		    else
			more_not_shown = true;
		    
		    int xp = 5;
		    int yp = font_height + 5;
		    for(int i=0; i < max_lines; i++)
		    {
			//int spot = ((Integer)near_pt_lines.elementAt(i)).intValue();
			//selected_spots[i] = dplot.getTrimmedSpotLabel(spot);

			int sid = ((Integer)near_lines.elementAt(i)).intValue();

			if((i == (max_lines-1)) && more_not_shown)
			{
			    graphic.drawString(" [ . . . M O R E . . . ]", xp, yp);
			}
			else
			{
			    String n_str = nt_sel.getNameTag(sid);
			    if(n_str == null)
				n_str = "(no label for Spot '" + edata.getSpotName(sid) + "')";
			   graphic.drawString( n_str , xp, yp);
			}
			yp += font_height;
		    }
		}
		
	    }
	    
	    
	    return near_lines;
	}
	
	private void drawAxes(boolean grey_out, int pw, int ph)
	{
	    bg_image = createImage(pw, ph);

	    if(bg_image == null)
		return;

	    Graphics graphic = bg_image.getGraphics();

	    graphic.setColor(background_col);
	    graphic.fillRect(0, 0, pw, ph);

	    if(n_visible_meas > 0)
	    {
		
		if(grey_out)
		    drawLines(graphic, false, 0, null);
		else
		    drawLines(graphic, true,  0, null);
		
		double angle = .0;
		
		Point end_tpt = null;
		Point start_tpt = null;
		
		int set_name_pos = 0;
		int label_dx = 0;
		
		//System.out.println("center = " + center.x + "," + center.y);
		
		for(int spoke=0; spoke < meas_in_spoke.length; spoke++)
		{
		    int mi = meas_in_spoke[spoke];
		    
		    start_tpt = findPosOnSpoke( spoke, 0 );
		    end_tpt   = findPosOnSpoke( spoke, 1 );
		    
		    axis_man.drawAxis( graphic, spoke, start_tpt, end_tpt );
		    
		    angle += spoke_angle;
		    
		}
	    }

	    deco_man.drawDecorations(graphic, pw, ph);
	}

	public void paintComponent(Graphics graphic)
	{
	    if(graphic == null)
		return;

	    paintIntoRegion(graphic, getWidth(), getHeight());
	}
	
	private void paintIntoRegion(Graphics graphic, int pw, int ph)
	{
	    if(meas_in_spoke.length < 2)
	    {
		graphic.setColor(background_col);
		graphic.fillRect(0, 0, pw, ph);

		return;
	    }

	    if(bg_image == null)
	    {
		positionSpokes();
		drawAxes(mouse_tracking, pw, ph);
	    }
	    else
	    {
		// has this window resized?
		if((bg_image.getWidth(null) != pw) || (bg_image.getHeight(null) != ph))
		{
		    positionSpokes(pw, ph);
		    drawAxes(mouse_tracking, pw, ph);
		}
	    }

	    graphic.drawImage(bg_image, 0, 0, null /* ImageObserver */ );

	    if(mouse_is_valid)
	    {
		//System.out.println("  trying for points near " + mouse_pt + " in spoke " + mouse_spoke);
		near_pt_lines = drawLines(graphic, true, mouse_spoke, mouse_pt);
	    }
	    else
	    {
		near_pt_lines = null;
	    }

	    
	}

	public int print(Graphics g, PageFormat pf, int pg_num) throws PrinterException 
	{
	    // margins
	    //
	    g.translate((int)pf.getImageableX(), 
			(int)pf.getImageableY());
	    
	    // area of one page
	    //
	    int pw = (int)pf.getImageableWidth();
	    int ph = (int)pf.getImageableHeight();
	    
	    // System.out.println("PRINT REQUEST for page " + pg_num + " size=" + pw + "x" + ph); 
	    
	    panel.paintIntoRegion(g, pw, ph);

	    return (pg_num > 0) ? NO_SUCH_PAGE : PAGE_EXISTS;
	}
	
    }

    private maxdView mview;
    private ExprData edata;
    private DataPlot dplot;

    private ExprData.NameTagSelection nt_sel;

    private JFrame frame;

    private boolean mouse_is_valid = false;    // true when the mouse is near a spoke
    private int   mouse_spoke;                 // which spoke is the mouse near?
    private Point mouse_pt;                    // what the actual position of the mouse
    private int   mouse_spot;                  // the spot nearest the mouse_pt

    private boolean parallel_axes      = false;
    private boolean use_filter         = true;
    private boolean uniform_scaling    = false;
    private boolean mouse_tracking     = false;

    private Vector near_pt_lines = null;

    private int      n_visible_meas = 0;
    private double   spoke_angle = .0;
    private Point    spoke_center = new Point(0,0);

    private int      spoke_off = 0;
    private int      spoke_gap = 0;
    private double   spoke_len = .0;

    private Point2D.Double[] spoke_start;
    private Point2D.Double[] spoke_delta;
    private double[] spoke_iscale;

    private int[] meas_in_spoke = null;    // which set is in which spoke
    
    private boolean filter_alert = false;

    private JLabel status_label;

    private Color background_col;
    private Color text_col;

    private Image bg_image = null;

    private WebDrawPanel panel;

    private AxisManager axis_man;

    private DecorationManager deco_man;

}
