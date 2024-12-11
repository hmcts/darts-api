package uk.gov.hmcts.darts.admin.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.AuditActivityRepository;
import uk.gov.hmcts.darts.common.service.bankholidays.BankHolidaysService;
import uk.gov.hmcts.darts.common.service.bankholidays.Event;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyInt;
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
    @MockBean
    private BankHolidaysService mockBankHolidaysService;

    @MockBean
    private UserAccountEntity mockUserAccountEntity;

    @MockBean
    private SecurityGroupEntity mockSecurityGroupEntity;
    @MockBean
    private CourthouseEntity courthouseEntity;
    @SpyBean
    private AuditActivityRepository auditActivityRepository;

    @BeforeEach
    void beforeEach() {
        when(mockUserIdentity.getUserAccount()).thenReturn(mockUserAccountEntity);
        SecurityGroupEntity sge = new SecurityGroupEntity();
        sge.setId(1);
        Set<SecurityGroupEntity> sgeSet = new HashSet<>();
        sgeSet.add(sge);
        when(mockUserAccountEntity.getSecurityGroupEntities()).thenReturn(sgeSet);
    }

    @Test
    void rejectsCourthousesNotPrefixedCorrectly() throws Exception {
        var requestBuilder = post(ENDPOINT_URL + "/courthouse/swansea/courtroom/cr1");

        mockMvc.perform(requestBuilder).andExpect(status().isBadRequest());
    }

    @Test
    void createsCourthouseAndCourtroom() throws Exception {
        mockMvc.perform(post(ENDPOINT_URL + "/courthouse/FUNC-SWANSEA/courtroom/cr1"))
            .andExpect(status().isCreated());

        assertThat(dartsDatabase.findCourtroomBy("FUNC-SWANSEA", "cr1")).isNotNull();
    }

    @Test
    void createsCourtroomForExistingCourthouse() throws Exception {
        mockMvc.perform(post(ENDPOINT_URL + "/courthouse/FUNC-SWANSEA/courtroom/cr1"))
            .andExpect(status().isCreated());
        mockMvc.perform(post(ENDPOINT_URL + "/courthouse/FUNC-SWANSEA/courtroom/cr2"))
            .andExpect(status().isCreated());

        assertThat(dartsDatabase.findCourtroomBy("FUNC-SWANSEA", "cr1")).isNotNull();
        assertThat(dartsDatabase.findCourtroomBy("FUNC-SWANSEA", "cr2")).isNotNull();
    }


    @Test
    void createsAudit() throws Exception {
        mockMvc.perform(post(ENDPOINT_URL + "/courthouse/FUNC-SWANSEA/courtroom/cr1"))
            .andExpect(status().isCreated());

        mockMvc.perform(post(ENDPOINT_URL + "/audit/REQUEST_AUDIO/courthouse/FUNC-SWANSEA"))
            .andExpect(status().isCreated());

        assertEquals(1, dartsDatabase.getAuditRepository().findAll().size());

    }

    @Test
    void createsNoAuditAndNoCourtCaseOnBadRequest() throws Exception {
        when(auditActivityRepository.getReferenceById(anyInt())).thenThrow(DataIntegrityViolationException.class);

        mockMvc.perform(post(ENDPOINT_URL + "/courthouse/FUNC-SWANSEA/courtroom/cr1"))
            .andExpect(status().isCreated());

        mockMvc.perform(post(ENDPOINT_URL + "/audit/REQUEST_AUDIO/courthouse/FUNC-SWANSEA"))
            .andExpect(status().isBadRequest());

        assertEquals(0, dartsDatabase.getCaseRepository().findAll().size());
        assertEquals(0, dartsDatabase.getAuditRepository().findAll().size());
    }

    @Test
    void getsBankHolidays() throws Exception {
        Event event = new Event();
        event.setTitle("christmas");
        List<Event> events = new ArrayList<>();
        events.add(event);

        when(mockBankHolidaysService.getBankHolidays()).thenReturn(events);

        MvcResult response = mockMvc.perform(get(ENDPOINT_URL + "/bank-holidays/2023"))
            .andExpect(status().isOk())
            .andReturn();

        String actualResponseBody = response.getResponse().getContentAsString();
        assertThat(actualResponseBody.contains("christmas"));
    }

    @Test
    void createRetention() throws Exception {
        MvcResult response = mockMvc.perform(post(ENDPOINT_URL + "/case-retentions/caseNumber/FUNC-CASE-A"))
            .andExpect(status().isOk())
            .andReturn();

        assertFalse(response.getResponse().getContentAsString().isEmpty());
    }

    @Test
    void cleansData() throws Exception {
        mockMvc.perform(post(ENDPOINT_URL + "/courthouse/FUNC-SWANSEA/courtroom/FUNC-CR1"))
            .andExpect(status().isCreated());

        mockMvc.perform(delete(ENDPOINT_URL + "/clean"))
            .andExpect(status().is2xxSuccessful());

        assertThat(dartsDatabase.findCourtroomBy("FUNC-SWANSEA", "CR1")).isNull();
    }
}
