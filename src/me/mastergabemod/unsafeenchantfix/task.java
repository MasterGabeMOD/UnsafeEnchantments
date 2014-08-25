package me.mastergabemod.unsafeenchantfix;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import me.mastergabemod.unsafeenchantfix.main;


public class task extends JavaPlugin
{

    public task()
    {
    }
    public void onEnable()
    {
    	
Bukkit.getScheduler().runTaskTimer(this, new main(), 100L, 100L);

}
}