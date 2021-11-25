import javax.swing.*;
import javax.swing.event.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.print.*;

public class GraphPanel extends JPanel implements MouseListener, MouseMotionListener, Printable
{
    public GraphPanel()
    {
	h_axis = new GraphAxis( GraphAxis.HORIZONTAL, GraphAxis.OUTSIDE );
	v_axis = new GraphAxis( GraphAxis.VERTICAL, GraphAxis.OUTSIDE );

	gc = new GraphContext(h_axis, v_axis);
	
	setPreferredSize(new Dimension(256,256));
	setMinimumSize(new Dimension(64, 64));
	
	
	
	/*
	xticks = yticks = null;
	xlog = ylog = false;
	xtick_ndp = ytick_ndp = 0;
	xtick_mode = ytick_mode = 0;
	*/

	addMouseMotionListener(this);
	addMouseListener(this);
    }
    

    public GraphAxis getHorizontalAxis() { return h_axis; }
    public GraphAxis getVerticalAxis() { return v_axis; }
    public GraphContext getContext() { return gc; }


    // ======================================================
    //
    // the implementable of the Printable interface
    //
    // ======================================================

    public int print(Graphics g, PageFormat pf, int pg_num) throws PrinterException 
    {
	g.translate((int)pf.getImageableX(), 
		    (int)pf.getImageableY());
	
	paint( g );
	
	return ( pg_num > 0 ) ? NO_SUCH_PAGE : PAGE_EXISTS;
    }

    // ======================================================
    

    
    public void paintComponent(Graphics graphic)
    {
	if(graphic == null)
	    return;

	
	Graphics2D g2 = (Graphics2D) graphic;
	g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, 
			     RenderingHints.VALUE_ANTIALIAS_ON );


	// System.out.println("repainting");

	graphic.setColor( gc.background_col );
	graphic.fillRect( 0, 0, getWidth(), getHeight() );
	
	gc.setCurrentFont( graphic , new Font("Helvetica", Font.PLAIN, 12 ) );

	final int n_plots = gc.plots.size();

	if(n_plots == 0)
	    return;

	FontRenderContext frc = new FontRenderContext(null, false, false);

	int title_h = 0;
	if(gc.title != null)
	{
	    Rectangle2D title_r = gc.title_font.getStringBounds(gc.title, frc);
	    title_h = (int)title_r.getHeight();
	}

	int h_labelh = h_axis.getRequiredHeight( gc );
	int v_labelw = v_axis.getRequiredWidth( gc );


	h_axis_tl_x = gc.gap + v_labelw;
	h_axis_tl_y = (int)getHeight() - (gc.gap + h_labelh);
	
	h_axis_h = h_labelh;

	v_axis_tl_x = gc.gap;
	v_axis_tl_y = gc.gap + title_h + gc.gap;

	v_axis_w = v_labelw;

	graph_tl_x = gc.gap + v_labelw;
	graph_tl_y = gc.gap + title_h + gc.gap;
	
	graph_h = getHeight() - (3*gc.gap) - h_labelh - title_h;
	graph_w = getWidth() - (2*gc.gap) - v_labelw;

	int legend_h = 0;
	int legend_w = 0;

	int legend_tl_x = 0;
	int legend_tl_y = 0;

	int n_things_in_legend = gc.getNumNamedPlots();

	int legend_sample_size = 10;

	if(n_things_in_legend > 0)
	{
	    Rectangle2D legend_r = gc.legend_font.getStringBounds("A", frc);
	    legend_sample_size = (int) legend_r.getHeight();
	    
	    switch(legend_format)
	    {
	    case 0:
		legend_h =  legend_w = 0;
		break;

	    case 1: // tall

		legend_r = gc.legend_font.getStringBounds(gc.getLongestPlotName() + " ", frc);

		legend_h = n_things_in_legend * (int)legend_r.getHeight() + (2 * gc.gap);
		legend_w = gc.gap + (int)legend_r.getWidth() + legend_sample_size;
		break;

	    case 2: // wide
		legend_r = gc.legend_font.getStringBounds(gc.getConcatenatedPlotNames(), frc);
		
		legend_h = (int)legend_r.getHeight() + (2 * gc.gap);
		legend_w = (int)legend_r.getWidth() + 
                           (n_things_in_legend * legend_sample_size) + 
		           ((n_things_in_legend-1) * gc.gap) + (2*gc.gap);
		break;
	    }

	    
	   switch( gc.legend_horizontal_align )
	    {
	    case 0: // left
		gc.leg_pos_x = 0.0;
		break;
	    case 1: // middle
		gc.leg_pos_x = 0.5;
		break;
	    case 2: // right
		gc.leg_pos_x = 1.0;
		break;
	    }
	
	   switch( gc.legend_vertical_align )
	   {
	    case 0: // top
		gc.leg_pos_y = 1.0;
		break;
	    case 1: // middle
		gc.leg_pos_y = 0.5;
		break;
	    case 2: // bottom
		gc.leg_pos_y = 0.0;
		break;
	    }
		
	    
	    double leg_sx = (double)  graph_w - legend_w;
	    double leg_sy = (double)  graph_h - legend_h;
	    
	    legend_tl_x = graph_tl_x + (int)(gc.leg_pos_x * leg_sx );
	    legend_tl_y = graph_tl_y + (int)(leg_sy - (gc.leg_pos_y * leg_sy ));

	    
	    legend_tl_x = graph_tl_x + (int)(gc.leg_pos_x * leg_sx );
	    legend_tl_y = graph_tl_y + (int)(leg_sy - (gc.leg_pos_y * leg_sy ));
	}

