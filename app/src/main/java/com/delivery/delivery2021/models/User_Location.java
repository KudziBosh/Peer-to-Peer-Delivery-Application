package com.delivery.delivery2021.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class User_Location implements Parcelable {
    private GeoPoint geo_point;
    private @ServerTimestamp Date timestamp;
    private User user;

    public User_Location(GeoPoint geo_point, Date timestamp, User user) {
        this.geo_point = geo_point;
        this.timestamp = timestamp;
        this.user = user;
    }
    public User_Location() {

    }

    protected User_Location(Parcel in) {
        user = in.readParcelable(User.class.getClassLoader());
    }

    public static final Creator<User_Location> CREATOR = new Creator<User_Location>() {
        @Override
        public User_Location createFromParcel(Parcel in) {
            return new User_Location(in);
        }

        @Override
        public User_Location[] newArray(int size) {
            return new User_Location[size];
        }
    };

    public GeoPoint getGeo_point() {
        return geo_point;
    }

    public void setGeo_point(GeoPoint geo_point) {
        this.geo_point = geo_point;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "User_Location{" +
                "geo_point=" + geo_point +
                ", timestamp='" + timestamp + '\'' +
                ", user=" + user +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(user, flags);
    }
}
