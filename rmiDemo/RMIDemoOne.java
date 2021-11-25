import java.rmi.server.*;
import java.rmi.*;
import java.io.*;
import java.util.Vector;

//
// demo of the RemoteExprDataInterface and ClusterHandle features
//
// the probe names of the data are queried and a cluster is constructed
// containing a random selection of the names
// 

public class RMIDemoOne
{
    public RMIDemoOne(RemoteExprDataInterface ri) 
    {
	try 
	{
	    System.out.println( "RMIDemoOne....");
	    
	    Vector elem_data = new Vector();
	    
	    final int ns = ri.getNumSpots();

	    for(int s=0; s < ns; s++)
	    {
		String pn = ri.getProbeName(s);
		if(pn != null)
		    if (Math.random() > 0.80 )  // 20% prob
			elem_data.addElement(pn);
	    }
	    
	    ExprData.ClusterHandle new_clust = ri.createClusterHandle( "Random probes from RMI Demo Two", 
								       ExprData.ProbeName, 
								       elem_data );
	    
	    ri.addClusterHandle( ri.getRootClusterHandle(), new_clust );

	    System.out.println( "  cluster made ok, maxdView ID=" + new_clust.getId());
	    
	}
	catch (Exception e) 
	{
	    System.err.println("RMIDemoOne exception: " + 
			       e.getMessage());
	    e.printStackTrace();
	}
    }
}
