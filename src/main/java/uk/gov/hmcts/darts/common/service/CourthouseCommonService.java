package uk.gov.hmcts.darts.common.service;

import uk.gov.hmcts.darts.common.entity.CourthouseEntity;

@FunctionalInterface
public interface CourthouseCommonService {

    CourthouseEntity retrieveCourthouse(String courthouseName);

}
