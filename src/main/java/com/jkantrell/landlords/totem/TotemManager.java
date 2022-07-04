package com.jkantrell.landlords.totem;

import com.jkantrell.landlords.io.Serializer;
import com.jkantrell.regionslib.regions.Region;
import com.jkantrell.regionslib.regions.dataContainers.RegionData;
import com.jkantrell.landlords.Landlords;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public abstract class TotemManager {

    private static List<Blueprint> structures_ = null;
    private static ArrayList<Totem> totems_ = new ArrayList<Totem>();

    public static List<Blueprint> loadTotemStructures(){
        structures_ = Serializer.deserializeFileList(Serializer.FILES.TOTEM_STRUCTURES, Blueprint.class);
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
            if(s.chekStructureFromPoint(location)){
                r=s;
                break;
            }
        }
        return r;
    }

    public static void registerTotem(Totem totem) {
        if (!totems_.contains(totem)) {
            totems_.add(totem);
        }
    }
    public static void unregisterTotem(Totem totem) {
        TotemManager.totems_.remove(totem);
    }
    public static void removeTotem(Totem totem){
        totems_.remove(totem);
    }
}
