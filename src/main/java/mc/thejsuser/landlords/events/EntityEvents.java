package mc.thejsuser.landlords.events;

import mc.thejsuser.landlords.Landlords;
import mc.thejsuser.landlords.regions.Ability;
import mc.thejsuser.landlords.regions.Region;
import mc.thejsuser.landlords.regions.Rule;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class EntityEvents implements Listener {

    @EventHandler
    public void onHangingEntityBreak(HangingBreakByEntityEvent e) {

        Hanging entity = e.getEntity();
        Entity remover = e.getRemover();
        Ability ablt = null;
        Player player = null;
        if (remover instanceof Player player_) {
            player = player_;
        }
        if (remover instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player player_) {
                player = player_;
            }
        }

         switch (entity.getType()){
             case ITEM_FRAME -> ablt= Ability.can_break_item_frames;
             case GLOW_ITEM_FRAME -> ablt= Ability.can_break_glow_item_frames;
             case PAINTING -> ablt= Ability.can_break_paintings;

             default -> {}
         }
         if (ablt != null) {
             Landlords.Utils.handleEvent(e,player,entity.getLocation(),ablt);
         }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent e) {

        Entity damager = e.getDamager();
        Player player = null;
        if (damager instanceof Player player_) {
            player = player_;
        }
        if (damager instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player player_) {
                player = player_;
            }
        }

        Entity entity = e.getEntity();
        Ability ablt = null;
        switch (entity.getType()) {
            case ARMOR_STAND -> ablt = Ability.can_break_armor_stands;
            case ITEM_FRAME -> ablt = Ability.can_take_from_item_frames;
            case GLOW_ITEM_FRAME -> ablt = Ability.can_take_from_glow_item_frames;

            default -> {
                if (entity instanceof Animals) {
                    ablt = Ability.can_damage_animals;
                }
                if (entity instanceof Monster) {
                    ablt = Ability.can_damage_monsters;
                }
            }
        }
        if (ablt != null) {
            Landlords.Utils.handleEvent(e,player,entity.getLocation(),ablt);
        }

    }

    @EventHandler
    public void onEntityInteraction(PlayerInteractEntityEvent e) {

        Entity entity = e.getRightClicked();
        Ability ablt = null;

        switch (entity.getType()) {
            case ITEM_FRAME ->  ablt = Ability.can_interact_with_item_frames;
            case GLOW_ITEM_FRAME -> ablt = Ability.can_interact_with_glow_item_frames;
            default -> {
                if (entity instanceof Animals) {
                    ablt = Ability.can_interact_with_animals;
                }
            }
        }
        if (ablt != null) {
            Landlords.Utils.handleEvent(e,e.getPlayer(),entity.getLocation(),ablt);
        }
    }

    @EventHandler
    public void onArmorStandInteraction(PlayerInteractAtEntityEvent e){

        Entity entity = e.getRightClicked();
        Ability ablt = switch (entity.getType()) {
            case ARMOR_STAND -> Ability.can_interact_with_armor_stands;
            default -> null;
        };
        if(ablt != null) {
            Landlords.Utils.handleEvent(e,e.getPlayer(),entity.getLocation(),ablt);
        }
    }

    @EventHandler
    public void onCreeperExplode(EntityExplodeEvent e){
        e.blockList().removeIf(block -> {
            Entity entity = e.getEntity();
            for (Region region : Region.getAllAt(block.getLocation().add(.5,.5,.5))) {
                if (entity instanceof Creeper && region.hasRule("creeperProtected")) {
                    return region.getRuleValue("creeperProtected", Rule.DataType.BOOL);
                }
                if (entity instanceof TNTPrimed tnt && region.hasRule("tntProtected")) {
                    switch (region.getRuleValue("tntProtected", Landlords.RegionRules.TNT_PROTECTED_DT)) {
                        case all -> { return true; }
                        case ignitor -> {
                            if (tnt.getSource() != null) {
                                if (tnt.getSource() instanceof Player player) {
                                    return !region.checkAbility(player,Ability.can_ignite_tnt);
                                }
                            }
                            return true;
                        }
                        default -> { return false; }
                    }
                }
            }
            return false;
        });
    }
}
