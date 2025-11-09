package com.pablogb.multimediasorterapp.model;

import java.util.List;
import java.util.Map;

public class SessionState {
    private String sourcePath;
    private List<Destination> destinations;
    private Map<String, String> classifications;
    private int currentIndex;
    private long lastSaved;

    public SessionState() {
        this.lastSaved = System.currentTimeMillis();
    }

    public String getSourcePath() { return sourcePath; }
    public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }

    public List<Destination> getDestinations() { return destinations; }
    public void setDestinations(List<Destination> destinations) { this.destinations = destinations; }

    public Map<String, String> getClassifications() { return classifications; }
    public void setClassifications(Map<String, String> classifications) { this.classifications = classifications; }

    public int getCurrentIndex() { return currentIndex; }
    public void setCurrentIndex(int currentIndex) { this.currentIndex = currentIndex; }

    public long getLastSaved() { return lastSaved; }
    public void setLastSaved(long lastSaved) { this.lastSaved = lastSaved; }
}
