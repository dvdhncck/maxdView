import org.ncgr.isys.system.*;
import org.ncgr.isys.system.DefaultIsysObject;

import java.awt.event.ActionListener;
import java.util.Collection;

import org.ncgr.isys.objectmodel.GeneSymbol;
import org.ncgr.isys.objectmodel.ORFName;
import org.ncgr.isys.objectmodel.ECNumber;
import org.ncgr.isys.objectmodel.SequenceText;
import org.ncgr.isys.objectmodel.Description;
import org.ncgr.isys.objectmodel.ProbeName;
import org.ncgr.isys.objectmodel.GeneName;
import org.ncgr.isys.objectmodel.SpotName;

import org.ncgr.isys.objectmodel.IcAccession;
import org.ncgr.isys.objectmodel.SpAccession;
import org.ncgr.isys.objectmodel.SpName;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

// ========================================================================================
// ====  m a x d V i e w I S Y S D a t a G r a b b e r   ==================================
// ========================================================================================
//
//  processes data received from an ISYS Data or Viewer Service
//
//    two main features:
//
//       - convert an IsysObjectCollection into an array of maxdView spot IDs
//
//       - grab the data from an IsysObjectCollection
//
//
// ========================================================================================

class maxdViewISYSDataGrabber implements ExprData.ExprDataObserver
{
    final int n_isys_attr_types = 8;

    // note: this ordering has changed in 0.9.2 
    //       so that SpotName is the preferred matchig attr

    final String[] isys_attrs_names = 
    {
	"Spot Name",             // best for matching ...
	"Probe Name",
	"Gene Name", 
	"Gene Symbol", 
	"ORF Name",  
	"EC Number", 
	"Sequence Text", 
	"Description",           // ... worst for matching
    };

    final Class[] isys_classes = new Class[] 
    { SpotName.class, 
      ProbeName.class, 
      GeneName.class, 
      GeneSymbol.class, 
      ORFName.class, 
      ECNumber.class, 
      SequenceText.class, 
      Description.class 
    };

    public maxdViewISYSDataGrabber( maxdViewISYSClient client_, maxdView mview_, maxdViewISYSDataPackager packer_)
    {
	data_map = new DataMap();

	mview = mview_;
	client = client_;
	packer = packer_;

	logo = new ImageIcon(mview.getImageDirectory() + "isys-maxd.jpg");

	/*
	isys_classes = new Class[n_isys_attr_types];

	isys_classes[0] = GeneSymbol.class;
	isys_classes[1] = ORFName.class;
	isys_classes[2] = ECNumber.class;
	isys_classes[3] = SequenceText.class;
	isys_classes[4] = Description.class;
	isys_classes[5] = GeneName.class;
	isys_classes[6] = ProbeName.class;
	isys_classes[7] = SpotName.class;
	*/

	mview.getExprData().addObserver(this);
    }

    protected void finalize()
    {
	debug("finalize() : removing data observer..");
	mview.getExprData().removeObserver(this);
    }

    private class DataMap
    {
	// Hashtable isys_to_mv_map;     // maps classes to ?
	
	boolean[] enabled;              // allowed for matching 

	int[] match_with_name_type;     // one for each supported ISYS attr that records...
	int[] match_with_name_attr_id;  // ...which maxdView name or tag attr to match with corresponding isys attr
	
	NameTagSelector[] nts_a;        // one nts for each possible ISYS attr

	String name_tags;             // used for working out when the tags have changed
 	                              // in which case the NameTagSelectors need to be updated
 	                              // (except they will already have changed themselves)

	public DataMap()
	{
	    nts_a = new NameTagSelector[n_isys_attr_types];

	    enabled = new boolean[n_isys_attr_types];
	    
	    //	    match_on_isys_attr      = new int[n_isys_attr_types];

	    match_with_name_type    = new int[n_isys_attr_types];
	    match_with_name_attr_id = new int[n_isys_attr_types];
	}
    }
    
    // ==================================================================================
    //
    //   spot id generator: matches the data in the IsysObjectCollection with 
    //                      maxdView name or name tag data and returns the spot IDs which match
    //
    // ==================================================================================
    //
    //   displays the mapping dialog box whenever:
    //
    //    - an ISYS attribute that hasn't been seen is encountered
    //    - the maxdView name tags have changed since the last grab
    //   
    // ========================================================================================
    
    public int[] getSpotIDs( IsysObjectCollection ioc, boolean ask_user )
    {
	if(ask_user)
	{
	    return askUserThenGetSpotIDs( ioc );
	}
	else
	{
	    return getSpotIDs( ioc );
	}
 
    }

    public int[] getSpotIDs( IsysObjectCollection ioc )
    {
	if( ( tagAttrsHaveChanged() ) || ( !allAttrsMapped(ioc) ) )
	{
	    debug("getSpotIDs(): map or attrs have changed");
	    return askUserThenGetSpotIDs( ioc );
	}
	else
	{
	    return doGetSpotIDs( ioc );
	}
    }

    private int[] askUserThenGetSpotIDs( IsysObjectCollection ioc )
    {
	if(displayMapDialog(ioc, "Match which data?", false, true, true) == false)
	    return null;

	return doGetSpotIDs( ioc );

    }

