import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.io.*;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

public class TextExplorer implements Plugin
{
    public TextExplorer(maxdView mview_)
    {
	mview = mview_;
	edata = mview.getExprData();
	anlo = mview.getAnnotationLoader();
    }

    public void cleanUp()
    {
	if(building == true)
	{
	    // stop existing thread
	    int counter = 0;
	    abort_build = true;
	    while((counter < 100) && (building == true))
	    {
		try
		{
		    Thread.sleep(100);
		}
		catch(InterruptedException tie)
		{
		}
		counter++;
	    }
	}
	
	if(frame != null)
	{
	    frame.setVisible(false);

	    source_nts.saveSelection("TextExplorer.source");
	    
	    mview.putBooleanProperty("TextExplorer.at_most_N", at_most_N);
	    mview.putBooleanProperty("TextExplorer.all_tokens", all_tokens);
	    mview.putBooleanProperty("TextExplorer.sort_alpha", sort_alpha);
	    mview.putBooleanProperty("TextExplorer.case_sens", case_sens);
	    
	    mview.putProperty("TextExplorer.delims", delims_jtf.getText());

	    mview.putIntProperty("TextExplorer.n_top_toks", n_top_toks);
	    mview.putBooleanProperty("TextExplorer.apply_filter", apply_filter);
	    
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
	delims = " \":;()-{}[],.";

	at_most_N  = mview.getBooleanProperty("TextExplorer.at_most_N", true);
	all_tokens = mview.getBooleanProperty("TextExplorer.all_tokens", false);
	sort_alpha = mview.getBooleanProperty("TextExplorer.sort_alpha", false);
	case_sens  = mview.getBooleanProperty("TextExplorer.case_sens", false);

	apply_filter = mview.getBooleanProperty("TextExplorer.apply_filter", false);
	source_mode  = mview.getIntProperty("TextExplorer.source", 1);
	n_top_toks   = mview.getIntProperty("TextExplorer.n_top_toks", n_top_toks);

	// name_list = getTexts();

	// findMostCommonTokens(name_list);

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
	PluginInfo pinf = new PluginInfo("Text Explorer", "viewer", 
							"Find common substrings in text (experimental)", "",
							1, 0, 0);
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
    // --- --- ---  gooey
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    JTable common_table = null;
    JScrollPane jscp = null;

    private void addComponents()
    {
	frame = new JFrame("Text Explorer");
	
	mview.decorateFrame(frame);

	frame.addWindowListener(new WindowAdapter() 
			  {
			      public void windowClosing(WindowEvent e)
			      {
				  cleanUp();
			      }
			  });

	JPanel panel = new JPanel();
	GridBagLayout gridbag = new GridBagLayout();
	panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	panel.setLayout(gridbag);
	GridBagConstraints c = null;

	int line = 0;


	common_table = new JTable();

	common_table.setModel(new MostCommonTableModel());

	jscp = new JScrollPane(common_table);

	{
	    // 
	    // the delimiter and count controls
	    //

	    int cline = 0;

	    JPanel wrapper = new JPanel();
	    //wrapper.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrapper.setLayout(w_gridbag);
	    
	    JLabel label = new JLabel("Source ");
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = cline;
	    c.anchor = GridBagConstraints.EAST;
	    w_gridbag.setConstraints(label, c);
	    wrapper.add(label);

	    {
		JPanel line_wrapper = new JPanel();
		//wrapper.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
		GridBagLayout line_gridbag = new GridBagLayout();
		line_wrapper.setLayout(line_gridbag);
		
		/*
		String[] word_srcs = { "Annotation", "Gene name(s)", "Probe name" };
		
		source_jcb = new JComboBox(word_srcs);
		source_jcb.setSelectedIndex(source_mode);
		source_jcb.addActionListener( new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    source_mode =  source_jcb.getSelectedIndex();
			    name_list = null; // force it to be regenerated
			    startUpdate();
			}
		    });
		*/
		source_nts = new NameTagSelector(mview, "Annotation");
		source_nts.loadSelection("TextExplorer.source");
		source_nts.addActionListener(new ActionListener() 
		    { 
			public void actionPerformed(ActionEvent e) 
			{
			    name_list = null; // force it to be regenerated
			    startUpdate();
			}
		    });

		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		line_gridbag.setConstraints(source_nts, c);
		line_wrapper.add(source_nts);
		
		JCheckBox jchkb = new JCheckBox("Apply filter");
		jchkb.setSelected(apply_filter);
		jchkb.addActionListener(new ActionListener() 
		    { 
			public void actionPerformed(ActionEvent e) 
			{
			    JCheckBox es = (JCheckBox) e.getSource();
			    apply_filter = es.isSelected();
			    name_list = null; // force it to be regenerated
			    startUpdate();
			}
		    });
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		line_gridbag.setConstraints(jchkb, c);
		line_wrapper.add(jchkb);

		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = cline++;
		c.gridwidth = 3;
		//c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		//c.fill = GridBagConstraints.HORIZONTAL;
		w_gridbag.setConstraints(line_wrapper, c);
		wrapper.add(line_wrapper);
	    }

	    // ====================================================================== //

	    label = new JLabel("Display ");
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = cline;
	    c.anchor = GridBagConstraints.EAST;
	    w_gridbag.setConstraints(label, c);
	    wrapper.add(label);

	    ButtonGroup bg = new ButtonGroup();
	    
	    JCheckBox jchkb = new JCheckBox();
	    jchkb.setSelected(at_most_N);
	    jchkb.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			JCheckBox es = (JCheckBox) e.getSource();
			at_most_N = es.isSelected();
			all_tokens = !at_most_N;
			startUpdate();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = cline;
	    //c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    w_gridbag.setConstraints(jchkb, c);
	    wrapper.add(jchkb);
	    bg.add(jchkb);

	    top_N_tok_jtf = new JTextField(5);
	    top_N_tok_jtf.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			startUpdate();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = cline;
	    //c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    //c.fill = GridBagConstraints.HORIZONTAL;
	    w_gridbag.setConstraints(top_N_tok_jtf, c);
	    wrapper.add(top_N_tok_jtf);
	    top_N_tok_jtf.setText(String.valueOf(n_top_toks));

	    label = new JLabel(" most common tokens");
	    c = new GridBagConstraints();
	    c.gridx = 3;
	    c.anchor = GridBagConstraints.WEST;
	    c.gridy = cline++;
	    w_gridbag.setConstraints(label, c);
	    wrapper.add(label);

	    jchkb = new JCheckBox();
	    jchkb.setSelected(all_tokens);
	    jchkb.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			JCheckBox es = (JCheckBox) e.getSource();
			all_tokens = es.isSelected();
			at_most_N = !all_tokens;
			startUpdate();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = cline;
	    //c.weightx = 1.0;
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    w_gridbag.setConstraints(jchkb, c);
	    wrapper.add(jchkb);
	    bg.add(jchkb);

	    label = new JLabel("all tokens which occur more than once");
	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = cline++;
	    c.gridwidth = 2;
	    c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints(label, c);
	    wrapper.add(label);

	    label = new JLabel("Sort by ");
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = cline;
	    c.anchor = GridBagConstraints.EAST;
	    w_gridbag.setConstraints(label, c);
	    wrapper.add(label);


	    {
		JPanel line_wrapper = new JPanel();
		//wrapper.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
		GridBagLayout line_gridbag = new GridBagLayout();
		line_wrapper.setLayout(line_gridbag);

		bg = new ButtonGroup();
		
		jchkb = new JCheckBox("Token");
		jchkb.setSelected(sort_alpha);
		jchkb.addActionListener(new ActionListener() 
		    { 
			public void actionPerformed(ActionEvent e) 
			{
			    JCheckBox es = (JCheckBox) e.getSource();
			    sort_alpha = es.isSelected();
			    startUpdate();
			}
		    });
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridwidth = 2;
		c.gridy = cline;
		//c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		line_gridbag.setConstraints(jchkb, c);
		line_wrapper.add(jchkb);
		bg.add(jchkb);
		
		jchkb = new JCheckBox("Count");
		jchkb.setSelected(!sort_alpha);
		jchkb.addActionListener(new ActionListener() 
		    { 
			public void actionPerformed(ActionEvent e) 
			{
			    JCheckBox es = (JCheckBox) e.getSource();
			    sort_alpha = !(es.isSelected());
			    startUpdate();
			}
		    });
		c = new GridBagConstraints();
		c.gridx = 3;
		c.gridy = cline;
		//c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		line_gridbag.setConstraints(jchkb, c);
		line_wrapper.add(jchkb);
		bg.add(jchkb);


		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = cline++;
		c.gridwidth = 3;
		//c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		//c.fill = GridBagConstraints.HORIZONTAL;
		w_gridbag.setConstraints(line_wrapper, c);
		wrapper.add(line_wrapper);
	    }
	    
	    label = new JLabel("Delimiters ");
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = cline;
	    c.anchor = GridBagConstraints.EAST;
	    w_gridbag.setConstraints(label, c);
	    wrapper.add(label);

	    delims_jtf = new JTextField(12);
	    delims_jtf.setText(delims);
	    delims_jtf.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			JTextField es = (JTextField) e.getSource();
			delims =  es.getText();
			name_list = null; // force it to be regenerated
			startUpdate();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = cline++;;
	    c.gridwidth = 3;
	    //c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    w_gridbag.setConstraints(delims_jtf, c);
	    wrapper.add(delims_jtf);
	    

	    jchkb = new JCheckBox();
	    jchkb.setSelected(case_sens);
	    jchkb.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			JCheckBox es = (JCheckBox) e.getSource();
			case_sens = es.isSelected();
			name_list = null; // force it to be regenerated
			startUpdate();
		    }
		});
	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = cline;
	    //c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    w_gridbag.setConstraints(jchkb, c);
	    wrapper.add(jchkb);

	    label = new JLabel("case sensitive");
	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = cline++;
	    c.gridwidth = 2;
	    c.anchor = GridBagConstraints.WEST;
	    w_gridbag.setConstraints(label, c);
	    wrapper.add(label);

	    panel.add(wrapper);
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    //c.gridwidth = 2;
	    //c.weighty = 2.0;
	    //c.weightx = 1.0;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    //c.anchor = GridBagConstraints.SOUTH;
	    gridbag.setConstraints(wrapper, c);
	    
	}
	
