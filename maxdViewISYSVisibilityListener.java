import org.ncgr.isys.system.*;
import org.ncgr.isys.system.DefaultIsysObjectCollection;
import org.ncgr.util.*;
import org.ncgr.isys.system.event.*;
/*
import org.ncgr.isys.objectmodel.GeneSymbol;
import org.ncgr.isys.objectmodel.ORFName;
*/
import java.util.HashSet;

// ========================================================================================
// ====  maxdViewISYSVisibilityListener  ===================================================
//
//  handles incoming ISYS hide/show events and uses a maxdView filter 
//  to pass them to the wrapped maxdView object
//
// ========================================================================================


public class maxdViewISYSVisibilityListener implements IsysEventListener, ExprData.Filter, ExprData.ExprDataObserver
{
    public maxdViewISYSVisibilityListener(maxdViewISYSClient client_, maxdView mview_)
    {
	client = client_;
	mview = mview_;
	filter_is_installed = false;

	hidden = new HashSet();
	currently_hidden_spots = new int[0];
	mview.getExprData().addObserver(this);
    }

    public void finalize()
    {
	mview.getExprData().removeObserver(this);
    }

    // ==================================================================================
    //
    // receives all ISYS show/hide events from synchronisation partners
    //
    // ==================================================================================

    public void handleEvent(IsysEvent event)
    {
	if(client.recv_hide_show_events == false)
	    return;
	
	if (event instanceof ItemShownEvent) 
	{
	    debug("ItemShownEvent");

	    IsysObjectCollection data = event.getData();
	   
	    int[] spot_ids = client.data_grabber.getSpotIDs( data );

	    final int ns = spot_ids.length;

	    debug(ns + " elements shown");

	    for(int s=0; s < ns; s++)
		hidden.remove( new Integer( spot_ids[s] ));
	    
	    updateFilter();

	    return;
	}

	if (event instanceof ItemHiddenEvent) 
	{
	    debug("ItemHiddenEvent");

	    IsysObjectCollection data = event.getData();
	   
	    int[] spot_ids = client.data_grabber.getSpotIDs( data );

	    final int ns = spot_ids.length;

	    debug(ns + " elements hidden");

	    for(int s=0; s < ns; s++)
		hidden.add( new Integer( spot_ids[s] ));
	    
	    updateFilter();

	    return;
	}

	if (event instanceof ShowAllEvent) 
	{
	    debug("ShowAllEvent");

	    hidden.clear();
	    currently_hidden_spots = new int[0];

	    updateFilter();

	    return;
	}

	debug("unhandled ISYS event");
    }

    // ========================================================================================
    //
    //  updateFilter
    //
    // ========================================================================================

    private void updateFilter()
    {
	if(hidden.size() > 0)
	{
	    if(filter_is_installed)
	    {
		debug("updateFilter(): existing filter updated");
		mview.getExprData().notifyFilterChanged(/*(ExprData.Filter)*/ this);
	    }
	    else
	    {
		debug("updateFilter(): filter installed");
		filter_is_installed = true;
		mview.getExprData().addFilter( this );
		// mview.getExprData().notifyFilterChanged(this);  // NOT NEEDED!
	    }
	}
	else
	{
	    if(filter_is_installed)
	    {
		// remove filter

		debug("updateFilter(): filter removed");
		mview.getExprData().removeFilter( this );
		// must remain enabled until removed
		filter_is_installed = false;
	    }
	    else
	    {
		// nothing to do....
		debug("updateFilter(): nothing to do");
	    }
	}

	// now reapply the maxdView filters
	restartFilterChecker();
    }

    // ========================================================================================
    //
    //  the filter implementation
    //
    // ========================================================================================

    public boolean filter(int spot) // should this spot be displayed?
    {
	return ( hidden.contains( new Integer( spot )) );
    }
    
    public boolean enabled()  // is it currently enabled?
    {
	return filter_is_installed;
    }

    public String  getName() // descriptive name of this filter
    {
	return "External ISYS Filter";
    }


    // ========================================================================================
    //
    //  a data observer so that the effects of other ExprData.Filters can be
    //  transmitted to ISYS
    //
    // ========================================================================================

    public void dataUpdate(ExprData.DataUpdateEvent due)
    {
	if(client.send_hide_show_events == false)
	    return;

	switch(due.event)
	{
	case ExprData.SizeChanged:
	    // debug("dataUpdate(): size change....");
	    restartFilterChecker();
	    break;
	}
    }
    
