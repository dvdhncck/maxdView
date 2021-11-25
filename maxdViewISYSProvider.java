import org.ncgr.isys.system.IsysObject;
import org.ncgr.isys.system.IsysObjectCollection;
import org.ncgr.isys.system.IsysAttribute;
import java.util.Vector;
import java.util.Random;
import java.net.URL;
import java.io.File;
import java.io.IOException;
import org.ncgr.isys.system.*;


public class maxdViewISYSProvider implements ServiceProvider
{
    public maxdViewISYSProvider()
    {
    }
    
    public Class[] getServicesProvided()
    {
	Class[] cls = new Class[1];
	cls[0] = maxdViewISYSService.class;
	return cls;
    }
    public Class[] getProvidedStaticServices()
    {
	Class[] cls = new Class[1];
	cls[0] = maxdViewISYSService.class;
	return cls;
    }
    
    public Class[] getOptionalStaticServices()
    {
	return null;
    }

    public String getCopyright()
    {
	return("(c)2006 Manchester Bioinformatics & (c)2006 NCGR");	
    }	
    public String getVersion()
    {
	return("1.0.6");
    }
    public String getDeveloper()
    {
	return("developed by David Hancock, wrapped for ISYS by Andrew Farmer, Kevin Garwood & David Hancock");
    }
    public String getDescription()
    {
	return("Analysis and visualisation of expression data");	
    }
    public String getDisplayName()
    {
	return "maxdView";
    }
    public Class[] getServicesRequired()
    {
	return null;
    }
    public Class[] getRequiredStaticServices()
    {
	return null;
    }

    public Service getService(java.lang.Class serviceClass)
    {
	return null;
	
    }	
    public EntryPointService[] getEntryPointServices()
    {
	EntryPointService cls[] = new EntryPointService[1];
	cls[0] = new maxdViewISYSService(this);
	return cls;
    }

    public DynamicViewerService[] discoverEligibleViewerServices(IsysObjectCollection obj)
    {
	return null;
    }
    
    public DynamicDataService[] discoverEligibleDataServices(IsysObjectCollection o)
    {
	return null;
    }
    
    public URL getURL()
    {
	URL link;
	
	try
	{
	    link = new URL("http://www.bioinf.man.ac.uk/microarray/resources.html");
	}
	catch(java.net.MalformedURLException ex)
	{
	    link = null;
	}
			
	return link;
	
    }
}




