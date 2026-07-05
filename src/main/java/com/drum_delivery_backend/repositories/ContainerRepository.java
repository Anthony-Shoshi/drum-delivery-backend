package com.drum_delivery_backend.repositories;

import com.drum_delivery_backend.models.ContainerModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContainerRepository extends JpaRepository<ContainerModel, String> {
    
    // Find containers by shipment
    List<ContainerModel> findByShipmentShipmentId(String shipmentId);
    
    // Find containers without shipment assignment
    List<ContainerModel> findByShipmentIsNull();
    
    // Find containers by shipment ID with null check
    @Query("SELECT c FROM ContainerModel c WHERE c.shipment.shipmentId = :shipmentId")
    List<ContainerModel> findContainersByShipmentId(@Param("shipmentId") String shipmentId);
    
    // Check if container exists by number
    boolean existsByContainerNumber(String containerNumber);
    
    // Find container by number (alternative to findById for clarity)
    Optional<ContainerModel> findByContainerNumber(String containerNumber);
    
    // Count containers by shipment
    @Query("SELECT COUNT(c) FROM ContainerModel c WHERE c.shipment.shipmentId = :shipmentId")
    Long countContainersByShipmentId(@Param("shipmentId") String shipmentId);
    
    // Count containers without shipment assignment
    Long countByShipmentIsNull();
    
    // Find containers by pattern (for searching)
    List<ContainerModel> findByContainerNumberContainingIgnoreCase(String containerNumber);
    
    // Find containers with drums
    @Query("SELECT c FROM ContainerModel c WHERE SIZE(c.drums) > 0")
    List<ContainerModel> findContainersWithDrums();
    
    // Find empty containers
    @Query("SELECT c FROM ContainerModel c WHERE SIZE(c.drums) = 0")
    List<ContainerModel> findEmptyContainers();
    
    // Get container with maximum drums count
    @Query("SELECT c FROM ContainerModel c WHERE SIZE(c.drums) = (SELECT MAX(SIZE(c2.drums)) FROM ContainerModel c2)")
    List<ContainerModel> findContainersWithMaxDrums();
    
    // Get total drum count across all containers
    @Query("SELECT SUM(SIZE(c.drums)) FROM ContainerModel c")
    Long getTotalDrumsInAllContainers();
    
    // Find containers by creation date range
    @Query("SELECT c FROM ContainerModel c WHERE c.createdAt BETWEEN :startDate AND :endDate")
    List<ContainerModel> findByCreatedAtBetween(@Param("startDate") java.util.Date startDate, 
                                                @Param("endDate") java.util.Date endDate);
}