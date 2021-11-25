
public void doIt()
{
  //
  // iterator-examples.java
  // 
  // demonstrates each of the features in turn....
  //


  // first a SpotIterator...

  int count = 0;
  ExprData.SpotIterator s_iter = edata.getSpotIterator(  );
  String name = null;

  try
  { 
    while(s_iter.isValid())
    {
      name = edata.getSpotName( s_iter.getSpotID() );
      count++;
      s_iter.next();
      
    } 
  }
  catch(ExprData.InvalidIteratorException iie)
  {
  }
  
  String msg = "There are " + count + " Spots, last one is called " + name;


  // and now a MeasurementIterator, which is essentially the same:

  ExprData.MeasurementIterator m_iter = edata.getMeasurementIterator(  );
  name = null;
  count = 0;

  try
  { 
    while(m_iter.isValid())
    {
      name = edata.getMeasurementName( m_iter.getMeasurementID() );
      count++;
      m_iter.next();      
    } 
  }
  catch(ExprData.InvalidIteratorException iie)
  {
  }

  msg += "\nThere are " + count + " Measurements, last one is called " + name; 

  
  //
  // now combine the two to traverse the matrix row-by-row...
  //
  s_iter = edata.getSpotIterator(  );
  count = 0;

    while(s_iter.isValid())
    {
      m_iter = s_iter.getMeasurementIterator( );

      while(m_iter.isValid())
      {
        count++;
        m_iter.next();      
      } 
 
      s_iter.next();
    } 
 
  msg += "\nThere are a total of " + count + " SpotMeasurements"; 

  //
  // or reverse the iterators to traverse the matrix column-by-column...
  //
  m_iter = edata.getMeasurementIterator( );
  count = 0;

  while(m_iter.isValid())
    {
      s_iter = m_iter.getSpotIterator( );

      while(s_iter.isValid())
      {
        count++;
        s_iter.next();      
      } 
 
      m_iter.next();
    } 
 
 

  //
  // optional iterator features: 'ApplyFilter'
  //
  // (only visits Spots which pass through the currently active filters)
  //

  s_iter = edata.getSpotIterator( ExprData.ApplyFilter );
  count = 0;

  while(s_iter.isValid())
  {
     count++;
     s_iter.next();
  } 
 
  msg += "\nThere are " + count + " unfiltered Spots"; 
 
  //
  // optional iterator features: 'SelectedDataOnly'
  //
  // (only visits things in the current data selection)
  //

  s_iter = edata.getSpotIterator( ExprData.SelectedDataOnly );
  count = 0;

  while(s_iter.isValid())
  {
     count++;
     s_iter.next();
  } 
 
  msg += "\nThere are " + count + " selected Spots"; 
 
  //
  // optional iterator features: 'ApplyFilter' and 'SelectedDataOnly'
  //
  // (combine the previous two constraints)
  //
  s_iter = edata.getSpotIterator( ExprData.ApplyFilter | ExprData.SelectedDataOnly );
  count = 0;

  while(s_iter.isValid())
  {
     count++;
     s_iter.next();
  } 
 
  msg += "\nThere are " + count + " unfiltered and selected Spots";

  //
  // optional iterator features: 'TraversalOrder'
  //
  // (visits things in the order they are shown in the main display) 

  s_iter = edata.getSpotIterator( ExprData.TraversalOrder );
  count = 0;

  try
  {
    String first_name =  edata.getSpotName( s_iter.getSpotID() );
    String last_name = null;
    while(s_iter.isValid())
    {
       last_name = edata.getSpotName( s_iter.getSpotID() );
       count++;
       s_iter.next();
    } 
 
    msg += "\nDisplayed first is " + first_name + " and last is " + last_name;
  }
  catch(ExprData.InvalidIteratorException iie)
  {
  }
 

  //
  // optional iterator features: 'TraversalOrder' and 'SelectedDataOnly'
  //

  s_iter = edata.getSpotIterator( ExprData.TraversalOrder | ExprData.SelectedDataOnly );
  count = 0;

  try
  {
    String first_name = edata.getSpotName( s_iter.getSpotID() );
    String last_name  = first_name;
    while(s_iter.isValid())
    {
       last_name = edata.getSpotName( s_iter.getSpotID() );
       count++;
       s_iter.next();
    } 
 
    if(first_name != null)
      msg += "\nSelected first is " + first_name + " and last is " + last_name;
  }
  catch(ExprData.InvalidIteratorException iie)
  {
  }
 
  //
  // optional iterator features: 'TraversalOrder' and 'SelectedDataOnly'
  // (as above, but with a MeasurementIterator)

  m_iter = edata.getMeasurementIterator( ExprData.TraversalOrder | ExprData.SelectedDataOnly );
  count = 0;

  try
  {
    String first_name = edata.getMeasurementName( m_iter.getMeasurementID() );
    String last_name  = first_name;
    while(m_iter.isValid())
    {
       last_name = edata.getMeasurementName( m_iter.getMeasurementID() );
       count++;
       m_iter.next();
    } 
 
    if(count > 0)
      msg += "\n" + count + " selected Measurements, from " + first_name + " to " + last_name;
  }
  catch(ExprData.InvalidIteratorException iie)
  {
  }

  // and finish....
 
  mview.infoMessage( msg );
    
}
