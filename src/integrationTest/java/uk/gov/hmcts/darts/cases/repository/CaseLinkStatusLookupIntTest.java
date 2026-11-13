package uk.gov.hmcts.darts.cases.repository;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.cases.enums.CaseLinkImportStatus;
import uk.gov.hmcts.darts.cases.enums.CaseLinkResultStatus;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportResultStatusRepository;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportStatusRepository;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

class CaseLinkStatusLookupIntTest extends PostgresIntegrationBase {
    
    @Autowired
    private DataSource dataSource;
    @Autowired
    private CaseLinkImportStatusRepository importStatusRepository;
    @Autowired
    private CaseLinkImportResultStatusRepository resultStatusRepository;

    @ParameterizedTest
    @EnumSource(CaseLinkImportStatus.class)
    void findById_shouldReturnMatchingCodeAndDisplayName_whenImportStatusExists(CaseLinkImportStatus status) {
        var lookup = importStatusRepository.findById(status.getId()).orElseThrow();
        assertThat(lookup.getStatusType()).isEqualTo(status.name());
        assertThat(lookup.getDisplayName()).isNotBlank();
    }

    @ParameterizedTest
    @EnumSource(CaseLinkResultStatus.class)
    void findById_shouldReturnMatchingCodeAndDisplayName_whenResultStatusExists(CaseLinkResultStatus status) {
        var lookup = resultStatusRepository.findById(status.getId()).orElseThrow();
        assertThat(lookup.getStatusType()).isEqualTo(status.name());
        assertThat(lookup.getDisplayName()).isNotBlank();
    }
}
