# Backend Testing Guide

This document provides an overview of the testing approach for the backend application.

## Test Structure

The project uses the following test structure:

- **Unit Tests**: Testing individual components (services, repositories)
- **Integration Tests**: Testing component interactions
- **Controller Tests**: Testing REST API endpoints using MockMvc

## Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ClientControllerTest

# Run tests with specific profile
mvn test -Dspring.profiles.active=test
```

## Test Configuration

- **Application Properties**: Test-specific configurations are in `src/test/resources/application-test.properties`
- **Test Profile**: Use `@ActiveProfiles("test")` to apply test configuration

## Testing Examples

### Controller Tests

```java
@WebMvcTest(ClientController.class)
@ActiveProfiles("test")
public class ClientControllerTest {
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ClientService clientService;
    
    @Test
    @WithMockUser(roles = "USER")
    public void getAllClients_ShouldReturnAllClients() throws Exception {
        // Setup test data
        when(clientService.getAllClients()).thenReturn(testClients);
        
        // Perform request and verify response
        mockMvc.perform(get("/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }
}
```

### Service Tests

```java
@ExtendWith(MockitoExtension.class)
public class ClientServiceTest {
    @Mock
    private ClientRepository clientRepository;
    
    @InjectMocks
    private ClientService clientService;
    
    @Test
    public void getAllClients_ShouldReturnAllClients() {
        // Setup test data
        List<ClientModel> clients = Arrays.asList(
            new ClientModel(1L, "Client 1"),
            new ClientModel(2L, "Client 2")
        );
        when(clientRepository.findAll()).thenReturn(clients);
        
        // Call service and verify result
        List<ClientModel> result = clientService.getAllClients();
        assertEquals(2, result.size());
    }
}
```

### Repository Tests

```java
@DataJpaTest
@ActiveProfiles("test")
public class ClientRepositoryTest {
    @Autowired
    private ClientRepository clientRepository;
    
    @Test
    public void findByName_ShouldReturnClient() {
        // Create test data
        ClientModel client = new ClientModel();
        client.setName("Test Client");
        clientRepository.save(client);
        
        // Test repository method
        Optional<ClientModel> result = clientRepository.findByName("Test Client");
        assertTrue(result.isPresent());
        assertEquals("Test Client", result.get().getName());
    }
}
```

## Testing Best Practices

1. **Keep tests independent**: Each test should be able to run in isolation
2. **Use test data factories**: Create reusable methods for test data setup
3. **Use meaningful assertions**: Be specific about what you're testing
4. **Clean up after tests**: Use `@Transactional` or cleanup code to maintain test database state
5. **Mock external dependencies**: Use MockBean for services, repositories, etc.
6. **Test edge cases**: Test error conditions, empty results, etc.

## Security Testing

For security-related testing:

```java
@Test
@WithMockUser(roles = "USER")
public void userCanAccessOwnData() { /* ... */ }

@Test
@WithMockUser(roles = "USER")
public void userCannotAccessOtherUserData() { /* ... */ }

@Test
@WithAnonymousUser
public void anonymousCannotAccessProtectedResources() { /* ... */ }
```