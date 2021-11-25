
public void doIt()
{
 String[] args = new String[]
 {
  "file", "/home/dave/test.dat",
  "significant_digits", "3",
  "delimiter", "space",
  "missing_value", "FISH",
  "apply_filter", "false",
  "which_columns", "list",
  "column_list", "Ex1 Ex2.sid Ex3.isid",
  "include_column_labels", "true",
  "tidy_column_labels", "false",
  "row_labels", "Gene.symb Spot.COMMENT",
  "tidy_row_labels", "false",
  "compress", "false",
  "force_overwrite", "true",
  "report_status", "false",
 };

 mview.runCommand( "Save As Text", "set", args );


}
