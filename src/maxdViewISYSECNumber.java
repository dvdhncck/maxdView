import org.ncgr.isys.objectmodel.ECNumber;

public class maxdViewISYSECNumber implements ECNumber
{
    private String gene;
    
    public maxdViewISYSECNumber(String label)
    {
	this.gene = label;
    }
    
    public String getECNumber()
    {
	return gene;
    }

    public String getIdentifier() 
    {
	return gene;
    }
}
