package com.drum_delivery_backend.controllers;

import com.drum_delivery_backend.models.ClientModel;
import com.drum_delivery_backend.services.ClientService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClientController.class)
@ActiveProfiles("test")
public class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
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
    @WithMockUser(roles = "USER")
    public void getAllClients_ShouldReturnAllClients() throws Exception {
        when(clientService.getAllClients()).thenReturn(testClients);

        mockMvc.perform(get("/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Test Client")))
                .andExpect(jsonPath("$[1].name", is("Second Client")));

        verify(clientService, times(1)).getAllClients();
    }

    @Test
    @WithMockUser(roles = "USER")
    public void getClientById_WithValidId_ShouldReturnClient() throws Exception {
        String clientId = testClient.getId();
        when(clientService.getClientById(clientId)).thenReturn(Optional.of(testClient));

        mockMvc.perform(get("/clients/" + clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(clientId)))
                .andExpect(jsonPath("$.name", is("Test Client")))
                .andExpect(jsonPath("$.email", is("test@example.com")));

        verify(clientService, times(1)).getClientById(clientId);
    }

    @Test
    @WithMockUser(roles = "USER")
    public void getClientById_WithInvalidId_ShouldReturnNotFound() throws Exception {
        String invalidId = "invalid-id";
        when(clientService.getClientById(invalidId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/clients/" + invalidId))
                .andExpect(status().isNotFound());

        verify(clientService, times(1)).getClientById(invalidId);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    public void createClient_WithValidData_ShouldReturnCreatedClient() throws Exception {
        when(clientService.createClient(any(ClientModel.class))).thenReturn(testClient);

        mockMvc.perform(post("/clients")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testClient)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Test Client")))
                .andExpect(jsonPath("$.email", is("test@example.com")));

        verify(clientService, times(1)).createClient(any(ClientModel.class));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    public void updateClient_WithValidData_ShouldReturnUpdatedClient() throws Exception {
        String clientId = testClient.getId();
        testClient.setName("Updated Client Name");
        when(clientService.updateClient(eq(clientId), any(ClientModel.class))).thenReturn(Optional.of(testClient));

        mockMvc.perform(put("/clients/" + clientId)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testClient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Client Name")));

        verify(clientService, times(1)).updateClient(eq(clientId), any(ClientModel.class));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    public void updateClient_WithInvalidId_ShouldReturnNotFound() throws Exception {
        String invalidId = "invalid-id";
        when(clientService.updateClient(eq(invalidId), any(ClientModel.class))).thenReturn(Optional.empty());

        mockMvc.perform(put("/clients/" + invalidId)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testClient)))
                .andExpect(status().isNotFound());

        verify(clientService, times(1)).updateClient(eq(invalidId), any(ClientModel.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void deleteClient_WithValidId_ShouldReturnNoContent() throws Exception {
        String clientId = testClient.getId();
        when(clientService.deleteClient(clientId)).thenReturn(true);

        mockMvc.perform(delete("/clients/" + clientId)
                    .with(csrf()))
                .andExpect(status().isNoContent());

        verify(clientService, times(1)).deleteClient(clientId);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void deleteClient_WithInvalidId_ShouldReturnNotFound() throws Exception {
        String invalidId = "invalid-id";
        when(clientService.deleteClient(invalidId)).thenReturn(false);

        mockMvc.perform(delete("/clients/" + invalidId)
                    .with(csrf()))
                .andExpect(status().isNotFound());

        verify(clientService, times(1)).deleteClient(invalidId);
    }

    @Test
    public void testAccessDeniedForUnauthenticatedUser() throws Exception {
        mockMvc.perform(get("/clients"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testForbiddenForInsufficientPermissions() throws Exception {
        mockMvc.perform(post("/clients")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testClient)))
                .andExpect(status().isForbidden());
    }
}