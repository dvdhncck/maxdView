import java.io.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.font.*;

public final class PlotAxis
{
    public PlotAxis(maxdView mview_, String name_)
    {
	mview = mview_;
	name = name_;

	use_auto_min = use_auto_max = true;

	user_min = .0;
	user_max = 1.;

	axis_len = 0.7;

	tick_mode  = AutoTicks;

	user_title = auto_title = null;
	use_auto_title = true;

	tick_positions = null;

	decimals = 3;

	scale = LinearScale;

	tick_dir = 1;
	tick_len = 5.0;
	number_of_ticks = 8;

	title_font = font = mview.getDataPlot().getFont();

	title_font_fam = font_fam  = fontToFamily(font);
	title_font_sty = font_sty  = fontToStyle(font);
	font_size = font.getSize();
	title_font_size = font_size * 2;

	updateFonts();
    }


    // ----------------------------------------------------------------

    //
    // convert the actual data value into a scaled value suitable for plotting
    //
    public final double toScale( double val, double total_len )
    {
	switch( scale )
	{
	case LinearScale:
	{
	    return ((val - offset) * one_over_range) * total_len;
	}
	case LogScale:
	{
	    // get the value shifted to (0....range)
	     double raw_val = Math.exp( val );
	    // range and one_over_range will be correctly set to compensate for the exp'd values...
	    return ((raw_val - offset) * one_over_range) * total_len;
	}
	case Log10Scale:
	{
	    // get the value shifted to (0....range)
	     double raw_val = Math.exp( val * ln_to_log10_scale );
	    // range and one_over_range will be correctly set to compensate for the exp'd values...
	    return ((raw_val - offset) * one_over_range) * total_len;
	}
	case Log2Scale:
	{
	    // get the value shifted to (0....range)
	     double raw_val = Math.exp( val * ln_to_log2_scale );
	    // range and one_over_range will be correctly set to compensate for the exp'd values...
	    return ((raw_val - offset) * one_over_range) * total_len;
	}
	case ExpScale:
	{
	    // get the value shifted to (0....range)
	     double raw_val = Math.log( val );
	    // range and one_over_range will be correctly set to compensate for the exp'd values...
	    return ((raw_val - offset) * one_over_range) * total_len;
	}
	case Exp2Scale:
	{
	    // get the value shifted to (0....range)
	     double raw_val = Math.log( val ) * inverse_ln_to_log2_scale;
	    // range and one_over_range will be correctly set to compensate for the exp'd values...
	    return ((raw_val - offset) * one_over_range) * total_len;
	}
	case Exp10Scale:
	{
	    // get the value shifted to (0....range)
	     double raw_val = Math.log( val ) * inverse_ln_to_log10_scale;
	    // range and one_over_range will be correctly set to compensate for the exp'd values...
	    return ((raw_val - offset) * one_over_range) * total_len;
	}
	}

	return Double.NaN;
    }

    //
    // convert a scaled value from the axis into the actual data value
    //
    public final double fromScale( double val, double total_len )
    {
	try
	{
	    switch( scale )
	    {
	    case LinearScale:
		return ((val / total_len) * range) + offset;
	    case ExpScale:
	    {
		double raw_val = ((val / total_len) * range) + offset;
		return Math.exp( raw_val );
	    }
	    case LogScale:
	    {
		double raw_val = ((val / total_len) * range) + offset;
		return Math.log( raw_val );
	    }
	    }
	}
	catch( ArithmeticException ae )
	{
	    return Double.NaN;
	}

	return Double.NaN;
     }


    // ----------------------------------------------------------------

    public final int getScale()
    {
	return scale;
    }
    public final void setScale( int s )
    {
	scale = s;
    }
    

    public final void setComputedRange(double min_, double max_)
    {
	auto_min = min_;
	auto_max = max_;

	updateRange();
    }

    public final void setUseAutoTitle( boolean use_auto_title_ )
    {
	use_auto_title = use_auto_title_;
    }
    public final boolean getUseAutoTitle()
    {
	return use_auto_title;
    }
    public final String getTitle()
    {
	return use_auto_title ? auto_title : user_title;
    }
    public final void setAutoTitle( String t )
    {
	auto_title  = t;
    }
    public final String getAutoTitle()
    {
	return auto_title;
    }
    public final String setUserTitle( String t )
    {
	return user_title = t;
    }
     public final String getUserTitle()
    {
	return user_title;
    }
  
