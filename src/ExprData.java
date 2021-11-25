// package uk.ac.man.bioinf.maxdView

import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.lang.reflect.Array;

import java.rmi.*;
import java.rmi.server.*;

/**
ExprData combines Measurements and Clusters

@author David Hancock
*/

public class ExprData extends UnicastRemoteObject implements RemoteExprDataInterface
{
    public ExprData() throws java.rmi.RemoteException
    {
	n_measurements = n_spots = 0;

	probe_ta = new TagAttrs();
	gene_ta = new TagAttrs();
	spot_ta = new TagAttrs();

	cluster_id_to_cluster = new Hashtable();
    }

   // ---------------- --------------- --------------- ------------- ------------
   
    public int next_free_cluster_id = 0;

    public final int n_glyph_types = 8;

    // ---------------- --------------- --------------- ------------- ------------

    public ExprData getRemoteExprData()
    {
	return this;
    }

    // ---------------- --------------- --------------- ------------- ------------

    public String toString()
    {
	return "ExprData[" + n_measurements + " sets of " + n_spots + " spots, " + getNumClusters() + " clusters]";
    }

    // ---------------- --------------- --------------- ------------- ------------
    //
    // A Cluster is an ordered collections of 0 or more spots, and 0 or more Clusters
    // 
    //
    //
    //
    //  NOTE that the accessor methods of Cluster change the state but do not 
    //  generate update events. This allows you to build a tree of clusters
    //  in peace without updates continually being generated and without the
    //  inefficiency of plugins repeatedly updating themselves.
    //  
    //  To change the state of a Cluster and have an event generated, use the
    //  corresponding methods of ExprData, e.g.  use ExprData.setClusterName() 
    //  instead of Cluster.setName()
    //
    // ---------------- --------------- --------------- ------------- ------------

    public ExprData.ClusterHandle getRootClusterHandle()  throws java.rmi.RemoteException
    {
	return new RemoteCluster(cluster_root);
    }

    // create a new a parent-less cluster (no ClusterUpdate event generated)
    public ExprData.ClusterHandle createClusterHandle( ExprData.ClusterHandle parent, 
						       String name, int name_mode, Vector elems) throws RemoteException
    {
	Cluster cl = new Cluster(name, name_mode, elems);
	//	((Cluster)parent).addCluster(cl);
	
	//System.out.println("attempting to install cluster '" + name + "' which has " + elems.size() + " elements..." );
	
	Cluster pa = getClusterByID( parent.getId() );

	//System.out.println("parent name is " + (getClusterByID(parent.getId())).getName());

	if(parent != null)
	{
	    pa.addCluster(cl);
	    // generateClusterUpdate(ElementsAdded);
	}
	else
	    throw new RemoteException();

	return new RemoteCluster(cl);
    }

    // create a new cluster with the specified parent (no ClusterUpdate event generated)
    public ExprData.ClusterHandle createClusterHandle( String name, int name_mode, Vector elems) throws RemoteException
    {
	//System.out.println("attempting to create handle for new cluster '" + name + "' which has " + elems.size() + " elements..." );
	Cluster cl = new Cluster(name, name_mode, elems);
	return new RemoteCluster(cl);
    }

    // adds a child to the specified parent (generates a ClusterUpdate event)
    public void addClusterHandle(ExprData.ClusterHandle parent, ExprData.ClusterHandle cl) throws RemoteException
    {
	Cluster pa = getClusterByID( parent.getId() );
	Cluster ch = getClusterByID( cl.getId() );
	   
	if((pa != null) && (ch != null))
	{
	    pa.addCluster(ch);
	    generateClusterUpdate(ElementsAdded);
	}
	else
	{
	    if(pa==null)
	    {
		if(parent != null)
		    System.err.println("addClusterHandle(): EXCEPTION: parent handle (id=" + parent.getId() + ") not valid");
		else
		    System.err.println("addClusterHandle(): EXCEPTION: parent handle was null");
	    }
	    
	    if(ch==null)
	    {
		if(cl != null)
		    System.err.println("addClusterHandle(): EXCEPTION: child handle (id=" + cl.getId() + ") not valid");
		else
		    System.err.println("addClusterHandle(): EXCEPTION: child handle was null");
	    }
	    throw new RemoteException();
	}
    }

    public ExprData.Cluster getClusterByID(int id)
    {
	return (ExprData.Cluster) cluster_id_to_cluster.get(new Integer(id));
    }

    // this is a list of which Cluster methods can be called via RMI 
    public interface ClusterHandle extends Remote 
    {
	public int     getId() throws RemoteException;   // used for conversion from ClusterHandle -> Cluster

	public String  getName() throws RemoteException;
	
	public int     getNumElements() throws RemoteException;
	public int[]   getElements() throws RemoteException;
	
	public int     getNumChildren() throws RemoteException;
	public Vector  getChildren() throws RemoteException;

	public boolean getIsSpot() throws RemoteException;

	public Color   getColour()  throws RemoteException;
	public int     getGlyph() throws RemoteException;
	public boolean getShow()  throws RemoteException;

	public int     getElementNameMode() throws RemoteException;
	public void    setElements(int nm, Vector e) throws RemoteException;
    }

    // this is what gets transmitted by RMI
    // (i.e. a handle to the cluster object rather than sending the cluster data itself)

    public class RemoteCluster extends UnicastRemoteObject implements ClusterHandle
    {
	private int cluster_id;

	public RemoteCluster( Cluster cl ) throws RemoteException
	{
	    cluster_id = cl.getId();
	}

	public int     getId() { return cluster_id; }

	public String  getName() { return getCluster().getName(); }
	
	public int     getNumElements() { return getCluster().getNumElements(); }
	public int[]   getElements() { return getCluster().getElements(); }
	
	public int     getNumChildren() { return getCluster().getNumChildren(); }
	public Vector  getChildren() { return getCluster().getChildren(); }
	
	public boolean getIsSpot() { return getCluster().getIsSpot(); }
	
	public Color   getColour() { return getCluster().getColour(); }
	public int     getGlyph() { return getCluster().getGlyph(); }
	public boolean getShow() { return getCluster().getShow(); }
	
	public int     getElementNameMode() { return getCluster().getElementNameMode(); }

	public void    setElements(int nm, Vector e) { getCluster().setElements(nm, e); }
	
	private Cluster getCluster()
	{
	    return  getClusterByID( cluster_id );
	}
	
	// public void    addClusterHandle(ClusterHandle child) throws RemoteException { clust.addCluster(child.getCluster()); }

	// only use this on server side
	// public Cluster getCluster() { return clust; }	
    }

    
    /**
     *
     * 
     *
     */
    public class Cluster
    {
	private int id;
	private Cluster parent;
	private int depth;       // how many ancestors has this node got?
	private String name;

	private boolean is_spot;   // clusters can be for either Spots or Measurements

	private double q1, q2;   // quantities, can be used for any purpose
	                         // such as a measure of 'tightness'

	private boolean show;
	
	private int colour_set;
	private Color colour;
	
	private int glyph_set;
	private int glyph;
	private boolean glyph_cycle;
	
	private Vector element_names;         // this is the original list of names
	private int    element_name_mode;     // what is in the original list of names?

	private int[] elements;

	private Vector children; // the list of clusters which are children of this cluster

	// ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- 
	// ACCESSORS  ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ----
	// ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- 

	public int     getId()                 { return id; }
	public Cluster getParent()             { return parent; }
	public int     getSize()               { return (elements == null) ? 0 : elements.length; }
	public int     getNumElements()        { return (elements == null) ? 0 : elements.length; }
	public int[]   getElements()           { return elements; }
	public String  getName()               { return name; }
	public int     getGlyph()              { return glyph; }
	public int     getGlyphSet()           { return glyph_set; }
	public boolean getGlyphCycle()         { return glyph_cycle; }
	public Color   getColour()             { return colour; }
	public int     getColourSet()          { return colour_set; }
	public boolean getShow()               { return show; }
	public int     getNumChildren()        { return (children == null) ? 0 : children.size(); }
	public Vector  getChildren()           { return children; }
	
	public boolean getIsSpot()            { return is_spot; }

	// this is the 'correct' name for the method
	public void setElementNames(int name_mode, Vector elem_names)
	{
	    setElements(name_mode, elem_names);
	}

	// ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- 
	// C'TORS  -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ----
	// ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- 

	/*
	public Cluster(String n, int i)
	{
	    name = n;
	    id = i;
	    if(id > next_free_cluster_id)
		next_free_cluster_id = id;
		initialise();
	}
	*/

	public Cluster(String n, int name_mode, Vector elems) //throws java.rmi.RemoteException
	{
	    name = n;
	    id = ++next_free_cluster_id;
	    initialise();
	    setElements(name_mode, elems);
	}

	public Cluster(String n) //throws java.rmi.RemoteException
	{
	    name = n;
	    id = ++next_free_cluster_id;
	    initialise();
	}

	public Cluster(String n, int name_mode) //throws java.rmi.RemoteException
	{
	    name = n;
	    id = ++next_free_cluster_id;
	    initialise();
	    setElements(name_mode, null);
	}

	public Cluster(int name_mode, Vector elems) //throws java.rmi.RemoteException
	{
	    name = new String("Cluster_" + (++next_free_cluster_id));
	    id = next_free_cluster_id;
	    initialise();
	    setElements(name_mode, elems);
	}

	public Cluster() //throws java.rmi.RemoteException
	{ 
	    this(SpotIndex, (Vector) null);
	}

	private void initialise()
	{
	     show = true;
	     is_spot = true;

	     colour = Color.white;
	     glyph = 0;
	     glyph_set = 0;
	     glyph_cycle = false;
	     colour_set = 0;

	     parent = null;

	     elements = null;
	     element_names = null;
	     element_name_mode = SpotIndex;

	     children = null;

	     cluster_id_to_cluster.put( new Integer(id), this );
	     
	     chooseSuitableColourAndGlyph(parent);
	}

	/*
	public void addCluster(RemoteClusterInterface child)
	{
	    addCluster((Cluster) child);
	}
	*/

	public void addCluster(ExprData.Cluster child)
	{
	    if(child == null)
	    {
		System.out.println("Cluster.addCluster(): WARNING - null child ignored");
		return;
	    }

	    
	    if(children == null)
	    {
		children = new Vector();
	    }
	    children.addElement(child);
	    child.parent = this;

	    checkIsSpot();

	    // update any genes which are in this new cluster
	    //
	    // DONT UPDATE ANYTHING....
	    //
	    // This should be the ONLY time that clusters are added to other clusters
	    // without the ExprData finding out. During a large cluster creation process,
	    // all nodes should be added before any update events are fired so that
	    // the intermediate states are not seen.
	    // Instead the whole tree should be built and then a single update event
	    // fired to notify all interested parties that the clusters have changed
	    // 
	    // If you want to add a child to a cluster and generate an event right away,
	    // use the addChildToCluster() method of ExprData
	    //
	    //child.recursivelyCallChangedVisibility(true);

	    // System.out.println("-*-*-*-*- new cluster (" + child.name + ") added to " + name + 
	    //     ", now has " + children.size() + " children");
	}
	

	public boolean removeCluster( Cluster chi )
	{
	    if(( children == null ) || (children.size() == 0))
		return false;

	    int  ind = -1;
	    for(int c=0; c < children.size(); c++)
		if(((Cluster) children.elementAt(c)) == chi)
		    ind = c;
	    
	    if(ind >=0)
	    {
		children.removeElementAt(ind);
		if(children.size() == 0)
		    children = null;
		return true;
	    }
	    return false;
	}


	// a Cluster is a Spot cluster if any of it's elements are spots or children are spot clusters....
	//
	private boolean checkIsSpot()
	{
	    is_spot = false;

	    if((elements != null) && (elements.length > 0))
		if((element_name_mode != MeasurementName) && (element_name_mode != MeasurementIndex))
		{
		    //System.out.println("-*-*-" + name + " has spot elem(s)");
		    is_spot = true;
		}

	    if((children != null) && (children.size() > 0))
	    {
		for(int c=0; c < children.size(); c++)
		{
		    if((((Cluster) children.elementAt(c)).checkIsSpot()))
		    {
			//System.out.println("-*-*-" + name + " has spot child");
			is_spot = true;
		    }
		}
	    }

	    //if(!is_spot)
	    //  System.out.println("-*-*-" + name + " " + (is_spot ? "is spot" : "is meas"));

	    return is_spot;
	}
	
	// ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- 
	// -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ----
	// ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- 

	// returns the Spot index of the first element in this cluster...
	//
	public int getFirstSpot()
	{
	    if(!is_spot)
		return -1;

	    if((elements == null) || (elements.length < 1))
	    {
		// has no elements itself, what about the children?

		if((children == null) || (children.size() < 1))
		    // hs no children either
		    return -1;
		
		int nc = 0;
		while(nc < children.size())
		{
		    int fs = ((Cluster) children.elementAt(nc)).getFirstSpot();
		    // did this child has a first spot?
		    if(fs >= 0)
			return fs;
		    // check next child
		    nc++;
		}
		// no children has a first spot
		return -1;
	    }

	    // return the first element of this cluster
	    return elements[0];
	}

	// ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- 
	// -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ----
	// ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- 


	private boolean elementNameKnown(String nm)
	{
	    if(element_names == null)
		return false;
	    for(int n=0;n < element_names.size(); n++)
		if(((String)element_names.elementAt(n)).equals(nm))
		    return true;
	    return false;
	}

	// set the spot or measurement indexes directly
	// 
	//  WARNING: THIS IS FOR INTERNAL USE ONLY.
	//           USERS SHOULDN'T BE ABLE TO DO THIS........
	// 
	private void setElements(int[] e)        
	{
	    if(e == null)
	    {
		elements = null;
		return;
	    }

	    if(e.length == 0)
	    {
		elements = null;
		return;
	    }

	    final int n_elems = e.length;
	    int n_valid_elems = 0;

	    for(int i=0; i < n_elems; i++)
	    {
		if(is_spot)
		{
		    if((e[i] >= 0) && (e[i] < getNumSpots()))
			n_valid_elems++;
		}
		else
		{
		    if((e[i] >= 0) && (e[i] < getNumMeasurements()))
			n_valid_elems++;
		}
	    }
	    elements = new int[n_valid_elems];
	    
	    int newe = 0;
	    for(int i=0; i < n_elems; i++)
	    {
		if(is_spot)
		{
		    if((e[i] >= 0) && (e[i] < getNumSpots()))
			elements[newe++] = e[i];
		}
		else
		{
		    if((e[i] >= 0) && (e[i] < getNumMeasurements()))
			elements[newe++] = e[i];
		}
	    }
	}
	
	public void setElements(int nm, Vector e)        
	{
	    // get the spot or measurement indexes of the given list of names
	    // 

	    element_names = e;
	    element_name_mode = nm;

	    if(element_names == null)
	    {
		elements = null;
		return;
	    }

	    //
	    // work out how many valid names are in the vector
	    //
	    int n_valid_elems = 0;
	    final int n_elems =  (element_names == null) ? 0 : element_names.size();

	    Hashtable ht = null;

	    switch(element_name_mode)
	    {
	    case SpotIndex:
		is_spot = true;
		for(int s=0; s < n_elems; s++)
		{
		    try
		    {
			int si = ((Integer)element_names.elementAt(s)).intValue();
			if(si >= 0)
			    n_valid_elems++;
		    }
		    catch(NumberFormatException nfe)
		    {
		    }
		}
		break;

	    case SpotName:
		is_spot = true;
		for(int s=0; s < n_elems; s++)
		{
		    int si = getIndexBySpotName((String)element_names.elementAt(s));
		    if(si >= 0)
			n_valid_elems++;
		}
		break;
		
	    case ProbeName:
		is_spot = true;
		ht = getProbeNameHashtable();    // should cache this!
		for(int s=0; s < n_elems; s++)
		{
		    String pnm = (String) element_names.elementAt(s);
		    Vector indices = (Vector) ht.get(pnm);
		    if(indices != null)
			n_valid_elems += indices.size();
		}
		break;

	    case GeneName:
		is_spot = true;
		ht = getGeneNameHashtable();    // should cache this!
		for(int s=0; s < n_elems; s++)
		{
		    String gnm = (String) element_names.elementAt(s);
		    Vector indices = (Vector) ht.get(gnm);
		    if(indices != null)
			n_valid_elems += indices.size();
		}
		break;

	    case MeasurementName:
		is_spot = false;
		for(int m=0; m < n_elems; m++)
		{
		    String mnm = (String) element_names.elementAt(m); 
		    
		    int m_id = getMeasurementFromName(mnm);
		    if(m_id >= 0)
		    {
		        n_valid_elems++;
		    }
		}
		break;

	    case MeasurementIndex:
		is_spot = false;
		final int nms = getNumMeasurements();
		for(int m=0; m < n_elems; m++)
		{
		    int m_id = ((Integer)element_names.elementAt(m)).intValue();
		    
		    if((m_id >= 0) && (m_id < nms))
		    {
		        n_valid_elems++;
		    }
		}
		break;

	    default:
		reportAccessError("setElements", "Element 'name_mode' must be SpotIndex, SpotName or ProbeName, MeasurementName or MeasurementIndex");
		element_name_mode = SpotIndex;
		element_names = null;
		elements = null;
		return;
	    }

	    //
	    // rebuild the elements array
	    // to match the list of elements
	    //
	    if(n_valid_elems > 0)
	    {
		elements = new int[n_valid_elems];
		
		//
		// now store the indices in the array
		//
		int el = 0;
		switch(element_name_mode)
		{
		case SpotIndex:
		    for(int s=0; s < n_elems; s++)
		    {
			try
			{
			    int si = ((Integer)element_names.elementAt(s)).intValue();
			    if(si >= 0)
				elements[el++] = si;
			}
			catch(NumberFormatException nfe)
			{
			}
		    }
		    break;
		    
		case SpotName:
		    for(int s=0; s < n_elems; s++)
		    {
			int si = getIndexBySpotName((String)element_names.elementAt(s));
			if(si >= 0)
			    elements[el++] = si;
		    }
		    break;
		    
		case ProbeName:
		case GeneName:
		    for(int s=0; s < n_elems; s++)
		    {
			String enm = (String) element_names.elementAt(s);
			Vector indices = (Vector) ht.get(enm);
			if(indices != null)
			{
			    for(int i=0; i < indices.size(); i++)
				elements[el++] = ((Integer)indices.elementAt(i)).intValue();
			}
		    }
		    break;

		case MeasurementName:
		    for(int m=0; m < n_elems; m++)
		    {
			String mnm = (String) element_names.elementAt(m); 
			
			int m_id = getMeasurementFromName(mnm);
			if(m_id >= 0)
			{
			    // System.out.println(mnm + " is " + m_id);
			    
			    elements[el++] = m_id;
			}
		    }
		    break;
		    
		case MeasurementIndex:
		    final int nms = getNumMeasurements();
		    for(int m=0; m < n_elems; m++)
		    {
			int m_id = ((Integer)element_names.elementAt(m)).intValue();
			
			if((m_id >= 0) && (m_id < nms))
			{
			    elements[el++] = m_id;
			}
		    }
		    break;
		}
	    }
	    else
	    {
		elements = null;
	    }

	    // if this cluster has changed mode, the parent might need to update too
	    //
	    if(parent != null)
		parent.checkIsSpot();

	    // System.out.println(name + " " + n_valid_elems + " valid elems, " + n_elems + " names or ids");
	}

	//
	// recompute the mappings from whatever the cluster elements
	// were specified by to an array of spot indices
	//
	private void resetElements()
	{
	    setElements(element_name_mode, element_names);
	}

	public Vector getElementNames() { return element_names; }
	public int getElementNameMode() { return element_name_mode; }
   
	// set the elements using their symbolic name
	// (currently either spot or probe names)
	//

	/*
	public void setElementNames(int name_mode, Vector en)   
	{
	    element_names = en; 
	    element_name_mode = name_mode;

	    elements = new Vector();

	    for(int eln = 0; eln < en.size(); eln++)
	    {
		String nm = (String) en.elementAt(eln);
		switch(name_mode)
		{
		case ProbeName:
		    for(int s=0; s < getNumSpots(); s++)
		    {
			
			if(getProbeName(s).equals(nm))
			{
			    Integer i = new Integer(s);

			    // make sure each spot is only included once
			    // even if it's probe name is repeated in
			    // the list
			    //
			    if(elements.indexOf((Object)i) == -1)
				elements.addElement((Object)i);
			}
		    }
		    break;
		case SpotName:
		    Integer i = new Integer(getIndexBySpotName(nm));
		    if(i.intValue() >= 0)
			elements.addElement((Object)i);
		    break;
		case GeneNames:
		    reportAccessError("setElementNames", "gene mode not implemented yet");
		    return;
		}
	    }
	}
	*/

	public void setName(String n )      
	{ 
	    name = n;
	}

	// ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- 
	// -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ----
	// ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- 

	// returns the spot_id's of the cluster and all of it's children
	public int[] getAllClusterElements()
	{
	    Vector s_ids_v = new Vector();
	    
	    addAllClusterElements(s_ids_v);
	    
	    int vc = 0;
	    for(int v=0; v < s_ids_v.size(); v++)
		vc += ((int[]) s_ids_v.elementAt(v)).length;
	    
	    int p = 0;
	    int[] s_ids_a = new int[vc];
	    
	    for(int v=0; v < s_ids_v.size(); v++)
	    {
		int[] va = (int[]) s_ids_v.elementAt(v);
		for(int v2=0; v2 < va.length; v2++)
		    s_ids_a[p++] = va[v2];
	    }
	    
	    return  s_ids_a;
	}
	// adds to 's_ids_v' the elements int[] of the cluster 'cl' and all it's children
	private void addAllClusterElements(Vector s_ids_v)
	{
	    int[] c_s_ids = getElements();
	    
	    if(c_s_ids != null)
		s_ids_v.addElement(c_s_ids);
	    
	    for(int c=0; c < getNumChildren(); c++)
	    {
		ExprData.Cluster ch = (ExprData.Cluster)(getChildren().elementAt(c));
		ch.addAllClusterElements(s_ids_v);
	    }
	}
 

	// ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- 
	// -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ----
	// ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- 

	/**
	 * set glyph, propogate to children if there are any
	 */
	public void recursivelySetGlyph(int gly)
	{
	    glyph = gly;
	    glyph_cycle = false;

	    if(children != null)
	    {
		for(int c=0; c < children.size(); c++)
		    ((Cluster)children.elementAt(c)).recursivelySetGlyph(gly);
	    }
	}

	public void setGlyph(int gly)
	{
	    recursivelySetGlyph(gly);
	}

	// issue glyphs cyclicly to any leaf children
	//
	private void allocateGlyphs()
	{
	   glyph = 0;
	   int next_glyph = 0;
	   for(int c=0; c < children.size(); c++)
	   {
	       Cluster child = (Cluster)children.elementAt(c);
	       if(((Cluster)children.elementAt(c)).children == null)
		   child.glyph =  next_glyph++ % n_glyph_types;
	   }
	}

	/**
	 * set glyph cycle mode, propogate to children if there are any
	 */
	public void recursivelySetGlyphCycle(boolean cyc)
	{
	    glyph_cycle = cyc;

	    if(children != null)
	    {
		if(cyc == true)
		    allocateGlyphs();

		for(int c=0; c < children.size(); c++)
		    ((Cluster)children.elementAt(c)).recursivelySetGlyphCycle(cyc);
	    }
	}

	public void setGlyphCycle(boolean cyc)
	{
	    recursivelySetGlyphCycle(cyc);
	    
	    //generateClusterUpdate(ColourChanged, null);
	}
	
	public void chooseSuitableColourAndGlyph(Cluster root)
	{
	    glyph = Math.abs((int)(Math.random() * n_glyph_types));
	    int r = Math.abs((int)(Math.random() * 255));
	    int g = Math.abs((int)(Math.random() * 255));
	    int b = Math.abs((int)(Math.random() * 255));
	    colour = new Color(r, g, b);
	}
	
	// ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- 
	// -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ----
	// ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- 
	
	public void setColour(Color c) 
	{
	    colour = c;
	}

	private Color to_colour   = Color.white;
	private Color from_colour = Color.black;

	public Color getToColour()   { return to_colour; }
	public Color getFromColour() { return from_colour; }

	public void setColourRamp(Color new_from, Color new_to)
	{
	    from_colour = new_from;
	    to_colour = new_to;
	}

	private void recursivelySetColourSet(Cluster dest, Cluster src)
	{
	    dest.colour_set  = src.colour_set;
	    dest.to_colour   = src.to_colour;
	    dest.from_colour = src.from_colour;

	    if(dest.children != null)
	    {
		// how many of this nodes children are leafs nodes rather
		// than branches?

		int leaf_children = 0;
		for(int c=0;c < dest.children.size(); c++)
		{
		    if(((Cluster)dest.children.elementAt(c)).children == null)
			leaf_children++;
		}
		
		// generate a ramp of the correct size for the number of children
		//
		Color[] ramp = getColourRamp(dest.colour_set, leaf_children);

		// and assign the colours, or propagate the ColourSet
		//
		leaf_children = 0;
		for(int c=0;c < dest.children.size(); c++)
		{
		    Cluster child = (Cluster)dest.children.elementAt(c);
		    if(((Cluster)dest.children.elementAt(c)).children == null)
			child.colour = ramp[leaf_children++];
		    else
			recursivelySetColourSet(child, src);
		}		
	    }

	} 

	public void setColourSet(int cs)
	{
	    colour_set = cs;
	    recursivelySetColourSet(this, this);
	    //generateClusterUpdate(ColourChanged, this);
	}

	public Color[] getColourRamp(int cs, int steps)
	{
	    Color[] cvec = new Color[steps];

	    switch(cs)
	    {
	    case 0:
		float h = 0.0f;
		float h_step = (0.8f / ((float) steps));
		
		for(int c=0; c < steps; c++)
		{
		    cvec[c] = Color.getHSBColor(h, 0.75f, 1.0f);
		    h += h_step;
		}
		break;
	    case 1:
		double from_d_r = (double)(from_colour.getRed());
		double from_d_g = (double)(from_colour.getGreen());
		double from_d_b = (double)(from_colour.getBlue());
		
		double to_d_r = (double)(to_colour.getRed());
		double to_d_g = (double)(to_colour.getGreen());
		double to_d_b = (double)(to_colour.getBlue());

		double d_step = 1.0 / (double)steps;
		double d = 0.0;
		
		for(int c=0; c < steps; c++)
		{
		    cvec[c] = new Color((int)(((1.0-d) * from_d_r) + (d * to_d_r)),
					(int)(((1.0-d) * from_d_g) + (d * to_d_g)),
					(int)(((1.0-d) * from_d_b) + (d * to_d_b)));
		    d += d_step;
		}
	    }

	    return cvec;
	}

	// ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- 
	// -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ----
	// ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- 

	public void toggleShow()
	{
	    show = !show;
	    clusterChangedVisibility(this, false);
	}

	public void setShow(boolean s)
	{
	    if(s != show)
	    {
		show = s;
		clusterChangedVisibility(this, false);
	    }
	}

	public void showAll(boolean show)
	{
	    // this is a leaf node, hide all other nodes everywhere...
	    //
	    ClusterIterator clit = new ClusterIterator();
	    Cluster clust = clit.getCurrent();
	    
	    while(clust != null)
	    {
		if(clust != this)
		{  
		    if(clust.show != show)
		    {
			clust.show = show;
		    }
		}
		clust = clit.getNext();
	    }
	    clusterChangedVisibility(cluster_root, true);
	
	}

	/**
	 *  keep the recursive tail separate to make it easy to ensure only 
	 *  only notifyUpdate Event is generated at the very end, rather than lots...
	 */
	public void recursivelyShowGroup(Cluster clust, boolean show, int same_type, boolean is_spot)
	{
	    if(clust.show != show)
	    {
		clust.show = show;
	    }
	    if(clust.children != null)
	    {
		for(int c=0; c < clust.children.size(); c++)
		{
		    Cluster child = (Cluster) clust.children.elementAt(c);
		    if(child.show != show)
		    {
			child.show = show;
		    }
		    
		    if((same_type > 0) && (child.getIsSpot() != is_spot))
			return;
		    else
			recursivelyShowGroup(child, show, same_type, is_spot);
		}
	    }
	}
	
	public void showGroup(boolean show)
	{
	    recursivelyShowGroup(this, show, 0, false);
	    clusterChangedVisibility(cluster_root, true);
	}

	public void showGroup(boolean show, boolean same_type)
	{
	    recursivelyShowGroup(this, show, 1, getIsSpot());
	    clusterChangedVisibility(cluster_root, true);
	}

	// ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- 
	// -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ----
	// ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- 

	public String toString()
	{
	    return name;
	}

	public String describe()
	{
	    String res = "< '" + name + "' ";
	    if(name == null)
		res = "< [no name] ";
	    if(children == null)
		res += " no children ";
	    else
		res += (children.size() + " children");
	    if(elements == null)
		res += " no elements";
	    else
		res += (elements.length + " elements");
	    res += " >";

	    return res;
	}

	
	public int recursivelyCountDescendants()
	{
	    int desc_total = 0;
	    
	    if(children != null)
	    {
		for(int c=0; c < children.size(); c++)
		{
		    Cluster child = (Cluster) children.elementAt(c);

		    desc_total += child.recursivelyCountDescendants();
		}
	    }
	    
	    return 1 + desc_total;
	}
    }

    private Cluster cluster_root = null;
    private Hashtable cluster_id_to_cluster;

    // ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- 
    // -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ----
    // ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- 

    public class ClusterIterator 
    {
	public ClusterIterator()
	{
	    this(true);
	}

	public ClusterIterator(boolean only_non_group_)
	{
	    only_non_group = only_non_group_;
	    reset();
	}

	public Cluster reset()
	{
	    // need to keep track of the path taken through the tree
	    path        = new Vector();
	    // which child was traversed at the corresponding step in the path
	    child_count = new Vector();
	    child = 0;
	    current = cluster_root;
	    
	    //findFirstSuitableNode();

	    return current;
	}

	public Cluster getCurrent() 
	{ return current; }

	public Cluster getFirstLeaf()
	{
	    if(current == null)
		return null;
	    
	    reset();
	    
	    // there might just be only one node which is a leaf,
	    // otherwise, use the normal leaf search method
	    // to descend to the first leaf
	    //
	    if(current.children != null)
		getNextLeaf();

	    return current;
	}

	/**
	 *  find the next cluster in sequence
	 */
	public Cluster getNext()
	{
	    if(current == null)
		return null;

	    if(debug)
		System.out.println("--- getNext(): current is " + current);
	    
	    if(current.children == null)
	    {
		// go back up the path to this nodes parent, and take the next child
		//
		while(path.size() > 0)
		{
		    Cluster last = (Cluster) path.elementAt(path.size() - 1);
		    
		    if(debug)
		    {
			if(last != null)
			{
			    System.out.println("---            last path node: " + last);
			}
			else
			    System.out.println("---            path is empty!");
		    }

		    int last_child = ((Integer)child_count.elementAt(child_count.size() - 1)).intValue();
		    last_child++;

		    if(debug)
			System.out.println("---               seeking child number " + last_child);

		    if(last_child < last.children.size())
		    {
			// increment the child_count of the node at the end of the path
			//
			child_count.setElementAt(new Integer(last_child), (child_count.size() - 1));

			current = (Cluster) last.children.elementAt(last_child);

			if(debug)
			    System.out.println("---           found, new current is " + current);

			return current;
		    }
		    else
		    {
			// there are no more children in this parent,
			// go back one more step along the path
			//
			child_count.removeElementAt(child_count.size() - 1);
			path.removeElementAt(path.size() - 1);

			if(debug)
			{
			    System.out.println("---            NOT found, returning to parent...");
			    System.out.println("---            (there are " + path.size() + " nodes in the path)");
			}
			
		    }
		}

		if(debug)
		    System.out.println("--- getNext(): no more nodes");

		current = null;
		return null;
	    }
	    else
	    {
		if(debug)
		    System.out.println("---            descending to first child");

		// add this node to the path
		path.addElement(current);
		child_count.addElement(new Integer(0));

		// descend to the first child
		current = (Cluster) current.children.elementAt(0);

		if(debug)
		    System.out.println("---            new current is " + current);

		return current;
	    }
	}
	
	/**
	 *  find the next non-group cluster in sequence
	 */
	public Cluster getNextLeaf()
	{
	    getNext();
	    while((current != null) && (current.children != null))
		getNext();
	    return current;
	}

	/**
	 *  returns the current tree depth of the iterator
	 */
	public int getDepth()
	{
	    return path.size();
	}

	/**
	 *  move the interator to a particular position
	 */
	public void positionAt(Cluster c)
	{
	    if(debug)
		System.out.println("- trying to position at " + c);

	    reset();
	    
	    if(current == null)
		System.out.println("- ODD! no clusters?");

	    // search until found , or we run out of clusters
	    //
	    while((current != null) && (current != c))
	    {
		getNext();
		//System.out.println("- checking: " + current);
	    }
	    if(current == null)
	    {
		// not found, reposition at the start
		//System.out.println("- all clusters searched, not found");
		reset();
	    }
	    else
	    {
		//System.out.println("- all clusters searched, found ok");
	    }
	}

	/**
	 *  search downwards for a non-group (i.e. terminal) node
	 *  start from current position and update current position
	 *  as the search progresses
	 */
	private void findFirstNodeWithElems()
	{
	    boolean not_found = ((current != null) && (current.children != null));

	    while(not_found)
	    {
		if(debug)
		{
		    if(only_non_group)
			System.out.println("--- fFNWElems(): seeking node with no children, start at " + current);
		    else
			System.out.println("--- fFNWElems(): seeking any node, start at " + current);
		}

		// this is a group node, 
		// add it to the path
		//
		if(debug)
		    System.out.println("--- fFNWElems(): adding <" + current + "> to search path"); 
		
		path.addElement(current);
		child_count.addElement(new Integer(0));
		
		// descend to the first child
		// (this is safe because there must eventually
		//  be a non-group child along this path)
		//
		current = (Cluster) current.children.elementAt(0);
		
		not_found = ((current != null) && (current.children != null));
	    }

	    if(debug)
	    {
		if(current != null) 
		{
		    if(only_non_group)
		    {
			if(current.children != null)
			    System.out.println("--- fFSNode(): not found !!!");
			else
			    System.out.println("--- fFSNode(): found ok, is " + current);
		    }
		    else
		    {
			if(current != null)
			    System.out.println("--- fFSNode(): something found");
			else
			    System.out.println("--- fFSNode(): nothing found");
		    }
		}
		else
		    System.out.println("--- fFSNode(): not found, no more nodes");
	    }
	}

	private final boolean debug = false;

	private boolean only_non_group;
	private Vector path;
	private Vector child_count;
	private Cluster current;
	private int child;
    }

    public Cluster getRootCluster()
    { 
	if(cluster_root == null)
	{
	    cluster_root = new Cluster("Root");	
	}

	return cluster_root; 
    }

    // ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- 
    // -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ---- -- ----

    /*
    // used by RMI interface
    public Cluster createCluster(String name, int name_mode, Vector elems) throws java.rmi.RemoteException
    {
	return new Cluster(name, name_mode, elems);
    }
    */
    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------

    // A set of accessor methods for clusters which generate update events
    // This interface should be used when you are modiying 'live' clusters,
    // and the Cluster's own accessor methods should be used when you want to 
    // modifiy state without generating events (e.g. when loading).
    //
    public void setClusterElements(Cluster cl, int name_mode, Vector e)
    {
	cl.setElements(name_mode, e);
	generateClusterUpdate(ElementsAdded, cl);
    }
    /*
    public void setClusterElementNames(Cluster cl, int name_mode, Vector en)   
    {
	cl.setElementNames(name_mode, en);
	// need to do something else?
	generateClusterUpdate(NameChanged, null);
    }
    */
    public void setClusterName(Cluster cl, String n )    
    {
	cl.setName(n);
	generateClusterUpdate(NameChanged, null);
    }
    public void setClusterGlyph(Cluster cl, int gly)
    {
	cl.setGlyph(gly);
	generateClusterUpdate(ColourChanged, cl);
    }

    public void setClusterGlyphCycle(Cluster cl, boolean cyc)
    {
	cl.setGlyphCycle(cyc);
	generateClusterUpdate(ColourChanged, cl);
    }

    public void setClusterColour(Cluster cl, Color c) 
    {
	cl.setColour(c);
	generateClusterUpdate(ColourChanged, cl);
    }
    public void setClusterColourRamp(Cluster cl, Color new_from, Color new_to)
    {
	cl.setColourRamp(new_from, new_to);
	generateClusterUpdate(ColourChanged, cl);
    }
    public void setClusterColourSet(Cluster cl, int cs)
    {
	cl.setColourSet(cs);
	generateClusterUpdate(ColourChanged, cl);
    }
    public void toggleClusterShow(Cluster cl)
    {
	cl.toggleShow();
	generateClusterUpdate(VisibilityChanged, cl);
    }
    
    public void setClusterShow(Cluster cl, boolean show)
    {
	if(cl.getShow() != show)
	{
	    cl.setShow(show);
	    generateClusterUpdate(VisibilityChanged, cl);
	}
    }
    public void clusterShowAll(Cluster cl, boolean show)
    {
	cl.showAll(show);
	generateClusterUpdate(VisibilityChanged, cl);
    }

    public void clusterShowAllSpots(Cluster cl, boolean show)
    {
	clusterShowAllOfType(true, cl, show);
    }
    public void clusterShowAllMeasurements(Cluster cl, boolean show)
    {
	clusterShowAllOfType(false, cl, show);
    }

    private void clusterShowAllOfType(boolean spot, Cluster cl, boolean show)
    {
	ClusterIterator clit = new ClusterIterator();
	    
	Cluster clust = clit.getCurrent();
	while(clust != null)
	{
	    if(clust != cl)
	    {
		if(clust.getIsSpot())
		{
		    if(spot)
			clust.setShow(show);
		}
		else
		{
		    if(!spot)
			clust.setShow(show);
		}
			
	    }
	    clust = clit.getNext();
	}	
	generateClusterUpdate(VisibilityChanged, cl);
    }

    public void clusterShowGroup(Cluster cl, boolean show)
    {
	if(cl != null)
	    cl.showGroup(show);
	//recursivelyShowGroup(cl, show);
	generateClusterUpdate(VisibilityChanged, cl);
    }

    public void clusterShowGroup(Cluster cl, boolean show, boolean same_type)
    {
	if(same_type)
	    cl.showGroup(show, true);
	else
	    cl.showGroup(show);
	//recursivelyShowGroup(cl, show);
	generateClusterUpdate(VisibilityChanged, cl);
    }

    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------

    // keep a count of how many visible clusters each gene is in
    //
    private int[]     in_visible_clusters = null;
    private Hashtable cluster_visible_ht = new Hashtable();    // what the cluster counts currently represent

    public int[] getInVisibleClusterArray() { return in_visible_clusters; }

    public int inVisibleClusters(int spot)
    {
	if(in_visible_clusters == null)
	{
	   countInVisibleClusters();
	}
	return in_visible_clusters[spot];
    }
   
    public void clusterChangedVisibility(Cluster clust, boolean recurse)
    {
	//System.out.println("clusterChangedVisibility() thinking...");

	final String id_str = String.valueOf(clust.id);

	final String isvis = (String) cluster_visible_ht.get(id_str);

	final boolean was_visible = ((isvis != null) && (isvis.length() == 1));

	if(was_visible != clust.show)
	{
	    // if we are here, then the visibility really has changed
	    // as far as the counts are concerned
	    
	    if(clust.getIsSpot())
	    {
		if(clust.show)
		{
		    cluster_visible_ht.put(id_str, "x");
		}
		else
		{
		    cluster_visible_ht.put(id_str, "");
		}
		
		//System.out.println("Cluster " + clust.name + (clust.show ? " shown" : " hidden"));
		
		int iv;
		int[] ve = clust.elements;
		
		int changes = 0;
		
		if(ve != null)
		{
		    final int n_elems = ve.length;
		    for(int si=0; si < n_elems; si++)
		    {
			iv = ve[si];
			if((iv >= 0) && (iv < n_spots))
			{
			    if(clust.show)
				in_visible_clusters[iv]++;
			    else
				in_visible_clusters[iv]--;
			    
			    changes++;
			}
		    }
		}
		//System.out.println("  " + changes + " changes made");
	    }
	}

	if(recurse == true)
	{
	    Vector v = clust.children;
	    if(v != null)
	    {
		final int n_children = v.size();
		for(int ch=0; ch < n_children; ch++)
		{
		    clusterChangedVisibility((Cluster) clust.children.elementAt(ch), true);
		}
	    }
	}
    }

    /**
     *  called when spots are removed or added, 
     *  rebuilds the mapping from cluster element names to spot indexes
     */
    private void updateClusterToSpotMappings()
    {
	ClusterIterator clit = new ClusterIterator();
	    
	Cluster clust = clit.getCurrent();
	while(clust != null)
	{
	    if(clust.getIsSpot())
	    {
		clust.resetElements();
	    }
	    clust = clit.getNext();
	}
	countInVisibleClusters();
    }

    /**
     *  called when measurements are removed or added, 
     *  rebuilds the mapping from cluster element names to measurement indexes
     */
    private void updateClusterToMeasurementMappings()
    {
	//int visit = 0;
	//int changed = 0;

	ClusterIterator clit = new ClusterIterator();
	    
	Cluster clust = clit.getCurrent();
	while(clust != null)
	{
	    if(!clust.getIsSpot())
	    {
		
		//int old_n_els = (clust.getElements() == null ? 0 : clust.getElements().length);

		clust.resetElements();

		//int n_els = (clust.getElements() == null ? 0 : clust.getElements().length);

		//if(old_n_els != n_els)
		//    changed++;

		//visit++;
	    }
	    clust = clit.getNext();
	}
	countInVisibleClusters();
	
	//System.out.println("updateClusterToMeasurementMappings() " + visit + " clusters checked, " + changed + " changed");
    }


    /**
     *  reset the arrays and do the counts from scratch
     */
    public void countInVisibleClusters()
    {
	//System.out.println("countInVisibleClusters() begins");
	    
	int visible_count = 0;
	int visible_spots = 0;

	cluster_visible_ht = new Hashtable();

	in_visible_clusters = new int[n_spots];		

	for(int s=0;s<n_spots;s++)
	    in_visible_clusters[s] = 0;

	ClusterIterator clit = new ClusterIterator();
	    
	Cluster clust = clit.getCurrent();
	while(clust != null)
	{
	    if(clust.show)
	    {
		visible_count++;
		
		cluster_visible_ht.put(String.valueOf(clust.id), "x");

		Integer i;
		int iv, gene_index;
		int[] ev = clust.elements;
		if(ev != null)
		{
		    for(int si=0; si < ev.length; si++)
		    {
			iv = ev[si];
			if(iv < n_spots)
			{
			    in_visible_clusters[iv]++;
			    visible_spots++;
			}
		    }
		}
	    }
	    clust = clit.getNext();
	}
	
	//System.out.println("countInVisibleClusters():  " + visible_spots + " spots in " + 
	//		   visible_count + " clusters");
    }

    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------
    // -------------------------------------------------------------------------------

    public void addChildToCluster(Cluster cl, Cluster ch)
    {
	cl.addCluster(ch);
	clusterChangedVisibility(cl, true);
	generateClusterUpdate(ElementsAdded, cl);
    }

    public void addCluster(Cluster cl)
    {
	//try
	{
	    getRootCluster().addCluster(cl);
	    clusterChangedVisibility(cl, true);
	    generateClusterUpdate(ElementsAdded, cl);
	}
	//catch(java.rmi.RemoteException re)
	{
	}
    }

    public void addCluster(String name, int elem_name_mode, Vector elems)
    {
	Cluster newie = new Cluster(name, elem_name_mode, elems);
	addCluster(newie);
    }

    public void deleteCluster(Cluster goner)
    {
	// locate it in the tree....
	if((goner.parent == null) || (goner.parent.children == null))
	    return;
	
	goner.showGroup(false);
	goner.setShow(false);
	clusterChangedVisibility(goner, true);

	// System.out.println("deleting " + goner.name);

	// remove it from parent's child list
	goner.parent.children.removeElement(goner);
	goner.checkIsSpot();

	// if it was the only child, kill the parent's child vector
	if(goner.parent.children.size() == 0)
	    goner.parent.children = null;

	// if it has any children, kill them too....
	// ...except we dont need to, the link to
	// them has been lost so that will do as
	// far as johnny garbage collector is concerned
	//

	generateClusterUpdate(ElementsRemoved, goner);
    }

    public void removeAllClusters()
    {
	if(cluster_root == null)
	    return;

	Vector root_ch = cluster_root.getChildren();

	if(root_ch != null)
	{
	    int ch =0 ;
	    while(root_ch.size() > 0)
	    {
		// deleting a cluster will remove it from it's
		// parent's (ie this one) child list
		//
		deleteCluster((Cluster) root_ch.elementAt(0));
	    }
	}

	cluster_id_to_cluster.clear();

	// remember to keep the root in the table...
	if(cluster_root != null)
	    cluster_id_to_cluster.put( new Integer(cluster_root.id), cluster_root );
    }

    // ---------------- --------------- --------------- ------------- ------------

    // could allow a mask to blot out points with no value
    // (like Norm's NaN values in matlab)
    // or could use float.isNan()
    //

    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    //
    // main accessor fns
    //
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------

    public class Measurement implements Cloneable
    {
	private String name;
	private int    data_type;
	
	private Measurement parent;  // associates error and probability values with another Measurement


	// what is this for? why should a measurement have unique names?
	private DataTags  dtags;     // unique probe/spot names for this measurement, or null if using the master tags


	private double[]  data;      // the actua values
	
	private boolean show;        // is this Measurement enabled?
	
	private double min_e_value;  // range of values
	private double max_e_value;

	public String id;               // the MaXD ID of this Measurement

	// -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / --
	//
	// the Measurement can contain any number of MeasurementAttrs
	//
	// -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / --

	public class MeasurementAttr
	{
	    public String name;            // unique id
	    public String source;          // who created it
	    public String time_created;    // when was it created
	    public String time_last_modified;   // when was it last changed
	    public String value;           // value for name
	}

	private Hashtable db_attributes; // maps name -> MeasurementAttr

	public Hashtable getAttributes()                         { return db_attributes; };

	// !! DEPRECATED
	public void setAttribute(String name, String value) 
	{ 
	    MeasurementAttr ma = (MeasurementAttr) db_attributes.get(name);
	    if(ma == null)
	    {
		ma = new MeasurementAttr();
		ma.name = name;
		ma.source = null;
		ma.time_created = (new Date()).toString();
		ma.time_last_modified = null;
		ma.value = value;
		db_attributes.put(name, ma);
	    }
	    else
	    {
		ma.time_last_modified = (new Date()).toString();
		ma.value = value;
	    }
	}
	
	public void setAttribute(String name, String source, String value)
	{ 
	    setAttribute(name, source, value, null, null);
	}


	public void setAttribute(String name, String source, String value, String created, String modified)
	{ 
	    MeasurementAttr ma = (MeasurementAttr) db_attributes.get(name);
	    
	    boolean make_new = false;
	    
	    if(ma == null)
	    {
		make_new = true;
	    }
	    else
	    {
		if(source == null)
		{
		    if(ma.source != null)
			make_new = true;
		}
		else
		{
		    if(ma.source != null)
			if(!source.equals(ma.source))
			    make_new = true;
		}
	    }

	    if(make_new)
	    {
		ma = new MeasurementAttr();
		ma.name = name;
		ma.source = source;
		ma.time_created = (created == null ? (new Date()).toString() : created);
		ma.time_last_modified = modified;
		ma.value = value;
		db_attributes.put(name, ma);
	    }
	    else
	    {
		ma.time_last_modified = modified;
		ma.value = value;
	    }
	}
	

	public String getAttribute(String name)               
	{ 
	    MeasurementAttr ma = (MeasurementAttr) db_attributes.get(name);
	    if(ma == null)
	    {
		return null;
	    }
	    else
	    {
		return ma.value;
	    }
	}
	
	public String getAttribute(String source, String name)               
	{ 
	    MeasurementAttr ma = (MeasurementAttr) db_attributes.get(name);
	    if(ma == null)
	    {
		return null;
	    }
	    else
	    {
		if(ma.source.equals(source))
		    return ma.value;
		else
		    return null;
	    }
	}

	// addAttribute()  
	//   - makes sure the name is unique
	//
	public void addAttribute(String name, String source, String value) 
	{ 
	    MeasurementAttr ma = (MeasurementAttr) db_attributes.get(name);
	    String new_name = name;

	    if(ma != null)
	    {
		// find the highest suffix number....
		int max_suf = 0;
		for (Enumeration e = db_attributes.keys(); e.hasMoreElements() ;) 
		{
		    String key = (String) e.nextElement();
		    if(key.startsWith(name))
		    {
			try
			{
			    String suffix = key.substring( new_name.length() + 1 );
			    int isuf = (Integer.valueOf(suffix)).intValue();
			    if(isuf > max_suf)
				max_suf = isuf;
			}
			catch(NumberFormatException nfe)
			{
			}
			catch(StringIndexOutOfBoundsException sioobe)
			{
			}
		    }
		}
		
		new_name = name + "." + (max_suf+1);

		// System.out.println("addAttribute() : " + name + " -> " + new_name);
		
	    }
	    
	    ma = new MeasurementAttr();
	    ma.name = new_name;
	    ma.source = source;
	    ma.time_created = (new Date()).toString();
	    ma.time_last_modified = null;
	    ma.value = value;
	    
	    db_attributes.put(new_name, ma);

	    
	}

	// -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / --
	//
	// the Measurement can contain any number of SpotAttributes
	//  with arbitrary types (i.e. anything supported by maxdSQL)
	// 
	// -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / --

	public Vector spot_att_name;
	public Vector spot_att_unit;
	public Vector spot_att_datatype;
	public Vector spot_att_data;

	public final static int SpotAttributeIntDataType    = 0;
	public final static int SpotAttributeDoubleDataType = 1;
	public final static int SpotAttributeCharDataType   = 2;
	public final static int SpotAttributeTextDataType   = 3;

	public int getNumSpotAttributes() { return spot_att_name.size(); }

	public String getSpotAttributeName(int a)
	{
	    return (String) spot_att_name.elementAt(a);
	}
	public String getSpotAttributeUnit(int a)
	{
	    return (String) spot_att_unit.elementAt(a);
	}
	public String getSpotAttributeDataType(int a)
	{
	    return (String) spot_att_datatype.elementAt(a);
	}
	public int getSpotAttributeDataTypeCode(int a)
	{
	    final String[] type_names = { "INTEGER", "DOUBLE", "CHAR", "TEXT" } ;
	    String dtype = (String)spot_att_datatype.elementAt(a);
	    for(int tn=0; tn < type_names.length; tn++)
		if(dtype.equals(type_names[tn]))
		    return tn;
	    return -1;
	}
	public Object getSpotAttributeData(int a)
	{
	    return (Object) spot_att_data.elementAt(a);
	}
	public void setSpotAttributeData(int a, Object o)
	{
	    spot_att_data.setElementAt(o, a);
	}

	public String getSpotAttributeDataValueAsString(int a, int s)
	{
	    if(s > n_spots)
	    {
		reportAccessError("getSpotAttributeDataValueAsString", "spot index " + s+ " too high");
		return null;
	    }
	    if(s < 0)
	    {
		reportAccessError("getSpotAttributeDataValueAsString", "spot index must be >= 0, not " + s);
		return null;
	    }
	    
	    String dt = (String) spot_att_datatype.elementAt(a);
	    if(dt.equals("INTEGER"))
	    {
		int[] ivec = (int[]) spot_att_data.elementAt(a);
		return String.valueOf(ivec[s]);
	    }
	    if(dt.equals("DOUBLE"))
	    {
		double[] dvec = (double[]) spot_att_data.elementAt(a);
		return String.valueOf(dvec[s]);
	    }
	    if(dt.equals("CHAR"))
	    {
		char[] cvec = (char[]) spot_att_data.elementAt(a);
		return String.valueOf(cvec[s]);
	    }
	    if(dt.equals("TEXT"))
	    {
		String[] tvec = (String[]) spot_att_data.elementAt(a);
		return tvec[s];
	    }
	    return null;
	}

	public Object getSpotAttributeData(String name)
	{
	    int a = getSpotAttributeFromName(name);
	    if(a >= 0)
		return (Object) spot_att_data.elementAt(a);
	    else
	    {
		reportAccessError("getSpotAttributeData", "name \"" + name + "\" not found");
		return null;
	    }
	}
	public int getSpotAttributeFromName(String name)
	{
	    for(int a=0; a < spot_att_name.size(); a++)
	    {
		if(name.equals((String)spot_att_name.elementAt(a)))
		    return a;
	    }
	    return -1;
	}

