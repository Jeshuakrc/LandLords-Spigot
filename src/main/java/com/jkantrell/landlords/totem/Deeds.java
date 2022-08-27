package com.jkantrell.landlords.totem;

import com.jkantrell.landlords.io.LangProvider;
import com.jkantrell.regionslib.regions.*;
import com.jkantrell.regionslib.regions.dataContainers.*;
import com.jkantrell.landlords.Landlords;
import org.apache.commons.lang3.StringUtils;
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

    //STATIC FIELDS
    private static final String deedsIdContainerKey_ = "deedsId";
    private static final String deedsMinutesContainerKey_ = "deedsMinutes";
    private static final NamespacedKey deedsIdNsKey_ = new NamespacedKey(Landlords.getMainInstance(),"deedsId");
    private static final NamespacedKey deedsRegionIdNsKey_ = new NamespacedKey(Landlords.getMainInstance(),"deedsRegionId");
    private static final NamespacedKey deedsMinutesNsKey_ = new NamespacedKey(Landlords.getMainInstance(),"deedsMinutes");

    //STATIC METHODS
    public static boolean isTotemDeeds(ItemStack itemStack){
        if (itemStack == null) { return false; }
        if (!checkMaterial_(itemStack)) { return false; }
        return isTotemDeeds((BookMeta) itemStack.getItemMeta());
    }
    public static boolean isTotemDeeds(BookMeta bookMeta) {
        if (bookMeta == null) { return false; }
        PersistentDataContainer dataContainer = bookMeta.getPersistentDataContainer();
        if (!dataContainer.has(deedsIdNsKey_, PersistentDataType.STRING)) { return false; }
        try {
            UUID.fromString(dataContainer.get(deedsIdNsKey_, PersistentDataType.STRING));
        } catch (Exception e) {return false; }
        return (dataContainer.has(deedsMinutesNsKey_, PersistentDataType.INTEGER) &&
                dataContainer.has(deedsIdNsKey_, PersistentDataType.STRING)
        );
    }
    public static Optional<Deeds> fromBook(ItemStack itemStack, Player getter) {
        if (itemStack == null) { return Optional.empty(); }
        if (!checkMaterial_(itemStack)) { return Optional.empty(); }
        Optional<Deeds> r = Deeds.fromBook((BookMeta) itemStack.getItemMeta(),getter);
        if (r.isEmpty()) { return r; }
        r.get().itemStack_ = itemStack;
        return r;
    }
    @SuppressWarnings("null")
    public static Optional<Deeds> fromBook(BookMeta bookMeta, Player getter) {
        //Checking if the ItemMetaProvided is from a Deeds item
        if (!Deeds.isTotemDeeds(bookMeta)) { return Optional.empty(); }

        //Checking if there's a region under the extracted ID
        PersistentDataContainer dataContainer = bookMeta.getPersistentDataContainer();
        Region region = Regions.get(dataContainer.get(deedsRegionIdNsKey_,PersistentDataType.INTEGER)).orElse(null);
        if (region == null) { return Optional.empty(); }

        //Checking if the region matches this deed's ID
        RegionDataContainer regionDataContainer = region.getDataContainer();
        if (!regionDataContainer.has(deedsIdContainerKey_)) { return Optional.empty(); }
        UUID id = UUID.fromString(dataContainer.get(deedsIdNsKey_,PersistentDataType.STRING));
        if (!regionDataContainer.get(deedsIdContainerKey_).getAsString().equals(id.toString())) { return Optional.empty(); }

        //Building deeds
        int minutes = dataContainer.get(deedsMinutesNsKey_,PersistentDataType.INTEGER);
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        item.setItemMeta(bookMeta);

        return Optional.of(new Deeds(item,region,minutes,id,getter));
    }
    public static Optional<Integer> idOf(Region region) {
        RegionData data = region.getDataContainer().get(deedsMinutesContainerKey_);
        return Optional.ofNullable((data == null) ? null : data.getAsInt());
    }

    //FIELDS
    private ItemStack itemStack_;
    private Region region_;
    private Permission[] permissions_;
    private String name_;
    private int minutes_;
    private Player holder_;
    private final UUID id_;

    //CONSTRUCTORS
    public Deeds(Region region, Player creator){
        this.region_ = region;
        this.permissions_ = region.getPermissions();
        this.name_ = region.getName();
        this.id_ = UUID.randomUUID();
        this.holder_ = creator;
        this.setMinutes_();
        this.itemStack_ = this.convertToDeeds(new ItemStack(Material.WRITABLE_BOOK),creator);
    }
    private Deeds(ItemStack item, Region region, int minutes, UUID id, Player getter) {
        this.region_ = region;
        this.permissions_ = this.getRegion().getPermissions();
        this.name_ = this.getRegion().getName();
        this.minutes_ = minutes;
        this.itemStack_ = item;
        this.holder_ = getter;
        this.id_ = id;
    }

    //GETTERS
    public ItemStack getItemStack(){
        return itemStack_.clone();
    }
    public Region getRegion() {
        return region_;
    }
    public UUID getId() {
        return this.id_;
    }
    public int getMinutes() {
        return minutes_;
    }

    //SETTERS
    private int setMinutes_(){
        Region region = this.getRegion();
        this.minutes_ = Deeds.idOf(region).orElse(1);
        return this.getMinutes();
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

        int total, index, perPage = Landlords.CONFIG.deedsPlayersPerPage;
        List<String> players;
        List<String> names = new ArrayList<>();
        List<Hierarchy.Group> groups = region.getHierarchy().getGroups();
        for (Hierarchy.Group g : groups) {
            players = permissionsMap.getOrDefault(g,Collections.emptyList());
            total = players.size();
            index = 0;

            do {
                names.clear();
                for (int i = 0; i < perPage && index < total; i++) {
                    names.add(players.get(index));
                    index++;
                }
                pages.add(this.getStyledPermissionsPage_(g,names));
            } while (index < total);
        }

        bookMeta.setPages(pages);
        book.setItemMeta(bookMeta);

        itemStack_ = book;
        this.saveToRegion_();

        return book;
    }
    public Change readPage(BookMeta bookMeta , int page){
        ItemStack book = this.getItemStack();
        if (page < 1 || !checkMaterial_(book)) { throw new IllegalArgumentException(); }
        //if (!(page <= bookMeta.getPages().size())) { throw new IllegalArgumentException(); }
        String pag = removeStyle_(bookMeta.getPage(page));
        String errorBasePath = "deeds.error_message.compose.";
        readExceptionThrower_ exceptionThrower = new readExceptionThrower_(page);
        Change change;
        BookMeta meta = (BookMeta) book.getItemMeta();

        if (page == 1) {
            boolean hasInBracket = pag.contains("["), hasOutBrackets = pag.contains("]");
            if (hasInBracket || hasOutBrackets) {
                if (!(hasInBracket && hasOutBrackets)) {
                    String missing = hasOutBrackets ? "[" : "]";
                    exceptionThrower.trowException(errorBasePath + "enclosure_not_found.singular",missing);
                }
            } else {
                exceptionThrower.trowException(errorBasePath + "enclosure_not_found.plural","[","]");
            }

            String regionName = StringUtils.substringBetween(pag,"[","]");
            int     length = regionName.length(),
                    minLength = Landlords.CONFIG.regionsMinimumNameLength,
                    maxLength = Landlords.CONFIG.regionsMaximumNameLength;

            if (length <= minLength) {
                exceptionThrower.trowException(errorBasePath + "illegal_name_length.short",Integer.toString(minLength),Integer.toString(length),Integer.toString(minLength - length));
            }
            if (length >= maxLength) {
                exceptionThrower.trowException(errorBasePath + "illegal_name_length.long",Integer.toString(maxLength),Integer.toString(length),Integer.toString(length - maxLength));
            }

            change = new Change(regionName,null);
            meta.setPage(page, getStyledFirstPage_(regionName));

        } else {
            if (pag.indexOf(':') == -1) {
                exceptionThrower.trowException(errorBasePath + "permission_separator.missing");
            }
            String[] sections = StringUtils.split(pag,":");
            if (sections.length > 2) {
                exceptionThrower.trowException(errorBasePath + "permission_separator.too_many",Integer.toString(sections.length-1));
            }

            Hierarchy hierarchy = this.getRegion().getHierarchy();
            Hierarchy.Group regionGroup = hierarchy.getGroup(sections[0]);
            if (regionGroup == null){
                exceptionThrower.trowException(errorBasePath + "group_not_found",sections[0]);
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
    public ItemStack convertToDeeds(ItemStack itemStack, Player holder) {
        itemStack.setType(Material.WRITABLE_BOOK);
        ItemMeta meta = itemStack.getItemMeta();
        PersistentDataContainer dataContainer = meta.getPersistentDataContainer();

        dataContainer.set(deedsIdNsKey_,PersistentDataType.STRING,this.getId().toString());
        dataContainer.set(deedsMinutesNsKey_,PersistentDataType.INTEGER,this.getMinutes());
        dataContainer.set(deedsRegionIdNsKey_,PersistentDataType.INTEGER,this.getRegion().getId());
        meta.addEnchant(Enchantment.BINDING_CURSE,1,true);
        meta.setDisplayName(Landlords.getLangProvider().getEntry(holder, "deeds.book.title", this.getRegion().getName()));

        itemStack.setItemMeta(meta);

        return itemStack;
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
                    Landlords.getLangProvider().getEntry(holder_,"deeds.error_message.compose.page_indicator",Integer.toString(this.page)) + "§r" +
                    Landlords.getLangProvider().getEntry(holder_,"deeds.error_message.compose.general_style") +
                    Landlords.getLangProvider().getEntry(holder_,path,parms) + "§r"
            );
        }
    }

    //PRIVATE METHODS
    private String getStyledFirstPage_(String name){
        String  nameField = "[" + name + "]",
                deedsId = String.format("%03d",this.getMinutes()),
                firstPage = Landlords.getLangProvider().getNonFormattedEntry(this.holder_,"deeds.book.first_page")+"§r";
        firstPage = firstPage.replaceAll("\\[","").replaceAll("]","");
        return String.format(firstPage,nameField,deedsId,this.getRegion().getId());
    }
    private String getStyledPermissionsPage_(Hierarchy.Group group, List<String> players){
        StringBuilder page = new StringBuilder();
        page.append(extractStyle_(Landlords.getLangProvider().getEntry(this.holder_,"deeds.book.styles.headers")))
                .append(group.getName())
                .append(":§r")
                .append(extractStyle_(Landlords.getLangProvider().getEntry(this.holder_,"deeds.book.styles.player_names")));
        for (String p : players) {
            page.append("\n").append(p);
        }
        return page.toString();
    }
    private void saveToRegion_() {
        Region region = this.getRegion();
        RegionDataContainer data = region.getDataContainer();
        data.remove(deedsIdContainerKey_);
        data.remove(deedsMinutesContainerKey_);
        data.add(new RegionData(deedsIdContainerKey_,this.getId().toString()));
        data.add(new RegionData(deedsMinutesContainerKey_,this.getMinutes()));
        region.save();
    }

    //PRIVATE STATIC METHODS
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