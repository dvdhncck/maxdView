
public void doIt()
{
 
  String[] args = new String[]
  {
    "select",            "list",
    "measurements",      "Ex1 Ex2 Ex3 Ex4",
    "show_statistics",   "false",
    "max_iterations",    "100",
    "tolerance",         "0.05",
    "mode",              "create_new",
    "noise_threshold",   "0.02",
    "new_name_prefix",   "LS:"
  };

  mview.runCommand( "Normalise", "LSNormalise", args );

  args = new String[]
  {
    "select",            "list",
    "measurements",      "Ex1 Ex2 Ex3 Ex4",
    "mode",              "create_new",
    "noise_threshold",   "0.001",
    "new_name_prefix",   "LV:"
  };

  mview.runCommand( "Normalise", "CenterNormalise", args );

  args = new String[]
  {
    "select",            "list",
    "measurements",      "Ex1 Ex2 Ex3 Ex4",
    "show_statistics",   "false",
    "mode",              "create_new",
    "noise_threshold",   "0.004",
    "new_name_prefix",   "GGM:"
  };

  mview.runCommand( "Normalise", "GGMNormalise", args );



}
