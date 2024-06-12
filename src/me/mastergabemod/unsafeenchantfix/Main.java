package me.mastergabemod.unsafeenchantfix;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.Map;

public class Main extends JavaPlugin implements Runnable, Listener {

    private static Main instance;

    @Override
    public void onEnable() {
        instance = this;
        getServer().getScheduler().runTaskTimerAsynchronously(this, this, 100L, 100L);
        getCommand("unsafecheck").setExecutor(new UnsafeCheckCommand());
        getServer().getPluginManager().registerEvents(this, this);
    }

    public static Main getInstance() {
        return instance;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkInventory(player);
        }
    }

    private void checkInventory(Player player) {
        for (ItemStack item : player.getInventory().getArmorContents()) {
            checkAndRemoveUnsafeItem(player, item);
        }
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                checkAndRemoveUnsafeItem(player, item);
            }
        }
    }

    private void checkAndRemoveUnsafeItem(Player player, ItemStack item) {
        if (item != null) {
            if (item.getType() == Material.POTION) {
                if (!isVanillaPotion(item)) {
                    player.getInventory().remove(item);
                    player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));
                } else {
                    clearUnsafeEnchantments(item);
                }
            } else {
                clearUnsafeEnchantments(item);
            }
        }
    }

    public boolean isVanillaPotion(ItemStack item) {
        if (!(item.getItemMeta() instanceof PotionMeta)) {
            return false;
        }

        PotionMeta meta = (PotionMeta) item.getItemMeta();
        PotionData data = meta.getBasePotionData();
        PotionType type = data.getType();

        if (!item.getEnchantments().isEmpty()) {
            return false;
        }

        return type == PotionType.AWKWARD || type == PotionType.MUNDANE || type == PotionType.THICK || type == PotionType.WATER;
    }

    public void clearUnsafeEnchantments(ItemStack item) {
        if (item != null && item.getType() != Material.AIR) {
            Map<Enchantment, Integer> enchantments = item.getEnchantments();
            for (Enchantment enchantment : enchantments.keySet()) {
                int level = enchantments.get(enchantment);
                if (level > enchantment.getMaxLevel() || !enchantment.canEnchantItem(item)) {
                    item.removeEnchantment(enchantment);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        Inventory inventory = event.getInventory();
        for (ItemStack item : inventory.getContents()) {
            if (item != null) {
                checkAndRemoveUnsafeItem((Player) event.getPlayer(), item);
            }
        }
    }
}