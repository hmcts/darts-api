package uk.gov.hmcts.darts.dailylist.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.common.entity.DefenceEntity;
import uk.gov.hmcts.darts.common.entity.DefendantEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.ProsecutorEntity;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.DailyListRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.dailylist.enums.JobStatusType;
import uk.gov.hmcts.darts.dailylist.enums.SourceType;
import uk.gov.hmcts.darts.dailylist.service.impl.DailyListProcessorImpl;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.ExcessiveImports"})
class DailyListProcessorImplTest {

    public static final String EDINBURGH = "EDINBURGH";
    public static final String CASE_NUMBER = "Case1";
    public static final String COURTROOM = "1";
    public static final String SWANSEA = "SWANSEA";
    public static final String URN = "42GD2391421";
    public static final String NON_EXISTING_COURTHOUSE = "MOON";
    public static final String DAILYLIST_LOCATION = "Tests/dailylist/dailyListProcessorTest/dailyList.json";
    @Mock
    private CourthouseRepository courthouseRepository;
    @Mock
    private DailyListRepository dailyListRepository;
    @Mock
    private HearingRepository hearingRepository;
    @Mock
    private RetrieveCoreObjectService retrieveCoreObjectService;
    private DailyListProcessor dailyListProcessor;
    @Captor
    private ArgumentCaptor<HearingEntity> hearingEntityArgumentCaptor;

