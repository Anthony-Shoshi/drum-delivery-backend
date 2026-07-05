package com.drum_delivery_backend.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Custom health indicator for monitoring database connectivity
 */
@Configuration
public class DatabaseHealthIndicator {

    /**
     * Creates a custom database health indicator bean that monitors
     * the database connectivity using a validation query
     *
     * @param dataSource The application datasource
     * @return A health indicator for the database
     */
    @Bean
    public HealthIndicator dbHealthIndicator(DataSource dataSource) {
        DataSourceHealthIndicator indicator = new DataSourceHealthIndicator(dataSource, "SELECT 1");
        
        // Add additional details to health check
        return () -> {
            Health health = indicator.health();
            if (health.getStatus().getCode().equals("UP")) {
                JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                String dbProductName = jdbcTemplate.queryForObject("SELECT COALESCE(@@version, VERSION())", String.class);
                long connectionCount = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM information_schema.processlist", Long.class);
                
                return Health.up()
                        .withDetail("database", dbProductName)
                        .withDetail("active_connections", connectionCount)
                        .build();
            }
            return health;
        };
    }
}