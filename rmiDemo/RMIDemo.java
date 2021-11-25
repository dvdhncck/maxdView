import java.rmi.server.*;
import java.rmi.*;
import java.io.*;
import java.util.Vector;

//
//  options:  -host HOSTNAME [ -demo 1|2|3|4 ]
//
public class RMIDemo
{
    public void runDemo(RemoteExprDataInterface ri, int demo_id)
    {
	switch(demo_id)
	{
	case 1:
	    new RMIDemoOne(ri);    // creates a cluster of Probe names
	    break;

	case 2:
	    new RMIDemoTwo(ri);    // creates a RemoteExprDataObserver which receives events remotely
	    break;

	case 3:
	    new RMIDemoThree(ri);  // creates a RemoteDataSink which appears in the "Send to" submenu
	    break;

	case 4:
	    new RMIDemoFour(ri);   // creates a RemoteSelectionListener which is notified whenever the selection changes
	    break;

	case 5:
	    new RMIDemoFive(ri);   // creates a hierarchy of Clusters
	    break;
	}

    }

    public static void main(String[] args) 
    {
	try 
	{
	    if (System.getSecurityManager() == null) 
	    {
		System.setSecurityManager(new RMISecurityManager());
	    }

	    String hostname = "localhost";
	    String demoname = null;

	    for(int a=0; a < args.length; a++)
	    {
		if(args[a].equals("-host"))
		    hostname = args[a+1];
		if(args[a].equals("-demo"))
		    demoname = args[a+1];
	    }

	    
	    String remote_object_name = "//" + hostname + "/maxdViewRemoteExprData";

	    System.out.println( "trying to bind to " +  remote_object_name);

	    RemoteExprDataInterface ri = (RemoteExprDataInterface) Naming.lookup( remote_object_name);
	    
	    System.out.println( "remote data has "+ ri.getNumMeasurements() + 
				" measurements, " + ri.getNumSpots() + 
				" spots and " + ri.getNumClusters() + 
				" clusters  ");

	    RMIDemo app = new RMIDemo();

	    if(demoname == null)
	    {
		app.runDemo(ri, 1);
		app.runDemo(ri, 2);
		app.runDemo(ri, 3);
		app.runDemo(ri, 4);
		app.runDemo(ri, 5);
	    }
	    else
	    {
		if(demoname.equals("1"))
		    app.runDemo(ri, 1);
		if(demoname.equals("2"))
		    app.runDemo(ri, 2);
		if(demoname.equals("3"))
		    app.runDemo(ri, 3);
		if(demoname.equals("4"))
		    app.runDemo(ri, 4);
		if(demoname.equals("5"))
		    app.runDemo(ri, 5);
	    }
	}

	catch (Exception e) 
	{
	    System.err.println("Client exception: " + 
			       e.getMessage());
	    e.printStackTrace();
	}
    }
}
