package com.drum_delivery_backend.controllers;

import com.drum_delivery_backend.models.ShipmentModel;
import com.drum_delivery_backend.models.validation.ValidationGroups;
import com.drum_delivery_backend.services.DocumentStorageService;
import com.drum_delivery_backend.services.ShipmentService;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/shipments")
public class ShipmentController {

    private final ShipmentService shipmentService;
    private final DocumentStorageService documentStorageService;

    public ShipmentController(ShipmentService shipmentService, DocumentStorageService documentStorageService) {
        this.shipmentService = shipmentService;
        this.documentStorageService = documentStorageService;
    }

    @GetMapping
    public ResponseEntity<List<ShipmentModel>> getAllShipments() {
        return ResponseEntity.ok(shipmentService.getAllShipments());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShipmentModel> getShipmentById(@PathVariable String id) {
        return shipmentService.getShipmentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{shipmentNumber}")
    public ResponseEntity<ShipmentModel> getShipmentByNumber(@PathVariable String shipmentNumber) {
        return shipmentService.getShipmentByShipmentNumber(shipmentNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ShipmentModel>> getShipmentsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(shipmentService.getShipmentsByStatus(status));
    }

    @GetMapping("/destination/{siteId}")
    public ResponseEntity<List<ShipmentModel>> getShipmentsByDestination(@PathVariable String siteId) {
        return ResponseEntity.ok(shipmentService.getShipmentsByDestinationSite(siteId));
    }

    @PostMapping
    public ResponseEntity<ShipmentModel> createShipment(@Validated(ValidationGroups.OnCreate.class) @RequestBody ShipmentModel shipment) {
        System.out.println("DEBUG: Received shipment creation request");
        System.out.println("DEBUG: Shipment number: '" + shipment.getShipmentNumber() + "'");
        System.out.println("DEBUG: Destination site: " + (shipment.getDestinationSite() != null ? shipment.getDestinationSite().getId() : "null"));
        System.out.println("DEBUG: Invoice no: '" + shipment.getInvoiceNo() + "'");
        System.out.println("DEBUG: BL no: '" + shipment.getBlNo() + "'");
        System.out.println("DEBUG: Status: '" + shipment.getStatus() + "'");
        
        try {
            ShipmentModel createdShipment = shipmentService.createShipment(shipment);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdShipment);
        } catch (Exception e) {
            System.out.println("DEBUG: Error creating shipment: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShipmentModel> updateShipment(@PathVariable String id, @Validated(ValidationGroups.OnUpdate.class) @RequestBody ShipmentModel shipmentDetails) {
        return shipmentService.updateShipment(id, shipmentDetails)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShipment(@PathVariable String id) {
        if (shipmentService.deleteShipment(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{shipmentId}/add-order/{orderId}")
    public ResponseEntity<ShipmentModel> addOrderToShipment(@PathVariable String shipmentId, @PathVariable String orderId) {
        return shipmentService.addOrderToShipment(shipmentId, orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{shipmentId}/remove-order/{orderId}")
    public ResponseEntity<ShipmentModel> removeOrderFromShipment(@PathVariable String shipmentId, @PathVariable String orderId) {
        return shipmentService.removeOrderFromShipment(shipmentId, orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Upload CMR document for a shipment
     */
    @PostMapping("/{shipmentId}/upload-cmr")
    public ResponseEntity<Map<String, String>> uploadCmrDocument(
            @PathVariable String shipmentId,
            @RequestParam("file") MultipartFile file) {
            
        String fileName = documentStorageService.storeCmrDocument(file, shipmentId);
        return ResponseEntity.ok(Map.of("fileName", fileName));
    }
    
    /**
     * Upload photos for a shipment
     * Only accessible to users with OPERATOR, MANAGER, or ADMIN roles
     */
    @PreAuthorize("hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    @PostMapping("/{shipmentId}/upload-photos/{photoType}")
    public ResponseEntity<Map<String, List<String>>> uploadPhotos(
            @PathVariable String shipmentId,
            @PathVariable String photoType,
            @RequestParam("files") List<MultipartFile> files) {
            
        List<String> fileNames = documentStorageService.storePhotos(files, shipmentId, photoType);
        return ResponseEntity.ok(Map.of("fileNames", fileNames));
    }
    
    /**
     * Delete a photo from a shipment
     * Only accessible to users with OPERATOR, MANAGER, or ADMIN roles
     */
    @PreAuthorize("hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    @DeleteMapping("/{shipmentId}/photos/{photoType}/{fileName}")
    public ResponseEntity<Void> deletePhoto(
            @PathVariable String shipmentId,
            @PathVariable String photoType,
            @PathVariable String fileName) {

        documentStorageService.deleteShipmentPhoto(shipmentId, fileName, photoType);
        return ResponseEntity.noContent().build();
    }

    /**
     * Upload documents for a shipment
     * Only accessible to users with OPERATOR, MANAGER, or ADMIN roles
     */
    @PreAuthorize("hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    @PostMapping("/{shipmentId}/upload-documents")
    public ResponseEntity<Map<String, List<String>>> uploadDocuments(
            @PathVariable String shipmentId,
            @RequestParam("files") List<MultipartFile> files) {

        List<String> fileNames = documentStorageService.storeDocuments(files, shipmentId);
        return ResponseEntity.ok(Map.of("fileNames", fileNames));
    }

    /**
     * Delete a document from a shipment
     * Only accessible to users with OPERATOR, MANAGER, or ADMIN roles
     */
    @PreAuthorize("hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    @DeleteMapping("/{shipmentId}/documents/{fileName:.+}")
    public ResponseEntity<?> deleteDocument(
            @PathVariable String shipmentId,
            @PathVariable String fileName) {

        try {
            documentStorageService.deleteShipmentDocument(shipmentId, fileName);
            return ResponseEntity.noContent().build();
        } catch (com.drum_delivery_backend.exceptions.ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Shipment not found", "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid request", "message", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error deleting document: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Server error", "message", "Failed to delete document. Please try again."));
        }
    }
    
    /**
     * Get a document or photo
     */
    @GetMapping("/documents/{fileName:.+}")
    public ResponseEntity<Resource> getDocument(
            @PathVariable String fileName,
            HttpServletRequest request) {
            
        Resource resource = documentStorageService.loadFileAsResource(fileName);
        
        // Try to determine file's content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            // logger.info("Could not determine file type.");
        }
        
        // Fallback to the default content type if type could not be determined
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
    
    /**
     * Update shipment status
     */
    @PutMapping("/{shipmentId}/status")
    public ResponseEntity<ShipmentModel> updateStatus(
            @PathVariable String shipmentId,
            @RequestBody Map<String, String> statusUpdate) {
            
        String newStatus = statusUpdate.get("status");
        if (newStatus == null || newStatus.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        ShipmentModel shipmentDetails = new ShipmentModel();
        shipmentDetails.setStatus(newStatus);
        
        return shipmentService.updateShipment(shipmentId, shipmentDetails)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}