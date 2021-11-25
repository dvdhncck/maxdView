public class WrapperDemo
{
    // ==============================================================
    //
    // this class demonstrates how to build a wrapper 
    // around the maxdView class 
    //
    // the class instantiates a maxdView object and starts
    // the application using the 'getBusy()' method.
    //
    // it then periodically adds or removes a pair of
    // ExprData.ExternalDataSink objects.
    //
    // when the sink objects are added, entries for them will be seen
    // in maxdView's 'Send to' popup menu.
    //
    // ==============================================================


    // ==============================================================
    //
    // the ExternalDataSink interface marks classes that can be sent data
    //
    class WrapperSink implements ExprData.ExternalDataSink
    {
	// these methods specify what sort of data that class can accept
	public boolean likesSpots()            { return true; }
	public boolean likesSpotMeasurements() { return false; }
	public boolean likesClusters()         { return false; }

	// and these methods are used to pass data to the class
	public void consumeSpots(int[] spots_ids) 
	{ 
	    System.out.println(name + " got " + spots_ids.length + " spots"); 
	}
	public void consumeSpotMeasurements(int n_spots, int n_meas, double[][] data) 
	{}
	public void consumeClusters(ExprData.Cluster[] clusters) 
	{}

	// this method returns the name that will be used for this sink in menu entries 
	public String getName() { return name; }

	public WrapperSink(String n) { name = n; }
	private String name;
    };
    
    class WrapperSelListener implements ExprData.ExternalSelectionListener
    {
	public void spotSelectionChanged(int[] spots_ids)
	{
	    System.out.println(spots_ids.length + " spots selected");
	}

	public void clusterSelectionChanged(ExprData.Cluster[] clusters) { }

	public void spotMeasurementSelectionChanged(int[] spot_ids, int[] meas_ids) { }
    };
 
    public static void main(String[] args) 
    {
	try
	{
	    WrapperDemo wd = new WrapperDemo();
	    maxdView app = new maxdView(args);

	    WrapperSink ws1 = wd.new WrapperSink("Wrapper Demo I");
	    WrapperSink ws2 = wd.new WrapperSink("Wrapper Demo II");

	    int ws1_handle = 0;
	    int ws2_handle = 0;

	    app.getBusy();

	    app.getExprData().addExternalSelectionListener(wd.new WrapperSelListener());

	    boolean ws1_in = false;
	    boolean ws2_in = false;

	    // randomly add and remove the two sinks every few seconds....

	    while(true)
	    {
		if(Math.random() > .8) // 20% probabilty
		{
		    if(ws1_in)
		    {
			ws1_in = false;
			app.getExprData().removeExternalDataSink( ws1_handle );
		    }
		    else
		    {
			ws1_in = true;
			ws1_handle = app.getExprData().addExternalDataSink( ws1 );
		    }
		}
		
		if(Math.random() > .8)
		{
		    if(ws2_in)
		    {
			ws2_in = false;
			app.getExprData().removeExternalDataSink( ws2_handle );
		    }
		    else
		    {
			ws2_in = true;
			ws2_handle = app.getExprData().addExternalDataSink( ws2 );
		    }
		}

		try
		{
		    Thread.sleep(10000);
		}
		catch(InterruptedException ie)
		{
		    
		}
		
		
	    }
		
	}

	catch(Exception ex) 
	{
	    System.err.println("Unexpected exception.\nSave your data and run like hell.\n" + ex);
	}
	catch(Error er) 
	{
	    System.err.println("Unexpected error.\nSave your data and run like hell.\n" + er);
	}

    }
}
