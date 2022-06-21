package com.jkantrell.landlords.totems;

import org.bukkit.Material;

public class TotemBlock extends TotemElement<Material>{

    public TotemBlock(Material type, int x, int y, int z, Blueprint structure) {
        super(type, x, y, z, structure);
    }
}
