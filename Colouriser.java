import java.awt.*;
import javax.swing.*;
import java.util.Hashtable;
import java.util.Vector;

public interface Colouriser
{
    public String getName(); 
    public void  setName(String name); 

    public Color lookup(double val);
    public void  setRange(double min, double max, Vector data_arrays);

    public double getMin();
    public double getMax();

    public Colouriser cloneColouriser();

    // for interactions
    public JPanel getEditorPanel(maxdView mview, ExprData edata);

    //public void drawLegend(Rectangle2D, Graphics2D g2);
    public int getNumDiscreteColours();
    public double getDiscreteColourValue(int dc);

    // for I/O...

    public Colouriser createFromAttrs(Hashtable attrs);
    public Hashtable createAttrs();

}
