package com.jkantrell.landlords.totems;

import com.jkantrell.landlords.io.Config;
import com.jkantrell.landlords.io.LangManager;
import com.jkantrell.regionslib.regions.*;
import com.jkantrell.regionslib.regions.dataContainers.RegionData;
import com.jkantrell.landlords.Landlords;
import com.jkantrell.regionslib.regions.dataContainers.RegionDataContainer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;

public class Totem {

    //STATIC FIELDS
    private static List<Totem> totems_ = new ArrayList<>();
    private static boolean listenerRegistered_ = false;
    protected static NamespacedKey regionIdKey = new NamespacedKey(Landlords.getMainInstance(),"regionId");
    protected static NamespacedKey blueprintIdKey = new NamespacedKey(Landlords.getMainInstance(),"blueprintId");
    protected static NamespacedKey leveledKey = new NamespacedKey(Landlords.getMainInstance(),"leveled");
    protected static NamespacedKey isTotemKey = new NamespacedKey(Landlords.getMainInstance(),"isTotem");
    protected static final BlockFace[] BLOCK_FACE_DIRECTIONS = new BlockFace[]{
        BlockFace.WEST,BlockFace.DOWN,BlockFace.NORTH,BlockFace.EAST,BlockFace.UP,BlockFace.SOUTH
    };

    //STATIC METHODS
    public static void registerListener() {
        Landlords mainInstance = Landlords.getMainInstance();
    }
    public static boolean isListenerRegistered() {
        return Totem.listenerRegistered_;
    }
    public static boolean isTotem(Entity entity) {
        if (!(entity instanceof EnderCrystal enderCrystal)) { return false; }
        return Totem.isTotem(enderCrystal);
    }
    public static boolean isTotem(EnderCrystal enderCrystal) {
        return enderCrystal.getPersistentDataContainer().has(isTotemKey,PersistentDataType.BYTE);
    }
    public static Totem fromRegion(Region region) {
        RegionData data = region.getDataContainer().get("totemId");
        if (data == null) { return null; }
        String id = data.getAsString();
        return Totem.totems_.stream()
                .filter(t -> t.getRegionId() == region.getId())
                .filter(t -> t.getUniqueId().toString().equals(id))
                .findFirst()
                .orElse(null);
    }
    @SuppressWarnings("ConstantConditions")
    public static Totem fromEnderCrystal(EnderCrystal crystal) {
        if (!Totem.isTotem(crystal)) { return null; }

        PersistentDataContainer data = crystal.getPersistentDataContainer();
        int bluePrintId = data.get(blueprintIdKey, PersistentDataType.INTEGER);
        Location loc = crystal.getLocation();

        Totem t = new Totem(loc.add(0,.5,0), Blueprint.get(bluePrintId));
        t.regionId_ = data.get(regionIdKey, PersistentDataType.INTEGER);
        t.leveled_ = data.get(leveledKey, PersistentDataType.INTEGER);
        t.endCrystal_ = crystal;
        t.placed_ = true;

        if (t.blueprint_ == null) {
            Logger logger = Landlords.getMainInstance().getLogger();
            logger.warning("Loaded totem with blueprint ID " + bluePrintId + ", but no blueprint under such ID was found. The totem won't work properly.");
            logger.warning("    UUID: " + crystal.getUniqueId().toString());
            logger.warning("    Location: [" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + "]");
            logger.warning("    Region ID: " + t.regionId_);
        }

        if (!Totem.totems_.contains(t)) {Totem.totems_.add(t);}

        return t;
    }

    //FIELDS
    private final Blueprint blueprint_;
    private final Face[] faces_ = new Face[6];
    private Location location_;
    private boolean placed_ = false;
    private int leveled_ = 0;
    private int regionId_;
    private Region region_ = null;
    private EnderCrystal endCrystal_ = null;
    private int cooldown_ = Landlords.CONFIG.totemInteractCoolDown;

    //CONSTRUCTORS
    public Totem(Location location, Blueprint blueprint){
        this.relocate(location);
        this.blueprint_ = blueprint;
        for (int i = 0; i < 6; i++) { faces_[i] = new Face(Totem.BLOCK_FACE_DIRECTIONS[i]); }
    }