	{
	    // 
	    // the JTable containing the counts & tokens
	    //
	    
	    JPanel wrapper = new JPanel();
	    wrapper.setBorder(BorderFactory.createEmptyBorder(0,0,3,0));

	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrapper.setLayout(w_gridbag);

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weighty = 10.0;
	    c.weightx = 10.0;
	    c.fill = GridBagConstraints.BOTH;
	    //c.anchor = GridBagConstraints.SOUTH;
	    w_gridbag.setConstraints(jscp, c);
	    wrapper.add(jscp);
	    
	    panel.add(wrapper);
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    //c.gridwidth = 2;
	    c.weighty = 1.0;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.BOTH;
	    //c.anchor = GridBagConstraints.SOUTH;
	    gridbag.setConstraints(wrapper, c);
	    
	    names_label = new JLabel("Ready...");
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    //c.gridwidth = 4;
	    //c.anchor = GridBagConstraints.EAST;
	    gridbag.setConstraints(names_label, c);
	    panel.add(names_label);

	}

	{
	    // 
	    // the command buttons
	    //

	    JPanel wrapper = new JPanel();
	    wrapper.setBorder(BorderFactory.createEmptyBorder(3,0,5,0));
	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrapper.setLayout(w_gridbag);

	    JButton jb = new JButton("Make cluster(s)");
	    jb.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			makeClusters();
		    }
		});

	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    w_gridbag.setConstraints(jb, c);
	    wrapper.add(jb);
	    
	    jb = new JButton("Find phrases");
	    jb.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			findPhrases();
		    }
		});

	    c = new GridBagConstraints();
	    c.gridx = 1;
	    c.gridy = 0;
	    w_gridbag.setConstraints(jb, c);
	    wrapper.add(jb);

	    jb = new JButton("View context");
	    jb.setEnabled(false);
	    jb.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			viewContext();
		    }
		});

	    c = new GridBagConstraints();
	    c.gridx = 2;
	    c.gridy = 0;
	    w_gridbag.setConstraints(jb, c);
	    wrapper.add(jb);


	    panel.add(wrapper);
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    //c.gridwidth = 2;
	    //c.weighty = 2.0;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    //c.anchor = GridBagConstraints.SOUTH;
	    gridbag.setConstraints(wrapper, c);
	    
	}
	

	{
	    // 
	    // the Help & Cancel buttons
	    //

	    JPanel wrapper = new JPanel();
	    //wrapper.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
	    GridBagLayout w_gridbag = new GridBagLayout();
	    wrapper.setLayout(w_gridbag);

	    {
		JButton button = new JButton("Close");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    cleanUp();
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		w_gridbag.setConstraints(button, c);
	    }
	    {
		JButton button = new JButton("Update");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    startUpdate();
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		w_gridbag.setConstraints(button, c);
	    }
	    {
		JButton button = new JButton("Help");
		wrapper.add(button);
		
		button.addActionListener(new ActionListener() 
		    {
			public void actionPerformed(ActionEvent e) 
			{
			    mview.getPluginHelpTopic("TextExplorer", "TextExplorer");
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = 0;
		w_gridbag.setConstraints(button, c);
	    }

	    panel.add(wrapper);
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    //c.gridwidth = 2;
	    //c.weighty = 2.0;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    //c.anchor = GridBagConstraints.SOUTH;
	    gridbag.setConstraints(wrapper, c);
	    
	}


	frame.getContentPane().add(panel);

	//System.out.println("starting...");

	frame.pack();
	frame.setVisible(true);
	
	TableColumn column = null;
	column = common_table.getColumnModel().getColumn(0);
	column.setPreferredWidth(300);
	
	column = common_table.getColumnModel().getColumn(1);
	column.setPreferredWidth(50);
	
	//System.out.println("finished...");
    }

    public void startUpdate()
    {
	//System.out.println("startUpdate(): ....");
	
	if(all_tokens == false)
	{
	    try
	    {
		n_top_toks = new Integer(top_N_tok_jtf.getText()).intValue();
	    }
	    catch(NumberFormatException nfe)
	    {
	    }
	}

	if(name_list == null)
	    name_list = getTexts();

	findMostCommonTokens(name_list);

    }
    
    public void updateTable()
    {
	//System.out.println(" updateTable(): ....");
	
	int c0w = common_table.getColumnModel().getColumn(0).getWidth();
	int c1w = common_table.getColumnModel().getColumn(1).getWidth();

	//System.out.println(c0w + " ... " + c1w);

	MostCommonTableModel mctm = new MostCommonTableModel();
	
	common_table.setModel(mctm);
	
	//common_table.validate();
	//jscp.validate();
	
	int w = common_table.getWidth();
	//System.out.println("table is " + w);

	TableColumn column = null;
	column = common_table.getColumnModel().getColumn(0);
	column.setMaxWidth(w);
	column.setMinWidth(c0w);
	column.setPreferredWidth(c0w);
	
	column = common_table.getColumnModel().getColumn(1);
	column.setMaxWidth(w);
	//column.setMinWidth(c1w);
	column.setPreferredWidth(c1w);

	//common_table.validate();
	//jscp.validate();

	System.gc();

    }

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    Vector name_list;

    public Vector getTexts()
    {
	//System.out.println("filter is " + apply_filter);

	anno_id_to_spot_ids = new Vector();
	
	if(source_nts.userOptionSelected())
	{
	    return getAnnotations();
	}
	else
	{
	    return getTagAttrs();
	}
    }

    Vector anno_id_to_spot_ids = null;  // Vector of ( Vector of Integers )

    public Vector getTagAttrs()
    {
	Hashtable dupls = new Hashtable();

	Vector nv = new Vector();
	
	ExprData.NameTagSelection nt_sel = source_nts.getNameTagSelection();

	for(int s=0; s < edata.getNumSpots(); s++)
	{
	    if((!apply_filter) || (!edata.filter(s)))
	    {
		String name = nt_sel.getNameTag(s);

		if(name != null)
		{
		    Integer sid = new Integer(s);
		    if(dupls.get(name) == null)
		    {
			nv.addElement(name);
			
			Vector sids = new Vector();
			sids.addElement(sid);

			dupls.put(name, new Integer(anno_id_to_spot_ids.size()));
			anno_id_to_spot_ids.addElement(sids);
			
		    }
		    else
		    {
			// this name is already seen...
			int old_anno_id_i = ((Integer) dupls.get(name)).intValue();
			Vector sids = (Vector)  anno_id_to_spot_ids.elementAt(old_anno_id_i);
			sids.addElement(sid);
		    }
		}
	    }
	}

	return nv;
    }

    public Vector getAnnotations()
    {
	anlo = mview.getAnnotationLoader();

	Vector av = new Vector();
	for(int s=0; s < edata.getNumSpots(); s++)
	{
	     if((!apply_filter) || (!edata.filter(s)))
	     {
		 String ce = anlo.loadAnnotationFromCache(s);
		 if(ce != null)
		 {
		     av.addElement(ce);
		     
		     Integer sid = new Integer(s);
		     Vector sids = new Vector();
		     sids.addElement(sid);
		     anno_id_to_spot_ids.addElement(sids);
		 }
	     }
	}

	return av;
    }

    private class MostCommonTableModel extends javax.swing.table.AbstractTableModel
    {
	public int getRowCount()    { return most_common.length; }
	public int getColumnCount() { return 2; } 
	
	public Object getValueAt(int row, int col)
	{
	    if(col == 1) // col 0 is the line number
	    {
		return String.valueOf(most_common[row].count); //_cnt.elementAt(row);
	    }
	    else
	    {
		return most_common[row].str; //most_common.elementAt(row);
	    }
	}
	public String getColumnName(int col) 
	{
	    if(col == 1)
		return "Count";
	    else
		return "Token";
	}
    }
    

    //Vector most_common     = null;
    //Vector most_common_cnt = null;
    CommonToken[] most_common = new CommonToken[0];

    String delims = null;

    int n_top_toks      = 100;

    int source_mode = 0;

    boolean apply_filter = false;

    private WordIndex word_index = null;

    private CommonTokenFinderThread cmft = null;
    private boolean abort_build = false;
    private boolean building = false;
    private void findMostCommonTokens(Vector input)
    {
	if(building == true)
	{
	    // stop existing thread
	    int counter = 0;
	    abort_build = true;
	    while((counter < 100) && (building == true))
	    {
		try
		{
		    Thread.sleep(100);
		}
		catch(InterruptedException tie)
		{
		}
		counter++;
	    }
	}
	abort_build = false;
	cmft = new CommonTokenFinderThread(input);
	cmft.start();
    }

    private String unescapeDelimiters(String d)
    {
	boolean armed = false;
	StringBuffer sbuf = new StringBuffer();

	for(int c=0; c < d.length(); c++)
	{
	    char ch  = d.charAt(c);
	    if((ch == '\\') && (!armed))
	    {
		armed = true;
	    }
	    else
	    {
		if(armed)
		{
		    if(ch == 'n') 
			sbuf.append('\n');
		    if(ch == '\\') 
			sbuf.append('\\');
		    if(ch == 't') 
			sbuf.append('\t');
		    armed = false;
		}
		else
		{
		    sbuf.append(ch);
		}

	    }
	}
	
	// System.out.println("\"" + d + "\"" + " -> \"" + sbuf.toString() + "\"");

	return sbuf.toString();
    }

    private class CommonTokenFinderThread extends Thread
    {
	Vector input = null;

	public CommonTokenFinderThread(Vector i)
	{
	    input = i;
	}
	
	public void run()
	{
	// first build a hashtable recording all of the occurences of each of the tokens...
	    
	if(input == null)
	    return;
	
	building = true;
	frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

	word_index = new WordIndex();
		
	// tokenise the names
	//

	total_tokens = 0;

	java.util.Date start_time = new java.util.Date();

	final int n_annos = input.size();
	
	String real_delims = unescapeDelimiters(delims_jtf.getText());

	int ticks = 0;

	for(int an=0; an < n_annos; an++)
	{
	    if(++ticks == 20)
	    {
		if(names_label != null)
		    names_label.setText("Tokenising: " + an + " of " + n_annos);
		ticks = 0;
	    }

	    if(abort_build)
	    {
		building = false;
		return;
	    }

	    StringBuffer sbuf = new StringBuffer();

	    final String str = (String) input.elementAt(an);
	    final int len = str.length();

	    int c = 0;
	    boolean intok = true;
	    int w_count = 0;

	    while(c < len)
	    {
		char ch = str.charAt(c++);

		if(!case_sens)
		    ch = Character.toLowerCase(ch);

		if(real_delims.indexOf(ch) >= 0)
		{
		    if(sbuf.length() > 0)
		    {
			word_index.addEntry(sbuf.toString(), an, w_count);
			w_count++;
			sbuf = new StringBuffer();
		    }
		}
		else
		{
		    sbuf.append(ch);
		}

	    }
	    // add the last token (if any)
	    if(sbuf.length() > 0)
	    {
		word_index.addEntry(sbuf.toString(), an, w_count);
		w_count++;
			
	    }

	    total_tokens += w_count;

	    Thread.yield();
	}

	int n_words = word_index.getNumWords(); 
	//System.out.println(total_tokens + " words found (" + n_words + " unique)");

	WordIndexHeader[] wih_a = word_index.getRepeats();
	int n_reps = wih_a.length;

	//System.out.println(n_reps + " words occur at least once");
	
	int local_n_top_toks = n_top_toks;
	if(all_tokens == true)
	{
	    local_n_top_toks = n_reps;
	}

	if(local_n_top_toks > n_reps)
	    local_n_top_toks = n_reps;

	if(names_label != null)
	    names_label.setText(total_tokens + " tokens, " + n_words + " unique, " + n_reps + " repeats");
	
	// System.out.println("seeking " + local_n_top_toks + " most common tokens");

	// build an array of 'MostCommon' objects for the top N words

	most_common = new  CommonToken[local_n_top_toks];
	int mc = 0;

	for(int i=0; i < local_n_top_toks; i++)
	{
	    if(wih_a[i] != null)
	    {
		most_common[mc++] = new CommonToken(wih_a[i].entries.size(), wih_a[i].word);
	    }
	}

	// potentially sort the list alphanumerically....

	if(sort_alpha)
	{
	    sort_mode = 0;

	    java.util.Arrays.sort(most_common);
	}

	frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	
	updateTable();

	building = false;

	System.out.println("tokenised in " + mview.niceTime( mview.secondsSince( start_time )));
	}

    }

    // =====================================================================================
    // ====                            =====================================================
    // ====   v i e w  C o n t e x t   =====================================================
    // ====                            =====================================================
    // =====================================================================================


    private void viewContext()
    {
	if(word_index == null)
	    return;

	ListSelectionModel lsm = common_table.getSelectionModel();

	int s= lsm.getMinSelectionIndex();

	String target = most_common[s].str;

	//System.out.println("phrases for " + target + " ...");

	// new ContextDisplay(target);

    }

    // ====================================================================================
    // ====                           =====================================================
    // ====   f i n d P h r a s e s   =====================================================
    // ====                           =====================================================
    // ====================================================================================

    public class Phrase
    {
	public Phrase(int count_, String w1_, int dist_, String w2_)
	{
	    count = count_;
	    w1    = w1_;
	    w2    = w2_;
	    dist  = dist_;
	}
	public int count, dist;
	public String w1, w2;
    }


    // ====================================================================================

    private void findPhrases()
    {
	if(word_index == null)
	    return;

	ListSelectionModel lsm = common_table.getSelectionModel();

	int s= lsm.getMinSelectionIndex();

	String target = most_common[s].str;

	//System.out.println("phrases for " + target + " ...");

	final int max_dist  = 5;
	final int min_count = 1;
	final int sort_mode = 0;

	new PhraseDisplay(target, max_dist, min_count, sort_mode);

    }

    //
    // sort_mode = 0 ... by distance
    //             1 ... by count
    //             2 ... by 'other' token
    //

    private class PhraseFinderThread extends Thread
    {
	private Phrase[] hits_a;
	private String target;
	private int max_dist;
	private int min_count;
	private int sort_mode;
	private ActionListener al;
	private JLabel label;

	public PhraseFinderThread(String target_, int max_dist_, int min_count_, int sort_mode_, 
				  JLabel label_, ActionListener al_)
	{
	    target = target_;
	    max_dist  = max_dist_;
	    min_count = min_count_;
	    sort_mode = sort_mode_;
	    label = label_;
	    al = al_;
	}

	public Phrase[] getPhrases()
	{
	    return hits_a;
	}

	public void run()
	{
	    
	    hits_a = null;
	    
	    //
	    // foreach string which contains the selected token
	    //   
	    // T <- target word
	    //
	    // foreach word W
	    //    foreach string S
	    //       if ( W in S and T in S )
	    //         hits[W]++
	    
	    int n_words = word_index.getNumWords();
	    
	    int[][] a_hits   = new int[n_words][]; // when the word occurs _after_ the target
	    int[][] b_hits   = new int[n_words][]; // when the word occurs _before_ the target

	    //int[] a_dists = new int[n_words];  // when the word occurs _after_ the target
	    //int[] b_dists = new int[n_words];  // when the word occurs _before_ the target
	    
	    WordIndexHeader target_wih = word_index.getEntry(target);
	    
	    final int ne =  target_wih.getNumEntries();
	    
	    int ticker = 0;
	    // for each A of the annotations which the target word occurs in ...
	    for(int ent=0; ent < ne; ent++)
	    {
		if(label != null)
		{
		    if(++ticker == 10)
		    {
			ticker = 0;
			label.setText("Checking: " + (ent+1) + " of " + ne);
		    }

		}
		
		int target_anno_id = ((WordIndexEntry) target_wih.entries.elementAt(ent)).anno_id;
		int target_index = ((WordIndexEntry) target_wih.entries.elementAt(ent)).index[0];
		
		// for each W of the words ...
		int w = 0;
		WordIndexEntry wie = null;
		
		for (Enumeration e = word_index.getNames(); e.hasMoreElements() ;) 
		{
		    String wrd = (String) e.nextElement();
		    WordIndexHeader wih = word_index.getEntry(wrd);
		    
		    // does this word also occur in annotation A ?
		    
		    if(wih != target_wih)
		    {
			final int ne2 = wih.getNumEntries();
			for(int ent2=0; ent2 < ne2; ent2++)
			{
			    wie = (WordIndexEntry) wih.entries.elementAt(ent2);
			    if(wie.anno_id == target_anno_id)
			    {
				// entries can record more than one instance of word
				// find the occurence nearest the target word
				
				int dist = wie.index[0] - target_index;
				
				for(int h=1; h < wie.index.length; h++)
				{
				    int odist = wie.index[h] - target_index;
				    if((Math.abs(odist) < dist))
					dist = odist;
				}
				
				/*
				  if(dist > 0)
				  dist--;
				  else 
				  if(dist < 0)
				  dist++;
				*/
				
				if(dist < 0)
				{
				// word occurs before target
				    if(-dist <= max_dist)
				    {
					if(b_hits[w] == null)
					    b_hits[w] = new int[max_dist+1];
					b_hits[w][-dist]++;
				    }
				}
				else
				{
				    // word occurs after target
				    if(dist <= max_dist)
				    {
					if(a_hits[w] == null)
					    a_hits[w] = new int[max_dist+1];
					a_hits[w][dist]++;
				    }
				}
				
				// this is guaranteed to be the only instance of this anno_id in entries
				// force the loop to end early
				ent2 = ne2;
			    }
			}
		    }
		    
		    w++;
		}
	    }
	    
	    // the hits vector records each phrase that occur at least 'min_count' times
	    
	    // StringBuffer sbuf = new StringBuffer();
	    
	    Vector hits_v = new Vector();
	    
	    for(int d=0; d <= max_dist; d++)
	    {
		//String blanks = "";
		//for(int b=1; b < d; b++)
		//  blanks += ". ";
		
		int w = 0;
		for (Enumeration e = word_index.getNames(); e.hasMoreElements() ;) 
		{
		    String wrd = (String) e.nextElement();
		    
		    if((a_hits[w] != null) && (a_hits[w][d] >= min_count))
		    {
			//sbuf.append(a_hits[w][d] + " x " + target + " " + blanks + wrd + "\n");
			hits_v.addElement(new Phrase(a_hits[w][d], target, d, wrd));
		    }
		    
		    if((b_hits[w] != null) && (b_hits[w][d] >= min_count))
		    {
			//sbuf.append(b_hits[w][d] + " x " + wrd + " " + blanks + target + "\n");
			hits_v.addElement(new Phrase(b_hits[w][d], wrd, d, target));
		    }
		    
		    w++;
		}
	    }
	    
	    System.gc();
	    
	    hits_a = (Phrase[]) hits_v.toArray(new Phrase[0]);
	    
	    if(sort_mode == 1)
	    {
		java.util.Arrays.sort(hits_a, new SortByCountComparator());
	    }
	    if(sort_mode == 2)
	    {
		java.util.Arrays.sort(hits_a, new SortByTokenComparator(target));
	    }
	    
	    if(al != null)
		al.actionPerformed(null);
	    // mview.infoMessage(sbuf.toString());
	    //return hits_a;
	}
	
    }
    

    private class SortByCountComparator implements java.util.Comparator
    {
	public int compare(Object o1, Object o2)
	{
	    Phrase p1 = (Phrase) o1;
	    Phrase p2 = (Phrase) o2;
	    
	    return (p2.count - p1.count);
	}
	
	public boolean equals(Object o) { return false; }
    }

    private class SortByTokenComparator implements java.util.Comparator
    {
	private String target;

	public SortByTokenComparator(String target_)
	{
	    target = target_;
	}
	public int compare(Object o1, Object o2)
	{
	    Phrase p1 = (Phrase) o1;
	    Phrase p2 = (Phrase) o2;
	    
	    
	    //String s1 = (p1.w1 == target) ? p1.w2 : p1.w1;
	    //String s2 = (p2.w1 == target) ? p2.w2 : p2.w1;

	    //return (s1.compareTo(s2));
	    if(p1.w1.equals(p2.w1))
		return (p1.w2.compareTo(p2.w2));
	    else
		return (p1.w1.compareTo(p2.w1));
	}
	
	public boolean equals(Object o) { return false; }
    }

    // ====================================================================================

    // returns array of anno_id's for the specified phrase
    // (actually re-does part of the search because the annos are not
    //  stored in the initial search as they will take acres of mememroy)
    //
    private int[] findAnnoFor(Phrase ph)
    {
	Vector anno_ids = new Vector();

	WordIndexHeader target_wih = word_index.getEntry(ph.w1);

	final int ne =  target_wih.getNumEntries();

	// for each A of the annotations which the target word occurs in ...
	for(int ent=0; ent < ne; ent++)
	{
	    // System.out.println("checking '" + ph.w1 + "' entry " + (ent+1) + " of " + ne);

	    int target_anno_id = ((WordIndexEntry) target_wih.entries.elementAt(ent)).anno_id;
	    int target_index = ((WordIndexEntry) target_wih.entries.elementAt(ent)).index[0];
	    
	    // for each W of the words ...
	    int w = 0;
	    WordIndexEntry wie = null;

	    WordIndexHeader wih = word_index.getEntry(ph.w2);
	    
	    // does this word also occur in annotation A ?
	    
	    if(wih != target_wih)
	    {
		final int ne2 = wih.getNumEntries();
		for(int ent2=0; ent2 < ne2; ent2++)
		{
		    wie = (WordIndexEntry) wih.entries.elementAt(ent2);
		    if(wie.anno_id == target_anno_id)
		    {
			// entries can record more than one instance of word
			// find the occurence nearest the target word
			
			int dist = wie.index[0] - target_index;

			for(int h=1; h < wie.index.length; h++)
			{
			    int odist = wie.index[h] - target_index;
			    if((Math.abs(odist) < dist))
				dist = odist;
			}
			
			if(dist == ph.dist)
			{
			    // found a candidate
			    
			    // System.out.println(ph.w1 + " d=" + dist + " " + ph.w2);
			    
			    anno_ids.addElement(new Integer(target_anno_id));
			    
			}
		    }
		}
	    }
	}

	int[] result = new int[anno_ids.size()];
	
	for(int a=0; a < anno_ids.size(); a++)
	{
	    result[a] = ((Integer) anno_ids.elementAt(a)).intValue();
	}
	
	return result;
	 
    }

    // ====================================================================================

    public class PhraseDisplay
    {
	private int max_dist, min_count;
	private boolean sort_by_count;
	private String target;
	private JTable phrase_table;
	private JLabel status_label;
	private Phrase[] phrases;
	private PhraseFinderThread pft;

	public PhraseDisplay(final String target_, 
			     final int max_dist_, final int min_count_, 
			     final int sort_mode_)
	{
	    max_dist = max_dist_;
	    min_count = min_count_;
	    target = target_;
	    sort_mode = sort_mode_;
	    //
	    // do the actual search
	    //
	    
	    //final ProgressOMeter pm = new ProgressOMeter("Checking", 2);

	    
	    //pm.startIt();
	    //phrases = findPhrases(target, max_dist, min_count, sort_mode);
	    //pm.stopIt();

	    //	    pm.startIt();

	    final JFrame frame = new JFrame("Find Phrases: " + target);
	    
	    mview.decorateFrame(frame);
	
	    JPanel panel = new JPanel();
	    GridBagLayout gridbag = new GridBagLayout();
	    panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	    panel.setLayout(gridbag);
	    GridBagConstraints c = null;
	    
	    int line = 0;
	    
	    {
		// 
		// the target and max_dist controls
		//
		
		int cline = 0;
		
		JPanel wrapper = new JPanel();
		//wrapper.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
		GridBagLayout w_gridbag = new GridBagLayout();
		wrapper.setLayout(w_gridbag);
		
		JLabel label = new JLabel("Token ");
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = cline;
		c.anchor = GridBagConstraints.EAST;
		w_gridbag.setConstraints(label, c);
		wrapper.add(label);

		JTextField target_jtf = new JTextField(20);
		target_jtf.setText(target);
		target_jtf.setEditable(false);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = cline++;
		c.gridwidth = 3;
		c.weightx = 1.0;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.VERTICAL;
		w_gridbag.setConstraints(target_jtf, c);
		wrapper.add(target_jtf);

		// ==================================

		label = new JLabel("Max. distance ");

		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = cline;
		c.anchor = GridBagConstraints.EAST;
		w_gridbag.setConstraints(label, c);
		wrapper.add(label);
		
		final JTextField max_dist_jtf = new JTextField(10);
		max_dist_jtf.setText(String.valueOf(max_dist));
		max_dist_jtf.addActionListener(new ActionListener() 
		    { 
			public void actionPerformed(ActionEvent e) 
			{
			    try 
			    { 
				max_dist = (new Integer(max_dist_jtf.getText()).intValue()) + 1; 
				updatePhraseDisplay();
			    }
			    catch(NumberFormatException nfe) 
			    { }
			}
		    });
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = cline++;
		c.gridwidth = 3;
		c.anchor = GridBagConstraints.WEST;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.REMAINDER;
		w_gridbag.setConstraints(max_dist_jtf, c);
		wrapper.add(max_dist_jtf);

		// ==================================

		label = new JLabel("Min. count ");
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = cline;
		c.anchor = GridBagConstraints.EAST;
		w_gridbag.setConstraints(label, c);
		wrapper.add(label);
		

		final JTextField min_count_jtf = new JTextField(10);
		min_count_jtf.setText(String.valueOf(min_count));
		min_count_jtf.addActionListener(new ActionListener() 
		    { 
			public void actionPerformed(ActionEvent e) 
			{
			    try 
			    { 
				min_count = new Integer(min_count_jtf.getText()).intValue(); 
				if(min_count < 1)
				{
				    min_count_jtf.setText("1");
				    min_count = 1;
				}
				updatePhraseDisplay();
			    }
			    catch(NumberFormatException nfe) 
			    { }
			}
		    });
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridwidth = 3;
		c.gridy = cline++;
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.REMAINDER;
		w_gridbag.setConstraints(min_count_jtf, c);
		wrapper.add(min_count_jtf);

		// ==================================

		label = new JLabel("Sort by ");

		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = cline;
		c.anchor = GridBagConstraints.EAST;
		w_gridbag.setConstraints(label, c);
		wrapper.add(label);

		final JCheckBox sort_dist_jchkb = new JCheckBox("Distance");
		
		sort_dist_jchkb.addActionListener(new ActionListener() 
		    { 
			public void actionPerformed(ActionEvent e) 
			{
			    sort_mode = 0;
			    updatePhraseDisplay();
			}
		    });
		sort_dist_jchkb.setSelected(true);
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = cline;
		c.anchor = GridBagConstraints.WEST;
		w_gridbag.setConstraints(sort_dist_jchkb, c);
		wrapper.add(sort_dist_jchkb);

		final JCheckBox sort_count_jchkb = new JCheckBox("Count");
		sort_count_jchkb.addActionListener(new ActionListener() 
		    { 
			public void actionPerformed(ActionEvent e) 
			{
			    sort_mode = 1;
			    updatePhraseDisplay();
			}
		    });
		c = new GridBagConstraints();
		c.gridx = 2;
		c.gridy = cline;
		c.anchor = GridBagConstraints.WEST;
		w_gridbag.setConstraints(sort_count_jchkb, c);
		wrapper.add(sort_count_jchkb);

		final JCheckBox sort_token_jchkb = new JCheckBox("Token");
		sort_token_jchkb.addActionListener(new ActionListener() 
		    { 
			public void actionPerformed(ActionEvent e) 
			{
			    sort_mode = 2;
			    updatePhraseDisplay();
			}
		    });
		c = new GridBagConstraints();
		c.gridx = 3;
		c.gridy = cline;
		c.anchor = GridBagConstraints.WEST;
		w_gridbag.setConstraints(sort_token_jchkb, c);
		wrapper.add(sort_token_jchkb);

		ButtonGroup bg = new ButtonGroup();
		bg.add(sort_dist_jchkb);
		bg.add(sort_count_jchkb);
		bg.add(sort_token_jchkb);

		cline++;

		// ==================================

		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line++;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 2.0;
		c.anchor = GridBagConstraints.NORTH;
		gridbag.setConstraints(wrapper, c);
		panel.add(wrapper);
	    }

	    {
		// 
		// the table
		//
		phrase_table = new JTable();
		
		//phrase_table.setModel(new PhraseTableModel(phrases));

		JScrollPane jscp = new JScrollPane(phrase_table);
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line++;
		c.weightx = c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		gridbag.setConstraints(jscp, c);
		panel.add(jscp);
	    }

	    status_label = new JLabel("Initialising...");
	    c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = line++;
	    //c.gridwidth = 4;
	    //c.anchor = GridBagConstraints.EAST;
	    c.fill = GridBagConstraints.VERTICAL;
	    gridbag.setConstraints(status_label, c);
	    panel.add(status_label);

	    {
		// 
		// the command buttons
		//
		
		JPanel wrapper = new JPanel();
		wrapper.setBorder(BorderFactory.createEmptyBorder(3,0,5,0));
		GridBagLayout w_gridbag = new GridBagLayout();
		wrapper.setLayout(w_gridbag);
		
		JButton jb = new JButton("Make cluster(s)");
		jb.addActionListener(new ActionListener() 
		    { 
			public void actionPerformed(ActionEvent e) 
			{
			    ListSelectionModel lsm = phrase_table.getSelectionModel();
			    
			    for(int s= lsm.getMinSelectionIndex(); s <= lsm.getMaxSelectionIndex(); s++)
			    {
				if(lsm.isSelectedIndex(s))
				{
				    //System.out.println("cluster for " + s + " " + most_common[s].str);
				    makeClustersForPhrase(phrases[s]);
				}
			    }
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		w_gridbag.setConstraints(jb, c);
		wrapper.add(jb);
		
		/*
		jb = new JButton("Find phrases");
		jb.setEnabled(false);
		jb.addActionListener(new ActionListener() 
		    { 
			public void actionPerformed(ActionEvent e) 
			{
			    findPhrases();
			}
		    });
		
		c = new GridBagConstraints();
		c.gridx = 1;
		c.gridy = 0;
		w_gridbag.setConstraints(jb, c);
		wrapper.add(jb);
		*/
		
		panel.add(wrapper);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line++;
		//c.gridwidth = 2;
		//c.weighty = 2.0;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		//c.anchor = GridBagConstraints.SOUTH;
		gridbag.setConstraints(wrapper, c);
		
	    }
	    
	    {
		// 
		// the Help & Cancel buttons
		//
		
		JPanel wrapper = new JPanel();
		//wrapper.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
		GridBagLayout w_gridbag = new GridBagLayout();
		wrapper.setLayout(w_gridbag);
		
		{
		    JButton button = new JButton("Close");
		    wrapper.add(button);
		    
		    button.addActionListener(new ActionListener() 
			{
			    public void actionPerformed(ActionEvent e) 
			    {
				frame.setVisible(false);
			    }
			});
		    
		    c = new GridBagConstraints();
		    c.gridx = 0;
		    c.gridy = 0;
		    w_gridbag.setConstraints(button, c);
		}
		{
		    JButton button = new JButton("Help");
		    wrapper.add(button);
		    
		    button.addActionListener(new ActionListener() 
			{
			    public void actionPerformed(ActionEvent e) 
			    {
				mview.getPluginHelpTopic("TextExplorer", "TextExplorer", "#phrase");
			    }
			});
		    
		    c = new GridBagConstraints();
		    c.gridx = 1;
		    c.gridy = 0;
		    w_gridbag.setConstraints(button, c);
		}

		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = line++;
		//c.gridwidth = 2;
		//c.weighty = 2.0;
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.SOUTH;
		gridbag.setConstraints(wrapper, c);
		panel.add(wrapper);
		
	    }
	    
	    
	    frame.getContentPane().add(panel);
	    
	    //System.out.println("starting...");
	    
	    frame.pack();

	    //initColWidths();
	       
	    frame.setVisible(true);

	    pft = new PhraseFinderThread(target, max_dist, min_count, sort_mode,
					 status_label, 
					 new ActionListener()
					     {
						 public void actionPerformed(ActionEvent e)
					     {
						 //pm.stopIt();
						 phrases = pft.getPhrases();
						 if(phrases.length == 0)
						 {
						     mview.infoMessage("No phrases containing '" + target + "' found");
						     frame.setVisible(false);
						     return;
						 }
						 
						 phrase_table.setModel(new PhraseTableModel(phrases));
						 initColWidths();
						 status_label.setText("N phrases");
					     }
					     });
	    pft.start();
	}

	private void initColWidths()
	{
	    int w = phrase_table.getWidth();
	    int wf = w / 8;
	    //System.out.println("table w is " + w);

	    TableColumnModel tcm = phrase_table.getColumnModel();

	    TableColumn column = tcm.getColumn(0);
	    //column.setMaxWidth(c0w);
	    //column.setMinWidth(c0w);
	    column.setPreferredWidth(3 * wf);
	    
	    column = tcm.getColumn(1);
	    column.setPreferredWidth(wf);

	    column = tcm.getColumn(2);
	    column.setPreferredWidth(3 * wf);
	    
	    column = tcm.getColumn(3);
	    column.setPreferredWidth(wf);
	}

	// called when the underlying word_index has changed
	// therefore no more updates can be made
	//
	public void invalidateDisplay()
	{

	}

	private void updatePhraseDisplay()
	{
	    pft = new PhraseFinderThread(target, max_dist, min_count, sort_mode,
					 status_label, 
					 new ActionListener()
					     {
						 public void actionPerformed(ActionEvent e)
					     {
						 //pm.stopIt();
						 phrases = pft.getPhrases();
						 if(phrases.length == 0)
						 {
						     mview.infoMessage("No phrases containing '" + target + "' found");
						     return;
						 }
						 
						 phrase_table.setModel(new PhraseTableModel(phrases));
						 initColWidths();
						 status_label.setText("N phrases");
					     }
					     });
	    pft.start();
	}

	private void makeClustersForPhrase(Phrase ph)
	{
	    String blanks = " ";
	    for(int b=1; b < ph.dist; b++)
		blanks += ". ";
	    
	    String cluster_name = ph.w1 + blanks + ph.w2;

	    // System.out.println("cluster: " + cluster_name);

	    int[] anno_ids = findAnnoFor(ph);

	    Vector snames = new Vector();
	    
	    for(int a=0; a < anno_ids.length; a++)
	    {

		Vector sids = (Vector) anno_id_to_spot_ids.elementAt(anno_ids[a]);
		
		for(int s=0; s < sids.size(); s++)
		{
		    Integer sid = (Integer) sids.elementAt(s);
		    
		    snames.addElement( edata.getSpotName( sid.intValue() ));
		    
		    // System.out.println("anno=" + anno_ids[a] + " -> spot=" + sid.intValue() );
		}
	    }

	    if(snames.size() > 0)
	    {
		edata.addCluster(cluster_name, ExprData.SpotName, snames);
	    }
	}

    }


    private class PhraseTableModel extends javax.swing.table.AbstractTableModel
    {
	private final String[] col_name = { "Token", "Distance", "Token", "Count" };

	private Phrase[] data;

	public PhraseTableModel(Phrase[] data_)
	{
	    data = data_;
	}
	
	public int getRowCount()    { return data.length; }
	public int getColumnCount() { return 4; } 
	
	public Object getValueAt(int row, int col)
	{
	    if(row < data.length)
	    {
		switch(col)
		{
		case 0:
		    return data[row].w1;
		case 1:
		    return String.valueOf(data[row].dist - 1);
		case 2:
		    return data[row].w2;
		case 3:
		    return String.valueOf(data[row].count);
		}
	    }
	    return null;
	}
	public String getColumnName(int col) 
	{
	    return col_name[col];
	}
    }

    // ====================================================================================
    // ====================================================================================
    // ====================================================================================

    private int sort_mode = 0;

    private class CommonToken implements Comparable
    {
	public int count;
	public String str;

	public CommonToken(int c, String s)
	{
	    count = c;
	    str = s;
	}

	public int compareTo(Object o)
	{
	    CommonToken ct = (CommonToken) o;
	    return str.compareTo(ct.str);
	}
    }

    // make a cluster contain all spots which match any selected name in the table
    public void makeClusters()
    {
	ListSelectionModel lsm = common_table.getSelectionModel();

	for(int s= lsm.getMinSelectionIndex(); s <= lsm.getMaxSelectionIndex(); s++)
	{
	    if(lsm.isSelectedIndex(s))
	    {
		// System.out.println("cluster for " + s + " " + most_common[s].str);

		makeCluster(s);

	    }
	}
    }

    private void makeCluster(int target)
    {
	// find the spot names....
	String wrd = most_common[target].str;

	WordIndexHeader wih = word_index.getEntry(wrd);
	
	Vector snames = new Vector();

	for(int e=0; e < wih.entries.size(); e++)
	{
	    WordIndexEntry wie = (WordIndexEntry) wih.entries.elementAt(e);

	    Vector sids = (Vector) anno_id_to_spot_ids.elementAt(wie.anno_id);

	    for(int s=0; s < sids.size(); s++)
	    {
		Integer sid = (Integer) sids.elementAt(s);
		
		snames.addElement( edata.getSpotName( sid.intValue() ));

		// System.out.println("anno=" + wie.anno_id + " -> spot=" + sid.intValue() );
	    }
	}

	if(snames.size() > 0)
	{
	    edata.addCluster(wrd, ExprData.SpotName, snames);
	}
    }

    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------
    // ===== Word Index =======================================================
    //
    // (lifted from JustOClust - try to keep concurrent)
    //
    // for each unique word in a set of annotations
    //   store all of the positions of that word in each of the annotations
    //
    // ------------------------------------------------------------------------
    // ------------------------------------------------------------------------

    private class WordIndexHeader
    {
	public String  word;

	public int     word_id;  // allocated automatically

	public Vector  entries;  // Vector of WordIndexEntry's

	public Vector  has_substrings;   // Vector of WordIndexEntry's

	public Vector  is_substring_of;  // Vector of WordIndexEntry's

	public WordIndexHeader(String w, int wid)
	{
	    word = w;
	    word_id = wid;
	    entries = new Vector();

	    has_substrings = new Vector();
	    is_substring_of = new Vector();
	}

	public int getNumEntries() { return entries.size(); }
    }


    private class WordIndexEntry
    {
	int[]   index;    // the index of where the word occur(s) in 'anno_id'
	int     anno_id;
    }
    
    private class WordIndex
    {
	private Hashtable word_to_wih; // maps 'word' to WordIndexHeader
	private int next_word_id = 0;

	public WordIndex()
	{
	    word_to_wih = new Hashtable();
	}

	public Enumeration getNames() 
	{
	    return word_to_wih.keys();
	}

	public WordIndexHeader getEntry(String name)
	{
	    return (WordIndexHeader) word_to_wih.get(name);
	}
	
	public int getNumWords() { return word_to_wih.size(); }

	// record the occurence of a word
	//
	public void addEntry(String name, int anno_id, int index)
	{
	    WordIndexHeader wih = getEntry(name);
	    
	    if(wih == null)
	    {
		// this word has not been seen before...
		wih = new WordIndexHeader(name, next_word_id++);
		
		// locate any of the existing words which are substrings of this word
		// 
		for (Enumeration e = word_to_wih.keys(); e.hasMoreElements() ;) 
		{
		    String oname = (String) e.nextElement();
		    if(name.indexOf(oname) >= 0)
		    {
			WordIndexHeader owih = getEntry(oname);
			wih.has_substrings.addElement(owih);
			owih.is_substring_of.addElement(wih);
		    }
		    
		}		
		
		// locate any of the existing words which this word is a substring of
		//
		for (Enumeration e = word_to_wih.keys(); e.hasMoreElements() ;) 
		{
		    String oname = (String) e.nextElement();
		    if(oname.indexOf(name) >= 0)
		    {
			WordIndexHeader owih = getEntry(oname);
			owih.has_substrings.addElement(wih);
			wih.is_substring_of.addElement(owih);
		    }
		    
		}	

		// and store the header into the hashtable
		word_to_wih.put(name, wih);

	    }

	    Vector wiev = wih.entries;
	    WordIndexEntry wie = null;

	    // find the entry corresponding to this anno_id (if any)
	    for(int e=0; e < wiev.size(); e++)
	    {
		if(((WordIndexEntry) wiev.elementAt(e)).anno_id == anno_id)
		{
		    wie = (WordIndexEntry) wiev.elementAt(e);
		    break;
		}
	    }

	    // if we didn't find an existing entry, create one
	    if(wie == null)
	    {
		wie = new WordIndexEntry();
		wie.anno_id = anno_id;
		wie.index   = new int[0];

		wiev.addElement(wie);
	    }

	    // add the index to the existing array...
	    
	    int[] tmp = new int[wie.index.length + 1];
	    for(int t=0; t < wie.index.length; t++)
		tmp[t] = wie.index[t];
	    tmp[wie.index.length] = index;

	    wie.index = tmp;

	    // System.out.println("word '" + wih.word + "' in anno " + wie.anno_id + " at index " + index);
	}
	
	// merges all of the WordIndexEntry's in 'entries' with the existing entries for 'name'
	//
	public void addEntries(String name, Vector entries)
	{
	    for(int e=0; e < entries.size(); e++)
	    {
		WordIndexEntry wie = (WordIndexEntry) entries.elementAt(e);
		for(int i=0; i < wie.index.length; i++)
		    addEntry(name, wie.anno_id, wie.index[i]);
	    }
	}

	// returns a vector of headers for words which occur more than once
	// the headers are sorted by the repeat frequency (highest first)
	//
	public WordIndexHeader[] getRepeats()
	{
	    Vector reps = new Vector();

	    // find any word with more than 1 entry...

	    for (Enumeration e = word_to_wih.keys(); e.hasMoreElements() ;) 
	    {
		String name = (String) e.nextElement();
		WordIndexHeader wih = getEntry(name);
		Vector wiev = wih.entries;
		if(wiev != null)
		{
		    if(wiev.size() > 1)
		    {
			reps.addElement( wih );
		    }
		}
	    }

	    // convert to an array and sort by RepFreq

	    WordIndexHeader[] reps_a = (WordIndexHeader[]) reps.toArray(new WordIndexHeader[0]);
	    
	    java.util.Arrays.sort(reps_a, new SortByRepeatFreq());
	    
	    return reps_a;
	}

	// debugging
	public void listWords()
	{
	    for (Enumeration w = word_to_wih.keys(); w.hasMoreElements() ;) 
	    {
		String name = (String) w.nextElement();
		WordIndexHeader wih = getEntry(name);
		int total_occ = 0;
		for(int e=0; e < wih.entries.size(); e++)
		{
		    total_occ += ((WordIndexEntry) wih.entries.elementAt(e)).index.length;
		}
		System.out.println(wih.word + " x " + wih.entries.size() + " (" + total_occ + ")");
	    }

	}

	// merge the WordIndexEntry's for any word with those of the word of which it is a substring
	// 
	// i.e. if  'human' and 'subhuman' are both words, then copy all elements in
	//      the vector recording the occurences of 'subhuman' into the vector recording the occurences of 'human'
	//
	public void mergeSubstrings()
	{
	    for (Enumeration e = word_to_wih.keys(); e.hasMoreElements() ;) 
	    {
		String name = (String) e.nextElement();
		WordIndexHeader wih = getEntry(name);
	
		if(wih.has_substrings.size() > 0)
		{
		    for(int ss=0; ss < wih.has_substrings.size(); ss++)
		    {
			WordIndexHeader swih = (WordIndexHeader) wih.has_substrings.elementAt(ss);

			addEntries(swih.word, wih.entries);
		    }
		}

	    }
	}

	private class SortByRepeatFreq implements java.util.Comparator
	{
	    public int compare(Object o1, Object o2)
	    {
		WordIndexHeader wih1 = (WordIndexHeader) o1;
		WordIndexHeader wih2 = (WordIndexHeader) o2;

		return (wih2.entries.size() - wih1.entries.size());
	    }

	    public boolean equals(Object o) { return false; }
	}

    }

    

    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  gubbins
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    String cur_path = null;

    int total_tokens = 0;

    private maxdView mview;
    private DataPlot dplot;
    private ExprData edata;
    private AnnotationLoader anlo;

    private JTextField top_N_tok_jtf, delims_jtf;
    private JLabel names_label = null;
    private boolean at_most_N, all_tokens, sort_alpha, case_sens;
    private JComboBox source_jcb;
    private NameTagSelector source_nts;

    private JFrame     frame = null;
}
