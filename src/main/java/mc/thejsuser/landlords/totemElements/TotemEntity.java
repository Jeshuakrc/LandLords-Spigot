package mc.thejsuser.landlords.totemElements;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public class TotemEntity implements TotemElement{
    private EntityType entity_;
    private int[] position_ = new int[3];
    private TotemStructure structure_;

    //CONSTRUCTORS
    public TotemEntity(EntityType entity, int x, int y, int z) {
        setType(entity);
        setPosition(x,y,z);
    }

    //GETTERS
    @Override
    public EntityType getEntityType() {
        return entity_;
    }
    @Override
    public int[] getPosition() {
        return position_;
    }
    @Override
    public Material getBlockType() {
        return null;
    }
    @Override
    public TotemStructure getStructure(){
        return this.structure_;
    }

    //SETTERS
    public void setPosition(int x, int y, int z){
        position_[0]=x;
        position_[1]=y;
        position_[2]=z;
    }
    @Override
    public void setType(Material blockTye) {

    }
    @Override
    public void setType(EntityType entityType) {
        entity_=entityType;
    }
    @Override
    public void setStructure(TotemStructure structure) {
        this.structure_ = structure;
    }

}
