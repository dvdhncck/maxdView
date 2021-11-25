import java.io.*;

public class AnnoSource implements Serializable
{
    public final static int SourceFile   = 0;
    public final static int SourceScript = 1;
    public final static int SourceURL    = 2;
    public final static int SourceJDBC   = 3;

    public int      mode;  
    public boolean  active; 
    public boolean  for_probe; 
    public boolean  for_gene; 
    public String   name;  // nice name
    public String   code;  // the command-line or url
    public String[] args;  // [name=value]*
    
    public AnnoSource(int m, boolean ac, String n, String c, String[] ar)
    {
	mode = m; active = ac; name = n; code = c; args = ar;
	for_probe = for_gene = false;
    }
}
