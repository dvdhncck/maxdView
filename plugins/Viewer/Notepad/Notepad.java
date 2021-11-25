import javax.swing.*;

import javax.swing.table.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import java.awt.dnd.*;
import java.awt.datatransfer.*;

//
// writes some or all of the expression data in some unspecified XML format
//

public class Notepad implements Plugin, ExprData.ExprDataObserver
{
    
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---  plugin implementation
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    static private int note_list_count = 0;

    final String[] col_names = { "Entity", "Comment" };

    public Notepad(maxdView mview_)
    {
	note_list_count++;

	mview = mview_;
	edata = mview.getExprData();
    }


    public void startPlugin()
    {
	frame = createFrame();
	
	mview.decorateFrame(frame);

	frame.setVisible(true);

	mview.getExprData().addObserver(this);   


	Object[] new_row = new Object[2];
	new_row[0] = DragAndDropEntity.createSpotNameEntity(0);
	new_row[1] = new String();
	(( DefaultTableModel )note_list.getModel()).addRow ( new_row );
    }

    public void stopPlugin()
    {
	cleanUp();
    }

    public PluginInfo getPluginInfo()
    { 
	PluginInfo pinf = 
	   new PluginInfo("Notepad", "viewer", "A temporary workspace for storing names", "", 1, 0, 0);
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
	// should 'check' the id's of things in the list
    }

    public void clusterUpdate(ExprData.ClusterUpdateEvent cue)
    {
    }

    public void measurementUpdate(ExprData.MeasurementUpdateEvent mue)
    {
    }

    public void environmentUpdate(ExprData.EnvironmentUpdateEvent eue)
    {
    }


    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  
    // --- --- ---   Notepad
    // --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  
    // --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  --- --- ---  

    public void cleanUp()
    {
	mview.getExprData().removeObserver(this);
	frame.setVisible(false);

    }

