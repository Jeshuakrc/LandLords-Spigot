package com.jkantrell.landlords.totems.Exception;

import org.bukkit.Axis;

public class MaxSizeUnresizableReason extends UnresizableReason {

    //FIELDS
    Axis axis_;

    //CONSTRUCTOR;
    public MaxSizeUnresizableReason(Axis direction) {
        this.axis_ = direction;
    }

    //GETTERS
    public Axis getAxis() {
        return this.axis_;
    }

}
