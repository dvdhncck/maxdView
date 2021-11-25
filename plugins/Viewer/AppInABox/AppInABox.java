import javax.swing.*;
import javax.swing.text.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.awt.event.KeyEvent;
import java.util.Enumeration;
import java.net.*;

//
// writes some or all of the expression data in some unspecified XML format
//

public class AppInABox implements Plugin, ExprData.ExprDataObserver
{
    public final boolean debug_start_stop = true;
    public final boolean debug_comms      = true;

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  plugin implementation
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public AppInABox(maxdView mview_)
    {
	mview = mview_;
	edata = mview.getExprData();

	app_path    = mview.getProperty("AppInABox.app_path", "/bin/sh");
	remote      = mview.getBooleanProperty("AppInABox.remote", false);
	remote_host = mview.getProperty("AppInABox.remote_host", "127.0.0.1");
	remote_port = mview.getIntProperty("AppInABox.remote_port", 28282);
    }


    public void startPlugin()
    {
	frame = createFrame();
	
	frame.setVisible(true);
	//mview.getExprData().addObserver(this);

	startApplication();

	lsd.insertString(0, readFromApp(), app_text_style);
	
	last_text_end =  lsd.getLength(); // app_jtp.getCaretPosition();
	
	app_jtp.setCaretPosition(lsd.getLength());
    }

