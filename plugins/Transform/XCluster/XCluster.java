import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
//import ExprData;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

//
// interface to X
//

public class XCluster extends JFrame implements ExprData.ExprDataObserver, Plugin
{
    public XCluster(maxdView mview_)
    {
	super("XCluster by Gavin Sherlock");

	//System.out.println("++ XCluster is constructed ++");

	mview = mview_;
	edata = mview.getExprData();
	dplot = mview.getDataPlot();

	mview.decorateFrame( this );

	addWindowListener(new WindowAdapter() 
			  {
			      public void windowClosing(WindowEvent e)
			      {
				  cleanUp();
			      }
			  });

    }

    public void cleanUp()
    {
	saveProps();
	
	//System.out.println("++ XCluster has been stopped ++");
	edata.removeObserver(this);
	setVisible(false);
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  plugin implementation
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void startPlugin()
    {
	//System.out.println("++ XCluster has been started ++");
	addComponents();
	pack();
	setVisible(true);
	edata.addObserver(this);
    }

    public void stopPlugin()
    {
	cleanUp();
    }
  
    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("XCluster", "transform", 
					 "Interface to the XCluster algorithms", 
					 "Requires XCluster by Gavin Sherlock, <BR>" + 
					 "See <A HREF=\"http://www-genome.standford.edu/~sherlock/\">" + 
					 "http://www-genome.standford.edu/~sherlock/</A>",
					 1, 1, 0);
	return pinf;
    }

    public PluginCommand[] getPluginCommands()
    {
	return null;
    }

    public void   runCommand(String name, String[] args, CommandSignal done) 
    {
	if(done != null)
	    done.signal();
    } 
    
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  observer 
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // the observer interface of ExprData notifies us whenever something
    // interesting happens to the data as a result of somebody (including us) 
    // manipulating it
    //
    public void dataUpdate(ExprData.DataUpdateEvent due)
    {
    }

    public void clusterUpdate(ExprData.ClusterUpdateEvent cue)
    {
	// reset the label?
    }

    public void measurementUpdate(ExprData.MeasurementUpdateEvent mue)
    {
	switch(mue.event)
	{
	case ExprData.SizeChanged:
	case ExprData.ElementsAdded:
	case ExprData.ElementsRemoved:
	case ExprData.NameChanged:
	case ExprData.OrderChanged:
	    meas_list.setModel(new MeasListModel());
	    // updateDisplay();
	    
	    break;
	}	
    }
    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  command handlers
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private void saveProps()
    {
	mview.putBooleanProperty("XCluster.apply_filter", apply_filter_jchkb.isSelected());

	mview.putProperty("XCluster.prog", prog_loc_jtf.getText() );
	mview.putProperty("XCluster.tmp_dir", tmp_dir_jtf.getText() );
	mview.putBooleanProperty("XCluster.del_files", delete_files_jchkb.isSelected());

	mview.putBooleanProperty("XCluster.none", no_part_jrb.isSelected());

	mview.putBooleanProperty("XCluster.k_means", kmeans_part_jrb.isSelected());
	mview.putProperty("XCluster.k_val", k_val_jtf.getText());

	mview.putBooleanProperty("XCluster.som", som_part_jrb.isSelected());
	mview.putProperty("XCluster.som_rows", som_rows_jtf.getText());
	mview.putProperty("XCluster.som_cols", som_cols_jtf.getText());
	
	mview.putBooleanProperty("XCluster.pearson", pears_dist_jrb.isSelected());
	mview.putBooleanProperty("XCluster.euclidian", eucl_dist_jrb.isSelected());
	mview.putBooleanProperty("XCluster.centered", centered_met_jchkb.isSelected());
	mview.putBooleanProperty("XCluster.log_trans", log_trans_jchkb.isSelected());
	
	mview.putBooleanProperty("XCluster.by_spots", cluster_spots_jrb.isSelected());
	mview.putBooleanProperty("XCluster.by_meas", cluster_meas_jrb.isSelected());
    }

    public void cluster()
    {
	saveProps();
	ClusteringThread ct = new ClusteringThread();
	ct.start();
    }

    // ---------------- --------------- --------------- ------------- ------------

    public void cancel()
    {
	cleanUp();
    }

    public class ClusteringThread extends Thread
    {
	public void run()
	{
	    cluster();
	}

	//
	// create a cluster hierarchy
	//
	public void createClustersFromIndices(String cname, int[] clust_allocs, int[] X_to_maxd, int index_type)
	{
	    //
	    //System.out.println("expecting " +  clust_allocs.length + " clusters...");
	    //System.out.println("       for " +  X_to_maxd.length + " things...");
	    
	    ExprData.Cluster local_root = edata.new Cluster(cname);
	    
	    Hashtable clust_ids = new Hashtable();  // maps X name -> new cluster name
	    
	    edata.addCluster(local_root);
	}
	
