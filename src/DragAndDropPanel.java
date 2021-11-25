//
// use the EntityAdaptor inner class to extract a name from DragAndDropEntity based
// on the context in which the DragAndDropTextField is being used.
//
import java.io.*;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;

import javax.swing.JPanel;

public class DragAndDropPanel 
    extends JPanel
    implements DropTargetListener, DragSourceListener, DragGestureListener
{
    
    DropTarget dropTarget = null;
    DragSource dragSource = null;

    public DragAndDropPanel(boolean is_drag_source) 
    {
	dropTarget = new DropTarget (this, DnDConstants.ACTION_COPY_OR_MOVE, this);
	
	if(is_drag_source)
	{
	    dragSource = DragSource.getDefaultDragSource();
	    dragSource.createDefaultDragGestureRecognizer( this, DnDConstants.ACTION_COPY, this);
	}
    }

    public DragAndDropPanel() 
    {
	this(true);
    }
    
    //
    // ======================================================
    //  DropTargetListener interface
    // ======================================================
    //
    public void dragEnter (DropTargetDragEvent event) 
    {
	//System.out.println( "DropTarget:dragEnter");
	event.acceptDrag (DnDConstants.ACTION_COPY_OR_MOVE);
    }
    
    public void dragExit (DropTargetEvent event) 
    {
	//System.out.println( "DropTarget:dragExit");
    }
    
    public void dragOver (DropTargetDragEvent event) 
    {
	//System.out.println( "DropTarget:dragOver");
	event.acceptDrag (DnDConstants.ACTION_COPY_OR_MOVE);
    }
    
    public void drop (DropTargetDropEvent event) 
    {
	//System.out.println( "DropTarget:drop attempted");
	try 
	{
	    Transferable transferable = event.getTransferable();

	    DragAndDropEntity dnde = null;
	    String text = null;
		
	    if(transferable.isDataFlavorSupported (DragAndDropEntity.DragAndDropEntityFlavour))
	    {
		if((drop_action != null) || (pos_drop_action != null))
		{
		    dnde = (DragAndDropEntity) transferable.getTransferData(DragAndDropEntity.DragAndDropEntityFlavour);

		    event.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
		    
		    //System.out.println( "'" + dnde.getEntityType() + "' dropped");
		    //System.out.println( "name is '" + dnde.toString() + "'");
		    
		    if(drop_action != null)
			drop_action.dropped(dnde);

		    if(pos_drop_action != null)
			pos_drop_action.dropped(dnde,event);

		    event.getDropTargetContext().dropComplete(true);

		    return;
		}
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
	//System.out.println( "DropTarget:dropActionChanged");
    }
    
    //
    // ======================================================
    //  DragGestureListener interface
    // ======================================================
    //

    public void dragGestureRecognized( DragGestureEvent event) 
    {
	//System.out.println( " dragGesturedRecognized() start....");

	if(drag_action != null)
	{
	    DragAndDropEntity dnde = drag_action.getEntity(event);
	    
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
	    else
	    {
		// System.out.println( " null drag....");
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
	//System.out.println( "DragSource:dragDropEnd");
    }
    
    public void dragEnter (DragSourceDragEvent event) 
    {
	//System.out.println( "DragSource:dragEnter");
    }
    
    public void dragExit (DragSourceEvent event) 
    {
	//System.out.println( "DragSource:dragExit");
    }
    
    public void dragOver (DragSourceDragEvent event) 
    {
	//System.out.println( "DragSource:dragOver");
    }
    
    public void dropActionChanged ( DragSourceDragEvent event) 
    {
	//System.out.println( "DragSource:dropActionChanged");
    }
    
    
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
    
    public void       setDropAction(DropAction da) { drop_action = da; }
    public DropAction getDropAction()              { return drop_action; }

    // a different version for subclasses that are interested in the location of the drag
    //
    public interface PositionedDropAction
    {
	public void dropped(DragAndDropEntity dnde, DropTargetDropEvent event);
    }
    
    private PositionedDropAction pos_drop_action = null;
    
    public void                 setPositionedDropAction(PositionedDropAction pda) { pos_drop_action = pda; }
    public PositionedDropAction getPositionedDropAction()                         { return pos_drop_action; }


    //
    // ======================================================
    //  DragAction inner class
    // ======================================================
    //

    // provides something to drag
    //
    public interface DragAction
    {
	public DragAndDropEntity getEntity(DragGestureEvent event);
    }
    
    private DragAction drag_action = null;
    
    public void       setDragAction(DragAction da) { drag_action = da; }
    public DragAction getDragAction()              { return drag_action; }

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

}