    //GETTERS
    public Blueprint getBlueprint() {
        return blueprint_;
    }
    public Location getLocation() {
        return location_;
    }
    public Block getContainingBlock() {
        return this.location_.getBlock();
    }
    public World getWorld() {
        return this.location_.getWorld();
    }
    public boolean isEnabled() {
        Region region = this.getRegion();
        if (region == null) { return false; }
        return region.isEnabled();
    }
    public boolean isPlaced() {
        return placed_;
    }
    public int getLevel() {
        return this.leveled_;
    }
    public Region getRegion() {
        if (this.region_ == null) {
            if (!this.placed_) { return null; }
            Region r = Region.get(this.regionId_);
            if (r == null) { return null; }
            RegionData regionData = r.getDataContainer().get("totemId");
            if (regionData == null) { return null; }
            if (!regionData.getAsString().equals(this.getUniqueId().toString())) { return null; }
            this.region_ = r;
        }
        return this.region_;
    }
    public int getRegionId() {
        return regionId_;
    }
    public UUID getUniqueId() {
        if (this.endCrystal_ == null) { return null; }
        return this.endCrystal_.getUniqueId();
    }
    public EnderCrystal getEndCrystal() {
        return endCrystal_;
    }

    //SETTERS
    public void relocate(Location loc) {
        if (loc.getWorld() == null) {
            throw new NullPointerException("The 'World' parameter passed to a totem's constructor cannot be null.");
        }
        this.location_ = loc;

        if (this.endCrystal_ == null) { return; }

        this.endCrystal_.teleport(loc.add(0,-.5,0));

        Region r = this.getRegion();
        if (r == null) { return; }
        RegionDataContainer dataContainer = r.getDataContainer();
        Block b = this.getContainingBlock();
        dataContainer.remove("totemLoc");
        dataContainer.add(new RegionData("totemLoc", new int[] {b.getX(), b.getY(), b.getZ()}));
    }
    public void setEnabled(Boolean isEnabled) {
        try {
            this.getRegion().enabled(isEnabled);
        } catch (NullPointerException ignored) {}
    }

    //METHODS
    public Region place(Player placer) {
        //Creating the region
        BoundingBox vertex = new BoundingBox();
        vertex.shift(this.location_);
        for (int i = 0; i < 3; i++) {
            vertex.expand(Totem.BLOCK_FACE_DIRECTIONS[i], this.blueprint_.getRegionInitialVertex()[i]);
            vertex.expand(Totem.BLOCK_FACE_DIRECTIONS[i+3], -this.blueprint_.getRegionInitialVertex()[i+3]);
        }

        this.region_ = new Region(
            new double[] { vertex.getMinX(), vertex.getMinY(), vertex.getMinZ(), vertex.getMaxX(), vertex.getMaxY(), vertex.getMaxZ() },
            this.location_.getWorld(),
            (placer == null) ? "Unnamed Region" : LangManager.getString("totem_region_playerPlacedName",placer,placer.getName()),
            this.blueprint_.getHierarchy(),
            placer
        );
        this.regionId_ = this.region_.getId();

        //Spawning an EndCrystal
        this.endCrystal_ = (EnderCrystal) this.location_.getWorld().spawnEntity(this.location_.add(0,-.5,0), EntityType.ENDER_CRYSTAL);
        this.endCrystal_.setShowingBottom(false);

        //Assigning the region to the crystal
        RegionDataContainer regionData = this.region_.getDataContainer();
        Block b = this.getContainingBlock();
        regionData.add(new RegionData("totemId", this.getUniqueId().toString()));
        regionData.add(new RegionData("totemLoc", new int[] {b.getX(), b.getY(), b.getZ()}));

        //Enabling
        this.placed_ = true;
        this.save();

        //Diegetic feedback
        this.region_.displayBoundaries(Landlords.CONFIG.regionsBorderRefreshRate,Landlords.CONFIG.regionsBorderPersistencePlaced);
        this.getWorld().playSound(this.getLocation(), Sound.BLOCK_BEACON_ACTIVATE,3, 0.5f);
        double[] particlePos = Landlords.CONFIG.totemPlaceParticlePos;
        Config.ParticleData particleData = Landlords.CONFIG.totemPlaceParticleEffect;
        this.getWorld().spawnParticle(
                particleData.particle(),
                this.location_.add(particlePos[0], particlePos[1], particlePos[2]),
                particleData.count(),
                particleData.delta()[0], particleData.delta()[1], particleData.delta()[2]
        );


        return this.region_;
    }
    public void scale(int levelAdd) throws TotemUnresizableException {
        for (int i = 0; i < 6; i++) {
            faces_[i].expand(this.getBlueprint().getRegionGrowthRate()[i] * levelAdd);
        }
        this.leveled_ += levelAdd;
        this.save();
    }
    public TotemLectern getLecternAt(int x, int y, int z) {

        TotemLectern r = null;
        Block containingBlock = this.getContainingBlock();
        int[]   pos = {containingBlock.getX(), containingBlock.getY(), containingBlock.getX()},
                blockPos = {x, y, z},
                lPos;

        for (TotemLectern l : this.getBlueprint().lecterns) {
            lPos = l.getAbsolutePosition(pos[0], pos[1], pos[2]);
            if (Arrays.equals(lPos, blockPos)) {
                r = l.clone();
                r.setTotem(this);
                break;
            }
        }
        return r;
    }
    public TotemLectern getLecternAt(Block block) {
        if (!block.getWorld().equals(this.getWorld())) { return null; }
        return this.getLecternAt(block.getX(), block.getY(), block.getZ());
    }
    public void save() {
        PersistentDataContainer endCrystalData = this.endCrystal_.getPersistentDataContainer();
        endCrystalData.set(Totem.isTotemKey, PersistentDataType.BYTE, (byte)1);
        endCrystalData.set(Totem.regionIdKey, PersistentDataType.INTEGER, this.regionId_);
        endCrystalData.set(Totem.blueprintIdKey, PersistentDataType.INTEGER, this.blueprint_.getId());
        endCrystalData.set(Totem.leveledKey, PersistentDataType.INTEGER, this.leveled_);
        try { this.getRegion().save(); } catch (NullPointerException ignored) {}
        if (!Totem.totems_.contains(this)) { Totem.totems_.add(this); }
    }
    public void destroy() {
        this.dropItem_(this.getLevel());
        TotemManager.removeTotem(this);
        this.getWorld().createExplosion(this.endCrystal_.getLocation(), 6f,true,true);
        this.endCrystal_.remove();
    }

