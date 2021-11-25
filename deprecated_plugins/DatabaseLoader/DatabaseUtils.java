//package uk.ac.man.cs.hancockd.maxdLoader;
//package maxdLoader;

import javax.swing.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.sql.*;
import javax.swing.event.*;
import javax.swing.border.*;

//import maxdLoader.*;

//
// uses JDBC to extract data from the MAXD database
//

public class Controller extends JFrame
{
    public Controller(boolean is_applet_)
    {
	super("maxdLoader");

	addWindowListener(new WindowAdapter() 
			  {
			      public void windowClosing(WindowEvent e)
			      {
				  exitGracefully();
			      }
			  });

	is_applet = is_applet;
	if(!is_applet)
	{
	   loadPrefs();
	   openLogFile();
	}
	connect_panel = createConnectPanel();
	getContentPane().add(connect_panel, BorderLayout.CENTER);
	connect_panel.setVisible(true);
    }

    public void exitGracefully()
    {
	setVisible(false);
	if(connection != null)
	    disconnect();
	if(!is_applet)
	{
	    closeLogFile();
	    savePrefs();
	}
	System.exit(0);
    }
 
    JPanel current_panel = null;
    Vector panel_list = new Vector();

    public void addPanelToStack(JPanel panel)
    {
	if(current_panel != null)
	{
	    current_panel.setVisible(false);
	    getContentPane().remove(current_panel);
	}

	current_panel = panel;

	getContentPane().add(panel);
	panel.setVisible(true);

	panel_list.insertElementAt(panel, 0);

	//System.out.println("current panel is " + current_panel.toString());
	//System.out.println("panel added, " + panel_list.size() + " panels in stack");
    }

