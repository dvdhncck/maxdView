
public void doIt()
{
   ExprData.DataTags dt = edata.getMasterDataTags();

   for(int s=0; s< edata.getNumSpots(); s++)
   {
     String name = "Y" + String.valueOf((int)(Math.random() * 20000));
     dt.setProbeName(s, name);
   }

}
