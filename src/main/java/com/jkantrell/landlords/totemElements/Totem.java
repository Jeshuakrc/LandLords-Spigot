package com.jkantrell.landlords.totemElements;

import com.jkantrell.landlords.io.ConfigManager;
import com.jkantrell.landlords.io.LangManager;
import com.jkantrell.regionslib.regions.*;
import com.jkantrell.regionslib.regions.dataContainers.RegionData;
import com.jkantrell.landlords.Landlords;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;

import java.util.*;

public class Totem {

    //FIELDS
    protected static final BlockFace[] BLOCK_FACE_DIRECTIONS = new BlockFace[]{
            BlockFace.WEST,BlockFace.DOWN,BlockFace.NORTH,BlockFace.EAST,BlockFace.UP,BlockFace.SOUTH
    };

    private int[] position_ = new int[3];
    private World world_;
    private TotemStructure structure_;
    private boolean enabled_ = true;
    private int leveled_ = 0;
    private int regionId_;
    private EnderCrystal endCrystal_;
    private long lastInteraction_;
    private int cooldown_;
    private BoundingBox structureBox_;
    private final ConfigManager.ParticleData placeParticle_ = ConfigManager.getTotemPlaceParticle();
    private final int[] placeParticlePos_ = ConfigManager.getTotemPlaceParticlePos();

    private static List<Totem> totems_ = new ArrayList<>();

    private static NamespacedKey regionIdKey = new NamespacedKey(Landlords.getMainInstance(),"regionId");
    private static NamespacedKey leveledKey = new NamespacedKey(Landlords.getMainInstance(),"leveled");
    private static NamespacedKey isTotemKey = new NamespacedKey(Landlords.getMainInstance(),"isTotem");

    //CONSTRUCTORS
    public Totem(int x, int y, int z, World world, TotemStructure structure){
        setPosition(x,y,z);
        setWorld(world);
        setStructure(structure);
        cooldown_= ConfigManager.getTotemInteractCooldown();
        lastInteraction_=System.currentTimeMillis() - cooldown_;

    }

    //SETTERS
    public void setPosition(int x, int y, int z){
        position_[0] = x;
        position_[1] = y;
        position_[2] = z;
    }
    public void setWorld(World world){
        world_ = world;
    }
    public void setStructure(TotemStructure structure){

        structure_ = structure;

        int[] corners = this.getStructure().getStructureBox();
        structureBox_ = new BoundingBox(
                corners[0]+this.getPosX(),corners[1]+this.getPosY(),corners[2]+this.getPosZ(),
                corners[3]+this.getPosX(),corners[4]+this.getPosY(),corners[5]+this.getPosZ()
        );
    }
    public void setRegionId(int id){
        regionId_ = id;
    }
    public void setLevel(int level){
        leveled_=level;
    }
    public void setEndCrystal(EnderCrystal endCrystal){
        endCrystal_=endCrystal;
    }

    //GETTERS
    public int[] getPosition(){
        return this.position_;
    }
    public int getPosX(){
        return this.position_[0];
    }
    public int getPosY(){
        return this.position_[1];
    }
    public int getPosZ(){
        return this.position_[2];
    }
    public Location getLocation(){
        return new Location(this.getWorld(),this.getPosX(),this.getPosY(),this.getPosZ());
    }
    public World getWorld(){
        return this.world_;
    }
    public TotemStructure getStructure(){
        return this.structure_;
    }
    public boolean isEnabled(){
        return this.enabled_;
    }
    public int getRegionId(){
        return this.regionId_;
    }
    public Region getRegion(){
        return Region.getFromId(this.getRegionId());
    }
    public int getLevel(){
        return this.leveled_;
    }
    public EnderCrystal getEndCrystal(){
        return this.endCrystal_;
    }

