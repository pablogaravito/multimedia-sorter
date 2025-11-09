package com.pablogb.multimediasorterapp.model;

import java.util.List;
import java.util.Map;

public class SortRequest {
    private String sourcePath;
    private List<Destination> destinations;
    private Map<String, String> classifications; // imagePath -> destinationName

    public SortRequest() {}

    public String getSourcePath() { return sourcePath; }
    public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }

    public List<Destination> getDestinations() { return destinations; }
    public void setDestinations(List<Destination> destinations) { this.destinations = destinations; }

    public Map<String, String> getClassifications() { return classifications; }
    public void setClassifications(Map<String, String> classifications) { this.classifications = classifications; }
}