    public void returnToPreviousPanel()
    {
	if(panel_list.size() > 1)
	{
	    
	    if(current_panel != null)
	    {
		current_panel.setVisible(false);
		getContentPane().remove(current_panel);
	    }
	    
	    panel_list.removeElementAt(0);
	    current_panel = (JPanel) panel_list.elementAt(0);
	    
	    getContentPane().add(current_panel);
	    current_panel.setVisible(true);

	    //System.out.println("goto previous panel, " + panel_list.size() + " panels left in stack");
	    //System.out.println("current panel is " + current_panel.toString());
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  gui
    // --- --- ---  
    // --- --- ---  -- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public JPanel connect_panel = null;
    private JTextField host_jtf, acct_jtf, log_file_jtf;
    private JCheckBox do_log_jcb;
    private JPasswordField passwd_jtf;

    public JPanel createConnectPanel()
    {
	JPanel panel = new JPanel();
	panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	GridBagLayout gridbag = new GridBagLayout();
	panel.setPreferredSize(new Dimension(400, 600));
	panel.setLayout(gridbag);

	int line = 0;

	{
	    JLabel label = new JLabel("Host  ");
	    panel.add(label);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.weightx = c.weighty = 0.0;
	    c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(label, c);
	}
	{
	    host_jtf = new JTextField(20);
	    panel.add(host_jtf);

	    if(default_host != null)
		host_jtf.setText(default_host);
	    
	    // jdbc:oracle:thin:@aardvark.cs.man.ac.uk:1526:teach
	    // jdbc:postgresql:maxd
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    c.weightx = 2.0;
	    c.weighty = 1.0;
	    gridbag.setConstraints(host_jtf, c);
	    
	}
	line++;
	{
	    JLabel label = new JLabel("Account  ");
	    panel.add(label);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.weightx = c.weighty = 0.0;
	    c.anchor = GridBagConstraints.EAST;
      	    gridbag.setConstraints(label, c);
	}
	{
	    acct_jtf = new JTextField(20);
	    panel.add(acct_jtf);

	    if(default_account != null)
		acct_jtf.setText(default_account);

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    c.weightx = 2.0;
	    c.weighty = 1.0;
	    gridbag.setConstraints(acct_jtf, c);
	    
	}
	line++;
	{
	    JLabel label = new JLabel("Password  ");
	    panel.add(label);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.anchor = GridBagConstraints.EAST;
	    c.weightx = c.weighty = 0.0;
	    gridbag.setConstraints(label, c);
	}
	{
	    passwd_jtf = new JPasswordField(20);
	    panel.add(passwd_jtf);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    c.weightx = 2.0;
	    c.weighty = 1.0;
	    gridbag.setConstraints(passwd_jtf, c);
	    
	}
	line++;

	{
	    JLabel label = new JLabel("Keep log ?  ");
	    panel.add(label);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.anchor = GridBagConstraints.EAST;
	    c.weightx = c.weighty = 0.0;
	    gridbag.setConstraints(label, c);
	}
	{
	    do_log_jcb = new JCheckBox();
	    panel.add(do_log_jcb);
	    do_log_jcb.setSelected(logging);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    c.weightx = 2.0;
	    c.weighty = 1.0;
	    gridbag.setConstraints(do_log_jcb, c);
	    
	}
	line++;

	{
	    JLabel label = new JLabel("file  ");
	    panel.add(label);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.anchor = GridBagConstraints.NORTHEAST;
	    c.weightx = c.weighty = 0.0;
	    gridbag.setConstraints(label, c);
	}
	{
	    log_file_jtf = new JTextField(20);
	    panel.add(log_file_jtf);
	    log_file_jtf.setText(log_file_name);

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.NORTHWEST;
	    c.weightx = 2.0;
	    c.weighty = 1.0;
	    gridbag.setConstraints(log_file_jtf, c);
	    
	}
	line++;

	{
	    JPanel buttons_panel = new JPanel();
	    GridBagLayout inner_gridbag = new GridBagLayout();
	    buttons_panel.setLayout(inner_gridbag);
	    {   
		final JButton jb = new JButton("Close");
		buttons_panel.add(jb);
		
		jb.setToolTipText("Close this dialog box");

		jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					     {
						 exitGracefully();
					     }
				     });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		gridbag.setConstraints(jb, c);
	    }
	    {
		
		final JButton jb = new JButton("Connect");
		buttons_panel.add(jb);
		
		jb.setToolTipText("Attempt to open a connection to this database");
		jb.addActionListener(new ActionListener() 
				     {
					 public void actionPerformed(ActionEvent e) 
					     {
						 attemptConnect();
					     }
				     });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		gridbag.setConstraints(jb, c);
	    }
	    {
		
		final JButton jb = new JButton("Help");
		buttons_panel.add(jb);
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		gridbag.setConstraints(jb, c);
	    }
	    panel.add(buttons_panel);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.gridwidth = 2;
	    c.weighty = c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    gridbag.setConstraints(buttons_panel, c);
	}

	return panel;
    }

    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    
    public JPanel top_menu_panel = null;

