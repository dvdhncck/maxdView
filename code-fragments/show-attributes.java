//
// displays the names and types of any SpotAttributes
// in any of the Measurements
//
public void doIt()
{
  StringBuffer info = new StringBuffer();

  for(int m=0; m < edata.getNumMeasurements(); m++)
  {
    ExprData.Measurement meas = edata.getMeasurement(m);
 
    int n_atts = meas.getNumSpotAttributes();
  
    info.append(meas.getName() + " has " + n_atts + " attrs:\n");

    if(n_atts > 0)
    {
      for(int a=0; a < n_atts; a++)
        info.append(" " + (a+1) + ". " + 
	            meas.getSpotAttributeName(a) + " (" + 
	            meas.getSpotAttributeDataType(a) + ")\n");
    }
  }
  mview.informationMessage(info.toString());
}
