import java.rmi.server.*;
import java.rmi.*;
import java.io.*;
import java.util.Vector;

//
// demo of the RemoteSelectionListener feature
//
// an object is created and sent to maxdView which will invokes the 
// spotSelectedChanged() method whenever the spot selection is changed
// 

public class RMIDemoFour implements ExprData.RemoteSelectionListener, Serializable
{
    public void spotSelectionChanged(int[] spot_ids)
    {
	System.out.println( spot_ids.length + " spots selected");
    }
    
    public void clusterSelectionChanged(ExprData.Cluster[] clusters) { }

    public void spotMeasurementSelectionChanged(int[] spot_ids, int[] meas_ids) { }
    
    public RMIDemoFour(RemoteExprDataInterface ri)
    {
	try 
	{
	    System.out.println( "RMIDemoFour....");
	   
	    int listener_handle = ri.addRemoteSelectionListener(this);
	    
	    System.out.println( "  remote listener added");
	    
	    try 
	    {
		Thread.sleep(10000);
	    }
	    catch (InterruptedException e)
	    {
	    }
	    
	    ri.removeRemoteSelectionListener(listener_handle);
	    
	    System.out.println( "  remote listener removed");

	}
	catch (Exception e) 
	{
	    System.err.println("RMIDemoFour exception: " + 
			       e.getMessage());
	    e.printStackTrace();
	}
    }
}
