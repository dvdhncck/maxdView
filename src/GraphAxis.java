import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import java.awt.geom.*;
import java.awt.font.*;

public class GraphAxis
{
    int direction, orient;
    int w, h;

    double min, max;   // should be separated into 'data_min,data_max' and 'axis_min,axis_max'
                       // where 'axis_min,axis_max' include the possibly bigger extent defined
                       // by custom or auto ticks

    double tick_min, tick_max;
    double data_min, data_max;

    double scale, iscale;

    double[] ticks;
    int tick_len;
    int tick_label_gap;
    int tick_mode;
    int ndp;
    boolean log;
    String title;

    boolean mouse_tracking;

    int point_width;

    Font tick_font, title_font;

    String[] tick_labels;

    // double zero_log_pt;
    double log_offset;                // shifts values so that 'min' is just above .0
    boolean log_two_scales;
    int log_zero_pt;               // the zero position between the two log scales
    
    double neg_log_scale, neg_log_iscale;  // only used when 'log_two_scales'==true
    double pos_log_scale, pos_log_iscale;
    double two_log_small_value;

    boolean log_pos_scale;  // only used when 'log_two_scales'==false, tells whether +ve or -ve logs

    public static final int HORIZONTAL = 0;
    public static final int VERTICAL   = 1;

    public static final int OUTSIDE = 0;
    public static final int INSIDE  = 1;

    public static final int NO_TICKS_MODE     = 0;
    public static final int MIN_MAX_MODE      = 1;
    public static final int MIN_MAX_ZERO_MODE = 2;
    public static final int CUSTOM_MODE       = 3;
    public static final int AUTO_MODE         = 4;

    public GraphAxis( int dir, int ore )
    {
	title     = null;
	direction = dir;
	orient    = ore;
	tick_mode = MIN_MAX_MODE;
	ndp       = 2;
	log       = false;
	tick_len  = 5;

	tick_label_gap = 5;

	mouse_tracking = true;

	title_font = new Font( "Helvetica", Font.PLAIN, 12 );
	tick_font  = new Font( "Helvetica", Font.BOLD,  10 );

	point_width = 0;    //  for centering the bars in bar charts
    }



    public void setDataExtent( double min_, double max_ )
    {
	data_min = min_; 
	data_max = max_;

	update();
    }
    
    public void setTickMode( int mode_ )
    {
	tick_mode = mode_;
	update();
    }
    
    public void setTicks( double[] ticks_ )
    {
	if((ticks_ == null) || (ticks_.length == 0))
	{
	    tick_mode = NO_TICKS_MODE;
	    ticks = null;
	}
	else
	{
	    tick_mode = CUSTOM_MODE;
	    ticks = ticks_;
	}
	update();
    }

    public void setAutoTicks( )
    {
	tick_mode = AUTO_MODE;

	update();
    }

    public void setTickSigDigits( int ndp_ )
    {
	ndp = ndp_;
	update();
    }
    
    public void setTickFont( Font font_ )
    {
	tick_font = font_;
	update();
    }

    public double getMinTick()
    {
	/*
	if(tick_mode == CUSTOM_MODE)
	{
	    if((ticks != null) && (ticks.length > 0))
		return ticks[0];
	}
	return Double.NaN;
	*/
	return tick_min;
    }
    public double getMaxTick()
    {
	/*
	  if(tick_mode == CUSTOM_MODE)
	{
	    if((ticks != null) && (ticks.length > 0))
		return ticks[ticks.length - 1];
	}
	return Double.NaN;
	*/
	return tick_max;
    }

    public void setLog(  )
    {
	log = true;
	update();
    }

    public void setLinear(  )
    {
	log = false;
	update();
    }

    public void setTitle( String s )
    {
	title = s;
	update();
    }

    public void setMouseTracking( boolean mouse_tracking_ )
    {
	mouse_tracking =  mouse_tracking_;
	update();
    }

    public void setTitleFont( Font font_ )
    {
	title_font = font_;
	update();
    }


    // =======================================================================================

    public void setPointWidth( int pw )
    {
	point_width = pw;
	update();
    }

    // =======================================================================================

