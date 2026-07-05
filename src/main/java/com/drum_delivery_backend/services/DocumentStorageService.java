package com.drum_delivery_backend.services;

import com.drum_delivery_backend.exceptions.ResourceNotFoundException;
import com.drum_delivery_backend.models.ShipmentModel;
import com.drum_delivery_backend.repositories.ShipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.util.unit.DataSize;

@Service
public class DocumentStorageService {

    private final Path storageLocation;
    private final ShipmentRepository shipmentRepository;
    private final String maxFileSize;
    private final String allowedFileTypes;
    
    // Maximum file size in bytes (default 10MB)
    private static final long DEFAULT_MAX_FILE_SIZE = 10 * 1024 * 1024;

    @Autowired
    public DocumentStorageService(
            @Value("${app.document.storage-dir}") String storageDir,
            @Value("${app.document.max-file-size:10MB}") String maxFileSize,
            @Value("${app.document.allowed-types:jpg,jpeg,png,gif,pdf,doc,docx}") String allowedFileTypes,
            ShipmentRepository shipmentRepository) {
        this.storageLocation = Paths.get(storageDir).toAbsolutePath().normalize();
        this.maxFileSize = maxFileSize;
        this.allowedFileTypes = allowedFileTypes;
        this.shipmentRepository = shipmentRepository;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.storageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Could not create document storage directory", ex);
        }
    }

    /**
     * Store a CMR document for a shipment
     */
    public String storeCmrDocument(MultipartFile file, String shipmentId) {
        ShipmentModel shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + shipmentId));

        String fileName = storeFile(file, shipment, "cmr");
        
        // Delete old file if exists
        if (shipment.getCmrDocumentPath() != null) {
            deleteFile(shipment.getCmrDocumentPath());
        }
        
        shipment.setCmrDocumentPath(fileName);
        shipmentRepository.save(shipment);
        
        return fileName;
    }

    /**
     * Store photos for a shipment
     */
    public List<String> storePhotos(List<MultipartFile> files, String shipmentId, String photoType) {
        ShipmentModel shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + shipmentId));

        List<String> fileNames = new ArrayList<>();

        for (MultipartFile file : files) {
            String fileName = storeFile(file, shipment, photoType);
            fileNames.add(fileName);
        }
        
        // Update the appropriate photo paths in the shipment model
        updateShipmentPhotoPaths(shipment, fileNames, photoType);
        shipmentRepository.save(shipment);
        
        return fileNames;
    }

    /**
     * Store a single file and return its filename
     */
    private String storeFile(MultipartFile file, ShipmentModel shipment, String fileType) {
        // Validate file before storing
        validateFile(file);

        // Sanitize filename
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new RuntimeException("Invalid filename: filename is null");
        }
        originalFilename = StringUtils.cleanPath(originalFilename);

        // Check for invalid characters
        if (originalFilename.contains("..")) {
            throw new RuntimeException("Filename contains invalid path sequence: " + originalFilename);
        }

        // Generate filename: {shipmentNumber}_{originalFilename}
        String shipmentNumber = shipment.getShipmentNumber();
        String baseFilename = shipmentNumber + "_" + originalFilename;

        // Check for conflicts and add counter if needed
        String uniqueFileName = resolveFilenameConflict(baseFilename);

        try {
            // Create the full path
            Path targetLocation = this.storageLocation.resolve(uniqueFileName);
            
            // Copy file to storage location
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
            return uniqueFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to store file " + originalFilename, ex);
        }
    }

    /**
     * Load a file as a Resource
     */
    public Resource loadFileAsResource(String fileName) {
        try {
            Path filePath = this.storageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("File not found: " + fileName);
        }
    }

    /**
     * Delete a file
     */
    public void deleteFile(String fileName) {
        try {
            Path filePath = this.storageLocation.resolve(fileName).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to delete file: " + fileName, ex);
        }
    }

    /**
     * Extract file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * Resolve filename conflicts by appending a counter if file already exists
     * Example: file.pdf -> file(1).pdf -> file(2).pdf
     */
    private String resolveFilenameConflict(String baseFilename) {
        Path filePath = this.storageLocation.resolve(baseFilename);

        // If no conflict, return original filename
        if (!Files.exists(filePath)) {
            return baseFilename;
        }

        // Extract filename without extension and extension
        String filenameWithoutExt;
        String extension = getFileExtension(baseFilename);

        if (!extension.isEmpty()) {
            filenameWithoutExt = baseFilename.substring(0, baseFilename.length() - extension.length());
        } else {
            filenameWithoutExt = baseFilename;
        }

        // Try adding counter until we find an available filename
        int counter = 1;
        String newFilename;
        do {
            newFilename = filenameWithoutExt + "(" + counter + ")" + extension;
            filePath = this.storageLocation.resolve(newFilename);
            counter++;
        } while (Files.exists(filePath) && counter < 1000); // Limit to 1000 attempts

        if (counter >= 1000) {
            throw new RuntimeException("Unable to generate unique filename after 1000 attempts");
        }

        return newFilename;
    }

    /**
     * Update the appropriate photo paths in the shipment model
     */
    private void updateShipmentPhotoPaths(ShipmentModel shipment, List<String> newFileNames, String photoType) {
        String existingPathsStr;
        List<String> existingPaths;
        
        switch (photoType) {
            case "bahrainContainer":
                existingPathsStr = shipment.getBahrainContainerPhotosPaths();
                existingPaths = stringToList(existingPathsStr);
                existingPaths.addAll(newFileNames);
                shipment.setBahrainContainerPhotosPaths(listToString(existingPaths));
                break;
                
            case "rotterdamContainer":
                existingPathsStr = shipment.getRotterdamContainerPhotosPaths();
                existingPaths = stringToList(existingPathsStr);
                existingPaths.addAll(newFileNames);
                shipment.setRotterdamContainerPhotosPaths(listToString(existingPaths));
                break;
                
            case "rotterdamTruck":
                existingPathsStr = shipment.getRotterdamTruckPhotosPaths();
                existingPaths = stringToList(existingPathsStr);
                existingPaths.addAll(newFileNames);
                shipment.setRotterdamTruckPhotosPaths(listToString(existingPaths));
                break;
                
            case "siteTruck":
                existingPathsStr = shipment.getSiteTruckPhotosPaths();
                existingPaths = stringToList(existingPathsStr);
                existingPaths.addAll(newFileNames);
                shipment.setSiteTruckPhotosPaths(listToString(existingPaths));
                break;
                
            default:
                throw new IllegalArgumentException("Invalid photo type: " + photoType);
        }
    }

    /**
     * Store documents for a shipment
     */
    public List<String> storeDocuments(List<MultipartFile> files, String shipmentId) {
        ShipmentModel shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + shipmentId));

        List<String> fileNames = new ArrayList<>();

        for (MultipartFile file : files) {
            String fileName = storeFile(file, shipment, "document");
            fileNames.add(fileName);
        }

        // Update the documents paths in the shipment model
        String existingPathsStr = shipment.getDocumentsPaths();
        List<String> existingPaths = stringToList(existingPathsStr);
        existingPaths.addAll(fileNames);
        shipment.setDocumentsPaths(listToString(existingPaths));

        shipmentRepository.save(shipment);

        return fileNames;
    }

    /**
     * Delete a document from a shipment
     * Uses targeted update query to avoid loading entire shipment entity with all relationships
     */
    @Transactional
    public void deleteShipmentDocument(String shipmentId, String fileName) {
        // Validate inputs
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }

        // Security check: prevent path traversal attacks
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new IllegalArgumentException("Invalid file name: " + fileName);
        }

        // Get only the documents_paths field (not the entire entity)
        // This is much faster as it doesn't load relationships (orders, drums, containers)
        String documentsPathsStr = shipmentRepository.findDocumentsPathsByShipmentId(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + shipmentId));

        // Get the list of documents for this shipment
        List<String> fileList = stringToList(documentsPathsStr);

        // Verify the file is actually in this shipment's document list
        if (!fileList.contains(fileName)) {
            throw new IllegalArgumentException("Document '" + fileName + "' is not associated with this shipment");
        }

        // Remove the filename from the documents list
        fileList.remove(fileName);
        String updatedDocumentsPaths = listToString(fileList);

        // Update only the documents_paths field using targeted query
        // This bypasses Hibernate's dirty checking on all relationships
        try {
            int rowsUpdated = shipmentRepository.updateDocumentsPaths(shipmentId, updatedDocumentsPaths);
            if (rowsUpdated == 0) {
                throw new RuntimeException("Failed to update shipment document list - shipment may have been deleted");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to update shipment document list in database", e);
        }

        // Delete the physical file asynchronously to avoid blocking the transaction
        // If this fails, the database is already consistent (file just becomes orphaned)
        final String fileNameToDelete = fileName; // For lambda capture
        new Thread(() -> {
            try {
                System.out.println("[DELETE DOCUMENT] Starting async file deletion for: " + fileNameToDelete);
                deleteFile(fileNameToDelete);
                System.out.println("[DELETE DOCUMENT] Async file deletion completed for: " + fileNameToDelete);
            } catch (Exception e) {
                // Log the error but don't fail the operation since DB is already updated
                System.err.println("[DELETE DOCUMENT] Warning: Failed to delete physical file '" + fileNameToDelete + "': " + e.getMessage());
                // File will be orphaned but won't cause inconsistency
            }
        }).start();
    }

    /**
     * Delete a photo from a shipment
     */
    public void deleteShipmentPhoto(String shipmentId, String fileName, String photoType) {
        ShipmentModel shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipment not found with ID: " + shipmentId));
        
        // Delete the file
        deleteFile(fileName);
        
        // Remove the filename from the appropriate list
        switch (photoType) {
            case "bahrainContainer":
                removeFileFromList(shipment, fileName, shipment.getBahrainContainerPhotosPaths(), 
                        shipment::setBahrainContainerPhotosPaths);
                break;
                
            case "rotterdamContainer":
                removeFileFromList(shipment, fileName, shipment.getRotterdamContainerPhotosPaths(), 
                        shipment::setRotterdamContainerPhotosPaths);
                break;
                
            case "rotterdamTruck":
                removeFileFromList(shipment, fileName, shipment.getRotterdamTruckPhotosPaths(), 
                        shipment::setRotterdamTruckPhotosPaths);
                break;
                
            case "siteTruck":
                removeFileFromList(shipment, fileName, shipment.getSiteTruckPhotosPaths(), 
                        shipment::setSiteTruckPhotosPaths);
                break;
                
            default:
                throw new IllegalArgumentException("Invalid photo type: " + photoType);
        }
        
        shipmentRepository.save(shipment);
    }

    /**
     * Helper method to remove a file from a comma-separated list
     */
    private void removeFileFromList(ShipmentModel shipment, String fileName, String listStr, 
                                   java.util.function.Consumer<String> setter) {
        List<String> fileList = stringToList(listStr);
        fileList.remove(fileName);
        setter.accept(listToString(fileList));
    }

    /**
     * Convert a comma-separated string to a list
     */
    private List<String> stringToList(String str) {
        if (str == null || str.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(str.split(",")).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Convert a list to a comma-separated string
     */
    private String listToString(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return String.join(",", list);
    }

    /**
     * Validate uploaded file for security and business rules
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is empty or not provided");
        }

        // Check file size
        long maxSizeBytes = parseFileSize(maxFileSize);
        if (file.getSize() > maxSizeBytes) {
            throw new RuntimeException("File size exceeds maximum allowed size of " + maxFileSize);
        }

        // Check file type by extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new RuntimeException("Invalid filename");
        }

        String fileExtension = getFileExtension(originalFilename).toLowerCase();
        if (fileExtension.isEmpty()) {
            throw new RuntimeException("File must have an extension");
        }

        // Remove leading dot from extension
        if (fileExtension.startsWith(".")) {
            fileExtension = fileExtension.substring(1);
        }

        // Check if file type is allowed
        List<String> allowedTypes = Arrays.asList(allowedFileTypes.toLowerCase().split(","));
        if (!allowedTypes.contains(fileExtension)) {
            throw new RuntimeException("File type ." + fileExtension + " is not allowed. Allowed types: " + allowedFileTypes);
        }

        // Check MIME type if available
        String contentType = file.getContentType();
        if (contentType != null && !isValidMimeType(contentType, fileExtension)) {
            throw new RuntimeException("File content type does not match file extension");
        }

        // Basic content validation to prevent malicious files
        // validateFileContent(file, fileExtension); // Temporarily disabled for debugging
    }

    /**
     * Parse file size string (e.g., "10MB", "1GB") to bytes
     */
    private long parseFileSize(String sizeStr) {
        try {
            if (sizeStr == null || sizeStr.isEmpty()) {
                return DEFAULT_MAX_FILE_SIZE;
            }
            return DataSize.parse(sizeStr).toBytes();
        } catch (Exception e) {
            return DEFAULT_MAX_FILE_SIZE;
        }
    }

    /**
     * Validate MIME type matches file extension
     */
    private boolean isValidMimeType(String mimeType, String extension) {
        extension = extension.toLowerCase();
        mimeType = mimeType.toLowerCase();

        // Define allowed MIME types for each extension
        return switch (extension) {
            case "jpg", "jpeg" -> mimeType.equals("image/jpeg");
            case "png" -> mimeType.equals("image/png");
            case "gif" -> mimeType.equals("image/gif");
            case "pdf" -> mimeType.equals("application/pdf");
            case "doc" -> mimeType.equals("application/msword");
            case "docx" -> mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            default -> true; // Allow other types if not specifically defined
        };
    }

    /**
     * Basic content validation to detect malicious files
     */
    private void validateFileContent(MultipartFile file, String extension) {
        try {
            byte[] fileBytes = file.getBytes();
            
            // Check for minimum file size (prevent empty files disguised with extensions)
            if (fileBytes.length < 10) {
                throw new RuntimeException("File appears to be corrupted or too small");
            }

            // Basic magic number validation for common file types
            if (extension.equals("pdf") && !isPdfFile(fileBytes)) {
                throw new RuntimeException("File does not appear to be a valid PDF");
            } else if ((extension.equals("jpg") || extension.equals("jpeg")) && !isJpegFile(fileBytes)) {
                throw new RuntimeException("File does not appear to be a valid JPEG image");
            } else if (extension.equals("png") && !isPngFile(fileBytes)) {
                throw new RuntimeException("File does not appear to be a valid PNG image");
            } else if (extension.equals("gif") && !isGifFile(fileBytes)) {
                throw new RuntimeException("File does not appear to be a valid GIF image");
            }

            // Check for embedded executable content in images (basic check)
            if (isImageFile(extension) && containsSuspiciousContent(fileBytes)) {
                throw new RuntimeException("File contains suspicious content");
            }

        } catch (IOException e) {
            throw new RuntimeException("Error reading file content: " + e.getMessage());
        }
    }

    /**
     * Check if file is a PDF by magic number
     */
    private boolean isPdfFile(byte[] fileBytes) {
        return fileBytes.length >= 4 && 
               fileBytes[0] == 0x25 && fileBytes[1] == 0x50 && 
               fileBytes[2] == 0x44 && fileBytes[3] == 0x46; // %PDF
    }

    /**
     * Check if file is a JPEG by magic number
     */
    private boolean isJpegFile(byte[] fileBytes) {
        return fileBytes.length >= 3 && 
               fileBytes[0] == (byte) 0xFF && fileBytes[1] == (byte) 0xD8 && 
               fileBytes[2] == (byte) 0xFF;
    }

    /**
     * Check if file is a PNG by magic number
     */
    private boolean isPngFile(byte[] fileBytes) {
        return fileBytes.length >= 8 && 
               fileBytes[0] == (byte) 0x89 && fileBytes[1] == 0x50 && 
               fileBytes[2] == 0x4E && fileBytes[3] == 0x47 &&
               fileBytes[4] == 0x0D && fileBytes[5] == 0x0A && 
               fileBytes[6] == 0x1A && fileBytes[7] == 0x0A;
    }

    /**
     * Check if file is a GIF by magic number
     */
    private boolean isGifFile(byte[] fileBytes) {
        return fileBytes.length >= 6 && 
               ((fileBytes[0] == 0x47 && fileBytes[1] == 0x49 && fileBytes[2] == 0x46 && 
                 fileBytes[3] == 0x38 && fileBytes[4] == 0x37 && fileBytes[5] == 0x61) || // GIF87a
                (fileBytes[0] == 0x47 && fileBytes[1] == 0x49 && fileBytes[2] == 0x46 && 
                 fileBytes[3] == 0x38 && fileBytes[4] == 0x39 && fileBytes[5] == 0x61)); // GIF89a
    }

    /**
     * Check if extension is for an image file
     */
    private boolean isImageFile(String extension) {
        return Arrays.asList("jpg", "jpeg", "png", "gif").contains(extension.toLowerCase());
    }

    /**
     * Basic check for suspicious content in files
     */
    private boolean containsSuspiciousContent(byte[] fileBytes) {
        String content = new String(fileBytes, 0, Math.min(fileBytes.length, 1024)).toLowerCase();
        
        // Check for common script tags or executable signatures
        String[] suspiciousPatterns = {
            "<script", "javascript:", "vbscript:", "data:text/html",
            "<%", "<?php", "#!/bin/", "mz" // MZ header for executables
        };
        
        for (String pattern : suspiciousPatterns) {
            if (content.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }
}