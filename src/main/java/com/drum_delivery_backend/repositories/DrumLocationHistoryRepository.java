package com.drum_delivery_backend.repositories;

import com.drum_delivery_backend.models.DrumLocationHistory;
import com.drum_delivery_backend.models.DrumStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface DrumLocationHistoryRepository extends JpaRepository<DrumLocationHistory, Long> {
    
    // Find history by drum number
    List<DrumLocationHistory> findByDrumDrumNumberOrderByChangeDateDesc(String drumNumber);
    
    // Find history by drum number within date range
    List<DrumLocationHistory> findByDrumDrumNumberAndChangeDateBetweenOrderByChangeDateDesc(
            String drumNumber, Date startDate, Date endDate);
    
    // Find history by order ID
    @Query("SELECT h FROM DrumLocationHistory h WHERE h.previousOrderId = :orderId OR h.newOrderId = :orderId ORDER BY h.changeDate DESC")
    List<DrumLocationHistory> findByOrderId(@Param("orderId") String orderId);
    
    // Find history by shipment ID
    @Query("SELECT h FROM DrumLocationHistory h WHERE h.previousShipmentId = :shipmentId OR h.newShipmentId = :shipmentId ORDER BY h.changeDate DESC")
    List<DrumLocationHistory> findByShipmentId(@Param("shipmentId") String shipmentId);
    
    // Find history by status change
    List<DrumLocationHistory> findByNewStatusOrderByChangeDateDesc(DrumStatus status);
    
    // Find history by who made the change
    List<DrumLocationHistory> findByChangedByOrderByChangeDateDesc(String changedBy);
    
    // Find recent changes (last N days)
    @Query("SELECT h FROM DrumLocationHistory h WHERE h.changeDate >= :sinceDate ORDER BY h.changeDate DESC")
    List<DrumLocationHistory> findRecentChanges(@Param("sinceDate") Date sinceDate);
    
    // Find status transitions to missing
    List<DrumLocationHistory> findByNewStatusAndChangeDateAfterOrderByChangeDateDesc(
            DrumStatus status, Date afterDate);
    
    // Get audit trail for specific drum within date range
    @Query("SELECT h FROM DrumLocationHistory h WHERE h.drum.drumNumber = :drumNumber " +
           "AND h.changeDate BETWEEN :startDate AND :endDate ORDER BY h.changeDate DESC")
    List<DrumLocationHistory> getAuditTrail(@Param("drumNumber") String drumNumber, 
                                          @Param("startDate") Date startDate, 
                                          @Param("endDate") Date endDate);
    
    // Find drums that went missing in a specific order or shipment
    @Query("SELECT h FROM DrumLocationHistory h WHERE h.newStatus = 'MISSING' " +
           "AND (h.previousOrderId = :id OR h.previousShipmentId = :id) ORDER BY h.changeDate DESC")
    List<DrumLocationHistory> findMissingDrumsFromOrderOrShipment(@Param("id") String id);
    
    // Count changes by user
    @Query("SELECT COUNT(h) FROM DrumLocationHistory h WHERE h.changedBy = :username " +
           "AND h.changeDate >= :sinceDate")
    Long countChangesByUser(@Param("username") String username, @Param("sinceDate") Date sinceDate);
    
    // Find location changes for drum
    @Query("SELECT h FROM DrumLocationHistory h WHERE h.drum.drumNumber = :drumNumber " +
           "AND (h.location IS NOT NULL OR h.previousOrderId != h.newOrderId OR h.previousShipmentId != h.newShipmentId) " +
           "ORDER BY h.changeDate DESC")
    List<DrumLocationHistory> findLocationChangesForDrum(@Param("drumNumber") String drumNumber);
}