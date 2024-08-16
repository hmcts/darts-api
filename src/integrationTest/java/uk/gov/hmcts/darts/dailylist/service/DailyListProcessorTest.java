package uk.gov.hmcts.darts.dailylist.service;

import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.DailyListRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.dailylist.enums.JobStatusType;
import uk.gov.hmcts.darts.dailylist.enums.SourceType;
import uk.gov.hmcts.darts.log.util.DailyListLogJobReport;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.test.common.data.DailyListTestData;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static uk.gov.hmcts.darts.dailylist.enums.JobStatusType.FAILED;
import static uk.gov.hmcts.darts.dailylist.enums.JobStatusType.IGNORED;
import static uk.gov.hmcts.darts.dailylist.enums.JobStatusType.PARTIALLY_PROCESSED;
import static uk.gov.hmcts.darts.dailylist.enums.JobStatusType.PROCESSED;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;

@Slf4j
@Disabled("Impacted by V1_364_*.sql")
class DailyListProcessorTest extends IntegrationBase {

    public static final String SWANSEA = "SWANSEA";
    public static final String URN_1 = "42GD2391421";
    public static final String URN_2 = "42GD2391433";
    public static final String COURTROOM_1 = "1A";
    public static final String COURTROOM_2 = "2B";
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
        log.info("start dailyListProcessorMultipleDailyList");
        CourthouseEntity swanseaCourtEntity = dartsDatabase.createCourthouseWithTwoCourtrooms();
        LocalTime dailyListTIme = LocalTime.of(13, 0);
        DailyListEntity dailyListEntity = DailyListTestData.createDailyList(
            dailyListTIme,
            String.valueOf(SourceType.CPP),
            swanseaCourtEntity.getCourthouseName(),
            "tests/dailyListProcessorTest/dailyListCPP.json"
        );

        DailyListEntity oldDailyListEntity = DailyListTestData.createDailyList(
            dailyListTIme.minusHours(3),
            String.valueOf(SourceType.CPP),
            swanseaCourtEntity.getCourthouseName(),
            "tests/dailyListProcessorTest/dailyListCPP.json"
        );

        dailyListRepository.saveAllAndFlush(List.of(dailyListEntity, oldDailyListEntity));

        dailyListProcessor.processAllDailyLists();

        DailyListLogJobReport report = new DailyListLogJobReport(2, SourceType.CPP);
        report.registerResult(PROCESSED);
        report.registerResult(IGNORED);

        List<DailyListEntity> savedDailyLists = dailyListRepository.findAll();
        List<JobStatusType> dailyListStatuses = savedDailyLists.stream().map(dailyList -> dailyList.getStatus()).toList();
        long countOfProcessed = dailyListStatuses.stream().filter(status -> status.equals(PROCESSED)).count();
        assertEquals(1, countOfProcessed);
        long countOfIgnored = dailyListStatuses.stream().filter(status -> status.equals(IGNORED)).count();
        assertEquals(1, countOfIgnored);
        assertFalse(logAppender.searchLogApiLogs(report.toString(), Level.toLevel(Level.INFO_INT)).isEmpty());

        CourtCaseEntity newCase1 = caseRepository.findByCaseNumberAndCourthouse_CourthouseNameIgnoreCase(URN_1, SWANSEA).get();
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
        assertEquals(LocalTime.of(11, 0), newHearing1.getScheduledStartTime());

        CourtCaseEntity newCase2 = caseRepository.findByCaseNumberAndCourthouse_CourthouseNameIgnoreCase(URN_2, SWANSEA).get();
        assertEquals(URN_2, newCase2.getCaseNumber());
        assertEquals(SWANSEA, newCase2.getCourthouse().getCourthouseName());
        assertEquals(1, newCase2.getDefendantList().size());
        assertEquals(1, newCase2.getDefenceList().size());
        assertEquals(1, newCase2.getProsecutorList().size());
        assertEquals(1, newCase2.getJudges().size());

