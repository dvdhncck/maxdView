
public void doIt()
{
  String[] mnames = mview.getListOfMeasurementNames();

  try
  {
    boolean another = true;
    
    while(another)
    {
    int src = mview.getChoice("Pick the source Measurement", mnames, 0);

    ExprData.Measurement sm = edata.getMeasurement(src);

    if(sm.getNumSpotAttributes() == 0)
    {
       mview.alertMessage(sm.getName() + " has no SpotAttributes");
       return;
    }

    String[] anames = new String[ sm.getNumSpotAttributes() ];

    for(int sa=0; sa < sm.getNumSpotAttributes(); sa++)
       anames[sa] = sm.getSpotAttributeName(sa);

    int[] spotattrs = mview.getChoice("Pick the source SpotAttribute(s)", null, anames);

    for(int sa=0; sa < spotattrs.length; sa++)
    {
      if(spotattrs[sa] == 1)
      { 
	
        if(sm.getSpotAttributeDataType(sa).equals("DOUBLE"))
        { 
          double[] data = (double[]) sm.getSpotAttributeData(sa);

          edata.addOrderedMeasurement(edata.new Measurement(anames[sa], edata.ExpressionAbsoluteDataType, data));
        }
        if(sm.getSpotAttributeDataType(sa).equals("INTEGER"))
        { 
          int[] data = (int[]) sm.getSpotAttributeData(sa);
          double[] datad = new double[data.length];
          for(int d=0; d < data.length; d++)
            datad[d] = (double) data[d];

          edata.addOrderedMeasurement(edata.new Measurement(anames[sa], edata.ExpressionAbsoluteDataType, datad));
        }
        if(sm.getSpotAttributeDataType(sa).equals("CHAR"))
        { 
          mview.infoMessage("CHAR data type cannot be converted to Measurement");
        }
        if(sm.getSpotAttributeDataType(sa).equals("STRING"))
        { 
          mview.infoMessage("STRING data type cannot be converted to Measurement");
        }
      }
    }
    another = (mview.infoQuestion("Another?", "Yes", "No") == 0);
    }
  }
  catch(UserInputCancelled uic)
  { }
}
