package me.mastergabemod.unsafeenchantfix;

import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;

import java.util.Arrays;
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

        if (item.getType() == Material.FIREWORK_ROCKET) {
            if (config.getBoolean("checks.check_fireworks") && isUnobtainableFirework(item)) {
                logSuspiciousItem(player, item, "Illegal firework detected and removed.");
                player.getInventory().remove(item);
                itemChanged = true;
            }
        }

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
        } else if (item.getType().toString().contains("SIGN")) {
            if (config.getBoolean("checks.check_crash_signs") && isCrashSign(item)) {
                logSuspiciousItem(player, item, "Crash sign detected and removed.");
                player.getInventory().remove(item);
                itemChanged = true;
            }
        } else if (item.getType().toString().contains("BOOK")) {
            if (config.getBoolean("checks.check_crash_books") && isCrashBook(item)) {
                logSuspiciousItem(player, item, "Crash book detected and removed.");
                player.getInventory().remove(item);
                itemChanged = true;
            }
        } else if (isCrashContainer(item)) {
            if (config.getBoolean("checks.check_crash_chests")) {
                logSuspiciousItem(player, item, "Crash chest or shulker box detected and removed.");
                player.getInventory().remove(item);
                itemChanged = true;
            }
        }

        if (itemChanged) {
            player.updateInventory();
        }
    }
    
    private boolean isCrashContainer(ItemStack item) {
        if (!(item.getItemMeta() instanceof BlockStateMeta)) return false;

        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        if (!(meta.getBlockState() instanceof Container)) return false;

        Container container = (Container) meta.getBlockState();
        Inventory inventory = container.getInventory();

        int maxItemStackSize = config.getInt("checks.container_checks.max_item_stack_size", 64);
        for (ItemStack content : inventory.getContents()) {
            if (content != null) {
                if (content.getAmount() > maxItemStackSize || hasUnsafeNBT(content)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean hasUnsafeNBT(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
            int modelData = item.getItemMeta().getCustomModelData();
            int maxModelData = config.getInt("checks.container_checks.max_custom_model_data", 1000);
            if (modelData > maxModelData) {
                return true;
            }
        }
        return false;
    }

    private boolean isUnobtainableFirework(ItemStack item) {
        if (item.getType() != Material.FIREWORK_ROCKET) return false;

        FireworkMeta meta = (FireworkMeta) item.getItemMeta();
        if (meta == null) return false;

        int maxFlightDuration = config.getInt("checks.firework_checks.max_flight_duration", 3);
        int flightDuration = meta.getPower();
        if (flightDuration < 1 || flightDuration > maxFlightDuration) {
            return true; 
        }

        List<FireworkEffect> effects = meta.getEffects();
        List<String> allowedEffects = config.getStringList("checks.firework_checks.allowed_effects");
        for (FireworkEffect effect : effects) {
            if (!allowedEffects.contains(effect.getType().name())) {
                return true; 
            }
        }

        return false;
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

    private boolean isCrashSign(ItemStack item) {
        if (!(item.getItemMeta() instanceof BlockStateMeta)) return false;

        BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
        if (!(meta.getBlockState() instanceof Sign)) return false;

        Sign sign = (Sign) meta.getBlockState();
        for (String line : sign.getLines()) {
            if (line.length() > config.getInt("checks.sign_checks.max_line_length", 100) ||
                line.matches(config.getString("checks.sign_checks.invalid_pattern", "[^\\x20-\\x7E]"))) {
                return true;
            }
        }
        return false;
    }

    private boolean isCrashBook(ItemStack item) {
        if (!(item.getItemMeta() instanceof BookMeta)) return false;

        BookMeta meta = (BookMeta) item.getItemMeta();
        if (meta.getPageCount() > config.getInt("checks.book_checks.max_pages", 50)) {
            return true;
        }

        for (String page : meta.getPages()) {
            if (page.length() > config.getInt("checks.book_checks.max_page_length", 2560) ||
                page.matches(config.getString("checks.book_checks.invalid_pattern", "[^\\x20-\\x7E]"))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasUnsafeEffects(ItemStack item) {
        if (!(item.getItemMeta() instanceof PotionMeta)) {
            return true;
        }
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        if (!meta.hasCustomEffects()) {
            return false;
        }

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
        return Arrays.asList(PotionType.values()).contains(data.getType());
    }


    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (config.getBoolean("checks.check_crash_signs") && event.getBlock().getType().toString().contains("SIGN")) {
            ItemStack item = event.getItemInHand();
            if (isCrashSign(item)) {
                event.setCancelled(true);
                logSuspiciousItem(event.getPlayer(), item, "Attempted to place a crash sign.");
            }
        }
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
        if (config.getBoolean("checks.check_crash_chests") && event.getInventory().getType() == InventoryType.CHEST) {
            Inventory inventory = event.getInventory();
            for (ItemStack item : inventory.getContents()) {
                if (item != null && isCrashContainer(item)) {
                    event.setCancelled(true);
                    logSuspiciousItem((Player) event.getPlayer(), item, "Illegal chest or shulker box detected in inventory.");
                    break;
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