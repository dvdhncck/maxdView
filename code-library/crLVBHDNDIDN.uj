
public void doIt()
{
  String[] args = null;

  // ---------------------------------------------------------
  // use 'Load Plain Text' to import a file 
 
  args = new String[] 
    { 
      "file",           "./demo/short-plain-text.dat",
      "delimiter",      "tab",
      "start_line",     "2",
      "end_line",       "101",
      "col_names_line", "1",
      "mode",           "replace",
      "auto_parse",     "true",
      "really_replace", "true", 
      "report_status",  "false"
    };

    
  mview.runCommand("Load Plain Text", "load", args);


  // ---------------------------------------------------------
  // use the name munger to load gene symbols based
  // on the Probe name

  edata.getGeneTagAttrs().addAttr("symbol");

  args = new String[] 
    { 
      "load",            "symbol", 
      "from_column",     "2",
      "of_file",         "./demo/example-mapping.dat", 
      "indexed_by",      "Gene name(s)",
      "in_column",       "1",
      "report_status",   "false"
   };

  mview.runCommand("Name Munger", "load", args);
 
  // ---------------------------------------------------------
  // create a string containing all of measurement names
  // (each enclosed in quotes "like this" )

  String m_names = "( ";

  for(int m=0; m < edata.getNumMeasurements(); m++)
    m_names += "\"" + edata.getMeasurementName( m ) + "\" ";

  m_names += " )";



  // ---------------------------------------------------------
  // build arguments for the 'Simple Math' plugin
  // to calculate the mean values across all measurements

  args = new String[]
  {
    "expr",         "mean" + m_names,
    "apply_filter", "true", 
    "new_name",     "mean(all)"
  };

  mview.runCommand("Simple Maths", "execute", args);



  // ---------------------------------------------------------
  // and calculate the std.deviation in the same way

  args = new String[]
  {
    "expr",         "stddev" + m_names,
    "apply_filter", "true", 
    "new_name",     "stddev(all)"
  };

  mview.runCommand("Simple Maths", "execute", args);



  // ---------------------------------------------------------
  // and then sort by the std.deviation

  args = new String[] 
  { 
    "value",     "stddev(all)",
    "order",     "descending"
  };

    
  mview.runCommand("Sort by Name or Value", "sort", args);


  // ---------------------------------------------------------
  // activate the 'Math Filter'

  args = new String[] 
  { 
      "filter",     "Time_2 > Time_1"
  };

    
  Plugin filter = mview.loadPlugin("Math Filter");
  mview.runCommand(filter, "start", args);



  // ---------------------------------------------------------
  // finally write the data to a new plain text file
  // using the 'Save As Text' plugin

  args = new String[]
  {
    "file",                   "./demo/test.dat",
    "apply_filter",           "true", 
    "significant_digits",     "4", 
    "delimiter",              "tab",
    "row_labels",             "Gene name(s) symbol",
    "tidy_row_labels",        "true", 
    "which_columns",          "all",
    "include_column_labels",  "true",
    "compress",               "false",
    "report_status",          "false"
  }; 

  mview.runCommand("Save As Text", "save", args);
 


  // ---------------------------------------------------------
  // and stop the filter
   
  mview.runCommand(filter, "stop", null); 

}