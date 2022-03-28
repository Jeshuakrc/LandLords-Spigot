package com.jkantrell.landlords.totems;

import com.google.gson.*;
import com.jkantrell.landlords.io.Serializer;
import com.jkantrell.regionslib.regions.Hierarchy;
import com.jkantrell.regionslib.regions.Rule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.BoundingBox;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class TotemStructure {

    private double[] regionInitialVertex_ = new double[6];
    private double[] regionGrowthRate_ = new double[6];
    private double[] regionMaxSize_ = new double[3];
    private int[] structureBox_;
    private Hierarchy hierarchy_;
    private List<Rule> rules_;

    public final Elements elements = new Elements(this);
    public final Lecterns lecterns = new Lecterns(this);

    //CONSTRUCTORS
    public TotemStructure(double[] regionBaseSize, double[] regionGrowthRate, double[] regionMaxSize, Hierarchy hierarchy){
        this.setRegionInitialVertex(regionBaseSize);
        this.setRegionGrowthRate(regionGrowthRate);
        this.setRegionMaxSize(regionMaxSize);
        this.setHierarchy(hierarchy);
    }

    //GETTERS
    public double[] getRegionInitialVertex(){
        return this.regionInitialVertex_;
    }
    public double[] getRegionGrowthRate(){
        return this.regionGrowthRate_;
    };
    public double[] getRegionMaxSize() {
        return this.regionMaxSize_;
    }
    int[] getStructureBox() {
        this.setStructureBox_();;
        return this.structureBox_;
    }
    public double[] getRegionInitialSize(){

        double[] r = new double[3];
        double[] vert = this.getRegionInitialVertex();
        for (int i = 0; i < 3; i++) {
            r[i] = Math.abs(vert[i] - vert[i+3]);
        }
        return r;
    }
    public Hierarchy getHierarchy() {
        return this.hierarchy_;
    }
    public Rule[] getRules() {
        return this.rules_.toArray(new Rule[0]);
    }

    //SETTERS
    public void setRegionInitialVertex(double[] baseSizeArray){
        regionInitialVertex_ = baseSizeArray;
    }
    public void setRegionGrowthRate(double[] growthRateArray){
        regionGrowthRate_ = growthRateArray;
    };
    public void setRegionMaxSize(double[] maxSizeArray) {
        regionMaxSize_ = maxSizeArray;
    }
    public void setHierarchy(Hierarchy hierarchy) {
        this.hierarchy_ = hierarchy;
    }
    public void setRules (List<Rule> rules) {
        this.rules_ = rules;
    }

    //PUBLIC METHODS
    public boolean chekStructureFromPoint(int x, int y, int z, World world){

        boolean r = true;
        for (TotemElement<?> i : this.elements){

            Block b = world.getBlockAt(
                    x + i.getPosition()[0],
                    y + i.getPosition()[1],
                    z + i.getPosition()[2]
                    );
            if(i instanceof TotemBlock){
                if(!b.getType().equals(i.getType())){
                    r=false;
                    break;
                }
            }
            if(i instanceof TotemEntity){
                BoundingBox bounding = new BoundingBox(
                        b.getX(),
                        b.getY(),
                        b.getZ(),
                        b.getX() + 1,
                        b.getY() + 1,
                        b.getZ() + 1
                        );
                Collection<Entity> entities = world.getNearbyEntities(bounding);
                boolean found = false;
                for (Entity e : entities){
                    if(e.getType().equals(i.getType())){
                        found = true;
                        break;
                    }
                }
                if(!found){
                    r=false;
                    break;
                }
            }
        }
        return r;
    }

    //PRIVATE METHODS
    private void setStructureBox_() {
        Elements elements = this.elements;

        int[] mins = elements.get(0).getPosition().clone();
        int[] maxs = elements.get(0).getPosition().clone();

        for (TotemElement<?> e : elements) {
            int[] pos = e.getPosition();
            for(int i = 0; i < 3; i++){
                maxs[i] = Math.max(maxs[i],pos[i]);
                mins[i] = Math.min(mins[i],pos[i]);
            }
        }

        structureBox_ = new int[6];
        for (int i = 0; i < 6 ; i++){
            if(i<3){
                structureBox_[i]=mins[i];
            }else{
                structureBox_[i]=maxs[i-3]+1;
            }
        }
    }

    //CLASSES
    public static class Elements extends ArrayList<TotemElement<?>> {
        private final TotemStructure structure_;

        private Elements(TotemStructure structure) {
            super();
            this.structure_ = structure;
        }

        @Override
        @Deprecated
        public boolean add(TotemElement<?> totemElement) {
            return super.add(totemElement);
        }

        public <T> boolean add (T type,int x, int y, int z) {

            TotemElement<?> element;
            if (type instanceof EntityType entityType) {
                element = new TotemEntity(entityType,x,y,z,structure_);
            } else if (type instanceof Material material) {
                if (!material.isBlock()) { return false; }
                element = new TotemBlock(material,x,y,z,structure_);
            } else { return false; }
            return super.add(element);
        }
    }
    public static class Lecterns extends ArrayList<TotemLectern> {
        private final TotemStructure structure_;

        private Lecterns(TotemStructure structure) {
            this.structure_ = structure;
        }
        @Override
        @Deprecated
        public boolean add(TotemLectern totemLectern) {
            return super.add(totemLectern);
        }

        public boolean add (int x, int y, int z, BlockFace facing) {
            return super.add(new TotemLectern(x,y,z,facing,structure_));
        }
    }

    public static class JDeserializer implements JsonDeserializer<TotemStructure> {

        @Override
        public TotemStructure deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

            JsonObject jsonStructure = json.getAsJsonObject();
            JsonObject jsonRegion = jsonStructure.get("region").getAsJsonObject();

            TotemStructure structure = new TotemStructure(
                    Serializer.GSON.fromJson(jsonRegion.get("initial_size"),double[].class),
                    Serializer.GSON.fromJson(jsonRegion.get("growth_rate"),double[].class),
                    Serializer.GSON.fromJson(jsonRegion.get("max_size"),double[].class),
                    Hierarchy.get(jsonRegion.get("hierarchy").getAsInt())
            );

            JsonObject jsonObject; TotemElement.ElementType type; int[] pos; String name;
            for (JsonElement elm : jsonStructure.get("elements").getAsJsonArray()) {
                jsonObject = elm.getAsJsonObject();
                type = TotemElement.ElementType.valueOf(jsonObject.get("type").getAsString());
                pos = Serializer.GSON.fromJson(jsonObject.get("position"),int[].class);
                name = jsonObject.get("name").getAsString();

                switch (type) {
                    case block -> structure.elements.add(
                            Material.valueOf(name),
                            pos[0], pos[1], pos [2]
                    );

                    default -> structure.elements.add(
                            EntityType.valueOf(name),
                            pos[0], pos[1], pos [2]
                    );
                }
            }
            BlockFace facing;
            for (JsonElement elm : jsonStructure.get("lecterns").getAsJsonArray()) {
                jsonObject = elm.getAsJsonObject();
                pos = Serializer.GSON.fromJson(jsonObject.get("position"),int[].class);
                facing = BlockFace.valueOf(jsonObject.get("direction").getAsString());

                structure.lecterns.add(pos[0],pos[1],pos[2],facing);
            }
            if (jsonRegion.has("rules")) {
                JsonObject jsonRules = jsonRegion.get("rules").getAsJsonObject();
                ArrayList<Rule> rules = new ArrayList<>();
                for (Map.Entry<String,JsonElement> entry : jsonRules.entrySet()) {
                    JsonObject jsonRule = new JsonObject();
                    jsonRule.add(entry.getKey(),entry.getValue());
                    rules.add(Serializer.GSON.fromJson(jsonRule,Rule.class));
                }
                structure.setRules(rules);
            }

            return structure;
        }
    }
}

