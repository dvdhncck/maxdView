
public void doIt()
{
  //
  // iterator-demo.java
  // 
  // uses an ExprData.SpotIterator  
  // 
  //

  ExprData.SpotIterator spot_it = edata.getSpotIterator( ExprData.ApplyFilter );

  try
  { 
    while(spot_it.hasNext())
    {
      double mean = .0;
      int count = 0;
      ExprData.MeasurementIterator meas_it = spot_it.getMeasurementIterator( );
  
      while(meas_it.hasNext())
      {
  	 mean += meas_it.value();
  	 count++;
	 meas_it.next();
      }
  
      mean /= (double) count;
  
      String mean_str = String.valueOf(mean);
       
      edata.getSpotTagAttrs().setTagAttr(edata.getSpotName( spot_it.getSpotID() ), "Mean", mean_str);
  
      spot_it.next();
    } 
  }
  catch(ExprData.InvalidIteratorException iie)
  {
  }
}
