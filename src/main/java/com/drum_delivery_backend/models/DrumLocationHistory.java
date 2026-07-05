package com.drum_delivery_backend.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "drum_location_history")
@Getter
@Setter
public class DrumLocationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Drum is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drum_number", nullable = false)
    @JsonBackReference(value = "drum-history")
    private DrumModel drum;

    @NotNull(message = "Previous status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false)
    private DrumStatus previousStatus;

    @NotNull(message = "New status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private DrumStatus newStatus;

    @Column(name = "previous_order_id", length = 50)
    private String previousOrderId;

    @Column(name = "new_order_id", length = 50)
    private String newOrderId;

    @Column(name = "previous_shipment_id", length = 50)
    private String previousShipmentId;

    @Column(name = "new_shipment_id", length = 50)
    private String newShipmentId;

    @Size(max = 100, message = "Changed by must not exceed 100 characters")
    @Column(name = "changed_by", length = 100)
    private String changedBy; // Username or system that made the change

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    @Column(name = "notes", length = 1000)
    private String notes;

    @NotNull(message = "Change date is required")
    @Column(name = "change_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date changeDate;

    @Size(max = 50, message = "Location must not exceed 50 characters")
    @Column(name = "location", length = 50)
    private String location; // Physical location where change occurred

    // Constructor
    public DrumLocationHistory() {
        this.changeDate = new Date();
    }

    public DrumLocationHistory(DrumModel drum, DrumStatus previousStatus, DrumStatus newStatus, String changedBy) {
        this();
        this.drum = drum;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
    }

    public DrumLocationHistory(DrumModel drum, DrumStatus previousStatus, DrumStatus newStatus, 
                              String previousOrderId, String newOrderId, 
                              String previousShipmentId, String newShipmentId, 
                              String changedBy, String notes) {
        this();
        this.drum = drum;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.previousOrderId = previousOrderId;
        this.newOrderId = newOrderId;
        this.previousShipmentId = previousShipmentId;
        this.newShipmentId = newShipmentId;
        this.changedBy = changedBy;
        this.notes = notes;
    }

    // Helper methods to check what type of change occurred
    public boolean isStatusChange() {
        return !previousStatus.equals(newStatus);
    }

    public boolean isOrderChange() {
        return !java.util.Objects.equals(previousOrderId, newOrderId);
    }

    public boolean isShipmentChange() {
        return !java.util.Objects.equals(previousShipmentId, newShipmentId);
    }

    public String getChangeDescription() {
        StringBuilder description = new StringBuilder();
        
        if (isStatusChange()) {
            description.append("Status changed from ").append(previousStatus.getDescription())
                      .append(" to ").append(newStatus.getDescription());
        }
        
        if (isOrderChange()) {
            if (description.length() > 0) description.append("; ");
            if (previousOrderId == null && newOrderId != null) {
                description.append("Assigned to order ").append(newOrderId);
            } else if (previousOrderId != null && newOrderId == null) {
                description.append("Removed from order ").append(previousOrderId);
            } else {
                description.append("Moved from order ").append(previousOrderId)
                          .append(" to order ").append(newOrderId);
            }
        }
        
        if (isShipmentChange()) {
            if (description.length() > 0) description.append("; ");
            if (previousShipmentId == null && newShipmentId != null) {
                description.append("Assigned to shipment ").append(newShipmentId);
            } else if (previousShipmentId != null && newShipmentId == null) {
                description.append("Removed from shipment ").append(previousShipmentId);
            } else {
                description.append("Moved from shipment ").append(previousShipmentId)
                          .append(" to shipment ").append(newShipmentId);
            }
        }
        
        return description.toString();
    }
}