	public void cluster()
	{
	    int n_meas_out = 0;
	    int n_spots_out = 0;

	    final String temp_dir = tmp_dir_jtf.getText();
	    final String temp_path = temp_dir + System.getProperty("file.separator");
	    final File dir = new File(temp_path);
	    if(!dir.isDirectory())
	    {
		mview.alertMessage("Cannot find temporary file directory\n" + 
				   "(called '" +dir.getPath() + "')\n" + 
				   "Check the 'Directory for temporary files' option");
		return;
	    }

	    final File file = new File(temp_path + "input.txt");
	    
	    // sanity check
	    for(int m=meas_list.getMinSelectionIndex(); m <= meas_list.getMaxSelectionIndex(); m++)
	    {
		if(meas_list.getSelectionModel().isSelectedIndex(m))
		{
		    n_meas_out++;
		}
	    }
	    
	    if(n_meas_out == 0)
	    {
		mview.errorMessage("Cannot cluster: No Measurements are selected");
		return;
	    }
	    
	    for(int s=0; s < edata.getNumSpots(); s++)
	    {
		if((!apply_filter_jchkb.isSelected()) || (!edata.filter(s)))
		{
		    {
			n_spots_out++;
		    }
		}
	    }
	    
	    if(n_spots_out == 0)
	    {
		mview.errorMessage("Cannot cluster: No Spots are selected.");
		return;
	    }

	    ProgressOMeter pm = new ProgressOMeter("Clustering");
	    pm.startIt();
	    pm.setMessage("Preparing...");
	    
	    int total_elems = 0;

	    // convert (filtered) spots indices to a contiguous ordering 
	    //
	    int[] cluster_to_spot = new int[n_spots_out];

	    if(apply_filter_jchkb.isSelected())
	    {
		int c2s = 0;
		for(int s=0; s < edata.getNumSpots(); s++)
		{
		    if((!apply_filter_jchkb.isSelected()) || (!edata.filter(s)))
		    {
			{
			    cluster_to_spot[c2s++] = s;
			}
		    }
		}
	    }
	    else
	    {
		// no filter, use all spots in their current order
		for(int s=0; s < n_spots_out; s++)
		    cluster_to_spot[s] = s;
	    }

	    // convert (selected) measurement indices into a contiguous ordering 
	    //
	    int[] cluster_to_meas = new int[n_meas_out];
	    int cm = 0;
	    for(int m=meas_list.getMinSelectionIndex(); m <= meas_list.getMaxSelectionIndex(); m++)
	    {
		if(meas_list.getSelectionModel().isSelectedIndex(m))
		{
		    int mi = edata.getMeasurementAtIndex(m);
		    cluster_to_meas[cm++] = mi;
		    
		    System.out.println("including " + edata.getMeasurementName(mi) + "...");
		}
	    } 

	    // now we know which data we are dealing with....write the input file
	    //
	    int[] back_convert = null;
	    int thing_count = 0;
	    int att_count = 0;
	    int index_type = -1;

	    
	    pm.setMessage("Writing file...");

	    try
	    {
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		
		writer.write("UID\tBLANK\tGWEIGHT");

		for(int m=0; m < cluster_to_meas.length; m++)
		{
		    writer.write("\t" + edata.getMeasurementName(cluster_to_meas[m]));
		}
		
		writer.write("\n");
		writer.write("EWEIGHT\t\t");
		for(int m=0; m < cluster_to_meas.length; m++)
		{
		    writer.write("\t1");
		}
		writer.write("\n");

		for(int s=0; s < n_spots_out; s++)
		{
		    final int sid = cluster_to_spot[s];

		    writer.write(edata.getSpotName(sid) + "\t\t1");

		    for(int m=0; m < n_meas_out; m++)
		    {
			writer.write("\t" + edata.eValue(cluster_to_meas[m],sid));
		    }
		    
		    writer.write("\n");
		}

		writer.flush();

		pm.setMessage("File written, starting XCluster...");

	    }
	    catch (java.io.IOException ioe)
	    {
		pm.stopIt();
		mview.errorMessage("Unable to write to '" + file.getName() + "'");
		return;
	    }

	   	    
	    pm.setMessage("Clustering..." + n_meas_out + "x" + n_spots_out);
	    
	    
	    String clust_result = runXCluster( prog_loc_jtf.getText(), temp_path );

	    if(delete_files_jchkb.isSelected())
		file.delete();

	    if(clust_result == null)
	    {
		pm.setMessage("Reading result...");

		// =======================================================================
		// read results after clustering with no partition
		// =======================================================================
		
		if(no_part_jrb.isSelected())
		{
		    File result =  null;
		    File cdt_result = new File( temp_path + "input.cdt");
		    
		    if(!cdt_result.canRead())
		    {
			pm.stopIt();
			mview.alertMessage("Unable to read temporary output file,\n" + 
					   "(in '" + cdt_result.getPath() + "'\nClustering aborted.");
			return;
		    }
		    if( cluster_meas_jrb.isSelected() )
		    {
			result =  new File(temp_path + "input.atr");
			
			if(!result.canRead())
			{
			    pm.stopIt();
			    mview.alertMessage("Unable to read temporary output file,\n" + 
					       "(from '" + result.getPath() + "')\nClustering aborted.");
			    return;
			}

			ExprData.Cluster cl = loadClusterDataFromStanfordFormatFile( "", cdt_result, null, result );

			if(cl != null)
			{
			    cl.setName("XCluster: " + n_meas_out + " Measurements");
			    edata.addCluster(cl);
			}
		    }
		    else
		    {
			result = new File(temp_path + "input.gtr");
			
			if(!result.canRead())
			{
			    pm.stopIt();
			    mview.alertMessage("Unable to read temporary output file,\n" + 
					       "(from '" + result.getPath() + "')\nClustering aborted.");
			    return;
			}

			ExprData.Cluster cl = loadClusterDataFromStanfordFormatFile( "", cdt_result, result, null );

			if(cl != null)
			{
			    cl.setName("XCluster: " + n_spots_out + " Spots");
			    edata.addCluster(cl);
			}
		    }

		    if(delete_files_jchkb.isSelected())
			if(result != null)
			    result.delete();
		    
		    cdt_result.delete();
		}

		// =======================================================================
		// read results after clustering with k-means partition
		// =======================================================================
		
		if(kmeans_part_jrb.isSelected())
		{
		   int missing_files = 0;
		   int empty_parts = 0;
		   String name_p = "input_" + kmeans_k + "_k";
		   
		   ExprData.Cluster new_cl = edata.new Cluster("XCluster:k-means, k=" + kmeans_k);

		   System.out.println("loading results after k=" + kmeans_k + " clustering");
			
		   for(int k=0; k < kmeans_k; k++)
		   {
		       try
		       {
			   System.out.println("looking for data for k=" + k);
			   
			   ExprData.Cluster cl = null;
			   String cl_name_prefix = "k=" + k + ":";
			   File cdt_result = new File(temp_path + name_p + k + ".cdt");
			   
			   boolean skip = false;
			   
			   if(!cdt_result.canRead())
			   {
			       //pm.stopIt();
			       //mview.alertMessage("Unable to read temporary output file,\n" + 
			       //	      "(from '" + cdt_result.getPath() + "')");
			       missing_files++;
			       skip = true;
			       
			   }
			   File result = null;
			   
			   if(!skip)
			   {
			       if( cluster_meas_jrb.isSelected() )
			       {
				   result = new File(temp_path + name_p + k + ".atr");
				   
				   if(!result.canRead())
				   {
				       //pm.stopIt();
				       missing_files++;
				       //mview.alertMessage("Unable to read temporary output file,\n" + 
				       //	      "(from '" + result.getPath() + "')");
				   }
				   else
				       cl = loadClusterDataFromStanfordFormatFile( cl_name_prefix, cdt_result, null, result );
			       }
			       else
			       {
				   result = new File(temp_path + name_p + k + ".gtr");
				   
				   if(!result.canRead())
				   {
				       //pm.stopIt();
				   missing_files++;
				   //mview.alertMessage("Unable to read temporary output file,\n" + 
				   //	      "(from '" + result.getPath() + "')");
				   }
				   else
				       cl = loadClusterDataFromStanfordFormatFile( cl_name_prefix, cdt_result, result, null );
			       }
			       
			       if(cl != null)
			       {
				   cl.setName( cl_name_prefix );
				   
				   System.out.println("one or more elements found for k=" + k + "...");
				   
				   // make all the children of each partition the same colour
				   recursivelyColourCluster(cl,  cl.getColour());
				   
				   new_cl.addCluster(cl);
			       }
			       else
			       {
				   empty_parts++;
				   System.out.println("nothing found for k=" + k + "...");
			       }
			   }
			   
			   if(delete_files_jchkb.isSelected())
			   {
			       cdt_result.delete();
			       result.delete();
			   }
		       }
		       catch(Exception e)
		       {
			   System.out.println("...Exception!\n   " + e);
		       }
		       
		       System.out.println("....k=" + k + " finished");

		   }

		   if(empty_parts > 0)
		   {
		       pm.stopIt();
		       String msg = (empty_parts == 1) ? "One partition was" : (empty_parts + " parititions were");
		       mview.alertMessage("Warning: " + msg + " empty.");
		   }
		   
		   edata.addCluster(new_cl);
		}

		// =======================================================================
		// read results after clustering with SOM partition
		// =======================================================================
		
		if(som_part_jrb.isSelected())
		{
		    String name_p = "input_" + som_cols + "_" + som_rows + "_som";
		    
		    ExprData.Cluster new_cl = edata.new Cluster("XCluster:SOM");

		    for(int k=0; k < (som_rows * som_cols); k++)
		    {
			String cl_name_prefix = "c=" + (k / som_rows) + ",r=" + (k % som_rows) + ":";
		       
			ExprData.Cluster cl = null;
			File cdt_result = new File(temp_path + name_p + k + ".cdt");
			if(!cdt_result.canRead())
			{
			    pm.stopIt();
			    mview.alertMessage("Unable to read temporary output file,\n" + 
					       "(from '" + cdt_result.getPath() + "')\nClustering aborted.");
			    return;
			}
			File result = null;
			
			if( cluster_meas_jrb.isSelected() )
			{
			    result = new File(temp_path + name_p + k + ".atr");
			    
			    if(!result.canRead())
			    {
				pm.stopIt();
				mview.alertMessage("Unable to read temporary output file,\n" + 
						   "(from '" + result.getPath() + "')\nClustering aborted.");
			       return;
			    }

			    cl = loadClusterDataFromStanfordFormatFile( cl_name_prefix, cdt_result, null, result );
			}
			else
			{
			    result = new File(temp_path + name_p + k + ".gtr");
			    
			    if(!result.canRead())
			    {
				pm.stopIt();
				mview.alertMessage("Unable to read temporary output file,\n" + 
						   "(from '" + result.getPath() + "')\nClustering aborted.");
				return;
			    }

			    cl = loadClusterDataFromStanfordFormatFile( cl_name_prefix, cdt_result, result, null );
			}
			
			if(delete_files_jchkb.isSelected())
			{
			    result.delete();
			    cdt_result.delete();

			    // also delete the other files made by SOM

			    File evo_f = new File(temp_path + name_p + k + ".evo");
			    evo_f.delete();
			    File cor_f = new File(temp_path + name_p + k + ".cor");
			    cor_f.delete();
			    File som_f = new File(temp_path + "input_" + som_cols + "_" + som_rows + ".som");
			    som_f.delete();
			}
		       
			if(cl != null)
			{
			    cl.setName( cl_name_prefix );
			    
			    // make all the children of each partition the same colour
			    recursivelyColourCluster(cl,  cl.getColour());
			    
			    new_cl.addCluster(cl);
			}
		    }

		    edata.addCluster(new_cl);
		}
		
		pm.stopIt();
	    }
	    else
	    {
		pm.stopIt();
		mview.errorMessage(clust_result);
	    }

	}

    }

