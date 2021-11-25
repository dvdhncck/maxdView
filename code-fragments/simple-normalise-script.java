
public void doIt()
{
   // simple use of the Normaliser plugin 

    String[] args = new String[]
    {
      "measurements", measurements_list,
      "select", "list",
 
       "red_data",   "F635_Chemostat2",
       "green_data", "F532_Chemostat2",

       "apply_filter", "false",
       "mode", "create_new",

       "show_ma_plot", "true",

       "new_name_prefix", new_name
    };
 
    mview.runCommand( "Normalise", "IDNormalise", args );

}
