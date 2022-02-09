package mc.thejsuser.landlords;

import mc.thejsuser.landlords.io.LangManager;
import mc.thejsuser.landlords.regionElements.Ability;
import mc.thejsuser.landlords.regionElements.Hierarchy;
import mc.thejsuser.landlords.regionElements.Region;
import mc.thejsuser.landlords.events.*;
import mc.thejsuser.landlords.io.ConfigManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Collection;

public final class Landlords extends JavaPlugin {

    private static Landlords mainInstance;
    public static Landlords getMainInstance(){
        return mainInstance;
    }

    @Override
    public void onEnable() {
        //Setting Main Instance
        mainInstance = this;

        //Initializing and loading files
        ConfigManager.initialize();
        Hierarchy.loadHierarchies();
        Region.loadRegions();

        //Registering events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new BlockEvents(), this);
        pm.registerEvents(new ContainerEvents(), this);
        pm.registerEvents(new RedstoneEvents(), this);
        pm.registerEvents(new EntityEvents(), this);
        pm.registerEvents(new TotemEvents(), this);
        pm.registerEvents(new PlayerEvents(), this);

        //Initialize regionNameDisplayer
        if (ConfigManager.getRegionTitleDisplayEnabled()) {
            PlayerEvents.TitleDisplayer.runTaskTimerAsynchronously(this, 0, ConfigManager.getRegionTitleDisplayRefreshRate());
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static class Utils {
        public static void broadcastMessageLang(String path, String[] args, Collection<? extends Player> players) {
            for (Player player : players) {
                assert player != null;
                player.sendMessage(LangManager.getString(path, player, args));
            }
        }

        public static void broadcastMessageLang(String path, String[] args) {
            broadcastMessageLang(path, args, Landlords.getMainInstance().getServer().getOnlinePlayers());
        }


        public static boolean handleEvent(Cancellable event, Player player, Location location, Ability ability) {
            Region[] regions = Region.getFromPoint(location);
            if (regions.length < 1) {
                return true;
            }
            boolean a = Region.checkAbilityInRegions(regions, player, ability);
            if (!a) {
                event.setCancelled(true);

                if (player != null) {
                    String regionName = regions[0].getName();
                    try {
                        String  message,
                                path = "action_denied." + ability.toString();
                        try {
                            message = LangManager.getString(path, player, regionName);
                        } catch (NullPointerException e) {
                            message = LangManager.getString("action_denied.default", player, regionName);
                            Bukkit.getLogger().warning(String.format(
                                    """
                                    %1$s doesn't have the ability "%2$s" in the region "%3$s", but a specific denial message wasn't found in the "%4$s" lang file.
                                    Displaying the default denied action message.
                                    Include the "action_denied.%2$s" entry in the lang file to provide an specific message.""",
                                    player.getName(),ability.toString(),regionName, LangManager.getLangFileName(player)
                            ));
                        }
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
                    } catch (NullPointerException e) {
                        Bukkit.getLogger().warning(String.format(
                                """
                                %1$s doesn't have the ability "%2$s" in the region "%3$s", but neither a specific nor default denial message was found in the "%4$s" lang file.
                                Make sure to include the "action_denied.%2$s" or "action_denier.default" entry in the lang file.""",
                                player.getName(),ability.toString(),regionName, LangManager.getLangFileName(player)
                        ));
                    }
                }
            }
            return a;
        }
    }
}

