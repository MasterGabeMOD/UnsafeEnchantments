package me.mastergabemod.unsafeenchantfix;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.Map;

public class Main extends JavaPlugin implements Runnable {

    private static Main instance;

    public void onEnable() {
        instance = this;
        getServer().getScheduler().runTaskTimerAsynchronously(this, this, 200L, 200L); // Run the task every second

        // Register the command
        getCommand("unsafecheck").setExecutor(new UnsafeCheckCommand());
    }

    public static Main getInstance() {
        return instance;
    }

    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (ItemStack it : player.getInventory().getArmorContents()) {
                removeUnsafeEnchantments(it);
            }
            for (ItemStack it : player.getInventory().getContents()) {
                if (it != null) {
                    if (it.getType() == Material.POTION) {
                        if (!isVanillaPotion(it)) {
                            player.getInventory().removeItem(it);
                            player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));
                        } else {
                            // Check if the item has enchantments before trying to clear them
                            if (it.getEnchantments() != null && !it.getEnchantments().isEmpty()) {
                                it.getEnchantments().clear();
                            }
                        }
                    } else {
                        removeUnsafeEnchantments(it);
                    }
                }
            }
        }
    }

    public boolean isVanillaPotion(ItemStack it) {
        PotionMeta meta = (PotionMeta) it.getItemMeta();
        PotionData data = meta.getBasePotionData();
        PotionType type = data.getType();
        
        // Check for illegal enchantments on potions
        if (it.getEnchantments() != null && !it.getEnchantments().isEmpty()) {
            return false;
        }
        
        return type == PotionType.AWKWARD || type == PotionType.MUNDANE || type == PotionType.THICK || type == PotionType.WATER;
    }

    public void removeUnsafeEnchantments(ItemStack it) {
        if (it != null && it.getType() != Material.AIR) {
            for (Map.Entry<Enchantment, Integer> entry : it.getEnchantments().entrySet()) {
                Enchantment e = entry.getKey();
                int level = entry.getValue();
                if (level > e.getMaxLevel() || !(e.canEnchantItem(it) && it.getItemMeta().hasEnchants())) {
                    it.removeEnchantment(e);
                }
            }
        }
    }
}
