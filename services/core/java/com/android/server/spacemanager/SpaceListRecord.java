package com.android.server.spacemanager;

import java.util.ArrayList;

public class SpaceListRecord {
    
    private Space space;
    private ArrayList<RestrictionRecord> restrictionRecords;
    private ArrayList<DelegationRecord> delegationRecords;

    // constructor
    public SpaceListRecord() {
        this.restrictionRecords = new ArrayList<RestrictionRecord>();
        this.delegationRecords = new ArrayList<DelegationRecord>();
    }

    // set space
    public void setSpace(Space space) {
        this.space = space;
    }

    // get space
    public Space getSpace() {
        return this.space;
    }

    // add restriction record
    public void addRestrctionRecord(String enforcerId, String permission, String appId) {
        RestrictionRecord record = new RestrictionRecord();
        record.setEnforcer(enforcerId);
        record.setPermission(permission);
        record.setAppId(appId);
        this.restrictionRecords.add(record);
    }

    // get restriction records
    public ArrayList<RestrictionRecord> getRestrictionRecords() {
        return this.restrictionRecords;
    }

    // add delegation record
    public void addDelegationRecord(Space space, String delegatorId) {
        DelegationRecord record = new DelegationRecord();
        record.setSpace(space);
        record.setDelegator(delegatorId);
        this.delegationRecords.add(record);
    }

    // get delegation records
    public ArrayList<DelegationRecord> getDelegationRecords() {
        return this.delegationRecords;
    }

    // get delegator of space where coordinate is located
    // return null if coordinate is not within any delegated space
    public String getDelegator(Coordinate coordinate) {
        for (int i = 0; i < delegationRecords.size(); i++) {
            if (delegationRecords.get(i).getSpace().isContains(coordinate)) {
                return delegationRecords.get(i).getDelegator();
            }
        }
        return null;
    }
}