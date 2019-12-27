package org.tastefuljava.jedo.testdb;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class Picture {
    public static Comparator<Picture> BY_NAME
            = (a,b)->a.name.compareTo(b.name);

    private int id;
    private Folder folder;
    private String name;
    private Date timestamp = new Date();
    private int width;
    private int height;
    private GpsData gpsData;
    private List<Tag> tags = new ArrayList<>();

    public int getId() {
        return id;
    }

    public Folder getFolder() {
        return folder;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Dimension getSize() {
        return new Dimension(width, height);
    }

    public void setSize(int width, int height) {
        setWidth(width);
        setHeight(height);
    }

    public void setDimension(Dimension dim) {
        setSize(dim.width, dim.height);
    }

    public GpsData getGpsData() {
        return gpsData;
    }

    public void setGpsData(GpsData gpsData) {
        this.gpsData = gpsData;
    }

    public int tagCount() {
        return tags.size();
    }

    public Tag getTag(int index) {
        return tags.get(index);
    }

    public void addTag(Tag tag) {
        tags.add(tag);
    }

    public void removeTag(int index) {
        tags.remove(index);
    }
}
