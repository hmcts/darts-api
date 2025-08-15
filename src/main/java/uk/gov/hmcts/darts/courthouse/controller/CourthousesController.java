package uk.gov.hmcts.darts.courthouse.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.RegionEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.util.CourtValidationUtils;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseApiError;
import uk.gov.hmcts.darts.courthouse.http.api.CourthousesApi;
import uk.gov.hmcts.darts.courthouse.mapper.AdminRegionToRegionEntityMapper;
import uk.gov.hmcts.darts.courthouse.model.AdminCourthouse;
import uk.gov.hmcts.darts.courthouse.model.AdminRegion;
import uk.gov.hmcts.darts.courthouse.model.CourthousePatch;
import uk.gov.hmcts.darts.courthouse.model.CourthousePost;
import uk.gov.hmcts.darts.courthouse.model.ExtendedCourthouse;
import uk.gov.hmcts.darts.courthouse.model.ExtendedCourthousePost;
import uk.gov.hmcts.darts.courthouse.service.CourthouseService;

import java.util.List;

import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.ANY_ENTITY_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_USER;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "darts", name = "api-pod", havingValue = "true")
public class CourthousesController implements CourthousesApi {

    private final CourthouseService courthouseService;

    private final AdminRegionToRegionEntityMapper regionMapper;

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = {SUPER_ADMIN, SUPER_USER})
    public ResponseEntity<AdminCourthouse> adminCourthousesCourthouseIdGet(Integer courthouseId) {
        try {
            AdminCourthouse adminCourthouse = courthouseService.getAdminCourtHouseById(courthouseId);
            return new ResponseEntity<>(adminCourthouse, HttpStatus.OK);
        } catch (EntityNotFoundException ex) {
            throw new DartsApiException(CourthouseApiError.COURTHOUSE_NOT_FOUND, ex);
        }
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = {SUPER_ADMIN, SUPER_USER})
    public ResponseEntity<List<AdminRegion>> adminRegionsGet() {
        List<RegionEntity> regionsEntities = courthouseService.getAdminAllRegions();
        List<AdminRegion> adminRegions = regionMapper.mapFromEntityToAdminRegion(regionsEntities);
        return new ResponseEntity<>(adminRegions, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<ExtendedCourthouse>> courthousesGet() {
        List<CourthouseEntity> courthouseEntities = courthouseService.getAllCourthouses();
        List<ExtendedCourthouse> responseEntities = courthouseService.mapFromEntitiesToExtendedCourthouses(courthouseEntities);

        return new ResponseEntity<>(responseEntities, HttpStatus.OK);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = {SUPER_ADMIN})
    public ResponseEntity<ExtendedCourthousePost> adminCourthousesPost(CourthousePost courthousePost) {
        validateUppercase(courthousePost.getCourthouseName());
        ExtendedCourthousePost extendedCourthouse = courthouseService.createCourthouseAndGroups(courthousePost);
        return new ResponseEntity<>(extendedCourthouse, HttpStatus.CREATED);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = {SUPER_ADMIN})
    public ResponseEntity<AdminCourthouse> updateCourthouse(Integer courthouseId, CourthousePatch courthousePatch) {
        validateUppercase(courthousePatch.getCourthouseName());
        var adminCourthouse = courthouseService.updateCourthouse(courthouseId, courthousePatch);
        return new ResponseEntity<>(adminCourthouse, HttpStatus.OK);
    }

    private void validateUppercase(String courthouse) {
        if (!CourtValidationUtils.isUppercase(courthouse)) {
            throw new DartsApiException(CourthouseApiError.INVALID_REQUEST, "Courthouse must be uppercase.");
        }
    }

}
