import java.io.*;
import java.awt.*;
import java.awt.event.*;

public class GraphContext
{	
    public int top, bottom, left, right;
    public int width, height, xoff, yoff;

    public double[] h_grid;
    public double[] v_grid;
    /*
    public double xs, ys;
    public double ixs, iys;
    */

    public int gap = 5;
    
    public String title;
   
    //public int text_height;
    //public FontMetrics fm;
    
    public Color background_col;
    public Color foreground_col;
   
    public Font title_font;
    public Font legend_font;
    public Font label_font;

    public double xmax = -Double.MAX_VALUE;
    public double ymax = -Double.MAX_VALUE;
    
    public double xmin = Double.MAX_VALUE;
    public double ymin = Double.MAX_VALUE;
    
    public java.util.Vector plots = new java.util.Vector();
    private int next_id = 0;

    public GraphAxis h_axis;
    public GraphAxis v_axis;

    public double leg_pos_x, leg_pos_y;

    public boolean copy_data;

    public int legend_horizontal_align = 0;
    public int legend_vertical_align = 2;

    // ==============================================

    public GraphContext( GraphAxis ha, GraphAxis va )
    {
	h_axis = ha;
	v_axis = va;

	background_col = Color.white;
	foreground_col = Color.black;

	leg_pos_x = 1.0;
	leg_pos_y = 1.0;

	title_font = legend_font = new Font( "Helvetica", Font.PLAIN, 14 );

	label_font = new Font( "Helvetica", Font.PLAIN, 10 );

	copy_data = true;
    }

    public void setTitle( String title_ )
    {
	title = title_;
	//repaint();
    }

    public void setBorderGap( int gap_ )
    {
	gap = gap_;
	update();
    }
    
    public void setSizeAndPosition( int dw, int dh, int offx, int offy )  // world (ww, wh) and device (dw, dh) sizes
    {
	
	width  = dw - (2*gap);
	height = dh - (2*gap);
	
	top = gap;
	bottom = height - gap;
	
	left = gap;
	right = width - gap;

	xoff = offx + gap;
	yoff = offy + gap;
	
	/*
	xs = (width  / (double)width);
	ys = (height / (double)height);
	
	ixs = 1.0 / xs;
	iys = 1.0 / ys;
	*/

	update();
    }
    
    public void setBackgroundColour( Color col )
    {
	background_col = col;
	update();
    }

    public void setForegroundColour( Color col )
    {
	foreground_col = col;
	update();
    }

     public void setTitleFont( Font font_ )
    {
	title_font = font_;
	update();
    }

    public void setLegendFont( Font font_ )
    {
	legend_font = font_;
	update();
    }

    public void setLabelFont( Font font_ )
    {
	label_font = font_;
	update();
    }


   public void setGrid( double[] h_positions, double[] v_positions )
    {
	h_grid = h_positions;
	v_grid = v_positions;
    }
    public void removeGrid( )
    {
	h_grid = v_grid = null;
    }

    public void setAutoGrid( int n_grid_lines )
    {
	v_grid = generateGrid( xmin, xmax, n_grid_lines );
	h_grid = generateGrid( ymin, ymax, n_grid_lines );
    }
  
    public void setLegendAlignment( int lha, int lva )
    {
	legend_horizontal_align = lha;
	legend_vertical_align   = lva;
    }


    protected void setCurrentFont( Graphics graphic, Font font )
    {
	//if(font != null)
	//    graphic.setFont(font);

	//fm = graphic.getFontMetrics();
	//text_height = fm.getAscent();

	// update();
    }
    
    protected void setCurrentFont( Font font )
    {
	//Graphics graphic = Toolkit.getGraphics();
	//fm = graphic.getFontMetrics( font );
	//text_height = fm.getAscent();
    }


    private void update( )
    {
	/*
	*/
	
	/*
	xs = (double)w / xrange;
	ys = (double)h / yrange;
	*/
	/*
	xoff =  xlog ? (int)( safelog(xmin) * xs) : (int)(xmin * xs);
	yoff =  ylog ? (int)( safelog(ymin) * ys) : (int)(ymin * ys);
	*/
    }
    
    /*
    public int toX( double x)
    {
	
    }
    public int toY( double x)
    {
	
    }
    
    public double fromX( int x)
    {

    }
    public double fromY( int x)
    {
	
    }
    */
    /*
    public static final int X_AXIS = 0;
    public static final int Y_AXIS = 1;
    */

    // ======================================================
    
    public void setCopyData(boolean copy_)
    {
	copy_data = copy_;
    }

    

    // ======================================================


    public int addBarChart( String name, double[] xdata, double[] ydata, Color col )
    {
	return addPlot( 0, name, xdata, ydata, null, col, 0 );
    }
    public int addBarChart( double[] xdata, double[] ydata, Color col )
    {
	return addPlot( 0, null, xdata, ydata, null, col, 0 );
    }
    

    public int addLinePlot( String name, double[] xdata, double[] ydata, Color col, int glyph )
    {
	return addPlot( 1, name, xdata, ydata, null, col, glyph );
    }
    public int addLinePlot( String name, double[] xdata, double[] ydata, Color col )
    {
	return addPlot( 1, name, xdata, ydata, null, col, 0 );
    }


