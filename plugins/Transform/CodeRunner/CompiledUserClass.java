import java.io.*;

//
// this cannot be an inner class of CodeRunner because that
// buggers up the serialisation as it wants to serialise the parent class too
//

public class CompiledUserClass implements Serializable
{
    public String name;          // name as used in the library list
    public String class_name;    // the name of the file containing the class
    public String code_name;     // the name of the file containing the code
    public String description;   // not used
}
