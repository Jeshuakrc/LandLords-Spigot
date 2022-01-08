package mc.thejsuser.landlords;

import com.google.common.collect.Lists;
import mc.thejsuser.landlords.io.LangManager;
import mc.thejsuser.landlords.regionElements.Group;
import mc.thejsuser.landlords.regionElements.Permission;
import mc.thejsuser.landlords.regionElements.Region;
import mc.thejsuser.landlords.events.*;
import mc.thejsuser.landlords.io.ConfigManager;
import mc.thejsuser.landlords.totemElements.TotemManager;
import net.md_5.bungee.api.chat.hover.content.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
        Group.loadGroups();
        Region.loadRegions();

        //Registering events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new BlockEvents(), this);
        pm.registerEvents(new ContainerEvents(), this);
        pm.registerEvents(new RedstoneEvents(), this);
        pm.registerEvents(new EntityEvents(), this);
        pm.registerEvents(new TotemEvents(), this);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    //GENERAL PURPOSE METHODS
    public static void broadcastMessageLang(String path, String[] args, Collection<? extends Player> players){
        for (Player player : players) {
                assert player != null;
                player.sendMessage(LangManager.getString(path, player, args));
        }
    }
    public static void broadcastMessageLang(String path, String[] args){
        Landlords.broadcastMessageLang(path,args, mainInstance.getServer().getOnlinePlayers());
    }

    //FOR DEBUGGING PURPOSES. DELETE BEFORE RELEASE
    public static void sendLogMessage(String message){
        var l = getMainInstance();
        l.getServer().getPlayer("TheJsUser").sendMessage(message);
    }
}

