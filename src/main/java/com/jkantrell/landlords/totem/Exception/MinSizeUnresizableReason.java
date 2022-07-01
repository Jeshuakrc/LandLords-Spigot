package com.jkantrell.landlords.totem.Exception;

import org.bukkit.Axis;

public class MinSizeUnresizableReason extends MaxSizeUnresizableReason {
    public MinSizeUnresizableReason(Axis direction) {
        super(direction);
    }
}
