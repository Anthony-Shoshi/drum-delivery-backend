package com.drum_delivery_backend.payload.request;

import jakarta.validation.constraints.NotBlank;

public class ResendTokenRequest {
    
    @NotBlank(message = "Username is required")
    private String username;
    
    public ResendTokenRequest() {}
    
    public ResendTokenRequest(String username) {
        this.username = username;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
}