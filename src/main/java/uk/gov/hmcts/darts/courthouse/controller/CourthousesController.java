package uk.gov.hmcts.darts.courthouse.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.authorisation.annotation.Authorisation;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.RegionEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseApiError;
import uk.gov.hmcts.darts.courthouse.http.api.CourthousesApi;
import uk.gov.hmcts.darts.courthouse.mapper.AdminRegionToRegionEntityMapper;
import uk.gov.hmcts.darts.courthouse.mapper.CourthouseToCourthouseEntityMapper;
import uk.gov.hmcts.darts.courthouse.model.AdminCourthouse;
import uk.gov.hmcts.darts.courthouse.model.AdminRegion;
import uk.gov.hmcts.darts.courthouse.model.Courthouse;
import uk.gov.hmcts.darts.courthouse.model.ExtendedCourthouse;
import uk.gov.hmcts.darts.courthouse.service.CourthouseService;

import java.util.List;
import javax.validation.Valid;

import static uk.gov.hmcts.darts.authorisation.constants.AuthorisationConstants.SECURITY_SCHEMES_BEARER_AUTH;
import static uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum.ANY_ENTITY_ID;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.ADMIN;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "darts", name = "api-pod", havingValue = "true")
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class CourthousesController implements CourthousesApi {

    private final CourthouseService courthouseService;

    private final CourthouseToCourthouseEntityMapper courthouseMapper;

    private final AdminRegionToRegionEntityMapper regionMapper;

    @Override
    public ResponseEntity<Void> courthousesCourthouseIdDelete(
        @Parameter(name = "courthouse_id", description = "", required = true, in = ParameterIn.PATH) @PathVariable("courthouse_id") Integer courthouseId
    ) {
        courthouseService.deleteCourthouseById(courthouseId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = ADMIN)
    public ResponseEntity<AdminCourthouse> adminCourthousesCourthouseIdGet(
        @Parameter(name = "courthouse_id", description = "", required = true, in = ParameterIn.PATH) @PathVariable("courthouse_id") Integer courthouseId
    ) {
        try {
            AdminCourthouse adminCourthouse = courthouseService.getAdminCourtHouseById(courthouseId);
            return new ResponseEntity<>(adminCourthouse, HttpStatus.OK);
        } catch (EntityNotFoundException e) {
            throw new DartsApiException(CourthouseApiError.COURTHOUSE_NOT_FOUND);
        }
    }

    @Override
    @SecurityRequirement(name = SECURITY_SCHEMES_BEARER_AUTH)
    @Authorisation(contextId = ANY_ENTITY_ID, globalAccessSecurityRoles = ADMIN)
    public ResponseEntity<List<AdminRegion>> adminRegionsGet() {
            List<RegionEntity> regionsEntities = courthouseService.getAdminAllRegions();
            List<AdminRegion> adminRegions = regionMapper.mapFromEntityToAdminRegion(regionsEntities);
            return new ResponseEntity<>(adminRegions, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> courthousesCourthouseIdPut(
        @Parameter(name = "courthouse_id", description = "", required = true, in = ParameterIn.PATH) @PathVariable("courthouse_id") Integer courthouseId,
        @Parameter(name = "Courthouse", description = "", required = true) @Valid @RequestBody Courthouse courthouse
    ) {
        try {
            courthouseService.amendCourthouseById(courthouse, courthouseId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (EntityNotFoundException exception) {
            throw new DartsApiException(CourthouseApiError.COURTHOUSE_NOT_FOUND);
        }


    }

    @Override
    public ResponseEntity<List<ExtendedCourthouse>> courthousesGet(

    ) {

        List<CourthouseEntity> courthouseEntities = courthouseService.getAllCourthouses();
        List<ExtendedCourthouse> responseEntities = courthouseService.mapFromEntitiesToExtendedCourthouses(courthouseEntities);

        return new ResponseEntity<>(responseEntities, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ExtendedCourthouse> courthousesPost(
        @Parameter(name = "Courthouse", description = "", required = true) @Valid @RequestBody Courthouse courthouse
    ) {
        CourthouseEntity addedCourtHouse = courthouseService.addCourtHouse(courthouse);
        ExtendedCourthouse extendedCourthouse = courthouseMapper.mapFromEntityToExtendedCourthouse(addedCourtHouse);
        return new ResponseEntity<>(extendedCourthouse, HttpStatus.CREATED);
    }
}