    public int worldToAxis( double val )
    {
	if(log)
	{
	    if(log_two_scales)
	    {
		if( val > 0)
		{
		    return log_zero_pt + (int) ((safe_log(val) - two_log_small_value) * pos_log_scale);
		}
		if( val < 0)
		{
		    return log_zero_pt - (int) ((safe_log(-val) - two_log_small_value) * neg_log_scale);
		}
		// val == 0
		return log_zero_pt;
	    }		
	    else
	    {
		if(log_pos_scale)
		{
		    return (int) ( (safe_log(val) - log_offset) * scale);
		}
		else
		{
		    // (-ve scale)
		    return (int) ( (log_offset - safe_log(-val)) * scale);
		}

	    }
	}
	else
	{
	    return (int)((val-min)*scale);  // should be 'actual_min'
	}
    }

    public double axisToWorld( int val )
    {
	if(log)
	{
	    if(log_two_scales)
	    {
		
		if( val > log_zero_pt )
		{
		    double val_a = (double) (val - log_zero_pt) * pos_log_iscale;   // 0...pos_range
		    
		    double val_l = val_a + two_log_small_value;   //  -20 ... log(max)

		    return safe_inv_log( val_l );
		}
		if( val < log_zero_pt )
		{
		    double val_a = (double) (log_zero_pt - val) * neg_log_iscale;   // 0...neg_range
		    
		    double val_l = val_a + two_log_small_value;   //  -20 ... log(max)

		    return - safe_inv_log( val_l );
		    
		    // double tmp = (double)(log_zero_pt - val) * neg_log_iscale;
		    // return safe_inv_log( tmp );
		}

		/*
		double tmp = (double) val * iscale;
		
		if(tmp > log_zero_pt)
		{
		    return safe_inv_log( tmp - log_zero_pt );
		}
		if(tmp < log_zero_pt)
		{
		    return safe_inv_log( log_zero_pt - tmp );
		}
		*/

		return .0;
	    }
	    else
	    {
		if(log_pos_scale)
		{
		    double tmp = log_offset + (double) val * iscale;
		    
		    return safe_inv_log( tmp );
		}
		else
		{
		    double tmp = log_offset - (double) val * iscale;
		    
		    return  -safe_inv_log( tmp );
		}

	    }
	}
	else
	{
	    return ((double)val*iscale)+min;  // should be 'actual_min'
	}
    }
    

    // =======================================================================================

    // =======================================================================================

    private void update()
    {
	if(tick_mode == MIN_MAX_MODE)
	{
	    ticks = new double[2];
	    ticks[0] = data_min;
	    ticks[1] = data_max;
	}
	if(tick_mode == MIN_MAX_ZERO_MODE)
	{
	    if((data_min < 0) && (data_max > 0) )
	    {
		ticks = new double[3];
		ticks[0] = data_min;
		ticks[1] = .0;
		ticks[2] = data_max;
	    }
	    else
	    {
		ticks = new double[2];
		ticks[0] = data_min;
		ticks[1] = data_max;
	    }
	}
	if(tick_mode == AUTO_MODE)
	{
	    pickTicks();
	}

	if((ticks != null) && (ticks.length > 0))
	{
	    tick_min = ticks[0];
	    tick_max = ticks[ticks.length-1];
	}

	tick_labels = null;
    }

    // =======================================================================================

    public int getRequiredWidth( GraphContext gc )
    {
	if(direction == HORIZONTAL)
	{
	    return 0;
	}
	else
	{
	    FontRenderContext frc = new FontRenderContext(null, false, false);
	    
	    int title_s = 0;
	    if(title != null)
	    {
		Rectangle2D title_r = title_font.getStringBounds(title, frc);
		
		title_s = gc.gap + (int)title_r.getHeight() + gc.gap;
	    }
	    
	    return getMaxTickLabelLength(gc) + tick_len + title_s;
	}
    }

    public int getRequiredHeight( GraphContext gc )
    {
	if(direction == HORIZONTAL)
	{
	    FontRenderContext frc = new FontRenderContext(null, false, false);
	    
	    int title_s = 0;
	    if(title != null)
	    {
		Rectangle2D title_r = title_font.getStringBounds(title, frc);
	    
		title_s = gc.gap + (int)title_r.getHeight() + gc.gap;
	    }
	    
	    Rectangle2D tick_r = tick_font.getStringBounds("0", frc);
	    
	    return (int)tick_r.getHeight() + tick_len + title_s;
	}
	else
	{
	    return 0;
	}

    }

    public int getMaxTickLabelWidth( GraphContext gc )
    {
	return getMaxTickLabelLength(gc);
    }

