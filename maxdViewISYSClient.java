import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.HashSet;

import java.util.Collection;
import java.util.Iterator;

import java.io.File;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.border.*;

import org.ncgr.util.*;
import org.ncgr.isys.system.event.*;

import org.ncgr.isys.system.*;
import org.ncgr.isys.system.DefaultIsysObject;

import org.ncgr.isys.system.DefaultIsysObjectCollection;

// used for debug
import org.ncgr.isys.objectmodel.GeneSymbol;
import org.ncgr.isys.objectmodel.ORFName;
import org.ncgr.isys.objectmodel.ECNumber;
import org.ncgr.isys.objectmodel.SequenceText;
import org.ncgr.isys.objectmodel.Description;
import org.ncgr.isys.objectmodel.ProbeName;
import org.ncgr.isys.objectmodel.GeneName;
import org.ncgr.isys.objectmodel.SpotName;

//ISYSADF
import org.ncgr.isys.util.DynamicDiscoveryDialog;
import org.ncgr.isys.util.DataServiceResultCallback;
import org.ncgr.isys.util.IsysOptionPane;


// ========================================================================================
// ====  m a x d V i e w I S Y S C l i e n t   ============================================
//
//  does most of the work....
//
// ========================================================================================
// ========================================================================================

class maxdViewISYSClient implements Client, AsynchronousCallback
{

    public maxdViewISYSClient()
    {
	try
	{
	    // 1. make a maxdView object and configure it for use as an ISYS component
	    //
	    mview = new maxdView(null);

	    mview.setOption(maxdView.CanExit, false);
	    //mview.setOption(maxdView.DebugPlugins, true);


	    //ISYSADF: to avoid other component's exceptions from looking as if maxd is
	    //catching them...
	    //
	    //ISYSDJH: good call!
	    //
	    mview.setOption(maxdView.LogErrorsToFile, false);

	    final String default_loc = "Components" + File.separator + "maxdView" + File.separator + "lib";
	    String loc = System.getProperty("maxdView.location");

	    if((loc == null) || (loc.length() == 0))
		loc = default_loc;
	    
	    mview.setTopDirectory( loc );
	    
	    mview.addExitListener( new ActionListener()
		{
		    // this will be called when maxdView closes down
		    public void actionPerformed(ActionEvent e)
		    {
			// System.out.println("bye bye maxdView...");
			Isys isys = Isys.getInstance();		
			isys.unregisterClient(maxdViewISYSClient.this);		
			System.gc();
		    }
		});
 
	    mview.addPopupMenuEntry( "ISYS Options", new ActionListener()
		{
		    // this will be called when maxdView closes down
		    public void actionPerformed(ActionEvent e)
		    {
			openOptionsPanel();
		    }
		});
	    
	    data_packer  = new maxdViewISYSDataPackager(this, mview);
	    data_grabber = new maxdViewISYSDataGrabber(this, mview, data_packer);

	    // 2. add a SelectionListener to maxdView so we can notify ISYS when the selection changes
	    //
	    mv_sel_listener = new SelectionListener();
	    
	    int mv_sel_listener_handle = mview.getExprData().addExternalSelectionListener( mv_sel_listener );

	    // 3. add some ISYSSelectionListeners to ISYS to we can notify maxdView when the selection changes
	    //
	    maxdViewISYSSelectionListener isys_sel_listener = new maxdViewISYSSelectionListener(this, mview);
	    Isys isys = Isys.getInstance();
	    
	    isys.addEventListener(ItemSelectedEvent.class,   isys_sel_listener, this);                         
	    isys.addEventListener(ItemDeselectedEvent.class, isys_sel_listener, this);
	    isys.addEventListener(DeselectAllEvent.class,    isys_sel_listener, this);   

	    // 4. add an ISYSSelectionListeners to ISYS to we can notify maxdView when the visibility changes
	    //
	    maxdViewISYSVisibilityListener isys_vis_listener = new maxdViewISYSVisibilityListener(this, mview);

	    isys.addEventListener(ItemShownEvent.class,      isys_vis_listener, this);   
	    isys.addEventListener(ItemHiddenEvent.class,     isys_vis_listener, this);   
	    isys.addEventListener(ShowAllEvent.class,        isys_vis_listener, this);   


	    

	}
	catch(java.rmi.RemoteException re)
	{
	    // ooops...
	}
    }
   
