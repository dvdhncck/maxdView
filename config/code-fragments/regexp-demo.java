
public void doIt()
{
    File file = new File("demo/short-synthetic-1.maxd.gz");
    if(!file.exists())
    {
	mview.errorMessage("Unable to find demo file " + file.getPath());
	return;
    }
    
    mview.infoMessage("This example shows how the Regular Expression filter works");
    
    String[] args = { "file", "demo/short-synthetic-1.maxd.gz" };
    
    mview.runCommand("Read Native", "load", args);
    
    mview.infoMessage("loading some data...");
    
    maxdView.Plugin rf = mview.startPlugin("RegExp Filter");
    
    if(rf == null)
    {
      mview.errorMessage("Couldn't start RegExp Filter plugin");
      return;
    } 
    else
      mview.infoMessage("The RegExp Filter plugin has started...");
    
    args[0] = "filter"; args[1] =  "";
    
    mview.runCommand(rf, "set", args);
    
    nap();

    args[0] = "gene_names"; args[1] =  "true";
    mview.runCommand(rf, "set", args);

  nap();

  args[0] = "gene_names"; args[1] =  "false";
  mview.runCommand(rf, "set", args);
  args[0] = "probe_name"; args[1] =  "true";
  mview.runCommand(rf, "set", args);

  nap();

  args[0] = "probe_name"; args[1] =  "false";
  mview.runCommand(rf, "set", args);
  args[0] = "spot_name"; args[1] =  "true";
  mview.runCommand(rf, "set", args);

  nap();

  args[0] = "spot_name"; args[1] =  "false";
  mview.runCommand(rf, "set", args);
  args[0] = "annotation"; args[1] =  "true";
  mview.runCommand(rf, "set", args);

  nap();

  args[0] = "annotation"; args[1] =  "false";
  mview.runCommand(rf, "set", args);

  nap();

  args[0] = "case_sensitive"; args[1] =  "true";
  mview.runCommand(rf, "set", args);

  nap();

  args[0] = "case_sensitive"; args[1] =  "false";
  mview.runCommand(rf, "set", args);

  nap();

  args[0] = "probe_name"; args[1] =  "true";
  mview.runCommand(rf, "set", args);

    mview.infoMessage("The checkboxes control which names will be matched against. We will use \"Probe names\".");

  final String demo_s = "y11846";

  args[0] = "filter";

  for(int c=1; c < demo_s.length(); c++)
  {
    args[1] =  demo_s.substring(0, c);
    mview.runCommand(rf, "set", args);
 
    wink();
  }

  nap();

  mview.infoMessage("As a search string is entered into the box, spots with Probe name that do not match disappear.");

  args[0] = "case_sensitive"; args[1] =  "true";
  mview.runCommand(rf, "set", args);

  nap();
  mview.infoMessage("Demonstration finished");

  mview.stopPlugin(rf);
}

// ------------------------------

public void nap()
{
  try
  { 
    Thread.sleep(1000);
  }
  catch(java.lang.InterruptedException ie)
  {
  }
}
public void wink()
{
  try
  { 
    Thread.sleep(200);
  }
  catch(java.lang.InterruptedException ie)
  {
  }
}
