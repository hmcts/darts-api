package uk.gov.hmcts.darts.cases.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import uk.gov.hmcts.darts.cases.enums.CaseLinkImportStatus;
import uk.gov.hmcts.darts.cases.enums.CaseLinkResultStatus;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportResultStatusRepository;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportStatusRepository;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CaseLinkStatusLookupIntTest extends PostgresIntegrationBase {

    private static final String MIGRATIONS = "db/migration/common/";

    @Autowired
    private DataSource dataSource;
    @Autowired
    private CaseLinkImportStatusRepository importStatusRepository;
    @Autowired
    private CaseLinkImportResultStatusRepository resultStatusRepository;

    @ParameterizedTest
    @EnumSource(CaseLinkImportStatus.class)
    void given_importStatus_when_readingLookup_then_matchStableIdAndCode(CaseLinkImportStatus status) {
        var lookup = importStatusRepository.findById(status.getId()).orElseThrow();
        assertThat(lookup.getStatusType()).isEqualTo(status.name());
        assertThat(lookup.getDisplayName()).isNotBlank();
    }

    @ParameterizedTest
    @EnumSource(CaseLinkResultStatus.class)
    void given_rowStatus_when_readingLookup_then_matchStableIdAndCode(CaseLinkResultStatus status) {
        var lookup = resultStatusRepository.findById(status.getId()).orElseThrow();
        assertThat(lookup.getStatusType()).isEqualTo(status.name());
        assertThat(lookup.getDisplayName()).isNotBlank();
    }

    @Test
    void given_emptySchema_when_creatingImportTables_then_supportLookupStatusesAndBothCourthouses() throws SQLException {
        String schema = "case_link_status_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = dataSource.getConnection()) {
            String originalSchema = connection.getSchema();
            var jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
            jdbc.execute("CREATE SCHEMA " + schema);
            try {
                connection.setSchema(schema);
                jdbc.execute("CREATE TABLE user_account (usr_id INTEGER PRIMARY KEY)");
                jdbc.execute("INSERT INTO user_account VALUES (0)");
                jdbc.execute("CREATE TABLE court_case (cas_id INTEGER PRIMARY KEY)");
                jdbc.execute("CREATE TABLE case_linked_case (clc_id INTEGER PRIMARY KEY)");
                ScriptUtils.executeSqlScript(connection, new ClassPathResource(MIGRATIONS + "V1_523__create_case_link_import_tables.sql"));
                seedAudits(jdbc);
                assertStoredAudits(jdbc);
                assertThatThrownBy(() -> jdbc.update("UPDATE case_link_import SET cis_id = 999 WHERE cli_id = 1"))
                    .isInstanceOf(DataIntegrityViolationException.class);
                assertThatThrownBy(() -> jdbc.update("UPDATE case_link_import_result SET crs_id = 999 WHERE clr_id = 1"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            } finally {
                connection.setSchema(originalSchema);
                jdbc.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }
    }

    private void seedAudits(JdbcTemplate jdbc) {
        for (var status : CaseLinkImportStatus.values()) {
            jdbc.update("""
                INSERT INTO case_link_import (cli_id, original_filename, cis_id, created_ts, created_by, last_modified_ts, last_modified_by)
                VALUES (?, 'original.csv', ?, CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0)
                """, status.getId(), status.getId());
        }
        for (var status : CaseLinkResultStatus.values()) {
            jdbc.update("""
                INSERT INTO case_link_import_result
                    (clr_id, cli_id, row_number, case_1_case_number, case_1_courthouse, case_2_case_number, case_2_courthouse,
                     column_count, crs_id, error_message, created_ts, created_by, last_modified_ts, last_modified_by)
                VALUES (?, 1, ?, 'X1', 'YORK', 'CP1', 'LEEDS', 3, ?, 'Original outcome', CURRENT_TIMESTAMP, 0, CURRENT_TIMESTAMP, 0)
                """, status.getId(), status.getId() + 1, status.getId());
        }
    }

    private void assertStoredAudits(JdbcTemplate jdbc) {
        for (var status : CaseLinkImportStatus.values()) {
            assertThat(jdbc.queryForObject("SELECT cis_id FROM case_link_import WHERE cli_id = ?", Integer.class, status.getId()))
                .isEqualTo(status.getId());
        }
        for (var status : CaseLinkResultStatus.values()) {
            assertThat(jdbc.queryForObject("SELECT crs_id FROM case_link_import_result WHERE clr_id = ?", Integer.class, status.getId()))
                .isEqualTo(status.getId());
        }
        assertThat(jdbc.queryForList("SELECT original_filename FROM case_link_import", String.class)).containsOnly("original.csv");
        assertThat(jdbc.queryForList("SELECT error_message FROM case_link_import_result", String.class)).containsOnly("Original outcome");
        assertThat(jdbc.queryForList("SELECT case_1_courthouse FROM case_link_import_result", String.class)).containsOnly("YORK");
        assertThat(jdbc.queryForList("SELECT case_2_courthouse FROM case_link_import_result", String.class)).containsOnly("LEEDS");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM case_link_import_result WHERE crs_id = ?", Long.class,
                                      CaseLinkResultStatus.SUCCESS.getId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM case_link_import_result WHERE crs_id = ?", Long.class,
                                      CaseLinkResultStatus.FAILED.getId())).isEqualTo(1);
    }
}
