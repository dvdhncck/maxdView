//
// writes a text file suitable for loading
// as an ArrayType in maxdLoad
//
public void doIt()
{
   File file = null;
   
   try
   {
       javax.swing.JFileChooser jfc = new javax.swing.JFileChooser();
       int rv = jfc.showSaveDialog(null);

       if(rv == javax.swing.JFileChooser.APPROVE_OPTION)
       {
	   file = jfc.getSelectedFile();
	   
	   if(file.exists())
	   {
	       if(mview.infoQuestion("File exists, overwrite?", "No", "Yes") == 0)
		   return;
	   } 
	   
	   PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
	   
	   int row = 0;
	   int col = 0;

	   for(int s=0; s < edata.getNumSpots(); s++)
	   {
	       writer.write(edata.getSpotNameAtIndex(s) + "\t" +
                            edata.getProbeNameAtIndex(s) + "\t" +
			    String.valueOf(col) + "\t" + 
                            String.valueOf(row) + "\n");
	   }

	   writer.close();
       }
   }
   catch(java.io.IOException e)
   {
       mview.errorMessage("Unable to write to " + file.getName() + "\nerror: " + e);
   }
}
