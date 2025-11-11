package com.pablogb.multimediasorterapp.model;

public class MultimediaMetadata {
    private long size;
    private Integer width;
    private Integer height;
    private Float duration;// in seconds, null for images
    private String type; // "image" or "video"

    public MultimediaMetadata() {}

    public MultimediaMetadata(long size, Integer width, Integer height, Float duration, String type) {
        this.size = size;
        this.width = width;
        this.height = height;
        this.duration = duration;
        this.type = type;
    }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }

    public Float getDuration() { return duration; }
    public void setDuration(Float duration) { this.duration = duration; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
