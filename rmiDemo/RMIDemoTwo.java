import java.rmi.server.*;
import java.rmi.*;
import java.io.*;
import java.util.Vector;

//
// demo of the RemoteExprDataObserver feature
//
// an RMIDemoTwo object is created and sent to maxdView
// which calls the methods below whenever update events occur
// 
// this class has to be Serializable so it can be transported by RMI
//

public class RMIDemoTwo implements ExprData.RemoteExprDataObserver, Serializable
{
    public void dataUpdate (ExprData.DataUpdateEvent due)
    {
	System.out.println("RMIDemoTwo: data update");
    }
    
    public void clusterUpdate (ExprData.ClusterUpdateEvent cue)
    {
	System.out.println("RMIDemoTwo: cluster update");
    }

    public void measurementUpdate (ExprData.MeasurementUpdateEvent mue)
    {
	System.out.println("RMIDemoTwo: measurement update");
    }
    
    public void environmentUpdate (ExprData.EnvironmentUpdateEvent eue)
    {
	System.out.println("RMIDemoTwo: environment update");
    }

    public RMIDemoTwo(RemoteExprDataInterface ri)
    {
	try 
	{
	    System.out.println( "RMIDemoTwo....");

	    int redo_handle = ri.addRemoteDataObserver(this);
	    
	    System.out.println( "  remote observer added");

	    try 
	    {
		Thread.sleep(10000);
	    }
	    catch (InterruptedException e)
	    {
	    }
	    
	    ri.removeRemoteDataObserver(redo_handle);
	    
	    System.out.println( "  remote observer removed");
	    
	}
	catch (Exception e) 
	{
	    System.err.println("RMIDemoTwo exception: " + 
			       e.getMessage());
	    e.printStackTrace();
	}
    }
 }
