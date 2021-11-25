
public void doIt()
{
  int m = 0;
  ExprData.Cluster lastc = null;

  while(m < edata.getNumMeasurements())
  {
    Vector names = new Vector();
    names.addElement(edata.getMeasurementName(m));
    
    ExprData.Cluster newc = edata.new Cluster(String.valueOf(m), 
                                              ExprData.MeasurementName,
	                                      names);
    
    if(lastc != null)
      newc.addCluster(lastc);

    lastc = newc;
  
    m++;
  }
 
  if(lastc != null)
    edata.addCluster(lastc);
       
}
