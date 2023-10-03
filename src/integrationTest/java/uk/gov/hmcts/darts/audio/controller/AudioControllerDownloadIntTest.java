package uk.gov.hmcts.darts.audio.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.component.Authorisation;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.TransientObjectDirectoryStub;

import java.net.URI;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.STORED;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@AutoConfigureMockMvc
class AudioControllerDownloadIntTest extends IntegrationBase {

    private static final URI ENDPOINT = URI.create("/audio/download");

    @MockBean
    private Authorisation authorisation;

    @MockBean
    private UserIdentity mockUserIdentity;

    @Autowired
    protected TransientObjectDirectoryStub transientObjectDirectoryStub;

    @Autowired
    private AuthorisationStub authorisationStub;

    @Autowired
    private MockMvc mockMvc;

    @SpyBean
    private DataManagementService dataManagementService;

    @Test
    void audioDownloadShouldDownloadFromOutboundStorageAndReturnSuccess() throws Exception {

        var blobId = UUID.randomUUID();

        var requestor = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        var mediaRequestEntity = dartsDatabase.createAndLoadCurrentMediaRequestEntity(requestor);
        var objectDirectoryStatusEntity = dartsDatabase.getObjectDirectoryStatusEntity(STORED);
        dartsDatabase.getTransientObjectDirectoryRepository()
            .saveAndFlush(transientObjectDirectoryStub.createTransientObjectDirectoryEntity(
                mediaRequestEntity,
                objectDirectoryStatusEntity,
                blobId
            ));

        doNothing().when(authorisation)
            .authoriseByMediaRequestId(mediaRequestEntity.getId(), Set.of(TRANSCRIBER));

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT)
            .queryParam("media_request_id", String.valueOf(mediaRequestEntity.getId()));

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk());

        verify(dataManagementService).getBlobData(eq("darts-outbound"), any());

        verify(authorisation).authoriseByMediaRequestId(
            mediaRequestEntity.getId(),
            Set.of(TRANSCRIBER)
        );

        assertEquals(1, dartsDatabase.getAuditRepository().findAll().size());
    }

    @Test
    @Transactional
    void audioDownloadGetShouldReturnErrorWhenNoRelatedTransientObjectExistsInDatabase() throws Exception {
        authorisationStub.givenTestSchema();

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT)
            .queryParam("media_request_id", String.valueOf(authorisationStub.getMediaRequestEntity().getId()));

        doNothing().when(authorisation)
            .authoriseByMediaRequestId(authorisationStub.getMediaRequestEntity().getId(), Set.of(TRANSCRIBER));

        mockMvc.perform(requestBuilder)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.type").value("AUDIO_101"));

        verify(authorisation).authoriseByMediaRequestId(
            authorisationStub.getMediaRequestEntity().getId(),
            Set.of(TRANSCRIBER)
        );
    }

    @Test
    void audioDownloadGetShouldReturnBadRequestWhenNoRequestBodyIsProvided() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT);

        mockMvc.perform(requestBuilder)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(authorisation);
    }

}
