package com.delivery.delivery2021;

import android.app.Application;

import com.delivery.delivery2021.models.User;
import com.delivery.delivery2021.models.User_Location;


public class UserClient extends Application {

    private User user = null;
    private User_Location mUser_location=null;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User_Location getUser_location() {
        return mUser_location;
    }

    public void setUser_location(User_Location user_location) {
        mUser_location = user_location;
    }
}
