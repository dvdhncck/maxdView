public class UserInputCancelled extends Exception
{
    public UserInputCancelled(String iname)
    {
	input_name = iname;
    }
    public String toString() { return input_name; }

    private String input_name;
}
