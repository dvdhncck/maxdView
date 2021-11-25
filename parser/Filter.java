import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.Vector;

import java.io.*;

public class Filter
{

    private class TypeMismatch extends Throwable
    {
	public TypeMismatch( String s ) { super( s ); }
    }

    private interface Operand
    {
	public String name;
	public int type;
	
	public abstract int     getInt(int index)    throws TypeMismatch;
	public abstract double  getDouble(int index) throws TypeMismatch;
	public abstract char    getChar(int index)   throws TypeMismatch;
	public abstract String  getText(int index)   throws TypeMismatch;

	public abstract boolean lessThan(int index, Operand op)    throws TypeMismatch;
	public abstract boolean greaterThan(int index, Operand op) throws TypeMismatch;
	public abstract boolean equals(int index, Operand op)      throws TypeMismatch;
	public abstract boolean notEquals(int index, Operand op)   throws TypeMismatch;

	public abstract boolean contains(int index, Operand op) throws TypeMismatch;
	public abstract boolean matches(int index, Operand op)  throws TypeMismatch;

	public String toString(int index)
	{ 
	    try 
	    { 
		return ((name == null) ? "const:" : (name + ":")) + getText(index); 
	    }
	    catch(TypeMismatch tm) 
	    { 
		return "TypeMismatch"; 
	    }

	}
    }

    public final int IntOperand    = 0;
    public final int DoubleOperand = 1;
    public final int CharOperand   = 2;
    public final int TextOperand   = 3;

    // need special handling for NaNs
    //
    //  lessThan, greaterThan:
    //
    //           NaN    !NaN
    //  NaN     false   true
    //  !NaN    true   normal
    //

    private final boolean doubleLessThan(double d1, double d2)
    {
	if(Double.isNaN(d1))
	{
	    return Double.isNaN(d2) ? false : true; 
	}
	else
	{
	    return Double.isNaN(d2) ? true : (d1 < d2);
	}
    }
    private final boolean doubleGreaterThan(double d1, double d2)
    {
	if(Double.isNaN(d1))
	{
	    return Double.isNaN(d2) ? false : true; 
	}
	else
	{
	    return Double.isNaN(d2) ? true : (d1 > d2);
	}
    }

    //  equals & notEquals
    //
    //           NaN    !NaN
    //  NaN     true    false
    //  !NaN    false   normal
    //
    private final boolean doubleEquals(double d1, double d2)
    {
	if(Double.isNaN(d1))
	{
	    return Double.isNaN(d2) ? true : false; 
	}
	else
	{
	    return Double.isNaN(d2) ? false : (d1 == d2);
	}
    }
    
    private final boolean doubleNotEquals(double d1, double d2)
    {
	if(Double.isNaN(d1))
	{
	    return Double.isNaN(d2) ? false : true; 
	}
	else
	{
	    return Double.isNaN(d2) ? true : (d1 != d2);
	}
    }

    // -----------------------------------------------------------------------------------------------

    private class PropertyOperand extends Operand
    {
	public PropertyOperand( String name_ ) { name = name_; }
	public String name;
    }

    private class PropertyIntegerOperand extends PropertyOperand
    {
	public PropertyIntegerOperand( String name_, int[] data_v_ ) { name = name_;  data_v = data_v_; }

	public int[] data_v;

	public final int getInt(int index)    throws TypeMismatch 
	{ 
	    return data_v[index];
	}
	public final double getDouble(int index) throws TypeMismatch
	{ 
	    return (double) data_v[index]; 
	}
	public final char   getChar(int index)   throws TypeMismatch 
	{ 
	    throw new TypeMismatch( "Cannot convert the INTEGER '" + data_v[ index ] "' to a CHARACTER value in '" + name + "'" );
	}
	public final String getText(int index)   throws TypeMismatch
	{ 
	    return String.valueOf( data_v[index] ); 
	}

	public final boolean lessThan(int index, Operand op)    throws TypeMismatch { return data_v[index] < op.getInt(index); }
	public final boolean greaterThan(int index, Operand op) throws TypeMismatch { return data_v[index] > op.getInt(index); }
	public final boolean equals(int index, Operand op)      throws TypeMismatch { return data_v[index] == op.getInt(index); }
	public final boolean notEquals(int index, Operand op)   throws TypeMismatch { return data_v[index] != op.getInt(index); }
    }

     
    private class PropertyDoubleOperand extends PropertyOperand
    {
	public PropertyDoubleOperand( String name_, double[] data_v_ ) { name = name_;  data_v = data_v_; }

