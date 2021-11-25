import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.util.Vector;
import java.awt.print.*;
import java.io.*;

public class PrintManager
{
    public PrintManager( maxdView mview, Printable printable, JPanel source_panel )
    {
	this.mview = mview;
	this.printable = printable;
	this.source_panel = source_panel;

	listeners = new Vector();
    }

    public final static int PRINT_TO_PRINTER = 0;
    public final static int CREATE_PNG       = 1;
    public final static int CREATE_JPEG      = 2;

    public final static String[] modes = new String[] { "Printer", 
							"PNG Image (uncompressed)", 
							"JPEG Image (compressed)" };

    public final static String[] mode_comments = new String[] { "Generate hard-copy", 
								"Create a PNG image file (high quality)", 
								"Create a JPG image file (smaller file size, but lower quality)"  };
 
    public interface PrintListener
    {
	public void print();
    }

    public void addPrintListener(PrintListener al)
    {
	listeners.addElement(al);
    }


    protected Vector listeners;

    //private int printer_print_what = 2;
    //
    //private boolean printer_print_black_text = false;
    //private boolean printer_print_white_bg = false;


    public void  openPrintDialog( )
    {
	openPrintDialog( null );
    }

    public class PrintInfo
    {
	public int print_mode;
	public String image_format;
	public File destination;
	
	public PageFormat page_format;
	public PrinterJob printer_job;

	public PrintInfo( int print_mode, String image_format, File destination ) 
	{ 
	    this.print_mode = print_mode;
	    this.image_format = image_format; 
	    this.destination = destination; 
	}

	public PrintInfo( int print_mode, PageFormat page_format, PrinterJob printer_job ) 
	{ 
	    this.print_mode = print_mode;
	    this.page_format = page_format;
	    this.printer_job = printer_job;
	}
    }

    public interface PrintRequestListener
    {
	public void doPrint( PrintInfo pi );
    }

