package uk.gov.hmcts.darts.cases.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.cases.model.GetCasesRequest;
import uk.gov.hmcts.darts.cases.model.ScheduledCase;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.darts.testutils.data.CaseTestData.createCaseAt;
import static uk.gov.hmcts.darts.testutils.data.CourthouseTestData.someMinimalCourthouse;
import static uk.gov.hmcts.darts.testutils.data.CourtroomTestData.someMinimalCourtRoom;
import static uk.gov.hmcts.darts.testutils.data.DefenceTestData.createListOfDefenceForCase;
import static uk.gov.hmcts.darts.testutils.data.DefendantTestData.createListOfDefendantsForCase;
import static uk.gov.hmcts.darts.testutils.data.HearingTestData.createHearingWith;
import static uk.gov.hmcts.darts.testutils.data.JudgeTestData.createListOfJudgesForHearing;
import static uk.gov.hmcts.darts.testutils.data.ProsecutorTestData.createListOfProsecutor;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
class CaseServiceTest extends IntegrationBase {

    @Autowired
    CaseService service;
    CourthouseEntity swanseaCourthouse;

    @BeforeEach
    void setupData() {
        swanseaCourthouse = someMinimalCourthouse();
        swanseaCourthouse.setCourthouseName("SWANSEA");
    }

    @Test
    void testGetCasesOk1() {
        var swanseaCourtroom1 = someMinimalCourtRoom();
        swanseaCourtroom1.setName("1");
        swanseaCourtroom1.setCourthouse(swanseaCourthouse);
        var hearingForCase1 = setupCase1(swanseaCourthouse, swanseaCourtroom1);
        var hearingForCase2 = setupCase2(swanseaCourthouse, swanseaCourtroom1);
        var hearingForCase3 = setupCase3(swanseaCourthouse, swanseaCourtroom1);
        dartsDatabase.saveAll(hearingForCase1, hearingForCase2, hearingForCase3);

        GetCasesRequest request = new GetCasesRequest();
        request.setCourthouse("Swansea");
        request.setCourtroom("1");
        request.setDate(LocalDate.of(2023, 6, 20));

        List<ScheduledCase> resultList = service.getCases(request);

        assertThat(resultList.size()).isEqualTo(3);
    }

    @Test
    void testGetCasesOk2() throws IOException {
        var swanseaCourtroom2 = someMinimalCourtRoom();
        swanseaCourtroom2.setName("2");
        swanseaCourtroom2.setCourthouse(swanseaCourthouse);
        var hearingForCase1 = setupCase1(swanseaCourthouse, swanseaCourtroom2);
        var hearingForCase2 = setupCase2(swanseaCourthouse, swanseaCourtroom2);
        var hearingForCase3 = setupCase3(swanseaCourthouse, swanseaCourtroom2);
        dartsDatabase.saveAll(hearingForCase1, hearingForCase2, hearingForCase3);

        GetCasesRequest request = new GetCasesRequest();
        request.setCourthouse("Swansea");
        request.setCourtroom("2");
        request.setDate(LocalDate.of(2023, 6, 20));
        List<ScheduledCase> resultList = service.getCases(request);

        assertThat(resultList.size()).isEqualTo(3);
    }

    @Test
    void testGetCasesCreateCourtroom() {
        String courthouseName = "CARDIFF";
        String courtroomName = "99";
        dartsDatabase.createCourthouseWithoutCourtrooms(courthouseName);

        GetCasesRequest request = new GetCasesRequest();
        request.setCourthouse(courthouseName);
        request.setCourtroom(courtroomName);
        request.setDate(LocalDate.of(2023, 6, 20));

        List<ScheduledCase> resultList = service.getCases(request);

        assertEquals(0, resultList.size());
        var foundCourtroom = dartsDatabase.findCourtroomBy(courthouseName, courtroomName);
        assertEquals(courtroomName.toUpperCase(Locale.ROOT), foundCourtroom.getName());
        assertEquals(courthouseName, foundCourtroom.getCourthouse().getCourthouseName());
    }

    // To be replaced by something more reusable as the last PR in this ticket
    private static HearingEntity setupCase1(CourthouseEntity swanseaCourthouse, CourtroomEntity swanseaCourtroom1) {
        var case1 = createCaseAt(swanseaCourthouse);
        case1.setCaseNumber("Case0000001");
        case1.setDefendantList(createListOfDefendantsForCase(2, case1));
        case1.setDefenceList(createListOfDefenceForCase(2, case1));
        case1.setProsecutorList(createListOfProsecutor(2, case1));

        var hearingForCase1 = createHearingWith(case1, swanseaCourtroom1);
        hearingForCase1.setJudgeList(createListOfJudgesForHearing(1, hearingForCase1));
        hearingForCase1.setHearingDate(LocalDate.parse("2023-06-20"));
        hearingForCase1.setScheduledStartTime(LocalTime.parse("09:00"));
        return hearingForCase1;
    }

    // To be replaced by something more reusable as the last PR in this ticket
    private static HearingEntity setupCase2(CourthouseEntity swanseaCourthouse, CourtroomEntity swanseaCourtroom1) {
        var case2 = createCaseAt(swanseaCourthouse);
        case2.setCaseNumber("Case0000002");
        case2.setDefendantList(createListOfDefendantsForCase(2, case2));
        case2.setDefenceList(createListOfDefenceForCase(2, case2));
        case2.setProsecutorList(createListOfProsecutor(2, case2));

        var hearingForCase2 = createHearingWith(case2, swanseaCourtroom1);
        hearingForCase2.setJudgeList(createListOfJudgesForHearing(1, hearingForCase2));
        hearingForCase2.setHearingDate(LocalDate.parse("2023-06-20"));
        hearingForCase2.setScheduledStartTime(LocalTime.parse("10:00"));
        return hearingForCase2;
    }

    // To be replaced by something more reusable as the last PR in this ticket
    private static HearingEntity setupCase3(CourthouseEntity swanseaCourthouse, CourtroomEntity swanseaCourtroom1) {
        var case3 = createCaseAt(swanseaCourthouse);
        case3.setCaseNumber("Case0000003");
        case3.setDefendantList(createListOfDefendantsForCase(2, case3));
        case3.setDefenceList(createListOfDefenceForCase(2, case3));
        case3.setProsecutorList(createListOfProsecutor(2, case3));

        var hearingForCase3 = createHearingWith(case3, swanseaCourtroom1);
        hearingForCase3.setJudgeList(createListOfJudgesForHearing(1, hearingForCase3));
        hearingForCase3.setHearingDate(LocalDate.parse("2023-06-20"));
        hearingForCase3.setScheduledStartTime(LocalTime.parse("11:00"));
        return hearingForCase3;
    }
}
