import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.util.Vector;

//
//  Interface to other programs or scripts
//
//    save some of the measurements to a temporary file,
//    exec the program with the file as an input,
//    capture the output of the program and create
//    new measurements based on this data
//

public class RunExternal implements Plugin
{
    public RunExternal(maxdView mview_)
    {
	//System.out.println("++ RunExternal is constructed ++");

	mview = mview_;

	loadDefaultOptions();
    }

    public void cleanUp()
    {
	if(frame != null)
	{
	    saveDefaultOptions();
	    
	    saveExternalProgsToFile(mview.getConfigDirectory() + "external.dat");

	    frame.setVisible(false);
	}
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
	frame = new JFrame("Run External");
	
	mview.decorateFrame( frame );

	frame.addWindowListener(new WindowAdapter() 
	    {
		public void windowClosing(WindowEvent e)
		{
		    cleanUp();
		}
	    });
	

	addComponents();
	frame.pack();
	frame.setVisible(true);

	loadExternalProgsFromFile(mview.getConfigDirectory() + "external.dat");

	buildList();

	/*
	ExternalProg ep = new ExternalProg();
	ep.name = "test_run_ext";
	ep.filename = "/home/dave/bio/maxd/scripts/test_run_ext.perl";
	ep.i_name_mode = 0;
	ep.i_meas = 0;
	ep.o_meas = 0;
	ep.output_is_contig = true;
	ep.output_has_names = false;

	addExternalProg(ep);

	ep = new ExternalProg();
	ep.name = "Another Test (v1.2)";
	ep.filename = "/home/dave/bio/maxd/scripts/another_test.perl";
	ep.i_name_mode = 0;
	ep.i_meas = 0;
	ep.o_meas = 0;
	ep.output_is_contig = true;
	ep.output_has_names = false;

	addExternalProg(ep);
	*/

    }

