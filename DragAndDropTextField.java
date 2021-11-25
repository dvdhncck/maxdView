import javax.swing.JTextField;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.io.*;

//
// a JTextfield which like to have DragAndDropEntity's dropped on it.
//
// use the EntityAdaptor subclass to extract a name from DragAndDropEntity based
// on the context in which the DragAndDropTextField is being used.
//
public class DragAndDropTextField extends JTextField implements DropTargetListener
{
    // converts an entity to a specific name
    //
    public interface EntityAdaptor
    {
	public String getName(DragAndDropEntity dnde);
    }

    // ------------------------------------------------------------------

    public DragAndDropTextField(int width) 
    {
	super(width);

	new DropTarget (this, DnDConstants.ACTION_COPY_OR_MOVE, this);
    }
    
    // ----------------------------------------------------0-------------

    //
    // ======================================================
    //  DropTargetListener interface
    // ======================================================
    //
 
    public void dragEnter (DropTargetDragEvent event) 
    {
	event.acceptDrag (DnDConstants.ACTION_MOVE);
	//System.out.println( "DropTargetListener.dragEnter()");
    }
    public void dragExit (DropTargetEvent event) 
    {
	//System.out.println( "DropTargetListener.dragExit()");
    }
    
    public void dragOver (DropTargetDragEvent event) 
    {
	//event.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
    }
    
    public void drop (DropTargetDropEvent event) 
    {
	//System.out.println( "DropTargetListener.drop()");

	try 
	{
	    Transferable transferable = event.getTransferable();
	    
	    if(transferable.isDataFlavorSupported (DragAndDropEntity.DragAndDropEntityFlavour))
	    {
		event.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
		
		DragAndDropEntity dnde = (DragAndDropEntity) transferable.getTransferData(DragAndDropEntity.DragAndDropEntityFlavour);
		
		//System.out.println( "'" + dnde.getEntityType() + "' dropped");
		//System.out.println( "name is '" + dnde.toString() + "'");
		
		if(adapt != null)
		{
		    String t = adapt.getName(dnde);
		    if(t != null)
		    {
			setText(t);
			//System.out.println( "adapted name is '" + t + "'");
		    }
		    else
		    {
			setText("");
			//System.out.println( "adapted name was null");
		    }
		}
		else
		{
		    setText(dnde.toString());
		}

		event.getDropTargetContext().dropComplete(true);
	    } 
	    else
	    {
		event.rejectDrop();
	    }
	}
	catch (IOException exception) 
	{
	    exception.printStackTrace();
	    System.err.println( "Exception" + exception.getMessage());
	    //event.rejectDrop();
	} 
	catch (UnsupportedFlavorException ufException ) 
	{
	    ufException.printStackTrace();
	    System.err.println( "Exception" + ufException.getMessage());
	    //event.rejectDrop();
	}
    }
    
    public void dropActionChanged ( DropTargetDragEvent event ) 
    {
    }

    
    // DropTarget dropTarget = null;
    
    private EntityAdaptor adapt = null;

    public void setEntityAdaptor(EntityAdaptor ea) { adapt = ea; }
    public EntityAdaptor getEntityAdaptor() { return adapt; }
    
}
    
