package com.android.server.spacemanager;

import java.util.ArrayList;

public class SpaceAuthority {
    
    private String id;
    private ArrayList<SpaceListRecord> spaceList;
    
    // constructor
    public SpaceAuthority() {
        this.spaceList = new ArrayList<SpaceListRecord>();
    }

    // set id
    public void setId(String id) {
        this.id = id;
    }

    // get id
    public String getId() {
        return this.id;
    }

    // add space list record
    public void addSpaceListRecord(SpaceListRecord record) {
        this.spaceList.add(record);
    }

    // get space list records
    public ArrayList<SpaceListRecord> getSpaceListRecords() {
        return this.spaceList;
    }
 
    // get space list record
    // return null if coordinate is not within a space under space authority control
    public SpaceListRecord getSpaceListRecord(Coordinate coordinate) {
        for (SpaceListRecord spaceListRecord : this.spaceList) {
            if (spaceListRecord.getSpace().isContains(coordinate)) {
                return spaceListRecord;
            }
        }
        return null;
    }
}