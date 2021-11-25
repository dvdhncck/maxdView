import java.io.*;

public class SpatiallyAcceleratedCluster
{

    public SpatiallyAcceleratedCluster( int[] spot_ids, double[][] profiles )
    {
	this.profiles = profiles;

	n_dimensions = profiles[ 0 ].length;


        // ::TODO:: pick a target number of levels, and then use that to
	//          calculate the 'n_points_per_division'
	//

	//int n_points_per_division = (int) Math.log( spot_ids.length );

	int n_points_per_division = spot_ids.length / 20;

	if( n_points_per_division > spot_ids.length )
	    n_points_per_division = spot_ids.length;

	if( n_points_per_division < 2 )
	    n_points_per_division = 2;

	
	System.out.println( "SpatiallyAcceleratedCluster(): " + 
			    spot_ids.length + 
			    " total spots, aiming for " + 
			    n_points_per_division + 
			    " spots per division" );


	partitionSpots( spot_ids, n_points_per_division );
    }


/*
    public ExprData.Cluster getClusterRoot( )
    {
	
    }
*/

    private NSpace root = null;

    private int n_dimensions;

    private double[][] profiles;


    private static final boolean debug_paritition = true; //false;
    private static final boolean debug_search     = true; //false;
    private static final boolean debug_test       = true; //false;


    // ===============================================================================
    //
    //
    //
    //
    //
    //
    //
    // ===============================================================================


    private void partitionSpots( final int[] spot_ids, final int n_points_per_division )
    {
	root = new NSpace( null, spot_ids, n_points_per_division );
    }	

    public int findNearest( final double[] target )
    {

	if( debug_search )
	    System.out.println("Searching for nearest to " + coordsToString( target ) );

	return root.findNearest( target );

    }

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


    public class NSpace
    {
	private NSpace   parent;
	private int[]    spot_ids;


	// these values are used when an NSpace has children, and indicate
	// the split point which those children have been created from
	private NSpace[] children;
	private double[] decider_value; 
	private int    partition_dimension;
//	private double partition_value;


	public NSpace( final NSpace parent, final int[] spot_ids, final int n_points_per_division )
	{ 
	    this.parent = parent;
	    
	    this.spot_ids = spot_ids;

	    children = null;

	    if( spot_ids.length > (2 * n_points_per_division ) )
	    {
		split( n_points_per_division );
	    }
	    else
	    {
		if( debug_paritition )
		{
		    System.out.print( "Contents:" );
		    for( int s = 0; s < spot_ids.length; s++ )
			System.out.print( spot_ids[ s ] + " " );
		    System.out.println( );
		}
	    }
	}


	public final int findNearest( final double[] target )
	{
	    if( children == null )
	    {
		// search the points in this space
		final int ns = spot_ids.length;

		if( debug_search )
		    System.out.println("seaching the " + ns + " spots in this node..." );

		double min_dist = distance( target, profiles[ spot_ids[ 0 ] ] );
		int nearest_spot = spot_ids[ 0 ];

		for( int s = 1; s < ns; s++ )
		{
		    final double dist = distance( target, profiles[ spot_ids[ s ] ] );

		    if( dist < min_dist )
		    {
			min_dist = dist;
			nearest_spot = spot_ids[ s ];
		    }
		}
		
		if( debug_search )
		    System.out.println("nearest spot_id is " + nearest_spot );


		return nearest_spot;
	    }
	    else
	    {

		// figure out which child to look in

		final double dist0 = Math.abs( target[ partition_dimension ] - decider_value[ 0 ] );
		final double dist1 = Math.abs( target[ partition_dimension ] - decider_value[ 1 ] );

		if( debug_search )
		{
		    System.out.println("delegating search, spliting on dimension " + partition_dimension + 
				       ", qchoosing the " + 
				       ( dist0 < dist1 ? "lower" : "upper" ) + 
				       " space" );
		}
		
		if( dist0 < dist1 )
		    return children[ 0 ].findNearest( target );
		else
		    return children[ 1 ].findNearest( target );

		/*
		if( target[ partition_dimension ] <= partition_value )
		    return children[ 0 ].findNearest( target );
		else
		    return children[ 1 ].findNearest( target );
		*/

	    }
	}