    public void stopPlugin()
    {
	cleanUp();
    }
  
    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("Run External", "transform", "Interface to another program", "",
								1, 0, 0);
	return pinf;
    }
    
    public PluginCommand[] getPluginCommands()
    {
	PluginCommand[] com = new PluginCommand[1];

	String[] args = new String[] { "name", "string", "", "m", "name of the external script to run" };
	
	com[0] = new PluginCommand("run", args);

	return com;
    }
	
    public void   runCommand(String name, String[] args, CommandSignal done) 
    { 
	if(name.equals("run"))
	{
	    String val = mview.getPluginArg("name", args);

	    if(val != null)
	    {
		if(ext_progs == null)
		    loadExternalProgsFromFile(mview.getConfigDirectory() + "external.dat");
		
		ExternalProg ep  = findByName(val);

		if(ep != null)
		    createRunDialog(ep);
		
	    }
	}
	if(done != null)
	    done.signal();
    } 

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  definition of external interface
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    //
    // sorts of things we might want to do:
    //
    //    output all (spot/probe/gene) names and values from one or more measurement
    //     read back a subset of the names (possibly in a differnet order) with new values,
    //     and create a new measurement
    // 
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  management of the list of known external programs
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    Vector ext_progs = null;

    public void addExternalProg(ExternalProg ep)
    {
	if(ext_progs == null)
	    ext_progs = new Vector();
	ext_progs.addElement(ep);

	buildList();
    }
    
    public ExternalProg findByName(String name)
    {
	//System.out.println("find by name: " + name);

	if(ext_progs == null)
	    return null;
	for(int ep=0; ep < ext_progs.size(); ep++)
	{
	    //System.out.println("  checking: " + ((ExternalProg) ext_progs.elementAt(ep)).name);
	
	    if(((ExternalProg) ext_progs.elementAt(ep)).name.equals(name))
		return (ExternalProg) ext_progs.elementAt(ep);
	}
	return null;
    }
    
    private void loadExternalProgsFromFile(String name)
    {
	try
	{
	    FileInputStream fis = new FileInputStream(new File(name));
	    ObjectInputStream ois = new ObjectInputStream(fis);
	    ext_progs = (Vector) ois.readObject();
	    ois.close();
	}
	catch(FileNotFoundException ioe)
	{
	    // no problem, it just means the plugin has not been run before
	    ext_progs = new Vector();

	}
	catch(ClassNotFoundException fnfe)
	{
	    mview.errorMessage("Cannot understand definitions file\n  " + fnfe);
	    ext_progs = new Vector();

	}
	catch(IOException ioe) // other than FileNotFound
	{
	    mview.errorMessage("Cannot load definitions file\n  " + ioe);
	    ext_progs = new Vector();
	}
    }

   private void saveExternalProgsToFile(String name)
    {
	try
	{
	    FileOutputStream fos = new FileOutputStream(new File(name));
	    ObjectOutputStream oos = new ObjectOutputStream(fos);
	    oos.writeObject(ext_progs);
	    oos.flush();
	    oos.close();
	}
	catch(IOException ioe)
	{
	    mview.errorMessage("Cannot save definitions file\n  " + ioe);
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  properties
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void loadDefaultOptions()
    {
	load_path_str = mview.getProperty("runext.load_path", System.getProperty("user.dir"));
	save_path_str = mview.getProperty("runext.save_path", System.getProperty("user.dir"));
	temp_path_str = mview.getProperty("runext.temp_dir",  "/tmp");
    }
    
    public void saveDefaultOptions()
    {
	mview.putProperty("runext.load_path", load_path_str);
	mview.putProperty("runext.save_path", save_path_str);
	mview.putProperty("runext.temp_dir",  temp_path_str);
    }
    
    
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  gui stuff: the list of programs to choose from
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private JPopupMenu popup = null;

    private void addComponents()
    {
	frame.getContentPane().setLayout(new BorderLayout());
	
	//getContentPane().setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

	JToolBar tool_bar = new JToolBar();
	tool_bar.setFloatable(false);

	JButton jb = new JButton("Run");
	jb.addActionListener(new ActionListener() 
			     {
				 public void actionPerformed(ActionEvent e) 
				 {
				      runProgram();
				 }
			     });
	tool_bar.add(jb);
	tool_bar.addSeparator();

	jb = new JButton("Edit");
	jb.addActionListener(new ActionListener() 
			     {
				 public void actionPerformed(ActionEvent e) 
				 {
				     editProgram();
				 }
			     });
	tool_bar.add(jb);
	tool_bar.addSeparator();
		
	jb = new JButton("Delete");
	jb.addActionListener(new ActionListener() 
			     {
				 public void actionPerformed(ActionEvent e) 
				 {
				     deleteProgram();
				 }
			     });
	tool_bar.add(jb);
	tool_bar.addSeparator();
		
	jb = new JButton("New");
	jb.addActionListener(new ActionListener() 
			     {
				 public void actionPerformed(ActionEvent e) 
				 {
				     newProgram();
				 }
			     });
	tool_bar.add(jb);
	tool_bar.addSeparator();

	jb = new JButton("Clone");
	jb.addActionListener(new ActionListener() 
			     {
				 public void actionPerformed(ActionEvent e) 
				 {
				     cloneProgram();
				 }
			     });
	tool_bar.add(jb);

	frame.getContentPane().add(tool_bar, BorderLayout.NORTH);
	
	progs_list = new JList();

	MouseListener popup_listener = new PopupMouseListener();

	progs_list.addMouseListener(popup_listener);
	
	buildList();

	JScrollPane jsp = new JScrollPane(progs_list);
	frame.getContentPane().add(jsp);
	

	tool_bar = new JToolBar();
	tool_bar.setFloatable(false);

	GridBagLayout tbar_gbag = new GridBagLayout();
	tool_bar.setLayout(tbar_gbag);

	jb = new JButton("Help");
	jb.addActionListener(new ActionListener() 
			     {
				 public void actionPerformed(ActionEvent e) 
				 {
				     mview.getPluginHelpTopic("RunExternal", "RunExternal");
				 }
			     });
	{ 
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    tbar_gbag.setConstraints(jb, c);
	}
	tool_bar.add(jb);

	tool_bar.addSeparator();
	
	jb = new JButton("Options");
	jb.addActionListener(new ActionListener() 
			     {
				 public void actionPerformed(ActionEvent e) 
				 {
				     showOptions();
				 }
			     });
	{ 
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    tbar_gbag.setConstraints(jb, c);
	}
	tool_bar.add(jb);

	jb = new JButton("Close");
	jb.addActionListener(new ActionListener() 
			     {
				 public void actionPerformed(ActionEvent e) 
				 {
				     cleanUp();
				 }
			     });
	{ 
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.EAST;
	    tbar_gbag.setConstraints(jb, c);
	}
	tool_bar.add(jb);
	frame.getContentPane().add(tool_bar, BorderLayout.SOUTH);
    }

    class PopupMouseListener extends MouseAdapter
    {
	public void mousePressed(MouseEvent e)
	{
	    if(e.isPopupTrigger() || e.isControlDown()) 
	    {
		//System.out.println("trigger...");
		
		if(popup == null)
		{
		    popup = new JPopupMenu();
		    
		    JMenuItem menu_item = new JMenuItem("Run");
		    menu_item.addActionListener(new ActionListener()
			{
			    public void actionPerformed(ActionEvent e) 
			    {
				runProgram();
			    }
			});
		    popup.add(menu_item);
		    
		    menu_item = new JMenuItem("Edit");
		    menu_item.addActionListener(new ActionListener()
			{
			    public void actionPerformed(ActionEvent e) 
			    {
				editProgram();
			    }
			});
		    popup.add(menu_item);
		    
		    menu_item = new JMenuItem("Delete");
		    menu_item.addActionListener(new ActionListener()
			{
			    public void actionPerformed(ActionEvent e) 
			    {
				deleteProgram();
			    }
			});
		    popup.add(menu_item);
		    
		    menu_item = new JMenuItem("New");
		    menu_item.addActionListener(new ActionListener()
			{
			    public void actionPerformed(ActionEvent e) 
			    {
				newProgram();
			    }
			});
		    popup.add(menu_item);
		    
		    menu_item = new JMenuItem("Clone");
		    menu_item.addActionListener(new ActionListener()
			{
			    public void actionPerformed(ActionEvent e) 
			    {
				cloneProgram();
			    }
			});
		    popup.add(menu_item);
		    
		    popup.addSeparator();
		    
		    menu_item = new JMenuItem("Close");
		    menu_item.addActionListener(new ActionListener()
			{
			    public void actionPerformed(ActionEvent e) 
			    {
				cleanUp();
			    }
			});
		    popup.add(menu_item);
		}
		popup.show(e.getComponent(), e.getX(), e.getY());
	    }
	}
	
	public void mouseClicked(MouseEvent e) 
	{
	    if (e.getClickCount() == 2) 
	    {
		//int index = progs_list.locationToIndex(e.getPoint());
		//System.out.println("Double clicked on Item " + index);
		runProgram();
	    }

	}
    };

    private void buildList()
    {
	if(ext_progs == null)
	    return;

	progs_list_model = new DefaultListModel();

	for(int ep=0; ep < ext_progs.size(); ep++)
	{
	    progs_list_model.addElement(((ExternalProg) ext_progs.elementAt(ep)).name);
	}

	progs_list.setModel(progs_list_model);
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  gui stuff: the attribute editing panel (also used for creating new entries)
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private void applyChanges(ExternalProg ep)
    {
	copyValuesToObject(ep);

	// might need to update the list
	buildList();
    }

    private void copyValuesToObject(ExternalProg ep)
    {
	ep.name     = name_jtf.getText();
	ep.filename = filename_jtf.getText();
	ep.params   = params_jtf.getText();
	
	ep.i_mode      = i_mode_jcb.getSelectedIndex();
	ep.i_delim     = i_delim_jcb.getSelectedIndex();
	ep.i_name_mode = i_name_mode_jcb.getSelectedIndex();
	ep.i_meas      = i_meas_jcb.getSelectedIndex();

	ep.o_mode      = o_mode_jcb.getSelectedIndex();
	ep.o_delim     = o_delim_jcb.getSelectedIndex();
	//ep.o_name_mode = o_name_mode_jcb.getSelectedIndex();
	ep.o_meas      = o_meas_jcb.getSelectedIndex();
    }

    private void makeNewProgram()
    {
	ExternalProg ep = new ExternalProg();

	copyValuesToObject(ep);

	addExternalProg(ep);
    }

    private void createExtProgDescDialog(final ExternalProg ep)
    {
	final JFrame frame = new JFrame("External Program Attributes");
	mview.decorateFrame( frame );
	JPanel panel = new JPanel();
	panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

	frame.getContentPane().add(panel, BorderLayout.CENTER);

	GridBagLayout gridbag = new GridBagLayout();
	panel.setLayout(gridbag);
	
	int line = 0;

	// -------------  -------------  -------------  -------------  -------------  
	// -------------  -------------  -------------  -------------  -------------  
	// -------------  -------------  -------------  -------------  -------------  

	JPanel p_panel = new JPanel();
	GridBagLayout p_gbag = new GridBagLayout();
	p_panel.setLayout(p_gbag);
	
	Color title_colour = new JLabel().getForeground().brighter();

	TitledBorder title = BorderFactory.createTitledBorder(" Program ");
	title.setTitleColor(title_colour);
	p_panel.setBorder(title);

	{
	    JLabel label = new JLabel("Name ");

	    GridBagConstraints c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.EAST;
	    c.gridx = 0;
	    c.gridy = line;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    p_gbag.setConstraints(label, c);

	    p_panel.add(label);
	}
	{
	    name_jtf = new JTextField(20);
	    
	    if(ep != null)
		name_jtf.setText(ep.name);

	    GridBagConstraints c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.BOTH;
	    c.gridx = 1;
	    c.gridwidth = 2;
	    c.gridy = line;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    p_gbag.setConstraints(name_jtf, c);
	    
	    p_panel.add(name_jtf);
	}
	line++;

	{
	    JLabel label = new JLabel("Filename ");

	    GridBagConstraints c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.EAST;
	    c.gridx = 0;
	    c.gridy = line;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    p_gbag.setConstraints(label, c);

	    p_panel.add(label);
	}
	{
	    filename_jtf = new JTextField(20);
	    if(ep != null)
		filename_jtf.setText(ep.filename);

	    GridBagConstraints c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.BOTH;
	    c.gridx = 1;
	    c.gridy = line;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    p_gbag.setConstraints(filename_jtf, c);
	    
	    p_panel.add(filename_jtf);
	}
	{
	    final JButton browse_jb = new JButton("Browse");

	    browse_jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			if(ep != null)
			{
			    JFileChooser fc = mview.getFileChooser(ep.filename);
			    if(fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
			    {
				File file = fc.getSelectedFile();
				if(file != null)
				    filename_jtf.setText(file.getPath());
			    }
			}
		    }
		});
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = line;
	    p_gbag.setConstraints(browse_jb, c);

	    p_panel.add(browse_jb);
	}

	
	line++;

	{
	    JLabel label = new JLabel("Arguments ");

	    GridBagConstraints c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.EAST;
	    c.gridx = 0;
	    c.gridy = line;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    p_gbag.setConstraints(label, c);

	    p_panel.add(label);
	}
	{
	    params_jtf = new JTextField(20);
	    if(ep != null)
		params_jtf.setText(ep.params);

	    GridBagConstraints c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.BOTH;
	    c.gridx = 1;
	    c.gridy = line;
	    c.gridwidth = 2;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    p_gbag.setConstraints(params_jtf, c);
	    
	    p_panel.add(params_jtf);
	}

	{
	    GridBagConstraints c = new GridBagConstraints();
	    //c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.BOTH;
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    gridbag.setConstraints(p_panel, c);
	    panel.add(p_panel);
	}


	// -------------  -------------  -------------  -------------  -------------  
	// -------------  -------------  -------------  -------------  -------------  
	// -------------  -------------  -------------  -------------  -------------  

	JPanel i_panel = new JPanel();
	GridBagLayout i_gbag = new GridBagLayout();
	i_panel.setLayout(i_gbag);

	JLabel dummy = new JLabel("");
	dummy.setMinimumSize(new Dimension(20,20));
	dummy.setPreferredSize(new Dimension(20,20));

	// make a dummy entry to give a blank line in the gridbag
	{
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    gridbag.setConstraints(dummy, c);
	    
	    panel.add(dummy);
	}
	
	title = BorderFactory.createTitledBorder(" from maxdView to program ");
	title.setTitleColor(title_colour);
	i_panel.setBorder(title);

	line = 0;

	{
	    JLabel label = new JLabel("Method ");

	    GridBagConstraints c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.EAST;
	    c.gridx = 0;
	    c.gridy = line;
	    //	    c.weightx = 1.0;
	    //c.weighty = 1.0;
	    i_gbag.setConstraints(label, c);

	    i_panel.add(label);
	}
	{
	    String[] opts = new String[] { "Standard Input (STDIN)", "Temporary file" };

	    i_mode_jcb = new JComboBox(opts);
	    if(ep != null)
	      i_mode_jcb.setSelectedIndex(ep.i_mode);

	    GridBagConstraints c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.BOTH;
	    c.gridx = 1;
	    c.gridy = line;
	    c.gridwidth = 2;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    i_gbag.setConstraints(i_mode_jcb, c);
	    
	    i_panel.add(i_mode_jcb);
	}
	line++;

	String[] delim_opts = new String[] { "Tab delimited ('\\t')", "Space delimited (' ')", "Comma delimited (',')" };

	{
	    JLabel label = new JLabel("Format ");

	    GridBagConstraints c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.EAST;
	    c.gridx = 0;
	    c.gridy = line;
	    //c.weightx = 1.0;
	    //c.weighty = 1.0;
	    i_gbag.setConstraints(label, c);

	    i_panel.add(label);
	}
	{
	    i_delim_jcb = new JComboBox(delim_opts);
	    if(ep != null)
	      i_delim_jcb.setSelectedIndex(ep.i_delim);

	    GridBagConstraints c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.BOTH;
	    c.gridx = 1;
	    c.gridy = line;
	    c.gridwidth = 2;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    i_gbag.setConstraints(i_delim_jcb, c);
	    
	    i_panel.add(i_delim_jcb);
	}
	line++;

	{
	    JLabel label = new JLabel("Contents ");

	    GridBagConstraints c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.EAST;
	    c.gridx = 0;
	    c.gridy = line;
	    //c.weightx = 1.0;
	    //c.weighty = 1.0;
	    i_gbag.setConstraints(label, c);

	    i_panel.add(label);
	}

	{
	    String[] opts = new String[] { "No measurements", "One Measurement", "Any set of Measurements", "Measurements of one type" };

	    i_meas_jcb = new JComboBox(opts);
	    if(ep != null)
	      i_meas_jcb.setSelectedIndex(ep.i_meas);
	    //if(ep != null)
	    //params_jtf.setText(ep.params);

	    GridBagConstraints c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.BOTH;
	    c.gridx = 1;
	    c.gridy = line;
	    c.gridwidth = 2;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    i_gbag.setConstraints(i_meas_jcb, c);
	    
	    i_panel.add(i_meas_jcb);
	}
	line++;

	{
	    String[] opts = new String[] { "No names", "Names optional", "Names required" };

	    i_name_mode_jcb = new JComboBox(opts);
	    if(ep != null)
	      i_name_mode_jcb.setSelectedIndex(ep.i_name_mode);

	    GridBagConstraints c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.BOTH;
	    c.gridx = 1;
	    c.gridwidth = 2;
	    c.gridy = line;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    i_gbag.setConstraints(i_name_mode_jcb, c);
	    
	    i_panel.add(i_name_mode_jcb);
	}

	{
	    GridBagConstraints c = new GridBagConstraints();
	    //c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.BOTH;
	    c.gridx = 0;
	    c.gridy = 2;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    gridbag.setConstraints(i_panel, c);
	    panel.add(i_panel);
	}
	
	// ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 
	// ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 
	// output data (i.e. the data sent to the external program)
	// ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 
	// ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 

	// make a dummy entry to give a blank line in the gridbag
	{
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 3;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    gridbag.setConstraints(dummy, c);
	    
	    panel.add(dummy);
	}

	JPanel o_panel = new JPanel();
	GridBagLayout o_gbag = new GridBagLayout();
	o_panel.setLayout(o_gbag);
	
        title = BorderFactory.createTitledBorder(" from program to maxdView ");
	title.setTitleColor(title_colour);
	o_panel.setBorder(title);

	line = 0;

	{
	    JLabel label = new JLabel("Method ");

	    GridBagConstraints c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.EAST;
	    c.gridx = 0;
	    c.gridy = line;
	    //c.weightx = 1.0;
	    //c.weighty = 1.0;
	    o_gbag.setConstraints(label, c);

	    o_panel.add(label);
	}
	{
	    String[] opts = new String[] { "Standard Output (STDOUT)", "Temporary file" };

	    o_mode_jcb = new JComboBox(opts);
	    if(ep != null)
	      o_mode_jcb.setSelectedIndex(ep.o_mode);

	    GridBagConstraints c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.BOTH;
	    c.gridx = 1;
	    c.gridwidth = 2;
	    c.gridy = line;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    o_gbag.setConstraints(o_mode_jcb, c);
	    
	    o_panel.add(o_mode_jcb);
	}
	line++;
	{
	    JLabel label = new JLabel("Format ");

	    GridBagConstraints c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.EAST;
	    c.gridx = 0;
	    c.gridy = line;
	    //c.weightx = 1.0;
	    //c.weighty = 1.0;
	    o_gbag.setConstraints(label, c);

	    o_panel.add(label);
	}
	{
	    o_delim_jcb = new JComboBox(delim_opts);
	    if(ep != null)
	      o_delim_jcb.setSelectedIndex(ep.o_delim);

	    GridBagConstraints c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.BOTH;
	    c.gridx = 1;
	    c.gridwidth = 2;
	    c.gridy = line;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    o_gbag.setConstraints(o_delim_jcb, c);
	    
	    o_panel.add(o_delim_jcb);
	}
	line++;

	{
	    JLabel label = new JLabel("Contents ");

	    GridBagConstraints c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.EAST;
	    c.gridx = 0;
	    c.gridy = line;
	    //c.weightx = 1.0;
	    //c.weighty = 1.0;
	    o_gbag.setConstraints(label, c);

	    o_panel.add(label);
	}

	{
	    String[] opts = new String[] { "No Measurements (just text)", "One Measurement", "One or more Measurements", "List of names" };

	    o_meas_jcb = new JComboBox(opts);
	    if(ep != null)
	      o_meas_jcb.setSelectedIndex(ep.o_meas);
	    //if(ep != null)
	    //params_jtf.setText(ep.params);

	    GridBagConstraints c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.BOTH;
	    c.gridx = 1;
	    c.gridwidth = 2;
	    c.gridy = line;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    o_gbag.setConstraints(o_meas_jcb, c);
	    
	    o_panel.add(o_meas_jcb);
	}
	line++;

	{
	    GridBagConstraints c = new GridBagConstraints();
	    //c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.BOTH;
	    c.gridx = 0;
	    c.gridy = 4;
	    //c.gridwidth = 2;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    gridbag.setConstraints(o_panel, c);
	    panel.add(o_panel);
	}

	/*
	{
	    String[] opts = new String[] { "No names", "Depends on input", "Names supplied" };

	    o_name_mode_jcb = new JComboBox(opts);
	    if(ep != null)
	      o_name_mode_jcb.setSelectedIndex(ep.o_name_mode);

	    GridBagConstraints c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.BOTH;
	    c.gridx = 1;
	    c.gridy = line;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    gridbag.setConstraints(o_name_mode_jcb, c);
	    
	    panel.add(o_name_mode_jcb);
	}
	line++;
	*/

	// make a dummy entry to give a blank line in the gridbag
	{
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 5;
	    c.weightx = 1.0;
	    c.weighty = 1.0;
	    gridbag.setConstraints(dummy, c);
	    
	    panel.add(dummy);
	}
	
	{
	    JPanel buttons_panel = new JPanel();
	    buttons_panel.setBorder(BorderFactory.createEmptyBorder(10,10,0,10));
	    GridBagLayout inner_gridbag = new GridBagLayout();
	    buttons_panel.setLayout(inner_gridbag);

	    {
		final JButton jb = new JButton("Cancel");
		buttons_panel.add(jb);
		
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    frame.setVisible(false);
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		gridbag.setConstraints(jb, c);
	    }
	    {
		final JButton jb = new JButton("Help");
		buttons_panel.add(jb);
		
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		gridbag.setConstraints(jb, c);
	    }
	    
	    if(ep == null)
	    {   
		// we are creating a new one
		//
		
		final JButton jb = new JButton("OK");
		buttons_panel.add(jb);
		
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    makeNewProgram();
			    frame.setVisible(false);
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		gridbag.setConstraints(jb, c);
	    }
	    else
	    {
		// we are editing an existing program
		//
		final JButton jb = new JButton("Apply");
		buttons_panel.add(jb);
		
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    applyChanges(ep);
			    frame.setVisible(false);
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		gridbag.setConstraints(jb, c);
	    }

	    panel.add(buttons_panel);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 6;
	    gridbag.setConstraints(buttons_panel, c);
	}

	frame.pack();
	frame.setVisible(true);

    }

    private JTextField params_jtf, filename_jtf, name_jtf;

    private JComboBox i_meas_jcb, o_meas_jcb;
    private JComboBox i_name_mode_jcb; //, o_name_mode_jcb;
    private JComboBox o_mode_jcb, i_mode_jcb;
    private JComboBox o_delim_jcb, i_delim_jcb;

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  gui stuff: parameter selection before the program is run
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private JComboBox meas_choice_jcb, meas_type_choice_jcb, name_choice_jcb;
    private JTextField i_tmp_name_jtf, o_tmp_name_jtf;
    private JCheckBox[] meas_sel_jchkb = null;
    private JTextField o_new_name_prefix_jtf;
    private JCheckBox apply_filter_jchkb, only_clusters_jchkb;
    private JComboBox o_name_result_mode_jcb;

    private void createRunDialog(final ExternalProg ep)
    {
	final JFrame run_frame = new JFrame("Run External Program: " + ep.name);

	mview.decorateFrame( run_frame );

	if(process != null)
	{
	    mview.alertMessage("External program already running");
	    return;
	}

	JLabel dummy = new JLabel("");
	dummy.setMinimumSize(new Dimension(20,20));
	dummy.setPreferredSize(new Dimension(20,20));
	Color title_colour = dummy.getForeground().brighter();

	JPanel panel = new JPanel();
	panel.setMinimumSize(new Dimension(350,200));
	panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

	run_frame.getContentPane().add(panel, BorderLayout.CENTER);

	GridBagLayout gridbag = new GridBagLayout();
	panel.setLayout(gridbag);
	
	int line = 0;

	// ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 
	// ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 
	// input data (i.e. the data sent to the external program)
	// ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 
	// ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 

	boolean has_input = ((ep.i_meas != InputNoMeasurement) || (ep.i_name_mode != InputNoNames));

	JPanel i_panel = new JPanel();
	GridBagLayout i_gbag = new GridBagLayout();
	i_panel.setLayout(i_gbag);
	

	TitledBorder title = BorderFactory.createTitledBorder(" from maxdView to program ");
	title.setTitleColor(title_colour);
	//title2.setTitleJustification(TitledBorder.CENTER);
	i_panel.setBorder(title);
	
	if(has_input)
	{
	    int i_line = 0;
	    
	    JLabel meas_label = new JLabel("Measurement ");
	    
	    if(ep.i_meas != InputNoMeasurement)
	    {
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0;
		c.gridy = i_line;
		//c.weightx = 1.0;
		//c.weighty = 1.0;
		i_gbag.setConstraints(meas_label, c);
		
		i_panel.add(meas_label);
	    }
	    
	    ExprData edata = mview.getExprData();
	    
	    if(ep.i_meas == InputOneMeasurement) // one measurement only
	    {
		meas_choice_jcb = new JComboBox();
		for(int m=0; m < edata.getNumMeasurements(); m++)
		{
		    meas_choice_jcb.addItem(edata.getMeasurementName(m));
		}
		
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = i_line;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		i_gbag.setConstraints(meas_choice_jcb, c);
		
		i_panel.add(meas_choice_jcb);
		
	    }
	    
	    if(ep.i_meas == InputMeasurementsOfOneType) // any set of measurements
	    {
		meas_label.setText("Measurements ");
		
		String[] opts = { "Absolute Expression Data", "Ratio Expression Data", "Probability Data", "Error Data" };
		meas_type_choice_jcb = new JComboBox(opts);
		
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = i_line;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		i_gbag.setConstraints(meas_type_choice_jcb, c);
		
		i_panel.add(meas_type_choice_jcb);
	    }
	    
	    if(ep.i_meas == InputAnySetOfMeasurements) // any set of measurements
	    {
		meas_label.setText("Measurements  ");
		
		JPanel meas_sel_panel = new JPanel();
		GridBagLayout ms_gridbag = new GridBagLayout();
		meas_sel_panel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
		meas_sel_panel.setLayout(ms_gridbag);
		meas_sel_jchkb = new JCheckBox[edata.getNumMeasurements()];
		
		int col = 0;
		int row = 0;
		
		final int max_rows = 15;


		for(int m=0; m < edata.getNumMeasurements(); m++)
		{
		    meas_sel_jchkb[m] = new JCheckBox(edata.getMeasurementName(m));
		    GridBagConstraints c = new GridBagConstraints();
		    c.anchor = GridBagConstraints.WEST;
		    c.gridx = col;
		    c.gridy = row;
		    c.weightx = 1.0; 
		    c.weighty = 1.0;
		    ms_gridbag.setConstraints(meas_sel_jchkb[m], c);
		    meas_sel_panel.add(meas_sel_jchkb[m]);

		    if(++row == max_rows)
		    {
			col++;
			row = 0;
		    }
		    
		}
		
		col++;

		int m = edata.getNumMeasurements();
		if(m > 3)
		{
		    Font small_font = null;

		    {
			JButton jb = new JButton("All");
			
			Font f = jb.getFont();
			small_font = new Font(f.getName(), Font.PLAIN, f.getSize() - 2);
			jb.setFont(small_font);

			jb.addActionListener(new ActionListener() 
			{
			    public void actionPerformed(ActionEvent e) 
			    {
				for(int tm=0; tm < mview.getExprData().getNumMeasurements(); tm++)
				    meas_sel_jchkb[tm].setSelected(true);
			    }
			    });
			
			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.EAST;
			c.gridx = col;
			c.gridy = 0;
			ms_gridbag.setConstraints(jb, c);
			meas_sel_panel.add(jb);		
		    }
		    {
			JButton jb = new JButton("None");
			
			jb.setFont(small_font);

			jb.addActionListener(new ActionListener() 
			    {
				public void actionPerformed(ActionEvent e) 
				{
				    for(int tm=0; tm < mview.getExprData().getNumMeasurements(); tm++)
					meas_sel_jchkb[tm].setSelected(false);
				}
			    });
			
			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.EAST;
			c.gridx = col;
			c.gridy = 1;
			ms_gridbag.setConstraints(jb, c);
			meas_sel_panel.add(jb);		
		    }
		}
		
		JScrollPane jsp = new JScrollPane(meas_sel_panel);
		jsp.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
		//jsp.setLayout(new BorderLayout());
		//jsp.add(meas_sel_panel, BorderLayout.CENTER);
		
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 1;
		c.gridy = i_line;
		c.weightx = 5.0;
		c.weighty = 5.0;
		i_gbag.setConstraints(jsp, c);
		
		i_panel.add(jsp);
	    }
	    
	    if(ep.i_meas != InputNoMeasurement)
		i_line++;
	    
	    if(ep.i_name_mode != InputNoNames) // names optional or required?
	    {
		{
		    JLabel label = new JLabel("Names ");
		    
		    GridBagConstraints c = new GridBagConstraints();
		    c.anchor = GridBagConstraints.EAST;
		    c.gridx = 0;
		    c.gridy = i_line;
		    //c.weightx = 1.0;
		    //c.weighty = 1.0;
		    i_gbag.setConstraints(label, c);
		    
		    i_panel.add(label);
		}
		{
		    String[] opts = null;
		    
		    if(ep.i_name_mode == InputNamesOptional) 
			opts = new String[] { "Probe names", "Gene names", "Spot names", "No names" };
		    else
			opts= new String[] { "Probe names", "Gene names", "Spot names" };
		    
		    name_choice_jcb = new JComboBox(opts);
		    
		    name_choice_jcb.setSelectedIndex(2);
		    
		    GridBagConstraints c = new GridBagConstraints();
		    c.anchor = GridBagConstraints.WEST;
		    c.fill = GridBagConstraints.HORIZONTAL;
		    c.gridx = 1;
		    c.gridy = i_line;
		    c.weightx = 1.0;
		    //c.weighty = 1.0;
		    i_gbag.setConstraints(name_choice_jcb, c);
		    
		    i_panel.add(name_choice_jcb);
		}
		i_line++;
	    }

	    {
		{
		    JLabel label = new JLabel("Spots ");
		    
		    GridBagConstraints c = new GridBagConstraints();
		    c.anchor = GridBagConstraints.EAST;
		    c.gridx = 0;
		    c.gridy = i_line;
		    //c.weightx = 1.0;
		    //c.weighty = 1.0;
		    i_gbag.setConstraints(label, c);
		    
		    i_panel.add(label);
		}
		
		{
		    apply_filter_jchkb = new JCheckBox("Apply filter");

		    GridBagConstraints c = new GridBagConstraints();
		    c.gridx = 1;
		    c.gridy = i_line++;
		    c.weightx = 1.0;
		    //c.weighty = 1.0;
		    c.anchor = GridBagConstraints.WEST;
		    i_gbag.setConstraints(apply_filter_jchkb, c);

		    i_panel.add(apply_filter_jchkb);
		}
	    }
	}
	else
	{
	    JLabel label = new JLabel("no input data");
	    i_panel.add(label);
	}

	{
	    GridBagConstraints c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.BOTH; //HORIZONTAL;
	    c.gridx = 0;
	    c.gridwidth = 2;
	    c.gridy = line;
	    c.weightx = 1.0;
	    c.weighty = 6.0;
	    gridbag.setConstraints(i_panel, c);
	    panel.add(i_panel);
	    
	    line++;
	}

	// ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 
	// ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 
	// output data (i.e. the data received from the external program)
	// ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 
	// ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 

	// make a dummy entry to give a blank line in the gridbag
	{
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = line++;
	    c.weightx = 1.0;
	    //c.weighty = 1.0;
	    gridbag.setConstraints(dummy, c);
	    
	    panel.add(dummy);
	}
	
	JPanel o_panel = new JPanel();
	GridBagLayout o_gbag = new GridBagLayout();
	o_panel.setLayout(o_gbag);
	
	title = BorderFactory.createTitledBorder(" from program to maxdView ");
	title.setTitleColor(title_colour);
	o_panel.setBorder(title);
	
	
	if(ep.o_meas != OutputNoMeasurement)
	{
	    int o_line = 0;

	    {
		JLabel label = new JLabel("Prefix ");
		
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.EAST;
		c.gridx = 0;
		c.gridy = o_line;
		//c.weightx = 1.0;
		//c.weighty = 1.0;
		o_gbag.setConstraints(label, c);
		
		o_panel.add(label);
	    }
	    {
		o_new_name_prefix_jtf = new JTextField(20);
		
		if(ep != null)
		    o_new_name_prefix_jtf.setText(ep.name);

		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = o_line;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		o_gbag.setConstraints(o_new_name_prefix_jtf, c);
		
		o_panel.add(o_new_name_prefix_jtf);
	    }
	    o_line++;
	}
	else
	{
	    JLabel label = new JLabel("no data output (just text)");
	    o_panel.add(label);
	}

	{
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.BOTH; //HORIZONTAL;
		c.gridx = 0;
		c.gridwidth = 2;
		c.gridy = line;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		gridbag.setConstraints(o_panel, c);
		panel.add(o_panel);
		
		line++;
	}
	
	// ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 
	// ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 
	// temporary files
	// ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 
	// ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 

	if((ep.i_mode == InputTempFile) || (ep.o_mode == OutputTempFile))
	{
	    // make a dummy entry to give a blank line in the gridbag
	    dummy.setMinimumSize(new Dimension(20,20));
	    dummy.setPreferredSize(new Dimension(20,20));
	    {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = line++;
		c.weightx = 1.0;
		//c.weighty = 1.0;
		gridbag.setConstraints(dummy, c);
		
		panel.add(dummy);
	    }
	    
	    JPanel io_panel = new JPanel();
	    GridBagLayout io_gbag = new GridBagLayout();
	    io_panel.setLayout(io_gbag);
	    
	    title = BorderFactory.createTitledBorder(" Temporary files ");
	    title.setTitleColor(title_colour);
	    //title2.setTitleJustification(TitledBorder.CENTER);
	    io_panel.setBorder(title);
	    
	    if(ep.i_mode == InputTempFile)
	    {
		JLabel label = new JLabel("Input ");
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0.1;
		//c.weighty = 1.0;
		c.anchor = GridBagConstraints.EAST;
		io_gbag.setConstraints(label, c);
		io_panel.add(label);

		i_tmp_name = generateTempFileName();

		i_tmp_name_jtf = new JTextField(20);
		i_tmp_name_jtf.setText(i_tmp_name);

		c = new GridBagConstraints();
		c.gridx = 1;
		//c.gridy = 0;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		//c.weighty = 1.0;
		io_gbag.setConstraints(i_tmp_name_jtf, c);
		io_panel.add(i_tmp_name_jtf);
	    }
	    if(ep.o_mode == OutputTempFile)
	    {
		JLabel label = new JLabel("Output ");
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 0.1;
		//c.weighty = 1.0;
		c.anchor = GridBagConstraints.EAST;
		io_gbag.setConstraints(label, c);
		io_panel.add(label);

		o_tmp_name = generateTempFileName();

		o_tmp_name_jtf = new JTextField(20);
		o_tmp_name_jtf.setText(o_tmp_name);
	       		
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 1;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		//c.weighty = 1.0;
		io_gbag.setConstraints(o_tmp_name_jtf, c);
		io_panel.add(o_tmp_name_jtf);
	    }
	
	    {
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.BOTH; //HORIZONTAL;
		c.gridx = 0;
		c.gridwidth = 2;
		c.gridy = line;
		c.weightx = 1.0;
		c.weighty = 3.0;
		gridbag.setConstraints(io_panel, c);
		panel.add(io_panel);
	    }
	}

	// ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 
	// ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 
	// cancel, help, run buttons
	// ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 
	// ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- ---- 

	{
	    JPanel buttons_panel = new JPanel();
	    buttons_panel.setBorder(BorderFactory.createEmptyBorder(10,10,0,10));
	    GridBagLayout inner_gridbag = new GridBagLayout();
	    buttons_panel.setLayout(inner_gridbag);

	    final JButton cancel_jb = new JButton("Cancel");
	    {
		buttons_panel.add(cancel_jb);
		
		cancel_jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    run_frame.setVisible(false);
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		//c.weighty = c.weightx = 1.0;
		gridbag.setConstraints(cancel_jb, c);
	    }
	    
	    final JButton help_jb = new JButton("Help");
	    {
		buttons_panel.add(help_jb);
		
		help_jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		//c.weighty = c.weightx = 1.0;
		gridbag.setConstraints(help_jb, c);
	    }
	    {   
		final JButton jb = new JButton("Run");
		buttons_panel.add(jb);
		
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    if(process == null)
			    {
				actuallyRunProgram(ep, run_frame);
				jb.setText("Stop");
				help_jb.setVisible(false);
				cancel_jb.setVisible(false);
			    }
			    else
			    {
				System.out.println("destroying process");
				process.destroy();
				mview.infoMessage("External program terminated");
			    }
			    //frame.setVisible(false);
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 0;
		//c.weighty = c.weightx = 1.0;
		gridbag.setConstraints(jb, c);
	    }

	    panel.add(buttons_panel);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line + 1;
	    c.gridwidth = 2;
	    gridbag.setConstraints(buttons_panel, c);
	}

	//Point win_pt = main_fram.getLocationOnScreen();
	//frame.setLocation(win_pt.x + pt.x, pt.y + pt.y);

	run_frame.pack();

	mview.locateWindowAtCenter(run_frame);

	run_frame.setVisible(true);

    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  options dialog panel
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private void showOptions()
    {
	final JFrame frame = new JFrame("Run External: Options");

	mview.decorateFrame( frame );

	JPanel panel = new JPanel();
	panel.setMinimumSize(new Dimension(350,200));
	panel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

	frame.getContentPane().add(panel, BorderLayout.CENTER);

	GridBagLayout gridbag = new GridBagLayout();
	panel.setLayout(gridbag);
	
	int line = 0;

	JLabel label = new JLabel("Temporary directory:");
	{
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    //c.weighty = c.weightx = 1.0;
	    gridbag.setConstraints(label, c);
	    panel.add(label);
	}
	line++;
	
	final JTextField temp_dir_jtf = new JTextField(20);
	{
	    temp_dir_jtf.setText(temp_path_str);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line;
	    c.weighty = c.weightx = 1.0;
	    gridbag.setConstraints(temp_dir_jtf, c);
	    panel.add(temp_dir_jtf);
	}
	line++;

	final JCheckBox unlink_files_jcb = new JCheckBox("Remove temporary files after use");
	{
	     unlink_files_jcb.setSelected(unlink_temp_files);
	     GridBagConstraints c = new GridBagConstraints();
	     c.gridx = 0;
	     c.gridy = line;
	     c.weighty = c.weightx = 1.0;
	     gridbag.setConstraints(unlink_files_jcb, c);
	     panel.add(unlink_files_jcb);
	}
	line++;

	{
	    JPanel buttons_panel = new JPanel();
	    buttons_panel.setBorder(BorderFactory.createEmptyBorder(10,10,0,10));
	    GridBagLayout inner_gridbag = new GridBagLayout();
	    buttons_panel.setLayout(inner_gridbag);

	    {
		final JButton jb = new JButton("Cancel");
		buttons_panel.add(jb);
		
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    frame.setVisible(false);
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		gridbag.setConstraints(jb, c);
	    }
	    {
		final JButton jb = new JButton("Help");
		buttons_panel.add(jb);
		
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		gridbag.setConstraints(jb, c);
	    }
	    {   
		final JButton jb = new JButton("OK");
		buttons_panel.add(jb);
		
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    // apply the options
			    temp_path_str = temp_dir_jtf.getText();
			    unlink_temp_files = unlink_files_jcb.isSelected();

			    frame.setVisible(false);
			}
		    });
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 0;
		c.weighty = c.weightx = 1.0;
		gridbag.setConstraints(jb, c);
	    }

	    panel.add(buttons_panel);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line + 1;
	    //c.gridwidth = 2;
	    gridbag.setConstraints(buttons_panel, c);
	}

	frame.pack();
	frame.setVisible(true);

    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  talk to the outside world
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    protected void deleteTempFiles(ExternalProg ep)
    {
	if(ep.i_mode == InputTempFile)
	{
	    File ifile = new File(i_tmp_name);
	    if(ifile.exists())
		if(ifile.delete() == false)
		{
		    ifile.deleteOnExit();
		    //mview.alertMessage("Unable to delete temporary input file " + i_tmp_name);
		}
	}
	if(ep.o_mode == OutputTempFile)
	{
	    File ofile = new File(o_tmp_name);
	    if(ofile.exists())
		if(ofile.delete() == false)
		{
		    ofile.deleteOnExit();
		    //mview.alertMessage("Unable to delete temporary output file " + o_tmp_name);
		}
	}
    }

    protected String generateTempFileName()
    {
	//String temp_root = System.getProperty("file.separator") + "tmp" + System.getProperty("file.separator");
	String temp_root = temp_path_str + System.getProperty("file.separator");

	String unique_suf = "";

	for(int c=0; c < 6; c++)
	{
	    unique_suf += (char) ('A' + (int)(Math.abs((Math.random() * 25))));
	}

	// does this file exist already?

	String cand_name = temp_root +  "maxd_" + unique_suf;

	File f = new File(cand_name);
	if(f.exists())
	    // damn! try again...
	    return generateTempFileName();
	else
	    return cand_name;
	    
    }

    // replace %IN or %OUT in the params string with the
    //   names of the temporary files
    //
    // replace %ROWS and %COLS with the size of output data
    //
    protected String parseParams(String params)
    {
	System.out.println("parseParams(): start is " + params);
	    
	String pp = params;
	String tmp = params;
	while(tmp != null)
	{
	    tmp = replaceFirstVariable(tmp);
	    if(tmp != null)
		pp = tmp;
	    //System.out.println("tmp is " + tmp);
	}
	System.out.println("parseParams(): end is " + pp);
	return pp;
    }

    protected String replaceFirstVariable(String str)
    {
	//System.out.println("replaceFirstVariable(): " + str);

	String res = "";
	int p = str.indexOf('%');
	if(p >= 0)
	{
	    //System.out.println("        % found at char pos " + p);

	    if(str.substring(p,p+3).equals("%IN"))
	    {
		if(p > 0)
		    res = str.substring(0,p);
		res += i_tmp_name;
		res += str.substring(p+3);
		return res;
	    }
	    if(str.substring(p,p+4).equals("%OUT"))
	    {
		if(p > 0)
		    res = str.substring(0,p);
		res += o_tmp_name;
		res += str.substring(p+4);
		return res;
	    }
	    if(str.substring(p,p+5).equals("%ROWS"))
	    {
		if(p > 0)
		    res = str.substring(0,p);
		res += rows_in_output;
		res += str.substring(p+5);
		return res;
	    }
	    if(str.substring(p,p+5).equals("%COLS"))
	    {
		if(p > 0)
		    res = str.substring(0,p);
		res += cols_in_output;
		res += str.substring(p+5);
		return res;
	    }
	}
	return null;
    }

    protected void writeInputData(BufferedWriter bw, ExternalProg ep)
    {
	int nm =  mview.getExprData().getNumMeasurements();
	
	// build a vector of booleans showin which measurements to put in the 
	// input for the external program
	//
	
	boolean out_vec[] = new boolean[nm];
	for(int ov=0;ov<nm;ov++)
	    out_vec[ov] = false;

	for(int ov=0;ov<nm;ov++)
	{
	    switch(ep.i_meas)
	    {
	    case InputOneMeasurement:
		if(ov == meas_choice_jcb.getSelectedIndex())
		    out_vec[ov] = true;
		break;
	    case InputMeasurementsOfOneType:
		if( mview.getExprData().getMeasurementDataType(ov) == meas_type_choice_jcb.getSelectedIndex() )
		   out_vec[ov] = true; 
		break;
	    case InputAnySetOfMeasurements:
		if(meas_sel_jchkb[ov].isSelected())
		    out_vec[ov] = true;
		break;
	    }
	}

	// do the actual outputting
	//
	if((ep.i_meas != InputNoMeasurement) || (ep.i_name_mode != InputNoNames))
	    writeMeasurementsAndNamesToStream(bw, ep, out_vec);
    }


    // Spot names might be purely numeric, as so they must be
    // marked in some way so that they are not interpreted as
    // numbers
    // 
    // enclosing them in ''s 
    //
    private String quoteIfNumber(String str)
    {
	try
	{
	    double dummy = NumberParser.tokenToDouble(str);
	    return "'" + str + "'";
	}
	catch(TokenIsNotNumber tinn)
	{
	    return str;
	}
    }

    protected void writeMeasurementsAndNamesToStream(BufferedWriter bw, ExternalProg ep, boolean[] sel)
    {
	int nm = mview.getExprData().getNumMeasurements();
	int ns = mview.getExprData().getNumSpots();
	ExprData edata = mview.getExprData();

	char delim = '\t';
	switch(ep.i_delim)
	{
	case SpaceDelimited:
	    delim = ' ';
	    break;
	case CommaDelimited:
	    delim = ',';
	    break;
	}

	try
	{
	    for(int s=0; s < ns; s++)
	    {
		if((apply_filter_jchkb.isSelected() == false)  || (!edata.filter(s)))
		{
		    //if((only_clusters_jchkb.isSelected() == false) || (edata.inVisibleClusters(s) > 0))
		    {
			boolean first_on_line = true;
			// any names to be output?
			//
			if(ep.i_name_mode != InputNoNames)
			{
			    String name = null;
			    switch(name_choice_jcb.getSelectedIndex())
			    {
			    case 0: // probe name
				name = edata.getProbeName(s);
				break;
			    case 1: // gene names
				name = edata.getGeneName(s);  // composite name
				break;
			    case 2: // spot name
				name =  edata.getSpotName(s);
				break;
			    }
			    
			    if(name != null)
			    {
				bw.write(quoteIfNumber(name));
				
				bw.write(delim);
				first_on_line = false;
			    }
			}
			
			for(int m=0; m < nm; m++)
			{
			    if(sel[m])
			    {
				if(first_on_line)
				{
				    first_on_line = false;
				}
				else
				{
				    bw.write(delim);
				}
				bw.write(String.valueOf(edata.eValue(m, s)));
				
				if(debug_input)
				    if(s == 0)
					System.out.println(edata.getMeasurementName(m) + ", ");
			    }
			}
			
			bw.write(System.getProperty("line.separator"));
		    }
		}
	    }  // end for(int s...

	    //System.out.println("measurement " + mview.getExprData().getMeasurementName(m_id) + " written to Stdin");
	}
	catch(java.io.IOException ioe)
	{
	    mview.errorMessage("Cannot write to input stream\n  " + ioe);
	}
    }

    // extracts the name from the input string containing (name, val1, val2, ..., valN)
    //
    private String findSymbolicName(String s, char delim)
    {
	StringReader sr = new StringReader(s);
	StreamTokenizer stok = new StreamTokenizer(sr);

	stok.resetSyntax();
	stok.eolIsSignificant(true);
	stok.wordChars('!','~');        // this covers all printable ASCII chars
	stok.whitespaceChars(delim, delim);

	//System.out.println("seeking word on line '" + s + "'");

	boolean done = false;

	String result = null;

	try
	{
	    while(!done)
	    {
		int token = stok.nextToken();
		
		switch(token)
		{ 
		case java.io.StreamTokenizer.TT_EOF:
		    done = true;
		    break;
		case java.io.StreamTokenizer.TT_EOL:
		    done = true;
		    break;
		case java.io.StreamTokenizer.TT_WORD:
		    
		    // is the word actually a number enclosed in ''s ?
		    // (i.e. a Spot Name consisting soley of numbers which
		    //  has been quoted in the input file)
		    //
		    if((stok.sval.charAt(0) == '\'') && (stok.sval.charAt(stok.sval.length()-1) == '\''))
		    {
			
			String buried_str = stok.sval.substring(1, stok.sval.length()-1);

			//System.out.println("quoted word on line is " + buried_str);

			return buried_str;

			/*
			try
			{
			    double quoted_number = NumberParser.tokenToDouble(buried_str);
			    
			    result = String.valueOf(quoted_number);
			}
			catch(TokenIsNotNumber tinn)
			{
			    // found a symbolic name buried within the ''s
			    result = buried_str;
			    
			    //System.out.println("word on line is " + result);
			    return result;
			}
			*/
		    }
		    else
		    {
			// make sure it's not a number...
			try
			{
			    double dummy = NumberParser.tokenToDouble(stok.sval);
			}
			catch(TokenIsNotNumber tinn)
			{
			    // found a symbolic name
			    result = stok.sval;
			    
			    //System.out.println("word on line is " + result);
			    return result;
			}
		    }
		}
	    }
	}
	catch(IOException ioe)
	{
	    System.out.println("dubious line: " + s);
	}

	return result;
    }

    // extract an array of numbers from the input string containing (name, val1, val2, ..., valN)
    //
    private double[] stringToNumberArray(String s, char delim)
    {
	StringReader sr = new StringReader(s);
	StreamTokenizer stok = new StreamTokenizer(sr);

	// :: TODO ::
	// unfortunately, stream tokenisers don't appear to
	// recognise numbers with exponentials in them....
	//
	// we'll have to do it ourselves (using the ExprData convenience routines)
	//

	stok.resetSyntax();
	stok.eolIsSignificant(true);
	stok.wordChars('!','~');        // this covers all printable ASCII chars

	stok.whitespaceChars(delim, delim);

	boolean done = false;
	int nums = 0;

	Vector nums_v = new Vector();

	try
	{
	    while(!done)
	    {
		int token = stok.nextToken();
		
		switch(token)
		{ 
		case java.io.StreamTokenizer.TT_EOF:
		    done = true;
		    break;
		case java.io.StreamTokenizer.TT_EOL:
		    done = true;
		    break;
		case java.io.StreamTokenizer.TT_WORD:
		    try
		    {
			double d = NumberParser.tokenToDouble(stok.sval);
			nums++;
			nums_v.addElement(stok.sval);
		    }
		    catch(TokenIsNotNumber tinn)
		    {
			//System.out.println("ignored word " +  stok.sval + " on line");
		    }

		    //System.out.println(stok.sval);
		    break;
		}
	    }
	}
	catch(IOException ioe)
	{
	    System.out.println("dubious line: " + s);
	}

	double[] da = new double[nums];
	
	for(int n=0; n< nums; n++)
	{
	     try
	     {
		 da[n] = NumberParser.tokenToDouble((String) nums_v.elementAt(n));
	     }
	     catch(TokenIsNotNumber tinn)
	     {
		 da[n] = Double.NaN;
	     }
	}

	//System.out.println(words + " words, " + nums + " numbers on this line");

	return da;

    }

    protected void parseOutputData(BufferedReader br, ExternalProg ep)
    {
	try
	{
	    Vector input_strs = new Vector();
	    String str = br.readLine();
	    while(str != null)
	    {
		input_strs.addElement(str);
		str = br.readLine();
	    }

	    if(debug_output)
	    {
		System.out.println("   o_mode is " +ep.o_mode);
		System.out.println("   o_meas is " +ep.o_meas);

		System.out.println(input_strs.size() + " lines read from stdout");
	    }

	    // are there any names in the strings, are are they
	    // just numbers?
	    //

	    
	    switch(ep.o_meas)
	    {
	    case OutputListOfNames:
		if(debug_output)
		    System.out.println("parsing for a list of names (with no values)");
		{
		    String[] name = new String[input_strs.size()];
		    
		    char delim = '\t';
		    switch(ep.o_delim)
		    {
		    case SpaceDelimited:
			delim = ' ';
			break;
		    case CommaDelimited:
			delim = ',';
			break;
		    }

		    int good_names = 0;
		    String[] name_v = new String[input_strs.size()];

		    for(int s=0; s < input_strs.size(); s++)
		    {
			String this_line = (String)input_strs.elementAt(s);
			name_v[s] = findSymbolicName(this_line, delim);
			
			if(name_v[s] != null)
			    good_names++;
		    }

		    System.out.println(good_names + " names found in output");

		    // get the row ID's for these spots, and create cluster
		    int illegal_names = 0;

		    ExprData edata = mview.getExprData();

		    

		    Vector elems = new Vector();
		    for(int s=0; s < input_strs.size(); s++)
		    {
			elems.addElement(name_v[s]);
		    }

		    mview.addMessageToLog(elems.size() + " elements found");
		    if(elems.size() > 0)
		    {
			String new_name = "Cluster";

			if(o_new_name_prefix_jtf != null)
			    new_name = o_new_name_prefix_jtf.getText();

			int name_mode =  ExprData.SpotIndex;
			switch(name_choice_jcb.getSelectedIndex())
			{
			case 0: // probe name
			    name_mode = ExprData.ProbeName;
			    break;
			case 1: // gene names
			    name_mode = ExprData.GeneName;
			    break;
			case 2: // spot name
			    name_mode = ExprData.SpotName;
			    break;
			}

			edata.addCluster(o_new_name_prefix_jtf.getText(), name_mode, elems);
			mview.informationMessage("Cluster with " + elems.size() + " elements created");
		    }
		    if(illegal_names > 0)
		    {
			mview.errorMessage(illegal_names + " names were not recognised");
		    }
		}
		break;

	    case OutputOneMeasurement:
	    case OutputOneOrMoreMeasurements:
		if(debug_output)
		    System.out.println("parsing for a one or more measurements");
		{
		    // convert each of the strings into an array of words
		    double[][] data = new double[input_strs.size()][];
		    String[] name = new String[input_strs.size()];

		    int max_len = 0;
		    
		    char delim = '\t';
		    switch(ep.o_delim)
		    {
		    case SpaceDelimited:
			delim = ' ';
			break;
		    case CommaDelimited:
			delim = ',';
			break;
		    }

		    if(debug_output)
			System.out.println("delimiter is '"  + delim + "'");

		    int good_names = 0;

		    for(int s=0; s < input_strs.size(); s++)
		    {
			String this_line = (String)input_strs.elementAt(s);
			data[s] = stringToNumberArray(this_line, delim);

			name[s] = findSymbolicName(this_line, delim);

			if(name[s] != null)
			    good_names++;

			if(data[s].length > max_len)
			    max_len = data[s].length;
		    }

		    System.out.println(good_names + " names found in output");

		    // now insert each of the new measurements
		    //
		    for(int m=0; m < max_len; m++)
		    {
			// reconstuct this column by extracting the m'th element from each of the rows
			double[] m_data = new double[input_strs.size()];
			try
			{
			    for(int s=0; s < input_strs.size(); s++)
			    {
				m_data[s] = data[s][m];
			    }
			    
			    ExprData.Measurement new_m = mview.getExprData().new Measurement();

			    if(max_len > 1)
				new_m.setName(o_new_name_prefix_jtf.getText() + m);
			    else
				new_m.setName(o_new_name_prefix_jtf.getText());

			    new_m.setShow(true);
			    new_m.setDataType(ExprData.ExpressionAbsoluteDataType);
			    new_m.setData(m_data);

			    if(good_names > 0)
			    {
				ExprData.DataTags dtags = new_m.getDataTags();
			    
				switch(name_choice_jcb.getSelectedIndex())
				{
				case 0:  // probe names
				    dtags.probe_name = name;
				    break;
				case 1:  // gene names
				//new_m.dtags.gene_names = name;
				    mview.errorMessage("Gene name matching not done yet");
				    return;
				case 2:  // spot names
				    dtags.spot_name = name;
				    break;
				}

				new_m.setDataTags(dtags);
			    }
			    
			    mview.getExprData().addMeasurement(new_m);
			}
			catch(ArrayIndexOutOfBoundsException aioobe)
			{
			    mview.errorMessage("Short line in output data (parsing column " + m  +")");
			    return;
			}
		    }

		}

		break;
	    case OutputNoMeasurement:
		if(debug_output)
		    System.out.println("shouldn't get here!");
		break;
			

	    }
	}
	catch(IOException ioe)
	{
	    System.out.println("problem in stdout\n  " + ioe);
	}
    }
    
    protected void actuallyRunProgram(final ExternalProg ep, final JFrame frame)
    {
	if(ep == null)
	{
	    System.out.println("no program!");
	    return;
	}
	else
	{
	    ExtProgRunnerThread eprt = new ExtProgRunnerThread(ep, frame);
	    eprt.start();
	}
    }
    
    class ExtProgRunnerThread extends Thread
    {
	private ExternalProg ep;
	private JFrame frame;

	public ExtProgRunnerThread(final ExternalProg ep_, final JFrame frame_)
	{
	    ep = ep_; 
	    frame = frame_;
	}

	public void run()
	{
	    try
	    {
		Runtime rt = Runtime.getRuntime();
		
		String exec_cmd = ep.filename;
		BufferedReader br  = null;
		BufferedWriter bw = null;
		
		if((ep.i_mode  == InputTempFile) || (ep.o_mode  == OutputTempFile))
		{
		    // work out size of output data befor substituting variables
		    
		    rows_in_output = mview.getExprData().getNumSpots();
		    switch(ep.i_meas)
		    {
		    case InputNoMeasurement:
			cols_in_output = 0;
			break;
		    case InputOneMeasurement:
			cols_in_output = 1;
			break;
		    case InputAnySetOfMeasurements:
			cols_in_output = 0;
			for(int s=0;s < meas_sel_jchkb.length; s++)
			    if(meas_sel_jchkb[s].isSelected())
				cols_in_output++;
			break;
		    }
		}
		if(ep.i_mode  == InputTempFile)
		{
		    if(debug_input)
			System.out.println("writing data to " + i_tmp_name);
		    
		    // write the input file
		    FileWriter fw = new FileWriter(new File(i_tmp_name));
		    bw = new BufferedWriter(fw);
		    writeInputData(bw, ep);
		    bw.flush();
		    fw.close();
		    
		    if(debug_input)
			System.out.println("  ... written ok");
		}
		
		// replace %IN, %OUT, %ROWS and %COLS in the params string with the
		// names of the temporary files and the size of the data
		//
		String parsed_params = parseParams(ep.params);
		exec_cmd += "  " + parsed_params;
		
		if(debug_input)
		    System.out.println("temp filename variables substituted");
		
		// run the command...
		//
		
		if(debug_exec)
		    System.out.println("exec:\n-----\n" + exec_cmd  +"\n-----");
		
		process = rt.exec(exec_cmd);
		
		if(ep.i_mode  == InputStdin)
		{
		    if(debug_exec)
			System.out.println("connecting to input pipe of program");
		    
		    // connect to the pipe
		    //
		    OutputStream os  = process.getOutputStream();
		    bw = new BufferedWriter(new OutputStreamWriter(os));
		    
		    if(debug_exec)
			System.out.println("writing input data to pipe");
		    
		    writeInputData(bw, ep); // measurement(s) and names (if needed)
		    
		    if(debug_exec)
			System.out.println("  ... written ok");
		    
		    bw.flush();
		    os.close();
		}
		
		
		if(ep.o_mode  == OutputStdout)
		{
		    // grab the output as the program runs
		    //
		    InputStream is = process.getInputStream();
		    br = new BufferedReader(new InputStreamReader(is));
		    
		    if(debug_exec)
			System.out.println("grabbed program's output pipe");
		}
		
		if(debug_exec)
		    System.out.println("waiting for program to complete");
		
		// wait for the external program to complete...
		//
		try
		{
		    process.waitFor();
		}
		catch (InterruptedException ie)
		{
		}
		
		frame.setVisible(false);
		
		if(debug_exec)
		    System.out.println("script has finished");
		
		// if the script terminated abnormally...
		
		if(process.exitValue() != 0)
		{
		    if(debug_exec)
			System.out.println("script exited abnormally");
		    
		    // collate the error messages into a String
		    //
		    StringWriter sw = new StringWriter();
		    
		    // record the error stream
		    InputStream errors = process.getErrorStream();
		    
		    if(debug_exec)
			System.out.println("collating error stream");
		    
		    try
		    {
			int ch = 0;
			while(ch >= 0)
			{
			    sw.write(ch);
			    ch = errors.read();
			}
		    }
		    catch(IOException ioe)
		    {
			
		    }
		    
		    // bring up an alert box with the error message in it...
		    mview.errorMessage("Error Output:\n\n" + sw.toString());
		}
		
		process = null;

		try
		{
		    // has the output file appeared?
		    //
		    if(ep.o_mode == OutputTempFile)
		    {
			if(debug_exec)
			    System.out.println("looking for program's output file");
			
			try
			{
			    br = new BufferedReader(new FileReader(new File(o_tmp_name)));
			}
			catch(IOException ioe)
			{
			    mview.errorMessage("Cannot find output file\n  " + ioe);
			    
			    deleteTempFiles(ep);
			    
			    return;
			}
		    }
		    
		    if(ep.o_meas == OutputNoMeasurement)
		    {
			if(debug_exec)
			    System.out.println("collating text output...");
			
			StringWriter sw = new StringWriter();
			
			try
			{
			    int ch = 1;
			    while(ch >= 0)
			    {
				ch = br.read();
				if(ch >= 0)
				    sw.write(ch);
			    }
			}
			catch(IOException ioe)
			{
			    
			}
			mview.informationMessage("Output:\n\n" + sw.toString() + "\n\n");
		    }
		    else
		    {
			if(debug_exec)
			    System.out.println("parsing results from output...");
			
			// parse the results
			//
			parseOutputData(br, ep);
			
			//readOutputFromStream(br, ep);
		    }
		    
		    deleteTempFiles(ep);
		}
		catch(NullPointerException npe)
		{
		    mview.errorMessage("Couldn't read output\n  " + npe);
		    process = null;
		    return;
		}
		
		//  int retval = p.exitValue();
		process = null;

		if(debug_exec)
		    System.out.println("external execution completed");
		
		return;
	    }
	    catch (java.io.IOException ioe)
	    {
		mview.errorMessage("Couldn't run program\n  " + ioe);
	    }
	    catch (Exception e)
	    {
		mview.errorMessage("Couldn't run program\n  " + e);
	    }
	    catch (Error e)
	    {
		mview.errorMessage("Couldn't run program\n  " + e);
	    }
	    frame.setVisible(false);
	    process = null;
	    return;
	}
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  button handlers
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void editProgram()
    {
	//System.out.println("editing.. ");
	    
	ExternalProg ep = findByName((String)progs_list.getSelectedValue());
	
	if(ep != null)
	{
	    //System.out.println("editing " + ep.name);
	    createExtProgDescDialog(ep);
	}
    }

    public void newProgram()
    {
	createExtProgDescDialog(null);
    }
    
    public void cloneProgram()
    {
	ExternalProg ep = findByName((String)progs_list.getSelectedValue());
	
	if(ep != null)
	{
	    ExternalProg new_ep =  ep.makeClone();
	    
	    new_ep.name     = ep.name + " (copy)";

	    addExternalProg(new_ep);
	}
    }
    

    public void deleteProgram()
    {
	ExternalProg ep = findByName((String)progs_list.getSelectedValue());
	
	if(ep != null)
	{
	    if(mview.infoQuestion("Really delete '" + ep.name + "' ?", "No", "Yes") == 1)
	    {
		ext_progs.removeElement(ep);
		buildList();
	    }
	}
	
    }
 
    public void runProgram()
    {
	ExternalProg ep = findByName((String)progs_list.getSelectedValue());
	if(ep != null)
	    createRunDialog(ep);
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  useful identifers
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public static final int InputNoMeasurement         = 0;
    public static final int InputOneMeasurement        = 1;
    public static final int InputAnySetOfMeasurements  = 2;
    public static final int InputMeasurementsOfOneType = 3;

    public static final int InputOneOrMoreMeasurement   = 4;
    public static final int InputNoneOrMoreMeasurements = 5;

    public static final int InputNoNames       = 0;
    public static final int InputNamesOptional = 1;
    public static final int InputNamesRequired = 2;

    public static final int InputStdin       = 0;
    public static final int InputTempFile    = 1;

    public static final int OutputNoMeasurement         = 0;
    public static final int OutputOneMeasurement        = 1;
    public static final int OutputOneOrMoreMeasurements = 2;
    public static final int OutputListOfNames           = 3;

    public static final int OutputNoNames       = 0;
    public static final int OutputNamesOptional = 1;
    public static final int OutputNamesRequired = 2;

    public static final int OutputStdout      = 0;
    public static final int OutputTempFile    = 1;

    public static final int TabDelimted   = 0;
    public static final int SpaceDelimited = 1;
    public static final int CommaDelimited = 2;

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  intestines
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private String temp_path_str = "";
    private boolean unlink_temp_files = true;

    public boolean debug_exec   = true; 
    public boolean debug_input  = false;    // the input of the External program...
    public boolean debug_output = false;

    private Process process = null;

    protected String i_tmp_name = null;
    protected String o_tmp_name = null;
    protected int rows_in_output = 0;
    protected int cols_in_output = 0;

    private JFrame frame = null;

    protected JList progs_list = null;
    protected DefaultListModel progs_list_model = null;

    protected String load_path_str = null;
    protected String save_path_str = null;

    protected maxdView mview;
}


