import java.io.*;

import javax.swing.*;
import javax.swing.event.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;

import java.awt.font.*;
import java.awt.geom.*;
import java.awt.dnd.*;

import java.util.*;

import javax.swing.tree.*;
import javax.swing.event.*;

public class ProfilePanel extends DragAndDropPanel implements MouseListener, MouseMotionListener, Printable
{
    final static int n_sel_colours = 8;

    public ProfilePanel( final maxdView mview, final ExprData edata, final NewProfileViewer viewer )
    {
	super();
	
	this.viewer = viewer;
	this.mview = mview;
	this.edata = edata;

	addMouseListener(this);
	addMouseMotionListener(this);

	background_col = mview.getBackgroundColour();
	text_col       = mview.getTextColour();

	sel_colour = text_col.brighter();
	unsel_colour = text_col.darker();

    }


    public void update( final SpotPicker spot_picker, final MeasPicker meas_picker )
    {
	Selection sel_spot = spot_picker.getSelection();
	Selection sel_meas = meas_picker.getSelection();
	
	if( sel_meas == null )
	    return;
	
	if( sel_spot == null )
	    return;
	
	
	System.out.println( "Spots: " + sel_spot.getSize() );
	System.out.println( "Measurements: " + sel_meas.getSize() );
	
	// what do we need to plot?
	
	if( ( sel_spot.getSize() == 0 ) || ( sel_meas.getSize() == 0 ) )
	    // nothing (result!)
	    return;


	// how many points are in each profile?
	// 
	int num_data_points_per_profile = meas_picker.getNumDataPointsPerProfile();

	System.out.println( "ProfileSize: " + num_data_points_per_profile );

	GraphInfo[] graphs = new GraphInfo[ 1 ];

	graphs[ 0 ] = constructGraph( sel_spot, sel_meas, num_data_points_per_profile );
		
	
	repaint();
    }
    
    
    
    
    // ========================================================================================
    //
    // graph construction
    //
    //  ( a separate step from graph drawing so that repainting is as fast as possible )
    //
    // ========================================================================================
    
    
    private GraphInfo constructGraph( Selection sel_spot, Selection sel_meas, int num_data_points_per_profile )
    {
	
	// scan the data to find the range
	
	double min = Double.MAX_VALUE;
	double max = Double.MIN_VALUE;
	
	// work out which data we need to consider
	
/*
	int spot_start = ( spot_group == -1 ) ? 0 : spot_group;
	int spot_end   = ( spot_group == -1 ) ? sel_spot.getCount() : ( spot_group + 1 );
	
	int meas_start = ( meas_group == -1 ) ? 0 : meas_group;
	int meas_end   = ( meas_group == -1 ) ? sel_meas.getCount() : ( meas_group + 1 );
	
	
	for( int m = meas_start; m < meas_end; m++ )
	{
	    int[] m_ids = sel_meas.getIDs( m );
	    
	    for( int m2 = 0; m2 < m_ids.length; m2++ )
	    {
		for( int s = spot_start; s < spot_end; s++ )
		{
		    int[] s_ids = sel_spot.getIDs( s );
		    
		    for( int s2 = 0; s2 < s_ids.length; s2++ )
		    {
			final double value = edata.eValue( m_ids[ m2 ], s_ids[ s2 ] );
			
			if( value < min )
			    min = value;
			
			if( value > max )
			    max = value;
		    }
		}
	    }
	}
	
	// work out how many profiles this graph will contain
	
	int total_profiles = 0;
	
	for( int m = meas_start; m < meas_end; m++ )
	{
	    for( int s = spot_start; s < spot_end; s++ )
	    {
		int[] s_ids = sel_spot.getIDs( s );
		
		total_profiles += s_ids.length;
	    }
	}
	
	System.out.println( total_profiles + " profiles, min=" + min + ", max=" + max );
*/
	
	for( int m = 0; m < sel_meas.getSize(); m++ )
	{
	    final int m_id = sel_meas.getID( m );

	    for( int s = 0; s < sel_spot.getSize(); s++ )
	    {
		final double value = edata.eValue( m_id, sel_spot.getID( s ) );
		
		if( value < min )
		    min = value;
		
		if( value > max )
		    max = value;
	    }
	}


	GraphInfo ginfo = new GraphInfo( min, max, num_data_points_per_profile );
	
	return ginfo;
    }
    
    
    
