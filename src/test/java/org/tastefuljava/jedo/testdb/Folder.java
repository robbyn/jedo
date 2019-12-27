package org.tastefuljava.jedo.testdb;

import java.util.Comparator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.tastefuljava.jedo.Ref;

public class Folder {
    public static Comparator<Folder> BY_NAME
            = (a,b)->a.name.compareTo(b.name);

    private int id;
    private final Ref<Folder> parent = new Ref<>();
    private String name;
    private String title;
    private String description;
    private final SortedSet<Folder> subfolders = new TreeSet<>(BY_NAME);
    private final SortedSet<Picture> pictures = new TreeSet<>(Picture.BY_NAME);

    public int getId() {
        return id;
    }

    public Folder getParent() {
        return parent.get();
    }

    public void setParentId(Folder parent) {
        this.parent.set(parent);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isRoot() {
        return parent.get() == null;
    }

    public Set<Folder> getSubfolders() {
        return subfolders;
    }

    public Set<Picture> getPictures() {
        return pictures;
    }
}