    public static JudgeEntity createJudgeWithName(String name) {
        var judgeEntity = new JudgeEntity();
        judgeEntity.setName(name);
        return judgeEntity;
    }

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        dailyListProcessor = new DailyListProcessorImpl(dailyListRepository, retrieveCoreObjectService,
                                                        courthouseRepository, hearingRepository, objectMapper
        );
    }

    @Test
    void processSingleDailyListsWithMultipleCourthouses() throws IOException {
        CourthouseEntity edinburgh = CommonTestDataUtil.createCourthouse(EDINBURGH);
        edinburgh.setId(1);

        CourtroomEntity edinburghCourtroom = CommonTestDataUtil.createCourtroom(edinburgh, COURTROOM);
        edinburgh.setCourtrooms(List.of(edinburghCourtroom));


        CourthouseEntity swansea = CommonTestDataUtil.createCourthouse(SWANSEA);
        swansea.setId(2);

        CourtroomEntity swanseaCourtroom = CommonTestDataUtil.createCourtroom(swansea, COURTROOM);
        swansea.setCourtrooms(List.of(swanseaCourtroom));

        CourtCaseEntity courtCase = CommonTestDataUtil.createCase(CASE_NUMBER, swansea);


        List<CourthouseEntity> courthouses = List.of(swansea, edinburgh);

        Mockito.when(courthouseRepository.findAll()).thenReturn(courthouses);
        Mockito.when(courthouseRepository.findByCourthouseNameIgnoreCase(SWANSEA)).thenReturn(Optional.of(swansea));

        HearingEntity hearing = CommonTestDataUtil.createHearing(courtCase, swanseaCourtroom, LocalDate.now());
        Mockito.when(retrieveCoreObjectService.retrieveOrCreateHearing(any(), any(), any(), any())).thenReturn(hearing);

        Mockito.when(retrieveCoreObjectService.retrieveOrCreateJudge(anyString()))
            .thenReturn(createJudgeWithName("JudgeName Surname"));
        Mockito.when(retrieveCoreObjectService.createDefendant(anyString(), any()))
            .thenReturn(createDefendantForCaseWithName(courtCase, "DefendantName Surname"));
        Mockito.when(retrieveCoreObjectService.createDefence(anyString(), any()))
            .thenReturn(createDefenceForCaseWithName(courtCase, "DefenceName Surname"));
        Mockito.when(retrieveCoreObjectService.createProsecutor(anyString(), any()))
            .thenReturn(createProsecutorForCaseWithName(courtCase, "ProsecutorName Surname"));

        List<DailyListEntity> dailyListEntities = List.of(CommonTestDataUtil.createDailyList(
            LocalTime.now(),
            String.valueOf(SourceType.XHB),
            DAILYLIST_LOCATION
        ));
        Mockito.when(dailyListRepository
                         .findByCourthouse_IdAndStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(
                             1, JobStatusType.NEW, LocalDate.now(), String.valueOf(SourceType.XHB)))
            .thenReturn(dailyListEntities);

        Mockito.when(dailyListRepository
                         .findByCourthouse_IdAndStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(
                             2, JobStatusType.NEW, LocalDate.now(), String.valueOf(SourceType.XHB)))
            .thenReturn(Collections.emptyList());

        Mockito.when(dailyListRepository
                         .findByCourthouse_IdAndStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(
                             1, JobStatusType.NEW, LocalDate.now(), String.valueOf(SourceType.CPP)))
            .thenReturn(Collections.emptyList());

        Mockito.when(dailyListRepository
                         .findByCourthouse_IdAndStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(
                             2, JobStatusType.NEW, LocalDate.now(), String.valueOf(SourceType.CPP)))
            .thenReturn(Collections.emptyList());

        dailyListProcessor.processAllDailyLists(LocalDate.now());

        Mockito.verify(hearingRepository).saveAndFlush(hearingEntityArgumentCaptor.capture());


        CourtCaseEntity savedCase = hearingEntityArgumentCaptor.getValue().getCourtCase();
        assertEquals(3, savedCase.getDefenceList().size());
        assertEquals(1, savedCase.getJudges().size());
        assertEquals(3, savedCase.getProsecutorList().size());
        assertEquals(3, savedCase.getDefendantList().size());
        assertEquals(CASE_NUMBER, savedCase.getCaseNumber());
        assertEquals(SWANSEA, courtCase.getCourthouse().getCourthouseName());


        HearingEntity savedHearing = hearingEntityArgumentCaptor.getValue();
        assertEquals(LocalDate.now(), savedHearing.getHearingDate());
        assertEquals(LocalTime.of(11, 0), savedHearing.getScheduledStartTime());
        assertEquals("1", savedHearing.getCourtroom().getName());
        assertEquals(swanseaCourtroom, savedHearing.getCourtroom());

        assertEquals(JobStatusType.PROCESSED, dailyListEntities.get(0).getStatus());
    }


    @Test
    void processMultipleDailyListForSameCourthouse() throws IOException {

        CourthouseEntity swansea = CommonTestDataUtil.createCourthouse(SWANSEA);
        swansea.setId(1);

        CourtroomEntity swanseaCourtroom = CommonTestDataUtil.createCourtroom(swansea, COURTROOM);
        swansea.setCourtrooms(List.of(swanseaCourtroom));

        CourtCaseEntity courtCase = CommonTestDataUtil.createCase(CASE_NUMBER, swansea);


        List<CourthouseEntity> courthouses = List.of(swansea);

        Mockito.when(courthouseRepository.findAll()).thenReturn(courthouses);
        Mockito.when(courthouseRepository.findByCourthouseNameIgnoreCase(SWANSEA)).thenReturn(Optional.of(swansea));

        HearingEntity hearing = CommonTestDataUtil.createHearing(courtCase, swanseaCourtroom, LocalDate.now());
        Mockito.when(retrieveCoreObjectService.retrieveOrCreateHearing(any(), any(), any(), any())).thenReturn(hearing);

        Mockito.when(retrieveCoreObjectService.retrieveOrCreateJudge(anyString()))
            .thenReturn(createJudgeWithName("JudgeName Surname"));
        Mockito.when(retrieveCoreObjectService.createDefendant(anyString(), any()))
            .thenReturn(createDefendantForCaseWithName(courtCase, "DefendantName Surname"));
        Mockito.when(retrieveCoreObjectService.createDefence(anyString(), any()))
            .thenReturn(createDefenceForCaseWithName(courtCase, "DefenceName Surname"));
        Mockito.when(retrieveCoreObjectService.createProsecutor(anyString(), any()))
            .thenReturn(createProsecutorForCaseWithName(courtCase, "ProsecutorName Surname"));

        DailyListEntity oldDailyList = CommonTestDataUtil.createDailyList(
            LocalTime.now().minusHours(4),
            String.valueOf(SourceType.XHB),
            DAILYLIST_LOCATION
        );

        List<DailyListEntity> dailyListEntities = List.of(CommonTestDataUtil.createDailyList(
            LocalTime.now(),
            String.valueOf(SourceType.XHB),
            DAILYLIST_LOCATION
        ), oldDailyList);

        Mockito.when(dailyListRepository
                         .findByCourthouse_IdAndStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(
                             1, JobStatusType.NEW, LocalDate.now(), String.valueOf(SourceType.XHB)))
            .thenReturn(dailyListEntities);

        Mockito.when(dailyListRepository
                         .findByCourthouse_IdAndStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(
                             1, JobStatusType.NEW, LocalDate.now(), String.valueOf(SourceType.CPP)))
            .thenReturn(Collections.emptyList());

        dailyListProcessor.processAllDailyLists(LocalDate.now());

        Mockito.verify(hearingRepository).saveAndFlush(hearingEntityArgumentCaptor.capture());


        CourtCaseEntity savedCase = hearingEntityArgumentCaptor.getValue().getCourtCase();
        assertEquals(3, savedCase.getDefenceList().size());
        assertEquals(1, savedCase.getJudges().size());
        assertEquals(3, savedCase.getProsecutorList().size());
        assertEquals(3, savedCase.getDefendantList().size());
        assertEquals(CASE_NUMBER, savedCase.getCaseNumber());
        assertEquals(SWANSEA, courtCase.getCourthouse().getCourthouseName());


        HearingEntity savedHearing = hearingEntityArgumentCaptor.getValue();
        assertEquals(LocalDate.now(), savedHearing.getHearingDate());
        assertEquals("1", savedHearing.getCourtroom().getName());
        assertEquals(swanseaCourtroom, savedHearing.getCourtroom());
        assertEquals(JobStatusType.IGNORED, oldDailyList.getStatus());
        assertEquals(1, savedHearing.getJudges().size());
        assertEquals(LocalTime.of(11, 0), savedHearing.getScheduledStartTime());

        assertEquals(JobStatusType.PROCESSED, dailyListEntities.get(0).getStatus());
        assertEquals(JobStatusType.IGNORED, dailyListEntities.get(1).getStatus());
    }

    @Test
    void processMultipleDailyListForCpp() throws IOException {
        CourthouseEntity edinburgh = CommonTestDataUtil.createCourthouse(EDINBURGH);
        edinburgh.setId(1);

        CourtroomEntity edinburghCourtroom = CommonTestDataUtil.createCourtroom(edinburgh, COURTROOM);
        edinburgh.setCourtrooms(List.of(edinburghCourtroom));


        CourthouseEntity swansea = CommonTestDataUtil.createCourthouse(SWANSEA);
        swansea.setId(2);

        CourtroomEntity swanseaCourtroom = CommonTestDataUtil.createCourtroom(swansea, COURTROOM);
        swansea.setCourtrooms(List.of(swanseaCourtroom));

        CourtCaseEntity courtCase = CommonTestDataUtil.createCase(URN, swansea);


        List<CourthouseEntity> courthouses = List.of(swansea, edinburgh);

        Mockito.when(courthouseRepository.findAll()).thenReturn(courthouses);
        Mockito.when(courthouseRepository.findByCourthouseNameIgnoreCase(SWANSEA)).thenReturn(Optional.of(swansea));

        HearingEntity hearing = CommonTestDataUtil.createHearing(courtCase, swanseaCourtroom, LocalDate.now());
        Mockito.when(retrieveCoreObjectService.retrieveOrCreateHearing(any(), any(), any(), any())).thenReturn(hearing);

        Mockito.when(retrieveCoreObjectService.retrieveOrCreateJudge(anyString()))
            .thenReturn(createJudgeWithName("JudgeName Surname"));
        Mockito.when(retrieveCoreObjectService.createDefendant(anyString(), any()))
            .thenReturn(createDefendantForCaseWithName(courtCase, "DefendantName Surname"));
        Mockito.when(retrieveCoreObjectService.createDefence(anyString(), any()))
            .thenReturn(createDefenceForCaseWithName(courtCase, "DefenceName Surname"));
        Mockito.when(retrieveCoreObjectService.createProsecutor(anyString(), any()))
            .thenReturn(createProsecutorForCaseWithName(courtCase, "ProsecutorName Surname"));

        DailyListEntity oldDailyList = CommonTestDataUtil.createDailyList(
            LocalTime.now().minusHours(4),
            String.valueOf(SourceType.CPP),
            DAILYLIST_LOCATION
        );
        List<DailyListEntity> dailyListEntities = List.of(CommonTestDataUtil.createDailyList(
            LocalTime.now(),
            String.valueOf(SourceType.CPP),
            DAILYLIST_LOCATION
        ), oldDailyList);

        Mockito.when(dailyListRepository
                         .findByCourthouse_IdAndStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(
                             1, JobStatusType.NEW, LocalDate.now(), String.valueOf(SourceType.CPP)))
            .thenReturn(dailyListEntities);

        Mockito.when(dailyListRepository
                         .findByCourthouse_IdAndStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(
                             2, JobStatusType.NEW, LocalDate.now(), String.valueOf(SourceType.CPP)))
            .thenReturn(Collections.emptyList());


        Mockito.when(dailyListRepository
                         .findByCourthouse_IdAndStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(
                             1, JobStatusType.NEW, LocalDate.now(), String.valueOf(SourceType.XHB)))
            .thenReturn(dailyListEntities);

        Mockito.when(dailyListRepository
                         .findByCourthouse_IdAndStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(
                             2, JobStatusType.NEW, LocalDate.now(), String.valueOf(SourceType.XHB)))
            .thenReturn(Collections.emptyList());


        dailyListProcessor.processAllDailyLists(LocalDate.now());

        Mockito.verify(hearingRepository, Mockito.times(2)).saveAndFlush(hearingEntityArgumentCaptor.capture());


        CourtCaseEntity savedCase = hearingEntityArgumentCaptor.getValue().getCourtCase();
        assertEquals(3, savedCase.getDefenceList().size());
        assertEquals(1, savedCase.getJudges().size());
        assertEquals(3, savedCase.getProsecutorList().size());
        assertEquals(3, savedCase.getDefendantList().size());
        assertEquals(URN, savedCase.getCaseNumber());
        assertEquals(SWANSEA, courtCase.getCourthouse().getCourthouseName());

        HearingEntity savedHearing = hearingEntityArgumentCaptor.getValue();
        assertEquals(LocalDate.now(), savedHearing.getHearingDate());
        assertEquals("1", savedHearing.getCourtroom().getName());
        assertEquals(swanseaCourtroom, savedHearing.getCourtroom());
        assertEquals(JobStatusType.IGNORED, oldDailyList.getStatus());
        assertEquals(1, savedHearing.getJudges().size());
        assertEquals(LocalTime.of(11, 0), savedHearing.getScheduledStartTime());

        assertEquals(JobStatusType.PROCESSED, dailyListEntities.get(0).getStatus());
        assertEquals(JobStatusType.IGNORED, dailyListEntities.get(1).getStatus());
    }


    @Test
    void processAllSingleDailyListsWithMissingTimeMarkingNote() throws IOException {


        CourthouseEntity swansea = CommonTestDataUtil.createCourthouse(SWANSEA);
        swansea.setId(1);

        CourtroomEntity swanseaCourtroom = CommonTestDataUtil.createCourtroom(swansea, COURTROOM);
        swansea.setCourtrooms(List.of(swanseaCourtroom));

        CourtCaseEntity courtCase = CommonTestDataUtil.createCase(CASE_NUMBER, swansea);


        List<CourthouseEntity> courthouses = List.of(swansea);

        Mockito.when(courthouseRepository.findAll()).thenReturn(courthouses);
        Mockito.when(courthouseRepository.findByCourthouseNameIgnoreCase(SWANSEA)).thenReturn(Optional.of(swansea));

        HearingEntity hearing = CommonTestDataUtil.createHearing(courtCase, swanseaCourtroom, LocalDate.now());
        Mockito.when(retrieveCoreObjectService.retrieveOrCreateHearing(any(), any(), any(), any())).thenReturn(hearing);

        Mockito.when(retrieveCoreObjectService.retrieveOrCreateJudge(anyString()))
            .thenReturn(createJudgeWithName("JudgeName Surname"));
        Mockito.when(retrieveCoreObjectService.createDefendant(anyString(), any()))
            .thenReturn(createDefendantForCaseWithName(courtCase, "DefendantName Surname"));
        Mockito.when(retrieveCoreObjectService.createDefence(anyString(), any()))
            .thenReturn(createDefenceForCaseWithName(courtCase, "DefenceName Surname"));
        Mockito.when(retrieveCoreObjectService.createProsecutor(anyString(), any()))
            .thenReturn(createProsecutorForCaseWithName(courtCase, "ProsecutorName Surname"));

        List<DailyListEntity> dailyListEntities = List.of(
            CommonTestDataUtil.createDailyList(
                LocalTime.now(),
                String.valueOf(SourceType.XHB),
                "Tests/dailylist/dailyListProcessorTest/dailyListMissingTimeMarkNote.json"
            ));
        Mockito.when(dailyListRepository
                         .findByCourthouse_IdAndStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(
                             1,
                             JobStatusType.NEW,
                             LocalDate.now(),
                             String.valueOf(
                                 SourceType.XHB)
                         ))
            .thenReturn(dailyListEntities);

        Mockito.when(dailyListRepository
                         .findByCourthouse_IdAndStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(
                             1, JobStatusType.NEW, LocalDate.now(), String.valueOf(SourceType.CPP)))
            .thenReturn(Collections.emptyList());

        dailyListProcessor.processAllDailyLists(LocalDate.now());

        Mockito.verify(hearingRepository).saveAndFlush(hearingEntityArgumentCaptor.capture());


        CourtCaseEntity savedCase = hearingEntityArgumentCaptor.getValue().getCourtCase();
        assertEquals(3, savedCase.getDefenceList().size());
        assertEquals(1, savedCase.getJudges().size());
        assertEquals(3, savedCase.getProsecutorList().size());
        assertEquals(3, savedCase.getDefendantList().size());
        assertEquals(CASE_NUMBER, savedCase.getCaseNumber());
        assertEquals(SWANSEA, courtCase.getCourthouse().getCourthouseName());


        HearingEntity savedHearing = hearingEntityArgumentCaptor.getValue();
        assertEquals(LocalDate.now(), savedHearing.getHearingDate());
        assertEquals(LocalTime.of(11, 0), hearing.getScheduledStartTime());
        assertEquals("1", savedHearing.getCourtroom().getName());
        assertEquals(swanseaCourtroom, savedHearing.getCourtroom());

        assertEquals(JobStatusType.PROCESSED, dailyListEntities.get(0).getStatus());
    }


    @Test
    void processDailyListWithNonExistingCourthouse() throws IOException {

        CourthouseEntity swansea = CommonTestDataUtil.createCourthouse(SWANSEA);
        swansea.setId(1);

        CourtroomEntity swanseaCourtroom = CommonTestDataUtil.createCourtroom(swansea, COURTROOM);
        swansea.setCourtrooms(List.of(swanseaCourtroom));


        List<CourthouseEntity> courthouses = List.of(swansea);

        Mockito.when(courthouseRepository.findAll()).thenReturn(courthouses);
        Mockito.when(courthouseRepository.findByCourthouseNameIgnoreCase(NON_EXISTING_COURTHOUSE))
            .thenReturn(Optional.empty());


        List<DailyListEntity> dailyListEntities = List.of(CommonTestDataUtil.createDailyList(
            LocalTime.now(),
            String.valueOf(SourceType.XHB),
            "Tests/dailylist/dailyListProcessorTest/dailyList_NonExistingCourthouse.json"
        ));

        Mockito.when(dailyListRepository
                         .findByCourthouse_IdAndStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(
                             1, JobStatusType.NEW, LocalDate.now(), String.valueOf(SourceType.XHB)))
            .thenReturn(dailyListEntities);

        Mockito.when(dailyListRepository
                         .findByCourthouse_IdAndStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(
                             1, JobStatusType.NEW, LocalDate.now(), String.valueOf(SourceType.CPP)))
            .thenReturn(dailyListEntities);

        dailyListProcessor.processAllDailyLists(LocalDate.now());

        Mockito.verify(hearingRepository, Mockito.never()).saveAndFlush(any());

        assertEquals(JobStatusType.PARTIALLY_PROCESSED, dailyListEntities.get(0).getStatus());
    }


    @Test
    void processDailyListWithInvalidContent() {

        CourthouseEntity swansea = CommonTestDataUtil.createCourthouse(SWANSEA);
        swansea.setId(1);

        CourtroomEntity swanseaCourtroom = CommonTestDataUtil.createCourtroom(swansea, COURTROOM);
        swansea.setCourtrooms(List.of(swanseaCourtroom));

        List<CourthouseEntity> courthouses = List.of(swansea);

        Mockito.when(courthouseRepository.findAll()).thenReturn(courthouses);

        DailyListEntity invalidDailyList = CommonTestDataUtil.createInvalidDailyList(LocalTime.now());
        Mockito.when(dailyListRepository
                         .findByCourthouse_IdAndStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(
                             1, JobStatusType.NEW, LocalDate.now(), String.valueOf(SourceType.XHB)))
            .thenReturn(List.of(invalidDailyList));

        Mockito.when(dailyListRepository
                         .findByCourthouse_IdAndStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(
                             1, JobStatusType.NEW, LocalDate.now(), String.valueOf(SourceType.CPP)))
            .thenReturn(Collections.emptyList());

        dailyListProcessor.processAllDailyLists(LocalDate.now());

        Mockito.verify(hearingRepository, Mockito.never()).saveAndFlush(hearingEntityArgumentCaptor.capture());
        assertEquals(JobStatusType.FAILED, invalidDailyList.getStatus());
    }

    @Test
    void processDailyListWithInvalidXmlContent() {

        CourthouseEntity swansea = CommonTestDataUtil.createCourthouse(SWANSEA);
        swansea.setId(1);

        CourtroomEntity swanseaCourtroom = CommonTestDataUtil.createCourtroom(swansea, COURTROOM);
        swansea.setCourtrooms(List.of(swanseaCourtroom));

        List<CourthouseEntity> courthouses = List.of(swansea);

        Mockito.when(courthouseRepository.findAll()).thenReturn(courthouses);

        DailyListEntity invalidDailyList = CommonTestDataUtil.createInvalidXmlDailyList(LocalTime.now());
        Mockito.when(dailyListRepository
                         .findByCourthouse_IdAndStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(
                             1, JobStatusType.NEW, LocalDate.now(), String.valueOf(SourceType.XHB)))
            .thenReturn(List.of(invalidDailyList));

        Mockito.when(dailyListRepository
                         .findByCourthouse_IdAndStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(
                             1, JobStatusType.NEW, LocalDate.now(), String.valueOf(SourceType.CPP)))
            .thenReturn(Collections.emptyList());

        dailyListProcessor.processAllDailyLists(LocalDate.now());

        Mockito.verify(hearingRepository, Mockito.never()).saveAndFlush(hearingEntityArgumentCaptor.capture());
        assertEquals(JobStatusType.FAILED, invalidDailyList.getStatus());
    }

    @Test
    void processSingleDailyListsForSingleCourthouse() throws IOException {

        CourthouseEntity swansea = CommonTestDataUtil.createCourthouse(SWANSEA);
        swansea.setId(1);

        CourtroomEntity swanseaCourtroom = CommonTestDataUtil.createCourtroom(swansea, COURTROOM);
        swansea.setCourtrooms(List.of(swanseaCourtroom));

        CourtCaseEntity courtCase = CommonTestDataUtil.createCase(CASE_NUMBER, swansea);

        Mockito.when(courthouseRepository.findByCourthouseNameIgnoreCase(SWANSEA)).thenReturn(Optional.of(swansea));

        HearingEntity hearing = CommonTestDataUtil.createHearing(courtCase, swanseaCourtroom, LocalDate.now());
        Mockito.when(retrieveCoreObjectService.retrieveOrCreateHearing(any(), any(), any(), any())).thenReturn(hearing);

        Mockito.when(retrieveCoreObjectService.retrieveOrCreateJudge(anyString()))
            .thenReturn(createJudgeWithName("JudgeName Surname"));
        Mockito.when(retrieveCoreObjectService.createDefendant(anyString(), any()))
            .thenReturn(createDefendantForCaseWithName(courtCase, "DefendantName Surname"));
        Mockito.when(retrieveCoreObjectService.createDefence(anyString(), any()))
            .thenReturn(createDefenceForCaseWithName(courtCase, "DefenceName Surname"));
        Mockito.when(retrieveCoreObjectService.createProsecutor(anyString(), any()))
            .thenReturn(createProsecutorForCaseWithName(courtCase, "ProsecutorName Surname"));


        List<DailyListEntity> dailyListEntities = List.of(CommonTestDataUtil.createDailyList(
            LocalTime.now(),
            String.valueOf(SourceType.XHB),
            DAILYLIST_LOCATION
        ));
        Mockito.when(dailyListRepository
                         .findByCourthouse_IdAndStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(
                             1, JobStatusType.NEW, LocalDate.now(), String.valueOf(SourceType.XHB)))
            .thenReturn(dailyListEntities);

        Mockito.when(dailyListRepository
                         .findByCourthouse_IdAndStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(
                             1, JobStatusType.NEW, LocalDate.now(), String.valueOf(SourceType.CPP)))
            .thenReturn(Collections.emptyList());

        dailyListProcessor.processAllDailyListForCourthouse(swansea);

        Mockito.verify(hearingRepository).saveAndFlush(hearingEntityArgumentCaptor.capture());


        CourtCaseEntity savedCase = hearingEntityArgumentCaptor.getValue().getCourtCase();
        assertEquals(3, savedCase.getDefenceList().size());
        assertEquals(1, savedCase.getJudges().size());
        assertEquals(3, savedCase.getProsecutorList().size());
        assertEquals(3, savedCase.getDefendantList().size());
        assertEquals(CASE_NUMBER, savedCase.getCaseNumber());
        assertEquals(SWANSEA, courtCase.getCourthouse().getCourthouseName());


        HearingEntity savedHearing = hearingEntityArgumentCaptor.getValue();
        assertEquals(LocalDate.now(), savedHearing.getHearingDate());
        assertEquals(LocalTime.of(11, 0), savedHearing.getScheduledStartTime());
        assertEquals("1", savedHearing.getCourtroom().getName());
        assertEquals(swanseaCourtroom, savedHearing.getCourtroom());

        assertEquals(JobStatusType.PROCESSED, dailyListEntities.get(0).getStatus());
    }


    private DefendantEntity createDefendantForCaseWithName(CourtCaseEntity courtCase, String name) {
        var defendant = new DefendantEntity();
        defendant.setCourtCase(courtCase);
        defendant.setName(name);
        return defendant;
    }

    private DefenceEntity createDefenceForCaseWithName(CourtCaseEntity courtCase, String name) {
        var defence = new DefenceEntity();
        defence.setCourtCase(courtCase);
        defence.setName(name);
        return defence;
    }

    private ProsecutorEntity createProsecutorForCaseWithName(CourtCaseEntity courtCase, String name) {
        var prosecutor = new ProsecutorEntity();
        prosecutor.setCourtCase(courtCase);
        prosecutor.setName(name);
        return prosecutor;
    }

}
