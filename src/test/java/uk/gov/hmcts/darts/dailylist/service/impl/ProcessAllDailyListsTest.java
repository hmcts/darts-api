package uk.gov.hmcts.darts.dailylist.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.common.repository.DailyListRepository;
import uk.gov.hmcts.darts.dailylist.enums.SourceType;

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
class ProcessAllDailyListsTest {

    public static final LocalDate NOW = now();
    @Mock
    private DailyListUpdater dailyListUpdater;
    @Mock
    private DailyListRepository dailyListRepository;
    @Mock
    private DailyListProcessorImpl dailyListProcessor;
    @Mock
    private DailyListEntity dailyListForSwansea;
    @Mock
    private DailyListEntity oldDailyListForLeeds;
    @Mock
    private DailyListEntity oldestDailyListForLeeds;
    @Mock
    private DailyListEntity latestDailyListForLeeds;



    @BeforeEach
    void setUp() {
        dailyListProcessor = new DailyListProcessorImpl(dailyListRepository, dailyListUpdater);
        setCourthouseForStubs("Swansea", dailyListForSwansea);
        setCourthouseForStubs("Leeds",
                              oldestDailyListForLeeds,
                              oldDailyListForLeeds,
                              latestDailyListForLeeds
        );
    }

    @ParameterizedTest
    @EnumSource(SourceType.class)
    void handlesScenarioWhereNoDailyListsAreFound(SourceType sourceType) {
        lenient().when(dailyListRepository.findByStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(NEW, NOW, sourceType.name()))
            .thenReturn(emptyList());

        dailyListProcessor.processAllDailyLists();

        verify(dailyListRepository).findByStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(NEW, NOW, sourceType.name());
        verifyNoInteractions(dailyListUpdater);
    }


    @Test
    void handlesSingleDailyListItemForOneSourceType() throws JsonProcessingException {
        when(dailyListRepository.findByStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(NEW, NOW, SourceType.XHB.name()))
            .thenReturn(emptyList());
        when(dailyListRepository.findByStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(NEW, NOW, SourceType.CPP.name()))
            .thenReturn(List.of(dailyListForSwansea));

        dailyListProcessor.processAllDailyLists();

        verify(dailyListUpdater, times(1)).processDailyList(dailyListForSwansea);
        verifyNoMoreInteractions(dailyListUpdater);
    }

    @Test
    void groupsDailyListsByListingCourthouse() throws JsonProcessingException {
        when(dailyListRepository.findByStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(NEW, NOW, SourceType.XHB.name()))
            .thenReturn(emptyList());
        when(dailyListRepository.findByStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(NEW, NOW, SourceType.CPP.name()))
            .thenReturn(List.of(latestDailyListForLeeds, oldDailyListForLeeds, oldestDailyListForLeeds, dailyListForSwansea));

        dailyListProcessor.processAllDailyLists();

        verify(dailyListUpdater, times(1)).processDailyList(latestDailyListForLeeds);
        verify(dailyListUpdater, times(1)).processDailyList(dailyListForSwansea);
        verifyNoMoreInteractions(dailyListUpdater);
    }

    @Test
    void handlesDailyListsFromBothSourceTypes() throws JsonProcessingException {
        when(dailyListRepository.findByStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(NEW, NOW, SourceType.XHB.name()))
            .thenReturn(List.of(oldDailyListForLeeds));
        when(dailyListRepository.findByStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(NEW, NOW, SourceType.CPP.name()))
            .thenReturn(List.of(latestDailyListForLeeds, oldestDailyListForLeeds));

        dailyListProcessor.processAllDailyLists();

        verify(dailyListUpdater, times(1)).processDailyList(oldDailyListForLeeds);
        verify(dailyListUpdater, times(1)).processDailyList(latestDailyListForLeeds);
        verifyNoMoreInteractions(dailyListUpdater);
    }

    private void setCourthouseForStubs(String listingCourthouse, DailyListEntity... dailyListEntityForSwansea) {
        stream(dailyListEntityForSwansea)
            .forEach(dailyListEntity -> lenient().when(dailyListEntity.getListingCourthouse()).thenReturn(listingCourthouse));
    }
}