    public final double getMin()
    {
	return use_auto_min ? auto_min : user_min;
    }
    public final double getMax()
    {
	return use_auto_max ? auto_max : user_max;
    }

    public final void updateRange()
    {
	switch( scale )
	{
	case LinearScale:
	    range  = getMax() - getMin();
	    offset = getMin();
	    //System.out.println("lin scale: min=" +  getMin() + " max=" + getMax() );
	    break;
	case LogScale:
	    range  = Math.exp( getMax() ) -  Math.exp( getMin() );
	    offset = Math.exp( getMin() );
	    //System.out.println("log scale: min=" + Math.exp( getMin() ) + " max=" + Math.exp( getMax() ) );
	    break;
	case ExpScale:
	    range  = Math.log( getMax() ) -  Math.log( getMin() );
	    offset = Math.log( getMin() );
	    //System.out.println("exp scale: min=" + Math.log( getMin() ) + " max=" + Math.log( getMax() ) );
	    break;
	}
	one_over_range = 1.0 / range;

	updateTicks();
    }

    public final void updateTicks()
    {
	if( tick_mode == AutoTicks )
	{
	    boolean is_log = ( scale == LogScale ) || ( scale == Log2Scale ) || ( scale == Log10Scale );

	    tick_positions = ComputeMajorTickPositions( getMin(), getMax(), number_of_ticks, is_log ); 
	}
    }
    
    public void updateFonts()
    {
	font = new Font( mview.getDataPlot().font_family_names[ font_fam ], 
			 fontStyle( font_sty ), 
			 font_size );
	title_font = new Font( mview.getDataPlot().font_family_names[ title_font_fam ], 
			 fontStyle( title_font_sty ), 
			 title_font_size );
    }

    // what a mess - this is duplicatedeverywhere.... !

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


    // ==========================================================================================================
    // ==========================================================================================================


/*
  
Clever maths trickery lifted from 'Specview'

  http://www.stsci.edu/resources/software_hardware/specview/

  author: Ivo Busko 

  Specview  a product of the Space Telescope Science Institute, which is operated by AURA for NASA.


*/


    //  this code utilises an "extended" logarithm function for negative
    //  or negative data.  The function is piecewise, continuous, monotonic 
    //  reasonably smooth, and most importantly, is defined for all inputs


    // 'safe' logarithm function.
    public static double safe_log10 ( final double x ) 
    {
        if (x > 0.0) 
	    {
                return Math.log (x) * inverse_ln_to_log10_scale;
            } 
	    else 
	    {
                return Double.NaN;
            }
    }

    // 'safe' natural logarithm function.
    public static double safe_log ( final double x ) 
    {
	if (x > 10.0) 
	{
	    return Math.log (x);
	}
	if ((x >= -10.0)) 
	{
	    return x / 10.0;
	}
	return - Math.log (-x);
    } 

    // 'safe' inverse logarithm function
    public static double safe_pow10 ( final double x ) 
    {
        return Math.pow (10.0, x);
    }

    // 'safe' inverse natural logarithm function
    public static double safe_pow ( final double x ) 
    {
        if (x < -1.0) 
	{
	    return - Math.pow (10.0, -x);
	}
	if (x <= 1.0) 
	{
	    return x * 10.0;
	}
	return Math.pow (10.0, x);
    }

 
    /**
     *  Build an array of major tick positions for a linear axis.
     *
     *  @param   a1       the lower axis extremity
     *  @param   a2       the upper axis extremity
     *  @param   n        the (approximate) number of ticks
     *  @return           the array with major tick positions
     */
    public static double[] ComputeMajorTickPositions ( final double a1, final double a2, final int n, final boolean is_log ) 
    {
	//System.out.println("ComputeMajorTickPositions() " + a1 + " ... " + a2 + ", " + n + " ticks");

	if( Double.isInfinite( a1 ) || Double.isNaN( a1 ) || Double.isInfinite( a2 ) || Double.isNaN( a2 ) )
	    return null;
	
	if( a1 >= a2 )
	    return null;

	if( n < 1 )
	    return null;

	//System.out.println("ComputeMajorTickPositions() range is OK");

        if (is_log) 
            return ComputeMajorLogTickPositions ( a1, a2, n );
	else
	    return ComputeMajorLinearTickPositions ( a1, a2, n );
    }

