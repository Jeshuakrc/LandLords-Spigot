package com.jkantrell.landlords.totems.Exception;

import com.jkantrell.landlords.totems.Totem;

public abstract class UnresizableReason {

    protected Totem totem;

    void setTotem(Totem totem) {
        this.totem = totem;
    }
    public Totem getTotem(){
        return this.totem;
    }

}
