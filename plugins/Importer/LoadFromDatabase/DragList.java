//package uk.ac.man.bioinf.maxdLoad2;

/*
  a JList with support for moving things around using drag-n-drop

  (c)2002 David Hancock
*/

import java.awt.*;
import java.io.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

public class DragList extends JList implements DropTargetListener, DragSourceListener, DragGestureListener    
{

    DropTarget dropTarget = null;
    DragSource dragSource = null;
    
    int drop_index_above = -1;   // used to show where the drop will occur
    int drop_index_below = -1;   // used to show where the drop will occur

    int drag_index;         // records which node is being dragged

    public DragList()
    {
	dropTarget = new DropTarget (this, this);

	dragSource = DragSource.getDefaultDragSource(); // new DragSource();
	dragSource.createDefaultDragGestureRecognizer( this, DnDConstants.ACTION_MOVE, this);

	setCellRenderer(new CustomListCellRenderer());
    }

    public void paintComponent( Graphics g )
    {
	if( ComponentFactory.useAntialiasing() )
	{
	    Graphics2D g2 = (Graphics2D) g;
	    
	    ComponentFactory.setRenderingHints( g2 );
	    
	    super.paintComponent( g2 );
	}
	else
	{
	    super.paintComponent( g );
	}   
    }

    //
    // ======================================================
    //  DropTargetListener interface
    // ======================================================
    //

    public void dragEnter (DropTargetDragEvent event) 
    {
	//System.out.println("DropTargetListener: dragEnter()");

	//event.acceptDrag (DnDConstants.ACTION_MOVE);
    }
    
    public void dragExit (DropTargetEvent event) 
    {
	//System.out.println("DropTargetListener: dragExit()");
    }
    
    public void dragOver (DropTargetDragEvent event) 
    {
	Point pt = event.getLocation();

	//System.out.println("DropTargetListener: dragOver() @ " + pt.getX() + "," + pt.getY() );

	// work out where the drop would occur in the tree....

	int index_under_mouse = locationToIndex( pt );
	
	if( index_under_mouse >= 0 )
	{
	    Rectangle rect = getCellBounds( index_under_mouse, index_under_mouse );
	    
	    int mid_y = rect.y + (rect.height/2);
	    
	    // record which node needs to be altered in the visual representation
	    
	    if( (int)pt.getY() >= mid_y )
	    {
		drop_index_above = -1;
		drop_index_below = index_under_mouse;
	    }
	    else
	    {
		drop_index_below = -1;
		drop_index_above = index_under_mouse;
	    }
	    
	    repaint();
	    
	    return;
	}
	
	drop_index_above = drop_index_below = -1;
    }

    public void drop (DropTargetDropEvent event) 
    {
	//System.out.println("DropTargetListener: drop()");

	try 
	{
	    Transferable transferable = event.getTransferable();

	    CustomTransferable ct = null;
	    String text = null;
		
	    
	    if(transferable.isDataFlavorSupported (DragAndDropEntityFlavour))
	    {
		ct = (CustomTransferable) transferable.getTransferData(DragAndDropEntityFlavour);
		
		if(ct != null)
		{
		    // ....do remove then insert....

		    int drop_index = locationToIndex( event.getLocation() );
	
		    if(drop_index == -1)
		    {
			// drop is right at the bottom of the list
			drop_index = getModel().getSize();
		    }

		    // System.out.println( "move " + drag_index + " to " + drop_index );

		    try
		    {
			DefaultListModel dlm = (DefaultListModel) getModel();
			
			String value_to_move = (String) dlm.getElementAt( drag_index );
			dlm.removeElement( value_to_move );
			
			if(drop_index >= dlm.getSize())
			{
			    // add at end of list
			    dlm.addElement( value_to_move );
			}
			else
			{
			    dlm.add( drop_index, value_to_move );
			}
			
			setSelectedValue( value_to_move , true );
			
			drop_index_above = drop_index_below = -1;
			
			repaint();
			
			event.acceptDrop(DnDConstants.ACTION_MOVE);
			event.getDropTargetContext().dropComplete(true);


		    }
		    catch( ClassCastException cce )
		    {
			// wrong sort of list model
			System.err.println("WARNING: DragList requires a DefaultListModel (or subclass)");
		    }
		}
	    }
	    
	    drop_index_above = drop_index_below = -1;
	    repaint();
	    event.rejectDrop();
	}

	catch (IOException exception) 
	{
	    exception.printStackTrace();
	    System.err.println( "Exception" + exception.getMessage());
	    event.rejectDrop();
	} 
	catch (UnsupportedFlavorException ufException ) 
	{
	    ufException.printStackTrace();
	    System.err.println( "Exception" + ufException.getMessage());
	    event.rejectDrop();
	}	

	drop_index_above = drop_index_below = -1;
	repaint();
    }

