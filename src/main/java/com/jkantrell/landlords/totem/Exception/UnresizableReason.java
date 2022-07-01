package com.jkantrell.landlords.totem.Exception;

import com.jkantrell.landlords.totem.Totem;

public abstract class UnresizableReason {

    protected Totem totem;

    void setTotem(Totem totem) {
        this.totem = totem;
    }
    public Totem getTotem(){
        return this.totem;
    }

}
