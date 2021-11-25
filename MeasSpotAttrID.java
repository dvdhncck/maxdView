    public class MeasSpotAttrID
    {
	public MeasSpotAttrID(int m, int sa)
	{
	    meas_id = m;
	    spot_attr_id = sa;
	}

	public MeasSpotAttrID(int m)
	{
	    meas_id = m;
	    spot_attr_id = -1;
	}

	final public boolean isSpotAttr()    { return spot_attr_id >= 0; }
	final public boolean isMeasurement() { return spot_attr_id == -1; }

	public int meas_id;
	public int spot_attr_id;   // -1 for Measurement, >=0 for Spot Attr
    }

