package me.mastergabemod.unsafeenchantfix;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class main
    implements Runnable
{
	

    public main()
    {
    }

    public void run()
    {
        Player aplayer[];
        @SuppressWarnings("deprecation")
		int j = (aplayer = Bukkit.getOnlinePlayers()).length;
        for(int i = 0; i < j; i++)
        {
            Player player = aplayer[i];
            ItemStack aitemstack[];
            int i1 = (aitemstack = player.getInventory().getArmorContents()).length;
            for(int k = 0; k < i1; k++)
            {
                ItemStack it = aitemstack[k];
                if(it != null && it.getEnchantments().size() > 0)
                {
                    Enchantment e;
                    for(Iterator<Enchantment> iterator = getUnsafeEnchantments(it).iterator(); iterator.hasNext(); it.removeEnchantment(e))
                        e = (Enchantment)iterator.next();

                }
            }

            i1 = (aitemstack = player.getInventory().getContents()).length;
            for(int l = 0; l < i1; l++)
            {
                ItemStack it = aitemstack[l];
                if(it != null && it.getEnchantments().size() > 0)
                {
                    Enchantment e;
                    for(Iterator<Enchantment> iterator1 = getUnsafeEnchantments(it).iterator(); iterator1.hasNext(); it.removeEnchantment(e))
                        e = (Enchantment)iterator1.next();

                }
            }

        }

    }

    private List<Enchantment> getUnsafeEnchantments(ItemStack it)
    {
        List<Enchantment> list = new ArrayList<Enchantment>();
        if(it != null && it.getType() != Material.AIR)
        {
            for(Iterator<?> iterator = it.getEnchantments().entrySet().iterator(); iterator.hasNext();)
            {
                @SuppressWarnings("rawtypes")
				java.util.Map.Entry entry = (java.util.Map.Entry)iterator.next();
                Enchantment e = (Enchantment)entry.getKey();
                int level = ((Integer)entry.getValue()).intValue();
                if(level > 5)
                	
                    list.add(e);
          
            }

        }
        return list;
    }
}
