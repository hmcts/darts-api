package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.test.common.data.builder.CustomHearingEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Objects;

import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.createCourthouseWithName;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.createCourtRoomWithNameAtCourthouse;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.someMinimalCourtRoom;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

@SuppressWarnings({"HideUtilityClassConstructor"})
public class HearingTestData  implements Persistable<CustomHearingEntity.CustomHearingEntityBuilderRetrieve> {

    HearingTestData() {
    }

    private static final LocalDate HEARING_DATE = LocalDate.of(2023, 6, 20);

    public HearingEntity someMinimalHearing() {
        return someMinimal().getBuilder().build().getEntity();
    }

    public HearingEntity createHearingFor(CourtCaseEntity courtCase) {
        var minimalHearing = someMinimalHearing();
        minimalHearing.setCourtCase(courtCase);
        return minimalHearing;
    }

    public HearingEntity createHearingWith(CourtCaseEntity courtCase, CourtroomEntity courtroom, LocalDate hearingDate) {
        var hearing = createHearingFor(courtCase);
        hearing.setCourtroom(courtroom);
        hearing.setHearingDate(hearingDate);
        return hearing;
    }

    public HearingEntity createHearingWith(String caseNumber, LocalTime scheduledStartTime) {
        HearingEntity hearing1 = createHearingWithDefaults(
            PersistableFactory.getCourtCaseTestData().createCaseWithCaseNumber(caseNumber),
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

    public HearingEntity createHearingWith(CourtCaseEntity caseEntity, CourtroomEntity courtroomEntity) {
        HearingEntity hearingEntity = someMinimalHearing();
        hearingEntity.setCourtCase(caseEntity);
        hearingEntity.setCourtroom(courtroomEntity);
        hearingEntity.setHearingDate(HEARING_DATE);
        return hearingEntity;
    }

    public HearingEntity createHearingWithDefaults(CourtCaseEntity courtCase, CourtroomEntity courtroom, LocalDate hearingDate, JudgeEntity judge) {
        return createHearingWithDefaults(courtCase, courtroom, hearingDate, judge, true);
    }


    public HearingEntity createHearingWithDefaults(CourtCaseEntity courtCase, CourtroomEntity courtroom, LocalDate hearingDate, JudgeEntity judge,
                                                          boolean isHearingActual) {
        HearingEntity hearing = someMinimalHearing();
        hearing.setCourtCase(Objects.requireNonNullElseGet(courtCase, () -> PersistableFactory.getCourtCaseTestData().someMinimal().build()));

        hearing.setCourtroom(Objects.requireNonNullElseGet(courtroom, CourtroomTestData::someMinimalCourtRoom));

        hearing.setHearingDate(Objects.requireNonNullElseGet(hearingDate, LocalDate::now));

        hearing.setHearingIsActual(isHearingActual);
        hearing.addJudge(judge, false);

        return hearing;
    }

    public HearingEntity hearingWith(String caseNumber, String courthouseName, String courtroomName, String hearingDatetime) {
        var courtroom =
            createCourtRoomWithNameAtCourthouse(
                createCourthouseWithName(courthouseName),
                courtroomName);

        var hearing = createHearingFor(PersistableFactory.getCourtCaseTestData().caseWithCaseNumber(caseNumber));
        var hearingStartDateTime = LocalDateTime.parse(hearingDatetime);
        hearing.setHearingDate(hearingStartDateTime.toLocalDate());
        hearing.setScheduledStartTime(hearingStartDateTime.toLocalTime());
        hearing.setCourtroom(courtroom);
        return hearing;
    }

    @Override
    public CustomHearingEntity.CustomHearingEntityBuilderRetrieve  someMinimal() {
        CustomHearingEntity.CustomHearingEntityBuilderRetrieve builder = new CustomHearingEntity.CustomHearingEntityBuilderRetrieve();

        CourtCaseEntity courtCaseEntity = PersistableFactory.getCourtCaseTestData().createSomeMinimalCase();

        var minimalCourtRoom = someMinimalCourtRoom();
        minimalCourtRoom.setCourthouse(courtCaseEntity.getCourthouse());

        builder.getBuilder().courtCase(courtCaseEntity).courtroom(minimalCourtRoom)
            .hearingIsActual(true)
            .hearingDate(LocalDate.now().plusWeeks(1))
            .createdBy(minimalUserAccount())
            .lastModifiedBy(minimalUserAccount())
            .createdDateTime(OffsetDateTime.now())
            .lastModifiedDateTime(OffsetDateTime.now()).judges(new ArrayList<>())
            .mediaList(new ArrayList<>());

        return builder;
    }

    @Override
    public CustomHearingEntity.CustomHearingEntityBuilderRetrieve  someMaximal() {
        return someMinimal();
    }
}