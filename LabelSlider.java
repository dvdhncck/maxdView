import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;

public class LabelSlider extends JPanel
{
    
    public LabelSlider( String name, int layout, int orient, double min, double max, double init )
    {
	mode = DOUBLE;

	if(max <= min)
	    max = min + 1;
	if(init > max)
	    init = max;
	if(init < min)
	    init = min;

	GridBagLayout gridbag = new GridBagLayout();
	GridBagConstraints c = null;
	setLayout(gridbag);
	
	int col = 0;
	int row = 0;

	if(name != null)
	{
	    JLabel label = new JLabel(name+" ");
	    c = new GridBagConstraints();
	    // c.weightx = 1.0;
	    c.anchor = GridBagConstraints.EAST;
	    // c.fill = GridBagConstraints.HORIZONTAL;
	    
	    c.gridx = 0;
	    c.gridy = 0;
	    col = 1;
	    gridbag.setConstraints(label, c);
	    add(label);
	}

	slider = new JSlider( orient, (int) (min*1000.), (int) (max*1000.), (int) (init*1000.) );
	c = new GridBagConstraints();
	c.weightx = 2.0;
	c.fill = GridBagConstraints.HORIZONTAL;

	if(layout == JSlider.HORIZONTAL)
	{
	    c.gridx = col;
	    c.gridy = 0;
	    col++;
	}
	else
	{
	    c.gridx = 0;
	    c.gridy = 1;
	    c.gridwidth = 2;
	}

	gridbag.setConstraints(slider, c);
	add(slider);

	slider_ccl = new CustomChangeListener();
	slider.addChangeListener( slider_ccl );

	
	edit = new JTextField(12);
	edit.setText( NiceDouble.valueOf( init, 12, 4 ) );
	edit_cdl = new CustomDocumentListener();
	edit.getDocument().addDocumentListener(edit_cdl);
	c = new GridBagConstraints();
	c.weightx = 1.0;
	c.anchor = GridBagConstraints.WEST;
	c.fill = GridBagConstraints.HORIZONTAL;
	
	if(layout == JSlider.HORIZONTAL)
	{
	    c.gridx = col;
	    c.gridy = 0;
	}
	else
	{
	    c.gridx = col;
	    c.gridy = 0;
	}
	
	gridbag.setConstraints(edit, c);
	add(edit);
	
	// System.out.println("range=" + min + " ... " + max);

	if(min >= max)
	{
	    min = -.1;
	    max = .1;
	}
	if(init > max)
	    init = max;
	if(init < min)
	    init = min;

    }
    
    public LabelSlider( String name, int orient, double min, double max, double init )
    {
	this( name, JSlider.HORIZONTAL, orient, min, max, init );

    }

    public LabelSlider( String name, double min, double max, double init )
    {
	this( name, JSlider.HORIZONTAL, JSlider.HORIZONTAL, min, max, init );

    }
    public LabelSlider( double min, double max, double init )
    {
	this( null, JSlider.HORIZONTAL, JSlider.HORIZONTAL, min, max, init );
    }


    public void addChangeListener(  ChangeListener cl )
    {
	slider.addChangeListener(cl);
    }

    public void setValue(double v)
    {
	updateTextField(v);
	updateSlider();
	// (via the miracle of listeners this will also update the slider)
    }


    public double getValue()
    {
	try
	{
	    return Double.valueOf( edit.getText() ).doubleValue();
	}
	catch(NumberFormatException nfe)
	{
	    return Double.NaN;
	}
    }

    public final static int INTEGER = 1;
    public final static int DOUBLE = 2;

    public void setMode( int mode_)
    {
	// maybe 'mode' can be a bitfield ?
	mode = mode_;
	updateTextField( (double)slider.getValue() * 0.001 );
    }
    public int getMode( )
    {
	return mode;
    }



    // handles any changes in the slider
    //
    private class CustomChangeListener implements ChangeListener
    {
	public void stateChanged(ChangeEvent e) 
	{
	    double value = (double)slider.getValue() * 0.001;
	    if((mode & DOUBLE) > 0)
		edit.setText( NiceDouble.valueOf( value, 12, 4 ));
	    else
		edit.setText( String.valueOf( (int)  value ));
	}
    }
    

    // handles any changes in text fields
    //
    private class CustomDocumentListener implements DocumentListener 
    {
	public void insertUpdate(DocumentEvent e)  
	{ 
	    propagate(e); 
	}

	public void removeUpdate(DocumentEvent e)  { propagate(e); }
	public void changedUpdate(DocumentEvent e) { propagate(e); }

	private void propagate(DocumentEvent e)
	{
	    updateSlider();
	}
    }

    // synchronise the slider with the text field
    private void updateSlider()
    {
	String t_val = edit.getText();
	try
	{
	    double d_val = Double.valueOf( t_val ).doubleValue();
	    
	    int i_val = (int)(d_val * 1000.0);

	    slider.removeChangeListener( slider_ccl );
	    
	    slider.setValue(i_val);
	    
	    slider.addChangeListener( slider_ccl );
	}
	catch(NumberFormatException nfe)
	{
	}
    }

    // synchronise the text field and slider with the actual value
    private void updateTextField(double v)
    {
	edit.getDocument().removeDocumentListener( edit_cdl );
	
	if(mode == INTEGER)
	    edit.setText( String.valueOf( (int) v ));
	else
	    edit.setText( NiceDouble.valueOf( v, 12, 4 ) );
	
	edit.getDocument().addDocumentListener( edit_cdl );
    }

    private JSlider slider;
    private JTextField edit;
    private int mode;
    private CustomChangeListener slider_ccl;
    private CustomDocumentListener edit_cdl;

}

    

