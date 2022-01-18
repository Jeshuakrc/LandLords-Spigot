package mc.thejsuser.landlords.events;

import mc.thejsuser.landlords.Landlords;
import mc.thejsuser.landlords.regionElements.Abilities;
import mc.thejsuser.landlords.regionElements.Region;
import mc.thejsuser.landlords.io.ConfigManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Switch;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Collections;
import java.util.List;

public class RedstoneEvents implements Listener {

    private static List<Material> buttons_ = null;

    @EventHandler
    public void onButtonPress(PlayerInteractEvent e){

        Block block = e.getClickedBlock();
        Action action = e.getAction();
        if (block != null && action.equals(Action.RIGHT_CLICK_BLOCK)) {
            Material material = block.getType();
            if (getButtons_().contains(material)) {
                Abilities ablt = (ConfigManager.getEnforcedButtons().contains(material)) ?
                        Abilities.can_press_enforced_buttons :
                        Abilities.can_press_buttons;

                Landlords.Utils.handleEvent(e, e.getPlayer(), block.getLocation().add(.5,.5,.5), ablt);
            }
        }
    }

    @EventHandler
    public void onLeverPulled(PlayerInteractEvent e){

        Block block = e.getClickedBlock();
        Action action = e.getAction();
        if (block != null && action.equals(Action.RIGHT_CLICK_BLOCK)) {
            if (block.getType() == Material.LEVER) {
                boolean a;
                Abilities ablt = Abilities.can_pull_levers;
                Directional dir = (Directional) block.getBlockData();
                Switch sw = (Switch) block.getBlockData();

                BlockFace face = switch (sw.getFace()) {
                    case FLOOR -> BlockFace.DOWN;
                    case CEILING -> BlockFace.UP;
                    default -> switch (dir.getFacing()) {
                        case NORTH -> BlockFace.SOUTH;
                        case EAST -> BlockFace.WEST;
                        case SOUTH -> BlockFace.NORTH;
                        default ->  BlockFace.EAST;
                    };
                };

                Block connected = block.getRelative(face);
                if (ConfigManager.getLeverLockerBlocks().contains(connected.getType())) {
                    ablt = Abilities.can_pull_locked_levers;
                }

                Landlords.Utils.handleEvent(e,e.getPlayer(),block.getLocation().add(.5,.5,.5),ablt);
            }
        }
    }

    private static List<Material> getButtons_(){

        if(buttons_ == null){
            buttons_ = new java.util.ArrayList<>(Collections.emptyList());
            for (Material i :Material.values()){
                if(i.name().contains("BUTTON")){
                    buttons_.add(i);
                }
            }
        }
        return buttons_;
    }

}