    public void clusterUpdate(ExprData.ClusterUpdateEvent cue)
    {
    }

    public void measurementUpdate(ExprData.MeasurementUpdateEvent mue)
    {
    }
    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
    }


    private boolean abort_thread   = false;
    private boolean thread_running = false;

    public void restartFilterChecker()
    {
	if(thread_running)
	{
	    abort_thread = true;
	    int counter = 0;
	    // System.out.println("waiting for existing thread to stop");
	    while((counter < 25) && (thread_running == true))
	    {
		try
		{
		    Thread.sleep(100);
		}
		catch(InterruptedException tie)
		{
		}
		counter++;
	    }
	    if(thread_running == true)
		System.out.println("restartFilterChecker(): WARNING: existing thread had not died");
	}
	abort_thread = false;
	thread_running = true;
	new UpdateThread().start();
    }
    

    private class UpdateThread extends Thread
    {
	public void run()
	{
	    ExprData edata = mview.getExprData();
	    if( filter_is_installed && ( edata.getNumFilters() == 1 ))
	    {
		// this is the only filter, so we need to synchronize
		// with the ISYS show/hide
		
		// don't need to do anything?

		currently_hidden_spots = new int[0];
	    }
	    else
	    {
		// need to check all other filters
		
		final int nspots = mview.getExprData().getNumSpots();
		int spot = 0;

		HashSet hidden_by_all_filters = new HashSet();
		
		if(edata.getNumFilters() > 0)
		{
		    while((spot < nspots) && (!abort_thread))
		    {
			if(edata.filter(spot))
			    hidden_by_all_filters.add( new Integer( spot ));
			spot++;
		    }
		}
		
		// debug("there are a total of " + hidden_by_all_filters.size() + " spots hidden");

		
		// which other spots need to be hidden?
		
		java.util.Iterator it = hidden.iterator();
		Integer sid = null;
		while(it.hasNext())
		{
		    hidden_by_all_filters.remove( (Integer) it.next() );
		}
		
		// debug("excluding ISYS hides, there are " + hidden_by_all_filters.size() + " spots hidden");
		
		if(!abort_thread)
		{
		    
		    final int n_hides = hidden_by_all_filters.size();
		    int[] new_hidden_spots = new int[n_hides];
		    int h = 0;
		    it = hidden_by_all_filters.iterator();
		    while(it.hasNext())
		    {
			new_hidden_spots[h++] = ( ( (Integer) it.next() ).intValue() );
		    }
		    
		    // how does this differ from the current ISYS show/hide ?
		    
		    debug("total hides=" + new_hidden_spots.length + " current total hides=" + currently_hidden_spots.length);
		    int[] spots_to_hide = client.getDifference( new_hidden_spots, currently_hidden_spots );
		    int[] spots_to_show = client.getDifference( currently_hidden_spots, new_hidden_spots );
		    
		    if(!abort_thread)
		    {
			maxdViewISYSDataPackager data_packer = client.getDataPackager();
			final Isys isys = Isys.getInstance();
			
			if((spots_to_hide != null) && (spots_to_hide.length > 0))
			{
			    debug("need to hide " + spots_to_hide.length + " spots");
			    
			    IsysObjectCollection hide_spots_coll = data_packer.packageData( spots_to_hide );
			    ItemHiddenEvent hide_data_event = new ItemHiddenEvent(hide_spots_coll);
			    isys.fireEvent(hide_data_event, client);
			}
			
			if((spots_to_show != null) && (spots_to_show.length > 0))
			{
			    debug("need to show " + spots_to_show.length + " spots");
			    
			    IsysObjectCollection show_spots_coll = data_packer.packageData( spots_to_show );
			    ItemShownEvent show_data_event = new ItemShownEvent(show_spots_coll);
			    isys.fireEvent(show_data_event, client);
			}
			
			currently_hidden_spots = new_hidden_spots;
		    }
		}
	    }
	    thread_running = false;
	}
    }
    // ========================================================================================
    //
    //  utils
    //
    // ========================================================================================
    //
    //  print debug messages
    //
    // ========================================================================================

    private void debug(String msg)
    {
	if(client.do_debug)
	    System.err.println("miISYSVisibilityListener: " + msg);
	// mview.infoMessage("maxdViewISYSClient: " + msg);
    }

    private maxdView mview;
    private maxdViewISYSClient client;
    private boolean filter_is_installed;
    private HashSet hidden;
    private int[] currently_hidden_spots;
}

