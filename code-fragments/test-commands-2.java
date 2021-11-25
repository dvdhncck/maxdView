
public void doIt()
{
  String[] args = new String[] 
  {
    "filter",    " Ex2 > (2 *Ex1) and Ex3 > (2 * Ex2)"
  };

  Plugin my_filter = mview.loadPlugin( "Math Filter" );

  edata.clearSpotSelection( );

  mview.runCommand( my_filter, "start", args );

  edata.addFilteredSpots();

  mview.runCommand( my_filter, "stop", null );
}
