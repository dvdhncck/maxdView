import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import javax.swing.border.*;
import javax.swing.event.*;

public class TagEditor
{
    public TagEditor(final maxdView mview_, final int x, final int y, final int spot_id_)
    {
	this(mview_, x, y, spot_id_, 0);
    }

    public TagEditor(final maxdView mview_, final int x, final int y, final int spot_id_, final int cur_gene_)
    {
	mview = mview_;
	spot_id = spot_id_;
	cur_gene = cur_gene_;

	
	current_data_ht = new Hashtable();

	edata = mview.getExprData();

	edit_popup = new JFrame("Name & Attribute Editor");

	mview.decorateFrame(edit_popup);

	final JPanel panel = new JPanel();
	panel.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

	probe_jtf = new JTextField(32);
	spot_jtf  = new JTextField(32);
	gene_jtf  = new JTextField(32);

	GridBagConstraints c = null;
	GridBagLayout gbag = new GridBagLayout();
	panel.setLayout(gbag);

	// -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

	JPanel i_panel = new JPanel();
	GridBagLayout i_gbag = new GridBagLayout();
	i_panel.setLayout(i_gbag);

	Color title_colour = new JLabel().getForeground().brighter();
	
	Font f = new JLabel().getFont();
	Font title_font = new Font(f.getName(), Font.BOLD, f.getSize() + 2);
	Font bfont = new Font(f.getName(), Font.PLAIN, f.getSize() - 2);
	    
	TitledBorder title = BorderFactory.createTitledBorder("  Genes  ");
	title.setTitleColor(title_colour);
	title.setTitleFont(title_font);
	i_panel.setBorder(title);

	
	// -- -- -- -- -- -- -- -- --

	int line = 0;

	final String[] gnames = edata.getGeneNames(spot_id);
	
	final int ngnames = (gnames == null ? 0 : gnames.length);
	    
	// System.out.println("editing " + cur_gene + " in spot "+ spot_id + " which has " + ngnames + " genes");

	ExprData.TagAttrs ta = edata.getGeneTagAttrs();
	      
	// -- -- -- -- -- -- -- -- --

	JPanel bwrap = new JPanel();
	bwrap.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));

	{
	    final JLabel gene_l = new JLabel( (ngnames == 0) ? "(no gene)" : ((cur_gene+1) + " of " + ngnames));
	    
	    JButton jb = new JButton("<");
	    jb.setFont(bfont);
	    bwrap.add(jb);
	    jb.setEnabled(cur_gene > 0);
	    jb.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			if(safeToAlterDisplay("display a different Gene"))
			{
			
			    if(--cur_gene < 0)
				cur_gene = ngnames-1;
			    
			    Point cur_pos = panel.getLocationOnScreen();
			    edit_popup.setVisible(false);
			    
			    new TagEditor(mview, cur_pos.x, cur_pos.y, spot_id, cur_gene );
			}
			
		    }
		});
	    
	    gene_l.setFont(bfont);
	    bwrap.add(gene_l);
	    
	    
	    jb = new JButton(">");
	    jb.setEnabled(cur_gene < (ngnames-1));
	    jb.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			if(safeToAlterDisplay("display a different Gene"))
			{
			    if(++cur_gene >= ngnames)
				cur_gene = 0;
			    
			    Point cur_pos = panel.getLocationOnScreen();
			    edit_popup.setVisible(false);
			    
			    new TagEditor(mview, cur_pos.x, cur_pos.y, spot_id, cur_gene );
			}
			
		    }
		});
	    jb.setFont(bfont);
	    bwrap.add(jb);

	    
	    
	}

	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.WEST;
	//c.fill = GridBagConstraints.HORIZONTAL;
	//c.weightx = 1.0;
	c.gridx = 0;
	c.gridy = line++;
	i_gbag.setConstraints(bwrap, c);
	i_panel.add(bwrap);

	// -- -- -- -- -- -- -- -- --

	JPanel ii_panel = new JPanel();
	GridBagLayout ii_gbag = new GridBagLayout();
	ii_panel.setLayout(ii_gbag);

	gene_jtf.setEditable(false);
	{
	    
	    JLabel label = new JLabel("Name ");
	    //label.setForeground(label_col);
	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.EAST;
	    ii_gbag.setConstraints(label, c);
	    ii_panel.add(label);
	    
	    if(ngnames > cur_gene)
		gene_jtf.setText(gnames[ cur_gene ]);
	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.weightx = 1.0;
	    c.gridx = 1;
	    ii_gbag.setConstraints(gene_jtf, c);
	    ii_panel.add(gene_jtf);
	
	    // -- -- -- -- -- -- -- -- --
	    
	    // any gene tag attrs
	    
	    boolean valid = ((ngnames > cur_gene) && (gnames[cur_gene] != null));

	    for(int a=0; a < ta.getNumAttrs(); a++)
	    {
		label = new JLabel(ta.getAttrName(a) + " ");
		//label.setForeground(label_col);
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.EAST;
		c.gridy = a+1;
		ii_gbag.setConstraints(label, c);
		ii_panel.add(label);
		
		JTextField ta_jtf = new JTextField(32);
		
		ta_jtf.setEnabled(valid);
		if(valid)
		{
		    ta_jtf.setText(ta.getTagAttr(gnames[cur_gene], a));
		    
		    ta_jtf.getDocument().addDocumentListener( new TagEditDocListener( "Gene." + ta.getAttrName(a) ) );
		}
		
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		c.gridx = 1;
		c.gridy = a+1;
		ii_gbag.setConstraints(ta_jtf, c);
		ii_panel.add(ta_jtf);
	    }
	}

	JScrollPane ii_jsp = new JScrollPane( ii_panel );
  
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.WEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.weightx = 10.0;
	c.gridx = 0;
	c.gridy = line++;
	i_gbag.setConstraints( ii_jsp, c);
	i_panel.add( ii_jsp );

	// -- -- -- -- -- -- -- -- --
	

	bwrap = new JPanel();
	bwrap.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));

	{
	    final JButton cg_jb = new JButton("Change Gene");
	    cg_jb.setFont(bfont);
	    cg_jb.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			if(changeGeneOrProbe(true))
			{
			    updateNamesAndTagAttrs();

			    Point cur_pos = panel.getLocationOnScreen();
			    edit_popup.setVisible(false);
			    
			    new TagEditor(mview, cur_pos.x, cur_pos.y, spot_id, cur_gene );
			}

		    }
		});
	    bwrap.add(cg_jb);
	   
	    final JButton add_jb = new JButton("Add Gene");
	    add_jb.setFont(bfont);
	    add_jb.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			if(safeToAlterDisplay("add a Gene"))
			{
			    if(addGene())
			    {
				Point cur_pos = panel.getLocationOnScreen();
				
				edit_popup.setVisible(false);
				
				String[] gns = edata.getGeneNames(spot_id);
				int new_gene_number = 0;
				if(gns != null) 
				    new_gene_number = edata.getGeneNames(spot_id).length - 1;
				if(new_gene_number < 0)
				    new_gene_number = 0;

				new TagEditor(mview, cur_pos.x, cur_pos.y, spot_id, new_gene_number );
			    }
			}
		    }
		});
	    bwrap.add(add_jb);
	    
	    final JButton rem_jb = new JButton("Remove Gene");
	    rem_jb.setFont(bfont);
	    rem_jb.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			if(ngnames > 0)
			{
			    if(safeToAlterDisplay("remove a Gene"))
			    {
				if(removeGene())
				{
				    Point cur_pos = panel.getLocationOnScreen();
				    
				    edit_popup.setVisible(false);
				    
				    new TagEditor(mview, cur_pos.x, cur_pos.y, spot_id);
				}
			    }
			}
			else
			    mview.alertMessage("No Genes to remove");
		    }
		});
	    bwrap.add(rem_jb);
	    
	    JButton add_att_jb = new JButton("Add attribute");
	    add_att_jb.setFont(bfont);
	    add_att_jb.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			try
			{
			    if(safeToAlterDisplay("add an attribute"))
			    {
				ExprData.TagAttrs gta = edata.getGeneTagAttrs();
				String new_name = mview.getString("New Gene attribute name");
				Point cur_pos = panel.getLocationOnScreen();
				edit_popup.setVisible(false);
				gta.addAttr(new_name);
				new TagEditor(mview, cur_pos.x, cur_pos.y, spot_id);
			    }
			}
			catch(UserInputCancelled uic)
			{
			}
		    }
		});
	    bwrap.add(add_att_jb);

	    JButton rem_att_jb = new JButton("Remove attribute");
	    rem_att_jb.setFont(bfont);
	    rem_att_jb.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			if(safeToAlterDisplay("remove an attribute"))
			{
			    if(removeAttribute( edata.getGeneTagAttrs() ))
			    {
				Point cur_pos = panel.getLocationOnScreen();
				edit_popup.setVisible(false);
				new TagEditor(mview, cur_pos.x, cur_pos.y, spot_id);
			    }
			}
		    }
		});
	    bwrap.add(rem_att_jb);
	}

	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.WEST;
	//c.fill = GridBagConstraints.HORIZONTAL;
	//c.weightx = 1.0;
	c.gridx = 0;
	c.gridy = line++;
	i_gbag.setConstraints(bwrap, c);
	i_panel.add(bwrap);

	c = new GridBagConstraints();
	c.fill = GridBagConstraints.BOTH;
	c.weightx = 10.0;
	c.weighty = 1.0;
	gbag.setConstraints( i_panel, c);
	panel.add( i_panel);

	
	// end of gui for gene names & attrs

	// -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --
	
	i_panel = new JPanel();
	i_gbag = new GridBagLayout();
	i_panel.setLayout(i_gbag);
	line = 0;
	
	String pname = edata.getProbeName(spot_id);

	title = BorderFactory.createTitledBorder("  Probe  ");
	title.setTitleColor(title_colour);
	title.setTitleFont(title_font);
	i_panel.setBorder(title);

	ii_panel = new JPanel();
	ii_gbag = new GridBagLayout();
	ii_panel.setLayout(ii_gbag);

	JLabel label = new JLabel("Name ");
	//label.setForeground(label_col);
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.EAST;
	ii_gbag.setConstraints(label, c);
	ii_panel.add(label);
	
	if(pname != null)
	    probe_jtf.setText(pname);
	probe_jtf.setEditable(false);
	//probe_jtf.getDocument().addDocumentListener( new TagEditDocListener( "Probe.Name" ));
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.WEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.weightx = 1.0;
	c.gridx = 1;
	ii_gbag.setConstraints(probe_jtf, c);
	ii_panel.add(probe_jtf);
	
	// -- -- -- -- -- -- -- -- --
	// any probe tag attrs

	boolean valid = (pname != null);

	ta = edata.getProbeTagAttrs();

	for(int a=0; a < ta.getNumAttrs(); a++)
	{
	    label = new JLabel(ta.getAttrName(a) + " ");
	    //label.setForeground(label_col);
	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.EAST;
	    c.gridy = a + 1;
	    ii_gbag.setConstraints(label, c);
	    ii_panel.add(label);
	    
	    JTextField pta_jtf = new JTextField(32);
	    pta_jtf.setEnabled(valid);
	    if(valid)
	    {
		pta_jtf.setText(ta.getTagAttr(pname, a));
		pta_jtf.getDocument().addDocumentListener( new TagEditDocListener( "Probe." + ta.getAttrName(a) ) );
	    }
	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.weightx = 1.0;
	    c.gridx = 1;
	    c.gridy = a + 1;
	    ii_gbag.setConstraints(pta_jtf, c);
	    ii_panel.add(pta_jtf);
	}
	
	ii_jsp = new JScrollPane( ii_panel );
  
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.WEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.weightx = 10.0;
	c.gridx = 0;
	c.gridy = line++;
	i_gbag.setConstraints( ii_jsp, c);
	i_panel.add( ii_jsp );

	{
	    bwrap = new JPanel();
	    
	    final JButton cp_jb = new JButton("Change Probe");
	    cp_jb.setFont(bfont);
	    cp_jb.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			if(changeGeneOrProbe(false))
			{
			    // write the new name back to edata
			    updateNamesAndTagAttrs();

			    // and redisplay the panel with the correct tagattrs for this probe name
			    Point cur_pos = panel.getLocationOnScreen();
			    edit_popup.setVisible(false);
			    
			    new TagEditor(mview, cur_pos.x, cur_pos.y, spot_id, cur_gene );
			}
		    }
		});
	    bwrap.add(cp_jb);
	    
	    JButton add_att_jb = new JButton("Add attribute");
	    add_att_jb.setFont(bfont);
	    add_att_jb.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			try
			{
			    if(safeToAlterDisplay("add an attribute"))
			    {
				ExprData.TagAttrs pta = edata.getProbeTagAttrs();
				String new_name = mview.getString("New Probe attribute name");
				Point cur_pos = panel.getLocationOnScreen();
				edit_popup.setVisible(false);
				pta.addAttr(new_name);
				new TagEditor(mview, cur_pos.x, cur_pos.y, spot_id);
			    }
			}
			catch(UserInputCancelled uic)
			{
			}
		    }
		});
	    bwrap.add(add_att_jb);
	    
	    JButton rem_att_jb = new JButton("Remove attribute");
	    rem_att_jb.setFont(bfont);
	    rem_att_jb.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			if(safeToAlterDisplay("remove an attribute"))
			{
			    if(removeAttribute( edata.getProbeTagAttrs() ))
			    {
				Point cur_pos = panel.getLocationOnScreen();
				edit_popup.setVisible(false);
				new TagEditor(mview, cur_pos.x, cur_pos.y, spot_id);
			    }
			}
		    }
		});
	    bwrap.add(rem_att_jb);

	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.weightx = 1.0;
	    c.gridx = 0;
	    c.gridy = line++;
	    i_gbag.setConstraints(bwrap, c);
	    i_panel.add(bwrap);
	}
	
	c = new GridBagConstraints();
	c.fill = GridBagConstraints.BOTH;
	c.gridy = 1;
	c.weightx = 10.0;
	c.weighty = 1.0;
	gbag.setConstraints( i_panel, c);
	panel.add( i_panel);

	// end of gui for probe names & attrs

	// -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- -- --

	line = 0;
	i_panel = new JPanel();
	i_gbag = new GridBagLayout();
	i_panel.setLayout(i_gbag);
	
	title = BorderFactory.createTitledBorder("  Spot  ");
	title.setTitleColor(title_colour);
	title.setTitleFont(title_font);
	i_panel.setBorder(title);

	ii_panel = new JPanel();
	ii_gbag = new GridBagLayout();
	ii_panel.setLayout(ii_gbag);

	label = new JLabel("Name ");
	//label.setForeground(label_col);
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.EAST;
	ii_gbag.setConstraints(label, c);
	ii_panel.add(label);
	
	spot_jtf.setText(edata.getSpotName(spot_id));
	spot_jtf.setEditable(false);
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.WEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.weightx = 1.0;
	c.gridx = 1;
	ii_gbag.setConstraints(spot_jtf, c);
	ii_panel.add(spot_jtf);
	
	// -- -- -- -- -- -- -- -- --
	// any spot tag attrs

	ta = edata.getSpotTagAttrs();

	for(int a=0; a < ta.getNumAttrs(); a++)
	{
	    label = new JLabel(ta.getAttrName(a) + " ");
	    //label.setForeground(label_col);
	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.EAST;
	    c.gridy = a + 1;
	    ii_gbag.setConstraints(label, c);
	    ii_panel.add(label);
	    
	    JTextField sta_jtf = new JTextField(32);
	    sta_jtf.setText(ta.getTagAttr(edata.getSpotName(spot_id), a));
	    sta_jtf.getDocument().addDocumentListener( new TagEditDocListener( "Spot." + ta.getAttrName(a) ) );
	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.weightx = 1.0;
	    c.gridx = 1;
	    c.gridy = a + 1;
	    ii_gbag.setConstraints(sta_jtf, c);
	    ii_panel.add(sta_jtf);
	}

	ii_jsp = new JScrollPane( ii_panel );
  
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.WEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.weightx = 10.0;
	c.gridx = 0;
	c.gridwidth = 2;
	c.gridy = line++;
	i_gbag.setConstraints( ii_jsp, c);
	i_panel.add( ii_jsp );

	{
	    bwrap = new JPanel();

	    JButton add_att_jb = new JButton("Add attribute");
	    add_att_jb.setFont(bfont);
	    add_att_jb.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			try
			{
			   if(safeToAlterDisplay("add an attribute"))
			   {
			       ExprData.TagAttrs sta = edata.getSpotTagAttrs();
			       String new_name = mview.getString("New Spot attribute name");
			       Point cur_pos = panel.getLocationOnScreen();
			       edit_popup.setVisible(false);
			       sta.addAttr(new_name);
			       new TagEditor(mview, cur_pos.x, cur_pos.y, spot_id);
			   }
			}
			catch(UserInputCancelled uic)
			{
			}
		    }
		});
	    bwrap.add(add_att_jb);
	    
	    JButton rem_att_jb = new JButton("Remove attribute");
	    rem_att_jb.setFont(bfont);
	    rem_att_jb.addActionListener(new ActionListener() 
		{ 
		    public void actionPerformed(ActionEvent e) 
		    {
			if(safeToAlterDisplay("remove an attribute"))
			{
			    if(removeAttribute( edata.getSpotTagAttrs() ))
			    {
				Point cur_pos = panel.getLocationOnScreen();
				edit_popup.setVisible(false);
				new TagEditor(mview, cur_pos.x, cur_pos.y, spot_id);
			    }
			}
		    }
		});
	    bwrap.add(rem_att_jb);

	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.weightx = 1.0;
	    c.gridx = 0;
	    c.gridwidth = 2;
	    c.gridy = line++;
	    i_gbag.setConstraints(bwrap, c);
	    i_panel.add(bwrap);
	}


	c = new GridBagConstraints();
	c.fill = GridBagConstraints.BOTH;
	c.gridy = 2;
	c.weightx = 10.0;
	c.weighty = 1.0;
	gbag.setConstraints( i_panel, c);
	panel.add( i_panel);	    

	// ===========================================

	JPanel wrapper = new JPanel();
	wrapper.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
	GridBagLayout wbag = new GridBagLayout();
	wrapper.setLayout(wbag);
	apply_jb = new JButton("Apply");
	apply_jb.setEnabled(false);
	apply_jb.addActionListener(new ActionListener() 
	    { 
		public void actionPerformed(ActionEvent e) 
		{
		    /*
		    String[] gene_names = new String[gene_jtf.length];
		    for(int n=0; n < gene_jtf.length; n++)
			gene_names[n] = gene_jtf[n].getText();

		    if(changeLabels(spot_id, 
				    spot_jtf.getText(), spot_comment_jtf.getText(), 
				    probe_jtf.getText(), gene_names) == true)
			edit_popup.setVisible(false);
		    */

		    if(updateNamesAndTagAttrs())
			edit_popup.setVisible(false);
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 0;
	c.weightx = 1.0;
	wbag.setConstraints(apply_jb, c);
	wrapper.add(apply_jb);

	JButton help_jb = new JButton("Help");
	help_jb.addActionListener(new ActionListener() 
	    { 
		public void actionPerformed(ActionEvent e) 
		{
		    mview.getHelpTopic("NameTagEditor");
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 1;
	c.weightx = 1.0;
	wbag.setConstraints(help_jb, c);
	wrapper.add(help_jb);
	
	cancel_jb = new JButton("Close");
	cancel_jb.addActionListener(new ActionListener() 
	    { 
		public void actionPerformed(ActionEvent e) 
		{
		    edit_popup.setVisible(false);
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 2;
	c.weightx = 1.0;
	wbag.setConstraints(cancel_jb, c);
	wrapper.add(cancel_jb);
	
	// -- -- -- -- -- -- -- -- --

	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.SOUTH;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.gridy = 3;
	gbag.setConstraints(wrapper, c);
	panel.add(wrapper);

	// ===========================================

	edit_popup.getContentPane().add(panel);
	edit_popup.pack();
	
	if(x < 0)
	{
	    Point pt = mview.getDataPlot().getLocationOnScreen();
	    edit_popup.setLocation(pt.x, pt.y);
	}
	else
	{
	    edit_popup.setLocation(x, y);
	}

	edit_popup.setVisible(true);
	
    }

    private void anUpdateHasHappened()
    {
	apply_jb.setEnabled(true);
	cancel_jb.setText("Cancel");
    }

    private boolean safeToAlterDisplay(String msg)
    {
	if(apply_jb.isEnabled())
	{
	    if(mview.infoQuestion("You must 'Apply' the changes you have made\nbefore you can " + 
				  msg, "OK", "Apply now and continue") == 1)
	    {
		updateNamesAndTagAttrs();
		apply_jb.setEnabled(false);
		cancel_jb.setText("Close");
		return true;
	    }
	    return false;
	}
	return true;
    }

    class TagEditDocListener implements DocumentListener 
    {
	private String name;

	public TagEditDocListener(String n)
	{
	    name = n;
	}
	
	public void insertUpdate(DocumentEvent e)  { propagate(e); }
	public void removeUpdate(DocumentEvent e)  { propagate(e); }
	public void changedUpdate(DocumentEvent e) { propagate(e); }

	private void propagate(DocumentEvent e)
	{
	    try
	    {
		String t = e.getDocument().getText(0, e.getDocument().getLength());
		
		// System.out.println(name + "=" + t);
		
		// store the jtf values in a hash table until they are needed when then
		// "apply" button is pressed

		current_data_ht.put(name, t);

		anUpdateHasHappened();
	    }
	    catch (javax.swing.text.BadLocationException ble)
	    {
		System.out.println("wierd string....\n");
	    }
	}
    }
    
    // =================================================================================================================


    // TODO: gene labels should be an array....
    //
    // !!NOT USED!!
    //
    private boolean changeLabels(int spot_id, String spot, String spot_comment, String probe, String[] genes)
    {
	ExprData.DataTags dtags = edata.getMasterDataTags();
	
	int check_s_id = edata.getIndexBySpotName(spot);
	
	if(check_s_id >= 0)
	    if(check_s_id != spot_id)
	    {
		mview.alertMessage("Update not allowed.\nSpot name '" + spot + "' already exists.\nSpot names must be unique.");
		return false;
	    }
	
	if(spot.length() == 0)
	{
	    mview.alertMessage("Spots must have a name.");
	    return false;
	}

	dtags.setSpotName(spot_id, spot);
	
	String cur_probe = edata.getProbeName(spot_id);

	// has the probe name changed?
	if(probe.equals(cur_probe) == false)
	{
	    if(probe.length() > 0)
	    {
		//System.out.println("probe name changed...");
		
		// yes, are there any other probes with the same name?
		int others = 0;
		for(int pn=0; pn < edata.getNumSpots(); pn++)
		{
		    if(cur_probe.equals(edata.getProbeName(pn)))
			others++;
		}
		
		//System.out.println("this probe name occurs " + others +  " times...");
		
		if(others > 1) // others == 1 means just this one, >1 means more probes with this name
		{
		    String question = (others == 2) ? "There is one other spot which has" :  "There are " + (others-1) + " other spots which have";
		    question += " the same Probe name\nChange all occurences?";
		    
		    if(mview.infoQuestion(question, "Yes, change all", "No, just this one") == 0)
		    {
			// change all.....
			for(int pn=0; pn < edata.getNumSpots(); pn++)
			{
			    if(cur_probe.equals(edata.getProbeName(pn)))
			    {
				dtags.setProbeName(pn, probe);
			    }
			}
		    }
		}
	    }
	    // and change this one....
	    dtags.setProbeName(spot_id, probe);

	}

	String[] cur_genes = edata.getGeneNames(spot_id);
	boolean same = true;

	if((cur_genes == null) && (genes != null))
	    same = false;
	
	if((genes == null) && (cur_genes != null))
	    same = false;
	
	if(same)
	    if(cur_genes.length != genes.length)
		same = false;
	
	int gn = 0;
	while((same) &&  (gn < genes.length))
	{
	    if(cur_genes[gn] == null)
	    {
		same = false;
	    }
	    else
	    {
		if(!cur_genes[gn].equals(genes[gn]))
		    same = false;
	    }
	    gn++;
	}

	if(!same)
	{
	    // are any of the gene names found elsewhere?
	    //
	    
	    // change the names
	    
	    edata.setGeneNames(spot_id, genes);
	    
	    
	}
	
	    /*
	// have any of the gene names?
	if(genes.equals(cur_genes) == false)
	{

	    if(genes.length() > 0)
	    {
		// yes, are there any other gene with the same name?
		int others = 0;
		for(int gn=0; gn < edata.getNumSpots(); gn++)
		{
		    if(cur_genes.equals(edata.getGeneName(gn)))
			others++;
		}
		
		// System.out.println("this probe name occurs " + others +  " times...");
		
		if(others > 1) // others == 1 means just this one, >1 means more probes with this name
		{
		    String question = (others == 2) ? "There is one other spot which has" :  "There are " + (others-1) + " other spots which have";
		    question += " the same Gene name(s)\nChange all occurences?";
		    
		    if(mview.infoQuestion(question, "Yes, change all", "No, just this one") == 0)
		    {
			// change all.....
			for(int gn=0; gn < edata.getNumSpots(); gn++)
			{
			    if(cur_genes.equals(edata.getGeneName(gn)))
			    {
				dtags.setGeneNames(gn, genes_a);
			    }
			}
		    }
		}
	    }
	    // and change this one....
	    dtags.setGeneNames(spot_id, genes_a);

	}
	    */


	// force the ExprData to check for the longest name
	//dtags.setGeneNames(dtags.gene_names);
	edata.generateDataUpdate(ExprData.NameChanged); 

	//updateDisplay();

	return true;
    }

    private boolean updateNamesAndTagAttrs()
    {
	// gene Name and TagAttrs

	String gname = (String) current_data_ht.get("Gene.Name");
	if(gname != null)
	    edata.setGeneName(spot_id, cur_gene, gname); 
	else
	    // name not changed, grab the current name
	{
	    String[] gns = edata.getGeneNames(spot_id);
	    gname = (gns == null) ? null: gns[cur_gene];
	}
	
	ExprData.TagAttrs ta = edata.getGeneTagAttrs();
	for(int a=0; a < ta.getNumAttrs(); a++)
	{
	    String v = (String) current_data_ht.get("Gene." + ta.getAttrName(a));
	    if(v != null)
	    {
		if((gname == null) || (gname.length() == 0))
		{
		    mview.alertMessage("This Gene cannot have an attribute as the Gene has no name");
		    break;
		}
		else
		{
		    ta.setTagAttr(gname, a, v);
		}
	    }
	}

	
	// probe Name and TagAttrs

	String pname = (String) current_data_ht.get("Probe.Name");
	if(pname != null)
	    edata.setProbeName(spot_id, pname);
	else
	    // name not changed, grab the current name
	    pname = edata.getProbeName(spot_id);

	ta = edata.getProbeTagAttrs();
	for(int a=0; a < ta.getNumAttrs(); a++)
	{
	    if((pname == null) || (pname.length() == 0))
	    {
		mview.alertMessage("This Probe cannot have an attribute as the Probe has no name");
		break;
	    }
	    else
	    {
		String v = (String) current_data_ht.get("Probe." + ta.getAttrName(a));
		if(v != null)
		{
		    ta.setTagAttr(pname, a, v);
		}
	    }
	}
	
	ta = edata.getSpotTagAttrs();
	for(int a=0; a < ta.getNumAttrs(); a++)
	{
	    String v = (String) current_data_ht.get("Spot." + ta.getAttrName(a));
	    if(v != null)
		ta.setTagAttr(edata.getSpotName(spot_id), a, v);
	}
	
	edata.generateDataUpdate(ExprData.NameChanged);
	
	return true;
    }
    
    // =================================================================================================================

    private boolean removeAttribute( ExprData.TagAttrs ta )
    {
	// generate a list of attr names
	String[] a_names = ta.getAttrNames();
	
	if((a_names == null) || (a_names.length == 0))
	{
	    mview.alertMessage("No attributes to remove");
	}
	else
	{
	    try
	    {
		int att = 0;

		if(a_names.length > 1)
		{
		    att = mview.getChoice("Remove attribute:", a_names); 
		}
		
		int count = ta.getTagAttrCount(att);
		
		String msg = "Really remove attribute '" +a_names[att] + "' ?\n(it contains ";
		
		if(count == 0)
		    msg += "no entries for any names)";
		if(count == 1)
		    msg += "one entry)";
		if(count > 1)
		    msg += "entries for " + count + " names)";
		
		if(mview.infoQuestion(msg, "Yes", "No") == 0)
		{
		    ta.removeAttr(att);
		    return true;
		}
	    }
	    catch(UserInputCancelled uic)
	    {
	    }
	}

	return false;
    }

    // =================================================================================================================
    // =================================================================================================================

    private boolean removeGene()
    {
	String[] gnames = edata.getGeneNames(spot_id);
	if(gnames != null)
	{
	    // System.out.println("removing gene no. " + cur_gene);

	    final int ngn = gnames.length;
	    if(cur_gene < ngn)
	    {
		String[] new_gnames = (ngn == 1) ? null : new String[ngn - 1];
		
		int p = 0;
		for(int i=0; i < ngn; i++)
		    if(i != cur_gene)
			new_gnames[p++] = gnames[i];
		
		edata.setGeneNames(spot_id, new_gnames);

		return true;
	    }
	}
	return false;
    }

    private boolean addGene()
    {
	try
	{
	    final String[] choices = { "Enter a new name", "Pick from existing names" };

	    String result = null;

	    int opt = mview.getChoice("Add Gene", choices, 0);

	    if(opt == 0)
	    {
		result = mview.getString("New name:");
	    }
	    else
	    {
		Hashtable ht = edata.getGeneNameHashtable();
		java.util.Enumeration e = ht.keys();
		
		String[] names = new String[ ht.size() ];
		int p =0;
		while(e.hasMoreElements())
		{
		    names[p++] = (String) e.nextElement();
		}
		
		Arrays.sort(names);
		
		//System.out.println("there are " + names.length + " names to pick from");
		
	        result = pickFromList( "Pick a Gene" , names );
	    }

	    String[] gnames = edata.getGeneNames(spot_id);
	    String[] new_gnames = null;
	    if(gnames != null)
	    {
		new_gnames = new String[gnames.length+1];
		for(int g=0; g< gnames.length; g++)
		    new_gnames[g] =  gnames[g];
		new_gnames[gnames.length] = result;
	    }
	    else
	    {
		new_gnames = new String[1];
		new_gnames[0] = result;
	    }
	    edata.setGeneNames(spot_id, new_gnames);

	    return true;
	}
	catch(UserInputCancelled uic)
	{
	    return false;
	}
    }
    
    // =================================================================================================================
    // =================================================================================================================
    
    // returns true if the change was made...

    public boolean changeGeneOrProbe(boolean is_gene)
    {
	try
	{
	    final String[] choices = { "Enter a new name", "Pick from existing names" };

	    int opt = mview.getChoice("Change " + (is_gene ? "Gene" : "Probe"), choices, 0);

	    String result = null;

	    if(opt == 0)
	    {
		result = mview.getString("New name:");
	    }
	    else
	    {
		Hashtable ht = is_gene ? edata.getGeneNameHashtable() : edata.getProbeNameHashtable();
		java.util.Enumeration e = ht.keys();
		
		String[] names = new String[ ht.size() ];
		int p =0;
		while(e.hasMoreElements())
		{
		    names[p++] = (String) e.nextElement();
		}
		
		Arrays.sort(names);
		
		//System.out.println("there are " + names.length + " names to pick from");
		
	        result = pickFromList( is_gene ? "Pick a Gene" : "Pick a Probe", names );
	    }

	    // store the new name in the current data ready for when "Apply" is pressed
	    current_data_ht.put( is_gene ? "Gene.Name" : "Probe.Name", result);
	    
	    // update the label
	    if(is_gene)
		gene_jtf.setText(result);
	    else
		probe_jtf.setText(result);

	    anUpdateHasHappened();

	    return true;
	}
	catch(UserInputCancelled uic)
	{
	}

	return false;
	
    }
    
    public String pickFromList(final String message, final String[] list_data) throws UserInputCancelled
    {
	JPanel panel = new JPanel();
	panel.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
	GridBagLayout gridbag = new GridBagLayout();
	panel.setLayout(gridbag);

	final JList list = new JList( list_data);
	JScrollPane jsp = new JScrollPane(list);
	jsp.setPreferredSize(new Dimension(100,400));
	
	JLabel label = new JLabel(message);
	label.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
	GridBagConstraints con = new GridBagConstraints();
	con.gridx = 0;
	con.gridy = 0;
	gridbag.setConstraints(label, con);
	panel.add(label);
	
	con = new GridBagConstraints();
	con.gridx = 0;
	con.gridy = 1;
	con.weighty = 8.0;
	con.weightx = 2.0;
	con.fill = GridBagConstraints.BOTH;
	gridbag.setConstraints(jsp, con);
	panel.add(jsp);
	
	Object[] but_opts = new Object[2];	  
	
	but_opts[0] = "OK";
	but_opts[1] = "Cancel";
	
	int res = JOptionPane.showOptionDialog(edit_popup, panel, "Choose", 
					       JOptionPane.YES_NO_CANCEL_OPTION,
					       JOptionPane.PLAIN_MESSAGE, 
					       /*input_icon*/null, 
					       but_opts, 
					       but_opts[1]);
	
	if(res == 0)
	{
	    return (String) list.getSelectedValue();
	}
	else
	{
	    throw new UserInputCancelled(message);
	}
	
    }

    // =================================================================================================================
    // =================================================================================================================

    private JFrame edit_popup;
    private Hashtable current_data_ht;

    private JTextField gene_jtf;
    private JTextField probe_jtf;
    private JTextField spot_jtf;

    private JButton apply_jb, cancel_jb;

    private int spot_id;
    private int cur_gene;

    private maxdView mview;
    private ExprData edata;

}
