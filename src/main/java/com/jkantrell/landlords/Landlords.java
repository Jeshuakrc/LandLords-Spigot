package com.jkantrell.landlords;

import com.jkantrell.landlords.io.Config;
import com.jkantrell.landlords.io.LangManager;
import com.jkantrell.landlords.region.LandLordsAbilities;
import com.jkantrell.landlords.region.LandLordsRuleKeys;
import com.jkantrell.landlords.region.RegionListener;
import com.jkantrell.landlords.totem.TotemListener;
import com.jkantrell.landlords.totem.TotemManager;
import com.jkantrell.regionslib.RegionsLib;
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
    private static Landlords mainInstance_;
    private final Logger LOGGER_ = new LandlordsLogger("LandLords",this.getServer().getLogger().getResourceBundleName(),this.getServer().getLogger());
    private LandLordsRuleKeys ruleKeys_;

    //STATIC METHODS
    public static Landlords getMainInstance(){
        return mainInstance_;
    }

    //PLUGIN EVENTS
    @Override
    public void onEnable() {
        //Setting Main Instance
        mainInstance_ = this;
        Landlords.CONFIG.setFilePath(this.getDataFolder().getPath() + "/config.yml");
        try {
            Landlords.CONFIG.load();
        } catch (FileNotFoundException e) {
            this.saveResource("config.yml",true);
            this.onEnable();
            return;
        }

        //Initializing RegionsLib
        RegionsLib.configLocation = new String[] {"./plugins/Landlords/config.yml", "regions"};
        this.ruleKeys_ = new LandLordsRuleKeys(this);
        RegionsLib.enable(this);
        RegionsLib.getAbilityHandler().registerAll(LandLordsAbilities.class);

        //Initializing totems
        TotemManager.loadTotemStructures();

        //Registering listeners
        this.getServer().getPluginManager().registerEvents(new TotemListener(),this);
        this.getServer().getPluginManager().registerEvents(new RegionListener(),this);
    }
    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    //METHODS
    @Override
    @Nonnull
    public Logger getLogger() {
        return this.LOGGER_;
    }
    public LandLordsRuleKeys getRuleKeys() {
        return this.ruleKeys_;
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
                if (!this.isLoggable(level)) { return; }
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

