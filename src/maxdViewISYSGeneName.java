import org.ncgr.isys.objectmodel.GeneName;

public class maxdViewISYSGeneName implements GeneName
{
    private String gene;
    
    public maxdViewISYSGeneName(String label)
    {
	this.gene = label;
    }
    
    public String getGeneName()
    {
	return gene;
    }
    public String getName()
    {
	return gene;
    }   
}
