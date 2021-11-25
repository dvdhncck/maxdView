
public void doIt()
{
  String[] args1 = new String[]
  {
    "file", "C:/Documents and Settings/dave/bio/data/synthetic/large/small1.dat.gz",
    "mode", "replace",
    "apply_quickset_file", "C:/Documents and Settings/dave/qset.dat",
    "auto_parse", "false",
    "report_status", "false",
  };

   mview.runCommand( "Load Plain Text", "load", args1 );

  String[] args2 = new String[]
  {
    "file", "C:/Documents and Settings/dave/bio/data/synthetic/large/small1.dat.gz",
    "mode", "merge",
    "apply_quickset_file", "C:/Documents and Settings/dave/qset.dat",
    "auto_parse", "false",
    "report_status", "false",
  };

  mview.runCommand( "Load Plain Text", "load", args2 );


}
