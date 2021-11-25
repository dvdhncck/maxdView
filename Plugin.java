public interface Plugin
{
    public void   startPlugin();
    public void   stopPlugin();
    
    public void   runCommand(String name, String[] args, CommandSignal done);
    
    public PluginInfo      getPluginInfo();
    public PluginCommand[] getPluginCommands();
}
