package com.drum_delivery_backend.models.validation;

/**
 * Validation groups for different operations
 */
public class ValidationGroups {
    
    /**
     * Validation group for creating new entities
     */
    public interface OnCreate {}
    
    /**
     * Validation group for updating existing entities
     */
    public interface OnUpdate {}
}