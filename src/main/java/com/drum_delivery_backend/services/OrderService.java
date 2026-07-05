package com.drum_delivery_backend.services;

import com.drum_delivery_backend.models.ClientModel;
import com.drum_delivery_backend.models.OrderModel;
import com.drum_delivery_backend.models.ShipmentModel;
import com.drum_delivery_backend.repositories.ClientRepository;
import com.drum_delivery_backend.repositories.OrderRepository;
import com.drum_delivery_backend.repositories.ShipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ClientRepository clientRepository;
    private final ShipmentRepository shipmentRepository;

    @Autowired
    public OrderService(OrderRepository orderRepository, ClientRepository clientRepository, ShipmentRepository shipmentRepository) {
        this.orderRepository = orderRepository;
        this.clientRepository = clientRepository;
        this.shipmentRepository = shipmentRepository;
    }

    public List<OrderModel> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<OrderModel> getOrderById(String id) {
        return orderRepository.findById(id);
    }

    public Optional<OrderModel> getOrderByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber);
    }

    public List<OrderModel> getOrdersByClient(String clientId) {
        return orderRepository.findByClientId(clientId);
    }

    public List<OrderModel> getOrdersByShipment(String shipmentId) {
        return orderRepository.findByShipmentId(shipmentId);
    }

    public List<OrderModel> getOrdersByStatus(String status) {
        return orderRepository.findByStatus(status);
    }

    public List<OrderModel> getUnassignedOrders() {
        return orderRepository.findByShipmentIsNull();
    }

    @Transactional
    public OrderModel createOrder(OrderModel order) {
        if (order.getId() == null || order.getId().isEmpty()) {
            order.setId(UUID.randomUUID().toString());
        }
        
        // Debug logging
        System.out.println("DEBUG: Order client object: " + order.getClient());
        if (order.getClient() != null) {
            System.out.println("DEBUG: Client ID: " + order.getClient().getId());
        }
        
        // Validate client exists
        if (order.getClient() != null && order.getClient().getId() != null && !order.getClient().getId().isEmpty()) {
            Optional<ClientModel> client = clientRepository.findById(order.getClient().getId());
            if (client.isEmpty()) {
                throw new IllegalArgumentException("Client not found with ID: " + order.getClient().getId());
            }
            order.setClient(client.get());
        } else {
            throw new IllegalArgumentException("Client is required for creating an order");
        }
        
        // Set creation date if not provided
        if (order.getCreationDate() == null) {
            order.setCreationDate(new Date());
        }
        
        // Set status if not provided
        if (order.getStatus() == null || order.getStatus().isEmpty()) {
            order.setStatus("CREATED");
        }
        
        return orderRepository.save(order);
    }

    @Transactional
    public Optional<OrderModel> updateOrder(String id, OrderModel orderDetails) {
        return orderRepository.findById(id)
                .map(existingOrder -> {
                    if (orderDetails.getOrderNumber() != null) {
                        existingOrder.setOrderNumber(orderDetails.getOrderNumber());
                    }
                    if (orderDetails.getDescription() != null) {
                        existingOrder.setDescription(orderDetails.getDescription());
                    }
                    if (orderDetails.getQuantity() != null) {
                        existingOrder.setQuantity(orderDetails.getQuantity());
                    }
                    if (orderDetails.getUnit() != null) {
                        existingOrder.setUnit(orderDetails.getUnit());
                    }
                    if (orderDetails.getStatus() != null) {
                        existingOrder.setStatus(orderDetails.getStatus());
                    }
                    
                    // Update client if provided
                    if (orderDetails.getClient() != null && orderDetails.getClient().getId() != null) {
                        Optional<ClientModel> client = clientRepository.findById(orderDetails.getClient().getId());
                        client.ifPresent(existingOrder::setClient);
                    }
                    
                    return orderRepository.save(existingOrder);
                });
    }

    @Transactional
    public boolean deleteOrder(String id) {
        return orderRepository.findById(id)
                .map(order -> {
                    orderRepository.delete(order);
                    return true;
                }).orElse(false);
    }

    @Transactional
    public Optional<OrderModel> assignOrderToShipment(String orderId, String shipmentId) {
        Optional<OrderModel> orderOpt = orderRepository.findById(orderId);
        Optional<ShipmentModel> shipmentOpt = shipmentRepository.findById(shipmentId);
        
        if (orderOpt.isPresent() && shipmentOpt.isPresent()) {
            OrderModel order = orderOpt.get();
            ShipmentModel shipment = shipmentOpt.get();
            
            // Remove from previous shipment if exists
            if (order.getShipment() != null && !order.getShipment().getId().equals(shipmentId)) {
                ShipmentModel previousShipment = order.getShipment();
                previousShipment.removeOrder(order);
                shipmentRepository.save(previousShipment);
            }
            
            // Add to new shipment
            shipment.addOrder(order);
            shipmentRepository.save(shipment);
            
            return Optional.of(orderRepository.save(order));
        }
        
        return Optional.empty();
    }

    @Transactional
    public Optional<OrderModel> removeOrderFromShipment(String orderId) {
        Optional<OrderModel> orderOpt = orderRepository.findById(orderId);
        
        if (orderOpt.isPresent()) {
            OrderModel order = orderOpt.get();
            
            if (order.getShipment() != null) {
                ShipmentModel shipment = order.getShipment();
                shipment.removeOrder(order);
                shipmentRepository.save(shipment);
                
                order.setShipment(null);
                order.setStatus("CREATED");
                return Optional.of(orderRepository.save(order));
            }
        }
        
        return Optional.empty();
    }
}