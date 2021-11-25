import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.io.*;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import javax.swing.border.*;

// --- --- --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
// --- --- --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
//
// new design for handling Names & Attrs
//
//
// things user might want to do:
//
//   copy Name/Attr to other Name/Attr 
//    
//   save all Name/Attr (optionally making list unique)
//
//   load Attr based on Name/Attr (using a map file)
// 
//   save Attr indexed by Name/Attr (optionally making list unique)
//
//   translate Name/Attr (using a map file)
//
//   replace Name/Attr (for hiding identifiers)
//
//
// --- --- --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
// --- --- --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

public class NameMunger implements Plugin
{
    public NameMunger(maxdView mview_)
    {
	mview = mview_;
	edata = mview.getExprData();
	anlo = mview.getAnnotationLoader();
    }

    public void cleanUp()
    {
	saveProps(mode);
	//edata.removeObserver(this);
	if(frame != null)
	    frame.setVisible(false);
    }


    public void saveProps(int mode)
    {
	mview.putIntProperty("NameMunger.mode", mode);
	mview.putBooleanProperty("NameMunger.filter", apply_filter_jchkb.isSelected() );

	switch(mode)
	{
	case 0:
	    mview.putBooleanProperty("NameMunger.copy_nulls", copy_nulls_jchkb.isSelected() );
	    break;

	case 1:
	    mview.putBooleanProperty("NameMunger.translate_direct", translate_direct_jrb.isSelected() );
	    mview.putBooleanProperty("NameMunger.translate_file",   translate_file_jrb.isSelected() );
	    mview.putBooleanProperty("NameMunger.translate_substitute",  translate_subst_jrb.isSelected() );
	    mview.putProperty("NameMunger.file",  file_jtf.getText());
	    mview.putProperty("NameMunger.translate_from",  translate_from_jtf.getText());
	    mview.putProperty("NameMunger.translate_to",  translate_to_jtf.getText());
	    mview.putProperty("NameMunger.translate_srch",  translate_srch_jtf.getText());
	    mview.putProperty("NameMunger.translate_rplc",  translate_rplc_jtf.getText());
	    mview.putBooleanProperty("NameMunger.translate_upper",  translate_upper_jrb.isSelected() );
	    mview.putBooleanProperty("NameMunger.translate_lower",  translate_lower_jrb.isSelected() );
	    break;

	case 2:
	    mview.putProperty("NameMunger.src_col",  src_col_jtf.getText());
	    mview.putProperty("NameMunger.dest_col", dest_col_jtf.getText());
	    mview.putProperty("NameMunger.file",  file_jtf.getText());
	    break;

	case 3:
	    mview.putProperty("NameMunger.file",  file_jtf.getText());
	    mview.putBooleanProperty("NameMunger.make_unique", unique_jchkb.isSelected() );
	    break;

	case 4:
	    mview.putBooleanProperty("NameMunger.set_blank", blank_val_jrb.isSelected() );
	    mview.putBooleanProperty("NameMunger.set_clear", clear_val_jrb.isSelected() );
	    mview.putBooleanProperty("NameMunger.set_to",    set_val_jrb.isSelected() );
	    mview.putProperty("NameMunger.set_filter",       set_val_jtf.getText());
	    break;
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
	addComponents();
	frame.pack();
	frame.setVisible(true);
    }

    public void stopPlugin()
    {
	cleanUp();
    }
  
    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = new PluginInfo("Name Munger", "transform", 
							"Manipulate Gene, Probe and Spot names", "",
							1, 0, 0);
	return pinf;
    }

    public PluginCommand[] getPluginCommands()
    {
	String[] args;
	
	PluginCommand[] com = new PluginCommand[7];

	com[0] = new PluginCommand("start", null); 
	com[1] = new PluginCommand("stop", null); 

	args = new String[] 
	{ 
	    // name          // type       //default         // flag         // comment
	    "from",          "string",     "",               "m",           "source Name or Name Attribute",
	    "to",            "string",     "",               "m",           "destination Name or Name Attribute",
	    "copy_nulls",    "boolean",    "false",          "",            "",
	    "report_status", "boolean",    "true",           "",            "show either success or failure message after saving",
	    "apply_filter",  "boolean",    "false",          "" ,           ""
	};
	com[2] = new PluginCommand("copy", args);


	args = new String[] 
	{ 
	    // name          // type   //default  // flag  // comment
 
	    "translate",     "string",  "",    	  "m",     "which Name or Name Attribute to modify",
	    "mode",          "string",  "",    	  "m",     "one of:'using_file', 'from_to', 'substitute', 'to_upper' or 'to_lower'",
	       
	    "file",          "file",    "",    	  "",      "when 'mode' = 'file'",
	       
	    "from",          "string",  "",    	  "",      "when 'mode' = 'from_to'",
	    "to",            "string",  "",    	  "",      "when 'mode' = 'from_to'",
   
	    "substitute",    "string",  "",    	  "",      " when 'mode' = 'substitute'",
	    "with",          "string",  "",    	  "",      " when 'mode' = 'substitute'",

	    "report_status", "boolean", "true",   "",      "show either success or failure message after saving",
	    "apply_filter",  "boolean", "false",  "",      ""
	};
	com[3] = new PluginCommand("translate", args);


	args = new String[] 
	{ 
	    // name           // type     //default     // flag   // comment

	    "load",          "string",    "",          "m",      "which Name or Name Attribute to load",
	    "from_column",   "integer",   "",          "m",      "",
	    "of_file",       "file",      "",          "m",      "",
	    "indexed_by",    "string",    "",          "m",      "",
	    "in_column",     "integer",   "",          "m",      "",
	    "report_status", "boolean",   "true",      "",       "",
	    "apply_filter",  "boolean",   "false",     "",       ""
	};
	com[4] = new PluginCommand("load", args);


	args = new String[] 
	{ 
	    // name           // type       //default   // flag   // comment

	    "save",            "string",    "",          "m",      "which Name or Name Attribute to save",
	    "to_file",         "file",      "",          "m",      "destination file name", 
	    "indexed_by",      "string",    "",          "m",      "", 
	    "make_unique",     "boolean",   "false",     "",       "", 
	    "force_overwrite", "boolean",   "false",     "",       "overwrite file of the same name if present", 
	    "report_status",   "boolean",   "true",      "",       "show either success or failure message after execution", 
	    "apply_filter",    "boolean",   "false",     "",       "",
	};
	com[5] = new PluginCommand("save", args);


	args = new String[] 
	{ 
	    // name           // type       //default   // flag   // comment

	    "in",             "string",      "",          "m",      "which Name or Name Attribute to clear",
	    "mode",           "string",      "",          "m",      "one of: 'empty', 'blank' or 'value'",
	    "value",          "string",      "",          "",       "used when 'mode'='value'",
	    "report_status",  "boolean",     "true",      "",       "show either success or failure message after execution",
	    "apply_filter",   "boolean",     "false",     "",       ""
	};
	com[6] = new PluginCommand("clear", args);


	return com;
    }

