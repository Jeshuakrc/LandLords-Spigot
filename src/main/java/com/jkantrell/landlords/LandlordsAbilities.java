package com.jkantrell.landlords;

import com.jkantrell.regionslib.RegionsLib;
import com.jkantrell.regionslib.regions.abilities.Ability;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;

import java.lang.reflect.Field;

public class LandlordsAbilities {

    public final static Ability<BlockBreakEvent> BLOCK_BREAK = new Ability<>(
            "block_break",
            e -> true,
            BlockBreakEvent::getPlayer,
            e -> e.getBlock().getLocation().add(.5,.5,.5)
            );

    static void registerAll() throws IllegalAccessException {
        for (Field field : LandlordsAbilities.class.getFields()) {
            Ability<Event> ability = (Ability<Event>) field.get(null);
            if (field.getGenericType() instanceof Class<?> clazz) {
                Class<Event> eventClass = (Class<Event>) clazz;
                RegionsLib.getAbilityHandler().register(ability,eventClass);
            }
        }
    }

}
