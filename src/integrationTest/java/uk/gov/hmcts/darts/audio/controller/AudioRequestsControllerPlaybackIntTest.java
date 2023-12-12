package uk.gov.hmcts.darts.audio.controller;

import org.junit.jupiter.api.BeforeEach;
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
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.model.AuditSearchQuery;
import uk.gov.hmcts.darts.audit.service.AuditService;
import uk.gov.hmcts.darts.authorisation.component.Authorisation;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.TransientObjectDirectoryStub;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.audiorequests.model.AudioRequestType.PLAYBACK;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.STORED;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.LANGUAGE_SHOP_USER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.RCJ_APPEALS;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@AutoConfigureMockMvc
@SuppressWarnings({"PMD.ExcessiveImports"})
class AudioRequestsControllerPlaybackIntTest extends IntegrationBase {

    private static final URI ENDPOINT = URI.create("/audio-requests/playback");

    private static final Integer PLAYBACK_AUDIT_ACTIVITY_ID = AuditActivity.AUDIO_PLAYBACK.getId();
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

    @Autowired
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
    }

    @Test
    void audioRequestPlaybackShouldPlaybackFromOutboundStorageAndReturnSuccess() throws Exception {
        var blobId = UUID.randomUUID();

        var requestor = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        var mediaRequestEntity = dartsDatabase.createAndLoadOpenMediaRequestEntity(requestor, PLAYBACK);
        var objectDirectoryStatusEntity = dartsDatabase.getObjectDirectoryStatusEntity(STORED);


        dartsDatabase.getTransientObjectDirectoryRepository()
            .saveAndFlush(transientObjectDirectoryStub.createTransientObjectDirectoryEntity(
                mediaRequestEntity,
                objectDirectoryStatusEntity,
                blobId
            ));

        doNothing().when(authorisation)
            .authoriseByMediaRequestId(
                mediaRequestEntity.getId(),
                Set.of(JUDGE, REQUESTER, APPROVER, TRANSCRIBER, LANGUAGE_SHOP_USER, RCJ_APPEALS)
            );

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT)
            .queryParam("media_request_id", String.valueOf(mediaRequestEntity.getId()));

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk());

        verify(dataManagementService).getBlobData(eq("darts-outbound"), any());

        verify(authorisation, times(1)).authoriseByMediaRequestId(
            mediaRequestEntity.getId(),
            Set.of(JUDGE, REQUESTER, APPROVER, TRANSCRIBER, LANGUAGE_SHOP_USER, RCJ_APPEALS)
        );

        AuditSearchQuery searchQuery = new AuditSearchQuery();
        searchQuery.setCaseId(mediaRequestEntity.getHearing().getCourtCase().getId());
        searchQuery.setFromDate(OffsetDateTime.now().minusDays(1));
        searchQuery.setToDate(OffsetDateTime.now().plusDays(1));
        searchQuery.setAuditActivityId(PLAYBACK_AUDIT_ACTIVITY_ID);

        List<AuditEntity> auditEntities = auditService.search(searchQuery);
        assertEquals("2", auditEntities.get(0).getCourtCase().getCaseNumber());
        assertEquals(1, auditEntities.size());

    }

    @Test
    @Transactional
    void audioRequestPlaybackGetShouldReturnBadRequestWhenMediaRequestEntityIsDownload() throws Exception {
        authorisationStub.givenTestSchema();

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT)
            .queryParam("media_request_id", String.valueOf(authorisationStub.getMediaRequestEntity().getId()));

        doNothing().when(authorisation)
            .authoriseByMediaRequestId(
                authorisationStub.getMediaRequestEntity().getId(),
                Set.of(JUDGE, REQUESTER, APPROVER, TRANSCRIBER, LANGUAGE_SHOP_USER, RCJ_APPEALS)
            );

        mockMvc.perform(requestBuilder)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("AUDIO_REQUESTS_102"));

        verify(authorisation, times(1)).authoriseByMediaRequestId(
            authorisationStub.getMediaRequestEntity().getId(),
            Set.of(JUDGE, REQUESTER, APPROVER, TRANSCRIBER, LANGUAGE_SHOP_USER, RCJ_APPEALS)
        );
    }

    @Test
    @Transactional
    void audioRequestPlaybackGetShouldReturnErrorWhenNoRelatedTransientObjectExistsInDatabase() throws Exception {
        authorisationStub.givenTestSchema();
        var mediaRequestEntity = authorisationStub.getMediaRequestEntity();
        mediaRequestEntity.setRequestType(PLAYBACK);
        dartsDatabase.save(mediaRequestEntity);

        doNothing().when(authorisation)
            .authoriseByMediaRequestId(
                mediaRequestEntity.getId(),
                Set.of(JUDGE, REQUESTER, APPROVER, TRANSCRIBER, LANGUAGE_SHOP_USER, RCJ_APPEALS)
            );

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT)
            .queryParam("media_request_id", String.valueOf(mediaRequestEntity.getId()));

        mockMvc.perform(requestBuilder)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.type").value("AUDIO_101"));

        verify(authorisation, times(1)).authoriseByMediaRequestId(
            authorisationStub.getMediaRequestEntity().getId(),
            Set.of(JUDGE, REQUESTER, APPROVER, TRANSCRIBER, LANGUAGE_SHOP_USER, RCJ_APPEALS)
        );
    }

    @Test
    void audioPlaybackGetShouldReturnBadRequestWhenNoRequestBodyIsProvided() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT);

        mockMvc.perform(requestBuilder)
            .andExpect(header().string("Content-Type", "application/problem+json"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(authorisation);
    }

}