    private void recursivelyColourCluster(ExprData.Cluster cl, Color col)
    {
	cl.setColour(col);

	Vector chld = cl.getChildren();
	if(chld != null)
	{
	    for(int ci=0; ci < chld.size(); ci++)
	    {
		ExprData.Cluster ch = (ExprData.Cluster) chld.elementAt(ci);
		
		recursivelyColourCluster(ch, col);
	    }
	}
    }

    private int som_rows = 1;
    private int som_cols = 1;
    private int kmeans_k = 1;
    
    private Process process = null;

    // returns null to signify success, or an error message for failure
    //
    private String runXCluster(String prog, String temp_path)
    {
	if((prog == null) || (prog.length() == 0))
	   return ("No location specified for the program");
	File binary = new File(prog);
	if(!binary.exists())
	    return ("The file '" + prog + "' does not exist");
	if(binary.isDirectory())
	    return ("'" + prog + "' is a directory, not an executable file");
	if(!binary.canRead())
	    return ("'" + prog + "' exists but is not readable (check the permissions)");
	
	String args = "-f \"" + temp_path + "input.txt\"";

	int g_val = 0;
	if(cluster_spots_jrb.isSelected())
	    g_val = centered_met_jchkb.isSelected() ? 2 : 1;
	args += (" -g " + g_val);

	int e_val = 0;
	if(cluster_meas_jrb.isSelected())
	    e_val = centered_met_jchkb.isSelected() ? 2 : 1;
	args += (" -e " + e_val);
	
	if(pears_dist_jrb.isSelected())
	    args += (" -p 1");
	if(eucl_dist_jrb.isSelected())
	    args += (" -p 0");

	if(kmeans_part_jrb.isSelected())
	{
	    kmeans_k = 1;
	    try
	    {
		kmeans_k = new Integer( k_val_jtf.getText() ).intValue();
	    }
	    catch(NumberFormatException nfe)
	    {
	    }	   

	    args += (" -k " + kmeans_k);
	}
	
	if(som_part_jrb.isSelected())
	{
	    try
	    {
		som_rows = new Integer( som_rows_jtf.getText() ).intValue();
	    }
	    catch(NumberFormatException nfe)
	    {
	    }	    
	    try
	    {
		som_cols = new Integer( som_cols_jtf.getText() ).intValue();
	    }
	    catch(NumberFormatException nfe)
	    {
	    }	    

	    args += (" -s 1 -x " + som_cols + " -y " + som_rows);
	}
	
	if(log_trans_jchkb.isSelected())
	{
	    // checkDataCanBeLogged();

	    args += (" -l 1");
	}
	else
	    args += (" -l 0");

	// args += (" -u " + temp_path);

	String exec_cmd = "\"" + prog + "\" " + args;
	
	try
	{
	    BufferedReader br  = null;
	    BufferedWriter bw = null;
	    
	    System.out.println("running...: " + exec_cmd);

	    process = Runtime.getRuntime().exec(exec_cmd);
	    
	    // any error message?
	    StreamGobbler errorGobbler = new StreamGobbler( process.getErrorStream(), "ERROR" );
	    
	    // any output?
	    StreamGobbler outputGobbler = new StreamGobbler( process.getInputStream(), "OUTPUT" );
	    
	    // kick them off
	    errorGobbler.start();
	    outputGobbler.start();
	    
	    try
	    {
		process.waitFor();
	    }
	    catch (InterruptedException ie)
	    {
	    }
	    
	    System.out.println("...finished");
	    
	    if(process.exitValue() == 0)
	    {
		System.out.println("INFO: child process exited happily");
		
		return null;
	    }
	    else
	    {
		
/*
  StringWriter sw = new StringWriter();
  
  // record the error stream
  InputStream outstr = process.getInputStream();
  
  System.out.println("collating output stream");
  
  try
  {
  int ch = 0;
  while(ch >= 0)
  {
  if(ch > 0)
  sw.write(ch);
  ch = outstr.read();
  }
  }
  catch(IOException ioe)
  {
  System.out.println("IOException when collating output stream");
  }
  sw.write("\n");
  
  InputStream errors = process.getErrorStream();
  
  System.out.println("collating error stream");
  
  try
  {
  int ch = 0;
  while(ch >= 0)
  {
  if(ch > 0)
  sw.write(ch);
  ch = errors.read();
  }
  }
  catch(IOException ioe)
  {
  System.out.println("IOException when collating error stream");
  }
*/

		// bring up an alert box with the error message in it...
		return("Problem invoking 'XCluster':\n\n" + errorGobbler.getResult() );
	    }
	}
	catch (java.io.IOException ioe)
	{
	    System.out.println("IOException when trying to run XCluster");
	    return("Couldn't run XCluster\n  " + ioe);
	}
	catch (Exception e)
	{
	    System.out.println("Exception when trying to run XCluster");
	    return("Couldn't run XCluster\n  " + e);
	}
	catch (Error e)
	{
	   System.out.println("Error when trying to run XCluster");
	   return("Couldn't run XCluster\n  " + e);
	}
    }
	    
