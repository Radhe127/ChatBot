package com.radhe.springweb.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Configuration
public class DataSourceConfig {

    private static final String LOCAL_H2_URL = "jdbc:h2:file:./data/springweb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";

    private HikariDataSource dataSource;

    @Bean
    @Primary
    public DataSource dataSource(Environment environment) {
        HikariDataSource external = tryBuildExternalDataSource(environment);
        dataSource = external != null ? external : buildLocalDataSource();
        return dataSource;
    }

    private HikariDataSource tryBuildExternalDataSource(Environment environment) {
        String rawUrl = firstNonBlank(
            environment.getProperty("DB_URL"),
            environment.getProperty("SUPABASE_DB_URL"),
            environment.getProperty("DATABASE_URL")
        );
        if (rawUrl == null || rawUrl.isBlank() || rawUrl.startsWith("jdbc:h2:")) {
            return null;
        }

        String url = normalizeJdbcUrl(rawUrl);
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(firstNonBlank(
            environment.getProperty("DB_USERNAME"),
            environment.getProperty("SUPABASE_DB_USERNAME"),
            extractUsername(rawUrl),
            "postgres"
        ));
        config.setPassword(firstNonBlank(
            environment.getProperty("DB_PASSWORD"),
            environment.getProperty("SUPABASE_DB_PASSWORD"),
            extractPassword(rawUrl),
            ""
        ));
        config.setDriverClassName("org.postgresql.Driver");

        try {
            HikariDataSource candidate = new HikariDataSource(config);
            try (Connection ignored = candidate.getConnection()) {
                return candidate;
            } catch (SQLException ex) {
                candidate.close();
                return null;
            }
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private String normalizeJdbcUrl(String rawUrl) {
        if (rawUrl.startsWith("jdbc:")) {
            return rawUrl;
        }

        if (rawUrl.startsWith("postgresql://") || rawUrl.startsWith("postgres://")) {
            return "jdbc:" + rawUrl.replaceFirst("^postgres(ql)?://", "postgresql://");
        }

        return rawUrl;
    }

    private String extractUsername(String rawUrl) {
        try {
            URI uri = parsePostgresUri(rawUrl);
            if (uri == null || uri.getUserInfo() == null || uri.getUserInfo().isBlank()) {
                return null;
            }
            String[] parts = uri.getUserInfo().split(":", 2);
            return parts.length > 0 ? parts[0] : null;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String extractPassword(String rawUrl) {
        try {
            URI uri = parsePostgresUri(rawUrl);
            if (uri == null || uri.getUserInfo() == null || uri.getUserInfo().isBlank()) {
                return null;
            }
            String[] parts = uri.getUserInfo().split(":", 2);
            if (parts.length < 2) {
                return null;
            }
            return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private URI parsePostgresUri(String rawUrl) {
        if (!(rawUrl.startsWith("postgresql://") || rawUrl.startsWith("postgres://"))) {
            return null;
        }
        try {
            return new URI(rawUrl);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Invalid PostgreSQL connection string", ex);
        }
    }

    private HikariDataSource buildLocalDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(LOCAL_H2_URL);
        config.setUsername("sa");
        config.setPassword("");
        return new HikariDataSource(config);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    @PreDestroy
    public void shutdown() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}