    //PRIVATE METHODS
    private void dropItem_(int amount) {
        for (int i = 0; i < amount; i++) {
            double random = Math.random();
            if (random <= Landlords.CONFIG.totemDropBackRate) {
                Item item = this.getWorld().dropItem(this.getLocation(), new ItemStack(Landlords.CONFIG.totemUpgradeItem.item()));
                item.setInvulnerable(true);
            }
        }
    }

    //CLASSES
    private class Face {

        //FIELDS
        private final BlockFace face_;
        private final Function<BoundingBox, Double> overlappingCalc_;
        private boolean colliding_ = false;

        //CONSTRUCTOR
        Face(BlockFace face) {
            this.face_ = face;
            this.overlappingCalc_ = switch (face) {
                case NORTH, SOUTH -> BoundingBox::getWidthZ;
                case WEST, EAST -> BoundingBox::getWidthX;
                default -> BoundingBox::getHeight;
            };
        }

        //GETTERS
        BlockFace getBlocFace() {
            return this.face_;
        }
        Face getOpposite() {
            return Arrays.stream(Totem.this.faces_)
                    .filter(f -> f.getBlocFace().equals(this.getBlocFace().getOppositeFace()))
                    .findFirst()
                    .orElse(null);
        }
        boolean isColliding() {
            return this.colliding_;
        }

        //METHODS
        void expand(double amount) throws TotemUnresizableException {
            Region reg = Totem.this.getRegion();
            if (reg == null) { return; }

            reg.expand(this.getBlocFace(),amount);

            if (amount < 0) { return; }

            double leftOver = 0;
            for (Region r : reg.getOverlappingRegions()) {
                leftOver = Math.max(leftOver, this.overlappingCalc_.apply(reg.getBoundingBox().intersection(r.getBoundingBox())));
            }
            if (leftOver == 0) {
                this.colliding_ = false;
                return;
            }

            reg.expand(this.getBlocFace(), - leftOver );

            this.colliding_ = true;
            Face opposite = this.getOpposite();

            if (opposite.isColliding()) {
                List<Face> faces = Arrays.stream(Totem.this.faces_).filter(f -> !f.isColliding()).toList();
                if (faces.isEmpty()) {
                    Bukkit.broadcastMessage("Unexpandable region");
                    throw new TotemUnresizableException();
                }
                double frac = leftOver / faces.size();
                for (Face f : faces) { f.expand(frac); }
            } else {
               opposite.expand(leftOver);
            }
        }
    }
}