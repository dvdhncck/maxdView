import org.ncgr.isys.objectmodel.ProbeName;

public class maxdViewISYSProbeName implements ProbeName
{
    private String pn;
    
    public maxdViewISYSProbeName(String label)
    {
	this.pn = label;
    }
    
    public String getProbeName()
    {
	return pn;
    }
    public String getName()
    {
	return pn;
    }   
}
