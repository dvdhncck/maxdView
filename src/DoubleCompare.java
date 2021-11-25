
public class DoubleCompare
{
    public final static boolean doubleLessThan( double d1, double d2 )
    {
	return (Double.isNaN( d1 ) && Double.isNaN( d2 ) ) ? false : ( d1 < d2 );
    }
    public final static boolean doubleGreaterThan( double d1, double d2 )
    {
	return (Double.isNaN( d1 ) && Double.isNaN( d2 ) ) ? false : ( d1 > d2 );
    }
    public final static boolean doubleLessEquals( double d1, double d2 )
    {
	return (Double.isNaN( d1 ) && Double.isNaN( d2 ) ) ? true : ( d1 <= d2 );
    }
    public final static boolean doubleGreaterEquals( double d1, double d2 )
    {
	return (Double.isNaN( d1 ) && Double.isNaN( d2 ) ) ? true : ( d1 >= d2 );
    }
    public final static boolean doubleEquals( double d1, double d2 )
    {
	return (Double.isNaN( d1 ) && Double.isNaN( d2 ) ) ? true : ( d1 == d2 );
    }
    public final static boolean doubleNotEquals( double d1, double d2 )
    {
	return (Double.isNaN( d1 ) && Double.isNaN( d2 ) ) ? false : ( d1 != d2 );
    }
    public final static boolean doubleInRange( double d1, double d2 )
    {
	return ( doubleLessEquals( Math.abs( d1 ), Math.abs( d2 ) ) );
    }
    public final static boolean doubleNotInRange( double d1, double d2 )
    {
	return ( doubleLessEquals( Math.abs( d1 ), Math.abs( d2 ) ) == false );
    }


    // ==========================================================================================

    private static void makeHeader( final String operator, final String description, final double[] values )
    {
	System.out.println( "<TABLE BORDER=1>\n" );

	System.out.println( "<TR><TD BGCOLOR=#dddddd COLSPAN=7 ALIGN=CENTER><TT><B>" + operator + "</B></TT>" + 
			    "&nbsp;&nbsp;<I>" + description + "</I></TD></TR>\n");

	System.out.println( "<TR><TD>&nbsp;</TD>\n");
	for( int j = 0; j < 6; j++ )
	{
	    System.out.println( "<TD ALIGN=CENTER BGCOLOR=#8888dd>" +  values[j] + "</TD>" );
	}
	System.out.println( "</TR>\n" );
    }

    private static void makeFooter(  )
    {
	System.out.println( "</TABLE>\n" );	
    }

    public static void main( final String[] args )
    {   
	double[] values = { Double.NEGATIVE_INFINITY, 
			    Double.POSITIVE_INFINITY,
			    Double.NaN,
			    -1.0,
			    0,
			    1.0 };

	/*
	          
	     OPERATOR  -Inf   +Inf    NaN   -1.0   0   1.0
	     -Inf
	     +Inf
	     NaN
             -1.0,
	     0,
	     1.0
	*/

	// ==============================================================

	System.out.println( "<HTML><BODY>\n" );

	System.out.println( "<TABLE>\n<TR><TD>\n" );

	// ==============================================================

	makeHeader( "&lt;", "less than" , values );

	for( int i = 0; i < 6; i++ )
	{
	    System.out.println( "<TR><TD ALIGN=RIGHT BGCOLOR=#88dd88>" + values[i] + "</TD>" );

	    for( int j = 0; j < 6; j++ )
	    {
		System.out.println( "<TD ALIGN=CENTER>" + doubleLessThan( values[i], values[j] ) + "</TD>" );
	    }
	    System.out.println( "</TR>\n" );
	}

	makeFooter();

	   
	// ==============================================================

	System.out.println( "</TD><TD>\n" );

	// ==============================================================

	makeHeader( "&lt;=", "less than or equal to" , values );

	for( int i = 0; i < 6; i++ )
	{
	    System.out.println( "<TR><TD ALIGN=RIGHT BGCOLOR=#88dd88>" + values[i] + "</TD>" );

	    for( int j = 0; j < 6; j++ )
	    {
		System.out.println( "<TD ALIGN=CENTER>" + doubleLessEquals( values[i], values[j] ) + "</TD>" );
	    }
	    System.out.println( "</TR>\n" );
	}

	makeFooter();
	   
	// ==============================================================

	System.out.println( "</TD></TR><TR><TD>\n" );

	// ==============================================================

	makeHeader( "&gt;", "greater than" , values );

	for( int i = 0; i < 6; i++ )
	{
	    System.out.println( "<TR><TD ALIGN=RIGHT BGCOLOR=#88dd88>" + values[i] + "</TD>" );

	    for( int j = 0; j < 6; j++ )
	    {
		System.out.println( "<TD ALIGN=CENTER>" + doubleGreaterThan( values[i], values[j] ) + "</TD>" );
	    }
	    System.out.println( "</TR>\n" );
	}

	makeFooter();

	   
	// ==============================================================

	System.out.println( "</TD><TD>\n" );

	// ==============================================================

	makeHeader( "&gt;=", "greater than or equal to" , values );

	for( int i = 0; i < 6; i++ )
	{
	    System.out.println( "<TR><TD ALIGN=RIGHT BGCOLOR=#88dd88>" + values[i] + "</TD>" );

	    for( int j = 0; j < 6; j++ )
	    {
		System.out.println( "<TD ALIGN=CENTER>" + doubleGreaterEquals( values[i], values[j] ) + "</TD>" );
	    }
	    System.out.println( "</TR>\n" );
	}

	makeFooter();

	   
	// ==============================================================

	// ==============================================================

	System.out.println( "</TD></TR><TR><TD>\n" );

	// ==============================================================

	makeHeader( "=", "equal to" , values );

	for( int i = 0; i < 6; i++ )
	{
	    System.out.println( "<TR><TD ALIGN=RIGHT BGCOLOR=#88dd88>" + values[i] + "</TD>" );

	    for( int j = 0; j < 6; j++ )
	    {
		System.out.println( "<TD ALIGN=CENTER>" + doubleEquals( values[i], values[j] ) + "</TD>" );
	    }
	    System.out.println( "</TR>\n" );
	}

	makeFooter();

	   
	// ==============================================================

	System.out.println( "</TD><TD>\n" );

	// ==============================================================

	makeHeader( "!=", "not equal to" , values );

	for( int i = 0; i < 6; i++ )
	{
	    System.out.println( "<TR><TD ALIGN=RIGHT BGCOLOR=#88dd88>" + values[i] + "</TD>" );

	    for( int j = 0; j < 6; j++ )
	    {
		System.out.println( "<TD ALIGN=CENTER>" + doubleNotEquals( values[i], values[j] ) + "</TD>" );
	    }
	    System.out.println( "</TR>\n" );
	}

	makeFooter();

	   
	// ==============================================================

	System.out.println( "</TD></TR>\n" );

	System.out.println( "</TABLE>\n" );

	System.out.println( "</BODY></HTML>\n" );

	// ==============================================================

    }
}
