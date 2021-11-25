import org.ncgr.isys.objectmodel.SequenceText;

public class maxdViewISYSSequenceText implements SequenceText
{
    private String label;
    
    public maxdViewISYSSequenceText(String label)
    {
	this.label = label;
    }
    
    public String getSequenceText()
    {
	return label;
    }

    public java.lang.Number getLength()
    {
	return new Integer(label.length());
    }
    public String getUnits()
    {
	return "unknown";
    }
    public String getName()
    {
	return label;
    }   
}
