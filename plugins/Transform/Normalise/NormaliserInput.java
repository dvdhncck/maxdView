public class NormaliserInput
{
    public double[][] data;
    public String[]   measurement_names;
    public int[]      spot_ids;
    public String[]   spot_attr_names;    // the attrs that are available for one or more measurements

    public NormaliserInput( double[][] d, String[] mn, String[] sa, int[] si )
    {
	data = d;
	measurement_names = mn;
	spot_attr_names = sa;
	spot_ids = si;
    }
}
