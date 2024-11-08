package uk.gov.hmcts.darts.dailylist.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.common.repository.DailyListRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DailyListStub;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DailyListServiceHousekeepingTest extends IntegrationBase {

    static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    DailyListService service;

    @Autowired
    DartsDatabaseStub dartsDatabaseStub;

    @Autowired
    DailyListRepository dailyListRepository;

    @Autowired
    DailyListStub dailyListStub;

    @BeforeAll
    static void beforeAll() {
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void housekeepingOk() {
        createEmptyDailyLists(50, LocalDate.now(), "courthouse1");

        List<DailyListEntity> resultList = dailyListRepository.findAll();
        assertEquals(50, resultList.size());

        service.runHouseKeeping(getAutomatedTaskBatchSize());

        List<DailyListEntity> newResultList = dailyListRepository.findAll();
        assertEquals(31, newResultList.size());

    }

    private void createEmptyDailyLists(int numOfDaysInPast, LocalDate startDate, String courthouse) {
        for (int counter = 0; counter < numOfDaysInPast; counter++) {
            dailyListStub.createEmptyDailyList(startDate.minusDays(counter), courthouse);
        }
    }

}
