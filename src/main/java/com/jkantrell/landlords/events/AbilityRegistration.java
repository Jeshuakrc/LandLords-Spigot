package com.jkantrell.landlords.events;

import com.jkantrell.landlords.oldRegions.ablt_;
import com.jkantrell.regionslib.RegionsLib;
import com.jkantrell.regionslib.regions.abilities.Ability;
import com.jkantrell.regionslib.regions.abilities.AbilityHandler;
import net.md_5.bungee.chat.SelectorComponentSerializer;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;

import java.lang.reflect.Field;

public class AbilityRegistration {

    public final static Ability<BlockBreakEvent> BLOCK_BREAK = new Ability<>(
            "block_break",
            e -> true,
            BlockBreakEvent::getPlayer,
            e -> e.getBlock().getLocation().add(.5,.5,.5)
            );

    static void registerAll() throws IllegalAccessException {
        for (Field field : AbilityRegistration.class.getFields()) {
            Ability<Event> ability = (Ability<Event>) field.get(null);
            if (field.getGenericType() instanceof Class<?> clazz) {
                Class<Event> eventClass = (Class<Event>) clazz;
                RegionsLib.getAbilityHandler().register(ability,eventClass);
            }
        }
    }

}
