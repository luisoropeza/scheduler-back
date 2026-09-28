package com.example.scheduler.service.impl;

import com.example.scheduler.service.SchemaProvisioningService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Service
@RequiredArgsConstructor
public class SchemaProvisioningServiceImpl implements SchemaProvisioningService {
    private static final String PUBLIC_SCHEMA_DDL = "sql/public-schema.sql";
    private static final String TENANT_SCHEMA_DDL = "sql/tenant-schema.sql";

    private final DataSource dataSource;

    @PostConstruct
    private void createPublicSchema() {
        try {
            executeDdl(loadDdl(PUBLIC_SCHEMA_DDL));
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Failed to provision the public schema", e);
        }
    }

    @Override
    public void createTenantSchema(String schemaName) {
        validateSchemaName(schemaName);
        try {
            var ddl = loadDdl(TENANT_SCHEMA_DDL).replace("{schema}", schemaName);
            executeDdl("CREATE SCHEMA IF NOT EXISTS " + schemaName + ";" + ddl);
        } catch (SQLException | IOException e) {
            throw new IllegalStateException("Failed to provision tenant schema: " + schemaName, e);
        }
    }

    private void executeDdl(String ddl) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(true);
            try (Statement stmt = conn.createStatement()) {
                for (String sql : ddl.split(";")) {
                    var trimmed = sql.strip();
                    if (!trimmed.isEmpty())
                        stmt.execute(trimmed);
                }
            }
        }
    }

    private String loadDdl(String resourcePath) throws IOException {
        var resource = new ClassPathResource(resourcePath);
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    private void validateSchemaName(String schemaName) {
        if (schemaName == null || !schemaName.matches("^clinic_\\d+$"))
            throw new IllegalArgumentException("Invalid tenant schema name: " + schemaName);
    }
}
