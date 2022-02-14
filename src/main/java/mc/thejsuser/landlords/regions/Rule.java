package mc.thejsuser.landlords.regions;

public class Rule <T> {

    //FIELDS
    private T value_;
    private final String name_;

    //CONSTRUCTORS
    public Rule(String name,T value) {
        this.set(value);
        this.name_ = name;
    }

    //SETTERS
    public void set(T value) {
        this.value_ = value;
    }

    //GETTERS
    public T getValue() {
        return this.value_;
    }
    public String getName() {
        return this.name_;
    }
}
