package me.mastergabemod.unsafeenchantfix;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

import java.util.List;
import java.util.Map;

public class Main extends JavaPlugin implements Runnable, Listener {

    private static Main instance;
    private FileConfiguration config;
    private long taskDelayTicks;
    private long taskPeriodTicks;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        config = getConfig();
        taskDelayTicks = config.getLong("task_settings.task_delay_seconds", 5) * 20;
        taskPeriodTicks = config.getLong("task_settings.task_period_seconds", 5) * 20;

        getServer().getScheduler().runTaskTimerAsynchronously(this, this, taskDelayTicks, taskPeriodTicks);
        getCommand("unsafecheck").setExecutor(new UnsafeCheckCommand());
        getServer().getPluginManager().registerEvents(this, this);
    }

    public static Main getInstance() {
        return instance;
    }

    @Override
    public void run() {
        if (config.getBoolean("checks.enable_checks")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (config.getBoolean("checks.check_inventory")) {
                    checkInventory(player);
                }
            }
        }
    }

    private void checkInventory(Player player) {
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null) {
                checkAndRemoveUnsafeItem(player, item);
            }
        }
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null) {
                checkAndRemoveUnsafeItem(player, item);
            }
        }
    }

    private void checkAndRemoveUnsafeItem(Player player, ItemStack item) {
        boolean itemChanged = false;

        if (item.getType() == Material.POTION || item.getType() == Material.SPLASH_POTION || item.getType() == Material.LINGERING_POTION) {
            if (config.getBoolean("checks.check_potions")) {
                if (!isVanillaPotion(item) || hasUnsafeEffects(item)) {
                    logSuspiciousItem(player, item, "Unsafe potion detected and removed.");
                    player.getInventory().remove(item);
                    player.getInventory().addItem(new ItemStack(Material.GLASS_BOTTLE));
                    itemChanged = true;
                }
            }
        } else if (config.getBoolean("checks.clear_unsafe_enchantments")) {
            Map<Enchantment, Integer> enchantments = item.getEnchantments();
            for (Enchantment enchantment : enchantments.keySet()) {
                int level = enchantments.get(enchantment);
                if (level > enchantment.getMaxLevel() || !enchantment.canEnchantItem(item)) {
                    item.removeEnchantment(enchantment);
                    itemChanged = true;
                }
            }
        }

        if (itemChanged) {
            player.updateInventory();
        }
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

    
    private boolean hasUnsafeEffects(ItemStack item) {
        if (!(item.getItemMeta() instanceof PotionMeta)) {
            return true;
        }

        PotionMeta meta = (PotionMeta) item.getItemMeta();
        for (PotionEffect effect : meta.getCustomEffects()) {
            if (!isSafeEffect(effect)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSafeEffect(PotionEffect effect) {
        List<String> allowedEffects = config.getStringList("checks.potion_checks.allowed_effects");
        int maxAmplifier = config.getInt("checks.potion_checks.max_effect_amplifier", 3);
        int maxDuration = config.getInt("checks.potion_checks.max_effect_duration_ticks", 6000);

        return allowedEffects.contains(effect.getType().getName()) &&
               effect.getAmplifier() <= maxAmplifier &&
               effect.getDuration() <= maxDuration;
    }


    private void logSuspiciousItem(Player player, ItemStack item, String reason) {
        getLogger().warning("Player " + player.getName() + " had an item removed: " + item.toString() + ". Reason: " + reason);
    }

    public boolean isVanillaPotion(ItemStack item) {
        if (!(item.getItemMeta() instanceof PotionMeta)) {
            return false;
        }
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        PotionData data = meta.getBasePotionData();
        PotionType type = data.getType();
        return type == PotionType.AWKWARD || type == PotionType.MUNDANE || type == PotionType.THICK || type == PotionType.WATER;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (config.getBoolean("checks.check_inventory") && event.getPlayer() instanceof Player) {
            Inventory inventory = event.getInventory();
            for (ItemStack item : inventory.getContents()) {
                if (item != null) {
                    checkAndRemoveUnsafeItem((Player) event.getPlayer(), item);
                }
            }
        }
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        if (config.getBoolean("checks.check_potions")) {
            ThrownPotion potion = event.getPotion();
            if (potion.getItem() != null && hasUnsafeEffects(potion.getItem())) {
                event.setCancelled(true);
                logSuspiciousItem((Player) potion.getShooter(), potion.getItem(), "Unsafe potion effect.");
            }
        }
    }

    @EventHandler
    public void onLingeringPotion(LingeringPotionSplashEvent event) {
        if (config.getBoolean("checks.check_potions")) {
            ThrownPotion potion = event.getEntity();
            if (potion.getItem() != null && hasUnsafeEffects(potion.getItem())) {
                event.setCancelled(true);
                logSuspiciousItem((Player) potion.getShooter(), potion.getItem(), "Unsafe lingering potion.");
            }
        }
    }

    public void reloadConfig() {
        super.reloadConfig();
        config = getConfig();
        taskDelayTicks = config.getLong("task_settings.task_delay_seconds", 5) * 20;
        taskPeriodTicks = config.getLong("task_settings.task_period_seconds", 5) * 20;
    }
}