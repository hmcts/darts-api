package uk.gov.hmcts.darts.courthouse.service;

import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.courthouse.model.AdminCourthouse;
import uk.gov.hmcts.darts.courthouse.model.CourthousePatch;

public interface CourthouseUpdateMapper {

    CourthouseEntity mapPatchToEntity(CourthousePatch courthousePatch, CourthouseEntity courthouseEntity);

    AdminCourthouse mapEntityToAdminCourthouse(CourthouseEntity patchedCourthouse);

}
