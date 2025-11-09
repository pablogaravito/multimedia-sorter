package com.pablogb.multimediasorterapp.model;

public class Destination {
    private String name;
    private String key;
    private String path;

    public Destination() {}

    public Destination(String name, String key, String path) {
        this.name = name;
        this.key = key;
        this.path = path;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
}