	//System.out.println( "graph area is " + graph_w + "x" + graph_h + " @ " + graph_tl_x + "," + graph_tl_y );

	gc.setSizeAndPosition( graph_w, graph_h, graph_tl_x, graph_tl_y);

	v_axis_img = v_axis.getImage( this, gc, graph_h );

	int n_bar_charts = gc.getNumBarCharts();

	int bar_width = 0;
	int half_bar_width = 0;

	if( n_bar_charts > 0)
	{
	    int max_bars_in_any_plot = gc.getMaxBars();
	    bar_width = (int)( (double)graph_w / (double)( (n_bar_charts * (max_bars_in_any_plot+1)) /*+ 1*/ ) );
	    half_bar_width = bar_width / 2;
	    
	    h_axis.setPointWidth( bar_width * n_bar_charts );
	}

	h_axis_img = h_axis.getImage( this, gc, graph_w );

	int max_bars_in_any_plot = 0;

	int bar_chart_count = 0;
	int bar_chart_offset = 0;

	int line_plot_offset = (bar_width * n_bar_charts) / 2;
	
	//System.out.println( "V axis @ " + v_axis_tl_x + "," + v_axis_tl_y );
	
	//graphic.setColor(Color.green);
	//graphic.drawRect(v_axis_tl_x, v_axis_tl_y, v_labelw, graph_h);

	//graphic.setColor(Color.blue);
	//graphic.drawRect(h_axis_tl_x, h_axis_tl_y, graph_w, h_labelh);
	
	//graphic.setColor(Color.red);
	//graphic.drawRect(graph_tl_x, graph_tl_y, graph_w, graph_h);

		
	graphic.setColor( gc.background_col.darker() );


	if(gc.h_grid != null)
	{
	    int xl = graph_tl_x;
	    int xr = graph_tl_x + graph_w;

	    for(int h=0; h < gc.h_grid.length; h++)
	    {
		int y = graph_tl_y + (graph_h - v_axis.worldToAxis(gc.h_grid[h]));
		
		graphic.drawLine(xl, y, xr, y);
	    }
	}
	if(gc.v_grid != null)
	{
	    int yt = graph_tl_y;
	    int yb = graph_tl_y + graph_h;

	    for(int v=0; v < gc.v_grid.length; v++)
	    {
		int x = graph_tl_x + h_axis.worldToAxis(gc.v_grid[v]);
		
		graphic.drawLine(x, yt, x, yb);
	    }
	}