    public JPanel createTopMenuPanel()
    {
	JPanel panel = new JPanel();
	panel = new JPanel();
	panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	GridBagLayout gridbag = new GridBagLayout();
	panel.setPreferredSize(new Dimension(400, 300));
	panel.setLayout(gridbag);

	//
	// Submitter      Source       Array 
	// Expt           Sample       Hyb
	// ArryType       Extract      Image
	//

	panel.setBackground(new Color(45, 67, 123));

	{
	   JLabel label = new JLabel("What do you want to load today?");
	   //label.setFont(small_bold_font);
	   panel.add(label);

	   GridBagConstraints c = new GridBagConstraints();
	   c.gridx = 0;
	   c.gridy = 0;
	   c.gridwidth = 3;
	   c.weightx = c.weighty = 1.0;
	   gridbag.setConstraints(label, c);
	}

	{
	   JButton button = new JButton("Measurement");
	   button.addActionListener(new ActionListener() 
				    {
					public void actionPerformed(ActionEvent e) 
					{
					    // make a new experiment creator
					    new Measurement(Controller.this).createCreateMeasurementPanel(null);
					}
				    });
	   panel.add(button);

	   GridBagConstraints c = new GridBagConstraints();
	   c.gridx = 0;
	   c.gridy = 1;
	   c.weightx = c.weighty = 1.0;
	   c.gridwidth = 3;
	   c.fill = GridBagConstraints.BOTH;
	   gridbag.setConstraints(button, c);
	}

	{
	   JButton button = new JButton("Submitter");
	   button.addActionListener(new ActionListener() 
				    {
					public void actionPerformed(ActionEvent e) 
					{
					    // make a new experiment creator
					    new Submitter(Controller.this).createCreateSubmitterPanel(null);
					}
				    });
	   panel.add(button);

	   GridBagConstraints c = new GridBagConstraints();
	   c.gridx = 0;
	   c.gridy = 2;
	   c.weightx = c.weighty = 1.0;
	   c.fill = GridBagConstraints.BOTH;
	   gridbag.setConstraints(button, c);
	}

	{
	   JButton button = new JButton("Experiment");
	   button.addActionListener(new ActionListener() 
				    {
					public void actionPerformed(ActionEvent e) 
					{
					    // make a new experiment creator
					    new Experiment(Controller.this).createCreateExperimentPanel(null);
					}
				    });
	   panel.add(button);

	   GridBagConstraints c = new GridBagConstraints();
	   c.gridx = 0;
	   c.gridy = 3;
	   c.weightx = c.weighty = 1.0;
	   c.fill = GridBagConstraints.BOTH;
	   gridbag.setConstraints(button, c);
	}

	{
	   JButton button = new JButton("ArrayType");
	   panel.add(button);
	   button.addActionListener(new ActionListener() 
				    {
					public void actionPerformed(ActionEvent e) 
					{
					    ArrayType at = new ArrayType(Controller.this);
					    at.createCreateArrayTypePanel(null);
					}
				    });

	   GridBagConstraints c = new GridBagConstraints();
	   c.gridx = 0;
	   c.gridy = 4;
	   c.weightx = c.weighty = 1.0;
	   c.fill = GridBagConstraints.BOTH;
	   gridbag.setConstraints(button, c);
	}

	{
	   JButton button = new JButton("Array");
	   button.addActionListener(new ActionListener() 
				    {
					public void actionPerformed(ActionEvent e) 
					{
					    Array a = new Array(Controller.this);
					    a.createCreateArrayPanel(null);
					}
				    });
	   panel.add(button);

	   GridBagConstraints c = new GridBagConstraints();
	   c.gridx = 2;
	   c.gridy = 2;
	   c.weightx = c.weighty = 1.0;
	   c.fill = GridBagConstraints.BOTH;
	   gridbag.setConstraints(button, c);
	}

	{
	   JButton button = new JButton("Hybridisation");
	   button.addActionListener(new ActionListener() 
				    {
					public void actionPerformed(ActionEvent e) 
					{
					    Hybridisation h = new Hybridisation(Controller.this);
					    h.createCreateHybridisationPanel(null);
					}
				    });
	   panel.add(button);

	   GridBagConstraints c = new GridBagConstraints();
	   c.gridx = 2;
	   c.gridy = 3;
	   c.weightx = c.weighty = 1.0;
	   c.fill = GridBagConstraints.BOTH;
	   gridbag.setConstraints(button, c);
	}

	{
	   JButton button = new JButton("Image");
	   button.addActionListener(new ActionListener() 
				    {
					public void actionPerformed(ActionEvent e) 
					{
					    ImageObj i = new ImageObj(Controller.this);
					    i.createCreateImagePanel(null);
					}
				    });
	   panel.add(button);

	   GridBagConstraints c = new GridBagConstraints();
	   c.gridx = 2;
	   c.gridy = 4;
	   c.weightx = c.weighty = 1.0;
	   c.fill = GridBagConstraints.BOTH;
	   gridbag.setConstraints(button, c);
	}

	{
	   JButton button = new JButton("Source");
	   panel.add(button);
	   button.addActionListener(new ActionListener() 
				    {
					public void actionPerformed(ActionEvent e) 
					{
					    Source src = new Source(Controller.this);
					    src.createCreateSourcePanel(null);
					}
				    });

	   GridBagConstraints c = new GridBagConstraints();
	   c.gridx = 1;
	   c.gridy = 2;
	   c.weightx = c.weighty = 1.0;
	   c.fill = GridBagConstraints.BOTH;
	   gridbag.setConstraints(button, c);
	}

	
	{
	   JButton button = new JButton("Sample");
	   panel.add(button);
	   button.addActionListener(new ActionListener() 
				    {
					public void actionPerformed(ActionEvent e) 
					{
					    Sample sam = new Sample(Controller.this);
					    sam.createCreateSamplePanel(null);
					}
				    });

	   GridBagConstraints c = new GridBagConstraints();
	   c.gridx = 1;
	   c.gridy = 3;
	   c.weightx = c.weighty = 1.0;
	   c.fill = GridBagConstraints.BOTH;
	   gridbag.setConstraints(button, c);
	}

	{
	   JButton button = new JButton("Extract");
	   panel.add(button);
	   button.addActionListener(new ActionListener() 
				    {
					public void actionPerformed(ActionEvent e) 
					{
					    Extract ex = new Extract(Controller.this);
					    ex.createCreateExtractPanel(null);
					}
				    });
	   GridBagConstraints c = new GridBagConstraints();
	   c.gridx = 1;
	   c.gridy = 4;
	   c.weightx = c.weighty = 1.0;
	   c.fill = GridBagConstraints.BOTH;
	   gridbag.setConstraints(button, c);
	}

	{
	   JButton button = new JButton("Database stats");
	   panel.add(button);
	   button.addActionListener(new ActionListener() 
				    {
					public void actionPerformed(ActionEvent e) 
					{
					    showDatabaseStats();
					}
				    });
	   GridBagConstraints c = new GridBagConstraints();
	   c.gridx = 1;
	   c.gridy = 6;
	   //c.gridwidth = 1;
	   c.weightx = c.weighty = 1.0;
	   //c.fill = GridBagConstraints.HORIZONTAL;
	   c.anchor = GridBagConstraints. SOUTH;
	   gridbag.setConstraints(button, c);
	}

	{
	   JButton button = new JButton("QUIT");
	   panel.add(button);
	   button.addActionListener(new ActionListener() 
				    {
					public void actionPerformed(ActionEvent e) 
					{
					    exitGracefully();
					}
				    });
	   GridBagConstraints c = new GridBagConstraints();
	   c.gridx = 0;
	   c.gridy = 7;
	   c.gridwidth = 3;
	   c.weightx = c.weighty = 1.0;
	   c.fill = GridBagConstraints.HORIZONTAL;
	   c.anchor = GridBagConstraints. SOUTH;
	   gridbag.setConstraints(button, c);
	}

	return panel;
    }

    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

