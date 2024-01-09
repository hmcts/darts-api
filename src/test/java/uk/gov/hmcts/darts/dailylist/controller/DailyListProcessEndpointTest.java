package uk.gov.hmcts.darts.dailylist.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.darts.dailylist.service.DailyListProcessor;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class DailyListProcessEndpointTest {

    @InjectMocks
    private DailyListController controller;
    @Mock
    private DailyListProcessor processor;

    @Test
    void dailyListRun() {
        ResponseEntity<Void> response = controller.dailylistsRunPost(null);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        Mockito.verify(processor, Mockito.timeout(100).times(1)).processAllDailyLists();
    }

    @Test
    void dailyListRunWithCourthouse() {
        ResponseEntity<Void> response = controller.dailylistsRunPost("Swansea");
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        Mockito.verify(processor, Mockito.timeout(100).times(1)).processAllDailyListForListingCourthouse("Swansea");
    }
}
