package SerializationClasses;

public class Friend {
    private String name;
    private boolean active;

    public Friend(String name, boolean active) {
        this.name = name;
        this.active = active;
    }

    public String getName() {
        return this.name;
    }

    public boolean isActive() {
        return this.active;
    }
}

