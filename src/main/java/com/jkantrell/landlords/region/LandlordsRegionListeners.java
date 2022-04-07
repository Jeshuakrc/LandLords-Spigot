package com.jkantrell.landlords.region;

import com.jkantrell.landlords.Landlords;
import com.jkantrell.landlords.io.Config;
import com.jkantrell.landlords.io.LangManager;
import com.jkantrell.regionslib.events.PlayerEnterRegionEvent;
import com.jkantrell.regionslib.events.PlayerLeaveRegionEvent;
import com.jkantrell.regionslib.regions.Region;
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
}