    public void openPrintDialog( final PrintRequestListener print_request_listener )
    {
	pframe = new JFrame("maxdView: Print");

	JPanel panel = new JPanel();
	panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	GridBagConstraints c = null;
	GridBagLayout gbag = new GridBagLayout();
	panel.setLayout(gbag);
	
	int line = 0;

	// - - - - - =================================== - - - - - 

	JLabel label = new JLabel("Choose a printing method");
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.WEST;
	c.gridx = 0;
	c.gridy = line++;
	c.gridwidth = 3;
	c.weighty = 1.0;
	gbag.setConstraints(label, c);
	panel.add(label);

	final JRadioButton[] mode_jrb = new JRadioButton[ modes.length ];

	ButtonGroup bg = new ButtonGroup();

	Dimension fillsize = new Dimension( 10,10 );

	Box.Filler filler = new Box.Filler( fillsize, fillsize, fillsize );
	c.anchor = GridBagConstraints.WEST;
	c.gridx = 0;
	c.gridy = line++;
	c.gridwidth = 5;
	gbag.setConstraints( filler, c );
	panel.add( filler );

	for( int m = 0 ;m < modes.length; m++ )
	{
	    mode_jrb[ m ] = new JRadioButton( modes[ m ] );
	    c = new GridBagConstraints();
	    c.anchor = GridBagConstraints.WEST;
	    c.gridx = 0;
	    c.gridy = line++;
	    c.gridwidth = 5;
	    gbag.setConstraints( mode_jrb[ m ], c );
	    panel.add( mode_jrb[ m ] );
	    bg.add( mode_jrb[ m ] );

	    label = new JLabel( mode_comments[ m ] );
	    if( mview != null )
		label.setFont( mview.getSmallFont() );
	    c.anchor = GridBagConstraints.WEST;
	    c.gridx = 0;
	    c.gridy = line++;
	    c.gridwidth = 5;
	    gbag.setConstraints( label, c );
	    panel.add( label );

	    filler = new Box.Filler( fillsize, fillsize, fillsize );
	    c.anchor = GridBagConstraints.WEST;
	    c.gridx = 0;
	    c.gridy = line++;
	    c.gridwidth = 5;
	    gbag.setConstraints( filler, c );
	    panel.add( filler );
	}

	// - - - - - =================================== - - - - - 
	
	/*
	String[] format = javax.imageio.ImageIO.getWriterFormatNames();

	for( int f = 0; f < format.length; f++ )
	{
	    System.out.println( format[ f ] );
	}
	*/

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
			if( mview != null )
			    mview.getHelpTopic("ViewerPrint");
		    }
		});

	filler = new Box.Filler( fillsize, fillsize, fillsize );
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.WEST;
	c.gridx = 1;
	c.gridy = line;
	gbag.setConstraints( filler, c );
	panel.add( filler );


	jb = new JButton("OK");
	c = new GridBagConstraints();
	c.gridx = 2;
	c.gridy = line;
	c.weightx = 1.0;
	c.weighty = 1.0;
	gbag.setConstraints(jb, c);
	panel.add(jb);

	filler = new Box.Filler( fillsize, fillsize, fillsize );
	c = new GridBagConstraints();
	c.anchor = GridBagConstraints.WEST;
	c.gridx = 3;
	c.gridy = line;
	gbag.setConstraints( filler, c );
	panel.add( filler );

	jb.addActionListener(new ActionListener() 
	    {
		public void actionPerformed(ActionEvent e) 
		{
		    int print_mode = -1;
		    
		    for( int m = 0 ; m < modes.length; m++ ) 
			if( mode_jrb[ m ].isSelected() )
			    print_mode = m;
		    
		    pframe.setVisible(false);
		    
		    doPrint( print_mode, print_request_listener );
		}
	    });


	jb = new JButton("Cancel");
	c = new GridBagConstraints();
	c.gridx = 4;
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

	if( mview != null )
	{
	    mview.decorateFrame( pframe );
	    
	    mview.locateWindowAtCenter(pframe);
	}

	pframe.setVisible(true);
    }
    
    private void doPrint( final int print_mode, final PrintRequestListener print_request_listener  )
    {
	if( print_mode == PRINT_TO_PRINTER )
	{
	    if( ( print_request_listener == null ) && ( printable == null ) )
	    {
		mview.alertMessage( "No 'Printable' has been registered for printing from" );
		return;
	    }

	    PrinterJob job = PrinterJob.getPrinterJob();

	    // Ask user for page format (e.g., portrait/landscape)
	    
	    PageFormat def_pf = job.defaultPage();
	    
	    PageFormat pf = job.pageDialog(def_pf);
	    
	    // inform the owner(s) ...
	    for(int l=0; l <listeners.size(); l++)
		( (PrintListener) listeners.elementAt(l)).print();
	    
	    if( print_request_listener != null )
	    {
		print_request_listener.doPrint( new PrintInfo( print_mode, pf, job ) );
		return;
	    }

	    job.setPrintable( printable , pf);
	    
	    if(!job.printDialog())
		return;
	    
	    ProgressOMeter pm = new ProgressOMeter( "Printing..." );

	    pm.startIt();
	    
	    try
	    {
		job.print();
	    }
	    catch( java.awt.print.PrinterException pe )
	    {
		pm.stopIt();
		mview.alertMessage("Printing problem\n  " + pe); 
		return;
	    }
	    
	    pm.stopIt();
	}

	if( ( print_mode == CREATE_JPEG ) || ( print_mode == CREATE_PNG ) )
	{
	    ProgressOMeter pm = new ProgressOMeter("Printing...");
		
	    try
	    {
		final String format = ( print_mode == CREATE_JPEG ) ? "jpg" : "png";

		JFileChooser fc = mview.getFileChooser();
		
		String cur_path = mview.getProperty("PrintManager.save_image_path", null);
		if(cur_path != null)
		{
		    File init = new File( cur_path );
		    
		    fc.setCurrentDirectory( init );
		}

		int returnVal = fc.showSaveDialog( pframe );
		    
		if (returnVal == JFileChooser.APPROVE_OPTION) 
		{
		    File file = fc.getSelectedFile();
		    
		    String file_name = file.getPath();
		    
		    if( file_name.toLowerCase().endsWith( format ) == false )
		    {
			if( mview.alertQuestion( "Warning: The file name should probably have the extension '." + format + "'\n" + 
						 "\nContinue anyhow ?", "Yes", "No" ) == 1 )
			    return;
			
		    }
		    
		    if( file.exists() )
		    {
			if( mview.alertQuestion( "Warning: A file with this name already exists.\n" + 
						 "\nOverwrite it ?", "Yes", "No" ) == 1 )
			    return;
			
		    }

		    mview.putProperty( "PrintManager.save_image_path", file_name );
		    
		    if( print_request_listener != null )
		    {
			// the calling class has elected to do it's own printing,
			// 
			print_request_listener.doPrint( new PrintInfo( print_mode, format, file ) );

			return;
		    }
		    else
		    {
			if( source_panel == null )
			{
			    mview.alertMessage( "No panel has been registered for printing from" );
			    return;
			}
			
			pm.startIt();
			
			BufferedImage image = (BufferedImage) source_panel.createImage( source_panel.getWidth(),
											source_panel.getHeight() );
			
			Graphics g = image.getGraphics();
			
			source_panel.paint( g );
			
			javax.imageio.ImageIO.write( image, format, file );
		    }
		    
		}
		pm.stopIt();
	    }
	    catch( java.io.IOException ioe )
	    {
		pm.stopIt();
		mview.alertMessage( "Unable to write the image\n\n" + ioe.getMessage() );
	    }
	    
	}
    }

    private maxdView mview;
    private Printable printable;
    private JPanel source_panel;
    private JFrame pframe;
}
