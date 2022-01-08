package mc.thejsuser.landlords.regionElements;

import mc.thejsuser.landlords.Landlords;
import mc.thejsuser.landlords.io.ConfigManager;
import mc.thejsuser.landlords.io.JsonManager;
import mc.thejsuser.landlords.io.LangManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Region {

    //FIELDS
    private int id_;
    private double[] vertex_ = null;
    private World.Environment dimension_ = null;
    private Permission[] permissions_;
    private String name_;
    private boolean enabled_ = true;
    private RegionBoundary boundary_ = null;
    private RegionDataContainer dataContainer_ = new RegionDataContainer();
    private RegionBoundingBox boundingBox_;
    private BoundayDisplayer boundayDisplayer_ = null;
    private final ConfigManager.ParticleData boundaryParticle_ = ConfigManager.getRegionBorderParticle();
    private final Region self_ = this;


    public final Permissions permissions = new Permissions();

    //CLASSES
    public class Permissions{

        public Group[] getFromPlayerName(String name) {
            return getFromPlayerName(name,getPermissions());
        }

        public Group[] getFromPlayerName(String name, Permission[] permissions) {
            List<Group> groups = new ArrayList<>();
            for (Permission permission : permissions) {
                if (permission.getPlayerName().equals(name)) {
                    groups.add(permission.getGroup());
                }
            }
            return groups.toArray(new Group[0]);
        }

    }

    //CONSTRUCTORS
    public Region() {
        boundingBox_ = new RegionBoundingBox(this);
    }
    public Region(double[] vertex, World.Environment dimension, Permission[] permissions, String name) {
        setId(getHighestId(regions_.toArray(Region[]::new)) + 1);
        setDimension(dimension);
        setVertex(vertex);
        setPermissions(permissions);
        setName(name);
    }

    //STATIC FIELDS
    private static List<Region> regions_ = new ArrayList<Region>();

    //SETTERS
    public void setVertex(double[] vertex) {

        double min, max;
        for (int i = 0; i < 3; i++) {
            min = Math.min(vertex[i], vertex[i + 3]);
            max = Math.max(vertex[i], vertex[i + 3]);
            vertex[i] = min; vertex[i+3] = max;
        }

        vertex_ = vertex;
        this.setBoundingBox_();
        this.attemptCalculateBoundaries_();
    }
    public void setDimension(World.Environment dimension) {
        dimension_ = dimension;
        this.attemptCalculateBoundaries_();
    }
    public void setPermissions(Permission[] permissions) {
        permissions_ = permissions;
    }
    public void setId(int id) {
        id_ = id;
    }
    public void setName(String name) {
        name_ = name;
    }
    public void enabled(boolean bool) {
        enabled_ = bool;
    }
    public void setDataContainer(RegionDataContainer dataContainer){
        dataContainer_ = dataContainer;
    }

    //GETTERS
    public Permission[] getPermissions() {
        return this.permissions_;
    }
    public int getId() {
        return this.id_;
    }
    public World.Environment getDimension() {
        return this.dimension_;
    }
    public String getName() {
        return this.name_;
    }
    public double[] getVertex() {
        return this.vertex_;
    }
    public boolean isEnabled() {
        return this.enabled_;
    }
    public RegionBoundary getBoundary(){ return boundary_; }
    public RegionDataContainer getDataContainer(){ return dataContainer_; }
    public RegionBoundingBox getBoundingBox(){
        return boundingBox_;
    }
    public double getHeight(){
        return this.getBoundingBox().getHeight();
    }
    public double getWidthX(){
        return this.getBoundingBox().getWidthX();
    }
    public double getWidthZ(){
        return this.getBoundingBox().getWidthZ();
    }
    public World getWorld() {

        World world = null;
        List<World> worlds = Landlords.getMainInstance().getServer().getWorlds();
        for (World w : worlds) {
            if (w.getEnvironment().equals(this.getDimension())) {
                world = w;
                break;
            }
        }
        return world;
    }
    public List<Player> getGroupLevelRagePlayers(int min, int max) {
       List <Player> r = new ArrayList<>();
        for (Permission p : this.getPermissions()) {
            int lvl = p.getGroup().getLevel();
            if (lvl <= max && lvl >= min) {
                r.add(Landlords.getMainInstance().getServer().getPlayer(p.getPlayerName()));
            }
        }
        return r;
    }

    //STATIC METHODS
    public static List<Region> loadRegions() {

        regions_ = JsonManager.loadRegions();
        return regions_;
    }
    public static List<Region> getRegions() {
        return regions_;
    }
    public static Region[] getFromPoint(double x, double y, double z, World.Environment dimension) {

        List<Region> l = new ArrayList<>(Collections.emptyList());
        for (Region i : regions_) {
            if (i.contains(x, y, z, dimension)) {
                l.add(i);
            }
        }
        return l.toArray(new Region[l.size()]);
    }
    public static Region[] getFromPoint(Location location) {
        return getFromPoint(location.getX(),location.getY(),location.getZ(), Objects.requireNonNull(location.getWorld()).getEnvironment());
    }
    public static boolean checkPlayerAbilityInRegions(Region[] regions, Player player, Abilities ability) {

        boolean r = true;
        List<Region> list = new ArrayList<>();
        for(Region i : regions){
            if(i.isEnabled()){
                list.add(i);
            }
        }
        regions=list.toArray(new Region[0]);

        if (regions.length > 0) {
            int pos = 0;
            switch (ConfigManager.getOverlappingRegionMode()) {
                case all:

                    r = true;
                    for (Region i : regions) {
                        if (!i.checkPlayerAbility(player, ability)) {
                            r = false;
                            break;
                        }
                    }
                    break;

                case any:

                    r = false;
                    for (Region i : regions) {
                        if (i.checkPlayerAbility(player, ability)) {
                            r = true;
                            break;
                        }
                    }
                    break;

                case oldest:

                    int min = getHighestId(regions);
                    for (int i = 0; i < regions.length; i++) {
                        if (regions[i].getId() < min) {
                            min = regions[i].getId();
                            pos = i;
                        }
                    }
                    r = regions[pos].checkPlayerAbility(player, ability);
                    break;

                case newest:

                    int max = 0;
                    for (int i = 0; i < regions.length; i++) {
                        if (regions[i].getId() > max) {
                            max = regions[i].getId();
                            pos = i;
                        }
                    }
                    r = regions[pos].checkPlayerAbility(player, ability);
                    break;

                default:
                    break;
            }
        }
        return r;
    }
    public static boolean checkPlayerAbilityAtPoint(Player player, Abilities ability, double x, double y, double z) {
        Region[] regions = getFromPoint(x, y, z, player.getWorld().getEnvironment());
        return checkPlayerAbilityInRegions(regions, player, ability);
    }
    public static boolean checkPlayerAbilityAtPoint(Player player, Abilities ability, Location location) {
        return checkPlayerAbilityAtPoint(player,ability,location.getX(),location.getY(),location.getZ());
    }
    public static int getHighestId(Region[] regions) {

        int max = 0;
        for (Region i : regions) {
            max = Math.max(max, i.getId());
        }
        return max;
    }
    public static Region getFromId(int id) {

        Region r = null;
        for (Region i : getRegions()) {
            if (i.getId() == id) {
                r = i;
                break;
            }
        }
        return r;
    }
    public static void addRegion(Region region) {
        regions_.add(region);
    }
    public static List<Region> getRegionsOverlapping(double[] vertex) {

        List<Region> r = new ArrayList<>();
        BoundingBox box1 = new BoundingBox(vertex[0],vertex[1],vertex[2],vertex[3],vertex[4],vertex[5]);
        for (Region i : Region.getRegions()) {
            RegionBoundingBox box2 = i.getBoundingBox();
            if(box2.overlaps(box1)){
                r.add(i);
            }
        }
        return r;
    }

    //PUBLIC METHODS
    public boolean checkPlayerAbility(Player player, Abilities ability) {

        boolean r = true;
        if(this.isEnabled()) {
            String name = player.getName();
            Permission perm = null;
            for (Permission i : permissions_) {
                if (i.getPlayerName().equals(name)) {
                    perm = i;
                    break;
                }
            }
            if (perm == null) {
                r = Group.checkForeignAbility(ability);
            } else {
                r = perm.getGroup().checkAbility(ability);
            }
        }
        return r;
    }
    public boolean contains(int x, int y, int z, World.Environment dimension) {

        if(!this.getDimension().equals(dimension)) { return false; }
        return this.getBoundingBox().contains(x+.5,y+.5,z+.5);
    }
    public boolean contains(double x, double y, double z, World.Environment dimension) {

        if(!this.getDimension().equals(dimension)) { return false; }
        return this.getBoundingBox().contains(x,y,z);
    }
    public void save() {

        Region r = getFromId(getId());
        if (r == null) {
            JsonManager.saveRegionNew(this);
            Region.addRegion(this);
        } else {
            JsonManager.saveRegionOverwrite(this, getId());
        }
    }
    public List<Region> getOverlappingRegions() {

        List<Region> l = Region.getRegionsOverlapping(this.getVertex());
        l.remove(this);
        return l;
    }
    public void destroy() {
        if(boundayDisplayer_ != null) {
            if (!boundayDisplayer_.displayer.isCancelled()) {
                this.boundayDisplayer_.cancel();
            }
        }
        regions_.remove(this);
        JsonManager.removeRegion(this);
    }
    public void displayBoundaries(int frequency ,long persistence) {
        if(boundayDisplayer_ != null) { boundayDisplayer_.cancel(); }

        boundayDisplayer_ = new BoundayDisplayer();
        boundayDisplayer_.displayBoundaries(frequency,persistence);
    }
    public void broadCastToMembers(String message, int maxLevel) {

    }
    public void broadCastToMembersLang(String path, String[] args, int maxLevel) {
        Landlords.broadcastMessageLang(path,args,this.getGroupLevelRagePlayers(0,maxLevel));
    }

    //PRIVATE METHODS
    private void attemptCalculateBoundaries_() {

        if(vertex_ != null && dimension_ != null){
            if (boundary_ == null) {
                boundary_ = new RegionBoundary(this);
            }else{
                boundary_.recalculate();
            }
        }
    }
    private void setBoundingBox_() {

        boolean cond;
        RegionBoundingBox box = this.getBoundingBox();
        if(box == null) { cond = true; } else { cond = box.getCorners() != this.getVertex(); }
        if (cond) {
            double[] v = this.getVertex();
            boundingBox_ = new RegionBoundingBox(v[0], v[1], v[2], v[3], v[4], v[5], this);
        }
    }

    //PRIVATE CLASSES
    private class BoundayDisplayer {

        //FIELDS
        boolean ran = false;

        //RUNNABLES
        private final BukkitRunnable displayer = new BukkitRunnable() {
            @Override
            public void run() {
                for (Location l : self_.getBoundary().getFullBoundaries()) {
                    Objects.requireNonNull(l.getWorld()).spawnParticle(
                            boundaryParticle_.particle(),
                            l,
                            boundaryParticle_.count(),
                            boundaryParticle_.delta()[0], boundaryParticle_.delta()[1], boundaryParticle_.delta()[2],
                            0, null, true
                    );
                }
            }
        };

        private final BukkitRunnable canceller = new BukkitRunnable() {
            @Override
            public void run() {
                displayer.cancel();
            }
        };

        //METHODS
        protected void displayBoundaries(int frequency, long persistence) {
            displayer.runTaskTimerAsynchronously(Landlords.getMainInstance(), 0, frequency);
            canceller.runTaskLater(Landlords.getMainInstance(), persistence);
            ran = true;
        }

        protected void cancel() {
            if (!ran) { return; }

            if (!displayer.isCancelled()) {
                canceller.cancel(); displayer.cancel();
            }
        }
    }
}

