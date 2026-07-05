package com.drum_delivery_backend.repositories;

import com.drum_delivery_backend.models.ClientModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<ClientModel, String> {
    
    Optional<ClientModel> findByEmail(String email);
    
    List<ClientModel> findByNameContainingIgnoreCase(String name);
    
    boolean existsByEmail(String email);
}