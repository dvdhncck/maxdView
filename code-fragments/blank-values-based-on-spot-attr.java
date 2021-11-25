
public void doIt()
{
  try
  {
    // display a list of Measurements and allow the user to pick one  
    int meas_id = mview.getChoice("Pick the source Measurement", mview.getListOfMeasurementNames());

    // get the corresponding Measurement object
    ExprData.Measurement meas = edata.getMeasurement(meas_id);

    // find out which Spot Attribute contains the "FLAG" Spot Attribute
    int spot_attr_id = meas.getSpotAttributeFromName("FLAG");

    // get an array of data for this Spot Attribute
    int[] flag_data = (int[]) meas.getSpotAttributeData(spot_attr_id);

    // make sure it exists...
    if(flag_data == null)
    {
       mview.alertMessage("This Measurement doesn't have a FLAG Spot Attribute");
       return;
    }
 
    // get the current expression data for the selected Measurement
    double[] meas_data = meas.getData();

    // visit each of the data points and do the modification
    for(int spot=0; spot < flag_data.length; spot++)
    { 
       if(flag_data[spot] != 0)
       {
          meas_data[spot] = Double.NaN;
       }
    }

    // and re-install the expression data array
    edata.setMeasurementData(meas_id, meas_data);

  }
  catch(UserInputCancelled uic)
  { }
}
