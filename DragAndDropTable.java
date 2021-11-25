//
// use the EntityAdaptor inner class to extract a name from DragAndDropEntity based
// on the context in which the DragAndDropTextField is being used.
//
import java.io.*;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;

import javax.swing.JTable;
import javax.swing.table.*;

public class DragAndDropTable
    extends JTable
    implements DropTargetListener, DragSourceListener, DragGestureListener    
{
    
    DropTarget dropTarget = null;
    DragSource dragSource = null;
    
    public DragAndDropTable() 
    {
	this(null);
    }

    public DragAndDropTable(javax.swing.table.TableModel model) 
    {
	super(model);

	dropTarget = new DropTarget (this, DnDConstants.ACTION_COPY_OR_MOVE, this);
	dragSource =  DragSource.getDefaultDragSource(); //new DragSource();
	dragSource.createDefaultDragGestureRecognizer( this, DnDConstants.ACTION_COPY, this);
    }
    
    //
    // ======================================================
    //  DropTargetListener interface
    // ======================================================
    //
    public void dragEnter (DropTargetDragEvent event) 
    {
	event.acceptDrag (DnDConstants.ACTION_COPY_OR_MOVE);
    }
    
    public void dragExit (DropTargetEvent event) 
    {
    }
    
    public void dragOver (DropTargetDragEvent event) 
    {
	//	event.acceptDrag (DnDConstants.ACTION_COPY_OR_MOVE);
    }
    
    public void drop (DropTargetDropEvent event) 
    {
	try 
	{
	    Transferable transferable = event.getTransferable();

	    DragAndDropEntity dnde = null;
	    String text = null;
		
	    if(transferable.isDataFlavorSupported (DragAndDropEntity.DragAndDropEntityFlavour))
	    {
		if(drop_action != null)
		{
		    dnde = (DragAndDropEntity) transferable.getTransferData(DragAndDropEntity.DragAndDropEntityFlavour);

		    drop_action.dropped(dnde);

		    event.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
		}
		event.getDropTargetContext().dropComplete(true);
	    }
	}
	catch (IOException exception) 
	{
	    exception.printStackTrace();
	    System.err.println( "Exception" + exception.getMessage());
	} 
	catch (UnsupportedFlavorException ufException ) 
	{
	    ufException.printStackTrace();
	    System.err.println( "Exception" + ufException.getMessage());
	}
	
	event.rejectDrop();
    }

    public void dropActionChanged ( DropTargetDragEvent event ) 
    {
    }
    
    //
    // ======================================================
    //  DragGestureListener interface
    // ======================================================
    //

    public void dragGestureRecognized( DragGestureEvent event) 
    {
	if(drag_action != null)
	{
	    DragAndDropEntity dnde = drag_action.getEntity();
	    
	    if(dnde != null) 
	    {
		//System.out.println( " drag start....");
		
		try
		{
		    dragSource.startDrag (event, DragSource.DefaultMoveDrop, dnde, this);
		}
		catch(java.awt.dnd.InvalidDnDOperationException ide)
		{
		    System.out.println( " BAD drag!\n" + ide);
		}
	    }
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
    //  EntityAdaptor inner class
    // ======================================================
    //

    // converts an entity to a specific name
    //
    public interface EntityAdaptor
    {
	public String getName(DragAndDropEntity dnde);
    }
    
    private EntityAdaptor adapt = null;
    
    public void setEntityAdaptor(EntityAdaptor ea) { adapt = ea; }
    public EntityAdaptor getEntityAdaptor() { return adapt; }

    //
    // ======================================================
    //  DropAction inner class
    // ======================================================
    //

    // notifies somebody that a drop has occured
    //
    public interface DropAction
    {
	public void dropped(DragAndDropEntity dnde);
    }
    
    private DropAction drop_action = null;
    
    public void       setDropAction(DropAction da)   { drop_action = da; }
    public DropAction getDropAction()                { return drop_action; }

    //
    // ======================================================
    //  DragAction inner class
    // ======================================================
    //

    // provides something to drag
    //
    public interface DragAction
    {
	public DragAndDropEntity getEntity();
    }
    
    private DragAction drag_action = null;
    
    public void       setDragAction(DragAction da) { drag_action = da; }
    public DragAction getDragAction()              { return drag_action; }

}
