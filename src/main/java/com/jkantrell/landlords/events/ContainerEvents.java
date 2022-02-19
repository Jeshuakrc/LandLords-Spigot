package com.jkantrell.landlords.events;

import com.jkantrell.landlords.Landlords;
import com.jkantrell.landlords.oldRegions.ablt_;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;

public class ContainerEvents implements Listener {

    //FIELDS
    private static HashMap<Material, ablt_> materialAbilityDictionary_ = null;

    //EVENTS
    @EventHandler
    public void onOpenedContainer(PlayerInteractEvent e){

        Block block = e.getClickedBlock();
        if(block != null) {
            Material material = block.getType();
            if (getMaterialAbilityDictionary().containsKey(material)) {
                ablt_ ablt = getMaterialAbilityDictionary().get(material);

                Landlords.Utils.handleEvent(e,e.getPlayer(), block.getLocation().add(.5,.5,.5),ablt);
            }
        }
    }

    //PRIVATE METHODS
    private static HashMap<Material, ablt_> getMaterialAbilityDictionary(){

        if(materialAbilityDictionary_ == null){
            HashMap<Material, ablt_> d = new HashMap<>();
            d.put(Material.CHEST, ablt_.can_open_chests);
            d.put(Material.BARREL, ablt_.can_open_barrels);
            d.put(Material.FURNACE, ablt_.can_access_furnaces);
            d.put(Material.BLAST_FURNACE, ablt_.can_access_blast_furnaces);
            d.put(Material.SMOKER, ablt_.can_access_smokers);
            d.put(Material.CRAFTING_TABLE, ablt_.can_access_crafting_tables);
            d.put(Material.CARTOGRAPHY_TABLE, ablt_.can_access_cartography_tables);
            d.put(Material.SMITHING_TABLE, ablt_.can_access_smithing_tables);
            d.put(Material.ENCHANTING_TABLE, ablt_.can_access_enchanting_tables);
            d.put(Material.FLETCHING_TABLE, ablt_.can_access_fletching_tables);
            d.put(Material.STONECUTTER, ablt_.can_access_stonecutters);
            d.put(Material.ANVIL, ablt_.can_access_anvils);
            d.put(Material.GRINDSTONE, ablt_.can_access_grindstones);
            d.put(Material.BREWING_STAND, ablt_.can_access_brewing_stands);
            d.put(Material.LOOM, ablt_.can_access_looms);

            materialAbilityDictionary_ = d;
        }

        return materialAbilityDictionary_;

    }

}
