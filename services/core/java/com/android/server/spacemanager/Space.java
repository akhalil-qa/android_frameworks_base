package com.android.server.spacemanager;

public class Space {
    
    private String id;
    private Polygon boundary;
    
    // constructor
    public Space() {
    	this.boundary = new Polygon();
    }

    // set id
    public void setId(String id) {
        this.id = id;
    }

    // get id
    public String getId() {
        return this.id;
    }
    
    // add boundary coordinate
    public void addBoundaryCoordinate(double latitude, double longitude, double altitude) {
    	this.boundary.addCoordinate(latitude, longitude, altitude);
    }
  
    // get boundary
    public Polygon getBoundary() {
        return this.boundary;
    }

    // check if a the space contained a given coordinate
    public boolean isContains(Coordinate coordinate) {
        return this.boundary.isContains(coordinate.getLatitude(), coordinate.getLongitude(), coordinate.getAltitude());
    }
}