    private class EisenClust
    {
	public String node_name;
	public String first_name;
	public String last_name;

	public EisenClust(String n, String f, String l)
	{
	    node_name = n;
	    first_name = f;
	    last_name = l;
	}

    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  Stanford .cdt/.gtr/.atr Format
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public ExprData.Cluster loadClusterDataFromStanfordFormatFile(String name_prefix, File cdt_file, File gtr_file, File atr_file)
    {
	// -----------------------------------------------------------------------
	//
	// the .cdt file specifies the order of the spots and gives the 
	// mapping from 'code' name to spot name
	// ( code names are of the form GENEnnnX )
	//
	// -----------------------------------------------------------------------
	
	if(cdt_file != null)
	{
	    //System.out.println("reading " + cdt_file.getPath());

	    
	    Vector eisen_clusts = new Vector();
	    
	    // Vector input_strs = new Vector();

	    // the firstlines is header to be skipped...
	    
     	    //int line = 0;

	    int found = 0;

	    Hashtable code_to_data_index_ht = new Hashtable();
	    // Hashtable code_name_to_index_ht = new Hashtable();
	    // Vector    code_name_sequence_v = new Vector();

	    //  Hashtable arry_name_to_meas_name_ht = new Hashtable();

	    try
	    {
		BufferedReader br = new BufferedReader(new FileReader(cdt_file));
		
		String str = br.readLine();

		// if clustering Measurements, the second line is special
		if(atr_file != null)
		{
		    Vector mnames = readNames(str,4);
		    
		    str = br.readLine();

		    Vector anames = readNames(str,4);

		    for(int m=0; m < anames.size(); m++)
		    {
			String mn = (String) mnames.elementAt(m);
			String an = (String) anames.elementAt(m);
			int m_id = edata.getMeasurementFromName(mn);
			if(m_id >= 0)
			{
			    code_to_data_index_ht.put( an , mn );
			    // System.out.println("'" + an + "' -> '" + m_id + "'");
			}
			else
			{
			    System.out.println("WARNING: " + mn + " not recognised");
			}
		    }
		    found = anames.size();
		    //System.out.println(anames.size() + " ARRY names found");
		    str = br.readLine();
		}

		if(gtr_file != null)
		{
		 
		    str = br.readLine();
		    
		    while(str != null)
		    {
			//String tstr = str.trim();
		    
			int tab_p = str.indexOf('\t');
			if(tab_p >= 0)
			{
			    String eisen_name = (str.substring(0, tab_p)).trim();
			    String remainder =  str.substring(tab_p+1);
			    int tab_p_2 = remainder.indexOf('\t');
			    if(tab_p_2 >= 0)
			    {
				String real_name = (remainder.substring(0, tab_p_2)).trim();
				
				int sid = edata.getIndexBySpotName( real_name );
				
				//if(eisen_name.equals("GENE374X"))
				//    System.out.println("GENE374X is spot " + sid);
				
				if(sid >= 0)
				{
				    Integer spot_id = new Integer( sid );
				    
				    code_to_data_index_ht.put(eisen_name, real_name );
				    
				    // code_name_to_index_ht.put(eisen_name, new Integer( line ));
				    
				    // code_name_sequence_v.addElement(eisen_name);
				    
				    found++;
				    
				    //line++;
				}
				else
				{
				    System.out.println("spot: '" + real_name + "' not recognised");
				}
			    }
			}
			
			str = br.readLine();
		    }
		    //System.out.println(found + " GENE names found");
		}
	    }
	    catch(java.io.IOException ioe)
	    {
		System.out.println("Unable to read name.\nError: " + ioe);
		return null;
	    }

	    // -----------------------------------------------------------------------
	    // report progress
	    // -----------------------------------------------------------------------

	    // -----------------------------------------------------------------------
	    // -----------------------------------------------------------------------
	    //    'gene' .gtr or 'arry' .atr file
	    // -----------------------------------------------------------------------
	    // -----------------------------------------------------------------------

	    File cluster_file =  (gtr_file == null) ? atr_file : gtr_file;

	    int data_type =      (gtr_file == null) ? ExprData.MeasurementName : ExprData.SpotName;

	    if((cluster_file != null) && (found > 0))
	    {
		System.out.println("reading " + cluster_file.getPath());

		// nothing in this cluster means there will be no .gtr file
		
		Hashtable eisen_clust_by_name_ht = new Hashtable();
		
		// now parse the .gtr file and build the clusters
		//
		
		Vector input_strs = new Vector();
		
		try
		{
		    BufferedReader br = new BufferedReader(new FileReader(cluster_file));
		    
		    String str = br.readLine();
		    while(str != null)
		    {
			//String tstr = str.trim();
			input_strs.addElement(str);
			str = br.readLine();
		    }
		}
		catch(java.io.IOException ioe)
		{
		    System.out.println("Unable to read name.\nError: " + ioe);
		    return null;
		}
		
		int dupls = 0;
		
		for(int en=0; en < input_strs.size(); en++)
		{
		    String str = (String) input_strs.elementAt(en);
		    
		    // extract the eisen-code as the chars up to the first tab
		    int tab_p = str.indexOf('\t');
		    if(tab_p >= 0)
		    {
			String eisen_name = (str.substring(0, tab_p)).trim();
			String part_2 =  str.substring(tab_p+1);
			int tab_p_2 = part_2.indexOf('\t');
			if(tab_p_2 >= 0)
			{
			    String clust_first = (part_2.substring(0, tab_p_2)).trim();
			    
			    String part_3 =  part_2.substring(tab_p_2+1);
			    
			    int tab_p_3 = part_3.indexOf('\t');
			    if(tab_p_3 >= 0)
			    {
				String clust_last = (part_3.substring(0, tab_p_3)).trim();
				
				// now we have the cluster details for this node
				EisenClust ec = new EisenClust(eisen_name, clust_first, clust_last);
				
				//if(eisen_name.equals("NODE185X"))
				//System.out.println("NODE185X is " + clust_first + "..." +  clust_last);
				
				if(eisen_clust_by_name_ht.get(eisen_name) != null)
				    dupls++;
				
				eisen_clust_by_name_ht.put(eisen_name, ec);
				eisen_clusts.addElement(ec);
			    }
			}
		    }
		}
		if(dupls > 0)
		{
		    mview.alertMessage("Warning: " + dupls + " node names were duplicated. Possibly bad?");
		}
		
		int spots_used = 0;
		
		//System.out.println(eisen_clusts.size() + " nodes found in the file.");
		
		// now build a cluster for each of the nodes
		
		//ExprData.Cluster new_root = edata.new Cluster("Stanford test");
		
		final int nn = eisen_clusts.size();
		
		Hashtable cluster_by_name_ht = new Hashtable();
		
		int prog_done = 0;
		int prog_update = (nn / 10);
		int prog_tick = 0;
		
		for(int n=0; n < nn; n++)
		{
		    if(++prog_tick == prog_update)
		    {
			prog_done += 10;
			prog_tick = 0;
			// System.out.println(prog_done + "%");
		    }
		    
		    EisenClust ec = (EisenClust) eisen_clusts.elementAt(n);
		    
		    boolean first_is_node = ec.first_name.startsWith("NODE");
		    boolean last_is_node  = ec.last_name.startsWith("NODE");
		    
		    //System.out.println(ec.node_name + " is " + ec.first_name + " to " + ec.last_name);
		    
		    if(first_is_node)
		    {
			if(last_is_node)
			{
			    // both are nodes...
			    
			    // get the cluster representing the left-hand side
			    ExprData.Cluster left_node = (ExprData.Cluster) cluster_by_name_ht.get(ec.first_name);
			    
			    if(left_node == null)
			    {
				System.out.println("WARNING: undefined reference to " + ec.first_name + " in " + ec.node_name);
			    }
			    else
			    {
				// get the cluster representing the right-hand side
				ExprData.Cluster right_node = (ExprData.Cluster) cluster_by_name_ht.get(ec.last_name);
				if(right_node == null)
				{
				    System.out.println("WARNING: undefined reference to " + ec.last_name + " in " + ec.node_name);
				}
				else
				{
				    // create the new node containing no probes,
				    ExprData.Cluster new_cl = edata.new Cluster(name_prefix + ec.node_name, data_type, null);
				    
				    // and add the other clusters as children
				    new_cl.addCluster(left_node);
				    new_cl.addCluster(right_node);
				    
				    // and put this cluster in the table
				    cluster_by_name_ht.put(ec.node_name, new_cl);
				}
			    }
			}
			else
			{
			    // node, !node
			    
			    // get the cluster representing the left-hand side
			    ExprData.Cluster left_node = (ExprData.Cluster) cluster_by_name_ht.get(ec.first_name);
			    
			    if(left_node == null)
			    {
				System.out.println("WARNING: undefined reference to " + ":" + ec.first_name + " in " + ec.node_name);
			    }
			    else
			    {
				// get the data for the right-hand side

				Vector maxd_data_ids = new Vector();
				
				String mn = (String) code_to_data_index_ht.get(ec.last_name);
				
				if(mn != null)
				{
				    maxd_data_ids.addElement(mn);
				    
				    spots_used++;
				    
				    // create the new node containing the single probe
				    ExprData.Cluster new_cl = edata.new Cluster(name_prefix + ec.node_name, data_type, maxd_data_ids);
				    // and add the other cluster as a child
				    new_cl.addCluster(left_node);
				    
				    // and put this cluster in the table
				    cluster_by_name_ht.put(ec.node_name, new_cl);
				}
			    }
			}
		    }
		    else
		    {
			if(last_is_node)
			{
			    // !node, node

			    // get the cluster representing the right-hand side
			    ExprData.Cluster right_node = (ExprData.Cluster) cluster_by_name_ht.get(ec.last_name);
			    
			    if(right_node == null)
			    {
				System.out.println("WARNING: undefined reference to " + ec.last_name + " in " + ec.node_name);
			    }
			    else
			    {
				// get the data for the left-hand side

				Vector maxd_data_ids = new Vector();
				
				String nm = (String) code_to_data_index_ht.get(ec.first_name);
				
				if(nm != null)
				{
				    maxd_data_ids.addElement(nm);
				    
				    spots_used++;
				    
				    // create the new node containing the single probe
				    ExprData.Cluster new_cl = edata.new Cluster(name_prefix + ec.node_name, data_type, maxd_data_ids);

				    // and add the other cluster as a child
				    new_cl.addCluster(right_node);
				    
				    // and put this cluster in the table
				    cluster_by_name_ht.put(ec.node_name, new_cl);
				}
			    }
			}
			else
			{
			    // neither are nodes, both are 'genes' or 'arrays'
			    
			    spots_used += 2;
			    
			    String n1  = (String) code_to_data_index_ht.get(ec.first_name);
			    String n2  = (String) code_to_data_index_ht.get(ec.last_name);
			    
			    //System.out.println(ec.node_name + " is " + ec.first_name + " + " + ec.last_name + 
			    //		   " s1=" + s1 + " s2=" + s2);
			    
			    if((n1 != null) && (n2 != null))
			    {
				Vector maxd_data_ids = new Vector();
				maxd_data_ids.addElement(n1);
				maxd_data_ids.addElement(n2);
				
				ExprData.Cluster new_cl = edata.new Cluster(name_prefix + ec.node_name, data_type, maxd_data_ids);
				
				// store a reference to this cluster for later usage
				cluster_by_name_ht.put(ec.node_name, new_cl);
				
				//new_root.addCluster(new_cl);
			    }
			}
		    }
		    
		}
		
		if(eisen_clusts.size() > 0)
		{
		    // assume that the last cluster defined is the root....
		    
		    // System.out.println(spots_used + " items placed in " + cluster_by_name_ht.size() + " clusters.\n");
		    
		    EisenClust ec = (EisenClust) eisen_clusts.elementAt(eisen_clusts.size() - 1);
		    
		    ExprData.Cluster root_node = (ExprData.Cluster) cluster_by_name_ht.get(ec.node_name);
		    
		    if(root_node == null)
		    {
			System.out.println("null node found in ht");
		    }
		    else
		    {
			//edata.addCluster(root_node);
		    }
		    return root_node;

		    //edata.addCluster(new_root);
		}
	    }
	}
	    
	return null;
    }

