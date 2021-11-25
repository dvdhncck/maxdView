import java.io.*;

public class AnnoCacheEntry implements Serializable
{
    public String source_loc;    // the location
    public String source_args;   // the parsed arg list
    public String data;          // the actual anno data
    
    public AnnoCacheEntry(String sl, String sa, String d)
    {
	source_loc = sl; source_args = sa; data = d;
    }
    
}