    public String getTableSize(String table)
    {
	String size = new String("no");

	ResultSet rs = executeQuery("SELECT COUNT(ID) FROM \"" + table  + "\"");
	
	if(rs != null)
	{
	    try
	    {
		while (rs.next()) 
		{
		    String str = rs.getString(1);
		    if(str != null)
		    {
			size = str;
		    }
		}
	    }
	    catch(SQLException sqle)
	    {
	    }
	}
	return size;
    }

    public void showDatabaseStats()
    {
	String stats = new String("");

	stats += new String(getTableSize("Submitter") + " Submitters\n");
	stats += new String(getTableSize("Experiment") + " Experiments\n");
	stats += new String(getTableSize("ArrayType") + " ArrayTypes\n\n");

	stats += new String(getTableSize("Source") + " Sources\n");
	stats += new String(getTableSize("Sample") + " Samples\n");
	stats += new String(getTableSize("Extract") + " Extracts\n\n");

	stats += new String(getTableSize("Array") + " Arrays\n");
	stats += new String(getTableSize("Hybridisation") + " Hybridisations\n");
	stats += new String(getTableSize("Image") + " Images\n");
	stats += new String(getTableSize("Measurement") + " Measurements\n\n");

	stats += new String(getTableSize("ScanningProtocol") + " ScanningProtocols\n");
	stats += new String(getTableSize("HybridisationProtocol") + " HybridisationProtocols\n");
	stats += new String(getTableSize("ExtractionProtocol") + " ExtractionProtocols\n");
	stats += new String(getTableSize("ImageAnalysisProtocol") + " ImageAnalysisProtocols\n\n");

	stats += new String(getTableSize("Gene") + " Genes\n");
	stats += new String(getTableSize("Probe") + " Probes\n");

	alertMessage(stats);
    }

    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

