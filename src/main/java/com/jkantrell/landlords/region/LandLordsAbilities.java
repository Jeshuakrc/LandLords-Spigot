package com.jkantrell.landlords.region;

import com.jkantrell.landlords.Landlords;
import com.jkantrell.landlords.event.TotemDestroyedByPlayerEvent;
import com.jkantrell.landlords.totems.TotemLectern;
import com.jkantrell.regionslib.events.BlockRightClickedEvent;
import com.jkantrell.regionslib.regions.abilities.Ability;
import com.jkantrell.regionslib.regions.abilities.AbilityRegistration;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;

public final class LandLordsAbilities {

    @AbilityRegistration
    public static final Ability<BlockRightClickedEvent>
    PRESS_ENFORCED_BUTTONS = new Ability<>(
            com.jkantrell.regionslib.regions.abilities.Abilities.PRESS_BUTTONS,
            (e) -> Landlords.CONFIG.regionsEnforcedButtons.contains(e.getBlock().getType())
    ).extend(com.jkantrell.regionslib.regions.abilities.Abilities.PRESS_BUTTONS),

    PULL_LOCKED_LEVERS = new Ability<>(
            com.jkantrell.regionslib.regions.abilities.Abilities.PULL_LEVERS,
            (e) -> {
                BlockData block = e.getBlock().getBlockData();
                Directional dir = (Directional) block;
                FaceAttachable fa = (FaceAttachable) block;
                BlockFace face = switch (fa.getAttachedFace()) {
                    case FLOOR -> BlockFace.DOWN;
                    case CEILING -> BlockFace.UP;
                    default -> switch (dir.getFacing()) {
                        case NORTH -> BlockFace.SOUTH;
                        case EAST -> BlockFace.WEST;
                        case SOUTH -> BlockFace.NORTH;
                        default ->  BlockFace.EAST;
                    };
                };
                return Landlords.CONFIG.regionsLeverLockerBlocks.contains(e.getBlock().getRelative(face).getType());
            }
    ).extend(com.jkantrell.regionslib.regions.abilities.Abilities.PULL_LEVERS),

    ACCESS_TOTEM_LECTERNS = new Ability<>(
            com.jkantrell.regionslib.regions.abilities.Abilities.ACCESS_LECTERNS,
            e -> TotemLectern.isTotemLectern(e.getBlock())
    ).extend(com.jkantrell.regionslib.regions.abilities.Abilities.ACCESS_LECTERNS),

    PUT_BOOKS_ON_TOTEM_LECTERNS = new Ability<>(
            com.jkantrell.regionslib.regions.abilities.Abilities.PUT_BOOKS_ON_LECTERNS,
            e -> TotemLectern.isTotemLectern(e.getBlock())
    ).extend(com.jkantrell.regionslib.regions.abilities.Abilities.PUT_BOOKS_ON_LECTERNS);

    @AbilityRegistration
    public static final Ability<PlayerTakeLecternBookEvent> TAKE_BOOKS_FROM_TOTEM_LECTERNS = new Ability<>(
            com.jkantrell.regionslib.regions.abilities.Abilities.TAKE_BOOKS_FROM_LECTERNS,
            e -> TotemLectern.isTotemLectern(e.getLectern().getBlock())
    ).extend(com.jkantrell.regionslib.regions.abilities.Abilities.TAKE_BOOKS_FROM_LECTERNS);

    @AbilityRegistration
    public static final Ability<TotemDestroyedByPlayerEvent> DESTROY_TOTEMS = new Ability<>(
            TotemDestroyedByPlayerEvent.class,
            e -> true,
            TotemDestroyedByPlayerEvent::getDestroyer,
            e -> e.getTotem().getLocation()
    );
}
