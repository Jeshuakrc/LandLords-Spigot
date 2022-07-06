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
import java.util.stream.Stream;

public class Totem {

    //STATIC FIELDS
    private static List<Totem> totems_ = new ArrayList<>();
    protected static NamespacedKey regionIdKey = new NamespacedKey(Landlords.getMainInstance(),"regionId");
    protected static NamespacedKey blueprintIdKey = new NamespacedKey(Landlords.getMainInstance(),"blueprintId");
    protected static NamespacedKey leveledKey = new NamespacedKey(Landlords.getMainInstance(),"leveled");
    protected static NamespacedKey isTotemKey = new NamespacedKey(Landlords.getMainInstance(),"isTotem");
    protected static final BlockFace[] BLOCK_FACE_DIRECTIONS = new BlockFace[]{
        BlockFace.WEST,BlockFace.DOWN,BlockFace.NORTH,BlockFace.EAST,BlockFace.UP,BlockFace.SOUTH
    };

    //STATIC METHODS
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

        Totem t = Totem.totems_.stream()
                .filter(tot -> tot.getEndCrystal().equals(crystal))
                .findFirst()
                .orElse(null);

        if (t != null) { return t; }

        PersistentDataContainer data = crystal.getPersistentDataContainer();
        int bluePrintId = data.get(blueprintIdKey, PersistentDataType.INTEGER);
        Location loc = crystal.getLocation();

        t = new Totem(loc.add(0,.5,0), Blueprint.get(bluePrintId));
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

