package com.drum_delivery_backend.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "containers")
@Getter
@Setter
public class ContainerModel implements Identifiable<String> {

    public static final String TABLE_NAME = "containers";

    @Id
    @NotBlank(message = "Container number is required")
    @Size(max = 11, min = 11, message = "Container number must be exactly 11 characters")
    @Pattern(regexp = "^[A-Z]{4}[0-9]{7}$", message = "Container number must follow format: 4 letters followed by 7 digits (e.g., ACBD1234567)")
    @Column(name = "container_number", nullable = false, unique = true, updatable = false)
    private String containerNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = true)
    @JsonBackReference(value = "shipment-containers")
    private ShipmentModel shipment;

    @OneToMany(mappedBy = "container", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference(value = "container-drums")
    private List<DrumModel> drums = new ArrayList<>();

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

    // Constructors
    public ContainerModel() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    public ContainerModel(String containerNumber) {
        this();
        this.containerNumber = containerNumber;
    }

    public ContainerModel(String containerNumber, ShipmentModel shipment) {
        this(containerNumber);
        this.shipment = shipment;
    }

    // Implement Identifiable interface
    @Override
    public String getId() {
        return containerNumber;
    }

    @Override
    public void setId(String id) {
        this.containerNumber = id;
    }

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = new Date();
    }

    // Business logic methods
    
    /**
     * Add a drum to this container
     */
    public void addDrum(DrumModel drum) {
        drums.add(drum);
        drum.setContainer(this);
    }

    /**
     * Remove a drum from this container
     */
    public void removeDrum(DrumModel drum) {
        drums.remove(drum);
        drum.setContainer(null);
    }

    /**
     * Get the total number of drums in this container
     */
    public int getDrumCount() {
        return drums != null ? drums.size() : 0;
    }

    /**
     * Check if container is assigned to a shipment
     */
    public boolean isAssignedToShipment() {
        return shipment != null;
    }

    /**
     * Get shipment ID if assigned
     */
    public String getShipmentId() {
        return shipment != null ? shipment.getId() : null;
    }

    /**
     * Assign container to a shipment
     */
    public void assignToShipment(ShipmentModel shipment) {
        this.shipment = shipment;
        this.updatedAt = new Date();
        
        // Also assign all drums in this container to the same shipment
        if (drums != null) {
            for (DrumModel drum : drums) {
                drum.setShipment(shipment);
                drum.setStatus(DrumStatus.IN_SHIPMENT);
            }
        }
    }

    /**
     * Remove container from shipment
     */
    public void removeFromShipment() {
        this.shipment = null;
        this.updatedAt = new Date();
        
        // Also remove all drums from the shipment
        if (drums != null) {
            for (DrumModel drum : drums) {
                drum.setShipment(null);
                // Set status based on whether drum has an order
                drum.setStatus(drum.getOrder() != null ? DrumStatus.IN_ORDER : DrumStatus.AVAILABLE);
            }
        }
    }

    /**
     * Validation method to check container number format
     */
    public boolean isValidContainerNumber() {
        return containerNumber != null && containerNumber.matches("^[A-Z]{4}[0-9]{7}$");
    }

    // Override equals and hashCode for proper entity comparison
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContainerModel)) return false;
        ContainerModel that = (ContainerModel) o;
        return containerNumber != null && containerNumber.equals(that.containerNumber);
    }

    @Override
    public int hashCode() {
        return containerNumber != null ? containerNumber.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "ContainerModel{" +
                "containerNumber='" + containerNumber + '\'' +
                ", shipmentId=" + getShipmentId() +
                ", drumCount=" + getDrumCount() +
                ", createdAt=" + createdAt +
                '}';
    }
}