package com.jkantrell.landlords.region;

import com.jkantrell.landlords.Landlords;
import com.jkantrell.landlords.io.Config;
import com.jkantrell.landlords.io.LangManager;
import com.jkantrell.regionslib.events.AbilityTriggeredEvent;
import com.jkantrell.regionslib.events.PlayerEnterRegionEvent;
import com.jkantrell.regionslib.events.PlayerLeaveRegionEvent;
import com.jkantrell.regionslib.regions.Region;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class LandlordsRegionListeners implements Listener {

    @EventHandler
    private void onPlayerEnterRegion(PlayerEnterRegionEvent e) {
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
    private void onPlayerEnterRegion(PlayerLeaveRegionEvent e) {
        //Nothing here
    }

    @EventHandler
    private void onAbilityTriggered(AbilityTriggeredEvent e) {
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
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
        } catch (NullPointerException ex) {
            Bukkit.getLogger().warning(String.format(
                    """
                            %1$s doesn't have the ability "%2$s" in the region "%3$s", but neither a specific nor default denial message was found in the "%4$s" lang file.
                            Make sure to include the "action_denied.%2$s" or "action_denier.default" entry in the lang file.""",
                    player.getName(), abilityName, regionName, LangManager.getLangFileName(player)
            ));
        }
    }
}
