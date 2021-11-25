
public void doIt()
{
    java.util.Hashtable pnht = edata.getProbeNameHashtable();

    // build an array of probe names...

    String[] pnames = new String[pnht.size()];
    
    int p = 0;
    for (java.util.Enumeration e = pnht.keys(); e.hasMoreElements() ;) 
    {
	String pname = (String) e.nextElement();
	pnames[p++] = pname;
    }

    // sort this array...

    Arrays.sort(pnames);
    
    // and now build the spot id array using this sorted list of names

    int[] new_order = new int[edata.getNumSpots()];

    
    for (p=0; p < pnames.length; p++)
    {
	Vector sids = (Vector) pnht.get( pnames[p] );

	for(int s=0; s < sids.size(); s++)
	{
	    int sid = ((Integer) sids.elementAt(s)).intValue();
	    
	    new_order[s++] = sid;
	}
    }
    
    edata.setSpotOrder(new_order);
}
