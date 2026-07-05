package com.drum_delivery_backend.repositories;

import com.drum_delivery_backend.models.SiteModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SiteRepository extends JpaRepository<SiteModel, String> {
    
    List<SiteModel> findByNameContainingIgnoreCase(String name);
    
    List<SiteModel> findByAddressContainingIgnoreCase(String address);
    
    List<SiteModel> findByCity(String city);
    
    List<SiteModel> findByCountry(String country);
}