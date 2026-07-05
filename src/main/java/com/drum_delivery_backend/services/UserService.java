package com.drum_delivery_backend.services;

import com.drum_delivery_backend.models.User;
import com.drum_delivery_backend.models.Role;
import com.drum_delivery_backend.models.ERole;
import com.drum_delivery_backend.repositories.UserRepository;
import com.drum_delivery_backend.repositories.RoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    public User createUser(User user) {
        // Check if username already exists
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username is already taken: " + user.getUsername());
        }

        // Check if email already exists
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email is already in use: " + user.getEmail());
        }

        // Encode password
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Set default values
        if (user.getPasswordChangeRequired() == null) {
            user.setPasswordChangeRequired(false);
        }

        // Validate and set roles
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            Set<Role> validatedRoles = new HashSet<>();
            for (Role role : user.getRoles()) {
                Optional<Role> foundRole = roleRepository.findByName(role.getName());
                if (foundRole.isPresent()) {
                    validatedRoles.add(foundRole.get());
                } else {
                    throw new IllegalArgumentException("Role not found: " + role.getName());
                }
            }
            user.setRoles(validatedRoles);
        } else {
            // Set default USER role if no roles provided
            Optional<Role> userRole = roleRepository.findByName(ERole.ROLE_USER);
            if (userRole.isPresent()) {
                Set<Role> defaultRoles = new HashSet<>();
                defaultRoles.add(userRole.get());
                user.setRoles(defaultRoles);
            }
        }

        return userRepository.save(user);
    }

    @Transactional
    public Optional<User> updateUser(Long id, User userDetails) {
        return userRepository.findById(id)
                .map(existingUser -> {
                    // Update basic fields
                    if (userDetails.getFirstName() != null) {
                        existingUser.setFirstName(userDetails.getFirstName());
                    }
                    if (userDetails.getLastName() != null) {
                        existingUser.setLastName(userDetails.getLastName());
                    }
                    if (userDetails.getEmail() != null && !userDetails.getEmail().equals(existingUser.getEmail())) {
                        // Check if new email is already in use
                        if (userRepository.existsByEmail(userDetails.getEmail())) {
                            throw new IllegalArgumentException("Email is already in use: " + userDetails.getEmail());
                        }
                        existingUser.setEmail(userDetails.getEmail());
                    }

                    // Update roles if provided
                    if (userDetails.getRoles() != null) {
                        Set<Role> validatedRoles = new HashSet<>();
                        for (Role role : userDetails.getRoles()) {
                            Optional<Role> foundRole = roleRepository.findByName(role.getName());
                            if (foundRole.isPresent()) {
                                validatedRoles.add(foundRole.get());
                            } else {
                                throw new IllegalArgumentException("Role not found: " + role.getName());
                            }
                        }
                        existingUser.setRoles(validatedRoles);
                    }

                    // Update password change required flag if provided
                    if (userDetails.getPasswordChangeRequired() != null) {
                        existingUser.setPasswordChangeRequired(userDetails.getPasswordChangeRequired());
                    }

                    return userRepository.save(existingUser);
                });
    }

    @Transactional
    public boolean deleteUser(Long id) {
        return userRepository.findById(id)
                .map(user -> {
                    userRepository.delete(user);
                    return true;
                }).orElse(false);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    @Transactional
    public User updateUserPassword(Long id, String newPassword) {
        return userRepository.findById(id)
                .map(user -> {
                    user.setPassword(passwordEncoder.encode(newPassword));
                    user.setPasswordChangeRequired(false);
                    return userRepository.save(user);
                })
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + id));
    }

    public long getUserCount() {
        return userRepository.count();
    }
}