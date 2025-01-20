package me.mastergabemod.unsafeenchantfix;

import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class UnsafeCheckCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (player.hasPermission("unsafecheck.admin.reload")) {
                Main.getInstance().reloadConfig();
                player.sendMessage(ChatColor.GREEN + "Configuration reloaded successfully.");
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "You do not have permission to reload the configuration.");
                return true;
            }
        }

        if (!player.hasPermission("unsafecheck.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand != null && itemInHand.getType() != Material.AIR) {
            boolean isSafe = checkItemSafety(itemInHand);

            if (isSafe) {
                player.sendMessage(ChatColor.GREEN + "Item checked successfully. Item appears to be safe.");
            } else {
                clearUnsafeEnchantments(itemInHand);
                clearItemMetadata(itemInHand);
                player.sendMessage(ChatColor.RED + "Item checked. Item wasn't safe. Enchantments, metadata, lore, and display name cleared.");
            }
        } else {
            player.sendMessage(ChatColor.RED + "You must hold an item in your hand to check.");
        }

        return true;
    }

    private boolean checkItemSafety(ItemStack item) {
        if (item.getType() == Material.POTION) {
            return Main.getInstance().isVanillaPotion(item);
        } else {
            return !hasUnsafeEnchantments(item);
        }
    }

    private boolean hasUnsafeEnchantments(ItemStack item) {
        for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
            Enchantment enchantment = entry.getKey();
            int level = entry.getValue();
            if (level > enchantment.getMaxLevel() || !enchantment.canEnchantItem(item)) {
                return true;
            }
        }
        return false;
    }

    private void clearUnsafeEnchantments(ItemStack item) {
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

    private void clearItemMetadata(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(null);
            meta.setLore(null);
            item.setItemMeta(meta);
        }
    }
}