    public void stopPlugin()
    {
	cleanUp();
    }

    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = 
	   new PluginInfo("App In A Box", "viewer", "Runs a text-mode application in a window", "", 1, 0, 0);
	return pinf;
    }

    public PluginCommand[] getPluginCommands()
    {
	PluginCommand[] com = new PluginCommand[2];

	String[] args = new String[] 
	{
	    // name      // type      //default   // flag   // comment
	    "app",      "file",       "",         "m",      "name of application to start", 
	    "remote",   "boolean",    "false",    "",       "",
	    "host",     "string",     "",         "",       "remote host name", 
	    "port",     "integer",    "",         "",       "remote host port number"
	};

	com[0] = new PluginCommand("start", args);
	com[1] = new PluginCommand("stop",  null);

	return com;
    }
 
    public void   runCommand(String name, String[] args, CommandSignal done) 
    { 
	if(name.equals("start"))
	{
	    
	    String val  =  mview.getPluginArg("app", args);
	    if(val != null)
		app_path = val;

	    val  =  mview.getPluginArg("remote", args);
	    if(val != null)
		remote = mview.parseBooleanArg(val);

	    val  =  mview.getPluginArg("host", args);
	    if(val != null)
		remote_host = val;

	    val = mview.getPluginArg("port", args);
	    if(val != null)
	    {
		remote_port = (new Integer(val)).intValue();
	    }
	    
	    startPlugin();
	}
	if(name.equals("stop"))
	{
	    cleanUp();
	}

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
	//	String ev_data = (due.spot >= 0) ? ("Spot " + due.spot + " in Measurement " + due.measurement) : null;
	//logEvent("DataUpdateEvent", due.event, ev_data);
    }

    public void clusterUpdate(ExprData.ClusterUpdateEvent cue)
    {
	//String ev_data = (cue.cluster != null) ? cue.cluster.getName() : null;
	//logEvent("ClusterUpdateEvent", cue.event, ev_data);
    }

    public void measurementUpdate(ExprData.MeasurementUpdateEvent mue)
    {
	//String ev_data = (mue.measurement >=0) ? ("Measurement " + mue.measurement) : null;
	//logEvent("MeasurementUpdateEvent", mue.event, ev_data);
    }

    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
	//logEvent("EnvironmentUpdateEvent", eue.event, eue.data);
    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   AppInABox
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    // handles any changes in text fields
    //
    /*
      class CustomChangeListener implements DocumentListener 
    {
	public void insertUpdate(DocumentEvent e)  
	{ 
	    propagate(e); 
	}

	public void removeUpdate(DocumentEvent e)  { propagate(e); }
	public void changedUpdate(DocumentEvent e) { propagate(e); }

	private void propagate(DocumentEvent e)
	{
	    ruleHasChanged();
	}
    }
    */
    private Vector last_command_v = new Vector();
    private int current_last_command = 0;

    private void insertOldCommand(int index)
    {
	int del_len = lsd.getLength() - last_text_end;

	//System.out.println("inserting index " + index);
	//System.out.println("  deleting from  " + last_text_end + " for " + del_len + " chars");
	//System.out.println("  inserting string: " + (String)last_command_v.elementAt(index));

	try
	{
	    if(del_len > 0)
		lsd.remove(last_text_end, del_len);
	}
	catch(javax.swing.text.BadLocationException ble)
	{
	}
	
	// allow -1 to mean 'no command'
	if(index >= 0)
	{
	    lsd.insertString(last_text_end, 
			     (String)last_command_v.elementAt(index), 
			     user_text_style);
	    app_jtp.setCaretPosition(lsd.getLength());
	}
    }

    public class PreviousAction extends AbstractAction
    {
	public void actionPerformed(ActionEvent ae) 
	{
	    if((current_last_command) < last_command_v.size())
	    {
		insertOldCommand(current_last_command);
		current_last_command++;
	    }
	}
    }
    public class NextAction extends AbstractAction
    {
	public void actionPerformed(ActionEvent ae) 
	{
	    // allow -1 to mean 'no command'
	    if(current_last_command >= 0)
	    {
		current_last_command--;
		insertOldCommand(current_last_command);
	    }
	}
    }

    public class ReturnAction extends AbstractAction
    {
	public void actionPerformed(ActionEvent ae) 
	{
	    //System.out.println("RETURN");

	    // extract the text entered since the last return...

	    String new_text = app_jtp.getText().substring(last_text_end);

	    last_command_v.insertElementAt(new_text,0);
	    current_last_command = 0;

	    //System.out.println("new text is '" + new_text + "'");

	    last_text_end =  lsd.getLength(); // app_jtp.getCaretPosition();

	    //app_jtp.insert("\n", last_text_end);
	    lsd.insertString(lsd.getLength(), "\n", app_text_style);
	    
	    if(new_text.length() > 0)
	    {
		writeToApp(new_text);
		
		snooze();
		
		//app_jtp.insert(readFromApp(), app_jtp.getCaretPosition());
		lsd.insertString(lsd.getLength(), readFromApp(), app_text_style);
		
		last_text_end =  lsd.getLength(); // app_jtp.getCaretPosition();
	    }

	    app_jtp.setCaretPosition(lsd.getLength());
	}
    }

    public void cleanUp()
    {
	//mview.getExprData().removeObserver(this);
	mview.putProperty("AppInABox.app_path", app_path);

	mview.putBooleanProperty("AppInABox.remote", remote);
	mview.putProperty("AppInABox.remote_host", remote_host);
	mview.putIntProperty("AppInABox.remote_port", remote_port);
	
	stopApplication();

	frame.setVisible(false);
    }

    private JFrame createFrame()
    {
	frame = new JFrame("Application In A Box");

	frame.addWindowListener(new WindowAdapter() 
	    {
		public void windowClosing(WindowEvent e)
		{
		    cleanUp();
		}
	    });

	int line = 0;

	text_panel = new JPanel();
	//text_panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
	GridBagLayout gridbag = new GridBagLayout();

	text_panel.setPreferredSize(new Dimension(400, 300));

	text_panel.setLayout(gridbag);

	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout wrapbag = new GridBagLayout();
	    wrapper.setLayout(wrapbag);
	    

	    JButton button = new JButton("App");
	    button.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			JFileChooser fc = mview.getFileChooser(app_path);
			if(fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
			{
			    File file = fc.getSelectedFile();
			    if(file != null)
			    {
				if(!(file.getPath().equals(app_path)))
				{
				    stopApplication();
				    
				    app_path = file.getPath();
				    app_label.setText(app_path);
				    
				    app_jtp.setText("");
				    last_text_end = 0;
				    app_jtp.requestFocus();

				    startApplication();
				}
			    }
			}
		    }
		});
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    //c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    wrapbag.setConstraints(button, c);
	    wrapper.add(button);

	    app_label = new JTextField(20);
	    app_label.setText(app_path);
	    app_label.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			stopApplication();

			app_path = app_label.getText();
			app_jtp.requestFocus();
			startApplication();
		    }
		});
	    //app_label.setEditable(false);
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    c.gridwidth = 3;
	    c.weightx = 3.0;
	    //c.weightx = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    //c.anchor = GridBagConstraints.WEST;
	    wrapbag.setConstraints(app_label, c);
	    wrapper.add(app_label);

	
	    JCheckBox jchkb = new JCheckBox("Remote");
	    jchkb.setSelected(remote);
	    jchkb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			stopApplication();
			remote = ((JCheckBox)e.getSource()).isSelected();
			app_jtp.requestFocus();
			startApplication();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    //c.weightx = 1.0;
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    wrapbag.setConstraints(jchkb, c);
	    wrapper.add(jchkb);


	    JTextField jtf = new JTextField(20);
	    jtf.setText(remote_host);
	    jtf.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			stopApplication();
			remote_host = ((JTextField)e.getSource()).getText();
			app_jtp.requestFocus();
			startApplication();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 1;
	    c.gridwidth = 2;
	    //c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    wrapbag.setConstraints(jtf, c);
	    wrapper.add(jtf);

	    jtf = new JTextField(5);
	    jtf.setText(String.valueOf(remote_port));
	    jtf.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			stopApplication();
			String rp = ((JTextField)e.getSource()).getText();
			try
			{
			    remote_port = new Integer(rp).intValue();
			    if((remote_port < 1024) || (remote_port >  65535))
			    {
				mview.alertMessage("Port number must be in the range 1024...65535");
			    }
			    else
			    {
				app_jtp.requestFocus();
				startApplication();
			    }
			}
			catch(NumberFormatException nfe)
			{
			    mview.alertMessage("'" + rp + "' not recognised as a port number");
			}
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = 3;
	    c.gridy = 1;
	    //c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    wrapbag.setConstraints(jtf, c);
	    wrapper.add(jtf);

	    // ---------------------

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    //c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    //c.anchor = GridBagConstraints.WEST;

	    gridbag.setConstraints(wrapper, c);
	    text_panel.add(wrapper);
	}
	/*

	{
	    JPanel wrapper = new JPanel();

	    
	    // ---------------------

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;

	    gridbag.setConstraints(wrapper, c);
	    text_panel.add(wrapper);
	}
	*/

	{
	    //Create the document for the text area.
	    lsd = new LimitedStyledDocument();

	    app_jtp = new JTextPane(lsd);

	    //app_jta.getDocument().addDocumentListener(new CustomChangeListener());

	    Keymap km = app_jtp.getKeymap();
	    km.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0), new ReturnAction());
	    km.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_UP,0),    new PreviousAction());
	    km.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,0),  new NextAction());

	    app_jtp.setKeymap(km);

	    //app_jtp.setTabSize(8);
	    
	    //app_text_style  = new StyledEditorKit.ForegroundAction("Blue", Color.blue);
	    //user_text_style = new StyledEditorKit.ForegroundAction("Red", Color.red);

	    app_text_style  = new SimpleAttributeSet();
	    StyleConstants.setFontFamily(app_text_style, "Courier");
	    StyleConstants.setFontSize(app_text_style, 12);
	    StyleConstants.setForeground(app_text_style, new Color(0,0,255-160));

	    user_text_style  = new SimpleAttributeSet();
	    StyleConstants.setFontFamily(user_text_style, "Courier");
	    StyleConstants.setFontSize(user_text_style, 12);
	    StyleConstants.setForeground(user_text_style, new Color(96,96,255));
	    
	    error_text_style  = new SimpleAttributeSet();
	    StyleConstants.setFontFamily(error_text_style, "Courier");
	    StyleConstants.setFontSize(error_text_style, 12);
	    StyleConstants.setForeground(error_text_style, new Color(96,255,96));
	
	    info_text_style  = new SimpleAttributeSet();
	    StyleConstants.setFontFamily(info_text_style, "Courier");
	    StyleConstants.setFontSize(info_text_style, 12);
	    StyleConstants.setForeground(info_text_style, new Color(100,100,100));
	
	    StyleContext sc = new StyleContext();
	    StyleContext.NamedStyle  user_text_scns = sc.new NamedStyle();
	    user_text_scns.addAttributes(user_text_style);
	    lsd.setLogicalStyle(0, user_text_scns);

	    JScrollPane scroll_pane = new JScrollPane(app_jtp);

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    c.weighty = 2.0;
	    c.weightx = 2.0;
	    c.fill = GridBagConstraints.BOTH;

	    gridbag.setConstraints(scroll_pane, c);
	    
	    //scroll_pane.setPreferredSize(new Dimension(400, 200));

	    text_panel.add(scroll_pane);

	}

	{ 
	    JPanel wrapper = new JPanel();

	    GridBagLayout wrapbag = new GridBagLayout();
	    wrapper.setLayout(wrapbag);

	    // ---------------------

	    JButton button = new JButton("Send");
	    button.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			openSendDataPanel();
		    }
		});
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.WEST;
	    wrapbag.setConstraints(button, c);
	    wrapper.add(button);

	    button = new JButton("Rec'v");
	    button.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			openRecvDataPanel();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.WEST;
	    wrapbag.setConstraints(button, c);
	    wrapper.add(button);

	    final JButton hjb = new JButton("Help");
	    wrapper.add(hjb);
	    
	    hjb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			mview.getPluginHelpTopic("AppInABox", "AppInABox");
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.EAST;
	    c.weightx = 1.0;
	    wrapbag.setConstraints(hjb, c);

	    final JButton cljb = new JButton("Clear");
	    wrapper.add(cljb);
	    cljb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			app_jtp.setText("");
			last_text_end = 0;
			app_jtp.requestFocus();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = 3;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.EAST;
	    wrapbag.setConstraints(cljb, c);

	    final JButton cjb = new JButton("Close");
	    wrapper.add(cjb);
	    
	    cjb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			cleanUp();
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = 4;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.EAST;
	    wrapbag.setConstraints(cjb, c);

	    
	    // ---------------------

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    //c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;

	    gridbag.setConstraints(wrapper, c);
	    text_panel.add(wrapper);
	}

	frame.getContentPane().add(text_panel, BorderLayout.CENTER);
	frame.pack();

	return frame;
	
    }
    
    //
    // =====================================================================
    // controlling the styles....
    // =====================================================================
    //


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   run the program....
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    private OutputStream os;
    private InputStream is;
    private InputStream es;

    private void stopApplication()
    {
	try
	{
	    if(writer != null)
		os.close();
	}
	catch (IOException ioe)
	{
	    System.out.println("unable to close OutputStream...error: \n" + ioe);
	}
	
	try
	{
	    if(reader != null)
		is.close();
	}
	catch (IOException ioe)
	{
	    System.out.println("unable to close InputStream...error: \n" + ioe);
	}
	
	if(remote)
	{
	    //System.out.println("remote host is " + remote_host);
	    
	    try
	    {
		if(socket_to_server != null)
		    socket_to_server.close();
	    }
	    catch (IOException ioe)
	    {
		System.out.println("unable to close Socket...error: \n" + ioe);
	    }
	}
	else
	{
	    if(process != null)
	    {
		new CleanStopper().start();
		new DirtyStopper().start();
	    }
	}
	
    }

    // special handling required for processes which dont show any inclination of stopping
    // when the pipe is closed...

    public class CleanStopper extends Thread
    {
	public void run()
	{
	    try
	    {
		System.out.println("trying to let process exit cleanly...");

		if(process != null)
		    process.waitFor();
		
		process = null;

		if(debug_start_stop)
		    System.out.println("app stopped  ok...");
	    }
	    catch (InterruptedException ie)
	    {
		System.out.println("unable to stop application...error: \n" + ie);
	    }
	}
    }

    public class DirtyStopper extends Thread
    {
	public void run()
	{
	    try
	    {
		// give it a few seconds to die naturally....
		sleep(5000);

		if(process != null)
		{
		    System.out.println("AppInABox.DirtyStopper() killing process...");
		    process.destroy();
		
		    process = null;
		}
		else
		{
		    System.out.println("AppInABox.DirtyStopper() app stopped  ok...");
		}
	    }
	    catch (InterruptedException ie)
	    {
		System.out.println("AppInABox.DirtyStopper() unable to sleep...error: \n" + ie);
	    }
	}
    }

    private void startApplication()
    {
	if(remote)
	{
	    //System.out.println("remote host is " + remote_host + " port " + remote_port);
	    try 
	    {
		socket_to_server = new Socket(remote_host, remote_port);
		
		is  = socket_to_server.getInputStream();
		os  = socket_to_server.getOutputStream();
		es  = null;

		writer = new PrintWriter(os, true);
		reader = new InputStreamReader(is);
	    } 
	    catch (UnknownHostException e) 
	    {
		mview.alertMessage("Unknown host: " + remote_host);
		System.err.println("Unknown host: " + remote_host);
		return;
	    } 
	    catch (IOException e) 
	    {
		mview.alertMessage("Unable to connect to port " + remote_port + " on " + remote_host + "\n\nIs there is a RemoteAppServer running on the host?");
		System.err.println("Unable to connect to port " + remote_port + " on " + remote_host);
		return;
	    }
	    
	    System.out.println("connection established");
	    
	     // what are we talking to?
	    writeCommandToApp("version", "");
	    // check for acknowedgement
	    String remote_ver = readFromApp();
	    if(remote_ver == null)
	    {
		remote_ver = "(no response from 'version' command)";
	    }

	    // send a request to start the app
	    writeCommandToApp("start", app_path);

	    // check for acknowedgement
	    String response = readFromApp();
	    if(response == null)
	    {
		mview.alertMessage("Couldn't get response to 'start' command");
		return;
	    }
	    if(response.startsWith("ack"))
	    {
		//mview.infoMessage("Application started\n\napp: " + app_path + 
		//		  ",\nvia: " + remote_ver + ",\n" +
		//		  "on: " + remote_host);
		lsd.insertString(lsd.getLength(), "(connected to " + remote_host + 
				 "\n via " + remote_ver + ")\n", info_text_style);
		app_jtp.setCaretPosition(lsd.getLength());
	    }
	    else
	    {
		String reason = response.substring(5);
		
		lsd.insertString(lsd.getLength(), "(cannot connect to " + remote_host + 
				 " reason: " + reason + ")\n", error_text_style);
		app_jtp.setCaretPosition(lsd.getLength());
		
		mview.alertMessage("Application not started\n\napp: " + app_path + 
				   ",\nvia: " + remote_ver + ",\n" +
				   "on: " + remote_host + ",\n" +
				   "reason: " + reason);	
		return;
	    }
	    
	    //reader   = new BufferedReader(new InputStreamReader(socket_to_server.getOutputStream()));
	    //e_reader = new BufferedReader(new InputStreamReader(socket_to_server.getErrorStream()));
	    
	}
	else
	{
	    //System.out.println("local app is " + app_path);

	    try
	    {
		Runtime rt = Runtime.getRuntime();
		
		process = rt.exec(app_path);
		
		if(debug_start_stop)
		    System.out.println("app '" + app_path + "' started ok...");
		
		os  = process.getOutputStream(); // output _from_ plugin to Process
		//writer = new BufferedWriter(new OutputStreamWriter(os));
		writer = new OutputStreamWriter(os);
		
		is = process.getInputStream();   // input _from_ Process to plugin 
		//reader = new BufferedReader(new InputStreamReader(is));
		reader = new InputStreamReader(is);
		
		es = process.getInputStream();   // input _from_ Process to plugin 
		//reader = new BufferedReader(new InputStreamReader(is));
		e_reader = new InputStreamReader(es);
		
		if(debug_start_stop)
		    System.out.println("streams connected...");

		lsd.insertString(lsd.getLength(), "(connected)\n", info_text_style);
		app_jtp.setCaretPosition(lsd.getLength());
	    }
	    catch(java.io.IOException ioe)
	    {
		process = null;
		writer = null;
		reader = null;
		mview.alertMessage("Unable to start application '" + app_path+  "'\n...error: \n" + ioe);
	    }
	    catch(Exception e)
	    {
		process = null;
		writer = null;
		reader = null;
		mview.alertMessage("Unexpected exception whilst starting application '" + app_path+  "'\n...error: \n" + e);
	    }
	}
    }

    private void writeCommandToApp(String com, String arg)
    {
	//System.out.println("writing to app...");
	if(writer == null)
	    return;

	try
	{
	    writer.write(com);
	    writer.write(":");
	    writer.write(arg);
	    writer.write("\n");
	    writer.flush();
	    
	    if(debug_comms)
		System.out.println("command : " + com + ":" + arg + " sent to remote app");
	}
	catch(IOException ioe)
	{
	    //System.out.println("problem writing to app\n  " + ioe);
	    lsd.insertString(lsd.getLength(), "(unable to write to app)\n", error_text_style);
	}
	
    }


    private void writeToApp(String str)
    {
	//System.out.println("writing to app...");
	if(writer == null)
	    return;

	try
	{
	    if(remote)
		writer.write("input:");

	    writer.write(str);
	    writer.write("\n");
	    writer.flush();
	    
	    if(debug_comms)
		System.out.println((str.length()+1) + " char" + ((str.length() == 1) ? " sent" : "s sent"));

	}
	catch(IOException ioe)
	{
	    //System.out.println("problem writing to app\n  " + ioe);
	    lsd.insertString(lsd.getLength(), "(unable to write to app)\n", error_text_style);
	}
	
    }

    private void snooze()
    {
	try
	{
	    Thread.sleep(500);
	}
	catch(java.lang.InterruptedException ie)
	{
	    
	}
    }

    private String readFromApp()
    {
	if(reader == null)
	    return "(not connected)\n";

	StringBuffer sbuf = new StringBuffer();
	
	if(debug_comms)
	    System.out.println("reading from app...");

	try
	{
	    int avail = 1;

	    while(avail > 0)
	    {
		//System.out.println(len +  " chars available");
		int len = is.available();
		char[] cbuf = new char[len];

		if(reader.read(cbuf, 0, len) != len)
		{
		    System.out.println("couldn't read all " + len + " chars");
		}
		sbuf.append(new String(cbuf));

		if(es != null)
		{
		    len = es.available();
		    cbuf = new char[len];
		    
		    if(e_reader.read(cbuf, 0, len) != len)
		    {
			System.out.println("couldn't read all " + len + " chars");
		    }
		    sbuf.append(new String(cbuf));
		}

		if(debug_comms)
		{
		    String res = sbuf.toString();
		    System.out.println(" ..." + res.length() + " char" + ((res.length() == 1) ? " read" : "s read"));
		}
		
		String res = sbuf.toString();
		int cnt = 0;
		for(int c=0; c< res.length(); c++)
		    if(res.charAt(c) == '\t')
			cnt++;
		if(cnt > 0)
		    System.out.println(cnt + " TABs in data");

		// give the application a chance to do something....
		snooze();

		avail = is.available();
		if(es != null)
		    avail += es.available();
	    }


	    if(sbuf.length() == 0)
	    {
		if(debug_comms)
		    System.out.println(" ...no data available");
	    }
	}
	catch(IOException ioe)
	{
	    if(debug_comms)
		System.out.println("problem reading from app\n  " + ioe);
	}

	//System.out.println(sbuf.toString().length() + " chars read");

	return sbuf.toString();
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   communications
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void openSendDataPanel()
    {
	//if(send_panel == null)
	{
	    send_panel = new JPanel();
	    send_panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
	    GridBagLayout gridbag = new GridBagLayout();
	    //panel.setPreferredSize(new Dimension(400, 300));
	    send_panel.setLayout(gridbag);
	    
	    final JCheckBox[] meas_sel_jchkb = new JCheckBox[edata.getNumMeasurements()];
	    final JTextField  name_jtf = new JTextField(12);
	    final JCheckBox   filter_jchkb = new JCheckBox("Apply filter");

	    int line = 0;
	    GridBagConstraints c = null;
	    
	    {
		JPanel wrapper = new JPanel();
		JLabel label = new JLabel("Matrix name:");
		wrapper.add(label);

		wrapper.add(name_jtf);

		// ---------------------
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridwidth = 2;
		c.gridy = line++;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		
		gridbag.setConstraints(wrapper, c);
		send_panel.add(wrapper);
	    }

	    {
		JPanel meas_panel = new JPanel();
		GridBagLayout meas_bag = new GridBagLayout();
		meas_panel.setLayout(meas_bag);
		
		for(int m=0; m < edata.getNumMeasurements(); m++)
		{
		    meas_sel_jchkb[m] = new JCheckBox(edata.getMeasurementName(m));
		    c = new GridBagConstraints();
		    c.anchor = GridBagConstraints.WEST;
		    c.gridx = 0;
		    c.gridy = m;
		    c.weightx = 1.0;
		    meas_bag.setConstraints(meas_sel_jchkb[m], c);
		    meas_panel.add(meas_sel_jchkb[m]);
		}
		
		JScrollPane jsp = new JScrollPane(meas_panel);

		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line++;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.gridwidth = 2;
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.BOTH;
		gridbag.setConstraints(jsp, c);
		send_panel.add(jsp);
	    }

	    if((edata.getNumMeasurements() / 2) > 1)
	    {
		{
		    JButton jb = new JButton("All");
		    
		    jb.addActionListener(new ActionListener() 
			{
			    public void actionPerformed(ActionEvent e) 
			    {
				for(int tm=0; tm < mview.getExprData().getNumMeasurements(); tm++)
				    meas_sel_jchkb[tm].setSelected(true);
			    }
			});
		    
		    c = new GridBagConstraints();
		    c.anchor = GridBagConstraints.EAST;
		    c.gridx = 0;
		    c.weightx = 1.0;
		    c.gridy = line;
		    gridbag.setConstraints(jb, c);
		    send_panel.add(jb);		
		}
		{
		    JButton jb = new JButton("None");
		    
		    jb.addActionListener(new ActionListener() 
			{
			    public void actionPerformed(ActionEvent e) 
			    {
				for(int tm=0; tm < mview.getExprData().getNumMeasurements(); tm++)
				    meas_sel_jchkb[tm].setSelected(false);
			    }
			});
		    
		    c = new GridBagConstraints();
		    c.anchor = GridBagConstraints.WEST;
		    c.gridx = 1;
		    c.weightx = 1.0;
		    c.gridy = line++;
		    gridbag.setConstraints(jb, c);
		    send_panel.add(jb);		
		}
	    }

	    {
		c = new GridBagConstraints();
		//c.anchor = GridBagConstraints.WEST;
		c.gridx = 0;
		c.gridy = line++;
		c.weightx = 1.0;
		c.gridwidth = 2;
		gridbag.setConstraints(filter_jchkb, c);
		send_panel.add(filter_jchkb);
	    }


	    { 
		JPanel wrapper = new JPanel();
		
		final JButton cjb = new JButton("Cancel");
		wrapper.add(cjb);
		
		cjb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    send_panel.setVisible(false);
			    frame.getContentPane().remove(send_panel);
			    frame.getContentPane().add(text_panel, BorderLayout.CENTER);
			    text_panel.setVisible(true);

			    app_jtp.requestFocus();
			}
		    });
		
		final JButton hjb = new JButton("Help");
		wrapper.add(hjb);
		
		hjb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    mview.getPluginHelpTopic("AppInABox", "AppInABox", "#send");
			}
		    });
		
		final JButton sjb = new JButton("Send");
		wrapper.add(sjb);
		
		sjb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    if(name_jtf.getText().length() > 0)
			    {
				int mc = 0;
				for(int tm=0; tm < mview.getExprData().getNumMeasurements(); tm++)
				    if(meas_sel_jchkb[tm].isSelected())
					mc++;
				if(mc > 0)
				{
				    sendData(meas_sel_jchkb, name_jtf, filter_jchkb);

				    send_panel.setVisible(false);
				    frame.getContentPane().remove(send_panel);
				    frame.getContentPane().add(text_panel, BorderLayout.CENTER);
				    text_panel.setVisible(true);

				    app_jtp.requestFocus();
				}
				else
				    mview.alertMessage("You must choose at least one Measurement.");
			    }
			    else
				mview.alertMessage("You must specify the name of the matrix to load data into.");
			}
		    });
		
		// ---------------------
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridwidth = 2;
		c.gridy = line++;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		
		gridbag.setConstraints(wrapper, c);
		send_panel.add(wrapper);
	    }

	}
	
	text_panel.setVisible(false);
	frame.getContentPane().remove(text_panel);
	frame.getContentPane().add(send_panel, BorderLayout.CENTER);
	send_panel.setVisible(true);

	//text_panel.setVisible(false);
	//send_panel.setVisible(true);

	//app_jtp.requestFocus();
    }


    public void openRecvDataPanel()
    {
	if(recv_panel == null)
	{
	    final JTextField  mat_name_jtf  = new JTextField(12);
	    final JTextField  meas_name_jtf = new JTextField(12);
	    
	    recv_panel = new JPanel();
	    recv_panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
	    GridBagLayout gridbag = new GridBagLayout();
	    GridBagConstraints c = null;
	    //panel.setPreferredSize(new Dimension(400, 300));
	    recv_panel.setLayout(gridbag);
	    
	    int line = 0;

	    
	    JLabel label = new JLabel("Matrix name: ");
	    
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	    recv_panel.add(label);
	    
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    gridbag.setConstraints(mat_name_jtf, c);
	    recv_panel.add(mat_name_jtf);
	    
	    
	    label = new JLabel("Measurement name: ");

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	    recv_panel.add(label);
	    
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 1;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    gridbag.setConstraints(meas_name_jtf, c);
	    recv_panel.add(meas_name_jtf);
	    

	    { 
		JPanel wrapper = new JPanel();
		
		final JButton cjb = new JButton("Cancel");
		wrapper.add(cjb);
		
		cjb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    recv_panel.setVisible(false);
			    frame.getContentPane().remove(recv_panel);
			    frame.getContentPane().add(text_panel, BorderLayout.CENTER);
			    text_panel.setVisible(true);

			    app_jtp.requestFocus();
			}
		    });
		
		final JButton hjb = new JButton("Help");
		wrapper.add(hjb);
		
		hjb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    mview.getPluginHelpTopic("AppInABox", "AppInABox", "#recv");
			}
		    });
		
		final JButton sjb = new JButton("Rec'v");
		wrapper.add(sjb);
		
		sjb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    if(mat_name_jtf.getText().length() > 0)
			    {
				recvData(mat_name_jtf.getText(), meas_name_jtf.getText());

				recv_panel.setVisible(false);
				frame.getContentPane().remove(recv_panel);
				frame.getContentPane().add(text_panel, BorderLayout.CENTER);
				text_panel.setVisible(true);

				app_jtp.requestFocus();
			    }
			    else
				mview.alertMessage("You must specify the name of the matrix to load data from.");
			}
		    });
		
		// ---------------------
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 3;
		c.gridwidth = 2;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.SOUTH;
		
		gridbag.setConstraints(wrapper, c);
		recv_panel.add(wrapper);
	    }

	}
	
	text_panel.setVisible(false);
	frame.getContentPane().remove(text_panel);
	frame.getContentPane().add(recv_panel, BorderLayout.CENTER);
	recv_panel.setVisible(true);

	app_jtp.requestFocus();
    }


    public void sendData(JCheckBox[] meas_sel_jchkb, JTextField name_jtf, JCheckBox filter)
    {
	boolean[] meas_a = new boolean[meas_sel_jchkb.length];
	for(int m=0; m < meas_a.length; m++)
	    meas_a[m] = meas_sel_jchkb[m].isSelected();
	
	sendDataToMatlab(meas_a, name_jtf.getText(), filter.isSelected());

    }
    
    public void recvData(String mat_name, String meas_name)
    {
	recvDataFromMatlab(mat_name, meas_name);
    }
    
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   insert data from a tokeniser string
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
 
    private String getWordFromString(String s,  Vector word_start, int w)
    {
	int ws = ((Integer)word_start.elementAt(w)).intValue();
	if((w+1) < word_start.size())
	{
	    int we = ((Integer)word_start.elementAt(w+1)).intValue();
	    return s.substring(ws, we);
	}
	else
	{
	    return s.substring(ws);
	}
    }

    private void addMeasurements(String name, int n_cols, int n_rows, String data, Vector word_start)
    {

	// are there the same number of lines as there are spots in the data?
	if(n_rows == edata.getNumSpots())
	{
	    System.out.println("exact match with Spots....");

	    for(int m=0; m < n_cols; m++)
	    {
		double[] ddata = new double[n_rows];
		
		for(int s=0; s < n_rows; s++)
		{
		    int w_pos = (s * n_cols )+ m;

		    String w = getWordFromString(data, word_start, w_pos).trim();
		    
		    //System.out.println("word " + w_pos + " is '" + w + "'");

		    try
		    {
			ddata[s] = NumberParser.tokenToDouble(w);

			//System.out.println("parsed as double " + NumberParser.tokenToDouble(w));
		    }
		    catch(TokenIsNotNumber tinn)
		    {
			ddata[s] = Double.NaN;
		    }
		}

		edata.addOrderedMeasurement(edata.new Measurement(name, ExprData.ExpressionAbsoluteDataType, ddata));
	    }
	}
	else
	{
	    // are there the same number of lines as there are filtered spots in the data?
	    
	    Vector filter_locs = new Vector();

	    int n_filtered = 0;
	    final int ns = edata.getNumSpots();
	    for(int sp=0; sp < ns; sp++)
		if(!edata.filter(sp))
		{
		    n_filtered++;
		    filter_locs.addElement(new Integer(sp));
		}
	    
	    if(n_rows == n_filtered)
	    {
		System.out.println("exact match with filtered Spots....");
		
		for(int m=0; m < n_cols; m++)
		{
		    double[] ddata = new double[ns];
		    
		    for(int s=0; s < ns; s++)
		    {
			ddata[s] = Double.NaN;
		    }

		    for(int s=0; s < n_rows; s++)
		    {
			int w_pos = (s * n_cols ) + m;
			
			int s_pos = ((Integer) filter_locs.elementAt(s)).intValue();

			String w = getWordFromString(data, word_start, w_pos).trim();
			
			//System.out.println("word " + w_pos + " is '" + w + "'");
			
			try
			{
			    ddata[s_pos] = NumberParser.tokenToDouble(w);
			    
			    //System.out.println("parsed as double " + NumberParser.tokenToDouble(w));
			}
			catch(TokenIsNotNumber tinn)
			{
			    ddata[s_pos] = Double.NaN;
			}
		    }
		    
		    edata.addOrderedMeasurement(edata.new Measurement(name, ExprData.ExpressionAbsoluteDataType, ddata));
		}

	    }
	    else
	    {
		// dont know what to do with this number of lines
		System.out.println("unable to handle " + n_rows + 
				   " rows, doesn't match all spot count, or filtered spot count.");
	    }
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   matlab interface
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // 
    // 
    // >> a = [ 1 2 3 ; 4 6 5 ; 8 8 8 ];
    // >> a
    // 
    // a =
    // 
    //      1     2     3
    //      4     6     5
    //      8     8     8
    // 
    // >
    //
    // 

    
    //
    // ===== r e c e i v e ========================================================
    //
    public void recvDataFromMatlab(String mat_name, String meas_name)
    {
	int cols_in = 0;
	int rows_in = 0;


	// send the name of the matrix to matlab, this makes it spit out the values...
	writeToApp(mat_name);

	// give matlab a chance to respond...
	snooze();

	// what if the matrix is not known? do we get an empty matrix or an error message?

	String s = readFromApp();

	Vector word_start = new Vector();

	if(s.length() > 0)
	{
	    final int string_l = s.length();

	    int p = 0;

	    int words = 0;
	    int lines = 0;

	    boolean in_word = false;

	    while(p < string_l)
	    {
		char c = s.charAt(p);
		
		if(c == '\n')
		{
		    if(in_word)
		    {
			words++;
			in_word = false;
		    }
		    lines++;
		}
		else
		{
		    if(Character.isWhitespace(c))
		    {
			if(in_word)
			{
			    words++;
			    in_word = false;
			}
		    }
		    else
		    {
			if(!in_word)
			{
			    in_word = true;
			    
			    // record the start pos of this word...
			    word_start.addElement(new Integer(p));
			}
		    }
		}
		p++;
	    }
	    
	    // the first two lines are the matrix name and a blank line
	    rows_in = lines - 2;

	    // the first two words are the matrix name and an equals sign
	    cols_in = ((words-2) / rows_in);

	    // cross check the validity of the matrix dimensions
	    float c_in_f = (float)(words-2) / (float)rows_in;
	    float words_needed_f = (c_in_f * (float)rows_in);

	    if(words_needed_f != (float)(words-2))
	    {
		System.out.println("unexpected format");
		return;
	    }
	    else
	    {
		System.out.println(words + " words, " + lines + " lines");

		// throw away the first two words
		word_start.removeElementAt(0);
		word_start.removeElementAt(0);

		addMeasurements(meas_name, cols_in, rows_in, s, word_start);
	    }
	}

	lsd.insertString(lsd.getLength(), 
			 "( " + cols_in + " x " + rows_in + " values received from " + mat_name + " )\n", 
			 info_text_style);

	// make sure the info text is not counted as user text....
	last_text_end =  lsd.getLength();

	
    }

    //
    // ===== s e n d ==============================================================
    //
    public void sendDataToMatlab(boolean[] meas, String name, boolean filter)
    {
	int spots_out = 0;
	int rows_out = 0;

	//System.out.println("<<start>>");

	StringBuffer sbuf = new StringBuffer();
		
	sbuf.append(name + " = [ ");

	//
	//
	// N O T E:
	//
	//  make sure there are no \n's in the string sent
	//  to the app as the RemoteAppServer expects single
	//  line commands.
	//
	// TODO: should probably send them as blocked chunks
	//  with a limted line length as the data gets copied 
	//  at least 3 times....
	//

	for(int s=0; s < edata.getNumSpots(); s++)
	{
	    // int si = edata.getSpotAtIndex(s);

	    // use the natural ordering rather than the current ordering
	    // as it makes reading the data back in far easier....
	    int si = s;
	    

	    if((!filter) || (!edata.filter(si)))
	    {
		if(rows_out > 0)
		    sbuf.append("; ");

		for(int m=0; m < edata.getNumMeasurements(); m++)
		{
		    if(meas[m] == true)
		    {
			double d = edata.eValue(m, si);

			sbuf.append(String.valueOf(d) + " ");

			spots_out++;
		    }
		}

		rows_out++;

	    }
	}

	sbuf.append(" ];");
	
	writeToApp(sbuf.toString());
	
	//System.out.println("<<SEND>>" + sbuf.toString() + "<<END>>");
	
	//System.out.println("<<end>>");

	int cols_out = (spots_out / rows_out);

	lsd.insertString(lsd.getLength(), 
			 "( " + cols_out + " x " + rows_out + " values sent to " + name + " )\n", 
			 info_text_style);

	// make sure the info text is not counted as user text....
	last_text_end =  lsd.getLength();

    }

    //
    // =====================================================================
    // doings
    // =====================================================================
    //

    private JTextPane app_jtp;

    private LimitedStyledDocument lsd;

    //private StyledEditorKit.ForegroundAction app_text_style;
    //private StyledEditorKit.ForegroundAction user_text_style;

    private SimpleAttributeSet app_text_style;
    private SimpleAttributeSet user_text_style;
    private SimpleAttributeSet error_text_style;
    private SimpleAttributeSet info_text_style;

    private JTextField app_label;

    private String app_name;    // just the name
    private String app_path;    // full path including name
    private String remote_host = "127.0.0.1";
    private int remote_port = 28282;
    private boolean remote = false;

    private Process process;

    private Writer writer;
    private Reader reader;
    private Reader e_reader;
    private Socket socket_to_server = null;
	  
    private int last_text_end = 0;

    private JFrame frame;
    private JPanel text_panel, send_panel, recv_panel;

    private maxdView mview;
    private ExprData edata;

    private JPanel panel;
    private JTextArea text_area;
    
}
