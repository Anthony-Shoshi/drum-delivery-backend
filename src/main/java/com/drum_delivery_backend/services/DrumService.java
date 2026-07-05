package com.drum_delivery_backend.services;

import com.drum_delivery_backend.models.*;
import com.drum_delivery_backend.repositories.ContainerRepository;
import com.drum_delivery_backend.repositories.DrumRepository;
import com.drum_delivery_backend.repositories.DrumLocationHistoryRepository;
import com.drum_delivery_backend.repositories.OrderRepository;
import com.drum_delivery_backend.repositories.ShipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.*;
import jakarta.persistence.criteria.Predicate;

@Service
@Transactional
public class DrumService {

    @Autowired
    private DrumRepository drumRepository;

    @Autowired
    private DrumLocationHistoryRepository historyRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;
    
    @Autowired
    private ContainerRepository containerRepository;
    
    @Autowired
    private ContainerService containerService;

    // CRUD Operations
    public List<DrumModel> getAllDrums() {
        return drumRepository.findAll();
    }

    public Page<DrumModel> getAllDrums(Pageable pageable) {
        return drumRepository.findAll(pageable);
    }

    public Page<DrumModel> getAllDrumsFiltered(Pageable pageable, DrumStatus status, 
                                               BigDecimal minWeight, BigDecimal maxWeight,
                                               Boolean unassigned, Boolean assigned) {
        Specification<DrumModel> spec = Specification.where(null);

        // Status filter
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        // Weight filters
        if (minWeight != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("netWeightMt"), minWeight));
        }
        if (maxWeight != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("netWeightMt"), maxWeight));
        }

        // Assignment filters
        if (Boolean.TRUE.equals(unassigned)) {
            spec = spec.and((root, query, cb) -> 
                cb.and(
                    cb.isNull(root.get("order")),
                    cb.isNull(root.get("shipment"))
                )
            );
        } else if (Boolean.TRUE.equals(assigned)) {
            spec = spec.and((root, query, cb) -> 
                cb.or(
                    cb.isNotNull(root.get("order")),
                    cb.isNotNull(root.get("shipment"))
                )
            );
        }

        return drumRepository.findAll(spec, pageable);
    }

    public Optional<DrumModel> getDrumByNumber(String drumNumber) {
        return drumRepository.findById(drumNumber);
    }

    public DrumModel createDrum(DrumModel drum) {
        // Validate drum data
        if (drum.getDrumNumber() == null || drum.getDrumNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Drum number is required");
        }

        // Handle container assignment if containerNo is provided
        if (drum.getContainerNo() != null && !drum.getContainerNo().trim().isEmpty()) {
            // Check if container exists, create if it doesn't
            ContainerModel container = containerService.createOrGetContainer(drum.getContainerNo());
            drum.setContainer(container);
            
            // Check for duplicate drum number in the same container
            if (drumRepository.existsByContainerAndDrumNumber(container, drum.getDrumNumber())) {
                throw new IllegalArgumentException("Drum with number " + drum.getDrumNumber() + 
                                                 " already exists in container " + drum.getContainerNo());
            }
        }

        if (!drum.isValidWeights()) {
            throw new IllegalArgumentException("Gross weight must be greater than or equal to net weight");
        }

        // Set default status if not provided
        if (drum.getStatus() == null) {
            drum.setStatus(DrumStatus.AVAILABLE);
        }

        return drumRepository.save(drum);
    }

    public DrumModel updateDrum(String drumNumber, DrumModel drumDetails) {
        DrumModel drum = drumRepository.findById(drumNumber)
                .orElseThrow(() -> new RuntimeException("Drum not found with number: " + drumNumber));

        // Update allowed fields
        drum.setLengthKms(drumDetails.getLengthKms());
        drum.setNetWeightMt(drumDetails.getNetWeightMt());
        drum.setGrossWeightMt(drumDetails.getGrossWeightMt());
        drum.setNotes(drumDetails.getNotes());
        
        // Update container assignment if provided
        if (drumDetails.getContainerNo() != null) {
            drum.setContainerNo(drumDetails.getContainerNo());
        }

        if (!drum.isValidWeights()) {
            throw new IllegalArgumentException("Gross weight must be greater than or equal to net weight");
        }

        return drumRepository.save(drum);
    }

    public void deleteDrum(String drumNumber) {
        DrumModel drum = drumRepository.findById(drumNumber)
                .orElseThrow(() -> new RuntimeException("Drum not found with number: " + drumNumber));

        if (drum.getStatus() != DrumStatus.AVAILABLE) {
            throw new IllegalStateException("Cannot delete drum that is assigned to order or shipment");
        }

        drumRepository.delete(drum);
    }

    // Order Assignment Operations
    @Transactional
    public DrumModel assignDrumToOrder(String drumNumber, String orderId) {
        DrumModel drum = drumRepository.findById(drumNumber)
                .orElseThrow(() -> new RuntimeException("Drum not found with number: " + drumNumber));

        OrderModel order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        // Validation: Check if drum is already assigned to a different order
        if (drum.getOrder() != null && !drum.getOrder().getId().equals(orderId)) {
            throw new IllegalStateException("Drum " + drumNumber + " is already assigned to order " + drum.getOrder().getId());
        }

        drum.assignToOrder(order);
        order.addDrum(drum); // Ensure bidirectional relationship
        
        DrumModel savedDrum = drumRepository.save(drum);
        orderRepository.save(order); // Save order side to maintain bidirectional consistency
        return savedDrum;
    }

    @Transactional
    public DrumModel removeDrumFromOrder(String drumNumber) {
        DrumModel drum = drumRepository.findById(drumNumber)
                .orElseThrow(() -> new RuntimeException("Drum not found with number: " + drumNumber));

        OrderModel order = drum.getOrder();
        drum.removeFromOrder();
        
        DrumModel savedDrum = drumRepository.save(drum);
        
        if (order != null) {
            order.removeDrum(drum); // Ensure bidirectional relationship
            orderRepository.save(order); // Save order side to maintain bidirectional consistency
        }
        
        return savedDrum;
    }

    @Transactional
    public List<DrumModel> assignMultipleDrumsToOrder(List<String> drumNumbers, String orderId) {
        OrderModel order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        List<DrumModel> drums = drumRepository.findAllById(drumNumbers);
        
        for (DrumModel drum : drums) {
            if (drum.getStatus() != DrumStatus.AVAILABLE) {
                throw new IllegalStateException("Drum " + drum.getDrumNumber() + " is not available for assignment");
            }
            drum.assignToOrder(order);
            order.addDrum(drum); // Ensure bidirectional relationship
        }

        List<DrumModel> savedDrums = drumRepository.saveAll(drums);
        orderRepository.save(order); // Save order side to maintain bidirectional consistency
        return savedDrums;
    }

    // Shipment Assignment Operations
    @Transactional
    public DrumModel assignDrumToShipment(String drumNumber, String shipmentId) {
        DrumModel drum = drumRepository.findById(drumNumber)
                .orElseThrow(() -> new RuntimeException("Drum not found with number: " + drumNumber));

        ShipmentModel shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found with ID: " + shipmentId));

        // Validation: Check if drum is already assigned to a different shipment
        if (drum.getShipment() != null && !drum.getShipment().getId().equals(shipmentId)) {
            throw new IllegalStateException("Drum " + drumNumber + " is already assigned to shipment " + drum.getShipment().getId());
        }

        drum.assignToShipment(shipment);
        shipment.addDrum(drum); // Ensure bidirectional relationship
        
        // If drum has a container, assign the container to the shipment too
        if (drum.getContainer() != null) {
            ContainerModel container = drum.getContainer();
            // Only assign if container is not already assigned to this shipment
            if (container.getShipment() == null || !container.getShipment().getId().equals(shipmentId)) {
                container.assignToShipment(shipment);
                containerRepository.save(container);
            }
        }
        
        DrumModel savedDrum = drumRepository.save(drum);
        shipmentRepository.save(shipment); // Save shipment side to maintain bidirectional consistency
        return savedDrum;
    }

    @Transactional
    public DrumModel removeDrumFromShipment(String drumNumber) {
        DrumModel drum = drumRepository.findById(drumNumber)
                .orElseThrow(() -> new RuntimeException("Drum not found with number: " + drumNumber));

        ShipmentModel shipment = drum.getShipment();
        drum.removeFromShipment();
        
        DrumModel savedDrum = drumRepository.save(drum);
        
        if (shipment != null) {
            shipment.removeDrum(drum); // Ensure bidirectional relationship
            shipmentRepository.save(shipment); // Save shipment side to maintain bidirectional consistency
        }
        
        return savedDrum;
    }

    // Status Management
    @Transactional
    public DrumModel markDrumAsDelivered(String drumNumber) {
        DrumModel drum = drumRepository.findById(drumNumber)
                .orElseThrow(() -> new RuntimeException("Drum not found with number: " + drumNumber));

        drum.markAsDelivered();
        return drumRepository.save(drum);
    }

    @Transactional
    public DrumModel markDrumAsMissing(String drumNumber, String notes) {
        DrumModel drum = drumRepository.findById(drumNumber)
                .orElseThrow(() -> new RuntimeException("Drum not found with number: " + drumNumber));

        drum.markAsMissing(notes);
        return drumRepository.save(drum);
    }

    @Transactional
    public DrumModel markDrumAsDamaged(String drumNumber, String notes) {
        DrumModel drum = drumRepository.findById(drumNumber)
                .orElseThrow(() -> new RuntimeException("Drum not found with number: " + drumNumber));

        drum.markAsDamaged(notes);
        return drumRepository.save(drum);
    }

    // Query Operations
    public List<DrumModel> getDrumsByOrder(String orderId) {
        return drumRepository.findByOrderOrderId(orderId);
    }

    public List<DrumModel> getDrumsByShipment(String shipmentId) {
        return drumRepository.findByShipmentShipmentId(shipmentId);
    }

    public List<DrumModel> getDrumsByStatus(DrumStatus status) {
        return drumRepository.findByStatus(status);
    }

    public List<DrumModel> getAvailableDrums() {
        return drumRepository.findByStatusAndOrderIsNull(DrumStatus.AVAILABLE);
    }

    public List<DrumModel> getMissingDrums() {
        return drumRepository.findByStatusOrderByUpdatedAtDesc(DrumStatus.MISSING);
    }

    public List<DrumModel> searchDrumsByNumber(String searchTerm) {
        return drumRepository.findByDrumNumberContainingIgnoreCase(searchTerm);
    }

    // Summary Operations
    public Integer getTotalDrumCountByOrder(String orderId) {
        return drumRepository.countDrumsByOrderId(orderId);
    }

    public Integer getTotalDrumCountByShipment(String shipmentId) {
        return drumRepository.countDrumsByShipmentId(shipmentId);
    }

    public BigDecimal getTotalNetWeightByOrder(String orderId) {
        return drumRepository.getTotalNetWeightByOrderId(orderId);
    }

    public BigDecimal getTotalGrossWeightByOrder(String orderId) {
        return drumRepository.getTotalGrossWeightByOrderId(orderId);
    }

    public BigDecimal getTotalLengthByOrder(String orderId) {
        return drumRepository.getTotalLengthByOrderId(orderId);
    }

    public BigDecimal getTotalNetWeightByShipment(String shipmentId) {
        return drumRepository.getTotalNetWeightByShipmentId(shipmentId);
    }

    public BigDecimal getTotalGrossWeightByShipment(String shipmentId) {
        return drumRepository.getTotalGrossWeightByShipmentId(shipmentId);
    }

    public BigDecimal getTotalLengthByShipment(String shipmentId) {
        return drumRepository.getTotalLengthByShipmentId(shipmentId);
    }

    // Bulk Operations
    public List<DrumModel> createMultipleDrums(List<DrumModel> drums) {
        for (DrumModel drum : drums) {
            if (drumRepository.existsByDrumNumber(drum.getDrumNumber())) {
                throw new IllegalArgumentException("Drum with number " + drum.getDrumNumber() + " already exists");
            }
            if (!drum.isValidWeights()) {
                throw new IllegalArgumentException("Invalid weights for drum " + drum.getDrumNumber());
            }
        }
        return drumRepository.saveAll(drums);
    }

    public BulkDrumCreationResult createMultipleDrumsWithSkipDuplicates(List<DrumModel> drums) {
        List<DrumModel> createdDrums = new ArrayList<>();
        List<String> skippedDrums = new ArrayList<>();
        List<String> validationErrors = new ArrayList<>();

        for (DrumModel drum : drums) {
            try {
                // Set default status and timestamps if not provided
                if (drum.getStatus() == null) {
                    drum.setStatus(DrumStatus.AVAILABLE);
                }
                if (drum.getCreatedAt() == null) {
                    drum.setCreatedAt(new Date());
                }
                if (drum.getUpdatedAt() == null) {
                    drum.setUpdatedAt(new Date());
                }

                // Handle container assignment if containerNo is provided
                if (drum.getContainerNo() != null && !drum.getContainerNo().trim().isEmpty()) {
                    // Check if container exists, create if it doesn't
                    ContainerModel container = containerService.createOrGetContainer(drum.getContainerNo());
                    drum.setContainer(container);
                    
                    // Check for duplicate drum number in the same container
                    if (drumRepository.existsByContainerAndDrumNumber(container, drum.getDrumNumber())) {
                        skippedDrums.add(drum.getDrumNumber() + " (duplicate in container " + drum.getContainerNo() + ")");
                        continue;
                    }
                } else {
                    // Check for duplicate drum number globally if no container
                    if (drumRepository.existsByDrumNumber(drum.getDrumNumber())) {
                        skippedDrums.add(drum.getDrumNumber() + " (already exists)");
                        continue;
                    }
                }

                // Validate weights
                if (!drum.isValidWeights()) {
                    validationErrors.add(drum.getDrumNumber() + " (invalid weights: gross weight must be >= net weight)");
                    continue;
                }

                // Save the drum
                DrumModel savedDrum = drumRepository.save(drum);
                createdDrums.add(savedDrum);

            } catch (Exception e) {
                validationErrors.add(drum.getDrumNumber() + " (error: " + e.getMessage() + ")");
            }
        }

        return new BulkDrumCreationResult(createdDrums, skippedDrums, validationErrors);
    }

    /**
     * Create multiple drums from Excel/CSV data with container support
     */
    @Transactional
    public BulkDrumCreationResult createMultipleDrumsFromExcel(List<Map<String, Object>> drumData) {
        List<DrumModel> drums = new ArrayList<>();
        
        for (Map<String, Object> data : drumData) {
            DrumModel drum = new DrumModel();
            
            drum.setDrumNumber(String.valueOf(data.get("drumNumber")));
            drum.setLengthKms(new BigDecimal(String.valueOf(data.get("length"))));
            drum.setNetWeightMt(new BigDecimal(String.valueOf(data.get("netWeight"))));
            drum.setGrossWeightMt(new BigDecimal(String.valueOf(data.get("grossWeight"))));
            drum.setContainerNo(String.valueOf(data.get("containerNumber")));
            
            if (data.get("notes") != null) {
                drum.setNotes(String.valueOf(data.get("notes")));
            }
            
            drums.add(drum);
        }
        
        return createMultipleDrumsWithSkipDuplicates(drums);
    }

    @Transactional
    public List<DrumModel> markMultipleDrumsAsDelivered(List<String> drumNumbers) {
        List<DrumModel> drums = drumRepository.findAllById(drumNumbers);
        for (DrumModel drum : drums) {
            drum.markAsDelivered();
        }
        return drumRepository.saveAll(drums);
    }

    // History tracking methods
    private void recordLocationHistory(DrumModel drum, DrumStatus previousStatus, DrumStatus newStatus,
                                     String previousOrderId, String newOrderId,
                                     String previousShipmentId, String newShipmentId,
                                     String changedBy, String notes) {
        DrumLocationHistory history = new DrumLocationHistory(
            drum, previousStatus, newStatus,
            previousOrderId, newOrderId,
            previousShipmentId, newShipmentId,
            changedBy, notes
        );
        historyRepository.save(history);
    }

    private void recordLocationHistory(DrumModel drum, DrumStatus previousStatus, DrumStatus newStatus, String changedBy) {
        String previousOrderId = drum.getOrder() != null ? drum.getOrder().getId() : null;
        String newOrderId = previousOrderId; // Same order
        String previousShipmentId = drum.getShipment() != null ? drum.getShipment().getId() : null;
        String newShipmentId = previousShipmentId; // Same shipment
        
        recordLocationHistory(drum, previousStatus, newStatus, 
                             previousOrderId, newOrderId, 
                             previousShipmentId, newShipmentId, 
                             changedBy, null);
    }

    // Enhanced assignment methods with history tracking
    public DrumModel assignDrumToOrderWithHistory(String drumNumber, String orderId, String changedBy) {
        DrumModel drum = drumRepository.findById(drumNumber)
                .orElseThrow(() -> new RuntimeException("Drum not found with number: " + drumNumber));

        OrderModel order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));

        // Record history before change
        DrumStatus previousStatus = drum.getStatus();
        String previousOrderId = drum.getOrder() != null ? drum.getOrder().getId() : null;
        String previousShipmentId = drum.getShipment() != null ? drum.getShipment().getId() : null;

        drum.assignToOrder(order);
        DrumModel savedDrum = drumRepository.save(drum);

        // Record history after change
        recordLocationHistory(savedDrum, previousStatus, DrumStatus.IN_ORDER,
                             previousOrderId, orderId,
                             previousShipmentId, null,
                             changedBy, "Drum assigned to order");

        return savedDrum;
    }

    public DrumModel assignDrumToShipmentWithHistory(String drumNumber, String shipmentId, String changedBy) {
        DrumModel drum = drumRepository.findById(drumNumber)
                .orElseThrow(() -> new RuntimeException("Drum not found with number: " + drumNumber));

        ShipmentModel shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found with ID: " + shipmentId));

        // Record history before change
        DrumStatus previousStatus = drum.getStatus();
        String previousOrderId = drum.getOrder() != null ? drum.getOrder().getId() : null;
        String previousShipmentId = drum.getShipment() != null ? drum.getShipment().getId() : null;

        drum.assignToShipment(shipment);
        shipment.addDrum(drum); // Ensure bidirectional relationship
        
        // If drum has a container, assign the container to the shipment too
        if (drum.getContainer() != null) {
            ContainerModel container = drum.getContainer();
            // Only assign if container is not already assigned to this shipment
            if (container.getShipment() == null || !container.getShipment().getId().equals(shipmentId)) {
                container.assignToShipment(shipment);
                containerRepository.save(container);
            }
        }
        
        DrumModel savedDrum = drumRepository.save(drum);
        shipmentRepository.save(shipment); // Save shipment side to maintain bidirectional consistency

        // Record history after change
        recordLocationHistory(savedDrum, previousStatus, DrumStatus.IN_SHIPMENT,
                             previousOrderId, previousOrderId,
                             previousShipmentId, shipmentId,
                             changedBy, "Drum assigned to shipment");

        return savedDrum;
    }

    public DrumModel markDrumAsMissingWithHistory(String drumNumber, String notes, String changedBy) {
        DrumModel drum = drumRepository.findById(drumNumber)
                .orElseThrow(() -> new RuntimeException("Drum not found with number: " + drumNumber));

        // Record history before change
        DrumStatus previousStatus = drum.getStatus();
        String previousOrderId = drum.getOrder() != null ? drum.getOrder().getId() : null;
        String previousShipmentId = drum.getShipment() != null ? drum.getShipment().getId() : null;

        drum.markAsMissing(notes);
        DrumModel savedDrum = drumRepository.save(drum);

        // Record history after change
        recordLocationHistory(savedDrum, previousStatus, DrumStatus.MISSING,
                             previousOrderId, previousOrderId,
                             previousShipmentId, previousShipmentId,
                             changedBy, notes);

        return savedDrum;
    }

    // History query methods
    public List<DrumLocationHistory> getDrumHistory(String drumNumber) {
        return historyRepository.findByDrumDrumNumberOrderByChangeDateDesc(drumNumber);
    }

    public List<DrumLocationHistory> getOrderHistory(String orderId) {
        return historyRepository.findByOrderId(orderId);
    }

    public List<DrumLocationHistory> getShipmentHistory(String shipmentId) {
        return historyRepository.findByShipmentId(shipmentId);
    }

    public List<DrumLocationHistory> getRecentMissingDrums(java.util.Date sinceDate) {
        return historyRepository.findByNewStatusAndChangeDateAfterOrderByChangeDateDesc(DrumStatus.MISSING, sinceDate);
    }

    // Summary Operations
    public Map<String, Object> getDrumsSummary() {
        long totalDrums = drumRepository.count();
        long availableDrums = drumRepository.countByStatusAndOrderIsNull(DrumStatus.AVAILABLE);
        long inOrderDrums = drumRepository.countByStatus(DrumStatus.IN_ORDER);
        long inShipmentDrums = drumRepository.countByStatus(DrumStatus.IN_SHIPMENT);
        long deliveredDrums = drumRepository.countByStatus(DrumStatus.DELIVERED);
        long missingDrums = drumRepository.countByStatus(DrumStatus.MISSING);
        long damagedDrums = drumRepository.countByStatus(DrumStatus.DAMAGED);

        // In transit = drums that are in orders or shipments
        long inTransitDrums = inOrderDrums + inShipmentDrums;

        return Map.of(
            "total", totalDrums,
            "available", availableDrums,
            "inTransit", inTransitDrums,
            "inOrder", inOrderDrums,
            "inShipment", inShipmentDrums,
            "delivered", deliveredDrums,
            "missing", missingDrums,
            "damaged", damagedDrums
        );
    }

    // Data Consistency Operations
    public Map<String, Object> checkDataConsistency() {
        List<String> inconsistencies = new ArrayList<>();
        int totalInconsistencies = 0;

        // Check for drums with IN_ORDER status but no order assignment
        List<DrumModel> orphanedInOrderDrums = drumRepository.findByStatusAndOrderIsNull(DrumStatus.IN_ORDER);
        if (!orphanedInOrderDrums.isEmpty()) {
            inconsistencies.add("Drums with IN_ORDER status but no order assignment: " + 
                orphanedInOrderDrums.size() + " drums");
            totalInconsistencies += orphanedInOrderDrums.size();
        }

        // Check for drums with IN_SHIPMENT status but no shipment assignment
        List<DrumModel> orphanedInShipmentDrums = drumRepository.findByStatusAndShipmentIsNull(DrumStatus.IN_SHIPMENT);
        if (!orphanedInShipmentDrums.isEmpty()) {
            inconsistencies.add("Drums with IN_SHIPMENT status but no shipment assignment: " + 
                orphanedInShipmentDrums.size() + " drums");
            totalInconsistencies += orphanedInShipmentDrums.size();
        }

        // Check for drums with order assignment but AVAILABLE status
        List<DrumModel> availableWithOrderDrums = drumRepository.findByStatusAndOrderIsNotNull(DrumStatus.AVAILABLE);
        if (!availableWithOrderDrums.isEmpty()) {
            inconsistencies.add("Drums with AVAILABLE status but have order assignment: " + 
                availableWithOrderDrums.size() + " drums");
            totalInconsistencies += availableWithOrderDrums.size();
        }

        // Check for drums with shipment assignment but not IN_SHIPMENT status
        List<DrumModel> shipmentDrumsWrongStatus = drumRepository.findByShipmentIsNotNullAndStatusNot(DrumStatus.IN_SHIPMENT);
        if (!shipmentDrumsWrongStatus.isEmpty()) {
            inconsistencies.add("Drums with shipment assignment but not IN_SHIPMENT status: " + 
                shipmentDrumsWrongStatus.size() + " drums");
            totalInconsistencies += shipmentDrumsWrongStatus.size();
        }

        return Map.of(
            "totalInconsistencies", totalInconsistencies,
            "inconsistencies", inconsistencies,
            "isConsistent", totalInconsistencies == 0,
            "orphanedInOrderDrums", orphanedInOrderDrums.stream().map(DrumModel::getDrumNumber).toList(),
            "orphanedInShipmentDrums", orphanedInShipmentDrums.stream().map(DrumModel::getDrumNumber).toList(),
            "availableWithOrderDrums", availableWithOrderDrums.stream().map(DrumModel::getDrumNumber).toList(),
            "shipmentDrumsWrongStatus", shipmentDrumsWrongStatus.stream().map(DrumModel::getDrumNumber).toList()
        );
    }

    @Transactional
    public Map<String, Object> repairDataConsistency() {
        int repairedCount = 0;
        List<String> repairs = new ArrayList<>();

        try {
            // Fix drums with IN_ORDER status but no order assignment -> set to AVAILABLE
            List<DrumModel> orphanedInOrderDrums = drumRepository.findByStatusAndOrderIsNull(DrumStatus.IN_ORDER);
            for (DrumModel drum : orphanedInOrderDrums) {
                drum.setStatus(DrumStatus.AVAILABLE);
                drum.setUpdatedAt(new Date());
                drumRepository.save(drum);
                repairs.add("Set drum " + drum.getDrumNumber() + " to AVAILABLE (was IN_ORDER with no order)");
                repairedCount++;
            }

            // Fix drums with IN_SHIPMENT status but no shipment assignment -> set to IN_ORDER or AVAILABLE
            List<DrumModel> orphanedInShipmentDrums = drumRepository.findByStatusAndShipmentIsNull(DrumStatus.IN_SHIPMENT);
            for (DrumModel drum : orphanedInShipmentDrums) {
                if (drum.getOrder() != null) {
                    drum.setStatus(DrumStatus.IN_ORDER);
                    repairs.add("Set drum " + drum.getDrumNumber() + " to IN_ORDER (was IN_SHIPMENT with no shipment but has order)");
                } else {
                    drum.setStatus(DrumStatus.AVAILABLE);
                    repairs.add("Set drum " + drum.getDrumNumber() + " to AVAILABLE (was IN_SHIPMENT with no shipment or order)");
                }
                drum.setUpdatedAt(new Date());
                drumRepository.save(drum);
                repairedCount++;
            }

            // Fix drums with order assignment but AVAILABLE status -> set to IN_ORDER
            List<DrumModel> availableWithOrderDrums = drumRepository.findByStatusAndOrderIsNotNull(DrumStatus.AVAILABLE);
            for (DrumModel drum : availableWithOrderDrums) {
                drum.setStatus(DrumStatus.IN_ORDER);
                drum.setUpdatedAt(new Date());
                drumRepository.save(drum);
                repairs.add("Set drum " + drum.getDrumNumber() + " to IN_ORDER (was AVAILABLE but has order assignment)");
                repairedCount++;
            }

            // Fix drums with shipment assignment but not IN_SHIPMENT status -> set to IN_SHIPMENT
            List<DrumModel> shipmentDrumsWrongStatus = drumRepository.findByShipmentIsNotNullAndStatusNot(DrumStatus.IN_SHIPMENT);
            for (DrumModel drum : shipmentDrumsWrongStatus) {
                // Only set to IN_SHIPMENT if not already delivered, missing, or damaged
                if (drum.getStatus() != DrumStatus.DELIVERED && 
                    drum.getStatus() != DrumStatus.MISSING && 
                    drum.getStatus() != DrumStatus.DAMAGED) {
                    drum.setStatus(DrumStatus.IN_SHIPMENT);
                    drum.setUpdatedAt(new Date());
                    drumRepository.save(drum);
                    repairs.add("Set drum " + drum.getDrumNumber() + " to IN_SHIPMENT (has shipment assignment)");
                    repairedCount++;
                }
            }

            return Map.of(
                "success", true,
                "repairedCount", repairedCount,
                "repairs", repairs,
                "message", "Successfully repaired " + repairedCount + " data inconsistencies"
            );

        } catch (Exception e) {
            return Map.of(
                "success", false,
                "error", "Failed to repair data consistency: " + e.getMessage(),
                "repairedCount", repairedCount,
                "repairs", repairs
            );
        }
    }

    // Container Assignment Operations
    @Transactional
    public List<DrumModel> assignDrumsToContainers(List<String> drumNumbers, List<String> containerNumbers) {
        if (drumNumbers.size() != containerNumbers.size()) {
            throw new IllegalArgumentException("Number of drums and containers must match");
        }

        List<DrumModel> updatedDrums = new ArrayList<>();
        
        for (int i = 0; i < drumNumbers.size(); i++) {
            String drumNumber = drumNumbers.get(i);
            String containerNumber = containerNumbers.get(i);
            
            DrumModel drum = drumRepository.findById(drumNumber)
                    .orElseThrow(() -> new RuntimeException("Drum not found with number: " + drumNumber));
                    
            drum.assignToContainer(containerNumber);
            updatedDrums.add(drumRepository.save(drum));
        }
        
        return updatedDrums;
    }

    @Transactional
    public List<DrumModel> assignDrumsToContainerWithQuantityDistribution(List<DrumModel> drums, 
                                                                           int drumsPerContainer, 
                                                                           String containerPrefix) {
        List<DrumModel> updatedDrums = new ArrayList<>();
        
        for (int i = 0; i < drums.size(); i++) {
            int containerIndex = i / drumsPerContainer;
            String containerNumber = generateContainerNumber(containerPrefix, containerIndex);
            
            DrumModel drum = drums.get(i);
            drum.assignToContainer(containerNumber);
            updatedDrums.add(drumRepository.save(drum));
        }
        
        return updatedDrums;
    }

    private String generateContainerNumber(String prefix, int index) {
        // Generate container number based on prefix + index
        // Example: ACBD + index 0 -> ACBD1234567, index 1 -> ACBD1234568
        String baseNumber = "1234567";
        int numberSuffix = Integer.parseInt(baseNumber) + index;
        return prefix + String.format("%07d", numberSuffix);
    }

    @Transactional
    public DrumModel removeContainerFromDrum(String drumNumber) {
        DrumModel drum = drumRepository.findById(drumNumber)
                .orElseThrow(() -> new RuntimeException("Drum not found with number: " + drumNumber));
                
        drum.removeFromContainer();
        return drumRepository.save(drum);
    }

    public List<DrumModel> getDrumsByContainer(String containerNumber) {
        return drumRepository.findByContainerNo(containerNumber);
    }

    public Map<String, Integer> getContainerDrumCounts(String shipmentId) {
        ShipmentModel shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found with ID: " + shipmentId));
                
        Map<String, Integer> containerCounts = new HashMap<>();
        
        for (DrumModel drum : shipment.getDrums()) {
            if (drum.getContainerNo() != null) {
                containerCounts.merge(drum.getContainerNo(), 1, Integer::sum);
            }
        }
        
        return containerCounts;
    }
}