    public void show( )
    {
	if(mview == null)
	    System.err.println("FATAL: maxdView object not found");
	else
	    mview.getBusy();
	
    }
 
    public maxdViewISYSDataPackager getDataPackager() { return data_packer; } 
    public maxdViewISYSDataGrabber  getDataGrabber()  { return data_grabber; } 

    // ========================================================================================
    // ====  AsynchronousCallback  ===========================================================
    //
    // used to invoke data services asynchronously
    //  
    // ========================================================================================

    //ISYSADF: implement AsynchronousCallback so that we can
    //invoke data services asynchronously
    public void returnAsynchronousResult( Object result, AsynchronousService svc )
    {
	if ( result instanceof IsysObjectCollection ) 
	    handleDataServiceResult((IsysObjectCollection)result, svc);
	else
	    IsysOptionPane.notify( null, svc.getDisplayName() + " returned data in an unknown" + " format. Result cannot be viewed." );
    }
    
    void handleDataServiceResult(IsysObjectCollection result, Service svc)
    {
	if(allow_data_capture)
	{
	    data_grabber.grabData( result, true );
	}
	else
	{
	    if (result.size() == 0) 
	    {
		IsysOptionPane.notify( null, svc.getDisplayName() + " returned no data for the given " + "input set" );
		return;
	    }
	    //ISYSADF:
	    //I don't understand native handling of returned data at this point...
	    //anyway, it doesn't seem like this is right, since the data grabber
	    //only does matching, not addition to data model...
	    //data_grabber.grabData( (IsysObjectCollection)result, true );

	    //
	    //ISYSDJH: the grabber _does_ do both things, matching and capturing...
	    //         but anyhow, I've made this behaviour the default.
	    //         there is now an option "Allow data capture" which controls
	    //         whether the maxdViewISYS capture dialog is used or not.
	    //

	    DynamicDiscoveryDialog.open((IsysObjectCollection)result, this, svc);
	}
    }
    
    // ========================================================================================
    // ====  ViewerServiceSpotSink  ===========================================================
    //
    //  the consumer for the spot selection that handles ISYS ViewerServices
    //
    // ========================================================================================

    class ViewerServiceSpotSink implements ExprData.ExternalDataSink
    {
	// these methods specify what sort of data that this class can accept
	public final boolean likesSpots()            { return true; }
	public final boolean likesSpotMeasurements() { return false; }
	public final boolean likesClusters()         { return false; }

	// and these methods are used to pass data to the class
	public final void consumeSpots(int[] spot_ids) 
	{ 
	    debug( "service " + name + " got " + spot_ids.length + " spots"); 

	    {
		//ISYSADF: don't allow the mapping to be changed now!
		//ISYSDJH: i agree...

		IsysObjectCollection data = data_packer.packageData( spot_ids, false );
		
		if(data == null)
		{
		    // the send has been cancelled....
		    return;
		}

		dvs.setSyncPartner( maxdViewISYSClient.this );
		dvs.setViewableData(data);

		try
		{
		    debug( "executing viewer " + name + " with " + data.size() + " objects"); 
		    spawned_viewer = dvs.execute();
		}
		catch(java.lang.reflect.InvocationTargetException ite)
		{
		}
	    }
	}

	public final void consumeSpotMeasurements(int n_spots, int n_meas, double[][] data) 
	{
	    // not currently used
	}
	public final void consumeClusters(ExprData.Cluster[] clusters) 
	{
	    // not currently used
	}

	// this method returns the name that will be used for this sink in 
	// maxdView's "send to" menu entries 
	public final String getName() { return name; }

	public ViewerServiceSpotSink(/*IsysObjectCollection data_,*/ DynamicViewerService dvs_) 
	{
	    dvs = dvs_; 
	    //	    data = data_;
	    name = dvs.getDisplayName() + " (V)"; 
	    spawned_viewer = null;
	}
	private String name;
	private DynamicViewerService dvs;
	//private IsysObjectCollection data;
	private Client spawned_viewer;
    };
 
    // ========================================================================================
    // ====  DataServiceSpotSink  =============================================================
    //
    //  the consumer for the spot selection that handles ISYS DataServices
    //
    // ========================================================================================

    class DataServiceSpotSink implements ExprData.ExternalDataSink
    {
	// these methods specify what sort of data that this class can accept
	public final boolean likesSpots()            { return true; }
	public final boolean likesSpotMeasurements() { return false; }
	public final boolean likesClusters()         { return false; }

