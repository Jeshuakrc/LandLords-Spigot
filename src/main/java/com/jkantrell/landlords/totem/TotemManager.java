package com.jkantrell.landlords.totem;

import com.jkantrell.landlords.io.Serializer;
import org.bukkit.Location;

import java.util.List;

public abstract class TotemManager {

    private static List<Blueprint> structures_ = null;

    public static List<Blueprint> loadTotemStructures(){
        structures_ = Serializer.deserializeFileList(Serializer.FILES.BLUEPRINTS, Blueprint.class);
        return structures_;
    }
    public static List<Blueprint> getTotemStructures(){
        if(structures_==null){
            loadTotemStructures();
        }
        return structures_;
    }

    public static Blueprint chekStructuresFromPoint(Location location) {

        Blueprint r = null;
        for (Blueprint s : getTotemStructures()){
            if(s.testStructure(location)){
                r=s;
                break;
            }
        }
        return r;
    }
}
