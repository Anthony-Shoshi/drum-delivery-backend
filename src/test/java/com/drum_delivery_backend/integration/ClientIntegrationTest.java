package com.drum_delivery_backend.integration;

import com.drum_delivery_backend.models.ClientModel;
import com.drum_delivery_backend.models.ERole;
import com.drum_delivery_backend.models.Role;
import com.drum_delivery_backend.models.User;
import com.drum_delivery_backend.repositories.ClientRepository;
import com.drum_delivery_backend.repositories.RoleRepository;
import com.drum_delivery_backend.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ClientIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private User regularUser;
    private ClientModel testClient;

    @BeforeEach
    public void setup() {
        // Clean up test data
        clientRepository.deleteAll();
        
        // Create test client
        testClient = new ClientModel();
        testClient.setId(UUID.randomUUID().toString());
        testClient.setName("Integration Test Client");
        testClient.setContactPerson("Integration Test Contact");
        testClient.setEmail("integration@example.com");
        testClient.setPhone("555-123-4567");
        testClient.setAddress("789 Integration St, Test City");
        clientRepository.save(testClient);

        // Setup test users with roles
        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(ERole.ROLE_ADMIN);
                    return roleRepository.save(newRole);
                });

        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(ERole.ROLE_USER);
                    return roleRepository.save(newRole);
                });

        Set<Role> adminRoles = new HashSet<>();
        adminRoles.add(adminRole);

        Set<Role> userRoles = new HashSet<>();
        userRoles.add(userRole);

        adminUser = new User(
                "admin",
                "admin@example.com",
                passwordEncoder.encode("adminPassword")
        );
        adminUser.setRoles(adminRoles);
        userRepository.save(adminUser);

        regularUser = new User(
                "user",
                "user@example.com",
                passwordEncoder.encode("userPassword")
        );
        regularUser.setRoles(userRoles);
        userRepository.save(regularUser);
    }

    @Test
    public void getAllClients_AsAdmin_ShouldReturnAllClients() throws Exception {
        mockMvc.perform(get("/clients")
                    .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].name", is("Integration Test Client")));
    }

    @Test
    public void getClientById_AsUser_ShouldReturnClient() throws Exception {
        String clientId = testClient.getId();

        mockMvc.perform(get("/clients/" + clientId)
                    .with(user("user").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(clientId)))
                .andExpect(jsonPath("$.name", is("Integration Test Client")))
                .andExpect(jsonPath("$.email", is("integration@example.com")));
    }

    @Test
    public void createClient_AsAdmin_ShouldCreateAndReturnClient() throws Exception {
        ClientModel newClient = new ClientModel();
        newClient.setId(UUID.randomUUID().toString());
        newClient.setName("New Integration Client");
        newClient.setContactPerson("New Integration Contact");
        newClient.setEmail("newintegration@example.com");
        newClient.setPhone("555-987-6543");
        newClient.setAddress("321 New Integration Ave, Test Town");

        mockMvc.perform(post("/clients")
                    .with(user("admin").roles("ADMIN"))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newClient)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("New Integration Client")))
                .andExpect(jsonPath("$.email", is("newintegration@example.com")));
    }

    @Test
    public void updateClient_AsAdmin_ShouldUpdateAndReturnClient() throws Exception {
        String clientId = testClient.getId();
        testClient.setName("Updated Integration Client");

        mockMvc.perform(put("/clients/" + clientId)
                    .with(user("admin").roles("ADMIN"))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(testClient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(clientId)))
                .andExpect(jsonPath("$.name", is("Updated Integration Client")));
    }

    @Test
    public void deleteClient_AsAdmin_ShouldDeleteClient() throws Exception {
        String clientId = testClient.getId();

        mockMvc.perform(delete("/clients/" + clientId)
                    .with(user("admin").roles("ADMIN"))
                    .with(csrf()))
                .andExpect(status().isNoContent());

        // Verify client was deleted
        mockMvc.perform(get("/clients/" + clientId)
                    .with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    public void createClient_AsUser_ShouldBeForbidden() throws Exception {
        ClientModel newClient = new ClientModel();
        newClient.setId(UUID.randomUUID().toString());
        newClient.setName("Forbidden Client");
        newClient.setContactPerson("Forbidden Contact");
        newClient.setEmail("forbidden@example.com");

        mockMvc.perform(post("/clients")
                    .with(user("user").roles("USER"))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newClient)))
                .andExpect(status().isForbidden());
    }
}