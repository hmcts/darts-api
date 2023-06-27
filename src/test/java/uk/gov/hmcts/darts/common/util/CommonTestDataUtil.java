package uk.gov.hmcts.darts.common.util;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.Case;
import uk.gov.hmcts.darts.common.entity.Courthouse;
import uk.gov.hmcts.darts.common.entity.Courtroom;
import uk.gov.hmcts.darts.common.entity.Hearing;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class CommonTestDataUtil {

    public Courthouse createCourthouse(String name) {
        Courthouse courthouse = new Courthouse();
        courthouse.setCourthouseName(name);
        return courthouse;
    }

    public Courtroom createCourtroom(Courthouse courthouse, String name) {
        Courtroom courtroom = new Courtroom();
        courtroom.setCourthouse(courthouse);
        courtroom.setName(name);
        return courtroom;
    }

    public Courtroom createCourtroom(String name) {
        createCourthouse("SWANSEA");
        return createCourtroom(createCourthouse("SWANSEA"), name);
    }

    public Case createCase(String caseNumber) {
        Case courtcase = new Case();
        courtcase.setCaseNumber(caseNumber);
        courtcase.setDefenders(List.of("defender_" + caseNumber + "_1", "defender_" + caseNumber + "_2"));
        courtcase.setDefendants(List.of("defendant_" + caseNumber + "_1", "defendant_" + caseNumber + "_2"));
        courtcase.setProsecutors(List.of("Prosecutor_" + caseNumber + "_1", "Prosecutor_" + caseNumber + "_2"));
        return courtcase;
    }

    public Hearing createHearing(Case courtcase, Courtroom courtroom, LocalDate date) {
        Hearing hearing1 = new Hearing();
        hearing1.setCourtCase(courtcase);
        hearing1.setCourtroom(courtroom);
        hearing1.setHearingDate(date);
        return hearing1;
    }

    public Hearing createHearing(String caseNumber, LocalTime time) {
        Hearing hearing1 = new Hearing();
        hearing1.setCourtCase(createCase(caseNumber));
        hearing1.setCourtroom(createCourtroom("1"));
        hearing1.setHearingDate(LocalDate.of(2023, 6, 20));
        hearing1.setScheduledStartTime(time);
        return hearing1;
    }

    public List<Hearing> createHearings(int numOfHearings) {
        List<Hearing> returnList = new ArrayList<>();
        LocalTime time = LocalTime.of(9, 0, 0);
        for (int counter = 1; counter <= numOfHearings; counter++) {
            returnList.add(createHearing("caseNum_" + counter, time));
            time = time.plusHours(1);
        }
        return returnList;
    }

}
