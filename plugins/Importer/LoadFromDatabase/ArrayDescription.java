public class ArrayDescription
{
    public Instance[] features;
    
    public Instance[] reporters;   // note that there will be repeats in this array if there are replicates on the microarray
    public Instance[][] genes;     // note that there will be repeats in this array if there are replicates on the microarray

    // this is used to arrange the property values into a consistent order
    public java.util.Hashtable feature_id_to_index;

    
    // the attribute_id's for the features, reporters & genes

    public java.util.Hashtable feature_to_attribute_id;
    public java.util.Hashtable reporter_to_attribute_id;
    public java.util.Hashtable gene_to_attribute_id;

}
