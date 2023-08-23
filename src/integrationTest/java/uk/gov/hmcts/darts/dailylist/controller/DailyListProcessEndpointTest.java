package uk.gov.hmcts.darts.dailylist.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.dailylist.service.DailyListProcessor;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class DailyListProcessEndpointTest {

    @InjectMocks
    private DailyListController controller;
    @Mock
    private DailyListProcessor processor;


    @Test
    void dailyListRun() {
        controller.dailylistsRunPost(null);
        Mockito.verify(processor).processAllDailyLists(LocalDate.now());
    }

    @Test
    void dailyListRunWithCourthouse() {
        controller.dailylistsRunPost(1);
        Mockito.verify(processor).processAllDailyListForCourthouse(any());
    }
}
