package uk.gov.hmcts.darts.audio.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.audio.enums.AudioRequestOutputFormat;
import uk.gov.hmcts.darts.audio.model.SearchTransformedMediaRequest;
import uk.gov.hmcts.darts.audio.model.SearchTransformedMediaResponse;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.repository.TranscriptionStatusRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;
import uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub;
import uk.gov.hmcts.darts.testutils.stubs.TransformedMediaStub;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AudioControllerAdminGetTransformedMediaIntTest extends IntegrationBase {

    private static final String ENDPOINT_URL = "/admin/transformed-medias/search";

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @Autowired
    private TranscriptionStub transcriptionStub;

    @Autowired
    private TransformedMediaStub transformedMediaStub;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private TranscriptionStatusRepository transcriptionStatusRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserIdentity userIdentity;

    @Autowired
    private ObjectMapper objectMapper;

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

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());

        SearchTransformedMediaResponse[] transformedMediaResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), SearchTransformedMediaResponse[].class);
        Assertions.assertEquals(1, transformedMediaResponses.length);

        assertResponseEquality(transformedMediaResponses[0], getTransformMediaEntity(transformedMediaResponses[0].getId(), transformedMediaEntityList));
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

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());

        SearchTransformedMediaResponse[] transformedMediaResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), SearchTransformedMediaResponse[].class);
        Assertions.assertEquals(transformedMediaEntityList.size(), transformedMediaResponses.length);

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

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());

        SearchTransformedMediaResponse[] transformedMediaResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), SearchTransformedMediaResponse[].class);
        Assertions.assertEquals(transformedMediaEntityList.size(), transformedMediaResponses.length);

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

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());

        SearchTransformedMediaResponse[] transformedMediaResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), SearchTransformedMediaResponse[].class);
        Assertions.assertEquals(0, transformedMediaResponses.length);
    }

    @Test
     void testSearchForTransformedMediaUsingAllSearchCriteria() throws Exception {
        List<TransformedMediaEntity> transformedMediaEntityList = transformedMediaStub.generateTransformedMediaEntities(4);

        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        TransformedMediaEntity mediaEntityToRequest = transformedMediaEntityList.get(2);

        // use all search criteria
        SearchTransformedMediaRequest request = new SearchTransformedMediaRequest();
        request.setRequestedAtFrom(mediaEntityToRequest.getMediaRequest().getCreatedDateTime().minusDays(2).toLocalDate());
        request.setRequestedAtTo(mediaEntityToRequest.getMediaRequest().getCreatedDateTime().minusDays(1).toLocalDate());
        request.setCaseNumber(mediaEntityToRequest.getMediaRequest().getHearing().getCourtCase().getCaseNumber());
        request.setHearingDate(mediaEntityToRequest.getMediaRequest().getHearing().getHearingDate());
        request.setOwner(mediaEntityToRequest.getMediaRequest().getCurrentOwner().getUserFullName());
        request.setRequestedBy(mediaEntityToRequest.getMediaRequest().getCreatedBy().getUserFullName());
        request.setRequestedAtFrom(mediaEntityToRequest.getCreatedDateTime().toLocalDate());
        request.setRequestedAtTo(mediaEntityToRequest.getCreatedDateTime().toLocalDate());

        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json")
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());

        SearchTransformedMediaResponse[] transformedMediaResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), SearchTransformedMediaResponse[].class);
        Assertions.assertEquals(1, transformedMediaResponses.length);
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

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());

        SearchTransformedMediaResponse[] transformedMediaResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), SearchTransformedMediaResponse[].class);
        Assertions.assertEquals(transformedMediaEntityList.size(), transformedMediaResponses.length);

        for (SearchTransformedMediaResponse response : transformedMediaResponses) {
            assertResponseEquality(response, getTransformMediaEntity(response.getId(), transformedMediaEntityList));
        }
    }

    @Test
    void testNoRequestReturnsAllResults() throws Exception  {
        List<TransformedMediaEntity> transformedMediaEntityList = transformedMediaStub.generateTransformedMediaEntities(4);

        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .header("Content-Type", "application/json"))
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        Assertions.assertEquals(200, mvcResult.getResponse().getStatus());

        SearchTransformedMediaResponse[] transformedMediaResponses
            = objectMapper.readValue(mvcResult.getResponse().getContentAsByteArray(), SearchTransformedMediaResponse[].class);
        Assertions.assertEquals(transformedMediaEntityList.size(), transformedMediaResponses.length);

        for (SearchTransformedMediaResponse response : transformedMediaResponses) {
            assertResponseEquality(response, getTransformMediaEntity(response.getId(), transformedMediaEntityList));
        }
    }

    @Test
    void testAuthorisationProblem() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity, SecurityRoleEnum.DAR_PC);

        mockMvc.perform(post(ENDPOINT_URL).header("Content-Type", "application/json"))
            .andExpect(status().isForbidden())
            .andReturn();
    }


    private TransformedMediaEntity getTransformMediaEntity(Integer id,  List<TransformedMediaEntity> transformedMediaEntityList) {
        return transformedMediaEntityList.stream().filter(e -> e.getId().equals(id)).findFirst().get();
    }

    private void assertResponseEquality(SearchTransformedMediaResponse response, TransformedMediaEntity entity) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        Assertions.assertEquals(response.getId(), entity.getId());
        Assertions.assertEquals(AudioRequestOutputFormat.ZIP.name(), entity.getOutputFormat().name());
        Assertions.assertEquals(response.getFileName(), entity.getOutputFilename());
        Assertions.assertEquals(response.getFileSizeBytes(), entity.getOutputFilesize());
        Assertions.assertEquals(dateTimeFormatter.format(response.getLastAccessedAt()),
                                dateTimeFormatter.format(entity.getLastAccessed().atZoneSameInstant(ZoneId.of("UTC")).toOffsetDateTime()));
        Assertions.assertEquals(response.getMediaRequest().getId(), entity.getMediaRequest().getId());
        Assertions.assertEquals(dateTimeFormatter.format(response.getMediaRequest().getRequestedAt()),
                                dateTimeFormatter.format(entity.getMediaRequest().getCreatedDateTime().atZoneSameInstant(ZoneId.of("UTC")).toOffsetDateTime()));
        Assertions.assertEquals(response.getMediaRequest().getOwnerUserId(),
                                entity.getMediaRequest().getCurrentOwner().getId());
        Assertions.assertEquals(response.getMediaRequest().getRequestedByUserId(), entity.getMediaRequest().getRequestor().getId());
        Assertions.assertEquals(response.getCase().getId(), entity.getMediaRequest().getHearing().getCourtCase().getId());
        Assertions.assertEquals(response.getCase().getCaseNumber(), entity.getMediaRequest().getHearing().getCourtCase().getCaseNumber());
        Assertions.assertEquals(response.getCourthouse().getId(),
                                entity.getMediaRequest().getHearing().getCourtroom().getCourthouse().getId());
        Assertions.assertEquals(response.getCourthouse().getDisplayName(),
                                entity.getMediaRequest().getHearing().getCourtroom().getCourthouse().getDisplayName());
        Assertions.assertEquals(response.getHearing().getId(), entity.getMediaRequest().getHearing().getId());
        Assertions.assertEquals(response.getHearing().getHearingDate(), entity.getMediaRequest().getHearing().getHearingDate());
    }
}