package uk.gov.hmcts.darts.common.service;

import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;

import java.time.LocalDate;

public interface RetrieveCoreObjectService {

    HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber, LocalDate hearingDate);

    CourtroomEntity retrieveOrCreateCourtroom(CourthouseEntity courthouse, String courtroomName);

    CourtroomEntity retrieveOrCreateCourtroom(String courthouseName, String courtroomName);

    CourtCaseEntity retrieveOrCreateCase(String courthouseName, String caseNumber);

    CourthouseEntity retrieveCourthouse(String courthouseName);
}
