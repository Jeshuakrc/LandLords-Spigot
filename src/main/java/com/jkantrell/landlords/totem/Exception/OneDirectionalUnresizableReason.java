package com.jkantrell.landlords.totem.Exception;

import org.bukkit.block.BlockFace;

public abstract class OneDirectionalUnresizableReason extends UnresizableReason {

    //FIELDS
    protected BlockFace direction;

    //CONSTRUCTOR
    public OneDirectionalUnresizableReason(BlockFace direction) {
        this.direction = direction;
    }

    //GETTERS
    public BlockFace getDirection() {
        return this.direction;
    }
}
