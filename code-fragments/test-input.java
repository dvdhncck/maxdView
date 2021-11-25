//
// tests the user input routines
//
public void doIt()
{
  try
  {

    // choice of 3 text options
    String[] mode_opts = { "Input numbers", "Input choices", "Input strings" };
    int mode = mview.getChoice("Test What?", mode_opts);
 
    if(mode == 0)
    {
      int i1 = mview.getInteger("number 1", 0, 255);
      int i2 = mview.getInteger("number 2", 0, 255, 64);

      mview.infoMessage("number 1 is " + i1 + "\nnumber 2 is " + i2);

      double d1 = mview.getDouble("number 3", .0, 1.0);
      mview.infoMessage("number 3 is " + d1);
    }
   
    if(mode == 1)
    {
      //choice of 3 text options
      String[] options1 = { "option 1", "option 2", "option 3" };
      int c1 = mview.getChoice("test3", options1);
 
      mview.infoMessage("test1 is " + options1[c1]);

      // choice of 3 text options, with an inital setting
      String[] options2 = { "fast", "normal", "Slow" };
      int c2 = mview.getChoice("test4", options2, 2);

      mview.infoMessage("test2 is " + options2[c2]);
    }
 

    if(mode == 2)
    {
      String s1 = mview.getString("what is your name?");
      String s2 = mview.getString("how many arms do you have", "two");

    } 
  }
  catch(UserInputCancelled e)
  {
  }
}
