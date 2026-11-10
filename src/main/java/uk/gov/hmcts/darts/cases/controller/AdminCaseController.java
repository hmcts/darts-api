package uk.gov.hmcts.darts.cases.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.cases.http.api.CaseLinkImportsApi;
import uk.gov.hmcts.darts.cases.model.CaseLinkImportFailure;
import uk.gov.hmcts.darts.cases.model.CaseLinkImportResponse;
import uk.gov.hmcts.darts.cases.service.CaseLinkImportService;
import uk.gov.hmcts.darts.cases.service.CaseLinkImportSummaryService;
import uk.gov.hmcts.darts.util.pagination.PaginationDto;

import java.util.List;

import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.ANY_ENTITY_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "darts", name = "api-pod", havingValue = "true")
public class AdminCaseController implements CaseLinkImportsApi {

    private final CaseLinkImportService caseLinkImportService;
    private final CaseLinkImportSummaryService caseLinkImportSummaryService;

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = {SUPER_ADMIN})
    public ResponseEntity<CaseLinkImportResponse> adminCaseLinkImportsPost(MultipartFile file) {
        return new ResponseEntity<>(caseLinkImportService.importCaseLinkingFile(file), HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = {SUPER_ADMIN})
    public ResponseEntity<CaseLinkImportResponse> adminCaseLinkImportFailuresGet(Long importId, Integer pageNumber, Integer pageSize) {
        PaginationDto<CaseLinkImportFailure> paginationDto = new PaginationDto<>(pageNumber, pageSize, List.of(), List.of());
        return new ResponseEntity<>(caseLinkImportSummaryService.getSummary(importId, paginationDto), HttpStatus.OK);
    }

}
