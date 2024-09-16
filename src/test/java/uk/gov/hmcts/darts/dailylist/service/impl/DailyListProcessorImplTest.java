package uk.gov.hmcts.darts.dailylist.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.common.repository.DailyListRepository;
import uk.gov.hmcts.darts.dailylist.enums.JobStatusType;
import uk.gov.hmcts.darts.dailylist.enums.SourceType;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.log.util.DailyListLogJobReport;
import uk.gov.hmcts.darts.task.api.AutomatedTasksApi;

import java.time.LocalDate;
import java.util.List;

import static java.time.LocalDate.now;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.dailylist.enums.JobStatusType.NEW;

@ExtendWith(MockitoExtension.class)
class DailyListProcessorImplTest {

    public static final LocalDate NOW = now();
    @Mock
    private DailyListUpdater dailyListUpdater;
    @Mock
    private DailyListRepository dailyListRepository;
    @Mock
    private DailyListEntity dailyListEntityForSwansea;
    @Mock
    private DailyListEntity oldDailyListEntityForLeeds;
    @Mock
    private DailyListEntity oldestDailyListEntityForLeeds;
    @Mock
    private DailyListEntity latestDailyListEntityForLeeds;
    @Mock
    private LogApi logApi;
    @Mock
    private AutomatedTasksApi automatedTasksApi;

    private DailyListProcessorImpl dailyListProcessor;

    @BeforeEach
    void setUp() {
        dailyListProcessor = new DailyListProcessorImpl(dailyListRepository, dailyListUpdater, logApi, automatedTasksApi);
        setCourthouseForStubs("Swansea", dailyListEntityForSwansea);
        setCourthouseForStubs(
            "Leeds",
            oldestDailyListEntityForLeeds,
            oldDailyListEntityForLeeds,
            latestDailyListEntityForLeeds
        );
    }

    @ParameterizedTest
    @EnumSource(SourceType.class)
    void handlesScenarioWhereNoDailyListsAreFound(SourceType sourceType) {
        lenient().when(dailyListRepository.findByListingCourthouseAndStatusAndStartDateAndSourceOrderByPublishedTimestampDescCreatedDateTimeDesc(
                "Swansea", NEW, NOW, sourceType.name())).thenReturn(emptyList());

        dailyListProcessor.processAllDailyListForListingCourthouse("Swansea");

        verify(dailyListRepository).findByListingCourthouseAndStatusAndStartDateAndSourceOrderByPublishedTimestampDescCreatedDateTimeDesc(
            "Swansea", NEW, NOW, sourceType.name());
        verifyNoInteractions(dailyListUpdater);
        verify(logApi, times(2)).processedDailyListJob(Mockito.notNull());
    }

    @Test
    void handlesSingleDailyListItemForOneSourceType() throws JsonProcessingException {
        when(dailyListEntityForSwansea.getStatus()).thenReturn(JobStatusType.PROCESSED);

        when(dailyListRepository.findByListingCourthouseAndStatusAndStartDateAndSourceOrderByPublishedTimestampDescCreatedDateTimeDesc(
                "Swansea", NEW, NOW, SourceType.XHB.name())).thenReturn(emptyList());
        when(dailyListRepository.findByListingCourthouseAndStatusAndStartDateAndSourceOrderByPublishedTimestampDescCreatedDateTimeDesc(
                "Swansea", NEW, NOW, SourceType.CPP.name())).thenReturn(List.of(dailyListEntityForSwansea));

        dailyListProcessor.processAllDailyListForListingCourthouse("Swansea");

        DailyListLogJobReport expectedReportCpp = new DailyListLogJobReport(1, SourceType.CPP);
        expectedReportCpp.registerResult(JobStatusType.PROCESSED);

        verify(logApi, times(1)).processedDailyListJob(Mockito.argThat(new DailyListLogReportMatcher(expectedReportCpp)));

        verify(dailyListUpdater, times(1)).processDailyList(dailyListEntityForSwansea);
        verifyNoMoreInteractions(dailyListUpdater);
    }

    @Test
    void handlesDailyListsFromBothSourceTypes() throws JsonProcessingException {

        when(oldDailyListEntityForLeeds.getStatus()).thenReturn(JobStatusType.PROCESSED);
        when(latestDailyListEntityForLeeds.getStatus()).thenReturn(JobStatusType.PROCESSED);

        when(dailyListRepository.findByListingCourthouseAndStatusAndStartDateAndSourceOrderByPublishedTimestampDescCreatedDateTimeDesc(
                "Leeds", NEW, NOW, SourceType.XHB.name())).thenReturn(List.of(oldDailyListEntityForLeeds));

        when(dailyListRepository.findByListingCourthouseAndStatusAndStartDateAndSourceOrderByPublishedTimestampDescCreatedDateTimeDesc(
            "Leeds", NEW, NOW, SourceType.CPP.name())).thenReturn(List.of(latestDailyListEntityForLeeds, oldestDailyListEntityForLeeds));

        dailyListProcessor.processAllDailyListForListingCourthouse("Leeds");

        DailyListLogJobReport expectedReportXhb = new DailyListLogJobReport(1, SourceType.XHB);
        expectedReportXhb.registerResult(JobStatusType.PROCESSED);

        DailyListLogJobReport expectedReportCpp = new DailyListLogJobReport(2, SourceType.CPP);
        expectedReportCpp.registerResult(JobStatusType.PROCESSED);
        expectedReportCpp.registerResult(JobStatusType.IGNORED);

        verify(dailyListUpdater, times(1)).processDailyList(oldDailyListEntityForLeeds);
        verify(dailyListUpdater, times(1)).processDailyList(latestDailyListEntityForLeeds);

        verify(logApi, times(2)).processedDailyListJob(Mockito.argThat(new DailyListLogReportMatcher(expectedReportXhb, expectedReportCpp)));

        verifyNoMoreInteractions(dailyListUpdater);
    }

    private void setCourthouseForStubs(String listingCourthouse, DailyListEntity... dailyListEntityForSwansea) {
        stream(dailyListEntityForSwansea)
            .forEach(dailyListEntity -> lenient().when(dailyListEntity.getListingCourthouse()).thenReturn(listingCourthouse));
    }

    class DailyListLogReportMatcher implements ArgumentMatcher<DailyListLogJobReport> {

        private final DailyListLogJobReport[] objectToAssertAgainst;

        DailyListLogReportMatcher(DailyListLogJobReport... report) {
            this.objectToAssertAgainst = report.clone();
        }

        @Override
        public boolean matches(DailyListLogJobReport argument) {
            for (DailyListLogJobReport dailyListLogJobReport : objectToAssertAgainst) {
                if (dailyListLogJobReport.toString().equals(argument.toString())) {
                    return true;
                }
            }
            return false;
        }
    }
}