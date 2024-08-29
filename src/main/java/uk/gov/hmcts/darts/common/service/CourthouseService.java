package uk.gov.hmcts.darts.common.service;

import uk.gov.hmcts.darts.common.entity.CourthouseEntity;

public interface CourthouseService {

    CourthouseEntity retrieveCourthouse(String courthouseName);

}
