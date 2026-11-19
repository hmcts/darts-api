package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.SQLWarningException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DetsCleanupMaintenanceRepositoryTest {

    private static final String VACUUM_SQL = "VACUUM (ANALYZE) darts.external_object_directory, darts.object_state_record";

    @Mock
    private DataSource dataSource;
    @Mock
    private Connection connection;
    @Mock
    private Statement statement;

    private DetsCleanupMaintenanceRepository repository;

    @BeforeEach
    void setUp() throws SQLException {
        repository = new DetsCleanupMaintenanceRepository(new JdbcTemplate(dataSource));
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void vacuumTables_shouldExecuteInAutoCommitAndRestoreConnection_whenVacuumSucceeds(boolean autoCommit) throws SQLException {
        when(connection.getAutoCommit()).thenReturn(autoCommit);

        repository.vacuumTables();

        InOrder ordered = inOrder(connection, statement);
        ordered.verify(connection).getAutoCommit();
        ordered.verify(connection).setAutoCommit(true);
        ordered.verify(connection).createStatement();
        ordered.verify(statement).execute(VACUUM_SQL);
        ordered.verify(statement).close();
        ordered.verify(connection).setAutoCommit(autoCommit);
        ordered.verify(connection).close();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void vacuumTables_shouldRestoreConnectionAndPropagateFailure_whenVacuumFails(boolean autoCommit) throws SQLException {
        when(connection.getAutoCommit()).thenReturn(autoCommit);
        SQLException failure = new SQLException("Vacuum failed", "XX000");
        when(statement.execute(VACUUM_SQL)).thenThrow(failure);

        assertThatThrownBy(() -> repository.vacuumTables())
            .isInstanceOf(DataAccessException.class)
            .hasCause(failure);

        InOrder ordered = inOrder(connection, statement);
        ordered.verify(connection).setAutoCommit(true);
        ordered.verify(statement).execute(VACUUM_SQL);
        ordered.verify(statement).close();
        ordered.verify(connection).setAutoCommit(autoCommit);
        verify(connection).close();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void vacuumTables_shouldRestoreConnectionAndReportWarning_whenPostgresSkipsTable(boolean autoCommit) throws SQLException {
        when(connection.getAutoCommit()).thenReturn(autoCommit);
        SQLWarning warning = new SQLWarning("Skipping table: permission denied", "01000");
        when(statement.getWarnings()).thenReturn(warning);

        assertThatThrownBy(() -> repository.vacuumTables())
            .isInstanceOf(SQLWarningException.class)
            .hasCause(warning);

        InOrder ordered = inOrder(connection, statement);
        ordered.verify(connection).setAutoCommit(true);
        ordered.verify(statement).execute(VACUUM_SQL);
        ordered.verify(statement).close();
        ordered.verify(connection).setAutoCommit(autoCommit);
        verify(connection).close();
    }
}
