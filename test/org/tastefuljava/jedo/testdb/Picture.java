package org.tastefuljava.jedo.testdb;

import java.awt.Dimension;
import java.util.Date;

public class Picture {
    private int id;
    private int folderId;
    private String name;
    private int width;
    private int height;

    public int getId() {
        return id;
    }

    public int getFolderId() {
        return folderId;
    }

    public void setFolderId(int folderId) {
        this.folderId = folderId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}
