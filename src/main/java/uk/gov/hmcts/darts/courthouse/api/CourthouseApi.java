package uk.gov.hmcts.darts.courthouse.api;

import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseCodeNotMatchException;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseNameNotFoundException;

public interface CourthouseApi {
    CourthouseEntity retrieveAndUpdateCourtHouse(Integer courthouseCode, String courthouseName)
        throws CourthouseNameNotFoundException, CourthouseCodeNotMatchException;
}
