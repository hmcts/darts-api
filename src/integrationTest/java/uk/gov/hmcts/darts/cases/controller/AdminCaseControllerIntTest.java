package uk.gov.hmcts.darts.cases.controller;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.cases.enums.CaseLinkImportStatus;
import uk.gov.hmcts.darts.cases.enums.CaseLinkResultStatus;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.cases.model.CaseLinkImportResponse;
import uk.gov.hmcts.darts.cases.service.CaseLinkImportService;
import uk.gov.hmcts.darts.cases.service.CaseLinkImportSummaryService;
import uk.gov.hmcts.darts.cases.service.CaseLinkRowService;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultEntity;
import uk.gov.hmcts.darts.common.entity.CaseLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportRepository;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportResultRepository;
import uk.gov.hmcts.darts.common.repository.CaseLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;

@AutoConfigureMockMvc
class AdminCaseControllerIntTest extends PostgresIntegrationBase {

    private static final String FAILING_CASE = "CP_FAIL";
    private static final String ENDPOINT = "/admin/case-link-imports";
    private static final String HEADER = "XHIBIT_CASE,XHIBIT_COURTHOUSE,CP_URN\n";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CaseLinkImportRepository importRepository;
    @Autowired
    private CaseLinkImportResultRepository resultRepository;
    @Autowired
    private CaseLinkedCaseRepository linkedCaseRepository;
    @Autowired
    private CaseRepository caseRepository;
    @Autowired
    private CaseLinkImportSummaryService summaryService;
    @Autowired
    private CaseLinkImportService importService;
    @MockitoSpyBean
    private CaseLinkRowService rowService;

    private CourthouseEntity courthouse;
    private UserAccountEntity user;

    @BeforeEach
    void setUp() {
        CourthouseEntity courthouseEntity = PersistableFactory.getCourthouseTestData().someMinimal();
        courthouseEntity.setCourthouseName("YORK");
        courthouse = dartsPersistence.save(courthouseEntity);
        user = givenBuilder.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        createCase("X1");
        createCase("X2");
    }

