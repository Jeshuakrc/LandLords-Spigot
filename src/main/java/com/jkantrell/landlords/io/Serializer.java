package com.jkantrell.landlords.io;

import com.google.gson.*;
import com.jkantrell.regionslib.regions.*;
import com.jkantrell.landlords.totemElements.TotemStructure;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public abstract class Serializer {

    //NEW IMPLEMENTATION
    public static final Gson GSON;
    static {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(TotemStructure.class, new TotemStructure.JDeserializer());

        GSON = builder.create();
    }

    public static class FILES {
        public static final File REGIONS = new File(ConfigManager.getConfigPath(), "regions.json");
        public static final File HIERARCHIES = new File(ConfigManager.getConfigPath(), "hierarchies.json");
        public static final File TOTEM_STRUCTURES = new File(ConfigManager.getConfigPath(), "totemStructures.json");
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
}
