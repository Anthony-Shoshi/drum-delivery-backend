package com.drum_delivery_backend.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "site")
public class SiteModel implements Identifiable<String>{

    public static final String TABLE_NAME = "site";

    @Id
    @Size(max = 50, message = "Site ID must not exceed 50 characters")
    @Column(name = "id_site", nullable = false, unique = true, updatable = false)
    private String siteId;

    @NotBlank(message = "Site name is required")
    @Size(min = 2, max = 100, message = "Site name must be between 2 and 100 characters")
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank(message = "Address is required")
    @Size(min = 5, max = 255, message = "Address must be between 5 and 255 characters")
    @Column(name = "address", nullable = false)
    private String address;

    @Size(max = 100, message = "City must not exceed 100 characters")
    @Column(name = "city", nullable = true)
    private String city;

    @Size(max = 100, message = "State must not exceed 100 characters")
    @Column(name = "state", nullable = true)
    private String state;

    @Size(max = 100, message = "Country must not exceed 100 characters")
    @Column(name = "country", nullable = true)
    private String country;

    @Pattern(regexp = "^[0-9A-Za-z\\s\\-]{3,20}$", message = "Postal code format is invalid")
    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    @Column(name = "postal_code", nullable = true)
    private String postalCode;

    @Size(min = 2, max = 100, message = "Contact person name must be between 2 and 100 characters")
    @Column(name = "contact_person", nullable = true)
    private String contactPerson;

    @Pattern(regexp = "^[+]?[0-9\\s\\-()]{7,20}$", message = "Contact phone format is invalid")
    @Size(max = 20, message = "Contact phone must not exceed 20 characters")
    @Column(name = "contact_phone", nullable = true)
    private String contactPhone;

    @OneToMany(mappedBy = "destinationSite", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ShipmentModel> shipments = new ArrayList<>();

    // Getters and setters
    @Override
    public String getId() {
        return siteId;
    }

    @Override
    public void setId(String id) {
        this.siteId = id;
    }

    public String getSiteId() {
        return siteId;
    }

    public void setSiteId(String siteId) {
        this.siteId = siteId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public List<ShipmentModel> getShipments() {
        return shipments;
    }

    public void setShipments(List<ShipmentModel> shipments) {
        this.shipments = shipments;
    }
}
