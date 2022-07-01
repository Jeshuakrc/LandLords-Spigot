package com.jkantrell.landlords.totem.Exception;

import com.jkantrell.regionslib.regions.Region;
import org.bukkit.block.BlockFace;

public class RegionCollisionUnresizableReason extends OneDirectionalUnresizableReason {

    //FIELDS
    private final Region collidingRegion_;

    //CONSTRUCTOR
    public RegionCollisionUnresizableReason(Region whichWith, BlockFace towards) {
        super(towards);
        if (!towards.isCartesian()) {
            throw new IllegalArgumentException("The provided BlockFace must be cartesian (NORTH, SOUTH, EAST, WEST, UP, DOWN).");
        }
        this.collidingRegion_ = whichWith;
    }

    //GETTERS
    public Region getCollidingRegion() {
        return this.collidingRegion_;
    }
}
