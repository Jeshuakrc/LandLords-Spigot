package com.jkantrell.landlords.totemElements;

import com.jkantrell.landlords.io.LangManager;
import com.jkantrell.landlords.regions.Permission;
import com.jkantrell.landlords.regions.Region;
import com.jkantrell.landlords.Landlords;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Lectern;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class TotemLectern implements TotemRelative, Cloneable {

    //FIELDS
    private int[] position_ = new int[3];
    private BlockFace facing_;
    private final TotemStructure structure_;
    private Totem totem_;

    //STATIC FIELDS
    private static NamespacedKey lecternRegionIDKey_ = new NamespacedKey(Landlords.getMainInstance(),"deedLecternRegionID");

    //CONSTRUCTORS
    public TotemLectern(int x, int y, int z, BlockFace facing, TotemStructure structure){
        this.setPosition(x,y,z);
        this.setFacing(facing);
        this.structure_ = structure;
    }

    //SETTERS
    void setPosition(int x, int y, int z) {
        position_[0]=x;
        position_[1]=y;
        position_[2]=z;
    }
    void setFacing(BlockFace face) {
        facing_= face;
    }
    void setTotem(Totem totem) {
        this.totem_ = totem;
    }

    //GETTERS
    public int[] getPosition() {
        return position_;
    }
    public BlockFace getFacing() {
        return facing_;
    }
    public TotemStructure getStructure() {
        return this.structure_;
    }
    public Totem getTotem() {
        return this.totem_;
    }

    //METHODS
    public void convert(Block block) {

        if (block.getState() instanceof Lectern lectern) {
            PersistentDataContainer data = lectern.getPersistentDataContainer();
            data.set(lecternRegionIDKey_, PersistentDataType.INTEGER, this.getTotem().getRegionId());
            lectern.update();
        }

    }
    public Deeds.ReadingResults readDeeds(ItemStack itemStack, Player player) {
        if (!Deeds.isTotemDeeds(itemStack)) { throw new IllegalArgumentException(); }

        Deeds deeds = Deeds.getFromBook(itemStack, player);
        Region deedsRegion = deeds.getRegion(), lecternRegion = this.getTotem().getRegion();
        if (!deedsRegion.equals(lecternRegion)) {
            throw new IllegalArgumentException(LangManager.getString("deeds_placing_error_regionsMismatch",player,deedsRegion.getName(),lecternRegion.getName()));
        }
        Integer deedsID = deeds.getId(), regionID = lecternRegion.getDataContainer().get(Deeds.deedsIdContainerKey).getAsInt();
        if (!deedsID.equals(regionID)) {
            throw new IllegalArgumentException(LangManager.getString("deeds_placing_error_idMismatch",player,deedsID.toString(),regionID.toString()));
        }

        Deeds.ReadingResults read = deeds.read();
        if(!read.errors().isEmpty()){
            throw new unreadableDeedsException(LangManager.getString("deeds_placing_error_readingErrors",player),read.errors());
        }

        if (read.name() != null) {
            lecternRegion.setName(read.name());
        }
        lecternRegion.setPermissions(read.permissions().toArray(new Permission[0]));
        lecternRegion.save();

        return read;
    }
    public static boolean isTotemLectern(Block block) {
        if (block.getState() instanceof Lectern lectern) {
            return lectern.getPersistentDataContainer().has(lecternRegionIDKey_,PersistentDataType.INTEGER);
        } else { return false; }
    }
    @Override
    public TotemLectern clone() {
        try {
            TotemLectern clone = (TotemLectern) super.clone();
            // TODO: copy mutable state here, so the clone can't change the internals of the original
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    //EXCEPTIONS
    public class unreadableDeedsException extends RuntimeException {

        public final List<String> errors;

        public unreadableDeedsException(List<String> errors) {
            super();
            this.errors = errors;
        }

        public unreadableDeedsException(String message, List<String> errors) {
            super(message);
            this.errors = errors;
        }
    }
}
