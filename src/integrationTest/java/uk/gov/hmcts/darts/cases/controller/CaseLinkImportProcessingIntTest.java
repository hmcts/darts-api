package uk.gov.hmcts.darts.cases.controller;

import jakarta.persistence.EntityManager;
import org.apache.commons.codec.digest.DigestUtils;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.cases.enums.CaseLinkImportStatus;
import uk.gov.hmcts.darts.cases.enums.CaseLinkResultStatus;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.cases.service.CaseLinkRowValidator;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportRepository;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportResultRepository;
import uk.gov.hmcts.darts.common.repository.CaseLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;

@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.jpa.open-in-view=true")
class CaseLinkImportProcessingIntTest extends PostgresIntegrationBase {

    private static final String ENDPOINT = "/admin/case-link-imports";
    private static final String HEADER = "XHIBIT_CASE,XHIBIT_COURTHOUSE,CP_URN\n";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private CaseLinkImportRepository importRepository;
    @MockitoSpyBean
    private CaseLinkImportResultRepository resultRepository;
    @Autowired
    private CaseLinkedCaseRepository linkedCaseRepository;
    @Autowired
    private CaseRepository caseRepository;
    @MockitoSpyBean
    private CaseLinkRowValidator rowValidator;

    private CourthouseEntity courthouse;

    @BeforeEach
    void setUp() {
        CourthouseEntity courthouseEntity = PersistableFactory.getCourthouseTestData().someMinimal();
        courthouseEntity.setCourthouseName("YORK");
        courthouse = dartsPersistence.save(courthouseEntity);
        givenBuilder.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        dartsPersistence.save(PersistableFactory.getCourtCaseTestData().createCaseWith("X1", courthouse));
    }

    @DisplayName("""
        adminCaseLinkImportsPost()
        SHOULD bound the managed entity count while staging audit rows
            AND prevent managed entities accumulating across consecutive successful and failed rows
            AND retain every successful and failed row outcome
        WHEN an HTTP upload spans multiple staging batches and contains successful rows,
            missing source cases and duplicate mappings with Open EntityManager in View enabled
        """)
    @Test
    void adminCaseLinkImportsPost_shouldBoundManagedEntities_whenLargeImportContainsSuccessesAndFailures() throws Exception {
        AtomicInteger stagedBatchCount = new AtomicInteger();
        AtomicInteger processedRowCount = new AtomicInteger();
        // Spring Data repositories are interface proxies; retain the spy's original delegate.
        var repositoryDelegate = mockingDetails(resultRepository).getMockCreationSettings().getDefaultAnswer();
        doAnswer(invocation -> {
            Object persistedBatch = repositoryDelegate.answer(invocation);
            List<CaseLinkImportResultEntity> stagedRows = invocation.getArgument(0);
            assertThat(stagedRows).hasSizeLessThanOrEqualTo(1000);
            assertThat(entityManager.unwrap(Session.class).getStatistics().getEntityCount()).isLessThanOrEqualTo(1100);
            stagedBatchCount.incrementAndGet();
            return persistedBatch;
        }).when(resultRepository).saveAllAndFlush(any());
        doAnswer(invocation -> {
            CaseLinkImportResultEntity currentRow = invocation.getArgument(0);
            assertThat(entityManager.unwrap(Session.class).getStatistics().getEntityCount())
                .as("Managed entities before processing CSV row %s", currentRow.getRowNumber()).isLessThanOrEqualTo(10);
            processedRowCount.incrementAndGet();
            return invocation.callRealMethod();
        }).when(rowValidator).validate(any(), any());
        String successfulRows = IntStream.range(0, 400).mapToObj(index -> "X1,YORK,CP" + index + "\n").collect(Collectors.joining());
        String missingCaseRows = IntStream.range(0, 400).mapToObj(index -> "MISSING" + index + ",YORK,CP_MISSING" + index + "\n")
            .collect(Collectors.joining());
        String dataRows = successfulRows + missingCaseRows + successfulRows;
        var uploadedFile = new MockMultipartFile("file", "cases.csv", "text/csv", (HEADER + dataRows).getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart(ENDPOINT).file(uploadedFile))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED_WITH_ERRORS"))
            .andExpect(jsonPath("$.total_rows").value(1200))
            .andExpect(jsonPath("$.successful_rows").value(400))
            .andExpect(jsonPath("$.failed_rows").value(800));

        assertThat(stagedBatchCount).hasValue(2);
        assertThat(processedRowCount).hasValue(1200);
        assertThat(resultRepository.count()).isEqualTo(1200);
        assertThat(linkedCaseRepository.count()).isEqualTo(400);
        assertThat(caseRepository.count()).isEqualTo(401);
        assertThat(importRepository.findAll()).singleElement().satisfies(importAudit -> {
            assertThat(importAudit.getProcessingCompletedAt()).isNotNull();
            assertThat(resultRepository.countByImportIdAndStatusId(importAudit.getId(), CaseLinkResultStatus.SUCCESS.getId())).isEqualTo(400);
            assertThat(resultRepository.countByImportIdAndStatusId(importAudit.getId(), CaseLinkResultStatus.FAILED.getId())).isEqualTo(800);
        });
    }