	// and these methods are used to pass data to the class
	public final void consumeSpots(int[] spot_ids) 
	{ 
	    debug("viewer " + name + " got " + spot_ids.length + " spots");

	    {
		// dds.setSyncPartner( maxdViewISYSClient.this );

		//ISYSADF: don't allow the mapping to be changed now!
		//ISYSDJH: i agree...

		IsysObjectCollection data = data_packer.packageData( spot_ids, false );

		if(data == null)
		{
		    // the send has been cancelled....
		    return;
		}
		
		try
		{
		    debug( "executing data-service " + name + " with " + data.size() + " objects"); 

		    //dumpObjects("Sending", data);

		    dds.setData(data);
		    
		    //ISYSADF: for asynchronous invocation:
		    if (dds instanceof AsynchronousService)
			((AsynchronousService)dds).setAsynchronousRequester((AsynchronousCallback)maxdViewISYSClient.this);

		    //ISYSADF: if not an asynchronous service, should I be spawning a new thread??
		    //ISYSDJH: yes, probably a good idea.....

		    IsysObjectCollection result = dds.execute();

		    //ISYSADF: for asynchronous invocation:
		    if (dds instanceof AsynchronousService)
			return;

		    debug( "....got a result with " + result.size() + " objects"); 
		    
		    // what to do with the result?
		    //
		    //   extract data and store in maxdView
		    //   or
		    //   offer a selection of ISYS functions that operate on the data

		    if(allow_data_capture)
		    {
			//dumpObjects("Received", result);
			
			data_grabber.grabData( result, true );
		    }
		    else
		    {
			DynamicDiscoveryDialog.open((IsysObjectCollection)result, maxdViewISYSClient.this, dds);
		    }
		}
		catch(java.lang.reflect.InvocationTargetException ite)
		{
		}
	    }
	}

	public final void consumeSpotMeasurements(int n_spots, int n_meas, double[][] data) 
	{
	    // not currently used
	}
	public final void consumeClusters(ExprData.Cluster[] clusters) 
	{
	    // not currently used
	}

	// this method returns the name that will be used for this sink in 
	// maxdView's "send to" menu entries 
	public final String getName() { return name; }

	public DataServiceSpotSink(/*IsysObjectCollection data_,*/ DynamicDataService dds_) 
	{
	    dds = dds_; 
	    name = dds.getDisplayName() + " (D)"; 
	    //data = data_;
	}
	private String name;
	private DynamicDataService dds;
	//private IsysObjectCollection data;
    };

    // ========================================================================================
    // ====  SelectionListener  ===============================================================
    //
    //  monitors the selection state of the wrapped maxdView object and notifys
    //  ISYS when the selection changes
    //
    //    ( sends ISYS a collection of IsysObjects which combine
    //      GeneSymbols with optional ORFNames )
    //
    // ========================================================================================


    class SelectionListener implements ExprData.ExternalSelectionListener
    {
	private boolean enabled = true;

	

	// the listener can be disabled 
	// (this feature is used when ISYS events change the selection)
	//
	public final void setEnabled(boolean state)
	{
	    enabled = state;

	    debug("maxdView SelectionListener is " + enabled);
	}

	public void spotSelectionChanged(int[] spot_ids)
	{
	    
	    final ExprData edata = mview.getExprData();
	    
	    final Isys isys = Isys.getInstance();
	
	    // ----------------------		
	    // two things to do:
	    //   1. update the data sinks (i.e. the entries in maxdViews 'send to' popup menu)
	    //   2. generate ISYS event(s) telling it the selection has changed
	    // ----------------------
	    

	    //
	    // TODO: check whether the class(es) of data has changed since last time
	    //       and if not, step (1) can be avoided.....
	    //  

	    // 1. is now in its own method

	    updateServiceList( spot_ids );

	    // 2. generate ISYS event(s) telling the world the selection has changed
	    
	    if(enabled)
	    {
		// work out how the selection has changed
		
		//
		// TODO:: this pair of ops could be done more efficiently together
		//
		int[] sel_spots   = getDifference( spot_ids, cur_isys_selection );
		int[] unsel_spots = getDifference( cur_isys_selection, spot_ids );
		
		if((sel_spots != null) && (sel_spots.length > 0))
		{
		    debug("spotSelectionChanged(): " + sel_spots.length + " spots selected");
		    
		    IsysObjectCollection sel_spots_coll = data_packer.packageData( sel_spots );
		    ItemSelectedEvent sel_data_event = new ItemSelectedEvent(sel_spots_coll);
		    isys.fireEvent(sel_data_event, maxdViewISYSClient.this);
		}
		
		if((unsel_spots != null) && (unsel_spots.length > 0))
		{
		    debug("spotSelectionChanged(): " + unsel_spots.length + " spots unselected");
		    
		    IsysObjectCollection unsel_spots_coll = data_packer.packageData( unsel_spots );
		    ItemDeselectedEvent unsel_data_event = new ItemDeselectedEvent( unsel_spots_coll );
		    isys.fireEvent(unsel_data_event, maxdViewISYSClient.this);
		}
		
		cur_isys_selection = spot_ids;
	    }
	}
	
