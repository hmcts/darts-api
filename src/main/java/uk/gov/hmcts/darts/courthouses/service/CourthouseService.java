package uk.gov.hmcts.darts.courthouses.service;

import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.courthouse.model.Courthouse;
import uk.gov.hmcts.darts.courthouses.exception.CourthouseCodeNotMatchException;
import uk.gov.hmcts.darts.courthouses.exception.CourthouseNameNotFoundException;

import java.util.List;

public interface CourthouseService {

    void deleteCourthouseById(Integer id);

    CourthouseEntity amendCourthouseById(Courthouse courthouse, Integer id);

    CourthouseEntity getCourtHouseById(Integer id);

    List<CourthouseEntity> getAllCourthouses();

    CourthouseEntity addCourtHouse(Courthouse courthouse);

    CourthouseEntity retrieveAndUpdateCourtHouse(Integer courthouseCode, String courthouseName)
        throws CourthouseNameNotFoundException, CourthouseCodeNotMatchException;
}
