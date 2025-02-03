package uk.gov.hmcts.darts.dailylist.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.dailylist.model.PatchDailyListRequest;
import uk.gov.hmcts.darts.dailylist.model.PostDailyListRequest;
import uk.gov.hmcts.darts.dailylist.model.PostDailyListResponse;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;

@AutoConfigureMockMvc
class DailyListPatchControllerTest extends IntegrationBase {

    private static final String ENDPOINT_URL = "/dailylists";

    @Autowired
    private transient MockMvc mockMvc;
    @MockitoBean
    private UserIdentity mockUserIdentity;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @Test
    void success() throws Exception {

        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity);

        String courthouseName = "FUNC-SWANSEA-HOUSE-" + randomAlphanumeric(7);
        String uniqueId = "FUNC-unique-id-" + randomAlphanumeric(7);
        String messageId = "FUNC-unique-id-" + randomAlphanumeric(7);

        PostDailyListRequest request = new PostDailyListRequest();
        request.setSourceSystem("CPP");
        request.setCourthouse(courthouseName);
        request.setHearingDate(LocalDate.of(2020, 10, 10));
        request.setUniqueId(uniqueId);
        request.setPublishedTs(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));
        request.setMessageId(messageId);
        request.setXmlDocument("<?xml version=\"1.0\"?><dummy></dummy>");

        String requestBody = objectMapper.writeValueAsString(request);
        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URL)
            .content(requestBody)
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful())
            .andReturn();

        PostDailyListResponse postDailyListResponse = objectMapper.readValue(response.getResponse().getContentAsString(), PostDailyListResponse.class);

        Optional<DailyListEntity> dailyListInDb = dartsDatabase.getDailyListRepository().findById(postDailyListResponse.getDalId());
        assertNull(dailyListInDb.get().getContent());

        final String jsonPostRequest = getContentsFromFile("tests/DailyListTest/dailyListAddDailyListEndpoint/requestBody.json");

        PatchDailyListRequest patchRequest = new PatchDailyListRequest();
        patchRequest.setDalId(postDailyListResponse.getDalId());
        patchRequest.setJsonString(jsonPostRequest);
        String patchRequestBody = objectMapper.writeValueAsString(patchRequest);
        requestBuilder = patch(ENDPOINT_URL)
            .content(patchRequestBody)
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful());

        dailyListInDb = dartsDatabase.getDailyListRepository().findById(patchRequest.getDalId());
        assertNotNull(dailyListInDb.get().getContent());
    }

}