    public Font big_font = null;
    public Font big_bold_font = null;
    public Font small_font = null;
    public Font small_bold_font = null;

    public void getDifferentFonts()
    {
	Graphics g = getGraphics();
	Font f = g.getFont();
	big_font        = new Font(f.getName(), f.getStyle(), f.getSize() + 2);
	big_bold_font   = new Font(f.getName(), Font.BOLD,    f.getSize() + 2);
	small_font      = new Font(f.getName(), f.getStyle(), f.getSize() - 2);
	small_bold_font = new Font(f.getName(), Font.BOLD,    f.getSize() - 2);
    }

    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    private int next_id = 0;

    public String generateUniqueID(String table_name, String column_name)
    {
	if(synthesise_connection == true)
	{
	    next_id++;
	    //return new String(column_name + "." + next_id);
	    return new String(Integer.toString(next_id));
	}
	else
	{
	    String result = null;

	    int max_id = 0;
	    
	    /*
	    ResultSet rs = executeQuery("SELECT " + column_name + " FROM \"" + table_name + "\"");
	    if(rs != null)
	    {
		try
		{
		    while (rs.next()) 
		    {
			String str = rs.getString(column_name);
			if(str != null)
			{
			    int id = new Integer(str).intValue();
			    if(id >= max_id)
				max_id = id+1;
			}
			//System.out.println("generateUniqueID(): max id in " + column_name + " is " + max_id);
		    }
		}
		catch(SQLException sqle)
		{
		    
		}
		
		result = new String(Integer.toString(max_id));
	    }
	    else
		alertMessage("generateUniqueID(): table missing?");  
	    */

	    ResultSet rs = executeQuery("SELECT MAX(INT(" + column_name + ")) FROM \"" + table_name + "\"");

	    //System.out.println("SELECT MAX(INT(" + column_name + ")) FROM \"" + table_name + "\"");

	    if(rs != null)
	    {
		try
		{
		    while (rs.next()) 
		    {
			String str = rs.getString(1);
			if(str != null)
			{
			    try
			    {
				//System.out.println("generateUniqueID(): max id found, is " + str);
				max_id = (new Integer(str).intValue()) + 1;
			    }
			    catch (NumberFormatException nfe)
			    {
				max_id = 0;
				alertMessage("generateUniqueID(): " + nfe);
			    }
			}
			//System.out.println("generateUniqueID(): max id in " + column_name + " is " + max_id);
		    }
		}
		catch(SQLException sqle)
		{
		    alertMessage("generateUniqueID(): " + sqle);
		}
		
		result = new String(Integer.toString(max_id));
	    }
	    else
		alertMessage("generateUniqueID(): table missing?");  

	    return result;
	}
    }
    
    // add a new description row to the database
    //
    public String generateDescriptionID(String desc)
    {
	String id = generateUniqueID("Description", "ID");
	String sql = "INSERT INTO \"Description\" (ID, Text) VALUES ( " + id + ", '" + tidyText(desc) + "')\n";
	if(executeStatement(sql))
	{
	    //System.out.println("new description '" + desc + "' added as id " + id);
	    return id;
	}
	return null;
    }

    // matches name's to id's
    //
    public String getDescription(String id)
    {
	if(id == null)
	    return new String("");
	else
	    return getMatch("ID", id, "text", "Description");
    }