	public void removeAllSpotAttributes()
	{
	    spot_att_name.removeAllElements();
	    spot_att_unit.removeAllElements();
	    spot_att_datatype.removeAllElements();
	    spot_att_data.removeAllElements();
	}

	// replaces the data of an attribute of the same name if there was one,
	// or installs a new attribute if not
	//
	// att_data should be an array of the appropriate type, int[], double[], char[] or String[]
	// or be null
	//
	public void addSpotAttribute(String name, String unit, String datatype, Object att_data)
	{
	    // sanity check on type and data length
	    //
	    boolean ok = false;
	    int len = -1;

	    if(datatype.equals("INTEGER"))
	    {
		ok = true;
		if(att_data != null)
		    len = ((int[]) att_data).length;
	    }
	    if(datatype.equals("DOUBLE"))
	    {
		ok = true;
		if(att_data != null)
		    len = ((double[]) att_data).length;
	    }
	    if(datatype.equals("CHAR"))
	    {
		ok = true;
		if(att_data != null)
		    len = ((char[]) att_data).length;
	    }
	    if(datatype.equals("TEXT"))
	    {
		ok = true;
		if(att_data != null)
		    len = ((String[]) att_data).length;
	    }
	    if(ok == false)
	    {
		reportAccessError("addSpotAttribute", "data type '" + datatype + "' not recognised");
		return;
	    }
	    if((len >= 0) && (data != null) && (len != data.length))
	    {
		reportAccessError("addSpotAttribute", "data vector is not the right length (is " + 
				  len + ", should be " + data.length + ")");
		return;
	    }
	    int pos =  getSpotAttributeFromName(name);
	    if(pos >= 0)
	    {
		spot_att_datatype.setElementAt(datatype, pos);
		spot_att_unit.setElementAt(unit, pos);
		spot_att_data.setElementAt(att_data, pos);
	    }
	    else
	    {
		spot_att_name.addElement(name);
		spot_att_datatype.addElement(datatype);
		spot_att_unit.addElement(unit);
		spot_att_data.addElement(att_data);
	    }
	}
	
	public void setSpotAttributeName( int sa_id, String new_name )
	{
	    if((sa_id >= 0) && (sa_id < spot_att_name.size()))
	    {
		spot_att_name.setElementAt( new_name, sa_id );
	    }
	    else
	    {
		reportAccessError("setSpotAttributeName", "illegal SpotAttribute index");
	    }

	}

	
	public void removeSpotAttribute( int sa_id )
	{
	    if((sa_id >= 0) && (sa_id < spot_att_name.size()))
	    {
		spot_att_name.removeElementAt( sa_id );
		spot_att_datatype.removeElementAt( sa_id );
		spot_att_unit.removeElementAt( sa_id );
		spot_att_data.removeElementAt( sa_id );
	    }
	    else
	    {
		reportAccessError("removeSpotAttribute", "illegal SpotAttribute index");
	    }

	}


	// -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / --
	//
	// a variety of constructors
	// 
	// -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / --

	public Measurement() 
	{
	    this("Undefined", UnknownDataType, null);
	}

	public Measurement(String n_, int dt_, double[] da_)
	{
	    name = n_;
	    data = da_;
	    data_type = dt_;
	    show = true;
	    dtags = new DataTags();
	    db_attributes= new Hashtable();
	    spot_att_name = new Vector();
	    spot_att_data = new Vector();
	    spot_att_unit = new Vector();
	    spot_att_datatype = new Vector();
	}

	public Measurement cloneMeasurement()
	{
	    Measurement new_m = null;
	    try
	    {
		new_m = (Measurement) this.clone();

		// duplicate the data arrays
		if(data != null)
		{
		    new_m.data = new double[data.length];
		    for(int s=0;s<data.length;s++)
			new_m.data[s] = data[s];
		}

		new_m.db_attributes = (Hashtable) (this.db_attributes).clone();

		new_m.spot_att_name = (Vector) (this.spot_att_name).clone();
		new_m.spot_att_data = (Vector) (this.spot_att_data).clone();
		new_m.spot_att_datatype = (Vector) (this.spot_att_datatype).clone();

		new_m.dtags = new DataTags();
	    }
	    catch(CloneNotSupportedException cnse)
	    {
	    }
	    
	    return new_m;
	}

	public String toString()
	{
	    String desc = name + ": ";

	    if(data == null)
	    {
		desc += "no data,";
	    }
	    else
	    {
		desc += data.length + " data items, ";

	    }

	    if(dtags == null)
	    {
		desc += "no tags";
	    }
	    else
	    {
		desc += ((dtags.spot_name==null) ? "no spot names, " : (dtags.spot_name.length + " spot names, ")) + 
			((dtags.probe_name==null) ? "no probe names, " : (dtags.probe_name.length + " probe names, ")) + 
		        ((dtags.gene_names==null) ? "no gene names " : (dtags.gene_names.length + " gene names ")); 
	    }
	    return desc;
	}

	public String getName() { return name; }
	public void setName(String n) { name = n; }
	
	public void setDataType(int dt) { data_type = dt; }
	public int  getDataType()       { return data_type; }

	public void setDataTypeString(String dt) 
	{ 
	    data_type = UnknownDataType;
	    for(int dtt=0; dtt < UnknownDataType; dtt++)
		if(dt.equals(data_type_name[dtt]))
		    data_type = dtt;
	}

	public String  getDataTypeString()       
	{ return data_type_name[data_type]; }
	
	private final String[] data_type_name = { "Abs.Expression", "Ratio Expression", "Probability", "Error Value", "Unknown" };

	public void     setShow(boolean s) { show = s; }
	public boolean  getShow()          { return show; }
	
	public void     setData(double[] newd) { data = newd; }
	public double[] getData()              { return data; }

	public String getArrayTypeName() { return getAttribute("ArrayType name"); };

	
	public int getNumSpots() { return ( data == null ? 0 : data.length ); }


	public String getSpotName(int s) { return ( dtags == null ? null : dtags.spot_name[s] ); }

	public void setDataTags(DataTags new_dts) { dtags = new_dts; }
	public DataTags  getDataTags() { return dtags; }

	// --------------

	public double eValue(int s) { return data[s]; }

	public void setEValue(int s, double v)    
	{
	    double old_v = data[s];
	    data[s] = v;
	    
	    boolean min_max_changed = false;
	    
	    if( ! Double.isNaN( v ) && ! Double.isInfinite( v ) )
	    {
		if(v > max_e_value)
		{
		    max_e_value = v;
		    min_max_changed = true;
		}
		if(v < min_e_value)
		{
		    min_e_value = v;
		    min_max_changed = true;
		}
	    }

	    // dont generate any events....
	    /*
	    if(min_max_changed)
		generateDataUpdate(RangeChanged);

	    generateDataUpdate(ValuesChanged);
	    */
	}

    }
    
    public final String[] spot_attribute_types = { "INTEGER", "DOUBLE", "CHAR", "TEXT" };

    // -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / --
    //
    // DataTags describe what (probe,gene,spot) is each elements of the data vector
    // 
    // -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / -- / --
    
    public class DataTags 
    {
	public String[]   spot_name;
	public String[]   probe_name;
	public String[][] gene_names;  // each probe can be looking for multiple Genes

	public Hashtable  spot_name_to_index;

	//public Hashtable  probe_name_to_indices;
	//public Hashtable  gene_name_to_indices;
	
	// all measured in _chars_ not pixels
	public int longest_name;
	public int longest_len;
	public int longest_gene_name;
	public int longest_spot_name;
	public int longest_probe_name;

	public DataTags()
	{
	    spot_name = null;
	    probe_name = null;
	    gene_names = null;
	    spot_name_to_index = null;
	    initLengths();
	}
	
	public DataTags(String[] sn, String[] pn)
	{
	    this(sn, pn, null);
	}
	
	public DataTags(String[] sn, String[] pn, String[][] gn)
	{
	    spot_name = sn;
	    probe_name = (pn == null) ? new String[spot_name.length] : pn;
	    gene_names = gn;
	    initLengths();
	}
	private void initLengths()
	{
	    longest_name = -1;
	    longest_len = 0;
	    longest_gene_name = -1;
	    longest_spot_name = -1;
	    longest_probe_name = -1;
	}

	public final String[] getSpotName()  { return spot_name; }
	public final String[] getProbeName() { return probe_name; }
	public final String[][] getGeneNames() { return gene_names; }

	public final String getSpotName(int s)  { return spot_name == null ? null :  spot_name[s]; }
	public final String getProbeName(int s) { return probe_name == null ? null :  probe_name[s]; }
	public final String[] getGeneNames(int s) { return gene_names == null ? null :  gene_names[s]; }

	public final void setSpotName(String[] sn)    
	{ 
	    spot_name = sn;
	    buildSpotNameToIndex();
	    //	    findLongestNames(SpotName);
	}

	public final void setProbeName(String[] pn)   
	{ 
	    cached_probe_name_ht = null;
	    probe_name = pn;   
	    //findLongestNames(ProbeName); 
	}

	public final void setGeneNames(String[][] gn) 
	{ 
	    cached_gene_name_ht = null;
	    gene_names = gn;   
	    //findLongestNames(GeneName); 
	}
	
	public final void addGeneName(int s, String gn)
	{
	    cached_gene_name_ht = null;
	    
	    // make sure there is a data array to write to first
	    if(gene_names == null)
	    {
		if(spot_name == null)
		    return;
		gene_names = new String[spot_name.length][];
	    }

	    String[] new_gns = null;

	    if(gene_names[s] != null)
	    {
		new_gns = new String[gene_names[s].length + 1];
		for(int a=0; a < gene_names[s].length; a++)
		    new_gns[a] = gene_names[s][a];
		new_gns[ gene_names[s].length ] = gn;
	    }
	    else
	    {
		new_gns = new String[1];
		new_gns[0] = gn;
	    }
	    
	    gene_names[s] = new_gns;
	}

	public final void setSpotName(int s, String sn)    
	{ 
	    spot_name[s] = sn; 
	    if(spot_name_to_index == null)
		spot_name_to_index = new Hashtable();
	    spot_name_to_index.put(sn, new Integer(s));
	    //findLongestNames(SpotName); 
	}

	public final void setProbeName(int s, String pn)   
	{ 
	    cached_probe_name_ht = null;
	    probe_name[s] = pn;   
	    //findLongestNames(ProbeName); 
	}
	
	public final void setGeneName(int s, int g, String gn) 
	{ 
	    cached_gene_name_ht = null;
	    // make sure there is a data array to write to first
	    if(gene_names == null)
	    {
		if(spot_name == null)
		    return;
		gene_names = new String[spot_name.length][];
	    }
	    if(gene_names[s].length > g)
	    {
		gene_names[s][g] = gn; 

		//findLongestNames(GeneName); 
	    }
	}

	public final void setGeneNames(int s, String[] gn) 
	{ 
	    cached_gene_name_ht = null;
	    
	    // make sure there is a data array to write to first
	    if(gene_names == null)
	    {
		if(spot_name == null)
		    return;
		gene_names = new String[spot_name.length][];
	    }

	    gene_names[s] = gn; 
	    //findLongestNames(GeneName); 
	}
	
	public final void buildSpotNameToIndex()
	{
	    master_dtags.spot_name_to_index = new Hashtable();
	    
	    if(spot_name == null)
		return;
	    
	    for(int s=0; s < spot_name.length; s++)
	    {
		master_dtags.spot_name_to_index.put(spot_name[s], new Integer(s));
	    }
	}

	// makes a new name and puts it into the table 
	// so the next call with the same frefix will also generate a unique name
	//
	public final String getUniqueSpotName(String prefix, int sid)
	{
	    String test_name = prefix;
	    int name_index = 1;

	    boolean name_used = (master_dtags.spot_name_to_index.get(test_name) != null);

	    while(name_used == true)
	    {
		test_name = prefix + "(" + (++name_index) + ")";
		
		name_used = (master_dtags.spot_name_to_index.get(test_name) != null);
	    }
	    
	    master_dtags.spot_name_to_index.put(test_name, new Integer(sid));

	    return test_name;
	}

	// probe names mapped to vectors of spot indices
	// (because probes can occur more thah once....)
	//
	public Hashtable getProbeNameHashtable()
	{
	    if(cached_probe_name_ht == null)
	    {
		Hashtable probe_name = new Hashtable();
		
		for(int psearch=0; psearch < n_spots; psearch++)
		{
		    String pname = getProbeName(psearch);
		    if(pname != null)
		    {
			Vector indices = (Vector) probe_name.get(pname);
			if(indices == null)
			{
			    indices = new Vector();
			    probe_name.put(pname, indices);
			}
			indices.addElement(new Integer(psearch));
		    }
		}
		cached_probe_name_ht = probe_name;
		return probe_name;
	}
	    else
	    {
		return  cached_probe_name_ht;
	    }
	}
	private Hashtable cached_probe_name_ht;
	
	// gene names mapped to vectors of spot indices
	// (because probes & therefore genes can occur more than once....)
	//
	public Hashtable getGeneNameHashtable()
	{
	    if(cached_gene_name_ht == null)
	    {
		Hashtable gene_name = new Hashtable();
		
		for(int gsearch=0; gsearch < n_spots; gsearch++)
		{
		    String[] gname = getGeneNames(gsearch);
		    if(gname != null)
		    {
			for(int g2=0; g2 < gname.length; g2++)
			{
			    if(gname[g2] != null)
			    {
				
				Vector indices = (Vector) gene_name.get( gname[g2] );
				if(indices == null)
				{
				    indices = new Vector();
				    gene_name.put(gname[g2], indices);
				}
				indices.addElement(new Integer(gsearch));
			    }
			}
		    }
		}
		cached_gene_name_ht = gene_name;
		return gene_name;
	    }
	    else
	    {
		return cached_gene_name_ht;
	    }
	}
	private Hashtable cached_gene_name_ht;


    } // end of data tags

    // ---------------- --------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- --------------- ------------- 
    // ---------------- --------------- --------------- --------------- 

    // --- new in 0.8.6  (22.03.01)
    //
    //  abilty to add spots
    // 
    //   lengthen all arrays:
    //     meas.data
    //     meas.spot_attr
    //     master datatags
    //
    //   update spot_to_index and id_to_spot maps
    //
    //  returns the actual spot_id's of the new spots
    //  or null if none created
    //

    public synchronized int[] addSpots(int how_many)
    {
	return addSpots(how_many, "NewSpot");
    }

    public synchronized int[] addSpots(int how_many, String prefix)
    {
	if(how_many < 1)
	    return null;

	int[] new_ids = new int[how_many];
	
	for(int s=0; s < how_many; s++)
	    new_ids[s] = n_spots + s;
	
	for(int m =0; m < n_measurements; m++)
	{
	    measurement[m].data = extendDoubleArray(measurement[m].data, how_many);

	    for(int s=0; s < how_many; s++)
		measurement[m].data[new_ids[s]] = Double.NaN;
	    
	    final int n_s_a = measurement[m].getNumSpotAttributes();
	    for(int sa=0; sa < n_s_a; sa++)
	    {
		Object old_data = measurement[m].getSpotAttributeData(sa);
		Object new_data = null;
		switch( measurement[m].getSpotAttributeDataTypeCode(sa) )
		{
		case Measurement.SpotAttributeIntDataType:
		    new_data = extendIntArray((int[]) old_data, how_many);
		    break;
		case Measurement.SpotAttributeDoubleDataType:
		    new_data = extendDoubleArray((double[]) old_data, how_many);
		    break;
		case Measurement.SpotAttributeCharDataType:
		    new_data = extendCharArray((char[]) old_data, how_many);
		    break;
		case Measurement.SpotAttributeTextDataType:
		    new_data = extendStringArray((String[]) old_data, how_many);
		    break;
		}
		measurement[m].setSpotAttributeData(sa, new_data);
	    } 
	}
	
	master_dtags.spot_name = extendStringArray( master_dtags.spot_name, how_many);
	master_dtags.probe_name = extendStringArray( master_dtags.probe_name, how_many);
	master_dtags.gene_names = extendStringArrayArray( master_dtags.gene_names, how_many);
	
	
	for(int s=0; s < how_many; s++)
	    master_dtags.spot_name[ new_ids[s] ] = master_dtags.getUniqueSpotName(prefix, new_ids[s]);
	
	current_spot_traversal = extendIntArray(current_spot_traversal, how_many);

	for(int s=0; s < how_many; s++)
	    current_spot_traversal[ new_ids[s] ] = new_ids[s];
	
	n_spots += how_many;
	
	inverse_spot_traversal = new int[n_spots];
	
	for(int g=0; g < n_spots; g++)
	{
	    inverse_spot_traversal[current_spot_traversal[g]] = g;
	}
       
	// sort out the cluster visibility arrays..
	countInVisibleClusters();

	master_dtags.buildSpotNameToIndex();
	//findLongestNames();

	generateDataUpdate(ElementsAdded);

	return new_ids;

    }


    private int[] extendIntArray(int[] in, int delta)
    {
	if(in == null)
	    return null;
	final int il = in.length;
	int[] out = new int[il + delta];
	for(int i=0; i < il; i++)
	    out[i] = in[i];
	return out;
	    
    }
    private double[] extendDoubleArray(double[] in, int delta)
    {
	if(in == null)
	    return null;
	final int il = in.length;
	double[] out = new double[il + delta];
	for(int i=0; i < il; i++)
	    out[i] = in[i];
	return out;
    }
    private String[] extendStringArray(String[] in, int delta)
    {
	if(in == null)
	    return null;
	final int il = in.length;
	String[] out = new String[il + delta];
	for(int i=0; i < il; i++)
	    out[i] = in[i];
	return out;
    }
    private char[] extendCharArray(char[] in, int delta)
    {
	if(in == null)
	    return null;
	final int il = in.length;
	char[] out = new char[il + delta];
	for(int i=0; i < il; i++)
	    out[i] = in[i];
	return out;
    }
    private String[][] extendStringArrayArray(String[][] in, int delta)
    {
	if(in == null)
	    return null;
	final int il = in.length;
	String[][] out = new String[il + delta][];
	for(int i=0; i < il; i++)
	    out[i] = in[i];
	return out;
    }

    // ---------------- --------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- --------------- ------------- 
    // ---------------- --------------- --------------- --------------- 
    //
    // --- new in version 0.8.0
    // 
    // TagAttrs store a set of key:value tuples for tag names (e.g. spot, probe or gene names)
    //
    // used to store things like gene accession codes, comments etc
    // also stores gene and probe data retrieved from a maxdSQL database
    //
    public class TagAttrs 
    {
	public TagAttrs()
	{
	    removeAllAttrs();
	}

	// returns the index of the named attr or -1 if not found
	public final int getAttrID(String the_attr_name)
	{
	    if(the_attr_name == null)
		return -1;
		
	    for(int a=0; a < attr_names.length; a++)
		if(attr_names[a].equals(the_attr_name))
		    return a;
	    return -1;
	}
	public final int addAttr(String the_attr_name)
	{
	    if(getAttrID(the_attr_name) == -1)
	    {
		// rebuild all of the arrays with an extra element

		attr_names = appendTo(attr_names, the_attr_name);
		
		java.util.Enumeration e = tag_to_attrs.keys();
		while(e.hasMoreElements())
		{
		    String key = (String) e.nextElement();
		    String[] da = (String[]) tag_to_attrs.get(key);
		    tag_to_attrs.put(key, appendTo(da, null));
		}
		
		//System.out.println("addAttr():" + the_attr_name + " added in index " + (attr_names.length-1));
	    }
	    return getAttrID(the_attr_name);
	}

	public final int getNumAttrs()
	{
	    return attr_names.length;
	}

	public final String getAttrName(int the_attr_id)
	{
	    return (the_attr_id < attr_names.length) ? attr_names[the_attr_id] : null;
	}
	public final String[] getAttrNames()
	{
	    return attr_names;
	}

	public final void removeAttr(String the_attr_name)
	{
	    int a_id = getAttrID(the_attr_name);
	    if(a_id >= 0)
		removeAttr(a_id);
	}
	public final void removeAttr(int the_attr_id)
	{
	    // need to shorten each of the String[]s in the tag hashtable
	    
	    attr_names = shortenStringArray( attr_names, the_attr_id );
	    java.util.Enumeration e = tag_to_attrs.keys();
	    while(e.hasMoreElements())
	    {
		String name = (String) e.nextElement();
		
		String[] tags = (String[]) tag_to_attrs.get(name);
		
		String[] new_tags = shortenStringArray( tags, the_attr_id);
		
		tag_to_attrs.put(name, new_tags);
	    }
	}

	// returns an array 1 element shorter than the input, with element index 'kill' missing 
	public  String[] shortenStringArray(final String[] in, final int kill)
	{
	    if(in == null)
		return null;

	    final int in_len = in.length;
	    if(in_len < kill)
		return in;
	    
	    String[] out = new String[in_len - 1];

	    int p = 0;
	    for(int i=0; i < in_len; i++)
		if(i != kill)
		    out[p++] = in[i];

	    return out;
	}

	public final void removeAllAttrs()
	{
	    tag_to_attrs = new Hashtable();
	    attr_names = new String[0];
	}

	public final String getTagAttr(String the_tag_name, int the_attr_id)
	{
	    String[] da = (the_tag_name == null) ? null : (String[]) tag_to_attrs.get(the_tag_name);
	    
	    //if(da == null)
	    //System.out.println("getTagAttr(): lookup " + the_tag_name + ":" + attr_names[the_attr_id] + " has no data");
	    //else
	    //System.out.println("getTagAttr(): lookup " + the_tag_name + ":" + attr_names[the_attr_id] + " has=" + da.length);
	    
	    return ((da == null) || (the_attr_id >= da.length)) ? null : da[the_attr_id];
	}
	
	public final String getTagAttr(String the_tag_name, String the_attr_name)
	{
	    int attr_id = getAttrID(the_attr_name);
	    return (attr_id < 0) ? null : getTagAttr(the_tag_name, attr_id);
	}

	// how many entries are there for this attr?
	// (used when checking whether 'safe' to delete the attr)
	public final int getTagAttrCount(int the_attr_id)
	{
	    int count = 0;
	    
	    java.util.Enumeration e = tag_to_attrs.elements();
	    while(e.hasMoreElements())
	    {
		String[] attrs = (String[]) e.nextElement();

		if((attrs != null) && (attrs.length > the_attr_id) && 
		   (attrs[the_attr_id] != null) && (attrs[the_attr_id].length() > 0) )
		    count++;
	    }
	    return count;
	}

	public final void setTagAttr(String the_tag_name, int the_attr_id, String the_val)
	{
	    // if(the_val != null)
	    {
		String[] da = (String[]) tag_to_attrs.get(the_tag_name);
		if(da == null)
		{
		    da = new String[ attr_names.length ];
		    tag_to_attrs.put(the_tag_name, da);
		}
		if(the_attr_id < da.length)
		{
		    da[the_attr_id] = the_val;
		    
		    // System.out.println("setTagAttr():" + the_tag_name + ": " + attr_names[the_attr_id] + "=" + the_val);
		}
	    }
	}
	
	public final void setTagAttr(String the_tag_name, String the_attr_name, String the_val)
	{
	    int attr_id = getAttrID(the_attr_name);
	    if(attr_id < 0)
		// this attr_name not recognised, create a new attr
		attr_id = addAttr(the_attr_name);

	    setTagAttr(the_tag_name, attr_id, the_val);

	}
	
	//
	// returns a hashtable containing the key:value tuples for the specified tag name
	//
	public final Hashtable getTagAttrs(String the_tag_name)
	{
	    Hashtable ht = new Hashtable();
	    String[] da = (String[]) tag_to_attrs.get(the_tag_name);
	    if(da != null)
	    {
		for(int a=0; a < da.length; a++)
		    if(da[a] != null)
			ht.put(attr_names[a], da[a]);
	    }
	    return ht;
	}

	//
	// adds an extra String to the end of a String[]
	//
	private final String[] appendTo(String[] ar, String s)
	{
	    String[] new_ar;
	    if(ar != null)
	    {
		new_ar = new String[ar.length + 1];
		for(int a=0; a < ar.length; a++)
		    new_ar[a] = ar[a];
		new_ar[ar.length] = s;
	    }
	    else
	    {
		new_ar = new String[1];
		new_ar[0] = s;
	    }
	    return new_ar;
	}

	private Hashtable tag_to_attrs;    // (String -> String[])
	private String[] attr_names;
    }

    private TagAttrs probe_ta;
    private TagAttrs gene_ta;
    private TagAttrs spot_ta;

    final public TagAttrs getProbeTagAttrs() {  return probe_ta; }
    final public TagAttrs getGeneTagAttrs()  {  return gene_ta; }
    final public TagAttrs getSpotTagAttrs()  {  return spot_ta; }

    public class NameTagSelection 
    {
	boolean s_name, p_name, g_names;
	boolean[] s_attrs;
	boolean[] p_attrs;
	boolean[] g_attrs;

	public NameTagSelection()
	{
	    s_attrs = new boolean[ spot_ta.getNumAttrs() ];
	    p_attrs = new boolean[ probe_ta.getNumAttrs() ];
	    g_attrs = new boolean[ gene_ta.getNumAttrs() ];
	}

	public final boolean isSpotName()  { return s_name; }
	public final boolean isProbeName() { return p_name; }
	public final boolean isGeneNames() { return g_names; }

	public final boolean isSpotNameAttr()  { return anyTrueIn(s_attrs); }
	public final boolean isProbeNameAttr() { return anyTrueIn(p_attrs); }
	public final boolean isGeneNamesAttr() { return anyTrueIn(g_attrs); }

	public final boolean isSpotNameOrAttr()  { return (s_name || anyTrueIn(s_attrs)); }
	public final boolean isProbeNameOrAttr() { return (p_name || anyTrueIn(p_attrs)); }
	public final boolean isGeneNamesOrAttr() { return (g_names || anyTrueIn(g_attrs)); }

	private final boolean anyTrueIn(final boolean[] ba)
	{
	    for(int b=0; b < ba.length; b++)
		if(ba[b])
		    return true;
	    return false;
	}

	public NameTagSelection copy()
	{
	    NameTagSelection new_nts = new NameTagSelection();
	    new_nts.s_name = s_name;
	    new_nts.p_name = p_name;
	    new_nts.g_names = g_names;
	    
	    new_nts.g_attrs = new boolean[g_attrs.length];
	    for(int a=0; a < g_attrs.length; a++)
		new_nts.g_attrs[a] = g_attrs[a];
	    new_nts.p_attrs = new boolean[p_attrs.length];
	    for(int a=0; a < p_attrs.length; a++)
		new_nts.p_attrs[a] = p_attrs[a];
	    new_nts.s_attrs = new boolean[s_attrs.length];
	    for(int a=0; a < s_attrs.length; a++)
		new_nts.s_attrs[a] = s_attrs[a];
	    
	    return new_nts;
	}

