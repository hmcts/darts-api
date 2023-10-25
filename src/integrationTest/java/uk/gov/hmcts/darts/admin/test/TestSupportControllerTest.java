package uk.gov.hmcts.darts.admin.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.service.bankholidays.BankHolidaysService;
import uk.gov.hmcts.darts.common.service.bankholidays.Event;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestSupportControllerTest extends IntegrationBase {

    private static final String ENDPOINT_URL = "/functional-tests";
    @Autowired
    private transient MockMvc mockMvc;
    @MockBean
    private UserIdentity mockUserIdentity;
    @Mock
    private UserAccountEntity mockUserAccountEntity;
    @MockBean
    private BankHolidaysService mockBankHolidaysService;
    @MockBean
    private SecurityGroupEntity mockSecurityGroupEntity;
    @MockBean
    private CourthouseEntity courthouseEntity;
    private UserAccountEntity testUser;

    @BeforeAll
    void beforeEach() {

        testUser = new UserAccountEntity();
        testUser.setEmailAddress("test.user@example.com");

        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        Set<SecurityGroupEntity> sgeSet = new HashSet<>();
        sgeSet.add(mockSecurityGroupEntity);
        when(mockUserAccountEntity.getSecurityGroupEntities()).thenReturn(sgeSet);

    }

    @Test
    void rejectsCourthousesNotPrefixedCorrectly() throws Exception {
        var requestBuilder = post(ENDPOINT_URL + "/courthouse/swansea/courtroom/cr1");

        mockMvc.perform(requestBuilder).andExpect(status().isBadRequest());
    }

    @Test
    void createsCourthouseAndCourtroom() throws Exception {
        mockMvc.perform(post(ENDPOINT_URL + "/courthouse/func-swansea/courtroom/cr1"))
            .andExpect(status().isCreated());

        assertThat(dartsDatabase.findCourtroomBy("func-swansea", "cr1")).isNotNull();
    }

    @Test
    void createsCourtroomForExistingCourthouse() throws Exception {
        mockMvc.perform(post(ENDPOINT_URL + "/courthouse/func-swansea/courtroom/cr1"))
            .andExpect(status().isCreated());

        mockMvc.perform(post(ENDPOINT_URL + "/courthouse/func-swansea/courtroom/cr2"))
            .andExpect(status().isCreated());

        assertThat(dartsDatabase.findCourtroomBy("func-swansea", "cr1")).isNotNull();
        assertThat(dartsDatabase.findCourtroomBy("func-swansea", "cr2")).isNotNull();
    }


    @Test
    void createsAudit() throws Exception {

        mockMvc.perform(post(ENDPOINT_URL + "/courthouse/func-swansea/courtroom/cr1"))
            .andExpect(status().isCreated());


        mockMvc.perform(post(ENDPOINT_URL + "/audit/REQUEST_AUDIO/courthouse/func-swansea"))
            .andExpect(status().isCreated());

        assertEquals(1, dartsDatabase.getAuditRepository().findAll().size());

    }

    @Test
    void getsBankHolidays() throws Exception {
        Event event = new Event();
        event.title = "christmas";
        List<Event> events = new ArrayList<>();
        events.add(event);

        when(mockBankHolidaysService.getBankHolidaysFor(2023)).thenReturn(events);

        MvcResult response = mockMvc.perform(get(ENDPOINT_URL + "/bank-holidays/2023"))
            .andExpect(status().isOk())
            .andReturn();

        String actualResponseBody = response.getResponse().getContentAsString();
        assertThat(actualResponseBody.contains("christmas"));
    }

    @Test
    void cleansData() throws Exception {
        mockMvc.perform(post(ENDPOINT_URL + "/courthouse/func-swansea/courtroom/func-cr1"))
            .andExpect(status().isCreated());

        mockMvc.perform(delete(ENDPOINT_URL + "/clean"))
            .andExpect(status().is2xxSuccessful());

        assertThat(dartsDatabase.findCourtroomBy("func-swansea", "cr1")).isNull();
    }
}
