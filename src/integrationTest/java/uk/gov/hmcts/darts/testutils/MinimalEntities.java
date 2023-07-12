package uk.gov.hmcts.darts.testutils;

import uk.gov.hmcts.darts.common.entity.CaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;

import static java.time.LocalDate.now;

@SuppressWarnings({"MethodName", "HideUtilityClassConstructor"})
public class MinimalEntities {

    public static CaseEntity aCase() {
        var caseEntity = new CaseEntity();
        caseEntity.setCaseNumber("1");
        caseEntity.setCourthouse(aCourtHouse());
        return caseEntity;
    }

    public static CaseEntity aCaseEntityAt(CourthouseEntity courthouse) {
        var caseEntity = new CaseEntity();
        caseEntity.setCaseNumber("1");
        caseEntity.setCourthouse(courthouse);
        return caseEntity;
    }

    public static CourtroomEntity aCourtroomAt(CourthouseEntity courthouse) {
        CourtroomEntity courtroom = new CourtroomEntity();
        courtroom.setCourthouse(courthouse);
        courtroom.setName("some-court-room");
        return courtroom;
    }

    public static CourtroomEntity aCourtroom() {
        CourtroomEntity courtroom = new CourtroomEntity();
        courtroom.setCourthouse(aCourtHouse());
        courtroom.setName("some-court-room");
        return courtroom;
    }

    public static CourthouseEntity aCourtHouse() {
        var courtHouse = new CourthouseEntity();
        courtHouse.setCourthouseName("some-courthouse");
        return courtHouse;
    }

    public static CourthouseEntity aCourtHouseWithName(String name) {
        var courtHouse = new CourthouseEntity();
        courtHouse.setCourthouseName(name);
        return courtHouse;
    }

    public static HearingEntity aHearing() {
        var courtroom = aCourtroom();
        var caseEntity = aCase();
        caseEntity.setCourthouse(courtroom.getCourthouse());

        HearingEntity hearing = new HearingEntity();
        hearing.setCourtCase(caseEntity);
        hearing.setCourtroom(courtroom);
        hearing.setHearingDate(now());
        return hearing;
    }

    public static HearingEntity aHearingForCaseInRoom(CaseEntity courtCase, CourtroomEntity room) {
        courtCase.setCourthouse(room.getCourthouse());
        HearingEntity hearing = new HearingEntity();
        hearing.setCourtCase(courtCase);
        hearing.setCourtroom(room);
        hearing.setHearingDate(now());
        return hearing;
    }
}
