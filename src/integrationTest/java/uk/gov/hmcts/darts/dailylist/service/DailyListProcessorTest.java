package uk.gov.hmcts.darts.dailylist.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.cases.repository.CaseRepository;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.dailylist.enums.SourceType;
import uk.gov.hmcts.darts.dailylist.repository.DailyListRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.data.DailyListTestData;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@ExtendWith(MockitoExtension.class)
class DailyListProcessorTest extends IntegrationBase {

    public static final String SWANSEA = "SWANSEA";
    public static final String URN_1 = "42GD2391421";
    public static final String URN_2 = "42GD2391433";
    public static final String COURTROOM_1 = "1";
    public static final String COURTROOM_2 = "2";
    public static final String CASE_NUMBER_1 = "Case1";
    public static final String CASE_NUMBER_2 = "Case2";
    static final ObjectMapper MAPPER = new ObjectMapper();
    @Autowired
    DailyListProcessor dailyListProcessor;

    @Autowired
    DailyListRepository dailyListRepository;

    @Autowired
    CaseRepository caseRepository;

    @Autowired
    HearingRepository hearingRepository;

    @BeforeAll
    static void beforeAll() {
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void dailyListProcessorMultipleDailyList() throws IOException {
        CourthouseEntity swanseaCourtEntity = dartsDatabase.createCourthouseWithTwoCourtrooms();
        DailyListEntity dailyListEntity = DailyListTestData.createDailyList(LocalTime.now(), String.valueOf(SourceType.CPP),
                swanseaCourtEntity, "tests/dailyListProcessorTest/dailyListCPP.json");

        DailyListEntity oldDailyListEntity = DailyListTestData.createDailyList(LocalTime.now().minusHours(3),
                String.valueOf(SourceType.CPP), swanseaCourtEntity, "tests/dailyListProcessorTest/dailyListCPP.json");

        dailyListRepository.saveAndFlush(dailyListEntity);
        dailyListRepository.saveAndFlush(oldDailyListEntity);

        dailyListProcessor.processAllDailyLists(LocalDate.now());

        CourtCaseEntity newCase1 = caseRepository.findByCaseNumberIgnoreCaseAndCourthouse_CourthouseNameIgnoreCase(URN_1, SWANSEA).get();
        assertEquals(URN_1, newCase1.getCaseNumber());
        assertEquals(SWANSEA, newCase1.getCourthouse().getCourthouseName());
        assertEquals(1, newCase1.getDefendantList().size());
        assertEquals(1, newCase1.getDefenceList().size());
        assertEquals(1, newCase1.getProsecutorList().size());
        assertEquals(1, newCase1.getJudges().size());


        HearingEntity newHearing1 = hearingRepository.findByCourthouseCourtroomAndDate(SWANSEA, COURTROOM_1, LocalDate.now()).get(0);
        assertEquals(LocalDate.now(), newHearing1.getHearingDate());
        assertEquals(COURTROOM_1, newHearing1.getCourtroom().getName());
        assertEquals(1, newHearing1.getJudges().size());

        CourtCaseEntity newCase2 = caseRepository.findByCaseNumberIgnoreCaseAndCourthouse_CourthouseNameIgnoreCase(URN_2, SWANSEA).get();
        assertEquals(URN_2, newCase2.getCaseNumber());
        assertEquals(SWANSEA, newCase2.getCourthouse().getCourthouseName());
        assertEquals(1, newCase2.getDefendantList().size());
        assertEquals(1, newCase2.getDefenceList().size());
        assertEquals(1, newCase2.getProsecutorList().size());
        assertEquals(1, newCase2.getJudges().size());


        HearingEntity newHearing2 = hearingRepository.findByCourthouseCourtroomAndDate(SWANSEA, COURTROOM_2, LocalDate.now()).get(0);
        assertEquals(LocalDate.now(), newHearing2.getHearingDate());
        assertEquals(COURTROOM_2, newHearing2.getCourtroom().getName());
        assertEquals(1, newHearing2.getJudges().size());
    }

    @Test
    void dailyListProcessorCppAndXhbDailyLists() throws IOException {
        CourthouseEntity swanseaCourtEntity = dartsDatabase.createCourthouseWithTwoCourtrooms();

        dartsDatabase.createDailyLists(swanseaCourtEntity);

        dailyListProcessor.processAllDailyLists(LocalDate.now());

        CourtCaseEntity newCase1 = caseRepository.findByCaseNumberIgnoreCaseAndCourthouse_CourthouseNameIgnoreCase(URN_1, SWANSEA).get();
        assertEquals(URN_1, newCase1.getCaseNumber());
        assertEquals(SWANSEA, newCase1.getCourthouse().getCourthouseName());
        assertEquals(1, newCase1.getDefendantList().size());
        assertEquals(1, newCase1.getDefenceList().size());
        assertEquals(1, newCase1.getProsecutorList().size());
        assertEquals(1, newCase1.getJudges().size());

        CourtCaseEntity newCase2 = caseRepository.findByCaseNumberIgnoreCaseAndCourthouse_CourthouseNameIgnoreCase(URN_2, SWANSEA).get();
        assertEquals(URN_2, newCase2.getCaseNumber());
        assertEquals(SWANSEA, newCase2.getCourthouse().getCourthouseName());
        assertEquals(1, newCase2.getDefendantList().size());
        assertEquals(1, newCase2.getDefenceList().size());
        assertEquals(1, newCase2.getProsecutorList().size());
        assertEquals(1, newCase2.getJudges().size());


        CourtCaseEntity newCase3 = caseRepository.findByCaseNumberIgnoreCaseAndCourthouse_CourthouseNameIgnoreCase(CASE_NUMBER_1, SWANSEA).get();
        assertEquals(CASE_NUMBER_1, newCase3.getCaseNumber());
        assertEquals(SWANSEA, newCase3.getCourthouse().getCourthouseName());
        assertEquals(1, newCase3.getDefendantList().size());
        assertEquals(1, newCase3.getDefenceList().size());
        assertEquals(1, newCase3.getProsecutorList().size());
        assertEquals(1, newCase3.getJudges().size());


        CourtCaseEntity newCase4 = caseRepository.findByCaseNumberIgnoreCaseAndCourthouse_CourthouseNameIgnoreCase(CASE_NUMBER_2, SWANSEA).get();
        assertEquals(CASE_NUMBER_2, newCase4.getCaseNumber());
        assertEquals(SWANSEA, newCase4.getCourthouse().getCourthouseName());
        assertEquals(1, newCase4.getDefendantList().size());
        assertEquals(1, newCase4.getDefenceList().size());
        assertEquals(1, newCase4.getProsecutorList().size());
        assertEquals(1, newCase4.getJudges().size());


        List<HearingEntity> hearings = hearingRepository.findAll();
        for (HearingEntity hearing : hearings) {
            assertEquals(LocalDate.now(), hearing.getHearingDate());
            assertThat(hearing.getCourtroom().getName(), Matchers.either(Matchers.is(COURTROOM_1)).or(Matchers.is(COURTROOM_2)));
            assertEquals(1, hearing.getJudges().size());

        }

    }


}
