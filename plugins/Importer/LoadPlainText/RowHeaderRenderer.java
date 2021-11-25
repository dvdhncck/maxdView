import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

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
    RowHeaderRenderer(JTable table) {
        JTableHeader header = table.getTableHeader();
        setOpaque(true);
        setBorder(UIManager.getBorder("TableHeader.cellBorder"));
        setHorizontalAlignment(CENTER);
        setForeground(header.getForeground());
        setBackground(header.getBackground());
        setFont(header.getFont());
    }
    
    /**
     * Returns the JLabel after setting the text of the cell
     **/
    public Component getListCellRendererComponent( JList list,
    Object value, int index, boolean isSelected, boolean cellHasFocus) {
        
        setText((value == null) ? "" : value.toString());
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
            new DefaultTableModel(listModel.getSize(),10);
        JTable table = new JTable( defaultModel );
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        // Create single component to add to scrollpane
        JList rowHeader = new JList(listModel);
        rowHeader.setFixedCellWidth(50);
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
