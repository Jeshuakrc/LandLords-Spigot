package com.jkantrell.landlords;

import com.jkantrell.regionslib.RegionsLib;
import com.jkantrell.regionslib.regions.abilities.Ability;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;

public class LandlordsAbilities {

    public final static Ability<BlockBreakEvent> BLOCK_BREAK = new Ability<>(
            BlockBreakEvent.class,
            "block_break",
            e -> true,
            BlockBreakEvent::getPlayer,
            e -> e.getBlock().getLocation().add(.5,.5,.5)
            );

    static void registerAll() throws IllegalAccessException {
        for (Field field : LandlordsAbilities.class.getFields()) {
            ((Ability<?>) field.get(null)).register();
        }
    }

}
