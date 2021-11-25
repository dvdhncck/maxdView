public class MeasDataProp
{
    public MeasDataProp( String n, String u /*, String dt*/)
    {
	name = n;
	unit = u;
	//data_type = dt.toUpperCase();
	data_type_code = UnknownDataType;
	col_spec = null;
	type_id = null;
	quant_type = null;
	scale = null;
	origin = null;
	channel_instance = null;
    }

    public MeasDataProp()
    {
	data_type_code = UnknownDataType;
	name = unit = origin = quant_type = col_spec = scale = null;
    }

    public MeasDataProp( String n, String u, String cs )
    {
	name = n;
	unit = u;
	data_type_code = UnknownDataType;
	//data_type = dt.toUpperCase();
	col_spec = cs;
	type_id = null;
	origin = null;
	channel_instance = null;
    }

    public MeasDataProp( String n, String u, String cs, String qt, String s, String o, String lex )
    {
	name = n;
	unit = u;
	data_type_code = UnknownDataType;
	//data_type = dt.toUpperCase();
	col_spec = cs;
	quant_type = qt;
	scale = s;
	origin = o;
	type_id = null;

	// lookup the the LabelledExtract name and convert to 'channel_instance'
	// ( only do this if there is a single LabelledExtract with a matching name )
	//
	/*
	Instance[] lex_insts = cntrl.getConnection().getMostRecentInstancesFromName( lex, "LabelledExtract" );
	if( ( lex_insts != null ) && ( lex_insts.length == 1 ) )
	    channel_instance = lex_insts[ 0 ];
	else
	    channel_instance = null;
	*/
    }

    public MeasDataProp( String name_, String type_id_, Instance channel_instance_, java.util.Hashtable attr_vals_ht )
    {
	name = name_;
	type_id = type_id_;
	channel_instance = channel_instance_;
	
	if( attr_vals_ht != null )
	{
	    scale          = (String) attr_vals_ht.get("Scale");
	    origin         = (String) attr_vals_ht.get("Origin");
	    unit           = (String) attr_vals_ht.get("Unit");
	    quant_type     = (String) attr_vals_ht.get("Quantitation_Type");
	    data_type_code = getDataTypeCode( (String) attr_vals_ht.get("Data_Type") );
	}
    }

    public void setDataType( String dt )
    {
	data_type_code = getDataTypeCode( dt );
    }

    public Instance channel_instance;

    public String name;
    public String unit;
    public int data_type_code;
    public String quant_type;      // corresponds to MAGE 'QuantitationType' names
    public String scale;           // corresponds to MAGE 'Scale' names
    public String origin;          // corresponds to MAGE 'isBackground' boolean
    public String col_spec;

    public String type_id;

/*
  public JCheckBox  sel_jchkb;

  public JTextField colspec_jtf;
  public MemoryInstancePicker channel_mip;
  public InstanceHandler channel_picker_ih;   // only used when the user goes for a manual 'Select' operation
  public JTextField name_jtf;
  public JTextField unit_jtf;
  public JComboBox scale_jcb;
  public JComboBox origin_jcb;
  public JComboBox quant_type_jcb;
*/

    // note that these types are in order of precedence, i.e. a double is 'worse' than an integer
    public final static  String[] data_type_names = 
    { 
	"INTEGER", "DOUBLE", "CHAR", "STRING"
    };

    public final static int UnknownDataType = -1;

    public final static int IntegerDataType = 0;
    public final static int DoubleDataType  = 1;
    public final static int CharDataType    = 2;
    public final static int StringDataType  = 3;

    public final static int getDataTypeCode( final String s )
    {
	if( s == null )
	    return UnknownDataType;

	for(int i=0; i < data_type_names.length; i++)
	    if( data_type_names[ i ].equals( s ) )
		return i;
	return UnknownDataType;
    }

    public final static String getDataTypeName( final int c ) { return data_type_names[ c ]; }

    public final static String[] mage_quantitation_types = 
    { 
	"MeasuredSignal", "Ratio", "PresentAbsent", "Failed", "DerivedSignal", 
	"PValue", "Error", "ExpectedValue",
	"SpecializedQuanititationType"
    };

    public final static String[] mage_scales = 
    { 
	"LINEAR","LN","LOG2","LOG10","FOLD_CHANGE","OTHER"
    };

    public final static String[] mage_origins = 
    { 
	"Feature", "Background"
    };


}
