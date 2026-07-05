package com.drum_delivery_backend.controllers;

import com.drum_delivery_backend.models.SiteModel;
import com.drum_delivery_backend.services.SiteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/sites")
@PreAuthorize("hasRole('USER') or hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
public class SiteController {

    private final SiteService siteService;

    @Autowired
    public SiteController(SiteService siteService) {
        this.siteService = siteService;
    }

    @GetMapping
    public ResponseEntity<List<SiteModel>> getAllSites() {
        return ResponseEntity.ok(siteService.getAllSites());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SiteModel> getSiteById(@PathVariable String id) {
        return siteService.getSiteById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<List<SiteModel>> searchSites(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String country) {
        
        if (name != null && !name.isEmpty()) {
            return ResponseEntity.ok(siteService.searchSitesByName(name));
        } else if (address != null && !address.isEmpty()) {
            return ResponseEntity.ok(siteService.searchSitesByAddress(address));
        } else if (city != null && !city.isEmpty()) {
            return ResponseEntity.ok(siteService.getSitesByCity(city));
        } else if (country != null && !country.isEmpty()) {
            return ResponseEntity.ok(siteService.getSitesByCountry(country));
        } else {
            return ResponseEntity.ok(siteService.getAllSites());
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<SiteModel> createSite(@Valid @RequestBody SiteModel site) {
        return ResponseEntity.status(HttpStatus.CREATED).body(siteService.createSite(site));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OPERATOR') or hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<SiteModel> updateSite(@PathVariable String id, @Valid @RequestBody SiteModel siteDetails) {
        return siteService.updateSite(id, siteDetails)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSite(@PathVariable String id) {
        if (siteService.deleteSite(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}