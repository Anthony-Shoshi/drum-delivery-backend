package com.drum_delivery_backend.services;

import com.drum_delivery_backend.models.DrumModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Result class for bulk drum creation operations
 */
@Getter
@Setter
@AllArgsConstructor
public class BulkDrumCreationResult {
    private List<DrumModel> createdDrums;
    private List<String> skippedDrums;
    private List<String> validationErrors;

    public int getTotalProcessed() {
        return createdDrums.size() + skippedDrums.size() + validationErrors.size();
    }

    public int getCreatedCount() {
        return createdDrums.size();
    }

    public int getSkippedCount() {
        return skippedDrums.size();
    }

    public int getErrorCount() {
        return validationErrors.size();
    }

    public boolean hasErrors() {
        return !validationErrors.isEmpty();
    }

    public boolean hasSkipped() {
        return !skippedDrums.isEmpty();
    }

    public boolean isCompleteSuccess() {
        return !hasErrors() && !hasSkipped() && getCreatedCount() > 0;
    }
}