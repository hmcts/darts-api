package uk.gov.hmcts.darts.testutils.data;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

import static uk.gov.hmcts.darts.testutils.data.CaseTestData.createCaseWithCaseNumber;
import static uk.gov.hmcts.darts.testutils.data.CourthouseTestData.createCourthouse;

@UtilityClass
@SuppressWarnings({"PMD.TooManyMethods", "HideUtilityClassConstructor"})
public class HearingTestData {

    public static HearingEntity someMinimalHearing() {

        return createHearingWithDefaults(null, null, null, null);
    }

    public static HearingEntity createHearingWith(CourtCaseEntity courtCase, CourtroomEntity courtroom, LocalDate hearingDate) {
        return createHearingWithDefaults(courtCase, courtroom, hearingDate, null);
    }

    public static HearingEntity createHearingWith(String caseNumber, LocalTime scheduledStartTime) {
        HearingEntity hearing1 = createHearingWithDefaults(
            createCaseWithCaseNumber(caseNumber),
            CourtroomTestData.createCourtRoomWithNameAtCourthouse(
                createCourthouse("NEWCASTLE"),
                "1"
            ),
            LocalDate.of(2023, 6, 20),
            null
        );
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

    public static HearingEntity createHearingWithDefaults(CourtCaseEntity courtCase, CourtroomEntity courtroom, LocalDate hearingDate, JudgeEntity judge) {
        HearingEntity hearing = new HearingEntity();
        hearing.setCourtCase(Objects.requireNonNullElseGet(courtCase, CaseTestData::someMinimalCase));

        hearing.setCourtroom(Objects.requireNonNullElseGet(courtroom, CourtroomTestData::someMinimalCourtRoom));

        hearing.setHearingDate(Objects.requireNonNullElseGet(hearingDate, LocalDate::now));

        hearing.addJudge(judge);

        return hearing;
    }


}
