package com.drum_delivery_backend.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "client")
public class ClientModel implements Identifiable<String>{

    public static final String TABLE_NAME = "client";

    @Id
    @Size(max = 50, message = "Client ID must not exceed 50 characters")
    @Column(name = "id_client", nullable = false, unique = true, updatable = false)
    private String clientId;

    @NotBlank(message = "Client name is required")
    @Size(min = 2, max = 100, message = "Client name must be between 2 and 100 characters")
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Column(name = "email", nullable = false)
    private String email;

    @NotBlank(message = "Contact person is required")
    @Size(min = 2, max = 100, message = "Contact person name must be between 2 and 100 characters")
    @Column(name = "contact_person", nullable = false)
    private String contactPerson;

    @Pattern(regexp = "^[+]?[0-9\\s\\-()]{7,20}$", message = "Phone number format is invalid")
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    @Column(name = "phone", nullable = true)
    private String phone;

    @Size(max = 255, message = "Address must not exceed 255 characters")
    @Column(name = "address", nullable = true)
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

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference(value="client-order")
    private List<OrderModel> orders = new ArrayList<>();

    // Getters and setters
    @Override
    public String getId() {
        return clientId;
    }

    @Override
    public void setId(String id) {
        this.clientId = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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

    public List<OrderModel> getOrders() {
        return orders;
    }

    public void setOrders(List<OrderModel> orders) {
        this.orders = orders;
    }
}
