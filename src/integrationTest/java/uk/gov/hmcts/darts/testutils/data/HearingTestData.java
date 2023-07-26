package uk.gov.hmcts.darts.testutils.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;

import java.time.LocalDate;
import java.time.LocalTime;

import static uk.gov.hmcts.darts.testutils.data.CaseTestData.createCaseWithCaseNumber;
import static uk.gov.hmcts.darts.testutils.data.CaseTestData.someMinimalCase;
import static uk.gov.hmcts.darts.testutils.data.CourthouseTestData.createCourthouse;
import static uk.gov.hmcts.darts.testutils.data.CourtroomTestData.someMinimalCourtRoom;

@UtilityClass
@SuppressWarnings({"PMD.TooManyMethods", "HideUtilityClassConstructor"})
public class HearingTestData {

    public static HearingEntity someMinimalHearing() {
        var hearing = new HearingEntity();
        hearing.setCourtroom(someMinimalCourtRoom());
        hearing.setCourtCase(someMinimalCase());
        return hearing;
    }

    public static HearingEntity createHearingWith(CourtCaseEntity courtCase, CourtroomEntity courtroom, LocalDate hearingDate) {
        HearingEntity hearing1 = someMinimalHearing();
        hearing1.setCourtCase(courtCase);
        hearing1.setCourtroom(courtroom);
        hearing1.setHearingDate(hearingDate);
        return hearing1;
    }

    public static HearingEntity createHearingWith(String caseNumber, LocalTime scheduledStartTime) {
        HearingEntity hearing1 = someMinimalHearing();
        hearing1.setCourtCase(createCaseWithCaseNumber(caseNumber));
        hearing1.setCourtroom(CourtroomTestData.createCourtRoomWithNameAtCourthouse(createCourthouse("NEWCASTLE"), "1"));
        hearing1.setHearingDate(LocalDate.of(2023, 6, 20));
        hearing1.setScheduledStartTime(scheduledStartTime);
        return hearing1;
    }

    public static HearingEntity createHearingWith(CourtCaseEntity caseEntity, CourtroomEntity courtroomEntity) {
        HearingEntity hearingEntity = someMinimalHearing();
        hearingEntity.setCourtCase(caseEntity);
        hearingEntity.setCourtroom(courtroomEntity);
        hearingEntity.setHearingDate(null);
        return hearingEntity;
    }

}
