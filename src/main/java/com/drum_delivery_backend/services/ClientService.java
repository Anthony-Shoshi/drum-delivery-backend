package com.drum_delivery_backend.services;

import com.drum_delivery_backend.models.ClientModel;
import com.drum_delivery_backend.repositories.ClientRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public List<ClientModel> getAllClients() {
        return clientRepository.findAll();
    }

    public Optional<ClientModel> getClientById(String id) {
        return clientRepository.findById(id);
    }

    public Optional<ClientModel> getClientByEmail(String email) {
        return clientRepository.findByEmail(email);
    }

    public List<ClientModel> searchClientsByName(String name) {
        return clientRepository.findByNameContainingIgnoreCase(name);
    }

    public ClientModel createClient(ClientModel client) {
        if (client.getId() == null || client.getId().isEmpty()) {
            client.setId(UUID.randomUUID().toString());
        }
        return clientRepository.save(client);
    }

    public Optional<ClientModel> updateClient(String id, ClientModel clientDetails) {
        return clientRepository.findById(id)
                .map(existingClient -> {
                    if (clientDetails.getName() != null) {
                        existingClient.setName(clientDetails.getName());
                    }
                    if (clientDetails.getEmail() != null) {
                        existingClient.setEmail(clientDetails.getEmail());
                    }
                    if (clientDetails.getContactPerson() != null) {
                        existingClient.setContactPerson(clientDetails.getContactPerson());
                    }
                    if (clientDetails.getPhone() != null) {
                        existingClient.setPhone(clientDetails.getPhone());
                    }
                    if (clientDetails.getAddress() != null) {
                        existingClient.setAddress(clientDetails.getAddress());
                    }
                    if (clientDetails.getCity() != null) {
                        existingClient.setCity(clientDetails.getCity());
                    }
                    if (clientDetails.getState() != null) {
                        existingClient.setState(clientDetails.getState());
                    }
                    if (clientDetails.getCountry() != null) {
                        existingClient.setCountry(clientDetails.getCountry());
                    }
                    if (clientDetails.getPostalCode() != null) {
                        existingClient.setPostalCode(clientDetails.getPostalCode());
                    }
                    return clientRepository.save(existingClient);
                });
    }

    public boolean deleteClient(String id) {
        return clientRepository.findById(id)
                .map(client -> {
                    clientRepository.delete(client);
                    return true;
                }).orElse(false);
    }
}