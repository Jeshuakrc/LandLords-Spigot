package com.jkantrell.landlords.totem;

import org.bukkit.util.Vector;

public abstract class TotemElement<T> implements TotemRelative {

    private final Blueprint blueprint_;
    private final T type_;
    private final Vector pos_;

    public TotemElement(T type, Vector position, Blueprint blueprint) {
        this.blueprint_ = blueprint;
        this.type_ = type;
        this.pos_ = position;
    }
    public TotemElement (T type, int x, int y, int z, Blueprint blueprint) {
        this(type, new Vector(x,y,z), blueprint);
    }
    public Vector getPosition() {
        return this.pos_;
    }
    T getType() {
        return this.type_;
    };

    @Override
    public Blueprint getStructure() {
        return this.blueprint_;
    }

    public enum ElementType { block , entity }
}
