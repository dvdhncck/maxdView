import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.awt.print.*;

public class PrintManager
{
    public PrintManager(maxdView mview_, Printable  printable_)
    {
	mview = mview_;
	// dplot = mview.getDataPlot();
	printable = printable_;
	listeners = new Vector();
    }

    public interface PrintListener
    {
	public void print();
    }

    public void addPrintListener(PrintListener al)
    {
	listeners.addElement(al);
    }


    protected Vector listeners;

    protected Printable printable;

    private int printer_print_what = 2;

    private boolean printer_print_black_text = false;
    private boolean printer_print_white_bg = false;

    public void  openPrintDialog()
    {
	final JFrame pframe = new JFrame("maxdView: Print");
	JPanel panel = new JPanel();
	panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

	GridBagConstraints c = null;
	GridBagLayout gbag = new GridBagLayout();
	panel.setLayout(gbag);
	
	int line = 0;

	// - - - - - =================================== - - - - - 

	JLabel label = new JLabel("Print how?");
	c = new GridBagConstraints();
	//c.anchor = GridBagConstraints.WEST;
	c.gridx = 0;
	c.gridy = line++;
	c.gridwidth = 3;
	c.weighty = 1.0;
	gbag.setConstraints(label, c);
	panel.add(label);

	JCheckBox jchkb = new JCheckBox("White background");
	jchkb.setSelected(printer_print_white_bg);
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.WEST;
	c.gridx = 0;
	c.gridy = line++;
	c.gridwidth = 3;
	gbag.setConstraints(jchkb, c);
	panel.add(jchkb);
	jchkb.addActionListener(new ActionListener() 
	    {
		    public void actionPerformed(ActionEvent e) 
		    {
			printer_print_white_bg = ((JCheckBox)e.getSource()).isSelected();
		    }
		});
	jchkb = new JCheckBox("Black text");
	jchkb.setSelected(printer_print_black_text);
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.WEST;
	c.gridx = 0;
	c.gridy = line++;
	c.gridwidth = 3;
	gbag.setConstraints(jchkb, c);
	panel.add(jchkb);
	jchkb.addActionListener(new ActionListener() 
	    {
		    public void actionPerformed(ActionEvent e) 
		    {
			printer_print_black_text = ((JCheckBox)e.getSource()).isSelected();
		    }
		});
	
	// - - - - - =================================== - - - - - 

	JButton jb = new JButton("Help");
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.EAST;
	c.gridx = 0;
	c.weightx = 1.0;
	c.weighty = 1.0;
	c.gridy = line;
	gbag.setConstraints(jb, c);
	panel.add(jb);

	jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			mview.getHelpTopic("ViewerPrint");
		    }
		});


	jb = new JButton("Print");
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = line;
	c.weightx = 1.0;
	c.weighty = 1.0;
	gbag.setConstraints(jb, c);
	panel.add(jb);

	jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			pframe.setVisible(false);
			
			doPrint();
		    }
		});

	jb = new JButton("Cancel");
	c = new GridBagConstraints();
	c.gridx = 2;
	c.gridy = line;
	c.anchor = GridBagConstraints.WEST;
	c.weightx = 1.0;
	c.weighty = 1.0;
	gbag.setConstraints(jb, c);
	panel.add(jb);

	jb.addActionListener(new ActionListener() 
		{
		    public void actionPerformed(ActionEvent e) 
		    {
			pframe.setVisible(false);
		    }
		});

	pframe.getContentPane().add(panel);
	pframe.pack();

	mview.locateWindowAtCenter(pframe);
	
	pframe.setVisible(true);
    }
    
    private void doPrint()
    {
	PrinterJob job = PrinterJob.getPrinterJob();
	// Ask user for page format (e.g., portrait/landscape)
	
	PageFormat def_pf = job.defaultPage();
	
	/*
	if(dplot_panel.getWidth() > dplot_panel.getHeight())
	{
	    def_pf.setOrientation(PageFormat.LANDSCAPE);
	}
	else
	{
	    def_pf.setOrientation(PageFormat.PORTRAIT);
	}
	*/

	PageFormat pf = job.pageDialog(def_pf);

	// inform the owner(s) ...
	for(int l=0; l <listeners.size(); l++)
	    ((PrintListener) listeners.elementAt(l)).print();

	job.setPrintable( printable , pf);
	
	if(!job.printDialog())
	    return;

	ProgressOMeter pm = new ProgressOMeter("Printing...");
	pm.startIt();

	try
	{
	    job.print();
	}
	catch(java.awt.print.PrinterException pe)
	{
	    pm.stopIt();
	    mview.alertMessage("Printing problem\n  " + pe); 
	    return;
	}

	pm.stopIt();
    }

    private maxdView mview;
    // private DataPlot dplot;
}