        List<HearingEntity> newHearing2 = hearingRepository.findByCourthouseCourtroomAndDate(SWANSEA, COURTROOM_2, LocalDate.now());
        assertEquals(1, newHearing2.size());
        assertEquals(LocalDate.now(), newHearing2.get(0).getHearingDate());
        assertEquals(COURTROOM_2, newHearing2.get(0).getCourtroom().getName());
        assertEquals(1, newHearing2.get(0).getJudges().size());
        assertEquals(LocalTime.of(16, 0), newHearing2.get(0).getScheduledStartTime());
        log.info("end dailyListProcessorMultipleDailyList");
    }

    @Test
    void dailyListForListingCourthouseWithIgnore() throws IOException {
        log.info("start dailyListProcessorMultipleDailyList");
        CourthouseEntity swanseaCourtEntity = dartsDatabase.createCourthouseWithTwoCourtrooms();
        LocalTime dailyListTIme = LocalTime.of(13, 0);
        DailyListEntity dailyListEntity = DailyListTestData.createDailyList(
            dailyListTIme,
            String.valueOf(SourceType.CPP),
            swanseaCourtEntity.getCourthouseName(),
            "tests/dailyListProcessorTest/dailyListCPP.json"
        );

        DailyListEntity oldDailyListEntity = DailyListTestData.createDailyList(
            dailyListTIme.minusHours(3),
            String.valueOf(SourceType.CPP),
            swanseaCourtEntity.getCourthouseName(),
            "tests/dailyListProcessorTest/dailyListCPP.json"
        );

        dailyListRepository.saveAllAndFlush(List.of(dailyListEntity, oldDailyListEntity));

        dailyListProcessor.processAllDailyListForListingCourthouse(swanseaCourtEntity.getCourthouseName());

        DailyListLogJobReport report = new DailyListLogJobReport(2, SourceType.CPP);
        report.registerResult(PROCESSED);
        report.registerResult(IGNORED);

        assertEquals(1, logAppender.searchLogApiLogs(report.toString(), Level.toLevel(Level.INFO_INT)).size());

        CourtCaseEntity newCase1 = caseRepository.findByCaseNumberAndCourthouse_CourthouseNameIgnoreCase(URN_1, SWANSEA).get();
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
        assertEquals(LocalTime.of(11, 0), newHearing1.getScheduledStartTime());

        CourtCaseEntity newCase2 = caseRepository.findByCaseNumberAndCourthouse_CourthouseNameIgnoreCase(URN_2, SWANSEA).get();
        assertEquals(URN_2, newCase2.getCaseNumber());
        assertEquals(SWANSEA, newCase2.getCourthouse().getCourthouseName());
        assertEquals(1, newCase2.getDefendantList().size());
        assertEquals(1, newCase2.getDefenceList().size());
        assertEquals(1, newCase2.getProsecutorList().size());
        assertEquals(1, newCase2.getJudges().size());


        List<HearingEntity> newHearing2 = hearingRepository.findByCourthouseCourtroomAndDate(SWANSEA, COURTROOM_2, LocalDate.now());
        assertEquals(1, newHearing2.size());
        assertEquals(LocalDate.now(), newHearing2.get(0).getHearingDate());
        assertEquals(COURTROOM_2, newHearing2.get(0).getCourtroom().getName());
        assertEquals(1, newHearing2.get(0).getJudges().size());
        assertEquals(LocalTime.of(16, 0), newHearing2.get(0).getScheduledStartTime());
        log.info("end dailyListProcessorMultipleDailyList");

    }

    @Test
    void dailyListProcessorCppAndXhbDailyLists() throws IOException {
        CourthouseEntity swanseaCourtEntity = dartsDatabase.createCourthouseWithTwoCourtrooms();

        dartsDatabase.createDailyLists(swanseaCourtEntity.getCourthouseName());

        dailyListProcessor.processAllDailyLists();

        DailyListLogJobReport reportCpp = new DailyListLogJobReport(1, SourceType.CPP);
        reportCpp.registerResult(PROCESSED);

        DailyListLogJobReport reportXhb = new DailyListLogJobReport(1, SourceType.XHB);
        reportXhb.registerResult(PROCESSED);

        CourtCaseEntity newCase1 = caseRepository
            .findByCaseNumberAndCourthouse_CourthouseNameIgnoreCase(URN_1, SWANSEA).get();

        assertEquals(1, logAppender.searchLogApiLogs(reportCpp.toString(), Level.toLevel(Level.INFO_INT)).size());
        assertEquals(1, logAppender.searchLogApiLogs(reportXhb.toString(), Level.toLevel(Level.INFO_INT)).size());

        assertEquals(URN_1, newCase1.getCaseNumber());

        assertEquals(SWANSEA, newCase1.getCourthouse().getCourthouseName());
        assertEquals(1, newCase1.getDefendantList().size());
        assertEquals(1, newCase1.getDefenceList().size());
        assertEquals(1, newCase1.getProsecutorList().size());
        assertEquals(1, newCase1.getJudges().size());

        CourtCaseEntity newCase2 = caseRepository
            .findByCaseNumberAndCourthouse_CourthouseNameIgnoreCase(URN_2, SWANSEA).get();
        assertEquals(URN_2, newCase2.getCaseNumber());
        assertEquals(SWANSEA, newCase2.getCourthouse().getCourthouseName());
        assertEquals(1, newCase2.getDefendantList().size());
        assertEquals(1, newCase2.getDefenceList().size());
        assertEquals(1, newCase2.getProsecutorList().size());
        assertEquals(1, newCase2.getJudges().size());


        CourtCaseEntity newCase3 = caseRepository.findByCaseNumberAndCourthouse_CourthouseNameIgnoreCase(CASE_NUMBER_1, SWANSEA).get();
        assertEquals(CASE_NUMBER_1, newCase3.getCaseNumber());
        assertEquals(SWANSEA, newCase3.getCourthouse().getCourthouseName());
        assertEquals(1, newCase3.getDefendantList().size());
        assertEquals(1, newCase3.getDefenceList().size());
        assertEquals(1, newCase3.getProsecutorList().size());
        assertEquals(1, newCase3.getJudges().size());


        CourtCaseEntity newCase4 = caseRepository.findByCaseNumberAndCourthouse_CourthouseNameIgnoreCase(CASE_NUMBER_2, SWANSEA).get();
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

            assertThat(
                hearing.getScheduledStartTime(),
                Matchers.either(Matchers.is(LocalTime.of(16, 0))).or(Matchers.is(LocalTime.of(11, 0)))
            );

        }

    }

    @Test
    void setsDailyListStatusToFailedIfUpdateFails() {
        var dailyListEntity = DailyListTestData.minimalDailyList();
        dailyListEntity.setListingCourthouse("some-courthouse");
        dailyListEntity.setStartDate(LocalDate.now());
        dailyListEntity.setSource("CPP");

        var dailyListEntities = dartsDatabase.saveAll(dailyListEntity);

        dailyListProcessor.processAllDailyLists();

        int id = dailyListEntities.get(0).getId();

        DailyListLogJobReport report = new DailyListLogJobReport(1, SourceType.CPP);
        report.registerResult(FAILED);
        assertEquals(1, logAppender.searchLogApiLogs(report.toString(), Level.toLevel(Level.INFO_INT)).size());

        var dailyListStatus = dartsDatabase.getDailyListRepository()
            .findById(id).orElseThrow()
            .getStatus();

        Assertions.assertThat(dailyListStatus).isEqualTo(FAILED);
    }

    @Test
    void setsDailyListStatusToIgnoredIfNotLatest() throws IOException {
        var latestDailyList = DailyListTestData.minimalDailyList();
        latestDailyList.setListingCourthouse("some-courthouse");
        latestDailyList.setPublishedTimestamp(OffsetDateTime.now());
        latestDailyList.setStartDate(LocalDate.now());
        latestDailyList.setContent(TestUtils.substituteHearingDateWithToday(getContentsFromFile("tests/dailyListProcessorTest/dailyListCPP.json")));
        latestDailyList.setSource("CPP");

        var oldDailyList = DailyListTestData.minimalDailyList();
        oldDailyList.setListingCourthouse("some-courthouse");
        oldDailyList.setPublishedTimestamp(OffsetDateTime.now().minusHours(1));
        oldDailyList.setStartDate(LocalDate.now());
        oldDailyList.setSource("CPP");
        dartsDatabase.saveAll(latestDailyList, oldDailyList);

        dailyListProcessor.processAllDailyLists();

        DailyListLogJobReport report = new DailyListLogJobReport(2, SourceType.CPP);
        report.registerResult(PARTIALLY_PROCESSED);
        report.registerResult(IGNORED);

        assertEquals(1, logAppender.searchLogApiLogs(report.toString(), Level.toLevel(Level.INFO_INT)).size());

        var dailyListStatus = dartsDatabase.getDailyListRepository()
            .findById(oldDailyList.getId()).orElseThrow()
            .getStatus();

        Assertions.assertThat(dailyListStatus).isEqualTo(IGNORED);
    }
}