package com.drum_delivery_backend.repositories;

import com.drum_delivery_backend.models.TruckDeliveryModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface TruckDeliveryRepository extends JpaRepository<TruckDeliveryModel, String> {
    
    List<TruckDeliveryModel> findByShipmentId(String shipmentId);
    
    List<TruckDeliveryModel> findByTruckNumber(String truckNumber);
    
    List<TruckDeliveryModel> findByStatus(String status);
    
    List<TruckDeliveryModel> findByScheduledDateBetween(Date startDate, Date endDate);
    
    List<TruckDeliveryModel> findByActualArrivalDateBetween(Date startDate, Date endDate);
    
    @Query("SELECT td FROM TruckDeliveryModel td JOIN td.orderIds o WHERE o = :orderId")
    List<TruckDeliveryModel> findByOrderId(@Param("orderId") String orderId);
    
    List<TruckDeliveryModel> findByDriverNameContainingIgnoreCase(String driverName);
}