    private Vector readNames(String data, int skip)
    {
	Vector d = new Vector();

	for(int s=0; s < skip; s++)
	    data = removeToken(data,'\t');
	
	while(data != null)
	{
	    String n = getToken(data,'\t');
	    if(n != null)
		d.addElement(n);
	    data = removeToken(data,'\t');
	}

	return d;
    }
    
    private String getToken(String data, char delim)
    {
	int i = data.indexOf(delim);
	if(i >= 0)
	{
	    String result = data.substring(0, i);
	    return (result.length() == 0) ? null : result;
	}
	else
	{
	    return data;
	}
    }
    
    private String removeToken(String data, char delim)
    {
	if(data == null)
	    return null;
	int i = data.indexOf(delim);
	if(i >= 0)
	{
	    String result = data.substring(i+1);
	    return (result.length() == 0) ? null : result;
	}
	else
	{
	    return null;
	}
    }
    

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  stuff
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private JTabbedPane tabbed_pane;

    private JList meas_list;

    private JSlider cobweb_acuity;
    private JSlider cobweb_cutoff;
    private JSlider em_num_clusts;
    private JSlider em_max_iters;
    private JCheckBox apply_filter_jchkb;
    private JRadioButton cluster_spots_jrb, cluster_meas_jrb;
    private JTextField prog_loc_jtf;
    private JTextField tmp_dir_jtf;
    private JCheckBox delete_files_jchkb;
    private JRadioButton pears_dist_jrb, eucl_dist_jrb;
    private JCheckBox log_trans_jchkb, centered_met_jchkb;
    private JRadioButton  no_part_jrb, som_part_jrb, kmeans_part_jrb;

