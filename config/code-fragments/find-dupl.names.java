
public void doIt()
{
  String dupls = "Duplicates:";  
  for(int g=0; g < edata.getNumSpots(); g++)  
  {   
   for(int g2=g+1; g2 < edata.getNumSpots(); g2++) 
   {      
    if(edata.getGeneName(g2).equals(edata.getGeneName(g)))        
      dupls += edata.getGeneName(g2));    
   }  
  } 
  mview.infoMessage(dupls); 
}
