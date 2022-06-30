package com.jkantrell.landlords.totems.Exception;

import org.bukkit.Axis;

public class MinSizeUnresizableReason extends MaxSizeUnresizableReason {
    public MinSizeUnresizableReason(Axis direction) {
        super(direction);
    }
}
