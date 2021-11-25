
public void doIt()
{
  //
  // iterator-examples-two.java
  // 
  // combining iterators with Measurement Attributes and NameAttributes ...
  //


  // first, using a SpotIterator to select all unfiltered Spots
  // and set a NameAttribute for Spot Name:
  //
  ExprData.TagAttrs sta = edata.getSpotTagAttrs();


  // clear the "state" NameAttribute for all Spots

  ExprData.SpotIterator s_iter = edata.getSpotIterator( );
  try
  { 
    while(s_iter.isValid())
    {
      String spot_name = edata.getSpotName( s_iter.getSpotID() );

      sta.setTagAttr( spot_name, "state", "");
 
      s_iter.next();     
    } 
  }
  catch(ExprData.InvalidIteratorException iie)
  {
  }        
 
  // now set the "state" NameAttribute to "F" for filtered Spots
 
  s_iter = edata.getSpotIterator( ExprData.ApplyFilter );

  try
  { 
    while(s_iter.isValid())
    {
      String spot_name = edata.getSpotName( s_iter.getSpotID() );

      sta.setTagAttr( spot_name, "state", "F");
 
     s_iter.next();     
    } 
  }
  catch(ExprData.InvalidIteratorException iie)
  {
  }

  // now set the "state" NameAttribute to "S" for selected Spots
 
  s_iter = edata.getSpotIterator( ExprData.SelectedDataOnly );

  try
  { 
    while(s_iter.isValid())
    {
      String spot_name = edata.getSpotName( s_iter.getSpotID() );

      sta.setTagAttr( spot_name, "state", "S");
 
     s_iter.next();     
    } 
  }
  catch(ExprData.InvalidIteratorException iie)
  {
  }
    
 
  // we need to generate a data update 

  edata.generateDataUpdate( ExprData.NameChanged );
    
}
