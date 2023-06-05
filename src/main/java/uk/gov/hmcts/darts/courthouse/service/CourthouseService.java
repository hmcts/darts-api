package uk.gov.hmcts.darts.courthouse.service;

import uk.gov.hmcts.darts.common.entity.Courthouse;

import java.util.List;

public interface CourthouseService {

    void deleteCourthouseById(Integer id);

    Courthouse amendCourthouseById(uk.gov.hmcts.darts.courthouse.model.Courthouse courthouse, Integer id);

    Courthouse getCourtHouseById(Integer id);

    List<Courthouse> getAllCourthouses();

    Courthouse addCourtHouse(uk.gov.hmcts.darts.courthouse.model.Courthouse courthouse);
}
