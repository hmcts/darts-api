package uk.gov.hmcts.darts.courthouses.api;

import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.courthouses.exception.CourthouseCodeNotMatchException;
import uk.gov.hmcts.darts.courthouses.exception.CourthouseNameNotFoundException;

public interface CourthouseApi {
    CourthouseEntity retrieveAndUpdateCourtHouse(Integer courthouseCode, String courthouseName)
        throws CourthouseNameNotFoundException, CourthouseCodeNotMatchException;
}