	public boolean equals( NameTagSelection nts )
	{
	    if(nts.s_name != s_name)
		return false;
	    if(nts.p_name != p_name)
		return false;
	    if(nts.g_names != g_names)
		return false;
	    if(nts.s_attrs.length != s_attrs.length)
		return false;
	    for(int a=0; a < s_attrs.length; a++)
		if(nts.s_attrs[a] != s_attrs[a])
		    return false;
	    if(nts.p_attrs.length != p_attrs.length)
		return false;
	    for(int a=0; a < p_attrs.length; a++)
		if(nts.p_attrs[a] != p_attrs[a])
		    return false;
	    if(nts.g_attrs.length != g_attrs.length)
		return false;
	    for(int a=0; a < g_attrs.length; a++)
		if(nts.g_attrs[a] != g_attrs[a])
		    return false;
	    
	    return true;
	}

	final public String getNames() // composite the names of the TagAttrs currently selected
	{
	  

	   StringBuffer res = new StringBuffer();
	   if(g_names)
	       res.append("Gene name(s) ");
	   for(int a=0; a < g_attrs.length; a++)
	       if(g_attrs[a])
		   res.append(gene_ta.getAttrName(a) + " ");
	   if(p_name)
	       res.append("Probe name ");
	   for(int a=0; a < p_attrs.length; a++)
	       if(p_attrs[a])
		   res.append(probe_ta.getAttrName(a) + " ");

	   if(s_name)
	       res.append("Spot name ");
	   for(int a=0; a < s_attrs.length; a++)
	       if(s_attrs[a])
		   res.append(spot_ta.getAttrName(a) + " ");

	   return (res.length() > 0) ? (res.toString().trim()) : null;
	}
	
	final public String[] getNamesArray() // composite the names of the TagAttrs currently selected
	{
	   Vector res = new Vector();
	   if(g_names)
	       res.addElement("Gene name(s) ");
	   for(int a=0; a < g_attrs.length; a++)
	       if(g_attrs[a])
		   res.addElement(gene_ta.getAttrName(a) + " ");
	   if(p_name)
	       res.addElement("Probe name ");
	   for(int a=0; a < p_attrs.length; a++)
	       if(p_attrs[a])
		   res.addElement(probe_ta.getAttrName(a) + " ");

	   if(s_name)
	       res.addElement("Spot name ");
	   for(int a=0; a < s_attrs.length; a++)
	       if(s_attrs[a])
		   res.addElement(spot_ta.getAttrName(a) + " ");

	   return (String[]) res.toArray(new String[0]);
	}

	final public String[] getAllNamesArray() // composite all Names and TagAttrs into an array
	{
	   Vector res = new Vector();
	   res.addElement("Gene name(s)");
	   for(int a=0; a < g_attrs.length; a++)
	       res.addElement(gene_ta.getAttrName(a));

	   res.addElement("Probe name");
	   for(int a=0; a < p_attrs.length; a++)
	       res.addElement(probe_ta.getAttrName(a));

	   res.addElement("Spot name");
	   for(int a=0; a < s_attrs.length; a++)
	       res.addElement(spot_ta.getAttrName(a));

	   return (String[]) res.toArray(new String[0]);
	}

	final public void setNames(String[] str) // sets selection based on a String[]
	{
	    if( ( str == null ) || ( str.length == 0 ) )
		return;

	    java.util.HashSet names = new java.util.HashSet();

	    for( int s=0; s < str.length; s++ )
		names.add( str[s].trim() );
	    
	    g_names = names.contains("Gene name(s)");
	    
	    for(int a=0; a < g_attrs.length; a++)
		g_attrs[a] = names.contains( gene_ta.getAttrName(a) );

	    p_name = names.contains("Probe name");
	    
	    for(int a=0; a < p_attrs.length; a++)
		p_attrs[a] = names.contains( probe_ta.getAttrName(a) );
	    
	    s_name = names.contains("Spot name");
	    
	    for(int a=0; a < s_attrs.length; a++)
		s_attrs[a] = names.contains( spot_ta.getAttrName(a) );   
	    
	}

	final public void setNames(String str) // sets selection based on str containg 0 or more names
	{
	    // System.out.println("NTS.setNames(): '" + str + "'");

	    g_names = (str.indexOf("Gene name(s)") >= 0);
	    
	    for(int a=0; a < g_attrs.length; a++)
		g_attrs[a] = (str.indexOf(gene_ta.getAttrName(a)) >= 0);

	    p_name = (str.indexOf("Probe name") >= 0);
	    
	    for(int a=0; a < p_attrs.length; a++)
		p_attrs[a] = (str.indexOf(probe_ta.getAttrName(a)) >= 0);

	    s_name = (str.indexOf("Spot name") >= 0);
	    
	    for(int a=0; a < s_attrs.length; a++)
		s_attrs[a] = (str.indexOf(spot_ta.getAttrName(a))  >= 0);   

	    // System.out.println("NTS.setNames(): set to '" + getNames() + "'");

	}

	final public void setNameTag(String value, int spot_id) // sets any selected names to 'value', no event generated
	{
	    final String[] gnames_a = getGeneNames(spot_id);

	    if(g_names)
	    {
		// it is not clear what should be done here because we dont
		// know which gene name to perform the operation on.

		// so, do nothing, and provide specific methods for gene name(s).

	    }

	    if(gnames_a != null)
	    {
		for(int a=0; a < g_attrs.length; a++)
		{
		    if(g_attrs[a])
		    {
			// dont actually want to set the tag for _all_ gene names
			// but which one do we want?
			//
			// so, do nothing, and provide specific methods for gene name(s).

		    }
		}
	    }
	    
	    if(p_name)
		master_dtags.setProbeName(spot_id, value);
	    String pname = getProbeName(spot_id);
	    if(pname != null)
	    {
		for(int a=0; a < p_attrs.length; a++)
		{
		    if(p_attrs[a])
		    {
			probe_ta.setTagAttr(pname, a, value); 
		    }
		}
	    }
	    
	    if(s_name) // do we really want to be able to do this?
		master_dtags.setSpotName(spot_id, value);

	    String sname = getSpotName(spot_id);
	    if(sname != null)
	    {
		for(int a=0; a < s_attrs.length; a++)
		{
		    if(s_attrs[a])
		    {
			spot_ta.setTagAttr(sname, a, value); 
		    }
		}
	    }
	}

	// specific methods for modifying gene name(s).

	// sets any selected attr names for 'gname' to 'value'
	// and/or changes gene name 'gname' or 'value', 
	// no event generated
	final public void setGeneNameTag(String gname, String value, int spot_id) 
	{
	    
	    if(g_names)
	    {
		if(gname == null)
		{
		    if( value != null )
		    {
			final String[] gnames_a = new String[1];
			gnames_a[0] = value;
			master_dtags.setGeneNames( spot_id, gnames_a );
		    }
		}
		else
		{
		    final String[] gnames_a = getGeneNames(spot_id);
		    
		    if(gnames_a != null)
		    {
			for(int g=0; g < gnames_a.length; g++)
			    if(gname.equals(gnames_a[g]))
				gnames_a[g] = value;
		    }
		}
	    }

	    for(int a=0; a < g_attrs.length; a++)
	    {
		if(g_attrs[a])
		{
		    if(gname != null)
		    {
			// System.out.println("setting '" + gname + "' attr id=" + a + " to "+ value);
			gene_ta.setTagAttr(gname, a, value);
		    }
		    else
		    {
			// set the tag for the first gene name, if one exists
			final String[] gnames_a = getGeneNames(spot_id);
			
			if( ( gnames_a != null ) && ( gnames_a.length > 0 ) )
			{
			    gene_ta.setTagAttr( gnames_a[0], a, value);
			}
		    }
		}
	    }
	}

	final public String[] getNameTagArray(int spot_id)
	{
	    return getNameTagArray(spot_id, false);
	}

	// the 'full' version will include nulls when then requested
	// value is not present
	//
	final public String[] getFullNameTagArray(int spot_id)
	{
	    return getNameTagArray(spot_id, true);
	}


	// 'full' mode fixed in 0.9.5/b3 so that nulls are returned in all cases
	// when no value is available
	//
	// repaired in 1.0.2 so that attrs can be retrieved without names being specified
	// 
	final private String[] getNameTagArray(int spot_id, boolean full)
	{
	    Vector strs = new Vector();
	    String[] gnames_a = getGeneNames(spot_id);
	    
	    if(g_names)
	    {
		if(gnames_a != null)
		{
		    for(int g=0; g < gnames_a.length; g++)
		    {
			if(gnames_a[g] != null)
			{
			    strs.addElement( gnames_a[g] );
			}
			else
			{
			    if(full)
				strs.addElement(null);
			}   
		    }
		}
		else
		{
		    if(full)
			strs.addElement(null);
		}
	    }

	    // now check attrs for each of the names....
	    
	    for(int a=0; a < g_attrs.length; a++)
	    {
		if(g_attrs[a])
		{
		    if(gnames_a != null)
		    {
			for(int g=0; g < gnames_a.length; g++)
			{
			    if(gnames_a[g] != null)
			    {
				String av = gene_ta.getTagAttr(gnames_a[g], a);
				if(full || (av != null))
				    strs.addElement(av);
			    }
			    else
			    {
				if(full)
				    strs.addElement(null);
			    }
			}
		    }
		    else
		    {
			if(full)
			    strs.addElement(null);
		    }
		}
	    }
	    
	    String pname_s = getProbeName(spot_id);
	    if(p_name)
	    {
		if(pname_s != null)
		{
		    strs.addElement(pname_s);
		}
		else
		{
		    if(full)
			strs.addElement(null);
		}
	    }
	    
	    for(int a=0; a < p_attrs.length; a++)
	    {
		if(p_attrs[a])
		{
		    if(pname_s != null)
		    {
			String av = probe_ta.getTagAttr(pname_s, a);
			if(full || (av != null))
			    strs.addElement(av);
		    }
		    else
		    {
			if(full)
			    strs.addElement(null);
		    }
		}
	    }


	    String sname_s = getSpotName(spot_id);
	    if(s_name)
	    {
		strs.addElement( sname_s );
	    }
	    
	    for(int a=0; a < s_attrs.length; a++)
	    {
		if(s_attrs[a])
		{
		    String av = spot_ta.getTagAttr(sname_s, a);

		    if(full || (av != null))
			strs.addElement(av);
		}
	    }


	    return (String[]) strs.toArray(new String[0]);
	}

	final private String[] OLD_getNameTagArray(int spot_id, boolean full)
	{
	    Vector strs = new Vector();
	    String[] gnames_a = getGeneNames(spot_id);
	    
	    if(gnames_a != null)
	    {
		for(int g=0; g < gnames_a.length; g++)
		{
		    if((g_names) && (gnames_a[g] != null))
			strs.addElement( gnames_a[g] );
		    
		    for(int a=0; a < g_attrs.length; a++)
		    {
			if(g_attrs[a])
			{
			    if(gnames_a[g] != null)
			    {
				String av = gene_ta.getTagAttr(gnames_a[g], a);
				if(full || (av != null))
				    strs.addElement(av);
			    }
			}
		    }
		}
	    }

	    String pname_s = getProbeName(spot_id);
	    if(pname_s != null)
	    {
		if(p_name)
		    strs.addElement(pname_s);
		for(int a=0; a < p_attrs.length; a++)
		{
		    if(p_attrs[a])
		    {
			String av = probe_ta.getTagAttr(pname_s, a);
			if(full || (av != null))
			    strs.addElement(av);
		    }
		}
	    }
	    
	    String sname_s = getSpotName(spot_id);
	    if(sname_s != null)
	    {
		if(s_name)
		    strs.addElement( sname_s );
		for(int a=0; a < s_attrs.length; a++)
		{
		    if(s_attrs[a])
		    {
			String av = spot_ta.getTagAttr(sname_s, a);
			if(full || (av != null))
			    strs.addElement(av);
		    }
		}
	    }

	    return (String[]) strs.toArray(new String[0]);
	}


	final public String getNameTag(int spot_id) // composite any selected names together
	{
	    StringBuffer res = new StringBuffer();
	    
	    String[] gnames_a = getGeneNames(spot_id);
	    
	    if(gnames_a != null)
	    {
		for(int g=0; g < gnames_a.length; g++)
		{
		    if((g_names) && (gnames_a[g] != null))
			res.append( gnames_a[g] + " " );
		    
		    for(int a=0; a < g_attrs.length; a++)
		    {
			if(g_attrs[a])
			{
			    if(gnames_a[g] != null)
			    {
				String av = gene_ta.getTagAttr(gnames_a[g], a);
				if(av != null)
				    res.append(av + " ");
			    }
			}
		    }
		}
	    }

	    String pname_s = getProbeName(spot_id);
	    if(pname_s != null)
	    {
		if(p_name)
		    res.append( pname_s + " " );
		for(int a=0; a < p_attrs.length; a++)
		{
		    if(p_attrs[a])
		    {
			String av = probe_ta.getTagAttr(pname_s, a);
			if(av != null)
			    res.append(av + " ");
		    }
		}
	    }
	    
	    String sname_s = getSpotName(spot_id);
	    if(sname_s != null)
	    {
		if(s_name)
		    res.append( sname_s + " " );
		for(int a=0; a < s_attrs.length; a++)
		{
		    if(s_attrs[a])
		    {
			String av = spot_ta.getTagAttr(sname_s, a);
			if(av != null)
			    res.append(av + " ");
		    }
		}
	    }

	    
	    return (res.length() > 0) ? (res.toString().trim()) : null;
	}
		    
	final public String[] getNameTags(int spot_id) // returns any selected names
	{
	    return null;
	}
	
	// specific version for getting the selected tag of the Nth gene of a spot
	//
	final public String getGeneNameTag(int spot_id, int gene_number)
	{
	    String[] gnames_a = getGeneNames(spot_id);
	    
	    if(gnames_a != null)
	    {
		if(gene_number >= gnames_a.length)
		    return null;

		if(g_names)
		    return gnames_a[ gene_number ];
		
		if(gnames_a[gene_number] != null)
		{
		    for(int a=0; a < g_attrs.length; a++)
		    {
			if(g_attrs[a])
			{
			    String av = gene_ta.getTagAttr(gnames_a[gene_number], a);
			    return av;
			}
		    }
		}
	    }
	    return null;
	}


	/*
	final public void setSingleSelection( String name )
	{
	    
	}
	*/

	final public void setSingleSelection( int name_t, int attr_i )
	{
	    // set everything else to off....

	    s_name = p_name = g_names = false;

	    for(int b=0; b <  g_attrs.length; b++)
		g_attrs[b] = false;

	    for(int b=0; b <  p_attrs.length; b++)
		p_attrs[b] = false;

	    for(int b=0; b <  s_attrs.length; b++)
		s_attrs[b] = false;

	    // then set the selected item to on....

	    if(name_t == 0)
	    {
		if(attr_i == -1)
		{
		    g_names = true;
		} 
		else
		{
		    g_attrs[attr_i] = true;
		}
	    }

	    if(name_t == 1)
	    {
		if(attr_i == -1)
		{
		    p_name = true;
		} 
		else
		{
		    p_attrs[attr_i] = true;
		}
	    }

	    if(name_t == 2)
	    {
		if(attr_i == -1)
		{
		    s_name = true;
		} 
		else
		{
		    s_attrs[attr_i] = true;
		}
	    }

	}

    }

    // ---------------- --------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- --------------- ------------- ------------

    private int n_measurements = 0;
    Measurement[] measurement = null;
    double[][] e_data = null;

    private int n_spots = 0;
    
    DataTags master_dtags;   // the complete set of Tags merged from all Measurements

    public final DataTags getMasterDataTags() { return master_dtags; }

    public final int getNumMeasurements()                     { return n_measurements; }
    public final int getNumSpots()                            { return n_spots; }


    public final int getNumClusters()                         
    { 
	if(cluster_root == null)
	    return 1;
	else
	    return cluster_root.recursivelyCountDescendants();
    }

    // ---------------- --------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- -------------
    //
    // accessing the data values
    //
    // ---------------- --------------- --------------- -------------
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- --------------- ------------- ------------
    //
    public double eValue(int m, int s) { return measurement[m].data[s]; }

    public void setEValue(int m, int s, double v)    
    {
	double old_v = measurement[m].data[s];
	measurement[m].data[s] = v;

	boolean min_max_changed = false;

	if( ! Double.isNaN( v ) && ! Double.isInfinite( v ) )
	{
	    if(v > max_e_value)
	    {
		max_e_value = v;
		min_max_changed = true;
	    }
	    if(v < min_e_value)
	    {
		min_e_value = v;
		min_max_changed = true;
	    }
	    
	    if(v > measurement[m].max_e_value)
	    {
		measurement[m].max_e_value = v;
		min_max_changed = true;
	    }
	    if(v < measurement[m].min_e_value)
	    {
		measurement[m].min_e_value = v;
		min_max_changed = true;
	    }
	}

	if(min_max_changed)
	    generateDataUpdate(RangeChanged);

	generateDataUpdate(ValuesChanged, s, m, old_v);
    }

    public double getMinEValue()                  { return min_e_value; } 
    public double getMaxEValue()                  { return max_e_value; } 

    public double getMeasurementMinEValue(int m)  
    { 
	try
	{
	    return measurement[m].min_e_value;
	}
	catch(ArrayIndexOutOfBoundsException aioobe)
	{
	    return Double.NaN;
	}
	catch(NullPointerException npe)
	{
	    return Double.NaN;
	}
    }

    public double getMeasurementMaxEValue(int m)  
    { 
	try
	{
	    return measurement[m].max_e_value; 
	}
	catch(ArrayIndexOutOfBoundsException aioobe)
	{
	    return Double.NaN;
	}
	catch(NullPointerException npe)
	{
	    return Double.NaN;
	}
    } 

    public double getMaxErrorValue()              { return max_error_value; }
    public double getMinErrorValue()              { return min_error_value; }

    private double min_e_value, max_e_value;          // global min and max
    private double min_error_value, max_error_value;  // for any sets of type ErrorDataType

    public void updateRanges()
    {
	System.out.println("rechecking ranges....");
	checkGlobalMinMax();
    }

    // assumes that the min_meas and max_meas values are correct
    //
    private void checkGlobalMinMax()
    {
	double old_min = min_e_value;
	double old_max = max_e_value;

	min_e_value = Double.MAX_VALUE;
	max_e_value = -Double.MAX_VALUE;

	double old_min_err = min_error_value;
	double old_max_err = max_error_value;

	min_error_value = Double.MAX_VALUE;
	max_error_value = -Double.MAX_VALUE;

	boolean min_max_changed = false;

	for(int m=0;m<n_measurements;m++)
	{
	    if(measurement[m].data_type == ErrorDataType)
	    {
		if(measurement[m].min_e_value < min_error_value)
		    min_error_value = measurement[m].min_e_value;
		if(measurement[m].max_e_value > max_error_value)
		    max_error_value = measurement[m].max_e_value;
	    }
	    else
	    {
		if(measurement[m].min_e_value < min_e_value)
		    min_e_value = measurement[m].min_e_value;
		if(measurement[m].max_e_value > max_e_value)
		    max_e_value = measurement[m].max_e_value;
	    }
	}

	if((old_min != min_e_value) || 
	   (old_max != max_e_value) || 
	   (old_min_err != min_error_value) || 
	   (old_max_err != max_error_value))
	{
	    //if((old_min != min_e_value) || (old_max != max_e_value))
	    //System.out.println(" >>>> min/max has changed, min = " + min_e_value + ", max = " + max_e_value);
	    //if((old_min_err != min_error_value) || (old_max_err != max_error_value))
	    //System.out.println(" >>>> err min/max has changed, min = " + min_error_value + ", max = " + max_error_value);
	    
	    //System.out.println("range has changed....");
	
	    generateDataUpdate(RangeChanged);
	}
	else
	{
	    //System.out.println("range has NOT changed....");
	    
	    //System.out.println(" >>>> min/max     is unchanged at min = " + min_e_value + ", max = " + max_e_value);
	    //System.out.println(" >>>> err min/max is unchanged at min = " + min_error_value + ", max = " + max_error_value);
	}
    }

    private void checkMeasurementMinMax(int m)
    {
	// check min/max

	double tmp_max = -Double.MAX_VALUE;
	double tmp_min = Double.MAX_VALUE;
	
	for(int s=0; s< n_spots; s++)
	{
	    double d = measurement[m].data[s];

	    if( ( d != Double.NaN )&& ( ! Double.isInfinite( d ) ) )
	    {
		if(d > tmp_max)
		    tmp_max = d;
		if(d < tmp_min)
		    tmp_min = d;
	    }
	}
	/*
	if((set_data_type[measurement] == ExpressionAbsoluteDataType) || 
	   (set_data_type[measurement] == ExpressionRatioDataType))
	*/
	{
	    measurement[m].min_e_value = tmp_min;
	    measurement[m].max_e_value = tmp_max;
	}

	// special treatment for sets of type ErrorDataType
	//
	/*
	if(measurement[m].data_type == ErrorDataType)
	{
	    if(tmp_min < min_error_value)
		min_error_value = tmp_min;
	    if(tmp_max > max_error_value)
		max_error_value = tmp_max;
	}
	*/

    }

    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    //
    //  iterator like interface for applying the same tranformation to
    //    multiple things
    //
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    
    public class ScalarFunc
    {
	public double eval(double in) { return in; }
    }

    public boolean visitAll(ScalarFunc sf)
    {
	for(int m=0;m<n_measurements;m++)
	{
	    double[] tmp = new double[n_spots];
	    for(int s=0;s < n_spots; s++)
	    {
		tmp[s] = sf.eval(measurement[m].data[s]);
	    }
	    setMeasurementData(m, tmp);
	}
	return true;
    }
    
    /**
     *   visit all elements of the named Measurement
     */
    public boolean visitAll(String name, ScalarFunc sf)
    {
	int m_id = getMeasurementFromName(name);
	if(m_id >= 0)
	{
	    double[] tmp = new double[n_spots];
	    for(int s=0;s < n_spots; s++)
	    {
		tmp[s] = sf.eval(measurement[m_id].data[s]);
	    }
	    setMeasurementData(m_id, tmp);

	    return true;
	}
	else
	{
	    reportAccessError("visitAll()", "Measurement '" + name + "' not found");
	    return false;
	}
    }
    
    public class ScalarRedFunc
    {
	public double eval(double red, double in) { return in; }
    }
    public class FindMin extends ScalarRedFunc
    {
	public double eval(double red, double in) { return ((in < red) ? in : red); }
    }
    public class FindMax extends ScalarRedFunc
    {
	public double eval(double red, double in) { return ((in > red) ? in : red); }
    }
    public class FindSum extends ScalarRedFunc
    {
	public double eval(double red, double in) { return (red + in); }
    }

    /**
     *   do a reduction over all elements 
     */
     public double visitAll(ScalarRedFunc srf)
    {
	return visitAll(srf, .0);
    }
    public double visitAll(ScalarRedFunc srf, double init)
    {
	double red = init;
	for(int m=0;m<n_measurements;m++)
	{
	    for(int s=0;s < n_spots; s++)
	    {
		red = srf.eval(red, measurement[m].data[s]);
	    }
	}
	return red;
    }
    /**
     *  do a reduction over all elements of the named Measurement
     */
    public double visitAll(String name, ScalarRedFunc srf)
    {
	return visitAll(name, srf, .0);
    }
    public double visitAll(String name, ScalarRedFunc srf, double init)
    {
	double red = init;
	int m_id = getMeasurementFromName(name);
	if(m_id >= 0)
	{
	    for(int s=0;s < n_spots; s++)
	    {
		red = srf.eval(red, measurement[m_id].data[s]);
	    }
	    return red;
	}
	else
	{
	    reportAccessError("visitAll()", "Measurement '" + name + "' not found");
	    return Double.NaN;
	}
    }
 
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    //
    // mapping names to vector indices
    //
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    
    // indexing is mainly done on spot names
    // 
    public int getSpotAtIndex(int index) {  return current_spot_traversal[index]; }

    // returns the actual index (i.e. the position in the source file or database)
    //
    public int getIndexOf(int spot)       {  return inverse_spot_traversal[spot]; }
    public int getIndexOfSpot(int spot)   {  return inverse_spot_traversal[spot]; }

    public double eValueAtIndex(int m, int index) { return measurement[m].data[current_spot_traversal[index]]; }
  
    // returns the actual index of a spot given the spot name
    //
    public int getIndexBySpotName(String name)
    {
	int result = -1;

	if(master_dtags.spot_name_to_index == null)
	{
	    //System.out.println("getIndexByName(): ODD: hash table is missing");
	    master_dtags.spot_name_to_index = new Hashtable();
	    
	    return -1;
	}

	Integer index = (Integer)master_dtags.spot_name_to_index.get(name);
	if(index != null)
	{
	    result = index.intValue();
	}

	return result;
    }

    /**
     * returns the first index of a gene name or -1 if not present
     *
    /*
    public int getFirstIndexOfGeneName(String name)
    {
	for(int s=0; s < n_spots; s++)
	{
	    if(gene_name[s].equals(name))
		return s;
	}
	return -1;
    }
    */

    public String[] getGeneNamesAtIndex(int index)         
    {
	try
	{
	    if(master_dtags.gene_names[current_spot_traversal[index]] == null)
	    {
		/*
		System.out.println("index " + index + " (real index " + 
				   current_spot_traversal[index] + ") has no gene name");
		*/
		return null;
	    }
	    return master_dtags.gene_names[current_spot_traversal[index]];
	}
	catch(ArrayIndexOutOfBoundsException aioobe)
	{
	    return null;
	}
	catch(NullPointerException npe)
	{
	    return null;
	}
    }
    public String getGeneNameAtIndex(int index)         
    {
	String[] res = getGeneNamesAtIndex(index);
	String comp = null;
	if(res != null && (res.length > 0))
	{
	    comp = res[0];
	    for(int g=1;g<res.length;g++)
		comp +=  (" " + res[g]);
	}
	return comp;
    }
   
    public String getProbeNameAtIndex(int index)         
    {
	try
	{
	    if(master_dtags.probe_name[current_spot_traversal[index]] == null)
	    {
		System.out.println("index " + index + " (real index " + 
				   current_spot_traversal[index] + ") has no probe name");
	    }
	    return master_dtags.probe_name[current_spot_traversal[index]];
	}
	catch(ArrayIndexOutOfBoundsException aioobe)
	{
	    return "[no data]";
	}
	catch(NullPointerException npe)
	{
	    return "[no data]";
	}
    }

