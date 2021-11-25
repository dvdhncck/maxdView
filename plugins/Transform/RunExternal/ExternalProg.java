import java.io.*;

public class ExternalProg implements Serializable, Cloneable
{
    public String name;      // short name used for convenience
    public String filename;  // actual filename (including path)
    public String params;    // command-line flags or params for the program

    public String suffix;    // what the new data will be called
    public String prefix;    // what the new data will be called
    
    public int i_mode;       // 0 = stdin, 1 = tmp file
    public int o_mode;       // 0 = stdin, 1 = tmp file

    public int i_delim;       // 0 = \t, 1 = ' ', 2 = ', '
    public int o_delim;       // 

    public int i_name_mode;  // what names (if any) are sent in the input file?
    public int o_name_mode;  // what names (if any) are sent in the input file?

    public int i_meas;       // what form of input does the program accept?
    public int o_meas;       // what form of output are we expecting?
    
    public boolean output_is_contig;  // i.e. in exactly the same order as the input
    public boolean output_has_names;  // the lines are tagged with the names from the input

    public ExternalProg makeClone() 
    { 
	try
	{
	    return (ExternalProg) clone();
	}
	catch(CloneNotSupportedException cnse)
	{
	    return null;
	}
    }
}