    // ========================================================================================
    //
    // mouse tracking
    //
    // ========================================================================================
    
    
    public void mouseMoved(MouseEvent e) 
    {
	Point pt = new Point();
	double xval, yval, xval_t, yval_t;

	pt.x  = e.getX();
	pt.y  = e.getY();

	/*
	  if(root_profile_picker != null)
	  {
	  int sid = root_profile_picker.findProfile(pt.x, pt.y);
	  if(sid >= 0)
	  {
	  nt_sel = nts.getNameTagSelection();
		    
	  String str = nt_sel.getNameTag(sid);
		   
	  tool_tip_text = str;
		    
	  setToolTipText(str);
	  }
	  else
	  {
	  tool_tip_text = null;
	  }
	  }
	*/
    }

    public String getToolTipText(MouseEvent event)
    {
	return tool_tip_text;
    }

    public void mouseDragged(MouseEvent e) 
    {
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
	
    public void paintComponent(Graphics graphic)
    {
	if( graphic == null )
	    return;
	
	if( ( graphs == null ) || ( graphs.length < 1 ) || ( graphs[ 0 ]  == null ) )
	    return;
	
	drawGraph( graphic, getWidth(), getHeight(), graphs[ 0 ] );
	
	/*
	if(sel_colours == null)
	{
	    sel_colours = new Color[n_sel_colours];
	    sel_colours[0] = Color.red;
	    sel_colours[1] = Color.green;
	    sel_colours[2] = Color.blue;
	    sel_colours[3] = Color.yellow;
	    sel_colours[4] = Color.magenta;
	    sel_colours[5] = Color.cyan;
	    sel_colours[6] = Color.pink;
	    sel_colours[7] = Color.white;
	}

	    
	( (Graphics2D) graphic ).setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
	    
	label_spacer = new LabelSpacer( getWidth(), getHeight() );

	drawProfiles(graphic, getWidth(), getHeight());
	*/


	    
    }


    public int print(Graphics g, PageFormat pf, int pg_num) throws PrinterException 
    {
	// margins
	//
	g.translate((int)pf.getImageableX(), 
		    (int)pf.getImageableY());
	    
	// area of one page
	//
	// ??  area seems to be too small, c.f. ScatterPlot...
	//
	int pw = (int)(pf.getImageableWidth() - pf.getImageableX());   
	int ph = (int)(pf.getImageableHeight() - pf.getImageableY());
	    
	//System.out.println("PRINT REQUEST for page " + pg_num + " size=" + pw + "x" + ph); 
	
	//frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

	//drawProfiles(g, pw, ph );

	//frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		
	// panel.paintIntoRegion(g, pw, ph);

	return (pg_num > 0) ? NO_SUCH_PAGE : PAGE_EXISTS;
    }


    public void drawGraph( Graphics graphic, int width, int height, GraphInfo graph_info )
    {
	try
	{
		
	    graphic.setColor( background_col );
	    graphic.fillRect( 0, 0, width, height );
	    graphic.setColor( text_col );
	    graphic.setFont( mview.getDataPlot().getFont() );
		
	    root_profile_picker = new ProfilePicker();
	    root_profile_picker.setupPicker( width, height );

	    graph_info.setScreenSize( (int)( (double)width * 0.8 ), (int)( (double)height * 0.8 ) );
	    graph_info.setOrigin( (int)( (double)width * 0.1 ), height - (int) ( (double)width * 0.1 ) );
	    

	    font = new Font("Helvetica", 1, /*(int)( (double)ticklen * scale * spot_font_scale)*/  14 );
	    frc = new FontRenderContext(null, false, false);
		    
	    Graphics2D g2 = (Graphics2D) graphic;
	    
	    g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );


	    // X axis (horizontal)
	    g2.drawLine( graph_info.origin_x, graph_info.origin_y, graph_info.origin_x+graph_info.width, graph_info.origin_y );

	    // Y axis (horizontal)
	    g2.drawLine( graph_info.origin_x, graph_info.origin_y, graph_info.origin_x, graph_info.origin_y-graph_info.height );

	    
	    // Y axis labels
	    drawLabel( g2,  graph_info.min,  graph_info.origin_x, graph_info.toScreenY( graph_info.min ), -90 );
	    
	    if( graph_info.min < 0.0 )
		drawLabel( g2, 0.0, graph_info.origin_x, graph_info.toScreenY( 0.0 ), -90 );
	    
	    drawLabel( g2, graph_info.max,  graph_info.origin_x, graph_info.toScreenY( graph_info.max ), -90 );
	    
	    
	}
	catch(Throwable th)
	{
	    th.printStackTrace();
	}
    }


