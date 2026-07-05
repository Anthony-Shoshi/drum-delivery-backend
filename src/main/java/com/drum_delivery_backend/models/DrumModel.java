package com.drum_delivery_backend.models;

import com.drum_delivery_backend.models.validation.ValidationGroups;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "drums", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"container_id", "drum_number"}))
@Getter
@Setter
public class DrumModel implements Identifiable<String> {

    public static final String TABLE_NAME = "drums";

    @Id
    @NotBlank(message = "Drum number is required", groups = ValidationGroups.OnCreate.class)
    @Size(max = 50, message = "Drum number must not exceed 50 characters")
    @Pattern(regexp = "^[A-Za-z0-9\\-_]+$", message = "Drum number can only contain alphanumeric characters, hyphens, and underscores")
    @Column(name = "drum_number", nullable = false, updatable = false)
    private String drumNumber;

    @NotNull(message = "Length in kilometers is required")
    @DecimalMin(value = "0.001", message = "Length must be greater than 0")
    @DecimalMax(value = "999999.999", message = "Length must not exceed 999999.999 km")
    @Column(name = "length_kms", nullable = false, precision = 10, scale = 3)
    private BigDecimal lengthKms;

    @NotNull(message = "Net weight is required")
    @DecimalMin(value = "0.001", message = "Net weight must be greater than 0")
    @DecimalMax(value = "999999.999", message = "Net weight must not exceed 999999.999 MT")
    @Column(name = "net_weight_mt", nullable = false, precision = 10, scale = 3)
    private BigDecimal netWeightMt;

    @NotNull(message = "Gross weight is required")
    @DecimalMin(value = "0.001", message = "Gross weight must be greater than 0")
    @DecimalMax(value = "999999.999", message = "Gross weight must not exceed 999999.999 MT")
    @Column(name = "gross_weight_mt", nullable = false, precision = 10, scale = 3)
    private BigDecimal grossWeightMt;

    @NotNull(message = "Drum status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DrumStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = true)
    @JsonBackReference(value = "order-drums")
    private OrderModel order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = true)
    @JsonBackReference(value = "shipment-drums")
    private ShipmentModel shipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "container_id", nullable = true)
    @JsonBackReference(value = "container-drums")
    private ContainerModel container;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "truck_delivery_id", nullable = true)
    @JsonBackReference(value = "truck-delivery-drums")
    private TruckDeliveryModel truckDelivery;

    @NotNull(message = "Creation date is required")
    @PastOrPresent(message = "Creation date cannot be in the future")
    @Column(name = "created_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @NotNull(message = "Update date is required")
    @Column(name = "updated_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    @Column(name = "notes", length = 1000)
    private String notes;

    @Size(max = 50, message = "Container number must not exceed 50 characters")
    @Pattern(regexp = "^[A-Z]{4}[0-9]{7}$", message = "Container number must follow format: 4 letters followed by 7 digits", 
             groups = ValidationGroups.OnCreate.class)
    @Column(name = "container_no", nullable = true)
    private String containerNo;

    @OneToMany(mappedBy = "drum", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference(value = "drum-history")
    private List<DrumLocationHistory> locationHistory = new ArrayList<>();

    // Constructor
    public DrumModel() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.status = DrumStatus.AVAILABLE;
    }

    public DrumModel(String drumNumber, BigDecimal lengthKms, BigDecimal netWeightMt, BigDecimal grossWeightMt) {
        this();
        this.drumNumber = drumNumber;
        this.lengthKms = lengthKms;
        this.netWeightMt = netWeightMt;
        this.grossWeightMt = grossWeightMt;
    }

    public DrumModel(String drumNumber, BigDecimal lengthKms, BigDecimal netWeightMt, BigDecimal grossWeightMt, String containerNo) {
        this(drumNumber, lengthKms, netWeightMt, grossWeightMt);
        this.containerNo = containerNo;
    }

    public DrumModel(String drumNumber, BigDecimal lengthKms, BigDecimal netWeightMt, BigDecimal grossWeightMt, ContainerModel container) {
        this(drumNumber, lengthKms, netWeightMt, grossWeightMt);
        this.container = container;
        this.containerNo = container != null ? container.getContainerNumber() : null;
    }

    // Implement Identifiable interface
    @Override
    public String getId() {
        return drumNumber;
    }

    @Override
    public void setId(String id) {
        this.drumNumber = id;
    }

    // Business logic methods
    public void assignToOrder(OrderModel order) {
        if (this.status != DrumStatus.AVAILABLE) {
            throw new IllegalStateException("Drum " + drumNumber + " is not available for assignment");
        }
        this.order = order;
        this.status = DrumStatus.IN_ORDER;
        this.updatedAt = new Date();
    }

    public void assignToShipment(ShipmentModel shipment) {
        if (this.status != DrumStatus.IN_ORDER) {
            throw new IllegalStateException("Drum " + drumNumber + " must be in order before assignment to shipment");
        }
        this.shipment = shipment;
        this.status = DrumStatus.IN_SHIPMENT;
        this.updatedAt = new Date();
    }

    public void markAsDelivered() {
        if (this.status != DrumStatus.IN_SHIPMENT) {
            throw new IllegalStateException("Drum " + drumNumber + " must be in shipment before marking as delivered");
        }
        this.status = DrumStatus.DELIVERED;
        this.updatedAt = new Date();
    }

    public void markAsMissing(String notes) {
        this.status = DrumStatus.MISSING;
        this.notes = notes;
        this.updatedAt = new Date();
    }

    public void markAsDamaged(String notes) {
        this.status = DrumStatus.DAMAGED;
        this.notes = notes;
        this.updatedAt = new Date();
    }

    public void removeFromOrder() {
        this.order = null;
        this.status = DrumStatus.AVAILABLE;
        this.updatedAt = new Date();
    }

    public void removeFromShipment() {
        this.shipment = null;
        this.status = DrumStatus.IN_ORDER;
        this.updatedAt = new Date();
    }

    public void assignToTruckDelivery(TruckDeliveryModel truckDelivery) {
        if (this.status != DrumStatus.IN_SHIPMENT) {
            throw new IllegalStateException("Drum " + drumNumber + " must be in shipment before assignment to truck delivery");
        }
        this.truckDelivery = truckDelivery;
        this.updatedAt = new Date();
    }

    public void removeFromTruckDelivery() {
        this.truckDelivery = null;
        this.updatedAt = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = new Date();
    }

    // Container management methods
    public void assignToContainer(String containerNumber) {
        this.containerNo = containerNumber;
        this.updatedAt = new Date();
    }

    public void assignToContainer(ContainerModel container) {
        this.container = container;
        this.containerNo = container != null ? container.getContainerNumber() : null;
        this.updatedAt = new Date();
    }

    public void removeFromContainer() {
        this.container = null;
        this.containerNo = null;
        this.updatedAt = new Date();
    }

    public boolean hasContainer() {
        return container != null || (containerNo != null && !containerNo.trim().isEmpty());
    }

    public String getContainerNumber() {
        return container != null ? container.getContainerNumber() : containerNo;
    }

    public TruckDeliveryModel getTruckDelivery() {
        return truckDelivery;
    }

    public void setTruckDelivery(TruckDeliveryModel truckDelivery) {
        this.truckDelivery = truckDelivery;
        this.updatedAt = new Date();
    }

    // Validation method
    public boolean isValidWeights() {
        return grossWeightMt.compareTo(netWeightMt) >= 0;
    }
}