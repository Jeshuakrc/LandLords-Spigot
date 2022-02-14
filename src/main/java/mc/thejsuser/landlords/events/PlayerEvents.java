package mc.thejsuser.landlords.events;

import mc.thejsuser.landlords.regions.Ability;
import mc.thejsuser.landlords.Landlords;
import mc.thejsuser.landlords.io.ConfigManager;
import mc.thejsuser.landlords.io.LangManager;
import mc.thejsuser.landlords.regions.Region;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;

public class PlayerEvents implements Listener {
    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent e) {
        Player player = e.getPlayer();
        PlayerTeleportEvent.TeleportCause cause = e.getCause();
        if (cause.equals(PlayerTeleportEvent.TeleportCause.ENDER_PEARL) || cause.equals(PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT)) {
            Landlords.Utils.handleEvent(e,player,e.getTo(), Ability.can_teleport_in);
        }
    }

    @EventHandler
    public void onPlayerMovement(PlayerMoveEvent e) {

    }

    //RUnnables
    public static BukkitRunnable TitleDisplayer = new BukkitRunnable() {

        private HashMap<Player,List<Region>> prevRegions = new HashMap<>();
        private ConfigManager.TitleData titleConfig = ConfigManager.getRegionTitleDisplayData();

        @Override
        public void run() {
             Collection<? extends Player> players = Landlords.getMainInstance().getServer().getOnlinePlayers();

            for (Player player : players) {
                List<Region> regions = Arrays.asList(Region.getFromPoint(player.getLocation()));
                for (Region region : regions) {
                    if (!prevRegions.containsKey(player)) { return; }
                    if (!prevRegions.get(player).contains(region)) {

                        String mainOwner = (region.getPermissions().length < 1) ? "" : region.getPermissions()[0].getPlayerName();

                        player.sendTitle(
                                LangManager.getString("region_enter_title",player,region.getName(),mainOwner),
                                LangManager.getString("region_enter_title_subtitle",player,region.getName(),mainOwner),
                                titleConfig.fadeIn(),
                                titleConfig.stay(),
                                titleConfig.fadeOut()
                        );
                    }
                }
                prevRegions.put(player,regions);
            }
        }
    };
}
