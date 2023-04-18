package me.mastergabemod.unsafeenchantfix;

import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

public class Main implements Runnable {

    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() != GameMode.CREATIVE) {
                continue; // Ignore players in Survival mode
            }
            for (ItemStack it : player.getInventory().getArmorContents()) {
                removeUnsafeEnchantments(it);
            }
            for (ItemStack it : player.getInventory().getContents()) {
                if (it != null && it.getType() == Material.POTION) {
                    if (!isVanillaPotion(it)) {
                        // Remove illegal potion by replacing it with empty bottle
                        player.getInventory().removeItem(it);
                        player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));
                    } else {
                        it.getEnchantments().clear(); // Remove all enchantments from potions
                    }
                } else {
                    removeUnsafeEnchantments(it);
                }
            }
        }
    }

    private boolean isVanillaPotion(ItemStack it) {
        PotionMeta meta = (PotionMeta) it.getItemMeta();
        PotionData data = meta.getBasePotionData();
        PotionType type = data.getType();
        return type == PotionType.AWKWARD || type == PotionType.MUNDANE || type == PotionType.THICK || type == PotionType.WATER;
    }

    private void removeUnsafeEnchantments(ItemStack it) {
        if (it != null && it.getType() != Material.AIR) {
            for (Map.Entry<Enchantment, Integer> entry : it.getEnchantments().entrySet()) {
                Enchantment e = entry.getKey();
                int level = entry.getValue();
                if (level > e.getMaxLevel() || !e.getItemTarget().includes(it.getType())) {
                    it.removeEnchantment(e);
                }
            }
        }
    }
}

// Feeling really unsafe

// MORE UNSAFE