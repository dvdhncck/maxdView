import java.rmi.server.*;
import java.rmi.*;
import java.io.*;
import java.util.Vector;

//
// demo of the RemoteExprDataInterface and ClusterHandle features
//
// contructs a  cluster hierarchy containing all of the spots
// in a roughly balanced tree
// 
//
// the important feature of this demo is that the hierarchy is
// created recursively, with the local root only added to the
// 'master' root once all the leaves are completed.
//
public class RMIDemoFive
{
    public RMIDemoFive(RemoteExprDataInterface ri) 
    {
	try 
	{
	    System.out.println( "RMIDemoFive....");
	    
	    final int n_elems = 4;

	    final int ns = ri.getNumSpots();

	    if(ns > 0)
	    {
		//get a handle to the root cluster to use as a parent below

		ExprData.ClusterHandle local_root = ri.createClusterHandle( "RMI Demo 5", ExprData.SpotName, new Vector() );
		
		// start the recusion at the top

		n_leaves = n_branches = 0;
		splitSpace( ri, local_root, "N=11", 0, ns-1, 11 );

		System.out.println( n_branches + " branches, " + n_leaves + " leaves");
		
		n_leaves = n_branches = 0;
		splitSpace( ri, local_root, "N=18", 0, ns-1, 18 );

		System.out.println( n_branches + " branches, " + n_leaves + " leaves");
		
		// add the newly created handle to the root
		ExprData.ClusterHandle master_root = ri.getRootClusterHandle();
		
		if(master_root == null)
		    System.out.println("root is null...."); 
		else
		    System.out.println("root is ok...."); 

		ri.addClusterHandle(master_root, local_root);
	    }	    
	}
	catch ( RemoteException re)
	{
	    System.err.println("RMIDemoFive exception: " + 
			       re.getMessage());
	    re.printStackTrace();
	}

	catch (Exception e) 
	{
	    System.err.println("RMIDemoFive exception: " + 
			       e.getMessage());
	    e.printStackTrace();
	}
    }

    ExprData.ClusterHandle splitSpace( final RemoteExprDataInterface ri, 
				       final ExprData.ClusterHandle parent,
				       final String name,
				       final int start, final int end, final int max_size ) throws RemoteException
    {
	if(parent == null)
	    System.out.println("parent is null....name=" + name); 

	ExprData.ClusterHandle result = null;

	int len = end - start + 1;

	if(len > max_size)
	{
	    int half_len = len / 2;
	    int e1 = start + half_len;
	    int s2 = e1 + 1;

	    // make a new cluster and divide the space in two...

	    result = ri.createClusterHandle( parent, name, ExprData.SpotName, new Vector() );

	    splitSpace( ri, result, name+".L", start, e1, max_size );
	    splitSpace( ri, result, name+".R", s2, end, max_size );

	    n_branches++;
	}
	else
	{
	    Vector elems = new Vector();
	    for(int s=start; s <= end; s++)
		elems.addElement( ri.getSpotName(s) );
	    result = ri.createClusterHandle( parent, 
					     name, 
					     ExprData.SpotName, 
					     elems );

	    //System.out.println("Leaf: '" + name + "' with " + elems.size() + " elements");

	    n_leaves++;
	    
	}
	return result;
    }

    private int n_leaves;
    private int n_branches;
}
