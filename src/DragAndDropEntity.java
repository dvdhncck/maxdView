import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.util.Vector;

//
// lifted from examples at 
//
// http://java.miningco.com/compute/java/library/weekly/aa011299.htm
//
//

public class DragAndDropEntity implements Transferable
{
    // should be one of these things....

    public static final int UnknownEntity         = 0;
    public static final int SpotNameEntity        = 1;
    public static final int ProbeNameEntity       = 2;
    public static final int GeneNameEntity        = 3;
    public static final int MeasurementNameEntity = 4;
    public static final int ClusterEntity         = 5;
    public static final int ColouriserEntity      = 6;

    public int entity_type;
    
    public int   spot_id;
    public int   meas_id;

    public int[] spot_ids;
    public int[] meas_ids;

    public ExprData.Cluster cluster;
    public Colouriser colouriser;

    // some components don't understand the different entity types and
    // just want a text string (e.g the find boxes)
    //
    public boolean id_valid;
    public String name;                  // only used when the ID's are invalidated by a data change

    public DragAndDropEntity()
    {
	entity_type = UnknownEntity;
	id_valid = true;
	name = null;
    }

    private static final String[] tnames = 
    { "Unknown", "SpotName", "ProbeName", "GeneName", "MeasurementName", "ClusterName" };

    
    public String getEntityType()
    {
	return tnames[entity_type];
    }
	
    public static DragAndDropEntity createSpotNameEntity(int spot_id)
    {
	DragAndDropEntity de = new DragAndDropEntity();
	de.entity_type = SpotNameEntity;
	de.spot_id = spot_id;
	return de;
    }

    public static DragAndDropEntity createGeneNameEntity(int spot_id)
    {
	DragAndDropEntity de = new DragAndDropEntity();
	de.entity_type = GeneNameEntity;
	de.spot_id = spot_id;
	return de;
    }

    public static DragAndDropEntity createSpotNamesEntity(int[] spot_ids)
    {
	DragAndDropEntity de = new DragAndDropEntity();
	de.entity_type = SpotNameEntity;
	de.spot_ids = spot_ids;
	return de;
    }


    public static DragAndDropEntity createProbeNameEntity(int spot_id)
    {
	DragAndDropEntity de = new DragAndDropEntity();
	de.entity_type = ProbeNameEntity;
	de.spot_id = spot_id;
	return de;
    }
    public static DragAndDropEntity createMeasurementNameEntity(int meas_id)
    {
	DragAndDropEntity de = new DragAndDropEntity();
	de.entity_type = MeasurementNameEntity;
	de.meas_id = meas_id;
	return de;
    }
    public static DragAndDropEntity createMeasurementNamesEntity(int[] meas_ids)
    {
	DragAndDropEntity de = new DragAndDropEntity();
	de.entity_type = MeasurementNameEntity;
	de.meas_ids = meas_ids;
	return de;
    }

    public static DragAndDropEntity createClusterEntity(ExprData.Cluster cl)
    {
	DragAndDropEntity de = new DragAndDropEntity();
	de.entity_type = ClusterEntity;
	de.cluster = cl;
	return de;
    }

    public static DragAndDropEntity createColouriserEntity(Colouriser col)
    {
	DragAndDropEntity de = new DragAndDropEntity();
	de.entity_type = ColouriserEntity;
	de.colouriser = col;
	return de;
    }

    // ------------------ resolving entities.... ------------------

    // the name used for display can only be resolved given an ExprData object
    //
    public String toString(ExprData edata)
    { 
	if(id_valid)
	{
	    int one_m = (meas_ids == null) ? meas_id : ((meas_ids.length == 1 ? meas_ids[0] : -1));
	    int one_s = (spot_ids == null) ? spot_id : ((spot_ids.length == 1 ? spot_ids[0] : -1));
	    
	    switch(entity_type)
	    {
	    case MeasurementNameEntity:
		if(one_m >= 0)
		    return edata.getMeasurementName(one_m);
		else
		    return (meas_ids.length + " Measurements");
	    case SpotNameEntity:
		if(one_s >= 0)
		    return edata.getSpotName(one_s);
		else
		    return (spot_ids.length + " Spots");
	    case ProbeNameEntity:
		if(one_s >= 0)
		    return edata.getProbeName(one_s);
		else
		    return (spot_ids.length + " Probes");
	    case GeneNameEntity:
		if(one_s >= 0)
		    return edata.getGeneName(one_s);
		else
		    return (spot_ids.length + " Genes");
	    case ColouriserEntity:
		return colouriser.getName();
	    case ClusterEntity:
		return cluster.getName();
	    default:
		return "(unhandled entity type)";
	    }
	}
	else
	    return name;
    }

    private String plural( int count, String word )
    {
	return String.valueOf(count) + " " + (count == 1 ? word : (word + "s"));
    }

    public String toString()
    { return "(unresolved name)"; }

    public String getName(ExprData edata) { return toString(edata); }

    // ------------------ handling for each type.... ------------------

    public int getSpotId() throws WrongEntityException
    {
	if((entity_type == SpotNameEntity) || 
	   (entity_type == ProbeNameEntity) || 
	   (entity_type == GeneNameEntity))
	{
	    if((spot_ids == null) || (spot_ids.length == 0))
		return spot_id;
	    else
		return spot_ids[0];
	}
	throw new WrongEntityException("Not a SpotNameEntity (or Compatible)");
    }

