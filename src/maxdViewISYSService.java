import javax.swing.Icon;
import java.lang.String;
import java.io.*;
import javax.swing.ImageIcon;

import org.ncgr.isys.system.Service;
import org.ncgr.isys.system.ServiceProvider;
import org.ncgr.isys.system.DynamicViewerService;

import org.ncgr.isys.system.IsysObject;
import org.ncgr.isys.system.Client;
import org.ncgr.isys.system.EntryPointService;

// import org.ncgr.util.TextManipulation;


class maxdViewISYSService implements EntryPointService
{
    public maxdViewISYSService(ServiceProvider sp)
    {
	this.sp = sp;
    }	
    
    public void launch()
    {
	mview_client = new maxdViewISYSClient();
	mview_client.show();		
    }
    
    public Client execute() throws java.lang.reflect.InvocationTargetException
    {	
	mview_client = new maxdViewISYSClient();
	mview_client.show();				
	
	return (Client) mview_client;		
    }

    public String getDisplayName()
    {
	return sp.getDisplayName(); // "maxdView";
    }
    
    public String getDescription()
    {	
	return sp.getDescription(); // "Analysis and visualisation of gene expression data";
    }
    
    public Icon getSmallIcon()
    {
	String root = System.getProperty("maxdView.location");

	String imageFileName = root + File.separator + "images" + File.separator + "maxdView_w64.jpg";
	
	//String imageFileName = "images" + File.separator + "maxdView_w64.jpg";

	ImageIcon ii = new ImageIcon(imageFileName);	
	return ii;
    }	
    
    public Icon getLargeIcon()	
    {
	String root = System.getProperty("maxdView.location");

	String imageFileName = root + File.separator + "images" + File.separator + "maxdView_w200.jpg";

	/*
	String imageFileName = "Components" + 
	File.separator +
	"maxdView" +  
	File.separator +					
	"lib" + 
	File.separator +
	"images" + 
	File.separator +
	"maxdView_w200.jpg";
	*/

	//String imageFileName = "images" + File.separator + "maxdView_w200.jpg";
	
	ImageIcon ii = new ImageIcon(imageFileName);	
	
	return ii;
    }
    
    public ServiceProvider getServiceProvider()
    {
	return sp;
    }	
    
    private maxdViewISYSClient mview_client;
    private ServiceProvider sp;
}



