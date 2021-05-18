package com.android.server.spacemanager;

import java.util.ArrayList;

public class DelegationRecord {
    
    private Space space;
    private String delegator;

    // constructor
    public DelegationRecord() {}

    // set space
    public void setSpace(Space space) {
        this.space = space;
    }
    
    // get space
    public Space getSpace() {
        return this.space;
    }

    // set delegator
    public void setDelegator(String delegatorId) {
    	this.delegator = delegatorId;
    }
    
    // get delegator
    public String getDelegator() {
        return this.delegator;
    }
}