	public double[] data_v;

	public final int    getInt(int index)    throws TypeMismatch 
	{ 
	    if( Double.isNaN( data_v[index] ) )
		throw new TypeMismatch("Cannot convert 'NaN' value to an INTEGER value in '" + name + "'" );
	    return (int) data_v[index]; 
	}

	public final double getDouble(int index) throws TypeMismatch { return data_v[index]; }

	public final char   getChar(int index)   throws TypeMismatch 
	{ 
	    throw new TypeMismatch("Cannot convert DOUBLE value '" + data_v[index] + "' to a CHARACTER value in '" + name + "'" );
	}

	public final String getText(int index)   throws TypeMismatch 
	{ 
	    if( Double.isNaN( data_v[index] ) )
		return "NaN";
	    else
		return String.valueOf(data_v[index]); 
	}

	public final boolean lessThan(int index, Operand op)    throws TypeMismatch { return doubleLessThan( data_v[index], op.getDouble(index) ); }
	public final boolean greaterThan(int index, Operand op) throws TypeMismatch { return doubleGreaterThan( data_v[index], op.getDouble(index) ); }
	public final boolean equals(int index, Operand op)      throws TypeMismatch { return doubleEquals( data_v[index],  op.getDouble(index) ); }
	public final boolean notEquals(int index, Operand op)   throws TypeMismatch { return doubleNotEquals( data_v[index], op.getDouble(index ) ); }

	public PropertyDoubleOperand(String name_, double[] data_v_) { name= name_;  data_v = data_v_; }
    }
     

    private class PropertyCharOperand extends PropertyOperand
    {
	public char[] data_v;

	public final int  getInt(int index)  throws TypeMismatch 
	{ 
	    try
	    {
		Integer i = Integer.valueOf( String.valueOf( data_v[index] ) );
		return i.intValue();
	    }
	    catch(NumberFormatException nfe)
	    {
		throw new TypeMismatch( "Cannot convert CHARACTER value '" + data_v[index] + "' to an INTEGER value in '" + name + "'" );
	    }
	}

	public final double getDouble(int index) throws TypeMismatch 
	{ 
	    try
	    {
		Double d = Double.valueOf( String.valueOf( data_v[index] ) );
		return d.doubleValue();
	    }
	    catch(NumberFormatException nfe)
	    {
		throw new TypeMismatch( "Cannot convert CHARACTER value '" + data_v[index] + "' to a DOUBLE value in '" + name + "'" );
	    }
	}

	public final char   getChar(int index)   throws TypeMismatch { return data_v[index]; }

	public final String getText(int index)   throws TypeMismatch { return String.valueOf(data_v[index]); }

	public final boolean lessThan(int index, Operand op)    throws TypeMismatch { return data_v[index] < op.getChar(index); }
	public final boolean greaterThan(int index, Operand op) throws TypeMismatch { return data_v[index] > op.getChar(index); }
	public final boolean equals(int index, Operand op)      throws TypeMismatch { return data_v[index] == op.getChar(index); }
	public final boolean notEquals(int index, Operand op)   throws TypeMismatch { return data_v[index] != op.getChar(index); }

	public PropertyCharOperand(String name_, int meas_id_, char[] data_v ) 
	{ 
	    name= name_; 
	    data_v = data_v_;
	}
    }

    private class PropertyTextOperand extends PropertyOperand
    {
	public String[] data_v;

	public final int  getInt(int index)    throws TypeMismatch 
	{ 
	    try
	    {
		Integer i = Integer.valueOf( data_v[index] );
		return i.intValue();
	    }
	    catch(NumberFormatException nfe)
	    {
		throw new TypeMismatch( "Cannot convert TEXT value '" + data_v[index] + "' to an INTEGER value in '" + name + "'"  ); 
	    }
	}

	public final double getDouble(int index) throws TypeMismatch 
	{ 
	    try
	    {
		Double d = Double.valueOf( data_v[index] );
		return d.doubleValue();
	    }
	    catch(NumberFormatException nfe)
	    {
		throw new TypeMismatch( "Cannot convert TEXT value '" + data_v[index] + "' to a DOUBLE value in '"+ name + "'"  ); 
	    }
	}
	
