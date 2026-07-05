package com.drum_delivery_backend.services;

import com.drum_delivery_backend.models.ContainerModel;
import com.drum_delivery_backend.models.DrumModel;
import com.drum_delivery_backend.models.ShipmentModel;
import com.drum_delivery_backend.repositories.ContainerRepository;
import com.drum_delivery_backend.repositories.ShipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ContainerService {

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    /**
     * Create a new container
     */
    public ContainerModel createContainer(String containerNumber) {
        validateContainerNumber(containerNumber);
        
        if (containerRepository.existsByContainerNumber(containerNumber)) {
            throw new IllegalArgumentException("Container " + containerNumber + " already exists");
        }
        
        ContainerModel container = new ContainerModel(containerNumber);
        return containerRepository.save(container);
    }

    /**
     * Create or get existing container
     */
    public ContainerModel createOrGetContainer(String containerNumber) {
        validateContainerNumber(containerNumber);
        
        Optional<ContainerModel> existingContainer = containerRepository.findByContainerNumber(containerNumber);
        if (existingContainer.isPresent()) {
            return existingContainer.get();
        }
        
        ContainerModel container = new ContainerModel(containerNumber);
        return containerRepository.save(container);
    }

    /**
     * Create container with shipment assignment
     */
    public ContainerModel createContainer(String containerNumber, String shipmentId) {
        validateContainerNumber(containerNumber);
        
        ShipmentModel shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found with ID: " + shipmentId));
        
        if (containerRepository.existsByContainerNumber(containerNumber)) {
            throw new IllegalArgumentException("Container " + containerNumber + " already exists");
        }
        
        ContainerModel container = new ContainerModel(containerNumber, shipment);
        return containerRepository.save(container);
    }

    /**
     * Get container by number
     */
    public Optional<ContainerModel> getContainer(String containerNumber) {
        return containerRepository.findByContainerNumber(containerNumber);
    }

    /**
     * Get container by number or throw exception
     */
    public ContainerModel getContainerById(String containerNumber) {
        return containerRepository.findById(containerNumber)
                .orElseThrow(() -> new RuntimeException("Container not found: " + containerNumber));
    }

    /**
     * Get all containers
     */
    public List<ContainerModel> getAllContainers() {
        return containerRepository.findAll();
    }

    /**
     * Get containers by shipment ID
     */
    public List<ContainerModel> getContainersByShipmentId(String shipmentId) {
        return containerRepository.findContainersByShipmentId(shipmentId);
    }

    /**
     * Get unassigned containers
     */
    public List<ContainerModel> getUnassignedContainers() {
        return containerRepository.findByShipmentIsNull();
    }

    /**
     * Assign container to shipment
     */
    public ContainerModel assignContainerToShipment(String containerNumber, String shipmentId) {
        ContainerModel container = getContainerById(containerNumber);
        
        ShipmentModel shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new RuntimeException("Shipment not found with ID: " + shipmentId));
        
        container.assignToShipment(shipment);
        return containerRepository.save(container);
    }

    /**
     * Remove container from shipment
     */
    public ContainerModel removeContainerFromShipment(String containerNumber) {
        ContainerModel container = getContainerById(containerNumber);
        container.removeFromShipment();
        return containerRepository.save(container);
    }

    /**
     * Add drum to container
     */
    public ContainerModel addDrumToContainer(String containerNumber, DrumModel drum) {
        ContainerModel container = getContainerById(containerNumber);
        container.addDrum(drum);
        return containerRepository.save(container);
    }

    /**
     * Remove drum from container
     */
    public ContainerModel removeDrumFromContainer(String containerNumber, DrumModel drum) {
        ContainerModel container = getContainerById(containerNumber);
        container.removeDrum(drum);
        return containerRepository.save(container);
    }

    /**
     * Update container
     */
    public ContainerModel updateContainer(String containerNumber, ContainerModel containerDetails) {
        ContainerModel container = getContainerById(containerNumber);
        
        // Update modifiable fields
        if (containerDetails.getNotes() != null) {
            container.setNotes(containerDetails.getNotes());
        }
        
        container.setUpdatedAt(new Date());
        return containerRepository.save(container);
    }

    /**
     * Delete container (only if empty)
     */
    public void deleteContainer(String containerNumber) {
        ContainerModel container = getContainerById(containerNumber);
        
        if (container.getDrumCount() > 0) {
            throw new IllegalStateException("Cannot delete container " + containerNumber + " - it contains " + 
                                          container.getDrumCount() + " drums");
        }
        
        containerRepository.delete(container);
    }

    /**
     * Get containers with drums
     */
    public List<ContainerModel> getContainersWithDrums() {
        return containerRepository.findContainersWithDrums();
    }

    /**
     * Get empty containers
     */
    public List<ContainerModel> getEmptyContainers() {
        return containerRepository.findEmptyContainers();
    }

    /**
     * Get container statistics
     */
    public ContainerStatistics getContainerStatistics() {
        long totalContainers = containerRepository.count();
        long assignedContainers = totalContainers - containerRepository.countByShipmentIsNull();
        long unassignedContainers = containerRepository.countByShipmentIsNull();
        Long totalDrums = containerRepository.getTotalDrumsInAllContainers();
        
        return new ContainerStatistics(
            totalContainers,
            assignedContainers,
            unassignedContainers,
            totalDrums != null ? totalDrums : 0L
        );
    }

    /**
     * Search containers by number pattern
     */
    public List<ContainerModel> searchContainers(String searchTerm) {
        return containerRepository.findByContainerNumberContainingIgnoreCase(searchTerm);
    }

    /**
     * Validate container number format
     */
    private void validateContainerNumber(String containerNumber) {
        if (containerNumber == null || containerNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Container number is required");
        }
        
        if (!containerNumber.matches("^[A-Z]{4}[0-9]{7}$")) {
            throw new IllegalArgumentException("Invalid container number format. Must be 4 letters followed by 7 digits (e.g., ACBD1234567)");
        }
    }

    /**
     * Container statistics data class
     */
    public static class ContainerStatistics {
        private final long totalContainers;
        private final long assignedContainers;
        private final long unassignedContainers;
        private final long totalDrums;

        public ContainerStatistics(long totalContainers, long assignedContainers, long unassignedContainers, long totalDrums) {
            this.totalContainers = totalContainers;
            this.assignedContainers = assignedContainers;
            this.unassignedContainers = unassignedContainers;
            this.totalDrums = totalDrums;
        }

        // Getters
        public long getTotalContainers() { return totalContainers; }
        public long getAssignedContainers() { return assignedContainers; }
        public long getUnassignedContainers() { return unassignedContainers; }
        public long getTotalDrums() { return totalDrums; }
    }
}