
public void doIt()
{
  

  String[] args = new String[]
  {
  "select", "list",
  "measurements", "Ex1 Ex2 Ex3",
  "target_spot_tag", "Spot name",
  "target_spot_value", "spot180",
  "n_spots", "20",
  "metric", "distance",
  };

  mview.runCommand( "Profile Filter", "start", args );


}
