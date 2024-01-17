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
class ProcessAllDailyListsForCourthouseTest {

    public static final LocalDate NOW = now();
    @Mock
    private DailyListUpdater dailyListUpdater;
    @Mock
    private DailyListRepository dailyListRepository;
    @Mock
    private DailyListProcessorImpl dailyListProcessor;
    @Mock
    private DailyListEntity dailyListEntityForSwansea;
    @Mock
    private DailyListEntity oldDailyListEntityForLeeds;
    @Mock
    private DailyListEntity oldestDailyListEntityForLeeds;
    @Mock
    private DailyListEntity latestDailyListEntityForLeeds;


    @BeforeEach
    void setUp() {
        dailyListProcessor = new DailyListProcessorImpl(dailyListRepository, dailyListUpdater);
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
        lenient().when(dailyListRepository.findByListingCourthouseAndStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(
                "Swansea", NEW, NOW, sourceType.name())).thenReturn(emptyList());

        dailyListProcessor.processAllDailyListForListingCourthouse("Swansea");

        verify(dailyListRepository).findByListingCourthouseAndStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(
            "Swansea", NEW, NOW, sourceType.name());
        verifyNoInteractions(dailyListUpdater);
    }


    @Test
    void handlesSingleDailyListItemForOneSourceType() throws JsonProcessingException {
        when(dailyListRepository.findByListingCourthouseAndStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(
                "Swansea", NEW, NOW, SourceType.XHB.name())).thenReturn(emptyList());
        when(dailyListRepository.findByListingCourthouseAndStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(
                "Swansea", NEW, NOW, SourceType.CPP.name())).thenReturn(List.of(dailyListEntityForSwansea));

        dailyListProcessor.processAllDailyListForListingCourthouse("Swansea");

        verify(dailyListUpdater, times(1)).processDailyList(dailyListEntityForSwansea);
        verifyNoMoreInteractions(dailyListUpdater);
    }

    @Test
    void handlesDailyListsFromBothSourceTypes() throws JsonProcessingException {
        when(dailyListRepository.findByListingCourthouseAndStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(
                "Leeds", NEW, NOW, SourceType.XHB.name())).thenReturn(List.of(oldDailyListEntityForLeeds));

        when(dailyListRepository.findByListingCourthouseAndStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(
            "Leeds", NEW, NOW, SourceType.CPP.name())).thenReturn(List.of(latestDailyListEntityForLeeds, oldestDailyListEntityForLeeds));

        dailyListProcessor.processAllDailyListForListingCourthouse("Leeds");

        verify(dailyListUpdater, times(1)).processDailyList(oldDailyListEntityForLeeds);
        verify(dailyListUpdater, times(1)).processDailyList(latestDailyListEntityForLeeds);
        verifyNoMoreInteractions(dailyListUpdater);
    }

    private void setCourthouseForStubs(String listingCourthouse, DailyListEntity... dailyListEntityForSwansea) {
        stream(dailyListEntityForSwansea)
            .forEach(dailyListEntity -> lenient().when(dailyListEntity.getListingCourthouse()).thenReturn(listingCourthouse));
    }
}
