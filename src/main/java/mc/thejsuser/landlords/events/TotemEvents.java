package mc.thejsuser.landlords.events;

import mc.thejsuser.landlords.Landlords;
import mc.thejsuser.landlords.io.LangManager;
import mc.thejsuser.landlords.regionElements.Abilities;
import mc.thejsuser.landlords.regionElements.Group;
import mc.thejsuser.landlords.regionElements.Permission;
import mc.thejsuser.landlords.regionElements.Region;
import mc.thejsuser.landlords.io.ConfigManager;
import mc.thejsuser.landlords.totemElements.*;
import org.bukkit.*;
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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class TotemEvents implements Listener {

    //STATIC FIELDS
    private static HashMap<Player, EquipmentSlot> deedPlayerSlotMap_ = new HashMap<>();
    private static final ConfigManager.groupLevelReach regionResizeMessageRange = ConfigManager.getTotemResizeMessageLevelReach();

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
                    boolean onObsidian = block.getType().equals(Material.OBSIDIAN);

                    switch (ConfigManager.getEndCystalOnAnyBlock()) {
                        case never:
                            if (!onObsidian) {
                                break;
                            }

                        case on_totem:
                            if (structure == null) {
                                break;
                            }

                        case always:
                            if (structure == null && !onObsidian) {
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
                        String oldName = region.getName(), newName;
                        lectern.readDeeds(item,player);
                        newPerms = new ArrayList<>(Arrays.asList(region.getPermissions()));
                        newName = region.getName();

                        if (oldPerms.equals(newPerms) && oldName.equals(region.getName())) { return; }

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

                        HashMap<String,List<Group>> playerPermissionsMap = new HashMap<>();
                        for (Permission oldPerm : oldPerms) {
                            String name = oldPerm.getPlayerName();
                            if (!playerPermissionsMap.containsKey(name)) {
                                playerPermissionsMap.put(name,new ArrayList<>());
                            }
                            playerPermissionsMap.get(name).add(oldPerm.getGroup());
                        }

                        for (Permission perm : newPerms) {

                            String affected = perm.getPlayerName();
                            List<Group> groups = playerPermissionsMap.getOrDefault(affected,Collections.emptyList());
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
                                Group group = groups.get(0);
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

                        for (String playerName : playerPermissionsMap.keySet()) {
                            for (Group g : playerPermissionsMap.get(playerName)) {
                                for (Player p : msgRecipients) {
                                    messages = playerMessagesMap.get(p);
                                    messagePath.setLength(0);
                                    messagePath.append("region_permission_remove_");
                                    messagePath.append(p.equals(player) ? "firstPerson" : "thirdPerson").append("_");
                                    messagePath.append(p.getName().equals(playerName) ? "firstPerson" : "thirdPerson");

                                    messages.add(LangManager.getString(messagePath.toString(),p,playerName,player.getName(),g.getName(),region.getName()));
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
                            upgrade = ConfigManager.getTotemUpgradeItem(),
                            downgrade = ConfigManager.getTotemDowngradeItem();

                if(upgrade.equals(item) || downgrade.equals(item)){
                    boolean resized, consume; int toResize;

                    e.setCancelled(true);

                    if (upgrade.equals(item)) {
                        toResize = 1;
                        consume = ConfigManager.getTotemUpgradeItemConsume();
                    } else {
                        toResize = -1;
                        consume = ConfigManager.getTotemDowngradeItemConsume();
                    }
                    try {
                        resized = totem.resize(toResize, toResize);
                        if(resized){
                            Region region = totem.getRegion();
                            String msgPath = "region_resize";
                            String[] msgArgs = { player.getName(), region.getName(), Double.toString(region.getWidthX()), Double.toString(region.getHeight()), Double.toString(region.getWidthZ()) };

                            switch (regionResizeMessageRange) {
                                case lvl -> region.broadCastToMembersLang(msgPath, msgArgs, regionResizeMessageRange.getLevel());
                                case members -> region.broadCastToMembersLang(msgPath, msgArgs, Group.getHighestLevel());
                                case all -> Landlords.Utils.broadcastMessageLang(msgPath, msgArgs);
                                case responsible -> player.sendMessage(LangManager.getString(msgPath, player, msgArgs));
                                default -> {}
                            }

                            if (consume) {
                                inventory.removeItem(new ItemStack(item));
                            }
                        }
                    } catch (TotemUnresizableException ignored) {

                    }
                }
                if (item.equals(ConfigManager.getScriptureExchangeItem())) {

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
    public void onPlayerJoining(PlayerJoinEvent e){

        if(Landlords.getMainInstance().getServer().getOnlinePlayers().size()==1){
            new BukkitRunnable(){
                @Override
                public void run() {
                    TotemManager.loadTotems();
                }
            }.runTaskLater(Landlords.getMainInstance(),40);
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

        Entity entity = e.getEntity();
        if(!entity.getType().equals(EntityType.ENDER_CRYSTAL)){ return; }
        EnderCrystal crystal = (EnderCrystal) entity;
        if(!Totem.isTotem(crystal)) { return; }
        Totem totem = Totem.getFromEndCrystal(crystal);

        entity = e.getDamager();
        if(entity instanceof Arrow arrow) {
            List<PotionType> effects = ConfigManager.getTotemDestroyArrowEffects();

            if(effects.isEmpty()) {
                totem.destroy();
                return;
            }
            if(effects.contains(arrow.getBasePotionData().getType())){
                totem.destroy();
                return;
            }
        } else if (entity instanceof Player player) {
            if (
                    totem.getRegion().checkAbility(player,Abilities.can_destroy_totems) &&
                    totem.getLevel() == 0
            ) {
                totem.destroy();
                return;
            }
        }
        e.setCancelled(true);
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
}
