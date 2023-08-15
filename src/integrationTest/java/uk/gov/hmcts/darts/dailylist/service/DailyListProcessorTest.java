package uk.gov.hmcts.darts.dailylist.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.transaction.Transactional;
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
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;
import uk.gov.hmcts.darts.dailylist.enums.SourceType;
import uk.gov.hmcts.darts.dailylist.repository.DailyListRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.data.CourtroomTestData;
import uk.gov.hmcts.darts.testutils.data.DailyListTestData;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@ExtendWith(MockitoExtension.class)
class DailyListProcessorTest extends IntegrationBase {

    static final ObjectMapper MAPPER = new ObjectMapper();
    public static final String SWANSEA = "SWANSEA";
    public static final String CASE_NUMBER_1 = "42GD2391421";
    public static final String CASE_NUMBER_2 = "42GD2391433";
    public static final String COURTROOM_1 = "1";
    public static final String COURTROOM_2 = "2";

    @Autowired
    DailyListProcessor dailyListProcessor;

    @Autowired
    DailyListRepository dailyListRepository;

    @Autowired
    CourtroomRepository courtroomRepository;

    @Autowired
    CourthouseRepository courthouseRepository;

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
    @Transactional
    void dailyListProcessorSingleDailyList() throws IOException {
        courthouseRepository.deleteAll();
        CourthouseEntity swanseaCourtEntity = dartsDatabase.createCourthouseWithNameAndCode(SWANSEA, 457);
        courtroomRepository.saveAndFlush(CourtroomTestData.createCourtRoomWithNameAtCourthouse(swanseaCourtEntity, COURTROOM_1));
        courtroomRepository.saveAndFlush(CourtroomTestData.createCourtRoomWithNameAtCourthouse(swanseaCourtEntity, COURTROOM_2));

        DailyListEntity dailyListEntity = DailyListTestData.createDailyList(LocalTime.now(), String.valueOf(SourceType.CPP),
                swanseaCourtEntity, "tests/dailyListProcessorTest/dailyList.json");

        DailyListEntity oldDailyListEntity = DailyListTestData.createDailyList(LocalTime.now().minusHours(3),
                String.valueOf(SourceType.CPP), swanseaCourtEntity, "tests/dailyListProcessorTest/dailyList.json");

        dailyListRepository.saveAndFlush(dailyListEntity);
        dailyListRepository.saveAndFlush(oldDailyListEntity);

        dailyListProcessor.processAllDailyLists(LocalDate.now());

        CourtCaseEntity newCase1 = caseRepository.findByCaseNumberIgnoreCaseAndCourthouse_CourthouseNameIgnoreCase(CASE_NUMBER_1, SWANSEA).get();
        assertEquals(CASE_NUMBER_1, newCase1.getCaseNumber());
        assertEquals(SWANSEA, newCase1.getCourthouse().getCourthouseName());
        assertEquals(1, newCase1.getDefendantList().size());
        assertEquals(1, newCase1.getDefenceList().size());
        assertEquals(1, newCase1.getProsecutorList().size());
        assertEquals(1, newCase1.getJudges().size());
        assertEquals(1, newCase1.getHearings().size());


        HearingEntity newHearing1 = hearingRepository.findByCourthouseCourtroomAndDate(SWANSEA, COURTROOM_1, LocalDate.now()).get(0);
        assertEquals(LocalDate.now(), newHearing1.getHearingDate());
        assertEquals(COURTROOM_1, newHearing1.getCourtroom().getName());
        assertEquals(1, newHearing1.getJudges().size());

        CourtCaseEntity newCase2 = caseRepository.findByCaseNumberIgnoreCaseAndCourthouse_CourthouseNameIgnoreCase(CASE_NUMBER_2, SWANSEA).get();
        assertEquals(CASE_NUMBER_2, newCase2.getCaseNumber());
        assertEquals(SWANSEA, newCase2.getCourthouse().getCourthouseName());
        assertEquals(1, newCase2.getDefendantList().size());
        assertEquals(1, newCase2.getDefenceList().size());
        assertEquals(1, newCase2.getProsecutorList().size());
        assertEquals(1, newCase2.getJudges().size());
        assertEquals(1, newCase2.getHearings().size());


        HearingEntity newHearing2 = hearingRepository.findByCourthouseCourtroomAndDate(SWANSEA, COURTROOM_2, LocalDate.now()).get(0);
        assertEquals(LocalDate.now(), newHearing2.getHearingDate());
        assertEquals(COURTROOM_2, newHearing2.getCourtroom().getName());
        assertEquals(1, newHearing2.getJudges().size());
    }

}
