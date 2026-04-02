package com.group5.paul_esys.modules.users.services;

import com.group5.paul_esys.modules.users.models.user.UserInformation;

public class UserSession {

    private static UserSession instance;
    private UserInformation userInformation;
    private long timestamp;

    private UserSession() {
        if (instance != null) {
            throw new RuntimeException("UserSession already exists. Use getInstance() instead.");
        }
    }

    public static synchronized UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public UserInformation getUserInformation() {
        return userInformation;
    }

    public void setUserInformation(UserInformation userInformation) {
        this.userInformation = userInformation;
        this.timestamp = System.currentTimeMillis();
    }

    public void logout() {
        this.userInformation = null;
        instance = null;
    }

}