	public final char   getChar(int index)   throws TypeMismatch 
	{ 
	    if( data_v[index] == null )
		throw new TypeMismatch( "Cannot convert missing TEXT to CHARACTER value in '"+ name + "'"  ); 

	    if( data_v[index].length() == 1 )
		return data_v[index].charAt( 0 );

	    throw new TypeMismatch( "Cannot convert TEXT value '" + data_v[index] + "' to a CHARACTER value in '" + name + "'"  ); 
	}
	
	public final String getText(int index)   throws TypeMismatch 
	{ 
	    return data_v[index]; 
	}
	
	public final boolean lessThan(int index, Operand op)    throws TypeMismatch 
	{ return data_v[index] == null ? true :  (data_v[index].compareTo(op.getText(index)) < 0); }

	public final boolean greaterThan(int index, Operand op) throws TypeMismatch 
	{ return data_v[index] == null ? true :  (data_v[index].compareTo(op.getText(index)) > 0); }

	public final boolean equals(int index, Operand op)      throws TypeMismatch 
	{ 
	    if(data_v[index] == null)
		return (op.getText(index) == null);
	    else
		return data_v[index].equals(op.getText(index));
	}
	
	public final boolean notEquals(int index, Operand op)   throws TypeMismatch 
	{ 
	    if(data_v[index] == null)
		return (op.getText(index) != null);
	    else
		return !(data_v[index].equals(op.getText(index)));
	}

	public PropertyTextOperand(String name_, String[] data_v_) { name= name_; data_v = data_v_; }
    }


    // -----------------------------------------------------------------------------------------------
    
    
    private class ConstIntOperand extends Operand
    {
	public int v;
	
	public final int    getInt(int index)    throws TypeMismatch { return v; }
	public final double getDouble(int index) throws TypeMismatch { return (double) v; }
	public final char   getChar(int index)   throws TypeMismatch 
	{ 
	    if( v >= 0 && v <= 10 )
		return Character.forDigit( v, 10 );
	    else
		throw new TypeMismatch( "Cannot convert INTEGER value '" + v + "' to a CHARACTER" ); 
	}
	public final String getText(int index)   throws TypeMismatch { return String.valueOf(v); }

	public final boolean lessThan(int index, Operand op)    throws TypeMismatch 
	{ return v < op.getInt(index); }
	public final boolean greaterThan(int index, Operand op) throws TypeMismatch
	{ return v > op.getInt(index); }
	public final boolean equals(int index, Operand op)      throws TypeMismatch 
	{ return v == op.getInt(index); }
	public final boolean notEquals(int index, Operand op)   throws TypeMismatch 
	{ return v != op.getInt(index); }

	public ConstIntOperand(int i)  { v = i; name = String.valueOf(i); }
    }
    private class ConstDoubleOperand extends Operand
    {
	public double v;

	public final int    getInt(int index)    throws TypeMismatch 
	{ 
	    if( Double.isNaN( v ) )
		throw new TypeMismatch("Cannot convert DOUBLE value 'NaN' to an INTEGER value" );
	    return (int) v; 
	}
	public final double getDouble(int index) throws TypeMismatch { return v; }
	public final char   getChar(int index)   throws TypeMismatch 
	{ 
	    throw new TypeMismatch("Cannot convert DOUBLE value '" + v + "' to a CHARACTER value" ); 
	}
	public final String getText(int index)   throws TypeMismatch 
	{
	    if( Double.isNaN( v ) )
		return "NaN";
	    else
		return String.valueOf(v); 
	}

	public final boolean lessThan(int index, Operand op)    throws TypeMismatch 
	{ 
	    return doubleLessThan(v, op.getDouble(index));
	}
	public final boolean greaterThan(int index, Operand op) throws TypeMismatch 
	{ 
	    return doubleGreaterThan(v, op.getDouble(index));
	}
	public final boolean equals(int index, Operand op)      throws TypeMismatch 
	{ 
	    return doubleEquals(v, op.getDouble(index));
	}
	public final boolean notEquals(int index, Operand op)  throws TypeMismatch 
	{ 
	    return doubleNotEquals(v, op.getDouble(index));
	}

	public ConstDoubleOperand(double d) { v = d; name = String.valueOf(d); }
    }

    private class ConstTextOperand extends Operand
    {
	public String v;

	public final int    getInt(int index)    throws TypeMismatch 
	{
	    try
	    {
		Integer i = Integer.valueOf( v );
		return i.intValue();
	    }
	    catch(NumberFormatException nfe)
	    {
		throw new TypeMismatch( "Cannot convert TEXT '" + v + "' to an INTEGER value"); 
	    }
	}

