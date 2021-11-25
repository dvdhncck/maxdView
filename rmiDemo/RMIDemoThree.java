import java.rmi.server.*;
import java.rmi.*;
import java.io.*;
import java.util.Vector;

//
// demo of the RemoteDataSink feature
//
// an RMIDemoThree object is created and sent to maxdView
// which adds an entry to the "Send to..." submenu 
// 
// this class has to be Serializable so it can be transported by RMI
//

public class RMIDemoThree implements ExprData.RemoteDataSink, Serializable
{
    // ===== implementation of the interface ======================================
    //
    public void consumeSpotMeasurements(int n_spots, int n_meas, double[][] data)
    {
    }

    public void consumeClusters(ExprData.ClusterHandle[] ch)
    {
    }

    public void consumeSpots(int[] spot_ids)
    {
	System.out.println(spot_ids.length + " spots received");
	
	try
	{
	    for(int s=0; s < spot_ids.length; s++)
		System.out.println( ri.getProbeName( spot_ids[s] ) );
	}
	catch(java.rmi.RemoteException re)
	{
	}
    }

    public boolean likesSpots() { return true; }
    public boolean likesClusters() { return false; }
    public boolean likesSpotMeasurements() { return false; }

    public String getName() { return "RMIDemoThree"; }


    // ===== constructor notifies maxdView about the newly created object =========
    //
    public RMIDemoThree(RemoteExprDataInterface ri_)
    {
	ri = ri_;

	try 
	{
	    System.out.println( "RMIDemoThree....");

	    int rds_handle = ri.addRemoteDataSink(this);

	    System.out.println( "  remote data sink added");
	    
	    try 
	    {
		Thread.sleep(10000);
	    }
	    catch (InterruptedException e)
	    {
	    }
	    
	    ri.removeRemoteDataSink(rds_handle);

	    System.out.println( "  remote data sink removed");
	    

	}
	catch (Exception e) 
	{
	    System.err.println("RMIDemoThree exception: " + 
			       e.getMessage());
	    e.printStackTrace();
	}
    }

    private RemoteExprDataInterface ri;
}
