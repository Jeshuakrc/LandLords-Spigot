package com.jkantrell.landlords.totems.Exception;

import org.bukkit.block.BlockFace;
import org.bukkit.util.BoundingBox;

public class OverShrunkUnresizableReason extends OneDirectionalUnresizableReason {

    //CONSTRUCTOR
    public OverShrunkUnresizableReason(BlockFace direction) {
        super(direction);
    }

    //GETTERS
    BoundingBox getBaseSize() {
        if (this.totem == null) { return null; }
        return this.totem.getBaseRegionBox();
    }
}
