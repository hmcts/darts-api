package uk.gov.hmcts.darts.dailylist.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DailyListStub;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class DailyListHousekeepingControllerTest extends IntegrationBase {

    @Autowired
    private transient MockMvc mockMvc;

    @Autowired
    protected DailyListStub dailyListStub;

    @Test
    void housekeepingPostEndpoint() throws Exception {

        CourthouseEntity courthouse = dartsDatabase.createCourthouseUnlessExists("courthouse1");
        createEmptyDailyLists(50, LocalDate.now(), courthouse);

        List<DailyListEntity> resultList = dartsDatabase.getDailyListRepository().findAll();
        Assertions.assertEquals(50, resultList.size());

        MockHttpServletRequestBuilder requestBuilder = post("/dailylists/housekeeping")
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful()).andReturn();

        List<DailyListEntity> newResultList = dartsDatabase.getDailyListRepository().findAll();
        Assertions.assertEquals(31, newResultList.size());
    }

    private void createEmptyDailyLists(int numOfDaysInPast, LocalDate startDate, CourthouseEntity courthouse) {
        for (int counter = 0; counter < numOfDaysInPast; counter++) {
            dailyListStub.createEmptyDailyList(startDate.minusDays(counter), courthouse);
        }
    }

}
