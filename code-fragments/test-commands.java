
public void doIt()
{
  String[] args = null;

 
  args = new String[] 
    { 
      "select",        "all",
      "direction",     "genes",
      "n_components",  "3",
      "mode",          "create_new",
    };

   
   mview.runCommand("SVD", "project", args); 
   
}