    //PUBLIC METHODS
    public void place(Player placer){

        TotemStructure structure = this.getStructure();
        double[] vert = structure.getRegionInitialVertex();
        double[] regionVertex = new double[6];
        int[] totemPos = this.getPosition();
        Hierarchy hierarchy = structure.getHierarchy();
        World world = this.getWorld();
        for (int i = 0; i < 3; i++) {
            regionVertex[i] = vert[i] + totemPos[i] + 0.5;
            regionVertex[i+3] = vert[i+3] + totemPos[i] + 0.5;
        }

        Permission[] perms = {new Permission(
                placer.getName(),
                hierarchy,
                ConfigManager.getDefaultGroupLevel()
            )};
        String name = LangManager.getString("totem_region_defauldName",placer,placer.getName());
        Region region = new Region(regionVertex,this.getWorld(),perms,name,hierarchy);
        RegionData totemData = new RegionData("totemRegion",true);
        region.getDataContainer().add(totemData);
        for (Rule rule : structure.getRules()) {
            region.addRule(rule);
        }
        region.save();

        EnderCrystal crystal = placeEndCrystal(placer,this.getPosX(),this.getPosY(),this.getPosZ());
        this.setRegionId(region.getId());
        this.saveOnEndCrystal(crystal);
        TotemManager.registerTotem(this);
        region.displayBoundaries(ConfigManager.getRegionBorderRefreshRate(),ConfigManager.getRegionBorderPersistencePlaced());
        world.playSound(this.getLocation(), Sound.BLOCK_BEACON_ACTIVATE,3, 0.5f);
        world.spawnParticle(
                placeParticle_.particle(),
                this.getPosX() + placeParticlePos_[0],
                this.getPosY() + placeParticlePos_[1],
                this.getPosZ() + placeParticlePos_[2],
                placeParticle_.count(),
                placeParticle_.delta()[0], placeParticle_.delta()[1], placeParticle_.delta()[2]
        );
    }
    public boolean resize(int amount, int level) throws TotemUnresizableException {

        try { checkCoolDown_(); } catch (TotemNonColdDownException e) { return false; }

        Region region = this.getRegion();
        RegionBoundingBox regBox = region.getBoundingBox();
        double[] exp = this.getStructure().getRegionGrowthRate().clone();
        double[] size = {regBox.getWidthX(),regBox.getHeight(),regBox.getWidthZ()};
        double[] max = this.getStructure().getRegionMaxSize();

        for (int i = 0; i < 6; i++) {       //Multiply the growth rate by the amount factor
            exp[i] = exp[i]*amount;
        }

        if (amount > 0) {

            HashMap<Integer,Double> dues = new HashMap<Integer,Double>();

            for (int i = 0; i < 3; i++ ) {      //Limit and adjust expansion based on max size
                double expt = exp[i] + exp[i+3];
                if (size[i] + expt >= max[i] && max[i] > 0 && expt != 0) {
                    double frac = (max[i] - size[i]) / expt;
                    exp[i] = exp[i] * frac;
                    exp[i+3] = exp[i+3] * frac;
                }
            }

            for (int i = 0; i < 6; i++) {       //Putting the values to de dues list
                if (exp[i] > 0) {
                    dues.put(i, exp[i]);
                }
            }

            double pending;
            do {
                pending = 0;
                for (int i : new HashMap<Integer,Double>(dues).keySet()){

                    double d = dues.get(i);
                    double dif = expandRegionDirectional_(i,d);
                    dues.put(i,0d);
                    pending += dif;

                    if(dif > 0){
                        dues.remove(i);
                        int ic = getComplement_(i);

                        if (dues.containsKey(ic)) {
                            dues.put(ic, dues.get(ic) + dif);
                        } else {
                            int in1 = getNext_(i), in2 = getComplement_(in1);
                            double[] rate = this.getStructure().getRegionGrowthRate();
                            double part, v, whole;

                            if (in1 == -1) {
                                int[] indexes = {0,2,3,5};
                                whole = 0;
                                for (int j : indexes) { whole += rate[j]; }
                                for (int j : indexes) {
                                    part = rate[j]/whole;
                                    v = dif * part;

                                    if(dues.containsKey(j)) { v += dues.get(j); }
                                    dues.put(j,v);
                                }

                            } else if (dues.containsKey(in1) || dues.containsKey(in2)) {
                                int[] indexes = {in1,in2};
                                whole = rate[in1] + rate [in2];
                                for (int j : indexes) {
                                    part = rate[j]/whole;
                                    v = dif * part;

                                    if(dues.containsKey(j)) { v += dues.get(j); }
                                    dues.put(j,v);
                                }

                            } else if (dues.containsKey(1) || dues.containsKey(4)) {
                                int[] indexes = {1,4};
                                whole = rate[1] + rate [4];
                                for (int j : indexes) {
                                    part = rate[j]/whole;
                                    v = dif * part;

                                    if(dues.containsKey(j)) { v += dues.get(j); }
                                    dues.put(j,v);
                                }

                            } else {
                                this.setLastInteraction_();
                                throw new TotemUnresizableException();
                            }
                        }
                    }
                }
            } while (pending>0);


        } else {

            double[] min = this.getStructure().getRegionInitialSize();
            for (int i = 0; i < 3; i++ ) {      //Limit and adjust expansion based on max size
                int p1=0, p2=0;
                switch (i) {
                    case 0: p1 = 1; p2 = 2; break;
                    case 1: p1 = 0; p2 = 2; break;
                    case 2: p1 = 0; p2 = 1; break;
                }
                if ((size[i] >= max[i]) && (size[i]*2 < size[p1] + size[p2]) && max[i] > 0) {
                    exp[i] = 0; exp[i+3] = 0;
                }

                double expt = exp[i] + exp[i+3];
                double x = (min[i]+expt);
                if (size[i] + expt <= min[i] && expt != 0) {

                    double frac = (size[i] - min[i]) / -expt;
                    exp[i] = exp[i] * frac;
                    exp[i+3] = exp[i+3] * frac;
                }
            }
            for (int i = 0; i < 6; i++) {
                regBox.expand(BLOCK_FACE_DIRECTIONS[i], exp[i]);
            }
        }

        region.save();
        setLevel(Math.max(getLevel()+level,0));
        saveOnEndCrystal(getEndCrystal());
        if (level < 0) {
            this.dropItem_(-level);
        }

        double[] s = {
                region.getWidthX(), region.getHeight(), region.getWidthZ()
        };

        setLastInteraction_();

        return true;
    }
    public void saveOnEndCrystal(EnderCrystal endCrystal){

        PersistentDataContainer data = endCrystal.getPersistentDataContainer();
        data.set(isTotemKey, PersistentDataType.BYTE, (byte)1);
        data.set(regionIdKey, PersistentDataType.INTEGER, getRegionId());
        data.set(leveledKey, PersistentDataType.INTEGER, getLevel());
        setEndCrystal(endCrystal);
    }
    public boolean structureContainsBlock(Block block){

        Location loc = block.getLocation();
        loc.add(0.5,0.5,0.5);

        return (
                this.getWorld().equals(loc.getWorld()) &&
                this.structureBox_.contains(loc.getX(),loc.getY(), loc.getZ())
        );
    };
    public void enabled(boolean enabled){
        this.getRegion().enabled(enabled);
        this.getRegion().save();
    };
    public TotemLectern getLecternAtSpot(Block block) {

        TotemLectern r = null;
        if (block.getWorld().equals(this.getWorld())) {
            int[]   pos = this.getPosition(),
                    blockPos = {block.getX(), block.getY(), block.getZ()},
                    lPos;

            for (TotemLectern l : this.getStructure().lecterns) {
                lPos = l.getAbsolutePosition(pos[0], pos[1], pos[2]);

                if (Arrays.equals(lPos, blockPos)) {
                    r = l.clone();
                    r.setTotem(this);
                    break;
                }
            }
        }
        return r;
    }
    public void destroy() {

        this.dropItem_(this.getLevel());
        TotemManager.removeTotem(this);
        this.getRegion().destroy();
    }