    public String getSpotNameAtIndex(int index)         
    {
	try
	{
	    if(master_dtags.spot_name[current_spot_traversal[index]] == null)
	    {
		System.out.println("index " + index + " (real index " + 
				   current_spot_traversal[index] + ") has no spot name");
	    }
	    return master_dtags.spot_name[current_spot_traversal[index]];
	}
	catch(ArrayIndexOutOfBoundsException aioobe)
	{
	    return "[no data]";
	}
	catch(NullPointerException npe)
	{
	    return "[no data]";
	}
    
    }

    // NOTE: the name in these functions is the actual position of
    // the spot in the original file, not it's position using the
    // current traversal
    //
    //public   void setGeneName(int s, String name) { master_dtags.gene_name[s] = new String(name); }
    //public String getGeneName(int s)               { return (s < n_spots) ? master_dtags.gene_name[s] : "Unknown"; }
    
    // return all of the names for all spots
    // (convenience versions of the DataTags methods of the same name)
    public String[][] getGeneNames()               
    {
	return master_dtags.gene_names;
    }

    public String[] getProbeName()               
    {
	return master_dtags.probe_name;
    }

    public String[] getSpotName()               
    {
	return master_dtags.spot_name;
    }

    // return all of the names for this spot
    public String[] getGeneNames(int s)               
    {
	if(master_dtags.gene_names != null)
	{
	    return master_dtags.gene_names[s];
	}
	else
	    return null;
    }

    // return a composite name...
    public String getGeneName(int s)               
    { 
	final String no_val = "";

	if (s < n_spots)
	{
	    try
	    {
		if(master_dtags.gene_names == null)
		{
		    return no_val;
		}
		else
		{
		    String composite = null;
		    String[] gns = master_dtags.gene_names[s];
		    if((gns != null) && ( gns.length > 0))
		    {
			composite = gns[0];
			
			for(int g=1;g < gns.length; g++)
			    composite += (" " + gns[g]);
			
			return composite;
		    }
		    else
			return no_val;
		    
		}
	    }
	    catch(ArrayIndexOutOfBoundsException aioobe)
	    {
		return no_val;
	    }
	}
	else
	{
	    return no_val; 
	}
    }

    public String getProbeName(int s) { return master_dtags.getProbeName(s); }

    public String getSpotName(int s)  { return master_dtags.getSpotName(s); }

    // the set accessors are just wrappers around the DataTag methods
    // but also generate a NameChanged event
    //
    public void setSpotName(String[] sn)    
    { 
	master_dtags.setSpotName(sn);
	generateDataUpdate(NameChanged);
    }
    
    public void setProbeName(String[] pn)   
    { 
	master_dtags.setProbeName(pn);
	generateDataUpdate(NameChanged);
    }
    
    public void setGeneNames(String[][] gn) 
    { 
	master_dtags.setGeneNames(gn);
	generateDataUpdate(NameChanged);
    }
    
    public void setSpotName(int s, String sn)    
    { 
	master_dtags.setSpotName(s, sn);
	generateDataUpdate(NameChanged);
    }

    public void setProbeName(int s, String pn)   
    { 
	master_dtags.setProbeName(s, pn);
	generateDataUpdate(NameChanged);
    }
    
    public void setGeneNames(int s, String[] gn) 
    { 
	master_dtags.setGeneNames(s, gn);
	generateDataUpdate(NameChanged);
    }
    public void setGeneName(int s, int g, String gn) 
    { 
	master_dtags.setGeneName(s, g, gn);
	generateDataUpdate(NameChanged);
    }
    
    
    // probe names mapped to vectors of spot indices
    // (because probes can occur more thah once....)
    //
    public Hashtable getProbeNameHashtable()
    {
	return master_dtags.getProbeNameHashtable();
    }
    
    // gene names mapped to vectors of spot indices
    // (because probes & therefore genes can occur more than once....)
    //
    public Hashtable getGeneNameHashtable()
    {
	return master_dtags.getGeneNameHashtable();
    }

    // -------------------------------------------------------------------------------------------------
    //
    // names lengths
    //
    // old: measured in chars not pixels
    //
    // new: NOW stores the index of the longest name rather than the length itself
    //     or -1 if there are no names
    //
    //
    // DEPRECATED AS OF version 0.9.0
    //
    public int getLongestName()       { return ((master_dtags == null) ? 0 : master_dtags.longest_name); }
    public int getLongestNameLength() { return ((master_dtags == null) ? 0 : master_dtags.longest_len); }
    public int getLongestProbeName()  { return ((master_dtags == null) ? 0 : master_dtags.longest_probe_name); }
    public int getLongestGeneName()   { return ((master_dtags == null) ? 0 : master_dtags.longest_gene_name);  }
    public int getLongestSpotName()   { return ((master_dtags == null) ? 0 : master_dtags.longest_spot_name);  }

    public void findLongestNames()
    {
	master_dtags.longest_name = 0;
		
	master_dtags.longest_len = 0;

	master_dtags.longest_probe_name = master_dtags.longest_spot_name = master_dtags.longest_gene_name = 0;

	/*
	findLongestNames(GeneName);
	findLongestNames(ProbeName);
	findLongestNames(SpotName);
	*/

	//System.out.println("ExprData.findLongestNames()\n  longest id " + master_dtags.longest_name + 
	//		   " at " + master_dtags.longest_len + " chars");	    
    }

    public void findLongestNames(int name_type)
    {
	if(master_dtags == null)
	{
	    return;
	}

	int len = 0;

	switch(name_type)
	{
	case GeneName:

	    if(master_dtags.longest_gene_name == master_dtags.longest_name)
	    {
		// this spot used to be the longest name, it might not be any more...
		master_dtags.longest_name = -1;
		master_dtags.longest_len = 0;
	    }

	    if(master_dtags.gene_names != null)
	    {
		for(int s=0;s<n_spots;s++)
		{
		    String gn = getGeneName(s);  // why does this return null? its not suppoed to
		    int l = (gn == null ? 0 : gn.length());
		    if(l > len)
		    {
			master_dtags.longest_gene_name = s;
			len = l;
		    }
		}
	    }

	    // System.out.println("ExprData.findLongestNames()\n  longest gene name id " + master_dtags.longest_gene_name);	    

	    if(len > 0)
	    {
		int ll = getGeneName(master_dtags.longest_gene_name).length();
		if(ll > master_dtags.longest_len)
		{
		    master_dtags.longest_len = ll;
		    master_dtags.longest_name = master_dtags.longest_gene_name;
		}
	    }

	    break;

	case SpotName:
	    if(master_dtags.longest_spot_name == master_dtags.longest_name)
	    {
		// this spot used to be the longest name, it might not be any more...
		master_dtags.longest_name = -1;
		master_dtags.longest_len = 0;
	    }


	    if(master_dtags.spot_name != null)
		for(int s=0;s<n_spots;s++)
		{
		    int l = master_dtags.spot_name[s].length();
		    if(l > len)
		    {
			master_dtags.longest_spot_name = s;
			len = l;
		    }
		}
	    //System.out.println("ExprData.findLongestNames()\n  longest spot name id " + master_dtags.longest_spot_name);

	    if(len > 0)
	    {
		int ll = getSpotName(master_dtags.longest_spot_name).length();
		if(ll > master_dtags.longest_len)
		{
		    master_dtags.longest_len = ll;
		    master_dtags.longest_name = master_dtags.longest_spot_name;
		}
	    }
	    break;
	    
	case ProbeName:
	    if(master_dtags.longest_probe_name == master_dtags.longest_name)
	    {
		// this spot used to be the longest name, it might not be any more...
		master_dtags.longest_name = -1;
		master_dtags.longest_len = 0;
	    }

	    master_dtags.longest_probe_name = -1;
	    if(master_dtags.probe_name != null)
		for(int s=0;s<n_spots;s++)
		{
		    int l = master_dtags.probe_name[s] == null ? 0 : master_dtags.probe_name[s].length();
		    if(l > len)
		    {
			master_dtags.longest_probe_name = s;
			len = l;
		    }
		}
	    //System.out.println("ExprData.findLongestNames()\n  longest probe name id " + master_dtags.longest_probe_name);

	    if(len > 0)
	    {
		int ll = getProbeName(master_dtags.longest_probe_name).length();
		if(ll > master_dtags.longest_len)
		{
		    master_dtags.longest_len = ll;
		    master_dtags.longest_name = master_dtags.longest_probe_name;
		}
	    }

	    break;
	}
    }

    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    //
    // spot ordering
    //
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------

    public int[] getSpotOrder() { return getRowOrder(); }

    public boolean setSpotOrder(int[] new_order) { return setRowOrder(new_order); }


    public int[] getRowOrder() { return current_spot_traversal; }

    public boolean setRowOrder(int[] new_order)
    {
	if(new_order.length != n_spots)
	{
	    return false;
	}
	
	/*
	int limit = (n_spots > 100) ? 100 : n_spots;
	System.out.println("current traversal:");
	for(int g=0; g < limit; g++)
	{
	    System.out.print(current_spot_traversal[g] + ",");
	}
	System.out.println("\nnew traversal:");
	for(int g=0; g < limit; g++)
	{
	    System.out.print(new_order[g] + ",");
	}
	*/

	current_spot_traversal = new_order;

	// work out the inverse mapping
	//
	inverse_spot_traversal = new int[n_spots];

	for(int g=0; g < n_spots; g++)
	{
	    inverse_spot_traversal[current_spot_traversal[g]] = g;
	}
       
	generateDataUpdate(OrderChanged);

	return true;
    }

    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    //
    // Measurement accessor functions
    //
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    //

    // returns a list of all Measurements in the current traversal order
    //
    public String[] getMeasurementNames()
    {
	String[] mnl = new String[ n_measurements ];
	for(int m=0;m< n_measurements;m++)
	{
	    mnl[m] = getMeasurementName( current_meas_traversal[m] );
	}
	return mnl;
    }

    public Measurement getMeasurement(int m) 
    { 
	if(measurement == null)
	    return null;

	try
	{
	    return measurement[m];
	}
	catch(ArrayIndexOutOfBoundsException aioobe)
	{
	    return null;
	}
    }

    public int getMeasurementAtIndex(int ind) 
    { 
	return (ind < current_meas_traversal.length) ? current_meas_traversal[ind] : -1;
    }

    public int getIndexOfMeasurement(int m) 
    { 
	return (m < inverse_meas_traversal.length) ? inverse_meas_traversal[m] : -1;
    }

    public void setMeasurementName(int m, String name)   	
    {
	measurement[m].name = name;
	generateMeasurementUpdate(NameChanged, m);
    }
    public String getMeasurementName(int m)                	
    { 
	try
	{
	    return measurement[m].name;
	}
	catch(NullPointerException npe)
	{
	    return "";
	}
    }    

    public void setMeasurementDataType(int m, int dtype)    
    {
	try
	{
	    measurement[m].data_type = dtype;
	    
	    if(measurement[m].data_type == ErrorDataType)
	    {
		if(measurement[m].min_e_value < min_error_value)
		    min_error_value = measurement[m].min_e_value;
		if(measurement[m].max_e_value > max_error_value)
		    max_error_value = measurement[m].max_e_value;
	    }
	    // might affect the max expr or error values
	    //
	    checkGlobalMinMax();
	    
	    generateMeasurementUpdate(ValuesChanged, m);
	}
	catch(NullPointerException npe)
	{
	    reportAccessError("setMeasurementDataType", "index " + m + " out of range");
	}
    }

    public int getMeasurementDataType(int m)               
    {
	try
	{
	    return measurement[m].data_type; 
	}
	catch(NullPointerException npe)
	{
	    reportAccessError("getMeasurementDataType", "index " + m + " out of range");
	    return -1;
	}

    }

    public int getMeasurementFromName(String m_name)
    {
	for(int m=0;m<n_measurements;m++)
	{
	    if(measurement[m].name.equals(m_name))
		return m;
	}
	return -1;
    }

    public double[] getMeasurementData(String m_name) //throws java.rmi.RemoteException
    {
	int m = getMeasurementFromName(m_name);
	if(m>=0)
	    return getMeasurementData(m);
	else
	{
	    reportAccessError("getMeasurementData", "measurement '" + m_name + "' not found");
	    return null;
	}
    }

    public double[] getMeasurementData(int m) // throws java.rmi.RemoteException
    {
	try
	{
	    return measurement[m].data; 
	}
	catch(NullPointerException npe)
	{
	    reportAccessError("getMeasurementData", "index " + m + " out of range");
	    return null;
	}
    }

    // convenience function which returns a copy of the data values that have been passed through the current filter(s)
    //
    public double[] getFilteredMeasurementData(int m_id)
    {
	final double[] data = getMeasurementData(m_id);
	final int ns = getNumSpots();
	double[] vals;

	if( filterIsOn() )
	{
	    boolean[] filt = new boolean[ ns ];
	    int nsf = 0;
	    for(int s=0; s < ns; s++)
	    {
		if((filt[s] = filter(s)) == false)
		    nsf++;
	    }
	    vals = new double[nsf];
	    int vi = 0;
	    for(int s=0; s < ns; s++)
	    {
		if(!filt[s])
		    vals[vi++] = data[s];
	    }
	}
	else
	{
	    vals = new double[ns];
	    for(int s=0; s < ns; s++)
		vals[s] = data[s];
	}

	return vals;
    }


    public boolean setMeasurementData(String m_name, double[] data)
    {
	try
	{
	    int m = getMeasurementFromName(m_name);
	    if(m>=0)
		return setMeasurementData(m, data);
	    else
	    {
		reportAccessError("setSetData", "name not found");
		return false;
	    }
	}
	catch(NullPointerException npe)
	{
	    reportAccessError("setMeasurementData", "name " + m_name + " not known");
	    return false;
	}
    }

    // change all the sets data values in a single go
    //
    public boolean setMeasurementData(int m, double[] data)
    {
	if(data.length != n_spots)
	{
	    reportAccessError("setSetData", "vector is the wrong length (" + data.length + ")");
	    return false;
	}
	if(m >= n_measurements)
	{
	    reportAccessError("setSetData", "measurement index is too big (" + m + ")");
	    return false;
	}

	measurement[m].data = data;
	
	// check min/max
	//
	checkMeasurementMinMax(m);
	checkGlobalMinMax();

	generateDataUpdate(ValuesChanged);

	return true;
    }

    public boolean getMeasurementShow(int m) 
    {
	try
	{
	    return measurement[m].show; 
	}
	catch(NullPointerException npe)
	{
	    reportAccessError("setMeasurementShow", "index " + m + " out of range");
	    return false;
	}
    }
    
    public void setMeasurementShow(int m, boolean show) 
    { 
	try
	{
	    measurement[m].show = show; 
	    generateMeasurementUpdate(VisibilityChanged, m);
	}
	catch(NullPointerException npe)
	{
	    reportAccessError("setMeasurementShow", "index " + m + " out of range");
	    return;
	}
    }

    // new in 0.9.4

    public final void showAllMeasurements()
    {
	for(int m=0; m < n_measurements; m++)
	{
	    measurement[ m ].show = true; 
	}
	generateMeasurementUpdate(VisibilityChanged);
    }
    
    public final void hideSelectedMeasurements()
    {
	if(measurement_selection.size() > 0)
	{
	    java.util.Enumeration e = measurement_selection.keys();
	    while(e.hasMoreElements())
	    {
		Integer mi = (Integer) e.nextElement();
		measurement[ mi.intValue() ].show = false; 
	    }
	    generateMeasurementUpdate(VisibilityChanged);
	}
    }

    public final void hideUnselectedMeasurements()
    {
	for(int m=0; m < n_measurements; m++)
	{
	    if( measurement_selection.get( new Integer( m ) ) == null )
	    {
		measurement[ m ].show = false; 
	    }
	}
	generateMeasurementUpdate(VisibilityChanged);
    }

   // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    //
    // several methods for adding new Measurements
    //
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    //
    /**
     *  adds a new Measurement in which the data array contains elements
     *  in the same order as the existing Measurement
     */
    public boolean addOrderedMeasurement(Measurement new_meas)
    {
	if(new_meas.data.length != n_spots)
	{
	    reportAccessError("addOrderedMeasurement", "data vector is the wrong length (is " + new_meas.data.length + ", should be " + n_spots +")");
	    return false;
	}

	makeMeasurementNameUnique(new_meas);

	// need to grow the Measurement array
	//
	Measurement[] new_meas_a = new Measurement[n_measurements+1];

	for(int m=0; m< n_measurements; m++)
	{
	    new_meas_a[m] = measurement[m];
	}
	// add the new measurement at the end
	//
	new_meas_a[n_measurements] = new_meas;

	measurement =  new_meas_a;
	
	// probe_names, gene_name and spot_name do not change...

	// the gene_name_to_index hash table does not need to be updated
	// as no new gene names have been added

	// rebuild the measurement traversal order to include the new Measurement at the end

	int[] new_current_meas_traversal  = new int[n_measurements+1];
	for(int m=0; m < n_measurements; m++)
	{
	    new_current_meas_traversal[m] = current_meas_traversal[m];
	}
	new_current_meas_traversal[n_measurements] = n_measurements;

	current_meas_traversal = new_current_meas_traversal;

	inverse_meas_traversal = new int[n_measurements+1];
	for(int m=0; m < n_measurements+1; m++)
	    inverse_meas_traversal[current_meas_traversal[m]] = m;

	checkMeasurementMinMax(n_measurements);

	n_measurements++;

	checkGlobalMinMax();

	generateMeasurementUpdate(ElementsAdded, n_measurements-1);

	return true;
    }

    /**
     *   are there any null's in the array of names?
     */
    private boolean namesAreAllPresent(String[] test_names)
    {
	boolean legal = true;
	for(int n=0; n < test_names.length; n++)
	{
	    // names must not be null
	    if(test_names[n] == null)
		legal = false;
	}
	return legal;
    }

    /**
     *   do the proposed new spot_name and probe_name tuples match those
     *   already in the data?
     *
     *   returns true if 'test' DataTags is a subset of 'master_dtags'
     *
     *   also true if all test.spot_names are present in master_dtags
     *                and test.probe_names is null
     */
    private boolean spotAndProbeNamesMatch(DataTags test)
    {
	boolean match = true;
	for(int n=0; n < test.spot_name.length; n++)
	{
	    int i = getIndexBySpotName(test.spot_name[n]);
	    if(i >= 0)
	    {
		if(test.probe_name != null)
		    if(!(master_dtags.probe_name[i].equals(test.probe_name[n])))
			return false;
	    }
	    else
		return false;
	}
	return match;
    }

    /**
     *   returns count of number of duplicated names in the input array
     */
    private int countDuplicatedNames(String[] test_names)
    {
	Integer count = null;
	Hashtable name_ht = new Hashtable();
	for(int n=0; n < test_names.length; n++)
	{
	    if((count = (Integer) name_ht.get(test_names[n])) != null)
	    {
		int ci = count.intValue();
		name_ht.put(test_names[n], new Integer(ci+1));
	    }
	    else
	    {
		name_ht.put(test_names[n], new Integer(1));
	    }
	}
	int unique_names = name_ht.size();
	return test_names.length - unique_names;
    }

    /**
     *   replace any existing data with a new Measurement
     *   and updates all global values
     */
    public boolean addFirstMeasurement(Measurement new_meas) 
    {
	measurement = new Measurement[1];
	n_measurements = 1;
	measurement[0] = new_meas;
	
	current_meas_traversal  = new int[1];
	current_meas_traversal[0] = 0;
	inverse_meas_traversal  = new int[1];
	inverse_meas_traversal[0] = 0;
	
	master_dtags = new DataTags();

	master_dtags.spot_name    = new_meas.dtags.spot_name;
	master_dtags.probe_name   = new_meas.dtags.probe_name;
	master_dtags.gene_names   = new_meas.dtags.gene_names;

	n_spots = new_meas.data.length;  

	// build the spot_name_to_index hash table
	//
	master_dtags.buildSpotNameToIndex();

	// and the traversal (and it's inverse) orders
	//
	current_spot_traversal = new int[n_spots];
	inverse_spot_traversal = new int[n_spots];
	for(int s=0; s< n_spots; s++)
	{
	    current_spot_traversal[s] = s;
	    inverse_spot_traversal[current_spot_traversal[s]] = s;
	}

	//System.out.println("first measurement installed, name is  " + measurement[0].name);
	//findLongestNames();
	checkMeasurementMinMax(0);
	checkGlobalMinMax();

	// there might be exsiting clusters which refer to this
	// newly loaded data
	//
	// countInVisibleClusters();
	updateClusterToSpotMappings();
	
	generateDataUpdate(ElementsAdded);		
	generateMeasurementUpdate(ElementsAdded, 0);	

	return true;
    }
    
    /**
     *  is the set name unique?
     *    if not, add a unique suffix to it
     */
    public boolean makeMeasurementNameUnique(Measurement new_m)
    {
	boolean name_used = true;
	String unique_name = new_m.getName();
	int name_index = 1;
	
	if((unique_name == null) || (unique_name.length() == 0))
	{
	    unique_name = "(no name)";
	    new_m.setName(unique_name);
	}

	while(name_used == true)
	{
	    name_used = false;
	    for(int m=0; m< n_measurements; m++)
	    {
		if(measurement[m].getName().equals(unique_name))
		{
		    name_used = true;
		    name_index++;
		    unique_name = new String(new_m.getName() + "(" + name_index + ")");
		}
	    }
	    new_m.setName(unique_name);
	}
	return true;
    }

    /**
     *  merge a Measurement with the existing ones
     *  by creating new (Spot,Probe) lines in 'master_dtags' as neccessary
     *
     *  all other data points are filled with NaNs
     *
     */ 
    public boolean mergeMeasurement(Measurement new_meas) 
    {
	Measurement[] ma = new Measurement[1];
	ma[0] = new_meas;
	return mergeMeasurements(ma);
    }

    // ======================================================================================
    // =========================== start of merging GUI stuff ===============================
    // ======================================================================================

    /**
     *  merges one or more Measurement with the existing ones
     *
     *  by creating new (Spot,Probe) lines in 'master_dtags' as neccessary
     *
     *  all other data points are filled with NaNs
     */ 

    private JRadioButton[][] ignore_opt = null;
    private JRadioButton[]   mode_opt = null;
    private int[]            names_matched = null;

    private final static boolean debug_merge = true; //false;