    private static double[] ComputeMajorLinearTickPositions ( double a1, double a2, int n ) 
    {
        double major_step  = Round (Math.abs (a2 - a1) / n);
        double major_start;

        if (a2 > a1)
            major_start = Math.floor (a1 / major_step ) * major_step;
        else
            major_start = Math.ceil (a1 / major_step ) * major_step;

        double hold = major_start;
        int major_nticks = 1;
        if (a2 > a1) 
	{
            while (hold < a2) 
	    {
                hold += major_step;
                major_nticks++;
            }
        } 
	else 
	{
            major_step = -major_step;
            while (hold > a2) 
	    {
                hold += major_step;
                major_nticks++;
            }
        }
	
        double[] ticks = new double[major_nticks];
        for (int i = 0; i < major_nticks; i++) 
	{
            ticks[i] = major_start + major_step * i;
        }
	
        return ticks;
    }

    private static double[] ComputeMajorLogTickPositions ( double a1,double a2,int n )
    {
        double hold;
        double[] ticks = new double[2];
        double na1 = a1;
        double na2 = a2;
        if (a1 > a2) 
	{
            hold = a1;
            na1 = a2;
            na2 = hold;
        }
        double log_na1 = safe_log10 (na1);
        double major_start = safe_pow10 (Math.ceil (safe_log10 (na2)));
        double major_step = 1.0;
        int major_nticks;

        do 
	{
            major_nticks = 1;
            major_step *= 10.0;
            double log_major_step = safe_log10 (major_step);
            hold = safe_log10 (major_start);
            while (hold > log_na1) 
	    {
                hold -= log_major_step;
                major_nticks++;
            }
        } while (major_nticks > 5);

        if (major_nticks > 2) 
	{
            ticks = new double[major_nticks];
            ticks[major_nticks-1] = major_start;
            major_step = safe_log10 (major_step);
            for (int i = major_nticks-2; i >= 0; i--) 
	    {
                ticks[i] = safe_pow10 (safe_log10 (ticks[i+1]) - major_step);
                if (Math.abs (ticks[i]) < 1.E-3) 
		{
                    ticks[i] = 0.0;
                }
            }
        } 
	else 
	{
            major_step  = Round (Math.abs (a2 - a1) / n);
            na1 = Math.ceil (na1 / major_step ) * major_step;
            na2 = Math.floor (na2 / major_step ) * major_step;
            ticks[0] = na1;
            ticks[1] = na2;
        }
        return ticks;
    }

    /**
     *  Given an array of major tick positions, returns an array
     *  with minor tick optimal positions.
     *
     *  @param   major    the array with major tick positions
     *  @param   is_log   log axis ?
     *  @return           the array with minor tick positions, or null in
     *                    case of invalid input array
     */
    public static double[] ComputeMinorTickPositions (double[] major, boolean is_log) 
    {
        int n;

        if (is_log) 
	{
            return ComputeMinorLogTickPositions (major);
        } 
	else 
	{
            double order = 1.0;

            if (major.length > 1) 
	    {
                order = Mantissa (Math.abs (major[1] - major[0]));
            }

            if (order > 3.0) // 3.0 takes into account rounding errors
	    {
                n = 4;
            } 
	    else 
	    {
                n = 3;
            }

            return ComputeMinorLinearTickPositions (major, n);
        }
    }

    private static double[] ComputeMinorLinearTickPositions (double[] major, int n) 
    {

        if (major == null) 
	{
            return null;
        }

        double[] minor = null;
        double step = 1.0;

        // Only one major tick mark was provided.

        if (major.length < 2) 
	{
            minor = new double[n];
            step = major[0] / (n + 1);
            for (int j = 0; j < n; j++) 
	    {
                minor[j] = major[0] + step * j;
            }
        } 
	else 
	{
            // More than one tick mark was provided.

            minor = new double[(major.length - 1) * n];
            int k = 0;

            step = (major[1] - major[0]) / (n + 1);

            for (int i = 0; i < (major.length - 1); i++) 
	    {
                for (int j = 1; j <= n; j++) 
		{
                    minor[k++] = major[i] + step * j;
                }
            }
        }

        return minor;
    }

