public abstract class Kernel
{
    //Scale parameter sets "width" of kernel
    double scale;

    //This Method returns kernel value
    //x0 is where center of kernel will be placed
    //x1 point where I wish to calculate kernel value given the center x0
    //Most kernels will be of form f( |x0 - x1| / scale )
    public abstract double value( double x0, double x1 );


    //Accessor methods
    public void set_scale( double lambda )
    {
	this.scale = lambda;
    }

    public double get_scale()
    {
	double lambda = scale;
	return lambda;
    }
}
