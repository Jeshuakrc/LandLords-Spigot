package mc.thejsuser.landlords.totemElements;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;

import java.util.Collection;
import java.util.List;

public class TotemStructure {

    private TotemElement[] elements_;
    private TotemLectern[] lecterns_;
    private double[] regionInitialVertex_ = new double[6];
    private double[] regionGrowthRate_ = new double[6];
    private double[] regionMaxSize_ = new double[3];
    private int[] structureBox_;

    //CONSTRUCTORS
    public TotemStructure(TotemElement[] elements, TotemLectern[] lecterns, double[] regionBaseSize, double[] regionGrowthRate, double[] regionMaxSize){
        for (TotemElement e : elements){
            e.setStructure(this);
        }
        for (TotemLectern l : lecterns){
            l.setStructure(this);
        }

        setElements(elements);
        setLecterns(lecterns);
        setRegionInitialVertex(regionBaseSize);
        setRegionGrowthRate(regionGrowthRate);
        setRegionMaxSize(regionMaxSize);
    }

    //GETTERS
    public TotemElement[] getElements(){
        return this.elements_;
    }
    public TotemLectern[] getLecterns(){
        return this.lecterns_;
    }
    public double[] getRegionInitialVertex(){
        return this.regionInitialVertex_;
    }
    public double[] getRegionGrowthRate(){
        return this.regionGrowthRate_;
    };
    public double[] getRegionMaxSize() {
        return this.regionMaxSize_;
    }
    int[] getStructureBox(){return structureBox_;}
    public double[] getRegionInitialSize(){

        double[] r = new double[3];
        double[] vert = this.getRegionInitialVertex();
        for (int i = 0; i < 3; i++) {
            r[i] = Math.abs(vert[i] - vert[i+3]);
        }
        return r;
    }

    //SETTERS
    public void setElements(TotemElement[] elements){
        elements_=elements;
        this.setStructureBox_();
    }
    public void setLecterns(TotemLectern[] lecterns){
        lecterns_=lecterns;
    }
    public void setRegionInitialVertex(double[] baseSizeArray){
        regionInitialVertex_ = baseSizeArray;
    }
    public void setRegionGrowthRate(double[] growthRateArray){
        regionGrowthRate_ = growthRateArray;
    };
    public void setRegionMaxSize(double[] maxSizeArray) {
        regionMaxSize_ = maxSizeArray;
    }

    //PUBLIC METHODS
    public boolean chekStructureFromPoint(int x, int y, int z, World world){

        boolean r = true;
        for (TotemElement i : elements_){

            Block b = world.getBlockAt(
                    x + i.getPosition()[0],
                    y + i.getPosition()[1],
                    z + i.getPosition()[2]
                    );
            if(i instanceof TotemBlock){
                if(!b.getType().equals(i.getBlockType())){
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
                    if(e.getType().equals(i.getEntityType())){
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
        TotemElement[] elements = this.elements_;

        int[] mins = elements[0].getPosition().clone();
        int[] maxs = elements[0].getPosition().clone();

        for (TotemElement e : elements) {
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
}