    public int getMaxTickLabelHeight( GraphContext gc )
    {
	FontRenderContext frc = new FontRenderContext(null, false, false);
	Rectangle2D tick_r = tick_font.getStringBounds("0", frc);
	return (int)tick_r.getHeight(); 
    }

    public int getMaxTickLabelLength( GraphContext gc )
    {
	
	if(tick_mode == NO_TICKS_MODE)
	    return 0;

	makeTickLabels();

	final String digits = "00000000000000000000";
	
	double max_t = max;

	for(int s=0; s < ticks.length; s++)
	{
	    if(ticks[s] > max_t)
		max_t = ticks[s];
	}

	int max_i = (int) max_t;
	
	int real_ndp = (ndp > digits.length()) ? digits.length() : ndp;

	String sample = max_i + "." + digits.substring(0, ndp);
	
	// System.out.println("ndp = " + ndp + " sample=" + sample);

	FontRenderContext frc = new FontRenderContext(null, false, false);
	Rectangle2D r = tick_font.getStringBounds(sample, frc);
		   
	return (int) r.getWidth();
    }
    

    public Image getImage( JComponent comp, GraphContext gc, int length )
    {
	if(gc == null)
	    return null;

	double range = .0;

	final double very_small = 1.0E-7;

	update();

	min = data_min;

	if(!Double.isNaN(tick_min))
	    if(tick_min < data_min)
		min = tick_min;
	
	max = data_max;
	
	if(!Double.isNaN(tick_max))
	    if(tick_max > data_max)
		max = tick_max;

	/*
	if((tick_mode == CUSTOM_MODE) || (tick_mode == AUTO_MODE))
	{
	    if(ticks[0] < min)
		min = ticks[0];
	    if(ticks[ticks.length-1] > actual_max)
		actual_max = ticks[ticks.length-1];
	}
	*/

	if(log)
	{
	    log_two_scales = false;

	    // if min >= 0 then use a single log scale ...
	    
	    if( min >= 0 )
	    {
		log_pos_scale = true;

		range = safe_log(max) - safe_log(min);
		
		log_offset = safe_log(min);
		    
		//System.out.println("min>=0: range is (" + safe_log(max) + " - " + safe_log(min) + ") = " + range + 
		//		   "  offset is " + log_offset);
	    }
	    else
	    {
		// (min < 0) 
	    
		if (max > 0)
		{
		    // use two log scales ...

		    log_two_scales = true;

		    double len_d = (double)(length-point_width-1);

		    two_log_small_value = safe_log(1.0E-10);   // ~ -20
		    
		    double pos_range = safe_log(max) - two_log_small_value;
		    double neg_range = safe_log(-min) - two_log_small_value;

		    range = pos_range + neg_range;

		    double pos_frac = pos_range / (pos_range + neg_range);
		    double neg_frac = neg_range / (pos_range + neg_range);

		    pos_log_scale = (len_d*pos_frac) / pos_range;
		    pos_log_iscale = 1.0 / pos_log_scale;


		    neg_log_scale = (len_d*neg_frac) / neg_range;
		    neg_log_iscale = 1.0 / neg_log_scale;
		    
		    log_offset = -neg_range;

		    log_zero_pt = (int) (len_d * neg_frac);

		    //System.out.println("min<0,max>=0: using two scales,  range is (" + neg_range + " + " + pos_range + 
		    //	       ") = " + range + "  zero_pt is " + log_zero_pt + " offset is " + log_offset);
		    
		    
		    
		}
		else
		{
		    // (min < 0) && (max < 0) 
		    
		    // use a single log scale ... but flip the axis 

		    log_pos_scale = false;
		    
		    log_offset = safe_log(-min);

		    range = safe_log(-min) - safe_log(-max);

		    //System.out.println("min<0,max<0: range is (" + safe_log(-min) + " - " + safe_log(-max) + 
		    //	       ") = " + range + " offset is " + log_offset);
		}
	    }


	    scale = (double)(length-point_width-1) / range;
	    
	    iscale = 1.0 / scale;
	    
	    // System.out.println("range is " + range);
	    
	}
	else
	{
	    range = max-min;
	    scale = (double)(length-point_width-1) / range;
	    iscale = 1.0 / scale;
	}

	int breadth = 0;

	int w, h;
	
	if(direction == HORIZONTAL)
	{
	    breadth = getRequiredHeight(gc);
	    w = length; 
	    h = breadth;
	}
	else
	{
	    breadth = getRequiredWidth(gc);
	    w = breadth;
	    h = length;
	}
	
	makeTickLabels();


	// System.out.println( (direction == HORIZONTAL ? "horizontal" : "vertical") +  " axis length=" + length + " size=" + w + "x" + h);
	

	if((w == 0) || (h == 0))
	    return null;

	Image img = comp.createImage( w, h  );
	
	Graphics graphic = img.getGraphics();
	

	final Graphics2D g2 = (Graphics2D) graphic;
	g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, 
			     RenderingHints.VALUE_ANTIALIAS_ON );
	

