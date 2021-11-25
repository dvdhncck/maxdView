//package uk.ac.man.bioinf.maxdLoad2;

public class Instance
{
    public String id;
    public String name;
    
    public Instance(String n, String i)
    {
	name = n; id = i;
    } 
    
    public String toString()
    {
	return id + ":" + name;
    }

    public boolean equals( Instance i )
    {
	if( i == null )
	    return false;

	if( id == null )
	    return ( i.id == null );

	return ( ( id.equals( i.id ) ) && ( name.equals( i.name ) ) );
    }

    public boolean isDefined()
    {
	return ( ( id != null ) && ( id.length() > 0 ) );
    }
}
