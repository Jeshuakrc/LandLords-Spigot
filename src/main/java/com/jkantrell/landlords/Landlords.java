package com.jkantrell.landlords;

import com.jkantrell.landlords.io.Config;
import com.jkantrell.landlords.io.LangManager;
import com.jkantrell.landlords.totems.TotemEventListener;
import com.jkantrell.landlords.totems.TotemManager;
import com.jkantrell.regionslib.RegionsLib;
import com.jkantrell.regionslib.regions.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import javax.annotation.Nonnull;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class Landlords extends JavaPlugin {

    //FIELDS
    public static final Config CONFIG = new Config("");
    private static Landlords mainInstance;
    public static Landlords getMainInstance(){
        return mainInstance;
    }
    private final Logger LOGGER_ = new LandlordsLogger("LandLords",this.getServer().getLogger().getResourceBundleName(),this.getServer().getLogger());

    public static class RegionRules {
        public static final Rule.DataType<tntProtectedType> TNT_PROTECTED_DT = new Rule.EnumDataType<>(tntProtectedType.class);
    }

    public enum tntProtectedType { none, all, ignitor }

    @Override
    public void onEnable() {
        //Setting Main Instance
        mainInstance = this;

        Landlords.CONFIG.setFilePath(this.getDataFolder().getPath() + "/config.yml");
        try {
            Landlords.CONFIG.load();
        } catch (FileNotFoundException e) {
            this.saveResource("config.yml",true);
            this.onEnable();
            return;
        }
        this.getLogger().setLevel(Landlords.CONFIG.loggingLevel);

        //Initializing and loading files
        new Rule.Key("tntProtected", Landlords.RegionRules.TNT_PROTECTED_DT);
        this.getServer().getPluginManager().registerEvents(new TotemEventListener(),this);

        RegionsLib.configLocation = new String[] {"./plugins/Landlords/config.yml", "regions"};
        RegionsLib.enable(this);
        RegionsLib.getAbilityHandler().registerAll(LandlordsAbilities.class);
        TotemManager.loadTotemStructures();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    @Nonnull
    public Logger getLogger() {
        return this.LOGGER_;
    }

    private static class LandlordsLogger extends Logger {

        private final Logger logger_;

        protected LandlordsLogger(String name, String resourceBundleName, Logger logger) {
            super(name, resourceBundleName);
            this.logger_ = logger;
        }

        @Override
        public void log(LogRecord record){
            Level level = record.getLevel();
            if (level.intValue() < Level.INFO.intValue()) {
                record.setLevel(Level.INFO);
                String message = record.getMessage();
                record.setMessage("[" + level + "] " + message);
            }

            logger_.log(record);
        }
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