    private JTextField som_rows_jtf, som_cols_jtf, k_val_jtf;

    /*
    private void setupSlider(JSlider js, int major_tick_space, int minor_tick_space)
    {
	js.setMajorTickSpacing(major_tick_space);
	js.setMinorTickSpacing(minor_tick_space);
	js.setPaintTicks(true);
	js.setPaintLabels(true);
    }
    */

    private void addComponents()
    {
	GridBagLayout gridbag = new GridBagLayout();
	JPanel panel = new JPanel();
	GridBagConstraints c = null;

	panel.setLayout(gridbag);
	panel.setPreferredSize(new Dimension(500, 350));

	// measurement selection

	{
	    JPanel meas_sel_panel = new JPanel();
	    
	    GridBagLayout ms_gridbag = new GridBagLayout();
	    meas_sel_panel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
	    meas_sel_panel.setLayout(ms_gridbag);

	    meas_list = new JList();
	    JScrollPane jsp = new JScrollPane(meas_list);
	    meas_list.setModel(new MeasListModel());
	    meas_list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weighty = c.weightx = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    ms_gridbag.setConstraints(jsp, c);
	    meas_sel_panel.add(jsp);
	    
	    JPanel innerp = new JPanel();
	    {
		JButton jb = new JButton("All");
		jb.setFont(mview.getSmallFont());
		
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    meas_list.setSelectionInterval( 0,  meas_list.getModel().getSize() - 1 );
			}
		    });
		innerp.add(jb);
		
