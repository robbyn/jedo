package org.tastefuljava.jedo.testdb;

public class Tag {
    private int id;
    private String name;

    @Deprecated // only for instanciation via reflection
    public Tag() {
    }

    public Tag(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
