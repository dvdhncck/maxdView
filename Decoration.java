import java.io.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.font.*;

public class Decoration
{
    protected maxdView mview;

    protected int    d_type;
    protected String d_text;
    protected Point  d_loc;
    //protected Point  d_dir;
    protected int    d_orient;
    protected int    d_align;
    protected int    d_font_fam;
    protected int    d_font_sty;
    protected int    d_font_size;
    protected Font   d_font;
    
    protected Color  d_col;
    protected Color  d_back;
    protected int    d_fill_mode;    // 0==solid, 1==transp
    protected int    d_back_mode;    // 0==solid, 1==transp

    protected int     d_arrow_mode;

    protected float   d_line_w;
    protected float   d_arrow_len;
    protected float   d_line_dir;
    protected float   d_arrow_w;
    protected float   d_arrow_head_w;
    protected float   d_arrow_head_l;
 
    protected Color   d_outline_col;
    
    protected int d_leg_w;
    protected int d_leg_h;
    protected int d_leg_orient;
    protected int d_leg_label_mode;
    protected int d_leg_label_offset;
    protected Colouriser d_colouriser;

    protected int d_img_w;
    protected int d_img_h;
    protected Image d_img_i;
    protected String d_img_fn;
    
    public Decoration(maxdView mview_, int dt)
    {
	mview = mview_;

	d_type = dt;
	d_text = "(New)";
	d_loc  = new Point( 50, 50 );
	d_col  = mview.getDataPlot().getTextColour();
	d_back = mview.getDataPlot().getBackgroundColour();
	d_font      = mview.getDataPlot().getFont();
	d_font_fam  = fontToFamily(d_font);
	d_font_sty  = fontToStyle(d_font);
	d_font_size = d_font.getSize();
	
	d_arrow_len     = (float) 10.0;
	d_line_dir     = (float)  0.0;
	d_arrow_w      = (float) 1.0;
	d_line_w       = (float) 0.0;
	d_arrow_head_w = (float) 2.0;
	d_arrow_head_l = (float) 2.0;
	d_outline_col  = d_col;
	
	d_fill_mode = d_back_mode = 1;

	d_leg_w = 25;
	d_leg_h = 15;
	
	d_img_w = 25;
	d_img_h = 15;
	d_img_i = null;
	d_img_fn = "";
	
	d_colouriser = null;
    }
    
    protected Object clone()
    {
	Decoration d = new  Decoration(mview, d_type);
	d.d_text = new String(d_text);
	d.d_loc  = new Point( d_loc.x, d_loc.y );
	d.d_orient = d_orient;
	d.d_align  = d_align;

	d.d_col  = d_col;
	d.d_back = d_back;
	d.d_fill_mode = d_fill_mode;
	d.d_back_mode = d_back_mode;

	d.d_font      = d_font;
	d.d_font_fam  = d_font_fam;
	d.d_font_sty  = d_font_sty;
	d.d_font_size = d_font_size;
	
	d.d_arrow_mode   = d_arrow_mode;

	d.d_arrow_w      = d_arrow_w;
	d.d_line_w       = d_line_w;
	d.d_arrow_len    = d_arrow_len;
	d.d_line_dir     = d_line_dir;
	d.d_arrow_head_w = d_arrow_head_w;
	d.d_arrow_head_l = d_arrow_head_l;

	d.d_outline_col  = d_outline_col;
	
	d.d_leg_w = d_leg_w;
	d.d_leg_h = d_leg_h;
	d.d_leg_orient = d_leg_orient;
	d.d_leg_label_mode = d_leg_label_mode;
	d.d_leg_label_offset = d_leg_label_offset;
	d.d_colouriser = d_colouriser;
	
	d.d_img_w = d_img_w;
	d.d_img_h = d_img_w;
	d.d_img_i = d_img_i;
	d.d_img_fn = new String(d.d_img_fn);
	
	
	return d;
    }
    
    public Decoration copyMe()
    {
	return (Decoration) this.clone();
    }
    
