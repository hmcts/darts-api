package uk.gov.hmcts.darts.courthouse.service;

import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseCodeNotMatchException;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseNameNotFoundException;
import uk.gov.hmcts.darts.courthouse.model.Courthouse;

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
