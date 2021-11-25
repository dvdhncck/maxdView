
public void doIt()
{
  ExprData.Measurement m = edata.getMeasurement(0);
  int n_atts = m.getNumSpotAttributes();
  String info =  n_atts + " attrs:\n";
  if(n_atts > 0)
  {
    for(int a=0; a < n_atts; a++)
      info += (" " + a + ". " + 
	       m.getSpotAttributeName(a) + " (" + 
	       m.getSpotAttributeDataType(a) + ")\n");
  }
  mview.informationMessage(info);
  String name = m.getName();
  if(n_atts > 0)
  {
    for(int a=0; a < n_atts; a++)
    {
      
      double[] d_data = new double[edata.getNumSpots()];

      String type_name = m.getSpotAttributeDataType(a);
      
      if(type_name.equals("DOUBLE"))
      {
        d_data = (double[]) m.getSpotAttributeData(a);
      }
      
      if(type_name.equals("INTEGER"))
      {
	int[] data = (int[]) m.getSpotAttributeData(a);
	for(int d=0; d < data.length; d++)
	  d_data[d] = (double) data[d];
      }
      if(type_name.equals("CHAR"))
      {
	char[] data = (char[]) m.getSpotAttributeData(a);
	for(int d=0; d < data.length; d++)
	  d_data[d] = (data[d] == 'y') ? 1.0 : -1.0;
      }


      String new_name = (m.getName() + "_" + m.getSpotAttributeName(a));
        
      ExprData.Measurement m1 = edata.new Measurement(new_name, ExprData.ErrorDataType, d_data);
        
      edata.addOrderedMeasurement(m1);
      
   }
  }
}