        t.save();
        return t;
    }
    public static Totem[] getAll() {
        return Totem.totems_.toArray(new Totem[0]);
    }
    public static Totem[] loadAll(Stream<Entity> entities) {
        List<Totem> r = entities
                .filter(e -> e instanceof EnderCrystal)
                .map(e -> (EnderCrystal) e)
                .filter(Totem::isTotem)
                .map(Totem::fromEnderCrystal)
                .toList();
        r.forEach(t -> {
            if (t.getRegion().isEmpty()) { t.destroy(); } else {
                Landlords.getMainInstance().getLogger().fine("Loaded totem. Region Id: " + t.getRegion().get().getId() + ". Totem Id: " + t.getUniqueId().toString() + ".");
            }
        });
        return r.toArray(new Totem[0]);
    }
    public static Optional<Totem> get(String id) {
        return Totem.get(UUID.fromString(id));
    }
    public static Optional<Totem> get(UUID id) {
        return Totem.totems_.stream().filter(t -> t.getUniqueId().equals(id)).findFirst();
    }
    public static boolean exists(String id) {
        return Totem.get(id).isPresent();
    }
    public static boolean exists(UUID id) {
        return Totem.get(id).isPresent();
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
    private BoundingBox structuralBox_ = null;

    //CONSTRUCTORS
    public Totem(Location location, Blueprint blueprint){
        this.blueprint_ = blueprint;
        this.relocate(location);
    }

    //GETTERS
    public Blueprint getBlueprint() {
        return blueprint_;
    }
    public Location getLocation() {
        return location_.clone();
    }
    public Block getContainingBlock() {
        return this.location_.getBlock();
    }
    public World getWorld() {
        return this.location_.getWorld();
    }
    public boolean isEnabled() {
        return this.getRegion().map(Region::isEnabled).orElse(false);
    }
    public boolean isPlaced() {
        return placed_;
    }
    public int getLevel() {
        return this.leveled_;
    }
    public Optional<Region> getRegion() {
        if (this.region_ == null) {
            if (!this.placed_) { return Optional.empty(); }
            Optional<Region> r = Regions.get(this.regionId_);
            if (r.isEmpty()) { return r; }
            RegionData regionData = r.get().getDataContainer().get("totemId");
            if (regionData == null) { return Optional.empty(); }
            if (!regionData.getAsString().equals(this.getUniqueId().toString())) { return Optional.empty(); }
            this.region_ = r.get();
        }
        return Optional.of(this.region_);
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
    public BoundingBox getStructuralBox() {
        return new BoundingBox().copy(this.structuralBox_);
    }

    //SETTERS
    public void relocate(Location loc) {
        if (loc.getWorld() == null) {
            throw new NullPointerException("The 'World' parameter passed to a totem's constructor cannot be null.");
        }
        this.location_ = loc;
        this.structuralBox_ = this.blueprint_.getStructuralBox().shift(loc.clone().add(-.5,-.5,-.5));

        if (this.endCrystal_ == null) { return; }

        this.endCrystal_.teleport(loc.add(0,-.5,0));

        Optional<Region> r = this.getRegion();
        if (r.isEmpty()) { return; }
        RegionDataContainer dataContainer = r.get().getDataContainer();
        Block b = this.getContainingBlock();
        dataContainer.remove("totemLoc");
        dataContainer.add(new RegionData("totemLoc", new int[] {b.getX(), b.getY(), b.getZ()}));
    }
    public void setEnabled(boolean isEnabled) {
        if (isEnabled == isEnabled()) { return; }
        this.getRegion().ifPresent(r -> { r.enabled(isEnabled); r.save();});
        (isEnabled ? Landlords.CONFIG.totemEnableParticleData : Landlords.CONFIG.totemDisableParticleData).spawn(this.getLocation(),1.3);
        this.getWorld().playSound(this.location_,isEnabled ? Sound.BLOCK_BEACON_ACTIVATE : Sound.BLOCK_CONDUIT_DEACTIVATE,1f,1.3f);
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
        this.endCrystal_ = (EnderCrystal) this.location_.getWorld().spawnEntity(this.getLocation().add(0,-.5,0), EntityType.ENDER_CRYSTAL);
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
        Landlords.CONFIG.totemEnableParticleData.spawn(this.location_,1.3);

        return this.region_;
    }
    public void feed(double howMuch) throws TotemUnresizableException {
        if (howMuch < 1) { return; }
        this.feedbackScale_(
                Arrays.stream(this.getBlueprint().getRegionGrowthRate()).map(d -> d * howMuch).toArray(),
                Landlords.CONFIG.totemFeedParticleData,
                Sound.BLOCK_RESPAWN_ANCHOR_CHARGE
        );
        this.leveled_ += howMuch;
    }
    public void hurt(double howMuch) throws TotemUnresizableException {
        howMuch = -Math.abs(howMuch);
        double[] i = new double[6];
        Arrays.fill(i,howMuch);
        this.feedbackScale_(i, Landlords.CONFIG.totemHurtParticleData, Sound.ENTITY_BLAZE_HURT);
        this.leveled_ += howMuch;
    }
    public void scale(double x1, double y1, double z1, double x2, double y2, double z2) throws TotemUnresizableException {
        this.scale(new double[] {x1,y1,z1,x2,y2,z2});
    }
    public void scale(double[] magnitudes) throws TotemUnresizableException {
        try {
            for (int i = 0; i < 3; i++) {
                this.directions_[i].expand(magnitudes[i], magnitudes[i + 3]);
            }
        } finally {
            Arrays.stream(this.directions_).forEach(Direction::resetCollisions);
            this.save();
        }
    }
    public TotemLectern getLecternAt(int x, int y, int z, World world) {

        if (!world.equals(this.getWorld())) { return null; }

        TotemLectern r = null;
        Block containingBlock = this.getContainingBlock();
        int[]   pos = {containingBlock.getX(), containingBlock.getY(), containingBlock.getZ()},
                blockPos = {x, y, z},
                lPos;

        for (TotemLectern l : this.getBlueprint().getLecterns()) {
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
        return this.getLecternAt(block.getX(), block.getY(), block.getZ(), block.getWorld());
    }
    public boolean contains(Vector vector, World world) {
        if (!this.getWorld().equals(world)) { return false; }
        if (this.structuralBox_ == null) { return false; }
        return this.structuralBox_.contains(vector);
    }
    public boolean contains(Block block) {
        return this.contains(new Vector(block.getX() + .5, block.getY() + .5 , block.getZ() + .5), block.getWorld());
    }
    public void save() {
        PersistentDataContainer endCrystalData = this.endCrystal_.getPersistentDataContainer();
        endCrystalData.set(Totem.isTotemKey, PersistentDataType.BYTE, (byte)1);
        endCrystalData.set(Totem.regionIdKey, PersistentDataType.INTEGER, this.regionId_);
        endCrystalData.set(Totem.blueprintIdKey, PersistentDataType.INTEGER, this.blueprint_.getId());
        endCrystalData.set(Totem.leveledKey, PersistentDataType.INTEGER, this.leveled_);
        this.getRegion().ifPresent(Region::save);
        if (!Totem.totems_.contains(this)) { Totem.totems_.add(this); }
    }
    public void destroy() {
        try {
            this.feedbackScale_(null,Landlords.CONFIG.totemHurtParticleData,Sound.ENTITY_BLAZE_HURT);
        } catch (Exception ignored) {}
        this.getWorld().createExplosion(this.endCrystal_.getLocation(), 6f,true,true);
        this.endCrystal_.remove();
        Totem.totems_.remove(this);
    }
    public void unload() {
        Totem.totems_.remove(this);
        Landlords.getMainInstance().getLogger().fine("Unloaded totem. Region Id: " + this.getRegionId() + ". Totem Id: " + this.getUniqueId().toString() + ".");
    }

    //PRIVATE METHODS
    private void feedbackScale_(double[] magnitudes, Config.ParticleData particleData, Sound sound) throws TotemUnresizableException {
        TotemUnresizableException exception = null;
        try {
            if (magnitudes != null) { this.scale(magnitudes); }
        } catch (TotemUnresizableException e) { exception = e; }
        if (!(exception == null || exception.wasResized())) { throw exception; }
        this.getWorld().playSound(this.location_, sound,SoundCategory.AMBIENT,3f, 0.3f);
        particleData.spawn(this.location_,6);
        if (exception != null) { throw exception; }
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
            Region reg = Totem.this.getRegion().orElse(null);
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
            boolean resized = false;
            try {
                this.min_.expand(toExpandMin);
                this.max_.expand(toExpandMax);
            } catch (TotemUnresizableException e) {
                resized = e.wasResized();
                leftOverMin += Math.abs(e.getExceeded(this.min_.getBlocFace()));
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
                if (resized) { e.announceResizing(); }
                throw e;
            }
            toExpand = leftOver / availableDirections.size();
            for (Direction d : availableDirections) {
                try {
                    d.expand(toExpand/2, toExpand/2);
                } catch (TotemUnresizableException e) {
                    e.addExceeded(leftOverMin,leftOverMax,this.axis_);
                    e.addReasons(innerReasons);
                    e.addReason(reason);
                    if (resized) { e.announceResizing(); }
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
            Region reg = Totem.this.getRegion().orElse(null);
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
                TotemUnresizableException e = new TotemUnresizableException(Totem.this, leftOver, reason);
                if (leftOver != amount) { e.announceResizing(); }
                throw e;
            } else {
                try {
                    opposite.expand(leftOver);
                } catch (TotemUnresizableException e) {
                    e.addReason(new RegionCollisionUnresizableReason(collidingWith,this.getBlocFace()));
                    e.addExceeded(leftOver, this.getBlocFace());
                    if (leftOver != amount) { e.announceResizing(); }
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