    // matches name's to id's (and any other pair of columns)
    //
    public String getMatch(String c1, String s1, String c2, String table)
    {
	ResultSet rs = executeQuery("SELECT " + c2 + " FROM \"" + table + "\" WHERE " + c1 + " = " + s1);
	String s2 = new String("");

	if(rs != null)
	{
	    try
	    {
		while (rs.next()) 
		{
		    String str = rs.getString(1);
		    if(str != null)
		    {
			s2 = str;
		    }
		    //System.out.println("getMatch(): max id in " + column_name + " is " + max_id);
		}
	    }
	    catch(SQLException sqle)
	    {
		alertMessage("getMatch(): " + sqle);
	    }
	}
	else
	    alertMessage("getMatch(): table missing?");  
	
	return s2;
    }

    // remove any illegal characters from the string
    public String tidyText(String str)
    {
	String result = str.replace('`', ' ');
	result = result.replace('\'', ' ');
	return result;
    }

    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

    private ImageIcon goat_icon = null;

    public void alertMessage(String str)
    {
	if(goat_icon == null)
	{
	    goat_icon = new ImageIcon("goat.jpg");
	}

	JOptionPane.showMessageDialog(null, str, "Alert!",  JOptionPane.ERROR_MESSAGE, goat_icon);
    }

    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

    public ResultSet executeQuery(String sql)
    {
	if(synthesise_connection == true)
	{
	    System.out.println("PRETEND EXECUTE:\n" + sql);
	    return null;
	}
	else
	{
	    try
	    {
	        //System.out.println("executeQuery(): trying to exec:\n  " + sql);
	      
		Statement stmt = connection.createStatement();
		stmt.setEscapeProcessing(false);
		ResultSet rs = stmt.executeQuery(sql);

		//System.out.println("executeQuery(): ResultSet retrieved ok");

		/*
		ResultSetMetaData rsmd = rs.getMetaData();

		int cc = rsmd.getColumnCount();
		System.out.println("               " + cc + " columns");
		for(int ccc=0;ccc<cc;ccc++)
		  System.out.println("                " + rsmd.getColumnName(ccc+1));
		*/

		return rs;
	    }
	    catch(SQLException sqle)
	    {
		String emesg = (sqle.toString().length() > 256) ? sqle.toString().substring(0,255) : sqle.toString();
		String sql_short = (sql.length() > 256) ? sql.substring(0,255) : sql;
		    
		alertMessage("executeQuery(): Unable to execute SQL query:\n  '" + sql_short + "'\nerror: " + emesg);

	    }
	    return null;
	}
    }

    public boolean executeStatement(String sql)
    {
	if(synthesise_connection == true)
	{
	    System.out.println("PRETEND EXECUTE:\n" + sql);
	    return true;
	}
	else
	{
	    try
	    {
	        // System.out.println("executeStatement(): trying to exec:\n  " + sql);
	      
		Statement stmt = connection.createStatement();
		stmt.setEscapeProcessing(false);
		stmt.executeUpdate(sql);

		if(logging && (log_file != null))
		{
		    try
		    {
			log_file.write(sql);
		    }
		    catch(IOException ioe)
		    {
			if(first_failure_of_log_file)
			{
			    alertMessage("executeQuery(): Unable to write SQL to log file");
			    first_failure_of_log_file = false;
			}
		    }
		}

		return true;
	    }
	    catch(SQLException sqle)
	    {
		String sql_short = (sql.length() > 256) ? sql.substring(0,255) : sql;
		String emesg = (sqle.toString().length() > 256) ? sqle.toString().substring(0,255) : sqle.toString();
		
		alertMessage("executeQuery(): Unable to execute SQL update:\n  '" + sql_short + "'\nerror: " + emesg);
	    }
	    return false;
	}
    }

    public void disconnect()
    {
	getContentPane().add(connect_panel, BorderLayout.CENTER);

	connect_panel.setVisible(true);
	top_menu_panel.setVisible(false);

	if(synthesise_connection)
	{

	}
	else
	{
	    try
	    {
		connection.close();
	    }
	    catch(SQLException sqle)
	    {
		JOptionPane.showMessageDialog(null, "Unable to close connection", "Unable to close connection", 
					      JOptionPane.ERROR_MESSAGE);
		
	    }
	}

    }

