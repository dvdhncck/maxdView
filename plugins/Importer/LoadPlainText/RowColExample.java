import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;

/**
 * Define the look/content for a cell in the row header
 * In this instance uses the JTables header properties
 **/
class RowHeaderRenderer extends JLabel implements ListCellRenderer {
    
    /**
     * Constructor creates all cells the same
     * To change look for individual cells put code in
     * getListCellRendererComponent method
     **/

    public static CompoundBorder nice_border = null;

    RowHeaderRenderer(JTable table) 
    {
	JTableHeader header = table.getTableHeader();
	if( nice_border == null )
	{
	    Border outer_border   = BorderFactory.createEmptyBorder( 0,8,0,8 );
	    Border inner_border   = UIManager.getBorder("TableHeader.cellBorder");
	    nice_border = BorderFactory.createCompoundBorder( inner_border, outer_border );
	}
	setBorder( nice_border );
        setFont(header.getFont());
        setOpaque(true);
        setHorizontalAlignment(RIGHT);
        setForeground(header.getForeground());
        setBackground(header.getBackground());
    }
    
    /**
     * Returns the JLabel after setting the text of the cell
     **/
    public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) 
    {
	setText((value == null) ? "" : ("Row:" + value.toString()));
	//setToolTipText((value == null) ? "" : ("Row:" + value.toString()));
        return this;
    }
}

class CustomTableCellRenderer extends JLabel implements javax.swing.table.TableCellRenderer
{
    public CustomTableCellRenderer(JTable table) 
    {
	JTableHeader header = table.getTableHeader();
	setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        setFont(header.getFont());

        setOpaque(true);
        setHorizontalAlignment(CENTER);
        setForeground(header.getForeground());
        setBackground(header.getBackground());
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
	setText((value == null) ? "" : ("Col:" + value.toString()) );
	setToolTipText((value == null) ? "" : ("Col:" + value.toString()) );
        return this;
 
    } 
}

public class RowColExample extends JFrame {
    
    public String[] headers;

    public RowColExample() {
        super( "Fixed Row Col Example" );

        setSize( 500, 350 );

	headers = new String[100];
	for(int h=0; h < 100; h++)
	    headers[ h ] = String.valueOf( h );

        
       DefaultTableModel defaultModel = 
            new DefaultTableModel(listModel.getSize(),40);

        JTable table = new JTable( defaultModel );
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
	CustomTableCellRenderer ctcr = new CustomTableCellRenderer( table );
	table.getTableHeader().setDefaultRenderer( ctcr  );

/*
	ctcr.addMouseListener( new MouseAdapter()
	    {
		public void mouseClicked(MouseEvent e) 
		{
		    System.out.println("hello");
		}
	    });
*/

        // Create single component to add to scrollpane
        final JList rowHeader = new JList(listModel);

        //rowHeader.setFixedCellWidth(200);
        rowHeader.setFixedCellHeight(table.getRowHeight());
        rowHeader.setCellRenderer(new RowHeaderRenderer(table));

	rowHeader.addMouseListener( new MouseAdapter()
	    {
		public void mouseClicked(MouseEvent e) 
		{
		    // which component is being clicked upon?
		    int row = rowHeader.locationToIndex( e.getPoint() );
		    System.out.println( "hello in row " + row );
		    
		}
	    });
	rowHeader.addMouseMotionListener( new MouseMotionAdapter()
	    {
		public void mouseMoved(MouseEvent e) 
		{
		    // which component is being clicked upon?
		    int row = rowHeader.locationToIndex( e.getPoint() );
		    rowHeader.setToolTipText( "Row:" + ( row + 1 ) );
		    
		}
	    });

        JScrollPane scroll = new JScrollPane( table );
        scroll.setRowHeaderView(rowHeader); // Adds row-list left of the table

	JLabel info_label = new JLabel("100x19");
	info_label.setToolTipText("100 rows, 19 columns");
        info_label.setHorizontalAlignment(SwingConstants.CENTER);
	info_label.setBorder( UIManager.getBorder("TableHeader.cellBorder") );
	scroll.setCorner(JScrollPane.UPPER_LEFT_CORNER, info_label );

        getContentPane().add(scroll, BorderLayout.CENTER);
    }
    
    public static void main(String[] args) {
        
        RowColExample frame = new RowColExample();
        frame.addWindowListener( new WindowAdapter() {
            public void windowClosing( WindowEvent e ) {
                System.exit(0);
            }
        });
        frame.setVisible(true);
    }
}
