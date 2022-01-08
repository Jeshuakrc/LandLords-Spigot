package mc.thejsuser.landlords.regionElements;

import com.google.gson.JsonPrimitive;

public class RegionData {

    //FIELDS
    private String key_;
    private JsonPrimitive val_;

    //CONSTRUCTOR
    public RegionData(String key, JsonPrimitive val){
        key_ = key;
        val_ = val;
    }
    public RegionData(String key, Number val){

        key_ = key;
        val_ = new JsonPrimitive(val);
    }
    public RegionData(String key, String val){

        key_ = key;
        val_ = new JsonPrimitive(val);
    }
    public RegionData(String key, char val){

        key_ = key;
        val_ = new JsonPrimitive(val);
    }
    public RegionData(String key, boolean val){

        key_ = key;
        val_ = new JsonPrimitive(val);
    }

    //GETTERS
    public String getKey(){
        return key_;
    }
    public int getAsInt(){
        return val_.getAsInt();
    }
    public byte getAsByte(){
        return val_.getAsByte();
    }
    public short getAsShort(){
        return val_.getAsShort();
    }
    public boolean getAsBoolean(){
        return val_.getAsBoolean();
    }
    public long getAsLong(){
        return val_.getAsLong();
    }
    public double getAsDouble(){
        return val_.getAsDouble();
    }
    public float getAsFloat(){
        return val_.getAsFloat();
    }
    public char getAsChar(){
        return val_.getAsCharacter();
    }
    public String getAsString(){
        return val_.getAsString();
    }
    public JsonPrimitive getValue() {
        return val_;
    }

    //SETTERS
    void setValue(JsonPrimitive val){
        val_ = val;
    }
    public void setValue(Number val){
        val_ = new JsonPrimitive(val);
    }
    public void setValue(String val){
        val_ = new JsonPrimitive(val);
    }
    public void setValue(char val){
        val_ = new JsonPrimitive(val);
    }
    public void setValue(boolean val){
        val_ = new JsonPrimitive(val);
    }

}
