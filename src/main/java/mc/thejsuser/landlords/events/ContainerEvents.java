package mc.thejsuser.landlords.events;

import mc.thejsuser.landlords.regionElements.Abilities;
import mc.thejsuser.landlords.regionElements.Region;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;

public class ContainerEvents implements Listener {

    //FIELDS
    private static HashMap<Material,Abilities> materialAbilityDictionary_ = null;

    //EVENTS
    @EventHandler
    public void onOpenedContainer(PlayerInteractEvent e){

        Block block = e.getClickedBlock();
        if(block != null) {
            Material material = block.getType();
            if (getMaterialAbilityDictionary().containsKey(material)) {
                Abilities ablt = getMaterialAbilityDictionary().get(material);

                boolean a = Region.checkAbilityAtPoint(
                        e.getPlayer(),
                        ablt,
                        block.getLocation().add(.5,.5,.5));
                e.setCancelled(!a);
            }
        }
    }

    //PRIVATE METHODS
    private static HashMap<Material,Abilities> getMaterialAbilityDictionary(){

        if(materialAbilityDictionary_ == null){
            HashMap<Material,Abilities> d = new HashMap<>();
            d.put(Material.CHEST, Abilities.can_open_chests);
            d.put(Material.BARREL, Abilities.can_open_barrels);
            d.put(Material.FURNACE, Abilities.can_access_furnaces);
            d.put(Material.BLAST_FURNACE, Abilities.can_access_blast_furnaces);
            d.put(Material.SMOKER, Abilities.can_access_smokers);
            d.put(Material.CRAFTING_TABLE, Abilities.can_access_crafting_tables);
            d.put(Material.CARTOGRAPHY_TABLE, Abilities.can_access_cartography_tables);
            d.put(Material.SMITHING_TABLE,Abilities.can_access_smithing_tables);
            d.put(Material.ENCHANTING_TABLE,Abilities.can_access_enchanting_tables);
            d.put(Material.FLETCHING_TABLE,Abilities.can_access_fletching_tables);
            d.put(Material.STONECUTTER,Abilities.can_access_stonecutters);
            d.put(Material.ANVIL,Abilities.can_access_anvils);
            d.put(Material.GRINDSTONE,Abilities.can_access_grindstones);
            d.put(Material.BREWING_STAND,Abilities.can_access_brewing_stands);
            d.put(Material.LOOM, Abilities.can_access_looms);

            materialAbilityDictionary_ = d;
        }

        return materialAbilityDictionary_;

    }

}
