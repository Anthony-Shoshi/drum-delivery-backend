package com.drum_delivery_backend.models;

public enum DrumStatus {
    AVAILABLE("Available for assignment"),
    IN_ORDER("Assigned to order"),
    IN_SHIPMENT("In shipment"),
    DELIVERED("Delivered to destination"),
    MISSING("Reported missing"),
    DAMAGED("Damaged or unusable");

    private final String description;

    DrumStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}