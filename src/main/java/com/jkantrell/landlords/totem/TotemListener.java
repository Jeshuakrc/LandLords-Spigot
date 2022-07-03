package com.jkantrell.landlords.totem;

import com.jkantrell.landlords.Landlords;
import com.jkantrell.landlords.event.TotemDestroyedByPlayerEvent;
import com.jkantrell.landlords.io.Config;
import com.jkantrell.landlords.io.LangManager;
import com.jkantrell.landlords.totem.Exception.*;
import com.jkantrell.regionslib.RegionsLib;
import com.jkantrell.regionslib.events.RegionDestroyEvent;
import com.jkantrell.regionslib.regions.Region;
import com.jkantrell.regionslib.regions.dataContainers.RegionData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Lectern;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

public class TotemListener implements Listener {

    //STATIC FIELDS
    private static HashMap<Player, EquipmentSlot> deedPlayerSlotMap_ = new HashMap<>();

    //EVENTS
    @EventHandler
    public void onPlaceTotem(PlayerInteractEvent e) {
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) { return; }

        ItemStack item = e.getItem();
        if (item == null) { return; }
        if (!item.getType().equals(Material.END_CRYSTAL)) { return; }

        Block block = e.getClickedBlock();
        if (block == null) { return; }

        Location loc = block.getRelative(e.getBlockFace()).getLocation().add(.5,.5,.5);
        Blueprint blueprint = TotemManager.chekStructuresFromPoint(loc);
        if (blueprint == null) { return; }

