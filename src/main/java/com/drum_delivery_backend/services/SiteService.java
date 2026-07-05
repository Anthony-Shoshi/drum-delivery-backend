package com.drum_delivery_backend.services;

import com.drum_delivery_backend.models.SiteModel;
import com.drum_delivery_backend.repositories.SiteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SiteService {

    private final SiteRepository siteRepository;

    @Autowired
    public SiteService(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }

    public List<SiteModel> getAllSites() {
        return siteRepository.findAll();
    }

    public Optional<SiteModel> getSiteById(String id) {
        return siteRepository.findById(id);
    }

    public List<SiteModel> searchSitesByName(String name) {
        return siteRepository.findByNameContainingIgnoreCase(name);
    }

    public List<SiteModel> searchSitesByAddress(String address) {
        return siteRepository.findByAddressContainingIgnoreCase(address);
    }

    public List<SiteModel> getSitesByCity(String city) {
        return siteRepository.findByCity(city);
    }

    public List<SiteModel> getSitesByCountry(String country) {
        return siteRepository.findByCountry(country);
    }

    public SiteModel createSite(SiteModel site) {
        if (site.getId() == null || site.getId().isEmpty()) {
            site.setId(UUID.randomUUID().toString());
        }
        return siteRepository.save(site);
    }

    public Optional<SiteModel> updateSite(String id, SiteModel siteDetails) {
        return siteRepository.findById(id)
                .map(existingSite -> {
                    if (siteDetails.getName() != null) {
                        existingSite.setName(siteDetails.getName());
                    }
                    if (siteDetails.getAddress() != null) {
                        existingSite.setAddress(siteDetails.getAddress());
                    }
                    if (siteDetails.getCity() != null) {
                        existingSite.setCity(siteDetails.getCity());
                    }
                    if (siteDetails.getState() != null) {
                        existingSite.setState(siteDetails.getState());
                    }
                    if (siteDetails.getCountry() != null) {
                        existingSite.setCountry(siteDetails.getCountry());
                    }
                    if (siteDetails.getPostalCode() != null) {
                        existingSite.setPostalCode(siteDetails.getPostalCode());
                    }
                    if (siteDetails.getContactPerson() != null) {
                        existingSite.setContactPerson(siteDetails.getContactPerson());
                    }
                    if (siteDetails.getContactPhone() != null) {
                        existingSite.setContactPhone(siteDetails.getContactPhone());
                    }
                    return siteRepository.save(existingSite);
                });
    }

    public boolean deleteSite(String id) {
        return siteRepository.findById(id)
                .map(site -> {
                    siteRepository.delete(site);
                    return true;
                }).orElse(false);
    }
}