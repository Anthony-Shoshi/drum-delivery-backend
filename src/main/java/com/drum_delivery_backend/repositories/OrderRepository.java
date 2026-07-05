package com.drum_delivery_backend.repositories;

import com.drum_delivery_backend.models.OrderModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderModel, String> {
    
    Optional<OrderModel> findByOrderNumber(String orderNumber);
    
    @Query("SELECT o FROM OrderModel o WHERE o.client.id = :clientId")
    List<OrderModel> findByClientId(@Param("clientId") String clientId);
    
    @Query("SELECT o FROM OrderModel o WHERE o.shipment.id = :shipmentId")
    List<OrderModel> findByShipmentId(@Param("shipmentId") String shipmentId);
    
    List<OrderModel> findByStatus(String status);
    
    List<OrderModel> findByCreationDateBetween(Date startDate, Date endDate);
    
    List<OrderModel> findByShipmentIsNull();
}