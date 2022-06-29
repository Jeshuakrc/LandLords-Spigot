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
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
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
    private final Direction[] directions_ = {new Direction(Axis.X), new Direction(Axis.Y), new Direction(Axis.Z)};
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
            Region r = Regions.get(this.regionId_);
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
        double[] expansions = this.getBlueprint().getRegionGrowthRate();
        try {
            for (int i = 0; i < 3; i++) {
                this.directions_[i].expand(expansions[i] * levelAdd, expansions[i + 3] * levelAdd);
            }
            this.leveled_ += levelAdd;
        } finally {
            this.save();
        }
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
    private class Direction {
        //FIELDS
        private final Face min_;
        private final Face max_;
        private final Axis axis_;
        private final Function<Vector,Double> vectorExtractor_;
        private boolean maxed_;

        //CONSTRUCTOR
        Direction(Axis axis) {
            BlockFace min, max;
            switch (axis) {
                case X -> {
                    min = BlockFace.WEST;
                    max = BlockFace.EAST;
                    this.vectorExtractor_ = Vector::getX;
                }
                case Z -> {
                    min = BlockFace.NORTH;
                    max = BlockFace.SOUTH;
                    this.vectorExtractor_ = Vector::getZ;
                }
                case Y -> {
                    min = BlockFace.DOWN;
                    max = BlockFace.UP;
                    this.vectorExtractor_ = Vector::getY;
                }
                default -> { min = null; max = null; this.vectorExtractor_ = null; }
            }
            this.min_ = new Face(min,this);
            this.max_ = new Face(max, this);
            this.axis_ = axis;
        }

        //GETTERS
        Face getOppositeFace(Face face) {
            return this.getOppositeFace(face.getBlocFace());
        }
        Face getOppositeFace(BlockFace face) {
            if (face.equals(this.min_.getBlocFace())) {
                return max_;
            } else if (face.equals(this.max_.getBlocFace())) {
                return min_;
            }
            return null;
        }
        boolean isMaxed() {
            return this.maxed_;
        }

        //METHODS
        void expand(double minDir, double maxDir) throws TotemUnresizableException {
            Region reg = Totem.this.getRegion();
            if (reg == null) { return; }

            TotemUnresizableException.Reason errorReason = TotemUnresizableException.Reason.SIZE_MAXED_OUT;
            double  toExpandMin = minDir, toExpandMax = maxDir,
                    leftOverMin = 0, leftOverMax =0,
                    toExpand = toExpandMin + toExpandMax,
                    allowedExpansion = this.vectorExtractor_.apply(Totem.this.getBlueprint().getRegionMaxSize()) - reg.getLength(this.axis_);

            if (toExpand < 0) {



            } else if (toExpand > allowedExpansion) {
                toExpandMax = allowedExpansion / 2;
                toExpandMin = allowedExpansion / 2;
                if (toExpandMin > minDir) {
                    toExpandMax += toExpandMin - minDir;
                    toExpandMin = minDir;
                }
                if (toExpandMax > maxDir) {
                    toExpandMin += toExpandMax - maxDir;
                    toExpandMax = maxDir;
                }
                leftOverMin = minDir - toExpandMin; leftOverMax = maxDir - toExpandMax;
            }

            try {
                this.min_.expand(toExpandMin);
                this.max_.expand(toExpandMax);
            } catch (TotemUnresizableException e) {
                leftOverMin += e.getExceeded(this.min_.getBlocFace());
                leftOverMax += e.getExceeded(this.max_.getBlocFace());
                errorReason = TotemUnresizableException.Reason.FACE_COLLISION;
            }

            double leftOver = leftOverMin + leftOverMax;
            if (leftOver <= 0) {  this.maxed_ = false; return; }
            this.maxed_ = true;

            List<Direction> availableDirections = Arrays.stream(Totem.this.directions_).filter(d -> !d.isMaxed()).toList();
            if (availableDirections.isEmpty()) {
                throw new TotemUnresizableException(leftOverMin,leftOverMax,this.axis_, errorReason);
            }
            toExpand = leftOver / availableDirections.size();
            for (Direction d : availableDirections) {
                d.expand(toExpand/2, toExpand/2);
            }
        }
    }
    private class Face {
        //FIELDS
        private final BlockFace face_;
        private final Direction direction_;
        private boolean colliding_ = false;

        //CONSTRUCTOR
        Face(BlockFace face, Direction direction) {
            this.face_ = face;
            this.direction_ = direction;
        }

        //GETTERS
        BlockFace getBlocFace() {
            return this.face_;
        }
        Face getOpposite() {
            return this.direction_.getOppositeFace(this);
        }
        boolean isColliding() {
            return this.colliding_;
        }

        //METHODS
        void expand(double amount) throws TotemUnresizableException {
            if (amount == 0) { return; }
            Region reg = Totem.this.getRegion();
            if (reg == null) { return; }

            reg.expand(this.getBlocFace(),amount);

            if (amount < 0) { return; }

            double leftOver = 0;
            for (Region r : reg.getOverlappingRegions()) {
                leftOver = Math.max(leftOver, Math.abs(reg.getFacePos(this.getBlocFace()) - r.getFacePos(this.getOpposite().getBlocFace())));
            }
            if (leftOver == 0) {
                this.colliding_ = false;
                return;
            }

            reg.expand(this.getBlocFace(), -leftOver);
            this.colliding_ = true;

            Face opposite = this.getOpposite();
            if (opposite.isColliding()) {
                throw new TotemUnresizableException(leftOver, this.getBlocFace(), TotemUnresizableException.Reason.FACE_COLLISION);
            } else {
               opposite.expand(leftOver);
            }
        }
    }
}