public class NiceDouble
{ 
   public static final String valueOf(double d, int len, int dp)
    {
	String d_s = new Double(d).toString();

	// special handling for exponential values
	int exp_pt = d_s.indexOf('E');
	if(exp_pt > 0)
	{
	    
	    // trim the part before the 'E'
	    String np = trimString( d_s.substring(0, exp_pt), len, dp );
	    
	    // System.out.println( d_s + "  ->  " + (np+d_s.substring(exp_pt)));
	    
	    return np + d_s.substring(exp_pt);
	}
	else
	{
	    return trimString( d_s, len, dp );
	}
    }
    public static final String valueOf(double d, int len)
    {
	return valueOf(d, len, -1);
    }
    private static final String trimString( String d_s, int len, int dp)
    {
	//System.out.println( "trimString: " + d_s + "   dp=" + dp); 

	// dont trim numbers without a decimal point
	int dec_pt = d_s.indexOf('.');
	if(dec_pt < 0)
	    return d_s;

	int actual_decimals = (d_s.length() - dec_pt) - 1;

	String trim;

	if(dp < actual_decimals)
	{
	    int trim_pt = dec_pt + dp + 1;
	    
	    if(trim_pt > d_s.length())
	    {
		trim_pt = d_s.length();
	    }
	    
	    trim =  d_s.substring(0, trim_pt);
	}
	else
	{
	    trim = d_s;
	}

	int dpi = trim.indexOf('.');

	//System.out.println( "before: " + trim + "   dpi=" + dpi); 

	if(dpi < 0)
	    return trim;

	if(dpi == (trim.length()-1))
	    return trim.substring(0, trim.length()-1);
	

	// has a decimal pt remove trailing zeroes

	while((trim.length() > 0) && (trim.charAt(trim.length() - 1) == '0'))
	    trim = trim.substring(0, trim.length()-1);

	if((trim.length() > 0) && (trim.charAt(trim.length() - 1) == '.'))
	    trim = trim.substring(0, trim.length()-1);

	//System.out.println( "after: " + trim);

	return trim;
    }

}
