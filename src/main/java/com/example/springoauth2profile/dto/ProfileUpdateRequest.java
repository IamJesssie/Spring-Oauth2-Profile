package com.example.springoauth2profile.dto;

public class ProfileUpdateRequest {

    private String displayName;
    private String bio;

    // Default constructor
    public ProfileUpdateRequest() {}

    // Getters and setters
    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}
