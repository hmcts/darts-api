package uk.gov.hmcts.darts.dailylist.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.dailylist.repository.DailyListRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DailyListStub;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@ExtendWith(MockitoExtension.class)
class DailyListServiceHousekeepingTest extends IntegrationBase {

    static final String CPP = "CPP";
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
    void housekeepingOk() throws IOException {

        CourthouseEntity courthouse = dartsDatabaseStub.createCourthouse("courthouse1");
        createEmptyDailyLists(50, LocalDate.now(), courthouse);

        List<DailyListEntity> resultList = dailyListRepository.findAll();
        assertEquals(50, resultList.size());

        service.runHouseKeepingNow();

        List<DailyListEntity> newResultList = dailyListRepository.findAll();
        assertEquals(31, newResultList.size());

    }

    private void createEmptyDailyLists(int numOfDaysInPast, LocalDate startDate, CourthouseEntity courthouse) {
        for (int counter = 0; counter < numOfDaysInPast; counter++) {
            dailyListStub.createEmptyDailyList(startDate.minusDays(counter), courthouse);
        }
    }


}
