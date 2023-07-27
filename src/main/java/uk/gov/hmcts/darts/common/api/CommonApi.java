package uk.gov.hmcts.darts.common.api;

import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;

import java.time.LocalDate;

public interface CommonApi {

    CourtroomEntity retrieveOrCreateCourtroom(String courthouseName, String courtroomName);

    CourtroomEntity retrieveOrCreateCourtroom(CourthouseEntity courthouse, String courtroomName);

    HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber, LocalDate hearingDate);

    CourtCaseEntity retrieveOrCreateCase(String courthouseName, String caseNumber);

    CourthouseEntity retrieveCourthouse(String courthouseName);
}