    @DisplayName("""
        adminCaseLinkImportsPost()
        SHOULD create the missing case 2 and reuse it for the next mapping
            AND store both case links and successful row outcomes
            AND retain the uploader, filename, checksum and processing timestamps
            AND return a completed summary that can be regenerated from the audit
        WHEN valid rows refer to the same missing case 2 at an existing courthouse
        """)
    @Test
    void adminCaseLinkImportsPost_shouldCompleteImport_whenRowsShareMissingCase2() throws Exception {
        var upload = file(" X1 , york , CP1 \nX2,YORK,CP1\n");
        var response = post(upload);
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getSuccessfulRows()).isEqualTo(2);
        assertThat(response.getFailedRows()).isZero();
        assertThat(response.getFailures()).isEmpty();
        assertThat(linkedCaseRepository.count()).isEqualTo(2);
        var cp = caseRepository.findByCaseNumberAndCourthouse("CP1", courthouse).orElseThrow();
        assertThat(cp.getClosed()).isFalse();
        assertThat(cp.getInterpreterUsed()).isFalse();
        assertThat(cp.getCreatedById()).isEqualTo(user.getId());
        var rows = resultRepository.findByImportIdAndStatusIdOrderByRowNumber(response.getImportId(), CaseLinkResultStatus.SUCCESS.getId());
        assertThat(rows).extracting(CaseLinkImportResultEntity::isCase2Created).containsExactly(true, false);
        assertThat(rows).extracting(CaseLinkImportResultEntity::getCase2Id).containsOnly(cp.getId());
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.getLinkedCaseId()).isNotNull();
            assertThat(row.getCreatedById()).isEqualTo(user.getId());
            assertThat(row.getCase2Courthouse()).isEqualTo(row.getCase1Courthouse());
        });
        var audit = importRepository.findById(response.getImportId()).orElseThrow();
        assertThat(audit.getOriginalFilename()).isEqualTo("cases.csv");
        assertThat(audit.getFileChecksum()).isEqualTo(DigestUtils.md5Hex(upload.getBytes()));
        assertThat(audit.getCreatedById()).isEqualTo(user.getId());
        assertThat(audit.getCreatedDateTime()).isNotNull();
        assertThat(audit.getProcessingStartedAt()).isNotNull();
        assertThat(audit.getProcessingCompletedAt()).isAfterOrEqualTo(audit.getProcessingStartedAt());
        assertThat(summaryService.getSummary(response.getImportId())).isEqualTo(response);
    }

    @Test
    void adminCaseLinkImportsPost_shouldReuseLink_whenItExistsInEitherOrder() throws Exception {
        var first = post(file("X1,YORK,CP1\n"));
        var second = post(file("X1,YORK,CP1\n"));
        var reversed = post(file("CP1,YORK,X1\n"));
        assertThat(linkedCaseRepository.count()).isEqualTo(1);
        var link = resultRepository.findByImportIdAndStatusIdOrderByRowNumber(first.getImportId(), CaseLinkResultStatus.SUCCESS.getId()).getFirst();
        for (var response : new CaseLinkImportResponse[]{second, reversed}) {
            assertThat(response.getSuccessfulRows()).isEqualTo(1);
            assertThat(resultRepository.findByImportIdAndStatusIdOrderByRowNumber(response.getImportId(), CaseLinkResultStatus.SUCCESS.getId()).getFirst()
                           .getLinkedCaseId()).isEqualTo(link.getLinkedCaseId());
        }
    }

    // Exercise competing import transactions against PostgreSQL.
    @DisplayName("""
        importCaseLinkingFile()
        SHOULD complete both imports successfully
            AND create one case 2 and one case link
            AND record which import created case 2
        WHEN two imports process the same mapping concurrently
        """)
    @Test
    @SuppressWarnings("PMD.DoNotUseThreads")
    void importCaseLinkingFile_shouldCreateOneCaseAndLink_whenSameMappingIsImportedConcurrently() throws Exception {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var start = new CountDownLatch(1);
        Callable<CaseLinkImportResponse> upload = () -> {
            SecurityContextHolder.getContext().setAuthentication(authentication);
            try {
                assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                return importService.importCaseLinkingFile(file("X1,YORK,CP_CONCURRENT"));
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(upload);
            var second = executor.submit(upload);
            start.countDown();
            assertThat(first.get(30, TimeUnit.SECONDS).getSuccessfulRows()).isEqualTo(1);
            assertThat(second.get(30, TimeUnit.SECONDS).getSuccessfulRows()).isEqualTo(1);
        }
        assertThat(linkedCaseRepository.count()).isEqualTo(1);
        assertThat(caseRepository.findByCaseNumberAndCourthouse("CP_CONCURRENT", courthouse)).isPresent();
        assertThat(resultRepository.findAll()).extracting(CaseLinkImportResultEntity::isCase2Created).containsExactlyInAnyOrder(true, false);
    }

    @DisplayName("""
        adminCaseLinkImportsPost()
        SHOULD process valid rows and report partial success
            AND audit every row with the correct outcome
            AND report each failure with its CSV row number and generic error code
            AND avoid creating cases for unresolved mappings
        WHEN rows contain missing cases or courthouses, missing values, self-links,
            duplicate or reversed mappings, and incorrect column counts
        """)
    @Test
    void adminCaseLinkImportsPost_shouldReportPartialSuccess_whenFileContainsInvalidRows() throws Exception {
        var response = post(file("""
                                     MISSING,YORK,CP_MISSING
                                     X1,UNKNOWN,CP_UNKNOWN
                                     ,YORK,CP_REQUIRED
                                     X1,YORK,X1
                                     X1,YORK,CP1
                                      X1 , york , CP1
                                     CP1,YORK,X1
                                     X2,YORK
                                     X2,YORK,CP2,EXTRA
                                     X2,YORK,CP2
                                     """));
        assertThat(response.getStatus()).isEqualTo("COMPLETED_WITH_ERRORS");
        assertThat(response.getTotalRows()).isEqualTo(10);
        assertThat(response.getSuccessfulRows()).isEqualTo(2);
        assertThat(response.getFailedRows()).isEqualTo(8);
        assertThat(response.getFailures()).extracting(failure -> failure.getErrorCode()).containsExactly(
            "CASE_1_NOT_FOUND", "COURTHOUSE_NOT_FOUND", "CASE_1_CASE_NUMBER_REQUIRED", "SELF_LINK",
            "DUPLICATE_MAPPING", "DUPLICATE_MAPPING", "INVALID_COLUMN_COUNT", "INVALID_COLUMN_COUNT");
        assertThat(response.getFailures()).extracting(failure -> failure.getRowNumber()).containsExactly(2, 3, 4, 5, 7, 8, 9, 10);
        assertThat(caseRepository.findByCaseNumberAndCourthouse("CP_MISSING", courthouse)).isEmpty();
        assertThat(linkedCaseRepository.count()).isEqualTo(2);
        assertThat(resultRepository.countByImportId(response.getImportId())).isEqualTo(10);
        assertThat(summaryService.getSummary(response.getImportId())).isEqualTo(response);
    }

    @Test
    void adminCaseLinkImportsPost_shouldLinkAndAuditWarnings_whenCasesAreDeletedOrAnonymised() throws Exception {
        var first = caseRepository.findByCaseNumberAndCourthouse("X1", courthouse).orElseThrow();
        first.setDeleted(true);
        first.setDataAnonymised(true);
        caseRepository.saveAndFlush(first);
        var second = createCase("CP1");
        second.setDeleted(true);
        second.setDataAnonymised(true);
        caseRepository.saveAndFlush(second);
        var response = post(file("X1,YORK,CP1\n"));
        assertThat(response.getSuccessfulRows()).isEqualTo(1);
        var row = resultRepository.findByImportIdAndStatusIdOrderByRowNumber(response.getImportId(), CaseLinkResultStatus.SUCCESS.getId()).getFirst();
        assertThat(row.getWarningCodes()).isEqualTo("CASE_1_DELETED,CASE_2_DELETED,CASE_1_ANONYMISED,CASE_2_ANONYMISED");
        assertThat(row.isCase1Deleted()).isTrue();
        assertThat(row.isCase2Deleted()).isTrue();
        assertThat(row.isCase1Anonymised()).isTrue();
        assertThat(row.isCase2Anonymised()).isTrue();
        assertThat(row.getLinkedCaseId()).isNotNull();
    }

    @DisplayName("""
        adminCaseLinkImportsPost()
        SHOULD roll back the case and link created by the failed row
            AND retain a failure audit without rolled-back IDs or creation flags
            AND process the next valid row successfully
        WHEN an API or unexpected exception occurs after case and link creation
        """)
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void adminCaseLinkImportsPost_shouldRollBackChangesAndAuditFailure_whenRowProcessingFails(boolean apiFailure) throws Exception {
        doAnswer(invocation -> {
            invocation.callRealMethod();
            CaseLinkImportResultEntity row = invocation.getArgument(0);
            if (FAILING_CASE.equals(row.getCase2CaseNumber())) {
                if (apiFailure) {
                    throw new DartsApiException(CaseApiError.INVALID_REQUEST);
                }
                throw new IllegalStateException("Simulated failure after case and link creation");
            }
            return null;
        }).when(rowService).linkCaseToCase(any(), any());
        var response = post(file("X1,YORK,CP_FAIL\nX2,YORK,CP_OK\n"));
        assertThat(response.getSuccessfulRows()).isEqualTo(1);
        assertThat(response.getFailedRows()).isEqualTo(1);
        assertThat(caseRepository.findByCaseNumberAndCourthouse("CP_FAIL", courthouse)).isEmpty();
        assertThat(caseRepository.findByCaseNumberAndCourthouse("CP_OK", courthouse)).isPresent();
        assertThat(linkedCaseRepository.findAll()).hasSize(1).extracting(CaseLinkedCaseEntity::getId).doesNotContainNull();
        var failure = resultRepository.findByImportIdAndStatusIdOrderByRowNumber(response.getImportId(), CaseLinkResultStatus.FAILED.getId()).getFirst();
        assertThat(failure.getErrorCode()).isEqualTo(apiFailure ? "INVALID_REQUEST" : "ROW_PROCESSING_FAILED");
        assertThat(failure.getCase2Id()).isNull();
        assertThat(failure.isCase2Created()).isFalse();
        assertThat(failure.getLinkedCaseId()).isNull();
    }

    @Test
    void adminCaseLinkImportsPost_shouldAuditRejectionBeforeProcessingRows_whenCsvIsMalformed() throws Exception {
        mockMvc.perform(multipart(ENDPOINT).file(file("X1,YORK,CP1\nX2,YORK,\"unclosed")))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value(CaseApiError.MALFORMED_CSV.getType()))
            .andExpect(jsonPath("$.title").value(CaseApiError.MALFORMED_CSV.getTitle()))
            .andExpect(jsonPath("$.error_code").value("MALFORMED_CSV"))
            .andExpect(jsonPath("$.import_id").isNumber());
        assertThat(caseRepository.findByCaseNumberAndCourthouse("CP1", courthouse)).isEmpty();
        assertThat(resultRepository.count()).isZero();
        assertThat(linkedCaseRepository.count()).isZero();
        assertThat(importRepository.findAll()).singleElement().satisfies(audit -> {
            assertThat(audit.getStatus().getId()).isEqualTo(CaseLinkImportStatus.REJECTED.getId());
            assertThat(audit.getErrorCode()).isEqualTo("MALFORMED_CSV");
            assertThat(audit.getFileChecksum()).isNotBlank();
        });
    }

    @ParameterizedTest
    @CsvSource({
        "cases.txt,text/csv,INVALID_FILE_TYPE",
        "cases.xlsx,text/csv,INVALID_FILE_TYPE",
        "cases.csv,application/json,INVALID_CONTENT_TYPE",
        "cases.csv,application/csv,INVALID_CONTENT_TYPE",
        "cases.csv,application/vnd.ms-excel,INVALID_CONTENT_TYPE",
        "cases.csv,text/plain,INVALID_CONTENT_TYPE"
    })
    void adminCaseLinkImportsPost_shouldAuditRejection_whenFileTypeIsInvalid(String filename, String contentType,
                                                                             CaseApiError error) throws Exception {
        var upload = new MockMultipartFile("file", filename, contentType, (HEADER + "X1,YORK,CP1").getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart(ENDPOINT).file(upload))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value(error.getType()))
            .andExpect(jsonPath("$.title").value(error.getTitle()))
            .andExpect(jsonPath("$.error_code").value(error.name()))
            .andExpect(jsonPath("$.error_message").value(error.getTitle()))
            .andExpect(jsonPath("$.import_id").isNumber());
        assertThat(importRepository.findAll()).singleElement().satisfies(audit -> {
            assertThat(audit.getStatus().getId()).isEqualTo(CaseLinkImportStatus.REJECTED.getId());
            assertThat(audit.getErrorCode()).isEqualTo(error.name());
            assertThat(audit.getErrorMessage()).isEqualTo(error.getTitle());
            assertThat(audit.getOriginalFilename()).isEqualTo(filename);
            assertThat(audit.getCreatedById()).isEqualTo(user.getId());
        });
        assertThat(resultRepository.count()).isZero();
        assertThat(linkedCaseRepository.count()).isZero();
        assertThat(caseRepository.findByCaseNumberAndCourthouse("CP1", courthouse)).isEmpty();
    }

    @Test
    void adminCaseLinkImportsPost_shouldAuditRejection_whenFileIsEmpty() throws Exception {
        var upload = new MockMultipartFile("file", "cases.csv", "text/csv", new byte[0]);
        mockMvc.perform(multipart(ENDPOINT).file(upload))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value(CaseApiError.EMPTY_FILE.getType()))
            .andExpect(jsonPath("$.error_code").value("EMPTY_FILE"))
            .andExpect(jsonPath("$.import_id").isNumber());
        assertThat(importRepository.findAll()).singleElement().satisfies(audit -> {
            assertThat(audit.getStatus().getId()).isEqualTo(CaseLinkImportStatus.REJECTED.getId());
            assertThat(audit.getErrorCode()).isEqualTo("EMPTY_FILE");
            assertThat(audit.getErrorMessage()).isEqualTo(CaseApiError.EMPTY_FILE.getTitle());
        });
        assertThat(resultRepository.count()).isZero();
        assertThat(linkedCaseRepository.count()).isZero();
    }

    @Test
    void adminCaseLinkImportsPost_shouldAuditRejection_whenFileExceedsSizeLimit() throws Exception {
        var upload = spy(file("X1,YORK,CP1"));
        doReturn(Long.MAX_VALUE).when(upload).getSize();
        mockMvc.perform(multipart(ENDPOINT).file(upload))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value(CaseApiError.FILE_TOO_LARGE.getType()))
            .andExpect(jsonPath("$.error_code").value("FILE_TOO_LARGE"))
            .andExpect(jsonPath("$.import_id").isNumber());
        assertThat(importRepository.findAll()).singleElement().satisfies(audit -> {
            assertThat(audit.getStatus().getId()).isEqualTo(CaseLinkImportStatus.REJECTED.getId());
            assertThat(audit.getErrorCode()).isEqualTo("FILE_TOO_LARGE");
            assertThat(audit.getErrorMessage()).isEqualTo(CaseApiError.FILE_TOO_LARGE.getTitle());
        });
        assertThat(resultRepository.count()).isZero();
        assertThat(linkedCaseRepository.count()).isZero();
        assertThat(caseRepository.findByCaseNumberAndCourthouse("CP1", courthouse)).isEmpty();
    }

    @Test
    void adminCaseLinkImportsPost_shouldAuditRejectionWithoutCreatingLinks_whenBlankRowsExceedDefaultMaximum() throws Exception {
        var upload = file("X1,YORK,CP1\n" + "\n".repeat(100_000));

        mockMvc.perform(multipart(ENDPOINT).file(upload))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value("CASE_126"))
            .andExpect(jsonPath("$.title").value(CaseApiError.TOO_MANY_ROWS.getTitle()))
            .andExpect(jsonPath("$.error_code").value("TOO_MANY_ROWS"))
            .andExpect(jsonPath("$.error_message").value(CaseApiError.TOO_MANY_ROWS.getTitle()))
            .andExpect(jsonPath("$.import_id").isNumber());

        assertThat(importRepository.findAll()).singleElement().satisfies(caseLinkImportEntity -> {
            assertThat(caseLinkImportEntity.getStatus().getId()).isEqualTo(CaseLinkImportStatus.REJECTED.getId());
            assertThat(caseLinkImportEntity.getErrorCode()).isEqualTo("TOO_MANY_ROWS");
            assertThat(caseLinkImportEntity.getErrorMessage()).isEqualTo(CaseApiError.TOO_MANY_ROWS.getTitle());
            assertThat(caseLinkImportEntity.getOriginalFilename()).isEqualTo("cases.csv");
            assertThat(caseLinkImportEntity.getCreatedById()).isEqualTo(user.getId());
            assertThat(caseLinkImportEntity.getProcessingCompletedAt()).isNotNull();
        });
        assertThat(resultRepository.count()).isZero();
        assertThat(linkedCaseRepository.count()).isZero();
        assertThat(caseRepository.findByCaseNumberAndCourthouse("CP1", courthouse)).isEmpty();
    }

    @Test
    void adminCaseLinkImportsPost_shouldAuditRejection_whenCsvHasNoDataRows() throws Exception {
        var upload = new MockMultipartFile("file", "cases.csv", "text/csv", HEADER.getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart(ENDPOINT).file(upload))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value(CaseApiError.EMPTY_FILE.getType()))
            .andExpect(jsonPath("$.title").value(CaseApiError.EMPTY_FILE.getTitle()))
            .andExpect(jsonPath("$.import_id").isNumber());
        assertThat(importRepository.findAll()).singleElement().satisfies(audit -> {
            assertThat(audit.getStatus().getId()).isEqualTo(CaseLinkImportStatus.REJECTED.getId());
            assertThat(audit.getErrorCode()).isEqualTo("EMPTY_FILE");
        });
        assertThat(resultRepository.count()).isZero();
        assertThat(linkedCaseRepository.count()).isZero();
    }

    @Test
    void adminCaseLinkImportsPost_shouldRejectRequest_whenFileIsMissing() throws Exception {
        mockMvc.perform(multipart(ENDPOINT)).andExpect(status().isBadRequest());
        assertThat(importRepository.count()).isZero();
        assertThat(resultRepository.count()).isZero();
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = "SUPER_ADMIN", mode = EnumSource.Mode.EXCLUDE)
    void adminCaseLinkImportsPost_shouldForbidAccess_whenGlobalRoleIsNotSuperAdmin(SecurityRoleEnum role) throws Exception {
        givenBuilder.anAuthenticatedUserWithGlobalAccessAndRole(role);
        mockMvc.perform(multipart(ENDPOINT).file(file("X1,YORK,CP1"))).andExpect(status().isForbidden());
        assertThat(importRepository.count()).isZero();
    }

    @Test
    void adminCaseLinkImportsPost_shouldForbidAccess_whenSuperAdminIsCourthouseScoped() throws Exception {
        givenBuilder.anAuthenticatedUserWithRoles(courthouse, SUPER_ADMIN);
        mockMvc.perform(multipart(ENDPOINT).file(file("X1,YORK,CP1"))).andExpect(status().isForbidden());
        assertThat(importRepository.count()).isZero();
    }

    private CourtCaseEntity createCase(String number) {
        return dartsPersistence.save(PersistableFactory.getCourtCaseTestData().createCaseWith(number, courthouse));
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException") // MockMvc.perform declares Exception.
    private CaseLinkImportResponse post(MockMultipartFile file) throws Exception {
        var result = mockMvc.perform(multipart(ENDPOINT).file(file)).andExpect(status().isOk()).andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsByteArray(), CaseLinkImportResponse.class);
    }

    private MockMultipartFile file(String rows) {
        return new MockMultipartFile("file", "cases.csv", "text/csv", (HEADER + rows).getBytes(StandardCharsets.UTF_8));
    }
}