		jb = new JButton("None");
		jb.setFont(mview.getSmallFont());
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    meas_list.setSelectedIndices( new int[0] );
			}
		    });
		innerp.add(jb);
	    }

	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.gridx = 0;
	    c.gridy = 1;
	    ms_gridbag.setConstraints(innerp, c);
	    meas_sel_panel.add(innerp);	

	    
	    apply_filter_jchkb = new JCheckBox("Apply filter");
	    apply_filter_jchkb.setSelected(mview.getBooleanProperty("XCluster.apply_filter", true));
	    c = new GridBagConstraints();
		//c.anchor = GridBagConstraints.WEST;
	    c.gridx = 0;
	    c.gridy = 2;
	    ms_gridbag.setConstraints(apply_filter_jchkb, c);
	    meas_sel_panel.add(apply_filter_jchkb);
	    
	    c = new GridBagConstraints();
	    //c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.BOTH;
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    gridbag.setConstraints(meas_sel_panel, c);
	
	    panel.add(meas_sel_panel);
	}


	JPanel tab_wrapper = new JPanel();
	GridBagLayout tab_bag = new GridBagLayout();
	    
	{
	    tab_wrapper.setLayout(tab_bag);

	    tabbed_pane = new JTabbedPane();
	    tabbed_pane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	    
	    GridBagLayout tab_panel_bag = new GridBagLayout();
	    JPanel tab_panel = new JPanel();
	    tab_panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	    tab_panel.setLayout(tab_panel_bag);

	    {
		JLabel label = new JLabel("Location of Cluster program");
		tab_panel.add(label);
		 c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		c.anchor = GridBagConstraints.SOUTHWEST;
		tab_panel_bag.setConstraints(label, c);
	    }

	    {
		prog_loc_jtf = new JTextField(30);
		tab_panel.add(prog_loc_jtf);
		prog_loc_jtf.setText(mview.getProperty("XCluster.prog", "[XCluster-binary-location-here]"));

		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.weighty = c.weightx = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		tab_panel_bag.setConstraints(prog_loc_jtf, c);
	    }

	    {
		JButton browse = new JButton("Browse");
		browse.setFont(mview.getSmallFont());
		browse.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    
			    JFileChooser jfc = new JFileChooser();
			    jfc.setCurrentDirectory(new File(prog_loc_jtf.getText()));
			    int returnVal =  jfc.showOpenDialog(mview.getDataPlot()); 
			    if(returnVal == JFileChooser.APPROVE_OPTION) 
			    {
				prog_loc_jtf.setText( jfc.getSelectedFile().getPath() );
			    }
			}
		    });
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 1;
		c.anchor = GridBagConstraints.NORTHWEST;
		tab_panel_bag.setConstraints(browse, c);
		tab_panel.add(browse);
	    }

	    {
		JLabel label = new JLabel("Directory for temporary files");
		tab_panel.add(label);
		 c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 2;
		c.weighty = c.weightx = 1.0;
		c.anchor = GridBagConstraints.SOUTHWEST;
		tab_panel_bag.setConstraints(label, c);
	    }
	    {
		tmp_dir_jtf = new JTextField(30);
		tmp_dir_jtf.setText(mview.getProperty("XCluster.tmp_dir", ""));
		tab_panel.add(tmp_dir_jtf);

		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 3;
		c.weighty = 0.5;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		tab_panel_bag.setConstraints(tmp_dir_jtf, c);
	    }
	    {
		JButton browse = new JButton("Browse");
		browse.setFont(mview.getSmallFont());
		browse.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    
			    JFileChooser jfc = new JFileChooser();
			    jfc.setApproveButtonText( "Select" );
			    jfc.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
			    jfc.setCurrentDirectory(new File(tmp_dir_jtf.getText()));
			    int returnVal =  jfc.showOpenDialog(mview.getDataPlot()); 
			    if(returnVal == JFileChooser.APPROVE_OPTION) 
			    {
				tmp_dir_jtf.setText( jfc.getSelectedFile().getPath() );
			    }
			}
		    });
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 3;
		c.anchor = GridBagConstraints.NORTHWEST;
		tab_panel_bag.setConstraints(browse, c);
		tab_panel.add(browse);
	    }
	    {
		delete_files_jchkb = new JCheckBox("Delete after use");
		delete_files_jchkb.setSelected(mview.getBooleanProperty("XCluster.del_files", true));
		tab_panel.add(delete_files_jchkb);

		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 4;
		c.weighty = 0.5;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		tab_panel_bag.setConstraints(delete_files_jchkb, c);
	    }
	    /*
	    {
		JLabel label = new JLabel("Program args ");
		tab_panel.add(label);
		 c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.anchor = GridBagConstraints.EAST;
		tab_panel_bag.setConstraints(label, c);
	    }
	    
	    {
		prog_args_jtf = new JTextField(20);
		prog_args_jtf.setText(mview.getProperty("XCluster.args", "-f input.txt -g 1 -e 0 -p 0"));
		tab_panel.add(prog_args_jtf);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 1;
		c.weighty = c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		tab_panel_bag.setConstraints(prog_args_jtf, c);
	    }
	    */

	    tabbed_pane.addTab(" Options ", tab_panel);
	}

	// =======================================================

	{
	    GridBagLayout tab_panel_bag = new GridBagLayout();
	    JPanel tab_panel = new JPanel();
	    
	    tab_panel.setLayout(tab_panel_bag);
	    //panel.setPreferredSize(new Dimension(350, 200));
	    tab_panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	    int line = 0;

	    ButtonGroup bg = new ButtonGroup();

	    {
		no_part_jrb = new JRadioButton();
		no_part_jrb.setSelected(mview.getBooleanProperty("XCluster.none",true));
		bg.add(no_part_jrb);
		tab_panel.add(no_part_jrb);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.weighty = 1.0;
		//c.weightx = 1.0;
		c.anchor = GridBagConstraints.EAST;
		//c.fill = GridBagConstraints.HORIZONTAL;
		tab_panel_bag.setConstraints(no_part_jrb, c);
	    }
	    {
		JLabel label = new JLabel("No partition");
		tab_panel.add(label);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line++;
		c.anchor = GridBagConstraints.WEST;
		tab_panel_bag.setConstraints(label, c);
	    }

	    {
		som_part_jrb = new JRadioButton();
		som_part_jrb.setSelected(mview.getBooleanProperty("XCluster.som", false));
		bg.add(som_part_jrb);
		tab_panel.add(som_part_jrb);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.weighty = 1.0; 
		//c.weightx = 1.0;
		c.anchor = GridBagConstraints.EAST;
		//c.fill = GridBagConstraints.HORIZONTAL;
		tab_panel_bag.setConstraints(som_part_jrb, c);
	    }
	    {
		JLabel label = new JLabel("Self-Organising-Map");
		tab_panel.add(label);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line++;
		c.anchor = GridBagConstraints.WEST;
		tab_panel_bag.setConstraints(label, c);
	    }
	    
	    {
		JLabel label = new JLabel("rows ");
		label.setFont(mview.getSmallFont());
		tab_panel.add(label);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.anchor = GridBagConstraints.EAST;
		tab_panel_bag.setConstraints(label, c);
	    }
	    {
		som_rows_jtf = new JTextField(4);
		som_rows_jtf.setText(mview.getProperty("XCluster.som_rows", ""));
		//som_rows_jtf.setEnabled(false);
		tab_panel.add(som_rows_jtf);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line++;
		c.weighty = 0.5;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		//c.fill = GridBagConstraints.HORIZONTAL;
		tab_panel_bag.setConstraints(som_rows_jtf, c);
	    }

	    {
		JLabel label = new JLabel("cols ");
		label.setFont(mview.getSmallFont());
		tab_panel.add(label);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.anchor = GridBagConstraints.EAST;
		tab_panel_bag.setConstraints(label, c);
	    }
	    {
		som_cols_jtf = new JTextField(4);
		som_cols_jtf.setText(mview.getProperty("XCluster.som_cols", ""));
		//som_cols_jtf.setEnabled(false);
		tab_panel.add(som_cols_jtf);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line++;
		c.weighty = 0.5;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		//c.fill = GridBagConstraints.HORIZONTAL;
		tab_panel_bag.setConstraints(som_cols_jtf, c);
	    }

	    // =======================================================

	    {
		kmeans_part_jrb = new JRadioButton();
		kmeans_part_jrb.setSelected(mview.getBooleanProperty("XCluster.k_means", false));
		bg.add(kmeans_part_jrb);
		tab_panel.add(kmeans_part_jrb);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.weighty = 1.0;
		//c.weightx = 1.0;
		c.anchor = GridBagConstraints.EAST;
		//c.fill = GridBagConstraints.HORIZONTAL;
		tab_panel_bag.setConstraints(kmeans_part_jrb, c);
	    }
	    {
		JLabel label = new JLabel("k-means");
		tab_panel.add(label);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line++;
		c.anchor = GridBagConstraints.WEST;
		tab_panel_bag.setConstraints(label, c);
	    }
	
	    {
		JLabel label = new JLabel("k= ");
		label.setFont(mview.getSmallFont());
		tab_panel.add(label);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line;
		c.anchor = GridBagConstraints.EAST;
		tab_panel_bag.setConstraints(label, c);
	    }
	    {
		k_val_jtf = new JTextField(4);
		k_val_jtf.setText(mview.getProperty("XCluster.k_val", ""));
		tab_panel.add(k_val_jtf);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line++;
		c.weighty = 0.5;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		//c.fill = GridBagConstraints.HORIZONTAL;
		tab_panel_bag.setConstraints(k_val_jtf, c);
	    }

	    tabbed_pane.addTab(" Partition ", tab_panel);
	}

	
	{
	    GridBagLayout tab_panel_bag = new GridBagLayout();
	    JPanel tab_panel = new JPanel();
	    tab_panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	    tab_panel.setLayout(tab_panel_bag);

	    ButtonGroup bg = new ButtonGroup();

	    {
		pears_dist_jrb = new JRadioButton("Pearson corelation");
		pears_dist_jrb.setSelected(mview.getBooleanProperty("XCluster.pearson", true));
		bg.add(pears_dist_jrb );
		tab_panel.add(pears_dist_jrb);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		tab_panel_bag.setConstraints(pears_dist_jrb, c);
	    }

	    {
		eucl_dist_jrb = new JRadioButton("Euclidian distance");
		eucl_dist_jrb.setSelected(mview.getBooleanProperty("XCluster.euclidian", false));
		bg.add(eucl_dist_jrb );
		tab_panel.add(eucl_dist_jrb);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 1;
		c.weighty = c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		tab_panel_bag.setConstraints(eucl_dist_jrb, c);
	    }

	    {
		centered_met_jchkb = new JCheckBox("Centered distance");
		centered_met_jchkb.setSelected(mview.getBooleanProperty("XCluster.centered", false));
		tab_panel.add(centered_met_jchkb);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 2;
		c.weighty = c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		tab_panel_bag.setConstraints(centered_met_jchkb, c);
	    }

	    {
		log_trans_jchkb = new JCheckBox("Log transform data");
		log_trans_jchkb.setSelected(mview.getBooleanProperty("XCluster.log_trans", false));
		tab_panel.add(log_trans_jchkb);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 3;
		c.weighty = c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		tab_panel_bag.setConstraints(log_trans_jchkb, c);
	    }

	    tabbed_pane.addTab(" Cluster ", tab_panel);

	    // =================================

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weighty = 1.0;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    tab_bag.setConstraints(tabbed_pane, c);
	    tab_wrapper.add(tabbed_pane);

	    bg = new ButtonGroup();
	    
	    JPanel wrapper = new JPanel();
	    wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.X_AXIS));
	    cluster_spots_jrb = new JRadioButton("Cluster by Spots");
	    cluster_spots_jrb.setSelected(mview.getBooleanProperty("XCluster.by_spots", true));
	    wrapper.add(cluster_spots_jrb);
	    bg.add(cluster_spots_jrb);
	    cluster_meas_jrb  = new JRadioButton("Cluster by Measurements");
	    cluster_meas_jrb.setSelected(mview.getBooleanProperty("XCluster.by_meas", false));
	    wrapper.add(cluster_meas_jrb);
	    bg.add(cluster_meas_jrb);

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.weightx = 1.0;
	    //c.anchor = GridBagConstraints.NORTH;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    tab_bag.setConstraints(wrapper, c);
	    tab_wrapper.add(wrapper);
	    
	    // =================

	    c = new GridBagConstraints();
	    c.fill = GridBagConstraints.BOTH;
	    c.gridx = 1;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    gridbag.setConstraints(tab_wrapper, c);
	    panel.add(tab_wrapper);
	}

	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrapper.setLayout(w_gridbag);
	    wrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

	    {
		JButton button = new JButton("Close");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    cancel();
			}
		    });
		
		 c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		//c.weighty = c.weightx = 1.0;
		//c.anchor = GridBagConstraints.EAST;
		w_gridbag.setConstraints(button, c);
	    }
	    {
		JButton button = new JButton("Cluster");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    cluster();
			}
		    });
		
		 c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		//c.weighty = c.weightx = 1.0;
		w_gridbag.setConstraints(button, c);
	    }
	    {
		JButton button = new JButton("Help");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    mview.getPluginHelpTopic("XCluster", "XCluster");
			}
		    });
		
		 c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 0;
		//c.weighty = c.weightx = 1.0;
		w_gridbag.setConstraints(button, c);
	    }

	    c = new GridBagConstraints();
	    c.gridwidth = 2;
	    c.gridx = 0;
	    c.gridy = 1;
	    //c.weighty = c.weightx = 1.0;
	    gridbag.setConstraints(wrapper, c);
	    panel.add(wrapper);	    
	}

	getContentPane().add(panel);
    }

    public class MeasListModel extends DefaultListModel
    {
	public Object getElementAt(int index) 
	{
	    return edata.getMeasurementName( edata.getMeasurementAtIndex(index) );
	}
	public int getSize() 
	{
	    return edata.getNumMeasurements();
	}
    }
    

    class StreamGobbler extends Thread
    {
	InputStream is;
	String type;
	StringBuffer result;

	StreamGobbler(InputStream is, String type)
	{
	    this.is = is;
	    this.type = type;
	    result = new StringBuffer();
	}
	
	public String getResult() { return result.toString(); }

	public void run()
	{
	    try
	    {
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line=null;
		while ( (line = br.readLine()) != null)
		{
		    result.append( line + "\n" );
		}
	    } catch (IOException ioe)
	    {
		ioe.printStackTrace();
	    }
	}
    }


    private maxdView mview;
    private DataPlot dplot;
    private ExprData edata;


}
