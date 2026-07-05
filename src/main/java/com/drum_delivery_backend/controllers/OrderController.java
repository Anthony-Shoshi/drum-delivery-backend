package com.drum_delivery_backend.controllers;

import com.drum_delivery_backend.models.OrderModel;
import com.drum_delivery_backend.models.validation.ValidationGroups;
import com.drum_delivery_backend.services.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<OrderModel>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderModel> getOrderById(@PathVariable String id) {
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<OrderModel> getOrderByNumber(@PathVariable String orderNumber) {
        return orderService.getOrderByOrderNumber(orderNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<OrderModel>> getOrdersByClient(@PathVariable String clientId) {
        return ResponseEntity.ok(orderService.getOrdersByClient(clientId));
    }

    @GetMapping("/shipment/{shipmentId}")
    public ResponseEntity<List<OrderModel>> getOrdersByShipment(@PathVariable String shipmentId) {
        return ResponseEntity.ok(orderService.getOrdersByShipment(shipmentId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderModel>> getOrdersByStatus(@PathVariable String status) {
        return ResponseEntity.ok(orderService.getOrdersByStatus(status));
    }

    @GetMapping("/unassigned")
    public ResponseEntity<List<OrderModel>> getUnassignedOrders() {
        return ResponseEntity.ok(orderService.getUnassignedOrders());
    }

    @PostMapping
    public ResponseEntity<OrderModel> createOrder(@Validated(ValidationGroups.OnCreate.class) @RequestBody OrderModel order) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(order));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderModel> updateOrder(@PathVariable String id, @Validated(ValidationGroups.OnUpdate.class) @RequestBody OrderModel orderDetails) {
        return orderService.updateOrder(id, orderDetails)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable String id) {
        if (orderService.deleteOrder(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{orderId}/assign-to-shipment/{shipmentId}")
    public ResponseEntity<OrderModel> assignOrderToShipment(@PathVariable String orderId, @PathVariable String shipmentId) {
        return orderService.assignOrderToShipment(orderId, shipmentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{orderId}/remove-from-shipment")
    public ResponseEntity<OrderModel> removeOrderFromShipment(@PathVariable String orderId) {
        return orderService.removeOrderFromShipment(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}