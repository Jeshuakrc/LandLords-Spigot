package com.jkantrell.landlords.totem;

import org.bukkit.entity.EntityType;

public class TotemEntity extends TotemElement<EntityType>{

    //CONSTRUCTORS
    public TotemEntity(EntityType entity, int x, int y, int z, Blueprint structure) {
        super(entity, x, y, z, structure);
    }
}
