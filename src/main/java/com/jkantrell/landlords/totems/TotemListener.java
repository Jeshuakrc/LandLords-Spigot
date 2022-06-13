package com.jkantrell.landlords.totems;

import com.jkantrell.landlords.Landlords;
import com.jkantrell.landlords.event.TotemDestroyedByPlayerEvent;
import com.jkantrell.landlords.io.Config;
import com.jkantrell.landlords.io.LangManager;
import com.jkantrell.regionslib.RegionsLib;
import com.jkantrell.regionslib.events.RegionDestroyEvent;
import com.jkantrell.regionslib.regions.Hierarchy;
import com.jkantrell.regionslib.regions.Permission;
import com.jkantrell.regionslib.regions.Region;
import com.jkantrell.regionslib.regions.dataContainers.RegionData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Lectern;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.potion.PotionType;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

public class TotemListener implements Listener {

    //STATIC FIELDS
    private static HashMap<Player, EquipmentSlot> deedPlayerSlotMap_ = new HashMap<>();

    //EVENTS
    @EventHandler
    public void onPlayerInteraction(PlayerInteractEvent e) {

        ItemStack item = e.getItem();
        if (item == null) { return; }
        Player player = e.getPlayer();
        BlockFace blockFace = e.getBlockFace();
        Block block = e.getClickedBlock();
        Action action = e.getAction();
        TotemStructure structure;

        if (Deeds.isTotemDeeds(item) && action.equals(Action.RIGHT_CLICK_BLOCK)) {
            deedPlayerSlotMap_.put(player,e.getHand());
        } else {
            deedPlayerSlotMap_.remove(player);
        }

        if (block != null) {
            if (action == Action.RIGHT_CLICK_BLOCK) {
                if (
                        blockFace == BlockFace.UP &&
                        item.getType() == Material.END_CRYSTAL
                ) {
                    structure = TotemManager.chekStructuresFromPoint(block.getX(), block.getY() + 1, block.getZ(), player.getWorld());
                    boolean onValidBlock = block.getType().equals(Material.OBSIDIAN) || block.getType().equals(Material.BEDROCK);

                    switch (Landlords.CONFIG.endCrystalOnAnyBlock) {
                        case never:
                            if (!onValidBlock) {
                                break;
                            }

                        case on_totem:
                            if (structure == null) {
                                break;
                            }

                        case always:
                            if (structure == null && !onValidBlock) {
                                e.setCancelled(true);
                                Totem.placeEndCrystal(player, block.getX(), block.getY() + 1, block.getZ());
                            }
                            if (structure != null) {
                                e.setCancelled(true);
                                Totem totem = new Totem(block.getX(), block.getY() + 1, block.getZ(), player.getWorld(), structure);
                                totem.place(player);
                            }

                        default:
                    }
                }
                if (Deeds.isTotemDeeds(item) && TotemLectern.isTotemLectern(block)) {

                    TotemLectern lectern = TotemManager.getLecternAtSpot(block);
                    if (lectern == null ) { return; }

                    try {
                        Region region = lectern.getTotem().getRegion();
                        List<Permission>    oldPerms = new ArrayList<>(Arrays.asList(region.getPermissions().clone())),
                                newPerms;
                        String              oldName = region.getName(),
                                newName;
                        lectern.readDeeds(item,player);
                        newPerms = new ArrayList<>(Arrays.asList(region.getPermissions()));
                        newName = region.getName();

                        if (oldPerms.equals(newPerms) && oldName.equals(newName)) { return; }

                        HashMap<Player,List<String>> playerMessagesMap = new HashMap<>();
                        List<Player> msgRecipients = new ArrayList<>(Landlords.getMainInstance().getServer().getOnlinePlayers());
                        for (Player p : msgRecipients) {
                            playerMessagesMap.put(p,new ArrayList<>());
                        }

                        List<String> messages;
                        StringBuilder messagePath = new StringBuilder();
                        if(!oldName.equals(newName)){
                            for (Player p : msgRecipients) {
                                messages = playerMessagesMap.get(p);
                                messagePath.setLength(0);
                                messagePath.append("region_name_update_");
                                messagePath.append(p.equals(player) ? "firstPerson" : "thirdPerson");

                                messages.add(LangManager.getString(messagePath.toString(),p,oldName,newName,player.getName()));
                            }
                        }

                        HashMap<String,List<Hierarchy.Group>> playerPermissionsMap = new HashMap<>();
                        for (Permission oldPerm : oldPerms) {
                            String p = oldPerm.getPlayerName();
                            if (!playerPermissionsMap.containsKey(p)) {
                                playerPermissionsMap.put(p,new ArrayList<>());
                            }
                            playerPermissionsMap.get(p).add(oldPerm.getGroup());
                        }

                        for (Permission perm : newPerms) {

                            String affected = perm.getPlayerName();
                            List<Hierarchy.Group> groups = playerPermissionsMap.getOrDefault(affected,Collections.emptyList());
                            if(groups.isEmpty()){
                                for (Player p : msgRecipients) {
                                    messages = playerMessagesMap.get(p);
                                    messagePath.setLength(0);
                                    messagePath.append("region_permission_add_");
                                    messagePath.append(p.equals(player) ? "firstPerson" : "thirdPerson").append("_");
                                    messagePath.append(p.getName().equals(affected) ? "firstPerson" : "thirdPerson");

                                    messages.add(LangManager.getString(messagePath.toString(),p,affected,player.getName(),perm.getGroup().getName(),region.getName()));
                                }
                            } else{
                                Hierarchy.Group group = groups.get(0);
                                if (!groups.contains(perm.getGroup())) {
                                    for (Player p : msgRecipients) {
                                        messages = playerMessagesMap.get(p);
                                        messagePath.setLength(0);
                                        messagePath.append("region_permission_change_");
                                        messagePath.append(p.equals(player) ? "firstPerson" : "thirdPerson").append("_");
                                        messagePath.append(p.getName().equals(affected) ? "firstPerson" : "thirdPerson");

                                        messages.add(LangManager.getString(messagePath.toString(), p, affected, player.getName(), group.getName(), perm.getGroup().getName(), region.getName()));
                                    }
                                }
                                groups.remove(group);
                            }
                        }

                        for (String name : playerPermissionsMap.keySet()) {
                            for (Hierarchy.Group g : playerPermissionsMap.get(name)) {
                                for (Player p : msgRecipients) {
                                    messages = playerMessagesMap.get(p);
                                    messagePath.setLength(0);
                                    messagePath.append("region_permission_remove_");
                                    messagePath.append(p.equals(player) ? "firstPerson" : "thirdPerson").append("_");
                                    messagePath.append(p.getName().equals(name) ? "firstPerson" : "thirdPerson");

                                    messages.add(LangManager.getString(messagePath.toString(),p,name,player.getName(),g.getName(),region.getName()));
                                }
                            }
                        }

                        for (Player p : msgRecipients) {
                            for (String s : playerMessagesMap.get(p)) {
                                p.sendMessage(s);
                            }
                        }

                    } catch (IllegalArgumentException ex) {
                        player.sendMessage(ex.getMessage());
                        e.setCancelled(true);
                    } catch (TotemLectern.unreadableDeedsException ex) {
                        StringBuilder message = new StringBuilder();
                        message.append(ex.getMessage()).append("§r");
                        for (String error : ex.errors) {
                            message.append("\n").append(error);
                        }
                        player.sendMessage(message.toString());
                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInteractWithTotem(PlayerInteractEntityEvent e){

        if(e.getRightClicked() instanceof EnderCrystal crystal){
            if(Totem.isTotem(crystal)){
                Player player = e.getPlayer();
                PlayerInventory inventory = player.getInventory();
                Totem totem = Totem.getFromEndCrystal(crystal);
                ItemStack handed = inventory.getItemInMainHand();
                Material    item = handed.getType(),
                            upgrade = Landlords.CONFIG.totemUpgradeItem.item(),
                            downgrade = Landlords.CONFIG.totemDowngradeItem.item();

                if(upgrade.equals(item) || downgrade.equals(item)){
                    boolean resized; int toResize;
                    Config.TotemInteractionData interactionData;

                    e.setCancelled(true);

                    if (upgrade.equals(item)) {
                        toResize = 1;
                        interactionData = Landlords.CONFIG.totemUpgradeItem;
                    } else {
                        toResize = -1;
                        interactionData = Landlords.CONFIG.totemDowngradeItem;
                    }
                    try {
                        ItemStack itemStack = new ItemStack(item,interactionData.count());
                        if (inventory.contains(itemStack,interactionData.count())) {
                            resized = totem.resize(toResize, toResize);
                        } else {
                            resized = false;
                        }

                        if(resized){
                            Region region = totem.getRegion();
                            String msgPath = "region_resize";
                            String[] msgArgs = { player.getName(), region.getName(), Double.toString(region.getWidthX()), Double.toString(region.getHeight()), Double.toString(region.getWidthZ()) };
                            Config.GroupLevelReach msgReach = Landlords.CONFIG.msgReachRegionResize;

                            switch (msgReach) {
                                case lvl -> region.broadCastToMembersLang(msgPath, msgArgs, msgReach.getLevel());
                                case members -> region.broadCastToMembersLang(msgPath, msgArgs, region.getHierarchy().getHighestLevel());
                                case all -> Landlords.Utils.broadcastMessageLang(msgPath, msgArgs);
                                case responsible -> player.sendMessage(LangManager.getString(msgPath, player, msgArgs));
                                default -> {}
                            }

                            if (interactionData.consume()) {
                                inventory.removeItem(itemStack);
                            }
                        }
                    } catch (TotemUnresizableException ignored) {

                    }
                }
                if (item.equals(Landlords.CONFIG.deedsExchangeItem)) {

                    if(Deeds.isTotemDeeds(handed)) { return; }

                    Deeds deeds = new Deeds(totem.getRegion(),player);
                    ItemStack book = deeds.write();
                    player.closeInventory();
                    inventory.removeItem(handed);
                    inventory.setItemInMainHand(book);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (Bukkit.getOnlinePlayers().size() > 0) { return; }
        this.loadTotems_(Bukkit.getWorlds().stream().flatMap(w -> w.getEntities().stream()));
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        this.loadTotems_(Arrays.stream(e.getChunk().getEntities()));
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent e) {
        Entity[] entities = e.getChunk().getEntities();
        for (Entity entity : entities) {
            if (entity instanceof EnderCrystal crystal) {
                if(Totem.isTotem(crystal)){
                    TotemManager.getTotems().remove(Totem.getFromEndCrystal(crystal));
                }
            }
        }
    }

    @EventHandler
    public void onBlockUpdateEvent(BlockPhysicsEvent e){
        TotemStructure structure;
        Totem[] totems = TotemManager.getPossibleTotemsAtBlock(e.getBlock());
        for(Totem t : totems){
            structure = TotemManager.chekStructuresFromPoint(t.getPosX(), t.getPosY(), t.getPosZ(),t.getWorld());
            if(structure == null){
                t.enabled(false);
            }else{
                t.setStructure(structure);
                t.enabled(true);
            }
        }
    }

    @EventHandler
    public void onPlaceLectern(BlockPlaceEvent e) {

        Block block = e.getBlock();
        if (!(block.getState() instanceof Lectern)) { return; }

        TotemLectern totemLectern = TotemManager.getLecternAtSpot(block);
        if (totemLectern == null) {
            return;
        }
        Directional dir = (Directional) block.getBlockData();
        dir.setFacing(totemLectern.getFacing());
        totemLectern.convert(block);
        block.setBlockData(dir);

    }

    @EventHandler
    public void onDestroyCrystal(EntityDamageByEntityEvent e){
        if (!(e.getEntity() instanceof EnderCrystal crystal)) { return; }
        if (!Totem.isTotem(crystal)) { return; }

        e.setCancelled(true);
        Totem totem = Totem.getFromEndCrystal(crystal);
        BiConsumer<Player,Arrow> consumer = (p,a) -> {
            TotemDestroyedByPlayerEvent event = new TotemDestroyedByPlayerEvent(p,totem,a);
            RegionsLib.getMain().getServer().getPluginManager().callEvent(event);
            if (!event.isCancelled()) { totem.getRegion().destroy(p); }
        };

        Entity destroyer = e.getDamager();
        if (destroyer instanceof Player player) {
            if (totem.getLevel() > 0) { return; }
            consumer.accept(player,null);
        } else if (destroyer instanceof Arrow arrow) {
            if (!(arrow.getShooter() instanceof Player player)) { return; }
            List<PotionType> effects = Landlords.CONFIG.totemDestroyArrowEffects;
            if (!effects.isEmpty() && !effects.contains(arrow.getBasePotionData().getType())) { return; }
            consumer.accept(player,arrow);
        }

    }

    @EventHandler
    public void onRegionDestroy(RegionDestroyEvent e) {
        RegionData regionData = e.getRegion().getDataContainer().get("totemRegion");
        if (regionData == null) { return; }
        if (!regionData.getAsBoolean()) { return; }
        Totem totem = TotemManager.getTotemFromId(e.getRegion().getId());
        if (totem != null) { totem.destroy(); }
    }

    @EventHandler
    public void onEditBook(PlayerEditBookEvent e){
        Player player = e.getPlayer();

        if (!deedPlayerSlotMap_.containsKey(player)) {
            return;
        }

        BookMeta oldMeta = e.getPreviousBookMeta();
        Deeds deeds = Deeds.getFromBook(oldMeta, player);
        if (deeds == null) { return; }

        List<String> errors = new ArrayList<>();
        BookMeta newMeta = e.getNewBookMeta();
        for (int i = 1; i <= newMeta.getPages().size(); i++) {
            try {
                deeds.readPage(newMeta,i);
            } catch (IllegalArgumentException ex) {
                errors.add(ex.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            StringBuilder message = new StringBuilder();
            message.append(LangManager.getString("deeds_read_errorHeader",player,Integer.toString(errors.size())) + "§r");
            for (String error : errors) {
                message.append("\n" + error);
            }
            player.sendMessage(message.toString());
        }

        player.getInventory().setItem(deedPlayerSlotMap_.get(player),deeds.getItemStack());

    }

    private void loadTotems_(Stream<Entity> entities) {
        entities
                .filter(e -> e instanceof EnderCrystal)
                .map(e -> (EnderCrystal) e)
                .filter(Totem::isTotem)
                .forEach(c -> TotemManager.removeTotem(Totem.getFromEndCrystal(c)));
    }
}
