import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.util.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.JTree;
import javax.swing.tree.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreeSelectionModel;

public class CustomMenu
{
    public final boolean old_version = false;

    Vector commands = null;       // holds PluginCommands
    Vector menu_entries = null;   // holds the names given to the commands, these names are used in menu entries
    Vector hotkeys = null;        // holds the accelerator key used for menu entries

    Vector menus = null;        // holds all Menus made by this object

    private maxdView mview;

    private Vector all_commands_v;

    public CustomMenu(maxdView mv_)
    {
	mview = mv_;
	all_commands_v = null;

	if(!readCustomMenuTree())
	    makeCustomMenuTreeFromOldFormat();

    }

    public void commandsHaveBeenUpdated()
    {
	all_commands_v = null;
    }

    private Vector getAllCommands()
    {
	if(all_commands_v == null)
	{
	    all_commands_v = mview.getAllCommands();
	    addBuiltInCommands( all_commands_v );
	}
	return all_commands_v;
    }

    public void createEditorPanel()
    {
	if(frame != null)
	{
	    frame.setVisible(true);
	    return;
	}

	frame = new JFrame("Custom Menu Editor");

	mview.decorateFrame(frame);

	frame.addWindowListener(new WindowAdapter() 
	    {
		public void windowClosing(WindowEvent e)
		{
		    writeCustomMenuTree();
		    frame = null;
		}
	    });

	edit_panel = new JPanel();

	edit_panel.setPreferredSize(new Dimension(500, 400));
	
	edit_panel.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
	
	GridBagConstraints c = null;
	GridBagLayout gridbag = new GridBagLayout();
	edit_panel.setLayout(gridbag);

	// ------------------------------------------------------------------------------- //
	// ------------------------------------------------------------------------------- //

	JPanel tree_wrap = new JPanel();
	// butwrap.setBorder(BorderFactory.createEmptyBorder(0,2,0,2));
	GridBagLayout tree_bag = new GridBagLayout();
	tree_wrap.setLayout(tree_bag);

	MouseListener mouseListener = null;

	command_tree = new JTree();
	
	// makeCustomMenuTreeFromOldFormat();
	    
	populateTreeWithCommands( command_tree );
	
	
	command_tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
	
	command_tree.addTreeSelectionListener(new TreeSelectionListener() 
	    {
		public void valueChanged(TreeSelectionEvent e) 
		{
		    treeSelectionHasChanged();
		}
	    });
	
	DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
	renderer.setLeafIcon(null);
	// renderer.setOpenIcon(null);
	// renderer.setClosedIcon(null);
	command_tree.setCellRenderer(renderer);
	// command_tree.setShowsRootHandles(true);

	JScrollPane command_jsp = new JScrollPane(command_tree);
	//command_jsp.setPreferredSize(new Dimension(100, 300));
	
	c = new GridBagConstraints();
	c.fill = GridBagConstraints.BOTH;
	c.weightx = 4.0;
	c.weighty = 5.0;
	tree_bag.setConstraints(command_jsp, c);
	tree_wrap.add(command_jsp);
	
	// --------------------
	// tree control buttons
	// --------------------

	JButton jb = null;
	int col = 0;
	JPanel butwrap = new JPanel();
	butwrap.setBorder(BorderFactory.createEmptyBorder(0,2,0,2));
	GridBagLayout butbag = new GridBagLayout();
	butwrap.setLayout(butbag);
	
	Insets ins = new Insets(0,0,0,0);
	
	jb = new JButton("New Command");
	jb.setFont(mview.getSmallFont());
	jb.setMargin(ins);
	jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    addCommand();
		}
	    });
	c = new GridBagConstraints();
	c.fill = GridBagConstraints.HORIZONTAL;
	c.weightx = 0.5;
	butbag.setConstraints(jb, c);
	butwrap.add(jb);

	jb = new JButton("New Menu");
	jb.setMargin(ins);
	jb.setFont(mview.getSmallFont());
	jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    addMenu();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 1;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.weightx = 0.5;
	butbag.setConstraints(jb, c);
	butwrap.add(jb);

	// --  --  --  --  --  --  

	c = new GridBagConstraints();
	c.gridy = 1;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.anchor = GridBagConstraints.NORTH;
	c.weightx = 4.0;
	tree_bag.setConstraints(butwrap, c);
	tree_wrap.add(butwrap);
	
	butwrap = new JPanel();
	butwrap.setBorder(BorderFactory.createEmptyBorder(0,2,0,2));
	butbag = new GridBagLayout();
	butwrap.setLayout(butbag);

	// --  --  --  --  --  --  
	
	jb = new JButton("Copy");
	jb.setMargin(ins);
	jb.setFont(mview.getSmallFont());
	jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    cutNode(false);
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 0;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.weightx = 0.333;
	butbag.setConstraints(jb, c);
	butwrap.add(jb);

	jb = new JButton("Cut");
	jb.setMargin(ins);
	jb.setFont(mview.getSmallFont());
	jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    cutNode(true);
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 1;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.weightx = 0.333;
	butbag.setConstraints(jb, c);
	butwrap.add(jb);

	jb = new JButton("Paste");
	jb.setMargin(ins);
	jb.setFont(mview.getSmallFont());
	jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    pasteNode();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 2;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.weightx = 0.333;
	butbag.setConstraints(jb, c);
	butwrap.add(jb);

	// --  --  --  --  --  --  


	c = new GridBagConstraints();
	c.gridy = 2;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.anchor = GridBagConstraints.NORTH;
	c.weightx = 4.0;
	tree_bag.setConstraints(butwrap, c);
	tree_wrap.add(butwrap);

	butwrap = new JPanel();
	butwrap.setBorder(BorderFactory.createEmptyBorder(0,2,0,2));
	butbag = new GridBagLayout();
	butwrap.setLayout(butbag);
	
	// --  --  --  --  --  --  

	jb = new JButton("Raise");
	jb.setMargin(ins);
	jb.setFont(mview.getSmallFont());
	jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    raiseNode();
		}
	    });
	c = new GridBagConstraints();
	c.fill = GridBagConstraints.HORIZONTAL;
	c.weightx = 0.33;
	butbag.setConstraints(jb, c);
	butwrap.add(jb);

	jb = new JButton("Lower");
	jb.setMargin(ins);
	jb.setFont(mview.getSmallFont());
	jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    lowerNode();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 1;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.weightx = 0.33;
	butbag.setConstraints(jb, c);
	butwrap.add(jb);

	jb = new JButton("Delete");
	jb.setMargin(ins);
	jb.setFont(mview.getSmallFont());
	jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    deleteNode();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 2;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.weightx = 0.33;
	butbag.setConstraints(jb, c);
	butwrap.add(jb);

	// --  --  --  --  --  --  

	c = new GridBagConstraints();
	c.gridy = 3;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.anchor = GridBagConstraints.NORTH;
	c.weightx = 4.0;
	tree_bag.setConstraints(butwrap, c);
	tree_wrap.add(butwrap);

	JSplitPane jspltp = new JSplitPane();

	jspltp.setLeftComponent( tree_wrap );

	/*
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 3;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.anchor = GridBagConstraints.NORTH;
	c.weightx = 4.0;
	//c.weighty = 3.0;
	//c.gridwidth = 2;
	gridbag.setConstraints(butwrap, c);
	edit_panel.add(butwrap);
	*/

	// ------------------------------------------------------------------------------- //
	// ------------------------------------------------------------------------------- //

	JPanel wrapper = new JPanel();
	wrapper.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
	GridBagLayout wrapbag = new GridBagLayout();
	wrapper.setLayout(wrapbag);

	int line = 0;

	JLabel label = new JLabel("Name ");
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.SOUTHWEST;
	wrapbag.setConstraints(label, c);
	wrapper.add(label);

	name_jtf = new JTextField(20);
	name_jtf.addMouseListener(new CustomMouseListener(-1));
	name_jtf.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    updateCurrentCommandName();
		}
	    });
			    
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.weightx = c.weighty = 1.0;
	wrapbag.setConstraints(name_jtf, c);
	wrapper.add(name_jtf);

	show_in_menu_jchkb = new JCheckBox("Show in menu ?");
	show_in_menu_jchkb.setFont(mview.getSmallFont());
	show_in_menu_jchkb.setForeground( label.getForeground() );
	show_in_menu_jchkb.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    updateCurrentCommandShowInMenu();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.SOUTHWEST;
	wrapbag.setConstraints(show_in_menu_jchkb, c);
	wrapper.add(show_in_menu_jchkb);

	Dimension fillsize = new Dimension(16,16);
	Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.weightx = c.weighty = 1.0;
	wrapbag.setConstraints(filler, c);
	wrapper.add(filler);
	

	label = new JLabel("Plugin ");
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.SOUTHWEST;
	wrapbag.setConstraints(label, c);
	wrapper.add(label);

	plugin_jtf = new JTextField(20);
	plugin_jtf.setEditable(false);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.weightx = c.weighty = 1.0;
	wrapbag.setConstraints(plugin_jtf, c);
	wrapper.add(plugin_jtf);

	fillsize = new Dimension(16,16);
	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.weightx = c.weighty = 1.0;
	wrapbag.setConstraints(filler, c);
	wrapper.add(filler);
	

	label = new JLabel("Command ");
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.SOUTHWEST;
	wrapbag.setConstraints(label, c);
	wrapper.add(label);

	command_jtf = new JTextField(20);
	command_jtf.setEditable(false);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.weightx = c.weighty = 1.0;
	wrapbag.setConstraints(command_jtf, c);
	wrapper.add(command_jtf);

	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.weightx = c.weighty = 1.0;
	wrapbag.setConstraints(filler, c);
	wrapper.add(filler);

	label = new JLabel("Arguments ");
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.SOUTHWEST;
	wrapbag.setConstraints(label, c);
	wrapper.add(label);

	arg_panel = new JPanel();
	arg_panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.gray),
							       BorderFactory.createEmptyBorder(5, 5, 5, 5)));
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.weightx = 1.0;
	c.weighty = 4.0;
	wrapbag.setConstraints(arg_panel, c);
	wrapper.add(arg_panel);

	filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.weightx = c.weighty = 1.0;
	wrapbag.setConstraints(filler, c);
	wrapper.add(filler);

	label = new JLabel("Hotkey ");
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.SOUTHWEST;
	wrapbag.setConstraints(label, c);
	wrapper.add(label);

	hotkey_jtf = new JTextField(20);
	hotkey_jtf.addMouseListener(new CustomMouseListener(1));
	hotkey_jtf.addActionListener(new ActionListener()
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    updateCurrentCommandHotkey();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line++;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.weightx = c.weighty = 1.0;
	wrapbag.setConstraints(hotkey_jtf, c);
	wrapper.add(hotkey_jtf);

	/*
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 0;
	c.gridheight = 4;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.anchor = GridBagConstraints.NORTHWEST;
	c.weightx = 6.0;
	c.weighty = 5.0;
	//c.gridwidth = 2;
	gridbag.setConstraints(wrapper, c);
	edit_panel.add(wrapper);
	*/

	jspltp.setRightComponent( wrapper );
	
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	c.fill = GridBagConstraints.BOTH;
	c.weightx = 9.0;
	c.weighty = 8.0;
	gridbag.setConstraints(jspltp, c);
	edit_panel.add(jspltp);
	

	// ------------------------------------------------------------------------------- //
	// ------------------------------------------------------------------------------- //

	
	butwrap = new JPanel();
	butwrap.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));

	butbag = new GridBagLayout();
	butwrap.setLayout(butbag);
	
	col = 0;


	jb = new JButton("Help");
	jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    mview.getHelpTopic("CustomMenu");
		}
	    });
	c = new GridBagConstraints();
	c.gridx = col++;
	butbag.setConstraints(jb, c);
	butwrap.add(jb);

	jb = new JButton("Close");
	jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    writeCustomMenuTree();
		    new_panel.setVisible(false);
		    frame.getContentPane().remove(new_panel);
		    frame.getContentPane().add(edit_panel);
		    edit_panel.setVisible(true);
		    frame.setVisible(false);
		    frame = null;
		}
	    });
	c = new GridBagConstraints();
	c.gridx = col++;
	butbag.setConstraints(jb, c);
	butwrap.add(jb);

	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 1;
	// c.weighty = 0.5;
	c.weightx = 9.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	gridbag.setConstraints(butwrap, c);
	edit_panel.add(butwrap);

	// ------------------------------------------------------------------------------- //
	// ------------------------------------------------------------------------------- //

	new_panel = new JPanel();

	new_panel.setPreferredSize(new Dimension(500, 350));
	  
	gridbag = new GridBagLayout();
	new_panel.setLayout(gridbag);

	// ------------------------------------------------------------------------------- //
	// ------------------------------------------------------------------------------- //

	label = new JLabel("Plugins");
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	gridbag.setConstraints(label, c);
	new_panel.add(label);

	label = new JLabel("Commands");
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 0;
	gridbag.setConstraints(label, c);
	new_panel.add(label);

	label = new JLabel("Arguments");
	c = new GridBagConstraints();
	c.gridx = 2;
	c.gridy = 0;
	gridbag.setConstraints(label, c);
	new_panel.add(label);

	new_plugin_list = new JList();
	
	mouseListener = new MouseAdapter() 
	    {
		public void mouseClicked(MouseEvent e) 
		{
		    //current_command = (PluginCommand) commands.elementAt(command_list.getSelectedIndex());
		    updateNewCommandList();
		}
	    };
	
	new_plugin_list.addMouseListener(mouseListener);
	JScrollPane new_plugin_jsp = new JScrollPane(new_plugin_list);
	    
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 1;
	c.fill = GridBagConstraints.BOTH;
	c.weightx = 2.0;
	c.weighty = 8.0;
	gridbag.setConstraints(new_plugin_jsp, c);
	new_panel.add(new_plugin_jsp);

	new_command_list = new JList();
	
	mouseListener = new MouseAdapter() 
	    {
		public void mouseClicked(MouseEvent e) 
		{
		    updateNewArgsList();
		}
	    };
	
	new_command_list.addMouseListener(mouseListener);
	JScrollPane new_command_jsp = new JScrollPane(new_command_list);
	    
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 1;
	c.fill = GridBagConstraints.BOTH;
	c.weightx = 2.0;
	c.weighty = 8.0;
	gridbag.setConstraints(new_command_jsp, c);
	new_panel.add(new_command_jsp);
	
	new_args_list = new JList();
       	JScrollPane new_args_jsp = new JScrollPane(new_args_list);

	c = new GridBagConstraints();
	c.gridx = 2;
	c.gridy = 1;
	c.fill = GridBagConstraints.BOTH;
	c.weightx = 2.0;
	c.weighty = 8.0;
	gridbag.setConstraints(new_args_jsp, c);
	new_panel.add(new_args_jsp);
		
	// ------------------------------------------------------------------------------- //
	// ------------------------------------------------------------------------------- //

	butwrap = new JPanel();
	butwrap.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

	butbag = new GridBagLayout();
	butwrap.setLayout(butbag);
	
	jb = new JButton("Add");
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	butbag.setConstraints(jb, c);
	butwrap.add(jb);
	jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    String pname = (String) new_plugin_list.getSelectedValue();
		    String cname = (String) new_command_list.getSelectedValue();
		    
		    if((pname == null) || (cname == null))
		    {
			Toolkit.getDefaultToolkit().beep();
		    }
		    else
		    {
			addCommand((String) new_plugin_list.getSelectedValue(), (String) new_command_list.getSelectedValue());
			
			new_panel.setVisible(false);
			frame.getContentPane().remove(new_panel);
			frame.getContentPane().add(edit_panel);
			edit_panel.setVisible(true);
		    }
		}
	    });

	jb = new JButton("Help");
	jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    mview.getHelpTopic("CustomMenu", "#add");
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 0;
	butbag.setConstraints(jb, c);
	butwrap.add(jb);

	jb = new JButton("Cancel");
	jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    new_panel.setVisible(false);
		    frame.getContentPane().remove(new_panel);
		    frame.getContentPane().add(edit_panel);
		    edit_panel.setVisible(true);
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 2;
	c.gridy = 0;
	butbag.setConstraints(jb, c);
	butwrap.add(jb);

	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 2;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.gridwidth = 3;
	gridbag.setConstraints(butwrap, c);
	new_panel.add(butwrap);

	// ------------------------------------------------------------------------------- //
	// ------------------------------------------------------------------------------- //

	frame.getContentPane().add(edit_panel);

	frame.pack();

	current_command = null;

	updateDetails();

	command_tree.clearSelection( );

	Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
   
	frame.setLocation((int)((screen_size.getWidth() - frame.getWidth()) / 2.0),
			  (int)((screen_size.getHeight() - frame.getHeight()) / 2.0));

	frame.setVisible(true);
    }
    


    private PluginCommand current_command;
    private JFrame frame = null;
    private JList  command_list, new_command_list, new_plugin_list, new_args_list;
    private JPanel arg_panel, edit_panel, new_panel;
    private DefaultListModel command_list_model, new_command_list_model, new_plugin_list_model, new_args_list_model;
    JTextField plugin_jtf, command_jtf, name_jtf, hotkey_jtf;

    private JCheckBox show_in_menu_jchkb;

    private PluginCommand getCommand(String pname, String cname)
    {
	Vector all_commands = getAllCommands();

	for(int c=0; c < all_commands.size(); c++)
	{
	    PluginCommand pc = (PluginCommand) all_commands.elementAt(c);
	    if( (pc.name.equals(cname)) && (pc.plugin_name.equals(pname)) )
	    {
		return pc;
	    }
	}
	return null;
    }

    private void updateDetails()
    {
	
	// get the args for the current selection
	arg_panel.removeAll();
	
	boolean disable = (current_command_node == null) || (current_command_node.isMenu());

	hotkey_jtf.setEnabled(!disable);
	show_in_menu_jchkb.setEnabled(!disable);

	if(current_command == null)
	{
	    name_jtf.setText( current_command_node == null ? "" : current_command_node.name);
	    plugin_jtf.setText("");
	    command_jtf.setText("");
	    hotkey_jtf.setText("");
	}
	else
	{
	    // cannot edit root name
	    name_jtf.setEnabled( current_command_node != cmc_root );

	    // set controls to the right values

	    name_jtf.setText(current_command_node.name );
	    plugin_jtf.setText(current_command.plugin_name);
	    command_jtf.setText(current_command.name);
	    hotkey_jtf.setText(current_command_node.hotkey);
	    show_in_menu_jchkb.setSelected(current_command_node.show_in_menu);

	    // get the possible args for this command

	    GridBagLayout gridbag = new GridBagLayout();
	    arg_panel.setLayout(gridbag);
	    
	    /*
	      Font f = plugin_jtf.getFont();
	      Font small_font = new Font(f.getName(), Font.PLAIN, f.getSize() - 2);
	    */
	    
	    if(current_command.args != null)
	    {
		
		// get the arg types from the real CommandInfo object
		PluginCommand the_pc = getCommand(current_command.plugin_name, current_command.name);
		
		int line = 0;
		
		for(int a=0; a < current_command.args.length; a+=2)
		{
		    int off = (a/2) * 5;
		    String arg_type  =  the_pc.args[off+1];
		    String arg_cmmnt =  the_pc.args[off+4];
			
		    // common stuff

		    JLabel label = new JLabel( current_command.args[a] + " ");
		    label.setToolTipText( arg_cmmnt );

		    GridBagConstraints gbc = new GridBagConstraints();
		    gbc.gridx = 0;
		    gbc.gridy = line;
		    gbc.anchor = GridBagConstraints.EAST;
		    gridbag.setConstraints(label, gbc);
		    arg_panel.add(label);
		    
		    // special controls for the different arg types:
		    
		    
		    JComponent comp = null;
		    
		    JPanel argwrap = new JPanel();
		    GridBagLayout argbag = new GridBagLayout();
		    argwrap.setLayout(argbag);
		    
		    // ---- ------------
		    
		    if(!arg_type.equals("file") && !arg_type.equals("boolean"))
		    {
			final JTextField jtf  = new JTextField(15);
			
			jtf.setToolTipText( arg_cmmnt );

			if(current_command.args[a+1] != null)
			    jtf.setText( current_command.args[a+1] );
			
			// catch all updates to the the text in these boxes
			jtf.getDocument().addDocumentListener(new ArgChangeListener(a+1));
			
			
			gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.weightx = gbc.weighty = 1.0;
			argbag.setConstraints(jtf, gbc);
			argwrap.add(jtf);
			
		    }

		    // ---- ------------
 		    
		    if(arg_type.equals("file"))
		    {
			final JTextField jtf  = new JTextField(15);
		    
			jtf.setToolTipText( arg_cmmnt );

			if(current_command.args[a+1] != null)
			    jtf.setText( current_command.args[a+1] );
			
			// catch all updates to the the text in these boxes
			jtf.getDocument().addDocumentListener(new ArgChangeListener(a+1));
			
			
			gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.fill = GridBagConstraints.BOTH;
			gbc.weightx = gbc.weighty = 1.0;
			argbag.setConstraints(jtf, gbc);
			argwrap.add(jtf);
			
			final JButton jb = new JButton(" ... ");
			jb.addActionListener(new CustomFileArgListener(a+1));
			gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = 0;
			argbag.setConstraints(jb, gbc);
			argwrap.add(jb);
			
		    }

		    // ---- ------------
 		    
		    if(arg_type.equals("boolean"))
		    {
			String val = (current_command.args[a+1] == null) ? "null" : current_command.args[a+1];

			ButtonGroup bg = new ButtonGroup();
			
			JRadioButton jchkb = new JRadioButton("true");
			jchkb.setSelected(val.equals("true"));
			jchkb.setHorizontalTextPosition(AbstractButton.LEFT);
			jchkb.addActionListener(new CustomBooleanArgListener(a+1, "true", "false"));
			
			gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.anchor = GridBagConstraints.WEST;
			argbag.setConstraints(jchkb, gbc);

			argwrap.add(jchkb);
			bg.add(jchkb);

			jchkb = new JRadioButton("false");
			jchkb.setSelected(val.equals("false"));
			jchkb.addActionListener(new CustomBooleanArgListener(a+1, "false", "true"));
			
			gbc = new GridBagConstraints();
			gbc.gridx = 1;
			gbc.gridy = 0;
			gbc.anchor = GridBagConstraints.WEST;
			argbag.setConstraints(jchkb, gbc);

			argwrap.add(jchkb);
			bg.add(jchkb);
		    }
		    
		    gbc = new GridBagConstraints();
		    gbc.gridx = 1;
		    gbc.gridy = line;
		    gbc.fill = GridBagConstraints.BOTH;
		    gbc.weightx = gbc.weighty = 1.0;
		    gbc.anchor = GridBagConstraints.WEST;
		    gridbag.setConstraints(argwrap, gbc);
		    arg_panel.add(argwrap);

		    line++;
		}
	    }
	}
	arg_panel.updateUI();
    }

    private void updateNewPluginList()
    {
	// build a unique array of all plugins which have commands

	Vector all_commands = getAllCommands();

	Hashtable plugin_seen = new Hashtable();
	
	new_plugin_list_model = new DefaultListModel();

	Vector plugin_names = new Vector();
	
	for(int c=0; c < all_commands.size(); c++)
	{
	    PluginCommand pc = (PluginCommand) all_commands.elementAt(c);
	    
	    if(plugin_seen.get(pc.plugin_name) == null)
	    {
		plugin_seen.put(pc.plugin_name, "x");
		plugin_names.addElement(pc.plugin_name);
	    }
	}
	
	if(plugin_names.size() > 0)
	{
	    Object[] pn_array = plugin_names.toArray(new String[0]);

	    Arrays.sort(pn_array);
	    
	    for(int p=0; p < pn_array.length; p++)
	    {
		new_plugin_list_model.addElement(pn_array[p]);
	    }
	}

	new_plugin_list.setModel(new_plugin_list_model);
    }

    private void updateNewCommandList()
    {
	Vector all_commands = getAllCommands();

	String plg = (String) new_plugin_list.getSelectedValue();
	
	new_command_list_model = new DefaultListModel();

	if(plg != null)
	{
	    // build an array of the commands that this plugin has...
	    
	    Vector possible_commands = new Vector();

	    for(int c=0; c < all_commands.size(); c++)
	    {
		PluginCommand pc = (PluginCommand) all_commands.elementAt(c);
		if(pc.plugin_name.equals(plg))
		{
		    possible_commands.addElement(pc.name);
		}
	    }
	    
	    if(possible_commands.size() > 0)
	    {
		Object[] pc_array = possible_commands.toArray(new String[0]);

		Arrays.sort(pc_array);
		
		for(int c=0; c < pc_array.length; c++)
		{
		    new_command_list_model.addElement(pc_array[c]);
		}
	    }
	}


	new_command_list.setModel(new_command_list_model);
	updateNewArgsList();
    }

    private void updateNewArgsList()
    {
	Vector all_commands = getAllCommands();

	String plg = (String) new_plugin_list.getSelectedValue();
	String com = (String) new_command_list.getSelectedValue();

	new_args_list_model = new DefaultListModel();
	    
	if((plg != null) && (com != null))
	{
	    // build an array of the commands that this plugin has...
	    
	    for(int c=0; c < all_commands.size(); c++)
	    {
		PluginCommand pc = (PluginCommand) all_commands.elementAt(c);
		if( (pc.name.equals(com)) && (pc.plugin_name.equals(plg)) )
		{
		    int arg_len = (pc.args == null) ? 0 : pc.args.length;
		    if(arg_len > 0)
		    {
			for(int a=0; a < arg_len; a += 5)
			{
			    new_args_list_model.addElement(pc.args[a]);
			}
		    }
		}
	    }
	}
	new_args_list.setModel(new_args_list_model);
	
    }

    public void updateCurrentCommandName()
    {
	TreePath tp = command_tree.getSelectionPath();
	if(tp == null)
	    return;

	CustomMutableTreeNode dmtn = (CustomMutableTreeNode) tp.getLastPathComponent();
	CustomMenuCommandTreeNode cmc = (CustomMenuCommandTreeNode) dmtn.getUserObject();
	
	if(cmc != null)
	{
	    cmc.name = name_jtf.getText();
	    DefaultTreeModel tm = (DefaultTreeModel) command_tree.getModel();
	    tm.nodeChanged(dmtn);

	    // mark the custom menu as out of date
	    updateMenus();
	}


    }

    public void updateCurrentCommandShowInMenu()
    {
	if(current_command_node != null)
	{
	    // change the actual list
	    current_command_node.show_in_menu = show_in_menu_jchkb.isSelected();

	    // mark the custom menu as out of date
	    updateMenus();
	}
    }

    public void updateCurrentCommandHotkey()
    {
	if(current_command_node != null)
	{
	    // change the actual list
	    current_command_node.hotkey = hotkey_jtf.getText();

	    // and any custom popup menus which exist
	    updateMenus();
	}
    }

    public void updateCurrentCommandArg(int arg, String t)
    {
	if(current_command != null)
	{
	    current_command.args[arg] = t;
	}
    }

    // --------------------------------------------------------------------------------------------------- //
    // ----  c r e a t e M e n u  ------------------------------------------------------------------------ //
    // --------------------------------------------------------------------------------------------------- //
    
    JMenu custom_menu = null;
    boolean custom_menu_needs_rebuilding = true;
    Hashtable custom_menu_map = null;

    public JMenu createMenu()
    {
	if(custom_menu_needs_rebuilding)
	{
	    custom_menu = new JMenu("Custom");
	    
	    custom_menu_map = new Hashtable();

	    CustomMenuListener cml = new CustomMenuListener();

	    if(cmc_root.getNumChildren() > 0)
	    {
		CustomMenuCommandTreeNode[] kids = cmc_root.getChildren();
		for(int c=0; c < kids.length; c++)
		    addItemsToMenu(custom_menu, cml, kids[c] );
	    }
	    
	    custom_menu.addSeparator();

	    JMenuItem mi = new JMenuItem("Edit");
	    mi.addActionListener(new CustomMenuListener());
	    custom_menu.add(mi);

	    custom_menu_needs_rebuilding = false;
	}
	
	return custom_menu;
    }

    private void updateMenus()
    {
	custom_menu_needs_rebuilding = true;
    }

    private void addItemsToMenu( JMenu menu, CustomMenuListener cml, CustomMenuCommandTreeNode node)
    {
	if( node.isMenu() )
	{
	    JMenu submenu = new JMenu( node.name );
	    CustomMenuCommandTreeNode[] kids = node.getChildren();
	    if(kids != null)
		for(int c=0; c < kids.length; c++)
		    addItemsToMenu( submenu, cml, kids[c] );
	    menu.add(submenu);
	}
	else
	{
	    if( node.show_in_menu )
	    {
		JMenuItem mi = new JMenuItem( node.name );
		mi.addActionListener( cml );
		menu.add(mi);

		custom_menu_map.put( mi, node );
	    }
	}
    }

    public class CustomMenuListener implements ActionListener
    {
	public void actionPerformed(ActionEvent e) 
	{
	    JMenuItem mi = (JMenuItem) e.getSource();

	    CustomMenuCommandTreeNode node = (CustomMenuCommandTreeNode) custom_menu_map.get( mi );

	    if(node == null)
		createEditorPanel();
	    else
		runCommand( node.command );
	}
    }

    // --------------------------------------------------------------------------------------------------- //
    // ----   key watching   ----------------------------------------------------------------------------- //
    // --------------------------------------------------------------------------------------------------- //

    private boolean custom_menu_hotkey_armed = false;
    private boolean escape_key_armed = false;
    
    public boolean handleKeyEvent( KeyEvent e )
    {
	
	if(custom_menu_hotkey_armed)
	{
	    if(e.getKeyCode() == KeyEvent.VK_ALT)
	    {
		//System.out.println("ignore alt");

		return true;
	
		// this is alt being released, ignore it....
	    }
	    else
	    {
		mview.getDataPlot().stopAnimation();
		
		// System.out.println("send to custom menu: " + e.getKeyCode());
		
		custom_menu_hotkey_armed = false;
		
		matchAltKeyCommand( e.getKeyChar(), cmc_root );
		
		return true;
	    }
	}
	
	if(e.isAltDown())
	{
	    //custom menu hotkey...
	    if(e.getKeyCode() == KeyEvent.VK_C)
	    {
		// System.out.println("custom_menu_hotkey_armed... ");
		
		custom_menu_hotkey_armed = true;

		return true;
	    }
	}

	if(e.getKeyCode() == KeyEvent.VK_ESCAPE)
	{
	    // System.out.println("escape_key_armed... ");

	    mview.getDataPlot().stopAnimation();
	    
	    resetEscapeMatching();
	    escape_key_armed = true;

	    mview.setMessage( getPossibleEscapeMatches( cmc_root ) );

	    return true;
	}

	if(escape_key_armed)
	{
	    String hk = String.valueOf( e.getKeyChar() );
	    hk = hk.toUpperCase();
	    match_str += hk;
	    
	    //System.out.println("send to custom menu: " + e.getKeyCode());
	    if(matchEscapeChars( match_str, cmc_root ) == true)
	    {
		
		// the sequence has been successfully matched and a command executed
		escape_key_armed = false;
		return true;
	    }
	    else
	    {
		mview.setMessage( getPossibleEscapeMatches( cmc_root ) );
	    }
	}
	
	if(matchFunctionKeys( e, cmc_root ) == true)
	{
	    mview.getDataPlot().stopAnimation();
	    return true;
	}

	return false;
    }

    // --------------------------------------------------------------------------------------------------- //
    // ----   alt C key matching   ---------------------------------------------------------------------- //
    // --------------------------------------------------------------------------------------------------- //

    public void matchAltKeyCommand(char ch, CustomMenuCommandTreeNode cmc ) 
    {
	if( cmc.isMenu() )
	{
	    CustomMenuCommandTreeNode[] kids = cmc.getChildren();
	    if(kids != null)
		for(int c=0; c < kids.length; c++)
		    matchAltKeyCommand( ch, kids[c] );
	}
	else
	{
	    String hk = String.valueOf(ch);
	    hk = hk.toUpperCase();

	    if(cmc.hotkey.startsWith(hk))
	    {
		runCommand( cmc.command );
	    }
	}
    }

    // --------------------------------------------------------------------------------------------------- //
    // ----   function key matching   -------------------------------------------------------------------- //
    // --------------------------------------------------------------------------------------------------- //

    final int[] fkey_codes = { KeyEvent.VK_F1,  KeyEvent.VK_F2,  KeyEvent.VK_F3,  KeyEvent.VK_F4,
			       KeyEvent.VK_F5,  KeyEvent.VK_F6,  KeyEvent.VK_F7,  KeyEvent.VK_F8,
			       KeyEvent.VK_F9,  KeyEvent.VK_F10, KeyEvent.VK_F11, KeyEvent.VK_F12,
			       KeyEvent.VK_F13, KeyEvent.VK_F14, KeyEvent.VK_F15, KeyEvent.VK_F16 };
    
    public boolean matchFunctionKeys( KeyEvent e, CustomMenuCommandTreeNode cmc )
    {
	for(int fk=0; fk < fkey_codes.length; fk++)
	    if( e.getKeyCode() == fkey_codes[ fk ] )
	    {
		// System.out.println("it's a function key, number " + (fk+1) + " in fact");
		matchFunctionKey( String.valueOf( fk+1 ), cmc );
	    }
	
	return false;
    }

    public void matchFunctionKey(String function_key, CustomMenuCommandTreeNode node) 
    {
	if( node.isMenu() )
	{
	    CustomMenuCommandTreeNode[] kids = node.getChildren();
	    if(kids != null)
		for(int c=0; c < kids.length; c++)
		     matchFunctionKey( function_key, kids[c] );
	}
	else
	{
	    if(node.hotkey != null)
	    {
		String hk = node.hotkey.toLowerCase();
		if(hk.startsWith("fn"))
		{
		    String fki = hk.substring(2).trim();
		    String fks = function_key;
		    if(fks.equals(fki))
		    {
			runCommand( node.command );
			return;
		    }
		}
	    }
	}
    }

    // --------------------------------------------------------------------------------------------------- //
    // ----   escape sequence matching   ----------------------------------------------------------------- //
    // --------------------------------------------------------------------------------------------------- //

    public boolean matchEscapeChars( String match_str, CustomMenuCommandTreeNode node) 
    {
	
		
	if( node.isMenu() )
	{
	    CustomMenuCommandTreeNode[] kids = node.getChildren();
	    if(kids != null)
		for(int c=0; c < kids.length; c++)
		    if(matchEscapeChars( match_str, kids[c] ))
			return true;
	}
	else
	{
	    if( node.hotkey != null )
	    {
		//System.out.println("matchEscapeChars() sequence is: '" + match_str + "'");
		
		String spec = node.hotkey.toUpperCase();
		
		if(spec.startsWith("ESC "))
		{
		    String chars = "";
		    int chi = 4;
		    while(chi < spec.length())
		    {
			char ch = spec.charAt(chi);
			if(Character.isLetterOrDigit(ch))
			    chars += ch;
			chi++;
		    }
		    
		    //System.out.println("matchEscapeChars():  testing: '" + spec + 
		    //	       "' -> '" + chars + "' with '" + match_str + "'");
		
		    if(chars.equals( match_str ))
		    {
			//System.out.println("...hit!");

			mview.setMessage( "Running command '" + node.name + "'");
			
			runCommand( node.command );
			
			return true;
		    }
		}
	    }
	}
	return false;
    }

    public void resetEscapeMatching()
    {
	match_str = "";
    }

    public String getPossibleEscapeMatches( CustomMenuCommandTreeNode node )
    {
	java.util.Hashtable possible_next_char = new java.util.Hashtable();

	addPossibleEscapeMatches(  possible_next_char, node );

	String result = "";

	for (Enumeration e = possible_next_char.keys(); e.hasMoreElements() ;) 
	{
	    Character ch = (Character)  e.nextElement();
	    String cn = (String) possible_next_char.get( ch );
	    
	    if(possible_next_char.size() < 3)
	    {
		result += ch + " = " + cn + "  ";
	    }
	    else
	    {
		result += ch + " ";
	    }
	}

	return result;
    }

    private void addPossibleEscapeMatches( java.util.Hashtable match, CustomMenuCommandTreeNode node )
    {
		
	if( node.isMenu() )
	{
	    CustomMenuCommandTreeNode[] kids = node.getChildren();
	    if(kids != null)
		for(int c=0; c < kids.length; c++)
		    addPossibleEscapeMatches( match, kids[c] );
	}
	else
	{
	    if( node.hotkey != null )
	    {
		
		String spec = node.hotkey.toUpperCase();
		
		if(spec.startsWith("ESC "))
		{
		    
		    String chars = "";
		    int chi = 4;
		    while(chi < spec.length())
		    {
			char ch = spec.charAt(chi);
			if(Character.isLetterOrDigit(ch))
			    chars += ch;
			chi++;
		    }
		    
		    // System.out.println(" addPossibleEscapeMatches() sequence is: '" + chars + "'");
		
		    if(match_str.length() == 0)
		    {
			match.put(new Character(chars.charAt(0)), node.name );
		    }
		    else
		    {
			if(chars.startsWith( match_str ))
			{
			    int len = match_str.length();
			    
			    if(len < chars.length())
				match.put(new Character(chars.charAt(len)),  node.name);
			}
		    }
		}
	    }
	}
    }

    private String match_str = "";

    // --------------------------------------------------------------------------------------------------- //
    // ----   c o m m a n d s   -------------------------------------------------------------------------- //
    // --------------------------------------------------------------------------------------------------- //

    public void runCommand(PluginCommand pc)
    {
	if(pc.plugin_name.startsWith("["))
	    runBuiltInCommand( pc );
	else
	    mview.sendCommandToPlugin(pc.plugin_name, pc.name, pc.args);
    }
    

    // ------------------------------------------ //

    public void addCommand()
    {
	
	// activate the new command panel

	updateNewPluginList();
	updateNewCommandList();
	updateNewArgsList();

	edit_panel.setVisible(false);

	frame.getContentPane().remove(edit_panel);

	frame.getContentPane().add(new_panel);

	new_panel.setVisible(true);

    }

    public void addCommand(String pname, String cname)
    {
	Vector all_commands = getAllCommands();
	
	// how many args does this command have?

	int n_args = 0;
	String[] real_args = null;
	for(int c=0; c < all_commands.size(); c++)
	{
	    PluginCommand pc = (PluginCommand) all_commands.elementAt(c);
	    if((pc.plugin_name.equals(pname)) &&
	       (pc.name.equals(cname)))
	    {
		n_args = (pc.args == null) ? 0 : (pc.args.length / 5);
		real_args = pc.args;
	    }
	}
	
	// assemble a new PluginCommand object
	
	PluginCommand pc = new  PluginCommand(cname, null);
	pc.plugin_name = pname;
	pc.args = null;

	if(n_args > 0)
	{
	    pc.args = new String[n_args * 2];
	    for(int a=0; a < n_args; a++)
	    {
		pc.args[a*2]     = real_args[a*5];
		pc.args[(a*2)+1] = null;
	    }
	}
	
	// add it to tree
	
	CustomMenuCommandTreeNode new_cmc = new CustomMenuCommandTreeNode( pname + "." + cname, "", pc );
	
	updateMenus();
	
	insertNode( new_cmc, true );
    }
    
    
    public void loadCommands()
    {

	final String data_file_name = mview.getConfigDirectory() + "custom-menu.dat";
	
	command_list = null;
	menu_entries = null;

	try
	{
	    FileInputStream fis = new FileInputStream(new File(data_file_name));
	    ObjectInputStream ois = new ObjectInputStream(fis);
	    commands     = (Vector) ois.readObject();
	    menu_entries = (Vector) ois.readObject();
	    try
	    {
		hotkeys = (Vector) ois.readObject();
	    }
	    catch(IOException ioe) // other than FileNotFound
	    {
		// no problem, just means that hot keys aren't defined yet
		// setup empty values
		hotkeys = new Vector();
		for(int e=0; e < menu_entries.size(); e++)
		    hotkeys.addElement(new String(""));
	    }
	    ois.close();
	}
	catch(FileNotFoundException ioe)
	{
	    // no problem, it just means the no menu entries exist yet
	}
	catch(ClassNotFoundException fnfe)
	{
	    mview.errorMessage("Cannot understand class definition in Custom Menu file '" + data_file_name + "' \n  " + fnfe);

	}
	catch(IOException ioe) // other than FileNotFound
	{
	    mview.errorMessage("Cannot parse Custom Menu file '" + data_file_name + "' \n  " + ioe);
	}
     }

    /*
    public void saveCommands()
    {
	
	final String data_file_name = mview.getConfigDirectory() + "custom-menu.dat";

	try
	{
	    FileOutputStream fos = new FileOutputStream(new File(data_file_name));
	    ObjectOutputStream oos = new ObjectOutputStream(fos);
	    oos.writeObject(commands);
	    oos.writeObject(menu_entries);
	    oos.writeObject(hotkeys);
	    oos.flush();
	    oos.close();
	}
	catch(IOException ioe)
	{
	    mview.errorMessage("Cannot save definitions file\n  " + ioe);
	}
    }
    */

    // --------------------------------------------------------------------------------------------------- //
    // ----   l i s t e n e r s   ------------------------------------------------------------------------ //
    // --------------------------------------------------------------------------------------------------- //

    public class CustomArgListener implements ActionListener
    {
	private int item;

	public CustomArgListener(int item_)
	{
	    item = item_;
	}

	public void actionPerformed(ActionEvent e) 
	{
	    updateCurrentCommandArg(item, ((JTextField) (e.getSource())).getText());
	}
    }

    // handles the name text field in the NameFilter controls on the Filter panel
    //
    class ArgChangeListener implements DocumentListener 
    {
	private int item;

	public ArgChangeListener(int item_)
	{
	    item = item_;
	}

	public void insertUpdate(DocumentEvent e)  { propagate(e); }
	public void removeUpdate(DocumentEvent e)  { propagate(e); }
	public void changedUpdate(DocumentEvent e) { propagate(e); }

	private void propagate(DocumentEvent e)
	{
	    try
	    {
		updateCurrentCommandArg(item, e.getDocument().getText(0, e.getDocument().getLength()));
	    }
	    catch (javax.swing.text.BadLocationException ble)
	    {
		System.out.println("wierd string....\n");
	    }
	}
    }

    public class CustomFileArgListener implements ActionListener
    {
	private int item;

	public CustomFileArgListener(int item_)
	{
	    item = item_;
	}

	public void actionPerformed(ActionEvent e) 
	{
	    JFileChooser fc = mview.getFileChooser();
	    if(fc.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION)
	    {
		File file = fc.getSelectedFile();
		
		if(file != null)
		{
		    updateCurrentCommandArg(item, file.getPath());

		    // also need to update the file name in the text field...

		    updateDetails();
		}
	    }
	}
    }

    public class CustomBooleanArgListener implements ActionListener
    {
	private int item;
	private String tstr;
	private String fstr;
	
	public CustomBooleanArgListener(int item_, String tstr_, String fstr_)
	{
	    item = item_;
	    tstr = tstr_;
	    fstr = fstr_;
	}
	
	public void actionPerformed(ActionEvent e) 
	{
	    JRadioButton jrb = (JRadioButton) e.getSource();
	    updateCurrentCommandArg(item, (jrb.isSelected() ? tstr : fstr));
	}
    }

    public class CustomMouseListener implements MouseListener
    {
	private int item;

	public CustomMouseListener(int item_)
	{
	    item = item_;
	}


	public void mousePressed(MouseEvent e) 
	{
	}
	
	public void mouseReleased(MouseEvent e) 
	{
	}
	
	public void mouseClicked(MouseEvent e) 
	{
	}
	
	public void mouseEntered(MouseEvent e) 
	{
	}

	public void mouseExited(MouseEvent e) 
	{
	    //System.out.println("exited " + item);

	    if(item == -1)
	    {
		// update current command name
		updateCurrentCommandName();
	    }
	    else
	    {
		if(item == 1)
		{
		    // update hotkey
		    updateCurrentCommandHotkey();
		}
		else
		{
		    // update current command args
		    updateCurrentCommandArg(item, ((JTextField) (e.getSource())).getText());
		}
	    }
	}
    }

    // --------------------------------------------------------------------------------------------------- //
    // ----   support for built-in commands new in verion 0.9.2   ---------------------------------------- //
    // --------------------------------------------------------------------------------------------------- //

    final String built_in_edata_command = "[ExprData]";
    final String built_in_dplot_command = "[DataPlot]";
    final String built_in_mview_command = "[maxdView]";

    private void addBuiltInCommands( Vector vec )
    {
	String[] args = null;
	PluginCommand bpc = null;
	
	//// /// /// // /// // // /  /   /    / 
	
	bpc = new PluginCommand("Select all Spots", null);
	bpc.plugin_name = built_in_edata_command;
	vec.addElement(bpc);

	bpc = new PluginCommand("Unselect all Spots", null);
	bpc.plugin_name = built_in_edata_command;
	vec.addElement(bpc);

	bpc = new PluginCommand("Invert Spot selection", null);
	bpc.plugin_name = built_in_edata_command;
	vec.addElement(bpc);

	bpc = new PluginCommand("Select all Measurements", null);
	bpc.plugin_name = built_in_edata_command;
	vec.addElement(bpc);

	bpc = new PluginCommand("Unselect all Measurements", null);
	bpc.plugin_name = built_in_edata_command;
	vec.addElement(bpc);

	bpc = new PluginCommand("Invert Measurement selection", null);
	bpc.plugin_name = built_in_edata_command;
	vec.addElement(bpc);

	bpc = new PluginCommand("Select all Spots and Measurements", null);
	bpc.plugin_name = built_in_edata_command;
	vec.addElement(bpc);

	bpc = new PluginCommand("Unselect all Spots and Measurements", null);
	bpc.plugin_name = built_in_edata_command;
	vec.addElement(bpc);

	bpc = new PluginCommand("Invert Spot and Measurement selections", null);
	bpc.plugin_name = built_in_edata_command;
	vec.addElement(bpc);

	//// /// /// // /// // // /  /   /    / 
	
	args = new String[] { "scale", "integer" };
	bpc = new PluginCommand("Set zoom scale", args);
	bpc.plugin_name = built_in_dplot_command;
	vec.addElement(bpc);
	
	bpc = new PluginCommand("Zoom in", null);
	bpc.plugin_name = built_in_dplot_command;
	vec.addElement(bpc);

	bpc = new PluginCommand("Zoom out", null);
	bpc.plugin_name = built_in_dplot_command;
	vec.addElement(bpc);

	bpc = new PluginCommand("Open Find Dialog", null);
	bpc.plugin_name = built_in_dplot_command;
	vec.addElement(bpc);

	bpc = new PluginCommand("Open Layout Dialog", null);
	bpc.plugin_name = built_in_dplot_command;
	vec.addElement(bpc);

	bpc = new PluginCommand("Open Colouriser Dialog", null);
	bpc.plugin_name = built_in_dplot_command;
	vec.addElement(bpc);

	bpc = new PluginCommand("Open Annotation Loader Options Dialog", null);
	bpc.plugin_name = built_in_dplot_command;
	vec.addElement(bpc);

	bpc = new PluginCommand("Open Custom Menu Editor Dialog", null);
	bpc.plugin_name = built_in_dplot_command;
	vec.addElement(bpc);


	bpc = new PluginCommand("New View", null);
	bpc.plugin_name = built_in_dplot_command;
	vec.addElement(bpc);

	bpc = new PluginCommand("Open Print Dialog", null);
	bpc.plugin_name = built_in_dplot_command;
	vec.addElement(bpc);

	bpc = new PluginCommand("Apply Filter", null);
	bpc.plugin_name = built_in_dplot_command;
	vec.addElement(bpc);

	bpc = new PluginCommand("Do Not Apply Filter", null);
	bpc.plugin_name = built_in_dplot_command;
	vec.addElement(bpc);


	//// /// /// // /// // // /  /   /    / 

	bpc = new PluginCommand("Rescan plugins", null);
	bpc.plugin_name = built_in_mview_command;
	vec.addElement(bpc);

	args = new String[] { "request confirmation", "boolean" };
	bpc = new PluginCommand("Exit", args);
	bpc.plugin_name = built_in_mview_command;
	vec.addElement(bpc);

    }

    private void runBuiltInCommand( PluginCommand pc )
    {
	// System.out.println(" runBuiltInCommand() " + pc.name);

	final ExprData edata = mview.getExprData();
	final DataPlot dplot = mview.getDataPlot();
	
	//// /// /// // /// // // /  /   /    / 
	
	if(pc.name.equals("Select all Spots"))
	{
	    edata.selectAllSpots();
	    return;
	}
	if(pc.name.equals("Unselect all Spots"))
	{
	    edata.clearSpotSelection();
	    return;
	}
	if(pc.name.equals("Invert Spot selection"))
	{
	    edata.invertSpotSelection();
	    return;
	}
	
	//// /// /// // /// // // /  /   /    / 
	
	if(pc.name.equals("Select all Measurements"))
	{
	    edata.selectAllMeasurements();
	    return;
	}
	if(pc.name.equals("Unselect all Measurements"))
	{
	    edata.clearMeasurementSelection();
	    return;
	}
	if(pc.name.equals("Invert Measurement selection"))
	{
	    edata.invertMeasurementSelection();
	    return;
	}

	//// /// /// // /// // // /  /   /    / 
	
	if(pc.name.equals("Select all Spots and Measurements"))
	{
	    edata.selectAllSpots();
	    edata.selectAllMeasurements();
	    return;
	}
	if(pc.name.equals("Unselect all Spots and Measurements"))
	{
	    edata.clearSpotSelection();
	    edata.clearMeasurementSelection();
	    return;
	}
	if(pc.name.equals("Invert Spots and Measurement selections"))
	{
	    edata.invertSpotSelection();
	    edata.invertMeasurementSelection();
	    return;
	}

	//// /// /// // /// // // /  /   /    / 
	
	if(pc.name.equals("Set zoom scale"))
	{
	    int zs = mview.parseIntArg( mview.getPluginArg("scale", pc.args ) );
	    mview.setZoom( zs );
	    return;
	}

	if(pc.name.equals("Zoom in"))
	{
	    mview.changeZoom( +1 );
	    return;
	}

	if(pc.name.equals("Zoom out"))
	{
	    mview.changeZoom( -1 );
	    return;
	}

	//// /// /// // /// // // /  /   /    / 
	
	if(pc.name.equals("Open Find Dialog"))
	{
	    mview.createFindDialog();
	    return;
	}
	if(pc.name.equals("Open Layout Dialog"))
	{
	    new DataPlotLayoutOptions(mview, dplot);
	    return;
	}
	if(pc.name.equals("Open Colouriser Dialog"))
	{
	    new DataPlotColourOptions(mview);
	    return;
	}
	if(pc.name.equals("Open Annotation Loader Options Dialog"))
	{
	    mview.getAnnotationLoader().createOptionsDialog();
	    return;
	}
	if(pc.name.equals("Open Custom Menu Editor Dialog"))
	{
	    createEditorPanel();
	    return;
	}

	//// /// /// // /// // // /  /   /    / 

	if(pc.name.equals("New View"))
	{
	    mview.openNewView();
	    return;
	}
	if(pc.name.equals("Open Print Dialog"))
	{
	    dplot.printDisplay();
	    return;
	}
	if(pc.name.equals("Apply Filter"))
	{
	    mview.setApplyFilter( true );
	    return;
	}
	if(pc.name.equals("Do Not Apply Filter"))
	{
	    mview.setApplyFilter( false );
	    return;
	}

	//// /// /// // /// // // /  /   /    / 
	
	if(pc.name.equals("Exit"))
	{
	    boolean rc = mview.parseBooleanArg( mview.getPluginArg("request confirmation", pc.args ) );
	    if( (rc==false) || (mview.infoQuestion("Really exit?", "Yes", "No") == 0) )
		mview.cleanExit();

	    return;
	}
	
	if(pc.name.equals("Rescan plugins"))
	{
	    mview.scanPlugins();
	    return;
	}
	

	//// /// /// // /// // // /  /   /    / 

	mview.alertMessage("Unhandled command");
    }

    // --------------------------------------------------------------------------------------------------- //
    // ----   new tree format in verion 0.9.2   ---------------------------------------------------------- //
    // --------------------------------------------------------------------------------------------------- //

    private JTree command_tree;
    
    private CustomMenuCommandTreeNode cmc_root;

    private CustomMenuCommandTreeNode current_command_node = null;

    private CustomMenuCommandTreeNode clipboard = null;

    private void debugCustomMenuTree( String message )
    {
	System.out.println( "--- " + message + " ---");
	debugCustomMenuTree( 0, cmc_root );
 	System.out.println( "-----------------" );
   }


    // ----------------------------------------------------------- //

    private class CustomMutableTreeNode extends DefaultMutableTreeNode
    {
	public CustomMutableTreeNode( CustomMenuCommandTreeNode cmc_node )
	{
	    super( cmc_node );
	}
	
	public boolean isLeaf()
	{
	    return (getAllowsChildren() == false);
	}

	public boolean getAllowsChildren()
	{
	    CustomMenuCommandTreeNode cmc_node = (CustomMenuCommandTreeNode) getUserObject();
	    if(cmc_node == null)
		return false;
	    return ( cmc_node.isMenu() );
	}
    }
    
    // ----------------------------------------------------------- //

    private void debugCustomMenuTree( int depth, CustomMenuCommandTreeNode node )
    {
	for(int d=0; d < depth; d++)
	    System.out.print(" ");

	if(node.children == null)
	{
	    System.out.println( "[ " + node.name + " ]");
	}
	else
	{
	    System.out.println( node.name );
	    for(int c=0; c < node.children.length; c++)
		debugCustomMenuTree( depth+1, node.children[c] );
	}
    }


    private boolean readCustomMenuTree()
    {
	final String data_file_name = mview.getConfigDirectory() + "custom-menu-tree.dat";
	
	try
	{
	    FileInputStream fis = new FileInputStream(new File(data_file_name));
	    ObjectInputStream ois = new ObjectInputStream(fis);
	
	    cmc_root = (CustomMenuCommandTreeNode) ois.readObject();

	    // debugCustomMenuTree("loaded:");

	    return true;
	}
	catch(FileNotFoundException ioe)
	{
	    // no problem, it just means the no menu entries exist yet
	}
	catch(ClassNotFoundException fnfe)
	{
	    mview.errorMessage("Cannot understand class definition in Custom Menu file '" + data_file_name + "' \n  " + fnfe);

	}
	catch(IOException ioe) // other than FileNotFound
	{
	    mview.errorMessage("Cannot parse Custom Menu file '" + data_file_name + "' \n  " + ioe);
	}
	catch(Exception e) // something odd, such as a format problem
	{
	    mview.errorMessage("Unexpected exception when loading the Custom Menu file." + e);
	}
	
	return false;
    }

    private boolean writeCustomMenuTree()
    {

	final String data_file_name = mview.getConfigDirectory() + "custom-menu-tree.dat";

	try
	{
	    FileOutputStream fos = new FileOutputStream(new File(data_file_name));
	    ObjectOutputStream oos = new ObjectOutputStream(fos);
	    oos.writeObject( cmc_root );
	    oos.flush();
	    oos.close();

	    //debugCustomMenuTree("saved:");

	    return true;
	}
	catch(IOException ioe)
	{
	    mview.errorMessage("Cannot save definitions file '" + data_file_name + "'\n  " + ioe);
	}
	return false;
    }

    private boolean makeCustomMenuTreeFromOldFormat()
    {
	loadCommands();

	if(commands == null)
	{
	    // mview.alertMessage("Unable to load old custom menu data format");
	    commands = new Vector();
	}

	// System.out.println( commands.size() + " entries loaded");

	cmc_root = new CustomMenuCommandTreeNode("Custom");
	
	CustomMenuCommandTreeNode[] children = new CustomMenuCommandTreeNode[commands.size()];
	for(int c=0; c <  commands.size(); c++)
	{
	    PluginCommand pc = (PluginCommand) commands.elementAt(c);
	    String name = (String) menu_entries.elementAt(c);
	    String hotkey = (String) hotkeys.elementAt(c);
	    children[c] = new CustomMenuCommandTreeNode( name, hotkey, pc );
	}
	cmc_root.setChildren( children );

	return true;
    }

    private void populateTreeWithCommands( JTree tree )
    {
	CustomMutableTreeNode dmtn = generateTreeNodes( cmc_root );
	DefaultTreeModel model =  new DefaultTreeModel( dmtn );
	tree.setModel(model);
    }

    private CustomMutableTreeNode generateTreeNodes( CustomMenuCommandTreeNode cmc )
    {
	CustomMutableTreeNode node = new CustomMutableTreeNode( cmc );
	
	if(cmc.isMenu())
	{
	    if(cmc.getNumChildren() > 0)
	    {
		CustomMenuCommandTreeNode[] kids = cmc.getChildren();
		for(int ch=0; ch < kids.length; ch++)
		{
		    node.add( generateTreeNodes( kids[ ch ] ) );
		}
	    }
	}

	return node;
    }

    // -----------------------

    private CustomMenuCommandTreeNode getTreeSelection()
    {
	TreePath tp = command_tree.getSelectionPath();
	if(tp == null)
	    return null;
	CustomMutableTreeNode dmtn = (CustomMutableTreeNode) tp.getLastPathComponent();
	CustomMenuCommandTreeNode cmc = (CustomMenuCommandTreeNode) dmtn.getUserObject();
	return cmc;
    }

    private void setTreeSelection( CustomMutableTreeNode node )
    {
	setTreeSelection( node, false);
    }

    private void setTreeSelection( CustomMutableTreeNode node, boolean expand )
    {
	DefaultTreeModel tm = (DefaultTreeModel) command_tree.getModel();
	
	TreeNode[] tnp = tm.getPathToRoot( node );
	
	TreePath tp = new TreePath( tnp );
	
	command_tree.setSelectionPath( tp );

	if(expand)
	{
	    command_tree.expandPath( tp );
	    command_tree.scrollPathToVisible( tp );
	}
    }

    // -----------------------

    // called when the tree has been modified
    //
    // rebuilds the CustomMenuCommandTreeNode
    // to match the corresponding CustomMutableTreeNode

    private void rebuildTreeNode( CustomMutableTreeNode node )
    {
	CustomMenuCommandTreeNode cmc = (CustomMenuCommandTreeNode) node.getUserObject();

	final int cc = node.getChildCount();

	if(cc == 0)
	{
	    cmc.setChildren( null );
	}
	else
	{
	    CustomMenuCommandTreeNode[] ch_a = new CustomMenuCommandTreeNode[ cc ];

	    for(int c=0; c < cc ; c++)
		ch_a[c] = (CustomMenuCommandTreeNode) ((CustomMutableTreeNode)node.getChildAt(c)).getUserObject() ;

	    cmc.setChildren( ch_a );
	}

    }

    // -----------------------

    private void treeSelectionHasChanged()
    {
	CustomMenuCommandTreeNode cmc = getTreeSelection();
	
	current_command_node = cmc;

	if(cmc == null)
	{
	    current_command = null;
	    return;	
	}

	current_command = current_command_node.command;

	updateDetails();
    }

    // -----------------------

    private void addMenu()
    {
	TreePath tp = command_tree.getSelectionPath();
	if(tp == null)
	{
	    mview.alertMessage("You must select an existing Menu first");
	    return;
	}

	CustomMutableTreeNode dmtn = (CustomMutableTreeNode) tp.getLastPathComponent();
	CustomMenuCommandTreeNode cmc = (CustomMenuCommandTreeNode) dmtn.getUserObject();
	
	if((cmc == null) || (cmc.isMenu() == false))
	{
	    mview.alertMessage("You must select an existing Menu first");
	    return;
	}

	insertNode( new CustomMenuCommandTreeNode("[new menu]"), true );

    }
  
    private void raiseNode()
    {
	TreePath tp = command_tree.getSelectionPath();
	if(tp == null)
	    return;

	DefaultTreeModel tm = (DefaultTreeModel) command_tree.getModel();
	
	CustomMutableTreeNode dmtn = (CustomMutableTreeNode) tp.getLastPathComponent();
	
	CustomMutableTreeNode parent = (CustomMutableTreeNode) dmtn.getParent();
	if(parent == null)
	    return;
    
	int pos = parent.getIndex( dmtn );
	
	if(--pos >= 0 )
	{ 
	    tm.removeNodeFromParent( dmtn );
	    
	    tm.insertNodeInto( dmtn, parent, pos );

	    rebuildTreeNode( parent );

	    setTreeSelection( dmtn );
	}
	
    }

    private void lowerNode()
    {
	TreePath tp = command_tree.getSelectionPath();
	if(tp == null)
	    return;

	DefaultTreeModel tm = (DefaultTreeModel) command_tree.getModel();
	
	CustomMutableTreeNode dmtn = (CustomMutableTreeNode) tp.getLastPathComponent();
	
	CustomMutableTreeNode parent = (CustomMutableTreeNode) dmtn.getParent();
	if(parent == null)
	    return;
    
	int pos = parent.getIndex( dmtn );
	
	if(++pos < parent.getChildCount() )
	{ 
	    tm.removeNodeFromParent( dmtn );
	    
	    tm.insertNodeInto( dmtn, parent, pos );

	    rebuildTreeNode( parent );

	    setTreeSelection( dmtn );
	}

	/*
	else
	{
	    // fallen off the bottom, move to top of next menu

	    CustomMutableTreeNode grand_parent = (CustomMutableTreeNode) parent.getParent();
	    if(grand_parent == null)
		return;

	    pos = grand_parent.getIndex( dmtn );
	    
	    if(++pos < grand_parent.getChildCount() )
	    { 
		tm.removeNodeFromParent( dmtn );
		
		tm.insertNodeInto( dmtn, grand_parent, pos );
		
		rebuildTreeNode( parent );
		rebuildTreeNode( grand_parent );
		
		setTreeSelection( dmtn );
	    }

	}
	*/
    }

    private void cutNode( boolean do_cut )
    {
	TreePath tp = command_tree.getSelectionPath();
	if(tp == null)
	    return;
	
	DefaultTreeModel tm = (DefaultTreeModel) command_tree.getModel();
	
	CustomMutableTreeNode dmtn = (CustomMutableTreeNode) tp.getLastPathComponent();
	
	CustomMenuCommandTreeNode node = (CustomMenuCommandTreeNode) dmtn.getUserObject();

	{
	    clipboard = ((CustomMenuCommandTreeNode) dmtn.getUserObject()).copy();
	    
	    if(do_cut)
	    {
		CustomMutableTreeNode parent = (CustomMutableTreeNode) dmtn.getParent();

		tm.removeNodeFromParent( dmtn );

		rebuildTreeNode( parent );
	    }
	}
    }
    
    private void pasteNode()
    {
	if( clipboard == null )
	    return;

	insertNode( clipboard.copy(), true );

    }
 
    private void insertNode( CustomMenuCommandTreeNode cmc, boolean expand )
    {
	DefaultTreeModel tm = (DefaultTreeModel) command_tree.getModel();
	
	TreePath tp = command_tree.getSelectionPath();
	
	if(tp == null)
	{
	    tp = command_tree.getPathForRow( 0 );

	    if(tp == null)
		return;
	}

	CustomMutableTreeNode dmtn = (CustomMutableTreeNode) tp.getLastPathComponent();
	
	CustomMenuCommandTreeNode node = (CustomMenuCommandTreeNode) dmtn.getUserObject();
	
	CustomMutableTreeNode new_node = new CustomMutableTreeNode( cmc );
	    
	if(node.isMenu() == false)
	{
	    // insert into the parent Menu at the same position as the selected Command
	    
	    CustomMutableTreeNode parent = (CustomMutableTreeNode) dmtn.getParent();
	    if(parent == null)
		return;
	    
	    int pos = parent.getIndex( dmtn );
	    
	    recursiveInsert( tm, pos, parent , new_node );
	}
	else
	{
	    // insert into the selected Menu (at the bottom of existing entries in that Menu)

	    int pos = dmtn.getChildCount();
	    //if(pos > 0)
	    //pos--;

	    recursiveInsert( tm, pos, dmtn , new_node );
	}
	
	setTreeSelection( new_node, expand );
    }

    private void recursiveInsert( DefaultTreeModel tm, int pos, 
				  CustomMutableTreeNode parent, CustomMutableTreeNode new_node )
    {
	CustomMenuCommandTreeNode cmc = (CustomMenuCommandTreeNode) new_node.getUserObject();
	
	if(cmc.isMenu() && (cmc.getNumChildren() > 0))
	{
	    CustomMenuCommandTreeNode[] kids = cmc.getChildren();
	    for(int c=0; c < kids.length; c++)
	    {
		CustomMutableTreeNode knode = new CustomMutableTreeNode( kids[c] );
		recursiveInsert( tm, c, new_node, knode );
	    }
	}

	tm.insertNodeInto( new_node, parent, pos );
	
	rebuildTreeNode( parent );
    }

    private void deleteNode(  )
    {
	TreePath tp = command_tree.getSelectionPath();
	if(tp == null)
	    return;

	DefaultTreeModel tm = (DefaultTreeModel) command_tree.getModel();
	
	CustomMutableTreeNode dmtn = (CustomMutableTreeNode) tp.getLastPathComponent();
	
	CustomMenuCommandTreeNode node = (CustomMenuCommandTreeNode) dmtn.getUserObject();
	   
	CustomMutableTreeNode parent = (CustomMutableTreeNode) dmtn.getParent();
		
	if(node.isMenu() == false)
	{
	    // this is a Command node

	    if(mview.infoQuestion("Really delete the Command '" + node.name + "' ?", "Yes", "No") == 0)
	    {
		tm.removeNodeFromParent( dmtn );
		rebuildTreeNode( parent );
	    }
	}
	else
	{
	    // this is a menu node

	    if( (node.getNumChildren() == 0) || 
		(mview.infoQuestion("Really delete the Menu '" + node.name + "' ?", "Yes", "No") == 0) )
	    {
		tm.removeNodeFromParent( dmtn );
		
		rebuildTreeNode( parent );
	    }
	}
    }
}