    @DisplayName("""
        adminCaseLinkImportsPost()
        SHOULD reject the import with FILE_READ_FAILED
            AND retain the calculated checksum and rejection audit
            AND create no row results, cases or links
        WHEN checksum calculation succeeds but opening or reading the CSV stream subsequently fails
        """)
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void adminCaseLinkImportsPost_shouldAuditFileReadFailure_whenCsvStreamFailsAfterChecksum(boolean openingFails) throws Exception {
        byte[] csvContent = (HEADER + "X1,YORK,CP1\n").getBytes(StandardCharsets.UTF_8);
        var uploadedFile = spy(new MockMultipartFile("file", "cases.csv", "text/csv", csvContent));
        try (InputStream checksumInputStream = new ByteArrayInputStream(csvContent);
             InputStream failingCsvInputStream = mock(InputStream.class)) {
            IOException csvReadingFailure = new IOException("CSV stream unavailable after checksum");
            if (openingFails) {
                doReturn(checksumInputStream).doThrow(csvReadingFailure).when(uploadedFile).getInputStream();
            } else {
                when(failingCsvInputStream.read(any(byte[].class), anyInt(), anyInt())).thenThrow(csvReadingFailure);
                doReturn(checksumInputStream, failingCsvInputStream).when(uploadedFile).getInputStream();
            }

            mockMvc.perform(multipart(ENDPOINT).file(uploadedFile))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value(CaseApiError.FILE_READ_FAILED.getType()))
                .andExpect(jsonPath("$.title").value(CaseApiError.FILE_READ_FAILED.getTitle()))
                .andExpect(jsonPath("$.error_code").value("FILE_READ_FAILED"))
                .andExpect(jsonPath("$.import_id").isNumber());

            assertThat(importRepository.findAll()).singleElement().satisfies(importAudit -> {
                assertThat(importAudit.getStatus().getId()).isEqualTo(CaseLinkImportStatus.REJECTED.getId());
                assertThat(importAudit.getErrorCode()).isEqualTo("FILE_READ_FAILED");
                assertThat(importAudit.getErrorMessage()).isEqualTo(CaseApiError.FILE_READ_FAILED.getTitle());
                assertThat(importAudit.getFileChecksum()).isEqualTo(DigestUtils.md5Hex(csvContent));
                assertThat(importAudit.getProcessingCompletedAt()).isNotNull();
            });
            assertThat(resultRepository.count()).isZero();
            assertThat(linkedCaseRepository.count()).isZero();
            assertThat(caseRepository.findByCaseNumberAndCourthouse("CP1", courthouse)).isEmpty();
        }
    }
}
