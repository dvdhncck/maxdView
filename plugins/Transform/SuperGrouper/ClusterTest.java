import java.io.*;
import edu.wlu.cs.levy.CG.*;

public class ClusterTest
{
    
    private KDTree tree;

    private double[][] profiles;
    private int[]      ids;

    private int        n_dims;
    private int        n_spots;

    private static final boolean debug_paritition = true; //false;
    private static final boolean debug_search     = true; //false;
    private static final boolean debug_test       = true; //false;

    // ===============================================================================
    //
    // ===============================================================================


    public class EventTimer
    {
	public EventTimer()
	{
	    start = new java.util.Date();
	}
	
	public String elapsed()
	{
	    java.util.Date now = new java.util.Date();
	    
	    return niceTime( now.getTime() - start.getTime() );
	}
	
	private String niceTime( final long m_seconds )
	{
	    long seconds = m_seconds / 1000l;      // note: this is 1000long not 10001 !
	    long hrs = seconds / (60 * 60);
	    seconds -= (hrs * (60 * 60));
	    long mins = seconds / 60;
	    seconds -= (mins * 60);
	    
	    double milli_seconds = (double) ( m_seconds % 1000l );
	    
	    int deci_seconds  = (int) ( milli_seconds * 0.01 );
	    
	    if((hrs == 0) && (mins == 0))
	    {
		return seconds + "." + deci_seconds + "s";
	    }
	    
	    return hrs + ":" + (mins<10 ? "0" : "") + mins + ":" + (seconds<10 ? "0" : "") + seconds + "." + deci_seconds;
	    
	}
	
	private java.util.Date start;
    }


    // ===============================================================================
    //
    //
    //
    //
    //
    //
    //
    // ===============================================================================


    private String coordsToString( double[] da )
    {
	String s = "<";

	for( int d=0; d < da.length; d++)
	{
	    if( d > 0 )
		s += ",";
	    s += String.valueOf( da[ d ] );
	}
	s += ">";

	return s;
    }

    // ===============================================================================
    

    private static double distance( final double[] p1, final double[] p2 )
    {
	double acc = .0;
	
	for(int d=0; d < p1.length; d++)
	{
	    final double tmp = p1[d] - p2[d];
	    acc += (tmp * tmp);
	}
	
	return acc;
    }


    // ===============================================================================
    // test harness
    // ===============================================================================


    public int findNearestByExhaustiveSearch( final double[] search_profile )
    {
	// search all profiles in this space
	
	final int ns = ids.length;
	
	double min_dist = distance( search_profile, profiles[ ids[ 0 ] ] );

	int nearest_spot = ids[ 0 ];
	
	for( int s = 1; s < ns; s++ )
	{
	    final double dist = distance( search_profile, profiles[ ids[ s ] ] );
	    
	    if( dist < min_dist )
	    {
		min_dist = dist;
		nearest_spot = ids[ s ];
	    }
	}
	
	return nearest_spot;
    }


    public  void testResults( final int spots_to_test )
    {

	int failed = 0;
	int passed = 0;

	for( int s=0; s < spots_to_test; s++ )
	{
	    double[] search_profile = new double[ n_dims ];

	    for( int d=0; d < n_dims; d++ )
	    {
		search_profile[ d ] = ( Math.random() * 20.0 ) - 10.0;
	    }


	    int brute_force_nearest_id = findNearestByExhaustiveSearch( search_profile );
	    int accelerated_nearest_id = findNearestUsingTree(  search_profile );


	    if( brute_force_nearest_id != accelerated_nearest_id )
	    {
		if( debug_test )
		{
		    System.out.println( "Failed after " + (s+1) + " tests." );
		    
		    System.out.println( "Exhaustive Search: id=" + brute_force_nearest_id + " dist=" + 
					distance( profiles[ brute_force_nearest_id ], search_profile ) );
		    
		    System.out.println( "Accelerated Search: id=" + accelerated_nearest_id + " dist=" + 
					distance( profiles[ accelerated_nearest_id ], search_profile ) );
		    
		    System.exit( -1 );
		}

		failed++;
	    }
	    else
	    {
		passed++;
	    }
	}
	
	System.out.println( passed + " passed, " + failed + " failed." );
    
    }


    public void timeResults( final double[][] search_profiles, final boolean use_kd_tree )
    {

	
	EventTimer et = new EventTimer();

	for( int s=0; s < search_profiles.length; s++ )
	{
	    int nearest_id = use_kd_tree ? 
		findNearestUsingTree( search_profiles[ s  ] ) : 
		findNearestByExhaustiveSearch( search_profiles[ s  ] );
	}
	
	System.out.println( search_profiles.length + " " + 
			    (use_kd_tree ? "accelerated" : "brute force" ) + 
			    " searches perfomed in " + et.elapsed() );
    }
    

    public ClusterTest( int n_dims, int n_spots )
    {
	this.n_dims = n_dims;
	this.n_spots = n_spots;

	profiles = new double[ n_spots ][ n_dims ];
	
	ids = new int[ n_spots ];
	
	for( int s=0; s < n_spots; s++ )
	{
	    ids[ s ] = s;

	    for( int d=0; d < n_dims; d++ )
	    {
		profiles[ s ][ d ] = ( Math.random() * 20.0 ) - 10.0;
	    }
	}


	EventTimer et = new EventTimer();

	tree = new KDTree( profiles[ 0 ].length );

	try
	{
	    for(int s=0; s < ids.length; s++)
	    {
		tree.insert( profiles[ s ], new Integer( ids[ s] ) );
	    }

	    System.out.println("KdTree initialised in " + et.elapsed() );
	}
	catch( KeySizeException kse )
	{
	    System.err.println( kse );
	    System.exit( -1 );
	}
	catch( KeyDuplicateException kde )
	{
	    System.err.println( kde );
	    System.exit( -1 );
	}


    }

    public int findNearestUsingTree( final double[] target )
    {
	try
	{
	    Integer sid = (Integer) tree.nearest( target );
	    if( sid != null )
		return sid.intValue();
	}
	catch( KeySizeException kse )
	{
	    System.err.println( kse );
	    System.exit( -1 );
	}
	return -1;
    }


    public void runTests( final int spots_to_test, final int spots_to_find )
    {
	// -----------------------------

	testResults( spots_to_test );

	// -----------------------------

	double[][] search_profiles = new double[ spots_to_find ][ n_dims ];

	for( int s=0; s < spots_to_find; s++ )
	{
	    for( int d=0; d < n_dims; d++ )
	    {
		search_profiles[ s ] [ d ] = ( Math.random() * 20.0 ) - 10.0;
	    }
	}
	

	timeResults( search_profiles, false );

	timeResults( search_profiles, true );
    }

    // ===========================================================================


    public static void main( String[] args )
    {
	//final int n_dims = 4;
	//final int n_spots = 2000;

	new ClusterTest( 3, 20000 ).runTests( 1000, 20000);

    }

}
