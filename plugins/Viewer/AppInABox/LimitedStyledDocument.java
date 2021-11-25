import javax.swing.*; 
import javax.swing.text.*; 
import java.awt.Toolkit;

public class LimitedStyledDocument extends DefaultStyledDocument 
{
    public void insertString(int offs, String str, AttributeSet a) 
    {
	try
	{
	    super.insertString(offs, str, a);
	}
	catch(javax.swing.text.BadLocationException ble)
	{
	    try
	    {
		super.insertString(getLength(), str, a);
	    }
	    catch(javax.swing.text.BadLocationException another_ble)
	    {
		
	    }
	}

    }
}
