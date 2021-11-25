
public void doIt()
{
  final int n_steps = 10;
  final int iters_per_step = 1000;

  String[] args = null;

  // --------------------------------------------------------
  // start the plugin with some initial settings

  args = new String[] 
  { 
      "n_groups",         "9",
      "adjust_what",      "biggest",
      "adjust_towards",   "mean",
      "auto_remove_empty","false"
  };

   
  Plugin pl = mview.loadPlugin("Super Grouper");

  mview.runCommand(pl, "start", args); 

  // --------------------------------------------------------
  // randomise the profiles

  if( edata.getNumMeasurements() < 5)
    mview.runCommand(pl, "permutations", null); 
  else
    mview.runCommand(pl, "randomise", null); 

  // --------------------------------------------------------
  //
  // repeatedly:
  //
  //   iterate a fixed number times
  //   at progressively lower
  //   rate and noise levels
  //

  for(int step=n_steps; step > 1; step--)
  {
    // -----------------------------------------------------
    // set the noise and rate values for this step

    double rate  = (double)step / (double)(10*n_steps);
    double noise = (rate / 10.0);

    args = new String[] 
    { 
      "adjust_rate",  String.valueOf( rate ),
      "noise_level",  String.valueOf( noise )
    };

    mview.runCommand(pl, "set",  args); 

    // -----------------------------------------------------
    // and auto-adjust for N iterations

    args = new String[] 
    { 
        "iterations",  String.valueOf( iters_per_step )
    };

    mview.runCommand(pl, "autoAdjust",  args); 

    // -----------------------------------------------------
    // remove any empty groups
    mview.runCommand(pl, "removeEmpty", null); 

  }

  // --------------------------------------------------------
  // then create the clusters

  args = new String[] 
  { 
      "mode",     "most accurate",
      "new_name", "demo"
  };

  mview.runCommand(pl, "makeClusters",  args); 
   
  // --------------------------------------------------------
  // finally  shutdown the plugin

  mview.runCommand(pl, "stop", null); 


  // --------------------------------------------------------
  // and sort the clusters

  mview.runCommand("Sort Clusters", "sortSpots", null); 

}
