package uk.gov.hmcts.darts.courthouse.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.courthouse.api.CourthousesApi;
import uk.gov.hmcts.darts.courthouse.model.CourtHouse;
import uk.gov.hmcts.darts.courthouse.model.ExtendedCourtHouse;

import java.util.List;

@RestController
public class CourthousesController implements CourthousesApi {
    @Override
    public ResponseEntity<Void> courthousesCourthouseIdDelete(Integer courthouseId) {
        return CourthousesApi.super.courthousesCourthouseIdDelete(courthouseId);
    }

    @Override
    public ResponseEntity<List<ExtendedCourtHouse>> courthousesCourthouseIdPut(Integer courthouseId, CourtHouse courtHouse) {
        return CourthousesApi.super.courthousesCourthouseIdPut(courthouseId, courtHouse);
    }

    @Override
    public ResponseEntity<List<ExtendedCourtHouse>> courthousesGet() {
        return CourthousesApi.super.courthousesGet();
    }

    @Override
    public ResponseEntity<Void> courthousesPost(CourtHouse courtHouse) {
        return CourthousesApi.super.courthousesPost(courtHouse);
    }
}
