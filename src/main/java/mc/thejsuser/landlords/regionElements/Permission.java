package mc.thejsuser.landlords.regionElements;

import mc.thejsuser.landlords.Landlords;
import org.bukkit.entity.Player;

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

}
