public class TriCube extends Kernel
{
    public double value( double x0, double x1 )
    {
	double s = Math.abs( x0 - x1 );
	s /= scale;
	
	double ker = 0.0;
	if( s <= 1.0 )
	{
	    ker = Math.pow( (1.0 - Math.pow( s, 3.0 ) ), 3.0 );
	}

	return ker;
    }
}
