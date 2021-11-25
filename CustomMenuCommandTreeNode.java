import java.io.*;
import java.util.Vector;

public class CustomMenuCommandTreeNode implements Serializable
{
    public String name;
    public String hotkey;
    public boolean show_in_menu;
    public PluginCommand command;                 // null implies a branch node
    public CustomMenuCommandTreeNode[] children;

    public Vector spare;   // can be used for future expansion

    public CustomMenuCommandTreeNode( String nm, String hk, PluginCommand pc )
    {
	command = pc;
	name = nm;
	hotkey = hk;
	show_in_menu = true;
	children = null;
    }

    public CustomMenuCommandTreeNode( String n  )
    {
	name = n;
	show_in_menu = true;
	hotkey = null;
	command = null;
	children = null;
    }

    public CustomMenuCommandTreeNode copy(  )
    {
	CustomMenuCommandTreeNode cmc = new CustomMenuCommandTreeNode(  new String(name), 
									new String(hotkey),
									clonePluginCommand(command) );
	if( children != null )
	{
	    cmc.children = new CustomMenuCommandTreeNode[ children.length ];
	    
	    for(int c=0; c < children.length; c++)
		cmc.children[ c ] = children[ c ].copy();
	}
	else
	{
	    cmc.children = null;
	}

	cmc.show_in_menu = show_in_menu;

	return cmc;
    }

    public String toString() { return name; }

    public boolean isMenu( )
    {
	return (command == null);
    }
    
    public void setIsMenu( boolean is_menu )
    {
	if(is_menu)
	    command = null;
    }

    public void setChildren( CustomMenuCommandTreeNode[] ch )
    {
	children = ch;
    }

    public CustomMenuCommandTreeNode[] getChildren()
    {
	return children;
    }
    
    public int getNumChildren()
    {
	return children == null ? 0 : children.length;
    }

    // ----------------

    private PluginCommand clonePluginCommand( PluginCommand src )
    {
	if(src == null)
	    return null;

	PluginCommand pc = new PluginCommand ( null, null );
	pc.plugin_name = new String( src.plugin_name );
	pc.name = new String( src.name );

	if( src.args != null )
	{
	    pc.args = new String[ src.args.length ];
	    
	    for(int a=0; a < src.args.length; a++)
		pc.args[ a ] = new String(src.args[ a ]);
	}
	else
	{
	    pc.args = null;
	}

	return pc;
    }
}
