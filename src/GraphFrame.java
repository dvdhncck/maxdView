import javax.swing.*;
import javax.swing.event.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;



public class GraphFrame extends JFrame
{
    public GraphPanel getGraphPanel() { return panel[0]; }
    
    public GraphPanel getGraphPanel( int p ) { return panel[p]; }

    public GraphFrame( final maxdView mview, final String title )
    {
	this( mview, title, 1 );
    }

    public GraphFrame( final maxdView mview, final String title, final int desired_n_panels )
    {
	super(title);

	this.mview = mview;

	final int n_panels = desired_n_panels < 1 ? 1 : desired_n_panels;

	mview.decorateFrame( this );
       
	GridBagConstraints c;
	GridBagLayout gridbag = new GridBagLayout();
	getContentPane().setLayout(gridbag);

	// ======================================================

	wrapper = new PrintablePanel();
	wrapper.setBorder( BorderFactory.createEmptyBorder( 3,3,3,3 ) );
	GridBagLayout wrapper_gridbag = new GridBagLayout();
	wrapper.setLayout(wrapper_gridbag);

	panel = new GraphPanel[ n_panels ];

	for( int p=0; p < n_panels; p++ )
	{
	    panel[p] = new GraphPanel();
	    c = new GridBagConstraints();
	    c.gridx = p;
	    c.weighty = 10.0;
	    c.weightx = 9.9 / (double)n_panels;
	    c.fill = GridBagConstraints.BOTH;
	    wrapper_gridbag.setConstraints( panel[p], c );
	    wrapper.add( panel[p] );
	}

	// ======================================================

	JPanel button_wrapper = new JPanel();
	wrapper_gridbag = new GridBagLayout();
	button_wrapper.setLayout(wrapper_gridbag);

	
	JButton button = new JButton("Print");
	button.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    new PrintManager( mview, wrapper, wrapper ).openPrintDialog();
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 0;
	c.weightx = 1.0;
	//c.fill = GridBagConstraints.HORIZONTAL;
	wrapper_gridbag.setConstraints(button, c);
	button_wrapper.add(button);
	

	Dimension fillsize = new Dimension(16,16);
	Box.Filler filler = new Box.Filler(fillsize, fillsize, fillsize);
	c = new GridBagConstraints();
	c.gridx = 1;
	c.anchor = GridBagConstraints.WEST;
	wrapper_gridbag.setConstraints(filler, c);
	button_wrapper.add(filler);

	
	button = new JButton("Close");
	button.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    setVisible(false);
		}
	    });
	c = new GridBagConstraints();
	c.gridx = 2;
	c.weightx = 1.0;
	//c.fill = GridBagConstraints.HORIZONTAL;
	wrapper_gridbag.setConstraints(button, c);
	button_wrapper.add(button);

	// ======================================================


	c = new GridBagConstraints();
	c.weightx = 10.0;
	c.weighty = 10.0;
	c.fill = GridBagConstraints.BOTH;
	gridbag.setConstraints( wrapper, c);
	getContentPane().add( wrapper );

	c = new GridBagConstraints();
	c.weightx = 10.0;
	c.gridy = 1;
	//c.fill = GridBagConstraints.HORIZONTAL;
	gridbag.setConstraints( button_wrapper, c);
	getContentPane().add( button_wrapper );
	
	pack();
	setVisible(true);
    }

    public class PrintablePanel extends JPanel implements Printable
    {
	public int print(Graphics g, PageFormat pf, int pg_num) throws PrinterException 
	{
	    g.translate( (int)pf.getImageableX(), 
			 (int)pf.getImageableY() );
	    
	    paint( g );

	    return ( pg_num > 0 ) ? NO_SUCH_PAGE : PAGE_EXISTS;
	}
    }

    private GraphPanel[] panel;
    private PrintablePanel wrapper;
    private maxdView mview;
    
}
 
