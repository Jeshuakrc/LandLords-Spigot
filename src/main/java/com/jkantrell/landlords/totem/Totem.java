package com.jkantrell.landlords.totem;

import com.jkantrell.landlords.io.Config;
import com.jkantrell.landlords.io.LangManager;
import com.jkantrell.landlords.totem.Exception.*;
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

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
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
    public BoundingBox getBaseRegionBox() {
        BoundingBox box = new BoundingBox();
        box.shift(this.location_);
        for (int i = 0; i < 3; i++) {
            box.expand(Totem.BLOCK_FACE_DIRECTIONS[i], this.blueprint_.getRegionInitialVertex()[i]);
            box.expand(Totem.BLOCK_FACE_DIRECTIONS[i+3], -this.blueprint_.getRegionInitialVertex()[i+3]);
        }
        return box;
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
        String namePath = "totems.default_name." + ((placer == null) ? "undefined_placer" : "player_placed");
        this.region_ = new Region(
            this.getBaseRegionBox(),
            this.location_.getWorld(),
            LangManager.getString(namePath,placer,placer.getName()),
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
        double[] expansions = (levelAdd > 0) ?
                this.getBlueprint().getRegionGrowthRate() :
                new double[] {1,1,1,1,1,1};
        try {
            for (int i = 0; i < 3; i++) {
                this.directions_[i].expand(expansions[i] * levelAdd, expansions[i + 3] * levelAdd);
            }
            this.leveled_ += levelAdd;
        } finally {
            Arrays.stream(this.directions_).forEach(Direction::resetCollisions);
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
        private final Function<BoundingBox, Double> boxExtractorMin_;
        private final Function<BoundingBox, Double> boxExtractorMax_;
        private boolean maxed_;
        private boolean minned_;

        //CONSTRUCTOR
        Direction(Axis axis) {
            BlockFace min, max;
            switch (axis) {
                case X -> {
                    min = BlockFace.WEST;
                    max = BlockFace.EAST;
                    this.vectorExtractor_ = Vector::getX;
                    this.boxExtractorMin_ = BoundingBox::getMinX;
                    this.boxExtractorMax_ = BoundingBox::getMaxX;
                }
                case Z -> {
                    min = BlockFace.NORTH;
                    max = BlockFace.SOUTH;
                    this.vectorExtractor_ = Vector::getZ;
                    this.boxExtractorMin_ = BoundingBox::getMinZ;
                    this.boxExtractorMax_ = BoundingBox::getMaxZ;
                }
                case Y -> {
                    min = BlockFace.DOWN;
                    max = BlockFace.UP;
                    this.vectorExtractor_ = Vector::getY;
                    this.boxExtractorMin_ = BoundingBox::getMinY;
                    this.boxExtractorMax_ = BoundingBox::getMaxY;
                }
                default -> throw new IllegalArgumentException("Axis must be either X, Y or Z");
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
        boolean isMinned() {
            return this.minned_;
        }

        //METHODS
        void expand(double minDir, double maxDir) throws TotemUnresizableException {
            Region reg = Totem.this.getRegion();
            if (reg == null) { return; }

            double  toExpandMin = minDir, toExpandMax = maxDir,
                    leftOverMin = 0, leftOverMax =0,
                    toExpand = toExpandMin + toExpandMax,
                    regionMazSize = this.vectorExtractor_.apply(Totem.this.getBlueprint().getRegionMaxSize()),
                    allowedExpansion = regionMazSize - reg.getLength(this.axis_);

            if (toExpand == 0) { return; }

            UnresizableReason reason = null;
            if (toExpand < 0) {
                toExpand = 0;
                BoundingBox baseBox = Totem.this.getBaseRegionBox(), regBox = reg.getBoundingBox();
                Function<BoundingBox, Double>[] extractors = new Function[] { this.boxExtractorMin_, this.boxExtractorMax_ };
                double[] toExpands = new double[2];
                double[] orgExpands = {minDir, maxDir};
                for (int i = 0; i < 2; i++) {
                    double distance = Math.abs(extractors[i].apply(regBox) - extractors[i].apply(baseBox));
                    toExpands[i] = distance / Totem.this.getLevel() * orgExpands[i];
                    toExpand += toExpands[i];
                }
                toExpandMin = toExpands[0]; toExpandMax = toExpands[1];
            } else if ((regionMazSize > 0) && (toExpand > allowedExpansion)) {
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
                reason = new MaxSizeUnresizableReason(this.axis_);
            }

            List<UnresizableReason> innerReasons = Collections.emptyList();
            try {
                this.min_.expand(toExpandMin);
                this.max_.expand(toExpandMax);
            } catch (TotemUnresizableException e) {
                leftOverMin += e.getExceeded(this.min_.getBlocFace());
                leftOverMax += e.getExceeded(this.max_.getBlocFace());
                innerReasons = e.getReasons();
                if (innerReasons.stream().allMatch(r -> r instanceof OverShrunkUnresizableReason)) {
                    innerReasons = Collections.emptyList();
                    reason = new MinSizeUnresizableReason(this.axis_);
                } else {
                    reason = null;
                }
            }

            double leftOver = leftOverMin + leftOverMax;
            if (leftOver == 0) {
                this.maxed_ = false;
                this.minned_ = false;
                return;
            }
            if (leftOver < 0) { minned_ = true; } else { maxed_ = true; }

            Predicate<Direction> checker = (leftOver < 0) ? Direction::isMinned : Direction::isMaxed;
            List<Direction> availableDirections = Arrays.stream(Totem.this.directions_).filter(d -> !checker.test(d)).toList();
            if (availableDirections.isEmpty()) {
                TotemUnresizableException e = new TotemUnresizableException(Totem.this, leftOverMin,leftOverMax,this.axis_, null);
                e.addReason(reason);
                e.addReasons(innerReasons);
                throw e;
            }
            toExpand = leftOver / availableDirections.size();
            for (Direction d : availableDirections) {
                try {
                    d.expand(toExpand/2, toExpand/2);
                } catch (TotemUnresizableException e) {
                    e.addReasons(innerReasons);
                    e.addReason(reason);
                    throw e;
                }
            }
        }
        void resetCollisions() {
            this.maxed_ = false;
            this.minned_ = false;
            this.min_.resetCollisions();
            this.min_.resetCollisions();
        }
    }
    private class Face {
        //FIELDS
        private final BlockFace face_;
        private final Direction direction_;
        private boolean collidingOut_ = false;
        private boolean collidingIn_ = false;

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
        boolean isCollidingOut() {
            return this.collidingOut_;
        }
        boolean isCollidingIn() {
            return this.collidingIn_;
        }
        byte getSign() {
            return (byte) switch (this.getBlocFace()) {
                case DOWN, WEST, NORTH -> -1;
                default -> 1;
            };
        }
        double getFacePos(BoundingBox box) {
            return switch (this.getBlocFace()) {
                case UP -> box.getMaxY();
                case DOWN -> box.getMinY();
                case SOUTH -> box.getMaxZ();
                case NORTH -> box.getMinZ();
                case EAST -> box.getMaxX();
                case WEST -> box.getMinX();
                default -> throw new IllegalArgumentException("The provided BlockFace must be cartesian (NORTH, SOUTH, EAST, WEST, UP, DOWN).");
            };
        }

        //METHODS
        void expand(double amount) throws TotemUnresizableException {
            if (amount == 0) { return; }
            Region reg = Totem.this.getRegion();
            if (reg == null) { return; }

            reg.expand(this.getBlocFace(),amount);

            double leftOver = 0;
            Region collidingWith = null;

            if (amount > 0) {
                for (Region r : reg.getOverlappingRegions()) {
                    double length = Math.abs(reg.getFacePos(this.getBlocFace()) - r.getFacePos(this.getOpposite().getBlocFace()));
                    if (leftOver > length) { continue; }
                    leftOver = length;
                    collidingWith = r;
                }
            } else {
                double distance = (reg.getFacePos(this.getBlocFace()) - this.getFacePos(Totem.this.getBaseRegionBox())) * this.getSign();
                leftOver = Math.min(0, distance);
            }

            if (leftOver == 0) {
                this.collidingOut_ = false;
                this.collidingIn_ = false;
                return;
            }

            OneDirectionalUnresizableReason reason = null;
            Face opposite = this.getOpposite();
            boolean check;
            if (leftOver > 0) {
                this.collidingOut_ = true;
                reason = new RegionCollisionUnresizableReason(collidingWith, this.getBlocFace());
                check = opposite.isCollidingOut();
            } else {
                this.collidingIn_ = true;
                reason = new OverShrunkUnresizableReason(this.getBlocFace());
                check = opposite.isCollidingIn();
            }

            reg.expand(this.getBlocFace(), -leftOver);

            if (check) {
                throw new TotemUnresizableException(Totem.this, leftOver, reason);
            } else {
                try {
                    opposite.expand(leftOver);
                } catch (TotemUnresizableException e) {
                    e.addReason(new RegionCollisionUnresizableReason(collidingWith,this.getBlocFace()));
                    throw e;
                }
            }
        }
        void resetCollisions() {
            this.collidingIn_ = false;
            this.collidingOut_ = false;
        }
    }
}