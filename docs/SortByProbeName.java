import java.util.Vector;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Enumeration;

public class SortByProbeName implements Plugin
{
    private maxdView mview;
    private ExprData edata;

    public SortByProbeName(maxdView mview_)
    {
        mview = mview_;
        edata = mview.getExprData();
    }
    
    public void startPlugin()
    {
        doSort();
    }
    
    public void stopPlugin()
    {

    }

    public PluginInfo getPluginInfo()
    { 
        return new PluginInfo(
              "Sort by Probe name", 
              "transform", 
              "Orders the Spots alphabetically by Probe names", 
              "",
              1, 0, 0 );
    }
    public PluginCommand[] getPluginCommands()
    {
        return null;
    }

    public void  runCommand(String name, String[] args, CommandSignal done) 
    { } 
    
    // ----------------------------------------------------------------

    public void doSort()
    {
        Hashtable pnht = edata.getProbeNameHashtable();
        
        // build an array of probe names...

        String[] pnames = new String[pnht.size()];
        
        int p = 0;
        for (Enumeration e = pnht.keys(); e.hasMoreElements() ;) 
        {
            String pname = (String) e.nextElement();
            pnames[p++] = pname;
        }
        
        // sort this array...
        
        Arrays.sort(pnames);
        
        // and now build the spot id array using this sorted list of names
        
        int[] new_order = new int[edata.getNumSpots()];
        
        int so = 0;
        
        for (p=0; p < pnames.length; p++)
        {
            Vector sids = (Vector) pnht.get( pnames[p] );
            
            for(int s=0; s < sids.size(); s++)
            {
                int sid = ((Integer) sids.elementAt(s)).intValue();
                
                new_order[so++] = sid;
            }
        }
        
        edata.setSpotOrder(new_order);
    }
}
