package com.drum_delivery_backend.payload.response;

public class TwoFactorRequiredResponse {
    
    private String message;
    private String username;
    private boolean twoFactorRequired;
    
    public TwoFactorRequiredResponse() {}
    
    public TwoFactorRequiredResponse(String message, String username, boolean twoFactorRequired) {
        this.message = message;
        this.username = username;
        this.twoFactorRequired = twoFactorRequired;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public boolean isTwoFactorRequired() {
        return twoFactorRequired;
    }
    
    public void setTwoFactorRequired(boolean twoFactorRequired) {
        this.twoFactorRequired = twoFactorRequired;
    }
}