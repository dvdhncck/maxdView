public class Selection
{
    public Selection( String[] group_titles, int[] ids, int[] groups )
    {
	this.group_titles = group_titles;
	this.ids          = ids;       // either an array of Spot indices or Measurement indices
	this.groups       = groups;    // optional mapping indicating how the indices are grouped
    }
    
    public String getGroupTitle( int index )
    {
	try
	{
	    return ( groups == null ) ? group_titles[ 0 ] : group_titles[ groups[ index ] ];
	}
	catch( ArrayIndexOutOfBoundsException aioobe )
	{
	    return null;
	}
    }
    
    public int getGroupCount()
    { 
	return group_titles == null ? 0 : group_titles.length;
    }
    
    public int getSize()
    {
	return ids == null ? 0 : ids.length;
    }
    
    public int getID( int index )
    {
	return ids[ index ];
    }
    
    private String[] group_titles;
    
    private int[] ids;
    
    private int[] groups;
}