	public final double getDouble(int index) throws TypeMismatch 
	{
	    if( v == null )
		throw new TypeMismatch( "Cannot convert missing TEXT value to a DOUBLE value"); 

	    if( v.equals("NaN" ) )
		return Double.NaN;

	    try
	    {
		Double d = Double.valueOf( v );
		return d.intValue();
	    }
	    catch(NumberFormatException nfe)
	    {
		throw new TypeMismatch( "Cannot convert TEXT value '" + v + "' to a DOUBLE value"); 
	    }
	}
	
	public final char   getChar(int index)   throws TypeMismatch 
	{ 
	    if( v == null )
		throw new TypeMismatch( "Cannot convert missing TEXT value to a CHARACTER"); 

	    if( v.length() == 1 )
		return v.charAt(0); 
	    
	    throw new TypeMismatch( "Cannot convert TEXT value '" + v + "' to a CHARACTER value " );
	}
	public final String getText(int index)   throws TypeMismatch { return v; }

	public final boolean lessThan(int index, Operand op)    throws TypeMismatch { return (v.compareTo(op.getText(index)) < 0); }
	public final boolean greaterThan(int index, Operand op) throws TypeMismatch { return (v.compareTo(op.getText(index)) > 0); }
	public final boolean equals(int index, Operand op)      throws TypeMismatch { return v.equals(op.getText(index)); }
	public final boolean notEquals(int index, Operand op)   throws TypeMismatch { return !v.equals(op.getText(index)); }

	public ConstTextOperand(String s) { v = s; name = s; }
    }

    private class ConstCharOperand extends Operand
    {
	public char v;

	public final int    getInt(int index)    throws TypeMismatch 
	{ 
	    try
	    {
		Integer i = Integer.valueOf( String.valueOf( v ) );
		return i.intValue();
	    }
	    catch(NumberFormatException nfe)
	    {
		throw new TypeMismatch( "Cannot convert CHARACTER value '" + v + "' to an INTEGER value" ); 
	    }
	}

	public final double getDouble(int index) throws TypeMismatch
	{ 
	    try
	    {
		Double d = Double.valueOf( String.valueOf( v ) );
		return d.doubleValue();
	    }
	    catch(NumberFormatException nfe)
	    {
		throw new TypeMismatch( "Cannot convert CHARACTER value '" + v + "' to a DOUBLE value" ); 
	    }
	}

	public final char   getChar(int index)   throws TypeMismatch { return v; }
	public final String getText(int index)   throws TypeMismatch { return String.valueOf(v); }

	public final boolean lessThan(int index, Operand op)    throws TypeMismatch { return v < op.getChar(index); }
	public final boolean greaterThan(int index, Operand op) throws TypeMismatch { return v > op.getChar(index); }
	public final boolean equals(int index, Operand op)      throws TypeMismatch { return v == op.getChar(index); }
	public final boolean notEquals(int index, Operand op)   throws TypeMismatch { return v != op.getChar(index); }

	public ConstCharOperand(char c)  { v = c; name = String.valueOf(c); }
    }


    // -----------------------------------------------------------------------------------------------


    public Operand makeConstOperand(String str_in)
    {
	String str = str_in.trim();

	// System.out.println( " makeOperand() : " + str );

	if(str.toLowerCase().equals("nan"))
	{
	    // System.out.println("that's a NaN that is...");

	    return new ConstDoubleOperand(Double.NaN);
	}
	
	try
	{
	    int i = (Integer.valueOf(str)).intValue();
	    // System.out.println("that's an integer...");
	    return new ConstIntOperand(i);
	}
	catch(NumberFormatException i_nfe)
	{
	    try
	    {
		double d = (Double.valueOf(str)).doubleValue();
		// System.out.println("that's an double...");
		return new ConstDoubleOperand(d);
	    }
	    catch(NumberFormatException d_nfe)
	    {
		if(str.length() == 1)
		{
		    // System.out.println("that's a char...");
		    return new ConstCharOperand(str.charAt(0));
		}
		else
		{
		    // System.out.println("that's some text...");
		    return new ConstTextOperand(str);
		}
	    }
	}
    }



    // -----------------------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------------------


    /*
      syntax:

	  A > B and B > C

	  3 * C < D

          any of ( A, B, C, D ) > 5

	  all of ( *.value ) != NaN

	  at least 2 of ( A, B, C ) == 0

	  at most 1 of ( A > B, B > C, C > D ) == true

	  
    */


    // -----------------------------------------------------------------------------------------------
    // -----------------------------------------------------------------------------------------------

}
