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
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.darts.testutils.MinimalEntities.aCaseEntityAt;
import static uk.gov.hmcts.darts.testutils.MinimalEntities.aCourtHouse;
import static uk.gov.hmcts.darts.testutils.MinimalEntities.aCourtroom;
import static uk.gov.hmcts.darts.testutils.MinimalEntities.aHearingForCaseInRoom;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
class CaseServiceTest extends IntegrationBase {

    @Autowired
    CaseService service;
    CourthouseEntity swanseaCourthouse;

    @BeforeEach
    void setupData() {
        swanseaCourthouse = aCourtHouse();
        swanseaCourthouse.setCourthouseName("SWANSEA");
    }

    @Test
    void testGetCasesOk1() {
        var swanseaCourtroom1 = aCourtroom();
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
        var swanseaCourtroom2 = aCourtroom();
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
        var case1 = aCaseEntityAt(swanseaCourthouse);
        case1.setCaseNumber("Case0000001");
        case1.setDefendantList(CommonTestDataUtil.createDefendantList(case1));
        case1.setDefenceList(CommonTestDataUtil.createDefenceList(case1));
        case1.setProsecutorList(CommonTestDataUtil.createProsecutorList(case1));

        var hearingForCase1 = aHearingForCaseInRoom(case1, swanseaCourtroom1);
        hearingForCase1.setJudgeList(List.of(CommonTestDataUtil.createJudge(hearingForCase1, "Judge1")));
        hearingForCase1.setHearingDate(LocalDate.parse("2023-06-20"));
        hearingForCase1.setScheduledStartTime(LocalTime.parse("09:00"));
        return hearingForCase1;
    }

    // To be replaced by something more reusable as the last PR in this ticket
    private static HearingEntity setupCase2(CourthouseEntity swanseaCourthouse, CourtroomEntity swanseaCourtroom1) {
        var case2 = aCaseEntityAt(swanseaCourthouse);
        case2.setCaseNumber("Case0000002");
        case2.setDefendantList(CommonTestDataUtil.createDefendantList(case2));
        case2.setDefenceList(CommonTestDataUtil.createDefenceList(case2));
        case2.setProsecutorList(CommonTestDataUtil.createProsecutorList(case2));

        var hearingForCase2 = aHearingForCaseInRoom(case2, swanseaCourtroom1);
        hearingForCase2.setJudgeList(List.of(CommonTestDataUtil.createJudge(hearingForCase2, "Judge1")));
        hearingForCase2.setHearingDate(LocalDate.parse("2023-06-20"));
        hearingForCase2.setScheduledStartTime(LocalTime.parse("10:00"));
        return hearingForCase2;
    }

    // To be replaced by something more reusable as the last PR in this ticket
    private static HearingEntity setupCase3(CourthouseEntity swanseaCourthouse, CourtroomEntity swanseaCourtroom1) {
        var case3 = aCaseEntityAt(swanseaCourthouse);
        case3.setCaseNumber("Case0000003");
        case3.setDefendantList(CommonTestDataUtil.createDefendantList(case3));
        case3.setDefenceList(CommonTestDataUtil.createDefenceList(case3));
        case3.setProsecutorList(CommonTestDataUtil.createProsecutorList(case3));

        var hearingForCase3 = aHearingForCaseInRoom(case3, swanseaCourtroom1);
        hearingForCase3.setJudgeList(List.of(CommonTestDataUtil.createJudge(hearingForCase3, "Judge1")));
        hearingForCase3.setHearingDate(LocalDate.parse("2023-06-20"));
        hearingForCase3.setScheduledStartTime(LocalTime.parse("11:00"));
        return hearingForCase3;
    }
}