	public void clusterSelectionChanged(ExprData.Cluster[] clusters) { }

	public void spotMeasurementSelectionChanged(int[] spot_ids, int[] meas_ids) { }
 
    };

    // ========================================================================================
    //
    //  updateServiceList
    //
    // ========================================================================================
    //
    //  does the dynamic discovery and adds things to maxdView's "Send to" menu
    //
    // ========================================================================================

    Hashtable data_services = new Hashtable();
    Hashtable viewer_services = new Hashtable();
    
	
    public void updateServiceList(  )
    {
	updateServiceList( mview.getExprData().getSpotSelection() );
    }

    public void updateServiceList( int[] spot_ids )
    {
	// System.out.println("updateServiceList() ... ");
	
	final ExprData edata = mview.getExprData();
	    
	// 1. update the data sinks
	
	// 1.1 first remove the existing DataService sinks
	
	for (Enumeration e = data_services.keys(); e.hasMoreElements() ;) 
	{
	    Integer sink_handle = (Integer) e.nextElement();
		edata.removeExternalDataSink( sink_handle.intValue() );
	}
	data_services.clear();
	
	// 1.2 then remove the existing ViewerService sinks
	
	for (Enumeration e = viewer_services.keys(); e.hasMoreElements() ;) 
	{
	    Integer sink_handle = (Integer) e.nextElement();
	    edata.removeExternalDataSink( sink_handle.intValue() );
	}
	viewer_services.clear();
	
	// 1.3 and if any spots are selected, do dynamic discovery to find possible services
	// (maybe should delay this until the selection popup is requested?)
	
	final Isys isys = Isys.getInstance();
	
	if( (spot_ids  != null) && (spot_ids.length > 0) )
	{
	    
	    // TODO:: for dynamic discovery, instead of real data, could have spoof attrs that
	    //        represent the possible attrs in the current selection
	    //        or something like  data_packer.getPossibleISYSAttrs();
	    //
	    // this doesn't work ; why?
	    //
	    //ISYSADF: try not including everything! 
	    //(if they don't ask to have bogus probe/spot names sent out, we shouldn't send them out!!)
	    //
	    //ISYSDJH: well it seemed like a good idea at the time ;)
	    //
	    //IsysObjectCollection current_spot_selection = data_packer.getPossibleISYSAttrs( spot_ids );
	    
	    IsysObjectCollection current_spot_selection = data_packer.packageData( spot_ids );
	    
	    if(current_spot_selection != null)
	    {
		
		if(spot_ids.length > 0)
		{
		    // 1.3.1 add a DataServiceSpotSink for each of the data services
		    
		    DynamicDataService[] dds = isys.discoverDataServices(current_spot_selection);
			
		    for(int s=0; s < dds.length; s++)
		    {
			DataServiceSpotSink ss = new DataServiceSpotSink( dds[s] );
			int sink_handle = mview.getExprData().addExternalDataSink( ss );
			data_services.put( new Integer(sink_handle), ss );
		    }
		    
		    // 1.3.2 add a ViewerServiceSpotSink for each of the viewer services
		    
		    DynamicViewerService[] dvs = isys.discoverViewerServices(current_spot_selection);
		    
		    for(int s=0; s < dvs.length; s++)
		    {
			ViewerServiceSpotSink ss = new ViewerServiceSpotSink( dvs[s] );
			int sink_handle = mview.getExprData().addExternalDataSink( ss );
			viewer_services.put( new Integer(sink_handle), ss );
		    }
			
		}
	    }
	}
    }


