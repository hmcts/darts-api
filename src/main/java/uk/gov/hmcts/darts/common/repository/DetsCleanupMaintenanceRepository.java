package uk.gov.hmcts.darts.common.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.SQLWarningException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLWarning;
import java.sql.Statement;

@Repository
@RequiredArgsConstructor
public class DetsCleanupMaintenanceRepository {

    private final JdbcTemplate jdbcTemplate;

    public void vacuumTables(int timeoutSeconds) {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            boolean autoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(true);
                try (Statement statement = connection.createStatement()) {
                    statement.setQueryTimeout(timeoutSeconds);
                    statement.execute("VACUUM (ANALYZE) darts.external_object_directory, darts.object_state_record");
                    SQLWarning warning = statement.getWarnings();
                    if (warning != null) {
                        throw new SQLWarningException("DETS cleanup vacuum reported a warning", warning);
                    }
                }
            } finally {
                connection.setAutoCommit(autoCommit);
            }
            return null;
        });
    }
}