	graphic.setColor( gc.background_col );
	graphic.fillRect( 0, 0, w, h );

	FontRenderContext frc = new FontRenderContext(null, false, false);

	// graphic.setColor( Color.black );
	// graphic.fillRect( 0, 0, w, h );
	
	int xp = 0;
	int yp = 0;

	graphic.setColor( gc.foreground_col );
	
	if(direction == HORIZONTAL)
	{
	    yp = (orient == OUTSIDE) ? 0 : (breadth-1);

	    graphic.drawLine( 0, yp, length, yp );
	}
	else
	{
	    xp = (orient == OUTSIDE) ? (breadth-1) : 0;
	    
	    graphic.drawLine( xp, 0, xp, length );
	}
	
	if(title != null)
	{
	    Rectangle2D title_r = title_font.getStringBounds(title, frc);
		
	    graphic.setFont( title_font );

	    if(direction == HORIZONTAL)
	    {
		// System.out.println("title=" + title);
		
		//gc.setCurrentFont( graphic, title_font );
		
		int tx = ( length - (int)title_r.getWidth() ) / 2;
		
		graphic.drawString( title, tx, h - gc.gap );   // 'h' dubious?
	    }
	    else
	    {

		AffineTransform curr_at = g2.getTransform();

		TextLayout text_layout = new TextLayout(title, g2.getFont(), frc);
		
		AffineTransform new_at = new AffineTransform();

		int tw = (int) text_layout.getBounds().getWidth();
		int th = (int) text_layout.getBounds().getHeight();

		new_at.translate( (int)title_r.getHeight()/2 + th/2, length/2 + tw/2);
		
		new_at.rotate(Math.toRadians(270), 0, 0); // tw/2, th/2);
	       
		Shape shape = text_layout.getOutline(new_at);
		
		g2.fill(shape);

		g2.setTransform(curr_at);
		
	    }
	}

	int half_point_width = point_width / 2;

	if(ticks != null)
	{
	    graphic.setFont( tick_font );


	    for(int pt=0; pt < ticks.length; pt++)
	    {
		// System.out.println( tick_labels[pt] );

		int pos = worldToAxis( ticks[pt] );
		
		Rectangle2D tick_r = tick_font.getStringBounds(tick_labels[pt], frc);
		
		int len = (int) tick_r.getWidth();
		int th =  (int) tick_r.getHeight();

		int half_len = len / 2;
		
		if(direction == HORIZONTAL)
		{
		    xp = pos + half_point_width;
		    yp = (int) tick_r.getHeight() + tick_len;
		    
		    graphic.drawLine( xp, 0, xp, tick_len );
		    
		    xp -= half_len;
		    if(xp < 0)
			xp = 0;
		    if((xp+len) > w)
			xp = w - len;

		    graphic.drawString( tick_labels[pt], xp, yp );
		}
		else
		{
		    xp = breadth-(len+tick_len);
		    yp = ((length-1) - pos);
		    
		    graphic.drawLine( breadth, yp, breadth-tick_len, yp );
		    
		    yp += (th/2);
		    
		    if(yp > h)
			yp = h;
		    if(yp < th)
			yp = th;

		    graphic.drawString( tick_labels[pt], xp, yp );
		}
	    }
	}

