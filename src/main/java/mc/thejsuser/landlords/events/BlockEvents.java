package mc.thejsuser.landlords.events;

import mc.thejsuser.landlords.Landlords;
import mc.thejsuser.landlords.regions.Ability;
import mc.thejsuser.landlords.io.ConfigManager;
import mc.thejsuser.landlords.regions.Region;
import mc.thejsuser.landlords.regions.Rule;
import mc.thejsuser.landlords.totemElements.TotemLectern;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Collections;
import java.util.List;

public class BlockEvents implements Listener {

    //EVENTS
    @EventHandler
    public void onBreakBlock(BlockBreakEvent e){

        Block block = e.getBlock();
        Material material = block.getType();
        Ability ablt = Ability.can_break_blocks;

        if (block.getType() == Material.FIRE) {
            ablt = Ability.can_extinguish_fire;
        } else {
            if (ConfigManager.getPlantableBlocks().contains(material)) {
                ablt = Ability.can_break_crops;
            }

            if (ConfigManager.getBreakableRedstoneBlocks().contains(material)) {
                ablt = Ability.can_break_redstone;
            }
        }

        Landlords.Utils.handleEvent(e,e.getPlayer(),block.getLocation().add(.5,.5,.5),ablt);
    }

    @EventHandler
    public void onPlaceBlock(BlockPlaceEvent e){

        Block block = e.getBlock();
        Material material = block.getType();
        Ability ablt = Ability.can_place_blocks;

        if (block.getType() == Material.FIRE) {
            ablt = Ability.can_place_fire;
        } else if (ConfigManager.getPlantableBlocks().contains(material)) {
                ablt = Ability.can_plant;
        } else if (ConfigManager.getBreakableRedstoneBlocks().contains(material)) {
                ablt = Ability.can_place_redstone;
        } else if (
                block.getType().toString().contains("COPPER") &&
                e.getBlockReplacedState().getType().toString().contains("COPPER")
        ) {;
            return; // The onBlockInteraction event handler takes care of these scenarios
        }

        Landlords.Utils.handleEvent(e,e.getPlayer(),block.getLocation().add(.5,.5,.5),ablt);
    }

    @EventHandler
    public void onBlockInteraction(PlayerInteractEvent e){

        Block block = e.getClickedBlock();
        ItemStack item = e.getItem();
        Ability ablt = null;
        if (item != null) {
            //
        }
        if (block != null && e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {

            Material blockType = block.getType();
            switch (blockType) {
                case JUKEBOX -> ablt = Ability.can_click_jukeboxes;
                case NOTE_BLOCK -> ablt = Ability.can_click_note_blocks;
                case RESPAWN_ANCHOR -> ablt = Ability.can_use_respawn_anchors;
                case BELL -> ablt = Ability.can_ring_bells;
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
                    ablt = Ability.valueOf(abltString.toString());
                }
                default -> {
                    if (blockType.toString().contains("BED")) {
                        ablt = Ability.can_use_beds;
                    } else if (blockType.toString().contains("DOOR")) {
                        ablt = blockType.toString().contains("TRAPDOOR") ? Ability.can_open_doors : Ability.can_open_trapdoors;
                    } else if (blockType.toString().contains("FENCE_GATE")) {
                        ablt = Ability.can_open_fence_gates;
                    } else if (item != null) {
                        Material itemType = item.getType();
                        switch (itemType) {
                            case WATER_BUCKET -> ablt = Ability.can_put_water;
                            case LAVA_BUCKET -> ablt = Ability.can_put_lava;
                            case ITEM_FRAME -> ablt = Ability.can_place_item_frames;
                            case GLOW_ITEM_FRAME -> ablt = Ability.can_place_glow_item_frames;
                            case PAINTING -> ablt = Ability.can_place_paintings;
                            case ARMOR_STAND -> ablt = Ability.can_place_armor_stands;
                            case BUCKET ->  {
                                List<Block> lineOfSight = e.getPlayer().getLineOfSight(null, 10);
                                List<Material> lineOfSightMaterials = new java.util.ArrayList<>(Collections.emptyList());
                                for (Block b : lineOfSight) {
                                    lineOfSightMaterials.add(b.getType());
                                }
                                if (lineOfSightMaterials.contains(Material.LAVA)) {
                                    ablt = Ability.can_take_lava;
                                } else if (lineOfSightMaterials.contains(Material.WATER)) {

                                    ablt = Ability.can_take_water;
                                    int i = 0;
                                    BlockFace[] faces = {BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH};
                                    for (BlockFace f : faces) {
                                        Block b = block.getRelative(f);
                                        if (
                                                b.getType().equals(Material.WATER) &&
                                                ((Levelled) b.getBlockData()).getLevel() == 0
                                        ) { i++; }
                                    }
                                    if (i >= 2) {
                                        ablt = Ability.can_take_infinite_water;
                                    }
                                }
                            }
                            case FLINT_AND_STEEL -> {
                                if (blockType.equals(Material.TNT)) {
                                    ablt = Ability.can_ignite_tnt;
                                }
                            }
                            default -> {
                                if (blockType.toString().contains("COPPER")) {
                                    if (itemType.toString().contains("AXE")) {
                                        ablt = Ability.can_scrap_copper_blocks;
                                    } else if(itemType.equals(Material.HONEYCOMB)) {
                                        ablt = Ability.can_wax_copper_blocks;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (ablt != null && block != null) {
            boolean a = Landlords.Utils.handleEvent(e,e.getPlayer(),block.getLocation().add(.5,.5,.5),ablt);
            if (a && ablt == Ability.can_ring_bells) {
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

        Ability ablt = TotemLectern.isTotemLectern(lectern) ? Ability.can_take_books_from_totem_lecterns : Ability.can_take_books_from_lecterns;

        Landlords.Utils.handleEvent(e,e.getPlayer(),lectern.getLocation().add(.5,.5,.5),ablt);
    }

    @EventHandler
    public void onFireSpread(BlockBurnEvent e) {
        Region[] regions = Region.getRuleContainersAt("fireProtected",e.getBlock().getLocation().add(.5,.5,.5));
        for (Region r : regions) {
            if (r.getRuleValue("fireProtected",Rule.DataType.BOOL)) {
                e.setCancelled(true);
                return;
            }
        }
    }
}