    public void dropActionChanged ( DropTargetDragEvent event ) 
    {
	//System.out.println("DropTargetListener: dropActionChanged()");
    }
   

    //
    // ======================================================
    //  DragGestureListener interface
    // ======================================================
    //

    public void dragGestureRecognized( DragGestureEvent event) 
    {
	//System.out.println( " dragGesturedRecognized() start....");
	try
	{
	    int index_under_mouse = getSelectedIndex(); //locationToIndex( event.getLocation() );
	    
	    if( index_under_mouse >= 0 )
	    {  
		String selected = (String) getSelectedValue();
		
		if (selected != null)
		{
		    // System.out.println( " drag start.... with " + selected);
		    
		    CustomTransferable ct = new CustomTransferable( selected );
		    
		    dragSource.startDrag (event, DragSource.DefaultMoveDrop, ct, this);
		    
		    drag_index = index_under_mouse;
		    
		    return;
		}
	    }
	}
	catch(ArrayIndexOutOfBoundsException aioobe)
	{
	}
	
	drag_index = -1;
    }
    
    //
    // ======================================================
    //  Transferable interface
    // ======================================================
    //

    final private static DataFlavor 
	DragAndDropEntityFlavour = new DataFlavor("x-application/java-maxdLoad-DragList", 
						  "maxdLoad-DragList-DragAndDropEntity");
    
    static DataFlavor flavours[] = { DragAndDropEntityFlavour };

    public class CustomTransferable implements Transferable
    {
	public CustomTransferable( String data )
	{
	}

	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException
	{
	    if(flavor.equals(DragAndDropEntityFlavour))
	    {
		return this;
	    }
	    else
	    {
		throw new UnsupportedFlavorException(flavor);
	    }
	}

	public DataFlavor[] getTransferDataFlavors() 
	{
	    return flavours;
	}

	public boolean isDataFlavorSupported(DataFlavor flavor) 
	{
	    return flavor.equals(DragAndDropEntityFlavour);
	}

    }


    //
    // ======================================================
    //  DragSourceListener interface
    // ======================================================
    //

    public void dragDropEnd (DragSourceDropEvent event) 
    {   
    }
    
    public void dragEnter (DragSourceDragEvent event) 
    {
    }
    
    public void dragExit (DragSourceEvent event) 
    {
    }
    
    public void dragOver (DragSourceDragEvent event) 
    {
    }
    
    public void dropActionChanged ( DragSourceDragEvent event) 
    {
    }
    

    //
    // ======================================================
    //  CustomTreeCellRenderer
    // ======================================================
    //

    public class CustomListCellRenderer extends DefaultListCellRenderer 
    {
	private boolean highlight_above = false;
	private boolean highlight_below = false;

 	public void paintComponent(Graphics g)
	{
	    super.paintComponent(g);

	    if(highlight_above)
	    {
		g.setColor( Color.black );
		g.drawLine( 0, 1, getWidth(), 1 );
	    }
	    if(highlight_below)
	    {
		g.setColor( Color.black );
		g.drawLine( 0, getHeight()-1, getWidth(), getHeight()-1 );
	    }
	}

	public Component getListCellRendererComponent(JList list,
						      Object value,
						      int index,
						      boolean isSelected,
						      boolean hasFocus) 
	{
	    
	    super.getListCellRendererComponent( list,value,index,isSelected,hasFocus );

	    // if a drag is underway, then indicate the potential drop location
	    highlight_above = highlight_below = false;

	    if((drop_index_above != -1) && (index == drop_index_above))
	    {
		highlight_above = true;
	    }
	    if((drop_index_below != -1) && (index == drop_index_below))
	    {
		highlight_below = true;
	    }

	    return this;
	}
    }

}
