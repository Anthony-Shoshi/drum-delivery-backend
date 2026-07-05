package com.drum_delivery_backend.services;

import com.drum_delivery_backend.models.ContainerModel;
import com.drum_delivery_backend.models.DrumModel;
import com.drum_delivery_backend.models.OrderModel;
import com.drum_delivery_backend.models.ShipmentModel;
import com.drum_delivery_backend.models.SiteModel;
import com.drum_delivery_backend.repositories.OrderRepository;
import com.drum_delivery_backend.repositories.ShipmentRepository;
import com.drum_delivery_backend.repositories.SiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ShipmentService {

    private final ShipmentRepository shipmentRepository;
    private final OrderRepository orderRepository;
    private final SiteRepository siteRepository;
    private final ContainerService containerService;
    private final DrumService drumService;
    private final com.drum_delivery_backend.repositories.DrumRepository drumRepository;
    private final com.drum_delivery_backend.repositories.ContainerRepository containerRepository;
    private final DocumentStorageService documentStorageService;

    @Autowired
    public ShipmentService(ShipmentRepository shipmentRepository, OrderRepository orderRepository,
                          SiteRepository siteRepository, ContainerService containerService, DrumService drumService,
                          com.drum_delivery_backend.repositories.DrumRepository drumRepository,
                          com.drum_delivery_backend.repositories.ContainerRepository containerRepository,
                          DocumentStorageService documentStorageService) {
        this.shipmentRepository = shipmentRepository;
        this.orderRepository = orderRepository;
        this.siteRepository = siteRepository;
        this.containerService = containerService;
        this.drumService = drumService;
        this.drumRepository = drumRepository;
        this.containerRepository = containerRepository;
        this.documentStorageService = documentStorageService;
    }

    public List<ShipmentModel> getAllShipments() {
        return shipmentRepository.findAll();
    }

    public Optional<ShipmentModel> getShipmentById(String id) {
        return shipmentRepository.findById(id);
    }

    public Optional<ShipmentModel> getShipmentByShipmentNumber(String shipmentNumber) {
        return shipmentRepository.findByShipmentNumber(shipmentNumber);
    }

    public List<ShipmentModel> getShipmentsByStatus(String status) {
        return shipmentRepository.findByStatus(status);
    }

    public List<ShipmentModel> getShipmentsByDestinationSite(String siteId) {
        return shipmentRepository.findByDestinationSiteId(siteId);
    }

    @Transactional
    public ShipmentModel createShipment(ShipmentModel shipment) {
        if (shipment.getId() == null || shipment.getId().isEmpty()) {
            shipment.setId(UUID.randomUUID().toString());
        }
        
        // Handle shipment number - auto-generate if not provided or if duplicate exists
        if (shipment.getShipmentNumber() == null || shipment.getShipmentNumber().isEmpty()) {
            shipment.setShipmentNumber(generateUniqueShipmentNumber());
        } else {
            // Check if shipment number already exists
            if (shipmentRepository.findByShipmentNumber(shipment.getShipmentNumber()).isPresent()) {
                throw new IllegalArgumentException("Shipment number '" + shipment.getShipmentNumber() + "' already exists. Please choose a different number.");
            }
        }
        
        // Validate destination site exists
        if (shipment.getDestinationSite() != null && shipment.getDestinationSite().getId() != null) {
            Optional<SiteModel> site = siteRepository.findById(shipment.getDestinationSite().getId());
            if (site.isEmpty()) {
                throw new IllegalArgumentException("Destination site not found with ID: " + shipment.getDestinationSite().getId());
            }
            shipment.setDestinationSite(site.get());
        } else {
            throw new IllegalArgumentException("Destination site is required for creating a shipment");
        }
        
        // Set creation date if not provided
        if (shipment.getCreationDate() == null) {
            shipment.setCreationDate(new Date());
        }
        
        // Set status if not provided
        if (shipment.getStatus() == null || shipment.getStatus().isEmpty()) {
            shipment.setStatus("CREATED");
        }
        
        // Process containers sent in the request
        if (shipment.getContainers() != null && !shipment.getContainers().isEmpty()) {
            List<ContainerModel> processedContainers = processContainersForShipment(shipment.getContainers());
            shipment.getContainers().clear();
            for (ContainerModel container : processedContainers) {
                shipment.addContainer(container);
            }
        }
        
        // Process drums sent in the request
        if (shipment.getDrums() != null && !shipment.getDrums().isEmpty()) {
            List<DrumModel> processedDrums = processDrumsForShipment(shipment.getDrums(), shipment);
            shipment.getDrums().clear();
            for (DrumModel drum : processedDrums) {
                shipment.addDrum(drum);
            }
        }
        
        return shipmentRepository.save(shipment);
    }
    
    /**
     * Process containers for shipment creation
     */
    private List<ContainerModel> processContainersForShipment(List<ContainerModel> containers) {
        List<ContainerModel> processedContainers = new ArrayList<>();
        
        for (ContainerModel containerData : containers) {
            try {
                // Create or get container using containerService
                ContainerModel container = containerService.createOrGetContainer(containerData.getContainerNumber());
                
                // Update notes if provided
                if (containerData.getNotes() != null && !containerData.getNotes().trim().isEmpty()) {
                    container.setNotes(containerData.getNotes());
                    containerService.updateContainer(container.getContainerNumber(), container);
                }
                
                processedContainers.add(container);
            } catch (Exception e) {
                throw new RuntimeException("Failed to process container " + containerData.getContainerNumber() + ": " + e.getMessage());
            }
        }
        
        return processedContainers;
    }
    
    /**
     * Process drums for shipment creation
     */
    private List<DrumModel> processDrumsForShipment(List<DrumModel> drums, ShipmentModel shipment) {
        List<DrumModel> processedDrums = new ArrayList<>();
        
        for (DrumModel drumData : drums) {
            try {
                // Create new drum
                DrumModel drum = new DrumModel();
                drum.setDrumNumber(drumData.getDrumNumber());
                drum.setLengthKms(drumData.getLengthKms());
                drum.setNetWeightMt(drumData.getNetWeightMt());
                drum.setGrossWeightMt(drumData.getGrossWeightMt());
                drum.setNotes(drumData.getNotes());
                drum.setStatus(drumData.getStatus());
                
                // Assign drum to container if containerNumber is provided
                String containerNumber = drumData.getContainerNumber();
                if (containerNumber != null && !containerNumber.trim().isEmpty()) {
                    // Find the container in the shipment
                    Optional<ContainerModel> containerOpt = shipment.getContainers().stream()
                        .filter(c -> c.getContainerNumber().equals(containerNumber))
                        .findFirst();
                    
                    if (containerOpt.isPresent()) {
                        ContainerModel container = containerOpt.get();
                        drum.setContainer(container);
                    } else {
                        // Container not found in shipment, log warning but continue
                        System.out.println("Warning: Container " + containerNumber + " not found in shipment for drum " + drum.getDrumNumber());
                    }
                }
                
                // Save drum using drumService
                drum = drumService.createDrum(drum);
                
                processedDrums.add(drum);
            } catch (Exception e) {
                throw new RuntimeException("Failed to process drum " + drumData.getDrumNumber() + ": " + e.getMessage());
            }
        }
        
        return processedDrums;
    }
    
    /**
     * Generate a unique shipment number in the format SH{YYYY}{sequential_number}
     */
    private String generateUniqueShipmentNumber() {
        int currentYear = java.time.Year.now().getValue();
        String yearPrefix = "SH" + currentYear;
        
        // Find the highest existing shipment number for this year
        List<ShipmentModel> shipmentsThisYear = shipmentRepository.findByShipmentNumberStartingWith(yearPrefix);
        int nextNumber = 1;
        
        for (ShipmentModel existingShipment : shipmentsThisYear) {
            String shipmentNumber = existingShipment.getShipmentNumber();
            if (shipmentNumber.startsWith(yearPrefix)) {
                try {
                    String numberPart = shipmentNumber.substring(yearPrefix.length());
                    int num = Integer.parseInt(numberPart);
                    if (num >= nextNumber) {
                        nextNumber = num + 1;
                    }
                } catch (NumberFormatException e) {
                    // Skip if number format is invalid
                    continue;
                }
            }
        }
        
        return yearPrefix + String.format("%03d", nextNumber);
    }

    @Transactional
    public Optional<ShipmentModel> updateShipment(String id, ShipmentModel shipmentDetails) {
        return shipmentRepository.findById(id)
                .map(existingShipment -> {
                    if (shipmentDetails.getShipmentNumber() != null) {
                        existingShipment.setShipmentNumber(shipmentDetails.getShipmentNumber());
                    }
                    if (shipmentDetails.getInvoiceNo() != null) {
                        existingShipment.setInvoiceNo(shipmentDetails.getInvoiceNo());
                    }
                    if (shipmentDetails.getBlNo() != null) {
                        existingShipment.setBlNo(shipmentDetails.getBlNo());
                    }
                    if (shipmentDetails.getExpectedArrivalDate() != null) {
                        existingShipment.setExpectedArrivalDate(shipmentDetails.getExpectedArrivalDate());
                    }
                    if (shipmentDetails.getStatus() != null) {
                        existingShipment.setStatus(shipmentDetails.getStatus());
                    }
                    
                    // Update destination site if provided
                    if (shipmentDetails.getDestinationSite() != null && shipmentDetails.getDestinationSite().getId() != null) {
                        Optional<SiteModel> site = siteRepository.findById(shipmentDetails.getDestinationSite().getId());
                        site.ifPresent(existingShipment::setDestinationSite);
                    }
                    
                    return shipmentRepository.save(existingShipment);
                });
    }

    @Transactional
    public boolean deleteShipment(String id) {
        return shipmentRepository.findById(id)
                .map(shipment -> {
                    long startTime = System.currentTimeMillis();
                    System.out.println("[DELETE SHIPMENT] Starting delete for shipment ID: " + id);

                    // 1. Batch update orders - Remove shipment reference
                    List<OrderModel> orders = orderRepository.findByShipmentId(id);
                    System.out.println("[DELETE SHIPMENT] Found " + orders.size() + " orders to update");
                    orders.forEach(order -> {
                        order.setShipment(null);
                        order.setStatus("CREATED");
                    });
                    orderRepository.saveAll(orders); // Batch save instead of individual saves
                    System.out.println("[DELETE SHIPMENT] Orders updated in " + (System.currentTimeMillis() - startTime) + "ms");

                    // 2. Batch update drums - Remove shipment reference
                    long drumStartTime = System.currentTimeMillis();
                    List<DrumModel> drums = drumRepository.findByShipmentShipmentId(id);
                    System.out.println("[DELETE SHIPMENT] Found " + drums.size() + " drums to update");
                    drums.forEach(drum -> drum.setShipment(null));
                    drumRepository.saveAll(drums); // Batch save
                    System.out.println("[DELETE SHIPMENT] Drums updated in " + (System.currentTimeMillis() - drumStartTime) + "ms");

                    // 3. Batch update containers - Remove shipment reference
                    long containerStartTime = System.currentTimeMillis();
                    List<ContainerModel> containers = containerRepository.findByShipmentShipmentId(id);
                    System.out.println("[DELETE SHIPMENT] Found " + containers.size() + " containers to update");
                    containers.forEach(container -> container.setShipment(null));
                    containerRepository.saveAll(containers); // Batch save
                    System.out.println("[DELETE SHIPMENT] Containers updated in " + (System.currentTimeMillis() - containerStartTime) + "ms");

                    // 4. Delete the shipment from database
                    // Note: Truck deliveries will be cascade-deleted automatically (CascadeType.ALL)
                    long deleteStartTime = System.currentTimeMillis();
                    shipmentRepository.delete(shipment);
                    System.out.println("[DELETE SHIPMENT] Shipment deleted from DB in " + (System.currentTimeMillis() - deleteStartTime) + "ms");

                    // 5. Delete physical files AFTER transaction (non-blocking)
                    // Store shipment data for async deletion
                    ShipmentModel shipmentCopy = new ShipmentModel();
                    shipmentCopy.setCmrDocumentPath(shipment.getCmrDocumentPath());
                    shipmentCopy.setBahrainContainerPhotosPaths(shipment.getBahrainContainerPhotosPaths());
                    shipmentCopy.setRotterdamContainerPhotosPaths(shipment.getRotterdamContainerPhotosPaths());
                    shipmentCopy.setRotterdamTruckPhotosPaths(shipment.getRotterdamTruckPhotosPaths());
                    shipmentCopy.setSiteTruckPhotosPaths(shipment.getSiteTruckPhotosPaths());
                    shipmentCopy.setDocumentsPaths(shipment.getDocumentsPaths());

                    System.out.println("[DELETE SHIPMENT] Total DB operation time: " + (System.currentTimeMillis() - startTime) + "ms");

                    // Schedule file deletion to run after transaction commits
                    // This prevents file I/O from blocking the transaction
                    new Thread(() -> {
                        try {
                            System.out.println("[DELETE SHIPMENT] Starting async file deletion for shipment: " + id);
                            deleteShipmentFiles(shipmentCopy);
                            System.out.println("[DELETE SHIPMENT] Async file deletion completed for shipment: " + id);
                        } catch (Exception e) {
                            System.err.println("[DELETE SHIPMENT] Error in async file deletion: " + e.getMessage());
                        }
                    }).start();

                    return true;
                }).orElse(false);
    }

    /**
     * Delete all physical files (photos and documents) associated with a shipment
     */
    private void deleteShipmentFiles(ShipmentModel shipment) {
        try {
            // Delete CMR document if exists
            if (shipment.getCmrDocumentPath() != null && !shipment.getCmrDocumentPath().trim().isEmpty()) {
                try {
                    documentStorageService.deleteFile(shipment.getCmrDocumentPath());
                } catch (Exception e) {
                    System.err.println("Warning: Failed to delete CMR document: " + e.getMessage());
                }
            }

            // Delete all photos from different categories
            deletePhotosFromPath(shipment.getBahrainContainerPhotosPaths());
            deletePhotosFromPath(shipment.getRotterdamContainerPhotosPaths());
            deletePhotosFromPath(shipment.getRotterdamTruckPhotosPaths());
            deletePhotosFromPath(shipment.getSiteTruckPhotosPaths());

            // Delete all documents
            deleteDocumentsFromPath(shipment.getDocumentsPaths());

        } catch (Exception e) {
            // Log the error but don't fail the delete operation
            System.err.println("Error deleting shipment files: " + e.getMessage());
        }
    }

    /**
     * Delete photos from a comma-separated path string
     */
    private void deletePhotosFromPath(String pathsString) {
        if (pathsString == null || pathsString.trim().isEmpty()) {
            return;
        }

        String[] paths = pathsString.split(",");
        for (String path : paths) {
            String trimmedPath = path.trim();
            if (!trimmedPath.isEmpty()) {
                try {
                    documentStorageService.deleteFile(trimmedPath);
                } catch (Exception e) {
                    System.err.println("Warning: Failed to delete photo file '" + trimmedPath + "': " + e.getMessage());
                }
            }
        }
    }

    /**
     * Delete documents from a comma-separated path string
     */
    private void deleteDocumentsFromPath(String pathsString) {
        if (pathsString == null || pathsString.trim().isEmpty()) {
            return;
        }

        String[] paths = pathsString.split(",");
        for (String path : paths) {
            String trimmedPath = path.trim();
            if (!trimmedPath.isEmpty()) {
                try {
                    documentStorageService.deleteFile(trimmedPath);
                } catch (Exception e) {
                    System.err.println("Warning: Failed to delete document file '" + trimmedPath + "': " + e.getMessage());
                }
            }
        }
    }

    @Transactional
    public Optional<ShipmentModel> addOrderToShipment(String shipmentId, String orderId) {
        Optional<ShipmentModel> shipmentOpt = shipmentRepository.findById(shipmentId);
        Optional<OrderModel> orderOpt = orderRepository.findById(orderId);
        
        if (shipmentOpt.isPresent() && orderOpt.isPresent()) {
            ShipmentModel shipment = shipmentOpt.get();
            OrderModel order = orderOpt.get();
            
            // Remove from previous shipment if exists
            if (order.getShipment() != null && !order.getShipment().getId().equals(shipmentId)) {
                ShipmentModel previousShipment = order.getShipment();
                previousShipment.removeOrder(order);
                shipmentRepository.save(previousShipment);
                orderRepository.save(order); // Save order after removing from previous shipment
            }
            
            // Add to this shipment
            shipment.addOrder(order);
            shipmentRepository.save(shipment); // Save shipment side
            orderRepository.save(order); // Save order side to persist the shipment reference and status
            return Optional.of(shipment);
        }
        
        return Optional.empty();
    }

    @Transactional
    public Optional<ShipmentModel> removeOrderFromShipment(String shipmentId, String orderId) {
        Optional<ShipmentModel> shipmentOpt = shipmentRepository.findById(shipmentId);
        Optional<OrderModel> orderOpt = orderRepository.findById(orderId);
        
        if (shipmentOpt.isPresent() && orderOpt.isPresent()) {
            ShipmentModel shipment = shipmentOpt.get();
            OrderModel order = orderOpt.get();
            
            if (order.getShipment() != null && order.getShipment().getId().equals(shipmentId)) {
                shipment.removeOrder(order);
                shipmentRepository.save(shipment); // Save shipment side
                orderRepository.save(order); // Save order side to persist the removed shipment reference and status
                return Optional.of(shipment);
            }
        }
        
        return Optional.empty();
    }
}