package org.tastefuljava.jedo.testdb;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.tastefuljava.jedo.Ref;

public class Folder extends Named {
    private final Ref<Folder> parent = new Ref<>();
    private String title;
    private String description;
    private final SortedSet<Folder> subfolders = new TreeSet<>(BY_NAME);
    private final SortedSet<Picture> pictures = new TreeSet<>(Picture.BY_NAME);

    public Folder getParent() {
        return parent.get();
    }

    public void setParentId(Folder parent) {
        this.parent.set(parent);
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
