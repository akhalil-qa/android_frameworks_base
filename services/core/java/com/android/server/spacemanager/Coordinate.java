package com.android.server.spacemanager;

public class Coordinate {

    private double latitude;
    private double longitude;
    private double altitude;

    // constructor
    public Coordinate() {
        this.latitude = 0;
        this.longitude = 0;
        this.altitude = 0;
    }
    
    // constructor
    public Coordinate(double latitude, double longitude, double altitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
    }

    // get latitude
    public double getLatitude() {
        return this.latitude;
    }

    // set latitude
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    // get longitude
    public double getLongitude() {
        return this.longitude;
    }

    // set longitude
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    // get altitude
    public double getAltitude() {
        return this.altitude;
    }

    // set altitude
    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }
}