    // ==================================================================================
    //
    //   data grabber: extracts matching from the data in the IsysObjectCollection
    //                 into name or name tag data
    //
    // ==================================================================================
    //
    //   displays the mapping dialog box whenever:
    //
    //    - an ISYS attribute that hasn't been seen is encountered
    //    - the maxdView name tags have changed since the last grab
    //   
    // ==================================================================================

    public void grabData( IsysObjectCollection ioc, boolean ask_user )
    {
	if(ask_user)
	{
	    askUserThenGrabData( ioc );
	}
	else
	{
	    grabData( ioc );
	}
 
    }

    public void grabData( IsysObjectCollection ioc )
    {
	if( ( tagAttrsHaveChanged() ) || ( !allAttrsMapped(ioc) ) )
	{
	    askUserThenGrabData( ioc );
	}
	else
	{
	    doGrab( ioc );
	}
    }

    public void askUserThenGrabData( IsysObjectCollection ioc )
    {
	if(displayMapDialog(ioc, "Capture which data?", true, false, true) == false)
	    return;

	// doGrab( ioc );  // now called from within displayMapDialog when it is non modal
    }


    // ========================================================================================
    //
    //  stuff to work out when the used needs to be asked to specify the mapping
    //   from ISYS attrs to maxdView names and name tags
    //
    // ========================================================================================


    private boolean tagAttrsHaveChanged()
    {
	String cur_n_tags = getCurrentTagAttrs();

	//debug("tagAttrs are '" + cur_n_tags + "'");

	if(data_map.name_tags == null)
	{
	    data_map.name_tags = cur_n_tags;
	    return true;
	}
	
	if(!data_map.name_tags.equals(cur_n_tags))
	{
	    data_map.name_tags = cur_n_tags;
	    return true;
	}
	
	//debug("tagAttrsHaveChanged not changed");

	return false;
    }

    private String getCurrentTagAttrs()
    {
	ExprData edata = mview.getExprData();
	String comp = "";
	
	comp += collapseStringArray( edata.getGeneTagAttrs().getAttrNames() );
	
	comp += collapseStringArray( edata.getProbeTagAttrs().getAttrNames() );
	
	comp += collapseStringArray( edata.getSpotTagAttrs().getAttrNames() );

	return comp;
    }

    private String collapseStringArray(String[] sa)
    {
	if(sa != null)
	{
	    StringBuffer sbuf = new StringBuffer();
	    for(int i=0; i < sa.length; i++)
		sbuf.append(sa[i]);
	    return sbuf.toString();
	}
	else
	    return "";
    }
    //
    // is there a selection for each of the attr types in the collection?
    //
    private boolean allAttrsMapped( IsysObjectCollection ioc )
    {
	// work out what is in the collection....
    
	// -------- debug start

	/*
	debug("checking if all Attrs are Mapped");
	
	Collection coll_debug = ioc.getAttributeInstances(GeneSymbol.class);
	if((coll_debug != null) && (coll_debug.size() > 0) && (data_map.nts_a[0] == null))
	    debug("  GeneSymbol not mapped");

	coll_debug = ioc.getAttributeInstances(ORFName.class);
	if((coll_debug != null)  && (coll_debug.size() > 0) && (data_map.nts_a[1] == null))
	    debug("  ORFName not mapped");

	coll_debug = ioc.getAttributeInstances(ECNumber.class);
	if((coll_debug != null)  && (coll_debug.size() > 0) && (data_map.nts_a[2] == null))
	    debug("  ECNumber not mapped");

	coll_debug = ioc.getAttributeInstances(SequenceText.class);
	if((coll_debug != null)  && (coll_debug.size() > 0) && (data_map.nts_a[3] == null))
	    debug("  SequenceText not mapped");

	coll_debug = ioc.getAttributeInstances(Description.class);
	if((coll_debug != null)  && (coll_debug.size() > 0) && (data_map.nts_a[4] == null))
	    debug("  Description not mapped");

	coll_debug = ioc.getAttributeInstances(GeneName.class);
	if((coll_debug != null)  && (coll_debug.size() > 0) && (data_map.nts_a[5] == null))
	    debug("  GeneName not mapped");

	coll_debug = ioc.getAttributeInstances(ProbeName.class);
	if((coll_debug != null)  && (coll_debug.size() > 0) && (data_map.nts_a[6] == null))
	    debug("  ProbeName not mapped");

	coll_debug = ioc.getAttributeInstances(SpotName.class);
	if((coll_debug != null)  && (coll_debug.size() > 0) && (data_map.nts_a[7] == null))
	    debug("  SpotName not mapped");


	for(int i=0; i <  n_isys_attr_types; i++)
	    System.out.println( "a" + i + ":" + 
				((data_map.nts_a[i] == null) ? "--" : data_map.nts_a[i].getNameTagSelection().getNames() ) );
	*/

	// -------- debug end

	Collection coll = null;

	// CHECK 1:
	//   if any of the supported ISYS attrs are present, then there must be
	//   a mapping specified for that attr....
	//
	for(int a=0; a < n_isys_attr_types; a++)
	{
	    coll = ioc.getAttributeInstances( isys_classes[ a ] );
	    if((coll != null) && (coll.size() > 0) && (data_map.nts_a[ a ] == null))
		return false;
	}

	/*
	  was:

	coll = ioc.getAttributeInstances(SpotName.class);
	if((coll != null) && (coll.size() > 0) && (data_map.nts_a[0] == null))
	    return false;

	coll = ioc.getAttributeInstances(ProbeName.class);
	if((coll != null) && (coll.size() > 0) && (data_map.nts_a[1] == null))
	    return false;

	coll = ioc.getAttributeInstances(GeneName.class);
	if((coll != null) && (coll.size() > 0) && (data_map.nts_a[2] == null))
	    return false;

	coll = ioc.getAttributeInstances(GeneSymbol.class);
	if((coll != null) && (coll.size() > 0) && (data_map.nts_a[3] == null))
	    return false;

	coll = ioc.getAttributeInstances(ORFName.class);
	if((coll != null) && (coll.size() > 0) && (data_map.nts_a[4] == null))
	    return false;

	coll = ioc.getAttributeInstances(ECNumber.class);
	if((coll != null) && (coll.size() > 0) && (data_map.nts_a[5] == null))
	    return false;

	coll = ioc.getAttributeInstances(SequenceText.class);
	if((coll != null) && (coll.size() > 0) && (data_map.nts_a[6] == null))
	    return false;

	coll = ioc.getAttributeInstances(Description.class);
	if((coll != null) && (coll.size() > 0) && (data_map.nts_a[7] == null))
	    return false;
	*/

	// debug("all Attrs are Mapped");

	// CHECK 2:
	//   at least one mapping must be selected for matching
	//
	
	int matchers = 0;
	for(int id=0; id < n_isys_attr_types; id++)
	{
	    if(data_map.enabled[id])
		matchers++;
	}

	if(matchers == 0)
	{
	    //mview.alertMessage("You must select at least one attribute to use for matching");
	    return false;
	}
	
	return true;
    }


