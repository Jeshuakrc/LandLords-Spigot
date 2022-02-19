package com.jkantrell.landlords;

import com.jkantrell.landlords.io.ConfigManager;
import com.jkantrell.landlords.io.LangManager;
import com.jkantrell.regionslib.regions.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;

public final class Landlords extends JavaPlugin {

    private static Landlords mainInstance;
    public static Landlords getMainInstance(){
        return mainInstance;
    }
    public static class RegionRules {
        public static final Rule.DataType<tntProtectedType> TNT_PROTECTED_DT = new Rule.EnumDataType<>(tntProtectedType.class);
    }
    public enum tntProtectedType { none, all, ignitor }

    @Override
    public void onEnable() {
        //Setting Main Instance
        mainInstance = this;

        //Initializing and loading files
        new Rule.Key("tntProtected", Landlords.RegionRules.TNT_PROTECTED_DT);
        ConfigManager.initialize();
        Hierarchy.loadAll();
        Region.loadAll();

        //Registering events
        PluginManager pm = getServer().getPluginManager();
        try {
            LandlordsAbilities.registerAll();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
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
    }
}