    private void drawLabel( final Graphics2D g2, final double value, final int xp, final int yp, final int rotation )
    {
	final TextLayout tl = new TextLayout( mview.niceDouble( value, 9, 4 ), font, frc );
	final int offset = (int)(tl.getBounds().getWidth() / 2.0);
	final AffineTransform at = new AffineTransform();
	at.translate( xp-ticklen, yp+offset );
	at.rotate( Math.toRadians( rotation ), 0, 0 );
	g2.fill( tl.getOutline( at ) );
    }








// ============================================================================



    private void drawElementsInto( Graphics graphic, ExprData.Cluster cl, int[] ids, int xp, int yp, int w, int h )
    {
	if(show_mean)
	{
	    // calculate the mean....
	    drawMeanElementsInto( graphic, cl, ids, false, false, xp, yp, w, h );
	}
	else
	{
	    Vector show_spot_label = viewer.getLabelledSpots();

	    if(show_spot_label.size() > 0)
	    {
		// draw the selected spots on top of (i.e. after) the unselected ones
		    
		drawElementsInto( graphic, cl, ids, true, false, xp, yp, w, h );
		    
		drawElementsInto( graphic, cl, ids, true, true, xp, yp, w, h );
	    }
	    else
	    {
		drawElementsInto( graphic, cl, ids, false, false, xp, yp, w, h );
	    }
	}
    }
			 
    // ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 
			 
    private int colour_alloc = 0;
    private Color[] sel_colours = null;

    private void drawElementsInto( Graphics graphic,  ExprData.Cluster cl, int[] ids, 
				   boolean do_sel, boolean is_sel, 
				   int xp, int yp, int w, int h )
    {
	if((cl == null) || (cl.getIsSpot()))
	{
	    Graphics2D g2 = null;

	    int spot_label_w = 0;
	    int spot_label_h = 0;
	    int spot_label_o = 0;
	    TextLayout spot_label = null;
	    
	    for(int e=0; e < ids.length; e++)
	    {
		final int sid = ids[e];
		final int sel_colour_id = -1; // getSpotLabel( sid );
		final boolean spot_is_sel = (sel_colour_id >= 0);
		
		if( (do_sel == false) || (spot_is_sel == is_sel) )
		{
		    boolean show_label = spot_is_sel;
		    
		    String label = spot_is_sel ? viewer.getSpotLabel( sid ) : null;
		    
		    if((label == null) || (label.length() == 0))
			show_label = false;
		    
		    if(show_label)
		    {
			spot_label = new TextLayout( label, font, frc);
			spot_label_w = (int)spot_label.getBounds().getWidth();
			spot_label_h = (int)spot_label.getBounds().getHeight();
			spot_label_o = spot_label_w / 2;
			    
		    }

		    if( ( apply_filter == false) || ( ! edata.filter(sid) ) )
		    {
			int exp = xp;
			int last_eyp = 0;
			int last_exp = 0;
			    
			Color draw_col = text_col;

			if(do_sel)
			{
			    if( col_sel )
			    {
				//graphic.setColor( is_sel ? sel_colour : unsel_colour ); 
				if(is_sel)
				{
				    draw_col = sel_colours[ sel_colour_id ];
				}
				else
				    draw_col = unsel_colour; 
			    }
			    else
			    {
				draw_col = is_sel ? sel_colour : unsel_colour ; 
			    }
			}
			
			graphic.setColor( draw_col );
			if(g2 == null)
			    g2 = (Graphics2D) graphic;
			
			for(int m=0; m < meas_ids.length; m++)
			{
			    final double eval = edata.eValue(meas_ids[m], sid);
			    int eyp = yp + h - (int)((eval - graph_y_axis_min) * graph_y_axis_scale);
			    
			    
			    // System.out.println("m=" + m + " s=" + ids[e] + " y=" + eyp);
			    
			    
			    if(m > 0)
			    {
				if(root_profile_picker != null)
				    root_profile_picker.addSegment(last_exp, last_eyp, exp, eyp, sid);
				
				graphic.drawLine( last_exp, last_eyp, exp, eyp);
			    }
			    
			    if(show_label)
			    {
				// put the label underneath when the value is rising
				int label_y = eyp;
				int label_x = exp-spot_label_o;
				
				if((m+1) < meas_ids.length)
				{
				    if( eval < edata.eValue(meas_ids[m+1], sid) )
					label_y += spot_label_h;
				}
				else
				{
				    // put the last label underneath when the value is falling
				    if( eval < edata.eValue(meas_ids[m-1], sid) )
					label_y += spot_label_h;
				}
				
				if( label_spacer.spaceForLabel( 18, spot_label_w, spot_label_h, label_x, label_y ) )
				{
				    graphic.setColor( background_col );
				    graphic.fillRect( label_x, label_y-spot_label_h, spot_label_w, spot_label_h);
				    graphic.setColor( draw_col );
				    
				    AffineTransform new_at = new AffineTransform();
				    new_at.translate(label_x, label_y);
				    Shape shape = spot_label.getOutline(new_at);
				    g2.fill(shape);
				    
				    label_spacer.storeLabelExtent( spot_label_w, spot_label_h, label_x, label_y );
				}
			    }
			    
			    last_exp = exp;
			    last_eyp = eyp;
			    
			    
			    exp += graph_x_axis_step;
			}
		    }
		}
	    }
	}
    }
			 
			 
    // ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 
			 
