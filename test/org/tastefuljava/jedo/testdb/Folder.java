package org.tastefuljava.jedo.testdb;

public class Folder {
    private int id;
    private Integer parentId;
    private String name;
    private String title;
    private String description;

    public int getId() {
        return id;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
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
        return parentId == null;
    }
}
