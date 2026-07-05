package com.drum_delivery_backend.controllers;

import com.drum_delivery_backend.models.OrderModel;
import com.drum_delivery_backend.models.ShipmentModel;
import com.drum_delivery_backend.repositories.OrderRepository;
import com.drum_delivery_backend.repositories.ShipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/test")
public class TestController {
    
    @Autowired
    private OrderRepository orderRepository;
    
    @Autowired
    private ShipmentRepository shipmentRepository;
    
    @GetMapping("/all")
    public String allAccess() {
        return "Public Content.";
    }
    
    @GetMapping("/user")
    @PreAuthorize("hasRole('USER') or hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public String userAccess() {
        return "User Content.";
    }

    @GetMapping("/operator")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public String operatorAccess() {
        return "Operator Board.";
    }
    
    @GetMapping("/manager")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public String managerAccess() {
        return "Manager Board.";
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminAccess() {
        return "Admin Board.";
    }
    
    @GetMapping("/fix-order-relationships")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Map<String, Object> fixOrderShipmentRelationships() {
        Map<String, Object> result = new HashMap<>();
        int fixedCount = 0;
        
        // Get all shipments
        List<ShipmentModel> shipments = shipmentRepository.findAll();
        
        for (ShipmentModel shipment : shipments) {
            // For each shipment, check if its orders actually reference this shipment
            List<OrderModel> ordersInShipment = shipment.getOrders();
            
            for (OrderModel order : ordersInShipment) {
                // If the order doesn't reference this shipment, fix it
                if (order.getShipment() == null || !order.getShipment().getId().equals(shipment.getId())) {
                    order.setShipment(shipment);
                    order.setStatus("ASSIGNED_TO_SHIPMENT");
                    orderRepository.save(order);
                    fixedCount++;
                }
            }
        }
        
        // Also check for orphaned orders that reference non-existent shipments
        List<OrderModel> allOrders = orderRepository.findAll();
        for (OrderModel order : allOrders) {
            if (order.getShipment() != null) {
                // Check if the referenced shipment exists
                if (!shipmentRepository.existsById(order.getShipment().getId())) {
                    order.setShipment(null);
                    order.setStatus("CREATED");
                    orderRepository.save(order);
                    fixedCount++;
                }
            }
        }
        
        result.put("message", "Fixed shipment-order relationships");
        result.put("fixedCount", fixedCount);
        return result;
    }
    
    @GetMapping("/check-relationships")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> checkOrderShipmentRelationships() {
        Map<String, Object> result = new HashMap<>();
        int inconsistencies = 0;
        
        // Check shipments and their orders
        List<ShipmentModel> shipments = shipmentRepository.findAll();
        for (ShipmentModel shipment : shipments) {
            List<OrderModel> ordersInShipment = shipment.getOrders();
            
            for (OrderModel order : ordersInShipment) {
                if (order.getShipment() == null || !order.getShipment().getId().equals(shipment.getId())) {
                    inconsistencies++;
                }
            }
        }
        
        result.put("message", "Relationship check completed");
        result.put("inconsistencies", inconsistencies);
        result.put("totalShipments", shipments.size());
        
        return result;
    }
}