	for(int p=0; p < n_plots; p++)
	{
	    GraphPlot plot = (GraphPlot) gc.plots.elementAt(p);
	    
	    // System.out.println("t2");
	
	    graphic.setColor( plot.col );
	    graphic.setFont( gc.label_font );

	    switch(plot.type)
	    {
	    case 0: // bar graph
		for(int pt=0; pt < plot.xdata.length; pt++)
		{
		    int xp =  bar_chart_offset + graph_tl_x + h_axis.worldToAxis(plot.xdata[pt]);

		    int len = v_axis.worldToAxis(plot.ydata[pt]);
		    int yp = graph_tl_y + (graph_h - len);
		    
		    
		    // System.out.println( plot.xdata[pt] + " = " + plot.ydata[pt] );
		    
		    graphic.fillRect( xp, yp, bar_width, len);
		}
		
		bar_chart_count++;
		bar_chart_offset += bar_width;
		
		break;

	    case 1: // line plot
	    
		{ 
		    int lx = 0;
		    int ly = 0;
		    
		    // final int bar_width = (int)( (double)gc.width / (double)(plot.xdata.length+1) ) - 1;
		    
		    
		    for(int pt=0; pt < plot.xdata.length; pt++)
		    {
			int xp = line_plot_offset + graph_tl_x + h_axis.worldToAxis(plot.xdata[pt]);
			int yp = graph_tl_y + (graph_h - v_axis.worldToAxis(plot.ydata[pt]));
			
			
			if(pt > 0)
			{
			    graphic.drawLine( lx, ly, xp, yp );
			}

			lx = xp;
			ly = yp;

			if( plot.glyph != GraphPlot.NO_GLYPH )
			    drawGlyph( graphic, xp, yp, plot.glyph );
			
			if( plot.label != null )
			{
			    if( pt < plot.label.length )
			    {
				Rectangle2D bounds = gc.label_font.getStringBounds( plot.label[ pt ], frc);
				
				graphic.drawString( plot.label[ pt ],
						    xp - (int) bounds.getWidth() / 2,
						    yp + (int) bounds.getHeight() );
			    }
			}
		    }
		}

	    case 2: // scatter plot

		//if( plot.label != null )
		//    System.out.println( "scatterplot: " + plot.label.length + " labels defined...");

	        for(int pt=0; pt < plot.xdata.length; pt++)
		{
		    int xp = line_plot_offset + graph_tl_x + h_axis.worldToAxis(plot.xdata[pt]);
		    int yp = graph_tl_y + (graph_h - v_axis.worldToAxis(plot.ydata[pt]));
		    
		    
		    if( plot.glyph != GraphPlot.NO_GLYPH )
			drawGlyph( graphic, xp, yp, plot.glyph );



		    if( plot.label != null )
		    {
			if( pt < plot.label.length )
			{
			    Rectangle2D bounds = gc.label_font.getStringBounds( plot.label[ pt ], frc);
			    
			    graphic.drawString( plot.label[ pt ],
						xp - (int) bounds.getWidth() / 2,
						yp + (int) bounds.getHeight() );

			    //System.out.println( pt + " = " + plot.label[ pt ] );
			}
		    }
		    
		    /*
		    graphic.fillRect( xp, yp, 1, 1 );

		    switch( plot.glyph )
		    {
		    case GraphPlot.BOX_GLYPH:
			graphic.drawRect( xp-2, yp-2, 5, 5 );
			break;
			
		    case GraphPlot.CIRCLE_GLYPH:
			graphic.drawOval( xp-2, yp-2, 5, 5 );
			    break;
			    
		    case GraphPlot.CROSS_GLYPH:
			graphic.drawLine( xp-5, yp, xp+5, yp );
			graphic.drawLine( xp, yp-5, xp, yp+5 );
			break;
		    }
		    */
		}

	    }
	}

	if(h_axis_img != null) 
	    graphic.drawImage( h_axis_img, h_axis_tl_x, h_axis_tl_y, null );
	if(v_axis_img != null) 
	    graphic.drawImage( v_axis_img, v_axis_tl_x, v_axis_tl_y, null );


	// and the title
	
	if(gc.title != null)
	{
	    Rectangle2D title_r = gc.title_font.getStringBounds(gc.title + " ", frc);

	    int ty = gc.gap + (int)title_r.getHeight() ;
	    int tx = ((getWidth() - (2*gc.gap)) - (int)title_r.getWidth()) / 2;
	    
	    graphic.setFont( gc.title_font );

	    graphic.setColor(gc.foreground_col);
	    graphic.drawString(gc.title, tx, ty);
	}

	// and legend

