package uk.gov.hmcts.darts.cases.controller;

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
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.cases.enums.CaseLinkResultStatus;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.cases.model.CaseLinkImportResponse;
import uk.gov.hmcts.darts.cases.service.CaseLinkImportAuditService;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultEntity;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultStatusEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportRepository;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportResultRepository;
import uk.gov.hmcts.darts.common.repository.CaseLinkedCaseRepository;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;

@AutoConfigureMockMvc
class CaseLinkImportPaginationIntTest extends PostgresIntegrationBase {

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
    private CaseLinkImportAuditService auditService;

    private CourthouseEntity courthouse;
    private UserAccountEntity user;

    @BeforeEach
    void setUp() {
        CourthouseEntity courthouseEntity = PersistableFactory.getCourthouseTestData().someMinimal();
        courthouseEntity.setCourthouseName("YORK");
        courthouse = dartsPersistence.save(courthouseEntity);
        user = givenBuilder.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);
        dartsPersistence.save(PersistableFactory.getCourtCaseTestData().createCaseWith("X1", courthouse));
    }

    @DisplayName("""
        adminCaseLinkImportsPost()
        SHOULD return the first 100 failures and pagination metadata
            AND retain whole-import success and failure counts
            AND allow retrieval of the remaining failures without reprocessing the import
        WHEN the upload contains more failed rows than fit on one page and a successful row
        """)
    @Test
    void adminCaseLinkImportsPost_shouldPaginateFailures_whenFailuresExceedDefaultPageSize() throws Exception {
        String failedRows = IntStream.range(0, 105).mapToObj(index -> "MISSING" + index + ",YORK,CP" + index + "\n")
            .collect(Collectors.joining());
        var firstPage = post(file(failedRows + "X1,YORK,CP_VALID\n"));

        assertThat(firstPage.getTotalRows()).isEqualTo(106);
        assertThat(firstPage.getSuccessfulRows()).isEqualTo(1);
        assertThat(firstPage.getFailedRows()).isEqualTo(105);
        assertThat(firstPage.getCurrentPage()).isEqualTo(1);
        assertThat(firstPage.getPageSize()).isEqualTo(100);
        assertThat(firstPage.getTotalItems()).isEqualTo(105);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getFailures()).hasSize(100);
        assertThat(firstPage.getFailures().getFirst().getRowNumber()).isEqualTo(2);
        assertThat(firstPage.getFailures().getLast().getRowNumber()).isEqualTo(101);
        mockMvc.perform(get(ENDPOINT + "/{import_id}/failures", firstPage.getImportId()).param("page_number", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total_rows").value(106))
            .andExpect(jsonPath("$.successful_rows").value(1))
            .andExpect(jsonPath("$.failed_rows").value(105))
            .andExpect(jsonPath("$.current_page").value(2))
            .andExpect(jsonPath("$.page_size").value(100))
            .andExpect(jsonPath("$.total_items").value(105))
            .andExpect(jsonPath("$.total_pages").value(2))
            .andExpect(jsonPath("$.failures.length()").value(5))
            .andExpect(jsonPath("$.failures[0].row_number").value(102))
            .andExpect(jsonPath("$.failures[4].row_number").value(106));
        assertThat(importRepository.count()).isEqualTo(1);
        assertThat(resultRepository.count()).isEqualTo(106);
        assertThat(linkedCaseRepository.count()).isEqualTo(1);
    }

    @Test
    void adminCaseLinkImportFailuresGet_shouldReturnFailuresBeyondOneThousand_whenImportSpansManyPages() throws Exception {
        Long importId = createFailedImport(1205);

        mockMvc.perform(get(ENDPOINT + "/{import_id}/failures", importId).param("page_number", "13"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED_WITH_ERRORS"))
            .andExpect(jsonPath("$.failed_rows").value(1205))
            .andExpect(jsonPath("$.total_items").value(1205))
            .andExpect(jsonPath("$.total_pages").value(13))
            .andExpect(jsonPath("$.failures.length()").value(5))
            .andExpect(jsonPath("$.failures[0].row_number").value(1202))
            .andExpect(jsonPath("$.failures[4].row_number").value(1206));
        assertThat(importRepository.count()).isEqualTo(1);
        assertThat(resultRepository.count()).isEqualTo(1205);
        assertThat(linkedCaseRepository.count()).isZero();
    }

    @Test
    void adminCaseLinkImportFailuresGet_shouldUseDefaultPageNumberAndRequestedPageSize_whenPageNumberIsOmitted() throws Exception {
        Long importId = createFailedImport(26);

        mockMvc.perform(get(ENDPOINT + "/{import_id}/failures", importId).param("page_size", "25"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.current_page").value(1))
            .andExpect(jsonPath("$.page_size").value(25))
            .andExpect(jsonPath("$.total_pages").value(2))
            .andExpect(jsonPath("$.failures.length()").value(25));
        assertThat(resultRepository.count()).isEqualTo(26);
    }

    @ParameterizedTest
    @ValueSource(ints = {2, Integer.MAX_VALUE})
    void adminCaseLinkImportFailuresGet_shouldReturnEmptyPageWithCounts_whenPageIsBeyondLastPage(int pageNumber) throws Exception {
        Long importId = createFailedImport(1);

        mockMvc.perform(get(ENDPOINT + "/{import_id}/failures", importId).param("page_number", Integer.toString(pageNumber)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.current_page").value(pageNumber))
            .andExpect(jsonPath("$.failed_rows").value(1))
            .andExpect(jsonPath("$.total_items").value(1))
            .andExpect(jsonPath("$.total_pages").value(1))
            .andExpect(jsonPath("$.failures").isEmpty());
        assertThat(resultRepository.count()).isEqualTo(1);
    }

    @Test
    void adminCaseLinkImportFailuresGet_shouldReturnNotFound_whenImportDoesNotExist() throws Exception {
        mockMvc.perform(get(ENDPOINT + "/{import_id}/failures", Long.MAX_VALUE))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type").value(CaseApiError.CASE_LINK_IMPORT_NOT_FOUND.getType()))
            .andExpect(jsonPath("$.title").value(CaseApiError.CASE_LINK_IMPORT_NOT_FOUND.getTitle()));
        assertThat(importRepository.count()).isZero();
    }

    @ParameterizedTest
    @CsvSource({"page_number,0", "page_number,-1", "page_number,invalid", "page_size,0", "page_size,-1", "page_size,invalid"})
    void adminCaseLinkImportFailuresGet_shouldRejectRequest_whenPaginationParameterIsInvalid(String parameter, String value) throws Exception {
        mockMvc.perform(get(ENDPOINT + "/1/failures").param(parameter, value)).andExpect(status().isBadRequest());
        assertThat(importRepository.count()).isZero();
    }

    @Test
    void adminCaseLinkImportFailuresGet_shouldRejectRequest_whenPageSizeExceedsConfiguredMaximum() throws Exception {
        mockMvc.perform(get(ENDPOINT + "/1/failures").param("page_size", "101"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value(CaseApiError.INVALID_REQUEST.getType()));
        assertThat(importRepository.count()).isZero();
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = "SUPER_ADMIN", mode = EnumSource.Mode.EXCLUDE)
    void adminCaseLinkImportFailuresGet_shouldForbidAccess_whenGlobalRoleIsNotSuperAdmin(SecurityRoleEnum role) throws Exception {
        givenBuilder.anAuthenticatedUserWithGlobalAccessAndRole(role);

        mockMvc.perform(get(ENDPOINT + "/1/failures")).andExpect(status().isForbidden());

        assertThat(importRepository.count()).isZero();
    }

    @Test
    void adminCaseLinkImportFailuresGet_shouldForbidAccess_whenSuperAdminIsCourthouseScoped() throws Exception {
        givenBuilder.anAuthenticatedUserWithRoles(courthouse, SUPER_ADMIN);

        mockMvc.perform(get(ENDPOINT + "/1/failures")).andExpect(status().isForbidden());

        assertThat(importRepository.count()).isZero();
    }

    private Long createFailedImport(int failureCount) {
        var caseLinkImportEntity = auditService.createCaseLinkImport("cases.csv", user);
        List<CaseLinkImportResultEntity> failedRows = IntStream.range(0, failureCount).mapToObj(index -> {
            CaseLinkImportResultEntity failedRow = new CaseLinkImportResultEntity();
            failedRow.setRowNumber(index + 2);
            failedRow.setColumnCount(3);
            failedRow.setCase1CaseNumber("MISSING" + index);
            failedRow.setCase1Courthouse("YORK");
            failedRow.setCase2CaseNumber("CP" + index);
            failedRow.setCase2Courthouse("YORK");
            failedRow.setErrorCode("CASE_1_NOT_FOUND");
            failedRow.setErrorMessage(CaseApiError.CASE_1_NOT_FOUND.getTitle());
            failedRow.setStatus(new CaseLinkImportResultStatusEntity(CaseLinkResultStatus.FAILED.getId()));
            return failedRow;
        }).toList();
        auditService.updateCaseLinkImport(caseLinkImportEntity.getId(), failedRows, user);
        auditService.setCompletion(caseLinkImportEntity.getId());
        return caseLinkImportEntity.getId();
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException") // MockMvc.perform declares Exception.
    private CaseLinkImportResponse post(MockMultipartFile uploadedFile) throws Exception {
        var result = mockMvc.perform(multipart(ENDPOINT).file(uploadedFile)).andExpect(status().isOk()).andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsByteArray(), CaseLinkImportResponse.class);
    }

    private MockMultipartFile file(String rows) {
        return new MockMultipartFile("file", "cases.csv", "text/csv", (HEADER + rows).getBytes(StandardCharsets.UTF_8));
    }
}