	public final void split( final int n_points_per_division )
	{
	    if( spot_ids.length < 2 )
		return;

	    // pick one of the dimensions and partition the contents of this NSpace into 2 NSpaces
	    // by allocating each of the contents into or or other of the subspaces

	    // figure out which dimension to split on by find the dimension with the largest range
	    // and then choosing the median value from that dimension

	    final int ns = spot_ids.length;
	    final int nd = n_dimensions;

	    double[] min = new double[ n_dimensions ];
	    double[] max = new double[ n_dimensions ];

	    for( int d=0; d < nd; d++ )
	    {
		min[ d ] = max[ d ] = profiles[ spot_ids[ 0 ] ][ d ];
	    }

	    for( int s=1; s < ns; s++ )
	    {
		for( int d=0; d < nd; d++ )
		{
		    final double v = profiles[ spot_ids[ s ] ][ d ];
		    
		    if( v < min[ d ] )
			min[ d ] = v;

		    if( v > max[ d ] )
			max[ d ] = v;
		}
	    }

	    partition_dimension = 0;
	    double max_range = max[ 0 ] - min[ 0 ];

	    for( int d=1; d < nd; d++ )
	    {
		double this_range = max[ d ] - min[ d ];

		if( this_range > max_range )
		{
		    max_range = this_range;
		    partition_dimension = d;
		}
	    }
	    

	    double[] sorted_values = new double[ ns ];
	    
	    for( int s=0; s < ns; s++ )
		sorted_values[ s ] = profiles[ spot_ids[ s ] ][ partition_dimension ];

	    java.util.Arrays.sort( sorted_values );

	    double partition_value = sorted_values[ sorted_values.length / 2 ];

	    if( debug_paritition )
		System.out.println( "spliting on dimension " + partition_dimension + "\n" + 
				    "  min=" + min[ partition_dimension ] + ", max=" + max[ partition_dimension ] + "\n" +
				    "  range=" + max_range + ", median=" + partition_value  );
	    
	    
		
	    // now partition the spots into two (hopefully equally sized) groups 
	    
	    int[] lower = new int[ ns ];
	    int[] upper = new int[ ns ];

	    int lower_i = 0;
	    int upper_i = 0;

	    for( int s=0; s < ns; s++ )
	    {
		if( profiles[ spot_ids[ s ] ][ partition_dimension ] <= partition_value )
		{
		    lower[ lower_i++ ] = spot_ids[ s ];
		}
		else
		{
		    upper[ upper_i++ ] = spot_ids[ s ];
		}
	    }


	    lower = shortenArray( lower, lower_i );

	    upper = shortenArray( upper, upper_i );

	    decider_value = new double[ 2 ];

	    decider_value[ 0 ] = sorted_values[ 0 ];

	    decider_value[ 1 ] = sorted_values[ ns - 1 ];

	    if( debug_paritition )
	    {
		System.out.println( ns + " spots partitioned, " + lower.length +  " in lower half, " + upper.length + " in upper" );
		System.out.println( "   lower decider=" + decider_value[ 0 ] + ", upper decider=" + decider_value[ 1 ] );
	    }
	    
	    // finally convert this NSpace into a transitional node and set up the two children
	    
	    spot_ids = null;

	    children = new NSpace[ 2 ];

	    children[ 0 ] = new NSpace( this, lower, n_points_per_division );

	    children[ 1 ] = new NSpace( this, upper, n_points_per_division );


	    

	    // ::TODO::
	    // store either the values furthest away from the median or perhaps the
	    // median of the subspace and use this to select which child to search in 
	    
	}

	
	// ------- utilities -----------------------------------------------------------------------------

	private int[] shortenArray( final int[] input, final int length )
	{
	    int[] output = new int[ length ];
	    for( int i=0; i < length; i++ )
		output[ i ] = input[ i ];
	    return output;
	}


    }
    

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


    public static int findNearestByExhaustiveSearch( final int[] test_ids, 
						     final double[][] test_profiles, 
						     final double[] search_profile )
    {
	// search all profiles in this space
	
	final int ns = test_ids.length;
	
	double min_dist = distance( search_profile, test_profiles[ test_ids[ 0 ] ] );

	int nearest_spot = test_ids[ 0 ];
	
	for( int s = 1; s < ns; s++ )
	{
	    final double dist = distance( search_profile, test_profiles[ test_ids[ s ] ] );
	    
	    if( dist < min_dist )
	    {
		min_dist = dist;
		nearest_spot = test_ids[ s ];
	    }
	}
	
	return nearest_spot;
    }


    
    public static void main( String[] args )
    {
	//final int n_dims = 4;
	//final int n_spots = 2000;

	final int n_dims = 3;
	final int n_spots = 20;

	double[][] test_profiles = new double[ n_spots ][ n_dims ];

	int[] test_ids = new int[ n_spots ];
	
	for( int s=0; s < n_spots; s++ )
	{
	    test_ids[ s ] = s;

	    for( int d=0; d < n_dims; d++ )
	    {
		test_profiles[ s ][ d ] = ( Math.random() * 20.0 ) - 10.0;
	    }
	}

	SpatiallyAcceleratedCluster sac = new SpatiallyAcceleratedCluster( test_ids, test_profiles );

	int failed = 0;
	int passed = 0;

	for( int s=0; s < n_spots; s++ )
	{
	    double[] search_profile = new double[ n_dims ];

	    for( int d=0; d < n_dims; d++ )
	    {
		search_profile[ d ] = ( Math.random() * 20.0 ) - 10.0;
	    }


	    int brute_force_nearest_id = findNearestByExhaustiveSearch( test_ids, test_profiles, search_profile );
	    int accelerated_nearest_id = sac.findNearest(  search_profile );


	    if( brute_force_nearest_id != accelerated_nearest_id )
	    {
		if( debug_test )
		{
		    System.out.println( "Failed after " + (s+1) + " tests." );
		    
		    System.out.println( "Exhaustive Search: id=" + brute_force_nearest_id + " dist=" + 
					distance( test_profiles[ brute_force_nearest_id ], search_profile ) );
		    
		    System.out.println( "Accelerated Search: id=" + accelerated_nearest_id + " dist=" + 
					distance( test_profiles[ accelerated_nearest_id ], search_profile ) );
		    
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

}
