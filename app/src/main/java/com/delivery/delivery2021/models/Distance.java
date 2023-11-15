package com.delivery.delivery2021.models;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Distance {
    private String userID;
    private double distance;

    public Distance() {

    }

    public Distance(String userID, double distance) {
        this.userID = userID;
        this.distance = distance;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }
}
