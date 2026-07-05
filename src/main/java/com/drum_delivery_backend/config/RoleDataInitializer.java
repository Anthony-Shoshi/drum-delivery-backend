package com.drum_delivery_backend.config;

import com.drum_delivery_backend.models.ERole;
import com.drum_delivery_backend.models.Role;
import com.drum_delivery_backend.models.User;
import com.drum_delivery_backend.repositories.RoleRepository;
import com.drum_delivery_backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class RoleDataInitializer implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Value("${ADMIN_DEFAULT_PASSWORD_CHANGE_REQUIRED:true}")
    private Boolean adminPasswordChangeRequired;

    @Override
    public void run(String... args) throws Exception {
        // Initialize roles if they don't exist
        initializeRoles();
        
        // Create admin user if doesn't exist
        createDefaultAdminUser();
    }

    private void initializeRoles() {
        // Check if roles are already initialized
        if (roleRepository.count() == 0) {
            // Create roles
            Role userRole = new Role(ERole.ROLE_USER);
            Role operatorRole = new Role(ERole.ROLE_OPERATOR);
            Role managerRole = new Role(ERole.ROLE_MANAGER);
            Role adminRole = new Role(ERole.ROLE_ADMIN);

            // Save roles
            roleRepository.save(userRole);
            roleRepository.save(operatorRole);
            roleRepository.save(managerRole);
            roleRepository.save(adminRole);
        }
    }
    
    private void createDefaultAdminUser() {
        // Check if admin user already exists
        if (!userRepository.existsByUsername("admin")) {
            // Create admin user
            User adminUser = new User(
                "admin",
                "admin@drumdelivery.com",
                passwordEncoder.encode("DrumAdmin2024!@#"),
                "Admin",
                "User"
            );
            
            // Add admin role to the user
            Set<Role> roles = new HashSet<>();
            Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(adminRole);
            adminUser.setRoles(roles);
            
            // Set password change requirement based on environment
            adminUser.setPasswordChangeRequired(adminPasswordChangeRequired);
            
            // Save admin user
            userRepository.save(adminUser);
        }
    }
}