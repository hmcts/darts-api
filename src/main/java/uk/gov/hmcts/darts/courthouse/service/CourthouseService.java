package uk.gov.hmcts.darts.courthouse.service;

import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.RegionEntity;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseCodeNotMatchException;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseNameNotFoundException;
import uk.gov.hmcts.darts.courthouse.model.AdminCourthouse;
import uk.gov.hmcts.darts.courthouse.model.CourthousePatch;
import uk.gov.hmcts.darts.courthouse.model.CourthousePost;
import uk.gov.hmcts.darts.courthouse.model.ExtendedCourthouse;
import uk.gov.hmcts.darts.courthouse.model.ExtendedCourthousePost;

import java.util.List;

public interface CourthouseService {

    CourthouseEntity getCourtHouseById(Integer id);

    List<CourthouseEntity> getAllCourthouses();

    ExtendedCourthousePost createCourthouseAndGroups(CourthousePost courthousePost);

    CourthouseEntity retrieveAndUpdateCourtHouse(Integer courthouseCode, String courthouseName)
        throws CourthouseNameNotFoundException, CourthouseCodeNotMatchException;

    AdminCourthouse getAdminCourtHouseById(Integer id);

    List<RegionEntity> getAdminAllRegions();

    List<ExtendedCourthouse> mapFromEntitiesToExtendedCourthouses(List<CourthouseEntity> courthouseEntities);

    AdminCourthouse updateCourthouse(Integer courthouseId, CourthousePatch courthousePatch);

}