    private boolean actuallyMergeMeasurements(final Measurement[] new_meas)
    {
	//if(debug_merge)
	    System.out.println("merge starts, there are presently " + n_spots + " spots....");

	
	if(new_meas == null)
	    return true;

	int name_mode = 0;

	boolean discard_unrecognised = false;

	if(mode_opt != null)
	{
	    for(int m=0; m < 3; m++)
	    {
		if(mode_opt[m].isSelected())
		{
		    name_mode = m;
		    
		    if((ignore_opt[m][1] != null) && (ignore_opt[m][1].isSelected()))
			discard_unrecognised = true;
		}
	    }
	}
	else // 'auto' mode which is "matching on Spot names and adding unrecongised spots"
	{
	    name_mode = 0;
	    discard_unrecognised = false;
	}

	if(debug_merge)
	    System.out.println(" name mode is : " + name_mode);

	// build a name->spot id map

	Hashtable name_to_spot_id = new Hashtable();
	
	switch(name_mode)
	{
	case 0:
	    for(int s=0; s< n_spots; s++)
	    {
		Vector spot_list = new Vector();
		spot_list.addElement(new Integer(s));
		name_to_spot_id.put(master_dtags.spot_name[s], spot_list);
	    }
	    break;
	case 1:
	    for(int s=0; s < n_spots; s++)
	    {
		Vector spot_list = (Vector) name_to_spot_id.get(master_dtags.probe_name[s]);
		if(spot_list == null)
		{
		    spot_list = new Vector();
		    name_to_spot_id.put(master_dtags.probe_name[s], spot_list);
		}
		spot_list.addElement(new Integer(s));
	    }
	    break;
	case 2:
	    if(master_dtags.gene_names != null)
	    {
		for(int s=0; s < n_spots; s++)
		{
		    String[] gn = master_dtags.gene_names[s];
		    if(gn != null)
		    {
			for(int g=0; g < gn.length; g++)
			{
			    Vector spot_list = (Vector) name_to_spot_id.get(gn[g]);
			    if(spot_list == null)
			    {
				spot_list = new Vector();
				name_to_spot_id.put(gn[g], spot_list);
			    }
			    spot_list.addElement(new Integer(s));
			}
		    }
		}
	    }
	    break;
	}


	int new_spots = new_meas[0].data.length - names_matched[name_mode];

	if(debug_merge)
	    System.out.println(" new data has " + new_meas[0].data.length + " spots");

	if(debug_merge)
	    System.out.println(" " + names_matched[name_mode] + " names are thought to have matched");

	if(debug_merge)
	    System.out.println(" expecting to add " + new_spots + " spots");

	//
	// use the name->spot id map to build an array of destination 
	// spot id's for the new data
	///
	// -1 means no existing spot matches, and a new row should be used
	//
	//
	// name_to_spot_id contains a vector of spot id's for each of the
	// names which might match. 
	//
	// for each of the names in the input:
	//  
	//  1. get the vector of spot id's for the name
	//  2. if ( vector.size > 0 )
	//        REMOVE the spot id from the head of the vector,
	//           and store it in the destination_spot array
	//     otherwise
	//           set the destination_spot to -1 for this spot
	//  

	int no_match_count = 0;
	
	int[] destination_spot = new int[ new_meas[0].data.length ];

	for(int s=0; s < new_meas[0].data.length; s++)
	{
	    Vector spot_list = null;

	    switch(name_mode)
	    {
	    case 0:
		spot_list = (Vector) name_to_spot_id.get( new_meas[0].dtags.spot_name[s] );
		break;
	    case 1:
		spot_list = (Vector) name_to_spot_id.get( new_meas[0].dtags.probe_name[s] );
		break;
	    case 2:
		String[] gn = new_meas[0].dtags.gene_names[s];
		spot_list = null;
		for(int gni=0; gni < gn.length; gni++)
		{
		    if((spot_list == null) || (spot_list.size() == 0))
			spot_list = (Vector) name_to_spot_id.get( gn[gni] );
		}
		break;
	    }

	    destination_spot[s] = -1;
	    if(spot_list != null)
	    {
		if(spot_list.size() > 0)
		{
		    destination_spot[s] = ((Integer) spot_list.elementAt(0)).intValue();
		    spot_list.removeElementAt(0);
		}
	    }
	    if(destination_spot[s] == -1)
		no_match_count++;
	}

	if(debug_merge)
	    System.out.println(no_match_count + " have no match with current spots");
	
	new_spots = no_match_count;

	if(discard_unrecognised)
	    new_spots = 0;

	int new_len = n_spots + new_spots;
		
	//
	// build new data tag arrays (for spot_, probe_ and gene_ names)
	// which are long enough to hold all of the existing spots
	// and the ones that will be added
	// 
	//

	DataTags new_dtags = null;

	if(new_spots > 0)
	{
	    if(debug_merge)
		System.out.println("rebuilding data tags to be for " + new_len + " spots");

	    new_dtags = new DataTags();

	    new_dtags.spot_name    = new String[new_len];
	    new_dtags.probe_name   = new String[new_len];

	    for(int s=0; s < n_spots; s++)
	    {
		new_dtags.spot_name[s]    = master_dtags.spot_name[s];
		new_dtags.probe_name[s]   = master_dtags.probe_name[s];
	    }

	    // gene names are optional
	    if( master_dtags.gene_names != null )
	    {
		new_dtags.gene_names = new String[new_len][];

		for(int s=0; s < n_spots; s++)
		{
		    new_dtags.gene_names[s] = master_dtags.gene_names[s];
		}
	    }
	    else
	    {
		if( new_meas[0].dtags.gene_names != null )
		{
		    // existing data doesn't have gene names. but new data does...
		    new_dtags.gene_names = new String[new_len][];
		}
	    }
	}
	else
	{
	    // we aren't adding any spots so the existing data tags will be fine

	    if(debug_merge)
		System.out.println("reusing old data tags (new new spots added)");

	    new_dtags = master_dtags;
	}


	int first_new_row = n_spots;
		
	//
	// now visit the new data tags and insert them into place....
	//
	if(new_spots > 0)
	{
	    if(debug_merge)
		System.out.println("inserting " + new_spots + " new data tag elements");
		
	    System.out.println("spot name array length=" +  new_dtags.spot_name.length);

	    int debug_count = 0;

	    for(int s=0; s < new_meas[0].data.length; s++)
	    {
		if(destination_spot[s] == -1)
		{
		    if(debug_merge)
			debug_count++;

		    //System.out.println("number " + ((first_new_row - n_spots) + 1));

		    // not already known, insert at end of existing names...
		    new_dtags.spot_name[first_new_row]  = new_meas[0].dtags.spot_name[s];
		    new_dtags.probe_name[first_new_row] = new_meas[0].dtags.probe_name[s];
		    
		    if( new_meas[0].dtags.gene_names != null)
			new_dtags.gene_names[first_new_row] = new_meas[0].dtags.gene_names[s];
		    
		    first_new_row++;
		}
	    }

	    if(debug_merge)
		System.out.println("  " + debug_count + " tags inserted");

	}

	
	//
	// grow the existing data vectors to accomodate the new spots
	//
	if(new_spots > 0)
	{
	    for(int m=0;m<n_measurements;m++)
	    {
		if(debug_merge)
		    System.out.println("growing data vector for existing Measurement " + measurement[m].getName());

		double[] new_data = new double[new_len];
		for(int s=0; s < n_spots; s++)
		{
		    new_data[s] = measurement[m].data[s];
		}
		for(int s=0; s < new_spots; s++)
		{
		    new_data[n_spots+s] = Double.NaN;
		}
		measurement[m].data = new_data;

		//
		// likewise for any spot attribute vectors
		
		// PROBLEM:: what to use as the default value
		//           for spots which do not have the measurement?
		//
		//  double -> NaN
		//  char   -> '\0' ?
		//  string -> null ?
		//  int    -> .... ?
		//

		for(int sa=0; sa < measurement[m].getNumSpotAttributes(); sa++)
		{
		    Object new_sa_data = null;
		    Object sa_data = (Object) measurement[m].getSpotAttributeData(sa);

		    int type_code = measurement[m].getSpotAttributeDataTypeCode(sa);
		    switch(type_code)
		    {
		    case Measurement.SpotAttributeIntDataType:
			new_sa_data = new int[new_len];
			break;
		    case Measurement.SpotAttributeDoubleDataType:
			new_sa_data = new double[new_len];
			break;
		    case Measurement.SpotAttributeCharDataType:
			new_sa_data = new char[new_len];
			break;
		    case Measurement.SpotAttributeTextDataType:
			new_sa_data = new String[new_len];
			break;
		    }

		    // copy existing data
		    //
		    try
		    {
			for(int s=0; s < n_spots; s++)
			{
			    Array.set(new_sa_data, s, Array.get(sa_data,s));
			}
		    }
		    catch(IllegalArgumentException npe) 
		    { } 
		    catch(ArrayIndexOutOfBoundsException iae) 
		    { } 

		    // and fill blanks with some appropriate NaN-like value
		    switch(type_code)
		    {
		    case Measurement.SpotAttributeIntDataType:
			int[] ivec = (int[]) new_sa_data;
			for(int s=0; s < new_spots; s++)
			    ivec[n_spots+s] = 0;
			break;
		    case Measurement.SpotAttributeDoubleDataType:
			double[] dvec = (double[]) new_sa_data;
			for(int s=0; s < new_spots; s++)
			    dvec[n_spots+s] = Double.NaN;
			break;
		    case Measurement.SpotAttributeCharDataType:
			char[] cvec = (char[]) new_sa_data;
			for(int s=0; s < new_spots; s++)
			    cvec[n_spots+s] = '\0';
			break;
		    case Measurement.SpotAttributeTextDataType:
			String[] tvec = (String[]) new_sa_data;
			for(int s=0; s < new_spots; s++)
			    tvec[n_spots+s] = null;
			break;
		    }

		    // and store this new array in the data
		    measurement[m].setSpotAttributeData(sa, new_sa_data);

		}
	    }
	    System.gc();
	}

	int old_n_spots = n_spots;
	    
	// and install the new master data tags
	//
	if(new_spots > 0)
	{
	    
	    master_dtags = new_dtags;
	    n_spots = new_len;
	    
	    // then build the spot_name_to_index hash table
	    master_dtags.spot_name_to_index = new Hashtable();
	    for(int s=0; s < n_spots; s++)
	    {
		master_dtags.spot_name_to_index.put(master_dtags.spot_name[s], new Integer(s));
	    }
	    
	    // and the traversal (and it's inverse) order
	    int[] new_current_spot_traversal  = new int[n_spots];
	    int[] new_inverse_spot_traversal  = new int[n_spots];
	    for(int s=0; s < old_n_spots; s++)
	    {
		new_current_spot_traversal[s]               = current_spot_traversal[s];
		new_inverse_spot_traversal[new_current_spot_traversal[s]] = inverse_spot_traversal[current_spot_traversal[s]];
	    }
	    for(int s=0; s < new_spots; s++)
	    {
		new_current_spot_traversal[old_n_spots + s]               = old_n_spots + s;
		new_inverse_spot_traversal[new_current_spot_traversal[old_n_spots + s]] = old_n_spots + s;
	    }
	    current_spot_traversal = new_current_spot_traversal;
	    inverse_spot_traversal          = new_inverse_spot_traversal;
	}

	generateDataUpdate(ElementsAdded);

	//
	// now create new Measurements (using the merged data tags) from the 
	// input Measurements
	//
	for(int nm = 0; nm < new_meas.length; nm++)
	{
	    if(debug_merge)
		System.out.println("rebuilding " + new_meas[nm].getName() + " for insert");

	    double[] new_data = new double[new_len];

	    for(int s=0; s < new_len; s++)
	    {
		new_data[s] = Double.NaN;
	    }
	    
	    first_new_row = old_n_spots;

	    for(int s=0; s < new_meas[nm].data.length; s++)
	    {
		if(destination_spot[s] == -1)
		{
		    if(!discard_unrecognised)
			new_data[first_new_row++] = new_meas[nm].data[s];
		}
		else
		{
		    new_data[destination_spot[s]] = new_meas[nm].data[s];
		}
	    }

	    new_meas[nm].dtags = null;         // new_dtags;
	    new_meas[nm].data  = new_data;
	    
	    
	    //
	    // and do the same for any Spot Attributes which are part of this new Measurement
	    //
	    for(int sa=0; sa < new_meas[nm].getNumSpotAttributes(); sa++)
	    {
		if(debug_merge)
		  System.out.println("   & rebuilding SpotAttr " + new_meas[nm].getSpotAttributeName(sa) + " for insert");

		Object new_sa_data = null;
		Object sa_data = new_meas[nm].getSpotAttributeData(sa);
		
		int type_code = new_meas[nm].getSpotAttributeDataTypeCode(sa);
		switch(type_code)
		{
		case Measurement.SpotAttributeIntDataType:
		    int[] ivec = new int[new_len];
		    for(int s=0; s < new_len; s++)
			ivec[s] = 0;
		    new_sa_data = ivec;
		    break;
		case Measurement.SpotAttributeDoubleDataType:
		    double[] dvec = new double[new_len];
		    for(int s=0; s < new_len; s++)
			dvec[s] = Double.NaN;
		    new_sa_data = dvec;
		    break;
		case Measurement.SpotAttributeCharDataType:
		    char[] cvec = new char[new_len];
		    for(int s=0; s < new_len; s++)
			cvec[s] = '\0';
		    new_sa_data = cvec;
		    break;
		case Measurement.SpotAttributeTextDataType:
		    String[] tvec = new String[new_len];
		    for(int s=0; s < new_len; s++)
			tvec[s] = null;
		    new_sa_data = tvec;
		    break;
		}

		first_new_row = old_n_spots;

		try
		{
		    for(int s=0; s < new_meas[nm].data.length; s++)
		    {
			if(destination_spot[s] == -1)
			{
			    if(!discard_unrecognised)
				Array.set(new_sa_data, first_new_row++, Array.get(sa_data, s));
			}
			else
			{
			    Array.set(new_sa_data, destination_spot[s], Array.get(sa_data, s));
			}
		    }
		}
		catch(IllegalArgumentException npe) 
		{ } 
		catch(ArrayIndexOutOfBoundsException iae) 
		{ } 

		new_meas[nm].setSpotAttributeData(sa, new_sa_data);
		
	    }

	    //
	    // now the data tags, the data and the spot attributes have been resized,
	    // this measurement can be added using addOrderedMeasurement()
	    //

	    if(debug_merge)
		System.out.println( "inserting " + new_meas[nm].getName() );
	    
	    addOrderedMeasurement(new_meas[nm]);
	}

	// findLongestNames();
	
	if(debug_merge)
	    System.out.println("updating clusters....");
	
	// reset the in_visible_clusters array
	//
	updateClusterToSpotMappings();

	if(debug_merge)
	    System.out.println("updating range....");

	// check whether ranges have changed
	//
	checkMeasurementMinMax(n_measurements-1);
	checkGlobalMinMax();
	
	if(debug_merge)
	    System.out.println("generating event....");

	// notify everybody that new names have been added
	//
	generateMeasurementUpdate(ElementsAdded, n_measurements-1);

	if(debug_merge)
	    System.out.println("done....");
	
	return true;
    }

    /**
     *  analyses the new and existing data tags, offers the user a choice
     *  of merge methods then calls actuallyMergeMeasurements()
     *
     */ 

    private boolean merge_done;
    private boolean merge_cancelled;

    public boolean mergeMeasurements(final Measurement[] new_meas) 
    {
	merge_done = false;
	merge_cancelled = false;
	    
	System.out.println("mergeMeasurements() starting thread...");

	new MeasurementMergerThread(new_meas).start();

	// need to spin until the merge is either OK'ed or Cancel'ed

	/*
	while((!merge_done) && (!merge_cancelled))
	{
	    try
	    {
		Thread.sleep(250);
		System.out.println("mergeMeasurements() spinning...");
	    }
	    catch(java.lang.InterruptedException ie)
	    {
		
	    }
	}
	*/

	if(merge_done)
	    return true;
	else
	    return false;
    }

    public class MeasurementMergerThread extends Thread
    {
	private Measurement[] new_meas;

	public MeasurementMergerThread(final Measurement[] new_meas_) 
	{
	    new_meas = new_meas_;
	}

	public void run()
	{
	    
	    if((new_meas == null) || (new_meas.length == 0))
	    {
		merge_done = true;
		return; // true;
	    }
	    
	    // just in case the user is a bit dippy
	    if(n_measurements == 0)
	    {
		addMeasurement(new_meas[0]);
		for(int nm=1; nm < new_meas.length; nm++)
		    addOrderedMeasurement(new_meas[nm]);
		merge_done = true;
		return; //true;
	    }	    
	    
	    System.out.println("mergeMeasurements() it's a real merge....");
	    
	    
	    // first try to match on Spot names...
	    // which are guaranteed to be unique
	    int spots_matched = 0;
	    
	    for(int s=0; s < new_meas[0].dtags.spot_name.length; s++)
	    {
		if(getIndexBySpotName(new_meas[0].dtags.spot_name[s]) >= 0)
		{
		    spots_matched++;
		}
	    }
	    
	    //
	    // build a hashtable mapping existing probe names -> spot id(s)
	    //
	    
	    Hashtable pnames = new Hashtable();
	    
	    if( master_dtags.probe_name != null )
	    {
		for(int s=0; s < n_spots; s++)
		{
		    if( master_dtags.probe_name[s] != null )
		    {
			Vector spot_list = (Vector) pnames.get( master_dtags.probe_name[s] );
			if(spot_list == null)
			{
			    spot_list = new Vector();
			    pnames.put(master_dtags.probe_name[s], spot_list);
			}
			spot_list.addElement(new Integer(s));
		    }
		}
	    }
	    
		
	    int probes_matched = 0;
	    int probes_known_but_not_matched = 0;
    
	    if( new_meas[0].dtags.probe_name != null )
	    {
		for(int s=0; s < new_meas[0].dtags.probe_name.length; s++)
		{
		    if( new_meas[0].dtags.probe_name[s] != null )
		    {
			Vector spot_list = (Vector) pnames.get( new_meas[0].dtags.probe_name[s] );
			if(spot_list != null)
			{
			    if(spot_list.size() > 0)
			    {
				probes_matched++;
				spot_list.removeElementAt(0);
			    }
			    else
			    {
				probes_known_but_not_matched++;
			    }
			}
		    }
		}
	    }
    
	    int genes_matched = 0;
	    int genes_known_but_not_matched = 0;
    
	    //
	    // build a hashtable mapping existing gene names -> spot id(s)
	    //
	    Hashtable gnames = new Hashtable();
	    
	    if(master_dtags.gene_names != null)
	    {
		for(int s=0; s < n_spots; s++)
		{
		    String[] gn = master_dtags.gene_names[s];

		    if(gn != null)
		    {
			for(int gni=0; gni < gn.length; gni++)
			{
			    if( gn[ gni ] != null )
			    {
				Vector spot_list = (Vector) gnames.get( gn[ gni ] );

				if(spot_list == null)
				{
				    spot_list = new Vector();
				    gnames.put( gn[ gni ], spot_list );
				}

				spot_list.addElement( new Integer(s) );
			    }
			}
		    }
		}
	    }
	    // now match the new gene names against this...
	    //
	    // TODO::
	    //
	    // what if a new spot has multiple gene names, and
	    // they match differing spot id's in the existing data?
	    // currently just pick the first match
	    //
	    if(new_meas[0].dtags.gene_names != null)
	    {
		for(int s=0; s < new_meas[0].dtags.gene_names.length; s++)
		{
		    String[] gn = new_meas[0].dtags.gene_names[s];

		    if(gn != null)
		    {
			boolean matched = false;
			for(int gni=0; gni < gn.length; gni++)
			{
			    // only needs to match once...
			    if(!matched)
			    {
				if( gn[ gni ] != null )
				{
				    Vector spot_list = (Vector) gnames.get( gn[ gni ] );
				    if(spot_list != null)
				    {
					if(spot_list.size() > 0)
					{
					    genes_matched++;
					    matched = true;
					    spot_list.removeElementAt(0);
					}
					else
					{
					    genes_known_but_not_matched++;
					}
				    }
				}
			    }
			}
		    }
		}
	    }
    
	    System.out.println("mergeMeasurements() tags matched....");
    
	    // offer johnny user a choice...
	    
	    int new_spot  = new_meas[0].dtags.spot_name.length - spots_matched;
	    int new_probe = new_meas[0].dtags.spot_name.length - probes_matched;
	    int new_gene  = new_meas[0].dtags.spot_name.length - genes_matched;
	    
	    names_matched = new int[3];
	    names_matched[0] = spots_matched;
	    names_matched[1] = probes_matched;
	    names_matched[2] = genes_matched;
	    
	    // this is bad..... except for here, no GUI stuff is included in ExprData
	    //
	    // [should | how can] this be fixed?
	    //
    
	    final JFrame frame = new JFrame("Choose merging method");
	    JPanel panel = new JPanel();
	    panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	    GridBagLayout gridbag = new GridBagLayout();
	    panel.setLayout(gridbag);
    
	    ButtonGroup bg = new ButtonGroup();
    
	    JRadioButton jrb = null;
	    GridBagConstraints c = null;
	    String msg = null;
	    JLabel label = null;
    
	    int opt = 0;
    
    
	    ignore_opt = new JRadioButton[3][];
	    mode_opt = new JRadioButton[3];
    
	    ignore_opt[0] = new JRadioButton[2];
	    mode_opt[0] = addMergeChoice("Spot", spots_matched, new_spot, 0, 
					 gridbag, panel, bg, 
					 opt, ignore_opt[0]);
	    opt += 5;
    
	    ignore_opt[1] = new JRadioButton[2];
	    mode_opt[1] = addMergeChoice("Probe", probes_matched, new_probe, probes_known_but_not_matched, 
					 gridbag, panel, bg, 
					 opt, ignore_opt[1]);
	    opt += 5;
    
	    ignore_opt[2] = new JRadioButton[2];
	    mode_opt[2] = addMergeChoice("Gene", genes_matched, new_gene, genes_known_but_not_matched, 
					 gridbag, panel, bg, 
					 opt, ignore_opt[2]);
	    opt += 5;
    
	    updateMergeOptions();
    
	    JPanel butpan = new JPanel();
	    butpan.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 0));
	    
