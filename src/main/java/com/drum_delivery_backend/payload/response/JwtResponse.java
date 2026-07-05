package com.drum_delivery_backend.payload.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private List<String> roles;
    private Boolean passwordChangeRequired;
    
    // Manual implementation of builder pattern in case Lombok doesn't work
    public static JwtResponseBuilder builder() {
        return new JwtResponseBuilder();
    }
    
    public static class JwtResponseBuilder {
        private String accessToken;
        private String refreshToken;
        private String tokenType;
        private Long id;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private List<String> roles;
        private Boolean passwordChangeRequired;
        
        public JwtResponseBuilder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }
        
        public JwtResponseBuilder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }
        
        public JwtResponseBuilder tokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }
        
        public JwtResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }
        
        public JwtResponseBuilder username(String username) {
            this.username = username;
            return this;
        }
        
        public JwtResponseBuilder email(String email) {
            this.email = email;
            return this;
        }
        
        public JwtResponseBuilder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }
        
        public JwtResponseBuilder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }
        
        public JwtResponseBuilder roles(List<String> roles) {
            this.roles = roles;
            return this;
        }
        
        public JwtResponseBuilder passwordChangeRequired(Boolean passwordChangeRequired) {
            this.passwordChangeRequired = passwordChangeRequired;
            return this;
        }
        
        public JwtResponse build() {
            return new JwtResponse(accessToken, refreshToken, tokenType, id, username, email, firstName, lastName, roles, passwordChangeRequired);
        }
    }
}