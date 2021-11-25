import java.io.*;

public class PluginCommand implements Serializable
{
    public String plugin_name;
    public String name;
    public String[] args;
    
    public PluginCommand(String n, String[] a)
    {
	plugin_name = null;
	name = n; args = a;
    }
}