	return img;
    }


    private void makeTickLabels()
    {
	if(tick_labels == null)
	{
	    if(ticks != null)
	    {
		tick_labels = new String[ticks.length];
		for(int s=0; s < ticks.length; s++)
		    tick_labels[s] = NiceDouble.valueOf( ticks[s], 20, ndp );

		// System.out.println("tick labels with " + ndp + " decimal pts made");
	    }
	    else
	    {
		tick_labels = new String[0];
	    }
	}
    }

    private void pickTicks( /*GraphContext gc, int length*/ )
    {
	int n_ticks = 4;

	ticks = pickTicks( n_ticks );

	if(ticks == null)
	    return;

	/*
	min = ticks[0];
	max = ticks[ticks.length-1];
	*/

    }

    final static double log10( final double d) { return Math.log(d) / Math.log(10); }

    public static double nearest( double in, double step, boolean round_up )
    {
	if(in == .0)
	    return .0;

	double lowest_d = ( in / step );

	//double lowest_i = (int) lowest_d;

	int lowest =  round_up ? (int)Math.ceil(lowest_d) : (int)Math.floor(lowest_d);

	double out = (step * (double)lowest);
	return out;
    }

    //
    // returns some X where X = pow(10,D) that is the nearest power of ten smaller than the input value
    //
    public static double step( double in )
    {
	if(in < 0)
	    in = -in;

	if(in == .0)
	{
	    return 0.1;
	}
	else
	{
	    double l10 = log10( in );
	    
	    int dbd, zaf;
	    
	    if(l10 < 0)
	    {
		zaf = -(int) l10;   // digits before decimal pt
		dbd = 0;            // zeroes after decimal pt
		
		return Math.pow(10,-(zaf+1));
	    }
	    else
	    {
		dbd= (int) l10;    // digits before decimal pt
		zaf = 0;           // zeroes after decimal pt
		
		return Math.pow(10,(dbd-1));
	    }
	}
    }
    
    public static boolean isSuitable( int n_pts, double range, double step )
    {
	int n_pts_gen = (int)( range / step );
	return ( n_pts_gen < n_pts );
    }

    public static double rangeWithStep( double rmin, double rmax, double step )
    {
	double range = nearest( rmax, step, true ) - nearest( rmin, step, false );
	if(range < 0)
	    range = -range;
	return range;
    }

    public double[] pickTicks( int n_pts )
    {
	//final double step = { 0.1, 0.25, 0.5 };

	
	// attempt a step factor of 0.001, 0.1, 1, 10, 100, 1000, .... etc

	//System.out.println("pickTicks(): input: " + data_min + " ... " + data_max);

	if(Double.isNaN(data_min) || Double.isNaN(data_max))
	    return null;

	double step_min = step(data_min);
	double step_max = step(data_max);

	//System.out.println("pickTicks(): candidate steps: " + step_min + " ... " + step_max);

	double step = (step_max > step_min) ? step_max : step_min;

	//System.out.println("step=" + step);

	
	
	//System.out.println("range=" + range);

	final double[] nice_numbers = { 0.2, 0.25, 0.5, 1.0 };

	int nice_i = 0;
	double t_step = step;

	double tick_d = t_step * nice_numbers[ nice_i ];
	double range = rangeWithStep( data_min, data_max, tick_d );
	boolean suitable = isSuitable( n_pts, range, tick_d );
	
	while(!suitable)
	{
	    //System.out.println("  delta of =" + tick_d + " is too small");

	    if(++nice_i >= nice_numbers.length)
	    {
		nice_i = 0;
		t_step *= 10.0;
	    }

	    tick_d = t_step * nice_numbers[ nice_i ];
	    range = rangeWithStep( data_min, data_max, tick_d );
	    suitable = isSuitable( n_pts, range, tick_d );
	}
	
	
	
	int n_pts_gen = (int)( range / tick_d ) + 1;
	
	
	double[] out = new double[ n_pts_gen ];

	double tick_v = nearest( data_min, tick_d, false );
	
	//System.out.println("delta of " + tick_d + " makes "+ n_pts_gen + " points");
 
	//System.out.println("pickTicks(): output: " + nearest( data_min, tick_d, false ) + 
	//	   " ... " + nearest( data_max, tick_d, true ));

	for(int p=0; p < n_pts_gen; p++)
	{
	    // System.out.println("tick " + (p+1) + " @ " + tick_v );

	    out[p] = tick_v;
	    tick_v += tick_d;

	}
	
	return out;
    }
    

    // =======================================================================================

    final private double safe_log( final double in )
    {
	if( in > .0)
	    return Math.log(in);
	else
	{
	    /*
	    if( in < .0)
		return (-Math.log(-in));
	    else
		return .0;
	    */
	    return .0;
	}
    }
    final private double safe_inv_log( final double in )
    {
	return Math.pow(Math.E, in);
    }

}
    