    public void attemptConnect()
    {
	boolean connected = false;

	if(debug_connect)
	    System.out.println("attempt to connect to " + host_jtf.getText() + 
			       "(account: " + acct_jtf.getText() + ")");

	if(synthesise_connection)
	{
	    if(debug_connect)
		System.out.println("synthetic connection to " + host_jtf.getText());
	    connected = true;
	}
	else
	{
	    try
	    {
		//DriverManager.registerDriver (new oracle.jdbc.driver.OracleDriver());

		//DriverManager.registerDriver (new jdbc.driver.OracleDriver());
	      
		connection = DriverManager.getConnection(host_jtf.getText(), 
							 acct_jtf.getText(), 
							 new String(passwd_jtf.getPassword()));
		
		if(connection != null)
		{
		    if(debug_connect)
			System.out.println("connected to " + host_jtf.getText());
		    connected = true;
		    
		}
	    }
	    catch(SQLException sqle)
	    {
		JOptionPane.showMessageDialog(null, "Unable to connect: " + sqle, "Unable to connect", 
					      JOptionPane.ERROR_MESSAGE);
	    }
	}

	if(connected)
	{
	    if(top_menu_panel == null)
	    {
		top_menu_panel = createTopMenuPanel();
	    }
	    connect_panel.setVisible(false);
	    addPanelToStack(top_menu_panel);
	}
    }

    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

    private Properties application_props = new Properties();
 
    private void loadPrefs()
    {
	try
	{
	    FileInputStream in = new FileInputStream("maxdLoader.config");
	    if(in != null)
	    {
		application_props.load(in);
		in.close();
		
		default_host     = (String)application_props.getProperty("database.host");
		default_account  = (String)application_props.getProperty("database.account");
	    }
	}
 	catch(java.io.IOException  ioe)
	{
	    default_host     = new String("jdbc:");
	}
    }

    private void savePrefs()
    {
	default_account = acct_jtf.getText();
	default_host    = host_jtf.getText();

	if(default_account != null)
	    application_props.put("database.account",    default_account);
	if(default_host != null)
	    application_props.put("database.host", default_host);

	try
	{
	    FileOutputStream out = new FileOutputStream("maxdLoader.config");
	    application_props.save(out, "maxdLoader config file v1");
	    out.close();
	}
	catch (java.io.IOException ioe)
	{
	    
	}
    }

    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

    public boolean logging = true;
    private FileWriter log_file = null;
    private boolean first_failure_of_log_file;

    private void openLogFile()
    {
	first_failure_of_log_file = true;

	try
	{
	    // generate a new file name for the log files
	    int index = 0;

	    boolean already_used = true;

	    while(already_used)
	    {
		index++;
		already_used = false;
		
		log_file_name = new String("maxdLoader.log." + index);

		File test = new File(log_file_name);
		if(test.exists())
		    already_used = true;
	    }

	    //System.out.println("using " + log_file_name + " as log file name");

	    log_file = new FileWriter(log_file_name);
	}
	catch(IOException ioe)
	{
	    log_file = null;
	    logging = false;
	}
    }

    private void closeLogFile()
    {
	try
	{
	    if(log_file != null)
		log_file.close();
	    if(log_file.length() == 0)
	    {
		System.out.println("nothing in log file, deleting it");
		log_file.delete();
	    }
	}
	catch(IOException ioe)
	{
	}
    }

    public void setLogging(boolean log_it) 
    { 
	logging = log_it;
    }
    
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

    JFileChooser fc = null;

    public JFileChooser getFileChooser() 
    {
	if(fc == null)
	    fc = new JFileChooser();
	return fc;
    }

    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
    // -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

    public boolean synthesise_connection = false;

    public boolean debug_connect = false;

    private boolean is_applet;
    private String log_file_name = null;

    public Connection connection = null;

    private String default_host = null;
    private String default_account = null;
}
