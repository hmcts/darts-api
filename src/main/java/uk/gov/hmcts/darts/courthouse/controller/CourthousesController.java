package uk.gov.hmcts.darts.courthouse.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.courthouse.http.api.CourthousesApi;
import uk.gov.hmcts.darts.courthouse.mapper.CourthouseToCourthouseEntityMapper;
import uk.gov.hmcts.darts.courthouse.model.Courthouse;
import uk.gov.hmcts.darts.courthouse.model.ExtendedCourthouse;
import uk.gov.hmcts.darts.courthouse.service.CourthouseService;

import java.util.List;
import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class CourthousesController implements CourthousesApi {

    private final CourthouseService courthouseService;

    private final CourthouseToCourthouseEntityMapper mapper;

    @Override
    public ResponseEntity<Void> courthousesCourthouseIdDelete(
        @Parameter(name = "courthouse_id", description = "", required = true, in = ParameterIn.PATH) @PathVariable("courthouse_id") Integer courthouseId
    ) {
        courthouseService.deleteCourthouseById(courthouseId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);

    }

    @Override
    public ResponseEntity<ExtendedCourthouse> courthousesCourthouseIdGet(
        @Parameter(name = "courthouse_id", description = "", required = true, in = ParameterIn.PATH) @PathVariable("courthouse_id") Integer courthouseId
    ) {
        try {
            CourthouseEntity courtHouseEntity = courthouseService.getCourtHouseById(
                courthouseId);
            ExtendedCourthouse responseEntity = mapper.mapFromEntityToExtendedCourthouse(courtHouseEntity);
            return new ResponseEntity<>(responseEntity, HttpStatus.OK);
        } catch (EntityNotFoundException | JpaObjectRetrievalFailureException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
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
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }


    }

    @Override
    public ResponseEntity<List<ExtendedCourthouse>> courthousesGet(

    ) {
        List<CourthouseEntity> courtHouseEntities = courthouseService.getAllCourthouses();
        List<ExtendedCourthouse> responseEntities = mapper.mapFromListEntityToListExtendedCourthouse(courtHouseEntities);
        return new ResponseEntity<>(responseEntities, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ExtendedCourthouse> courthousesPost(
        @Parameter(name = "Courthouse", description = "", required = true) @Valid @RequestBody Courthouse courthouse
    ) {
        CourthouseEntity addedCourtHouse = courthouseService.addCourtHouse(courthouse);
        ExtendedCourthouse extendedCourthouse = mapper.mapFromEntityToExtendedCourthouse(addedCourtHouse);
        return new ResponseEntity<>(extendedCourthouse, HttpStatus.CREATED);
    }
}