    private JFrame createFrame()
    {
	JFrame frame = new JFrame("Notepad");

	frame.addWindowListener(new WindowAdapter() 
	    {
		public void windowClosing(WindowEvent e)
			      {
				  cleanUp();
			      }
	    });

	panel = new JPanel();
	panel.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
	GridBagLayout gridbag = new GridBagLayout();
	
	panel.setPreferredSize(new Dimension(256, 350));

	panel.setLayout(gridbag);

	/*
	{
	    JPanel wrapper = new JPanel();
	    prev_jb = new JButton("<");
	    wrapper.add(prev_jb);
	    prev_jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			changeMeasurement(-1);
		    }
		});

	    meas_jcb = new JComboBox();
	    wrapper.add(meas_jcb);

	    meas_al = new CustomActionListener();

	    next_jb = new JButton(">");
	    wrapper.add(next_jb);
	    next_jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			changeMeasurement(+1);
		    }
		});

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;

	    gridbag.setConstraints(wrapper, c);
	    panel.add(wrapper);
	}
	*/

	{
	    comment_jta = new JTextArea();

	    JScrollPane scroll_pane = new JScrollPane(comment_jta);

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weighty = 2.0;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.BOTH;

	    gridbag.setConstraints(scroll_pane, c);
	    
	    panel.add(scroll_pane);

	}

	{
	    note_list = new DragAndDropTable();
	    //note_list = new DNDList();

	    note_list.setModel( new CustomTableModel() );
	    
	    note_list.setDropAction(new DragAndDropTable.DropAction()
		{
		    public void dropped(DragAndDropEntity dnde)
		    {
			System.out.println(dnde.toString(edata) + " dropped....");

			Object[] new_row = new Object[2];
			new_row[0] = dnde;
			new_row[1] = new String("[no comment]");
			(( DefaultTableModel )note_list.getModel()).addRow ( new_row );
		    }
		});

	    note_list.setDragAction(new DragAndDropTable.DragAction()
		{
		    public DragAndDropEntity getEntity()
		    {
			int sel_r = note_list.getSelectedRow();
			if(sel_r >= 0)
			{
			    CustomTableModel ctm = (CustomTableModel) note_list.getModel();
			    Vector row_v = (Vector) ctm.getDataVector().elementAt(sel_r);
			    return (DragAndDropEntity) row_v.elementAt(0);
			}
			return null;
		    }
		});

	
	    note_list.setEntityAdaptor(new DragAndDropTable.EntityAdaptor()
		{
		    public String getName(DragAndDropEntity dnde)
		    {
			return dnde.toString(edata);
		    }
		});

	    JScrollPane scroll_pane = new JScrollPane(note_list);

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 1;
	    c.weighty = 10.0;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.BOTH;

	    gridbag.setConstraints(scroll_pane, c);
	    
	    scroll_pane.setPreferredSize(new Dimension(400, 200));
	    panel.add(scroll_pane);

	}

	{ 
	    JPanel wrapper = new JPanel();

	    // ---------------------

	    final JButton sjb = new JButton("Save");
	    wrapper.add(sjb);

	    Font f = sjb.getFont();
	    Font small_font = new Font(f.getName(), f.getStyle(), f.getSize() - 2);
	    sjb.setFont(small_font); 
	   
	    sjb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			saveData();
		    }
		});
	    
	    final JButton cjb = new JButton("Clear");
	    wrapper.add(cjb);
	    
	    cjb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			clearData();
		    }
		});
	    cjb.setFont(small_font); 
		    
	    final JButton cvjb = new JButton("Convert");
	    wrapper.add(cvjb);
	    
	    cvjb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			convertData();
		    }
		});
	    cvjb.setFont(small_font); 

	    // ---------------------

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 2;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;

	    gridbag.setConstraints(wrapper, c);
	    panel.add(wrapper);
	}


	{ 
	    JPanel wrapper = new JPanel();

	    // ---------------------

	    final JButton cjb = new JButton("Close");
	    wrapper.add(cjb);
	    
	    cjb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			cleanUp();
		    }
		});
	    
	    final JButton hjb = new JButton("Help");
	    wrapper.add(hjb);
	    
	    hjb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			mview.getPluginHelpTopic("Notepad", "Notepad");
		    }
		});
	   
	    // ---------------------

	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 3;
	    c.weightx = 1.0;
	    c.fill = GridBagConstraints.HORIZONTAL;

	    gridbag.setConstraints(wrapper, c);
	    panel.add(wrapper);
	}


	frame.getContentPane().add(panel, BorderLayout.CENTER);
	frame.pack();

	return frame;
	
    }
    
    //
    // =====================================================================
    //
    // =====================================================================
    //

    public void saveData() 
    {
	
	JFileChooser jfc = new JFileChooser();
	//jfc.setAccessory(opts_pan);
	String dld = mview.getProperties().getProperty("Notepad.save_path");
	if(dld != null)
	{
	    File ftmp = new File(dld);
	    jfc.setCurrentDirectory(ftmp);
	}
	
	int ret_val =  jfc.showSaveDialog(mview.getDataPlot()); 
	if(ret_val == JFileChooser.APPROVE_OPTION) 
	{
	    File file = jfc.getSelectedFile();
	    mview.putProperty("Notepad.save_path", file.getPath());

	    try
	    {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		
		String com = comment_jta.getText();
		if(com.length() > 0)
		    writer.write(com + "\n\n");
		
		//for(int n=0; n < note_list.getModel().getRowCount(); n++)
		//{
		//    writer.write( (String) (note_list.getModel().getElementAt(n)));
		//    writer.write("\n");
		//}
		
		writer.close();
		System.out.println("...done");

	    }
	    catch (java.io.IOException ioe)
	    {
		mview.errorMessage("Unable to write to '" + file.getName() + "'");
		
	    }

	}
    }
    public void clearData()
    {
	comment_jta.setText("");
	note_list.setModel( new CustomTableModel() );
    }

    public void convertData()
    {
    }

    //
    // =====================================================================
    //
    // =====================================================================
    //

    private class CustomTableModel extends javax.swing.table.DefaultTableModel
    {
	// public int getRowCount()    { return most_common.length; }

	public int getColumnCount() { return 2; } 
	
	public Object getValueAt(int row, int col)
	{
	    Vector row_v = (Vector) getDataVector().elementAt(row);
	    Object data = row_v.elementAt(col);

	    if(data != null)
	    {
		if(col == 0)
		{
		    return ((DragAndDropEntity) data).toString(edata);
		}
		else
		{
		    return (String) data;
		}
	    }
	    return null;
	}
	public String getColumnName(int column) 
	{
	    if(column == 0)
		return "Entity";
	    else
		return "Comment";
	}
	public boolean isCellEditable(int row, int column) 
	{
	    return (column != 0);
	}
    }

    //
    // =====================================================================
    //
    // =====================================================================
    //

    private JTextArea comment_jta;
    private JPanel panel;
    private DragAndDropTable note_list;
    //private DNDList note_list;
    private JFrame frame;

    private maxdView mview;
    private ExprData edata;

    
}
