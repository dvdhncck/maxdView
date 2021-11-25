
public void doIt()
{
  String[] args = null;

  // --------------------------------------------------------
  // start the plugin with some initial settings

  args = new String[] 
  { 
      "n_groups",         "9",
      "adjust_what",      "random",
      "adjust_towards",   "mean spot",
      "auto_remove_empty","false",
      "select",           "list",
      "measurements",     "Ex2 Ex11 Ex7 Ex12 Ex17"
  };

   
  Plugin pl = mview.loadPlugin("Super Grouper");

  mview.runCommand(pl, "start", args); 

}
