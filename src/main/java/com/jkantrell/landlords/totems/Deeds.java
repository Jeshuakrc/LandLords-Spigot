package com.jkantrell.landlords.totems;

import com.jkantrell.landlords.io.ConfigManager;
import com.jkantrell.landlords.io.LangManager;
import com.jkantrell.regionslib.regions.*;
import com.jkantrell.regionslib.regions.dataContainers.*;
import com.jkantrell.landlords.Landlords;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class Deeds {

    //FIELDS
    private ItemStack itemStack_;
    private Region region_;
    private Permission[] permissions_;
    private String name_;
    private int id_;
    private Player holder_;

    public static final String deedsIdContainerKey = "deedsId";
    private static NamespacedKey isDeedsNsKey_ = new NamespacedKey(Landlords.getMainInstance(),"isDeeds");
    private static NamespacedKey deedsRegionIdNsKey_ = new NamespacedKey(Landlords.getMainInstance(),"deedsRegionId");
    private static NamespacedKey deedsIdNsKey_ = new NamespacedKey(Landlords.getMainInstance(),"deedsId");

    //CONSTRUCTORS
    public Deeds(Region region, Player creator){
        this.setRegion_(region);
        this.permissions_ = this.getRegion().getPermissions();
        this.name_ = this.getRegion().getName();
        this.setID_();
        this.itemStack_ = this.convertToDeeds(new ItemStack(Material.WRITABLE_BOOK),creator);
        this.holder_ = creator;

    }
    private Deeds(ItemStack item, Region region, int id, Player getter) {
        this.setRegion_(region);
        this.permissions_ = this.getRegion().getPermissions();
        this.name_ = this.getRegion().getName();
        this.id_ = id;
        this.itemStack_ = item;
        this.holder_ = getter;
    }

    //PUBLIC METHODS
    public ItemStack write() {

        ItemStack book = this.getItemStack();
        BookMeta bookMeta = (BookMeta) book.getItemMeta();
        Region region = this.getRegion();

        List<String> pages = new ArrayList<>();
        pages.add(getStyledFirstPage_(region.getName()));

        HashMap<Hierarchy.Group,List<String>> permissionsMap = new HashMap<>();
        Hierarchy.Group group;
        for (Permission p : region.getPermissions()) {
            group = p.getGroup();
            if (!permissionsMap.containsKey(group)) {
                permissionsMap.put(group,new ArrayList<>());
            }
            permissionsMap.get(group).add(p.getPlayerName());
        }

        int size, index, perPage = ConfigManager.getDeedsPlayersPerPage();
        List<String> players;
        boolean newPage;
        List<String> names = new ArrayList<>();
        for (Hierarchy.Group g : region.getHierarchy().getGroups()) {
            index = 0;
            players = permissionsMap.getOrDefault(g, Collections.emptyList());
            size = players.size();
            newPage = false;

            do {
                while (index < size || newPage) {
                    names.clear();
                    for (int i = 0; i < perPage && index < size; i++) {
                        names.add(players.get(index));
                        index++;
                    }
                    pages.add(getStyledPermissionsPage_(g, names));
                    if (newPage) { break; }
                }
                newPage = (index % perPage) >= ConfigManager.getDeedsPlayersForNewPage() && !newPage;
            } while (newPage);
        }

        bookMeta.setPages(pages);
        book.setItemMeta(bookMeta);

        itemStack_ = book;

        return book;
    }
    public Change readPage(BookMeta bookMeta , int page){
        ItemStack book = this.getItemStack();
        if (page < 1 || !checkMaterial_(book)) { throw new IllegalArgumentException(); }
        //if (!(page <= bookMeta.getPages().size())) { throw new IllegalArgumentException(); }
        String pag = removeStyle_(bookMeta.getPage(page));
        readExceptionThrower_ exceptionThrower = new readExceptionThrower_(page);
        Change change;
        BookMeta meta = (BookMeta) book.getItemMeta();

        if (page == 1) {
            boolean hasInBracket = pag.contains("["), hasOutBrackets = pag.contains("]");
            if (hasInBracket || hasOutBrackets) {
                if (!(hasInBracket && hasOutBrackets)) {
                    String missing = hasOutBrackets ? "[" : "]";
                    exceptionThrower.trowException("deeds_read_error_enclosureNotFound_singular",missing);
                }
            } else {
                exceptionThrower.trowException("deeds_read_error_enclosureNotFound_plural","[","]");
            }

            String regionName = StringUtils.substringBetween(pag,"[","]");
            int     length = regionName.length(),
                    minLength = ConfigManager.getRegionMinNameLength(),
                    maxLength = ConfigManager.getRegionMaxNameLength();

            if (length <= minLength) {
                exceptionThrower.trowException("deeds_read_error_illegalNameLength_short",Integer.toString(minLength),Integer.toString(length),Integer.toString(minLength - length));
            }
            if (length >= maxLength) {
                exceptionThrower.trowException("deeds_read_error_illegalNameLength_long",Integer.toString(maxLength),Integer.toString(length),Integer.toString(length - maxLength));
            }

            change = new Change(regionName,null);
            meta.setPage(page, getStyledFirstPage_(regionName));

        } else {
            if (pag.indexOf(':') == -1) {
                exceptionThrower.trowException("deeds_error_permissionMissingSeparator");
            }
            String[] sections = StringUtils.split(pag,":");
            if (sections.length > 2) {
                exceptionThrower.trowException("deeds_error_permissionOverSeparators",Integer.toString(sections.length-1));
            }

            Hierarchy hierarchy = this.getRegion().getHierarchy();
            Hierarchy.Group regionGroup = hierarchy.getGroup(sections[0]);
            if (regionGroup == null){
                exceptionThrower.trowException("deeds_error_groupNotFound",sections[0]);
            }

            String[] players = new String[0];
            Permission[] permissions = new Permission[0];
            if (sections.length > 1) {
                players = StringUtils.split(sections[1], "\n");
                permissions = new Permission[players.length];
                for (int i = 0; i < players.length; i++) {
                    permissions[i] = new Permission(players[i],hierarchy,regionGroup.getLevel());
                }
            }

            pag = getStyledPermissionsPage_(regionGroup, new ArrayList<String>(Arrays.asList(players)));

            if (page > meta.getPages().size()) {
                meta.addPage(pag);
            } else {
                meta.setPage(page,pag);
            }

            change = new Change(null, new ArrayList<>(Arrays.asList(permissions)));
        }

        this.itemStack_.setItemMeta(meta);
        return change;
    }
    public Change readPage(int page){
        return this.readPage((BookMeta) this.getItemStack().getItemMeta(),page);
    }
    public ReadingResults read(BookMeta bookMeta) {

        List<Change> changes = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<Permission> permissions = new ArrayList<>();
        String name = null;

        for (int i = 1; i <= bookMeta.getPages().size(); i++) {
            try {
                changes.add(this.readPage(bookMeta,i));
            } catch (IllegalArgumentException e) {
                errors.add(e.getMessage());
            }
        }
        for (Change change : changes) {
            if (change.name() != null) {
                name = change.name;
            }
            if (change.permissions() != null){
                permissions.addAll(change.permissions());
            }
        }

        return new ReadingResults(name, permissions, errors);
    }
    public ReadingResults read(){
        return read((BookMeta) this.getItemStack().getItemMeta());
    }

    //GETTERS
    public ItemStack getItemStack(){
        return itemStack_.clone();
    }
    public Region getRegion() {
        return region_;
    }
    public int getId() {
        return id_;
    }

    //SETTERS
    private int setID_(){
        Region region = this.getRegion();
        int id = Deeds.getDeedsId(region);
        id = (id == -1) ? 1 : id +1;
        Deeds.setDeedsId_(region,id);
        id_ = id;
        return this.getId();
    }
    private void setRegion_(Region region){
        region_=region;
    }

    //RECORDS
    public record Change(String name, List<Permission> permissions) {}
    public record ReadingResults(String name, List<Permission> permissions, List<String> errors) {}

    //CLASSES
    private class readExceptionThrower_ {
        final int page;

        readExceptionThrower_(int page){
            this.page = page;
        }
        void trowException(String path, String... parms){
            throw new IllegalArgumentException(
                        LangManager.getString("deeds_read_errorPageIndicator",holder_,Integer.toString(this.page)) + "§r" +
                        LangManager.getString("deeds_read_errorGeneralStyle",holder_) +
                        LangManager.getString(path,holder_,parms) + "§r"
                        );
        }
    }

    //STATIC METHODS
    public ItemStack convertToDeeds(ItemStack itemStack, Player holder) {

        itemStack.setType(Material.WRITABLE_BOOK);
        ItemMeta meta = itemStack.getItemMeta();
        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();

        dataContainer.set(isDeedsNsKey_,PersistentDataType.BYTE,(byte) 1);
        dataContainer.set(deedsIdNsKey_,PersistentDataType.INTEGER,this.getId());
        dataContainer.set(deedsRegionIdNsKey_,PersistentDataType.INTEGER,this.getRegion().getId());
        meta.addEnchant(Enchantment.BINDING_CURSE,1,true);
        meta.setDisplayName(LangManager.getString("deeds_bookTittle", holder, this.getRegion().getName()));

        itemStack.setItemMeta(meta);

        return itemStack;
    }
    public static boolean isTotemDeeds(ItemStack itemStack){
        if (checkMaterial_(itemStack)) {
            return isTotemDeeds((BookMeta) itemStack.getItemMeta());
        } else {
            return false;
        }
    }
    public static boolean isTotemDeeds(BookMeta bookMeta) {
        return bookMeta.getPersistentDataContainer().has(isDeedsNsKey_, PersistentDataType.BYTE);
    }
    public static Deeds getFromBook(ItemStack itemStack, Player getter) {
        if (!Deeds.isTotemDeeds(itemStack)) { return null; }
        Deeds r = Deeds.getFromBook((BookMeta) itemStack.getItemMeta(),getter);
        assert r != null;
        r.itemStack_ = itemStack;

        return r;
    }
    public static Deeds getFromBook(BookMeta bookMeta, Player getter) {

        if (!Deeds.isTotemDeeds(bookMeta)) { return null; }

        PersistentDataContainer dataContainer = bookMeta.getPersistentDataContainer();
        Region region = Region.getFromId(dataContainer.get(deedsRegionIdNsKey_,PersistentDataType.INTEGER));
        int id = dataContainer.get(deedsIdNsKey_,PersistentDataType.INTEGER);
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        item.setItemMeta(bookMeta);

        return new Deeds(item,region,id,getter);

    }
    public static int getDeedsId(Region region) {
        RegionData data = region.getDataContainer().get(deedsIdContainerKey);

        if (data != null) {
            return data.getAsInt();
        } else {
            return -1;
        }
    }

    //PRIVATE METHODS
    private String getStyledFirstPage_(String name){
        String  nameField = "[" + name + "]",
                deedsId = String.format("%03d",this.getId()),
                firstPage = LangManager.getStringNonFormatted("deeds_bookContent",this.holder_)+"§r";
        firstPage = firstPage.replaceAll("\\[","").replaceAll("]","");
        return String.format(firstPage,nameField,deedsId,this.getRegion().getId());
    }
    private String getStyledPermissionsPage_(Hierarchy.Group group, List<String> players){
        StringBuilder page = new StringBuilder();
        page.append(extractStyle_(LangManager.getString("deeds_bookHeaders_style", this.holder_)))
                .append(group.getName())
                .append(":§r")
                .append(extractStyle_(LangManager.getString("deeds_bookPlayerNames_style", this.holder_)));
        for (String p : players) {
            page.append("\n").append(p);
        }
        return page.toString();
    }

    //PRIVATE STATIC METHODS
    private static void setDeedsId_(Region region, int id) {
        RegionDataContainer data = region.getDataContainer();
        if (data.has(deedsIdContainerKey)) {
            data.remove(deedsIdContainerKey);
        }
        data.add(new RegionData(deedsIdContainerKey,id));
        region.save();
    }
    private static boolean checkMaterial_(ItemStack itemStack) {
        Material material = itemStack.getType();
        return material.equals(Material.WRITTEN_BOOK) || material.equals(Material.WRITABLE_BOOK);
    }
    private static String extractStyle_(String string){
        StringBuilder r = new StringBuilder();
        char[] chars = string.toCharArray();
        int prev = 0;
        for (int i = 0; i < chars.length; i++) {
            if(chars[i] == '§' || chars[i-prev] == '§'){
                r.append(chars[i]);
            }
            prev = 1;
        }
        return r.toString();
    }
    private static String removeStyle_(String string) {

        StringBuilder r = new StringBuilder().append(string);
        int charInd=-1;
        boolean keepGoing = false;
        do {
            if (keepGoing) {
                r.deleteCharAt(charInd);
                char next = r.charAt(charInd);
                if (!(next == ' ' || next == '§')) {
                    r.deleteCharAt(charInd);
                }
            }
            charInd = r.indexOf("§");
            keepGoing = charInd != -1;
        }while (keepGoing);
        return r.toString();
    }
}