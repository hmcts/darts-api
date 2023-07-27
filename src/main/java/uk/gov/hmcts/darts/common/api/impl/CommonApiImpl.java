package uk.gov.hmcts.darts.common.api.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.api.CommonApi;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommonApiImpl implements CommonApi {

    private final RetrieveCoreObjectService retrieveCoreObjectService;

    @Override
    public CourtroomEntity retrieveOrCreateCourtroom(String courthouseName, String courtroomName) {
        return retrieveCoreObjectService.retrieveOrCreateCourtroom(courthouseName, courtroomName);
    }

    @Override
    public CourtroomEntity retrieveOrCreateCourtroom(CourthouseEntity courthouse, String courtroomName) {
        return retrieveCoreObjectService.retrieveOrCreateCourtroom(courthouse, courtroomName);
    }

    @Override
    public HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber, LocalDate hearingDate) {
        return retrieveCoreObjectService.retrieveOrCreateHearing(
            courthouseName,
            courtroomName,
            caseNumber,
            hearingDate
        );
    }


    @Override
    public CourtCaseEntity retrieveOrCreateCase(String courthouseName, String caseNumber) {
        return retrieveCoreObjectService.retrieveOrCreateCase(courthouseName, caseNumber);
    }

    @Override
    public CourthouseEntity retrieveCourthouse(String courthouseName) {
        return retrieveCoreObjectService.retrieveCourthouse(courthouseName);
    }


}
