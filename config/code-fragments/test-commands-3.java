
public void doIt()
{
  String[] args = new String[] 
    { 
      "translate",   "desc",
      "mode",        "substitute",
      "substitute",  "blank",
      "with",        "monkey!",

      "apply_filter", "true", 
      "report_status", "false"
    };

   
   mview.runCommand("Name Munger", "translate", args); 
   
}