    //PUBLIC STATIC METHODS
    public static EnderCrystal placeEndCrystal(Player placer, double x, double y, double z){

        Location l = new Location(placer.getWorld(),x+0.5,y,z+0.5);
        EnderCrystal endCrystal = (EnderCrystal)placer.getWorld().spawnEntity(l, EntityType.ENDER_CRYSTAL);
        endCrystal.setShowingBottom(false);
        ItemStack item = new ItemStack(Material.END_CRYSTAL);
        placer.getInventory().removeItem(item);
        return endCrystal;
    }
    public static boolean isTotem(EnderCrystal enderCrystal){
        PersistentDataContainer data = enderCrystal.getPersistentDataContainer();
        return data.has(isTotemKey,PersistentDataType.BYTE);
    }
    public static Totem getFromEndCrystal(EnderCrystal enderCrystal){

        Totem t = TotemManager.getTotemFromEndCrystal(enderCrystal);
        if(t == null) {
            PersistentDataContainer data = enderCrystal.getPersistentDataContainer();
            int level = data.get(leveledKey, PersistentDataType.INTEGER);
            int regionID = data.get(regionIdKey, PersistentDataType.INTEGER);
            Location loc = enderCrystal.getLocation();
            TotemStructure structure = TotemManager.chekStructuresFromPoint(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), enderCrystal.getWorld());
            t = new Totem(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), enderCrystal.getWorld(), structure);
            t.setRegionId(regionID);
            t.setLevel(level);
            t.setEndCrystal(enderCrystal);
            TotemManager.registerTotem(t);
        }
        return t;

    }

    //PRIVATE METHODS
    private void checkCoolDown_() throws TotemNonColdDownException {
        if(lastInteraction_ + cooldown_ > System.currentTimeMillis()){
            throw new TotemNonColdDownException();
        }
    }
    private void setLastInteraction_() {
        lastInteraction_ = System.currentTimeMillis();
    }
    private void dropItem_(int amount) {

        if (this.getLevel() >0) {
            for (int i = 0; i < amount; i++) {
                double random = Math.random();
                if (random <= ConfigManager.getTotemDropBackRate()) {
                    Item item = this.getWorld().dropItem(this.getLocation(), new ItemStack(ConfigManager.getTotemUpgradeItem()));
                    item.setInvulnerable(true);
                }
            }
        }
    }
    private double expandRegionDirectional_(int index, double amount) {

        BlockFace direction = BLOCK_FACE_DIRECTIONS[index];
        double dif = 0;
        Region region = this.getRegion();
        RegionBoundingBox box = region.getBoundingBox();

        box.expand(direction,amount);
        List<Region> overlapping = region.getOverlappingRegions();
        if (!overlapping.isEmpty()) {
            for (Region r : overlapping) {
                dif = Math.max(dif, Math.abs(r.getVertex()[getComplement_(index)] - box.getCorners()[index]));
            }
            box.expand(direction,-dif);
        }
        return dif;
    }
    private int getComplement_(int index){
        if (index == -1) { return -1; }
        if (index < 3) {
            return index + 3;
        } else {
            return index - 3;
        }
    }
    private int getNext_(int index) {

        int r = -1;
        switch (index) {
            case 0:
            case 3:
                r = index + 2;
                break;

            case 2:
            case 5:
                r = index - 2;
                break;

            default: break;
        }
        return r;
    }
}
