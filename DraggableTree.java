
/*
  a JTree with support for moving things around using drag-n-drop

  (c)2002 David Hancock
*/

import java.awt.*;
import java.io.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

public class DraggableTree extends JTree implements DropTargetListener, DragSourceListener, DragGestureListener    
{

    DropTarget dropTarget = null;
    DragSource dragSource = null;
    
    DefaultMutableTreeNode drop_node_above;   // used to show where the drop will occur
    DefaultMutableTreeNode drop_node_below;   // used to show where the drop will occur

    DefaultMutableTreeNode drag_node;   // records which node is being dragged

    boolean allow_reparenting = true;

    public DraggableTree()
    {
	dropTarget = new DropTarget (this, this);
	dragSource = DragSource.getDefaultDragSource(); // new DragSource();
	dragSource.createDefaultDragGestureRecognizer( this, DnDConstants.ACTION_MOVE, this);

	setCellRenderer(new CustomTreeCellRenderer());


    }

    public void setAllowReparenting( boolean ar )
    {
	allow_reparenting = ar;
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

	TreePath tp = getClosestPathForLocation( (int)pt.getX() , (int)pt.getY() );
	
	if(tp != null)
	{
	    DefaultMutableTreeNode node = (DefaultMutableTreeNode) tp.getLastPathComponent();
	    
	    if(node != null)
	    {
		DefaultTreeModel tmodel = (DefaultTreeModel) getModel();
		DefaultMutableTreeNode root_node = (DefaultMutableTreeNode) tmodel.getRoot();
		
		if(node != root_node)
		{
		    if( isDropAllowed( node, drag_node ) )
		    {
			String selected = (String) node.getUserObject();
			
			// System.out.println("DropTargetListener: dragOver() : " + selected);
			
			// above or below the drop node?
			
			int row = getRowForPath( tp );
			Rectangle rect = getRowBounds(row);
			
			int mid_y = rect.y + (rect.height/2);
			
			// record which node needs to be altered in the visual representation
			
			if( (int)pt.getY() >= mid_y )
			{
			    drop_node_above = null;
			    drop_node_below = node;
			}
			else
			{
			    drop_node_below = null;
			    drop_node_above = node;
			}
			
			repaint();
			
			return;
		    }
		    else
		    {
			drop_node_above = drop_node_below = null;
			repaint();
		    }
		}
	    }
	}
	
	drop_node_above = drop_node_below = null;
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
		    DefaultMutableTreeNode drop_node = drop_node_above;
		    if(drop_node == null)
			drop_node = drop_node_below;
		    
		    //Point pt = event.getLocation();
		    //TreePath tp = getPathForLocation( (int)pt.getX() , (int)pt.getY() );

		    if( drop_node != null )
		    {
			if( isDropAllowed( drop_node, drag_node ) )
			{
			    
			    DefaultMutableTreeNode drop_parent = (DefaultMutableTreeNode) drop_node.getParent();
			    event.acceptDrop(DnDConstants.ACTION_MOVE);
			    event.getDropTargetContext().dropComplete(true);
			    
			    String selected = (String) drop_node.getUserObject();
			    
			    // System.out.println("DropTargetListener: drop() accepted over " + selected);
			    
			    
			    // remove the dragged node from it's current position
			    DefaultTreeModel tmodel = (DefaultTreeModel) getModel();
			    tmodel.removeNodeFromParent( drag_node );
			    
			    //and insert it at the drop position
			    if( drop_node.isLeaf() )
			    {
				// dropping before or after another leaf
				int position = tmodel.getIndexOfChild( drop_parent, drop_node );
				if(drop_node == drop_node_below)
				    position++;
				int n_children = drop_parent.getChildCount();
				if(position > n_children)
				    position = n_children;
				tmodel.insertNodeInto( drag_node, drop_parent, position );
			    }
			    else
			    {
				// dropping directly into a menu.....
				tmodel.insertNodeInto( drag_node, drop_node, 0 );
			    }
			} 
			// and repaint the tree
			drop_node_above = drop_node_below = null;
			repaint();
			
			return;
		    }
		}
	    }
	    
	    drop_node_above = drop_node_below = null;
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

	drop_node_above = drop_node_below = null;
	repaint();
    }

    public void dropActionChanged ( DropTargetDragEvent event ) 
    {
	//System.out.println("DropTargetListener: dropActionChanged()");
    }
    

    private boolean isDropAllowed( DefaultMutableTreeNode src, DefaultMutableTreeNode dest )
    {
	if( allow_reparenting )
	    return true;
	else
	    return ( src.getParent() == dest.getParent() );
    }

    private boolean isDragAllowed( DefaultMutableTreeNode src )
    {
	if( allow_reparenting )
	    return true;
	else
	    return ( src.getChildCount() == 0 );
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
	    TreePath tpath = getSelectionPath();

	    if(tpath != null)
	    {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) tpath.getLastPathComponent();
	    
		if(node != null)
		{
		    if( isDragAllowed( node ))
		    {
			String selected = (String) node.getUserObject();
			
			if (selected != null)
			{
			    // System.out.println( " drag start.... with " + selected);
			    
			    CustomTransferable ct = new CustomTransferable( selected );
			    
			    dragSource.startDrag (event, DragSource.DefaultMoveDrop, ct, this);
			
			    drag_node = node;
			    
			    return;
			}
		    }
		}
	    }
	}
	catch(ArrayIndexOutOfBoundsException aioobe)
	{
	}
	
	drag_node = null;
    }
    
    //
    // ======================================================
    //  Transferable interface
    // ======================================================
    //

    final private static DataFlavor 
	DragAndDropEntityFlavour = new DataFlavor("x-application/java-maxdView-EditableTree", 
						  "maxdView-EditableTree-DragAndDropEntity");
    
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

    public class CustomTreeCellRenderer extends DefaultTreeCellRenderer
    {
	private boolean highlight_above = false;
	private boolean highlight_below = false;

 	public void paintComponent(Graphics g)
	{
	    super.paintComponent(g);

	    if(highlight_above)
	    {
		Icon icon = getIcon();
		int offset = (icon == null) ? 0 : icon.getIconWidth();
		g.setColor( Color.black );
		g.drawLine( offset, 1, getWidth(), 1 );
	    }
	    if(highlight_below)
	    {
		Icon icon = getIcon();
		int offset = (icon == null) ? 0 : icon.getIconWidth();
		g.setColor( Color.black );
		g.drawLine( offset, getHeight()-1, getWidth(), getHeight()-1 );
	    }
	}

	public Component getTreeCellRendererComponent(JTree tree,
						      Object value,
						      boolean sel,
						      boolean expanded,
						      boolean leaf,
						      int row,
						      boolean hasFocus) 
	{
	    
	    super.getTreeCellRendererComponent(tree, value, sel,
					       expanded, leaf, row,
					       hasFocus);

	    DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
	    
	    // if a drag is underway, then indicate the drop location
	    highlight_above = highlight_below = false;

	    if((drop_node_above != null) && (node == drop_node_above))
	    {
		highlight_above = true;
	    }
	    if((drop_node_below != null) && (node == drop_node_below))
	    {
		highlight_below = true;
	    }


	    return this;
	}
    }

}
