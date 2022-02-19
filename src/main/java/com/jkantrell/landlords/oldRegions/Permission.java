package com.jkantrell.landlords.oldRegions;

import com.google.gson.*;
import com.jkantrell.landlords.Landlords;
import org.bukkit.entity.Player;

import java.lang.reflect.Type;

public class Permission {

    //FIELDS
    private final String playerName_;
    private final Hierarchy hierarchy_;
    private final Hierarchy.Group group_;

    //CONSTRUCTORS
    public Permission (String player, Hierarchy hierarchy, int level) {
        this.playerName_ = player;
        this.hierarchy_ = hierarchy;
        this.group_ = hierarchy_.getGroup(level);
    }

    //GETTERS
    public Hierarchy.Group getGroup(){
        return this.group_;
    }
    public String getPlayerName() {
        return this.playerName_;
    }
    public Player getPlayer(){
        return Landlords.getMainInstance().getServer().getPlayer(this.playerName_);
    }

    //PUBLIC CLASSES
    public static class JSerializer implements JsonSerializer<Permission> {

        @Override
        public JsonElement serialize(Permission src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonPermission = new JsonObject();
            jsonPermission.addProperty("player_name",src.getPlayerName());

            jsonPermission.addProperty("level", src.getGroup().getLevel());

            return jsonPermission;
        }
    }
}
