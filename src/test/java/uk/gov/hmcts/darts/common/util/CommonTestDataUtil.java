package uk.gov.hmcts.darts.common.util;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.CaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class CommonTestDataUtil {

    public CourthouseEntity createCourthouse(String name) {
        CourthouseEntity courthouse = new CourthouseEntity();
        courthouse.setCourthouseName(name);
        return courthouse;
    }

    public CourtroomEntity createCourtroom(CourthouseEntity courthouse, String name) {
        CourtroomEntity courtroom = new CourtroomEntity();
        courtroom.setCourthouse(courthouse);
        courtroom.setName(name);
        return courtroom;
    }

    public CourtroomEntity createCourtroom(String name) {
        createCourthouse("SWANSEA");
        return createCourtroom(createCourthouse("SWANSEA"), name);
    }

    public CaseEntity createCase(String caseNumber) {
        CaseEntity courtcase = new CaseEntity();
        courtcase.setCaseNumber(caseNumber);
        courtcase.setDefenders(List.of("defender_" + caseNumber + "_1", "defender_" + caseNumber + "_2"));
        courtcase.setDefendants(List.of("defendant_" + caseNumber + "_1", "defendant_" + caseNumber + "_2"));
        courtcase.setProsecutors(List.of("Prosecutor_" + caseNumber + "_1", "Prosecutor_" + caseNumber + "_2"));
        return courtcase;
    }

    public HearingEntity createHearing(CaseEntity courtcase, CourtroomEntity courtroom, LocalDate date) {
        HearingEntity hearing1 = new HearingEntity();
        hearing1.setCourtCase(courtcase);
        hearing1.setCourtroom(courtroom);
        hearing1.setHearingDate(date);
        return hearing1;
    }

    public HearingEntity createHearing(String caseNumber, LocalTime time) {
        HearingEntity hearing1 = new HearingEntity();
        hearing1.setCourtCase(createCase(caseNumber));
        hearing1.setCourtroom(createCourtroom("1"));
        hearing1.setHearingDate(LocalDate.of(2023, 6, 20));
        hearing1.setScheduledStartTime(time);
        return hearing1;
    }

    public List<HearingEntity> createHearings(int numOfHearings) {
        List<HearingEntity> returnList = new ArrayList<>();
        LocalTime time = LocalTime.of(9, 0, 0);
        for (int counter = 1; counter <= numOfHearings; counter++) {
            returnList.add(createHearing("caseNum_" + counter, time));
            time = time.plusHours(1);
        }
        return returnList;
    }

}
