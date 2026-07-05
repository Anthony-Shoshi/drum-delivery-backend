package com.drum_delivery_backend.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "truck_delivery")
public class TruckDeliveryModel implements Identifiable<String> {

    public static final String TABLE_NAME = "truck_delivery";

    @Id
    @Size(max = 50, message = "Truck delivery ID must not exceed 50 characters")
    @Column(name = "id_truck_delivery", nullable = false, unique = true, updatable = false)
    private String truckDeliveryId;

    @NotBlank(message = "Truck number is required")
    @Size(max = 20, message = "Truck number must not exceed 20 characters")
    @Pattern(regexp = "^[A-Za-z0-9\\-_]+$", message = "Truck number can only contain alphanumeric characters, hyphens, and underscores")
    @Column(name = "truck_number", nullable = false)
    private String truckNumber;

    @Size(min = 2, max = 100, message = "Driver name must be between 2 and 100 characters")
    @Pattern(regexp = "^[A-Za-z\\s\\-']+$", message = "Driver name can only contain letters, spaces, hyphens, and apostrophes")
    @Column(name = "driver_name", nullable = true)
    private String driverName;

    @Pattern(regexp = "^[+]?[0-9\\s\\-()]{7,20}$", message = "Driver phone format is invalid")
    @Size(max = 20, message = "Driver phone must not exceed 20 characters")
    @Column(name = "driver_phone", nullable = true)
    private String driverPhone;

    @Size(max = 15, message = "License plate must not exceed 15 characters")
    @Pattern(regexp = "^[A-Za-z0-9\\s\\-]+$", message = "License plate format is invalid")
    @Column(name = "license_plate", nullable = true)
    private String licensePlate;

    @Future(message = "Scheduled date must be in the future")
    @Column(name = "scheduled_date", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    private Date scheduledDate;

    @PastOrPresent(message = "Actual departure date cannot be in the future")
    @Column(name = "actual_departure_date", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    private Date actualDepartureDate;

    @PastOrPresent(message = "Actual arrival date cannot be in the future")
    @Column(name = "actual_arrival_date", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    private Date actualArrivalDate;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(SCHEDULED|IN_TRANSIT|DELIVERED|CANCELLED)$", message = "Status must be one of: SCHEDULED, IN_TRANSIT, DELIVERED, CANCELLED")
    @Column(name = "status", nullable = false)
    private String status; // SCHEDULED, IN_TRANSIT, DELIVERED, etc.

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    @Column(name = "notes", nullable = true, length = 1000)
    private String notes;

    @NotNull(message = "Shipment is required")
    @ManyToOne
    @JoinColumn(name = "id_shipment", nullable = false)
    @JsonBackReference(value="shipment-delivery")
    private ShipmentModel shipment;

    @ElementCollection
    @CollectionTable(name = "truck_delivery_order", joinColumns = @JoinColumn(name = "id_truck_delivery"))
    @Column(name = "id_order")
    private List<String> orderIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "truck_delivery_drum", joinColumns = @JoinColumn(name = "id_truck_delivery"))
    @Column(name = "drum_number")
    private List<String> drumNumbers = new ArrayList<>();

    @OneToMany(mappedBy = "truckDelivery", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference(value = "truck-delivery-drums")
    private List<DrumModel> drums = new ArrayList<>();

    @NotNull(message = "Creation date is required")
    @PastOrPresent(message = "Creation date cannot be in the future")
    @Column(name = "creation_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    // Constructor
    public TruckDeliveryModel() {
        this.creationDate = new Date();
        this.status = "SCHEDULED";
    }

    // Getters and setters
    @Override
    public String getId() {
        return truckDeliveryId;
    }

    @Override
    public void setId(String id) {
        this.truckDeliveryId = id;
    }

    public String getTruckNumber() {
        return truckNumber;
    }

    public void setTruckNumber(String truckNumber) {
        this.truckNumber = truckNumber;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getDriverPhone() {
        return driverPhone;
    }

    public void setDriverPhone(String driverPhone) {
        this.driverPhone = driverPhone;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public Date getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(Date scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public Date getActualDepartureDate() {
        return actualDepartureDate;
    }

    public void setActualDepartureDate(Date actualDepartureDate) {
        this.actualDepartureDate = actualDepartureDate;
    }

    public Date getActualArrivalDate() {
        return actualArrivalDate;
    }

    public void setActualArrivalDate(Date actualArrivalDate) {
        this.actualArrivalDate = actualArrivalDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public ShipmentModel getShipment() {
        return shipment;
    }

    public void setShipment(ShipmentModel shipment) {
        this.shipment = shipment;
    }

    public List<String> getOrderIds() {
        return orderIds;
    }

    public void setOrderIds(List<String> orderIds) {
        this.orderIds = orderIds;
    }

    public List<String> getDrumNumbers() {
        return drumNumbers;
    }

    public void setDrumNumbers(List<String> drumNumbers) {
        this.drumNumbers = drumNumbers;
    }

    public List<DrumModel> getDrums() {
        return drums;
    }

    public void setDrums(List<DrumModel> drums) {
        this.drums = drums;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    // Helper methods
    public void addOrderId(String orderId) {
        if (!orderIds.contains(orderId)) {
            orderIds.add(orderId);
        }
    }

    public void removeOrderId(String orderId) {
        orderIds.remove(orderId);
    }

    public void addDrumNumber(String drumNumber) {
        if (!drumNumbers.contains(drumNumber)) {
            drumNumbers.add(drumNumber);
        }
    }

    public void removeDrumNumber(String drumNumber) {
        drumNumbers.remove(drumNumber);
    }

    public boolean containsDrum(String drumNumber) {
        return drumNumbers.contains(drumNumber);
    }

    public int getDrumCount() {
        return drumNumbers.size();
    }

    public void addDrum(DrumModel drum) {
        if (!drums.contains(drum)) {
            drums.add(drum);
            drum.setTruckDelivery(this);
            addDrumNumber(drum.getDrumNumber());
        }
    }

    public void removeDrum(DrumModel drum) {
        if (drums.contains(drum)) {
            drums.remove(drum);
            drum.setTruckDelivery(null);
            removeDrumNumber(drum.getDrumNumber());
        }
    }
}