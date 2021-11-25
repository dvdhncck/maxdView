public void doIt()
{

   final int ns = edata.getNumSpots();

   double[] ccount = new double[ns];

   for(int s=0; s < ns; s++)
   {
      ccount[s] = (double)edata.inVisibleClusters(s);
   }


   edata.addOrderedMeasurement(edata.new Measurement("in clusters", ExprData.UnknownDataType, ccount));

}