    public void write(Writer w) throws IOException
    {
	w.write("1\n");  // format id (so we can handle future extensions)
	
	w.write(d_type + "\n" + d_text + "\n" + d_loc.x + "\n" + d_loc.y + "\n" + 
		d_orient + "\n" +   d_align + "\n" +
		d_font_fam + "\n" + d_font_sty + "\n" + d_font_size + "\n");

	w.write(mview.colorToInt(d_col) + "\n" + 
		mview.colorToInt(d_back) + "\n" +
		d_fill_mode + "\n" +
		d_back_mode + "\n");

	w.write(d_arrow_mode + "\n" +
		d_arrow_w + "\n" +
		d_line_w + "\n" +
		d_arrow_len + "\n" +
		d_line_dir + "\n" +
		d_arrow_head_w + "\n" +
		d_arrow_head_l + "\n");

	w.write(mview.colorToInt(d_outline_col) + "\n");

	w.write(d_leg_orient + "\n" + 
		d_leg_w + "\n" +
		d_leg_h + "\n" +
		d_leg_label_mode + "\n" +
		d_leg_label_offset + "\n");

	w.write((d_colouriser == null ? "" : d_colouriser.getName()) + "\n");

	w.write(d_img_w + "\n" +
		d_img_h + "\n" +
		d_img_fn + "\n");
	

	w.write("\n");
    }
    
    public static Decoration read(maxdView mview, BufferedReader r)
    {
	boolean ok = false;
	Decoration dec = new Decoration(mview, 0);
	String line = null;
	
	try
	{
	    line = r.readLine();
	    int format = new Integer(line).intValue();
	    if(format == 1)
	    {
		line = r.readLine();
		dec.d_type = new Integer(line).intValue();

		line = r.readLine();
		dec.d_text = line;

		line = r.readLine();
		dec.d_loc.x = new Integer(line).intValue();
		line = r.readLine();
		dec.d_loc.y = new Integer(line).intValue();

		line = r.readLine();
		dec.d_orient = new Integer(line).intValue();
		line = r.readLine();
		dec.d_align = new Integer(line).intValue();

		line = r.readLine();
		dec.d_font_fam = new Integer(line).intValue();
		line = r.readLine();
		dec.d_font_sty = new Integer(line).intValue();
		line = r.readLine();
		dec.d_font_size = new Integer(line).intValue();


		line = r.readLine();
		dec.d_col = mview.intToColor(new Integer(line).intValue());
		line = r.readLine();
		dec.d_back = mview.intToColor(new Integer(line).intValue());
		line = r.readLine();
		dec.d_fill_mode = new Integer(line).intValue();
		line = r.readLine();
		dec.d_back_mode = new Integer(line).intValue();


		line = r.readLine();
		dec.d_arrow_mode = new Integer(line).intValue();
		line = r.readLine();
		dec.d_arrow_w = new Float(line).floatValue();
		line = r.readLine();
		dec.d_line_w = new Float(line).floatValue();
		line = r.readLine();
		dec.d_arrow_len = new Float(line).floatValue();
		line = r.readLine();
		dec.d_line_dir = new Float(line).floatValue();
		line = r.readLine();
		dec.d_arrow_head_w = new Float(line).floatValue();
		line = r.readLine();
		dec.d_arrow_head_l = new Float(line).floatValue();
		

		line = r.readLine();
		dec.d_outline_col = mview.intToColor(new Integer(line).intValue());
		
		line = r.readLine();
		dec.d_leg_orient = new Integer(line).intValue();
		line = r.readLine();
		dec.d_leg_w = new Integer(line).intValue();
		line = r.readLine();
		dec.d_leg_h = new Integer(line).intValue();
		line = r.readLine();
		dec.d_leg_label_mode = new Integer(line).intValue();
		line = r.readLine();
		dec.d_leg_label_offset = new Integer(line).intValue();

		line = r.readLine();
		dec.d_colouriser = mview.getDataPlot().getColouriserByName(line);
		
		line = r.readLine();
		dec.d_img_w = new Integer(line).intValue();
		line = r.readLine();
		dec.d_img_h = new Integer(line).intValue();
		line = r.readLine();
		dec.d_img_fn = line;
		
		// and the blank line at the end...
		line = r.readLine();
	    }
	    else
	    {
		System.out.println("Decoration.read() unrecognised format: " + format);
	    }
	}
	catch (NumberFormatException nfe)
	{
	    System.out.println("Decoration.read(): NumberFormatException: line was '" + line + "'");
	    return null;
	}
	catch (java.io.IOException ioe)
	{
	    return null;
	}
	
	return dec;
    }
    private int fontToFamily(Font f)
    {
	String name = f.getName();
	if(name.equals("Courier"))
	    return 1;
	if(name.equals("Times"))
	    return 2;
	return 0;
    }
 
    private int fontToStyle(Font f)
    {
	if(f.isBold())
	    return 1;
	if(f.isItalic())
	    return 2;
	return 0;
    }
 
}
