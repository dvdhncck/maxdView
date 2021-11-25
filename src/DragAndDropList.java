/**
 * originally lifted directly from the Java tutorial example by Sheetal Gupta
 *
 * @version 1.0
 */

//
// use the EntityAdaptor subclass to extract a name from DragAndDropEntity based
// on the context in which the DragAndDropTextField is being used.
//

import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;

import java.util.Vector;
import java.util.List;
import java.util.Iterator;

import java.io.*;
import java.io.IOException;

import javax.swing.JList;
import javax.swing.DefaultListModel;




public class DragAndDropList 
    extends JList
    implements DropTargetListener, DragSourceListener, DragGestureListener    
{
    
    Vector dnde_v = null;
    DropTarget dropTarget = null;
    DragSource dragSource = null;
    
    
    public DragAndDropList() 
    {
	dnde_v = new Vector();

	dropTarget = new DropTarget (this, this);
	dragSource = DragSource.getDefaultDragSource(); // new DragSource();
	dragSource.createDefaultDragGestureRecognizer( this, DnDConstants.ACTION_MOVE, this);
    }
    
    //
    // ======================================================
    //  direct connect
    // ======================================================
    //

    public void addEntity(DragAndDropEntity dnde)
    {
	String text = "(unresolved)";

	if(adapt != null)
	{
	    text = adapt.getName(dnde);
	}
	else
	{
	    text = dnde.toString();
	}

	(( DefaultListModel )getModel()).addElement ( text );
	
	// and store the entity for later dragging...
	//
	dnde_v.addElement(dnde);
    }

    public void makeEmpty()
    {
	Vector dnde_v = new Vector();
	((DefaultListModel)getModel()).removeAllElements();
    }
    //
    // ======================================================
    //  DropTargetListener interface
    // ======================================================
    //

    public void dragEnter (DropTargetDragEvent event) 
    {
	//System.out.println("DropTargetListener: dragEnter()");

	event.acceptDrag (DnDConstants.ACTION_MOVE);
    }
    
    public void dragExit (DropTargetEvent event) 
    {
	//System.out.println("DropTargetListener: dragExit()");
    }
    
    public void dragOver (DropTargetDragEvent event) 
    {
	//System.out.println("DropTargetListener: dragOver()");
    }

    public void drop (DropTargetDropEvent event) 
    {
	//System.out.println("DropTargetListener: drop()");

	try 
	{
	    Transferable transferable = event.getTransferable();

	    DragAndDropEntity dnde = null;
	    String text = null;
		
	    if(transferable.isDataFlavorSupported (DragAndDropEntity.DragAndDropEntityFlavour))
	    {
		dnde = (DragAndDropEntity) transferable.getTransferData(DragAndDropEntity.DragAndDropEntityFlavour);
		

		if(adapt != null)
		{
		    text = adapt.getName(dnde);

		    //System.out.println( "adapted name is '" + text + "'");
		}
		else
		{
		    text = dnde.toString();
		}
		
		if(text != null)
		{
		    event.acceptDrop(DnDConstants.ACTION_MOVE);
		    
		    // System.out.println( "'" + dnde.getEntityType() + "' dropped");
		    //System.out.println( "name is '" + text + "'");

		    if(drop_action != null)
		    {
			drop_action.dropped(dnde);		    
		    }
		    else
		    {
			try
			{
			    (( DefaultListModel )getModel()).addElement ( text );
			}
			catch(java.lang.ClassCastException cce)
			{
			    // list is using a diferent model, don't do an insert....
			}
		    }

		    // and store the entity for later dragging...
		    //
		    dnde_v.addElement(dnde);
		    
		    event.getDropTargetContext().dropComplete(true);
		    
		    //System.out.println("DropTargetListener: drop() accepted ");
		}
		else
		{
		    //System.out.println("DropTargetListener: drop() rejected due to null text ");
		    event.rejectDrop();
		}
	    }
	    else
	    {
		//System.out.println("DropTargetListener: drop() rejected due to incorrect flavour");
		event.rejectDrop();
	    }
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
	    Object selected = getSelectedValue();
	    if (selected != null)
	    {
		//System.out.println( " drag start....");
		
		DragAndDropEntity dnde = null;
		
		if(drag_action != null)
		{
		    dnde = drag_action.getEntity();
		}
		else
		{
		    dnde = (DragAndDropEntity) dnde_v.elementAt(getSelectedIndex());
		}
		
		if(dnde == null)
		{
		    //System.out.println( " dragging null thing");
		}
		else
		{
		    //System.out.println( " dragging " + dnde.toString());
		    dragSource.startDrag (event, DragSource.DefaultMoveDrop, dnde, this);
		}
	    } 
	}
	catch(ArrayIndexOutOfBoundsException aioobe)
	{
	}
    }
    
    //
    // ======================================================
    //  DragSourceListener interface
    // ======================================================
    //

    public void dragDropEnd (DragSourceDropEvent event) 
    {   
	//System.out.println( "DragSourceListener.dragDropEnd()");
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
    
    public void          setDropAction(DropAction da) { drop_action = da; }
    public DropAction getDropAction()                 { return drop_action; }

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
    //  handy utilty to set the selection
    // ======================================================
    //
    // a convenience function for working with JLists
    public void selectItems( String[] items )
    {
	try
	{
	    javax.swing.ListModel dlm = (javax.swing.ListModel) getModel();
	    
	    java.util.Hashtable ht = new java.util.Hashtable();
	    for(int i=0; i < dlm.getSize(); i++)
		ht.put(dlm.getElementAt(i), new Integer(i));


	    
	    int[] list_indices = new int[ items.length ];

	    int hits = 0;
	    for(int i=0; i < items.length; i++)
	    {
		try
		{
		    int index = ((Integer) ht.get( items[i] )).intValue();
		    list_indices[hits++] = index;
		}
		catch(Exception ex)
		{
		    // null pointer means item not found in list
		}
	    }
	    
	    
	    if(hits < items.length)
	    {
		int[] shortened = new int[hits];
		for(int i=0; i < hits; i++)
		    shortened[i] = list_indices[i];
		list_indices = shortened;
	    }
	    
	    setSelectedIndices( list_indices );

	    if(list_indices.length > 0)
		ensureIndexIsVisible( list_indices[0] );
	}
	catch(ClassCastException cce)
	{
	    // it wasn't a ListModel then....
	    
	}
    }

    public void selectAll( )
    {
	javax.swing.ListModel dlm = (javax.swing.ListModel) getModel();
	setSelectionInterval(0, dlm.getSize()-1);
    }
    
}
