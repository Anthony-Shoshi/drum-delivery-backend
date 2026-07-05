package com.drum_delivery_backend.models;

public interface Identifiable<T>{
    T getId();
    void setId(T id);
}