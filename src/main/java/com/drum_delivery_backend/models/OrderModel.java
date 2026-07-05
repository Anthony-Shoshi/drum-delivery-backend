package com.drum_delivery_backend.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "orders")
public class OrderModel implements Identifiable<String> {

    public static final String TABLE_NAME = "orders";

    @Id
    @Size(max = 50, message = "Order ID must not exceed 50 characters")
    @Column(name = "id_order", nullable = false, unique = true, updatable = false)
    @JsonProperty("id")
    private String orderId;

    @NotBlank(message = "Order number is required")
    @Size(max = 50, message = "Order number must not exceed 50 characters")
    @Pattern(regexp = "^[A-Za-z0-9\\-_]+$", message = "Order number can only contain alphanumeric characters, hyphens, and underscores")
    @Column(name = "order_number", nullable = false, unique = true, updatable = false)
    private String orderNumber;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Column(name = "description", nullable = true)
    private String description;

    // Quantity is now calculated from drums, but keeping column for backward compatibility
    @Column(name = "quantity", nullable = true)
    private Integer quantity;

    @Size(max = 20, message = "Unit must not exceed 20 characters")
    @Pattern(regexp = "^[A-Za-z\\s]+$", message = "Unit can only contain letters and spaces")
    @Column(name = "unit", nullable = true)
    private String unit;

    @NotNull(message = "Creation date is required")
    @PastOrPresent(message = "Creation date cannot be in the future")
    @Column(name = "creation_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @NotNull(message = "Client is required")
    @ManyToOne
    @JoinColumn(name = "id_client", nullable = false)
    @JsonBackReference(value="client-order")
    private ClientModel client;

    @ManyToOne
    @JoinColumn(name = "id_shipment", nullable = true)
    @JsonBackReference(value="shipment-order")
    private ShipmentModel shipment;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference(value = "order-drums")
    private List<DrumModel> drums = new ArrayList<>();

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(CREATED|ASSIGNED_TO_SHIPMENT|COMPLETED|CANCELLED)$", message = "Status must be one of: CREATED, ASSIGNED_TO_SHIPMENT, COMPLETED, CANCELLED")
    @Column(name = "status", nullable = false)
    private String status; // CREATED, ASSIGNED_TO_SHIPMENT, COMPLETED, etc.

    // Constructor, getters, and setters
    public OrderModel() {
        this.creationDate = new Date();
        this.status = "CREATED";
    }

    @Override
    public String getId() {
        return orderId;
    }

    @Override
    public void setId(String id) {
        this.orderId = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public ClientModel getClient() {
        return client;
    }

    public void setClient(ClientModel client) {
        this.client = client;
    }

    // Add clientId getter for JSON serialization
    public String getClientId() {
        return client != null ? client.getId() : null;
    }

    // Add clientName getter for JSON serialization
    public String getClientName() {
        return client != null ? client.getName() : null;
    }

    public ShipmentModel getShipment() {
        return shipment;
    }

    public void setShipment(ShipmentModel shipment) {
        this.shipment = shipment;
    }

    // Add shipmentId getter for JSON serialization
    public String getShipmentId() {
        return shipment != null ? shipment.getId() : null;
    }

    // Add shipmentNumber getter for JSON serialization (for display purposes)
    public String getShipmentNumber() {
        return shipment != null ? shipment.getShipmentNumber() : null;
    }

    // Add shipment status getter for JSON serialization
    public String getShipmentStatus() {
        return shipment != null ? shipment.getStatus() : null;
    }

    // Add shipment destination getter for JSON serialization
    public String getShipmentDestination() {
        return shipment != null && shipment.getDestinationSite() != null ? 
               shipment.getDestinationSite().getName() : null;
    }

    // Add shipment expected arrival date getter for JSON serialization
    public java.util.Date getShipmentExpectedArrivalDate() {
        return shipment != null ? shipment.getExpectedArrivalDate() : null;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Drum-related methods
    public List<DrumModel> getDrums() {
        return drums;
    }

    public void setDrums(List<DrumModel> drums) {
        this.drums = drums;
    }

    public void addDrum(DrumModel drum) {
        drums.add(drum);
        drum.setOrder(this);
    }

    public void removeDrum(DrumModel drum) {
        drums.remove(drum);
        drum.setOrder(null);
    }

    // Override quantity getter to return drum count if drums exist, otherwise stored value
    public Integer getQuantity() {
        if (drums != null && !drums.isEmpty()) {
            return drums.size();
        }
        return quantity != null ? quantity : 0;
    }

    // Keep setter for backward compatibility - stores value in case there are no drums yet
    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    // Calculate total weights and length from drums
    public BigDecimal getTotalNetWeight() {
        if (drums == null || drums.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return drums.stream()
                .map(DrumModel::getNetWeightMt)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalGrossWeight() {
        if (drums == null || drums.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return drums.stream()
                .map(DrumModel::getGrossWeightMt)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalLength() {
        if (drums == null || drums.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return drums.stream()
                .map(DrumModel::getLengthKms)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Get count of drums by status
    public long getDrumCountByStatus(DrumStatus status) {
        if (drums == null || drums.isEmpty()) {
            return 0;
        }
        return drums.stream()
                .filter(drum -> drum.getStatus() == status)
                .count();
    }

    // Helper method to check if order has any drums
    public boolean hasDrums() {
        return drums != null && !drums.isEmpty();
    }
}