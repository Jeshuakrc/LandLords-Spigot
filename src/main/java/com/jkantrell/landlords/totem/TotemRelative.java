package com.jkantrell.landlords.totem;

import org.bukkit.util.Vector;

public interface TotemRelative {

    Vector getPosition();
    default int[] getBlockPositionArray() {
        Vector pos = this.getPosition();
        return new int[] {pos.getBlockX(),pos.getBlockY(),pos.getBlockZ()};
    }
    default int[] getAbsolutePosition(int x, int y, int z) {
        int[]   pos = getBlockPositionArray(),
                add = {x,y,z},
                r = new int[3];
        for (int i = 0; i < 3; i++) {
            r[i] = pos[i] + add[i];
        }
        return r;
    }
    Blueprint getStructure();
}
