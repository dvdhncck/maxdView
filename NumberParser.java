
public class NumberParser
{
    public static double tokenToDouble(String str) throws TokenIsNotNumber
    {
	double d;
	boolean has_exp_part = false;
	
	// look for the pattern [-]num[+|-]['e'|'E']num
	
	// System.out.println("tokenToDouble() " + str + " = ?");
	
	final String lwr = str.toLowerCase();

	// is it a recognised NaN symbol?
	if( lwr.equals("nan") )
	    return Double.NaN;
	
	if( lwr.equals("infinity") )
	    return Double.POSITIVE_INFINITY;

	if( lwr.equals("-infinity") )
	    return Double.NEGATIVE_INFINITY;

	// is there an 'e' or an 'E' in the number?
	//
	int e_pos = str.indexOf('e');
	if(e_pos > 0)  // has to be not the first char.....
	{
	    has_exp_part = true;
	}
	else
	{  
	    e_pos = str.indexOf('E');
	    if(e_pos > 0)
		has_exp_part = true;
	}

	if(has_exp_part)
	{
	    String mant = str.substring(0, e_pos);
	    String exp  = str.substring(e_pos+1, str.length());
	    
	    // System.out.println("tokenToDouble() trying to parse [" + mant + ":" + exp + "]");
	    
	    if((!isFloatNumber(mant)) || (!isIntNumber(exp)))
	    {
		//System.out.println("tokenToDouble() wrd " + str);
		throw new TokenIsNotNumber(str);
	    }
	    double m = extractFloatNumber(mant);
	    int e = extractIntNumber(exp);
	    
	    while(e > 0)
	    {
		m *= 10.0;
		e--;
	    }
	    while(e < 0)
	    {
		m /= 10.0;
		e++;
	    }
	    
	    //System.out.println("tokenToDouble() exp " + str + " = " + m);
	    
	    d = m;
	}
	else
	{
	    if(!isFloatNumber(str))
	    {
		//System.out.println("tokenToDouble() word " + str);
		throw new TokenIsNotNumber(str);
	    }
	    d = extractFloatNumber(str);
	    //System.out.println("tokenToDouble() flt " + d);
	    
	}
	return d;
    }

    // can this string be interpreted as a floating point number?
    // (exponential values not allowed!)
    public static boolean isFloatNumber(String str)
    {
	boolean no_digits_yet = true;
	boolean is_first_dp = true;
	
	for(int ci=0;ci<str.length();ci++)
	{
	    char c = str.charAt(ci);
	    boolean ok = false;
	    
	    if((c == '.') && is_first_dp)
	    {
		ok = true;
		is_first_dp = false;
	    }
	    if((c >= '0') && (c <= '9'))
	    {
		ok = true;
		no_digits_yet = false;
	    }
	    if((c == '+') && (no_digits_yet))
		ok = true;
	    if((c == '-') && (no_digits_yet))
		ok = true;

	    if(!ok)
	    {
		//System.out.println(str + " NOT a float");
		return false;
	    }
	}
	//System.out.println(str + " IS a float");
	return true;
    }
    // can this string be interpreted as an integer?
    //
    public static boolean isIntNumber(String str)
    {
	for(int ci=0;ci<str.length();ci++)
	{
	    char c = str.charAt(ci);
	    boolean ok = false;
	    
	    if(c == '+')
		ok = true;
	    if(c == '-')
		ok = true;
	    if((c >= '0') && (c <= '9'))
		ok = true;
	    
	    if(!ok)
		return false;
	}
	return true;
    }
    
    public static double extractFloatNumber(String str)
    {
	try 
	{ Double d = new Double(str);
	//System.out.println("extractFloatNumber() " + str + " = " + d.doubleValue());
	return d.doubleValue();
	}
	catch (NumberFormatException nfe)
	{
	    return .0;
	}
    }
    public static int extractIntNumber(String str)
    {
	int it = 0;
	int unit = 1;
	
	for(int ci=str.length()-1; ci >= 0; ci--)
	{
	    char c = str.charAt(ci);
	    switch(c)
	    {
	    case '+':
		break;
	    case '-':
		it = -it;
		break;
	    default:
		int i  = c - '0';
		it += (i * unit);
		unit *= 10;
	    }
	}
	
	//System.out.println("extractIntNumber() " + str + " = " + it);
	
	return it;
    }
    

}