    public void   runCommand(String name, String[] args, CommandSignal done) 
    { 
	boolean started_this_time = false;

	String bad = null;

	// ===============================================

	if(name.equals("copy"))
	{
	    if(frame == null)
	    {
		startPlugin();
		started_this_time = true;
	    }

	    makeCopyPanel();

	    apply_filter_jchkb.setSelected( mview.getPluginBooleanArg("apply_filter", args, false) );
	    report_status = mview.getPluginBooleanArg("report_status", args, false);

	    ExprData.NameTagSelection nts = mview.getPluginNameTagSelectionArg("from", args, null);
	    if(nts != null)
		src_nts.setNameTagSelection( nts );
	    else
		bad = "No 'from' argument specified";

	    nts = mview.getPluginNameTagSelectionArg("to", args, null);
	    if(nts != null)
		dest_nts.setNameTagSelection( nts );
	    else
		bad = "No 'to' argument specified";
	    
	    copy_nulls_jchkb.setSelected( mview.getPluginBooleanArg("copy_nulls", args, true) );
	    
	    if(bad == null)
	    {
		doCopy(false);
		edata.generateDataUpdate(ExprData.NameChanged);
	    }
	    else
		mview.alertMessage(bad);
	}
	
	// ===============================================

	if(name.equals("translate"))
	{
	    if(frame == null)
	    {
		startPlugin();
		started_this_time = true;
	    }

	    makeTranslatePanel();

	    apply_filter_jchkb.setSelected( mview.getPluginBooleanArg("apply_filter", args, false) );
	    report_status = mview.getPluginBooleanArg("report_status", args, false);
	   
	    ExprData.NameTagSelection nts = mview.getPluginNameTagSelectionArg("translate", args, null);
	    if(nts != null)
		dest_nts.setNameTagSelection( nts );
	    else
		bad = "No 'translate' argument specified";
	    
	    String mode = mview.getPluginStringArg("mode", args, null);
	    if(mode == null)
		bad = "No 'mode' argument specified";
	    else
	    {
		if(mode.startsWith("us"))
		{
		    translate_file_jrb.setSelected(true);

		    file_jtf.setText(  mview.getPluginStringArg("file", args, null) );
		}
		if(mode.startsWith("fro"))
		{
		    translate_direct_jrb.setSelected(true);

		    translate_from_jtf.setText( mview.getPluginStringArg("from", args, null) );
		    translate_to_jtf.setText( mview.getPluginStringArg("to", args, null) );
		}
		if(mode.startsWith("to_u"))
		{
		    translate_upper_jrb.setSelected(true);
		}
		if(mode.startsWith("to_l"))
		{
		    translate_lower_jrb.setSelected(true);
		}
		if(mode.startsWith("subst"))
		{
		    translate_subst_jrb.setSelected(true);
		    
		    translate_srch_jtf.setText( mview.getPluginStringArg("substitute", args, null) );
		    translate_rplc_jtf.setText( mview.getPluginStringArg("with", args, null) );
		}
	    }

	    if(bad == null)
	    {
		doTranslate(false);
		edata.generateDataUpdate(ExprData.NameChanged);
	    }
	    else
		mview.alertMessage(bad);
	}

	// ===============================================


	if(name.equals("load"))
	{
	    if(frame == null)
	    {
		startPlugin();
		started_this_time = true;
	    }

	    makeLoadPanel();

	    apply_filter_jchkb.setSelected( mview.getPluginBooleanArg("apply_filter", args, false) );
	    report_status = mview.getPluginBooleanArg("report_status", args, false);
	   
	    ExprData.NameTagSelection nts = mview.getPluginNameTagSelectionArg("load", args, null);
	    if(nts != null)
		dest_nts.setNameTagSelection( nts );
	    else
		bad = "No 'load' argument specified";
	    
	    dest_col_jtf.setText(mview.getPluginArg("from_column", args, null) );
	    
	    file_jtf.setText( mview.getPluginArg("of_file", args, null) );
    
	    nts = mview.getPluginNameTagSelectionArg("indexed_by", args, null);
	    if(nts != null)
		src_nts.setNameTagSelection( nts );
	    else
		bad = "No 'indexed_by' argument specified";

	    src_col_jtf.setText( mview.getPluginArg("in_column", args, null) );
    
	    if(bad == null)
	    {
		doLoad(false);
		edata.generateDataUpdate(ExprData.NameChanged);
	    }
	    else
		mview.alertMessage(bad);
	}

	// ===============================================


	if(name.equals("save"))
	{
	    if(frame == null)
	    {
		startPlugin();
		started_this_time = true;
	    }

	    makeSavePanel();

	    apply_filter_jchkb.setSelected( mview.getPluginBooleanArg("apply_filter", args, false) );
	    report_status = mview.getPluginBooleanArg("report_status", args, false);
	   
	    ExprData.NameTagSelection nts = mview.getPluginNameTagSelectionArg("save", args, null);
	    if(nts != null)
		dest_nts.setNameTagSelection( nts );
	    else
		bad = "No 'save' argument specified";
	    
	    file_jtf.setText( mview.getPluginArg("to_file", args, null) );

	    nts = mview.getPluginNameTagSelectionArg("indexed_by", args, null);
	    if(nts != null)
		src_nts.setNameTagSelection( nts );
	    else
		bad = "No 'indexed_by' argument specified";

	    unique_jchkb.setSelected(mview.getPluginBooleanArg("make_unique", args, false) );
	    
	    if(bad == null)
		doSave(false);
	    else
		mview.alertMessage(bad);
	}

	// ===============================================

	if(name.equals("clear"))
	{
	    if(frame == null)
	    {
		startPlugin();
		started_this_time = true;
	    }

	    makeClearPanel();

	    apply_filter_jchkb.setSelected( mview.getPluginBooleanArg("apply_filter", args, false) );
	    report_status = mview.getPluginBooleanArg("report_status", args, false);

	    ExprData.NameTagSelection nts = mview.getPluginNameTagSelectionArg("in", args, null);
	    if(nts != null)
		dest_nts.setNameTagSelection( nts );
	    else
		bad = "No 'in' argument specified";
	    
	    String mode = mview.getPluginStringArg("mode", args, null);

	    if(mode == null)
	       bad = "No 'mode' argument specified";
	    else
	    {
		if(mode.equals("empty"))
		{
		    clear_val_jrb.setSelected(true);
		}
		if(mode.equals("blank"))
		{
		    blank_val_jrb.setSelected(true);
		}
		if(mode.equals("value"))
		{
		    set_val_jrb.setSelected(true);

		    set_val_jtf.setText( mview.getPluginStringArg("value", args, null ));
		}
	    }

	    if(bad == null)
	    {
		doClear(false);
		edata.generateDataUpdate(ExprData.NameChanged);
	    }
	    else
		mview.alertMessage(bad);
	}

	// ===============================================

	if(started_this_time)
	    cleanUp();

	if(done != null)
	    done.signal();
    } 

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  gooey
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private void addComponents()
    {
	frame = new JFrame("Name Munger");
	
	mview.decorateFrame( frame );

	frame.addWindowListener(new WindowAdapter() 
			  {
			      public void windowClosing(WindowEvent e)
			      {
				  cleanUp();
			      }
			  });

	GridBagLayout gridbag = new GridBagLayout();
	JPanel panel = new JPanel();
	panel.setBorder(BorderFactory.createEmptyBorder(10,10,0,10));
	panel.setLayout(gridbag);
	panel.setPreferredSize(new Dimension(500, 350));
	GridBagConstraints c = null;

	int line = 0;

	
	//src_col = mview.getIntProperty("NameMunger.src_col", 1);
	//dest_col = mview.getIntProperty("NameMunger.dest_col", 2);
	int initial_mode = mview.getIntProperty( "NameMunger.mode", 0 );

	// ==============================================================================
	//   m o d e    b u t t o n s
	// ==============================================================================

	{
	    JPanel mode_wrap = new JPanel();
	    TitledBorder title = BorderFactory.createTitledBorder(" Mode ");
	    mode_wrap.setBorder(title);

	    GridBagLayout w_gridbag = new GridBagLayout();
	    mode_wrap.setLayout(w_gridbag);

	    ButtonGroup bg = new ButtonGroup();

	    JRadioButton jrb = new JRadioButton("Copy");
	    jrb.addActionListener(new ActionListener()
		{
		    public void actionPerformed(ActionEvent e)
		    {
			setMode(0);
		    }
		});
	    jrb.setSelected(initial_mode == 0);
	    bg.add(jrb);
	    mode_wrap.add(jrb);
	    c = new GridBagConstraints();
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints(jrb, c);

	    jrb = new JRadioButton("Translate");
	    jrb.setSelected(initial_mode == 1);
	    bg.add(jrb);
	    jrb.addActionListener(new ActionListener()
		{
		    public void actionPerformed(ActionEvent e)
		    {
			setMode(1);
		    }
		});
	    mode_wrap.add(jrb);
	    c = new GridBagConstraints();
	    c.gridy = 1;
	    c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints(jrb, c);

	    jrb = new JRadioButton("Load");
	    jrb.setSelected(initial_mode == 2);
	    jrb.addActionListener(new ActionListener()
		{
		    public void actionPerformed(ActionEvent e)
		    {
			setMode(2);
		    }
		});
	    bg.add(jrb);
	    mode_wrap.add(jrb);
	    c = new GridBagConstraints();
	    c.gridy = 2;
	    c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints(jrb, c);

	    jrb = new JRadioButton("Save");
	    jrb.setSelected(initial_mode == 3);
	    jrb.addActionListener(new ActionListener()
		{
		    public void actionPerformed(ActionEvent e)
		    {
			setMode(3);
		    }
		});
	    bg.add(jrb);
	    mode_wrap.add(jrb);
	    c = new GridBagConstraints();
	    c.gridy = 3;
	    c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints(jrb, c);

	    jrb = new JRadioButton("Clear/Set");
	    jrb.setSelected(initial_mode == 4);
	    jrb.addActionListener(new ActionListener()
		{
		    public void actionPerformed(ActionEvent e)
		    {
			setMode(4);
		    }
		});
	    bg.add(jrb);
	    mode_wrap.add(jrb);
	    c = new GridBagConstraints();
	    c.gridy = 4;
	    c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints(jrb, c);

	    Dimension fillsize = new Dimension(20,20);
	    Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
	    c = new GridBagConstraints();
	    c.gridy = 5;
	    w_gridbag.setConstraints(filler, c);
	    mode_wrap.add(filler);

	    apply_filter_jchkb = new JCheckBox("Apply filter");
	    apply_filter = mview.getBooleanProperty("NameMunger.filter", false);
	    apply_filter_jchkb.setSelected(apply_filter);
	    c = new GridBagConstraints();
	    c.gridy = 6;
	    c.anchor = GridBagConstraints.SOUTH;
	    w_gridbag.setConstraints(apply_filter_jchkb, c);
	    mode_wrap.add(apply_filter_jchkb);

	    // ---------

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weighty = 1.0;
	    // c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.VERTICAL;
	    //c.anchor = GridBagConstraints.SOUTH;
	    gridbag.setConstraints(mode_wrap, c);
	    panel.add(mode_wrap);
	}

	// ==============================================================================
	//   m o d e   c o n t r o l s
	// ==============================================================================

	{
	    JPanel mode_wrap = new JPanel();
	    TitledBorder title = BorderFactory.createTitledBorder(" Options ");
	    mode_wrap.setBorder(title);
	    GridBagLayout w_gridbag = new GridBagLayout();
	    mode_wrap.setLayout(w_gridbag);
	    
	    mode_panel = new JPanel();

	    setMode( initial_mode );

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weighty = 1.0;
	    c.weightx = 1.0;
	    c.gridwidth = 2;
	    c.fill = GridBagConstraints.BOTH;
	    //c.anchor = GridBagConstraints.SOUTH;
	    w_gridbag.setConstraints(mode_panel, c);
	    mode_wrap.add(mode_panel);
	    
	    // ---------

	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    c.weighty = 1.0;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.EAST;
	    c.fill = GridBagConstraints.BOTH;
	    //c.anchor = GridBagConstraints.SOUTH;
	    gridbag.setConstraints(mode_wrap, c);
	    panel.add(mode_wrap);
	}

	// ==============================================================================
	//   c o n t r o l   b u t t o n s
	// ==============================================================================


	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrapper.setLayout(w_gridbag);
	    wrapper.setBorder(BorderFactory.createEmptyBorder(10,0,5,0));

	    
	    {
		JButton jb = new JButton("Check");
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    checkOperation();
			}
		    });
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		w_gridbag.setConstraints(jb, c);
		wrapper.add(jb);
		
	    }

	    addFiller( wrapper, w_gridbag, 0, 1, 16 );

	    {
		JButton jb = new JButton("Apply");
		jb.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    doOperation();
			}
		    });
		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 0;
		w_gridbag.setConstraints(jb, c);
		wrapper.add(jb);
	    }
	    
	    addFiller( wrapper, w_gridbag, 0, 3, 16 );
	    addFiller( wrapper, w_gridbag, 0, 4, 16 );
	    addFiller( wrapper, w_gridbag, 0, 5, 16 );

	    {
		JButton button = new JButton("Close");
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    cleanUp();
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 6;
		c.gridy = 0;
		w_gridbag.setConstraints(button, c);
		wrapper.add(button);
	    }

	    addFiller( wrapper, w_gridbag, 0, 7, 16 );

	    {
		JButton button = new JButton("Help");
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    switch(mode)
			    {
			    case 0:
				mview.getPluginHelpTopic("NameMunger", "NameMunger", "#copy");
				break;
			    case 1:
				mview.getPluginHelpTopic("NameMunger", "NameMunger", "#trans");
				break;
			    case 2:
				mview.getPluginHelpTopic("NameMunger", "NameMunger", "#load");
				break;
			    case 3:
				mview.getPluginHelpTopic("NameMunger", "NameMunger", "#save");
				break;
			    case 4:
				mview.getPluginHelpTopic("NameMunger", "NameMunger", "#clear");
				break;
			    }
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 8;
		c.gridy = 0;
		w_gridbag.setConstraints(button, c);
		wrapper.add(button);
	    }
	    panel.add(wrapper);
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 2;
	    c.gridwidth = 2;
	    // c.weighty = 1.0;
	    c.weightx = 2.0;
	    c.fill = GridBagConstraints.BOTH;
	    //c.anchor = GridBagConstraints.SOUTH;
	    gridbag.setConstraints(wrapper, c);
	    
	}

	frame.getContentPane().add(panel);
    }

    private void checkOperation()
    {
	apply_filter = apply_filter_jchkb.isSelected();

	switch(mode)
	{
	case 0:
	    doCopy(true);
	    break;
	case 1:
	    doTranslate(true);
	    break;
	case 2:
	    doLoad(true);
	    break;
	case 3:
	    doSave(true);
	    break;
	case 4:
	    doClear(true);
	    break;
	}
    }

    private void doOperation()
    {
	apply_filter = apply_filter_jchkb.isSelected();

	switch(mode)
	{
	case 0:
	    doCopy(false);
	    break;
	case 1:
	    doTranslate(false);
	    break;
	case 2:
	    doLoad(false);
	    break;
	case 3:
	    doSave(false);
	    break;
	case 4:
	    doClear(false);
	    break;
	}
	edata.generateDataUpdate(ExprData.NameChanged);
    }

    private void setMode(int mode_)
    {
	System.out.println("old mode=" + mode + " new mode=" + mode_);
	
	if(mode >= 0)
	    saveProps(mode);

	mode = mode_;
	JPanel panel = null;

	// System.out.println("mode=" + mode);

	switch(mode)
	{
	case 0:
	    panel = makeCopyPanel();
	    break;
	case 1:
	    panel = makeTranslatePanel();
	    break;
	case 2:
	    panel = makeLoadPanel();
	    break;
	case 3:
	    panel = makeSavePanel();
	    break;
	case 4:
	    panel = makeClearPanel();
	    break;
	}
	
	if(panel != null)
	{
	    mode_panel.removeAll();
	    
	    GridBagLayout gridbag = new GridBagLayout();
	    mode_panel.setLayout(gridbag);
	    
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weighty = 1.0;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.BOTH; // HORIZONTAL;
	    gridbag.setConstraints(panel, c);
	    mode_panel.add(panel);

	    Dimension fillsize = new Dimension(20,20);
	    Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    gridbag.setConstraints(filler, c);
	    mode_panel.add(filler);

	    
	    panel.updateUI();      // causes a repaint...
	}

    }
    
    // shared GUI things
    
    private boolean apply_filter;
    private JCheckBox apply_filter_jchkb;

    private NameTagSelector src_nts;
    private NameTagSelector dest_nts;
    private JTextField file_jtf;
    private JCheckBox unique_jchkb;
    private JCheckBox copy_nulls_jchkb;

    private JTextField src_col_jtf;
    private JTextField dest_col_jtf;

    private JRadioButton translate_subst_jrb;
    private JRadioButton translate_direct_jrb;
    private JRadioButton translate_file_jrb;
    private JTextField   translate_from_jtf;
    private JTextField   translate_to_jtf;
    private JTextField   translate_srch_jtf;
    private JTextField   translate_rplc_jtf;

    private JRadioButton translate_upper_jrb;
    private JRadioButton translate_lower_jrb;

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  c o p y   m o d e
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public JPanel makeCopyPanel()
    {
	JPanel panel = new JPanel();
	GridBagLayout gridbag = new GridBagLayout();
	panel.setBorder(BorderFactory.createEmptyBorder(10,10,0,10));
	panel.setLayout(gridbag);
	GridBagConstraints c = null;
	
	JLabel label = new JLabel("Copy from ");
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	gridbag.setConstraints(label, c);
	panel.add(label);

	src_nts = new NameTagSelector(mview);
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 0;
	gridbag.setConstraints(src_nts, c);
	panel.add(src_nts);

	label = new JLabel(" to ");
	c = new GridBagConstraints();
	c.gridx = 2;
	c.gridy = 0;
	gridbag.setConstraints(label, c);
	panel.add(label);

	dest_nts = new NameTagSelector(mview);
	c = new GridBagConstraints();
	c.gridx = 3;
	c.gridy = 0;
	gridbag.setConstraints(dest_nts, c);
	panel.add(dest_nts);

	copy_nulls_jchkb = new JCheckBox("Copy NULL values");
	copy_nulls_jchkb.setSelected( mview.getBooleanProperty("NameMunger.copy_nulls" , false ));
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 2;
	c.gridwidth = 2;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(copy_nulls_jchkb, c);
	panel.add(copy_nulls_jchkb);

	return panel;
    }


    // rewritten 12.7.01 to fixes problems with gene-name-attr copying

    public void doCopy(boolean check_it)
    {
	String[] vals = getValues(src_nts);
	
	ExprData.NameTagSelection dest_nt_sel = dest_nts.getNameTagSelection();
	ExprData.NameTagSelection src_nt_sel  = src_nts.getNameTagSelection();

	String tname = dest_nt_sel.getNames();
	if(tname == null)
	{
	    mview.alertMessage("No Name or Attribute selected");
	    return;
	}
	
	if(dest_nt_sel.equals( src_nt_sel ))
	{
	    mview.alertMessage("Cannot copy something to itself");
	    return;
	}

	if(dest_nt_sel.isSpotName())
	{
	    if(mview.infoQuestion("Spot names should not really be modified\nContinue?", "Yes", "No") == 1)
		return;
	}

   
	final int n_spots = edata.getNumSpots();
	final boolean cnulls = copy_nulls_jchkb.isSelected();


	int lost = 0;
	int copied = 0;

	// can be reading and writing one of three things:
	//
	//  0. gene names
	//  1. gene name attributes
	//  2. anything else


	//
	//  reading: 
	//
	//  0. gene names      --> N srcs/spot
	//  1. gene name attr  --> N srcs/spot
	//  2. anything else   --> 1 src/spot
	//
	//
	//  writing: 
	//
	//  0. gene names      --> N dests/spot
	//  1. gene name attr  --> N dests/spot
	//  2. anything else   --> 1 dest/spot
	//

	//
	// the problem arises when spots have more than one gene, for example:
	//
	//    spot1  gene1 geneZ gtag1.1 gtag1.Z probe1 ptag1
	//    spot2  gene2       gtag1.2         probe2 ptag2
	//    spot3                              probe3
	//    spot4  gene4                       probe4 ptag4
	//    spot5  gene5       gtag1.5         probe4
	//
	// (gtags are numbered like this: TAG_ID.GENE_ID, so gtag1.2 is tag 1 for gene 2)
	//
	// (spot 1 has 2 genes, and thus 2 sets of tags)
	//
	//
	//
	// A. copy  "ptag" -> "gtag1"
	//
	//    gtag1.1 = ptag1, ??  gtag1.Z=ptag1 ??, gtag1.2 = ptag2, ....
	//
	// should all gtags in spot1 get the value of ptag1 ?
	// 
	//
	// B. copy "gtag1" -> "ptag"  
	//
	//    ptag1 = gtag1.1; ?? ptag1 = gtag1.Z??, ptag2 = gtag1.2, ....
	// 
	//  which of the gtags in spot1 is copied to ptag1 ?
	//
	// 
	// C. copy "gtag1" -> "gtag2"
	// 
	//    should be ok, iterate over gene names for each spot
	// 
	//
	// D. copy "pname" -> "gtag1"
	//
	//    gtag1.1 = pname1, gtag1.Z=pname1, gtag1.2 = pname2, ....
	//
	//    ok,
	//
	//
	// E. copy "gtag1" -> pname
	//
	//    pname1 = gtag1.1, ?? pname1 = gtag1.Z??, pname2 = gtag1.2, ....
	//
	//    same problem as with B
	//
	//
	// same pattern of problems occur with gname....
	//
	//

	int dest_mode = 2;
	if( dest_nt_sel.isGeneNamesAttr() )
	    dest_mode = 1;
	if( dest_nt_sel.isGeneNames() )
	    dest_mode = 0;

	int src_mode = 2;
	if( src_nt_sel.isGeneNamesAttr() )
	    src_mode = 1;
	if( src_nt_sel.isGeneNames() )
	    src_mode = 0;

	System.out.println(src_mode + " -> " + dest_mode);
	
	ExprData.DataTags dtags = edata.getMasterDataTags();

	String[] src;
	

	int bad = 0;

	for(int spot_id=0; spot_id < n_spots; spot_id++)
	{
	    if(!apply_filter || !edata.filter(spot_id))
	    {
		switch(src_mode)
		{
		case 0:
		    src = edata.getGeneNames(spot_id);
		    break;
		case 1:
		    src = src_nt_sel.getFullNameTagArray( spot_id ); // ask for missing values to be included
		    break;
		default:
		    String[] singleton = new String[1];
		    singleton[0] = src_nt_sel.getNameTag( spot_id );
		    src = singleton;
		    break;
		}

		switch(dest_mode)
		{
		case 0: // gene names

		    String[] old = edata.getGeneNames(spot_id);
		    int n_old = (old == null) ? 0 : old.length;
		    
		    if((src == null) || (src.length == 0))
		    {
			if(cnulls)
			{
			    lost += n_old;
			    copied+=1;

			    if(!check_it)
				dtags.setGeneNames(spot_id, null);
			}
		    }
		    else
		    {
			lost   += n_old;
			copied += src.length;

			if(!check_it)
			{
			    dtags.setGeneNames(spot_id, src );
			}
		    }
		    break;
		    
		case 1: // gene name attrs

		    String[] gn = edata.getGeneNames(spot_id);
		    int ngn = (gn == null) ? 0 : gn.length;
		   
		    for(int g=0; g < ngn; g++)
		    {
			if(gn[g] != null)
			{
			    String real_src = (src == null) ? null : ((g < src.length) ? src[g] : src[0]);
			    
			    if(cnulls || (real_src != null))
			    {
				if( dest_nt_sel.getGeneNameTag(spot_id, g) != null)
				    lost++;
				
				copied++;
				
				if(!check_it)
				    dest_nt_sel.setGeneNameTag(gn[g], real_src, spot_id);
			    }
			}
		    }
		    break;

		case 2: // anything else

		    
		    if((src == null) || (src.length < 2))
		    {
			boolean skip = true;
			String val = null;
			
			if(src == null || (src.length == 0))
			{
			    if(cnulls)
			    {
				skip =false;
			    }
			}
			else
			{
			    if(cnulls || (src[0] != null))
			    {
				skip = false;
				val = src[0];
			    }
			}

			if(!skip)
			{
			    if(dest_nt_sel.getNameTag( spot_id ) != null)
				lost++;
			    
			    copied++;
			    
			    if(!check_it)
				dest_nt_sel.setNameTag( val, spot_id);
			}

		    }
		    else
		    {
			// more than one value for source, but only one destination...
			
			bad++;
		    }
		}
	    }
	}

	
	String msg = copied + " value";
	if(copied != 1)
	    msg += "s";
	if(check_it)
	    msg += " will be";
	msg += " copied, ";

	msg += lost + " old value";
	if(lost != 1)
	    msg += "s";
	if(check_it)
	    msg += " will be";
	
	msg += " lost from '" + dest_nts.getNameTagSelection().getNames() + "'";
	
	if(bad > 0)
	{
	    msg += "\n\nWarning:\n  " + ((bad==1) ? "1 Spot" : (bad + " Spots")) +
	    	   " had multiple source values but only a single destination\n" + 
	    	   "\n  This is not yet supported, so these values " + 
	    	   (check_it ? "will not be" : "were not") + " copied";
	}
	
	if(check_it || report_status)
	    mview.infoMessage(msg);
    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  t r a n s l a t e   m o d e
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public JPanel makeTranslatePanel()
    {
	JPanel panel = new JPanel();
	GridBagLayout gridbag = new GridBagLayout();
	panel.setBorder(BorderFactory.createEmptyBorder(10,10,0,10));
	panel.setLayout(gridbag);
	GridBagConstraints c = null;
	
	JLabel label = new JLabel("Translate ");
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	c.anchor = GridBagConstraints.EAST;
	gridbag.setConstraints(label, c);
	panel.add(label);

	dest_nts = new NameTagSelector(mview);
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 0;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(dest_nts, c);
	panel.add(dest_nts);
	    

	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout wrapbag = new GridBagLayout();
	    wrapper.setLayout(wrapbag);
	    
	    
	    translate_file_jrb = new JRadioButton();
	    translate_file_jrb.setSelected( mview.getBooleanProperty("NameMunger.translate_file", true));
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.EAST;
	    wrapbag.setConstraints(translate_file_jrb, c);
	    wrapper.add(translate_file_jrb);

	    label = new JLabel("using file ");
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.WEST;
	    wrapbag.setConstraints(label, c);
	    wrapper.add(label);
	    
	    file_jtf = new JTextField(20);
	    file_jtf.setText(mview.getProperty("NameMunger.file", ""));
	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = 0;
	    c.weightx = 8.0;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.BOTH;
	    wrapbag.setConstraints(file_jtf, c);
	    wrapper.add(file_jtf);
	    
	    addFiller( wrapper, wrapbag, 0, 3, 8 );

	    JButton jb = new JButton("Browse");
	    jb.setFont(mview.getSmallFont());
	    jb.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			JFileChooser fc = mview.getFileChooser();
			fc.setCurrentDirectory( new File( file_jtf.getText() ) );
			if(fc.showDialog(frame, "Select") == JFileChooser.APPROVE_OPTION)
			{
			    file_jtf.setText(fc.getSelectedFile().getPath());
			}
		    }
		});
	    
	    c = new GridBagConstraints();
	    c.gridx = 4;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.WEST;
	    wrapbag.setConstraints(jb, c);
	    wrapper.add(jb);

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.gridwidth = 2;
	    c.weightx = 9.0;
	    c.weighty = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    gridbag.setConstraints(wrapper, c);
	    panel.add(wrapper);
	}
	
	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout wrapbag = new GridBagLayout();
	    wrapper.setLayout(wrapbag);
	    
	    translate_direct_jrb = new JRadioButton();
	    translate_direct_jrb.setSelected( mview.getBooleanProperty("NameMunger.translate_direct", false));
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.EAST;
	    wrapbag.setConstraints(translate_direct_jrb, c);
	    wrapper.add(translate_direct_jrb);
	    
	    label = new JLabel("from  ");
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    //c.anchor = GridBagConstraints.WEST;
	    wrapbag.setConstraints(label, c);
	    wrapper.add(label);
	    
	    translate_from_jtf = new JTextField(10);
	    translate_from_jtf.setText( mview.getProperty("NameMunger.translate_from", ""));
	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    //c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.BOTH;
	    wrapbag.setConstraints(translate_from_jtf, c);
	    wrapper.add(translate_from_jtf);
	    
	    label = new JLabel(" to ");
	    c = new GridBagConstraints();
	    c.gridx = 3;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.WEST;
	    wrapbag.setConstraints(label, c);
	    wrapper.add(label);
	    
	    translate_to_jtf = new JTextField(10);
	    translate_to_jtf.setText( mview.getProperty("NameMunger.translate_to", ""));
	    c = new GridBagConstraints();
	    c.gridx = 4;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    //c.fill = GridBagConstraints.BOTH;
	    wrapbag.setConstraints(translate_to_jtf, c);
	    wrapper.add(translate_to_jtf);
	    
	    
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 2;
	    c.gridwidth = 2;
	    c.weightx = 9.0;
	    c.weighty = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    gridbag.setConstraints(wrapper, c);
	    panel.add(wrapper);
	}

	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout wrapbag = new GridBagLayout();
	    wrapper.setLayout(wrapbag);
	    
	    translate_upper_jrb = new JRadioButton();
	    translate_upper_jrb.setSelected( mview.getBooleanProperty("NameMunger.translate_upper", false));
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.EAST;
	    wrapbag.setConstraints(translate_upper_jrb, c);
	    wrapper.add(translate_upper_jrb);

	    label = new JLabel("to upper case ");
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    wrapbag.setConstraints(label, c);
	    wrapper.add(label);
	
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 3;
	    c.gridwidth = 2;
	    c.weightx = 9.0;
	    c.weighty = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    gridbag.setConstraints(wrapper, c);
	    panel.add(wrapper);
	}
	
	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout wrapbag = new GridBagLayout();
	    wrapper.setLayout(wrapbag);
	    
	    translate_lower_jrb = new JRadioButton();
	    translate_lower_jrb.setSelected( mview.getBooleanProperty("NameMunger.translate_lower", false));
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.EAST;
	    wrapbag.setConstraints(translate_lower_jrb, c);
	    wrapper.add(translate_lower_jrb);

	    label = new JLabel("to lower case ");
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    wrapbag.setConstraints(label, c);
	    wrapper.add(label);
	
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 4;
	    c.gridwidth = 2;
	    c.weightx = 9.0;
	    c.weighty = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    gridbag.setConstraints(wrapper, c);
	    panel.add(wrapper);
	}

	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout wrapbag = new GridBagLayout();
	    wrapper.setLayout(wrapbag);
	    
	    translate_subst_jrb = new JRadioButton();
	    translate_subst_jrb.setSelected( mview.getBooleanProperty("NameMunger.translate_substitute", false));
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.EAST;
	    wrapbag.setConstraints(translate_subst_jrb, c);
	    wrapper.add(translate_subst_jrb);
	    
	    label = new JLabel("substitute ");
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    //c.anchor = GridBagConstraints.WEST;
	    wrapbag.setConstraints(label, c);
	    wrapper.add(label);
	    
	    translate_srch_jtf = new JTextField(10);
	    translate_srch_jtf.setText( mview.getProperty("NameMunger.translate_srch", ""));
	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    //c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.BOTH;
	    wrapbag.setConstraints(translate_srch_jtf, c);
	    wrapper.add(translate_srch_jtf);
	    
	    label = new JLabel(" with ");
	    c = new GridBagConstraints();
	    c.gridx = 3;
	    c.gridy = 0;
	    c.anchor = GridBagConstraints.WEST;
	    wrapbag.setConstraints(label, c);
	    wrapper.add(label);
	    
	    translate_rplc_jtf = new JTextField(10);
	    translate_rplc_jtf.setText( mview.getProperty("NameMunger.translate_rplc", ""));
	    c = new GridBagConstraints();
	    c.gridx = 4;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    //c.fill = GridBagConstraints.BOTH;
	    wrapbag.setConstraints(translate_rplc_jtf, c);
	    wrapper.add(translate_rplc_jtf);
	    
	    
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 5;
	    c.gridwidth = 2;
	    c.weightx = 9.0;
	    c.weighty = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    gridbag.setConstraints(wrapper, c);
	    panel.add(wrapper);
	}


	ButtonGroup bg = new ButtonGroup();
	bg.add(translate_direct_jrb);
	bg.add(translate_subst_jrb);
	bg.add(translate_file_jrb);
	bg.add(translate_upper_jrb);
	bg.add(translate_lower_jrb);
	    
	return panel;
    }

    public void doTranslate(boolean check_it)
    {
	String srch = null;
	String rplc = null;

	int tmode = 0;
	if(translate_direct_jrb.isSelected())
	    tmode = 1;
	if(translate_upper_jrb.isSelected())
	    tmode = 2;
	if(translate_lower_jrb.isSelected())
	    tmode = 3;
	if(translate_subst_jrb.isSelected())
	{
	    tmode = 4;

	    srch = translate_srch_jtf.getText();
	    if((srch != null) && (srch.length() == 0))
		srch = null;
	    rplc = translate_rplc_jtf.getText();
	    if((rplc != null) && (rplc.length() == 0))
		rplc = null;

	    System.out.println("s=" + srch + " r=" + rplc);

	}
	
	String fname = file_jtf.getText();
	if((tmode == 0) && (fname.length() == 0))
	{
	    mview.alertMessage("No file chosen");
	    return;
	}
	

	Hashtable data = (tmode == 0) ? loadTuplesFromFile(fname) : null;
	
	final int n_spots = edata.getNumSpots();
	
	ExprData.NameTagSelection dest_nt_sel = dest_nts.getNameTagSelection();
	String tname = dest_nt_sel.getNames();
	if(tname == null)
	{
	    mview.alertMessage("No Name or Attribute selected");
	    return;
	}
	if(dest_nt_sel.isSpotName())
	{
	    if(mview.infoQuestion("Spot names should not really be modified\nContinue?", "Yes", "No") == 1)
		return;
	}

	int changes = 0;

	final boolean is_gene = dest_nt_sel.isGeneNamesOrAttr();

	if(is_gene)
	{
	    String[][] old_gnames = new String[n_spots][];
	    for(int s=0; s < n_spots; s++)
	    {
		old_gnames[s] = edata.getGeneNames(s);
	    }

	    for(int s=0; s < n_spots; s++)
	    {
		if(!apply_filter || !edata.filter(s))
		{
		    String[] gnames = old_gnames[s];
		    
		    for(int g=0; g < gnames.length; g++)
		    {
			String gname = gnames[g];
			String cur   = dest_nt_sel.getGeneNameTag(s, g);

			String res = null;
			switch(tmode)
			{
			case 0:  // file
			    res = (String) data.get(cur);
			    break;
			    
			case 1:  // direct
			    if(cur != null)
				if(cur.equals( translate_from_jtf.getText()))
				    res = translate_to_jtf.getText();
			    break;
			    
			case 2:  // to upper
			    res = (cur == null) ? null : cur.toUpperCase();
			    break;
			    
			case 3:  // to lower
			    res = (cur == null) ? null : cur.toLowerCase();
			    break;

			case 4:
			    res = substitute(cur, srch, rplc);
			    break;
			}
			
			if(res != null)
			{
			    if(!res.equals(cur))
			    {
				changes++;
				
				if(!check_it)
				    dest_nt_sel.setGeneNameTag( gname, res, s );
			    }
			}
		    }
		}
	    }
	}
	else
	{
	    for(int s=0; s < n_spots; s++)
	    {
		if(!apply_filter || !edata.filter(s))
		{
		    String cur = dest_nt_sel.getNameTag(s);
		    if(cur != null)
		    {
			String res = null;
			switch(tmode)
			{
			case 0:  // file
			    res = (String) data.get(cur);
			    break;
			    
			case 1:  // direct
			    if((cur != null) && (cur.equals( translate_from_jtf.getText())))
				res = translate_to_jtf.getText();
			    break;
			    
			case 2:  // to upper
			    res = (cur == null) ? null : cur.toUpperCase();
			    break;
			    
			case 3:  // to lower
			    res = (cur == null) ? null : cur.toLowerCase();
			    break;

			case 4:
			    res = substitute(cur, srch, rplc);
			    break;
			}
			
			if(res != null)
			{
			    if(!res.equals(cur))
			    {
				changes++;
				
				if(!check_it)
				    dest_nt_sel.setNameTag(res, s);
			    }
			}
		    }
		}
	    }
	}
	
	String msg = changes + " value";
	if(changes != 1)
	    msg += "s";
	if(check_it)
	    msg += " will be";

	if(check_it || report_status)
	    mview.infoMessage(msg + " changed in '" + dest_nt_sel.getNames() + "'");	
    }

    private String substitute( String src, String search, String replace )
    {
	if((src == null) || (src.length() == 0))
	{
	    // src is blank or empty...

	    if(search == null)
		return replace;
	}
	else
	{
	    // src is _not_ blank or empty...

	    if(search == null)
	    {
		return src;
	    }
	    else
	    {
		int init = 0;
		
		int pos = src.indexOf(search);

		if(pos >= 0)
		{
		    final int len = search.length();
		    final int src_l = src.length();
		    StringBuffer res = new StringBuffer();
		    
		    for(int c=0; c < src_l; c++)
		    {
			if( ((c+len) <= src_l) && (search.equals(src.substring(c, (c+len)))) )
			{
			    if(replace != null)
				res.append(replace);
				    
			    c += (len-1);  // c gets incremented at the start of the loop, so need (len-1)
			}
			else
			{
			    res.append(src.charAt(c));
			}
		    }

		    return (res.length() == 0) ? null : res.toString();
		}
	    }
	}
	return null;
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  c l e a r    m o d e
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    private JRadioButton blank_val_jrb, clear_val_jrb, set_val_jrb;
    private JTextField set_val_jtf;

    public JPanel makeClearPanel()
    {
	JPanel panel = new JPanel();
	GridBagLayout gridbag = new GridBagLayout();
	panel.setBorder(BorderFactory.createEmptyBorder(10,10,0,10));
	panel.setLayout(gridbag);
	GridBagConstraints c = null;
	
	JLabel label = new JLabel(" In ");
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	c.anchor = GridBagConstraints.EAST;
	gridbag.setConstraints(label, c);
	panel.add(label);

	dest_nts = new NameTagSelector(mview);
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 0;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(dest_nts, c);
	panel.add(dest_nts);
	    


	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout wrapbag = new GridBagLayout();
	    wrapper.setLayout(wrapbag);
	    
	    addFiller( wrapper, wrapbag, 0 );
	 
	    clear_val_jrb = new JRadioButton();
	    clear_val_jrb.setSelected( mview.getBooleanProperty("NameMunger.set_clear", false) );
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    wrapbag.setConstraints(clear_val_jrb, c);
	    wrapper.add(clear_val_jrb);

	    label = new JLabel("set all values to empty (i.e. null)");
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 1;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    wrapbag.setConstraints(label, c);
	    wrapper.add(label);

	    addFiller( wrapper, wrapbag, 2 );

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.gridwidth = 2;
	    c.weightx = 1.0;
	    //c.weighty = 1.0;
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    gridbag.setConstraints(wrapper, c);
	    panel.add(wrapper);
	}

	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout wrapbag = new GridBagLayout();
	    wrapper.setLayout(wrapbag);
	    
	    blank_val_jrb = new JRadioButton();
	    blank_val_jrb.setSelected( mview.getBooleanProperty("NameMunger.set_blank", false) );
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    wrapbag.setConstraints(blank_val_jrb, c);
	    wrapper.add(blank_val_jrb);

	    label = new JLabel("set all values to blank");
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    wrapbag.setConstraints(label, c);
	    wrapper.add(label);

	    addFiller( wrapper, wrapbag, 1 );

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 2;
	    c.gridwidth = 2;
	    c.weightx = 1.0;
	    //c.weighty = 1.0;
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    gridbag.setConstraints(wrapper, c);
	    panel.add(wrapper);
	}
	
	{
	    JPanel wrapper = new JPanel();
	    GridBagLayout wrapbag = new GridBagLayout();
	    wrapper.setLayout(wrapbag);
	    
	    set_val_jrb = new JRadioButton();
	    set_val_jrb.setSelected( mview.getBooleanProperty("NameMunger.set_to", false) );
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    wrapbag.setConstraints(set_val_jrb, c);
	    wrapper.add(set_val_jrb);
	    
	    label = new JLabel("set all values to ");
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    wrapbag.setConstraints(label, c);
	    wrapper.add(label);
	    
	    set_val_jtf = new JTextField(10);
	    set_val_jtf.setText( mview.getProperty("NameMunger.set_filter", "") );
	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.VERTICAL;
	    wrapbag.setConstraints(set_val_jtf, c);
	    wrapper.add(set_val_jtf);
	    
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 3;
	    c.gridwidth = 2;
	    //c.weightx = 1.0;
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.WEST;
	    gridbag.setConstraints(wrapper, c);
	    panel.add(wrapper);
	}

	ButtonGroup bg = new ButtonGroup();
	bg.add(clear_val_jrb);
	bg.add(blank_val_jrb);
	bg.add(set_val_jrb);

	    
	return panel;
    }

    public void doClear(boolean check_it)
    {
	final int n_spots = edata.getNumSpots();
	
	final boolean clear_vals = clear_val_jrb.isSelected();

	int cmode = 0;  // set mode (format  string)
	if(clear_val_jrb.isSelected())
	    cmode = 1;  // clear mode  (i.e. null)
	if(blank_val_jrb.isSelected())
	    cmode = 2;  // blank (i.e. "")

	String format_str = set_val_jtf.getText();

	ExprData.NameTagSelection dest_nt_sel = dest_nts.getNameTagSelection();
	String tname = dest_nt_sel.getNames();
	if(tname == null)
	{
	    mview.alertMessage("No Name or Attribute selected");
	    return;
	}
	if(dest_nt_sel.isSpotName())
	{
	    if(mview.infoQuestion("Spot names should not really be modified\nContinue?", "Yes", "No") == 1)
		return;
	}

	
	// save the old names prior to translation
	// (in case names are duplicated, in which case the format string will get upset)
	//
	String[] old_names = null;
	if(cmode == 0)
	{
	    boolean is_gene_name = dest_nt_sel.isGeneNames();
	    
	    old_names = new String[n_spots];
	    for(int s=0; s < n_spots; s++)
	    {
		if(!apply_filter || !edata.filter(s))
		{
		    old_names[s] = dest_nt_sel.getNameTag(s);
		}
	    }
	}
	
	int changes = 0;

	final boolean is_gene = dest_nt_sel.isGeneNamesOrAttr();
	
	if(is_gene)
	{
	    System.out.println("is gene or gene attr: saving old names");

	    String[][] old_gnames = new String[n_spots][];
	    for(int s=0; s < n_spots; s++)
	    {
		old_gnames[s] = edata.getGeneNames(s);
	    }

	    for(int s=0; s < n_spots; s++)
	    {
		if(!apply_filter || !edata.filter(s))
		{
		    String[] gnames = old_gnames[s];
		    
		    // no existing name, generate a blank for the time being
		    if(gnames == null)
		    {
			gnames = new String[1];
			gnames[0] = "";
		    }

		    for(int g=0; g < gnames.length; g++)
		    {
			String gname = gnames[g];
			String cur   = dest_nt_sel.getGeneNameTag(s, g);
			
			switch(cmode)
			{
			case 0:  // set
			    String upd = translateFormatString( gname, format_str, s );
			    if((cur == null) || (!cur.equals(upd)))
				changes++;
			    if(!check_it)
				dest_nt_sel.setGeneNameTag( gname, upd, s );
			    break;
			    
			case 1:  // clear
			    if(cur != null)
				changes++;
			    if(!check_it)
			    {
				if(dest_nt_sel.isGeneNames())
				    // this causes findlongestname to be called over and over again!
				    edata.getMasterDataTags().setGeneNames( s, null ); 
				else
				    dest_nt_sel.setGeneNameTag( gname, null, s );
			    }
			    break;
			    
			case 2:  // blank
			    if((cur == null) || (cur.length() > 0))
				changes++;
			    if(!check_it)
				dest_nt_sel.setGeneNameTag( gname, "", s);
			    break;
			}
		    }
		}
	    }
	}
	else
	{
	    for(int s=0; s < n_spots; s++)
	    {
		if(!apply_filter || !edata.filter(s))
		{
		    String[] cur_a = dest_nt_sel.getNameTagArray(s);
		    
		    for(int c=0; c < cur_a.length; c++)
		    {
			String cur = (cmode == 0) ?  old_names[s] : cur_a[c];
			
			switch(cmode)
			{
			case 0:  // set
			    String upd = translateFormatString( cur, format_str, s );
			    if((cur == null) || (!cur.equals(upd)))
				changes++;
			    if(!check_it)
				dest_nt_sel.setNameTag( upd, s );
			    break;
			    
			case 1:  // clear
			    if(cur != null)
				changes++;
			    if(!check_it)
				dest_nt_sel.setNameTag( null, s );
			    break;
			    
			case 2:  // blank
			    if((cur == null) || (cur.length() > 0))
				changes++;
			    if(!check_it)
				dest_nt_sel.setNameTag( "", s);
			    break;
			}
		    }
		}
	    }
	}

	final String[] mode_name = { " set", " cleared", " blanked" };

	String msg = changes + " value";
	if(changes != 1)
	    msg += "s";
	if(check_it)
	    msg += " will be ";
	msg += mode_name[cmode];

	    
	if(check_it || report_status)
	    mview.infoMessage(msg + " in '" + dest_nt_sel.getNames() + "'");	
    }

    
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    //
    // format string format:
    //
    //    $N   existing name
    //    $i   index (counting from 0)
    //    $I   index (counting from 1)
    //    $$   escaped $ char
    //
    //    $4I  index with padding width 4 (eg 0001, 0002 etc)
    //
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    
    private String translateFormatString(final String name, final String format, final int index)
    {
	boolean var_armed = false;
	StringBuffer res = new StringBuffer();
	int var_width = 0;

	if((format == null) || (format.length() == 0))
	    return format;

	for(int c=0; c < format.length(); c++)
	{
	    char ch = format.charAt(c);
	    if(ch == '$')
	    {
		if(var_armed)
		{
		    res.append('$');
		    var_armed = false;
		}
		else
		{
		    var_armed = true;
		}
	    }
	    else
	    {
		if(var_armed)
		{
		    if(Character.isDigit(ch))
		    {
			try
			{
			    var_width = (Integer.valueOf(String.valueOf(ch))).intValue();
			}
			catch(NumberFormatException nfe)
			{
			}
		    }
		    if((ch == 'N') || (ch == 'n'))
		    {
			res.append(name);
			var_armed = false;
		    }
		    if(ch == 'i')
		    {
			res.append(getPaddedNumber(index, var_width));
			var_armed = false;
		    }
		    if(ch == 'I')
		    {
			res.append(getPaddedNumber(index+1, var_width));
			var_armed = false;
		    }
		}
		else
		{
		    res.append(ch);
		}
	    }
	} 
	// System.out.println("index=" + index + " name=" + name + " ---> " +  res.toString());

	return res.toString();
    }

    private final String zero_pad_str = "0000000000000";

    private String getPaddedNumber(int number, int width)
    {
	String number_s = String.valueOf( number );

	if(width > 0)
	{
	    int pad = width - number_s.length();
	    
	    // System.out.println( "id=" + id + " with w=" + width + " needs pad=" + pad);
	    
	    if(pad > 0)
		number_s = (zero_pad_str.substring(0,pad) + number_s);
	}
	return number_s;
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  l o a d    m o d e
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public JPanel makeLoadPanel()
    {
	JPanel panel = new JPanel();
	GridBagLayout gridbag = new GridBagLayout();
	panel.setBorder(BorderFactory.createEmptyBorder(10,10,0,10));
	panel.setLayout(gridbag);
	GridBagConstraints c = null;
	
	int line = 0;

	JLabel label = new JLabel("Using file ");
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line;
	c.anchor = GridBagConstraints.EAST;
	gridbag.setConstraints(label, c);
	panel.add(label);

	file_jtf = new JTextField(20);
	file_jtf.setText(mview.getProperty("NameMunger.file", ""));
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = line;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.WEST;
	c.fill = GridBagConstraints.BOTH;
	gridbag.setConstraints(file_jtf, c);
	panel.add(file_jtf);

	addFiller( panel, gridbag, line, 2, 8 );

	JButton jb = new JButton("Browse");
	jb.setFont(mview.getSmallFont());
	jb.addActionListener(new ActionListener() 
	    { 
		public void actionPerformed(ActionEvent e) 
		{
		    JFileChooser fc = mview.getFileChooser();
		    fc.setCurrentDirectory( new File( file_jtf.getText() ) );
		    if(fc.showDialog(frame, "Select" ) == JFileChooser.APPROVE_OPTION)
		    {
			file_jtf.setText(fc.getSelectedFile().getPath());
		    }
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 3;
	c.gridy = line++;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(jb, c);
	panel.add(jb);

	addFiller( panel, gridbag, line++ );

	label = new JLabel("load ");
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line;
	c.anchor = GridBagConstraints.EAST;
	gridbag.setConstraints(label, c);
	panel.add(label);

	dest_nts = new NameTagSelector(mview);
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = line++;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(dest_nts, c);
	panel.add(dest_nts);

	addFiller( panel, gridbag, line++ );

	label = new JLabel("from column ");
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line;
	c.anchor = GridBagConstraints.EAST;
	gridbag.setConstraints(label, c);
	panel.add(label);

	dest_col_jtf = new JTextField(10);
	dest_col_jtf.setText(mview.getProperty("NameMunger.dest_col", ""));
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = line++;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.WEST;
	c.fill = GridBagConstraints.BOTH;
	gridbag.setConstraints(dest_col_jtf, c);
	panel.add(dest_col_jtf);

	addFiller( panel, gridbag, line++ );

	label = new JLabel("indexed by ");
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line;
	c.anchor = GridBagConstraints.EAST;
	gridbag.setConstraints(label, c);
	panel.add(label);

	src_nts = new NameTagSelector(mview);
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = line++;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(src_nts, c);
	panel.add(src_nts);

	addFiller( panel, gridbag, line++ );

	label = new JLabel("in column ");
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line;
	c.anchor = GridBagConstraints.EAST;
	gridbag.setConstraints(label, c);
	panel.add(label);

	src_col_jtf = new JTextField(10);
	src_col_jtf.setText(mview.getProperty("NameMunger.src_col", ""));
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = line;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.WEST;
	c.fill = GridBagConstraints.BOTH;
	gridbag.setConstraints(src_col_jtf, c);
	panel.add(src_col_jtf);

	return panel;
    }

    private void addFiller( JPanel panel, GridBagLayout gb, int line )
    {
	Dimension fillsize = new Dimension(20,20);
	Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
	GridBagConstraints c = new GridBagConstraints();
	c.gridy = line;
	gb.setConstraints(filler, c);
	panel.add(filler);
    }
    private void addFiller( JPanel panel, GridBagLayout gb, int line, int col, int size )
    {
	Dimension fillsize = new Dimension(size,size);
	Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
	GridBagConstraints c = new GridBagConstraints();
	c.gridx = col;
	c.gridy = line;
	gb.setConstraints(filler, c);
	panel.add(filler);
    }


    public void doLoad(boolean check_it)
    {
	ExprData.NameTagSelection dest_nt_sel = dest_nts.getNameTagSelection();   // where to load to
	ExprData.NameTagSelection src_nt_sel  = src_nts.getNameTagSelection();    // where to get index from

	String tname = dest_nt_sel.getNames();
	if(tname == null)
	{
	    mview.alertMessage("No Name or Attribute selected for \"Load\"");
	    return;
	}

	if(dest_nt_sel.isSpotName())
	{
	    if(mview.infoQuestion("Spot names should not really be modified\nContinue?", "Yes", "No") == 1)
		return;
	}

	try
	{
	    src_col = new Integer(src_col_jtf.getText()).intValue();
	    if(src_col < 1)
	    {
		mview.alertMessage("The \"Load\" column must be a whole nmuber, 1 or greater");
		return;
	    }
		
	}
	catch(NumberFormatException nfe)
	{
	    mview.alertMessage("The \"Load\" column must be a whole nmuber, 1 or greater");
	    return;
	}

	String fname = file_jtf.getText();
	if(fname.length() == 0)
	{
	    mview.alertMessage("No file chosen");
	    return;
	}


	tname = src_nt_sel.getNames();
	if(tname == null)
	{
	    mview.alertMessage("No Name or Attribute selected for \"indexed by\"");
	    return;
	}
	
	try
	{
	    dest_col = new Integer(dest_col_jtf.getText()).intValue();
	    if(dest_col < 1)
	    {
		mview.alertMessage("The \"indexed by\" column must be a whole nmuber, 1 or greater");
		return;
	    }
	}
	catch(NumberFormatException nfe)
	{
	    mview.alertMessage("The \"indexed by\" column must be a whole nmuber, 1 or greater");
	    return;
	}

	Hashtable data = loadTuplesFromFile(fname, src_col, dest_col);

	System.out.println( data.size() + " tuples loaded....");

	if(data == null)
	{
	    mview.alertMessage("Loading aborted");
	    return;
	}
       
	final int n_spots = edata.getNumSpots();

	int loaded = 0;

	// file contains
	//
	//   X1   Y1
	//   X2   Y2
        //   X3   Y2
	//     ....
	//
	//
	//  set dest_nts to YN for any spot with src_nts = XN 
	//
	//

	final boolean is_gene = dest_nt_sel.isGeneNamesOrAttr();

	for(int s=0; s < n_spots; s++)
	{
	    if( !apply_filter || !edata.filter(s) )
	    {
		String[] cur_a = src_nt_sel.getNameTagArray(s);   // set_nt_sel specifies the index tag

		for(int c=0; c < cur_a.length; c++)
		{
		    String curkey = cur_a[c];
		    
		    if(curkey != null)
		    {
			String newval = (String) data.get(curkey);

			if(newval != null)
			{
			    if(!check_it)
			    {
				if(is_gene)
				    dest_nt_sel.setGeneNameTag( null, newval, s );
				else
				    dest_nt_sel.setNameTag( newval, s );
			    }
				
			    loaded++;

			    //System.out.println("spot " + s + " has " + src_nt_sel.getNames() + "=" + curkey + " -> setting " + 
			    //	       dest_nt_sel.getNames() + "=" + newval);
				
			}
		    }
		}
	    }
	}
	String msg = loaded + " '" + dest_nt_sel.getNames() + "'";
	if(loaded != 1)
	    msg += "s";
	if(check_it)
	    msg += " will be";

	if(check_it || report_status)
	    mview.infoMessage(msg + " read from '" + fname + "'");	
    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  s a v e    m o d e
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public JPanel makeSavePanel()
    {
	JPanel panel = new JPanel();
	GridBagLayout gridbag = new GridBagLayout();
	panel.setBorder(BorderFactory.createEmptyBorder(10,10,0,10));
	panel.setLayout(gridbag);
	GridBagConstraints c = null;
	
	int line = 0;

	JLabel label = new JLabel("Save ");
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line;
	c.anchor = GridBagConstraints.EAST;
	gridbag.setConstraints(label, c);
	panel.add(label);

	dest_nts = new NameTagSelector(mview);
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = line++;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(dest_nts, c);
	panel.add(dest_nts);

	addFiller( panel, gridbag, line++ );

	label = new JLabel("to file ");
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line;
	c.anchor = GridBagConstraints.EAST;
	gridbag.setConstraints(label, c);
	panel.add(label);

	file_jtf = new JTextField(20);
	file_jtf.setText(mview.getProperty("NameMunger.file", ""));
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = line;
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.WEST;
	c.fill = GridBagConstraints.BOTH;
	gridbag.setConstraints(file_jtf, c);
	panel.add(file_jtf);

	addFiller( panel, gridbag, line, 2, 8 );

	JButton jb = new JButton("Browse");
	jb.setFont(mview.getSmallFont());
	jb.addActionListener(new ActionListener() 
	    { 
		public void actionPerformed(ActionEvent e) 
		{
		    JFileChooser fc = mview.getFileChooser();
		    fc.setCurrentDirectory( new File ( file_jtf.getText() ) );
		    if(fc.showDialog(frame, "Select") == JFileChooser.APPROVE_OPTION)
		    {
			file_jtf.setText(fc.getSelectedFile().getPath());
		    }
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 3;
	c.gridy = line++;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(jb, c);
	panel.add(jb);

	addFiller( panel, gridbag, line++ );

	label = new JLabel("indexed by ");
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = line;
	c.anchor = GridBagConstraints.EAST;
	gridbag.setConstraints(label, c);
	panel.add(label);

	src_nts = new NameTagSelector(mview, "Nothing");
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = line++;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(src_nts, c);
	panel.add(src_nts);

	addFiller( panel, gridbag, line++ );

	unique_jchkb = new JCheckBox("Make unique");
	unique_jchkb.setToolTipText("Ensure that each of the index values occurs only once in the file");
	unique_jchkb.setSelected(mview.getBooleanProperty("NameMunger.make_unique", true));
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = line;
	c.gridwidth = 2;
	c.anchor = GridBagConstraints.WEST;
	gridbag.setConstraints(unique_jchkb, c);
	panel.add(unique_jchkb);


	return panel;
    }


    public void doSave(boolean check_it)
    {
	ExprData.NameTagSelection dest_nt_sel = dest_nts.getNameTagSelection();
	String tname = dest_nt_sel.getNames();
	if(tname == null)
	{
	    mview.alertMessage("No Name or Attribute selected for \"Save\"");
	    return;
	}

	ExprData.NameTagSelection src_nt_sel  = src_nts.getNameTagSelection();
	if(!src_nts.userOptionSelected())
	{
	    tname = src_nt_sel.getNames();
	    if(tname == null)
	    {
		mview.alertMessage("No Name or Attribute selected for \"indexed by\"");
		return;
	    }
	}

	String fname = file_jtf.getText();
	if(fname.length() == 0)
	{
	    mview.alertMessage("No file chosen");
	    return;
	}

	int output = 0;
	File file = new File(fname);

	boolean file_exists = false;

	if(file.exists())
	{
	    if(!check_it)
	    {
		if(mview.infoQuestion("File exists, overwrite?", "Yes", "No") == 1)
		    return;
	    }
	    else
	    {
		file_exists = true;
	    }
	}

	boolean no_index = src_nts.userOptionSelected();
	boolean unique = unique_jchkb.isSelected();

	try
	{
	   
	    PrintWriter writer = check_it ? null : new PrintWriter(new BufferedWriter(new FileWriter(file)));
	    
	    final int n_spots = edata.getNumSpots();
	    
	    Hashtable unique_ht = new Hashtable();

	    for(int s=0; s < n_spots; s++)
	    {
		if(!apply_filter || !edata.filter(s))
		{
		    String key = no_index ? null : src_nt_sel.getNameTag(s);  // this is the 'indexed_by' bit

		    
		    if(no_index || ((key != null) && (key.length() > 0)))
		    {
			
			String[] vals = dest_nt_sel.getNameTagArray(s);  // this is the 'save' bit

			//System.out.println("key " + s + " = " + key + " has " + vals.length + " values");

			for(int v=0; v < vals.length; v++)
			{
			    String val = vals[v];

			    if((val != null) && (val.length() > 0))
			    {
				boolean skipit = false;
				if(unique)
				{
				    if(unique_ht.get(val) == null)
					unique_ht.put(val,"x");
				    else
					skipit = true;
				}
				
				if(!skipit)
				{
				    output++;
				    if(!check_it)
				    {
					if(no_index)
					    writer.write(val + "\n");
					else
					    writer.write(key + "\t" + val + "\n");
				    }
				}
			    }
			}
			
		    }
		}
	    }
	    if(!check_it)
		writer.close();
	}
	catch(java.io.IOException e)
	{
	    mview.errorMessage("Unable to write to " + file.getName() + "\nerror: " + e);
	}

	String msg = output + " '";
	if(!no_index)
	    msg +=  src_nt_sel.getNames() + ":";
	msg += dest_nt_sel.getNames() + "'";
	if(!no_index)
	{
	    msg += " pair";
	    if(output != 1)
		msg += "s";
	}

	if(check_it)
	{
	    msg += " will be";
	    
	}

	msg += " written to '" + file.getName() + "'";

	if(check_it && file_exists)
	{
	    msg += "\nWarning: A file with this name already exists";
	}
	
	if(check_it || report_status)
	    mview.infoMessage(msg);	
    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  u s e f u l    s t u f f
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public String[] getValues(final NameTagSelector nts)
    {
	ExprData.NameTagSelection nt_sel = nts.getNameTagSelection();
	final int n_spots = edata.getNumSpots();
	
	String[] res = new String[n_spots];

	for(int s=0; s < n_spots; s++)
	{
	    res[s] = nt_sel.getNameTag(s);
	}

	return res;
    }
    
    public int countNulls(final String[] vals)
    {
	final int n_vals = vals.length;
	int nulls = 0;
	for(int v=0; v < n_vals; v++)
	    if(vals[v] == null)
		nulls++;

	return nulls;
    }


    // returns a Hashtable of String:String mappings
    //
    private Hashtable loadTuplesFromFile(String fname)
    {
	File file = new File(fname); //fc.getSelectedFile();

	Hashtable ht = new Hashtable();

	try
	{
	    BufferedReader br = new BufferedReader(new FileReader(file));
	    
	    String str = br.readLine();
	    while(str != null)
	    {
		int dpos = str.indexOf('\t');
		if(dpos > 0)
		{
		    String lhs = str.substring(0, dpos).trim();
		    String rhs = str.substring(dpos+1).trim();

		    if((lhs != null) && (rhs != null))
		    {
			if((lhs.length() > 0) && (rhs.length() > 0))
			{
			    ht.put(lhs, rhs);
			    
			    // System.out.println("'" + lhs + "' -> '" + rhs + "'");
			}
		    }
		}
		str = br.readLine();
	    }
	}
	catch(java.io.IOException ioe)
	{
	    mview.alertMessage("Unable to read name.\nError: " + ioe);
	}
	
	return ht;
    }

    private class ShortLineException extends Exception
    {
    }

    // col counts from 0
    //
    private String getNthCol(String line, int col) throws ShortLineException
    {
	// detect and store delimiters....
	
	int n_delim = 0;
	final int nc = line.length();
	for(int c=0; c < nc; c++)
	    if(line.charAt(c) == '\t')
		n_delim++;
		
	int[] delims_a = new int[n_delim];
	n_delim = 0;
	
	for(int c=0; c < nc; c++)
	    if(line.charAt(c) == '\t')
		delims_a[n_delim++] = c;
	
	
	if(n_delim > 0)
	{		    
	    // now match tokens to cols
	    
	    if(col >= 0)
	    {
		if(col > delims_a.length)
		    throw new ShortLineException();
		
		if(col == 0)
		{
		    int start = delims_a[0];
		    return line.substring(0, start).trim();
		}
		else
		{
		    if(col == delims_a.length)
		    {
			int start = delims_a[col-1];
			return line.substring(start+1).trim();
		    }
		    else
		    {
			int start = delims_a[col-1];
			int end   = delims_a[col];
			
			return line.substring(start+1, end).trim();
		    }
		}
	    }
	    else
	    {
		return null;
	    }
	}
	else
	{
	    throw new ShortLineException();
	}
    }

    // returns a Hashtable of String:String mappings
    // (as above, but with any 2 columns, counting from 1)
    //
    private Hashtable loadTuplesFromFile(String fname, int c1, int c2)
    {
	File file = new File(fname); //fc.getSelectedFile();

	Hashtable ht = new Hashtable();

	int line_n = 1;
	    
	try
	{
	    BufferedReader br = new BufferedReader(new FileReader(file));
	    String str = br.readLine();
	    while(str != null)
	    {
		// extract the relevant columns....
		
		String key = getNthCol(str, c1-1);
		String val = getNthCol(str, c2-1);

		if((key != null) && (val != null))
		    if((key.length() > 0) && (val.length() > 0))
			ht.put(key, val);

		str = br.readLine();

		line_n++;
	    }
	}
	catch(ShortLineException sle)
	{
	    mview.errorMessage("Insufficient columns on line " + line_n + " of file");
	    return null;
	}
	catch(java.io.IOException ioe)
	{
	    mview.alertMessage("Unable to read name.\nError: " + ioe);
	}
	
	return ht;
    }



    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  gubbins
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    String cur_path = null;
    
    private int src_col;
    private int dest_col;

    private maxdView mview;
    private DataPlot dplot;
    private ExprData edata;
    private AnnotationLoader anlo;

    private int mode = -1;
    private JPanel mode_panel;

    private boolean report_status = true;

    private JFrame     frame = null;
}
