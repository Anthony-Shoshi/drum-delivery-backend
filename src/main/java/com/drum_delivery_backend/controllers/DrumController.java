package com.drum_delivery_backend.controllers;

import com.drum_delivery_backend.models.DrumModel;
import com.drum_delivery_backend.models.DrumStatus;
import com.drum_delivery_backend.services.DrumService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/drums")
public class DrumController {

    @Autowired
    private DrumService drumService;

    // Basic CRUD Operations

    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Page<DrumModel>> getAllDrums(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "drumNumber") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) DrumStatus status,
            @RequestParam(required = false) BigDecimal minWeight,
            @RequestParam(required = false) BigDecimal maxWeight,
            @RequestParam(required = false) Boolean unassigned,
            @RequestParam(required = false) Boolean assigned) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<DrumModel> drums = drumService.getAllDrumsFiltered(pageable, status, minWeight, maxWeight, unassigned, assigned);
        return ResponseEntity.ok(drums);
    }

    @GetMapping("/{drumNumber}")
    @PreAuthorize("hasRole('USER') or hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<DrumModel> getDrumByNumber(@PathVariable String drumNumber) {
        Optional<DrumModel> drum = drumService.getDrumByNumber(drumNumber);
        return drum.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<DrumModel> createDrum(@Valid @RequestBody DrumModel drum) {
        try {
            DrumModel createdDrum = drumService.createDrum(drum);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdDrum);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{drumNumber}")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<DrumModel> updateDrum(
            @PathVariable String drumNumber,
            @Valid @RequestBody DrumModel drumDetails) {
        try {
            DrumModel updatedDrum = drumService.updateDrum(drumNumber, drumDetails);
            return ResponseEntity.ok(updatedDrum);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{drumNumber}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDrum(@PathVariable String drumNumber) {
        try {
            drumService.deleteDrum(drumNumber);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Bulk Operations

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<DrumModel>> createMultipleDrums(@Valid @RequestBody List<DrumModel> drums) {
        try {
            List<DrumModel> createdDrums = drumService.createMultipleDrums(drums);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdDrums);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/bulk-with-skip")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createMultipleDrumsWithSkipDuplicates(@Valid @RequestBody List<DrumModel> drums) {
        try {
            var result = drumService.createMultipleDrumsWithSkipDuplicates(drums);
            
            Map<String, Object> response = Map.of(
                "totalProcessed", result.getTotalProcessed(),
                "createdCount", result.getCreatedCount(),
                "skippedCount", result.getSkippedCount(),
                "errorCount", result.getErrorCount(),
                "createdDrums", result.getCreatedDrums(),
                "skippedDrums", result.getSkippedDrums(),
                "validationErrors", result.getValidationErrors(),
                "success", result.getCreatedCount() > 0 || result.getSkippedCount() > 0
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Order Assignment Operations

    @PostMapping("/{drumNumber}/assign-to-order/{orderId}")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<DrumModel> assignDrumToOrder(
            @PathVariable String drumNumber,
            @PathVariable String orderId) {
        try {
            DrumModel updatedDrum = drumService.assignDrumToOrder(drumNumber, orderId);
            return ResponseEntity.ok(updatedDrum);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/assign-to-order/{orderId}")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<DrumModel>> assignMultipleDrumsToOrder(
            @PathVariable String orderId,
            @RequestBody List<String> drumNumbers) {
        try {
            List<DrumModel> updatedDrums = drumService.assignMultipleDrumsToOrder(drumNumbers, orderId);
            return ResponseEntity.ok(updatedDrums);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{drumNumber}/remove-from-order")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<DrumModel> removeDrumFromOrder(@PathVariable String drumNumber) {
        try {
            DrumModel updatedDrum = drumService.removeDrumFromOrder(drumNumber);
            return ResponseEntity.ok(updatedDrum);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Shipment Assignment Operations

    @PostMapping("/{drumNumber}/assign-to-shipment/{shipmentId}")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<DrumModel> assignDrumToShipment(
            @PathVariable String drumNumber,
            @PathVariable String shipmentId) {
        try {
            DrumModel updatedDrum = drumService.assignDrumToShipment(drumNumber, shipmentId);
            return ResponseEntity.ok(updatedDrum);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{drumNumber}/remove-from-shipment")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<DrumModel> removeDrumFromShipment(@PathVariable String drumNumber) {
        try {
            DrumModel updatedDrum = drumService.removeDrumFromShipment(drumNumber);
            return ResponseEntity.ok(updatedDrum);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Status Management

    @PostMapping("/{drumNumber}/mark-delivered")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<DrumModel> markDrumAsDelivered(@PathVariable String drumNumber) {
        try {
            DrumModel updatedDrum = drumService.markDrumAsDelivered(drumNumber);
            return ResponseEntity.ok(updatedDrum);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{drumNumber}/mark-missing")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<DrumModel> markDrumAsMissing(
            @PathVariable String drumNumber,
            @RequestBody Map<String, String> request) {
        try {
            String notes = request.get("notes");
            DrumModel updatedDrum = drumService.markDrumAsMissing(drumNumber, notes);
            return ResponseEntity.ok(updatedDrum);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{drumNumber}/mark-damaged")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<DrumModel> markDrumAsDamaged(
            @PathVariable String drumNumber,
            @RequestBody Map<String, String> request) {
        try {
            String notes = request.get("notes");
            DrumModel updatedDrum = drumService.markDrumAsDamaged(drumNumber, notes);
            return ResponseEntity.ok(updatedDrum);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/mark-delivered")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<DrumModel>> markMultipleDrumsAsDelivered(@RequestBody List<String> drumNumbers) {
        try {
            List<DrumModel> updatedDrums = drumService.markMultipleDrumsAsDelivered(drumNumbers);
            return ResponseEntity.ok(updatedDrums);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Query Operations

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('USER') or hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<DrumModel>> getDrumsByOrder(@PathVariable String orderId) {
        List<DrumModel> drums = drumService.getDrumsByOrder(orderId);
        return ResponseEntity.ok(drums);
    }

    @GetMapping("/shipment/{shipmentId}")
    @PreAuthorize("hasRole('USER') or hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<DrumModel>> getDrumsByShipment(@PathVariable String shipmentId) {
        List<DrumModel> drums = drumService.getDrumsByShipment(shipmentId);
        return ResponseEntity.ok(drums);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasRole('USER') or hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<DrumModel>> getDrumsByStatus(@PathVariable DrumStatus status) {
        List<DrumModel> drums = drumService.getDrumsByStatus(status);
        return ResponseEntity.ok(drums);
    }

    @GetMapping("/available")
    @PreAuthorize("hasRole('USER') or hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<DrumModel>> getAvailableDrums() {
        List<DrumModel> drums = drumService.getAvailableDrums();
        return ResponseEntity.ok(drums);
    }

    @GetMapping("/missing")
    @PreAuthorize("hasRole('USER') or hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<DrumModel>> getMissingDrums() {
        List<DrumModel> drums = drumService.getMissingDrums();
        return ResponseEntity.ok(drums);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('USER') or hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<DrumModel>> searchDrums(@RequestParam String q) {
        List<DrumModel> drums = drumService.searchDrumsByNumber(q);
        return ResponseEntity.ok(drums);
    }

    // Summary Operations

    @GetMapping("/order/{orderId}/summary")
    @PreAuthorize("hasRole('USER') or hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getOrderDrumSummary(@PathVariable String orderId) {
        Integer totalDrums = drumService.getTotalDrumCountByOrder(orderId);
        BigDecimal totalNetWeight = drumService.getTotalNetWeightByOrder(orderId);
        BigDecimal totalGrossWeight = drumService.getTotalGrossWeightByOrder(orderId);
        BigDecimal totalLength = drumService.getTotalLengthByOrder(orderId);

        Map<String, Object> summary = Map.of(
            "totalDrums", totalDrums != null ? totalDrums : 0,
            "totalNetWeightMt", totalNetWeight != null ? totalNetWeight : BigDecimal.ZERO,
            "totalGrossWeightMt", totalGrossWeight != null ? totalGrossWeight : BigDecimal.ZERO,
            "totalLengthKms", totalLength != null ? totalLength : BigDecimal.ZERO
        );

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/shipment/{shipmentId}/summary")
    @PreAuthorize("hasRole('USER') or hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getShipmentDrumSummary(@PathVariable String shipmentId) {
        Integer totalDrums = drumService.getTotalDrumCountByShipment(shipmentId);
        BigDecimal totalNetWeight = drumService.getTotalNetWeightByShipment(shipmentId);
        BigDecimal totalGrossWeight = drumService.getTotalGrossWeightByShipment(shipmentId);
        BigDecimal totalLength = drumService.getTotalLengthByShipment(shipmentId);

        Map<String, Object> summary = Map.of(
            "totalDrums", totalDrums != null ? totalDrums : 0,
            "totalNetWeightMt", totalNetWeight != null ? totalNetWeight : BigDecimal.ZERO,
            "totalGrossWeightMt", totalGrossWeight != null ? totalGrossWeight : BigDecimal.ZERO,
            "totalLengthKms", totalLength != null ? totalLength : BigDecimal.ZERO
        );

        return ResponseEntity.ok(summary);
    }

    // Overall Summary Operations

    @GetMapping("/summary")
    @PreAuthorize("hasRole('USER') or hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDrumsSummary() {
        Map<String, Object> summary = drumService.getDrumsSummary();
        return ResponseEntity.ok(summary);
    }

    // Data Consistency Operations

    @GetMapping("/consistency-check")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> checkDataConsistency() {
        Map<String, Object> consistencyReport = drumService.checkDataConsistency();
        return ResponseEntity.ok(consistencyReport);
    }

    @PostMapping("/repair-consistency")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> repairDataConsistency() {
        try {
            Map<String, Object> repairResult = drumService.repairDataConsistency();
            return ResponseEntity.ok(repairResult);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to repair data consistency: " + e.getMessage()));
        }
    }
}