package uk.gov.hmcts.darts.dailylist.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;
import uk.gov.hmcts.darts.dailylist.service.DailyListProcessor;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

@ExtendWith(MockitoExtension.class)
class DailyListProcessEndpointTest {

    @InjectMocks
    private DailyListController controller;
    @Mock
    private DailyListProcessor processor;

    @Mock
    private CourthouseRepository courthouseRepository;


    @Test
    void dailyListRun() {
        controller.dailylistsRunPost(null);
        Mockito.verify(processor, Mockito.timeout(100).times(1)).processAllDailyLists(LocalDate.now());
    }

    @Test
    void dailyListRunWithCourthouse() {
        CourthouseEntity courthouse = Mockito.mock(CourthouseEntity.class);
        Mockito.when(courthouseRepository.findById(anyInt())).thenReturn(Optional.of(courthouse));
        controller.dailylistsRunPost(1);
        Mockito.verify(processor, Mockito.timeout(100).times(1)).processAllDailyListForCourthouse(any());
    }

    @Test
    void dailyListRunWithCourthouseDoesNotExist() {
        assertThrows(DartsApiException.class, () -> controller.dailylistsRunPost(1));
    }
}
