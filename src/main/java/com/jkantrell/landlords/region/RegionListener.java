package com.jkantrell.landlords.region;

import com.jkantrell.landlords.Landlords;
import com.jkantrell.landlords.io.Config;
import com.jkantrell.landlords.io.LangManager;
import com.jkantrell.regionslib.events.AbilityTriggeredEvent;
import com.jkantrell.regionslib.events.PlayerEnterRegionEvent;
import com.jkantrell.regionslib.events.PlayerLeaveRegionEvent;
import com.jkantrell.regionslib.regions.Region;
import com.jkantrell.regionslib.regions.abilities.Abilities;
import com.jkantrell.regionslib.regions.rules.Rule;
import com.jkantrell.regionslib.regions.rules.RuleDataType;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.raid.RaidTriggerEvent;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class RegionListener implements Listener {

    @EventHandler
    public void onPlayerEnterRegion(PlayerEnterRegionEvent e) {
        if (!Landlords.CONFIG.regionsNameTitleEnabled) { return; }

        Player player = e.getPlayer();
        Region region = e.getRegion();
        String mainOwner = (region.getPermissions().length < 1) ? "" : region.getPermissions()[0].getPlayerName();
        Config.TitleData titleData = Landlords.CONFIG.regionsNameTitleData;
        player.sendTitle(
                LangManager.getString("region_enter_title",player,region.getName(),mainOwner),
                LangManager.getString("region_enter_title_subtitle",player,region.getName(),mainOwner),
                titleData.fadeIn(),
                titleData.stay(),
                titleData.fadeOut()
        );
    }

    @EventHandler
    public void onPlayerLeaveRegion(PlayerLeaveRegionEvent e) {
        //Nothing here
    }

    @EventHandler
    public void onAbilityTriggered(AbilityTriggeredEvent e) {
        if (e.isAllowed()) { return; }

        Player player = e.getPlayer();
        String  regionName = e.getRegion().getName(),
                abilityName = e.getAbility().getName().toLowerCase();

        try {
            String  message,
                    path = "action_denied." + abilityName.toLowerCase();
            try {
                message = LangManager.getString(path, player, regionName);
            } catch (NullPointerException ex) {
                message = LangManager.getString("action_denied.default", player, regionName);
                Bukkit.getLogger().warning(String.format(
                        """
                            %1$s doesn't have the ability "%2$s" in the region "%3$s", but a specific denial message wasn't found in the "%4$s" lang file.
                            Displaying the default denied action message.
                            Include the "action_denied.%2$s" entry in the lang file to provide an specific message.""",
                        player.getName(), abilityName, regionName, LangManager.getLangFileName(player)
                ));
            }
            if (message.equals("")) { return; }
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
        } catch (NullPointerException ex) {
            Bukkit.getLogger().warning(String.format(
                    """
                            %1$s doesn't have the ability "%2$s" in the region "%3$s", but neither a specific nor default denial message was found in the "%4$s" lang file.
                            Make sure to include the "action_denied.%2$s" or "action_denier.default" entry in the lang file.""",
                    player.getName(), abilityName, regionName, LangManager.getLangFileName(player)
            ));
        }
    }

    @EventHandler
    public void onMonsterSpawn(CreatureSpawnEvent e) {
        if (!e.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.NATURAL)) { return; }
        if (!(e.getEntity() instanceof Monster monster)) { return; }

        for (Region r : Region.getRuleContainersAt("noMonsterSpawn",RuleDataType.BOOL,monster.getLocation())) {
            if (r.getRuleValue("noMonsterSpawn",RuleDataType.BOOL)) {
                e.setCancelled(true);
                Landlords.getMainInstance().getLogger().finest(
                monster.getName() + " prevented from spawning in " + r.getName() + " as 'noMonsterSpawn' rule is enabled in the region."
                );
                return;
            }
        }
    }

    @EventHandler
    public void onFireSpread(BlockBurnEvent e) {
        for (Region r :  Region.getRuleContainersAt("fireProtected",RuleDataType.BOOL,e.getBlock().getLocation().add(.5,.5,.5))) {
            if (r.getRuleValue("fireProtected",RuleDataType.BOOL)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onRaidStart(RaidTriggerEvent e) {
        for (Region r :  Region.getRuleContainersAt("raidProtected",RuleDataType.BOOL,e.getRaid().getLocation())) {
            if (r.getRuleValue("raidProtected",RuleDataType.BOOL)) {
                e.setCancelled(true);
                Landlords.getMainInstance().getLogger().finest(
                 "A raid was prevented from triggering in " + r.getName() + " as 'raidProtected' rule is enabled in the region."
                );
                return;
            }
        }
    }

    @EventHandler
    public void onExplosion(EntityExplodeEvent e) {
        boolean intercept = false;
        String ruleLabel = null;
        RuleDataType<?> dataType = null;
        BiPredicate<Rule, Region> predicate = null;

        if (e.getEntity() instanceof Creeper) {
            ruleLabel = "creeperProtected";
            dataType = RuleDataType.BOOL;
            predicate = (ru,re) -> ru.getValue(RuleDataType.BOOL);
            intercept = true;
        } else if (e.getEntity() instanceof TNTPrimed tnt) {
            ruleLabel = "tntProtected";
            dataType = Landlords.getMainInstance().getRuleKeys().TNT_PROTECTED.getDataType();
            predicate = (ru,re) -> {
                switch ((LandLordsRuleKeys.TntProtection) ru.getValue()) {
                    case ignitor -> {
                        Entity source = tnt.getSource();
                        if (source == null ) { return true; }
                        if (!(source instanceof Player p)) { return true; }
                        return !re.checkAbility(p, Abilities.IGNITE_TNT);
                    }
                    case all -> { return true; }
                    default -> { return false; }
                }
            };
            intercept = true;
        }

        if (!intercept) { return; }

        Iterator<Block> i = e.blockList().iterator();
        Block block;
        while (i.hasNext()) {
            block = i.next();
            Rule rule;
            for (Region r : Region.getRuleContainersAt(ruleLabel,dataType,block.getLocation().add(.5,.5,.5))) {
                rule = r.getRule(ruleLabel);
                if (predicate.test(rule,r)) { e.setCancelled(true); return; }
            }
        }
    }
}
