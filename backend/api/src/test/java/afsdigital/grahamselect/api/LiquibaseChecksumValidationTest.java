package afsdigital.grahamselect.api;

import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class LiquibaseChecksumValidationTest {

    @Test
    void shouldValidateExistingDatabaseWithLegacyChecksumForChangeset15() throws Exception {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:liquibase_checksum_repro;DB_CLOSE_DELAY=-1;MODE=MySQL",
                "sa",
                ""
        );

        SpringLiquibase initialLiquibase = createSpringLiquibase(dataSource);
        initialLiquibase.afterPropertiesSet();

        // Simula banco existente onde o changeset 15 foi aplicado antes do commit ad75a72 (type: UUID)
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "UPDATE DATABASECHANGELOG SET MD5SUM = ? WHERE ID = ? AND AUTHOR = ? AND FILENAME = ?")) {
            stmt.setString(1, "9:9bf85a2d48a98906e2fa648f8f81aec1");
            stmt.setString(2, "15-create-manual-trade-audits-table");
            stmt.setString(3, "gemini-cli");
            stmt.setString(4, "db/changelog/15-create-manual-trade-audits-table.yaml");
            int updated = stmt.executeUpdate();
            if (updated != 1) {
                throw new IllegalStateException("Expected 1 row updated in DATABASECHANGELOG, got " + updated);
            }
        }

        SpringLiquibase reRunLiquibase = createSpringLiquibase(dataSource);
        assertDoesNotThrow(reRunLiquibase::afterPropertiesSet,
                "Liquibase não deve falhar na validação de checksum ao inicializar contra um banco existente");
    }

    private SpringLiquibase createSpringLiquibase(DataSource dataSource) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog("classpath:db/changelog/db.changelog-master.yaml");
        liquibase.setResourceLoader(new DefaultResourceLoader());
        return liquibase;
    }
}
