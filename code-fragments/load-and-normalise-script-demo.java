
public void doIt()
{
  String[] args = null;

  // ---------------------------------------------------------
  //
  // read a native file
 
  args = new String[] 
    { 
      "file",            "../data/cogeme/Cogeme_Chemostats.maxd.gz",
      "mode",            "replace",
      "really_replace",  "true",
    };

    
  mview.runCommand("Read Native", "load", args);

  // ---------------------------------------------------------
  //
  // use the name munger to load gene symbols based
  // on the Probe name

  edata.getGeneTagAttrs().addAttr("symbol");

  args = new String[] 
    { 
      "load",            "Gene name(s)", 
      "from_column",     "2",
      "of_file",         "../data/cogeme/Cogeme_Chemostat_probe_symbols.dat", 
      "indexed_by",      "Probe name",
      "in_column",       "1",
      "report_status",   "false"
   };

  mview.runCommand("Name Munger", "load", args);
 

  // ---------------------------------------------------------
  //
  // create a string containing all of measurement names
  // (each enclosed in quotes "like this" )

  String m_names = "( ";

  int count = 0;
  for(int m=0; m < edata.getNumMeasurements(); m++)
    if( edata.getMeasurementName( m ).startsWith( "F635" ) )
    {
      if (count > 0 )
	m_names += ",";
      count++;
      m_names += "\"" + edata.getMeasurementName( m ) + "\" ";
    }

  m_names += " )";

  // uses this string to build arguments to get  the 'Simple Math'
  // plugin to calculate the mean values across all measurements

  args = new String[]
  {
    "expr",         "mean" + m_names,
    "apply_filter", "true", 
    "new_name",     "mean(F635...)"
  };

  mview.runCommand("Simple Maths", "execute", args);



  // ---------------------------------------------------------
  //
  // and calculate the std.deviation in the same way

  args = new String[]
  {
    "expr",         "stddev" + m_names,
    "apply_filter", "true", 
    "new_name",     "stddev(F635...)"
  };

  mview.runCommand("Simple Maths", "execute", args);



  // ---------------------------------------------------------
  //
  // and then sort by the std.deviation

  args = new String[] 
  { 
    "value",     "stddev(F635...)",
    "order",     "descending"
  };

    
  mview.runCommand("Sort by Name or Value", "sort", args);


  // ---------------------------------------------------------
  //
  // now create a new Colouriser and that will be used for the normalised measurements

  java.util.Hashtable attrs = new java.util.Hashtable();
  attrs.put( "LEVELS", "16" );
  dplot.addColouriser("Equalising", "normalised", attrs );


  // ---------------------------------------------------------
  //
  // and then Normalise pairs using the Intensity Dependant LOWESS method

  for( int i = 1; i <= 7; i++ )
  {
    String red_name   = "F635_Chemostat" + i;
    String green_name = "F532_Chemostat" + i;

    String measurements_list = red_name + "," + green_name;

    String new_name   = "Chmst" + i;

    args = new String[]
    {
      "measurements", measurements_list,
      "select", "list",
 
       "red_data",   red_name,
       "green_data", green_name,

       "apply_filter", "false",
       "mode", "create_new",

       "show_ma_plot", "false",

       "new_name_prefix", new_name
    };
 
    mview.runCommand( "Normalise", "IDNormalise", args );
 
    dplot.setColouriserForMeasurement( new_name, "normalised" );
 
 }


}