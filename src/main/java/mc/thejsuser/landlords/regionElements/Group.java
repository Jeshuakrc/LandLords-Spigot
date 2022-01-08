package mc.thejsuser.landlords.regionElements;

import mc.thejsuser.landlords.io.JsonManager;
import java.util.List;

public class Group {

    //FIELDS
    private int level_;
    private String name_;
    private List<Abilities> abilities_;

    //STATIC FIELDS
    private static Group[] groups_;

    //SETTERS
    public void setLevel(int level){
        level_=level;
    }
    public void setName(String name){
        name_=name;
    }
    public void setAbilities(List<Abilities> abilities){
        abilities_ = abilities;
    }

    //GETTERS
    public int getLevel(){
        return this.level_;
    }
    public String getName(){
        return this.name_;
    }
    public List<Abilities> getAbilities(){
        return this.abilities_;
    }

    //METHODS
    public boolean checkAbility(Abilities ability){

        boolean r;
        if(level_ > 1) {
            r = abilities_.contains(ability);
            if (!r){
                for (int i = level_-1; i>0 && !r; i--) {
                    if (Group.getFromLevel(i).getAbilities().contains(ability)) {
                        r = true;
                    }
                }
                r = !r;
            }
        } else {
            r = true;
        }
        return r;
    }

    //STATIC METHODS
    public static Group getFromName(String name){

        Group g = null;
        for (Group i : groups_) {
            if (i.getName().equals(name)) {
                g = i;
            }
        }
        return g;
    }
    public static Group getFromLevel(int level){

        Group g = null;
        for (Group group : groups_){
            if (group.getLevel() == level) { g = group; }
        }

        return g;
    }
    public static Group[] getGroups(){
        return groups_;
    }
    public static Group[] loadGroups(){
        groups_ = JsonManager.loadGroups();
        return groups_;
    }
    public static boolean checkForeignAbility(Abilities ability){

        boolean r = false;
        for (int i = getHighestLevel(); i>0; i--) {
            if (Group.getFromLevel(i).getAbilities().contains(ability)) {
                r = true;
            }
        }
        r = !r;

        return r;
    }
    public static int getHighestLevel(){

        int[] levels = new int[groups_.length];
        for (int i = 0; i < levels.length; i++) {
            levels[i] = groups_[i].getLevel();
        }
        int max = 0;
        for (int i : levels){
            max = Math.max(i,max);
        }
        return max;
    }

}
