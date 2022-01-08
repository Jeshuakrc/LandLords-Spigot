package mc.thejsuser.landlords.totemElements;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public class TotemBlock implements TotemElement{

    private Material block_;
    private int[] position_ = new int[3];
    private TotemStructure structure_;

    //CONSTRUCTORS
    public TotemBlock(Material block, int x, int y, int z) {
        setType(block);
        setPosition(x,y,z);
    }

    //GETTERS
    @Override
    public int[] getPosition() {
        return position_;
    }
    @Override
    public Material getBlockType() {
        return block_;
    }
    @Override
    public EntityType getEntityType() {
        return null;
    }
    @Override
    public TotemStructure getStructure(){
        return this.structure_;
    }

    //SETTERS
    @Override
    public void setType(Material block){
        block_=block;
    }
    @Override
    public void setPosition(int x, int y, int z){
        position_[0]=x;
        position_[1]=y;
        position_[2]=z;
    }
    @Override
    public void setType(EntityType entityType) {

    }
    @Override
    public void setStructure(TotemStructure structure){
        this.structure_ = structure;
    }



}
