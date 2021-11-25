public class TickPositioner
{
/*

Clever maths trickery lifted from 'Specview'

  http://www.stsci.edu/resources/software_hardware/specview/

  author: Ivo Busko 

  Specview  a product of the Space Telescope Science Institute, which is operated by AURA for NASA.


*/

    private static final double NATURAL_TO_DECIMAL = 2.30258509299404568401;


    //  this code utilises an "extended" logarithm function for negative
    //  or negative data.  The function is piecewise, continuous, monotonic 
    //  reasonably smooth, and most importantly, is defined for all inputs


    // 'safe' logarithm function.
    public static double safe_log10 ( final double x ) 
    {
        if (x > 0.0) 
	    {
                return Math.log (x) / NATURAL_TO_DECIMAL;
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
        return Math.log(x) / 2.30258509299404568401;
    }

    // --------------------------

    public static void main( String[] args )
    {
	try
	{

	    double min = (new Double( args[0] )).doubleValue();
	    double max = (new Double( args[1] )).doubleValue();

	    int n_ticks = (new Integer( args[2] )).intValue();

	    boolean log = args[3].equals("log");

	    double[] ticks = ComputeMajorTickPositions( min, max, n_ticks, log );
	    
	    for(int t=0; t < ticks.length; t++)
	    {
		System.out.print( ticks[ t ] );
		System.out.print( " " );
	    }
	    
	}
	catch( NumberFormatException nfe )
	{
	}


    }

}
