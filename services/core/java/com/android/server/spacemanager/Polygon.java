package com.android.server.spacemanager;

import java.util.ArrayList;

public class Polygon {

    private ArrayList<Coordinate> coordinates;

    // constructor
    public Polygon() {
        this.coordinates = new ArrayList<Coordinate>();
    }

    // add coordinate to draw the polygon
    public void addCoordinate(double latitude, double longitude, double altitude) {
        this.coordinates.add(new Coordinate(latitude, longitude, altitude));
    }

    // get polygon coordinates
    public ArrayList<Coordinate> getCoordinates() {
        return this.coordinates;
    }

    // check if a the polygon contained a given coordinate
    // See: https://stackoverflow.com/questions/8721406/how-to-determine-if-a-point-is-inside-a-2d-convex-polygon
    public boolean isContains(double latitude, double longitude, double altitude) {
    	
    	Coordinate coordinate = new Coordinate(latitude, longitude, altitude);
    	
        int i;
        int j;
        boolean result = false;

        for (i = 0, j = this.coordinates.size() - 1; i < this.coordinates.size(); j = i++) {
            if ((this.coordinates.get(i).getLongitude() > coordinate.getLongitude()) != (this.coordinates.get(j).getLongitude() > coordinate.getLongitude()) &&
                (coordinate.getLatitude() < (this.coordinates.get(j).getLatitude() - this.coordinates.get(i).getLatitude()) * (coordinate.getLongitude() - this.coordinates.get(i).getLongitude()) / (this.coordinates.get(j).getLongitude()-this.coordinates.get(i).getLongitude()) + this.coordinates.get(i).getLatitude())) {
                result = !result;
            }
        }
        return result;
    }
    
}