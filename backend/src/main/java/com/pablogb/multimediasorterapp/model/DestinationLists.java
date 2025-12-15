package com.pablogb.multimediasorterapp.model;

import java.util.List;
import java.util.Map;

public class DestinationLists {
    private Map<String, List<Destination>> lists;

    public DestinationLists() {}

    public DestinationLists(Map<String, List<Destination>> lists) {
        this.lists = lists;
    }

    public Map<String, List<Destination>> getLists() {
        return lists;
    }

    public void setLists(Map<String, List<Destination>> lists) {
        this.lists = lists;
    }
}
