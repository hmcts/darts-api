package uk.gov.hmcts.darts.audio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.audio.enums.AudioRequestOutputFormat;
import uk.gov.hmcts.darts.audiorequests.model.SearchTransformedMediaRequest;
import uk.gov.hmcts.darts.audiorequests.model.SearchTransformedMediaResponse;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;
import uk.gov.hmcts.darts.testutils.stubs.TransformedMediaStub;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AudioControllerAdminGetTransformedMediaIntTest extends IntegrationBase {

    private static final String ENDPOINT_URL = "/admin/transformed-medias/search";

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @Autowired
    private TransformedMediaStub transformedMediaStub;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserIdentity userIdentity;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void openHibernateSession() {
        openInViewUtil.openEntityManager();
    }

    @AfterEach
    void closeHibernateSession() {
        openInViewUtil.closeEntityManager();
    }

    @Test
    void testSearchForTransformedMediaWithCaseNumberAndReturnApplicableResults() throws Exception {
        List<TransformedMediaEntity> transformedMediaEntityList = transformedMediaStub.generateTransformedMediaEntities(4);

        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        SearchTransformedMediaRequest request = new SearchTransformedMediaRequest();
        request.setCaseNumber(transformedMediaEntityList.get(2).getMediaRequest().getHearing().getCourtCase().getCaseNumber());

        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        SearchTransformedMediaResponse[] transformedMediaResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), SearchTransformedMediaResponse[].class);
        assertEquals(1, transformedMediaResponses.length);

        assertResponseEquality(transformedMediaResponses[0], getTransformMediaEntity(transformedMediaResponses[0].getId(), transformedMediaEntityList));
    }


    @Test
    void testSearchForTransformedMedia_multipleResults_shouldBeOrderedByMediaId() throws Exception {
        transformedMediaStub.generateTransformedMediaEntities(4);

        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        SearchTransformedMediaRequest request = new SearchTransformedMediaRequest();

        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        SearchTransformedMediaResponse[] transformedMediaResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), SearchTransformedMediaResponse[].class);

        assertEquals(4, transformedMediaResponses.length);
        assertEquals(4, transformedMediaResponses[0].getId());
        assertEquals(3, transformedMediaResponses[1].getId());
        assertEquals(2, transformedMediaResponses[2].getId());
        assertEquals(1, transformedMediaResponses[3].getId());
    }

    @Test
    void testSearchForTransformedMediaWithDateFromAndReturnApplicableResults() throws Exception {
        List<TransformedMediaEntity> transformedMediaEntityList = transformedMediaStub.generateTransformedMediaEntities(4);

        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        SearchTransformedMediaRequest request = new SearchTransformedMediaRequest();
        request.setRequestedAtFrom(transformedMediaEntityList.get(2).getMediaRequest().getCreatedDateTime().minusDays(2).toLocalDate());

        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        SearchTransformedMediaResponse[] transformedMediaResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), SearchTransformedMediaResponse[].class);
        assertEquals(transformedMediaEntityList.size(), transformedMediaResponses.length);

        for (SearchTransformedMediaResponse response : transformedMediaResponses) {
            assertResponseEquality(response, getTransformMediaEntity(response.getId(), transformedMediaEntityList));
        }
    }

    @Test
    void testSearchForTransformedMediaWithDateFromAndDateToAndReturnApplicableResults() throws Exception {
        List<TransformedMediaEntity> transformedMediaEntityList = transformedMediaStub.generateTransformedMediaEntities(4);

        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        SearchTransformedMediaRequest request = new SearchTransformedMediaRequest();
        request.setRequestedAtFrom(transformedMediaEntityList.get(2).getMediaRequest().getCreatedDateTime().minusDays(2).toLocalDate());
        request.setRequestedAtTo(transformedMediaEntityList.get(2).getMediaRequest().getCreatedDateTime().toLocalDate());

        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        SearchTransformedMediaResponse[] transformedMediaResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), SearchTransformedMediaResponse[].class);
        assertEquals(transformedMediaEntityList.size(), transformedMediaResponses.length);

        for (SearchTransformedMediaResponse response : transformedMediaResponses) {
            assertResponseEquality(response, getTransformMediaEntity(response.getId(), transformedMediaEntityList));
        }
    }

    @Test
    void testSearchForTransformedMediaWithDateFromAndDateToAndReturnNoResults() throws Exception {
        List<TransformedMediaEntity> transformedMediaEntityList = transformedMediaStub.generateTransformedMediaEntities(4);

        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        SearchTransformedMediaRequest request = new SearchTransformedMediaRequest();
        request.setRequestedAtFrom(transformedMediaEntityList.get(2).getMediaRequest().getCreatedDateTime().minusDays(2).toLocalDate());
        request.setRequestedAtTo(transformedMediaEntityList.get(2).getMediaRequest().getCreatedDateTime().minusDays(1).toLocalDate());

        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        SearchTransformedMediaResponse[] transformedMediaResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), SearchTransformedMediaResponse[].class);
        assertEquals(0, transformedMediaResponses.length);
    }

    @Test
    void testSearchForTransformedMediaUsingAllSearchCriteria() throws Exception {
        List<TransformedMediaEntity> transformedMediaEntityList = transformedMediaStub.generateTransformedMediaEntities(4);

        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        TransformedMediaEntity mediaEntityToRequest = transformedMediaEntityList.get(2);

        UserAccountEntity mediaRequestCreatedBy = dartsDatabase.getUserAccountRepository()
            .findById(mediaEntityToRequest.getMediaRequest().getCreatedById()).orElseThrow();

        // use all search criteria
        SearchTransformedMediaRequest request = new SearchTransformedMediaRequest();
        request.setRequestedAtFrom(mediaEntityToRequest.getMediaRequest().getCreatedDateTime().minusDays(2).toLocalDate());
        request.setRequestedAtTo(mediaEntityToRequest.getMediaRequest().getCreatedDateTime().minusDays(1).toLocalDate());
        request.setCaseNumber(mediaEntityToRequest.getMediaRequest().getHearing().getCourtCase().getCaseNumber());
        request.setHearingDate(mediaEntityToRequest.getMediaRequest().getHearing().getHearingDate());
        request.setOwner(mediaEntityToRequest.getMediaRequest().getCurrentOwner().getUserFullName());
        request.setRequestedBy(mediaRequestCreatedBy.getUserFullName());
        request.setRequestedAtFrom(mediaEntityToRequest.getCreatedDateTime().toLocalDate());
        request.setRequestedAtTo(mediaEntityToRequest.getCreatedDateTime().toLocalDate());

        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        SearchTransformedMediaResponse[] transformedMediaResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), SearchTransformedMediaResponse[].class);
        assertEquals(1, transformedMediaResponses.length);
        assertResponseEquality(transformedMediaResponses[0], getTransformMediaEntity(transformedMediaResponses[0].getId(), transformedMediaEntityList));

    }

    @Test
    void testSearchWithoutCriteriaAndReturnAll() throws Exception {
        List<TransformedMediaEntity> transformedMediaEntityList = transformedMediaStub.generateTransformedMediaEntities(4);

        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content("{}"))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        assertEquals(200, mvcResult.getResponse().getStatus());

        SearchTransformedMediaResponse[] transformedMediaResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), SearchTransformedMediaResponse[].class);
        assertEquals(transformedMediaEntityList.size(), transformedMediaResponses.length);

        for (SearchTransformedMediaResponse response : transformedMediaResponses) {
            assertResponseEquality(response, getTransformMediaEntity(response.getId(), transformedMediaEntityList));
        }
    }

    @Test
    void testNoRequestPayloadReturnsABadRequest() throws Exception {
        mockMvc.perform(post(ENDPOINT_URL)
                            .header("Content-Type", "application/json"))
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    @Test
    void testAuthorisationProblem() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity, SecurityRoleEnum.DAR_PC);

        mockMvc.perform(post(ENDPOINT_URL).header("Content-Type", "application/json").header("Content-Type", "application/json")
                            .content("{}"))
            .andExpect(status().isForbidden())
            .andReturn();
    }

    @Test
    void searchForTransformedMedia_shouldReturnUnprocessableEntity_whenCourthouseDisplayNameIsLowercase() throws Exception {
        // Authorize the user
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        // Create request with lowercase courthouse display name
        SearchTransformedMediaRequest request = new SearchTransformedMediaRequest();
        request.setCourthouseDisplayName("london crown court"); // lowercase value

        // Perform request and verify response
        mockMvc.perform(post(ENDPOINT_URL)
                            .header("Content-Type", "application/json")
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(result -> {
                String response = result.getResponse().getContentAsString();
                Assertions.assertTrue(response.contains("Courthouse display name must be uppercase"));
            });
    }


    private TransformedMediaEntity getTransformMediaEntity(Integer id, List<TransformedMediaEntity> transformedMediaEntityList) {
        return transformedMediaEntityList.stream().filter(e -> e.getId().equals(id)).findFirst().get();
    }

    private void assertResponseEquality(SearchTransformedMediaResponse response, TransformedMediaEntity entity) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        assertEquals(response.getId(), entity.getId());
        assertEquals(AudioRequestOutputFormat.ZIP.name(), entity.getOutputFormat().name());
        assertEquals(response.getFileName(), entity.getOutputFilename());
        assertEquals(response.getFileSizeBytes(), entity.getOutputFilesize());
        assertEquals(dateTimeFormatter.format(response.getLastAccessedAt()),
                     dateTimeFormatter.format(entity.getLastAccessed().atZoneSameInstant(ZoneId.of("UTC")).toOffsetDateTime()));
        assertEquals(response.getMediaRequest().getId(), entity.getMediaRequest().getId());
        assertEquals(dateTimeFormatter.format(response.getMediaRequest().getRequestedAt()),
                     dateTimeFormatter.format(entity.getMediaRequest().getCreatedDateTime().atZoneSameInstant(ZoneId.of("UTC")).toOffsetDateTime()));
        assertEquals(response.getMediaRequest().getOwnerUserId(),
                     entity.getMediaRequest().getCurrentOwner().getId());
        assertEquals(response.getMediaRequest().getRequestedByUserId(), entity.getMediaRequest().getRequestor().getId());
        assertEquals(response.getCase().getId(), entity.getMediaRequest().getHearing().getCourtCase().getId());
        assertEquals(response.getCase().getCaseNumber(), entity.getMediaRequest().getHearing().getCourtCase().getCaseNumber());
        assertEquals(response.getCourthouse().getId(),
                     entity.getMediaRequest().getHearing().getCourtroom().getCourthouse().getId());
        assertEquals(response.getCourthouse().getDisplayName(),
                     entity.getMediaRequest().getHearing().getCourtroom().getCourthouse().getDisplayName());
        assertEquals(response.getHearing().getId(), entity.getMediaRequest().getHearing().getId());
        assertEquals(response.getHearing().getHearingDate(), entity.getMediaRequest().getHearing().getHearingDate());
    }
}