    // ========================================================================================
    //
    //  utils
    //
    // ========================================================================================
    //
    //  applies the filter(s) to a collection of spot ids
    //
    // ========================================================================================

    

    // ========================================================================================
    //
    //  utils
    //
    // ========================================================================================
    //
    //  int[] getDifference( int[] a, int[] b )  returns all elements of 'a' which are not in 'b'
    //
    // ========================================================================================

    public int[] getDifference( final int[] a, final int[] b )
    {
	if((b == null) || (b.length == 0))   // nothing in b, result is all of a
	    return a;

	if((a == null) || (a.length == 0))   // nothing in a, result is nothing
	    return null; 

	final Isys isys = Isys.getInstance();
	
	// convert b to a hashtable
	final int bl =  b.length;
	final HashSet bhs = new HashSet();
	for(int bi=0; bi < bl; bi++)
	    bhs.add(new Integer(b[bi]));
	
	// and search a
	Vector hits = new Vector();
	final int al =  a.length;
	for(int ai=0; ai < al; ai++)
	{
	    Integer val = new Integer(a[ai]);
	    if(!bhs.contains(val))
		hits.addElement(val);
	}
	
	final int n_hits = hits.size();
	int[] hits_a = new int[n_hits];
	for(int hi=0; hi < n_hits; hi++)
	    hits_a[hi] = ((Integer)hits.elementAt(hi)).intValue();
	
	return hits_a;
    }

    // ========================================================================================
    //
    //  call service
    //
    // ========================================================================================

    public Client invokeViewerService( DynamicViewerService dvs, IsysObjectCollection ioc )
    {
	dvs.setSyncPartner( this );
	dvs.setViewableData( ioc );
	try
	{
	    return dvs.execute();
	}
	catch(java.lang.reflect.InvocationTargetException ite)
	{
	}
	return null;
    }

