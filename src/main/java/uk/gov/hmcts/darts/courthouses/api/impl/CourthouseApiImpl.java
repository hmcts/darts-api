package uk.gov.hmcts.darts.courthouses.api.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.courthouses.api.CourthouseApi;
import uk.gov.hmcts.darts.courthouses.exception.CourthouseCodeNotMatchException;
import uk.gov.hmcts.darts.courthouses.exception.CourthouseNameNotFoundException;
import uk.gov.hmcts.darts.courthouses.service.CourthouseService;

@RequiredArgsConstructor
@Service
public class CourthouseApiImpl implements CourthouseApi {

    private final CourthouseService courthouseService;

    @Override
    public CourthouseEntity retrieveAndUpdateCourtHouse(Integer courthouseCode, String courthouseName)
        throws CourthouseNameNotFoundException, CourthouseCodeNotMatchException {
        return courthouseService.retrieveAndUpdateCourtHouse(courthouseCode, courthouseName);
    }
}
