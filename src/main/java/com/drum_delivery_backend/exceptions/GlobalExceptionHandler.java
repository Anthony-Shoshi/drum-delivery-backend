package com.drum_delivery_backend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @Value("${DETAILED_ERROR_RESPONSES:false}")
    private Boolean detailedErrorResponses;

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> resourceNotFoundException(ResourceNotFoundException ex) {
        ErrorResponse errorDetails = new ErrorResponse(new Date(), HttpStatus.NOT_FOUND.value(),
                "Resource Not Found", ex.getMessage());
        return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> illegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse errorDetails = new ErrorResponse(new Date(), HttpStatus.BAD_REQUEST.value(),
                "Invalid Request", ex.getMessage());
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        ErrorResponse errorDetails = new ErrorResponse(new Date(), HttpStatus.BAD_REQUEST.value(),
                "Validation Error", "Validation failed for one or more fields", errors);
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleJsonParsingException(HttpMessageNotReadableException ex) {
        System.out.println("DEBUG: JSON parsing error: " + ex.getMessage());
        ex.printStackTrace();
        
        String message = "Invalid JSON format in request body";
        if (detailedErrorResponses && ex.getMessage() != null) {
            message = ex.getMessage();
        }
        
        ErrorResponse errorDetails = new ErrorResponse(new Date(), HttpStatus.BAD_REQUEST.value(),
                "JSON Parse Error", message);
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String message = "Data integrity violation occurred";
        
        // Extract meaningful error message from the constraint violation
        String rootCause = null;
        Throwable cause = ex.getRootCause();
        if (cause != null && cause.getMessage() != null) {
            rootCause = cause.getMessage();
        } else if (ex.getMessage() != null) {
            rootCause = ex.getMessage();
        }
        
        if (rootCause != null) {
            if (rootCause.contains("shipment_number") || rootCause.contains("UK8gftp2bg6vt9mn5pspd1iwo4")) {
                message = "Shipment number already exists. Please choose a unique shipment number.";
            } else if (rootCause.contains("container_no")) {
                message = "Container number already exists. Please choose a unique container number.";
            } else if (rootCause.contains("invoice_no")) {
                message = "Invoice number already exists. Please choose a unique invoice number.";
            } else if (rootCause.contains("bl_no")) {
                message = "B/L number already exists. Please choose a unique B/L number.";
            } else if (rootCause.contains("destination_site") || rootCause.contains("FKqpwmio5qid57giqspxc3stsr0")) {
                message = "Invalid destination site. Please select a valid destination site.";
            } else if (detailedErrorResponses) {
                message = "Database constraint violation: " + rootCause;
            }
        }
        
        ErrorResponse errorDetails = new ErrorResponse(new Date(), HttpStatus.BAD_REQUEST.value(),
                "Data Integrity Error", message);
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<?> handleDataAccessException(DataAccessException ex) {
        String message = detailedErrorResponses ? ex.getMessage() : "Database operation failed";
        ErrorResponse errorDetails = new ErrorResponse(new Date(), HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Database Error", message);
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> globalExceptionHandler(Exception ex) {
        String message = detailedErrorResponses ? ex.getMessage() : "An internal server error occurred";
        ErrorResponse errorDetails = new ErrorResponse(new Date(), HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Server Error", message);
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<?> securityExceptionHandler(SecurityException ex) {
        String message = detailedErrorResponses ? ex.getMessage() : "Access denied";
        ErrorResponse errorDetails = new ErrorResponse(new Date(), HttpStatus.FORBIDDEN.value(),
                "Security Error", message);
        return new ResponseEntity<>(errorDetails, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> runtimeExceptionHandler(RuntimeException ex) {
        // For file upload and other runtime exceptions, provide more specific but safe messages
        String message = ex.getMessage();
        if (!detailedErrorResponses) {
            // Sanitize error messages for production
            if (message != null && message.contains("storage")) {
                message = "File processing error";
            } else if (message != null && message.contains("database")) {
                message = "Data processing error";
            } else {
                message = "Operation failed";
            }
        }
        
        ErrorResponse errorDetails = new ErrorResponse(new Date(), HttpStatus.BAD_REQUEST.value(),
                "Operation Error", message);
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST);
    }

    public static class ErrorResponse {
        private Date timestamp;
        private int status;
        private String error;
        private String message;
        private Map<String, String> details;

        public ErrorResponse(Date timestamp, int status, String error, String message) {
            this.timestamp = timestamp;
            this.status = status;
            this.error = error;
            this.message = message;
        }

        public ErrorResponse(Date timestamp, int status, String error, String message, Map<String, String> details) {
            this.timestamp = timestamp;
            this.status = status;
            this.error = error;
            this.message = message;
            this.details = details;
        }

        public Date getTimestamp() {
            return timestamp;
        }

        public int getStatus() {
            return status;
        }

        public String getError() {
            return error;
        }

        public String getMessage() {
            return message;
        }

        public Map<String, String> getDetails() {
            return details;
        }
    }
}