    private void drawMeanElementsInto( Graphics graphic,  ExprData.Cluster cl, int[] ids, 
				       boolean do_sel, boolean is_sel, 
				       int xp, int yp, int w, int h )
    {
	if((cl == null) || (cl.getIsSpot()))
	{
	    Graphics2D g2 = null;

	    int spot_label_w = 0;
	    int spot_label_h = 0;
	    int spot_label_o = 0;
	    TextLayout spot_label = null;

	    final int n_spots = ids.length;
	    final int n_meas  = meas_ids.length;
		
	    if(n_spots == 0)
		return;

	    final double[] emean = new double[n_meas];
	    final double[] emin = new double[n_meas];
	    final double[] emax = new double[n_meas];

	    for(int m=0; m < n_meas; m++)
	    {
		emin[m] = Double.MAX_VALUE; 
		emax[m] = -Double.MAX_VALUE; 
	    }
		
	    // get mean, min and max for each Measurement
							  
	    for(int s=0; s < n_spots; s++)
	    {
		for(int m=0; m < n_meas; m++)
		{
		    final double eval = edata.eValue(meas_ids[m], ids[s]);
		    if( eval > emax[m] )
			emax[m] = eval;
		    if( eval < emin[m] )
			emin[m] = eval;
		    emean[m] += eval;
		}
	    }
	    for(int m=0; m < n_meas; m++)
	    {
		emean[m] /= (double) n_spots;
	    }
		
		
		
	    {
		int exp = xp;
		int last_eyp = 0;
		int last_exp = 0;
		    
		Color draw_col = text_col;
		    
		graphic.setColor( draw_col );
		    
		for(int m=0; m < n_meas; m++)
		{
		    int eyp = yp + h - (int)((emean[m] - graph_y_axis_min) * graph_y_axis_scale);

		    // System.out.println("m=" + m + " s=" + ids[e] + " y=" + eyp);
			
		    if(m > 0)
		    {
			graphic.drawLine( last_exp, last_eyp, exp, eyp);
		    }
			
		    int min_yp = yp + h - (int)((emin[m] - graph_y_axis_min) * graph_y_axis_scale);
		    int max_yp = yp + h - (int)((emax[m] - graph_y_axis_min) * graph_y_axis_scale);
			
		    // draw the error bars
		    graphic.drawLine( exp, min_yp, exp, max_yp );
		    graphic.drawLine( exp-1, min_yp, exp+1, min_yp );
		    graphic.drawLine( exp-1, max_yp, exp+1, max_yp );
			
		    // and update for the next meas
		    last_exp = exp;
		    last_eyp = eyp;
			
		    exp += graph_x_axis_step;
		}
	    }
	}
    }
	
    // ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 
	
