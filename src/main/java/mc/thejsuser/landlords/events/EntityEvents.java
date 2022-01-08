package mc.thejsuser.landlords.events;

import mc.thejsuser.landlords.regionElements.Abilities;
import mc.thejsuser.landlords.regionElements.Region;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreeperPowerEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.List;

public class EntityEvents implements Listener {

    @EventHandler
    public void onHangingEntityBreak(HangingBreakByEntityEvent e) {

         Hanging entity = e.getEntity();
        Abilities ablt = null;

         switch (entity.getType()){
             case ITEM_FRAME:
                 ablt=Abilities.can_break_item_frames;

             case PAINTING:
                 ablt=Abilities.can_break_paintings;

             default: break;
         }
         if (ablt != null) {
            boolean a = Region.checkPlayerAbilityAtPoint(
                    (Player) e.getRemover(),
                    ablt,
                    entity.getLocation().getX(),
                    entity.getLocation().getY(),
                    entity.getLocation().getZ()
            );
            e.setCancelled(!a);
         }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent e) {

        Entity damager = e.getDamager();
        if (damager instanceof Player) {

            Entity entity = e.getEntity();
            Abilities ablt = null;
            if (entity instanceof Animals) {
                ablt = Abilities.can_damage_animals;
            }
            if (entity instanceof Monster) {
                ablt = Abilities.can_damage_monsters;
            }
            if (entity.getType().equals(EntityType.ARMOR_STAND)) {
                ablt = Abilities.can_break_armor_stands;
            }
            if (ablt != null) {
                boolean a = Region.checkPlayerAbilityAtPoint(
                        (Player) damager,
                        ablt,
                        entity.getLocation().getX(),
                        entity.getLocation().getY(),
                        entity.getLocation().getZ()
                );
                e.setCancelled(!a);
            }
        }



    }

    @EventHandler
    public void onEntityInteraction(PlayerInteractEntityEvent e) {

        Entity entity = e.getRightClicked();
        Abilities ablt = null;

        if (entity.getType() == EntityType.ITEM_FRAME) {
            ablt = Abilities.can_interact_with_item_frames;
        } else {
            if (entity instanceof Animals) {
                ablt = Abilities.can_interact_with_animals;
            }
        }
        if (ablt != null) {
            boolean a = Region.checkPlayerAbilityAtPoint(
                    e.getPlayer(),
                    ablt,
                    entity.getLocation().getX(),
                    entity.getLocation().getY(),
                    entity.getLocation().getZ()
                    );
            e.setCancelled(!a);
        }
    }

    @EventHandler
    public void onArmorStandInteraction(PlayerInteractAtEntityEvent e){

        Entity entity = e.getRightClicked();
        if (entity.getType().equals(EntityType.ARMOR_STAND)) {
            boolean a = Region.checkPlayerAbilityAtPoint(
                    e.getPlayer(),
                    Abilities.can_interact_with_armor_stands,
                    entity.getLocation().getX(),
                    entity.getLocation().getY(),
                    entity.getLocation().getZ()
            );
            e.setCancelled(!a);
        }
    }

    @EventHandler
    public void onCreeperExplode(EntityExplodeEvent e){
        if(e.getEntity() instanceof Creeper) {
            List<Block> blocks = e.blockList();
            blocks.removeIf(block -> Region.getFromPoint(block.getLocation().add(.5, .5, .5)).length > 0);
        }
    }
}
