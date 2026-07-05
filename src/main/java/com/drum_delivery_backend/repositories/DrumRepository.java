package com.drum_delivery_backend.repositories;

import com.drum_delivery_backend.models.ContainerModel;
import com.drum_delivery_backend.models.DrumModel;
import com.drum_delivery_backend.models.DrumStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface DrumRepository extends JpaRepository<DrumModel, String>, JpaSpecificationExecutor<DrumModel> {
    
    // Find drums by order
    List<DrumModel> findByOrderOrderId(String orderId);
    
    // Find drums by shipment
    List<DrumModel> findByShipmentShipmentId(String shipmentId);
    
    // Find drums by status
    List<DrumModel> findByStatus(DrumStatus status);
    
    // Find drums by multiple statuses
    List<DrumModel> findByStatusIn(List<DrumStatus> statuses);
    
    // Find available drums (not assigned to any order)
    List<DrumModel> findByStatusAndOrderIsNull(DrumStatus status);
    
    // Count drums by order
    @Query("SELECT COUNT(d) FROM DrumModel d WHERE d.order.orderId = :orderId")
    Integer countDrumsByOrderId(@Param("orderId") String orderId);
    
    // Count drums by shipment
    @Query("SELECT COUNT(d) FROM DrumModel d WHERE d.shipment.shipmentId = :shipmentId")
    Integer countDrumsByShipmentId(@Param("shipmentId") String shipmentId);
    
    // Get total net weight for order
    @Query("SELECT COALESCE(SUM(d.netWeightMt), 0) FROM DrumModel d WHERE d.order.orderId = :orderId")
    BigDecimal getTotalNetWeightByOrderId(@Param("orderId") String orderId);
    
    // Get total gross weight for order
    @Query("SELECT COALESCE(SUM(d.grossWeightMt), 0) FROM DrumModel d WHERE d.order.orderId = :orderId")
    BigDecimal getTotalGrossWeightByOrderId(@Param("orderId") String orderId);
    
    // Get total net weight for shipment
    @Query("SELECT COALESCE(SUM(d.netWeightMt), 0) FROM DrumModel d WHERE d.shipment.shipmentId = :shipmentId")
    BigDecimal getTotalNetWeightByShipmentId(@Param("shipmentId") String shipmentId);
    
    // Get total gross weight for shipment
    @Query("SELECT COALESCE(SUM(d.grossWeightMt), 0) FROM DrumModel d WHERE d.shipment.shipmentId = :shipmentId")
    BigDecimal getTotalGrossWeightByShipmentId(@Param("shipmentId") String shipmentId);
    
    // Get total length for order
    @Query("SELECT COALESCE(SUM(d.lengthKms), 0) FROM DrumModel d WHERE d.order.orderId = :orderId")
    BigDecimal getTotalLengthByOrderId(@Param("orderId") String orderId);
    
    // Get total length for shipment
    @Query("SELECT COALESCE(SUM(d.lengthKms), 0) FROM DrumModel d WHERE d.shipment.shipmentId = :shipmentId")
    BigDecimal getTotalLengthByShipmentId(@Param("shipmentId") String shipmentId);
    
    // Find drums that are missing
    List<DrumModel> findByStatusOrderByUpdatedAtDesc(DrumStatus status);
    
    // Find drums by drum number pattern (for searching)
    List<DrumModel> findByDrumNumberContainingIgnoreCase(String drumNumber);
    
    // Check if drum exists by number
    boolean existsByDrumNumber(String drumNumber);
    
    // Find drums without order assignment
    List<DrumModel> findByOrderIsNull();
    
    // Find drums without shipment assignment but with order
    List<DrumModel> findByShipmentIsNullAndOrderIsNotNull();
    
    // Find drums by weight range
    List<DrumModel> findByNetWeightMtBetween(BigDecimal minWeight, BigDecimal maxWeight);
    
    // Find drums by length range
    List<DrumModel> findByLengthKmsBetween(BigDecimal minLength, BigDecimal maxLength);
    
    // Additional methods for consistency checking
    List<DrumModel> findByStatusAndShipmentIsNull(DrumStatus status);
    List<DrumModel> findByStatusAndOrderIsNotNull(DrumStatus status);
    List<DrumModel> findByShipmentIsNotNullAndStatusNot(DrumStatus status);
    
    // Count methods for summary statistics
    long countByStatus(DrumStatus status);
    long countByStatusAndOrderIsNull(DrumStatus status);
    
    // Container-related methods (legacy string-based)
    List<DrumModel> findByContainerNo(String containerNo);
    List<DrumModel> findByContainerNoIsNull();
    List<DrumModel> findByContainerNoIsNotNull();
    
    @Query("SELECT COUNT(d) FROM DrumModel d WHERE d.containerNo = :containerNo")
    Integer countDrumsByContainerNo(@Param("containerNo") String containerNo);
    
    // Container-related methods (entity-based)
    List<DrumModel> findByContainer(ContainerModel container);
    List<DrumModel> findByContainerIsNull();
    List<DrumModel> findByContainerIsNotNull();
    
    // Check if drum exists in container
    boolean existsByContainerAndDrumNumber(ContainerModel container, String drumNumber);
    
    @Query("SELECT COUNT(d) FROM DrumModel d WHERE d.container = :container")
    Integer countDrumsByContainer(@Param("container") ContainerModel container);
    
    // Find drums by container ID
    @Query("SELECT d FROM DrumModel d WHERE d.container.containerNumber = :containerNumber")
    List<DrumModel> findByContainerContainerNumber(@Param("containerNumber") String containerNumber);
}