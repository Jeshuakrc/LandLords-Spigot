package com.jkantrell.landlords.totems;

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
    public static List<Totem> getTotems(){
        return List.copyOf(totems_);
    }
    public static Totem getTotemFromEndCrystal(EnderCrystal enderCrystal){

        Totem t = null;
        for (Totem i : getTotems()){
            if(i.getEndCrystal().equals(enderCrystal)){
                t = i;
                break;
            }
        }
        return t;
    }
    public static Totem getTotemFromId(int id){

        Totem t = null;

        for (Totem i : getTotems()){
            if (i.getRegionId() == id) {
                t = i;
                break;
            }
        }
        return t;
    }
    public static List<Totem> loadTotems() {

        List<World> worlds = Landlords.getMainInstance().getServer().getWorlds();
        totems_.clear();
        for(World w : worlds){

            for (Entity e : w.getEntities()) {
                if(e.getType().equals(EntityType.ENDER_CRYSTAL)){

                    EnderCrystal crystal = (EnderCrystal) e;
                    if(Totem.isTotem(crystal)){
                        totems_.add(Totem.fromEnderCrystal(crystal));
                    }
                }
            }
        }
        //removeOrphanedRegions(); //Causing regions in non-generated terrain to be deleted
        return totems_;
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
    public static TotemLectern getLecternAtSpot(Block block) {

        TotemLectern r = null;
        for (Totem t : TotemManager.getTotems()){
            r = t.getLecternAt(block);
            if (r != null) { break; }
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
    public static void removeOrphanedRegions(Stream<Region> regions) {
        regions
                .filter(r -> {
                    RegionData data = r.getDataContainer().get("totemRegion");
                    if (data == null) { return false; }
                    return data.getAsBoolean();
                })
                .filter(r -> TotemManager.getTotemFromId(r.getId()) == null)
                .forEach(Region::destroy);
    }
}