	    JButton jb = new JButton("Merge");
	    butpan.add(jb);
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e)
		    {
			if(checkMergeOptionsAreGood())
			{
			    actuallyMergeMeasurements(new_meas);
			    merge_done = true;
			    frame.setVisible(false);
			}
			else
			{
			    Toolkit.getDefaultToolkit().beep();
			}
		    }
		});
	    
	    jb = new JButton("Help");
	    butpan.add(jb);
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e)
		    {
			// how do we call help?????
		    }
		});
    
	    jb = new JButton("Cancel");
	    butpan.add(jb);
	    jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e)
		    {
			frame.setVisible(false);
			merge_cancelled = true;
		    }
		});
    
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = opt++;
	    //c.weighty = c.weightx = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(butpan,c);
	    panel.add(butpan);
	    
	    System.out.println("mergeMeasurements() open dialog box....");
    
	    frame.getContentPane().add(panel);
	    frame.pack();
	    //mview.locateWindowAtCenter(frame);
	    frame.setVisible(true);
    
	    
	    
	    //return true;
	}
    }

    private JRadioButton addMergeChoice(String name, int matched, int new_spots, int known_but_not_matched,
					GridBagLayout gridbag, JPanel panel, ButtonGroup bg, 
					int line, JRadioButton opt_jrb[])
    {
	JRadioButton jrb = null;
	GridBagConstraints c = null;
	String msg = null;
	int lline = line;

	if(new_spots > 0)
	{

	    jrb = new JRadioButton("Match by " + name + " names");
	    bg.add(jrb);
	    jrb.addActionListener(new ActionListener()  { public void actionPerformed(ActionEvent e)  { updateMergeOptions(); } });
	    
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = lline++;
	    //c.weighty = c.weightx = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(jrb,c);
	    panel.add(jrb);

	    JLabel label = new JLabel("  (" + matched + (matched == 1 ? " name" : " names") + " can be matched)");
	    c.gridx = 0;
	    c.gridy = lline++;
	    //c.weighty = c.weightx = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(label,c);
	    panel.add(label);
	    
	    if(known_but_not_matched > 0)
	    {
		label = new JLabel("  (" +  known_but_not_matched + (known_but_not_matched == 1 ? " name" : " names") + " are known but cannot be matched)");
		c.gridx = 0;
		c.gridy = lline++;
		//c.weighty = c.weightx = 1.0;
		c.fill = GridBagConstraints.BOTH;
		gridbag.setConstraints(label,c);
		panel.add(label);
	    }

	    ButtonGroup ibg = new ButtonGroup();

	    JPanel inner = new JPanel();
	    inner.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 10));
	    GridBagLayout innerbag = new GridBagLayout();
	    inner.setLayout(innerbag);
	    
	    msg = "generate " + new_spots + " new Spot";
	    if(new_spots > 1)
		msg += "s";

	    opt_jrb[0] = new JRadioButton(msg);
	    ibg.add(opt_jrb[0]);
	    //opt_jrb[0].setEnabled(false);
	    opt_jrb[0].addActionListener(new ActionListener()  { public void actionPerformed(ActionEvent e)  { updateMergeOptions(); } });

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    //c.weighty = c.weightx = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    innerbag.setConstraints(opt_jrb[0],c);
	    inner.add(opt_jrb[0]);
	    
	    msg = "ignore " + new_spots + " unmatched Spot";
	    if(new_spots > 1)
		msg += "s";

	    opt_jrb[1] = new JRadioButton(msg);
	    ibg.add(opt_jrb[1]);
	    //opt_jrb[1].setEnabled(false);
	    opt_jrb[1].addActionListener(new ActionListener()  { public void actionPerformed(ActionEvent e)  { updateMergeOptions(); } });

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    //c.weighty = c.weightx = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    innerbag.setConstraints(opt_jrb[1],c);
	    inner.add(opt_jrb[1]);
	    
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = lline++;
	    //c.weighty = c.weightx = 1.0;
	    //c.fill = GridBagConstraints.BOTH;
	    c.anchor = GridBagConstraints.WEST;
	    gridbag.setConstraints(inner,c);
	    panel.add(inner);
	}
	else
	{
	    msg = "Match by " + name + " names";

	    jrb = new JRadioButton(msg);
	    bg.add(jrb);
	    
	    jrb.addActionListener(new ActionListener()  { public void actionPerformed(ActionEvent e)  { updateMergeOptions(); } });

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = lline++;
	    //c.weighty = c.weightx = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(jrb,c);
	    panel.add(jrb);

	    JPanel inner = new JPanel();
	    inner.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 20));
	    GridBagLayout innerbag = new GridBagLayout();
	    inner.setLayout(innerbag);
	    
	    JLabel label = new JLabel("  (all names were recognised)");
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    //c.weighty = c.weightx = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    innerbag.setConstraints(label,c);
	    inner.add(label);

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = lline++;
	    c.anchor = GridBagConstraints.WEST;
	    gridbag.setConstraints(inner,c);
	    panel.add(inner);

	    opt_jrb[0] = opt_jrb[1] = null;

	}
	return jrb;
    }

    private boolean checkMergeOptionsAreGood()
    {
	boolean name_valid = false;
	boolean mode_valid = false;
	
	for(int m=0; m < 3; m++)
	{
	    if(mode_opt[m].isSelected())
	    {
		name_valid = true;
		if(ignore_opt[m][0] == null)
		{
		    mode_valid = true;
		}
		else
		{
		    if(ignore_opt[m][0].isSelected() || ignore_opt[m][1].isSelected())
			mode_valid = true;
		}
	    }
	}
	return ((name_valid == true) && (mode_valid == true));
    }

    private void updateMergeOptions()
    {
	for(int m=0; m < 3; m++)
	{
	    if(ignore_opt[m][0] != null)
	    {
		ignore_opt[m][0].setEnabled(mode_opt[m].isSelected());
		ignore_opt[m][1].setEnabled(mode_opt[m].isSelected());
	    }
	}
    }

    // ======================================================================================
    // ============================ end of merging GUI stuff ================================
    // ======================================================================================

   /**
     * 	 <P>Adds a new Measurement, specifed with a new set of (spot, probe) tuples</P>
     *	
     * 	 <P>ArrayType.ID and ArrayType.Name are checked, if they match then the (spot,probe)
     * 	 tuples are checked, and if they match then the new_data in inserted
     * 	 into the correct rows</P>
     * 	 
     * 	 <P>This is expected to be used when results from two similar, but not
     * 	 identical array chips are to be compared.</P>
     */

    public boolean addMeasurement(Measurement new_meas) 
    {
	boolean exact_fit = false;

	//System.out.println("addMeasurement(): attempting to add " + new_meas);

	if((new_meas.dtags.spot_name  == null) &&
	   (new_meas.dtags.probe_name == null) &&
	   (new_meas.dtags.gene_names == null) &&
	   (new_meas.data.length == n_spots))
	{
	    // special case, assume data in exactly the same order as the
	    // existing rows
	    //
	    exact_fit = true;
	}
	else
	{
	    // sanity checks

	    // there _must_ be an array of spot names
	    //
	    if(new_meas.dtags.spot_name == null)
	    {
		reportAccessError("addMeasurement", "new Measurement has no spot names");
		return false;
	    }
	    // spot names _must_ be unique
	    //
	    if(countDuplicatedNames(new_meas.dtags.spot_name) > 0)
	    {
		reportAccessError("addMeasurement", "spot names are not unique in new Measurement");
		return false;
	    }
	    // and must all be present
	    //
	    if(new_meas.dtags.spot_name.length != new_meas.data.length)
	    {
		reportAccessError("addMeasurement", 
				  "spot name vector length  is not the same as data vector length ");
		return false;
	    }

	    // if probe names are supplied, they must all be present
	    // (really needed?)
	    if((new_meas.dtags.probe_name != null) && (new_meas.dtags.probe_name.length != new_meas.data.length))
	    {
		reportAccessError("addMeasurement", 
				  "probe name vector length is not the same as data vector length ");
		return false;
	    }
	    
	}

	makeMeasurementNameUnique(new_meas);

	// check for special cases of adding Measurements
	//
	if(n_measurements == 0)
	{
	    // System.out.println("addMeasurement(): adding first");

	    // this is the first Measurement to be added
	    return addFirstMeasurement(new_meas);
	}
	else
	{
	    if(exact_fit)
	    {
		// System.out.println("addMeasurement(): adding ordered");
		    
		return addOrderedMeasurement(new_meas);   
	    }
	    else
	    {
		if(spotAndProbeNamesMatch(new_meas.dtags) == false)
		{
		    // System.out.println("addMeasurement(): merging");

		    // the new Measurement contains (spot,probe) lines not already
		    // in existing measurements
		    //
		    return mergeMeasurement(new_meas);
		}
		else
		{
		    // System.out.println("addMeasurement(): adding with NaNs");

		    // to make sure the data elements are in the same order as those already
		    // here, and that any missing values are correctly set to NaN,
		    // build a new Measurement with a new data array and new Spot Attrs arrays

		    // use the actuallyMergeMeasurement method
		    // set for matching on 'Spot Names' in 'create new spots' mode
		    //
		    // (clearing the mode_opt radio button array does this)
		    //
		    mode_opt = null;
		    
		    // also store the fact that all spot names match
		    names_matched = new int[1];
		    names_matched[0] = new_meas.data.length;

		    Measurement[] new_m_a = new Measurement[1];
		    new_m_a[0] = new_meas;
		    return actuallyMergeMeasurements(new_m_a);

		    /*
		    // copy all of the attributes into a new Measurement
		    Measurement ordered_meas = new_meas.cloneMeasurement();
		    
		    // reset the data and data tags in the clone
		    // (in case any values are missing in the new data)
		    ordered_meas.data = new double[n_spots];
		    ordered_meas.dtags = null;
		    for(int s=0;s < n_spots; s++)
		    {
			ordered_meas.data[s] = Double.NaN;
		    }
		    
		    for(int sa=0; sa < ordered_meas.getNumSpotAttributes(); sa++)
		    {
			Object new_sa_data = null;
			Object sa_data = (Array) ordered_meas.getSpotAttributeData(sa);
			
			int type_code = ordered_meas.getSpotAttributeDataTypeCode(sa);
			switch(type_code)
			{
			case Measurement.SpotAttributeIntDataType:
			    new_sa_data = new int[new_len];
			    break;
			case Measurement.SpotAttributeDoubleDataType:
			    new_sa_data = new double[new_len];
			    break;
			case Measurement.SpotAttributeCharDataType:
			    new_sa_data = new char[new_len];
			    break;
			case Measurement.SpotAttributeTextDataType:
			    new_sa_data = new String[new_len];
			    break;
			}
		    }

		    // re-order the data values
		    for(int s=0;s < new_meas.data.length; s++)
		    {
			int ordered_i = getIndexBySpotName(new_meas.dtags.spot_name[s]);
			ordered_meas.data[ordered_i] = new_meas.data[s];
		    }
		    
		    // and insert
		    return addOrderedMeasurement(ordered_meas);
		    */
		}
	    }
	}
    }

    public boolean removeAllMeasurements()
    {
	boolean result = removeAllMeasurements(true);
	if(result)
	    generateMeasurementUpdate(ElementsRemoved);
	return result;

    }

    public boolean removeAllMeasurements(boolean remove_tag_attrs)
    {
	clearSpotSelection();
	clearMeasurementSelection();
	clearClusterSelection();

	n_measurements = 0;
	n_spots = 0;
	measurement = null;
	master_dtags = new DataTags();

	if(remove_tag_attrs)
	{
	    probe_ta = new TagAttrs();
	    gene_ta = new TagAttrs();
	    spot_ta = new TagAttrs();
	}

	
	current_meas_traversal = new int[0];
	inverse_meas_traversal = new int[0];

	current_spot_traversal = new int[0];
	inverse_spot_traversal = new int[0];

	updateClusterToMeasurementMappings();

	return true;
    }

    public boolean removeMeasurement(String name)
    {
	int m_to_kill = getMeasurementFromName(name);
	return removeMeasurement(m_to_kill);
    }

    public boolean removeMeasurement(int m_to_kill)
    {
	if(m_to_kill >= 0)
	{
	   if(n_measurements == 1)
	   {
	       // this is the only one measurement left
	       //
	       removeAllMeasurements( true );

	       /*
	       n_measurements = 0;
	       n_spots = 0;
	       measurement = null;
	       master_dtags = new DataTags();

	       current_meas_traversal = new int[0];
	       inverse_meas_traversal = new int[0];

	       current_spot_traversal = new int[0];
	       inverse_spot_traversal = new int[0];

	       updateClusterToMeasurementMappings();
	       */
	   }
	   else
	   {
	       
	       boolean recompute_min_max = false;
	       
	       // the min/max values might change as a result of removing this Measurement
	       //
	       if((max_e_value == measurement[m_to_kill].max_e_value) || 
		  (min_e_value == measurement[m_to_kill].min_e_value)) 
	       {
		   recompute_min_max = true;
	       }
	       

	       // copy all but one of the Measurements into a new array
	       //
	       Measurement[] new_measurement = new Measurement[n_measurements-1];
	       
	       int dest_m = 0;
	       for(int m=0;m< n_measurements; m++)
	       {
		   if(m != m_to_kill)
		   {
		      new_measurement[dest_m++] = measurement[m];
		   }
	       }
	       
	       // rebuild the traversal order
	       int[] new_current_meas_traversal = new int[n_measurements-1];
	       dest_m = 0;
	       for(int m=0;m< n_measurements; m++)
	       {
		   if(current_meas_traversal[m] != m_to_kill)
		   {
		       int cmt = current_meas_traversal[m];
		       
		       // adjust the index of any Measurement after the deleted one
		       //
		       if(cmt > m_to_kill)
			   cmt--;

		       new_current_meas_traversal[dest_m++] = cmt;
		       
		   }
	       }
	       current_meas_traversal = new_current_meas_traversal;

	       /*
	       for(int t=0; t < current_meas_traversal.length; t++)
		   System.out.println(t + "->" + current_meas_traversal[t] + "  ");
	       */

	       // and the inverse traversal order
	       inverse_meas_traversal = new int[n_measurements-1];
	       for(int m=0; m < n_measurements-1; m++)
		   inverse_meas_traversal[current_meas_traversal[m]] = m;

	       /*
	       for(int t=0; t < current_meas_traversal.length; t++)
		   System.out.println(t + "<-" + inverse_meas_traversal[t] + "  ");
	       */

	       // install the new array
	       measurement = new_measurement;
	       
	       n_measurements--;
	       
	       if(recompute_min_max)
	       {
		   checkGlobalMinMax();
	       }
	   
	       updateClusterToMeasurementMappings();
	   }
	   
	   System.gc();

	   generateMeasurementUpdate(ElementsRemoved);
	   
	   return true;
	}
	else
	    return false;
	
    }

    public static final int ExpressionAbsoluteDataType = 0;
    public static final int ExpressionRatioDataType    = 1;
    public static final int ProbabilityDataType        = 2;
    public static final int ErrorDataType              = 3;
    public static final int UnknownDataType            = 4;

    // apply a new ordering to the Measurements
    //
    public void setMeasurementOrder(int[] new_order)
    {
	if(new_order.length != n_measurements)
	{
	    reportAccessError("setMeasurementOrder", "new Measurement ordering is the wrong length");
	    return;
	}

	//Measurement[] new_measurement = new Measurement[n_measurements];

	/*
	for(int m=0;m< n_measurements; m++)
	{
	    new_measurement[m] = measurement[new_order[m]];
	}
	*/
	current_meas_traversal = new_order;

	inverse_meas_traversal = new int[new_order.length];

	for(int m=0; m <  n_measurements; m++)
	    inverse_meas_traversal[current_meas_traversal[m]] = m;

	//measurement = new_measurement;

	generateMeasurementUpdate(OrderChanged);
    }

    public int[] getMeasurementOrder()
    {
	return current_meas_traversal;
    }

    private void reportAccessError(String method_name, String error)
    {
	JOptionPane.showMessageDialog(null, 
				      method_name + "\nError: " + error,
				      "Data access error", 
				      JOptionPane.ERROR_MESSAGE); 
    }

    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    //
    // filtering....
    //
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------

    //private double[] set_filter_value = null;

    public boolean filterIsOn() 
    {
	return (external_filter_enabled_count > 0);
    }

    public int getEnabledFilterCount()
    {
	return external_filter_enabled_count;
    }

    // ---------------- --------------- --------------- ------------- ------------

    // should this spot (NOT index) be displayed given the current filtering parameters?
    //
    // (i.e. return TRUE for spots which match the filter and are 'killed',
    //          and FALSE for spots which pass through the filter and are visible)
    //
    public boolean filter(int spot)
    { 
	boolean result = false;

	if((spot >=n_spots) || (spot < 0))
	    return true;

	
	// check any filters which are currently enabled
	//
	// cascade the AND logic so that once any filter kills the spot,
	// no other filters are applied....
	//

	for(int fc=0; fc < external_filter.size(); fc++)
	{
	    Filter f = (Filter)external_filter.elementAt(fc);
	    if(f.enabled())
	    {
		if(f.filter(spot) == true)
		    return true; // short-circuit the rest of the serach
	    }
	}

	return result;
    }
    
    // ---------------- --------------- --------------- ------------- ------------
    //
    //  pluggable filter interface
    //

    Vector external_filter = new Vector();

    private int external_filter_enabled_count = 0;

    public interface Filter
    {
	
	// should this spot (NOT index) be displayed given the current filtering parameters?
	// (i.e. return TRUE for spots which pass through the filter,
	//          and FALSE for spots which match the filter)
	//
	public boolean filter(int spot); 

	public boolean enabled();  // is it currently enabled?

	public String  getName(); // descriptive name of this filter
    }

    public int getNumFilters()
    {
	return external_filter.size();
    }

    public boolean addFilter(Filter f)
    {
	external_filter.addElement(f);
	if(f.enabled())
	{
	    external_filter_enabled_count++;
	    generateDataUpdate(SizeChanged);
	}
	//System.out.println("filter " + f.getName() + " added");
	return true;
    }

    public boolean removeFilter(Filter f)
    {
	external_filter.removeElement(f);
	if(f.enabled())
	{
	    external_filter_enabled_count--;
	    generateDataUpdate(SizeChanged);
	}
	//System.out.println("filter " + f.getName() + " removed");
	return true;
    }

    // should be called by a filter when it has changed
    //
    public boolean notifyFilterChanged(Filter f)
    {
	// cant do it incrementally because we dont
	// know what the state was before the change happened
	// (should be ok anyway as there wont be too many filters)
	//
	external_filter_enabled_count = 0;
	for(int fc=0; fc < external_filter.size(); fc++)
	    if(((Filter)external_filter.elementAt(fc)).enabled())
		external_filter_enabled_count++;
	
	generateDataUpdate(SizeChanged);
	return true;
    }

    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    //
    // Selections (general)
    //
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------

    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    //
    // A Selection of Clusters.....
    //
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------

    public final void setClusterSelected(Cluster cluster, boolean is_selected)
    {
	setClusterSelected(cluster, is_selected, true);
    }

    public final void setClusterSelected(Cluster cluster, boolean is_selected, boolean notify)
    {
	if(is_selected)
	    cluster_selection.put( cluster, cluster );
	else
	    cluster_selection.remove( cluster );

	if(notify)
	    notifySelectionListeners(ClusterSelection);
    }

    public final boolean isClusterSelected(Cluster cluster)
    {
	return (cluster_selection.get( cluster ) != null);
    }
    
    public final void clearClusterSelection()
    {
	cluster_selection.clear();
    }

    public final void unselectSpotClusters()
    {
	java.util.Enumeration e = cluster_selection.keys();
	while(e.hasMoreElements())
	{
	    Cluster cl = (Cluster) e.nextElement();
	    if(cl.getIsSpot())
		cluster_selection.remove(cl);
	}
    }

    public final void unselectMeasurementClusters()
    {
	java.util.Enumeration e = cluster_selection.keys();
	while(e.hasMoreElements())
	{
	    Cluster cl = (Cluster) e.nextElement();
	    if(!cl.getIsSpot())
		cluster_selection.remove(cl);
	}
    }


    
    public final void convertClusterSelectionToSpotSelection()
    {
	java.util.Enumeration e = cluster_selection.keys();
	int total = 0;
	Vector parts = new Vector();
	while(e.hasMoreElements())
	{
	    Cluster cl = (Cluster) e.nextElement();
	    if(cl.getIsSpot())
	    {
		int[] s_ids = cl.getAllClusterElements();
		total +=  s_ids.length;
		parts.addElement(s_ids);
	    }
	}
	if(total > 0)
	{
	    int pos = 0;
	    int[] sum = new int[total];
	    for(int p=0; p < parts.size(); p++)
	    {
		int[] s_ids = (int[]) parts.elementAt(p);
		for(int s=0; s < s_ids.length; s++)
		    sum[pos++] = s_ids[s];
	    }
	    setSpotSelection(sum);
	}
    }

    public final void convertClusterSelectionToMeasurementSelection()
    {
	java.util.Enumeration e = cluster_selection.keys();
	int total = 0;
	Vector parts = new Vector();
	while(e.hasMoreElements())
	{
	    Cluster cl = (Cluster) e.nextElement();
	    if(!cl.getIsSpot())
	    {
		int[] m_ids = cl.getAllClusterElements();
		total +=  m_ids.length;
		parts.addElement(m_ids);
	    }
	}
	if(total > 0)
	{
	    int pos = 0;
	    int[] sum = new int[total];
	    for(int p=0; p < parts.size(); p++)
	    {
		int[] m_ids = (int[]) parts.elementAt(p);
		for(int m=0; m < m_ids.length; m++)
		    sum[pos++] = m_ids[m];
	    }
	    setMeasurementSelection(sum);
	}

	 
    }

    public final Cluster[] getClusterSelection()
    {
	final int ss = cluster_selection.size();
	int s = 0;
	Cluster[] sel = new Cluster[ ss ];
	
	java.util.Enumeration e = cluster_selection.keys();
	while(e.hasMoreElements())
	{
	    sel[s++] = (Cluster) e.nextElement();
	}
	return sel;
    }
    
    public final void setClusterSelection(Cluster[] sel)
    {
	cluster_selection.clear();
	if(sel != null)
	{
	    for(int c=0; c < sel.length; c++)
		cluster_selection.put(sel[c], sel[c]);

	    notifySelectionListeners(SpotSelection);
	}
    }
    
    private Hashtable cluster_selection = new Hashtable();
    

    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    //
    // A Selection of Spots.....
    //
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------

    public final void setSpotSelected(int spot_id, boolean is_selected)
    {
	setSpotSelected(spot_id, is_selected, true);
    }

    public final void setSpotSelected(int spot_id, boolean is_selected, boolean notify)
    {
	Integer si =  new Integer( spot_id );
	if(is_selected)
	    spot_selection.put( si, si );
	else
	    spot_selection.remove( si );

	if(notify)
	    notifySelectionListeners(SpotSelection);
    }

    public final boolean isSpotSelected(int spot_id)
    {
	Integer si = (Integer) spot_selection.get( new Integer( spot_id ));
	return si != null;
    }

    public final void clearSpotSelection()
    {
	spot_selection.clear();
	notifySelectionListeners(SpotSelection);
    }

    public final void selectAllSpots( )
    {
	selectAllSpots( false );
    }

    public final void selectAllSpots( boolean filter )
    {
	Hashtable new_sel = new Hashtable();
	for(int s=0; s < n_spots; s++)
	{
	    if(!filter || !filter(s))
		new_sel.put(new Integer( s ), new Integer( s ));
	}
	spot_selection = new_sel;
	notifySelectionListeners(SpotSelection);
    }

    public final void invertSpotSelection()
    {
	Hashtable new_sel = new Hashtable();
	for(int s=0; s < n_spots; s++)
	{
	    if(!isSpotSelected(s))
		new_sel.put(new Integer( s ), new Integer( s ));
	}
	spot_selection = new_sel;
	notifySelectionListeners(SpotSelection);
    }

    public final int[] getSpotSelection()
    {
	final int ss = spot_selection.size();
	int s = 0;
	int[] sel = new int[ ss ];
	
	java.util.Enumeration e = spot_selection.keys();
	while(e.hasMoreElements())
	{
	    Integer si = (Integer) e.nextElement();
	    sel[s++] = si.intValue();
	}
	return sel;
    }
    
    public final void setSpotSelection(int[] sel)
    {
	spot_selection.clear();
	if(sel != null)
	{
	    for(int s=0; s < sel.length; s++)
		spot_selection.put(new Integer(sel[s]), new Integer(s));
	}
	notifySelectionListeners(SpotSelection);
    }
    
    // convenience function for single selection
    public final void setSpotSelection(int spot_id)
    {
	int[] sid_a = new int[1];
	sid_a[0] = spot_id;
	setSpotSelection(sid_a);
    }
    
    private Hashtable spot_selection = new Hashtable();
    
    // ---------------- --------------- 

    public final void addFilteredSpots()
    {
	if( filterIsOn() )
	    for(int s=0; s < n_spots; s++)
		if(!filter(s))
		    setSpotSelected(s, true, false);
	notifySelectionListeners(SpotSelection);
    }

    public final void removeFilteredSpots()
    {
	if( filterIsOn() )
	    for(int s=0; s < n_spots; s++)
		if(!filter(s))
		    setSpotSelected(s, false, false);
	notifySelectionListeners(SpotSelection);
    }

    // ---------------- --------------- 

    public final void addToSpotSelection(int[] sel)
    {
	if(sel != null)
	{
	    for(int s=0; s < sel.length; s++)
		spot_selection.put(new Integer(sel[s]), new Integer(s));
	    notifySelectionListeners(SpotSelection);
	}
    }

    public final void removeFromSpotSelection(int[] sel)
    {
	if(sel != null)
	{
	    for(int s=0; s < sel.length; s++)
		spot_selection.remove(new Integer(sel[s]));
	    notifySelectionListeners(SpotSelection);
	}
    }

    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    //
    // A Selection of Measurements.....
    //
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------

    public final void setMeasurementSelected(int measurement_id, boolean is_selected)
    {
	setMeasurementSelected(measurement_id, is_selected, true);
    }

    public final void setMeasurementSelected(int measurement_id, boolean is_selected, boolean notify)
    {
	Integer mi =  new Integer( measurement_id );
	if(is_selected)
	    measurement_selection.put( mi, mi );
	else
	    measurement_selection.remove( mi );

	if(notify)
	    notifySelectionListeners(MeasurementSelection);
    }

    public final boolean isMeasurementSelected(int measurement_id)
    {
	Integer mi = (Integer) measurement_selection.get( new Integer( measurement_id ));
	return mi != null;
    }

    public final void clearMeasurementSelection()
    {
	measurement_selection.clear();
	notifySelectionListeners(MeasurementSelection);
    }

    public final void invertMeasurementSelection()
    {
	Hashtable new_sel = new Hashtable();
	for(int m=0; m < n_measurements; m++)
	{
	    if(measurement[m].show)
		if(!isMeasurementSelected(m))
		    new_sel.put(new Integer( m ), new Integer( m ));
	}
	measurement_selection = new_sel;
	notifySelectionListeners(MeasurementSelection);
    }

    public final void selectAllMeasurements()
    {
	Hashtable new_sel = new Hashtable();
	for(int m=0; m < n_measurements; m++)
	{
	    if(measurement[m].show)
		new_sel.put(new Integer( m ), new Integer( m ));
	}
	measurement_selection = new_sel;
	notifySelectionListeners(MeasurementSelection);
    }

    public final int[] getMeasurementSelection()
    {
	final int ms = measurement_selection.size();
	int m = 0;
	int[] sel = new int[ ms ];
	
	java.util.Enumeration e = measurement_selection.keys();

	while(e.hasMoreElements())
	{
	    Integer mi = (Integer) e.nextElement();
	    sel[m++] = mi.intValue();
	}

	return sel;
    }
    
    public final void setMeasurementSelection(int[] sel)
    {
	measurement_selection.clear();
	if(sel != null)
	{
	    for(int m=0; m < sel.length; m++)
		measurement_selection.put(new Integer(sel[m]), new Integer(m));
	}
	notifySelectionListeners(MeasurementSelection);
    }
    
    public final void addToMeasurementSelection(int[] sel)
    {
	if(sel != null)
	{
	    for(int m=0; m < sel.length; m++)
		measurement_selection.put(new Integer(sel[m]), new Integer(m));
	}
	notifySelectionListeners(MeasurementSelection);
    }

    // convenience function for single selection
    public final void setMeasurementSelection(int measurement_id)
    {
	int[] mid_a = new int[1];
	mid_a[0] = measurement_id;
	setMeasurementSelection(mid_a);
    }
    
    private Hashtable measurement_selection = new Hashtable();



    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    //
    // Selection Listeners: Remote and External
    //
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------


    public static final int SpotSelection            = 1;
    public static final int ClusterSelection         = 2;
    public static final int MeasurementSelection     = 4;
    public static final int SpotMeasurementSelection = 5;   // ==(1&4)


    public void notifySelectionListeners(int changes)
    {
	int[] ss     = ((changes & SpotMeasurementSelection) > 0) ? getSpotSelection() : null;
	int[] ms     = ((changes & SpotMeasurementSelection) > 0) ? getMeasurementSelection() : null;
	Cluster[] cs = ((changes & ClusterSelection) > 0) ? getClusterSelection() : null;
	
	
	final int esls = external_selection_listeners.size();
	if(esls > 0)
	{
	    ExternalSelectionListener esl = null;

	    for(int e=0; e < esls; e++)
	    {
		esl = (ExternalSelectionListener)external_selection_listeners.elementAt(e);
		
		if(ss != null)
		    esl.spotSelectionChanged(ss);
		if(ms != null)
		    esl.spotMeasurementSelectionChanged(ss, ms);
		if(cs != null)
		    esl.clusterSelectionChanged(cs);
	    }
	}

	final int rsls = remote_selection_listeners.size();
	if(rsls > 0)
	{
	    RemoteSelectionListener rsl = null;

	    for(int r=0; r < rsls; r++)
	    {
		try
		{
		    rsl = (RemoteSelectionListener)remote_selection_listeners.elementAt(r);

		    if(ss != null)
			rsl.spotSelectionChanged(ss);
		    if(ms != null)
			rsl.spotMeasurementSelectionChanged(ss, ms);
		    if(cs != null)
			rsl.clusterSelectionChanged(cs);

		}
		catch(java.rmi.RemoteException re)
		{
		    // this listener is not responding for some reason...
		}
	    }
	}
    }

    // ------- remote access to the selections -----

    public interface RemoteSelectionListener extends Remote 
    {
	public void spotSelectionChanged(int[] spots_ids) throws RemoteException;
	public void clusterSelectionChanged(Cluster[] clusters) throws RemoteException;
	public void spotMeasurementSelectionChanged(int[] spot_ids, int[] meas_ids) throws RemoteException;
    }
 
    public int addRemoteSelectionListener(RemoteSelectionListener rsl)
    {
	remote_selection_listeners.addElement(rsl);
	int h_id  = next_free_rsl_handle++;
	rsl_handles.put(new Integer(h_id), rsl);
	System.out.println("remote selection listener added");
	return h_id;
    }
    public void removeRemoteSelectionListener(int rsl_handle)
    {
	final Integer key = new Integer(rsl_handle);
	RemoteSelectionListener rsl = (RemoteSelectionListener) rsl_handles.get(key);
	if(rsl != null)
	{
	    remote_selection_listeners.removeElement(rsl);
	    rsl_handles.remove(key);
	    System.out.println("remote selection listener removed");
	}
    }
    
    public Vector getRemoteSelectionListeners()
    {
	return remote_selection_listeners;
    }

    int next_free_rsl_handle = 0;
    Vector remote_selection_listeners = new Vector();
    Hashtable rsl_handles = new Hashtable();

    // ------- external (i.e. wrapper apps) access to the selections -----

    public interface ExternalSelectionListener
    {
	public void spotSelectionChanged(int[] spot_ids);
	public void clusterSelectionChanged(Cluster[] clusters);
	public void spotMeasurementSelectionChanged(int[] spot_ids, int[] meas_ids);
    }
 
    public int addExternalSelectionListener(ExternalSelectionListener esl)
    {
	external_selection_listeners.addElement(esl);
	int h_id  = next_free_esl_handle++;
	esl_handles.put(new Integer(h_id), esl);
	//System.out.println("external selection listener added");
	return h_id;
    }
    public void removeExternalSelectionListener(int esl_handle)
    {
	final Integer key = new Integer(esl_handle);
	ExternalSelectionListener esl = (ExternalSelectionListener) esl_handles.get(key);
	if(esl != null)
	{
	    external_selection_listeners.removeElement(esl);
	    esl_handles.remove(key);
	    // System.out.println("External selection listener removed");
	}
    }
    
    public Vector getExternalSelectionListeners()
    {
	return external_selection_listeners;
    }

    int next_free_esl_handle = 0;
    Vector external_selection_listeners = new Vector();
    Hashtable esl_handles = new Hashtable();

     // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    //
    // ExprDataObserver interface
    //
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------

    private final boolean observer_debug = false;

    public class UpdateEvent
    {
	public int event;

	public int getEventCode() { return event; }
    }

    public class DataUpdateEvent extends UpdateEvent
    {
	public int spot;              // the actual index of the first spot that has changed or -1 if all spots have changed
	public int measurement;       // which measurement was affected, or -1 if all measurments
	public double old_value;      // the previous value of the spot (NOT USED)
    }

    public class ClusterUpdateEvent extends UpdateEvent
    {
	public Cluster cluster;
    }

    public class MeasurementUpdateEvent extends UpdateEvent
    {
	public int measurement;
    }

    public class EnvironmentUpdateEvent extends UpdateEvent
    {
	public String data;
    }

    public interface ExprDataObserver
    {
	public void dataUpdate        (DataUpdateEvent due);
	public void clusterUpdate     (ClusterUpdateEvent cue);
	public void measurementUpdate (MeasurementUpdateEvent mue);
	public void environmentUpdate (EnvironmentUpdateEvent eue);
    }

    public interface RemoteExprDataObserver extends Remote 
    {
	public void dataUpdate        (DataUpdateEvent due) throws RemoteException;
	public void clusterUpdate     (ClusterUpdateEvent cue) throws RemoteException;
	public void measurementUpdate (MeasurementUpdateEvent mue) throws RemoteException;
	public void environmentUpdate (EnvironmentUpdateEvent eue) throws RemoteException;
    }

    public void addObserver(ExprDataObserver edo)
    {
	generateEnvironmentUpdate(ObserversChanged, edo.getClass().getName());

	observers.addElement( edo );

	if(observer_debug)
	{
	    System.out.println(" -=> -=> observer (" + edo.getClass().getName() + 
			       ") added, " + observers.size() + " active"); 
	}
    }

    public void removeObserver(ExprDataObserver edo)
    {
       generateEnvironmentUpdate(ObserversChanged, edo.getClass().getName());

       observers.removeElement(edo);

       if(observer_debug)
       {
	   System.out.println(" -=> -=> observer (" + edo.getClass().getName() + ") removed, " + 
			      observers.size() + " still active"); 
       }
    }

    // Data Updates 
    //
    // allows other objects (ie not this ExprData) to initiate
    // update messages to the observers
    //
    public void generateDataUpdate(int event, int spot, int meas, double old_value)
    {
	DataUpdateEvent dui = new DataUpdateEvent();

	dui.event = event;
	dui.spot = spot;
	dui.measurement = meas;
	dui.old_value = old_value;

	notifyObservers(dui);
    }
    // different form, used when more than one gene has changed
    public void generateDataUpdate(int event)
    {
	DataUpdateEvent dui = new DataUpdateEvent();

	dui.event = event;
	dui.spot = -1;
	dui.measurement = -1;

	notifyObservers(dui);
    }

    private void notifyObservers(DataUpdateEvent dui)
    {
	ExprDataObserver edo = null;

	for(int o=0; o < observers.size(); o++)
	{
	    edo = (ExprDataObserver) observers.elementAt( o );
	    edo.dataUpdate(dui);
	}
	for(int o=0; o < remote_observers.size(); o++)
	{
	    RemoteExprDataObserver redo = (RemoteExprDataObserver) remote_observers.elementAt( o );
	    try
	    {
		redo.dataUpdate(dui);
	    }
	    catch(java.rmi.RemoteException re)
	    {
	    }
	}
    }

    // Cluster updates
    //
    public void generateClusterUpdate(int event, Cluster cl)
    {
	ClusterUpdateEvent cue = new ClusterUpdateEvent();

	cue.event = event;
	cue.cluster = cl;

	notifyObservers(cue);
    }
    public void generateClusterUpdate(int event)
    {
	ClusterUpdateEvent cue = new ClusterUpdateEvent();

	cue.event = event;
	cue.cluster = null;

	notifyObservers(cue);
    }

    private void notifyObservers(ClusterUpdateEvent cue)
    {
	for(int o=0; o < observers.size(); o++)
	{
	    ExprDataObserver edo = (ExprDataObserver) observers.elementAt( o );
	    edo.clusterUpdate(cue);
	}
	for(int o=0; o < remote_observers.size(); o++)
	{
	    RemoteExprDataObserver redo = (RemoteExprDataObserver) remote_observers.elementAt( o );
	    try
	    {
		redo.clusterUpdate(cue);
	    }
	    catch(java.rmi.RemoteException re)
	    {
	    }
	}
    }

    // Set updates
    //
    public void generateMeasurementUpdate(int event, int m)
    {
        MeasurementUpdateEvent sue = new MeasurementUpdateEvent();
	sue.event = event;
	sue.measurement = m;	
	notifyObservers(sue);
    }
    public void generateMeasurementUpdate(int event)
    {
        MeasurementUpdateEvent sue = new MeasurementUpdateEvent();
	sue.event = event;
	sue.measurement = -1;	
	notifyObservers(sue);
    }

    private void notifyObservers(MeasurementUpdateEvent mue)
    {
	ExprDataObserver edo = null;

	for(int o=0; o < observers.size(); o++)
	{
	    edo = (ExprDataObserver) observers.elementAt( o );
	    edo.measurementUpdate(mue);
	}
	for(int o=0; o < remote_observers.size(); o++)
	{
	    RemoteExprDataObserver redo = (RemoteExprDataObserver) remote_observers.elementAt( o );
	    try
	    {
		redo.measurementUpdate(mue);
	    }
	    catch(java.rmi.RemoteException re)
	    {
	    }
	}
    }

    // Environment updates
    //
    // allows other objects (ie not this ExprData) to initiate
    // update messages to the observers
    //
    public void generateEnvironmentUpdate(int event, String data)
    {
	EnvironmentUpdateEvent eue = new EnvironmentUpdateEvent();
	eue.event = event;
	eue.data = data;
	notifyObservers(eue);
    }
    public void generateEnvironmentUpdate(int event)
    {
	EnvironmentUpdateEvent eue = new EnvironmentUpdateEvent();
	eue.event = event;
	eue.data = null;
	notifyObservers(eue);
    }

    private void notifyObservers(EnvironmentUpdateEvent eue)
    {
	ExprDataObserver edo = null;
	for(int o=0; o < observers.size(); o++)
	{
	    edo = (ExprDataObserver) observers.elementAt( o );
	    edo.environmentUpdate(eue);
	}
	for(int o=0; o < remote_observers.size(); o++)
	{
	    RemoteExprDataObserver redo = (RemoteExprDataObserver) remote_observers.elementAt( o );
	    try
	    {
		redo.environmentUpdate(eue);
	    }
	    catch(java.rmi.RemoteException re)
	    {
	    }
	}
    }

    public int getNumObservers() { return observers.size(); }

    // ---------------- --------------- --------------- ------------- ------------

    public int addRemoteDataObserver(RemoteExprDataObserver redo)
    {
	generateEnvironmentUpdate(ObserversChanged, redo.getClass().getName());
	int h_id  = next_free_redo_handle++;
	redo_handles.put(new Integer(h_id), redo);
	remote_observers.addElement( redo );
	return h_id;
    }
    public void removeRemoteDataObserver(int redo_handle)
    {
	final Integer key = new Integer(redo_handle);
	RemoteExprDataObserver redo = (RemoteExprDataObserver)  redo_handles.get(key);
	if(redo != null)
	{
	    generateEnvironmentUpdate(ObserversChanged, redo.getClass().getName());
	    remote_observers.removeElement( redo );
	    redo_handles.remove(key);
	}
    }

    int next_free_redo_handle = 0;
    Hashtable redo_handles = new Hashtable();
 
    // ---------------- --------------- --------------- ------------- ------------

    public interface ExternalDataSink
    {
	public boolean likesSpots();
	public boolean likesSpotMeasurements();
	public boolean likesClusters();

	public void consumeSpots(int[] spots_ids);
	public void consumeSpotMeasurements(int n_spots, int n_meas, double[][] data);
	public void consumeClusters(Cluster[] clusters);

	public String getName();
    }

    public interface RemoteDataSink extends Remote
    {
	public boolean likesSpots();
	public boolean likesSpotMeasurements();
	public boolean likesClusters();

	public void consumeSpots(int[] spots_ids) throws RemoteException;
	public void consumeSpotMeasurements(int n_spots, int n_meas, double[][] data) throws RemoteException;
	public void consumeClusters(ClusterHandle[] rci) throws RemoteException;

	public String getName();
    }
    
    public int addExternalDataSink(ExternalDataSink eds)
    {
	int h_id  = next_free_rds_handle++;
	eds_handles.put(new Integer(h_id), eds);
	external_data_sinks.addElement(eds);
	//System.out.println("external sink '" + eds.getName() + "' added");
	return h_id;
    }
    public void removeExternalDataSink(int eds_handle)
    {
	final Integer key = new Integer(eds_handle);
	ExternalDataSink eds = (ExternalDataSink)  eds_handles.get(key);
	if(eds != null)
	{
	    external_data_sinks.removeElement(eds);
	    //System.out.println("external sink '" + eds.getName() + "' removed");
	    rds_handles.remove(key);
	}
    }

    public Vector getExternalDataSinks()
    {
	return external_data_sinks;
    }

    int next_free_eds_handle = 0;
    Hashtable eds_handles = new Hashtable();
 
    public int addRemoteDataSink(RemoteDataSink rds)
    {
	int h_id  = next_free_rds_handle++;
	rds_handles.put(new Integer(h_id), rds);
	remote_data_sinks.addElement(rds);
	System.out.println("remote sink '" + rds.getName() + "' added");
	return h_id;
    }
    public void removeRemoteDataSink(int rds_handle)
    {
	final Integer key = new Integer(rds_handle);
	RemoteDataSink rds = (RemoteDataSink)  rds_handles.get(key);
	if(rds != null)
	{
	    remote_data_sinks.removeElement(rds);
	    System.out.println("remote sink '" + rds.getName() + "' removed");
	    rds_handles.remove(key);
	}
    }
    
    public Vector getRemoteDataSinks()
    {
	return remote_data_sinks;
    }

    int next_free_rds_handle = 0;
    Hashtable rds_handles = new Hashtable();
 
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    //
    //  Sorting 
    //
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------
    // ---------------- --------------- --------------- ------------- ------------

    //
    // the current traversal order shows which order the
    // spots should be visited in (eg to get a sorted ordering)
    //
    // this is neccessary because clusters can be mapped to spots using the 'natural' indexing
    // (i.e. that used in the source of the data) and if we change this ordering, then the 
    // cluster->spot mappings need to be updated. 
    //
    // it is also more efficient to work this way as the data is never moved,
    // just processed in a different order
    //

    private int[] current_spot_traversal = null;
    
    // and the inverse mapping
    private int[] inverse_spot_traversal = null;

    // likewise for Measurements

    private int[] current_meas_traversal = null;
    private int[] inverse_meas_traversal = null;
    
    // ---------------- --------------- --------------- ------------- ------------

    public class SortList
    {
	public double value;
	public int index;
	public SortList next;
	
	public SortList(double v, int i)
	{
	    value = v;
	    index = i;
	    next = null;
	}
    }

    public void sortSpots(int meas, int order, SortListener listener)
    {
	doSort(true, meas, order, listener);
    }

    public int[] getTraversal(int meas, int order)
    {
	return doSort(false,  meas, order, null);
    }

    public int[] getTraversal(int meas, int order, SortListener listener)
    {
	return doSort(false,  meas, order, listener);
    }

    public final static int SortAscending = 0;
    public final static int SortDescending = 0;

    // an ordering that always puts NaNs at > +Infinity
    private boolean isGreaterThan(double lhs, double rhs) 
    {
	if(Double.isNaN(lhs))
	    return true;
	if(Double.isNaN(rhs))
	    return false;
	return(lhs > rhs);
    }

    // generate an ordering on the data based on the values
    // in the specified Measurement
    //
    // uses a multi-headed insertion sort 
    //
    private int[] doSort(boolean update_traversal, int m, int order, SortListener listener)
    {
	// System.out.println("sorting set " + set + " in " + ((order > 0) ? "descending" : "ascending") + " order");
	
	int[] traversal_order   = new int[n_spots];
	int[] inverse_traversal = new int[n_spots];

	// find the range of the values to be sorted
	//
	double emax = measurement[m].data[0];
	double emin = emax;
	double e;

	for(int g=1; g < n_spots; g++)
	{
	    e = measurement[m].data[g];
	    if(!Double.isNaN(e))
	    {
		if(e > emax)
		    emax = e;
		if(e < emin)
		    emin = e;
	    }
	}

	// select how many lists to use
	// - assuming we want about 50 elements per list
	// - and assuming an equal distribution of values
	//
	int n_lists = (n_spots / 50);
	
	if(n_lists <= 0)
	    n_lists = 1;

	//System.out.println("using " + n_lists + " lists");

	double escale = (n_lists-1) / (emax - emin);

	SortList[] head = new SortList[n_lists];
	SortList[] tail = new SortList[n_lists];

	for(int l=0; l <n_lists; l++)
	    head[l] = tail[l] = null;

	for(int i=0; i < n_spots; i++)
	{ 
	    final double eval = measurement[m].data[i];

	    SortList elem = new SortList(eval, i);
	    
	    int list = (n_lists-1);

	    // make sure NaNs always end up at the end
	    //
	    
	    if(!Double.isNaN(eval))
		list = (int)((eval - emin)  * escale);
	    //else
	    //System.out.println("NaN shoved at end");

	    //int list = (int)((measurement[m].data[g] - emin)  * escale);

	    if(list >= n_lists)
	    {
		System.out.println("list index overflow (" + list + ")");
		list = (n_lists-1);
	    }
	    if(list < 0)
	    {
		System.out.println("list index underflow (" + list + ")");
		list = 0;
	    }

	    //System.out.println(e_data[measurement][g] + " goes into list " + list);

	    SortList insert = head[list];

	    // insert into the correct place

	    if(insert == null)
	    {
		// insert as the new head
		elem.next = insert;
		head[list] = elem;
		// and it's also the tail of this list
		tail[list] = elem;

		//System.out.println("inserted as new head");
	    }
	    else
	    {
		if(/*(insert.value == Double.NaN) ||*/ !isGreaterThan(eval, insert.value))
		{
		    // insert as the new head
		    elem.next = insert;
		    head[list] = elem;
		    
		    //System.out.println("inserted at head");
		}
		else
		{
		    /*if(measurement[m].data[g] == Double.NaN)
		    {
			// make sure NaNs always end up at the end
			tail[list].next = elem;
			tail[list] = elem;  
		    }
		    else
		    */
		    {
			
			// search for insertion point
			while((insert != null) && 
			      (insert.next != null) && 
			      (isGreaterThan(eval, insert.next.value)))
			{
			    insert = insert.next;
			}
			if(insert == null)
			{ 
			    // add at the tail
			    tail[list].next = elem;
			    tail[list] = elem;  
			    
			    //System.out.println("inserted as tail");
			}
			else
			{
			    // add somewhere in the list 
			    elem.next = insert.next;
			    insert.next = elem;
			    
			    //System.out.println("inserted in Nth pos");
			}
		    }
		}
	    }
	}

	// now build a the traversal order using the lists
	//
	int dest_index;

	int list = 0;   // start with the first list

	for(int s=0; s < n_spots; s++)
	{ 
	    if(head[list] != null)
	    {
		if(order > 0)
		    dest_index = n_spots - (s+1);
		else
		    dest_index = s;
		
		traversal_order[dest_index] = head[list].index;

		inverse_traversal[head[list].index] = dest_index;

		if(head[list].next != null)
		{
		    head[list] = head[list].next;
		}
		else
		{
		    //System.out.println(count + " in list " + list);
		    head[list] = null;
		    // must allow for empty lists being found
		    while((head[list] == null) && (list < (n_lists-1)))
		    { 
			list++;
		    }
		    if(list >= n_lists)
			head[list] = null;
		}
	    }
	    else
		System.out.println("doSort(): WARNING! missing elements");
	}

	if(update_traversal)
	{
	    current_spot_traversal = traversal_order;
	    inverse_spot_traversal = inverse_traversal;
	    generateDataUpdate(OrderChanged);
	}

	return traversal_order;
    }

    // ---------------- --------------- --------------- ------------- ------------

    public abstract class SortListener
    {
	abstract void sortInProgress(int percent);
    }

    // ---------------- --------------- --------------- ------------- ------------
    //
    //  easy data access via SpotIterator and MeasurementIterator
    //
    // ---------------- --------------- --------------- ------------- ------------
 
    public static final int ApplyFilter       = 1; // by default the filters are not applied
    public static final int AllMeasurements   = 2; // by default only Measurments with (show==true)
    public static final int SelectedDataOnly  = 4; // only consider the currently selected data
    public static final int TraversalOrder    = 8; // otherwise use natural ordering

    public class InvalidIteratorException extends Throwable
    { 
	public InvalidIteratorException ( String msg_ ) { msg = msg_; }
	public String toString() {  return msg; }
	private String msg;
    }
    
    public class SpotIterator
    {
	private SpotIterator( int flags_ )
	{
	    meas_valid = false;
	    flags = flags_;
	    getFirstSpot();
	}

	private SpotIterator( MeasurementIterator owner, int flags_ )
	{
	    flags = flags_;
	    if(owner != null)
	    {
		meas_id = owner.meas_id;
		meas_valid = true;
	    }
	    else
	    {
		meas_valid = false;
	    }
	    getFirstSpot();
	}
	
	private SpotIterator( int meas_id_, int flags_ )
	{
	    meas_valid = ((meas_id >= 0) && (meas_id < n_measurements));
	    meas_id = meas_id_;
	    
	    flags = flags_;
	    getFirstSpot();
	}



	public MeasurementIterator getMeasurementIterator( )
	{
	    return new MeasurementIterator( this, 0 );
	}
	public MeasurementIterator getMeasurementIterator( int flags_ )
	{
	    return new MeasurementIterator( this, flags_ );
	}



	public int getSpotID() throws InvalidIteratorException
	{
	    if((spot_id >= 0) && (spot_id < n_spots))
		return spot_id;
	    else
		throw new InvalidIteratorException("SpotID " + spot_id + " is out of bounds");
	}


	public boolean isValid()
	{
	    return spot_valid;
	}
	
	public boolean hasNext() // true if increment would be ok, false otherwise
	{
	    if(spot_valid)
	    {
		int next_spot_id = nextID( spot_id );

		if((next_spot_id < 0) || (next_spot_id >= n_spots))
		    return false;
		else
		    return true;
	    }
	    else
		return false;
	}

	public boolean next()    // true if increment was ok, false otherwise
	{
	    if(spot_valid)
	    {
		spot_id = nextID( spot_id );
		
		if((spot_id < 0) || (spot_id >= n_spots))
		    spot_valid = false;
		
		return spot_valid;
	    }
	    else
		return false;
	}

	public double value() throws InvalidIteratorException  // returns current value
	{
	    if(spot_valid)
	    {
		if(meas_valid)
		{
		    return eValue( meas_id, spot_id );
		}
		else
		    throw new InvalidIteratorException("Undefined Measurement");
	    }
	    else
		throw new InvalidIteratorException("SpotID " + spot_id + " is out of bounds");
	}
	
	public void reset()
	{
	    getFirstSpot();
	}

	// ----------------

	// TODO: mutability

	/*
	public double setValue(double);

	public void setSpotTagAttr();
	public void setProbeTagAttr();
	public void setGeneTagAttr();
	*/

	// ----------------

	private void getFirstSpot()
	{
	    spot_id = ((flags & TraversalOrder) > 0) ? current_spot_traversal[0] : 0;
	    spot_valid = allowSpot(spot_id);
	    while(!spot_valid && (spot_id < n_spots))
	    {
		spot_id = nextID( spot_id );
		spot_valid = allowSpot(spot_id);
	    }
	}

	private boolean allowSpot( int id ) throws ArrayIndexOutOfBoundsException
	{
	    if( id >= n_spots )
		return false;
	    if(((flags & ApplyFilter) > 0) && filter(id))
		return false;
	    if(((flags & SelectedDataOnly) > 0) && !isSpotSelected(id))
		return false;
	    return true;
	}

	private int nextID( int id )
	{
	    try
	    {
		int next_id = ((flags & TraversalOrder) > 0) ? current_spot_traversal[inverse_spot_traversal[id]+1] : (id+1);
		while( !allowSpot( next_id ) && ( next_id < n_spots ) )
		{
		    next_id = ((flags&TraversalOrder)>0) ? current_spot_traversal[inverse_spot_traversal[next_id]+1] : (next_id+1);
		}
		return next_id;
	    }
	    catch(ArrayIndexOutOfBoundsException aioobe)
	    {
		// caused in 'TraversalOrder' mode when the last spot is reached
		return n_spots;
	    }
	}

	private boolean meas_valid;
	private boolean spot_valid;
	private int flags;
	private int spot_id;
	private int meas_id;  // when created by a MeasIterator
    }

    public class MeasurementIterator
    {
	private MeasurementIterator( int flags_ )
	{
	    spot_valid = false;
	    flags = flags_;
	    getFirstMeas();
	}
	private MeasurementIterator( SpotIterator owner, int flags_ )
	{
	    flags = flags_;
	    if(owner != null)
	    {
		spot_id = owner.spot_id;
		spot_valid = true;
	    }
	    else
	    {
		spot_valid = false;
	    }
	    getFirstMeas();
	}
	private MeasurementIterator( int spot_id_, int flags_ )
	{
	    spot_valid = ((spot_id >= 0) && (spot_id < n_spots));
	    spot_id = spot_id_;
	    
	    flags = flags_;
	    getFirstMeas();
	}
	


	public SpotIterator getSpotIterator(  )
	{
	    return new SpotIterator( this, 0 );
	}

	public SpotIterator getSpotIterator( int flags_ )
	{
	    return new SpotIterator( this, flags_ );
	}



	public int getMeasurementID() throws InvalidIteratorException
	{
	   if((meas_id >= 0) && (meas_id < n_measurements))
		return meas_id;
	    else
		throw new InvalidIteratorException("MeasurementID " + meas_id + " is out of bounds"); 
	}


	public boolean isValid()
	{
	    return meas_valid;
	}
	
	public boolean hasNext()
	{
	    if(meas_valid)
	    {
		int next_meas_id = nextID( meas_id );

		if((next_meas_id < 0) || (next_meas_id >= n_measurements))
		    return false;
		else
		    return true;
	    }
	    else
		return false;
	}

	public boolean next()    // true if increment was ok, false otherwise
	{
	    if(meas_valid)
	    {
		meas_id = nextID( meas_id );

		if((meas_id < 0) || (meas_id >= n_measurements))
		    meas_valid = false;

		return meas_valid;
	    }
	    else
		return false;
	}

	public double value() throws InvalidIteratorException  // returns current value
	{
	    if(meas_valid)
	    {
		if(spot_valid)
		{
		    return eValue( meas_id, spot_id );
		}
		else
		    throw new InvalidIteratorException("Undefined Spot");
	    }
	    else
		throw new InvalidIteratorException("MeasurementID " + meas_id + " is out of bounds");
	}

	public void reset()
	{
	    getFirstMeas();
	}

	// --------------------------

	private void getFirstMeas()
	{
	    meas_id = ((flags & TraversalOrder) > 0) ? current_meas_traversal[0] : 0;
	    meas_valid = allowMeas( meas_id );
	    while(!meas_valid && (meas_id < n_measurements))
	    {
		meas_id = nextID( meas_id );
		meas_valid = allowMeas( meas_id );
	    }
	}

	private boolean allowMeas( int id )
	{
	    if( id >= n_measurements)
		return false;
	    if( ((flags & AllMeasurements) == 0) && (measurement[id].show == false) )
		return false;
	    if( ((flags & SelectedDataOnly) > 0) && !isMeasurementSelected(id) )
		return false;
	    return true;
	}

	private int nextID( int id )
	{
	    try
	    {
		int next_id = ((flags & TraversalOrder) > 0) ? current_meas_traversal[inverse_meas_traversal[id]+1] : (id+1);
		while( !allowMeas( next_id ) && ( next_id < n_measurements ) )
		{
		    next_id = ((flags&TraversalOrder)>0) ? current_meas_traversal[inverse_meas_traversal[next_id]+1] : (next_id+1);
		}
		return next_id;

	    }
	    catch(ArrayIndexOutOfBoundsException aioobe)
	    {
		// caused in 'TraversalOrder' mode when the last measurement is reached
		return n_measurements;
	    }

	}

	
	private boolean meas_valid;
	private boolean spot_valid;
	private int flags;
	private int meas_id;
	private int spot_id;  // when created by a MeasIterator
    }

    // lots of ways of creating a SpotIterator
    
    public SpotIterator getSpotIterator(  )
    {
	return new SpotIterator( 0 );
    }
    public SpotIterator getSpotIterator( int flags )
    {
	return new SpotIterator( flags );
    }
    public SpotIterator getSpotIterator( int flags, int meas_id )
    {
	return new SpotIterator( meas_id, flags );
    }
    public SpotIterator getSpotIterator( int flags, String meas_name )
    {
	int m_id = getMeasurementFromName( meas_name );
	return (m_id >= 0) ? new SpotIterator( m_id, flags ) : null;
    }
    public SpotIterator getSpotIterator( String meas_name )
    {
	return getSpotIterator( 0, meas_name );
    }
    
    // lots of ways of creating a MeasurementIterator
    
    public MeasurementIterator getMeasurementIterator( )
    {		      			     
	return new MeasurementIterator( 0 );	      			     
    }		      			     
    public MeasurementIterator getMeasurementIterator( int flags )
    {		      			     
	return new MeasurementIterator( flags );	      			     
    }		      			     
    public MeasurementIterator getMeasurementIterator( int flags, int spot_id )
    {		      			     
	return new MeasurementIterator( spot_id, flags );      			     
    }		      			     
    public MeasurementIterator getMeasurementIterator( int flags, String spot_name )
    {		      			     
	int s_id = getIndexBySpotName( spot_name );
	return (s_id >= 0) ? new MeasurementIterator( s_id, flags ) : null;      			     
    }		      			     
    public MeasurementIterator getMeasurementIterator( String spot_name )
    {
	return getMeasurementIterator( 0, spot_name );
    }
     

    // ---------------- --------------- --------------- ------------- ------------
    //
    // enumerations
    //
    // ---------------- --------------- --------------- ------------- ------------
    
    // types of names of things
    public static final int SpotName         = 0;
    public static final int SpotIndex        = 1;
    public static final int ProbeName        = 2;
    public static final int GeneName         = 3;
    public static final int MeasurementName  = 4;
    public static final int MeasurementIndex = 5;

    public static final int ExpressionData = 1;
    public static final int ClusterData    = 2;

    // major changes
    public static final int Data         = 0;
    public static final int Set          = 1;
    public static final int Cluster      = 2;
    public static final int Environment  = 3;   // everything that isn't data...
    
    // minor changes
    public static final int SizeChanged        = 0;
    public static final int ColourChanged      = 1;
    public static final int VisibilityChanged  = 2;
    public static final int OrderChanged       = 3;
    public static final int ElementsAdded      = 4;
    public static final int ElementsRemoved    = 5;
    public static final int NameChanged        = 6;
    public static final int ValuesChanged      = 7;
    public static final int RangeChanged       = 8;   // min/max has changed

    public static final int NameAttrsChanged    = 9;   // actual change of value

    // specialised things...
    public static final int ObserversChanged     = 10;  // only occurs with Environment as major change

    // these should probably have their own event type
    // when the Great Event Handling Rewrite occurs.
    public static final int ColouriserAdded    = 11;  // send as Measurement updates
    public static final int ColouriserRemoved  = 12;
    public static final int ColouriserChanged  = 13;

    public static final int VisibleNameAttrsChanged = 14;  // used in main display and other plots

    // and these should also be added....

    // public static final int FilterAdded   = ...
    // public static final int FilterRemoved = ...
    // public static final int FilterChanged = ...

    // public static final int SpotAttrsAdded   = ...
    // public static final int SpotAttrsRemoved = ...
    // public static final int SpotAttrsChanged = ...


    private Vector observers = new Vector();

    private Vector remote_observers = new Vector();

    private Vector remote_data_sinks = new Vector();

    private Vector external_data_sinks = new Vector();

}           // end of class ExprData

