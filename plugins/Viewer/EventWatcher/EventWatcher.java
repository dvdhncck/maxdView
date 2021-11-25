import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.sql.*;
import java.util.Date;

//
// writes some or all of the expression data in some unspecified XML format
//

public class EventWatcher extends JFrame implements Plugin, ExprData.ExprDataObserver
{
    
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  plugin implementation
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void startPlugin()
    {
	mview.decorateFrame(this);
	pack();
	setVisible(true);
	mview.getExprData().addObserver(this);
    }

    public void stopPlugin()
    {
	mview.getExprData().removeObserver(this);
	setVisible(false);
    }

    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = 
	   new PluginInfo("Event Watcher", "viewer", "Shows internal update events as they happen", "", 1, 0, 0);
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

    public String pluginType() { return "viewer"; }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  observer 
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    //
    public void dataUpdate(ExprData.DataUpdateEvent due)
    {
	String ev_data = (due.spot >= 0) ? ("Spot " + due.spot + " in Measurement " + due.measurement) : null;
	logEvent("DataUpdateEvent", due.event, ev_data);
    }

    public void clusterUpdate(ExprData.ClusterUpdateEvent cue)
    {
	String ev_data = (cue.cluster != null) ? cue.cluster.getName() : null;
	logEvent("ClusterUpdateEvent", cue.event, ev_data);
    }

    public void measurementUpdate(ExprData.MeasurementUpdateEvent mue)
    {
	String ev_data = (mue.measurement >=0) ? ("Measurement " + mue.measurement) : null;
	logEvent("MeasurementUpdateEvent", mue.event, ev_data);
    }

    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
	logEvent("EnvironmentUpdateEvent", eue.event, eue.data);
    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   EventWatcher
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private void logEvent(String type, int ev_code, String ev_data)
    {
	String ev_name = null;

	switch(ev_code)
	{
	case ExprData.ColourChanged:
	    ev_name = "ColourChanged";
	    break;
	case ExprData.OrderChanged:
	    ev_name = "OrderChanged";
	    break;
	case ExprData.VisibilityChanged:
	    ev_name = "VisibilityChanged";
	    break;
	case ExprData.SizeChanged:
	    ev_name = "SizeChanged";
	    break;
	case ExprData.ElementsAdded:
	    ev_name = "ElementsAdded";
	    break;
	case ExprData.ElementsRemoved:
	    ev_name = "ElementsRemoved";
	    break;
	case ExprData.NameChanged:
	    ev_name = "NameChanged";
	    break;
	case ExprData.ValuesChanged:
	    ev_name = "ValuesChanged";
	    break;
	case ExprData.RangeChanged:
	    ev_name = "RangeChanged";
	    break;
	case ExprData.ObserversChanged:
	    ev_name = "ObserversChanged";
	    break;

	case ExprData.NameAttrsChanged:
	    ev_name = "NameAttrsChanged";
	    break;

	case ExprData.ColouriserAdded:
	    ev_name = "ColouriserAdded";
	    break;
	case ExprData.ColouriserRemoved:
	    ev_name = "ColouriserRemoved";
	    break;
	case ExprData.ColouriserChanged:
	    ev_name = "ColouriserChanged";
	    break;
	case ExprData.VisibleNameAttrsChanged:
	    ev_name = "VisibleNameAttrsChanged";
	    break;

	}

	Date this_event_time = new Date();
	if(last_event_time != null)
	{
	    long elapsed = this_event_time.getTime() - last_event_time.getTime();
	    elapsed /= 1000;
	    if(elapsed > 1)
	    {
		text_area.append("... +" + elapsed + "s ...\n");
	    }
	}
	last_event_time = this_event_time;

	text_area.append(type + "." + ev_name + "\n");
	if(ev_data != null)
	    text_area.append("  data < " + ev_data + " >\n");
    }
    
    public EventWatcher(maxdView mview_)
    {
	super("Event Watcher");

	mview = mview_;

	panel = new JPanel();
	panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	getContentPane().add(panel, BorderLayout.CENTER);
	GridBagLayout gridbag = new GridBagLayout();
	//panel.setPreferredSize(new Dimension(400, 300));
	panel.setLayout(gridbag);

	{
	    text_area = new JTextArea();
	    JScrollPane jsp = new JScrollPane(text_area);
	    jsp.setPreferredSize(new Dimension(300, 250));
	    panel.add(jsp);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = 10.0;
	    c.weighty = 9.0;
	    c.fill = GridBagConstraints.BOTH;
	    gridbag.setConstraints(jsp, c);
	}

	{   
	    JPanel wrapper = new JPanel();
	    wrapper.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrapper.setLayout(w_gridbag);

	    JButton jb = new JButton("Close");
	    wrapper.add(jb);
	    jb.addActionListener(new ActionListener() 
				 {
				     public void actionPerformed(ActionEvent e) 
				     {
					 stopPlugin();
				     }
		});
	    
	   
	    jb = new JButton("Help");
	    wrapper.add(jb);
	    jb.addActionListener(new ActionListener() 
				 {
				     public void actionPerformed(ActionEvent e) 
				     {
					 mview.getPluginHelpTopic("EventWatcher", "EventWatcher");
				     }
		});
	    
	    
	    jb = new JButton("Clear");
	    wrapper.add(jb);
	    jb.addActionListener(new ActionListener() 
				 {
				     public void actionPerformed(ActionEvent e) 
				     {
					 text_area.setText("");
				     }
		});
	    

	    GridBagConstraints c = new GridBagConstraints();
	    c.weightx = 10.0;
	    c.gridx = 0;
	    c.gridy = 1;
	    c.weightx = 10.0;
	    gridbag.setConstraints(wrapper, c);
	    panel.add(wrapper);
	}
	
	
    }

    private Date last_event_time = null;

    private maxdView mview;
    private JPanel panel;
    private JTextArea text_area;
    
}
