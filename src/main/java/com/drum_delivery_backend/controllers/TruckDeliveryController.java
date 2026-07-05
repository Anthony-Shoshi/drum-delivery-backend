package com.drum_delivery_backend.controllers;

import com.drum_delivery_backend.models.DrumModel;
import com.drum_delivery_backend.models.TruckDeliveryModel;
import com.drum_delivery_backend.services.TruckDeliveryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/truck-deliveries")
public class TruckDeliveryController {

    private final TruckDeliveryService truckDeliveryService;

    @Autowired
    public TruckDeliveryController(TruckDeliveryService truckDeliveryService) {
        this.truckDeliveryService = truckDeliveryService;
    }

    @GetMapping
    public ResponseEntity<List<TruckDeliveryModel>> getAllTruckDeliveries() {
        return ResponseEntity.ok(truckDeliveryService.getAllTruckDeliveries());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TruckDeliveryModel> getTruckDeliveryById(@PathVariable String id) {
        return truckDeliveryService.getTruckDeliveryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/shipment/{shipmentId}")
    public ResponseEntity<List<TruckDeliveryModel>> getTruckDeliveriesByShipment(@PathVariable String shipmentId) {
        return ResponseEntity.ok(truckDeliveryService.getTruckDeliveriesByShipment(shipmentId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<TruckDeliveryModel>> getTruckDeliveriesByStatus(@PathVariable String status) {
        return ResponseEntity.ok(truckDeliveryService.getTruckDeliveriesByStatus(status));
    }

    @GetMapping("/truck/{truckNumber}")
    public ResponseEntity<List<TruckDeliveryModel>> getTruckDeliveriesByTruckNumber(@PathVariable String truckNumber) {
        return ResponseEntity.ok(truckDeliveryService.getTruckDeliveriesByTruckNumber(truckNumber));
    }

    @GetMapping("/scheduled")
    public ResponseEntity<List<TruckDeliveryModel>> getTruckDeliveriesByScheduledDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate) {
        return ResponseEntity.ok(truckDeliveryService.getTruckDeliveriesByScheduledDate(startDate, endDate));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<TruckDeliveryModel>> getTruckDeliveriesByOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(truckDeliveryService.getTruckDeliveriesByOrder(orderId));
    }

    @PostMapping
    public ResponseEntity<TruckDeliveryModel> createTruckDelivery(@Valid @RequestBody TruckDeliveryModel truckDelivery) {
        return ResponseEntity.status(HttpStatus.CREATED).body(truckDeliveryService.createTruckDelivery(truckDelivery));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TruckDeliveryModel> updateTruckDelivery(@PathVariable String id, @Valid @RequestBody TruckDeliveryModel truckDeliveryDetails) {
        return truckDeliveryService.updateTruckDelivery(id, truckDeliveryDetails)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTruckDelivery(@PathVariable String id) {
        if (truckDeliveryService.deleteTruckDelivery(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{truckDeliveryId}/add-order/{orderId}")
    public ResponseEntity<TruckDeliveryModel> addOrderToTruckDelivery(@PathVariable String truckDeliveryId, @PathVariable String orderId) {
        return truckDeliveryService.addOrderToTruckDelivery(truckDeliveryId, orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{truckDeliveryId}/remove-order/{orderId}")
    public ResponseEntity<TruckDeliveryModel> removeOrderFromTruckDelivery(@PathVariable String truckDeliveryId, @PathVariable String orderId) {
        return truckDeliveryService.removeOrderFromTruckDelivery(truckDeliveryId, orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{truckDeliveryId}/add-drum/{drumNumber}")
    public ResponseEntity<TruckDeliveryModel> addDrumToTruckDelivery(@PathVariable String truckDeliveryId, @PathVariable String drumNumber) {
        return truckDeliveryService.addDrumToTruckDelivery(truckDeliveryId, drumNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{truckDeliveryId}/remove-drum/{drumNumber}")
    public ResponseEntity<TruckDeliveryModel> removeDrumFromTruckDelivery(@PathVariable String truckDeliveryId, @PathVariable String drumNumber) {
        return truckDeliveryService.removeDrumFromTruckDelivery(truckDeliveryId, drumNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{truckDeliveryId}/assign-drums")
    public ResponseEntity<TruckDeliveryModel> assignMultipleDrumsToTruckDelivery(
            @PathVariable String truckDeliveryId, 
            @RequestBody List<String> drumNumbers) {
        return truckDeliveryService.assignMultipleDrumsToTruckDelivery(truckDeliveryId, drumNumbers)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{truckDeliveryId}/drums")
    public ResponseEntity<List<String>> getDrumsForTruckDelivery(@PathVariable String truckDeliveryId) {
        return truckDeliveryService.getTruckDeliveryById(truckDeliveryId)
                .map(truckDelivery -> ResponseEntity.ok(truckDelivery.getDrumNumbers()))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{truckDeliveryId}/drums/details")
    public ResponseEntity<List<DrumModel>> getDrumDetailsForTruckDelivery(@PathVariable String truckDeliveryId) {
        return truckDeliveryService.getTruckDeliveryById(truckDeliveryId)
                .map(truckDelivery -> ResponseEntity.ok(truckDelivery.getDrums()))
                .orElse(ResponseEntity.notFound().build());
    }
}