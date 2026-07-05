package com.drum_delivery_backend.controllers;

import com.drum_delivery_backend.models.ContainerModel;
import com.drum_delivery_backend.services.ContainerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/containers")
public class ContainerController {

    @Autowired
    private ContainerService containerService;

    // Basic CRUD Operations

    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<ContainerModel>> getAllContainers() {
        List<ContainerModel> containers = containerService.getAllContainers();
        return ResponseEntity.ok(containers);
    }

    @GetMapping("/{containerNumber}")
    @PreAuthorize("hasRole('USER') or hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ContainerModel> getContainer(@PathVariable String containerNumber) {
        return containerService.getContainer(containerNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ContainerModel> createContainer(@Valid @RequestBody Map<String, String> request) {
        try {
            String containerNumber = request.get("containerNumber");
            if (containerNumber == null || containerNumber.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            ContainerModel container = containerService.createContainer(containerNumber);
            return ResponseEntity.status(HttpStatus.CREATED).body(container);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/create-or-get")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ContainerModel> createOrGetContainer(@RequestBody Map<String, String> request) {
        try {
            String containerNumber = request.get("containerNumber");
            if (containerNumber == null || containerNumber.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            ContainerModel container = containerService.createOrGetContainer(containerNumber);
            return ResponseEntity.ok(container);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{containerNumber}")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ContainerModel> updateContainer(
            @PathVariable String containerNumber,
            @Valid @RequestBody ContainerModel containerDetails) {
        try {
            ContainerModel updatedContainer = containerService.updateContainer(containerNumber, containerDetails);
            return ResponseEntity.ok(updatedContainer);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{containerNumber}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteContainer(@PathVariable String containerNumber) {
        try {
            containerService.deleteContainer(containerNumber);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Shipment-related Operations

    @GetMapping("/by-shipment/{shipmentId}")
    @PreAuthorize("hasRole('USER') or hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<ContainerModel>> getContainersByShipment(@PathVariable String shipmentId) {
        List<ContainerModel> containers = containerService.getContainersByShipmentId(shipmentId);
        return ResponseEntity.ok(containers);
    }

    @GetMapping("/unassigned")
    @PreAuthorize("hasRole('USER') or hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<ContainerModel>> getUnassignedContainers() {
        List<ContainerModel> containers = containerService.getUnassignedContainers();
        return ResponseEntity.ok(containers);
    }

    @PostMapping("/{containerNumber}/assign-shipment")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ContainerModel> assignContainerToShipment(
            @PathVariable String containerNumber,
            @RequestBody Map<String, String> request) {
        try {
            String shipmentId = request.get("shipmentId");
            if (shipmentId == null || shipmentId.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            ContainerModel container = containerService.assignContainerToShipment(containerNumber, shipmentId);
            return ResponseEntity.ok(container);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{containerNumber}/remove-shipment")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ContainerModel> removeContainerFromShipment(@PathVariable String containerNumber) {
        try {
            ContainerModel container = containerService.removeContainerFromShipment(containerNumber);
            return ResponseEntity.ok(container);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Container Management Operations

    @GetMapping("/with-drums")
    @PreAuthorize("hasRole('USER') or hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<ContainerModel>> getContainersWithDrums() {
        List<ContainerModel> containers = containerService.getContainersWithDrums();
        return ResponseEntity.ok(containers);
    }

    @GetMapping("/empty")
    @PreAuthorize("hasRole('USER') or hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<ContainerModel>> getEmptyContainers() {
        List<ContainerModel> containers = containerService.getEmptyContainers();
        return ResponseEntity.ok(containers);
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('USER') or hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ContainerService.ContainerStatistics> getContainerStatistics() {
        ContainerService.ContainerStatistics statistics = containerService.getContainerStatistics();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('USER') or hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<List<ContainerModel>> searchContainers(@RequestParam String q) {
        if (q == null || q.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        List<ContainerModel> containers = containerService.searchContainers(q.trim());
        return ResponseEntity.ok(containers);
    }

    // Drum-related Operations

    @PostMapping("/{containerNumber}/drums/{drumNumber}")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ContainerModel> addDrumToContainer(
            @PathVariable String containerNumber,
            @PathVariable String drumNumber) {
        try {
            // Note: This would require integration with DrumService to get the drum
            // For now, return method not implemented
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{containerNumber}/drums/{drumNumber}")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ContainerModel> removeDrumFromContainer(
            @PathVariable String containerNumber,
            @PathVariable String drumNumber) {
        try {
            // Note: This would require integration with DrumService to get the drum
            // For now, return method not implemented
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}