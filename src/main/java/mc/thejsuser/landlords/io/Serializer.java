package mc.thejsuser.landlords.io;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import mc.thejsuser.landlords.regions.*;
import mc.thejsuser.landlords.regions.dataContainers.RegionData;
import mc.thejsuser.landlords.regions.dataContainers.RegionDataContainer;
import mc.thejsuser.landlords.totemElements.*;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class Serializer {

    //NEW IMPLEMENTATION
    public static final Gson GSON;
    static {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Hierarchy.class, new Hierarchy.JDeserializer());
        builder.registerTypeAdapter(Region.class, new Region.JSerializer());
        builder.registerTypeAdapter(Region.class, new Region.JDeserializer());
        builder.registerTypeAdapter(Permission.class, new Permission.JSerializer());
        builder.registerTypeAdapter(RegionData.class, new RegionData.JSerializer());
        builder.registerTypeAdapter(RegionData.class, new RegionData.JDeserializer());
        builder.registerTypeAdapter(RegionDataContainer.class, new RegionDataContainer.JSerializer());
        builder.registerTypeAdapter(RegionDataContainer.class, new RegionDataContainer.JDeserializer());

        GSON = builder.create();
    }

    public static class FILES {
        public static final File REGIONS = new File(ConfigManager.getConfigPath(), "regions.json");
        public static final File HIERARCHIES = new File(ConfigManager.getConfigPath(), "hierarchies.json");
    }

    public static <T> T deserializeFile (File file, Class<T> typeOf) {
        Serializer.ensureFileExistence(file, false);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            return Serializer.GSON.fromJson(reader,typeOf);
        } catch (FileNotFoundException e) {
            return null;
        }
    }
    public static <T> List<T> deserializeFileList(File file, Class<T> typeOf) {
        Serializer.ensureFileExistence(file, true);
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
            ArrayList<T> list = new ArrayList<>();
            for (JsonElement element : array) {
                list.add(GSON.fromJson(element,typeOf));
            }
            return list;
        } catch (FileNotFoundException e) {
            return null;
        }
    }
    public static String serialize (Object toSerialize) {
        return GSON.toJson(toSerialize);
    }
    public static void serializeToFile(File file, Object toSerialize) {
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(serialize(toSerialize));
            writer.flush();
        } catch (IOException e) {

        }

    }

    private static void ensureFileExistence (File file, boolean array) {
        if (file.exists()) { return; }
        file.getParentFile().mkdirs();

        if (!array) { return; }
        try {
            FileWriter writer = new FileWriter(file);
            writer.write("[]");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //OLD IMPLEMENTATION - working on getting rid of it"dimension"
    private static final File totemStructuresFile_ = new File(ConfigManager.getConfigPath(), "totemStructures.json");

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

    private static TotemStructure loadTotemStructureFromJson(JsonObject jsonTotemStructure) {

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

    public static TotemStructure[] loadTotemStructures() {

        totemStructuresEnsureExistence();

        JsonArray list = getJsonFromFile(totemStructuresFile_).getAsJsonArray();
        TotemStructure[] structures = new TotemStructure[list.size()];
        for (int i = 0; i < list.size(); i++) {
            structures[i]=loadTotemStructureFromJson(list.get(i).getAsJsonObject());
        }
        return structures;
    }
}
