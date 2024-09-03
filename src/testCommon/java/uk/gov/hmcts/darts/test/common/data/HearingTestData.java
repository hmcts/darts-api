package uk.gov.hmcts.darts.test.common.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

import static uk.gov.hmcts.darts.test.common.data.CaseTestData.caseWithCaseNumber;
import static uk.gov.hmcts.darts.test.common.data.CaseTestData.createCaseWithCaseNumber;
import static uk.gov.hmcts.darts.test.common.data.CaseTestData.createSomeMinimalCase;
import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.createCourthouseWithName;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.createCourtRoomWithNameAtCourthouse;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.someMinimalCourtRoom;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class HearingTestData {

    private static final LocalDate HEARING_DATE = LocalDate.of(2023, 6, 20);

    public static HearingEntity someMinimalHearing() {
        var minimalCase = createSomeMinimalCase();
        var minimalCourtRoom = someMinimalCourtRoom();
        minimalCourtRoom.setCourthouse(minimalCase.getCourthouse());

        var hearingEntity = new HearingEntity();
        hearingEntity.setCourtCase(minimalCase);
        hearingEntity.setCourtroom(minimalCourtRoom);
        hearingEntity.setHearingIsActual(true);
        hearingEntity.setHearingDate(LocalDate.now().plusWeeks(1));
        var userAccount = minimalUserAccount();
        hearingEntity.setCreatedBy(userAccount);
        hearingEntity.setLastModifiedBy(userAccount);
        return hearingEntity;
    }

    public static HearingEntity createHearingFor(CourtCaseEntity courtCase) {
        var minimalHearing = someMinimalHearing();
        minimalHearing.setCourtCase(courtCase);
        return minimalHearing;
    }

    public static HearingEntity createHearingWith(CourtCaseEntity courtCase, CourtroomEntity courtroom, LocalDate hearingDate) {
        var hearing = createHearingFor(courtCase);
        hearing.setCourtroom(courtroom);
        hearing.setHearingDate(hearingDate);
        return hearing;
    }

    public static HearingEntity createHearingWith(String caseNumber, LocalTime scheduledStartTime) {
        HearingEntity hearing1 = createHearingWithDefaults(
            createCaseWithCaseNumber(caseNumber),
            createCourtRoomWithNameAtCourthouse(
                createCourthouseWithName("NEWCASTLE"),
                "1"
            ),
            HEARING_DATE,
            null
        );
        hearing1.setScheduledStartTime(scheduledStartTime);
        return hearing1;
    }

    public static HearingEntity createHearingWith(CourtCaseEntity caseEntity, CourtroomEntity courtroomEntity) {
        HearingEntity hearingEntity = someMinimalHearing();
        hearingEntity.setCourtCase(caseEntity);
        hearingEntity.setCourtroom(courtroomEntity);
        hearingEntity.setHearingDate(HEARING_DATE);
        return hearingEntity;
    }

    public static HearingEntity createHearingWithDefaults(CourtCaseEntity courtCase, CourtroomEntity courtroom, LocalDate hearingDate, JudgeEntity judge) {
        return createHearingWithDefaults(courtCase, courtroom, hearingDate, judge, true);
    }


    public static HearingEntity createHearingWithDefaults(CourtCaseEntity courtCase, CourtroomEntity courtroom, LocalDate hearingDate, JudgeEntity judge,
                                                          boolean isHearingActual) {
        HearingEntity hearing = someMinimalHearing();
        hearing.setCourtCase(Objects.requireNonNullElseGet(courtCase, CaseTestData::someMinimalCase));

        hearing.setCourtroom(Objects.requireNonNullElseGet(courtroom, CourtroomTestData::someMinimalCourtRoom));

        hearing.setHearingDate(Objects.requireNonNullElseGet(hearingDate, LocalDate::now));

        hearing.setHearingIsActual(isHearingActual);
        hearing.addJudge(judge, false);

        return hearing;
    }

    public static HearingEntity hearingWith(String caseNumber, String courthouseName, String courtroomName, String hearingDatetime) {
        var courtroom =
            createCourtRoomWithNameAtCourthouse(
                createCourthouseWithName(courthouseName),
                courtroomName);

        var hearing = createHearingFor(caseWithCaseNumber(caseNumber));
        var hearingStartDateTime = LocalDateTime.parse(hearingDatetime);
        hearing.setHearingDate(hearingStartDateTime.toLocalDate());
        hearing.setScheduledStartTime(hearingStartDateTime.toLocalTime());
        hearing.setCourtroom(courtroom);
        return hearing;
    }
}
