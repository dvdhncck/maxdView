import org.ncgr.isys.system.IsysObject;
import org.ncgr.isys.objectmodel.ORFName;


class maxdViewISYSORFName implements ORFName
{

    public maxdViewISYSORFName(String name)
    {
	this.name = name;
    }
    
    public String getORFName()
    {
	return name;
    }
    
    String name;
}
