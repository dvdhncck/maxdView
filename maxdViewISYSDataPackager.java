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
// ========================================================================================
// ====  m a x d V i e w I S Y S D a t a P a c k a g e r   ================================
// ========================================================================================
// ========================================================================================
//
//  used to assemble maxdView data into an ISYS object collection
//
//  remembers the mapping between calls to packageData( )
//
// ========================================================================================
// ========================================================================================

class maxdViewISYSDataPackager implements ExprData.ExprDataObserver
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

    public maxdViewISYSDataPackager(final maxdViewISYSClient client_, final maxdView mview_)
    {
	client = client_;
	mview = mview_;

	logo = new ImageIcon(mview.getImageDirectory() + "maxd-isys.jpg");

	mv_to_isys_map = null; // new Hashtable();

	mview.getExprData().addObserver(this);
    }

    protected void finalize()
    {
	debug("finalize() : removing data observer..");
	mview.getExprData().removeObserver(this);
    }

    private class SelOpt
    {
	String name;
	int name_type;    // 0==gene, 1==probe, 2==spot
	int attr_id;      // -1==name, >=0 == tag attr id
	boolean include;
	int isys_class;

	public String toString()
	{
	    return "'" + name + "':" + name_type + ":" +attr_id + ":" + include + ":" + isys_class;

	}
    }


    public IsysObjectCollection packageData( final int[] spot_ids  )
    {
	// if no data has been packaged before or the TagAttrs have
	// changed since the last pack, then display the mapping
	// dialog box before packaging the data

	if(( tagAttrsHaveChanged() ) || (mv_to_isys_map == null) || (mv_to_isys_map.size() == 0) )
	{
	    cacheFlush();
	    if(askUserForMapping(  ) == false)
		return null;
	}
	return doPackage( spot_ids, false );
    }

    public IsysObjectCollection packageData( final int[] spot_ids, boolean ask_user )
    {
	if(ask_user)
	{
	    cacheFlush();
	    if(askUserForMapping(  ))
		return doPackage( spot_ids, false );
	    else
		return null;
	}
	else
	{
	    // the mapping dialog might have to be shown anyhow...
	    return packageData( spot_ids );
	}
    }

    public boolean askUserForMapping()
    {
        return askUserForMapping("Package which data?", true);
    }

    public boolean askUserForMapping(final String title, final boolean modal)
    {
	if(mv_to_isys_map == null)
	    mv_to_isys_map = new Hashtable();
	
	final boolean[] result = new boolean[1];

	edata = mview.getExprData();
	
	frame = new JDialog();

	frame.setModal( modal );

	frame.setTitle(title);

	options = new JPanel();
	options.setPreferredSize(new Dimension(350, 300));
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
	gridbag.setConstraints(button, c);
	options.add(button);

	JLabel label = new JLabel(title);
	c  = new GridBagConstraints();
	label.setForeground(title_colour);
	c.gridy = 1;
	c.weighty = 1.0;
	c.gridwidth = 3;
	gridbag.setConstraints(label, c);
	options.add(label);

	optwrap = new JPanel();
	optbag = new GridBagLayout();
	optwrap.setLayout(optbag);

	label = new JLabel("Include");
	c  = new GridBagConstraints();
	label.setForeground(header_colour);
	c.gridy = 0;
	c.weighty = 1.0;

	optbag.setConstraints(label, c);
	optwrap.add(label);

	label = new JLabel("Name / Attr");
	c  = new GridBagConstraints();
	label.setForeground(header_colour);
	c.gridx = 1;
	c.gridy = 0;
	c.weighty = 1.0;
	optbag.setConstraints(label, c);
	optwrap.add(label);
	
	label = new JLabel("ISYS Attr");
	c  = new GridBagConstraints();
	label.setForeground(header_colour);
	c.gridx = 2;
	c.gridy = 0;
	c.weighty = 1.0;
	optbag.setConstraints(label, c);
	optwrap.add(label);


	int line = 1;
	int id = 0;

	addOptionFor( 0, -1, "Gene name(s)", line++ );

	ExprData.TagAttrs gta = edata.getGeneTagAttrs();
	
	for(int a=0; a < gta.getNumAttrs(); a++)
	{
	    String an = gta.getAttrName(a);
	    addOptionFor( 0, a, an, line++ );
	}

	addOptionFor( 1, -1, "Probe name", line++ );

	ExprData.TagAttrs pta = edata.getProbeTagAttrs();
	
	for(int a=0; a < pta.getNumAttrs(); a++)
	{
	    String an = pta.getAttrName(a);
	    addOptionFor( 1, a, an, line++ );
	}
	
	addOptionFor( 2, -1, "Spot name", line++ );

	ExprData.TagAttrs sta = edata.getSpotTagAttrs();
	
	for(int a=0; a < sta.getNumAttrs(); a++)
	{
	    String an = sta.getAttrName(a);
	    addOptionFor( 2, a, an, line++ );
	}

	JScrollPane jsp = new JScrollPane(optwrap);
	int n_vis_opts = line > 7 ? 6 : (line-1);
	jsp.setPreferredSize( new Dimension( 350, n_vis_opts * 30 ));
	jsp.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	c  = new GridBagConstraints();
	c.gridy = 3;
	c.weighty = 5.0;
	c.weightx = 9.0;
	c.gridwidth = 3;
	c.fill = GridBagConstraints.BOTH;
	gridbag.setConstraints(jsp, c);
	options.add(jsp);

	// - - - - - - - - - - - - 

	JPanel butwrap = new JPanel();
	butwrap.setBorder(BorderFactory.createEmptyBorder(8,5,0,5));
	JButton jb = null;

	if(modal)
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
		    mview.getHelpTopic("maxdViewISYS", "#package");
		}
	    });
	c  = new GridBagConstraints();
	//c.gridx = 1;
	//c.gridy = line;
	//gridbag.setConstraints(jb, c);
	butwrap.add(jb);

	jb = new JButton("OK");
	jb.addActionListener(new ActionListener() 
	    { 
		public void actionPerformed(ActionEvent e) 
		{
		    client.updateServiceList();
		    saveSize();
		    result[0] = true;
		    frame.setVisible(false);
		}
	    });
	c  = new GridBagConstraints();
	//c.gridx = 1;
	//c.gridy = line;
	//gridbag.setConstraints(jb, c);
	butwrap.add(jb);
	
	c  = new GridBagConstraints();
	c.gridy = 4;
	c.weighty = 1.0;
	c.gridwidth = 3;
	gridbag.setConstraints(butwrap, c);
	options.add(butwrap);

	final int iw = mview.getIntProperty("maxdViewISYSDataPackager.panel_width", 350);
	final int ih = mview.getIntProperty("maxdViewISYSDataPackager.panel_height", 300);
	options.setPreferredSize(new Dimension(iw, ih));

	frame.getContentPane().add(options);

	frame.pack();
	mview.locateWindowAtCenter(frame);
	frame.setVisible(true);

	return result[0];
    }

    private SelOpt getOption( int name_t, int attr, String name )
    {
	// get the existing options for this NameAttr (if any)
	boolean found_match = false;
	
	SelOpt so = (SelOpt) mv_to_isys_map.get( name );

	//debug("seeking: '" + name + "':" + name_t + ":" +attr);

	if(so != null)
	{
	    //debug("   found name :: " + so);
	    
	    if( (so.name_type == name_t) && (so.attr_id == attr) && (so.name.equals(name)) )
	    {
		found_match = true;
		//debug("found existing: " + so);
	    }
	}

	if(!found_match)
	{
	    so = new SelOpt();
	    
	    so.name = name;
	    so.name_type = name_t;
	    so.attr_id = attr;
	    so.isys_class = bestGuessFor(name);

	    debug("getOption(): option '" + so.name + "' for '" + so.isys_class + "' saved");

	    mv_to_isys_map.put( name, so );
	}
	
	return so;
    }

    private void saveSize()
    {
	final int iw = options.getWidth();
	final int ih = options.getHeight();
	
	mview.putIntProperty("maxdViewISYSDataPackager.panel_width",  iw);
	mview.putIntProperty("maxdViewISYSDataPackager.panel_height", ih);
    }

    private int bestGuessFor( String name )
    {
	String   name_c = canonical(name);
	
	int[] score = new int[isys_attrs_names.length];
	int max_s = -1;
	int max_i = 0;

	for(int o=0; o < isys_attrs_names.length; o++)
	{
	    String options_c = canonical( isys_attrs_names[o] );

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
	}

	// debug("bestGuessFor(): '" + name + "' -> '" + isys_attrs_names[max_i] + "'");

	return max_i;
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

    private void addOptionFor( int name_t, int attr, String name, int pos )
    {
	final SelOpt so = getOption( name_t, attr, name );

	final JCheckBox jchb = new JCheckBox();
	final JComboBox jcb = new JComboBox( isys_attrs_names );
	

	jchb.setSelected(so.include);

	jchb.addActionListener(new ActionListener() 
	    { 
		public void actionPerformed(ActionEvent e) 
		{
		    so.include = jchb.isSelected();
		    jcb.setEnabled(so.include);
		    //debug(so);
		}
	    });

	GridBagConstraints c  = new GridBagConstraints();
	c.weightx = 1.0;
	c.gridy = pos;
	//c.anchor = GridBagConstraints.WEST;
	optbag.setConstraints(jchb, c);
	optwrap.add(jchb);


	JLabel label = new JLabel( (attr == -1) ? name : ("  " + name) );
	c  = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = pos;
	c.weightx = 1.0;
	// c.anchor = GridBagConstraints.WEST;
	optbag.setConstraints(label, c);
	optwrap.add(label);


	jcb.setSelectedIndex(so.isys_class);

	jcb.addActionListener(new ActionListener() 
	    { 
		public void actionPerformed(ActionEvent e) 
		{
		    so.isys_class = jcb.getSelectedIndex();

		    //debug(so);
		}
	    });

	jcb.setEnabled(so.include);

	c  = new GridBagConstraints();
	c.gridx = 2;
	c.gridy = pos;
	c.weightx = 1.0;
	// c.anchor = GridBagConstraints.EAST;
	optbag.setConstraints(jcb, c);
	optwrap.add(jcb);
    }


    // used by the DataGrabber to make things easier for the user by automatically
    // figuring out the reverse mapping of maxdView -> ISYS attrs
    //
    public String getInverseMapFor( int isys_attr_id )
    {
	// which maxdView name or name attr is mapped to this isys_name ?
	// (there can be more than one, just return the first)

	
	for (Enumeration e = mv_to_isys_map.keys(); e.hasMoreElements(); ) 
	{
	    String nm = (String) e.nextElement();
	    SelOpt so = (SelOpt) mv_to_isys_map.get(nm);

	    if(so.isys_class == isys_attr_id)
	    {
		return nm;
	    }
	}

	// not found....

	return null;
    }

    // ========================================================================================
 
    // ========================================================================================
    //
    //  stuff to work out when the used needs to be asked to specify the mapping
    //   from ISYS attrs to maxdView names and name tags
    //
    // ========================================================================================

    private String name_tags = "";

    private boolean tagAttrsHaveChanged()
    {
	String cur_n_tags = getCurrentTagAttrs();

	//debug("tagAttrs are '" + cur_n_tags + "'");

	if(name_tags == null)
	{
	    name_tags = cur_n_tags;
	    return true;
	}
	
	if(!name_tags.equals(cur_n_tags))
	{
	    name_tags = cur_n_tags;
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


    // ========================================================================================

    private void addISYSAttr( java.util.ArrayList attrs, Vector equiv, int isys_class, String value )
    {
	if(value != null)
	{
	    switch(isys_class)
	    {
	    case 0:     // SpotName
		attrs.add( new maxdViewISYSSpotName( value ) );
		equiv.addElement( SpotName.class );
		break;

	    case 1:     // ProbeName
		attrs.add( new maxdViewISYSProbeName( value ) );
		equiv.addElement( ProbeName.class );
		break;

	    case 2:     // GeneName
		attrs.add( new maxdViewISYSGeneName( value ) );
		equiv.addElement( GeneName.class );
		break;

	    case 3:     // GeneSymbol
		attrs.add( new maxdViewISYSGeneSymbol( value ) );
		equiv.addElement( GeneSymbol.class );
		break;

	    case 4:     // ORFName
		attrs.add( new maxdViewISYSORFName( value ) );
		equiv.addElement( ORFName.class );
		break;

	    case 5:     // ECNumber
		attrs.add( new maxdViewISYSECNumber( value ) );
		equiv.addElement( ECNumber.class );
		break;

	    case 6:     // SequenceText
		attrs.add( new maxdViewISYSSequenceText( value ) );
		equiv.addElement( SequenceText.class );
		break;

	    case 7:     // Description
		attrs.add( new maxdViewISYSDescription( value ) );
		equiv.addElement( Description.class );
		break;


	    }

	    //debug( "addISYSAttr(): class=" + isys_class + " value='" + value + "'");
	}
    }

    private IsysObjectCollection doPackage( final int[] spot_ids, boolean allow_all )
    {
	// check each of the names&attrs in turn....
	Vector stuff_to_include = new Vector();

	for (Enumeration e = mv_to_isys_map.keys(); e.hasMoreElements(); ) 
	{
	    String nm = (String) e.nextElement();
	    SelOpt so = (SelOpt) mv_to_isys_map.get(nm);

	    if(allow_all || so.include)
	    {
		// this could be an name/name.attr which has been deleted
		// since the last packing operation...

		// make sure it still exists
		boolean exists = false;

		try
		{
		    if(so.attr_id == -1)
		    {
			// gene name, probe name and spot name always exist
			exists = true;
		    }
		    else
		    {
			if(so.name_type == 0)
			{
			    // gene name(s)
			    
			    if(edata.getGeneTagAttrs().getAttrName(so.attr_id).equals(so.name))
				exists = true;
			}
			if(so.name_type == 1)
			{
			    // gene name(s)
			    
			    if(edata.getProbeTagAttrs().getAttrName(so.attr_id).equals(so.name))
				exists = true;
			}
			if(so.name_type == 2)
			{
			    // gene name(s)
			    
			    if(edata.getSpotTagAttrs().getAttrName(so.attr_id).equals(so.name))
				exists = true;
			}
		    }
		}
		catch(ArrayIndexOutOfBoundsException aioobe)
		{
		}
		catch(NullPointerException aioobe)
		{
		}

		if(exists)
		{
		    // this name/name.attr exists and is included...

		    //System.out.println( "doPackage(): include: " + so.name + 
		    //		" mv=" + so.name_type + ":" + 
		    //		so.attr_id + " isys class=" + so.isys_class );

		    stuff_to_include.addElement(so);
		}

	    }
	}
	
	// now we have a vector of ISYSAttributes to include in each ISYS object, build the
	// ISYS object collection

	if(stuff_to_include.size() == 0)
	    return null;

	// convert to an array for efficiency...
	SelOpt[] stuff_to_include_a = (SelOpt[]) stuff_to_include.toArray( new SelOpt[0] );
	
	final Isys isys = Isys.getInstance();

	// build a collection of maxdViewISYSGeneSymbol (with option ORFName) objects
	java.util.ArrayList isys_objects = new java.util.ArrayList();

	ExprData.TagAttrs gta = edata.getGeneTagAttrs();
	ExprData.TagAttrs pta = edata.getProbeTagAttrs();
	ExprData.TagAttrs sta = edata.getSpotTagAttrs();

	final Class[] class_t = new Class[0];   // used for convertion from vec to array later...

	int c_hits = 0;
	int c_misses = 0;

	final int n_spots = spot_ids.length;
	//
	// should use the existing traversal order....
	//
	/*
	int[] ordered_spot_indices = new int[spot_ids.length];
	for(int s=0; s <  n_spots; s++)
	    ordered_spot_indices[ s ] = edata.getIndexOfSpot( spot_ids[ s ] );
	Arrays.sort( ordered_spot_indices );
	for(int s=0; s <  n_spots; s++)
	    spot_ids[ s ] = edata.getSpotAtIndex( ordered_spot_indices[ s ] );
	*/

	for(int s=0; s <  n_spots; s++)
	{
	    DefaultIsysObject cached_io = (client.allow_packaging_cache) ? cacheLoad( spot_ids[s] ) : null;

	    if(cached_io != null)
	    {
		c_hits ++;
		// debug(" cache HIT on " + spot_ids[s]);
		isys_objects.add(cached_io);
	    }
	    else
	    {
		c_misses ++;

		//put the _attributes_ into a collection

		java.util.ArrayList attrs = new java.util.ArrayList();

		Vector equiv_v = new Vector();
		
		for(int a=0; a < stuff_to_include_a.length; a++)
		{
		    SelOpt so = stuff_to_include_a[a];
		    
		    switch(so.name_type)
		    {
		    case 0: // gene name or name attr
			String[] gnames = edata.getGeneNames(spot_ids[s]);
			if((gnames != null) && (gnames.length > 0))
			{
			    if(so.attr_id == -1)
			    {
				for(int g=0; g < gnames.length; g++)
				    if((gnames[g] != null) && (gnames[g].length() > 0))
					addISYSAttr( attrs, equiv_v, so.isys_class, gnames[g]);
			    }
			    else
			    {
				for(int g=0; g < gnames.length; g++)
				    if((gnames[g] != null) && (gnames[g].length() > 0))
					addISYSAttr( attrs, equiv_v, so.isys_class, gta.getTagAttr(gnames[g], so.attr_id));
			    }
			}
			break;
			
		    case 1: // probe name or name attr
			String pname = edata.getProbeName(spot_ids[s]);
			if((pname != null) && (pname.length() > 0))
			{
			    if(so.attr_id == -1)
				addISYSAttr( attrs, equiv_v, so.isys_class, pname );
			    else
				addISYSAttr( attrs, equiv_v, so.isys_class, pta.getTagAttr(pname, so.attr_id) );
			}
			break;
			
		    case 2: // spot name or name attr
			String sname = edata.getSpotName(spot_ids[s]);
			if((sname != null) && (sname.length() > 0))
			{
			if(so.attr_id == -1)
			    addISYSAttr( attrs, equiv_v, so.isys_class, sname );
			else
			    addISYSAttr( attrs, equiv_v, so.isys_class, sta.getTagAttr(sname, so.attr_id) );
			}
			break;
		    }
		}
		
		// make an object from these attributes
		// 
		Class[] equiv_a = (Class[]) equiv_v.toArray( class_t );
		
		DefaultIsysObject io = new DefaultIsysObject( attrs, equiv_a );
		if(client.allow_packaging_cache)
		    cacheSave( spot_ids[s], io );
		isys_objects.add( io );
	    }

	    //debug( "doPackage()   sid=" + spot_ids[s] + " na=" + attrs.size() );
	}
	
	debug("doPackage() : cache: " + c_hits + " hits, " + c_misses + " misses");

	// debug( "doPackage() there are " + isys_objects.size() + " objects");

//	return new DefaultIsysObjectCollection2( isys_objects );
	// DJH:: changed from DefaultIsysObjectCollection2 for DefaultIsysObjectCollection

	return new DefaultIsysObjectCollection( isys_objects );

    }

    // ========================================================================================
    //
    //   builds collection of object(s) which have all currently mapped ISYS Attribute
    //
    //  !!disabled!!
    //    this is done for dynamic discovery to avoid the over head of building the
    //    entire data collection 
    //
    //    if count==1 then the collection contains a single object, otherwise it contains 2 objects
    //     (to distinguish between services that operate or either "1" or "more than 1" objects)
    //  !!disabled!!
    //
    // ========================================================================================

    public IsysObjectCollection getPossibleISYSAttrs(int[] spot_ids)
    {
	if(( tagAttrsHaveChanged() ) || (mv_to_isys_map == null) || (mv_to_isys_map.size() == 0) )
	{
	    cacheFlush();
	    if(askUserForMapping() == false)
		return null;
	}

	// package all possible attrs so the discovery finds every relevant service
	// irrespective of which mappings are currently 'include'd
	return doPackage( spot_ids, true );

	/*
	// ======

	// final int ns = mview.getExprData().getNumSpots();

	com.sun.java.util.collections.ArrayList attrs = new com.sun.java.util.collections.ArrayList();
	Vector equiv_v = new Vector();
	
	for (Enumeration e = mv_to_isys_map.keys(); e.hasMoreElements(); ) 
	{
	    String nm = (String) e.nextElement();
	    SelOpt so = (SelOpt) mv_to_isys_map.get(nm);

	    // this could be an name/name.attr which has been deleted
	    // since the last packing operation...
	    // ...make sure it still exists 
	    //
	    //   TODO: also make sure it has at least one element in it
	    //
	    
	    try
	    {
		if(so.attr_id == -1)
		{
		    // gene name, probe name and spot name always exist
		    addISYSAttr( attrs, equiv_v, so.isys_class, "dummy" );
		}
		else
		{
		    if(so.name_type == 0)
		    {
			// gene name(s) tag
			
			if(edata.getGeneTagAttrs().getAttrName(so.attr_id).equals(so.name))
			{
			    addISYSAttr( attrs, equiv_v, so.isys_class, "dummy" );
			}
			
		    }
		    if(so.name_type == 1)
		    {
			// probe name tag
			
			if(edata.getProbeTagAttrs().getAttrName(so.attr_id).equals(so.name))
			    addISYSAttr( attrs, equiv_v, so.isys_class, "dummy" );
		    }
		    if(so.name_type == 2)
		    {
			// spot name tag
			
			if(edata.getSpotTagAttrs().getAttrName(so.attr_id).equals(so.name))
			    addISYSAttr( attrs, equiv_v, so.isys_class, "dummy" );
		    }
		}
	    }
	    catch(ArrayIndexOutOfBoundsException aioobe)
	    {
	    }
	    catch(NullPointerException aioobe)
	    {
	    }
	}

	Class[] equiv_a = (Class[]) equiv_v.toArray( new Class[0] );
	DefaultIsysObject dio = new DefaultIsysObject(  attrs, equiv_a );

	java.util.ArrayList isys_objects = new java.util.ArrayList();
	isys_objects.add(dio);

	return new DefaultIsysObjectCollection2( isys_objects );
	*/
    }

    // ========================================================================================
    // ========================================================================================
    //
    //
    //  Object cache
    //    DefaultIsysObjects are cached based on spot ID
    //    the cache is flushed whevener the mapping is changed,
    //    or whenever maxdView names or name tags change
    //    
    // ========================================================================================
    // ========================================================================================

    public void cacheFlush()
    {
	isys_object_cache.clear();
    }

    private void cacheSave( int spot_id, DefaultIsysObject io )
    {
	isys_object_cache.put( new Integer(spot_id), io );
    }
    private DefaultIsysObject cacheLoad( int spot_id )
    {
	return (DefaultIsysObject) isys_object_cache.get( new Integer(spot_id) );
    }

    private Hashtable isys_object_cache = new Hashtable();

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
    // ========================================================================================


    private maxdView mview;
    private ExprData edata;
    private maxdViewISYSClient client;

    private JCheckBox[] jchb_a;
    private JComboBox[] jcb_a;
    
    // private NameTagSelector[] nts_a;

    // private Class[] isys_classes;

    // private boolean[][] ta_sel;

    private Hashtable mv_to_isys_map;

    private JDialog frame;
    private JPanel options;
    private GridBagLayout gridbag;

    private JPanel optwrap;
    private GridBagLayout optbag;

    private ImageIcon logo;

    // ========================================================================================
    //
    //  utils
    //
    // ========================================================================================
    private void debug(String msg)
    {
	if(client.do_debug)
	    System.err.println("miDataPackage: " + msg);
    }


}
