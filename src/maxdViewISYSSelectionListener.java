import org.ncgr.isys.system.*;
import org.ncgr.isys.system.DefaultIsysObjectCollection;
import org.ncgr.util.*;
import org.ncgr.isys.system.event.*;
/*
import org.ncgr.isys.objectmodel.GeneSymbol;
import org.ncgr.isys.objectmodel.ORFName;
*/

// ========================================================================================
// ====  maxdViewISYSSelectionListener  ===================================================
//
//  handles incoming ISYS selection events and passes them to the wrapped maxdView object
//
// ========================================================================================


public class maxdViewISYSSelectionListener implements IsysEventListener
{
    public maxdViewISYSSelectionListener(maxdViewISYSClient client_, maxdView mview_)
    {
	client = client_;
	mview = mview_;
    }

    // ==================================================================================
    //
    // receives all ISYS selection events from synchronisation partners
    //
    // ==================================================================================

    public void handleEvent(IsysEvent event)
    {
	if (event instanceof ItemSelectedEvent) 
	{
	    debug("ItemSelectedEvent");

	    IsysObjectCollection data = event.getData();

	    // have to make sure we don't rebroadcast ISYS events
	    // that we have just recieved....

	    // temporarily disable the selection listener.....
	    //client.mv_sel_listener.setEnabled(false);

	    // int[] spot_ids = client.convertISYSObjectsToSpotIDs( data );
	    int[] spot_ids = client.data_grabber.getSpotIDs( data );

	    // need to add these 'spot_ids' to the current selection
	    if((spot_ids != null) && (spot_ids.length > 0))
	    {
		mview.getExprData().addToSpotSelection( spot_ids );
		
		if(client.scroll_to_new_selection)
		{
		    // mview.getDataPlot().repaint();
		    mview.getDataPlot().displaySpot( spot_ids[0] );
		}
		else
		{
		    mview.getDataPlot().repaint();
		}
	    }

	    client.cur_isys_selection = mview.getExprData().getSpotSelection();

	    // and now re-enable the selection listener.....
	    //client.mv_sel_listener.setEnabled(true);

	    return;
	}

	if (event instanceof ItemDeselectedEvent)          
	{
	    debug("ItemDeselectedEvent");
	    
	    IsysObjectCollection data = event.getData();
	    
	    
	    // temporarily disable the selection listener.....
	    //client.mv_sel_listener.setEnabled(false);
	    
	    // int[] spot_ids = client.convertISYSObjectsToSpotIDs( data );
	    int[] spot_ids = client.data_grabber.getSpotIDs( data );
	    if(spot_ids != null)
	    {
		// need to add remove 'spot_ids' from the current selection
		
		mview.getExprData().removeFromSpotSelection( spot_ids );

		mview.getDataPlot().repaint();
	    }

	    client.cur_isys_selection = mview.getExprData().getSpotSelection();

	    // and now re-enable the selection listener.....
	    //client.mv_sel_listener.setEnabled(true);

	    return;
	}

	if (event instanceof DeselectAllEvent)
	{
	    // debug("DeselectAllEvent");
	    
	    debug("DeselectAllEvent");
	    
	    // temporarily disable the selection listener.....
	    //client.mv_sel_listener.setEnabled(false);
	    
	    // do the local update
	    mview.getExprData().clearSpotSelection();
	    mview.getDataPlot().repaint();

	    // this is an ABSOLUTE event, so it should be rebroadcast...
	    DeselectAllEvent deselect_all_event = new DeselectAllEvent();
	    Isys.getInstance().fireEvent(deselect_all_event, client);
	    
	    client.cur_isys_selection = null;

	    // and now re-enable the selection listener.....
	    //client.mv_sel_listener.setEnabled(true);

	    return;
	}  

	debug("unhandled ISYS event");
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
	    System.err.println("miISYSSelectionListener: " + msg);
	// mview.infoMessage("maxdViewISYSClient: " + msg);
    }

    private maxdView mview;
    private maxdViewISYSClient client;

}

