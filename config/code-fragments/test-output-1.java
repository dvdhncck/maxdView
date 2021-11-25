//
// demonstrates the question and information dialogs
//
public void doIt()
{
  //try
  {
    mview.infoMessage("This is an information message");


    mview.infoMessage("This\n is\n an\ni nformation\n message");
    
    if(mview.infoQuestion("Continue with this demo?", "Yes", "No") == 1)
      return;

    if(mview.alertQuestion("Really continue this demo?", "Yes", "No") == 1)
      return;
  }
  //catch(UserInputCancelled uic)
  {
  } 
}
