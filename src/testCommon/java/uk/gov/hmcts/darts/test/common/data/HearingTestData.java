package uk.gov.hmcts.darts.test.common.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

import static uk.gov.hmcts.darts.test.common.data.CaseTestData.createCaseWithCaseNumber;
import static uk.gov.hmcts.darts.test.common.data.CaseTestData.createSomeMinimalCase;
import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.createCourthouse;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.createCourtRoomWithNameAtCourthouse;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.someMinimalCourtRoom;

@UtilityClass
@SuppressWarnings({"HideUtilityClassConstructor"})
public class HearingTestData {

    private static final LocalDate HEARING_DATE = LocalDate.of(2023, 6, 20);

    public static HearingEntity createSomeMinimalHearing() {
        var minimalCase = createSomeMinimalCase();
        var minimalCourtRoom = someMinimalCourtRoom();
        minimalCourtRoom.setCourthouse(minimalCase.getCourthouse());

        var hearingEntity = new HearingEntity();
        hearingEntity.setCourtCase(minimalCase);
        hearingEntity.setCourtroom(minimalCourtRoom);
        hearingEntity.setHearingIsActual(true);
        hearingEntity.setHearingDate(LocalDate.now().plusWeeks(1));
        return hearingEntity;
    }

    // Refactor, this isn't a minimal hearing
    public static HearingEntity someMinimalHearing() {
        return createHearingWithDefaults(null, null, null, null);
    }

    public static HearingEntity createHearingWith(CourtCaseEntity courtCase, CourtroomEntity courtroom, LocalDate hearingDate) {
        return createHearingWithDefaults(courtCase, courtroom, hearingDate, null);
    }

    public static HearingEntity createHearingWith(String caseNumber, LocalTime scheduledStartTime) {
        HearingEntity hearing1 = createHearingWithDefaults(
            createCaseWithCaseNumber(caseNumber),
            createCourtRoomWithNameAtCourthouse(
                createCourthouse("NEWCASTLE"),
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
        HearingEntity hearing = new HearingEntity();
        hearing.setCourtCase(Objects.requireNonNullElseGet(courtCase, CaseTestData::someMinimalCase));

        hearing.setCourtroom(Objects.requireNonNullElseGet(courtroom, CourtroomTestData::someMinimalCourtRoom));

        hearing.setHearingDate(Objects.requireNonNullElseGet(hearingDate, LocalDate::now));

        hearing.setHearingIsActual(false);
        hearing.addJudge(judge);

        return hearing;
    }


}
