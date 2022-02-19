package com.jkantrell.landlords.totems;

public abstract class TotemElement<T> implements TotemRelative {

    private final TotemStructure structure_;
    private final T type_;
    private int[] pos_;
    public TotemElement (T type, int x, int y, int z, TotemStructure structure) {
        this.type_ = type;
        this.pos_ = new int[] {x,y,z};
        this.structure_ = structure;
    }
    public int[] getPosition() {
        return this.pos_;
    };
    public void setPosition(int x, int y, int z) {
        this.pos_ = new int[] {x,y,z};
    };
    T getType() {
        return this.type_;
    };

    @Override
    public TotemStructure getStructure() {
        return this.structure_;
    }

    public enum ElementType { block , entity }
}
