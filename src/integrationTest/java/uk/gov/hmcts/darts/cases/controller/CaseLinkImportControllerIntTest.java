package uk.gov.hmcts.darts.cases.controller;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.cases.enums.CaseLinkImportStatus;
import uk.gov.hmcts.darts.cases.enums.CaseLinkResultStatus;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;

@AutoConfigureMockMvc
class CaseLinkImportControllerIntTest extends PostgresIntegrationBase {

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
        var entity = PersistableFactory.getCourthouseTestData().someMinimal();
        entity.setCourthouseName("YORK");
        courthouse = dartsPersistence.save(entity);
        user = givenBuilder.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        createCase("X1");
        createCase("X2");
    }

    @Test
    void given_validRows_when_importing_then_createMissingCaseAndReuseItForAnotherMapping() throws Exception {
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
        assertThat(audit.getFileChecksum()).isEqualTo(DigestUtils.sha256Hex(upload.getBytes()));
        assertThat(audit.getCreatedById()).isEqualTo(user.getId());
        assertThat(audit.getCreatedDateTime()).isNotNull();
        assertThat(audit.getProcessingStartedAt()).isNotNull();
        assertThat(audit.getProcessingCompletedAt()).isAfterOrEqualTo(audit.getProcessingStartedAt());
        assertThat(summaryService.getSummary(response.getImportId())).isEqualTo(response);
    }

    @Test
    void given_existingLinkInEitherOrder_when_importingAgain_then_returnIdempotentSuccess() throws Exception {
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

    @Test
    @SuppressWarnings("PMD.DoNotUseThreads") // Exercise competing import transactions against PostgreSQL.
    void given_concurrentImportsOfSameMapping_when_processing_then_createOneCaseAndOneLink() throws Exception {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var start = new CountDownLatch(1);
        Callable<CaseLinkImportResponse> upload = () -> {
            SecurityContextHolder.getContext().setAuthentication(authentication);
            try {
                assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                return importService.importFile(file("X1,YORK,CP_CONCURRENT"), 1);
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

    @Test
    void given_invalidDuplicateReversedAndSelfRows_when_importing_then_auditFailuresAndContinue() throws Exception {
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
    void given_deletedAndAnonymisedCases_when_importing_then_linkAndAuditWarnings() throws Exception {
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

    @Test
    void given_failureAfterCreatingCaseAndLink_when_importing_then_rollbackBothAndRetainFailureAudit() throws Exception {
        doAnswer(invocation -> {
            invocation.callRealMethod();
            CaseLinkImportResultEntity row = invocation.getArgument(0);
            if (FAILING_CASE.equals(row.getCase2CaseNumber())) {
                throw new IllegalStateException("Simulated failure after case and link creation");
            }
            return null;
        }).when(rowService).process(any(), any());
        var response = post(file("X1,YORK,CP_FAIL\nX2,YORK,CP_OK\n"));
        assertThat(response.getSuccessfulRows()).isEqualTo(1);
        assertThat(response.getFailedRows()).isEqualTo(1);
        assertThat(caseRepository.findByCaseNumberAndCourthouse("CP_FAIL", courthouse)).isEmpty();
        assertThat(caseRepository.findByCaseNumberAndCourthouse("CP_OK", courthouse)).isPresent();
        assertThat(linkedCaseRepository.findAll()).hasSize(1).extracting(CaseLinkedCaseEntity::getId).doesNotContainNull();
        var failure = resultRepository.findByImportIdAndStatusIdOrderByRowNumber(response.getImportId(), CaseLinkResultStatus.FAILED.getId()).getFirst();
        assertThat(failure.getErrorCode()).isEqualTo("ROW_PROCESSING_FAILED");
        assertThat(failure.getCase2Id()).isNull();
        assertThat(failure.isCase2Created()).isFalse();
        assertThat(failure.getLinkedCaseId()).isNull();
    }

    @Test
    void given_malformedFileAfterValidRow_when_importing_then_rejectBeforeProcessingAndAuditImport() throws Exception {
        mockMvc.perform(multipart(ENDPOINT).file(file("X1,YORK,CP1\nX2,YORK,\"unclosed")))
            .andExpect(status().isUnprocessableEntity());
        assertThat(caseRepository.findByCaseNumberAndCourthouse("CP1", courthouse)).isEmpty();
        assertThat(resultRepository.count()).isZero();
        assertThat(linkedCaseRepository.count()).isZero();
        assertThat(importRepository.findAll()).singleElement().satisfies(audit -> {
            assertThat(audit.getStatus().getId()).isEqualTo(CaseLinkImportStatus.REJECTED.getId());
            assertThat(audit.getErrorCode()).isEqualTo("MALFORMED_CSV");
            assertThat(audit.getFileChecksum()).isNotBlank();
        });
    }

    @Test
    void given_multipleFiles_when_importing_then_rejectAndAuditImport() throws Exception {
        mockMvc.perform(multipart(ENDPOINT).file(file("X1,YORK,CP1")).file(file("X2,YORK,CP2")))
            .andExpect(status().isUnprocessableEntity());
        assertThat(importRepository.findAll()).singleElement().extracting(audit -> audit.getErrorCode()).isEqualTo("INVALID_FILE_COUNT");
        assertThat(resultRepository.count()).isZero();
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = "SUPER_ADMIN", mode = EnumSource.Mode.EXCLUDE)
    void given_otherGlobalRole_when_importing_then_forbidAccess(SecurityRoleEnum role) throws Exception {
        givenBuilder.anAuthenticatedUserWithGlobalAccessAndRole(role);
        mockMvc.perform(multipart(ENDPOINT).file(file("X1,YORK,CP1"))).andExpect(status().isForbidden());
        assertThat(importRepository.count()).isZero();
    }

    @Test
    void given_courthouseScopedSuperAdmin_when_importing_then_forbidAccess() throws Exception {
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
