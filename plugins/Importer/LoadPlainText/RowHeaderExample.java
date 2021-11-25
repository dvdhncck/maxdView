import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

/**
 * Define the look/content for a cell in the row header
 * In this instance uses the JTables header properties
 **/
class RowHeaderRenderer extends JPanel implements ListCellRenderer {
    
    /**
     * Constructor creates all cells the same
     * To change look for individual cells put code in
     * getListCellRendererComponent method
     **/
    RowHeaderRenderer(JTable table) 
    {
	JTableHeader header = table.getTableHeader();
	setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );
	setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        setFont(header.getFont());
	
        /*
        
	setOpaque(true);
        setHorizontalAlignment(CENTER);
        setForeground(header.getForeground());
        setBackground(header.getBackground());
        */
	row = new JLabel("Row:100");
	add( row );
	thing = new JLabel("[[100]]");
	thing.setHorizontalAlignment(SwingConstants.RIGHT);
	add( thing );
	mode = new JComboBox( modes );
	add( mode );
    }

    JLabel row;
    JLabel thing;
    JComboBox mode;
    final String[] modes = {"Huge","Big","Small","Tiny","Minute"};
 
   /**
     * Returns the JLabel after setting the text of the cell
     **/
    public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) 
    {
	row.setText((value == null) ? "" : ("Row:" + value.toString()));
	thing.setText((value == null) ? "" : ("[[" + value.toString() + "]]" ) );

	row.setToolTipText( "this is 'row' for " + value );
	thing.setToolTipText( "this is 'thing' for " + value );

        return this;
    }
}

class CustomTableCellRenderer extends JPanel implements javax.swing.table.TableCellRenderer
{
    public CustomTableCellRenderer(JTable table) 
    {
	JTableHeader header = table.getTableHeader();
	setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
	setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        setFont(header.getFont());

        /*
        
	setOpaque(true);
        setHorizontalAlignment(CENTER);
        setForeground(header.getForeground());
        setBackground(header.getBackground());
        	*/
	col = new JLabel("Col");
	add( col );
	thing = new JLabel("Thing");
	add( thing );
	mode = new JComboBox( modes );
	add( mode );

    }
    JLabel col;
    JLabel thing;
    JComboBox mode;
    final String[] modes = {"Up","Down","Left","Right"};

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
	col.setText((value == null) ? "" : ("Col:" + value.toString()) );
	thing.setText((value == null) ? "" : ("<<" + value.toString() + ">>" ) );

	col.setToolTipText( "this is 'col' for " + value );
	thing.setToolTipText( "this is 'thing' for " + value );

        return this;
 
    } 
}

public class RowHeaderExample extends JFrame {
    
    public RowHeaderExample() {
        super( "Row Header Example" );
        setSize( 300, 150 );
        
        ListModel listModel = new AbstractListModel() {
            String headers[] = {"a", "b", "c", "d", "e", "f", "g", "h", "i"};
            public int getSize() { return headers.length; }
            public Object getElementAt(int index) { return headers[index]; }
        };
        
        DefaultTableModel defaultModel = 
            new DefaultTableModel(listModel.getSize(),40);
        JTable table = new JTable( defaultModel );
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
	CustomTableCellRenderer ctcr = new CustomTableCellRenderer( table );
	table.getTableHeader().setDefaultRenderer( ctcr  );
	ctcr.addMouseListener( new MouseAdapter()
	    {
		public void mouseClicked(MouseEvent e) 
		{
		    System.out.println("hello");
		}
	    });

        // Create single component to add to scrollpane
        JList rowHeader = new JList(listModel);
        //rowHeader.setFixedCellWidth(200);
        rowHeader.setFixedCellHeight(table.getRowHeight());
        rowHeader.setCellRenderer(new RowHeaderRenderer(table));
        
        JScrollPane scroll = new JScrollPane( table );
        scroll.setRowHeaderView(rowHeader); // Adds row-list left of the table
        getContentPane().add(scroll, BorderLayout.CENTER);
    }
    
    public static void main(String[] args) {
        
        RowHeaderExample frame = new RowHeaderExample();
        frame.addWindowListener( new WindowAdapter() {
            public void windowClosing( WindowEvent e ) {
                System.exit(0);
            }
        });
        frame.setVisible(true);
    }
}