        Player player = e.getPlayer();
        e.setCancelled(true);
        if (!player.getGameMode().equals(GameMode.CREATIVE)) {
            player.getInventory().removeItem(new ItemStack(Material.END_CRYSTAL,1));
        }
        new Totem(loc,blueprint).place(player);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (Bukkit.getOnlinePlayers().size() > 0) { return; }
        this.loadTotems_(Bukkit.getWorlds().stream().flatMap(w -> w.getEntities().stream()));
    }

    @EventHandler
    public void onChunkLoad(EntitiesLoadEvent e) {
        this.loadTotems_(e.getEntities().stream());
    }

    @EventHandler
    public void onChunkUnload(EntitiesUnloadEvent e) {
        e.getEntities().stream()
                .filter(ent -> ent instanceof EnderCrystal)
                .map(ent -> (EnderCrystal) ent)
                .filter(Totem::isTotem)
                .map(Totem::fromEnderCrystal)
                .forEach(t -> {
                    TotemManager.unregisterTotem(t);
                    Bukkit.broadcastMessage("Totem unloaded. ID: " + t.getRegionId());
                });
    }

    @EventHandler
    public void onBlockUpdateEvent(BlockPhysicsEvent e){

    }

    @EventHandler
    public void onPlaceLectern(BlockPlaceEvent e) {
        Block block = e.getBlock();
        if (!(block.getState() instanceof Lectern)) { return; }
        TotemLectern totemLectern = TotemManager.getLecternAtSpot(block);
        if (totemLectern == null) { return; }
        Directional dir = (Directional) block.getBlockData();
        dir.setFacing(totemLectern.getFacing());
        totemLectern.convert(block);
        block.setBlockData(dir);
    }

    @EventHandler
    public void onFeedTotem(PlayerInteractEntityEvent e){
        //Checking if the entity is a totem
        if (!(e.getRightClicked() instanceof EnderCrystal crystal)) { return; }
        if (!Totem.isTotem(crystal)) { return; }

        //Checking if the totem has a region
        Totem totem = Totem.fromEnderCrystal(crystal);
        Optional<Region> region = totem.getRegion();
        if (region.isEmpty()) { return; }

        //Checking if the item can interact with a totem
        Player player = e.getPlayer();
        PlayerInventory inventory = player.getInventory();
        ItemStack item = inventory.getItem(e.getHand());
        if (item == null) { return; }
        Config.TotemInteractionData totemData = Stream.of(Landlords.CONFIG.totemUpgradeItem, Landlords.CONFIG.totemDowngradeItem)
                .filter(d -> item.getType().equals(d.item())).findFirst().orElse(null);
        if (totemData == null) { return; }

        //Checking if the player has enough items to interact
        if (totemData.consume() && !inventory.contains(totemData.item(), totemData.count())) { return; }

        //Saving the region's original size
        double[] originalSize = region.get().getCorners();

        //Placeholder for possible exceptions
        List<UnresizableReason> unresizableReasons = null;

        //Expanding the region
        try {
            int toResize = (totemData.equals(Landlords.CONFIG.totemDowngradeItem)) ? -1 : 1; //Scale in or out depending on the item
            totem.scale(toResize);
        } catch (TotemUnresizableException ex) {
            unresizableReasons = ex.getReasons(); //Fills placeholder for exception handling to execute.
        }

        //Checks if the region's size actually changed.
        String regionName = region.get().getName();
        if (!Arrays.equals(region.get().getCorners(), originalSize)) {
            if (totemData.consume() && !player.getGameMode().equals(GameMode.CREATIVE)) {
                inventory.removeItem(new ItemStack(totemData.item(), totemData.count()));
            }
            Vector size = region.get().getDimensions();
            DecimalFormat formater = new DecimalFormat("#0.00");
            World world = totem.getWorld();
            Location loc = totem.getLocation();
            player.sendMessage(LangManager.getString("totems.resized",
                    player,
                    regionName,
                    formater.format(size.getX()),
                    formater.format(size.getY()),
                    formater.format(size.getZ())
                    ));
            world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,SoundCategory.AMBIENT,3f, 1.5f);
            world.spawnParticle(Particle.PORTAL, loc, 120, 0.1,0.1,0.1,6);
            return;
        }

        //Checks if there is any exception to handle
        if (unresizableReasons == null) { return; }
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 3f, 0.5f);

        String  header = LangManager.getString("totems.unrezisable.header",player,regionName),
                basePath = "totems.unrezisable.", subPath = "default";
        String[] params = {};
        LinkedList<String> messages = new LinkedList<>();
        LinkedList<MaxSizeUnresizableReason> maxSizeReasons = new LinkedList<>();

        //Properly handling all unscaling reasons
        for (UnresizableReason reason : unresizableReasons) {
            if (reason instanceof MaxSizeUnresizableReason r) {
                maxSizeReasons.add(r);
            } else if (reason instanceof RegionCollisionUnresizableReason r) {
                subPath = "region_collision";
                params = new String[] {regionName, r.getCollidingRegion().getName(), r.getDirection().toString().toLowerCase()};
            } else if (reason instanceof OverShrunkUnresizableReason r) {
                subPath = "overshrink";
                params = new String[] {regionName, r.getDirection().toString().toLowerCase()};
            } else {
                continue;
            }
            messages.add(LangManager.getString(basePath + subPath, player, params));
        }

        //Unifying maz size unrezisable reasons, if eny
        if (!maxSizeReasons.isEmpty()) {
            Function<Axis, String> extractor = (a) -> {
                Vector v = totem.getBlueprint().getRegionMaxSize();
                return Double.toString(switch (a) { case X -> v.getX(); case Y -> v.getY(); case Z -> v.getZ(); });
            };
            int size = maxSizeReasons.size();
            subPath = "max_size.";
            if (size < 2) {
                Axis axis = maxSizeReasons.get(0).getAxis();
                subPath += "one";
                params = new String[] {regionName, axis.toString(), extractor.apply(axis)};
            } else if (size < 3) {
                Axis axis1 = maxSizeReasons.get(0).getAxis(), axis2 = maxSizeReasons.get(1).getAxis();
                subPath += "two";
                params = new String[] {regionName, axis1.toString(), axis2.toString(), extractor.apply(axis1), extractor.apply(axis2)};
            } else {
                subPath += "three";
                params = new String[] {regionName, extractor.apply(Axis.X), extractor.apply(Axis.Y), extractor.apply(Axis.Z)};
            }
            messages.add(LangManager.getString(basePath + subPath,player,params));
        }

        //If not a singe reason could be handled, adds a default message to the header.
        if (messages.isEmpty()) {
            String def = LangManager.getString("totems.unrezisable.default",player,region.get().getName());
            if (!def.equals("")) { header += " " + def; }
        }
        messages.push(header);

        //Warns the player of why the region could not be resized
        messages.stream().distinct().filter(m -> !m.equals("")).forEach(player::sendMessage);
    }

    @EventHandler
    public void onHurtTotem(EntityDamageByEntityEvent e){
        if (!(e.getEntity() instanceof EnderCrystal crystal)) { return; }
        if (!Totem.isTotem(crystal)) { return; }

        e.setCancelled(true);
        Totem totem = Totem.fromEnderCrystal(crystal);

        Player player = null;
        Arrow arrow = null;
        Entity destroyer = e.getDamager();
        if (destroyer instanceof Player player_) {
            player = player_;
        } else if (destroyer instanceof Arrow arrow_) {
            if (!(arrow_.getShooter() instanceof Player player_)) { return; }
            List<PotionType> effects = Landlords.CONFIG.totemDestroyArrowEffects;
            if (!effects.isEmpty() && !effects.contains(arrow_.getBasePotionData().getType())) { return; }
            player = player_; arrow = arrow_;
        }

        if (player == null) { return; }

        TotemDestroyedByPlayerEvent event = new TotemDestroyedByPlayerEvent(player,totem,arrow);
        RegionsLib.getMain().getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) { return; }

        if (totem.getLevel() > 0) {
            try {
                totem.scale(-1);
            } catch (TotemUnresizableException ignored) {}
        } else {
            Player finalPlayer = player;
            totem.getRegion().ifPresent(r -> r.destroy(finalPlayer));
        }
        World world = totem.getWorld();
        Location loc = totem.getLocation();
        world.playSound(loc, Sound.ENTITY_BLAZE_HURT,SoundCategory.AMBIENT,5f, 0.5f);
        world.spawnParticle(Particle.DRAGON_BREATH, loc, 80, 0.2,0.2,0.2);

        if (Math.random() > Landlords.CONFIG.totemDropBackRate) { return; }
        Config.TotemInteractionData itemData = Landlords.CONFIG.totemUpgradeItem;
        totem.getWorld().dropItemNaturally(totem.getLocation(),new ItemStack(itemData.item(), itemData.count())).setInvulnerable(true);
    }

    @EventHandler
    public void OnTotemKilled(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof EnderCrystal crystal)) { return; }
        if (!Totem.isTotem(crystal)) { return; }
        Totem.fromEnderCrystal(crystal)
                .getRegion()
                .ifPresent(Region::destroy);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onRegionDestroy(RegionDestroyEvent e) {
        Region region = e.getRegion();
        RegionData regionData = region.getDataContainer().get("totemId");
        if (regionData == null) { return; }

        Totem totem = Totem.fromRegion(region);
        if (totem != null) { totem.destroy(); }
    }

    @EventHandler
    public void onPlayerOpenBook(PlayerInteractEvent e) {

        ItemStack item = e.getItem();
        if (item == null) { return; }
        Player player = e.getPlayer();

        if (Deeds.isTotemDeeds(item)) {
            deedPlayerSlotMap_.put(player,e.getHand());
        } else {
            deedPlayerSlotMap_.remove(player);
        }

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
            message.append(LangManager.getString("deeds.error_message.compose.header",player,Integer.toString(errors.size())) + "§r");
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
                .forEach(c -> {
                    Totem totem = Totem.fromEnderCrystal(c);
                    TotemManager.registerTotem(totem);
                    if (totem.getRegion() == null) {totem.destroy();}
                    Bukkit.broadcastMessage("Totem loaded. ID: " +totem.getRegionId());
                });
    }
}