    public int[] getSpotIds()  throws WrongEntityException
    {
	if((entity_type == SpotNameEntity) || 
	   (entity_type == GeneNameEntity) || 
	   (entity_type == ProbeNameEntity))
	{
	    if(spot_ids == null)
	    {
		int[] res = new int[1];
		res[0] = spot_id;
		return res;
	    }
	    else
	    {
		return spot_ids;
	    }
	}

	throw new WrongEntityException("Not a SpotNameEntity (or Compatible)");
    }
    
    public String getSpotName(ExprData edata)  throws WrongEntityException
    {
	if((entity_type == SpotNameEntity) || 
	   (entity_type == GeneNameEntity) || 
	   (entity_type == ProbeNameEntity))
	{
	    return edata.getSpotName(spot_id);
	}
	throw new WrongEntityException("Not a SpotNameEntity");
    }

    public String[] getSpotNames(ExprData edata)  throws WrongEntityException
    {
	if((entity_type == SpotNameEntity) || 
	   (entity_type == GeneNameEntity) || 
	   (entity_type == ProbeNameEntity))
	{
	    if(spot_ids == null)
	    {
		String[] res = new String[1];
		res[0] = edata.getSpotName(spot_id);
		return res;
	    }
	    else
	    {
		String[] res = new String[ spot_ids.length ];

		for(int s=0; s < spot_ids.length; s++)
		    res[s] = edata.getSpotName(spot_ids[s]);

		return res;
	    }
	}

	throw new WrongEntityException("Not a SpotNameEntity (or Compatible)");
    }

    public String getProbeName(ExprData edata)  throws WrongEntityException
    {
	if((entity_type == ProbeNameEntity) || 
	   (entity_type == GeneNameEntity) ||
	   (entity_type == SpotNameEntity))
	{
	    return edata.getProbeName(spot_id);
	}
	throw new WrongEntityException("Not a ProbeNameEntity");
    }

    public String getGeneName(ExprData edata)  throws WrongEntityException
    {
	if((entity_type == ProbeNameEntity) || 
	   (entity_type == GeneNameEntity) ||
	   (entity_type == SpotNameEntity))
	{
	    return edata.getGeneName(spot_id);
	}
	throw new WrongEntityException("Not a GeneNameEntity");
    }

    public String getMeasurementName(ExprData edata)  throws WrongEntityException
    {
	if(entity_type == MeasurementNameEntity)
	{
	    if(meas_ids == null)
	    {
		return edata.getMeasurementName(meas_id);
	    }
	    else
	    {
		return edata.getMeasurementName(meas_ids[0]);
	    }
	}
	throw new WrongEntityException("Not a MeasurementNameEntity");
    }
    public String[] getMeasurementNames(ExprData edata)  throws WrongEntityException
    {
	if(entity_type == MeasurementNameEntity)
	{
	    if(meas_ids == null)
	    {
		// this is a single MeasName, but return it as an array anyhow
		String[] res = new String[1];
		res[0] = edata.getMeasurementName(meas_id);
		return res;
	    }
	    else
	    {
		String[] res = new String[meas_ids.length];
		for(int m=0; m < meas_ids.length; m++)
		    res[m] = edata.getMeasurementName(meas_ids[m]);
		return res;
	    }
	}
	if(entity_type == ClusterEntity)
	{
	    if(cluster.getIsSpot())
		throw new WrongEntityException("Expecting Measurement ClusterEntity");
	    if(cluster.getElementNameMode() == ExprData.MeasurementName)
	    {
		Vector mnames = cluster.getElementNames();
		return (String[]) mnames.toArray(new String[0]);
	    }
	}

	throw new WrongEntityException("Not a MeasurementNameEntity");
    }

    public int getMeasurementId() throws WrongEntityException
    {
	if(entity_type == MeasurementNameEntity)
	{
	    if((meas_ids == null) || (meas_ids.length == 0))
		return meas_id;
	    else
		return meas_ids[0];
	}
	throw new WrongEntityException("Not a MeasurementNameEntity");
    }

    public int[] getMeasurementIds() throws WrongEntityException
    {
	if(entity_type == MeasurementNameEntity)
	{
	    if(meas_ids == null)
	    {
		int[] res = new int[1];
		res[0] = meas_id;
		return res;
	    }
	    else
	    {
		return meas_ids;
	    }
	}
	throw new WrongEntityException("Not a MeasurementNameEntity");
    }

    public ExprData.Cluster getCluster() throws WrongEntityException
    {
	if(entity_type == ClusterEntity)
	{
	    return cluster;
	}
	throw new WrongEntityException("Not a ClusterEntity");
    }

    public Colouriser getColouriser() throws WrongEntityException
    {
	if(entity_type == ColouriserEntity)
	{
	    return colouriser;
	}
	throw new WrongEntityException("Not a ColouriserEntity");
    }

    // ------------------ handling wrong types.... ------------------

    public class WrongEntityException extends Exception
    {
	String exp;

	public WrongEntityException(String s)
	{
	    exp = s;
	}
	
	public String toString() { return "DragAndDropEntity.WrongEntityException:" + exp; }
    }

    // ----------------------- internal doings ----------------------

    final public static DataFlavor 
	DragAndDropEntityFlavour = new DataFlavor("x-application/java-maxdView-DragAndDropEntity", "maxdView DragAndDropEntity");

    static DataFlavor flavours[] = { DragAndDropEntityFlavour };


    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException
    {
	if(flavor.equals(DragAndDropEntityFlavour))
	{
	    return this;
	}
	else
	{
	    throw new UnsupportedFlavorException(flavor);
	}
    }

    public DataFlavor[] getTransferDataFlavors() 
    {
	return flavours;
    }
    public boolean isDataFlavorSupported(DataFlavor flavor)
    {
	return flavor.equals(DragAndDropEntityFlavour);
    }
}
