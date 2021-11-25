
public void doIt()
{
   String[] args;

   // -----------------------------------------------
   // add a new Gene NameAttribute tag 

   edata.getGeneTagAttrs().addAttr("demo");
 
   // -----------------------------------------------
   // and copy existing 'desc' tags to it

   args = new String[] 
    { 
      "from",  "desc",
      "to",    "demo",

      "report_status", "false"
    };

   
   mview.runCommand("Name Munger", "copy", args); 

   // ------------------------------------------------
   // now do substitute " " with "_" in the new tag

   args = new String[] 
    { 
      "translate",   "demo",
      "mode",        "substitute",
      "substitute",  " ",
      "with",        "_",

      "apply_filter",  "true", 
      "report_status", "false"
    };

   
   mview.runCommand("Name Munger", "translate", args); 

   // ------------------------------------------------
   // and convert all text in the tag to upper case

   args = new String[] 
    { 
      "translate",   "demo",
      "mode",        "to_uppercase",

      "apply_filter", "true", 
      "report_status", "false"
    };

    
   // -------------------------------------------------
   // and finally write the tags to a file
   
   mview.runCommand("Name Munger", "translate", args); 
    
   args = new String[] 
    { 
      "save",          "demo",
      "indexed_by",    "Spot name",
      "to_file",       "/home/dave/test1.txt",

      "apply_filter",  "true", 
      "report_status", "false"
    };

   
   mview.runCommand("Name Munger", "save", args); 
   
    
}
