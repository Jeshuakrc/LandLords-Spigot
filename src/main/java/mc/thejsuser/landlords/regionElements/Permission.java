package mc.thejsuser.landlords.regionElements;

public class Permission {

    //FIELDS
    private Group group_;
    private String playerName_;

    //CONSTRUCTORS
    public Permission (Group group, String player) {
        this.setGroup(group);
        this.setPlayerName(player);
    }

    //SETTERS
    public void setGroup(Group group){
        group_= group;
    }
    public void setPlayerName(String name){
        playerName_=name;
    }

    //GETTERS
    public Group getGroup(){
        return this.group_;
    }
    public String getPlayerName(){
        return this.playerName_;
    }

}