    public int addLinePlot( double[] xdata, double[] ydata, String[] label, Color col, int glyph )
    {
	return addPlot( 1, null, xdata, ydata, label, col, glyph );
    }
    public int addLinePlot( double[] xdata, double[] ydata, Color col, int glyph )
    {
	return addPlot( 1, null, xdata, ydata, null, col, glyph );
    }
    public int addLinePlot( double[] xdata, double[] ydata, Color col )
    {
	return addPlot( 1, null, xdata, ydata, null, col, 0 );
    }
   

    public int addScatterPlot( double[] xdata, double[] ydata, String[] label, Color col, int glyph )
    {
	return addPlot( 2, null, xdata, ydata, label, col, glyph );
    }
    public int addScatterPlot( double[] xdata, double[] ydata, Color col, int glyph )
    {
	return addPlot( 2, null, xdata, ydata, null, col, glyph );
    }
    public int addScatterPlot( double[] xdata, double[] ydata, Color col )
    {
	return addPlot( 2, null, xdata, ydata, null, col, 0 );
    }
   
    
    public void removeAllPlots( )
    {
	plots = new java.util.Vector();
	//repaint();
	next_id = 0;
    }
    
    private int addPlot( int type, String name, double[] xdata, double[] ydata, String[] label, Color col, int glyph )
    {
	GraphPlot plot = new GraphPlot( type, name, xdata, ydata, label, col, glyph, copy_data );

	plots.addElement( plot );

	double[] minmax = getRange( xdata );
	
	if(plots.size() > 1)
	{
	    if(minmax[0] < xmin)
		xmin = minmax[0];
	    if(minmax[1] > xmax)
		xmax = minmax[1];
	}
	else
	{
	    xmin = minmax[0];
	    xmax = minmax[1];
	}

	minmax = getRange( ydata );
	
	if(plots.size() > 1)
	{
	    if(minmax[0] < ymin)
		ymin = minmax[0];
		if(minmax[1] > ymax)
		    ymax = minmax[1];
	    }
	else
	{
	    ymin = minmax[0];
	    ymax = minmax[1];
	}
	
	/*
	double hmin = h_axis.getMinTick();
	if(!Double.isNaN(hmin))
	    xmin = (hmin < xmin) ? hmin : xmin;

	double hmax = h_axis.getMaxTick();
	if(!Double.isNaN(hmax))
	    xmax = (hmax > xmax) ? hmax : xmax;
	*/

	h_axis.setDataExtent( xmin, xmax );
	
	/*
	double vmin = v_axis.getMinTick();
	if(!Double.isNaN(vmin))
	    ymin = (vmin < ymin) ? vmin : ymin;

	double vmax = v_axis.getMaxTick();
	if(!Double.isNaN(vmax))
	    ymax = (vmax > ymax) ? vmax : ymax;
	*/

	v_axis.setDataExtent( ymin, ymax );
	
	// what is the 'width' of each data point?
	// each bar chart adds one to the width

	h_axis.setPointWidth( getNumBarCharts() );

	return next_id++;
    }
    
    public int getNumNamedPlots()
    {
	int count = 0;
	for(int p=0; p < plots.size(); p++)
	{
	    GraphPlot plot = (GraphPlot) plots.elementAt(p);
	    if(plot.name != null)
		count++;
	}
	return count;
    }

    public String getLongestPlotName()
    {
	String best = null;
	int best_len = 0;
	for(int p=0; p < plots.size(); p++)
	{
	    GraphPlot plot = (GraphPlot) plots.elementAt(p);
	    if(plot.name != null)
	    {
		if(plot.name.length() > best_len)
		{
		    best_len = plot.name.length();
		    best = plot.name;
		}
	    }
	}
	return best;
    }

    // used for figuring out the width of the legend
    public String getConcatenatedPlotNames()
    {
	String concat = "";
	for(int p=0; p < plots.size(); p++)
	{
	    GraphPlot plot = (GraphPlot) plots.elementAt(p);
	    if(plot.name != null)
		concat += plot.name;
	}
	return concat;
    }

    public int getNumBarCharts()
    {
	int width = 0;
	for(int p=0; p < plots.size(); p++)
	{
	    GraphPlot plot = (GraphPlot) plots.elementAt(p);
	    if(plot.type == 0)
		width++;
	}
	return width;
    }

    // the number of data points in the bar chart with the most data points
    public int getMaxBars()
    {
	int max = 0;
	for(int p=0; p < plots.size(); p++)
	{
	    GraphPlot plot = (GraphPlot) plots.elementAt(p);
	    if(plot.type == 0)
		if(plot.xdata.length > max)
		    max = plot.xdata.length;
	}
	return max;
    }

    private double[] getRange(  double[] data ) 
    {
	double[] result = new double[2];
	
	if((data == null) || (data.length == 0))
	{
	    result[0] = result[1] = Double.NaN;
	    return result;
	}
	
	double min = data[0];
	double max = data[0];
	
	boolean non_nan_seen = false;

	for(int v=0; v < data.length; v++)
	{
	    if(! Double.isNaN(data[v] ))
	    {
		if( non_nan_seen )
		{
		    if(data[v] < min) 
			min = data[v];
		    if(data[v] > max) 
			max = data[v];
		}
		else
		{
		    min = max = data[v];
		    non_nan_seen = true;
		}
	    }
	}

	result[0] = min;
	result[1] = max;
	
	return result;
    }
    
    // ===============================================

    private double[] generateGrid(double min, double max, int count)
    {
	double[] pts = new double[count];
	
	double d = (max-min) / (double)(count-1);
	double v = min;
	for(int p=0; p < count; p++)
	{
	    pts[p] = v;
	    v += d;
	}

	return pts;
    }
}

