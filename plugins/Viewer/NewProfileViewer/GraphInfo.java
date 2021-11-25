public class GraphInfo
{
    /*
      
    all of the required data and transform info to quickly plot a profile graph

    
    'num_data_points' is the number of discrete steps along the X axis (i.e. how many points in each profile)



    */


    public GraphInfo( double min, double max, int num_data_points )
    {
	this.min = min;
	this.max = max;

	width = height = 1;

	origin_x  = origin_y = 0;

	this.num_data_points = num_data_points;

	updateTransforms();
    }

    public void setNumDataPoints( int num_data_points )
    {
	this.num_data_points = num_data_points;

	disable = ( this.num_data_points < 0 );

	updateTransforms();
    }


    public void setOrigin( int x, int y )
    {
	origin_x = x;
	origin_y = y;

	updateTransforms();
    }

    public void setScreenSize( int width, int height )
    {
	this.width = width;
	this.height = height;
	
	updateTransforms();
   }

    
    public final int toScreenY( final double v )
    {
	return origin_y - (int) ( ( v - min ) * y_scale );
    }
    
    public final int toScreenX( final int p )
    {
	return origin_x + (int)( (double) p * x_scale );
	
    }

    private void updateTransforms()
    {
	if( disable ) 
	    return;

	y_scale = ( 1.0 / ( max - min ) ) * (double) height;
	x_scale = ( 1.0 / num_data_points ) * (double) width;

    }

    public double min, max;   // vertical range

    private double x_scale, y_scale;

    private int num_data_points;

    public int origin_x, origin_y;

    public int width, height;

    private boolean disable = false;
}