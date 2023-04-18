package me.mastergabemod.unsafeenchantfix;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;


public class task extends JavaPlugin {

    public task() {

    }
    public void onEnable() {
        Bukkit.getScheduler().runTaskTimer(this, new Main(), 100L, 100L);
    }
}