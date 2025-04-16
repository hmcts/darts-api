package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.test.common.data.builder.TestHearingEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Objects;

import static java.time.OffsetDateTime.now;
import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.createCourthouseWithName;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.createCourtRoomWithNameAtCourthouse;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.someMinimalCourtRoom;

public final class HearingTestData
    implements Persistable<TestHearingEntity.TestHearingEntityBuilderRetrieve, HearingEntity, TestHearingEntity.TestHearingEntityBuilder> {

    private static final LocalDate HEARING_DATE = LocalDate.of(2023, 6, 20);

    HearingTestData() {
        // This constructor is intentionally empty. Nothing special is needed here.
    }

    public HearingEntity someMinimalHearing() {
        var minimalCase = PersistableFactory.getCourtCaseTestData().createSomeMinimalCase();
        var minimalCourtRoom = someMinimalCourtRoom();
        minimalCourtRoom.setCourthouse(minimalCase.getCourthouse());

        var hearingEntity = new HearingEntity();
        hearingEntity.setCourtCase(minimalCase);
        hearingEntity.setCourtroom(minimalCourtRoom);
        hearingEntity.setHearingIsActual(true);
        hearingEntity.setHearingDate(LocalDate.now().plusWeeks(1));
        hearingEntity.setCreatedById(0);
        hearingEntity.setLastModifiedById(0);

        hearingEntity.setLastModifiedDateTime(now());
        hearingEntity.setCreatedDateTime(now());

        return hearingEntity;
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
        hearing.setCourtCase(Objects.requireNonNullElseGet(courtCase, () -> PersistableFactory.getCourtCaseTestData().someMinimal()));

        hearing.setCourtroom(Objects.requireNonNullElseGet(courtroom, CourtroomTestData::someMinimalCourtRoom));

        hearing.setHearingDate(Objects.requireNonNullElseGet(hearingDate, LocalDate::now));

        hearing.setHearingIsActual(isHearingActual);
        hearing.addJudge(judge, false);

        return hearing;
    }

    public HearingEntity hearingWith(String caseNumber,
                                     String courthouseName,
                                     String courtroomName,
                                     String hearingDatetime) {
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
    public HearingEntity someMinimal() {
        return someMinimalBuilder().build().getEntity();
    }

    @Override
    public TestHearingEntity.TestHearingEntityBuilderRetrieve someMinimalBuilderHolder() {
        TestHearingEntity.TestHearingEntityBuilderRetrieve builder = new TestHearingEntity.TestHearingEntityBuilderRetrieve();

        CourtCaseEntity courtCaseEntity = PersistableFactory.getCourtCaseTestData().createSomeMinimalCase();

        var minimalCourtRoom = someMinimalCourtRoom();
        minimalCourtRoom.setCourthouse(courtCaseEntity.getCourthouse());

        builder.getBuilder().courtCase(courtCaseEntity).courtroom(minimalCourtRoom)
            .hearingIsActual(true)
            .hearingDate(LocalDate.now().plusWeeks(1))
            .createdById(0)
            .lastModifiedById(0)
            .createdDateTime(now())
            .lastModifiedDateTime(now()).judges(new ArrayList<>())
            .mediaList(new ArrayList<>());

        return builder;
    }

    @Override
    public TestHearingEntity.TestHearingEntityBuilder someMinimalBuilder() {
        return someMinimalBuilderHolder().getBuilder();
    }
}