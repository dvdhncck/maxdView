import org.ncgr.isys.objectmodel.Description;

public class maxdViewISYSDescription implements Description
{
    private String label;
    
    public maxdViewISYSDescription(String label)
    {
	this.label = label;
    }
    
    public String getDescription()
    {
	return label;
    }
}
