package mc.thejsuser.landlords.events;

import mc.thejsuser.landlords.Landlords;
import mc.thejsuser.landlords.regionElements.Abilities;
import mc.thejsuser.landlords.regionElements.Region;
import mc.thejsuser.landlords.io.ConfigManager;
import mc.thejsuser.landlords.totemElements.TotemLectern;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.inventory.ItemStack;

import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class BlockEvents implements Listener {

    private static HashMap<Material,Abilities> doorsMaterialAbilityDictionary_ = null;
    private static List<Material> beds_ = null;

    //EVENTS
    @EventHandler
    public void onBreakBlock(BlockBreakEvent e){

        Block block = e.getBlock();
        Material material = block.getType();
        Abilities ablt = Abilities.can_break_blocks;

        if (block.getType() == Material.FIRE) {
            ablt = Abilities.can_extinguish_fire;
        } else {
            if (ConfigManager.getPlantableBlocks().contains(material)) {
                ablt = Abilities.can_break_crops;
            }

            if (ConfigManager.getBreakableRedstoneBlocks().contains(material)) {
                ablt = Abilities.can_break_redstone;
            }
        }

        Landlords.Utils.handleEvent(e,e.getPlayer(),block.getLocation().add(.5,.5,.5),ablt);
    }

    @EventHandler
    public void onPlaceBlock(BlockPlaceEvent e){

        Block block = e.getBlock();
        Material material = block.getType();
        Abilities ablt = Abilities.can_place_blocks;

        if (block.getType() == Material.FIRE) {
            ablt = Abilities.can_place_fire;
        } else {
            if (ConfigManager.getPlantableBlocks().contains(material)) {
                ablt = Abilities.can_plant;
            }

            if (ConfigManager.getBreakableRedstoneBlocks().contains(material)) {
                ablt = Abilities.can_place_redstone;
            }
        }

        Landlords.Utils.handleEvent(e,e.getPlayer(),block.getLocation().add(.5,.5,.5),ablt);
    }

    @EventHandler
    public void onBlockInteraction(PlayerInteractEvent e){

        Block block = e.getClickedBlock();
        ItemStack item = e.getItem();
        Abilities ablt = null;
        if (item != null) {
            if (item.getType().equals(Material.BUCKET)) {
                List<Block> lineOfSight = e.getPlayer().getLineOfSight(null, 10);
                List<Material> lineOfSightMaterials = new java.util.ArrayList<>(Collections.emptyList());
                block = lineOfSight.get(lineOfSight.size() - 1);
                for (Block b : lineOfSight) {
                    lineOfSightMaterials.add(b.getType());
                }
                if (lineOfSightMaterials.contains(Material.LAVA)) {
                    ablt = Abilities.can_take_lava;
                }
                if (lineOfSightMaterials.contains(Material.WATER)) {

                    ablt = Abilities.can_take_water;
                    int i = 0;
                    BlockFace[] faces = {BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH};
                    for (BlockFace f : faces) {
                        Block b = block.getRelative(f);
                        if (b.getType().equals(Material.WATER)) {
                            Levelled level = (Levelled) b.getBlockData();
                            if (level.getLevel() == 0) {
                                i++;
                            }
                        }
                    }
                    if (i >= 2) {
                        ablt = Abilities.can_take_infinite_water;
                    }
                }
            }
        }
        if (block != null) {

            Action action = e.getAction();
            if (action.equals(Action.RIGHT_CLICK_BLOCK)) {

                Material material = block.getType();
                switch (material) {
                    case JUKEBOX -> ablt = Abilities.can_click_jukeboxes;
                    case NOTE_BLOCK -> ablt = Abilities.can_click_note_blocks;
                    case RESPAWN_ANCHOR -> ablt = Abilities.can_use_respawn_anchors;
                    case BELL -> ablt = Abilities.can_ring_bells;
                    case LECTERN -> {

                        StringBuilder abltString = new StringBuilder();
                        abltString.append("can_");

                        if (item == null) {
                            abltString.append("access_");
                        } else {
                            if (item.getType().equals(Material.WRITTEN_BOOK) || item.getType().equals(Material.WRITABLE_BOOK)) {
                                abltString.append("place_books_on_");
                            } else {
                                abltString.append("access_");
                            }
                        }
                        if (TotemLectern.isTotemLectern(block)) { abltString.append("totem_"); }
                        abltString.append("lecterns");
                        ablt = Abilities.valueOf(abltString.toString());
                    }
                    default -> {
                        if (getDoorsMaterialAbilityDictionary_().containsKey(material)) {
                            ablt = getDoorsMaterialAbilityDictionary_().get(material);
                        }
                        if (block.getType().equals(Material.TNT)) {
                            ablt = Abilities.can_ignite_tnt;
                        }
                        if (getBeds_().contains(material)) {
                            ablt = Abilities.can_use_beds;
                        }
                    }
                }
                if (item != null) {
                    ablt = switch (item.getType()) {
                        case WATER_BUCKET -> Abilities.can_put_water;
                        case LAVA_BUCKET -> Abilities.can_put_lava;
                        case ITEM_FRAME -> Abilities.can_place_item_frames;
                        case GLOW_ITEM_FRAME -> Abilities.can_place_glow_item_frames;
                        case PAINTING -> Abilities.can_place_paintings;
                        case ARMOR_STAND -> Abilities.can_place_armor_stands;
                        default -> ablt;
                    };
                }
            }
        }

        if (ablt != null && block != null) {
            boolean a = Landlords.Utils.handleEvent(e,e.getPlayer(),block.getLocation().add(.5,.5,.5),ablt);
            if (a && ablt == Abilities.can_ring_bells) {
                /*
                Region[] regions = Region.getFromPoint(block.getX() + .5, block.getY() + .5, block.getZ() + .5, block.getWorld().getEnvironment());
                for (Region region : regions) {
                    region.displayBoundaries(ConfigManager.getRegionBorderRefreshRate(),ConfigManager.getRegionBorderPersistenceBell());
                }
                 */
            }
        }
    }

    @EventHandler
    public void onPlayerTakeBookFromLectern(PlayerTakeLecternBookEvent e) {
        Player player = e.getPlayer();
        Block lectern = e.getLectern().getBlock();

        Abilities ablt = TotemLectern.isTotemLectern(lectern) ? Abilities.can_take_books_from_totem_lecterns : Abilities.can_take_books_from_lecterns;

        Landlords.Utils.handleEvent(e,e.getPlayer(),lectern.getLocation().add(.5,.5,.5),ablt);
    }

    //PRIVATE METHODS

    private static HashMap<Material,Abilities> getDoorsMaterialAbilityDictionary_(){

        if(doorsMaterialAbilityDictionary_ == null){
            HashMap<Material,Abilities> d = new HashMap<>();
            d.put(Material.DARK_OAK_DOOR,Abilities.can_open_doors);
            d.put(Material.ACACIA_DOOR,Abilities.can_open_doors);
            d.put(Material.BIRCH_DOOR,Abilities.can_open_doors);
            d.put(Material.IRON_DOOR,Abilities.can_open_doors);
            d.put(Material.OAK_DOOR,Abilities.can_open_doors);
            d.put(Material.CRIMSON_DOOR,Abilities.can_open_doors);
            d.put(Material.JUNGLE_DOOR,Abilities.can_open_doors);
            d.put(Material.SPRUCE_DOOR,Abilities.can_open_doors);
            d.put(Material.WARPED_DOOR,Abilities.can_open_doors);
            d.put(Material.ACACIA_TRAPDOOR,Abilities.can_open_trapdoors);
            d.put(Material.BIRCH_TRAPDOOR,Abilities.can_open_trapdoors);
            d.put(Material.CRIMSON_TRAPDOOR,Abilities.can_open_trapdoors);
            d.put(Material.IRON_TRAPDOOR,Abilities.can_open_trapdoors);
            d.put(Material.DARK_OAK_TRAPDOOR,Abilities.can_open_trapdoors);
            d.put(Material.JUNGLE_TRAPDOOR,Abilities.can_open_trapdoors);
            d.put(Material.OAK_TRAPDOOR,Abilities.can_open_trapdoors);
            d.put(Material.SPRUCE_TRAPDOOR,Abilities.can_open_trapdoors);
            d.put(Material.WARPED_TRAPDOOR,Abilities.can_open_trapdoors);
            d.put(Material.ACACIA_FENCE_GATE,Abilities.can_open_fence_gates);
            d.put(Material.BIRCH_FENCE_GATE,Abilities.can_open_fence_gates);
            d.put(Material.CRIMSON_FENCE_GATE,Abilities.can_open_fence_gates);
            d.put(Material.JUNGLE_FENCE_GATE,Abilities.can_open_fence_gates);
            d.put(Material.DARK_OAK_FENCE_GATE,Abilities.can_open_fence_gates);
            d.put(Material.OAK_FENCE_GATE,Abilities.can_open_fence_gates);
            d.put(Material.SPRUCE_FENCE_GATE,Abilities.can_open_fence_gates);
            d.put(Material.WARPED_FENCE_GATE,Abilities.can_open_fence_gates);

            doorsMaterialAbilityDictionary_ = d;
        }
        return doorsMaterialAbilityDictionary_;
    }
    private static List<Material> getBeds_(){

        if(beds_ == null){
            beds_ = new java.util.ArrayList<>(Collections.emptyList());
            for (Material i :Material.values()){
                if(i.name().contains("_BED")){
                    beds_.add(i);
                }
            }
        }
        return beds_;
    }
}
