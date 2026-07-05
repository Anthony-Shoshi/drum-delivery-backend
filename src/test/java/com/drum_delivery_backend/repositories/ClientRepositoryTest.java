package com.drum_delivery_backend.repositories;

import com.drum_delivery_backend.models.ClientModel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
public class ClientRepositoryTest {

    @Autowired
    private ClientRepository clientRepository;

    @Test
    public void testSaveClient() {
        // Create test client
        ClientModel client = new ClientModel();
        client.setId(UUID.randomUUID().toString());
        client.setName("Test Repository Client");
        client.setContactPerson("Test Contact");
        client.setEmail("test-repo@example.com");
        client.setPhone("555-987-6543");
        client.setAddress("123 Test Repo St, Test City");

        // Save client
        ClientModel savedClient = clientRepository.save(client);

        // Verify client was saved correctly
        assertNotNull(savedClient.getId(), "Client ID should not be null after saving");
        assertEquals("Test Repository Client", savedClient.getName());
        assertEquals("test-repo@example.com", savedClient.getEmail());
    }

    @Test
    public void testFindClientById() {
        // Create and save test client
        ClientModel client = new ClientModel();
        client.setId(UUID.randomUUID().toString());
        client.setName("Find By ID Client");
        client.setContactPerson("Test Contact");
        client.setEmail("find-by-id@example.com");
        ClientModel savedClient = clientRepository.save(client);

        // Find client by ID
        Optional<ClientModel> foundClient = clientRepository.findById(savedClient.getId());

        // Verify client was found
        assertTrue(foundClient.isPresent(), "Client should be found by ID");
        assertEquals("Find By ID Client", foundClient.get().getName());
    }

    @Test
    public void testFindAllClients() {
        // Clear any existing clients
        clientRepository.deleteAll();

        // Create and save multiple clients
        ClientModel client1 = new ClientModel();
        client1.setId(UUID.randomUUID().toString());
        client1.setName("First Test Client");
        client1.setContactPerson("First Contact");
        client1.setEmail("first@example.com");
        clientRepository.save(client1);

        ClientModel client2 = new ClientModel();
        client2.setId(UUID.randomUUID().toString());
        client2.setName("Second Test Client");
        client2.setContactPerson("Second Contact");
        client2.setEmail("second@example.com");
        clientRepository.save(client2);

        // Find all clients
        List<ClientModel> clients = clientRepository.findAll();

        // Verify all clients were found
        assertEquals(2, clients.size(), "Should find 2 clients");
    }

    @Test
    public void testDeleteClient() {
        // Create and save test client
        ClientModel client = new ClientModel();
        client.setId(UUID.randomUUID().toString());
        client.setName("Delete Test Client");
        client.setContactPerson("Delete Contact");
        client.setEmail("delete@example.com");
        ClientModel savedClient = clientRepository.save(client);
        String clientId = savedClient.getId();

        // Delete client
        clientRepository.deleteById(clientId);

        // Verify client was deleted
        Optional<ClientModel> foundClient = clientRepository.findById(clientId);
        assertFalse(foundClient.isPresent(), "Client should not be found after deletion");
    }

    @Test
    public void testFindClientByNameContaining() {
        // Create and save test clients
        ClientModel client1 = new ClientModel();
        client1.setId(UUID.randomUUID().toString());
        client1.setName("ABC Company");
        client1.setContactPerson("ABC Contact");
        client1.setEmail("abc@example.com");
        clientRepository.save(client1);

        ClientModel client2 = new ClientModel();
        client2.setId(UUID.randomUUID().toString());
        client2.setName("XYZ Corporation");
        client2.setContactPerson("XYZ Contact");
        client2.setEmail("xyz@example.com");
        clientRepository.save(client2);

        // Find clients by partial name match
        List<ClientModel> foundClients = clientRepository.findByNameContainingIgnoreCase("Corp");

        // Verify clients were found
        assertEquals(1, foundClients.size(), "Should find 1 client with 'Corp' in name");
        assertEquals("XYZ Corporation", foundClients.get(0).getName());
    }

    @Test
    public void testFindClientByEmail() {
        // Create and save test client
        ClientModel client = new ClientModel();
        client.setId(UUID.randomUUID().toString());
        client.setName("Email Test Client");
        client.setContactPerson("Email Contact");
        client.setEmail("unique-email@example.com");
        clientRepository.save(client);

        // Find client by email
        Optional<ClientModel> foundClient = clientRepository.findByEmail("unique-email@example.com");

        // Verify client was found
        assertTrue(foundClient.isPresent(), "Client should be found by email");
        assertEquals("Email Test Client", foundClient.get().getName());
    }
}