    private void drawAxes( Graphics graphic,  int xp, int yp, int w, int h, double min, double max )
    {
	graphic.drawRect( xp, yp, w, h );
	    
	Shape shape = null;
	TextLayout tl = null;
	    
	//int eyp = yp + h - (int)((edata.eValue(meas_ids[m], ids[e]) - graph_y_axis_min) * graph_y_axis_scale);
	int miny = yp + h;
	int maxy = yp;
	int zeroy = yp + h - (int)((0 - graph_y_axis_min) * graph_y_axis_scale);
	    
	graphic.drawLine( xp, miny, xp-ticklen, miny );
	    
	if(uniform_scale)
	{
	    tl = min_label;
	}
	else
	{
	    //tl = min_label;
	    tl = new TextLayout( mview.niceDouble( min, 9, 4 ), font, frc );
	    min_label_o = (int)(tl.getBounds().getWidth() / 2.0);
	}
	AffineTransform new_at = new AffineTransform();
	new_at.translate(xp-ticklen, miny+min_label_o);
	new_at.rotate(Math.toRadians(-90), 0, 0);
	shape = tl.getOutline(new_at);
	Graphics2D g2 = (Graphics2D) graphic;
	g2.fill(shape);
	    
	if(uniform_scale)
	{
	    tl = max_label;
	}
	else
	{
	    //tl = max_label;
	    tl = new TextLayout( mview.niceDouble( max, 9, 4 ), font, frc );
	    max_label_o = (int)(tl.getBounds().getWidth() / 2.0);
	}
	new_at = new AffineTransform();
	new_at.translate(xp-ticklen, maxy+max_label_o);
	new_at.rotate(Math.toRadians(-90), 0, 0);
	shape = tl.getOutline(new_at);
	g2.fill(shape);
	graphic.drawLine( xp, maxy, xp-ticklen, maxy );

	if(draw_zero)
	{
	    //if(spaceForLabel( ticklen, (zero_label_o*2), xp-ticklen, zeroy+zero_label_o ))
	    {
		new_at = new AffineTransform();
		new_at.translate(xp-ticklen, zeroy+zero_label_o);
		new_at.rotate(Math.toRadians(-90), 0, 0);
		shape = zero_label.getOutline(new_at);
		g2.fill(shape);
		graphic.drawLine( xp, zeroy, xp-ticklen, zeroy );
		//storeLabelExtent( ticklen, (zero_label_o*2), xp-ticklen, zeroy+zero_label_o );
	    }
	}

	// measurement ticks & labels 
	int exp = xp;
	int eyp = yp+h+ticklen;
	int etyp = eyp+ticklen;
	
	for(int m=0; m < meas_ids.length; m++)
	{
	    graphic.drawLine( exp, yp+h, exp, eyp );

	    if( label_spacer.spaceForLabel( 18, meas_labels_o[m]*2, ticklen, exp-meas_labels_o[m], etyp ))
	    {
		new_at = new AffineTransform();
		new_at.translate(exp-meas_labels_o[m], etyp);
		shape = meas_labels[m].getOutline(new_at);
		g2.fill(shape);

		label_spacer.storeLabelExtent( meas_labels_o[m]*2, ticklen, exp-meas_labels_o[m], etyp );
	    }
	    
	    exp += graph_x_axis_step;
	}
    }



    private maxdView mview;
    private ExprData edata;

    private NewProfileViewer viewer;


    private GraphInfo[] graphs = null;


    // drawing with selection
    private Color sel_colour, unsel_colour;

    private boolean coloured = false;

    private Color background_col;

    private Color text_col;


    private ProfilePicker root_profile_picker;


    private TextLayout min_label, zero_label, max_label;
    private TextLayout[] meas_labels;
    private int  min_label_o, zero_label_o, max_label_o;
    private int[] meas_labels_o;
    private Font font;
    private FontRenderContext frc;

    private double spot_font_scale = 1.0;

    private boolean draw_zero = false;

    private boolean apply_filter;

    private Vector sel_cls;       // the selected clusters
    private Vector sel_cl_ids;    // the element_ids for the selected clusters

    private int[]  meas_ids;

 
    private int n_cols;
    private int n_rows;

    private int graph_w;
    private int graph_h;

    private int ticklen = 10;

    private int graph_sx;   // step size between graphs
    private int graph_sy;

    private int xoff, yoff;

    private int graph_x_axis_step;
    private int graph_y_axis_step;

    private double graph_y_axis_scale;
    private double graph_y_axis_min;

    private Point last_pt = null;
    
    private double scale = 1.0;
    
    private Polygon[] glyph_poly = null;
    private int glyph_poly_height;

    private String tool_tip_text = null;

    private boolean show_mean = false;

    private LabelSpacer label_spacer;
			 
    private boolean col_sel = false;

    private boolean uniform_scale = false;
}
	    