    /**
     *  Given an array of major log tick positions, returns an array
     *  with minor log tick positions. The major tick positions are
     *  assumed to be at WCS values with an exact mantissa of 1.0.
     *
     *  @param   major  the array with major tick positions
     *  @return         the array with minor tick positions, or null in
     *                  case of invalid input array
     */
    private static double[] ComputeMinorLogTickPositions (double[] major) 
    {
        if (major == null) 
	{
            return null;
        }

        // For every major tick in array, add 18 minor ticks, 9 above
        // and 9 below.

        double[] minor = new double[major.length * 18];
        double here;
        int k = 0;
        for (int i = 0; i < major.length; i++) 
	{
            here = major[i];
            for (int j = 1; j <= 9 ; j++) 
	    {
                minor[k++] = major[i] + major[i] * j;
            }
            here = major[i];
            for (int j = 1; j <= 9 ; j++) 
	    {
                minor[k++] = major[i] - major[i] / 10.0 * j;
            }
        }
        return minor;
    }

    /**
     *  Rounds up the parameter.
     *
     *  @param  val  the value to be rounded up
     *  @return      the rounded up value
     */
    private static double Round (double val) 
    {

        int exponent = Exponent (val);
        val = Mantissa (val);

        if (val > 5.0) {
            val = 10.0;
        } else if (val > 2.0) {
            val = 5.0;
        } else if (val > 1.0) {
            val = 2.0;
        } else {
            val = 1.0;
        }

        if( exponent < 0 ) {
            for (int i = exponent; i < 0; i++) {
                val /= 10.0;
            }
        } else {
            for (int i = 0; i < exponent; i++) {
                val *= 10.0;
            }
        }

        return val;
    }

    /**
     *  Gets exponent (characteristic) of a number in scientific notation.
     *
     *  @param  val  the input value
     *  @return      its exponent
     */
    private static int Exponent (double val) { return (int)(Math.floor(Log10(val)));  }

    /**
     *  Gets mantissa of a number in scientific notation.
     *
     *  @param  val  the input value
     *  @return      its mantissa
     */
    private static double Mantissa (double val) {
        int exponent = Exponent (val);
        if( exponent < 0 ) {
            for (int i = exponent; i < 0; i++) {
                val *= 10.0;
            }
        } else {
            for (int i = 0; i < exponent; i++) {
                val /= 10.0;
            }
        }
        return val;
    }

    /**
     *  Computes log base 10.
     *
     *  @param  x  a double value
     *  @return The log<sub>10</sub> of x
     */
    private static double Log10 (double x) throws ArithmeticException {
        if( x <= 0.0 ) {
            throw new ArithmeticException ("Range exception in Log10");
        }
        return Math.log(x) * inverse_ln_to_log10_scale;
    }


    // ==========================================================================================================

    // ==========================================================================================================

    public final static double ln_to_log10_scale = 2.3025850929940456840179914546844;
    public final static double ln_to_log2_scale = 0.69314718055994530941723212145818;

    public final static double inverse_ln_to_log10_scale = 1.0 / ln_to_log10_scale;
    public final static double inverse_ln_to_log2_scale = 1.0 / ln_to_log2_scale;


    protected maxdView mview;

    protected String name;

    protected int scale;

    public final static int LinearScale = 0;
    public final static int LogScale    = 1;
    public final static int Log2Scale   = 2;
    public final static int Log10Scale  = 3;
    public final static int ExpScale    = 4;
    public final static int Exp2Scale   = 5;
    public final static int Exp10Scale  = 6;

    protected int decimals;

    protected double offset, range, one_over_range;

    protected double min, max;

    protected String user_title, auto_title;
    protected boolean use_auto_title;

    protected double user_min, user_max;

    protected double auto_min, auto_max;

    protected boolean use_auto_min, use_auto_max;

    protected double axis_len;    // as a fraction of panel width or height

    protected double tick_len;
    protected int    tick_dir;
    protected int    number_of_ticks;

    protected double[] tick_positions;

    protected String manual_tick_spec;

    public final static int NoTicks         = 0;
    public final static int MinMaxTicks     = 1;
    public final static int MinMaxZeroTicks = 2;
    public final static int AutoTicks       = 3;
    public final static int ManualTicks     = 4;

    protected int tick_mode;

    protected Color  colour;

    protected int    font_fam;
    protected int    font_sty;
    protected int    font_size;
    protected Font   font;
    
    protected int    title_font_fam;
    protected int    title_font_sty;
    protected int    title_font_size;
    protected Font   title_font;
}