    // ==================================================================================
    //
    //   displayMapDialog(): displays a dialog controlling the mapping from ISYS attrs 
    //                       to maxdView name and name tags
    //
    // ==================================================================================

    //
    //  possibly needs to be modal to handle selection events?
    //

    public boolean displayMapDialog( final IsysObjectCollection ioc, 
				     final String msg, 
				     final boolean allow_resend,
				     final boolean modal, 
				     final boolean allow_cancel )
    {
	
	final boolean[] result = { true };

	int n_gene_symbols, n_orf_names, n_ec_numbers, n_seq_texts, n_descs, n_gene_names, n_probe_names, n_spot_names;
	
	int oline = 0;

	if(ioc != null)
	{
	    Collection coll = ioc.getAttributeInstances(GeneSymbol.class);
	    n_gene_symbols = (coll == null) ? 0 : coll.size();
	    
	    coll = ioc.getAttributeInstances(ORFName.class);
	    n_orf_names = (coll == null) ? 0 : coll.size();
	    
	    coll = ioc.getAttributeInstances(ECNumber.class);
	    n_ec_numbers = (coll == null) ? 0 : coll.size();
	    
	    coll = ioc.getAttributeInstances(SequenceText.class);
	    n_seq_texts = (coll == null) ? 0 : coll.size();
	    
	    coll = ioc.getAttributeInstances(Description.class);
	    n_descs = (coll == null) ? 0 : coll.size();
	    
	    coll = ioc.getAttributeInstances(GeneName.class);
	    n_gene_names = (coll == null) ? 0 : coll.size();
	    
	    coll = ioc.getAttributeInstances(ProbeName.class);
	    n_probe_names = (coll == null) ? 0 : coll.size();
	    
	    coll = ioc.getAttributeInstances(SpotName.class);
	    n_spot_names = (coll == null) ? 0 : coll.size();
	}
	else
	{
	    // special case for empty collections: assume everything is present

	    n_gene_symbols = n_orf_names = n_ec_numbers = 
	    n_seq_texts = n_descs = n_gene_names = 
	    n_probe_names = n_spot_names = 1;
	}

	frame = new JDialog();
	frame.setTitle(msg);

	if(modal)
	    frame.setModal(true);
	
	options = new JPanel();
	// options.setPreferredSize(new Dimension(350, 300));
	options.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

	gridbag = new GridBagLayout();
	options.setLayout(gridbag);

	Color title_colour =  new JLabel().getForeground().brighter();	    
	Color header_colour = new JLabel().getForeground().darker();	    

	JButton button = new JButton(logo);
	button.setMargin(new Insets(0,0,0,0));
	GridBagConstraints c  = new GridBagConstraints();
	c.weighty = 1.0;
	c.gridwidth = 3;
	c.gridy = oline++;
	gridbag.setConstraints(button, c);
	options.add(button);

	// = = = = = = = = = = = = = = = = = = = = 

	JButton jb = null;
	JLabel label = null;

	if(allow_resend)
	{
	    if(ioc != null)
	    {
		label = new JLabel("Send data back to ISYS");
		c  = new GridBagConstraints();
		label.setForeground(title_colour);
		c.gridy = oline++;
		c.weighty = 1.0;
		c.gridwidth = 3;
		gridbag.setConstraints(label, c);
		options.add(label);
		
		JPanel sendwrap = new JPanel();
		GridBagLayout sendbag = new GridBagLayout();
		sendwrap.setLayout(sendbag);
		

		final Isys isys = Isys.getInstance();
		final DynamicDataService[] dds_a   = isys.discoverDataServices(ioc);
		final DynamicViewerService[] dvs_a = isys.discoverViewerServices(ioc);

		final int first_viewer = dds_a.length;

		// -- - -- - -- - -- - -- - -- - -- - -- - -- - -- - 

		int n_sers =  dds_a.length +  dvs_a.length;
		
		String[] service_list = new String[ n_sers ];
	    		
		int p = 0;
		for(int s=0; s < dds_a.length; s++)
		    service_list[p++] = dds_a[s].getDisplayName();
		for(int s=0; s < dvs_a.length; s++)
		    service_list[p++] = dvs_a[s].getDisplayName();


		final JComboBox jcb = new JComboBox( service_list );
		c  = new GridBagConstraints();
		c.weightx = 1.0;
		sendbag.setConstraints(jcb, c);
		sendwrap.add(jcb);
		jcb.setSelectedIndex(-1);
		
		// -- - -- - -- - -- - -- - -- - -- - -- - -- - -- - 

		JButton sjb = new JButton("Send");
		sjb.setMargin(new Insets(1,1,1,1));
		sjb.addActionListener(new ActionListener() 
		    { 
			public void actionPerformed(ActionEvent e) 
			{
			    frame.setVisible(false);
			    
			    final int sel = jcb.getSelectedIndex();
			    
			    if(sel < first_viewer)
			    {
				// it's a data service...

				// this will involve a reentrant call to doGrab...
				
				debug( "resend: executing data with " + ioc.size() + " objects"); 

				client.invokeDataService(  dds_a[ sel ], ioc );
			    }
			    else
			    {
				debug( "resend: executing viewer with " + ioc.size() + " objects"); 

				// it's a viewer service...
				client.invokeViewerService(  dvs_a[ sel - first_viewer ], ioc );
			    }
			}
		    });
		c  = new GridBagConstraints();
		c.weightx = 1.0;
		c.gridx = 1;
		sendbag.setConstraints(sjb, c);
		sendwrap.add(sjb);


		// -- - -- - -- - -- - -- - -- - -- - -- - -- - -- - 

		c  = new GridBagConstraints();
		c.gridx = 0;
		c.weightx = 10.0;
		c.gridwidth = 3;
		c.gridy = oline++;
		gridbag.setConstraints(sendwrap, c);
		options.add(sendwrap);
	    }
	}


	label = new JLabel(msg);
	c  = new GridBagConstraints();
	label.setForeground(title_colour);
	c.gridy = oline++;
	c.weighty = 1.0;
	c.anchor = GridBagConstraints.SOUTH;
	c.gridwidth = 3;
	gridbag.setConstraints(label, c);
	options.add(label);

	optwrap = new JPanel();
	optbag = new GridBagLayout();
	optwrap.setLayout(optbag);

	
	label = new JLabel("ISYS Attr");
	c  = new GridBagConstraints();
	label.setForeground(header_colour);
	c.gridx = 0;
	c.gridy = 0;
	c.weightx = 1.0;
	//c.weighty = 1.0;
	optbag.setConstraints(label, c);
	optwrap.add(label);
	
	label = new JLabel("Name / Attr");
	c  = new GridBagConstraints();
	label.setForeground(header_colour);
	c.gridx = 1;
	c.gridy = 0;
	c.weightx = 2.0;
	//c.weighty = 1.0;
	optbag.setConstraints(label, c);
	optwrap.add(label);
	
	label = new JLabel("Match");
	c  = new GridBagConstraints();
	label.setForeground(header_colour);
	c.gridx = 2;
	c.gridy = 0;
	c.weightx = 1.0;
	//c.weighty = 1.0;
	optbag.setConstraints(label, c);
	optwrap.add(label);

	// make sure the 'match' details are synchronised (if tags have changed)
	//
	for(int id=0; id < n_isys_attr_types; id++)
	{
	    if(data_map.nts_a[id] != null)
	    {
		data_map.match_with_name_type[id]    = data_map.nts_a[id].getSelectedNameType();
		data_map.match_with_name_attr_id[id] = data_map.nts_a[id].getSelectedAttrID();
	    }
	}

	// add options for any ISYS attrs found in the data

	ButtonGroup bg = new ButtonGroup();

	int line = 2;

	if(n_spot_names > 0)
	    addOptionFor( 0, "Spot Names", n_spot_names, SpotName.class, line++, bg);
	if(n_probe_names > 0)
	    addOptionFor( 1, "Probe Names", n_probe_names, ProbeName.class, line++, bg);
	if(n_gene_names > 0)
	    addOptionFor( 2, "Gene Names", n_gene_names, GeneName.class, line++, bg);

	if(n_gene_symbols > 0)
	    addOptionFor( 3, "Gene Symbols", n_gene_symbols, GeneSymbol.class, line++, bg);
	if(n_orf_names > 0)
	    addOptionFor( 4, "ORF Names", n_orf_names, ORFName.class, line++, bg);
	if(n_ec_numbers > 0)
	    addOptionFor( 5, "EC Numbers", n_ec_numbers, ECNumber.class, line++, bg);
	if(n_seq_texts > 0)
	    addOptionFor( 6, "Sequence Texts", n_seq_texts, SequenceText.class, line++, bg);
	if(n_descs > 0)
	    addOptionFor( 7, "Descriptions", n_descs, Description.class, line++, bg);

	if(line == 2)
	{
	    // no data or no recognised attrs in the collection
	    label = new JLabel( (ioc.size() > 0) ? "(no recognised ISYS Attributes)" : "(no data in result)");
	    c  = new GridBagConstraints();
	    label.setForeground(header_colour);
	    c.gridwidth = 3;
	    c.gridy = line++;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    optbag.setConstraints(label, c);
	    optwrap.add(label);
	}

	JScrollPane jsp = new JScrollPane(optwrap);

	int n_vis_opts = (line > 8) ? 6 : (line-2);
	jsp.setPreferredSize( new Dimension(380, n_vis_opts * 30 ));
	jsp.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	c  = new GridBagConstraints();
	c.gridy = oline++;
	c.anchor = GridBagConstraints.NORTH;
	c.weightx = 10.0;
	c.weighty = 7.0;
	c.gridwidth = 3;
	c.fill = GridBagConstraints.BOTH;
	gridbag.setConstraints(jsp, c);
	options.add(jsp);

	// = = = = = = = = = = = = = = = = = = = = 

	JPanel butwrap = new JPanel();
	// butwrap.setBorder(BorderFactory.createEmptyBorder(8,5,0,5));


	if(allow_cancel)
	{
	    jb = new JButton("Cancel");
	    jb.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			client.updateServiceList();
			saveSize();
			result[0] = false;
			frame.setVisible(false);
		    }
		});
	    
	    c  = new GridBagConstraints();
	    //c.gridy = line;
	    //gridbag.setConstraints(jb, c);
	    butwrap.add(jb);
	}

	jb = new JButton("Help");
	jb.addActionListener(new ActionListener() 
	    { 
		public void actionPerformed(ActionEvent e) 
		{
		    saveSize();

		    if(modal)
			frame.setVisible(false);
		    
		    if(modal)
			// this is Spot matching
			mview.getHelpTopic("maxdViewISYS", "#match");
		    else
			// this is data grabbing
			mview.getHelpTopic("maxdViewISYS", "#grab");
		}
	    });
	c  = new GridBagConstraints();
	//c.gridx = 1;
	//c.gridy = line;
	//gridbag.setConstraints(jb, c);
	butwrap.add(jb);

	jb = new JButton( allow_resend ? "Capture" : "OK");
	jb.addActionListener(new ActionListener() 
	    { 
		public void actionPerformed(ActionEvent e) 
		{
		    saveSize();

		    client.updateServiceList();

		    if(!modal && (ioc != null))
		    {
			doGrab( ioc );
		    }
		    frame.setVisible(false);
		}
	    });
	c  = new GridBagConstraints();
	//c.gridx = 1;
	//c.gridy = line;
	//gridbag.setConstraints(jb, c);
	butwrap.add(jb);
	
	c  = new GridBagConstraints();
	c.gridy = oline++;
	c.weightx = 10.0;
	c.weighty = 0.5;
	c.gridwidth = 3;
	gridbag.setConstraints(butwrap, c);
	options.add(butwrap);

	final int iw = mview.getIntProperty("maxdViewISYSDataGrabber.panel_width", 350);
	final int ih = mview.getIntProperty("maxdViewISYSDataGrabber.panel_height", 300);
	options.setPreferredSize(new Dimension(iw, ih));

	frame.getContentPane().add(options);
	frame.pack();
	
	mview.locateWindowAtCenter(frame);

	// the dialog is modal ... this code will block until it is closed
	frame.setVisible(true);

	// result will be false if "cancel" was selected

	return result[0];
    }

    private void saveSize()
    {
	final int iw = options.getWidth();
	final int ih = options.getHeight();
	
	mview.putIntProperty("maxdViewISYSDataGrabber.panel_width",  iw);
	mview.putIntProperty("maxdViewISYSDataGrabber.panel_height", ih);
    }

    private void addOptionFor( final int id, String name, int count, 
			       Class cls, int pos, ButtonGroup rb_bg )
    {
	JLabel label = new JLabel(name + "  "); //  + " ( x " + count + " ) ");
	GridBagConstraints c  = new GridBagConstraints();
	c.gridy = pos;
	c.weightx = 1.0;
	//c.anchor = GridBagConstraints.EAST;
	optbag.setConstraints(label, c);
	optwrap.add(label);

	if(data_map.nts_a[id] == null)
	{
	    debug("making new nts for id==" + id);
	    data_map.nts_a[id] = new NameTagSelector(mview, "Ignore");
	    setBestGuessFor( data_map.nts_a[id], id, name );

	    debug("nts selection is " + data_map.nts_a[id].getNameTagSelection().getNames());

	    data_map.enabled[id] = ((id == 3) || (id == 4)) ? false : true;
	}

	/*
	if(data_map.match_with_name_type == -1)
	{
	    data_map.nts_a[id].setUserOptionSelected();
	}
	else
	{
	    System.out.println("setting nts selection to " + 
			       data_map.match_with_name_type + ":" + 
			       data_map.match_with_name_attr_id);

	    data_map.nts_a[id].setSingleSelection( data_map.match_with_name_type, data_map.match_with_name_attr_id );
	}
	*/

	c  = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = pos;
	c.weightx = 2.0;
	c.anchor = GridBagConstraints.WEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	optbag.setConstraints(data_map.nts_a[id], c);
	optwrap.add(data_map.nts_a[id]);

	JCheckBox jrb = new JCheckBox();
	jrb.setSelected(data_map.enabled[id]);
	jrb.addActionListener(new ActionListener() 
	    { 
		public void actionPerformed(ActionEvent e) 
		{
		    data_map.enabled[id]      = ((JCheckBox) e.getSource()).isSelected();
		    
		    data_map.match_with_name_type[id]    = data_map.nts_a[id].getSelectedNameType();
		    data_map.match_with_name_attr_id[id] = data_map.nts_a[id].getSelectedAttrID();

		    if(data_map.enabled[id])
		    {
			//debug("mapping changed by user id=" + id + " ::isys=" + id + 
			//      " mv=" + data_map.match_with_name_type[id] + ":" + data_map.match_with_name_attr_id[id]);
		    }
		    else
		    {
			//debug("mapping for id=" + id + " is disabled");
		    }
		}
	    });

	// rb_bg.add(jrb);

	c  = new GridBagConstraints();
	c.gridx = 2;
	c.weightx = 1.0;
	c.gridy = pos;
	//c.anchor = GridBagConstraints.WEST;
	optbag.setConstraints(jrb, c);
	optwrap.add(jrb);
    }

    private void setBestGuessFor( NameTagSelector ntsel, int id, String name )
    {
	if(packer != null)
	{
	    // ask the maxdViewISYSDataPackager for the inverse mapping

	    String imap = packer.getInverseMapFor( id );

	    
	    if(imap != null)
	    {
		debug("setBestGuessFor(): '" + name + "' --imap--> '" + imap + "'");
		
		ExprData.NameTagSelection nts = ntsel.getNameTagSelection();
		nts.setNames( imap );
		ntsel.setNameTagSelection(nts);
		return;
	    }
	    else
	    {
		debug("setBestGuessFor(): '" + name + "' : imap not found");
	    }
	}

	// inverse mapping not found, make a best guess based on the 
	// text strings of the names

	{
	    String   name_c = canonical(name);
	    String[] options = ntsel.getNameTagSelection().getAllNamesArray();
	    
	    int[] score = new int[options.length];
	    int max_s = 0;
	    int max_i = -1;
	    
	    for(int o=0; o < options.length; o++)
	    {
		String options_c = canonical( options[o] );
		
		if(name_c.equals(options_c))
		    score[o] = 10000;
		if(name_c.startsWith(options_c))
		    score[o] = 100;
		if(name_c.indexOf(options_c) >= 0)
		    score[o] += 10;
		if(options_c.indexOf(name_c) >= 0)
		    score[o] += 10;
		
		if(score[o] > max_s)
		{
		    max_s = score[o];
		    max_i = o;
		}
		
		//debug("setBestGuessFor(): checking : " + name_c + " with " + options_c + " ... score=" + score[o]);
	    }
	    
	    if(max_i >= 0)
	    {
		//debug("setBestGuessFor(): '" + name + "' -> '" + options[max_i] + "'");
		ExprData.NameTagSelection nts = ntsel.getNameTagSelection();
		nts.setNames(options[max_i]);
		ntsel.setNameTagSelection(nts);
	    }
	    else
	    {
		//debug("setBestGuessFor(): '" + name + "' -> IGNORE");
		ntsel.setUserOptionSelected();
	    }
	}
    }

    private String canonical(String src)
    {
	String src_l = src.toLowerCase();
	StringBuffer dest = new StringBuffer();
	for(int c=0; c < src_l.length(); c++)
	{
	    char ch = src_l.charAt(c);
	    if(Character.isLetterOrDigit(ch))
		dest.append(ch);
	}
	return dest.toString();
    }

    // ========================================================================================
    //
    //  doGrab()   does the actual data capture, the mapping is assumed to be correctly set
    //
    // ========================================================================================

    private boolean doGrab( IsysObjectCollection ioc )
    {

	// work out what attribute to match on...

	// pick the first mapping that is 
	//    enabled
	//    present in the collection
	//    present in the ExprData
	//

	Collection coll = null;
	Hashtable tag_to_sids = null;

	int matchers = 0;
	for(int id=0; id < n_isys_attr_types; id++)
	{
	    if(data_map.enabled[id])
		matchers++;
	}

	if(ioc.size() > 0)
	{
	    if(matchers == 0)
	    {
		mview.alertMessage("You must select at least one attribute to use for matching");
		return false;
	    }
	}
	else
	{
	    return false;
	}

	boolean found = false;
	int mapping_attr = -1;

	// store the NameTagSelections
	
	ExprData.NameTagSelection[] ntsel_a = new ExprData.NameTagSelection[n_isys_attr_types];
	for(int n=0; n < n_isys_attr_types; n++)
	{
	    if(data_map.nts_a[n] != null)
		ntsel_a[n] = data_map.nts_a[n].getNameTagSelection();
	}


	// try to find an enabled mapping rule which matches what is in the collection

	while((!found) && (++mapping_attr < n_isys_attr_types))
	{
	    coll = ioc.getAttributeInstances( isys_classes[ mapping_attr ] );

	    /*
	      was:

	    switch(mapping_attr)
	    {
	    case 0:
		coll = ioc.getAttributeInstances(SpotName.class);
		break;
	    case 1:
		coll = ioc.getAttributeInstances(ProbeName.class);
		break;
	    case 2:
		coll = ioc.getAttributeInstances(GeneName.class);
		break;
	    case 3:
		coll = ioc.getAttributeInstances(GeneSymbol.class);
		break;
	    case 4:
		coll = ioc.getAttributeInstances(ORFName.class);
		break;
	    case 5:
		coll = ioc.getAttributeInstances(ECNumber.class);
		break;
	    case 6:
		coll = ioc.getAttributeInstances(SequenceText.class);
		break;
	    case 7:
		coll = ioc.getAttributeInstances(Description.class);
		break;
	    }
	    */

	    
	    if((coll != null) && (coll.size() >= ioc.size()))
	    {
		tag_to_sids = makeNameTagMap( ntsel_a[ mapping_attr ] );

		if(tag_to_sids.size() > 0)
		    found = true;
	    }
	}

	if(mapping_attr < n_isys_attr_types)
	{
	    
	    // foreach object in the collection, check each of the NameTagSelectors in turn....
	    
	    debug("doGrab(): matching on '" + ntsel_a[mapping_attr].getNames() );
	    
	    java.util.Iterator iter = ioc.iterator();

	    while (iter.hasNext() )
	    {
		String[] labels = new String[ n_isys_attr_types ];
		
		IsysObject io = (IsysObject) iter.next();
		
		for(int n=0; n < n_isys_attr_types; n++)
		{
		    // GROSS CODE ALERT!
		    
		    if(data_map.nts_a[n] != null)
		    {
			if(!data_map.nts_a[n].userOptionSelected())
			{
			    
			    //Collection coll = ioc.getAttributeInstances(GeneSymbol.class);
			    
			    if(ntsel_a[n] != null)
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
					    labels[n] = ((SpotName)ioa).getSpotName();
					    break;
					case 1:
					    labels[n] = ((ProbeName)ioa).getProbeName();
					    break;
					case 2:
					    labels[n] = ((GeneName)ioa).getGeneName();
					    break;
					case 3:
					    labels[n] = ((GeneSymbol)ioa).getGeneSymbol();
					    break;
					case 4:
					    labels[n] = ((ORFName)ioa).getORFName();
					    break;
					case 5:
					    labels[n] = ((ECNumber)ioa).getECNumber();
					    break;
					case 6:
					    labels[n] = ((SequenceText)ioa).getSequenceText();
					    break;
					case 7:
					    labels[n] = ((Description)ioa).getDescription();
					    break;

					}
				    }
				}
			    }
			}
		    }
		}
		
		
		// if(labels[data_map.match_on_isys_attr] != null)
		{
		    // debug("matching on '" + ntsel_a[match_attr].getNames() + "' (" + labels[match_attr] + ")");
		    
		    // get the spot id(s) that match this label
		    
		    
		    Vector s_ids = (Vector) tag_to_sids.get( labels[mapping_attr] );
		    
		    if(s_ids != null)
		    {
			for(int s=0; s < s_ids.size(); s++)
			{
			    int s_id = ((Integer)s_ids.elementAt(s)).intValue();
			    
			    for(int n=0; n < n_isys_attr_types; n++)
			    {
				if((n != mapping_attr) && (labels[n] != null))
				{
				// debug("  sid:" + s_id + " " + ntsel_a[n].getNames() + "=" + labels[n]);
				    if(ntsel_a[n].isGeneNamesOrAttr())
				    {
					ntsel_a[n].setGeneNameTag( labels[mapping_attr], labels[n], s_id );
				    }
				    else
				    {
					ntsel_a[n].setNameTag( labels[n], s_id );
				    }
				}
			    }
			}
		    }
		}
	    }
	}

	mview.getExprData().generateDataUpdate(ExprData.NameChanged);

	return true;
    }

    // ========================================================================================
    //
    //  doGetSpotIDs()   does the actual spot ID matching
    //
    // ========================================================================================

    private int[] doGetSpotIDs( IsysObjectCollection ioc )
    {

	
	boolean found = false;
	int mapping_attr = -1;

	// get the correct collection based on the specified match attr

	Collection coll = null;
	Hashtable tag_to_sids = null;

	while((!found) && (++mapping_attr < n_isys_attr_types))
	{
	    coll = ioc.getAttributeInstances( isys_classes[ mapping_attr ] );
	    
	    /*
	      was:
	    switch(mapping_attr)
	    {
	    case 0:
		coll = ioc.getAttributeInstances(SpotName.class);
		break;
	    case 1:
		coll = ioc.getAttributeInstances(ProbeName.class);
		break;
	    case 2:
		coll = ioc.getAttributeInstances(GeneName.class);
		break;
	    case 3:
		coll = ioc.getAttributeInstances(GeneSymbol.class);
		break;
	    case 4:
		coll = ioc.getAttributeInstances(ORFName.class);
		break;
	    case 5:
		coll = ioc.getAttributeInstances(ECNumber.class);
		break;
	    case 6:
		coll = ioc.getAttributeInstances(SequenceText.class);
		break;
	    case 7:
		coll = ioc.getAttributeInstances(Description.class);
		break;
	    }
	    */

	    if((coll != null) && (coll.size() >= ioc.size()))
	    {
		if (data_map.nts_a[mapping_attr] == null)
			continue;
		
		ExprData.NameTagSelection ntsel = data_map.nts_a[mapping_attr].getNameTagSelection();
		tag_to_sids = makeNameTagMap( ntsel );
		
		if((tag_to_sids != null) && (tag_to_sids.size() > 0))
		{
		    debug("doGetSpotIDs(): matching on '" + ntsel.getNames() + "'");
		    
		    found = true;
		}
	    }
	}

	if(mapping_attr < n_isys_attr_types)
	{
	    
	    if((coll == null) || (coll.size() == 0))
	    {
		// nothing to match with
		
		return null;
	    }
	    
	    final ExprData edata = mview.getExprData();
	    
	     // foreach object in the collection, add the matched spot IDs
	    
	    Vector total_sids = new Vector();
	    
	    java.util.Iterator iter = coll.iterator();
	    
	    while (iter.hasNext() )
	    {
		IsysAttribute ia = (IsysAttribute) iter.next();
		
		String label = null;
		
		switch( mapping_attr )
		{
		case 0:
		    label = ((SpotName)ia).getSpotName();
		    break;
		case 1:
		    label = ((ProbeName)ia).getProbeName();
		    break;
		case 2:
		    label = ((GeneName)ia).getGeneName();
		    break;
		case 3:
		    label = ((GeneSymbol)ia).getGeneSymbol();
		    break;
		case 4:
		    label = ((ORFName)ia).getORFName();
		    break;
		case 5:
		    label = ((ECNumber)ia).getECNumber();
		    break;
		case 6:
		    label = ((SequenceText)ia).getSequenceText();
		    break;
		case 7:
		    label = ((Description)ia).getDescription();
		    break;
	    
		}
		
		if(label != null)
		{
		    Vector sids = (Vector) tag_to_sids.get( label );
		    
		    if(sids != null)
		    {
			for(int s=0; s < sids.size(); s++)
			{
			    total_sids.addElement( (Integer)sids.elementAt(s) );
			}
		    }
		}
	    }
	    
	    final int n_results = total_sids.size();
	    
	    if(n_results == 0)
		return null;
	    
	    int[] result = new int[ n_results ];
	    
	    for(int s=0; s < n_results; s++)
	    {
		result[s] = ((Integer)total_sids.elementAt( s )).intValue();
	    }
	    
	    return result;
	}
	else
	{
	    return null;
	}

    }

    // ========================================================================================
 

    private maxdView mview;

    //    private IsysObjectCollection data;

    private JDialog frame;
    private JPanel options;
    private GridBagLayout gridbag;

    private JPanel optwrap;
    private GridBagLayout optbag;

    private ImageIcon logo;

    private DataMap data_map;

    private maxdViewISYSClient client;

    private maxdViewISYSDataPackager packer;

    // private JRadioButton[] jrb_a;



    // ========================================================================================
    // ========================================================================================
    //
    //  utils:
    //
    //    NameTagMap cache
    //
    //      whenever maxdView names or name tags change
    //    
    // ========================================================================================
    // ========================================================================================

    private void cacheFlush()
    {
	name_tag_map_cache.clear();
    }

    private void cacheSave( String name_tag, Hashtable io )
    {
	name_tag_map_cache.put( name_tag, io );
    }
    private Hashtable cacheLoad( String name_tag )
    {
	return (Hashtable) name_tag_map_cache.get( name_tag );
    }

    private Hashtable name_tag_map_cache = new Hashtable();


    // ========================================================================================
    //  needs to be a data observer so that name change events can be monitored
    // ========================================================================================

     // the observer interface of ExprData notifies us whenever something
    // interesting happens to the data as a result of somebody (including us) 
    // manipulating it
    //
    public void dataUpdate(ExprData.DataUpdateEvent due)
    {
	switch(due.event)
	{
	case ExprData.NameChanged:
	case ExprData.NameAttrsChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	    debug("cache:: spots, names or tags have changed, cache flushed");
	    cacheFlush();
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

    // ========================================================================================
    //
    //  utils:
    //
    //    makeNameTagMap( )
    //
    // ========================================================================================

    private Hashtable makeNameTagMap(ExprData.NameTagSelection ntsel)
    {
	String tn = ntsel.getNames();

	if(tn == null)
	    return null;

	Hashtable tag_to_sids = cacheLoad( tn );

	if(tag_to_sids != null)
	{
	    //debug("name tag map '" + tn + "' found in cache");
	}
	else
	{
	    //debug("creating name tag map '" + tn + "'");
	    
	    tag_to_sids = new Hashtable();
	    final int ns = mview.getExprData().getNumSpots();
	    for(int s=0; s < ns; s++)
	    {
		String[] nta = ntsel.getNameTagArray(s);
		
		if(nta != null)
		    for(int n=0; n < nta.length; n++)
		    {
			Vector vec = (Vector) tag_to_sids.get( nta[n] );
			if(vec == null)
			    tag_to_sids.put( nta[n], vec = new Vector() );
			vec.addElement( new Integer( s ));
		    }
	    }

	    cacheSave( tn, tag_to_sids );
	}

	return tag_to_sids;
    }
    

    // ========================================================================================
    //
    //  utils
    //
    // ========================================================================================

    private void debug(String msg)
    {
	if(client.do_debug)
	    System.err.println("miDataGrabber: " + msg);
    }


}
