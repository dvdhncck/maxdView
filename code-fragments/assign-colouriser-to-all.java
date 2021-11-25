
// assign Colourisers en masse

public void doIt()
{
   try
   {
      
      String[] cn = dplot.getColouriserNameArray();

      int co = mview.getChoice("Pick one:", cn );      

      for(int m=0; m < edata.getNumMeasurements(); m++)
      {
         dplot.setColouriserForMeasurement(m, cn[co]);
      }

   }
   catch(UserInputCancelled e)
   {
   }
}
