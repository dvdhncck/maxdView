import java.io.*;

public class PluginCommandNew implements Serializable
{
    public String plugin_name;
    public String name;
    public String comment;
    public String[] args;
    
    public PluginCommandNew(String n, String[] a)
    {
	plugin_name = null;
	name = n; args = a;
	comment = null; 
    }
    public PluginCommandNew(String n, String c, String[] a)
    {
	plugin_name = null;
	name = n; comment = c; args = a;
    }
}
