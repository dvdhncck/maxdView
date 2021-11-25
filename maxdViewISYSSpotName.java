import org.ncgr.isys.objectmodel.SpotName;

public class maxdViewISYSSpotName implements SpotName
{
    private String sn;
    
    public maxdViewISYSSpotName(String label)
    {
	this.sn = label;
    }
    
    public String getSpotName()
    {
	return sn;
    }

    public String getName()
    {
	return sn;
    }   
}
