package mc.thejsuser.landlords.totemElements;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public interface TotemElement extends TotemRelative {

    int[] getPosition();
    void setPosition(int x, int y, int z);
    Material getBlockType();
    EntityType getEntityType();
    void setType(Material blockTye);
    void setType(EntityType entityType);

}
