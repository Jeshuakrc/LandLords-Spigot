package mc.thejsuser.landlords.io;

import com.google.gson.*;
import mc.thejsuser.landlords.regionElements.*;
import mc.thejsuser.landlords.totemElements.*;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public abstract class JsonManager {

    //FIELDS
    private static final File regionsFile_ = new File(ConfigManager.getConfigPath(), "regions.json");
    private static final File hierarchiesFile_ = new File(ConfigManager.getConfigPath(), "hierarchies.json");
    private static final File totemStructuresFile_ = new File(ConfigManager.getConfigPath(), "totemStructures.json");

    //PRIVATE METHODS
    private static void regionsEnsureExistence() {

        if(!regionsFile_.exists()){
            regionsFile_.getParentFile().mkdirs();

            FileWriter writer = null;
            try {
                writer = new FileWriter(regionsFile_);
                writer.write("[]");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }
    private static void hierarchiesEnsureExistence() {

        if(!hierarchiesFile_.exists()){
            regionsFile_.getParentFile().mkdirs();

            FileWriter writer = null;
            try {
                writer = new FileWriter(hierarchiesFile_);
                writer.write("[]");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }
    private static void totemStructuresEnsureExistence() {

        if(!totemStructuresFile_.exists()){
            totemStructuresFile_.getParentFile().mkdirs();

            FileWriter writer = null;
            try {
                writer = new FileWriter(totemStructuresFile_);
                writer.write("[]");
                writer.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }
    private static JsonElement getJsonFromFile(File file){
        JsonElement element = null;
        try {
            JsonParser parser = new JsonParser();
            element = parser.parse(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return element;
    }

    //CREATE OBJECTS
    private static Region loadRegionFromJson(JsonObject jsonRegion) {

        Hierarchy hierarchy = Hierarchy.getHierarchy(jsonRegion.get("hierarchy").getAsInt());

        JsonArray vertJsonArray = jsonRegion.getAsJsonArray("vertex");
        double[] vertArray = new double[6];
        for (int j = 0; j < vertJsonArray.size(); j++) {
            vertArray[j] = vertJsonArray.get(j).getAsDouble();
        }

        JsonArray permJsonArray = jsonRegion.getAsJsonArray("permissions");
        Permission[] permArray = new Permission[permJsonArray.size()];
        for (int j = 0; j < permJsonArray.size(); j++) {
            JsonObject permJson = permJsonArray.get(j).getAsJsonObject();
            Permission perm = loadPermissionFromJson(permJson,hierarchy);
            permArray[j]=perm;
        }

        JsonArray dataJsonArray = jsonRegion.getAsJsonArray("data_container");
        RegionDataContainer dataContainer = new RegionDataContainer();
        for (int j = 0; j < dataJsonArray.size(); j++) {
            JsonObject dataJson = dataJsonArray.get(j).getAsJsonObject();
            RegionData data = loadRegionDataFromJson(dataJson);
            dataContainer.add(data);
        }

        Region r = new Region(
                vertArray,
                World.Environment.valueOf(jsonRegion.get("dimension").getAsString()),
                permArray,
                jsonRegion.get("name").getAsString(),
                hierarchy
        );
        r.setId(jsonRegion.get("id").getAsInt());
        r.enabled(jsonRegion.get("enabled").getAsBoolean());
        r.setDataContainer(dataContainer);

        return r;
    }
    private static RegionData loadRegionDataFromJson(JsonObject jsonData){

        String key = jsonData.get("key").getAsString();
        JsonPrimitive val = jsonData.get("value").getAsJsonPrimitive();
        return new RegionData(key,val);
    }
    private static Permission loadPermissionFromJson(JsonObject jsonPermission, Hierarchy hierarchy){
        return new Permission(
                jsonPermission.get("player_name").getAsString(),
                hierarchy,
                jsonPermission.get("level").getAsInt()
        );

    }
    private static Hierarchy loadHierarchyFromJson(JsonObject jsonHierarchy) {
        Hierarchy hierarchy = new Hierarchy(
            jsonHierarchy.get("id").getAsInt(),
            jsonHierarchy.get("name").getAsString()
        );
        JsonArray groupJsonList = jsonHierarchy.getAsJsonArray("groups");
        for (JsonElement jsonGroupElement : groupJsonList) {
            JsonObject jsonGroup = jsonGroupElement.getAsJsonObject();
            loadGroupFromJson(jsonGroup,hierarchy);
        }
        return hierarchy;
    }
    private static void loadGroupFromJson(JsonObject jsonGroup, Hierarchy hierarchy){

        JsonArray abltJsonList = jsonGroup.getAsJsonArray("abilities");
        List<Ability> abltList = new ArrayList<>();
        for (int j = 0; j < abltJsonList.size(); j++){
            String abltString = abltJsonList.get(j).getAsString();
            abltList.add(Ability.valueOf(abltString));
        }

        hierarchy.getGroups().add(
                jsonGroup.get("name").getAsString(),
                jsonGroup.get("level").getAsInt(),
                abltList
        );
    }
    private static TotemStructure loadTotemStructureFromJson(JsonObject jsonTotemStructure)     {

        JsonArray list = jsonTotemStructure.getAsJsonArray("elements");
        TotemElement[] elements = new TotemElement[list.size()];
        for (int i = 0; i < list.size(); i++){
            elements[i]=loadTotemElementFromJson(list.get(i).getAsJsonObject());
        }
        list = jsonTotemStructure.getAsJsonArray("lecterns");
        TotemLectern[] lecterns = new TotemLectern[list.size()];
        for (int i = 0; i < list.size(); i++){
            lecterns[i] = loadTotemLecternFromJson(list.get(i).getAsJsonObject());
        }
        list = jsonTotemStructure.get("region_initial_size").getAsJsonArray();
        double[] regionBaseSize = new double[6];
        for (int i = 0; i < regionBaseSize.length; i++){
            regionBaseSize[i] = list.get(i).getAsDouble();
        }
        list = jsonTotemStructure.get("region_growth_rate").getAsJsonArray();
        double[] regionGrowthRate = new double[6];
        for (int i = 0; i < regionGrowthRate.length; i++){
            regionGrowthRate[i] = list.get(i).getAsDouble();
        }
        list = jsonTotemStructure.get("region_max_size").getAsJsonArray();
        double[] regionMaxSixe = new double[3];
        for (int i = 0; i < regionMaxSixe.length; i++){
            regionMaxSixe[i] = list.get(i).getAsDouble();
        }
        return new TotemStructure(
                elements,
                lecterns,
                regionBaseSize,
                regionGrowthRate,
                regionMaxSixe,
                Hierarchy.getHierarchy(jsonTotemStructure.get("region_hierarchy").getAsInt())
        );
    }
    private static TotemElement loadTotemElementFromJson(JsonObject jsonTotemElement){

        TotemElement r = null;
        String type = jsonTotemElement.get("type").getAsString();
        JsonArray posJson = jsonTotemElement.get("position").getAsJsonArray();
        int [] pos = new int[3];
        for (int i = 0; i < pos.length; i++){
            pos[i] = posJson.get(i).getAsInt();
        }
        String name = jsonTotemElement.get("name").getAsString();
        if (type.equals("block")) {
            Material block = Material.valueOf(name);
            r = new TotemBlock(block,pos[0],pos[1],pos[2]);
        }
        if (type.equals("entity")) {
            EntityType entity = EntityType.valueOf(name);
            r = new TotemEntity(entity,pos[0],pos[1],pos[2]);
        }
        return r;
    }
    private static TotemLectern loadTotemLecternFromJson(JsonObject jsonTotemLectern){

        JsonArray posJson = jsonTotemLectern.getAsJsonArray("position");
        int[] pos = new int[3];
        for (int i = 0; i < posJson.size(); i++) {
            pos[i] = posJson.get(i).getAsInt();
        }
        BlockFace facing = BlockFace.valueOf(jsonTotemLectern.get("direction").getAsString());

        return new TotemLectern(pos[0], pos[1], pos[2], facing);
    }

    //LOAD OBJECTS
    public static Hierarchy[] loadHierarchies(){

        hierarchiesEnsureExistence();

        JsonArray list = getJsonFromFile(hierarchiesFile_).getAsJsonArray();
        Hierarchy[] hierarchies = new Hierarchy[list.size()];
        for (int i = 0; i < list.size(); i++) {
            hierarchies[i] = loadHierarchyFromJson(list.get(i).getAsJsonObject());
        }
        return hierarchies;
    }
    public static List<Region> loadRegions(){
        regionsEnsureExistence();

        JsonArray list = getJsonFromFile(regionsFile_).getAsJsonArray();
        List<Region> regions = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            regions.add(loadRegionFromJson(list.get(i).getAsJsonObject()));
        }
        return regions;
    }
    public static TotemStructure[] loadTotemStructures() {

        totemStructuresEnsureExistence();

        JsonArray list = getJsonFromFile(totemStructuresFile_).getAsJsonArray();
        TotemStructure[] structures = new TotemStructure[list.size()];
        for (int i = 0; i < list.size(); i++) {
            structures[i]=loadTotemStructureFromJson(list.get(i).getAsJsonObject());
        }
        return structures;
    }

    public static JsonObject generateJsonFromRegion(Region region) {

        JsonObject jsonRegion = new JsonObject();
        JsonArray jsonVertex = new JsonArray();
        JsonArray jsonPermissions = new JsonArray();
        JsonArray jsonData = new JsonArray();

        for (double i : region.getVertex()) {
            jsonVertex.add(i);
        }
        for (Permission i : region.getPermissions()){
            jsonPermissions.add(generateJsonFromPermission(i));
        }
        RegionDataContainer container = region.getDataContainer();
        for (int i = 0; i < container.size(); i++) {

            JsonObject obj = new JsonObject();
            RegionData data = container.get(i);
            obj.addProperty("key",data.getKey());
            obj.add("value",data.getValue());

            jsonData.add(obj);
        }

        jsonRegion.addProperty("id",region.getId());
        jsonRegion.addProperty("dimension",region.getDimension().toString());
        jsonRegion.addProperty("name",region.getName());
        jsonRegion.addProperty("hierarchy",region.getHierarchy().getId());
        jsonRegion.addProperty("enabled",region.isEnabled());
        jsonRegion.add("vertex",jsonVertex);
        jsonRegion.add("permissions",jsonPermissions);
        jsonRegion.add("data_container",jsonData);

        return jsonRegion;
    }
    public static JsonObject generateJsonFromPermission(Permission permission){

        JsonObject jsonPermission = new JsonObject();

        jsonPermission.addProperty("player_name",permission.getPlayerName());
        jsonPermission.addProperty("level",permission.getGroup().getLevel());

        return jsonPermission;
    }

    public static void saveRegionNew(Region region) {

        JsonArray jsonRegions = getJsonFromFile(regionsFile_).getAsJsonArray();
        jsonRegions.add(generateJsonFromRegion(region));

        try {
            FileWriter writer = new FileWriter(regionsFile_);
            writer.write(jsonRegions.toString());
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public static void saveRegionOverwrite(Region region, int id) {

        JsonArray jsonRegions = getJsonFromFile(regionsFile_).getAsJsonArray();
        JsonObject jsonRegionNew = generateJsonFromRegion(region);
        jsonRegionNew.remove("id");
        jsonRegionNew.addProperty("id", id);

        for (int i = 0; i < jsonRegions.size(); i++) {
            JsonObject jsonRegionOld = jsonRegions.get(i).getAsJsonObject();
            if (jsonRegionOld.get("id").getAsInt() == id) {
                jsonRegions.remove(i);
                jsonRegions.add(jsonRegionNew);
                break;
            }
        }
        try {
            FileWriter writer = new FileWriter(regionsFile_);
            writer.write(jsonRegions.toString());
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void removeRegion(Region region) {

        JsonArray jsonRegions = getJsonFromFile(regionsFile_).getAsJsonArray();
        int regionId = region.getId();
        Integer index = null;
        for(int i = 0; i<jsonRegions.size(); i++){
            int id = jsonRegions.get(i).getAsJsonObject().get("id").getAsInt();
            if(id == regionId){
                index = i;
                break;
            }
        }
        if (index != null) {
            jsonRegions.remove(index);
        }

        try {
            FileWriter writer = new FileWriter(regionsFile_);
            writer.write(jsonRegions.toString());
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
