package uk.gov.hmcts.darts.dailylist.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SystemUsersEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.CreateCoreObjectService;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.dailylist.mapper.CitizenNameMapper;
import uk.gov.hmcts.darts.dailylist.util.CitizenNameComparator;
import uk.gov.hmcts.darts.test.common.TestUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.dailylist.enums.JobStatusType.PARTIALLY_PROCESSED;
import static uk.gov.hmcts.darts.dailylist.enums.JobStatusType.PROCESSED;

@ExtendWith(MockitoExtension.class)
class DailyListUpdaterTest {

    @Mock
    private RetrieveCoreObjectService retrieveCoreObjectService;
    @Mock
    private CreateCoreObjectService createCoreObjectService;
    @Mock
    private CourthouseRepository courthouseRepository;
    @Mock
    private HearingRepository hearingRepository;
    @Mock
    private SystemUserHelper systemUserHelper;
    @Mock
    private CurrentTimeHelper currentTimeHelper;
    @Mock
    private CitizenNameMapper citizenNameMapper;
    @Mock
    private CitizenNameComparator citizenNameComparator;

    private DailyListUpdater dailyListUpdater;

    @Captor
    private ArgumentCaptor<HearingEntity> hearingEntityCaptor;

    private static final LocalDateTime HEARING_DATE = LocalDateTime.of(2023, 9, 23, 11, 0, 0);

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
        dailyListUpdater = new DailyListUpdater(retrieveCoreObjectService, createCoreObjectService, courthouseRepository,
                                                hearingRepository, objectMapper, systemUserHelper,
                                                currentTimeHelper, citizenNameMapper, citizenNameComparator);
    }

    @Test
    void handlesCaseNumberMissingForCpp() throws IOException {
        var dailyListUser = new UserAccountEntity();
        when(systemUserHelper.getReferenceTo(SystemUsersEnum.DAILY_LIST_PROCESSOR)).thenReturn(dailyListUser);
        when(courthouseRepository.findByCourthouseName("SWANSEA")).thenReturn(Optional.of(new CourthouseEntity()));
        HearingEntity hearing = new HearingEntity();
        CourtCaseEntity courtCase = new CourtCaseEntity();
        hearing.setCourtCase(courtCase);

        DailyListEntity dailyList = setUpDailyList("dailyListWithoutUrn.json");
        dailyListUpdater.processDailyList(dailyList);

        verifyNoInteractions(retrieveCoreObjectService);

        assertThat(dailyList.getStatus()).isEqualTo(PARTIALLY_PROCESSED);
    }

    @Test
    void handlesCaseNumberForCpp() throws IOException {

        var dailyListUser = new UserAccountEntity();
        OffsetDateTime testTime = DateConverterUtil.toOffsetDateTime(HEARING_DATE);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);
        when(systemUserHelper.getReferenceTo(SystemUsersEnum.DAILY_LIST_PROCESSOR)).thenReturn(dailyListUser);
        when(courthouseRepository.findByCourthouseName("SWANSEA")).thenReturn(Optional.of(new CourthouseEntity()));
        HearingEntity hearing = new HearingEntity();
        CourtCaseEntity courtCase = new CourtCaseEntity();
        hearing.setCourtCase(courtCase);
        when(retrieveCoreObjectService.retrieveOrCreateHearing("SWANSEA", "1A", "42GD2391421", HEARING_DATE, dailyListUser))
            .thenReturn(hearing);
        DailyListEntity dailyList = setUpDailyList("dailyList.json");
        dailyListUpdater.processDailyList(dailyList);

        verify(hearingRepository, times(1)).saveAndFlush(hearingEntityCaptor.capture());

        HearingEntity hearingEntityCaptorValue = hearingEntityCaptor.getValue();
        assertThat(hearingEntityCaptorValue.getLastModifiedDateTime()).isEqualTo(testTime);
        assertThat(hearingEntityCaptorValue.getCourtCase().getLastModifiedDateTime()).isEqualTo(testTime);

        assertThat(dailyList.getStatus()).isEqualTo(PROCESSED);
    }

    @Test
    void handlesNoTimeMarkingNote() throws IOException {

        var dailyListUser = new UserAccountEntity();
        OffsetDateTime testTime = DateConverterUtil.toOffsetDateTime(HEARING_DATE);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);
        when(systemUserHelper.getReferenceTo(SystemUsersEnum.DAILY_LIST_PROCESSOR)).thenReturn(dailyListUser);
        when(courthouseRepository.findByCourthouseName("SWANSEA")).thenReturn(Optional.of(new CourthouseEntity()));
        HearingEntity hearing = new HearingEntity();
        CourtCaseEntity courtCase = new CourtCaseEntity();
        hearing.setCourtCase(courtCase);
        LocalDateTime expectedHearingDate = LocalDateTime.of(2023, 9, 23, 9, 0, 0);
        when(retrieveCoreObjectService.retrieveOrCreateHearing("SWANSEA", "1A", "42GD2391421", expectedHearingDate, dailyListUser))
            .thenReturn(hearing);
        DailyListEntity dailyList = setUpDailyList("handlesNoTimeMarkingNote.json");
        dailyListUpdater.processDailyList(dailyList);
        verify(retrieveCoreObjectService, times(1)).retrieveOrCreateHearing("SWANSEA", "1A", "42GD2391421", expectedHearingDate, dailyListUser);
    }

    @ParameterizedTest
    @CsvSource({
        "NOT BEFORE 10:00 am, 10:00",
        "NOT BEFORE  10:00 am, 10:00",
        "SITTING AT  10:00 am, 10:00",
        "SITTING AT 10:00 am, 10:00",
        "11:00 am, 11:00",
        "3:00 PM, 15:00"
    })
    void getTimeFromTimeMarkingNote(String timeMarkingNote, String result) {
        assertThat(dailyListUpdater.getTimeFromTimeMarkingNote(timeMarkingNote)).isEqualTo(result);
    }

    private DailyListEntity setUpDailyList(String filename) throws IOException {
        String dailyListJson = TestUtils.getContentsFromFile(
            "Tests/dailylist/DailyListUpdaterTest/" + filename);
        dailyListJson = dailyListJson.replace("todays_date", HEARING_DATE.toLocalDate().toString());

        DailyListEntity dailyList = new DailyListEntity();
        dailyList.setId(1);
        dailyList.setSource("CPP");
        dailyList.setContent(dailyListJson);
        return dailyList;
    }

}