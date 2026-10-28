package uk.gov.hmcts.darts.cases.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.cases.enums.CaseLinkImportStatus;
import uk.gov.hmcts.darts.cases.enums.CaseLinkResultStatus;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportResultStatusRepository;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportStatusRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import static org.assertj.core.api.Assertions.assertThat;

class CaseLinkStatusLookupH2IntTest extends IntegrationBase {

    @Autowired
    private CaseLinkImportStatusRepository importStatusRepository;
    @Autowired
    private CaseLinkImportResultStatusRepository resultStatusRepository;

    @Test
    void given_h2Migration_when_loadingLookups_then_seedEveryStatus() {
        assertThat(importStatusRepository.findAll()).hasSize(CaseLinkImportStatus.values().length);
        assertThat(resultStatusRepository.findAll()).hasSize(CaseLinkResultStatus.values().length);
        for (var status : CaseLinkImportStatus.values()) {
            assertThat(importStatusRepository.findById(status.getId()).orElseThrow().getStatusType()).isEqualTo(status.name());
        }
        for (var status : CaseLinkResultStatus.values()) {
            assertThat(resultStatusRepository.findById(status.getId()).orElseThrow().getStatusType()).isEqualTo(status.name());
        }
    }
}
