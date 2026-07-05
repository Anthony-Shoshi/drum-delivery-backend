package com.drum_delivery_backend.models;

public enum ERole {
    ROLE_USER,       // Basic access to view data
    ROLE_OPERATOR,   // Can create and modify orders and deliveries
    ROLE_MANAGER,    // Access to reporting and analytics
    ROLE_ADMIN       // Full system access
}