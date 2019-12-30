package org.tastefuljava.jedo.testdb;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Picture {
    public static Comparator<Picture> BY_NAME
            = (a,b)->{
                return a.name.compareTo(b.name);
            };
    private int id;
    private Folder folder;
    private String name;
    private Date timestamp = new Date();
    private int width;
    private int height;
    private GpsData gpsData;
    private final List<String> tags = new ArrayList<>();
    private final Map<String,String> descriptions = new HashMap<>();

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

    public String getTag(int index) {
        return tags.get(index);
    }

    public void addTag(String tag) {
        tags.add(tag);
    }

    public void removeTag(int index) {
        tags.remove(index);
    }

    public String getDescription(String language) {
        return descriptions.get(language);
    }

    public void setDescription(String language, String text) {
        if (text == null) {
            descriptions.remove(language);
        } else {
            descriptions.put(language, text);
        }
    }

    public Set<String> getDescriptionLanguages() {
        return descriptions.keySet();
    }
}
