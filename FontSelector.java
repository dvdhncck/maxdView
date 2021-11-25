import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;

public class FontSelector extends JPanel
{
    
    public FontSelector( String name, int layout, Font init_font )
    {
	cur_font = init_font;

	JLabel label;
	GridBagConstraints c;
	GridBagLayout wrapbag = new GridBagLayout();
	setLayout(wrapbag);
	
	if(name != null)
	   setBorder(BorderFactory.createTitledBorder(name));

	label = new JLabel(" Size  ");
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	wrapbag.setConstraints(label,c);
	add(label);
	
	size_ls = new LabelSlider( 1, 128, 14 );
	size_ls.setMode( LabelSlider.INTEGER );
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 0;
	c.weightx = 2.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	c.gridwidth = 2;
	wrapbag.setConstraints(size_ls,c);
	add(size_ls);
	
	
	label = new JLabel(" Style  ");
	c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 1;
	wrapbag.setConstraints(label,c);
	add(label);

	family_jcb = new JComboBox(font_family_names);
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 1;
	c.weightx = 1.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	wrapbag.setConstraints(family_jcb,c);
	add(family_jcb);
		
	
	style_jcb = new JComboBox(font_style_names);
	c = new GridBagConstraints();
	c.gridx = 2;
	c.gridy = 1;
	c.weightx = 1.0;
	c.fill = GridBagConstraints.HORIZONTAL;
	wrapbag.setConstraints(style_jcb,c);
	add(style_jcb);
    }

    public Font getFont()
    {
	if( family_jcb == null)
	    return cur_font;

	String family = font_family_names[ family_jcb.getSelectedIndex() ];

	int style = Font.PLAIN;
	if( style_jcb.getSelectedIndex() == 1 )
	    style = Font.BOLD;
	if( style_jcb.getSelectedIndex() == 2 )
	    style = Font.ITALIC;
	
	int size = (int) size_ls.getValue();

	return new Font( family, style, size );
    }

    public void setFont( Font new_font )
    {
	cur_font = new_font;
    }

    private JComboBox family_jcb, style_jcb;
    private LabelSlider size_ls;
    private Font cur_font;

    final String[] font_family_names = { "Helvetica", "Courier", "Times" };
    final String[] font_style_names  = { "Plain", "Bold", "Italic" };

}
