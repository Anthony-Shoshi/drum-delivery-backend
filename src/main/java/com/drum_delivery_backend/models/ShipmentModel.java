package com.drum_delivery_backend.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.drum_delivery_backend.models.validation.ValidationGroups;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "shipment")
public class ShipmentModel implements Identifiable<String>{

    public static final String TABLE_NAME = "shipment";

    @Id
    @Size(max = 50, message = "Shipment ID must not exceed 50 characters")
    @Column(name = "id_shipment", nullable = false, unique = true, updatable = false)
    private String shipmentId;

    @Size(max = 50, message = "Shipment number must not exceed 50 characters", groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class})
    @Pattern(regexp = "^[A-Za-z0-9\\-_]*$", message = "Shipment number can only contain alphanumeric characters, hyphens, and underscores", groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class})
    @Column(name = "shipment_number", nullable = false, unique = true, updatable = false)
    private String shipmentNumber;

    @NotBlank(message = "Invoice number is required", groups = {ValidationGroups.OnCreate.class})
    @Size(max = 50, message = "Invoice number must not exceed 50 characters", groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class})
    @Column(name = "invoice_no", nullable = false)
    private String invoiceNo;

    @NotBlank(message = "BL number is required", groups = {ValidationGroups.OnCreate.class})
    @Size(max = 50, message = "BL number must not exceed 50 characters", groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class})
    @Column(name = "bl_no", nullable = false)
    private String blNo;

    // Legacy field - kept for database compatibility but not used in new workflow
    @Column(name = "container_no", nullable = true)
    private String containerNo;

    @NotNull(message = "Creation date is required", groups = {ValidationGroups.OnCreate.class})
    @PastOrPresent(message = "Creation date cannot be in the future", groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class})
    @Column(name = "creation_date", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date creationDate;

    @Future(message = "Expected arrival date must be in the future", groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class})
    @Column(name = "expected_arrival_date", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    private Date expectedArrivalDate;

    @NotBlank(message = "Status is required", groups = {ValidationGroups.OnCreate.class})
    @Pattern(regexp = "^(CREATED|IN_TRANSIT|ARRIVED|DELIVERED|CANCELLED)$", message = "Status must be one of: CREATED, IN_TRANSIT, ARRIVED, DELIVERED, CANCELLED", groups = {ValidationGroups.OnCreate.class, ValidationGroups.OnUpdate.class})
    @Column(name = "status", nullable = false)
    private String status; // CREATED, IN_TRANSIT, ARRIVED, DELIVERED, etc.
    
    @Size(max = 500, message = "CMR document path must not exceed 500 characters")
    @Column(name = "cmr_document_path", nullable = true)
    private String cmrDocumentPath;
    
    @Size(max = 1000, message = "Bahrain container photos paths must not exceed 1000 characters")
    @Column(name = "bahrain_container_photos_paths", nullable = true, length = 1000)
    private String bahrainContainerPhotosPaths;
    
    @Size(max = 1000, message = "Rotterdam container photos paths must not exceed 1000 characters")
    @Column(name = "rotterdam_container_photos_paths", nullable = true, length = 1000)
    private String rotterdamContainerPhotosPaths;
    
    @Size(max = 1000, message = "Rotterdam truck photos paths must not exceed 1000 characters")
    @Column(name = "rotterdam_truck_photos_paths", nullable = true, length = 1000)
    private String rotterdamTruckPhotosPaths;
    
    @Size(max = 1000, message = "Site truck photos paths must not exceed 1000 characters")
    @Column(name = "site_truck_photos_paths", nullable = true, length = 1000)
    private String siteTruckPhotosPaths;

    @Size(max = 2000, message = "Documents paths must not exceed 2000 characters")
    @Column(name = "documents_paths", nullable = true, length = 2000)
    private String documentsPaths;

    @OneToMany(mappedBy = "shipment", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JsonManagedReference(value="shipment-order")
    private List<OrderModel> orders = new ArrayList<>();

    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference(value="shipment-delivery")
    private List<TruckDeliveryModel> truckDeliveries = new ArrayList<>();

    @OneToMany(mappedBy = "shipment", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JsonManagedReference(value = "shipment-drums")
    private List<DrumModel> drums = new ArrayList<>();

    @OneToMany(mappedBy = "shipment", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @JsonManagedReference(value = "shipment-containers")
    private List<ContainerModel> containers = new ArrayList<>();

    @NotNull(message = "Destination site is required", groups = {ValidationGroups.OnCreate.class})
    @ManyToOne
    @JoinColumn(name = "id_destination_site", nullable = false)
    @JsonIgnoreProperties("shipments")
    private SiteModel destinationSite;

    // Constructor
    public ShipmentModel() {
        this.creationDate = new Date();
        this.status = "CREATED";
    }

    // Helper methods to manage relationship
    public void addOrder(OrderModel order) {
        orders.add(order);
        order.setShipment(this);
        order.setStatus("ASSIGNED_TO_SHIPMENT");
    }

    public void removeOrder(OrderModel order) {
        orders.remove(order);
        order.setShipment(null);
        order.setStatus("CREATED");
    }

    public void addTruckDelivery(TruckDeliveryModel truckDelivery) {
        truckDeliveries.add(truckDelivery);
        truckDelivery.setShipment(this);
    }

    public void removeTruckDelivery(TruckDeliveryModel truckDelivery) {
        truckDeliveries.remove(truckDelivery);
        truckDelivery.setShipment(null);
    }

    // Getters and setters
    @Override
    public String getId() {
        return shipmentId;
    }

    @Override
    public void setId(String id) {
        this.shipmentId = id;
    }

    public String getShipmentNumber() {
        return shipmentNumber;
    }

    public void setShipmentNumber(String shipmentNumber) {
        this.shipmentNumber = shipmentNumber;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public String getBlNo() {
        return blNo;
    }

    public void setBlNo(String blNo) {
        this.blNo = blNo;
    }


    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public Date getExpectedArrivalDate() {
        return expectedArrivalDate;
    }

    public void setExpectedArrivalDate(Date expectedArrivalDate) {
        this.expectedArrivalDate = expectedArrivalDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<OrderModel> getOrders() {
        return orders;
    }

    public void setOrders(List<OrderModel> orders) {
        this.orders = orders;
    }

    public List<TruckDeliveryModel> getTruckDeliveries() {
        return truckDeliveries;
    }

    public void setTruckDeliveries(List<TruckDeliveryModel> truckDeliveries) {
        this.truckDeliveries = truckDeliveries;
    }

    public SiteModel getDestinationSite() {
        return destinationSite;
    }

    public void setDestinationSite(SiteModel destinationSite) {
        this.destinationSite = destinationSite;
    }
    
    public String getCmrDocumentPath() {
        return cmrDocumentPath;
    }
    
    public void setCmrDocumentPath(String cmrDocumentPath) {
        this.cmrDocumentPath = cmrDocumentPath;
    }
    
    public String getBahrainContainerPhotosPaths() {
        return bahrainContainerPhotosPaths;
    }
    
    public void setBahrainContainerPhotosPaths(String bahrainContainerPhotosPaths) {
        this.bahrainContainerPhotosPaths = bahrainContainerPhotosPaths;
    }
    
    public String getRotterdamContainerPhotosPaths() {
        return rotterdamContainerPhotosPaths;
    }
    
    public void setRotterdamContainerPhotosPaths(String rotterdamContainerPhotosPaths) {
        this.rotterdamContainerPhotosPaths = rotterdamContainerPhotosPaths;
    }
    
    public String getRotterdamTruckPhotosPaths() {
        return rotterdamTruckPhotosPaths;
    }
    
    public void setRotterdamTruckPhotosPaths(String rotterdamTruckPhotosPaths) {
        this.rotterdamTruckPhotosPaths = rotterdamTruckPhotosPaths;
    }
    
    public String getSiteTruckPhotosPaths() {
        return siteTruckPhotosPaths;
    }
    
    public void setSiteTruckPhotosPaths(String siteTruckPhotosPaths) {
        this.siteTruckPhotosPaths = siteTruckPhotosPaths;
    }

    public String getDocumentsPaths() {
        return documentsPaths;
    }

    public void setDocumentsPaths(String documentsPaths) {
        this.documentsPaths = documentsPaths;
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
        drum.setShipment(this);
    }

    public void removeDrum(DrumModel drum) {
        drums.remove(drum);
        drum.setShipment(null);
    }

    // Calculate total drums count
    public Integer getTotalDrumCount() {
        return drums != null ? drums.size() : 0;
    }

    // Calculate total weights and length from drums
    public java.math.BigDecimal getTotalNetWeight() {
        if (drums == null || drums.isEmpty()) {
            return java.math.BigDecimal.ZERO;
        }
        return drums.stream()
                .map(DrumModel::getNetWeightMt)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    public java.math.BigDecimal getTotalGrossWeight() {
        if (drums == null || drums.isEmpty()) {
            return java.math.BigDecimal.ZERO;
        }
        return drums.stream()
                .map(DrumModel::getGrossWeightMt)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    public java.math.BigDecimal getTotalLength() {
        if (drums == null || drums.isEmpty()) {
            return java.math.BigDecimal.ZERO;
        }
        return drums.stream()
                .map(DrumModel::getLengthKms)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
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

    // Helper method to check if shipment has any drums
    public boolean hasDrums() {
        return drums != null && !drums.isEmpty();
    }

    // Get all drums from orders in this shipment
    public java.util.List<DrumModel> getAllDrumsIncludingFromOrders() {
        java.util.List<DrumModel> allDrums = new java.util.ArrayList<>();
        
        // Add drums directly assigned to shipment
        if (drums != null) {
            allDrums.addAll(drums);
        }
        
        // Add drums from orders in this shipment
        if (orders != null) {
            for (OrderModel order : orders) {
                if (order.getDrums() != null) {
                    allDrums.addAll(order.getDrums());
                }
            }
        }
        
        return allDrums;
    }

    // Container-related methods
    public List<ContainerModel> getContainers() {
        return containers;
    }

    public void setContainers(List<ContainerModel> containers) {
        this.containers = containers;
    }

    public void addContainer(ContainerModel container) {
        containers.add(container);
        container.setShipment(this);
    }

    public void removeContainer(ContainerModel container) {
        containers.remove(container);
        container.setShipment(null);
    }

    // Calculate total containers count
    public Integer getTotalContainerCount() {
        return containers != null ? containers.size() : 0;
    }

    // Get count of containers with drums
    public long getContainerCountWithDrums() {
        if (containers == null || containers.isEmpty()) {
            return 0;
        }
        return containers.stream()
                .filter(container -> container.getDrums() != null && !container.getDrums().isEmpty())
                .count();
    }

    // Get count of empty containers
    public long getEmptyContainerCount() {
        if (containers == null || containers.isEmpty()) {
            return 0;
        }
        return containers.stream()
                .filter(container -> container.getDrums() == null || container.getDrums().isEmpty())
                .count();
    }

    // Helper method to check if shipment has any containers
    public boolean hasContainers() {
        return containers != null && !containers.isEmpty();
    }

    // Get all drums from containers in this shipment
    public java.util.List<DrumModel> getAllDrumsFromContainers() {
        java.util.List<DrumModel> allDrums = new java.util.ArrayList<>();
        
        if (containers != null) {
            for (ContainerModel container : containers) {
                if (container.getDrums() != null) {
                    allDrums.addAll(container.getDrums());
                }
            }
        }
        
        return allDrums;
    }
}
