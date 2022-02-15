package mc.thejsuser.landlords.totemElements;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public class TotemBlock extends TotemElement<Material>{

    public TotemBlock(Material type, int x, int y, int z, TotemStructure structure) {
        super(type, x, y, z, structure);
    }
}
