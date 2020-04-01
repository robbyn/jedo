package org.tastefuljava.jedo.testdb;

import java.util.Comparator;

public class Named {
    public static Comparator<Named> BY_NAME
            = (a,b)->a.name.compareTo(b.name);

    private int id;
    private String name;

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
