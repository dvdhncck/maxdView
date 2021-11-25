public class TokenIsNotNumber extends Exception
{
    public TokenIsNotNumber(String str_)
    { 
	super();
	str = str_;
    }
    public String str;
}
    
