package org.tastefuljava.jedo.testdb;

import java.util.List;

public class Folder {
    private int id;
    private Folder parent;
    private String name;
    private String title;
    private String description;
    private List<Folder> subfolders;
    private List<Picture> pictures;

    public int getId() {
        return id;
    }

    public Folder getParent() {
        return parent;
    }

    public void setParentId(Folder parent) {
        this.parent = parent;
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
        return parent == null;
    }

    public List<Folder> getSubfolders() {
        return subfolders;
    }

    public List<Picture> getPictures() {
        return pictures;
    }
}
