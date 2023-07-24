package uk.gov.hmcts.darts.testutils;

import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.time.OffsetDateTime;

import static java.time.LocalDate.now;

@SuppressWarnings({"MethodName", "HideUtilityClassConstructor"})
public class MinimalEntities {

    public static CourtCaseEntity aCase() {
        var caseEntity = new CourtCaseEntity();
        caseEntity.setCaseNumber("1");
        caseEntity.setCourthouse(aCourtHouse());
        return caseEntity;
    }

    public static CourtCaseEntity aCaseEntityAt(CourthouseEntity courthouse) {
        var caseEntity = new CourtCaseEntity();
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

    public static HearingEntity aHearingForCaseInRoom(CourtCaseEntity courtCase, CourtroomEntity room) {
        courtCase.setCourthouse(room.getCourthouse());
        HearingEntity hearing = new HearingEntity();
        hearing.setCourtCase(courtCase);
        hearing.setCourtroom(room);
        hearing.setHearingDate(now());
        return hearing;
    }

    public static MediaEntity aMediaEntity(OffsetDateTime startTime, OffsetDateTime endTime, int channel) {
        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setStart(startTime);
        mediaEntity.setEnd(endTime);
        mediaEntity.setChannel(channel);

        return mediaEntity;
    }

}
