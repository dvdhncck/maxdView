import org.ncgr.isys.objectmodel.GeneSymbol;

public class maxdViewISYSGeneSymbol implements GeneSymbol
{
    private String gene;
    
    public maxdViewISYSGeneSymbol(String label)
    {
	this.gene = label;
    }
    
    public String getGeneSymbol()
    {
	return gene;
    }
    public String getName()
    {
	return gene;
    } 
}
