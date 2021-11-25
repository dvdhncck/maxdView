import java.io.*;

public class PluginInfo implements Serializable
{
    public String name;
    public String type;
    public String short_description;
    public String long_description;
    
    public boolean is_shared;
    public String class_name;
    public String root_path;


    public int version_major, version_minor, build;
    
    public PluginInfo(String n, String t, String sd, String ld, int vmaj, int vmin, int bld)
    {
	name =  n; 
	type = t; 
	short_description = sd; 
	long_description  = ld; 
	
	//is_shared = sh;
	//class_name = cn;
	//root_path = rp;

	version_major = vmaj; 
	version_minor = vmin; 
	build         = bld;
    }
}
