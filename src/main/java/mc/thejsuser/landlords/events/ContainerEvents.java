package mc.thejsuser.landlords.events;

import mc.thejsuser.landlords.Landlords;
import mc.thejsuser.landlords.regions.Ability;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;

public class ContainerEvents implements Listener {

    //FIELDS
    private static HashMap<Material, Ability> materialAbilityDictionary_ = null;

    //EVENTS
    @EventHandler
    public void onOpenedContainer(PlayerInteractEvent e){

        Block block = e.getClickedBlock();
        if(block != null) {
            Material material = block.getType();
            if (getMaterialAbilityDictionary().containsKey(material)) {
                Ability ablt = getMaterialAbilityDictionary().get(material);

                Landlords.Utils.handleEvent(e,e.getPlayer(), block.getLocation().add(.5,.5,.5),ablt);
            }
        }
    }

    //PRIVATE METHODS
    private static HashMap<Material, Ability> getMaterialAbilityDictionary(){

        if(materialAbilityDictionary_ == null){
            HashMap<Material, Ability> d = new HashMap<>();
            d.put(Material.CHEST, Ability.can_open_chests);
            d.put(Material.BARREL, Ability.can_open_barrels);
            d.put(Material.FURNACE, Ability.can_access_furnaces);
            d.put(Material.BLAST_FURNACE, Ability.can_access_blast_furnaces);
            d.put(Material.SMOKER, Ability.can_access_smokers);
            d.put(Material.CRAFTING_TABLE, Ability.can_access_crafting_tables);
            d.put(Material.CARTOGRAPHY_TABLE, Ability.can_access_cartography_tables);
            d.put(Material.SMITHING_TABLE, Ability.can_access_smithing_tables);
            d.put(Material.ENCHANTING_TABLE, Ability.can_access_enchanting_tables);
            d.put(Material.FLETCHING_TABLE, Ability.can_access_fletching_tables);
            d.put(Material.STONECUTTER, Ability.can_access_stonecutters);
            d.put(Material.ANVIL, Ability.can_access_anvils);
            d.put(Material.GRINDSTONE, Ability.can_access_grindstones);
            d.put(Material.BREWING_STAND, Ability.can_access_brewing_stands);
            d.put(Material.LOOM, Ability.can_access_looms);

            materialAbilityDictionary_ = d;
        }

        return materialAbilityDictionary_;

    }

}
