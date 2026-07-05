package com.drum_delivery_backend.services;

import com.drum_delivery_backend.models.ClientModel;
import com.drum_delivery_backend.repositories.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientService clientService;

    private ClientModel testClient;
    private List<ClientModel> testClients;

    @BeforeEach
    public void setup() {
        testClient = new ClientModel();
        testClient.setId(UUID.randomUUID().toString());
        testClient.setName("Test Client");
        testClient.setContactPerson("Test Contact");
        testClient.setEmail("test@example.com");
        testClient.setPhone("123-456-7890");
        testClient.setAddress("123 Test St, Test City");

        ClientModel secondClient = new ClientModel();
        secondClient.setId(UUID.randomUUID().toString());
        secondClient.setName("Second Client");
        secondClient.setContactPerson("Another Contact");
        secondClient.setEmail("another@example.com");
        secondClient.setPhone("987-654-3210");
        secondClient.setAddress("456 Test Ave, Test Town");

        testClients = Arrays.asList(testClient, secondClient);
    }

    @Test
    public void getAllClients_ShouldReturnAllClients() {
        when(clientRepository.findAll()).thenReturn(testClients);

        List<ClientModel> result = clientService.getAllClients();

        assertEquals(2, result.size());
        assertEquals("Test Client", result.get(0).getName());
        assertEquals("Second Client", result.get(1).getName());
        verify(clientRepository, times(1)).findAll();
    }

    @Test
    public void getClientById_WithValidId_ShouldReturnClient() {
        String clientId = testClient.getId();
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(testClient));

        Optional<ClientModel> result = clientService.getClientById(clientId);

        assertTrue(result.isPresent());
        assertEquals("Test Client", result.get().getName());
        assertEquals("test@example.com", result.get().getEmail());
        verify(clientRepository, times(1)).findById(clientId);
    }

    @Test
    public void getClientById_WithInvalidId_ShouldReturnEmpty() {
        String invalidId = "invalid-id";
        when(clientRepository.findById(invalidId)).thenReturn(Optional.empty());

        Optional<ClientModel> result = clientService.getClientById(invalidId);

        assertFalse(result.isPresent());
        verify(clientRepository, times(1)).findById(invalidId);
    }

    @Test
    public void getClientByEmail_WithValidEmail_ShouldReturnClient() {
        String email = "test@example.com";
        when(clientRepository.findByEmail(email)).thenReturn(Optional.of(testClient));

        Optional<ClientModel> result = clientService.getClientByEmail(email);

        assertTrue(result.isPresent());
        assertEquals("Test Client", result.get().getName());
        assertEquals(email, result.get().getEmail());
        verify(clientRepository, times(1)).findByEmail(email);
    }

    @Test
    public void searchClientsByName_ShouldReturnMatchingClients() {
        String searchTerm = "Test";
        when(clientRepository.findByNameContainingIgnoreCase(searchTerm)).thenReturn(Arrays.asList(testClient));

        List<ClientModel> result = clientService.searchClientsByName(searchTerm);

        assertEquals(1, result.size());
        assertEquals("Test Client", result.get(0).getName());
        verify(clientRepository, times(1)).findByNameContainingIgnoreCase(searchTerm);
    }

    @Test
    public void createClient_ShouldSaveAndReturnClient() {
        when(clientRepository.save(any(ClientModel.class))).thenReturn(testClient);

        ClientModel clientToCreate = new ClientModel();
        clientToCreate.setName("New Client");
        clientToCreate.setEmail("new@example.com");
        clientToCreate.setContactPerson("New Contact");

        ClientModel result = clientService.createClient(clientToCreate);

        assertNotNull(result);
        assertEquals("Test Client", result.getName());
        assertEquals("test@example.com", result.getEmail());
        verify(clientRepository, times(1)).save(any(ClientModel.class));
    }

    @Test
    public void createClient_WithoutId_ShouldGenerateIdAndSave() {
        ClientModel clientWithoutId = new ClientModel();
        clientWithoutId.setName("Client Without ID");
        clientWithoutId.setEmail("noid@example.com");
        clientWithoutId.setContactPerson("No ID Contact");

        ClientModel savedClient = new ClientModel();
        savedClient.setId(UUID.randomUUID().toString());
        savedClient.setName("Client Without ID");
        savedClient.setEmail("noid@example.com");
        savedClient.setContactPerson("No ID Contact");

        when(clientRepository.save(any(ClientModel.class))).thenReturn(savedClient);

        ClientModel result = clientService.createClient(clientWithoutId);

        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals("Client Without ID", result.getName());
        verify(clientRepository, times(1)).save(any(ClientModel.class));
    }

    @Test
    public void updateClient_WithValidId_ShouldUpdateAndReturnClient() {
        String clientId = testClient.getId();
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(testClient));
        when(clientRepository.save(any(ClientModel.class))).thenReturn(testClient);

        ClientModel updateDetails = new ClientModel();
        updateDetails.setName("Updated Client Name");
        updateDetails.setEmail("updated@example.com");

        Optional<ClientModel> result = clientService.updateClient(clientId, updateDetails);

        assertTrue(result.isPresent());
        verify(clientRepository, times(1)).findById(clientId);
        verify(clientRepository, times(1)).save(any(ClientModel.class));
    }

    @Test
    public void updateClient_WithInvalidId_ShouldReturnEmpty() {
        String invalidId = "invalid-id";
        when(clientRepository.findById(invalidId)).thenReturn(Optional.empty());

        ClientModel updateDetails = new ClientModel();
        updateDetails.setName("Updated Client Name");

        Optional<ClientModel> result = clientService.updateClient(invalidId, updateDetails);

        assertFalse(result.isPresent());
        verify(clientRepository, times(1)).findById(invalidId);
        verify(clientRepository, never()).save(any(ClientModel.class));
    }

    @Test
    public void deleteClient_WithValidId_ShouldReturnTrue() {
        String clientId = testClient.getId();
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(testClient));
        doNothing().when(clientRepository).delete(testClient);

        boolean result = clientService.deleteClient(clientId);

        assertTrue(result);
        verify(clientRepository, times(1)).findById(clientId);
        verify(clientRepository, times(1)).delete(testClient);
    }

    @Test
    public void deleteClient_WithInvalidId_ShouldReturnFalse() {
        String invalidId = "invalid-id";
        when(clientRepository.findById(invalidId)).thenReturn(Optional.empty());

        boolean result = clientService.deleteClient(invalidId);

        assertFalse(result);
        verify(clientRepository, times(1)).findById(invalidId);
        verify(clientRepository, never()).delete(any(ClientModel.class));
    }
}