    public void invokeDataService( DynamicDataService dds, IsysObjectCollection ioc )
    {
	dds.setData(ioc);
	try
	{
	    
	    IsysObjectCollection result = dds.execute();
	    data_grabber.grabData( result, true );
	}
	catch(java.lang.reflect.InvocationTargetException ite)
	{
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
	if(do_debug)
	    System.err.println("maxdViewISYSClient: " + msg);
	// mview.infoMessage("maxdViewISYSClient: " + msg);
    }

    // ========================================================================================
    //
    //  utils
    //
    // ========================================================================================
    //
    //  dump objects and attrs
    //
    // ========================================================================================

    public void dumpObjects(String name, IsysObjectCollection ioc)
    {
	final int n_isys_attr_types = 8;
	final Class[] isys_classes = new Class[n_isys_attr_types];
	
	isys_classes[0] = GeneSymbol.class;
	isys_classes[1] = ORFName.class;
	isys_classes[2] = ECNumber.class;
	isys_classes[3] = SequenceText.class;
	isys_classes[4] = Description.class;
	isys_classes[5] = GeneName.class;
	isys_classes[6] = ProbeName.class;
	isys_classes[7] = SpotName.class;
	
	System.out.println("<<<< " + name + " with " + ioc.size() + " objects:");

	java.util.Iterator iter = ioc.iterator();

	int cnt = 0;
	while (iter.hasNext() )
	{
	    IsysObject io = (IsysObject) iter.next();
	    
	    System.out.print("  " + (++cnt));

	    for(int n=0; n < n_isys_attr_types; n++)
	    {
		java.util.Collection coll2 = io.getAttribute( isys_classes[n] );

		if(coll2.size() > 0)
		{
		    java.util.Iterator iter2 = coll2.iterator();
		    
		    while (iter2.hasNext() )
		    {
			IsysAttribute ioa = (IsysAttribute) iter2.next();
			
			String label = null;
			
			switch(n)
			{
			case 0:
			    label = ((GeneSymbol)ioa).getGeneSymbol();
			    break;
			case 1:
			    label = ((ORFName)ioa).getORFName();
			    break;
			case 2:
			    label = ((ECNumber)ioa).getECNumber();
			    break;
			case 3:
			    label = ((SequenceText)ioa).getSequenceText();
			    break;
			case 4:
			    label = ((Description)ioa).getDescription();
			    break;
			case 5:
			    label = ((GeneName)ioa).getGeneName();
			    break;
			case 6:
			    label = ((ProbeName)ioa).getProbeName();
			    break;
			case 7:
			    label = ((SpotName)ioa).getSpotName();
			    break;
			}

			if(label != null)
			{
			    switch(n)
			    {
			    case 0:
				System.out.print(" GS:" + label);
				break;
			    case 1:
				System.out.print(" ON:" + label);
				break;
			    case 2:
				System.out.print(" EC:" + label);
				break;
			    case 3:
				System.out.print(" ST:" + label);
				break;
			    case 4:
				System.out.print(" DE:" + label);
				break;
			    case 5:
				System.out.print(" GN:" + label);
				break;
			    case 6:
				System.out.print(" PN:" + label);
				break;
			    case 7:
				System.out.print(" SN:" + label);
				break;
			    }
			}
		    }
		}
	    }

	    System.out.println();
	}
	System.out.println(name + ">>>>");
    }

    // ========================================================================================
    //
    //  configuration panel 
    //
    // ========================================================================================
    //
    //  openOptionsPanel()
    //
    // ========================================================================================

    private JFrame frame = null;

    private void openOptionsPanel()
    {
	if(frame != null)
	{
	    frame.setVisible(true);
	    return;
	}
	
	frame = new JFrame("ISYS Options");
	
	frame.addWindowListener(new WindowAdapter() 
	    {
		public void windowClosing(WindowEvent e)
		{
		    frame = null;
		}
	    });

	JPanel options = new JPanel();
	// options.setPreferredSize(new Dimension(350, 300));
	options.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	
	GridBagLayout gridbag = new GridBagLayout();
	options.setLayout(gridbag);

	GridBagConstraints c = null;
	Color title_colour =  new JLabel().getForeground().brighter();	    
	Color header_colour = new JLabel().getForeground().darker();	    
	
	int line = 0;

	// // // // // // // // // // // // // // // // // // 

	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout wrapbag = new GridBagLayout();
	    wrapper.setLayout(wrapbag);
	    TitledBorder title = BorderFactory.createTitledBorder(" Packing ");
	    wrapper.setBorder(title);
	    
	    // // // // // // //

	    JButton jb = new JButton("Edit mapping");
	    jb.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    wrapbag.setConstraints(jb, c);
	    wrapper.add(jb);

	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			data_packer.cacheFlush();
			data_packer.askUserForMapping("Data Packer : Edit mapping", false);
		    }
		});
	    
	    // // // // // // //
	    
	    JCheckBox jchkb = new JCheckBox("Enable caching");
	    jchkb.setSelected(allow_packaging_cache);
	    jchkb.addActionListener(new ActionListener()
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			JCheckBox j = (JCheckBox) e.getSource();
			allow_packaging_cache = j.isSelected();
		    }
		});
	    jchkb.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    wrapbag.setConstraints(jchkb, c);
	    wrapper.add(jchkb);

	    // // // // // // //

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    c.weightx = 9.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(wrapper, c);
	    options.add(wrapper);
	}

	// // // // // // // // // // // // // // // // // // 

	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout wrapbag = new GridBagLayout();
	    wrapper.setLayout(wrapbag);
	    TitledBorder title = BorderFactory.createTitledBorder(" Matching ");
	    wrapper.setBorder(title);

	    // // // // // // //
	    
	    JButton jb = new JButton("Edit mapping");
	    jb.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    wrapbag.setConstraints(jb, c);
	    wrapper.add(jb);
    
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			data_grabber.displayMapDialog( null, "Data Matcher : Edit mapping", false, false, false );
		    }
		});

	    // // // // // // //
	
	    JCheckBox jchkb = new JCheckBox("Allow capture");
	    
	    jchkb.setSelected(allow_data_capture);
	    jchkb.addActionListener(new ActionListener()
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			JCheckBox j = (JCheckBox) e.getSource();
			allow_data_capture = j.isSelected();
		    }
		});
	    jchkb.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    wrapbag.setConstraints(jchkb, c);
	    wrapper.add(jchkb);
 
	    // // // // // // //

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    c.weightx = 9.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(wrapper, c);
	    options.add(wrapper);
	}	
	
	// // // // // // // // // // // // // // // // // // 

	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout wrapbag = new GridBagLayout();
	    wrapper.setLayout(wrapbag);
	    TitledBorder title = BorderFactory.createTitledBorder(" Show/Hide ");
	    wrapper.setBorder(title);

	    // // // // // // //
	    
	    JCheckBox jchkb = new JCheckBox("Enable send");
	    
	    jchkb.setSelected(send_hide_show_events);
	    jchkb.addActionListener(new ActionListener()
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			JCheckBox j = (JCheckBox) e.getSource();
			send_hide_show_events = j.isSelected();
		    }
		});
	    jchkb.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    wrapbag.setConstraints(jchkb, c);
	    wrapper.add(jchkb);
    
	    jchkb = new JCheckBox("Enable receive");
	    jchkb.setSelected(recv_hide_show_events);
	    jchkb.addActionListener(new ActionListener()
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			JCheckBox j = (JCheckBox) e.getSource();
			recv_hide_show_events = j.isSelected();
		    }
		});
	    jchkb.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    wrapbag.setConstraints(jchkb, c);
	    wrapper.add(jchkb);

	    // // // // // // //

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    c.weightx = 9.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(wrapper, c);
	    options.add(wrapper);
	}	

	// // // // // // // // // // // // // // // // // // 

	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout wrapbag = new GridBagLayout();
	    wrapper.setLayout(wrapbag);
	    TitledBorder title = BorderFactory.createTitledBorder(" Selection ");
	    wrapper.setBorder(title);

	    // // // // // // //
	    
	    JCheckBox jchkb = new JCheckBox("Scroll display");
	    jchkb.setSelected(scroll_to_new_selection);
	    jchkb.addActionListener(new ActionListener()
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			JCheckBox j = (JCheckBox) e.getSource();
			scroll_to_new_selection = j.isSelected();
		    }
		});
	    jchkb.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    wrapbag.setConstraints(jchkb, c);
	    wrapper.add(jchkb);

	    jchkb = new JCheckBox("Lock to drag & drop");
	    jchkb.setSelected(true);
	    jchkb.setEnabled(false);
	    jchkb.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    wrapbag.setConstraints(jchkb, c);
	    wrapper.add(jchkb);
    
	    JButton jb = new JButton("Synchronise ISYS");
	    jb.setEnabled(false);
	    jb.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 2;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    wrapbag.setConstraints(jb, c);
	    wrapper.add(jb);
    
	    jb = new JButton("Sync. maxdView");
	    jb.setEnabled(false);
	    jb.setFont(mview.getSmallFont());
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 3;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    wrapbag.setConstraints(jb, c);
	    wrapper.add(jb);

	    // // // // // // //

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    c.weightx = 9.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(wrapper, c);
	    options.add(wrapper);
	}	

	// // // // // // // // // // // // // // // // // // 

	{
	    JPanel buttons_panel = new JPanel();
	    buttons_panel.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
	
	    GridBagLayout inner_gridbag = new GridBagLayout();
	    buttons_panel.setLayout(inner_gridbag);
	    {   
		
		final JButton jb = new JButton("Close");
		buttons_panel.add(jb);
		
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    frame.setVisible(false);
			    frame = null;
			}
		    });
		
		c = new GridBagConstraints();
		c.weighty = c.weightx = 1.0;
		inner_gridbag.setConstraints(jb, c);
	    }
	    {   
		final JButton jb = new JButton("Help");
		buttons_panel.add(jb);
		
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    mview.getHelpTopic("maxdViewISYS", "#options");
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 1;
		c.weighty = c.weightx = 1.0;
		inner_gridbag.setConstraints(jb, c);
	    }

	    
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    c.weightx = 9.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(buttons_panel, c);
	    options.add(buttons_panel);
	    
	}
	
	// // // // // // // // // // // // // // // // // // 

	frame.getContentPane().add(options);
	frame.pack();
	frame.setVisible(true);

    }

    // ========================================================================================
    //
    // state
    //
    // ========================================================================================

    public maxdView mview;
    public SelectionListener mv_sel_listener;

    public int[] cur_isys_selection = null;

    public int op_sink_handle = -1;

    public boolean do_debug = false;

    public boolean send_hide_show_events = true;
    public boolean recv_hide_show_events = true;

    public boolean allow_data_capture = false;
    public boolean allow_packaging_cache = true;

    public boolean scroll_to_new_selection = true;

    public maxdViewISYSDataPackager data_packer;
    public maxdViewISYSDataGrabber  data_grabber;

}
