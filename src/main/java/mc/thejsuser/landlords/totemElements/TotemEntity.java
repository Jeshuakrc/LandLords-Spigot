package mc.thejsuser.landlords.totemElements;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public class TotemEntity extends TotemElement<EntityType>{

    //CONSTRUCTORS
    public TotemEntity(EntityType entity, int x, int y, int z, TotemStructure structure) {
        super(entity, x, y, z, structure);
    }
}