	if((legend_format > 0) && (n_things_in_legend > 0))
	{
	    graphic.setFont( gc.legend_font );

	    graphic.setColor(gc.background_col);
	    graphic.fillRect( legend_tl_x, legend_tl_y, legend_w-1, legend_h-1 );
	    
	    //graphic.setColor(Color.orange);
	    //graphic.drawRect( legend_tl_x, legend_tl_y, legend_w, legend_h );
	    
	    
	    int sx = legend_tl_x + gc.gap;

	    int rx = sx + legend_sample_size;
	    int ry = legend_tl_y + legend_sample_size + gc.gap;

	    Rectangle2D legend_r = gc.legend_font.getStringBounds("A", frc);
	    
	    for(int p=0; p < n_plots; p++)
	    {
		GraphPlot plot = (GraphPlot) gc.plots.elementAt(p);
		if(plot.name != null)
		{
		    graphic.setColor(plot.col);

		    if(plot.type == 0)
		    {
			// bar graph
			graphic.fillRect( rx - legend_sample_size, ry - (int)legend_r.getHeight(), legend_sample_size, (int)legend_r.getHeight());
		    }
		    else
		    {
			// line plot
			graphic.drawLine( rx - legend_sample_size, ry - (int)legend_r.getHeight(), rx, ry );
			int mx = rx - (legend_sample_size/2);
			int my = ry - ((int)legend_r.getHeight()/2);
			graphic.drawRect( mx-1, my-1, 3, 3 );
		   
		    }

		    graphic.drawString( plot.name, rx + gc.gap, ry );
		    
		    if(legend_format == 1)
		    {
			ry += (int)legend_r.getHeight();
		    }
		    else
		    {
			legend_r = gc.legend_font.getStringBounds(plot.name, frc);

			rx += legend_r.getWidth() + legend_sample_size + gc.gap;
		    }
		}
	    }
	}



    }
    
    // ======================================================
    
    int[] glyph_data_x = new int[ 4 ];
    int[] glyph_data_y = new int[ 4 ];
    
    private void drawGlyph( Graphics graphic, int xp, int yp, int glyph_code )
    {
	switch( glyph_code )
	{
	    case GraphPlot.BOX_GLYPH:
		graphic.drawRect( xp-1, yp-1, 3, 3 );
		break;
		
	    case GraphPlot.CIRCLE_GLYPH:
		graphic.drawOval( xp-1, yp-1, 3, 3 );
		break;
		
	    case GraphPlot.CROSS_GLYPH:
		graphic.drawLine( xp-5, yp, xp+5, yp );
		graphic.drawLine( xp, yp-5, xp, yp+5 );
		break;
		
	    case GraphPlot.DIAMOND_GLYPH:
		glyph_data_x[ 0 ] = xp-5;  glyph_data_y[ 0 ] = yp;
		glyph_data_x[ 1 ] = xp;  glyph_data_y[ 1 ] = yp-5;
		glyph_data_x[ 2 ] = xp+5;  glyph_data_y[ 2 ] = yp;
		glyph_data_x[ 3 ] = xp;  glyph_data_y[ 3 ] = yp+5;
		graphic.drawPolyline( glyph_data_x, glyph_data_y, 4 );
		break;
		
	    case GraphPlot.FILLED_BOX_GLYPH:
		graphic.fillRect( xp-1, yp-1, 3, 3 );
		break;
		
	    case GraphPlot.FILLED_CIRCLE_GLYPH:
		graphic.fillOval( xp-1, yp-1, 3, 3 );
		break;
		
	    case GraphPlot.FILLED_DIAMOND_GLYPH:
		glyph_data_x[ 0 ] = xp-5;  glyph_data_y[ 0 ] = yp;
		glyph_data_x[ 1 ] = xp;  glyph_data_y[ 1 ] = yp-5;
		glyph_data_x[ 2 ] = xp+5;  glyph_data_y[ 2 ] = yp;
		glyph_data_x[ 3 ] = xp;  glyph_data_y[ 3 ] = yp+5;
		graphic.fillPolygon( glyph_data_x, glyph_data_y, 4 );
		break;
	}
    }
    
    // ======================================================
    
    // custom tool tips
    
    /*
    public JToolTip createToolTip()
    {
	if(custom_tool_tip_singleton == null)
	    custom_tool_tip_singleton = new CustomToolTip();
	return custom_tool_tip_singleton;
    }

    private CustomToolTip custom_tool_tip_singleton;

    private class CustomToolTip extends JToolTip
    {
       public void setTipText(String text1_, String text2_) 
	{
	    text1 = text1_;
	    text2 = text2_;

	    max_h = 80;
	    max_w = 200;
	    setMinimumSize( new Dimension( max_w, max_h ));
	    setPreferredSize( new Dimension( max_w, max_h ));
	}
	
	public void paintComponent(Graphics graphic)
	{
	    graphic.setColor( new Color(100,100,200) );
	    graphic.fillRect( 0, 0, getWidth(), getHeight() );
	    graphic.setColor( Color.black );
	    graphic.drawString( text1, 10, 40 );
	    graphic.drawString( text2, 10, 70 );
	}

	String text1;
	String text2;
	int max_h, max_w;
    }
    */


    // ======================================================


    /*
    public void paintV_AxisWithMovingTick( Graphics graphic, Image im, int pos, double val)
    {
	graphic.drawImage( im, 0, pos, null );
    }
    */

    // ======================================================

    
    public void mouseMoved(MouseEvent e)
    {
	/*
	CustomToolTip ctt = (CustomToolTip) createToolTip();

	ctt.setTipText( "x=" + e.getX(), "y=" + e.getX() );

	setToolTipText ("dummy");
	*/

	Graphics graphic  = null;

	FontRenderContext frc = new FontRenderContext(null, false, false);
	
	int x = e.getX();
	if(h_axis.mouse_tracking && ((x > h_axis_tl_x) && (x < (h_axis_tl_x+graph_w))))
	{
	    Rectangle2D tick_r = h_axis.tick_font.getStringBounds("0", frc);
	    int th = (int) tick_r.getHeight();

	    graphic = getGraphics();

	    if(h_axis_img != null) 
	    {
		graphic.drawImage( h_axis_img, h_axis_tl_x, h_axis_tl_y, null );
		
		double val = h_axis.axisToWorld( x - h_axis_tl_x );
		String val_str = NiceDouble.valueOf( val, 20,  h_axis.ndp );

		if( val_str.indexOf('E') < 0 )
		{
		    tick_r = h_axis.tick_font.getStringBounds(val_str, frc);
		    int len = (int)tick_r.getWidth();
		    
		    graphic.setColor( gc.foreground_col );
		    
		    int yp = h_axis_tl_y;
		    
		    // clip the label to the side edges
		    int x_label_pos = (x-h_axis_tl_x)-(len/2);
		    if(x_label_pos < 0)
			x_label_pos = 0;
		    if((x_label_pos+len) > graph_w)
			x_label_pos = graph_w - len;
		    x_label_pos += h_axis_tl_x;
		    
		    
		    graphic.setColor( gc.background_col );
		    graphic.fillRect( x_label_pos-1, yp+1, len+2, th + h_axis.tick_len );
		    
		    graphic.setColor( gc.foreground_col.brighter() );
		    graphic.drawLine( x, yp, x, yp + h_axis.tick_len );
		    
		    graphic.setFont( h_axis.tick_font );
		    graphic.drawString( val_str, x_label_pos, yp + h_axis.tick_len + th );
		}
	    }
	}
	    
	int y = e.getY();
	if(v_axis.mouse_tracking && ((y > v_axis_tl_y) && (y < (v_axis_tl_y+graph_h))))
	{
	    Rectangle2D tick_r = v_axis.tick_font.getStringBounds("0", frc);
	    int th = (int) tick_r.getHeight();

	    if(graphic == null)
		graphic = getGraphics();
	    
	    if(v_axis_img != null) 
	    {
		graphic.drawImage( v_axis_img, v_axis_tl_x, v_axis_tl_y, null );
		
		double val = v_axis.axisToWorld( graph_h - (y - v_axis_tl_y) );
		String val_str = NiceDouble.valueOf( val, 20,  v_axis.ndp );

		if( val_str.indexOf('E') < 0 )
		{
		    tick_r = v_axis.tick_font.getStringBounds(val_str, frc);
		    int len = (int) tick_r.getWidth();
		    
		    int xp = v_axis_tl_x + v_axis_w - 1;
		    
		    // clip the label to the side edges
		    int y_label_pos = (y-v_axis_tl_y)+(th/2);
		    if(y_label_pos < th)
			y_label_pos = th;
		    if((y_label_pos) > graph_h)
			y_label_pos = graph_h;
		    y_label_pos += v_axis_tl_y;
		    
		    graphic.setColor( gc.background_col );
		    graphic.fillRect( xp-len-v_axis.tick_len, y_label_pos-th-1, len+v_axis.tick_len, th+2 );
		    
		    graphic.setColor( gc.foreground_col.brighter() );
		    graphic.drawLine( xp, y, xp-v_axis.tick_len, y );
		    
		    graphic.setFont( v_axis.tick_font );
		    graphic.drawString( val_str, xp-len-v_axis.tick_len, y_label_pos );
		}
	    }
	}
    }
    
    
    public void mouseDragged(MouseEvent e){}
    
    public void mousePressed(MouseEvent e) 
    {
	// display the options dialog

	displayOptions();
    }
    
    public void mouseReleased(MouseEvent e) {}
    
    public void mouseClicked(MouseEvent e) {} 

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) 
    {
	Graphics graphic = getGraphics();
	if(h_axis_img != null)
	    graphic.drawImage( h_axis_img, h_axis_tl_x, h_axis_tl_y, null );
	if(v_axis_img != null)
	    graphic.drawImage( v_axis_img, v_axis_tl_x, v_axis_tl_y, null );
    }
    
    // ======================================================

    public void displayOptions()
    {
	if(options_frame == null)
	    makeOptionsFrame();
	
	syncGUIWithOptions();

	options_frame.setVisible(true);
    }


    public void syncOptionsWithGUI()
    {
	if(components == null)
	    return;

	if( ((JRadioButton) components.get("legend:format:hidden")).isSelected() )
	    legend_format = 0;
	if( ((JRadioButton) components.get("legend:format:tall")).isSelected() )
	    legend_format = 1;
	if( ((JRadioButton) components.get("legend:format:wide")).isSelected() )
	    legend_format = 2;
	
	if( ((JRadioButton) components.get("legend:vertical:top")).isSelected() )
	    gc.legend_vertical_align = 0;
	if( ((JRadioButton) components.get("legend:vertical:middle")).isSelected() )
	    gc.legend_vertical_align = 1;
	if( ((JRadioButton) components.get("legend:vertical:bottom")).isSelected() )
	    gc.legend_vertical_align = 2;

	if( ((JRadioButton) components.get("legend:horizontal:left")).isSelected() )
	    gc.legend_horizontal_align = 0;
	if( ((JRadioButton) components.get("legend:horizontal:middle")).isSelected() )
	    gc.legend_horizontal_align = 1;
	if( ((JRadioButton) components.get("legend:horizontal:right")).isSelected() )
	    gc.legend_horizontal_align = 2;

	gc.legend_font = ((FontSelector) components.get("legend:font")).getFont();

	gc.title_font = ((FontSelector) components.get("title:font")).getFont();

	gc.h_axis.title_font = ((FontSelector) components.get("Horizontal:title:font")).getFont();
	gc.h_axis.tick_font  = ((FontSelector) components.get("Horizontal:tick:font")).getFont();

	gc.v_axis.title_font = ((FontSelector) components.get("Vertical:title:font")).getFont();
	gc.v_axis.tick_font  = ((FontSelector) components.get("Vertical:tick:font")).getFont();
	
	repaint();

    }

    public void syncGUIWithOptions()
    {
	if(components == null)
	    return;

	((JRadioButton) components.get("legend:format:hidden")).setSelected( legend_format == 0 );
	((JRadioButton) components.get("legend:format:tall")).setSelected(   legend_format == 1 );
	((JRadioButton) components.get("legend:format:wide")).setSelected(   legend_format == 2 );
	
	((JRadioButton) components.get("legend:vertical:top")).setSelected(    gc.legend_vertical_align == 0 );
	((JRadioButton) components.get("legend:vertical:middle")).setSelected( gc.legend_vertical_align == 1 );
	((JRadioButton) components.get("legend:vertical:bottom")).setSelected( gc.legend_vertical_align == 2 );

	((JRadioButton) components.get("legend:horizontal:left")).setSelected(   gc.legend_horizontal_align == 0 );
	((JRadioButton) components.get("legend:horizontal:middle")).setSelected( gc.legend_horizontal_align == 1 );
	((JRadioButton) components.get("legend:horizontal:right")).setSelected(  gc.legend_horizontal_align == 2 );

	((FontSelector) components.get("legend:font")).setFont(gc.legend_font);

	((FontSelector) components.get("title:font")).setFont(gc.title_font);

	((FontSelector) components.get("Horizontal:title:font")).setFont(gc.h_axis.title_font);
	((FontSelector) components.get("Vertical:title:font")).setFont(gc.v_axis.title_font);

	((FontSelector) components.get("Horizontal:tick:font")).setFont(gc.h_axis.tick_font);
	((FontSelector) components.get("Vertical:tick:font")).setFont(gc.v_axis.tick_font);
    }

    private void makeOptionsFrame()
    {
	options_frame = new JFrame("Graph Options");
	JPanel panel = new JPanel();
	JPanel wrapper = null;
	    
	panel.setPreferredSize(new Dimension(400,300));

	GridBagLayout gridbag = new GridBagLayout();
	panel.setLayout(gridbag);
	    
	panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

	GridBagConstraints c = null;
	Color title_colour = new JLabel().getForeground().brighter();	    
	JLabel label = null;
	
	components = new java.util.Hashtable();

	JTabbedPane tabbed = new JTabbedPane();

	{
	    // legend options

	    wrapper = new JPanel();
	    wrapper.setBorder(BorderFactory.createEmptyBorder(5,15,5,15));

	    GridBagLayout wrapbag = new GridBagLayout();
	    wrapper.setLayout(wrapbag);
	
	    ButtonGroup bg;
	    JRadioButton jrb;
	    int line = 0;

	    label = new JLabel("Format");
	    add( wrapper, wrapbag, label, 0, line++, GridBagConstraints.WEST );

	    bg = new ButtonGroup();
	    jrb = new JRadioButton("Hidden");
	    bg.add(jrb);
	    add( wrapper, wrapbag, jrb, 0, line );
	    components.put("legend:format:hidden", jrb);

	    jrb = new JRadioButton("Tall");
	    bg.add(jrb);
	    add( wrapper, wrapbag, jrb, 1, line );
	    components.put("legend:format:tall", jrb);
	    
	    jrb = new JRadioButton("Wide");
	    bg.add(jrb);
	    add( wrapper, wrapbag, jrb, 2, line++ );
	    components.put("legend:format:wide", jrb);
	    
	    addSpace( wrapper, wrapbag,  3, line++, 15, 15 );

	    label = new JLabel("Vertical alignment");
	    add( wrapper, wrapbag, label, 0, line++, GridBagConstraints.WEST,  GridBagConstraints.NONE, 3, 1 );

	    bg = new ButtonGroup();
	    jrb = new JRadioButton("Top");
	    bg.add(jrb);
	    add( wrapper, wrapbag, jrb, 0, line, GridBagConstraints.WEST );
	    components.put("legend:vertical:top", jrb);
	    
	    jrb = new JRadioButton("Middle");
	    bg.add(jrb);
	    add( wrapper, wrapbag, jrb, 1, line, GridBagConstraints.WEST );
	    components.put("legend:vertical:middle", jrb);
	    
	    jrb = new JRadioButton("Bottom");
	    bg.add(jrb);
	    add( wrapper, wrapbag, jrb, 2, line++, GridBagConstraints.WEST );
	    components.put("legend:vertical:bottom", jrb);

	    addSpace( wrapper, wrapbag,  3, line++, 15, 15 );

	    label = new JLabel("Horizontal alignment");
	    add( wrapper, wrapbag, label, 0, line++, GridBagConstraints.WEST,  GridBagConstraints.NONE, 3, 1 );

	    bg = new ButtonGroup();
	    jrb = new JRadioButton("Left");
	    bg.add(jrb);
	    add( wrapper, wrapbag, jrb, 0, line, GridBagConstraints.WEST );
	    components.put("legend:horizontal:left", jrb);

	    jrb = new JRadioButton("Middle");
	    bg.add(jrb);
	    add( wrapper, wrapbag, jrb, 1, line, GridBagConstraints.WEST );
	    components.put("legend:horizontal:middle", jrb);
	    
	    jrb = new JRadioButton("Right");
	    bg.add(jrb);
	    add( wrapper, wrapbag, jrb, 2, line++, GridBagConstraints.WEST );
	    components.put("legend:horizontal:right", jrb);

	    addSpace( wrapper, wrapbag,  3, line++, 15, 15 );

	    FontSelector fsel = new FontSelector( "Labels", JSlider.HORIZONTAL, null );
	    add( wrapper, wrapbag, fsel, 0, line++, GridBagConstraints.WEST,  GridBagConstraints.NONE, 3, 1 );
	    components.put("legend:font", fsel );

    
	    tabbed.add( " Legend ", wrapper );
	}


	{
	    // title options
	    int line = 0;

	    wrapper = new JPanel();
	    wrapper.setBorder(BorderFactory.createEmptyBorder(5,15,5,15));

	    GridBagLayout wrapbag = new GridBagLayout();
	    wrapper.setLayout(wrapbag);

	    FontSelector fsel = new FontSelector( null, JSlider.HORIZONTAL, null );
	    add( wrapper, wrapbag, fsel, 0, 0 );

	    components.put("title:font", fsel );

	    tabbed.add( " Title ", wrapper );
	}



	for(int axis=0; axis < 2; axis++)
	{
	    // axis options

	    String aname = (axis == 0) ? "Horizontal" : "Vertical";

	    wrapper = new JPanel();
	    wrapper.setBorder(BorderFactory.createEmptyBorder(5,15,5,15));

	    GridBagLayout wrapbag = new GridBagLayout();
	    wrapper.setLayout(wrapbag);

	    FontSelector fsel = new FontSelector( "Tick labels", JSlider.HORIZONTAL, null );
	    add( wrapper, wrapbag, fsel, 0, 0 );
	    components.put(aname + ":tick:font", fsel );

	    addSpace( wrapper, wrapbag,  0, 1, 15, 15 );

	    fsel = new FontSelector( "Axis label", JSlider.HORIZONTAL, null );
	    add( wrapper, wrapbag, fsel, 0, 2 );
	    components.put(aname + ":title:font", fsel );

	    tabbed.add( " " + aname + " ", wrapper );
	}


	add( panel, gridbag, tabbed, 0, 0, GridBagConstraints.WEST, GridBagConstraints.BOTH, 10.0, 9.0 );
	

	{
	    // command buttons
	    
	    wrapper = new JPanel();
	    GridBagLayout wrapbag = new GridBagLayout();
	    wrapper.setLayout(wrapbag);

	    wrapper.setBorder(BorderFactory.createEmptyBorder(5,5,0,5));
    
	    JButton jb;
	    
	    jb = new JButton("Apply");
	    add( wrapper, wrapbag, jb, 0, 0, GridBagConstraints.EAST );
	    jb.addActionListener(new ActionListener()
		{
		    public void actionPerformed(ActionEvent evt) 
		    {
			syncOptionsWithGUI();
		    }
		});
	    jb = new JButton("Help");
	    jb.setEnabled(false);
	    add( wrapper, wrapbag, jb, 1, 0, GridBagConstraints.EAST );

	    jb = new JButton("Close");
	    add( wrapper, wrapbag, jb, 2, 0, GridBagConstraints.WEST );
	    jb.addActionListener(new ActionListener()
		{
		    public void actionPerformed(ActionEvent evt) 
		    {
			options_frame.setVisible(false);
		    }
		});
	}

	add( panel, gridbag, wrapper, 0, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH );

	options_frame.getContentPane().add( panel );
	options_frame.pack();
    }

    private void add( JComponent dest, GridBagLayout layout, JComponent src, 
		      int col, int row, 
		      int anchor, int fill, 
		      int gridwidth , int gridheight,
		      double weightx, double weighty )
    {
	GridBagConstraints c = new GridBagConstraints();
	c.gridx = col;
	c.gridy = row;
	if(anchor >= 0)
	    c.anchor = anchor;
	if(fill >= 0)
	    c.fill = fill;
	if(gridwidth > 0) 
	    c.gridwidth = gridwidth;
	if(gridheight > 0) 
	    c.gridheight = gridheight;
	if(weightx > .0)
	    c.weightx = weightx;
	if(weighty > .0)
	    c.weighty = weighty;
	layout.setConstraints(src, c);
	dest.add(src);
    }

    private void add( JComponent dest, GridBagLayout layout, JComponent src, 
		      int col, int row, 
		      int anchor, int fill, 
		      int gridwidth , int gridheight)
    {
	add( dest, layout, src, col, row, anchor, fill, gridwidth, gridheight, .0 ,.0);
    }

     private void add( JComponent dest, GridBagLayout layout, JComponent src, 
		      int col, int row, 
		      int anchor, int fill, 
		      double weightx, double weighty  )
    {
	add( dest, layout, src, col, row, anchor, fill, -1, -1, weightx, weighty);
    }

   private void add( JComponent dest, GridBagLayout layout, JComponent src, int col, int row, int anchor, int fill )
    {
	add( dest, layout, src, col, row, anchor, fill, -1, -1, .0, .0);
    }


    private void add( JComponent dest, GridBagLayout layout, JComponent src, int col, int row, int anchor )
    {
	add( dest, layout, src, col, row, anchor, -1, -1, -1, .0, .0 );
    }

    private void add( JComponent dest, GridBagLayout layout, JComponent src, int col, int row )
    {
	add( dest, layout, src, col, row, -1, -1, -1, -1, .0, .0 );
    }

    private void addSpace(JComponent dest, GridBagLayout layout, int col, int row, int w, int h )
    {
	Dimension fillsize = new Dimension(w, h);
	Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
	GridBagConstraints c = new GridBagConstraints();
	c.gridx = col;
	c.gridy = row;
	layout.setConstraints(filler, c);
	dest.add(filler);
    }


    JFrame options_frame = null;


    // ======================================================
    
    private int graph_tl_x, graph_tl_y, graph_h, graph_w;
    private int h_axis_tl_x, h_axis_tl_y, h_axis_h, v_axis_tl_x, v_axis_tl_y, v_axis_w;

    private Image v_axis_img, h_axis_img;

    private int legend_format = 1;
    
    private GraphContext gc;
    private GraphAxis h_axis, v_axis;

    private java.util.Hashtable components;
}
    
    
