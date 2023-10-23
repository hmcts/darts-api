package uk.gov.hmcts.darts.admin.test;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
    private UserAccountEntity mockUserAccountEntity;

    @MockBean
    private SecurityGroupEntity mockSecurityGroupEntity;
    @MockBean
    private CourthouseEntity courthouseEntity;

    @BeforeAll
    void beforeAll() {
        when(mockUserIdentity.getUserAccount()).thenReturn(mockUserAccountEntity);
        /*
        SecurityGroupEntity sge = new SecurityGroupEntity();
        Set<SecurityGroupEntity> sgeSet = new HashSet<SecurityGroupEntity>();
        sgeSet.add(sge);
        when(mockUserAccountEntity.getSecurityGroupEntities()).thenReturn(sgeSet);
        */
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
    void cleansData() throws Exception {
        mockMvc.perform(post(ENDPOINT_URL + "/courthouse/func-swansea/courtroom/func-cr1"))
            .andExpect(status().isCreated());

        mockMvc.perform(delete(ENDPOINT_URL + "/clean"))
            .andExpect(status().is2xxSuccessful());

        assertThat(dartsDatabase.findCourtroomBy("func-swansea", "cr1")).isNull();
    }
}
